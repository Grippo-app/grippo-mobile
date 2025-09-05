package com.grippo.calculation.models

import com.grippo.date.utils.DateRange
import com.grippo.state.datetime.PeriodState
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus

// Keep your enum & data class
internal enum class BucketScale { EXERCISE, DAY, WEEK, MONTH }
internal data class Bucket(val start: LocalDateTime, val end: LocalDateTime)

// Tunable target: aim to keep bucket count within this bound (pick your UI sweet spot)
private const val TARGET_BUCKETS: Int = 24

internal fun deriveScale(period: PeriodState): BucketScale = when (period) {
    is PeriodState.ThisDay -> BucketScale.EXERCISE
    is PeriodState.ThisWeek -> BucketScale.DAY
    is PeriodState.ThisMonth -> BucketScale.WEEK
    is PeriodState.CUSTOM -> deriveCustomScale(period.range)
}

private fun deriveCustomScale(range: DateRange): BucketScale {
    val fromDT = range.from
    val toDT = range.to
    if (fromDT > toDT) return BucketScale.DAY

    val from = fromDT.date
    val to = toDT.date
    if (from == to) return BucketScale.EXERCISE

    val days = daysInclusive(from, to)                 // ~ exact day buckets
    val weeks = ((days + 6) / 7)                        // ceil(days/7)
    monthsInclusive(from, to)               // inclusive month span

    return when {
        days <= TARGET_BUCKETS -> BucketScale.DAY
        weeks <= TARGET_BUCKETS -> BucketScale.WEEK
        else -> BucketScale.MONTH
    }
}

internal fun daysInclusive(from: LocalDate, to: LocalDate): Int {
    // O(1): inclusive day count
    return (to.toEpochDays() - from.toEpochDays() + 1)
}

/** Inclusive month span count between two dates (month-granular). */
private fun monthsInclusive(from: LocalDate, to: LocalDate): Int {
    // Normalize to month starts, then count hops by months
    var cur = LocalDate(from.year, from.monthNumber, 1)
    val end = LocalDate(to.year, to.monthNumber, 1)
    var cnt = 0
    while (cur <= end) {
        cnt++
        cur = cur.plus(DatePeriod(months = 1))
    }
    return cnt
}

/** Optional: whole-month checker if you still use it elsewhere */
internal fun isWholeMonths(range: DateRange): Boolean {
    val from = range.from.date
    val to = range.to.date
    if (from.dayOfMonth != 1) return false
    val toIsLast = to.plus(DatePeriod(days = 1)).dayOfMonth == 1
    return toIsLast && from <= to
}