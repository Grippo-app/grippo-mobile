# Packages

## Base scheme

```
com.<org>.<product>.<area>.<feature>[.<subscreen>]
```

- **`<org>`** — organization root (e.g. `acme`).
- **`<product>`** — product name (e.g. `pulse`). Skip `<org>` for single-org projects; use `com.<product>.*`.
- **`<area>`** — broad layer (e.g. `data.services.backend`, `profile`, `core.foundation`).
- **`<feature>`** — narrower scope.
- **`<subscreen>`** — specific MVI package.

## Examples

| Module | Package |
|---|---|
| `:ui-screen-features:profile` (body sub-screen) | `com.<org>.<product>.profile.body` |
| `:ui-screen-features:trainings` (list sub-screen) | `com.<org>.<product>.trainings.list` |
| `:ui-dialog-features:weight-picker` | `com.<org>.<product>.weight.picker` |
| `:ui-core:foundation` | `com.<org>.<product>.core.foundation` |
| `:ui-core:state` | `com.<org>.<product>.core.state` |
| `:data-features:feature-api` (training types) | `com.<org>.<product>.data.features.api.training` |
| `:data-features:trainings` (repo impl) | `com.<org>.<product>.data.features.trainings.data` |
| `:data-features:trainings` (interfaces) | `com.<org>.<product>.data.features.trainings.domain` |
| `:data-services:backend` (DTOs) | `com.<org>.<product>.services.backend.dto.training` |
| `:data-services:database` (Entities) | `com.<org>.<product>.services.database.entity` |
| `:data-services:database` (DAOs) | `com.<org>.<product>.services.database.dao` |
| `:data-services:database` (Packs) | `com.<org>.<product>.services.database.models` |
| `:data-services:database` (Migrations) | `com.<org>.<product>.services.database.migrations` |
| `:data-mappers:dto-to-entity` (training mappers) | `com.<org>.<product>.dto.entity.training` |
| `:data-mappers:entity-to-domain` (training mappers) | `com.<org>.<product>.entity.domain.training` |
| `:data-mappers:domain-to-state` (training mappers) | `com.<org>.<product>.domain.state.training` |
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

## Dotted directory names — legacy

The reference repo has some modules where the **directory** name contains dots:

```
:data-features/trainings/src/commonMain/kotlin/com/grippo/data.features.trainings/
:data-mappers/dto-to-entity/src/commonMain/kotlin/com/grippo/dto.entity.training/
:ui-dialog-features/dialog-api/src/commonMain/kotlin/com/grippo/dialog.api/
:ui-dialog-features/weight-picker/src/commonMain/kotlin/com/grippo/weight.picker/
```

Inside, the `package` declaration is regular: `package com.grippo.data.features.trainings`. The dotted directory is **not** standard Kotlin layout — it's a packaging convention.

**Rules:**

- **Existing legacy modules**: keep the dotted directories. Don't refactor.
- **New modules in the same group**: follow the same convention for consistency.
- **New modules in a fresh project**: dot-free directories are fine (`com/grippo/datatrainings/`). Pick one style per module group.

This document treats the **non-dotted** style as the recommended default for new projects.

## `internal` sub-packages

When a module has both public API and internal helpers:

```
com.<org>.<product>.toolkit.http/
  HttpModule.kt                    // public
  internal/
    ApiErrorParser.kt              // internal
    DefaultResponseValidator.kt    // internal
```

The `internal/` sub-package is **convention only** — it doesn't add code-level enforcement (Kotlin doesn't have package-private). But it signals to readers "don't import from here outside this module".

## Per-feature MVI package

Every screen's seven files live in their own package:

```
:ui-screen-features/profile/src/commonMain/kotlin/com/<org>/<product>/profile/
  ProfileRootComponent.kt
  ProfileRootScreen.kt
  body/                             // sub-screen package
    ProfileBodyComponent.kt
    ProfileBodyContract.kt
    ProfileBodyState.kt
    ProfileBodyDirection.kt
    ProfileBodyLoader.kt
    ProfileBodyViewModel.kt
    ProfileBodyScreen.kt
  settings/
    ProfileSettingsComponent.kt
    // ... seven files
```

The package matches the sub-screen folder name (lowercase).

## DTO / Entity / Mapper area sub-packages

Within `:data-services:backend/dto/`, group by **business area**:

```
dto/
  auth/
    TokenResponse.kt
    EmailAuthBody.kt
    GoogleBody.kt
    AppleBody.kt
  training/
    TrainingResponse.kt
    ExerciseResponse.kt
    IterationResponse.kt
    TrainingBody.kt
  user/
    UserResponse.kt
    CreateProfileBody.kt
```

The same scheme applies to:
- `services/database/entity/<area>/`
- `services/database/dao/<area>/`
- `services/database/models/<area>/`

`:data-mappers/*` modules also have area sub-packages: `dto.entity.training/`, `entity.domain.training/`, `domain.state.training/`.

## Naming the `<area>`

Match the backend's API section if there is one (e.g. backend's `Auth service` → `auth/`). Otherwise, pick a noun that names the data domain (e.g. `training/`, `goal/`, `weight/`).

When data spans multiple areas (e.g. weight history connects User and Weight), pick the **primary** area (`weight/`) and reference others via foreign keys.

## Anti-patterns

- **Single mega-package**: `com.<org>.<product>` with 200 files. Subdivide.
- **`util/` package** for orphan helpers. Either expand a meaningful package or fold into the consumer.
- **Package names with abbreviations** that aren't common: `cust` for `customer`, `xfr` for `transfer`. Spell it out.
- **Mixing camelCase and snake_case in directories**. Pick lowercase + dots-or-no-dots and stick with it.
- **`com.example.*`** in production code. Replace with the real `com.<org>.<product>`.
- **Importing `com.<org>.<product>.<otherFeature>.*` from a different feature module** — feature modules don't share packages. Use `:ui-screen-features:screen-api` for cross-feature contracts.
