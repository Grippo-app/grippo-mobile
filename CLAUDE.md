# grippo-mobile

## Reading these rules

`MUST` / `MUST NOT` are mandatory; `MAY` identifies a permitted choice, not a
requirement. Each topic has one canonical owning skill/reference. Summaries and
examples MUST NOT override it. Current source establishes actual signatures,
versions and callers; an existing deviation does not weaken a declared rule.
Resolve a material conflict using source evidence and the authorized task scope,
then update the owning rule and affected references together.

## Project purpose

Grippo KMP client — Android + iOS from a single codebase. UI in Compose
Multiplatform, navigation via Decompose. Primary consumer of the backend
API.

---

## Stack

- Kotlin **2.4.20**, explicit API mode (enabled in `KotlinMultiplatformConventionPlugin`).
- Compose Multiplatform **1.12.1** + Kotlin Compose plugin. Compiler metrics and reports enabled via convention plugin.
- Targets: `commonMain`, `androidMain`, `iosMain` (`iosArm64`, `iosSimulatorArm64`). Android `compileSdk = 37`, `minSdk = 26`. JVM toolchain 19.
- Decompose **3.5** + Essenty (`back-handler`, `state-keeper`, `lifecycle`).
- Koin **4.2** + `koin-annotations` **2.3.1** + KSP. KOIN_CONFIG_CHECK is disabled (TODO until next Koin Annotations release).
- Ktor **3.6** (Android engine + Darwin engine), `kotlinx-serialization-json` **1.11**.
- Room **2.8** multiplatform, `androidx.sqlite-bundled`, `BundledSQLiteDriver` on iOS, migrations 2→3, 3→4, 4→5 in `:data-services:database`.
- AndroidX DataStore **1.2** (preferences-core).
- Coil **3** (`coil-compose`, `coil-network-ktor3`).
- Firebase: Android SDK adapters in `:data-services:firebase`, Swift SDK adapters in `iosApp`; common API/core in `:toolkit:{analytics,crash-reporting,messaging}:{api,core}`.
- Auth: Google (`androidx-credentials` + Google Identity), Apple (custom).
- Build: Gradle with convention plugins in `build-logic/convention/`, version catalog in `gradle/libs.versions.toml`. Type-safe project accessors enabled.
- iOS deliverable: static XCFramework `shared.xcframework` via `ios.swiftpackage.convention` (Decompose api re-exported for Swift, `-lsqlite3` linker flag).

---

## Module layout

`settings.gradle.kts` is the single source of truth for the module list.
Groups:

### App shells (thin, no business logic)

- `:androidApp` — `Application` + `MainActivity` + DI startup + Firebase setup. Decomposes root via `retainedComponent { RootComponent(...) }`. Edge-to-edge, splash screen, deeplink from intent (`onCreate` for cold start, `onNewIntent` for warm).
- `:iosApp` — Xcode project. Links `shared.xcframework` and starts `RootViewController` from `iosMain`. AppDelegate owns one lazy root and restores its StateKeeper before construction. The single SwiftUI scene saves a background checkpoint and discards it on becoming active; existing ApplicationLifecycle owns lifecycle transitions.

### `:shared` — composition root

- `Koin.kt` — `Koin.init { ... }` starts all Koin modules **explicitly by enumeration** and returns `bootstrap/ApplicationBootstrap` with typed startup contracts. Any new module is added here manually. Each service core owns its public annotated DI module; implementations live in `core/internal` with Kotlin `internal` visibility. AnalyticsModule owns its internal platform configuration through an `expect/actual` factory and `@Single`. App shells attach SDK adapters.
- `RootComponent` / `RootViewModel` / `RootScreen` / `RootContract` / `RootDirection` / `RootLoader` / `RootState` — primary stack navigator + token observer for auto-logout (`authorizationFeature.getToken()` → `onEach { if (it == null) navigateTo(Login) }`).
- `state/ApplicationState` — shell archive codec and StateKeeperDispatcher owner. Swift uses its typed stateKeeper when constructing the root; features keep their existing local PersistenceKey drafts. No platform storage, session authority or generic ViewModel persistence lives in this class.
- `DialogComponent` — separate slot navigator for bottom sheets layered above the stack.

### Design system

- `:design-system:core` — `AppTokens` (facade over CompositionLocals).
- `:design-system:components` — atomic Composables (`Button`, `Toolbar`, `Input*`, `WeightHistoryChart`, ...).
- `:design-system:resources:provider` — `Res.string.*` (Compose Multiplatform Resources), `StringProvider` and `AppColor` interfaces, plus `AppDp`/`AppTypography` data objects and `AppString`/`AppDrawable`/`AppIcon` objects.
- `:design-system:resources:provider-impl` — `ResourcesProviderModule` + `StringProviderImpl`.
- `:design-system:preview` — `@AppPreview` (multi-preview annotation: phone EN big + UK small), `PreviewContainer` (CompositionLocalProvider + Coil preview handler + `AppTheme(darkTheme = isSystemInDarkTheme())`).

### UI core

- `:ui-core:foundation` — `BaseViewModel`, `BaseComponent`, `BaseScreen`, `OperationManager`, `ResultManager`/`ResultEmitter` (cross-component results), `BaseDirection`/`BaseLoader`/`BaseRouter`/`BaseResult`/`PersistenceKey`, `collectAsStateMultiplatform` (expect/actual).
- `:ui-core:state` — reusable UI data classes (`MuscleLoadSummaryState`, `DigestState`, `TrainingStreakState`, formatters: `WeightFormatState`, `EmailFormatState`, `PasswordFormatState`, `DateFormatState`, `UiText`, etc.). Each class is `@Immutable`; many have a `stub*()` function.
- `:ui-core:error:error-provider` (`ErrorProvider` interface, `AppError` sealed) and `:ui-core:error:error-provider-impl` (`ErrorProviderImpl` maps `AppError` → `AppErrorState` → shows `DialogConfig.ErrorDisplay`).

### `:ui-screen-features:*` — full-screen features

- `:screen-api` — public `*Router` sealed classes (`@Serializable`) and `Deeplink` enum. Used by Decompose for type-safe navigation **between features**.
- Features include `authorization`, `main`, `home`, `profile`, `training`, `trainings`, `debug`. Multi-screen flows have a public entry Component with an inner stack and internal child Components. A single-screen feature may expose its screen Component directly; do not create an empty graph for future screens. Follow [UI module ownership](.claude/skills/ui-feature/references/module-structure.md).

- `:main` owns the Home/Calendar/Profile tab shell and bottom bar. `:shared` supplies child-component factories; `:main` does not depend on feature implementations. Factories return `BaseComponent<*>` directly. The bottom bar remains visible throughout all tab flows. Main owns the central workout action, including draft continuation and the goal setup suggestion; Home and Calendar do not own launch buttons.

### `:ui-dialog-features:*` — bottom sheet flows (~25 modules)

- `:dialog-api` — `DialogConfig` sealed, `DialogController.open(config)`, `DialogProvider`, `DialogModule`.
- Each dialog feature is a standalone `Component`/`ViewModel`/`Screen` package, looking **identical to a screen feature** except the host is `DialogComponent` instead of `RootComponent`.

### `:data-services:*` — low-level services and DTOs

