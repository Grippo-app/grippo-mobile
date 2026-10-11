# Light theme implementation

## Crash remediation — 2026-10-11

Scope: diagnose the downloaded Apple reports and Android Crashlytics issues, fix confirmed current defects, and verify the artifacts that will actually ship. Existing theme work below is retained.

1. Inventory crash reports by incident, crashed thread, app build, and hardware. Trace the Skia lifetime failure to the upstream fix; inspect the separate UIKit invalid release without assuming it has the same cause.
2. Verify the resolved Compose renderer and make Xcode build the matching Kotlin framework before linking. Preserve the fixed Surface renderer; never hide native crashes with exception suppression. Verify Debug simulator and Release device artifacts.
3. Make symbol upload part of the archive pipeline with declared sandbox inputs. Verify project parsing and dSYM/binary UUIDs. Improve Kotlin nonfatal reports so the actual Kotlin frames and per-event metadata survive the Swift boundary.
4. Audit lifecycle/cancellation paths reached by rendering, networking and app foreground/background transitions. Fix only demonstrated defects, with focused regression tests.
5. Build Android and iOS, exercise cancellation and renderer/lifecycle transitions, review the diff, and record remaining physical-device/release verification requirements honestly.

Acceptance: every change has a demonstrated mechanism; shipped dependencies are verifiable; no stale Kotlin binary can silently bypass the normal build; archive symbols upload automatically; cancellation stays cancellation; focused tests pass. Absence of simulator crashes is not proof of a physical Metal race being eliminated. Production closure requires a new release and observation of real-device crashes.

Evidence: Apple Organizer downloaded 20 distinct reports (2026-05-08 through 2026-10-05): 19 crashed in GrResourceCache::notifyARefCntReachedZero; one EXC_BAD_ACCESS in UITraitCollection deallocation inside MetalRedrawer autoreleasepool. Five hardware models, builds 2.0(1), 2.0(3), 2.1(1). Exact historical framework provenance remains an inference until tied to archive/source metadata.

Primary sources: https://github.com/JetBrains/compose-multiplatform-core/pull/2732 (drawable management, CMP-7598); https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.11.0 ; https://firebase.google.com/docs/crashlytics/ios/get-deobfuscated-reports .

### Confirmed mechanisms and decisions

- **iOS GPU lifetime failure (19 reports).** All 19 actual crashed threads enter the same Skia resource-cache assertion during surface/command-buffer release or allocation. Organizer labels such as Worker.park, FIRCLSMachExceptionReply and dispatch_sema4_timedwait refer to other threads and are not separate causes. Upstream PR 2732 explicitly closes CMP-7598 and replaces drawable management with CMPMetalLayer/SurfaceMetalRedrawer. Cached resolved 1.12.1 sources contain that implementation. Keep parallelRendering=true; disabling it selects the legacy renderer. Historical catalog 23c1b58df uses 1.10.3; the exact archived commit has not been proven.
- **UIKit invalid release (one report).** objc_release at address 0x29 during UITraitCollection deallocation, under Kotlin_objc_autoreleasePoolPop -> MetalRedrawer.draw. This is a memory-lifetime failure in the rendering path, but the stack cannot identify who corrupted/released the object. Do not label it a confirmed GC bug or claim the Skia fix proves it resolved. Requires device/sanitizer reproduction or observation of the rebuilt release.
- **Android HTTP close race.** The supplied fatal trace ends in com.android.okhttp.okio.AsyncTimeout.enter during cancellation/response close. The platform HttpURLConnection engine has already been replaced with Ktor OkHttp. A real loopback HTTP test cancels 30 partially streamed responses and checks for completion-handler failures; it passes on the host JVM. This is not an Android-device reproduction of the old engine.
- **Operation Job leak.** One unattached completion path left 100 active SupervisorJob children after 100 completed operations. Removing the unnecessary intermediate job preserves BaseViewModel's existing SupervisorJob isolation. Tests fail before the change and pass after; child errors and owner cancellation remain covered.
- **Connectivity lifecycle.** stop() cancelled the caller's entire scope; the restart/sibling-isolation test fails before and passes after. iOS monitor allocation was outside the cold-flow collection: first() cancels that native monitor, making a later collection reuse a cancelled instance. Allocation now belongs to each collector. A native iOS simulator test collects the same flow three times successfully.
- **HTTP cancellation semantics.** A TimeoutCancellationException from a caller's deadline was converted to a business/network error. Generic error translation also constructed but did not throw its wrapper. Focused tests exposed both; cancellation now propagates, genuine HttpRequestTimeoutException retains the timeout mapping, and other network failures preserve their causes.
- **Symbol/exception observability.** Missing UUID 3C7CB6A3-06A9-3A1C-B365-407C3F4105FE was matched to the archived 2.1(1) executable and uploaded successfully. An archive-only official upload phase is added with the required five inputs. Kotlin nonfatal reporting now uses ExceptionModel/native frame addresses and the cause chain, with event-scoped metadata. Type names must be resolved in Kotlin, because Swift may only see a generic KotlinThrowable wrapper.
- **Android missing XML resource.** The current APK contains all three .cvr string bundles and no values/strings.xml literal in any DEX. No matching source reference exists. Historical emulator reports remain unexplained until reproduced; no placeholder file or broad MissingResourceException suppression is added.
- **Reproduced mismatched iOS resource bundle.** Replacing only a linked framework while keeping an older XCFramework resource bundle crashes at startup in Base64.decode with `Invalid symbol '_'`, because compiled string offsets refer to different bytes. The generated current English cvr hash is 8ee02fc69324; the stale packaged file was d8e774b1a67f. The full assembleSharedDebugXCFramework task graph includes both platform AggregateResources tasks; individual link tasks do not. Use the full assembly for manual integration. Automatic direct integration remains pending approval. This reproduction does not prove the historical missing-XML report has the same cause.

