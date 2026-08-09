# `AppDp` — spacing and sizing tokens

`AppDp` (read via `AppTokens.dp`) is the **single source of dimensional values**: padding,
sizing, corner radii, icon sizes, screen offsets. Every Composable that takes a `Dp`
parameter sources it from here.

## Shape

`AppDp` is a `public data object` containing nested groups. The shape is verbose but
explicit:

```kotlin
public data object AppDp {
    // private base scales — referenced by named groups below
    private data object Padding {
        val tiny: Dp = 2.dp
        val extraSmall: Dp = 4.dp
        val small: Dp = 8.dp
        val medium: Dp = 12.dp
        val large: Dp = 16.dp
        val xLarge: Dp = 20.dp
    }
    private data object Size {
        val tiny: Dp = 24.dp
        val small: Dp = 32.dp
        val medium: Dp = 50.dp
        val large: Dp = 64.dp
        val xLarge: Dp = 96.dp
        val xxLarge: Dp = 262.dp
    }
    private data object Radius {
        val small: Dp = 8.dp
        val medium: Dp = 12.dp
        val large: Dp = 28.dp
    }
    private data object Icon {
        val extraSmall: Dp = 12.dp
        val small: Dp = 18.dp
        val medium: Dp = 22.dp
        val large: Dp = 24.dp
        val xLarge: Dp = 32.dp
        val xxLarge: Dp = 64.dp
        val xxxLarge: Dp = 100.dp
        val xxxxLarge: Dp = 200.dp
    }

    // public groups exposed via AppTokens.dp.<group>.<slot>
    val screen: Screen = Screen
    val bottomSheet: BottomSheet = BottomSheet
    val dialog: Dialog = Dialog
    val contentPadding: ContentPadding = ContentPadding
    val input: Input = Input
    val button: Button = Button
    val chip: Chip = Chip
    val toggle: Toggle = Toggle
    val segment: Segment = Segment
    val menu: Menu = Menu
    val wheelPicker: WheelPicker = WheelPicker
    val tooltip: Tooltip = Tooltip
    // ... ~30 named groups, plus product-specific cards
    //   (noteCard, tagCard, noteHistoryCard, <entity>Card, noteDetail, <entity>Metrics, ...)
}
```

There are no public `radius`, `icon`, `card`, or `toolbar` top-level groups — radius/icon
scales are private, and each card / toolbar variant lives inside the component group that
uses it (`Screen.Toolbar`, `BottomSheet.Toolbar`, `NoteCard.radius`,
`TagImage.Medium.radius`, etc.).

### Naming

- Base scales (`Padding.small`, `Size.medium`, ...) are **private** — they define the
  design grid.
- Named public groups (`screen.horizontalPadding`, `toolbar.height`, ...) compose from
  base scales and have **semantic** names that describe the intent.

Composables use only the named public groups, never the private base scales.
`AppTokens.dp.screen.horizontalPadding` is correct; `AppTokens.dp.padding.large` is not
exposed (the private scale is internal).

## Representative groups

```kotlin
public data object Screen {
    val toolbar: Toolbar = Toolbar               // nested — see below
    val horizontalPadding: Dp = padding.xLarge   // 20.dp
    val verticalPadding: Dp = padding.xLarge     // 20.dp

    public data object Toolbar {
        val height: Dp = size.small              // 32.dp
    }
}

public data object Button {
    public val small: Small = Small
    public val medium: Medium = Medium

    public data object Medium {
        val height: Dp = size.medium             // 50.dp
        val horizontalPadding: Dp = padding.medium
        val icon: Dp = AppDp.icon.medium
        val space: Dp = padding.small
        val spaceTransparent: Dp = padding.extraSmall
    }

    public data object Small {
        val height: Dp = size.small              // 32.dp
        val horizontalPadding: Dp = padding.medium
        val icon: Dp = AppDp.icon.medium
        val space: Dp = padding.small
        val spaceTransparent: Dp = padding.extraSmall
    }
}

public data object Input {
    val height: Dp = size.medium                 // 50.dp
    val radius: Dp = AppDp.radius.medium
    val horizontalPadding: Dp = padding.xLarge
    val icon: Dp = AppDp.icon.medium
}

public data object BottomSheet {
    val radius: Dp = AppDp.radius.large
    val toolbar: Toolbar = Toolbar

    public data object Toolbar {
        val height: Dp = size.small
    }
}

public data object ContentPadding {
    val block: Dp = padding.xLarge       // top-level block spacing
    val content: Dp = padding.medium     // standard content row spacing
    val subContent: Dp = padding.small   // dense in-row spacing
    val text: Dp = padding.extraSmall    // gap between adjacent text lines
}
```

