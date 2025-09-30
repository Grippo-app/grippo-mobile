package com.grippo.design.components.datetime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.date.utils.DateFormat
import com.grippo.date.utils.DateRange
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.from
import com.grippo.design.resources.provider.to

@Composable
public fun DateRangeSelector(
    modifier: Modifier = Modifier,
    value: DateRange,
    enabled: Boolean,
    onFromClick: () -> Unit,
    onToClick: () -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {
        DatePicker(
            modifier = Modifier.weight(1f),
            title = AppTokens.strings.res(Res.string.from),
            value = value.from,
            format = DateFormat.DATE_MMM_DD_YYYY,
            enabled = enabled,
            onClick = onFromClick,
            onNextClick = {}, // TODO FIX
            onPreviousClick = {} // TODO FIX
        )

        DatePicker(
            modifier = Modifier.weight(1f),
            title = AppTokens.strings.res(Res.string.to),
            value = value.to,
            format = DateFormat.DATE_MMM_DD_YYYY,
            enabled = enabled,
            onClick = onToClick,
            onNextClick = {}, // TODO FIX
            onPreviousClick = {} // TODO FIX
        )
    }
}