### Approval dependency

Auto-review rejected applying direct integration because it changes ENABLE_USER_SCRIPT_SANDBOXING for the iOS app target. A separate user approval is pending. A parsed project proposal exists at /private/tmp/grippo-xcode-proposal/iosApp.xcodeproj/project.pbxproj. The working project currently keeps its existing XCFramework link and sandbox settings; only the independent archive dSYM phase was applied. The direct-integration script is prepared but not connected to the target.

### Follow-up code review fixes

- Serialize monitor Job replacement, stop and completion-state updates through a small internal reentrant platform lock (JVM synchronized / iOS NSRecursiveLock). Completion uses invokeOnCompletion so cancellation before the coroutine body starts also clears the current state. Launch starts outside the lock. No additional runtime dependency or public API change.
- Centralize host-test registration, Kotlin Test and coroutine-test dependencies in kotlin.multiplatform.test.convention, applied to connectivity, HTTP client and foundation. Module-specific Ktor Mock stays in HTTP client.
- `/tmp/grippo-review-fixes-final.log`: BUILD SUCCESSFUL. All 19 host tests pass, including four concurrent workers performing 800 start/stop cycles and restart during owner cancellation. iPhone connectivity compilation and Simulator native-test compilation pass. `git diff --check` passes. The earlier app/archive checks precede this follow-up; full app artifacts were not rebuilt again for these two review fixes.

### Verification evidence

- `/tmp/grippo-crash-final-tests.log`: BUILD SUCCESSFUL. Operation tests (3), existing result tests (8), connectivity host tests (2), HTTP tests (4), native iOS repeated-collection test (1): no failures. The HTTP suite includes cancelling 30 real partially streamed loopback responses.
- `/tmp/grippo-monitor-restart-final.log`: BUILD SUCCESSFUL after adding the immediate-restart assertion. Owner cancellation now resets monitoring; an old collector cannot reset the replacement collector's state.
- `/tmp/grippo-crash-platform-build.log`: Android assembleDebug and iOS Simulator Debug framework BUILD SUCCESSFUL. `/tmp/grippo-crash-release-framework.log`: iPhone Release framework BUILD SUCCESSFUL. `nm` confirms SurfaceMetalRedrawer in that Release framework. Further edits to the reporting bridge require the subsequent final framework/Swift build; these initial builds alone do not verify those edits.
- Archive upload phase parses successfully with XcodeProj and retains ENABLE_USER_SCRIPT_SANDBOXING=YES. `/tmp/grippo-crash-archive-final.log`: unsigned local Release ARCHIVE SUCCEEDED; official upload phase ran, validation succeeded and background upload was scheduled. Backend receipt of this new archive's symbols is not independently confirmed. App and dSYM UUID both `0EC27565-47BE-3A6B-BE3E-C00025A5D799`; current English/Russian/Ukrainian cvr hashes verified in the archive.
- `/tmp/grippo-final-connectivity-framework.log`: final Simulator Kotlin framework BUILD SUCCESSFUL, exported exceptionTypeName verified in its header. `/tmp/grippo-crash-swift-resources-final.log`: Swift app BUILD SUCCEEDED with the updated reporting bridge and matching resources. Installed on iPhone 17 Pro Simulator/iOS 26.1, login screen verified; background/foreground preserved PID 10036 and the screen. This is a startup/lifecycle smoke check, not reproduction of the physical-device GPU race.
- The same app starts on iPhone 15 Pro Simulator/iOS 17.5 and preserves PID 20828 and the login screen across background/foreground.
- `/tmp/grippo-crash-final-release-android.log`: final Android assembleDebug and iPhone Release framework BUILD SUCCESSFUL after all functional edits. The local archive verification uses a freshly created device-only Release XCFramework with the current aggregated resources; it is a verification artifact, not a published universal distribution bundle.
- Final focused diff review and `git diff --check` completed. User's pre-existing staged navigation additions/deletions were left untouched. Remaining acceptance gaps: automatic Kotlin build integration approval, physical-device GPU/UITraitCollection reproduction or a new-release observation, and confirmation of backend processing for the new nonfatal frame model. No TestFlight/App Store release was published.

