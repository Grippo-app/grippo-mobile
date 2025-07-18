package com.grippo.design.components.equipment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.presentation.api.equipment.models.EquipmentState
import com.grippo.presentation.api.equipment.models.stubEquipments
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Composable
public fun EquipmentsCard(
    modifier: Modifier = Modifier,
    value: ImmutableList<EquipmentState>
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(
            AppTokens.dp.contentPadding.subContent,
            Alignment.CenterHorizontally
        ),
    ) {
        items(items = value, key = { it.id }, contentType = { it::class }) { item ->
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