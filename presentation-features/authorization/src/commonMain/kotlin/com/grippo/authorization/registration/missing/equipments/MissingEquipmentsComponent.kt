package com.grippo.authorization.registration.missing.equipments

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform

internal class MissingEquipmentsComponent(
    componentContext: ComponentContext,
    private val toCompleted: (missingEquipmentIds: List<String>) -> Unit,
    private val onBack: () -> Unit,
) : BaseComponent<MissingEquipmentsDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        MissingEquipmentsViewModel(equipmentFeature = getKoin().get())
    }

    private val backCallback = BackCallback(onBack = viewModel::back)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: MissingEquipmentsDirection) {
        when (direction) {
            is MissingEquipmentsDirection.Completed -> toCompleted.invoke(direction.missingEquipmentIds)
            MissingEquipmentsDirection.Back -> onBack.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        MissingEquipmentsScreen(state.value, loaders.value, viewModel)
    }
}