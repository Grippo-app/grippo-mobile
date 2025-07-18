package com.grippo.design.components.chip

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    Chip(
        modifier = modifier,
        label = AppTokens.strings.res(Res.string.repetitions),
        value = value.toString(),
        icon = AppTokens.icons.Repeat,
        backgroundColor = AppTokens.colors.background.accent.copy(alpha = 0.1f),
        contentColor = AppTokens.colors.text.primary,
        borderColor = AppTokens.colors.border.defaultPrimary
    )
}

@AppPreview
@Composable
private fun ChipPreview() {
    PreviewContainer {
        RepetitionsChip(value = 12)
    }
}
