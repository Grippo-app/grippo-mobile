# Dimension tokens

MUST inspect [AppDp](../../../../design-system/resources/provider/src/commonMain/kotlin/com/grippo/design.resources/provider/AppDp.kt)
before choosing a slot. It is one static data object, not a per-theme interface.
Private Padding/Size/Radius/Icon scales support public component-specific groups.
There are no public `AppTokens.dp.padding`, `size`, `radius`, `icon`, `card` or `toolbar`
scale groups. Toolbar dimensions are under `screen.toolbar` or `bottomSheet.toolbar`.

- Consumers MUST use semantic AppTokens.dp slots for product dimensions. MUST NOT
  add raw dp offsets to a token to create an undocumented consumer dimension.
  Neutral technical constants such as 0.dp are permitted; decorative spacing is
  still a product dimension requiring a token.
- Reuse a slot only when its meaning matches. Add a demanded slot in the owning
  group when needed; do not borrow an unrelated icon/card token merely for its number.
- Producers reuse private scales where their meaning fits. An isolated demanded
  dimension may have a literal inside its semantic group. A new base-scale entry
  requires an authorized design-grid change with concrete consumers.
- MUST preserve actual adaptive thresholds, fractions and minimum heights owned by
  AppDp. ViewModels MUST NOT own visual dimensions. Dimensions do not vary by theme;
  adaptation follows measured constraints/font scale in the existing layout.

`contentPadding.block/content/subContent/text` describe nesting depth. Their role
usage is maintained in [visual usage rules](../../../../design-system/CONSISTENCY.md).
Do not duplicate numeric scales or component API snapshots in this reference.
Review caller meaning and adaptive layout, not just matching literal values.
