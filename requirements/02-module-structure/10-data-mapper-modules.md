# `:data-mappers:*` Modules

There are **seven** mapper modules, one per direction. Each is **isolated** — no cross-mapper imports.

## The seven directions

| Module | Package prefix | From | To | When used |
|---|---|---|---|---|
| `:data-mappers:dto-to-entity` | `com.<org>.<product>.dto.entity.<area>` | DTO (`*Response`) | Entity (`*Entity`) | Repository caches API response into Room |
| `:data-mappers:entity-to-domain` | `com.<org>.<product>.entity.domain.<area>` | Entity / `*Pack` | Domain (`<X>`) | Repository observes DAO and emits domain |
| `:data-mappers:dto-to-domain` | `com.<org>.<product>.dto.domain.<area>` | DTO | Domain | Repository skips caching (rare; e.g. one-shot reports) |
| `:data-mappers:domain-to-state` | `com.<org>.<product>.domain.state.<area>` | Domain | UI State | ViewModel transforms domain into `*State`/`UiText`/`*FormatState` |
| `:data-mappers:state-to-domain` | `com.<org>.<product>.state.domain.<area>` | UI State | Domain | ViewModel builds a domain object from form state to send to the repository |
| `:data-mappers:domain-to-entity` | `com.<org>.<product>.domain.entity.<area>` | Domain | Entity | Drafts: ViewModel persists a domain draft to Room without touching the API |
| `:data-mappers:domain-to-dto` | `com.<org>.<product>.domain.dto.<area>` | Domain | DTO (`*Body`) | Repository builds a POST/PUT body from a domain object |

## Function name conventions

- Top-level extension function: `<Source>.toEntity()`, `<Source>.toDomain()`, `<Source>.toState()`, `<Source>.toBody()`.
- Plural: `List<Source>.toEntities()`, `List<Source>.toDomain()`, `List<Source>.toState()`.
- Nullable variant for DTO→Entity / DTO→Domain (where DTO fields are nullable but domain/entity are not): `<Source>.toEntityOrNull(): T?`, `<Source>.toDomainOrNull(): T?`.

Example:

```kotlin
// data-mappers/dto-to-entity/.../notes/NoteMapper.kt
public fun NoteResponse.toEntityOrNull(): NoteEntity? {
    val entityId = AppLogger.Mapping.log(id) { "NoteResponse.id is null" }
        ?: return null
    val entityTitle = AppLogger.Mapping.log(title) { "NoteResponse.title is null" }
        ?: return null
    // ...
    return NoteEntity(id = entityId, title = entityTitle, /* ... */)
}

public fun List<NoteResponse>.toEntities(): List<NoteEntity> =
    mapNotNull { it.toEntityOrNull() }
```

`AppLogger.Mapping.log(value) { msg }`: if `value != null`, returns it; otherwise writes `[MAPPING] <msg>` to the log and returns `null`. Each required field is logged separately for diagnostics.

See `07-mappers/02-mapping-conventions.md` for the canonical pattern and `07-mappers/03-null-safety.md` for the rationale.

## Rules

1. **One direction per module.** Never put a DTO→Entity mapper and an Entity→Domain mapper in the same file or module.
2. **No cross-mapper dependencies.** `:data-mappers:dto-to-entity` does not import `:data-mappers:entity-to-domain`. If a chain conversion is needed, the consumer composes two calls.
3. **No business logic.** Mappers translate fields; they do not perform validation, derive values, or call into other services. Validation goes in `<X>UseCase`; derivation is in the ViewModel.
4. **Stateless top-level functions.** No classes. No DI. Mapper modules have no Koin annotations.
5. **One file per source type.** `NoteResponse → NoteEntity` lives in `NoteMapper.kt`; `TagResponse → TagEntity` in `TagMapper.kt`. **Not** all of `dto-to-entity` in a single mega-file.

## Build (typical)

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.data.mappers.<from>.to.<to>" }

    sourceSets.commonMain.dependencies {
        implementation(projects.dataServices.database)   // for Entity
        implementation(projects.dataServices.backend)    // for DTO
        implementation(projects.toolkit.logger)
        // ... add other source/target modules as needed for this direction
    }
}
```

A mapper module imports **only** the modules that define its source and target types, plus `:toolkit:logger` for `AppLogger.Mapping`.

## Why seven modules?

- **Isolation.** A change to `NoteResponse` (a DTO) cannot accidentally affect the entity-to-domain mapper. They're in different modules.
- **Compile parallelism.** Mapper modules compile in parallel; one big mapper module would serialize.
- **Discoverability.** "Where does NoteResponse → NoteEntity live?" → look in `:dto-to-entity`. The module name is the answer.
- **Direction-specific rules.** Each direction has slightly different conventions (e.g. only DTO→Entity uses nullable variants). Separate modules make the rules enforceable per module.

## When to add a new mapper

If a new domain area is introduced (e.g. `Notification`):

1. Identify which directions are needed (usually `dto-to-entity` + `entity-to-domain` at minimum).
2. Add one file per direction in the corresponding mapper module: `NotificationMapper.kt`.
3. If the area needs new directions (e.g. `state-to-domain` for a form), add files there.
4. **Do not** create a new mapper module — the seven are exhaustive.

## When to update DTO/Entity/Domain shapes

Whenever a DTO field is added, removed, or renamed:

1. Update the DTO file in `:data-services:backend/dto/<area>`.
2. Update the corresponding mappers in `:data-mappers:dto-to-entity`, `:data-mappers:dto-to-domain`, and `:data-mappers:domain-to-dto`.
3. If the entity shape also changes, update `:data-services:database/entity/<X>Entity` and add a Room migration (separate, deliberate task — see `06-data-layer/06-room-migrations.md`).
