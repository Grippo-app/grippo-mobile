# Module build.gradle.kts — :androidApp, :shared, representative shapes

Self-contained reference for the per-module `build.gradle.kts` of the Android app shell, the `:shared` composition root, and the representative build-file shapes for every module category.

## :androidApp module build

The Android app module's `build.gradle.kts` is the **only** module that:

- Uses `android.application.convention` (not `android.library.convention`).
- Declares `applicationId`, `versionCode`, `versionName`.
- Applies Compose plugins directly (not via the multiplatform convention).
- Applies Firebase plugins (`google-services`, `firebase-crashlytics`).

### Full file

```kotlin
plugins {
    id("android.application.convention")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.<org>.<product>.android.app"

    defaultConfig {
        applicationId = "com.<org>.<product>"
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true
        manifestPlaceholders["GOOGLE_SERVER_CLIENT_ID"] = "<oauth-server-client-id>"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            val default = getDefaultProguardFile("proguard-android-optimize.txt")
            proguardFiles(default, "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(projects.shared)
    implementation(projects.uiCore.foundation)
    implementation(projects.toolkit.dateUtils)
    implementation(projects.toolkit.theme)
    implementation(projects.toolkit.notificationManager)
    implementation(projects.designSystem.core)

    implementation(compose.foundation)
    implementation(compose.material3)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.koin.android)

    // Firebase
    implementation(projects.dataServices.firebase)
    implementation(project.dependencies.platform(libs.android.firebase.bom))
    implementation(libs.android.firebase.analytics)
    implementation(libs.android.firebase.crashlytics)
    implementation(libs.android.firebase.messaging)
}
```

### Plugins

