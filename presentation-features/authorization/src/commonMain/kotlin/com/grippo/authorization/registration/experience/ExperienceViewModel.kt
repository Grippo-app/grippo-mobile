package com.grippo.authorization.registration.experience

import com.grippo.core.BaseViewModel

internal class ExperienceViewModel :
    BaseViewModel<ExperienceState, ExperienceDirection, ExperienceLoader>(ExperienceState()),
    ExperienceContract {
    override fun next() {

    }
}