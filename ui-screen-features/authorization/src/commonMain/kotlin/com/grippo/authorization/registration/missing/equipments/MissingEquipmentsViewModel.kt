package com.grippo.authorization.registration.missing.equipments

import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.equipment.EquipmentFeature
import com.grippo.data.features.api.equipment.models.EquipmentGroup
import com.grippo.domain.state.equipment.toState
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
        val previous = state.value

        val previousKnownIds = previous.suggestions
            .flatMap { it.equipments }
            .map { it.id }
            .toSet()
        val allIds = suggestions.flatMap { it.equipments }.map { it.id }

        val retainedSet = previous.selectedEquipmentIds
            .filter { it in allIds }
            .toSet()
        val newlyDiscovered = allIds
            .filter { it !in previousKnownIds }
            .toSet()

        val selectedIds = allIds
            .filter { it in retainedSet || it in newlyDiscovered }
            .toPersistentList()
            .ifEmpty { allIds.toPersistentList() }

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

    override fun onGroupClick(id: String) {
        update { it.copy(selectedGroupId = id) }
    }

    override fun onEquipmentClick(id: String) {
        update {
            val newList: PersistentList<String> = it.selectedEquipmentIds
                .toMutableList()
                .apply { if (contains(id)) remove(id) else add(id) }
                .toPersistentList()

            it.copy(selectedEquipmentIds = newList)
        }
    }

    override fun onNextClick() {
        val formattedList = state.value.suggestions
            .flatMap { it.equipments }
            .map { it.id } - state.value.selectedEquipmentIds

        val direction = MissingEquipmentsDirection.Completed(
            missingEquipmentIds = formattedList
        )
        navigateTo(direction)
    }

    override fun onBack() {
        navigateTo(MissingEquipmentsDirection.Back)
    }
}
