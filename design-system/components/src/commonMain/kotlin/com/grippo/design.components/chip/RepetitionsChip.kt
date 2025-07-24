package com.grippo.design.components.chip

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.icons.Repeat
import com.grippo.design.resources.repetitions

@Composable
public fun RepetitionsChip(
    modifier: Modifier = Modifier,
    value: Int
) {
    val colors = AppTokens.colors.chip.repetitions

    Chip(
        modifier = modifier,
        label = AppTokens.strings.res(Res.string.repetitions),
        value = value.toString(),
        trailing = Trailing.Icon(AppTokens.icons.Repeat),
        contentColor = colors.contentColor,
        borderColor = colors.borderColor,
        brush = Brush.horizontalGradient(
            colors = listOf(colors.startColor, colors.endColor)
        )
    )
}

@AppPreview
@Composable
private fun ChipPreview() {
    PreviewContainer {
        RepetitionsChip(value = 12)
    }
}
