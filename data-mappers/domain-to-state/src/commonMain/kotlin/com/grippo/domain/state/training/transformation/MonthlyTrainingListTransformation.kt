package com.grippo.domain.state.training.transformation

import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.digest.MonthlyDigestState
import com.grippo.core.state.trainings.TrainingListValue
import com.grippo.core.state.trainings.TrainingPosition
import com.grippo.core.state.trainings.TrainingState
import com.grippo.data.features.api.training.models.Training
import com.grippo.domain.state.metrics.toMonthlyDigestState
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO

internal fun List<TrainingState>.toMonthlyTrainingListValue(
    range: DateRange?,
    sourceTrainings: List<Training>? = null,
): ImmutableList<TrainingListValue> {
    val groupedByDate: Map<LocalDate, List<TrainingState>> = groupBy { training ->
        training.createdAt.date
    }
    val sortedDates = groupedByDate.keys.sortedDescending()
    val digest = when {
        !sourceTrainings.isNullOrEmpty() -> sourceTrainings.toMonthlyDigestState(range)
        else -> toMonthlyDigestStateFallback(range)
    }
    val flat = mutableListOf<TrainingListValue>()

    flat += TrainingListValue.MonthlyDigest(
        summary = digest,
        month = digest.month,
        key = "monthly-digest-${digest.month.year}-${digest.month.month.number}",
    )

    sortedDates.forEachIndexed { index, date ->
        val trainingsForDate = groupedByDate.getValue(date).sortedBy { it.createdAt }
        val position = when {
            sortedDates.size == 1 -> TrainingPosition.SINGLE
            index == 0 -> TrainingPosition.FIRST
            index == sortedDates.lastIndex -> TrainingPosition.LAST
            else -> TrainingPosition.MIDDLE
        }

        val monthReference = LocalDate(date.year, date.month, 1)
        flat += TrainingListValue.MonthlyTrainingsDay(
            date = date,
            month = monthReference,
            trainings = trainingsForDate.toPersistentList(),
            position = position,
            key = "monthly-day-$date",
        )
    }

    return flat.toPersistentList()
}

private fun List<TrainingState>.toMonthlyDigestStateFallback(
    range: DateRange?,
): MonthlyDigestState {
    val trainings = this

    val referenceDate: LocalDate =
        range?.from?.date ?: trainings.minByOrNull { it.createdAt }!!.createdAt.date
    val month = LocalDate(referenceDate.year, referenceDate.month, 1)

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

    return MonthlyDigestState(
        month = month,
        exercisesCount = exercisesCount,
        trainingsCount = trainingsCount,
        duration = totalDuration,
        total = totalVolumeState,
        totalSets = totalSets,
    )
}
