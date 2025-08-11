package com.grippo.design.components.datetime

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.date.utils.DateFormat
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.NavArrowDown
import com.grippo.state.datetime.PeriodState

@Composable
public fun PeriodPicker(
    modifier: Modifier = Modifier,
    value: PeriodState,
    format: DateFormat,
    enabled: Boolean = true,
    onClick: () -> Unit
) {

    val titleColor = when (enabled) {
        true -> AppTokens.colors.text.primary
        false -> AppTokens.colors.text.disabled
    }

    val descriptionColor = when (enabled) {
        true -> AppTokens.colors.text.secondary
        false -> AppTokens.colors.text.disabled
    }

    val iconColor = when (enabled) {
        true -> AppTokens.colors.icon.secondary
        false -> AppTokens.colors.icon.disabled
    }

    Column(modifier = modifier.scalableClick(onClick = onClick)) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value.text(),
                style = AppTokens.typography.b16Bold(),
                color = titleColor
            )

            Spacer(Modifier.width(AppTokens.dp.periodPicker.spacer))

            Icon(
                modifier = Modifier
                    .size(AppTokens.dp.periodPicker.icon),
                imageVector = AppTokens.icons.NavArrowDown,
                tint = iconColor,
                contentDescription = null
            )
        }

        Spacer(Modifier.width(AppTokens.dp.contentPadding.text))

        Text(
            text = value.range(format),
            style = AppTokens.typography.b13Med(),
            color = descriptionColor
        )
    }
}

@AppPreview
@Composable
private fun DatePickerPreview() {
    PreviewContainer {
        PeriodPicker(
            value = PeriodState.ThisDay,
            format = DateFormat.MMM_d,
            enabled = true,
            onClick = {}
        )

        PeriodPicker(
            value = PeriodState.ThisDay,
            format = DateFormat.MMM_d,
            enabled = false,
            onClick = {}
        )
    }
}