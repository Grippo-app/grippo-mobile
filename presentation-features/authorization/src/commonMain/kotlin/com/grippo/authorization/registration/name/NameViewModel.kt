package com.grippo.authorization.registration.name

import com.grippo.core.BaseViewModel
import com.grippo.presentation.api.auth.models.Name

internal class NameViewModel : BaseViewModel<NameState, NameDirection, NameLoader>(NameState()),
    NameContract {
    override fun setName(value: String) {
        update { it.copy(name = Name.of(value)) }
    }

    override fun next() {
        navigateTo(NameDirection.Body)
    }
}