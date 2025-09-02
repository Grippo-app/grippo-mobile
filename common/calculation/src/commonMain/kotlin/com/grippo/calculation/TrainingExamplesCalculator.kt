package com.grippo.calculation

import com.grippo.design.components.chart.DSPieData
import com.grippo.design.components.chart.DSPieSlice
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.state.exercise.examples.CategoryEnumState
import com.grippo.state.exercise.examples.ForceTypeEnumState
import com.grippo.state.exercise.examples.WeightTypeEnumState
import com.grippo.state.profile.ExperienceEnumState
import com.grippo.state.trainings.ExerciseState

public class TrainingExamplesCalculator(
    private val stringProvider: StringProvider,
    private val colorProvider: ColorProvider
) {

    // -----------------------------------------
    // Weighting strategy for distributions
    // -----------------------------------------
    public sealed interface Weighting {
        /** Each exercise contributes 1. Good for "catalog mix", NOT for workload. */
        public data object Count : Weighting

        /** Sum of sets per exercise (iterations count). */
        public data object Sets : Weighting

        /** Sum of repetitions per exercise across all sets. */
        public data object Reps : Weighting

        /** Sum of volume per exercise across all sets (expects iteration.volume.value as weight*reps or similar). */
        public data object Volume : Weighting
    }

    // -----------------------------------------
    // Internal helpers (kept inside the class)
    // -----------------------------------------

    private fun weightOfExercise(ex: ExerciseState, w: Weighting): Float {
        return when (w) {
            Weighting.Count -> 1f
            Weighting.Sets -> ex.iterations.size.toFloat()
            Weighting.Reps -> ex.iterations.sumOf { it.repetitions.value ?: 0 }.toFloat()
            Weighting.Volume -> ex.iterations
                .sumOf { (it.volume.value ?: 0f).toDouble() }
                .toFloat()
        }.coerceAtLeast(0f)
    }

    private inline fun <reified E : Enum<E>> stableOrder(): List<E> {
        // Keep a deterministic order based on the enum declaration order.
        return enumValues<E>().toList()
    }

    // -----------------------------------------
    // 🍰 Category Distribution (Compound vs Isolation)
    // -----------------------------------------

    /**
     * 🏗 Category Distribution — Compound vs Isolation
     *
     * WHAT:
     * Counts or weights exercises by their category (Compound / Isolation).
     *
     * WHY:
     * Shows structural balance between multi-joint and single-joint work.
     * Useful for high-level program audit; not a fatigue proxy by itself.
     *
     * HOW:
     * Aggregate by selected [weighting]:
     *  - Count: each exercise = 1
     *  - Sets/Reps/Volume/TUT: workload-aware
     *
     * PITFALLS:
     * Count-only will overstate isolation if it has tiny sets and understate a heavy compound with many sets.
     */
    public suspend fun calculateCategoryDistribution(
        exercises: List<ExerciseState>,
        weighting: Weighting = Weighting.Count
    ): DSPieData {
        val colors = colorProvider.get()
        val ordered = stableOrder<CategoryEnumState>()

        // Aggregate by chosen weighting
        val totals: Map<CategoryEnumState, Float> = exercises
            .mapNotNull { ex ->
                ex.exerciseExample?.category?.let {
                    it to weightOfExercise(
                        ex,
                        weighting
                    )
                }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, xs) -> xs.sum() }

        if (totals.isEmpty()) return DSPieData(slices = emptyList())

        // Build deterministic slices in enum order, skipping zeroes
        val slices = ordered.mapNotNull { category ->
            val value = totals[category]?.takeIf { it > 0f } ?: return@mapNotNull null
            DSPieSlice(
                id = category.name,
                label = category.title().text(stringProvider),
                value = value,
                color = when (category) {
                    CategoryEnumState.COMPOUND -> colors.example.category.compound
                    CategoryEnumState.ISOLATION -> colors.example.category.isolation
                }
            )
        }

        return DSPieData(slices = slices)
    }

    // -----------------------------------------
    // ⚖️ Weight Type Distribution (Free / Fixed / Bodyweight)
    // -----------------------------------------

    /**
     * ⚖️ Weight Type Distribution — Free vs Fixed vs Bodyweight
     *
     * WHAT:
     * Aggregates workload by equipment type.
     *
     * WHY:
     * Quick sanity check for equipment bias (e.g., too much machines vs free weights).
     *
     * HOW:
     * Uses [weighting] to turn "catalog mix" into workload-aware chart if needed.
     *
     * PITFALLS:
     * As a pie chart, many small slices are hard to compare — prefer stacked bars for multi-session views.
     */
    public suspend fun calculateWeightTypeDistribution(
        exercises: List<ExerciseState>,
        weighting: Weighting = Weighting.Count
    ): DSPieData {
        val colors = colorProvider.get()
        val ordered = stableOrder<WeightTypeEnumState>()

        val totals: Map<WeightTypeEnumState, Float> = exercises
            .mapNotNull { ex ->
                ex.exerciseExample?.weightType?.let {
                    it to weightOfExercise(
                        ex,
                        weighting
                    )
                }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, xs) -> xs.sum() }

        if (totals.isEmpty()) return DSPieData(slices = emptyList())

        val slices = ordered.mapNotNull { type ->
            val value = totals[type]?.takeIf { it > 0f } ?: return@mapNotNull null
            DSPieSlice(
                id = type.name,
                label = type.title().text(stringProvider),
                value = value,
                color = when (type) {
                    WeightTypeEnumState.FREE -> colors.example.weightType.free
                    WeightTypeEnumState.FIXED -> colors.example.weightType.fixed
                    WeightTypeEnumState.BODY_WEIGHT -> colors.example.weightType.bodyWeight
                }
            )
        }

        return DSPieData(slices = slices)
    }

    // -----------------------------------------
    // 🔄 Force Type Distribution (Push / Pull / Hinge)
    // -----------------------------------------

    /**
     * 🔄 Force Type Distribution — Push / Pull / Hinge
     *
     * WHAT:
     * Aggregates workload by movement force pattern.
     *
     * WHY:
     * Helps monitor push/pull/hinge balance over a session or mesocycle.
     *
     * HOW:
     * Same [weighting] logic as above.
     *
     * PITFALLS:
     * Misclassification will break this chart — rely on canonical exercise metadata,
     * avoid name-based inference whenever possible.
     */
    public suspend fun calculateForceTypeDistribution(
        exercises: List<ExerciseState>,
        weighting: Weighting = Weighting.Count
    ): DSPieData {
        val colors = colorProvider.get()
        val ordered = stableOrder<ForceTypeEnumState>()

        val totals: Map<ForceTypeEnumState, Float> = exercises
            .mapNotNull { ex ->
                ex.exerciseExample?.forceType?.let {
                    it to weightOfExercise(
                        ex,
                        weighting
                    )
                }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, xs) -> xs.sum() }

        if (totals.isEmpty()) return DSPieData(slices = emptyList())

        val slices = ordered.mapNotNull { type ->
            val value = totals[type]?.takeIf { it > 0f } ?: return@mapNotNull null
            DSPieSlice(
                id = type.name,
                label = type.title().text(stringProvider),
                value = value,
                color = when (type) {
                    ForceTypeEnumState.PULL -> colors.example.forceType.pull
                    ForceTypeEnumState.PUSH -> colors.example.forceType.push
                    ForceTypeEnumState.HINGE -> colors.example.forceType.hinge
                }
            )
        }

        return DSPieData(slices = slices)
    }

    // -----------------------------------------
    // 🎯 Experience Level Distribution (Beginner / Intermediate / Advanced / Pro)
    // -----------------------------------------

    /**
     * 🎯 Experience Level Distribution — Beginner / Intermediate / Advanced / Pro
     *
     * WHAT:
     * Mix of difficulty tags across selected exercises, optionally workload-weighted.
     *
     * WHY:
     * Surfaces bias toward overly advanced or overly easy selections.
     *
     * HOW:
     * Choose [weighting] to make it reflect real work done (Sets/Reps/Volume/TUT) instead of raw counts.
     *
     * PITFALLS:
     * Labels are coarse and may be inconsistent across data sources. Treat as advisory, not prescriptive.
     */
    public suspend fun calculateExperienceDistribution(
        exercises: List<ExerciseState>,
        weighting: Weighting = Weighting.Count
    ): DSPieData {
        val colors = colorProvider.get()
        val ordered = stableOrder<ExperienceEnumState>()

        val totals: Map<ExperienceEnumState, Float> = exercises
            .mapNotNull { ex ->
                ex.exerciseExample?.experience?.let {
                    it to weightOfExercise(
                        ex,
                        weighting
                    )
                }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, xs) -> xs.sum() }

        if (totals.isEmpty()) return DSPieData(slices = emptyList())

        val slices = ordered.mapNotNull { level ->
            val value = totals[level]?.takeIf { it > 0f } ?: return@mapNotNull null
            DSPieSlice(
                id = level.name,
                label = level.title().text(stringProvider),
                value = value,
                color = when (level) {
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