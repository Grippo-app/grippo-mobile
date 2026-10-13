package com.grippo.profile.equipments

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.models.PersistenceKey
import com.grippo.core.foundation.platform.collectAsStateMultiplatform

internal class ProfileEquipmentsComponent(
    componentContext: ComponentContext,
    private val back: () -> Unit
) : BaseComponent<ProfileEquipmentsDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        ProfileEquipmentsViewModel(
            analytics = getKoin().get(),
            initialState = restoreState(DRAFT) ?: ProfileEquipmentsState(),
            equipmentFeature = getKoin().get(),
            excludedEquipmentsFeature = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        saveState(DRAFT) { viewModel.state.value }
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: ProfileEquipmentsDirection) {
        when (direction) {
            ProfileEquipmentsDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ProfileEquipmentsScreen(state.value, loaders.value, viewModel)
    }

    private companion object {
        val DRAFT = PersistenceKey("ProfileEquipmentsDraft", ProfileEquipmentsState.serializer())
    }
}
