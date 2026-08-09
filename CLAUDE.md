# grippo-mobile

## Project purpose

Grippo KMP client — Android + iOS from a single codebase. UI in Compose
Multiplatform, navigation via Decompose. Primary consumer of the backend
API.

---

## Stack

- Kotlin **2.3.21**, explicit API mode (enabled in `KotlinMultiplatformConventionPlugin`).
- Compose Multiplatform **1.10.3** + Kotlin Compose plugin. Metrics and stability config enabled via convention plugin.
- Targets: `commonMain`, `androidMain`, `iosMain` (`iosArm64`, `iosSimulatorArm64`, `iosX64`). Android `compileSdk = 36`, `minSdk = 26`. JVM toolchain 19.
- Decompose **3.5** + Essenty (`back-handler`, `state-keeper`, `lifecycle`).
- Koin **4.2** + `koin-annotations` **2.3.1** + KSP. KOIN_CONFIG_CHECK is disabled (TODO until next Koin Annotations release).
- Ktor **3.4** (Android engine + Darwin engine), `kotlinx-serialization-json` **1.11**.
- Room **2.8** multiplatform, `androidx.sqlite-bundled`, `BundledSQLiteDriver` on iOS, migrations 2→3, 3→4, 4→5 in `:data-services:database`.
- AndroidX DataStore **1.2** (preferences-core).
- Coil **3** (`coil-compose`, `coil-network-ktor3`).
- Firebase: Analytics, Crashlytics, Messaging — Android shell only.
- Auth: Google (`androidx-credentials` + Google Identity), Apple (custom).
- Build: Gradle with convention plugins in `build-logic/convention/`, version catalog in `gradle/libs.versions.toml`. Type-safe project accessors enabled.
- iOS deliverable: static XCFramework `shared.xcframework` via `ios.swiftpackage.convention` (Decompose api re-exported for Swift, `-lsqlite3` linker flag).

---

## Module layout

`settings.gradle.kts` is the single source of truth for the module list.
Groups:

### App shells (thin, no business logic)

- `:androidApp` — `Application` + `MainActivity` + DI startup + Firebase setup. Decomposes root via `retainedComponent { RootComponent(...) }`. Edge-to-edge, splash screen, deeplink from intent (`onCreate` for cold start, `onNewIntent` for warm).
- `:iosApp` — Xcode project. Links `shared.xcframework` and starts `RootViewController` from `iosMain`.

### `:shared` — composition root

- `Koin.kt` — `Koin.init { ... }` starts all Koin modules **explicitly by enumeration**. Any new module is added here manually.
- `RootComponent` / `RootViewModel` / `RootScreen` / `RootContract` / `RootDirection` / `RootLoader` / `RootState` — primary stack navigator + token observer for auto-logout (`authorizationFeature.getToken()` → `onEach { if (it == null) navigateTo(Login) }`).
- `DialogComponent` — separate slot navigator for bottom sheets layered above the stack.

### Design system

- `:design-system:core` — `AppTokens` (facade over CompositionLocals).
- `:design-system:components` — atomic Composables (`Button`, `Toolbar`, `Input*`, `WeightHistoryChart`, ...).
- `:design-system:resources:provider` — `Res.string.*` (Compose Multiplatform Resources), `interface StringProvider`, `interface AppColor`/`AppDp`/`AppTypography`/`AppString`/`AppDrawable`/`AppIcon`.
- `:design-system:resources:provider-impl` — `ResourcesProviderModule` + `StringProviderImpl`.
- `:design-system:preview` — `@AppPreview` (multi-preview annotation: phone EN big + UK small), `PreviewContainer` (CompositionLocalProvider + Coil preview handler + `AppTheme(darkTheme = true)`).

### UI core

- `:ui-core:foundation` — `BaseViewModel`, `BaseComponent`, `BaseScreen`, `OperationManager`, `ResultManager`/`ResultEmitter` (cross-component results), `BaseDirection`/`BaseLoader`/`BaseRouter`/`BaseResult`/`ComponentIdentifier`, `collectAsStateMultiplatform` (expect/actual).
- `:ui-core:state` — reusable UI data classes (`MuscleLoadSummaryState`, `DigestState`, `TrainingStreakState`, formatters: `WeightFormatState`, `EmailFormatState`, `PasswordFormatState`, `DateFormatState`, `UiText`, etc.). Each class is `@Immutable`; many have a `stub*()` function.
- `:ui-core:error:error-provider` (`ErrorProvider` interface, `AppError` sealed) and `:ui-core:error:error-provider-impl` (`ErrorProviderImpl` maps `AppError` → `AppErrorState` → shows `DialogConfig.ErrorDisplay`).

### `:ui-screen-features:*` — full-screen features

- `:screen-api` — public `*Router` sealed classes (`@Serializable`) and `Deeplink` enum. Used by Decompose for type-safe navigation **between features**.
- Features: `authorization`, `home`, `profile`, `training`, `trainings`, `debug`. Inside a feature — a root `*RootComponent` with its own `StackNavigation<*Router>` and **internal** sub-Components for each screen.

### `:ui-dialog-features:*` — bottom sheet flows (~25 modules)

- `:dialog-api` — `DialogConfig` sealed, `DialogController.show(config)`, `DialogProvider`, `DialogModule`.
- Each dialog feature is a standalone `Component`/`ViewModel`/`Screen` package, looking **identical to a screen feature** except the host is `DialogComponent` instead of `RootComponent`.

### `:data-services:*` — low-level services and DTOs

- `:backend` — `GrippoApi` (flat class with methods like `suspend fun <action>(body): Result<T>`), `BackendClient` (Ktor with Auth, Logging, ContentNegotiation, defaultRequest pointed at `https://grippo-app.com`), `TokenProvider` (Auth `Bearer` + refresh with mutex and retry-with-backoff), `ClientLogger`, `dto/<area>/*` — `@Serializable data class`, all fields nullable with `@SerialName`.
- `:database` — Room `@Database(version = 5)`, all `@Entity`, DAOs, migrations, `models/*Pack` (`@Embedded` + `@Relation`), `converters/StringListConverter`, `DatabaseBuilder` (expect/actual), **`fallbackToDestructiveMigration(dropAllTables = true)` on both platforms — intentional**.
- `:datastore` — wrapper over AndroidX DataStore.
- `:google-auth` / `:apple-auth` — platform-specific wrappers for ID token.
- `:firebase` — `interface FirebaseProvider` + `Firebase{Analytics,Crashlytics,Messaging}` with Android implementations (via `FirebaseProvider.setup(...)` in `App.onCreate`); iOS empty stub.

