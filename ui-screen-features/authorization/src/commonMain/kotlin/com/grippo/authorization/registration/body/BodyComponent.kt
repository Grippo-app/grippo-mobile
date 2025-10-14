package com.grippo.authorization.registration.body

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform

internal class BodyComponent(
    componentContext: ComponentContext,
    private val toExperience: (weight: Float, height: Int) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<BodyDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        BodyViewModel(dialogController = getKoin().get())
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: BodyDirection) {
        when (direction) {
            is BodyDirection.Experience -> toExperience.invoke(direction.weight, direction.height)
            BodyDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        BodyScreen(state.value, loaders.value, viewModel)
    }
}