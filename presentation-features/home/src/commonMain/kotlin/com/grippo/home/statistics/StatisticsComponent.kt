package com.grippo.home.statistics

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform

internal class StatisticsComponent(
    componentContext: ComponentContext,
) : BaseComponent<StatisticsDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        StatisticsViewModel()
    }

    override suspend fun eventListener(direction: StatisticsDirection) {
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        StatisticsScreen(state.value, loaders.value, viewModel)
    }
}