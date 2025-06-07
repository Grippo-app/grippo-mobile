package com.grippo.authorization.auth.process

import com.grippo.core.BaseViewModel

internal class AuthProcessViewModel :
    BaseViewModel<AuthProcessState, AuthProcessDirection, AuthProcessLoader>(AuthProcessState),
    AuthProcessContract {

    override fun back() {
        navigateTo(AuthProcessDirection.Back)
    }
}