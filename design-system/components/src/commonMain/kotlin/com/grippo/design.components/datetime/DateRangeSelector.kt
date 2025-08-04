package com.grippo.design.components.datetime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.date.utils.DateCompose
import com.grippo.date.utils.DateFormat
import com.grippo.date.utils.DateRange
import com.grippo.design.components.internal.Input
import com.grippo.design.components.internal.InputStyle
import com.grippo.design.components.internal.PlaceHolder
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.Res
import com.grippo.design.resources.from
import com.grippo.design.resources.to

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
        val formattedFrom = DateCompose.rememberFormat(value = value.from, DateFormat.uuuu_MM_d)

        Input(
            modifier = Modifier.weight(1f),
            inputStyle = InputStyle.Clickable(onClick = onFromClick),
            value = formattedFrom,
            placeholder = PlaceHolder.OverInput(AppTokens.strings.res(Res.string.from))
        )

        val formattedTo = DateCompose.rememberFormat(value = value.to, DateFormat.uuuu_MM_d)

        Input(
            modifier = Modifier.weight(1f),
            inputStyle = InputStyle.Clickable(onClick = onToClick),
            value = formattedTo,
            placeholder = PlaceHolder.OverInput(AppTokens.strings.res(Res.string.to))
        )
    }
}