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
  MUST NOT duplicate or silently override them in feature scripts.
- New modules, dependencies, convention changes and nontrivial shell/Xcode changes require the user's
  scope to cover them. Existing authorization suffices; a feature request does not authorize unrelated
  infrastructure cleanup or version changes.
- Platform APIs MUST preserve required Android/iOS actuals and lifecycle/disposal. MUST NOT pass Android
  Context into common code or add platform SDK dependencies to shared contracts.
- Use existing AppLogger categories and sinks; inspect actual build-mode behavior.
  Preserve cancellation without new generic wrappers; explicit checks follow the
  [canonical coroutine rules](../validation-gates/references/forbidden-patterns.md#coroutines).

## Completion gate

MUST review dependency direction, convention/alias usage, public API/export compatibility, platform actuals,
startup ownership and privacy/cancellation. Follow the canonical
[verification policy](../../../CLAUDE.md#verification-policy). Run relevant static/manual
checks; builds needed within task scope do not require separate consent. Record actual checks
and absent platform/runtime verification.

Read [build wiring](references/gradle-build.md), [conventions/catalog](references/convention-plugins.md),
[toolkit ownership](references/toolkit-utilities-core.md), [platform utilities](references/toolkit-utilities-platform.md)
or [iOS framework/project](references/ios-framework.md) for the affected topic.
