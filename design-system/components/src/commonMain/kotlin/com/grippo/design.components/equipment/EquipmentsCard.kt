package com.grippo.design.components.equipment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.core.state.equipments.EquipmentEnumState
import com.grippo.core.state.equipments.EquipmentState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
public fun EquipmentsCard(
    modifier: Modifier = Modifier,
    value: ImmutableList<EquipmentState>,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(
            AppTokens.dp.contentPadding.block,
            Alignment.CenterHorizontally
        ),
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
            modifier = Modifier.fillMaxWidth(),
            value = persistentListOf(
                EquipmentState(id = "barbell", type = EquipmentEnumState.BARBELL),
                EquipmentState(id = "smith", type = EquipmentEnumState.SMITH_MACHINES),
                EquipmentState(id = "bench", type = EquipmentEnumState.DECLINE_BENCH_WITH_RACK),
            )
        )
    }
}
