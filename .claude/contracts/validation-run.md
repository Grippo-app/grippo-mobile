# Contract: validation-run

The validator-invocation envelope: which validators ran, which were expected, so a required validator cannot disappear without a `skipped` finding.

Source: the `task-orchestrator` skill (`references/validator-routing.md`, Step 4).

## Frozen mechanics
- The **three always-on validators** (`build-validator`, `scope-leak-validator`, `acceptance-tracer`) run on every task regardless of `task-intake`.
- Always-on refers to assessment, not build authorization: when compilation is excluded, `build-validator` reviews source/configuration and records excluded compile/assemble checks as unrun with the user's reason. It must not claim PASS or require execution to clear this authorized exclusion.
- Conditional validators run when their kind applies; a required conditional that does not run must emit a `skipped` `validator-finding` with a reason (fail-closed).
- Dedup at the routing layer: `same (file, rule_id) from two validators counts as one`.
- Missing required report fails the gate.
