# Naming Conventions

## Files

| Kind | Pattern | Example |
|---|---|---|
| Class | `<ClassName>.kt` (PascalCase) | `BaseViewModel.kt`, `NoteRepository.kt` |
| Sealed type family in one file | `<RootName>.kt` | `DialogConfig.kt`, `NotesRouter.kt` |
| Top-level functions | `<Topic>.kt` | `DateFormatting.kt`, `NoteMapper.kt` |
| Composable function | `<Name>.kt` matching the function | `Toolbar.kt`, `Button.kt`, `NoteArchiveScreen.kt` |

One file = one top-level declaration, unless tightly-coupled.

## Classes

| Kind | Pattern | Example |
|---|---|---|
| ViewModel | `<Name>ViewModel` | `NoteArchiveViewModel` |
| Component | `<Name>Component` | `NoteArchiveComponent` |
| Screen (Composable) | `<Name>Screen` | `NoteArchiveScreen` |
| Contract | `<Name>Contract` | `NoteArchiveContract` |
| State | `<Name>State` | `NoteArchiveState` |
| Direction | `<Name>Direction` | `NoteArchiveDirection` |
| Loader | `<Name>Loader` | `NoteArchiveLoader` |
| Feature interface | `<X>Feature` | `NoteFeature`, `UserFeature` |
| Feature impl | `<X>FeatureImpl` | `NoteFeatureImpl` |
| Repository interface | `<X>Repository` | `NoteRepository` |
| Repository impl | `<X>RepositoryImpl` | `NoteRepositoryImpl` |
| Use case | `<Verb><Noun>UseCase` (or `<Noun>UseCase` when a single verb fits poorly — see Functions row below) | `DeleteNoteUseCase`, `UpdateNoteUseCase`, `CreateProfileUseCase` (verb-noun); `NoteDigestUseCase`, `NoteSummaryUseCase` (noun-only) |
| Koin module | `<X>FeatureModule` / `<X>Module` | `NoteFeatureModule`, `BackendModule` |
| DTO response | `<X>Response` | `NoteResponse` |
| DTO body | `<X>Body` | `NoteBody`, `EmailAuthBody` |
| Room entity | `<X>Entity` | `NoteEntity` |
| Room DAO | `<X>Dao` | `NoteDao` |
| Room pack | `<X>Pack` | `NotePack` |
| Format state | `<X>FormatState` | `AmountFormatState`, `EmailFormatState` |
| Router | `<Feature>Router` | `RootRouter`, `NotesRouter` |
| Dialog config | nested in `DialogConfig` | `DialogConfig.NotePicker` |

## Functions

| Kind | Pattern | Example |
|---|---|---|
| Composable | PascalCase | `NoteArchiveScreen()`, `Button()` |
| UI callback (Contract) | `on<What><Action>()` | `onApplyClick()`, `onValueChange()`, `onBack()` |
| Observable (Repository, Feature) | `observe<X>()` | `observeUser()`, `observeNotes(start, end)` |
| Get (one-shot fetch) | `get<X>()` | `getUser()`, `getNotes(start, end)` |
| Mutation | `<verb><X>()` | `saveNote(n)`, `deleteUser()`, `updateProfile(p)` |
| Mapper | `<Source>.to<Target>()` / `to<Target>OrNull()` | `NoteResponse.toEntityOrNull()`, `Note.toBody()` |
| Plural mapper | `List<Source>.to<Target>s()` | `List<NoteResponse>.toEntities()` |
| ViewModel verb | `<verb>` | `loadNotes()`, `submitForm()` |
| Use case action | `execute(...)` (default; domain-named variants when a single verb fits poorly) | `DeleteNoteUseCase.execute(id)`, `UpdateNoteUseCase.execute(value)`, `LoginUseCase.executeEmail/executeGoogle/executeApple` |

## Parameters

| Kind | Pattern | Example |
|---|---|---|
| Composable modifier | `modifier: Modifier = Modifier` first | |
| Composable callback | `on<X>: () -> Unit` | `onClick: () -> Unit` |
| ViewModel constructor | `<name>Feature: <X>Feature` | `userFeature: UserFeature` |
| Component constructor | `<name>: <Type>` | `back: () -> Unit`, `initialRange: DateRange` |
| Repository constructor | `<name>: <X>Api`/`<X>Dao`/`<X>DataStore` | `api: <Product>Api`, `noteDao: NoteDao`, `dataStore: DataStore<Preferences>` |

## Packages

```
com.<org>.<product>.<area>.<feature>.[<subscreen>]
```

Examples:
- `com.<org>.<product>.notes.archive` (`:ui-screen-features:notes`' archive sub-screen)
- `com.<org>.<product>.note.picker` (`:ui-dialog-features:note-picker`)
- `com.<org>.<product>.data.features.notes.data` (Repository impl)
- `com.<org>.<product>.data.features.notes.domain` (Feature interface)
- `com.<org>.<product>.services.backend.dto.note` (DTOs)
- `com.<org>.<product>.services.database.entity` (Entities)
- `com.<org>.<product>.services.database.dao` (DAOs)
- `com.<org>.<product>.services.database.models` (Packs)
- `com.<org>.<product>.services.database.migrations` (Migration objects)
- `com.<org>.<product>.dto.entity.note` (`:data-mappers:dto-to-entity`)
- `com.<org>.<product>.entity.domain.note` (`:data-mappers:entity-to-domain`)

Some legacy projects have modules with **dotted directory names** (`data.features.<feature>/`, `dialog.api/`). Keep dots **inside Kotlin file paths** consistently for those modules; **new modules** can use dot-free directories (`com/<org>/<product>/<feature>/`) if you prefer. Either is fine; pick one per module group and stick to it.

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
public fun stubNotes(): ImmutableList<Note> = persistentListOf(...)
public fun stubUser(): User = User(...)
public fun stubNote(): Note = Note(...)
```

`stub*()` functions return **realistic preview data** — used in `*Preview()` functions.

## Anti-patterns

- **Hungarian notation.** `m_foo`, `s_bar`, `g_baz` — forbidden. Kotlin's visibility modifiers + naming convention covers this.
- **Type suffixes that aren't structural.** `AmountInteger`, `LocaleString` — type system already says it. Acceptable: `<X>List` (semantic, e.g. `NoteList`).
- **Acronyms in CamelCase.** Use `HttpClient`, not `HTTPClient`. The convention: only first letter uppercase, rest lowercase, for acronyms 3+ chars.
- **`Util`-suffixed classes.** Use a top-level function file (`DateUtils.kt`) instead.
- **`Helper`-suffixed classes.** Same — top-level functions or a meaningful name.
- **`Manager`-suffixed classes for stateless services.** OK for `NotificationManager`, `PermissionManager` (stateful platform integrations). Not for `StringManager`.
- **`Service` suffix for in-app helpers.** Reserved for platform-edge wrappers (`FirebaseProvider`, not `FirebaseService`).
