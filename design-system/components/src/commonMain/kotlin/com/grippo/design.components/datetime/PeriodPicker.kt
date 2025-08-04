package com.grippo.design.components.datetime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.grippo.date.utils.DateFormat
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.icons.NavArrowDown
import com.grippo.state.datetime.PeriodState

@Composable
public fun PeriodPicker(
    modifier: Modifier = Modifier,
    value: PeriodState,
    format: DateFormat,
    enabled: Boolean = true,
    onClick: () -> Unit
) {

    val shape = CircleShape

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

    val backgroundIconColor = when (enabled) {
        true -> AppTokens.colors.background.primary
        false -> Color.Transparent
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
                    .background(backgroundIconColor, shape)
                    .size(AppTokens.dp.periodPicker.icon)
                    .padding(2.dp),
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
            value = PeriodState.DAILY,
            format = DateFormat.MM_d,
            enabled = true,
            onClick = {}
        )

        PeriodPicker(
            value = PeriodState.DAILY,
            format = DateFormat.MM_d,
            enabled = false,
            onClick = {}
        )
    }
}