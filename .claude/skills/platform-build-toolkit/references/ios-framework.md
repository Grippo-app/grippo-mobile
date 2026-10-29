# iOS shared framework and shell

Inspect `IosSwiftPackageConventionPlugin` under `build-logic/convention/src/main/kotlin/`,
`shared/build.gradle.kts`, `iosApp/iosApp.xcodeproj/project.pbxproj`, `iosApp/iosApp/AppDeletage.swift`
(the file name typo is real; the class is `AppDelegate`) and the root hosting code before editing.

## Current shape (verified)

- The deliverable is the static `shared.xcframework` assembled by
  `./gradlew :shared:assembleSharedDebugXCFramework` (output `shared/build/XCFrameworks/debug/`) or
  `:shared:assembleSharedReleaseXCFramework`; exports and the `-lsqlite3` linker flag come from the
  convention ([conventions](convention-plugins.md)).
- The Xcode project links the manually built XCFramework through
  `../shared/build/XCFrameworks/$(SHARED_XCFRAMEWORK_CONFIGURATION)/shared.xcframework`.
  `iosApp/scripts/build-shared-framework.sh` (runs `:shared:embedAndSignAppleFrameworkForXcode`)
  exists but is not wired into the project's build phases; the only shell-script phase is
  "Upload Crashlytics symbols" (Release device builds). Rebuild the XCFramework before an Xcode
  build; do not assume the linked framework is current.
- The `iosApp` scheme is user-local (`xcuserdata` only), so `xcodebuild -list` may show no scheme;
  use [CLI scheme recovery](#cli-scheme-recovery).
- `AppDelegate` calls `Koin().doInit(appDeclaration:)` once, keeps the returned `ApplicationBootstrap`
  and attaches the Swift Firebase adapters through its analytics, crash-reporting and messaging
  contracts ([composition root](../../di-modules/references/composition-root.md)).

## Rules

- Public shared APIs exposed to Swift require compatible declared exports; keep implementation/core
  types internal. Android Firebase transport is not exported.
- The shell initializes Koin once, retains `ApplicationBootstrap` and owns its lazy root and
  StateKeeper archive lifecycle. Do not introduce a second root, init or competing lifecycle owner.
- Edit `project.pbxproj` by inspecting its actual object references, build phases and settings and
  the resulting diff. Preserve existing UUID references; do not replace it with a generic template
  or duplicate build phases.
- Preserve SQLite linkage and the exported Decompose/toolkit API contracts.
- Apply the [verification policy](../../../../CLAUDE.md#verification-policy) for
  framework and host build triggers. The XCFramework build proves framework
  compilation; an Xcode host build also checks Swift compilation and linking.
  Startup and restoration require authorized
  [runtime verification](../../runtime-verification/SKILL.md).
- Do not touch secure configuration ([secrets](secrets-and-config.md)) or unrelated shell behavior
  as a framework fix.

## CLI scheme recovery

The main session resolves a missing scheme for either simulator builds or archiving.
Inspect existing `.xcscheme` files and the project's target references. Reuse/share
a matching scheme through file tools; create a minimal one from those references
only when absent, with the actions needed for the requested build. Verifiers report
the missing scheme to the main session and keep their read-only rights. This setup
needs no Xcode window and grants no archive or upload authorization.
