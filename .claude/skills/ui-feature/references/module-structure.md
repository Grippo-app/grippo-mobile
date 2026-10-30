# Module Structure (feature, ui-core, shells, compose-libs)

> Note/Tag/AmountPicker/NoteDetail examples are educational, nonexistent APIs. They are not the current route, dependency or source catalog; inspect real callers and signatures before adapting them.

> Examples use `Note` / `Tag` / `Profile` / `Home` as the generic `<Feature>`/`<Entity>`. Substitute your domain.

## `:ui-screen-features:*`

### `:ui-screen-features:screen-api` (the contract module)

Houses **only**: `<Feature>Router` sealed classes (`@Serializable public sealed class HomeRouter : BaseRouter { ... }` — all sub-routes plus cross-feature routes the root navigator must know); the `Deeplink` enum; result protocol types nested inside `Router.<Screen>.Action`. This module is `public` API; UI screen features depend on it, and `:shared` depends on it.

Build: `android.library.convention` + `kotlin.multiplatform.convention` + `compose.multiplatform.convention` + `kotlin.serialization` alias; namespace `com.grippo.ui.screen.features.screen.api`; deps `uiCore.foundation`, `uiCore.state`, `toolkit.dateUtils`, `designSystem.core`, `designSystem.resources.provider`, serialization-json, datetime, immutable-collections, `compose.foundation`. (`compose.multiplatform.convention` is applied because `Action` types may carry Compose-friendly payloads.)

### `:ui-screen-features:<feature>`

One module per top-level feature flow. Each owns:
- A public entry Component. Multi-screen flows use a container root with
  StackNavigation<FeatureRouter>; a single-screen feature can expose its screen
  Component directly, with seven MVI files and its own screenPresentation. Naming:
  - **Bare-name** (`<Feature>Component`/`<Feature>Screen`/`<Feature>ViewModel`) — the default. Examples: `:profile` (`ProfileComponent`), `:debug` (`DebugComponent`), `:authorization` (`AuthComponent`), `:main` (`MainComponent`), `:training` (`TrainingComponent`).
  - **`<Feature>Root*`** — reserved for features whose first sub-screen reuses the feature name. Examples: `:home` (root `HomeRootComponent`; sub-screen package `home/HomeComponent`), `:trainings` (root `TrainingsRootComponent`; sub-screen `trainings/TrainingsComponent`). `:exercises` (`ExercisesRootComponent` over `library/ExerciseLibraryComponent`) is an existing exception, not a precedent.
  - The root component is `public` (consumed from `:shared`); all sub-screen components are `internal`.
- One package per sub-screen (`com.grippo.<feature>.<subscreen>`) with the **seven MVI files**.
- A multi-screen feature-root Screen delegates to child Render() through ChildStack.
  A single-screen feature may expose its public Component directly, as Debug does;
  do not create an empty graph solely for hypothetical future screens.

Build (typical): `android.library.convention` + `kotlin.multiplatform.convention` + `compose.multiplatform.convention` + `koin.annotation.convention`; the Android convention sets the default `com.grippo` namespace and each module overrides it in `kotlin { android { namespace = "com.grippo.ui.screen.features.<name>" } }`. **Dependencies follow actual usage**, not a blanket scaffold: `:ui-core:foundation`, `:ui-screen-features:screen-api`, `:design-system:core/components/resources/preview`, `:data-features:feature-api`, `:ui-dialog-features:dialog-api` (plus typically `:ui-core:state`, `:data-mappers:domain-to-state`, `:toolkit:date-utils`, `compose.foundation`/`material3`, immutable-collections, datetime). `:compose-libs:*` only when actually used.

Every new screen feature depends on `:analytics:analytics-api`; shared registers
its implementation. Each independent screen must declare its ViewModel's
screenPresentation; containers keep default null. ViewModels use one typed feature
service and foundation owns the common screen binding. Apply the mandatory coverage
gate in [feature analytics](../../feature-analytics/SKILL.md) before completion;
existing-feature changes cover the affected scope.

The single wider-data-layer carve-out is `:ui-screen-features:authorization` (adds `:data-services:google-auth`/`apple-auth` for the platform credential flow before a domain user exists). Firebase transport belongs to application bootstrap. Documented carve-out — do not copy to other features.

