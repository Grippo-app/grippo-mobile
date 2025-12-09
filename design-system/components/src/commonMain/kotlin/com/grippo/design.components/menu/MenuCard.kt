package com.grippo.design.components.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.ArrowRight
import com.grippo.design.resources.provider.icons.User

@Stable
public sealed interface MenuTrailing {
    @Stable
    public data class Icon(
        val icon: ImageVector,
    ) : MenuTrailing

    @Stable
    public data object Empty : MenuTrailing
}

@Composable
public fun MenuCard(
    modifier: Modifier = Modifier,
    title: String,
    trailing: MenuTrailing,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .scalableClick(onClick = onClick)
            .padding(
                vertical = AppTokens.dp.menu.item.verticalPadding,
                horizontal = AppTokens.dp.menu.item.horizontalPadding
            ),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        verticalAlignment = Alignment.CenterVertically
    ) {

        when (trailing) {
            MenuTrailing.Empty -> {
                // Empty
            }

            is MenuTrailing.Icon -> {
                Icon(
                    modifier = Modifier.size(AppTokens.dp.menu.item.icon),
                    imageVector = trailing.icon,
                    tint = AppTokens.colors.icon.primary,
                    contentDescription = null
                )
            }
        }

        Text(
            modifier = Modifier.weight(1f),
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = AppTokens.typography.b14Bold(),
            color = AppTokens.colors.text.primary
        )

        Icon(
            modifier = Modifier.size(AppTokens.dp.menu.item.icon),
            imageVector = AppTokens.icons.ArrowRight,
            tint = AppTokens.colors.icon.secondary,
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
            trailing = MenuTrailing.Icon(AppTokens.icons.User),
            onClick = {}
        )

        MenuCard(
            title = "User Settings",
            trailing = MenuTrailing.Empty,
            onClick = {}
        )
    }
}