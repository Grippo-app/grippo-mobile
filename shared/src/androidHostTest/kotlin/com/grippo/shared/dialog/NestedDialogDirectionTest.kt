package com.grippo.shared.dialog

import com.grippo.core.state.examples.stubExerciseExample
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
    fun bodyWeightIntentCarriesOnlyFeatureDataAndAcceptsTheSelectedValue() = runBlocking {
        val initial = stubIteration().copy(bodyWeight = WeightFormatState.of(70f))
        val viewModel = IterationPickerViewModel(
            initial = initial,
            example = stubExerciseExample(),
            suggestions = emptyList(),
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
