package com.grippo.design.components.metrics.distribution.muscle.loading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.metrics.distribution.MuscleLoadSummaryState
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummaryEmpty
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummarySingleGroup
import com.grippo.core.state.muscles.MuscleGroupEnumState
import com.grippo.design.components.indicators.LineIndicator
import com.grippo.design.components.metrics.MetricCardPresentation
import com.grippo.design.components.metrics.distribution.muscle.loading.internal.ColoredEntry
import com.grippo.design.components.metrics.distribution.muscle.loading.internal.colorizeEntries
import com.grippo.design.components.metrics.distribution.muscle.loading.internal.rememberMuscleLoadIndicatorColors
import com.grippo.core.state.formatters.PercentageFormatState
import com.grippo.design.components.metrics.internal.MetricOverviewHeader
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.highlight_muscle_focus
import com.grippo.design.resources.provider.metric_overview_muscle_context
import com.grippo.design.resources.provider.metric_overview_muscle_details
import com.grippo.design.resources.provider.metric_overview_muscle_distribution
import com.grippo.design.resources.provider.metric_overview_muscle_primary
import com.grippo.design.resources.provider.muscle_load_set_share_note
import com.grippo.design.resources.provider.muscle_load_no_data
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
public fun MuscleLoadingCard(
    modifier: Modifier = Modifier,
    summary: MuscleLoadSummaryState,
    presentation: MetricCardPresentation = MetricCardPresentation.Detailed,
) {
    MetricSectionPanel(
        modifier = modifier,
        style = if (presentation == MetricCardPresentation.Overview) {
            MetricSectionPanelStyle.Large
        } else {
            MetricSectionPanelStyle.Small
        },
    ) {
        if (presentation == MetricCardPresentation.Overview) {
            MuscleLoadingOverview(summary = summary)
        } else {
            Text(
                text = AppTokens.strings.res(Res.string.highlight_muscle_focus),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            MuscleLoading(
                summary = summary,
                mode = MuscleLoadingMode.PerGroup,
            )
        }
    }
}

@Composable
private fun MuscleLoadingOverview(summary: MuscleLoadSummaryState) {
    val palette = AppTokens.colors.muscle.palette6MuscleCalm
    val entries = remember(summary.perGroup, palette) {
        colorizeEntries(
            entries = summary.perGroup.entries,
            palette = palette,
        ).sortedByDescending { it.entry.value }.take(3)
    }
    val emptyLabel = AppTokens.strings.res(Res.string.muscle_load_no_data)
    val imageDescription = summary.perGroup.entries.map { entry ->
        "${entry.group.title().text()} ${PercentageFormatState.shortShare(entry.value)}"
    }.joinToString(separator = ", ").ifEmpty { emptyLabel }
    val singleGroup = summary.perGroup.entries.singleOrNull()?.group
    val primaryMuscle = if (singleGroup != null) {
        summary.perMuscle.entries
            .filter { it.group == singleGroup && it.muscles.isNotEmpty() }
            .maxByOrNull { it.value }
            ?.muscles
            ?.firstOrNull()
    } else {
        null
    }

    Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
        MetricOverviewHeader(
            title = AppTokens.strings.res(Res.string.metric_overview_muscle_distribution),
        )
        Text(
            text = AppTokens.strings.res(
                Res.string.metric_overview_muscle_context,
                summary.meta.trainingsCount,
                summary.meta.totalSets,
            ),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
        )
    }

    Text(
        text = AppTokens.strings.res(Res.string.muscle_load_set_share_note),
        style = AppTokens.typography.b12Med(),
        color = AppTokens.colors.text.secondary,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MuscleLoadingImagesRow(
            modifier = Modifier
                .width(AppTokens.dp.metrics.overview.muscleImageWidth)
                .height(AppTokens.dp.metrics.overview.muscleImageHeight)
                .semantics { contentDescription = imageDescription },
            summary = summary,
            mode = MuscleLoadingImagesMode.PerGroup,
            compact = true,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        ) {
            if (entries.isEmpty()) {
                Text(
                    text = emptyLabel,
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.secondary,
                )
            } else {
                entries.forEach { colored ->
                    key(colored.entry.group) {
                        MuscleLoadingOverviewRow(colored = colored)
                    }
                }
            }

            if (primaryMuscle != null) {
                Text(
                    text = AppTokens.strings.res(
                        Res.string.metric_overview_muscle_primary,
                        primaryMuscle.title().text(),
                    ),
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }
        }
    }

    MuscleLoadingDataNotice(summary = summary)

    Text(
        text = AppTokens.strings.res(Res.string.metric_overview_muscle_details),
        style = AppTokens.typography.b12Med(),
        color = AppTokens.colors.text.secondary,
    )
}

@Composable
private fun MuscleLoadingOverviewRow(colored: ColoredEntry) {
    val indicatorColors = rememberMuscleLoadIndicatorColors(colored.color)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = colored.entry.group.title().text(),
                style = AppTokens.typography.b13Semi(),
                color = AppTokens.colors.text.primary,
            )
            Text(
                text = PercentageFormatState.shortShare(colored.entry.value),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
            )
        }
        LineIndicator(
            modifier = Modifier.fillMaxWidth(),
            progress = (colored.entry.value / 100f).coerceIn(0f, 1f),
            colors = indicatorColors,
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingCardPreview() {
    PreviewContainer {
        MuscleLoadingCard(
            summary = stubMuscleLoadSummary(),
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingCardOverviewIncompletePreview() {
    PreviewContainer {
        val summary = stubMuscleLoadSummary()
        MuscleLoadingCard(
            summary = summary.copy(
                meta = summary.meta.copy(
                    excludedSets = 2,
                    unassignedSets = 3,
                    unmeasuredVolumeSets = 4,
                ),
            ),
            presentation = MetricCardPresentation.Overview,
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingCardOverviewPreview() {
    PreviewContainer {
        MuscleLoadingCard(
            modifier = Modifier.fillMaxWidth(),
            summary = stubMuscleLoadSummary(),
            presentation = MetricCardPresentation.Overview,
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingCardOverviewEmptyPreview() {
    PreviewContainer {
        MuscleLoadingCard(
            modifier = Modifier.fillMaxWidth(),
            summary = stubMuscleLoadSummaryEmpty(),
            presentation = MetricCardPresentation.Overview,
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingCardOverviewSingleGroupPreview() {
    PreviewContainer {
        MuscleLoadingCard(
            modifier = Modifier.fillMaxWidth(),
            summary = stubMuscleLoadSummarySingleGroup(),
            presentation = MetricCardPresentation.Overview,
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingCardOverviewSingleGroupWithoutMusclePreview() {
    PreviewContainer {
        val summary = stubMuscleLoadSummarySingleGroup()
        MuscleLoadingCard(
            modifier = Modifier.fillMaxWidth(),
            summary = summary.copy(
                perMuscle = summary.perMuscle.copy(entries = persistentListOf()),
            ),
            presentation = MetricCardPresentation.Overview,
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingCardOverviewTwoGroupsPreview() {
    PreviewContainer {
        val summary = stubMuscleLoadSummary()
        val groupEntries = persistentListOf(
            summary.perGroup.entries[2].copy(value = 55f),
            summary.perGroup.entries[0].copy(value = 45f),
        )
        MuscleLoadingCard(
            modifier = Modifier.fillMaxWidth(),
            summary = summary.copy(
                meta = summary.meta.copy(dominantGroup = MuscleGroupEnumState.ARMS_AND_FOREARMS),
                perGroup = summary.perGroup.copy(
                    entries = groupEntries,
                ),
                perMuscle = summary.perMuscle.copy(
                    entries = summary.perMuscle.entries
                        .filter {
                            it.group == MuscleGroupEnumState.ARMS_AND_FOREARMS ||
                                it.group == MuscleGroupEnumState.CHEST_MUSCLES
                        }
                        .toPersistentList(),
                ),
            ),
            presentation = MetricCardPresentation.Overview,
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingCardDetailedEmptyPreview() {
    PreviewContainer {
        MuscleLoadingCard(
            summary = stubMuscleLoadSummaryEmpty(),
        )
    }
}
