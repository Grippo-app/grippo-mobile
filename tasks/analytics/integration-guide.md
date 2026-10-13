# Analytics core: контракт и подключение

Правила интеграции фич — [feature-standard](feature-standard.md) и
[канонический скилл](../../.claude/skills/feature-analytics/SKILL.md).
Текущая реализация и проверяемые факты — [авторизация](authorization-coverage.md).
Этот документ описывает механизмы toolkit core, не заменяет feature API.

## Границы и схемы

UI получает один stateless `<Feature>Analytics` из analytics-api: typed
trackEvent, complete trackOperation и presentScreen. Public feature facts содержат
прямые enum/status/Boolean/count параметры; public enums не раскрывают wire-коды.
Internal exhaustive encoders и typed matrix/schema создают toolkit wire events;
field/action keys имеют закрытые типы. Общие models — только reusable enums,
без capability/scenario runtime.
Реализация в analytics/<feature> не импортирует UI/domain/SDK; shared подключает
её generated module. Кодирование — синхронное, без IO. Toolkit API, core queue/state
и platform sink adapters остаются отдельными слоями.

`Analytics.track` принимает AnalyticsEvent, optional FlowContext/OperationId.
Сырые имена/maps остаются в декларациях и транспортном payload. Event использует
тот же экземпляр Schema и ParameterKey, что зарегистрирован в каталоге.
Имена lower_snake_case, версии и enum analyticsCode явные. Проверяемые значения:
AnalyticsCount, DurationMillis и finite AnalyticsNumber; Boolean кодируется Long 0/1.
Free-form text не поддерживается. Не подменять единицы измерения.

Каталог создаётся один раз, проверяет collisions/reserved names и бюджет Firebase:
до 15 собственных параметров, резерв для 10 общих. Невалидная декларация — ошибка
конфигурации независимо от consent. При Granted неверное событие/identity в debug
вызывает исключение, release отклоняет; бизнес-декораторы изолируют обычные
reporting exceptions. Прямые проверки остаются строгими. Debug хранит до 64
диагностических записей с именем/статусом, без значений/ID/messages; release журнал
выключен. Изменение смысла требует решения по отчётам, не только новой версии.

Feature catalog подключается через `<Feature>AnalyticsModule().module` до резолюции
core, @Single с уникальным qualifier фичи возвращает полный AnalyticsSchema. Built-in screen/dialog
schemas повторно не регистрируются. Нет runtime мутаций или ручного production DI.

## Контекст

| Тип | Значение |
|---|---|
| NavigationContext | Видимый экран, известный referrer/reason, active dialog |
| FlowContext | Стабильные FlowId, EntryPoint и origin сценария |
| AnalyticsContext.launchSource | Подтверждённый способ внешнего запуска |
| OperationId | Одна попытка и её terminal; не backend idempotency key |

ScreenId/DialogId/EntryPoint — валидируемые стабильные codes, не пользовательский
ввод. FlowId/OperationId/UserId — непрозрачные ID; синтаксис не гарантирует privacy.
UserId не может быть email, именем, token или их хешем. Flow/operation IDs не
регистрируют как GA4 dimensions высокой кардинальности.
Текущая авторизация не передаёт FlowContext/FlowId в аналитику. Её route IDs/
entry points остаются navigation concerns; entity/draft ID не становится telemetry ID.

Origin/entry не меняются при возврате на экран. Каждая попытка получает новый
OperationId. screen_id в payload — поверхность в момент track; поздний результат
не сохраняет видимость ушедшего экрана. Кампании/install attribution требуют
отдельного источника данных и схемы; наличие URL не доказывает launch source.

## Consent, identity и worker

Ядро начинает с Unknown. Unknown/Denied ничего не отправляют и не буферизуют для
replay. Сбор SDK выключен платформенными defaults и bootstrap до первого consumer.
Consent UI/persistence пока отдельная задача: production сбор остаётся выключен
до явного Granted. Feature integration не предоставляет это разрешение.