| Plugin | Why |
|---|---|
| `android.application.convention` | `compileSdk`, `minSdk`, `targetSdk`, JVM toolchain |
| `compose.compiler` | Compose compiler (Kotlin's plugin) |
| `jetbrains.compose` | Compose Multiplatform plugin |
| `google.services` | Reads `google-services.json`, generates Firebase config |
| `firebase.crashlytics` | Uploads NDK symbols (optional) and integrates with Crashlytics |

Note: `:androidApp` does **not** use `kotlin.multiplatform.convention` because it's a **single-target Android** project, not a KMP module. Compose plugins are applied directly.

### `android { ... }`

#### `namespace`

```kotlin
namespace = "com.<org>.<product>.android.app"
```

The package namespace for resources and BuildConfig. Distinct from `applicationId` — `namespace` is the **build-time** identifier; `applicationId` is the **runtime** identifier (used by Google Play).

#### `defaultConfig`

```kotlin
defaultConfig {
    applicationId = "com.<org>.<product>"
    versionCode = 1
    versionName = "1.0"
    multiDexEnabled = true
    manifestPlaceholders["GOOGLE_SERVER_CLIENT_ID"] = "<oauth-server-client-id>"
}
```

- **`applicationId`** — the unique ID on Google Play. **Don't change** after first ship; it would orphan all existing installs.
- **`versionCode` / `versionName`** — bumped per release.
- **`multiDexEnabled = true`** — required because the dex method count exceeds 65k (Compose + Decompose + Koin + Room + Ktor).
- **`manifestPlaceholders["GOOGLE_SERVER_CLIENT_ID"]`** — defines a manifest placeholder reserved for a future Google Sign-In `<meta-data>` element; it is not yet consumed inside `AndroidManifest.xml` (see "Other files in `:androidApp`" below).

#### `buildTypes.release`

```kotlin
release {
    isMinifyEnabled = true
    isShrinkResources = true
    val default = getDefaultProguardFile("proguard-android-optimize.txt")
    proguardFiles(default, "proguard-rules.pro")
}
```

- **`isMinifyEnabled = true`** — R8 code shrinking.
- **`isShrinkResources = true`** — strips unused resources.
- **`proguardFiles(...)`** — the default Android optimize config + project-specific rules in `proguard-rules.pro`.

#### `buildTypes.debug` (implicit)

Debug builds inherit defaults from AGP — `isMinifyEnabled = false`, no shrinking, debug signing config. This template doesn't explicitly configure `debug`.

### Dependencies

#### `implementation(projects.shared)`

The main entry. Everything else flows from `:shared`.

#### Toolkit / core deps

```kotlin
implementation(projects.uiCore.foundation)
implementation(projects.toolkit.dateUtils)
implementation(projects.toolkit.theme)
implementation(projects.toolkit.notificationManager)
implementation(projects.designSystem.core)
```

`:androidApp` reaches a handful of toolkit / core modules directly for `MainActivity` glue: configuring the theme, scheduling notifications, formatting dates in shell logic. These deps are **transitively available** via `:shared`, but listing them explicitly documents what the shell actually uses.

#### Compose

```kotlin
implementation(compose.foundation)
implementation(compose.material3)
```

`compose.foundation` and `compose.material3` come from the Compose Multiplatform plugin's `compose.*` namespace — these are aliases for the Compose libraries.

#### AndroidX

```kotlin
implementation(libs.androidx.activity.compose)
implementation(libs.androidx.core.splashscreen)
implementation(libs.koin.android)
```

- `activity-compose` for `setContent { ... }`.
- `core-splashscreen` for the splash screen API (Android 12+).
- `koin-android` for `androidContext(this)` extension in `Koin.init`.

#### Firebase

```kotlin
implementation(projects.dataServices.firebase)
implementation(project.dependencies.platform(libs.android.firebase.bom))
implementation(libs.android.firebase.analytics)
implementation(libs.android.firebase.crashlytics)
implementation(libs.android.firebase.messaging)
```

Firebase is **Android-only**. The BOM aligns library versions. `:data-services:firebase` provides the Kotlin-side `FirebaseProvider` interface; the actual SDK libs are pulled in here so the AGP plugin (`google-services`) can wire them.

### Other files in `:androidApp`

- **`AndroidManifest.xml`** — single `<application>` themed via `@style/Theme.<Product>`, a single `MainActivity` with the default `MAIN`/`LAUNCHER` intent filter (`launchMode = singleTop`, `configChanges = keyboardHidden|orientation|screenSize`, `windowSoftInputMode = adjustPan`). This template declares **no** deeplink intent filters, **no** `<uses-permission>` entries, and **no** `<meta-data>` (the `GOOGLE_SERVER_CLIENT_ID` `manifestPlaceholder` is reserved for a future Google Sign-In `<meta-data>` element but is currently unused inside the manifest).
- **`google-services.json`** — Firebase config. **Per-environment** (debug/release/staging); typically gitignored except for prod.
- **`proguard-rules.pro`** — R8 rules. Common entries: keep Decompose's reflection, keep Koin's annotations, keep kotlinx-serialization generated classes.
- **`src/main/java/com/<org>/<product>/android/`** — `App.kt`, `MainActivity.kt`.
- **`src/main/res/`** — Android-only resources (launcher icons, splash, themes.xml).

### Anti-patterns

- **Using `android.library.convention`.** `:androidApp` is an application, not a library.
- **Putting business logic in `:androidApp`.** It's a shell.
- **`implementation(libs.<shared-module>)`** — those live in `:shared` and flow up transitively.
- **`api(projects.shared)`.** `:androidApp` doesn't export anything; `implementation` is correct.
- **Multiple Activities.** Single Activity hosting Decompose root.
- **Changing `applicationId` post-launch.** Orphans existing installs.
- **Committing the prod `google-services.json`** without a security review. Most teams gitignore Firebase configs and inject per-environment via CI.

## :shared module build

The composition root's build file. The only module that:

- Applies **five** convention plugins.
- Uses the `ios.swiftpackage.convention` to assemble the XCFramework.
- Re-exports Decompose API + `:data-services:firebase` to Swift consumers.

### Full file

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
        // region firebase-conditional (firebaseEnabled = true only)
        api(projects.dataServices.firebase)
        // endregion firebase-conditional

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

### Plugin set

| Plugin | Why |
|---|---|
| `android.library.convention` | Android target (compileSdk, minSdk, namespace) |
| `kotlin.multiplatform.convention` | KMP base (explicitApi, iOS targets, opt-ins) |
| `ios.swiftpackage.convention` | Builds `shared.xcframework` |
| `compose.multiplatform.convention` | Compose Compiler + Compose Multiplatform |
| `koin.annotation.convention` | KSP + Koin Core + Koin Annotations |

### `api` vs `implementation`

Most deps are `implementation` — internal to `:shared`. **Two groups are `api`**:

#### Decompose

```kotlin
api(libs.decompose.core)
api(libs.decompose.extensions)
api(libs.decompose.back.handler)
api(libs.decompose.state.keeper)
```

Decompose's `ComponentContext`, `ChildStack`, `Value<T>`, `StackNavigation`, etc. appear in **`RootComponent`'s public API**. Both `:androidApp` (via `retainedComponent { RootComponent(componentContext) }`) and `:iosApp` (via the Swift bridge) need to see these types. `api` propagates them transitively.

#### `:data-services:firebase`

```kotlin
// region firebase-conditional (firebaseEnabled = true only)
api(projects.dataServices.firebase)
// endregion firebase-conditional
```

The iOS XCFramework re-exports `:data-services:firebase` so Swift code can call into the `FirebaseProvider` interface to bridge between iOS-native Firebase and the Kotlin shared layer. The convention plugin's `export(...)` line consumes this `api`:

```kotlin
// in IosSwiftPackageConventionPlugin
export(project(":data-services:firebase"))
```

### Anti-patterns

- **`api` on everything.** Forbidden. Only Decompose API + `:data-services:firebase`. Adding more `api` deps creates hidden transitive coupling for `:androidApp` and `:iosApp`.
- **Single-implementation feature module missing from this list.** `Koin.init` won't see it.
- **`compose.material3` / `compose.foundation` redeclared in feature modules** that already get them via `:design-system:components`. This template declares them in `:shared` because some shared Composables need them.
- **Direct `androidx-*` deps** like `androidx.activity.compose` here. Those are Android-app-only (single platform) — they go in `:androidApp`, not `:shared`.
- **Removing the explicit list** in favor of programmatic discovery. The list is deliberately explicit for auditability.

## Representative build.gradle.kts shapes

A `build.gradle.kts` template per module category. Use these as the starting point for new modules.

### Pure KMP module (no UI, no Koin, e.g. a small toolkit)

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.toolkit.<name>" }

    sourceSets.commonMain.dependencies {
        // minimal — only the libraries this toolkit module wraps
        implementation(libs.kotlinx.coroutines.core)
    }
}
```

### KMP module with Koin DI (e.g. a `:data-features:<feature>`)

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.data.features.<feature>" }

    sourceSets.commonMain.dependencies {
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataServices.database)
        implementation(projects.dataServices.backend)
        implementation(projects.dataMappers.dtoToEntity)
        implementation(projects.dataMappers.entityToDomain)
        implementation(projects.dataMappers.domainToDto)
        implementation(projects.toolkit.dateUtils)

        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.datetime)
    }
}
```

