# `:ui-screen-features:*` and `:ui-dialog-features:*`

These two module groups hold the actual product UI. Both follow the same MVI seven-file pattern; the difference is the **host**: screen features render inside `RootComponent`'s `ChildStack`; dialog features render inside `DialogComponent`'s `ChildSlot`.

## `:ui-screen-features:*`

### `:ui-screen-features:screen-api`

The contract module. Houses **only**:

- `<Feature>Router` sealed classes — `@Serializable public sealed class HomeRouter : BaseRouter { ... }`. Defines all sub-routes within a feature **plus** any cross-feature routes the root navigator must know about.
- `Deeplink` enum — `enum class Deeplink(val key: String) { ... ; companion object { fun fromKey(key: String): Deeplink? = entries.firstOrNull { it.key == key } } }`.
- Result protocol types nested inside `Router.<Screen>.Action` — see `03-architecture-patterns/04-cross-component-results.md`.

This module is `public` API. UI screen features depend on it; `:shared` depends on it.

#### Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android { namespace = "com.<org>.<product>.ui.screen.features.screen.api" }

    sourceSets.commonMain.dependencies {
        implementation(projects.uiCore.foundation)
        implementation(projects.uiCore.state)
        implementation(projects.toolkit.dateUtils)
        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources.provider)

        implementation(libs.kotlinx.serialization.json)
        implementation(libs.datetime)
        implementation(libs.immutable.collections)

        implementation(compose.foundation)
    }
}
```

Notes:
- The `compose.multiplatform.convention` plugin is applied because `Router.<Screen>.Action` types may carry Compose-friendly payloads (`ImmutableList`, etc.) declared in design-system / state modules.
- `:design-system:core` and `:design-system:resources:provider` are included so router payloads can reference `Res.*` keys when needed (e.g. preconfigured deeplink labels).

### `:ui-screen-features:<feature>`

One module per top-level feature flow (`home`, `profile`, `authorization`, `training`, `trainings`, `debug`, etc.).

Each feature module owns:

- An `<Feature>RootComponent` — has its own `StackNavigation<<Feature>Router>` and routes among its sub-screens.
- One package per sub-screen (`com.<org>.<product>.<feature>.<subscreen>`) containing the **seven MVI files** (`Component`, `Contract`, `State`, `Direction`, `Loader`, `ViewModel`, `Screen`).
- An optional `<Feature>RootScreen` if the feature is the only renderer; otherwise the root component's `Render()` delegates to per-sub-screen `Render()` via `ChildStack`.

#### Build (typical)

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.ui.screen.<feature>" }

    sourceSets.commonMain.dependencies {
        implementation(projects.uiCore.foundation)
        implementation(projects.uiCore.state)
        implementation(projects.dataMappers.domainToState)
        implementation(projects.uiDialogFeatures.dialogApi)
        implementation(projects.uiScreenFeatures.screenApi)
        implementation(projects.dataFeatures.featureApi)        // ONLY data-layer dep
        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources.provider)
        implementation(projects.designSystem.components)
        implementation(projects.designSystem.preview)
        implementation(projects.toolkit.dateUtils)

        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(libs.immutable.collections)
        implementation(libs.datetime)
    }
}
```

Mandatory deps for any screen feature: `:ui-core:foundation`, `:ui-screen-features:screen-api`, `:design-system:core/components/resources/preview`, `:data-features:feature-api`, `:ui-dialog-features:dialog-api`.

Note: `compose-libs:*` modules (e.g. `:compose-libs:chart`) are imported only by features that actually use them.

## `:ui-dialog-features:*`

### `:ui-dialog-features:dialog-api`

The contract module for bottom sheets.

Houses:

- `DialogConfig` — `@Serializable sealed class DialogConfig(val onDismiss: (() -> Unit)? = null, val dismissBySwipe: Boolean = true) { abstract val key: String }`. Each concrete config (`WeightPicker`, `Confirmation`, `ErrorDisplay`, ...) is a subclass with a typed `onResult: (T) -> Unit` parameter marked `@Transient`.
- `DialogController` — `interface { fun show(config: DialogConfig); fun dismiss() }`. Injected into ViewModels via Koin.
- `DialogProvider` — interface implemented by `DialogComponent` to observe the slot.
- `DialogModule` — `@Module @ComponentScan public class DialogModule` (provides `DialogController` implementation, normally living in `:shared` and bound via Koin).
- The `buildKey(...)` helper for config keys: length-prefixed `${value.length}:${value}|...`.

#### Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android { namespace = "com.<org>.<product>.ui.dialog.features.dialog.api" }

    sourceSets.commonMain.dependencies {
        implementation(projects.uiCore.foundation)
        implementation(projects.uiCore.state)
        implementation(projects.toolkit.dateUtils)

        implementation(libs.kotlinx.serialization.json)
        implementation(libs.datetime)

        implementation(compose.foundation)
    }
}
```

`compose.multiplatform.convention` is required because `DialogConfig.ErrorDisplay` (and other configs) reference `@Composable`-aware state types from `:ui-core:state`. The `AppErrorState` type itself is brought in transitively through `:ui-core:foundation` / `:ui-core:state`; explicit `errorProvider` import is unnecessary at the API surface.

### `:ui-dialog-features:<dialog>`

One module per bottom-sheet flow (`weight-picker`, `confirmation`, `error-display`, `iteration-picker`, `month-picker`, `period-picker`, etc.).

Each module looks **identical** to a screen feature except:

- The Component's constructor parameters typically include `(componentContext, initial: <Type>, onResult: (<Type>) -> Unit, back: () -> Unit, close: () -> Unit)` instead of `(componentContext, back, ...)`.
- `BottomSheetToolbar` is used instead of `Toolbar`.
- The dialog's content is rendered inside an `ModalBottomSheet` (managed by `DialogComponent`, not by the feature itself).
- The dialog feature **never** holds a `StackNavigation` of its own. In-sheet multi-step flows use an internal stack maintained in `State` (push/pop without dismissing the sheet), not Decompose.

#### Build (typical)

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.ui.dialog.<feature>" }

    sourceSets.commonMain.dependencies {
        implementation(projects.uiCore.foundation)
        implementation(projects.uiCore.state)
        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources.provider)
        implementation(projects.designSystem.components)
        implementation(projects.designSystem.preview)
        // pickers may also use :compose-libs:wheel-picker etc.

        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(libs.immutable.collections)
    }
}
```

Note: dialog features do **not** depend on `:data-features:feature-api` unless they need to read or write domain data themselves (rare; most dialogs are pure pickers and return a value to the caller).

## File layout for a feature

### Screen feature

```
ui-screen-features/profile/
  build.gradle.kts
  src/commonMain/kotlin/com/<org>/<product>/profile/
    ProfileRootComponent.kt
    ProfileRootScreen.kt
    body/                         // sub-screen "body"
      ProfileBodyComponent.kt
      ProfileBodyContract.kt
      ProfileBodyState.kt
      ProfileBodyDirection.kt
      ProfileBodyLoader.kt
      ProfileBodyViewModel.kt
      ProfileBodyScreen.kt
    settings/
      ProfileSettingsComponent.kt
      ProfileSettingsContract.kt
      // ... seven files
    workouthistory/
      // ... seven files
```

### Dialog feature

```
ui-dialog-features/weight-picker/
  build.gradle.kts
  src/commonMain/kotlin/com/<org>/<product>/weight/picker/
    WeightPickerComponent.kt
    WeightPickerContract.kt
    WeightPickerState.kt
    WeightPickerDirection.kt      // Back, Close only — no nav
    WeightPickerLoader.kt         // usually empty
    WeightPickerViewModel.kt
    WeightPickerScreen.kt
```

## Rules summary

- One module per feature. **Do not** put two unrelated features in the same module.
- One package per sub-screen / sub-dialog. Each package has exactly the seven files.
- Sub-screens are `internal` to the feature module. Only the root component is `internal` and only `:shared` (composition root) consumes it.
- A screen feature exposes its public routes via `:ui-screen-features:screen-api`. Direct symbol imports between feature modules are forbidden.
- A dialog feature exposes its config via `:ui-dialog-features:dialog-api`. Same rule.
- A feature module may depend on `:compose-libs:*` for specialized widgets (charts, konfetti, wheel-picker, segment-control). It must **not** depend on another feature module.