Follow the system theme through the existing AppTheme/AppTokens contract. Add the shared avatar shade slot to DarkColor; follow-up dark review improves contrast and surface hierarchy. Implement a complete LightColor with deliberate surface/content pairs, readable accents, chart tracks, muscle diagrams, selected states, and scrims. Update system bars and preview background to follow the same theme.

1. Map every token to its consumers and implement light palette + provider switching.
2. Build Android, inspect authenticated home/profile/training screens and several read-only dialogs in light mode; correct visual issues found in screenshots.
3. Verify dark-mode regression and iOS build/start, review palette coverage and contrast, record evidence.

Do not change user account data, workouts, auth state, or release UI navigation for screenshot checks. Use existing authenticated Android emulator; iOS authorization screen is sufficient for platform startup and theme checks.

## Verification

- `./gradlew :androidApp:assembleDebug :shared:assembleSharedDebugXCFramework` — exit 0, BUILD SUCCESSFUL; final log `/tmp/grippo-light-completion-build.log`.
- `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'platform=iOS Simulator,id=8A3E8CAA-5BCC-4A9C-9FB3-610B71034B0F' CODE_SIGNING_ALLOWED=NO build` — exit 0, BUILD SUCCEEDED; `/tmp/grippo-light-final-xcode.log`.
- Android Pixel 10 Pro API 37.1: inspected Dashboard, profile/avatar, period dialog, body fields/history, chart tooltip, calendar, daily workouts, exercise dialog/details/chips, muscles/toggles. Theme switch with an open bottom sheet verified after setting its contentColor.
- iPhone 17 Pro iOS 26.1: installed and launched; inspected login in light/dark, including corrected Apple icon tint. Authenticated iOS screens and physical devices were not tested.
- Contrast calculation: all 17 light accents on card >=4.54:1; tertiary text on card 4.80:1; light muscle ramp against its background >=3.29:1. These checks do not claim full-app WCAG compliance.
- Staged/unstaged `git diff --check`: clean. Final read-only code review: no remaining concrete findings.
- Original dark system appearance restored on both test devices. User profile/workout data were not changed during checks.

## Screenshots

- [Dashboard](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/light-theme/android-light-dashboard.png)
- [Диалог периода](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/light-theme/android-light-period-dialog.png)
- [Меню профиля и аватар](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/light-theme/android-light-profile.png)
- [Вес, рост и история](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/light-theme/android-light-body.png)
- [Подсказка графика](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/light-theme/android-light-chart-tooltip.png)
- [Календарь](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/light-theme/android-light-calendar.png)
- [Тренировки](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/light-theme/android-light-workouts.png)
- [Диалог упражнения](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/light-theme/android-light-exercise-dialog.png)
- [Детали упражнения и chips](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/light-theme/android-light-exercise-details.png)
- [Мышцы и переключатели](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/light-theme/android-light-muscles.png)
- [iOS: светлый экран входа](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/light-theme/ios-light-login.png)
- [Android: тёмный Dashboard](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/light-theme/android-dark-dashboard.png)
- [Android: тёмный диалог периода](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/light-theme/android-dark-period-dialog.png)
- [iOS: тёмный экран входа](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/light-theme/ios-dark-login.png)

