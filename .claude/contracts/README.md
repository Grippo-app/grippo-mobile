# orchestrator/contracts — frozen pipeline contracts

Stable mechanical shapes of the orchestrator pipeline. Each `*.md` pins a
shape or invariant so the skills can share one explicit contract. These files
are reference records rather than generated artifacts. Structural drift is
guarded by `orchestrator/skills/checks/` and `orchestrator/lint.sh`.

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
| `agents/<agent>.md` ×40 | roster | frozen per-role capability contracts | FROZEN ✓ |

The skills cite the 11 versioned contracts and 40 per-role records by name. The
guard surface is the skill gate suite plus `orchestrator/lint.sh`.

`outcome-shape.json` is different in kind: it is the one LIVE machine-read data
contract here. Its consumers load it at runtime, so editing it changes the
board badge, the Task Details result projection, and the ship gate at once —
which is the point.
