package com.grippo.exercise.examples.list

import androidx.compose.runtime.Composable
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.state.exercise.examples.stubExerciseExample
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ExerciseExampleListScreen(
    state: ExerciseExampleListState,
    loaders: ImmutableSet<ExerciseExampleListLoader>,
    contract: ExerciseExampleListContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        ExerciseExampleListScreen(
            state = ExerciseExampleListState(
                exerciseExamples = persistentListOf(stubExerciseExample(), stubExerciseExample())
            ),
            loaders = persistentSetOf(),
            contract = ExerciseExampleListContract.Empty
        )
    }
}
