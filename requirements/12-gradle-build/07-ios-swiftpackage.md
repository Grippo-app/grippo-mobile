# iOS XCFramework Setup

`:shared` is shipped to iOS as a **static XCFramework** named `shared.xcframework`. The aggregate framework bundles all Apple targets (`iosX64`, `iosArm64`, `iosSimulatorArm64`) so Xcode can link it as a single artifact.

## The `IosSwiftPackageConventionPlugin`

```kotlin
class IosSwiftPackageConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.applySafely("org.jetbrains.kotlin.multiplatform")

            extensions.getByType<KotlinMultiplatformExtension>().apply {
                val xcf = XCFramework("shared")

                targets
                    .withType<KotlinNativeTarget>()
                    .matching { it.konanTarget.family.isAppleFamily }
                    .configureEach {
                        binaries.framework(listOf(NativeBuildType.DEBUG, NativeBuildType.RELEASE)) {
                            baseName = "shared"
                            isStatic = true

                            linkerOpts.add("-lsqlite3")

                            xcf.add(this)

                            listOf(
                                libs.findLibrary("decompose.core").get(),
                                libs.findLibrary("decompose.essenty").get(),
                                libs.findLibrary("decompose.state.keeper").get(),
                                libs.findLibrary("decompose.back.handler").get(),
                            ).forEach { exportedDep -> export(exportedDep) }

                            export(project(":data-services:firebase"))
                        }
                    }
            }
        }
    }
}
```

## Output

After running `./gradlew :shared:assembleSharedDebugXCFramework` (or `assembleSharedReleaseXCFramework`):

```
shared/build/XCFrameworks/debug/shared.xcframework/
  Info.plist
  ios-arm64/
    shared.framework/
      ...
  ios-arm64_x86_64-simulator/
    shared.framework/
      ...
```

The XCFramework contains a per-architecture `.framework` for each target. Xcode picks the right one at build time.

## What is exported

```kotlin
export(libs.decompose.core)
export(libs.decompose.essenty)
export(libs.decompose.state.keeper)
export(libs.decompose.back.handler)
export(project(":data-services:firebase"))
```

`export(...)` makes the dep's symbols **visible to Swift**. Without it, Swift would see Kotlin types from `:shared` that reference Decompose types, but the Decompose types themselves would be opaque.

**Only export what Swift needs to call directly.** Excessive exports inflate the framework size and slow Swift compilation.

## `linkerOpts.add("-lsqlite3")`

Room on iOS uses the system SQLite library. Without this linker option, the framework fails to link (`Undefined symbols: _sqlite3_open`).

## `isStatic = true`

The framework is statically linked into the iOS app. Trade-offs:

- **Static** (`isStatic = true`): single binary, no dynamic linker, larger app binary, slower link time.
- **Dynamic** (`isStatic = false`): smaller app binary, dynamic linker at runtime, more flexibility.

The reference repo chooses static — simpler distribution, no `@rpath` issues, and Kotlin/Native's release optimizer benefits from full link-time visibility.

## How Xcode consumes the XCFramework

Two methods (the reference repo uses the first):

### Method 1: Drag-and-drop the XCFramework

1. Run `./gradlew :shared:assembleSharedDebugXCFramework` (or Release).
2. In Xcode, drag `shared/build/XCFrameworks/debug/shared.xcframework` into the project navigator.
3. Add it to "Frameworks, Libraries, and Embedded Content" with **"Do Not Embed"** (static framework — already linked into the app).
4. Build the Xcode target.

### Method 2: CocoaPods or Swift Package Manager

KMP supports both. The reference repo doesn't use them — direct XCFramework integration is the simplest.

## Refresh workflow

```bash
# After editing Kotlin code:
./gradlew :shared:assembleSharedDebugXCFramework

# Xcode auto-detects the change. Trigger a rebuild:
# Cmd+B in Xcode
```

For release builds:

```bash
./gradlew :shared:assembleSharedReleaseXCFramework

# Update Xcode reference to ../shared/build/XCFrameworks/release/shared.xcframework
```

If Xcode is stuck:

- Product > Clean Build Folder (Shift+Cmd+K).
- Quit Xcode, delete `~/Library/Developer/Xcode/DerivedData`, reopen.
- Verify `arch -arm64 brew install ...` if any tool is missing on Apple Silicon.

## Per-target binary

Each Apple target gets its own `.framework`:

```kotlin
binaries.framework(listOf(NativeBuildType.DEBUG, NativeBuildType.RELEASE)) {
    baseName = "shared"
    isStatic = true
    // ...
}
```

The plugin builds Debug AND Release variants. The XCFramework aggregates them.

## `XCFramework("shared")`

```kotlin
val xcf = XCFramework("shared")
```

`XCFramework` is a Kotlin Multiplatform helper that registers a Gradle task to aggregate per-target `.framework`s into a single `shared.xcframework`. The argument is the **baseName** — the final framework's name. Match `baseName` in `binaries.framework { ... }`.

## Build tasks

| Task | Purpose |
|---|---|
| `:shared:assembleSharedDebugXCFramework` | Build debug XCFramework |
| `:shared:assembleSharedReleaseXCFramework` | Build release XCFramework |
| `:shared:linkDebugFrameworkIosArm64` | Build only the iOS device debug `.framework` |
| `:shared:linkReleaseFrameworkIosSimulatorArm64` | Build only the iOS simulator (Apple Silicon) release `.framework` |

Use the XCFramework tasks for distribution; the per-target tasks for fast iteration.

## Common iOS-build issues

### "Undefined symbol _sqlite3_open"

The `-lsqlite3` linker opt isn't in the convention. Verify `IosSwiftPackageConventionPlugin` has `linkerOpts.add("-lsqlite3")`.

### "Module 'Decompose' not found" in Swift

Swift sees the Kotlin-side `RootComponent`, but Decompose types are unknown. The Decompose deps aren't exported. Verify `export(libs.decompose.core)` and siblings are present.

### "duplicate symbols" between Kotlin-Firebase and Swift-Firebase

`:data-services:firebase` is `api`-exposed from `:shared` because the convention plugin's `export(project(":data-services:firebase"))` re-exports it. iOS Swift code calls into the Kotlin `FirebaseProvider`, which delegates to the iOS Firebase SDK initialized natively in Swift (`FirebaseApp.configure()`).

If Swift also initializes Firebase from the Kotlin side, you'd get duplicate symbols. The reference convention is:

- iOS: `FirebaseApp.configure()` in `iOSApp.swift` — initializes the iOS SDK.
- Kotlin `:data-services:firebase` iOS implementation: empty stub. The Kotlin `FirebaseProvider` on iOS doesn't call into the iOS SDK (Swift handles it directly).

### XCFramework is built but stale

The XCFramework artifact lands at `shared/build/XCFrameworks/<config>/shared.xcframework`. Re-running the task **overwrites** it. Xcode caches modules by path + checksum; if Xcode shows stale APIs, do Product > Clean Build Folder.

## Anti-patterns

- **`isStatic = false`** without a reason. Dynamic frameworks add complexity.
- **Exporting every transitive dep.** Inflates framework size, slows Swift compile, leaks internals.
- **Forgetting `-lsqlite3`.** Room link errors on iOS.
- **Hardcoding Xcode build settings to override Kotlin defaults.** The Kotlin/Native plugin manages framework attributes.
- **Per-arch binary distribution** (separate `.framework` per arch). Use the XCFramework — single artifact.
- **iOS-specific Kotlin code that calls into the iOS Firebase SDK.** Let Swift handle that. Keep Kotlin's iOS Firebase impl empty.
