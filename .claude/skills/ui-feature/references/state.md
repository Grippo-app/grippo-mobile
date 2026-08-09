# State, `UiText`, and `*FormatState` (`:ui-core:state`)

> Examples use `Note` / `Tag` / `User` as the generic `<Entity>`. Substitute your domain.

## `UiText` — localizable strings in state

State cannot use `String` for values that depend on resources (locale may change at runtime). `UiText` is the sealed type for all localizable strings (lives in `:ui-core:state/formatters/UiText.kt`):

```kotlin
@Stable
public sealed interface UiText {
    @Immutable
    public data class Res(
        public val value: StringResource,
        public val formatArgs: ImmutableList<Any> = persistentListOf(),
    ) : UiText

    @Immutable
    public data class Str(public val value: String) : UiText

    @Composable
    public fun text(): String = when (this) {
        is Str -> value
        is Res -> {
            val args = remember(formatArgs) { formatArgs.toTypedArray() }
            AppTokens.strings.res(value, *args)
        }
    }

    public suspend fun text(stringProvider: StringProvider): String = when (this) {
        is Str -> value
        is Res -> stringProvider.get(value, *formatArgs.toTypedArray())
    }
}
```

| Subtype | When |
|---|---|
| `Res(value, formatArgs)` | Localized text from `strings.xml`. Carries the **resource ID**, resolved lazily at render (picks up the current locale). Format args interpolated. |
| `Str(value)` | Verbatim text — a user's name, an email, a server-provided message. Won't change with locale. |

Resolving: in a Composable `Text(text = uiText.text())`; in a ViewModel/suspend context `uiText.text(stringProvider)` (resolve before crossing a layer boundary — notifications, error reports).

Usage in state: `val title: UiText = UiText.Res(Res.string.foo)`; with args `UiText.Res(Res.string.welcome_user, formatArgs = persistentListOf(user.name))`; verbatim `UiText.Str(serverError.title)`.

`formatArgs` is `ImmutableList<Any>` (Compose stability; `Any` matches `strings.xml` placeholder types; `persistentListOf()` default). Sealed interface so `when` is exhaustive. Equality is by-value (`Res(foo) == Res(foo)`; equal instances skip re-resolution during recomposition).

### Serialization (MUST)

`UiText` is `@Stable` **only** — **not** `@Serializable` (wraps a non-serializable `StringResource`). Safe inside in-memory `*State` data classes, but **must not** appear inside a `*Router` payload or a `DialogConfig` field. For copy crossing a serialized boundary, carry the `StringResource` (or a plain `String` for verbatim) and wrap to `UiText` at render time.

### `UiText` anti-patterns (MUST avoid)

- `String` in state for localizable values.
- Pre-resolving `UiText` at VM construction by injecting `StringProvider` (pass it raw; let the UI resolve — exception: when it crosses a boundary like a notification).
- `UiText.Res(Res.string.foo, listOf(arg))` — `listOf` is mutable; use `persistentListOf`.
- `uiText.text(stringProvider)` from a `@Composable` — use the `@Composable` `text()`.
- `UiText.Str` for a localized `strings.xml` value — use `UiText.Res`.
- Wrapping a `String?` as `UiText.Str(it)` — use `UiText?` and null-check at render.
- `UiText` inside a `*Router`/`DialogConfig` field — carry the `StringResource` instead.

## `*FormatState` — form-field state

Form fields (email, password, amount, date) are **sealed `*FormatState`** classes with `Empty`/`Invalid`/`Valid` subtypes. All `*FormatState` types are `@Immutable @Serializable` so they survive process death inside `*Router` payloads or `DialogConfig` configs.

```kotlin
@Immutable
public sealed interface FormatState<T> {
    public val display: String   // the raw text as the user typed it. Always present.
    public val value: T?         // the parsed value (Double, String, LocalDate, ...). Present only when valid.
    public interface Valid<T> : FormatState<T>
    public interface Invalid<T> : FormatState<T>
    public interface Empty<T> : FormatState<T>
}
```

Each concrete type has three subtypes (`Valid`/`Invalid`/`Empty`), `@Immutable @Serializable`:
- **`Valid`** — parsed cleanly + passes domain limits.
- **`Invalid`** — malformed (not a number) **or** out of range.
- **`Empty`** — blank input, distinct from `Invalid("")`. Numeric formatters also collapse literal `0` to `Empty`.

