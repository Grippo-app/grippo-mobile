package com.grippo.design.components.datetime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.ArrowDown
import com.grippo.toolkit.date.utils.DateRangeKind

@Composable
public fun PeriodPicker(
    modifier: Modifier = Modifier,
    value: DateRangeFormatState,
    enabled: Boolean = true,
    onSelect: () -> Unit,
) {

    val titleColor = when (enabled) {
        true -> AppTokens.colors.text.primary
        false -> AppTokens.colors.text.disabled
    }

    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                role = Role.Button
                if (!enabled) disabled()
            }
            .scalableClick(enabled = enabled, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = value.label()?.text() ?: "-",
            style = AppTokens.typography.h6(),
            color = titleColor
        )

        Spacer(Modifier.width(AppTokens.dp.contentPadding.text))

        Icon(
            modifier = Modifier.size(AppTokens.dp.periodPicker.icon),
            imageVector = AppTokens.icons.ArrowDown,
            contentDescription = null,
            tint = if (enabled) AppTokens.colors.icon.secondary else AppTokens.colors.icon.disabled,
        )
    }
}

@AppPreview
@Composable
private fun PeriodPickerPreview() {
    PreviewContainer {
        PeriodPicker(
            value = DateRangeFormatState.of(DateRangeKind.Last7Days),
            enabled = true,
            onSelect = {},
        )

        PeriodPicker(
            value = DateRangeFormatState.of(DateRangeKind.Last7Days),
            enabled = false,
            onSelect = {},
        )
    }
}
