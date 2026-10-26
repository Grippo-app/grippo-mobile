package com.grippo.design.components.datetime

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.toolkit.date.utils.DateRangeKind
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

@Composable
public fun DialogRangeHeader(
    modifier: Modifier = Modifier,
    text: String,
) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = AppTokens.dp.dialog.horizontalPadding,
                end = AppTokens.dp.dialog.horizontalPadding,
                bottom = AppTokens.dp.contentPadding.content,
            ),
        text = text,
        style = AppTokens.typography.b14Med(),
        color = AppTokens.colors.text.secondary,
        textAlign = TextAlign.Center,
    )
}

@AppPreview
@Composable
private fun ShortRangePreview() {
    PreviewContainer {
        DialogRangeHeader(
            text = DateRangeFormatState.of(
                kind = DateRangeKind.Last7Days,
                anchor = LocalDate(year = 2026, month = 10, day = 17),
            ).shortStartEnd(),
        )
    }
}

@AppPreview
@Composable
private fun CrossMonthRangePreview() {
    PreviewContainer {
        DialogRangeHeader(
            text = DateRangeFormatState.of(
                kind = DateRangeKind.Last30Days,
                anchor = LocalDate(year = 2026, month = 5, day = 27),
            ).shortStartEnd(),
        )
    }
}

@AppPreview
@Composable
private fun LongRangePreview() {
    PreviewContainer {
        DialogRangeHeader(
            text = DateRangeFormatState.of(
                from = LocalDateTime(year = 2025, month = 9, day = 30, hour = 0, minute = 0),
                to = LocalDateTime(year = 2026, month = 10, day = 20, hour = 23, minute = 59),
            ).shortStartEnd(),
        )
    }
}
