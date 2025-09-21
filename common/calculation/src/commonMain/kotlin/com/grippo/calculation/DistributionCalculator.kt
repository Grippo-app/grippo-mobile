package com.grippo.calculation

import com.grippo.date.utils.contains
import com.grippo.design.components.chart.DSPieData
import com.grippo.design.components.chart.DSPieSlice
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.state.datetime.PeriodState
import com.grippo.state.exercise.examples.CategoryEnumState
import com.grippo.state.exercise.examples.ForceTypeEnumState
import com.grippo.state.exercise.examples.WeightTypeEnumState
import com.grippo.state.profile.ExperienceEnumState
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.TrainingState

/**
 * 📊 Training Distribution Calculator (no instructions)
 *
 * Builds pie-chart distributions across exercises or trainings using a chosen weighting:
 *  - Category         → Compound vs Isolation
 *  - Weight Type      → Free vs Fixed vs Bodyweight
 *  - Force Type       → Push vs Pull vs Hinge
 *  - Experience Level → Beginner / Intermediate / Advanced / Pro
 *
 * Weighting strategies:
 *  - Count  → each exercise = 1
 *  - Sets   → number of valid sets (reps > 0)
 *  - Reps   → Σ(reps) for valid sets
 *  - Volume → Σ(weight × reps) for valid sets (filters tiny/negative weights)
 *
 * Notes:
 *  - Uses double accumulation for volume to reduce rounding error.
 *  - Filters tiny/negative weights with EPS to avoid noise.
 *  - Exercises missing exerciseExample or the requested enum key are skipped.
 */
public class DistributionCalculator(
    private val stringProvider: StringProvider,
    private val colorProvider: ColorProvider
) {

    // --- Noise threshold for weight in kg (domain epsilon) ---
    private companion object {
        private const val EPS: Float = 1e-3f
    }

    // ---------------- Weighting strategy ----------------

    public sealed interface Weighting {
        public data object Count : Weighting
        public data object Sets : Weighting
        public data object Reps : Weighting
        public data object Volume : Weighting
    }

    // Fast, allocation-free accumulation per exercise
    private fun weightOfExercise(ex: ExerciseState, w: Weighting): Float {
        return when (w) {
            Weighting.Count -> 1f

            Weighting.Sets -> {
                var sets = 0
                for (itn in ex.iterations) {
                    val r = itn.repetitions.value ?: 0
                    if (r > 0) sets++
                }
                sets.toFloat()
            }

            Weighting.Reps -> {
                var reps = 0
                for (itn in ex.iterations) {
                    val r = itn.repetitions.value ?: 0
                    if (r > 0) reps += r
                }
                reps.toFloat()
            }

            Weighting.Volume -> {
                var tonnage = 0.0
                for (itn in ex.iterations) {
                    val r = itn.repetitions.value ?: 0
                    if (r <= 0) continue
                    val wRaw = itn.volume.value ?: 0f
                    val wPos = if (wRaw > EPS) wRaw else 0f // ignore negatives/tiny noise
                    if (wPos == 0f) continue
                    tonnage += (wPos.toDouble() * r.toDouble())
                }
                tonnage.toFloat()
            }
        }.coerceAtLeast(0f)
    }

    private inline fun <reified E : Enum<E>> stableOrder(): List<E> = enumValues<E>().toList()

    private fun <E : Enum<E>> aggregateTotals(
        exercises: List<ExerciseState>,
        weighting: Weighting,
        keySelector: (ExerciseState) -> E?
    ): Map<E, Float> {
        val totals = HashMap<E, Float>(8)
        for (ex in exercises) {
            val key = keySelector(ex) ?: continue
            val w = weightOfExercise(ex, weighting)
            if (w <= 0f) continue
            totals[key] = (totals[key] ?: 0f) + w
        }
        return totals
    }

    // ---------------- Category Distribution ----------------

    /** Single session */
    public suspend fun calculateCategoryDistributionFromExercises(
        exercises: List<ExerciseState>,
        weighting: Weighting = Weighting.Count
    ): DSPieData {
        return buildCategoryPie(exercises, weighting)
    }

    /** Multi-session with PeriodState */
    public suspend fun calculateCategoryDistributionFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState,
        weighting: Weighting = Weighting.Count
    ): DSPieData {
        val inRange = trainings.filter { it.createdAt in period.range }
        return buildCategoryPie(inRange.flatMap { it.exercises }, weighting)
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
    ): DSPieData {
        return buildWeightTypePie(exercises, weighting)
    }

    /** Multi-session with PeriodState */
    public suspend fun calculateWeightTypeDistributionFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState,
        weighting: Weighting = Weighting.Count
    ): DSPieData {
        val inRange = trainings.filter { it.createdAt in period.range }
        return buildWeightTypePie(inRange.flatMap { it.exercises }, weighting)
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
    ): DSPieData {
        return buildForceTypePie(exercises, weighting)
    }

    /** Multi-session with PeriodState */
    public suspend fun calculateForceTypeDistributionFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState,
        weighting: Weighting = Weighting.Count
    ): DSPieData {
        val inRange = trainings.filter { it.createdAt in period.range }
        return buildForceTypePie(inRange.flatMap { it.exercises }, weighting)
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
    ): DSPieData {
        return buildExperiencePie(exercises, weighting)
    }

    /** Multi-session with PeriodState */
    public suspend fun calculateExperienceDistributionFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState,
        weighting: Weighting = Weighting.Count
    ): DSPieData {
        val inRange = trainings.filter { it.createdAt in period.range }
        return buildExperiencePie(inRange.flatMap { it.exercises }, weighting)
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
                    ExperienceEnumState.BEGINNER -> colors.profile.experience.beginner
                    ExperienceEnumState.INTERMEDIATE -> colors.profile.experience.intermediate
                    ExperienceEnumState.ADVANCED -> colors.profile.experience.advanced
                    ExperienceEnumState.PRO -> colors.profile.experience.pro
                }
            )
        }
        return DSPieData(slices)
    }
}
