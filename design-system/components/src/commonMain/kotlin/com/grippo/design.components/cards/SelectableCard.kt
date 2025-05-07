package com.grippo.design.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.grippo.design.core.AppTokens

@Composable
public fun SelectableCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    icon: ImageVector?,
    isSelected: Boolean,
    select: () -> Unit
) {

    val iconTint = when (isSelected) {
        true -> AppTokens.colors.icon.accent
        false -> AppTokens.colors.icon.default
    }

    val border = when (isSelected) {
        true -> AppTokens.colors.border.focus
        false -> AppTokens.colors.border.defaultPrimary
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(AppTokens.dp.shape.component))
            .background(color = AppTokens.colors.background.primary)
            .border(
                width = 1.dp,
                color = border,
                shape = RoundedCornerShape(AppTokens.dp.shape.component)
            )
            .padding(
                vertical = AppTokens.dp.paddings.componentVertical,
                horizontal = AppTokens.dp.paddings.componentHorizontal
            ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                modifier = Modifier.size(AppTokens.dp.icon.card),
                imageVector = icon,
                tint = iconTint,
                contentDescription = null
            )
        }

        Text(
            text = title,
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.primary,
        )

        Spacer(modifier = Modifier.size(12.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = description,
            style = AppTokens.typography.b12Semi(),
            color = AppTokens.colors.text.secondary,
        )
    }
}