## Dark theme follow-up

- Opaque card surface N200 and elevated dialog N150 separate content from screen N100.
- DarkAccent colors preserve semantic roles while keeping colored text readable on cards (minimum 5.49:1); tertiary text/placeholders N650 reach 5.74:1.
- Primary gradient retains its warm brand colors with dark labels/icons (minimum 5.84:1 at the endpoints).
- Focus border uses the blue accent; info chart tokens now agree with semantic info. Muscle diagrams have a separate dark ramp.
- Two shared metric status chips use primary text over tinted backgrounds in both themes.
- Android visually checked Dashboard, period dialog, profile accents/avatar, muscles/toggles, body inputs/history, and light/dark switching. No profile values were changed.

- Gradle follow-up: `:androidApp:assembleDebug :shared:assembleSharedDebugXCFramework` — BUILD SUCCESSFUL in 7m12s; `/tmp/grippo-dark-refinement-build.log`.
- Additional Android visual checks: monthly calendar, exercise summary/details, achievements and chart accents.

Dark follow-up screenshots:
- [android-dashboard](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/dark-refinement/android-dashboard.png)
- [android-period](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/dark-refinement/android-period.png)
- [android-profile](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/dark-refinement/android-profile.png)
- [android-muscles](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/dark-refinement/android-muscles.png)
- [android-body](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/dark-refinement/android-body.png)
- [android-light-body](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/dark-refinement/android-light-body.png)
- [android-calendar](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/dark-refinement/android-calendar.png)
- [android-exercise-dialog](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/dark-refinement/android-exercise-dialog.png)
- [android-exercise-details](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/dark-refinement/android-exercise-details.png)

- iOS follow-up: Xcode Debug simulator build — exit 0, BUILD SUCCEEDED; `/tmp/grippo-dark-refinement-xcode-final.log` (cached Swift packages, no dependency updates). Installed and launched `com.grippo.app`; login visually checked in both themes. Authenticated iOS screens were not tested.
- Existing dependency/opt-in compiler warnings remain; this theme follow-up does not modify dependency resolution.
- Final staged/unstaged diff whitespace checks passed; both simulators restored to dark appearance.
- [ios-dark-login](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/dark-refinement/ios-dark-login.png)
- [ios-light-login](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/dark-refinement/ios-light-login.png)

## Button + shared shimmer

Keep the current pill shape, warm primary gradient, medium 50 dp height and 14 sp bold label. Match small buttons to the compact type scale (12 sp semibold, 18 dp icon). Use minimum rather than fixed text-button height for larger system fonts; reserve trailing icons while long labels ellipsize.

Add a shared `Modifier.shimmer(enabled, highlightColor)` with cached drawing resources, draw-phase animation, RTL traversal and no animation when disabled. Keep loading button contents/layout stable, prevent repeated clicks and expose indeterminate progress semantics. Keep the public Button call sites and state contract; migrate only Button in this task.

Verification: compile common components for Android/iOS, temporary button gallery in both themes (all styles/states, long labels, font scaling, click prevention), remove gallery, final Android build and iOS component compile. No account/workout changes.

### Button verification

- `:androidApp:assembleDebug :design-system:components:compileKotlinIosArm64 :design-system:components:compileKotlinIosSimulatorArm64` — exit 0, BUILD SUCCESSFUL in 57s; `/tmp/grippo-button-final-build.log`.
- Android gallery: all five styles, all states, text/icon buttons, preserved multicolor image icons in Loading, long label with trailing icon, both themes. Normal-scale small button sizes were identical across states: 232×96 px (transparent 160×49 px).
- Toggling the main button to Loading preserved layout. Enabled click incremented the counter from 0 to 1; subsequent Loading click kept it at 1. Android accessibility tree exposed a disabled indeterminate progress node.
- Font scale 2.5: small buttons grew from 32 dp to 49 dp to fit the label; full-width medium labels were not vertically clipped. Last buttons in a narrow three-button gallery row ellipsized as expected.
- Temporary gallery/source and MainActivity hook removed, including the staged gallery addition. Final app installed/launched and Dashboard inspected. Original emulator light appearance and font scale 1.0 restored. No account/workout data changed.
- Final iOS verification was compilation of components for device/simulator; no new iOS runtime shimmer capture.
- Five-axis code review and staged/unstaged `git diff --check` passed. Existing unrelated compiler warnings remain.
- [Light gallery](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/button-shimmer/light.png)
- [Dark gallery](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/button-shimmer/dark.png)
- [Shimmer animation](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/button-shimmer/dark-animation.mp4)
- [Font scale 2.5](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/button-shimmer/large-font.png)
- [Loading click prevention](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/button-shimmer/loading-click.png)
- [Final app Dashboard](/Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/button-shimmer/final-dashboard.png)

