package com.grippo.domain.state.training

import com.grippo.core.state.trainings.TimelineState
import com.grippo.core.state.trainings.TrainingPosition
import com.grippo.data.features.api.training.models.TrainingTimeline
import com.grippo.data.features.api.training.models.TrainingTimelinePosition
import com.grippo.data.features.api.training.models.TrainingTimelineValue
import com.grippo.domain.state.metrics.toState
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
        createAt = createdAt,
        duration = duration,
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
        month = month,
        key = key,
        position = position.toState(),
    )

    is TrainingTimelineValue.MonthlyTrainingsDay -> TimelineState.MonthlyTrainingsDay(
        date = date,
        month = month,
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
        date = date,
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
