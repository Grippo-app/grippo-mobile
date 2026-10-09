package com.grippo.design.components.training.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.stubExercise
import com.grippo.core.state.trainings.stubPendingIteration
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.exercise_max_weight
import com.grippo.design.resources.provider.exercise_sets_count
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ExerciseSummary(
    value: ExerciseState,
    modifier: Modifier = Modifier,
) {
    val largeFont = LocalDensity.current.fontScale > AppTokens.dp.screen.largeFontScaleThreshold

    val completedCount = value.iterations.count { !it.isPending }

    val totalCount = value.iterations.size

    val setsColor = when {
        totalCount > 0 && completedCount == totalCount -> AppTokens.colors.semantic.success
        else -> AppTokens.colors.text.secondary
    }

    val maximum = value.iterations
        .filterNot { it.isPending }
        .maxOfOrNull { it.volume().value ?: 0f }

    FlowRow(
        modifier = modifier,
        maxItemsInEachRow = if (largeFont) 1 else Int.MAX_VALUE,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Text(
            text = AppTokens.strings.res(Res.string.exercise_sets_count, completedCount, totalCount),
            style = AppTokens.typography.b13Med(),
            color = setsColor,
        )
        if (maximum != null) {
            val maximumWeight = when (maximum) {
                0f -> VolumeFormatState.Valid(display = "0", value = 0f)
                else -> VolumeFormatState.of(maximum)
            }
            if (!largeFont) {
                Text(
                    text = "·",
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.tertiary,
                )
            }
            Text(
                text = AppTokens.strings.res(Res.string.exercise_max_weight, maximumWeight.short()),
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary,
            )
        }
    }
}

@AppPreview
@Composable
private fun ExerciseSummaryPresetPreview() {
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
        ExerciseSummary(
            value = exercise,
        )
    }
}

@AppPreview
@Composable
private fun ExerciseSummaryInProgressPreview() {
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
        ExerciseSummary(
            value = exercise,
        )
    }
}

@AppPreview
@Composable
private fun ExerciseSummaryCompletedPreview() {
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
        ExerciseSummary(
            value = exercise,
        )
    }
}

@AppPreview
@Composable
private fun ExerciseSummaryEmptyPreview() {
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
        ExerciseSummary(
            value = exercise,
        )
    }
}

@AppPreview
@Composable
private fun ExerciseSummaryLongTitlePreview() {
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
        ExerciseSummary(
            value = exercise,
        )
    }
}
