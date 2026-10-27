package com.grippo.design.components.datetime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.grippo.design.components.indicators.Dot
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Immutable
public enum class TimelineStyle {
    Start,
    Middle,
    End,
    Single,
    Line,
    Empty
}

@Composable
public fun TimelineIndicator(
    modifier: Modifier = Modifier,
    style: TimelineStyle,
    content: @Composable RowScope.() -> Unit
) {
    val lineColor = AppTokens.colors.divider.default
    val lineWidth = AppTokens.dp.timeline.lineWidth

    Row(
        modifier = modifier.height(intrinsicSize = IntrinsicSize.Min),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(AppTokens.dp.dot.size),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (style) {
                TimelineStyle.Start -> {
                    Spacer(
                        modifier = Modifier
                            .width(lineWidth)
                            .weight(1f)
                    )
                    TimelineMarker()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .width(lineWidth)
                            .background(lineColor)
                    )
                }

                TimelineStyle.Middle -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .width(lineWidth)
                            .background(lineColor)
                    )
                    TimelineMarker()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .width(lineWidth)
                            .background(lineColor)
                    )
                }

                TimelineStyle.End -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .width(lineWidth)
                            .background(lineColor)
                    )
                    TimelineMarker()
                    Spacer(
                        modifier = Modifier
                            .width(lineWidth)
                            .weight(1f)
                    )
                }

                TimelineStyle.Single -> {
                    Spacer(
                        modifier = Modifier
                            .width(lineWidth)
                            .weight(1f)
                    )
                    TimelineMarker()
                    Spacer(
                        modifier = Modifier
                            .width(lineWidth)
                            .weight(1f)
                    )
                }

                TimelineStyle.Line -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .width(lineWidth)
                            .background(lineColor)
                    )
                }

                TimelineStyle.Empty -> {
                    Spacer(
                        modifier = Modifier
                            .width(lineWidth)
                            .weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(AppTokens.dp.contentPadding.subContent))

        CompositionLocalProvider(
            LocalContentColor provides AppTokens.colors.text.primary,
            LocalTextStyle provides AppTokens.typography.b13Med(),
        ) {
            content()
        }
    }
}

@Composable
private fun TimelineMarker() {
    val background = AppTokens.colors.timeline.markerBackground
    val halo = AppTokens.dp.timeline.markerHalo

    Dot(
        modifier = Modifier
            // Keep the halo above both adjacent connectors without changing their geometry.
            .zIndex(1f)
            .drawBehind {
                drawCircle(
                    color = background,
                    radius = size.minDimension / 2f + halo.toPx(),
                )
            },
        color = AppTokens.colors.timeline.marker,
    )
}

@AppPreview
@Composable
private fun TimelineIndicatorPreviews() {
    PreviewContainer {
        Column {
            TimelineStyle.entries.forEach { style ->
                TimelineIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    style = style,
                ) {
                    Text(
                        modifier = Modifier.padding(vertical = AppTokens.dp.contentPadding.subContent),
                        text = style.name,
                    )
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun TimelineIndicatorConnectedRowsPreview() {
    PreviewContainer {
        Column {
            TimelineIndicator(style = TimelineStyle.Start) {
                Text(
                    modifier = Modifier.padding(vertical = AppTokens.dp.contentPadding.subContent),
                    text = "08:00 – 08:45",
                    style = AppTokens.typography.h6(),
                )
            }
            TimelineIndicator(style = TimelineStyle.Line) {
                Text(
                    modifier = Modifier.padding(vertical = AppTokens.dp.contentPadding.subContent),
                    text = "Exercise with a longer localized name\n3 × 12 · 24 kg",
                )
            }
            TimelineIndicator(style = TimelineStyle.Middle) {
                Text(
                    modifier = Modifier.padding(vertical = AppTokens.dp.contentPadding.subContent),
                    text = "12:00 – 12:30",
                    style = AppTokens.typography.h6(),
                )
            }
            TimelineIndicator(style = TimelineStyle.Line) {
                Spacer(Modifier.height(48.dp))
            }
            TimelineIndicator(style = TimelineStyle.End) {
                Text(
                    modifier = Modifier.padding(vertical = AppTokens.dp.contentPadding.subContent),
                    text = "18:00 – 19:00",
                    style = AppTokens.typography.h6(),
                )
            }
            TimelineIndicator(style = TimelineStyle.Empty) {
                Text(
                    modifier = Modifier.padding(vertical = AppTokens.dp.contentPadding.subContent),
                    text = "Last exercise\n3 × 8 · 32 kg",
                )
            }
        }
    }
}
