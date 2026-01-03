package com.grippo.data.features.api.metrics

import com.grippo.data.features.api.metrics.models.EstimatedOneRepMaxEntry
import com.grippo.data.features.api.metrics.models.EstimatedOneRepMaxSeries
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.data.features.api.training.models.Iteration
import com.grippo.data.features.api.training.models.SetExercise
import com.grippo.data.features.api.training.models.SetIteration
import com.grippo.data.features.api.training.models.Training
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlin.math.max

public class EstimatedOneRepMaxUseCase {

    private companion object {
        private const val EPS = 1e-3f
        private const val MAX_VOLUME_REPS = 12f
        private const val MIN_VOLUME_REPS = 1f
        private const val EXERCISE_LABEL_PREFIX = "Ex"
        private const val WEEK_LABEL_PREFIX = "W"
        private const val TARGET_BUCKETS = 24
        private const val PERCENTILE = 0.90f
    }

    public fun fromSetExercises(exercises: List<SetExercise>): EstimatedOneRepMaxSeries {
        return buildExerciseSeries(
            exercises = exercises,
            estimator = { exercise ->
                estimateSessionOneRm(
                    iterations = exercise.iterations,
                    weightSelector = SetIteration::volume,
                    repetitionsSelector = SetIteration::repetitions
                )
            }
        )
    }

    private fun fromExercises(exercises: List<Exercise>): EstimatedOneRepMaxSeries {
        return buildExerciseSeries(
            exercises = exercises,
            estimator = { exercise ->
                estimateSessionOneRm(
                    iterations = exercise.iterations,
                    weightSelector = Iteration::volume,
                    repetitionsSelector = Iteration::repetitions
                )
            }
        )
    }

    public fun fromTrainings(trainings: List<Training>): EstimatedOneRepMaxSeries {
        if (trainings.isEmpty()) return EstimatedOneRepMaxSeries(emptyList())
        val start = trainings.minOf { it.createdAt }
        val end = trainings.maxOf { it.createdAt }
        if (start > end) return EstimatedOneRepMaxSeries(emptyList())

        return when (val scale = deriveScale(start, end)) {
            BucketScale.Exercise -> {
                val exercises = trainings.flatMap(Training::exercises)
                if (exercises.isEmpty()) {
                    EstimatedOneRepMaxSeries(emptyList())
                } else {
                    fromExercises(exercises)
                }
            }

            else -> {
                val buckets = buildBuckets(start, end, scale)
                if (buckets.isEmpty()) return EstimatedOneRepMaxSeries(emptyList())
                val grouped = groupTrainingsByBucket(trainings, scale)

                val entries = buckets.mapNotNull { bucket ->
                    val trainings = grouped[bucket.start] ?: return@mapNotNull null
                    if (trainings.isEmpty()) return@mapNotNull null

                    val estimates = trainings
                        .flatMap(Training::exercises)
                        .mapNotNull { exercise ->
                            estimateSessionOneRm(
                                iterations = exercise.iterations,
                                weightSelector = Iteration::volume,
                                repetitionsSelector = Iteration::repetitions
                            )
                        }
                        .filter { it.isFinite() && it > 0f }

                    if (estimates.isEmpty()) {
                        null
                    } else {
                        EstimatedOneRepMaxEntry(
                            label = bucketLabel(bucket.start, scale),
                            value = percentile(estimates, PERCENTILE).coerceAtLeast(0f)
                        )
                    }
                }

                EstimatedOneRepMaxSeries(entries)
            }
        }
    }

    private fun <T> buildExerciseSeries(
        exercises: List<T>,
        estimator: (T) -> Float?,
    ): EstimatedOneRepMaxSeries {
        if (exercises.isEmpty()) return EstimatedOneRepMaxSeries(emptyList())

        val entries = exercises.mapIndexedNotNull { index, exercise ->
            val estimate = estimator(exercise) ?: return@mapIndexedNotNull null
            EstimatedOneRepMaxEntry(
                label = exerciseLabel(index),
                value = estimate.coerceAtLeast(0f)
            )
        }

        return EstimatedOneRepMaxSeries(entries)
    }

