package com.grippo.profile.equipments

import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.equipment.EquipmentFeature
import com.grippo.data.features.api.equipment.models.Equipment
import com.grippo.data.features.api.equipment.models.EquipmentGroup
import com.grippo.data.features.api.excluded.equipments.ExcludedEquipmentsFeature
import com.grippo.domain.state.equipment.toState
import kotlinx.collections.immutable.PersistentList
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
            .minus(excluded.map { it.id })
            .toPersistentList()

        update {
            val currentGroup = it.selectedGroupId
            val resolvedGroupId = when {
                currentGroup != null && suggestions.any { group -> group.id == currentGroup } -> currentGroup
                else -> suggestions.firstOrNull()?.id
            }

            it.copy(
                suggestions = suggestions,
                selectedEquipmentIds = selectedIds,
                selectedGroupId = resolvedGroupId
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
            val newList: PersistentList<String> = it.selectedEquipmentIds
                .toMutableList()
                .apply { if (contains(id)) remove(id) else add(id) }
                .toPersistentList()

            it.copy(selectedEquipmentIds = newList)
        }
    }

    override fun onApply() {
        val formattedList = state.value.suggestions
            .flatMap { it.equipments }
            .map { it.id } - state.value.selectedEquipmentIds

        safeLaunch(loader = ProfileEquipmentsLoader.ApplyButton) {
            excludedEquipmentsFeature.setExcludedEquipments(formattedList).getOrThrow()
            navigateTo(ProfileEquipmentsDirection.Back)
        }
    }
}
