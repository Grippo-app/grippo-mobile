package com.grippo.design.components.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp

private const val LINE_SAMPLE = "0"

// Measured instead of derived from style.lineHeight because Android trims the line-height spacing of the outer lines.
@Composable
public fun rememberTextHeight(
    style: TextStyle,
    lines: Int = 1,
    textMeasurer: TextMeasurer = rememberTextMeasurer(),
): Dp {
    require(lines > 0) { "Text height needs at least one line, got $lines" }
    val density = LocalDensity.current

    return remember(style, lines, textMeasurer, density) {
        val sample = List(lines) { LINE_SAMPLE }.joinToString(separator = "\n")
        val layout = textMeasurer.measure(text = sample, style = style, maxLines = lines)
        with(density) { layout.size.height.toDp() }
    }
}
