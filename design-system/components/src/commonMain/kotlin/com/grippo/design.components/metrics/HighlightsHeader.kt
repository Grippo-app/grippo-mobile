package com.grippo.design.components.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.design.components.datetime.PeriodPicker
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.progress
import com.grippo.toolkit.date.utils.DateRangeKind

@Composable
public fun HighlightsHeader(
    modifier: Modifier = Modifier,
    range: DateRangeFormatState,
    onPeriodChange: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = AppTokens.strings.res(Res.string.progress),
            style = AppTokens.typography.h4(),
            color = AppTokens.colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        PeriodPicker(
            value = range,
            onSelect = onPeriodChange
        )
    }
}

@AppPreview
@Composable
private fun HighlightsHeaderPreview() {
    PreviewContainer {
        HighlightsHeader(
            range = DateRangeFormatState.of(DateRangeKind.Last7Days),
            onPeriodChange = {},
        )
    }
}
