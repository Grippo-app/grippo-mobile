package com.grippo.design.components.chart

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.calculation.models.MuscleLoadSummary
import com.grippo.chart.progress.ProgressChartData
import com.grippo.chart.progress.ProgressData
import com.grippo.design.components.chart.internal.ProgressChart
import com.grippo.design.core.AppTokens

@Composable
public fun MuscleLoadChart(
    modifier: Modifier = Modifier,
    value: MuscleLoadSummary,
) {
    val chartData = remember(value) {
        value.toProgressData()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {
        val images = value.images

        if (images != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    modifier = Modifier.weight(1f),
                    imageVector = images.front,
                    contentDescription = null,
                )
                Image(
                    modifier = Modifier.weight(1f),
                    imageVector = images.back,
                    contentDescription = null,
                )
            }
        }

        ProgressChart(
            modifier = Modifier.fillMaxWidth(),
            data = chartData,
        )
    }
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
