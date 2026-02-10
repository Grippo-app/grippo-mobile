package com.grippo.duration.picker

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.duration_picker_title
import com.grippo.design.resources.provider.submit_btn
import com.grippo.duration.picker.internal.DurationWheelPicker
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Composable
internal fun DurationPickerScreen(
    state: DurationPickerState,
    loaders: ImmutableSet<DurationPickerLoader>,
    contract: DurationPickerContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

    Text(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth(),
        text = AppTokens.strings.res(Res.string.duration_picker_title),
        style = AppTokens.typography.h2(),
        color = AppTokens.colors.text.primary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    DurationWheelPicker(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
        hours = state.hours,
        minutes = state.minutes,
        value = state.value,
        select = contract::onSelectDuration
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    Button(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth(),
        content = ButtonContent.Text(
            text = AppTokens.strings.res(Res.string.submit_btn),
        ),
        style = ButtonStyle.Primary,
        onClick = contract::onSubmitClick
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))

    Spacer(modifier = Modifier.navigationBarsPadding())
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        DurationPickerScreen(
            state = DurationPickerState(
                hours = resolveDurationHours(1.hours + 30.minutes),
                value = 1.hours + 30.minutes
            ),
            contract = DurationPickerContract.Empty,
            loaders = persistentSetOf()
        )
    }
}
