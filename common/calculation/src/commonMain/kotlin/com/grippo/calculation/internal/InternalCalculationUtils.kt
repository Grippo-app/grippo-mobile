package com.grippo.calculation.internal

import com.grippo.calculation.models.Bucket
import com.grippo.calculation.models.BucketScale
import com.grippo.calculation.models.minDT
import com.grippo.calculation.models.maxDT
import com.grippo.date.utils.DateRange
import com.grippo.state.exercise.examples.ExerciseExampleState
import com.grippo.state.muscles.MuscleEnumState
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.IterationState
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.math.max

/**
 * Internal calculation utilities shared across calculators.
 * Contains common patterns and reusable logic.
 */
internal object InternalCalculationUtils {

    // ============ Constants ============
    internal const val WEIGHT_EPS_KG: Float = 0.5f

    // ============ Weight & Reps Utilities ============

    /**
     * Represents calculated metrics for an iteration.
     */
    internal data class IterationMetrics(
        val weight: Float,
        val reps: Int,
        val tonnage: Float,
        val isValid: Boolean
    )

    /**
     * Extracts weight, reps, and tonnage from an iteration.
     * Handles null values and validation.
     */
    internal fun extractIterationMetrics(iteration: IterationState): IterationMetrics {
        val weight = iteration.volume.value ?: 0f
        val reps = iteration.repetitions.value ?: 0
        val tonnage = if (weight > 0f && reps > 0) weight * reps else 0f
        val isValid = weight > 0f && reps > 0
        return IterationMetrics(weight, reps, tonnage, isValid)
    }

    /**
     * Sums tonnage and reps across multiple iterations.
     */
    internal fun sumIterationsMetrics(iterations: List<IterationState>): Pair<Float, Int> {
        var totalTonnage = 0f
        var totalReps = 0
        iterations.forEach { iteration ->
            val metrics = extractIterationMetrics(iteration)
            if (metrics.isValid) {
                totalTonnage += metrics.tonnage
                totalReps += metrics.reps
            }
        }
        return totalTonnage to totalReps
    }

    /**
     * Calculates average weight per rep across iterations.
     */
    internal fun avgWeightPerRep(iterations: List<IterationState>): Float {
        val (sumT, sumR) = sumIterationsMetrics(iterations)
        return if (sumR > 0) sumT / sumR else 0f
    }

    // ============ Workload Selection ============

    /**
     * Workload calculation strategy.
     */
    internal sealed interface WorkloadStrategy {
        data object Volume : WorkloadStrategy
        data object Reps : WorkloadStrategy
    }

    /**
     * Automatically chooses workload strategy based on exercise data.
     * If any meaningful weight is entered, uses Volume; otherwise Reps.
     */
    internal fun chooseWorkloadStrategy(exercises: List<ExerciseState>): WorkloadStrategy {
        if (exercises.isEmpty()) return WorkloadStrategy.Reps

        var hasMeaningfulWeight = false
        exercises.forEach { exercise ->
            if (exercise.iterations.isEmpty()) return@forEach

            exercise.iterations.forEach { iteration ->
                val weight = iteration.volume.value ?: 0f
                val reps = iteration.repetitions.value ?: 0
                if (weight > WEIGHT_EPS_KG && reps > 0) {
                    hasMeaningfulWeight = true
                    return@forEach
                }
            }
            if (hasMeaningfulWeight) return@forEach
        }
        return if (hasMeaningfulWeight) WorkloadStrategy.Volume else WorkloadStrategy.Reps
    }

    /**
     * Calculates workload for an exercise using the given strategy.
     */
    internal fun calculateExerciseWorkload(
        exercise: ExerciseState,
        strategy: WorkloadStrategy
    ): Float {
        if (exercise.iterations.isEmpty()) return 0f

        val result = when (strategy) {
            WorkloadStrategy.Volume -> exercise.iterations.fold(0f) { acc, iteration ->
                val weight = iteration.volume.value ?: 0f
                val reps = iteration.repetitions.value ?: 0
                val load = if (weight > WEIGHT_EPS_KG) weight else 0f
                if (reps == 0 || load <= 0f) acc else acc + load * reps
            }

            WorkloadStrategy.Reps -> exercise.iterations.fold(0) { acc, iteration ->
                acc + max(0, iteration.repetitions.value ?: 0)
            }.toFloat()
        }

        return result.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    }

