package com.grippo.training.preferences

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.domain.state.muscles.toState
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.onEach

internal class TrainingPreferencesViewModel(
    private val muscleFeature: MuscleFeature,
) : BaseViewModel<TrainingPreferencesState, TrainingPreferencesDirection, TrainingPreferencesLoader>(
    TrainingPreferencesState()
), TrainingPreferencesContract {

    init {
    }

    override fun onToggleMuscle(id: String) {
        update { prev ->
            val new = prev.selectedMuscleIds.toMutableSet().also { set ->
                if (id in set) set.remove(id) else set.add(id)
            }
            prev.copy(selectedMuscleIds = new)
        }
    }

    override fun onContinueClick() {
        navigateTo(TrainingPreferencesDirection.ToRecording(state.value.selectedMuscleIds.toList()))
    }

    override fun onBack() {
        navigateTo(TrainingPreferencesDirection.Back)
    }
}