### `:data-features:*` — domain layer

- `:feature-api` — `*Feature` interfaces + `*UseCase` classes + domain models. **Only this** is visible to UI modules.
- Implementations: `:trainings`, `:user`, `:weight-history`, `:goal`, `:muscle`, `:equipment`, `:exercise-examples`, `:exercise-metrics`, `:excluded-muscles`, `:excluded-equipments`, `:authorization`, `:local-settings`. Each has its own Koin `*FeatureModule(includes = [BackendModule, DatabaseModule])`. Inside: `data/<X>RepositoryImpl` + `domain/<X>Repository` + `domain/<X>FeatureImpl`. Repository — `@Single(binds = [<X>Repository::class])`, FeatureImpl — `@Single(binds = [<X>Feature::class])`. Repository **internal**, Feature **public**.

### `:data-mappers:*` — directional mappers

7 modules, one per direction. No cross-direction dependencies. See **Mappers** section below.

### `:toolkit:*` — platform-aware utilities

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

- **A UI module (`:ui-screen-features:*`, `:ui-dialog-features:*`) does NOT depend** on `:data-services:*` directly — only via `:data-features:feature-api`.
- **`:data-features:feature-api`** does not depend on `:data-services:*`. It is pure contracts + domain models.
- **`:data-mappers:*`** depend on adjacent layers (`dto-to-entity` → `:data-services:backend` + `:data-services:database`), but **never on UI**.
- **`:toolkit:*`** depends on nothing except other `:toolkit:*` and `:data-services:firebase` (for crash logging).
- **`:design-system:*`** does not depend on the data layer. `AppTokens` are pure UI primitives.
- `:shared` is the only module that imports "everything". `:androidApp`/`:iosApp` reach `RootComponent` and `Koin.init` only through `:shared`.

---

## Convention plugin matrix

A module-level `build.gradle.kts` contains **only** `plugins { id("...convention") }` and `kotlin { sourceSets.commonMain.dependencies { ... } }`. All settings live in convention plugins.

| Module type | Plugins to apply |
|---|---|
| `:androidApp` | `android.application.convention` + `compose.compiler` + `jetbrains.compose` + `google-services` + `firebase-crashlytics` |
| Pure KMP module (no UI) | `android.library.convention` + `kotlin.multiplatform.convention` |
| KMP module with Compose UI | `android.library.convention` + `kotlin.multiplatform.convention` + `compose.multiplatform.convention` |
| Module with Koin DI | + `koin.annotation.convention` |
| `:data-services:database` | + `room.convention` (KSP + Room compiler for all iOS targets) |
| `:shared` | all four: `android.library.convention` + `kotlin.multiplatform.convention` + `ios.swiftpackage.convention` + `compose.multiplatform.convention` + `koin.annotation.convention` |

What each does:

- `kotlin.multiplatform.convention` — applies `kotlin("multiplatform")`, **`explicitApi()`**, registers iOS targets (`iosX64`, `iosArm64`, `iosSimulatorArm64`), `applyDefaultHierarchyTemplate`, **global `optIn(...)`** (Material3 ExperimentalApi, Foundation, Coroutines, ForeignApi, Decompose Delicate/Experimental, kotlin.time, kotlin.uuid, etc.).
- `android.library.convention` — applies `com.android.kotlin.multiplatform.library`, KMP Android target with `compileSdk = 36`, `minSdk = 26`, `namespace = "com.grippo"`, JVM toolchain 19.
- `android.application.convention` — Android `applicationId/SDK`, `compileSdk = 36`, `minSdk = 26`, `targetSdk = 36`, JVM 19.
- `compose.multiplatform.convention` — Compose plugins, metrics/reports in `build/compose-{metrics,reports}`, `stabilityConfigurationFiles += compose-stability.conf` (file is currently missing — convention plugin silently ignores it; fix as a separate task).
- `koin.annotation.convention` — KSP, Koin Core + Annotations, `kspCommonMainMetadata`, `KOIN_CONFIG_CHECK = false` (TODO until next Koin release).
- `room.convention` — KSP + Room compiler for Android and all iOS targets, `room.schemaLocation = $projectDir/schemas`.
- `ios.swiftpackage.convention` — assembles `XCFramework("shared")`, **static** framework, exports Decompose API + `:data-services:firebase`, `linkerOpts: -lsqlite3`.

Additional global settings (`gradle.properties`):

- `kotlin.native.binary.gc=cms` — concurrent mark+sweep GC, reduces stop-the-world pauses on iOS.
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
| `protected fun safeLaunch(dispatcher = Dispatchers.Main.immediate, processing = Processing.Infinity, loader = null, onError = {}, block)` | The only way to start a coroutine. Defaults to `Infinity`. |
| `protected fun <T> Flow<T>.safeLaunch(dispatcher = Dispatchers.Main.immediate, processing = Processing.WhileActive, onError = {})` | Stream subscription. Defaults to `WhileActive` — stream sleeps 1s after the screen leaves RESUMED. |
| `protected enum class Processing { WhileActive, Infinity }` | `WhileActive` suppresses upstream when the screen is not RESUMED. |
| `attachActivation(flow)` / `detachActivation()` | Managed automatically by `BaseComponent` via lifecycle. |

When to use which dispatcher:

- `Dispatchers.Main.immediate` (default) — state updates, dialog show, navigation.
- `Dispatchers.IO` — explicit heavy IO **inside** the ViewModel (rare; usually IO is already in `Repository`/`BackendClient.invoke`).
- `Dispatchers.Default` — CPU-heavy work (sorting, mapping large collections).

Error pipeline (single path, from any `safeLaunch`):

```
throw inside safeLaunch
    ↓
operationManager catches via CoroutineExceptionHandler
    ↓
BaseViewModel.sendError(exception, onError)
    ↓
AppLogger.General.error(...)            // file log
FirebaseProvider.recordException(...)   // crashlytics
ErrorProvider.provide(exception, onError)
    ↓
ErrorProviderImpl maps AppError → AppErrorState
    ↓
DialogController.show(DialogConfig.ErrorDisplay(state, onClose = onError))
```

Catching exceptions manually inside a ViewModel is **forbidden** (exception: domain logic such as `result.onSuccess { ... }` after `api.call()`).

### `BaseComponent<DIRECTION : BaseDirection>`

