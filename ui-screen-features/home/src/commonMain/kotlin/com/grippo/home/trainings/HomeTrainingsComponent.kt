package com.grippo.home.trainings

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform

internal class HomeTrainingsComponent(
    componentContext: ComponentContext,
    private val back: () -> Unit,
    private val toEditTraining: (id: String) -> Unit,
    private val toExcludedMuscles: () -> Unit,
    private val toMissingEquipment: () -> Unit,
    private val toWeightHistory: () -> Unit,
    private val toDebug: () -> Unit,
    private val toAddTraining: () -> Unit,
) : BaseComponent<HomeTrainingsDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        HomeTrainingsViewModel(
            trainingFeature = getKoin().get(),
            dialogController = getKoin().get(),
            stringProvider = getKoin().get(),
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: HomeTrainingsDirection) {
        when (direction) {
            HomeTrainingsDirection.Back -> back.invoke()
            is HomeTrainingsDirection.EditTraining -> toEditTraining.invoke(direction.id)
            HomeTrainingsDirection.AddTraining -> toAddTraining.invoke()
            HomeTrainingsDirection.Debug -> toDebug.invoke()
            HomeTrainingsDirection.ExcludedMuscles -> toExcludedMuscles.invoke()
            HomeTrainingsDirection.MissingEquipment -> toMissingEquipment.invoke()
            HomeTrainingsDirection.WeightHistory -> toWeightHistory.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        HomeTrainingsScreen(state.value, loaders.value, viewModel)
    }
}