package com.grippo.home.trainings

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.date.utils.DateTimeUtils
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.date_picker_title
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.training.toState
import com.grippo.domain.state.training.transformToTrainingListValue
import com.grippo.state.formatters.DateFormatState
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

    override fun onExerciseClick(id: String) {
        val dialog = DialogConfig.Exercise(
            id = id,
        )

        dialogController.show(dialog)
    }

    override fun onSelectDate() {
        safeLaunch {
            val limits = DateTimeUtils.trailingYear()
            val value = DateFormatState.of(state.value.date, limits)

            val dialog = DialogConfig.DatePicker(
                title = stringProvider.get(Res.string.date_picker_title),
                initial = value,
                limitations = limits,
                onResult = { value ->
                    val date = value.value ?: return@DatePicker
                    update { it.copy(date = date) }
                }
            )

            dialogController.show(dialog)
        }
    }

    override fun onBack() {
        navigateTo(HomeTrainingsDirection.Back)
    }
}