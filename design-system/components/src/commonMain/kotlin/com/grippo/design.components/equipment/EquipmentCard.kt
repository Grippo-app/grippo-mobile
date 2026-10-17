package com.grippo.design.components.equipment

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.state.equipments.EquipmentEnumState
import com.grippo.core.state.equipments.EquipmentState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun EquipmentCard(
    modifier: Modifier = Modifier,
    value: EquipmentState
) {
    val shape = RoundedCornerShape(AppTokens.dp.equipmentCard.radius)

    Column(
        modifier = modifier
            .width(AppTokens.dp.equipmentCard.width)
            .clip(shape)
            .background(AppTokens.colors.background.card)
            .border(AppTokens.dp.equipmentCard.borderWidth, AppTokens.colors.border.default, shape)
            .padding(AppTokens.dp.equipmentCard.contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {
        Image(
            modifier = Modifier.size(AppTokens.dp.equipmentCard.icon),
            imageVector = value.image(),
            contentDescription = null
        )

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = value.type.title().text(),
            textAlign = TextAlign.Center,
            style = AppTokens.typography.b14Semi(),
            color = AppTokens.colors.text.primary
        )
    }
}

@AppPreview
@Composable
private fun EquipmentCardPreview() {
    PreviewContainer {
        listOf(
            EquipmentEnumState.BARBELL,
            EquipmentEnumState.SMITH_MACHINES,
            EquipmentEnumState.ROPE,
            EquipmentEnumState.CORD_HANDLES,
            EquipmentEnumState.DECLINE_BENCH_WITH_RACK,
        ).forEach { type ->
            EquipmentCard(value = EquipmentState(id = type.name, type = type))
        }
    }
}

@AppPreview
@Composable
private fun EquipmentCardLongLabelPreview() {
    PreviewContainer {
        EquipmentCard(
            value = EquipmentState(
                id = "decline-bench",
                type = EquipmentEnumState.DECLINE_BENCH_WITH_RACK,
            )
        )
    }
}
