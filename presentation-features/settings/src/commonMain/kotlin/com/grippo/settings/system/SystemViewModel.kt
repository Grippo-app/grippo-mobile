package com.grippo.settings.system

import com.grippo.core.BaseViewModel

internal class SystemViewModel :
    BaseViewModel<SystemState, SystemDirection, SystemLoader>(SystemState),
    SystemContract {

    override fun back() {
        navigateTo(SystemDirection.Back)
    }
}