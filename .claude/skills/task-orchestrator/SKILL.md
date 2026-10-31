---
name: task-orchestrator
description: Execute Grippo implementation slices with explicit ownership, independent review, checks and handoff. Apply to implementation and rulebook changes.
---

# Task execution

The main session is the orchestrator. It may implement work itself; independence
comes from reviewer agents that did not edit the task's files. Agent definitions
live in `.claude/agents/`; the roster is in
[CLAUDE.md](../../../CLAUDE.md#workflow-and-agents).

## Execution loop

1. **Start from the prepared scope** ([task-prep](../task-prep/SKILL.md)). Capture
   the initial working state under [change attribution](references/change-attribution.md)
   before editing, and read the owning skills of the slices you will touch.
2. **Implement the smallest coherent slice** that meets a concrete acceptance
   outcome, yourself or through an implementer agent (rules below). Update every
   necessary caller inside the authorized scope; never leave a broken caller for
   a hypothetical follow-up. Gather prerequisites first: backend evidence through
   `backend-contract-auditor` before a wire change; the usage/state inventory
   before a shared visual change; the admin-sync access attempt early for
   analytics work so a missing browser tab is requested while source work continues.
3. **Self-review the task delta against the baseline** and its source context
   for correctness, simplicity, boundaries, privacy,
   lifecycle and cancellation against the owning skill's completion gate,
   [forbidden patterns](../validation-gates/references/forbidden-patterns.md) and
   [code style](../validation-gates/references/code-style.md). Fix what you find.
4. **Independent review.** Required for every change that is not a small
   editorial change ([definitions](../../../CLAUDE.md#definitions)):
   - `code-reviewer` for every reviewed change;
   - `gate-auditor` for a behavior change or visual-only impact assessment
     (analytics coverage and preservation);
   - `design-reviewer` for a visual change (with renders when available);
   - `analytics-admin-auditor` whenever the
     [admin-sync gate](../feature-analytics/references/admin-sync.md#trigger-and-access-gate-must)
     applies (it owns the trigger).
   Send the brief below; receive findings with `path:line` and severity. The
   author's self-review never counts as independent review.
5. **Resolve findings.** Fix every blocking finding, then obtain independent
   re-review of the fixes and affected paths against the current task delta.
   Retain review conclusions for unchanged paths unless the fixes affect them.
   A finding the user explicitly chose to keep is an accepted risk, recorded
   with its trigger and consequence - never a fix or a pass. If the same finding
   returns twice, reassess its root cause before changing anything else; ask only
   for a real product or scope blocker. Never repeat an unchanged attempt or
   redefine a failure as a pass.
6. **Checks.** Run the default build rule from the
   [verification policy](../../../CLAUDE.md#verification-policy) through
   `build-verifier` (a single-module compile may run directly), using the
   [unavailable-delegation fallback](references/agent-runtimes.md#unavailable-delegation)
   when needed; run runtime
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
- Delegate implementation when a ready slice has disjoint owned paths, its
  prerequisites are available, and parallel progress or a separate source context
  outweighs briefing and integration cost; also delegate when the user asks.
  Match the slice to `ui-`, `data-`, `design-`, `platform-implementer` or
  `rulebook-maintainer`. Implement a tightly coupled slice in the main session.
  A count of skills or files alone does not decide delegation. Independent
  review still applies to the task under the execution loop above.
- One writer per file, always. Dependent edits run sequentially; independent
  slices on disjoint paths may run in parallel. Shared wiring files belong to one
  slice, by default the main session: `screen-api` routers, `shared/.../Koin.kt`,
  `shared/build.gradle.kts`, `settings.gradle.kts`, `DialogConfig`,
  `DialogContentComponent`, `strings.xml`, `CONSISTENCY.md`.
- Launch roles and apply their rights through
  [agent runtimes](references/agent-runtimes.md), for both Claude Code and Codex.
  Briefs are written in English; replies to the user follow the final response
  format. Prefer a fresh bounded context for an independent slice when the runtime
  supports it. Include everything the role needs regardless of context
  inheritance; send concrete source paths and acceptance criteria, not the
  full parent transcript.
- Linked sessions count as writers: a file another session is editing is
  off-limits until that session reports it done; coordinate by message, never
  by editing the same file.

### Brief template

```
Outcome and acceptance criteria: ...
Owned paths (edit only these): ...
Role definition and runtime capabilities/protection: ...
Baseline location, initial user changes and task-only delta: ...
Prerequisites available: contracts / evidence / inventory / decisions ...
Gates that must pass: analytics (screens, callbacks), preservation (fields), visual (states), backend evidence ...
Build authorized: :androidApp:assembleDebug [+ XCFramework] / none
Report: the agent's report format from its definition
```

Required additions per agent (each definition's "Inputs you must receive"):

| Agent | Add to the brief |
|---|---|
| `code-reviewer` | request, task delta against baseline, decisions taken, checks run with results, owning skills |
| `gate-auditor` | feature(s), changed files, new vs existing feature, the author's claims per slice (visual-only / no state impact) |
| `design-reviewer` | visual intent or supplied design, usage/state inventory, accepted departures, renders (path, theme, locale, viewport) |
| `backend-contract-auditor` | operations/schemas, evidence location and revision, mobile files involved |
| `analytics-admin-auditor` | scope, audit-only or authorized to change, property/streams or "ask the user", release facts |
| `scout` | the question, directories to search, facts wanted per hit |
| implementers | target module and names, contracts and stubs available, decisions, build authorization |
| `rulebook-maintainer` | rule owners, evidence for corrections, index updates required, owned rulebook paths |
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
