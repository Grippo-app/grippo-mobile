# Naming Conventions

## Files

| Kind | Pattern | Example |
|---|---|---|
| Class | `<ClassName>.kt` (PascalCase) | `BaseViewModel.kt`, `TrainingsRepository.kt` |
| Sealed type family in one file | `<RootName>.kt` | `DialogConfig.kt`, `TrainingRouter.kt` |
| Top-level functions | `<Topic>.kt` | `DateFormatting.kt`, `TrainingMapper.kt` |
| Composable function | `<Name>.kt` matching the function | `Toolbar.kt`, `Button.kt`, `ProfileBodyScreen.kt` |

One file = one top-level declaration, unless tightly-coupled.

## Classes

| Kind | Pattern | Example |
|---|---|---|
| ViewModel | `<Name>ViewModel` | `ProfileBodyViewModel` |
| Component | `<Name>Component` | `ProfileBodyComponent` |
| Screen (Composable) | `<Name>Screen` | `ProfileBodyScreen` |
| Contract | `<Name>Contract` | `ProfileBodyContract` |
| State | `<Name>State` | `ProfileBodyState` |
| Direction | `<Name>Direction` | `ProfileBodyDirection` |
| Loader | `<Name>Loader` | `ProfileBodyLoader` |
| Feature interface | `<X>Feature` | `TrainingFeature`, `UserFeature` |
| Feature impl | `<X>FeatureImpl` | `TrainingFeatureImpl` |
| Repository interface | `<X>Repository` | `TrainingRepository` |
| Repository impl | `<X>RepositoryImpl` | `TrainingRepositoryImpl` |
| Use case | `<Verb><Noun>UseCase` | `RecalculateGoalProgressUseCase` |
| Koin module | `<X>FeatureModule` / `<X>Module` | `TrainingFeatureModule`, `BackendModule` |
| DTO response | `<X>Response` | `TrainingResponse` |
| DTO body | `<X>Body` | `TrainingBody`, `EmailAuthBody` |
| Room entity | `<X>Entity` | `TrainingEntity` |
| Room DAO | `<X>Dao` | `TrainingDao` |
| Room pack | `<X>Pack` | `TrainingPack` |
| Format state | `<X>FormatState` | `WeightFormatState`, `EmailFormatState` |
| Router | `<Feature>Router` | `RootRouter`, `ProfileRouter` |
| Dialog config | nested in `DialogConfig` | `DialogConfig.WeightPicker` |

## Functions

| Kind | Pattern | Example |
|---|---|---|
| Composable | PascalCase | `ProfileBodyScreen()`, `Button()` |
| UI callback (Contract) | `on<What><Action>()` | `onApplyClick()`, `onValueChange()`, `onBack()` |
| Observable (Repository, Feature) | `observe<X>()` | `observeUser()`, `observeTrainings(start, end)` |
| Get (one-shot fetch) | `get<X>()` | `getUser()`, `getTrainings(start, end)` |
| Mutation | `<verb><X>()` | `saveTraining(t)`, `deleteUser()`, `updateExperience(b)` |
| Mapper | `<Source>.to<Target>()` / `to<Target>OrNull()` | `TrainingResponse.toEntityOrNull()`, `Training.toBody()` |
| Plural mapper | `List<Source>.to<Target>s()` | `List<TrainingResponse>.toEntities()` |
| ViewModel verb | `<verb>` | `loadTrainings()`, `submitForm()` |
| Use case action | `execute(...)` (default; domain-named variants when a single verb fits poorly) | `DeleteTrainingUseCase.execute(id)`, `UpdateWeightUseCase.execute(value)`, `LoginUseCase.executeEmail/executeGoogle/executeApple` |

## Parameters

| Kind | Pattern | Example |
|---|---|---|
| Composable modifier | `modifier: Modifier = Modifier` first | |
| Composable callback | `on<X>: () -> Unit` | `onClick: () -> Unit` |
| ViewModel constructor | `<name>Feature: <X>Feature` | `userFeature: UserFeature` |
| Component constructor | `<name>: <Type>` | `back: () -> Unit`, `initialRange: DateRange` |
| Repository constructor | `<name>: <X>Api`/`<X>Dao`/`<X>DataStore` | `api: <Product>Api`, `trainingDao: TrainingDao`, `dataStore: DataStore<Preferences>` |

