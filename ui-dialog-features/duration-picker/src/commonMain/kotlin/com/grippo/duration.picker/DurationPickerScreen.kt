package com.grippo.duration.picker

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.apply_btn
import com.grippo.duration.picker.internal.DurationWheelPicker
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun DurationPickerScreen(
    state: DurationPickerState,
    loaders: ImmutableSet<DurationPickerLoader>,
    contract: DurationPickerContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {
    val buttonState = when (state.value) {
        is DurationFormatState.Valid -> ButtonState.Enabled
        else -> ButtonState.Disabled
    }

    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

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
            text = AppTokens.strings.res(Res.string.apply_btn),
        ),
        state = buttonState,
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
                value = DurationFormatState.of(1.hours + 30.minutes)
            ),
            contract = DurationPickerContract.Empty,
            loaders = persistentSetOf()
        )
    }
}

@AppPreview
@Composable
private fun EmptyDurationPreview() {
    PreviewContainer {
        DurationPickerScreen(
            state = DurationPickerState(value = DurationFormatState.Empty()),
            loaders = persistentSetOf(),
            contract = DurationPickerContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun InvalidDurationPreview() {
    PreviewContainer {
        DurationPickerScreen(
            state = DurationPickerState(value = DurationFormatState.of(24.hours)),
            loaders = persistentSetOf(),
            contract = DurationPickerContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun MaximumDurationPreview() {
    PreviewContainer {
        DurationPickerScreen(
            state = DurationPickerState(
                value = DurationFormatState.of(DurationFormatState.DurationLimitation.endInclusive),
            ),
            loaders = persistentSetOf(),
            contract = DurationPickerContract.Empty,
        )
    }
}
