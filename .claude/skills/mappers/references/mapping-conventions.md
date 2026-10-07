# Mapping Conventions — Function Names, Signatures, Canonical Patterns

Every mapper module follows the same shape. This document describes function
naming, signature conventions, and the standard per-direction patterns.

## Function names (MUST)

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

## Nullable vs non-null variants (MUST)

- `toEntity()` / `toDomain()`: returns a non-null result. Use when **all required
  fields** are guaranteed non-null on the input.
- `toEntityOrNull()` / `toDomainOrNull()`: returns nullable for DTO-source
  mappers. Entity/Pack sources whose nullable fields can drop a row still use
  `toDomain(): T?`; collection mappers then use `mapNotNull`.

Plural variants:

- `List<Source>.toEntities()` typically uses `mapNotNull { it.toEntityOrNull() }`.
- `List<Source>.toDomain()` for Entity/Pack sources uses `map { it.toDomain() }`
  when the per-row mapper is non-null, or `mapNotNull { it.toDomain() }` when the
  per-row mapper returns nullable (e.g. `TagPack.toDomain(): Tag?` because the
  embedded `detail` relation may be absent).

### Nullable-receiver variant (MUST)

When a parent DTO embeds an **optional nested DTO** (e.g.
`UserResponse.stats: UserStatsResponse? = null`), declare the leaf mapper with a
**nullable receiver** so the parent does not need a null check at every call site:

```kotlin
// Reference: data-mappers/dto-to-entity/user/UserStatsMapper.kt
public fun UserStatsResponse?.toEntityOrNull(userId: String): UserStatsEntity? {
    val entityNotesCount = AppLogger.Mapping.log(this?.notesCount) {
        "UserStatsResponse.notesCount is null"
    } ?: return null
    // ... each field accessed via `this?.field`
    return UserStatsEntity(userId = userId, notesCount = entityNotesCount, /* ... */)
}

// Parent mapper / Repository:
val statsEntity = userDto.stats.toEntityOrNull(userId)   // works whether stats is null or not
```

Use this variant only when:

- The receiver itself is optional (a `null` parent is a valid input).
- The result type already has an `OrNull` suffix (so the contract still says "drop
  on missing data").
- All field accesses inside the body use `this?.<field>` — never bare `<field>`.

For a non-null receiver where the parent always exists, stick with the standard
`Source.toEntityOrNull()` form.

## DTO → Entity canonical pattern (MUST)

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

1. **Every required field uses `AppLogger.Mapping.log(value) { msg } ?: return null`.**
   If the field is null, log it (so the team can diagnose backend regressions from
   the append-only `[MAPPING]` log) and skip the row.
2. **The default stance is "required".** If an entity column is non-null, the
   mapper requires it — drop the row when missing rather than fabricating a value.
   Nullable target fields pass through as nullable. Use an Elvis fallback only for
   non-null collection targets that intentionally default to `emptyList()`.
3. **No business logic.** Just field translation.
4. **One `return <X>Entity(...)`** at the bottom. No intermediate
   `var entity = <X>Entity(...)`.
5. **Local variable names are prefixed** (`entityId`, `entityAmount`) to avoid
   shadowing same-named DTO fields inside the receiver scope.

## Entity → Domain canonical pattern (MUST)

Scalar entity columns are non-null by contract (the dto-to-entity step already
validated). Two cases still call for `AppLogger.Mapping.log`:

- An embedded `@Relation` is missing at read time (e.g. `TagPack.detail` is null
  because the parent detail row was deleted out from under the child) — the row is
  unusable, drop it.
- A column stores a string-encoded enum and the value doesn't parse (DB pre-dates a
  renamed enum case).

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

1. **Scalar columns: no null checks.** Read scalar columns directly — Room enforces
   the column's declared nullability at insert time, so a non-null column is
   guaranteed non-null and a nullable column (e.g. `ItemEntity.optionalAmount:
   Float?`, `NoteEntity.subtitle: String?`) passes straight through to a matching
   nullable domain field. Don't `AppLogger.Mapping.log` a scalar — if a non-null
   column somehow surfaces as null at runtime, that's a bug; let it crash with a
   clear `NullPointerException`.
