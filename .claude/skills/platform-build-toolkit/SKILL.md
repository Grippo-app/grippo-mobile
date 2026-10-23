---
name: platform-build-toolkit
description: Change Grippo Gradle conventions, module build wiring, platform toolkit utilities, reusable Compose libraries or thin app shells, and release iOS builds to TestFlight. Use for platform/build work or an explicit request to upload to TestFlight.
---

# Grippo platform and build foundation

Read `CLAUDE.md` and actual affected build/platform source. Versions, targets, module list and
startup signatures come from current source; do not copy a generic project template.

For a TestFlight release request, follow the [terminal release workflow](references/release.md#ios-testflight-release)
and its completion gate.

## Workflow and boundaries

- Toolkit owns platform utilities without data/service/Firebase dependencies. Preserve only the
  documented exceptions ([toolkit ownership](references/toolkit-utilities-core.md)); a new
  dependency edge the request does not imply is scope expansion.
  Service API/core modules keep typed contracts and state ownership separate from platform SDK adapters.
- Compose libraries receive style/data as parameters; product AppTokens and business behavior belong
  in the design-system/features. Shells initialize DI/root/platform adapters and stay thin.
- Module builds use existing convention plugins. SDK/toolchain/global opt-ins and common configuration
  belong in `build-logic/convention/`, versions in `gradle/libs.versions.toml`, modules in settings.gradle.kts.
  MUST NOT duplicate or silently override them in feature scripts.
- New modules, dependencies, convention changes and nontrivial shell/Xcode changes happen only
  inside the authorized scope ([scope discipline](../../../CLAUDE.md#scope-discipline)); a feature
  request does not authorize unrelated infrastructure cleanup or version changes
  ([dependency updates](references/convention-plugins.md#dependency-updates)).
- Platform APIs keep complete Android/iOS actuals and their lifecycle/disposal. MUST NOT pass Android
  Context into common code or add platform SDK dependencies to shared contracts.
- Use existing AppLogger categories and sinks; inspect actual build-mode behavior.
  Preserve cancellation without new generic wrappers; explicit checks follow the
  [canonical coroutine rules](../validation-gates/references/forbidden-patterns.md#coroutines).

## Completion gate

For changes to platform/build logic, review dependency direction, convention/alias usage,
public API/export compatibility, platform actuals, startup ownership and privacy/cancellation.
Release execution uses the [release completion gate](references/release.md#release-completion-gate).
Checks follow the [verification policy](../../../CLAUDE.md#verification-policy); report actual
results and relevant unverified platform/runtime behavior.

Read the reference for the affected topic:

- [build wiring](references/gradle-build.md) - module scripts, dependencies, which targets to run;
- [conventions, catalog and dependency updates](references/convention-plugins.md) - plugin matrix, opt-ins, global flags, version pins;
- [new module checklist](references/new-module.md) - end-to-end wiring of a new module by kind;
- [toolkit ownership](references/toolkit-utilities-core.md) and [platform utilities](references/toolkit-utilities-platform.md);
- [iOS framework and shell](references/ios-framework.md) - XCFramework, Xcode project, `AppDelegate`;
- [secrets and configuration](references/secrets-and-config.md) - `secure/`, synced configs, logging of sensitive data;
- [release](references/release.md) - versioning, compatibility bumps and the terminal iOS/TestFlight workflow with Crashlytics symbols.
