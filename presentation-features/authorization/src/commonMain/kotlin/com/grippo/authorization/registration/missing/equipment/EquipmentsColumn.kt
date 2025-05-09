package com.grippo.authorization.registration.missing.equipment

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import com.grippo.design.components.cards.SelectableCard
import com.grippo.design.components.cards.SelectableCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.presentation.api.equipment.models.EquipmentGroupState
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun EquipmentsColumn(
    modifier: Modifier = Modifier,
    item: EquipmentGroupState,
    selectedIds: ImmutableList<String>,
    onSelect: (id: String) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item.equipments.forEach { equipment ->
            val selectProvider = remember(equipment.id) {
                { onSelect.invoke(equipment.id) }
            }

            val isSelected = remember(equipment.id, selectedIds) {
                selectedIds.contains(equipment.id)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Image(
                    modifier = Modifier.size(AppTokens.dp.size.componentHeight),
                    imageVector = equipment.image(),
                    contentDescription = null,
                    colorFilter = when (isSelected) {
                        true -> null
                        false -> ColorFilter.tint(color = AppTokens.colors.equipment.inactive)
                    },
                )

                SelectableCard(
                    modifier = Modifier.fillMaxWidth(),
                    style = SelectableCardStyle.Small(
                        title = equipment.name
                    ),
                    isSelected = isSelected,
                    onSelect = selectProvider
                )
            }
        }
    }
}