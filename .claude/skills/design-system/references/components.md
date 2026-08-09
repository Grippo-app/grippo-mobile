# `:design-system:components` — shared components

Atomic Composables consumed by every UI feature. The project has dozens; the most-used
categories are listed here. Each component:

- Reads tokens via `AppTokens.*` (or accepts them as parameters).
- Exposes `modifier: Modifier = Modifier`.
- Receives callbacks as `() -> Unit` / `(T) -> Unit`.
- Is `public`.
- Has at least one `@AppPreview` covering the default state (the IDE preview).
- **If it is built from a Figma node** (a `figmaEnabled` task with a non-`none` `## Design`
  bullet): also has a Roborazzi capture `@Test` per bullet/state, wrapped in
  `PreviewContainerComponent` (sized to `frameSizeDp`) — the gate's real comparison. This is
  NOT optional and is NOT replaced by the `@AppPreview`; a missing capture is a
  `MISSING_CAPTURE` BLOCKER. Canonical harness + wrapper: the validation-gates
  `screenshot-fidelity-gate.md` §2 (source of truth — do not fork). Do NOT strip the design
  bullet to dodge this — the `ship-done`/`verify-done` UI-by-evidence backstop blocks a task
  that cites a node URL or a `designComponentId:` machine snapshot, and a newly created widget
  file structurally demands a bullet (see `../../task-prep/references/figma-split.md`).

## `Toolbar`

```kotlin
@Composable
public fun Toolbar(
    modifier: Modifier = Modifier,
    title: String? = null,
    style: ToolbarStyle = ToolbarStyle.Default,
    leading: Leading = Leading.Nothing,
    trailing: (@Composable BoxScope.() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
)

@Immutable
public enum class ToolbarStyle {
    Transparent,
    Default,
}

@Stable
public sealed class Leading {
    @Stable public data class Back(val onClick: (() -> Unit)) : Leading()
    @Stable public data class Profile(val onClick: (() -> Unit)) : Leading()
    @Immutable public data object Nothing : Leading()
}
```

Usage:

```kotlin
Toolbar(
    title = AppTokens.strings.res(Res.string.note_title),
    leading = Leading.Back(onClick = contract::onBack),
    trailing = {
        Button(
            content = ButtonContent.Icon(ButtonIcon.Icon(AppTokens.icons.MoreVertical)),
            style = ButtonStyle.Tertiary,
            size = ButtonSize.Small,
            onClick = contract::onMenuClick,
        )
    },
)
```

The top of nearly every screen.

## `BottomSheetToolbar`

```kotlin
@Composable
public fun BottomSheetToolbar(
    modifier: Modifier = Modifier,
    allowBack: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
)
```

Rendered **once** by the shared dialog host (`:shared/.../dialog/DialogScreen.kt`) at the
top of every `ModalBottomSheet`. Individual `:ui-dialog-features:*` `<X>Screen.kt` files do
**not** include their own `BottomSheetToolbar` — they only contribute the body content
(title `Text` + form + buttons) inside a
`BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog))`. Shows an
animated back arrow (when `allowBack`, driven by the internal dialog stack depth) and a
close button.

## `BottomOverlayContainer`

```kotlin
@Composable
public fun BottomOverlayContainer(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    overlay: Color,
    content: @Composable (Modifier, PaddingValues) -> Unit,
)
```

The standard "scrollable content + sticky bottom CTA" pattern. The `content` lambda
receives:

- A `Modifier` to apply to its outer container (handles weight/fillMaxSize).
- A `PaddingValues` to apply at the bottom of its scrolling content (so the last item isn't
  hidden behind the CTA).

Usage:

```kotlin
BottomOverlayContainer(
    overlay = AppTokens.colors.background.screen,
    bottom = {
        Button(onClick = contract::onApplyClick, content = ButtonContent.Text(text = AppTokens.strings.res(Res.string.apply)))
    }
) { contentModifier, contentPadding ->
    LazyColumn(
        modifier = contentModifier,
        contentPadding = contentPadding,
    ) {
        // ... items
    }
}
```

