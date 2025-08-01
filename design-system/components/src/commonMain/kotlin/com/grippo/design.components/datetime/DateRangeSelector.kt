package com.grippo.design.components.datetime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.date.utils.DateCompose
import com.grippo.date.utils.DateFormat
import com.grippo.date.utils.DateRange
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.Res
import com.grippo.design.resources.from_value
import com.grippo.design.resources.to_value

@Composable
public fun DateRangeSelector(
    modifier: Modifier = Modifier,
    value: DateRange,
    onFromClick: () -> Unit,
    onToClick: () -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {
        val formattedFrom = DateCompose.rememberFormat(value = value.from, DateFormat.MM_d)
        val from = AppTokens.strings.res(Res.string.from_value, formattedFrom)

        Button(
            modifier = Modifier.weight(1f),
            style = ButtonStyle.Secondary,
            onClick = onFromClick,
            text = from
        )

        val formattedTo = DateCompose.rememberFormat(value = value.to, DateFormat.MM_d)
        val to = AppTokens.strings.res(Res.string.to_value, formattedTo)

        Button(
            modifier = Modifier.weight(1f),
            style = ButtonStyle.Secondary,
            onClick = onToClick,
            text = to
        )
    }
}