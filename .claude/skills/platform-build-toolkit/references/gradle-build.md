# Current build wiring

Read `settings.gradle.kts`, the affected `build.gradle.kts`, `gradle/libs.versions.toml` and the
specific plugin under `build-logic/convention/`. These files own the current module/target/version graph.

- Declare each actually used project dependency; do not rely on hidden transitive exports.
  Use existing type-safe project/catalog accessors and convention-plugin IDs.
- Feature modules do not redeclare SDK/toolchain/repositories/global opt-ins. Source-specific
  configuration follows the real convention; do not remove valid current exceptions by pattern matching.
- New Koin bindings use the existing KSP convention; source review must check module/provider wiring.
- Common code changes may affect both platform actuals and the exported Swift API; the
  [verification policy](../../../../CLAUDE.md#verification-policy) names the triggers for the
  XCFramework build.
- No dependency/version changes to silence a warning
  ([dependency updates](convention-plugins.md#dependency-updates)). Review warnings in the output
  of a permitted check and report what was actually run.

Which builds run is decided by the default build rule in the
[verification policy](../../../../CLAUDE.md#verification-policy); the commands and their outputs
are in the [build target catalog](../../runtime-verification/SKILL.md#build-target-catalog).
Never report an unrun task as passing.
