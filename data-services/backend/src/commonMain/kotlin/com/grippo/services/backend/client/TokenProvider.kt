package com.grippo.services.backend.client

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
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.AuthScheme
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.http.path
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.koin.core.annotation.Single
import kotlin.coroutines.cancellation.CancellationException

@Single
internal class TokenProvider(
    private val tokenDao: com.grippo.services.database.dao.TokenDao,
    private val userActiveDao: com.grippo.services.database.dao.UserActiveDao,
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
        val applicable =
            auth is HttpAuthHeader.Parameterized && auth.authScheme == AuthScheme.Bearer
        logInfo { "Checking isApplicable: $applicable, scheme=${(auth as? HttpAuthHeader.Parameterized)?.authScheme}" }
        return applicable
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
                logInfo { "Set Authorization header: ${bearer.take(25)}..." }
            } else {
                logWarn { "No access token available to add" }
            }
        }
    }

    override suspend fun refreshToken(response: HttpResponse): Boolean {
        logInfo { "Refresh requested (HTTP ${response.status.value})" }

        return if (refreshMutex.isLocked) {
            waitForOngoingRefresh()
        } else {
            performTokenRefreshBlock(response)
        }
    }

    private suspend fun waitForOngoingRefresh(): Boolean {
        logInfo { "⏳ Waiting for another request to refresh token..." }

        return try {
            withTimeout(REFRESH_WAIT_TIMEOUT_MS) {
                refreshMutex.withLock {
                    val token = getCurrentToken()

                    if (token?.access != lastKnownInvalidAccessToken) {
                        logInfo { "🔁 Skip refresh — already handled by another request (token changed)" }
                        return@withLock
                    }

                    lastRefreshError?.let {
                        logError { "⛔ Refresh failed previously: ${it.message}" }
                        throw it
                    }

                    logWarn { "⚠️ Still same token, but no error recorded — unknown state" }
                }
            }
            logInfo { "✅ Refresh completed by another request" }
            true
        } catch (e: TimeoutCancellationException) {
            logError { "⏰ Timeout while waiting for refresh to complete" }
            throw e
        }
    }

    private suspend fun performTokenRefreshBlock(response: HttpResponse): Boolean {
        logInfo { "Starting exclusive token refresh..." }

        return try {
            withTimeout(REFRESH_WAIT_TIMEOUT_MS) {
                refreshMutex.withLock {
                    lastRefreshError = null

                    val (_, token) = getCurrentUserAndToken() ?: run {
                        logError { "No active user or token — aborting refresh" }
                        return@withLock false
                    }

                    val accessToken = token.requireAccess()
                    val refreshToken = token.requireRefresh()

                    lastKnownInvalidAccessToken = accessToken

                    try {
                        logInfo { "Calling /auth/refresh..." }
                        val refresh = retryWithBackoff {
                            performTokenRefresh(response.call.client, refreshToken)
                        }

                        val newId = AppLogger.Mapping.log(refresh.id) { "TokenResponse.id is null" }
                            ?: return@withLock false

                        tokenDao.insertOrUpdate(
                            _root_ide_package_.com.grippo.services.database.entity.TokenEntity(
                                id = newId,
                                access = refresh.accessToken,
                                refresh = refresh.refreshToken
                            )
                        )

                        logInfo { "Token refresh successful" }
                        true
                    } catch (e: Throwable) {
                        logError { "Refresh call failed: ${e.message}" }
                        handleRefreshFailure(e)
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            logError { "Timeout during token refresh" }
            handleRefreshFailure(e)
        }
    }

    private suspend fun performTokenRefresh(
        client: HttpClient,
        refreshToken: String
    ): com.grippo.services.backend.dto.auth.TokenResponse {
        logDebug { "Requesting token refresh..." }

        return client.submitForm {
            attributes.put(AuthCircuitBreaker, Unit)
            url {
                method = HttpMethod.Post
                path("/auth/refresh")
                setBody(
                    _root_ide_package_.com.grippo.services.backend.dto.auth.RefreshBody(
                        refreshToken = refreshToken
                    )
                )
            }
        }.also {
            logDebug { "Refresh response received: ${it.status.value}" }
        }.let { response ->
            if (response.status == HttpStatusCode.Unauthorized) {
                val errorBody = runCatching { response.bodyAsText() }.getOrNull()
                logWarn {
                    val suffix = errorBody?.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""
                    "🚫 Refresh rejected with HTTP 401$suffix"
                }
                throw RefreshUnauthorizedException("Refresh token was rejected by backend")
            }

            response.body()
        }
    }

    private suspend fun handleRefreshFailure(e: Throwable): Nothing {
        lastRefreshError = e
        lastKnownInvalidAccessToken = null

        logWarn { "🧨 Refresh failed — error will propagate to all waiting requests (${e::class.simpleName}: ${e.message})" }

        when (e) {
            is CancellationException -> {
                logWarn {
                    "⚠️ Refresh was cancelled by coroutine host — skipping token deletion to avoid side effects"
                }
            }

            else -> {
                val userId = userActiveDao.get().firstOrNull()
                if (userId != null) {
                    logInfo { "🗑 Deleting tokens for user $userId after refresh failure" }
                    tokenDao.delete(userId)
                } else {
                    logWarn { "⚠️ No active user found — cannot delete token after refresh failure" }
                }
            }
        }

        logError { "❌ Throwing refresh error" }
        throw e
    }

    private suspend fun getCurrentToken(): com.grippo.services.database.entity.TokenEntity? {
        return userActiveDao.get().firstOrNull()?.let { tokenDao.getById(it).firstOrNull() }
    }

    private suspend fun getCurrentUserAndToken(): Pair<String, com.grippo.services.database.entity.TokenEntity>? {
        val userId = userActiveDao.get().firstOrNull() ?: return null
        val token = tokenDao.getById(userId).firstOrNull() ?: return null
        return userId to token
    }

    private fun com.grippo.services.database.entity.TokenEntity?.requireAccess(): String {
        return this?.access?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Access token is missing")
    }

    private fun com.grippo.services.database.entity.TokenEntity?.requireRefresh(): String {
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
        repeat(maxAttempts - 1) { attempt ->
            try {
                return block()
            } catch (e: Throwable) {
                if (e is RefreshUnauthorizedException) throw e
                logWarn { "Retry $attempt failed: ${e.message}" }
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong()
        }
        return block()
    }

    private class RefreshUnauthorizedException(message: String) : IllegalStateException(message)

    private inline fun logInfo(message: () -> String) {
        AppLogger.Network.log("ℹ️ [TokenProvider] ${message()}")
    }

    private inline fun logWarn(message: () -> String) {
        AppLogger.Network.log("⚠️ [TokenProvider] ${message()}")
    }

    private inline fun logError(message: () -> String) {
        AppLogger.Network.log("❌ [TokenProvider] ${message()}")
    }

    private inline fun logDebug(message: () -> String) {
        AppLogger.Network.log("🔧 [TokenProvider] ${message()}")
    }
}
