package com.grippo.home.trainings

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.DateFormatState
import com.grippo.core.state.menu.MenuItemState
import com.grippo.core.state.profile.ProfileActivityMenu
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.date_picker_title
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.training.toState
import com.grippo.domain.state.training.transformation.transformToTrainingListValue
import com.grippo.home.trainings.HomeTrainingsDirection.Back
import com.grippo.home.trainings.HomeTrainingsDirection.EditTraining
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.DatePeriod

internal class HomeTrainingsViewModel(
    private val trainingFeature: TrainingFeature,
    private val dialogController: DialogController,
    private val stringProvider: StringProvider
) : BaseViewModel<HomeTrainingsState, HomeTrainingsDirection, HomeTrainingsLoader>(
    HomeTrainingsState()
), HomeTrainingsContract {

    init {
        state
            .map { it.date }
            .distinctUntilChanged()
            .flatMapLatest { trainingFeature.observeTrainings(start = it.from, end = it.to) }
            .onEach(::provideTrainings)
            .safeLaunch()

        state
            .map { it.date }
            .distinctUntilChanged()
            .onEach { trainingFeature.getTrainings(start = it.from, end = it.to).getOrThrow() }
            .safeLaunch()
    }

    private fun provideTrainings(list: List<Training>) {
        val trainings = list.toState().transformToTrainingListValue()
        update { it.copy(trainings = trainings) }
    }

    override fun onTrainingMenuClick(id: String) {
        safeLaunch {
            val list = TrainingMenu.entries.map {
                MenuItemState(
                    id = it.id,
                    text = it.text(stringProvider)
                )
            }

            val dialog = DialogConfig.MenuPicker(
                items = list,
                onResult = {
                    safeLaunch {
                        when (TrainingMenu.of(it)) {
                            TrainingMenu.Delete -> trainingFeature.deleteTraining(id).getOrThrow()
                            TrainingMenu.Edit -> navigateTo(EditTraining(id))
                            null -> {}
                        }
                    }
                }
            )

            dialogController.show(dialog)
        }
    }

    private fun onNextClick() {
        update { it.copy(date = DateTimeUtils.shift(it.date, DatePeriod(days = +1))) }
    }

    private fun onPreviousClick() {
        update { it.copy(date = DateTimeUtils.shift(it.date, DatePeriod(days = -1))) }
    }

    override fun onExerciseClick(id: String) {
        val dialog = DialogConfig.Exercise(
            id = id,
        )

        dialogController.show(dialog)
    }

    override fun onOpenProfile() {
        val dialog = DialogConfig.Profile(
            onResult = {
                when (it) {
                    ProfileActivityMenu.ExcludedMuscles -> navigateTo(HomeTrainingsDirection.ExcludedMuscles)
                    ProfileActivityMenu.MissingEquipment -> navigateTo(HomeTrainingsDirection.MissingEquipment)
                    ProfileActivityMenu.Debug -> navigateTo(HomeTrainingsDirection.Debug)
                }
            },
        )

        dialogController.show(dialog)
    }

    override fun onSelectDate() {
        safeLaunch {
            val value = DateFormatState.of(state.value.date.from, state.value.limitations)

            val dialog = DialogConfig.DatePicker(
                title = stringProvider.get(Res.string.date_picker_title),
                initial = value,
                limitations = state.value.limitations,
                onResult = { value ->
                    val date = value.value ?: return@DatePicker
                    val from = DateTimeUtils.startOfDay(date)
                    val to = DateTimeUtils.endOfDay(date)
                    update { it.copy(date = DateRange(from, to)) }
                }
            )

            dialogController.show(dialog)
        }
    }

    override fun onBack() {
        navigateTo(Back)
    }
}