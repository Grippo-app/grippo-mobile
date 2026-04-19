package com.grippo.design.components.indicators

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grippo.design.components.indicators.internal.LineIndicatorBar
import com.grippo.design.components.indicators.internal.toStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.AppColor

@Composable
public fun LineIndicator(
    modifier: Modifier = Modifier,
    progress: Float,
    colors: AppColor.Charts.IndicatorColors.IndicatorColors = AppTokens.colors.charts.indicator.primary,
    barHeight: Dp = 6.dp,
    labelSpacing: Dp = 6.dp,
    startLabel: (@Composable () -> Unit)? = null,
    endLabel: (@Composable () -> Unit)? = null,
    marker: (@Composable () -> Unit)? = null,
) {
    val style = remember(colors) { colors.toStyle() }
    val hasLabels = startLabel != null || endLabel != null

    if (!hasLabels) {
        LineIndicatorBar(
            modifier = modifier,
            progress = progress,
            style = style,
            barHeight = barHeight,
            marker = marker,
        )
        return
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Two fixed slots — SpaceBetween pushes them to the edges even when one is empty.
            Box(contentAlignment = Alignment.CenterStart) {
                startLabel?.invoke()
            }
            Box(contentAlignment = Alignment.CenterEnd) {
                endLabel?.invoke()
            }
        }

        Spacer(Modifier.height(labelSpacing))

        LineIndicatorBar(
            modifier = Modifier.fillMaxWidth(),
            progress = progress,
            style = style,
            barHeight = barHeight,
            marker = marker,
        )
    }
}

@AppPreview
@Composable
private fun LineIndicatorPreview() {
    PreviewContainer {
        LineIndicator(
            progress = 0.6f
        )

        LineIndicator(
            progress = 0.3f,
            colors = AppTokens.colors.charts.indicator.success,
        )

        LineIndicator(
            progress = 0.5f,
            colors = AppTokens.colors.charts.indicator.info,
            barHeight = 10.dp,
            marker = {
                Text(
                    text = "🔥",
                    style = AppTokens.typography.h4(),
                )
            },
        )

        LineIndicator(
            progress = 0.35f,
            marker = {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(AppTokens.colors.charts.indicator.primary.dominant),
                )
            },
        )

        LineIndicator(
            modifier = Modifier.fillMaxWidth(),
            progress = 0.4f,
            colors = AppTokens.colors.charts.indicator.muted,
            startLabel = {
                Text(
                    text = "Apr 1",
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.secondary,
                    maxLines = 1,
                )
            },
            endLabel = {
                Text(
                    text = "May 15",
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.secondary,
                    maxLines = 1,
                )
            },
        )

        LineIndicator(
            modifier = Modifier.fillMaxWidth(),
            progress = 0.72f,
            colors = AppTokens.colors.charts.indicator.warning,
            barHeight = 10.dp,
            startLabel = {
                Text(
                    text = "0 kg",
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.secondary,
                    maxLines = 1,
                )
            },
            endLabel = {
                Text(
                    text = "120 kg",
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.secondary,
                    maxLines = 1,
                )
            },
            marker = {
                Text(
                    text = "🏋️",
                    style = AppTokens.typography.h4().copy(fontSize = 22.sp),
                )
            },
        )
    }
}
