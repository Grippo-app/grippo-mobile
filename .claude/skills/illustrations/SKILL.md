---
name: illustrations
description: Create, refine, integrate or review Grippo product illustrations in the approved warm orange and charcoal 3D style. Use for requested onboarding, empty-state, achievement and feature artwork; ordinary UI layout, icons and logos remain owned by the design system.
---

# Grippo illustrations

Equipment catalog diagrams follow
[add-equipment's ImageVector pipeline](../add-equipment/references/imagevector-pipeline.md).
The reference, generation and export recipes linked below apply to decorative product artwork.

The owner approved the onboarding dumbbell illustration as the project's visual
reference. New illustrations should feel like members of this same family,
including when their subjects and screen roles differ.

Read [CLAUDE.md](../../../CLAUDE.md). MUST apply this skill when creating, refining
or reviewing product illustrations. Do not add artwork to an unrelated UI task.
Explicit user choices override style defaults; state a requested departure in the
reply rather than claiming it matches the approved family.

Before generating or reviewing style, MUST visually inspect the
[approved illustration](../../../design-system/resources/provider/src/commonMain/composeResources/drawable/onboarding_hero.webp).
For generation, use that existing asset as the image tool's style-reference input
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

## Workflow

Choose the requested activity and read its branch before acting:

| Activity | Read and do |
|---|---|
| Review existing artwork | Inspect the approved reference and the target; apply this skill's visual identity and completion gate. |
| Generate, refine or vary an asset | Read [generation](references/generation.md), then apply the completion gate to the exported result. |
| Integrate artwork or review its UI consumer | Read [resource and UI integration](references/integration.md), then apply the whole-screen checks in the completion gate. |

A task may combine branches. Standalone asset creation uses the generation recipe
and this root's completion gate. Load integration for Compose resource registration
or in-app consumer work; read generation only when creating or editing artwork.

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