- Delegates `ComponentContext`, is a `KoinComponent`. Created in the parent component via `<X>Component(componentContext = ..., closeCallback = ..., navCallback = ...)`.
- `protected abstract val viewModel: BaseViewModel<*, DIRECTION, *>`. Created via `componentContext.retainedInstance { <X>ViewModel(getKoin().get(), ...) }` — **dependencies are pulled via `getKoin().get()`**, not via the Component constructor.
- On `lifecycle.doOnCreate` — subscribes to `viewModel.navigator` and calls `eventListener(direction)`; calls `viewModel.attachActivation(lifecycle.asActiveFlow())`.
- On `lifecycle.doOnDestroy` — `detachActivation`, `resultManager.clear`, cancels coroutineScope.
- `protected abstract suspend fun eventListener(direction: DIRECTION)` — navigation handler; usually `when (direction) { ... }`. `Back/Close` map to constructor lambdas (`back()`/`close()`), nested routes — to `navigation.push(...)` of the root Component.
- `protected fun observeResult<T>(key: ResultKey<T>, onResult)` / `sendResult(key, data)` — cross-component communication on top of the Decompose stack (without threading callbacks down).
- `@Composable abstract fun Render()` — usually: `val state = viewModel.state.collectAsStateMultiplatform()`, `val loaders = viewModel.loaders.collectAsStateMultiplatform()`, `<X>Screen(state.value, loaders.value, viewModel)`.
- `BackCallback(onBack = viewModel::onBack)` is registered in `init { backHandler.register(...) }` if the screen handles back itself.

### `BaseScreen.kt` — UI root

`BaseComposeScreen(background, content)` — a `Column` with the background from `AppTokens.colors.background.screen`, `clickable` without indication for clear-focus on background tap. **All top-level screens** are wrapped in this.

### Inter-feature navigation: Routers

In `:ui-screen-features:screen-api`:

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

Decompose `childStack(serializer = RootRouter.serializer(), initialConfiguration = ..., key = "RootComponent", childFactory = ::createChild)`. `StateKeeper` serializes the router into `Bundle`/iOS state — therefore **all Routers and payloads inside them (`StageState`, etc.) must be `@Serializable`**.

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

- `DialogController.show(config: DialogConfig)` — emit a config from any ViewModel (the controller is injected into VM via Koin).
- `DialogConfig` — `@Serializable sealed class`; subtype implements `override val key: String` via `buildKey(...)` (length-prefixed parts: `${value.length}:${value}|...`).
- `onDismiss: (() -> Unit)?` — `@Transient`, not serialized.
- `dismissBySwipe: Boolean = true` — controls `ModalBottomSheetProperties.shouldDismissOnBackPress` and swipe-to-dismiss.
- `DialogComponent` is hosted in `:shared`, uses `SlotNavigation<DialogConfig>` + `childSlot(serializer = DialogConfig.serializer())`. The Dialog VM maintains an **internal stack** in state for in-sheet navigation (push/pop without closing the sheet).
- Pickers return results via a **callback in the config**: `DialogConfig.WeightPicker(initial = ..., onResult = { value -> update { it.copy(weight = WeightFormatState.of(value)) } })`. The callback is also `@Transient`.

### `ResultManager` vs callback in DialogConfig

- **Callback in DialogConfig** (`onResult = { value -> ... }`) — default path for pickers. Each picker has its own typed `onResult` parameter. Simple, explicit, local.
- **`ResultManager` via `ResultKey`** — for cases where a callback cannot be threaded:
  - Between two screens in different feature modules.
  - When the initiator and consumer are split by lifecycle (one dialog launched another, the answer needs to reach the initiator).

  Usage: `Component A` calls `sendResult(MyKey, data)`, `Component B` subscribes via `observeResult(MyKey) { data -> ... }`. `ResultEmitter` — singleton `Channel<Pair<String, BaseResult>>(BUFFERED)`; `ResultManager` is per-component with `Job` subscriptions.

  **Action types live inside the child's `Router.<Screen>` config.** When a screen emits results to a parent, declare them as a nested `public sealed interface Action` (or similar) on the corresponding `*Router.<Screen>` data class in `:screen-api`. Sender and observer reference the same fully-qualified path, e.g. `TrainingRouter.Exercise.Action.Sync(exercise)` / `Action.Remove(id)`. This keeps the screen's full contract (input + output) in one place and avoids leaking the result protocol into shared internal packages.

  **One `observeResult` call per Component per domain.** `ResultEmitter`'s `Channel` is single-consumer — multiple subscriptions inside one Component compete via FIFO and a non-matching filter silently drops the item. Use a single subscription with `when` dispatch on the sealed subtypes:

  ```kotlin
  observeResult<Result<TrainingRouter.Exercise.Action>>(
      key = ResultKeys.create("exercise"),
      onResult = {
          when (val action = it.data) {
              is TrainingRouter.Exercise.Action.Sync -> viewModel.updateExercise(action.exercise)
              is TrainingRouter.Exercise.Action.Remove -> viewModel.removeExercise(action.id)
          }
      }
  )
  ```

---

## Data layer (full path from UI to network)

A UI module depends **only** on `:data-features:feature-api` and `:ui-core:state` (plus its own design-system pieces). UseCase or `*Feature` is injected into the ViewModel via Koin.

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

`TrainingRepositoryImpl` (sample):

```kotlin
@Single(binds = [TrainingRepository::class])
internal class TrainingRepositoryImpl(
    private val api: GrippoApi,
    private val trainingDao: TrainingDao,
    private val draftTrainingDao: DraftTrainingDao,
) : TrainingRepository {

    override fun observeTrainings(start, end): Flow<List<Training>> =
        trainingDao.get(from = DateTimeUtils.toUtcIso(start), to = DateTimeUtils.toUtcIso(end))
            .map { it.toDomain() }   // Pack -> Domain via :entity-to-domain

    override suspend fun getTrainings(start, end): Result<Unit> {
        val response = api.getTrainings(startUtc, endUtc)
        response.onSuccess { r ->
            val actualIds = r.mapNotNull { provideTraining(it) }
            // Reconcile cache: delete stale rows in range, keep what server returned
            if (actualIds.isEmpty()) trainingDao.deleteByCreatedAtRange(startUtc, endUtc)
            else trainingDao.deleteByCreatedAtRangeExceptIds(startUtc, endUtc, actualIds)
        }
        return response.map {}
    }
}
```

Standard patterns:
- **Observe** returns `Flow<Domain>` from DAO. Never from API.
- **Get/Set/Update/Delete** returns `Result<T>`, hits the API, on success updates the DAO. UI writes `withLoader { feature.getTrainings(...).getOrThrow() }`.
- **Range reconciliation**: after `getTrainings`, delete everything in range except what the server returned (`deleteByCreatedAtRangeExceptIds`). Removes "deleted on another device" drift.
- **Drafts** live only in DB (`draftTrainingDao`), never go to the server.

