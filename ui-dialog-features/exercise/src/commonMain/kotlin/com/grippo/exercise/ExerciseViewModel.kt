package com.grippo.exercise

import com.grippo.analytics.exercise.ExerciseAnalytics
import com.grippo.analytics.exercise.models.ExerciseEvent
import com.grippo.analytics.exercise.models.ExerciseOperation
import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.training.SelectExerciseHistoryUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.domain.state.training.toState
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus

public class ExerciseViewModel(
    id: String,
    private val analytics: ExerciseAnalytics,
    trainingFeature: TrainingFeature,
    selectExerciseHistoryUseCase: SelectExerciseHistoryUseCase,
) : BaseViewModel<ExerciseState, ExerciseDirection, ExerciseLoader>(
    ExerciseState.Loading
), ExerciseContract {

    init {
        trainingFeature.observeExercise(id)
            .onEach(::provideExercise)
            .safeLaunch()

        safeLaunch(
            loader = ExerciseLoader.History,
        ) {
            val content = state.first { it !is ExerciseState.Loading }
            if (content !is ExerciseState.Available) {
                return@safeLaunch
            }

            analytics.trackOperation(operation = ExerciseOperation.LoadHistory) {
                val selected = trainingFeature.observeExercise(id).first() ?: return@trackOperation
                val available = state.value as? ExerciseState.Available ?: return@trackOperation
                if (available.exercise.id != selected.id) {
                    return@trackOperation
                }

                val start = DateTimeUtils.startOfDay(selected.createdAt.date.minus(DatePeriod(days = 180)))
                val end = DateTimeUtils.endOfDay(selected.createdAt.date)

                trainingFeature.getTrainings(start = start, end = end).getOrThrow()

                val trainings = trainingFeature.observeTrainings(start = start, end = end).first()
                val current = trainingFeature.observeExercise(id).first()

                if (current != null) {
                    val history = selectExerciseHistoryUseCase.execute(current = current, trainings = trainings)

                    provideHistory(exerciseId = current.id, value = history)
                }
            }
        }
    }

    private fun provideExercise(value: Exercise?) {
        val exercise = value?.toState()

        update { current ->
            when {
                exercise == null -> ExerciseState.Unavailable
                current is ExerciseState.Available && current.exercise.id == exercise.id -> {
                    current.copy(exercise = exercise)
                }

                else -> ExerciseState.Available(exercise = exercise)
            }
        }
    }

    private fun provideHistory(exerciseId: String, value: List<Exercise>) {
        val records = value.toState()
        val history = ExerciseHistoryState.Loaded(records = records)

        update { current ->
            if (current !is ExerciseState.Available || current.exercise.id != exerciseId) {
                return@update current
            }

            current.copy(history = history)
        }
    }

    override fun onDismiss() {
        analytics.trackEvent(ExerciseEvent.DismissRequested)
        navigateTo(ExerciseDirection.Back)
    }

    override fun onExampleDetailsClick() {
        val content = state.value as? ExerciseState.Available ?: return
        val id = content.exercise.exerciseExample.id

        analytics.trackEvent(ExerciseEvent.ExampleDetailsClicked)
        navigateTo(ExerciseDirection.ShowExampleDetails(id))
    }

}
