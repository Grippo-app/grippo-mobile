package com.grippo.profile.settings

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform

internal class ProfileSettingsComponent(
    componentContext: ComponentContext,
    private val back: () -> Unit,
) : BaseComponent<ProfileSettingsDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        ProfileSettingsViewModel(
            userFeature = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: ProfileSettingsDirection) {
        when (direction) {
            ProfileSettingsDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ProfileSettingsScreen(state.value, loaders.value, viewModel)
    }
}
