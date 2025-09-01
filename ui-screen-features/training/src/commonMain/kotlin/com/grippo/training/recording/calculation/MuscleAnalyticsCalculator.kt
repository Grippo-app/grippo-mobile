package com.grippo.training.recording.calculation

import com.grippo.design.components.chart.DSPieData
import com.grippo.design.components.chart.DSPieSlice
import com.grippo.design.components.chart.DSProgressData
import com.grippo.design.components.chart.DSProgressItem
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.state.exercise.examples.ExerciseExampleState
import com.grippo.state.exercise.examples.ForceTypeEnumState
import com.grippo.state.muscles.MuscleEnumState
import com.grippo.state.trainings.ExerciseState

internal class MuscleAnalyticsCalculator(
    private val stringProvider: StringProvider,
    private val colorProvider: ColorProvider,
) {
    private companion object {
        private const val MAX_VISIBLE_MUSCLES = 8
    }

    /**
     * Distribution of load per muscle (top 8).
     *
     * Load = exercise volume × bundle.percentage
     */
    suspend fun calculateMuscleLoadDistribution(
        exercises: List<ExerciseState>,
        examples: List<ExerciseExampleState>
    ): DSProgressData {
        val colors = colorProvider.get()
        val muscleLoads = mutableMapOf<MuscleEnumState, Float>()

        val exampleMap = examples.associateBy { it.value.id }

        exercises.forEach { exercise ->
            val exerciseVolume = exercise.iterations
                .sumOf { it.volume.value?.toDouble() ?: 0.0 }
                .toFloat()

            val exampleId = exercise.exerciseExample?.id ?: return@forEach
            val example = exampleMap[exampleId] ?: return@forEach

            example.bundles.forEach { bundle ->
                val percentage = bundle.percentage.value ?: 0
                if (percentage <= 0) return@forEach
                val load = exerciseVolume * (percentage / 100f)
                muscleLoads[bundle.muscle.type] =
                    (muscleLoads[bundle.muscle.type] ?: 0f) + load
            }
        }

        if (muscleLoads.isEmpty()) return DSProgressData(items = emptyList())

        val palette = colors.charts.progress.palette
        val maxLoad = muscleLoads.values.maxOrNull() ?: 1f

        val items = muscleLoads.entries
            .sortedByDescending { it.value }
            .take(MAX_VISIBLE_MUSCLES)
            .mapIndexed { index, (muscle, load) ->
                val percentage = (load / maxLoad) * 100f
                DSProgressItem(
                    label = muscle.title().text(stringProvider),
                    value = percentage,
                    color = palette[index % palette.size]
                )
            }

        return DSProgressData(
            items = items,
            valueUnit = "kg",
            title = "Muscle Load Distribution"
        )
    }

    /**
     * Push / Pull / Other balance based on ForceType.
     */
    suspend fun calculatePushPullBalance(
        exercises: List<ExerciseState>
    ): DSPieData {
        val colors = colorProvider.get()
        var pushVolume = 0f
        var pullVolume = 0f
        var otherVolume = 0f

        exercises.forEach { exercise ->
            val exerciseVolume = exercise.iterations
                .sumOf { it.volume.value?.toDouble() ?: 0.0 }
                .toFloat()

            when (exercise.exerciseExample?.forceType) {
                ForceTypeEnumState.PUSH -> pushVolume += exerciseVolume
                ForceTypeEnumState.PULL -> pullVolume += exerciseVolume
                else -> otherVolume += exerciseVolume
            }
        }

        val palette = colors.charts.categorical.palette
        val slices = listOfNotNull(
            if (pushVolume > 0) DSPieSlice("push", "Push", pushVolume, palette[0]) else null,
            if (pullVolume > 0) DSPieSlice("pull", "Pull", pullVolume, palette[1]) else null,
            if (otherVolume > 0) DSPieSlice("other", "Other", otherVolume, palette[2]) else null
        )

        return DSPieData(slices = slices)
    }
}