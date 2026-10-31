# Preview coverage

Use the existing [AppPreview and PreviewContainer](../../../../design-system/preview/src/commonMain/kotlin/com/grippo/design.preview/Preview.kt).
Inspect their current configuration/signature; do not copy providers or create
preview infrastructure without a concrete authorized need.

## Coverage gate

- New screens/widgets and visual changes MUST have private named AppPreview
  coverage of their [significant states](../../../../CLAUDE.md#definitions) in
  their files, inside PreviewContainer. Update fitting existing previews rather
  than duplicate them.
- Default previews and visual acceptance follow the
  [standard viewport scope](visual-consistency.md#default-viewport-scope): the two
  AppPreview variants (uk-light, en-dark) plus ru source review. Do not create a
  locale, small-device or font-scale matrix unless the user requests it.
- Nonvisual edits require a visual impact assessment (apply the
  [visual change](../../../../CLAUDE.md#definitions) definition) and relevant
  caller/preview review. Callback-only or Kotlin visibility changes need no new
  fixture when rendering and interaction are unaffected. Changing whether UI
  content is shown is visual.
- Derive coverage from the required
  [usage/state inventory](../../brand-design/references/usage-and-states.md):
  one fixture per distinct role/host and visual variant, covering its significant
  states and content boundaries. Call sites grouped by the inventory share
  fixtures; impossible state combinations do not improve coverage.
- Screens/dialogs use Contract.Empty. Widgets use lightweight parameters and
  existing ui-core stubs; private screen fixtures reuse shared stubs and override
  only distinguishing facts. No live VM/Koin is needed. Keep compared IDs, dates
  and counts deterministic. Explicit sample data/text MAY be literal; this does
  not authorize alternative production styling.

## What a preview establishes

Use the [default viewport variants](visual-consistency.md#default-viewport-scope)
for configured locale/theme settings and [theme ownership](theme.md) for provider
semantics; a preview's display name proves nothing. Current PreviewContainer adds
12 dp preview padding and replaces
Coil images with a placeholder. Its framing is not production host geometry, and
that placeholder does not verify the real image's silhouette, proportions or
loading outcome.

Inspect whole-screen consumers and actual loaded/exported imagery when those
outcomes matter. Static previews establish only the rendered state; focus,
keyboard, scrolling, gestures and transition timing need runtime evidence.
Use matching supplied design content for fidelity comparisons.

[Visual evidence](visual-consistency.md#evidence) owns what a render establishes,
fixture-to-state accounting and the reporting of unrendered or unexercised groups.
