package com.grippo.design.components.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.grippo.date.utils.DateCompose
import com.grippo.date.utils.DateFormat
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.icons.ClockOutline
import com.grippo.design.resources.training_at_value
import kotlinx.datetime.LocalDateTime

@Composable
public fun TimeLabel(
    modifier: Modifier = Modifier,
    value: LocalDateTime
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(AppTokens.colors.background.accent)
            .padding(
                vertical = AppTokens.dp.timeLabel.verticalPadding,
                horizontal = AppTokens.dp.timeLabel.horizontalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val text = DateCompose.rememberFormat(value, DateFormat.HH_MMM)

        Icon(
            modifier = Modifier.size(AppTokens.dp.timeLabel.icon),
            imageVector = AppTokens.icons.ClockOutline,
            tint = AppTokens.colors.icon.inverted,
            contentDescription = null
        )

        Spacer(Modifier.width(AppTokens.dp.timeLabel.verticalPadding))

        Text(
            text = AppTokens.strings.res(Res.string.training_at_value, text),
            style = AppTokens.typography.b13Bold(),
            color = AppTokens.colors.text.inverted
        )

        Spacer(Modifier.width(AppTokens.dp.timeLabel.verticalPadding))
    }
}

@AppPreview
@Composable
private fun TimeLabelPreview() {
    PreviewContainer {
        TimeLabel(
            value = LocalDateTime(2025, 7, 9, 14, 30)
        )
    }
}