package com.grippo.authorization.profile.creation.experience

import com.grippo.analytics.authorization.AuthAnalytics
import com.grippo.analytics.authorization.models.AuthEvent
import com.grippo.analytics.authorization.models.AuthScreen
import com.grippo.analytics.common.models.ControlAction
import com.grippo.authorization.analytics.AuthInputMapping
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.toolkit.analytics.context.NavigationReason

internal class ExperienceViewModel(
    private val analytics: AuthAnalytics,
    initialState: ExperienceState = ExperienceState(),
) : BaseViewModel<ExperienceState, ExperienceDirection, ExperienceLoader>(initialState),
    ExperienceContract {

    override val screenPresentation = { reason: NavigationReason ->
        analytics.presentScreen(AuthScreen.Experience, reason)
    }

    override fun onExperienceClick(value: ExperienceEnumState) {
        update { it.copy(selected = value) }
        analytics.trackEvent(AuthEvent.Experience.Selected(AuthInputMapping.experience(value)))
    }

    override fun onNextClick() {
        val experience = state.value.selected ?: return
        analytics.trackEvent(AuthEvent.Experience.Control(ControlAction.Next))
        val direction = ExperienceDirection.ExcludedMuscles(
            experience = experience
        )
        navigateTo(direction)
    }

    override fun onBack() {
        analytics.trackEvent(AuthEvent.Experience.Control(ControlAction.DismissRequested))
        navigateTo(ExperienceDirection.Back)
    }
}
