package com.grippo.authorization.registration.missing.equipments

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.equipment.EquipmentFeature
import com.grippo.data.features.api.equipment.models.EquipmentGroup
import com.grippo.domain.mapper.equipment.toState
import kotlinx.collections.immutable.PersistentList
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
        val selectedIds = suggestions.flatMap { it.equipments }.map { it.id }.toPersistentList()
        update {
            it.copy(
                suggestions = suggestions,
                selectedEquipmentIds = selectedIds,
                selectedGroupId = suggestions.firstOrNull()?.id
            )
        }
    }

    override fun selectGroup(id: String) {
        update { it.copy(selectedGroupId = id) }
    }

    override fun selectEquipment(id: String) {
        update {
            val newList: PersistentList<String> = it.selectedEquipmentIds
                .toMutableList()
                .apply { if (contains(id)) remove(id) else add(id) }
                .toPersistentList()

            it.copy(selectedEquipmentIds = newList)
        }
    }

    override fun next() {
        val formattedList = state.value.suggestions
            .flatMap { it.equipments }
            .map { it.id } - state.value.selectedEquipmentIds

        val direction = MissingEquipmentsDirection.Completed(
            missingEquipmentIds = formattedList
        )
        navigateTo(direction)
    }

    override fun back() {
        navigateTo(MissingEquipmentsDirection.Back)
    }
}