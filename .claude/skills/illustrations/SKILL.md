---
name: illustrations
description: Create, refine or review Grippo product illustrations in the approved warm orange and charcoal 3D style. Use for requested onboarding, empty-state, achievement and feature artwork; ordinary UI layout, icons and logos remain owned by the design system.
---

# Grippo illustrations

Equipment catalog diagrams follow
[add-equipment's ImageVector pipeline](../add-equipment/references/imagevector-pipeline.md).
The reference, generation and export rules below apply to decorative product artwork.

The owner approved the onboarding dumbbell illustration as the project's visual
reference. New illustrations should feel like members of this same family,
including when their subjects and screen roles differ.

Read [CLAUDE.md](../../../CLAUDE.md). MUST apply this skill when creating, refining
or reviewing product illustrations. Do not add artwork to an unrelated UI task.
Explicit user choices override style defaults; state a requested departure in the
reply rather than claiming it matches the approved family.

Before generating or reviewing style, MUST visually inspect the
[approved illustration](../../../design-system/resources/provider/src/commonMain/composeResources/drawable/onboarding_hero.webp).
Use that existing asset as the style-reference input of the image-generation tool
when one is available, not merely its filename or a remembered description. MUST
NOT duplicate it in this skill's assets.
If moved, locate its actual repository owner and repair this reference. If the
reference cannot be inspected, report style verification as unavailable; do not
invent its appearance or substitute a private generated-image path.

## Scope and reuse

Inspect existing provider artwork and its consumers before generating a duplicate.
Reuse an existing asset when its meaning and composition fit the request. Identify
whether the task requests a standalone asset, an edit, a set, or screen integration.
An asset-only request does not authorize a new screen or changes to navigation.
Keep generation intermediates outside the repository; integrate only selected
assets with real consumers, or deliver standalone outputs at the requested location.

## Visual identity

- Premium, stylized 3D product illustration: sculptural, rounded and tactile,
  with believable volume and restrained material detail. Match the reference's
  realism; avoid flat clipart, pixel art, cartoon outlines and unrelated styles.
- Matte midnight charcoal / graphite bodies with warm orange metallic accents,
  copper highlights and softly polished beveled edges. Keep the orange dominant
  among accents; use neutral surfaces to give it room. Do not introduce a rainbow
  palette, purple/blue neon or new brand colors by default.
- Soft studio lighting, gentle highlights, warm rim light and a subtle floating
  shadow. Glow is an accent, not a large opaque fog or a substitute for edges.
  Avoid blown-out highlights, harsh black shadows and uniformly glossy plastic.
- One clear focal subject, preferably in a diagonal or three-quarter view, with
  a few supporting shapes only when they help communicate the screen's meaning.
  An upward arc suits progress; it is not mandatory on every illustration.
- Centered, balanced silhouette, entire subject visible, useful clear margins
  and no tiny details needed to understand it at its actual UI size. Do not bake
  padding assumptions for one screen into every future asset.
- No embedded text, numbers, labels, watermark, logo or screenshot of UI by
  default. Product copy and branding stay in localized Compose content. Do not
  invent statistics, rewards or product capabilities through the artwork.

Keep the material language, lighting and color balance consistent across a set.
Vary the subject rather than repeating a dumbbell everywhere: for example, a
sculptural journal for history, a trophy for an achievement, or grouped equipment
for setup. Choose imagery that represents the actual feature, not a generic
fitness decoration attached to unrelated content.

## Generation

New raster artwork is produced with the image-generation tool available in the
session, if any; in Claude Code this may be the Figma MCP `generate_image` or none.
Generation runs in the main session (project agents have no MCP image tools); the
`design-implementer` integrates the exported asset.
When no tool is available, report generation as unavailable and deliver the
guidance and prompt below instead; do not fall back silently to a different image
service or model. Inspect the tool's actual reference and transparency inputs
before relying on them.

Supply the approved local image as the style reference through the tool's
supported reference mechanism. Distinguish the style reference from the edit
target. For a new subject, explicitly request that subject rather than preserving
the reference's dumbbell. For a refinement or variant, preserve the target's
subject and change only the requested properties. MUST inspect an edit target
before editing it.

Choose the screen role, subject, aspect ratio and intended display size before
generation. Default to a transparent square cutout for a single object, as in
onboarding. Wider compositions are appropriate when their real container needs
them; do not stretch a square image to fill a banner. For a series, reuse one
style prompt and reference while changing the subject-specific composition.

Suggested prompt core, adapted to the actual request:

> Use the attached Grippo artwork as the visual style reference. Create [subject]
> for [actual screen purpose], not a UI mockup. Premium sculptural 3D product
> illustration, matte midnight charcoal bodies, warm orange metallic accents,
> subtle copper highlights and beveled edges, soft studio light and gentle warm
> rim light. [Meaningful supporting element, if needed.] Balanced [aspect ratio]
> composition, clear silhouette, entire subject visible with generous margins.
> Transparent background and a soft floating shadow. No text, numbers, logos or
> watermark. Match the reference's materials and color balance, not its subject.

Cutouts MUST end up with real alpha: request transparency through whatever input
the tool offers, preserve the alpha through export and verify it in the converted
WebP. A checkerboard, white backdrop or painted black background is not
transparency. For an explicitly requested full-background scene, adapt the prompt
accordingly. Do not generate additional variants without a concrete quality
problem or user request.

## Resource and UI integration

For an actual UI composition or integration change, apply
[brand-design](../brand-design/SKILL.md) to choose artwork emphasis and placement
within the screen/dialog hierarchy. Standalone artwork delivery does not require
a screen redesign or full UI review; this skill remains the artwork style owner.

Apply [design-system](../design-system/SKILL.md) and its
[resource policy](../design-system/references/resources.md) for integration.
That skill owns paths, formats, accessors, tokens and previews; this skill owns
the illustration's visual identity. UI icons remain AppIcon vectors, not small
raster substitutes, and existing logos must not be regenerated incidentally.

For decorative UI artwork, MUST use the design-system
[Illustration](../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/illustration/Illustration.kt)
and its existing size styles instead of repeating Image sizing/scale logic.
For an illustration with an empty-state message, use
[IllustratedEmptyState](../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/empty/IllustratedEmptyState.kt).
Screen-specific backgrounds and actions remain with their consumers; do not add
an artwork-specific widget or style when an existing one fits.

For UI integration, export the chosen asset to the shared Compose resource provider
as WebP by default, preserving alpha for cutouts. Use descriptive snake_case names
under the resource policy. PNG is suitable as a lossless generation master or
when required by tooling, with the reason
stated; do not bundle duplicate PNG/WebP masters without a real consumer.
Choose pixel dimensions and compression for the largest actual display size;
the onboarding reference is 768 × 768, 104,178 bytes, with alpha, an example rather
than a universal budget. Inspect quality after conversion, not just the generator output.

Keep decorative art separate from the container background. Compose owns soft
theme-aware gradients and localized text; use existing AppTokens or add necessary
semantic slots in their real producers for both themes. Preserve aspect ratio,
avoid cropping the subject and keep illustrations from consuming space needed
for controls at the [standard phone viewport](../design-system/references/visual-consistency.md#default-viewport-scope).
Decorative images use null contentDescription;
meaningful information needs a localized accessible equivalent.

## Completion

MUST inspect the exported image at the intended display size on light/dark backgrounds:
alpha, silhouette, edge/glow artifacts, material/style match and subject clarity.
For UI integration, review the whole-screen consumer for spacing and CTA access at
the [standard viewport](../design-system/references/visual-consistency.md#default-viewport-scope)
through its two AppPreview variants or, when the user allows running the app,
[runtime-verification](../runtime-verification/SKILL.md). Fix observed problems
rather than adding more ornament. Report evidence as
[visual evidence](../design-system/references/visual-consistency.md#evidence)
defines; an artwork preview or successful build is not a verified screenshot of
the application.

Completion MUST identify the selected asset, dimensions, format, alpha where
required, actual inspection results and unresolved defects. Style mismatch,
unintended cropping, missing required alpha and broken consumer/resource references
require correction before claiming those outcomes complete. Unavailable rendering
remains explicitly unverified; generation itself does not guarantee style fidelity.
For standalone assets, use theme-like backgrounds; do not claim application checks
when no consumer was requested.

A [visual-only change](../../../CLAUDE.md#definitions) states its analytics/state
impact assessment without adding events or persistence. If controls, navigation or
behavior change, apply [analytics](../feature-analytics/SKILL.md) and
[state preservation](../state-preservation/SKILL.md) with their owning gates.
Builds for resource/UI edits follow the default build rule in the
[verification policy](../../../CLAUDE.md#verification-policy); skill-only
documentation runs the rulebook checker instead. Preserve unrelated assets; an
explicitly requested edit authorizes changing its target, not other artwork. No
deployment, new dependency or external publication is implied by this skill.
