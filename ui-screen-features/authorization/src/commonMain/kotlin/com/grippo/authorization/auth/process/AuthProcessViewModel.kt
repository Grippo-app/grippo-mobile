package com.grippo.authorization.auth.process

import com.grippo.core.foundation.BaseViewModel

internal class AuthProcessViewModel :
    BaseViewModel<AuthProcessState, AuthProcessDirection, AuthProcessLoader>(AuthProcessState),
    AuthProcessContract {

    override fun onClose() {
        navigateTo(AuthProcessDirection.Close)
    }

    override fun toRegistration() {
        navigateTo(AuthProcessDirection.ToRegistration)
    }

    override fun toHome() {
        navigateTo(AuthProcessDirection.ToHome)
    }

    override fun toProfileCreation() {
        navigateTo(AuthProcessDirection.ToProfileCreation)
    }

    override fun onBack() {
        navigateTo(AuthProcessDirection.Back)
    }
}
