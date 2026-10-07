# Module Structure (feature, ui-core, shells, compose-libs)

> Examples use `Note` / `Tag` / `Profile` / `Home` as the generic `<Feature>`/`<Entity>`. Substitute your domain.

## `:ui-screen-features:*`

### `:ui-screen-features:screen-api` (the contract module)

Houses **only**: `<Feature>Router` sealed classes (`@Serializable public sealed class HomeRouter : BaseRouter { ... }` — all sub-routes plus cross-feature routes the root navigator must know); the `Deeplink` enum; result protocol types nested inside `Router.<Screen>.Action`. This module is `public` API; UI screen features depend on it, and `:shared` depends on it.

Build: `android.library.convention` + `kotlin.multiplatform.convention` + `compose.multiplatform.convention` + `kotlin.serialization` alias; namespace `com.<org>.<product>.ui.screen.features.screen.api`; deps `uiCore.foundation`, `uiCore.state`, `toolkit.dateUtils`, `designSystem.core`, `designSystem.resources.provider`, serialization-json, datetime, immutable-collections, `compose.foundation`. (`compose.multiplatform.convention` is applied because `Action` types may carry Compose-friendly payloads.)

### `:ui-screen-features:<feature>`

One module per top-level feature flow. Each owns:
- A **feature-root component** with its own `StackNavigation<<Feature>Router>`. Naming:
  - **Bare-name** (`<Feature>Component`/`<Feature>Screen`/`<Feature>ViewModel`) — the default. Examples: `:profile` (`ProfileComponent`), `:debug` (`DebugComponent`), `:authorization` (`AuthComponent`).
  - **`<Feature>Root*`** — reserved for features whose first sub-screen reuses the feature name. Example: `:home` (root `HomeRootComponent`; sub-screen package `home/HomeComponent`).
  - The root component is `public` (consumed from `:shared`); all sub-screen components are `internal`.
- One package per sub-screen (`com.<org>.<product>.<feature>.<subscreen>`) with the **seven MVI files**.
- A feature-root `Screen` composable that delegates to per-sub-screen `Render()` via `ChildStack`. Single-screen features still ship one (keeps the door open for adding sub-screens without renaming).

Build (typical): `android.library.convention` + `kotlin.multiplatform.convention` + `compose.multiplatform.convention` + `koin.annotation.convention`; namespace `com.<org>.<product>.ui.screen.features.<feature>`. **Mandatory deps** for any screen feature: `:ui-core:foundation`, `:ui-screen-features:screen-api`, `:design-system:core/components/resources/preview`, `:data-features:feature-api`, `:ui-dialog-features:dialog-api` (plus typically `:ui-core:state`, `:data-mappers:domain-to-state`, `:toolkit:dateUtils`, `compose.foundation`/`material3`, immutable-collections, datetime). `:compose-libs:*` only when actually used.

The single wider-data-layer carve-out is `:ui-screen-features:authorization` (adds `:data-services:google-auth`/`apple-auth`/`firebase` for the platform credential flow before a domain user exists). Documented carve-out — do not copy.

## `:ui-dialog-features:*`

### `:ui-dialog-features:dialog-api` (the contract module)

Houses: `DialogConfig` (`@Serializable sealed class DialogConfig(@Transient open val onDismiss: (() -> Unit)? = null, open val dismissBySwipe: Boolean = true) { abstract val key: String }`, each subtype with a `@Transient onResult`); `DialogController` (`interface { fun show(config: DialogConfig) }` — **no `dismiss()`**, do not invent one); `DialogProvider` (`interface { val dialog: Flow<DialogConfig> }`); `DialogControllerImpl` (`@Single(binds = [DialogController::class, DialogProvider::class])`, buffered `Channel`); `DialogModule` (`@Module @ComponentScan public class DialogModule` — lives **here**, `:shared/Koin.kt` lists `DialogModule().module`); `Constants.kt` (`DIALOG_EXIT_ANIMATION_DURATION = 300.milliseconds`); the `buildKey(...)` helper (length-prefixed, `protected`).

Build: same plugins as screen-api; namespace `com.<org>.<product>.ui.dialog.features.dialog.api`; deps `uiCore.foundation`, `uiCore.state`, `toolkit.dateUtils`, serialization-json, datetime, `compose.foundation`. (`:ui-core:state` carries `AppErrorState`; `:ui-core:error:error-provider` is intentionally **not** depended on here.)

