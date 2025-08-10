package com.grippo.home.trainings

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.platform.collectAsStateMultiplatform

internal class HomeTrainingsComponent(
    componentContext: ComponentContext,
    private val back: () -> Unit
) : BaseComponent<HomeTrainingsDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        HomeTrainingsViewModel(
            trainingFeature = getKoin().get(),
            dialogController = getKoin().get(),
            stringProvider = getKoin().get(),
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::back)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: HomeTrainingsDirection) {
        when (direction) {
            HomeTrainingsDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        HomeTrainingsScreen(state.value, loaders.value, viewModel)
    }
}