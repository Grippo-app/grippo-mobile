package com.grippo.design.components.datetime

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.grippo.date.utils.DateCompose
import com.grippo.date.utils.DateFormat
import com.grippo.date.utils.DateRange
import com.grippo.date.utils.DateTimeUtils
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonColorTokens
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonState
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
    value: LocalDateTime,
    format: DateFormat,
    limitations: DateRange,
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

        val previousState = remember(limitations, value) {
            if (value.date == limitations.from.date) {
                ButtonState.Disabled
            } else ButtonState.Enabled
        }

        Button(
            content = ButtonContent.Icon(
                icon = AppTokens.icons.NavArrowLeft
            ),
            size = ButtonSize.Small,
            style = ButtonStyle.Custom(
                enabled = ButtonColorTokens(
                    background1 = Color.Transparent,
                    background2 = Color.Transparent,
                    content = AppTokens.colors.text.primary,
                    border = Color.Transparent,
                    icon = AppTokens.colors.icon.primary,
                ),
                disabled = ButtonColorTokens(
                    background1 = Color.Transparent,
                    background2 = Color.Transparent,
                    content = AppTokens.colors.text.disabled,
                    border = Color.Transparent,
                    icon = AppTokens.colors.icon.disabled
                ),
            ),
            onClick = onPreviousClick,
            state = previousState
        )

        val nextState = remember(limitations, value) {
            if (value.date == limitations.to.date) {
                ButtonState.Disabled
            } else ButtonState.Enabled
        }

        Button(
            content = ButtonContent.Icon(
                icon = AppTokens.icons.NavArrowRight
            ),
            size = ButtonSize.Small,
            style = ButtonStyle.Custom(
                enabled = ButtonColorTokens(
                    background1 = Color.Transparent,
                    background2 = Color.Transparent,
                    content = AppTokens.colors.text.primary,
                    border = Color.Transparent,
                    icon = AppTokens.colors.icon.primary,
                ),
                disabled = ButtonColorTokens(
                    background1 = Color.Transparent,
                    background2 = Color.Transparent,
                    content = AppTokens.colors.text.disabled,
                    border = Color.Transparent,
                    icon = AppTokens.colors.icon.disabled
                ),
            ),
            onClick = onNextClick,
            state = nextState
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
                    background1 = Color.Transparent,
                    background2 = Color.Transparent,
                    content = AppTokens.colors.text.primary,
                    border = Color.Transparent,
                    icon = AppTokens.colors.icon.primary,
                ),
                disabled = ButtonColorTokens(
                    background1 = Color.Transparent,
                    background2 = Color.Transparent,
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
            enabled = true,
            onClick = {},
            onNextClick = {},
            onPreviousClick = {},
            limitations = DateTimeUtils.thisWeek()
        )

        DatePicker(
            value = DateTimeUtils.now(),
            format = DateFormat.DATE_MMM_DD_YYYY,
            enabled = false,
            onClick = {},
            onNextClick = {},
            onPreviousClick = {},
            limitations = DateTimeUtils.thisWeek()
        )
    }
}