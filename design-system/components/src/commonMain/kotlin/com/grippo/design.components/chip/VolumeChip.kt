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
import com.grippo.design.resources.tonnage_chip
import com.grippo.state.formatters.VolumeFormatState

@Immutable
public enum class VolumeChipStyle {
    SHORT,
    LONG
}

@Composable
public fun VolumeChip(
    modifier: Modifier = Modifier,
    value: VolumeFormatState,
    style: VolumeChipStyle
) {
    val colors = AppTokens.colors.chip.volume

    val text = when (style) {
        VolumeChipStyle.SHORT -> Label.Empty
        VolumeChipStyle.LONG -> Label.Text(UiText.Str(AppTokens.strings.res(Res.string.tonnage_chip)))
    }

    val trailing = when (style) {
        VolumeChipStyle.SHORT -> Trailing.Icon(AppTokens.icons.Weight)
        VolumeChipStyle.LONG -> Trailing.Icon(AppTokens.icons.Weight)
    }

    Chip(
        modifier = modifier,
        label = text,
        value = value.short(),
        trailing = trailing,
        contentColor = colors.contentColor,
        brush = Brush.horizontalGradient(
            colors = listOf(colors.startColor, colors.endColor)
        )
    )
}

@AppPreview
@Composable
private fun VolumeChipPreview() {
    PreviewContainer {
        VolumeChip(
            value = VolumeFormatState(1250.5f),
            style = VolumeChipStyle.LONG
        )

        VolumeChip(
            value = VolumeFormatState(1250.5f),
            style = VolumeChipStyle.SHORT
        )
    }
}
