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

    private suspend fun provideMuscles(list: List<MuscleGroup>) {
        val suggestions = list.toState()
        val previous = state.value

        val previousKnownIds = previous.suggestions
            .flatMap { it.muscles }
            .map { it.value.id }
            .toSet()
        val allIds = suggestions.flatMap { it.muscles }.map { it.value.id }

        val retainedSet = previous.selectedMuscleIds
            .filter { it in allIds }
            .toSet()
        val newlyDiscovered = allIds
            .filter { it !in previousKnownIds }
            .toSet()

        val selected = allIds
            .filter { it in retainedSet || it in newlyDiscovered }
            .toPersistentList()
            .ifEmpty { allIds.toPersistentList() }

        update {
            val selectedGroupId = it.selectedGroupId
                ?.takeIf { id -> suggestions.any { group -> group.id == id } }
                ?: suggestions.firstOrNull()?.id

            it.copy(
                suggestions = suggestions,
                selectedMuscleIds = selected,
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
        val formattedList = state.value.suggestions
            .flatMap { it.muscles }
            .map { it.value.id } - state.value.selectedMuscleIds

        val direction = ExcludedMusclesDirection.MissingEquipment(
            excludedMuscleIds = formattedList
        )
        navigateTo(direction)
    }

    override fun onBack() {
        navigateTo(ExcludedMusclesDirection.Back)
    }

}
