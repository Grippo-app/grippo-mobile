# Preview coverage

Use the existing [AppPreview and PreviewContainer](../../../../design-system/preview/src/commonMain/kotlin/com/grippo/design.preview/Preview.kt).
Inspect their current configuration/signature; do not copy providers or create
preview infrastructure without a concrete authorized need.

## Coverage gate

- New screens/widgets and visual changes MUST have representative private named
  AppPreview coverage in their files, inside PreviewContainer. Update fitting
  existing previews rather than duplicate them.
- Default previews and visual acceptance follow the
  [standard viewport scope](visual-consistency.md#default-viewport-scope).
  Do not create a small-device or font-scale matrix unless the user requests it.
- Nonvisual edits require an impact assessment and relevant caller/preview review.
  Callback-only or Kotlin visibility changes need no new fixture when rendering
  and interaction are unaffected. Changing whether UI content is shown is visual.
- Derive coverage from the required
  [usage/state inventory](../../brand-design/references/usage-and-states.md).
  Account for every distinct role/host, visual variant, applicable state, meaningful
  reachable combination and content boundary. Inspected equivalent cases may share
  fixtures; impossible state combinations do not improve coverage.
- Screens/dialogs use Contract.Empty. Widgets use lightweight parameters and
  existing ui-core stubs; private screen fixtures reuse shared stubs and override
  only distinguishing facts. No live VM/Koin is needed. Keep compared IDs, dates
  and counts deterministic. Explicit sample data/text MAY be literal; this does
  not authorize alternative production styling.

## What a preview establishes

Trace theme/locale through the [effective provider](theme.md) and the actual render;
annotation names or a locale argument alone do not prove the rendered language.
Current PreviewContainer adds preview padding and replaces Coil images with a
placeholder. Its framing is not production host geometry, and that placeholder
does not verify the real image's silhouette, proportions or loading outcome.

Inspect whole-screen consumers and actual loaded/exported imagery when those
outcomes matter. Static previews establish only the rendered state; focus,
keyboard, scrolling, gestures and transition timing need relevant manual evidence.
Use matching supplied design content for fidelity comparisons.

Record fixture-to-context/state coverage and unrendered or unexercised groups.
[Visual consistency](visual-consistency.md) owns whole-screen checks;
[visual review](../../validation-gates/references/visual-review.md) owns evidence
handling. Automated checks and verification facilities follow the
[project policy](../../../../CLAUDE.md#verification-policy).