## `:ui-dialog-features:*`

### `:ui-dialog-features:dialog-api` (the contract module)

Houses `DialogConfig`, `DialogController`, `DialogProvider` and their annotated
`DialogModule`. The [dialog contract and host policy](dialogs.md) own their
content, session navigation, delivery, retention and callback ordering.

Build: Android/KMP/Compose conventions plus `koin.annotation.convention`;
namespace `com.grippo.ui.dialog.features.dialog.api`; dependencies `uiCore.foundation`,
`uiCore.state`, `toolkit.dateUtils`, datetime and `libs.compose.foundation`.
The current in-memory dialog API has no serialization plugin/json dependency. (`:ui-core:state` carries `AppErrorState`; `:ui-core:error:error-provider` is intentionally **not** depended on here.)

### `:ui-dialog-features:<dialog>`

One module per bottom-sheet flow (`weight-picker`, `date-picker`, `confirmation`, `error-display`, ...). Identical to a screen feature **except**:
- Component ctor params are typically `(componentContext, initial, onResult, back)`; no dialog Component takes a `close` parameter — the host closes sessions.
- Ships **no** toolbar or duplicate title — `BaseComposeScreen(ScreenBackground.Color(...background.dialog))`, laid out per step 3 of the [dialogs](dialogs.md) recipe. The outer `:shared/dialog/DialogScreen.kt` host renders `BottomSheetToolbar` using the active config's `title: UiText`.
- Content rendered inside a `ModalBottomSheet` managed by `DialogComponent`, not the feature.
- The dialog feature **never** holds a `StackNavigation` of its own; in-sheet multi-step flows use the shared `DialogContentComponent` inner `StackNavigation<DialogStep>` and the originating step's `DialogController.Session`. State is for mode/data, not navigation.

Build (typical): same plugins; each module overrides the default namespace with `com.grippo.ui.dialog.features.<name>`; deps `uiCore.foundation`, `analytics.analyticsApi`, `uiCore.state`, `designSystem.core/resources.provider/components/preview`, `compose.foundation`/`material3`, immutable-collections (pickers add `composeLibs.wheelPicker`). Dialog features do **not** depend on `:data-features:feature-api` unless they read/write domain data (rare).

