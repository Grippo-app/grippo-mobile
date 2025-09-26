package com.grippo.design.components.datetime

import androidx.compose.foundation.layout.Arrangement
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
import com.grippo.date.utils.DateCompose
import com.grippo.date.utils.DateFormat
import com.grippo.date.utils.DateTimeUtils
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.NavArrowDown
import kotlinx.datetime.LocalDateTime

@Composable
public fun DatePicker(
    modifier: Modifier = Modifier,
    title: String,
    value: LocalDateTime,
    format: DateFormat,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val text = DateCompose.rememberFormat(value, format)

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

    Column(
        modifier = modifier.scalableClick(enabled = enabled, onClick = onClick),
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = AppTokens.typography.h5(),
                color = titleColor
            )

            Spacer(Modifier.width(AppTokens.dp.datePicker.spacer))

            Icon(
                modifier = Modifier.size(AppTokens.dp.datePicker.icon),
                imageVector = AppTokens.icons.NavArrowDown,
                tint = iconColor,
                contentDescription = null
            )
        }

        Spacer(Modifier.width(AppTokens.dp.contentPadding.text))

        Text(
            text = text,
            style = AppTokens.typography.b14Med(),
            color = descriptionColor
        )
    }
}

@AppPreview
@Composable
private fun DatePickerPreview() {
    PreviewContainer {
        DatePicker(
            value = DateTimeUtils.now(),
            format = DateFormat.DATE_MMM_DD_YYYY,
            title = "Text",
            enabled = true,
            onClick = {}
        )

        DatePicker(
            value = DateTimeUtils.now(),
            format = DateFormat.DATE_MMM_DD_YYYY,
            title = "Text",
            enabled = false,
            onClick = {}
        )
    }
}