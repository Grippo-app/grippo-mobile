---
name: design-system
description: Create and change Grippo AppTokens, shared Compose components, localized resources, icons, fonts and previews. Review affected consumers and current token producers before changing their contracts.
---

# Grippo design system

Read `CLAUDE.md` and [the reference index](references/index.md); read the relevant
reference and actual producer/component/callers before editing. Use the user's
scope, existing packages, resource locales and font factory from source.

## Required rules

- Consumers use `AppTokens.colors|dp|typography|icons|strings|drawables`.
  Concrete color, dimension, typography, font and icon producers own their literals.
  Do not introduce raw visual values in consumers; existing neutral Compose constants
  such as Color.Transparent do not authorize new product colors.
- Add only demanded token slots. Update every concrete theme implementation on disk.
  Do not invent a theme implementation or token category for a hypothetical consumer.
- UI icons are real `ImageVector` extensions on `AppIcon`, with nonempty path data.
  Raster imagery uses WebP by default; PNG requires a documented lossless/tooling reason.
  No vector drawable substitutes or `ic_*`/`img_*`/`bg_*` naming prefixes.
- Strings and plurals cover every existing locale with matching positional placeholders.
  Use the actual resource/provider APIs. `AppString` currently has no plural accessor:
  add a real wrapper only when a feature needs it; do not claim it already exists.
  Font files and their weight registration must agree.
- Shared widgets remain domain-independent unless their existing contract intentionally
  accepts a reusable UI state. Neutral callbacks import no analytics types.
- Migrate all necessary callers atomically for a changed contract. Remove obsolete
  overloads and aliases within scope; leave no broken caller or deprecated escape path.
- Each changed/new widget has a private `@AppPreview` inside `PreviewContainer`.
  Use representative states; screens use their own `Contract.Empty`.
- With a supplied design, inspect the actual Figma node or supplied design artifact.
  Compare actual content, hierarchy, themes and enlarged-font behavior in whole-screen
  consumers when rendering is permitted. Source compliance does not prove visual fidelity.
- Behavior changes also apply [feature analytics](../feature-analytics/SKILL.md) and
  [state preservation](../state-preservation/SKILL.md). Visual-only changes record impact.

## Scope decisions

Honor the user's existing authorization. Ask a simple quiz only for an unresolved
product/design decision or expansion beyond that scope: new token category,
new typeface, public contract removal, theme/CompositionLocal wiring or module changes.
Do not ask again for work already explicitly authorized.

## Completion gate

Review actual changed source and every necessary caller for token ownership,
resource parity, icon data, stability, naming, previews and module boundaries.
Review design fidelity and downstream input/focus behavior where affected.
Fix findings, inspect the resulting diff again and record evidence and unverified
states. MUST follow CLAUDE.md Verification policy, including the explicit-user-request
exception for automated tests. Add no verification-only production hooks. Report changes and limits.
