package com.grippo.calculation

import androidx.compose.ui.graphics.Color
import com.grippo.calculation.models.BucketScale
import com.grippo.calculation.models.Instruction
import com.grippo.calculation.models.daysInclusive
import com.grippo.calculation.models.deriveScale
import com.grippo.date.utils.contains
import com.grippo.design.components.chart.DSProgressData
import com.grippo.design.components.chart.DSProgressItem
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.tooltip_muscle_load_description_day
import com.grippo.design.resources.provider.tooltip_muscle_load_description_month
import com.grippo.design.resources.provider.tooltip_muscle_load_description_training
import com.grippo.design.resources.provider.tooltip_muscle_load_description_year
import com.grippo.design.resources.provider.tooltip_muscle_load_title_day
import com.grippo.design.resources.provider.tooltip_muscle_load_title_month
import com.grippo.design.resources.provider.tooltip_muscle_load_title_training
import com.grippo.design.resources.provider.tooltip_muscle_load_title_year
import com.grippo.state.datetime.PeriodState
import com.grippo.state.exercise.examples.ExerciseExampleState
import com.grippo.state.formatters.UiText
import com.grippo.state.muscles.MuscleEnumState
import com.grippo.state.muscles.MuscleGroupState
import com.grippo.state.muscles.MuscleRepresentationState
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.TrainingState

/**
 * 💪 Muscle Load Calculator — SIMPLE API (no bodyweight special-casing)
 *
 * Public API:
 *  - calculateMuscleLoadDistributionFromExercises(exercises, examples, groups)
 *  - calculateMuscleLoadDistributionFromTrainings(trainings, period, examples, groups)
 *
 * Behavior:
 *  - Exercises (ThisDay): ABSOLUTE values
 *  - Trainings (any period): RELATIVE (SUM %) for predictable aggregates
 *
 * Workload auto-selection (simple & robust):
 *  - Volume (weight × reps) if there exists non-trivial entered weight across sets
 *  - Reps (sum of reps) otherwise
 *
 * Notes:
 *  - No user bodyweight parameter. We trust the entered set weight as-is.
 *  - Negative weights are ignored (treated as 0). Tiny noise weights are ignored via EPS.
 */
