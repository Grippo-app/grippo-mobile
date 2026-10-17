# Shared Compose components

MUST inspect actual declarations and affected callers before changing a contract.
Visual creation/changes first pass
[usage/state analysis](../../brand-design/references/usage-and-states.md).
Source owns complete signatures, token paths and callback behavior; this reference
records component-specific constraints.

Current shape, for orientation: 36 top-level packages under
`design-system/components/src/commonMain/kotlin/com/grippo/design.components/`:
achievement, badge, banner, brand, button, cards, chart, chip, datetime, divider, dragdrop,
empty, equipment, example, frames, illustration, indicators, inputs, konfetti,
loading, menu, metrics, modifiers, muscle, navigation, segment, selectors, spliter,
swipe, toolbar, tooltip, training, user, utils, welcome, wheel. There is no
`EmptyState` composable; the empty-list owner is `IllustratedEmptyState`.

## Entity component families

When several visual components render the same UI entity with different roles or
densities, MUST extend one design-system component family with a sealed style or
variant contract instead of adding a parallel feature-owned renderer. Treat the
variants as a component matrix: each variant has a clear visual role, shares the
entity's existing `*State` input and retains its own needed interactions. Name
variants by role, size or density rather than a generic `Default`. The entry
composable takes only inputs common to all variants; variant-specific arguments,
including callbacks and neutral content slots, belong to the sealed subtype.
Inspect
[ExerciseCard.kt](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/training/ExerciseCard.kt):
`ExerciseCardStyle` currently contains Large, Medium and Small variants.

