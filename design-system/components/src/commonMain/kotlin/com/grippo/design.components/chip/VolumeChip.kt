package com.grippo.design.components.chip

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.Stack
import com.grippo.design.resources.provider.tonnage_chip

@Immutable
public enum class VolumeChipStyle {
    SHORT,
    LONG
}

@Composable
public fun VolumeChip(
    modifier: Modifier = Modifier,
    value: VolumeFormatState,
    size: ChipSize,
    style: VolumeChipStyle
) {
    val colors = AppTokens.colors.training.volume

    val text = when (style) {
        VolumeChipStyle.SHORT -> ChipLabel.Empty
        VolumeChipStyle.LONG -> ChipLabel.Text(UiText.Str(AppTokens.strings.res(Res.string.tonnage_chip)))
    }

    Chip(
        modifier = modifier,
        label = text,
        value = value.short(),
        trailing = ChipTrailing.Icon(AppTokens.icons.Stack),
        size = size,
        stype = ChipStype.Default,
        textColor = colors.contentColor,
        iconColor = colors.startColor,
        brush = SolidColor(AppTokens.colors.background.card)
    )
}

@AppPreview
@Composable
private fun VolumeChipPreview() {
    PreviewContainer {
        VolumeChip(
            value = VolumeFormatState.of(1250.5f),
            style = VolumeChipStyle.LONG,
            size = ChipSize.Small
        )

        VolumeChip(
            value = VolumeFormatState.of(1250.5f),
            style = VolumeChipStyle.SHORT,
            size = ChipSize.Small
        )

        VolumeChip(
            value = VolumeFormatState.of(1250.5f),
            style = VolumeChipStyle.LONG,
            size = ChipSize.Medium
        )

        VolumeChip(
            value = VolumeFormatState.of(1250.5f),
            style = VolumeChipStyle.SHORT,
            size = ChipSize.Medium
        )
    }
}
