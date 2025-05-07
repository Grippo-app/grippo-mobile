package com.grippo.authorization.registration.experience

import com.grippo.core.BaseViewModel
import com.grippo.presentation.api.auth.models.Experience

internal class ExperienceViewModel :
    BaseViewModel<ExperienceState, ExperienceDirection, ExperienceLoader>(ExperienceState()),
    ExperienceContract {

    override fun select(value: Experience) {
        update { it.copy(selected = value) }
    }

    override fun next() {

    }
}