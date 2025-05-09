package com.grippo.design.components.segment

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.design.components.modifiers.nonRippleClick
import com.grippo.design.core.AppTokens
import com.grippo.segment.control.SegmentBox
import com.grippo.segment.control.SegmentSizing
import com.grippo.segment.control.SegmentedFrame
import kotlinx.collections.immutable.ImmutableList

@Composable
public fun <KEY> Segment(
    modifier: Modifier = Modifier,
    items: ImmutableList<Pair<KEY, String>>,
    selected: KEY?,
    onSelect: (KEY) -> Unit
) {
    SegmentedFrame(
        modifier = modifier,
        segmentSizing = SegmentSizing.Unspecified,
        thumb = {
            HorizontalDivider(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
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
                        Text(
                            modifier = Modifier
                                .padding(horizontal = 15.dp, vertical = 12.dp)
                                .nonRippleClick(onClick = clickProvider),
                            text = item.second,
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
                    },
                )
            }
        }
    )
}