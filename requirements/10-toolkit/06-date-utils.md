# `:toolkit:date-utils` — Date and Time

Every date and time interaction goes through this module: parsing, formatting, range arithmetic, range presets. Uses `kotlinx-datetime` types (`LocalDateTime`, `LocalDate`, `Instant`, `Duration`).

## `DateTimeUtils`

```kotlin
public object DateTimeUtils {

    public fun now(): LocalDateTime
    public fun asInstant(value: LocalDateTime): Instant

    // Date range factories
    public fun thisDay(): DateRange
    public fun leadingYear(): DateRange
    public fun thisWeek(): DateRange
    public fun thisMonth(): DateRange
    public fun thisYear(): DateRange
    public fun trailingYear(): DateRange
    public fun trailingMonth(): DateRange
    public fun trailing14Days(): DateRange
    public fun trailing60Days(): DateRange
    public fun trailingWeek(): DateRange
    public fun infinity(): DateRange

    // Time for date
    public fun startOfDay(value: LocalDateTime): LocalDateTime
    public fun endOfDay(value: LocalDateTime): LocalDateTime
    public fun startOfDay(value: LocalDate): LocalDateTime
    public fun endOfDay(value: LocalDate): LocalDateTime

    // ISO conversion (UTC)
    public fun toUtcIso(value: LocalDateTime): String
    public fun toLocalDateTime(timestamp: String): LocalDateTime

    // Formatting
    public fun format(value: LocalDateTime, format: DateFormat): String
    public fun format(value: LocalTime, format: DateFormat.TimeOnly): String
    public fun format(value: LocalDate, format: DateFormat.DateOnly): String
    public fun format(duration: Duration): String

    // Math
    public fun ago(value: LocalDateTime): Duration
    public fun minus(value: LocalDateTime, minus: Duration): LocalDateTime
    public fun plus(value: LocalDateTime, plus: Duration): LocalDateTime
    public fun shift(range: DateRange, period: DatePeriod): DateRange

    // Timeline predicates
    public fun isToday(date: LocalDate): Boolean
    public fun isYesterday(date: LocalDate): Boolean
    public fun isTomorrow(date: LocalDate): Boolean
    public fun isFuture(date: LocalDate): Boolean
    public fun isPast(date: LocalDate): Boolean
    public fun isPast(value: LocalDateTime): Boolean

    // Static helpers
    public fun getDaysInMonth(year: Int, month: Month): Int
    public fun weekDayShortLabels(): List<String>
}
```

## `DateRange`

```kotlin
@Serializable
public data class DateRange(
    val from: LocalDateTime,
    val to: LocalDateTime,
) {
    public fun isWellFormed(): Boolean
    public fun coerceWithin(limitations: DateRange): DateRange
}
```

- `@Serializable` so it can live in `*Router` payloads.
- `isWellFormed()` returns `from <= to`.
- `coerceWithin(limitations)` clamps to a bound range.

## `DateRangeKind`

```kotlin
@Serializable
public enum class DateRangeKind {
    Daily,
    Weekly,
    Last7Days,
    Last14Days,
    Monthly,
    Last30Days,
    Last60Days,
    Last365Days,
    Yearly,
    Infinity,
    Custom,
}
```

`Custom` is used when the user picks arbitrary dates that don't match any preset.

## `DateRangePresets`

```kotlin
public object DateRangePresets {
    public fun daily(): DateRange
    public fun weekly(): DateRange
    public fun last7Days(): DateRange
    public fun last14Days(): DateRange
    public fun monthly(): DateRange
    public fun last30Days(): DateRange
    public fun last60Days(): DateRange
    public fun last365Days(): DateRange
    public fun yearly(): DateRange
    public fun infinity(): DateRange

    public fun resolve(kind: DateRangeKind): DateRange?
    public fun classify(range: DateRange): DateRangeKind
}
```

- `resolve(kind)` returns the concrete range for a preset, or `null` for `Custom`.
- `classify(range)` reverses: if the range exactly matches a preset's bounds, returns the preset kind; otherwise `Custom`.

## `DateFormat`

```kotlin
@Immutable
@Serializable
public sealed interface DateFormat {
    public val pattern: String

    @Serializable
    public sealed interface DateOnly : DateFormat {
        public data object MonthFull : DateOnly
        public data object MonthFullStandalone : DateOnly
        public data object MonthShort : DateOnly
        public data object MonthShortStandalone : DateOnly
        public data object DateMmmDdYyyy : DateOnly        // "Jan 5, 2026"
        public data object MmmYyyy : DateOnly              // "Jan 2026"
        public data object DateMmmDdComma : DateOnly       // "Jan 5,"
        public data object DateDdMmm : DateOnly            // "5 Jan"
        public data object Mmmm : DateOnly                 // "January"
        public data object DateDdMmmm : DateOnly           // "5 January"
        public data object WeekdayShort : DateOnly         // "Mon"
        public data object WeekdayLong : DateOnly          // "Monday"
    }

    @Serializable
    public sealed interface TimeOnly : DateFormat {
        public data object Time24hHm : TimeOnly             // "14:30"
    }
}
```

