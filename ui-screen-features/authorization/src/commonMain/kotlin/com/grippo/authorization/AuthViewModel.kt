package com.grippo.authorization

import com.grippo.core.foundation.BaseViewModel

public class AuthViewModel : BaseViewModel<AuthState, AuthDirection, AuthLoader>(AuthState),
    AuthContract {

    override fun onBack() {
        navigateTo(AuthDirection.Back)
    }

    override fun toSplash() {
        navigateTo(AuthDirection.Splash)
    }

    override fun toOnboarding() {
        navigateTo(AuthDirection.Onboarding)
    }

    override fun toAuthProcess() {
        navigateTo(AuthDirection.AuthProcess)
    }

    override fun toHome() {
        navigateTo(AuthDirection.Home)
    }

    override fun toProfileCreation() {
        navigateTo(AuthDirection.ProfileCreation)
    }
}
