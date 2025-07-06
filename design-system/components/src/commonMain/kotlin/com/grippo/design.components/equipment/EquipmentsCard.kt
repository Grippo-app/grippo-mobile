package com.grippo.design.components.equipment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.grippo.design.components.modifiers.ShadowElevation
import com.grippo.design.components.modifiers.shadowDefault
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
    val shape = RoundedCornerShape(AppTokens.dp.equipmentsCard.radius)

    LazyRow(
        modifier = modifier
            .shadowDefault(
                elevation = ShadowElevation.Card,
                shape = shape,
                color = AppTokens.colors.overlay.defaultShadow
            )
            .clip(shape = shape)
            .background(AppTokens.colors.background.secondary)
            .border(1.dp, AppTokens.colors.border.defaultPrimary, shape)
            .padding(
                horizontal = AppTokens.dp.equipmentsCard.horizontalPadding,
                vertical = AppTokens.dp.equipmentsCard.verticalPadding,
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        items(value, key = { it.id }) { item ->
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