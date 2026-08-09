# Convention plugins (build-logic)

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
| Test-bearing module (owns at least one test) | + `kmp.test.convention` — **opt-in**, never transitive; see § Test conventions |
| Test-bearing module needing coroutines/Flow/HTTP/DI/Room/Compose-UI test evidence | + the matching `*.test.convention` capability plugin(s) — each applies `kmp.test.convention` itself |
| Feature module opting into the screenshot-fidelity gate (post-bootstrap) | + `screenshot.test.convention` — **optional**, see the screenshot-fidelity gate spec |

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

            iosX64()
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
- **iOS targets** registered up-front. `applyDefaultHierarchyTemplate()` sets up the standard source-set hierarchy (`commonMain`, `commonTest`, `iosMain`, `iosX64Main`, ...).
- **Global `optIn`** — the list of experimental APIs every module uses. Don't repeat `@OptIn` in source files.

### `AndroidLibraryConventionPlugin`

```kotlin
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.applySafely("com.android.kotlin.multiplatform.library")

        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            extensions.configure<KotlinMultiplatformExtension> {
                targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                    compileSdk = 36
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
- `compileSdk = 36`, `minSdk = 26`.
- `namespace = "com.<org>.<product>"` — the namespace **prefix**. Every module-level `build.gradle.kts` **must** override this with its full, unique namespace (`kotlin { android { namespace = "..." } }`); omitting the override leaves multiple modules sharing the bare prefix, causing duplicate-namespace/resource-merge build failures.
- JVM toolchain 19.

### `AndroidApplicationConventionPlugin`

```kotlin
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.applySafely("com.android.application")

        extensions.configure<ApplicationExtension> {
            compileSdk = 36
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
                stabilityConfigurationFiles.add(
                    rootProject.layout.projectDirectory.file("compose-stability.conf"),
                )
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
- **`compose-stability.conf`** — a project-root file telling the Compose Compiler about externally-stable types (e.g. immutable collections from the Decompose API). The file may not exist initially — the plugin silently ignores missing files.

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
- Makes platform KSP tasks (`kspAndroid`, `kspIosX64`, ...) depend on the common metadata task.

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
            add("kspIosX64", libs.findLibrary("androidx.room.compiler").get())
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

## Test conventions — opt-in general test foundation

The general KMP test stack is bootstrapped for every product but applied **only
by modules that own tests** — `kotlin.multiplatform.convention` never applies
it transitively (that would fan `NO-SOURCE` host tasks and test dependencies
across production-only modules). One base plugin owns the Android host test;
capability plugins add exactly one concern each and apply the base themselves
via `applySafely`. Plugin order never matters, and a second application is a
safe no-op. All names below are frozen decisions proven on the pinned stack
(AGP 9.0.1, Kotlin 2.3.21, Gradle 9.1.0): source set `androidHostTest`,
compilation `hostTest`, executable task `testAndroidHostTest`, iOS lane
`iosSimulatorArm64Test`, device lane `connectedAndroidDeviceTest` (source set
`androidDeviceTest`, compilation `deviceTest`). The provider-based
`androidDeviceCheck`/`deviceAndroidTest` tasks are false-green no-ops without
configured device providers — they are never alias targets.

### `KmpTestConventionPlugin` (`kmp.test.convention`)

The single owner of the Android host test. Nothing else in the build may call
`withHostTest` — AGP fails the configuration on a second enabler call
("Android host tests have already been enabled"), and that failure is the
enforcement, not a style rule.

```kotlin
class KmpTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        extensions.getByType<KotlinMultiplatformExtension>().apply {
            // THE single enabler call site for the whole build. Defaults stay
            // out of the enabler: capability plugins flip them reactively via
            // the compilation DSL (proven post-creation on the pinned stack).
            targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                withHostTest { }
            }
            sourceSets.configureEach {
                if (name == "commonTest") dependencies {
                    implementation(libs.findLibrary("kotlin.test").get())
                }
            }
        }

        // Stable per-module lane aliases. Each depends on the REAL test task
        // provider — a missing provider fails the build instead of printing
        // "nothing to do". iOS/device aliases appear only on modules whose
        // targets configure those lanes.
        tasks.register("hostTests") {
            group = "verification"
            description = "Runs this module's Android host tests (testAndroidHostTest)."
            dependsOn(tasks.named("testAndroidHostTest"))
        }
        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            extensions.getByType<KotlinMultiplatformExtension>().targets.configureEach {
                if (name == "iosSimulatorArm64") {
                    tasks.register("iosSimulatorTests") {
                        group = "verification"
                        description = "Runs this module's Kotlin/Native tests on the iOS simulator."
                        dependsOn(tasks.named("iosSimulatorArm64Test"))
                    }
                }
            }
        }

        // Capability inventory fragment: one JSON per module, aggregated by the
        // root `testCapabilityInventory` task (configuration-cache safe: the
        // root task consumes these files, never project state).
        tasks.register<TestCapabilityEntryTask>("testCapabilityEntry") {
            group = "verification"
            description = "Writes this module's test capability inventory fragment."
            modulePath.set(path)
            outputFile.set(layout.buildDirectory.file("test-capability/entry.json"))
        }
    }
}
```

- Base dependency is **runner/assertion API only** (`kotlin-test`); every other
  test library arrives through a capability plugin.
- Zero-test detection stays on: Gradle 9.1 fails an executed test task with no
  discovered tests, and a `--tests` filter matching nothing fails on its own.
  `NO-SOURCE` still produces a green build — that hole is closed by the
  deterministic test executor, never by disabling the check.
- Test dependencies never reach production source sets, runtime publication or
  XCFramework exports (host/iOS production classpaths stay clean — verified on
  the generated fixture).

### Capability conventions

Each capability plugin: `pluginManager.applySafely("kmp.test.convention")`,
then exactly one concern. Versions come from the catalog; a module build file
names capabilities, never Maven coordinates.

| Plugin id | Adds | Where |
|---|---|---|
| `coroutines.test.convention` | `kotlinx-coroutines-test` | `commonTest` |
| `flow.test.convention` | Turbine (applies `coroutines.test.convention` first) | `commonTest` |
| `network.test.convention` | `ktor-client-mock` | `commonTest` |
| `di.test.convention` | `koin-test` (JVM/host evidence only — never Native) | `androidHostTest` |
| `room.test.convention` | `androidx-room-testing` + androidx test runner/core; enables the device lane through the single internal enabler helper | `androidDeviceTest` |
| `compose.ui.test.convention` | shared `uiTest` scenario tree + thin platform entries: `compose.uiTest` in the tree, Robolectric + `junit4` + `compose-ui-test-manifest` on the host entry, JDK 21 launcher narrowed to the host task | `uiTest`, `androidHostTest`, `iosSimulatorArm64Test` |

The shapes that are non-obvious, verbatim:

```kotlin
class ComposeUiTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.applySafely("kmp.test.convention")

        extensions.getByType<KotlinMultiplatformExtension>().apply {
            sourceSets.apply {
                // Shared Compose UI SCENARIOS live here (ComposeUiTest extension
                // functions + fixtures). Thin platform test classes own the
                // runner: plain kotlin-test on the iOS simulator, Robolectric
                // (@RunWith + @GraphicsMode(NATIVE)) inside the single Android
                // host task. Common Compose UI tests never run on a plain host
                // (Build.FINGERPRINT NPE), and CMP 1.10.3 cannot even configure
                // `withDeviceTest` on a Compose module (its resources task
                // fails validation) — never enable the device lane here.
                val uiTest = maybeCreate("uiTest").apply {
                    dependsOn(getByName("commonTest"))
                    dependencies {
                        @OptIn(ExperimentalComposeLibrary::class)
                        implementation(compose.dependencies.uiTest)
                    }
                }
                configureEach {
                    if (name == "iosSimulatorArm64Test" || name == "androidHostTest") dependsOn(uiTest)
                    if (name == "androidHostTest") dependencies {
                        implementation(libs.findLibrary("robolectric").get())
                        implementation(libs.findLibrary("junit4").get())
                        implementation(libs.findLibrary("compose.ui.test.manifest").get())
                    }
                }
            }
            // Reactive default — the enabler stays untouched in the base plugin.
            targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                compilations.configureEach {
                    if (name == "hostTest") {
                        (this as KotlinMultiplatformAndroidHostTestCompilation).isIncludeAndroidResources = true
                    }
                }
            }
        }

        // Robolectric's own sandbox demands Java 21 for SDK 36 ("Android SDK 36
        // requires Java 21"). Narrowed to the exact host task — never a broad
        // tasks.withType<Test> that would silently move every general test in
        // the module onto another launcher.
        val toolchains = extensions.getByType<JavaToolchainService>()
        tasks.withType<Test>().matching { it.name == "testAndroidHostTest" }.configureEach {
            javaLauncher.set(toolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
        }
    }
}
```

```kotlin
// Internal helper (build-logic, not a public plugin): the single owner of the
// device-lane enabler, today called only by room.test.convention. Fail-closed
// on Compose modules — CMP 1.10.3's CopyResourcesToAndroidAssetsTask cannot
// configure the deviceTest compilation.
internal fun Project.enableAndroidDeviceLane() {
    check(!pluginManager.hasPlugin("org.jetbrains.compose")) {
        "The Android device lane cannot be enabled on a Compose module at the pinned stack " +
            "(CMP 1.10.3 deviceTest resources task fails configuration validation)."
    }
    extensions.getByType<KotlinMultiplatformExtension>()
        .targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
            withDeviceTest { }
        }
    tasks.register("androidDeviceTests") {
        group = "verification"
        description = "Runs this module's instrumented tests on connected devices."
        dependsOn(tasks.named("connectedAndroidDeviceTest"))
    }
}
```

- The `deviceTest` compilation is **standalone**: it does not include
  `commonTest` (an `actual` there has no visible `expect`). Device tests are
  platform-authored; shared substance reaches the lane only as plain shared
  scenario code compiled into the device source set explicitly.

### `TestCapabilityEntryTask`

The per-module inventory fragment the root `testCapabilityInventory` task
aggregates. The base plugin registers it with `capabilities = ["base"]` and the
host lane; every capability plugin contributes its own id (and lane entries)
through the same properties — the fragment is configuration data, so the Node
runtime never parses build scripts:

```kotlin
abstract class TestCapabilityEntryTask : DefaultTask() {
    @get:Input abstract val modulePath: Property<String>
    @get:Input abstract val capabilities: SetProperty<String>
    // lane id -> exact executable Gradle task path (never a guessed suffix)
    @get:Input abstract val lanes: MapProperty<String, String>
    @get:OutputFile abstract val outputFile: RegularFileProperty

    @TaskAction
    fun write() {
        val laneJson = lanes.get().toSortedMap().entries.joinToString(",") {
            "\"${it.key}\":{\"taskPath\":\"${it.value}\"}"
        }
        val caps = capabilities.get().sorted().joinToString(",") { "\"$it\"" }
        outputFile.get().asFile.writeText(
            "{\"path\":\"${modulePath.get()}\",\"capabilities\":[$caps],\"lanes\":{$laneJson}}"
        )
    }
}
```

Contributions (all reactive, order-independent): `kmp.test.convention` seeds
`base` + `host -> <module>:testAndroidHostTest` (+ `ios-simulator` when the
target exists); `room.test.convention` adds `room` +
`android-device -> <module>:connectedAndroidDeviceTest`;
`compose.ui.test.convention` adds `compose-ui`; `screenshot.test.convention`
adds `screenshot` + `screenshot -> <module>:verifyRoborazziAndroidHostTest`;
the remaining capabilities add only their id. The aggregated file must
validate against `orchestrator/tasks/test-capability-inventory.schema.json`.
- `room.test.convention` therefore wires: `enableAndroidDeviceLane()` +
  `androidx-room-testing`, `androidx-test-runner`, `androidx-test-core`,
  `junit4`, `kotlinx-coroutines-test` into `androidDeviceTest`. Fast common DAO
  tests run through `commonTest` against the iOS lane's real
  `BundledSQLiteDriver` actual; Android fidelity runs on the device lane.

### `coverage.test.convention` (optional — the JVM-host coverage ratchet)

Applied only after the mandatory-behavior pipeline is stable; Kover `0.9.9` is
compatibility-proven on the pinned stack (`koverXmlReport` over the Android
host lane, configuration-cache clean). Hard doctrine:

- the only label is **`jvm-host-coverage`** — Kover measures JVM bytecode and
  is NEVER presented as Native/iOS or whole-KMP coverage; the iOS lane stays a
  test-execution status without a percentage;
- the per-module baseline lives in ONE canonical machine-readable file at the
  product root (`config/jvm-host-coverage-baseline.json`: `{ "version": 1,
  "label": "jvm-host-coverage", "modules": { ":path": <line-covered-int> } }`),
  seeded from the first reviewed green host report — never rounded down and
  never edited by a feature task; lowering any value is a separate
  owner-authorized baseline task;
- generated-code exclusions are an explicit reviewed allowlist inside the
  convention, never a wildcard;
- percentages never replace behavior mapping — the ratchet runs inside the
  host CI lane (`product-host-tests`), and no new required job or check name
  is created for it.

```kotlin
class CoverageTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.applySafely("kmp.test.convention")
        pluginManager.applySafely("org.jetbrains.kotlinx.kover")
        // Reports ride the single host test task; the ratchet comparison against
        // config/jvm-host-coverage-baseline.json is a verification task that
        // fails on any per-module decrease.
    }
}
```

## Optional convention plugin — `screenshot.test.convention`

`ScreenshotTestConventionPlugin` ships in `build-logic` alongside the seven above but is **opt-in and inert**: only feature modules in the screenshot-fidelity gate apply it (`id("screenshot.test.convention")`) — i.e. `figmaEnabled` screen modules. A project without Figma (or that never pulls a screen oracle) pays nothing. Canonical spec — test file, Gradle task, JDK 21, calibration — is the screenshot-fidelity gate spec; this section owns only the convention-plugin wiring.

It **extends** the single host test owned by `kmp.test.convention` — it never
creates one (a second `withHostTest` call is an AGP configuration failure).
It applies Roborazzi, flips Android resources on reactively, wires the
screenshot-only dependencies, pins **exactly the host-test task** to a JDK 21
launcher (Robolectric's own sandbox check demands Java 21 for SDK 36; the
module itself still compiles on 19, and general tests in other modules stay on
the project toolchain), and registers an authoritative
`verifyScreenshotToolchain` preflight task:

```kotlin
class ScreenshotTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.applySafely("kmp.test.convention")
        pluginManager.applySafely("io.github.takahirom.roborazzi")

        extensions.getByType<KotlinMultiplatformExtension>().apply {
            // Reactive default on the compilation created by the base plugin —
            // never a second enabler call.
            targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                compilations.configureEach {
                    if (name == "hostTest") {
                        (this as KotlinMultiplatformAndroidHostTestCompilation).isIncludeAndroidResources = true
                    }
                }
            }
            sourceSets.configureEach {
                if (name == "androidHostTest") dependencies {
                    implementation(libs.findLibrary("roborazzi").get())
                    implementation(libs.findLibrary("roborazzi.compose").get())
                    implementation(libs.findLibrary("robolectric").get())
                    implementation(libs.findLibrary("junit4").get())
                }
            }
        }

        // Narrowed to the exact host task: a broad tasks.withType<Test> would
        // silently move every general test in this module onto JDK 21.
        val toolchains = extensions.getByType<JavaToolchainService>()
        tasks.withType<Test>().matching { it.name == "testAndroidHostTest" }.configureEach {
            javaLauncher.set(toolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
        }

        // Authoritative toolchain preflight (run once at bootstrap). Eagerly RESOLVES
        // the very same JDK 21 launcher the host-test pins, so a missing or foojay-unreachable
        // toolchain fails LOUD here instead of the screenshot gate silently not running on a
        // misconfigured box. `.get()` forces the real resolution (download path included) — it
        // proves the actual thing, not a `gradle.properties`/JAVA_HOME grep proxy.
        tasks.register("verifyScreenshotToolchain") {
            group = "verification"
            description = "Resolves the JDK 21 launcher the screenshot host-test requires; fails loud if it cannot be provisioned."
            val launcher = toolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) }
            doLast {
                val home = launcher.get().metadata.installationPath.asFile.absolutePath
                logger.lifecycle("screenshot toolchain OK: JDK 21 launcher resolved at $home")
            }
        }
    }
}
```

Bootstrap ships it inert, so enabling the gate later needs no new files — just the one-line opt-in:

- **Catalog** (`tech-stack.md` § Library version catalog) — `roborazzi`/`robolectric`/`junit4` versions, the `roborazzi`/`roborazzi-compose`/`robolectric`/`junit4` libraries, the `roborazzi-gradle-plugin` library (build-logic classpath), and the `roborazzi` plugin alias.
- **Registration** — `build-logic/convention/build.gradle.kts` puts the roborazzi plugin on the build-logic classpath and registers the id:
  ```kotlin
  dependencies { implementation(libs.roborazzi.gradle.plugin) }
  gradlePlugin {
      plugins {
          register("screenshot.test.convention") {
              id = "screenshot.test.convention"
              implementationClass = "ScreenshotTestConventionPlugin"
          }
      }
  }
  ```
- **JDK 21 toolchain** — auto-provisioned by the `foojay-resolver-convention` plugin in `settings.gradle.kts` (this skill’s version-catalog reference); **no machine-specific JDK path** — never commit `org.gradle.java.installations.paths` to `gradle.properties` (it pins the build to one box and defeats foojay; the gate then silently fails to run elsewhere). The `verifyScreenshotToolchain` task above is the **authoritative** preflight: run `./gradlew :<module>:verifyScreenshotToolchain` once when the gate is first enabled for a module (it resolves the real launcher, download path included). See the screenshot-fidelity gate spec § "JDK 21".

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
- **Adding a Compose plugin directly without the convention** for a Compose UI module. The metrics + stability config are lost.
- **Skipping `applySafely`** in a new convention plugin. Double-apply errors on rebuilds.
- **Adding global `optIn` opt-ins per-feature** instead of in `KotlinMultiplatformConventionPlugin`. Inconsistent — some files have it, some don't.
- **Using `apply(plugin = ...)` instead of `pluginManager.applySafely(...)`** in convention plugins.
- **Reading the version catalog by hand inside a convention plugin** (e.g. building a `VersionCatalogsExtension` lookup inline). Use the `Project.libs` getter from `com/<org>/ProjectExtensions.kt` and call `libs.findLibrary("…").get()` through it.
- **Calling `withHostTest` or `withDeviceTest` anywhere outside its single owner** (`kmp.test.convention` / the internal device-lane helper). AGP fails the second call at configuration time; the convention layer is what keeps the failure impossible.
- **Applying `kmp.test.convention` transitively from `kotlin.multiplatform.convention`.** The foundation is opt-in per test-bearing module; transitive application fans `NO-SOURCE` host tasks and test dependencies across production-only modules.
- **`tasks.withType<Test>().configureEach { JDK 21 }` without narrowing.** Once general host tests exist, the broad form silently moves them onto the screenshot launcher. Scope by exact task name (`matching { it.name == "testAndroidHostTest" }`).
- **Hand-adding test libraries to a module or putting them in production source sets.** Capabilities are declared through `*.test.convention` plugins only; production classpaths and the XCFramework stay test-free.
- **Enabling the Android device lane on a Compose module.** CMP 1.10.3's `CopyResourcesToAndroidAssetsTask` fails configuration validation for the `deviceTest` compilation; the internal enabler helper refuses fail-closed.
