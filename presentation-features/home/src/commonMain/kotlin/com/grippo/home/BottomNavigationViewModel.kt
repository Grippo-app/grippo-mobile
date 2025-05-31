package com.grippo.home

import com.grippo.core.BaseViewModel

public class BottomNavigationViewModel :
    BaseViewModel<BottomNavigationState, BottomNavigationDirection, BottomNavigationLoader>(
        BottomNavigationState()
    ), BottomNavigationContract {

    override fun selectPage(index: Int) {
        when (index) {
            0 -> navigateTo(BottomNavigationDirection.Profile)
            1 -> navigateTo(BottomNavigationDirection.Trainings)
        }
        update { it.copy(selectedIndex = index) }
    }
}