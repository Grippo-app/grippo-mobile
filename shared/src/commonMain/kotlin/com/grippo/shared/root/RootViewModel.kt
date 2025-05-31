package com.grippo.shared.root

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.authorization.AuthorizationFeature
import kotlinx.coroutines.flow.onEach

public class RootViewModel(
    authorizationFeature: AuthorizationFeature
) : BaseViewModel<RootState, RootDirection, RootLoader>(RootState),
    RootContract {

    init {
        authorizationFeature
            .getToken()
            .onEach { if (it == null) navigateTo(RootDirection.Login) }
            .safeLaunch()
    }
}