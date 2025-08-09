package com.grippo.date.picker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.date.picker.internal.DateWheelPicker
import com.grippo.date.utils.DateTimeUtils
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.date_picker_title
import com.grippo.design.resources.submit_btn
import com.grippo.state.formatters.DateFormatState
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun DatePickerScreen(
    state: DatePickerState,
    loaders: ImmutableSet<DatePickerLoader>,
    contract: DatePickerContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.date_picker_title),
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        DateWheelPicker(
            modifier = Modifier
                .height(AppTokens.dp.wheelPicker.height)
                .fillMaxWidth(),
            initial = state.value.value,
            select = contract::onSelectDate,
            limitations = state.limitations
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        val buttonState = remember(loaders, state.value) {
            when {
                state.value is DateFormatState.Valid -> ButtonState.Enabled
                else -> ButtonState.Disabled
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.submit_btn),
            style = ButtonStyle.Primary,
            state = buttonState,
            onClick = contract::onSubmitClick
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        DatePickerScreen(
            state = DatePickerState(
                value = DateFormatState.of(DateTimeUtils.now(), DateTimeUtils.thisWeek()),
                limitations = DateTimeUtils.trailingYear()
            ),
            loaders = persistentSetOf(),
            contract = DatePickerContract.Empty
        )
    }
}