# Implementation and design review

Start from the required [usage/state inventory](usage-and-states.md) and compact
brief in [SKILL.md](../SKILL.md#required-workflow). This reference owns design
acceptance; the canonical
[execution loop](../../task-orchestrator/SKILL.md#execution-loop) owns delegation,
independent review, findings and correction rounds.

## Choose the correction owner

| Problem | Correction |
|---|---|
| Shared default fails across consumers | Fix the component/token owner using the complete usage inventory |
| One screen groups content poorly | Change local composition with current shared pieces |
| A distinct recurring role needs another treatment | Reuse an explicit style; extend it only for a demonstrated unmet role |
| One-off introduction | Reuse a suitable header/artwork or keep composition private |
| Wrong artwork subject/style/export | Apply illustrations at the asset owner |
| Semantic status/data color conflicts with branding | Preserve meaning; correct neutral framing/hierarchy |
| Border/spacing anomaly in a capture | Confirm scale, actual state and drawing/layout owner before editing |

A new wrapper needs a cohesive repeated responsibility and real consumers. Merely
renaming a component or reducing a few modifiers does not justify another layer.

## Implement a coherent change

- For new UI, reuse source-supported contracts, widgets, tokens, copy and artwork.
  Compose title, working content, next action and navigation as one hierarchy.
- For a refresh, identify the concrete mismatch before editing. Remove redundant
  emphasis/framing before introducing an abstraction. Compare before/after using
  matching content and states when available.
- For shared changes, preserve contracts when their role stays the same. Necessary
  API migrations include all affected callers through design-system. Update all
  existing theme producers for a changed color role.
- Inspect enclosing compositions for every distinct inventoried context; isolated
  component geometry may conflict with headers, adjacent cards or pinned actions.
- Remove obsolete in-scope styles/resources only after checking all uses, providers
  and previews. Check removed names/imports in the final diff. A zero-result search
  establishes absence of those references, not visual quality.

Behavior, analytics and preservation remain with their owning skills as routed by
SKILL.md. Visual work does not create an additional VM, routing or persistence task.

## Design acceptance

MUST review each applicable axis against the final source and available observations.
Usage/state coverage and evidence accounting are mandatory for every visual task;
mark other axes inapplicable only when the affected source supports that conclusion.

| Axis | Acceptance |
|---|---|
| Usage/states | The [source gate](usage-and-states.md#source-gate) is complete; every distinct role, state, reachable combination and transition is accounted for |
| Composition | Purpose and next action are clear; headings, sections, cards, metrics and metadata have distinct hierarchy without redundant frames |
| Brand/semantics | Neutral structure and deliberate warmth fit the role; selection, feedback and data colors preserve meaning; artwork earns its space |
| Content | Actual copy, values, units, images and supported locales remain readable and complete |
| Interaction | Existing semantics, focus/keyboard, selection, scroll/insets and action reachability survive the change |
| Themes | Changed text, surfaces, outlines, gradients and artwork have appropriate relationships in both existing themes |
| Motion | Presentation explains or decorates existing changes without gating input, loading or navigation |
| Ownership | Components/tokens/resources have one owner, compatible consumers and no unused additions or behavioral scope expansion |
| Evidence | Source, renders, runtime and compilation are distinguished for the final revision; missing observations and unresolved decisions are explicit |

MUST correct source-proven ownership, semantic or behavior defects even when renders
are unavailable. Missing rendering alone does not establish a cosmetic defect or
prove its absence; apply the canonical verification policy for completion limits.

## Review handoff and evidence

Provide the request, brief, changed files, usage/state inventory, accepted departures
and actual checks, including unrendered groups. Findings identify the role/state,
trigger, consequence, file/line and smallest correct owner. Distinguish source-proven
defects, observed render issues and proposals needing visual confirmation; taste
alone is not a defect. Correct findings and recheck affected groups through the
canonical execution loop.

Use [visual review](../../validation-gates/references/visual-review.md) for evidence
handling and [CLAUDE.md](../../../../CLAUDE.md#verification-policy) for permitted
checks. Report the visual outcome, affected consumers and useful remaining limits.
Static renders do not verify live focus, scrolling, gestures or motion.

For skill-only edits, review metadata, links, ownership, routing and realistic use
cases. No application build or app-side fixture is required for documentation.
