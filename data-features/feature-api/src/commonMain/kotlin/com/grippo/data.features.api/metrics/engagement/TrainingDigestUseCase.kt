package com.grippo.data.features.api.metrics.engagement

import com.grippo.data.features.api.metrics.engagement.models.TrainingDigestResult
import com.grippo.data.features.api.training.models.Training
import com.grippo.toolkit.date.utils.DateRange
import kotlin.time.Duration.Companion.ZERO

public class TrainingDigestUseCase {

    /**
     * Assigns saved sessions by [Training.createdAt] inside the inclusive [range],
     * without prorating their duration across days.
     * Saved totals remain authoritative even when an exercise relation is unavailable.
     * Every selected record counts as a training; sets count the supplied exercise details.
     * An empty selection has zero totals and no average; an inverted range is invalid.
     * An invalid saved duration or volume makes that metric unavailable, never a partial total.
     */
    public fun digest(
        trainings: List<Training>,
        range: DateRange,
    ): TrainingDigestResult {
        val selected = selectTrainingsInRange(trainings = trainings, range = range)
        val trainingsCount = selected.size
        val duration = if (selected.all { it.duration.isFinite() && !it.duration.isNegative() }) {
            selected.fold(ZERO) { total, training -> total + training.duration }
                .takeIf { it.isFinite() }
        } else {
            null
        }
        val volume = if (selected.all { it.volume.isFinite() && it.volume >= 0f }) {
            selected.sumOf { it.volume.toDouble() }
        } else {
            null
        }
        val totalVolume = volume?.toFloat()?.takeIf { it.isFinite() }
        val totalSets = selected.sumOf { training ->
            training.exercises.sumOf { it.iterations.size }
        }
        val activeDays = selected.mapTo(mutableSetOf()) { it.createdAt.date }.size
        val avgVolumePerTraining = if (trainingsCount > 0 && volume != null) {
            (volume / trainingsCount).toFloat().takeIf { it.isFinite() }
        } else {
            null
        }

        return TrainingDigestResult(
            range = range,
            trainingsCount = trainingsCount,
            duration = duration,
            totalVolume = totalVolume,
            totalSets = totalSets,
            activeDays = activeDays,
            avgVolumePerTraining = avgVolumePerTraining,
        )
    }
}

internal fun selectTrainingsInRange(
    trainings: List<Training>,
    range: DateRange,
): List<Training> {
    require(range.isWellFormed()) { "Training range must end at or after its start." }

    return trainings
        .filter { it.createdAt >= range.from && it.createdAt <= range.to }
}
