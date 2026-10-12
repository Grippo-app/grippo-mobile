package com.grippo.profile.equipments

import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.equipment.EquipmentFeature
import com.grippo.data.features.api.equipment.models.Equipment
import com.grippo.data.features.api.equipment.models.EquipmentGroup
import com.grippo.data.features.api.excluded.equipments.ExcludedEquipmentsFeature
import com.grippo.domain.state.equipment.toState
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.combine

internal class ProfileEquipmentsViewModel(
    equipmentFeature: EquipmentFeature,
    private val excludedEquipmentsFeature: ExcludedEquipmentsFeature,
) : BaseViewModel<ProfileEquipmentsState, ProfileEquipmentsDirection, ProfileEquipmentsLoader>(
    ProfileEquipmentsState()
), ProfileEquipmentsContract {

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
        val selectedIds = suggestions
            .flatMap { it.equipments }
            .map { it.id }
            .minus(excluded.map { it.id }.toSet())
            .toPersistentList()

        update {
            val selectedGroupId = it.selectedGroupId
                ?.takeIf { id -> suggestions.any { group -> group.id == id } }
                ?: suggestions.firstOrNull()?.id

            it.copy(
                suggestions = suggestions,
                selectedEquipmentIds = selectedIds,
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
                it.selectedEquipmentIds.removing(id)
            } else {
                it.selectedEquipmentIds.adding(id)
            }
            it.copy(selectedEquipmentIds = selectedEquipmentIds)
        }
    }

    override fun onApply() {
        val current = state.value
        val excludedEquipmentIds = current.suggestions
            .flatMap { it.equipments }
            .map { it.id } - current.selectedEquipmentIds

        safeLaunch(loader = ProfileEquipmentsLoader.ApplyButton) {
            excludedEquipmentsFeature.setExcludedEquipments(excludedEquipmentIds).getOrThrow()
            navigateTo(ProfileEquipmentsDirection.Back)
        }
    }
}
