package com.grippo.trainings.trainings

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.models.PersistenceKey
import com.grippo.core.foundation.platform.collectAsStateMultiplatform

internal class TrainingsComponent(
    componentContext: ComponentContext,
    private val back: () -> Unit,
    private val toEditTraining: (id: String) -> Unit,
) : BaseComponent<TrainingsDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        TrainingsViewModel(
            analytics = getKoin().get(),
            trainingFeature = getKoin().get(),
            dialogController = getKoin().get(),
            trainingDigestUseCase = getKoin().get(),
            trainingTimelineUseCase = getKoin().get(),
            deleteTrainingUseCase = getKoin().get(),
            initialState = restoreState(DRAFT),
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        saveState(DRAFT) { viewModel.state.value }
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: TrainingsDirection) {
        when (direction) {
            TrainingsDirection.Back -> back.invoke()
            is TrainingsDirection.EditTraining -> toEditTraining.invoke(direction.id)
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        TrainingsScreen(state.value, loaders.value, viewModel)
    }

    private companion object {
        val DRAFT = PersistenceKey("TrainingsDraft", TrainingsState.serializer())
    }
}
