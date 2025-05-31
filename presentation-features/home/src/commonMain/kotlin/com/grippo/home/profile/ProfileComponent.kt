package com.grippo.home.profile

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform

internal class ProfileComponent(
    componentContext: ComponentContext,
) : BaseComponent<ProfileDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        ProfileViewModel(
            userFeature = getKoin().get(),
            authorizationFeature = getKoin().get()
        )
    }

    override suspend fun eventListener(direction: ProfileDirection) {
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ProfileScreen(state.value, loaders.value, viewModel)
    }
}