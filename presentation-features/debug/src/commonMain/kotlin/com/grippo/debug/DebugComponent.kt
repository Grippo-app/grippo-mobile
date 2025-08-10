package com.grippo.debug

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.platform.collectAsStateMultiplatform

public class DebugComponent(
    componentContext: ComponentContext,
    private val back: () -> Unit
) : BaseComponent<DebugDirection>(componentContext) {

    override val viewModel: DebugViewModel = componentContext.retainedInstance {
        DebugViewModel()
    }

    private val backCallback = BackCallback(onBack = viewModel::back)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: DebugDirection) {
        when (direction) {
            DebugDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        DebugScreen(state.value, loaders.value, viewModel)
    }
}