package com.grippo.profile.muscles

import com.grippo.analytics.profile.ProfileAnalytics
import com.grippo.analytics.profile.models.ProfileEvent
import com.grippo.analytics.profile.models.ProfileOperation
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
    muscleFeature: MuscleFeature,
    private val excludedMusclesFeature: ExcludedMusclesFeature,
    initialState: ProfileMusclesState,
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
            analytics.trackOperation(ProfileOperation.RefreshMuscles) {
                excludedMusclesFeature.getExcludedMuscles()
            }
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
        analytics.trackEvent(ProfileEvent.Muscles.GroupSelected)
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
        val selectedIds = state.value.selectedMuscleIds
        analytics.trackEvent(ProfileEvent.Muscles.SelectionChanged(id in selectedIds, selectedIds.size))
    }

    override fun onApply() {
        val current = state.value
        val allIds = current.suggestions
            .flatMap { it.muscles }
            .map { it.value.id }
        if (allIds.isEmpty()) return
        val excludedMuscleIds = allIds - current.selectedMuscleIds

        analytics.trackEvent(ProfileEvent.Muscles.Apply)
        safeLaunch(loader = ProfileMusclesLoader.ApplyButton) {
            analytics.trackOperation(ProfileOperation.SaveMuscles) {
                excludedMusclesFeature.setExcludedMuscles(excludedMuscleIds)
            }
            navigateTo(ProfileMusclesDirection.Back)
        }
    }

    override fun onBack() {
        analytics.trackEvent(ProfileEvent.Muscles.DismissRequested)
        navigateTo(ProfileMusclesDirection.Back)
    }
}
