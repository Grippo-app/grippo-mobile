package com.grippo.design.components.equipment

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.design.components.cards.selectable.MultiSelectableCard
import com.grippo.design.components.cards.selectable.MultiSelectableCardStyle
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.state.equipments.EquipmentState
import com.grippo.state.equipments.stubEquipments
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
public fun EquipmentRow(
    equipment: EquipmentState,
    selectedEquipmentIds: ImmutableList<String>,
    selectEquipment: (id: String) -> Unit
) {
    val selectProvider = remember(equipment.id) {
        { selectEquipment(equipment.id) }
    }

    val isSelected = remember(equipment.id, selectedEquipmentIds) {
        selectedEquipmentIds.contains(equipment.id)
    }

    MultiSelectableCard(
        modifier = Modifier.fillMaxWidth(),
        style = MultiSelectableCardStyle.Medium(
            title = equipment.name,
            icon = equipment.image(),
        ),
        isSelected = isSelected,
        onSelect = selectProvider
    )
}

@AppPreview
@Composable
private fun EquipmentRowPreview() {
    PreviewContainer {
        val equip = stubEquipments().random().equipments.random()
        EquipmentRow(
            equipment = equip,
            selectEquipment = {},
            selectedEquipmentIds = persistentListOf(equip.id)
        )

        EquipmentRow(
            equipment = equip,
            selectEquipment = {},
            selectedEquipmentIds = persistentListOf()
        )
    }
}