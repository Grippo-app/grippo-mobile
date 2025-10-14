package com.grippo.profile.muscles

import com.grippo.calculation.AnalyticsApi
import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.excluded.muscles.ExcludedMusclesFeature
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.Muscle
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.domain.state.muscles.toState
import com.grippo.state.muscles.MuscleGroupState
import com.grippo.state.muscles.MuscleRepresentationState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.combine

internal class ProfileMusclesViewModel(
    muscleFeature: MuscleFeature,
    private val excludedMusclesFeature: ExcludedMusclesFeature,
    stringProvider: StringProvider,
    colorProvider: ColorProvider,
) : BaseViewModel<ProfileMusclesState, ProfileMusclesDirection, ProfileMusclesLoader>(
    ProfileMusclesState()
), ProfileMusclesContract {

    private val analytics = AnalyticsApi(stringProvider, colorProvider)

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
            .minus(excluded.map { it.id })
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
