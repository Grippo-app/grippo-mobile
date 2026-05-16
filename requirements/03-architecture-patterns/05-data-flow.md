# Data Flow: UI → Network

The path from a UI tap to an HTTP request is **explicit at every layer**. No hidden state, no service locators, no shortcut.

## Layers

```
ViewModel
  → <X>Feature (interface in :feature-api)
    → <X>FeatureImpl (in :data-features:<x>, @Single(binds=[Feature::class]))
      → <X>Repository (internal interface)
        → <X>RepositoryImpl (@Single(binds=[Repository::class]))
          → <Product>Api.<method>(body): Result<DTO>     // HTTP via :data-services:backend
          → <X>Dao.<query>(): Flow<Pack>              // Room via :data-services:database
```

Each arrow is **one** module boundary; each box is **one** class with **one** responsibility.

## ViewModel layer

```kotlin
internal class NotesListViewModel(
    private val notesFeature: NotesFeature,
) : BaseViewModel<...>(...), NotesListContract {

    init {
        // Observe — Flow of domain, never blocks
        notesFeature.observeNotes(state.value.range.from, state.value.range.to)
            .map { it.toState() }                        // domain-to-state mapper
            .onEach { listState ->
                update { it.copy(items = listState) }
            }
            .safeLaunch()

        // Initial fetch — Result, triggers a loader
        safeLaunch(loader = NotesListLoader.LoadingNotes) {
            notesFeature.getNotes(
                start = state.value.range.from,
                end = state.value.range.to,
            ).getOrThrow()
        }
    }

    override fun onRangeChange(range: DateRange) {
        update { it.copy(range = range) }
        safeLaunch(loader = NotesListLoader.LoadingNotes) {
            notesFeature.getNotes(start = range.from, end = range.to).getOrThrow()
        }
    }
}
```

Rules at this layer:

- The ViewModel **never** touches `<Product>Api`, `Database`, `Dao`, `Repository`. Only `<X>Feature` (interface).
- `observe...()` returns `Flow<Domain>` — collect it inside `init { }` with `.safeLaunch()`.
- `get...()` returns `Result<T>` — call it inside `safeLaunch(loader = ...) { ... }` with `.getOrThrow()` so errors flow through `ErrorProvider`.
- Map domain to state via `:data-mappers:domain-to-state` (e.g. `it.toState()`).

## Feature interface

```kotlin
// :data-features:feature-api
public interface NotesFeature {
    public fun observeNotes(start: LocalDateTime, end: LocalDateTime): Flow<List<Note>>
    public suspend fun getNotes(start: LocalDateTime, end: LocalDateTime): Result<Unit>
    public suspend fun saveNote(note: Note): Result<String>
    public suspend fun deleteNote(id: String): Result<Unit>
}
```

Rules:

- Methods that **observe** return `Flow<Domain>` (never `Result<Flow<...>>`).
- Methods that **mutate** return `Result<T>` (never `T` directly — errors are values).
- Parameters are domain types or primitives; **never** DTOs or entities.
- No suspending observers (`suspend fun observe...` is wrong — observe is hot, get/set is suspend).
- No `Flow<Result<T>>` — a single failure in the stream would break observation; observe is for cached data that always succeeds.

## Feature implementation

```kotlin
// :data-features:notes
@Single(binds = [NotesFeature::class])
internal class NotesFeatureImpl(
    private val repository: NotesRepository,
) : NotesFeature {

    override fun observeNotes(start: LocalDateTime, end: LocalDateTime): Flow<List<Note>> =
        repository.observeNotes(start, end)

    override suspend fun getNotes(start: LocalDateTime, end: LocalDateTime): Result<Unit> =
        repository.getNotes(start, end)

    override suspend fun saveNote(note: Note): Result<String> =
        repository.saveNote(note)

    override suspend fun deleteNote(id: String): Result<Unit> =
        repository.deleteNote(id)
}
```

For most features, `FeatureImpl` is a thin pass-through to `Repository`. The layer exists for cases where:

- The Feature composes **multiple** repositories (`<X>FeatureImpl` injects `<X>Repository` + `<Y>Repository`).
- The Feature adds **post-fetch logic** (analytics, side effects, cache warming).
- The Feature exposes **cross-domain UseCases**.

When the Feature does nothing beyond delegation, that's fine — the layer is consistent across the codebase.

## Repository