### Backend layer

`GrippoApi` — flat class, **one method per endpoint**, sections marked with `/* * * * * Auth service * * * * */` comments:

```kotlin
@Single
public class GrippoApi internal constructor(private val client: BackendClient) {

    public suspend fun getTrainings(start: String, end: String): Result<List<TrainingResponse>> =
        request(method = HttpMethod.Get, path = "/trainings",
            queryParams = mapOf("start" to start, "end" to end))

    private suspend inline fun <reified T> request(
        method: HttpMethod, path: String,
        body: Any? = null, queryParams: Map<String, String>? = null,
    ): Result<T> = runCatching {
        client.invoke(method, path, body, queryParams).body()
    }
}
```

`BackendClient` (Ktor):
- `defaultRequest`: `host = "grippo-app.com"`, `URLProtocol.HTTPS`, JSON content type, **`Accept-Language = AppLocale.current()`** on every request.
- `HttpTimeout`: 10s (request, connect, socket).
- `Logging` plugin (`LogLevel.ALL`, custom `ClientLogger`).
- `Auth` plugin with custom `TokenProvider : AuthProvider`.
- `withContext(Dispatchers.IO)` wraps every `invoke`.

`TokenProvider` — bearer + refresh:
- `addRequestHeaders` always reads the fresh token from `TokenDao` (via `userActiveDao` for the current user) and sets `Bearer <token>`.
- `refreshToken(response)` — refresh with `Mutex` (one refresh at a time), `withTimeout(10s)`. Concurrent 401 requests wait for the result via `waitForOngoingRefresh`.
- Refresh goes through `client.submitForm` with `attributes.put(AuthCircuitBreaker, Unit)` (Ktor will not try to refresh the refresh).
- If backend returns 401 on refresh → `RefreshUnauthorizedException` → tokens are deleted from DB (`tokenDao.delete(userId)`) → `RootViewModel.authorizationFeature.getToken()` will see `null` and call `navigateTo(RootDirection.Login)`.
- `retryWithBackoff(maxAttempts = 3, initialDelay = 500ms, factor = 2.0)` for transient errors (but not for `RefreshUnauthorizedException`).

DTOs: `package com.grippo.services.backend.dto.<area>`, file `*Response.kt` or `*Body.kt`, `@Serializable data class` with all fields `@SerialName` and **nullable + default `= null`** (defense against partial responses).

### Database layer

`@Database(entities = [...], version = 5, exportSchema = true)` in `Database.kt`. `@TypeConverters(StringListConverter::class)` for `List<String>` → `pipe|delimited|string`.

Entities — flat `data class` in `package com.grippo.services.database.entity.*`:

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

Migrations — `Migration<N>To<N+1>` objects in `migrations/`, collected in `DatabaseMigrations.all`. **Do not touch without a request** — already shipped in production.

`DatabaseBuilder` (expect/actual):
- Android: `Room.databaseBuilder<Database>(context, name = dbFile.absolutePath).addMigrations(...).fallbackToDestructiveMigration(dropAllTables = true).setQueryCoroutineContext(Dispatchers.IO).build()` + `openHelper.writableDatabase` (warm up).
- iOS: path — `NSDocumentDirectory + "/grippo_database.db"`, `setDriver(BundledSQLiteDriver())`, the same `fallbackToDestructiveMigration(dropAllTables = true)`.

### Mappers

7 directions = 7 modules. Package names = `<source>.<target>.<area>`:

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

**Canonical mapping with null-friendly DTO** (DTO fields nullable for safety, but domain/entity — non-null):

```kotlin
public fun ExerciseResponse.toEntityOrNull(): ExerciseEntity? {
    val entityId = AppLogger.Mapping.log(id) { "ExerciseResponse.id is null" }
        ?: return null
    val entityName = AppLogger.Mapping.log(name) { "ExerciseResponse.name is null" }
        ?: return null
    return ExerciseEntity(id = entityId, name = entityName, /* ... */)
}

public fun List<ExerciseResponse>.toEntities(): List<ExerciseEntity> =
    mapNotNull { it.toEntityOrNull() }
```

`AppLogger.Mapping.log(value) { msg }` — if value is not null → returns it; otherwise → writes `[MAPPING] <msg> (file:line)` and returns null. Each field is logged separately for diagnostics.

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
- Plurals — `commonMain/composeResources/values/plurals.xml`. Access — `Res.plurals.<key>`.
- All keys — `snake_case`, descriptive (`registration_credential_title`, not `title1`).
- Changing Accept-Language on backend ⇒ changing `AppLocale.current` locally (`AppLocale.current` and `AppTheme.current` — expect/actual `@Composable` functions; in `RootScreen` they switch `AppTheme(darkTheme, localeTag)` and trigger `DateFormatting.install(systemLocaleTag)`).

### `Format*State` formatters

`:ui-core:state:formatters` contains specialized state classes for form fields:

- `EmailFormatState` — sealed interface with `Empty()`, `Invalid(value)`, `Valid(value)` subtypes + `EmailFormatState.of(rawString)` factory with regex validation.
- `PasswordFormatState` — `Empty()`, `Invalid(value, reason)`, `Valid(value)` + `hint(): String` for the hint.
- `WeightFormatState`, `HeightFormatState`, `DurationFormatState`, `VolumeFormatState` — formats for numeric fields with units.
- `DateFormatState`, `DateRangeFormatState` — for dates.

Pattern: instead of storing `String` or `Float?` in State, store `EmailFormatState`. UI reads `state.email is EmailFormatState.Valid` to derive button state; ViewModel writes `update { it.copy(email = EmailFormatState.of(rawInput)) }`.

### Preview

```kotlin
@AppPreview                                  // multi-preview: phone EN big + UK small
@Composable
private fun ScreenPreview() {
    PreviewContainer {                       // wraps in AppTheme + Coil preview handler
        ProfileBodyScreen(
            state = ProfileBodyState(
                weight = WeightFormatState.of(33f),
                height = HeightFormatState.of(90),
                history = stubWeightHistoryList(),  // stub from :ui-core:state
                user = stubUser(),
            ),
            loaders = persistentSetOf(),
            contract = ProfileBodyContract.Empty,    // companion object Empty
        )
    }
}
```

`stub*()` functions — single source of realistic preview data, live next to State classes in `:ui-core:state`.

---

## Toolkit deep dive

### `:date-utils`

