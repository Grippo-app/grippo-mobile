# The Seven Mapper Modules

Conversions between DTOs / Entities / Domain models / UI states / Bodies live in **seven dedicated modules**, one per direction. Each module is isolated — no cross-mapper imports, no shared base class, no DI.

## The seven directions

| Module | From | To | Used by |
|---|---|---|---|
| `:data-mappers:dto-to-entity` | DTO `<X>Response` | Entity `<X>Entity` | Repository caches a server response into Room |
| `:data-mappers:entity-to-domain` | Entity / `<X>Pack` | Domain `<X>` | Repository observes a DAO and emits domain models |
| `:data-mappers:dto-to-domain` | DTO `<X>Response` | Domain `<X>` | Repository skips caching for one-shot reads |
| `:data-mappers:domain-to-state` | Domain `<X>` | UI State (`*State`, `*FormatState`) | ViewModel maps domain into screen state before `update {}` |
| `:data-mappers:state-to-domain` | UI State `*State` | Domain `Set<X>` (submit form) | ViewModel collects form input and builds a domain object to send |
| `:data-mappers:domain-to-entity` | Domain `Draft<X>` (parents) / `Set<X>` (leaf rows) | `Draft<X>Pack` for parents (Note, Tag), bare `Draft<X>Entity` for leaf rows (Item) | ViewModel persists a draft to Room without the API. Ids are client-generated (`Uuid.random()`); each child takes its parent FK as a parameter. |
| `:data-mappers:domain-to-dto` | Domain `Set<X>` | Body `<X>Body` | Repository builds a POST/PUT body |

## Package naming

`com.<org>.<product>.<from>.<to>.<area>`. The mapper modules use **standard slash-separated directories** (unlike `:data-features:*` which use dotted directories):

```
data-mappers/dto-to-entity/src/commonMain/kotlin/com/<org>/<product>/dto/entity/note/
  NoteMapper.kt
  TagMapper.kt
  ItemMapper.kt
data-mappers/entity-to-domain/src/commonMain/kotlin/com/<org>/<product>/entity/domain/note/
  NotePackMapper.kt
  TagMapper.kt
  ItemMapper.kt
data-mappers/domain-to-state/src/commonMain/kotlin/com/<org>/<product>/domain/state/note/
  NoteMapper.kt
```

Package declaration mirrors the directory: `package com.<org>.<product>.dto.entity.note`. Same dot count.

## What lives in each module

Each module contains **only**:

1. Top-level extension functions named `<Source>.toX()`.
2. Optionally, helpers private to a file.

That's it. No classes. No singletons. No DI annotations.

### Example: `:data-mappers:dto-to-entity`

```
data-mappers/dto-to-entity/src/commonMain/kotlin/com/<org>/<product>/dto/entity/
  note/
    NoteMapper.kt
    TagMapper.kt
    ItemMapper.kt
  user/
    UserMapper.kt
  ...
```

### Example file: `NoteMapper.kt` in `:dto-to-entity`

```kotlin
package com.<org>.<product>.dto.entity.note

public fun List<NoteResponse>.toEntities(): List<NoteEntity> =
    mapNotNull { it.toEntityOrNull() }

public fun NoteResponse.toEntityOrNull(): NoteEntity? {
    val entityId = AppLogger.Mapping.log(id) { "NoteResponse.id is null" } ?: return null
    val entityTitle = AppLogger.Mapping.log(title) { "NoteResponse.title is null" } ?: return null
    val entityAmount = AppLogger.Mapping.log(amount) { "NoteResponse.amount is null" } ?: return null
    val entityDuration = AppLogger.Mapping.log(duration) { "NoteResponse.duration is null" } ?: return null
    val entityCreatedAt = AppLogger.Mapping.log(createdAt) { "NoteResponse.createdAt is null" } ?: return null
    val entityUpdatedAt = AppLogger.Mapping.log(updatedAt) { "NoteResponse.updatedAt is null" } ?: return null
    // Server emits either profileId or the legacy userId; either is acceptable.
    val entityProfileId = AppLogger.Mapping.log(profileId ?: userId) { "NoteResponse.profileId is null" } ?: return null

    return NoteEntity(
        id = entityId,
        profileId = entityProfileId,
        title = entityTitle,
        amount = entityAmount,
        duration = entityDuration,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
    )
}
```

