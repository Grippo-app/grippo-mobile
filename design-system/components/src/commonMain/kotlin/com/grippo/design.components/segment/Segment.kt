package com.grippo.design.components.segment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.segment.control.SegmentBox
import com.grippo.segment.control.SegmentSizing
import com.grippo.segment.control.SegmentedFrame
import com.grippo.state.formatters.UiText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public enum class SegmentWidth {
    Unspecified,
    EqualFill,
}

@Immutable
public enum class SegmentStyle {
    Outline,
    Fill
}

@Composable
public fun <KEY> Segment(
    modifier: Modifier = Modifier,
    items: ImmutableList<Pair<KEY, UiText>>,
    style: SegmentStyle,
    selected: KEY?,
    onSelect: (KEY) -> Unit,
    segmentWidth: SegmentWidth = SegmentWidth.Unspecified,
) {

    when (style) {
        SegmentStyle.Outline -> SegmentedFrame(
            modifier = modifier,
            segmentSizing = when (segmentWidth) {
                SegmentWidth.Unspecified -> SegmentSizing.Unspecified
                SegmentWidth.EqualFill -> SegmentSizing.EqualFill
            },
            thumb = {
                HorizontalDivider(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    color = AppTokens.colors.segment.selector,
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
                                    .scalableClick(onClick = clickProvider)
                                    .padding(horizontal = AppTokens.dp.segment.outline.horizontalPadding)
                                    .height(AppTokens.dp.segment.outline.height)
                                    .wrapContentHeight(),
                                text = item.second.text(),
                                style = if (item.first == selected) {
                                    AppTokens.typography.b14Bold()
                                } else {
                                    AppTokens.typography.b14Med()
                                },
                                color = if (item.first == selected) {
                                    AppTokens.colors.text.primary
                                } else {
                                    AppTokens.colors.text.tertiary
                                },
                            )
                        },
                    )
                }
            }
        )

        SegmentStyle.Fill -> SegmentedFrame(
            modifier = modifier.background(
                AppTokens.colors.background.dialog,
                RoundedCornerShape(AppTokens.dp.segment.fill.radius)
            ),
            segmentSizing = when (segmentWidth) {
                SegmentWidth.Unspecified -> SegmentSizing.Unspecified
                SegmentWidth.EqualFill -> SegmentSizing.EqualFill
            },
            thumb = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            AppTokens.colors.background.card,
                            RoundedCornerShape(AppTokens.dp.segment.fill.radius)
                        ),
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
                                    .scalableClick(onClick = clickProvider)
                                    .padding(
                                        horizontal = AppTokens.dp.segment.fill.horizontalPadding,
                                    ).height(
                                        AppTokens.dp.segment.fill.height
                                    )
                                    .wrapContentHeight(),
                                text = item.second.text(),
                                style = if (item.first == selected) {
                                    AppTokens.typography.b14Bold()
                                } else {
                                    AppTokens.typography.b14Med()
                                },
                                color = if (item.first == selected) {
                                    AppTokens.colors.text.primary
                                } else {
                                    AppTokens.colors.text.tertiary
                                },
                            )
                        },
                    )
                }
            }
        )
    }
}

@AppPreview
@Composable
private fun SegmentCirclePreview() {
    PreviewContainer {
        Segment(
            items = persistentListOf<Pair<String, UiText>>(
                "Profile" to UiText.Str("Profile"),
                "Home" to UiText.Str("Home"),
                "Dashboard" to UiText.Str("Dashboard"),
            ),
            selected = "Profile",
            onSelect = {},
            style = SegmentStyle.Fill
        )

        Segment(
            items = persistentListOf<Pair<String, UiText>>(
                "Profile" to UiText.Str("Profile"),
                "Home" to UiText.Str("Home"),
                "Dashboard" to UiText.Str("Dashboard"),
            ),
            selected = "Home",
            onSelect = {},
            style = SegmentStyle.Fill
        )
    }
}

@AppPreview
@Composable
private fun SegmentSquarePreview() {
    PreviewContainer {
        Segment(
            items = persistentListOf<Pair<String, UiText>>(
                "Profile" to UiText.Str("Profile"),
                "Home" to UiText.Str("Home"),
                "Dashboard" to UiText.Str("Dashboard"),
            ),
            selected = "Profile",
            onSelect = {},
            style = SegmentStyle.Outline
        )

        Segment(
            items = persistentListOf<Pair<String, UiText>>(
                "Profile" to UiText.Str("Profile"),
                "Home" to UiText.Str("Home"),
                "Dashboard" to UiText.Str("Dashboard"),
            ),
            selected = "Home",
            onSelect = {},
            style = SegmentStyle.Outline
        )
    }
}