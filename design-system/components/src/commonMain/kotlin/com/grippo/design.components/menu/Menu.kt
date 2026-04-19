package com.grippo.design.components.menu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.core.state.formatters.UiText
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.User
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class MenuItem(
    val title: UiText,
    val icon: ImageVector,
    val titleColor: Color,
    val iconColor: Color
)

@Composable
public fun <KEY> Menu(
    modifier: Modifier = Modifier,
    items: ImmutableList<Pair<KEY, MenuItem>>,
    onClick: (KEY) -> Unit,
) {
    // Indent the divider so it starts right after the icon column,
    // aligned with the beginning of the text.
    val dividerStartIndent = AppTokens.dp.menu.item.icon +
            AppTokens.dp.contentPadding.subContent

    Column(modifier = modifier) {

        items.forEachIndexed { index, (key, item) ->
            val onClickProvider = remember(key) {
                { onClick.invoke(key) }
            }

            MenuCard(
                modifier = Modifier.fillMaxWidth(),
                title = item.title.text(),
                icon = item.icon,
                titleColor = item.titleColor,
                iconColor = item.iconColor,
                onClick = onClickProvider
            )

            if (index < items.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = dividerStartIndent),
                    color = AppTokens.colors.divider.default
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun MenuPreview() {
    PreviewContainer {
        Menu(
            items = persistentListOf(
                "settings" to MenuItem(
                    title = UiText.Str("Settings"),
                    icon = AppTokens.icons.User,
                    titleColor = AppTokens.colors.text.primary,
                    iconColor = AppTokens.colors.icon.primary,
                ),
                "profile" to MenuItem(
                    title = UiText.Str("Profile"),
                    icon = AppTokens.icons.User,
                    titleColor = AppTokens.colors.text.primary,
                    iconColor = AppTokens.colors.icon.primary,
                ),
                "logout" to MenuItem(
                    title = UiText.Str("Logout"),
                    icon = AppTokens.icons.User,
                    titleColor = AppTokens.colors.semantic.error,
                    iconColor = AppTokens.colors.semantic.error,
                )
            ),
            onClick = {}
        )
    }
}
