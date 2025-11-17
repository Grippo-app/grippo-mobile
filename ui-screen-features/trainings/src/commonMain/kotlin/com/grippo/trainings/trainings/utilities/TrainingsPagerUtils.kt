package com.grippo.trainings.trainings.utilities

import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.daysUntil
import kotlin.math.max

internal val TrainingsPagerOffsets = listOf(-1, 0, 1)

internal fun DateRange.shiftForPager(offset: Int): DateRange {
    if (offset == 0) return this
    val span = pagerSpanDays() * offset
    return DateTimeUtils.shift(this, DatePeriod(days = span))
}

internal fun DateRange.pagerRanges(limitations: DateRange): Map<Int, DateRange> {
    return allowedPagerOffsets(limitations).associateWith { offset ->
        shiftForPager(offset).coerceWithin(limitations)
    }
}

private fun DateRange.allowedPagerOffsets(limitations: DateRange): List<Int> {
    if (TrainingsPagerOffsets.isEmpty()) return listOf(0)
    return TrainingsPagerOffsets
}

internal fun DateRange.pagerCombinedRange(limitations: DateRange): DateRange {
    val ranges = pagerRanges(limitations).values
    val first = ranges.firstOrNull() ?: this
    val last = ranges.lastOrNull() ?: this
    return DateRange(from = first.from, to = last.to)
}

internal fun DateRange.coerceWithin(limitations: DateRange): DateRange {
    val start = if (from < limitations.from) limitations.from else from
    val end = if (to > limitations.to) limitations.to else to
    return if (end < start) DateRange(start, start) else DateRange(start, end)
}

private fun DateRange.pagerSpanDays(): Int {
    val span = from.date.daysUntil(to.date) + 1
    return max(1, span)
}