- `DateTimeUtils.now() / .startOfDay(date) / .endOfDay(date) / .toUtcIso(localDateTime) / .toLocalDateTime(iso) / .shift(range, period) / .trailingYear() / .getDaysInMonth(year, month) / .weekDayShortLabels()`.
- `DateRange(from: LocalDateTime, to: LocalDateTime)` + `.coerceWithin(limitations)`.
- `DateRangeKind` — Last7Days, Last30Days, ThisMonth, AllTime, ...
- `DateRangePresets.daily() / .monthly() / .infinity()`.
- `DateFormat.DateOnly.{DateMmmDdYyyy, MmmYyyy, DateDdMmm}` / `DateFormat.DateTime.*`.
- `DateFormatting.install(localeTag)` — sets the locale for all formatters. Called from `RootComponent.Render` via `LaunchedEffect(systemLocaleTag) { DateFormatting.install(systemLocaleTag) }`.

### `:connectivity`

`Connectivity.statusUpdates: SharedFlow<Connectivity.Status>` (replay = 1, DROP_OLDEST), auto-start via `ConnectivityOptions(autoStart = true)`. In `RootViewModel`:

```kotlin
init {
    connectivity.statusUpdates
        .onEach(::provideConnectionStatus)
        .safeLaunch()
}
```

### `:notification-manager` / `:permission-manager` / `:link-opener`

- `NotificationManager.show(AppNotification(id, title, body, deeplink), ttl: Duration)` — local notification (Android — WorkManager/AlarmManager wrapper, iOS — `UNUserNotificationCenter`).
- `NotificationKey` — enum for deduplication (one notification per key).
- `PermissionManager.request(Permission)` / `.isGranted(Permission)`.
- `LinkOpener.open(url: String)` — opens in the system browser / Safari.

### `:logger` `AppLogger`

- `AppLogger.General.error(msg)` / `.warning(msg)` — general-purpose.
- `AppLogger.Network.log(msg)` — for `BackendClient`/`TokenProvider`.
- `AppLogger.Navigation.log(msg)` — for Decompose navigation (if we choose to log).
- `AppLogger.Mapping.log(value) { msg }: T?` — null-tracker for mappers.
- File sink — `~/grippo/logs/app.log` (Android) or `NSTemporaryDirectory()/grippo/logs/` (iOS). Access via `AppLogger.logFileContentsByCategory()`.

### `:theme` / `:localization`

- `AppTheme.current: Boolean` — system dark/light theme (expect/actual `@Composable`).
- `AppLocale.current(): String` — current BCP-47 language tag, used in `BackendClient.defaultRequest` for the `Accept-Language` header.

---

## Code style and naming

### Files and naming

- **Public API classes** — `public` explicitly (required by `explicitApi()`).
- **Internal-by-default** — anything not part of feature-api is marked `internal`. Implementations of interfaces — `Impl` suffix.
- Composable — `PascalCase`. ViewModel callback — `onXxx` (`onApplyClick`, `onWeightPickerClick`, `onBack`).
- One file = one class/interface. Group files allowed only for tightly-related sealed classes in one file (like `TrainingResponse`/`ExerciseResponse`/`IterationResponse` — one DTO family).
- DTO — `@Serializable public data class <Name>Response` or `<Name>Body`, fields `@SerialName("...")`, **all nullable with default `= null`**.
- Domain models — `public data class` with non-null fields.
- Entity — `public data class <Name>Entity` with `@Entity`, `@PrimaryKey`, `@Column(name = "snake_case")` if an explicit override is needed.
- State — `@Immutable internal data class <Feature><Subscreen>State` (or `data object` if stateless, or `sealed interface` with subtypes).
- Direction — `internal sealed interface <Feature>Direction : BaseDirection` with `data object`/`data class` subtypes.
- Loader — `@Immutable internal sealed interface <Feature>Loader : BaseLoader` with `@Immutable data object` subtypes.
- Contract — `@Immutable internal interface <Feature>Contract { ...; companion object Empty : <Feature>Contract { ... } }`. Empty — for preview.
- Use case — `public class <Verb><Noun>UseCase(...)` with one `public suspend fun execute(...): Result<T>`.

### Packages

- Base scheme: `com.grippo.<area>.<feature>` (`com.grippo.profile.body`, `com.grippo.authorization.registration.credential`).
- **Existing inconsistency:** `data-features/`, `data-mappers/`, `data-services/`, `dialog-api`, `weight-picker` use packages **with a dot in the directory name** (`com/grippo/data.features.trainings/`, `com/grippo/dto.entity.training/`, `com/grippo/dialog.api/`, `com/grippo/weight.picker/`). Inside, the `package` declaration is normal (`package com.grippo.data.features.trainings`). **Intentional, do not refactor — but new modules should be written without dots in directories.**
- `internal` functions in a public module — in an `internal/` subpackage.

### Kotlin

- Coroutines — **only** `safeLaunch` / `Flow.safeLaunch`. No `viewModelScope.launch`, `runBlocking`, `GlobalScope`.
- Collections in State and Loaders — **only** `kotlinx-collections-immutable` (`ImmutableList`, `ImmutableSet`, `PersistentList`). No `List`/`Set`/`MutableList` in `@Immutable` classes.
- Errors from a use case — `Result<T>`, `.getOrThrow()` inside `safeLaunch`, the exception bubbles to `BaseViewModel.sendError` → `ErrorProvider`.
- Every `@Composable internal fun <X>Screen` takes three arguments in the same order: `state, loaders, contract` (sometimes also `component` for the cross-feature navigator).
- `@AppPreview` + `PreviewContainer` for previews. Preview always uses `<X>Contract.Empty` and stub data (`stub*()` functions from `:ui-core:state`).
- `remember(...)` for derived button state (`val buttonState = remember(loaders, state.x, state.y) { when { ... } }`).
- VM-side string lookup — only via `StringProvider.get(Res.string.x)` (injected). Composable-side — `AppTokens.strings.res(Res.string.x)`.

### Compose specifics

- Recomposition: state classes — `@Immutable`/`@Stable`, immutable collections. Compose stability metrics enabled in convention plugin.
- Global `optIn`s (Material3, Foundation, Coroutines, ForeignApi, etc.) are already enabled in `KotlinMultiplatformConventionPlugin` — **do not duplicate in source files** via `@OptIn`.
- `LaunchedEffect(Unit)` for one-shot navigation side effects is **forbidden** — navigation goes through `Direction` + `eventListener`. `LaunchedEffect` is allowed only for non-navigational side effects (animation kickoff, system API init).
- `BottomSheetToolbar` for all bottom sheets; regular `Toolbar` for screens.
- In LazyColumn — always `key = { it.id }` for stability and `Modifier.animateItem()` for transitions.
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
- `@Single(binds = [TrainingFeature::class])` on `internal class TrainingFeatureImpl(...) : TrainingFeature` — the only way to declare an implementation.
- `@Factory` for cheap, per-call instances (e.g. `OperationManager`).
- `@InjectedParam` — for parameters passed at `get { parametersOf(...) }` time. Used by `BaseViewModel` to thread `coroutineScope` into `OperationManager` and `ResultManager`.

