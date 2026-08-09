# task-orchestrator references — routing table

Self-contained reference pack for the `task-orchestrator` skill — the run-loop
spine. These files carry the skill's own procedure, so a parent adopting the
orchestrator role reads **only** these references + the frozen contracts at
runtime.

Each numeric cap and frozen mechanic is pinned in the contracts under
`orchestrator/contracts/`; cite those (don't re-derive).

## Loop step / topic → reference file + frozen contract

| Loop step / topic | Reference file | Frozen contract |
|---|---|---|
| Whole run loop (Steps 0–6), parent-adopts-role, lock + journal discipline, BLOCKED format, escalation triggers + durable `## Questions` publication, no-inline-code, comment/deprecation discipline, move-back | [`run-loop.md`](run-loop.md) | [`orchestrator-loop.md`](../../../contracts/orchestrator-loop.md) |
| Step 1 — task-intake → execution plan | [`run-loop.md`](run-loop.md) §1 | [`execution-plan.md`](../../../contracts/execution-plan.md) |
| Step 1a — context-finder pre-flight (Tier-0 map, MAP/REGISTRY/CONTRACT_MISS producer side) | [`context-finder.md`](context-finder.md) | [`orchestrator-loop.md`](../../../contracts/orchestrator-loop.md) (miss-signal protocol) |
| Step 1a — inputs-resolver verdict (HIGH→BLOCK, MEDIUM→carry), signal scan + consumer-side miss handling | [`run-loop.md`](run-loop.md) §1a | (routed via `validation-gates`) |
| Step 1b — screen-design pre-flight (cache / census / spec / evidence) | [`run-loop.md`](run-loop.md) §1b | — |
| Step 2a — implementation-planner → file-level contract | [`planner.md`](planner.md) | [`planner-output.md`](../../../contracts/planner-output.md) |
| Step 3 — builders + Step 3.5 diff-sanity / footprint isolation | [`run-loop.md`](run-loop.md) §3, §3.5 | [`orchestrator-loop.md`](../../../contracts/orchestrator-loop.md) (footprint / `TASK_STEM`) |
| Step 4 — validator wave, dedup `(file, rule_id)`, adjudication, rotation + outer-reentry caps | [`validator-routing.md`](validator-routing.md) §4 | [`validation-run.md`](../../../contracts/validation-run.md); [`orchestrator-loop.md`](../../../contracts/orchestrator-loop.md) |
| Step 4.5 — assemble gate, 2-consecutive-FAIL cap | [`validator-routing.md`](validator-routing.md) §4.5 | [`orchestrator-loop.md`](../../../contracts/orchestrator-loop.md) |
| Step 4.6 — runtime verify gate, 3-invocation cap | [`validator-routing.md`](validator-routing.md) §4.6 | [`orchestrator-loop.md`](../../../contracts/orchestrator-loop.md) |
| Step 4.6b — screenshot fidelity gate, 3-invocation cap | [`validator-routing.md`](validator-routing.md) §4.6b | [`orchestrator-loop.md`](../../../contracts/orchestrator-loop.md) |
| Step 5 — external review (reviewer-agnostic), fallback policy | [`validator-routing.md`](validator-routing.md) §5 | [`reviewer-output.md`](../../../contracts/reviewer-output.md) |
| Step 5.5 — security review, 2-iteration cap, no severity threshold | [`validator-routing.md`](validator-routing.md) §5.5 | [`orchestrator-loop.md`](../../../contracts/orchestrator-loop.md) |
| Step 6 — chat summary + 6a Outcome draft (6 headings + Execution log) | [`outcome-appendix.md`](outcome-appendix.md) | [`acceptance-trace.md`](../../../contracts/acceptance-trace.md) |
| Step 6b–6d — recoverable finalizer: components/tokens phases, sanctioned move, INDEX/arch verification, lock release | [`outcome-appendix.md`](outcome-appendix.md) §6b–6d | [`orchestrator-loop.md`](../../../contracts/orchestrator-loop.md) |
| Drop / terminal-abandon a task | [`task-drop.md`](task-drop.md) | — |
| Keyword → skill-reference lookup helper | [`requirements-lookup.md`](requirements-lookup.md) | — |

## File map

| File | Covers |
|---|---|
| [`run-loop.md`](run-loop.md) | Steps 0–6 spine: bootstrap, rollback, lock/journal, intake, pre-flight, screen pre-flight, planner hand-off, builders, diff-sanity, ship hand-off, escalation + durable question publication, parallelism, hard rules, move-back |
| [`validator-routing.md`](validator-routing.md) | Steps 4 / 4.5 / 4.6 / 4.6b / 5 / 5.5: validator wave, dedup, adjudication, footprint-scoping, all caps, gate routing matrices, reviewer + security gates |
| [`outcome-appendix.md`](outcome-appendix.md) | Step 6 chat summary + 6a–6d: Outcome draft shape + recoverable `finalize-task` transaction (components/tokens phases, move, INDEX/arch checks, lock release) |
| [`planner.md`](planner.md) | implementation-planner: canonical names, files, signatures, acceptance mapping, builder contracts, behavioral edge-cases, output shape |
| [`context-finder.md`](context-finder.md) | context-finder: arch-map-first lookup, query recipes, figma component-binding match, MAP/REGISTRY/CONTRACT_MISS emit |
| [`requirements-lookup.md`](requirements-lookup.md) | requirements-lookup: keyword → skill-reference map, cookbook-recipe table, output shape |
| [`task-drop.md`](task-drop.md) | task-drop: canonical read-only impact receipt, explicit impact-hash acknowledgement, lock-absence fence, transactional task/journal removal, verified INDEX receipt |

## Frozen caps (cite [`orchestrator-loop.md`](../../../contracts/orchestrator-loop.md), never re-derive)

| Cap | Value | Reset scope |
|---|---|---|
| build-failure rollback hint | 2 builder retries | per task |
| BLOCKED loop (intake / inputs-resolver / planner) | 3 iterations | shared, per task |
| Step 4 rotation (stable finding-set) | no shrink across 2 consecutive cycles | per Step-4 entry session |
| Step 4 outer re-entry | 7 | per task, never reset mid-task |
| Step 4.5 assemble | 2 consecutive FAILs (PASS resets) | per task |
| Step 4.6 runtime verify | 3 invocations | per task |
| Step 4.6b screenshot | 3 invocations | per task |
| Step 5.5 security review | 2 iterations | per task |
| acceptance bullet repeatedly missing | 2 consecutive validator cycles | per task |
| site runner concurrency | `MAX_PARALLEL=1` (frozen serial safety; no env override until per-task worktree isolation) | n/a |

## Output contracts

- [`execution-plan.md`](../../../contracts/execution-plan.md) — resolved per-task routing (classification, builder order, validator set, gate sequence).
- [`planner-output.md`](../../../contracts/planner-output.md) — planner's file-level contract (referenced by the execution plan).
- [`orchestrator-loop.md`](../../../contracts/orchestrator-loop.md) — frozen loop mechanics (caps / dedup / footprint / parent-adopts-role).
- [`validation-run.md`](../../../contracts/validation-run.md) — validator-invocation envelope (always-on three + fail-closed conditionals).
- [`reviewer-output.md`](../../../contracts/reviewer-output.md) — shared reviewer output shape.
- [`acceptance-trace.md`](../../../contracts/acceptance-trace.md) — final `## Outcome` → `### Acceptance trace` shape.
