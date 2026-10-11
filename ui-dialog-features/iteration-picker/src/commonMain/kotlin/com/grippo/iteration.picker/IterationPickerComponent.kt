package com.grippo.iteration.picker

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.trainings.IterationFocusState
import com.grippo.core.state.trainings.IterationState

public class IterationPickerComponent(
    componentContext: ComponentContext,
    private val onPickBodyWeight: (initial: Float?, onResult: (Float) -> Unit) -> Unit,
    private val example: ExerciseExampleState,
    private val focus: IterationFocusState,
    private val number: Int,
    private val initial: IterationState,
    private val suggestions: List<IterationState>,
    private val onResult: (value: IterationState) -> Unit,
    private val back: () -> Unit,
    previousIteration: IterationState?,
) : BaseComponent<IterationPickerDirection>(componentContext) {

    override val viewModel: IterationPickerViewModel = componentContext.retainedInstance {
        IterationPickerViewModel(
            initial = initial,
            example = example,
            number = number,
            iterationSuggestions = suggestions,
            focus = focus,
            previousIteration = previousIteration,
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: IterationPickerDirection) {
        when (direction) {
            is IterationPickerDirection.PickBodyWeight ->
                onPickBodyWeight.invoke(direction.initial, viewModel::onBodyWeightSelected)
            is IterationPickerDirection.BackWithResult -> onResult.invoke(direction.value)
            IterationPickerDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        IterationPickerScreen(state.value, viewModel)
    }
}
