# Android runtime verification

Read [scenario evidence](evidence.md) before device actions. Locate the configured
Android SDK and its `platform-tools/adb` and `emulator/emulator`; list AVDs with
`emulator -list-avds` and select one from those results. The example below uses
the usual macOS SDK path; replace it with the installed path when different.

Select the emulator explicitly: inspect `adb devices -l`, verify the chosen
`emulator-<port>` serial with `adb -s "<emulator-serial>" emu avd name`, and use
that serial for every install, shell, log and screenshot command. Record it with
the evidence. Build targets remain in the [catalog](../SKILL.md#build-target-catalog).

Identifiers: application id `com.grippo.android`, launcher activity
`com.grippo.android.MainActivity`.

```bash
~/Library/Android/sdk/emulator/emulator -avd "<selected-avd>" -no-snapshot-load -no-boot-anim &   # background
adb devices -l
adb -s "<emulator-serial>" emu avd name
adb -s "<emulator-serial>" wait-for-device
./gradlew :androidApp:assembleDebug --console=plain
adb -s "<emulator-serial>" install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb -s "<emulator-serial>" shell am start -n com.grippo.android/.MainActivity
adb -s "<emulator-serial>" exec-out screencap -p > /private/tmp/<scratchpad from the brief>/<scenario>-<step>-android-<theme>.png
adb -s "<emulator-serial>" shell cmd uimode night yes        # dark theme; `no` for light
adb -s "<emulator-serial>" logcat -d -v time | rg "System.out|AndroidRuntime|grippo" | tail -200
```

- **Deeplinks** are delivered as a local-notification extra, not a URL scheme:
  `adb -s "<emulator-serial>" shell am start -n com.grippo.android/.MainActivity --es grippo_local_notification_deeplink weight_history`
  (keys: `training_draft`, `weight_history`; see `screen-api/.../deeplink/Deeplink.kt`).
  Cold start reads the extra in `onCreate`; a warm start reaches `onNewIntent`
  only when the activity's launch mode delivers it - inspect `AndroidManifest.xml`
  and add `-f 0x20000000` (single top) when needed.
- **Process death**: press Home (`adb -s "<emulator-serial>" shell input keyevent KEYCODE_HOME`), record
  the pid (`adb -s "<emulator-serial>" shell pidof com.grippo.android`), run
  `adb -s "<emulator-serial>" shell am kill com.grippo.android`, confirm `pidof` now prints nothing (a
  surviving pid means a warm resume, not restoration), then start the activity
  again. Android restores the Decompose stack and local drafts through
  `retainedComponent`'s saved state. This is the real restoration check; "Don't keep activities"
  (read `settings get global always_finish_activities` on the selected emulator,
  then use `adb -s "<emulator-serial>" shell settings put global always_finish_activities 1`
  and restore the recorded value afterwards) only proves Activity recreation
  with a live retained ViewModel.
- **Logs**: `AppLogger` prints to stdout in every build (logcat tag `System.out`,
  chunked) and appends to a file resolved in
  `toolkit/logger/src/androidMain/.../LogFileWriter.android.kt`; the in-app Debug
  screen (`:ui-screen-features:debug`, reachable through `RootRouter.Debug`) shows
  the file by category and can clear it. Prefer logcat plus the Debug screen.
- **Locale**: change it in the emulator's system Settings; `adb` cannot set the
  system locale without root. A language saved in Profile Settings overrides the
  system locale (`AppLocale`). Record the locale used in every screenshot name.
