package com.grippo.domain.state.training.transformation

import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.digest.WeeklyDigestState
import com.grippo.core.state.trainings.TrainingListValue
import com.grippo.core.state.trainings.TrainingPosition
import com.grippo.core.state.trainings.TrainingState
import com.grippo.data.features.api.training.models.Training
import com.grippo.domain.state.metrics.toWeeklyDigestState
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO

internal fun List<TrainingState>.toWeeklyTrainingListValue(
    range: DateRange,
    sourceTrainings: List<Training>? = null,
): ImmutableList<TrainingListValue> {
    val summary = when {
        !sourceTrainings.isNullOrEmpty() -> sourceTrainings.toWeeklyDigestState(range)
        else -> toWeeklyDigestStateFallback(range)
    }
    val groupedByDate: Map<LocalDate, List<TrainingState>> =
        sortedByDescending { it.createdAt }.groupBy { it.createdAt.date }
    val sortedDates = groupedByDate.keys.sortedDescending()

    val flat = mutableListOf<TrainingListValue>()
    flat += TrainingListValue.WeeklySummary(
        summary = summary,
        key = "weekly-summary-${summary.weekStart}",
    )

    sortedDates.forEachIndexed { index, date ->
        val trainingsForDate = groupedByDate.getValue(date)
        val position = when {
            sortedDates.size == 1 -> TrainingPosition.SINGLE
            index == 0 -> TrainingPosition.FIRST
            index == sortedDates.lastIndex -> TrainingPosition.LAST
            else -> TrainingPosition.MIDDLE
        }

        flat += TrainingListValue.WeeklyTrainingsDay(
            date = date,
            trainings = trainingsForDate.sortedByDescending { it.createdAt }.toPersistentList(),
            position = position,
            key = "weekly-day-$date",
        )
    }

    return flat.toPersistentList()
}

private fun List<TrainingState>.toWeeklyDigestStateFallback(
    range: DateRange?,
): WeeklyDigestState {
    val trainings = this

    val weekStart: LocalDate =
        range?.from?.date ?: trainings.minByOrNull { it.createdAt }!!.createdAt.date
    val weekEnd: LocalDate =
        range?.to?.date ?: trainings.maxByOrNull { it.createdAt }!!.createdAt.date

    val trainingsCount = trainings.size
    val exercisesCount = trainings.sumOf { it.exercises.size }
    val totalDuration: Duration = trainings.fold(ZERO) { acc, training -> acc + training.duration }
    val totalVolume: Float = trainings.fold(0f) { acc, training ->
        val trainingVolume = when (val volumeState = training.total.volume) {
            is VolumeFormatState.Valid -> volumeState.value
            is VolumeFormatState.Invalid -> volumeState.value ?: 0f
            is VolumeFormatState.Empty -> 0f
        }

        acc + trainingVolume
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
