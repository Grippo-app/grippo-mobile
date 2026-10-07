# Version catalog & settings.gradle

Self-contained reference for the Gradle version catalog (`gradle/libs.versions.toml`) and the root `settings.gradle.kts` — naming conventions, usage, extension procedures, the verbatim settings file, repository/module conventions, and anti-patterns.

## Version catalog

`gradle/libs.versions.toml` is the canonical version source. This section covers conventions for using and extending the catalog.

### Sections

- `[versions]` — version literals. Keys: kebab-case (`compose-plugin`, `koin-annotations`).
- `[libraries]` — library coordinates with `version.ref` to a `[versions]` entry. Keys: kebab-case (`kotlinx-coroutines-core`).
- `[plugins]` — Gradle plugins with `id` and `version.ref`.
- `[bundles]` — optional groupings (e.g. `compose-foundation`).

### Naming conventions

#### `[versions]` keys

- **Kebab-case**: `kotlinx-coroutines`, `firebase-bom`, `compose-plugin`.
- **Plain name** of the library or library family: `kotlin`, `agp`, `decompose`, `room`.
- **`-plugin` suffix** for plugin-version vars: `google-services-plugin`, `firebase-crashlytics-plugin`.

#### `[libraries]` keys

- **Kebab-case** matching the artifact, with the group/module collapsed:
  - `kotlinx-coroutines-core` for `org.jetbrains.kotlinx:kotlinx-coroutines-core`.
  - `androidx-room-compiler` for `androidx.room:room-compiler`.
  - `koin-annotations` for `io.insert-koin:koin-annotations`.
- **Vendor prefix** (`androidx-`, `kotlinx-`, `google-`, `android-` for Firebase) — disambiguates similar names.

Gradle generates **type-safe accessors** from these keys by converting each hyphen in a catalog alias into a `.` (so the dashed alias becomes a dot-separated path under `libs`):

| Catalog key | Gradle accessor |
|---|---|
| `kotlinx-coroutines-core` | `libs.kotlinx.coroutines.core` |
| `koin-annotations` | `libs.koin.annotations` |
| `androidx-room-compiler` | `libs.androidx.room.compiler` |
| `android-firebase-bom` | `libs.android.firebase.bom` |

#### `[plugins]` keys

- **Kebab-case** matching the plugin's purpose: `kotlin-multiplatform`, `kotlin-serialization`, `android-application`, `google-services`.
- Use `kotlin-...` prefix for Kotlin plugins (`kotlin-serialization`, `kotlin-parcelize`).
- Use `android-...` for AGP plugins (`android-application`, `android-kotlin-multiplatform-library`).

### Usage in module `build.gradle.kts`

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.datetime)
        implementation(libs.immutable.collections)
    }
}
```

- **`alias(libs.plugins.<x>)`** for plugin application.
- **`implementation(libs.<x>)`** for library deps.

### Adding a new library

1. Discuss with the team — every new dep is a deliberate decision (license, size, transitives).
2. Add a `[versions]` entry (or reuse an existing one).
3. Add the `[libraries]` entry with `version.ref`.
4. Use it via `libs.<accessor>` in module-level scripts.
5. If the lib must be **exported via the iOS XCFramework** (Swift code touches it), add an `export(libs.find...)` line in `IosSwiftPackageConventionPlugin`. Only do this for transitive deps of `:shared`'s public API (rare).

Example: adding `kotlinx-html` for an internal HTML builder:

```toml
[versions]
# ...
kotlinx-html = "0.11.0"

[libraries]
# ...
kotlinx-html = { module = "org.jetbrains.kotlinx:kotlinx-html", version.ref = "kotlinx-html" }
```

```kotlin
// some module
implementation(libs.kotlinx.html)
```

### Adding a new plugin

```toml
[versions]
ksp = "2.3.4"

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

```kotlin
// in module-level build.gradle.kts
plugins {
    alias(libs.plugins.ksp)
}
```

For plugins consumed by convention plugins (in `build-logic`), reference them in `build-logic/convention/build.gradle.kts`:

```kotlin
dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.compose.gradle.plugin)
    implementation(libs.ksp.plugin.api)
}
```

These are pulled from `[libraries]`, not `[plugins]` — Gradle uses the Maven coordinates to load the plugin's Gradle JAR.

### Bumping a version

