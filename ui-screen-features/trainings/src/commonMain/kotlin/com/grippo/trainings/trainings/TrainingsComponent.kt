package com.grippo.trainings.trainings

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.stage.TrainingSeed

internal class TrainingsComponent(
    componentContext: ComponentContext,
    private val back: () -> Unit,
    private val toEditTraining: (id: String) -> Unit,
    private val toStartTraining: (seed: TrainingSeed) -> Unit,
) : BaseComponent<TrainingsDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        TrainingsViewModel(
            trainingFeature = getKoin().get(),
            dialogController = getKoin().get(),
            stringProvider = getKoin().get(),
            trainingDigestUseCase = getKoin().get(),
            trainingTimelineUseCase = getKoin().get(),
            deleteTrainingUseCase = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: TrainingsDirection) {
        when (direction) {
            TrainingsDirection.Back -> back.invoke()
            is TrainingsDirection.EditTraining -> toEditTraining.invoke(direction.id)
            is TrainingsDirection.StartTraining -> toStartTraining.invoke(direction.seed)
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        TrainingsScreen(state.value, loaders.value, viewModel)
    }
}
