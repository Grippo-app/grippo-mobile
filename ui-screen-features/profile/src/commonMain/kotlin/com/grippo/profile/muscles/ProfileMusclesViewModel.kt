package com.grippo.profile.muscles

import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.excluded.muscles.ExcludedMusclesFeature
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.Muscle
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.domain.state.muscles.toState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.combine

internal class ProfileMusclesViewModel(
    muscleFeature: MuscleFeature,
    private val excludedMusclesFeature: ExcludedMusclesFeature,
) : BaseViewModel<ProfileMusclesState, ProfileMusclesDirection, ProfileMusclesLoader>(
    ProfileMusclesState()
), ProfileMusclesContract {

    init {
        combine(
            flow = muscleFeature.observeMuscles(),
            flow2 = excludedMusclesFeature.observeExcludedMuscles(),
            transform = ::provideMuscles
        ).safeLaunch()

        safeLaunch {
            excludedMusclesFeature.getExcludedMuscles().getOrThrow()
        }
    }

    private suspend fun provideMuscles(list: List<MuscleGroup>, excluded: List<Muscle>) {
        val suggestions = list.toState()
        val selectedIds = suggestions
            .flatMap { it.muscles }
            .map { it.value.id }
            .minus(excluded.map { it.id }.toSet())
            .toPersistentList()

        update { it.copy(suggestions = suggestions, selectedMuscleIds = selectedIds) }
    }

    override fun onSelect(id: String) {
        val newList: PersistentList<String> = state.value.selectedMuscleIds
            .toMutableList()
            .apply { if (contains(id)) remove(id) else add(id) }
            .toPersistentList()

        update { it.copy(selectedMuscleIds = newList) }
    }

    override fun onApply() {
        val formattedList = state.value.suggestions
            .flatMap { it.muscles }
            .map { it.value.id } - state.value.selectedMuscleIds

        safeLaunch(loader = ProfileMusclesLoader.ApplyButton) {
            excludedMusclesFeature.setExcludedMuscles(formattedList).getOrThrow()
            navigateTo(ProfileMusclesDirection.Back)
        }
    }

    override fun onBack() {
        navigateTo(ProfileMusclesDirection.Back)
    }

}