public class LoadCalculator(
    private val stringProvider: StringProvider,
    private val colorProvider: ColorProvider,
) {

    // Tiny threshold to ignore weight noise (e.g., 0.2..0.4 kg rounding)
    private companion object {
        private const val WEIGHT_EPS_KG: Float = 0.5f
    }

    // ======== PUBLIC SIMPLE API ========

    /** Single session (ABSOLUTE; auto workload). */
    public suspend fun calculateMuscleLoadDistributionFromExercises(
        exercises: List<ExerciseState>,
        examples: List<ExerciseExampleState>,
        groups: List<MuscleGroupState<MuscleRepresentationState.Plain>>,
    ): Pair<DSProgressData, Instruction> {
        val workload = chooseWorkload(exercises)
        val data = calculateCore(
            exercises = exercises,
            examples = examples,
            groups = groups,
            mode = Mode.RELATIVE,
            relativeMode = RelativeMode.SUM, // unused in ABSOLUTE
            workload = workload
        )
        val instruction = Instruction(
            title = UiText.Res(Res.string.tooltip_muscle_load_title_training),
            description = UiText.Res(Res.string.tooltip_muscle_load_description_training),
        )
        return data to instruction
    }

    /** Period aggregation (RELATIVE SUM %; auto workload). */
    public suspend fun calculateMuscleLoadDistributionFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState,
        examples: List<ExerciseExampleState>,
        groups: List<MuscleGroupState<MuscleRepresentationState.Plain>>,
    ): Pair<DSProgressData, Instruction> {
        val inRange = trainings.filter { it.createdAt in period.range }
        val exercises = inRange.flatMap { it.exercises }

        val workload = chooseWorkload(exercises)

        val data = calculateCore(
            exercises = exercises,
            examples = examples,
            groups = groups,
            mode = Mode.RELATIVE,            // forced for aggregates
            relativeMode = RelativeMode.SUM, // share of total (%)
            workload = workload
        )

        val instruction = instructionForPeriod(period)
        return data to instruction
    }

    // ======== INTERNAL ========

    private enum class Mode { ABSOLUTE, RELATIVE }
    private enum class RelativeMode { MAX, SUM }

    private sealed interface Workload {
        data object Volume : Workload        // Σ(weight × reps)
        data object Reps : Workload          // Σ(reps)
        data class TUT(val secPerRep: Float = 3f) : Workload // Σ(reps × secPerRep)
    }

    /** Simple rule: if there's any non-trivial weight entered across sets, use Volume; otherwise Reps. */
    private fun chooseWorkload(exercises: List<ExerciseState>): Workload {
        var tonnageLike = 0.0
        exercises.forEach { ex ->
            ex.iterations.forEach { itn ->
                val w = (itn.volume.value ?: 0f)
                val r = (itn.repetitions.value ?: 0).coerceAtLeast(0)
                val load = if (w > WEIGHT_EPS_KG) w else 0f
                if (r > 0 && load > 0f) tonnageLike += (load * r)
            }
        }
        return if (tonnageLike > 0.0) Workload.Volume else Workload.Reps
    }

    private suspend fun calculateCore(
        exercises: List<ExerciseState>,
        examples: List<ExerciseExampleState>,
        groups: List<MuscleGroupState<MuscleRepresentationState.Plain>>,
        mode: Mode,
        relativeMode: RelativeMode,
        workload: Workload,
    ): DSProgressData {
        val colors = colorProvider.get()
        val scaleStops = colors.palette.scaleStopsOrangeRed.sortedBy { it.first }

        val exampleMap = examples.associateBy { it.value.id }
        val muscleLoad = mutableMapOf<MuscleEnumState, Float>()

        // 1) Per-exercise workload with simplified semantics (no bodyweight augmentation)
        exercises.forEach { ex ->
            val exampleId = ex.exerciseExample?.id ?: return@forEach
            val example = exampleMap[exampleId] ?: return@forEach

            val exWorkload = when (workload) {
                Workload.Volume -> ex.iterations.fold(0f) { acc, itn ->
                    val w = (itn.volume.value ?: 0f)
                    val r = (itn.repetitions.value ?: 0).coerceAtLeast(0)
                    val load = if (w > WEIGHT_EPS_KG) w else 0f // ignore tiny/negative
                    if (r == 0 || load <= 0f) acc else acc + load * r
                }

                Workload.Reps -> ex.iterations.fold(0) { acc, itn ->
                    acc + (itn.repetitions.value ?: 0).coerceAtLeast(0)
                }.toFloat()

                is Workload.TUT -> {
                    val reps = ex.iterations.fold(0) { acc, itn ->
                        acc + (itn.repetitions.value ?: 0).coerceAtLeast(0)
                    }
                    (reps * workload.secPerRep).coerceAtLeast(0f)
                }
            }.coerceAtLeast(0f)

            if (exWorkload <= 0f) return@forEach

            // 2) Distribute workload across muscles using example bundles (percent shares)
            val denom = example.bundles.fold(0f) { acc, b ->
                acc + (b.percentage.value ?: 0).coerceAtLeast(0).toFloat()
            }
            if (denom <= 0f) return@forEach

            example.bundles.forEach { b ->
                val p = (b.percentage.value ?: 0).coerceAtLeast(0)
                if (p == 0) return@forEach
                val share = p / denom
                val loadShare = exWorkload * share
                val muscle = b.muscle.type
                muscleLoad[muscle] = (muscleLoad[muscle] ?: 0f) + loadShare
            }
        }

        if (muscleLoad.isEmpty()) return DSProgressData(items = emptyList())

        // 3) Aggregate by groups (or keep by muscle) — list avoids label collisions
        val labelValues: List<Pair<String, Float>> =
            if (groups.isNotEmpty()) {
                groups.map { group ->
                    val sum =
                        group.muscles.fold(0f) { acc, m -> acc + (muscleLoad[m.value.type] ?: 0f) }
                    group.type.title().text(stringProvider) to sum
                }.filter { it.second > 0f }
            } else {
                muscleLoad.map { (muscle, v) -> muscle.title().text(stringProvider) to v }
                    .filter { it.second > 0f }
            }

        if (labelValues.isEmpty()) return DSProgressData(items = emptyList())

        // 4) Normalize / format values and colorize as a heatmap
        val values = labelValues.map { it.second }
        val maxVal = values.maxOrNull() ?: 1f
        val sumVal = values.sum().takeIf { it > 0f } ?: 1f

        val items = labelValues
            .sortedByDescending { it.second }
            .map { (label, raw) ->
                val display = when (mode) {
                    Mode.ABSOLUTE -> raw
                    Mode.RELATIVE -> when (relativeMode) {
                        RelativeMode.MAX -> (raw / maxVal) * 100f
                        RelativeMode.SUM -> (raw / sumVal) * 100f
                    }
                }
                val ratio = if (maxVal == 0f) 0f else (raw / maxVal).coerceIn(0f, 1f)
                val color = interpolateColor(ratio, scaleStops)
                DSProgressItem(label = label, value = display, color = color)
            }

        return DSProgressData(items = items)
    }

    // ---- Tooltip selection by period (unified with bucketing scale) ----
    private fun instructionForPeriod(period: PeriodState): Instruction {
        val scale = deriveScale(period)
        val (titleRes, descRes) = when (scale) {
            BucketScale.EXERCISE ->
                Res.string.tooltip_muscle_load_title_training to
                        Res.string.tooltip_muscle_load_description_training

            BucketScale.DAY ->
                Res.string.tooltip_muscle_load_title_day to
                        Res.string.tooltip_muscle_load_description_day
            // No dedicated "week" strings; reuse month-view copy for weekly aggregation
            BucketScale.WEEK ->
                Res.string.tooltip_muscle_load_title_month to
                        Res.string.tooltip_muscle_load_description_month

            BucketScale.MONTH -> {
                val days = daysInclusive(period.range.from.date, period.range.to.date)
                if (days >= 365)
                    Res.string.tooltip_muscle_load_title_year to
                            Res.string.tooltip_muscle_load_description_year
                else
                    Res.string.tooltip_muscle_load_title_month to
                            Res.string.tooltip_muscle_load_description_month
            }
        }
        return Instruction(title = UiText.Res(titleRes), description = UiText.Res(descRes))
    }

    // ---- Palette / color helpers ----
    private fun interpolateColor(ratio: Float, stops: List<Pair<Float, Color>>): Color {
        if (stops.isEmpty()) return Color(0f, 0f, 0f, 1f)
        if (ratio <= stops.first().first) return stops.first().second
        if (ratio >= stops.last().first) return stops.last().second

        for (i in 0 until stops.size - 1) {
            val (p1, c1) = stops[i]
            val (p2, c2) = stops[i + 1]
            if (ratio >= p1 && ratio <= p2) {
                val t = (ratio - p1) / (p2 - p1)
                return lerpColor(c1, c2, t)
            }
        }
        return stops.last().second
    }

    private fun lerpColor(c1: Color, c2: Color, t: Float): Color =
        Color(
            red = (c1.red + (c2.red - c1.red) * t),
            green = (c1.green + (c2.green - c1.green) * t),
            blue = (c1.blue + (c2.blue - c1.blue) * t),
            alpha = (c1.alpha + (c2.alpha - c1.alpha) * t),
        )
}
