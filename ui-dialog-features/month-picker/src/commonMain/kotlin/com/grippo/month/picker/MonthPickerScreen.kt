package com.grippo.month.picker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateFormatState
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.continue_btn
import com.grippo.month.picker.internal.MonthWheelPicker
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun MonthPickerScreen(
    state: MonthPickerState,
    loaders: ImmutableSet<MonthPickerLoader>,
    contract: MonthPickerContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = state.title,
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        MonthWheelPicker(
            modifier = Modifier
                .height(AppTokens.dp.wheelPicker.height)
                .fillMaxWidth(),
            initial = state.value.value,
            select = contract::onSelectMonth,
            limitations = state.limitations
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        val buttonState = remember(loaders, state.value) {
            when (state.value) {
                is DateFormatState.Invalid -> ButtonState.Disabled
                is DateFormatState.Empty -> ButtonState.Disabled
                else -> ButtonState.Enabled
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
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
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        MonthPickerScreen(
            state = MonthPickerState(
                value = DateFormatState.of(DateTimeUtils.now(), DateTimeUtils.thisYear()),
                limitations = DateTimeUtils.trailingYear(),
                title = "Select month",
            ),
            loaders = persistentSetOf(),
            contract = MonthPickerContract.Empty
        )
    }
}