1. Update the `[versions]` entry.
2. Run a clean build (`./gradlew clean build`) — catches incompatible API changes early.
3. Check breaking changes in the changelog.
4. **Test on both platforms** — Android library bumps often work on Android but break Kotlin/Native targets.
5. If the lib is exported via XCFramework, run `./gradlew :shared:assembleSharedDebugXCFramework`.

### Avoid

- **Hardcoded version strings** in module-level scripts (`"2.3.21"`). Always via the catalog.
- **Multiple versions of the same library** declared in the catalog. Pick one.
- **Plugin versions out of sync** with the corresponding library (e.g. `compose-plugin = "1.10.3"` but `compose-compiler` derived from `kotlin = "2.3.21"`). Compose Compiler and Compose Multiplatform are intentionally independent.

### Common pitfalls

#### `compose-plugin` is the Multiplatform Compose plugin

`compose-plugin` in `[versions]` is the **Multiplatform** Compose version (`1.10.3`), not the Android Compose BOM. The Compose **Compiler** is `kotlin-version`-aligned (`2.3.21`).

| Concern | Source |
|---|---|
| Compose Multiplatform UI runtime | `libs.versions.compose.plugin` |
| Compose Compiler | `libs.versions.kotlin` (compose compiler matches Kotlin) |

#### `firebase-bom` aligns Firebase libraries

```toml
android-firebase-bom = { module = "com.google.firebase:firebase-bom", version.ref = "firebase-bom" }
android-firebase-analytics = { module = "com.google.firebase:firebase-analytics" }
```

The BOM defines versions for `firebase-analytics`, `firebase-crashlytics`, `firebase-messaging` — those libraries' coordinates have **no version**. Add the BOM as a platform dep in `:androidApp/build.gradle.kts`:

```kotlin
implementation(project.dependencies.platform(libs.android.firebase.bom))
implementation(libs.android.firebase.analytics)
implementation(libs.android.firebase.crashlytics)
```

### Anti-patterns

- **Inline versions in module scripts** instead of catalog references.
- **Stale catalog entries** for unused libraries. Remove on delete.
- **Splitting one library across multiple `[versions]` entries.** One version per artifact group.
- **Renaming a catalog key in flight** without updating all consumers. Catalog renames break every consumer's `build.gradle.kts`.

## settings.gradle.kts

The single source of truth for the module list. This section covers the settings file's structure and conventions.

### Full file

```kotlin
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")

    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    // Auto-provisions JDK toolchains via the Foojay Disco API (the JDK 21 the screenshot-gate host-test
    // requests, and the app's JDK 19) — no machine-specific JDK paths, works on CI. Inert unless a
    // toolchain is requested. See the implement-figma skill (screenshot-fidelity gate).
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"   // pin to latest at setup time
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement.repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

@Suppress("UnstableApiUsage")
dependencyResolutionManagement.repositories {
    google {
        content {
            includeGroupByRegex("com\\.android.*")
            includeGroupByRegex("com\\.google.*")
            includeGroupByRegex("androidx.*")
        }
    }
    mavenCentral()
}

rootProject.name = "<product>-mobile"

include(":androidApp")
include(":shared")

include(":design-system:preview")
include(":design-system:core")
include(":design-system:resources:provider")
include(":design-system:resources:provider-impl")
include(":design-system:components")

include(":data-services:google-auth")
include(":data-services:apple-auth")
include(":data-services:firebase")
include(":data-services:backend")
include(":data-services:database")
include(":data-services:datastore")

include(":data-features:feature-api")
include(":data-features:authorization")
// ... one per business area

include(":ui-core:foundation")
include(":ui-core:state")
include(":ui-core:error:error-provider-impl")
include(":ui-core:error:error-provider")

include(":ui-screen-features:screen-api")
include(":ui-screen-features:authorization")
include(":ui-screen-features:home")
// ... one per top-level screen flow

include(":ui-dialog-features:dialog-api")
include(":ui-dialog-features:confirmation")
include(":ui-dialog-features:error-display")
// ... one per bottom-sheet flow

include(":toolkit:context")
include(":toolkit:localization")
include(":toolkit:theme")
include(":toolkit:http-client")
include(":toolkit:image-loader")
include(":toolkit:logger")
include(":toolkit:connectivity")
include(":toolkit:serialization")
include(":toolkit:date-utils")
include(":toolkit:link-opener")
include(":toolkit:notification-manager")
include(":toolkit:permission-manager")

include(":compose-libs:wheel-picker")
include(":compose-libs:segment-control")
include(":compose-libs:konfetti")
include(":compose-libs:chart")

include(":data-mappers:entity-to-domain")
include(":data-mappers:dto-to-entity")
include(":data-mappers:dto-to-domain")
include(":data-mappers:domain-to-state")
include(":data-mappers:domain-to-entity")
include(":data-mappers:domain-to-dto")
include(":data-mappers:state-to-domain")

// Tooling (JVM-only Gradle helpers — not part of the KMP build graph).
// Append every `:tooling:<name>` module that the project ships. The reference
// project ships `:tooling:detekt-rules`; other tooling modules go in the same block.
include(":tooling:detekt-rules")
```

