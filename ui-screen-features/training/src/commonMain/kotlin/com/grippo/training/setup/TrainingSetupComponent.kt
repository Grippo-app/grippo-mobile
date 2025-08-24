package com.grippo.training.setup

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.platform.collectAsStateMultiplatform

internal class TrainingSetupComponent(
    componentContext: ComponentContext,
    private val toRecording: () -> Unit,
    private val back: () -> Unit,
) : BaseComponent<TrainingSetupDirection>(componentContext) {

    override val viewModel: TrainingSetupViewModel = componentContext.retainedInstance {
        TrainingSetupViewModel(
            muscleFeature = getKoin().get(),
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: TrainingSetupDirection) {
        when (direction) {
            is TrainingSetupDirection.ToRecording -> toRecording.invoke()
            TrainingSetupDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        TrainingSetupScreen(state.value, loaders.value, viewModel)
    }
}