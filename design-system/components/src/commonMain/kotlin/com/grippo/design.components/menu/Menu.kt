package com.grippo.design.components.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.grippo.design.components.modifiers.ShadowElevation
import com.grippo.design.components.modifiers.shadowDefault
import com.grippo.design.core.AppTokens
import com.grippo.design.core.UiText
import kotlinx.collections.immutable.ImmutableList

@Immutable
public data class MenuItem(
    val title: UiText,
    val icon: ImageVector
)

@Composable
public fun <KEY> Menu(
    modifier: Modifier = Modifier,
    items: ImmutableList<Pair<KEY, MenuItem>>,
    onClick: (KEY) -> Unit
) {
    val shape = RoundedCornerShape(AppTokens.dp.menu.radius)

    Column(
        modifier = Modifier
            .shadowDefault(
                elevation = ShadowElevation.Card,
                shape = shape,
                color = AppTokens.colors.overlay.defaultShadow
            )
            .fillMaxWidth()
            .clip(shape = shape)
            .background(AppTokens.colors.background.secondary)
            .border(1.dp, AppTokens.colors.border.defaultPrimary, shape)
    ) {

        items.forEachIndexed { index, item ->
            val onClickProvider = remember(item) { { onClick.invoke(item.first) } }

            MenuCard(
                modifier = Modifier.fillMaxWidth(),
                title = item.second.title.text(),
                icon = item.second.icon,
                onClick = onClickProvider
            )

            if (index < items.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier
                        .padding(horizontal = AppTokens.dp.menu.item.horizontalPadding)
                        .fillMaxWidth(),
                    color = AppTokens.colors.divider.primary
                )
            }
        }
    }
}