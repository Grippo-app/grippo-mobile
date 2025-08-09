package com.grippo.design.components.timeline

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.date.utils.DateCompose
import com.grippo.date.utils.DateFormat
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.icons.ClockOutline
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.training_at_value
import kotlinx.datetime.LocalDateTime

@Composable
public fun TimeLabel(
    modifier: Modifier = Modifier,
    value: LocalDateTime
) {
    val text = DateCompose.rememberFormat(value, DateFormat.HH_mm)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(AppTokens.dp.timeLabel.icon),
            imageVector = AppTokens.icons.ClockOutline,
            tint = AppTokens.colors.icon.secondary,
            contentDescription = null
        )

        Spacer(Modifier.width(AppTokens.dp.timeLabel.spacer))

        Text(
            text = AppTokens.strings.res(Res.string.training_at_value, text),
            style = AppTokens.typography.b13Bold(),
            color = AppTokens.colors.text.secondary
        )

        Spacer(Modifier.width(AppTokens.dp.contentPadding.subContent))
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