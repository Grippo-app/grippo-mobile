package com.grippo.toolkit.calculation.internal

import com.grippo.core.state.datetime.PeriodState
import com.grippo.core.state.trainings.TrainingState
import com.grippo.toolkit.calculation.models.Bucket
import com.grippo.toolkit.calculation.models.BucketScale
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus

private const val TARGET_BUCKETS: Int = 24

internal fun groupTrainingsByBucket(
    trainings: List<TrainingState>,
    scale: BucketScale
): Map<LocalDateTime, List<TrainingState>> {
    return trainings.groupBy { t ->
        when (scale) {
            BucketScale.EXERCISE -> t.createdAt // not used
            BucketScale.DAY -> com.grippo.toolkit.date.utils.DateTimeUtils.startOfDay(t.createdAt)
            BucketScale.WEEK -> startOfWeek(t.createdAt)
            BucketScale.MONTH -> startOfMonth(t.createdAt)
        }
    }
}

internal fun deriveScale(period: PeriodState): BucketScale = when (period) {
    is PeriodState.ThisDay -> BucketScale.EXERCISE
    is PeriodState.ThisWeek -> BucketScale.DAY
    is PeriodState.ThisMonth -> BucketScale.WEEK
    is PeriodState.ThisYear -> BucketScale.MONTH
    is PeriodState.Custom -> deriveCustomScale(period.range)
}

private fun deriveCustomScale(range: DateRange): BucketScale {
    // Guard invalid range
    if (range.from > range.to) return BucketScale.DAY

    val from = range.from.date
    val to = range.to.date

    // Single-day custom range: select by mode
    if (from == to) {
        return BucketScale.EXERCISE
    }

    // Multi-day range: calendar bucketing only
    val days = daysInclusive(from, to)
    val weeks = (days + 6) / 7
    return when {
        days <= TARGET_BUCKETS -> BucketScale.DAY
        weeks <= TARGET_BUCKETS -> BucketScale.WEEK
        else -> BucketScale.MONTH
    }
}

internal fun buildBuckets(range: DateRange, scale: BucketScale): List<Bucket> = when (scale) {
    BucketScale.EXERCISE -> emptyList() // handled by per-exercise path
    BucketScale.DAY -> buildDayBuckets(range)
    BucketScale.WEEK -> buildWeekBuckets(range)
    BucketScale.MONTH -> buildMonthBuckets(range)
}

internal fun buildDayBuckets(range: DateRange): List<Bucket> {
    val buckets = mutableListOf<Bucket>()
    var currentDate = range.from.date

    while (currentDate <= range.to.date) {
        val dayStart = LocalDateTime(currentDate, LocalTime(0, 0))
        val dayEnd = LocalDateTime(currentDate, LocalTime(23, 59, 59, 999_000_000))

        val start = maxDT(dayStart, range.from)
        val end = minDT(dayEnd, range.to)

        buckets += Bucket(start, end)
        currentDate = currentDate.plus(DatePeriod(days = 1))
    }

    return buckets
}

/**
 * Builds week buckets for the given range (Monday to Sunday).
 */
internal fun buildWeekBuckets(range: DateRange): List<Bucket> {
    val buckets = mutableListOf<Bucket>()
    var weekStart = startOfWeek(range.from)

    while (weekStart <= range.to) {
        val weekEndDate = weekStart.date.plus(DatePeriod(days = 6))
        val weekEnd = LocalDateTime(weekEndDate, LocalTime(23, 59, 59, 999_000_000))

        val start = maxDT(weekStart, range.from)
        val end = minDT(weekEnd, range.to)

        buckets += Bucket(start, end)
        weekStart = LocalDateTime(weekStart.date.plus(DatePeriod(days = 7)), LocalTime(0, 0))
    }

    return buckets
}

/**
 * Builds month buckets for the given range.
 */
internal fun buildMonthBuckets(range: DateRange): List<Bucket> {
    val buckets = mutableListOf<Bucket>()
    var monthStart = startOfMonth(range.from)

    while (monthStart <= range.to) {
        val firstOfNext = LocalDate(monthStart.year, monthStart.monthNumber, 1)
            .plus(DatePeriod(months = 1))
        val lastDay = firstOfNext.minus(DatePeriod(days = 1))
        val monthEnd = LocalDateTime(lastDay, LocalTime(23, 59, 59, 999_000_000))

        val start = maxDT(monthStart, range.from)
        val end = minDT(monthEnd, range.to)

        buckets += Bucket(start, end)
        monthStart = LocalDateTime(monthStart.date.plus(DatePeriod(months = 1)), LocalTime(0, 0))
    }

    return buckets
}
