package com.grippo.design.components.datetime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.ArrowLeft
import com.grippo.design.resources.provider.icons.ArrowRight
import com.grippo.toolkit.date.utils.DateCompose
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.datetime.LocalDateTime

@Composable
public fun DatePicker(
    modifier: Modifier = Modifier,
    value: LocalDateTime,
    format: DateFormat.DateOnly,
    limitations: DateRange,
    enabled: Boolean = true,
    onSelect: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    val text = DateCompose.rememberFormat(value.date, format)

    val titleColor = when (enabled) {
        true -> AppTokens.colors.text.primary
        false -> AppTokens.colors.text.disabled
    }

    when (enabled) {
        true -> AppTokens.colors.text.secondary
        false -> AppTokens.colors.text.disabled
    }

    when (enabled) {
        true -> AppTokens.colors.icon.tertiary
        false -> AppTokens.colors.icon.disabled
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            content = ButtonContent.Icon(
                icon = ButtonIcon.Icon(AppTokens.icons.ArrowLeft)
            ),
            size = ButtonSize.Small,
            style = ButtonStyle.Transparent,
            onClick = onPrevious
        )

        Spacer(Modifier.width(AppTokens.dp.contentPadding.subContent))

        Text(
            modifier = Modifier.scalableClick(
                enabled = enabled,
                onClick = onSelect
            ),
            text = text,
            style = AppTokens.typography.h6(),
            color = titleColor
        )

        Spacer(Modifier.width(AppTokens.dp.contentPadding.subContent))

        Button(
            content = ButtonContent.Icon(
                icon = ButtonIcon.Icon(AppTokens.icons.ArrowRight)
            ),
            size = ButtonSize.Small,
            style = ButtonStyle.Transparent,
            onClick = onNext
        )
    }
}

@AppPreview
@Composable
private fun DatePickerPreview() {
    PreviewContainer {
        DatePicker(
            value = DateTimeUtils.now(),
            format = DateFormat.DateOnly.DateMmmDdYyyy,
            enabled = true,
            onSelect = {},
            limitations = DateTimeUtils.thisWeek(),
            onNext = {},
            onPrevious = {}
        )

        DatePicker(
            value = DateTimeUtils.now(),
            format = DateFormat.DateOnly.DateMmmDdYyyy,
            enabled = false,
            onSelect = {},
            limitations = DateTimeUtils.thisWeek(),
            onNext = {},
            onPrevious = {}
        )
    }
}
