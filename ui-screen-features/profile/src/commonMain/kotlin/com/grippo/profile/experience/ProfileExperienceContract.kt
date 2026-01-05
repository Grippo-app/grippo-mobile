package com.grippo.profile.experience

import androidx.compose.runtime.Immutable
import com.grippo.core.state.profile.ExperienceEnumState

@Immutable
internal interface ProfileExperienceContract {
    fun onSelectExperience(value: ExperienceEnumState)
    fun onApply()
    fun onBack()

    @Immutable
    companion object Empty : ProfileExperienceContract {
        override fun onSelectExperience(value: ExperienceEnumState) {}
        override fun onApply() {}
        override fun onBack() {}
    }
}
