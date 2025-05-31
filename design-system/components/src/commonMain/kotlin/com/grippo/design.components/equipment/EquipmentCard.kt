package com.grippo.design.components.equipment

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.presentation.api.equipment.models.EquipmentState
import com.grippo.presentation.api.equipment.models.stubEquipments

@Composable
public fun EquipmentCard(
    modifier: Modifier = Modifier,
    value: EquipmentState
) {
    Column(
        modifier = modifier.width(intrinsicSize = IntrinsicSize.Max),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Image(
            modifier = Modifier.size(AppTokens.dp.icon.xl),
            imageVector = value.image(),
            contentDescription = null
        )

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = value.name,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = AppTokens.typography.b13Bold(),
            color = AppTokens.colors.text.primary
        )
    }
}

@AppPreview
@Composable
private fun EquipmentCardPreview() {
    PreviewContainer {
        EquipmentCard(
            value = stubEquipments().random().equipments.random()
        )
    }
}