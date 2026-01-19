package com.grippo.design.components.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
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
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.training_profile

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
            text = AppTokens.strings.res(Res.string.training_profile),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

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
        } ?: return@MetricSectionPanel

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadarChart(
                modifier = Modifier.size(AppTokens.dp.metrics.trainingProfile.chartSize),
                data = data,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = value.title(),
                    style = AppTokens.typography.h6(),
                    color = AppTokens.colors.text.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = value.subtitle(),
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = value.details(),
                    style = AppTokens.typography.b11Med(),
                    color = AppTokens.colors.text.tertiary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
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
                dominant = TrainingDimensionKindState.Hypertrophy,
                confidence = 40
            ),
        )
    }
}
