package com.grippo.design.components.metrics.volume

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.chart.bar.BarData
import com.grippo.chart.bar.BarEntry
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.volume.VolumeSeriesState
import com.grippo.core.state.metrics.volume.stubVolumeSeries
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
    modifier: Modifier = Modifier,
    value: VolumeSeriesState,
    xAxisLabels: BarChartXAxisLabels
) {
    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Large,
    ) {
        Text(
            text = AppTokens.strings.res(Res.string.tonnage),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        BarChart(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AppTokens.dp.metrics.volume.chart.height)
                .aspectRatio(AppTokens.dp.metrics.volume.chart.aspectRatio),
            data = value.toBarData(),
            valueFormatter = { volume -> VolumeFormatState.shortTotal(volume) },
            xAxisLabels = xAxisLabels,
        )
    }
}

@Composable
private fun VolumeSeriesState.toBarData(): BarData {
    val color = AppTokens.colors.palette.palette7BlueGrowth[2]
    return remember(entries, color) {
        BarData(
            items = entries.map { entry ->
                BarEntry(label = entry.label, value = entry.value, color = color)
            },
        )
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
