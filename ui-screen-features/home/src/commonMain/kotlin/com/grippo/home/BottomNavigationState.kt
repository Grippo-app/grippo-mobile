package com.grippo.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.core.state.formatters.UiText
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.Gym
import com.grippo.design.resources.provider.icons.Reports
import com.grippo.design.resources.provider.statistics
import com.grippo.design.resources.provider.trainings
import com.grippo.screen.api.BottomNavigationRouter

@Immutable
public data class BottomNavigationState(
    val selected: BottomBarMenu,
)

@Immutable
public enum class BottomBarMenu {
    Trainings,
    Statistics;

    @Composable
    internal fun title(): UiText {
        return when (this) {
            Trainings -> UiText.Res(Res.string.trainings)
            Statistics -> UiText.Res(Res.string.statistics)
        }
    }

    @Composable
    internal fun icon(): ImageVector {
        return when (this) {
            Trainings -> AppTokens.icons.Gym
            Statistics -> AppTokens.icons.Reports
        }
    }

    internal companion object {
        internal fun of(rout: BottomNavigationRouter): BottomBarMenu {
            return when (rout) {
                BottomNavigationRouter.Statistics -> Statistics
                BottomNavigationRouter.Trainings -> Trainings
            }
        }
    }
}