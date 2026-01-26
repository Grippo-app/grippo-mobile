package com.grippo.design.components.metrics.training.profile

import androidx.compose.foundation.layout.width
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
import com.grippo.core.state.metrics.stubTrainingLoadProfile
import com.grippo.design.components.chart.internal.RadarChart
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

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

    val orderedKinds = remember {
        listOf(
            TrainingDimensionKindState.Strength,
            TrainingDimensionKindState.Hypertrophy,
            TrainingDimensionKindState.Endurance,
        )
    }
    val axisIds = orderedKinds.associateWith { it.axisId() }
    val axisLabels = orderedKinds.associateWith { it.label() }
    val seriesName = value.kind.label()

    val data = remember(value, axisIds, axisLabels, seriesName) {
        val scoresByKind = value.dimensions.associateBy(
            keySelector = TrainingDimensionScoreState::kind,
            valueTransform = TrainingDimensionScoreState::score,
        )
        val maxScore = scoresByKind.values.maxOrNull() ?: return@remember null
        if (maxScore <= 0) return@remember null
        val axes = orderedKinds.map { kind ->
            RadarAxis(
                id = axisIds.getValue(kind),
                label = axisLabels.getValue(kind),
            )
        }
        val values = orderedKinds.associate { kind ->
            val normalizedScore = scoresByKind.getValue(kind).toFloat() / maxScore.toFloat()
            axisIds.getValue(kind) to normalizedScore
        }

        RadarData(
            axes = axes,
            series = listOf(
                RadarSeries(
                    name = seriesName,
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

    val clickable = when (style) {
        TrainingProfileRadarStyle.SMALL -> false
        TrainingProfileRadarStyle.LARGE -> true
    }

    RadarChart(
        modifier = modifier.width(size),
        data = data,
        showLabels = showLabels,
        clickable = clickable
    )
}

@AppPreview
@Composable
private fun TrainingProfileRadarPreview() {
    PreviewContainer {
        TrainingProfileRadar(
            value = stubTrainingLoadProfile(),
            style = TrainingProfileRadarStyle.LARGE
        )

        TrainingProfileRadar(
            value = stubTrainingLoadProfile(),
            style = TrainingProfileRadarStyle.SMALL
        )
    }
}
