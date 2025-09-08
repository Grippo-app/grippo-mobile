package com.grippo.drart.training

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.platform.collectAsStateMultiplatform

public class DraftTrainingComponent(
    private val onResult: () -> Unit,
    private val back: () -> Unit,
    componentContext: ComponentContext,
) : BaseComponent<DraftTrainingDirection>(componentContext) {

    override val viewModel: DraftTrainingViewModel = componentContext.retainedInstance {
        DraftTrainingViewModel(
            trainingFeature = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: DraftTrainingDirection) {
        when (direction) {
            DraftTrainingDirection.Continue -> onResult.invoke()
            DraftTrainingDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        DraftTrainingScreen(state.value, loaders.value, viewModel)
    }
}