## `DateFormatting`

```kotlin
public object DateFormatting {
    internal var current: DateFormatter
        private set

    public fun install(tag: String?)
}
```

`DateFormatting.install(localeTag)` swaps the internal `DateFormatter` to one configured for the given locale. Called from `RootScreen`:

```kotlin
LaunchedEffect(systemLocaleTag) {
    DateFormatting.install(systemLocaleTag)
}
```

After install, every `DateTimeUtils.format(...)` call uses the new locale. Without `install`, formatters use the system locale at process start — which may diverge if the user changes the language while the app is running.

## Usage

### Ranges in state

```kotlin
@Immutable
@Serializable
internal data class TrainingsListState(
    val range: DateRange = DateRangePresets.last30Days(),
    val rangeKind: DateRangeKind = DateRangeKind.Last30Days,
    // ...
)
```

### Range arithmetic

```kotlin
// "Previous month"
val prev = DateTimeUtils.shift(state.range, period = DatePeriod(months = -1))
```

### Formatting

```kotlin
val label = DateTimeUtils.format(
    training.createdAt,
    DateFormat.DateOnly.DateMmmDdYyyy,
)  // "Jan 5, 2026"

val durationLabel = DateTimeUtils.format(duration)  // "1h 23m"
```

### ISO-UTC for backend

```kotlin
val startUtc = DateTimeUtils.toUtcIso(range.from)        // "2026-01-05T00:00:00Z"
val endUtc = DateTimeUtils.toUtcIso(range.to)
api.getTrainings(startUtc, endUtc)
```

### Parsing from backend

```kotlin
val createdAt = DateTimeUtils.toLocalDateTime(dto.createdAt!!)
```

`toLocalDateTime` assumes input is ISO-8601 UTC. The result is in the **system zone** — so 14:00 UTC in the backend response renders as 14:00 in the user's local time only if they're in UTC; otherwise it's converted to their wall time.

### Timeline labels

```kotlin
@Composable
fun friendlyDate(date: LocalDate): String = when {
    DateTimeUtils.isToday(date) -> AppTokens.strings.res(Res.string.today)
    DateTimeUtils.isYesterday(date) -> AppTokens.strings.res(Res.string.yesterday)
    DateTimeUtils.isTomorrow(date) -> AppTokens.strings.res(Res.string.tomorrow)
    else -> DateTimeUtils.format(date, DateFormat.DateOnly.DateMmmDdYyyy)
}
```

## Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android { namespace = "com.<org>.<product>.toolkit.date.utils" }

    sourceSets.commonMain.dependencies {
        implementation(projects.designSystem.resources.provider)   // for Res.string localized labels
        implementation(projects.designSystem.core)
        implementation(projects.toolkit.logger)
        implementation(compose.foundation)
        implementation(libs.datetime)
        implementation(libs.kotlinx.serialization.json)
    }
}
```

## Rules

- **All dates exchanged with the backend are ISO-8601 UTC strings.** Convert at the boundary.
- **All dates in domain models are `LocalDateTime` / `LocalDate` / `Duration` / `Instant`.** No `java.util.Date`, no `Long` epoch.
- **All formatting goes through `DateTimeUtils.format(...)`.** No `SimpleDateFormat` ever.
- **`DateFormatting.install(localeTag)` is called from `RootScreen` whenever the system locale changes.**
- **Range pickers expose `DateRangeKind`.** The kind is the source of truth; the range is computed from it.

## Anti-patterns

- **`java.util.Date`, `SimpleDateFormat`, `Calendar`.** Forbidden — Java types don't work on iOS.
- **`System.currentTimeMillis()` / `Date()`.** Use `DateTimeUtils.now()`.
- **Inline `LocalDateTime.format(...)` with custom patterns.** Use `DateFormat.<...>`.
- **Storing date as `String` in domain.** Use `LocalDateTime`. Strings are for DTOs and DB entities only.
- **Hardcoded month names.** Use `DateFormat.DateOnly.MonthFull` / `MonthShort`.
- **Forgetting `DateFormatting.install(localeTag)`.** Dates render in the wrong language after system locale change.
- **Different ISO conventions in different DTOs.** Always `toUtcIso(...)` for backend.
