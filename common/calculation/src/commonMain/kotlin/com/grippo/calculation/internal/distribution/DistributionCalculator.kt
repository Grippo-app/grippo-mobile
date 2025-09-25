package com.grippo.calculation.internal.distribution

import androidx.compose.ui.graphics.Color
import com.grippo.calculation.models.DistributionBreakdown
import com.grippo.calculation.models.DistributionSlice
import com.grippo.calculation.models.DistributionWeighting
import com.grippo.date.utils.contains
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.state.datetime.PeriodState
import com.grippo.state.exercise.examples.CategoryEnumState
import com.grippo.state.exercise.examples.ForceTypeEnumState
import com.grippo.state.exercise.examples.WeightTypeEnumState
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.TrainingState

internal class DistributionCalculator(
    private val stringProvider: StringProvider,
    private val colorProvider: ColorProvider,
) {

    private companion object {
        private const val EPS: Float = 1e-3f
    }

    suspend fun calculateCategoryDistributionFromExercises(
        exercises: List<ExerciseState>,
        weighting: DistributionWeighting = DistributionWeighting.Count,
    ): DistributionBreakdown {
        val ordered = enumValues<CategoryEnumState>()
        val totals = aggregateTotals(exercises, weighting) { it.exerciseExample.category }
        val palette = colorProvider.get().example.category
        val labels = HashMap<CategoryEnumState, String>(ordered.size)
        for (value in ordered) {
            labels[value] = value.title().text(stringProvider)
        }

        return buildPie(
            ordered = ordered,
            totals = totals,
            colorProvider = { category: CategoryEnumState ->
                when (category) {
                    CategoryEnumState.COMPOUND -> palette.compound
                    CategoryEnumState.ISOLATION -> palette.isolation
                }
            },
            labelProvider = { category -> labels[category] ?: category.name },
        )
    }

    suspend fun calculateCategoryDistributionFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState,
        weighting: DistributionWeighting = DistributionWeighting.Count,
    ): DistributionBreakdown {
        val inRange = trainings.filter { it.createdAt in period.range }
        return calculateCategoryDistributionFromExercises(
            inRange.flatMap { it.exercises },
            weighting
        )
    }

    suspend fun calculateWeightTypeDistributionFromExercises(
        exercises: List<ExerciseState>,
        weighting: DistributionWeighting = DistributionWeighting.Count,
    ): DistributionBreakdown {
        val ordered = enumValues<WeightTypeEnumState>()
        val totals = aggregateTotals(exercises, weighting) { it.exerciseExample.weightType }
        val palette = colorProvider.get().example.weightType
        val labels = HashMap<WeightTypeEnumState, String>(ordered.size)
        for (value in ordered) {
            labels[value] = value.title().text(stringProvider)
        }

        return buildPie(
            ordered = ordered,
            totals = totals,
            colorProvider = { type: WeightTypeEnumState ->
                when (type) {
                    WeightTypeEnumState.FREE -> palette.free
                    WeightTypeEnumState.FIXED -> palette.fixed
                    WeightTypeEnumState.BODY_WEIGHT -> palette.bodyWeight
                }
            },
            labelProvider = { type -> labels[type] ?: type.name },
        )
    }

    suspend fun calculateWeightTypeDistributionFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState,
        weighting: DistributionWeighting = DistributionWeighting.Count,
    ): DistributionBreakdown {
        val inRange = trainings.filter { it.createdAt in period.range }
        return calculateWeightTypeDistributionFromExercises(
            inRange.flatMap { it.exercises },
            weighting
        )
    }

    suspend fun calculateForceTypeDistributionFromExercises(
        exercises: List<ExerciseState>,
        weighting: DistributionWeighting = DistributionWeighting.Count,
    ): DistributionBreakdown {
        val ordered = enumValues<ForceTypeEnumState>()
        val totals = aggregateTotals(exercises, weighting) { it.exerciseExample.forceType }
        val palette = colorProvider.get().example.forceType
        val labels = HashMap<ForceTypeEnumState, String>(ordered.size)
        for (value in ordered) {
            labels[value] = value.title().text(stringProvider)
        }

        return buildPie(
            ordered = ordered,
            totals = totals,
            colorProvider = { type: ForceTypeEnumState ->
                when (type) {
                    ForceTypeEnumState.PULL -> palette.pull
                    ForceTypeEnumState.PUSH -> palette.push
                    ForceTypeEnumState.HINGE -> palette.hinge
                }
            },
            labelProvider = { type -> labels[type] ?: type.name },
        )
    }

    suspend fun calculateForceTypeDistributionFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState,
        weighting: DistributionWeighting = DistributionWeighting.Count,
    ): DistributionBreakdown {
        val inRange = trainings.filter { it.createdAt in period.range }
        return calculateForceTypeDistributionFromExercises(
            inRange.flatMap { it.exercises },
            weighting
        )
    }

    private fun <E : Enum<E>> buildPie(
        ordered: Array<E>,
        totals: Map<E, Float>,
        colorProvider: (E) -> Color,
        labelProvider: (E) -> String,
    ): DistributionBreakdown {
        val slices = ordered.mapNotNull { key ->
            val value = totals[key]?.takeIf { it > 0f } ?: return@mapNotNull null
            DistributionSlice(
                id = key.name,
                label = labelProvider(key),
                value = value,
                color = colorProvider(key),
            )
        }
        return DistributionBreakdown(slices)
    }

    private fun <E : Enum<E>> aggregateTotals(
        exercises: List<ExerciseState>,
        weighting: DistributionWeighting,
        keySelector: (ExerciseState) -> E?,
    ): Map<E, Float> {
        val totals = HashMap<E, Float>(8)
        exercises.forEach { exercise ->
            val key = keySelector(exercise) ?: return@forEach
            val contribution = weightOfExercise(exercise, weighting)
            if (contribution <= 0f) return@forEach
            totals[key] = (totals[key] ?: 0f) + contribution
        }
        return totals
    }

    private fun weightOfExercise(exercise: ExerciseState, weighting: DistributionWeighting): Float =
        when (weighting) {
            DistributionWeighting.Count -> 1f
            DistributionWeighting.Sets -> exercise.iterations.count {
                (it.repetitions.value ?: 0) > 0
            }.toFloat()

            DistributionWeighting.Reps -> exercise.iterations.fold(0) { acc, iteration ->
                val reps = iteration.repetitions.value ?: 0
                if (reps > 0) acc + reps else acc
            }.toFloat()

            DistributionWeighting.Volume -> exercise.iterations.fold(0.0) { acc, iteration ->
                val reps = iteration.repetitions.value ?: 0
                if (reps <= 0) return@fold acc
                val wRaw = iteration.volume.value ?: 0f
                val weight = if (wRaw > EPS) wRaw else 0f
                if (weight == 0f) acc else acc + weight.toDouble() * reps.toDouble()
            }.toFloat()
        }.coerceAtLeast(0f)
}