Every dialog module depends on `:analytics:analytics-api` and has its own
`:analytics:<dialog>` implementation module registered in `shared/.../Koin.kt`.
The dialog ViewModel observes its own callbacks through that
service — `SelectionChanged`, `SubmitClicked`, `DismissRequested` in
`WeightPickerViewModel` — and declares no `screenPresentation` hook. Caller
open/result facts belong to the owning screen VM. Wiring:
[new module checklist](../../platform-build-toolkit/references/new-module.md#dialog-feature-ui-dialog-featuresdialog);
rules: [feature-analytics](../../feature-analytics/SKILL.md).

## File layout for a feature

Screen feature (bare-name root):
```
ui-screen-features/profile/
  build.gradle.kts
  src/commonMain/kotlin/com/grippo/profile/
    ProfileComponent.kt           // public — owns StackNavigation<ProfileRouter>; DRAFT companion if it owns a draft
    ProfileContract.kt  ProfileState.kt  ProfileDirection.kt  ProfileLoader.kt  ProfileViewModel.kt
    ProfileScreen.kt              // renders ChildStack of sub-screens
    summary/                      // sub-screen "summary" — internal
      ProfileSummaryComponent.kt  ProfileSummaryContract.kt  ... (seven files)
      ProfileSummaryScreen.kt     // screen composition
      components/                 // optional — extracted feature composables
        ProfileSummaryHeader.kt   //   entry composable + private helpers + @AppPreview
        ProfileStatsCard.kt
    settings/ ...
```

Screen feature (`*Root*` root — only when a sub-screen shares the feature name): `home/HomeRootComponent.kt` ... `HomeRootScreen.kt`, with sub-screen package `home/home/HomeComponent.kt` ...

Dialog feature: `ui-dialog-features/amount-picker/src/commonMain/kotlin/com/grippo/amount/picker/AmountPickerComponent.kt` ... `AmountPickerDirection.kt` (typed result, Back and semantic navigation intents as required), `AmountPickerLoader.kt` (usually empty), `AmountPickerScreen.kt`.

## Rules summary (MUST)

- One module per feature. Don't put two unrelated features in the same module.
- One package per sub-screen / sub-dialog; the seven MVI files at its root, plus an optional `components/` subfolder.
- Apply the [Compose extraction criteria](../../compose-components/references/structure-and-runtime.md#read-from-top-to-bottom) before splitting feature UI. Extracted feature composables live in the sibling `components/` folder, one entry composable per file named after it, with its private implementation helpers and previews co-located. Shared widget ownership follows [module ownership](#when-to-extract-a-widget-into-compose-libs).
- Sub-screens are `internal`; the feature-root component is `public` (so `:shared`'s `RootComponent.createChild` can instantiate it).
- Components with saved drafts follow [state-preservation's ownership and implementation](../../state-preservation/SKILL.md#ownership-and-implementation).
- A screen feature exposes routes via `:ui-screen-features:screen-api`; a dialog feature exposes config via `:ui-dialog-features:dialog-api`. **Direct symbol imports between feature modules are forbidden.**
- A feature may depend on `:compose-libs:*` for specialized widgets. It must **not** depend on another feature module.

## Anti-pattern: feature-to-feature cross-import (MUST)

Cross-feature jumps follow the canonical
[navigation recipe](navigation.md#cross-feature-navigation) and the
[module dependency rules](../../../../CLAUDE.md#module-dependency-rules).

---

## `:ui-core:*` modules

| Module | Purpose | Convention plugins |
|---|---|---|
| `:ui-core:foundation` | `BaseViewModel`, `BaseComponent`, `BaseComposeScreen`, `OperationManager`, `ResultManager`, platform helpers | KMP + Compose + Koin |
| `:ui-core:state` | `UiText`, `*FormatState`, `AppErrorState` (UI mirror, package `core.state.error`), reusable `*State` classes, `stub*` factories | KMP + Compose |
| `:ui-core:error:error-provider` | `AppError` sealed hierarchy + `ErrorProvider` interface | KMP |
| `:ui-core:error:error-provider-impl` | `ErrorProviderImpl` — maps `AppError` → `AppErrorState` → `DialogConfig.ErrorDisplay` | KMP + Koin |

### `:ui-core:foundation`
The heart of MVI (full API: `references/base-classes.md`). Exposes `BaseViewModel`/`BaseComponent`/`BaseComposeScreen`, the marker interfaces, `ResultKey`/`ResultKeys`, internal `OperationManager`/`OperationManagerImpl`, `ResultManager`/`ResultEmitter`, `CoreModule`, `collectAsStateMultiplatform()`, `platformAnimation()`/`platformStackAnimator()`. Build: KMP+Compose+Koin; namespace `com.grippo.ui.core.foundation`. Decompose, serialization and toolkit analytics APIs are exposed as required by public foundation types; use the actual build file for dependency/export details. Toolkit `CrashReporting` API is injected for the error pipeline; foundation does not import the Firebase transport. This module does **not** depend on `:design-system:*` — base classes never read tokens.

### `:ui-core:state`
Reusable UI state data classes + formatters: `UiText`, `*FormatState`, `AppErrorState` (package `core.state.error` — UI mirror of `AppError`, consumed by `DialogConfig.ErrorDisplay`, produced by `ErrorProviderImpl`), product-specific reusable state, `stub*()` factories. Every class `@Immutable`/`@Stable`; every collection immutable; resource-dependent strings as `UiText`; `stub*()` next to the class. Build: KMP+Compose + serialization alias (because `*FormatState`/`DateFormat` are `@Serializable` for `*Router` payloads; dialog configs stay in memory); namespace `com.grippo.ui.core.state`.

### `:ui-core:error:error-provider`
Pure types only — `AppError` (sealed: `Network.{NoInternet, Timeout, Expected, IncompleteResponse, Unexpected}`, `Expected`, `Unknown`; extends `Exception(message, cause)`) + `ErrorProvider` interface. **No DI**, deps-free. `AppErrorState` lives in `:ui-core:state`, **not** here — keeping this module UI-free lets `:data-services:*`/`:toolkit:http-client` reference `AppError` without UI.

### `:ui-core:error:error-provider-impl`
`ErrorProviderImpl` (`@Single(binds = [ErrorProvider::class])`, `internal`, `DialogController` injected) maps `AppError` → `AppErrorState` and shows `DialogConfig.ErrorDisplay`. `ErrorModule` (`@Module(includes = [DialogModule::class]) @ComponentScan public class`) — the `includes` is intentional (the pipeline is meaningless without the dialog controller). Depends on `:ui-dialog-features:dialog-api` + `:ui-core:error:error-provider` + `:ui-core:state`.

**Why two error modules:** the API (`error-provider`) is pure types anyone can depend on; the impl (with the heavy `DialogController` dependency) is separate so it stays out of the API surface — same pattern as `:design-system:resources:provider` vs `:provider-impl` and `:data-features:feature-api` vs `:data-features:<feature>`.

---

## App shells (scaffold context)

### `:androidApp`
Sources: `androidApp/src/main/java/com/grippo/android/` (`App.kt`, `MainActivity.kt`). `android.application.convention` (not `library`). Compose plugins applied directly (it's a single-target Android app, not KMP). `App : Application` starts Koin (`Koin.init { androidContext(this); androidLogger() }`) and attaches platform adapters through the returned `ApplicationBootstrap`. `MainActivity : ComponentActivity` holds `private val root: RootComponent by lazy { retainedComponent { RootComponent(componentContext = it, close = ::finishAffinity, deeplink = intent.getStringExtra(...)) } }`; `onCreate` installs splash (kept on screen until `AppLocale.isResolved`) + edge-to-edge + `setContent { root.Render() }` (cold-start deeplink); `onNewIntent` routes warm-start deeplinks via `root.handleDeeplink(it)`. Build directly depends on `:shared`, `:design-system:core`, `:ui-core:foundation`, a few toolkit modules; Firebase is wired here (Android-only SDKs). Inspect the current AndroidManifest and permission-manager declarations before permission changes; a runtime request alone does not establish manifest permission availability. Add deeplink intent filters only when exposing external deeplinks.

### `:iosApp`
**Not** a Gradle module — an Xcode project linking the static `XCFramework` from `:shared`. `iOSApp.swift` (`@main`) hosts `RootView(root:backDispatcher:)`. `iosApp/iosApp/AppDeletage.swift` (file name carries a typo; the class is `AppDelegate`) owns the `RootComponent` + `BackDispatcher` and initializes Koin once, retains `ApplicationBootstrap`, and calls `FirebaseApp.configure()` before attaching SDK adapters. `iosMain/RootViewController.kt` exports `rootViewController(root, backDispatcher): UIViewController = ComposeUIViewController { PredictiveBackGestureOverlay(... content = { root.Render() }) }`. XCFramework refresh: `./gradlew :shared:assembleSharedDebugXCFramework` (lands at `shared/build/XCFrameworks/debug/shared.xcframework`); Release variant analogous.

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
5. Apply [Compose stability contracts](../../compose-components/references/structure-and-runtime.md#stability-and-recomposition).

Build (typical): `android.library.convention` + `kotlin.multiplatform.convention` + `compose.multiplatform.convention`; namespace `com.grippo.compose.libs.<name>`; deps `compose.foundation` (+ `compose.material3` only if it uses Material3 primitives, immutable-collections). **No Koin. No `:toolkit:*`. No `:design-system:*`.**

### When to extract a widget into `:compose-libs:*`

Choose ownership from real reuse and product independence, not line count. A
product-agnostic widget shared by callers may belong in compose-libs; tokens or
product UI-state semantics belong in design-system components. A cohesive
feature-specific widget may remain in its owning feature regardless of size.
Do not create a new module merely to satisfy a LoC threshold; evaluate the
required dependencies and authorized architecture scope.

How widgets are styled by callers: the caller passes design tokens explicitly (`textStyle = AppTokens.typography.h4()`, `color = AppTokens.colors.text.primary`); the widget's own implementation never references `AppTokens`. If you want to import `AppTokens` into a `:compose-libs:*` module, the widget isn't product-agnostic — either move it to `:design-system:components`, or add a parameter (`color: Color`, `style: TextStyle`). Prefer the parameter unless the widget already has many product-specific assumptions baked in.
