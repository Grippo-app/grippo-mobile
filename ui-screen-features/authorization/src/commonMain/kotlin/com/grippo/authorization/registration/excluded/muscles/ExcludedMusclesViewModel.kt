package com.grippo.authorization.registration.excluded.muscles

import com.grippo.calculation.AnalyticsApi
import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.design.resources.provider.muscles.MuscleColorPreset
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.domain.state.muscles.toState
import com.grippo.state.muscles.MuscleGroupState
import com.grippo.state.muscles.MuscleRepresentationState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
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
        val selectedIds = suggestions
            .flatMap { it.muscles }
            .map { it.value.id }
            .toPersistentList()

        update { it.copy(suggestions = suggestions, selectedMuscleIds = selectedIds) }

        calculatePresets(suggestions, selectedIds)
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
        val builder = persistentMapOf<String, MuscleColorPreset>().builder()

        for (group in suggestions) {
            val preset = analytics.musclePresetFromSelection(group, selectedSet)
            builder[group.id] = preset
        }

        val presets = builder.build()

        update { it.copy(musclePresets = presets) }
    }
}
