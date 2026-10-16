package com.grippo.training.completed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.core.state.trainings.TimelineState
import com.grippo.core.state.trainings.TimelineState.Companion.exercise
import com.grippo.core.state.trainings.TrainingPosition
import com.grippo.core.state.trainings.stubPendingIteration
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.components.brand.BrandHeader
import com.grippo.design.components.brand.BrandHeaderStyle
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.loading.Loader
import com.grippo.design.components.metrics.TrainingSummaryCard
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.components.training.ExerciseCard
import com.grippo.design.components.training.ExerciseCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.exercises
import com.grippo.design.resources.provider.training_completed_details_unavailable
import com.grippo.design.resources.provider.training_completed_done
import com.grippo.design.resources.provider.training_completed_result_unavailable
import com.grippo.design.resources.provider.training_completed_updated_description
import com.grippo.design.resources.provider.training_completed_updated_title
import com.grippo.design.resources.provider.workout_summary
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Composable
internal fun TrainingCompletedScreen(
    state: TrainingCompletedState,
    loaders: ImmutableSet<TrainingCompletedLoader>,
    contract: TrainingCompletedContract,
) = BaseComposeScreen(
    background = ScreenBackground.Color(
        value = AppTokens.colors.background.screen,
    ),
) {
    if (loaders.contains(TrainingCompletedLoader.SaveTraining)) {
        Loader(modifier = Modifier.fillMaxSize())
        return@BaseComposeScreen
    }

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        style = ToolbarStyle.Transparent,
        title = AppTokens.strings.res(Res.string.workout_summary),
    )

    val basePadding = PaddingValues(
        start = AppTokens.dp.screen.horizontalPadding,
        end = AppTokens.dp.screen.horizontalPadding,
        top = AppTokens.dp.contentPadding.content,
        bottom = AppTokens.dp.contentPadding.subContent,
    )

    val training = state.training
    val hasSavedResult = training != null || state.savedTrainingId != null

    BottomOverlayContainer(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = basePadding,
        content = { containerModifier, resolvedPadding ->
            LazyColumn(
                modifier = containerModifier.fillMaxSize(),
                contentPadding = resolvedPadding,
            ) {
                if (hasSavedResult) {
                    item(key = "completion_header") {
                        BrandHeader(
                            modifier = Modifier.fillMaxWidth(),
                            title = state.title.text(),
                            description = state.description.text(),
                            style = BrandHeaderStyle.Compact,
                        )

                        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))
                    }
                }

                if (training != null) {
                    item(key = "training_summary") {
                        TrainingSummaryCard(
                            modifier = Modifier.fillMaxWidth(),
                            training = training,
                            muscleLoad = state.muscleLoad,
                            onClick = contract::onSummaryClick,
                        )

                        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))
                    }

                    if (training.exercises.isNotEmpty()) {
                        item(key = "exercises_section") {
                            ExercisesSectionHeader(
                                modifier = Modifier.fillMaxWidth(),
                                count = training.exercises.size,
                            )

                            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))
                        }
                    }

                    items(
                        items = state.timeline,
                        key = { it.key },
                        contentType = { it::class },
                    ) { value ->
                        val exercise = value.exercise()

                        if (exercise != null) {
                            ExerciseCard(
                                modifier = Modifier.fillMaxWidth(),
                                value = exercise,
                                style = ExerciseCardStyle.Medium(
                                    onClick = { contract.onExerciseClick(id = exercise.id) },
                                ),
                            )
                        }
                    }
                } else {
                    item(key = "completion_unavailable") {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = AppTokens.strings.res(
                                if (hasSavedResult) {
                                    Res.string.training_completed_details_unavailable
                                } else {
                                    Res.string.training_completed_result_unavailable
                                },
                            ),
                            style = AppTokens.typography.b14Med(),
                            color = AppTokens.colors.text.secondary,
                        )
                    }
                }
            }
        },
        overlay = AppTokens.colors.background.screen,
        bottom = {
            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            Button(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth(),
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.training_completed_done),
                ),
                style = ButtonStyle.Primary,
                onClick = contract::onBack,
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

            Spacer(modifier = Modifier.navigationBarsPadding())
        },
    )
}

@Composable
private fun ExercisesSectionHeader(
    modifier: Modifier = Modifier,
    count: Int,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = AppTokens.strings.res(Res.string.exercises),
            style = AppTokens.typography.h4(),
            color = AppTokens.colors.text.primary,
        )

        Text(
            text = count.toString(),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.tertiary,
        )
    }
}

