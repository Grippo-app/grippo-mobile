package com.grippo.authorization.profile.creation.excluded.muscles

import com.grippo.analytics.authorization.AuthAnalytics
import com.grippo.analytics.authorization.models.AuthEvent
import com.grippo.analytics.authorization.models.AuthScreen
import com.grippo.analytics.common.models.ControlAction
import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.domain.state.muscles.toState
import com.grippo.toolkit.analytics.context.NavigationReason
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.onEach

internal class ExcludedMusclesViewModel(
    private val analytics: AuthAnalytics,
    muscleFeature: MuscleFeature,
    initialState: ExcludedMusclesState = ExcludedMusclesState(),
) : BaseViewModel<ExcludedMusclesState, ExcludedMusclesDirection, ExcludedMusclesLoader>(
    initialState
), ExcludedMusclesContract {

    override val screenPresentation = { reason: NavigationReason ->
        analytics.presentScreen(AuthScreen.ExcludedMuscles, reason)
    }

    init {
        muscleFeature
            .observeMuscles()
            .onEach(::provideMuscles)
            .safeLaunch()
    }

    private fun provideMuscles(list: List<MuscleGroup>) {
        val suggestions = list.toState()
        if (suggestions.isEmpty()) {
            update { it.copy(suggestions = suggestions) }
            return
        }
        val allIds = suggestions
            .flatMap { it.muscles }
            .map { it.value.id }

        update { previous ->
            val knownIds = previous.knownMuscleIds.toSet()
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
                knownMuscleIds = allIds.toPersistentList(),
                selectedGroupId = selectedGroupId
            )
        }
    }

    override fun onGroupClick(id: String) {
        update { it.copy(selectedGroupId = id) }
        analytics.trackEvent(AuthEvent.Muscles.GroupSelected)
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
        analytics.trackEvent(
            AuthEvent.Muscles.SelectionChanged(id in selectedIds, selectedIds.size)
        )
    }

    override fun onNextClick() {
        analytics.trackEvent(AuthEvent.Muscles.Control(ControlAction.Next))
        val current = state.value
        val excludedMuscleIds = current.knownMuscleIds - current.selectedMuscleIds

        val direction = ExcludedMusclesDirection.MissingEquipment(
            excludedMuscleIds = excludedMuscleIds
        )
        navigateTo(direction)
    }

    override fun onBack() {
        analytics.trackEvent(AuthEvent.Muscles.Control(ControlAction.Back))
        navigateTo(ExcludedMusclesDirection.Back)
    }
}
