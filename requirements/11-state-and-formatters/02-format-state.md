# `*FormatState` — Form-Field State

Form fields (email, password, amount, date, ...) are represented in state as **sealed `*FormatState` classes** with `Empty` / `Invalid` / `Valid` subtypes. The UI reads the state to determine display and button enablement; the ViewModel constructs the right subtype on every input change.

All `*FormatState` types are `@Immutable @Serializable` so they survive process death inside `*Router` payloads or `DialogConfig` configs.

## Base shape (`FormatState`)

```kotlin
@Immutable
public sealed interface FormatState<T> {
    public val display: String
    public val value: T?

    public interface Valid<T> : FormatState<T>
    public interface Invalid<T> : FormatState<T>
    public interface Empty<T> : FormatState<T>
}
```

- `display: String` — the **raw text** as the user typed it. Always present.
- `value: T?` — the **parsed value** (`Double`, `String`, `LocalDate`, ...). Present only when valid.

## Standard subtypes

Each concrete `*FormatState` has three subtypes:

```kotlin
@Immutable
@Serializable
public sealed class AmountFormatState : FormatState<Double> {

    public abstract val unit: String?

    @Immutable
    @Serializable
    public data class Valid(
        override val display: String,
        override val value: Double,
        override val unit: String?,
    ) : AmountFormatState(), FormatState.Valid<Double>

    @Immutable
    @Serializable
    public data class Invalid(
        override val display: String,
        override val value: Double?,
        override val unit: String?,
    ) : AmountFormatState(), FormatState.Invalid<Double>

    @Immutable
    @Serializable
    public data class Empty(
        override val display: String = "",
        override val value: Double? = null,
        override val unit: String? = null,
    ) : AmountFormatState(), FormatState.Empty<Double>

    public companion object {
        public val AmountLimitation: ClosedFloatingPointRange<Double>
        public fun of(display: String, unit: String? = null): AmountFormatState
        public fun of(value: Double?, unit: String? = null): AmountFormatState
    }
}
```

- **`Valid`**: parsed cleanly + passes domain limits (e.g. value in `AmountLimitation`).
- **`Invalid`**: input is malformed (not a number) **or** out of range.
- **`Empty`**: blank input — distinct from `Invalid("")`. Numeric formatters also collapse the literal `0` value to `Empty` (a `Valid(0)` would mean nothing in domain terms), so `Empty` covers both "untouched" and "explicitly zeroed".

## Factories

```kotlin
public companion object {
    public fun of(display: String, unit: String? = null): AmountFormatState
    public fun of(value: Double?, unit: String? = null): AmountFormatState
}
```

- `of(display: String, …)` — used by the UI's `onValueChange`. Parses the string; returns `Empty` if blank, `Valid` if parseable + in range, `Invalid` otherwise.
- `of(value: Double?, …)` — used when initializing state from a domain value. Always produces `Valid` (or `Empty` if null), with a sensible default `display` format.

Factories vary per type — not every formatter exposes both overloads. Date types take a `range`/`format` parameter; some numeric types only accept `Double?`; `EmailFormatState` only accepts `value`. See each type below for its exact factory set.

## Standard `*FormatState` types

### `EmailFormatState`

```kotlin
@Immutable
@Serializable
public sealed class EmailFormatState : FormatState<String> {
    @Immutable @Serializable public data class Valid(override val display: String, override val value: String) : EmailFormatState(), FormatState.Valid<String>
    @Immutable @Serializable public data class Invalid(override val display: String, override val value: String?) : EmailFormatState(), FormatState.Invalid<String>
    @Immutable @Serializable public data class Empty(override val display: String = "", override val value: String? = null) : EmailFormatState(), FormatState.Empty<String>

    public companion object {
        public fun of(value: String?): EmailFormatState
    }
}
```

Uses a regex to validate the address.

### `PasswordFormatState`

```kotlin
@Immutable
@Serializable
public sealed class PasswordFormatState : FormatState<String> {
    public data class Valid(...) : PasswordFormatState(), FormatState.Valid<String>
    public data class Invalid(...) : PasswordFormatState(), FormatState.Invalid<String>
    public data class Empty(...) : PasswordFormatState(), FormatState.Empty<String>

    public companion object {
        public fun of(display: String): PasswordFormatState
    }

    @Composable
    public fun hint(): String
}
```

`hint()` returns the localized hint that matches the active validation rule (e.g. `"At least 6 characters"`). Composable because it reads strings.

### `DateFormatState`

```kotlin
public sealed class DateFormatState : FormatState<LocalDate> {
    public abstract val format: DateFormat.DateOnly

    public data class Valid(override val display: String, override val value: LocalDate, override val format: DateFormat.DateOnly) : DateFormatState(), FormatState.Valid<LocalDate>
    public data class Invalid(override val display: String, override val value: LocalDate?, override val format: DateFormat.DateOnly) : DateFormatState(), FormatState.Invalid<LocalDate>
    public data class Empty(override val format: DateFormat.DateOnly, override val display: String = "", override val value: LocalDate? = null) : DateFormatState(), FormatState.Empty<LocalDate>

    public companion object {
        public fun of(value: LocalDate?, range: DateRange, format: DateFormat.DateOnly): DateFormatState
    }
}
```

