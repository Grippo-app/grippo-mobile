---
name: platform-build-toolkit
description: Change Grippo Gradle conventions, module build wiring, platform toolkit utilities, reusable Compose libraries or thin app shells. Use for platform/build ownership rather than feature business logic.
---

# Grippo platform and build foundation

Read `CLAUDE.md` and actual affected build/platform source. Versions, targets, module list and
startup signatures come from current source; do not copy a generic project template.

## Workflow and boundaries

- Toolkit owns platform utilities without data/service/Firebase dependencies. Preserve only existing
  documented exceptions; new dependency edges require an architecture decision within task scope.
  Service API/core modules keep typed contracts and state ownership separate from platform SDK adapters.
- Compose libraries receive style/data as parameters; product AppTokens and business behavior belong
  in the design-system/features. Shells initialize DI/root/platform adapters and stay thin.
- Module builds use existing convention plugins. SDK/toolchain/global opt-ins and common configuration
  belong in `build-logic/convention/`, versions in `gradle/libs.versions.toml`, modules in settings.gradle.kts.
  Do not duplicate or silently override them in feature scripts.
- New modules, dependencies, convention changes and nontrivial shell/Xcode changes require the user's
  scope to cover them. Existing authorization suffices; a feature request does not authorize unrelated
  infrastructure cleanup or version changes.
- Platform APIs preserve required Android/iOS actuals and lifecycle/disposal. Never pass Android
  Context into common code or add platform SDK dependencies to shared contracts.
- Logs exclude secrets, fragments and hashes; auth logs contain only method/path/status.
  Preserve cancellation without new generic wrappers/manual ensureActive checks.

## Completion gate

Review dependency direction, convention/alias usage, public API/export compatibility, platform actuals,
startup ownership and privacy/cancellation. No automated test sources/infrastructure. Run static/manual
checks relevant to the change; builds only within authorized user scope. A green build proves compilation,
not runtime startup or restoration. Record checks run and absent platform/runtime verification.

Read [build wiring](references/gradle-build.md), [conventions/catalog](references/convention-plugins.md),
[toolkit ownership](references/toolkit-utilities-core.md), [platform utilities](references/toolkit-utilities-platform.md)
or [iOS framework/project](references/ios-framework.md) for the affected topic.
