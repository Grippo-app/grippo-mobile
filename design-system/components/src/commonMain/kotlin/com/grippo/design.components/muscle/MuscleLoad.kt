package com.grippo.design.components.muscle

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.calculation.models.MuscleLoadSummary
import com.grippo.design.components.chart.DSProgressData
import com.grippo.design.components.chart.ProgressChart
import com.grippo.design.core.AppTokens

@Composable
public fun MuscleLoad(
    modifier: Modifier = Modifier,
    chartData: DSProgressData,
    valueSummary: MuscleLoadSummary,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {
        val images = valueSummary.images
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
