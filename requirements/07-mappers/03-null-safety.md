# Null Safety in Mappers

DTOs are entirely nullable. Entity scalar columns and domain models are non-null by default. The mappers are where the gap is bridged. The convention is:

> If a **required** field (target is non-null) is null on the source, log it and drop the record. If an **optional** field (target is nullable, or there's an equivalent server alias) is null, pass through or coalesce — do not invent a default for required columns.

## `AppLogger.Mapping.log` — the bridge

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

Behavior:

- If `value != null`: returns it unchanged.
- If `value == null`: resolves caller `(file:line)`, invokes `msg()` (lazy), writes one `[MAPPING] $msg (file:line)` entry to the file log, returns `null`.

Usage:

```kotlin
val entityId = AppLogger.Mapping.log(dto.id) { "NoteResponse.id is null" } ?: return null
```

The `?: return null` is mandatory after every required-field log. The pattern says "this value is required; if missing, drop the record". Use a prefixed local name (`entityId`, `domainId`, `mappedId`) to avoid shadowing same-named DTO/state fields inside the receiver scope.

## What is required vs optional

A field is **required** when:

- The entity/domain model has it as a non-null field. **This is the default stance** — if the target column or property is non-null, the source must supply a value or the row is dropped.
- It is part of the primary key or a foreign-key.
- It is needed to construct any further objects (e.g. a missing `id` means the row is uninsertable).

A field is **optional** when:

- **The target field itself is nullable** (e.g. `ItemEntity.optionalAmount: Float?` — the column is nullable, so the mapper just passes `optionalAmount` through).
- It has a **server-emitted alias** that can satisfy it (`profileId ?: userId` — the response carries a legacy `userId` for backward compatibility).

Do **not** fabricate defaults like `amount ?: 0f` for non-null target columns just because the value might be missing — log it and drop the row. Required scalar fields stay required in the reference mappers.

## Required field: `?: return null`

```kotlin
val entityId = AppLogger.Mapping.log(id) { "NoteResponse.id is null" } ?: return null
val entityProfileId = AppLogger.Mapping.log(profileId ?: userId) { "NoteResponse.profileId is null" } ?: return null
val entityCreatedAt = AppLogger.Mapping.log(createdAt) { "NoteResponse.createdAt is null" } ?: return null
```

Each required field is checked **separately** with a **specific** log message. This lets the team diagnose **which** field went missing — not just "some field".

## Optional field: pass-through or alias coalesce

```kotlin
// Entity column itself is nullable — pass DTO value through unchanged.
optionalAmount = optionalAmount,
subtitle = subtitle,
note = note,

// Two equivalent server fields, take whichever is present.
val entityProfileId = AppLogger.Mapping.log(profileId ?: userId) {
    "NoteResponse.profileId is null"
} ?: return null
```

Rules:

- If the entity field is nullable, just write `entityField = sourceField` — Kotlin already permits the null.
- If the server provides two equivalent fields (legacy alias migration), use `a ?: b` inside the `AppLogger.Mapping.log(...)` call.
- Do not invent numeric or string defaults for required columns.

## Composing nested mappers

Each level is an independent leaf mapper — the parent ID lives in the DTO of the child, so the child reads it from itself:

```kotlin
public fun NoteResponse.toEntityOrNull(): NoteEntity? { /* ... */ }

public fun TagResponse.toEntityOrNull(): TagEntity? {
    val entityId = AppLogger.Mapping.log(id) { "TagResponse.id is null" } ?: return null
    val entityNoteId = AppLogger.Mapping.log(noteId) { "TagResponse.noteId is null" } ?: return null
    // ...
    return TagEntity(id = entityId, noteId = entityNoteId, /* ... */)
}

// Repository
val note = dto.toEntityOrNull() ?: return null
val tags = dto.tags.toEntities()
val items = dto.tags.flatMap { it.items }.toEntities()
noteDao.insertOrReplace(note, tags, items)
```

The Repository flat-maps children out of the parent DTO and feeds them to leaf `toEntities()` calls. Each child carries its own parent FK (`noteId`, `tagId`) from the wire payload, so it logs and drops on its own without taking a `parentId` parameter.

**Exception — `:data-mappers:domain-to-entity` (drafts).** Drafts have no wire payload; ids are generated locally via `Uuid.random().toString()`. The parent therefore must hand its freshly-minted id down to the child mapper as a parameter:

```kotlin
public fun DraftNote.toEntity(profileId: String): DraftNotePack {
    val id = Uuid.random().toString()
    val note = DraftNoteEntity(id = id, profileId = profileId, /* ... */)
    return DraftNotePack(note = note, tags = tags.map { it.toEntity(id) })
}

public fun DraftTag.toEntity(noteId: String): DraftTagPack { /* ... */ }
public fun SetItem.toEntity(tagId: String): DraftItemEntity { /* ... */ }
```

Domain models have non-null fields, so there is **no `AppLogger.Mapping.log`** in this direction — null-and-drop has nothing to do.

## What `AppLogger.Mapping` produces

A file log entry like:

```
[MAPPING] NoteResponse.id is null (NoteMapper.kt:12)
```

The file log is written through `:toolkit:logger`'s `LogDispatcher` to a single append-only file (Android JVM: `${user.home}/<product>/logs/app.log`, with `${java.io.tmpdir}` and `/tmp` as fallbacks; iOS: `${NSTemporaryDirectory()}/<product>/logs/app.log`). No rotation — the file grows until `AppLogger.clearLogFile()` is called from the debug screen. Surfaced via `AppLogger.logFileContentsByCategory(): Map<String, List<String>>` keyed by category name (`MAPPING`, `NETWORK`, `NAVIGATION`, `ERROR`, `WARNING`).

When the team sees an unexpected drop ("user's note list is shorter than expected"), the log entries pinpoint the missing field. Often the cause is a backend regression — a field that used to be always set is sometimes null now.

## DTO → Domain (no entity step)

Same rules as DTO → Entity. The example below is from a one-shot tag read (the related `TagDetail` is passed in by the caller because the wire payload doesn't include it):

```kotlin
public fun TagResponse.toDomainOrNull(detail: TagDetail): Tag? {
    val domainId = AppLogger.Mapping.log(id) { "TagResponse.id is null" } ?: return null
    val domainName = AppLogger.Mapping.log(name) { "TagResponse.name is null" } ?: return null
    val domainAmount = AppLogger.Mapping.log(amount) { "TagResponse.amount is null" } ?: return null
    val domainCreatedAt = AppLogger.Mapping.log(createdAt) { "TagResponse.createdAt is null" } ?: return null

    return Tag(
        id = domainId,
        name = domainName,
        amount = domainAmount,
        items = items.toDomain(),
        detail = detail,
        createdAt = DateTimeUtils.toLocalDateTime(domainCreatedAt),
    )
}

public fun List<TagResponse>.toDomain(detail: TagDetail): List<Tag> =
    mapNotNull { it.toDomainOrNull(detail) }
```

Use this direction when:

- The data is **one-shot** (analytics, reports, summaries) — no caching benefit.
- The data has no canonical entity (transient computed result, e.g. a one-off report DTO).

For everything cached, prefer the `DTO → Entity → Domain` chain so observations work.

## Entity → Domain (scalars: no null checks; relations: yes)

```kotlin
public fun NotePack.toDomain(): Note = Note(
    id = note.id,
    duration = note.duration.minutes,
    createdAt = DateTimeUtils.toLocalDateTime(note.createdAt),
    amount = note.amount,
    tags = tags.toDomain(),
)

public fun TagPack.toDomain(): Tag? {
    val mappedDetail = AppLogger.Mapping.log(detail?.toDomain()) {
        "TagPack detail by ${tag.detailId} is null"
    } ?: return null
    // ... assemble Tag(...)
}
```

Two cases:

- **Scalar columns** — no `?: return null`. Room enforces non-null at insert, so read them directly. If a scalar somehow is null at runtime, that's a bug — let it crash with a clear `NullPointerException`.
- **Embedded relations and enum string parses** — `AppLogger.Mapping.log(...) ?: return null`. A relation (`@Relation val detail: ...?`) may be absent because the related row was deleted or never inserted; an enum string may not parse if the DB pre-dates a renamed case. Both are recoverable: drop the row and log.

The per-row mapper returns `T?` when it consumes a relation or enum-string; the plural variant uses `mapNotNull`.

## Anti-patterns

- **`!!` on a nullable DTO field.** Forbidden. Use `?: return null`.
- **Defaulting required fields to placeholder values** (`val id = dto.id ?: "missing"`). The row is invalid; drop it, don't fabricate.
- **Logging once for "the whole DTO is null"** instead of per-field. Loses diagnostic value.
- **Swallowing nulls silently.** Always log via `AppLogger.Mapping.log`.
- **Catching exceptions** to convert them into null. Not the right tool here — `try/catch` is for exceptions, `?: return null` is for nullability.
- **Mixing required-field-check style with Elvis fallback style** inconsistently. Be deliberate: required → `?: return null`; nullable target → pass-through; server-alias coalesce → `(a ?: b)` inside the same `AppLogger.Mapping.log(...) ?: return null` call.
