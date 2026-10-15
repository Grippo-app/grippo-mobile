package com.grippo.date.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.date.picker.internal.DateWheelPicker
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.toolbar.BottomSheetToolbar
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.continue_btn
import com.grippo.design.resources.provider.select_date
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.datetime.LocalDateTime

@Composable
internal fun DatePickerScreen(
    state: DatePickerState,
    loaders: ImmutableSet<DatePickerLoader>,
    contract: DatePickerContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {
    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

    DateWheelPicker(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .height(AppTokens.dp.wheelPicker.height)
            .fillMaxWidth(),
        initial = state.value.value,
        select = contract::onSelectDate,
        limitations = state.limitations
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    val buttonState = remember(loaders, state.value) {
        when (state.value) {
            is DateTimeFormatState.Invalid -> ButtonState.Disabled
            is DateTimeFormatState.Empty -> ButtonState.Disabled
            else -> ButtonState.Enabled
        }
    }

    Button(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth(),
        content = ButtonContent.Text(
            text = AppTokens.strings.res(Res.string.continue_btn),
        ),
        style = ButtonStyle.Primary,
        state = buttonState,
        onClick = contract::onSubmitClick
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))

    Spacer(modifier = Modifier.navigationBarsPadding())
}

@AppPreview
@Composable
private fun ScreenPreview() {
    DatePickerPreview(value = LocalDateTime(2026, 9, 18, 12, 0))
}

@AppPreview
@Composable
private fun ScreenLowerBoundPreview() {
    DatePickerPreview(value = LocalDateTime(2025, 9, 5, 0, 0))
}

@AppPreview
@Composable
private fun ScreenUpperBoundPreview() {
    DatePickerPreview(value = LocalDateTime(2026, 10, 21, 23, 59))
}

@AppPreview
@Composable
private fun ScreenEmptyPreview() {
    DatePickerPreview(value = null)
}

@AppPreview
@Composable
private fun ScreenInvalidPreview() {
    DatePickerPreview(value = LocalDateTime(2027, 1, 1, 12, 0))
}

@Composable
private fun DatePickerPreview(value: LocalDateTime?) {
    val limitations = DateRange(
        from = LocalDateTime(2025, 9, 5, 0, 0),
        to = LocalDateTime(2026, 10, 21, 23, 59),
    )

    PreviewContainer {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = AppTokens.colors.background.dialog,
                    shape = RoundedCornerShape(
                        topStart = AppTokens.dp.bottomSheet.radius,
                        topEnd = AppTokens.dp.bottomSheet.radius,
                    ),
                ),
        ) {
            BottomSheetToolbar(
                modifier = Modifier.fillMaxWidth(),
                title = AppTokens.strings.res(Res.string.select_date),
                allowBack = false,
                onBack = {},
                onClose = {},
            )

            DatePickerScreen(
                state = DatePickerState(
                    value = DateTimeFormatState.of(
                        value = value,
                        range = limitations,
                        format = DateFormat.DateOnly.DateMmmDdYyyy,
                    ),
                    limitations = limitations,
                ),
                loaders = persistentSetOf(),
                contract = DatePickerContract.Empty,
            )
        }
    }
}
