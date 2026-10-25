package com.grippo.draft.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.trainings.stubExercise
import com.grippo.core.state.trainings.stubPendingIteration
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.divider.ContentDivider
import com.grippo.design.components.frames.OverlayContainer
import com.grippo.design.components.frames.OverlayStyle
import com.grippo.design.components.training.ExerciseCard
import com.grippo.design.components.training.ExerciseCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.continue_btn
import com.grippo.design.resources.provider.draft_training_alert_description_add
import com.grippo.design.resources.provider.draft_training_alert_description_edit
import com.grippo.design.resources.provider.draft_training_alert_title_add
import com.grippo.design.resources.provider.draft_training_alert_title_edit
import com.grippo.design.resources.provider.draft_training_contents
import com.grippo.design.resources.provider.draft_training_empty
import com.grippo.design.resources.provider.start_new
import com.grippo.draft.training.components.DraftTrainingSkeleton
import com.grippo.draft.training.components.DraftTrainingSummary
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@Composable
internal fun DraftTrainingScreen(
    state: DraftTrainingState,
    loaders: ImmutableSet<DraftTrainingLoader>,
    contract: DraftTrainingContract,
) = BaseComposeScreen(
    background = ScreenBackground.Color(AppTokens.colors.background.dialog),
) {
    val isLoading = DraftTrainingLoader.Draft in loaders
    val actionState = if (loaders.isEmpty()) ButtonState.Enabled else ButtonState.Disabled
    val startNewState = when {
        DraftTrainingLoader.DeleteDraft in loaders -> ButtonState.Loading
        else -> actionState
    }

    if (!isLoading) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = AppTokens.dp.dialog.horizontalPadding,
                    end = AppTokens.dp.dialog.horizontalPadding,
                    top = AppTokens.dp.dialog.top,
                ),
        ) {
            Column(
                modifier = Modifier.padding(bottom = AppTokens.dp.contentPadding.block),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            ) {
                Text(
                    text = when (state.stage) {
                        StageState.Add, StageState.Draft -> AppTokens.strings.res(
                            Res.string.draft_training_alert_title_add,
                        )
                        is StageState.Edit -> AppTokens.strings.res(Res.string.draft_training_alert_title_edit)
                    },
                    style = AppTokens.typography.h3(),
                    color = AppTokens.colors.text.primary,
                )
                Text(
                    text = when (state.stage) {
                        StageState.Add, StageState.Draft -> AppTokens.strings.res(
                            Res.string.draft_training_alert_description_add,
                        )
                        is StageState.Edit -> AppTokens.strings.res(Res.string.draft_training_alert_description_edit)
                    },
                    style = AppTokens.typography.b14Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }
            if (state.exercises.isNotEmpty()) {
                DraftTrainingSummary(state = state)
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = AppTokens.dp.contentPadding.block,
                            bottom = AppTokens.dp.contentPadding.text,
                        ),
                    text = AppTokens.strings.res(Res.string.draft_training_contents),
                    style = AppTokens.typography.h4(),
                    color = AppTokens.colors.text.primary,
                )
            }
        }
    }

    OverlayContainer(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, false),
        contentPadding = PaddingValues(
            start = AppTokens.dp.dialog.horizontalPadding,
            end = AppTokens.dp.dialog.horizontalPadding,
            top = if (isLoading) AppTokens.dp.dialog.top else 0.dp,
            bottom = AppTokens.dp.contentPadding.block,
        ),
        overlay = AppTokens.colors.background.dialog,
        bottomStyle = OverlayStyle.Stacked(fadeHeight = AppTokens.dp.contentPadding.content),
        content = { containerModifier, resolvedPadding ->
            LazyColumn(
                modifier = containerModifier.fillMaxWidth(),
                contentPadding = resolvedPadding,
            ) {
                if (isLoading) {
                    item(key = "skeleton", contentType = "skeleton") {
                        DraftTrainingSkeleton(modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    if (state.exercises.isEmpty()) {
                        item(key = "empty", contentType = "empty") {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = AppTokens.strings.res(Res.string.draft_training_empty),
                                style = AppTokens.typography.b14Med(),
                                color = AppTokens.colors.text.secondary,
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = state.exercises,
                            key = { _, exercise -> exercise.id },
                            contentType = { _, _ -> "exercise" },
                        ) { index, exercise ->
                            ExerciseCard(
                                modifier = Modifier.fillMaxWidth(),
                                value = exercise,
                                style = ExerciseCardStyle.Medium(),
                            )
                            if (index < state.exercises.lastIndex) {
                                ContentDivider(
                                    modifier = Modifier.padding(
                                        start = AppTokens.dp.exerciseExampleImage.compact.size +
                                                AppTokens.dp.contentPadding.subContent,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        },
        bottom = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        start = AppTokens.dp.dialog.horizontalPadding,
                        end = AppTokens.dp.dialog.horizontalPadding,
                        bottom = AppTokens.dp.dialog.bottom,
                    ),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    content = ButtonContent.Text(
                        text = AppTokens.strings.res(Res.string.continue_btn),
                    ),
                    style = ButtonStyle.Primary,
                    state = actionState,
                    onClick = contract::onContinue,
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    content = ButtonContent.Text(
                        text = AppTokens.strings.res(Res.string.start_new),
                    ),
                    style = ButtonStyle.Secondary,
                    state = startNewState,
                    onClick = contract::onStartNew,
                )
            }
        },
    )
}

@AppPreview
@Composable
private fun DraftTrainingInitialPreview() {
    PreviewContainer {
        DraftTrainingScreen(
            state = DraftTrainingState(),
            loaders = persistentSetOf(DraftTrainingLoader.Draft),
            contract = DraftTrainingContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun DraftTrainingMixedPreview() {
    PreviewContainer {
        DraftTrainingScreen(
            state = draftTrainingPreviewState(),
            loaders = persistentSetOf(),
            contract = DraftTrainingContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun DraftTrainingDeletingPreview() {
    PreviewContainer {
        DraftTrainingScreen(
            state = draftTrainingPreviewState(),
            loaders = persistentSetOf(DraftTrainingLoader.DeleteDraft),
            contract = DraftTrainingContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun DraftTrainingEditPreview() {
    PreviewContainer {
        DraftTrainingScreen(
            state = draftTrainingPreviewState(stage = StageState.Edit("saved-workout")),
            loaders = persistentSetOf(),
            contract = DraftTrainingContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun DraftTrainingEmptyPreview() {
    PreviewContainer {
        DraftTrainingScreen(
            state = DraftTrainingState(
                stage = StageState.Add,
                duration = DurationFormatState.of(Duration.ZERO),
            ),
            loaders = persistentSetOf(),
            contract = DraftTrainingContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun DraftTrainingPlannedPreview() {
    PreviewContainer {
        DraftTrainingScreen(
            state = draftTrainingPreviewState(filledSets = 0).copy(
                duration = DurationFormatState.of(Duration.ZERO),
            ),
            loaders = persistentSetOf(),
            contract = DraftTrainingContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun DraftTrainingFilledPreview() {
    PreviewContainer {
        DraftTrainingScreen(
            state = draftTrainingPreviewState(filledSets = 3),
            loaders = persistentSetOf(),
            contract = DraftTrainingContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun DraftTrainingLongContentPreview() {
    PreviewContainer {
        val base = draftTrainingPreviewState()
        val exercises = List(10) { index ->
            val exercise = base.exercises[index % base.exercises.size]
            val name = "Жим гантелей лежачи на похилій лаві нейтральним хватом"
            exercise.copy(
                id = "long-exercise-$index",
                name = name,
                exerciseExample = exercise.exerciseExample.copy(
                    id = "long-example-$index",
                    name = name,
                ),
            )
        }.toPersistentList()

        DraftTrainingScreen(
            state = base.copy(exercises = exercises),
            loaders = persistentSetOf(),
            contract = DraftTrainingContract.Empty,
        )
    }
}

private fun draftTrainingPreviewState(
    stage: StageState = StageState.Add,
    filledSets: Int? = null,
): DraftTrainingState {
    val names = listOf("Dumbbell bench press", "Lat pulldown", "Leg press")
    val exercises = names.mapIndexed { exerciseIndex, name ->
        val base = stubExercise()
        val recordedCount = filledSets ?: (2 - exerciseIndex)
        base.copy(
            id = "exercise-$exerciseIndex",
            name = name,
            exerciseExample = base.exerciseExample.copy(
                id = "example-$exerciseIndex",
                name = name,
                imageUrl = null,
            ),
            iterations = List(3) { setIndex ->
                stubPendingIteration().copy(
                    id = "exercise-$exerciseIndex-set-$setIndex",
                    repetitions = RepetitionsFormatState.of(10),
                    externalWeight = if (setIndex < recordedCount) {
                        VolumeFormatState.of(40f + exerciseIndex * 10f)
                    } else {
                        VolumeFormatState.Empty()
                    },
                )
            }.toPersistentList(),
        )
    }.toPersistentList()

    return DraftTrainingState(
        stage = stage,
        exercises = exercises,
        duration = DurationFormatState.of(18.minutes),
    )
}
