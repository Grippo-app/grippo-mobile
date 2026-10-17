package com.grippo.training.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import com.grippo.core.state.examples.ExerciseExampleComponentsState
import com.grippo.core.state.formatters.MultiplierFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.trainings.IterationState
import com.grippo.core.state.trainings.stubPendingIteration
import com.grippo.design.components.divider.ContentDivider
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.assist_weight_placeholder
import com.grippo.design.resources.provider.body_weight_placeholder
import com.grippo.design.resources.provider.extra_weight_placeholder
import com.grippo.design.resources.provider.icons.Body
import com.grippo.design.resources.provider.icons.Dumbbell
import com.grippo.design.resources.provider.icons.Kettlebell
import com.grippo.design.resources.provider.icons.Warning
import com.grippo.design.resources.provider.repetitions_placeholder
import com.grippo.design.resources.provider.set_value
import com.grippo.design.resources.provider.training_external_weight
import com.grippo.design.resources.provider.training_previous_set
import com.grippo.design.resources.provider.training_unusual_reps_subtitle
import com.grippo.design.resources.provider.training_unusual_reps_title
import com.grippo.design.resources.provider.training_unusual_weight_subtitle
import com.grippo.design.resources.provider.training_unusual_weight_title
import com.grippo.design.resources.provider.volume_placeholder

@Composable
internal fun TrainingExerciseSetRow(
    modifier: Modifier = Modifier,
    value: IterationState,
    number: Int,
    previousIteration: IterationState? = null,
    unusualWeight: Boolean,
    unusualRepetitions: Boolean,
    showDivider: Boolean,
    onVolumeClick: () -> Unit,
    onRepetitionClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .background(AppTokens.colors.background.screen)
            .padding(vertical = AppTokens.dp.contentPadding.subContent),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .widthIn(min = AppTokens.dp.iterationCard.editor.numberMinWidth)
                    .heightIn(min = AppTokens.dp.iterationCard.editor.minActionHeight),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = AppTokens.strings.res(Res.string.set_value, number),
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                    verticalAlignment = Alignment.Top,
                ) {
                    SetMetric(
                        modifier = Modifier.weight(1f),
                        label = buildString {
                            append(AppTokens.strings.res(Res.string.volume_placeholder))
                            value.volumeCalculation()?.let { append(" · $it") }
                        },
                        value = value.volume().shortAnnotated(),
                        pending = value.isPending,
                        warning = unusualWeight,
                        onClick = onVolumeClick,
                    )
                    SetMetric(
                        modifier = Modifier.weight(1f),
                        label = AppTokens.strings.res(Res.string.repetitions_placeholder),
                        value = value.repetitions.shortAnnotated(),
                        pending = value.repetitions.value == null,
                        warning = unusualRepetitions,
                        onClick = onRepetitionClick,
                    )
                }
                previousIteration?.let { previous ->
                    Text(
                        text = AppTokens.strings.res(
                            Res.string.training_previous_set,
                            previous.volume().short(),
                            previous.repetitions.short(),
                        ),
                        style = AppTokens.typography.b12Med(),
                        color = AppTokens.colors.text.secondary,
                    )
                }
            }
        }
        if (unusualWeight) {
            SetWarning(
                title = AppTokens.strings.res(Res.string.training_unusual_weight_title),
                detail = AppTokens.strings.res(Res.string.training_unusual_weight_subtitle),
                onClick = onVolumeClick,
            )
        }
        if (unusualRepetitions) {
            SetWarning(
                title = AppTokens.strings.res(Res.string.training_unusual_reps_title),
                detail = AppTokens.strings.res(Res.string.training_unusual_reps_subtitle),
                onClick = onRepetitionClick,
            )
        }
        if (showDivider) {
            ContentDivider()
        }
    }
}

