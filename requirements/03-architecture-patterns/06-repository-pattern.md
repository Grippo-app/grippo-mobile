# Repository Pattern

Each business area in `:data-features:<x>` has **one** Repository: an `internal interface <X>Repository` + an `internal class <X>RepositoryImpl` annotated `@Single(binds = [<X>Repository::class])`.

The Repository is the **only** place that combines the network and the database. Above it (Feature / UseCase / ViewModel) only the contract is visible; below it (`<Product>Api`, `<X>Dao`) the implementation depends.

## Anatomy

```
:data-features:<x>/
  src/commonMain/kotlin/com/<org>/<product>/data/features/<x>/
    domain/
      <X>Repository.kt           // internal interface
      <X>FeatureImpl.kt          // internal class; @Single(binds = [<X>Feature::class])
    data/
      <X>RepositoryImpl.kt       // internal class; @Single(binds = [<X>Repository::class])
    <X>FeatureModule.kt          // public; @Module(includes = [BackendModule, DatabaseModule]) @ComponentScan
```

## The interface

```kotlin
internal interface NotesRepository {
    fun observeNotes(start: LocalDateTime, end: LocalDateTime): Flow<List<Note>>
    suspend fun getNotes(start: LocalDateTime, end: LocalDateTime): Result<Unit>
    suspend fun saveNote(note: Note): Result<String>
    suspend fun deleteNote(id: String): Result<Unit>
}
```

Rules:

- **`internal`** — invisible to `<X>FeatureImpl`'s callers.
- **Observe methods** return `Flow<Domain>` (cold or hot, but **never** `Flow<Result<...>>`).
- **Mutation methods** return `Result<T>` so errors propagate as values.
- **Parameter types are domain or primitives**, never DTOs/entities.
- **No `@Composable`-aware types.** No `UiText`, no `*FormatState`.

## The implementation

```kotlin
@Single(binds = [NotesRepository::class])
internal class NotesRepositoryImpl(
    private val api: <Product>Api,
    private val noteDao: NoteDao,
) : NotesRepository {

    override fun observeNotes(start: LocalDateTime, end: LocalDateTime): Flow<List<Note>> {
        val startUtc = DateTimeUtils.toUtcIso(start)
        val endUtc = DateTimeUtils.toUtcIso(end)
        return noteDao.observe(startUtc, endUtc)
            .map { packs -> packs.toDomain() }
    }

    override suspend fun getNotes(start: LocalDateTime, end: LocalDateTime): Result<Unit> {
        val startUtc = DateTimeUtils.toUtcIso(start)
        val endUtc = DateTimeUtils.toUtcIso(end)
        val response = api.getNotes(startUtc, endUtc)
        response.onSuccess { dtos ->
            val entities = dtos.toEntities()
            val ids = entities.map { it.id }
            if (entities.isEmpty()) {
                noteDao.deleteByCreatedAtRange(startUtc, endUtc)
            } else {
                noteDao.deleteByCreatedAtRangeExceptIds(startUtc, endUtc, ids)
                noteDao.insertAll(entities)
            }
        }
        return response.map { }
    }

    override suspend fun saveNote(note: Note): Result<String> {
        val body = note.toBody()
        val response = api.setNote(body)
        return response.mapCatching { dto ->
            val id = dto.id ?: error("note id missing")
            // refresh local
            api.getNote(id).onSuccess { full ->
                full.toEntityOrNull()?.let { noteDao.insert(it) }
            }
            id
        }
    }

    override suspend fun deleteNote(id: String): Result<Unit> {
        val response = api.deleteNote(id)
        response.onSuccess { noteDao.delete(id) }
        return response.map { }
    }
}
```

Rules:

- **`@Single(binds = [<X>Repository::class])`** — exactly this annotation. No factory, no scope.
- **Constructor injection** — all deps come from Koin via constructor. No `getKoin().get()`.
- **Observe returns DAO flow + domain mapping** — never API.
- **Mutate calls API; on success, updates DAO** — never write to DAO speculatively.
- **Range reconciliation** for paged/ranged observes — `deleteByCreatedAtRangeExceptIds` after fetch.
- **Date normalization** at the API/DAO boundary — `DateTimeUtils.toUtcIso(...)` and `DateTimeUtils.toLocalDateTime(...)`.

## Common operations

### Single-record observe

```kotlin
override fun observeUser(): Flow<User?> =
    userDao.observeActive()
        .map { entity -> entity?.toDomain() }
```

