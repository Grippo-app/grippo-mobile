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
| `:data-mappers:domain-to-entity` | Domain `Draft<X>` | Entity `Draft<X>Entity` / `Draft<X>Pack` | ViewModel persists a draft to Room without the API |
| `:data-mappers:domain-to-dto` | Domain `Set<X>` | Body `<X>Body` | Repository builds a POST/PUT body |

## Package naming

`com.<org>.<product>.<from>.<to>.<area>`. The mapper modules use **standard slash-separated directories** (unlike `:data-features:*` which use dotted directories):

```
data-mappers/dto-to-entity/src/commonMain/kotlin/com/<org>/<product>/dto/entity/training/
  TrainingMapper.kt
  ExerciseMapper.kt
  IterationMapper.kt
data-mappers/entity-to-domain/src/commonMain/kotlin/com/<org>/<product>/entity/domain/training/
  TrainingPackMapper.kt
  ExerciseMapper.kt
  IterationMapper.kt
data-mappers/domain-to-state/src/commonMain/kotlin/com/<org>/<product>/domain/state/training/
  TrainingMapper.kt
```

Package declaration mirrors the directory: `package com.<org>.<product>.dto.entity.training`. Same dot count.

## What lives in each module

Each module contains **only**:

1. Top-level extension functions named `<Source>.toX()`.
2. Optionally, helpers private to a file.

That's it. No classes. No singletons. No DI annotations.

### Example: `:data-mappers:dto-to-entity`

```
data-mappers/dto-to-entity/src/commonMain/kotlin/com/<org>/<product>/dto/entity/
  training/
    TrainingMapper.kt
    ExerciseMapper.kt
    IterationMapper.kt
  user/
    UserMapper.kt
    GoalMapper.kt
    WeightHistoryMapper.kt
  equipment/
    EquipmentMapper.kt
    EquipmentGroupMapper.kt
  ...
```

### Example file: `TrainingMapper.kt` in `:dto-to-entity`

```kotlin
package com.<org>.<product>.dto.entity.training

public fun List<TrainingResponse>.toEntities(): List<TrainingEntity> =
    mapNotNull { it.toEntityOrNull() }

public fun TrainingResponse.toEntityOrNull(): TrainingEntity? {
    val entityId = AppLogger.Mapping.log(id) { "TrainingResponse.id is null" } ?: return null
    val entityDuration = AppLogger.Mapping.log(duration) { "TrainingResponse.duration is null" } ?: return null
    val entityCreatedAt = AppLogger.Mapping.log(createdAt) { "TrainingResponse.createdAt is null" } ?: return null
    val entityVolume = AppLogger.Mapping.log(volume) { "TrainingResponse.volume is null" } ?: return null
    val entityRepetitions = AppLogger.Mapping.log(repetitions) { "TrainingResponse.repetitions is null" } ?: return null
    val entityIntensity = AppLogger.Mapping.log(intensity) { "TrainingResponse.intensity is null" } ?: return null
    val entityUpdatedAt = AppLogger.Mapping.log(updatedAt) { "TrainingResponse.updatedAt is null" } ?: return null
    // Server emits either profileId or the legacy userId; either is acceptable.
    val entityProfileId = AppLogger.Mapping.log(profileId ?: userId) { "TrainingResponse.profileId is null" } ?: return null

    return TrainingEntity(
        id = entityId,
        profileId = entityProfileId,
        duration = entityDuration,
        createdAt = entityCreatedAt,
        volume = entityVolume,
        repetitions = entityRepetitions,
        intensity = entityIntensity,
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

The dependency set per direction (`:toolkit:logger` is in every row — `AppLogger.Mapping.log` is used by every mapper module, including entity-sourced and state-sourced ones, because relation rows or empty `*FormatState` values can require null-and-drop):

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

- **Isolation.** A change to `TrainingResponse` (DTO) cannot accidentally break the entity-to-domain mapper. They're in different modules.
- **Compile parallelism.** Each module compiles in parallel.
- **Discoverability.** "Where does `TrainingResponse → TrainingEntity` live?" → look in `:dto-to-entity`. The module name is the answer.
- **Direction-specific rules.** Each direction has slightly different conventions (DTO-sourced directions log every required field; entity-sourced directions log only `@Relation` and enum-string parses; state-sourced directions log each `*FormatState.value`).

## Anti-patterns

- **A new mapper module** for a new direction. The seven are exhaustive. If you genuinely have a new direction (`state-to-entity`, `entity-to-state`), reconsider — you almost certainly want to compose two existing directions.
- **A class that holds a mapper.** Mappers are **functions**.
- **DI for mappers.** No `@Single`, no `@Factory`. Top-level functions are stateless and don't need Koin.
- **Cross-mapper imports.** `:dto-to-entity` imports `:entity-to-domain`. Forbidden. If you need a `DTO → Domain` conversion, use `:dto-to-domain` or compose at the call site (`dto.toEntityOrNull()?.toDomain()`).
- **Business logic in a mapper.** Mappers translate fields. Validation, derivation, side effects belong in `<X>UseCase` or the Repository.
- **Inline mappers in Repository / ViewModel.** Always live in a dedicated module.
- **One big mapper file per module** with everything. Split by `<area>` (training, user, goal, ...) for navigability.
