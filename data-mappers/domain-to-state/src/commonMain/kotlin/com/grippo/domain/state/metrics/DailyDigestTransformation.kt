package com.grippo.domain.state.metrics

import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.digest.DailyDigestState
import com.grippo.core.state.trainings.TrainingState
import kotlinx.datetime.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO

// TODO change List<TrainingState> to List<Training>
internal fun List<TrainingState>.toDailyDigestState(
    date: LocalDate,
): DailyDigestState {
    val trainings = this

    // how many trainings and exercises per day
    val trainingsCount = trainings.size
    val exercisesCount = trainings.sumOf { it.exercises.size }

    // total duration
    val totalDuration: Duration = trainings.fold(ZERO) { acc, training ->
        acc + training.duration
    }

    // total volume: take volume from Training.metrics,
    // treat Empty/Invalid/null as 0
    val totalVolume: Float = trainings.fold(0f) { acc, training ->
        val trainingVolume = when (val volumeState = training.metrics.volume) {
            is VolumeFormatState.Valid -> volumeState.value
            is VolumeFormatState.Invalid -> volumeState.value ?: 0f
            is VolumeFormatState.Empty -> 0f
        }

        acc + trainingVolume
    }

    // total sets: each IterationState is one set
    val totalSets: Int = trainings.sumOf { training ->
        training.exercises.sumOf { exercise ->
            exercise.iterations.size
        }
    }

    // build VolumeFormatState for daily total
    val totalVolumeState: VolumeFormatState = when {
        totalVolume == 0f -> VolumeFormatState.Empty()
        else -> VolumeFormatState.Valid(
            display = totalVolume.toInt().toString(),
            value = totalVolume
        )
    }

    return DailyDigestState(
        date = date,
        exercisesCount = exercisesCount,
        trainingsCount = trainingsCount,
        duration = totalDuration,
        total = totalVolumeState,
        totalSets = totalSets,
    )
}