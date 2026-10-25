package com.grippo.design.components.skeleton

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.components.utils.rememberTextHeight
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Immutable
public sealed interface SkeletonPanelStyle {
    @Immutable
    public data object Small : SkeletonPanelStyle

    @Immutable
    public data object Large : SkeletonPanelStyle
}

@Composable
public fun SkeletonPanel(
    modifier: Modifier = Modifier,
    contentHeight: Dp,
    style: SkeletonPanelStyle = SkeletonPanelStyle.Large,
) {
    MetricSectionPanel(
        modifier = modifier,
        style = when (style) {
            SkeletonPanelStyle.Small -> MetricSectionPanelStyle.Small
            SkeletonPanelStyle.Large -> MetricSectionPanelStyle.Large
        },
    ) {
        SkeletonBone(
            modifier = Modifier
                .fillMaxWidth()
                .height(contentHeight),
        )
    }
}

@AppPreview
@Composable
private fun SkeletonPanelLargePreview() {
    PreviewContainer {
        val textMeasurer = rememberTextMeasurer()
        val titleLine = rememberTextHeight(style = AppTokens.typography.h5(), textMeasurer = textMeasurer)
        val captionLine = rememberTextHeight(style = AppTokens.typography.b12Med(), textMeasurer = textMeasurer)

        SkeletonPanel(
            contentHeight = titleLine + captionLine * 3 + AppTokens.dp.metrics.panel.large.spacer * 3,
            style = SkeletonPanelStyle.Large,
        )
    }
}

@AppPreview
@Composable
private fun SkeletonPanelSmallPreview() {
    PreviewContainer {
        val textMeasurer = rememberTextMeasurer()
        val titleLine = rememberTextHeight(style = AppTokens.typography.h5(), textMeasurer = textMeasurer)
        val valueLine = rememberTextHeight(style = AppTokens.typography.h2(), textMeasurer = textMeasurer)
        val captionLine = rememberTextHeight(style = AppTokens.typography.b12Med(), textMeasurer = textMeasurer)

        SkeletonPanel(
            contentHeight = titleLine + valueLine + captionLine + AppTokens.dp.metrics.panel.small.spacer * 2,
            style = SkeletonPanelStyle.Small,
        )
    }
}
