package com.grippo.training.goal.details

import com.grippo.analytics.training.goal.details.TrainingGoalDetailsAnalytics
import com.grippo.analytics.training.goal.details.models.TrainingGoalDetailsEvent
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.foundation.models.ErrorRecovery
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.goal.GoalFeature
import com.grippo.data.features.api.goal.models.Goal
import com.grippo.data.features.api.metrics.profile.GoalFollowingUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.domain.state.metrics.profile.toState
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import com.grippo.toolkit.date.utils.currentDateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest

public class TrainingGoalDetailsViewModel(
    private val range: DateRange,
    private val analytics: TrainingGoalDetailsAnalytics,
    trainingFeature: TrainingFeature,
    goalFeature: GoalFeature,
    exerciseExampleFeature: ExerciseExampleFeature,
    private val goalFollowingUseCase: GoalFollowingUseCase,
) : BaseViewModel<TrainingGoalDetailsDialogState, TrainingGoalDetailsDirection, TrainingGoalDetailsLoader>(
    TrainingGoalDetailsDialogState()
), TrainingGoalDetailsContract {

    init {
        combine(
            trainingFeature.observeTrainings(start = range.from, end = range.to),
            goalFeature.observeGoal(),
            currentDateFlow(),
        ) { trainings, goal, _ -> trainings to goal }
            .flatMapLatest { source ->
                val ids = source.first.flatMap { it.exercises }.map { it.exerciseExample.id }.distinct()
                exerciseExampleFeature.observeExerciseExamples(ids).map { source }
            }
            .mapLatest(::provideProgress)
            .safeLaunch(recovery = ErrorRecovery.Retry)
    }

    private suspend fun provideProgress(value: Pair<List<Training>, Goal?>) {
        val (trainings, goal) = value
        val now = DateTimeUtils.now()
        val adherence = goal?.let {
            goalFollowingUseCase.fromTrainingsByPrimary(
                trainings = trainings,
                goal = it,
                range = range,
                now = now,
            )
        }
        val progress = adherence?.toState(now = now)

        update { it.copy(progress = progress) }
    }

    override fun onBack() {
        analytics.trackEvent(TrainingGoalDetailsEvent.DismissRequested)
        navigateTo(TrainingGoalDetailsDirection.Back)
    }

    override fun onAddGoal() {
        analytics.trackEvent(TrainingGoalDetailsEvent.AddGoalClicked)
        navigateTo(TrainingGoalDetailsDirection.AddGoal)
    }
}
