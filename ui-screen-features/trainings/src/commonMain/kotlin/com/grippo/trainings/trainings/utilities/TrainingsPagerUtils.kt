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

internal fun DateRange.allowedPagerOffsets(limitations: DateRange): List<Int> {
    val offsets = TrainingsPagerOffsets.filter { offset ->
        val range = shiftForPager(offset)
        range.isWithin(limitations)
    }

    return offsets.ifEmpty { listOf(0) }
}

internal fun DateRange.pagerRanges(limitations: DateRange): Map<Int, DateRange> {
    return allowedPagerOffsets(limitations).associateWith { shiftForPager(it) }
}

internal fun DateRange.pagerCombinedRange(limitations: DateRange): DateRange {
    val ranges = pagerRanges(limitations).values
    val first = ranges.firstOrNull() ?: this
    val last = ranges.lastOrNull() ?: this
    return DateRange(from = first.from, to = last.to)
}

private fun DateRange.isWithin(limitations: DateRange): Boolean {
    return from >= limitations.from && to <= limitations.to
}

private fun DateRange.pagerSpanDays(): Int {
    val span = from.date.daysUntil(to.date) + 1
    return max(1, span)
}
