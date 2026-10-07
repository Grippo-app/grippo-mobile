# Execution plan — TASK_7_profile_note_archive

## Classification
- Brand-new sub-screen inside existing `:ui-screen-features:profile`.

## Builder routing (order)
1. feature-module-scaffold-builder (module absent)
2. screen-builder

## Validator set
- Always-on: build-validator, scope-leak-validator, acceptance-tracer.
- Conditional: mvi-contract-validator, architecture-validator, naming-convention-validator.

## Test gate
- Policy: version 1, hash `sha256:0000000000000000000000000000000000000000000000000000000000000000` (fixture placeholder — a real plan carries the live `policyHash` from `test-policy.json`).
- Planned impact: `test-impact.planned.json` (impactHash pinned there); required suites: `profile`; lanes: `common`; capabilities: `coroutines`, `flow`.

## Gate sequence
- Step 4 (compile) -> 4.4 (tests) -> 4.5 (assemble) -> 4.6 (verify, deferred) -> 4.6b (screenshot, n/a) -> 5 (review) -> 5.5 (security, n/a).

## Planner reference
- Implementation plan TASK_7_profile_note_archive (planner-output).