Hand-writing `module { single { ... } }` DSL for new modules is **forbidden**. Use only annotations.

All Koin modules are collected in `:shared/Koin.kt`:

```kotlin
public object Koin {
    public fun init(appDeclaration: KoinAppDeclaration = {}): KoinApplication =
        KoinPlatformTools.defaultContext().startKoin {
            appDeclaration()
            modules(
                ContextModule().module,
                DatabaseModule().module,
                BackendModule().module,
                // ... every module is listed explicitly
            )
        }
}
```

A new module is added to this list **manually**.

---

## Cookbook (step-by-step recipes)

### 1. Add a new screen inside an existing feature

Example: a "Workout history" screen in `:ui-screen-features:profile`.

1. Create the package `com.grippo.profile.workouthistory`.
2. Seven files:
   - `ProfileWorkoutHistoryState.kt` — `@Immutable internal data class ProfileWorkoutHistoryState(...)` or `data object` if static.
   - `ProfileWorkoutHistoryDirection.kt` — `internal sealed interface ProfileWorkoutHistoryDirection : BaseDirection { data object Back : ... }`.
   - `ProfileWorkoutHistoryLoader.kt` — `@Immutable internal sealed interface ProfileWorkoutHistoryLoader : BaseLoader { @Immutable data object LoadHistory : ... }`.
   - `ProfileWorkoutHistoryContract.kt` — `@Immutable internal interface ProfileWorkoutHistoryContract { fun onBack(); ...; companion object Empty : ... }`.
   - `ProfileWorkoutHistoryViewModel.kt` — `internal class ProfileWorkoutHistoryViewModel(private val feature: WorkoutHistoryFeature) : BaseViewModel<...>, ProfileWorkoutHistoryContract { ... }`.
   - `ProfileWorkoutHistoryComponent.kt` — `internal class ProfileWorkoutHistoryComponent(componentContext, private val back: () -> Unit) : BaseComponent<ProfileWorkoutHistoryDirection>(componentContext) { override val viewModel = componentContext.retainedInstance { ProfileWorkoutHistoryViewModel(getKoin().get()) }; override suspend fun eventListener(direction) = when (direction) { Back -> back() }; @Composable override fun Render() { ... } }`.
   - `ProfileWorkoutHistoryScreen.kt` — `@Composable internal fun ProfileWorkoutHistoryScreen(state, loaders, contract) = BaseComposeScreen(...) { ... }` + `@AppPreview private fun Preview() { PreviewContainer { ProfileWorkoutHistoryScreen(stub..., persistentSetOf(), ProfileWorkoutHistoryContract.Empty) } }`.
3. In `:ui-screen-features:screen-api/ProfileRouter.kt` add `@Serializable public data object WorkoutHistory : ProfileRouter()`.
4. In `ProfileComponent.createChild`: `ProfileRouter.WorkoutHistory -> WorkoutHistory(ProfileWorkoutHistoryComponent(context, back = viewModel::onBack))`.
5. In `ProfileComponent.Child` sealed add `data class WorkoutHistory(override val component: ProfileWorkoutHistoryComponent) : Child(component)`.
6. Verify the iOS XCFramework build (`./gradlew :shared:assembleSharedDebugXCFramework`).

**No** new Koin modules needed — VM dependencies come from existing ones.

### 2. Add a new cross-feature navigation point

Example: open a new screen in `:profile` from `:home`.

1. If the route doesn't exist yet — add it to the corresponding `<Feature>Router` in `:screen-api`.
2. In `RootDirection` add `data object NewProfileTab : RootDirection`.
3. In `RootContract` + `RootViewModel` add `fun toNewProfileTab() { navigateTo(RootDirection.NewProfileTab) }`.
4. In `RootComponent.eventListener`: `RootDirection.NewProfileTab -> navigation.push(RootRouter.Profile(ProfileRouter.NewTab))`.
5. In `HomeRootComponent` (or the specific child) — add a `private val toNewProfileTab: () -> Unit` parameter to the constructor, threaded through `RootComponent.createChild`.

### 3. Add a new dialog (bottom sheet)

Example: `:ui-dialog-features:rating-picker`.

1. Create the module `:ui-dialog-features:rating-picker` in `settings.gradle.kts`.
2. `build.gradle.kts`:
   ```kotlin
   plugins {
       id("android.library.convention")
       id("kotlin.multiplatform.convention")
       id("compose.multiplatform.convention")
   }
   kotlin {
       android { namespace = "com.grippo.rating.picker" }
       sourceSets.commonMain.dependencies {
           implementation(projects.uiCore.foundation)
           implementation(projects.uiCore.state)
           implementation(projects.designSystem.core)
           implementation(projects.designSystem.components)
           implementation(libs.immutable.collections)
       }
   }
   ```
3. Seven MVI files like a screen, but `<Name>Component`/`<Name>Screen` are called `RatingPickerComponent`/`RatingPickerScreen`. Component constructor is usually: `(componentContext, initial: <Type>, onResult: (<Type>) -> Unit, back: () -> Unit)`.
4. In `:ui-dialog-features:dialog-api/DialogConfig.kt` add:
   ```kotlin
   @Serializable
   public data class RatingPicker(
       val initial: Int,
       @Transient val onResult: (Int) -> Unit = { },
   ) : DialogConfig(onDismiss = null, dismissBySwipe = true) {
       override val key: String get() = buildKey("RatingPicker", initial)
   }
   ```
5. In `:shared/dialog/content/DialogContentComponent` add the child factory: `is DialogConfig.RatingPicker -> Child.RatingPicker(RatingPickerComponent(context, config.initial, config.onResult, back))`.
6. Use from any VM: `dialogController.show(DialogConfig.RatingPicker(initial = 5, onResult = { v -> update { ... } }))`.

### 4. Add a new data feature module

Example: `:data-features:notifications`.

1. Add modules to `settings.gradle.kts`.
2. `:feature-api` is shared — add to it:
   - `feature-api/.../notifications/NotificationsFeature.kt` (interface + UseCases).
   - `feature-api/.../notifications/models/Notification.kt` (domain model).
