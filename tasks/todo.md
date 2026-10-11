# Light theme — complete

## Crash remediation — 2026-10-11

- [x] Follow-up review: serialize connectivity state across threads and centralize shared test setup in a convention plugin. Verify 19 host tests, iPhone module and native-test compilation.

- [x] Diagnose Apple/Android incidents and verify the resolved renderer. Historical archive commit remains unproven; matching missing dSYM was uploaded.
- [ ] Make Kotlin framework preparation automatic in Xcode (specific approval pending). Archive dSYM phase verified: Release archive succeeds, upload validation succeeds, binary/dSYM UUIDs match.
- [x] Preserve Kotlin exception frames, actual type names, causes and event metadata. Swift compilation succeeds; backend symbolication of a new nonfatal event remains a release check.
- [x] Repair confirmed lifecycle/cancellation defects. Regression tests pass for operation ownership, monitoring stop/restart/owner cancellation, native repeated subscription and HTTP cancellation/error translation.
- [x] Review changes and exercise platform builds/startup/transitions. Android APK, final native frameworks, Swift app and unsigned iPhone archive build successfully; iOS 17.5/26.1 startup and background/foreground pass. Physical-device race and production closure remain explicitly unverified.

- [x] Full token palette and system-theme wiring.
- [x] Android build and screenshots of screens and dialogs; visual fixes.
- [x] Dark regression, iOS build/start, contrast checks and final review.

# Dark theme follow-up — complete

- [x] Re-audit tokens and consumers; fix dark contrast/surfaces and shared metric chips.
- [x] Android screenshots, light regression, iOS build/start and both login themes.
- [x] Final review, contrast calculations and diff checks.

# Button + shimmer — complete

- [x] Shared shimmer modifier and Button migration.
- [x] Small typography/icon scale, font scaling and loading semantics.
- [x] Android visual/interaction checks, iOS component compile and final app build.

- [x] Dedicated shimmer color tokens in both themes; Android build and iOS device/simulator component compilation.

- [x] Excluded muscles: fixed hero and group tabs, filtered muscle list, Android visual/interaction checks, final Android build and both iOS target compilations.

- [x] Recheck excluded muscles and migrate profile muscles to shared design-system MuscleSelection; font-scale fixes, light/dark Android checks, final Android build and both iOS target compilations.

# Whole-app consistency audit

- [x] Inventory all components and screens, establish role-based visual rules.
- [x] Capture and inspect whole-screen Android fixtures in both themes.
- [x] Align list cards and supporting metadata; verify affected screens.
- [x] Align metric cards, selectors, forms and screen spacing; verify affected screens.
- [x] Check small screens, long text and larger fonts.
- [x] Compile Android/iOS, remove capture harness, restore app/settings and record evidence.
