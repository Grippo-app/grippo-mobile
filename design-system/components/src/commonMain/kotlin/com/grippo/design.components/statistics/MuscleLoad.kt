package com.grippo.design.components.statistics

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.calculation.models.MuscleLoadBreakdown
import com.grippo.calculation.muscle.MuscleEngine
import com.grippo.design.components.chart.DSProgressData
import com.grippo.design.components.chart.ProgressChart
import com.grippo.design.core.AppTokens

@Composable
public fun MuscleLoad(
    modifier: Modifier = Modifier,
    chartData: DSProgressData,
    muscleBreakdown: MuscleLoadBreakdown,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {
        val entries = remember(muscleBreakdown) { muscleBreakdown.entries }

        if (entries.isNotEmpty()) {
            val preset = MuscleEngine.generatePreset(entries)
            val images = MuscleEngine.generateImages(preset)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    modifier = Modifier.weight(1f),
                    imageVector = images.first,
                    contentDescription = null,
                )
                Image(
                    modifier = Modifier.weight(1f),
                    imageVector = images.second,
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