- `:backend` — `GrippoApi` (flat class with methods like `suspend fun <action>(body): Result<T>`), `BackendClient` (Ktor with Auth, Logging, ContentNegotiation, defaultRequest pointed at `https://grippo-app.com`), `TokenProvider` (Auth `Bearer` + refresh with mutex and retry-with-backoff), `ClientLogger`, `dto/<area>/*` — `@Serializable data class`, all fields nullable with `@SerialName`.
- `:database` — Room `@Database(version = 5)`, all `@Entity`, DAOs, migrations, `models/*Pack` (`@Embedded` + `@Relation`), `converters/StringListConverter`, `DatabaseBuilder` (expect/actual), **`fallbackToDestructiveMigration(dropAllTables = true)` on both platforms — intentional**.
- `:datastore` — wrapper over AndroidX DataStore.
- `:google-auth` / `:apple-auth` — platform credential wrappers returning a Google ID token or Apple authorization code.
- `:firebase` — Android `AnalyticsSink` / `CrashReportingSink` / `PushTokenProvider` adapters and the system FCM service. Only `:androidApp` depends on this transport module. Swift adapters live in `iosApp`; shared and features depend on toolkit contracts.

### `:data-features:*` — domain layer

- `:feature-api` — `*Feature` interfaces + `*UseCase` classes + domain models. **Only this** is visible to UI modules.
- Implementations: `:trainings`, `:user`, `:weight-history`, `:goal`, `:muscle`, `:equipment`, `:exercise-examples`, `:exercise-metrics`, `:excluded-muscles`, `:excluded-equipments`, `:authorization`, `:local-settings`. Each has its own Koin `*FeatureModule` including only the actual service dependencies (Backend/Database for API-backed features, DataStore for local settings). Inside: `data/<X>RepositoryImpl` + `domain/<X>Repository` + `domain/<X>FeatureImpl`. Repository — `@Single(binds = [<X>Repository::class])`, FeatureImpl — `@Single(binds = [<X>Feature::class])`. Repository and FeatureImpl **internal**; Feature interface **public**.

### `:data-mappers:*` — directional mappers

7 modules, one per direction. No cross-direction dependencies. See **Mappers** section below.

### `:analytics:*` — feature telemetry

- `:analytics:analytics-api` defines one stateless `<Feature>Analytics` service with typed `trackEvent`, complete generic `trackOperation` and typed `presentScreen` as implemented by the current scope. Screen-only integration exposes presentScreen and its enum; event/operation APIs and models accompany actual instrumentation, with no empty scaffolding. Sibling `models/` hold closed event/operation families, screens and necessary feature enums; common/models reuses InputStatus/InputKeyboardAction/ControlAction. Public feature events are observations, not toolkit wire events. `:analytics:<feature>` keeps implementation/encoding/schema matrices internal and exposes one annotated module. UI main depends on the API; shared wires implementations. No feature analytics factories, snapshots, classifiers or capability hierarchy; no UI/domain/service/SDK dependencies in implementations.
- Feature behavior changes MUST apply the canonical [feature-analytics skill](.claude/skills/feature-analytics/SKILL.md), including its screen coverage gate and affected coverage matrix. Independent screens require their VM's `screenPresentation`; containers keep null. Visual-only changes require an impact assessment. The skill owns API, event, operation, privacy and lifecycle rules; this overview does not redefine them.

### `:toolkit:*` — platform-aware utilities

- `:analytics:{api,core}`, `:crash-reporting:{api,core}`, `:messaging:{api,core}` — typed service contracts and bounded app-lifetime cores. Features inject consumer APIs; app shells use bootstrap interfaces. Messaging callback validation belongs to core, not the Firebase service.

- `:context` — `NativeContext` (expect/actual: Android holds `Context`, iOS — pure object), `ContextModule`.
- `:http-client` — Ktor `HttpClient` with `responseValidator` + `ApiErrorParser` (translates HTTP into `AppError`).
- `:serialization` — `Json` provider (lenient, ignoreUnknownKeys).
- `:logger` — `AppLogger` (singleton with `General/Navigation/Network/Mapping`, file output expect/actual).
- `:date-utils` — `DateTimeUtils`, `DateRange`, `DateRangeKind`, `DateRangePresets`, `DateFormat`, `DateFormatting.install(locale)`.
- `:theme`, `:localization` — `AppTheme.current`/`AppLocale.current` (system theme and locale via expect/actual).
- `:image-loader` — Coil setup.
- `:link-opener`, `:notification-manager`, `:permission-manager` — platform expect/actual.
- `:connectivity` — `Connectivity.statusUpdates: SharedFlow<Connectivity.Status>`, auto-start via `ConnectivityOptions(autoStart = true)`.

### `:compose-libs:*` — reusable Compose widgets outside the design system

`chart`, `konfetti`, `segment-control`, `wheel-picker`.

### `:build-logic/convention/*` — Gradle convention plugins

See **Convention plugin matrix** below.

---

## Module dependency rules

The dependency graph is strictly directional. Violations break the architecture:

```
:androidApp / :iosApp
        ↓
     :shared  (composition root, imports EVERYTHING)
        ↓
┌──────────────────────────────────────────────────────────┐
│ :ui-screen-features:* → :ui-dialog-features:*           │
│        ↓                       ↓                        │
│ :ui-core:foundation, :ui-core:state, :ui-core:error     │
│        ↓                       ↓                        │
│ :design-system:components → :design-system:core         │
│                              ↓                          │
│              :design-system:resources:provider          │
└──────────────────────────────────────────────────────────┘
        ↓
:data-features:feature-api  (UI sees ONLY this from the data layer)
        ↓
:data-features:<feature>  (Repository/FeatureImpl, internal)
        ↓
:data-services:{backend, database, datastore, firebase, *-auth}
        ↓
:toolkit:*  (http-client, logger, serialization, ...)
```

Hard rules:

- **A UI module (`:ui-screen-features:*`, `:ui-dialog-features:*`) does NOT depend** on `:data-services:*` directly — only via `:data-features:feature-api`. The existing authorization credential flow is the exception for `:data-services:google-auth` and `:data-services:apple-auth`; Firebase transport stays app-owned.
- **`:data-features:feature-api`** does not depend on `:data-services:*`. It is pure contracts + domain models.
- **`:data-mappers:*`** depend on adjacent layers (`dto-to-entity` → `:data-services:backend` + `:data-services:database`), and `domain-to-state` / `state-to-domain` may depend on `:ui-core:state`. They MUST NOT depend on UI feature implementations or Compose widgets, or on another mapper direction.
- **`:analytics:analytics-api`** defines pure feature telemetry contracts. UI main depends on this API; shared wires feature implementations. **`:analytics:<feature>`** does not depend on UI, data/domain, services, SDKs or toolkit core; observe typed facts and decorate supplied complete business blocks.
- **`:toolkit:*`** does not depend on the data layer or Firebase SDK. Service API modules contain pure contracts; core modules own state/queues and their Koin modules.
- **`:design-system:*`** does not depend on the data layer. `AppTokens` are pure UI primitives.
- `:shared` is the only module that imports "everything". `:androidApp`/`:iosApp` reach `RootComponent` and `Koin.init` only through `:shared`.

---

## Convention plugin matrix