2. **Embedded relations and enum-string parses: nullable.** When the per-row mapper
   consumes a `@Relation` field (e.g. `TagPack.detail`) or a string column that
   decodes into an enum (e.g. `<Some>Enum.of(type)`), return `T?` and use
   `AppLogger.Mapping.log` to drop unusable rows.
3. **Type translation happens here.** `Long` (minutes — see `NoteEntity.duration`)
   → `Duration`; `String` (ISO-8601) → `LocalDateTime`.
4. **Nested Packs → nested domain.** `TagPack.toDomain()` is called from
   `NotePack.toDomain()`.

## Enum dictionaries (`String` ↔ `<X>Enum`) (MUST)

A closed-set backend field is a raw `String?` on the DTO and a raw `String` column
on the entity (the storage replica) — and a typed `<X>Enum` in the domain model.
The enum, its `key`, and its `of()` factory are defined in
`:data-features:feature-api`. The mappers only move between the two representations:

```kotlin
// entity→domain (or dto→domain): parse String → enum. Drop the row on an unknown key.
public fun <X>Pack.toDomain(): <X>? {
    val mappedStatus = AppLogger.Mapping.log(<X>StatusEnum.of(<x>.status)) {
        "<X>Entity ${<x>.id} has unrecognized status: ${<x>.status}"
    } ?: return null
    // ... one guarded parse per drop-on-unknown enum field

    return <X>(
        status = mappedStatus,        // String → <X>StatusEnum
        // a defaulted enum (of(...) ?: <DEFAULT>) is read bare — no guard, no drop
        // ...
    )
}

// domain→dto body: enum → key.
status = status.key,
```

Rules:

1. **`dto→entity` does not parse the enum** — it passes the raw `String` straight
   through (the entity is the replica). The `String → <X>Enum` parse happens only at
   the `…→domain` step.
2. **Drop vs default follows the enum's `of()` shape.** A nullable `of()`
   (drop-on-unknown) is guarded with
   `AppLogger.Mapping.log(<X>Enum.of(v)) { ... } ?: return null`; a defaulted `of()`
   (`... ?: <DEFAULT>`) is called bare.
3. **`domain→dto` reverses via `.key`** — `status.key`, `<field>?.key` for a
   nullable enum, `<list>.map { it.key }` for a list of enums.
4. **`domain→state` / `state→domain` use an exhaustive `when`** against the parallel
   `*EnumState` in `:ui-core:state` (`NoteStatusEnum.toState()` →
   `NoteStatusEnumState`, and back) — the domain enum stays pure; the state enum
   carries the UI `title()` / `color()`.
5. **Payload-carrying discriminators parse into a sealed type, not an enum.** When
   the backend `type` selects different fields (domain modeled as a
   `sealed interface <X>`), the mapper builds the variant with a guarded
   `when (type)` and preserves the wire token as the variant's `key`:

   ```kotlin
   val mapped = AppLogger.Mapping.log(
       when (type) {
           "<variant_a>" -> <X>.<VariantA>(key = type, <fieldA> = <fieldA>)
           "<variant_b>" -> <X>.<VariantB>(key = type, <fieldB> = <fieldB>, <fieldC> = <fieldC>)
           else          -> null
       }
   ) { "<X>Entity $id has unrecognized type: $type" } ?: return null
   ```

   Reverse (`domain→dto`) with a `when (value)` that emits `value.key` plus the
   variant's payload.

## Domain → DTO Body (MUST)

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

- Source type is the **submit variant** (`SetNote`, `SetTag`, `SetItem`), not the
  read variant — body mappers convert what the form has just produced.
- The plural function name is `toBody()` (overloaded on receiver), not `toBodies()`.
- Type translation reverses: `Duration` → `Long` (minutes, matching the entity
  unit); `LocalDateTime` → ISO-8601 UTC string for direction-specific bodies that
  carry timestamps. `NoteBody` itself has no timestamp.
- Enum fields reverse via `.key` — `status = status.key` (`<field>?.key` for a
  nullable enum field, `<list>.map { it.key }` for a list of enums). The mirror of
  `<X>Enum.of(key)` on the way in.

## Domain → State (MUST)

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
2. **Dates become `DateTimeFormatState.of(value, range, format)`** — never raw
   `LocalDateTime` and never an eagerly-formatted `String`. The `range` + `format`
   carry enough context for the UI to re-render on locale change.
