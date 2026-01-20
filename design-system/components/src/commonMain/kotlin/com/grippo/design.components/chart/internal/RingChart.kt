package com.grippo.design.components.chart.internal

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.chart.ring.RingData
import com.grippo.chart.ring.RingStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.AppColor
import com.grippo.chart.ring.RingChart as CoreRingChart

@Composable
internal fun RingChart(
    modifier: Modifier = Modifier,
    value: Float,
    max: Float,
    colors: AppColor.LineIndicatorColors.IndicatorColors,
) {
    val style = RingStyle(
        strokeWidth = 12.dp,
        trackColor = colors.track,
        indicatorColor = colors.indicator,
        startAngle = -90f,
        sweepAngle = 360f
    )

    CoreRingChart(
        modifier = modifier,
        data = RingData(value = value, min = 0f, max = max),
        style = style
    )
}

@AppPreview
@Composable
private fun RingChartPreview() {
    PreviewContainer {
        RingChart(
            modifier = Modifier.size(120.dp),
            value = 62f,
            max = 100f,
            colors = AppTokens.colors.lineIndicator.info
        )
    }
}
