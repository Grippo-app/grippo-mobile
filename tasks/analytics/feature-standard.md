# Простой стандарт аналитики фич

Текущий стандарт: [feature-analytics](../../.claude/skills/feature-analytics/SKILL.md).
Пример интеграции — [авторизация](authorization-coverage.md); детали transport core —
[integration guide](integration-guide.md). История изменений остаётся в todo/review.

При изменении поведения сначала перечислить affected leaf/graph routes, callbacks,
inputs и цельные операции в coverage. Расширять существующий API/implementation,
подключать affected VM; затем сверять матрицу с actual callers и разрешёнными checks.

## API и модули

На фичу — один stateless `<Feature>Analytics`:

```kotlin
fun trackEvent(event: FeatureEvent)
suspend fun <T> trackOperation(operation: FeatureOperation, block: suspend () -> T): T
fun presentScreen(screen: FeatureScreen, reason: NavigationReason): ScreenPresentation
```

`analytics-api/<feature>/models` содержит закрытые event/operation families,
screen enum и необходимые enums. Один самостоятельный тип на файл, связанные
sealed variants вместе. Fact constructors получают прямые typed values.
Общие InputStatus/InputKeyboardAction/ControlAction — в common/models;
fields/methods/pickers/experience остаются feature-specific.

Feature event — описание наблюдения, не toolkit AnalyticsEvent. Internal
implementation преобразует его через typed matrix в wire payload/schema.
Encoding-only данные не выходят в API: public fact enums обычные, без AnalyticsCode
и wire-string properties. Typed keys используют internal exhaustive enum encoders;
field/action keys принимают закрытые типы, не открытый AnalyticsCode.
Invalid field combinations исключены
типами: credential fields отдельно от picker enum, fixed Name не выбирается caller.
Нет string/map API, nullable bags, общих capability/scenario иерархий, aliases,
файла/helper/method на кнопку или action-wrapper над прямыми параметрами.

UI main зависит от `:analytics:analytics-api`, shared подключает implementation
`:analytics:<feature>`. Implementation internal; public только annotated module.
Он не зависит от UI/foundation/routes/domain/use cases/services/SDK/toolkit core.
Папки — по реальным обязанностям: internal implementation/events/operation/schema/
encoding models, без пустого scaffolding. Convention plugins, explicit API,
`@Single` и generated `.module`; manual production DI не добавляется.

## Бизнес отдельно

`trackEvent` наблюдает принятый callback. `trackOperation` оборачивает весь прежний
async business block и возвращает caller T без интерпретации. Submit intent не
подменяет начало запроса. Запросы/validation/retry/fallback/navigation решает бизнес.
Use case не делится на публичные prepare/stages ради аналитики; новых scope/job/
dispatcher или retry для неё нет. Оригинальные errors/cancellation сохраняются.
Обычные reporting Exceptions изолированы внутри реализации; fatal Error остаётся fatal.

Return/error contract остаётся прежним: Result разворачивается существующим
getOrThrow внутри tracked block, direct-value throwing use case вызывается напрямую.
Возвращённый Result.failure сам по себе не даёт failed execution; не прятать его
в completed и не менять business API ради decorator.

ViewModel использует существующий safeLaunch. Без local catch/
recovery wrappers/manual ensureActive; pure mapError меняет только диалог,
onError выполняется после dismissal. Ошибка reporting не меняет business result.
Бизнес lambdas multiline; короткие update/Flow operators compact. Named local
functions запрещены; повторяемый startup — private метод класса в существующем VM scope.

## ViewModel и показы

Вся feature аналитика находится в VM: `analytics: <Feature>Analytics` приходит
через обычный Component `retainedInstance { ... analytics = getKoin().get() }`.
Component constructors/routes/factories/Render не передают сервис, не выбирают
analytics screen и не создают событий. В UI analytics/ остаётся только нужный
input mapping; отдельный provider/route matrix/integration DI module не нужен.

Каждый отдельный экран обязан переопределять BaseViewModel.screenPresentation в своём VM:
`(NavigationReason) -> ScreenPresentation`, вызывая typed presentScreen.
BaseComponent в existing doOnCreate получает retained VM и вызывает internal
bindScreen(lifecycle) до attachActivation и navigator/eventListener. Foundation
ScreenAnalytics владеет per-binding reason и
close; VM не хранит Lifecycle/ComponentContext и не подписывается на activationFlow
ради screen views. Нет новых scope/jobs или ручного bind в фиче.

