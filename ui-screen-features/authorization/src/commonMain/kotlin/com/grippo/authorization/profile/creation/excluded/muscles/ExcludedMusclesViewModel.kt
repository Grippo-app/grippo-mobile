package com.grippo.authorization.profile.creation.excluded.muscles

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.muscles.MuscleGroupState
import com.grippo.core.state.muscles.MuscleRepresentationState
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.domain.state.muscles.toState
import com.grippo.toolkit.calculation.AnalyticsApi
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.onEach

internal class ExcludedMusclesViewModel(
    muscleFeature: MuscleFeature,
    stringProvider: StringProvider,
    colorProvider: ColorProvider,
) : BaseViewModel<ExcludedMusclesState, ExcludedMusclesDirection, ExcludedMusclesLoader>(
    ExcludedMusclesState()
), ExcludedMusclesContract {

    private val analytics = AnalyticsApi(stringProvider, colorProvider)

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

        update { it.copy(suggestions = suggestions, selectedMuscleIds = selected) }

        calculatePresets(suggestions, selected)
    }

    override fun onSelect(id: String) {
        val newList: PersistentList<String> = state.value.selectedMuscleIds
            .toMutableList()
            .apply { if (contains(id)) remove(id) else add(id) }
            .toPersistentList()

        update { it.copy(selectedMuscleIds = newList) }

        safeLaunch {
            calculatePresets(state.value.suggestions, newList)
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

    private suspend fun calculatePresets(
        suggestions: List<MuscleGroupState<MuscleRepresentationState.Plain>>,
        selectedIds: PersistentList<String>,
    ) {
        val selectedSet = selectedIds.toSet()
        val presets = suggestions
            .associate { group ->
                group.id to analytics.musclePresetFromSelection(group, selectedSet)
            }
            .toPersistentMap()

        update { it.copy(musclePresets = presets) }
    }
}
