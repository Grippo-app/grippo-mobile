package com.grippo.design.components.metrics

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grippo.chart.radar.RadarAxis
import com.grippo.chart.radar.RadarData
import com.grippo.chart.radar.RadarSeries
import com.grippo.chart.radar.RadarValues
import com.grippo.core.state.metrics.TrainingDimensionKindState
import com.grippo.core.state.metrics.TrainingDimensionScoreState
import com.grippo.core.state.metrics.TrainingLoadProfileState
import com.grippo.core.state.metrics.TrainingProfileKindState
import com.grippo.design.components.chart.internal.RadarChart
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun TrainingLoadProfileCard(
    modifier: Modifier = Modifier,
    value: TrainingLoadProfileState,
) {
    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Small,
    ) {

        Text(
            text = "Training profile",
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        val fallback = AppTokens.colors.charts.radar.strokeFallback
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
                        color = fallback,
                        values = RadarValues.ByAxisId(values),
                    )
                ),
            )
        } ?: return@MetricSectionPanel
        RadarChart(
            modifier = Modifier.size(200.dp),
            data = data,
        )
    }
}

@AppPreview
@Composable
private fun TrainingLoadProfileCardPreview() {
    PreviewContainer {
        TrainingLoadProfileCard(
            value = TrainingLoadProfileState(
                kind = TrainingProfileKindState.Powerbuilding,
                dimensions = listOf(
                    TrainingDimensionScoreState(
                        kind = TrainingDimensionKindState.Strength,
                        score = 78,
                    ),
                    TrainingDimensionScoreState(
                        kind = TrainingDimensionKindState.Hypertrophy,
                        score = 64,
                    ),
                    TrainingDimensionScoreState(
                        kind = TrainingDimensionKindState.Endurance,
                        score = 42,
                    ),
                ),
            ),
        )
    }
}

private fun TrainingDimensionKindState.axisId(): String {
    return when (this) {
        TrainingDimensionKindState.Strength -> "strength"
        TrainingDimensionKindState.Hypertrophy -> "hypertrophy"
        TrainingDimensionKindState.Endurance -> "endurance"
    }
}

private fun TrainingDimensionKindState.label(): String {
    return when (this) {
        TrainingDimensionKindState.Strength -> "Strength"
        TrainingDimensionKindState.Hypertrophy -> "Hypertrophy"
        TrainingDimensionKindState.Endurance -> "Endurance"
    }
}

private fun TrainingProfileKindState.label(): String {
    return when (this) {
        TrainingProfileKindState.Strength -> "Strength"
        TrainingProfileKindState.Hypertrophy -> "Hypertrophy"
        TrainingProfileKindState.Endurance -> "Endurance"
        TrainingProfileKindState.Powerbuilding -> "Powerbuilding"
        TrainingProfileKindState.Mixed -> "Mixed"
        TrainingProfileKindState.Easy -> "Easy"
    }
}
