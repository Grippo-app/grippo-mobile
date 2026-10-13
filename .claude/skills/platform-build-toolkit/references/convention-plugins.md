# Convention plugins (build-logic)

The sources below describe the bootstrap template. In this existing mobile
checkout, actual `build-logic` plugins and `CLAUDE.md` take precedence. The project
has no automated test infrastructure; do not introduce it without an explicit
user request.

Every module's `build.gradle.kts` is **declarative**: only `plugins { id("...convention") }` and `kotlin { ... }`. All configuration lives in convention plugins under `:build-logic:convention`. This reference is the self-contained spec for that wiring — the plugin matrix, each convention plugin's verbatim Kotlin, the build-logic helpers, the minimal module-script shape, and the anti-patterns.

## Plugin matrix

| Module type | Plugins to apply |
|---|---|
| `:androidApp` | `android.application.convention` + `compose.compiler` + `jetbrains.compose` + `google-services` + `firebase-crashlytics` |
| Pure KMP module (no UI) | `android.library.convention` + `kotlin.multiplatform.convention` |
| KMP module with Compose UI | `android.library.convention` + `kotlin.multiplatform.convention` + `compose.multiplatform.convention` |
| KMP module declaring Koin DI | + `koin.annotation.convention` |
| `:data-services:database` | + `room.convention` |
| `:shared` | all five: `android.library.convention` + `kotlin.multiplatform.convention` + `ios.swiftpackage.convention` + `compose.multiplatform.convention` + `koin.annotation.convention` |
| Module declaring `@Serializable` types | + `alias(libs.plugins.kotlin.serialization)` |

## Function-body style note

The examples below use the **expression-body** form
(`override fun apply(target: Project) = with(target) { ... }`) consistently for
readability. A block body is functionally equivalent; choose one style and keep
the generated build logic consistent.

## The seven plugins

### `KotlinMultiplatformConventionPlugin`

```kotlin
class KotlinMultiplatformConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.applySafely("org.jetbrains.kotlin.multiplatform")

        extensions.getByType<KotlinMultiplatformExtension>().apply {
            explicitApi()

            iosArm64()
            iosSimulatorArm64()

            applyDefaultHierarchyTemplate()

            sourceSets.configureEach {
                languageSettings.apply {
                    optIn("androidx.compose.material3.ExperimentalMaterial3Api")
                    optIn("androidx.compose.ui.text.ExperimentalTextApi")
                    optIn("androidx.compose.foundation.ExperimentalFoundationApi")
                    optIn("androidx.compose.ui.ExperimentalComposeUiApi")
                    optIn("androidx.compose.foundation.layout.ExperimentalLayoutApi")
                    optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                    optIn("androidx.compose.ui.unit.ExperimentalUnitApi")
                    optIn("androidx.compose.animation.ExperimentalAnimationApi")
                    optIn("kotlin.time.ExperimentalTime")
                    optIn("kotlinx.cinterop.ExperimentalForeignApi")
                    optIn("com.arkivanov.decompose.DelicateDecomposeApi")
                    optIn("androidx.compose.animation.ExperimentalSharedTransitionApi")
                    optIn("kotlin.uuid.ExperimentalUuidApi")
                    optIn("com.arkivanov.decompose.ExperimentalDecomposeApi")
                }
            }
        }
    }
}
```

- **`explicitApi()`** — every public declaration must have explicit visibility.
- **iOS targets** registered up-front. `applyDefaultHierarchyTemplate()` sets up the standard source-set hierarchy (`commonMain`, `iosMain`, ...).
- **Global `optIn`** — the list of experimental APIs every module uses. Don't repeat `@OptIn` in source files.

### `AndroidLibraryConventionPlugin`

```kotlin
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.applySafely("com.android.kotlin.multiplatform.library")

        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            extensions.configure<KotlinMultiplatformExtension> {
                targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                    compileSdk = 37
                    minSdk = 26
                    namespace = "com.<org>.<product>"
                }
            }
        }

        configureJvmToolchain(19)
    }
}
```

- Applies `com.android.kotlin.multiplatform.library` (the KMP-aware Android plugin).
- `compileSdk = 37`, `minSdk = 26`.
- `namespace = "com.<org>.<product>"` — the namespace **prefix**. Every module-level `build.gradle.kts` **must** override this with its full, unique namespace (`kotlin { android { namespace = "..." } }`); omitting the override leaves multiple modules sharing the bare prefix, causing duplicate-namespace/resource-merge build failures.
- JVM toolchain 19.

### `AndroidApplicationConventionPlugin`

```kotlin
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.applySafely("com.android.application")

        extensions.configure<ApplicationExtension> {
            compileSdk = 37
            namespace = "com.<org>.<product>"

            defaultConfig {
                minSdk = 26
                targetSdk = 36
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_19
                targetCompatibility = JavaVersion.VERSION_19
            }
        }

        configureJvmToolchain(19)
    }
}
```

- For `:androidApp` only.
- `applicationId`, `versionCode`, `versionName`, build types — set in `:androidApp/build.gradle.kts` (per-app, not shared).

### `ComposeMultiplatformConventionPlugin`

```kotlin
class ComposeMultiplatformConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.applySafely("org.jetbrains.kotlin.plugin.compose")
        pluginManager.applySafely("org.jetbrains.compose")

        pluginManager.withPlugin("org.jetbrains.kotlin.plugin.compose") {
            extensions.configure<ComposeCompilerGradlePluginExtension> {
                metricsDestination.set(layout.buildDirectory.dir("compose-metrics"))
                reportsDestination.set(layout.buildDirectory.dir("compose-reports"))
            }
        }

        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            extensions.configure<KotlinMultiplatformExtension> {
                targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                    packaging.resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
                }
            }
        }
    }
}
```

