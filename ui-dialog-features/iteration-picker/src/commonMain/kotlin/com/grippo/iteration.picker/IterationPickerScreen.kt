package com.grippo.iteration.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.inputs.InputRepetitions
import com.grippo.design.components.inputs.InputVolume
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.set_label
import com.grippo.design.resources.provider.submit_btn
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import com.grippo.state.trainings.IterationFocus
import com.grippo.state.trainings.stubIteration
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun IterationPickerScreen(
    state: IterationPickerState,
    loaders: ImmutableSet<IterationPickerLoader>,
    contract: IterationPickerContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    val volumeRequester = remember { FocusRequester() }
    val repetitionsRequester = remember { FocusRequester() }

    LaunchedEffect(state.focus) {
        // run after composition
        withFrameNanos { /* no-op, just await a frame */ }

        when (state.focus) {
            IterationFocus.VOLUME -> {
                volumeRequester.requestFocus()
            }

            IterationFocus.REPETITIONS -> {
                repetitionsRequester.requestFocus()
            }

            IterationFocus.UNIDENTIFIED -> {}
        }
    }

    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

    Text(
        modifier = Modifier.fillMaxWidth(),
        text = AppTokens.strings.res(Res.string.set_label),
        style = AppTokens.typography.h3(),
        color = AppTokens.colors.text.primary,
        textAlign = TextAlign.Center
    )

    Spacer(Modifier.size(AppTokens.dp.contentPadding.block))

    Row(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {
        InputVolume(
            modifier = Modifier
                .focusRequester(volumeRequester)
                .weight(1f),
            value = state.value.volume.display,
            onValueChange = contract::onVolumeChange
        )

        InputRepetitions(
            modifier = Modifier
                .focusRequester(repetitionsRequester)
                .weight(1f),
            value = state.value.repetitions.display,
            onValueChange = contract::onRepetitionsChange
        )
    }

    Spacer(Modifier.size(AppTokens.dp.contentPadding.block))

    val buttonState = remember(loaders, state.value.repetitions, state.value.volume) {
        when {
            state.value.repetitions is RepetitionsFormatState.Invalid -> ButtonState.Disabled
            state.value.volume is VolumeFormatState.Invalid -> ButtonState.Disabled
            else -> ButtonState.Enabled
        }
    }

    Button(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth(),
        content = ButtonContent.Text(
            text = AppTokens.strings.res(Res.string.submit_btn),
        ),
        style = ButtonStyle.Primary,
        state = buttonState,
        onClick = contract::onSubmit
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        IterationPickerScreen(
            state = IterationPickerState(
                value = stubIteration(),
                focus = IterationFocus.UNIDENTIFIED
            ),
            loaders = persistentSetOf(),
            contract = IterationPickerContract.Empty
        )
    }
}