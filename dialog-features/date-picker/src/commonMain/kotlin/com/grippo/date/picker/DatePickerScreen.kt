package com.grippo.date.picker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.BaseComposeDialog
import com.grippo.core.ScreenBackground
import com.grippo.date.picker.internal.DateWheelPicker
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.date_picker_title
import com.grippo.design.resources.submit_btn
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.datetime.LocalDateTime

@Composable
internal fun DatePickerScreen(
    state: DatePickerState,
    loaders: ImmutableSet<DatePickerLoader>,
    contract: DatePickerContract
) = BaseComposeDialog(ScreenBackground.Color(AppTokens.colors.background.secondary)) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = AppTokens.dp.screen.horizontalPadding,
                vertical = AppTokens.dp.screen.verticalPadding
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

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
            initial = state.initial,
            select = contract::select,
            limitations = state.limitations
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        Button(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.submit_btn),
            style = ButtonStyle.Primary,
            onClick = contract::submit
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        DatePickerScreen(
            state = DatePickerState(
                initial = LocalDateTime(2025, 7, 9, 14, 30),
            ),
            loaders = persistentSetOf(),
            contract = DatePickerContract.Empty
        )
    }
}