package com.grippo.design.components.chip

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.icons.FireFlame
import com.grippo.design.resources.intensity
import kotlin.math.roundToInt

@Composable
public fun IntensityChip(
    modifier: Modifier = Modifier,
    value: Float
) {
    val displayValue = if (value == value.roundToInt().toFloat()) {
        value.toInt().toString()
    } else {
        ((value * 10).roundToInt() / 10.0).toString()
    }

    val colors = AppTokens.colors.chip.intensity

    Chip(
        modifier = modifier,
        label = AppTokens.strings.res(Res.string.intensity),
        value = displayValue,
        trailing = Trailing.Icon(AppTokens.icons.FireFlame),
        contentColor = colors.contentColor,
        borderColor = colors.borderColor,
        brush = Brush.horizontalGradient(
            colors = listOf(colors.startColor, colors.endColor)
        )
    )
}

@AppPreview
@Composable
private fun ChipPreview() {
    PreviewContainer {
        IntensityChip(value = 85f)
    }
}
