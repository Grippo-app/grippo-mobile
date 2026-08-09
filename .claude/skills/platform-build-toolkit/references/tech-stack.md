# Tech stack — languages, versions, libraries, gradle.properties

These are the pinned toolkit/build baseline for the project: language and runtime versions, the canonical version catalog (`gradle/libs.versions.toml`), and the project-wide build flags (`gradle.properties`). Subsequent work assumes these versions; do not override them per module.

## Languages and versions

### Mandatory versions

| Concern | Version | Notes |
|---|---|---|
| Kotlin | **2.3.21** | `explicitApi()` enabled globally via convention plugin |
| KSP | **2.3.4** (tracks Kotlin 2.3.x) | `ksp.useKSP2=true` |
| AGP (Android Gradle Plugin) | **9.0.1** | uses `com.android.kotlin.multiplatform.library` for KMP modules |
| Compose Multiplatform | **1.10.3** | metrics + stability config enabled |
| Compose Compiler | matches Kotlin (2.3.21) | applied via `org.jetbrains.kotlin.plugin.compose` |
| Decompose | **3.5.0** | + Essenty 2.5.0 (`lifecycle`, `state-keeper`, `back-handler`) |
| Koin | **4.2.1** | + Koin Annotations **2.3.1** |
| Ktor | **3.4.3** | Android engine + Darwin engine |
| kotlinx-serialization | **1.11.0** | JSON only |
| kotlinx-datetime | **0.8.0** | for `LocalDateTime`/`LocalDate`/`DatePeriod`/`DateTimePeriod`/`TimeZone`/`Month`/`DayOfWeek` (`Instant`/`Clock`/`Duration` come from stdlib `kotlin.time`) |
| kotlinx-coroutines | **1.11.0** | `core` + `play-services` (Android) |
| kotlinx-collections-immutable | **0.4.0** | mandatory in all `@Immutable` state |
| Room | **2.8.4** multiplatform | + `androidx.sqlite-bundled` **2.6.2** |
| AndroidX DataStore | **1.2.1** | preferences-core |
| Coil | **3.4.0** | `coil-compose` + `coil-network-ktor3` |
| Firebase BOM | **34.13.0** | Android-only: Analytics + Crashlytics + Messaging |

### Targets

#### Android

- `compileSdk = 36`
- `minSdk = 26`
- `targetSdk = 36` (apps only)
- JVM toolchain **19**
- AndroidX enabled, non-transitive R class enabled

#### iOS (Kotlin/Native)

- `iosX64`, `iosArm64`, `iosSimulatorArm64`
- `IPHONEOS_DEPLOYMENT_TARGET = 16.0`
- Static `XCFramework` named `<iosFrameworkName>` (default `shared`)
- Linker option `-lsqlite3` (required for Room/SQLite)
- Keep the Kotlin/Native GC at the toolchain default. Enable an explicit GC mode only after profiling the generated project on its supported iOS versions.
- `smallBinary = true` — smaller frameworks
- Re-exports Decompose API + `:data-services:firebase` so Swift can see them

### Source-set layout

Every KMP module uses the default hierarchy template (`applyDefaultHierarchyTemplate()`). Source roots:

```
src/
  commonMain/kotlin/...
  androidMain/kotlin/...
  iosMain/kotlin/...          (shared across iosArm64, iosX64, iosSimulatorArm64)
  commonMain/composeResources/  (Compose Multiplatform Resources — strings/drawables/fonts)
```

The KSP-generated sources live at `build/generated/ksp/metadata/commonMain/kotlin` and are added as an additional `commonMain` source root by the relevant convention plugins (Koin annotations, Room).

### Global `optIn`

The following experimental APIs are opted in **globally** in `KotlinMultiplatformConventionPlugin`. Do **not** repeat them via `@OptIn` in source files:

```kotlin
androidx.compose.material3.ExperimentalMaterial3Api
androidx.compose.ui.text.ExperimentalTextApi
androidx.compose.foundation.ExperimentalFoundationApi
androidx.compose.ui.ExperimentalComposeUiApi
androidx.compose.foundation.layout.ExperimentalLayoutApi
kotlinx.coroutines.ExperimentalCoroutinesApi
androidx.compose.ui.unit.ExperimentalUnitApi
androidx.compose.animation.ExperimentalAnimationApi
kotlin.time.ExperimentalTime
kotlinx.cinterop.ExperimentalForeignApi
com.arkivanov.decompose.DelicateDecomposeApi
androidx.compose.animation.ExperimentalSharedTransitionApi
kotlin.uuid.ExperimentalUuidApi
com.arkivanov.decompose.ExperimentalDecomposeApi
```

