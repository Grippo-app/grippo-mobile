package com.grippo.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.examples.WeightTypeEnumState
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.MultiplierFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.metrics.volume.TrainingTotalState
import com.grippo.core.state.trainings.IterationState
import com.grippo.core.state.trainings.stubExercise
import com.grippo.core.state.trainings.stubPendingIteration
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.empty.EmptyState
import com.grippo.design.components.empty.EmptyStateStyle
import com.grippo.design.components.example.ExerciseExampleImage
import com.grippo.design.components.example.ExerciseExampleImageStyle
import com.grippo.design.components.frames.OverlayContainer
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.exercise_details_btn
import com.grippo.design.resources.provider.exercise_history_no_sets
import com.grippo.design.resources.provider.exercise_history_unavailable
import com.grippo.exercise.components.ExerciseProgressCard
import com.grippo.exercise.components.ExerciseSetsContent
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDateTime
import com.grippo.core.state.trainings.ExerciseState as TrainingExerciseState

@Composable
internal fun ExerciseScreen(
    state: ExerciseState,
    loaders: ImmutableSet<ExerciseLoader>,
    contract: ExerciseContract,
) = BaseComposeScreen(background = ScreenBackground.Color(AppTokens.colors.background.dialog)) {
    OverlayContainer(
        modifier = Modifier
            .fillMaxWidth()
            .weight(weight = 1f, fill = false),
        contentPadding = PaddingValues(
            start = AppTokens.dp.dialog.horizontalPadding,
            end = AppTokens.dp.dialog.horizontalPadding,
            top = AppTokens.dp.dialog.top,
        ),
        overlay = AppTokens.colors.background.dialog,
        content = { resolvedPadding ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(resolvedPadding),
            ) {
                val exercise = state.exercise
                when {
                    exercise == null -> EmptyState(
                        text = AppTokens.strings.res(Res.string.exercise_history_unavailable),
                        style = EmptyStateStyle.Text,
                    )

                    else -> {
                        val isHistoryLoading = loaders.contains(ExerciseLoader.History) && state.history.isEmpty()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ExerciseExampleImage(
                                value = exercise.exerciseExample.imageUrl,
                                style = ExerciseExampleImageStyle.MEDIUM,
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
                            ) {
                                Text(
                                    text = exercise.exerciseExample.name,
                                    style = AppTokens.typography.h4(),
                                    color = AppTokens.colors.text.primary,
                                )

                                if (exercise.createdAt.display.isNotEmpty()) {
                                    Text(
                                        text = exercise.createdAt.display,
                                        style = AppTokens.typography.b12Med(),
                                        color = AppTokens.colors.text.secondary,
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

                        if (exercise.iterations.isEmpty()) {
                            EmptyState(
                                text = AppTokens.strings.res(Res.string.exercise_history_no_sets),
                                style = EmptyStateStyle.Text,
                            )
                        } else {
                            ExerciseSetsContent(
                                value = exercise.iterations,
                                previous = state.history.lastOrNull(),
                                isHistoryLoading = isHistoryLoading,
                            )

                            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

                            ExerciseProgressCard(
                                modifier = Modifier.fillMaxWidth(),
                                value = state,
                                isHistoryLoading = isHistoryLoading,
                            )
                        }

                        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = contract::onExampleDetailsClick,
                            style = ButtonStyle.Secondary,
                            content = ButtonContent.Text(
                                text = AppTokens.strings.res(Res.string.exercise_details_btn),
                            ),
                        )
                    }
                }
            }
        },
        bottom = {
            Spacer(modifier = Modifier.navigationBarsPadding())
        },
    )
}

@AppPreview
@Composable
private fun ExerciseRecordedSetsPreview() {
    PreviewContainer {
        ExerciseScreen(
            state = exerciseWithPreviousPreview(),
            loaders = persistentSetOf(),
            contract = ExerciseContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ExerciseBodyAndPartialSetsPreview() {
    PreviewContainer {
        ExerciseScreen(
            state = ExerciseState(exercise = exerciseBodyHistoryPreview()),
            loaders = persistentSetOf(),
            contract = ExerciseContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ExerciseWithManySetsPreview() {
    PreviewContainer {
        ExerciseScreen(
            state = ExerciseState(exercise = exerciseLongHistoryPreview()),
            loaders = persistentSetOf(),
            contract = ExerciseContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ExerciseWithoutSetsPreview() {
    PreviewContainer {
        ExerciseScreen(
            state = ExerciseState(exercise = exerciseHistoryPreview().copy(iterations = persistentListOf())),
            loaders = persistentSetOf(),
            contract = ExerciseContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ExerciseUnavailablePreview() {
    PreviewContainer {
        ExerciseScreen(
            state = ExerciseState(),
            loaders = persistentSetOf(),
            contract = ExerciseContract.Empty,
        )
    }
}

private fun exerciseHistoryPreview(): TrainingExerciseState {
    val exercise = stubExercise()
    val date = DateTimeFormatState.of(
        value = LocalDateTime(year = 2026, monthNumber = 10, dayOfMonth = 14, hour = 18, minute = 30),
        range = DateRangePresets.infinity(),
        format = DateFormat.DateOnly.DateDdMmmYyyy,
    )

    return exercise.copy(
        id = "recorded-exercise",
        name = "Hammer Strength Bench Press",
        exerciseExample = exercise.exerciseExample.copy(
            id = "bench-press",
            name = "Hammer Strength Bench Press",
            weightType = WeightTypeEnumState.FIXED,
            lastUsed = date,
        ),
        createdAt = date,
        iterations = persistentListOf(
            previewIteration(id = "set-1", external = 80f, repetitions = 8),
            previewIteration(id = "set-2", external = 90f, repetitions = 8),
            previewIteration(id = "set-3", external = 100f, repetitions = 8),
            previewIteration(id = "set-4", external = 110f, repetitions = 6),
        ),
        total = TrainingTotalState(
            volume = VolumeFormatState.of(2_820f),
            repetitions = RepetitionsFormatState.of(30),
            intensity = IntensityFormatState.of(94f),
        ),
    )
}

private fun exerciseBodyHistoryPreview(): TrainingExerciseState {
    val exercise = exerciseHistoryPreview()
    return exercise.copy(
        id = "body-exercise",
        name = "Pull-ups",
        exerciseExample = exercise.exerciseExample.copy(
            id = "pull-ups",
            name = "Pull-ups",
            weightType = WeightTypeEnumState.BODY_WEIGHT,
        ),
        iterations = persistentListOf(
            previewIteration(id = "body", body = 80f, multiplier = 1f, repetitions = 8),
            previewIteration(id = "assist", body = 80f, multiplier = 1f, assist = 30f, repetitions = 10),
            previewIteration(id = "extra", body = 80f, multiplier = 1f, extra = 20f, repetitions = 6),
            previewIteration(id = "zero", body = 80f, multiplier = 1f, assist = 80f, repetitions = 8),
            previewIteration(id = "missing-multiplier", body = 80f, repetitions = 6),
            previewIteration(id = "missing-repetitions", body = 80f, multiplier = 1f),
            previewIteration(id = "missing-load", repetitions = 10),
        ),
        total = TrainingTotalState(
            volume = VolumeFormatState.of(1_740f),
            repetitions = RepetitionsFormatState.of(48),
            intensity = IntensityFormatState.Empty(),
        ),
    )
}

private fun exerciseLongHistoryPreview(): TrainingExerciseState {
    val exercise = exerciseHistoryPreview()
    return exercise.copy(
        id = "many-sets",
        exerciseExample = exercise.exerciseExample.copy(
            id = "long-exercise",
            name = "Жим гантелей на похилій лаві нейтральним хватом із паузою в нижній точці",
        ),
        iterations = List(24) { index ->
            previewIteration(id = "large-set-$index", external = 1_000f, repetitions = 100)
        }.toPersistentList(),
        total = TrainingTotalState(
            volume = VolumeFormatState.of(2_400_000f),
            repetitions = RepetitionsFormatState.of(2_400),
            intensity = IntensityFormatState.of(1_000f),
        ),
    )
}

private fun previewIteration(
    id: String,
    external: Float? = null,
    body: Float? = null,
    multiplier: Float? = null,
    extra: Float? = null,
    assist: Float? = null,
    repetitions: Int? = null,
): IterationState {
    return stubPendingIteration().copy(
        id = id,
        externalWeight = VolumeFormatState.of(external),
        bodyWeight = WeightFormatState.of(body),
        bodyMultiplier = MultiplierFormatState.of(multiplier),
        extraWeight = VolumeFormatState.of(extra),
        assistWeight = VolumeFormatState.of(assist),
        repetitions = repetitions?.let { RepetitionsFormatState.of(it) } ?: RepetitionsFormatState.Empty(),
    )
}

private fun exerciseWithPreviousPreview(): ExerciseState {
    val current = exerciseHistoryPreview()
    val previous = current.copy(
        id = "previous-exercise",
        createdAt = DateTimeFormatState.of(
            value = LocalDateTime(year = 2026, monthNumber = 10, dayOfMonth = 7, hour = 18, minute = 30),
            range = DateRangePresets.infinity(),
            format = DateFormat.DateOnly.DateDdMmmYyyy,
        ),
        iterations = persistentListOf(
            previewIteration(id = "previous-set-1", external = 75f, repetitions = 8),
            previewIteration(id = "previous-set-2", external = 85f, repetitions = 8),
            previewIteration(id = "previous-set-3", external = 95f, repetitions = 8),
            previewIteration(id = "previous-set-4", external = 100f, repetitions = 6),
        ),
        total = current.total.copy(volume = VolumeFormatState.of(2_640f)),
    )
    return ExerciseState(
        exercise = current,
        history = persistentListOf(previous),
    )
}

@AppPreview
@Composable
private fun ExerciseWithoutPreviousRecordPreview() {
    PreviewContainer {
        ExerciseScreen(
            state = exerciseWithPreviousPreview().copy(history = persistentListOf()),
            loaders = persistentSetOf(),
            contract = ExerciseContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ExerciseHistoryLoadingPreview() {
    PreviewContainer {
        ExerciseScreen(
            state = ExerciseState(exercise = exerciseHistoryPreview()),
            loaders = persistentSetOf(ExerciseLoader.History),
            contract = ExerciseContract.Empty,
        )
    }
}
