package com.grippo.authorization.registration

internal interface RegistrationContract {
    fun saveCredentials(email: String, password: String)
    fun saveName(name: String)
    fun saveWeightHeight(weight: Float, height: Int)

    companion object Empty : RegistrationContract {
        override fun saveCredentials(email: String, password: String) {}
        override fun saveName(name: String) {}
        override fun saveWeightHeight(weight: Float, height: Int) {}
    }
}