package com.grippo.design.components.training.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import com.grippo.core.state.formatters.MultiplierFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.trainings.IterationState
import com.grippo.core.state.trainings.stubPendingIteration
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.training.IterationCard
import com.grippo.design.components.training.IterationCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.Warning
import com.grippo.design.resources.provider.repetitions_placeholder
import com.grippo.design.resources.provider.set_value
import com.grippo.design.resources.provider.training_previous_set
import com.grippo.design.resources.provider.training_unusual_reps_subtitle
import com.grippo.design.resources.provider.training_unusual_reps_title
import com.grippo.design.resources.provider.training_unusual_weight_subtitle
import com.grippo.design.resources.provider.training_unusual_weight_title
import com.grippo.design.resources.provider.volume_placeholder

@Composable
internal fun EditableIterationCard(
    modifier: Modifier = Modifier,
    value: IterationState,
    style: IterationCardStyle.Editable,
) {
    val setDescription = AppTokens.strings.res(Res.string.set_value, style.number)
    val calculation = value.volumeCalculation()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppTokens.colors.background.screen)
            .padding(vertical = AppTokens.dp.contentPadding.subContent),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier.heightIn(min = AppTokens.dp.iterationCard.editor.minActionHeight),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(AppTokens.dp.iterationCard.numberSize)
                    .background(
                        color = AppTokens.colors.background.card,
                        shape = RoundedCornerShape(AppTokens.dp.iterationCard.radius),
                    )
                    .clearAndSetSemantics { contentDescription = setDescription },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = style.number.toString(),
                    style = AppTokens.typography.b12Semi(),
                    color = AppTokens.colors.text.secondary,
                )
            }
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
                EditableIterationMetric(
                    modifier = Modifier.weight(1f),
                    label = AppTokens.strings.res(Res.string.volume_placeholder),
                    value = value.volume().shortAnnotated(),
                    pending = value.isPending,
                    warning = style.unusualWeight,
                    onClick = style.onVolumeClick,
                )
                EditableIterationMetric(
                    modifier = Modifier.weight(1f),
                    label = AppTokens.strings.res(Res.string.repetitions_placeholder),
                    value = value.repetitions.shortAnnotated(),
                    pending = value.repetitions.value == null,
                    warning = style.unusualRepetitions,
                    onClick = style.onRepetitionClick,
                )
            }
            if (calculation != null) {
                Text(
                    text = calculation,
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }
            style.context?.invoke()
            if (style.unusualWeight) {
                EditableIterationWarning(
                    title = AppTokens.strings.res(Res.string.training_unusual_weight_title),
                    detail = AppTokens.strings.res(Res.string.training_unusual_weight_subtitle),
                    onClick = style.onVolumeClick,
                )
            }
            if (style.unusualRepetitions) {
                EditableIterationWarning(
                    title = AppTokens.strings.res(Res.string.training_unusual_reps_title),
                    detail = AppTokens.strings.res(Res.string.training_unusual_reps_subtitle),
                    onClick = style.onRepetitionClick,
                )
            }
        }
    }
}

