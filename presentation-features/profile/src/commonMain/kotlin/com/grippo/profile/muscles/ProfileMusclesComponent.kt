package com.grippo.profile.muscles

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform

internal class ProfileMusclesComponent(
    componentContext: ComponentContext,
    private val onBack: () -> Unit
) : BaseComponent<ProfileMusclesDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        ProfileMusclesViewModel(
            muscleFeature = getKoin().get(),
            excludedMusclesFeature = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::back)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: ProfileMusclesDirection) {
        when (direction) {
            ProfileMusclesDirection.Back -> onBack.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ProfileMusclesScreen(state.value, loaders.value, viewModel)
    }
}