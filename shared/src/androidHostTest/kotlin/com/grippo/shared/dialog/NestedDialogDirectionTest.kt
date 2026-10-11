package com.grippo.shared.dialog

import com.grippo.core.state.examples.ExerciseExampleComponentsState
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.core.state.formatters.MultiplierFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.trainings.IterationFocusState
import com.grippo.core.state.trainings.stubIteration
import com.grippo.iteration.picker.IterationPickerDirection
import com.grippo.iteration.picker.IterationPickerViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class NestedDialogDirectionTest {
    @Test
    fun previousSetAppliesEditableValuesWithoutReplacingCurrentBodyWeightOrIdentity() {
        val initial = stubIteration().copy(
            id = "current",
            bodyWeight = WeightFormatState.of(80f),
            bodyMultiplier = MultiplierFormatState.of(0.65f),
        )
        val previous = initial.copy(
            id = "previous",
            bodyWeight = WeightFormatState.of(70f),
            bodyMultiplier = MultiplierFormatState.of(0.5f),
            extraWeight = VolumeFormatState.of(20f),
            repetitions = RepetitionsFormatState.of(12),
        )
        val viewModel = IterationPickerViewModel(
            initial = initial,
            example = stubExerciseExample().copy(
                components = ExerciseExampleComponentsState.BodyAndExtra(true, 0.65f, false),
            ),
            iterationSuggestions = emptyList(),
            previousIteration = previous,
            number = 2,
            focus = IterationFocusState.VOLUME,
        )
        try {
            viewModel.onPreviousIterationClick()
            assertEquals(initial.copy(
                externalWeight = VolumeFormatState.Empty(),
                assistWeight = VolumeFormatState.Empty(),
                extraWeight = previous.extraWeight,
                repetitions = previous.repetitions,
            ), viewModel.state.value.value)
            assertEquals(previous, viewModel.state.value.previousIteration)
        } finally {
            viewModel.onDestroy()
        }
    }

    @Test
    fun bodyWeightIntentCarriesOnlyFeatureDataAndAcceptsTheSelectedValue() = runBlocking {
        val initial = stubIteration().copy(bodyWeight = WeightFormatState.of(70f))
        val viewModel = IterationPickerViewModel(
            initial = initial,
            example = stubExerciseExample(),
            iterationSuggestions = emptyList(),
            previousIteration = null,
            number = 1,
            focus = IterationFocusState.VOLUME,
        )
        try {
            viewModel.onWeightPickerClick()
            val direction = withTimeout(1_000) { viewModel.navigator.first() }
            val intent = assertIs<IterationPickerDirection.PickBodyWeight>(direction)
            assertEquals(70f, intent.initial)
            viewModel.onBodyWeightSelected(75f)
            assertEquals(initial.copy(bodyWeight = WeightFormatState.of(75f)), viewModel.state.value.value)
        } finally {
            viewModel.onDestroy()
        }
    }
}
