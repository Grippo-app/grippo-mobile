package com.grippo.trainings.trainings

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.models.PersistenceKey
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import kotlinx.datetime.LocalDate

internal class TrainingsComponent(
    componentContext: ComponentContext,
    day: LocalDate?,
    private val back: () -> Unit,
    private val toEditTraining: (id: String) -> Unit,
) : BaseComponent<TrainingsDirection>(componentContext = componentContext) {

    override val viewModel: TrainingsViewModel = componentContext.retainedInstance {
        TrainingsViewModel(
            day = day,
            analytics = getKoin().get(),
            trainingFeature = getKoin().get(),
            dialogController = getKoin().get(),
            trainingDigestUseCase = getKoin().get(),
            deleteTrainingUseCase = getKoin().get(),
            trainingTimelineUseCase = getKoin().get(),
            initialState = restoreState(DRAFT),
        )
    }

    private val backCallback: BackCallback = BackCallback(onBack = viewModel::onBack)

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
        TrainingsScreen(
            state = state.value,
            loaders = loaders.value,
            contract = viewModel,
        )
    }

    private companion object {
        val DRAFT: PersistenceKey<TrainingsState> = PersistenceKey(
            key = "TrainingsDraft",
            serializer = TrainingsState.serializer(),
        )
    }
}
