package com.grippo.design.components.chip

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import com.grippo.core.state.examples.ForceTypeEnumState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Immutable
public enum class ForceTypeChipStyle {
    DEFAULT
}

@Composable
public fun ForceTypeChip(
    modifier: Modifier = Modifier,
    value: ForceTypeEnumState,
    size: ChipSize,
    style: ForceTypeChipStyle = ForceTypeChipStyle.DEFAULT
) {
    when (style) {
        ForceTypeChipStyle.DEFAULT -> Unit
    }

    Chip(
        modifier = modifier,
        label = ChipLabel.Empty,
        value = value.title().text(),
        trailing = ChipTrailing.Empty,
        size = size,
        stype = ChipStype.Default,
        textColor = AppTokens.colors.static.white,
        iconColor = AppTokens.colors.static.white,
        brush = SolidColor(value.color())
    )
}

@AppPreview
@Composable
private fun ForceTypeChipPreview() {
    PreviewContainer {
        ForceTypeChip(
            value = ForceTypeEnumState.PULL,
            size = ChipSize.Medium
        )

        ForceTypeChip(
            value = ForceTypeEnumState.PUSH,
            size = ChipSize.Medium
        )

        ForceTypeChip(
            value = ForceTypeEnumState.HINGE,
            size = ChipSize.Medium
        )
    }
}
