package com.grippo.data.features.api.metrics

import com.grippo.data.features.api.metrics.models.Digest
import com.grippo.data.features.api.training.models.Training
import com.grippo.toolkit.date.utils.DateRange
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO

public class TrainingDigestUseCase {

    public fun digest(
        trainings: List<Training>,
        range: DateRange,
    ): Digest {
        val stats = trainings.aggregate()
        val start = range.from.date
        val end = range.to.date

        return Digest(
            start = start,
            end = end,
            exercisesCount = stats.exercisesCount,
            trainingsCount = stats.trainingsCount,
            duration = stats.duration,
            totalVolume = stats.totalVolume,
            totalSets = stats.totalSets,
        )
    }

    private fun List<Training>.aggregate(): DigestStats {
        val trainingsCount = size
        val exercisesCount = sumOf { it.exercises.size }
        val duration = fold(ZERO) { acc: Duration, training -> acc + training.duration }
        val totalVolume = fold(0f) { acc, training -> acc + training.volume }
        val totalSets = sumOf { training ->
            training.exercises.sumOf { it.iterations.size }
        }

        return DigestStats(
            exercisesCount = exercisesCount,
            trainingsCount = trainingsCount,
            duration = duration,
            totalVolume = totalVolume,
            totalSets = totalSets,
        )
    }

    private data class DigestStats(
        val exercisesCount: Int,
        val trainingsCount: Int,
        val duration: Duration,
        val totalVolume: Float,
        val totalSets: Int,
    )
}
