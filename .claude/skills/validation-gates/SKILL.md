---
name: validation-gates
description: Review a Grippo change against requirements, architecture, privacy, lifecycle, analytics coverage and state preservation before completion. Use for a diff review or implementation completion; source/manual checks and authorized builds replace no semantic gate.
---

# Validation and review

Read [project conventions](../../../CLAUDE.md), the user's accepted scope and the
actual change set. A review needs an identifiable outcome and footprint; recover
these from the request/source before asking for missing information.

## Mandatory review

1. MUST trace each requested outcome to actual code and callers. Review happy paths,
   rejected inputs, empty/nullable data, failure, cancellation and lifecycle edges
   relevant to the change. Do not infer runtime success from a keyword scan.
2. Apply [forbidden patterns](references/forbidden-patterns.md) and the owning
   skill's semantic completion gate. Review correctness, simplicity, dependencies,
   privacy and work done in frequently called paths.
3. Feature behavior changes require [analytics](../feature-analytics/SKILL.md):
   actual route/surface → Component → VM → screen identity → hook coverage, eligible
   callbacks, complete-operation results/errors and explicit event omissions.
   Independent screens MUST have exactly one screenPresentation; containers MUST keep null.
   Missing/wrong hooks and duplicate views block completion; an omission cannot waive them.
4. Feature changes require [preservation](../state-preservation/SKILL.md): actual
   input/selection/filter/parent owners, saved drafts, rebuilt sources, secret/runtime
   exclusions, fresh restoration wiring and no shadow flags/caches. Dialog sessions
   remain unsaved; accepted results belong to the screen's draft. No-state changes
   need an impact assessment only.
5. UI/design work compares real consumers and declared design states through
   [visual review](references/visual-review.md). Source compliance does not prove
   rendered fidelity; note states/platforms that could not be inspected.
6. Review real source/diff and run checks within the user's authorization. Builds
   prove compilation; static checks do not prove native lifecycle, SDK delivery,
   generated DI runtime or restoration. MUST follow the canonical
   [verification policy](../../../CLAUDE.md#verification-policy). Builds needed within
   task scope do not require separate consent.

## Findings and review loop

Report file/line, concrete trigger, consequence and required correction. Severity:
blocking (incorrect behavior, broken contract, privacy leak or mandatory gate),
nonblocking suggestion, or cosmetic nit. Preserve useful evidence from multiple
reviewers; route a shared defect once, at its highest justified severity.

MUST apply the canonical [execution/review loop](../task-orchestrator/SKILL.md#execution-loop)
for self-review, required independent review, authorized delegation and recurring
findings. An explicit user decision to retain a known behavior closes that finding
as an accepted risk, not a fix or PASS. Record its trigger and consequence; silence
is not acceptance and cannot waive unrelated mandatory coverage gates.

## Completion record

Record reviewed scope, unresolved findings, actual checks/results and unverified
runtime/platform behavior. No missing input or unrun check is a PASS. An unavailable
runtime/build check alone does not block completion after the required source gate,
provided its limitation is explicit and no unaccepted source defect remains.
For scope decisions use [when to ask](references/when-to-stop-and-ask.md).

## Final response format

Unless the user requests another format, task completion replies MUST be concise
but informative, in plain Russian, structured as lists under bold section labels.
No tables. Give each meaningful result enough detail to understand what changed
and why: usually 1–2 short sentences per item. Include concrete scope/counts or a
before/after example when useful. Avoid vague claims such as "fixed everything".
Use only status emojis: ✅ completed/verified, ⚠️ partial completion, deviation or
unverified check, ❌ blocker or requested work left undone. Use them on the overall status line and status list
items, not section headings or layer names; location lists and question options
need no emoji.

MUST start with one overall status line: ✅ **Задача завершена**,
⚠️ **Завершена частично — <missing requested outcome>**, or
❌ **Задача заблокирована — <specific blocker>**. Choose by requested outcomes,
not effort or number of checks. An unrun runtime check alone does not mean partial
completion; describe it under Не проверено. Immediately below the status, state
any limitation that materially affects use of the result or a required decision.
Keep ordinary verification limits in their section; do not repeat the same warning.
If all useful remaining work depends on an unresolved external condition or user
decision, use blocked even when a portion is ready. Partial means some requested
outcomes are ready and useful work can still proceed (e.g. a requested checkpoint).
This format does not authorize stopping an actionable task early. A scope reduction
requires user agreement before calling the reduced task complete.

Use the following order, omitting every empty or unhelpful section:
- **Сделано** — user-visible results and their purpose, not actions or a file-edit
  inventory. For internal/code/rules tasks, describe the resulting guarantee or
  convention. Implementation locations belong under Где изменил.
- **Где изменил** — affected layers in text (UI, navigation, data, analytics,
  project rules, etc.) and what changed there. Link key files when useful; group
  repetitive edits. For large tasks, MUST show affected layers. Use a short key-file
  tree for new/changed structure or an explicit request; otherwise group repetitive
  edits without a tree. Annotate relevant paths with their purpose/change, distinguish
  changed from planned files and link real key files outside the code block.
  Do not repeat results or list unchanged files/exhaustive inventories.
- **Что мешает продолжить** — for blocked work, identify the missing fact/access
  or dependency, which remaining outcome it prevents and what would unblock it.
  Add concrete detail beyond the brief opening limitation; do not repeat it verbatim.
- **Проверено** — actual checks, their scope and outcomes; compilation is not runtime proof.
- **Отклонения от правил проекта** — actual deviations, reason and whether agreed.
- **Не сделано** — requested outcomes still missing. Give the reason here only
  if it is not already explained in the opening limitation or blocker section.
- **Не проверено** — relevant unrun checks and what remains uncertain, separate
  from undone work. Give the reason when known and useful; do not invent it to
  fill the template. Mention only limitations material to the task.
- **Что проверить вручную** — actionable scenarios only when they help the user.
- **Вопросы** — unresolved questions numbered separately, each with 2–3 options
  labelled (А), (Б), (В) as needed. Mark and briefly justify the recommended option.
  If no recommendation has enough evidence, say which fact is missing instead
  of inventing a preferred answer. Ask during work when needed, not only at the end;
  the final section contains only still-unresolved questions.

MUST NOT invent questions, repeat answered decisions, include empty success claims
or end with a paragraph repeating the lists. Scale detail to the task; a small fix
may need only Сделано and Проверено. Preserve material limitations even in a short reply.

See [response examples](references/response-examples.md) for completed, partial and
blocked tasks. Read the relevant example only; examples guide detail, never supply
claims or require every section.