Note: local variables are prefixed `entity*` to disambiguate from same-named DTO fields inside the receiver scope.

## Build files

Each mapper module's `build.gradle.kts`:

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.data.mappers.<from>.to.<to>" }

    sourceSets.commonMain.dependencies {
        // direction-specific deps:
        implementation(projects.dataServices.database)     // for Entity (if relevant)
        implementation(projects.dataServices.backend)      // for DTO (if relevant)
        implementation(projects.dataFeatures.featureApi)   // for Domain (if relevant)
        implementation(projects.uiCore.state)              // for State (if relevant)
        implementation(projects.toolkit.logger)            // for AppLogger.Mapping — every module
        implementation(projects.toolkit.dateUtils)         // for DateTimeUtils (most directions)

        implementation(libs.datetime)                      // kotlinx-datetime, most directions
        implementation(libs.immutable.collections)         // state-producing directions
    }
}
```

The dependency set per direction (`:toolkit:logger` is wired into every row's build deps as a convention; **actual call-site usage** is limited to the four directions whose **source** carries nullable values — `:dto-to-entity`, `:dto-to-domain`, `:entity-to-domain` (`@Relation` rows + enum-string parses), and `:state-to-domain` (`*FormatState.value`). The three `:domain-to-*` directions take non-null domain inputs and therefore have **no `AppLogger.Mapping.log` call sites** — the dep is unused but kept for symmetry with the other directions):

| Direction | Source dep | Target dep | Other |
|---|---|---|---|
| `:dto-to-entity` | `:data-services:backend` | `:data-services:database` | `:toolkit:logger` |
| `:entity-to-domain` | `:data-services:database` | `:data-features:feature-api` | `:toolkit:logger`, `:toolkit:date-utils`, `libs.datetime` |
| `:dto-to-domain` | `:data-services:backend` | `:data-features:feature-api` | `:toolkit:logger`, `:toolkit:date-utils`, `libs.datetime` |
| `:domain-to-state` | `:data-features:feature-api` | `:ui-core:state` | `:toolkit:logger`, `:toolkit:date-utils`, `libs.datetime`, `libs.immutable.collections` |
| `:state-to-domain` | `:ui-core:state` | `:data-features:feature-api` | `:toolkit:logger`, `:toolkit:date-utils`, `libs.datetime`, `libs.immutable.collections` |
| `:domain-to-entity` | `:data-features:feature-api` | `:data-services:database` | `:toolkit:logger`, `:toolkit:date-utils`, `libs.datetime` |
| `:domain-to-dto` | `:data-features:feature-api` | `:data-services:backend` | `:toolkit:logger`, `:toolkit:date-utils`, `libs.datetime` |

## Why seven modules

- **Isolation.** A change to `NoteResponse` (DTO) cannot accidentally break the entity-to-domain mapper. They're in different modules.
- **Compile parallelism.** Each module compiles in parallel.
- **Discoverability.** "Where does `NoteResponse → NoteEntity` live?" → look in `:dto-to-entity`. The module name is the answer.
- **Direction-specific rules.** Each direction has slightly different conventions (DTO-sourced directions log every required field; entity-sourced directions log only `@Relation` and enum-string parses; state-sourced directions log each `*FormatState.value`).

## Anti-patterns

- **A new mapper module** for a new direction. The seven are exhaustive. If you genuinely have a new direction (`state-to-entity`, `entity-to-state`), reconsider — you almost certainly want to compose two existing directions.
- **A class that holds a mapper.** Mappers are **functions**.
- **DI for mappers.** No `@Single`, no `@Factory`. Top-level functions are stateless and don't need Koin.
- **Cross-mapper imports.** `:dto-to-entity` imports `:entity-to-domain`. Forbidden. If you need a `DTO → Domain` conversion, use `:dto-to-domain` or compose at the call site (`dto.toEntityOrNull()?.toDomain()`).
- **Business logic in a mapper.** Mappers translate fields. Validation, derivation, side effects belong in `<X>UseCase` or the Repository.
- **Inline mappers in Repository / ViewModel.** Always live in a dedicated module.
- **One big mapper file per module** with everything. Split by `<area>` (note, user, ...) for navigability.
