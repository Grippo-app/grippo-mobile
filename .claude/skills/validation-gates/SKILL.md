---
name: validation-gates
description: Review a Grippo change against the request, architecture, forbidden patterns, code style, privacy, lifecycle and the owning skills' gates, decide completion and write the final reply in the project format. Use for every diff review and every completion; owns forbidden patterns, code style, scope questions and the final response format.
---

# Validation and review

Read [CLAUDE.md](../../../CLAUDE.md), the user's accepted scope and the actual
change set (`git diff`, `git status`). A review needs an identifiable outcome and
footprint; recover them from the request and the source before asking.

## Mandatory review

1. Trace each requested outcome to code and callers. Review the happy path,
   rejected input, empty/nullable data, failure, cancellation and lifecycle edges
   relevant to the change. A keyword scan is not a review.
2. Apply [forbidden patterns](references/forbidden-patterns.md) and
   [code style](references/code-style.md), then the owning skill's completion gate
   ([skills index](../../../CLAUDE.md#skills-index)). Review correctness,
   simplicity, dependency direction, privacy and work done in frequently called paths.
3. A behavior change passes the [analytics screen coverage gate](../feature-analytics/SKILL.md#mandatory-screen-coverage-gate)
   (every independent screen has exactly one `screenPresentation`; containers keep
   `null`; typed facts only) and the [preservation gate](../state-preservation/SKILL.md#mandatory-preservation-gate).
   The [admin synchronization gate](../feature-analytics/references/admin-sync.md)
   applies under the trigger it owns; missing admin access leaves that task
   unfinished, not merely unverified.
4. A visual change passes [brand-design acceptance](../brand-design/references/workflow-and-review.md#design-acceptance)
   and the design-system gates; artwork passes the
   [illustration completion gate](../illustrations/SKILL.md#completion). Evidence
   handling is owned by [visual consistency](../design-system/references/visual-consistency.md).
5. Checks follow the [verification policy](../../../CLAUDE.md#verification-policy):
   an available, authorized check must have run; compilation proves no runtime,
   visual or restoration behavior.

## Findings

Report `path:line`, the concrete trigger, the consequence and the required
correction at its real owner. Severity:

- **blocking** - incorrect behavior, broken contract, privacy leak, forbidden
  pattern, or a mandatory gate not met;
- **suggestion** - a real improvement that does not block completion;
- **nit** - cosmetic.

A deviation from [code style](references/code-style.md) in changed lines is at
least a suggestion; it is blocking when [forbidden patterns](references/forbidden-patterns.md)
names it.

A defect reported twice is routed once at its highest justified severity. A
behavior the user explicitly kept is an accepted risk with its trigger and
consequence recorded; it is neither a fix nor a pass. Silence is not acceptance
and cannot waive a mandatory gate. The fix-and-re-review loop and the recurring
findings policy are owned by [task-orchestrator](../task-orchestrator/SKILL.md).

## Completion

The [definition of done](../../../CLAUDE.md#definition-of-done) decides.
Record the reviewed scope, unresolved findings, actual checks with results and
the unverified runtime/platform behavior. An unrun check is never a pass. For
scope decisions use [when to stop and ask](references/when-to-stop-and-ask.md).

## Final response format

This format applies to the main session's reply to the user. Subagents report in
the format their definition prescribes; tables belong in record files, not in the
reply. A verifier's evidence table (scenario / expected / observed / screenshot)
is summarized as a list in "Проверено", with the screenshot paths named; the
table itself stays in the agent's report in the session transcript.

Unless the user asks for another format, a completion reply is concise but
informative, in plain Russian, structured as lists under bold section labels.
No tables. Each meaningful item gets enough detail to understand what changed and
why: usually one or two short sentences, with concrete counts or a before/after
example when useful. No vague claims such as "fixed everything". Status emojis
only: ✅ completed/verified, ⚠️ partial, deviation or unverified check, ❌ blocker
or requested work left undone - on the overall status line and status items, not
on section headings; location lists and question options carry no emoji.

Start with one overall status line: ✅ **Задача завершена**,
⚠️ **Завершена частично — <missing requested outcome>**, or
❌ **Задача заблокирована — <specific blocker>**. Choose it by requested outcomes,
not by effort. Blocked means every useful remaining step depends on an external
condition or a user decision, even if part of the work is ready. Partial means
some requested outcomes are ready and useful work can still proceed. An unrun
runtime check alone never makes a task partial; it goes under "Не проверено".
Directly below the status, state any limitation that materially affects using the
result or a decision the user must make; do not repeat it in the sections. This
format never authorizes stopping an actionable task early, and a scope reduction
needs the user's agreement before the reduced task is called complete.

Sections, in this order, omitting empty ones:

- **Сделано** - user-visible results and their purpose, not a file-edit inventory.
  For internal/rule tasks, the resulting guarantee or convention.
- **Где изменил** - affected layers (UI, navigation, data, analytics, project
  rules, ...) and what changed there; link key files; group repetitive edits. A
  short key-file tree only for new or changed structure or on request.
- **Что мешает продолжить** - for blocked work: the missing fact, access or
  dependency, the outcome it prevents and what would unblock it.
- **Проверено** - actual checks, their scope and outcomes; compilation is not
  runtime proof.
- **Отклонения от правил проекта** - actual deviations, the reason, and whether
  the user agreed.
- **Не сделано** - requested outcomes still missing, with the reason if not
  already stated above.
- **Не проверено** - relevant unrun checks and what remains uncertain, separate
  from undone work; only material limitations.
- **Что проверить вручную** - actionable scenarios only when they help the user.
- **Вопросы** - still-unresolved questions, numbered, each with 2-3 options
  labelled (А), (Б), (В); the recommended option first with a brief reason. Ask
  during the work when the answer is needed; this section holds only what is
  still open.
- **Найдено попутно** - unrelated defects discovered during the task, with
  `path:line` and the trigger, left unfixed.

Do not invent questions, repeat answered decisions, include empty success claims
or end with a paragraph repeating the lists. Scale detail to the task: a small fix
may need only "Сделано" and "Проверено"; material limitations survive even in a
short reply. Delivery ends the reply with the proposed commit message from
[delivery](../delivery/SKILL.md) when files changed and no commit was requested.

See [response examples](references/response-examples.md) for completed, partial
and blocked tasks; they guide tone and detail, never supply claims.
