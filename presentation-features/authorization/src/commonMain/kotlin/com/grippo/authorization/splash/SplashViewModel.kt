package com.grippo.authorization.splash

import com.grippo.core.BaseViewModel
import kotlinx.coroutines.delay

internal class SplashViewModel : BaseViewModel<Unit, SplashDirection>(Unit), SplashContract {
    init {
        safeLaunch {
            delay(1000)
            navigateTo(SplashDirection.AuthProcess)
        }
    }
}