    // ============ Muscle Distribution ============

    /**
     * Represents muscle workload distribution.
     */
    internal data class MuscleWorkload(
        val muscle: MuscleEnumState,
        val load: Float
    )

    /**
     * Distributes exercise workload across muscles using example bundles.
     */
    internal fun distributeWorkloadToMuscles(
        exercise: ExerciseState,
        example: ExerciseExampleState,
        workload: Float
    ): List<MuscleWorkload> {
        if (workload <= 0f || !workload.isFinite()) return emptyList()
        if (example.bundles.isEmpty()) return emptyList()

        val totalPercentage = example.bundles.sumOf { (it.percentage.value ?: 0).toDouble() }.toFloat()
        if (totalPercentage <= 0f) return emptyList()

        return example.bundles.mapNotNull { bundle ->
            val percentage = bundle.percentage.value ?: 0
            if (percentage <= 0) return@mapNotNull null

            val share = percentage / totalPercentage
            val muscleLoad = workload * share
            if (!muscleLoad.isFinite() || muscleLoad <= 0f) return@mapNotNull null

            MuscleWorkload(bundle.muscle.type, muscleLoad)
        }
    }

    /**
     * Aggregates muscle workloads into a map.
     */
    internal fun aggregateMuscleWorkloads(workloads: List<MuscleWorkload>): Map<MuscleEnumState, Float> {
        val result = mutableMapOf<MuscleEnumState, Float>()
        workloads.forEach { workload ->
            result[workload.muscle] = (result[workload.muscle] ?: 0f) + workload.load
        }
        return result
    }

    // ============ Time Bucketing ============

    /**
     * Builds day buckets for the given range.
     */
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

    /**
     * Gets the start of week (Monday 00:00) for the given date.
     */
    internal fun startOfWeek(dateTime: LocalDateTime): LocalDateTime {
        val shift = dateTime.date.dayOfWeek.isoDayNumber - 1
        val monday = dateTime.date.minus(DatePeriod(days = shift))
        return LocalDateTime(monday, LocalTime(0, 0))
    }

    /**
     * Gets the start of month (1st day 00:00) for the given date.
     */
    internal fun startOfMonth(dateTime: LocalDateTime): LocalDateTime {
        return LocalDateTime(LocalDate(dateTime.year, dateTime.monthNumber, 1), LocalTime(0, 0))
    }

    // ============ Validation & Normalization ============

    /**
     * Normalizes a value to [0, 1] range.
     */
    internal fun normalizeToUnit(value: Float, maxValue: Float): Float {
        return if (maxValue <= 0f) 0f else (value / maxValue).coerceIn(0f, 1f)
    }

    /**
     * Safely calculates percentile from a list of floats.
     */
    internal fun calculatePercentile(values: List<Float>, percentile: Float): Float {
        if (values.isEmpty()) return 0f
        val sorted = values.sorted()
        val clampedPercentile = percentile.coerceIn(0f, 1f)
        val index = ((sorted.size - 1) * clampedPercentile).toDouble()
        val lowerIndex = index.toInt()
        val upperIndex = minOf(lowerIndex + 1, sorted.lastIndex)
        val fraction = (index - lowerIndex).toFloat()
        return sorted[lowerIndex] * (1 - fraction) + sorted[upperIndex] * fraction
    }

    /**
     * Checks if a training belongs to a bucket (inclusive range).
     */
    internal fun LocalDateTime.belongsTo(bucket: Bucket): Boolean {
        return this >= bucket.start && this <= bucket.end
    }
}
