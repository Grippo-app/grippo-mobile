package com.grippo.training.recording

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.MultiplierFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.examples.WeightTypeEnumState
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.stubExercise
import com.grippo.core.state.trainings.stubPendingIteration
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.frames.OverlayContainer
import com.grippo.design.components.toolbar.Leading
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.add_exercise_btn
import com.grippo.design.resources.provider.save_btn
import com.grippo.design.resources.provider.training
import com.grippo.design.resources.provider.update_btn
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import com.grippo.toolkit.date.utils.timerTextFlow
import com.grippo.training.recording.internal.ExercisesPage
import com.grippo.training.recording.internal.Header
import com.grippo.training.recording.components.TrainingRecordingHeaderSkeleton
import com.grippo.training.recording.components.TrainingRecordingSkeleton
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDateTime

@Composable
internal fun TrainingRecordingScreen(
    state: TrainingRecordingState,
    loaders: ImmutableSet<TrainingRecordingLoader>,
    contract: TrainingRecordingContract,
) {
    val timerFlow = remember(state.startAt) {
        timerTextFlow(start = state.startAt)
    }
    val durationText by timerFlow.collectAsState(initial = "")

    TrainingRecordingContent(
        state = state,
        loaders = loaders,
        duration = durationText,
        contract = contract,
    )
}

@Composable
private fun TrainingRecordingContent(
    state: TrainingRecordingState,
    loaders: ImmutableSet<TrainingRecordingLoader> = persistentSetOf(),
    duration: String,
    contract: TrainingRecordingContract,
) = BaseComposeScreen(
    background = ScreenBackground.Color(value = AppTokens.colors.background.screen),
) {
    val isLoading = state.stage != StageState.Add && state.exercises == null &&
        TrainingRecordingLoader.Training in loaders

    val totalVolume = remember(state.exercises) {
        if (state.exercises.orEmpty().any { it.total.volume is VolumeFormatState.Invalid }) {
            VolumeFormatState.Invalid(display = "", value = null)
        } else {
            val sum = state.exercises.orEmpty()
                .sumOf { (it.total.volume.value ?: 0f).toDouble() }
                .toFloat()
            VolumeFormatState.of(sum)
        }
    }

    val totalRepetitions = remember(state.exercises) {
        val sum = state.exercises.orEmpty().sumOf { it.total.repetitions.value ?: 0 }
        RepetitionsFormatState.of(sum)
    }

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        style = ToolbarStyle.Transparent,
        title = AppTokens.strings.res(Res.string.training),
        leading = Leading.Back(contract::onBack),
        trailing = {
            if (state.exercises.orEmpty().isEmpty()) {
                return@Toolbar
            }

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
                onClick = contract::onSave,
            )
        },
        content = {
            if (isLoading) {
                TrainingRecordingHeaderSkeleton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = AppTokens.dp.screen.horizontalPadding,
                            vertical = AppTokens.dp.contentPadding.content,
                        ),
                )
                return@Toolbar
            }

            Header(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = AppTokens.dp.screen.horizontalPadding,
                        vertical = AppTokens.dp.contentPadding.content,
                    ),
                duration = duration,
                volume = totalVolume,
                repetitions = totalRepetitions,
            )
        },
    )

    OverlayContainer(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = AppTokens.dp.contentPadding.content),
        overlay = AppTokens.colors.background.screen,
        content = { resolvedPadding ->
            if (isLoading) {
                TrainingRecordingSkeleton(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
                    contentPadding = resolvedPadding,
                )
            } else {
                ExercisesPage(
                    modifier = Modifier.fillMaxSize(),
                    exercises = state.exercises ?: persistentListOf(),
                    examples = state.examples,
                    contentPadding = resolvedPadding,
                    contract = contract,
                )
            }
        },
        bottom = {
            Button(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth(),
                content = ButtonContent.Text(text = AppTokens.strings.res(Res.string.add_exercise_btn)),
                style = ButtonStyle.Secondary,
                onClick = contract::onAddExercise,
            )
            Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))
            Spacer(modifier = Modifier.navigationBarsPadding())
        },
    )
}

