# Contract: execution-plan

The orchestrator's resolved per-task execution plan — composed from already-pinned parts, not invented: task classification (from `task-intake`), builder routing order (`builder-order`), the validator set (`validation-run`), the gate sequence (Step 4 / 4.5 / 4.6 ordering), and a reference to the `planner-output` it was derived from.

Distinct from `planner-output` (file-level contract).

## Required headings, in order

1. `# Execution plan — TASK_<N>_<title>`
2. `## Classification` — the change kinds `task-intake` resolved.
3. `## Builder routing (order)` — the resolved builder sequence (must obey `builder-order`).
4. `## Validator set` — always-on three + conditional validators that gate completion.
5. `## Gate sequence` — Step 4 (validators) → 4.5 (permitted assemble) → 4.6 (permitted runtime verify) → 4.6b (manual visual review) → 5 (review) → 5.5 (security) ordering for this task. Record excluded/unavailable checks as unrun.
6. `## Planner reference` — the `planner-output` plan this routes.

Parser rejects missing/reordered headings. Builder routing must not violate the `builder-order` hard-ordering rules.
