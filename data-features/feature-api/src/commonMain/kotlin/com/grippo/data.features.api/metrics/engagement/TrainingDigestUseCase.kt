package com.grippo.data.features.api.metrics.engagement

import com.grippo.data.features.api.metrics.engagement.models.TrainingDigestResult
import com.grippo.data.features.api.training.models.Training
import com.grippo.toolkit.date.utils.DateRange
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO

public class TrainingDigestUseCase {

    public fun digest(
        trainings: List<Training>,
        range: DateRange,
    ): TrainingDigestResult {
        val stats = trainings.aggregate()
        val start = range.from.date
        val end = range.to.date

        return TrainingDigestResult(
            start = start,
            end = end,
            trainingsCount = stats.trainingsCount,
            duration = stats.duration,
            totalVolume = stats.totalVolume,
            totalSets = stats.totalSets,
            activeDays = stats.activeDays,
            avgVolumePerTraining = stats.avgVolumePerTraining,
        )
    }

    private fun List<Training>.aggregate(): DigestStats {
        val trainingsCount = size
        val duration = fold(ZERO) { acc: Duration, training -> acc + training.duration }
        val totalVolume = fold(0f) { acc, training -> acc + training.volume }
        val totalSets = sumOf { training ->
            training.exercises.sumOf { it.iterations.size }
        }
        val activeDays = mapTo(mutableSetOf()) { it.createdAt.date }.size
        val avgVolumePerTraining = if (trainingsCount > 0) {
            totalVolume / trainingsCount
        } else {
            0f
        }

        return DigestStats(
            trainingsCount = trainingsCount,
            duration = duration,
            totalVolume = totalVolume,
            totalSets = totalSets,
            activeDays = activeDays,
            avgVolumePerTraining = avgVolumePerTraining,
        )
    }

    private data class DigestStats(
        val trainingsCount: Int,
        val duration: Duration,
        val totalVolume: Float,
        val totalSets: Int,
        val activeDays: Int,
        val avgVolumePerTraining: Float,
    )
}
