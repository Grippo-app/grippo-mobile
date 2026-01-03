package com.grippo.profile.muscles

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform

internal class ProfileMusclesComponent(
    componentContext: ComponentContext,
    private val back: () -> Unit
) : BaseComponent<ProfileMusclesDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        ProfileMusclesViewModel(
            muscleFeature = getKoin().get(),
            excludedMusclesFeature = getKoin().get(),
            colorProvider = getKoin().get(),
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: ProfileMusclesDirection) {
        when (direction) {
            ProfileMusclesDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ProfileMusclesScreen(state.value, loaders.value, viewModel)
    }
}