`Button` is split into `Button.small` / `Button.medium` sub-groups (no `Large` variant)
and does not expose its own `cornerRadius` — the canonical button uses `CircleShape`. Card
sizing (corner radius, padding) lives on each specific card group (`NoteCard`,
`NoteHistoryCard`, `UserCard.Compact`, ...) rather than a single `Card` group.

## Why expose `contentPadding` semantically

`contentPadding.block`, `.content`, `.subContent`, `.text` are **named for nesting depth**
rather than mirroring the raw padding scale. A composable picks `block` for outer
page-level gaps, `content` between sibling rows, `subContent` inside dense rows, and `text`
between adjacent lines of text. Prefer a feature-specific group name if one exists.

## Usage

```kotlin
Modifier
    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
    .height(AppTokens.dp.button.medium.height)

Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.block))

Icon(
    painter = ...,
    modifier = Modifier.size(AppTokens.dp.input.icon),   // reuse the closest semantic group
)

Surface(
    shape = RoundedCornerShape(AppTokens.dp.input.radius),
)
```

Sizes / radii are reached through whichever semantic group owns them — `input.icon`,
`bottomSheet.radius`, `noteCard.icon`, `welcome.checkmark.size`. There is no shared
`AppTokens.dp.icon.*` or `AppTokens.dp.radius.*` public path.

## Why no theme switch on `AppDp`

`AppDp` does **not** vary by theme. Dimensions are visual identity; they stay the same in
light and dark mode. (If a product needs density-adaptive sizes, the right hook is
`LocalDensity` or screen-size queries, not theme.)

`AppDp` is a static `data object`, not an interface. There's only one implementation.

## Why semantic group names

`AppTokens.dp.button.medium.height` documents intent. `AppTokens.dp.size.medium` doesn't —
`size.medium` for what? A button? An avatar? A card? Semantic group names tell the reader
where this dimension applies.

There is no general-purpose `icon.*` or `radius.*` public group: icon sizes and radii are
inlined into whichever semantic group owns them (`input.icon`, `bottomSheet.radius`, ...).
If a new family of components needs a shared size/radius value, add a slot to the closest
semantic group rather than reintroducing a flat scale.

## Adding a new dimension

1. Is there a **semantic group** it fits (`screen`, `card`, `button`, ...)?
   - Yes → add a slot in that group.
   - No → add a new group.
2. Is the value derivable from the **base scales** (`Padding`, `Size`, `Radius`, `Icon`)?
   - Yes → derive (e.g. `val Card.padding = Padding.large`).
   - No → assign a raw `Dp` (e.g. `val Toolbar.height = 56.dp`).

Don't add private base scale entries — extend the existing scales sparingly. The base
sequence (`tiny, extraSmall, small, medium, large, xLarge, ...`) is the design grid;
adding entries dilutes the discipline.

## Anti-patterns

- **`12.dp` inline.** Forbidden. Use a token.
- **`AppTokens.dp.padding.large` from a feature** — `padding` is the private base scale;
  use the public group `AppTokens.dp.screen.horizontalPadding` (or equivalent).
- **`AppTokens.dp.screen.horizontalPadding + 4.dp`.** Adding raw dp at the call site
  defeats the token system. Either the token is wrong (fix it) or you need a new token.
- **Hardcoding `64.dp` for an avatar size.** Use the existing
  `AppTokens.dp.<entity>Card.avatar.*` slot (or whichever semantic group owns avatar
  sizing) — don't reach into a generic `icon.*` scale.
- **Reading dimensions in a ViewModel.** Same as colors — dimensions are pure UI; if a
  ViewModel needs to know a size, restructure.
