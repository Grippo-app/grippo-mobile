package com.grippo.design.components.timer

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.toolkit.date.utils.DateTimeUtils
import com.grippo.toolkit.date.utils.timerTextFlow
import kotlinx.datetime.LocalDateTime

@Composable
public fun Timer(
    modifier: Modifier = Modifier,
    value: LocalDateTime,
) {
    val timerFlow = remember(value) { timerTextFlow(start = value) }
    val time = timerFlow.collectAsState(initial = "")

    Text(
        text = time.value,
        style = AppTokens.typography.b14Semi(),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = AppTokens.colors.text.secondary
    )
}

@AppPreview
@Composable
private fun TimerPreview() {
    PreviewContainer {
        Timer(
            value = DateTimeUtils.now(),
        )
    }
}
