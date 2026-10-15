---
name: brand-design
description: Design and visually review Grippo screens, dialogs and components in its warm orange, graphite and sculptural brand language. Owns composition, usage/state analysis and visual acceptance. Use for new UI and visual changes; behavior-only changes and standalone artwork use their own skills.
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
| This skill | Visual intent, composition, usage/state analysis and brand acceptance |
| [design-system](../design-system/SKILL.md) | Tokens, shared component contracts, resources, themes and previews |
| [CONSISTENCY.md](../../../design-system/CONSISTENCY.md) | Concrete product role-to-token choices |
| [illustrations](../illustrations/SKILL.md) | Artwork reference, generation, exports and asset acceptance |
| [ui-feature](../ui-feature/SKILL.md) | Screen/dialog behavior, navigation and state ownership |
| [task-orchestrator](../task-orchestrator/SKILL.md#execution-loop), [validation-gates](../validation-gates/SKILL.md) | Execution, independent review and verification policy |

The user's request and accepted decisions define scope and override visual
defaults. Record deliberate departures that affect consistency. Existing source
establishes behavior and APIs; it does not make every legacy visual choice a rule.
Resolve conflicts at the owning document rather than adding a competing policy.

## Required workflow

1. Establish the authorized outcome, existing working-tree changes and actual
   supplied design/state. Distinguish a local composition change from a shared
   component, token or artwork change.
2. Complete the usage/state analysis. Use its findings to choose the composition
   and the correct shared or local owner.
3. Keep a compact brief in the current task record: purpose and next action,
   usage/state constraints, intended visual treatment, affected owners, accepted
   departures and planned evidence. No separate report or plan approval is required.
4. Read the relevant visual references below and implement through design-system.
   Shared changes include necessary caller migrations; private one-off composition
   stays private. Add a component/style/token only for a demonstrated unmet role.
5. Apply [design acceptance](references/workflow-and-review.md#design-acceptance)
   to the final revision and the canonical execution/review loop. Recheck groups
   affected by corrections and report actual evidence and remaining limits.

## Read by task

| Task | Reference |
|---|---|
| New composition, redesign or brand review | [Design language](references/design-language.md) |
| Screen, form, dashboard, list, empty state or dialog | Relevant sections of [surface composition](references/surface-composition.md) |
| Controls, feedback, themes or animation | Relevant sections of [states and motion](references/states-and-motion.md) |
| Implementation, migration or completion review | [Workflow and review](references/workflow-and-review.md) |

Standalone artwork stays with illustrations. Apply this skill when its UI placement
or composition is also in scope; a layout edit does not require new artwork.

## Visual invariants

- Build hierarchy through grouping, space, type and neutral surfaces. Concentrate
  emphasis on the next action, meaningful selection/status or purposeful brand region.
- Keep enclosing surfaces, controls and image slots related by role rather than
  forcing identical geometry. Avoid redundant frames and competing focal points.
- Preserve distinctions between selection, semantic feedback, illustration treatment
  and data-series color. Similar hues do not make those roles interchangeable.
- Preserve necessary content and usable interaction. Resolve layout pressure through
  composition and content-driven sizing, not hidden values or shrunken useful imagery.
- Decoration MUST NOT gate readiness, input, loading or navigation. Motion uses
  existing presentation owners, without business timers or extra requests.
- Visual work preserves feature behavior unless a behavior change is authorized.
  Assess [analytics](../feature-analytics/SKILL.md) and
  [state preservation](../state-preservation/SKILL.md) at their owning gates;
  visual-only changes record impact without inventing events or persisted state.

## Completion

MUST pass the design acceptance source gate and record which contexts/states were
rendered or exercised. Compilation and token compliance do not establish visual
fidelity. Unavailable rendering remains an evidence limit under the canonical
[verification policy](../../../CLAUDE.md#verification-policy).
