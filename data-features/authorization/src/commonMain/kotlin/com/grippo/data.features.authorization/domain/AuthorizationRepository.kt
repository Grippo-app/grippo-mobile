package com.grippo.data.features.authorization.domain

import kotlinx.coroutines.flow.Flow

internal interface AuthorizationRepository {

    suspend fun login(email: String, password: String): Unit

    suspend fun google(token: String): Unit

    suspend fun apple(code: String): Unit

    suspend fun register(email: String, password: String): Unit

    suspend fun logout()

    fun getToken(): Flow<String?>

    suspend fun updatePushToken(token: String): Unit

    suspend fun getPushToken(): String?

    suspend fun deletePushToken(): Unit
}
