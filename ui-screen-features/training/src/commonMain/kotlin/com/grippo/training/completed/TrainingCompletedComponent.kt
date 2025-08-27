package com.grippo.training.completed

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.platform.collectAsStateMultiplatform
import com.grippo.state.trainings.TrainingState

internal class TrainingCompletedComponent(
    componentContext: ComponentContext,
    training: TrainingState,
    private val back: () -> Unit,
) : BaseComponent<TrainingCompletedDirection>(componentContext) {

    override val viewModel: TrainingCompletedViewModel = componentContext.retainedInstance {
        TrainingCompletedViewModel(
            training = training,
            trainingFeature = getKoin().get(),
            dialogController = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: TrainingCompletedDirection) {
        when (direction) {
            TrainingCompletedDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        TrainingCompletedScreen(state.value, loaders.value, viewModel)
    }
}
