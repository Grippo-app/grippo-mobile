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
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ExerciseCardRow(
    modifier: Modifier = Modifier,
    value: ExerciseState,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .scalableClick(onClick = onClick)
            .padding(vertical = AppTokens.dp.contentPadding.subContent),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExerciseExampleImage(
            value = value.exerciseExample.imageUrl,
            style = ExerciseExampleImageStyle.MEDIUM,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = value.exerciseExample.name,
                style = AppTokens.typography.h5(),
                color = AppTokens.colors.text.primary,
            )
            ExerciseSummary(value = value, modifier = Modifier.fillMaxWidth())
        }
        Icon(
            modifier = Modifier.size(AppTokens.dp.exerciseCard.chevron),
            imageVector = AppTokens.icons.ArrowRight,
            contentDescription = null,
            tint = AppTokens.colors.text.tertiary,
        )
    }
}

@AppPreview
@Composable
private fun ExerciseCardRowPresetPreview() {
    PreviewContainer {
        val base = stubExercise()
        val weights = listOf<Float>()
        val exercise = base.copy(
            exerciseExample = base.exerciseExample.copy(name = "Bench press"),
            iterations = List(3) { index ->
                stubPendingIteration().copy(
                    externalWeight = weights.getOrNull(index)?.let { VolumeFormatState.of(it) }
                        ?: VolumeFormatState.Empty(),
                )
            }.toPersistentList(),
        )
        ExerciseCardRow(
            value = exercise,
            onClick = {},
        )
    }
}

@AppPreview
@Composable
private fun ExerciseCardRowInProgressPreview() {
    PreviewContainer {
        val base = stubExercise()
        val weights = listOf<Float>(60f, 80f)
        val exercise = base.copy(
            exerciseExample = base.exerciseExample.copy(name = "Barbell squat"),
            iterations = List(5) { index ->
                stubPendingIteration().copy(
                    externalWeight = weights.getOrNull(index)?.let { VolumeFormatState.of(it) }
                        ?: VolumeFormatState.Empty(),
                )
            }.toPersistentList(),
        )
        ExerciseCardRow(
            value = exercise,
            onClick = {},
        )
    }
}

@AppPreview
@Composable
private fun ExerciseCardRowCompletedPreview() {
    PreviewContainer {
        val base = stubExercise()
        val weights = listOf<Float>(80f, 100f, 120f, 140f)
        val exercise = base.copy(
            exerciseExample = base.exerciseExample.copy(name = "Deadlift"),
            iterations = List(4) { index ->
                stubPendingIteration().copy(
                    externalWeight = weights.getOrNull(index)?.let { VolumeFormatState.of(it) }
                        ?: VolumeFormatState.Empty(),
                )
            }.toPersistentList(),
        )
        ExerciseCardRow(
            value = exercise,
            onClick = {},
        )
    }
}

@AppPreview
@Composable
private fun ExerciseCardRowEmptyPreview() {
    PreviewContainer {
        val base = stubExercise()
        val weights = listOf<Float>()
        val exercise = base.copy(
            exerciseExample = base.exerciseExample.copy(name = "Pull-ups"),
            iterations = List(0) { index ->
                stubPendingIteration().copy(
                    externalWeight = weights.getOrNull(index)?.let { VolumeFormatState.of(it) }
                        ?: VolumeFormatState.Empty(),
                )
            }.toPersistentList(),
        )
        ExerciseCardRow(
            value = exercise,
            onClick = {},
        )
    }
}

@AppPreview
@Composable
private fun ExerciseCardRowLongTitlePreview() {
    PreviewContainer {
        val base = stubExercise()
        val weights = listOf<Float>(22.5f)
        val exercise = base.copy(
            exerciseExample = base.exerciseExample.copy(name = "Жим гантелей лёжа на наклонной скамье нейтральным хватом"),
            iterations = List(12) { index ->
                stubPendingIteration().copy(
                    externalWeight = weights.getOrNull(index)?.let { VolumeFormatState.of(it) }
                        ?: VolumeFormatState.Empty(),
                )
            }.toPersistentList(),
        )
        ExerciseCardRow(
            value = exercise,
            onClick = {},
        )
    }
}
