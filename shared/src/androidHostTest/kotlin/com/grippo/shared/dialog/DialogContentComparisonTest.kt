package com.grippo.shared.dialog

import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.trainings.IterationFocusState
import com.grippo.core.state.trainings.stubIteration
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.set_value
import com.grippo.design.resources.provider.weight_picker_title
import com.grippo.dialog.api.DialogConfig
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.collections.immutable.persistentListOf

class DialogContentComparisonTest {
    @Test
    fun callbacksDoNotChangeContentIdentity() {
        val first = DialogConfig.WeightPicker(UiText.Str("Weight"), 70f, onResult = {})
        val second = first.copy(onResult = { error("Comparison must not invoke callbacks") })
        assertTrue(first.hasSameContentAs(second))
        assertTrue(second.hasSameContentAs(first))
    }

    @Test
    fun routeTypeTitleAndInitialValueArePartOfContentIdentity() {
        val weight = DialogConfig.WeightPicker(UiText.Str("Value"), 70f)
        assertFalse(weight.hasSameContentAs(DialogConfig.HeightPicker(weight.title, 70)))
        assertFalse(weight.hasSameContentAs(weight.copy(title = UiText.Str("Body weight"))))
        assertFalse(weight.hasSameContentAs(weight.copy(initial = 75f)))
    }

    @Test
    fun actionModeComparesItsTitleAndIgnoresItsCallback() {
        val action = DialogConfig.ExerciseExample.Mode.Action(UiText.Str("Change"), onClick = {})
        val example = DialogConfig.ExerciseExample(UiText.Str("Exercise"), "example", action)
        assertTrue(example.hasSameContentAs(example.copy(mode = action.copy(onClick = {}))))
        assertFalse(example.hasSameContentAs(example.copy(mode = action.copy(title = UiText.Str("Choose")))))
        assertFalse(example.hasSameContentAs(example.copy(mode = DialogConfig.ExerciseExample.Mode.Default)))
        assertFalse(example.hasSameContentAs(example.copy(id = "another-example")))
    }

    @Test
    fun resourceTitlesCompareNestedArgumentsAndTheirTypesDirectly() {
        val title = UiText.Res(
            Res.string.set_value,
            persistentListOf(UiText.Res(Res.string.weight_picker_title), 3),
        )
        val config = DialogConfig.WeightPicker(title, 70f)
        assertTrue(config.hasSameContentAs(config.copy(title = title.copy())))
        assertFalse(config.hasSameContentAs(config.copy(
            title = title.copy(formatArgs = persistentListOf(UiText.Res(Res.string.weight_picker_title), 3L)),
        )))
        assertFalse(config.hasSameContentAs(config.copy(
            title = title.copy(formatArgs = persistentListOf(UiText.Str("Weight"), 3)),
        )))
    }

    @Test
    fun iterationContentIncludesSuggestionsFocusAndNumber() {
        val initial = stubIteration()
        val config = DialogConfig.Iteration(
            title = UiText.Str("Set"), initial = initial, number = 1,
            example = stubExerciseExample(), focus = IterationFocusState.VOLUME,
            suggestions = emptyList(),
        )
        assertTrue(config.hasSameContentAs(config.copy(onResult = {})))
        assertFalse(config.hasSameContentAs(config.copy(number = 2)))
        assertFalse(config.hasSameContentAs(config.copy(focus = IterationFocusState.REPETITIONS)))
        assertFalse(config.hasSameContentAs(config.copy(suggestions = listOf(initial))))
    }
}
