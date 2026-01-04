package com.grippo.domain.state.training.timeline

import com.grippo.core.state.trainings.TrainingListValue
import com.grippo.core.state.trainings.TrainingPosition
import com.grippo.data.features.api.training.models.TrainingTimeline
import com.grippo.data.features.api.training.models.TrainingTimelinePosition
import com.grippo.data.features.api.training.models.TrainingTimelineValue
import com.grippo.domain.state.metrics.toState
import com.grippo.domain.state.training.toState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

public fun TrainingTimeline.toState(): ImmutableList<TrainingListValue> {
    if (values.isEmpty()) return persistentListOf()
    return values.toState()
}

public fun List<TrainingTimelineValue>.toState(): ImmutableList<TrainingListValue> {
    if (isEmpty()) return persistentListOf()
    return map { it.toStateValue() }.toPersistentList()
}

private fun TrainingTimelineValue.toStateValue(): TrainingListValue = when (this) {
    is TrainingTimelineValue.BetweenExercises -> TrainingListValue.BetweenExercises(
        position = position.toState(),
        key = key,
    )

    is TrainingTimelineValue.DateTime -> TrainingListValue.DateTime(
        createAt = createdAt,
        duration = duration,
        trainingId = trainingId,
        position = position.toState(),
        key = key,
    )

    is TrainingTimelineValue.FirstExercise -> TrainingListValue.FirstExercise(
        exerciseState = exercise.toState(),
        position = position.toState(),
        key = key,
        indexInTraining = indexInTraining,
    )

    is TrainingTimelineValue.LastExercise -> TrainingListValue.LastExercise(
        exerciseState = exercise.toState(),
        position = position.toState(),
        key = key,
        indexInTraining = indexInTraining,
    )

    is TrainingTimelineValue.MiddleExercise -> TrainingListValue.MiddleExercise(
        exerciseState = exercise.toState(),
        position = position.toState(),
        key = key,
        indexInTraining = indexInTraining,
    )

    is TrainingTimelineValue.SingleExercise -> TrainingListValue.SingleExercise(
        exerciseState = exercise.toState(),
        position = position.toState(),
        key = key,
        indexInTraining = indexInTraining,
    )

    is TrainingTimelineValue.MonthSummary -> TrainingListValue.MonthlyDigest(
        summary = summary.toState(),
        month = month,
        key = key,
        position = position.toState(),
    )

    is TrainingTimelineValue.MonthlyTrainingsDay -> TrainingListValue.MonthlyTrainingsDay(
        date = date,
        month = month,
        trainings = trainings.toState(),
        key = key,
        position = position.toState(),
    )

    is TrainingTimelineValue.WeeklySummary -> TrainingListValue.WeeklySummary(
        summary = summary.toState(),
        key = key,
        position = position.toState(),
    )

    is TrainingTimelineValue.WeeklyTrainingsDay -> TrainingListValue.WeeklyTrainingsDay(
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