## Packages

```
com.<org>.<product>.<area>.<feature>.[<subscreen>]
```

Examples:
- `com.grippo.profile.body` (`:ui-screen-features:profile`'s body sub-screen)
- `com.grippo.weight.picker` (`:ui-dialog-features:weight-picker`)
- `com.grippo.data.features.trainings.data` (Repository impl)
- `com.grippo.data.features.trainings.domain` (Feature interface)
- `com.grippo.services.backend.dto.training` (DTOs)
- `com.grippo.services.database.entity` (Entities)
- `com.grippo.services.database.dao` (DAOs)
- `com.grippo.services.database.models` (Packs)
- `com.grippo.services.database.migrations` (Migration objects)
- `com.grippo.dto.entity.training` (`:data-mappers:dto-to-entity`)
- `com.grippo.entity.domain.training` (`:data-mappers:entity-to-domain`)

The reference repo has some modules with **dotted directory names** (`data.features.trainings/`, `dialog.api/`) — legacy. Keep dots **inside Kotlin file paths** consistently for those modules; **new modules** can use dot-free directories (`com/grippo/datatrainings/`) if you prefer. Either is fine; pick one per module group and stick to it.

## Variables

- **`var current<X>` for state references that change** within a class.
- **`val <name>By<Index>: Map<K, V>`** for index maps.
- **Booleans use `is*`, `has*`, `can*`**: `isOnline`, `hasNetworkAccess`, `canSubmit`.
- **`<X>List` / `<X>s` for collections** — pick one style per file.

## Sealed types

- **`sealed interface`** for marker types (`BaseDirection`, `BaseLoader`, `BaseResult`).
- **`sealed class`** when you need a default constructor or fields shared across subtypes (`AppError`, `DialogConfig`).
- **Subtype names** describe the case, not the parent: `RootRouter.Home`, not `RootRouter.HomeRoute`.

## State defaults, Contract `Empty`, and `stub*`

States use **default constructor values** for the initial state; the ViewModel constructs them with `<Foo>State()`:

```kotlin
@Immutable
internal data class FooState(
    val user: User? = null,
    val items: ImmutableList<Item> = persistentListOf(),
)

// ViewModel constructs the initial state with all defaults:
internal class FooViewModel(...) : BaseViewModel<FooState, FooDirection, FooLoader>(
    FooState(),
), FooContract { ... }
```

A `companion object` on a State class is reserved for **constants** that belong with the state (e.g. `MIN_GOAL_HORIZON_DAYS`). It does **not** carry an `Empty` instance.

The `companion object Empty` pattern lives on **Contracts**, where a no-op implementation is needed for previews:

```kotlin
@Immutable
internal interface FooContract {
    fun onClick(id: String)

    companion object Empty : FooContract {
        override fun onClick(id: String) = Unit
    }
}
```

```kotlin
public fun stubWeightHistoryList(): ImmutableList<WeightPoint> = persistentListOf(...)
public fun stubUser(): User = User(...)
public fun stubTraining(): Training = Training(...)
```

`stub*()` functions return **realistic preview data** — used in `*Preview()` functions.

## Anti-patterns

- **Hungarian notation.** `m_foo`, `s_bar`, `g_baz` — forbidden. Kotlin's visibility modifiers + naming convention covers this.
- **Type suffixes that aren't structural.** `WeightInteger`, `LocaleString` — type system already says it. Acceptable: `<X>List` (semantic, e.g. `WeightHistoryList`).
- **Acronyms in CamelCase.** Use `HttpClient`, not `HTTPClient`. The convention: only first letter uppercase, rest lowercase, for acronyms 3+ chars.
- **`Util`-suffixed classes.** Use a top-level function file (`DateUtils.kt`) instead.
- **`Helper`-suffixed classes.** Same — top-level functions or a meaningful name.
- **`Manager`-suffixed classes for stateless services.** OK for `NotificationManager`, `PermissionManager` (stateful platform integrations). Not for `StringManager`.
- **`Service` suffix for in-app helpers.** Reserved for platform-edge wrappers (`FirebaseProvider`, not `FirebaseService`).
