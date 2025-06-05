package com.grippo.home.profile

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform

internal class HomeProfileComponent(
    componentContext: ComponentContext,
    private val toExcludedMuscles: () -> Unit,
    private val toMissingEquipment: () -> Unit,
    private val toWeightHistory: () -> Unit,
    private val toExerciseLibrary: () -> Unit,
) : BaseComponent<HomeProfileDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        HomeProfileViewModel(
            userFeature = getKoin().get(),
            authorizationFeature = getKoin().get()
        )
    }

    override suspend fun eventListener(direction: HomeProfileDirection) {
        when (direction) {
            HomeProfileDirection.ExcludedMuscles -> toExcludedMuscles.invoke()
            HomeProfileDirection.ExerciseLibrary -> toExerciseLibrary.invoke()
            HomeProfileDirection.MissingEquipment -> toMissingEquipment.invoke()
            HomeProfileDirection.WeightHistory -> toWeightHistory.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        HomeProfileScreen(state.value, loaders.value, viewModel)
    }
}