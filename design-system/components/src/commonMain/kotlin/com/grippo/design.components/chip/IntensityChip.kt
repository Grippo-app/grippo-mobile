package com.grippo.design.components.chip

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.FireFlame
import com.grippo.design.resources.provider.intensity_chip
import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.formatters.UiText

@Immutable
public enum class IntensityChipStyle {
    SHORT,
    LONG
}

@Composable
public fun IntensityChip(
    modifier: Modifier = Modifier,
    value: IntensityFormatState,
    size: ChipSize,
    style: IntensityChipStyle
) {
    val colors = AppTokens.colors.chip.intensity

    val text = when (style) {
        IntensityChipStyle.SHORT -> ChipLabel.Empty
        IntensityChipStyle.LONG -> ChipLabel.Text(UiText.Str(AppTokens.strings.res(Res.string.intensity_chip)))
    }

    val trailing = when (style) {
        IntensityChipStyle.SHORT -> ChipTrailing.Icon(AppTokens.icons.FireFlame)
        IntensityChipStyle.LONG -> ChipTrailing.Icon(AppTokens.icons.FireFlame)
    }

    Chip(
        modifier = modifier,
        label = text,
        value = value.short(),
        trailing = trailing,
        size = size,
        stype = ChipStype.Default,
        contentColor = colors.contentColor,
        brush = Brush.horizontalGradient(
            colors = listOf(colors.startColor, colors.endColor)
        )
    )
}

@AppPreview
@Composable
private fun IntensityChipPreview() {
    PreviewContainer {
        IntensityChip(
            value = IntensityFormatState.of(85f),
            style = IntensityChipStyle.LONG,
            size = ChipSize.Small
        )

        IntensityChip(
            value = IntensityFormatState.of(85f),
            style = IntensityChipStyle.SHORT,
            size = ChipSize.Small
        )

        IntensityChip(
            value = IntensityFormatState.of(85f),
            style = IntensityChipStyle.LONG,
            size = ChipSize.Medium
        )

        IntensityChip(
            value = IntensityFormatState.of(85f),
            style = IntensityChipStyle.SHORT,
            size = ChipSize.Medium
        )
    }
}
