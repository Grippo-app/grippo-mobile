package com.grippo.authorization.registration.name

import com.grippo.core.foundation.BaseViewModel
import com.grippo.state.formatters.NameFormatState

internal class NameViewModel : BaseViewModel<NameState, NameDirection, NameLoader>(NameState()),
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
        navigateTo(NameDirection.Back)
    }
}