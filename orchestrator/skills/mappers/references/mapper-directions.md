# The Seven Mapper Directions & Modules

Conversions between DTOs / Entities / Domain models / UI states / Bodies live in
**seven dedicated modules**, one per direction. Each module is isolated — **no
cross-mapper imports, no shared base class, no DI.**

## The seven directions

| Module | Package prefix | From | To | Used by |
|---|---|---|---|---|
| `:data-mappers:dto-to-entity` | `com.<org>.<product>.dto.entity.<area>` | DTO `<X>Response` | Entity `<X>Entity` | Repository caches a server response into Room |
| `:data-mappers:entity-to-domain` | `com.<org>.<product>.entity.domain.<area>` | Entity / `<X>Pack` | Domain `<X>` / `Draft<X>` / `Set<X>` (draft round-trip) | Repository observes a DAO and emits domain models |
| `:data-mappers:dto-to-domain` | `com.<org>.<product>.dto.domain.<area>` | DTO `<X>Response` | Domain `<X>` | Repository skips caching for one-shot reads (rare; e.g. reports) |
| `:data-mappers:domain-to-state` | `com.<org>.<product>.domain.state.<area>` | Domain `<X>` | UI State (`*State`, `*FormatState`) | ViewModel maps domain into screen state before `update {}` |
| `:data-mappers:state-to-domain` | `com.<org>.<product>.state.domain.<area>` | UI State `*State` | Domain `Set<X>` (submit form) | ViewModel collects form input and builds a domain object to send |
| `:data-mappers:domain-to-entity` | `com.<org>.<product>.domain.entity.<area>` | Domain `Draft<X>` (parents) / `Set<X>` (leaf rows) | `Draft<X>Pack` for parents (Note, Tag), bare `Draft<X>Entity` for leaf rows (Item) | ViewModel persists a draft to Room without the API. Ids client-generated (`Uuid.random()`); each child takes its parent FK as a parameter. |
| `:data-mappers:domain-to-dto` | `com.<org>.<product>.domain.dto.<area>` | Domain `Set<X>` | Body `<X>Body` | Repository builds a POST/PUT body |

## Package naming

`com.<org>.<product>.<from>.<to>.<area>`. The mapper modules use **standard
slash-separated directories** (unlike `:data-features:*`, which use dotted
directories). The `package` declaration mirrors the directory — same dot count.

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

Package declaration: `package com.<org>.<product>.dto.entity.note`.

## What lives in each module

Each module contains **only**:

1. Top-level extension functions named `<Source>.toX()`.
2. Optionally, helpers private to a file.

That's it. **No classes. No singletons. No DI annotations.**

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
    val entityProfileId = AppLogger.Mapping.log(profileId) { "NoteResponse.profileId is null" } ?: return null

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

Local variables are prefixed `entity*` to disambiguate from same-named DTO fields
inside the receiver scope.

## Rules (MUST)

1. **One direction per module / per file.** Never put a DTO→Entity mapper and an
   Entity→Domain mapper in the same file or module.
2. **No cross-mapper dependencies.** `:data-mappers:dto-to-entity` does not import
   `:data-mappers:entity-to-domain`. If a chain conversion is needed, the consumer
   composes two calls.
3. **No business logic.** Mappers translate fields; they do not validate, derive
   values, or call into other services. Validation goes in `<X>UseCase`;
   derivation is in the ViewModel.
4. **Stateless top-level functions.** No classes. No DI. Mapper modules have no
   Koin annotations.
5. **One file per source type.** `NoteResponse → NoteEntity` lives in
   `NoteMapper.kt`; `TagResponse → TagEntity` in `TagMapper.kt`. **Not** all of
   `dto-to-entity` in a single mega-file. Split by `<area>` (note, user, …) for
   navigability.

## Anti-pattern: mapper-to-mapper import (MUST)

Mapper modules `:data-mappers:dto-to-entity` and `:data-mappers:entity-to-domain`
**cannot import each other**. Each direction is isolated. If a chain conversion is
needed (e.g. DTO → Domain), either:

- Use `:data-mappers:dto-to-domain` directly (a separate direction), or
- Compose two calls in the consumer (Repository or VM):
  `dto.toEntityOrNull()?.toDomain()`.

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

