package com.grippo.authorization.profile.creation.experience

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.profile.ExperienceEnumState

internal class ExperienceViewModel :
    BaseViewModel<ExperienceState, ExperienceDirection, ExperienceLoader>(ExperienceState()),
    ExperienceContract {

    override fun onExperienceClick(value: ExperienceEnumState) {
        update { it.copy(selected = value) }
    }

    override fun onNextClick() {
        val experience = state.value.selected ?: return
        val direction = ExperienceDirection.ExcludedMuscles(
            experience = experience
        )
        navigateTo(direction)
    }

    override fun onBack() {
        navigateTo(ExperienceDirection.Back)
    }
}
