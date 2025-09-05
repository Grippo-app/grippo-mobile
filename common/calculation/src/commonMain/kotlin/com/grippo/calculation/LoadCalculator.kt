package com.grippo.calculation

import androidx.compose.ui.graphics.Color
import com.grippo.calculation.models.Instruction
import com.grippo.calculation.models.isWholeMonths
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
import com.grippo.state.exercise.examples.WeightTypeEnumState
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
 * 💪 Muscle Load Calculator — SIMPLE API (auto modes)
 *
 * Public API:
 *  - calculateMuscleLoadDistributionFromExercises(...) -> Pair<DSProgressData, Instruction>
 *  - calculateMuscleLoadDistributionFromTrainings(...) -> Pair<DSProgressData, Instruction>
 *
 * Internally auto-selects:
 *  - Workload: Volume (weight×reps) or Reps when bodyweight dominates
 *  - Mode:     ABSOLUTE for single session (ThisDay), RELATIVE for periods
 *  - Relative: SUM for periods (share of total); MAX only if RELATIVE is ever used for session
 *
 * Returns data + localized tooltip Instruction (title + description) matched to period.
 */
public class LoadCalculator(
    private val stringProvider: StringProvider,
    private val colorProvider: ColorProvider,
) {

    // ======== PUBLIC SIMPLE API ========

    /** 📊 Muscle / Muscle-Group load for a single session (auto modes). */
    public suspend fun calculateMuscleLoadDistributionFromExercises(
        exercises: List<ExerciseState>,
        examples: List<ExerciseExampleState>,
        groups: List<MuscleGroupState<MuscleRepresentationState.Plain>>,
    ): Pair<DSProgressData, Instruction> {
        val (workload, mode, relMode) = resolveAutoModes(
            period = PeriodState.ThisDay,
            exercises = exercises
        )
        val data = calculateCore(
            exercises = exercises,
            examples = examples,
            groups = groups,
            mode = mode,
            relativeMode = relMode,
            workload = workload
        )
        val instruction = Instruction(
            title = UiText.Res(Res.string.tooltip_muscle_load_title_training),
            description = UiText.Res(Res.string.tooltip_muscle_load_description_training),
        )
        return data to instruction
    }

    /** 📊 Muscle / Muscle-Group load for a period (auto modes + period-based tooltip). */
    public suspend fun calculateMuscleLoadDistributionFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState,
        examples: List<ExerciseExampleState>,
        groups: List<MuscleGroupState<MuscleRepresentationState.Plain>>,
    ): Pair<DSProgressData, Instruction> {
        val inRange = trainings.filter { it.createdAt in period.range }
        val exercises = inRange.flatMap { it.exercises }

        val (workload, mode, relMode) = resolveAutoModes(
            period = period,
            exercises = exercises
        )

        val data = calculateCore(
            exercises = exercises,
            examples = examples,
            groups = groups,
            mode = mode,
            relativeMode = relMode,
            workload = workload
        )

        val instruction = instructionForPeriod(period)
        return data to instruction
    }

    // ======== INTERNAL (HIDDEN) ========

    // Hidden display modes
    private enum class Mode { ABSOLUTE, RELATIVE }
    private enum class RelativeMode { MAX, SUM }

    // Hidden workload models
    private sealed interface Workload {
        data object Volume : Workload        // Σ(weight × reps)
        data object Reps : Workload          // Σ(reps)
        data class TUT(val secPerRep: Float = 3f) : Workload // Σ(reps × secPerRep)
    }

    /**
     * Heuristics for selecting modes based on period and exercise mix:
     * - ThisDay → ABSOLUTE; Volume if bodyweight share is low, otherwise Reps
     * - Periods → RELATIVE + SUM; Volume vs Reps by the same heuristic
     */
    private fun resolveAutoModes(
        period: PeriodState,
        exercises: List<ExerciseState>
    ): Triple<Workload, Mode, RelativeMode> {
        val bwShare = bodyweightRepsShare(exercises)
        val workload: Workload = if (bwShare >= 0.60f) Workload.Reps else Workload.Volume

        return when (period) {
            is PeriodState.ThisDay -> {
                val mode = Mode.ABSOLUTE
                val rel = RelativeMode.SUM // not used in ABSOLUTE, kept for uniform signature
                Triple(workload, mode, rel)
            }

            is PeriodState.ThisWeek,
            is PeriodState.ThisMonth,
            is PeriodState.CUSTOM -> {
                val mode = Mode.RELATIVE
                val rel = RelativeMode.SUM
                Triple(workload, mode, rel)
            }
        }
    }

    /** Returns share of reps performed as bodyweight across provided exercises. */
    private fun bodyweightRepsShare(exercises: List<ExerciseState>): Float {
        var bwReps = 0
        var allReps = 0
        exercises.forEach { ex ->
            val reps = ex.iterations.sumOf { (it.repetitions.value ?: 0).coerceAtLeast(0) }
            allReps += reps
            val isBW = ex.exerciseExample?.weightType == WeightTypeEnumState.BODY_WEIGHT
            if (isBW) bwReps += reps
        }
        if (allReps <= 0) return 0f
        return (bwReps.toFloat() / allReps.toFloat()).coerceIn(0f, 1f)
    }

    // ---- Core computation (hidden) ----
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

        // 1) Compute per-exercise workload using the selected model
        exercises.forEach { ex ->
            val exampleId = ex.exerciseExample?.id ?: return@forEach
            val example = exampleMap[exampleId] ?: return@forEach

            val exWorkload = when (workload) {
                Workload.Volume -> ex.iterations.fold(0f) { acc, itn ->
                    val w = (itn.volume.value ?: 0f).coerceAtLeast(0f)
                    val r = (itn.repetitions.value ?: 0).coerceAtLeast(0)
                    acc + (w * r)
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

        // 3) Aggregate by groups (if provided) or keep by muscle
        val labelValueMap: Map<String, Float> = if (groups.isNotEmpty()) {
            groups.map { group ->
                val sum =
                    group.muscles.fold(0f) { acc, m -> acc + (muscleLoad[m.value.type] ?: 0f) }
                group.type.title().text(stringProvider) to sum
            }.filter { it.second > 0f }.toMap()
        } else {
            muscleLoad.mapKeys { (muscle, _) -> muscle.title().text(stringProvider) }
        }

        if (labelValueMap.isEmpty()) return DSProgressData(items = emptyList())

        // 4) Normalize / format values and colorize as a heatmap
        val values = labelValueMap.values
        val maxVal = values.maxOrNull() ?: 1f
        val sumVal = values.sum().takeIf { it > 0f } ?: 1f

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

    // ---- Tooltip selection by period ----
    private fun instructionForPeriod(period: PeriodState): Instruction {
        val days = daysInclusive(period.range.from.date, period.range.to.date)
        val wholeMonths = isWholeMonths(period.range)

        val (titleRes, descRes) = when (period) {
            is PeriodState.ThisDay ->
                Res.string.tooltip_muscle_load_title_day to
                        Res.string.tooltip_muscle_load_description_day

            is PeriodState.ThisWeek ->
                Res.string.tooltip_muscle_load_title_day to
                        Res.string.tooltip_muscle_load_description_day

            is PeriodState.ThisMonth ->
                Res.string.tooltip_muscle_load_title_month to
                        Res.string.tooltip_muscle_load_description_month

            is PeriodState.CUSTOM -> when {
                days >= 365 ->
                    Res.string.tooltip_muscle_load_title_year to
                            Res.string.tooltip_muscle_load_description_year

                days >= 30 && wholeMonths ->
                    Res.string.tooltip_muscle_load_title_month to
                            Res.string.tooltip_muscle_load_description_month

                else ->
                    Res.string.tooltip_muscle_load_title_day to
                            Res.string.tooltip_muscle_load_description_day
            }
        }

        return Instruction(
            title = UiText.Res(titleRes),
            description = UiText.Res(descRes)
        )
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

    // ---- Date / period helpers ----

    private fun daysInclusive(from: LocalDate, to: LocalDate): Int {
        var cnt = 0
        var cur = from
        while (cur <= to) {
            cnt++
            cur = cur.plus(DatePeriod(days = 1))
        }
        return cnt
    }
}
