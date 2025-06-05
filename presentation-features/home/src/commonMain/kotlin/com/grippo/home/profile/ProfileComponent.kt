package com.grippo.home.profile

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform

internal class ProfileComponent(
    componentContext: ComponentContext,
    private val toExcludedMuscles: () -> Unit,
    private val toMissingEquipment: () -> Unit,
    private val toWeightHistory: () -> Unit,
    private val toExerciseLibrary: () -> Unit,
) : BaseComponent<ProfileDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        ProfileViewModel(
            userFeature = getKoin().get(),
            authorizationFeature = getKoin().get()
        )
    }

    override suspend fun eventListener(direction: ProfileDirection) {
        when (direction) {
            ProfileDirection.ExcludedMuscles -> toExcludedMuscles.invoke()
            ProfileDirection.ExerciseLibrary -> toExerciseLibrary.invoke()
            ProfileDirection.MissingEquipment -> toMissingEquipment.invoke()
            ProfileDirection.WeightHistory -> toWeightHistory.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ProfileScreen(state.value, loaders.value, viewModel)
    }
}