Graph/container VM обязан оставлять default null: ни screen_view, ни resolution ScreenAnalytics.
Nullable hook в foundation нужен контейнерам; для отдельных экранов он обязателен.
Роль определяется реально показанным UI: child-stack host и общий progress/chrome —
контейнеры; отдельная страница, включая nested/tab screen, — экран. Наличие router,
ViewModel, BaseComposeScreen или файла *Screen.kt само по себе роль не определяет.
Обычная Component/retainedInstance/ViewModel ownership сохраняется; analytics
StateKeeper/InstanceKeeper keys/snapshots/scenario retention отсутствуют.
Существующие route IDs/entry points обслуживают навигацию, не telemetry.

Первый RESUMED binding даёт Initial, последующие — Restore; pause/destroy closes
presentation. Новый binding не восстанавливает flow/referrer. ScreenAnalytics
остаётся foundation API + internal @Single implementation через CoreModule.
Автоматические SDK screen events выключены отдельно от collection consent.

Пример — [ViewModel reference](../../.claude/skills/feature-analytics/references/viewmodel.md).

## События и данные

Прямые typed events находятся в существующем Contract→VM callback после всех
existing eligibility/busy guards. Отклонённый callback не создаёт accepted intent
или operation events; проверки охватывают обе ветки без изменения business guards.
UI maps existing validation в bounded status в analytics/, не вводит новый validator.
Text status emits только при смене old/new status; Empty→Empty не создаёт первый
факт. Cache не нужен. Picker confirmations/clicks/selections не дедуплицируются.
Programmatic/error-dismissal navigation не вызывает fake user callback.
Design-system callbacks neutral/optional; default focus/keyboard поведение сохраняется.

Dialog/picker open и принятый result наблюдает owning VM в existing callbacks,
включая VM-owned onResult closure, переданный в config. Shared host/config plumbing
и Component не создают события; onDismiss не доказывает cancellation
или confirmation. Built-in schemas не подключают dialog views/close автоматически:
это explicit omission либо отдельно запрошенный typed contract с lifecycle owner.

Payload: bounded enums/status/Boolean/nonnegative counts. Нет input text/length,
credentials/tokens/codes, body measurements/entity IDs, error messages/URLs/bodies.
Consent/identity принадлежат core/application policy, не feature integration.

Schemas/keys/codes централизованы, versions явные, catalog регистрируется один раз
@Single с уникальным qualifier фичи до core resolution. Built-in screen/dialog schemas переиспользуются.
Смена смысла/удаление события требует решения для отчётов; version bump не мигрирует их.
Validation changes требуют оценки status meaning/event population даже без смены wire schema.
OperationId и duration создаёт toolkit decorator; started/completed/failed/interrupted
сохраняют business result/error. Completed означает завершение caller block.

Упрощение намеренно не собирает flow start/exit/transitions, precise flow continuity,
process-restored attribution/referrers, granular error categories и provider-cancel
outcomes. Не добавлять эти механизмы вместе с обычной фичей ради полноты отчётов.

## Coverage и проверки

Матрица перечисляет каждый route (leaf/graph), control/input и complete operation:
фактический owner/trigger/typed data и instrumentation либо объяснённый omission.
Новая фича/изменение поведения обновляет её; visual-only — impact assessment.
Одна интеграция не означает покрытие всего приложения или Firebase delivery.

Перед завершением новой фичи обязательно сверить все её реальные routes и rendered
surfaces по цепочке route → Component → VM → FeatureScreen → screenPresentation.
Для изменения существующей фичи сверить все экраны затронутого scope. Enum/матрица
сами по себе не доказывают полноту; проверить factories, Render, nested/tab screens
и альтернативные входы. Каждый экран имеет ровно один hook с правильным typed screen,
каждый контейнер — null с объяснением. Пропущенный hook, неверный screen, enum без
owner или screen views контейнера вместе с дочерним экраном блокируют завершение; omission это не отменяет.
Dialog/overlay presentation остаётся отдельным scope. Полная процедура —
[обязательная проверка](../../.claude/skills/feature-analytics/SKILL.md#mandatory-screen-coverage-gate).

Self-review: naming/boundaries/DI/matrix/privacy/business isolation/overhead;
исправить findings и повторить review. Удалять obsolete API/callers/helpers/tests/docs.
Проверять actual payloads, whole operations/results/errors/cancellation/reporting
failures, eligible callbacks, все leaf VM screen declarations и foundation hook/lifecycle.
Static/PSI/historical reports не заменяют compile/generated DI/UI/native runtime.
Запускать разрешённые affected
checks и записывать results/skips; YAML/links/examples отдельно.
Firebase sends/sign-in/uploads требуют явного разрешения.