### KMP module with Compose UI (no DI, e.g. `:design-system:components`)

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.design.system.components" }

    sourceSets.commonMain.dependencies {
        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources.provider)
        implementation(projects.designSystem.preview)

        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(libs.immutable.collections)
    }
}
```

> **Special case — `:design-system:resources:provider`.** This template covers `:design-system:components` (Compose UI, **no** `composeResources`). The resources-provider uses the same plugin set but **additionally** requires `androidLibrary { androidResources.enable = true }` inside `kotlin { ... }` (and the `compose.resources { ... }` block). Without it, Compose `composeResources` (`.cvr`) are not packaged into the APK on AGP 9 + `com.android.kotlin.multiplatform.library`, and the app crashes at runtime with `MissingResourceException`. Do **not** start the provider from this template — use the full build script in the design-system skill, references/design-system-modules.md § `:design-system:resources:provider`. The same opt-in applies to any module that ships `composeResources/` or androidMain `res/` (e.g. `:toolkit:notification-manager`). See the design-system skill, references/resources.md § Build requirement for the full rationale.

### KMP module with Compose UI and Koin (e.g. `:ui-screen-features:<feature>`)

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.ui.screen.features.<feature>" }

    sourceSets.commonMain.dependencies {
        implementation(projects.uiCore.foundation)
        implementation(projects.uiCore.state)
        implementation(projects.uiScreenFeatures.screenApi)
        implementation(projects.uiDialogFeatures.dialogApi)
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataMappers.domainToState)
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

### Dialog feature module (e.g. `:ui-dialog-features:<picker>`)

Same as the screen feature but typically without `:data-features:feature-api`:

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.ui.dialog.features.<feature>" }

    sourceSets.commonMain.dependencies {
        implementation(projects.uiCore.foundation)
        implementation(projects.uiCore.state)
        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources.provider)
        implementation(projects.designSystem.components)
        implementation(projects.designSystem.preview)
        implementation(projects.composeLibs.wheelPicker)   // if it's a picker that uses the wheel

        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(libs.immutable.collections)
    }
}
```

