package com.grippo.design.components.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Immutable
public enum class TimeLinePointStyle {
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
    style: TimeLinePointStyle,
    content: @Composable RowScope.() -> Unit
) {
    val dotColor = AppTokens.colors.semantic.accent
    val lineColor = AppTokens.colors.divider.default
    val lineWidth = 2.dp

    Row(
        modifier = modifier.height(intrinsicSize = IntrinsicSize.Min),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(AppTokens.dp.timeline.dot),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (style) {
                TimeLinePointStyle.Start -> {
                    Spacer(
                        modifier = Modifier
                            .width(lineWidth)
                            .weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(AppTokens.dp.timeline.dot)
                            .background(dotColor, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .width(lineWidth)
                            .background(lineColor)
                    )
                }

                TimeLinePointStyle.Middle -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .width(lineWidth)
                            .background(lineColor)
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(dotColor, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .width(lineWidth)
                            .background(lineColor)
                    )
                }

                TimeLinePointStyle.End -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .width(lineWidth)
                            .background(lineColor)
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(dotColor, CircleShape)
                    )
                    Spacer(
                        modifier = Modifier
                            .width(lineWidth)
                            .weight(1f)
                    )
                }

                TimeLinePointStyle.Single -> {
                    Spacer(
                        modifier = Modifier
                            .width(lineWidth)
                            .weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(dotColor, CircleShape)
                    )
                    Spacer(
                        modifier = Modifier
                            .width(lineWidth)
                            .weight(1f)
                    )
                }

                TimeLinePointStyle.Line -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .width(lineWidth)
                            .background(lineColor)
                    )
                }

                TimeLinePointStyle.Empty -> {
                    Spacer(
                        modifier = Modifier
                            .width(lineWidth)
                            .weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        content()
    }
}

@AppPreview
@Composable
private fun TimelineIndicatorPreviews() {
    PreviewContainer {
        TimeLinePointStyle.entries.forEach { style ->
            TimelineIndicator(
                modifier = Modifier.height(20.dp),
                style = style,
                content = { Text(text = style.name) }
            )
        }
    }
}