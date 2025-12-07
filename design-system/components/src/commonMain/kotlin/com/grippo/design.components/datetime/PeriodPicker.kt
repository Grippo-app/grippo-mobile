package com.grippo.design.components.datetime

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.state.datetime.PeriodState
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.NavArrowDown
import com.grippo.toolkit.date.utils.DateFormat

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
        Spacer(Modifier.width(AppTokens.dp.contentPadding.subContent))

        Text(
            modifier = Modifier.scalableClick(
                enabled = enabled,
                onClick = onClick
            ),
            text = value.label(),
            style = AppTokens.typography.h6(),
            color = titleColor
        )

        Spacer(Modifier.width(AppTokens.dp.contentPadding.text))

        Button(
            content = ButtonContent.Icon(
                icon = ButtonIcon.Icon(AppTokens.icons.NavArrowDown)
            ),
            size = ButtonSize.Small,
            style = ButtonStyle.Transparent,
            onClick = onClick
        )
    }
}

@AppPreview
@Composable
private fun PeriodPickerPreview() {
    PreviewContainer {
        PeriodPicker(
            value = PeriodState.ThisDay,
            format = DateFormat.DateOnly.DateMmmDdComma,
            enabled = true,
            onClick = {}
        )

        PeriodPicker(
            value = PeriodState.ThisDay,
            format = DateFormat.DateOnly.DateMmmDdComma,
            enabled = false,
            onClick = {}
        )
    }
}
