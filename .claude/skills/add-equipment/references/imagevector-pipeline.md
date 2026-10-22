# Equipment ImageVector pipeline

This reference owns the catalog equipment diagrams' visual family and authoring
pipeline. They are native Compose vectors under the
[equipment provider](../../../../design-system/resources/provider/src/commonMain/kotlin/com/grippo/design.resources/provider/equipment/).
Decorative onboarding, empty-state and achievement artwork follows
[illustrations](../../illustrations/SKILL.md).

## Choose and inspect the reference

1. Open the closest existing vector's Kotlin source and render its actual paths.
   Inspect that render before editing. Compare a second sibling when needed to
   distinguish a shared family trait from one machine's construction.
2. Identify what makes the requested machine recognizable: resistance mechanism,
   handles or contact pads, seat/backrest, supporting frame and operating position.
   For example, a forearm pec deck needs two substantial upright contact pads
   attached to its moving arms, rather than long thin handgrips alone.
3. Inspect the consuming card's aspect ratio and size. Match the equipment set's
   isometric perspective, light metal frame, dark upholstery and restrained facet
   shading using its real palette producer. Preserve useful negative space and the
   full silhouette. Source viewports vary; derive dimensions from the subject and
   its neighboring assets rather than forcing every machine into a square.

For a current variant example, inspect
[Butterfly](../../../../design-system/resources/provider/src/commonMain/kotlin/com/grippo/design.resources/provider/equipment/Butterfly.kt)
and [ButterflyForearm](../../../../design-system/resources/provider/src/commonMain/kotlin/com/grippo/design.resources/provider/equipment/ButterflyForearm.kt).
They use a 191 × 321 viewport and
[AppPalette.NeutralDark](../../../../design-system/resources/provider/src/commonMain/kotlin/com/grippo/design.resources/provider/colors/AppPalette.kt)
for the metal and upholstery faces. These are reference facts, not dimensions
required for unrelated equipment.

## Produce native geometry

For a variant, retain the closest vector's unchanged frame, seat and resistance
geometry and replace the operating parts that differ. For a new machine, draw
real path geometry using an appropriate neighboring diagram as the visual guide.
Build structural parts from back to front, then add contact surfaces, facets and
small functional details in a consistent depth order. Verify that moving arms,
handles, pads and supports connect mechanically and that paired parts share a
coherent perspective.

Use a cached `ImageVector` declaration and actual `AppPalette` brushes. Choose
the file, public property, builder and cache identifiers under
[equipment identifiers](naming.md); artwork-only work uses its vector naming rules.
Apply [code style](../../validation-gates/references/code-style.md) to new Kotlin
path data. Keep backgrounds, labels, people and logos outside the equipment
diagram; transparency comes from the unpainted vector canvas.

If a concept benefits from image generation, the main session can use
an available image tool to create a visual reference after inspecting its actual
reference-input capabilities. Supply the rendered equipment sibling as the style
reference and state the mechanical changes, for example:

> Match this equipment diagram's isometric angle, light metal frame, dark padded
> surfaces and simple facet shading. Show the requested machine with its complete
> frame and resistance mechanism. Make the specified operating parts clear and
> mechanically connected. Use a transparent background and omit people and text.

The generated bitmap is a concept reference. Author and inspect the final native
paths before connecting them to `EquipmentEnumState.icon`. Renaming a PNG to SVG,
embedding a bitmap in SVG or delivering a raster alone does not produce the
`ImageVector` consumed by the current equipment cards. Preserve supplied native
geometry when it already represents the requested machine.

## Export and inspect the final paths

Keep generation and inspection intermediates outside the repository under the
[change-attribution scratchpad convention](../../task-orchestrator/references/change-attribution.md).
For an isolated preview, export the actual Kotlin paths to SVG preserving every
command, curve, fill, stroke, alpha, fill rule, palette color, viewport, group
transforms, clipping and draw order. Resolve named coordinate arguments correctly;
use native rendering when an exporter cannot represent a source feature faithfully.
Rasterize that SVG with an existing
available renderer; discover bundled dependencies through the current client's
workspace dependency helper when available. This task does not require new
repository rendering scripts or verification infrastructure.

Inspect the final export on light and dark backgrounds at the actual consumer
sizes using the source viewport and its fit behavior. Keep inspection margins
out of these size checks. Inspect any consumer color transformation separately;
the current Compact card can soften highlights in dark theme. Check full silhouette, unclipped
extents, distinct contact surfaces, visible connections, facet consistency,
transparency and the difference from neighboring equipment. Fix observed defects
and regenerate the affected previews from the final paths.

For integration, inspect the actual `EquipmentCard` variants and selection host
through [design-system](../../design-system/SKILL.md) and
[brand-design](../../brand-design/SKILL.md). Use existing preview facilities and
authorized [runtime verification](../../runtime-verification/SKILL.md) under the
project's [verification policy](../../../../CLAUDE.md#verification-policy).
An isolated SVG/PNG export establishes the diagram's appearance; it does not
establish Compose rendering, selection behavior or application layout.

## Vector completion

- Final source is a real cached `ImageVector` at the provider owner, with a
  complete recognizable mechanism and no unintended background or clipping.
- The asset identifiers pass the applicable naming rules and collision check.
- Rendered paths match the final source and the chosen equipment visual family
  on both background types and at the inspected sizes.
- The report identifies the asset, viewport, renders inspected and remaining
  application evidence limits under
  [visual evidence](../../design-system/references/visual-consistency.md#evidence).
- A full equipment request continues through the
  [integration map](integration-map.md); an artwork-only request ends at the
  explicitly requested asset outcome.
