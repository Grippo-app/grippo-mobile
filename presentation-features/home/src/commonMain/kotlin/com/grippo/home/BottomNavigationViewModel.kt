package com.grippo.home

import com.grippo.core.BaseViewModel

public class BottomNavigationViewModel :
    BaseViewModel<BottomNavigationState, BottomNavigationDirection, BottomNavigationLoader>(
        BottomNavigationState()
    ), BottomNavigationContract {

    override fun selectPage(index: Int) {
        update { it.copy(selectedIndex = index) }
    }
}