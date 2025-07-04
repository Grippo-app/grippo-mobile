package com.grippo.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.core.AppTokens
import com.grippo.design.core.UiText
import com.grippo.design.resources.Res
import com.grippo.design.resources.icons.Box
import com.grippo.design.resources.icons.PieChart
import com.grippo.design.resources.icons.User
import com.grippo.design.resources.profile
import com.grippo.design.resources.statistics
import com.grippo.design.resources.trainings

@Immutable
public data class BottomNavigationState(
    val selectedIndex: Int = BottomBarMenu.entries.indexOf(BottomBarMenu.Profile),
)

@Immutable
internal enum class BottomBarMenu {
    Trainings,
    Statistics,
    Profile;

    @Composable
    fun title(): UiText {
        return when (this) {
            Trainings -> UiText.Res(Res.string.trainings)
            Statistics -> UiText.Res(Res.string.statistics)
            Profile -> UiText.Res(Res.string.profile)
        }
    }

    @Composable
    fun icon(): ImageVector {
        return when (this) {
            Trainings -> AppTokens.icons.Box
            Profile -> AppTokens.icons.User
            Statistics -> AppTokens.icons.PieChart
        }
    }
}