Adding a new global `optIn` is a deliberate, separate task — it touches every module.

### Explicit API mode

`explicitApi()` is enabled in `KotlinMultiplatformConventionPlugin`. Every top-level declaration in **every** module **must** carry a visibility modifier (`public`, `internal`, `private`). The compiler will fail otherwise. This is intentional — see the Kotlin style conventions.

### Tooling defaults

- `kotlin.code.style=official`
- `kotlin.incremental.native=true`
- Gradle: `caching=true`, `configuration-cache=true`, `daemon=true`, `parallel=true`, `vfs.watch=true`
- `org.gradle.workers.max=1` — peak memory during Kotlin/Native release linking
- Heap budgets: `org.gradle.jvmargs=-Xmx8g`, `kotlin.daemon.jvmargs=-Xmx2g`, `kotlin.native.jvmArgs=-Xmx6g`

## Library version catalog (libs.versions.toml)

This is the canonical `gradle/libs.versions.toml`. Do **not** add libraries or bump versions silently — both are deliberate, separate tasks. The catalog uses dashed (kebab-case) aliases (e.g. `kotlinx-coroutines-core`); Gradle's type-safe accessors translate each `-` into a `.` (so the alias becomes `libs.kotlinx.coroutines.core`).

### `gradle/libs.versions.toml`

