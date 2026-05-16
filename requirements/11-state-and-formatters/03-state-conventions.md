# State Conventions

Beyond `UiText` and `*FormatState`, state classes follow a set of conventions to keep UI predictable, Compose-stable, and process-death-safe.

## File and class shape

```kotlin
@Immutable
internal data class FooState(
    val title: UiText = UiText.Res(Res.string.foo_default_title),
    val items: ImmutableList<ItemState> = persistentListOf(),
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
)
```

Rules:

1. **`@Immutable`** on the class.
2. **`internal data class`** by default (`public` only for cross-module state in `:ui-core:state`).
3. **`val` only** — no `var`.
4. **Default ctor values for every property** — `FooState()` is the initial state passed to `BaseViewModel(FooState())` and to previews. No separate `companion object Empty` is used; the companion stays for thresholds, derived helpers, or shared constants only.
5. **Field order**: required first, optional last; primitives last (or grouped by domain).

## What goes in state

- **Display values** the UI renders.
- **Form-field values** as `*FormatState`.
- **Filter / pagination state** (range, page index, search query).
- **Derived display flags** the UI doesn't compute (e.g. `isOnboardingComplete`).
- **Sub-states** for sub-screens (`@Immutable data class SubState(...)`).

## What does NOT go in state

- **Domain objects directly**, when the UI needs a transformed representation. Map via `:data-mappers:domain-to-state` to produce a state class.
- **Loaders.** Use `loaders: StateFlow<ImmutableSet<LOADER>>` on the ViewModel.
- **Computed booleans** that can be derived from other state. Use `remember(state.x, state.y) { ... }` in the Composable.
- **Configuration that doesn't change** (a constant). Put it in a `companion object` or as a `val` in the screen module.

## Immutable collections

```kotlin
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

val items: ImmutableList<ItemState> = persistentListOf()

// or, after mapping
val items: ImmutableList<ItemState> = domain.map { it.toState() }.toImmutableList()
```

`ImmutableList<T>` for read-only collections in state. `PersistentList<T>` if you need to build incrementally (`.add(item)`, `.remove(item)` returning new lists). Both are Compose-stable.

`List<T>` / `MutableList<T>` are forbidden in state.

## Nullability

- **`UiText?`** for an optional description.
- **`*FormatState`** subtypes (`Empty`/`Invalid`/`Valid`) for form fields — don't use `String?`.
- **Domain references** as nullable when the relationship is optional (`val user: User? = null` for "not yet loaded").

## Sealed-interface states

For screens with discrete modes (Loading, Loaded, Empty, Error):

```kotlin
@Immutable
internal sealed interface NoteState {

    @Immutable
    data object Loading : NoteState

    @Immutable
    data class Loaded(
        val title: NameFormatState,
        val amount: AmountFormatState,
        val tags: ImmutableList<TagState>,
    ) : NoteState

    @Immutable
    data class Error(val message: UiText) : NoteState

    @Immutable
    data object Empty : NoteState
}
```

The Composable pattern-matches:

```kotlin
when (state) {
    is NoteState.Loading -> LoadingScreen()
    is NoteState.Loaded -> ContentScreen(state, contract)
    is NoteState.Error -> ErrorScreen(state.message, onRetry = contract::onRetryClick)
    is NoteState.Empty -> EmptyScreen(onAction = contract::onPrimaryAction)
}
```

Use sealed states when the screen has **truly different layouts** per mode. For "the same layout with some fields empty", use a single `data class` with nullable fields.

## Process-death safety

If the state appears inside a `*Router` payload or `DialogConfig`, it **must be `@Serializable`**:

```kotlin
@Immutable
@Serializable
public data class StageState(
    val step: Step,
    @Serializable(with = ImmutableListSerializer::class)
    val collected: ImmutableList<String>,
) {
    @Serializable
    public enum class Step { Welcome, Profile, Goals, Done }
}
```

Every field inside must also be serializable:

- **Primitives, `String`, `enum`** — natively `@Serializable`.
- **`LocalDateTime` / `LocalDate` / `Duration`** — covered by `kotlinx-serialization` + `kotlinx-datetime`.
- **`DateRange` / `*FormatState`** — declared `@Serializable` in `:toolkit:date-utils` / `:ui-core:state`.
- **`ImmutableList<T>`** — uses the project's custom `ImmutableListSerializer` (in `:ui-core:state`), opted in per field with `@Serializable(with = ImmutableListSerializer::class)`. No `ImmutableMap` serializer ships with the project; if you need a map, model it as a list of `(key, value)` records or use a regular `Map` field.
- **`UiText`** — `@Stable` only, **not** `@Serializable` (it wraps a non-serializable `StringResource`). Do not put `UiText` in a router/dialog payload. For copy that must survive process death, store the `StringResource` directly and wrap to `UiText` at render time, or carry a plain `String` for verbatim values.

If the state doesn't cross a router/dialog boundary (it lives entirely in the VM and is reconstructed from data sources on restart), `@Serializable` is **not required** — but it's harmless to add.

## Sub-state pattern

For nested data, factor a sub-state class:

```kotlin
@Immutable
internal data class NotesListState(
    val header: HeaderState,
    val items: ImmutableList<NoteRowState>,
    val isOnline: Boolean,
)

@Immutable
internal data class HeaderState(
    val title: UiText,
    val subtitle: UiText?,
    val rangeLabel: UiText,
)

@Immutable
internal data class NoteRowState(
    val id: String,
    val title: String,
    val summaryLabel: UiText,
    val createdAtLabel: String,
)
```

Sub-states:

- Are passed to sub-Composables (`HeaderComposable(state = state.header, ...)`).
- Compose stability inference works per-field — recomposition of a `NoteRowState` change skips the unrelated `HeaderState` rendering.

## Top-level `stub*` functions

Realistic preview data lives in `:ui-core:state` as **public top-level functions** next to the state class:

```kotlin
@Immutable
public data class NoteState(
    val id: String,
    val title: String,
    val summaryLabel: UiText,
)

public fun stubNote(): NoteState = NoteState(
    id = "preview-1",
    title = "Grocery list",
    summaryLabel = UiText.Str("3 items"),
)

public fun stubNotes(): ImmutableList<NoteState> = persistentListOf(
    stubNote(),
    stubNote().copy(id = "preview-2", title = "Reading list"),
    stubNote().copy(id = "preview-3", title = "Trip plan"),
)
```

- **Where**: `:ui-core:state`, next to the state class — never inside `:ui-screen-features:*` or `:ui-dialog-features:*`, so previews across the repo pull from the same source.
- **Visibility**: `public` (shared across feature modules).
- **Naming**: `stubX()` for a single instance, `stubXs()` / `stubXList()` for collections.
- **Content**: realistic data — not just `"foo"` placeholders.

## Anti-patterns

- **`var` fields in state.** Always `val`.
- **`String` for localizable values.** Use `UiText`.
- **`String` for form fields.** Use `*FormatState`.
- **`List<T>` in state.** Use `ImmutableList<T>`.
- **`MutableStateFlow<S>` exposed as a state field.** State holders are in the ViewModel; state classes are pure data.
- **`@Composable` content in state** (e.g. `val icon: @Composable () -> Unit`). State is data; the screen decides rendering.
- **Domain references that need formatting.** Map via `:data-mappers:domain-to-state` to a `*State` with formatted fields.
- **`null` checks scattered throughout the screen.** Use sealed states for discrete modes.
- **State that mirrors a Feature's signature 1:1.** State is UI-facing; if it adds nothing on top of domain, you're missing the mapper.
