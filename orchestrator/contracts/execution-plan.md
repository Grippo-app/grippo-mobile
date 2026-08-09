# Contract: execution-plan

The orchestrator's resolved per-task execution plan — composed from already-pinned parts, not invented: task classification (from `task-intake`), builder routing order (`builder-order`), the validator set (`validation-run`), the gate sequence (Step 4 / 4.5 / 4.6 ordering), and a reference to the `planner-output` it was derived from.

Distinct from `planner-output` (file-level contract).

## Required headings, in order

1. `# Execution plan — TASK_<N>_<title>`
2. `## Classification` — the change kinds `task-intake` resolved.
3. `## Builder routing (order)` — the resolved builder sequence (must obey `builder-order`).
4. `## Validator set` — always-on three + conditional validators that gate completion.
5. `## Test gate` — the resolved pre-builder test policy for this task: policy `version`/`policyHash`, required suites/lanes/capabilities from the sealed planned impact (`test-impact.planned.json`, referenced by `impactHash`), or the single typed `test-not-applicable` line. Never free-form shell — logical suite/lane ids only.
6. `## Gate sequence` — Step 4 (compile) → 4.4 (tests) → 4.5 (assemble) → 4.6 (verify) → 4.6b (screenshot) → 5 (review) → 5.5 (security) ordering for this task.
7. `## Planner reference` — the `planner-output` plan this routes.

Parser rejects missing/reordered headings. Builder routing must not violate the `builder-order` hard-ordering rules.
