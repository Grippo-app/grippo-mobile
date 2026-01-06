package com.grippo.design.components.muscle.internal

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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

    val muscleColors = AppTokens.colors.muscle

    val images = remember(muscleEntries, muscleColors) {
        generateMuscleImages(muscleEntries, muscleColors)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
    ) {
        MuscleLoadingImagesRow(
            images = images
        )

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
private fun MuscleLoadingImagesRow(
    modifier: Modifier = Modifier,
    images: MuscleLoadingImages
) {
    Row(
        modifier = modifier.fillMaxWidth()
    ) {
        Spacer(Modifier.weight(0.2f))
        Image(
            modifier = Modifier.weight(0.8f),
            imageVector = images.front,
            contentDescription = null
        )
        Image(
            modifier = Modifier.weight(0.8f),
            imageVector = images.back,
            contentDescription = null
        )

        Spacer(Modifier.weight(0.2f))
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
