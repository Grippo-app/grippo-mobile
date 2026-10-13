# Current build wiring

Read `settings.gradle.kts`, the affected `build.gradle.kts`, `gradle/libs.versions.toml` and the
specific plugin under `build-logic/convention/`. These files own the current module/target/version graph.

- Declare each actually used project dependency; do not rely on hidden transitive exports.
  Use existing type-safe project/catalog accessors and convention-plugin IDs.
- Feature modules do not redeclare SDK/toolchain/repositories/global opt-ins. Source-specific
  configuration follows the real convention; do not remove valid current exceptions by pattern matching.
- New Koin bindings use the existing KSP convention; source review must check module/provider wiring.
- Common code changes may affect both platform actuals and exported Swift API. Select relevant checks
  from actual changed modules rather than demanding all builds for documentation or local edits.
- No dependency/version changes to silence a warning without authorized scope. Review warnings in
  permitted build output and report what was actually run.

When builds are needed within task scope, existing targets include `:androidApp:assembleDebug` and
`:shared:assembleSharedDebugXCFramework`. Inspect source before claiming any target or output exists;
never report an unrun task as passing. Follow the [project verification policy](../../../../CLAUDE.md#verification-policy).
