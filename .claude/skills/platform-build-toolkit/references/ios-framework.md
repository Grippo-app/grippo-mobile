# iOS shared framework and shell

Inspect the actual IosSwiftPackageConventionPlugin under `build-logic/convention/`,
`shared/build.gradle.kts`, `iosApp` Xcode project, AppDeletage.swift and root hosting code.
The current deliverable is a static `shared.xcframework`; source owns exports/link flags.

- Public shared APIs exposed to Swift require compatible declared exports; keep implementation/core
  types internal. Android Firebase transport is not exported. Swift SDK adapters use bootstrap contracts.
- Shell initializes Koin once, retains ApplicationBootstrap and owns its existing lazy root and
  StateKeeper archive lifecycle. Do not introduce a second root, init or competing lifecycle owner.
- Edit pbxproj by inspecting its actual object references/build phases/settings and resulting diff.
  Preserve existing UUID references; do not replace it with a generic template or duplicate build phases.
- Framework name/task/output paths come from actual convention and Xcode settings. Do not assume a
  manually copied framework is current or claim a successful Xcode build from source review.
- Preserve SQLite linkage and required exported Decompose/toolkit API contracts when relevant.

When authorized, `:shared:assembleSharedDebugXCFramework` verifies framework compilation.
Swift startup/linking and restoration remain unverified unless their permitted scenarios were run.
Do not touch secure configuration or unrelated shell behavior as a framework fix.
