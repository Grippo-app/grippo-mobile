package com.grippo.iteration.picker

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.core.state.trainings.IterationFocusState
import com.grippo.core.state.trainings.stubExercises
import com.grippo.core.state.trainings.stubIteration
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun IterationPickerScreen(
    state: IterationPickerState,
    loaders: ImmutableSet<IterationPickerLoader>,
    contract: IterationPickerContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        IterationPickerScreen(
            state = IterationPickerState(
                value = stubIteration(),
                number = 2,
                suggestions = stubExercises().random().iterations,
                focus = IterationFocusState.UNIDENTIFIED,
                example = stubExerciseExample()
            ),
            loaders = persistentSetOf(),
            contract = IterationPickerContract.Empty
        )
    }
}