### Single-record get-and-cache

```kotlin
override suspend fun getUser(): Result<Unit> {
    val response = api.getUser()
    response.onSuccess { dto ->
        dto.toEntityOrNull()?.let { userDao.insertOrUpdate(it) }
    }
    return response.map { }
}
```

### List get-and-reconcile (the canonical pattern)

```kotlin
override suspend fun getNotes(start: LocalDateTime, end: LocalDateTime): Result<Unit> {
    val response = api.getNotes(...)
    response.onSuccess { dtos ->
        val entities = dtos.toEntities()
        val ids = entities.map { it.id }
        if (entities.isEmpty()) {
            noteDao.deleteByCreatedAtRange(...)
        } else {
            noteDao.deleteByCreatedAtRangeExceptIds(..., ids)
            noteDao.insertAll(entities)
        }
    }
    return response.map { }
}
```

The "delete except IDs returned" step is critical. Without it, deletes on another device leave stale rows locally indefinitely.

### Create-or-update

```kotlin
override suspend fun saveNote(note: Note): Result<String> {
    val body = note.toBody()
    val response = if (note.id == null) api.createNote(body) else api.updateNote(note.id, body)
    return response.mapCatching { idResponse ->
        val id = idResponse.id ?: error("missing id")
        api.getNote(id).onSuccess { full ->
            full.toEntityOrNull()?.let { noteDao.insert(it) }
        }
        id
    }
}
```

The follow-up `getNote(id)` refreshes the local cache with **server-canonical** data (timestamps set by backend, computed fields). Without it, the local copy would lag.

### Drafts (local-only)

```kotlin
override fun observeDraft(id: String): Flow<DraftNote?> =
    draftNoteDao.observe(id).map { it?.toDomain() }

override suspend fun saveDraft(draft: DraftNote): Result<Unit> = runCatching {
    val entity = draft.toEntity()    // domain-to-entity mapper
    draftNoteDao.insertOrUpdate(entity)
}

override suspend fun deleteDraft(id: String): Result<Unit> = runCatching {
    draftNoteDao.delete(id)
}
```

Drafts never round-trip through the server. They live in a `draft_*` table with cascade-delete on the parent user. The `domain-to-entity` mapper exists for this purpose.

## Why Repository + FeatureImpl (vs just one class)

| Layer | Visibility | Purpose |
|---|---|---|
| `<X>Repository` | `internal interface` | Data composition: API + DAO + mappers |
| `<X>RepositoryImpl` | `internal class` | Implementation |
| `<X>Feature` | `public interface` | UI-facing contract; pure types |
| `<X>FeatureImpl` | `internal class` | Composition (sometimes) of multiple repositories |

For simple features, `FeatureImpl` delegates to `Repository` one-to-one. For composing features, the layer is necessary — examples: a `NoteDigestUseCase` (composes `NotesRepository` + `TagRepository` to derive aggregate metrics) or a `LoginUseCase.executeEmail/executeGoogle/executeApple` (composes `AuthorizationRepository` + `UserRepository` to swap tokens and refresh the active profile in one call).

Both layers cost minimally (each is a thin class) and keep the seven-file convention consistent across the codebase.

## Module DI

```kotlin
// :data-features:notes/NotesFeatureModule.kt
@Module(includes = [BackendModule::class, DatabaseModule::class])
@ComponentScan
public class NotesFeatureModule
```

`@ComponentScan` discovers `NotesRepositoryImpl` and `NotesFeatureImpl` via their annotations. `includes = [BackendModule, DatabaseModule]` makes `<Product>Api` and DAOs available via transitive imports.

## Anti-patterns

- **`Repository` is `public`.** It must be `internal`.
- **`@Single` without `binds = [<X>Repository::class]`.** Koin won't resolve the interface.
- **`getKoin().get()` inside the Repository.** Constructor-inject everything.
- **Writing to DAO before `api.onSuccess { ... }`.** Speculative writes leave the cache inconsistent on failure.
- **Inline mappers (`val entity = NoteEntity(id = dto.id ?: ..., ...)`)** — use `:data-mappers:*`.
- **Returning `Flow<DTO>`.** DTOs never escape this layer.
- **`suspend fun observeX()`** — observe is hot; if you need to block on first emission, use `observeX().first()` at the call site.
- **Catching exceptions inside the Repository.** Let them propagate via `runCatching`/`Result`. The Repository's job is to translate API + DAO; error handling is the ViewModel's job (via `ErrorProvider`).
