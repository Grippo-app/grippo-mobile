# Cookbook — add a resource (string, plural, drawable, icon, font)

> Companion conventions: `accessors.md`, `resources.md`.

> **Concrete example.** The example task and identifiers below (`Note archive`, `tag-picker`,
> etc.) are illustrative; the recipe steps apply to any feature.

How to add new entries in `:design-system:resources:provider`.

## Add a string

### 1. Add the key + English value

In `:design-system:resources:provider/src/commonMain/composeResources/values/strings.xml`:

```xml
<string name="profile_note_archive_title">Note archive</string>
<string name="profile_note_archive_subtitle">Last %1$d days</string>
```

### 2. Add translations

In each concrete locale directory from `supportedLocales` (example:
`values-<locale>/strings.xml`):

```xml
<string name="profile_note_archive_title">Архів нотаток</string>
<string name="profile_note_archive_subtitle">Останні %1$d днів</string>
```

In another concrete locale directory when the project supports more than one locale (example:
another `values-<locale>/strings.xml`):

```xml
<string name="profile_note_archive_title">Архив заметок</string>
<string name="profile_note_archive_subtitle">Последние %1$d дней</string>
```

### 3. Compose Resources regenerates `Res.string.<key>`

Build the module:

```bash
./gradlew :design-system:resources:provider:assemble
```

The generated `Res.string.profile_note_archive_title` is now available.

### 4. Use in Composables

```kotlin
Text(
    text = AppTokens.strings.res(Res.string.profile_note_archive_title),
    style = AppTokens.typography.h2(),
)

// with format args
Text(
    text = AppTokens.strings.res(Res.string.profile_note_archive_subtitle, days),
)
```

### 5. Use in ViewModels

```kotlin
val title = stringProvider.get(Res.string.profile_note_archive_title)
val subtitle = stringProvider.get(Res.string.profile_note_archive_subtitle, days)
```

### 6. Use in State

```kotlin
@Immutable
internal data class ProfileNoteArchiveState(
    val title: UiText = UiText.Res(Res.string.profile_note_archive_title),
    val subtitle: UiText = UiText.Res(
        Res.string.profile_note_archive_subtitle,
        formatArgs = persistentListOf(30),
    ),
)
```

## Add a plural

This template does not ship a `plurals.xml` today (no
`composeResources/values/plurals.xml` exists), so this section is forward-looking — set it up
the first time you need pluralized text.

### 1. Create `plurals.xml`

Add `values/plurals.xml` for English (+ a copy in each non-English locale folder, e.g.
`values-<locale>/plurals.xml`):

```xml
<plurals name="notes_count">
    <item quantity="one">%d note</item>
    <item quantity="other">%d notes</item>
</plurals>
```

In `values-<locale>/plurals.xml`:

```xml
<plurals name="notes_count">
    <item quantity="one">%d нотатка</item>
    <item quantity="few">%d нотатки</item>
    <item quantity="many">%d нотаток</item>
    <item quantity="other">%d нотатки</item>
</plurals>
```

Languages with four quantity classes use `one|few|many|other` — make sure all four are
present.

### 2. Use

```kotlin
// Composable
val text = pluralStringResource(Res.plurals.notes_count, count, count)

// VM
val text = stringProvider.plural(Res.plurals.notes_count, count, count)
```

`StringProvider.plural` is already declared on the interface in
`:design-system:resources:provider`; the impl in `:design-system:resources:provider-impl`
calls `getPluralString(...)` from Compose Resources.

## Add a drawable (raster illustration)

Use this for raster artwork (illustrations, photos, hero images). For monochrome UI icons, see
**Add an icon** below — the template authors them as `ImageVector` code, not drawables.

### 1. Drop the file

```
:design-system:resources:provider/src/commonMain/composeResources/drawable/
  dumbbell.webp
  empty_notifications.webp
```

### 2. Naming

- `snake_case` lowercase, **bare noun** describing the asset (e.g. `note`, `tag`,
  `empty_inbox`, ...). This template does NOT use `ic_*` / `img_*` / `bg_*` prefixes.
