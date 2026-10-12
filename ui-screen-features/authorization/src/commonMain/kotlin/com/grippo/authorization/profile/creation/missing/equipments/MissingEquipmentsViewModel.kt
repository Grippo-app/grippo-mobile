package com.grippo.authorization.profile.creation.missing.equipments

import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.equipment.EquipmentFeature
import com.grippo.data.features.api.equipment.models.EquipmentGroup
import com.grippo.domain.state.equipment.toState
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.onEach

internal class MissingEquipmentsViewModel(
    equipmentFeature: EquipmentFeature,
) : BaseViewModel<MissingEquipmentsState, MissingEquipmentsDirection, MissingEquipmentsLoader>(
    MissingEquipmentsState()
), MissingEquipmentsContract {

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
    }

    override fun onNextClick() {
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
        navigateTo(MissingEquipmentsDirection.Back)
    }
}
