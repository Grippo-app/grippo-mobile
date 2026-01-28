package com.grippo.design.components.metrics

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.chart.bar.BarData
import com.grippo.chart.bar.BarEntry
import com.grippo.core.state.metrics.VolumeSeriesState
import com.grippo.core.state.metrics.stubVolumeSeries
import com.grippo.design.components.chart.internal.BarChart
import com.grippo.design.components.chart.internal.BarChartXAxisLabels
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.tonnage

@Composable
public fun VolumeMetricChart(
    value: VolumeSeriesState,
    modifier: Modifier = Modifier,
    xAxisLabels: BarChartXAxisLabels
) {
    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Large,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = AppTokens.strings.res(Res.string.tonnage),
            style = AppTokens.typography.h6(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        BarChart(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AppTokens.dp.metrics.volume.height)
                .aspectRatio(1.7f),
            data = value.toBarData(),
            xAxisLabels = xAxisLabels
        )
    }
}

@Composable
private fun VolumeSeriesState.toBarData(): BarData {
    val palette = AppTokens.colors.palette.palette7BlueGrowth
    val values = entries.map { it.value }
    val minValue = values.minOrNull() ?: 0f
    val maxValue = values.maxOrNull() ?: minValue
    val colors = assignColors(values, palette, minValue, maxValue)

    val items = entries.mapIndexed { index, entry ->
        BarEntry(
            label = entry.label,
            value = entry.value,
            color = colors[index]
        )
    }
    return BarData(items = items)
}

private fun assignColors(
    values: List<Float>,
    palette: List<Color>,
    minValue: Float,
    maxValue: Float,
): List<Color> {
    if (palette.isEmpty() || values.isEmpty()) {
        return List(values.size) { Color.Unspecified }
    }
    val span = (maxValue - minValue).coerceAtLeast(1e-3f)
    val singleColor = span <= 1e-3f

    return values.map { value ->
        if (singleColor) {
            palette.first()
        } else {
            val normalized = ((value - minValue) / span).coerceIn(0f, 1f)
            val index = (normalized * (palette.size - 1)).toInt().coerceIn(0, palette.size - 1)
            palette[index]
        }
    }
}

@AppPreview
@Composable
private fun VolumeMetricChartPreview() {
    PreviewContainer {
        VolumeMetricChart(
            value = stubVolumeSeries(),
            xAxisLabels = BarChartXAxisLabels.WithoutLabels
        )

        VolumeMetricChart(
            value = stubVolumeSeries(),
            xAxisLabels = BarChartXAxisLabels.WithLabels
        )
    }
}
