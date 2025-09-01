package com.grippo.training.recording.calculation

import com.grippo.design.components.chart.DSPieData
import com.grippo.design.components.chart.DSPieSlice
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.state.trainings.ExerciseState

internal class ExampleAnalyticsCalculator(
    private val stringProvider: StringProvider,
    private val colorProvider: ColorProvider
) {
    suspend fun calculateCategoryDistribution(
        exercises: List<ExerciseState>,
    ): DSPieData {
        val colors = colorProvider.get()
        val categoryGroups = exercises
            .mapNotNull { it.exerciseExample?.category }
            .groupBy { it }
            .mapValues { (_, list) -> list.size }

        if (categoryGroups.isEmpty()) {
            return DSPieData(slices = emptyList())
        }

        val palette = colors.charts.categorical.palette
        val slices = categoryGroups.entries.mapIndexed { index, (category, count) ->
            DSPieSlice(
                id = category.name,
                label = category.title().text(stringProvider),
                value = count.toFloat(),
                color = palette[index % palette.size]
            )
        }

        return DSPieData(slices = slices)
    }

    suspend fun calculateWeightTypeDistribution(
        exercises: List<ExerciseState>,
    ): DSPieData {
        val colors = colorProvider.get()

        val categoryGroups = exercises
            .mapNotNull { it.exerciseExample?.weightType }
            .groupBy { it }
            .mapValues { (_, list) -> list.size }

        if (categoryGroups.isEmpty()) {
            return DSPieData(slices = emptyList())
        }

        val palette = colors.charts.categorical.palette
        val slices = categoryGroups.entries.mapIndexed { index, (category, count) ->
            DSPieSlice(
                id = category.name,
                label = category.title().text(stringProvider),
                value = count.toFloat(),
                color = palette[index % palette.size]
            )
        }

        return DSPieData(slices = slices)
    }

    suspend fun calculateForceTypeDistribution(
        exercises: List<ExerciseState>,
    ): DSPieData {
        val colors = colorProvider.get()

        val categoryGroups = exercises
            .mapNotNull { it.exerciseExample?.forceType }
            .groupBy { it }
            .mapValues { (_, list) -> list.size }

        if (categoryGroups.isEmpty()) {
            return DSPieData(slices = emptyList())
        }

        val palette = colors.charts.categorical.palette
        val slices = categoryGroups.entries.mapIndexed { index, (category, count) ->
            DSPieSlice(
                id = category.name,
                label = category.title().text(stringProvider),
                value = count.toFloat(),
                color = palette[index % palette.size]
            )
        }

        return DSPieData(slices = slices)
    }

    suspend fun calculateExperienceDistribution(
        exercises: List<ExerciseState>,
    ): DSPieData {
        val colors = colorProvider.get()

        val categoryGroups = exercises
            .mapNotNull { it.exerciseExample?.experience }
            .groupBy { it }
            .mapValues { (_, list) -> list.size }

        if (categoryGroups.isEmpty()) {
            return DSPieData(slices = emptyList())
        }

        val palette = colors.charts.categorical.palette
        val slices = categoryGroups.entries.mapIndexed { index, (category, count) ->
            DSPieSlice(
                id = category.name,
                label = category.title().text(stringProvider),
                value = count.toFloat(),
                color = palette[index % palette.size]
            )
        }

        return DSPieData(slices = slices)
    }
}