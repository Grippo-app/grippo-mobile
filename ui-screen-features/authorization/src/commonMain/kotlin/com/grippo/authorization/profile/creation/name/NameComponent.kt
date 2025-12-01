package com.grippo.authorization.profile.creation.name

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform

internal class NameComponent(
    componentContext: ComponentContext,
    private val toBody: (name: String) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<NameDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        NameViewModel()
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: NameDirection) {
        when (direction) {
            is NameDirection.Body -> toBody.invoke(direction.name)
            NameDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        NameScreen(state.value, loaders.value, viewModel)
    }
}