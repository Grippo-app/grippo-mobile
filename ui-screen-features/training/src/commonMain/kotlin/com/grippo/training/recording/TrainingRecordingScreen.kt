package com.grippo.training.recording

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.trainings.stubPendingExercise
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.toolbar.Leading
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.edit_training_title
import com.grippo.design.resources.provider.save_btn
import com.grippo.design.resources.provider.training
import com.grippo.toolkit.date.utils.timerTextFlow
import com.grippo.training.recording.internal.ExercisesPage
import com.grippo.training.recording.internal.Header
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun TrainingRecordingScreen(
    state: TrainingRecordingState,
    loaders: ImmutableSet<TrainingRecordingLoader>,
    contract: TrainingRecordingContract
) = BaseComposeScreen(
    ScreenBackground.Color(
        value = AppTokens.colors.background.screen
    )
) {
    val timerFlow = remember(state.startAt) { timerTextFlow(start = state.startAt) }

    val durationText by timerFlow.collectAsState(initial = "")

    val totalVolume = remember(state.exercises) {
        val sum = state.exercises
            .sumOf { (it.total.volume.value ?: 0f).toDouble() }
            .toFloat()
        VolumeFormatState.of(sum)
    }

    val totalRepetitions = remember(state.exercises) {
        val sum = state.exercises.sumOf { it.total.repetitions.value ?: 0 }
        RepetitionsFormatState.of(sum)
    }

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        style = ToolbarStyle.Transparent,
        title = when (state.stage) {
            is StageState.Add -> AppTokens.strings.res(Res.string.training)
            StageState.Draft -> AppTokens.strings.res(Res.string.training)
            is StageState.Edit -> AppTokens.strings.res(Res.string.edit_training_title)
        },
        leading = Leading.Back(contract::onBack),
        trailing = {
            val buttonVisible = remember(state.exercises) {
                state.exercises.isNotEmpty()
            }

            if (buttonVisible.not()) return@Toolbar

            val buttonState = remember(loaders, state.exercises) {
                when {
                    state.exercises.isEmpty() -> ButtonState.Disabled
                    else -> ButtonState.Enabled
                }
            }

            Button(
                content = ButtonContent.Text(text = AppTokens.strings.res(Res.string.save_btn)),
                size = ButtonSize.Small,
                style = ButtonStyle.Primary,
                state = buttonState,
                onClick = contract::onSave
            )
        },
        content = {
            Header(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
                duration = durationText,
                volume = totalVolume,
                repetitions = totalRepetitions,
            )

            Spacer(Modifier.height(AppTokens.dp.contentPadding.subContent))
        }
    )

    ExercisesPage(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        state = state,
        contract = contract
    )
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        TrainingRecordingScreen(
            state = TrainingRecordingState(
                stage = StageState.Add,
                exercises = stubTraining().exercises,
            ),
            loaders = persistentSetOf(),
            contract = TrainingRecordingContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun ScreenEmptyPreview() {
    PreviewContainer {
        TrainingRecordingScreen(
            state = TrainingRecordingState(
                stage = StageState.Add,
                exercises = persistentListOf(),
            ),
            loaders = persistentSetOf(),
            contract = TrainingRecordingContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun ScreenPresetPreview() {
    PreviewContainer {
        TrainingRecordingScreen(
            state = TrainingRecordingState(
                stage = StageState.Add,
                exercises = persistentListOf(stubPendingExercise()),
            ),
            loaders = persistentSetOf(),
            contract = TrainingRecordingContract.Empty
        )
    }
}
