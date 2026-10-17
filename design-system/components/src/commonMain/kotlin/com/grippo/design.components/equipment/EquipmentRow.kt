package com.grippo.design.components.equipment

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.equipments.EquipmentEnumState
import com.grippo.core.state.equipments.EquipmentState
import com.grippo.design.components.cards.selectable.ToggleSelectableCard
import com.grippo.design.components.cards.selectable.ToggleSelectableCardStyle
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun EquipmentRow(
    modifier: Modifier = Modifier,
    equipment: EquipmentState,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    ToggleSelectableCard(
        modifier = modifier.fillMaxWidth(),
        style = ToggleSelectableCardStyle.Medium(
            title = equipment.type.title().text(),
            icon = equipment.image(),
        ),
        isSelected = isSelected,
        onSelect = onSelect
    )
}

@AppPreview
@Composable
private fun EquipmentRowPreview() {
    PreviewContainer {
        val equipments = listOf(
            EquipmentState(id = "rope", type = EquipmentEnumState.ROPE),
            EquipmentState(id = "cord-handles", type = EquipmentEnumState.CORD_HANDLES),
        )
        equipments.forEach { equipment ->
            EquipmentRow(
                equipment = equipment,
                isSelected = true,
                onSelect = {},
            )

            EquipmentRow(
                equipment = equipment,
                isSelected = false,
                onSelect = {},
            )
        }
    }
}
