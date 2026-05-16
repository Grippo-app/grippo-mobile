# Mapping Conventions

Every mapper module follows the same shape. This document describes the function naming, signature conventions, and standard patterns.

## Function names

| Direction | Function form | Plural form |
|---|---|---|
| `Source.toEntity()` / `Source.toEntityOrNull()` (DTO source) | one source → one entity | `List<Source>.toEntities()` |
| `Source.toEntity(parentId: String)` (Domain source, drafts) | one draft → one Pack or leaf Entity | no plural — caller does `.map { it.toEntity(parentId) }` |
| `Source.toDomain()` / `Source.toDomainOrNull()` | one source → one domain | `List<Source>.toDomain()` |
| `Source.toDraftDomain()` / `Source.toSetDomain()` (`:entity-to-domain`, draft round-trip) | one draft entity/pack → one draft (or Set) domain — suffix disambiguates when the same source has two distinct domain targets (`DraftNotePack → DraftNote`, `DraftItemEntity → SetItem`) | `List<Source>.toDraftDomain()` / `List<Source>.toSetDomain()` |
| `Source.toState()` | one source → one state | `List<Source>.toState()` (returns `PersistentList<XState>` in `:domain-to-state`) |
| `Source.toBody()` | one source → one request body | `List<Source>.toBody()` (same name, overloaded on receiver) |

Top-level extension functions. **No classes. No `@Single`. No DI.** Stateless.

```kotlin
public fun NoteResponse.toEntityOrNull(): NoteEntity? { ... }
public fun List<NoteResponse>.toEntities(): List<NoteEntity> = mapNotNull { it.toEntityOrNull() }

public fun NotePack.toDomain(): Note { ... }
public fun List<NotePack>.toDomain(): List<Note> = map { it.toDomain() }

public fun Note.toState(): NoteState { ... }
public fun List<Note>.toState(): PersistentList<NoteState> { ... }

// Body mappers source from the "submit" domain variant (Set<X>, Draft<X>), not the read variant.
public fun SetNote.toBody(): NoteBody { ... }
public fun List<SetTag>.toBody(): List<TagBody> = map { it.toBody() }
```

## Nullable vs non-null variants

- `toEntity()` / `toDomain()`: returns a non-null result. Use when **all required fields** are guaranteed non-null on the input.
- `toEntityOrNull()` / `toDomainOrNull()`: returns nullable. Use when the input has nullable fields (DTOs always do, and entity Packs frequently do — see "Entity → Domain" below) and a missing required field means "skip this row".

Plural variants:

- `List<Source>.toEntities()` typically uses `mapNotNull { it.toEntityOrNull() }`.
- `List<Source>.toDomain()` for Entity/Pack sources uses `map { it.toDomain() }` when the per-row mapper is non-null, or `mapNotNull { it.toDomain() }` when the per-row mapper returns nullable (e.g. `TagPack.toDomain(): Tag?` because the embedded `detail` relation may be absent).

## DTO → Entity canonical pattern

```kotlin
public fun NoteResponse.toEntityOrNull(): NoteEntity? {
    val entityId = AppLogger.Mapping.log(id) { "NoteResponse.id is null" } ?: return null
    val entityTitle = AppLogger.Mapping.log(title) { "NoteResponse.title is null" } ?: return null
    val entityAmount = AppLogger.Mapping.log(amount) { "NoteResponse.amount is null" } ?: return null
    val entityDuration = AppLogger.Mapping.log(duration) { "NoteResponse.duration is null" } ?: return null
    val entityCreatedAt = AppLogger.Mapping.log(createdAt) { "NoteResponse.createdAt is null" } ?: return null
    val entityUpdatedAt = AppLogger.Mapping.log(updatedAt) { "NoteResponse.updatedAt is null" } ?: return null
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

Rules:

1. **Every required field uses `AppLogger.Mapping.log(value) { msg } ?: return null`.** If the field is null, log it (so the team can diagnose backend regressions from the append-only `[MAPPING]` log) and skip the row.
2. **The default stance is "required".** If an entity column is non-null, the mapper requires it — drop the row when missing rather than fabricating a value. Use an Elvis fallback **only** when the entity field itself is nullable (e.g. leaf-row numeric columns) or when the server emits a deprecated alias that must be coalesced (`profileId ?: userId`).
3. **No business logic.** Just field translation.
4. **One `return <X>Entity(...)`** at the bottom. No intermediate `var entity = <X>Entity(...)`.
5. **Local variable names are prefixed** (`entityId`, `entityAmount`) to avoid shadowing same-named DTO fields inside the receiver scope.

## Entity → Domain canonical pattern

Scalar entity columns are non-null by contract (the dto-to-entity step already validated). Two cases still call for `AppLogger.Mapping.log`:

- An embedded `@Relation` is missing at read time (e.g. `TagPack.detail` is null because the parent detail row was deleted out from under the child) — the row is unusable, drop it.
- A column stores a string-encoded enum and the value doesn't parse (DB pre-dates a renamed enum case).

```kotlin
public fun NotePack.toDomain(): Note = Note(
    id = note.id,
    duration = note.duration.minutes,                          // Long (minutes) → Duration
    createdAt = DateTimeUtils.toLocalDateTime(note.createdAt), // String → LocalDateTime
    amount = note.amount,
    tags = tags.toDomain(),                                    // List<TagPack> → List<Tag>
)

