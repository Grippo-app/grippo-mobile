package com.grippo.exercise.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.unit.Dp
import com.grippo.design.components.modifiers.shimmer
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.exercise_history_comparison_loading

@Composable
internal fun ExerciseHistoryShimmer(
    modifier: Modifier = Modifier,
    captionMinHeight: Dp,
    chartHeight: Dp,
) {
    val loadingDescription = AppTokens.strings.res(Res.string.exercise_history_comparison_loading)
    val shape = RoundedCornerShape(AppTokens.dp.metrics.panel.small.radius)

    Column(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = loadingDescription
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        },
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(captionMinHeight),
            contentAlignment = Alignment.TopStart,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(captionMinHeight / 2)
                    .clip(shape)
                    .background(AppTokens.colors.divider.default)
                    .shimmer(),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .clip(shape)
                .background(AppTokens.colors.divider.default)
                .shimmer(),
        )
    }
}

@AppPreview
@Composable
private fun ExerciseHistoryShimmerPreview() {
    PreviewContainer {
        val captionStyle = AppTokens.typography.b12Med()
        val captionMinHeight = with(LocalDensity.current) { captionStyle.lineHeight.toDp() * 2 }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            ExerciseHistoryShimmer(
                captionMinHeight = captionMinHeight,
                chartHeight = maxWidth / AppTokens.dp.metrics.volume.chart.historyAspectRatio,
            )
        }
    }
}
