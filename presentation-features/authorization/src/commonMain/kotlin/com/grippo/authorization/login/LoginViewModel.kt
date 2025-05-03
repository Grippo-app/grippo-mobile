package com.grippo.authorization.login

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.authorization.AuthorizationFeature
import com.grippo.data.features.api.user.UserFeature

internal class LoginViewModel(
    private val authorizationFeature: AuthorizationFeature,
    private val userFeature: UserFeature
) : BaseViewModel<LoginState, LoginDirection>(LoginState),
    LoginContract