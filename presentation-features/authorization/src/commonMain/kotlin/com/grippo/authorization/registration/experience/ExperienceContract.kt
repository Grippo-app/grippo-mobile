package com.grippo.authorization.registration.experience

import com.grippo.presentation.api.user.models.ExperienceEnumState

internal interface ExperienceContract {
    fun select(value: ExperienceEnumState)
    fun next()

    companion object Empty : ExperienceContract {
        override fun select(value: ExperienceEnumState) {}
        override fun next() {}
    }
}