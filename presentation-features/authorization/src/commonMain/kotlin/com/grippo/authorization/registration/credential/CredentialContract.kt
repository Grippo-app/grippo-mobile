package com.grippo.authorization.registration.credential

internal interface CredentialContract {
    fun setEmail(value: String)
    fun setPassword(value: String)
    fun next()
    fun back()

    companion object Empty : CredentialContract {
        override fun setEmail(value: String) {}
        override fun setPassword(value: String) {}
        override fun next() {}
        override fun back() {}
    }
}