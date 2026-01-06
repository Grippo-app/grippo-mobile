package com.grippo.design.components.muscle.internal

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
    summary: MuscleLoadSummaryState,
    modifier: Modifier,
) {
    val colors = AppTokens.colors
    val palette = colors.palette.palette7BlueGrowth

    val muscleEntries = remember(summary, palette) {
        colorizeEntries(
            entries = summary.perMuscle.entries,
            palette = palette
        )
    }
    val displayedEntries = remember(muscleEntries) {
        muscleEntries
    }
    val muscleColors = colors.muscle
    val images = remember(muscleEntries, muscleColors) {
        generateMuscleImages(muscleEntries, muscleColors)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
    ) {
        MuscleLoadingImagesRow(images)

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

@Composable
private fun MuscleLoadingImagesRow(images: MuscleLoadingImages) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Image(
            modifier = Modifier.weight(1f),
            imageVector = images.front,
            contentDescription = null
        )
        Image(
            modifier = Modifier.weight(1f),
            imageVector = images.back,
            contentDescription = null
        )
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
