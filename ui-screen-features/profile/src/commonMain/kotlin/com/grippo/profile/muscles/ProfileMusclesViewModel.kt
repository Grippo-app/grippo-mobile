package com.grippo.profile.muscles

import com.grippo.analytics.profile.ProfileAnalytics
import com.grippo.analytics.profile.models.ProfileScreen
import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.excluded.muscles.ExcludedMusclesFeature
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.Muscle
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.domain.state.muscles.toState
import com.grippo.toolkit.analytics.context.NavigationReason
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.combine

internal class ProfileMusclesViewModel(
    private val analytics: ProfileAnalytics,
    initialState: ProfileMusclesState,
    muscleFeature: MuscleFeature,
    private val excludedMusclesFeature: ExcludedMusclesFeature,
) : BaseViewModel<ProfileMusclesState, ProfileMusclesDirection, ProfileMusclesLoader>(
    initialState
), ProfileMusclesContract {

    override val screenPresentation = { reason: NavigationReason ->
        analytics.presentScreen(ProfileScreen.Muscles, reason)
    }

    init {
        combine(
            flow = muscleFeature.observeMuscles(),
            flow2 = excludedMusclesFeature.observeExcludedMuscles(),
            transform = ::provideMuscles
        )
            .safeLaunch()

        safeLaunch {
            excludedMusclesFeature.getExcludedMuscles().getOrThrow()
        }
    }

    private fun provideMuscles(list: List<MuscleGroup>, excluded: List<Muscle>) {
        val suggestions = list.toState()
        val allIds = suggestions
            .flatMap { it.muscles }
            .map { it.value.id }
        if (allIds.isEmpty()) {
            update { it.copy(suggestions = suggestions) }
            return
        }
        val excludedIds = excluded.map { it.id }.toSet()

        update { previous ->
            val knownIds = previous.knownMuscleIds.toSet()
            val selectedIds = previous.selectedMuscleIds.toSet()
            val selectedMuscleIds = allIds.filter { id ->
                if (id !in knownIds) {
                    id !in excludedIds
                } else {
                    id in selectedIds
                }
            }.toPersistentList()
            val selectedGroupId = previous.selectedGroupId
                ?.takeIf { id -> suggestions.any { group -> group.id == id } }
                ?: suggestions.firstOrNull()?.id

            previous.copy(
                suggestions = suggestions,
                selectedMuscleIds = selectedMuscleIds,
                knownMuscleIds = allIds.toPersistentList(),
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
                it.selectedMuscleIds.toPersistentList().removing(id)
            } else {
                it.selectedMuscleIds.toPersistentList().adding(id)
            }
            it.copy(selectedMuscleIds = selectedMuscleIds)
        }
    }

    override fun onApply() {
        val current = state.value
        val allIds = current.suggestions
            .flatMap { it.muscles }
            .map { it.value.id }
        if (allIds.isEmpty()) return
        val excludedMuscleIds = allIds - current.selectedMuscleIds

        safeLaunch(loader = ProfileMusclesLoader.ApplyButton) {
            excludedMusclesFeature.setExcludedMuscles(excludedMuscleIds).getOrThrow()
            navigateTo(ProfileMusclesDirection.Back)
        }
    }

    override fun onBack() {
        navigateTo(ProfileMusclesDirection.Back)
    }
}
