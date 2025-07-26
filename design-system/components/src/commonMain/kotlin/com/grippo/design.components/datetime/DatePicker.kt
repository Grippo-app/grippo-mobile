package com.grippo.design.components.datetime

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import com.grippo.date.utils.DateCompose
import com.grippo.date.utils.DateFormat
import com.grippo.design.components.modifiers.ShadowElevation
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.modifiers.shadowDefault
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.icons.NavArrowDown
import kotlinx.datetime.LocalDateTime

@Composable
public fun DatePicker(
    modifier: Modifier = Modifier,
    value: LocalDateTime,
    onClick: () -> Unit
) {
    val text = DateCompose.rememberFormat(value, DateFormat.uuuu_MM_d)

    val shape = CircleShape

    Row(
        modifier = modifier.scalableClick(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = text,
            style = AppTokens.typography.b16Bold(),
            color = AppTokens.colors.text.primary
        )

        Spacer(Modifier.width(AppTokens.dp.datePicker.spacer))

        Icon(
            modifier = Modifier
                .shadowDefault(
                    elevation = ShadowElevation.Card,
                    shape = shape,
                    color = AppTokens.colors.overlay.defaultShadow
                )
                .background(AppTokens.colors.background.primary, shape)
                .border(1.dp, AppTokens.colors.border.defaultPrimary, shape)
                .size(AppTokens.dp.datePicker.icon)
                .padding(2.dp),
            imageVector = AppTokens.icons.NavArrowDown,
            tint = AppTokens.colors.icon.primary,
            contentDescription = null
        )
    }
}

@AppPreview
@Composable
private fun DatePickerPreview() {
    PreviewContainer {
        DatePicker(
            value = LocalDateTime(2025, 7, 9, 14, 30),
            onClick = {}
        )
    }
}