```toml
[versions]
agp = "9.0.1"
kotlin = "2.3.21"
coroutines = "1.11.0"
serialization = "1.11.0"
compose-plugin = "1.10.3"
activity-compose = "1.13.0"
datetime = "0.8.0"
koin = "4.2.1"
koin-annotations = "2.3.1"
decompose = "3.5.0"
decompose-essenty = "2.5.0"
room = "2.8.4"
ksp = "2.3.4"
ktor = "3.4.3"
immutable-collections = "0.4.0"
sqlite = "2.6.2"
datastore = "1.2.1"
appcompat = "1.7.1"
coil = "3.4.0"
core-splashscreen = "1.2.0"
google-identity = "1.2.0"
credentials = "1.6.0"
firebase-bom = "34.13.0"
google-services-plugin = "4.4.4"
firebase-crashlytics-plugin = "3.0.7"
detekt = "1.23.7"
roborazzi = "1.64.0"
robolectric = "4.16"
junit4 = "4.13.2"
turbine = "1.2.1"
androidx-test-runner = "1.7.0"
androidx-test-core = "1.7.0"
kover = "0.9.9"
# Android artifact line CMP `compose-plugin` 1.10.3 resolves to (observed via
# dependency resolution, not guessed). Moves in lockstep with `compose-plugin`.
compose-ui-test-manifest = "1.10.5"

[libraries]
datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "datetime" }
immutable-collections = { module = "org.jetbrains.kotlinx:kotlinx-collections-immutable", version.ref = "immutable-collections" }

# KotlinX
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-play-services = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-play-services", version.ref = "coroutines" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }

# AndroidX
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activity-compose" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
androidx-core-splashscreen = { group = "androidx.core", name = "core-splashscreen", version.ref = "core-splashscreen" }
androidx-credentials = { group = "androidx.credentials", name = "credentials", version.ref = "credentials" }
androidx-credentials-play-services-auth = { group = "androidx.credentials", name = "credentials-play-services-auth", version.ref = "credentials" }
google-identity-googleid = { module = "com.google.android.libraries.identity.googleid:googleid", version.ref = "google-identity" }

# Gradle plugins (consumed by build-logic/convention)
android-gradle-plugin = { module = "com.android.tools.build:gradle", version.ref = "agp" }
kotlin-gradle-plugin = { module = "org.jetbrains.kotlin:kotlin-gradle-plugin", version.ref = "kotlin" }
compose-gradle-plugin = { module = "org.jetbrains.kotlin:compose-compiler-gradle-plugin", version.ref = "kotlin" }
ksp-plugin-api = { module = "com.google.devtools.ksp:symbol-processing-gradle-plugin", version.ref = "ksp" }

# Firebase (Android shell only)
android-firebase-bom = { module = "com.google.firebase:firebase-bom", version.ref = "firebase-bom" }
android-firebase-analytics = { module = "com.google.firebase:firebase-analytics" }
android-firebase-crashlytics = { module = "com.google.firebase:firebase-crashlytics" }
android-firebase-messaging = { module = "com.google.firebase:firebase-messaging" }

# Decompose
decompose-core = { module = "com.arkivanov.decompose:decompose", version.ref = "decompose" }
decompose-extensions = { module = "com.arkivanov.decompose:extensions-compose-experimental", version.ref = "decompose" }
decompose-essenty = { module = "com.arkivanov.essenty:lifecycle", version.ref = "decompose-essenty" }
decompose-back-handler = { module = "com.arkivanov.essenty:back-handler", version.ref = "decompose-essenty" }
decompose-state-keeper = { module = "com.arkivanov.essenty:state-keeper", version.ref = "decompose-essenty" }

# Room
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
sqlite-bundled = { module = "androidx.sqlite:sqlite-bundled", version.ref = "sqlite" }
sqlite = { module = "androidx.sqlite:sqlite", version.ref = "sqlite" }

# DataStore
androidx-datastore-preferences-core = { module = "androidx.datastore:datastore-preferences-core", version.ref = "datastore" }

# Ktor
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-android = { module = "io.ktor:ktor-client-android", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-client-logging = { module = "io.ktor:ktor-client-logging", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-auth = { module = "io.ktor:ktor-client-auth", version.ref = "ktor" }

# Coil
coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
coil-network-ktor = { module = "io.coil-kt.coil3:coil-network-ktor3", version.ref = "coil" }

# Koin
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
koin-annotations = { module = "io.insert-koin:koin-annotations", version.ref = "koin-annotations" }
koin-ksp-compiler = { module = "io.insert-koin:koin-ksp-compiler", version.ref = "koin-annotations" }

# Detekt
detekt-api = { module = "io.gitlab.arturbosch.detekt:detekt-api", version.ref = "detekt" }
detekt-test = { module = "io.gitlab.arturbosch.detekt:detekt-test", version.ref = "detekt" }

# Screenshot test (Roborazzi — opt-in screenshot-fidelity gate; androidHostTest + build-logic classpath only; see the implement-figma skill)
roborazzi = { module = "io.github.takahirom.roborazzi:roborazzi", version.ref = "roborazzi" }
roborazzi-compose = { module = "io.github.takahirom.roborazzi:roborazzi-compose", version.ref = "roborazzi" }
roborazzi-gradle-plugin = { module = "io.github.takahirom.roborazzi:roborazzi-gradle-plugin", version.ref = "roborazzi" }
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
junit4 = { module = "junit:junit", version.ref = "junit4" }

# General test foundation (mandatory-test pipeline; wired ONLY through the
# opt-in `kmp.test.convention` + capability conventions — never added to a
# module by hand and never reachable from production configurations)
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }
koin-test = { module = "io.insert-koin:koin-test", version.ref = "koin" }
androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
androidx-test-runner = { module = "androidx.test:runner", version.ref = "androidx-test-runner" }
androidx-test-core = { module = "androidx.test:core", version.ref = "androidx-test-core" }
compose-ui-test-manifest = { module = "androidx.compose.ui:ui-test-manifest", version.ref = "compose-ui-test-manifest" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-kotlin-multiplatform-library = { id = "com.android.kotlin.multiplatform.library", version.ref = "agp" }
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
jetbrains-compose = { id = "org.jetbrains.compose", version.ref = "compose-plugin" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
room = { id = "androidx.room", version.ref = "room" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
kotlin-parcelize = { id = "org.jetbrains.kotlin.plugin.parcelize", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
google-services = { id = "com.google.gms.google-services", version.ref = "google-services-plugin" }
firebase-crashlytics = { id = "com.google.firebase.crashlytics", version.ref = "firebase-crashlytics-plugin" }
detekt = { id = "io.gitlab.arturbosch.detekt", version.ref = "detekt" }
roborazzi = { id = "io.github.takahirom.roborazzi", version.ref = "roborazzi" }
kover = { id = "org.jetbrains.kotlinx.kover", version.ref = "kover" }
```

