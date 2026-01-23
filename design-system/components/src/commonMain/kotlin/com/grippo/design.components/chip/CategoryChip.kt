package com.grippo.design.components.chip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.grippo.core.state.examples.CategoryEnumState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun CategoryChip(
    modifier: Modifier = Modifier,
    value: CategoryEnumState,
    size: ChipSize,
) {
    Chip(
        modifier = modifier,
        label = ChipLabel.Empty,
        value = value.title().text(),
        trailing = ChipTrailing.Content(
            lambda = {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .wrapContentSize()
                        .size(8.dp)
                        .background(
                            color = value.color(),
                            shape = CircleShape
                        )
                )
            }
        ),
        size = size,
        stype = ChipStype.Default,
        textColor = AppTokens.colors.static.white,
        iconColor = value.color(),
        brush = SolidColor(AppTokens.colors.background.card)
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
