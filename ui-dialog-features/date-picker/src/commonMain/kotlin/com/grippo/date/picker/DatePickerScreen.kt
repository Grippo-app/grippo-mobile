package com.grippo.date.picker

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun DatePickerScreen(
    state: DatePickerState,
    loaders: ImmutableSet<DatePickerLoader>,
    contract: DatePickerContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        DatePickerScreen(
            state = DatePickerState(
                value = DateTimeFormatState.of(
                    value = DateTimeUtils.now(),
                    range = DateRangePresets.weekly(),
                    format = DateFormat.DateOnly.DateMmmDdYyyy
                ),
                limitations = DateRangePresets.last365Days(),
                title = "Select date",
            ),
            loaders = persistentSetOf(),
            contract = DatePickerContract.Empty
        )
    }
}
