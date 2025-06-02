package com.grippo.design.components.tab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.grippo.design.components.modifiers.nonRippleClick
import com.grippo.design.components.modifiers.shimmerAnimation
import com.grippo.design.core.AppTokens
import com.grippo.design.core.UiText
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.icons.Box
import com.grippo.design.resources.icons.Play
import com.grippo.design.resources.icons.Settings
import com.grippo.segment.control.SegmentBox
import com.grippo.segment.control.SegmentSizing
import com.grippo.segment.control.SegmentedFrame
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
    SegmentedFrame(
        modifier = modifier,
        segmentSizing = SegmentSizing.EqualFill,
        thumb = {
            HorizontalDivider(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
                color = AppTokens.colors.divider.accent,
                thickness = 2.dp,
            )
        },
        content = {
            items.forEach { item ->

                val clickProvider = remember(item.first) { { onSelect.invoke(item.first) } }

                SegmentBox(
                    selected = item.first == selected,
                    content = {
                        Column(
                            modifier = Modifier
                                .nonRippleClick(onClick = clickProvider)
                                .padding(
                                    horizontal = AppTokens.dp.tab.horizontalPadding,
                                    vertical = AppTokens.dp.tab.verticalPadding,
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.tab.padding)
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

                            Text(
                                text = item.second.text.text(),
                                style = if (item.first == selected) {
                                    AppTokens.typography.b13Bold()
                                } else {
                                    AppTokens.typography.b13Semi()
                                },
                                color = if (item.first == selected) {
                                    AppTokens.colors.segment.active
                                } else {
                                    AppTokens.colors.segment.inactive
                                },
                            )
                        }
                    },
                )
            }
        }
    )
}

@Composable
public fun SegmentSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .shimmerAnimation(
                        visible = true,
                        radius = AppTokens.dp.segment.radius
                    )
                    .height(AppTokens.dp.segment.height)
                    .width(140.dp)
            )
        }
    }
}

@AppPreview
@Composable
private fun SegmentPreview() {
    PreviewContainer {
        Tab(
            modifier = Modifier,
            items = persistentListOf<Pair<String, TabItem>>(
                "Box" to TabItem(text = UiText.Str("Box"), icon = AppTokens.icons.Box),
                "Play" to TabItem(text = UiText.Str("Play"), icon = AppTokens.icons.Play),
                "Settings" to TabItem(
                    text = UiText.Str("Settings"),
                    icon = AppTokens.icons.Settings
                ),
            ),
            selected = "Play",
            onSelect = {}
        )
    }
}