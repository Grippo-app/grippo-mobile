# Dimension tokens

Inspect [AppDp](../../../../design-system/resources/provider/src/commonMain/kotlin/com/grippo/design.resources/provider/AppDp.kt)
before choosing a slot. Current shape, for orientation (source owns changes):
`public data object AppDp` is one static object, not a per-theme interface.
Private `padding`/`size`/`radius`/`icon` scales feed the top-level component
groups (inspect the file for the leaves):

- bottomBar, bottomSheet, divider, contentPadding, screen, muscleSelection, dialog
- input, button, loader, konfetti, segment, menu, checkSelectableCard
- toggleSelectableCard, wheelPicker, achievementCard, equipmentCard, timeline, exerciseCard
- exerciseExampleCard, weightHistoryCard, deleteUserCard, exerciseExampleImage, iterationCard, iterationsCard
- chip, toggle, check, calendar, metrics, brandHeader
- brandSplash, userCard, welcome, illustration, badge, bannerCard
- focusFrame, tooltip, training, accent, stepProgressIndicator, periodPicker
- pagerIndicator, swipe, shadow
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
- Producers reuse private scales where their meaning fits. An isolated demanded
  dimension may have a literal inside its semantic group. A new base-scale entry
  requires an authorized design-grid change with concrete consumers.
- Adaptive thresholds, fractions and minimum heights MUST remain at their AppDp
  owner; authorized changes update that owner and affected callers. ViewModels
  MUST NOT own visual dimensions. Dimensions do not vary by theme; adaptation
  follows measured constraints/font scale in the existing layout.

`contentPadding.block/content/subContent/text` describe nesting depth. Their role
usage is maintained in [visual usage rules](../../../../design-system/CONSISTENCY.md).
Do not duplicate numeric scales or component API snapshots in this reference.
Review caller meaning and adaptive layout, not just matching literal values.
