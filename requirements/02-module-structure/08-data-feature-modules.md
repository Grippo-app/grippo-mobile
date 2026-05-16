# `:data-features:*` Modules

The domain layer. UI sees `:data-features:feature-api` (interfaces + domain models); implementations live in `:data-features:<feature>` (Repository + FeatureImpl, `internal`).

## Module list

| Module | Purpose | Convention plugins |
|---|---|---|
| `:data-features:feature-api` | All `<X>Feature` interfaces, all domain models, all `<X>UseCase` classes | KMP + Koin |
| `:data-features:authorization` | Authorization feature impl | KMP + Koin |
| `:data-features:user` | User profile feature impl | KMP + Koin |
| `:data-features:<area>` | One per business area | KMP + Koin |

Per-project. Each module is `:data-features:<feature>` and follows the pattern below.

## `:data-features:feature-api`

The single point of contact between UI and data. Holds:

- `<X>Feature` interfaces — `public interface NoteFeature { fun observeNotes(...): Flow<List<Note>>; suspend fun getNotes(...): Result<Unit> }`.
- Domain models — `public data class Note(val id: String, val createdAt: LocalDateTime, ...)`.
- `<X>UseCase` classes — for use cases that combine multiple features (e.g. `NoteSummaryUseCase` composing `NoteFeature` + `TagFeature`; `LoginUseCase.executeEmail/executeGoogle/executeApple` composes `AuthorizationFeature` + `UserFeature`).
- `FeatureApiModule` — `@Module @ComponentScan public class FeatureApiModule` declaring `<X>UseCase` as `@Factory` (use cases are stateless and instantiated per call).

### Rules

- **No** `:data-services:*` imports. The api module is pure contracts and domain types.
- **No** UI types (no `UiText`, no `*FormatState`, no `@Immutable`). Domain models are pure Kotlin data classes.
- **No** `@Serializable` on domain models unless the model is part of `*Router` payload (rare; route payloads are usually simple primitives or sealed types defined in `:ui-screen-features:screen-api`).
- Mutations return `Result<T>`; observations return `Flow<Domain>`.

### Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.data.features.feature.api" }

    sourceSets.commonMain.dependencies {
        implementation(projects.toolkit.dateUtils)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.datetime)
    }
}
```

No design-system, no UI core, no other toolkit module besides `:toolkit:date-utils` (which is needed because several domain models carry `LocalDateTime`/`DateRange` values).

## `:data-features:<feature>` (implementation)

Each implementation module holds:

- `data/<X>RepositoryImpl.kt` — `internal class <X>RepositoryImpl(private val api: <Product>Api, private val dao: <X>Dao, ...) : <X>Repository`. Annotated `@Single(binds = [<X>Repository::class])`.
- `domain/<X>Repository.kt` — `internal interface <X>Repository { ... }`. **Internal** to this module.
- `domain/<X>FeatureImpl.kt` — `internal class <X>FeatureImpl(private val repository: <X>Repository) : <X>Feature`. Annotated `@Single(binds = [<X>Feature::class])`.
- `<X>FeatureModule.kt` — `@Module(includes = [BackendModule::class, DatabaseModule::class]) @ComponentScan public class <X>FeatureModule`. **Public** so `:shared/Koin.kt` can reference it.

### Why an extra `<X>Repository` layer between `<X>FeatureImpl` and the services?

- `<X>Feature` is the **UI-visible** contract. It uses domain models only, returns `Result<T>` for mutations, exposes `Flow<Domain>` for observations.
- `<X>Repository` is the **internal** contract that combines `<X>Dao` (Room) + `<Product>Api` (Ktor). Sometimes it adds caching policies, range reconciliation, draft handling, or `Mapper` calls.
- `<X>FeatureImpl` is a **thin** wrapper that mostly delegates to `<X>Repository` but may compose multiple repositories or add lightweight transformations.

Splitting them lets the Repository test against fakes for `<X>Dao` and `<Product>Api`, while the Feature stays a stable public surface.

For trivial features (no caching, no composition), `<X>FeatureImpl` may delegate one-to-one to `<X>Repository`. That is fine — the layer is consistent across the codebase regardless of complexity.

### Standard patterns

See `03-architecture-patterns/06-repository-pattern.md` for the full pattern. Highlights:

- **Observe** returns `Flow<Domain>` from the DAO — never from the API.
- **Get/Set/Update/Delete** returns `Result<Unit>` or `Result<T>`. Hits the API, then updates the DAO on `onSuccess`.
- **Range reconciliation**: after `getNotes(start, end)`, delete all rows in `[start, end]` except those returned by the server (`dao.deleteByCreatedAtRangeExceptIds(...)`). This removes "deleted on another device" drift.
- **Drafts** live only in the DB (`draftNoteDao`); they never round-trip through the server.

### Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.data.features.<feature>" }

    sourceSets.commonMain.dependencies {
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataServices.database)
        implementation(projects.dataServices.backend)
        implementation(projects.dataMappers.domainToEntity)
        implementation(projects.dataMappers.entityToDomain)
        implementation(projects.dataMappers.dtoToEntity)
        implementation(projects.dataMappers.domainToDto)
        implementation(projects.toolkit.dateUtils)

        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.datetime)
    }
}
```

A feature module always depends on `:data-features:feature-api` and the data services + mappers it actually uses. Pull only what the feature touches — don't pre-import.

## Adding a new feature

1. Add `<X>Feature` interface + domain models in `:data-features:feature-api`.
2. Create `:data-features:<x>` module (add to `settings.gradle.kts`).
3. Implement `<X>Repository` + `<X>RepositoryImpl` + `<X>FeatureImpl`.
4. Declare `<X>FeatureModule` (public Koin module).
5. Add `implementation(projects.dataFeatures.<x>)` to `:shared/build.gradle.kts`.
6. Add `<X>FeatureModule().module` to `:shared/Koin.kt`'s `modules(...)`.

See `14-cookbook/03-add-data-feature.md` for the full recipe.

## What `:data-features:feature-api` exposes — a checklist

For a new feature `<X>`:

- [ ] `<X>Feature` interface with `Flow`-returning observers and `Result`-returning mutators.
- [ ] `<X>` (and any sub-types) domain model — non-null fields, no platform types, no `@Serializable`.
- [ ] `<X>UseCase` only if logic spans multiple features.

For a new use case combining features:

- [ ] `<Verb><Noun>UseCase` class with one `public suspend fun execute(...): Result<T>`.
- [ ] Annotated `@Factory` (use cases are stateless).
- [ ] Declared in `FeatureApiModule`'s `@ComponentScan` package.
