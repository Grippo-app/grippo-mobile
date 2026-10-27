package com.grippo.design.components.training.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.examples.WeightTypeEnumState
import com.grippo.core.state.formatters.MultiplierFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.stubExercise
import com.grippo.core.state.trainings.stubPendingIteration
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.exercise_max_weight
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ExerciseSummary(
    modifier: Modifier = Modifier,
    value: ExerciseState,
) {
    val completedCount = value.iterations.count { !it.isPending }
    val totalCount = value.iterations.size

    val isCompleted = totalCount > 0 && completedCount == totalCount
    val setsColor = if (isCompleted) AppTokens.colors.semantic.success else AppTokens.colors.text.secondary

    val completedLoads = value.iterations
        .filterNot { it.isPending }
        .map { iteration ->
            if (value.exerciseExample.weightType == WeightTypeEnumState.BODY_WEIGHT &&
                (iteration.bodyWeight.value == null || iteration.bodyMultiplier.value == null)
            ) {
                null
            } else {
                iteration.recordedVolume()?.takeIf { it.isFinite() && it >= 0f }
            }
        }
    val maximum = if (value.total.volume is VolumeFormatState.Invalid || completedLoads.any { it == null }) {
        null
    } else {
        completedLoads.filterNotNull().maxOrNull()
    }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Text(
            text = ExerciseState.setsProgress(completed = completedCount, total = totalCount),
            style = AppTokens.typography.b13Med(),
            color = setsColor,
        )
        if (maximum != null) {
            val maximumWeight = VolumeFormatState.shortTotal(maximum)
            Text(
                text = "·",
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.tertiary,
            )
            Text(
                text = AppTokens.strings.res(Res.string.exercise_max_weight, maximumWeight),
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary,
            )
        }
    }
}

@AppPreview
@Composable
private fun ExerciseSummaryUnknownBodyLoadPreview() {
    val unknownBodyLoad = stubPendingIteration().copy(
        bodyWeight = WeightFormatState.of(80f),
        extraWeight = VolumeFormatState.of(125f),
        repetitions = RepetitionsFormatState.of(8),
    )
    val knownLoad = stubPendingIteration().copy(
        bodyWeight = WeightFormatState.of(80f),
        bodyMultiplier = MultiplierFormatState.of(1f),
        repetitions = RepetitionsFormatState.of(8),
    )
    PreviewContainer {
        ExerciseSummary(
            value = stubExercise().copy(iterations = persistentListOf(unknownBodyLoad, knownLoad)),
        )
    }
}

@AppPreview
@Composable
private fun ExerciseSummaryMissingBodyWeightPreview() {
    val unknownBodyLoad = stubPendingIteration().copy(
        extraWeight = VolumeFormatState.of(20f),
        bodyMultiplier = MultiplierFormatState.of(1f),
        repetitions = RepetitionsFormatState.of(8),
    )
    val knownLoad = stubPendingIteration().copy(
        bodyWeight = WeightFormatState.of(80f),
        bodyMultiplier = MultiplierFormatState.of(1f),
        repetitions = RepetitionsFormatState.of(8),
    )
    PreviewContainer {
        ExerciseSummary(
            value = stubExercise().copy(iterations = persistentListOf(unknownBodyLoad, knownLoad)),
        )
    }
}

@AppPreview
@Composable
private fun ExerciseSummaryZeroLoadPreview() {
    PreviewContainer {
        val completedIteration = stubPendingIteration().copy(
            bodyWeight = WeightFormatState.of(80f),
            bodyMultiplier = MultiplierFormatState.of(1f),
            assistWeight = VolumeFormatState.of(80f),
            repetitions = RepetitionsFormatState.of(10),
        )
        ExerciseSummary(
            value = stubExercise().copy(iterations = persistentListOf(completedIteration)),
        )
    }
}

@AppPreview
@Composable
private fun ExerciseSummaryPresetPreview() {
    PreviewContainer {
        val base = stubExercise()
        val weights = listOf<Float>()
        val exercise = base.copy(
            exerciseExample = base.exerciseExample.copy(
                name = "Bench press",
                weightType = WeightTypeEnumState.FREE,
            ),
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
            exerciseExample = base.exerciseExample.copy(
                name = "Barbell squat",
                weightType = WeightTypeEnumState.FREE,
            ),
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
            exerciseExample = base.exerciseExample.copy(
                name = "Deadlift",
                weightType = WeightTypeEnumState.FREE,
            ),
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
            exerciseExample = base.exerciseExample.copy(
                name = "Жим гантелей лёжа на наклонной скамье нейтральным хватом",
                weightType = WeightTypeEnumState.FREE,
            ),
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
