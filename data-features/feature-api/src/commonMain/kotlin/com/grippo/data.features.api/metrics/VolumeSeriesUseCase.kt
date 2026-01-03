package com.grippo.data.features.api.metrics

import com.grippo.data.features.api.metrics.models.VolumeSeries
import com.grippo.data.features.api.metrics.models.VolumeSeriesEntry
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.data.features.api.training.models.Iteration
import com.grippo.data.features.api.training.models.Training
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlin.math.max

public class VolumeSeriesUseCase {

    private companion object {
        private const val EPS = 1e-3f
        private const val TARGET_BUCKETS = 24
        private const val WEEK_LABEL_PREFIX = "W"
        private const val EXERCISE_LABEL_PREFIX = "Ex"
    }

    public fun fromExercises(exercises: List<Exercise>): VolumeSeries {
        if (exercises.isEmpty()) return VolumeSeries(emptyList())

        val entries = exercises.mapIndexed { index, exercise ->
            VolumeSeriesEntry(
                label = exerciseLabel(exercise, index),
                value = exerciseVolume(exercise.iterations)
            )
        }
        return VolumeSeries(entries)
    }

    public fun fromTrainings(trainings: List<Training>): VolumeSeries {
        if (trainings.isEmpty()) return VolumeSeries(emptyList())

        val start = trainings.minOf { it.createdAt }
        val end = trainings.maxOf { it.createdAt }
        if (start > end) return VolumeSeries(emptyList())

        return when (val scale = deriveScale(start, end)) {
            BucketScale.Exercise -> {
                val exercises = trainings.flatMap(Training::exercises)
                if (exercises.isEmpty()) {
                    VolumeSeries(emptyList())
                } else {
                    fromExercises(exercises)
                }
            }

            else -> {
                val buckets = buildBuckets(start, end, scale)
                if (buckets.isEmpty()) return VolumeSeries(emptyList())
                val grouped = groupTrainingsByBucket(trainings, scale)

                val entries = buckets.mapNotNull { bucket ->
                    val trainings = grouped[bucket.start] ?: return@mapNotNull null
                    if (trainings.isEmpty()) return@mapNotNull null
                    val total = trainings
                        .sumOf { training -> sessionVolume(training) }
                        .toFloat()
                        .coerceAtLeast(0f)
                    VolumeSeriesEntry(
                        label = bucketLabel(bucket, scale),
                        value = total
                    )
                }

                VolumeSeries(entries)
            }
        }
    }

    private fun exerciseLabel(exercise: Exercise, index: Int): String {
        return exercise.name
    }

    private fun exerciseVolume(iterations: List<Iteration>): Float {
        var total = 0.0
        iterations.forEach { iteration ->
            val repetitions = iteration.repetitions
            val weight = iteration.volume
            if (repetitions > 0 && weight > EPS) {
                total += repetitions.toDouble() * weight.toDouble()
            }
        }
        return total.toFloat().coerceAtLeast(0f)
    }

    private fun sessionVolume(training: Training): Double {
        var total = 0.0
        training.exercises.forEach { exercise ->
            exercise.iterations.forEach { iteration ->
                val repetitions = iteration.repetitions
                val weight = iteration.volume
                if (repetitions > 0 && weight > EPS) {
                    total += repetitions.toDouble() * weight.toDouble()
                }
            }
        }
        return total
    }

    private fun deriveScale(start: LocalDateTime, end: LocalDateTime): BucketScale {
        val days = max(1, start.date.daysUntil(end.date) + 1)
        if (days <= 1) return BucketScale.Exercise
        val weeks = (days + DayOfWeek.entries.size - 1) / DayOfWeek.entries.size
        return when {
            days <= TARGET_BUCKETS -> BucketScale.Day
            weeks <= TARGET_BUCKETS -> BucketScale.Week
            else -> BucketScale.Month
        }
    }

    private fun groupTrainingsByBucket(
        trainings: List<Training>,
        scale: BucketScale,
    ): Map<LocalDateTime, List<Training>> {
        return trainings.groupBy { training ->
            when (scale) {
                BucketScale.Exercise -> training.createdAt
                BucketScale.Day -> DateTimeUtils.startOfDay(training.createdAt)
                BucketScale.Week -> startOfWeek(training.createdAt)
                BucketScale.Month -> startOfMonth(training.createdAt)
            }
        }
    }

