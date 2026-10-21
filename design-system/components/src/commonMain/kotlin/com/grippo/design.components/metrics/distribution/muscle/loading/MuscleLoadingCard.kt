package com.grippo.design.components.metrics.distribution.muscle.loading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.formatters.PercentageFormatState
import com.grippo.core.state.metrics.distribution.MuscleLoadEntryState
import com.grippo.core.state.metrics.distribution.MuscleLoadSummaryState
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummaryEmpty
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummarySingleGroup
import com.grippo.core.state.muscles.MuscleGroupEnumState
import com.grippo.design.components.indicators.LineIndicator
import com.grippo.design.components.metrics.distribution.muscle.loading.internal.MuscleLoadingCardSummary
import com.grippo.design.components.metrics.distribution.muscle.loading.internal.rememberMuscleLoadIndicatorColors
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
import com.grippo.design.resources.provider.muscle_load_no_data
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Immutable
public sealed interface MuscleLoadingCardStyle {
    @Immutable
    public data object Overview : MuscleLoadingCardStyle

    @Immutable
    public data object Detailed : MuscleLoadingCardStyle

    @Immutable
    public data object Summary : MuscleLoadingCardStyle
}

@Composable
public fun MuscleLoadingCard(
    modifier: Modifier = Modifier,
    summary: MuscleLoadSummaryState,
    style: MuscleLoadingCardStyle = MuscleLoadingCardStyle.Detailed,
) {
    when (style) {
        MuscleLoadingCardStyle.Summary -> MuscleLoadingCardSummary(
            modifier = modifier,
            summary = summary,
        )

        MuscleLoadingCardStyle.Overview,
        MuscleLoadingCardStyle.Detailed -> MetricSectionPanel(
            modifier = modifier,
            style = if (style == MuscleLoadingCardStyle.Overview) {
                MetricSectionPanelStyle.Large
            } else {
                MetricSectionPanelStyle.Small
            },
        ) {
            if (style == MuscleLoadingCardStyle.Overview) {
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
}

@Composable
private fun MuscleLoadingOverview(summary: MuscleLoadSummaryState) {
    val entries = remember(summary.perGroup.entries) {
        summary.perGroup.entries.sortedByDescending { it.value }.take(3)
    }
    val emptyLabel = AppTokens.strings.res(Res.string.muscle_load_no_data)
    val imageDescription = summary.perGroup.entries.map { entry ->
        "${entry.group.title().text()} ${PercentageFormatState.shortShare(entry.value)}"
    }.joinToString(separator = ", ").ifEmpty { emptyLabel }
    val singleGroup = summary.perGroup.entries.singleOrNull()?.group
    val primaryMuscle = if (singleGroup != null) {
        val muscleEntries = summary.perMuscle.entries.filter {
            it.group == singleGroup && it.muscles.isNotEmpty()
        }
        val highestShare = muscleEntries.maxOfOrNull { it.value }
        muscleEntries.singleOrNull { it.value == highestShare }?.muscles?.firstOrNull()
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
                entries.forEach { entry ->
                    key(entry.group) {
                        MuscleLoadingOverviewRow(entry = entry, color = summary.perGroup.color(entry))
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
private fun MuscleLoadingOverviewRow(entry: MuscleLoadEntryState, color: Color) {
    val indicatorColors = rememberMuscleLoadIndicatorColors(color)

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
                text = entry.group.title().text(),
                style = AppTokens.typography.b13Semi(),
                color = AppTokens.colors.text.primary,
            )
            Text(
                text = PercentageFormatState.shortShare(entry.value),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
            )
        }
        LineIndicator(
            modifier = Modifier.fillMaxWidth(),
            progress = (entry.value / 100f).coerceIn(0f, 1f),
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
            style = MuscleLoadingCardStyle.Overview,
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
            style = MuscleLoadingCardStyle.Overview,
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
            style = MuscleLoadingCardStyle.Overview,
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
            style = MuscleLoadingCardStyle.Overview,
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
            style = MuscleLoadingCardStyle.Overview,
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingCardOverviewSingleGroupTiedMusclesPreview() {
    PreviewContainer {
        val summary = stubMuscleLoadSummary()
        val entry = summary.perGroup.entries.first().copy(value = 100f)
        MuscleLoadingCard(
            modifier = Modifier.fillMaxWidth(),
            summary = summary.copy(
                perGroup = summary.perGroup.copy(entries = persistentListOf(entry)),
                perMuscle = summary.perMuscle.copy(
                    entries = summary.perMuscle.entries
                        .filter { it.group == entry.group }
                        .map { it.copy(value = 50f) }
                        .toPersistentList(),
                ),
            ),
            style = MuscleLoadingCardStyle.Overview,
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
            style = MuscleLoadingCardStyle.Overview,
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
