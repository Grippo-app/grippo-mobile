package com.grippo.trainings.trainings.utilities

import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.daysUntil
import kotlin.math.max

internal const val TrainingsPagerCenterPage = 1
internal const val TrainingsPagerPageCount = 3

internal val TrainingsPagerOffsets = listOf(-1, 0, 1)

internal fun DateRange.shiftForPager(offset: Int): DateRange {
    if (offset == 0) return this
    val span = pagerSpanDays() * offset
    return DateTimeUtils.shift(this, DatePeriod(days = span))
}

internal fun DateRange.pagerRanges(): Map<Int, DateRange> {
    return TrainingsPagerOffsets.associateWith { shiftForPager(it) }
}

internal fun DateRange.pagerCombinedRange(): DateRange {
    val ranges = pagerRanges()
    val first = ranges.getValue(TrainingsPagerOffsets.first())
    val last = ranges.getValue(TrainingsPagerOffsets.last())
    return DateRange(from = first.from, to = last.to)
}

private fun DateRange.pagerSpanDays(): Int {
    val span = from.date.daysUntil(to.date) + 1
    return max(1, span)
}
