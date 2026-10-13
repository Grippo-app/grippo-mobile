package com.grippo.services.backend.client

import com.grippo.services.backend.dto.auth.RefreshBody
import com.grippo.services.backend.dto.auth.TokenResponse
import com.grippo.services.database.dao.TokenDao
import com.grippo.services.database.dao.UserActiveDao
import com.grippo.services.database.entity.TokenEntity
import com.grippo.toolkit.logger.AppLogger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.AuthCircuitBreaker
import io.ktor.client.plugins.auth.AuthProvider
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.headers
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.AuthScheme
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.http.path
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.koin.core.annotation.Single

@Single
internal class TokenProvider(
    private val tokenDao: TokenDao,
    private val userActiveDao: UserActiveDao,
) : AuthProvider {
    private val refreshMutex = Mutex()
    private var lastRefreshError: Throwable? = null
    private var lastKnownInvalidAccessToken: String? = null

    private companion object {
        private const val REFRESH_WAIT_TIMEOUT_MS = 10_000L
    }

    @Deprecated("Use sendWithoutRequest(request) instead", level = DeprecationLevel.ERROR)
    override val sendWithoutRequest: Boolean = true

    override fun sendWithoutRequest(request: HttpRequestBuilder): Boolean = true

    override fun isApplicable(auth: HttpAuthHeader): Boolean {
        return auth is HttpAuthHeader.Parameterized && auth.authScheme == AuthScheme.Bearer
    }

    override suspend fun addRequestHeaders(
        request: HttpRequestBuilder,
        authHeader: HttpAuthHeader?
    ) {
        val token = getCurrentToken()
        val accessToken = token?.access

        request.headers {
            remove("Authorization")

            if (!accessToken.isNullOrBlank()) {
                val bearer = "Bearer $accessToken"
                append("Authorization", bearer)
            }
        }
    }

    override suspend fun refreshToken(response: HttpResponse): Boolean {
        return if (refreshMutex.isLocked) {
            waitForOngoingRefresh()
        } else {
            performTokenRefreshBlock(response)
        }
    }

    private suspend fun waitForOngoingRefresh(): Boolean {
        withTimeout(REFRESH_WAIT_TIMEOUT_MS.milliseconds) {
            refreshMutex.withLock {
                val token = getCurrentToken()

                if (token?.access != lastKnownInvalidAccessToken) {
                    return@withLock
                }

                lastRefreshError?.let {
                    throw it
                }
            }
        }

        return true
    }

    private suspend fun performTokenRefreshBlock(response: HttpResponse): Boolean {
        return try {
            withTimeout(REFRESH_WAIT_TIMEOUT_MS.milliseconds) {
                refreshMutex.withLock {
                    lastRefreshError = null

                    val token = getCurrentToken() ?: return@withLock false

                    val accessToken = token.requireAccess()
                    val refreshToken = token.requireRefresh()

                    lastKnownInvalidAccessToken = accessToken

                    try {
                        val refresh = retryWithBackoff {
                            performTokenRefresh(response.call.client, refreshToken)
                        }

                        val newId = refresh.id ?: return@withLock false

                        tokenDao.insertOrUpdate(
                            TokenEntity(
                                id = newId,
                                access = refresh.accessToken,
                                refresh = refresh.refreshToken
                            )
                        )

                        true
                    } catch (e: Throwable) {
                        if (e is Error) throw e

                        handleRefreshFailure(e)
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            handleRefreshFailure(e)
        }
    }

    private suspend fun performTokenRefresh(
        client: HttpClient,
        refreshToken: String
    ): TokenResponse {
        return client.submitForm {
            attributes.put(AuthCircuitBreaker, Unit)
            url {
                method = HttpMethod.Post
                path("/auth/refresh")
                setBody(RefreshBody(refreshToken = refreshToken))
            }
        }.also {
            AppLogger.Network.log("POST /auth/refresh ${it.status.value}")
        }.let { response ->
            if (response.status == HttpStatusCode.Unauthorized) {
                throw RefreshUnauthorizedException("Refresh token was rejected by backend")
            }

            response.body()
        }
    }

    private suspend fun handleRefreshFailure(e: Throwable): Nothing {
        lastRefreshError = e
        lastKnownInvalidAccessToken = null

        if (e !is CancellationException) {
            val userId = userActiveDao.get().firstOrNull()
            if (userId != null) {
                tokenDao.delete(userId)
            }
        }

        throw e
    }

    private suspend fun getCurrentToken(): TokenEntity? {
        return userActiveDao.get().firstOrNull()?.let { tokenDao.getById(it).firstOrNull() }
    }

    private fun TokenEntity?.requireAccess(): String {
        return this?.access?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Access token is missing")
    }

    private fun TokenEntity?.requireRefresh(): String {
        return this?.refresh?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Refresh token is missing")
    }

    private suspend fun <T> retryWithBackoff(
        maxAttempts: Int = 3,
        initialDelay: Long = 500,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(maxAttempts - 1) {
            try {
                return block()
            } catch (e: Throwable) {
                if (e is CancellationException || e is Error) throw e
                if (e is RefreshUnauthorizedException) throw e
            }
            delay(currentDelay.milliseconds)
            currentDelay = (currentDelay * factor).toLong()
        }
        return block()
    }

    private class RefreshUnauthorizedException(message: String) : IllegalStateException(message)
}
