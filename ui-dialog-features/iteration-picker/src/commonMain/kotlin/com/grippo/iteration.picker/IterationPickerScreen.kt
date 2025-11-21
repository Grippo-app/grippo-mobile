package com.grippo.iteration.picker

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.trainings.IterationFocus
import com.grippo.core.state.trainings.stubExercises
import com.grippo.core.state.trainings.stubIteration
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.chip.Chip
import com.grippo.design.components.chip.ChipLabel
import com.grippo.design.components.chip.ChipSize
import com.grippo.design.components.chip.ChipStype
import com.grippo.design.components.chip.ChipTrailing
import com.grippo.design.components.inputs.InputRepetitions
import com.grippo.design.components.inputs.InputVolume
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.set_value
import com.grippo.design.resources.provider.submit_btn
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

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

        delay(250.milliseconds)

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
        text = AppTokens.strings.res(Res.string.set_value, state.number),
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

    if (state.suggestions.isNotEmpty()) {

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
        ) {
            state.suggestions.forEach { item ->
                key(item.id) {
                    val clickProvider = remember(item.id) {
                        { contract.onIterationClick(item.id) }
                    }

                    Chip(
                        label = ChipLabel.Empty,
                        stype = ChipStype.Clickable(clickProvider),
                        value = "${item.volume.short()} ${item.repetitions.short()}",
                        trailing = ChipTrailing.Empty,
                        contentColor = AppTokens.colors.text.secondary,
                        size = ChipSize.Medium,
                        brush = SolidColor(AppTokens.colors.background.card)
                    )
                }
            }
        }
    }

    Spacer(Modifier.size(AppTokens.dp.contentPadding.block))

    val buttonState = remember(loaders, state.value.repetitions, state.value.volume) {
        when {
            state.value.repetitions is RepetitionsFormatState.Invalid -> ButtonState.Disabled
            state.value.repetitions is RepetitionsFormatState.Empty -> ButtonState.Disabled
            state.value.volume is VolumeFormatState.Invalid -> ButtonState.Disabled
            state.value.volume is VolumeFormatState.Empty -> ButtonState.Disabled
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

    Spacer(modifier = Modifier.navigationBarsPadding())
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        IterationPickerScreen(
            state = IterationPickerState(
                value = stubIteration(),
                number = 2,
                suggestions = stubExercises().random().iterations,
                focus = IterationFocus.UNIDENTIFIED
            ),
            loaders = persistentSetOf(),
            contract = IterationPickerContract.Empty
        )
    }
}