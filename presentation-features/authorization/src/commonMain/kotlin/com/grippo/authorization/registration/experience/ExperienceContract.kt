package com.grippo.authorization.registration.experience

import com.grippo.presentation.api.profile.models.ExperienceEnumState

internal interface ExperienceContract {
    fun select(value: ExperienceEnumState)
    fun next()
    fun back()

    companion object Empty : ExperienceContract {
        override fun select(value: ExperienceEnumState) {}
        override fun next() {}
        override fun back() {}
    }
}