# Version Catalog (`libs.versions.toml`)

`gradle/libs.versions.toml` is the canonical version source. See `01-tech-stack/02-libraries.md` for the full file content. This document covers conventions for using and extending the catalog.

## Sections

- `[versions]` — version literals. Keys: kebab-case (`compose-plugin`, `koin-annotations`).
- `[libraries]` — library coordinates with `version.ref` to a `[versions]` entry. Keys: kebab-case (`kotlinx-coroutines-core`).
- `[plugins]` — Gradle plugins with `id` and `version.ref`.
- `[bundles]` — optional groupings (e.g. `compose-foundation`).

## Naming conventions

### `[versions]` keys

- **Kebab-case**: `kotlinx-coroutines`, `firebase-bom`, `compose-plugin`.
- **Plain name** of the library or library family: `kotlin`, `agp`, `decompose`, `room`.
- **`-plugin` suffix** for plugin-version vars: `google-services-plugin`, `firebase-crashlytics-plugin`.

### `[libraries]` keys

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

### `[plugins]` keys

- **Kebab-case** matching the plugin's purpose: `kotlin-multiplatform`, `kotlin-serialization`, `android-application`, `google-services`.
- Use `kotlin-...` prefix for Kotlin plugins (`kotlin-serialization`, `kotlin-parcelize`).
- Use `android-...` for AGP plugins (`android-application`, `android-kotlin-multiplatform-library`).

## Usage in module `build.gradle.kts`

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

## Adding a new library

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

## Adding a new plugin

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

## Bumping a version

1. Update the `[versions]` entry.
2. Run a clean build (`./gradlew clean build`) — catches incompatible API changes early.
3. Check breaking changes in the changelog.
4. **Test on both platforms** — Android library bumps often work on Android but break Kotlin/Native targets.
5. If the lib is exported via XCFramework, run `./gradlew :shared:assembleSharedDebugXCFramework`.

## Avoid

- **Hardcoded version strings** in module-level scripts (`"2.3.21"`). Always via the catalog.
- **Multiple versions of the same library** declared in the catalog. Pick one.
- **Plugin versions out of sync** with the corresponding library (e.g. `compose-plugin = "1.10.3"` but `compose-compiler` derived from `kotlin = "2.3.21"`). Compose Compiler and Compose Multiplatform are intentionally independent.

## Common pitfalls

### `compose-plugin` is the Multiplatform Compose plugin

`compose-plugin` in `[versions]` is the **Multiplatform** Compose version (`1.10.3`), not the Android Compose BOM. The Compose **Compiler** is `kotlin-version`-aligned (`2.3.21`).

| Concern | Source |
|---|---|
| Compose Multiplatform UI runtime | `libs.versions.compose.plugin` |
| Compose Compiler | `libs.versions.kotlin` (compose compiler matches Kotlin) |

### `firebase-bom` aligns Firebase libraries

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

## Anti-patterns

- **Inline versions in module scripts** instead of catalog references.
- **Stale catalog entries** for unused libraries. Remove on delete.
- **Splitting one library across multiple `[versions]` entries.** One version per artifact group.
- **Renaming a catalog key in flight** without updating all consumers. Catalog renames break every consumer's `build.gradle.kts`.
