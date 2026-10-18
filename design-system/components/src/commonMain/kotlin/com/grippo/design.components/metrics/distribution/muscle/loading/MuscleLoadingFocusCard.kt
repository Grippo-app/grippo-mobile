package com.grippo.design.components.metrics.distribution.muscle.loading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.distribution.MuscleLoadSummaryState
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.core.state.muscles.MuscleGroupEnumState
import com.grippo.design.components.chart.internal.RingChart
import com.grippo.design.components.metrics.distribution.muscle.loading.internal.muscleLoadPercentage
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.muscle_load_focus_title
import com.grippo.design.resources.provider.muscle_load_focus_note
import com.grippo.design.resources.provider.muscle_load_no_data
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
public fun MuscleLoadingFocusCard(
    modifier: Modifier = Modifier,
    summary: MuscleLoadSummaryState,
) {
    val topEntry = summary.perGroup.entries.maxByOrNull { it.value }
    val leadingGroups = if (topEntry != null) {
        summary.perGroup.entries
            .filter { it.value == topEntry.value }
            .map { it.group.title().text() }
            .joinToString()
    } else {
        AppTokens.strings.res(Res.string.muscle_load_no_data)
    }

    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Small,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                Text(
                    text = AppTokens.strings.res(Res.string.muscle_load_focus_title),
                    style = AppTokens.typography.b14Med(),
                    color = AppTokens.colors.text.secondary,
                )
                Text(
                    text = leadingGroups,
                    style = AppTokens.typography.h5(),
                    color = AppTokens.colors.text.primary,
                )
            }
            if (topEntry != null) {
                ShareRing(
                    modifier = Modifier.size(AppTokens.dp.metrics.distribution.muscleLoad.balance.chart),
                    share = topEntry.value,
                )
            }
        }
        Text(
            text = AppTokens.strings.res(Res.string.muscle_load_focus_note),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
        )
    }
}

@Composable
private fun ShareRing(
    modifier: Modifier = Modifier,
    share: Float,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        RingChart(
            modifier = Modifier.matchParentSize(),
            value = share,
            max = 100f,
            colors = AppTokens.colors.charts.ring.info,
        )
        Text(
            text = muscleLoadPercentage(share),
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary,
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingFocusCardTiedGroupsPreview() {
    PreviewContainer {
        val summary = stubMuscleLoadSummary()
        val groups = summary.perGroup.entries.take(2).map { it.copy(value = 50f) }
        MuscleLoadingFocusCard(
            summary = summary.copy(perGroup = summary.perGroup.copy(entries = groups.toPersistentList())),
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingFocusCardPreview() {
    PreviewContainer {
        MuscleLoadingFocusCard(
            summary = stubMuscleLoadSummary()
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingFocusCardSingleGroupPreview() {
    PreviewContainer {
        val summary = stubMuscleLoadSummary()
        val entry = summary.perGroup.entries.first {
            it.group == MuscleGroupEnumState.ARMS_AND_FOREARMS
        }.copy(value = 100f)
        MuscleLoadingFocusCard(
            summary = summary.copy(
                perGroup = summary.perGroup.copy(entries = persistentListOf(entry)),
                groupDominance = summary.groupDominance.copy(
                    top1SharePercent = 100f,
                    top2SharePercent = 100f,
                ),
            ),
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingFocusCardEmptyPreview() {
    PreviewContainer {
        val summary = stubMuscleLoadSummary()
        MuscleLoadingFocusCard(
            summary = summary.copy(
                perGroup = summary.perGroup.copy(entries = persistentListOf()),
                groupDominance = summary.groupDominance.copy(
                    top1SharePercent = 0f,
                    top2SharePercent = 0f,
                ),
            ),
        )
    }
}
