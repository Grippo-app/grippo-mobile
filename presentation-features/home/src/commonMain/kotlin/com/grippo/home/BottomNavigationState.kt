package com.grippo.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.core.AppTokens
import com.grippo.design.core.UiText
import com.grippo.design.resources.Res
import com.grippo.design.resources.icons.Gym
import com.grippo.design.resources.icons.Reports
import com.grippo.design.resources.icons.User
import com.grippo.design.resources.profile
import com.grippo.design.resources.statistics
import com.grippo.design.resources.trainings
import com.grippo.presentation.api.bottom.navigation.BottomNavigationRouter

@Immutable
public data class BottomNavigationState(
    val selected: BottomBarMenu,
)

@Immutable
public enum class BottomBarMenu {
    Trainings,
    Statistics,
    Profile;

    @Composable
    internal fun title(): UiText {
        return when (this) {
            Trainings -> UiText.Res(Res.string.trainings)
            Statistics -> UiText.Res(Res.string.statistics)
            Profile -> UiText.Res(Res.string.profile)
        }
    }

    @Composable
    internal fun icon(): ImageVector {
        return when (this) {
            Trainings -> AppTokens.icons.Gym
            Statistics -> AppTokens.icons.Reports
            Profile -> AppTokens.icons.User
        }
    }

    internal companion object {
        internal fun of(rout: BottomNavigationRouter): BottomBarMenu {
            return when (rout) {
                BottomNavigationRouter.Profile -> Profile
                BottomNavigationRouter.Statistics -> Statistics
                BottomNavigationRouter.Trainings -> Trainings
            }
        }
    }
}