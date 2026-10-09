package com.grippo.training.recording

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
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
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.stubExercise
import com.grippo.core.state.trainings.stubPendingExercise
import com.grippo.core.state.trainings.stubPendingIteration
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
import com.grippo.design.resources.provider.save_btn
import com.grippo.design.resources.provider.training
import com.grippo.design.resources.provider.update_btn
import com.grippo.toolkit.date.utils.timerTextFlow
import com.grippo.training.recording.internal.ExercisesPage
import com.grippo.training.recording.internal.Header
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList

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
        title = AppTokens.strings.res(Res.string.training),
        leading = Leading.Back(contract::onBack),
        trailing = {
            if (state.exercises.isEmpty()) return@Toolbar

            val buttonText = when (state.stage) {
                is StageState.Add -> AppTokens.strings.res(Res.string.save_btn)
                StageState.Draft -> AppTokens.strings.res(Res.string.save_btn)
                is StageState.Edit -> AppTokens.strings.res(Res.string.update_btn)
            }

            Button(
                content = ButtonContent.Text(text = buttonText),
                size = ButtonSize.Small,
                style = ButtonStyle.Primary,
                state = ButtonState.Enabled,
                onClick = contract::onSave
            )
        },
        content = {
            Header(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = AppTokens.dp.screen.horizontalPadding,
                        vertical = AppTokens.dp.contentPadding.content
                    ),
                duration = durationText,
                volume = totalVolume,
                repetitions = totalRepetitions,
            )

            HorizontalDivider(color = AppTokens.colors.divider.default)
        }
    )

    ExercisesPage(
        modifier = Modifier.fillMaxSize(),
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
                exercises = persistentListOf(
                    previewExercise("Bench press", 3, listOf(60f, 70f, 80f)),
                    previewExercise("Barbell squat", 4, listOf(80f, 100f)),
                    previewExercise("Pull-ups", 3, emptyList()),
                    previewExercise(
                        "Жим гантелей лёжа на наклонной скамье нейтральным хватом",
                        4,
                        listOf(22.5f),
                    ),
                    previewExercise("Cable row", 3, emptyList()),
                ),
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

private fun previewExercise(
    name: String,
    sets: Int,
    weights: List<Float>,
): ExerciseState {
    val base = stubExercise()
    val iterations = List(sets) { index ->
        stubPendingIteration().copy(
            externalWeight = weights.getOrNull(index)?.let { VolumeFormatState.of(it) }
                ?: VolumeFormatState.Empty(),
            repetitions = RepetitionsFormatState.of(10),
        )
    }.toPersistentList()
    return base.copy(
        exerciseExample = base.exerciseExample.copy(name = name),
        iterations = iterations,
        total = base.total.copy(
            volume = VolumeFormatState.of(weights.sum() * 10),
            repetitions = RepetitionsFormatState.of(weights.size * 10),
        ),
    )
}
