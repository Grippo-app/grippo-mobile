package com.grippo.design.components.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.grippo.chart.pie.PieData
import com.grippo.chart.pie.PieSlice
import com.grippo.core.state.examples.CategoryEnumState
import com.grippo.core.state.examples.ForceTypeEnumState
import com.grippo.core.state.examples.WeightTypeEnumState
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.metrics.ExerciseDistributionState
import com.grippo.core.state.metrics.stubCategoryDistributionState
import com.grippo.core.state.metrics.stubForceDistributionState
import com.grippo.core.state.metrics.stubWeightDistributionState
import com.grippo.design.components.chart.internal.PieChart
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.exercise_categories
import com.grippo.design.resources.provider.exercise_force_types
import com.grippo.design.resources.provider.exercise_weight_types

@Composable
public fun ExerciseDistributionChart(
    title: String,
    state: ExerciseDistributionState<CategoryEnumState>,
    modifier: Modifier = Modifier,
) {
    ExerciseDistributionChartContent(
        title = title,
        state = state,
        modifier = modifier,
        labelProvider = { it.title() },
        colorProvider = { it.color() }
    )
}

@Composable
public fun WeightTypeDistributionChart(
    title: String,
    state: ExerciseDistributionState<WeightTypeEnumState>,
    modifier: Modifier = Modifier,
) {
    ExerciseDistributionChartContent(
        title = title,
        state = state,
        modifier = modifier,
        labelProvider = { it.title() },
        colorProvider = { it.color() }
    )
}

@Composable
public fun ForceTypeDistributionChart(
    title: String,
    state: ExerciseDistributionState<ForceTypeEnumState>,
    modifier: Modifier = Modifier,
) {
    ExerciseDistributionChartContent(
        title = title,
        state = state,
        modifier = modifier,
        labelProvider = { it.title() },
        colorProvider = { it.color() }
    )
}

@Composable
private fun <T> ExerciseDistributionChartContent(
    title: String,
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
        Text(
            text = title,
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1
        )

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
        val category = stubCategoryDistributionState()
        val weight = stubWeightDistributionState()
        val force = stubForceDistributionState()

        Column(
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ExerciseDistributionChart(
                title = AppTokens.strings.res(Res.string.exercise_categories),
                state = category,
                modifier = Modifier.fillMaxWidth()
            )

            WeightTypeDistributionChart(
                title = AppTokens.strings.res(Res.string.exercise_weight_types),
                state = weight,
                modifier = Modifier.fillMaxWidth()
            )

            ForceTypeDistributionChart(
                title = AppTokens.strings.res(Res.string.exercise_force_types),
                state = force,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
