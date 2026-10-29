package com.grippo.exercise

import com.grippo.analytics.exercise.ExerciseAnalytics
import com.grippo.analytics.exercise.models.ExerciseEvent
import com.grippo.analytics.exercise.models.ExerciseOperation
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.foundation.models.ErrorRecovery
import com.grippo.data.features.api.training.SelectExerciseHistoryUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.domain.state.training.toState
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.persistentListOf
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
    ExerciseState()
), ExerciseContract {

    init {
        val exercises = trainingFeature.observeExercise(id)

        exercises
            .onEach(::provideExercise)
            .safeLaunch(recovery = ErrorRecovery.Retry)

        safeLaunch(loader = ExerciseLoader.History, recovery = ErrorRecovery.Retry) {
            val selected = exercises.first() ?: return@safeLaunch

            analytics.trackOperation(operation = ExerciseOperation.LoadHistory) {
                val start = DateTimeUtils.startOfDay(selected.createdAt.date.minus(DatePeriod(days = 180)))
                val end = DateTimeUtils.endOfDay(selected.createdAt.date)

                trainingFeature.getTrainings(start = start, end = end)

                val trainings = trainingFeature.observeTrainings(start = start, end = end).first()
                val current = trainingFeature.observeExercise(id).first()

                if (current != null) {
                    val history = selectExerciseHistoryUseCase.execute(current = current, trainings = trainings)

                    provideHistory(history)
                }
            }
        }
    }

    private fun provideExercise(value: Exercise?) {
        val exercise = value?.toState()

        update { current ->
            val history = if (exercise == null) persistentListOf() else current.history

            current.copy(exercise = exercise, history = history)
        }
    }

    private fun provideHistory(value: List<Exercise>) {
        val records = value.toState()

        update { it.copy(history = records) }
    }

    override fun onDismiss() {
        analytics.trackEvent(ExerciseEvent.DismissRequested)
        navigateTo(ExerciseDirection.Back)
    }

    override fun onExampleDetailsClick() {
        val exercise = state.value.exercise ?: return
        val id = exercise.exerciseExample.id

        analytics.trackEvent(ExerciseEvent.ExampleDetailsClicked)
        navigateTo(ExerciseDirection.ShowExampleDetails(id))
    }

}