Example (`AmountFormatState : FormatState<Double>`) has `abstract val unit: String?` and a companion with `AmountLimitation: ClosedFloatingPointRange<Double>`, `of(display, unit)`, `of(value, unit)`.

### Factories

- `of(display: String, …)` — used by the UI's `onValueChange`. Parses the string; `Empty` if blank, `Valid` if parseable + in range, `Invalid` otherwise.
- `of(value: T?, …)` — used to initialize from a domain value. Always `Valid` (or `Empty` if null).

Factories vary per type (not every formatter exposes both). Date types take a `range`/`format`; `EmailFormatState` only accepts `value`.

### Standard types

- **`EmailFormatState : FormatState<String>`** — `of(value: String?)`; regex-validated.
- **`PasswordFormatState : FormatState<String>`** — `of(display: String)`; `@Composable fun hint(): String` (localized hint matching the active rule).
- **`DateFormatState : FormatState<LocalDate>`** — `abstract val format: DateFormat.DateOnly`; `of(value: LocalDate?, range: DateRange, format: DateFormat.DateOnly)`.
- **`DateRangeFormatState : FormatState<DateRange>`** — `abstract val kind: DateRangeKind`; `fun label(): UiText?` ("Last 30 days"); `of(range)`, `of(kind)`, `of(from, to)`.
- **`DateTimeFormatState : FormatState<LocalDateTime>`** — `abstract val format: DateFormat`; `of(value, range, format)`.
- **`NameFormatState : FormatState<String>`** — `NameLimitation: IntRange  // 1..60`; `of(display: String)`.

### Usage

State: `val amount: AmountFormatState = AmountFormatState.Empty()`, `val due: DateFormatState = DateFormatState.Empty(format = DateFormat.DateOnly.DateMmmDdYyyy)`.

ViewModel: `update { it.copy(amount = AmountFormatState.of(raw)) }`; read the parsed value via `(state.value.amount as? AmountFormatState.Valid)?.value ?: return` (the `as? Valid` cast is canonical; no-op if not valid).

Compose: feed `value = state.amount` to the input; compute button enablement from `state.amount is AmountFormatState.Valid`.

`display` + `value` are separate so the user's raw typing is preserved (`"72.5"` vs `"72.50"`); round-tripping would lose formatting. `@Serializable` keeps them safe inside `*Router`/`DialogConfig`.

### `*FormatState` rules (MUST) & anti-patterns

