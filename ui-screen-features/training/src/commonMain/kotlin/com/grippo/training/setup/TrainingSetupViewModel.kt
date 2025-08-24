package com.grippo.training.setup

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.domain.state.muscles.toState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.onEach

internal class TrainingSetupViewModel(
    muscleFeature: MuscleFeature,
) : BaseViewModel<TrainingSetupState, TrainingSetupDirection, TrainingSetupLoader>(
    TrainingSetupState()
), TrainingSetupContract {

    init {
        muscleFeature.observeMuscles()
            .onEach(::provideMuscles)
            .safeLaunch()
    }

    private fun provideMuscles(list: List<MuscleGroup>) {
        val suggestions = list.toState()
        update { it.copy(suggestions = suggestions) }
    }

    override fun onSelect(id: String) {
        update {
            val newList: PersistentList<String> = it.selectedMuscleIds
                .toMutableList()
                .apply { if (contains(id)) remove(id) else add(id) }
                .toPersistentList()

            it.copy(selectedMuscleIds = newList)
        }
    }

    override fun onContinueClick() {
        navigateTo(TrainingSetupDirection.ToRecording(state.value.selectedMuscleIds))
    }

    override fun onBack() {
        navigateTo(TrainingSetupDirection.Back)
    }
}
