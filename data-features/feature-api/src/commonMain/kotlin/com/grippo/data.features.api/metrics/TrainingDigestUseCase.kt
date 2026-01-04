package com.grippo.data.features.api.metrics

import com.grippo.data.features.api.metrics.models.MonthlyDigest
import com.grippo.data.features.api.metrics.models.WeeklyDigest
import com.grippo.data.features.api.training.models.Training
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.datetime.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO

public class TrainingDigestUseCase {

    public fun weeklyDigest(
        trainings: List<Training>,
        range: DateRange,
    ): WeeklyDigest {
        val stats = trainings.aggregate()
        val weekStart = range.from.date
        val weekEnd = range.to.date

        return WeeklyDigest(
            weekStart = weekStart,
            weekEnd = weekEnd,
            exercisesCount = stats.exercisesCount,
            trainingsCount = stats.trainingsCount,
            duration = stats.duration,
            totalVolume = stats.totalVolume,
            totalSets = stats.totalSets,
        )
    }

    public fun monthlyDigest(
        trainings: List<Training>,
        range: DateRange,
    ): MonthlyDigest {
        val stats = trainings.aggregate()
        val referenceDate = range.from.date
        val month = LocalDate(referenceDate.year, referenceDate.month, 1)

        return MonthlyDigest(
            month = month,
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
