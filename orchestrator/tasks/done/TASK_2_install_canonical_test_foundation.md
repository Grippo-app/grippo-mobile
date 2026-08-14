# TASK 2 — Install canonical test foundation

## Source

- Kind: follow-up
- Type: test-foundation-prerequisite
- Ref: TASK_1_dobavit_home_screen
- Fingerprint: sha256:4f398d463e79025d9dd65fa2c4a8727f36c605ab35ce1e5c553fdde84543947b

## Goal

Install the canonical lean, opt-in Gradle test foundation the orchestrator requires before any task can register or certify tests. This adds the test-only version-catalog aliases, the seven per-capability test convention plugins plus their `build-logic` registrations, the root test-aggregate tasks, and the single host-test capability entry — moving the foundation doctor from `ABSENT_CAN_INSTALL` to `READY` so downstream product tasks (starting with TASK_1) can attach tests through the registered infrastructure. No product tests are added in this prerequisite.

## Inputs

- Test policy hash: sha256:f670f82775627be17641138eb85be29ae4d63ce176033f76852910d70f7e1742
- Doctor inventory hash: sha256:93836a2eefa10ceec3240390fe9d7951fa2fdf6b85f3a0653ed197f65941e8dc
- Foundation intent hash: sha256:deb9f4620b7054fed66e81c96488d7fbb48169b8290a0cb92a3ec92b35aebd36
- Foundation doctor `node orchestrator/tasks/task-test-foundation.mjs doctor --product-root .` currently reports `ABSENT_CAN_INSTALL`: a clean install with no foreign `withHostTest` owner.
- Required version pins already satisfied — `agp = "9.0.1"` and `kotlin = "2.3.21"` in `gradle/libs.versions.toml`, and `id("org.gradle.toolchains.foojay-resolver-convention")` in `settings.gradle.kts`.
- Assumed — canonical foundation contents are exactly the aliases / convention plugins / registration ids / root aggregates enumerated by the doctor: `CATALOG_ALIASES`, `CONVENTION_SOURCES`, `REGISTRATION_IDS`, `ROOT_AGGREGATES` in `orchestrator/tasks/task-test-foundation.mjs` (basis: the doctor `READY` predicate — the installer targets that exact inventory).
- Assumed — no alternative test-runner stack is adopted; the catalog stays free of `io.kotest`, `io.mockk`, `org.mockito`, `app.cash.paparazzi` (basis: doctor `CONFLICT_MARKERS`; the catalog currently declares none).

## Acceptance

### Automated

- Version-catalog test aliases added to `gradle/libs.versions.toml`: `kotlin-test`, `kotlinx-coroutines-test`, `turbine`, `ktor-client-mock`, `koin-test`, `androidx-room-testing`, `androidx-test-runner`, `androidx-test-core`, `compose-ui-test-manifest`.
- Seven test convention plugin sources present under `build-logic/convention/src/main/kotlin/`: `KmpTestConventionPlugin.kt`, `CoroutinesTestConventionPlugin.kt`, `FlowTestConventionPlugin.kt`, `NetworkTestConventionPlugin.kt`, `DiTestConventionPlugin.kt`, `RoomTestConventionPlugin.kt`, `ComposeUiTestConventionPlugin.kt`, plus the capability entry `TestCapabilityEntryTask.kt`.
- Convention plugin ids registered in `build-logic/convention/build.gradle.kts`: `kmp.test.convention`, `coroutines.test.convention`, `flow.test.convention`, `network.test.convention`, `di.test.convention`, `room.test.convention`, `compose.ui.test.convention`.
- Root test-aggregate tasks defined in `build.gradle.kts`: `allHostTests`, `allIosSimulatorTests`, `allAndroidDeviceTests`, `allScreenshotTests`, `allConfiguredTests`, `testCapabilityInventory`.
- Single host-test owner: exactly one `withHostTest` enabler call exists and lives only inside the base test convention plugin (no foreign enabler elsewhere in `build-logic/convention/src/main/kotlin/`).
- Foundation doctor reports `READY`: `node orchestrator/tasks/task-test-foundation.mjs doctor --product-root .` returns state `READY`.
- The task's allowlisted `bootstrap-foundation-fixture` structural gate passes.
- `./gradlew :shared:assembleSharedDebugXCFramework` and `./gradlew :androidApp:assembleDebug` both green.

