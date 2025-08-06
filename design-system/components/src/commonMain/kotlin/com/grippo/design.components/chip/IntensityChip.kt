package com.grippo.design.components.chip

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.grippo.design.core.AppTokens
import com.grippo.design.core.UiText
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.icons.FireFlame
import com.grippo.design.resources.intensity_chip
import com.grippo.design.resources.percent
import kotlin.math.roundToInt

@Immutable
public enum class IntensityChipStyle {
    SHORT,
    LONG
}

@Composable
public fun IntensityChip(
    modifier: Modifier = Modifier,
    value: Float,
    style: IntensityChipStyle
) {
    val displayValue = if (value == value.roundToInt().toFloat()) {
        value.toInt().toString()
    } else {
        ((value * 10).roundToInt() / 10.0).toString()
    }

    val colors = AppTokens.colors.chip.intensity

    val text = when (style) {
        IntensityChipStyle.SHORT -> Label.Empty
        IntensityChipStyle.LONG -> Label.Text(UiText.Str(AppTokens.strings.res(Res.string.intensity_chip)))
    }

    val trailing = when (style) {
        IntensityChipStyle.SHORT -> Trailing.Icon(AppTokens.icons.FireFlame)
        IntensityChipStyle.LONG -> Trailing.Icon(AppTokens.icons.FireFlame)
    }

    val percent = AppTokens.strings.res(Res.string.percent)

    Chip(
        modifier = modifier,
        label = text,
        value = "${displayValue}${percent}",
        trailing = trailing,
        contentColor = colors.contentColor,
        brush = Brush.horizontalGradient(
            colors = listOf(colors.startColor, colors.endColor)
        )
    )
}

@AppPreview
@Composable
private fun ChipPreview() {
    PreviewContainer {
        IntensityChip(
            value = 85f,
            style = IntensityChipStyle.LONG
        )

        IntensityChip(
            value = 85f,
            style = IntensityChipStyle.SHORT
        )
    }
}
