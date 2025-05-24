package com.grippo.authorization.registration.excluded.muscles

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.domain.mapper.muscles.toState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.onEach

internal class ExcludedMusclesViewModel(
    muscleFeature: MuscleFeature,
) : BaseViewModel<ExcludedMusclesState, ExcludedMusclesDirection, ExcludedMusclesLoader>(
    ExcludedMusclesState()
), ExcludedMusclesContract {

    init {
        muscleFeature
            .observeMuscles()
            .onEach(::provideMuscles)
            .safeLaunch()
    }

    private fun provideMuscles(list: List<MuscleGroup>) {
        val suggestions = list.toState()
        val selectedIds = suggestions.flatMap { it.muscles }.map { it.value.id }.toPersistentList()
        update { it.copy(suggestions = suggestions, selectedMuscleIds = selectedIds) }
    }

    override fun select(id: String) {
        update {
            val newList: PersistentList<String> = it.selectedMuscleIds
                .toMutableList()
                .apply { if (contains(id)) remove(id) else add(id) }
                .toPersistentList()

            it.copy(selectedMuscleIds = newList)
        }
    }

    override fun next() {
        val formattedList = state.value.suggestions
            .flatMap { it.muscles }
            .map { it.value.id } - state.value.selectedMuscleIds

        val direction = ExcludedMusclesDirection.MissingEquipment(
            excludedMuscleIds = formattedList
        )
        navigateTo(direction)
    }
}