@Composable
internal fun TrainingExerciseLoadType(
    modifier: Modifier = Modifier,
    components: ExerciseExampleComponentsState,
) {
    val (icon, label) = when (components) {
        is ExerciseExampleComponentsState.External -> AppTokens.icons.Dumbbell to Res.string.training_external_weight
        is ExerciseExampleComponentsState.BodyOnly -> AppTokens.icons.Body to Res.string.body_weight_placeholder
        is ExerciseExampleComponentsState.BodyAndExtra -> AppTokens.icons.Kettlebell to Res.string.extra_weight_placeholder
        is ExerciseExampleComponentsState.BodyAndAssist -> AppTokens.icons.Body to Res.string.assist_weight_placeholder
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        Icon(
            modifier = Modifier.size(AppTokens.dp.iterationCard.editor.icon),
            imageVector = icon,
            contentDescription = null,
            tint = AppTokens.colors.icon.secondary,
        )
        Text(
            text = AppTokens.strings.res(label),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
        )
    }
}

@Composable
private fun SetMetric(
    modifier: Modifier,
    label: String,
    value: AnnotatedString,
    pending: Boolean,
    warning: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .semantics { role = Role.Button }
            .scalableClick(onClick = onClick)
            .heightIn(min = AppTokens.dp.iterationCard.editor.minActionHeight),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Text(
            text = label,
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
        )
        Text(
            text = value,
            style = AppTokens.typography.h5(),
            color = when {
                warning -> AppTokens.colors.semantic.warning
                pending -> AppTokens.colors.text.secondary
                else -> AppTokens.colors.text.primary
            },
        )
    }
}

@Composable
private fun SetWarning(
    title: String,
    detail: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.Button }
            .scalableClick(onClick = onClick)
            .heightIn(min = AppTokens.dp.iterationCard.editor.minActionHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        Icon(
            modifier = Modifier.size(AppTokens.dp.iterationCard.editor.icon),
            imageVector = AppTokens.icons.Warning,
            contentDescription = null,
            tint = AppTokens.colors.semantic.warning,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        ) {
            Text(
                text = title,
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.primary,
            )
            Text(
                text = detail,
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
            )
        }
    }
}

private fun trainingExerciseExternalIteration(): IterationState {
    return stubPendingIteration().copy(
        id = "training-set-external",
        externalWeight = VolumeFormatState.of(60f),
        repetitions = RepetitionsFormatState.of(10),
    )
}

private fun trainingExerciseBodyIteration(): IterationState {
    return trainingExerciseExternalIteration().copy(
        id = "training-set-body",
        externalWeight = VolumeFormatState.Empty(),
        bodyWeight = WeightFormatState.of(80f),
        bodyMultiplier = MultiplierFormatState.of(0.65f),
    )
}

@AppPreview
@Composable
private fun TrainingExerciseExternalSetPreview() {
    val iteration = trainingExerciseExternalIteration()
    PreviewContainer {
        TrainingExerciseLoadType(components = ExerciseExampleComponentsState.External(required = true))
        TrainingExerciseSetRow(
            modifier = Modifier.fillMaxWidth(),
            value = iteration,
            number = 1,
            previousIteration = iteration.copy(
                id = "training-set-previous",
                externalWeight = VolumeFormatState.of(55f),
            ),
            unusualWeight = false,
            unusualRepetitions = false,
            showDivider = false,
            onVolumeClick = {},
            onRepetitionClick = {},
        )
    }
}

@AppPreview
@Composable
private fun TrainingExerciseBodySetPreview() {
    PreviewContainer {
        TrainingExerciseLoadType(
            components = ExerciseExampleComponentsState.BodyOnly(required = true, bodyMultiplier = 0.65f),
        )
        TrainingExerciseSetRow(
            modifier = Modifier.fillMaxWidth(),
            value = trainingExerciseBodyIteration(),
            number = 1,
            unusualWeight = false,
            unusualRepetitions = false,
            showDivider = false,
            onVolumeClick = {},
            onRepetitionClick = {},
        )
    }
}