```kotlin
// :data-features:notes
internal interface NotesRepository {
    fun observeNotes(start: LocalDateTime, end: LocalDateTime): Flow<List<Note>>
    suspend fun getNotes(start: LocalDateTime, end: LocalDateTime): Result<Unit>
    suspend fun saveNote(note: Note): Result<String>
    suspend fun deleteNote(id: String): Result<Unit>
}

@Single(binds = [NotesRepository::class])
internal class NotesRepositoryImpl(
    private val api: <Product>Api,
    private val noteDao: NoteDao,
    private val draftNoteDao: DraftNoteDao,
) : NotesRepository {

    override fun observeNotes(start: LocalDateTime, end: LocalDateTime): Flow<List<Note>> {
        val startUtc = DateTimeUtils.toUtcIso(start)
        val endUtc = DateTimeUtils.toUtcIso(end)
        return noteDao.observe(from = startUtc, to = endUtc)
            .map { packs -> packs.toDomain() }      // entity-to-domain mapper
    }

    override suspend fun getNotes(start: LocalDateTime, end: LocalDateTime): Result<Unit> {
        val startUtc = DateTimeUtils.toUtcIso(start)
        val endUtc = DateTimeUtils.toUtcIso(end)
        val response = api.getNotes(start = startUtc, end = endUtc)
        response.onSuccess { dtos ->
            val entities = dtos.toEntities()        // dto-to-entity mapper
            val actualIds = entities.map { it.id }

            // Range reconciliation — delete stale, keep what server returned
            if (entities.isEmpty()) {
                noteDao.deleteByCreatedAtRange(startUtc, endUtc)
            } else {
                noteDao.deleteByCreatedAtRangeExceptIds(startUtc, endUtc, actualIds)
                noteDao.insertAll(entities)
            }
        }
        return response.map { }
    }

    override suspend fun saveNote(note: Note): Result<String> {
        val body = note.toBody()                // domain-to-dto mapper
        val response = api.setNote(body)
        return response.mapCatching { dto ->
            val id = dto.id ?: error("note id missing from response")
            // Re-fetch to refresh local cache
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

- **Repository is `internal`.** Visible only inside its module.
- **`@Single(binds = [<X>Repository::class])`** — exact Koin binding.
- **Constructor injection only.** No `getKoin().get()`, no service locator.
- **Range reconciliation pattern** for collection observers — delete-except-ids after a fetch. Removes drift.
- **All DAO mutations are inside `response.onSuccess { ... }`.** Never assume the server succeeded; never write speculative changes.
- **Mappers, never inline conversion.** `dtos.toEntities()`, `packs.toDomain()`, `note.toBody()`.
- **Date normalization at the boundary.** Convert `LocalDateTime` to `toUtcIso` before storing or querying — see `10-toolkit/06-date-utils.md`.

## Service layer

`:data-services:backend`'s `<Product>Api`:

```kotlin
@Single
public class <Product>Api internal constructor(private val client: BackendClient) {

    public suspend fun getNotes(start: String, end: String): Result<List<NoteResponse>> =
        request(
            method = HttpMethod.Get,
            path = "/notes",
            queryParams = mapOf("start" to start, "end" to end),
        )

    private suspend inline fun <reified T> request(
        method: HttpMethod,
        path: String,
        body: Any? = null,
        queryParams: Map<String, String>? = null,
    ): Result<T> = runCatching {
        client.invoke(method, path, body, queryParams).body()
    }
}
```

See `06-data-layer/03-product-api-and-dtos.md` for the full pattern.

`:data-services:database`'s `NoteDao`:

```kotlin
@Dao
public interface NoteDao {
    @Transaction
    @Query("SELECT * FROM note WHERE createdAt BETWEEN :from AND :to ORDER BY createdAt DESC")
    public fun observe(from: String, to: String): Flow<List<NotePack>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insert(entity: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertAll(entities: List<NoteEntity>)

    @Query("DELETE FROM note WHERE id = :id")
    public suspend fun delete(id: String)

    @Query("DELETE FROM note WHERE createdAt BETWEEN :from AND :to")
    public suspend fun deleteByCreatedAtRange(from: String, to: String)

    @Query("DELETE FROM note WHERE createdAt BETWEEN :from AND :to AND id NOT IN (:keepIds)")
    public suspend fun deleteByCreatedAtRangeExceptIds(from: String, to: String, keepIds: List<String>)
}
```

## Mapper layer

Each conversion direction is one module. The Repository pulls mappers from `:data-mappers:*` (e.g. `:dto-to-entity`, `:entity-to-domain`, `:domain-to-dto`).

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

See `07-mappers/` for full conventions.

## Loaders, errors, and the UI surface

```
ViewModel.safeLaunch(loader = MyLoader.FetchA) { feature.getA().getOrThrow() }
                                                          ↓
                                            BackendClient → HTTP request
                                                          ↓
                                            Result<DTO>: Success or AppError
                                                          ↓
                                  on success: repository updates DAO
                                  on failure: getOrThrow throws → safeLaunch catches
                                                          ↓
                                            ErrorProvider.provide(exception)
                                                          ↓
                                  DialogController.show(DialogConfig.ErrorDisplay(...))
                                                          ↓
                                  loader removed from loaders StateFlow
```

The UI sees:
- `state.items` (updated via DAO observer).
- `loaders` (contains `MyLoader.FetchA` while the call is in flight).
- A bottom-sheet error dialog if the call fails.

## Anti-patterns

- **`Flow<Result<T>>`** — observation must succeed; failures belong in `Result` returns, not in streams.
- **Returning DTOs from a Repository.** Repository's job is to translate DTO → domain.
- **Observing API directly.** API is request/response; observation is for cached data.
- **`@Single` on a Repository without `binds = [...]`** — the interface won't be wired.
- **Inline mappers in ViewModel or Repository.** Mappers live in their dedicated modules.
- **`Repository` exposed publicly.** Only the `<X>Feature` interface escapes the module.
- **Triggering side effects (analytics, logs) inside a mapper.** Mappers are pure; side effects live in Repository/Feature/VM.
