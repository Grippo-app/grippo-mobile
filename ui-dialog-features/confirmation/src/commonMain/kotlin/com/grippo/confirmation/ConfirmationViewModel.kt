package com.grippo.confirmation

import com.grippo.analytics.confirmation.ConfirmationAnalytics
import com.grippo.analytics.confirmation.models.ConfirmationEvent
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.UiText

public class ConfirmationViewModel(
    description: UiText?,
    private val analytics: ConfirmationAnalytics,
) : BaseViewModel<ConfirmationState, ConfirmationDirection, ConfirmationLoader>(
    ConfirmationState(
        description = description
    )
), ConfirmationContract {

    override fun onConfirm() {
        analytics.trackEvent(ConfirmationEvent.Confirm)
        navigateTo(ConfirmationDirection.Confirm)
    }

    override fun onBack() {
        analytics.trackEvent(ConfirmationEvent.Back)
        navigateTo(ConfirmationDirection.Back)
    }
}
