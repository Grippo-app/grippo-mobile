package com.grippo.domain.state.training.transformation

import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.examples.ForceTypeEnumState
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.PercentageFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.muscles.MuscleEnumState
import com.grippo.core.state.muscles.MuscleGroupEnumState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.TrainingState
import com.grippo.core.state.trainings.highlight.Highlight
import com.grippo.core.state.trainings.highlight.HighlightConsistency
import com.grippo.core.state.trainings.highlight.HighlightExerciseFocus
import com.grippo.core.state.trainings.highlight.HighlightMuscleFocus
import com.grippo.core.state.trainings.highlight.HighlightPerformanceMetric
import com.grippo.core.state.trainings.highlight.HighlightPerformanceStatus
import com.grippo.toolkit.logger.AppLogger
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.minutes

public fun List<TrainingState>.toHighlight(
    exerciseExamples: List<ExerciseExampleState> = emptyList(),
): Highlight {
    val trainings = this
    val totalDuration: Duration = trainings.fold(ZERO) { acc, training -> acc + training.duration }
    val exampleIndex = exerciseExamples.associateBy { it.value.id }

    return Highlight(
        totalDuration = totalDuration,
        focusExercise = trainings.focusExercise(),
        muscleFocus = trainings.muscleFocus(exampleIndex),
        consistency = trainings.consistency(),
        performance = trainings.performanceMetrics()
    )
}

private fun List<TrainingState>.focusExercise(): HighlightExerciseFocus? {
    val volumes = flatMap { training ->
        training.exercises.mapNotNull { exercise ->
            val volume = exercise.metrics.volume.value ?: return@mapNotNull null
            ExerciseVolume(exercise, volume)
        }
    }
    if (volumes.isEmpty()) return null

    val topEntry = volumes
        .groupBy { it.exercise.exerciseExample.id }
        .maxByOrNull { entry -> entry.value.sumOf { item -> item.volume.toDouble() } }
        ?: return null

    val sample = topEntry.value.first().exercise
    val totalVolume = topEntry.value.sumOf { it.volume.toDouble() }.toFloat()
    if (totalVolume <= 0f) return null

    return HighlightExerciseFocus(
        exampleId = sample.exerciseExample.id,
        name = sample.name,
        sessions = topEntry.value.size,
        totalVolume = VolumeFormatState.Valid(
            display = totalVolume.toInt().toString(),
            value = totalVolume
        ),
        forceType = sample.exerciseExample.forceType,
        weightType = sample.exerciseExample.weightType
    )
}

