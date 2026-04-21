package com.grippo.training.streak.details

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.toolkit.date.utils.DateRange

public class TrainingStreakDetailsComponent(
    componentContext: ComponentContext,
    range: DateRange,
    private val back: () -> Unit,
) : BaseComponent<TrainingStreakDetailsDirection>(componentContext) {

    override val viewModel: TrainingStreakDetailsViewModel = componentContext.retainedInstance {
        TrainingStreakDetailsViewModel(
            range = range,
            trainingFeature = getKoin().get(),
            trainingStreakUseCase = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: TrainingStreakDetailsDirection) {
        when (direction) {
            TrainingStreakDetailsDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        TrainingStreakDetailsScreen(state.value, loaders.value, viewModel)
    }
}
