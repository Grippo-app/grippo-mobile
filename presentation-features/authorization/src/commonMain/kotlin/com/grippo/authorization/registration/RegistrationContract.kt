package com.grippo.authorization.registration

internal interface RegistrationContract {
    fun saveCredentials(email: String, password: String)
    fun saveName(name: String)
}