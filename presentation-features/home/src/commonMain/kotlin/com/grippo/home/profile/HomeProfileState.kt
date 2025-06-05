package com.grippo.home.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.Res
import com.grippo.design.resources.icons.Book
import com.grippo.design.resources.icons.Heart
import com.grippo.design.resources.icons.Mic
import com.grippo.design.resources.profile_menu_excluded_muscles
import com.grippo.design.resources.profile_menu_exercise_library
import com.grippo.design.resources.profile_menu_missing_equipment
import com.grippo.design.resources.profile_menu_weight_history
import com.grippo.presentation.api.profile.models.UserState

@Immutable
internal data class HomeProfileState(
    val user: UserState? = null
)

@Immutable
internal enum class HomeProfileMenu {
    WeightHistory,
    ExcludedMuscles,
    MissingEquipment,
    ExerciseLibrary;

    @Composable
    fun text(): String {
        return when (this) {
            WeightHistory -> AppTokens.strings.res(Res.string.profile_menu_weight_history)
            ExerciseLibrary -> AppTokens.strings.res(Res.string.profile_menu_exercise_library)
            ExcludedMuscles -> AppTokens.strings.res(Res.string.profile_menu_excluded_muscles)
            MissingEquipment -> AppTokens.strings.res(Res.string.profile_menu_missing_equipment)
        }
    }

    @Composable
    fun icon(): ImageVector {
        return when (this) {
            WeightHistory -> AppTokens.icons.Book
            ExerciseLibrary -> AppTokens.icons.Book
            ExcludedMuscles -> AppTokens.icons.Mic
            MissingEquipment -> AppTokens.icons.Heart
        }
    }
}