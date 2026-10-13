# Installed orchestrator contract references

Stable mechanical shapes of the orchestrator pipeline. Each `*.md` pins a
shape or invariant so the skills can share one explicit contract. These files
are reference records rather than generated artifacts. A configured orchestrator
guards structural drift with its `orchestrator/skills/checks/` and
`orchestrator/lint.sh`; those runtime tools are not installed in this checkout.

## Contract roster

| Contract | Kind | Contract source | Status |
|---|---|---|---|
| `planner-output` | shape | the `task-orchestrator` skill (`references/planner.md`) | FROZEN ✓ |
| `orchestrator-loop` | drift | the `task-orchestrator` skill | FROZEN ✓ |
| `builder-order` | drift | the `task-prep` skill | FROZEN ✓ |
| `execution-plan` | shape | composed (intake + builder-order + validator-set + gate-seq) | FROZEN ✓ |
| `builder-report` | shape | builder report envelope | FROZEN ✓ |
| `validation-run` | drift | orchestrator validator-set rules | FROZEN ✓ |
| `validator-finding` | shape | normalized finding JSON | FROZEN ✓ |
| `acceptance-trace` | drift | `outcome-shape.json` headings + verdicts | FROZEN ✓ |
| `outcome-shape.json` | data (runtime-read) | canonical `## Outcome` enum sets + headings — runtime-read by the task-state core and `figma/scripts/ship-done.mjs`; the site consumes the server projection without a browser copy | ACTIVE |
| `acceptance-tracer-report` | drift | the `validation-gates` skill | FROZEN ✓ |
| `reviewer-output` | drift | README + codex/internal-reviewer | FROZEN ✓ |
| `launch-sequence` | drift | `launch.md` (Step 0–14 + half-steps) | FROZEN ✓ |
| `agents/<agent>.md` ×39 | roster | frozen per-role capability contracts | FROZEN ✓ |

The skills cite these frozen contracts and per-role records by name. Their runtime
gate suite applies only in a configured orchestrator workspace; installed records
alone do not establish that a gate ran.

`outcome-shape.json` is machine-read data in a configured orchestrator. Its board,
Task Details and ship-gate consumers read it at runtime. The installed copy here
does not create those consumers.
