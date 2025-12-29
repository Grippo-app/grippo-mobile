package com.grippo.design.components.chart

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.chart.progress.ProgressChartData
import com.grippo.chart.progress.ProgressData
import com.grippo.design.components.chart.internal.ProgressChart
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.toolkit.calculation.models.MuscleLoadBreakdown
import com.grippo.toolkit.calculation.models.MuscleLoadEntry
import com.grippo.toolkit.calculation.models.MuscleLoadSummary

@Composable
public fun MuscleLoadChart(
    modifier: Modifier = Modifier,
    value: MuscleLoadSummary,
) {
    val chartData = remember(value) {
        value.toProgressData()
    }

    ProgressChart(
        modifier = modifier,
        data = chartData,
    )
}

private fun MuscleLoadSummary.toProgressData(): ProgressData = ProgressData(
    items = perGroup.entries.map { entry ->
        ProgressChartData(
            label = entry.label,
            value = entry.value,
            color = entry.color,
        )
    }
)

@AppPreview
@Composable
private fun MuscleLoadChartPreview() {
    PreviewContainer {
        MuscleLoadChart(
            modifier = Modifier.fillMaxWidth(),
            value = MuscleLoadSummary(
                perGroup = MuscleLoadBreakdown(
                    entries = listOf(
                        MuscleLoadEntry(
                            label = "Chest",
                            value = 0.8f,
                            color = AppTokens.colors.semantic.success,
                            muscles = emptyList()
                        ),
                        MuscleLoadEntry(
                            label = "Back",
                            value = 0.6f,
                            color = AppTokens.colors.semantic.info,
                            muscles = emptyList()
                        ),
                        MuscleLoadEntry(
                            label = "Legs",
                            value = 0.9f,
                            color = AppTokens.colors.semantic.warning,
                            muscles = emptyList()
                        ),
                        MuscleLoadEntry(
                            label = "Arms",
                            value = 0.4f,
                            color = AppTokens.colors.semantic.error,
                            muscles = emptyList()
                        )
                    )
                ),
                images = null
            )
        )
    }
}
