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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.inputs.InputRepetitions
import com.grippo.design.components.inputs.InputVolume
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.submit_btn
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

    val volumeRequester = FocusRequester()
    val repetitionsRequester = FocusRequester()

    LaunchedEffect(state.focus) {
        when (state.focus) {
            IterationFocus.VOLUME -> volumeRequester.requestFocus()
            IterationFocus.REPETITIONS -> repetitionsRequester.requestFocus()
            IterationFocus.UNIDENTIFIED -> {}
        }
    }

    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

    Text(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth(),
        text = "Iteration",
        style = AppTokens.typography.h1(),
        color = AppTokens.colors.text.primary,
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
            value = state.value.volume.value,
            onValueChange = contract::onVolumeChange
        )

        InputRepetitions(
            modifier = Modifier
                .focusRequester(repetitionsRequester)
                .weight(1f),
            value = state.value.repetitions.value,
            onValueChange = contract::onRepetitionsChange
        )
    }

    Spacer(Modifier.size(AppTokens.dp.contentPadding.block))

    Button(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth(),
        text = AppTokens.strings.res(Res.string.submit_btn),
        style = ButtonStyle.Primary,
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