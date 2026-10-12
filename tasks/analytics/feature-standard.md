# Единый стандарт аналитики фич

Стандарт применяется при реализации и изменении поведения фич. Канонический скилл:
[feature-analytics](../../.claude/skills/feature-analytics/SKILL.md).
Текущий эталон — [авторизация](authorization-coverage.md); механика core описана в
[integration-guide](integration-guide.md). Матрица фиксирует реальные факты и
осознанные пропуски, а не обещание трекать каждое действие приложения.

## Бизнес и аналитика

Бизнес работает на бизнес-контрактах. Аналитика — декор поверх цельных операций
и наблюдение уже известных фактов. Если убрать декоратор, запросы, fallback,
навигация и результат блока остаются теми же.

- Нельзя ветвить бизнес по аналитическим моделям, категории ошибки, snapshot или
  delivery status; нельзя создавать бизнес-результаты в analytics-api.
- Use case остаётся цельным. Login/register включают приватную подготовку;
  публичные prepare/stage callbacks/ports ради таймингов не добавляются.
- Generic T может сохранить результат без кастов и интерпретации. Детализацию,
  требующую нового бизнес-контракта исключительно ради аналитики, лучше опустить.
- Маршрут владеет своим ID. Когда это identity сценария, UI provider преобразует
  value в toolkit FlowId; serialized shape и валидация сохраняются. Entity/draft ID
  не подменяет flow ID; отсутствие route-flow identity не требует её добавления.
- Обычные ошибки reporting не блокируют бизнес, навигацию и lifecycle, не заменяют
  успешный результат или исходную бизнес-ошибку. Вторичная ошибка может быть
  suppressed; caller cancellation и fatal Error сохраняют свою семантику.
  Нарушения owner/state invariants и прямые schema checks остаются явными:
  несовместимый snapshot не маскируется новым flow.

## Слои и структура

```text
analytics/
  analytics-api/src/commonMain/.../<feature>/
    <Feature>Analytics.kt              consumer API; nested scenario contracts
    models/                            public facts/descriptors/snapshots
  <feature>/src/commonMain/.../<feature>/
    <Feature>AnalyticsModule.kt        annotated catalog registration
    internal/
      scenario/                        entry point и сценарии
      state/                           flow/navigation state
      models/                          encoding-only типы
      events/                          sealed event families
      operation/                       operation event factories
      schema/                          schemas, keys и wire matrices
ui-screen-features/<feature>/src/commonMain/.../<feature>/analytics/
  <Feature>AnalyticsProvider.kt        stateless UI adapter
  <Feature>FailureClassifierImpl.kt    при необходимости
  <Feature>AnalyticsIntegrationModule.kt  существующие annotated bindings
ui-core/foundation/src/commonMain/.../foundation/
  analytics/ScreenAnalytics.kt         public lifecycle API
  analytics/RetainedAnalytics.kt       owner-bound inline/reified mechanism
  internal/analytics/ScreenAnalyticsImpl.kt  stateless DI implementation
```

Это роли, не требование создавать пустые папки или файл на каждый event.
Один самостоятельный тип на файл; связанные варианты sealed family остаются вместе.
На фичу — один верхнеуровневый `<Feature>Analytics`, сценарии и необходимые classifier
ports вложены. Consumer модели и immutable snapshots — в соседней models/.
Encoding-only модели internal. Нет отдельного Factory, mode checks или unchecked casts.

Stateless entry point — @Single, каждый factory call создаёт независимый mutable
сценарий. Только annotated module публичен для composition root; реализация internal.
UI main зависит от analytics-api, shared подключает implementations. Implementation
зависит от analytics-api, toolkit consumer API и нужных KMP-библиотек; не зависит от
UI/foundation/Essenty/routes, domain/use cases, services/SDKs или toolkit core/bootstrap.
Pure implementation tests сохраняют эту границу; test-only dependency UI на реальную
implementation допустима для DI/integration. Public signatures определяют api
зависимости. Настройки Gradle — через convention plugins и type-safe accessors.

## Операции и переходы

Consumer вызывает `trackOperation(typedDescription, block)` внутри существующего
safeLaunch/use-case coroutine. API не получает features/use cases/SDK providers,
не делает запросов, не создаёт scope/job/dispatcher/retry policy. Один callback
содержит исходную бизнес-последовательность, без credentials/server split.

