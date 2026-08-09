# Image loader — `AsyncImage` usage

This template wraps `AsyncImage` per design-system component rather than inlining it at
call sites — see `:design-system:components/note/NoteImage.kt`:

```kotlin
val fallback = rememberInsetVectorPainter(AppTokens.icons.QuestionCircle, 12.dp)

AsyncImage(
    modifier = modifier
        .clip(RoundedCornerShape(AppTokens.dp.noteImage.medium.radius))
        .size(AppTokens.dp.noteImage.medium.size)
        .background(AppTokens.colors.background.card),
    model = value,
    contentScale = ContentScale.Crop,
    contentDescription = null,
    placeholder = fallback,
    error = fallback,
)
```

Two things to copy from this pattern:

- **Sizes/radii live in a component-scoped `AppDp` group**
  (`AppTokens.dp.noteImage.medium.size`). The `AppDp.icon` / `AppDp.padding` / `AppDp.size` /
  `AppDp.radius` primitives are `private` inside `AppDp` — call sites must use a public
  component group (`noteImage`, `userCard`, `tagCard`, …). Add a new public group if no
  existing one fits.
- **Placeholders/errors come from `AppTokens.icons.<Name>`** (ImageVector extensions in
  `:design-system:resources:provider/icons/`), wrapped in a small `Painter` adapter
  (`rememberInsetVectorPainter`). Photographic placeholders ship as bare-noun `.webp` under
  `composeResources/drawable/` (e.g. `avatar.webp`, `placeholder.webp`) and are reached via
  `AppTokens.drawables.res(Res.drawable.<name>)` — there is no `img_*` prefix convention.

The `AsyncImage` Composable itself lives in `coil-compose`.
