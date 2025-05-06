package com.grippo.height.picker

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform

public class HeightPickerComponent(
    componentContext: ComponentContext,
    private val onDismiss: () -> Unit,
    private val onResult: (value: Int) -> Unit,
) : BaseComponent<HeightPickerDirection>(componentContext) {

    override val viewModel: HeightPickerViewModel = componentContext.retainedInstance {
        HeightPickerViewModel()
    }

    override suspend fun eventListener(direction: HeightPickerDirection) {
        when (direction) {
            HeightPickerDirection.Dismiss -> onDismiss.invoke()
            is HeightPickerDirection.DismissWithResult -> onResult.invoke(direction.value)
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        HeightPickerScreen(state.value, loaders.value, viewModel)
    }
}