### `:ui-dialog-features:<dialog>`

One module per bottom-sheet flow (`amount-picker`, `date-picker`, `confirmation`, `error-display`, ...). Identical to a screen feature **except**:
- Component ctor params are typically `(componentContext, initial, onResult, back, close)`.
- Ships **no** toolbar (neither `Toolbar` nor `BottomSheetToolbar`) — `BaseComposeScreen(ScreenBackground.Color(...background.dialog))` with `Spacer(AppTokens.dp.dialog.top)`, centered title `Text`, body, `Spacer(AppTokens.dp.dialog.bottom)` + `Spacer(Modifier.navigationBarsPadding())`. The `BottomSheetToolbar` belongs to the outer `:shared/dialog/DialogScreen.kt` host.
- Content rendered inside a `ModalBottomSheet` managed by `DialogComponent`, not the feature.
- The dialog feature **never** holds a `StackNavigation` of its own; in-sheet multi-step flows use the outer `DialogContentComponent` inner `StackNavigation<DialogConfig>`. State is for mode/data, not navigation.

Build (typical): same plugins; namespace `com.<org>.<product>.ui.dialog.features.<feature>`; deps `uiCore.foundation`, `uiCore.state`, `designSystem.core/resources.provider/components/preview`, `compose.foundation`/`material3`, immutable-collections. Dialog features do **not** depend on `:data-features:feature-api` unless they read/write domain data (rare).

## File layout for a feature

Screen feature (bare-name root):
```
ui-screen-features/profile/
  build.gradle.kts
  src/commonMain/kotlin/com/<org>/<product>/profile/
    ProfileComponent.kt           // public — feature root, owns StackNavigation<ProfileRouter>
    ProfileContract.kt  ProfileState.kt  ProfileDirection.kt  ProfileLoader.kt  ProfileViewModel.kt
    ProfileScreen.kt              // renders ChildStack of sub-screens
    summary/                      // sub-screen "summary" — internal
      ProfileSummaryComponent.kt  ProfileSummaryContract.kt  ... (seven files)
      ProfileSummaryScreen.kt     // composes the components/ pieces; holds no large UI itself
      components/                 // non-*Screen UI — one composable per file
        ProfileSummaryHeader.kt   //   entry composable + private helpers + @AppPreview
        ProfileStatsCard.kt
    settings/ ...
```

Screen feature (`*Root*` root — only when a sub-screen shares the feature name): `home/HomeRootComponent.kt` ... `HomeRootScreen.kt`, with sub-screen package `home/home/HomeComponent.kt` ...

Dialog feature: `ui-dialog-features/amount-picker/src/commonMain/kotlin/com/<org>/<product>/amount/picker/AmountPickerComponent.kt` ... `AmountPickerDirection.kt` (Back, Close only — no nav), `AmountPickerLoader.kt` (usually empty), `AmountPickerScreen.kt`.

## Rules summary (MUST)

- One module per feature. Don't put two unrelated features in the same module.
- One package per sub-screen / sub-dialog; the seven MVI files at its root, plus an optional `components/` subfolder.
- **Non-`*Screen` UI lives in `components/`** — one cohesive composable per file (file name = the entry composable, with its `private` helpers and `@AppPreview`s co-located). The `Screen` orchestrates; it doesn't inline large UI or scatter it across the seven. A widget reused across features graduates to `:design-system:components` / `:compose-libs:*`.
- Sub-screens are `internal`; the feature-root component is `public` (so `:shared`'s `RootComponent.createChild` can instantiate it).
- A screen feature exposes routes via `:ui-screen-features:screen-api`; a dialog feature exposes config via `:ui-dialog-features:dialog-api`. **Direct symbol imports between feature modules are forbidden.**
- A feature may depend on `:compose-libs:*` for specialized widgets. It must **not** depend on another feature module.

## Anti-pattern: feature-to-feature cross-import (MUST)

Two `:ui-screen-features:*` modules cannot import each other. If feature A needs to navigate to a screen in feature B:
1. The route is declared in `:ui-screen-features:screen-api` (public `<B>Router` sealed class).
2. `RootDirection` adds an entry; `RootViewModel` exposes a callback (`fun toB(...)`).
3. Feature A's `RootComponent` receives the callback via its constructor (threaded from `:shared`'s `RootComponent.createChild`).
4. Feature A's ViewModel calls the callback; `RootComponent.eventListener` translates `RootDirection.B` into `navigation.push(RootRouter.B(...))`.

