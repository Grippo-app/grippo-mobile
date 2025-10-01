package com.grippo.design.components.datetime

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.grippo.date.utils.DateCompose
import com.grippo.date.utils.DateFormat
import com.grippo.date.utils.DateTimeUtils
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonColorTokens
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.NavArrowDown
import com.grippo.design.resources.provider.icons.NavArrowLeft
import com.grippo.design.resources.provider.icons.NavArrowRight
import kotlinx.datetime.LocalDateTime

@Composable
public fun DatePicker(
    modifier: Modifier = Modifier,
    title: String,
    value: LocalDateTime,
    format: DateFormat,
    enabled: Boolean = true,
    onClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
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
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            content = ButtonContent.Icon(
                icon = AppTokens.icons.NavArrowLeft
            ),
            size = ButtonSize.Small,
            style = ButtonStyle.Custom(
                enabled = ButtonColorTokens(
                    background = Color.Transparent,
                    content = AppTokens.colors.text.primary,
                    border = Color.Transparent,
                    icon = AppTokens.colors.icon.primary,
                ),
                disabled = ButtonColorTokens(
                    background = Color.Transparent,
                    content = AppTokens.colors.text.disabled,
                    border = Color.Transparent,
                    icon = AppTokens.colors.icon.disabled
                ),
            ),
            onClick = onPreviousClick
        )

        Button(
            content = ButtonContent.Icon(
                icon = AppTokens.icons.NavArrowRight
            ),
            size = ButtonSize.Small,
            style = ButtonStyle.Custom(
                enabled = ButtonColorTokens(
                    background = Color.Transparent,
                    content = AppTokens.colors.text.primary,
                    border = Color.Transparent,
                    icon = AppTokens.colors.icon.primary,
                ),
                disabled = ButtonColorTokens(
                    background = Color.Transparent,
                    content = AppTokens.colors.text.disabled,
                    border = Color.Transparent,
                    icon = AppTokens.colors.icon.disabled
                ),
            ),
            onClick = onNextClick
        )

        Spacer(Modifier.width(AppTokens.dp.contentPadding.content))

        Text(
            modifier = Modifier.scalableClick(
                enabled = enabled,
                onClick = onClick
            ),
            text = text,
            style = AppTokens.typography.h6(),
            color = titleColor
        )

        Spacer(Modifier.width(AppTokens.dp.contentPadding.text))

        Button(
            content = ButtonContent.Icon(
                icon = AppTokens.icons.NavArrowDown
            ),
            size = ButtonSize.Small,
            style = ButtonStyle.Custom(
                enabled = ButtonColorTokens(
                    background = Color.Transparent,
                    content = AppTokens.colors.text.primary,
                    border = Color.Transparent,
                    icon = AppTokens.colors.icon.primary,
                ),
                disabled = ButtonColorTokens(
                    background = Color.Transparent,
                    content = AppTokens.colors.text.disabled,
                    border = Color.Transparent,
                    icon = AppTokens.colors.icon.disabled
                ),
            ),
            onClick = onClick
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
            onClick = {},
            onNextClick = {},
            onPreviousClick = {}
        )

        DatePicker(
            value = DateTimeUtils.now(),
            format = DateFormat.DATE_MMM_DD_YYYY,
            title = "Text",
            enabled = false,
            onClick = {},
            onNextClick = {},
            onPreviousClick = {}
        )
    }
}