Module builds MUST use convention plugins and the version catalog. Shared SDK/toolchain/opt-in configuration belongs in conventions; module dependencies and existing module-specific Android namespaces remain local. Inspect the actual script before editing; do not copy historical overrides as permission to duplicate global settings.

| Module type | Plugins to apply |
|---|---|
| `:androidApp` | `android.application.convention` + `compose.compiler` + `jetbrains.compose` + `google-services` + `firebase-crashlytics` |
| Pure KMP module (no UI) | `android.library.convention` + `kotlin.multiplatform.convention` |
| KMP module with Compose UI | `android.library.convention` + `kotlin.multiplatform.convention` + `compose.multiplatform.convention` |
| Module with Koin DI | + `koin.annotation.convention` |
| `:data-services:database` | + `room.convention` (KSP + Room compiler for all iOS targets) |
| `:shared` | all five: `android.library.convention` + `kotlin.multiplatform.convention` + `ios.swiftpackage.convention` + `compose.multiplatform.convention` + `koin.annotation.convention` |

What each does:

- `kotlin.multiplatform.convention` — applies `kotlin("multiplatform")`, **`explicitApi()`**, registers iOS targets (`iosArm64`, `iosSimulatorArm64`), `applyDefaultHierarchyTemplate`, **global `optIn(...)`** (Material3 ExperimentalApi, Foundation, Coroutines, ForeignApi, Decompose Delicate/Experimental, kotlin.time, kotlin.uuid, etc.).
- `android.library.convention` — applies `com.android.kotlin.multiplatform.library`, KMP Android target with `compileSdk = 37`, `minSdk = 26`, `namespace = "com.grippo"`, JVM toolchain 19.
- `android.application.convention` — Android `applicationId/SDK`, `compileSdk = 37`, `minSdk = 26`, `targetSdk = 36`, JVM 19.
- `compose.multiplatform.convention` — Compose plugins, metrics/reports in `build/compose-{metrics,reports}`, stability is inferred from code annotations and types.
- `koin.annotation.convention` — KSP, Koin Core + Annotations, `kspCommonMainMetadata`, `KOIN_CONFIG_CHECK = false` (TODO until next Koin release).
- `room.convention` — KSP + Room compiler for Android and all iOS targets, `room.schemaLocation = $projectDir/schemas`.
- `ios.swiftpackage.convention` — assembles `XCFramework("shared")`, **static** framework, exports Decompose API + analytics/crash-reporting/messaging API contracts, `linkerOpts: -lsqlite3`.

Additional global settings (`gradle.properties`):

- `kotlin.native.binary.gc=cms` is commented out; no explicit collector override is active.
- `kotlin.native.binary.smallBinary=true` — smaller iOS frameworks.
- `org.gradle.workers.max=1` — peak memory during native release linking.
- `kotlin.daemon.jvmargs=-Xmx2g`, `kotlin.native.jvmArgs=-Xmx6g`, `org.gradle.jvmargs=-Xmx8g`.
- `org.gradle.configuration-cache=true` — enabled.

---

## MVI contract (mandatory for every screen and dialog)

Every screen/dialog = a package with **seven files** of the same template:

```
<feature>/
  <Name>Component.kt       // Decompose Component, owns ViewModel, handles Direction
  <Name>Contract.kt        // @Immutable interface with on*-callbacks + companion object Empty
  <Name>State.kt           // @Immutable data class | data object | sealed interface
  <Name>Direction.kt       // sealed interface : BaseDirection (navigation intents)
  <Name>Loader.kt          // @Immutable sealed interface : BaseLoader (types of async ops)
  <Name>ViewModel.kt       // BaseViewModel<State, Direction, Loader>, implements Contract
  <Name>Screen.kt          // @Composable internal fun, takes (state, loaders, contract)
```

### `BaseViewModel<STATE, DIRECTION : BaseDirection, LOADER : BaseLoader>`

Full API which **must** be used instead of manual coroutines/channels:

| API | Purpose |
|---|---|
| `state: StateFlow<STATE>` | Read-only snapshot for UI. |
| `protected fun update(updateFunc: (STATE) -> STATE)` | The only way to mutate state. |
| `loaders: StateFlow<ImmutableSet<LOADER>>` | Currently active operations (button loading, skeleton). |
| `protected suspend fun <T> withLoader(loader: LOADER?, block: suspend () -> T): T` | Add loader for the duration of the block. Use in `mapLatest`/`flatMapLatest` chains. |
| `navigator: Flow<DIRECTION>` | Internal `Channel<DIRECTION>(CONFLATED)`. **Intentionally conflated**: rapid events may collapse. |
| `protected fun navigateTo(destination: DIRECTION)` | Emit a Direction. |
| `protected fun safeLaunch(dispatcher, loader, onError, mapError, block)` | Single-operation launcher in the owned VM scope, until completion or cancellation/destruction. Defaults to `Main.immediate`, no loader, no dismissal callback, identity error mapping and the standard error pipeline. |
| `protected fun <T> Flow<T>.safeLaunch(dispatcher, processing, onError, mapError)` | Stream subscription. Defaults to `Main.immediate`, `WhileActive`, no dismissal callback, identity error mapping and the standard error pipeline. Stream sleeps 1s after the screen leaves RESUMED. |
| `protected enum class Processing { WhileActive, Infinity }` | Flow subscription modes only; `WhileActive` suppresses upstream when the screen is not RESUMED. |
| `attachActivation(flow)` / `detachActivation()` | Managed automatically by `BaseComponent` via lifecycle. |

