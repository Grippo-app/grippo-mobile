package com.grippo.design.components.tab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.core.state.formatters.UiText
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.NavArrowDown
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class TabItem(
    val text: UiText,
    val icon: ImageVector
)

@Composable
public fun <KEY> Tab(
    modifier: Modifier = Modifier,
    items: ImmutableList<Pair<KEY, TabItem>>,
    selected: KEY?,
    onSelect: (KEY) -> Unit,
) {

    Row(modifier = modifier) {
        items.forEach { item ->

            val clickProvider = remember(item.first) { { onSelect.invoke(item.first) } }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .scalableClick(onClick = clickProvider)
                    .animateContentSize()
                    .padding(
                        horizontal = AppTokens.dp.tab.horizontalPadding,
                        vertical = AppTokens.dp.tab.verticalPadding,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {

                Icon(
                    modifier = Modifier.size(AppTokens.dp.tab.icon),
                    imageVector = item.second.icon,
                    tint = if (item.first == selected) {
                        AppTokens.colors.segment.active
                    } else {
                        AppTokens.colors.segment.inactive
                    },
                    contentDescription = null
                )

                AnimatedVisibility(
                    visible = item.first == selected,
                    enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                    exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start),
                ) {
                    Text(
                        modifier = Modifier.padding(start = AppTokens.dp.contentPadding.text),
                        text = item.second.text.text(),
                        style = AppTokens.typography.b13Bold(),
                        color = AppTokens.colors.segment.active,
                    )
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun SegmentPreview() {
    PreviewContainer {
        Tab(
            items = persistentListOf(
                "Box" to TabItem(
                    text = UiText.Str("Box"),
                    icon = AppTokens.icons.NavArrowDown
                ),
                "Play" to TabItem(
                    text = UiText.Str("Play"),
                    icon = AppTokens.icons.NavArrowDown
                ),
                "Settings" to TabItem(
                    text = UiText.Str("Settings"),
                    icon = AppTokens.icons.NavArrowDown
                ),
            ),
            selected = "Play",
            onSelect = {}
        )
    }
}