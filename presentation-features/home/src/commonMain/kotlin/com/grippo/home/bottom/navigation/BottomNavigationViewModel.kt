package com.grippo.home.bottom.navigation

import com.grippo.core.BaseViewModel

internal class BottomNavigationViewModel :
    BaseViewModel<BottomNavigationState, BottomNavigationDirection, BottomNavigationLoader>(
        BottomNavigationState
    ), BottomNavigationContract