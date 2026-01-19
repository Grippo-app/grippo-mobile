package com.grippo.design.components.metrics

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.chart.radar.RadarAxis
import com.grippo.chart.radar.RadarData
import com.grippo.chart.radar.RadarSeries
import com.grippo.chart.radar.RadarValues
import com.grippo.core.state.metrics.TrainingDimensionKindState
import com.grippo.core.state.metrics.TrainingDimensionScoreState
import com.grippo.core.state.metrics.TrainingLoadProfileState
import com.grippo.design.components.chart.internal.RadarChart
import com.grippo.design.core.AppTokens

@Immutable
public enum class TrainingProfileRadarStyle {
    SMALL,
    LARGE
}

@Composable
public fun TrainingProfileRadar(
    modifier: Modifier = Modifier,
    value: TrainingLoadProfileState,
    style: TrainingProfileRadarStyle
) {
    val grid = AppTokens.colors.charts.radar.grid

    val data = remember(value) {
        val orderedKinds = listOf(
            TrainingDimensionKindState.Strength,
            TrainingDimensionKindState.Hypertrophy,
            TrainingDimensionKindState.Endurance,
        )
        val scoresByKind = value.dimensions.associateBy(
            keySelector = TrainingDimensionScoreState::kind,
            valueTransform = TrainingDimensionScoreState::score,
        )
        val maxScore = scoresByKind.values.maxOrNull() ?: return@remember null
        if (maxScore <= 0) return@remember null
        val axes = orderedKinds.map { kind ->
            RadarAxis(
                id = kind.axisId(),
                label = kind.label(),
            )
        }
        val values = orderedKinds.associate { kind ->
            val normalizedScore = scoresByKind.getValue(kind).toFloat() / maxScore.toFloat()
            kind.axisId() to normalizedScore
        }

        RadarData(
            axes = axes,
            series = listOf(
                RadarSeries(
                    name = value.kind.label(),
                    color = grid,
                    values = RadarValues.ByAxisId(values),
                )
            ),
        )
    } ?: return

    val size = when (style) {
        TrainingProfileRadarStyle.SMALL -> AppTokens.dp.metrics.trainingProfile.radar.small.size
        TrainingProfileRadarStyle.LARGE -> AppTokens.dp.metrics.trainingProfile.radar.large.size
    }

    val showLabels = when (style) {
        TrainingProfileRadarStyle.SMALL -> false
        TrainingProfileRadarStyle.LARGE -> true
    }

    RadarChart(
        modifier = modifier.size(size),
        data = data,
        showLabels = showLabels
    )
}