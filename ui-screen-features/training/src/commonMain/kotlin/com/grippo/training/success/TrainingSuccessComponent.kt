package com.grippo.training.success

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.platform.collectAsStateMultiplatform

internal class TrainingSuccessComponent(
    componentContext: ComponentContext,
    private val back: () -> Unit,
) : BaseComponent<TrainingSuccessDirection>(componentContext) {

    override val viewModel: TrainingSuccessViewModel = componentContext.retainedInstance {
        TrainingSuccessViewModel()
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: TrainingSuccessDirection) {
        when (direction) {
            TrainingSuccessDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        TrainingSuccessScreen(state.value, loaders.value, viewModel)
    }
}