    private fun buildBuckets(
        start: LocalDateTime,
        end: LocalDateTime,
        scale: BucketScale,
    ): List<Bucket> {
        return when (scale) {
            BucketScale.Exercise -> emptyList()
            BucketScale.Day -> buildDayBuckets(start, end)
            BucketScale.Week -> buildWeekBuckets(start, end)
            BucketScale.Month -> buildMonthBuckets(start, end)
        }
    }

    private fun buildDayBuckets(
        from: LocalDateTime,
        to: LocalDateTime,
    ): List<Bucket> {
        val buckets = mutableListOf<Bucket>()
        var current = from.date

        while (current <= to.date) {
            val dayStart = DateTimeUtils.startOfDay(current)
            val dayEnd = DateTimeUtils.endOfDay(current)

            val start = maxOf(dayStart, from)
            val end = minOf(dayEnd, to)

            buckets += Bucket(start = start, end = end)
            current = current.plus(DatePeriod(days = 1))
        }

        return buckets
    }

    private fun buildWeekBuckets(
        from: LocalDateTime,
        to: LocalDateTime,
    ): List<Bucket> {
        val buckets = mutableListOf<Bucket>()
        var weekStart = startOfWeek(from)

        while (weekStart <= to) {
            val weekEndDate = weekStart.date.plus(DatePeriod(days = 6))
            val weekEnd = DateTimeUtils.endOfDay(weekEndDate)

            val start = maxOf(weekStart, from)
            val end = minOf(weekEnd, to)

            buckets += Bucket(start = start, end = end)
            val nextWeekStartDate = weekStart.date.plus(DatePeriod(days = 7))
            weekStart = DateTimeUtils.startOfDay(nextWeekStartDate)
        }

        return buckets
    }

    private fun buildMonthBuckets(
        from: LocalDateTime,
        to: LocalDateTime,
    ): List<Bucket> {
        val buckets = mutableListOf<Bucket>()
        var monthStart = startOfMonth(from)

        while (monthStart <= to) {
            val firstOfNext = LocalDate(monthStart.year, monthStart.month.number, 1)
                .plus(DatePeriod(months = 1))
            val lastDay = firstOfNext.minus(DatePeriod(days = 1))
            val monthEnd = DateTimeUtils.endOfDay(lastDay)

            val start = maxOf(monthStart, from)
            val end = minOf(monthEnd, to)

            buckets += Bucket(start = start, end = end)
            val nextMonthStartDate = monthStart.date.plus(DatePeriod(months = 1))
            monthStart = DateTimeUtils.startOfDay(nextMonthStartDate)
        }

        return buckets
    }

    private fun bucketLabel(bucket: Bucket, scale: BucketScale): String {
        return when (scale) {
            BucketScale.Day -> DateTimeUtils.format(
                bucket.start,
                DateFormat.DateOnly.WeekdayShort
            )

            BucketScale.Week -> {
                val weekNumber = isoWeekNumber(bucket.start)
                val month = DateTimeUtils.format(bucket.start, DateFormat.DateOnly.MonthShort)
                "$WEEK_LABEL_PREFIX$weekNumber-$month"
            }

            BucketScale.Month -> DateTimeUtils.format(
                bucket.start,
                DateFormat.DateOnly.MonthShort
            )

            BucketScale.Exercise -> ""
        }
    }

    private fun isoWeekNumber(weekStartMonday: LocalDateTime): Int {
        val date = weekStartMonday.date
        val firstJan = LocalDate(date.year, 1, 1)
        val dayOfYear = (date.toEpochDays() - firstJan.toEpochDays()).toInt() + 1
        return ((dayOfYear - 1) / DayOfWeek.entries.size) + 1
    }

    private fun startOfWeek(dateTime: LocalDateTime): LocalDateTime {
        val shift = dateTime.date.dayOfWeek.isoDayNumber - 1
        val monday = dateTime.date.minus(DatePeriod(days = shift))
        return DateTimeUtils.startOfDay(monday)
    }

    private fun startOfMonth(dateTime: LocalDateTime): LocalDateTime {
        val firstDay = LocalDate(dateTime.year, dateTime.month.number, 1)
        return DateTimeUtils.startOfDay(firstDay)
    }

    private enum class BucketScale {
        Exercise,
        Day,
        Week,
        Month
    }

    private data class Bucket(
        val start: LocalDateTime,
        val end: LocalDateTime,
    )
}
