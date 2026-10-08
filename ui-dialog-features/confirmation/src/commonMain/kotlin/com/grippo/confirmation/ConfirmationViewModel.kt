package com.grippo.confirmation

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.UiText

public class ConfirmationViewModel(
    description: UiText?,
) : BaseViewModel<ConfirmationState, ConfirmationDirection, ConfirmationLoader>(
    ConfirmationState(
        description = description
    )
), ConfirmationContract {

    override fun onConfirm() {
        navigateTo(ConfirmationDirection.Confirm)
    }

    override fun onBack() {
        navigateTo(ConfirmationDirection.Back)
    }
}
