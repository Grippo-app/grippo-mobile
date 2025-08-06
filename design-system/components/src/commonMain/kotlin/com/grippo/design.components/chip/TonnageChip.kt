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
import com.grippo.design.resources.icons.Weight
import com.grippo.design.resources.kg
import com.grippo.design.resources.tonnage_chip
import kotlin.math.roundToInt

@Immutable
public enum class TonnageChipStyle {
    SHORT,
    LONG
}

@Composable
public fun TonnageChip(
    modifier: Modifier = Modifier,
    value: Float,
    style: TonnageChipStyle
) {
    val displayValue = if (value == value.roundToInt().toFloat()) {
        value.toInt().toString()
    } else {
        ((value * 10).roundToInt() / 10.0).toString()
    }

    val colors = AppTokens.colors.chip.tonnage

    val text = when (style) {
        TonnageChipStyle.SHORT -> Label.Empty
        TonnageChipStyle.LONG -> Label.Text(UiText.Str(AppTokens.strings.res(Res.string.tonnage_chip)))
    }

    val trailing = when (style) {
        TonnageChipStyle.SHORT -> Trailing.Icon(AppTokens.icons.Weight)
        TonnageChipStyle.LONG -> Trailing.Icon(AppTokens.icons.Weight)
    }

    val kg = AppTokens.strings.res(Res.string.kg)

    Chip(
        modifier = modifier,
        label = text,
        value = "${displayValue}$kg",
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
        TonnageChip(
            value = 1250.5f,
            style = TonnageChipStyle.LONG
        )

        TonnageChip(
            value = 1250.5f,
            style = TonnageChipStyle.SHORT
        )
    }
}
