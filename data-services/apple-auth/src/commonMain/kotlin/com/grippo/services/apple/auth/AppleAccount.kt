package com.grippo.services.apple.auth

public data class AppleAccount(
    val token: String,
    val authorizationCode: String,
    val userId: String,
    val fullName: String = "",
    val email: String = "",
)
