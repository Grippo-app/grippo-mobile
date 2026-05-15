# `*FormatState` — Form-Field State

Form fields (email, password, weight, height, duration, date, ...) are represented in state as **sealed `*FormatState` classes** with `Empty` / `Invalid` / `Valid` subtypes. The UI reads the state to determine display and button enablement; the ViewModel constructs the right subtype on every input change.

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
- `value: T?` — the **parsed value** (`Float`, `Int`, `Duration`, `LocalDate`, ...). Present only when valid.

## Standard subtypes

Each concrete `*FormatState` has three subtypes:

```kotlin
@Immutable
@Serializable
public sealed class WeightFormatState : FormatState<Float> {

    @Immutable
    @Serializable
    public data class Valid(
        override val display: String,
        override val value: Float,
    ) : WeightFormatState(), FormatState.Valid<Float>

    @Immutable
    @Serializable
    public data class Invalid(
        override val display: String,
        override val value: Float?,
    ) : WeightFormatState(), FormatState.Invalid<Float>

    @Immutable
    @Serializable
    public data class Empty(
        override val display: String = "",
        override val value: Float? = null,
    ) : WeightFormatState(), FormatState.Empty<Float>

    public companion object {
        public val WeightLimitation: ClosedFloatingPointRange<Float>
        public fun of(display: String): WeightFormatState
        public fun of(value: Float?): WeightFormatState
    }
}
```

- **`Valid`**: parsed cleanly + passes domain limits (e.g. weight in `30f..150f`).
- **`Invalid`**: input is malformed (not a number) **or** out of range (e.g. weight = 200kg).
- **`Empty`**: blank input — distinct from `Invalid("")`. Numeric formatters also collapse the literal `0` value to `Empty` (a `Valid(0)` would mean nothing in domain terms), so `Empty` covers both "untouched" and "explicitly zeroed".

## Factories

```kotlin
public companion object {
    public fun of(display: String): WeightFormatState
    public fun of(value: Float?): WeightFormatState
}
```

- `of(display: String)` — used by the UI's `onValueChange`. Parses the string; returns `Empty` if blank, `Valid` if parseable + in range, `Invalid` otherwise.
- `of(value: Float?)` — used when initializing state from a domain value. Always produces `Valid` (or `Empty` if null), with a sensible default `display` format.

Factories vary per type — not every formatter exposes both overloads. Date types take a `range`/`format` parameter; some numeric types only accept `Float?`; `EmailFormatState` only accepts `value`. See each type below for its exact factory set.

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

### `WeightFormatState`

```kotlin
public sealed class WeightFormatState : FormatState<Float> {
    public companion object {
        public val WeightLimitation: ClosedFloatingPointRange<Float>
        public fun of(display: String): WeightFormatState
        public fun of(value: Float?): WeightFormatState
    }
}
```

`WeightLimitation` is the static range used for the `Invalid` check (e.g. `30f..150f`). Reference implementation also normalizes to 1 decimal place via a private `display1dp(...)` formatter — values like `72.567f` round to `72.6f` before the range check.

### `HeightFormatState`

```kotlin
public sealed class HeightFormatState : FormatState<Int> {
    public companion object {
        public val HeightLimitation: IntRange
        public fun of(display: String): HeightFormatState
        public fun of(value: Int): HeightFormatState
    }
}
```

`Int` not `Float` — height in centimeters (the reference repo uses `100..250` cm; unit selection is product-level, not a property of the type).

### `DurationFormatState`

```kotlin
public sealed class DurationFormatState : FormatState<Duration> {
    public companion object {
        public val DurationLimitation: ClosedRange<Duration>
        public fun of(display: String): DurationFormatState
        public fun of(value: Duration?): DurationFormatState
    }
}
```

`Duration` from `kotlin.time`. `display` is produced by `DateTimeUtils.format(duration)` (locale-aware abbreviated style, e.g. `"1h 23m"`); the parser accepts ISO-8601 (`Duration.parse`). Normalized to whole-minute precision.

### `VolumeFormatState`

```kotlin
public sealed class VolumeFormatState : FormatState<Float> {
    public companion object {
        public fun of(display: String): VolumeFormatState
        public fun of(value: Float?): VolumeFormatState
    }

    @Composable public fun short(): String
    @Composable public fun shortAnnotated(): AnnotatedString
    @Composable public fun hint(): String
}
```

Display formatters: `short()` returns a grouped-thousands string with the unit suffix (e.g. `"1 250,5kg"`); `shortAnnotated()` builds the same string as an `AnnotatedString`, greying out the `"-"` placeholder when `value` is null.

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

### `PercentageFormatState`

```kotlin
public sealed class PercentageFormatState : FormatState<Int> {
    public companion object {
        public fun of(display: String): PercentageFormatState
        public fun of(value: Int): PercentageFormatState
    }

    @Composable public fun short(): String                  // e.g. "42%"
    @Composable public fun shortAnnotated(): AnnotatedString
}
```

