package com.grippo.home.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.activity
import com.grippo.design.resources.provider.debug
import com.grippo.design.resources.provider.icons.Flask
import com.grippo.design.resources.provider.icons.Gym
import com.grippo.design.resources.provider.icons.Settings
import com.grippo.design.resources.provider.icons.Waist
import com.grippo.design.resources.provider.profile_menu_excluded_muscles
import com.grippo.design.resources.provider.profile_menu_missing_equipment
import com.grippo.design.resources.provider.settings
import com.grippo.design.resources.provider.system
import com.grippo.state.profile.UserState

@Immutable
internal data class HomeProfileState(
    val user: UserState? = null
)

@Immutable
internal enum class HomeProfileActivityMenu {
    ExcludedMuscles,
    MissingEquipment;

    companion object {
        @Composable
        fun title(): String {
            return AppTokens.strings.res(Res.string.activity)
        }
    }

    @Composable
    fun text(): String {
        return when (this) {
            ExcludedMuscles -> AppTokens.strings.res(Res.string.profile_menu_excluded_muscles)
            MissingEquipment -> AppTokens.strings.res(Res.string.profile_menu_missing_equipment)
        }
    }

    @Composable
    fun icon(): ImageVector {
        return when (this) {
            ExcludedMuscles -> AppTokens.icons.Waist
            MissingEquipment -> AppTokens.icons.Gym
        }
    }
}

@Immutable
internal enum class HomeProfileSettingsMenu {
    System,
    Debug;

    companion object {
        @Composable
        fun title(): String {
            return AppTokens.strings.res(Res.string.settings)
        }
    }

    @Composable
    fun text(): String {
        return when (this) {
            System -> AppTokens.strings.res(Res.string.system)
            Debug -> AppTokens.strings.res(Res.string.debug)
        }
    }

    @Composable
    fun icon(): ImageVector {
        return when (this) {
            System -> AppTokens.icons.Settings
            Debug -> AppTokens.icons.Flask
        }
    }
}