### Dedicated shimmer colors

Added AppColor.ShimmerColors with a default highlight and five button style highlights in both palettes. Button and Modifier.shimmer consume tokens directly; palette values preserve the previously reviewed appearance. Verified Android assembleDebug and components compileKotlinIosArm64/compileKotlinIosSimulatorArm64: BUILD SUCCESSFUL (log /tmp/grippo-shimmer-tokens-build.log).

### Excluded muscles tabs

Replaced the alternating group grid with a fixed adaptive muscle illustration, the same horizontally scrolling outline Segment as missing equipment, and a separately scrolling full-width muscle list. Added selectedGroupId and onGroupClick to the screen state/contract/ViewModel; existing exclusion payload is unchanged. Data refresh retains a valid group or selects the first available one. Switching groups restarts the list while preserving muscle selection.

Verified Android assembleDebug and authorization compileKotlinIosArm64/compileKotlinIosSimulatorArm64: BUILD SUCCESSFUL, final log /tmp/grippo-muscle-final-build.log. Android visual checks: light/dark on Pixel; on Small Phone, group switch, quadriceps toggle and corresponding illustration change, selection retained after switching away/back, horizontal tab scrolling, final Abductor row accessible above Continue after vertical scrolling. Temporary stub-data harness removed; final ordinary APK rebuilt. Screenshots: /Users/maxvoitenko/.codex/visualizations/2026/10/07/01a1168d-f474-7f63-ae5a-eaadbd5d0643/muscle-tabs/. No new iOS runtime capture.

### Shared muscle selection and profile migration

Both authorization and profile now use the design-system MuscleSelection: fixed adaptive hero, horizontal outline tabs, and an independently scrolling keyed list. Spacing, colors and sizing use AppTokens; dedicated muscleSelection dimensions reserve list space after measuring the footer. Group selection lives in the ViewModel and muscle toggles update immutable state atomically. Profile Apply behavior/payload is preserved.

Android visual checks on Small Phone: both themes, group switching, selection retained after switching away/back, illustration updates, horizontally scrolling tabs; authorization final row is reachable. Font scales 1.5 and 2.0 exposed fixed-height toolbar/card clipping, corrected with minimum heights and tokenized padding. Final authorization at 2.0 retains a full two-line row and Continue button by shrinking the hero. Final profile light/dark screenshots are in profile-muscle-tabs under the workspace visualization directory.

Final verification: :androidApp:assembleDebug and profile/authorization compileKotlinIosArm64 + compileKotlinIosSimulatorArm64 succeeded (exit 0; /tmp/grippo-profile-muscle-final-build.log). Temporary UI harness and MainActivity hook removed. Ordinary APK installed on both Android emulators and launched. No profile save was sent to the backend. iOS verification covers compilation, without a new simulator/device runtime session. Existing unrelated compiler warnings remain.

## Whole-app card and screen consistency audit — October 9, 2026

Review all 140 component files and all 53 screen/dialog files. Preserve 64 dp exercise thumbnails and the current neutral training screen direction. Establish consistent roles rather than uniform sizes: screen/section headings, list-row titles, supporting metadata, status colors, selection colors and analytical chart accents. Keep functional contracts and backend data unchanged.

1. Inventory components/screens, hardcoded styling, preview coverage and caller relationships.
2. Capture existing whole-screen previews on the Android emulator with a temporary fixture-only harness; inspect both themes, representative small-screen/font-scale cases and long content.
3. Apply small batches to shared list cards, analytical cards, selector/form components and screen layouts, rebuilding and recapturing affected screens.
4. Review all source changes, compile Android/iOS targets, remove the temporary harness and restore the normal application and emulator settings.
5. Record screenshot coverage, actual findings/fixes and any unverified screens in a durable audit report.

Acceptance: visual values use AppTokens; related rows agree on typography/spacing; neutral metadata does not masquerade as status; selected/completed/error states remain distinguishable; no clipped headings, controls or CTA content on captured screens. Screenshot evidence distinguishes runtime fixture screens from ordinary authenticated navigation.
