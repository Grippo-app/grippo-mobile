package com.grippo.statistics

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.dialog.api.DialogConfig

public class StatisticsComponent(
    config: DialogConfig.Statistics,
    componentContext: ComponentContext,
    private val back: () -> Unit
) : BaseComponent<StatisticsDirection>(componentContext) {

    override val viewModel: StatisticsViewModel = componentContext.retainedInstance {
        StatisticsViewModel(
            config = config,
            exerciseExampleFeature = getKoin().get(),
            muscleFeature = getKoin().get(),
            colorProvider = getKoin().get(),
            stringProvider = getKoin().get(),
            trainingFeature = getKoin().get(),
            muscleLoadingUseCase = getKoin().get(),
            exerciseDistributionUseCase = getKoin().get(),
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: StatisticsDirection) {
        when (direction) {
            StatisticsDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        StatisticsScreen(state.value, loaders.value, viewModel)
    }
}
