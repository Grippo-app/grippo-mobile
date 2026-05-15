# `BaseScreen` / `BaseComposeScreen`

`BaseComposeScreen` is a thin Composable wrapper applied at the root of every screen. It sets the screen background, lays out content in a `Column`, and registers a tap-anywhere gesture that clears focus (dismisses the keyboard).

## Signature

```kotlin
@Composable
public fun BaseComposeScreen(
    background: ScreenBackground.Color,
    content: @Composable ColumnScope.() -> Unit,
)
```

## Implementation shape

```kotlin
@Composable
public fun BaseComposeScreen(
    background: ScreenBackground.Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val focusInteractionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .background(background.value)
            .clickable(
                interactionSource = focusInteractionSource,
                indication = null,
                onClick = { focusManager.clearFocus(force = true) },
            ),
        content = content,
    )
}
```

Notes:

- `Column` (not `Box` or `LazyColumn`). Top-level screen content stacks vertically.
- No `fillMaxSize()` is applied. The wrapper lets content drive height; full-screen sizing comes from the screen's own content (e.g. a `Toolbar` + `LazyColumn(Modifier.weight(1f))`) or from parent containers.
- `clickable` with `indication = null` — the click is invisible (no ripple).
- `focusManager.clearFocus(force = true)` — releases keyboard focus on background tap.

## `ScreenBackground`

```kotlin
@Stable
public sealed interface ScreenBackground {
    @Immutable
    public data class Color(public val value: ComposeColor) : ScreenBackground
}
```

Future-extensible (e.g. a `Gradient` subtype) without breaking the API.

## Usage

```kotlin
@Composable
internal fun ProfileBodyScreen(
    state: ProfileBodyState,
    loaders: ImmutableSet<ProfileBodyLoader>,
    contract: ProfileBodyContract,
) {
    BaseComposeScreen(background = ScreenBackground.Color(AppTokens.colors.background.screen)) {
        Toolbar(
            title = AppTokens.strings.res(Res.string.profile_body_title),
            leading = Leading.Back(onClick = contract::onBack),
        )

        // ... actual content
    }
}
```

The `content` lambda receives a `ColumnScope` — child Composables can use `Modifier.weight(1f)`, `Modifier.padding(...)`, etc.

## Rules

- **Every top-level Screen is wrapped in `BaseComposeScreen`.** No exceptions — both stack screens and dialog screens.
- **Background color comes from `AppTokens`** — `AppTokens.colors.background.screen` for stack screens; `AppTokens.colors.background.dialog` for dialog screens.
- **Dialog screens reuse `BaseComposeScreen`.** `DialogContentComponent` hosts each dialog inside a `ModalBottomSheet`; its content lambda calls the dialog's `<X>Screen`, which itself is a `BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) { ... }`. Dialogs lay out their `Column` content as: `Spacer(AppTokens.dp.dialog.top)` → centered title `Text` → body → `Spacer(AppTokens.dp.dialog.bottom)` → `Spacer(Modifier.navigationBarsPadding())`. Horizontal padding comes from `AppTokens.dp.dialog.horizontalPadding`. No dialog uses `BottomSheetToolbar`.

## What `BaseComposeScreen` does **not** do

- It does **not** provide a `Toolbar`. The screen adds its own `Toolbar(...)` at the top.
- It does **not** handle system insets (status bar, navigation bar). Edge-to-edge is configured at the Activity level; per-screen insets are handled by `Toolbar` and `BottomOverlayContainer`.
- It does **not** provide a scrolling container. Scrolling content uses `LazyColumn` inside the `content` lambda.

## Why a wrapper

Every screen needs the same background + same keyboard-dismiss gesture. Centralizing it:

- Ensures consistency across screens.
- Makes "tap anywhere to dismiss keyboard" automatic — no per-screen `Modifier.clickable { focusManager.clearFocus() }` boilerplate.
- Provides a single point to add future cross-cutting concerns (e.g. a global progress overlay, a theme override).
