# Dependency compatibility repair

- [x] Supported KMP targets and dependency resolution; Gradle configuration and shared metadata pass.
- [x] Android debug APK builds and launches (emulator-5554, Dashboard rendered, no AndroidRuntime errors).
- [x] iOS ARM frameworks and simulator app build and launch (iPhone 17 Pro, iOS 26.1; authorization screen rendered; process remains running).
- [x] Review and verification evidence recorded.

Verification on 2026-10-07:

- `:shared:assembleSharedDebugXCFramework`: BUILD SUCCESSFUL, both iosArm64 and iosSimulatorArm64; 19m 5s first native cache build.
- `:shared:compileCommonMainKotlinMetadata :androidApp:assembleDebug`: BUILD SUCCESSFUL, final state, 48s.
- Android APK installed and MainActivity launched successfully on emulator-5554; Dashboard screenshot inspected; no AndroidRuntime errors.
- Xcode Debug simulator build: BUILD SUCCEEDED; installed and launched `com.grippo.app`, PID 34700; screenshot inspected.
- Android Studio Gradle Sync: RESOLVE_PROJECT:48 onSuccess; import finished at 18:49:38, 2m 50s. Removed obsolete non-Gradle IDE module grippo-mobile2.
- Read-only review found no remaining defects in compatibility changes; `git diff --check` passed.

Scope: simulator/emulator startup verified; signing for a physical iPhone and full authenticated flows were not tested. Non-blocking warnings remain in dependency metadata bridges, existing global opt-in configuration, expect/actual beta notices, and ICU data-object deployment metadata.

Follow-up standards audit, 2026-10-07:

- Removed the missing stability-config reference and empty commonTest directory; no warning suppression added.
- Replaced internal/deprecated iOS theme API with public foundation.isSystemInDarkTheme.
- Updated platform-build skill references to supported targets, current catalog, and Android DSL; marked old test recipes as historical rather than verified on the current toolchain.
- Final combined Android/metadata/XCFramework build: BUILD SUCCESSFUL in 2m 13s (/tmp/grippo-audit-final-build.log).
- Final Xcode build: BUILD SUCCEEDED (/tmp/grippo-audit-xcode.log); iOS process 2079 stayed running and authorization screen rendered.
- Android reinstalled and started successfully, process 1136, no AndroidRuntime errors.
- Android Studio Sync RESOLVE_PROJECT:49: onSuccess, import completed 19:14:11 in 3m 2s.
- Reviewed actual dependency graph; no force/exclude rules added to hide JetBrains/AndroidX metadata-bridge warnings.
