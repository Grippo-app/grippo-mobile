package com.grippo.home.statistics

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.platform.collectAsStateMultiplatform

internal class HomeStatisticsComponent(
    componentContext: ComponentContext,
    private val back: () -> Unit
) : BaseComponent<HomeStatisticsDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        HomeStatisticsViewModel(
            dialogController = getKoin().get(),
            colorProvider = getKoin().get(),
            stringProvider = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: HomeStatisticsDirection) {
        when (direction) {
            HomeStatisticsDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        HomeStatisticsScreen(state.value, loaders.value, viewModel)
    }
}