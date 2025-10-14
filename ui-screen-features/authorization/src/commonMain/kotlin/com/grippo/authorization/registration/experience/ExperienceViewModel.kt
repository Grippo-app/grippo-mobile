package com.grippo.authorization.registration.experience

import com.grippo.core.foundation.BaseViewModel
import com.grippo.state.profile.ExperienceEnumState

internal class ExperienceViewModel :
    BaseViewModel<ExperienceState, ExperienceDirection, ExperienceLoader>(ExperienceState()),
    ExperienceContract {

    override fun onExperienceClick(value: ExperienceEnumState) {
        update { it.copy(selected = value) }
    }

    override fun onNextClick() {
        val direction = ExperienceDirection.ExcludedMuscles(
            experience = state.value.selected ?: return
        )
        navigateTo(direction)
    }

    override fun onBack() {
        navigateTo(ExperienceDirection.Back)
    }
}