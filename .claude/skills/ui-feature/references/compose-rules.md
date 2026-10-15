# Feature Compose integration

Shared component signatures, structure, stability, effects, lazy identity and
runtime discipline follow [compose-components](../../compose-components/SKILL.md).
[UI state](state.md) owns feature data models; [design-system](../../design-system/SKILL.md)
owns product tokens, widget contracts, resources and previews.

## Screen function signature

Every `<Name>Screen.kt` is an internal composable taking `state`, `loaders`,
`contract` in that order and with those names. Add a leading `component` only
when it renders a child stack/slot. Inspect the actual feature declarations for
their types; [MVI contract](mvi-contract.md#namescreenkt) owns the surrounding
seven-file responsibilities.

## Feature composition and placement

Apply the [Compose extraction criteria](../../compose-components/references/structure-and-runtime.md#read-from-top-to-bottom)
for screen composition and helper boundaries. [Feature file layout](module-structure.md#file-layout-for-a-feature)
owns extracted component placement; [module ownership](module-structure.md#when-to-extract-a-widget-into-compose-libs)
owns shared widget placement.

Regular Toolbar belongs to screens; dialogs reuse the shared host's toolbar.
Follow [BaseComposeScreen](base-classes.md#basecomposescreen--screenbackground)
for screen/dialog shells and [BottomOverlayContainer](../../design-system/references/components.md)
for list content padding around a bottom action.

## Secret input keyboard

Reuse InputPassword with `KeyboardType.Password`,
`KeyboardCapitalization.None` and `autoCorrectEnabled = false`, including while
the value is visible. Preserve its existing focus, visibility and IME callbacks
and default focus-clearing behavior; keyboard actions do not implicitly submit.
Keep callbacks neutral and do not add a separate input framework.

## Feature effects

Navigation goes through Direction/eventListener; a composition effect must not
initiate navigation. Business state and observation stay in the ViewModel;
presentation/system effects follow
[Compose lifetimes](../../compose-components/references/structure-and-runtime.md#effects-and-changing-callbacks).
Dependencies resolve through Component/ViewModel, never Koin lookup in a composable.

## Product primitives

Feature UI reuses existing design-system widgets for their actual product roles,
including Button, inputs, Toolbar and selection components. Add a missing demanded
product role at its owning category; do not generate wrappers for every imported
symbol. Text/Icon primitives use explicit token styling. Existing tooltips, shared
sheet hosts and product-independent compose-libs retain their actual boundaries.
