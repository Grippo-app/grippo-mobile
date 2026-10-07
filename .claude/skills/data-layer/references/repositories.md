# Repositories and data flow (UI → network)

Self-contained reference for the repository pattern and data-flow rules.

> **Illustrative domain.** Code uses `Note` / `Tag` / `User` as the generic
> `<Entity>` / `<RelatedEntity>`. Substitute identifiers from your product domain.

The path from a UI tap to an HTTP request is **explicit at every layer**. No hidden
state, no service locators, no shortcut.

---

## Layers (NORMATIVE)

```
ViewModel
  → <X>Feature (interface in :feature-api)
    → <X>FeatureImpl (in :data-features:<x>, @Single(binds=[<X>Feature::class]))
      → <X>Repository (internal interface)
        → <X>RepositoryImpl (@Single(binds=[<X>Repository::class]))
          → <Product>Api.<method>(body): Result<DTO>     // HTTP via :data-services:backend
          → <X>Dao.<query>(): Flow<Pack>              // Room via :data-services:database
```

Each arrow is **one** module boundary; each box is **one** class with **one**
responsibility.

The Repository is the **only** place that combines the network and the database. Above it
(Feature / UseCase / ViewModel) only the contract is visible; below it (`<Product>Api`,
`<X>Dao`) the implementation depends.

### Anatomy (REFERENCE)

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

---

## ViewModel layer (context)

The ViewModel **never** touches `<Product>Api`, `Database`, `Dao`, `Repository` — only the
`<X>Feature` interface. `observe...()` returns `Flow<Domain>`, collected inside `init { }`
with `.safeLaunch()`. `get...()` returns `Result<T>`, called inside
`safeLaunch(loader = ...) { ... }` with `.getOrThrow()` so errors flow through `ErrorProvider`.
Map domain to state via `:data-mappers:domain-to-state` (`it.toState()`).

```kotlin
internal class NotesListViewModel(
    private val noteFeature: NoteFeature,
) : BaseViewModel<...>(...), NotesListContract {

    init {
        noteFeature.observeNotes(state.value.range.from, state.value.range.to)
            .map { it.toState() }                        // domain-to-state mapper
            .onEach { listState -> update { it.copy(items = listState) } }
            .safeLaunch()

        safeLaunch(loader = NotesListLoader.LoadingNotes) {
            noteFeature.getNotes(
                start = state.value.range.from,
                end = state.value.range.to,
            ).getOrThrow()
        }
    }
}
```

---

## Feature interface (MUST)

