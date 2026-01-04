package com.grippo.domain.state.metrics

import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.digest.WeeklyDigestState
import com.grippo.data.features.api.training.models.Training
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.datetime.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO

public fun List<Training>.toWeeklyDigestState(range: DateRange): WeeklyDigestState {
    val trainings = this

    val weekStart: LocalDate = range.from.date
    val weekEnd: LocalDate = range.to.date

    val trainingsCount = trainings.size
    val exercisesCount = trainings.sumOf { it.exercises.size }
    val totalDuration: Duration = trainings.fold(ZERO) { acc, training -> acc + training.duration }
    val totalVolume: Float = trainings.fold(0f) { acc, training ->
        acc + training.volume
    }
    val totalSets: Int = trainings.sumOf { training ->
        training.exercises.sumOf { it.iterations.size }
    }

    val totalVolumeState = when {
        totalVolume == 0f -> VolumeFormatState.Empty()
        else -> VolumeFormatState.Valid(
            display = totalVolume.toInt().toString(),
            value = totalVolume
        )
    }

    return WeeklyDigestState(
        weekStart = weekStart,
        weekEnd = weekEnd,
        exercisesCount = exercisesCount,
        trainingsCount = trainingsCount,
        duration = totalDuration,
        total = totalVolumeState,
        totalSets = totalSets,
    )
}
