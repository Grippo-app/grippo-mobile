package com.grippo.authorization.registration.experience

import androidx.compose.runtime.Immutable
import com.grippo.core.state.profile.ExperienceEnumState

@Immutable
internal interface ExperienceContract {
    fun onExperienceClick(value: ExperienceEnumState)
    fun onNextClick()
    fun onBack()

    @Immutable
    companion object Empty : ExperienceContract {
        override fun onExperienceClick(value: ExperienceEnumState) {}
        override fun onNextClick() {}
        override fun onBack() {}
    }
}