public fun List<NotePack>.toDomain(): List<Note> = map { it.toDomain() }

// Relation may be missing — nullable result, plural variant uses mapNotNull.
public fun TagPack.toDomain(): Tag? {
    val mappedDetail = AppLogger.Mapping.log(detail?.toDomain()) {
        "TagPack detail by ${tag.detailId} is null"
    } ?: return null

    return Tag(
        id = tag.id,
        name = tag.name,
        items = items.toDomain(),
        amount = tag.amount,
        createdAt = DateTimeUtils.toLocalDateTime(tag.createdAt),
        detail = mappedDetail,
    )
}

public fun List<TagPack>.toDomain(): List<Tag> = mapNotNull { it.toDomain() }
```

Rules:

1. **Scalar columns: no null checks.** Read scalar columns directly — Room enforces the column's declared nullability at insert time, so a non-null column is guaranteed non-null and a nullable column (e.g. `ItemEntity.optionalAmount: Float?`, `NoteEntity.subtitle: String?`) passes straight through to a matching nullable domain field. Don't `AppLogger.Mapping.log` a scalar — if a non-null column somehow surfaces as null at runtime, that's a bug; let it crash with a clear `NullPointerException`.
2. **Embedded relations and enum-string parses: nullable.** When the per-row mapper consumes a `@Relation` field (e.g. `TagPack.detail`) or a string column that decodes into an enum (e.g. `<Some>Enum.of(type)`), return `T?` and use `AppLogger.Mapping.log` to drop unusable rows. The relation may be absent because the related row was deleted; an enum string may not parse if the DB pre-dates a renamed case.
3. **Type translation happens here.** `Long` (minutes — see `NoteEntity.duration`) → `Duration`; `String` (ISO-8601) → `LocalDateTime`.
4. **Nested Packs → nested domain.** `TagPack.toDomain()` is called from `NotePack.toDomain()`.

## Domain → DTO Body

```kotlin
public fun SetNote.toBody(): NoteBody = NoteBody(
    title = title,
    duration = duration.inWholeMinutes,
    amount = amount,
    tags = tags.toBody(),
)

public fun List<SetTag>.toBody(): List<TagBody> = map { it.toBody() }

public fun SetTag.toBody(): TagBody = TagBody(
    amount = amount,
    detailId = detail.id,
    items = items.toBody(),
    name = name,
)
```

Notes:

- Source type is the **submit variant** (`SetNote`, `SetTag`, `SetItem`), not the read variant — body mappers convert what the form has just produced.
- The plural function name is `toBody()` (overloaded on receiver), not `toBodies()`.
- Type translation reverses: `Duration` → `Long` (minutes, matching the entity unit); `LocalDateTime` → ISO-8601 UTC string for direction-specific bodies that carry timestamps. `NoteBody` itself has no timestamp.

## Domain → State

```kotlin
public fun Note.toState(): NoteState = NoteState(
    id = id,
    tags = tags.toState(),
    amount = AmountFormatState.of(amount),
    createdAt = DateTimeFormatState.of(
        value = createdAt,
        range = DateRangePresets.infinity(),
        format = DateFormat.DateOnly.DateMmmDdYyyy,
    ),
)

public fun List<Note>.toState(): PersistentList<NoteState> =
    map { it.toState() }.toPersistentList()
```

Rules:

1. **Numerics become `AmountFormatState`** (`AmountFormatState.of(value)`).
2. **Dates become `DateTimeFormatState.of(value, range, format)`** — never raw `LocalDateTime` and never an eagerly-formatted `String`. The `range` + `format` carry enough context for the UI to re-render on locale change.
3. **Collections become `PersistentList<XState>`** via `.toPersistentList()`. `PersistentList` is the concrete type used everywhere; consumers can still up-cast to `ImmutableList` where needed.
4. **No `UiText` here.** Mappers don't construct localized strings — that happens in the ViewModel (`stringProvider.get(...)`) or the screen (`AppTokens.strings.res(...)`).
5. **Per-row state** for list items lives in the same file: `Note.toState()` and `List<Note>.toState()`.

## State → Domain

```kotlin
public fun TagState.toDomain(): SetTag? {
    val mappedAmount = AppLogger.Mapping.log(amount.value) {
        "TagState amount value is null (id=$id)"
    } ?: return null

    val mappedCreatedAt = AppLogger.Mapping.log(createdAt.value) {
        "TagState createdAt value is null (id=$id)"
    } ?: return null

    return SetTag(
        name = name,
        items = items.toDomain(),
        detail = detail.toDomain(),
        amount = mappedAmount,
        createdAt = mappedCreatedAt,
    )
}

