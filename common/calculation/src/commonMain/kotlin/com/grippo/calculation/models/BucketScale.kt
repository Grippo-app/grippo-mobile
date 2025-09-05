package com.grippo.calculation.models

import com.grippo.date.utils.DateRange
import com.grippo.state.datetime.PeriodState
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus

internal enum class BucketScale { EXERCISE, DAY, WEEK, MONTH }

/** Inclusive date span bucket (closed range [start .. end]). */
internal data class Bucket(val start: LocalDateTime, val end: LocalDateTime)

internal fun deriveScale(period: PeriodState): BucketScale = when (period) {
    is PeriodState.ThisDay -> BucketScale.EXERCISE
    is PeriodState.ThisWeek -> BucketScale.DAY
    is PeriodState.ThisMonth -> BucketScale.WEEK
    is PeriodState.CUSTOM -> deriveCustomScale(period.range)
}

private fun deriveCustomScale(range: DateRange): BucketScale {
    val from = range.from.date
    val to = range.to.date
    if (from == to) return BucketScale.EXERCISE

    val days = daysInclusive(from, to)
    val fullMonths = isWholeMonths(range)

    return when {
        days < 30 && (days % 7 != 0) -> BucketScale.DAY
        days < 30 && (days % 7 == 0) -> BucketScale.WEEK
        days in 30..366 -> if (fullMonths) BucketScale.MONTH else BucketScale.WEEK
        else -> BucketScale.MONTH
    }
}

internal fun daysInclusive(from: LocalDate, to: LocalDate): Int {
    var cnt = 0
    var cur = from
    while (cur <= to) {
        cnt++
        cur = cur.plus(DatePeriod(days = 1))
    }
    return cnt
}

/** Check if the DateRange covers whole months exactly (from 1st day to last day). */
internal fun isWholeMonths(range: DateRange): Boolean {
    val from = range.from.date
    val to = range.to.date
    if (from.dayOfMonth != 1) return false
    val toIsLast = to.plus(DatePeriod(days = 1)).dayOfMonth == 1
    return toIsLast && from <= to
}