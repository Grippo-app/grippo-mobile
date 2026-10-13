# State, `UiText`, and `*FormatState` (`:ui-core:state`)

> Note/Tag/AmountPicker/NoteDetail examples are educational, nonexistent APIs. They are not the current route, dependency or source catalog; inspect real callers and signatures before adapting them.

> Examples use `Note` / `Tag` / `User` as the generic `<Entity>`. Substitute your domain.

## `UiText` — localizable strings in state

State cannot use `String` for values that depend on resources (locale may change at runtime). `UiText` is the sealed type for all localizable strings (lives in `:ui-core:state/formatters/UiText.kt`):

Inspect the current [UiText implementation](../../../../ui-core/state/src/commonMain/kotlin/com/grippo/core/state/formatters/UiText.kt). It resolves nested UiText format arguments recursively in both rendering and suspend contexts; do not replace this behavior with a cached raw argument array.

| Subtype | When |
|---|---|
| `Res(value, formatArgs)` | Localized text from `strings.xml`. Carries the **resource ID**, resolved lazily at render (picks up the current locale). Format args interpolated. |
| `Str(value)` | Verbatim text — a user's name, an email, a server-provided message. Won't change with locale. |

Resolving: in a Composable `Text(text = uiText.text())`; in a ViewModel/suspend context `uiText.text(stringProvider)` (resolve before crossing a layer boundary — notifications, error reports).

Usage in state: `val title: UiText = UiText.Res(Res.string.foo)`; with args `UiText.Res(Res.string.welcome_user, formatArgs = persistentListOf(user.name))`; verbatim `UiText.Str(serverError.title)`.

`formatArgs` is `ImmutableList<Any>` (Compose stability; `Any` matches `strings.xml` placeholder types; `persistentListOf()` default). Sealed interface so `when` is exhaustive. Equality is by-value; this does not establish skipped resource resolution, recomposition cost or locale behavior. Inspect actual UiText consumers and the current implementation.

### Serialization (MUST)

`UiText` is `@Stable` **only** — **not** `@Serializable` (wraps a non-serializable `StringResource`). Safe inside in-memory `*State` data classes and `DialogConfig` fields (`title` is required), but **must not** appear inside a serialized `*Router` payload. For copy crossing a serialized boundary, carry a serializable resource identifier (or a plain `String` for verbatim) and wrap to `UiText` at render time. Dialog configs are live session data, not a serialized boundary; see [dialogs.md](dialogs.md).

### `UiText` anti-patterns (MUST avoid)

- `String` in state for localizable values.
- Pre-resolving `UiText` at VM construction by injecting `StringProvider` (pass it raw; let the UI resolve — exception: when it crosses a boundary like a notification).
- `UiText.Res(Res.string.foo, listOf(arg))` — regular List does not guarantee deep immutability; use `persistentListOf`.
- `uiText.text(stringProvider)` from a `@Composable` — use the `@Composable` `text()`.
- `UiText.Str` for a localized `strings.xml` value — use `UiText.Res`.
- Wrapping a `String?` as `UiText.Str(it)` — use `UiText?` and null-check at render.
- `UiText` inside a serialized `*Router` field — carry a serializable identifier or a plain `String` instead. Live `DialogConfig` fields may use `UiText`.

## `*FormatState` — form-field state

Form fields (email, password, amount, date) are **sealed `*FormatState`** classes with `Empty`/`Invalid`/`Valid` subtypes. Concrete `*FormatState` types are `@Immutable @Serializable`; permitted nonsecret fields can cross serialized route or business-draft boundaries. Using one in a live dialog does not restore the dialog session.

Serialization support is not permission to persist secrets. PasswordFormatState
may remain in runtime VM state, but password fields are `@Transient` with Empty
defaults in saved drafts and never belong in route payloads. Preserve nonsecret
invalid/empty display values, not only their validated value; apply
[state-preservation](../../state-preservation/SKILL.md).

