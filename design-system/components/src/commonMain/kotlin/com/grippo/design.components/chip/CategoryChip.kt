package com.grippo.design.components.chip

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import com.grippo.core.state.examples.CategoryEnumState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Immutable
public enum class CategoryChipStyle {
    DEFAULT
}

@Composable
public fun CategoryChip(
    modifier: Modifier = Modifier,
    value: CategoryEnumState,
    size: ChipSize,
    style: CategoryChipStyle = CategoryChipStyle.DEFAULT
) {
    when (style) {
        CategoryChipStyle.DEFAULT -> Unit
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
private fun CategoryChipPreview() {
    PreviewContainer {
        CategoryChip(
            value = CategoryEnumState.COMPOUND,
            size = ChipSize.Medium
        )

        CategoryChip(
            value = CategoryEnumState.ISOLATION,
            size = ChipSize.Medium
        )
    }
}
