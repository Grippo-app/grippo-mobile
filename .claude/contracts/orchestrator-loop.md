# Contract: orchestrator-loop

Stable mechanics of the run loop. The `task-orchestrator` skill MUST preserve these exactly; the caps also live in its current references.

Source of truth: the `task-orchestrator` skill (`references/run-loop.md` + `validator-routing.md`).

## Numeric caps (value · reset scope · source substring)

| Cap | Value | Reset scope | Source substring (grep-pinned) |
|---|---|---|---|
| build-failure rollback hint | after 2 builder retries | per task | `resists 2 builder retries` |
| BLOCKED loop (task-intake / inputs-resolver / planner) | 3 iterations | shared, per task | `after 3 BLOCKED iterations` |
| Step 4 stable finding-set (rotation) | no shrink across 2 consecutive cycles | per Step-4 entry session | `does not shrink across 2 consecutive cycles` |
| Step 4 outer re-entry | 7 | per task, never reset mid-task | `reaches **7**` |
| Step 4.5 assemble | 2 consecutive FAILs (PASS resets) | per task | `2 *consecutive* assemble-mode FAILs per task` |
| Step 4.6 runtime verify | 3 invocations | per task | `at most 3 verify invocations per task` |
| Step 4.6b screenshot | 3 invocations | per task | `at most 3 invocations per task` |
| Step 5.5 security-review | 2 iterations | per task | `2 security-review iterations per task` |
| acceptance bullet repeatedly missing | 2 consecutive validator cycles | per task | `across 2 consecutive validator cycles` |
| site runner concurrency | `MAX_PARALLEL=2` (canary; source constant, no env override) | n/a | `MAX_PARALLEL=2` |

## Other frozen mechanics (preserve, parser-pinned where greppable)

- **Dedup** by `(file, rule_id)`, highest severity wins; same `(file,rule_id)` from two validators counts once. Pin: `same `(file, rule_id)` from two validators counts as one`.
- **footprint-scoping**: diff-sensitive validators scoped to the candidate diff — every change against the sealed base tree of the execution root. Pin: the footprint is the candidate diff; there is no `/tmp` baseline and no set difference.
- **parent-adopts-orchestrator-role** (not spawned as a sub-agent).
- **validator-adjudication** vs requirements (opposite verdicts resolved against the spec, not blind-routed) — distinct from dedup.
- **miss-signal protocol**: `MAP_MISS` / `REGISTRY_MISS` / `CONTRACT_MISS` not silently dropped.
- **suggestion-only / advisory** findings excluded from blocking routing.

(The task-orchestrator skill keeps the caps verbatim, and the prose mechanics remain stable here for the per-agent contracts to reference.)
