---
name: design-system
description: Create and change Grippo AppTokens, shared Compose components, localized resources, icons, fonts and previews using domain entity visual semantics. Map actual usage contexts and states before visual changes; inspect affected consumers and token producers before changing contracts.
---

# Grippo design system

Read [CLAUDE.md](../../../CLAUDE.md), the [reference index](references/index.md)
and affected producers, components and callers. Current source owns signatures,
packages, locales and provider wiring.

## Scope and ownership

For new or visually changed product UI and visual reviews, MUST apply
[brand-design](../brand-design/SKILL.md), including its required
[usage/state analysis](../brand-design/references/usage-and-states.md).
This skill owns implementation; [CONSISTENCY.md](../../../design-system/CONSISTENCY.md)
owns concrete role-to-token choices. Changes without visual impact require only
an impact assessment and relevant source/caller review.

Domain entity visual semantics follow brand-design's
[semantic recognition](../brand-design/references/design-language.md#semantic-recognition).

Compose implementation and source review apply
[compose-components](../compose-components/SKILL.md).

Product artwork creation, refinement and style review MUST apply
[illustrations](../illustrations/SKILL.md). Resource/UI integration remains here;
ordinary UI icons and existing logos do not trigger artwork generation.

## Implementation rules

- Product visual values in consumers MUST come from
  `AppTokens.colors|dp|typography|icons|strings|drawables`. Concrete producers own
  literals; neutral technical values such as `0.dp` and `Color.Transparent` are
  permitted. [Tokens](references/tokens.md) own facade/category boundaries.
- Add only demanded slots/components/styles at their real owners. A changed color
  contract MUST update every concrete theme. Do not create unused scaffold or a
  parallel palette/provider.
- Shared widgets accept neutral callbacks and remain domain-independent unless
  their existing contract deliberately accepts reusable UI state. Feature business
  logic, implementation dependencies and analytics types stay outside widgets.
- UI-model presentation mappings follow
  [UI-state ownership](../ui-feature/references/state.md#state-owned-presentation-mappings).
- Changed public contracts MUST migrate all necessary callers together. Remove
  obsolete in-scope overloads/aliases rather than retaining a broken migration.
  [Component rules](references/components.md) own contract and placement checks.
- Resources MUST pass [resource policy](references/resources.md): complete existing
  locales/placeholders, real AppIcon vector data, correct raster formats/names and
  matching font files/weights. Consumers use the real
  [accessors](references/accessors.md), including StringProvider outside composition.
- New UI and visual changes MUST pass the [preview coverage gate](references/previews.md)
  at the [standard viewport scope](references/visual-consistency.md#default-viewport-scope);
  do not add small-device acceptance work. Nonvisual changes do not mechanically
  require new fixtures.
- Whole-screen checks, supplied-design comparison and all visual evidence follow
  [visual checks and evidence](references/visual-consistency.md#evidence).
- Behavior changes apply [feature analytics](../feature-analytics/SKILL.md) and
  [state preservation](../state-preservation/SKILL.md); a
  [visual-only change](../../../CLAUDE.md#definitions) states its impact assessment
  in the reply.

## Completion gate

MUST reconcile final consumers, variants and state branches against the usage/state
inventory and pass each affected token, component, resource and preview source gate.
For visual changes, apply brand-design's
[semantic recognition acceptance](../brand-design/references/workflow-and-review.md#design-acceptance).
Inspect enclosing compositions and downstream input/focus behavior where affected.
Fix findings through the [execution loop](../task-orchestrator/SKILL.md#execution-loop)
and report evidence as [visual evidence](references/visual-consistency.md#evidence)
defines: checks run, unrendered groups and runtime/platform limits.

Scope-sensitive API/theme/module changes follow
[authorized scope](../../../CLAUDE.md#scope-discipline). Builds follow the default
build rule in the [verification policy](../../../CLAUDE.md#verification-policy);
renders on a device go through [runtime-verification](../runtime-verification/SKILL.md).
Compilation proves no visual or runtime outcome.
