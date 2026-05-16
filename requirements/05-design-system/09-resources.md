# Compose Multiplatform Resources

Strings, drawables, plurals, and fonts are managed via **Compose Multiplatform Resources** — `Res.string.*`, `Res.drawable.*`, `Res.plurals.*`, `Res.font.*`.

All resource files live in `:design-system:resources:provider/src/commonMain/composeResources/`:

```
:design-system:resources:provider/
  src/commonMain/composeResources/
    values/strings.xml              # default (English)
    values-uk/strings.xml           # Ukrainian
    values-ru/strings.xml           # Russian
    drawable/                       # raster (.webp) / vector drawables
    font/                           # font files (Manrope variants)
```

The Compose Resources plugin generates a `Res` object at build time. Resources are accessed as `Res.string.<key>`, `Res.drawable.<key>`, `Res.font.<key>`. The reference repo does not currently ship a `values/plurals.xml` — add one (with matching `values-XX/plurals.xml` per locale) only when a feature actually needs plural-aware strings.

## `strings.xml`

Standard Android-style XML, but processed by Compose Resources (so the file is multiplatform):

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="note_title">Note</string>
    <string name="amount_value_kg">%1$.1f kg</string>
    <string name="error_no_internet_title">No internet connection</string>
    <string name="error_no_internet_description">Check your connection and try again.</string>
    <string name="welcome_user">Welcome, %1$s</string>
</resources>
```

Per-locale variants live in `values-XX/strings.xml`:

```xml
<!-- values-uk/strings.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="note_title">Нотатка</string>
    <string name="amount_value_kg">%1$.1f кг</string>
    <!-- ... -->
</resources>
```

### Conventions

- **All keys `snake_case`** with a domain prefix when ambiguous: `note_title` (domain `note`), `error_no_internet_title` (domain `error`).
- **Descriptive, never numbered.** `title1`/`subtitle2` is forbidden.
- **Format args use positional placeholders**: `%1$s`, `%2$d`, `%3$.1f`. Order is **locale-stable** — different languages can rearrange placeholders.
- **No HTML or rich text** in `strings.xml`. If a string needs bold/italic spans, render it via `AnnotatedString` at the call site.

### Resource scope

Strings should be **stable enough to translate**. Adding `error_42_title` and removing it in the next sprint wastes translator effort. If a string is truly ephemeral (e.g. an in-progress feature flag), keep it English-only initially and translate when the feature stabilizes.

## `plurals.xml`

Not present in the reference repo yet. When a plural-aware string is needed, add `values/plurals.xml` (and a matching variant in every `values-XX/`) following the standard Compose Resources / Android shape:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <plurals name="notes_count">
        <item quantity="one">%d note</item>
        <item quantity="other">%d notes</item>
    </plurals>
</resources>
```

Per-locale variants must include the locale's quantity classes:

- English: `one`, `other`.
- Slavic (Russian, Ukrainian): `one`, `few`, `many`, `other`.

Access (once a plural is defined):

```kotlin
// VM
val message = stringProvider.plural(Res.plurals.notes_count, count, count)

// Composable
val message = pluralStringResource(Res.plurals.notes_count, count, count)
```

`StringProvider.plural(...)` is already declared in `:design-system:resources:provider` (and implemented in `:design-system:resources:provider-impl`) so VM-side usage works as soon as the XML is added.

## Drawables

```
:design-system:resources:provider/
  src/commonMain/composeResources/
    drawable/
      note_thumbnail.webp         # raster product imagery
      tag_badge.webp
      empty_state.webp
      welcome_hero.webp
      // ... add vector / .xml drawables here if needed
```

### Conventions

- **`snake_case` bare nouns** (`note_thumbnail`, `tag_badge`, `empty_state`). The reference repo keeps drawables suffix-less because line-art **icons** live as `ImageVector` extension properties under `icons/` (see `05-app-strings-drawables-icons.md`), so the `drawable/` bucket is reserved for raster product imagery and decorative illustrations. Add an `img_*` / `bg_*` prefix only if a real naming collision appears.
- **`.webp` is the default** raster format in the reference repo — small payload, good quality. Use SVG / vector `.xml` when an asset needs to scale crisply; PNG only for legacy artwork.
- **One file per resource.** No sprite sheets.

Access:

```kotlin
Image(
    painter = AppTokens.drawables.res(Res.drawable.note_thumbnail),
    contentDescription = null,
)

// For icons (ImageVector), reach them through AppTokens.icons.*:
Icon(
    imageVector = AppTokens.icons.Search,
    contentDescription = null,
    tint = AppTokens.colors.icon.primary,
)
```

## Fonts

```
:design-system:resources:provider/
  src/commonMain/composeResources/
    font/
      manrope_regular.ttf
      manrope_medium.ttf
      manrope_semi_bold.ttf
      manrope_bold.ttf
      manrope_extra_bold.ttf
      manrope_light.ttf
      manrope_extra_light.ttf
```

Wired into `AppTypography` via `manrope()` helper. See `05-design-system/04-app-typography.md`.

## Why everything in one module

`:design-system:resources:provider` is the **single home** for resources. UI feature modules don't carry their own `composeResources/` directories. Why:

- **Translation effort centralized.** Translators see one file per locale.
- **Reference consistency.** Every screen pulls from the same string namespace.
- **No duplicate keys.** A feature module's `Res.string.title` would collide with another's; centralizing prevents this.
- **Build performance.** One module's resource bundle compiles once.

Exception: very large feature modules (a fully internationalized "onboarding flow" of 100 screens) might have their own resource sub-module. The default is **one shared module**.

## Adding a resource

1. Add the key + English value to `values/strings.xml`.
2. Add the same key to **every** `values-XX/strings.xml` with an initial English fallback or the translation.
3. Compose Resources regenerates `Res.string.your_new_key`.
4. Use it: `AppTokens.strings.res(Res.string.your_new_key)` (UI) or `stringProvider.get(Res.string.your_new_key)` (VM).

For drawables/fonts, drop the file into the right directory; Compose Resources picks it up.

## Anti-patterns

- **Hardcoded strings in Composables.** `Text("Welcome")`. Forbidden — extract.
- **Missing keys in some locale files.** Causes the default (English) to render — pollutes Ukrainian/Russian UI with English text. Use a translation-management process to catch.
- **`stringResource(R.string.foo)`** from `androidx.compose.ui.res`. Android-only; breaks iOS build.
- **Resource files in feature modules.** Move them to `:design-system:resources:provider` unless there's a deliberate split.
- **PNG line-art icons.** Use `ImageVector` icons (`:design-system:resources:provider/icons/*.kt` → `AppTokens.icons.*`); reserve the `drawable/` bucket for raster product imagery (currently `.webp`).
- **Format args by name.** Compose Resources uses positional format args (`%1$s`, `%2$d`). Don't try named (`%(name)s`).
