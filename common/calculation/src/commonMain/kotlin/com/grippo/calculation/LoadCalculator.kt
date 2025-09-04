package com.grippo.calculation

import androidx.compose.ui.graphics.Color
import com.grippo.calculation.models.Instruction
import com.grippo.date.utils.DateRange
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
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * 💪 **Muscle Load Calculator**
 *
 * Calculates load distribution across muscles or muscle groups
 * with configurable workload models and heatmap coloring.
 *
 * ---
 * 🔎 **Workload modes**
 * - Volume → Σ(weight × reps)
 * - Reps → Σ(reps)
 * - TUT (time under tension) → Σ(reps × secPerRep)
 *
 * 📐 **Output modes**
 * - ABSOLUTE → raw values in workload units
 * - RELATIVE → normalized to %
 *   - by MAX → relative to maximum muscle load
 *   - by SUM → relative to total workload
 *
 * ---
 * 🛠 **Usage**
 * - Pass a list of `ExerciseState` for single training
 * - Pass a list of `TrainingState` for multiple trainings
 */
public class LoadCalculator(
    private val stringProvider: StringProvider,
    private val colorProvider: ColorProvider,
) {

    public enum class Mode { ABSOLUTE, RELATIVE }
    public enum class RelativeMode { MAX, SUM }

    public sealed interface Workload {
        public data object Volume : Workload
        public data object Reps : Workload
        public data class TUT(val secPerRep: Float = 3f) : Workload
    }

    // ---------------- Public API (returns Pair<Data, Instruction>) ----------------

    /** 📊 Muscle/Muscle-Group Load Distribution — single training (session) */
    public suspend fun calculateMuscleLoadDistributionFromExercises(
        exercises: List<ExerciseState>,
        examples: List<ExerciseExampleState>,
        groups: List<MuscleGroupState<MuscleRepresentationState.Plain>>,
        mode: Mode,
        relativeMode: RelativeMode,
        workload: Workload,
    ): Pair<DSProgressData, Instruction> {
        val data = calculateMuscleLoadDistributionCore(
            exercises = exercises,
            examples = examples,
            groups = groups,
            mode = mode,
            relativeMode = relativeMode,
            workload = workload
        )
        val instruction = Instruction(
            title = UiText.Res(Res.string.tooltip_muscle_load_title_training),
            description = UiText.Res(Res.string.tooltip_muscle_load_description_training),
        )
        return data to instruction
    }

    /** 📊 Muscle/Muscle-Group Load Distribution — multiple trainings (period-aware) */
    public suspend fun calculateMuscleLoadDistributionFromTrainings(
        trainings: List<TrainingState>,
        examples: List<ExerciseExampleState>,
        groups: List<MuscleGroupState<MuscleRepresentationState.Plain>>,
        mode: Mode,
        relativeMode: RelativeMode,
        workload: Workload,
        period: PeriodState,
    ): Pair<DSProgressData, Instruction> {
        val data = calculateMuscleLoadDistributionCore(
            exercises = trainings.flatMap { it.exercises },
            examples = examples,
            groups = groups,
            mode = mode,
            relativeMode = relativeMode,
            workload = workload
        )
        val instruction = instructionForPeriod(period)
        return data to instruction
    }

    // ---------------- Core computation ----------------

    private suspend fun calculateMuscleLoadDistributionCore(
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
        val muscleLoadMap = mutableMapOf<MuscleEnumState, Float>()

        // ---- Step 1 & 2: workload aggregation ----
        exercises.forEach { ex ->
            val exampleId = ex.exerciseExample?.id ?: return@forEach
            val example = exampleMap[exampleId] ?: return@forEach

            val exWorkload: Float = when (workload) {
                Workload.Volume -> ex.iterations.fold(0f) { acc, itn ->
                    acc + (itn.volume.value ?: 0f)
                }

                Workload.Reps -> ex.iterations.fold(0) { acc, itn ->
                    acc + (itn.repetitions.value ?: 0)
                }.toFloat()

                is Workload.TUT -> {
                    val reps =
                        ex.iterations.fold(0) { acc, itn -> acc + (itn.repetitions.value ?: 0) }
                    reps * workload.secPerRep
                }
            }.coerceAtLeast(0f)

            if (exWorkload <= 0f) return@forEach

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
                muscleLoadMap[muscle] = (muscleLoadMap[muscle] ?: 0f) + loadShare
            }
        }

        if (muscleLoadMap.isEmpty()) return DSProgressData(items = emptyList())

        // ---- Step 3: group aggregation ----
        val labelValueMap: Map<String, Float> = if (groups.isNotEmpty()) {
            groups.map { group ->
                val sum =
                    group.muscles.fold(0f) { acc, m -> acc + (muscleLoadMap[m.value.type] ?: 0f) }
                group.type.title().text(stringProvider) to sum
            }.filter { it.second > 0f }.toMap()
        } else {
            muscleLoadMap.mapKeys { (muscle, _) -> muscle.title().text(stringProvider) }
        }

        if (labelValueMap.isEmpty()) return DSProgressData(items = emptyList())

        // ---- Step 4: normalization ----
        val values = labelValueMap.values
        val maxVal = values.maxOrNull() ?: 1f
        val sumVal = values.sum().takeIf { it > 0f } ?: 1f

        // ---- Step 5: build items with heatmap ----
        val items = labelValueMap.entries
            .sortedByDescending { it.value }
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

    // ---------------- Tooltip selection by period ----------------

    private fun instructionForPeriod(period: PeriodState): Instruction {
        val days = daysInclusive(period.range.from.date, period.range.to.date)
        val isWholeMonths = isWholeMonths(period.range)

        val (titleRes, descRes) = when (period) {
            is PeriodState.ThisDay -> Res.string.tooltip_muscle_load_title_day to
                    Res.string.tooltip_muscle_load_description_day

            is PeriodState.ThisWeek -> Res.string.tooltip_muscle_load_title_day to
                    Res.string.tooltip_muscle_load_description_day

            is PeriodState.ThisMonth -> Res.string.tooltip_muscle_load_title_month to
                    Res.string.tooltip_muscle_load_description_month

            is PeriodState.CUSTOM -> when {
                days > 365 -> Res.string.tooltip_muscle_load_title_year to
                        Res.string.tooltip_muscle_load_description_year

                days >= 30 && isWholeMonths -> Res.string.tooltip_muscle_load_title_month to
                        Res.string.tooltip_muscle_load_description_month

                else -> Res.string.tooltip_muscle_load_title_day to
                        Res.string.tooltip_muscle_load_description_day
            }
        }

        return Instruction(
            title = UiText.Res(titleRes),
            description = UiText.Res(descRes)
        )
    }

    // ---------------- Helpers ----------------

    private fun interpolateColor(ratio: Float, stops: List<Pair<Float, Color>>): Color {
        for (i in 0 until stops.size - 1) {
            val (p1, c1) = stops[i]
            val (p2, c2) = stops[i + 1]
            if (ratio in p1..p2) {
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

    // ---- Date helpers for period mapping ----
    private fun daysInclusive(from: LocalDate, to: LocalDate): Int {
        var cnt = 0
        var cur = from
        while (cur <= to) {
            cnt++
            cur = cur.plus(DatePeriod(days = 1))
        }
        return cnt
    }

    /** Whole months = from is 1st day, to is last day of its month, and from <= to. */
    private fun isWholeMonths(range: DateRange): Boolean {
        val from = range.from.date
        val to = range.to.date
        if (from.dayOfMonth != 1) return false
        val toIsLast = to.plus(DatePeriod(days = 1)).dayOfMonth == 1
        return toIsLast && from <= to
    }
}
