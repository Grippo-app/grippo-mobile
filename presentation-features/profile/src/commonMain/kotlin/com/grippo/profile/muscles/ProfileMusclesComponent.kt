package com.grippo.profile.muscles

import com.arkivanov.decompose.ComponentContext
import androidx.compose.runtime.Composable
import com.grippo.core.collectAsStateMultiplatform
import com.grippo.core.BaseComponent
import com.arkivanov.essenty.instancekeeper.retainedInstance

internal class ProfileMusclesComponent(
    componentContext: ComponentContext,
) : BaseComponent<ProfileMusclesDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        ProfileMusclesViewModel()
    }

    override suspend fun eventListener(direction: ProfileMusclesDirection) {
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ProfileMusclesScreen(state.value, loaders.value, viewModel)
    }
}