package com.grippo.design.components.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.chart.sparkline.SparklineData
import com.grippo.chart.sparkline.SparklinePoint
import com.grippo.core.state.metrics.EstimatedOneRepMaxState
import com.grippo.core.state.metrics.stubEstimatedOneRepMax
import com.grippo.design.components.chart.internal.Sparkline
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.estimated_one_rm
import com.grippo.design.resources.provider.estimated_one_rm_peak
import com.grippo.design.resources.provider.kg
import kotlin.math.roundToInt

@Composable
public fun EstimatedOneRepMaxCard(
    state: EstimatedOneRepMaxState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
    ) {
        val title = AppTokens.strings.res(Res.string.estimated_one_rm)
        val kg = AppTokens.strings.res(Res.string.kg)
        val latest = state.entries.lastOrNull()?.value ?: 0f
        val peak = state.entries.maxOfOrNull { it.value } ?: latest
        val peakLabel = AppTokens.strings.res(
            Res.string.estimated_one_rm_peak,
            "${peak.roundToInt()} $kg"
        )

        Text(
            text = title,
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary
        )

        Text(
            text = "${latest.roundToInt()} $kg",
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary
        )

        Text(
            text = peakLabel,
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.tertiary
        )

        val sparklineData = remember(state.entries) {
            val points = state.entries.mapIndexed { index, entry ->
                SparklinePoint(index.toFloat(), entry.value)
            }
            SparklineData(points = points)
        }

        Sparkline(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTokens.dp.metrics.strength.height),
            data = sparklineData
        )
    }
}

@AppPreview
@Composable
private fun EstimatedOneRepMaxCardPreview() {
    PreviewContainer {
        EstimatedOneRepMaxCard(
            state = stubEstimatedOneRepMax()
        )
    }
}
