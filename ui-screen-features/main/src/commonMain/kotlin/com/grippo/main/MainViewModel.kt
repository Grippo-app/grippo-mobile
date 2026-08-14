package com.grippo.main

import com.grippo.core.foundation.BaseViewModel

public class MainViewModel :
    BaseViewModel<MainState, MainDirection, MainLoader>(MainState),
    MainContract {

    override fun onTabSelected(index: Int) {
        navigateTo(MainDirection.SelectTab(index))
    }

    override fun onStartTraining() {
        navigateTo(MainDirection.StartTraining)
    }
}