The overlay color fades from transparent (top) to `overlay` (bottom) behind the CTA, so the
last list item bleeds smoothly under it.

## `Button`

```kotlin
@Composable
public fun Button(
    modifier: Modifier = Modifier,
    content: ButtonContent,
    style: ButtonStyle = ButtonStyle.Primary,
    state: ButtonState = ButtonState.Enabled,
    size: ButtonSize = ButtonSize.Medium,
    onClick: () -> Unit,
    textStyle: TextStyle = AppTokens.typography.b14Bold(),
)

@Immutable
public sealed interface ButtonContent {
    @Immutable public data class Text(
        val text: String,
        val startIcon: ButtonIcon? = null,
        val endIcon: ButtonIcon? = null,
    ) : ButtonContent
    @Immutable public data class Icon(val icon: ButtonIcon) : ButtonContent
}

@Immutable public sealed interface ButtonStyle {
    public data object Primary : ButtonStyle
    public data object Secondary : ButtonStyle
    public data object Tertiary : ButtonStyle
    public data object Transparent : ButtonStyle
    public data object Error : ButtonStyle
}

@Immutable public sealed interface ButtonSize {
    public data object Small : ButtonSize
    public data object Medium : ButtonSize
}

@Immutable public enum class ButtonState { Enabled, Loading, Disabled }
```

`state = ButtonState.Loading` shows a spinner inside the button and blocks interaction;
`state = ButtonState.Disabled` greys it out without a spinner. Derive the state from active
loaders + form validity:

```kotlin
val isSaving = remember(loaders) { NoteDetailLoader.SavingAmount in loaders }
val buttonState = when {
    isSaving -> ButtonState.Loading
    state.amount !is AmountFormatState.Valid -> ButtonState.Disabled
    else -> ButtonState.Enabled
}
Button(
    content = ButtonContent.Text(text = AppTokens.strings.res(Res.string.apply)),
    state = buttonState,
    onClick = contract::onApplyClick,
)
```

Use `ButtonContent.Icon(ButtonIcon.Icon(AppTokens.icons.Cancel))` for square icon-only
buttons (see `BottomSheetToolbar`).

## `Input` family

This template specializes the family per domain rather than offering a generic
`InputText` — required infrastructure variants (`InputEmail`, `InputPassword`, `InputName`,
`InputDate`) plus product-specific ones (`InputAmount`, `InputSearch`, `InputToken`, and any
`<Domain>` extras the product needs). Each:

- Takes a `*FormatState` instead of a raw `String`.
- Calls back with a raw `String` (`onValueChange: (String) -> Unit`).
- Displays a placeholder, an error mapping derived from the format state, and (where
  relevant) leading/trailing icon affordances (clear, show-password, ...).
- Delegates rendering to an internal `Input` core that owns focus, keyboard options, and
  visual chrome.

```kotlin
@Composable
public fun InputEmail(
    modifier: Modifier = Modifier,
    value: EmailFormatState,
    placeholder: String = AppTokens.strings.res(Res.string.email_placeholder),
    onValueChange: (String) -> Unit,
)
```

ViewModel translates the raw `String` to `*FormatState.of(...)` on every change. Add a new
specialized input by composing the internal `Input` core with a domain-specific
`*FormatState` rather than parameterizing a generic `InputText`.

## `EmptyState`

```kotlin
@Composable
public fun EmptyState(
    modifier: Modifier = Modifier,
    value: ImageVector,
    text: String,
)
```

The standard zero-state — a centred illustration on top, a single line of caption text
underneath. Pick `value` from `AppTokens.icons.*` (`EmptyNote`, `EmptyTag`, ...) and source
`text` via `AppTokens.strings.res(...)`. If a CTA is needed, render an explicit `Button` next
to the `EmptyState` in the parent layout rather than threading it through this component.

## `BannerCard`

Card-style banner used for informational or promotional content:

