package com.grippo.training.recording.calculation

import com.grippo.design.components.chart.DSPieData
import com.grippo.design.components.chart.DSPieSlice
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.state.exercise.examples.CategoryEnumState
import com.grippo.state.exercise.examples.ForceTypeEnumState
import com.grippo.state.exercise.examples.WeightTypeEnumState
import com.grippo.state.profile.ExperienceEnumState
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

        val slices = categoryGroups.entries.mapIndexed { index, (category, count) ->
            DSPieSlice(
                id = category.name,
                label = category.title().text(stringProvider),
                value = count.toFloat(),
                color = when (category) {
                    CategoryEnumState.COMPOUND -> colors.example.category.compound
                    CategoryEnumState.ISOLATION -> colors.example.category.isolation
                }

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

        val slices = categoryGroups.entries.mapIndexed { index, (category, count) ->
            DSPieSlice(
                id = category.name,
                label = category.title().text(stringProvider),
                value = count.toFloat(),
                color = when (category) {
                    WeightTypeEnumState.FREE -> colors.example.weightType.free
                    WeightTypeEnumState.FIXED -> colors.example.weightType.fixed
                    WeightTypeEnumState.BODY_WEIGHT -> colors.example.weightType.bodyWeight
                }
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

        val slices = categoryGroups.entries.mapIndexed { index, (category, count) ->
            DSPieSlice(
                id = category.name,
                label = category.title().text(stringProvider),
                value = count.toFloat(),
                color = when (category) {
                    ForceTypeEnumState.PULL -> colors.example.forceType.pull
                    ForceTypeEnumState.PUSH -> colors.example.forceType.push
                    ForceTypeEnumState.HINGE -> colors.example.forceType.hinge
                }
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

        val slices = categoryGroups.entries.mapIndexed { index, (category, count) ->
            DSPieSlice(
                id = category.name,
                label = category.title().text(stringProvider),
                value = count.toFloat(),
                color = when (category) {
                    ExperienceEnumState.BEGINNER -> colors.profile.experienceColors.beginner
                    ExperienceEnumState.INTERMEDIATE -> colors.profile.experienceColors.intermediate
                    ExperienceEnumState.ADVANCED -> colors.profile.experienceColors.advanced
                    ExperienceEnumState.PRO -> colors.profile.experienceColors.pro
                }
            )
        }

        return DSPieData(slices = slices)
    }
}