private fun List<TrainingState>.muscleFocus(
    exerciseExamples: Map<String, ExerciseExampleState>,
): HighlightMuscleFocus? {
    if (isEmpty()) return null

    val contributions = mutableMapOf<MuscleGroupEnumState, Float>()

    for (exercise in flatMap { it.exercises }) {
        val volume = exercise.metrics.volume.value?.takeIf { it > 0f } ?: continue
        val exampleId = exercise.exerciseExample.id
        val example = exerciseExamples[exampleId]
        if (example != null) {
            val bundles = example.bundles.mapNotNull { bundle ->
                val sharePercent = when (val format = bundle.percentage) {
                    is PercentageFormatState.Valid -> format.value.toFloat()
                    is PercentageFormatState.Invalid -> format.value?.toFloat() ?: 0f
                    is PercentageFormatState.Empty -> 0f
                }
                if (sharePercent <= 0f) {
                    null
                } else {
                    val group = when (bundle.muscle.type) {
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
                    group to sharePercent
                }
            }

            val totalShare = bundles.fold(0f) { acc, entry -> acc + entry.second }
            if (bundles.isNotEmpty() && totalShare > 0f) {
                bundles.forEach { (group, sharePercent) ->
                    val share = (sharePercent / totalShare) * volume
                    contributions[group] = (contributions[group] ?: 0f) + share
                }
                continue
            }
        }

        val fallbackGroup = when (exercise.exerciseExample.forceType) {
            ForceTypeEnumState.PULL -> MuscleGroupEnumState.BACK_MUSCLES
            ForceTypeEnumState.PUSH -> MuscleGroupEnumState.CHEST_MUSCLES
            ForceTypeEnumState.HINGE -> MuscleGroupEnumState.LEGS
        }

        contributions[fallbackGroup] = (contributions[fallbackGroup] ?: 0f) + volume
    }

    val (group, loadValue) = contributions.maxByOrNull { it.value } ?: return null
    val totalLoad = contributions.values.sum()
    if (totalLoad <= 0f) return null

    val percentage = ((loadValue / totalLoad) * 100).roundToInt()

    return HighlightMuscleFocus(
        muscleGroup = group,
        load = PercentageFormatState.of(percentage)
    )
}

private fun List<TrainingState>.consistency(): HighlightConsistency {
    val dates = map { it.createdAt.date }.distinct().sorted()
    if (dates.isEmpty()) {
        return HighlightConsistency(activeDays = 0, bestStreakDays = 0)
    }

    var bestStreak = 0
    var current = 0
    var previous: LocalDate? = null
    dates.forEach { date ->
        val previousDate = previous
        current = when {
            previousDate == null -> 1
            date == previousDate -> current
            date == previousDate.plus(DatePeriod(days = 1)) -> current + 1
            else -> 1
        }
        previous = date
        if (current > bestStreak) {
            bestStreak = current
        }
    }

    return HighlightConsistency(
        activeDays = dates.size,
        bestStreakDays = bestStreak
    )
}

private fun List<TrainingState>.performanceMetrics(): List<HighlightPerformanceMetric> {
    val latestTraining = maxByOrNull { it.createdAt } ?: return emptyList()
    val performance = mutableListOf<HighlightPerformanceMetric>()

    // 1) Duration (vs average)
    run {
        val values = map { it.duration.inWholeMinutes.toDouble() }
        val current = latestTraining.duration.inWholeMinutes.toDouble()
        if (values.isNotEmpty() && current > 0) {
            val average = values.average()
            val best = values.maxOrNull() ?: current
            val delta = percentageDelta(current, average) ?: 0
            val status = determinePerformanceStatus(delta = delta, current = current, best = best)
            performance += HighlightPerformanceMetric.Duration(
                deltaPercentage = delta,
                current = latestTraining.duration,
                average = average.minutes,
                best = best.minutes,
                status = status
            )
        }
    }

    // 2) Volume (vs average)
    run {
        val currentValue = latestTraining.metrics.volume.value
        val values = mapNotNull { it.metrics.volume.value?.toDouble() }
        if (currentValue != null && values.isNotEmpty()) {
            val average = values.average()
            val best = values.maxOrNull() ?: currentValue.toDouble()
            val delta = percentageDelta(currentValue.toDouble(), average) ?: 0
            val status = determinePerformanceStatus(
                delta = delta,
                current = currentValue.toDouble(),
                best = best
            )
            performance += HighlightPerformanceMetric.Volume(
                deltaPercentage = delta,
                current = latestTraining.metrics.volume,
                average = VolumeFormatState.of(average.toFloat()),
                best = VolumeFormatState.of(best.toFloat()),
                status = status
            )
        }
    }

    // 3) Repetitions (vs average)
    run {
        val currentValue = latestTraining.metrics.repetitions.value
        val values = mapNotNull { it.metrics.repetitions.value?.toDouble() }
        if (currentValue == null || values.isEmpty()) {
            val nonNullCount = count { it.metrics.repetitions.value != null }
            AppLogger.General.warning(
                "Highlights: repetitions trend skipped (no reps value). " +
                        "latestRepetitions=${latestTraining.metrics.repetitions} " +
                        "nonNullTrainings=$nonNullCount totalTrainings=${size}"
            )
            return@run
        }

        val average = values.average()
        val best = values.maxOrNull() ?: currentValue.toDouble()
        val delta = percentageDelta(currentValue.toDouble(), average) ?: 0
        val status = determinePerformanceStatus(
            delta = delta,
            current = currentValue.toDouble(),
            best = best
        )
        performance += HighlightPerformanceMetric.Repetitions(
            deltaPercentage = delta,
            current = latestTraining.metrics.repetitions,
            average = RepetitionsFormatState.of(average.toInt()),
            best = RepetitionsFormatState.of(best.toInt()),
            status = status
        )
    }

    // 4) Intensity (vs average)
    run {
        val currentValue = latestTraining.metrics.intensity.value
        val values = mapNotNull { it.metrics.intensity.value?.toDouble() }
        if (currentValue != null && values.isNotEmpty()) {
            val average = values.average()
            val best = values.maxOrNull() ?: currentValue.toDouble()
            val delta = percentageDelta(currentValue.toDouble(), average) ?: 0
            val status = determinePerformanceStatus(
                delta = delta,
                current = currentValue.toDouble(),
                best = best
            )
            performance += HighlightPerformanceMetric.Intensity(
                deltaPercentage = delta,
                current = latestTraining.metrics.intensity,
                average = IntensityFormatState.of(average.toFloat()),
                best = IntensityFormatState.of(best.toFloat()),
                status = status
            )
        }
    }

    return performance
}

private fun percentageDelta(current: Double, average: Double): Int? {
    if (average == 0.0) return null
    val delta = ((current - average) / average) * 100
    return delta.roundToInt()
}

private const val PERFORMANCE_EPS: Double = 1e-2
private const val IMPROVED_THRESHOLD: Int = 5
private const val DECLINED_THRESHOLD: Int = -5

private fun determinePerformanceStatus(
    delta: Int,
    current: Double,
    best: Double?,
): HighlightPerformanceStatus {
    val bestValue = best ?: current
    if (current >= bestValue - PERFORMANCE_EPS) {
        return HighlightPerformanceStatus.Record
    }
    return when {
        delta >= IMPROVED_THRESHOLD -> HighlightPerformanceStatus.Improved
        delta <= DECLINED_THRESHOLD -> HighlightPerformanceStatus.Declined
        else -> HighlightPerformanceStatus.Stable
    }
}

private data class ExerciseVolume(
    val exercise: ExerciseState,
    val volume: Float,
)
