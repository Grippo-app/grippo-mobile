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

Form fields (email, password, weight, date) are **sealed `*FormatState`** classes with `Empty`/`Invalid`/`Valid` subtypes. Concrete `*FormatState` types are `@Immutable @Serializable`; permitted nonsecret fields can cross serialized route or business-draft boundaries. Using one in a live dialog does not restore the dialog session.

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

Example: `WeightFormatState : FormatState<Float>` has a companion with `WeightLimitation: ClosedFloatingPointRange<Float>` (`30.0f..150.0f`), `of(display: String)` and `of(value: Float?)`; both normalize to one decimal and collapse `0` to `Empty()`.

### Factories

- `of(display: String, …)` — used by the UI's `onValueChange`. Applies the concrete formatter's parsing, validation, normalization and empty-value rules; it may change the display string.
- Typed-value `of(value, …)` — initializes from domain values or accepted picker results through the same existing limits. It may return `Invalid` for out-of-range values; nullable overloads return `Empty` for null. A supplied typed value is not automatically valid.

Factories vary per type (not every formatter exposes both). Date types take a `range`/`format`; `EmailFormatState` only accepts `value`.

### Standard types

Fifteen concrete `*FormatState` sealed classes exist in `:ui-core:state`
(`core/state/formatters/`), all `@Immutable @Serializable`; inspect the file for
the exact factories:

- **`EmailFormatState : FormatState<String>`** — `of(value: String?)`; regex-validated.
- **`PasswordFormatState : FormatState<String>`** — `of(display: String)`; `@Composable fun hint(): String` (localized hint matching the active rule).
- **`NameFormatState : FormatState<String>`** — `NameLimitation: IntRange = 1..60`; `of(display: String)`.
- **`WeightFormatState : FormatState<Float>`** — `WeightLimitation` 30..150, one decimal; `of(display)`, `of(value: Float?)`.
- **`HeightFormatState : FormatState<Int>`** — `HeightLimitation: IntRange = 100..250`; `of(display)`, `of(value: Int)`.
- **`VolumeFormatState : FormatState<Float>`** — one decimal; `of(display)`, `of(value: Float?)`.
- **`DurationFormatState : FormatState<Duration>`** — `DurationLimitation: ClosedRange<Duration>`; `of(display)`, `of(value: Duration?)`.
- **`RepetitionsFormatState : FormatState<Int>`** — `of(display)`, `of(value: Int)`; `@Composable fun hint(): String`.
- **`PercentageFormatState : FormatState<Int>`** — `of(display)`, `of(value: Int)`.
- **`IntensityFormatState : FormatState<Float>`** — `of(value: Float)`.
- **`MultiplierFormatState : FormatState<Float>`** — `of(value: Float?)`.
- **`DensityFormatState : FormatState<Float>`** — `of(value: Float?)`.
- **`DateFormatState : FormatState<LocalDate>`** — `abstract val format: DateFormat.DateOnly`; `of(value: LocalDate?, range: DateRange, format: DateFormat.DateOnly)`.
- **`DateTimeFormatState : FormatState<LocalDateTime>`** — `abstract val format: DateFormat`; `of(value: LocalDateTime?, range: DateRange, format: DateFormat)`.
- **`DateRangeFormatState : FormatState<DateRange>`** — `abstract val kind: DateRangeKind`; `fun label(): UiText?` ("Last 30 days"); `of(range)`, `of(kind)`, `of(from, to)`.

A generic "amount" field reuses the unit-specific formatter above that matches it.

### Usage

State: `val weight: WeightFormatState = WeightFormatState.Empty()`, `val due: DateFormatState = DateFormatState.Empty(format = DateFormat.DateOnly.DateMmmDdYyyy)`.

ViewModel: `update { it.copy(weight = WeightFormatState.of(raw)) }`; read the parsed value via `(state.value.weight as? WeightFormatState.Valid)?.value ?: return` (the `as? Valid` cast is canonical; no-op if not valid).

Compose: feed `value = state.weight` to the input; compute button enablement from `state.weight is WeightFormatState.Valid`.

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

Anti-patterns: `String` for an email/password/weight field; `Float?` for a weight (loses raw display, can't validate); validating on submit only; computing button enablement from raw fields (use `state.field is Valid`); side effects inside `of(...)` (factories are pure).

### Read-only value presentation

Read-only values reuse the existing unit formatter's display methods, such as
`short()` and `shortAnnotated()`. Reusable parsing, precision, grouping and unit
formatting belong to that `*FormatState`; MUST NOT add a standalone formatting
extension file or a screen-specific presentation model as a second owner.

Keep a small surface-specific display choice, such as a missing-value dash or a
known-zero label, at its rendering site instead of introducing another formatter
or presentation layer. A numeric factory's `Empty` result is an input convention;
it does not turn a recorded zero into missing historical data. Inspect
[ExerciseSummary.kt](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/training/internal/ExerciseSummary.kt)
for an existing local zero-display branch beside the regular formatter call.

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
2. **`internal data class`** by default. `public` for cross-module state in `:ui-core:state` and for the State of a public entry Component (feature roots and dialogs expose `viewModel`, whose `state`/`loaders`/`navigator` make State, Loader and Direction public; the Contract stays `internal`).
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

An operation's progress belongs to the feature's existing `*Loader` and VM
`loaders`, including independently loaded sections. MUST NOT duplicate that
progress with a parallel `Loading`/`Loaded` state or an `isLoading` field. State
holds the resolved data; the Composable combines it with the existing loader to
render a skeleton or populated content. A distinct layout alone does not justify
another owner of the same operation lifecycle.

State must not hide restoration/edit flags or previous-source copies removed from
the ViewModel. Store actual draft values and needed display data; follow the
[canonical draft policy](../../state-preservation/SKILL.md#data-backed-form-synchronization).

### Immutable collections (MUST)

`ImmutableList<T>` for read-only; `PersistentList<T>` to build incrementally (`.add`/`.remove` returning new lists). Both Compose-stable. `List<T>` / `MutableList<T>` are **forbidden** in state.

### Nullability

`UiText?` for optional description; reuse suitable `*FormatState` subtypes for validated/formatted fields; plain
nonsecret free text without formatting/validation may remain String (a server length limit becomes a formatter only once the contract states it); nullable domain refs when optional (`val user: User? = null`).

A formatter *fits* an input when its unit, range and precision match the field. When the nearest formatter does not fit (for example a seconds-precision duration against the minute-precision `DurationFormatState`), hold the typed value in State and add a sibling formatter only for a real validation/display need; do not widen a shared formatter's contract for one screen.

### Sealed-interface states

A sealed content state represents mutually exclusive data or business modes with
**truly different layouts**. Use an `@Immutable sealed interface` with `@Immutable`
on the parent AND every subtype, and an exhaustive `when` in the Composable. For
the same layout with some fields empty, use one `data class` with nullable fields.
Operation progress retains the loader owner described above.

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

Factor cohesive data into sub-state classes (`HeaderState`, `NoteRowState`); component parameters follow [model boundaries](../../compose-components/references/structure-and-runtime.md#signatures-and-data-boundaries).

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
- Component model boundaries follow [Compose signatures](../../compose-components/references/structure-and-runtime.md#signatures-and-data-boundaries).
- Flat scalar pile (group the clusters; see above).
