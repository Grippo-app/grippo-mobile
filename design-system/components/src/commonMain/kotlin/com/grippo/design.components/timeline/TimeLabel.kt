package com.grippo.design.components.timeline

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.toolkit.date.utils.DateCompose
import com.grippo.toolkit.date.utils.DateFormat
import kotlinx.datetime.LocalDateTime

@Composable
public fun TimeLabel(
    modifier: Modifier = Modifier,
    value: LocalDateTime
) {
    val text = DateCompose.rememberFormat(value, DateFormat.DATE_TIME_DD_MMM_HH_MM)

    Text(
        modifier = modifier,
        text = text,
        style = AppTokens.typography.h6(),
        color = AppTokens.colors.text.secondary
    )
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