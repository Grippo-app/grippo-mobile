package com.grippo.design.components.training.internal

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.stubExercise
import com.grippo.core.state.trainings.stubPendingIteration
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.exercise_plan_no_sets
import com.grippo.design.resources.provider.exercise_plan_sets_repetitions
import com.grippo.design.resources.provider.exercise_sets_planned
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ExercisePlanSummary(
    modifier: Modifier = Modifier,
    value: ExerciseState,
) {
    val setCount = value.iterations.size
    val repetitions = value.iterations.map { it.repetitions.value }
    val summary = when {
        setCount == 0 -> AppTokens.strings.res(Res.string.exercise_plan_no_sets)
        repetitions.all { it == null } -> AppTokens.strings.res(Res.plurals.exercise_sets_planned, setCount, setCount)
        else -> {
            val first = repetitions.first()
            val repetitionsText = if (repetitions.all { it == first }) {
                first.toString()
            } else {
                repetitions.joinToString(separator = " / ") { it?.toString() ?: "—" }
            }

            AppTokens.strings.res(Res.plurals.exercise_plan_sets_repetitions, setCount, setCount, repetitionsText)
        }
    }

    Text(
        modifier = modifier,
        text = summary,
        style = AppTokens.typography.b12Med(),
        color = AppTokens.colors.text.secondary,
    )
}

@AppPreview
@Composable
private fun ExercisePlanSummaryEmptyPreview() {
    PreviewContainer {
        ExercisePlanSummary(value = planSummaryPreviewExercise(repetitions = emptyList()))
    }
}

@AppPreview
@Composable
private fun ExercisePlanSummaryUniformPreview() {
    PreviewContainer {
        ExercisePlanSummary(value = planSummaryPreviewExercise(repetitions = listOf(10, 10, 10)))
    }
}

@AppPreview
@Composable
private fun ExercisePlanSummaryVariablePreview() {
    PreviewContainer {
        ExercisePlanSummary(value = planSummaryPreviewExercise(repetitions = listOf(12, 10, 8)))
    }
}

@AppPreview
@Composable
private fun ExercisePlanSummaryMissingPreview() {
    PreviewContainer {
        ExercisePlanSummary(value = planSummaryPreviewExercise(repetitions = listOf(null, null)))
        ExercisePlanSummary(value = planSummaryPreviewExercise(repetitions = listOf(12, null, 8)))
    }
}

private fun planSummaryPreviewExercise(repetitions: List<Int?>): ExerciseState {
    val iterations = repetitions.mapIndexed { index, value ->
        stubPendingIteration().copy(
            id = "planned-set-$index",
            repetitions = value?.let { RepetitionsFormatState.of(it) } ?: RepetitionsFormatState.Empty(),
        )
    }.toPersistentList()

    return stubExercise().copy(id = "planned-exercise", iterations = iterations)
}
