package com.grippo.design.components.metrics.muscle.loading.internal

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.grippo.core.state.metrics.MuscleLoadEntryState
import com.grippo.core.state.metrics.stubMuscleLoadSummary
import com.grippo.design.components.indicators.LineIndicator
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.AppColor
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.percent
import kotlin.math.roundToInt

@Composable
internal fun MuscleLoadingItem(
    entry: MuscleLoadEntryState,
    color: Color,
    label: String,
    indicatorColors: AppColor.Charts.IndicatorColors.IndicatorColors,
    modifier: Modifier = Modifier,
    dominant: Boolean = false,
) {
    val progress = (entry.value / 100f).coerceIn(0f, 1f)

    val labelStyle =
        if (dominant) AppTokens.typography.h6()
        else AppTokens.typography.b13Med()

    val valueStyle =
        if (dominant) AppTokens.typography.h6()
        else AppTokens.typography.b13Semi()

    val textColor =
        if (dominant) AppTokens.colors.text.primary
        else AppTokens.colors.text.secondary

    val percentSymbol = AppTokens.strings.res(Res.string.percent)
    val valueText = "${entry.value.roundToInt()}$percentSymbol"

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
                maxLines = 1,
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
    colorTransformer: (index: Int, Color) -> Color = { _, color -> color },
): List<ColoredEntry> {
    if (entries.isEmpty()) return emptyList()

    val maxValue = entries.maxOf { it.value }
    val minValue = entries.minOf { it.value }
    val range = (maxValue - minValue).takeIf { it > 0f } ?: 1f
    val useMaxColor = entries.size == 1
    val maxPaletteColor = palette.lastOrNull() ?: Color.Unspecified

    return entries.mapIndexed { index, entry ->
        val baseColor = if (useMaxColor) {
            maxPaletteColor
        } else {
            colorByValue(entry.value, minValue, range, palette)
        }
        val color = colorTransformer(index, baseColor)
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

@Immutable
internal data class ColoredEntry(
    val entry: MuscleLoadEntryState,
    val color: Color,
)

@Composable
internal fun indicatorColorsForRank(
    index: Int,
    total: Int,
): AppColor.Charts.IndicatorColors.IndicatorColors {
    if (total <= 0) return AppTokens.colors.charts.indicator.success
    val third = total / 3f
    return when {
        index < third -> AppTokens.colors.charts.indicator.success
        index < 2f * third -> AppTokens.colors.charts.indicator.warning
        else -> AppTokens.colors.charts.indicator.error
    }
}

@AppPreview
@Composable
private fun MuscleLoadingItemPreview() {
    PreviewContainer {
        val summary = stubMuscleLoadSummary()
        val entry = summary.perGroup.entries.first()
        val color = AppTokens.colors.muscle.palette6MuscleCalm.last()

        MuscleLoadingItem(
            entry = entry,
            color = color,
            label = entry.group.title().text(),
            indicatorColors = AppTokens.colors.charts.indicator.success,
            dominant = true,
        )
    }
}
