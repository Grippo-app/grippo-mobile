package com.grippo.design.components.datetime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.grippo.date.utils.DateFormat
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

    when (enabled) {
        true -> AppTokens.colors.text.secondary
        false -> AppTokens.colors.text.disabled
    }

    when (enabled) {
        true -> AppTokens.colors.icon.tertiary
        false -> AppTokens.colors.icon.disabled
    }

    Row(
        modifier = modifier
            .background(
                AppTokens.colors.background.card,
                RoundedCornerShape(AppTokens.dp.periodPicker.radius)
            ).padding(
                horizontal = AppTokens.dp.periodPicker.horizontalPadding,
                vertical = AppTokens.dp.periodPicker.verticalPadding
            ),
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
private fun PeriodPickerPreview() {
    PreviewContainer {
        PeriodPicker(
            value = PeriodState.ThisDay,
            format = DateFormat.DATE_MMM_DD_COMMA,
            enabled = true,
            onClick = {}
        )

        PeriodPicker(
            value = PeriodState.ThisDay,
            format = DateFormat.DATE_MMM_DD_COMMA,
            enabled = false,
            onClick = {}
        )
    }
}