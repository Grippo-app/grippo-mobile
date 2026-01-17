package com.grippo.design.components.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.components.datetime.PeriodPicker
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.Intensity
import com.grippo.design.resources.provider.insights
import com.grippo.toolkit.date.utils.DateRange

@Composable
public fun HighlightsHeader(
    modifier: Modifier = Modifier,
    range: DateRange.Range,
    onPeriodChange: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(AppTokens.dp.metrics.highlights.icon),
            imageVector = AppTokens.icons.Intensity,
            tint = AppTokens.colors.semantic.warning,
            contentDescription = null
        )

        Text(
            modifier = Modifier.weight(1f),
            text = AppTokens.strings.res(Res.string.insights),
            style = AppTokens.typography.h4(),
            color = AppTokens.colors.semantic.warning,
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
            range = DateRange.Range.Last7Days(),
            onPeriodChange = {},
        )
    }
}
