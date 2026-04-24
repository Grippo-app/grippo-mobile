package com.grippo.domain.state.training

import com.grippo.core.state.formatters.DateFormatState
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.trainings.TimelineState
import com.grippo.core.state.trainings.TrainingPosition
import com.grippo.data.features.api.training.models.TrainingTimeline
import com.grippo.data.features.api.training.models.TrainingTimelinePosition
import com.grippo.data.features.api.training.models.TrainingTimelineValue
import com.grippo.domain.state.metrics.engagement.toState
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

public fun TrainingTimeline.toState(): ImmutableList<TimelineState> {
    if (values.isEmpty()) return persistentListOf()
    return values.toState()
}

public fun List<TrainingTimelineValue>.toState(): ImmutableList<TimelineState> {
    if (isEmpty()) return persistentListOf()
    return map { it.toStateValue() }.toPersistentList()
}

private fun TrainingTimelineValue.toStateValue(): TimelineState = when (this) {
    is TrainingTimelineValue.BetweenExercises -> TimelineState.BetweenExercises(
        position = position.toState(),
        key = key,
    )

    is TrainingTimelineValue.DateTime -> TimelineState.DateTime(
        startAt = DateTimeFormatState.of(
            value = DateTimeUtils.minus(createdAt, duration),
            range = DateRangePresets.infinity(),
            format = DateFormat.TimeOnly.Time24hHm
        ),
        createAt = DateTimeFormatState.of(
            value = createdAt,
            range = DateRangePresets.infinity(),
            format = DateFormat.TimeOnly.Time24hHm
        ),
        duration = DurationFormatState.of(duration),
        trainingId = trainingId,
        position = position.toState(),
        key = key,
    )

    is TrainingTimelineValue.FirstExercise -> TimelineState.FirstExercise(
        exerciseState = exercise.toState(),
        position = position.toState(),
        key = key,
        indexInTraining = indexInTraining,
    )

    is TrainingTimelineValue.LastExercise -> TimelineState.LastExercise(
        exerciseState = exercise.toState(),
        position = position.toState(),
        key = key,
        indexInTraining = indexInTraining,
    )

    is TrainingTimelineValue.MiddleExercise -> TimelineState.MiddleExercise(
        exerciseState = exercise.toState(),
        position = position.toState(),
        key = key,
        indexInTraining = indexInTraining,
    )

    is TrainingTimelineValue.SingleExercise -> TimelineState.SingleExercise(
        exerciseState = exercise.toState(),
        position = position.toState(),
        key = key,
        indexInTraining = indexInTraining,
    )

    is TrainingTimelineValue.MonthSummary -> TimelineState.MonthlyDigest(
        summary = summary.toState(),
        month = DateFormatState.of(
            value = month,
            range = DateRangePresets.infinity(),
            format = DateFormat.DateOnly.MmmYyyy,
        ),
        key = key,
        position = position.toState(),
    )

    is TrainingTimelineValue.MonthlyTrainingsDay -> TimelineState.MonthlyTrainingsDay(
        date = DateFormatState.of(
            value = date,
            range = DateRangePresets.infinity(),
            format = DateFormat.DateOnly.DateDdMmm,
        ),
        month = DateFormatState.of(
            value = month,
            range = DateRangePresets.infinity(),
            format = DateFormat.DateOnly.MmmYyyy,
        ),
        trainings = trainings.toState(),
        key = key,
        position = position.toState(),
    )

    is TrainingTimelineValue.WeeklySummary -> TimelineState.WeeklySummary(
        summary = summary.toState(),
        key = key,
        position = position.toState(),
    )

    is TrainingTimelineValue.WeeklyTrainingsDay -> TimelineState.WeeklyTrainingsDay(
        date = DateFormatState.of(
            value = date,
            range = DateRangePresets.infinity(),
            format = DateFormat.DateOnly.DateDdMmm,
        ),
        trainings = trainings.toState(),
        position = position.toState(),
        key = key,
    )
}

private fun TrainingTimelinePosition.toState(): TrainingPosition = when (this) {
    TrainingTimelinePosition.FIRST -> TrainingPosition.FIRST
    TrainingTimelinePosition.MIDDLE -> TrainingPosition.MIDDLE
    TrainingTimelinePosition.LAST -> TrainingPosition.LAST
    TrainingTimelinePosition.SINGLE -> TrainingPosition.SINGLE
    TrainingTimelinePosition.EMPTY -> TrainingPosition.EMPTY
}
