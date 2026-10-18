package com.grippo.design.components.metrics.distribution.muscle.loading.internal

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.grippo.core.state.metrics.distribution.MuscleLoadEntryState
import com.grippo.core.state.formatters.PercentageFormatState
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.design.components.indicators.LineIndicator
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.AppColor
import kotlin.math.roundToInt

@Immutable
internal data class ColoredEntry(
    val entry: MuscleLoadEntryState,
    val color: Color,
)

@Composable
internal fun MuscleLoadingItem(
    modifier: Modifier = Modifier,
    entry: MuscleLoadEntryState,
    label: String,
    indicatorColors: AppColor.Charts.IndicatorColors.IndicatorColors,
    dominant: Boolean = false,
) {
    val progress = (entry.value / 100f).coerceIn(0f, 1f)

    val labelStyle =
        if (dominant) AppTokens.typography.h6()
        else AppTokens.typography.b13Med()

    val valueStyle =
        if (dominant) AppTokens.typography.b15Med()
        else AppTokens.typography.b12Med()

    val textColor =
        if (dominant) AppTokens.colors.text.primary
        else AppTokens.colors.text.secondary

    val valueText = PercentageFormatState.shortShare(entry.value)

    LineIndicator(
        modifier = modifier.fillMaxWidth(),
        progress = progress,
        colors = indicatorColors,
        labelSpacing = AppTokens.dp.contentPadding.text,
        startLabel = {
            Text(
                text = label,
                style = labelStyle,
                color = textColor,
            )
        },
        endLabel = {
            Text(
                text = valueText,
                style = valueStyle,
                color = textColor,
                maxLines = 1,
            )
        },
    )
}

internal fun colorizeEntries(
    entries: List<MuscleLoadEntryState>,
    palette: List<Color>,
): List<ColoredEntry> {
    if (entries.isEmpty()) return emptyList()

    val maxValue = entries.maxOf { it.value }
    val minValue = entries.minOf { it.value }
    val range = (maxValue - minValue).takeIf { it > 0f } ?: 1f
    val useMaxColor = entries.size == 1
    val maxPaletteColor = palette.lastOrNull() ?: Color.Unspecified

    return entries.map { entry ->
        val color = if (useMaxColor) {
            maxPaletteColor
        } else {
            colorByValue(entry.value, minValue, range, palette)
        }
        ColoredEntry(entry = entry, color = color)
    }
}

private fun colorByValue(
    value: Float,
    minValue: Float,
    range: Float,
    palette: List<Color>,
): Color {
    if (palette.isEmpty()) return Color.Unspecified
    val normalized = ((value - minValue) / range).coerceIn(0f, 1f)
    val index = (normalized * (palette.size - 1)).roundToInt().coerceIn(0, palette.size - 1)
    return palette[index]
}

@Composable
internal fun rememberMuscleLoadIndicatorColors(
    color: Color,
): AppColor.Charts.IndicatorColors.IndicatorColors {
    val trackColor = AppTokens.colors.charts.indicator.muted.track
    return remember(color, trackColor) {
        object : AppColor.Charts.IndicatorColors.IndicatorColors {
            override val colors = listOf(color)
            override val track = trackColor
        }
    }
}

@AppPreview
@Composable
private fun MuscleLoadingItemPreview() {
    PreviewContainer {
        val summary = stubMuscleLoadSummary()
        val entry = summary.perGroup.entries.first()

        MuscleLoadingItem(
            entry = entry,
            label = entry.group.title().text(),
            indicatorColors = rememberMuscleLoadIndicatorColors(AppTokens.colors.muscle.palette6MuscleCalm.last()),
            dominant = true,
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingItemLongLabelPreview() {
    PreviewContainer {
        val summary = stubMuscleLoadSummary()
        val entry = summary.perGroup.entries.first()
        MuscleLoadingItem(
            entry = entry,
            label = "Pectoralis major, sternocostal and clavicular portions",
            indicatorColors = rememberMuscleLoadIndicatorColors(AppTokens.colors.muscle.palette6MuscleCalm.last()),
        )
    }
}
