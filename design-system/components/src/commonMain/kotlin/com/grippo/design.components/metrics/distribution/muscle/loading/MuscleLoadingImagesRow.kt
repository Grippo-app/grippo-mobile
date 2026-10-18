package com.grippo.design.components.metrics.distribution.muscle.loading

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.core.state.metrics.distribution.MuscleLoadSummaryState
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummaryEmpty
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummarySingleGroup
import com.grippo.core.state.muscles.MuscleEnumState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.AppColor
import com.grippo.design.resources.provider.muscles.fullBack
import com.grippo.design.resources.provider.muscles.fullFront

@Immutable
public enum class MuscleLoadingImagesMode {
    PerGroup,
    PerMuscle,
}

@Composable
public fun MuscleLoadingImagesRow(
    modifier: Modifier = Modifier,
    summary: MuscleLoadSummaryState,
    mode: MuscleLoadingImagesMode,
    compact: Boolean = false,
) {
    val muscleColors = AppTokens.colors.muscle
    val breakdown = when (mode) {
        MuscleLoadingImagesMode.PerGroup -> summary.perGroup
        MuscleLoadingImagesMode.PerMuscle -> summary.perMuscle
    }
    val colorMap = breakdown.entries.flatMap { entry ->
        val color = breakdown.color(entry)
        entry.muscles.map { muscle -> muscle to color }
    }.toMap()

    val images = remember(colorMap, muscleColors) {
        generateMuscleImages(colorMap, muscleColors)
    }

    if (compact) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                modifier = Modifier.weight(1f),
                imageVector = images.front,
                contentDescription = null,
            )

            Image(
                modifier = Modifier.weight(1f),
                imageVector = images.back,
                contentDescription = null,
            )
        }
    } else {
        Row(modifier = modifier) {
            Spacer(Modifier.weight(0.2f))

            Image(
                modifier = Modifier.weight(0.8f),
                imageVector = images.front,
                contentDescription = null
            )

            Spacer(Modifier.weight(0.2f))

            Image(
                modifier = Modifier.weight(0.8f),
                imageVector = images.back,
                contentDescription = null
            )

            Spacer(Modifier.weight(0.2f))
        }
    }
}

private fun generateMuscleImages(
    colorMap: Map<MuscleEnumState, Color>,
    muscleColors: AppColor.MuscleColors,
): MuscleLoadingImages {
    val preset = MuscleEnumState.colorPreset(
        fallback = muscleColors.inactive,
        outline = muscleColors.outline,
        background = muscleColors.background,
        colorFor = { muscle -> colorMap[muscle] ?: muscleColors.inactive },
    )
    return MuscleLoadingImages(
        front = fullFront(preset),
        back = fullBack(preset),
    )
}

@Immutable
private data class MuscleLoadingImages(
    val front: ImageVector,
    val back: ImageVector,
)

@AppPreview
@Composable
private fun MuscleLoadingCardPreview() {
    PreviewContainer {
        MuscleLoadingImagesRow(
            summary = stubMuscleLoadSummary(),
            mode = MuscleLoadingImagesMode.PerMuscle
        )

        MuscleLoadingImagesRow(
            summary = stubMuscleLoadSummary(),
            mode = MuscleLoadingImagesMode.PerGroup
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingImagesRowCompactPreview() {
    PreviewContainer {
        MuscleLoadingImagesRow(
            modifier = Modifier
                .width(AppTokens.dp.metrics.overview.muscleImageWidth)
                .height(AppTokens.dp.metrics.overview.muscleImageHeight),
            summary = stubMuscleLoadSummary(),
            mode = MuscleLoadingImagesMode.PerGroup,
            compact = true,
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingImagesRowEmptyPreview() {
    PreviewContainer {
        MuscleLoadingImagesRow(
            summary = stubMuscleLoadSummaryEmpty(),
            mode = MuscleLoadingImagesMode.PerGroup,
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingImagesRowSingleGroupPreview() {
    PreviewContainer {
        MuscleLoadingImagesRow(
            summary = stubMuscleLoadSummarySingleGroup(),
            mode = MuscleLoadingImagesMode.PerMuscle,
        )
    }
}
