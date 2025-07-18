package com.grippo.design.components.chip

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

    Chip(
        modifier = modifier,
        label = AppTokens.strings.res(Res.string.intensity),
        value = displayValue,
        icon = AppTokens.icons.FireFlame,
        backgroundColor = AppTokens.colors.semantic.error.copy(alpha = 0.1f),
        contentColor = AppTokens.colors.semantic.error,
        borderColor = AppTokens.colors.border.error
    )
}

@AppPreview
@Composable
private fun ChipPreview() {
    PreviewContainer {
        IntensityChip(value = 85f)
    }
}