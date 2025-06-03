package com.grippo.home

import com.grippo.core.BaseViewModel

public class BottomNavigationViewModel :
    BaseViewModel<BottomNavigationState, BottomNavigationDirection, BottomNavigationLoader>(
        BottomNavigationState()
    ), BottomNavigationContract {

    override fun selectPage(index: Int) {
        when (index) {
            0 -> navigateTo(BottomNavigationDirection.Trainings)
            1 -> navigateTo(BottomNavigationDirection.Statistics)
            2 -> navigateTo(BottomNavigationDirection.Profile)
            else -> return
        }
        update { it.copy(selectedIndex = index) }
    }
}