package com.grippo.performance.trend.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import com.grippo.design.components.divider.ContentDivider
import com.grippo.design.components.skeleton.SkeletonBone
import com.grippo.design.components.skeleton.SkeletonPanel
import com.grippo.design.components.skeleton.SkeletonPanelStyle
import com.grippo.design.components.skeleton.SkeletonText
import com.grippo.design.components.utils.rememberTextHeight
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.loading

private const val HISTORY_ROW_COUNT = 3

@Composable
internal fun PerformanceTrendDetailsSkeleton(modifier: Modifier = Modifier) {
    val loadingDescription = AppTokens.strings.res(Res.string.loading)
    val textMeasurer = rememberTextMeasurer()
    val valueLine = rememberTextHeight(style = AppTokens.typography.h4(), textMeasurer = textMeasurer)
    val captionLine = rememberTextHeight(style = AppTokens.typography.b12Med(), textMeasurer = textMeasurer)
    val metricHeight = maxOf(AppTokens.dp.metrics.performance.trend.icon, valueLine) + captionLine * 2 +
        AppTokens.dp.metrics.panel.small.spacer * 2

    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = loadingDescription
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        ) {
            PerformanceTrendLineSkeleton(
                startStyle = AppTokens.typography.b14Semi(),
                endStyle = AppTokens.typography.b12Med(),
                textMeasurer = textMeasurer,
            )
            SkeletonPanel(
                modifier = Modifier.fillMaxWidth(),
                contentHeight = metricHeight,
                style = SkeletonPanelStyle.Small,
            )
        }
        SkeletonText(
            modifier = Modifier
                .padding(top = AppTokens.dp.contentPadding.block)
                .fillMaxWidth(AppTokens.dp.skeleton.line.medium),
            style = AppTokens.typography.h4(),
            textMeasurer = textMeasurer,
        )
        SkeletonBone(
            modifier = Modifier
                .padding(top = AppTokens.dp.contentPadding.content)
                .fillMaxWidth()
                .aspectRatio(AppTokens.dp.metrics.performance.trend.historyAspectRatio),
        )
        repeat(HISTORY_ROW_COUNT) { index ->
            PerformanceTrendLineSkeleton(
                modifier = Modifier.padding(vertical = AppTokens.dp.contentPadding.subContent),
                startStyle = AppTokens.typography.b13Med(),
                endStyle = AppTokens.typography.b14Semi(),
                textMeasurer = textMeasurer,
            )
            if (index < HISTORY_ROW_COUNT - 1) {
                ContentDivider()
            }
        }
        SkeletonBone(
            modifier = Modifier
                .padding(top = AppTokens.dp.contentPadding.block)
                .fillMaxWidth()
                .height(AppTokens.dp.button.medium.height),
            shape = RoundedCornerShape(AppTokens.dp.button.medium.radius),
            animated = false,
        )
    }
}

@Composable
private fun PerformanceTrendLineSkeleton(
    modifier: Modifier = Modifier,
    startStyle: TextStyle,
    endStyle: TextStyle,
    textMeasurer: TextMeasurer,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            SkeletonText(
                modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.medium),
                style = startStyle,
                textMeasurer = textMeasurer,
            )
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd,
        ) {
            SkeletonText(
                modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.short),
                style = endStyle,
                textMeasurer = textMeasurer,
            )
        }
    }
}

@AppPreview
@Composable
private fun PerformanceTrendDetailsSkeletonPreview() {
    PreviewContainer {
        PerformanceTrendDetailsSkeleton(modifier = Modifier.fillMaxWidth())
    }
}
