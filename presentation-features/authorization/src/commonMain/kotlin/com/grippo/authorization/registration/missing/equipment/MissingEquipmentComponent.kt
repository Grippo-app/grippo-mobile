package com.grippo.authorization.registration.missing.equipment

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform

internal class MissingEquipmentComponent(
    componentContext: ComponentContext,
    private val toCompleted: () -> Unit
) : BaseComponent<MissingEquipmentDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        MissingEquipmentViewModel()
    }

    override suspend fun eventListener(direction: MissingEquipmentDirection) {
        when (direction) {
            MissingEquipmentDirection.Completed -> toCompleted.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        MissingEquipmentScreen(state.value, loaders.value, viewModel)
    }
}