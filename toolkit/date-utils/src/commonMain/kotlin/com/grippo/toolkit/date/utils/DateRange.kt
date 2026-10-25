package com.grippo.toolkit.date.utils

import kotlinx.datetime.LocalDate
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

    public fun daily(anchor: LocalDate = DateTimeUtils.today()): DateRange = DateTimeUtils.thisDay(anchor)
    public fun weekly(anchor: LocalDate = DateTimeUtils.today()): DateRange = DateTimeUtils.thisWeek(anchor)
    public fun last7Days(anchor: LocalDate = DateTimeUtils.today()): DateRange = DateTimeUtils.trailingWeek(anchor)
    public fun last14Days(anchor: LocalDate = DateTimeUtils.today()): DateRange = DateTimeUtils.trailing14Days(anchor)
    public fun monthly(anchor: LocalDate = DateTimeUtils.today()): DateRange = DateTimeUtils.thisMonth(anchor)
    public fun last30Days(anchor: LocalDate = DateTimeUtils.today()): DateRange = DateTimeUtils.trailingMonth(anchor)
    public fun last60Days(anchor: LocalDate = DateTimeUtils.today()): DateRange = DateTimeUtils.trailing60Days(anchor)
    public fun last365Days(anchor: LocalDate = DateTimeUtils.today()): DateRange =
        DateTimeUtils.trailingDays(days = 365, anchor = anchor)
    public fun yearly(anchor: LocalDate = DateTimeUtils.today()): DateRange = DateTimeUtils.thisYear(anchor)
    public fun infinity(): DateRange = DateTimeUtils.infinity()

    public fun resolve(kind: DateRangeKind, anchor: LocalDate = DateTimeUtils.today()): DateRange? = when (kind) {
        DateRangeKind.Daily -> daily(anchor)
        DateRangeKind.Weekly -> weekly(anchor)
        DateRangeKind.Last7Days -> last7Days(anchor)
        DateRangeKind.Last14Days -> last14Days(anchor)
        DateRangeKind.Monthly -> monthly(anchor)
        DateRangeKind.Last30Days -> last30Days(anchor)
        DateRangeKind.Last60Days -> last60Days(anchor)
        DateRangeKind.Last365Days -> last365Days(anchor)
        DateRangeKind.Yearly -> yearly(anchor)
        DateRangeKind.Infinity -> infinity()
        DateRangeKind.Custom -> null
    }

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
