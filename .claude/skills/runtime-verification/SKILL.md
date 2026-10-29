---
name: runtime-verification
description: Run scoped Grippo build checks and, when authorized, verify app behavior on Android emulators or iOS simulators. Owns the build-target catalog and conditional platform/scenario evidence procedures.
---

# Runtime verification

Source review and a green build prove nothing about runtime. This skill is how
build and runtime claims get evidence. Who runs what is decided by
[task-orchestrator](../task-orchestrator/SKILL.md#execution-loop): builds through
`build-verifier` (a single-module compile may run in the main session), runtime
scenarios through `runtime-verifier`.

## When to use

Use the catalog for scoped compilation or app packaging checks. Use the runtime
references only for authorized emulator/simulator scenarios. App execution
authorization follows the [verification policy](../../../CLAUDE.md#verification-policy).

## Preconditions

- Secure input preparation follows
  [secrets and configuration](../platform-build-toolkit/references/secrets-and-config.md).
  The main session owns synchronization because it can change tracked shell
  files; verifiers report a missing or stale input to that session.
- Evidence paths: the Bash guard of the verifier agents does not expand shell
  variables and refuses writes inside the repository. Use the literal absolute
  scratchpad path given in the brief for screenshots, logs and derived data.
- Gradle: `gradle.properties` already enables caching, configuration cache and
  `org.gradle.workers.max=1`. Never run `clean`, `build`, `assemble` or
  `--rerun-tasks` as routine; build the specific target. First builds take
  minutes; the XCFramework takes longer. Run long builds in the background and
  wait for `BUILD SUCCESSFUL`.

## Workflow

1. Select the scoped build target below under the verification policy.
2. For a compile-only brief, run the target and apply this skill's completion gate.
   Platform runtime and scenario references are not needed.
3. Before installing, launching or driving the app, read
   [scenario evidence](references/evidence.md) and the selected platform:
   [Android](references/android.md) or [iOS simulator](references/ios.md).
   Read both platform recipes only when both platforms are in scope.
4. Follow the evidence procedure for every requested runtime scenario and apply
   its runtime completion gate.

## Build target catalog

| Purpose | Command |
|---|---|
| Android compile check (default) | `./gradlew :androidApp:assembleDebug --console=plain` |
| Android install on the selected emulator | after `assembleDebug`, inspect `androidApp/build/outputs/apk/debug/output-metadata.json` for the APK name, then `adb -s "<emulator-serial>" install -r "<apk-path>"`; current output is `androidApp/build/outputs/apk/debug/androidApp-debug.apk` |
| iOS framework compile check | `./gradlew :shared:assembleSharedDebugXCFramework --console=plain` (output `shared/build/XCFrameworks/debug/shared.xcframework`) |
| Single module compile | `./gradlew :<group>:<module>:compileAndroidMain` (Android) or `:<group>:<module>:compileKotlinIosSimulatorArm64` (iOS); verified task names for KMP modules, see `./gradlew :<module>:tasks --all` |
| iOS app compile without device selection | after the XCFramework: `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' -arch arm64 -derivedDataPath <scratchpad>/ios-derived build` (current framework targets include `iosSimulatorArm64`; no simulator lookup or launch) |
| Release XCFramework | `./gradlew :shared:assembleSharedReleaseXCFramework` (only when asked) |

The Xcode project links the manually built XCFramework; the `iosApp/scripts/build-shared-framework.sh`
helper is not wired into the project's build phases. The `iosApp` scheme is
user-local (`xcuserdata`); report a missing scheme to the main session for recovery
under [CLI scheme recovery](../platform-build-toolkit/references/ios-framework.md#cli-scheme-recovery).
Replace every bracketed placeholder in the commands before running it.
The generic target checks compilation only. For an authorized runtime scenario,
use the selected simulator's explicit destination instead and follow the
[iOS procedure](references/ios.md); compilation alone authorizes no installation or launch.

## Completion gate

- Every required scoped build has an observed result or a concrete reason it
  could not run; report the exact command and relevant failure output.
- Identify the built revision and
  [input content](../task-orchestrator/references/change-attribution.md);
  relevant concurrent changes require rebuilding the affected artifacts before
  reusing their result.
- For runtime work, apply the
  [runtime completion gate](references/evidence.md#completion-gate) and its report.
  A build result alone establishes compilation, not runtime behavior.