```kotlin
// :data-features:feature-api
public interface NoteFeature {
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
- No `Flow<Result<T>>` — a single failure in the stream would break observation; observe is
  for cached data that always succeeds.

---

## Feature implementation (EXAMPLE)

```kotlin
// :data-features:notes
@Single(binds = [NoteFeature::class])
internal class NoteFeatureImpl(
    private val repository: NoteRepository,
) : NoteFeature {

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

For most features, `FeatureImpl` is a thin pass-through to `Repository`. The layer exists for
cases where the Feature composes **multiple** repositories, adds **post-fetch logic**
(analytics, side effects, cache warming), or exposes **cross-domain UseCases**. When the
Feature does nothing beyond delegation, that's fine — the layer is consistent across the
codebase.

---

## Repository — interface (MUST)

```kotlin
internal interface NoteRepository {
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

---

## Repository — implementation (EXAMPLE + MUST rules)

```kotlin
@Single(binds = [NoteRepository::class])
internal class NoteRepositoryImpl(
    private val api: <Product>Api,
    private val noteDao: NoteDao,
    private val draftNoteDao: DraftNoteDao,
) : NoteRepository {

    override fun observeNotes(start: LocalDateTime, end: LocalDateTime): Flow<List<Note>> {
        val startUtc = DateTimeUtils.toUtcIso(start)
        val endUtc = DateTimeUtils.toUtcIso(end)
        return noteDao.get(from = startUtc, to = endUtc)
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

Implementation rules (MUST):

- **Repository is `internal`.** Visible only inside its module.
- **`@Single(binds = [<X>Repository::class])`** — exactly this annotation. No factory, no scope.
- **Constructor injection only.** No `getKoin().get()`, no service locator.
- **Observe returns DAO flow + domain mapping** — never from the API.
- **Mutate calls API; on success, updates DAO** — never write to DAO speculatively. All DAO
  mutations are inside `response.onSuccess { ... }`.
- **Range reconciliation pattern** for collection observers — `deleteByCreatedAtRangeExceptIds`
  after a fetch. Removes drift.
- **Mappers, never inline conversion.** `dtos.toEntities()`, `packs.toDomain()`, `note.toBody()`.
- **Date normalization at the boundary.** Convert `LocalDateTime` to `toUtcIso` before storing
  or querying; `DateTimeUtils.toLocalDateTime(...)` on the way out.

---

## Common operations (EXAMPLE)

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

The "delete except IDs returned" step is critical. Without it, deletes on another device leave
stale rows locally indefinitely.

### Create-or-update

```kotlin
override suspend fun saveNote(note: Note): Result<String> {
    val body = note.toBody()
    val response = if (note.id == null) api.setNote(body) else api.updateNote(note.id, body)
    return response.mapCatching { idResponse ->
        val id = idResponse.id ?: error("missing id")
        api.getNote(id).onSuccess { full ->
            full.toEntityOrNull()?.let { noteDao.insert(it) }
        }
        id
    }
}
```

The follow-up `getNote(id)` refreshes the local cache with **server-canonical** data
(timestamps set by backend, computed fields). Without it, the local copy would lag.

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

Drafts never round-trip through the server. They live in a `draft_*` table with cascade-delete
on the parent user. The `domain-to-entity` mapper exists for this purpose.

---

## Service layer (EXAMPLE)

> Full `<Product>Api` shape in [`dtos-and-api.md`](dtos-and-api.md), `BackendClient` in [`backend-client.md`](backend-client.md).

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

`:data-services:database`'s `NoteDao`:

```kotlin
@Dao
public interface NoteDao {
    @Transaction
    @Query("SELECT * FROM note WHERE createdAt BETWEEN :from AND :to ORDER BY createdAt DESC")
    public fun get(from: String, to: String): Flow<List<NotePack>>

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

Type promotion lives at the entity→domain (and dto→domain) boundary: timestamp `String` →
`LocalDateTime`, minutes `Long` → `Duration`, and a closed-set `String` → its domain `<X>Enum`
via `<X>Enum.of(...)` (drop-on-unknown guarded with
`AppLogger.Mapping.log(...) ?: return null`; default-on-unknown called bare). The DTO and Entity
keep the raw `String?` / `Long` replica.

---

## Loaders, errors, and the UI surface (NORMATIVE)

> Full error path in [`error-pipeline.md`](error-pipeline.md).

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
                                            ErrorProvider.provide(exception, onError)
                                                          ↓
                                  DialogController.show(DialogConfig.ErrorDisplay(...))
                                                          ↓
                                  loader removed from loaders StateFlow
```

The UI sees: `state.items` (updated via DAO observer), `loaders` (contains `MyLoader.FetchA`
while the call is in flight), and a bottom-sheet error dialog if the call fails.

---

## Module DI (EXAMPLE)

```kotlin
// :data-features:notes/NotesFeatureModule.kt
@Module(includes = [BackendModule::class, DatabaseModule::class])
@ComponentScan
public class NotesFeatureModule
```

`@ComponentScan` discovers `NoteRepositoryImpl` and `NoteFeatureImpl` via their annotations.
`includes = [BackendModule, DatabaseModule]` makes `<Product>Api` and DAOs available via
transitive imports.

---

## Why Repository + FeatureImpl (REFERENCE)

| Layer | Visibility | Purpose |
|---|---|---|
| `<X>Repository` | `internal interface` | Data composition: API + DAO + mappers |
| `<X>RepositoryImpl` | `internal class` | Implementation |
| `<X>Feature` | `public interface` | UI-facing contract; pure types |
| `<X>FeatureImpl` | `internal class` | Composition (sometimes) of multiple repositories |

For simple features, `FeatureImpl` delegates to `Repository` one-to-one. For composing
features, the layer is necessary — examples: a `NoteDigestUseCase` (composes `NoteRepository` +
`TagRepository` to derive aggregate metrics) or a `LoginUseCase.executeEmail/executeGoogle/executeApple`
(composes `AuthorizationRepository` + `UserRepository` to swap tokens and refresh the active
profile in one call). Both layers cost minimally and keep the seven-file convention consistent.

---

## Anti-patterns (MUST)

- **`Flow<Result<T>>`** — observation must succeed; failures belong in `Result` returns, not in streams.
- **Returning DTOs from a Repository / `Flow<DTO>`** — Repository's job is to translate DTO → domain; DTOs never escape this layer.
- **Observing the API directly.** API is request/response; observation is for cached data.
- **`@Single` on a Repository without `binds = [...]`** — the interface won't be wired.
- **Inline mappers in ViewModel or Repository** (`val entity = NoteEntity(id = dto.id ?: ..., ...)`) — mappers live in their dedicated `:data-mappers:*` modules.
- **`Repository` exposed publicly.** It must be `internal` — only the `<X>Feature` interface escapes the module.
- **Triggering side effects (analytics, logs) inside a mapper.** Mappers are pure; side effects live in Repository/Feature/VM.
- **`getKoin().get()` inside the Repository.** Constructor-inject everything.
- **Writing to DAO before `api.onSuccess { ... }`.** Speculative writes leave the cache inconsistent on failure.
- **`suspend fun observeX()`** — observe is hot; if you need to block on first emission, use `observeX().first()` at the call site.
- **Catching exceptions inside the Repository.** Let them propagate via `runCatching`/`Result`. Error handling is the ViewModel's job (via `ErrorProvider`).