`format` is carried with the state so the `display` string is the right format. Range bounding via `range` (the date must fall within).

### `DateRangeFormatState`

```kotlin
public sealed class DateRangeFormatState : FormatState<DateRange> {
    public abstract val kind: DateRangeKind

    public data class Valid(...) : DateRangeFormatState(), FormatState.Valid<DateRange>
    public data class Invalid(...) : DateRangeFormatState(), FormatState.Invalid<DateRange>
    public data class Empty(...) : DateRangeFormatState(), FormatState.Empty<DateRange>

    public fun label(): UiText?

    public companion object {
        public fun of(range: DateRange): DateRangeFormatState
        public fun of(kind: DateRangeKind): DateRangeFormatState
        public fun of(from: LocalDateTime?, to: LocalDateTime?): DateRangeFormatState
    }
}
```

`label()` returns a `UiText` like "Last 30 days" — useful for UI labels.

### `DateTimeFormatState`

```kotlin
public sealed class DateTimeFormatState : FormatState<LocalDateTime> {
    public abstract val format: DateFormat

    public companion object {
        public fun of(
            value: LocalDateTime?,
            range: DateRange,
            format: DateFormat,
        ): DateTimeFormatState
    }
}
```

Like `DateFormatState` but for `LocalDateTime` (includes time-of-day) and accepts any `DateFormat` (not just `DateOnly`).

### `NameFormatState`

```kotlin
public sealed class NameFormatState : FormatState<String> {
    public companion object {
        public val NameLimitation: IntRange  // 1..60
        public fun of(display: String): NameFormatState
    }
}
```

Length-bounded free-form name field.

## Usage in state

```kotlin
@Immutable
@Serializable
internal data class NoteEditorState(
    val email: EmailFormatState = EmailFormatState.Empty(),
    val title: NameFormatState = NameFormatState.Empty(),
    val amount: AmountFormatState = AmountFormatState.Empty(),
    val due: DateFormatState = DateFormatState.Empty(
        format = DateFormat.DateOnly.DateMmmDdYyyy,
    ),
)
```

## Usage in ViewModel

```kotlin
override fun onAmountChange(raw: String) {
    update { it.copy(amount = AmountFormatState.of(raw)) }
}

override fun onApplyClick() {
    val amount = (state.value.amount as? AmountFormatState.Valid)?.value ?: return
    safeLaunch(loader = FooLoader.Saving) {
        noteFeature.updateAmount(amount).getOrThrow()
    }
}
```

The `as? Valid` cast is the canonical way to read the parsed value. If the user hasn't entered a valid value, the click is a no-op.

## Usage in Compose

```kotlin
InputNumeric(
    value = state.amount,
    onValueChange = contract::onAmountChange,
    label = AppTokens.strings.res(Res.string.amount),
)

val isApplyEnabled = remember(state) {
    state.amount is AmountFormatState.Valid &&
        state.title is NameFormatState.Valid
}

Button(
    onClick = contract::onApplyClick,
    text = AppTokens.strings.res(Res.string.apply),
    enabled = isApplyEnabled,
)
```

The button reads `state.amount is Valid` to know whether to enable.

## Why `display` + `value` separately

- `display` is what the **user sees** in the input field. Preserve their typing (`"72.5"` vs `"72.50"` vs `"72,5"`).
- `value` is what the **app uses** (`72.5f`). Round-tripping display → value → display would lose user formatting.

## Why `@Serializable`

`*FormatState` types are embedded in `@Immutable @Serializable` state classes in `:ui-core:state` — see `NoteState`, `TagState`, `NoteEditorState` — so they round-trip through `kotlinx.serialization`. The `@Serializable` annotation also keeps them safe to drop into a `*Router` payload or a `DialogConfig` field if a feature ever needs to carry a parsed form value across the Decompose `StateKeeper` boundary.

## Rules

- **One `*FormatState` per form field type.** Not one per screen.
- **Always start in `Empty()`.** Don't initialize as `Invalid("")`.
- **`of(...)` is the only construction path.** Don't instantiate `Valid(...)` / `Invalid(...)` directly — that bypasses validation. Use whichever `of` overload the type exposes (`of(display)`, `of(value)`, or the date variants with a `range` / `format`).
- **`Companion.Limitation` (or equivalent) is the range** for `Invalid`. Keep it static; tunable values belong in `:data-features:local-settings`.
- **`hint()` Composable methods** return localized strings — let the UI render them.

## Anti-patterns

- **`String` for an email/password/amount field in state.** Use the typed `*FormatState`.
- **`Double?` for an amount** in state — loses the user's raw display, can't validate.
- **Validating on submit only.** UI loses real-time feedback.
- **Computing button enablement from raw input fields.** Use `state.field is Valid`.
- **Side effects inside `of(...)`.** Factories are pure.
