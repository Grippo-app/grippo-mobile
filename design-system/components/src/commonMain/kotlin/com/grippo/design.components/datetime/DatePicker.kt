package com.grippo.design.components.datetime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.accessibility_next_period
import com.grippo.design.resources.provider.accessibility_previous_period
import com.grippo.design.resources.provider.icons.ArrowLeft
import com.grippo.design.resources.provider.icons.ArrowRight
import com.grippo.toolkit.date.utils.DateCompose
import com.grippo.toolkit.date.utils.DateFormat
import kotlinx.datetime.LocalDateTime

@Composable
public fun DatePicker(
    modifier: Modifier = Modifier,
    value: LocalDateTime,
    format: DateFormat.DateOnly,
    enabled: Boolean = true,
    onSelect: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    val text = DateCompose.rememberFormat(value.date, format)
    val previousLabel = AppTokens.strings.res(Res.string.accessibility_previous_period)
    val nextLabel = AppTokens.strings.res(Res.string.accessibility_next_period)
    val buttonState = if (enabled) ButtonState.Enabled else ButtonState.Disabled

    val titleColor = when (enabled) {
        true -> AppTokens.colors.text.primary
        false -> AppTokens.colors.text.disabled
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(
            modifier = Modifier.semantics { contentDescription = previousLabel },
            content = ButtonContent.Icon(
                icon = ButtonIcon.Icon(AppTokens.icons.ArrowLeft)
            ),
            size = ButtonSize.Small,
            style = ButtonStyle.Transparent,
            state = buttonState,
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
            modifier = Modifier.semantics { contentDescription = nextLabel },
            content = ButtonContent.Icon(
                icon = ButtonIcon.Icon(AppTokens.icons.ArrowRight)
            ),
            size = ButtonSize.Small,
            style = ButtonStyle.Transparent,
            state = buttonState,
            onClick = onNext
        )
    }
}

@AppPreview
@Composable
private fun DatePickerPreview() {
    PreviewContainer {
        DatePicker(
            value = LocalDateTime(2026, 1, 15, 12, 0),
            format = DateFormat.DateOnly.DateMmmDdYyyy,
            enabled = true,
            onSelect = {},
            onNext = {},
            onPrevious = {}
        )

        DatePicker(
            value = LocalDateTime(2026, 1, 15, 12, 0),
            format = DateFormat.DateOnly.DateMmmDdYyyy,
            enabled = false,
            onSelect = {},
            onNext = {},
            onPrevious = {}
        )
    }
}
