# `:shared/build.gradle.kts`

The composition root's build file. The only module that:

- Applies **five** convention plugins.
- Uses the `ios.swiftpackage.convention` to assemble the XCFramework.
- Re-exports Decompose API + `:data-services:firebase` to Swift consumers.

See `02-module-structure/04-shared-composition-root.md` for `:shared`'s role.

## Full file

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("ios.swiftpackage.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android {
        namespace = "com.<org>.<product>.shared"
    }

    sourceSets.commonMain.dependencies {
        api(libs.decompose.core)
        api(libs.decompose.extensions)
        api(libs.decompose.back.handler)
        api(libs.decompose.state.keeper)

        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources.provider)
        implementation(projects.designSystem.resources.providerImpl)
        implementation(projects.designSystem.components)

        implementation(projects.toolkit.context)
        implementation(projects.toolkit.localization)
        implementation(projects.toolkit.theme)
        implementation(projects.toolkit.httpClient)
        implementation(projects.toolkit.logger)
        implementation(projects.toolkit.connectivity)
        implementation(projects.toolkit.serialization)
        implementation(projects.toolkit.dateUtils)
        implementation(projects.toolkit.imageLoader)
        implementation(projects.toolkit.linkOpener)
        implementation(projects.toolkit.notificationManager)
        implementation(projects.toolkit.permissionManager)

        implementation(projects.uiCore.foundation)
        implementation(projects.uiCore.state)
        implementation(projects.uiCore.error.errorProviderImpl)

        implementation(projects.dataServices.database)
        implementation(projects.dataServices.datastore)
        implementation(projects.dataServices.backend)
        implementation(projects.dataServices.googleAuth)
        implementation(projects.dataServices.appleAuth)
        api(projects.dataServices.firebase)

        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataFeatures.authorization)
        implementation(projects.dataFeatures.user)
        implementation(projects.dataFeatures.localSettings)
        implementation(projects.dataFeatures.notes)
        // implementation(projects.dataFeatures.<feature>) — one per per-project data feature

        implementation(projects.dataMappers.domainToState)

        implementation(projects.uiScreenFeatures.screenApi)
        implementation(projects.uiScreenFeatures.authorization)
        implementation(projects.uiScreenFeatures.profile)
        implementation(projects.uiScreenFeatures.debug)
        implementation(projects.uiScreenFeatures.home)
        implementation(projects.uiScreenFeatures.notes)
        // implementation(projects.uiScreenFeatures.<feature>) — one per top-level screen flow

        implementation(projects.uiDialogFeatures.dialogApi)
        implementation(projects.uiDialogFeatures.datePicker)
        implementation(projects.uiDialogFeatures.monthPicker)
        implementation(projects.uiDialogFeatures.periodPicker)
        implementation(projects.uiDialogFeatures.profile)
        implementation(projects.uiDialogFeatures.errorDisplay)
        implementation(projects.uiDialogFeatures.confirmation)
        implementation(projects.uiDialogFeatures.menuPicker)
        implementation(projects.uiDialogFeatures.statistics)
        implementation(projects.uiDialogFeatures.notePicker)
        // implementation(projects.uiDialogFeatures.<picker>) — one per bottom-sheet flow

        implementation(libs.datetime)
        implementation(libs.immutable.collections)

        implementation(compose.ui)
        implementation(compose.material3)
        implementation(compose.foundation)
    }
}
```

## Plugin set

| Plugin | Why |
|---|---|
| `android.library.convention` | Android target (compileSdk, minSdk, namespace) |
| `kotlin.multiplatform.convention` | KMP base (explicitApi, iOS targets, opt-ins) |
| `ios.swiftpackage.convention` | Builds `shared.xcframework` |
| `compose.multiplatform.convention` | Compose Compiler + Compose Multiplatform |
| `koin.annotation.convention` | KSP + Koin Core + Koin Annotations |

## `api` vs `implementation`

Most deps are `implementation` — internal to `:shared`. **Two are `api`**:

### Decompose

```kotlin
api(libs.decompose.core)
api(libs.decompose.extensions)
api(libs.decompose.back.handler)
api(libs.decompose.state.keeper)
```

Decompose's `ComponentContext`, `ChildStack`, `Value<T>`, `StackNavigation`, etc. appear in **`RootComponent`'s public API**. Both `:androidApp` (via `retainedComponent { RootComponent(componentContext) }`) and `:iosApp` (via the Swift bridge) need to see these types. `api` propagates them transitively.

### `:data-services:firebase`

```kotlin
api(projects.dataServices.firebase)
```

The iOS XCFramework re-exports `:data-services:firebase` so Swift code can call into the `FirebaseProvider` interface to bridge between iOS-native Firebase and the Kotlin shared layer. The convention plugin's `export(...)` line consumes this `api`:

```kotlin
// in IosSwiftPackageConventionPlugin
export(project(":data-services:firebase"))
```

## Why every module is listed

`:shared` is the composition root. To wire a Koin module into `:shared/Koin.kt`, the module must be **reachable** as a dependency. The wholesale listing of every module is **intentional**.

When adding a new module:

1. Add `implementation(projects.<group>.<name>)` in this section.
2. Add `<X>Module().module` in `:shared/Koin.kt`.

Forgetting either is a runtime failure (Koin "no definition found") or a compile failure (`:shared` can't `import com.<org>.<product>.<feature>.<X>FeatureModule`).

## Anti-patterns

- **`api` on everything.** Forbidden. Only Decompose API + `:data-services:firebase`. Adding more `api` deps creates hidden transitive coupling for `:androidApp` and `:iosApp`.
- **Single-implementation feature module missing from this list.** `Koin.init` won't see it.
- **`compose.material3` / `compose.foundation` redeclared in feature modules** that already get them via `:design-system:components`. The reference repo declares them in `:shared` because some shared Composables need them.
- **Direct `androidx-*` deps** like `androidx.activity.compose` here. Those are Android-app-only (single platform) — they go in `:androidApp`, not `:shared`.
- **Removing the explicit list** in favor of programmatic discovery. The list is deliberately explicit for auditability.
