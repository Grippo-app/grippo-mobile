package com.grippo.authorization

import com.grippo.authorization.login.LoginContract

internal interface AuthContract {

    companion object Empty : LoginContract {

    }
}