# Illustration resource and UI integration

Apply the [illustration identity and acceptance gate](../SKILL.md) alongside
this recipe.

For an actual UI composition or integration change, apply
[brand-design](../../brand-design/SKILL.md) to choose artwork emphasis and placement
within the screen/dialog hierarchy. Standalone artwork delivery does not require
a screen redesign or full UI review; this skill remains the artwork style owner.

Apply [design-system](../../design-system/SKILL.md) and its
[resource policy](../../design-system/references/resources.md) for integration.
That skill owns paths, formats, accessors, tokens and previews; this skill owns
the illustration's visual identity. UI icons remain AppIcon vectors, not small
raster substitutes, and existing logos must not be regenerated incidentally.

For decorative UI artwork, MUST use the design-system
[Illustration](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/illustration/Illustration.kt)
and its existing size styles instead of repeating Image sizing/scale logic.
For an illustration with an empty-state message, use
[EmptyState](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/empty/EmptyState.kt)
with its illustrated style.
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
for controls at the [standard phone viewport](../../design-system/references/visual-consistency.md#default-viewport-scope).
Decorative images use null contentDescription;
meaningful information needs a localized accessible equivalent.
