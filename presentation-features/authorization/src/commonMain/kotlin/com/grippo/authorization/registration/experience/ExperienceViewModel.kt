package com.grippo.authorization.registration.experience

import com.grippo.core.BaseViewModel
import com.grippo.presentation.api.profile.models.ExperienceEnumState

internal class ExperienceViewModel :
    BaseViewModel<ExperienceState, ExperienceDirection, ExperienceLoader>(ExperienceState()),
    ExperienceContract {

    override fun select(value: ExperienceEnumState) {
        update { it.copy(selected = value) }
    }

    override fun next() {
        val direction = ExperienceDirection.ExcludedMuscles(
            experience = state.value.selected ?: return
        )
        navigateTo(direction)
    }

    override fun back() {
        navigateTo(ExperienceDirection.Back)
    }
}