Firebase сохраняет collection/consent между запусками и может перекрыть manifest/plist.
Будущая policy должна согласовать сохранённый выбор и bootstrap с auto-init;
одни static defaults не сбрасывают прежнее разрешение. Grant включает только
analytics storage/collection; advertising consent остаётся Denied.
Unknown/Denied очищают identity/properties через зарегистрированные ключи.

Смена identity/collection очищает ожидающие события. Даже изменение properties
того же пользователя очищает очередь ради атрибуции. При удалении property старый
ключ оставляют для очистки либо делают SDK migration; удаление из каталога не
удаляет уже сохранённое Firebase значение.

SDK calls сериализованы одним worker, уступающим dispatcher после 32 единиц.
Bootstrap происходит до attach, shutdown disable — после join. Gate/очередь
обновляются синхронно, SDK configuration применяется последовательно. Уже начатый
call отозвать нельзя; возврат updateCollection не подтверждает SDK completion.
Буфер до attachSink ограничен 128 событиями, overflow drop-new. Дискового хранения
и exactly-once обещания нет. Send success — принятие локальным SDK, не delivery.

Первый Granted применяет collection, затем identity; собственная очередь ждёт
обе операции. Identity update временно не отзывает consent. Это последовательность,
не атомарная транзакция автоматических SDK events.

Send failure диагностируется, следующие события продолжаются. Configuration failure
закрывает отправку и очищает очередь; retrySinkConfiguration выполняет явную
повторную настройку без replay. Настоящая отмена worker сохраняется. Синхронная
CancellationException активного sink — transport failure, не остановка worker.
Close завершает join/disable даже при отменённом caller; background не закрывает
app-lifetime worker. DiscardedBeforeSend обозначает уже извлечённое событие,
потерявшее актуальность; close учитывает оставшуюся очередь.

## Операции и lifecycle

Toolkit trackOperation вызывает четыре фабрики AnalyticsOperationEvents<T>:
started/completed/failed/interrupted, сохраняет исходный result и error identity,
измеряет monotonic duration вокруг block без времени фабрик. Reporting Exceptions
start/completed не предотвращают выполнение/возврат; вторичная failure-reporting
ошибка может быть suppressed. Выброшенная блоком CancellationException даёт
interrupted и пробрасывается без подмены. Возвращённый результат сохраняется;
декоратор не добавляет точки отмены до/после блока. Отменённый Job остаётся
отменённым, даже если несуспендящий блок успел завершиться. Fatal Error не
становится продуктовым failure. Coroutine/job/retry не создаются.

Feature API оборачивает цельную бизнес-операцию без factories/classifier/state.
Login/register включают private preparation; SessionCheck оборачивает только
hasProfile и не интерпретирует caller T. Ошибка проходит обычный safeLaunch →
logger/CrashReporting/ErrorProvider путь. Current Auth operations оставляют только
started/completed/failed/interrupted, duration и OperationId; SDK cancel отдельно
не классифицируется. Business fallback, уже обработанный внутри блока, означает
completed execution. Versions/omissions — в authorization-coverage.

