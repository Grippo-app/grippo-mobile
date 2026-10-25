# Dimension tokens

Inspect [AppDp](../../../../design-system/resources/provider/src/commonMain/kotlin/com/grippo/design.resources/provider/AppDp.kt)
before choosing a slot. Current shape, for orientation (source owns changes):
`public data object AppDp` is one static object, not a per-theme interface.
Private `padding`/`size`/`radius`/`icon` scales feed semantic component groups.
Inspect the producer for current groups and leaves.
There are no public `padding`, `size`, `radius`, `icon`, `card` or `toolbar` groups:
toolbar dimensions live at `screen.toolbar` and `bottomSheet.toolbar`; `screen` also
holds `onboardingHeroHeight`, `onboardingHeroRadius` and `verticalPadding`;
`dialog.maxMessageHeight` bounds message text; `shadow.component/card/container`
are the elevation steps.

- Consumers MUST use semantic AppTokens.dp slots for product dimensions. MUST NOT
  add raw dp offsets to a token to create an undocumented consumer dimension.
  Neutral technical constants such as 0.dp are permitted; decorative spacing is
  still a product dimension requiring a token.
- Reuse a slot only when its meaning matches. Add a demanded slot in the owning
  group when needed; do not borrow an unrelated icon/card token merely for its number.
- Groups are hierarchical. A leaf names a property of the group it sits in
  (`radius`, `horizontalPadding`, `borderWidth`); a sub-element or a variant is a
  nested `data object` exposed through a `val` of the same name:
  `iterationCard.editor.minActionHeight`,
  `metrics.panel.large.radius`, `screen.toolbar`, `bottomSheet.toolbar`,
  `checkSelectableCard.small.leadingIcon`. MUST NOT flatten a sub-element into
  prefixed leaves such as `setRadius`, `editorIcon` or `toolbarHeight`. The
  private base scales stay the producers behind the leaves. The same rule holds
  for every `AppTokens` producer with groups (`AppColor`:
  `colors.charts.tooltip.background`).
- Producers reuse private scales where their meaning fits. An isolated demanded
  dimension may have a literal inside its semantic group. A new base-scale entry
  requires an authorized design-grid change with concrete consumers.
- Adaptive thresholds, fractions and minimum heights MUST remain at their AppDp
  owner; authorized changes update that owner and affected callers. Text heights
  and text-slot minimums are not dimensions: [typography.md](typography.md) owns
  them. ViewModels MUST NOT own visual dimensions. Dimensions do not vary by theme; adaptation
  follows measured constraints/font scale in the existing layout.

`contentPadding.block/content/subContent/text` describe nesting depth. Their role
usage is maintained in [visual usage rules](../../../../design-system/CONSISTENCY.md).
Do not duplicate numeric scales or component API snapshots in this reference.
Review caller meaning and adaptive layout, not just matching literal values.
