package com.grippo.authorization.registration.experience

import com.grippo.presentation.api.user.models.Experience

internal interface ExperienceContract {
    fun select(value: Experience)
    fun next()

    companion object Empty : ExperienceContract {
        override fun select(value: Experience) {}
        override fun next() {}
    }
}