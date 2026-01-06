package com.grippo.design.components.muscle.internal

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
internal fun MuscleLoadingCollapsed(
    summary: MuscleLoadSummaryState,
    modifier: Modifier,
) {
    val colors = AppTokens.colors
    val palette = colors.palette.palette5OrangeRedGrowth
    val successColor = colors.semantic.success
    val restColor = colors.static.white

    val entries = remember(summary, palette, successColor, restColor) {
        colorizeEntries(
            entries = summary.perGroup.entries,
            palette = palette,
        ) { index, _ -> if (index == 0) successColor else restColor }
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
                    dominant = index == 0
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun MuscleLoadingCollapsedInternalPreview() {
    PreviewContainer {
        MuscleLoadingCollapsed(
            summary = stubMuscleLoadSummary(),
            modifier = Modifier,
        )
    }
}
