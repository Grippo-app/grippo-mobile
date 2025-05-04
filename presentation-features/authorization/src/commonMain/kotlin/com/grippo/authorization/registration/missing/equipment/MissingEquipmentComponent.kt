package com.grippo.authorization.registration.missing.equipment

import com.arkivanov.decompose.ComponentContext
import androidx.compose.runtime.Composable
import com.grippo.core.collectAsStateMultiplatform
import com.grippo.core.BaseComponent
import com.arkivanov.essenty.instancekeeper.retainedInstance

internal class MissingEquipmentComponent(
    componentContext: ComponentContext,
) : BaseComponent<MissingEquipmentDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        MissingEquipmentViewModel()
    }

    override suspend fun eventListener(rout: MissingEquipmentDirection) {
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        MissingEquipmentScreen(state.value, loaders.value, viewModel)
    }
}