### `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")`

Enables type-safe project accessors so module-level scripts can write:

```kotlin
implementation(projects.uiCore.foundation)
implementation(projects.dataServices.backend)
```

instead of:

```kotlin
implementation(project(":ui-core:foundation"))
implementation(project(":data-services:backend"))
```

The `projects.foo.bar` accessor is generated by Gradle from the `:foo:bar` module path (camelCased).

### `pluginManagement.includeBuild("build-logic")`

Makes the `build-logic` included build's plugins available to the root build. The conventions registered in `build-logic/convention/build.gradle.kts`'s `gradlePlugin { ... }` block are then usable as `plugins { id("android.library.convention") }` in module-level scripts.

### `plugins` — Foojay toolchain resolver

```kotlin
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}
```

Registers the [Foojay Disco API](https://github.com/gradle/foojay-toolchains) as a **Java toolchain download repository** for the whole build. When a task requests a JVM toolchain that isn't installed locally — for example, the **JDK 21** used by the screenshot-fidelity host test (see [`convention-plugins.md`](convention-plugins.md) § "Optional convention plugin") or the app's pinned toolchain — Gradle auto-downloads it instead of failing. This keeps the build hands-off on a fresh checkout and on CI, with no machine-specific `org.gradle.java.installations.paths`. It is **inert** for any build that never requests a missing toolchain. Pin the resolver version during project setup.

### `repositories` — restricted

```kotlin
google {
    content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
    }
}
gradlePluginPortal()
mavenCentral()
```

- **`google()`** with `includeGroupByRegex` — only fetches `com.android.*`, `com.google.*`, `androidx.*` from Google's Maven. Reduces unnecessary repository queries.
- **`gradlePluginPortal()`** — for Gradle plugins.
- **`mavenCentral()`** — for everything else.

No `mavenLocal()`, no `jitpack`, no custom Maven repos. Adding one requires an explicit decision (security review for jitpack-hosted libraries).

### `dependencyResolutionManagement.repositoriesMode.set(FAIL_ON_PROJECT_REPOS)`

Module-level scripts **cannot** declare their own repositories. All resolution happens through `dependencyResolutionManagement.repositories` (set at the settings level).

This is **enforced** at build time — declaring `repositories { ... }` in a `build.gradle.kts` fails with an error.

### `rootProject.name`

```kotlin
rootProject.name = "<product>-mobile"
```

Match the directory name. Used by Gradle as the project identifier and shown in `gradle/wrapper/...`.

### Module path conventions

```kotlin
include(":<group>:<name>")
```

- **`:<group>:<name>`** — colon-separated path. Each segment is a directory.
- **`<group>`** is one of: `androidApp` (no subgroup), `shared`, `design-system`, `data-services`, `data-features`, `data-mappers`, `ui-core`, `ui-screen-features`, `ui-dialog-features`, `toolkit`, `compose-libs`, `tooling`.
- **`<name>`** is kebab-case (`feature-api`, `dialog-api`, `note-picker`).

### Order

Modules are listed grouped:

1. App + composition root (`:androidApp`, `:shared`).
2. Design system.
3. Data services.
4. Data features.
5. UI core.
6. UI screen features.
7. UI dialog features.
8. Toolkit.
9. Compose libs.
10. Data mappers.
11. Tooling (e.g. `:tooling:detekt-rules` — JVM-only Gradle helpers, not part of the KMP build graph).

Within a group, order alphabetically or by topological dependency (`:feature-api` before specific features).

### Adding a module — three steps

1. Create the module directory (`mkdir -p path/to/module`).
2. Create the `build.gradle.kts` (template per `gradle-build.md` § Representative build.gradle.kts shapes).
3. Add `include(":path:to:module")` to **`settings.gradle.kts`** in the right group.

If the module declares a Koin module, also add `<X>Module().module` to `:shared/Koin.kt`. See the di-modules skill, references/composition-root.md.

### Removing a module

1. Delete the module directory.
2. Remove the `include(":...")` line from `settings.gradle.kts`.
3. Remove any `implementation(projects.<group>.<name>)` references from other modules.
4. Remove the Koin module from `:shared/Koin.kt` if applicable.
5. `./gradlew clean build` — verify the project still builds.

### Anti-patterns

- **Local repository declarations** in module scripts. Forbidden — `FAIL_ON_PROJECT_REPOS` enforces this.
- **`mavenLocal()`** — convenient for testing local builds but a hidden source of "works on my machine" failures.
- **Modules not listed in `settings.gradle.kts`.** Gradle won't see them.
- **Different `rootProject.name`** at different times. The name must be stable across the project lifetime.
- **Including a module twice** — Gradle errors with "Project ':foo' is included multiple times".
- **Spaces or special characters in module paths.** Use kebab-case only.

## Root `build.gradle.kts` — verification aggregates & capability inventory

The root build script is created at bootstrap (launch Step 2 item 6) and owns
exactly one concern: stable, fail-closed verification entry points. It applies
no plugins and configures no modules. Model output never guesses AGP/KGP task
names — these aggregates and the per-module lane aliases registered by
`kmp.test.convention` are the only supported commands.

```kotlin
// Root build.gradle.kts — verification aggregates only.
// dependsOn(subprojects.map { p -> p.tasks.matching { ... } }) is the frozen
// configuration-cache-safe aggregation idiom (two consecutive CC runs proven
// on the pinned stack: entry stored, then reused).

tasks.register("allHostTests") {
    group = "verification"
    description = "Runs every test-bearing module's Android host tests."
    dependsOn(subprojects.map { p -> p.tasks.matching { it.name == "hostTests" } })
}

tasks.register("allIosSimulatorTests") {
    group = "verification"
    description = "Runs every eligible module's Kotlin/Native tests on the iOS simulator."
    dependsOn(subprojects.map { p -> p.tasks.matching { it.name == "iosSimulatorTests" } })
}

tasks.register("allAndroidDeviceTests") {
    group = "verification"
    description = "Runs every device-enabled module's instrumented tests on connected devices."
    dependsOn(subprojects.map { p -> p.tasks.matching { it.name == "androidDeviceTests" } })
}

tasks.register("allScreenshotTests") {
    group = "verification"
    description = "Verifies every screenshot module's Roborazzi captures."
    dependsOn(subprojects.map { p -> p.tasks.matching { it.name == "verifyRoborazziAndroidHostTest" } })
}

tasks.register("allConfiguredTests") {
    group = "verification"
    description = "Runs every configured test lane (host, iOS simulator, device, screenshots)."
    dependsOn("allHostTests", "allIosSimulatorTests", "allAndroidDeviceTests", "allScreenshotTests")
}

tasks.register("testCapabilityInventory") {
    group = "verification"
    description = "Aggregates per-module test capability fragments into build/test-capability/inventory.json."
    dependsOn(subprojects.map { p -> p.tasks.matching { it.name == "testCapabilityEntry" } })
    val fragments = files(subprojects.map { p -> p.layout.buildDirectory.file("test-capability/entry.json") })
    inputs.files(fragments).skipWhenEmpty()
    val out = layout.buildDirectory.file("test-capability/inventory.json")
    outputs.file(out)
    doLast {
        val entries = fragments.files.filter { it.exists() }.map { it.readText() }.sorted()
        out.get().asFile.writeText(entries.joinToString(",\n", "[", "]\n"))
    }
}
```

Rules (all fail-closed):

- an aggregate depends on the **real** per-module task providers; a module that
  should own a lane but lost its alias fails the build instead of printing
  "nothing to do";
- an optional lane with zero configured owners is legitimately empty here —
  the typed `not-configured` verdict is decided by the capability inventory
  plus the machine test policy, never by a CI `if:` condition or a grep;
- the inventory JSON is the machine input for
  `orchestrator/tasks/test-capability-inventory.schema.json` (domain
  `test-capability-inventory`); the Node runtime validates schema and hash
  only and never parses build scripts;
- aggregates never create production artifacts and never touch the publication
  graph;
- raising these into per-task certification commands is owned by the
  deterministic test executor — task prose never invokes `bash -c` shell built
  from task content.
