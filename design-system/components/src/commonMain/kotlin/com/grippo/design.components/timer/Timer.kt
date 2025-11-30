package com.grippo.design.components.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Timer
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

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.timeLabel.spacer)
    ) {
        Icon(
            modifier = Modifier.size(AppTokens.dp.timeLabel.icon),
            tint = AppTokens.colors.icon.secondary,
            imageVector = AppTokens.icons.Timer,
            contentDescription = null
        )

        Text(
            text = time.value,
            style = AppTokens.typography.b14Semi(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = AppTokens.colors.text.secondary
        )
    }
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
