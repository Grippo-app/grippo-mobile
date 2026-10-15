# Convention plugins and version catalog

Actual definitions live in `build-logic/convention/src/main/kotlin/` (ignore the stale, gitignored
copies under `build-logic/convention/bin/`); plugin ids are registered in
`build-logic/convention/build.gradle.kts`; aliases/versions live in `gradle/libs.versions.toml`;
global flags in `gradle.properties`. Inspect them before relying on a detail below.

## Plugin matrix

| Module type | Plugins |
|---|---|
| `:androidApp` | `android.application.convention` + `libs.plugins.compose.compiler` + `libs.plugins.jetbrains.compose` + `libs.plugins.google.services` + `libs.plugins.firebase.crashlytics` |
| Pure KMP module (no UI) | `android.library.convention` + `kotlin.multiplatform.convention` |
| KMP module with Compose UI | + `compose.multiplatform.convention` |
| Module with Koin bindings | + `koin.annotation.convention` |
| `:data-services:database` | + `room.convention` |
| `:shared` | `android.library.convention` + `kotlin.multiplatform.convention` + `ios.swiftpackage.convention` + `compose.multiplatform.convention` + `koin.annotation.convention` |

What each plugin does (verified):

- `kotlin.multiplatform.convention` (`KotlinMultiplatformConventionPlugin`): applies
  `org.jetbrains.kotlin.multiplatform`, `explicitApi()`, `iosArm64()`, `iosSimulatorArm64()`,
  `applyDefaultHierarchyTemplate()` and the global opt-ins for every source set:
  `ExperimentalMaterial3Api`, `ExperimentalTextApi`, `ExperimentalFoundationApi`,
  `ExperimentalComposeUiApi`, `ExperimentalLayoutApi`, `ExperimentalCoroutinesApi`,
  `ExperimentalSerializationApi`, `ExperimentalUnitApi`, `ExperimentalAnimationApi`,
  `kotlin.time.ExperimentalTime`, `ExperimentalForeignApi`, `ExperimentalNativeApi`,
  `DelicateDecomposeApi`, `ExperimentalSharedTransitionApi`, `ExperimentalUuidApi`,
  `ExperimentalDecomposeApi`. Source files do not repeat these with `@OptIn`.
- `android.library.convention`: applies `com.android.kotlin.multiplatform.library`; the KMP
  Android target gets `compileSdk = 37`, `minSdk = 26`, `namespace = "com.grippo"` (modules set
  their own namespace locally); JVM toolchain 19.
- `android.application.convention`: `com.android.application` with `compileSdk = 37`,
  `minSdk = 26`, `targetSdk = 36`, Java/JVM 19. `applicationId = "com.grippo.android"`,
  `versionCode`/`versionName` (19 / "2.0" today) and `namespace = "com.grippo.android.app"`
  stay in `androidApp/build.gradle.kts` ([release](release.md)).
- `compose.multiplatform.convention`: applies `org.jetbrains.kotlin.plugin.compose` and
  `org.jetbrains.compose`; excludes `/META-INF/{AL2.0,LGPL2.1}` from Android packaging.
- `koin.annotation.convention`: applies KSP, adds `koin.core` (implementation) and
  `koin.annotations` (api) to `commonMain`, `koin-ksp-compiler` on `kspCommonMainMetadata`, the
  generated `build/generated/ksp/metadata/commonMain/kotlin` source dir and
  `KOIN_CONFIG_CHECK = false` (todo until the next Koin Annotations release).
- `room.convention`: applies KSP, adds `androidx.room.runtime`, `sqlite-bundled` and `sqlite` to
  `commonMain`, the Room compiler on `kspAndroid`, `kspIosArm64` and `kspIosSimulatorArm64`, and
  `room.schemaLocation = $projectDir/schemas`.
- `ios.swiftpackage.convention` (`IosSwiftPackageConventionPlugin`): `XCFramework("shared")` over
  every Apple target with `baseName = "shared"`, `isStatic = true`, `linkerOpts("-lsqlite3")`,
  exporting `decompose.core`, `decompose.essenty` (lifecycle), `decompose.state.keeper`,
  `decompose.back.handler` and `:toolkit:{analytics,crash-reporting,messaging}:api`
  (`decompose-extensions` is not exported). Tasks: `:shared:assembleSharedDebugXCFramework` and
  `:shared:assembleSharedReleaseXCFramework` ([iOS framework](ios-framework.md)).

## Global build settings

`gradle.properties`: `org.gradle.configuration-cache=true`, `org.gradle.caching=true`,
`org.gradle.parallel=true`, `org.gradle.vfs.watch=true`, `org.gradle.workers.max=1` (peak memory
during native release linking), `kotlin.native.binary.smallBinary=true`,
`kotlin.incremental.native=true`, `ksp.useKSP2=true`. No Kotlin/Native garbage-collector override
is set (the former commented `gc=cms` line was removed on 2026-10-15); a commented flag is never
an active setting. `settings.gradle.kts` enables
`TYPESAFE_PROJECT_ACCESSORS` and `RepositoriesMode.FAIL_ON_PROJECT_REPOS`, so module scripts
declare no repositories.

## Version catalog

Read [the version catalog](../../../../gradle/libs.versions.toml) before claiming
any dependency version; platform pins belong to the convention sources above.

## Rules

- Pure KMP, Compose, Koin, Room and the shared export module apply exactly their matrix row;
  module scripts never repeat SDK versions, toolchain, repositories or opt-ins.
- Add a global opt-in in `KotlinMultiplatformConventionPlugin` only when the authorized scope
  covers that convention change; a source-level `@OptIn` is not a bypass.
- Adding catalog entries, renaming aliases, changing versions or module membership requires the
  authorized scope to cover it. Inspect every consumer of an alias before a rename or removal.

## Dependency updates

A version change needs three things: the user's scope naming it; the vendor's release notes or
migration guide for compatibility; and the verification set for the change -
`:androidApp:assembleDebug`, `:shared:assembleSharedDebugXCFramework`, the KSP/Room generation
both builds run, and an `iosApp` compile when the exported Swift API or Decompose-facing types
change. No version changes to silence a warning.