```kotlin
@Composable
public fun BannerCard(
    modifier: Modifier = Modifier,
    style: BannerCardStyle,
    icon: ImageVector,
    title: String,
    description: String? = null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
)

@Immutable
public sealed interface BannerCardStyle {
    public data object Notice : BannerCardStyle
    public data object Info : BannerCardStyle
    public data object Success : BannerCardStyle
    public data object Warning : BannerCardStyle
    public data object Error : BannerCardStyle
    @Immutable public data class Custom(val tint: Color) : BannerCardStyle
}
```

`style` is a sealed interface (not an enum) so a screen can pass a one-off
`BannerCardStyle.Custom(tint = ...)` without expanding the shared palette. `enabled = false`
greys the banner out; `trailing` opts a slot composable into the right-hand side (typically
an icon button).

## `LineIndicator`

```kotlin
@Composable
public fun LineIndicator(
    modifier: Modifier = Modifier,
    progress: Float,
    colors: AppColor.Charts.IndicatorColors = AppTokens.colors.charts.indicator.primary,
    barHeight: Dp = AppTokens.dp.charts.lineIndicator.barHeight,
    labelSpacing: Dp = AppTokens.dp.charts.lineIndicator.labelSpacing,
    startLabel: (@Composable () -> Unit)? = null,
    endLabel: (@Composable () -> Unit)? = null,
    marker: (@Composable () -> Unit)? = null,
)
```

Slim horizontal progress bar reading colors from `AppTokens.colors.charts.indicator.*`
(sub-palettes for `primary`, `success`, `info`, `warning`, `error`, `muted`). Optional
`startLabel` / `endLabel` slots render captions on either end of the bar (e.g. min/max
values, percentages); `marker` is a free-form composable positioned at `progress` for
callouts like dots or arrows. Used for metric breakdowns and onboarding progress — there is
no built-in orientation flag or segment array; multi-segment displays are composed by
stacking multiple `LineIndicator`s.

## `Chip`

Small tag — typically used in filter rows. `Chip` is more configurable than a simple
`label + selected + onClick`; it carries a sealed `ChipLabel` / `ChipTrailing` / `ChipStyle`
family so a single component can render text, icon-only, or custom-trailing chips with or
without a click handler. The base `Chip` is fully parameter-driven (no defaults beyond
`modifier`) — colors and brush are passed in by the domain-specific wrapper:

```kotlin
@Composable
public fun Chip(
    modifier: Modifier = Modifier,
    label: ChipLabel,
    value: String,
    style: ChipStyle,
    trailing: ChipTrailing,
    size: ChipSize,
    textColor: Color,
    iconColor: Color,
    brush: Brush,
)

@Stable public sealed interface ChipLabel {
    @Stable public data class Text(val uiText: UiText) : ChipLabel
    @Stable public data object Empty : ChipLabel
}

@Stable public sealed interface ChipSize {
    @Stable public data object Small : ChipSize
    @Stable public data object Medium : ChipSize
}

@Stable public sealed interface ChipTrailing {
    @Stable public data object Empty : ChipTrailing
    @Stable public data class Icon(val icon: ImageVector) : ChipTrailing
    @Stable public data class Content(val lambda: @Composable () -> Unit) : ChipTrailing
}

@Stable public sealed interface ChipStyle {
    @Stable public data object Default : ChipStyle
    @Stable public data class Clickable(val onClick: () -> Unit) : ChipStyle
}
```

Pass `style = ChipStyle.Clickable { ... }` to make a chip interactive; otherwise it renders
as a static tag. Domain-specific variants (`NoteChip`, `TagChip`, `CategoryChip`, ...) live
next to `Chip.kt`, pick `textColor`/`iconColor`/`brush` from `AppTokens.colors.*`, and call
the base `Chip(...)`.

## Selectable cards

This template splits this into two siblings rather than a single `SelectableCard`:

- **`CheckSelectableCard`** — checkmark-style selection (`Large` / `Medium` / `Small` size
  variants under `cards/selectable/`). Used in lists where the user picks one or more
  options.
- **`ToggleSelectableCard`** — switch-style selection (`Medium` / `Small` variants). Used
  when the option semantically maps to "on / off".

