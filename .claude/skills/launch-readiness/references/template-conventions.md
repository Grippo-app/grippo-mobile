# Template Conventions

This template is project-agnostic; this file is the
**single source of truth** for placeholder names. Code blocks, example names, and
module paths in references MUST use the placeholders defined here.

## Illustrative domain in examples

Code blocks throughout these docs use `Note`, `Tag`, `User`, `NoteEntity`,
`TagResponse`, `NoteDao`, `NotePack`, etc. as an **illustrative `<Entity>` /
`<RelatedEntity>` domain** — they are not part of the required architecture.
Substitute identifiers from your own product domain. Only the abstract types
(`<Feature>Component`, `<Feature>ViewModel`, `<Product>Api`, `<Entity>Entity`,
etc.) are mandatory.

## 1. Slot placeholders

Vary 1:1 with the per-project bootstrap. Each project edits these in
[`project-config.md`](project-config.md) once; references use the placeholder form.

Slot placeholders are valid **anywhere** a project-specific identifier would
otherwise appear — prose, code blocks, file paths, and **markdown headings**. A
heading like `## How <Product>Api uses it` or `### <Name>State.kt` is canonical,
not a leak. Use the literal substitution-token shape (`<CamelCase>` or
`<kebab-case>` matching the list below); free-form prose tokens like `<TODO>`,
`<the user's product>`, or `<fill in>` are not placeholders — they signal an
unfinished sentence and must be removed or replaced.

| Placeholder | Substitution at bootstrap |
|---|---|
| `<Product>` | The product's PascalCase name (e.g. `SampleApp`) |
| `<product>` | Kebab/lowercase form (e.g. `sampleapp`) |
| `<org>` | Org segment of the Android namespace (e.g. `example`) |
| `<product-domain>` | Backend host (e.g. `api.example.com`) |
| `com.<org>.<product>` | Android namespace root (e.g. `com.example.sampleapp`) |
| `com.<org>.<product>.<area>` | Sub-package |
| `<product>-mobile` | Repo / Gradle root project name |
| `<oauth-server-client-id>` | Google OAuth server client ID (from Google Cloud Console, used in `manifestPlaceholders`; build-file-only — set directly in the Gradle app-module files, not a config field) |
| `<typeface>` | Typeface factory function name; used by `resource-builder` when registering fonts (maps to `typefaceFactory` in project config). |
| `<iosFrameworkName>` | iOS shared-framework name (maps to `iosFrameworkName`; default `shared`). |
| `<figma-library-url>` | Figma design-library file URL (maps to `figmaLibraryUrl`; only when `figmaEnabled`). |
| `<Name>` | Local artifact name within a feature or resource family (screen/dialog/DTO/icon; e.g. `NoteEditor`, `NoteResponse`, `QuestionCircle`) |
| `<X>` / `<x>` | Feature-area name, PascalCase / lowercase (e.g. `Notes` / `notes`) |
| `<Entity>` / `<RelatedEntity>` | Abstract illustrative domain type names used in examples (e.g. `Note` / `Tag`) |
| `<Feature>` / `<feature>` | Feature name in PascalCase / lowercase or kebab form, chosen by the generated project |
| `<area>` | Layer or API area segment (e.g. `auth`, `notes`, `data.services.backend`) |
| `<IosFrameworkName>` | PascalCase Gradle task segment derived from `iosFrameworkName` (e.g. `Shared` for `assembleSharedDebugXCFramework`) |

Use these wherever the value is project-level identity:

- `<Product>Api` — the flat backend client class.
- `https://<product-domain>` — the backend base URL.
- `package com.<org>.<product>.services.backend.dto.<area>`
- `applicationId = "com.<org>.<product>"`

## 2. Canonical example types

When a code block illustrates a pattern (Repository, Mapper, Entity, Feature,
Screen, Dialog), use one of these abstract types instead of inventing names.

| Type | Use when |
|---|---|
| `Note` | Primary list/detail entity for the bulk of pattern examples |
| `Tag` | Secondary related entity (illustrates `@Relation`, joins) |
| `Item` | Third generic entity if genuinely needed |
| `User` | Auth/profile-flavored entity when the pattern genuinely involves the signed-in user (sessions, tokens, foreign keys) |

A pattern walkthrough must pick **one** primary type and use it consistently
end-to-end (DTO → Entity → Pack → Repository → Feature → Screen). Mix `Note` +
`Tag` only when the pattern itself involves a relationship.

Class-name fan-out for the primary type `Note`:

- DTO: `NoteResponse`, `NoteBody`
- Entity: `NoteEntity`, `NoteDao`, `NotePack`
- Domain: `Note`, `NoteRepository`, `NoteFeature`, `NoteRepositoryImpl`, `NoteFeatureImpl`
- DI: `NotesFeatureModule`
- UI: `NotesRouter`, `NotesComponent` (or `NotesRootComponent` if the feature has a same-name sub-screen), `NotesState`, `NotesContract`, `NotesDirection`, `NotesLoader`, `NotesViewModel`, `NotesScreen`
- Sub-screens: `NoteDetail*`, `NoteArchive* (within :notes feature; the canonical worked example uses ProfileNoteArchive* in :profile)`, `NoteEditor*`

