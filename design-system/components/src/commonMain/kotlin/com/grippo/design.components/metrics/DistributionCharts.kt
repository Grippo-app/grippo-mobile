package com.grippo.design.components.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.grippo.chart.pie.PieData
import com.grippo.chart.pie.PieSlice
import com.grippo.core.state.examples.CategoryEnumState
import com.grippo.core.state.examples.ForceTypeEnumState
import com.grippo.core.state.examples.WeightTypeEnumState
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.metrics.ExerciseDistributionState
import com.grippo.core.state.metrics.stubCategoryDistribution
import com.grippo.core.state.metrics.stubForceDistribution
import com.grippo.core.state.metrics.stubWeightDistribution
import com.grippo.design.components.chart.internal.PieChart
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun ExerciseDistributionChart(
    value: ExerciseDistributionState<CategoryEnumState>,
    modifier: Modifier = Modifier,
) {
    ExerciseDistributionChartContent(
        state = value,
        modifier = modifier,
        labelProvider = { it.title() },
        colorProvider = { it.color() }
    )
}

@Composable
public fun WeightTypeDistributionChart(
    value: ExerciseDistributionState<WeightTypeEnumState>,
    modifier: Modifier = Modifier,
) {
    ExerciseDistributionChartContent(
        state = value,
        modifier = modifier,
        labelProvider = { it.title() },
        colorProvider = { it.color() }
    )
}

@Composable
public fun ForceTypeDistributionChart(
    value: ExerciseDistributionState<ForceTypeEnumState>,
    modifier: Modifier = Modifier,
) {
    ExerciseDistributionChartContent(
        state = value,
        modifier = modifier,
        labelProvider = { it.title() },
        colorProvider = { it.color() }
    )
}

@Composable
private fun <T> ExerciseDistributionChartContent(
    state: ExerciseDistributionState<T>,
    modifier: Modifier = Modifier,
    labelProvider: (T) -> UiText,
    colorProvider: @Composable (T) -> Color,
) {
    if (state.entries.isEmpty()) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val chartMinSize = AppTokens.dp.metrics.distribution.size

        val slices = buildList {
            state.entries.forEach { entry ->
                add(
                    PieSlice(
                        id = "${entry.key}_${entry.value}",
                        label = labelProvider(entry.key).text(),
                        value = entry.value,
                        color = colorProvider(entry.key)
                    )
                )
            }
        }

        PieChart(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(
                    minWidth = chartMinSize,
                    minHeight = chartMinSize
                )
                .aspectRatio(1f),
            data = PieData(slices = slices)
        )
    }
}

@AppPreview
@Composable
private fun ExerciseDistributionPreview() {
    PreviewContainer {
        val category = stubCategoryDistribution()
        val weight = stubWeightDistribution()
        val force = stubForceDistribution()

        Column(
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ExerciseDistributionChart(
                value = category,
                modifier = Modifier.size(200.dp)
            )

            WeightTypeDistributionChart(
                value = weight,
                modifier = Modifier.size(200.dp)
            )

            ForceTypeDistributionChart(
                value = force,
                modifier = Modifier.size(200.dp)
            )
        }
    }
}
