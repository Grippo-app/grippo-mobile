package com.grippo.main

import com.grippo.core.foundation.BaseViewModel
import com.grippo.screen.api.MainRouter

public class MainViewModel : BaseViewModel<MainState, MainDirection, MainLoader>(MainState), MainContract {
    override fun onSelectTab(tab: MainRouter) {
        navigateTo(MainDirection.SelectTab(tab))
    }

    override fun onBack() {
        navigateTo(MainDirection.Back)
    }
}