@Composable
private fun EditableIterationMetric(
    modifier: Modifier = Modifier,
    label: String,
    value: AnnotatedString,
    pending: Boolean,
    warning: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .semantics(mergeDescendants = true) {}
            .scalableClick(role = Role.Button, onClick = onClick)
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
private fun EditableIterationWarning(
    modifier: Modifier = Modifier,
    title: String,
    detail: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .scalableClick(role = Role.Button, onClick = onClick)
            .heightIn(min = AppTokens.dp.iterationCard.editor.minActionHeight),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        verticalAlignment = Alignment.CenterVertically,
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

@AppPreview
@Composable
private fun IterationCardEditableExternalPreview() {
    val value = stubPendingIteration().copy(
        id = "editable-iteration-external-preview",
        externalWeight = VolumeFormatState.of(60f),
        repetitions = RepetitionsFormatState.of(10),
    )

    PreviewContainer {
        IterationCard(
            value = value,
            style = IterationCardStyle.Editable(
                number = 1,
                unusualWeight = false,
                unusualRepetitions = false,
                onVolumeClick = {},
                onRepetitionClick = {},
            ),
        )
        IterationCard(
            value = value,
            style = IterationCardStyle.Editable(
                number = 2,
                unusualWeight = false,
                unusualRepetitions = false,
                onVolumeClick = {},
                onRepetitionClick = {},
                context = {
                    Text(
                        text = AppTokens.strings.res(
                            Res.string.training_previous_set,
                            VolumeFormatState.of(55f).short(),
                            RepetitionsFormatState.of(10).value.toString(),
                        ),
                        style = AppTokens.typography.b12Med(),
                        color = AppTokens.colors.text.tertiary,
                    )
                },
            ),
        )
    }
}

@AppPreview
@Composable
private fun IterationCardEditableBodyPreview() {
    val value = stubPendingIteration().copy(
        id = "editable-iteration-body-preview",
        bodyWeight = WeightFormatState.of(80f),
        bodyMultiplier = MultiplierFormatState.of(0.65f),
        repetitions = RepetitionsFormatState.of(10),
    )

    PreviewContainer {
        IterationCard(
            value = value,
            style = IterationCardStyle.Editable(
                number = 1,
                unusualWeight = false,
                unusualRepetitions = false,
                onVolumeClick = {},
                onRepetitionClick = {},
            ),
        )
    }
}

@AppPreview
@Composable
private fun IterationCardEditableExtraWeightPreview() {
    val value = stubPendingIteration().copy(
        id = "editable-iteration-extra-weight-preview",
        extraWeight = VolumeFormatState.of(20f),
        bodyWeight = WeightFormatState.of(80f),
        bodyMultiplier = MultiplierFormatState.of(0.65f),
        repetitions = RepetitionsFormatState.of(10),
    )

    PreviewContainer {
        IterationCard(
            value = value,
            style = IterationCardStyle.Editable(
                number = 1,
                unusualWeight = false,
                unusualRepetitions = false,
                onVolumeClick = {},
                onRepetitionClick = {},
            ),
        )
    }
}

@AppPreview
@Composable
private fun IterationCardEditableAssistedPreview() {
    val value = stubPendingIteration().copy(
        id = "editable-iteration-assisted-preview",
        assistWeight = VolumeFormatState.of(10f),
        bodyWeight = WeightFormatState.of(80f),
        bodyMultiplier = MultiplierFormatState.of(0.65f),
        repetitions = RepetitionsFormatState.of(10),
    )

    PreviewContainer {
        IterationCard(
            value = value,
            style = IterationCardStyle.Editable(
                number = 1,
                unusualWeight = false,
                unusualRepetitions = false,
                onVolumeClick = {},
                onRepetitionClick = {},
            ),
        )
    }
}

@AppPreview
@Composable
private fun IterationCardEditablePendingPreview() {
    val pending = stubPendingIteration().copy(
        id = "editable-iteration-pending",
        repetitions = RepetitionsFormatState.Empty(),
    )
    val pendingRepetitions = stubPendingIteration().copy(
        id = "editable-iteration-pending-repetitions-preview",
        externalWeight = VolumeFormatState.of(60f),
        repetitions = RepetitionsFormatState.Empty(),
    )

    PreviewContainer {
        IterationCard(
            value = pending,
            style = IterationCardStyle.Editable(
                number = 1,
                unusualWeight = false,
                unusualRepetitions = false,
                onVolumeClick = {},
                onRepetitionClick = {},
            ),
        )
        IterationCard(
            value = pending.copy(repetitions = RepetitionsFormatState.of(10)),
            style = IterationCardStyle.Editable(
                number = 2,
                unusualWeight = false,
                unusualRepetitions = false,
                onVolumeClick = {},
                onRepetitionClick = {},
            ),
        )
        IterationCard(
            value = pendingRepetitions,
            style = IterationCardStyle.Editable(
                number = 3,
                unusualWeight = false,
                unusualRepetitions = false,
                onVolumeClick = {},
                onRepetitionClick = {},
            ),
        )
    }
}

@AppPreview
@Composable
private fun IterationCardEditableWeightWarningPreview() {
    val value = stubPendingIteration().copy(
        id = "editable-iteration-weight-warning-preview",
        externalWeight = VolumeFormatState.of(600f),
        repetitions = RepetitionsFormatState.of(10),
    )

    PreviewContainer {
        IterationCard(
            value = value,
            style = IterationCardStyle.Editable(
                number = 1,
                unusualWeight = true,
                unusualRepetitions = false,
                onVolumeClick = {},
                onRepetitionClick = {},
            ),
        )
    }
}

@AppPreview
@Composable
private fun IterationCardEditableRepetitionWarningPreview() {
    val value = stubPendingIteration().copy(
        id = "editable-iteration-repetition-warning-preview",
        externalWeight = VolumeFormatState.of(60f),
        repetitions = RepetitionsFormatState.of(100),
    )

    PreviewContainer {
        IterationCard(
            value = value,
            style = IterationCardStyle.Editable(
                number = 1,
                unusualWeight = false,
                unusualRepetitions = true,
                onVolumeClick = {},
                onRepetitionClick = {},
            ),
        )
    }
}

@AppPreview
@Composable
private fun IterationCardEditableCombinedWarningsPreview() {
    val value = stubPendingIteration().copy(
        id = "editable-iteration-combined-warnings-preview",
        externalWeight = VolumeFormatState.of(600f),
        repetitions = RepetitionsFormatState.of(100),
    )

    PreviewContainer {
        IterationCard(
            value = value,
            style = IterationCardStyle.Editable(
                number = 1,
                unusualWeight = true,
                unusualRepetitions = true,
                onVolumeClick = {},
                onRepetitionClick = {},
            ),
        )
    }
}

@AppPreview
@Composable
private fun IterationCardEditableWarningContextPreview() {
    val value = stubPendingIteration().copy(
        id = "editable-iteration-warning-context-preview",
        extraWeight = VolumeFormatState.of(200f),
        bodyWeight = WeightFormatState.of(80f),
        bodyMultiplier = MultiplierFormatState.of(0.65f),
        repetitions = RepetitionsFormatState.of(10),
    )

    PreviewContainer {
        IterationCard(
            value = value,
            style = IterationCardStyle.Editable(
                number = 1,
                unusualWeight = true,
                unusualRepetitions = false,
                onVolumeClick = {},
                onRepetitionClick = {},
                context = {
                    Text(
                        text = AppTokens.strings.res(
                            Res.string.training_previous_set,
                            VolumeFormatState.of(72f).short(),
                            RepetitionsFormatState.of(10).value.toString(),
                        ),
                        style = AppTokens.typography.b12Med(),
                        color = AppTokens.colors.text.tertiary,
                    )
                },
            ),
        )
    }
}
