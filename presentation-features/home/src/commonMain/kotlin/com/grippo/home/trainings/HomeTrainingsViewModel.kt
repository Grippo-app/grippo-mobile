package com.grippo.home.trainings

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.mapper.training.toState
import com.grippo.domain.mapper.training.transformToTrainingListValue
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.LocalDateTime

@OptIn(FlowPreview::class)
internal class HomeTrainingsViewModel(
    private val trainingFeature: TrainingFeature,
    private val dialogController: DialogController
) : BaseViewModel<HomeTrainingsState, HomeTrainingsDirection, HomeTrainingsLoader>(
    HomeTrainingsState()
), HomeTrainingsContract {

    init {
        trainingFeature.observeTrainings(
            start = LocalDateTime(2024, 1, 1, 11, 11, 11, 11),
            end = LocalDateTime(2026, 1, 1, 11, 11, 11, 11)
        )
            .onEach(::provideTrainings)
            .safeLaunch()

        safeLaunch(loader = HomeTrainingsLoader.Trainings) {
            trainingFeature.getTrainings(
                start = LocalDateTime(2024, 1, 1, 11, 11, 11, 11),
                end = LocalDateTime(2026, 1, 1, 11, 11, 11, 11)
            ).getOrThrow()
        }
    }

    private fun provideTrainings(list: List<Training>) {
        val trainings = list.toState().transformToTrainingListValue()
        update { it.copy(trainings = trainings) }
    }

    override fun openExerciseExample(id: String) {
        val dialog = DialogConfig.ExerciseExample(
            id = id,
            onResult = { value -> }
        )

        dialogController.show(dialog)
    }

    override fun selectDate() {
        val dialog = DialogConfig.DatePicker(
            initial = state.value.date,
            onResult = { value -> update { it.copy(date = value) } }
        )

        dialogController.show(dialog)
    }

    override fun back() {
        navigateTo(HomeTrainingsDirection.Back)
    }
}