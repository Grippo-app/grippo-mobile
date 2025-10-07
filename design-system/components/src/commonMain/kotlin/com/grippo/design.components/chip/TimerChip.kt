package com.grippo.design.components.chip

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.grippo.date.utils.DateTimeUtils
import com.grippo.date.utils.timerTextFlow
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Timer
import kotlinx.datetime.LocalDateTime

@Composable
public fun TimerChip(
    modifier: Modifier = Modifier,
    value: LocalDateTime,
    size: ChipSize,
) {
    val colors = AppTokens.colors.chip.timer

    val timerFlow = remember(value) { timerTextFlow(start = value) }
    val time = timerFlow.collectAsState(initial = "")

    Chip(
        modifier = modifier,
        label = ChipLabel.Empty,
        value = time.value,
        trailing = ChipTrailing.Icon(AppTokens.icons.Timer),
        size = size,
        stype = ChipStype.Default,
        contentColor = colors.contentColor,
        brush = Brush.horizontalGradient(
            colors = listOf(colors.startColor, colors.endColor)
        )
    )
}

@AppPreview
@Composable
private fun TimerChipPreview() {
    PreviewContainer {
        TimerChip(
            value = DateTimeUtils.now(),
            size = ChipSize.Small
        )

        TimerChip(
            value = DateTimeUtils.now(),
            size = ChipSize.Medium
        )
    }
}