> **Note on `decompose-essenty`:** the correct spelling is `decompose-essenty` (the Essenty library). If you copy a catalog from somewhere that has the misspelled `decompose-essently`, fix it. The version-ref and module coordinates above are canonical.

### Adding a new library

Any new dependency requires a deliberate decision. Steps:

1. Discuss with the team that it is the right pick (build vs reuse, license, weight on iOS framework, transitive deps).
2. Add a `[versions]` entry.
3. Add the `[libraries]` entry — prefer `version.ref` over inline versions.
4. Reference it from the **module-level** `build.gradle.kts` via the type-safe accessor. Gradle splits each `-` in the catalog alias into a path separator — so `kotlinx-coroutines-core` becomes `libs.kotlinx.coroutines.core`, `androidx-core-splashscreen` becomes `libs.androidx.core.splashscreen`, and `decompose-back-handler` becomes `libs.decompose.back.handler`.
5. If it must be exported through the iOS XCFramework (visible from Swift), add an `export(libs.find...)` line in `IosSwiftPackageConventionPlugin` — only do this if the public API of `:shared`/`:data-services:firebase` exposes types from that library to Swift.

### Notable choices and why

- **Decompose**, not Compose Navigation / Voyager — type-safe routing, lifecycle-aware retained components, multiplatform state keeper.
- **Koin Annotations**, not Hilt — Hilt is Android-only; manual Koin DSL is verbose; annotations + KSP scale.
- **Room Multiplatform**, not SQLDelight — single ORM with `@Entity`/`@Relation` ergonomics on both platforms.
- **Ktor**, not Retrofit — only Ktor works on both platforms.
- **kotlinx-serialization**, not Moshi/Gson — only kotlinx-serialization works on KMP and integrates with Decompose `StateKeeper`.
- **kotlinx-collections-immutable** — required for Compose stability inference (`ImmutableList`, `ImmutableSet`).
- **Coil 3 + `coil-network-ktor3`**, not Glide / Kamel — Coil 3 is multiplatform and shares the Ktor client with the rest of the app.
- **Roborazzi** (opt-in, test-only), not Paparazzi — the screenshot-fidelity gate renders on Robolectric in the KMP `androidHostTest` source set; Paparazzi supports neither `com.android.kotlin.multiplatform.library` nor AGP 9. Wired via the `screenshot.test.convention` plugin and inert unless a `figmaEnabled` screen module opts in. See the implement-figma skill (`orchestrator/figma/skill/`) § screenshot-fidelity gate.
- **`kotlin.test` + hand-written fakes**, not Kotest/MockK/Mockito — one platform-neutral runner/assertion API in `commonTest`; no second DSL while `kotlin.test` suffices, and no JVM instrumentation/reflection mocking that a KMP architecture cannot carry to Native. Test doubles are hand-written fakes at boundaries.
- **Turbine + `kotlinx-coroutines-test`** for Flow/virtual-time evidence on both the Android host and iOS simulator lanes (Turbine upstream depends on the experimental `UnconfinedTestDispatcher` — treat its upgrades as deliberate, not mechanical).
- **`ktor-client-mock`**, not real HTTP — product tests stay hermetic; the MockEngine handler validates the request before responding.
- **No JVM desktop target for tests** — it would change the target/publication graph and can mask Android/iOS divergences; the host lane is the Android host test, not a desktop shortcut.

### Version interdependencies

Several versions in the catalog are not independent and must move together:

- `kotlin` + `compose-compiler` — the `compose-compiler` plugin tracks the Kotlin compiler version exactly. Bumping `kotlin` requires bumping `compose-compiler` to a matching release.
- `kotlin` + `agp` — the Kotlin/AGP compatibility matrix constrains which AGP versions work with a given Kotlin release; verify against JetBrains' published matrix before bumping either.
- `compose-plugin` (JetBrains Compose Multiplatform) — follows its own release cadence, but each release is generally tied to a Kotlin version range; check the Compose Multiplatform release notes when bumping `kotlin`.
- `compose-plugin` + `compose-ui-test-manifest` — the manifest artifact must match the androidx Compose UI line the CMP release resolves to (observe it via dependency resolution after any `compose-plugin` bump; 1.10.3 → androidx ui 1.10.5). A mismatched pin breaks the Robolectric host lane for Compose UI scenarios.

