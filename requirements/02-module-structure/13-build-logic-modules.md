# `:build-logic` — Convention Plugins

`:build-logic` is an **included build** (`includeBuild("build-logic")` in `settings.gradle.kts`'s `pluginManagement`). It hosts all Gradle convention plugins. Module-level `build.gradle.kts` files contain only `plugins { id("...convention") }` and a `kotlin { sourceSets ... }` block — every actual configuration lives in a convention plugin.

This keeps build configuration **DRY** and **consistent**: bumping `compileSdk`, changing JVM toolchain, or adding a global `optIn` is a single edit.

## Structure

```
build-logic/
  settings.gradle.kts                       // includes :convention, points at libs.versions.toml
  build.gradle.kts                          // empty (root)
  convention/
    build.gradle.kts                        // declares the plugins via gradlePlugin {}
    src/main/kotlin/
      KotlinMultiplatformConventionPlugin.kt
      AndroidLibraryConventionPlugin.kt
      AndroidApplicationConventionPlugin.kt
      ComposeMultiplatformConventionPlugin.kt
      KoinAnnotationConventionPlugin.kt
      RoomConventionPlugin.kt
      IosSwiftPackageConventionPlugin.kt
      com/<org>/                            // shared helpers (rename "<org>" per product)
        ConfigureJvmToolchain.kt            // Project.configureJvmToolchain(version)
        PluginManagerExtensions.kt          // PluginManager.applySafely(id)
        ProjectExtensions.kt                // Project.libs accessor
```

## `build-logic/settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")
```

This re-imports the **same** version catalog used by the main build, so convention plugins reference exactly the same versions as `settings.gradle.kts` does.

## `build-logic/convention/build.gradle.kts`

```kotlin
plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.compose.gradle.plugin)
    implementation(libs.ksp.plugin.api)
}

gradlePlugin {
    plugins {
        register("android.library.convention") {
            id = "android.library.convention"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("android.application.convention") {
            id = "android.application.convention"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("compose.multiplatform.convention") {
            id = "compose.multiplatform.convention"
            implementationClass = "ComposeMultiplatformConventionPlugin"
        }
        register("kotlin.multiplatform.convention") {
            id = "kotlin.multiplatform.convention"
            implementationClass = "KotlinMultiplatformConventionPlugin"
        }
        register("koin.annotation.convention") {
            id = "koin.annotation.convention"
            implementationClass = "KoinAnnotationConventionPlugin"
        }
        register("room.convention") {
            id = "room.convention"
            implementationClass = "RoomConventionPlugin"
        }
        register("ios.swiftpackage.convention") {
            id = "ios.swiftpackage.convention"
            implementationClass = "IosSwiftPackageConventionPlugin"
        }
    }
}
```

See `12-gradle-build/01-convention-plugins.md` for the verbatim body of every plugin.

## Rules

1. **One convention plugin per concern.** Don't merge "android library" and "compose" into one. The matrix is documented in `12-gradle-build/01-convention-plugins.md`.
2. **`applySafely`** — use `pluginManager.applySafely("com.foo.bar")` instead of `pluginManager.apply("com.foo.bar")`. The helper checks `hasPlugin(...)` first; multiple convention plugins may pull in the same upstream plugin and this avoids errors.
3. **Configuration cache compatible.** No `project.evaluationDependsOn(...)`. No eager access to other projects' configurations. Use `pluginManager.withPlugin("...") { extensions.configure<...> { ... } }` to defer configuration until the upstream plugin is applied.
4. **No conditional logic per project.** If a module needs different settings, it should consume a different convention plugin. Don't read `project.name` and branch.

## When to add a new convention plugin

Add when:

- A concern touches **multiple** modules with the **same** settings. (E.g. if every screen feature module needs the same Compose stability config beyond what `ComposeMultiplatformConventionPlugin` provides.)
- A convention plugin's behavior would become heavily conditional. Split it.

Don't add for:

- A single module's special needs — put the config in that module's `build.gradle.kts`.
- One-off plugin applications.

## Module-level `build.gradle.kts` shape

After convention plugins do their work, every module's `build.gradle.kts` shrinks to:

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")    // if the module has Compose UI
    id("koin.annotation.convention")          // if the module declares a Koin module
    id("room.convention")                     // only :data-services:database
    alias(libs.plugins.kotlin.serialization)  // if @Serializable types are declared
}

kotlin {
    android { namespace = "com.<org>.<product>.<module-path>" }

    sourceSets.commonMain.dependencies {
        // module-specific dependencies
    }
}
```

`compileSdk`, `minSdk`, `jvmToolchain`, `explicitApi`, `optIn`, KSP wiring, Room schema location — all in convention plugins. Module-level config is intentionally minimal.
