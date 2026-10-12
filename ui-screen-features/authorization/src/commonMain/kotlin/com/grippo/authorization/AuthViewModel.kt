package com.grippo.authorization

import com.grippo.core.foundation.BaseViewModel
import com.grippo.screen.api.AuthorizationEntryPoint

public class AuthViewModel : BaseViewModel<AuthState, AuthDirection, AuthLoader>(AuthState),
    AuthContract {

    override fun onBack() {
        navigateTo(AuthDirection.Back)
    }

    override fun toAuthProcess() {
        navigateTo(AuthDirection.AuthProcess(AuthorizationEntryPoint.ProfileSetupReturn))
    }

    internal fun toAuthProcess(entryPoint: AuthorizationEntryPoint) {
        navigateTo(AuthDirection.AuthProcess(entryPoint))
    }

    override fun toHome() {
        navigateTo(AuthDirection.Home)
    }

    override fun toProfileCreation() {
        navigateTo(AuthDirection.ProfileCreation)
    }
}
