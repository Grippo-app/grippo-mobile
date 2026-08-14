package com.grippo.design.components.bottombar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Body
import com.grippo.design.resources.provider.icons.Dumbbell
import com.grippo.design.resources.provider.icons.Muscle
import com.grippo.design.resources.provider.icons.Timer
import com.grippo.design.resources.provider.icons.User
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class BottomBarItem(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
)

@Composable
public fun BottomBar(
    items: ImmutableList<BottomBarItem>,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(AppTokens.dp.bottomBar.height + AppTokens.dp.bottomBar.actionSize / 2),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(AppTokens.dp.bottomBar.height)
                .background(AppTokens.colors.bottomBar.background)
                .padding(horizontal = AppTokens.dp.bottomBar.horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            items.forEach { item ->
                BottomBarTab(
                    modifier = Modifier.weight(1f),
                    item = item,
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(AppTokens.dp.bottomBar.actionSize)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            AppTokens.colors.bottomBar.actionBackground1,
                            AppTokens.colors.bottomBar.actionBackground2,
                        )
                    )
                )
                .clickable(onClick = onActionClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(AppTokens.dp.bottomBar.actionIcon),
                imageVector = AppTokens.icons.Dumbbell,
                contentDescription = null,
                tint = AppTokens.colors.bottomBar.actionIcon,
            )
        }
    }
}

@Composable
private fun BottomBarTab(
    modifier: Modifier = Modifier,
    item: BottomBarItem,
) {
    val color = when {
        item.selected -> AppTokens.colors.bottomBar.itemActive
        else -> AppTokens.colors.bottomBar.itemInactive
    }

    Column(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = item.onClick)
            .padding(AppTokens.dp.bottomBar.itemSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            AppTokens.dp.bottomBar.itemSpacing,
            Alignment.CenterVertically,
        ),
    ) {
        Icon(
            modifier = Modifier.size(AppTokens.dp.bottomBar.itemIcon),
            imageVector = item.icon,
            contentDescription = item.label,
            tint = color,
        )

        Text(
            text = item.label,
            style = AppTokens.typography.b11Med(),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@AppPreview
@Composable
private fun BottomBarPreview() {
    PreviewContainer {
        BottomBar(
            items = persistentListOf(
                BottomBarItem(
                    label = "Home",
                    icon = AppTokens.icons.Body,
                    selected = true,
                    onClick = {},
                ),
                BottomBarItem(
                    label = "Muscles",
                    icon = AppTokens.icons.Muscle,
                    selected = false,
                    onClick = {},
                ),
                BottomBarItem(
                    label = "History",
                    icon = AppTokens.icons.Timer,
                    selected = false,
                    onClick = {},
                ),
                BottomBarItem(
                    label = "Profile",
                    icon = AppTokens.icons.User,
                    selected = false,
                    onClick = {},
                ),
            ),
            onActionClick = {},
        )
    }
}
