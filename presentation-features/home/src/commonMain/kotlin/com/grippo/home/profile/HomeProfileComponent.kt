package com.grippo.home.profile

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform

internal class HomeProfileComponent(
    componentContext: ComponentContext,
    private val toExcludedMuscles: () -> Unit,
    private val toMissingEquipment: () -> Unit,
    private val toWeightHistory: () -> Unit,
    private val toExerciseLibrary: () -> Unit,
    private val toDebug: () -> Unit,
    private val toWorkout: () -> Unit,
    private val toSystemSettings: () -> Unit,
    private val back: () -> Unit,
) : BaseComponent<HomeProfileDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        HomeProfileViewModel(
            userFeature = getKoin().get(),
            authorizationFeature = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::back)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: HomeProfileDirection) {
        when (direction) {
            HomeProfileDirection.ExcludedMuscles -> toExcludedMuscles.invoke()
            HomeProfileDirection.ExerciseLibrary -> toExerciseLibrary.invoke()
            HomeProfileDirection.MissingEquipment -> toMissingEquipment.invoke()
            HomeProfileDirection.WeightHistory -> toWeightHistory.invoke()
            HomeProfileDirection.Back -> back.invoke()
            HomeProfileDirection.Debug -> toDebug.invoke()
            HomeProfileDirection.Workout -> toWorkout.invoke()
            HomeProfileDirection.SystemSettings -> toSystemSettings.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        HomeProfileScreen(state.value, loaders.value, viewModel)
    }
}