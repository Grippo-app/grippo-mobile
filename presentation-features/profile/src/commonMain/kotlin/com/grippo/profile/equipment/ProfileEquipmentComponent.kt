package com.grippo.profile.equipment

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform

internal class ProfileEquipmentComponent(
    componentContext: ComponentContext,
    private val onBack: () -> Unit
) : BaseComponent<ProfileEquipmentDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        ProfileEquipmentViewModel()
    }

    override suspend fun eventListener(direction: ProfileEquipmentDirection) {
        when (direction) {
            ProfileEquipmentDirection.Back -> onBack.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ProfileEquipmentScreen(state.value, loaders.value, viewModel)
    }
}