package com.grippo.weight.picker

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform

public class WeightPickerComponent(
    componentContext: ComponentContext,
    private val initial: Float,
    private val onResult: (value: Float) -> Unit,
) : BaseComponent<WeightPickerDirection>(componentContext) {

    override val viewModel: WeightPickerViewModel = componentContext.retainedInstance {
        WeightPickerViewModel(initial)
    }

    override suspend fun eventListener(direction: WeightPickerDirection) {
        when (direction) {
            is WeightPickerDirection.DismissWithResult -> onResult.invoke(direction.value)
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        WeightPickerScreen(state.value, loaders.value, viewModel)
    }
}