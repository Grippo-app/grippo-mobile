package com.grippo.design.components.chip

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import com.grippo.core.state.examples.CategoryEnumState
import com.grippo.design.components.indicators.Dot
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
        leading = ChipLeading.Content(
            content = {
                Dot(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .wrapContentSize(),
                    color = value.color(),
                )
            }
        ),
        size = size,
        interaction = ChipInteraction.Static,
        textColor = AppTokens.colors.text.primary,
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

@AppPreview
@Composable
private fun CategoryChipSmallPreview() {
    PreviewContainer {
        CategoryEnumState.entries.forEach { value ->
            CategoryChip(value = value, size = ChipSize.Small)
        }
    }
}
