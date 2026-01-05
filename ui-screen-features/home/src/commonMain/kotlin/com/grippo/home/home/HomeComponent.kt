package com.grippo.home.home

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform

internal class HomeComponent(
    componentContext: ComponentContext,
    private val back: () -> Unit,
    private val toExcludedMuscles: () -> Unit,
    private val toMissingEquipment: () -> Unit,
    private val toWeightHistory: () -> Unit,
    private val toExperience: () -> Unit,
    private val toDebug: () -> Unit,
    private val toAddTraining: () -> Unit,
    private val toTrainings: () -> Unit,
) : BaseComponent<HomeDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        HomeViewModel(
            trainingFeature = getKoin().get(),
            dialogController = getKoin().get(),
            muscleLoadingUseCase = getKoin().get(),
            exerciseSpotlightUseCase = getKoin().get(),
            trainingStreakUseCase = getKoin().get(),
            performanceTrendUseCase = getKoin().get(),
            trainingDigestUseCase = getKoin().get(),
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: HomeDirection) {
        when (direction) {
            HomeDirection.Back -> back.invoke()
            HomeDirection.AddTraining -> toAddTraining.invoke()
            HomeDirection.Debug -> toDebug.invoke()
            HomeDirection.ExcludedMuscles -> toExcludedMuscles.invoke()
            HomeDirection.MissingEquipment -> toMissingEquipment.invoke()
            HomeDirection.WeightHistory -> toWeightHistory.invoke()
            HomeDirection.Experience -> toExperience.invoke()
            HomeDirection.Trainings -> toTrainings.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        HomeScreen(state.value, loaders.value, viewModel)
    }
}
