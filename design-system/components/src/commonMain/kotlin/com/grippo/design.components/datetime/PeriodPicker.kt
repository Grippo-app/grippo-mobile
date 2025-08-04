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
    onClick: () -> Unit
) {

    val shape = CircleShape

    Column(modifier = modifier.scalableClick(onClick = onClick)) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value.text(),
                style = AppTokens.typography.b16Bold(),
                color = AppTokens.colors.text.primary
            )

            Spacer(Modifier.width(AppTokens.dp.periodPicker.spacer))

            Icon(
                modifier = Modifier
                    .background(AppTokens.colors.background.primary, shape)
                    .size(AppTokens.dp.periodPicker.icon)
                    .padding(2.dp),
                imageVector = AppTokens.icons.NavArrowDown,
                tint = AppTokens.colors.icon.secondary,
                contentDescription = null
            )
        }

        Spacer(Modifier.width(AppTokens.dp.contentPadding.text))

        Text(
            text = value.range(DateFormat.MM_d),
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary
        )
    }
}

@AppPreview
@Composable
private fun DatePickerPreview() {
    PreviewContainer {
        PeriodPicker(
            value = PeriodState.DAILY,
            onClick = {}
        )
    }
}