package com.grippo.design.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.state.formatters.UiText
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.calendar
import com.grippo.design.resources.provider.home
import com.grippo.design.resources.provider.icons.Calendar
import com.grippo.design.resources.provider.icons.Home
import com.grippo.design.resources.provider.icons.UserOutline
import com.grippo.design.resources.provider.profile
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class BottomBarItem(
    val title: UiText,
    val icon: ImageVector,
)

@Composable
public fun <KEY> BottomBar(
    items: ImmutableList<Pair<KEY, BottomBarItem>>,
    selected: KEY,
    onSelect: (KEY) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dp = AppTokens.dp.bottomBar
    Column(
        modifier.background(AppTokens.colors.background.card)) {
        HorizontalDivider(thickness = dp.borderWidth, color = AppTokens.colors.divider.default)
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = dp.horizontalPadding).selectableGroup(),
        ) {
            items.forEach { (key, item) ->
                val active = selected == key
                val color = if (active) AppTokens.colors.bottomBar.activeContent
                else AppTokens.colors.bottomBar.inactiveContent
                Column(
                    modifier = Modifier.weight(1f).heightIn(min = dp.minHeight)
                        .semantics { this.selected = active }
                        .scalableClick(role = Role.Tab, onClick = { onSelect(key) })
                        .padding(vertical = dp.verticalPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(dp.gap, Alignment.CenterVertically),
                ) {
                    Icon(
                        modifier = Modifier.size(dp.iconSize),
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = color,
                    )
                    Text(
                        text = item.title.text(),
                        style = AppTokens.typography.b10Med(),
                        color = color,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun BottomBarPreview() {
    PreviewContainer {
        Column {
            val items = persistentListOf(
                0 to BottomBarItem(UiText.Res(Res.string.home), AppTokens.icons.Home),
                1 to BottomBarItem(UiText.Res(Res.string.calendar), AppTokens.icons.Calendar),
                2 to BottomBarItem(UiText.Res(Res.string.profile), AppTokens.icons.UserOutline),
            )
            items.forEach { (key, _) -> BottomBar(items = items, selected = key, onSelect = {}) }
        }
    }
}
