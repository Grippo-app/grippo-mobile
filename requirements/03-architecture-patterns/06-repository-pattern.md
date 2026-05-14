# Repository Pattern

Each business area in `:data-features:<x>` has **one** Repository: an `internal interface <X>Repository` + an `internal class <X>RepositoryImpl` annotated `@Single(binds = [<X>Repository::class])`.

The Repository is the **only** place that combines the network and the database. Above it (Feature / UseCase / ViewModel) only the contract is visible; below it (`GrippoApi`, `<X>Dao`) the implementation depends.

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
internal interface TrainingsRepository {
    fun observeTrainings(start: LocalDateTime, end: LocalDateTime): Flow<List<Training>>
    suspend fun getTrainings(start: LocalDateTime, end: LocalDateTime): Result<Unit>
    suspend fun saveTraining(training: Training): Result<String>
    suspend fun deleteTraining(id: String): Result<Unit>
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
@Single(binds = [TrainingsRepository::class])
internal class TrainingsRepositoryImpl(
    private val api: GrippoApi,
    private val trainingDao: TrainingDao,
) : TrainingsRepository {

    override fun observeTrainings(start: LocalDateTime, end: LocalDateTime): Flow<List<Training>> {
        val startUtc = DateTimeUtils.toUtcIso(start)
        val endUtc = DateTimeUtils.toUtcIso(end)
        return trainingDao.observe(startUtc, endUtc)
            .map { packs -> packs.toDomain() }
    }

    override suspend fun getTrainings(start: LocalDateTime, end: LocalDateTime): Result<Unit> {
        val startUtc = DateTimeUtils.toUtcIso(start)
        val endUtc = DateTimeUtils.toUtcIso(end)
        val response = api.getTrainings(startUtc, endUtc)
        response.onSuccess { dtos ->
            val entities = dtos.toEntities()
            val ids = entities.map { it.id }
            if (entities.isEmpty()) {
                trainingDao.deleteByCreatedAtRange(startUtc, endUtc)
            } else {
                trainingDao.deleteByCreatedAtRangeExceptIds(startUtc, endUtc, ids)
                trainingDao.insertAll(entities)
            }
        }
        return response.map { }
    }

    override suspend fun saveTraining(training: Training): Result<String> {
        val body = training.toBody()
        val response = api.setTraining(body)
        return response.mapCatching { dto ->
            val id = dto.id ?: error("training id missing")
            // refresh local
            api.getTraining(id).onSuccess { full ->
                full.toEntityOrNull()?.let { trainingDao.insert(it) }
            }
            id
        }
    }

    override suspend fun deleteTraining(id: String): Result<Unit> {
        val response = api.deleteTraining(id)
        response.onSuccess { trainingDao.delete(id) }
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
override suspend fun getTrainings(start: LocalDateTime, end: LocalDateTime): Result<Unit> {
    val response = api.getTrainings(...)
    response.onSuccess { dtos ->
        val entities = dtos.toEntities()
        val ids = entities.map { it.id }
        if (entities.isEmpty()) {
            trainingDao.deleteByCreatedAtRange(...)
        } else {
            trainingDao.deleteByCreatedAtRangeExceptIds(..., ids)
            trainingDao.insertAll(entities)
        }
    }
    return response.map { }
}
```

The "delete except IDs returned" step is critical. Without it, deletes on another device leave stale rows locally indefinitely.

### Create-or-update

```kotlin
override suspend fun saveTraining(training: Training): Result<String> {
    val body = training.toBody()
    val response = if (training.id == null) api.createTraining(body) else api.updateTraining(training.id, body)
    return response.mapCatching { idResponse ->
        val id = idResponse.id ?: error("missing id")
        api.getTraining(id).onSuccess { full ->
            full.toEntityOrNull()?.let { trainingDao.insert(it) }
        }
        id
    }
}
```

The follow-up `getTraining(id)` refreshes the local cache with **server-canonical** data (timestamps set by backend, computed fields like volume/intensity). Without it, the local copy would lag.

### Drafts (local-only)

```kotlin
override fun observeDraft(id: String): Flow<DraftTraining?> =
    draftTrainingDao.observe(id).map { it?.toDomain() }

override suspend fun saveDraft(draft: DraftTraining): Result<Unit> = runCatching {
    val entity = draft.toEntity()    // domain-to-entity mapper
    draftTrainingDao.insertOrUpdate(entity)
}

override suspend fun deleteDraft(id: String): Result<Unit> = runCatching {
    draftTrainingDao.delete(id)
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

For simple features, `FeatureImpl` delegates to `Repository` one-to-one. For complex features (e.g. `RecalculateGoalProgressUseCase` that touches `TrainingsRepository` + `GoalRepository` + `UserRepository`), the layer is necessary.

Both layers cost minimally (each is a thin class) and keep the seven-file convention consistent across the codebase.

## Module DI

```kotlin
// :data-features:trainings/TrainingsFeatureModule.kt
@Module(includes = [BackendModule::class, DatabaseModule::class])
@ComponentScan
public class TrainingsFeatureModule
```

`@ComponentScan` discovers `TrainingsRepositoryImpl` and `TrainingsFeatureImpl` via their annotations. `includes = [BackendModule, DatabaseModule]` makes `GrippoApi` and DAOs available via transitive imports.

## Anti-patterns

- **`Repository` is `public`.** It must be `internal`.
- **`@Single` without `binds = [<X>Repository::class]`.** Koin won't resolve the interface.
- **`getKoin().get()` inside the Repository.** Constructor-inject everything.
- **Writing to DAO before `api.onSuccess { ... }`.** Speculative writes leave the cache inconsistent on failure.
- **Inline mappers (`val entity = TrainingEntity(id = dto.id ?: ..., ...)`)** — use `:data-mappers:*`.
- **Returning `Flow<DTO>`.** DTOs never escape this layer.
- **`suspend fun observeX()`** — observe is hot; if you need to block on first emission, use `observeX().first()` at the call site.
- **Catching exceptions inside the Repository.** Let them propagate via `runCatching`/`Result`. The Repository's job is to translate API + DAO; error handling is the ViewModel's job (via `ErrorProvider`).
