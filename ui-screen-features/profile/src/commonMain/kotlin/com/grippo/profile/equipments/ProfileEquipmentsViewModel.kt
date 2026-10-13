package com.grippo.profile.equipments

import com.grippo.analytics.profile.ProfileAnalytics
import com.grippo.analytics.profile.models.ProfileScreen
import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.equipment.EquipmentFeature
import com.grippo.data.features.api.equipment.models.Equipment
import com.grippo.data.features.api.equipment.models.EquipmentGroup
import com.grippo.data.features.api.excluded.equipments.ExcludedEquipmentsFeature
import com.grippo.domain.state.equipment.toState
import com.grippo.toolkit.analytics.context.NavigationReason
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.combine

internal class ProfileEquipmentsViewModel(
    private val analytics: ProfileAnalytics,
    initialState: ProfileEquipmentsState,
    equipmentFeature: EquipmentFeature,
    private val excludedEquipmentsFeature: ExcludedEquipmentsFeature,
) : BaseViewModel<ProfileEquipmentsState, ProfileEquipmentsDirection, ProfileEquipmentsLoader>(
    initialState
), ProfileEquipmentsContract {

    override val screenPresentation = { reason: NavigationReason ->
        analytics.presentScreen(ProfileScreen.Equipments, reason)
    }

    init {
        combine(
            flow = equipmentFeature.observeEquipments(),
            flow2 = excludedEquipmentsFeature.observeExcludedEquipments(),
            transform = ::provideEquipments
        )
            .safeLaunch()

        safeLaunch {
            excludedEquipmentsFeature.getExcludedEquipments().getOrThrow()
        }
    }

    private fun provideEquipments(
        list: List<EquipmentGroup>,
        excluded: List<Equipment>
    ) {
        val suggestions = list.toState()
        val allIds = suggestions
            .flatMap { it.equipments }
            .map { it.id }
        if (allIds.isEmpty()) {
            update { it.copy(suggestions = suggestions) }
            return
        }
        val excludedIds = excluded.map { it.id }.toSet()

        update { previous ->
            val knownIds = previous.knownEquipmentIds.toSet()
            val selectedIds = previous.selectedEquipmentIds.toSet()
            val selectedEquipmentIds = allIds.filter { id ->
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
                selectedEquipmentIds = selectedEquipmentIds,
                knownEquipmentIds = allIds.toPersistentList(),
                selectedGroupId = selectedGroupId
            )
        }
    }

    override fun onSelectGroup(id: String) {
        update { it.copy(selectedGroupId = id) }
    }

    override fun onBack() {
        navigateTo(ProfileEquipmentsDirection.Back)
    }

    override fun onSelectEquipment(id: String) {
        update {
            val selectedEquipmentIds = if (id in it.selectedEquipmentIds) {
                it.selectedEquipmentIds.toPersistentList().removing(id)
            } else {
                it.selectedEquipmentIds.toPersistentList().adding(id)
            }
            it.copy(selectedEquipmentIds = selectedEquipmentIds)
        }
    }

    override fun onApply() {
        val current = state.value
        val allIds = current.suggestions
            .flatMap { it.equipments }
            .map { it.id }
        if (allIds.isEmpty()) return
        val excludedEquipmentIds = allIds - current.selectedEquipmentIds

        safeLaunch(loader = ProfileEquipmentsLoader.ApplyButton) {
            excludedEquipmentsFeature.setExcludedEquipments(excludedEquipmentIds).getOrThrow()
            navigateTo(ProfileEquipmentsDirection.Back)
        }
    }
}
