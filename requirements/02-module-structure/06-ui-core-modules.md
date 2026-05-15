# `:ui-core:*` Modules

`:ui-core:*` contains the cross-cutting UI infrastructure: base classes for MVI, reusable state classes, and the error pipeline. It sits between the design system and the feature modules.

## Module list

| Module | Purpose | Convention plugins |
|---|---|---|
| `:ui-core:foundation` | `BaseViewModel`, `BaseComponent`, `BaseScreen`, `OperationManager`, `ResultManager`, platform helpers | KMP + Compose + Koin |
| `:ui-core:state` | `UiText`, `*FormatState`, `Digest*State`, `MuscleLoad*State`, `Stub*` factories, immutable UI data classes | KMP + Compose |
| `:ui-core:error:error-provider` | `AppError` sealed hierarchy + `ErrorProvider` interface + `AppErrorState` | KMP |
| `:ui-core:error:error-provider-impl` | `ErrorProviderImpl` — maps `AppError` → `AppErrorState` → `DialogConfig.ErrorDisplay` | KMP + Koin |

## `:ui-core:foundation`

The heart of MVI. See `04-base-classes/` for full API contracts. Exposes:

- `BaseViewModel<STATE, DIRECTION, LOADER>` — the only ViewModel base class.
- `BaseComponent<DIRECTION>` — Decompose component base.
- `BaseComposeScreen(background, content)` — the root composable wrapper.
- `BaseDirection`, `BaseLoader`, `BaseRouter`, `BaseResult` — marker interfaces.
- `ComponentIdentifier`, `NoneIdentifier`.
- `ResultKey<T>`, `ResultKeys` — typed cross-component result keys.
- `OperationManager` (interface, internal `OperationManagerImpl`) — `@Factory`-scoped coroutine launcher with `CoroutineExceptionHandler`.
- `ResultManager` (`@Factory`), `ResultEmitter` (`@Single`) — cross-component result bus.
- `CoreModule` — the Koin module that exposes everything above.
- `collectAsStateMultiplatform()` — `expect/actual` (`collectAsStateWithLifecycle` on Android, `collectAsState` on iOS).
- `platformAnimation()`, `platformStackAnimator()` — `expect/actual` for Decompose stack animations (iOS-like slide on iOS, fade+slide on Android).

### Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.ui.core.foundation" }

    sourceSets.commonMain.dependencies {
        implementation(projects.toolkit.logger)
        implementation(projects.uiCore.error.errorProvider)
        implementation(projects.dataServices.firebase)

        api(libs.decompose.core)
        api(libs.decompose.extensions)

        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.immutable.collections)

        implementation(compose.foundation)
    }
}
```

- Only `decompose.core` and `decompose.extensions` are `api`-exposed because public `Base*` types reference `ComponentContext`, `ChildStack`, and the Compose-flavored stack extensions. `decompose.back-handler` and `decompose.state-keeper` come in transitively from `decompose.core`/`decompose.extensions`; they're re-exported again from `:shared` for consumers that need direct access.
- `:data-services:firebase` is imported so the centralized error pipeline can call `FirebaseProvider.recordException(...)` from inside `BaseViewModel`'s exception handler.
- This module does **not** depend on `:design-system:*` — base classes never read tokens; tokens are read inside `Screen` composables that live in feature modules.

## `:ui-core:state`

Reusable UI state data classes that multiple features use, plus formatters.

Examples (each `@Immutable`):

- `UiText` — sealed (`Res`, `Str`) text type. See `11-state-and-formatters/01-ui-text.md`.
- `*FormatState` — `EmailFormatState`, `PasswordFormatState`, `WeightFormatState`, `HeightFormatState`, `DurationFormatState`, `VolumeFormatState`, `DateFormatState`, `DateRangeFormatState`. Sealed `Empty`/`Invalid`/`Valid`. See `11-state-and-formatters/02-format-state.md`.
- Product-specific reusable state: `MuscleLoadSummaryState`, `DigestState`, `TrainingStreakState`, etc. (rename / replace per product).
- Companion `stub*()` functions returning realistic preview data for each State.

Rules:

- Every class here is `@Immutable` or `@Stable`.
- Every collection is `ImmutableList`/`ImmutableSet`/`PersistentList`.
- Strings dependent on resources are stored as `UiText`, never `String`.
- Companion `stub*()` factories live next to the class — preview data is single-sourced.

### Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android { namespace = "com.<org>.<product>.ui.core.state" }

    sourceSets.commonMain.dependencies {
        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources.provider)
        implementation(projects.toolkit.dateUtils)

        implementation(compose.foundation)
        implementation(libs.datetime)
        implementation(libs.immutable.collections)
        implementation(libs.kotlinx.serialization.json)
    }
}
```

Serialization is enabled because `*FormatState` and `DateFormat` are `@Serializable` (they appear inside `*Router` and `DialogConfig`, which are persisted by Decompose `StateKeeper`).

## `:ui-core:error:error-provider`

Exposes:

- `AppError` — sealed hierarchy with `Network.{NoInternet, Timeout, Expected, Unexpected}`, `Expected`, `Unknown`.
- `AppErrorState` — corresponding sealed hierarchy for UI display.
- `ErrorProvider` — `interface { suspend fun provide(exception: Throwable, callback: () -> Unit) }`.

This module has **no DI** — only types. Consumers (`BaseViewModel`, `BackendClient`) reference these types.

### Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.ui.core.error.error.provider" }
}
```

This module is **deps-free** — it holds only pure types (`AppError`, `AppErrorState`, `ErrorProvider`). Keeping it isolated lets `:data-services:*` and `:toolkit:http-client` depend on `AppError` without dragging in any UI infrastructure. Anything more would push the dependency stack back up; resist the urge to add helpers here.

## `:ui-core:error:error-provider-impl`

Houses:

- `ErrorProviderImpl(@Single(binds = [ErrorProvider::class]))` — translates `AppError` subtypes to `AppErrorState` subtypes, then shows `DialogConfig.ErrorDisplay` via the injected `DialogController`.
- `ErrorModule` — `@Module @ComponentScan public class ErrorModule`.

Depends on `:ui-dialog-features:dialog-api` (for `DialogConfig.ErrorDisplay` and `DialogController`).

### Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.ui.core.error.error.provider.impl" }

    sourceSets.commonMain.dependencies {
        implementation(projects.uiDialogFeatures.dialogApi)
        implementation(projects.uiCore.error.errorProvider)
        implementation(projects.uiCore.state)
    }
}
```

`:ui-core:state` is included so the implementation can build `AppErrorState` instances wrapping `UiText` for localized error messages.

## Why two separate error modules

`:ui-core:error:error-provider` is pure types; anyone (including `:data-services:backend`) can depend on it without dragging in dialog infrastructure. The impl, which depends on `DialogController`, lives in a separate module so the heavy dependency stays out of the API surface.

This is the same pattern as `:design-system:resources:provider` vs `:provider-impl` and `:data-features:feature-api` vs `:data-features:<feature>`.
