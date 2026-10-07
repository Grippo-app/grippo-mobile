# Compose dependency migration

Replace deprecated Compose Gradle dependency shortcuts with version-catalog aliases. Preserve the currently resolved library versions: Compose 1.10.3, Material3 1.9.0-beta03, icons 1.7.3. Keep Compose plugin and resources DSL configuration.

1. Add catalog aliases; migrate preview module and verify commonMain and Android.
2. Replace remaining dependency shortcuts in batches of at most five module build files; migrate Compose UI test convention.
3. Replace legacy Preview imports in chart sources in batches of at most five files.
4. Verify all project configuration, Android compilation and iOS simulator compilation; audit remaining shortcuts and diff.

Verification: Gradle dependencyInsight before/after for Material3; compileCommonMainKotlinMetadata and compileAndroidMain for preview/chart; androidApp compileDebugKotlin; shared compileKotlinIosSimulatorArm64; git diff --check.

Risks: Material3 version differs from Compose; retain the resolved baseline. Icons have a frozen release; explicitly retain it. Existing user Firebase/config edits must be preserved.

## Results

All tasks completed. Preview and chart commonMain/Android compilation, Android app compileDebugKotlin, shared iOS simulator compilation passed. Build: 1793 actionable tasks. Material3 dependencyInsight before/after selects 1.9.0-beta03. No legacy shortcuts or preview imports remain. Independent review found no actionable issues. Existing compiler warnings remain; runtime rendering was not manually checked.

## Final release baseline

At the user's request, main was rebuilt on RELEASE (24bc1eee8). The post-release bottom-bar shell and test infrastructure were removed. Compose migration was reapplied to the release's modules; all project skills and CLAUDE.md were preserved. Necessary Gradle wrapper/toolchain/Detekt wiring fixes remain. Android 19 (2.0) and iOS 2.1 (1) version identifiers are unchanged. Today's Firebase and Google auth configuration is included for publication at the user's explicit request.

Release-baseline verification passed: preview/chart common metadata, androidApp compileDebugKotlin, shared compileKotlinIosSimulatorArm64 (1749 actionable tasks). Source comparison to RELEASE differs only in preview changes; both bottom-bar commits are absent from main ancestry.
