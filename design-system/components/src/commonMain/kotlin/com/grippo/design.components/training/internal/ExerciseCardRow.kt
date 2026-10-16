package com.grippo.design.components.training.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.stubExercise
import com.grippo.core.state.trainings.stubPendingIteration
import com.grippo.design.components.example.ExerciseExampleImage
import com.grippo.design.components.example.ExerciseExampleImageStyle
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.ArrowRight
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun ExerciseCardRow(
    modifier: Modifier = Modifier,
    value: ExerciseState,
    isCompact: Boolean,
    onClick: (() -> Unit)?,
) {
    val imageStyle = if (isCompact) {
        ExerciseExampleImageStyle.COMPACT
    } else {
        ExerciseExampleImageStyle.MEDIUM
    }
    val titleStyle = if (isCompact) {
        AppTokens.typography.b14Semi()
    } else {
        AppTokens.typography.h5()
    }
    val contentSpacing = if (isCompact) {
        AppTokens.dp.contentPadding.subContent
    } else {
        AppTokens.dp.contentPadding.content
    }
    val clickModifier = if (onClick == null) {
        Modifier
    } else {
        Modifier.scalableClick(role = Role.Button, onClick = onClick)
    }

    Row(
        modifier = modifier
            .then(clickModifier)
            .padding(vertical = AppTokens.dp.contentPadding.subContent),
        horizontalArrangement = Arrangement.spacedBy(contentSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExerciseExampleImage(
            value = value.exerciseExample.imageUrl,
            style = imageStyle,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = value.exerciseExample.name,
                style = titleStyle,
                color = AppTokens.colors.text.primary,
            )
            ExerciseSummary(value = value, modifier = Modifier.fillMaxWidth())
        }
        if (onClick != null) {
            Icon(
                modifier = Modifier.size(AppTokens.dp.exerciseCard.chevron),
                imageVector = AppTokens.icons.ArrowRight,
                contentDescription = null,
                tint = AppTokens.colors.text.tertiary,
            )
        }
    }
}

@AppPreview
@Composable
private fun ExerciseCardRowVariantsPreview() {
    PreviewContainer {
        val base = stubExercise()
        val completed = stubPendingIteration().copy(
            id = "completed-set",
            externalWeight = VolumeFormatState.of(80f),
            repetitions = RepetitionsFormatState.of(8),
        )
        val exercise = base.copy(
            id = "bench-press",
            exerciseExample = base.exerciseExample.copy(name = "Dumbbell bench press"),
            iterations = persistentListOf(completed),
        )

        ExerciseCardRow(
            value = exercise,
            isCompact = true,
            onClick = {},
        )
        ExerciseCardRow(
            value = exercise,
            isCompact = false,
            onClick = {},
        )
    }
}