- Applies the **Compose Compiler** plugin (Kotlin's official plugin, version-aligned with Kotlin).
- Applies **Jetbrains Compose** (the Multiplatform Compose plugin).
- **Metrics + reports** — Compose Compiler emits `compose-metrics/` and `compose-reports/` in each module's `build/`. Used for stability analysis ("which Composables are non-skippable?").
- Stability is inferred from code annotations and types. No external stability configuration is registered.

### `KoinAnnotationConventionPlugin`

```kotlin
class KoinAnnotationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.applySafely("com.google.devtools.ksp")

        val kotlinExt = extensions.getByType<KotlinMultiplatformExtension>()

        kotlinExt.sourceSets.named("commonMain").configure {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
            dependencies {
                implementation(libs.findLibrary("koin.core").get())
                api(libs.findLibrary("koin.annotations").get())
            }
        }

        dependencies {
            add("kspCommonMainMetadata", libs.findLibrary("koin.ksp.compiler").get())
        }

        extensions.getByType<KspExtension>().apply {
            arg("KOIN_CONFIG_CHECK", "false")
        }

        project.afterEvaluate {
            tasks.withType(KspAATask::class.java).configureEach {
                if (name != "kspCommonMainKotlinMetadata") {
                    dependsOn("kspCommonMainKotlinMetadata")
                }
            }
        }
    }
}
```

- Applies **KSP**.
- Registers Koin + Koin Annotations as common dependencies.
- Adds the **KSP compiler** to `kspCommonMainMetadata` (the shared metadata target).
- Sets `KOIN_CONFIG_CHECK = false` (Koin Annotations 2.3.1 sanity-check workaround).
- Makes platform KSP tasks (`kspAndroid`, `kspIosArm64`, `kspIosSimulatorArm64`) depend on the common metadata task.

### `RoomConventionPlugin`

```kotlin
class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.applySafely("com.google.devtools.ksp")

        val kotlinExt = extensions.getByType<KotlinMultiplatformExtension>()

        kotlinExt.sourceSets.named("commonMain") {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
            dependencies {
                implementation(libs.findLibrary("androidx.room.runtime").get())
                implementation(libs.findLibrary("sqlite.bundled").get())
                implementation(libs.findLibrary("sqlite").get())
            }
        }

        dependencies {
            add("kspAndroid", libs.findLibrary("androidx.room.compiler").get())
            add("kspIosArm64", libs.findLibrary("androidx.room.compiler").get())
            add("kspIosSimulatorArm64", libs.findLibrary("androidx.room.compiler").get())
        }

        extensions.configure<KspExtension> {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        project.afterEvaluate {
            tasks.withType(KspAATask::class.java).configureEach {
                if (name != "kspCommonMainKotlinMetadata") {
                    dependsOn("kspCommonMainKotlinMetadata")
                }
            }
        }
    }
}
```

- For `:data-services:database` only.
- Adds the **Room compiler** to KSP for Android **and all iOS targets**. Room generates platform-specific code per target.
- `room.schemaLocation = $projectDir/schemas` — JSON schemas are exported here per database version.

### `IosSwiftPackageConventionPlugin`

`IosSwiftPackageConventionPlugin` — see the iOS Swift-package spec for the canonical source and the `<iosFrameworkName>` substitution rule.

## Helpers (in `build-logic/convention/src/main/kotlin/com/<org>`)

### `PluginManagerExtensions.kt`

```kotlin
internal fun PluginManager.applySafely(pluginId: String) {
    if (!hasPlugin(pluginId)) {
        apply(pluginId)
    }
}
```

Avoids double-applying when multiple convention plugins all want, e.g., `kotlin.multiplatform`.

### `ConfigureJvmToolchain.kt`

```kotlin
fun Project.configureJvmToolchain(version: Int) {
    val kotlinExt = extensions.findByName("kotlin")
    if (kotlinExt is KotlinJvmProjectExtension ||
        kotlinExt is KotlinAndroidProjectExtension ||
        kotlinExt is KotlinMultiplatformExtension
    ) {
        (kotlinExt as KotlinProjectExtension).jvmToolchain(version)
    }
}
```

`findByName("kotlin")` returns whichever Kotlin extension this project carries (JVM, Android, or Multiplatform). Each subtype implements `KotlinProjectExtension.jvmToolchain(Int)`, so the cast is safe after the type check.

### `ProjectExtensions.kt`

```kotlin
internal val Project.libs
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
```

Used inside convention plugins to access the version catalog without a `libs.` accessor (which is only available in `build.gradle.kts` files).

## Module `build.gradle.kts` shape

After conventions, a module's build script is **minimal**:

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
        // ... module-specific deps
    }
}
```

`compileSdk`, `minSdk`, JVM toolchain, `explicitApi`, `optIn` — all gone (handled by the conventions).

## Anti-patterns

- **`kotlin { jvmToolchain(19) }` in a module-level script.** Already in the convention.
- **`@OptIn(...)` in source files for an opt-in already in `KotlinMultiplatformConventionPlugin`.** Redundant; remove.
- **Adding a Compose plugin directly without the convention** for a Compose UI module. The compiler metrics + reports are lost.
- **Skipping `applySafely`** in a new convention plugin. Double-apply errors on rebuilds.
- **Adding global `optIn` opt-ins per-feature** instead of in `KotlinMultiplatformConventionPlugin`. Inconsistent — some files have it, some don't.
- **Using `apply(plugin = ...)` instead of `pluginManager.applySafely(...)`** in convention plugins.
- **Reading the version catalog by hand inside a convention plugin** (e.g. building a `VersionCatalogsExtension` lookup inline). Use the `Project.libs` getter from `com/<org>/ProjectExtensions.kt` and call `libs.findLibrary("…").get()` through it.