    private fun exerciseLabel(index: Int): String {
        return "$EXERCISE_LABEL_PREFIX${index + 1}"
    }

    private fun <T> estimateSessionOneRm(
        iterations: List<T>,
        weightSelector: (T) -> Float,
        repetitionsSelector: (T) -> Int,
    ): Float? {
        if (iterations.isEmpty()) return null

        val pairs = mutableListOf<Pair<Float, Float>>()
        iterations.forEach { iteration ->
            val weight = weightSelector(iteration)
            if (weight <= EPS) return@forEach

            val repsInt = repetitionsSelector(iteration)
            if (repsInt <= 0) return@forEach
            val reps = repsInt.toFloat()

            val brzycki = brzycki(weight, reps)
            val epley = epley(weight, reps)
            val average = listOfNotNull(brzycki, epley)
                .takeIf { it.isNotEmpty() }
                ?.map { it.toDouble() }
                ?.average()
                ?.toFloat()
                ?.takeIf { it.isFinite() && it > 0f }
                ?: return@forEach

            val quality = qualityWeight(reps)
            pairs += average to quality
        }

        if (pairs.isEmpty()) return null
        val median = weightedMedian(pairs)
        return median.takeIf { it.isFinite() && it > 0f }
    }

    private fun brzycki(weight: Float, reps: Float): Float? {
        if (reps !in MIN_VOLUME_REPS..MAX_VOLUME_REPS) return null
        val denom = 1.0278f - 0.0278f * reps
        if (denom <= 0f) return null
        return weight / denom
    }

    private fun epley(weight: Float, reps: Float): Float? {
        if (reps !in MIN_VOLUME_REPS..MAX_VOLUME_REPS) return null
        return weight * (1f + reps / 30f)
    }

    private fun qualityWeight(reps: Float): Float {
        val diff = reps - 4f
        return 1f / (1f + diff * diff)
    }

    private fun weightedMedian(values: List<Pair<Float, Float>>): Float {
        if (values.isEmpty()) return 0f
        val sorted = values.sortedBy { it.first }
        val totalWeight = sorted.sumOf { it.second.toDouble() }
        var acc = 0.0
        for ((value, weight) in sorted) {
            acc += weight.toDouble()
            if (acc >= totalWeight * 0.5) return value
        }
        return sorted.last().first
    }

