package com.grippo.exercise.components

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import com.grippo.design.components.skeleton.SkeletonBone
import com.grippo.design.components.skeleton.SkeletonText
import com.grippo.design.components.utils.rememberTextHeight
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
internal fun ExerciseHistorySkeleton(
    modifier: Modifier = Modifier,
    captionStyle: TextStyle,
    chartHeight: Dp,
) {
    val textMeasurer = rememberTextMeasurer()
    val captionMinHeight = rememberTextHeight(style = captionStyle, lines = 2, textMeasurer = textMeasurer)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(captionMinHeight),
            contentAlignment = Alignment.TopStart,
        ) {
            SkeletonText(
                modifier = Modifier.fillMaxWidth(),
                style = captionStyle,
                textMeasurer = textMeasurer,
            )
        }

        SkeletonBone(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
            shape = RoundedCornerShape(AppTokens.dp.metrics.panel.small.radius),
        )
    }
}

@AppPreview
@Composable
private fun ExerciseHistorySkeletonPreview() {
    PreviewContainer {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            ExerciseHistorySkeleton(
                captionStyle = AppTokens.typography.b12Med(),
                chartHeight = maxWidth / AppTokens.dp.metrics.volume.chart.historyAspectRatio,
            )
        }
    }
}
