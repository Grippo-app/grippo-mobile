# `:androidApp/build.gradle.kts`

The Android app module's `build.gradle.kts` is the **only** module that:

- Uses `android.application.convention` (not `android.library.convention`).
- Declares `applicationId`, `versionCode`, `versionName`.
- Applies Compose plugins directly (not via the multiplatform convention).
- Applies Firebase plugins (`google-services`, `firebase-crashlytics`).

## Full file

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
        applicationId = "com.<org>.<product>.android"
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true
        manifestPlaceholders["GOOGLE_SERVER_CLIENT_ID"] = "<your-oauth-server-client-id>"
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

## Plugins

| Plugin | Why |
|---|---|
| `android.application.convention` | `compileSdk`, `minSdk`, `targetSdk`, JVM toolchain |
| `compose.compiler` | Compose compiler (Kotlin's plugin) |
| `jetbrains.compose` | Compose Multiplatform plugin |
| `google.services` | Reads `google-services.json`, generates Firebase config |
| `firebase.crashlytics` | Uploads NDK symbols (optional) and integrates with Crashlytics |

Note: `:androidApp` does **not** use `kotlin.multiplatform.convention` because it's a **single-target Android** project, not a KMP module. Compose plugins are applied directly.

## `android { ... }`

### `namespace`

```kotlin
namespace = "com.<org>.<product>.android.app"
```

The package namespace for resources and BuildConfig. Distinct from `applicationId` — `namespace` is the **build-time** identifier; `applicationId` is the **runtime** identifier (used by Google Play).

### `defaultConfig`

```kotlin
defaultConfig {
    applicationId = "com.<org>.<product>.android"
    versionCode = 1
    versionName = "1.0"
    multiDexEnabled = true
    manifestPlaceholders["GOOGLE_SERVER_CLIENT_ID"] = "<your-oauth-server-client-id>"
}
```

- **`applicationId`** — the unique ID on Google Play. **Don't change** after first ship; it would orphan all existing installs.
- **`versionCode` / `versionName`** — bumped per release.
- **`multiDexEnabled = true`** — required because the dex method count exceeds 65k (Compose + Decompose + Koin + Room + Ktor).
- **`manifestPlaceholders["GOOGLE_SERVER_CLIENT_ID"]`** — injected into `AndroidManifest.xml` for Google Sign-In's `<meta-data>` element.

### `buildTypes.release`

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

### `buildTypes.debug` (implicit)

Debug builds inherit defaults from AGP — `isMinifyEnabled = false`, no shrinking, debug signing config. The reference repo doesn't explicitly configure `debug`.

## Dependencies

### `implementation(projects.shared)`

The main entry. Everything else flows from `:shared`.

### Toolkit / core deps

```kotlin
implementation(projects.uiCore.foundation)
implementation(projects.toolkit.dateUtils)
implementation(projects.toolkit.theme)
implementation(projects.toolkit.notificationManager)
implementation(projects.designSystem.core)
```

`:androidApp` reaches a handful of toolkit / core modules directly for `MainActivity` glue: configuring the theme, scheduling notifications, formatting dates in shell logic. These deps are **transitively available** via `:shared`, but listing them explicitly documents what the shell actually uses.

### Compose

```kotlin
implementation(compose.foundation)
implementation(compose.material3)
```

`compose.foundation` and `compose.material3` come from the Compose Multiplatform plugin's `compose.*` namespace — these are aliases for the Compose libraries.

### AndroidX

```kotlin
implementation(libs.androidx.activity.compose)
implementation(libs.androidx.core.splashscreen)
implementation(libs.koin.android)
```

- `activity-compose` for `setContent { ... }`.
- `core-splashscreen` for the splash screen API (Android 12+).
- `koin-android` for `androidContext(this)` extension in `Koin.init`.

### Firebase

```kotlin
implementation(projects.dataServices.firebase)
implementation(project.dependencies.platform(libs.android.firebase.bom))
implementation(libs.android.firebase.analytics)
implementation(libs.android.firebase.crashlytics)
implementation(libs.android.firebase.messaging)
```

Firebase is **Android-only**. The BOM aligns library versions. `:data-services:firebase` provides the Kotlin-side `FirebaseProvider` interface; the actual SDK libs are pulled in here so the AGP plugin (`google-services`) can wire them.

## Other files in `:androidApp`

- **`AndroidManifest.xml`** — single `<application>` themed via `@style/Theme.Grippo`, a single `MainActivity` with the default `MAIN`/`LAUNCHER` intent filter (`launchMode = singleTop`, `configChanges = keyboardHidden|orientation|screenSize`, `windowSoftInputMode = adjustPan`). The reference repo declares **no** deeplink intent filters, **no** `<uses-permission>` entries, and **no** `<meta-data>` (the `GOOGLE_SERVER_CLIENT_ID` `manifestPlaceholder` is reserved for a future Google Sign-In `<meta-data>` element but is currently unused inside the manifest).
- **`google-services.json`** — Firebase config. **Per-environment** (debug/release/staging); typically gitignored except for prod.
- **`proguard-rules.pro`** — R8 rules. Common entries: keep Decompose's reflection, keep Koin's annotations, keep kotlinx-serialization generated classes.
- **`src/main/java/com/<org>/<product>/android/`** — `App.kt`, `MainActivity.kt`.
- **`src/main/res/`** — Android-only resources (launcher icons, splash, themes.xml).

## Anti-patterns

- **Using `android.library.convention`.** `:androidApp` is an application, not a library.
- **Putting business logic in `:androidApp`.** It's a shell.
- **`implementation(libs.<something for the shared layer>)`** — those live in `:shared` and flow up transitively.
- **`api(projects.shared)`.** `:androidApp` doesn't export anything; `implementation` is correct.
- **Multiple Activities.** Single Activity hosting Decompose root.
- **Changing `applicationId` post-launch.** Orphans existing installs.
- **Committing the prod `google-services.json`** without a security review. Most teams gitignore Firebase configs and inject per-environment via CI.
