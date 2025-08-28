package com.grippo.iteration.picker

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.platform.collectAsStateMultiplatform
import com.grippo.state.trainings.IterationFocus
import com.grippo.state.trainings.IterationState

public class IterationPickerComponent(
    componentContext: ComponentContext,
    private val focus: IterationFocus,
    private val initial: IterationState,
    private val suggestions: List<IterationState>,
    private val onResult: (value: IterationState) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<IterationPickerDirection>(componentContext) {

    override val viewModel: IterationPickerViewModel = componentContext.retainedInstance {
        IterationPickerViewModel(
            initial = initial,
            suggestions = suggestions,
            focus = focus
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