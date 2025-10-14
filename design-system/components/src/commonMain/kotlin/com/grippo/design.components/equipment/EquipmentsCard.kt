package com.grippo.design.components.equipment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.core.state.equipments.EquipmentState
import com.grippo.core.state.equipments.stubEquipments
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Composable
public fun EquipmentsCard(
    modifier: Modifier = Modifier,
    value: ImmutableList<EquipmentState>,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        contentPadding = contentPadding
    ) {
        items(items = value, key = { it.id }) { item ->
            EquipmentCard(
                value = item
            )
        }
    }
}

@AppPreview
@Composable
private fun EquipmentsCardPreview() {
    PreviewContainer {
        EquipmentsCard(
            value = stubEquipments()
                .shuffled()
                .flatMap { it.equipments }
                .take(3)
                .toPersistentList()
        )
    }
}