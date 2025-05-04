package com.grippo.authorization.registration.credential

internal interface CredentialContract {
    fun setEmail(value: String)
    fun setPassword(value: String)
    fun next()
}