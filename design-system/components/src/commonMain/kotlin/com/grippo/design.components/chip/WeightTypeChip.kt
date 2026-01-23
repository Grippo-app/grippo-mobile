package com.grippo.design.components.chip

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import com.grippo.core.state.examples.WeightTypeEnumState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Immutable
public enum class WeightTypeChipStyle {
    DEFAULT
}

@Composable
public fun WeightTypeChip(
    modifier: Modifier = Modifier,
    value: WeightTypeEnumState,
    size: ChipSize,
    style: WeightTypeChipStyle = WeightTypeChipStyle.DEFAULT
) {
    when (style) {
        WeightTypeChipStyle.DEFAULT -> Unit
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
private fun WeightTypeChipPreview() {
    PreviewContainer {
        WeightTypeChip(
            value = WeightTypeEnumState.FREE,
            size = ChipSize.Medium
        )

        WeightTypeChip(
            value = WeightTypeEnumState.FIXED,
            size = ChipSize.Medium
        )

        WeightTypeChip(
            value = WeightTypeEnumState.BODY_WEIGHT,
            size = ChipSize.Medium
        )
    }
}
