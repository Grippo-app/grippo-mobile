package com.grippo.authorization.profile.creation.name

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.NameFormatState
import com.grippo.data.features.api.authorization.AuthorizationFeature

internal class NameViewModel(
    private val authorizationFeature: AuthorizationFeature
) : BaseViewModel<NameState, NameDirection, NameLoader>(NameState()),
    NameContract {

    override fun onNameChange(value: String) {
        update { it.copy(name = NameFormatState.of(value)) }
    }

    override fun onNextClick() {
        val name = (state.value.name as? NameFormatState.Valid) ?: return

        val direction = NameDirection.Body(
            name = name.value
        )
        navigateTo(direction)
    }

    override fun onBack() {
        safeLaunch {
            authorizationFeature.logout()
            navigateTo(NameDirection.Back)
        }
    }
}