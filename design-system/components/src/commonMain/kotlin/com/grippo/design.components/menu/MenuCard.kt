package com.grippo.design.components.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.ArrowRight
import com.grippo.design.resources.provider.icons.User

@Composable
public fun MenuCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    titleColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .scalableClick(onClick = onClick)
            .padding(vertical = AppTokens.dp.menu.item.verticalPadding),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(AppTokens.dp.menu.item.icon),
            imageVector = icon,
            tint = titleColor,
            contentDescription = null
        )

        Text(
            modifier = Modifier.weight(1f),
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = AppTokens.typography.b14Semi(),
            color = titleColor
        )

        Icon(
            modifier = Modifier.size(AppTokens.dp.menu.item.icon),
            imageVector = AppTokens.icons.ArrowRight,
            tint = iconColor,
            contentDescription = null
        )
    }
}

@AppPreview
@Composable
private fun MenuCardPreview() {
    PreviewContainer {
        MenuCard(
            title = "User Settings",
            icon = AppTokens.icons.User,
            titleColor = AppTokens.colors.text.primary,
            iconColor = AppTokens.colors.text.primary,
            onClick = {}
        )

        MenuCard(
            title = "Logout",
            icon = AppTokens.icons.User,
            titleColor = AppTokens.colors.semantic.error,
            iconColor = AppTokens.colors.semantic.error,
            onClick = {}
        )
    }
}
