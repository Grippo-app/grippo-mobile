package com.grippo.design.components.user

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.chart.sparkline.SparklineData
import com.grippo.chart.sparkline.SparklinePoint
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.profile.WeightHistoryState
import com.grippo.core.state.profile.stubWeightHistory
import com.grippo.design.components.chart.internal.Sparkline
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
public fun WeightHistoryChart(
    modifier: Modifier = Modifier,
    list: ImmutableList<WeightHistoryState>,
) {
    val data = remember(list) {
        val values = list
            .asReversed()
            .mapNotNull { it.value.value?.takeIf { weight -> weight > 0f } }

        if (values.isEmpty()) {
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
            .aspectRatio(AppTokens.dp.weightHistoryCard.chartAspectRatio),
        data = data,
    )
}

@AppPreview
@Composable
private fun WeightHistoryChartPreview() {
    PreviewContainer {
        WeightHistoryChart(
            list = listOf(80f, 81f, 82f).mapIndexed { index, weight ->
                stubWeightHistory().copy(id = "weight-$index", value = WeightFormatState.of(weight))
            }.toImmutableList(),
        )
    }
}

@AppPreview
@Composable
private fun WeightHistoryChartSingleValuePreview() {
    PreviewContainer {
        WeightHistoryChart(
            list = persistentListOf(
                stubWeightHistory().copy(id = "weight", value = WeightFormatState.of(80f)),
            ),
        )
    }
}

@AppPreview
@Composable
private fun WeightHistoryChartEmptyPreview() {
    PreviewContainer {
        WeightHistoryChart(list = persistentListOf())
    }
}
