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
- An entity drawn by a card family keeps its drawing sizes in an entity group,
  and each card variant that sizes the drawing itself references them instead of
  the private scales, through the entity group's val (`muscle.image`, not the
  `Muscle.image` object path), as producers read `size` or `padding`; for
  example `muscleCard.compact.icon = muscle.image.medium`. The full role-to-token
  mapping lives in [CONSISTENCY.md](../../../../design-system/CONSISTENCY.md). The
  steps are per entity, so equal sizes across entities are not implied. Card-only
  geometry (tile radius, padding, width, optical padding) stays in the card
  variant and may be derived from the drawing (`muscleCard.load.size` is the
  drawing plus its padding). A variant that hands the drawing to a shared
  component, such as Selection through ToggleSelectableCard, keeps that
  component's slot. Entity-wide properties such as `muscle.figure.aspectRatio`
  also live in the entity group.
- MUST declare an entity val above its card group's val (`muscle` before
  `muscleCard`, `equipment` before `equipmentCard`). `AppDp` initializes its vals
  in declaration order, so a card group declared above its entity reads an
  uninitialized entity and crashes on the first access to `AppDp` at app start;
  a build does not catch it.
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
