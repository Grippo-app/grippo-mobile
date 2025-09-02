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

/**
 * Calculator for distributions of exercises by categorical attributes.
 *
 * 📌 Responsibilities:
 * - Aggregates `ExerciseState` metadata (category, weight type, force type, experience).
 * - Produces [DSPieData] ready for pie chart visualization.
 * - Helps visualize training structure and balance between different types of exercises.
 *
 * @param stringProvider used to localize enum titles into human-readable strings
 * @param colorProvider used to resolve consistent colors for each slice
 */
internal class ExampleAnalyticsCalculator(
    private val stringProvider: StringProvider,
    private val colorProvider: ColorProvider
) {

    /**
     * 🏗 Category Distribution
     *
     * - **Definition**: counts how many exercises are Compound vs Isolation.
     * - **Use case**: shows balance between multi-joint and single-joint work.
     *
     * Example:
     * - Bench Press → Compound
     * - Biceps Curl → Isolation
     * 👉 Pie chart: 70% Compound, 30% Isolation
     */
    suspend fun calculateCategoryDistribution(exercises: List<ExerciseState>): DSPieData {
        val colors = colorProvider.get()
        val categoryGroups = exercises
            .mapNotNull { it.exerciseExample?.category }
            .groupBy { it }
            .mapValues { (_, list) -> list.size }

        if (categoryGroups.isEmpty()) return DSPieData(slices = emptyList())

        val slices = categoryGroups.entries.map { (category, count) ->
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

    /**
     * ⚖️ Weight Type Distribution
     *
     * - **Definition**: counts how many exercises are Free weights, Fixed machines, or Bodyweight.
     * - **Use case**: shows equipment balance in the workout.
     *
     * Example:
     * - Bench Press → Free
     * - Leg Press → Fixed
     * - Push-Ups → Bodyweight
     * 👉 Pie chart: 50% Free, 30% Fixed, 20% Bodyweight
     */
    suspend fun calculateWeightTypeDistribution(exercises: List<ExerciseState>): DSPieData {
        val colors = colorProvider.get()
        val categoryGroups = exercises
            .mapNotNull { it.exerciseExample?.weightType }
            .groupBy { it }
            .mapValues { (_, list) -> list.size }

        if (categoryGroups.isEmpty()) return DSPieData(slices = emptyList())

        val slices = categoryGroups.entries.map { (category, count) ->
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

    /**
     * 🔄 Force Type Distribution
     *
     * - **Definition**: counts how many exercises are Push, Pull, or Hinge.
     * - **Use case**: helps check balance of movement patterns in the program.
     *
     * Example:
     * - Bench Press → Push
     * - Pull-Ups → Pull
     * - Deadlift → Hinge
     * 👉 Pie chart: 40% Push, 40% Pull, 20% Hinge
     */
    suspend fun calculateForceTypeDistribution(exercises: List<ExerciseState>): DSPieData {
        val colors = colorProvider.get()
        val categoryGroups = exercises
            .mapNotNull { it.exerciseExample?.forceType }
            .groupBy { it }
            .mapValues { (_, list) -> list.size }

        if (categoryGroups.isEmpty()) return DSPieData(slices = emptyList())

        val slices = categoryGroups.entries.map { (category, count) ->
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

    /**
     * 🎯 Experience Level Distribution
     *
     * - **Definition**: counts how many exercises are tagged as Beginner, Intermediate, Advanced, or Pro.
     * - **Use case**: shows difficulty bias of the workout plan.
     *
     * Example:
     * - Push-Ups → Beginner
     * - Bench Press → Intermediate
     * - Muscle-Up → Advanced
     * 👉 Pie chart: 1 Beginner, 1 Intermediate, 1 Advanced
     */
    suspend fun calculateExperienceDistribution(exercises: List<ExerciseState>): DSPieData {
        val colors = colorProvider.get()
        val categoryGroups = exercises
            .mapNotNull { it.exerciseExample?.experience }
            .groupBy { it }
            .mapValues { (_, list) -> list.size }

        if (categoryGroups.isEmpty()) return DSPieData(slices = emptyList())

        val slices = categoryGroups.entries.map { (category, count) ->
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