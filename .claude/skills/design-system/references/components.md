# Shared Compose components

MUST inspect the component's actual declaration and all affected consumers before
using or changing its contract. Source owns complete signatures, token paths and
callback behavior; this reference does not duplicate component implementations.

## Entry points and non-obvious behavior

| Topic | Current source and invariant |
|---|---|
| Toolbar | [Toolbar.kt](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/toolbar/Toolbar.kt); independent screens own their toolbar |
| Dialog title/actions | [BottomSheetToolbar.kt](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/toolbar/BottomSheetToolbar.kt); the shared DialogScreen renders one, dialog bodies MUST NOT add a duplicate |
| Bottom CTA/insets | [BottomOverlayContainer.kt](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/frames/BottomOverlayContainer.kt); apply its resolved content Modifier and PaddingValues to the scrolling content |
| Button | [Button.kt](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/button/Button.kt); loading keeps label/icons/width, shimmers, blocks interaction and exposes indeterminate progress |
| Input | [inputs/](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/inputs/); editable fields accept matching FormatState and raw text callbacks, picker-backed fields use typed selection and onClick |
| BannerCard | [BannerCard.kt](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/banner/BannerCard.kt); Custom tint still comes from token colors, disabled appearance has its own semantics |
| EmptyState | [EmptyState.kt](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/empty/EmptyState.kt); add a demanded CTA in its parent rather than assuming an action parameter |
| LineIndicator | [LineIndicator.kt](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/indicators/LineIndicator.kt); inspect nested chart color type and dp.metrics.charts.indicator defaults, optional start/end label and marker slots |
| Chip | [Chip.kt](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/chip/Chip.kt); primitive receives colors/brush and sealed label/trailing/style/size inputs; product wrappers own token selection |

- MUST reuse the appropriate existing input/formatter. Add a formatter only for
  different parsing/validation/display needs, never merely for another screen.
  Inspect onFocused/onClear/onNext/onDone callbacks where analytics coverage needs
  them; MUST NOT infer complete inputs from an old shortened signature.
- Button state comes from actual loaders and validity; MUST NOT add business
  eligibility rules inside the widget. Feature owners keep logical selection.
- [Shared shimmer](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/modifiers/Shimmer.kt)
  adds highlight only, not skeleton shape/loading semantics. Apply
  it after clip/background; owning component supplies shape/semantics. Opacity
  belongs to shimmer tokens. MUST NOT migrate unrelated loaders incidentally.
- Existing Check/CheckSelectableCard/ToggleSelectableCard/Toggle variants serve selection
  roles. MUST inspect actual sizes/contracts rather than assume a generic Switch,
  Checkbox, RadioGroup or a nonexistent product wrapper.
- Toggle owns its Switch state/disabled semantics. Use its enabled input for
  unavailable controls and contentDescription for a label; do not rebuild a
  feature-specific Material Switch or duplicate state semantics in a consumer.
- [Check](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/selectors/Check.kt)
  follows Toggle's controlled-state, callback, token and animation ownership.
  Selection semantics and interaction stay inside the component; consumers pass
  checked state/callback and supply a label, without a duplicate parent click.
- MuscleSelection's measured bottom slot reserves list space before adaptive hero
  sizing. Keep bounded layout, immutable inputs and caller-owned selection. Group
  changes may restart scrolling without clearing caller selection.

## Contract and placement gate

Public widget entry points MUST use explicit visibility, modifier default and
neutral callbacks. Helpers remain internal/private. Source-dependent signature,
entity-model parameter and Toggle outlier rules belong in
[compose-rules.md](../../ui-feature/references/compose-rules.md); MUST NOT keep a
second signature policy here. Compose stability annotations MUST match their
contracts. Project callback/composable-slot model types follow the existing @Stable
convention when its contract holds; MUST NOT label them @Immutable to hide mutable
observable data. Deeply immutable data uses @Immutable.

Place new widgets in the actual matching category/package (e.g. inputs, toolbar,
indicators); preserve existing dotted directories. MUST NOT move files to invented
input/navigation folders as incidental cleanup. New widgets and visual changes
require representative private AppPreview coverage in PreviewContainer; other
edits require a visual-impact assessment and review of relevant existing
previews/callers. [previews.md](previews.md) owns coverage; do not mechanically
add fixtures for changes without visual impact.

AppTokens/product UI-state consumers belong to design-system. Product-independent
widgets accept style/data parameters in compose-libs; MUST NOT classify ownership
by a copied signature or line count alone. Use
[module-structure.md](../../ui-feature/references/module-structure.md) for extraction.
Review source gates and [whole-screen consistency](visual-consistency.md), including
input/focus behavior when affected. Feature analytics and preservation remain
owned by their canonical skills.