### Data service module with `@Serializable` types (e.g. `:data-services:backend`)

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android { namespace = "com.<org>.<product>.data.services.backend" }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.toolkit.serialization)
            implementation(projects.toolkit.httpClient)
            implementation(projects.toolkit.logger)
            implementation(projects.toolkit.localization)
            implementation(projects.dataServices.database)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.auth)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.content.negotiation)
        }
    }
}
```

### Data mapper module (e.g. `:data-mappers:dto-to-entity`)

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.data.mappers.dto.to.entity" }

    sourceSets.commonMain.dependencies {
        implementation(projects.dataServices.database)
        implementation(projects.dataServices.backend)
        implementation(projects.toolkit.logger)
    }
}
```

### Toolkit `date-utils` module (Compose + Serialization)

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android { namespace = "com.<org>.<product>.toolkit.date.utils" }

    sourceSets.commonMain.dependencies {
        implementation(projects.designSystem.resources.provider)
        implementation(projects.designSystem.core)
        implementation(projects.toolkit.logger)

        implementation(compose.foundation)
        implementation(libs.datetime)
        implementation(libs.kotlinx.serialization.json)
    }
}
```

### Room database module (`:data-services:database`)

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("room.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.data.services.database" }

    sourceSets.commonMain.dependencies {
        implementation(projects.toolkit.context)
        implementation(projects.toolkit.logger)
        implementation(projects.uiCore.error.errorProvider)
    }
}
```

The `room.convention` plugin handles all Room+KSP wiring for Android and every iOS target.

