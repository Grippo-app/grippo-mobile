package com.grippo.confirm.training.completion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.stubExercise
import com.grippo.core.state.trainings.stubPendingIteration
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.inputs.InputDuration
import com.grippo.design.components.training.TrainingStat
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.confirm_training_completion_continue
import com.grippo.design.resources.provider.confirm_training_completion_exercises
import com.grippo.design.resources.provider.confirm_training_completion_hint
import com.grippo.design.resources.provider.confirm_training_completion_save
import com.grippo.design.resources.provider.confirm_training_completion_sets
import com.grippo.design.resources.provider.confirm_training_completion_subtitle
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ConfirmTrainingCompletionScreen(
    state: ConfirmTrainingCompletionState,
    loaders: ImmutableSet<ConfirmTrainingCompletionLoader>,
    contract: ConfirmTrainingCompletionContract,
) = BaseComposeScreen(background = ScreenBackground.Color(AppTokens.colors.background.dialog)) {
    val recordedExercises = state.exercises.count { exercise ->
        exercise.iterations.any { !it.isPending }
    }
    val recordedSets = state.exercises.sumOf { exercise ->
        exercise.iterations.count { !it.isPending }
    }
    val buttonState = when (state.duration) {
        is DurationFormatState.Invalid -> ButtonState.Disabled
        is DurationFormatState.Empty -> ButtonState.Disabled
        is DurationFormatState.Valid -> ButtonState.Enabled
    }

    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

    Text(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth(),
        text = AppTokens.strings.res(Res.string.confirm_training_completion_subtitle),
        style = AppTokens.typography.b14Med(),
        color = AppTokens.colors.text.secondary,
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    Row(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth()
            .background(
                color = AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.metrics.panel.large.radius),
            )
            .padding(
                horizontal = AppTokens.dp.metrics.panel.large.horizontalPadding,
                vertical = AppTokens.dp.metrics.panel.large.verticalPadding,
            ),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        TrainingStat(
            modifier = Modifier.weight(1f),
            label = AppTokens.strings.res(Res.string.confirm_training_completion_exercises),
            value = AnnotatedString(recordedExercises.toString()),
            contentColor = AppTokens.colors.text.primary,
        )
        TrainingStat(
            modifier = Modifier.weight(1f),
            label = AppTokens.strings.res(Res.string.confirm_training_completion_sets),
            value = AnnotatedString(recordedSets.toString()),
            contentColor = AppTokens.colors.text.primary,
        )
    }

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    InputDuration(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth(),
        value = state.duration,
        onClick = contract::onDurationInputClick,
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.text))

    Text(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth(),
        text = AppTokens.strings.res(Res.string.confirm_training_completion_hint),
        style = AppTokens.typography.b12Med(),
        color = AppTokens.colors.text.secondary,
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    Button(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth(),
        content = ButtonContent.Text(
            text = AppTokens.strings.res(Res.string.confirm_training_completion_save),
        ),
        state = buttonState,
        style = ButtonStyle.Primary,
        onClick = contract::onConfirm,
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

    Button(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth(),
        content = ButtonContent.Text(
            text = AppTokens.strings.res(Res.string.confirm_training_completion_continue),
        ),
        style = ButtonStyle.Secondary,
        onClick = contract::onBack,
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))

    Spacer(modifier = Modifier.navigationBarsPadding())
}

@AppPreview
@Composable
private fun CompletedTrainingPreview() {
    PreviewContainer {
        ConfirmTrainingCompletionScreen(
            state = ConfirmTrainingCompletionState(
                duration = DurationFormatState.of(45.minutes),
                exercises = previewExercises(),
            ),
            contract = ConfirmTrainingCompletionContract.Empty,
            loaders = persistentSetOf(),
        )
    }
}

@AppPreview
@Composable
private fun MixedCompletedAndPendingPreview() {
    val exercise = previewExercises().first()
    val pendingSet = stubPendingIteration().copy(
        id = "pending-set",
        repetitions = RepetitionsFormatState.of(10),
    )

    PreviewContainer {
        ConfirmTrainingCompletionScreen(
            state = ConfirmTrainingCompletionState(
                duration = DurationFormatState.of(45.minutes),
                exercises = persistentListOf(
                    exercise.copy(
                        iterations = persistentListOf(
                            exercise.iterations[0],
                            exercise.iterations[1],
                            pendingSet,
                        ),
                    ),
                    exercise.copy(
                        id = "partially-recorded-exercise",
                        iterations = persistentListOf(exercise.iterations[0]),
                    ),
                    exercise.copy(
                        id = "pending-only-exercise",
                        iterations = persistentListOf(pendingSet),
                    ),
                ),
            ),
            loaders = persistentSetOf(),
            contract = ConfirmTrainingCompletionContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun MaximumDurationPreview() {
    PreviewContainer {
        ConfirmTrainingCompletionScreen(
            state = ConfirmTrainingCompletionState(
                duration = DurationFormatState.of(DurationFormatState.DurationLimitation.endInclusive),
                exercises = previewExercises(),
            ),
            loaders = persistentSetOf(),
            contract = ConfirmTrainingCompletionContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun InvalidDurationPreview() {
    PreviewContainer {
        ConfirmTrainingCompletionScreen(
            state = ConfirmTrainingCompletionState(
                duration = DurationFormatState.of(24.hours),
                exercises = previewExercises(),
            ),
            loaders = persistentSetOf(),
            contract = ConfirmTrainingCompletionContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun EmptyDurationPreview() {
    PreviewContainer {
        ConfirmTrainingCompletionScreen(
            state = ConfirmTrainingCompletionState(
                duration = DurationFormatState.Empty(),
                exercises = previewExercises(),
            ),
            loaders = persistentSetOf(),
            contract = ConfirmTrainingCompletionContract.Empty,
        )
    }
}

private fun previewExercises(): ImmutableList<ExerciseState> {
    val recordedSet = stubPendingIteration().copy(
        id = "recorded-set",
        externalWeight = VolumeFormatState.of(60f),
        repetitions = RepetitionsFormatState.of(10),
    )
    val exercise = stubExercise().copy(
        id = "recorded-exercise",
        iterations = persistentListOf(
            recordedSet.copy(id = "recorded-set-1"),
            recordedSet.copy(id = "recorded-set-2"),
            recordedSet.copy(id = "recorded-set-3"),
        ),
    )

    return (1..4).map { index ->
        exercise.copy(id = "recorded-exercise-$index")
    }.toPersistentList()
}