Manual `ensureActive()` / `currentCoroutineContext().ensureActive()` checks are
forbidden in production as unwanted cancellation boilerplate. Preserve owned
scopes and cancellation propagation; follow the [project coroutine rules](.claude/skills/validation-gates/references/forbidden-patterns.md#coroutines).

ViewModels keep feature data in State and change it through update. Private runtime
flags, previous-source caches and shadow snapshots for initialization, restoration,
edit detection or synchronization are forbidden, including restorePending,
previousSource and previousSourceSelection. Do not hide equivalents in State,
wrappers or mutable closures. Derive conditions from actual data and use the existing
initialState boundary for one-time initialization; follow the
[draft policy](.claude/skills/state-preservation/SKILL.md#data-backed-form-synchronization).

When to use which dispatcher:

- `Dispatchers.Main.immediate` (default) — state updates, dialog show, navigation.
- `Dispatchers.IO` — explicit heavy IO **inside** the ViewModel (rare; usually IO is already in `Repository`/`BackendClient.invoke`).
- `Dispatchers.Default` — CPU-heavy work (sorting, mapping large collections).

Error pipeline (single path for unhandled failures, from any `safeLaunch`):

```
throw inside safeLaunch
    ↓
operationManager catches via CoroutineExceptionHandler
    ↓
BaseViewModel.sendError(exception, onError, mapError)
    ↓
AppLogger.General.error(...)            // file log
CrashReporting.reportException(...)              // common core → platform Crashlytics adapter
ErrorProvider.provide(mapError(exception), onError) // UI presentation only
    ↓
ErrorProviderImpl maps AppError → AppErrorState
    ↓
dialogController.open(DialogConfig.ErrorDisplay(title = state.title(), error = state, onClose = onError))
```

Unhandled failures use `getOrThrow()` inside the existing `safeLaunch`; do not
hide, replace or present them through local `try/catch`, `runCatching` or result
failure handlers. `Result.onSuccess` alone does not propagate failure. A required Result MUST be unwrapped with `getOrThrow()` or propagated to a caller that handles it; success-side effects do not waive that requirement.
Private suspend helpers use the caller's coroutine and let failures reach
`safeLaunch`; do not add recovery wrappers, extra launches or stage flags.
Analytics observes the original failure without determining recovery.
Necessary UI presentation mapping uses pure `mapError: (Throwable) -> Throwable`;
unknown errors remain unchanged and analytics/logging/crash reporting keep the
original error. Cancellation is not handled as an error; fatal errors are not
transformed. `onError` runs only after dialog dismissal. See the
[canonical error rules](.claude/skills/ui-feature/references/error-pipeline.md).

### `BaseComponent<DIRECTION : BaseDirection>`

- Delegates `ComponentContext`, is a `KoinComponent`. Created in the parent component via `<X>Component(componentContext = ..., closeCallback = ..., navCallback = ...)`.
- `protected abstract val viewModel: BaseViewModel<*, DIRECTION, *>`. Created via `componentContext.retainedInstance { <X>ViewModel(getKoin().get(), ...) }`. All dependencies, including stateless feature analytics, resolve via `getKoin().get()` inside that block, not via Component constructor.
- On `lifecycle.doOnCreate` — obtains the retained VM, calls its optional bindScreen(lifecycle), then attachActivation(lifecycle.asActiveFlow()), and finally subscribes to navigator/eventListener. Binding precedes delivery of navigation already emitted by VM init.
- On `lifecycle.doOnDestroy` — `detachActivation`, `resultManager.clear`, cancels coroutineScope.
- `protected abstract suspend fun eventListener(direction: DIRECTION)` — navigation handler; usually `when (direction) { ... }`. `Back/Close` map to constructor lambdas (`back()`/`close()`), nested routes — to `navigation.push(...)` of the root Component.
- `protected fun observeResult<T>(key: ResultKey<T>, onResult)` / `sendResult(key, data)` — cross-component communication on top of the Decompose stack (without threading callbacks down).
- `@Composable abstract fun Render()` — usually: `val state = viewModel.state.collectAsStateMultiplatform()`, `val loaders = viewModel.loaders.collectAsStateMultiplatform()`, `<X>Screen(state.value, loaders.value, viewModel)`.
- `BackCallback(onBack = viewModel::onBack)` is registered in `init { backHandler.register(...) }` if the screen handles back itself.

### `BaseScreen.kt` — UI root

`BaseComposeScreen(background, content)` wraps screen content in a Column and clears focus on background tap. The caller supplies `ScreenBackground.Color`: screen or dialog background from AppTokens. See [foundation API](.claude/skills/ui-feature/references/base-classes.md#basecomposescreen--screenbackground).

### Inter-feature navigation: Routers

Abbreviated route excerpt from `:ui-screen-features:screen-api`; omitted variants are not a complete route catalog:

```kotlin
@Serializable
public sealed class RootRouter : BaseRouter {
    @Serializable public data object Home : RootRouter()
    @Serializable public data class Profile(val value: ProfileRouter) : RootRouter()
    @Serializable public data class Training(val stage: StageState) : RootRouter()
}

@Serializable
public sealed class ProfileRouter : BaseRouter {
    @Serializable public data object Body : ProfileRouter()
    @Serializable public data object Settings : ProfileRouter()
}
```

Decompose `childStack(serializer = RootRouter.serializer(), initialConfiguration = ..., key = "RootComponent", childFactory = ::createChild)` supports screen-route restoration when the platform shell supplies saved StateKeeper state — therefore **all Routers and payloads inside them (`StageState`, etc.) must be `@Serializable`**. Android uses `retainedComponent`; iOS restores a consumed, validated checkpoint saved after entering background. An abrupt active-process termination has no guaranteed checkpoint. See [platform policy](.claude/skills/ui-feature/references/process-death.md#ios-specifics).

For every new or changed feature, MUST apply the canonical
[state-preservation skill](.claude/skills/state-preservation/SKILL.md), including
its ownership inventory and preservation gate. Nonsecret user-owned drafts use
local StateKeeper; data-backed presentation is rebuilt, secrets/runtime state
are excluded. The skill owns archive keys, restoration precedence and form
synchronization. [Process-death restoration](.claude/skills/ui-feature/references/process-death.md)
owns implementation examples and platform limits.

`Deeplink` — a simple `enum class Deeplink(val key: String)` with `fromKey(key)`. Handled in `RootViewModel` (`enqueueDeeplink` for cold start / `applyDeeplink` for warm / `parseDeeplink` to map to a Direction).

### Animations

In `RootScreen`/`*RootScreen` the stack animation is per-child:

```kotlin
animation = stackAnimation(selector = { child, _, _, _ -> child.instance.animator() })

private fun RootComponent.Child.animator(): StackAnimator = when (this) {
    is RootComponent.Child.Authorization -> fade()
    is RootComponent.Child.Home -> fade()
    is RootComponent.Child.Profile -> platformStackAnimator()  // iOS-style slide on iOS, default on Android
    // ...
}
```

`platformStackAnimator()` and `platformAnimation()` — `expect/actual` in `:ui-core:foundation`.

### Dialog navigation

Fully separate subgraph parallel to the screen stack navigator:

- `DialogController.open(config: DialogConfig)` starts a new sheet session; an existing session closes first. The controller is injected into ordinary screen ViewModels via Koin.
- `DialogConfig` is a runtime sealed class with a required `title: UiText`. It is not serialized. `hasSameContentAs(other)` compares typed content and titles, excluding callbacks, to ignore duplicate nested requests.
- `onDismiss: (() -> Unit)?` runs when a step is removed. Callbacks remain live only for the lifetime of the in-memory session.
- `dismissBySwipe: Boolean = true` controls swipe and scrim dismissal. Back navigates through the nested stack; toolbar Close dismisses the whole session.
- `DialogComponent` in `:shared` uses `SlotNavigation<DialogSession>`. Its content uses `StackNavigation<DialogStep>`. Both use `serializer = null`: dialogs are not restored after process death. The retained ViewModel preserves the live session during ordinary screen recreation.
- Nested-opening features emit semantic directions such as `PickBodyWeight(initial)` and `PickDuration(initial)`. Their components forward typed callbacks; the host chooses the dialog/title and connects the result to the feature. These feature modules do not depend on `dialog-api`.
- `DialogController.Session` is bound to the originating step and session. Its `push`, `replaceCurrent`, `back`, and `close` methods ignore inactive or removed steps. It is supplied by the host, not injected through Koin.
- Pickers return results via typed callbacks in their configs. The host guards delivery using the originating step and delivers results after popping the nested picker or closing the root sheet.

### Screen results vs dialog callbacks

The detailed rules are in [.claude/skills/ui-feature/references/results.md](.claude/skills/ui-feature/references/results.md).

- Dialogs keep typed `DialogConfig.onResult` callbacks and their session `finish(result)`.
- Returning screens use the existing BaseComponent result bus. The receiver defines
  keys in its ViewModel companion (constant or stable entity-ID function), subscribes
  once before launch, and passes explicit `resultKey: ResultKey<Result<Payload>>` through the producer's
  serializable route and Component constructor. Producer keys are never hardcoded.
- The producer Component handles a result Direction with `sendResult` followed by
  ordinary `back.invoke()`. Updates that keep the screen open only send data.
  Receiver observers update their ViewModel or request the next destination through
  normal Directions; they do not call Back to close the sender.
- No parent close-and-resend relay, `Origin` mapping, stack inspection/casts to find
  the receiver, second bus/key hop, or hidden runtime flags such as `selectionSent`.
- Protocol Actions stay nested in the producer's `Router.<Screen>.Action`; one key
  covers all its variants. Keys must avoid collisions among unrelated live receivers.
- ResultEmitter is a no-replay SharedFlow with 64 extra buffer slots; ResultManager
  subscriptions are component-scoped. Hidden callers continue observing; destruction
  cancels subscriptions/pending sends. Results are not durable across process death.
  Conflated navigation is not strict debounce or exactly-once delivery. Verify
  queued/immediate result-driven transitions rather than asserting unproven races.

---

## Data layer (full path from UI to network)

For data-layer access, UI MUST depend on public `:data-features:feature-api` contracts, with only the existing authorization credential exceptions documented above. This restriction does not exclude UI state, foundation, design-system, mapper or analytics API dependencies. UseCase or `*Feature` is injected into the ViewModel via Koin.

```
ViewModel
  → TrainingFeature (interface in :feature-api)
    → TrainingFeatureImpl (in :data-features:trainings, @Single(binds=[TrainingFeature::class]))
      → TrainingRepository (internal interface)
        → TrainingRepositoryImpl (@Single(binds=[TrainingRepository::class]))
          → GrippoApi.<method>(body): Result<DTO>      // HTTP via :data-services:backend
          → TrainingDao.<query>(): Flow<Pack>         // Room via :data-services:database
```

### Repository pattern

Repository owns API-backed cache consistency and returns domain models, never DTOs.
Observations use existing DAO Flows; commands preserve their declared Result or
throwing contract. Local training drafts remain DB-owned and are not sent to the server.
MUST apply the canonical [repository rules](.claude/skills/data-layer/references/repositories.md)
and [operation/error rules](.claude/skills/data-layer/references/error-pipeline.md)
for hydration, partial success, cancellation and range reconciliation. A filtered
mapped collection alone is not proof of complete server data.

### Backend layer

`GrippoApi` — flat class, **one method per endpoint**, sections marked with `/* * * * * Auth service * * * * */` comments:

Use the actual flat API in
`data-services/backend/src/commonMain/kotlin/com/grippo/services/backend/GrippoApi.kt`.
Each endpoint returns its existing `Result<T>`; cancellation propagates rather
than becoming `Result.failure`. At a suspend Result boundary, rethrow
`CancellationException` and preserve ordinary exceptions as the caller's failure.


`BackendClient` (Ktor):
- `defaultRequest`: `host = "grippo-app.com"`, `URLProtocol.HTTPS`, JSON content type, **`Accept-Language = AppLocale.current()`** on every request.
- `HttpTimeout`: 10s (request, connect, socket).
- `Logging` uses `LogLevel.INFO` and custom `ClientLogger`, including auth requests. No auth-specific error sanitation is used. AppLogger currently dispatches in every build mode; its onDebug method has no variant check.
- `Auth` plugin with custom `TokenProvider : AuthProvider`.
- `withContext(Dispatchers.IO)` wraps every `invoke`.

`TokenProvider` — bearer + refresh:
- `addRequestHeaders` always reads the fresh token from `TokenDao` (via `userActiveDao` for the current user) and sets `Bearer <token>`.
- `refreshToken(response)` — refresh with `Mutex` (one refresh at a time), `withTimeout(10s)`. Concurrent 401 requests wait for the result via `waitForOngoingRefresh`.
- Refresh goes through `client.submitForm` with `attributes.put(AuthCircuitBreaker, Unit)` (Ktor will not try to refresh the refresh).
- The shared validator maps refresh HTTP 4xx to `AppError.Network.Expected` before the response returns. After refresh retries fail, tokens are deleted from DB (`tokenDao.delete(userId)`); the existing authorization observer then navigates to login. The local `RefreshUnauthorizedException` branch applies only when an unauthorized response reaches TokenProvider directly.
- `retryWithBackoff(maxAttempts = 3, initialDelay = 500ms, factor = 2.0)` for refresh failures except cancellation, fatal errors and `RefreshUnauthorizedException`; validated HTTP 4xx currently follow the retry policy.

DTOs: `package com.grippo.services.backend.dto.<area>`, file `*Response.kt` or `*Body.kt`, `@Serializable data class` with every field `@SerialName` and **nullable + default `= null`**, including request bodies and collections. Request construction still supplies required business values; this defensive Kotlin shape does not make API-required fields optional.

### Database layer

`@Database(entities = [...], version = 5, exportSchema = true)` in `Database.kt`. `@TypeConverters(StringListConverter::class)` for `List<String>` → `pipe|delimited|string`.

Entities — flat data classes in the owning database entity package. The following is an abbreviated relationship excerpt, not the complete TrainingEntity signature:

```kotlin
@Entity(
    tableName = "training",
    indices = [Index(value = ["profileId"])],
    foreignKeys = [ForeignKey(entity = UserEntity::class, parentColumns = ["profileId"],
        childColumns = ["profileId"], onDelete = ForeignKey.CASCADE)],
)
public data class TrainingEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val duration: Long,
    val createdAt: String,
)
```

DAO — `interface` with `@Dao`, `@Query("...")` (raw SQL), `@Insert(onConflict = REPLACE)`, `@Transaction` for multi-step ops. Returns `Flow<...>` for observe and `suspend` for mutations.

`@Embedded` + `@Relation` models — in `models/*Pack.kt`:

```kotlin
public data class TrainingPack(
    @Embedded val training: TrainingEntity,
    @Relation(parentColumn = "id", entityColumn = "trainingId", entity = ExerciseEntity::class)
    val exercises: List<ExercisePack> = emptyList(),
)
```

Migrations — `Migration<N>To<N+1>` objects in `migrations/`, collected in `DatabaseMigrations.all`. MUST assess shipped-schema compatibility and the canonical [scope policy](#scope-discipline); changes necessary to the authorized task need no repeat approval.

`DatabaseBuilder` (expect/actual):
- Android: `Room.databaseBuilder<Database>(context, name = dbFile.absolutePath).addMigrations(...).fallbackToDestructiveMigration(dropAllTables = true).setQueryCoroutineContext(Dispatchers.IO).build()` + `openHelper.writableDatabase` (warm up).
- iOS: path — `NSDocumentDirectory + "/grippo_database.db"`, `setDriver(BundledSQLiteDriver())`, the same `fallbackToDestructiveMigration(dropAllTables = true)`.

### Mappers

Business-model conversions use 7 directions = 7 modules. Package names =
`<source>.<target>.<area>`. UI→telemetry status/enum mapping is a different boundary:
keep it in the feature UI `analytics/` package, using existing UI validation without
raw values, domain conversions or inline mapping in ViewModel/Screen:

| Module | Package prefix | Purpose |
|---|---|---|
| `:dto-to-entity` | `com.grippo.dto.entity.<area>` | Network DTO → Room Entity for caching. |
| `:entity-to-domain` | `com.grippo.entity.domain.<area>` | Room `*Pack` → domain models in Repository. |
| `:domain-to-state` | `com.grippo.domain.state.<area>` | Domain → UI state. Used in ViewModels before `update {}`. |
| `:state-to-domain` | `com.grippo.state.domain.<area>` | UI state → domain (when a screen builds a form). |
| `:domain-to-dto` | `com.grippo.domain.dto.<area>` | Domain → request body for `POST/PUT`. |
| `:domain-to-entity` | `com.grippo.domain.entity.<area>` | Drafts: domain → Entity, bypassing the backend. |
| `:dto-to-domain` | `com.grippo.dto.domain.<area>` | DTO → Domain without DB caching. |

Function name — top-level extension: `<Source>.toEntity()`, `<Source>.toDomain()`, `<Source>.toState()`, `<Source>.toBody()`. Plural variants — `List<Source>.toEntities()`/`toDomain()`/`toState()`. Nullable variants — `toEntityOrNull(): T?`.

**Required-field guard excerpt** (required Domain/Entity fields are non-null; semantically optional target fields may remain nullable):

```kotlin
public fun ExerciseResponse.toEntityOrNull(): ExerciseEntity? {
    val entityId = AppLogger.Mapping.log(id) { "ExerciseResponse.id is null" }
        ?: return null
    val entityName = AppLogger.Mapping.log(name) { "ExerciseResponse.name is null" }
        ?: return null
    return ExerciseEntity(id = entityId, name = entityName, /* ... */)
}
```

`AppLogger.Mapping.log(value) { msg }` — if value is not null → returns it; otherwise → writes `[MAPPING] <msg> (file:line)` and returns null. Each field is logged separately for diagnostics. Collection dropping MUST follow the [canonical null/identity rules](.claude/skills/mappers/references/null-safety-and-logging.md); the repository determines completeness.

---

## Design system / Resources / Strings

### `AppTokens` (only in @Composable)

Single entry point for design tokens **from Composable**:

```kotlin
@Stable
public object AppTokens {
    public val colors: AppColor
        @Composable @ReadOnlyComposable
        get() = LocalAppColors.current
    public val icons: AppIcon @Composable @ReadOnlyComposable get() = ...
    public val typography: AppTypography @Composable @ReadOnlyComposable get() = ...
    public val strings: AppString @Composable @ReadOnlyComposable get() = ...
    public val drawables: AppDrawable @Composable @ReadOnlyComposable get() = ...
    public val dp: AppDp @Composable @ReadOnlyComposable get() = ...
}
```

Use in Composable:

```kotlin
Text(
    text = AppTokens.strings.res(Res.string.weight_and_height),
    style = AppTokens.typography.h2(),
    color = AppTokens.colors.text.primary,
    modifier = Modifier.padding(horizontal = AppTokens.dp.screen.horizontalPadding),
)
```

`AppTheme(darkTheme, localeTag)` wraps the root and provides all CompositionLocals.

### `StringProvider` (only in non-@Composable, e.g. ViewModel)

When a string is needed outside `@Composable` (for notification body, error message, side effect):

```kotlin
public interface StringProvider {
    public suspend fun get(id: StringResource, vararg args: Any): String
    public suspend fun plural(id: PluralStringResource, quantity: Int, vararg args: Any): String
}
```

Injected into the ViewModel via `getKoin().get()` (in `Component.viewModel = retainedInstance { ... stringProvider = getKoin().get() }`):

```kotlin
val notification = AppNotification(
    id = NotificationKey.ChangeWeight,
    title = stringProvider.get(Res.string.notification_weight_title),
    body = stringProvider.get(Res.string.notification_weight_description),
    deeplink = Deeplink.WeightHistory.key,
)
notificationManager.show(notification, 7.days)
```

### `UiText` — text in State

State classes cannot hold a `String` if the value depends on resources (locale may change). Use `UiText`:

```kotlin
@Stable
public sealed interface UiText {
    @Immutable public data class Res(val value: StringResource, val formatArgs: ImmutableList<Any> = persistentListOf()) : UiText
    @Immutable public data class Str(val value: String) : UiText

    @Composable public fun text(): String = ...                   // for @Composable consumers
    public suspend fun text(stringProvider: StringProvider): String = ...  // for VM-side
}
```

In State use `val title: UiText`; in Screen use `Text(text = state.title.text())`.

### Resources

Compose Multiplatform Resources in `:design-system:resources:provider`:

- Strings — `commonMain/composeResources/values/strings.xml` (en) + `values-uk/`, `values-ru/` for localizations. Access — `Res.string.<key>`.
- Drawables — `commonMain/composeResources/drawable/`. Access — `Res.drawable.<key>`.
- No plural resources or composable AppString plural accessor exist currently. When needed, add localized plural resources and an AppString accessor; outside composition reuse the existing `StringProvider.plural`. Never document an invented API as available.
- All keys — `snake_case`, descriptive (`registration_title`, not `title1`).
- Backend Accept-Language follows AppLocale.current(). The composition root reads the composable system theme/locale properties and installs DateFormatting; localeTag does not currently override Compose resource language. Inspect [RootComponent](shared/src/commonMain/kotlin/com/grippo/shared/root/RootComponent.kt) and [theme wiring](.claude/skills/design-system/references/theme.md) before changing that policy.

### `Format*State` formatters

`:ui-core:state:formatters` contains specialized state classes for form fields:

- `EmailFormatState` — sealed interface with `Empty()`, `Invalid(value)`, `Valid(value)` subtypes + `EmailFormatState.of(rawString)` factory with regex validation.
- `PasswordFormatState` — `Empty()`, `Invalid(value, reason)`, `Valid(value)` + `hint(): String` for the hint.
- `WeightFormatState`, `HeightFormatState`, `DurationFormatState`, `VolumeFormatState` — formats for numeric fields with units.
- `DateFormatState`, `DateRangeFormatState` — for dates.

MUST reuse a suitable existing formatter for validated/formatted input; add a new formatter only for a real unmet need. For an email field, store `EmailFormatState` rather than `String`. UI reads `state.email is EmailFormatState.Valid` to derive button state; ViewModel writes `update { it.copy(email = EmailFormatState.of(rawInput)) }`.

### Preview

MUST use existing AppPreview/PreviewContainer and Contract.Empty. Cover meaningful
states with existing stubs; explicit preview fixture text/IDs are permitted, while
production product text/visual values use resources/tokens. The canonical
[preview rules](.claude/skills/design-system/references/previews.md) own the workflow;
inspect actual [preview helpers](design-system/preview/src/commonMain/kotlin/com/grippo/design.preview/Preview.kt)
before choosing locale/theme variants.

`stub*()` functions — single source of realistic preview data, live next to State classes in `:ui-core:state`.

---

## Toolkit ownership

MUST inspect actual signatures and platform actuals before calling a toolkit API.
[Core ownership](.claude/skills/platform-build-toolkit/references/toolkit-utilities-core.md)
and [platform utilities](.claude/skills/platform-build-toolkit/references/toolkit-utilities-platform.md)
own the detailed rules. Date persistence uses UTC-normalized representation;
logger consumers use AppLogger and do not assume debug-only dispatch; notification,
permission and link APIs retain their current platform contracts.

Useful sources: [DateTimeUtils](toolkit/date-utils/src/commonMain/kotlin/com/grippo/toolkit/date/utils/DateTimeUtils.kt),
[AppLogger](toolkit/logger/src/commonMain/kotlin/com/grippo/toolkit/logger/AppLogger.kt),
[AppLocale](toolkit/localization/src/commonMain/kotlin/com/grippo/toolkit/localization/AppLocale.kt)
and [AppTheme](toolkit/theme/src/commonMain/kotlin/com/grippo/toolkit/theme/AppTheme.kt).
Do not maintain a second catalog of their methods here.

---

## Code style and naming

### Files and naming

- **Public API classes** — `public` explicitly (required by `explicitApi()`).
- **Internal-by-default** — implementations and helpers stay internal/private; real module-boundary contracts, entry Components, state models, widgets and DI modules may be public. Public does not mean implementation details may leak. Implementations use the `Impl` suffix.
- Composable — `PascalCase`. ViewModel callback — `onXxx` (`onApplyClick`, `onWeightPickerClick`, `onBack`).
- Independent new types MUST have separate files. Related sealed variants and existing coherent model families may stay together; do not split or group unrelated types merely to satisfy a template.
- DTO — `@Serializable public data class <Name>Response` or `<Name>Body`, fields `@SerialName("...")`, **all nullable with default `= null`**.
- Domain models — public typed contracts; required fields are non-null, semantically optional fields may remain nullable. Use domain enums/sealed types for closed sets.
- Entity — `public data class <Name>Entity` with `@Entity`, `@PrimaryKey`, `@ColumnInfo(name = "snake_case")` if an explicit override is needed.
- State — `@Immutable internal data class <Feature><Subscreen>State` (or `data object` if stateless, or `sealed interface` with subtypes).
- Direction — `internal sealed interface <Feature>Direction : BaseDirection` with `data object`/`data class` subtypes.
- Loader — `@Immutable internal sealed interface <Feature>Loader : BaseLoader` with `@Immutable data object` subtypes.
- Contract — `@Immutable internal interface <Feature>Contract { ...; companion object Empty : <Feature>Contract { ... } }`. Empty — for preview.
- New use case convention — `public class <Verb><Noun>UseCase(...)` with one `public suspend fun execute(...): Result<T>`. Preserve existing complete APIs, including direct-value throwing auth use cases; analytics must not reshape them.

### Packages

- Base scheme: `com.grippo.<area>.<feature>` (`com.grippo.profile.body`, `com.grippo.authorization.registration`).
- Some existing feature/design directories contain dotted package segments (for example `com/grippo/data.features.trainings/` or `com/grippo/dialog.api/`); other areas, including current mapper/service sources, use slash-separated packages. Inspect actual neighboring files. Preserve existing layouts during feature work; new modules use slash-separated directories. Do not normalize unrelated paths.
- Keep helpers internal/private in the owning package. Service cores follow their established `internal/` package layout; this is not a requirement to move every internal declaration to another package.

### Kotlin

- Keep function/`init`/control-flow bodies and business blocks such as `safeLaunch`
  and `analytics.trackOperation` multiline, even for a single business call.
  Keep short single-expression state updates and simple Flow transformations
  compact: `update { it.copy(value = value) }`, `.map { it.toState() }`,
  `.filter { it.isSelected }`, `.onEach { update { state -> state.copy(items = it) } }`,
  `.flatMapLatest { it }`. Expand long expressions or blocks with several operations.
  Empty defaults such as `onError = {}` can remain inline.
- No named local function declarations inside `init`, constructors, another
  function or a lambda. Declare methods at class scope or functions at file scope.
  Ordinary callbacks, lambdas and DSLs remain allowed. `init` usually starts
  subscriptions/requests directly; a startup operation reused after error-dialog
  dismissal belongs in a private class-level method, called from `init` and
  `onError`, rather than duplicated or declared locally.
- ViewModel coroutine launches — **only** `safeLaunch` / `Flow.safeLaunch`. Toolkit/platform workers use their existing owned scopes. No `viewModelScope.launch`, `runBlocking` or `GlobalScope`.
- Collections in State and Loaders — **only** `kotlinx-collections-immutable` (`ImmutableList`, `ImmutableSet`, `PersistentList`). No `List`/`Set`/`MutableList` in `@Immutable` classes.
- Unwrap a Result-returning call with `.getOrThrow()` inside `safeLaunch` (inside the decorated block when tracked); existing direct-value throwing calls remain direct. Original exceptions bubble to `BaseViewModel.sendError` → `ErrorProvider`.
- Every `@Composable internal fun <X>Screen` takes three arguments in the same order: `state, loaders, contract` (sometimes also `component` for the cross-feature navigator).
- `@AppPreview` + `PreviewContainer` for previews. Preview always uses `<X>Contract.Empty` and stub data (`stub*()` functions from `:ui-core:state`).
- Use `remember` for expensive pure computations or a demonstrated stability/identity need, keyed on all relevant inputs. Simple derived checks may be inline; do not add mechanical caching to every lambda or Modifier.
- VM-side resource lookup uses injected StringProvider (`get` or `plural` as required). Composable consumers use the real AppTokens accessor; a new plural accessor must be implemented before use. See [accessor boundaries](.claude/skills/design-system/references/accessors.md).

### Compose specifics

- Recomposition: state classes — `@Immutable`/`@Stable`, immutable collections. Compose stability metrics enabled in convention plugin.
- Global `optIn`s (Material3, Foundation, Coroutines, ForeignApi, etc.) are already enabled in `KotlinMultiplatformConventionPlugin` — **do not duplicate in source files** via `@OptIn`.
- `LaunchedEffect(Unit)` for one-shot navigation side effects is **forbidden** — navigation goes through `Direction` + `eventListener`. `LaunchedEffect` is allowed only for non-navigational side effects (animation kickoff, system API init).
- Regular `Toolbar` belongs to screens. Dialogs follow the current session host/title layout; do not add a second toolbar to hosted dialog content.
- Lazy items MUST use stable unique keys from actual identity; `.id` is one option, not a required new field. Add `animateItem()` only for the intended insert/remove/reorder transitions. Modifier order follows required layout, clipping and hit area; there is no universal sequence.
- `BottomOverlayContainer` (`:design-system:components`) — standard "list + bottom CTA" pattern, propagates padding into content for correct bottom overscroll.

---

## Dependency injection (Koin)

Each module with injectable code:

```kotlin
@Module(includes = [BackendModule::class, DatabaseModule::class])
@ComponentScan
public class TrainingsFeatureModule
```

- `@Module(includes = [...])` — sets transitive includes.
- `@ComponentScan` — KSP scans the package for `@Single`/`@Factory`/`@Scoped` in implementations.
- Injectable implementations MUST use annotations and explicit interface bindings. Choose `@Single` / `@Factory` from actual ownership and lifetime; [DI annotation rules](.claude/skills/di-modules/references/annotations.md) own the binding policy.
- `@Factory` for cheap, per-call instances (e.g. `OperationManager`).
- `@InjectedParam` — for parameters passed at `get { parametersOf(...) }` time. Used by `BaseViewModel` to thread `coroutineScope` into `OperationManager` and `ResultManager`.

Hand-writing `module { single { ... } }` DSL for new modules is **forbidden**. Use only annotations.

All Koin modules are explicitly enumerated in
`shared/src/commonMain/kotlin/com/grippo/shared/Koin.kt`. Its `Koin.init` returns
`ApplicationBootstrap`, exposing typed analytics/crash-reporting/messaging startup
contracts. Reuse the real implementation; do not copy a former KoinApplication
startup recipe. A new module is registered manually within its authorized scope.
The existing `FeatureApiModule` UseCase aggregator is the sole hand-written DSL
exception; authorized UseCase additions may extend it, never create a second DSL module.

---

## Implementation recipes

Read the owning project skill and only the references relevant to the change:

| Change | Owning skill |
|---|---|
| Screen, dialog, navigation, UI state | [ui-feature](.claude/skills/ui-feature/SKILL.md) |
| Business operation, endpoint, persistence | [data-layer](.claude/skills/data-layer/SKILL.md) |
| DTO/Entity/Domain/State conversion | [mappers](.claude/skills/mappers/SKILL.md) |
| Koin bindings and composition | [di-modules](.claude/skills/di-modules/SKILL.md) |
| Tokens, widgets, resources | [design-system](.claude/skills/design-system/SKILL.md) |
| Platforms, modules, build configuration | [platform-build-toolkit](.claude/skills/platform-build-toolkit/SKILL.md) |

Feature work also applies analytics and preservation gates. Recipes do not grant
new scope or require redundant confirmation of work already authorized.

---

## Performance budgets and priorities

No explicit budgets in code. Default priorities:

- Keep heavy work off the main thread at its existing owner: IO on `Dispatchers.IO`, CPU-heavy work on `Dispatchers.Default`. Do not move simple state updates/navigation or already-dispatched service calls merely to satisfy a dispatcher template.
- Recomposition stability: state — `@Immutable`/`@Stable`, immutable collections everywhere, Compose metrics in `build/compose-metrics`.
- Public API changes in shared/foundation/design/data-feature contracts require assessing both platforms. Run `./gradlew :shared:assembleSharedDebugXCFramework` when authorized and available; record an unrun check and the remaining integration risk otherwise.
- The explicit `gc=cms` override is disabled in `gradle.properties`; do not describe it as enabled.
- `kotlin.native.binary.smallBinary=true` for smaller iOS frameworks.
- `org.gradle.workers.max=1` to reduce peak memory during native release linking.

---

## Verification policy

The project intentionally has no automated test sources or test infrastructure.
Do not add test folders, dependencies, runners, fixtures or screenshot automation
unless the user explicitly requests them. Global skill advice cannot override this
project policy.

MUST review actual source/diff against requirements and the owning skill's gates.
MUST run relevant permitted static/manual checks. Builds needed to verify the
requested task are authorized within that scope without a separate question;
honor explicit user restrictions and do not change unrelated infrastructure to
make a build pass. Documentation-only work does not require an application build.
A green build proves compilation, not runtime behavior. MUST report actual results,
unavailable checks and unverified runtime/platform scenarios. MUST NOT add
verification-only production hooks, flags, wrappers or dependencies.

## Locked architectural decisions

- Compose Multiplatform for UI on both platforms.
- Decompose 3.5 for navigation and lifecycle (not Compose Navigation, not Voyager).
- Koin 4.2 + annotations + KSP for DI (not Hilt, not Anvil, not Metro).
- Room 2.8 multiplatform for persistence (not SQLDelight).
- Ktor 3.6 for network.
- `kotlinx-serialization` for JSON and Decompose state.
- Pattern Component / Contract / State / Direction / Loader / ViewModel / Screen — mandatory for any new screen/dialog.
- `BaseViewModel` uses CONFLATED `Channel` for navigation — known trade-off with rapid events.
- `fallbackToDestructiveMigration(dropAllTables = true)` on both platforms — intentional.
- Convention plugins in `build-logic/` — the only way to configure modules.
- All Koin modules collected in `:shared/Koin.kt` explicitly.
- 7 mapper directions in 7 modules `:data-mappers:*`. Inline DTO↔Domain↔State conversion in ViewModel is forbidden.
- Response and request DTO fields, including collections, are nullable + default `= null`. Required inbound values use `AppLogger.Mapping.log(...) ?: return null`; required outbound values remain supplied by business code.
- API contract is owned by backend; codegen is NOT used — DTOs in `data-services/backend/dto` are written by hand.
- Resources (`Res.string`, `Res.drawable`) — Compose Multiplatform Resources, **not** `androidx.compose.ui.res`, not Moko Resources.

---

## Scope discipline

MUST preserve unrelated working-tree changes. Existing explicit or implied task
authorization covers necessary callers, routing, DI/module registration and coherent
migrations. MUST NOT ask for repeat approval of those steps. MUST ask when a change
expands scope, revises a locked architectural decision, or needs an unresolved
product/backend fact that affects correctness. Silence is not acceptance.

Before changing shared foundation, public routes, modules, Room schemas/migrations,
dependencies/conventions, app shells, network/auth core, tokens, CompositionLocals
or DialogConfig, assess that boundary against the authorized outcome. Necessary
changes are permitted; unrelated cleanup and silent library upgrades are not.
DialogConfig content changes MUST update its typed `hasSameContentAs` comparison.
MUST NOT edit `secure/` or `local.properties` as incidental cleanup. Global opt-ins
belong in conventions; new CompositionLocals belong in the owning design modules.

## When to stop and ask

Use a short self-contained quiz with 2–3 alternatives and an evidence-supported
recommendation. Honor a request for extra quizzes, but do not manufacture uncertainty
or repeat answered questions. Continue independent work while an answer is pending;
MUST NOT implement dependent behavior until a required answer arrives. External
messages require explicit user authorization. See [scope questions](.claude/skills/validation-gates/references/when-to-stop-and-ask.md).

## Anti-patterns (refuse to write)

MUST apply the canonical [forbidden patterns](.claude/skills/validation-gates/references/forbidden-patterns.md)
and the affected owning skill's semantic gate. This overview does not maintain a
second list of prohibitions. Existing source deviations are not permission to copy
them; fix affected defects within scope and report unrelated ones without silently
expanding the task.

---

## Local skill workflow

Project skills live in `.claude/skills/`. [task-prep](.claude/skills/task-prep/SKILL.md)
clarifies scope and dependencies; [task-orchestrator](.claude/skills/task-orchestrator/SKILL.md)
coordinates implementation and independent review; [validation-gates](.claude/skills/validation-gates/SKILL.md)
defines completion checks. Work directly with repository source and the user's
request. No task-board runtime, generated machine reports or bootstrap scaffold
is required.

MUST apply the canonical [execution and review loop](.claude/skills/task-orchestrator/SKILL.md#execution-loop)
and [completion checks](.claude/skills/validation-gates/SKILL.md). The execution skill
owns independent-review eligibility, unavailable-reviewer handling and recurring
findings; do not maintain a second review policy here. Reports distinguish source
review, successful builds and unverified runtime behavior.

### Backend contract rules

Backend owns wire behavior. MUST apply [backend-contract-client](.claude/skills/backend-contract-client/SKILL.md)
before changes affecting endpoints, keys, serialization, payload meaning or required
request values. It owns the evidence gate and bounded exception for Kotlin-only
nullability alignment. DTO properties and collections remain nullable with `= null`;
collection element types follow the verified backend contract. This Kotlin standard
does not relax server requirements.
