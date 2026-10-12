package com.grippo.authorization.profile.creation.excluded.muscles

import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.domain.state.muscles.toState
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
        val allIds = suggestions
            .flatMap { it.muscles }
            .map { it.value.id }

        update { previous ->
            val knownIds = previous.suggestions
                .flatMap { it.muscles }
                .map { it.value.id }
                .toSet()
            val selectedIds = previous.selectedMuscleIds.toSet()
            val selectedMuscleIds = allIds
                .filter { it in selectedIds || it !in knownIds }
                .toPersistentList()
            val selectedGroupId = previous.selectedGroupId
                ?.takeIf { id -> suggestions.any { group -> group.id == id } }
                ?: suggestions.firstOrNull()?.id

            previous.copy(
                suggestions = suggestions,
                selectedMuscleIds = selectedMuscleIds,
                selectedGroupId = selectedGroupId
            )
        }
    }

    override fun onGroupClick(id: String) {
        update { it.copy(selectedGroupId = id) }
    }

    override fun onSelect(id: String) {
        update {
            val selectedMuscleIds = if (id in it.selectedMuscleIds) {
                it.selectedMuscleIds.removing(id)
            } else {
                it.selectedMuscleIds.adding(id)
            }
            it.copy(selectedMuscleIds = selectedMuscleIds)
        }
    }

    override fun onNextClick() {
        val current = state.value
        val excludedMuscleIds = current.suggestions
            .flatMap { it.muscles }
            .map { it.value.id } - current.selectedMuscleIds

        val direction = ExcludedMusclesDirection.MissingEquipment(
            excludedMuscleIds = excludedMuscleIds
        )
        navigateTo(direction)
    }

    override fun onBack() {
        navigateTo(ExcludedMusclesDirection.Back)
    }
}