`safeLaunch`/`trackOperation` и бизнес-блоки остаются multiline, короткие `update`
и простые Flow transformations — compact. Named local functions внутри `init`,
конструкторов, функций и лямбд запрещены; callbacks/DSL разрешены. Для startup,
повторяемого после диалога, используется private метод класса из `init`/`onError`,
без дублирования запросов и нового scope; см. [Kotlin-стиль](../../CLAUDE.md#kotlin).

ViewModel разворачивает Result через `getOrThrow()` внутри существующего `safeLaunch`,
а при decoration — внутри tracked block. Direct-value throwing use cases остаются
прямыми вызовами; возвращённый Result.failure не становится failed execution сам.
Существующий return/error contract не меняется ради аналитики.
Необработанную исходную ошибку получают аналитика, log и crash reporting; чистый
`mapError` меняет только представление стандартного диалога. `onError` выполняется
после закрытия диалога. Обе перегрузки safeLaunch сохраняют одинаковые onError/mapError,
defaults и pipeline; callbacks передаются по необходимости. ViewModel и private
suspend helpers не перехватывают ошибки локально и не добавляют launches/scopes,
stage flags или recovery-обёртки. Cancellation остаётся отменой. В mapper или
аналитике нельзя прятать восстановление, fallback или навигацию; см.
[единый error pipeline](../../.claude/skills/ui-feature/references/error-pipeline.md).

Component resolves analytics в обычном retainedInstance block для VM. Все feature
events/operations/screen declarations находятся в VM; constructors/routes/factories/
Render не содержат analytics wiring. Каждый отдельный экран обязан переопределять BaseViewModel
screenPresentation: `(NavigationReason) -> ScreenPresentation`, вызывая typed
presentScreen. BaseComponent во время existing doOnCreate вызывает internal
VM.bindScreen(lifecycle) после создания retained VM, до attachActivation и
navigator/eventListener. Graph default null означает
отсутствие screen binding и resolution ScreenAnalytics.

Foundation ScreenAnalytics bind принимает reason-aware presenter, владеет flag
Initial→Restore и closes на pause/destroy. Новый binding не восстанавливает
flow/referrer. Stateless @Single implementation обнаруживается CoreModule; VM
не хранит Lifecycle/ComponentContext и не наблюдает activationFlow ради views.
Existing retainedInstance/ViewModel lifecycle остаётся; новых scopes/jobs нет.
Поздний close старой presentation не очищает новую. Destroy не означает user exit.

Автоматические SDK screen events выключены в Android manifest
`google_analytics_automatic_screen_reporting_enabled=false` и iOS Info.plist
`FirebaseAutomaticScreenReportingEnabled=false`, отдельно от consent.
[Официальные настройки Firebase](https://firebase.google.com/docs/analytics/screenviews).

Feature analytics не использует StateKeeper/InstanceKeeper keys, snapshots,
retention helpers или сохранённую funnel continuity. Примеры подключения —
[ViewModel reference](../../.claude/skills/feature-analytics/references/viewmodel.md).

## Конфигурация и проверки

Сервисные api содержат public contracts, core — публичный Module и internal
engines/locks/catalog/configuration/diagnostics. Shared подключает generated
AnalyticsModule/CrashReportingModule/MessagingModule и bootstrap contracts.
AnalyticsModule сам предоставляет internal AnalyticsConfiguration через @Single.
Android получает debug из Application FLAG_DEBUGGABLE своего Koin scope; отсутствие
Application — wiring error без fallback. iOS использует Platform.isDebugBinary
связанного framework; Xcode configurations должны выбирать соответствующий XCFramework.
App configuration module и runtime constructor args для generated modules не нужны.

Перед завершением новой фичи сверить все её экраны с actual routes/Render/VM hooks;
при изменении — все экраны затронутого scope. Контейнеры оставляют default null;
пропуск hook или screen view контейнера вместе с дочерним экраном блокирует завершение, а не становится omission.
См. [обязательную проверку](../../.claude/skills/feature-analytics/SKILL.md#mandatory-screen-coverage-gate).
Новая фича описывает каждый leaf в screenPresentation своего VM, direct typed events для
существующих controls/inputs и whole business operations. Text status compares
existing old/new validation без cache; equal status omitted. Picker confirmations/
clicks/selections distinct. Neutral design-system callbacks не зависят от analytics.
Catalog/payload и source review business/lifecycle проверяют покрытие и omissions.
Dialog/picker open/result наблюдает owning VM, включая переданный config onResult;
shared host/config plumbing не создаёт события,
onDismiss не доказывает cancel/confirmation. Built-in dialog schemas не означают
automatic lifecycle tracking: dedicated views/close имеют explicit omission либо
отдельно запрошенный typed contract и owner.
Historical/PSI/static checks не подтверждают generated DI/platform/UI runtime;
запускать affected checks по текущим разрешениям. GA4 reports/BigQuery и Firebase
sends/sign-in/uploads — отдельный scope.
