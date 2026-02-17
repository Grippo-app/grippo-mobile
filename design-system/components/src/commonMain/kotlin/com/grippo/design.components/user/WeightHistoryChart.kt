package com.grippo.design.components.user

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.chart.sparkline.SparklineData
import com.grippo.chart.sparkline.SparklinePoint
import com.grippo.core.state.profile.WeightHistoryState
import com.grippo.core.state.profile.stubWeightHistoryList
import com.grippo.design.components.chart.internal.Sparkline
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableList

@Composable
public fun WeightHistoryChart(
    modifier: Modifier = Modifier,
    list: ImmutableList<WeightHistoryState>,
) {
    val data = remember(list) {
        val values = list
            .asReversed()
            .mapNotNull { it.value.value?.takeIf { weight -> weight > 0f } }

        if (values.size < 2) {
            null
        } else {
            val points = values.mapIndexed { index, value ->
                SparklinePoint(index.toFloat(), value)
            }
            SparklineData(points = points)
        }
    } ?: return

    Sparkline(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(2.2f),
        data = data,
    )
}

@AppPreview
@Composable
private fun WeightHistoryChartPreview() {
    PreviewContainer {
        WeightHistoryChart(
            list = stubWeightHistoryList(),
        )
    }
}
