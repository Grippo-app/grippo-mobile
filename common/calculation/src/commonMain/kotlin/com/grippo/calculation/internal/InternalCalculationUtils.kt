package com.grippo.calculation.internal

import com.grippo.state.exercise.examples.ExerciseExampleState
import com.grippo.state.muscles.MuscleEnumState
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.IterationState
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

        val totalPercentage =
            example.bundles.sumOf { (it.percentage.value ?: 0).toDouble() }.toFloat()
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
}
