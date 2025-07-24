package com.grippo.settings.system

import com.arkivanov.decompose.ComponentContext
import androidx.compose.runtime.Composable
import com.grippo.core.collectAsStateMultiplatform
import com.grippo.core.BaseComponent
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.arkivanov.essenty.backhandler.BackCallback

internal class SystemComponent(
    componentContext: ComponentContext,
    private val back: () -> Unit
) : BaseComponent<SystemDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        SystemViewModel()
    }

    private val backCallback = BackCallback(onBack = viewModel::back)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: SystemDirection) {
        when (direction) {
            SystemDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        SystemScreen(state.value, loaders.value, viewModel)
    }
}