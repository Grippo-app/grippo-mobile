package com.grippo.design.components.metrics.distribution.muscle.loading.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.distribution.MuscleLoadSummaryState
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
internal fun MuscleLoadingPerGroup(
    modifier: Modifier = Modifier,
    summary: MuscleLoadSummaryState,
) {
    val palette = AppTokens.colors.muscle.palette6MuscleCalm

    val entries = remember(summary, palette) {
        colorizeEntries(
            entries = summary.perGroup.entries,
            palette = palette,
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
    ) {
        entries.forEachIndexed { index, colored ->
            key(colored.entry.group) {
                MuscleLoadingItem(
                    entry = colored.entry,
                    label = colored.entry.group.title().text(),
                    indicatorColors = indicatorColorsForRank(index, entries.size),
                    dominant = index == 0,
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun MuscleLoadingPerGroupPreview() {
    PreviewContainer {
        MuscleLoadingPerGroup(
            modifier = Modifier,
            summary = stubMuscleLoadSummary(),
        )
    }
}
