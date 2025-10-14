package com.grippo.weight.picker

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.formatters.WeightFormatState

public class WeightPickerComponent(
    componentContext: ComponentContext,
    private val initial: WeightFormatState,
    private val onResult: (value: WeightFormatState) -> Unit,
    private val back: () -> Unit
) : BaseComponent<WeightPickerDirection>(componentContext) {

    override val viewModel: WeightPickerViewModel = componentContext.retainedInstance {
        WeightPickerViewModel(initial)
    }
    private val backCallback = BackCallback(onBack = viewModel::onDismiss)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: WeightPickerDirection) {
        when (direction) {
            is WeightPickerDirection.BackWithResult -> onResult.invoke(direction.value)
            WeightPickerDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        WeightPickerScreen(state.value, loaders.value, viewModel)
    }
}