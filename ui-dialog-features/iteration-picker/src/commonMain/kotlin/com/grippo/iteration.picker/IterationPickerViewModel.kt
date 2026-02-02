package com.grippo.iteration.picker

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.trainings.IterationFocusState
import com.grippo.core.state.trainings.IterationState
import com.grippo.data.features.api.user.UserFeature
import com.grippo.data.features.api.user.models.User
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.onEach

public class IterationPickerViewModel(
    initial: IterationState,
    example: ExerciseExampleState,
    suggestions: List<IterationState>,
    number: Int,
    focus: IterationFocusState,
    userFeature: UserFeature
) : BaseViewModel<IterationPickerState, IterationPickerDirection, IterationPickerLoader>(
    IterationPickerState(
        value = initial,
        number = number,
        suggestions = suggestions.toPersistentList(),
        focus = focus,
        example = example
    )
), IterationPickerContract {

    init {
        userFeature.observeUser()
            .onEach(::provideUser)
            .safeLaunch()
    }

    private fun provideUser(user: User?) {
        val weight = user?.weight ?: return
        update { it.copy(userWeight = WeightFormatState.of(weight)) }
    }

    override fun onExternalWeightChange(value: String) {
        update {
            val iteration = it.value.copy(externalWeight = VolumeFormatState.of(value))
            it.copy(value = iteration)
        }
    }

    override fun onAssistWeightChange(value: String) {
        update {
            val iteration = it.value.copy(assistWeight = VolumeFormatState.of(value))
            it.copy(value = iteration)
        }
    }

    override fun onExtraWeightChange(value: String) {
        update {
            val iteration = it.value.copy(extraWeight = VolumeFormatState.of(value))
            it.copy(value = iteration)
        }
    }

    override fun onRepetitionsChange(value: String) {
        update {
            val iteration = it.value.copy(repetitions = RepetitionsFormatState.of(value))
            it.copy(value = iteration)
        }
    }

    override fun onIterationClick(id: String) {
        update {
            val selected = it.suggestions.find { f -> f.id == id } ?: return@update it
            val iteration = it.value.copy(
                externalWeight = selected.externalWeight,
                repetitions = selected.repetitions
            )
            it.copy(value = iteration)
        }
    }

    override fun onSubmit() {
        val direction = IterationPickerDirection.BackWithResult(
            value = state.value.value
        )
        navigateTo(direction)
    }

    override fun onBack() {
        navigateTo(IterationPickerDirection.Back)
    }
}