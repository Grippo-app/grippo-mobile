package com.grippo.authorization.registration.name

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform

internal class NameComponent(
    componentContext: ComponentContext,
    private val toBody: (name: String) -> Unit,
    private val onBack: () -> Unit,
) : BaseComponent<NameDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        NameViewModel()
    }

    private val backCallback = BackCallback(onBack = viewModel::back)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: NameDirection) {
        when (direction) {
            is NameDirection.Body -> toBody.invoke(direction.name)
            NameDirection.Back -> onBack.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        NameScreen(state.value, loaders.value, viewModel)
    }
}