- Prefer `.webp` (the format actually used in `composeResources/drawable/`). PNG only when
  truly needed; the repo currently ships no vector drawables in this folder.

### 3. Compose Resources regenerates `Res.drawable.<key>`

```bash
./gradlew :design-system:resources:provider:assemble
```

### 4. Use

```kotlin
Image(
    painter = AppTokens.drawables.res(Res.drawable.empty_notifications),
    contentDescription = null,
)
```

`AppTokens.drawables.res(...)` returns a `Painter` via Compose Resources `painterResource`.
There is no public `AppTokens.dp.icon` group — for icon sizing, use the closest contextual
group (`AppTokens.dp.button.*`, `AppTokens.dp.input.*`, …) or take size from the consuming
widget's tokens.

## Add an icon (ImageVector)

Icons are authored as Kotlin `ImageVector` extension properties on the `AppIcon` object,
**not** as drawables. This keeps tint, sizing, and theming consistent with the rest of the
design system.

### 1. Create the file

```
:design-system:resources:provider/src/commonMain/kotlin/com/<org>/<product>/design/resources/provider/icons/
  Notifications.kt
```

### 1b. Source the path data

`ImageVector.Builder { path { ... } }` needs raw SVG path commands. The recipe does NOT cover
where to get them — that's a per-project decision. Common sources:

- **Public icon library transcribed by hand** — feather-icons (MIT), hero-icons (MIT),
  tabler-icons (MIT). The path data is in the library's SVG files; transcribe to the Kotlin
  DSL.
- **Designer SVG export** — designer drops an SVG in `figma/svg-export/<Name>.svg` (or
  equivalent), the builder transcribes path commands.
- **Material as a fallback when no source is task-specified** — search
  `androidx.compose.material.icons.Icons.*` for a close-enough match and copy its
  `materialPath { ... }` commands into the new `AppIcon.<Name>`. Do NOT `import` the Material
  icon to use it directly — `AppIcon` stays the single icon hierarchy in this design system.
  If nothing matches, hand-roll path data from scratch (`moveTo` / `curveTo` / `lineTo`).

> **Gotcha — figma `figma-export` JSON is not a source.** Tools that export figma components to
> `figma/figma-export/components/<Name>/variants/Size-*.json` carry metadata only (`width`,
> `height`, `type: "VECTOR"`, `fills`). They do NOT include `pathData`/`d=...`. Treat
> figma-export JSON as a name-and-size manifest, not as path source.

### 2. Author the `ImageVector`

```kotlin
package com.<org>.<product>.design.resources.provider.icons

import androidx.compose.ui.graphics.vector.ImageVector
import com.<org>.<product>.design.resources.provider.AppIcon

public val AppIcon.Notifications: ImageVector
    get() {
        if (_Notifications != null) return _Notifications!!
        _Notifications = ImageVector.Builder(/* … */).apply { /* paths */ }.build()
        return _Notifications!!
    }

@Suppress("ObjectPropertyName")
private var _Notifications: ImageVector? = null
```

**Viewport convention.** Default to `viewportWidth = 24f, viewportHeight = 24f` — matches
Material and feather/hero stock viewports, scales correctly when consumers pass
`Modifier.size(16.dp)` or `Modifier.size(48.dp)`. Per-size variants (`AppIcon.<Name>16`,
`AppIcon.<Name>32`) are possible when designer stroke-tuning differs per size, but are NOT the
default — add them only when a specific icon visibly degrades at a specific size.

Naming: `PascalCase`, semantic noun (`ArrowLeft`, `Cancel`, `User`, `Notifications`). The
cached `_<Name>` backing field is the convention used across the existing icon files.

### 3. Use

```kotlin
Icon(
    imageVector = AppTokens.icons.Notifications,
    contentDescription = null,
    tint = AppTokens.colors.icon.primary,
)
```

### 4. (Optional) Catalog preview when adding a bucket

When `resource-builder` adds more than ~5 icons in one task, author one extra
`@AppPreview`-annotated composable in `:design-system:preview` that grids all newly-added
`AppTokens.icons.*` slots at 24dp. Designers use it to spot-check the bucket without opening
every consuming widget; the cost is one ~50-LoC file per bucket.