public fun List<TagState>.toDomain(): List<SetTag> =
    mapNotNull { it.toDomain() }.toPersistentList()
```

Rules:

1. **Returns `Set<X>?`** — incomplete form returns null. The ViewModel decides whether to submit. The target type is the **submit variant** (`SetTag`, `SetItem`, …), parallel to the body mappers.
2. **Use `AppLogger.Mapping.log(state.x.value) ?: return null`** for required values, same pattern as DTO-sourced mappers. `*FormatState.value` is the nullable underlying value (e.g. `AmountFormatState.Valid.value` vs `Empty.value = null`).
3. **Optional `*FormatState.value` passes through as nullable** if the domain field is nullable (e.g. leaf-row optional numeric fields).
4. **No `name.trim().ifBlank { return null }`-style validation** — the `*FormatState` types already encode validity; if a field is invalid, its `.value` is null and the standard `AppLogger.Mapping.log ?: return null` step drops the record.
5. **Pure field translation only.** Aggregation across child rows is computed when constructing the State (e.g. in the ViewModel from item values), not inside the State → Domain mapper.

## Domain → Entity (drafts)

```kotlin
public fun DraftNote.toEntity(profileId: String): DraftNotePack {
    val id = Uuid.random().toString()

    val note = DraftNoteEntity(
        id = id,
        profileId = profileId,
        noteId = noteId,
        duration = duration.inWholeMinutes,
    )

    return DraftNotePack(
        note = note,
        tags = tags.map { it.toEntity(id) },
    )
}

public fun DraftTag.toEntity(noteId: String): DraftTagPack { /* ... */ }
public fun SetItem.toEntity(tagId: String): DraftItemEntity { /* ... */ }
```

Rules:

1. **Ids are client-generated** with `Uuid.random().toString()`. Drafts never originate from the server.
2. **Parent FK is a parameter**, not a field on the source. The root caller passes `profileId`; each level passes its freshly-minted `id` down to its children.
3. **No `AppLogger.Mapping.log`.** Domain models have non-null fields, so there is no null to drop.
4. **Parent mappers return a `Pack`** (`DraftNotePack`, `DraftTagPack`); leaf mappers return a bare entity (`DraftItemEntity`).
5. **No plural variant** — drafts are submitted singly. The parent mapper does `.map { it.toEntity(parentId) }` inline.
6. **Source types are heterogeneous**: parents use `Draft<X>`, but the leaf reuses the submit variant `SetItem` (no `DraftItem` domain class).

## `AppLogger.Mapping` helper

```kotlin
public object AppLogger {
    public object Mapping {
        public fun <T> log(value: T?, msg: () -> String): T? {
            if (value != null) return value
            val location = getCallerLocation()
            present(LogCategory.MAPPING, "${msg()} $location")
            return null
        }
    }
}
```

Returns the input value verbatim. Side effect: when null, invokes `msg()` (lazy), appends caller `(file:line)`, and writes one `[MAPPING]` line to the append-only log file (no rotation; cleared via `AppLogger.clearLogFile()` from the debug screen). Pair it with `?: return null` at every call site — never `!!`.

## File and module layout

```
data-mappers/dto-to-entity/src/commonMain/kotlin/com/<org>/<product>/dto/entity/
  note/
    NoteMapper.kt           # NoteResponse.toEntityOrNull() + List variant
    TagMapper.kt            # TagResponse.toEntityOrNull() + List variant
    ItemMapper.kt           # ItemResponse.toEntityOrNull() + List variant
  user/
    UserMapper.kt
```

One file per source DTO. The repository pulls fields out of the parent DTO and calls each leaf mapper independently (e.g. `dto.tags.toEntities()`, `dto.tags.flatMap { it.items }.toEntities()`), so each level lives in its own file.

## Anti-patterns

- **`!!`** on a nullable field. Forbidden. Use `?: return null` (DTO sources) or `?: error("...")` (entity sources, where null is a bug).
- **Validation inside a mapper.** Throws are wrong here; either drop the row (`?: return null`) or compute a domain-meaningful default.
- **Caching computed fields.** Mappers are stateless; cache results in the consumer if needed.
- **Returning `T` from a DTO mapper.** DTOs are nullable; use `T?` and `toXOrNull`.
- **Mapper depending on another mapper module.** Each module is isolated.
- **`@Composable` mappers.** Mappers don't know Compose. UI formatting that needs `@Composable` (e.g. `UiText.text()`) is called by the **consumer**.
- **Logging from the consumer**. Logging is `AppLogger.Mapping`'s job — it's part of the canonical pattern.
- **Conversion in the Repository or ViewModel.** Always lives in a mapper module. The cost is one function call; the win is consistency.