A mapper module imports **only** the modules that define its source and target
types, plus `:toolkit:logger` for `AppLogger.Mapping`.

`:toolkit:logger` is wired into **every** row's build deps as a convention, but
**actual call-site usage** is limited to the four directions whose **source**
carries nullable values — `:dto-to-entity`, `:dto-to-domain`, `:entity-to-domain`
(`@Relation` rows + enum-string parses), and `:state-to-domain`
(`*FormatState.value`). The three `:domain-to-*` directions take non-null domain
inputs and therefore have **no `AppLogger.Mapping.log` call sites** — the dep is
unused but kept for symmetry.

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

- **Isolation.** A change to `NoteResponse` (a DTO) cannot accidentally affect the
  entity-to-domain mapper. They're in different modules.
- **Compile parallelism.** Mapper modules compile in parallel; one big mapper
  module would serialize.
- **Discoverability.** "Where does `NoteResponse → NoteEntity` live?" → look in
  `:dto-to-entity`. The module name is the answer.
- **Direction-specific rules.** Each direction has slightly different conventions
  (DTO-sourced directions log every required field; entity-sourced directions log
  only `@Relation` and enum-string parses; state-sourced directions log each
  `*FormatState.value`). Separate modules make the rules enforceable per module.

## When to add a new mapper (NORMATIVE)

If a new domain area is introduced (e.g. `Notification`):

1. Identify which directions are needed (usually `dto-to-entity` +
   `entity-to-domain` at minimum).
2. Add one file per direction in the corresponding mapper module:
   `NotificationMapper.kt`.
3. If the area needs new directions (e.g. `state-to-domain` for a form), add files
   there.
4. **Do not** create a new mapper module — the seven are exhaustive.

## When to update DTO/Entity/Domain shapes (NORMATIVE)

Whenever a DTO field is added, removed, or renamed:

1. Update the DTO file in `:data-services:backend/dto/<area>`.
2. Update the corresponding mappers in `:data-mappers:dto-to-entity`,
   `:data-mappers:dto-to-domain`, and `:data-mappers:domain-to-dto`.
3. If the entity shape also changes, update
   `:data-services:database/entity/<X>Entity` and add a Room migration (a
   separate, deliberate task — see `../../data-layer/references/cookbook-room-migration.md`).

## Mapper layer in the data flow

Each conversion direction is one module. The Repository pulls mappers from
`:data-mappers:*` (e.g. `:dto-to-entity`, `:entity-to-domain`, `:domain-to-dto`).

```kotlin
// :data-mappers:entity-to-domain
public fun NotePack.toDomain(): Note = Note(
    id = note.id,
    profileId = note.profileId,
    createdAt = DateTimeUtils.toLocalDateTime(note.createdAt),
    tags = tags.toDomain(),
)

public fun List<NotePack>.toDomain(): List<Note> = map { it.toDomain() }
```

Type promotion lives at this entity→domain (and dto→domain) boundary: timestamp
`String` → `LocalDateTime`, minutes `Long` → `Duration`, and a closed-set `String`
→ its domain `<X>Enum` via `<X>Enum.of(...)` (drop-on-unknown guarded with
`AppLogger.Mapping.log(...) ?: return null`; default-on-unknown called bare). The
DTO and Entity keep the raw `String?` / `Long` replica.

## Module-level anti-patterns (MUST)

- **A new mapper module** for a new direction. The seven are exhaustive. If you
  genuinely have a new direction (`state-to-entity`, `entity-to-state`),
  reconsider — you almost certainly want to compose two existing directions.
- **A class that holds a mapper.** Mappers are **functions**.
- **DI for mappers.** No `@Single`, no `@Factory`. Top-level functions are
  stateless and don't need Koin.
- **Cross-mapper imports.** `:dto-to-entity` imports `:entity-to-domain`.
  Forbidden. Use `:dto-to-domain` or compose at the call site
  (`dto.toEntityOrNull()?.toDomain()`).
- **Business logic in a mapper.** Mappers translate fields. Validation,
  derivation, side effects belong in `<X>UseCase` or the Repository.
- **Inline mappers in Repository / ViewModel.** Always live in a dedicated module.
- **One big mapper file per module** with everything. Split by `<area>`.
