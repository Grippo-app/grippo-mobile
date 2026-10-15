# Usage and state analysis

MUST complete this analysis before creating, visually changing or visually
reviewing a screen, dialog or component. It also applies to shared visual defaults
and tokens with unchanged public signatures. Keep the result in the current task
record; no separate registry, report file or design model is required.

## 1. Trace all affected uses

- Search declarations, calls, imports/aliases, overloads, styles, forwarded slots
  and affected token/resource accessors. Read actual arguments, conditions and
  modifiers; search hits alone are not an inspected inventory.
- For a shared change, MUST read all affected production call sites, including
  inherited defaults. Follow wrappers and nested components to their enclosing
  screen/dialog and host. Inspect previews separately from production callers.
- For an existing screen/dialog, trace entry points, route/configuration inputs
  and parent hosts. Identify tabs, flow steps or sheet contexts that alter initial
  content, composition or actions. Read affected children and shared owners.
- For new UI, identify concrete intended consumers and the authorized integration
  before choosing a contract/layout. Inspect implemented callers afterward. Do not
  invent future uses to justify additional variants or a generic abstraction.
- For each use, identify purpose, action/selection/status role, density, neighboring
  hierarchy, background and geometry. Inspect actual bounds, optional slots,
  scrolling, toolbar/bottom-bar ownership, pinned actions, keyboard and insets
  where relevant.

Equivalent occurrences MAY be grouped only after their actual arguments, role,
state and host constraints have been inspected. Keep covered source paths
identifiable. Inspect every unique context; an attractive representative does not
establish equivalence. Unrelated uses of an unchanged owner remain outside a local
edit's footprint. Neighboring screens provide context, not automatic redesign scope.

## 2. Derive all applicable states

Read component parameters/styles and internal branches, screen State/Loader/Contract,
VM/state producers and the presentation owners of interaction state. Defaults and
preview stubs alone do not define the complete state model. MUST account for every
supported state in the affected contexts, using the following discovery axes only
where source supports them:

| Axis | Inspect |
|---|---|
| Content/availability | Initial/loading/refreshing, empty versus filtered/no results, partial/populated, failure/retry, blocked prerequisites, completion; actual inline/dialog feedback ownership |
| Interaction | Idle/pressed/focused, selected/unselected, enabled/disabled, editable/read-only, valid/invalid, expanded/collapsed, operation loading |
| Content shape | Long/short localized copy, optional text/icons/actions, missing images/values, zero/large values, counts/units and image proportions |
| Environment/host | Existing themes and locales at the [standard viewport](../../design-system/references/visual-consistency.md#default-viewport-scope), screen/sheet/nested surface, keyboard, insets, scrolling and platform layout differences; extra device/font scales only when requested |
| Transition | First frame, state/content changes, focus/keyboard, loading-to-result/error, retry and return/restoration where supported |

MUST inspect reachable combinations and visual precedence: for example, focused +
invalid or selected + disabled when their owners allow both. Establish which
visual treatment wins and what remains readable and actionable. Mark unsupported
axes/combinations inapplicable from source evidence; do not require every Cartesian
combination. Preserve required transition geometry, focus, scroll and action access.
Styling does not add validation, state flags, loading phases or restoration behavior.

## 3. Use the findings to choose and verify the design

For each distinct usage/state group, record source paths/host, role and constraints,
applicable states/combinations, the resulting design choice and planned/actual
source, preview or manual evidence. A few lines suffice for a small edit.

Choose hierarchy, spacing, shape, emphasis, imagery and arrangement from those
constraints, including the enclosing header/cards/actions/navigation and dense
repeated use. Improve a shared default only if it fits all affected roles; use an
existing explicit style or local composition for a contextual difference. A new
variant needs a demonstrated unmet role and concrete consumers. Per-screen
overrides must not conceal a shared default that breaks other callers.

MUST review source for every inventoried use/state and plan permitted visual checks
for every materially different context, visual state or reachable combination.
Equivalent cases may share fixtures; a screenshot of every occurrence is not
required. [Preview rules](../../design-system/references/previews.md) own fixtures;
[visual consistency](../../design-system/references/visual-consistency.md) owns
whole-screen, theme and text-layout checks.

## Source gate

Reconcile final consumers and state branches against the inventory. An uninspected
discovered consumer, unique context or applicable state leaves this gate incomplete.
After corrections, recheck affected groups. Unavailable renders are recorded evidence
limits under the [verification policy](../../../../CLAUDE.md#verification-policy),
not omitted states or proof of visual success.