3. New module `:data-features:notifications` `build.gradle.kts`:
   ```kotlin
   plugins {
       id("android.library.convention")
       id("kotlin.multiplatform.convention")
       id("koin.annotation.convention")
   }
   kotlin {
       android { namespace = "com.grippo.data.features.notifications" }
       sourceSets.commonMain.dependencies {
           implementation(projects.dataFeatures.featureApi)
           implementation(projects.dataServices.backend)
           implementation(projects.dataServices.database)
           implementation(projects.toolkit.logger)
       }
   }
   ```
4. Files:
   - `data/NotificationsRepositoryImpl.kt` — `@Single(binds = [NotificationsRepository::class]) internal class NotificationsRepositoryImpl(private val api: GrippoApi, private val dao: NotificationsDao) : NotificationsRepository`.
   - `domain/NotificationsRepository.kt` — `internal interface NotificationsRepository { ... }`.
   - `domain/NotificationsFeatureImpl.kt` — `@Single(binds = [NotificationsFeature::class]) internal class NotificationsFeatureImpl(private val repository: NotificationsRepository) : NotificationsFeature`.
   - `NotificationsFeatureModule.kt` — `@Module(includes = [BackendModule::class, DatabaseModule::class]) @ComponentScan public class NotificationsFeatureModule`.
5. In `:shared/build.gradle.kts` add `implementation(projects.dataFeatures.notifications)`.
6. In `:shared/Koin.kt` add `NotificationsFeatureModule().module` to `modules(...)`.

### 5. Add a new mapper

Example: add `NotificationResponse → NotificationEntity`.

1. File `data-mappers/dto-to-entity/src/commonMain/kotlin/com/grippo/dto/entity/notification/NotificationMapper.kt`:
   ```kotlin
   public fun NotificationResponse.toEntityOrNull(): NotificationEntity? {
       val id = AppLogger.Mapping.log(id) { "NotificationResponse.id is null" } ?: return null
       val title = AppLogger.Mapping.log(title) { "NotificationResponse.title is null" } ?: return null
       // ...
       return NotificationEntity(id = id, title = title, ...)
   }
   public fun List<NotificationResponse>.toEntities(): List<NotificationEntity> = mapNotNull { it.toEntityOrNull() }
   ```
2. If a reverse direction is needed — separate file in `:domain-to-dto` with `Notification.toBody(): NotificationBody`.
3. If state/domain are needed — separate files in `:entity-to-domain` and `:domain-to-state`.
4. **Do not** add dependencies between mapper modules: each direction is isolated.

### 6. Add a new Room migration

**Only on request.** Steps (for context):

1. Bump `@Database(version = N+1)` in `Database.kt`.
2. Create `migrations/Migration<N>To<N+1>.kt`:
   ```kotlin
   internal object Migration5To6 : Migration(5, 6) {
       override fun migrate(connection: SQLiteConnection) {
           connection.execSQL("ALTER TABLE ...")
       }
   }
   ```
3. Add to `DatabaseMigrations.all`.
4. If an entity changes — update the `@Entity` class.
5. After Android build — verify `data-services/database/schemas/` (exported JSON schemas) — a new one must appear.
6. **MANDATORY** — verify iOS — `./gradlew :shared:assembleSharedDebugXCFramework` — Room on iOS uses the same migration path.

### 7. Add a new endpoint to `GrippoApi`

1. If there's a new response/body — `data-services/backend/src/commonMain/kotlin/com/grippo/services/backend/dto/<area>/<Name>Response.kt` (or `<Name>Body.kt`):
   ```kotlin
   @Serializable
   public data class NotificationResponse(
       @SerialName("id") val id: String? = null,
       @SerialName("title") val title: String? = null,
       @SerialName("createdAt") val createdAt: String? = null,
   )
   ```
   All fields nullable, default `= null`.
2. In `GrippoApi`, in the right section (`/* * * Notifications service * * */`):
   ```kotlin
   public suspend fun getNotifications(): Result<List<NotificationResponse>> =
       request(method = HttpMethod.Get, path = "/notifications")

   public suspend fun markNotificationRead(id: String): Result<Unit> =
       request(method = HttpMethod.Put, path = "/notifications/$id/read")
   ```
3. Cross-check with the backend contract (Swagger at `/docs`). Any drift — backend wins; mobile does not invent.

### 8. Add a new resource/string

1. `design-system/resources/provider/src/commonMain/composeResources/values/strings.xml` — add `<string name="my_new_key">English text</string>`.
2. Same in `values-uk/strings.xml`, `values-ru/strings.xml`.
3. Usage:
   - In Composable: `AppTokens.strings.res(Res.string.my_new_key)`.
   - In VM: `stringProvider.get(Res.string.my_new_key)`.
   - In State: `UiText.Res(Res.string.my_new_key, formatArgs = persistentListOf(arg1))`.
4. Plurals — `<plurals name="key">` with `<item quantity="one|other">...</item>`. Access — `Res.plurals.key`.

---

## Performance budgets and priorities

No explicit budgets in code. Default priorities:

- Don't block the main thread. All IO/CPU — via `safeLaunch(dispatcher = Dispatchers.IO)` or `withContext(Dispatchers.IO)` (like `BackendClient`).
- Recomposition stability: state — `@Immutable`/`@Stable`, immutable collections everywhere, Compose metrics in `build/compose-metrics`.
- Don't break the iOS XCFramework: any change in public API of `:shared`/`:ui-core:foundation`/`:design-system`/`:data-features:feature-api` is verified on iOS — `./gradlew :shared:assembleSharedDebugXCFramework` must pass.
- `kotlin.native.binary.gc=cms` (concurrent mark+sweep) enabled to reduce stop-the-world pauses on iOS.
- `kotlin.native.binary.smallBinary=true` for smaller iOS frameworks.
- `org.gradle.workers.max=1` to reduce peak memory during native release linking.

---

## Testing strategy

There are no test source sets or `*.kt` tests in the repo. Don't add without an explicit request.

---

## Locked architectural decisions

- Compose Multiplatform for UI on both platforms.
- Decompose 3.5 for navigation and lifecycle (not Compose Navigation, not Voyager).
- Koin 4.2 + annotations + KSP for DI (not Hilt, not Anvil, not Metro).
- Room 2.8 multiplatform for persistence (not SQLDelight).
- Ktor 3.4 for network.
- `kotlinx-serialization` for JSON and Decompose state.
- Pattern Component / Contract / State / Direction / Loader / ViewModel / Screen — mandatory for any new screen/dialog.
- `BaseViewModel` uses CONFLATED `Channel` for navigation — known trade-off with rapid events.
- `fallbackToDestructiveMigration(dropAllTables = true)` on both platforms — intentional.
- Convention plugins in `build-logic/` — the only way to configure modules.
- All Koin modules collected in `:shared/Koin.kt` explicitly.
- 7 mapper directions in 7 modules `:data-mappers:*`. Inline DTO↔Domain↔State conversion in ViewModel is forbidden.
- DTO fields are all nullable + default `= null`. Null mapping goes through `AppLogger.Mapping.log(...) ?: return null`.
- API contract is owned by backend; codegen is NOT used — DTOs in `data-services/backend/dto` are written by hand.
- Resources (`Res.string`, `Res.drawable`) — Compose Multiplatform Resources, **not** `androidx.compose.ui.res`, not Moko Resources.

