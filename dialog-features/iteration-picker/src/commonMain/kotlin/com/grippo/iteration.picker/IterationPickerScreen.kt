package com.grippo.iteration.picker

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.inputs.InputRepetitions
import com.grippo.design.components.inputs.InputVolume
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun IterationPickerScreen(
    state: IterationPickerState,
    loaders: ImmutableSet<IterationPickerLoader>,
    contract: IterationPickerContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

    Text(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
            .fillMaxWidth(),
        text = "Iteration",
        style = AppTokens.typography.h1(),
        color = AppTokens.colors.text.primary,
    )

    Spacer(Modifier.size(AppTokens.dp.contentPadding.block))

    InputRepetitions(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
            .fillMaxWidth(),
        value = state.repetitions,
        onValueChange = contract::onRepetitionsChange
    )

    Spacer(Modifier.size(AppTokens.dp.contentPadding.content))

    InputVolume(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
            .fillMaxWidth(),
        value = state.volume,
        onValueChange = contract::onVolumeChange
    )

    Spacer(Modifier.size(AppTokens.dp.contentPadding.block))

    Button(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
            .fillMaxWidth(),
        text = "Submit",
        style = ButtonStyle.Primary,
        onClick = contract::onSubmit
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        IterationPickerScreen(
            state = IterationPickerState(
                volume = 140f,
                repetitions = 6
            ),
            loaders = persistentSetOf(),
            contract = IterationPickerContract.Empty
        )
    }
}