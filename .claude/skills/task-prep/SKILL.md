---
name: task-prep
description: Clarify a Grippo task before coding - intended outcome, authorized scope, affected owners and platforms, open decisions as a short quiz, and implementation slices ordered by real dependencies with file ownership for delegation. Use for any non-trivial request, an ambiguous feature request or a change spanning several owners.
---

# Task preparation

Read [CLAUDE.md](../../../CLAUDE.md) (definitions, module map, policies) and the
affected source before planning. Preparation produces the scope and acceptance
criteria the implementation needs; it never invents product behavior, backend
contracts or infrastructure that does not exist.

## Output of preparation

Keep it in the reply (the task report), in the user's language like every reply
(Russian by default), as short lists:

- **Outcome** - what the user will be able to do or see, as acceptance criteria.
- **Scope** - the authorized scope and its exclusions; what would count as scope
  expansion (see [definitions](../../../CLAUDE.md#definitions)).
- **Owners** - affected modules, the owning skills from the
  [skills index](../../../CLAUDE.md#skills-index), and platforms.
- **Facts** - confirmed requirements, source-supported defaults, open decisions.
- **Slices** - ordered implementation slices, each with outcome, owned paths,
  prerequisites and the agent or session that will implement it.
- **Gates** - which gates the change triggers: behavior change -> feature-analytics
  and state-preservation; visual change -> brand-design and design-system; wire
  change -> backend evidence; new module -> platform checklist.
- **Checks** - the default build rule from the
  [verification policy](../../../CLAUDE.md#verification-policy) and the runtime
  scenarios worth running if the user allows the app to run.

## Questions

Use [scope questions](../validation-gates/references/when-to-stop-and-ask.md)
for the triggers, wording and handling of unanswered decisions.

## Dependency plan

1. Inspect the owning source, build files and callers before choosing names and
   shapes. Delegate an inventory to the `scout` agent when it spans more than about
   ten files or crosses module boundaries (every caller of a widget, all routes of
   a feature, module edges) and plan from its facts.
2. Route each part of the work to its owner: UI and navigation -> ui-feature;
   domain, storage and network -> data-layer (+ backend-contract-client for wire
   changes); conversions -> mappers; bindings -> di-modules; tokens, components,
   resources -> design-system; visual intent -> brand-design; artwork ->
   illustrations; build, modules, toolkit, shells -> platform-build-toolkit.
   Read only the references the task kind needs.
3. Order by producers and consumers: a domain contract before the screen that
   consumes it; verified backend evidence before an endpoint; source and target
   types before a mapper; tokens and components before the screens that use them;
   a route in `screen-api` before the Component that navigates to it. Dialogs
   have the same prerequisites as screens.
4. Split into slices whose owned paths do not overlap. Files shared by several
   slices (`screen-api` routers, `shared/.../Koin.kt`, `DialogConfig`,
   `CONSISTENCY.md`, `strings.xml`) belong to exactly one slice or to the main
   session; dependent slices run sequentially.
   Establish the [task baseline](../task-orchestrator/references/change-attribution.md)
   before the first edit; include existing user changes in delegation briefs.
5. Enumerate per slice the gate items that will have to pass: screens needing a
   `screenPresentation`, fields needing a draft, callers needing migration,
   locales needing strings.
6. Decide which checks the task can run and what they establish; name the runtime
   scenarios that would prove the behavior if the user allows running the app.

## Preparation gate

A slice is runnable when its outcome is checkable, its owner and owned paths are
named, its prerequisite types/evidence exist or are produced by an earlier slice,
and every behavior-affecting input is resolved. An unavailable prerequisite is
reported with the exact missing fact and the work that remains possible.

Hand the scope, ordered slices, accepted decisions, gates and limits to
[task-orchestrator](../task-orchestrator/SKILL.md). When the user already asked
for the implementation and the inputs are clear, start executing; a plan does not
need separate approval.