---

## Scope discipline

- Don't move modules and don't change `settings.gradle.kts` without a request. The ~70-module structure is intentional.
- Don't change `:shared/Koin.kt` order/composition without a request (composition root).
- Don't edit `build-logic/convention/*` while fixing a feature. Convention plugins are a separate task.
- Don't change `gradle/libs.versions.toml` library versions silently.
- Don't touch Room migrations (`Migration*To*.kt`) without an explicit request.
- Don't edit `secure/` or `local.properties`.
- Don't edit `TokenProvider` token-refresh logic without a request (it has mutex + circuit breaker + retry).
- Don't introduce new `@OptIn` in source files — add globally in `KotlinMultiplatformConventionPlugin`.
- Don't add a new CompositionLocal outside `:design-system:core` / `:design-system:resources:provider`.

---

## When to stop and ask

- Any new module or removal of an existing one.
- Changes to `BaseViewModel`, `BaseComponent`, `BaseScreen`, `OperationManager`, `ErrorProvider`, `ResultManager`.
- Changes to `RootComponent`/`RootRouter` or public `*Router` sealed classes in `:ui-screen-features:screen-api`.
- A new Room migration or `@Entity` schema change.
- A new dependency in `gradle/libs.versions.toml`.
- Any change to `:androidApp`/`:iosApp` shell beyond obvious bug fixes.
- Changes to DTOs in `:data-services:backend/dto/...` — this is a backend contract, sync with `grippo-backend` manually.
- Changes to `BackendClient`/`TokenProvider`/`HttpModule` (network core).
- Adding a field to `DialogConfig` (must serialize the `key` correctly).
- Adding a new CompositionLocal or a new `AppToken` (colors/dp/typography).

---

## Anti-patterns (refuse to write)

- Direct `viewModelScope.launch`, `runBlocking`, `GlobalScope.launch`, `CoroutineScope().launch`. Only `safeLaunch` / `Flow.safeLaunch`.
- Mutable collections (`MutableList`, `mutableStateListOf`, `mutableMapOf`) in `@Immutable` state.
- Business logic in `Component`, `Screen`, mappers, or DTOs. Logic — in `ViewModel` or domain layer (`*Repository`/`*UseCase`/`*FeatureImpl`).
- Mapping outside `:data-mappers:*`. Inline conversion in ViewModel/Screen — forbidden.
- Direct dependency of a UI module on `:data-services:*` — only via `:data-features:*`.
- Creating a Koin module via hand-written `module { ... }` DSL for a new module.
- `LaunchedEffect(Unit)` for navigation — use `Direction` + `eventListener`.
- Catching exceptions inside a ViewModel manually (`try { ... } catch { ... }`) — let it flow through `safeLaunch` → `ErrorProvider`. Exception: domain logic (`Result.onSuccess { ... }` after `api.<x>()`).
- Hardcoded strings in UI — only via `AppTokens.strings.res(Res.string.x)` (Composable) or `StringProvider.get(...)` (VM).
- Storing `String` in State if the value depends on resources/locale — use `UiText`.
- Direct Koin `get()` in Screen — DI goes only via `componentContext.retainedInstance { <X>ViewModel(getKoin().get(), ...) }` in Component.
- Accessing `Context`/`Activity` directly from `commonMain` — go through `:toolkit:context` (`NativeContext`).
- Hidden coupling of feature modules via transitive `api` — list every needed dependency in your own `build.gradle.kts`.
- Using `androidx.compose.ui.res.stringResource(...)` or `painterResource(R.drawable...)` — only Compose Multiplatform Resources (`Res.string.*` / `Res.drawable.*`).
- Calling `Composable` from `BaseViewModel` (e.g. `@Composable fun ...` directly). VM does not know Compose.
- Threading Decompose `ChildSlot`/`ChildStack` into Screen functions instead of a `component.childStack` reference to the Component itself — Screen works only with state/loaders/contract, optionally with the component for rendering stack/slot.
- Storing `LocalDateTime` without UTC normalization in DB. In DAO, always go through `DateTimeUtils.toUtcIso(...)`.

---

## Orchestrator workflow

The `orchestrator/` tree is the agent workforce bound to this project (config: `orchestrator/project-config.md`; end-user entry: `orchestrator/README.md`). The detailed skills installed in `.claude/skills/` (11: `task-prep`, `task-orchestrator`, `ui-feature`, `design-system`, `data-layer`, `mappers`, `di-modules`, `platform-build-toolkit`, `validation-gates`, `backend-contract-client`, `launch-readiness`) are the source of truth for task execution; this file stays the high-level guide.

- **Control surface**: `npm start` from the repo root → <http://localhost:8000/site/> (Node 22 workspace; `npm test` = fast verify suite).
- **Task workflow**: tasks flow through the four-column kanban under `orchestrator/tasks/`: `backlog -> task-prep -> pending -> task-prep -> todo -> orchestrator -> done`. Drop free-text ideas in `backlog/`, run `task-prep`; if questions land in `pending/`, answer them and run `task-prep` again; then ask Claude to run the task once it is in `todo/`. The orchestrator moves the file to `done/` on success. See `orchestrator/tasks/README.md`.
- **Figma**: disabled for this project (`figmaEnabled: false`) — there is no Figma library; the `orchestrator/figma/` sidecar stays dormant.

### Backend contract rules

The validated current generation (resolve exact inventory and area paths with `npm run --silent contract:paths` in `orchestrator/api-contract/`) is the contract of record for endpoint paths, methods, field names, types, nullability, and enums. Consult it before writing or changing any DTO / `GrippoApi` method / mapper; never invent endpoints or fields. The all-nullable defensive DTO discipline stays regardless of what the spec declares. When the backend changes, refresh the snapshot in the Backend tab or with typed `contract:probe` then the matching `contract:refresh-*`, and act on `backend-contract-drift` findings instead of patching blind. Until the first snapshot is published via Integrations → Backend (Test + Refresh), endpoint/DTO work is BLOCKED by the drift gate — the Swagger at `grippo-backend`'s `/docs` is the source it will bind.
