package com.grippo.profile.equipments

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform

internal class ProfileEquipmentsComponent(
    componentContext: ComponentContext,
    private val onBack: () -> Unit
) : BaseComponent<ProfileEquipmentsDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        ProfileEquipmentsViewModel()
    }

    override suspend fun eventListener(direction: ProfileEquipmentsDirection) {
        when (direction) {
            ProfileEquipmentsDirection.Back -> onBack.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ProfileEquipmentsScreen(state.value, loaders.value, viewModel)
    }
}