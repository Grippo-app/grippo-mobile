package com.grippo.training.setup

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.muscle.MuscleFeature

internal class TrainingSetupViewModel(
    private val muscleFeature: MuscleFeature,
) : BaseViewModel<TrainingSetupState, TrainingSetupDirection, TrainingSetupLoader>(
    TrainingSetupState()
), TrainingSetupContract {

    override fun onToggleMuscle(id: String) {
        update { prev ->
            val new = prev.selectedMuscleIds.toMutableSet().also { set ->
                if (id in set) set.remove(id) else set.add(id)
            }
            prev.copy(selectedMuscleIds = new)
        }
    }

    override fun onContinueClick() {
        navigateTo(TrainingSetupDirection.ToRecording(state.value.selectedMuscleIds.toList()))
    }

    override fun onBack() {
        navigateTo(TrainingSetupDirection.Back)
    }
}
