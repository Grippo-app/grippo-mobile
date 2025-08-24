package com.grippo.confirmation

import com.grippo.core.BaseViewModel

public class ConfirmationViewModel(
    title: String,
    description: String?,
) : BaseViewModel<ConfirmationState, ConfirmationDirection, ConfirmationLoader>(
    ConfirmationState(
        title = title,
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
