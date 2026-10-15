---
name: task-orchestrator
description: Execute a Grippo task - implement slices yourself or through the implementer agents, self-review, obtain independent review from the reviewer agents, resolve findings, run the default checks and hand off. Use for any implementation task; owns the delegation rules, agent briefs, file ownership and the recurring-findings policy.
---

# Task execution

The main session is the orchestrator. It may implement work itself; independence
comes from reviewer agents that did not edit the task's files. Agent definitions
live in `.claude/agents/`; the roster is in
[CLAUDE.md](../../../CLAUDE.md#workflow-and-agents).

## Execution loop

1. **Start from the prepared scope** ([task-prep](../task-prep/SKILL.md)). Run
   `git status --short`; the working tree usually holds the user's unrelated,
   uncommitted work - preserve it, never reset or revert it. Read the owning
   skills of the slices you will touch.
2. **Implement the smallest coherent slice** that meets a concrete acceptance
   outcome, yourself or through an implementer agent (rules below). Update every
   necessary caller inside the authorized scope; never leave a broken caller for
   a hypothetical follow-up. Gather prerequisites first: backend evidence through
   `backend-contract-auditor` before a wire change; the usage/state inventory
   before a shared visual change; the admin-sync access attempt early for
   analytics work so a missing browser tab is requested while source work continues.
3. **Self-review the diff** for correctness, simplicity, boundaries, privacy,
   lifecycle and cancellation against the owning skill's completion gate,
   [forbidden patterns](../validation-gates/references/forbidden-patterns.md) and
   [code style](../validation-gates/references/code-style.md). Fix what you find.
4. **Independent review.** Required for every change that is not a small
   editorial change ([definitions](../../../CLAUDE.md#definitions)):
   - `code-reviewer` for every reviewed change;
   - `gate-auditor` for a behavior change (analytics coverage and preservation);
   - `design-reviewer` for a visual change (with renders when available);
   - `analytics-admin-auditor` whenever the
     [admin-sync gate](../feature-analytics/references/admin-sync.md#trigger-and-access-gate-must)
     applies (it owns the trigger).
   Send the brief below; receive findings with `path:line` and severity. The
   author's self-review never counts as independent review.
5. **Resolve findings.** Fix every blocking finding and re-review the changed
   area. A finding the user explicitly chose to keep is an accepted risk, recorded
   with its trigger and consequence - never a fix or a pass. If the same finding
   returns twice, reassess its root cause before changing anything else; ask only
   for a real product or scope blocker. Never repeat an unchanged attempt or
   redefine a failure as a pass.
6. **Checks.** Run the default build rule from the
   [verification policy](../../../CLAUDE.md#verification-policy) through
   `build-verifier` (a single-module compile may run directly); run runtime
   scenarios through `runtime-verifier`
   when the user allows the app to run. If fixes changed checked code, repeat only
   the affected checks. Update the [records](../../../CLAUDE.md#records) the task
   changed.
7. **Complete** against the [definition of done](../../../CLAUDE.md#definition-of-done),
   reply in the [final response format](../validation-gates/SKILL.md#final-response-format),
   and end with the [delivery](../delivery/SKILL.md) hand-off (proposed commit
   message; no commit unless asked).

## Delegation rules

- Always allowed, no user permission needed: `scout`, `code-reviewer`,
  `design-reviewer`, `gate-auditor`, `backend-contract-auditor`, `build-verifier`;
  `runtime-verifier` and `analytics-admin-auditor` when the user allows running
  the app or using the browser tab respectively.
- Implementer agents (`ui-`, `data-`, `design-`, `platform-implementer`) are used
  when a task spans two or more owning skills or when the user asks for delegation.
  Otherwise implement in the main session.
- One writer per file, always. Dependent edits run sequentially; independent
  slices on disjoint paths may run in parallel. Shared wiring files belong to one
  slice, by default the main session: `screen-api` routers, `shared/.../Koin.kt`,
  `shared/build.gradle.kts`, `settings.gradle.kts`, `DialogConfig`,
  `DialogContentComponent`, `strings.xml`, `CONSISTENCY.md`.
- Subagents never commit, never expand scope, never delegate further, never
  change infrastructure outside their brief. The Bash guard enforces the git
  and build limits; the brief enforces the rest.
- Built-in `Explore`/`Plan` agents may do read-only research; they are not
  independent reviewers because they load no project gates.
- Agents start with a fresh context: the brief must carry everything they need.
  Briefs are written in English (the agents' working language); replies to the
  user follow the final response format.
- Linked sessions count as writers: a file another session is editing is
  off-limits until that session reports it done; coordinate by message, never
  by editing the same file.
- Agents are loaded at session start. If a roster agent is not available in the
  current session (it was just added or changed), report independent review as
  unperformed and ask the user to start a new session; a `general-purpose` agent
  loads no project gates and has no guard, so it does not count as independent
  review. Never report an independent review that did not happen.

### Brief template

```
Outcome and acceptance criteria: ...
Owned paths (edit only these): ...
Prerequisites available: contracts / evidence / inventory / decisions ...
Gates that must pass: analytics (screens, callbacks), preservation (fields), visual (states), backend evidence ...
Build authorized: :androidApp:assembleDebug [+ XCFramework] / none
Report: the agent's report format from its definition
```

Required additions per agent (each definition's "Inputs you must receive"):

| Agent | Add to the brief |
|---|---|
| `code-reviewer` | request, diff command, decisions taken, checks run with results, owning skills |
| `gate-auditor` | feature(s), changed files, new vs existing feature, the author's claims per slice (visual-only / no state impact) |
| `design-reviewer` | visual intent or supplied design, usage/state inventory, accepted departures, renders (path, theme, locale, viewport) |
| `backend-contract-auditor` | operations/schemas, evidence location and revision, mobile files involved |
| `analytics-admin-auditor` | scope, audit-only or authorized to change, property/streams or "ask the user", release facts |
| `scout` | the question, directories to search, facts wanted per hit |
| implementers | target module and names, contracts and stubs available, decisions, build authorization |
| `build-verifier` | targets, changed files, platform restrictions |
| `runtime-verifier` | scenarios with expected outcomes, platforms, theme/locale matrix, literal absolute scratchpad path, sign-in arrangement |

## Working with findings

Severity and format are owned by [validation-gates](../validation-gates/SKILL.md).
Route a defect reported by several reviewers once, at its highest justified
severity; keep each reviewer's evidence. A finding without `path:line` and a
concrete trigger is a question for the reviewer, not a fix to apply blindly.

## Completion

The [definition of done](../../../CLAUDE.md#definition-of-done) decides; this
skill adds nothing to it. Reports stay proportionate to the task: a one-file fix
reports what changed and what was checked; a multi-slice task reports per layer.
