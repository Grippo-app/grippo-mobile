package com.grippo.height.picker

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.formatters.HeightFormatState

public class HeightPickerComponent(
    componentContext: ComponentContext,
    private val initial: HeightFormatState,
    private val onResult: (value: HeightFormatState) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<HeightPickerDirection>(componentContext) {

    override val viewModel: HeightPickerViewModel = componentContext.retainedInstance {
        HeightPickerViewModel(initial)
    }

    private val backCallback = BackCallback(onBack = viewModel::onDismiss)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: HeightPickerDirection) {
        when (direction) {
            is HeightPickerDirection.BackWithResult -> onResult.invoke(direction.value)
            HeightPickerDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        HeightPickerScreen(state.value, loaders.value, viewModel)
    }
}