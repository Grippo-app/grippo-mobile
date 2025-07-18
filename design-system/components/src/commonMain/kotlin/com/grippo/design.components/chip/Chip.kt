package com.grippo.design.components.chip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.grippo.design.components.modifiers.border
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.icons.Weight

@Composable
public fun Chip(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    backgroundColor: Color,
    contentColor: Color,
    borderColor: Color = Color.Transparent,
) {
    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(AppTokens.dp.chip.radius)
            )
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(AppTokens.dp.chip.radius)
            )
            .padding(
                horizontal = AppTokens.dp.chip.horizontalPadding,
                vertical = AppTokens.dp.chip.verticalPadding
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(AppTokens.dp.chip.iconSize),
                imageVector = icon,
                contentDescription = null,
                tint = contentColor
            )

            Spacer(modifier = Modifier.width(AppTokens.dp.chip.spaceBetween))

            Text(
                text = "$label: $value",
                style = AppTokens.typography.b14Semi(),
                color = contentColor
            )
        }
    }
}

@AppPreview
@Composable
private fun ChipPreview() {
    PreviewContainer {
        Chip(
            label = "Basic",
            value = "Value",
            icon = AppTokens.icons.Weight,
            backgroundColor = Color.LightGray.copy(alpha = 0.3f),
            contentColor = Color.Black
        )
    }
}