Integer percentage (no range validator — any non-zero integer is Valid).

### `IntensityFormatState`

```kotlin
public sealed class IntensityFormatState : FormatState<Float> {
    public companion object {
        public fun of(value: Float): IntensityFormatState
    }

    @Composable public fun short(): String
    @Composable public fun shortAnnotated(): AnnotatedString

    public enum class Average { LOW, MEDIUM, LARGE }
    public fun average(): Average?
}
```

`average()` returns a coarse `LOW`/`MEDIUM`/`LARGE` bucket from the parsed value — used by UI for badges / colour cues.

### `DensityFormatState`

```kotlin
public sealed class DensityFormatState : FormatState<Float> {
    public companion object {
        public fun of(value: Float?): DensityFormatState
    }

    @Composable public fun short(): String                  // "<v>kg/min"
    @Composable public fun shortAnnotated(): AnnotatedString
}
```

Training-density (kg per minute). `short()` formats as `"<value>kg/<minutes-short>"`.

### `MultiplierFormatState`

```kotlin
public sealed class MultiplierFormatState : FormatState<Float> {
    public companion object {
        public fun of(value: Float?): MultiplierFormatState
    }

    @Composable public fun short(): String                  // "<value*100>%"
    @Composable public fun shortAnnotated(): AnnotatedString
}
```

Scale factor in `0.05f..2.0f`. UI renders as a percentage (`1.25f → "125%"`).

### `RepetitionsFormatState`

```kotlin
public sealed class RepetitionsFormatState : FormatState<Int> {
    public companion object {
        public fun of(display: String): RepetitionsFormatState
        public fun of(value: Int): RepetitionsFormatState
    }

    @Composable public fun hint(): String
    @Composable public fun short(): String                  // "x<value>"
    @Composable public fun shortAnnotated(): AnnotatedString
}
```

Integer reps in `1..100`. `short()` returns `"x12"`-style.

## Usage in state

```kotlin
@Immutable
@Serializable
internal data class ProfileBodyState(
    val email: EmailFormatState = EmailFormatState.Empty(),
    val weight: WeightFormatState = WeightFormatState.Empty(),
    val height: HeightFormatState = HeightFormatState.Empty(),
    val birthday: DateFormatState = DateFormatState.Empty(
        format = DateFormat.DateOnly.DateMmmDdYyyy,
    ),
)
```

## Usage in ViewModel

```kotlin
override fun onWeightChange(raw: String) {
    update { it.copy(weight = WeightFormatState.of(raw)) }
}

override fun onApplyClick() {
    val weight = (state.value.weight as? WeightFormatState.Valid)?.value ?: return
    safeLaunch(loader = FooLoader.Saving) {
        userFeature.updateWeight(weight).getOrThrow()
    }
}
```

The `as? Valid` cast is the canonical way to read the parsed value. If the user hasn't entered a valid value, the click is a no-op.

## Usage in Compose

```kotlin
InputNumeric(
    value = state.weight,
    onValueChange = contract::onWeightChange,
    label = AppTokens.strings.res(Res.string.weight),
)

val isApplyEnabled = remember(state) {
    state.weight is WeightFormatState.Valid &&
        state.height is HeightFormatState.Valid
}

Button(
    onClick = contract::onApplyClick,
    text = AppTokens.strings.res(Res.string.apply),
    enabled = isApplyEnabled,
)
```

The button reads `state.weight is Valid` to know whether to enable.

## Why `display` + `value` separately

- `display` is what the **user sees** in the input field. Preserve their typing (`"72.5"` vs `"72.50"` vs `"72,5"`).
- `value` is what the **app uses** (`72.5f`). Round-tripping display → value → display would lose user formatting.

## Why `@Serializable`

`*FormatState` types appear inside screen `State` classes that may live across process death, inside `*Router` payloads (e.g. `TrainingsRouter.MonthlyCalendar(date: DateFormatState)`), and occasionally inside `DialogConfig`. Compose state restoration needs to deserialize them.

## Rules

- **One `*FormatState` per form field type.** Not one per screen.
- **Always start in `Empty()`.** Don't initialize as `Invalid("")`.
- **`of(...)` is the only construction path.** Don't instantiate `Valid(...)` / `Invalid(...)` directly — that bypasses validation. Use whichever `of` overload the type exposes (`of(display)`, `of(value)`, or the date variants with a `range` / `format`).
- **`Companion.Limitation` (or equivalent) is the range** for `Invalid`. Keep it static; tunable values belong in `:data-features:local-settings`.
- **`hint()` Composable methods** return localized strings — let the UI render them.

## Anti-patterns

- **`String` for an email/password/weight field in state.** Use the typed `*FormatState`.
- **`Float?` for a weight** in state — loses the user's raw display, can't validate.
- **Validating on submit only.** UI loses real-time feedback.
- **Computing button enablement from raw input fields.** Use `state.field is Valid`.
- **Side effects inside `of(...)`.** Factories are pure.
