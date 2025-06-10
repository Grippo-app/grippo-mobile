package com.grippo.authorization.registration.name

import com.grippo.core.BaseViewModel
import com.grippo.presentation.api.auth.models.NameFormatState

internal class NameViewModel : BaseViewModel<NameState, NameDirection, NameLoader>(NameState()),
    NameContract {

    override fun setName(value: String) {
        update { it.copy(name = NameFormatState.of(value)) }
    }

    override fun next() {
        val direction = NameDirection.Body(
            name = state.value.name.value
        )
        navigateTo(direction)
    }

    override fun back() {
        navigateTo(NameDirection.Back)
    }
}