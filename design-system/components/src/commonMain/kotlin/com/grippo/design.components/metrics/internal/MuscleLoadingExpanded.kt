package com.grippo.design.components.metrics.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.MuscleLoadSummaryState
import com.grippo.core.state.metrics.stubMuscleLoadSummary
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
internal fun MuscleLoadingExpanded(
    modifier: Modifier = Modifier,
    summary: MuscleLoadSummaryState,
) {
    val palette = AppTokens.colors.muscle.palette6MuscleCalm

    val muscleEntries = remember(summary, palette) {
        colorizeEntries(
            entries = summary.perMuscle.entries,
            palette = palette
        )
    }

    val displayedEntries = remember(muscleEntries) {
        muscleEntries
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
    ) {
        displayedEntries.forEachIndexed { index, colored ->
            key(index) {
                MuscleLoadingItem(
                    entry = colored.entry,
                    color = colored.color,
                    dominant = index == 0
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun MuscleLoadingExpandedInternalPreview() {
    PreviewContainer {
        MuscleLoadingExpanded(
            summary = stubMuscleLoadSummary(),
            modifier = Modifier,
        )
    }
}
