package com.grippo.calculation

import com.grippo.calculation.models.Instruction
import com.grippo.design.components.chart.DSPieData
import com.grippo.design.components.chart.DSPieSlice
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.tooltip_category_description_training
import com.grippo.design.resources.provider.tooltip_category_description_trainings
import com.grippo.design.resources.provider.tooltip_category_title_training
import com.grippo.design.resources.provider.tooltip_category_title_trainings
import com.grippo.design.resources.provider.tooltip_experience_description_training
import com.grippo.design.resources.provider.tooltip_experience_description_trainings
import com.grippo.design.resources.provider.tooltip_experience_title_training
import com.grippo.design.resources.provider.tooltip_experience_title_trainings
import com.grippo.design.resources.provider.tooltip_force_type_description_training
import com.grippo.design.resources.provider.tooltip_force_type_description_trainings
import com.grippo.design.resources.provider.tooltip_force_type_title_training
import com.grippo.design.resources.provider.tooltip_force_type_title_trainings
import com.grippo.design.resources.provider.tooltip_weight_type_description_training
import com.grippo.design.resources.provider.tooltip_weight_type_description_trainings
import com.grippo.design.resources.provider.tooltip_weight_type_title_training
import com.grippo.design.resources.provider.tooltip_weight_type_title_trainings
import com.grippo.state.exercise.examples.CategoryEnumState
import com.grippo.state.exercise.examples.ForceTypeEnumState
import com.grippo.state.exercise.examples.WeightTypeEnumState
import com.grippo.state.formatters.UiText
import com.grippo.state.profile.ExperienceEnumState
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.TrainingState

/**
 * 📊 **Training Distribution Calculator**
 *
 * Calculates workload distributions across exercises or trainings,
 * using different weighting strategies. Each public API returns:
 *   Pair<DSPieData, Instruction>
 * where Instruction holds a localized tooltip (title + description).
 *
 * ---
 * 🔎 **Supported distributions**
 * - 🏗 **Category** → Compound vs Isolation
 * - ⚖️ **Weight Type** → Free vs Fixed vs Bodyweight
 * - 🔄 **Force Type** → Push vs Pull vs Hinge
 * - 🎯 **Experience Level** → Beginner / Intermediate / Advanced / Pro
 *
 * ---
 * ⚖️ **Weighting strategies**
 * - **Count** → each exercise = 1
 * - **Sets** → number of sets (iterations count)
 * - **Reps** → total repetitions
 * - **Volume** → Σ(weight × reps)
 *
 * ---
 * 🛠 **Usage**
 * - Pass a list of `ExerciseState` for single-session analysis
 * - Pass a list of `TrainingState` for multi-session analysis
 */
