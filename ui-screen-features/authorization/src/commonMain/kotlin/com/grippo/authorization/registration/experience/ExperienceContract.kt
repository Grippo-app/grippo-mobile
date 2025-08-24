package com.grippo.authorization.registration.experience

import com.grippo.state.profile.ExperienceEnumState

internal interface ExperienceContract {
    fun onExperienceClick(value: ExperienceEnumState)
    fun onNextClick()
    fun onBack()

    companion object Empty : ExperienceContract {
        override fun onExperienceClick(value: ExperienceEnumState) {}
        override fun onNextClick() {}
        override fun onBack() {}
    }
}