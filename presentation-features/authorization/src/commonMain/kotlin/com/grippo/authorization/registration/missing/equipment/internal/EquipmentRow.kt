package com.grippo.authorization.registration.missing.equipment.internal

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.design.components.cards.selectable.SelectableCard
import com.grippo.design.components.cards.selectable.SelectableCardStyle
import com.grippo.presentation.api.equipment.models.EquipmentState
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun EquipmentRow(
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

    SelectableCard(
        modifier = Modifier.fillMaxWidth(),
        style = SelectableCardStyle.Medium(
            title = equipment.name,
            icon = equipment.image(),
        ),
        isSelected = isSelected,
        onSelect = selectProvider
    )
}