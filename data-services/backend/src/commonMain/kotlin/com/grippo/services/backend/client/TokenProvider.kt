package com.grippo.services.backend.client

import com.grippo.core.error.provider.AppError
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
import io.ktor.util.AttributeKey
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CompletableDeferred
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
    private val refreshAttempts = mutableMapOf<TokenEntity, CompletableDeferred<TokenEntity?>>()

    private companion object {
        private const val REFRESH_WAIT_TIMEOUT_MS = 10_000L
        private val REQUEST_SESSION_KEY = AttributeKey<RequestSession>("GrippoRequestSession")
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
        val session = request.attributes.getOrNull(REQUEST_SESSION_KEY)
            ?: RequestSession(token).also {
                request.attributes.put(REQUEST_SESSION_KEY, it)
            }
        check(session.token?.id == token?.id) { "Authentication session changed" }
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
        val token = response.call.request.attributes.getOrNull(REQUEST_SESSION_KEY)?.token
            ?: return false
        val (completion, ownsRefresh) = refreshMutex.withLock {
            refreshAttempts.entries.removeAll { it.value.isCompleted }
            val current = refreshAttempts[token]
            if (current != null) {
                current to false
            } else {
                val next = CompletableDeferred<TokenEntity?>()
                refreshAttempts[token] = next
                next to true
            }
        }

        return if (ownsRefresh) {
            performTokenRefreshBlock(response = response, token = token, completion = completion)
        } else {
            waitForOngoingRefresh(completion)
        }
    }

    private suspend fun waitForOngoingRefresh(completion: CompletableDeferred<TokenEntity?>): Boolean {
        return withTimeout(REFRESH_WAIT_TIMEOUT_MS.milliseconds) {
            val rejected = completion.await() ?: return@withTimeout false
            val token = getCurrentToken() ?: return@withTimeout false
            check(token.id == rejected.id) { "Authentication session changed" }

            val access = token.access?.takeIf { it.isNotBlank() }
            access != null && access != rejected.access
        }
    }

    private suspend fun performTokenRefreshBlock(
        response: HttpResponse,
        token: TokenEntity,
        completion: CompletableDeferred<TokenEntity?>,
    ): Boolean {
        return try {
            val rejected = withTimeout(REFRESH_WAIT_TIMEOUT_MS.milliseconds) {
                refreshCurrentSession(response = response, token = token)
            }
            completion.complete(rejected)
            rejected != null
        } catch (e: Throwable) {
            completion.completeExceptionally(e)
            throw e
        }
    }

    /** Returns the rejected snapshot after storing its replacement, or null for an AuthProvider failure. */
    private suspend fun refreshCurrentSession(response: HttpResponse, token: TokenEntity): TokenEntity? {
        val current = getCurrentToken() ?: return null
        check(current.id == token.id) { "Authentication session changed" }
        if (current != token) {
            // Storage can advance before Ktor publishes the new provider version.
            return token.takeIf { !current.access.isNullOrBlank() && current.access != token.access }
        }
        check(!token.access.isNullOrBlank()) { "Access token is missing" }
        val refreshToken = token.refresh?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Refresh token is missing")

        return try {
            val refresh = retryWithBackoff {
                if (getCurrentToken() != token) {
                    throw RefreshSessionChangedException()
                }
                performTokenRefresh(
                    client = response.call.client,
                    token = token,
                    refreshToken = refreshToken,
                )
            }

            val newId = refresh.id ?: return null
            if (newId != token.id || newId.isBlank() ||
                refresh.accessToken.isNullOrBlank() || refresh.refreshToken.isNullOrBlank()
            ) {
                throw AppError.Network.IncompleteResponse()
            }

            val updated = tokenDao.updateIfCurrent(
                id = token.id,
                expectedAccess = token.access,
                expectedRefresh = token.refresh,
                access = refresh.accessToken,
                refresh = refresh.refreshToken,
            )
            if (updated == 0) {
                return null
            }
            token
        } catch (e: Throwable) {
            if (e is CancellationException || e !is Exception) {
                throw e
            }
            handleRefreshFailure(e = e, token = token)
        }
    }

    private suspend fun performTokenRefresh(
        client: HttpClient,
        token: TokenEntity,
        refreshToken: String
    ): TokenResponse {
        return client.submitForm {
            attributes.put(AuthCircuitBreaker, Unit)
            attributes.put(REQUEST_SESSION_KEY, RequestSession(token))
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

    private suspend fun handleRefreshFailure(e: Throwable, token: TokenEntity): Nothing {
        try {
            tokenDao.deleteIfCurrent(
                id = token.id,
                expectedAccess = token.access,
                expectedRefresh = token.refresh,
            )
        } catch (cleanupError: Throwable) {
            if (cleanupError is CancellationException || cleanupError !is Exception) {
                if (cleanupError !== e) {
                    cleanupError.addSuppressed(e)
                }
                throw cleanupError
            }
            if (cleanupError !== e) {
                e.addSuppressed(cleanupError)
            }
        }

        throw e
    }

    private suspend fun getCurrentToken(): TokenEntity? {
        return userActiveDao.get().firstOrNull()?.let { tokenDao.getById(it).firstOrNull() }
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
                if (e is CancellationException || e !is Exception) {
                    throw e
                }
                if (e is RefreshUnauthorizedException || e is RefreshSessionChangedException) {
                    throw e
                }
            }
            delay(currentDelay.milliseconds)
            currentDelay = (currentDelay * factor).toLong()
        }
        return block()
    }

    private class RefreshUnauthorizedException(message: String) : IllegalStateException(message)

    private class RefreshSessionChangedException : IllegalStateException("Authentication session changed")
}