### Compose-libs widget module (e.g. `:compose-libs:wheel-picker`)

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.compose.libs.<name>" }

    sourceSets.commonMain.dependencies {
        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(libs.immutable.collections)
    }
}
```

No Koin. No `:design-system:*`. No `:toolkit:*` (except trivial cases). Self-contained widget.

### Patterns

#### Module-level `android { namespace = ... }`

```kotlin
kotlin {
    android { namespace = "com.<org>.<product>.<group>.<name>" }
}
```

The `kotlin { android { ... } }` block is the KMP-style configuration. The convention plugin's default `namespace = "com.<org>.<product>"` is overridden here with the full per-module namespace.

#### Source-set grouping

For platform-specific deps:

```kotlin
sourceSets {
    commonMain.dependencies { ... }
    androidMain.dependencies {
        implementation(libs.androidx.appcompat)
    }
    iosMain.dependencies {
        // iosMain isn't a direct source set; iOS targets inherit
    }
}
```

Most modules only need `commonMain.dependencies`. Android-specific deps go in `androidMain.dependencies`. iOS deps usually come via the platform engine (e.g. `ktor-client-darwin` in `iosMain.dependencies` for `:toolkit:http-client`).

### Test-bearing module (any kind)

A module that owns tests adds the opt-in test conventions on top of its normal
plugin set — nothing else changes in the build script:

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    // opt-in test foundation: pick exactly the capabilities the tests use;
    // each capability plugin applies kmp.test.convention itself.
    id("coroutines.test.convention")   // runTest/virtual time in commonTest
    id("network.test.convention")      // MockEngine in commonTest
}

kotlin {
    android { namespace = "com.<org>.<product>.data.features.<feature>" }

    sourceSets.commonMain.dependencies { /* production deps */ }
    // Test dependencies NEVER appear here by hand — capabilities own them.
}
```

Source layout (frozen names): general tests in `src/commonTest/kotlin`,
host-only tests (Koin `verify()`, Robolectric entries) in
`src/androidHostTest/kotlin`, shared Compose UI scenarios in
`src/uiTest/kotlin` (created by `compose.ui.test.convention`), iOS entries in
`src/iosSimulatorArm64Test/kotlin`, device-only instrumented tests in
`src/androidDeviceTest/kotlin` (standalone compilation — it does not see
`commonTest`). Run lanes through the module aliases (`hostTests`,
`iosSimulatorTests`, `androidDeviceTests`) or the root aggregates — never
through guessed AGP/KGP task names.

### Per-module checklist

When you create a new module:

- [ ] Module directory exists under the right group.
- [ ] `build.gradle.kts` uses the right convention plugins.
- [ ] `kotlin { android { namespace = "..." } }` is set to the full namespace.
- [ ] If the module ships `composeResources/` or androidMain `res/`, `kotlin { androidLibrary { androidResources.enable = true } }` is set — otherwise its resources aren't packaged into the APK (runtime `MissingResourceException` on AGP 9). Today: `:design-system:resources:provider`, `:toolkit:notification-manager`.
- [ ] `sourceSets.commonMain.dependencies { ... }` lists only what the module actually uses.
- [ ] No redundant deps.
- [ ] No `repositories { ... }` block (forbidden by `FAIL_ON_PROJECT_REPOS`).
- [ ] No version literals (`"2.3.21"`). Use the catalog (`libs.kotlin.gradle.plugin`).
- [ ] Module added to `settings.gradle.kts`.
- [ ] Test conventions only if the module owns tests (`kmp.test.convention` is opt-in, never transitive); test dependencies arrive only through `*.test.convention` capabilities.
- [ ] No `withHostTest`/`withDeviceTest` call in the module script — the single owners live in build-logic.
- [ ] Compose module never enables the Android device lane (CMP 1.10.3 fails its `deviceTest` resources task at configuration).
- [ ] If declares Koin module, added to `:shared/Koin.kt`.
- [ ] If consumed by `:shared`, added to `:shared/build.gradle.kts`.

### Anti-patterns

- **`apply(plugin = "...")`** instead of `plugins { id("...") }`. Use the `plugins {}` DSL.
- **Inline version literals**: `implementation("org.foo:bar:1.0.0")`. Use the catalog.
- **`api(...)` everywhere.** Default is `implementation`; `api` only for genuine API exposure.
- **Compose plugins applied directly** in a module that uses Compose. Use `compose.multiplatform.convention`.
- **Kotlin serialization plugin without `@Serializable` types** in the module. Don't apply the plugin if it's unused.
- **`kotlin { jvmToolchain(19) }`** in a module. Already in the convention.
- **`compileSdk = 36`** in a module. Already in the convention.