Rules: one `*FormatState` per form-field type (not per screen); always start in `Empty()` (not `Invalid("")`); `of(...)` is the **only** construction path (don't instantiate `Valid`/`Invalid` directly — bypasses validation); `Companion.Limitation` is the range for `Invalid` (keep static; tunable values belong in `:data-features:local-settings`); `hint()` Composables return localized strings.

Anti-patterns: `String` for an email/password/amount field; `Double?` for an amount (loses raw display, can't validate); validating on submit only; computing button enablement from raw fields (use `state.field is Valid`); side effects inside `of(...)` (factories are pure).

## State conventions

```kotlin
@Immutable
internal data class FooState(
    val title: UiText = UiText.Res(Res.string.foo_default_title),
    val items: ImmutableList<ItemState> = persistentListOf(),
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
)
```

Rules (MUST):
1. **`@Immutable`** on the class.
2. **`internal data class`** by default (`public` only for cross-module state in `:ui-core:state`).
3. **`val` only** — no `var`.
4. **Default ctor values for every property** — `FooState()` is the initial state for `BaseViewModel(FooState())` and previews. No `companion object Empty` (the companion stays for thresholds/derived helpers/shared constants only).
5. **Field order**: required first, optional last; primitives last (or grouped by domain).

**Goes in state**: display values; form-field values as `*FormatState`; filter/pagination state; derived display flags the UI doesn't compute; sub-states.

**Does NOT go in state**: domain objects directly when a transformed representation is needed (map via `:data-mappers:domain-to-state`); loaders (use `loaders` on the VM); computed booleans derivable from other state (use `remember(...)` in the Composable); unchanging configuration (companion/`val`).

### Immutable collections (MUST)

`ImmutableList<T>` for read-only; `PersistentList<T>` to build incrementally (`.add`/`.remove` returning new lists). Both Compose-stable. `List<T>` / `MutableList<T>` are **forbidden** in state.

### Nullability

`UiText?` for optional description; `*FormatState` subtypes for form fields (not `String?`); nullable domain refs when optional (`val user: User? = null`).

### Sealed-interface states

For screens with discrete modes (Loading/Loaded/Empty/Error), use an `@Immutable sealed interface` with `@Immutable` on the parent AND every subtype. The Composable pattern-matches with an exhaustive `when`. Use sealed states only when the screen has **truly different layouts** per mode; for "same layout with some fields empty", use one `data class` with nullable fields.

### Process-death safety (MUST)

If the state appears inside a `*Router` payload or `DialogConfig`, it **must be `@Serializable`**, and every field inside must also be serializable:
- Primitives, `String`, `enum` — natively `@Serializable`.
- `LocalDateTime` / `LocalDate` / `Duration` — covered by `kotlinx-serialization` + `kotlinx-datetime`.
- `DateRange` / `*FormatState` — declared `@Serializable` in `:toolkit:date-utils` / `:ui-core:state`.
- `ImmutableList<T>` — the project's custom `ImmutableListSerializer` (in `:ui-core:state`), opted in per field: `@Serializable(with = ImmutableListSerializer::class)`. No `ImmutableMap` serializer ships; model a map as a list of records or a regular `Map`.
- `UiText` — `@Stable` only, **not** `@Serializable`. Do not put it in a router/dialog payload; store the `StringResource` or a plain `String`.

If the state doesn't cross a router/dialog boundary (lives in the VM, rebuilt from data sources on restart), `@Serializable` is **not required** (harmless to add).

### Sub-state pattern

Factor nested data into sub-state classes (`HeaderState`, `NoteRowState`). Sub-states are passed to sub-Composables **as one model** (`HeaderComposable(state = state.header, ...)`) — never spread flat. Compose stability inference works per-field (changing a `NoteRowState` skips unrelated `HeaderState` rendering).

### No flat scalar piles (MUST)

A `State` is not a bucket for every scalar. When fields cluster into obvious groups, model it — don't flatten. A lifecycle encoded as loose booleans (`isRunning` + `hasCompletedTest` + nullable `activePhase`) admits illegal combinations. Refactor: a **sealed run-phase** for the lifecycle (`Idle`/`Running(phase)`/`Completed(result)`), **sub-states** for the clusters (network info, latency stats), an **enum** for the fixed phase set. Rule of thumb: if a `State` carries more than a handful of same-typed primitives, or a set of booleans describing one lifecycle, you're missing a sub-state, an enum, or a sealed interface.

### Top-level `stub*` functions

Realistic preview data lives in `:ui-core:state` as **public top-level functions** next to the state class:

```kotlin
public fun stubNote(): NoteState = NoteState(id = "preview-1", title = "Grocery list", summaryLabel = UiText.Str("3 items"))
public fun stubNotes(): ImmutableList<NoteState> = persistentListOf(
    stubNote(), stubNote().copy(id = "preview-2", title = "Reading list"), stubNote().copy(id = "preview-3", title = "Trip plan"),
)
```

- **Where**: `:ui-core:state`, next to the state class — **never** inside `:ui-screen-features:*` / `:ui-dialog-features:*`, so previews across the repo pull from one source.
- **Visibility**: `public` (shared across feature modules).
- **Naming**: `stubX()` for a single instance, `stubXs()` / `stubXList()` for collections.
- **Content**: realistic data — not `"foo"` placeholders.

### State anti-patterns (MUST avoid)

- `var` fields in state (always `val`).
- `String` for localizable values (use `UiText`); `String` for form fields (use `*FormatState`).
- `List<T>` in state (use `ImmutableList<T>`).
- `MutableStateFlow<S>` exposed as a state field (state holders are in the VM; state classes are pure data).
- `@Composable` content in state (e.g. `val icon: @Composable () -> Unit`).
- Domain references that need formatting (map via `:data-mappers:domain-to-state`).
- `null` checks scattered through the screen (use sealed states for discrete modes).
- State that mirrors a Feature's signature 1:1 (if it adds nothing over domain, you're missing the mapper).
- Spreading a `*State` model into a composable's flat parameters (see `references/compose-rules.md` § Entity composables).
- Flat scalar pile (group the clusters; see above).