По [Kotlin-стилю](../../CLAUDE.md#kotlin) `safeLaunch`, `trackOperation`, бизнес-
и управляющие блоки развёрнуты, даже с одним вызовом. Короткие single-expression
`update` и простые Flow transformations остаются в одну строку; длинные выражения
и несколько операций разворачиваются. Named local functions внутри `init`,
конструкторов, других функций и лямбд запрещены; методы/функции — на уровне
класса/файла. Обычные callbacks, lambdas и DSL разрешены.
Startup обычно запускается из `init`. Цельная операция, повторяемая после закрытия
диалога ошибки, оформляется private методом класса и вызывается из `init`/`onError`,
без дублирования запросов или нового scope. Обёртки только ради аналитики не нужны.

```kotlin
val hasProfile = analytics.trackOperation(AuthOperation.SessionCheck) {
    hasProfile()
}
```

Ручные `ensureActive()` / `currentCoroutineContext().ensureActive()` и скрывающие
их обёртки в production запрещены как лишняя обвязка. Отмена сохраняется устройством
исполнения и распространением `CancellationException`; применять
[общее правило проекта](../../.claude/skills/validation-gates/references/forbidden-patterns.md#coroutines).

```kotlin
val hasProfile = analytics.trackOperation(AuthOperation.Login(Method.Google)) {
    val credential = googleAuthProvider.getUiProvider(context).signIn().getOrThrow()
    loginUseCase.loginGoogle(token = credential.token)
}
```

Toolkit `AnalyticsOperationEvents<T>` имеет started/completed/failed/interrupted.
Одна попытка определяет start/terminal с общим свежим OperationId; retry — новый.
Ошибки reporting, consent и queue policy могут исключить отправку этих фактов.
Timing monotonic вокруг блока. Если возвращается Result, failure раскрывается
getOrThrow в бизнес-блоке либо его существующий результат классифицируется явно;
completed не означает успех автоматически. Обработанный fallback означает завершение
выполнения, не достижение желаемого исхода. SessionCheck сохраняет caller T;
его текущий бизнес-блок возвращает Boolean проверки профиля, не SplashDirection.

SDK/user cancellation определяется конкретным типом и отличается от coroutine
interruption. Не анализировать message и не вводить stage exceptions ради payload.
Необработанные ошибки продолжают существующий safeLaunch → ErrorProvider/CrashReporting путь.
ViewModel раскрывает Result.failure через getOrThrow; необработанные ошибки
сохраняют стандартный путь. Для известной SDK-ошибки pure safeLaunch.mapError
меняет только представление диалога: аналитика, logger и CrashReporting сохраняют
оригинал. onError вызывается после закрытия диалога.
Явный бизнес-fallback допускает catch обычного Exception только вокруг минимальной
операции в той же корутине, с первым catch CancellationException и повторным throw.
Catch находится снаружи trackOperation: аналитика фиксирует исходный failure до
fallback. Нельзя ловить Throwable/fatal Error, включать в try посторонние запросы,
успешную навигацию или выполнение fallback. Их ошибки сохраняют стандартный путь.
Результат и навигация остаются бизнес-решениями; новые launches/scopes, stage flags
и обёртки не нужны. Не скрывать произвольные ошибки через catch/runCatching.
Правила и примеры — [error pipeline](../../.claude/skills/ui-feature/references/error-pipeline.md).

Синхронные переходы сообщаются реальным navigation owner через
`trackTransition(FeatureTransition)`. Enum подходит для вариантов без данных;
sealed family — когда вариант требует payload. Обязательная причина находится
в Exit(reason), не в nullable аргументе универсального метода.

```kotlin
analytics.trackTransition(AuthTransition.OpenRegistration)
analytics.trackTransition(AuthTransition.BackToLogin)
analytics.trackTransition(AuthTransition.Exit(ExitReason.UserBack))
```

Dispatch exhaustive, без else/aliases для старых методов. Повтор текущего шага
не создаёт StepChanged и не сбрасывает referrer/reason; exit deduplicated, closed
flow не переоткрывается. Метод сообщает факт, не выполняет navigation.
trackTransition, trackOperation и presentScreen имеют разные обязанности.
Destroy сам по себе не подтверждает пользовательский выход.

## UI provider и persistence

В UI analytics/ находится internal stateless `<Feature>AnalyticsProvider` с методами
`provide<Scenario>`, предоставляемый существующим annotated @Single binding.
Constructor получает API; ComponentContext приходит только в методы.
Route mapping — приватный именованный метод, ключи — const val внутри private
companion. Provider не хранит UI owners или mutable scenarios и не строит события.
Classifier при необходимости — отдельный internal файл с typed SDK/domain mapping;
implementation получает только вложенный API port.

Persisted scenarios используют общий
`RetainedAnalytics(context).provide<Scenario, Snapshot>(key, serializer, create, validate, save)`.
Порядок: consume → retainedSimpleInstance по типу Scenario → identity validation →
register save. Context локальный; helper не DI singleton. Explicit interface type
защищает ключ от смены implementation. Один сценарий такого типа на owner;
несколько требуют distinct owners/keys. Без persistence не выдумывать snapshots/keys.
Нет custom holders, публичных retention extensions или base analytics components.

```kotlin
// Component-владелец, до retained ViewModel:
private val analytics = getKoin().get<AuthAnalyticsProvider>()
    .provideAuthorization(componentContext, route)
```

Этот экземпляр передаётся детям/VM как analytics. После recreation callbacks,
navigation и StateKeeper.save используют его же; retained состояние актуальнее
старого snapshot. Snapshot создаёт сценарий только без retained instance.
Сохраняются wire shape, keys и необходимый legacy SerialName. Перед отправкой/rebinding
проверяется соответствие route ID; несовпадение не даёт тихого replacement.
Механика и executable examples:
[UI reference](../../.claude/skills/feature-analytics/references/ui-provider.md).

## Схемы, атрибуция и privacy

Schemas, keys, screen/entry codes централизованы в typed matrices. Payload принимает
bounded enum/sealed facts, проверяемые ID/единицы, не свободные strings/maps с call site.
Schema/Key instances совпадают с каталогом; lower_snake_case и версия заданы явно.
Enum кодируется explicit analyticsCode, не name. Смена смысла требует решения по
схеме/отчётам: increment version сам по себе не мигрирует GA4.

`<Feature>AnalyticsModule().module` подключается shared до первой резолюции Analytics.
Qualified @Single регистрирует полный AnalyticsSchema; built-in screen/dialog schemas
не повторяются. Каталог проверяет collisions/reserved names/budgets. Нет runtime
мутаций каталога, module-constructor runtime args или ручного production module DSL.

Flow стартует в фактической точке входа. Identity/entry/origin неизменны; referrer
и Initial/Action/Back/Restore/External reason — наблюдаемые navigation факты.
Unknown остаётся absent. Foreground не создаёт новый flow. LaunchSource отдельно
от EntryPoint; наличие URL не доказывает notification, null token — истечение сессии.
Restore обозначает повторную активацию существующей поверхности (включая foreground)
или восстановление состояния; snapshot для foreground-reason не требуется.
Поздний operation result сохраняет flow/operation ID, но не притворяется показом
экрана, который уже закрыт.

Consent/identity принадлежит core/application policy. Unknown/Denied ничего не
буферизуют для replay; фича не включает сбор и не сбрасывает identity. Credentials,
tokens/codes, input, error text, URLs и request/response bodies не попадают в payload.
Cardinality и единицы документируются. Field validation, reference loads, internal
stages и generic clicks добавляются под конкретный вопрос, а не по умолчанию.

## Показы

Leaf Component один раз разрешает ScreenAnalytics из DI:

```kotlin
getKoin().get<ScreenAnalytics>().bind(lifecycle) {
    analytics.presentScreen(Step.Login)
}
```

Public API в foundation, internal stateless @Single implementation через CoreModule.
Каждый bind владеет своим callback/presentation. Resume создаёт показ, pause/destroy
закрывает, destroy отписывает. На уже RESUMED lifecycle появляется начальный показ.
Render/VM/container не дублируют binding; dialogs/overlays имеют отдельную политику.
Позднее закрытие старой presentation не очищает новую. Старых extensions/overloads нет.
SDK automatic screen reporting выключен manifest/plist; collection consent отдельно.

## Матрица, review и проверки

`feature-coverage-template.md` задаёт матрицу: фактический trigger, один owner,
typed values, attribution, lifetime, outcomes и verifying tests. Новый behavior
имеет строку либо объяснение omission. Проверять actual Directions/callbacks/use cases,
а не только fixtures. Удалять obsolete API/schemas/helpers/bindings/tests/docs вместе.

Review проверяет простоту, naming/files, границы, каталог/ключи, DI, retention,
original errors/cancellation, reporting isolation и overhead. После исправлений —
повторный review. Static scans не проверяют Kotlin types или implicit operator/
serializer imports; нужны реальные контракты и, когда разрешено, компиляция.

Проверки включают payload/outcome fixtures, retry/IDs, failure injection,
independent owners, recreation → mutation → save → restore, mismatch до регистрации,
back/foreground/leaf lifecycle и generated DI. Тестировать production helper и actual
владельцев бизнес-запросов, не копию реализации. Snapshot/save/route-mismatch checks
применяются при наличии этих контрактов; для неперсистентного сценария — direct
retention/recreation и independent owners, без придуманного snapshot после завершения
процесса. Запускать affected Android host/iOS
simulator tests и platform integration checks в пределах указаний пользователя;
изменения app wiring расширяют соответствующие проверки. YAML/links/examples
проверяются отдельно. Фиксировать actual results/skips; исторические отчёты и
статический аудит не подтверждают последующие builds/runtime. Firebase delivery,
sign-in и mapping/symbol uploads требуют отдельного разрешения.
