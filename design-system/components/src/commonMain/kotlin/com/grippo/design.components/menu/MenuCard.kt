package com.grippo.design.components.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.icons.NavArrowRight
import com.grippo.design.resources.icons.User

@Composable
public fun MenuCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(
                vertical = AppTokens.dp.menu.item.verticalPadding,
                horizontal = AppTokens.dp.menu.item.horizontalPadding
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(AppTokens.dp.menu.item.icon),
            imageVector = icon,
            tint = AppTokens.colors.icon.primary,
            contentDescription = null
        )

        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = AppTokens.typography.b14Bold(),
            color = AppTokens.colors.text.primary
        )

        Icon(
            modifier = Modifier.size(AppTokens.dp.menu.item.icon),
            imageVector = AppTokens.icons.NavArrowRight,
            tint = AppTokens.colors.icon.primary,
            contentDescription = null
        )
    }
}

@AppPreview
@Composable
private fun ProfileScreenEmpty() {
    PreviewContainer {
        MenuCard(
            title = "User Settings",
            icon = AppTokens.icons.User,
            onClick = {}
        )
    }
}