@AppPreview
@Composable
private fun TrainingExerciseExtraWeightSetPreview() {
    PreviewContainer {
        TrainingExerciseLoadType(
            components = ExerciseExampleComponentsState.BodyAndExtra(
                bodyRequired = true,
                bodyMultiplier = 0.65f,
                extraRequired = false,
            ),
        )
        TrainingExerciseSetRow(
            modifier = Modifier.fillMaxWidth(),
            value = trainingExerciseBodyIteration().copy(
                id = "training-set-extra",
                extraWeight = VolumeFormatState.of(20f),
            ),
            number = 1,
            unusualWeight = false,
            unusualRepetitions = false,
            showDivider = false,
            onVolumeClick = {},
            onRepetitionClick = {},
        )
    }
}

@AppPreview
@Composable
private fun TrainingExerciseAssistedSetPreview() {
    PreviewContainer {
        TrainingExerciseLoadType(
            components = ExerciseExampleComponentsState.BodyAndAssist(
                bodyRequired = true,
                bodyMultiplier = 0.65f,
                assistRequired = true,
            ),
        )
        TrainingExerciseSetRow(
            modifier = Modifier.fillMaxWidth(),
            value = trainingExerciseBodyIteration().copy(
                id = "training-set-assist",
                assistWeight = VolumeFormatState.of(10f),
            ),
            number = 1,
            unusualWeight = false,
            unusualRepetitions = false,
            showDivider = false,
            onVolumeClick = {},
            onRepetitionClick = {},
        )
    }
}

@AppPreview
@Composable
private fun TrainingExerciseWeightWarningSetPreview() {
    PreviewContainer {
        TrainingExerciseSetRow(
            modifier = Modifier.fillMaxWidth(),
            value = trainingExerciseExternalIteration().copy(externalWeight = VolumeFormatState.of(600f)),
            number = 1,
            unusualWeight = true,
            unusualRepetitions = false,
            showDivider = false,
            onVolumeClick = {},
            onRepetitionClick = {},
        )
    }
}

@AppPreview
@Composable
private fun TrainingExerciseRepetitionsWarningSetPreview() {
    PreviewContainer {
        TrainingExerciseSetRow(
            modifier = Modifier.fillMaxWidth(),
            value = trainingExerciseExternalIteration().copy(repetitions = RepetitionsFormatState.of(100)),
            number = 1,
            unusualWeight = false,
            unusualRepetitions = true,
            showDivider = false,
            onVolumeClick = {},
            onRepetitionClick = {},
        )
    }
}

@AppPreview
@Composable
private fun TrainingExerciseCombinedWarningsSetPreview() {
    PreviewContainer {
        TrainingExerciseSetRow(
            modifier = Modifier.fillMaxWidth(),
            value = trainingExerciseExternalIteration().copy(
                externalWeight = VolumeFormatState.of(600f),
                repetitions = RepetitionsFormatState.of(100),
            ),
            number = 1,
            unusualWeight = true,
            unusualRepetitions = true,
            showDivider = false,
            onVolumeClick = {},
            onRepetitionClick = {},
        )
    }
}

@AppPreview
@Composable
private fun TrainingExercisePendingSetPreview() {
    PreviewContainer {
        TrainingExerciseSetRow(
            modifier = Modifier.fillMaxWidth(),
            value = stubPendingIteration().copy(
                id = "training-set-pending",
                repetitions = RepetitionsFormatState.Empty(),
            ),
            number = 1,
            unusualWeight = false,
            unusualRepetitions = false,
            showDivider = true,
            onVolumeClick = {},
            onRepetitionClick = {},
        )
        TrainingExerciseSetRow(
            modifier = Modifier.fillMaxWidth(),
            value = stubPendingIteration().copy(
                id = "training-set-pending-volume",
                repetitions = RepetitionsFormatState.of(10),
            ),
            number = 2,
            unusualWeight = false,
            unusualRepetitions = false,
            showDivider = true,
            onVolumeClick = {},
            onRepetitionClick = {},
        )
        TrainingExerciseSetRow(
            modifier = Modifier.fillMaxWidth(),
            value = trainingExerciseExternalIteration().copy(
                id = "training-set-pending-repetitions",
                repetitions = RepetitionsFormatState.Empty(),
            ),
            number = 3,
            unusualWeight = false,
            unusualRepetitions = false,
            showDivider = false,
            onVolumeClick = {},
            onRepetitionClick = {},
        )
    }
}
