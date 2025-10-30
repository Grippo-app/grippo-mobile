package com.grippo.design.components.datetime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.design.core.AppTokens
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils

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
            value = value.from,
            format = DateFormat.DATE_MMM_DD_YYYY,
            enabled = enabled,
            onClick = onFromClick,
            limitations = DateTimeUtils.thisWeek() // TODO FIX
        )

        DatePicker(
            modifier = Modifier.weight(1f),
            value = value.to,
            format = DateFormat.DATE_MMM_DD_YYYY,
            enabled = enabled,
            onClick = onToClick,
            limitations = DateTimeUtils.thisWeek() // TODO FIX
        )
    }
}