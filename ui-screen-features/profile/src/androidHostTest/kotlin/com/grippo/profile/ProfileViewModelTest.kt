package com.grippo.profile

import com.grippo.core.state.menu.ProfileMenu
import com.grippo.core.state.menu.SettingsMenu
import com.grippo.screen.api.ProfileRouter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.BeforeTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileViewModelTest {
    @BeforeTest
    fun prepareDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun profileMenuItemsOpenTheirScreensInsideTheProfileFeature() = runBlocking {
        val viewModel = ProfileViewModel()
        val destinations = listOf(
            ProfileMenu.Body to ProfileRouter.Body,
            ProfileMenu.Equipment to ProfileRouter.Equipments,
            ProfileMenu.Muscles to ProfileRouter.Muscles,
            ProfileMenu.Experience to ProfileRouter.Experience,
            ProfileMenu.Goal to ProfileRouter.Goal,
        )
        for ((item, router) in destinations) {
            viewModel.onProfileMenuClick(item)
            assertEquals(ProfileDirection.Open(router), viewModel.navigator.first())
        }
        viewModel.onDestroy()
    }

    @Test
    fun settingsStayInProfileAndDebugIsDelegatedToTheHost() = runBlocking {
        val viewModel = ProfileViewModel()
        viewModel.onSettingsMenuClick(SettingsMenu.Settings)
        assertEquals(ProfileDirection.Open(ProfileRouter.Settings), viewModel.navigator.first())
        viewModel.onSettingsMenuClick(SettingsMenu.Social)
        assertEquals(ProfileDirection.Open(ProfileRouter.Social), viewModel.navigator.first())
        viewModel.onSettingsMenuClick(SettingsMenu.Debug)
        assertEquals(ProfileDirection.Debug, viewModel.navigator.first())
        viewModel.onDestroy()
    }

    @Test
    fun backIsHandledByTheProfileComponent() = runBlocking {
        val viewModel = ProfileViewModel()
        viewModel.onBack()
        assertEquals(ProfileDirection.Back, viewModel.navigator.first())
        viewModel.onDestroy()
    }
}
