package com.grippo.design.components.muscle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.core.state.metrics.MuscleLoadEntryState
import com.grippo.core.state.metrics.MuscleLoadSummaryState
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

public enum class MuscleLoadingMode {
    Collapsed,
    Expanded,
}

@Composable
public fun MuscleLoading(
    summary: MuscleLoadSummaryState,
    modifier: Modifier = Modifier,
    maxVisibleEntries: Int = Int.MAX_VALUE,
    mode: MuscleLoadingMode = MuscleLoadingMode.Collapsed,
) {
    val assets = rememberMuscleLoadingAssets(summary, maxVisibleEntries, mode)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
    ) {
        assets.entries.forEachIndexed { index, colored ->
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
private fun MuscleLoadingItem(
    entry: MuscleLoadEntryState,
    color: Color,
    modifier: Modifier = Modifier,
    dominant: Boolean = false,
) {
    val progress = (entry.value / 100f).coerceIn(0f, 1f)
    val spacing = AppTokens.dp.contentPadding.text
    val labelStyle = if (dominant) AppTokens.typography.h5() else AppTokens.typography.b13Med()
    val valueStyle = if (dominant) AppTokens.typography.h5() else AppTokens.typography.b13Semi()
    val labelColor =
        if (dominant) AppTokens.colors.text.primary else AppTokens.colors.text.secondary
    val valueColor =
        if (dominant) AppTokens.colors.text.primary else AppTokens.colors.text.secondary
    val indicatorColors = indicatorColorsFor(color)
    val percentSymbol = AppTokens.strings.res(Res.string.percent)
    val valueText = "${entry.value.roundToInt()}$percentSymbol"

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing)
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

private fun indicatorColorsFor(color: Color): AppColor.LineIndicatorColors.IndicatorColors {
    return object : AppColor.LineIndicatorColors.IndicatorColors {
        override val indicator: Color = color
        override val track: Color = color.copy(alpha = 0.2f)
    }
}

@Composable
private fun rememberMuscleLoadingAssets(
    summary: MuscleLoadSummaryState,
    maxVisibleEntries: Int,
    mode: MuscleLoadingMode,
): MuscleLoadingAssets {
    val colors = AppTokens.colors
    val palette = colors.palette.palette5OrangeRedGrowth

    return remember(summary, palette, maxVisibleEntries, mode) {
        val successColor = colors.semantic.success
        val restColor = colors.static.white

        val groupEntries = colorizeEntries(
            entries = summary.perGroup.entries,
            palette = palette,
            maxVisibleEntries = maxVisibleEntries,
        ) { index, _ -> if (index == 0) successColor else restColor }

        val muscleEntries = colorizeEntries(
            entries = summary.perMuscle.entries,
            palette = palette
        )
        val displayedEntries = when (mode) {
            MuscleLoadingMode.Collapsed -> groupEntries
            MuscleLoadingMode.Expanded -> muscleEntries.take(maxVisibleEntries)
        }
        val images = generateMuscleImages(muscleEntries, colors.muscle)
        MuscleLoadingAssets(entries = displayedEntries, images = images)
    }
}

private fun colorizeEntries(
    entries: List<MuscleLoadEntryState>,
    palette: List<Color>,
    maxVisibleEntries: Int = Int.MAX_VALUE,
    colorTransformer: (index: Int, Color) -> Color = { _, color -> color },
): List<ColoredEntry> {
    val visible = entries.take(maxVisibleEntries)
    return visible.mapIndexed { index, entry ->
        val baseColor = colorByPercentage(entry.value, palette)
        val color = colorTransformer(index, baseColor)
        ColoredEntry(entry = entry, color = color)
    }
}

private fun colorByPercentage(value: Float, palette: List<Color>): Color {
    if (palette.isEmpty()) return Color(0xFFFF4C4C)
    val normalized = value.coerceIn(0f, 100f)
    val bandWidth = 100f / palette.size
    val index = (normalized / bandWidth).toInt().coerceIn(0, palette.size - 1)
    return palette[index]
}

private fun generateMuscleImages(
    entries: List<ColoredEntry>,
    muscleColors: AppColor.MuscleColors,
): MuscleLoadingImages? {
    if (entries.isEmpty()) return null
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

private data class ColoredEntry(
    val entry: MuscleLoadEntryState,
    val color: Color,
)

public data class MuscleLoadingImages(
    val front: ImageVector,
    val back: ImageVector,
)

private data class MuscleLoadingAssets(
    val entries: List<ColoredEntry>,
    val images: MuscleLoadingImages?,
)
