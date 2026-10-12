package com.grippo.authorization.profile.creation.missing.equipments

import com.grippo.analytics.authorization.AuthAnalytics
import com.grippo.analytics.authorization.models.AuthEvent
import com.grippo.analytics.authorization.models.AuthScreen
import com.grippo.analytics.common.models.ControlAction
import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.equipment.EquipmentFeature
import com.grippo.data.features.api.equipment.models.EquipmentGroup
import com.grippo.domain.state.equipment.toState
import com.grippo.toolkit.analytics.context.NavigationReason
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.onEach

internal class MissingEquipmentsViewModel(
    private val analytics: AuthAnalytics,
    equipmentFeature: EquipmentFeature,
) : BaseViewModel<MissingEquipmentsState, MissingEquipmentsDirection, MissingEquipmentsLoader>(
    MissingEquipmentsState()
), MissingEquipmentsContract {

    override val screenPresentation = { reason: NavigationReason ->
        analytics.presentScreen(AuthScreen.MissingEquipments, reason)
    }

    init {
        equipmentFeature
            .observeEquipments()
            .onEach(::provideEquipments)
            .safeLaunch()
    }

    private fun provideEquipments(list: List<EquipmentGroup>) {
        val suggestions = list.toState()
        val allIds = suggestions
            .flatMap { it.equipments }
            .map { it.id }

        update { previous ->
            val knownIds = previous.suggestions
                .flatMap { it.equipments }
                .map { it.id }
                .toSet()
            val selectedIds = previous.selectedEquipmentIds.toSet()
            val selectedEquipmentIds = allIds
                .filter { it in selectedIds || it !in knownIds }
                .toPersistentList()
            val selectedGroupId = previous.selectedGroupId
                ?.takeIf { id -> suggestions.any { group -> group.id == id } }
                ?: suggestions.firstOrNull()?.id

            previous.copy(
                suggestions = suggestions,
                selectedEquipmentIds = selectedEquipmentIds,
                selectedGroupId = selectedGroupId
            )
        }
    }

    override fun onGroupClick(id: String) {
        update { it.copy(selectedGroupId = id) }
        analytics.trackEvent(AuthEvent.Equipments.GroupSelected)
    }

    override fun onEquipmentClick(id: String) {
        update {
            val selectedEquipmentIds = if (id in it.selectedEquipmentIds) {
                it.selectedEquipmentIds.removing(id)
            } else {
                it.selectedEquipmentIds.adding(id)
            }
            it.copy(selectedEquipmentIds = selectedEquipmentIds)
        }
        val selectedIds = state.value.selectedEquipmentIds
        analytics.trackEvent(
            AuthEvent.Equipments.SelectionChanged(id in selectedIds, selectedIds.size)
        )
    }

    override fun onNextClick() {
        analytics.trackEvent(AuthEvent.Equipments.Control(ControlAction.Next))
        val current = state.value
        val missingEquipmentIds = current.suggestions
            .flatMap { it.equipments }
            .map { it.id } - current.selectedEquipmentIds

        val direction = MissingEquipmentsDirection.Completed(
            missingEquipmentIds = missingEquipmentIds
        )
        navigateTo(direction)
    }

    override fun onBack() {
        analytics.trackEvent(AuthEvent.Equipments.Control(ControlAction.Back))
        navigateTo(MissingEquipmentsDirection.Back)
    }
}
