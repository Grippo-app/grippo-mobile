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
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import com.grippo.design.components.modifiers.shimmer
import com.grippo.design.components.utils.rememberTextHeight
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.exercise_history_comparison_loading

@Composable
internal fun ExerciseHistoryShimmer(
    modifier: Modifier = Modifier,
    captionStyle: TextStyle,
    chartHeight: Dp,
) {
    val loadingDescription = AppTokens.strings.res(Res.string.exercise_history_comparison_loading)
    val shape = RoundedCornerShape(AppTokens.dp.metrics.panel.small.radius)
    val textMeasurer = rememberTextMeasurer()
    val captionLine = rememberTextHeight(style = captionStyle, textMeasurer = textMeasurer)
    val captionMinHeight = rememberTextHeight(style = captionStyle, lines = 2, textMeasurer = textMeasurer)

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
                    .height(captionLine)
                    .clip(shape)
                    .background(AppTokens.colors.divider.default),
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
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            ExerciseHistoryShimmer(
                captionStyle = AppTokens.typography.b12Med(),
                chartHeight = maxWidth / AppTokens.dp.metrics.volume.chart.historyAspectRatio,
            )
        }
    }
}