```kotlin
@Immutable
public sealed interface FormatState<T> {
    public val display: String   // current formatter display; may be normalized.
    public val value: T?         // parsed/supplied value; may also be present in Invalid.
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

- `of(display: String, …)` — used by the UI's `onValueChange`. Applies the concrete formatter's parsing, validation, normalization and empty-value rules; it may change the display string.
- Typed-value `of(value, …)` — initializes from domain values or accepted picker results through the same existing limits. It may return `Invalid` for out-of-range values; nullable overloads return `Empty` for null. A supplied typed value is not automatically valid.

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

`display` + `value` preserve the current formatter state, including invalid or
empty input, rather than reconstructing it from only a valid value. Display policy
belongs to the existing factory: `WeightFormatState.of("72.50")` normalizes the
display to `"72.5"`, while an unparseable `"abc"` remains invalid display. Exact
draft restoration means the latest state after that factory, not every original
keypress. Do not add raw numeric-string persistence or another validator to force
verbatim retention. Serialization applies only to permitted nonsecret fields;
dialog configs remain in memory.

### `*FormatState` rules (MUST) & anti-patterns

Rules: reuse a suitable existing `*FormatState`. Add a formatter only when the field
needs genuinely different parsing, validation or display semantics, not merely
because it belongs to another screen; absent input uses `Empty()` (not `Invalid("")`), while provided/retained drafts keep their existing formatter state; `of(...)` is the **only** construction path for supplied values (don't instantiate `Valid`/`Invalid` directly — bypasses validation); `Companion.Limitation` is the range for `Invalid` (keep static; tunable values belong in `:data-features:local-settings`); `hint()` Composables return localized strings.

Anti-patterns: `String` for an email/password/amount field; `Double?` for an amount (loses raw display, can't validate); validating on submit only; computing button enablement from raw fields (use `state.field is Valid`); side effects inside `of(...)` (factories are pure).

## State conventions

```kotlin
@Immutable
internal data class FooState(
    val title: UiText = UiText.Res(Res.string.foo_default_title),
    val items: ImmutableList<ItemState> = persistentListOf(),
)
```

Rules (MUST):
1. **`@Immutable`** on the class.
2. **`internal data class`** by default (`public` only for cross-module state in `:ui-core:state`).
3. **`val` only** — no `var`.
4. **Meaningful initial state** — constructor defaults MAY represent valid empty/loading/input values. Pass required data explicitly through the existing initialState/route boundary; MUST NOT invent empty IDs or placeholder business data just to enable `FooState()`. Shared models do not require a no-argument constructor. No `companion object Empty` snapshot (the companion stays for thresholds/derived helpers/shared constants only).
5. **Field order**: required first, optional last; primitives last (or grouped by domain).

When changing constructor defaults or VM initialization, inspect all affected
State constructors, including previews. A preview of loaded content must supply
its required resolved data rather than accidentally exercise startup loading.
Runtime initialization and preview inputs may differ; neither requires forcing
all constructor defaults to be loaded or empty. Follow the
[persistent-settings initialization policy](../../state-preservation/SKILL.md#initialization-from-persistent-settings)
when values arrive asynchronously.

**Goes in state**: display values; form-field values as `*FormatState`; filter/pagination state; business validation/eligibility; sub-states.

**Does NOT go in state**: domain objects directly when a transformed representation is needed (map via `:data-mappers:domain-to-state`); loaders (use `loaders` on the VM); pure presentation booleans derivable from other state (compute in the Composable; business decisions remain VM/domain-owned); unchanging configuration (companion/`val`).

State must not hide restoration/edit flags or previous-source copies removed from
the ViewModel. Store actual draft values and needed display data; follow the
[canonical draft policy](../../state-preservation/SKILL.md#data-backed-form-synchronization).

### Immutable collections (MUST)

`ImmutableList<T>` for read-only; `PersistentList<T>` to build incrementally (`.add`/`.remove` returning new lists). Both Compose-stable. `List<T>` / `MutableList<T>` are **forbidden** in state.

### Nullability

`UiText?` for optional description; reuse suitable `*FormatState` subtypes for validated/formatted fields; plain
nonsecret free text without formatting/validation may remain String; nullable domain refs when optional (`val user: User? = null`).

### Sealed-interface states

For screens with discrete modes (Loading/Loaded/Empty/Error), use an `@Immutable sealed interface` with `@Immutable` on the parent AND every subtype. The Composable pattern-matches with an exhaustive `when`. Use sealed states only when the screen has **truly different layouts** per mode; for "same layout with some fields empty", use one `data class` with nullable fields.

### Process-death safety (MUST)

If state appears inside a serialized `*Router` payload or an explicit Component
business draft, it **must be `@Serializable`**, and every persisted field must also
be serializable:
- Primitives, `String`, `enum` — natively `@Serializable`.
- `LocalDateTime` / `LocalDate` / `Duration` — covered by `kotlinx-serialization` + `kotlinx-datetime`.
- `DateRange` / `*FormatState` — declared `@Serializable` in `:toolkit:date-utils` / `:ui-core:state`.
- `ImmutableList<T>` — the project's custom `ImmutableListSerializer` (in `:ui-core:state`), opted in per field: `@Serializable(with = ImmutableListSerializer::class)`. No `ImmutableMap` serializer ships; model persisted entries as an immutable list of serializable records. Do not put regular Map in immutable State.
- `UiText` — `@Stable` only, **not** `@Serializable`. Do not put it in a serialized router payload; store a serializable identifier or a plain `String`. It is allowed in live dialog configs.

If state crosses no serialization boundary (lives in the VM, rebuilt from data
sources on restart), `@Serializable` is **not required**. For explicit business
drafts, keep rebuilt suggestions/presentation fields transient with defaults;
follow [process-death.md](process-death.md#editable-form-drafts). A `DialogConfig`
field alone adds no serialization requirement: sessions, steps and callbacks are
discarded on process death.

### Sub-state pattern

Factor nested data into sub-state classes (`HeaderState`, `NoteRowState`). Sub-states are passed to sub-Composables **as one model** (`HeaderComposable(state = state.header, ...)`) — never spread flat. Compose stability inference works per-field (changing a `NoteRowState` skips unrelated `HeaderState` rendering).

### No flat scalar piles (MUST)

A `State` is not a bucket for every scalar. When fields cluster into obvious groups, model it — don't flatten. A lifecycle encoded as loose booleans (`isRunning` + `hasCompletedTest` + nullable `activePhase`) admits illegal combinations. Refactor: a **sealed run-phase** for the lifecycle (`Idle`/`Running(phase)`/`Completed(result)`), **sub-states** for the clusters (network info, latency stats), an **enum** for the fixed phase set. Rule of thumb: if a `State` carries more than a handful of same-typed primitives, or a set of booleans describing one lifecycle, you're missing a sub-state, an enum, or a sealed interface.

### Top-level `stub*` functions

Shared reusable model preview data lives in `:ui-core:state` as **public top-level functions** next to the state class:

```kotlin
public fun stubNote(): NoteState = NoteState(id = "preview-1", title = "Grocery list", summaryLabel = UiText.Str("3 items"))
public fun stubNotes(): ImmutableList<NoteState> = persistentListOf(
    stubNote(), stubNote().copy(id = "preview-2", title = "Reading list"), stubNote().copy(id = "preview-3", title = "Trip plan"),
)
```

- **Where**: shared model stubs live next to the class in `:ui-core:state`. Private builders assembling a feature-owned screen/dialog MVI State may live beside that feature's preview and reuse shared model stubs. MUST NOT move feature State into ui-core or expose a public fixture API solely for a local preview.
- **Visibility**: shared model stubs are `public`; local feature-State fixture builders are `private`.
- **Naming**: shared stubs use `stubX()` for a single instance, `stubXs()` / `stubXList()` for collections; local builders use a descriptive preview name.
- **Content**: realistic data — not `"foo"` placeholders.

### State anti-patterns (MUST avoid)

- `var` fields in state (always `val`).
- `String` for localizable values (use `UiText`); raw String replacing an existing suitable formatter (reuse `*FormatState`).
- `List<T>` in state (use `ImmutableList<T>`).
- `MutableStateFlow<S>` exposed as a state field (state holders are in the VM; state classes are pure data).
- `@Composable` content in state (e.g. `val icon: @Composable () -> Unit`).
- Domain references that need formatting (map via `:data-mappers:domain-to-state`).
- `null` checks scattered through the screen (use sealed states for discrete modes).
- State that mirrors a Feature's signature 1:1 (if it adds nothing over domain, you're missing the mapper).
- Spreading a `*State` model into a composable's flat parameters (see `references/compose-rules.md` § Entity composables).
- Flat scalar pile (group the clusters; see above).
