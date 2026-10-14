---
name: design-system
description: Create and change Grippo AppTokens, shared Compose components, localized resources, icons, fonts and previews. Review affected consumers and current token producers before changing their contracts.
---

# Grippo design system

Read `CLAUDE.md` and [the reference index](references/index.md); read the relevant
reference and actual producer/component/callers before editing. Use the user's
scope, existing packages, resource locales and font factory from source.

For product artwork creation, refinement or review, MUST apply
[illustrations](../illustrations/SKILL.md)
for the approved Grippo illustration style. This skill still owns integration,
resource formats, tokens and previews; ordinary UI icons/logos do not trigger it.

## Required rules

- Consumers use `AppTokens.colors|dp|typography|icons|strings|drawables`.
  Concrete color, dimension, typography, font and icon producers own their literals.
  MUST NOT introduce raw product visual values in consumers. Neutral technical
  constants such as 0.dp and Color.Transparent are permitted; they do not authorize
  hardcoded product dimensions or palettes.
- Add only demanded token slots. Update every concrete theme implementation on disk.
  Do not invent a theme implementation or token category for a hypothetical consumer.
- UI icons are real `ImageVector` extensions on `AppIcon`, with nonempty path data.
  Raster imagery uses WebP by default; PNG requires a documented lossless/tooling reason.
  No vector drawable substitutes or `ic_*`/`img_*`/`bg_*` naming prefixes.
- User-facing strings and plurals MUST cover every existing locale with matching
  positional placeholders. Explicit preview fixtures MAY use literal sample text.
  Use the actual resource/provider APIs. `AppString` currently has no plural accessor:
  add a real wrapper only when a feature needs it; do not claim it already exists.
  Font files and their weight registration must agree.
- Shared widgets remain domain-independent unless their existing contract intentionally
  accepts a reusable UI state. Neutral callbacks import no analytics types.
- Migrate all necessary callers atomically for a changed contract. Remove obsolete
  overloads and aliases within scope; leave no broken caller or deprecated escape path.
- New widgets and visual changes MUST have representative private `@AppPreview`
  coverage inside `PreviewContainer`; screens use their own `Contract.Empty`.
  Other edits require a visual-impact assessment and review of relevant existing
  previews/callers, not mechanically added fixtures. [Preview rules](references/previews.md)
  own the detailed coverage gate.
- With a supplied design, inspect the actual Figma node or supplied design artifact.
  Compare actual content, hierarchy, themes and enlarged-font behavior in whole-screen
  consumers when rendering is permitted. Source compliance does not prove visual fidelity.
- Behavior changes also apply [feature analytics](../feature-analytics/SKILL.md) and
  [state preservation](../state-preservation/SKILL.md). Visual-only changes record impact.

## Scope decisions

Honor the user's existing authorization. Ask a simple quiz only for an unresolved
product/design decision or expansion beyond that scope: new token category,
new typeface, public contract removal, theme/CompositionLocal wiring or module changes.
MUST NOT ask again for necessary steps already covered by explicit or implied
authorization.

## Completion gate

Review actual changed source and every necessary caller for token ownership,
resource parity, icon data, stability, naming, previews and module boundaries.
Review design fidelity and downstream input/focus behavior where affected.
Fix findings, inspect the resulting diff again and record evidence and unverified
states. MUST follow CLAUDE.md Verification policy, including the explicit-user-request
exception for automated tests. Add no verification-only production hooks. Report changes and limits.
