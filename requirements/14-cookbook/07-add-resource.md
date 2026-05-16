# Add a Resource (String, Drawable, Font)

> **Concrete example.** The example task and identifiers below (`Note archive`, `tag-picker`, etc.) are illustrative; the recipe steps apply to any feature you build with this template.

How to add new entries in `:design-system:resources:provider`.

## Add a string

### 1. Add the key + English value

In `:design-system:resources:provider/src/commonMain/composeResources/values/strings.xml`:

```xml
<string name="profile_note_archive_title">Note archive</string>
<string name="profile_note_archive_subtitle">Last %1$d days</string>
```

### 2. Add translations

In `values-uk/strings.xml`:

```xml
<string name="profile_note_archive_title">Архів нотаток</string>
<string name="profile_note_archive_subtitle">Останні %1$d днів</string>
```

In `values-ru/strings.xml` (or your other locales):

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
internal data class State(
    val title: UiText = UiText.Res(Res.string.profile_note_archive_title),
    val subtitle: UiText = UiText.Res(
        Res.string.profile_note_archive_subtitle,
        formatArgs = persistentListOf(30),
    ),
)
```

## Add a plural

The reference repo does not ship a `plurals.xml` today (no `composeResources/values/plurals.xml` exists), so this section is forward-looking — set it up the first time you need pluralized text.

### 1. Create `plurals.xml`

Add `values/plurals.xml` (+ a copy in each locale folder, e.g. `values-uk/plurals.xml`):

```xml
<plurals name="notes_count">
    <item quantity="one">%d note</item>
    <item quantity="other">%d notes</item>
</plurals>
```

In `values-uk/plurals.xml`:

```xml
<plurals name="notes_count">
    <item quantity="one">%d нотатка</item>
    <item quantity="few">%d нотатки</item>
    <item quantity="many">%d нотаток</item>
    <item quantity="other">%d нотатки</item>
</plurals>
```

Slavic languages use `one|few|many|other` — make sure all four are present.

### 2. Use

```kotlin
// Composable
val text = pluralStringResource(Res.plurals.notes_count, count, count)

// VM
val text = stringProvider.plural(Res.plurals.notes_count, count, count)
```

`StringProvider.plural` is already declared on the interface in `:design-system:resources:provider`; the impl in `:design-system:resources:provider-impl` calls `getPluralString(...)` from Compose Resources.

## Add a drawable (raster illustration)

Use this for raster artwork (illustrations, photos, hero images). For monochrome UI icons, see **Add an icon** below — the reference repo authors them as `ImageVector` code, not drawables.

### 1. Drop the file

```
:design-system:resources:provider/src/commonMain/composeResources/drawable/
  dumbbell.webp
  empty_notifications.webp
```

### 2. Naming

- `snake_case` lowercase, **bare noun** describing the asset (e.g. `note`, `tag`, `empty_inbox`, ...). The reference repo does NOT use `ic_*` / `img_*` / `bg_*` prefixes.
- Prefer `.webp` (the format actually used in `composeResources/drawable/`). PNG only when truly needed; the repo currently ships no vector drawables in this folder.

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

`AppTokens.drawables.res(...)` returns a `Painter` via Compose Resources `painterResource`. There is no public `AppTokens.dp.icon` group — for icon sizing, use the closest contextual group (`AppTokens.dp.button.*`, `AppTokens.dp.input.*`, …) or take size from the consuming widget's tokens.

## Add an icon (ImageVector)

Icons are authored as Kotlin `ImageVector` extension properties on the `AppIcon` object, **not** as drawables. This keeps tint, sizing, and theming consistent with the rest of the design system.

### 1. Create the file

```
:design-system:resources:provider/src/commonMain/kotlin/com/<org>/<product>/design.resources/provider/icons/
  Notifications.kt
```

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

Naming: `PascalCase`, semantic noun (`ArrowLeft`, `Cancel`, `User`, `Notifications`). The cached `_<Name>` backing field is the convention used across the existing icon files.

### 3. Use

```kotlin
Icon(
    imageVector = AppTokens.icons.Notifications,
    contentDescription = null,
    tint = AppTokens.colors.icon.primary,
)
```

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

`<family>_<weight>.ttf`. The reference repo uses Manrope; replace with your product's typeface.

### 3. Wire into `AppTypography`

In `:design-system:resources:provider/AppTypography.kt` (a `private typealias AppFont = Res.font` at the top of the file gives the short accessor):

```kotlin
@Composable
internal fun manrope(): FontFamily = FontFamily(
    Font(AppFont.manrope_bold, weight = FontWeight.Bold),
    Font(AppFont.manrope_extra_bold, weight = FontWeight.ExtraBold),
    Font(AppFont.manrope_extra_light, weight = FontWeight.ExtraLight),
    Font(AppFont.manrope_light, weight = FontWeight.Light),
    Font(AppFont.manrope_medium, weight = FontWeight.Medium),
    Font(AppFont.manrope_regular, weight = FontWeight.Normal),
    Font(AppFont.manrope_semi_bold, weight = FontWeight.SemiBold),
)
```

`AppFont.manrope_bold` resolves to `Res.font.manrope_bold`, the accessor Compose Resources generates from the file `font/manrope_bold.ttf`. The reference repo ships all seven weights above — only `manrope()` itself decides which are visible to text styles, so register every weight you actually use in `AppTypography`.

### 4. Use in `AppTypography` tokens

```kotlin
@Composable
public fun h1(): TextStyle = TextStyle(
    fontSize = 30.sp,
    fontFamily = manrope(),
    fontWeight = FontWeight.Bold,
    lineHeight = 38.sp,
)
```

## Rules

- **All string keys `snake_case`** with semantic prefixes (`error_*`, `profile_*`, `<feature>_*`).
- **Every string key has a value in every locale.** Missing values cause unexpected English in non-English UI.
- **Positional placeholders** (`%1$s`, `%2$d`). Never positional indices implicit.
- **Format types** match between locales — if `%1$s` is a string in `values/`, it must be `%1$s` in `values-uk/`. Don't swap to `%1$d`.
- **Drawables are `.webp` (or other raster), named as a bare noun.** Icons live in code as `ImageVector` extensions on `AppIcon`, not as drawables.
- **Single resource per file** (one `<noun>.webp`, one `Icon.kt`).

## Anti-patterns

- **Hardcoded strings** in Composables. Always extract.
- **Single-locale features.** Add at least English; translators handle other locales later.
- **Resource keys in feature modules.** Centralize in `:design-system:resources:provider`.
- **`stringResource(R.string.foo)`** from `androidx.compose.ui.res`. Android-only.
- **Adding monochrome icons as drawables** (`ic_*.xml` / `ic_*.svg`). Author them as `ImageVector` extensions on `AppIcon` so tint, sizing, and previews stay consistent.
- **`ic_*` / `img_*` / `bg_*` prefixes** on raster files. The reference repo uses bare nouns (`note.webp`, `tag.webp`, …).
- **Font weight not registered in `manrope()`** — calls to that weight render the default (often "Regular").
