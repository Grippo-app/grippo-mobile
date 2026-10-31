# iOS simulator runtime verification

Read [scenario evidence](evidence.md) before device actions. Locate Xcode's
simulator runtimes, list devices with `xcrun simctl list devices available`, and
select a simulator UDID from those results. Use that UDID in every `simctl`
action and record it with the evidence. Build targets remain in the
[catalog](../SKILL.md#build-target-catalog).

Identifiers: bundle id `com.grippo.app`, scheme `iosApp`, app product
`<scratchpad>/ios-derived/Build/Products/Debug-iphonesimulator/Grippo.app` when
built with the [catalog command](../SKILL.md#build-target-catalog)
(`PRODUCT_NAME` in `iosApp/Configuration/Config.xcconfig`).

```bash
xcrun simctl list devices available
xcrun simctl boot "<simulator-udid>"           # or use the simulator tool's attach/launch
xcrun simctl install "<simulator-udid>" <scratchpad>/ios-derived/Build/Products/Debug-iphonesimulator/Grippo.app
xcrun simctl launch "<simulator-udid>" com.grippo.app
xcrun simctl io "<simulator-udid>" screenshot "<absolute-scratchpad>/<scenario>-<step>-ios-<theme>.png"
xcrun simctl ui "<simulator-udid>" appearance dark      # or light
xcrun simctl spawn "<simulator-udid>" log stream --level debug --predicate 'process == "Grippo"'
```

- When the iOS Simulator tool is available, prefer it: `attach` first so the user
  can watch, `launch` with the `.app` path, `tap`/`swipe`/`text` for input,
  `screenshot` for evidence. Coordinates are device points.
- **Background checkpoint and restoration**: the app saves one checkpoint when the
  scene enters background and consumes it at the next launch
  (`iosApp/iosApp/iOSApp.swift`, `ApplicationStateStore.swift`). Verify with:
  send to background (home gesture or `button HOME`), then
  `xcrun simctl terminate "<simulator-udid>" com.grippo.app`, then launch again. Before
  terminating, prove the checkpoint exists: the container path comes from
  `xcrun simctl get_app_container "<simulator-udid>" com.grippo.app data` (run it on its own;
  the guard rejects `$(...)`), then `ls "<container>/Library/Application Support/GrippoRestoration/state.json"`.
  Restoration also requires the same app version, build number and archive format
  between save and relaunch; `ApplicationStateStore.restore()` returns nil
  silently on a mismatch. Killing the app while active, or terminating before
  the background save completes, has no restoration guarantee and must not be
  reported as a failure of the mechanism.
- **App log file**: `<container>/tmp/grippo/logs/app.log`, with `<container>` from
  the `get_app_container` command above.
- **Deeplinks** on iOS arrive only through local notifications; there is no
  URL scheme for them (the only registered scheme belongs to Google sign-in).