For the secondary type `Tag`: `TagResponse`, `TagEntity`, `TagDao`, `Tag`,
`TagRepository`, etc. Used in `@Embedded`/`@Relation` examples (`NotePack`
containing `List<TagEntity>`).

## 3. Canonical worked example

The cross-file worked example is **"Note archive"** — adds a
`ProfileNoteArchiveScreen` to `:ui-screen-features:profile`, with `NoteFeature`
from `:data-features:feature-api`.

- Route: `ProfileRouter.NoteArchive(initialRange: DateRange)`
- Component: `ProfileNoteArchiveComponent`
- ViewModel: `ProfileNoteArchiveViewModel`
- Files: `ProfileNoteArchive{Component, ViewModel, Contract, State, Direction, Loader, Screen}.kt`
- Cross-feature entry: from `:ui-screen-features:home` (`HomeRootComponent` exposes `toProfileNoteArchive: (DateRange) -> Unit`).

Appears in:

- the add-screen and add-cross-feature-nav cookbook recipes (in the ui-feature skill)

## 4. Format-state policy

Three buckets:

### 4.1 Required infrastructure — keep, generic

Every project has these; they wrap product-neutral values:

- `EmailFormatState` — sealed `Empty | Invalid(value) | Valid(value)`, regex-validated.
- `PasswordFormatState` — sealed `Empty | Invalid(value, reason) | Valid(value)`, with `@Composable hint(): String`.
- `NameFormatState` — sealed `Empty | Invalid(value) | Valid(value)`.
- `DateFormatState`, `DateTimeFormatState`, `DateRangeFormatState` — date/time wrappers.

Reference these by name in reference examples.

### 4.2 Generic numeric example

For references that illustrate "numeric field with units, sealed Empty/Invalid/Valid",
use one generic class:

- `AmountFormatState(value: Double?, unit: String?)` — covers the pattern without leaking a product unit.

Don't introduce additional generic numeric variants; one is enough to teach the
pattern.

### 4.3 Product-specific `*FormatState` — define per project

Product-specific format states (e.g. weight, height, duration, volume,
percentage, intensity) are **per-project** — define them in `:ui-core:state` when
the product needs them, following the same `Empty | Invalid | Valid` sealed shape
as the generic `AmountFormatState` in §4.2. References MUST NOT list them as if they
were infrastructure.

## 5. Module list policy

References MUST NOT enumerate a fixed set of product features. Specifically:

- **the data-layer skill, references/module-structure.md** — data-feature module list
  is per-project. Each module is `:data-features:<feature>` and follows the
  pattern in that reference.
- **the data-layer skill, references/persistence-room.md** — `@Database(entities = [...])` uses
  placeholder list `[<Entity>Entity, <RelatedEntity>Entity, ...]`.
- **the ui-feature skill, references/module-structure.md** — concrete feature list is
  per-project; keep at most two as illustrative.
- **the design-system skill, references/design-system-modules.md** — specialized
  design-system components use the generic names in §6 below.

## 6. Generic component name examples

When references illustrate "specialized design-system component built on
primitives" (charts, heatmaps, complex cards):

- `<Entity>Chart`, `<Entity>Timeline`, `<Entity>Heatmap`, `<Entity>SummaryCard`, `<Entity>HistoryCard`.

E.g. `NoteTimelineChart`, `TagSummaryCard`. Don't invent unique example names —
the pattern is what teaches, not the name.

## 7. Reserved names

These are infrastructure-stable across projects. Do NOT substitute:

- Base classes: `BaseViewModel`, `BaseComponent`, `BaseComposeScreen`, `BaseDirection`, `BaseLoader`, `BaseRouter`, `BaseResult`, `ComponentIdentifier`, `NoneIdentifier`.
- Infrastructure: `OperationManager`, `ResultManager`, `ResultEmitter`, `ResultKey`, `ResultKeys`, `Processing`.
- Design tokens: `AppTokens`, `AppTheme`, `AppColor`, `AppDp`, `AppTypography`, `AppString`, `AppDrawable`, `AppIcon`, `AppPreview`, `PreviewContainer`, `AppLocale`.
- Resources: `StringProvider`, `UiText`, `Format*State` (the marker — instances are policy-bucketed in §4).
- Errors: `AppError`, `ErrorProvider`, `AppErrorState`.
- Dialogs: `DialogConfig`, `DialogController`, `DialogComponent`, `DialogProvider`.
- Toolkit: `AppLogger`, `DateTimeUtils`, `DateRange`, `DateRangeKind`, `DateRangePresets`, `DateFormat`, `DateFormatting`, `NativeContext`, `Connectivity`, `NotificationManager`, `PermissionManager`, `LinkOpener`.
