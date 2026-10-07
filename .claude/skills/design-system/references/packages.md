# Packages

## Base scheme

```
com.<org>.<product>.<area>.<feature>[.<subscreen>]
```

- **`<org>`** — organization root (e.g. `example`).
- **`<product>`** — product name (e.g. `sampleapp`). Skip `<org>` for single-org projects; use
  `com.<product>.*`.
- **`<area>`** — broad layer (e.g. `data.services.backend`, `profile`, `core.foundation`).
- **`<feature>`** — narrower scope.
- **`<subscreen>`** — specific MVI package.

## Examples

| Module | Package |
|---|---|
| `:ui-screen-features:notes` (archive sub-screen) | `com.<org>.<product>.notes.archive` |
| `:ui-screen-features:profile` (settings sub-screen) | `com.<org>.<product>.profile.settings` |
| `:ui-dialog-features:note-picker` | `com.<org>.<product>.note.picker` |
| `:ui-core:foundation` | `com.<org>.<product>.core.foundation` |
| `:ui-core:state` | `com.<org>.<product>.core.state` |
| `:data-features:feature-api` (note types) | `com.<org>.<product>.data.features.api.note` |
| `:data-features:notes` (repo impl) | `com.<org>.<product>.data.features.notes.data` |
| `:data-features:notes` (interfaces) | `com.<org>.<product>.data.features.notes.domain` |
| `:data-services:backend` (DTOs) | `com.<org>.<product>.services.backend.dto.note` |
| `:data-services:database` (Entities) | `com.<org>.<product>.services.database.entity` |
| `:data-services:database` (DAOs) | `com.<org>.<product>.services.database.dao` |
| `:data-services:database` (Packs) | `com.<org>.<product>.services.database.models` |
| `:data-services:database` (Migrations) | `com.<org>.<product>.services.database.migrations` |
| `:data-mappers:dto-to-entity` (note mappers) | `com.<org>.<product>.dto.entity.note` |
| `:data-mappers:entity-to-domain` (note mappers) | `com.<org>.<product>.entity.domain.note` |
| `:data-mappers:domain-to-state` (note mappers) | `com.<org>.<product>.domain.state.note` |
| `:toolkit:logger` | `com.<org>.<product>.toolkit.logger` |
| `:toolkit:date-utils` | `com.<org>.<product>.toolkit.date.utils` |
| `:toolkit:notification-manager` | `com.<org>.<product>.toolkit.local.notification` |
| `:design-system:core` | `com.<org>.<product>.design.core` |
| `:design-system:components` | `com.<org>.<product>.design.components` |
| `:design-system:resources:provider` | `com.<org>.<product>.design.resources.provider` |
| `:design-system:resources:provider-impl` | `com.<org>.<product>.design.resources.provider.impl` |
| `:design-system:preview` | `com.<org>.<product>.design.preview` |
| `:compose-libs:chart` | `com.<org>.<product>.chart.<kind>` (e.g. `.chart.area`, `.chart.pie`) |
| `:compose-libs:konfetti` | `com.<org>.<product>.konfetti.*` |
| `:compose-libs:segment-control` | `com.<org>.<product>.segment.control` |
| `:compose-libs:wheel-picker` | `com.<org>.<product>.wheel.picker` |

## Source-directory layout

Kotlin source directories mirror package components with regular path separators:
`com/<org>/<product>/data/features/<feature>/`. Directory names containing dots
are not part of this template's module convention.

## `internal` sub-packages

When a module has both public API and internal helpers:

```
com.<org>.<product>.toolkit.http.client/
  HttpModule.kt                    // public
  PlatformDriver.kt                // public (expect/actual)
  internal/
    ApiErrorParser.kt              // internal
    ResponseValidator.kt           // internal
```

The `internal/` sub-package is **convention only** — it doesn't add code-level enforcement
(Kotlin doesn't have package-private). But it signals to readers "don't import from here
outside this module".

## Per-feature MVI package

Every screen's seven files live in their own package:

```
:ui-screen-features/notes/src/commonMain/kotlin/com/<org>/<product>/notes/
  NotesComponent.kt                 // feature root — owns the inner stack
  NotesScreen.kt
  NotesState.kt
  NotesContract.kt
  NotesDirection.kt
  NotesLoader.kt
  NotesViewModel.kt
  archive/                          // sub-screen package
    NoteArchiveComponent.kt
    NoteArchiveContract.kt
    NoteArchiveState.kt
    NoteArchiveDirection.kt
    NoteArchiveLoader.kt
    NoteArchiveViewModel.kt
    NoteArchiveScreen.kt
    components/                      // non-*Screen UI — one composable per file
      NoteRow.kt
      ArchiveHeader.kt
  editor/
    NoteEditorComponent.kt
    // ... seven files
```

The package matches the sub-screen folder name (lowercase). A sub-screen's non-`*Screen` UI
(rows, cards, headers, content blocks) lives in a `components/` subfolder inside that package
— one cohesive composable per file. `components/` is a real package segment: a file there
declares `package com.<org>.<product>.<feature>.<subscreen>.components` (the directory segment
maps to a package segment), so the path-matches-package rule still holds. Naming for the
feature-root files varies: most features use the bare feature name (`NotesComponent`,
`ProfileComponent`, `AuthComponent`); a few use the `<Feature>Root*` prefix when the bare
name would clash with a sub-screen of the same name (`HomeRootComponent` coexisting with
`home/HomeComponent`). Pick one style per feature and stick to it.

## DTO / Entity / Mapper area sub-packages

Within `:data-services:backend/dto/`, group by **business area**:

```
dto/
  auth/
    TokenResponse.kt
    EmailAuthBody.kt
    GoogleBody.kt
    AppleBody.kt
  note/
    NoteResponse.kt
    TagResponse.kt
    ItemResponse.kt
    NoteBody.kt
  user/
    UserResponse.kt
    UserBody.kt
```

The same scheme applies to: `services/database/entity/<area>/`,
`services/database/dao/<area>/`, `services/database/models/<area>/`. `:data-mappers/*` modules
also have area sub-packages: `dto.entity.note/`, `entity.domain.note/`, `domain.state.note/`.

## Naming the `<area>`

Match the backend's API section if there is one (e.g. backend's `Auth service` → `auth/`).
Otherwise, pick a noun that names the data domain (e.g. `note/`, `tag/`, `user/`). When data
spans multiple areas (e.g. a note connects User and Tag), pick the **primary** area (`note/`)
and reference others via foreign keys.

## Anti-patterns

- **Single mega-package**: `com.<org>.<product>` with 200 files. Subdivide.
- **`util/` package** for orphan helpers. Either expand a meaningful package or fold into the
  consumer.
- **Package names with abbreviations** that aren't common: `cust` for `customer`. Spell it
  out.
- **Mixing camelCase and snake_case in directories**. Pick lowercase + dots-or-no-dots and
  stick with it.
- **`com.example.*`** in production code. Replace with the real `com.<org>.<product>`.
- **Importing `com.<org>.<product>.<otherFeature>.*` from a different feature module** —
  feature modules don't share packages. Use `:ui-screen-features:screen-api` for cross-feature
  contracts.