private fun trainingCompletedPreviewState(): TrainingCompletedState {
    val createdAt = DateTimeFormatState.of(
        value = LocalDateTime(
            year = 2026,
            month = 10,
            day = 16,
            hour = 12,
            minute = 0,
        ),
        range = DateRangePresets.infinity(),
        format = DateFormat.DateOnly.DateMmmDdYyyy,
    )
    val base = stubTraining()
    val exercise = base.exercises.first()
    val iteration = stubPendingIteration().copy(
        id = "completed-set",
        repetitions = RepetitionsFormatState.of(10),
        externalWeight = VolumeFormatState.of(40f),
    )
    val exercises = List(3) { index ->
        exercise.copy(
            id = "completed-exercise-$index",
            exerciseExample = exercise.exerciseExample.copy(
                id = "completed-example-$index",
                name = if (index == 0) {
                    "Жим гантелей лёжа на наклонной скамье нейтральным хватом"
                } else {
                    exercise.exerciseExample.name
                },
                lastUsed = createdAt,
            ),
            iterations = List(3) { setIndex ->
                iteration.copy(id = "completed-set-$index-$setIndex")
            }.toPersistentList(),
            createdAt = createdAt,
            total = exercise.total.copy(
                volume = VolumeFormatState.of(1_200f),
                intensity = IntensityFormatState.of(40f),
                repetitions = RepetitionsFormatState.of(30),
            ),
        )
    }.toPersistentList()
    val training = base.copy(
        id = "completed-training",
        exercises = exercises,
        createdAt = createdAt,
        duration = DurationFormatState.of(45.minutes),
        total = base.total.copy(
            volume = VolumeFormatState.of(3_600f),
            intensity = IntensityFormatState.of(40f),
            repetitions = RepetitionsFormatState.of(90),
        ),
    )
    val timeline = exercises.mapIndexed { index, value ->
        TimelineState.MiddleExercise(
            exerciseState = value,
            position = TrainingPosition.SINGLE,
            key = value.id,
            indexInTraining = index + 1,
        )
    }.toPersistentList()

    return TrainingCompletedState(
        savedTrainingId = training.id,
        timeline = timeline,
        training = training,
        muscleLoad = stubMuscleLoadSummary(),
    )
}

@AppPreview
@Composable
private fun TrainingCompletedPopulatedPreview() {
    PreviewContainer {
        TrainingCompletedScreen(
            state = trainingCompletedPreviewState(),
            loaders = persistentSetOf(),
            contract = TrainingCompletedContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingCompletedUpdatedPreview() {
    PreviewContainer {
        TrainingCompletedScreen(
            state = trainingCompletedPreviewState().copy(
                title = UiText.Res(value = Res.string.training_completed_updated_title),
                description = UiText.Res(value = Res.string.training_completed_updated_description),
            ),
            loaders = persistentSetOf(),
            contract = TrainingCompletedContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingCompletedWithoutMuscleLoadPreview() {
    PreviewContainer {
        TrainingCompletedScreen(
            state = trainingCompletedPreviewState().copy(muscleLoad = null),
            loaders = persistentSetOf(),
            contract = TrainingCompletedContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingCompletedWithoutDetailsPreview() {
    PreviewContainer {
        TrainingCompletedScreen(
            state = TrainingCompletedState(savedTrainingId = "saved-training"),
            loaders = persistentSetOf(),
            contract = TrainingCompletedContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingCompletedEmptyTrainingPreview() {
    val state = trainingCompletedPreviewState()
    val training = requireNotNull(state.training)

    PreviewContainer {
        TrainingCompletedScreen(
            state = state.copy(
                training = training.copy(
                    exercises = persistentListOf(),
                    duration = DurationFormatState.Empty(),
                    total = training.total.copy(
                        volume = VolumeFormatState.Empty(),
                        repetitions = RepetitionsFormatState.Empty(),
                    ),
                ),
                timeline = persistentListOf(),
                muscleLoad = null,
            ),
            loaders = persistentSetOf(),
            contract = TrainingCompletedContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingCompletedLongValuesPreview() {
    val state = trainingCompletedPreviewState()
    val training = requireNotNull(state.training)

    PreviewContainer {
        TrainingCompletedScreen(
            state = state.copy(
                training = training.copy(
                    duration = DurationFormatState.of(23.hours + 59.minutes),
                    total = training.total.copy(
                        volume = VolumeFormatState.of(12_345_678f),
                        repetitions = RepetitionsFormatState.of(123_456),
                    ),
                ),
            ),
            loaders = persistentSetOf(),
            contract = TrainingCompletedContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingCompletedInitialPreview() {
    PreviewContainer {
        TrainingCompletedScreen(
            state = TrainingCompletedState(),
            loaders = persistentSetOf(),
            contract = TrainingCompletedContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingCompletedLoadingPreview() {
    PreviewContainer {
        TrainingCompletedScreen(
            state = trainingCompletedPreviewState(),
            loaders = persistentSetOf(TrainingCompletedLoader.SaveTraining),
            contract = TrainingCompletedContract.Empty,
        )
    }
}
