package com.grippo.authorization.registration

import com.grippo.presentation.api.user.models.Experience

internal interface RegistrationContract {
    fun saveCredentials(email: String, password: String)
    fun saveName(name: String)
    fun saveWeightHeight(weight: Float, height: Int)
    fun saveExperience(experience: Experience)

    companion object Empty : RegistrationContract {
        override fun saveCredentials(email: String, password: String) {}
        override fun saveName(name: String) {}
        override fun saveWeightHeight(weight: Float, height: Int) {}
        override fun saveExperience(experience: Experience) {}
    }
}