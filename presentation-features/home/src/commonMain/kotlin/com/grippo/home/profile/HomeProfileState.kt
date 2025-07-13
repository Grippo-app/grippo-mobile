package com.grippo.home.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.Res
import com.grippo.design.resources.activity
import com.grippo.design.resources.debug
import com.grippo.design.resources.icons.Flask
import com.grippo.design.resources.icons.Gym
import com.grippo.design.resources.icons.Repository
import com.grippo.design.resources.icons.Settings
import com.grippo.design.resources.icons.Waist
import com.grippo.design.resources.profile_menu_excluded_muscles
import com.grippo.design.resources.profile_menu_exercise_library
import com.grippo.design.resources.profile_menu_missing_equipment
import com.grippo.design.resources.settings
import com.grippo.design.resources.system
import com.grippo.presentation.api.profile.models.UserState

@Immutable
internal data class HomeProfileState(
    val user: UserState? = null
)

@Immutable
internal enum class HomeProfileActivityMenu {
    ExcludedMuscles,
    MissingEquipment,
    ExerciseLibrary;

    companion object {
        @Composable
        fun title(): String {
            return AppTokens.strings.res(Res.string.activity)
        }
    }

    @Composable
    fun text(): String {
        return when (this) {
            ExerciseLibrary -> AppTokens.strings.res(Res.string.profile_menu_exercise_library)
            ExcludedMuscles -> AppTokens.strings.res(Res.string.profile_menu_excluded_muscles)
            MissingEquipment -> AppTokens.strings.res(Res.string.profile_menu_missing_equipment)
        }
    }

    @Composable
    fun icon(): ImageVector {
        return when (this) {
            ExerciseLibrary -> AppTokens.icons.Repository
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