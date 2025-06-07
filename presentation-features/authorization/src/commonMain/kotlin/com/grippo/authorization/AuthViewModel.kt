package com.grippo.authorization

import com.grippo.core.BaseViewModel

public class AuthViewModel : BaseViewModel<AuthState, AuthDirection, AuthLoader>(AuthState),
    AuthContract {

    override fun back() {
        navigateTo(AuthDirection.Back)
    }
}