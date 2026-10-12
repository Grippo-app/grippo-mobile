# Analytics core: контракт и подключение

Правила интеграции фич — [feature-standard](feature-standard.md) и
[канонический скилл](../../.claude/skills/feature-analytics/SKILL.md).
Текущая реализация и проверяемые факты — [авторизация](authorization-coverage.md).
Этот документ описывает механизмы toolkit core, не заменяет feature API.

## Границы и схемы

UI получает `<Feature>Analytics` из analytics-api. Реализация в analytics/<feature>
использует toolkit Analytics; shared подключает generated modules. Она не знает
Firebase, AnalyticsSink/Bootstrap, UI или domain. Кодирование событий — чистая
синхронная функция без IO. Analytics API, core state/queue и платформенные sink
adapters разделены.

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
core, qualified @Single возвращает полный AnalyticsSchema. Built-in screen/dialog
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
регистрируют как GA4 dimensions высокой кардинальности. Маршрут владеет своим
screen-api ID, преобразованным в toolkit FlowId только у UI provider.
Business entity/draft ID не является flow identity и не используется вместо него.

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

Feature API оборачивает цельную бизнес-операцию. Login/register включают приватную
подготовку, SessionCheck не интерпретирует caller T. Текущий SessionCheck оборачивает
только hasProfile; его исходная ошибка после failed проходит стандартный
safeLaunch → logger/CrashReporting/ErrorProvider путь, без локального catch.
Бизнес-fallback, уже обработанный внутри любого декорируемого блока, означал бы
completed execution, а не желаемый исход. Текущие event versions и
осознанные omissions находятся в authorization-coverage, без дублирования каталога.

`safeLaunch`/`trackOperation` и бизнес-блоки остаются multiline, короткие `update`
и простые Flow transformations — compact. Named local functions внутри `init`,
конструкторов, функций и лямбд запрещены; callbacks/DSL разрешены. Для startup,
повторяемого после диалога, используется private метод класса из `init`/`onError`,
без дублирования запросов и нового scope; см. [Kotlin-стиль](../../CLAUDE.md#kotlin).

ViewModel разворачивает Result через `getOrThrow()` внутри существующего `safeLaunch`.
Необработанную исходную ошибку получают аналитика, log и crash reporting; чистый
`mapError` меняет только представление стандартного диалога. `onError` выполняется
после закрытия диалога. Обе перегрузки safeLaunch сохраняют одинаковые onError/mapError,
defaults и pipeline; callbacks передаются по необходимости. ViewModel и private
suspend helpers не перехватывают ошибки локально и не добавляют launches/scopes,
stage flags или recovery-обёртки. Cancellation остаётся отменой. В mapper или
аналитике нельзя прятать восстановление, fallback или навигацию; см.
[единый error pipeline](../../.claude/skills/ui-feature/references/error-pipeline.md).

Leaf один раз вызывает DI ScreenAnalytics.bind(lifecycle) с callback presentScreen.
Один показ на RESUMED, включая уже resumed registration и foreground return;
pause/destroy закрывает, destroy отписывает. Stateless internal @Single implementation
обнаруживается CoreModule; callbacks хранят state отдельно для каждого bind.
Render/container/VM не дублируют показы. PresentScreen атомарно меняет видимость и
захватывает screen_view, сохраняя launch attribution; поздний close старой presentation
не очищает новую. Destroy не означает пользовательский exit.

Автоматические SDK screen events выключены в Android manifest
`google_analytics_automatic_screen_reporting_enabled=false` и iOS Info.plist
`FirebaseAutomaticScreenReportingEnabled=false`, отдельно от consent.
[Официальные настройки Firebase](https://firebase.google.com/docs/analytics/screenviews).

Persisted scenarios используют foundation RetainedAnalytics: consume → typed
retention → validate → register. Feature provider владеет mapping/keys/serializer/
create/save, не хранит owner. Полные примеры —
[UI reference](../../.claude/skills/feature-analytics/references/ui-provider.md).

## Конфигурация и проверки

Сервисные api содержат public contracts, core — публичный Module и internal
engines/locks/catalog/configuration/diagnostics. Shared подключает generated
AnalyticsModule/CrashReportingModule/MessagingModule и bootstrap contracts.
AnalyticsModule сам предоставляет internal AnalyticsConfiguration через @Single.
Android получает debug из Application FLAG_DEBUGGABLE своего Koin scope; отсутствие
Application — wiring error без fallback. iOS использует Platform.isDebugBinary
связанного framework; Xcode configurations должны выбирать соответствующий XCFramework.
App configuration module и runtime constructor args для generated modules не нужны.

Новая фича добавляет матрицу наблюдаемых фактов, catalog/payload fixtures и проверки
actual owners. Проверяются retries, ошибки/cancellation/reporting failures,
back/restore/foreground, independent flows, consent/identity и отсутствие sensitive
fields. Recording sink находится в commonTest; production/preview его не содержат.
Android host/iOS simulator tests и app/Swift compilation запускаются согласно
текущим разрешениям. GA4 dimensions/funnels/BigQuery — отдельный этап от Kotlin wiring.
Firebase delivery/sign-in и mapping/symbol uploads не входят в разрешение на builds/tests.