## gradle.properties

This file is **mandatory** at the project root. Each setting is intentional. Document any deviation in a code review.

```properties
# Gradle
org.gradle.jvmargs=-Xmx8g -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.vfs.watch=true

# Limit parallelism to reduce peak memory during Native release linking
org.gradle.workers.max=1

# Kotlin
kotlin.code.style=official

# Kotlin/Native (iOS)
kotlin.native.binary.smallBinary=true
kotlin.incremental.native=true

# kotlin.native.binary.gc=cms   # optional override; enable only when supported by the pinned Kotlin/Native version and justified by project benchmarks.

# Increase heap for Kotlin/Native compiler (konanc runs on JVM); default is small (~3g)
kotlin.native.jvmArgs=-Xmx6g -XX:+HeapDumpOnOutOfMemoryError

# Kotlin daemon heap (JVM/analysis tasks)
kotlin.daemon.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m

# Android
android.useAndroidX=true
android.nonTransitiveRClass=true

# KSP
ksp.useKSP2=true
ksp.verbose=false
```

### Rationale per setting

#### Gradle

- **`org.gradle.jvmargs=-Xmx8g`** — Compose Multiplatform + Kotlin/Native + Room/Koin KSP routinely peak above 4 GB. 8 GB is safe headroom.
- **`org.gradle.caching=true`** — local build cache; saves significant time on incremental builds.
- **`org.gradle.configuration-cache=true`** — configuration cache; further speeds up incremental builds. **Note:** convention plugins must be configuration-cache-compatible (no eager `project.afterEvaluate` access to other projects' state).
- **`org.gradle.parallel=true`** — runs tasks across modules in parallel where the task graph allows.
- **`org.gradle.workers.max=1`** — **counter-intuitive but required**. Kotlin/Native release linking can consume 4–6 GB per worker. With parallel + multiple iOS targets, peak memory exceeds typical CI machines. Limit one worker → linking serializes → peak stays manageable.

#### Kotlin/Native

- **`kotlin.native.binary.smallBinary=true`** — strips dead code more aggressively. Reduces iOS framework size.
- **`kotlin.native.binary.gc=cms`** — optional collector override. Leave it unset by default; enable it only when the pinned Kotlin/Native version documents support for it and project benchmarks justify the change.
- **`kotlin.incremental.native=true`** — incremental Kotlin/Native compilation. Faster Debug iteration.
- **`kotlin.native.jvmArgs=-Xmx6g`** — `konanc` runs on the JVM; default heap is too small for a project of this size.

#### Kotlin daemon

- **`kotlin.daemon.jvmargs=-Xmx2g`** — analysis tasks and Compose metrics generation need more than the default. 2 GB is enough.

#### Android

- **`android.useAndroidX=true`** — mandatory; the project uses AndroidX.
- **`android.nonTransitiveRClass=true`** — each module gets its own R class; reduces APK size and resource conflicts.

#### KSP

- **`ksp.useKSP2=true`** — KSP2 is the current generation (faster, better Kotlin compiler integration).
- **`ksp.verbose=false`** — reduces noise in build output.

### When to deviate

| Situation | Allowed change |
|---|---|
| CI machine has < 16 GB RAM | Drop `org.gradle.jvmargs` to `-Xmx4g`; expect slower builds |
| Local dev only on iOS | keep `kotlin.native.binary.gc=cms` commented (crash risk on K/N 2.3.21); re-enable only after iOS smoke-testing — see § Kotlin/Native |
| Build is hitting "OutOfMemoryError" in `konanc` | Bump `kotlin.native.jvmArgs` to `-Xmx8g` |
| Build is hitting OOM in Kotlin daemon | Bump `kotlin.daemon.jvmargs` to `-Xmx4g` |
| Configuration cache breaks after adding a plugin | Fix the plugin (don't disable the cache); see Gradle's CC docs |

Never disable `configuration-cache` to "fix" a problem — find the offending task or plugin.
