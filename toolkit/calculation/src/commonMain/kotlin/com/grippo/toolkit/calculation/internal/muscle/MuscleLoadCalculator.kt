package com.grippo.toolkit.calculation.internal.muscle

import androidx.compose.ui.graphics.Color
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.formatters.PercentageFormatState
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.muscles.MuscleEnumState
import com.grippo.core.state.muscles.MuscleGroupEnumState
import com.grippo.core.state.muscles.MuscleGroupState
import com.grippo.core.state.muscles.MuscleRepresentationState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.TrainingState
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.toolkit.calculation.models.MuscleLoadBreakdown
import com.grippo.toolkit.calculation.models.MuscleLoadEntry
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.contains

internal data class MuscleLoadBreakdowns(
    val perMuscle: MuscleLoadBreakdown,
    val perGroup: MuscleLoadBreakdown,
)

internal class MuscleLoadCalculator(
    private val stringProvider: StringProvider,
    private val colorProvider: ColorProvider,
) {

    private companion object {
        private const val EPS: Float = 1e-3f
    }

    suspend fun computeMuscleLoadBreakdownsFromExercises(
        exercises: List<ExerciseState>,
        examples: List<ExerciseExampleState>,
        groups: List<MuscleGroupState<MuscleRepresentationState.Plain>>,
    ): MuscleLoadBreakdowns {
        val workload = computeAutoWorkload(exercises)
        return calculateCore(
            exercises = exercises,
            examples = examples,
            groups = groups,
            workload = workload,
        )
    }

    suspend fun computeMuscleLoadBreakdownsFromTrainings(
        trainings: List<TrainingState>,
        range: DateRange,
        examples: List<ExerciseExampleState>,
        groups: List<MuscleGroupState<MuscleRepresentationState.Plain>>,
    ): MuscleLoadBreakdowns {
        val inRange = trainings.filter { it.createdAt in range }
        val exercises = inRange.flatMap { it.exercises }
        val workload = computeAutoWorkload(exercises)

        return calculateCore(
            exercises = exercises,
            examples = examples,
            groups = groups,
            workload = workload,
        )
    }

    suspend fun computeMuscleLoadBreakdownsFromExample(
        example: ExerciseExampleState,
    ): MuscleLoadBreakdowns {
        val muscleLoad = buildMap {
            example.bundles.forEach { bundle ->
                val weight = bundle.percentage.valueOrZero()
                if (weight <= 0f) return@forEach
                val muscle = bundle.muscle.type
                this[muscle] = (this[muscle] ?: 0f) + weight
            }
        }

        if (muscleLoad.isEmpty()) {
            val empty = MuscleLoadBreakdown(emptyList())
            return MuscleLoadBreakdowns(perMuscle = empty, perGroup = empty)
        }

        val colors = colorProvider.get()
        val scaleStops: List<Pair<Float, Color>> = colors.palette.palette5OrangeRedGrowth
            .let { palette ->
                val last = (palette.size - 1).coerceAtLeast(1)
                palette.mapIndexed { idx, color -> idx.toFloat() / last.toFloat() to color }
            }

        val perMuscleValues = buildExamplePerMuscleValues(muscleLoad)
        val perGroupValues = buildExampleGroupValues(muscleLoad)

        val perMuscleBreakdown = buildBreakdown(
            labelValues = perMuscleValues,
            mode = Mode.RELATIVE,
            relativeMode = RelativeMode.MAX,
            scaleStops = scaleStops,
        )
        val perGroupBreakdown = buildBreakdown(
            labelValues = perGroupValues,
            mode = Mode.RELATIVE,
            relativeMode = RelativeMode.SUM,
            scaleStops = scaleStops,
        )

        return MuscleLoadBreakdowns(
            perMuscle = perMuscleBreakdown,
            perGroup = perGroupBreakdown,
        )
    }

    private enum class Mode { ABSOLUTE, RELATIVE }
    private enum class RelativeMode { MAX, SUM }

    private sealed interface Workload {
        data object Volume : Workload
        data object Reps : Workload
        data class Tut(val secPerRep: Float = 3f) : Workload
    }

    private fun computeAutoWorkload(exercises: List<ExerciseState>): Workload {
        exercises.forEach { ex ->
            ex.iterations.forEach { iteration ->
                val weight = iteration.volume.value ?: 0f
                val reps = (iteration.repetitions.value ?: 0).coerceAtLeast(0)
                if (weight > EPS && reps > 0) return Workload.Volume
            }
        }
        return Workload.Reps
    }

    private suspend fun calculateCore(
        exercises: List<ExerciseState>,
        examples: List<ExerciseExampleState>,
        groups: List<MuscleGroupState<MuscleRepresentationState.Plain>>,
        workload: Workload,
    ): MuscleLoadBreakdowns {
        if (exercises.isEmpty()) {
            return MuscleLoadBreakdowns(
                perMuscle = MuscleLoadBreakdown(emptyList()),
                perGroup = MuscleLoadBreakdown(emptyList()),
            )
        }

        val colors = colorProvider.get()
        val scaleStops: List<Pair<Float, Color>> = colors.palette.palette5OrangeRedGrowth
            .let { palette ->
                val last = (palette.size - 1).coerceAtLeast(1)
                palette.mapIndexed { idx, color -> idx.toFloat() / last.toFloat() to color }
            }

        val exampleMap = examples.associateBy { it.value.id }
        val muscleLoad = computeMuscleLoad(exercises, exampleMap, workload)
        if (muscleLoad.isEmpty()) {
            return MuscleLoadBreakdowns(
                perMuscle = MuscleLoadBreakdown(emptyList()),
                perGroup = MuscleLoadBreakdown(emptyList()),
            )
        }

        val perMuscleValues = buildPerMuscleValues(muscleLoad, examples)
        val groupValues = if (groups.isNotEmpty()) {
            buildGroupValues(groups, muscleLoad)
        } else {
            perMuscleValues
        }

        val perMuscleBreakdown = buildBreakdown(
            labelValues = perMuscleValues,
            mode = Mode.RELATIVE,
            relativeMode = RelativeMode.MAX,
            scaleStops = scaleStops,
        )
        val perGroupBreakdown = buildBreakdown(
            labelValues = groupValues,
            mode = Mode.RELATIVE,
            relativeMode = RelativeMode.SUM,
            scaleStops = scaleStops,
        )

        return MuscleLoadBreakdowns(
            perMuscle = perMuscleBreakdown,
            perGroup = perGroupBreakdown,
        )
    }

    private fun computeMuscleLoad(
        exercises: List<ExerciseState>,
        exampleMap: Map<String, ExerciseExampleState>,
        workload: Workload,
    ): Map<MuscleEnumState, Float> {
        val muscleLoad = mutableMapOf<MuscleEnumState, Float>()

        exercises.forEach { ex ->
            val exampleId = ex.exerciseExample.id
            val example = exampleMap[exampleId] ?: return@forEach

            val exWorkload = when (workload) {
                Workload.Volume -> ex.iterations.fold(0f) { acc, itn ->
                    val w = itn.volume.value ?: 0f
                    val r = (itn.repetitions.value ?: 0).coerceAtLeast(0)
                    if (r == 0 || w <= 0f) acc else acc + w * r
                }

                Workload.Reps -> ex.iterations.fold(0) { acc, itn ->
                    acc + (itn.repetitions.value ?: 0).coerceAtLeast(0)
                }.toFloat()

                is Workload.Tut -> {
                    val reps = ex.iterations.fold(0) { acc, itn ->
                        acc + (itn.repetitions.value ?: 0).coerceAtLeast(0)
                    }
                    (reps * workload.secPerRep).coerceAtLeast(0f)
                }
            }.coerceAtLeast(0f)

            if (exWorkload <= 0f) return@forEach

            val denom = example.bundles.fold(0f) { acc, bundle ->
                acc + (bundle.percentage.value ?: 0).coerceAtLeast(0).toFloat()
            }
            if (denom <= 0f) return@forEach

            example.bundles.forEach { bundle ->
                val sharePercent = (bundle.percentage.value ?: 0).coerceAtLeast(0)
                if (sharePercent == 0) return@forEach
                val share = sharePercent / denom
                val loadShare = exWorkload * share
                val muscle = bundle.muscle.type
                muscleLoad[muscle] = (muscleLoad[muscle] ?: 0f) + loadShare
            }
        }

        return muscleLoad
    }

    private data class LabelValue(
        val label: UiText,
        val value: Float,
        val muscles: List<MuscleEnumState>,
    )

    private fun buildPerMuscleValues(
        muscleLoad: Map<MuscleEnumState, Float>,
        examples: List<ExerciseExampleState>,
    ): List<LabelValue> {
        val ordering = linkedSetOf<MuscleEnumState>().apply {
            examples.forEach { example ->
                example.bundles.forEach { add(it.muscle.type) }
            }
        }
        ordering.addAll(muscleLoad.keys)

        if (ordering.isEmpty()) return emptyList()

        return ordering.map { muscle ->
            LabelValue(
                label = muscle.title(),
                value = muscleLoad[muscle] ?: 0f,
                muscles = listOf(muscle),
            )
        }
    }

    private fun buildGroupValues(
        groups: List<MuscleGroupState<MuscleRepresentationState.Plain>>,
        muscleLoad: Map<MuscleEnumState, Float>,
    ): List<LabelValue> {
        if (groups.isEmpty()) return emptyList()
        return groups.map { group ->
            val muscles = group.muscles.map { it.value.type }
            val sum = muscles.fold(0f) { acc, muscle -> acc + (muscleLoad[muscle] ?: 0f) }
            LabelValue(
                label = group.type.title(),
                value = sum,
                muscles = muscles,
            )
        }
    }

    private fun buildExamplePerMuscleValues(
        muscleLoad: Map<MuscleEnumState, Float>,
    ): List<LabelValue> {
        if (muscleLoad.isEmpty()) return emptyList()
        return muscleLoad.map { (muscle, value) ->
            LabelValue(
                label = muscle.title(),
                value = value,
                muscles = listOf(muscle),
            )
        }
    }

    private fun buildExampleGroupValues(
        muscleLoad: Map<MuscleEnumState, Float>,
    ): List<LabelValue> {
        if (muscleLoad.isEmpty()) return emptyList()

        val totals = LinkedHashMap<MuscleGroupEnumState, Float>()
        val musclesByGroup = LinkedHashMap<MuscleGroupEnumState, MutableList<MuscleEnumState>>()

        muscleLoad.forEach { (muscle, value) ->
            val group = muscle.group()
            totals[group] = (totals[group] ?: 0f) + value
            musclesByGroup.getOrPut(group) { mutableListOf() }.add(muscle)
        }

        return totals.map { (group, value) ->
            LabelValue(
                label = group.title(),
                value = value,
                muscles = musclesByGroup[group]?.toList() ?: emptyList(),
            )
        }
    }

    private suspend fun buildBreakdown(
        labelValues: List<LabelValue>,
        mode: Mode,
        relativeMode: RelativeMode,
        scaleStops: List<Pair<Float, Color>>,
    ): MuscleLoadBreakdown {
        if (labelValues.isEmpty()) return MuscleLoadBreakdown(emptyList())

        val values = labelValues.map { it.value }
        val validValues = values.filter { it.isFinite() }
        if (validValues.isEmpty()) return MuscleLoadBreakdown(emptyList())

        val maxVal = validValues.maxOrNull() ?: 1f
        val sumVal = validValues.sum().takeIf { it > 0f } ?: 1f

        val entries = labelValues
            .sortedByDescending { it.value }
            .map { (label, raw, muscles) ->
                val labelText = label.text(stringProvider)
                val display = when (mode) {
                    Mode.ABSOLUTE -> raw
                    Mode.RELATIVE -> when (relativeMode) {
                        RelativeMode.MAX -> if (maxVal == 0f) 0f else (raw / maxVal) * 100f
                        RelativeMode.SUM -> if (sumVal == 0f) 0f else (raw / sumVal) * 100f
                    }
                }

                val color = when (mode) {
                    Mode.RELATIVE -> bandColorByPercent(display, scaleStops)
                    Mode.ABSOLUTE -> {
                        val ratio = if (maxVal == 0f) 0f else (raw / maxVal).coerceIn(0f, 1f)
                        interpolateColor(ratio, scaleStops)
                    }
                }

                MuscleLoadEntry(
                    label = labelText,
                    value = display,
                    color = color,
                    muscles = muscles,
                )
            }

        return MuscleLoadBreakdown(entries)
    }

    private fun interpolateColor(ratio: Float, stops: List<Pair<Float, Color>>): Color {
        if (stops.isEmpty()) return Color(0f, 0f, 0f, 1f)
        if (ratio <= stops.first().first) return stops.first().second
        if (ratio >= stops.last().first) return stops.last().second

        for (index in 0 until stops.size - 1) {
            val (p1, c1) = stops[index]
            val (p2, c2) = stops[index + 1]
            if (ratio >= p1 && ratio <= p2) {
                val t = (ratio - p1) / (p2 - p1)
                return lerpColor(c1, c2, t)
            }
        }
        return stops.last().second
    }

    private fun lerpColor(c1: Color, c2: Color, t: Float): Color = Color(
        red = (c1.red + (c2.red - c1.red) * t),
        green = (c1.green + (c2.green - c1.green) * t),
        blue = (c1.blue + (c2.blue - c1.blue) * t),
        alpha = (c1.alpha + (c2.alpha - c1.alpha) * t),
    )

    private fun bandColorByPercent(percent: Float, stops: List<Pair<Float, Color>>): Color {
        if (stops.isEmpty()) return Color.Black
        val colors = stops.map { it.second }
        val size = colors.size
        if (size == 1) return colors.first()

        val normalized = percent.coerceIn(0f, 100f)
        val bandWidth = 100f / size
        val idx = if (normalized >= 100f) size - 1 else (normalized / bandWidth).toInt()
            .coerceIn(0, size - 1)
        return colors[idx]
    }

    private fun PercentageFormatState.valueOrZero(): Float = when (this) {
        is PercentageFormatState.Valid -> value.toFloat()
        is PercentageFormatState.Invalid -> value?.toFloat() ?: 0f
        is PercentageFormatState.Empty -> 0f
    }

    private fun MuscleEnumState.group(): MuscleGroupEnumState = when (this) {
        MuscleEnumState.PECTORALIS_MAJOR_CLAVICULAR,
        MuscleEnumState.PECTORALIS_MAJOR_STERNOCOSTAL,
        MuscleEnumState.PECTORALIS_MAJOR_ABDOMINAL -> MuscleGroupEnumState.CHEST_MUSCLES

        MuscleEnumState.TRAPEZIUS,
        MuscleEnumState.LATISSIMUS_DORSI,
        MuscleEnumState.RHOMBOIDS,
        MuscleEnumState.TERES_MAJOR -> MuscleGroupEnumState.BACK_MUSCLES

        MuscleEnumState.RECTUS_ABDOMINIS,
        MuscleEnumState.OBLIQUES -> MuscleGroupEnumState.ABDOMINAL_MUSCLES

        MuscleEnumState.CALF,
        MuscleEnumState.GLUTEAL,
        MuscleEnumState.HAMSTRINGS,
        MuscleEnumState.QUADRICEPS,
        MuscleEnumState.ADDUCTORS,
        MuscleEnumState.ABDUCTORS -> MuscleGroupEnumState.LEGS

        MuscleEnumState.ANTERIOR_DELTOID,
        MuscleEnumState.LATERAL_DELTOID,
        MuscleEnumState.POSTERIOR_DELTOID -> MuscleGroupEnumState.SHOULDER_MUSCLES

        MuscleEnumState.BICEPS,
        MuscleEnumState.TRICEPS,
        MuscleEnumState.FOREARM -> MuscleGroupEnumState.ARMS_AND_FOREARMS
    }
}