Direct symbol import between feature modules is **forbidden**. (Full recipe in `references/navigation.md`.)

---

## `:ui-core:*` modules

| Module | Purpose | Convention plugins |
|---|---|---|
| `:ui-core:foundation` | `BaseViewModel`, `BaseComponent`, `BaseComposeScreen`, `OperationManager`, `ResultManager`, platform helpers | KMP + Compose + Koin |
| `:ui-core:state` | `UiText`, `*FormatState`, `AppErrorState` (UI mirror, package `core.state.error`), reusable `*State` classes, `stub*` factories | KMP + Compose |
| `:ui-core:error:error-provider` | `AppError` sealed hierarchy + `ErrorProvider` interface | KMP |
| `:ui-core:error:error-provider-impl` | `ErrorProviderImpl` — maps `AppError` → `AppErrorState` → `DialogConfig.ErrorDisplay` | KMP + Koin |

### `:ui-core:foundation`
The heart of MVI (full API: `references/base-classes.md`). Exposes `BaseViewModel`/`BaseComponent`/`BaseComposeScreen`, the marker interfaces, `ResultKey`/`ResultKeys`, `OperationManager`/`OperationManagerImpl`, `ResultManager`/`ResultEmitter`, `CoreModule`, `collectAsStateMultiplatform()`, `platformAnimation()`/`platformStackAnimator()`. Build: KMP+Compose+Koin; namespace `com.<org>.<product>.ui.core.foundation`. Only `decompose.core`/`decompose.extensions` are `api`-exposed (public `Base*` types reference `ComponentContext`/`ChildStack`). `:data-services:firebase` is imported for the error pipeline (**firebaseEnabled gate**: when `false`, strip the dependency line + the import/call site in `BaseViewModel`; `:data-services:firebase` isn't scaffolded for `false` projects). This module does **not** depend on `:design-system:*` — base classes never read tokens.

### `:ui-core:state`
Reusable UI state data classes + formatters: `UiText`, `*FormatState`, `AppErrorState` (package `core.state.error` — UI mirror of `AppError`, consumed by `DialogConfig.ErrorDisplay`, produced by `ErrorProviderImpl`), product-specific reusable state, `stub*()` factories. Every class `@Immutable`/`@Stable`; every collection immutable; resource-dependent strings as `UiText`; `stub*()` next to the class. Build: KMP+Compose + serialization alias (because `*FormatState`/`DateFormat` are `@Serializable` for `*Router`/`DialogConfig`); namespace `com.<org>.<product>.ui.core.state`.

### `:ui-core:error:error-provider`
Pure types only — `AppError` (sealed: `Network.{NoInternet, Timeout, Expected, Unexpected}`, `Expected`, `Unknown`; extends `Exception(message, cause)`) + `ErrorProvider` interface. **No DI**, deps-free. `AppErrorState` lives in `:ui-core:state`, **not** here — keeping this module UI-free lets `:data-services:*`/`:toolkit:http-client` reference `AppError` without UI.

### `:ui-core:error:error-provider-impl`
`ErrorProviderImpl` (`@Single(binds = [ErrorProvider::class])`, `internal`, `DialogController` injected) maps `AppError` → `AppErrorState` and shows `DialogConfig.ErrorDisplay`. `ErrorModule` (`@Module(includes = [DialogModule::class]) @ComponentScan public class`) — the `includes` is intentional (the pipeline is meaningless without the dialog controller). Depends on `:ui-dialog-features:dialog-api` + `:ui-core:error:error-provider` + `:ui-core:state`.

**Why two error modules:** the API (`error-provider`) is pure types anyone can depend on; the impl (with the heavy `DialogController` dependency) is separate so it stays out of the API surface — same pattern as `:design-system:resources:provider` vs `:provider-impl` and `:data-features:feature-api` vs `:data-features:<feature>`.

---

## App shells (scaffold context)

### `:androidApp`
`android.application.convention` (not `library`). Compose plugins applied directly (it's a single-target Android app, not KMP). `App : Application` starts Koin (`Koin.init { androidContext(this); androidLogger() }`) + `FirebaseProvider.setup(...)`. `MainActivity : ComponentActivity` holds `private val root: RootComponent by lazy { retainedComponent { RootComponent(componentContext = it, close = ::finishAffinity, deeplink = intent.getStringExtra(...)) } }`; `onCreate` installs splash + edge-to-edge + `setContent { root.Render() }` (cold-start deeplink); `onNewIntent` routes warm-start deeplinks via `root.handleDeeplink(it)`. Build directly depends on `:shared`, `:design-system:core`, `:ui-core:foundation`, a few toolkit modules; Firebase is wired here (Android-only SDKs). Manifest: single `MainActivity` (`exported=true`, `launchMode=singleTop`, `MAIN`/`LAUNCHER`, splash theme); **no** `<uses-permission>` (`POST_NOTIFICATIONS` requested at runtime via `:toolkit:permission-manager`); add deeplink intent filters only when exposing external deeplinks.

### `:iosApp`
**Not** a Gradle module — an Xcode project linking the static `XCFramework` from `:shared`. `iOSApp.swift` (`@main`) inits Koin (`Koin().doInit(appDeclaration: { _ in })`) and hosts `RootView(root:backDispatcher:)`. `AppDelegate` owns the `RootComponent` + `BackDispatcher` and calls `FirebaseApp.configure()` + `FirebaseProvider.shared.setup(...)`. `iosMain/RootViewController.kt` exports `rootViewController(root, backDispatcher): UIViewController = ComposeUIViewController { PredictiveBackGestureOverlay(... content = { root.Render() }) }`. XCFramework refresh: `./gradlew :shared:assembleSharedDebugXCFramework` (lands at `shared/build/XCFrameworks/debug/shared.xcframework`); Release variant analogous.

### What the shells MUST NOT do
Define Composables (except entry-point wrapping). Hold state classes / business logic. Import data-feature modules directly (they import `:shared`). Configure Koin modules (the only Koin call is `Koin.init` / `Koin().doInit`). Format dates, render strings, or know about resources.

---

## `:compose-libs:*` (when a widget graduates out of a feature)

Reference modules: `:compose-libs:chart`, `:konfetti`, `:wheel-picker`, `:segment-control`. Add/remove per product.

### Rules (MUST)
1. **No design-system imports.** Style customization comes through Composable parameters (`Color`, `TextStyle`, `Dp`), not by reading `AppTokens` — keeps the widget reusable across products.
2. **No data-layer access.** Input arrives as Composable parameters (`List<ChartPoint>`, `ImmutableList<WheelOption>`), never from a feature/repository.
3. **Stateless or self-contained state.** Owns animation/gesture state via `remember`/`rememberSaveable`; no ViewModel dependency.
4. **Public API.** All types/Composables `public`.
5. **`@Stable` / `@Immutable` everywhere** on input types.

Build (typical): `android.library.convention` + `kotlin.multiplatform.convention` + `compose.multiplatform.convention`; namespace `com.<org>.<product>.compose.libs.<name>`; deps `compose.foundation` (+ `compose.material3` only if it uses Material3 primitives, immutable-collections). **No Koin. No `:toolkit:*`. No `:design-system:*`.**

### When to extract a widget into `:compose-libs:*` (SHOULD)
Extract when: used by **two or more** feature modules and **not** a design-system primitive; has significant internal state/animation/gesture (≥ 100 LoC of logic); product-agnostic (swapping colors/typography makes it usable in another product without code changes). **Don't** extract: one-off styled wrappers around Material3 primitives (→ `:design-system:components`); widgets tightly coupled to product domain (→ `:design-system:components`, which has `AppTokens` + product types).

How widgets are styled by callers: the caller passes design tokens explicitly (`textStyle = AppTokens.typography.h4()`, `color = AppTokens.colors.text.primary`); the widget's own implementation never references `AppTokens`. If you want to import `AppTokens` into a `:compose-libs:*` module, the widget isn't product-agnostic — either move it to `:design-system:components`, or add a parameter (`color: Color`, `style: TextStyle`). Prefer the parameter unless the widget already has many product-specific assumptions baked in.
