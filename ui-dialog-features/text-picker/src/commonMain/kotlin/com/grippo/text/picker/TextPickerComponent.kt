package com.grippo.text.picker

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.platform.collectAsStateMultiplatform
import com.grippo.state.text.TextWithId

public class TextPickerComponent(
    componentContext: ComponentContext,
    private val initial: TextWithId,
    private val available: List<TextWithId>,
    private val onResult: (value: TextWithId) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<TextPickerDirection>(componentContext) {

    override val viewModel: TextPickerViewModel = componentContext.retainedInstance {
        TextPickerViewModel(
            initial = initial,
            available = available,
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onDismiss)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: TextPickerDirection) {
        when (direction) {
            is TextPickerDirection.BackWithResult -> onResult.invoke(direction.value)
            TextPickerDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        TextPickerScreen(state.value, loaders.value, viewModel)
    }
}