package com.grippo.exercise.example.picker

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun ExerciseExamplePickerScreen(
    state: ExerciseExamplePickerState,
    contract: ExerciseExamplePickerContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

}

@AppPreview
@Composable
private fun ScreenPreviewDefault() {
    PreviewContainer {
        ExerciseExamplePickerScreen(
            state = ExerciseExamplePickerState(
                mode = ExerciseExamplePickerMode.Default(preselectedMuscleGroupId = null),
                exerciseExamples = persistentListOf(
                    stubExerciseExample(),
                    stubExerciseExample(),
                ),
            ),
            contract = ExerciseExamplePickerContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewReplace() {
    val targetExample = stubExerciseExample().value
    PreviewContainer {
        ExerciseExamplePickerScreen(
            state = ExerciseExamplePickerState(
                mode = ExerciseExamplePickerMode.SimilarTo(
                    targetExerciseExampleId = targetExample.id,
                    target = targetExample,
                ),
                exerciseExamples = persistentListOf(
                    stubExerciseExample(),
                    stubExerciseExample(),
                ),
                queries = Queries(filter = QueryFilter.Suggestions),
            ),
            contract = ExerciseExamplePickerContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewEmpty() {
    PreviewContainer {
        ExerciseExamplePickerScreen(
            state = ExerciseExamplePickerState(
                mode = ExerciseExamplePickerMode.Default(preselectedMuscleGroupId = null),
            ),
            contract = ExerciseExamplePickerContract.Empty,
        )
    }
}
