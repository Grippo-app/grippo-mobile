package com.grippo.home

import com.grippo.core.foundation.BaseViewModel

public class HomeRootViewModel : BaseViewModel<HomeRootState, HomeRootDirection, HomeRootLoader>(
    HomeRootState
), HomeRootContract {

    override fun onBack() {
        navigateTo(HomeRootDirection.Back)
    }

    override fun toTrainings() {
        navigateTo(HomeRootDirection.Trainings)
    }

    override fun toGoal() {
        navigateTo(HomeRootDirection.Goal)
    }
}
