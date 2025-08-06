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
import com.grippo.design.resources.icons.Repeat
import com.grippo.design.resources.repetitions_chip
import com.grippo.state.formatters.RepetitionsFormatState

@Immutable
public enum class RepetitionsChipStyle {
    SHORT,
    LONG
}

@Composable
public fun RepetitionsChip(
    modifier: Modifier = Modifier,
    value: RepetitionsFormatState,
    style: RepetitionsChipStyle
) {
    val colors = AppTokens.colors.chip.repetitions

    val text = when (style) {
        RepetitionsChipStyle.SHORT -> Label.Empty
        RepetitionsChipStyle.LONG -> Label.Text(UiText.Str(AppTokens.strings.res(Res.string.repetitions_chip)))
    }

    val trailing = when (style) {
        RepetitionsChipStyle.SHORT -> Trailing.Icon(AppTokens.icons.Repeat)
        RepetitionsChipStyle.LONG -> Trailing.Icon(AppTokens.icons.Repeat)
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
private fun RepetitionsChipPreview() {
    PreviewContainer {
        RepetitionsChip(
            value = RepetitionsFormatState(12),
            style = RepetitionsChipStyle.LONG
        )

        RepetitionsChip(
            value = RepetitionsFormatState(12),
            style = RepetitionsChipStyle.SHORT
        )
    }
}
