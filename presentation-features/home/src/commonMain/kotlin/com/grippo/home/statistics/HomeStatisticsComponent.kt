package com.grippo.home.statistics

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform

internal class HomeStatisticsComponent(
    componentContext: ComponentContext,
) : BaseComponent<HomeStatisticsDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        HomeStatisticsViewModel()
    }

    override suspend fun eventListener(direction: HomeStatisticsDirection) {
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        HomeStatisticsScreen(state.value, loaders.value, viewModel)
    }
}