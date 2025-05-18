package com.grippo.home

import com.grippo.core.BaseViewModel

public class BottomNavigationViewModel :
    BaseViewModel<BottomNavigationState, BottomNavigationDirection, BottomNavigationLoader>(
        BottomNavigationState
    ), BottomNavigationContract