Place under
`design-system/preview/src/commonMain/kotlin/com/<org>/<product>/design/preview/IconCatalogPreview.kt`
(or wherever the project's preview-catalog files live).

## Add a font

### 1. Drop the file

```
:design-system:resources:provider/src/commonMain/composeResources/font/
  manrope_extra_light.ttf
  manrope_light.ttf
  manrope_regular.ttf
  manrope_medium.ttf
  manrope_semi_bold.ttf
  manrope_bold.ttf
  manrope_extra_bold.ttf
```

### 2. Naming

`<typeface>_<weight>.ttf`, where `<typeface>` is the `typefaceFactory` value from
`orchestrator/project-config.md` (requirements).

### 3. Wire into `AppTypography`

In `:design-system:resources:provider/AppTypography.kt` (a `private typealias AppFont = Res.font`
at the top of the file gives the short accessor):

```kotlin
@Composable
internal fun <typeface>(): FontFamily = FontFamily(
    Font(AppFont.<typeface>_bold, weight = FontWeight.Bold),
    Font(AppFont.<typeface>_extra_bold, weight = FontWeight.ExtraBold),
    Font(AppFont.<typeface>_extra_light, weight = FontWeight.ExtraLight),
    Font(AppFont.<typeface>_light, weight = FontWeight.Light),
    Font(AppFont.<typeface>_medium, weight = FontWeight.Medium),
    Font(AppFont.<typeface>_regular, weight = FontWeight.Normal),
    Font(AppFont.<typeface>_semi_bold, weight = FontWeight.SemiBold),
)
```

`AppFont.<typeface>_bold` resolves to `Res.font.<typeface>_bold`, the accessor Compose
Resources generates from the file `font/<typeface>_bold.ttf`. Only `<typeface>()` decides
which weights are visible to text styles, so register every weight you actually use in
`AppTypography`.

### 4. Use in `AppTypography` tokens

```kotlin
@Composable
public fun h1(): TextStyle = TextStyle(
    fontSize = 30.sp,
    fontFamily = <typeface>(),
    fontWeight = FontWeight.Bold,
    lineHeight = 38.sp,
)
```

## Verify

```bash
./gradlew :design-system:resources:provider:assemble
./gradlew :androidApp:assembleDebug
```

All resource modules must compile and the generated accessors (`Res.string.*`,
`Res.drawable.*`, `Res.font.*`) must resolve.

## Rules

- **All string keys `snake_case`** with semantic prefixes (`error_*`, `profile_*`,
  `<feature>_*`).
- **Every string key has a value in every locale.** Missing values cause unexpected English in
  non-English UI.
- **Positional placeholders** (`%1$s`, `%2$d`). Never positional indices implicit.
- **Format types** match between locales — if `%1$s` is a string in `values/`, it must be
  `%1$s` in `values-<locale>/`. Don't swap to `%1$d`.
- **Drawables are `.webp` (or other raster), named as a bare noun.** Icons live in code as
  `ImageVector` extensions on `AppIcon`, not as drawables.
- **Single resource per file** (one `<noun>.webp`, one `Icon.kt`).

## Anti-patterns

- **Hardcoded strings** in Composables. Always extract.
- **Single-locale features.** Add every key to every `supportedLocales` file; use an English
  fallback only when the task explicitly authorizes it.
- **Resource keys in feature modules.** Centralize in `:design-system:resources:provider`.
- **`stringResource(R.string.foo)`** from `androidx.compose.ui.res`. Android-only.
- **Adding monochrome icons as drawables** (`ic_*.xml` / `ic_*.svg`). Author them as
  `ImageVector` extensions on `AppIcon` so tint, sizing, and previews stay consistent.
- **`ic_*` / `img_*` / `bg_*` prefixes** on raster files. This template uses bare nouns
  (`note.webp`, `tag.webp`, …).
- **Font weight not registered in `<typeface>()`** — calls to that weight render the default
  (often "Regular").
