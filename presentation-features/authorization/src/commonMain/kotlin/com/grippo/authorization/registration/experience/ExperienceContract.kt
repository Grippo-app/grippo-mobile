package com.grippo.authorization.registration.experience

internal interface ExperienceContract {
    fun next()

    companion object Empty : ExperienceContract {
        override fun next() {}
    }
}