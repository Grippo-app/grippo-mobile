package com.grippo.design.components.datetime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.grippo.design.resources.provider.icons.ArrowDown
import com.grippo.toolkit.date.utils.DateRange

@Composable
public fun PeriodPicker(
    modifier: Modifier = Modifier,
    value: DateRange.Range,
    enabled: Boolean = true,
    onSelect: () -> Unit,
) {

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
        modifier = modifier.scalableClick(
            enabled = enabled,
            onClick = onSelect
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = value.range?.label() ?: "-",
            style = AppTokens.typography.h6(),
            color = titleColor
        )

        Spacer(Modifier.width(AppTokens.dp.contentPadding.subContent))

        val buttonState = remember(enabled) {
            when (enabled) {
                true -> ButtonState.Enabled
                false -> ButtonState.Disabled
            }
        }

        Button(
            content = ButtonContent.Icon(
                icon = ButtonIcon.Icon(AppTokens.icons.ArrowDown)
            ),
            state = buttonState,
            size = ButtonSize.Small,
            style = ButtonStyle.Transparent,
            onClick = onSelect
        )
    }
}

@AppPreview
@Composable
private fun PeriodPickerPreview() {
    PreviewContainer {
        PeriodPicker(
            value = DateRange.Range.Last7Days(),
            enabled = true,
            onSelect = {},
        )

        PeriodPicker(
            value = DateRange.Range.Last7Days(),
            enabled = false,
            onSelect = {},
        )
    }
}
