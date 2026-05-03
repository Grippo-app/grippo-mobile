package com.grippo.toolkit.date.utils

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.daysUntil
import kotlinx.serialization.Serializable
import kotlin.math.max

@Serializable
public data class DateRange(
    val from: LocalDateTime,
    val to: LocalDateTime,
) {
    public fun isWellFormed(): Boolean = from <= to

    /**
     * Clamps this range inside [limitations]. If the resulting range would be
     * inverted (end before start), collapses it to a zero-length range at the
     * coerced start.
     */
    public fun coerceWithin(limitations: DateRange): DateRange {
        val start = if (from < limitations.from) limitations.from else from
        val end = if (to > limitations.to) limitations.to else to
        return if (end < start) DateRange(start, start) else DateRange(start, end)
    }
}

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

public object DateRangePresets {

    public fun daily(): DateRange = DateTimeUtils.thisDay()
    public fun weekly(): DateRange = DateTimeUtils.thisWeek()
    public fun last7Days(): DateRange = DateTimeUtils.trailingWeek()
    public fun last14Days(): DateRange = DateTimeUtils.trailing14Days()
    public fun monthly(): DateRange = DateTimeUtils.thisMonth()
    public fun last30Days(): DateRange = DateTimeUtils.trailingMonth()
    public fun last60Days(): DateRange = DateTimeUtils.trailing60Days()
    public fun last365Days(): DateRange = DateTimeUtils.trailingYear()
    public fun yearly(): DateRange = DateTimeUtils.thisYear()
    public fun infinity(): DateRange = DateTimeUtils.infinity()

    /**
     * Materializes a preset kind into a concrete [DateRange].
     * Returns null for [DateRangeKind.Custom] — custom ranges carry their
     * own endpoints and cannot be resolved from the kind alone.
     */
    public fun resolve(kind: DateRangeKind): DateRange? = when (kind) {
        DateRangeKind.Daily -> daily()
        DateRangeKind.Weekly -> weekly()
        DateRangeKind.Last7Days -> last7Days()
        DateRangeKind.Last14Days -> last14Days()
        DateRangeKind.Monthly -> monthly()
        DateRangeKind.Last30Days -> last30Days()
        DateRangeKind.Last60Days -> last60Days()
        DateRangeKind.Last365Days -> last365Days()
        DateRangeKind.Yearly -> yearly()
        DateRangeKind.Infinity -> infinity()
        DateRangeKind.Custom -> null
    }

    /**
     * Classifies an arbitrary [DateRange] into a [DateRangeKind].
     * Prefers exact preset matches, then falls back to day-count heuristics,
     * then to [DateRangeKind.Custom].
     */
    public fun classify(range: DateRange): DateRangeKind {
        val days = max(0, range.from.date.daysUntil(range.to.date) + 1)

        return when {
            matches(range, daily()) -> DateRangeKind.Daily
            matches(range, weekly()) -> DateRangeKind.Weekly
            matches(range, last7Days()) -> DateRangeKind.Last7Days
            matches(range, last14Days()) -> DateRangeKind.Last14Days
            matches(range, monthly()) -> DateRangeKind.Monthly
            matches(range, last30Days()) -> DateRangeKind.Last30Days
            matches(range, last60Days()) -> DateRangeKind.Last60Days
            matches(range, last365Days()) -> DateRangeKind.Last365Days
            matches(range, yearly()) -> DateRangeKind.Yearly
            matches(range, infinity()) -> DateRangeKind.Infinity
            days == 1 -> DateRangeKind.Daily
            days == 7 -> DateRangeKind.Last7Days
            days == 14 -> DateRangeKind.Last14Days
            days == 30 -> DateRangeKind.Last30Days
            days == 60 -> DateRangeKind.Last60Days
            days in 28..31 -> DateRangeKind.Monthly
            days in 365..366 -> DateRangeKind.Yearly
            else -> DateRangeKind.Custom
        }
    }

    private fun matches(a: DateRange, b: DateRange): Boolean {
        return a.from == b.from && a.to == b.to
    }
}