Caller-specific data and logic, such as a comparison with another workout, stay
with the caller. Its UI may enter the card through a neutral `@Composable` trailing
slot, carried by the sealed subtype when only that variant uses it. Shared
components MUST NOT import a feature's history, comparison or presentation model
to fill the slot. Inspect
[Chip.kt](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/chip/Chip.kt):
`ChipLeading.Content` carries a neutral content lambda. Slot-model stability follows
[Compose stability](../../compose-components/references/structure-and-runtime.md#stability-and-recomposition);
feature-State purity follows [UI state](../../ui-feature/references/state.md).

Components with different entities or semantic responsibilities may keep separate
owners. A new family does not require speculative variants. During a family
migration, inspect every existing variant and caller, preserve the established
behavior and cover the added variant under the existing preview rules.

## Entry points and non-obvious behavior

| Topic | Current source and invariant |
|---|---|
| Branded splash presentation | [BrandSplash.kt](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/brand/BrandSplash.kt); owns logo entrance/glow and loader placement; no navigation callbacks or minimum display time |
| Branded title and description | [BrandHeader.kt](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/brand/BrandHeader.kt); compact warm header, optional existing Grippo logo; content and navigation belong to its caller |
| Toolbar | [Toolbar.kt](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/toolbar/Toolbar.kt); independent screens own their toolbar |
| Highlighted content | [FocusFrame.kt](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/frames/FocusFrame.kt); warm selection by default; custom semantic callers supply both accent and background from tokens |
| Dialog title/actions | [BottomSheetToolbar.kt](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/toolbar/BottomSheetToolbar.kt); the shared DialogScreen renders one, dialog bodies MUST NOT add a duplicate |
| Bottom CTA/insets | [BottomOverlayContainer.kt](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/frames/BottomOverlayContainer.kt); apply its resolved content Modifier and PaddingValues to the scrolling content |
| Neutral horizontal boundary | [ContentDivider.kt](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/divider/ContentDivider.kt); caller-owned geometry follows [divider usage](../../../../design-system/CONSISTENCY.md#content-dividers) |
| Button | [Button.kt](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/button/Button.kt); loading keeps label/icons/width, shimmers, blocks interaction and exposes indeterminate progress |
| Input | [inputs/](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/inputs/); editable fields accept matching FormatState and raw text callbacks, picker-backed fields use typed selection and onClick; the core `Input` is internal, so a new field kind gets a public `Input*` wrapper here even for a single consumer |
| Equipment selection | [EquipmentSelection.kt](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/equipment/EquipmentSelection.kt); fixed group tabs and scrolling EquipmentRow list; selection and bottom actions belong to the caller |
| BannerCard | [BannerCard.kt](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/banner/BannerCard.kt); Custom tint and surface are supplied by token colors; semantic and disabled surfaces are owned by the themes |
| Decorative illustration | [Illustration.kt](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/illustration/Illustration.kt) and [IllustrationStyle.kt](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/illustration/IllustrationStyle.kt); owns token sizes, centering, aspect ratio and Fit; meaningful information stays in localized consumer text |
| IllustratedEmptyState | [IllustratedEmptyState.kt](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/empty/IllustratedEmptyState.kt); uses Illustration, supports a message or title with supporting text; CTA/insets belong to the parent |
| LineIndicator | [LineIndicator.kt](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/indicators/LineIndicator.kt); caller-supplied labels/markers must fit their actual chart and layout contracts |
| Chip | [Chip.kt](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/chip/Chip.kt); primitive receives colors/brush and sealed label/leading/interaction/size inputs; product wrappers own token selection |

- MUST reuse the appropriate existing input/formatter. Add a formatter only for
  different parsing/validation/display needs, never merely for another screen.
  Inspect onFocused/onClear/onNext/onDone callbacks where analytics coverage needs
  them.
- Button state comes from actual loaders and validity; MUST NOT add business
  eligibility rules inside the widget. Feature owners keep logical selection.
- [Illustration background](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/modifiers/IllustrationBackground.kt)
  is the single owner of the warm illustration-region gradient used by brand headers,
  hero blocks, the profile identity header and the loader. Consumers pass their own
  shape; MUST NOT rebuild the region fill from `illustration.backgroundStart/End`
  inline. Accepted exception: `BrandSplash` draws its animated radial glow from the
  same two colors plus transparent, because it is a glow, not a region fill.
- [Shared shimmer](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/modifiers/Shimmer.kt)
  adds highlight only, not skeleton shape/loading semantics. Apply
  it after clip/background; owning component supplies shape/semantics. Opacity
  belongs to shimmer tokens. MUST NOT migrate unrelated loaders incidentally.
- Existing Check/CheckSelectableCard/ToggleSelectableCard/Toggle variants serve
  selection roles. Inspect their actual size and state contracts; role-to-token
  choices stay in [CONSISTENCY.md](../../../../design-system/CONSISTENCY.md).
- Toggle with a callback owns its Switch state/disabled semantics. Use its enabled
  input for unavailable controls and contentDescription for a label. Without a
  callback it is a passive indicator inside a parent that owns the single Switch
  action and state; do not add a second interactive target or rebuild a
  feature-specific Material Switch.
- [Check](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/selectors/Check.kt)
  is the shared circular check for selection. Pass a callback and label for a
  standalone checkbox with its own touch target/semantics. Omit the callback for
  an indicator inside an already selectable surface; that parent owns its click,
  label and selected state, and Check adds no second interactive/accessibility target.
  Reuse this owner rather than adding card-specific check indicators. Toggle keeps
  its separate switch role.
- MuscleSelection's measured bottom slot reserves list space before adaptive hero
  sizing. Keep bounded layout, immutable inputs and caller-owned selection. Group
  changes may restart scrolling without clearing caller selection.

## Contract and placement gate

Shared signature, structure, runtime and interaction checks follow
[compose-components](../../compose-components/SKILL.md); visibility follows
[code style](../../validation-gates/references/code-style.md). Place widgets in the
actual matching category and preserve existing package layout.
[Preview rules](previews.md) own coverage.

Reusable widgets with product token/UI-state semantics belong to design-system.
Product-independent widgets accept style/data parameters in compose-libs. Use
[module-structure.md](../../ui-feature/references/module-structure.md) for extraction.
Review source gates and [whole-screen consistency](visual-consistency.md), including
compatibility with the usage/state inventory and affected input/focus behavior.
