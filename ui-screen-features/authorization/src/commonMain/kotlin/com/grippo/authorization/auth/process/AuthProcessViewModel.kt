package com.grippo.authorization.auth.process

import com.grippo.core.foundation.BaseViewModel

internal class AuthProcessViewModel :
    BaseViewModel<AuthProcessState, AuthProcessDirection, AuthProcessLoader>(AuthProcessState),
    AuthProcessContract {

    override fun onClose() {
        navigateTo(AuthProcessDirection.Close)
    }

    override fun toRegistration() {
        navigateTo(AuthProcessDirection.Registration)
    }

    override fun toHome() {
        navigateTo(AuthProcessDirection.Home)
    }

    override fun toProfileCreation() {
        navigateTo(AuthProcessDirection.ProfileCreation)
    }

    override fun onBack() {
        navigateTo(AuthProcessDirection.Back)
    }
}
