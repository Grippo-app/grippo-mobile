package com.grippo.training.goal.details

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.toolkit.date.utils.DateRange

public class TrainingGoalDetailsComponent(
    componentContext: ComponentContext,
    range: DateRange,
    private val back: () -> Unit,
) : BaseComponent<TrainingGoalDetailsDirection>(componentContext) {

    override val viewModel: TrainingGoalDetailsViewModel = componentContext.retainedInstance {
        TrainingGoalDetailsViewModel(
            range = range,
            trainingFeature = getKoin().get(),
            goalFollowingUseCase = getKoin().get(),
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: TrainingGoalDetailsDirection) {
        when (direction) {
            TrainingGoalDetailsDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        TrainingGoalDetailsScreen(state.value, loaders.value, viewModel)
    }
}
