package com.grippo.calculation

import com.grippo.calculation.internal.daysInclusive
import com.grippo.calculation.internal.deriveScale
import com.grippo.calculation.models.BucketScale
import com.grippo.calculation.models.Instruction
import com.grippo.date.utils.contains
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
import com.grippo.state.datetime.PeriodState
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

    // -------------- Tooltip plumbing (scale-aware, like AnalyticsCalculator) --------------

    private enum class Metric { CATEGORY, WEIGHT_TYPE, FORCE_TYPE, EXPERIENCE }

    @Suppress("UNUSED_PARAMETER")
    private fun instructionFor(metric: Metric, scale: BucketScale, days: Int): Instruction {
        // For distributions we only distinguish: single session vs aggregated period
        val isTraining = (scale == BucketScale.EXERCISE)

        return when (metric) {
            Metric.CATEGORY ->
                if (isTraining) {
                    Instruction(
                        title = UiText.Res(Res.string.tooltip_category_title_training),
                        description = UiText.Res(Res.string.tooltip_category_description_training)
                    )
                } else {
                    Instruction(
                        title = UiText.Res(Res.string.tooltip_category_title_trainings),
                        description = UiText.Res(Res.string.tooltip_category_description_trainings)
                    )
                }

            Metric.WEIGHT_TYPE ->
                if (isTraining) {
                    Instruction(
                        title = UiText.Res(Res.string.tooltip_weight_type_title_training),
                        description = UiText.Res(Res.string.tooltip_weight_type_description_training)
                    )
                } else {
                    Instruction(
                        title = UiText.Res(Res.string.tooltip_weight_type_title_trainings),
                        description = UiText.Res(Res.string.tooltip_weight_type_description_trainings)
                    )
                }

            Metric.FORCE_TYPE ->
                if (isTraining) {
                    Instruction(
                        title = UiText.Res(Res.string.tooltip_force_type_title_training),
                        description = UiText.Res(Res.string.tooltip_force_type_description_training)
                    )
                } else {
                    Instruction(
                        title = UiText.Res(Res.string.tooltip_force_type_title_trainings),
                        description = UiText.Res(Res.string.tooltip_force_type_description_trainings)
                    )
                }

            Metric.EXPERIENCE ->
                if (isTraining) {
                    Instruction(
                        title = UiText.Res(Res.string.tooltip_experience_title_training),
                        description = UiText.Res(Res.string.tooltip_experience_description_training)
                    )
                } else {
                    Instruction(
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

    private fun weightOfExercise(ex: ExerciseState, w: Weighting): Float = when (w) {
        Weighting.Count -> 1f

        // Count only valid sets (positive reps)
        Weighting.Sets -> ex.iterations.count { (it.repetitions.value ?: 0) > 0 }.toFloat()

        // Sum only positive reps
        Weighting.Reps -> ex.iterations
            .sumOf { kotlin.math.max(0, it.repetitions.value ?: 0) }
            .toFloat()

        // Volume = Σ(weight × reps) with positive reps only
        Weighting.Volume -> ex.iterations
            .sumOf {
                val w = it.volume.value ?: 0f
                val r = kotlin.math.max(0, it.repetitions.value ?: 0)
                (w * r).toDouble()
            }
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

    /** Single session */
    public suspend fun calculateCategoryDistributionFromExercises(
        exercises: List<ExerciseState>,
        weighting: Weighting = Weighting.Count
    ): Pair<DSPieData, Instruction> {
        val data = buildCategoryPie(exercises, weighting)
        val tip = Instruction(
            title = UiText.Res(Res.string.tooltip_category_title_training),
            description = UiText.Res(Res.string.tooltip_category_description_training)
        )
        return Pair(data, tip)
    }

    /** Multi-session with PeriodState (bucket-aware tooltips) */
    public suspend fun calculateCategoryDistributionFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState,
        weighting: Weighting = Weighting.Count
    ): Pair<DSPieData, Instruction> {
        val inRange = trainings.filter { it.createdAt in period.range }
        val scale = deriveScale(period)
        val days = daysInclusive(period.range.from.date, period.range.to.date)
        val data = buildCategoryPie(inRange.flatMap { it.exercises }, weighting)
        val tip = instructionFor(Metric.CATEGORY, scale, days)
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

    /** Single session */
    public suspend fun calculateWeightTypeDistributionFromExercises(
        exercises: List<ExerciseState>,
        weighting: Weighting = Weighting.Count
    ): Pair<DSPieData, Instruction> {
        val data = buildWeightTypePie(exercises, weighting)
        val tip = Instruction(
            title = UiText.Res(Res.string.tooltip_weight_type_title_training),
            description = UiText.Res(Res.string.tooltip_weight_type_description_training)
        )
        return Pair(data, tip)
    }

    /** Multi-session with PeriodState */
    public suspend fun calculateWeightTypeDistributionFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState,
        weighting: Weighting = Weighting.Count
    ): Pair<DSPieData, Instruction> {
        val inRange = trainings.filter { it.createdAt in period.range }
        val scale = deriveScale(period)
        val days = daysInclusive(period.range.from.date, period.range.to.date)
        val data = buildWeightTypePie(inRange.flatMap { it.exercises }, weighting)
        val tip = instructionFor(Metric.WEIGHT_TYPE, scale, days)
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

    /** Single session */
    public suspend fun calculateForceTypeDistributionFromExercises(
        exercises: List<ExerciseState>,
        weighting: Weighting = Weighting.Count
    ): Pair<DSPieData, Instruction> {
        val data = buildForceTypePie(exercises, weighting)
        val tip = Instruction(
            title = UiText.Res(Res.string.tooltip_force_type_title_training),
            description = UiText.Res(Res.string.tooltip_force_type_description_training)
        )
        return Pair(data, tip)
    }

    /** Multi-session with PeriodState */
    public suspend fun calculateForceTypeDistributionFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState,
        weighting: Weighting = Weighting.Count
    ): Pair<DSPieData, Instruction> {
        val inRange = trainings.filter { it.createdAt in period.range }
        val scale = deriveScale(period)
        val days = daysInclusive(period.range.from.date, period.range.to.date)
        val data = buildForceTypePie(inRange.flatMap { it.exercises }, weighting)
        val tip = instructionFor(Metric.FORCE_TYPE, scale, days)
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

    /** Single session */
    public suspend fun calculateExperienceDistributionFromExercises(
        exercises: List<ExerciseState>,
        weighting: Weighting = Weighting.Count
    ): Pair<DSPieData, Instruction> {
        val data = buildExperiencePie(exercises, weighting)
        val tip = Instruction(
            title = UiText.Res(Res.string.tooltip_experience_title_training),
            description = UiText.Res(Res.string.tooltip_experience_description_training)
        )
        return Pair(data, tip)
    }

    /** Multi-session with PeriodState */
    public suspend fun calculateExperienceDistributionFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState,
        weighting: Weighting = Weighting.Count
    ): Pair<DSPieData, Instruction> {
        val inRange = trainings.filter { it.createdAt in period.range }
        val scale = deriveScale(period)
        val days = daysInclusive(period.range.from.date, period.range.to.date)
        val data = buildExperiencePie(inRange.flatMap { it.exercises }, weighting)
        val tip = instructionFor(Metric.EXPERIENCE, scale, days)
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


