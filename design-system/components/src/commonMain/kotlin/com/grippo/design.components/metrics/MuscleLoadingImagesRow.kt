package com.grippo.design.components.metrics

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.core.state.metrics.MuscleLoadSummaryState
import com.grippo.core.state.metrics.stubMuscleLoadSummary
import com.grippo.core.state.muscles.MuscleEnumState
import com.grippo.design.components.metrics.internal.ColoredEntry
import com.grippo.design.components.metrics.internal.colorizeEntries
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.AppColor
import com.grippo.design.resources.provider.muscles.MuscleColorPreset
import com.grippo.design.resources.provider.muscles.fullBack
import com.grippo.design.resources.provider.muscles.fullFront

@Immutable
public enum class MuscleLoadingImagesMode {
    Collapsed,
    Expanded,
}

@Composable
public fun MuscleLoadingImagesRow(
    modifier: Modifier = Modifier,
    summary: MuscleLoadSummaryState,
    mode: MuscleLoadingImagesMode
) {
    val muscleColors = AppTokens.colors.muscle

    val palette = AppTokens.colors.muscle.palette6MuscleCalm

    val muscleEntries = remember(summary, palette) {
        colorizeEntries(
            entries = summary.perMuscle.entries,
            palette = palette
        )
    }

    val images = remember(muscleEntries, muscleColors) {
        generateMuscleImages(muscleEntries, muscleColors)
    }

    Row(modifier = modifier) {
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

private fun generateMuscleImages(
    entries: List<ColoredEntry>,
    muscleColors: AppColor.MuscleColors,
): MuscleLoadingImages {
    val preset = buildPreset(
        sources = entries,
        fallback = muscleColors.inactive,
        outline = muscleColors.outline,
        background = muscleColors.background,
    )
    return MuscleLoadingImages(
        front = fullFront(preset),
        back = fullBack(preset),
    )
}

private fun buildPreset(
    sources: List<ColoredEntry>,
    fallback: Color,
    outline: Color,
    background: Color,
): MuscleColorPreset {
    val colorMap: Map<MuscleEnumState, Color> = sources
        .flatMap { source ->
            source.entry.muscles.map { muscle -> muscle to source.color }
        }
        .toMap()

    val resolve: (MuscleEnumState) -> Color = { muscle ->
        colorMap[muscle] ?: fallback
    }

    return MuscleColorPreset(
        biceps = resolve(MuscleEnumState.BICEPS),
        triceps = resolve(MuscleEnumState.TRICEPS),
        forearm = resolve(MuscleEnumState.FOREARM),
        forearmFront = resolve(MuscleEnumState.FOREARM),
        forearmBack = resolve(MuscleEnumState.FOREARM),
        lateralDeltoid = resolve(MuscleEnumState.LATERAL_DELTOID),
        anteriorDeltoid = resolve(MuscleEnumState.ANTERIOR_DELTOID),
        posteriorDeltoid = resolve(MuscleEnumState.POSTERIOR_DELTOID),
        pectoralisMajorAbdominal = resolve(MuscleEnumState.PECTORALIS_MAJOR_ABDOMINAL),
        pectoralisMajorClavicular = resolve(MuscleEnumState.PECTORALIS_MAJOR_CLAVICULAR),
        pectoralisMajorSternocostal = resolve(MuscleEnumState.PECTORALIS_MAJOR_STERNOCOSTAL),
        rectusAbdominis = resolve(MuscleEnumState.RECTUS_ABDOMINIS),
        obliquesAbdominis = resolve(MuscleEnumState.OBLIQUES),
        rhomboids = resolve(MuscleEnumState.RHOMBOIDS),
        latissimus = resolve(MuscleEnumState.LATISSIMUS_DORSI),
        trapezius = resolve(MuscleEnumState.TRAPEZIUS),
        teresMajor = resolve(MuscleEnumState.TERES_MAJOR),
        gluteal = resolve(MuscleEnumState.GLUTEAL),
        hamstrings = resolve(MuscleEnumState.HAMSTRINGS),
        calf = resolve(MuscleEnumState.CALF),
        quadriceps = resolve(MuscleEnumState.QUADRICEPS),
        adductors = resolve(MuscleEnumState.ADDUCTORS),
        abductors = resolve(MuscleEnumState.ABDUCTORS),
        other = fallback,
        outline = outline,
        backgroundFront = background,
        backgroundBack = background,
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
            mode = MuscleLoadingImagesMode.Expanded
        )

        MuscleLoadingImagesRow(
            summary = stubMuscleLoadSummary(),
            mode = MuscleLoadingImagesMode.Collapsed
        )
    }
}
