package com.grippo.profile.menu

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.menu.ProfileMenu
import com.grippo.core.state.menu.SettingsMenu

internal class ProfileMenuComponent(
    componentContext: ComponentContext,
    private val onProfileMenuClick: (ProfileMenu) -> Unit,
    private val onSettingsMenuClick: (SettingsMenu) -> Unit,
) : BaseComponent<ProfileMenuDirection>(componentContext) {

    override val viewModel: ProfileMenuViewModel = componentContext.retainedInstance {
        ProfileMenuViewModel(
            userFeature = getKoin().get()
        )
    }

    override suspend fun eventListener(direction: ProfileMenuDirection) {
        when (direction) {
            is ProfileMenuDirection.ProfileMenuSelected -> onProfileMenuClick.invoke(direction.item)
            is ProfileMenuDirection.SettingsMenuSelected -> onSettingsMenuClick.invoke(direction.item)
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ProfileMenuScreen(state.value, loaders.value, viewModel)
    }
}
