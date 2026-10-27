package com.grippo.design.components.metrics.distribution.muscle.loading.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.distribution.MuscleLoadSummaryState
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummaryEmpty
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummaryUnknown
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingCard
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingCardStyle
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.available_sets_label
import com.grippo.design.resources.provider.muscle_loading_details_coverage
import com.grippo.design.resources.provider.trainings

@Composable
internal fun MuscleLoadingCardSummary(
    modifier: Modifier = Modifier,
    summary: MuscleLoadSummaryState,
) {
    val outsideSets = summary.meta.excludedSets + summary.meta.unassignedSets

    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Large,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                Text(
                    text = summary.meta.totalSets.toString(),
                    style = AppTokens.typography.h3(),
                    color = AppTokens.colors.text.primary,
                )
                Text(
                    text = AppTokens.strings.res(Res.string.available_sets_label),
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                Text(
                    text = summary.meta.trainingsCount.toString(),
                    style = AppTokens.typography.h3(),
                    color = AppTokens.colors.text.primary,
                )
                Text(
                    text = AppTokens.strings.res(Res.string.trainings),
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }
        }

        if (outsideSets > 0) {
            Text(
                text = AppTokens.strings.res(Res.plurals.muscle_loading_details_coverage, outsideSets, outsideSets),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
            )
        }
    }
}

@AppPreview
@Composable
private fun MuscleLoadingCardSummaryPreview() {
    PreviewContainer {
        MuscleLoadingCard(
            summary = stubMuscleLoadSummary(),
            style = MuscleLoadingCardStyle.Summary,
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingCardSummaryPartialPreview() {
    val summary = stubMuscleLoadSummary()
    PreviewContainer {
        MuscleLoadingCard(
            summary = summary.copy(meta = summary.meta.copy(excludedSets = 2, unassignedSets = 3)),
            style = MuscleLoadingCardStyle.Summary,
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingCardSummaryUnknownPreview() {
    PreviewContainer {
        MuscleLoadingCard(
            summary = stubMuscleLoadSummaryUnknown(),
            style = MuscleLoadingCardStyle.Summary,
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingCardSummaryNoSetsPreview() {
    val summary = stubMuscleLoadSummaryEmpty()
    PreviewContainer {
        MuscleLoadingCard(
            summary = summary.copy(meta = summary.meta.copy(trainingsCount = 2, excludedSets = 3)),
            style = MuscleLoadingCardStyle.Summary,
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingCardSummaryLongCountsPreview() {
    val summary = stubMuscleLoadSummary()
    PreviewContainer {
        MuscleLoadingCard(
            summary = summary.copy(meta = summary.meta.copy(totalSets = 123_456, trainingsCount = 1_234)),
            style = MuscleLoadingCardStyle.Summary,
        )
    }
}
