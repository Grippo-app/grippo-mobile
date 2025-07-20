package com.grippo.shared.root

import com.grippo.connectivity.Connectivity
import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.authorization.AuthorizationFeature
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

public class RootViewModel(
    authorizationFeature: AuthorizationFeature,
    connectivity: Connectivity
) : BaseViewModel<RootState, RootDirection, RootLoader>(RootState), RootContract {

    init {
        authorizationFeature
            .getToken()
            .onEach { if (it == null) navigateTo(RootDirection.Login) }
            .safeLaunch()

        connectivity.start()

        connectivity.monitoring.onEach {

        }.launchIn(coroutineScope)
    }

    override fun back() {
        navigateTo(RootDirection.Back)
    }
}