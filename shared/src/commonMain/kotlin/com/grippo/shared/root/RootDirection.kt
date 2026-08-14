package com.grippo.shared.root

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.stage.StageState
import com.grippo.screen.api.AuthRouter
import com.grippo.screen.api.ProfileRouter
import com.grippo.screen.api.RootRouter

public sealed interface RootDirection : BaseDirection {
    public data object Login : RootDirection
    public data object Close : RootDirection
    public data object Main : RootDirection
    public data object Profile : RootDirection
    public data object Debug : RootDirection
    public data object Trainings : RootDirection
    public data class Training(val stage: StageState) : RootDirection
    public data object WeightHistory : RootDirection
    public data object MissingEquipment : RootDirection
    public data object ExcludedMuscles : RootDirection
    public data object Experience : RootDirection
    public data object Settings : RootDirection
    public data object Social : RootDirection
    public data object Goal : RootDirection
    public data object Back : RootDirection
}

/**
 * Pure navigation seam: maps a [RootDirection] intent to a concrete stack
 * operation over [RootRouter]. Kept side-effect free so it can be unit tested
 * without a live Decompose component or Koin graph.
 */
internal sealed interface RootNav {
    data class Push(val router: RootRouter) : RootNav
    data class ReplaceAll(val router: RootRouter) : RootNav
    data object Pop : RootNav
    data object Close : RootNav
}

internal fun RootDirection.toNav(): RootNav = when (this) {
    RootDirection.Main -> RootNav.ReplaceAll(RootRouter.Main)
    RootDirection.Login -> RootNav.ReplaceAll(RootRouter.Auth(AuthRouter.AuthProcess))
    is RootDirection.Training -> RootNav.Push(RootRouter.Training(stage))
    RootDirection.Profile -> RootNav.Push(RootRouter.Profile(ProfileRouter.Body))
    RootDirection.WeightHistory -> RootNav.Push(RootRouter.Profile(ProfileRouter.Body))
    RootDirection.MissingEquipment -> RootNav.Push(RootRouter.Profile(ProfileRouter.Equipments))
    RootDirection.ExcludedMuscles -> RootNav.Push(RootRouter.Profile(ProfileRouter.Muscles))
    RootDirection.Experience -> RootNav.Push(RootRouter.Profile(ProfileRouter.Experience))
    RootDirection.Settings -> RootNav.Push(RootRouter.Profile(ProfileRouter.Settings))
    RootDirection.Social -> RootNav.Push(RootRouter.Profile(ProfileRouter.Social))
    RootDirection.Goal -> RootNav.Push(RootRouter.Profile(ProfileRouter.Goal))
    RootDirection.Debug -> RootNav.Push(RootRouter.Debug)
    RootDirection.Trainings -> RootNav.Push(RootRouter.Trainings)
    RootDirection.Back -> RootNav.Pop
    RootDirection.Close -> RootNav.Close
}
