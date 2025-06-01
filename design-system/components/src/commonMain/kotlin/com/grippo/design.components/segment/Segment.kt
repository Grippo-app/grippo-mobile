package com.grippo.design.components.segment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.design.components.modifiers.nonRippleClick
import com.grippo.design.components.modifiers.shimmerAnimation
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.segment.control.SegmentBox
import com.grippo.segment.control.SegmentSizing
import com.grippo.segment.control.SegmentedFrame
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public enum class SegmentWidth {
    Unspecified,
    EqualFill,
}

@Immutable
public enum class ThumbPosition {
    Bottom,
    Top,
}

@Composable
public fun <KEY> Segment(
    modifier: Modifier = Modifier,
    items: ImmutableList<Pair<KEY, String>>,
    selected: KEY?,
    onSelect: (KEY) -> Unit,
    segmentWidth: SegmentWidth = SegmentWidth.Unspecified,
    thumbPosition: ThumbPosition = ThumbPosition.Bottom,
) {
    SegmentedFrame(
        modifier = modifier,
        segmentSizing = when (segmentWidth) {
            SegmentWidth.Unspecified -> SegmentSizing.Unspecified
            SegmentWidth.EqualFill -> SegmentSizing.EqualFill
        },
        thumb = {
            HorizontalDivider(
                modifier = Modifier
                    .align(
                        when (thumbPosition) {
                            ThumbPosition.Bottom -> Alignment.BottomCenter
                            ThumbPosition.Top -> Alignment.TopCenter
                        }
                    )
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
                                .padding(horizontal = AppTokens.dp.segment.horizontalPadding)
                                .height(AppTokens.dp.segment.height)
                                .wrapContentHeight()
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
        Segment(
            modifier = Modifier,
            items = persistentListOf<Pair<String, String>>(
                "Profile" to "Profile",
                "Home" to "Home",
                "Dashboard" to "Dashboard",
            ),
            selected = "Profile",
            onSelect = {}
        )

        Segment(
            modifier = Modifier,
            items = persistentListOf<Pair<String, String>>(
                "Profile" to "Profile",
                "Home" to "Home",
                "Dashboard" to "Dashboard",
            ),
            selected = "Home",
            onSelect = {}
        )
    }
}