    private fun percentile(values: List<Float>, p: Float): Float {
        if (values.isEmpty()) return 0f
        val valid = values.filter { it.isFinite() && it > 0f }
        if (valid.isEmpty()) return 0f

        val sorted = valid.sorted()
        val clamped = p.coerceIn(0f, 1f)
        val idx = ((sorted.size - 1) * clamped)
        val lo = idx.toInt()
        val hi = minOf(lo + 1, sorted.lastIndex)
        val fraction = idx - lo
        val value = sorted[lo] * (1 - fraction) + sorted[hi] * fraction
        return if (value.isFinite()) value else 0f
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

    private fun bucketLabel(timestamp: LocalDateTime, scale: BucketScale): String {
        return when (scale) {
            BucketScale.Day, BucketScale.Exercise -> DateTimeUtils.format(
                timestamp,
                DateFormat.DateOnly.DateDdMmm
            )

            BucketScale.Week -> {
                val weekNumber = isoWeekNumber(timestamp)
                val month = DateTimeUtils.format(timestamp, DateFormat.DateOnly.MonthShort)
                "$WEEK_LABEL_PREFIX${weekNumber}-$month"
            }

            BucketScale.Month -> DateTimeUtils.format(
                timestamp,
                DateFormat.DateOnly.MonthShort
            )
        }
    }

    private fun deriveScale(start: LocalDateTime, end: LocalDateTime): BucketScale {
        val days = max(1, start.date.daysUntil(end.date) + 1)
        return when {
            days == 1 -> BucketScale.Exercise
            days == 7 -> BucketScale.Day
            days in 28..31 -> BucketScale.Week
            days in 365..366 -> BucketScale.Month
            else -> deriveCustomScale(days)
        }
    }

    private fun deriveCustomScale(days: Int): BucketScale {
        val weeks = (days + DayOfWeek.entries.size - 1) / DayOfWeek.entries.size
        return when {
            days <= TARGET_BUCKETS -> BucketScale.Day
            weeks <= TARGET_BUCKETS -> BucketScale.Week
            else -> BucketScale.Month
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

    private fun buildDayBuckets(from: LocalDateTime, to: LocalDateTime): List<Bucket> {
        val buckets = mutableListOf<Bucket>()
        var current = from.date
        while (current <= to.date) {
            val dayStart = DateTimeUtils.startOfDay(current)
            val dayEnd = DateTimeUtils.endOfDay(current)
            val start = maxDateTime(dayStart, from)
            val end = minDateTime(dayEnd, to)
            buckets += Bucket(start, end)
            current = current.plus(DatePeriod(days = 1))
        }
        return buckets
    }

    private fun buildWeekBuckets(from: LocalDateTime, to: LocalDateTime): List<Bucket> {
        val buckets = mutableListOf<Bucket>()
        var weekStart = startOfWeek(from)
        while (weekStart <= to) {
            val weekEndDate = weekStart.date.plus(DatePeriod(days = 6))
            val weekEnd = DateTimeUtils.endOfDay(weekEndDate)
            val start = maxDateTime(weekStart, from)
            val end = minDateTime(weekEnd, to)
            buckets += Bucket(start, end)
            val nextWeekStartDate = weekStart.date.plus(DatePeriod(days = 7))
            weekStart = DateTimeUtils.startOfDay(nextWeekStartDate)
        }
        return buckets
    }

    private fun buildMonthBuckets(from: LocalDateTime, to: LocalDateTime): List<Bucket> {
        val buckets = mutableListOf<Bucket>()
        var monthStart = startOfMonth(from)
        while (monthStart <= to) {
            val firstOfNextMonth = LocalDate(monthStart.year, monthStart.month.number, 1)
                .plus(DatePeriod(months = 1))
            val lastDay = firstOfNextMonth.minus(DatePeriod(days = 1))
            val monthEnd = DateTimeUtils.endOfDay(lastDay)
            val start = maxDateTime(monthStart, from)
            val end = minDateTime(monthEnd, to)
            buckets += Bucket(start, end)
            val nextMonthDate = monthStart.date.plus(DatePeriod(months = 1))
            monthStart = DateTimeUtils.startOfDay(nextMonthDate)
        }
        return buckets
    }

    private fun startOfWeek(dateTime: LocalDateTime): LocalDateTime {
        val shift = dateTime.date.dayOfWeek.isoDayNumber - 1
        val monday = dateTime.date.minus(DatePeriod(days = shift))
        return LocalDateTime(monday, LocalTime(0, 0))
    }

    private fun startOfMonth(dateTime: LocalDateTime): LocalDateTime {
        return LocalDateTime(
            LocalDate(dateTime.year, dateTime.month.number, 1),
            LocalTime(0, 0)
        )
    }

    private fun maxDateTime(a: LocalDateTime, b: LocalDateTime): LocalDateTime {
        return if (a >= b) a else b
    }

    private fun minDateTime(a: LocalDateTime, b: LocalDateTime): LocalDateTime {
        return if (a <= b) a else b
    }

    private fun isoWeekNumber(weekStart: LocalDateTime): Int {
        val date = weekStart.date
        val firstJan = LocalDate(date.year, 1, 1)
        val doy = (date.toEpochDays() - firstJan.toEpochDays()).toInt() + 1
        return ((doy - 1) / DayOfWeek.entries.size) + 1
    }

    private data class Bucket(
        val start: LocalDateTime,
        val end: LocalDateTime,
    )

    private enum class BucketScale {
        Exercise,
        Day,
        Week,
        Month
    }
}
