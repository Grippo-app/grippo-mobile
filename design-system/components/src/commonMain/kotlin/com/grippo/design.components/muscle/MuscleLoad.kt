package com.grippo.design.components.muscle

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.calculation.models.MuscleImages
import com.grippo.calculation.models.MuscleLoadBreakdown
import com.grippo.design.components.chart.DSProgressData
import com.grippo.design.components.chart.ProgressChart
import com.grippo.design.core.AppTokens

@Composable
public fun MuscleLoad(
    modifier: Modifier = Modifier,
    chartData: DSProgressData,
    muscleBreakdown: MuscleLoadBreakdown,
    muscleImages: MuscleImages?,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {
        val entries = remember(muscleBreakdown) { muscleBreakdown.entries }

        if (entries.isNotEmpty() && muscleImages != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    modifier = Modifier.weight(1f),
                    imageVector = muscleImages.front,
                    contentDescription = null,
                )
                Image(
                    modifier = Modifier.weight(1f),
                    imageVector = muscleImages.back,
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
