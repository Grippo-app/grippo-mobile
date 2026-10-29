package com.grippo.home

import com.grippo.core.foundation.BaseViewModel
import com.grippo.screen.api.TrainingsRouter

public class HomeRootViewModel : BaseViewModel<HomeRootState, HomeRootDirection, HomeRootLoader>(
    HomeRootState
), HomeRootContract {

    override fun onBack() {
        navigateTo(HomeRootDirection.Back)
    }

    override fun toTrainings(route: TrainingsRouter) {
        navigateTo(HomeRootDirection.Trainings(route))
    }

    override fun toGoal() {
        navigateTo(HomeRootDirection.Goal)
    }
}