Each entry point delegates to an internal per-size implementation (`CheckSelectableCardLarge`,
`ToggleSelectableCardMedium`, ...). Use the matching `AppTokens.dp.checkSelectableCard.*` /
`AppTokens.dp.toggleSelectableCard.*` groups when laying out external spacing.

## `Toggle`

Custom toggle / switch primitive (`Toggle.kt`), themed via `AppTokens.colors.toggle.*` and
sized via `AppTokens.dp.toggle.*`. This template does not ship general-purpose `Switch`,
`Checkbox`, or `RadioGroup` composables — Material3 primitives are not re-exported as
design-system entries. When a "checkbox" feel is needed, prefer `CheckSelectableCard`; when a
"switch" feel is needed, prefer `ToggleSelectableCard` or `Toggle`.

## Rules for all components

- **Read `AppTokens` for visual values**, or take them as parameters if the component is in
  `:compose-libs:*`.
- **Annotate every input type and enum for Compose stability.** `@Stable` is the baseline
  (always correct, and what the shared primitives use); use `@Immutable` only for
  deeply-immutable data (all `val`, no lambdas or mutable state). A type that carries a
  lambda or `@Composable` slot — e.g. `ChipTrailing.Content`, `ChipStyle.Clickable` — must
  be `@Stable`, never `@Immutable`. (This template's `Toolbar.kt` predates this rule and
  still marks `Leading.Back`/`Leading.Profile` `@Immutable` — follow the rule and the
  `Chip.kt` convention, not that file.)
- **Names are nouns** (`Toolbar`, `Button`, `Chip`) — not verbs or adjectives.
- **One file per component** (with related types in the same file: `Toolbar.kt` contains
  `Toolbar`, `ToolbarStyle`, `Leading`).
- **Group files into category sub-packages.** Every component file lives in a category
  sub-folder of `:design-system:components`, not in the flat module root: `input/` (text
  fields, checkbox, radio, toggle), `button/`, `card/`, `navigation/` (nav bar, toolbar,
  tabs), `row/` (list-style rows), `indicator/` (badge, page indicator, step bar), `text/`
  (inline rich text). The `package` declaration matches the directory (directory = package —
  see `packages.md`), e.g. `navigation/Toolbar.kt` declares
  `package com.<org>.<product>.design.components.navigation`. A new widget goes in the
  matching category; add a new category sub-package only when the widget fits none of the
  existing ones.
- **Public API only** — every component is `public`. The implementation may have `private`
  helpers in the same file.
- **At least one `@AppPreview`** below each component.
- **`modifier: Modifier = Modifier` is the first parameter** of every public composable in
  this module (the template's convention diverges from the official Compose guideline
  of "modifier after required inputs"). Required content / state inputs follow. **The one
  exception is `Toggle`**, where `modifier` trails `onCheckedChange` — a known outlier, not a
  pattern to copy (see `../../ui-feature/references/compose-rules.md`
  § "Function signature").
- **Callbacks** are named `onClick`, `onApplyClick`, `onValueChange`; they sit alongside the
  other inputs and typically end the parameter list.
- **Entity components bind to a `*State` model; primitives stay slot/enum-driven.** A widget
  that renders a product entity (`NoteRow`, `NoteChip`, the `Input` family via `*FormatState`)
  takes that entity's `*State` model as **one** parameter — not its fields spread flat. A
  domain-agnostic primitive (`Button`, `Chip`, `Toolbar`, `BannerCard`) takes sealed/enum
  dimensions + slots instead, so it stays reusable. The line between the two is the table
  just below; the signature rule itself is
  `../../ui-feature/references/compose-rules.md` § "Entity composables".

## Component vs `:compose-libs:*`

| Test | Result |
|---|---|
| Uses `AppTokens` internally | `:design-system:components` |
| Uses product types (e.g. `Note`, `Tag`) | `:design-system:components` |
| Pure widget, takes Compose primitives (Color, TextStyle, Dp) as parameters | `:compose-libs:*` |
| Has its own gesture/animation logic > 100 LoC | `:compose-libs:*` |

See the ui-feature skill's [`module-structure.md`](../../ui-feature/references/module-structure.md) § "When to extract a widget into `:compose-libs:*`" for the decision logic.
