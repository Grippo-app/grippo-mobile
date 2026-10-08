# Light theme implementation

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
