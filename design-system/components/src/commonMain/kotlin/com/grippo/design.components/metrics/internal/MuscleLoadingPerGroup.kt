package com.grippo.design.components.metrics.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.MuscleLoadSummaryState
import com.grippo.core.state.metrics.stubMuscleLoadSummary
import com.grippo.design.components.metrics.MuscleLoadingStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
internal fun MuscleLoadingPerGroup(
    modifier: Modifier = Modifier,
    summary: MuscleLoadSummaryState,
    style: MuscleLoadingStyle
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
            key(index) {
                MuscleLoadingItem(
                    entry = colored.entry,
                    color = colored.color,
                    label = colored.entry.group.title().text(),
                    dominant = index == 0,
                    style = when (style) {
                        MuscleLoadingStyle.Expanded -> MuscleLoadingItemStyle.Expanded
                        MuscleLoadingStyle.Collapsed -> MuscleLoadingItemStyle.Collapsed
                    }
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
            summary = stubMuscleLoadSummary(),
            modifier = Modifier,
            style = MuscleLoadingStyle.Expanded
        )
        MuscleLoadingPerGroup(
            summary = stubMuscleLoadSummary(),
            modifier = Modifier,
            style = MuscleLoadingStyle.Collapsed
        )
    }
}
