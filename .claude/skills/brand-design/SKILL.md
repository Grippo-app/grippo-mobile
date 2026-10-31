---
name: brand-design
description: Design and review Grippo product UI, visual intent, domain semantics and usage/state acceptance. Apply to new or visually changed UI; pair decorative artwork with illustrations.
---

# Grippo brand design

Read [CLAUDE.md](../../../CLAUDE.md) and the affected source. Grippo uses calm
neutral structure, deliberate warmth, soft substantial forms and readable training
information. Artwork supports the task; it does not make every surface a hero.

MUST apply this skill to new product UI, visual changes and visual reviews of
screens, dialogs and components. Before choosing the design, MUST complete
[usage and state analysis](references/usage-and-states.md), including when a
shared default changes without an API change. A local edit limits the affected
footprint; it does not waive analysis of that footprint.

Visual acceptance uses the design system's
[standard phone viewport scope](../design-system/references/visual-consistency.md#default-viewport-scope).
Very small-device appearance is outside the default design/review scope.

## Ownership

| Owner | Responsibility |
|---|---|
| This skill | Visual intent, domain entity visual semantics, composition, usage/state analysis and brand acceptance |
| [design-system](../design-system/SKILL.md) | Tokens, shared component contracts, resources, themes and previews |
| [CONSISTENCY.md](../../../design-system/CONSISTENCY.md) | Concrete product role-to-token choices |
| [illustrations](../illustrations/SKILL.md) | Decorative artwork reference, generation, exports and acceptance |
| [add-equipment](../add-equipment/SKILL.md) | Equipment diagrams and catalog integration |
| [ui-feature](../ui-feature/SKILL.md) | Screen/dialog behavior, navigation and state ownership |
| [visual evidence](../design-system/references/visual-consistency.md#evidence) | What a render establishes and how visual evidence is reported |
| [task-orchestrator](../task-orchestrator/SKILL.md#execution-loop), [validation-gates](../validation-gates/SKILL.md) | Execution loop, independent review and completion checks |

The user's request and accepted decisions define scope and override visual
defaults. State deliberate departures that affect consistency in the brief.
Existing source establishes behavior and APIs; it does not make every legacy
visual choice a rule.
Resolve conflicts at the owning document rather than adding a competing policy.

## Required workflow

1. Establish the authorized outcome, existing working-tree changes and actual
   supplied design/state. Distinguish a local composition change from a shared
   component, token or artwork change.
2. Complete the usage/state analysis and
   [domain entity semantic recognition](references/design-language.md#semantic-recognition).
   Use the findings to choose the composition and the correct shared or local owner.
3. Keep a compact visual brief in the reply, the task report
   ([records](../../../CLAUDE.md#records)): purpose and next action, usage/state
   constraints, intended visual treatment, affected owners, accepted departures and
   planned evidence. A changed role-to-token choice goes to
   [CONSISTENCY.md](../../../design-system/CONSISTENCY.md); a dated visual-pass
   record under `docs/records/` only when the user asks. No separate report or plan
   approval is required.
4. Read the relevant visual references below and implement through design-system.
   Shared changes include necessary caller migrations; private one-off composition
   stays private. Add a component/style/token only for a demonstrated unmet role.
5. Apply [design acceptance](references/workflow-and-review.md#design-acceptance)
   to the final revision and the canonical execution/review loop. Recheck groups
   affected by corrections and report actual evidence and remaining limits.

## Read by task

| Task | Reference |
|---|---|
| New composition, redesign or brand review | [Design language](references/design-language.md), including [semantic recognition](references/design-language.md#semantic-recognition) |
| Screen, form, dashboard, list, empty state or dialog | Relevant sections of [surface composition](references/surface-composition.md) |
| Controls, feedback, themes or animation | Relevant sections of [states and motion](references/states-and-motion.md) |
| Implementation, migration or completion review | [Workflow and review](references/workflow-and-review.md) |

Standalone equipment diagrams follow [add-equipment](../add-equipment/SKILL.md);
decorative artwork follows [illustrations](../illustrations/SKILL.md). Apply this
skill when UI placement or composition is also in scope; a layout edit does not
require new artwork.

## Visual invariants

- Build hierarchy through grouping, space, type and neutral surfaces. Concentrate
  emphasis on the next action, meaningful selection/status or purposeful brand region.
- Choose containers through [shape and hierarchy](references/design-language.md#shape-and-hierarchy);
  avoid competing focal points.
- Preserve distinctions between selection, semantic feedback, illustration treatment
  and data-series color. Similar hues do not make those roles interchangeable.
- Preserve necessary content and usable interaction. Resolve layout pressure through
  composition and content-driven sizing, not hidden values or shrunken useful imagery.
- Decorative presentation follows the [motion contract](references/states-and-motion.md#motion).
- Visual work preserves feature behavior unless a behavior change is authorized.
  Assess [analytics](../feature-analytics/SKILL.md) and
  [state preservation](../state-preservation/SKILL.md) at their owning gates; a
  [visual-only change](../../../CLAUDE.md#definitions) states its impact assessment
  without inventing events or persisted state.

## Completion

MUST pass [design acceptance](references/workflow-and-review.md#design-acceptance)
and report which contexts/states were rendered or exercised as
[visual evidence](../design-system/references/visual-consistency.md#evidence)
defines. Compilation and token compliance do not establish visual fidelity. Builds
follow the default build rule in the
[verification policy](../../../CLAUDE.md#verification-policy); renders on a device
go through [runtime-verification](../runtime-verification/SKILL.md) when the user
allows running the app; an unavailable render is reported as an evidence limit.
