package com.grippo.home.trainings

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.date.utils.DateTimeUtils
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.StringProvider
import com.grippo.design.resources.provider.date_picker_title
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.training.toState
import com.grippo.domain.state.training.transformToTrainingListValue
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.LocalDateTime

internal class HomeTrainingsViewModel(
    private val trainingFeature: TrainingFeature,
    private val dialogController: DialogController,
    private val stringProvider: StringProvider
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

    override fun openExercise(id: String) {
        val dialog = DialogConfig.Exercise(
            id = id,
        )

        dialogController.show(dialog)
    }

    override fun selectDate() {
        safeLaunch {
            val dialog = DialogConfig.DatePicker(
                title = stringProvider.get(Res.string.date_picker_title),
                initial = state.value.date,
                limitations = DateTimeUtils.trailingYear(),
                onResult = { value -> update { it.copy(date = value) } }
            )

            dialogController.show(dialog)
        }
    }

    override fun back() {
        navigateTo(HomeTrainingsDirection.Back)
    }
}