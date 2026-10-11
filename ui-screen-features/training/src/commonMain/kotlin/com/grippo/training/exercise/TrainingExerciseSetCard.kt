package com.grippo.training.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.grippo.core.state.examples.ExerciseExampleComponentsState
import com.grippo.core.state.trainings.IterationState
import com.grippo.core.state.trainings.stubIteration
import com.grippo.core.state.trainings.stubPendingIteration
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.Body
import com.grippo.design.resources.provider.icons.Dumbbell
import com.grippo.design.resources.provider.icons.Kettlebell
import com.grippo.design.resources.provider.body_weight_placeholder
import com.grippo.design.resources.provider.extra_weight_placeholder
import com.grippo.design.resources.provider.assist_weight_placeholder
import com.grippo.design.resources.provider.training_external_weight
import com.grippo.design.resources.provider.icons.Warning
import com.grippo.design.resources.provider.repetitions_placeholder
import com.grippo.design.resources.provider.set_value
import com.grippo.design.resources.provider.training_unusual_reps_subtitle
import com.grippo.design.resources.provider.training_unusual_weight_subtitle
import com.grippo.design.resources.provider.training_unusual_reps_title
import com.grippo.design.resources.provider.training_unusual_weight_title
import com.grippo.design.resources.provider.volume_placeholder

@Composable
internal fun TrainingExerciseSetCard(
    modifier: Modifier = Modifier,
    value: IterationState,
    number: Int,
    onVolumeClick: () -> Unit,
    onRepetitionClick: () -> Unit,
    unusualWeight: Boolean,
    unusualRepetitions: Boolean,
) {
    val shape = RoundedCornerShape(AppTokens.dp.iterationCard.radius)
    val warning = unusualWeight || unusualRepetitions
    Column(
        modifier = modifier
            .background(AppTokens.colors.background.card, shape)
            .border(
                AppTokens.dp.iterationCard.borderWidth,
                if (warning) AppTokens.colors.semantic.warning else AppTokens.colors.border.default,
                shape,
            )
            .padding(AppTokens.dp.contentPadding.content),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        Text(
            text = AppTokens.strings.res(Res.string.set_value, number),
            style = AppTokens.typography.b14Bold(),
            color = AppTokens.colors.text.secondary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        ) {
            SetValueCell(
                modifier = Modifier.weight(0.6f),
                label = AppTokens.strings.res(Res.string.volume_placeholder),
                value = value.volume().shortAnnotated(),
                pending = value.isPending,
                warning = unusualWeight,
                onClick = onVolumeClick,
            )
            SetValueCell(
                modifier = Modifier.weight(0.4f),
                label = AppTokens.strings.res(Res.string.repetitions_placeholder),
                value = value.repetitions.shortAnnotated(),
                pending = value.repetitions.value == null,
                warning = unusualRepetitions,
                onClick = onRepetitionClick,
            )
        }
        if (unusualWeight) {
            SetWarning(
                text = AppTokens.strings.res(Res.string.training_unusual_weight_title),
                detail = AppTokens.strings.res(Res.string.training_unusual_weight_subtitle),
                onClick = onVolumeClick,
            )
        }
        if (unusualRepetitions) {
            SetWarning(
                text = AppTokens.strings.res(Res.string.training_unusual_reps_title),
                detail = AppTokens.strings.res(Res.string.training_unusual_reps_subtitle),
                onClick = onRepetitionClick,
            )
        }
    }
}

@Composable
private fun SetValueCell(
    modifier: Modifier,
    label: String,
    value: AnnotatedString,
    pending: Boolean,
    warning: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(AppTokens.dp.iterationCard.radius)
    Column(
        modifier = modifier
            .clip(shape)
            .background(AppTokens.colors.background.screen)
            .semantics { role = Role.Button }
            .scalableClick(onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(AppTokens.dp.contentPadding.subContent),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Text(
            text = label,
            style = AppTokens.typography.b12Semi(),
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
private fun SetWarning(text: String, detail: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.Button }
            .scalableClick(onClick = onClick)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = AppTokens.icons.Warning,
            contentDescription = null,
            tint = AppTokens.colors.semantic.warning,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = text, style = AppTokens.typography.b14Bold(), color = AppTokens.colors.semantic.warning)
            Text(text = detail, style = AppTokens.typography.b12Semi(), color = AppTokens.colors.text.secondary)
        }
    }
}

@Composable
internal fun TrainingExerciseLoadType(components: ExerciseExampleComponentsState) {
    val (icon, label) = when (components) {
        is ExerciseExampleComponentsState.External -> AppTokens.icons.Dumbbell to Res.string.training_external_weight
        is ExerciseExampleComponentsState.BodyOnly -> AppTokens.icons.Body to Res.string.body_weight_placeholder
        is ExerciseExampleComponentsState.BodyAndExtra -> AppTokens.icons.Kettlebell to Res.string.extra_weight_placeholder
        is ExerciseExampleComponentsState.BodyAndAssist -> AppTokens.icons.Body to Res.string.assist_weight_placeholder
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        Icon(modifier = Modifier.size(20.dp), imageVector = icon, contentDescription = null, tint = AppTokens.colors.text.secondary)
        Text(text = AppTokens.strings.res(label), style = AppTokens.typography.b12Semi(), color = AppTokens.colors.text.secondary)
    }
}

@AppPreview
@Composable
private fun TrainingExerciseSetCardPreview() {
    PreviewContainer {
        TrainingExerciseSetCard(value = stubIteration(), number = 1, onVolumeClick = {}, onRepetitionClick = {}, unusualWeight = false, unusualRepetitions = false)
        TrainingExerciseSetCard(value = stubPendingIteration(), number = 2, onVolumeClick = {}, onRepetitionClick = {}, unusualWeight = false, unusualRepetitions = false)
        TrainingExerciseSetCard(value = stubIteration(), number = 3, onVolumeClick = {}, onRepetitionClick = {}, unusualWeight = true, unusualRepetitions = true)
    }
}
