package com.grippo.profile.equipments

import com.arkivanov.decompose.ComponentContext
import androidx.compose.runtime.Composable
import com.grippo.core.collectAsStateMultiplatform
import com.grippo.core.BaseComponent
import com.arkivanov.essenty.instancekeeper.retainedInstance

internal class ProfileEquipmentsComponent(
    componentContext: ComponentContext,
) : BaseComponent<ProfileEquipmentsDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        ProfileEquipmentsViewModel()
    }

    override suspend fun eventListener(direction: ProfileEquipmentsDirection) {
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ProfileEquipmentsScreen(state.value, loaders.value, viewModel)
    }
}