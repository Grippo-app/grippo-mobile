---
name: runtime-verification
description: Build, install, run and inspect the Grippo app on the Android emulator and iOS simulator for manual checks - navigation, dialogs, drafts after process death, deeplinks, themes, logs and screenshots. Use when a task needs runtime evidence the verification policy allows and the user permits running the app; owns the build-target catalog used by build verification.
---

# Runtime verification

Source review and a green build prove nothing about runtime. This skill is how
runtime claims get evidence. Who runs what is decided by
[task-orchestrator](../task-orchestrator/SKILL.md#execution-loop): builds through
`build-verifier` (a single-module compile may run in the main session), runtime
scenarios through `runtime-verifier`; the catalog below is theirs to follow.

## Preconditions

- Secure inputs synced once per checkout: `./gradlew syncSecureConfigs` copies
  Firebase/Google configs from `secure/` (see
  [secrets and configuration](../platform-build-toolkit/references/secrets-and-config.md)).
  Without `secure/` the Android build fails at the google-services step; report it.
- Tooling on this machine: Android SDK at `~/Library/Android/sdk` (`platform-tools/adb`,
  `emulator/emulator`; `Pixel_10_Pro` is the AVD known to exist), Xcode with iOS
  simulator runtimes. List what is actually there with `emulator -list-avds` and
  `xcrun simctl list devices available`; do not assume a booted device.
- Sign-in: agents never type credentials. The user signs in on the emulator or
  simulator before the scenarios, or names a test account kept under `secure/`;
  a scenario that needs a logged-in state is blocked until then and says so.
- Evidence paths: the Bash guard of the verifier agents does not expand shell
  variables and refuses writes inside the repository. Use the literal absolute
  scratchpad path given in the brief for screenshots, logs and derived data.
- Gradle: `gradle.properties` already enables caching, configuration cache and
  `org.gradle.workers.max=1`. Never run `clean`, `build`, `assemble` or
  `--rerun-tasks` as routine; build the specific target. First builds take
  minutes; the XCFramework takes longer. Run long builds in the background and
  wait for `BUILD SUCCESSFUL`.

## Build target catalog

| Purpose | Command |
|---|---|
| Android compile check (default) | `./gradlew :androidApp:assembleDebug --console=plain` |
| Android install on the running device | `./gradlew :androidApp:installDebug --console=plain` |
| iOS framework compile check | `./gradlew :shared:assembleSharedDebugXCFramework --console=plain` (output `shared/build/XCFrameworks/debug/shared.xcframework`) |
| Single module compile | `./gradlew :<group>:<module>:compileAndroidMain` (Android) or `:<group>:<module>:compileKotlinIosSimulatorArm64` (iOS); verified task names for KMP modules, see `./gradlew :<module>:tasks --all` |
| iOS app for the simulator | after the XCFramework: `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator -destination 'platform=iOS Simulator,name=<device>' -derivedDataPath <scratchpad>/ios-derived build` (derived data in the scratchpad, not the repository) |
| Release XCFramework | `./gradlew :shared:assembleSharedReleaseXCFramework` (only when asked) |

The Xcode project links the manually built XCFramework; the `iosApp/scripts/build-shared-framework.sh`
helper is not wired into the project's build phases. The `iosApp` scheme is
user-local (`xcuserdata`); report a missing scheme to the main session for recovery
under [CLI scheme recovery](../platform-build-toolkit/references/ios-framework.md#cli-scheme-recovery).

## Android

Identifiers: application id `com.grippo.android`, launcher activity
`com.grippo.android.MainActivity`, debug build type unsigned-debug.

```bash
~/Library/Android/sdk/emulator/emulator -avd Pixel_10_Pro -no-snapshot-load -no-boot-anim &   # background
~/Library/Android/sdk/platform-tools/adb wait-for-device
./gradlew :androidApp:installDebug --console=plain
adb shell am start -n com.grippo.android/.MainActivity
adb exec-out screencap -p > /private/tmp/<scratchpad from the brief>/<scenario>-<step>-android-<theme>.png
adb shell cmd uimode night yes        # dark theme; `no` for light
adb logcat -d -v time | grep -E "System.out|AndroidRuntime|grippo" | tail -200
```

- **Deeplinks** are delivered as a local-notification extra, not a URL scheme:
  `adb shell am start -n com.grippo.android/.MainActivity --es grippo_local_notification_deeplink weight_history`
  (keys: `training_draft`, `weight_history`; see `screen-api/.../deeplink/Deeplink.kt`).
  Cold start reads the extra in `onCreate`; a warm start reaches `onNewIntent`
  only when the activity's launch mode delivers it - inspect `AndroidManifest.xml`
  and add `-f 0x20000000` (single top) when needed.
- **Process death**: press Home (`adb shell input keyevent KEYCODE_HOME`), record
  the pid (`adb shell pidof com.grippo.android`), run
  `adb shell am kill com.grippo.android`, confirm `pidof` now prints nothing (a
  surviving pid means a warm resume, not restoration), then start the activity
  again. Android restores the Decompose stack and local drafts through
  `retainedComponent`'s saved state. This is the real restoration check; "Don't keep activities"
  (`adb shell settings put global always_finish_activities 1`, reset to `0`
  afterwards) only proves Activity recreation with a live retained ViewModel.
- **Logs**: `AppLogger` prints to stdout in every build (logcat tag `System.out`,
  chunked) and appends to a file resolved in
  `toolkit/logger/src/androidMain/.../LogFileWriter.android.kt`; the in-app Debug
  screen (`:ui-screen-features:debug`, reachable through `RootRouter.Debug`) shows
  the file by category and can clear it. Prefer logcat plus the Debug screen.
- **Locale**: change it in the emulator's system Settings; `adb` cannot set the
  system locale without root. Record the locale used in every screenshot name.

## iOS simulator

Identifiers: bundle id `com.grippo.app`, scheme `iosApp`, app product
`<scratchpad>/ios-derived/Build/Products/Debug-iphonesimulator/iosApp.app` when
built with the catalog command above.

```bash
xcrun simctl list devices available
xcrun simctl boot "<device name>"           # or use the simulator tool's attach/launch
xcrun simctl install booted <scratchpad>/ios-derived/Build/Products/Debug-iphonesimulator/iosApp.app
xcrun simctl launch booted com.grippo.app
xcrun simctl io booted screenshot /private/tmp/<scratchpad from the brief>/<scenario>-<step>-ios-<theme>.png
xcrun simctl ui booted appearance dark      # or light
xcrun simctl spawn booted log stream --level debug --predicate 'process == "iosApp"'
```

- When the iOS Simulator tool is available, prefer it: `attach` first so the user
  can watch, `launch` with the `.app` path, `tap`/`swipe`/`text` for input,
  `screenshot` for evidence. Coordinates are device points.
- **Background checkpoint and restoration**: the app saves one checkpoint when the
  scene enters background and consumes it at the next launch
  (`iosApp/iosApp/iOSApp.swift`, `ApplicationStateStore.swift`). Verify with:
  send to background (home gesture or `button HOME`), then
  `xcrun simctl terminate booted com.grippo.app`, then launch again. Before
  terminating, prove the checkpoint exists: the container path comes from
  `xcrun simctl get_app_container booted com.grippo.app data` (run it on its own;
  the guard rejects `$(...)`), then `ls "<container>/Library/Application Support/GrippoRestoration/state.json"`.
  Restoration also requires the same binary, build number and archive format
  between save and relaunch; `ApplicationStateStore.restore()` returns nil
  silently on a mismatch. Killing the app while active, or terminating before
  the background save completes, has no restoration guarantee and must not be
  reported as a failure of the mechanism.
- **App log file**: `<container>/tmp/grippo/logs/app.log`, with `<container>` from
  the `get_app_container` command above.
- **Deeplinks** on iOS arrive only through local notifications; there is no
  URL scheme for them (the only registered scheme belongs to Google sign-in).

## Scenario discipline

1. Record the revision under test: `git rev-parse --short HEAD` and the count of
   dirty files. Evidence without a revision is not evidence.
2. Write each scenario as steps with an expected outcome before running it.
3. After each meaningful step take a screenshot named
   `<scenario>-<step>-<platform>-<theme>[-<locale>].png` in the session
   scratchpad (never inside the repository) and note the observed state.
4. On a failure capture the log window around it and stop that scenario; do not
   "fix" the app or retry blindly.
5. Restore every device setting you changed (appearance, locale, developer
   options, always-finish-activities) and say so.

## What runtime evidence establishes

- A screenshot proves the rendered state on that device, OS version, theme and
  locale at that revision. It does not prove other devices, font scales or motion.
- A completed restoration scenario proves the saved-state path that was used
  (Android kill/relaunch or iOS background/terminate/relaunch), not every cold start.
- Observed analytics logging proves emission from the app, not delivery to
  Firebase or admin processing.

## Report

```
Revision: <sha> (+<n> dirty files); Device: <name, OS>; Build: <command -> result>
| Scenario | Step | Expected | Observed | Evidence | Result |
Failures with log excerpts; Not exercised with reasons; Settings restored: yes/no
```