3. **Collections become `PersistentList<XState>`** via `.toPersistentList()`.
   `PersistentList` is the concrete type used everywhere; consumers can still
   up-cast to `ImmutableList` where needed.
4. **No `UiText` here.** Mappers don't construct localized strings — that happens
   in the ViewModel (`stringProvider.get(...)`) or the screen
   (`AppTokens.strings.res(...)`).
5. **Per-row state** for list items lives in the same file: `Note.toState()` and
   `List<Note>.toState()`.

## State → Domain (MUST)

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
    mapNotNull { it.toDomain() }
```

Rules:

1. **Returns the submit-variant type `Set<X>?` (e.g. `SetTag?`), not a
   collection** — incomplete form returns null. The ViewModel decides whether to
   submit. The target type is the **submit variant** (`SetTag`, `SetItem`, …),
   parallel to the body mappers.
2. **Use `AppLogger.Mapping.log(state.x.value) ?: return null`** for required
   values, same pattern as DTO-sourced mappers. `*FormatState.value` is the nullable
   underlying value (e.g. `AmountFormatState.Valid.value` vs `Empty.value = null`).
3. **Optional `*FormatState.value` passes through as nullable** if the domain field
   is nullable (e.g. leaf-row optional numeric fields).
4. **No `name.trim().ifBlank { return null }`-style validation** — the
   `*FormatState` types already encode validity; if a field is invalid, its `.value`
   is null and the standard `AppLogger.Mapping.log ?: return null` step drops the
   record.
5. **Pure field translation only.** Aggregation across child rows is computed when
   constructing the State (e.g. in the ViewModel from item values), not inside the
   State → Domain mapper.

## Domain → Entity (drafts) (MUST)

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

1. **Ids are client-generated** with `Uuid.random().toString()`. Drafts never
   originate from the server.
2. **Parent FK is a parameter**, not a field on the source. The root caller passes
   `profileId`; each level passes its freshly-minted `id` down to its children.
3. **No `AppLogger.Mapping.log`.** Domain models have non-null fields, so there is
   no null to drop.
4. **Parent mappers return a `Pack`** (`DraftNotePack`, `DraftTagPack`); leaf
   mappers return a bare entity (`DraftItemEntity`).
5. **No plural variant** — drafts are submitted singly. The parent mapper does
   `.map { it.toEntity(parentId) }` inline.
6. **Source types are heterogeneous**: parents use `Draft<X>`, but the leaf reuses
   the submit variant `SetItem` (no `DraftItem` domain class).

## File and module layout (MUST)

```
data-mappers/dto-to-entity/src/commonMain/kotlin/com/<org>/<product>/dto/entity/
  note/
    NoteMapper.kt           # NoteResponse.toEntityOrNull() + List variant
    TagMapper.kt            # TagResponse.toEntityOrNull() + List variant
    ItemMapper.kt           # ItemResponse.toEntityOrNull() + List variant
  user/
    UserMapper.kt
```

One file per source DTO. The repository pulls fields out of the parent DTO and
calls each leaf mapper independently (e.g. `dto.tags.toEntities()`,
`dto.tags.flatMap { it.items }.toEntities()`), so each level lives in its own file.

## Anti-patterns (MUST)

- **`!!`** on a nullable field. Forbidden. Use `?: return null` (DTO sources) or
  `?: error("...")` (entity sources, where null is a bug).
- **Validation inside a mapper.** Throws are wrong here; either drop the row
  (`?: return null`) or compute a domain-meaningful default.
- **Caching computed fields.** Mappers are stateless; cache results in the consumer
  if needed.
- **Returning `T` from a DTO mapper.** DTOs are nullable; use `T?` and `toXOrNull`.
- **Mapper depending on another mapper module.** Each module is isolated.
- **`@Composable` mappers.** Mappers don't know Compose. UI formatting that needs
  `@Composable` (e.g. `UiText.text()`) is called by the **consumer**.
- **Logging from the consumer.** Logging is `AppLogger.Mapping`'s job — it's part
  of the canonical pattern.
- **Conversion in the Repository or ViewModel.** Always lives in a mapper module.
  The cost is one function call; the win is consistency.
