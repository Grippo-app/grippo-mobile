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
        private const val TOP_SETS = 3
    }

    public sealed interface Segment {
        public data object All : Segment
        public data class ExerciseExample(val id: String) : Segment
    }

    public sealed interface Smoothing {
        public data object None : Smoothing
        public data class RollingDays(
            val days: Int,
            val method: RollingMethod,
        ) : Smoothing
    }

    public enum class RollingMethod {
        Median,
        Max,
    }

    public fun fromSetExercises(exercises: List<SetExercise>): EstimatedOneRepMaxSeries {
        val entries = exercises.mapIndexedNotNull { index, exercise ->
            val estimate = estimateExerciseSessionOneRm(
                iterations = exercise.iterations,
                weightSelector = SetIteration::volume,
                repetitionsSelector = SetIteration::repetitions,
            ) ?: return@mapIndexedNotNull null

            EstimatedOneRepMaxEntry(
                label = exerciseLabel(index),
                value = estimate.value,
                confidence = estimate.confidence,
                start = exercise.createdAt,
                sampleCount = 1,
            )
        }
        return EstimatedOneRepMaxSeries(entries)
    }

    private fun fromExercises(exercises: List<Exercise>): EstimatedOneRepMaxSeries {
        val entries = exercises.mapIndexedNotNull { index, exercise ->
            val estimate = estimateExerciseSessionOneRm(
                iterations = exercise.iterations,
                weightSelector = Iteration::volume,
                repetitionsSelector = Iteration::repetitions,
            ) ?: return@mapIndexedNotNull null

            EstimatedOneRepMaxEntry(
                label = exerciseLabel(index),
                value = estimate.value,
                confidence = estimate.confidence,
                start = exercise.createdAt,
                sampleCount = 1,
            )
        }
        return EstimatedOneRepMaxSeries(entries)
    }

    public fun fromTrainings(
        trainings: List<Training>,
        segment: Segment = Segment.All,
        smoothing: Smoothing = Smoothing.RollingDays(days = 7, method = RollingMethod.Median),
    ): EstimatedOneRepMaxSeries {
        if (trainings.isEmpty()) return EstimatedOneRepMaxSeries(emptyList())
        val start = trainings.minOf { it.createdAt }
        val end = trainings.maxOf { it.createdAt }
        if (start > end) return EstimatedOneRepMaxSeries(emptyList())

        return when (val scale = deriveScale(start, end)) {
            BucketScale.Exercise -> {
                val exercises = trainings
                    .flatMap(Training::exercises)
                    .filter { exercise ->
                        when (segment) {
                            Segment.All -> true
                            is Segment.ExerciseExample -> exercise.exerciseExample.id == segment.id
                        }
                    }
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

                val rawEntries = buckets.mapNotNull { bucket ->
                    val bucketTrainings = grouped[bucket.start] ?: return@mapNotNull null
                    if (bucketTrainings.isEmpty()) return@mapNotNull null

                    val estimates = bucketTrainings.mapNotNull { training ->
                        estimateTrainingOneRm(training = training, segment = segment)
                    }
                    if (estimates.isEmpty()) return@mapNotNull null

                    val best = estimates.maxBy { it.value }

                    EstimatedOneRepMaxEntry(
                        label = bucketLabel(bucket.start, scale),
                        value = best.value,
                        confidence = best.confidence,
                        start = bucket.start,
                        sampleCount = estimates.size,
                    )
                }.sortedBy { it.start }

                val entries = applySmoothing(rawEntries, smoothing)
                EstimatedOneRepMaxSeries(entries)
            }
        }
    }

    private fun exerciseLabel(index: Int): String {
        return "$EXERCISE_LABEL_PREFIX${index + 1}"
    }

    private data class SessionEstimate(
        val value: Float,
        val confidence: Float,
    )

    private data class SetEstimate(
        val oneRm: Float,
        val confidence: Float,
    )

    private fun <T> estimateExerciseSessionOneRm(
        iterations: List<T>,
        weightSelector: (T) -> Float,
        repetitionsSelector: (T) -> Int,
    ): SessionEstimate? {
        if (iterations.isEmpty()) return null

        val setEstimates = iterations.mapNotNull { iteration ->
            val weight = weightSelector(iteration)
            if (weight <= EPS) return@mapNotNull null
            val reps = repetitionsSelector(iteration)
            estimateSetOneRm(weight = weight, reps = reps)
        }

        if (setEstimates.isEmpty()) return null

        val top = setEstimates
            .sortedByDescending { it.oneRm }
            .take(TOP_SETS)
        if (top.isEmpty()) return null

        val value = top.map { it.oneRm }.average().toFloat()
        if (!value.isFinite() || value <= 0f) return null

        val coverage = (top.size.toFloat() / TOP_SETS.toFloat()).coerceIn(0f, 1f)
        val confidence = (top.map { it.confidence }.average().toFloat() * coverage).coerceIn(0f, 1f)

        return SessionEstimate(value = value, confidence = confidence)
    }

    private fun estimateTrainingOneRm(training: Training, segment: Segment): SessionEstimate? {
        val exercises = training.exercises.filter { exercise ->
            when (segment) {
                Segment.All -> true
                is Segment.ExerciseExample -> exercise.exerciseExample.id == segment.id
            }
        }
        if (exercises.isEmpty()) return null

        val estimates = exercises.mapNotNull { exercise ->
            estimateExerciseSessionOneRm(
                iterations = exercise.iterations,
                weightSelector = Iteration::volume,
                repetitionsSelector = Iteration::repetitions,
            )
        }
        if (estimates.isEmpty()) return null
        return estimates.maxBy { it.value }
    }

    private fun estimateSetOneRm(weight: Float, reps: Int): SetEstimate? {
        if (weight <= EPS) return null
        if (reps <= 0) return null

        val repsF = reps.toFloat()
        if (repsF !in MIN_VOLUME_REPS..MAX_VOLUME_REPS) return null

        val brzycki = brzycki(weight, repsF)
        val epley = epley(weight, repsF)
        val estimates = listOfNotNull(brzycki, epley)
        if (estimates.isEmpty()) return null

        val oneRm = (estimates.map { it.toDouble() }.average()).toFloat()
        if (!oneRm.isFinite() || oneRm <= 0f) return null

        val confidence = repsConfidence(repsF)
        return SetEstimate(oneRm = oneRm, confidence = confidence)
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

    /**
     * Confidence is intentionally lower for higher reps (formulas less stable).
     * Peaks around ~4 reps.
     */
    private fun repsConfidence(reps: Float): Float {
        val diff = reps - 4f
        val base = 1f / (1f + diff * diff)
        return base.coerceIn(0f, 1f)
    }

    private fun applySmoothing(
        entries: List<EstimatedOneRepMaxEntry>,
        smoothing: Smoothing,
    ): List<EstimatedOneRepMaxEntry> {
        if (entries.isEmpty()) return entries
        return when (smoothing) {
            Smoothing.None -> entries
            is Smoothing.RollingDays -> {
                val days = smoothing.days.coerceAtLeast(1)
                entries.map { current ->
                    val windowFrom = current.start.date.minus(DatePeriod(days = days - 1))
                    val window =
                        entries.filter { it.start.date >= windowFrom && it.start <= current.start }
                    if (window.isEmpty()) return@map current

                    val (value, confidence) = when (smoothing.method) {
                        RollingMethod.Median -> {
                            val v = median(window.map { it.value }) ?: return@map current
                            val c = median(window.map { it.confidence }) ?: return@map current
                            v to c
                        }

                        RollingMethod.Max -> {
                            val best = window.maxBy { it.value }
                            best.value to best.confidence
                        }
                    }

                    EstimatedOneRepMaxEntry(
                        label = current.label,
                        value = value,
                        confidence = confidence,
                        start = current.start,
                        sampleCount = window.sumOf { it.sampleCount },
                    )
                }
            }
        }
    }

    private fun median(values: List<Float>): Float? {
        val sorted = values.filter { it.isFinite() }.sorted()
        val n = sorted.size
        if (n == 0) return null
        val mid = n / 2
        return if (n % 2 == 1) {
            sorted[mid]
        } else {
            (sorted[mid - 1] + sorted[mid]) / 2f
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
        if (days <= 1) return BucketScale.Exercise
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