@AppPreview
@Composable
private fun ScreenEditLoadingPreview() {
    PreviewContainer {
        TrainingRecordingContent(
            state = TrainingRecordingState(stage = StageState.Edit(id = "preview-training"), startAt = previewStartAt),
            loaders = persistentSetOf(TrainingRecordingLoader.Training),
            duration = "00:00:00",
            contract = TrainingRecordingContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ScreenDraftLoadingPreview() {
    PreviewContainer {
        TrainingRecordingContent(
            state = TrainingRecordingState(stage = StageState.Draft, startAt = previewStartAt),
            loaders = persistentSetOf(TrainingRecordingLoader.Training),
            duration = "00:00:00",
            contract = TrainingRecordingContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ScreenAssistanceMissingPreview() {
    val base = previewExercise(name = "Assisted Pull-Up", sets = 1, weights = emptyList())
    val iteration = stubPendingIteration().copy(
        bodyWeight = WeightFormatState.of(80f),
        bodyMultiplier = MultiplierFormatState.of(1f),
        repetitions = RepetitionsFormatState.of(8),
    )
    val exercise = base.copy(
        exerciseExample = base.exerciseExample.copy(weightType = WeightTypeEnumState.BODY_WEIGHT),
        iterations = persistentListOf(iteration),
        total = base.total.copy(
            repetitions = RepetitionsFormatState.ofTotal(8),
            volume = VolumeFormatState.Invalid(display = "", value = null),
            intensity = IntensityFormatState.Invalid(display = "", value = null),
        ),
    )

    PreviewContainer {
        TrainingRecordingContent(
            state = TrainingRecordingState(
                stage = StageState.Add,
                startAt = previewStartAt,
                exercises = persistentListOf(exercise),
            ),
            duration = "00:02:08",
            contract = TrainingRecordingContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ScreenPopulatedPreview() {
    PreviewContainer {
        TrainingRecordingContent(
            state = TrainingRecordingState(
                stage = StageState.Add,
                startAt = previewStartAt,
                exercises = persistentListOf(
                    previewExercise(
                        name = "Bench press",
                        sets = 3,
                        weights = listOf(60f, 70f, 80f),
                    ),
                    previewExercise(
                        name = "Barbell squat",
                        sets = 4,
                        weights = listOf(80f, 100f),
                    ),
                    previewExercise(
                        name = "Pull-ups",
                        sets = 3,
                        weights = emptyList(),
                    ),
                    previewExercise(
                        name = "Cable row",
                        sets = 0,
                        weights = emptyList(),
                    ),
                ),
            ),
            duration = "00:45:12",
            contract = TrainingRecordingContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ScreenEmptyPreview() {
    PreviewContainer {
        TrainingRecordingContent(
            state = TrainingRecordingState(
                stage = StageState.Add,
                startAt = previewStartAt,
                exercises = persistentListOf(),
            ),
            duration = "00:00:00",
            contract = TrainingRecordingContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ScreenInitialPreview() {
    PreviewContainer {
        TrainingRecordingContent(
            state = TrainingRecordingState(
                stage = StageState.Add,
                startAt = previewStartAt,
            ),
            duration = "00:00:00",
            contract = TrainingRecordingContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ScreenPendingPreview() {
    PreviewContainer {
        TrainingRecordingContent(
            state = TrainingRecordingState(
                stage = StageState.Add,
                startAt = previewStartAt,
                exercises = persistentListOf(
                    previewExercise(name = "Bench press", sets = 3, weights = emptyList()),
                ),
            ),
            duration = "00:02:08",
            contract = TrainingRecordingContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ScreenEditLongTitlePreview() {
    PreviewContainer {
        TrainingRecordingContent(
            state = TrainingRecordingState(
                stage = StageState.Edit(id = "preview-training"),
                startAt = previewStartAt,
                exercises = persistentListOf(
                    previewExercise(
                        name = "Жим гантелей лёжа на наклонной скамье нейтральным хватом",
                        sets = 4,
                        weights = listOf(22.5f),
                    ),
                ),
            ),
            duration = "01:35:42",
            contract = TrainingRecordingContract.Empty,
        )
    }
}

private val previewStartAt = LocalDateTime.parse("2026-10-16T10:00")

private fun previewExercise(
    name: String,
    sets: Int,
    weights: List<Float>,
): ExerciseState {
    val base = stubExercise()
    val createdAt = DateTimeFormatState.of(
        value = previewStartAt,
        range = DateRangePresets.infinity(),
        format = DateFormat.DateOnly.DateDdMmmYyyy,
    )
    val iterations = List(sets) { index ->
        val weight = weights.getOrNull(index)
        stubPendingIteration().copy(
            id = "$name-set-$index",
            externalWeight = VolumeFormatState.of(weight),
            repetitions = RepetitionsFormatState.of(10),
        )
    }.toPersistentList()

    return base.copy(
        id = name,
        name = name,
        createdAt = createdAt,
        exerciseExample = base.exerciseExample.copy(id = name, name = name, lastUsed = createdAt),
        iterations = iterations,
        total = base.total.copy(
            volume = VolumeFormatState.of(weights.sum() * 10),
            repetitions = RepetitionsFormatState.of(weights.size * 10),
            intensity = IntensityFormatState.Empty(),
        ),
    )
}
