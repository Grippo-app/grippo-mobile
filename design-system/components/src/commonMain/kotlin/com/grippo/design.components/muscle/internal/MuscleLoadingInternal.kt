package com.grippo.design.components.muscle.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.core.state.metrics.MuscleLoadEntryState
import com.grippo.core.state.muscles.MuscleEnumState
import com.grippo.design.components.indicators.LineIndicator
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.AppColor
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.muscles.MuscleColorPreset
import com.grippo.design.resources.provider.muscles.fullBack
import com.grippo.design.resources.provider.muscles.fullFront
import com.grippo.design.resources.provider.percent
import kotlin.math.roundToInt

@Composable
internal fun MuscleLoadingItem(
    entry: MuscleLoadEntryState,
    color: Color,
    modifier: Modifier = Modifier,
    dominant: Boolean = false,
) {
    val progress = (entry.value / 100f).coerceIn(0f, 1f)

    val labelStyle =
        if (dominant) AppTokens.typography.h5()
        else AppTokens.typography.b13Med()

    val valueStyle =
        if (dominant) AppTokens.typography.h5()
        else AppTokens.typography.b13Semi()

    val labelColor =
        if (dominant) AppTokens.colors.text.primary
        else AppTokens.colors.text.secondary

    val valueColor =
        if (dominant) AppTokens.colors.text.primary
        else AppTokens.colors.text.secondary

    val indicatorColors = indicatorColorsFor(color)

    val percentSymbol = AppTokens.strings.res(Res.string.percent)

    val valueText = "${entry.value.roundToInt()}$percentSymbol"

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = entry.label,
                style = labelStyle,
                color = labelColor,
                maxLines = 1
            )

            Text(
                text = valueText,
                style = valueStyle,
                color = valueColor,
                maxLines = 1
            )
        }

        LineIndicator(
            modifier = Modifier.fillMaxWidth(),
            progress = progress,
            colors = indicatorColors
        )
    }
}

internal fun indicatorColorsFor(color: Color): AppColor.LineIndicatorColors.IndicatorColors {
    return object : AppColor.LineIndicatorColors.IndicatorColors {
        override val indicator: Color = color
        override val track: Color = color.copy(alpha = 0.2f)
    }
}

internal fun colorizeEntries(
    entries: List<MuscleLoadEntryState>,
    palette: List<Color>,
    colorTransformer: (index: Int, Color) -> Color = { _, color -> color },
): List<ColoredEntry> {
    val visible = entries
    return visible.mapIndexed { index, entry ->
        val baseColor = colorByPercentage(entry.value, palette)
        val color = colorTransformer(index, baseColor)
        ColoredEntry(entry = entry, color = color)
    }
}

internal fun colorByPercentage(value: Float, palette: List<Color>): Color {
    val normalized = value.coerceIn(0f, 100f)
    val bandWidth = 100f / palette.size
    val index = (normalized / bandWidth).toInt().coerceIn(0, palette.size - 1)
    return palette[index]
}

internal fun generateMuscleImages(
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

internal fun buildPreset(
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
internal data class ColoredEntry(
    val entry: MuscleLoadEntryState,
    val color: Color,
)

@Immutable
internal data class MuscleLoadingImages(
    val front: ImageVector,
    val back: ImageVector,
)
