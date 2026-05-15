# Languages and Versions

## Mandatory versions

| Concern | Version | Notes |
|---|---|---|
| Kotlin | **2.3.21** | `explicitApi()` enabled globally via convention plugin |
| KSP | **2.3.4** (matches Kotlin) | `ksp.useKSP2=true` |
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

## Targets

### Android

- `compileSdk = 36`
- `minSdk = 26`
- `targetSdk = 36` (apps only)
- JVM toolchain **19**
- AndroidX enabled, non-transitive R class enabled

### iOS (Kotlin/Native)

- `iosX64`, `iosArm64`, `iosSimulatorArm64`
- Static `XCFramework` named `shared`
- Linker option `-lsqlite3` (required for Room/SQLite)
- GC: `cms` (concurrent mark+sweep) — reduces stop-the-world pauses on iOS
- `smallBinary = true` — smaller frameworks
- Re-exports Decompose API + `:data-services:firebase` so Swift can see them

## Source-set layout

Every KMP module uses the default hierarchy template (`applyDefaultHierarchyTemplate()`). Source roots:

```
src/
  commonMain/kotlin/...
  androidMain/kotlin/...
  iosMain/kotlin/...          (shared across iosArm64, iosX64, iosSimulatorArm64)
  commonMain/composeResources/  (Compose Multiplatform Resources — strings/drawables/fonts)
```

The KSP-generated sources live at `build/generated/ksp/metadata/commonMain/kotlin` and are added as an additional `commonMain` source root by the relevant convention plugins (Koin annotations, Room).

## Global `optIn`

The following experimental APIs are opted in **globally** in `KotlinMultiplatformConventionPlugin`. Do **not** repeat them via `@OptIn` in source files:

```
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

## Explicit API mode

`explicitApi()` is enabled in `KotlinMultiplatformConventionPlugin`. Every top-level declaration in **every** module **must** carry a visibility modifier (`public`, `internal`, `private`). The compiler will fail otherwise. This is intentional — see `09-conventions/01-kotlin-style.md`.

## Tooling defaults

- `kotlin.code.style=official`
- `kotlin.incremental.native=true`
- Gradle: `caching=true`, `configuration-cache=true`, `daemon=true`, `parallel=true`, `vfs.watch=true`
- `org.gradle.workers.max=1` — peak memory during Kotlin/Native release linking
- Heap budgets: `org.gradle.jvmargs=-Xmx8g`, `kotlin.daemon.jvmargs=-Xmx2g`, `kotlin.native.jvmArgs=-Xmx6g`