public class DistributionCalculator(
    private val stringProvider: StringProvider,
    private val colorProvider: ColorProvider
) {

    // ---------------- Tooltip plumbing ----------------

    private enum class Scope { TRAINING, TRAININGS }
    private enum class Metric { CATEGORY, WEIGHT_TYPE, FORCE_TYPE, EXPERIENCE }

    private fun instructionFor(metric: Metric, scope: Scope): Instruction {
        return when (metric) {
            Metric.CATEGORY -> when (scope) {
                Scope.TRAINING -> Instruction(
                    title = UiText.Res(Res.string.tooltip_category_title_training),
                    description = UiText.Res(Res.string.tooltip_category_description_training)
                )

                Scope.TRAININGS -> Instruction(
                    title = UiText.Res(Res.string.tooltip_category_title_trainings),
                    description = UiText.Res(Res.string.tooltip_category_description_trainings)
                )
            }

            Metric.WEIGHT_TYPE -> when (scope) {
                Scope.TRAINING -> Instruction(
                    title = UiText.Res(Res.string.tooltip_weight_type_title_training),
                    description = UiText.Res(Res.string.tooltip_weight_type_description_training)
                )

                Scope.TRAININGS -> Instruction(
                    title = UiText.Res(Res.string.tooltip_weight_type_title_trainings),
                    description = UiText.Res(Res.string.tooltip_weight_type_description_trainings)
                )
            }

            Metric.FORCE_TYPE -> when (scope) {
                Scope.TRAINING -> Instruction(
                    title = UiText.Res(Res.string.tooltip_force_type_title_training),
                    description = UiText.Res(Res.string.tooltip_force_type_description_training)
                )

                Scope.TRAININGS -> Instruction(
                    title = UiText.Res(Res.string.tooltip_force_type_title_trainings),
                    description = UiText.Res(Res.string.tooltip_force_type_description_trainings)
                )
            }

            Metric.EXPERIENCE -> when (scope) {
                Scope.TRAINING -> Instruction(
                    title = UiText.Res(Res.string.tooltip_experience_title_training),
                    description = UiText.Res(Res.string.tooltip_experience_description_training)
                )

                Scope.TRAININGS -> Instruction(
                    title = UiText.Res(Res.string.tooltip_experience_title_trainings),
                    description = UiText.Res(Res.string.tooltip_experience_description_trainings)
                )
            }
        }
    }

    // ---------------- Weighting strategy ----------------

    public sealed interface Weighting {
        public data object Count : Weighting
        public data object Sets : Weighting
        public data object Reps : Weighting
        public data object Volume : Weighting
    }

    // FIXED: Volume = Σ(weight × reps), not just Σ(weight)
    private fun weightOfExercise(ex: ExerciseState, w: Weighting): Float = when (w) {
        Weighting.Count -> 1f
        Weighting.Sets -> ex.iterations.size.toFloat()
        Weighting.Reps -> ex.iterations.sumOf { it.repetitions.value ?: 0 }.toFloat()
        Weighting.Volume -> ex.iterations
            .sumOf { ((it.volume.value ?: 0f) * (it.repetitions.value ?: 0)).toDouble() }
            .toFloat()
    }.coerceAtLeast(0f)

    private inline fun <reified E : Enum<E>> stableOrder(): List<E> =
        enumValues<E>().toList()

    private fun <E : Enum<E>> aggregateTotals(
        exercises: List<ExerciseState>,
        weighting: Weighting,
        keySelector: (ExerciseState) -> E?
    ): Map<E, Float> {
        val totals = mutableMapOf<E, Float>()
        for (ex in exercises) {
            val key = keySelector(ex) ?: continue
            val weight = weightOfExercise(ex, weighting)
            totals[key] = (totals[key] ?: 0f) + weight
        }
        return totals
    }

    // ---------------- Category Distribution ----------------

    /** 🏗 Category Distribution — Compound vs Isolation (single session) */
    public suspend fun calculateCategoryDistributionFromExercises(
        exercises: List<ExerciseState>,
        weighting: Weighting = Weighting.Count
    ): Pair<DSPieData, Instruction> {
        val data = buildCategoryPie(exercises, weighting)
        val tip = instructionFor(Metric.CATEGORY, Scope.TRAINING)
        return Pair(data, tip)
    }

    /** 🏗 Category Distribution across multiple trainings (period) */
    public suspend fun calculateCategoryDistributionFromTrainings(
        trainings: List<TrainingState>,
        weighting: Weighting = Weighting.Count
    ): Pair<DSPieData, Instruction> {
        val data = buildCategoryPie(trainings.flatMap { it.exercises }, weighting)
        val tip = instructionFor(Metric.CATEGORY, Scope.TRAININGS)
        return Pair(data, tip)
    }

    private suspend fun buildCategoryPie(
        exercises: List<ExerciseState>,
        weighting: Weighting
    ): DSPieData {
        val colors = colorProvider.get()
        val ordered = stableOrder<CategoryEnumState>()
        val totals = aggregateTotals(exercises, weighting) { it.exerciseExample?.category }

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
        return DSPieData(slices)
    }

    // ---------------- Weight Type Distribution ----------------

    /** ⚖️ Weight Type Distribution — Free vs Fixed vs Bodyweight (single session) */
    public suspend fun calculateWeightTypeDistributionFromExercises(
        exercises: List<ExerciseState>,
        weighting: Weighting = Weighting.Count
    ): Pair<DSPieData, Instruction> {
        val data = buildWeightTypePie(exercises, weighting)
        val tip = instructionFor(Metric.WEIGHT_TYPE, Scope.TRAINING)
        return Pair(data, tip)
    }

    /** ⚖️ Weight Type Distribution across multiple trainings (period) */
    public suspend fun calculateWeightTypeDistributionFromTrainings(
        trainings: List<TrainingState>,
        weighting: Weighting = Weighting.Count
    ): Pair<DSPieData, Instruction> {
        val data = buildWeightTypePie(trainings.flatMap { it.exercises }, weighting)
        val tip = instructionFor(Metric.WEIGHT_TYPE, Scope.TRAININGS)
        return Pair(data, tip)
    }

    private suspend fun buildWeightTypePie(
        exercises: List<ExerciseState>,
        weighting: Weighting
    ): DSPieData {
        val colors = colorProvider.get()
        val ordered = stableOrder<WeightTypeEnumState>()
        val totals = aggregateTotals(exercises, weighting) { it.exerciseExample?.weightType }

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
        return DSPieData(slices)
    }

    // ---------------- Force Type Distribution ----------------

    /** 🔄 Force Type Distribution — Push / Pull / Hinge (single session) */
    public suspend fun calculateForceTypeDistributionFromExercises(
        exercises: List<ExerciseState>,
        weighting: Weighting = Weighting.Count
    ): Pair<DSPieData, Instruction> {
        val data = buildForceTypePie(exercises, weighting)
        val tip = instructionFor(Metric.FORCE_TYPE, Scope.TRAINING)
        return Pair(data, tip)
    }

    /** 🔄 Force Type Distribution across multiple trainings (period) */
    public suspend fun calculateForceTypeDistributionFromTrainings(
        trainings: List<TrainingState>,
        weighting: Weighting = Weighting.Count
    ): Pair<DSPieData, Instruction> {
        val data = buildForceTypePie(trainings.flatMap { it.exercises }, weighting)
        val tip = instructionFor(Metric.FORCE_TYPE, Scope.TRAININGS)
        return Pair(data, tip)
    }

    private suspend fun buildForceTypePie(
        exercises: List<ExerciseState>,
        weighting: Weighting
    ): DSPieData {
        val colors = colorProvider.get()
        val ordered = stableOrder<ForceTypeEnumState>()
        val totals = aggregateTotals(exercises, weighting) { it.exerciseExample?.forceType }

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
        return DSPieData(slices)
    }

    // ---------------- Experience Distribution ----------------

    /** 🎯 Experience Level Distribution — Beginner / Intermediate / Advanced / Pro (single session) */
    public suspend fun calculateExperienceDistributionFromExercises(
        exercises: List<ExerciseState>,
        weighting: Weighting = Weighting.Count
    ): Pair<DSPieData, Instruction> {
        val data = buildExperiencePie(exercises, weighting)
        val tip = instructionFor(Metric.EXPERIENCE, Scope.TRAINING)
        return Pair(data, tip)
    }

    /** 🎯 Experience Level Distribution across multiple trainings (period) */
    public suspend fun calculateExperienceDistributionFromTrainings(
        trainings: List<TrainingState>,
        weighting: Weighting = Weighting.Count
    ): Pair<DSPieData, Instruction> {
        val data = buildExperiencePie(trainings.flatMap { it.exercises }, weighting)
        val tip = instructionFor(Metric.EXPERIENCE, Scope.TRAININGS)
        return Pair(data, tip)
    }

    private suspend fun buildExperiencePie(
        exercises: List<ExerciseState>,
        weighting: Weighting
    ): DSPieData {
        val colors = colorProvider.get()
        val ordered = stableOrder<ExperienceEnumState>()
        val totals = aggregateTotals(exercises, weighting) { it.exerciseExample?.experience }

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
        return DSPieData(slices)
    }
}


