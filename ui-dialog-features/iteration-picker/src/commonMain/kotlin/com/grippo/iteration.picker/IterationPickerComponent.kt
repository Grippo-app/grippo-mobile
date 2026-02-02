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
    private val example: ExerciseExampleState,
    private val focus: IterationFocusState,
    private val number: Int,
    private val initial: IterationState,
    private val suggestions: List<IterationState>,
    private val onResult: (value: IterationState) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<IterationPickerDirection>(componentContext) {

    override val viewModel: IterationPickerViewModel = componentContext.retainedInstance {
        IterationPickerViewModel(
            initial = initial,
            example = example,
            number = number,
            suggestions = suggestions,
            focus = focus,
            dialogController = getKoin().get(),
            weightHistoryFeature = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: IterationPickerDirection) {
        when (direction) {
            is IterationPickerDirection.BackWithResult -> onResult.invoke(direction.value)
            IterationPickerDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        IterationPickerScreen(state.value, loaders.value, viewModel)
    }
}