## Out of scope

- No product, unit, integration, or UI tests are authored — this prerequisite installs the registered test infrastructure only.
- No test-runner stack other than the canonical one — no `io.kotest`, `io.mockk`, `org.mockito`, or `app.cash.paparazzi`.
- No version-pin bumps (`agp`, `kotlin`) — the required pins are already present and stay unchanged.
- no changes to CLAUDE.md or orchestrator/**
- no schema migration (Database.kt version, migrations/*) — separate task
- no TODO/FIXME markers left in code the task claims as done

## Origin
- split from TASK_1_dobavit_home_screen

---

## Outcome

**Status**: completed-with-caveats
**Completed at**: 2026-08-14T03:01:26Z
**Reviewer**: internal-reviewer
**Review iterations**: 1

### Build gates

- `:shared:assembleSharedDebugXCFramework` — pass
- `:androidApp:assembleDebug` — pass
- `tests` — pass

### Runtime verify

- Gate: skipped (no runtime-observable change)
- Result: n/a — build-infrastructure only; no product runtime behavior changed

### Acceptance trace

- `Version-catalog test aliases added to `gradle/libs.versions.toml`: `kotlin-test`` — verified — all 9 aliases present in libs.versions.toml
- `Seven test convention plugin sources present under `build-logic/convention/src/m` — verified — 8 sources present under build-logic/convention/src/main/kotlin
- `Convention plugin ids registered in `build-logic/convention/build.gradle.kts`: `` — verified — 7 ids registered in build-logic/convention/build.gradle.kts
- `Root test-aggregate tasks defined in `build.gradle.kts`: `allHostTests`, `allIos` — verified — 6 aggregates defined in root build.gradle.kts
- `Single host-test owner: exactly one `withHostTest` enabler call exists and lives` — verified — single withHostTest in KmpTestConventionPlugin only
- `Foundation doctor reports `READY`: `node orchestrator/tasks/task-test-foundation` — verified — doctor returns state READY
- `The task's allowlisted `bootstrap-foundation-fixture` structural gate passes.` — verified — cert anchor verified; bootstrap-foundation-fixture 8/8 pass
- ``./gradlew :shared:assembleSharedDebugXCFramework` and `./gradlew :androidApp:as` — verified — iOS XCFramework + androidApp assembleDebug both PASS

### Caveats

- Android assembleDebug built via a temporary real google-services.json swap; placeholder restored, candidate clean
- allScreenshotTests is an intentionally-empty forward placeholder until screenshot infra is added

### Follow-ups

- none

### Files touched

- `build-logic/convention/src/main/kotlin/KmpTestConventionPlugin.kt` — created
- `build-logic/convention/src/main/kotlin/CoroutinesTestConventionPlugin.kt` — created
- `build-logic/convention/src/main/kotlin/FlowTestConventionPlugin.kt` — created
- `build-logic/convention/src/main/kotlin/NetworkTestConventionPlugin.kt` — created
- `build-logic/convention/src/main/kotlin/DiTestConventionPlugin.kt` — created
- `build-logic/convention/src/main/kotlin/RoomTestConventionPlugin.kt` — created
- `build-logic/convention/src/main/kotlin/ComposeUiTestConventionPlugin.kt` — created
- `build-logic/convention/src/main/kotlin/TestCapabilityEntryTask.kt` — created
- `gradle/libs.versions.toml` — modified
- `build-logic/convention/build.gradle.kts` — modified
- `build.gradle.kts` — modified

### Execution log

- Phases: preflight · intake · planner · builders · validators (1 fix cycle) · tests · assemble-gate · review
- Totals: 2 turns · stops 1 (assemble env-blocker, resolved) · retries 1 (builder comment fix)
- Design: none
- Spawned: none
