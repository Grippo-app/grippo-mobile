package com.grippo.design.components.metrics.internal

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
import com.grippo.core.state.metrics.MuscleLoadEntryState
import com.grippo.design.components.indicators.LineIndicator
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.AppColor
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.percent
import kotlin.math.roundToInt

@Composable
internal fun MuscleLoadingItem(
    entry: MuscleLoadEntryState,
    color: Color,
    label: String,
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
                text = label,
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
