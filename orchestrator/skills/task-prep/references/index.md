# task-prep references — routing table

Self-contained reference pack for the `task-prep` skill — the backlog→todo preparation MECHANICS.
These files carry the skill's own normative rules. Start here; read routed external authorities
only where the cross-reference table below names them.

task-prep is MECHANICS. Builder ordering follows the frozen contract
`orchestrator/contracts/builder-order.md`; the per-agent surface (name/tools/inputs/outputs/stops)
is frozen by `orchestrator/contracts/agents/task-intake.md` and `task-prep.md`.

Route by prep topic.

| Prep topic | Read first | Contract |
|---|---|---|
| Which builders apply (change-kind taxonomy) + prerequisites + **builder order** + validator gate + intake output plan | [`intake-classification.md`](intake-classification.md) | `contracts/builder-order.md`, `contracts/agents/task-intake.md` |
| Full prep flow — Modes A/B, pre-flight, lock + journal, gap analysis, promote-vs-ask decision, questions sidecar, Mode B convergence, INDEX regen, lock release, Step-8 output blocks | [`prep-flow.md`](prep-flow.md) | `contracts/agents/task-prep.md` |
| Promoted todo shape — acceptance **Automated/Manual** split, per-bullet automation anchors, build gate, conditional `## Design` + spec-gate bullets, five **defensive** Out-of-scope bullets | [`acceptance-anchors.md`](acceptance-anchors.md) | `contracts/agents/task-prep.md` |
| Figma census **design-system-first split** — cache pre-flight, component census, AMBIGUOUS→ask, MISSING/INCOMPLETE→split, `## Origin` lineage, parent `## Depends on` chaining, `## Preview states` + screenshot-gate bullets | [`figma-split.md`](figma-split.md) | `contracts/agents/task-prep.md` |
| Every **BLOCKED / ESCALATE** condition — dependency gate, cross-column collision, migration-auth / backend-contract / refactor escalations, convergence-stall ESCALATE, BLOCKED-recovery loop | [`blockers-and-dependencies.md`](blockers-and-dependencies.md) | `contracts/agents/task-intake.md`, `contracts/agents/task-prep.md` |

## File map

| File | Covers |
|---|---|
| [`intake-classification.md`](intake-classification.md) | Change-kind taxonomy (canonical table), prerequisite resolution, builder order (low→high, hard-ordering + prepend rules), validator gate selection, intake execution-plan output |
| [`prep-flow.md`](prep-flow.md) | Modes A/B, authoritative reading, Step 0 writer authority + lock + journal, canonical validator/revision fence, Step 1 classify, Step 2 gap analysis, Step 3 decide, Step 4 helper-owned questions publication, Step 5 transactional promote/rollback, Step 6 Mode B convergence, Step 7 verified INDEX receipt, Step 7.5 release, Step 8 output blocks |
| [`acceptance-anchors.md`](acceptance-anchors.md) | Standard todo shape, the four required sections, `### Automated`/`### Manual` split, automation-anchor gate, build gate, design + spec-gate bullets, five defensive Out-of-scope bullets, frozen-snapshot preservation |
| [`figma-split.md`](figma-split.md) | Step 5.5 census + design-system-first split, ID-anchored component identity, splitting policy; Step 5.5a `## Preview states` + screenshot-gate bullets |
| [`blockers-and-dependencies.md`](blockers-and-dependencies.md) | Dependency gate (prepare warning → Run blocker, self-dependency/cycle hard stops), task-file state BLOCKs, cross-column collision, input/format BLOCKs, Figma cache pre-flight BLOCK, intake escalations (migration/contract/refactor/multi-screen), convergence-stall ESCALATE, BLOCKED recovery |

## Cross-reference (read for classification, owned by another skill)

task-prep reads these to classify, but they are owned by other skills — not part of this pack:

| read for | source |
|---|---|
| Recipe recognition (which builder a backlog maps to) | the implementation skills (ui-feature/data-layer/design-system/mappers) recipe references |
| Escalation triggers (when to stop and ask) | `../../validation-gates/references/when-to-stop-and-ask.md` |
| Project context flags (`figmaEnabled`, locales, framework name) | `orchestrator/project-config.md` |
| Screen cache + component census contract | `screensPrompt` in `orchestrator/site/scripts/figma-actions.js` (the authoring contract — no markdown mirror) |
| Lifecycle, board mechanics, todo/pending/outcome shapes | `orchestrator/tasks/README.md` |

Always pre-flight any path with `[ -f <path> ]` before relying on it; a miss is a BLOCKED return.
