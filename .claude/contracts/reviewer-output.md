# Contract: reviewer-output

Both review paths normalize to the SAME output shape so downstream routing is
reviewer-agnostic. `internal-reviewer` emits it directly. The official Codex
plugin command (`Skill(skill: "codex:review", args: "--wait --scope working-tree")`)
returns native review output; the parent orchestrator must validate and
normalize that output before routing it.

Source: the `validation-gates` + `task-orchestrator` skills (reviewer roles).

## Frozen mechanics
- Both review paths yield the **same normalized output shape** — the loop routes findings the same way regardless of reviewer identity.
- Severity scale Critical/Major/Minor/Info/Style; **high-severity = Critical or Major** (blocking); Minor/Info/Style batched to `### Caveats`.
- A Codex response that cannot be normalized into either a classified findings
  list or an explicit clean verdict is an invocation failure, never a pass.
- Findings carry `rule_id`, severity, file, evidence, `routed_to`, fix; grouped by responsible builder; the reviewer never spawns builders (orchestrator owns routing).
- The reviewer is the pipeline's only logic-correctness gate; `Behavioral edge-cases` from the planner are its per-task check items.
