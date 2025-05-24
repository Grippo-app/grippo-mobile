package com.grippo.network.client

import com.grippo.database.dao.TokenDao
import com.grippo.database.entity.TokenEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.RefreshBody
import com.grippo.network.dto.TokenResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.AuthProvider
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.headers
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import io.ktor.http.auth.AuthScheme
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.http.path
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

internal class TokenProvider(
    private val tokenDao: TokenDao,
) : AuthProvider {

    private val refreshMutex = Mutex()
    private var lastRefreshError: Throwable? = null
    private var lastKnownInvalidAccessToken: String? = null

    private companion object {
        private const val REFRESH_WAIT_TIMEOUT_MS = 10_000L
    }

    @Deprecated("Please use sendWithoutRequest function instead", level = DeprecationLevel.ERROR)
    override val sendWithoutRequest: Boolean = true

    override fun sendWithoutRequest(request: HttpRequestBuilder): Boolean = true

    override fun isApplicable(auth: HttpAuthHeader): Boolean {
        return auth is HttpAuthHeader.Parameterized && auth.authScheme == AuthScheme.Bearer
    }

    override suspend fun addRequestHeaders(
        request: HttpRequestBuilder,
        authHeader: HttpAuthHeader?
    ) {
        val accessToken = tokenDao.get().firstOrNull()?.access?.let {
            "Authorization" to "Bearer $it"
        }

        request.headers {
            if (contains("Authorization")) remove("Authorization")
            accessToken?.let { append(it.first, it.second) }
        }
    }

    override suspend fun refreshToken(response: HttpResponse): Boolean {
        if (refreshMutex.isLocked) {
            AppLogger.network("🔄 [TokenProvider] Waiting for ongoing token refresh...")

            try {
                withTimeout(REFRESH_WAIT_TIMEOUT_MS) {
                    refreshMutex.withLock {
                        val currentTokens = tokenDao.get().firstOrNull()
                            ?: throw IllegalStateException("No tokens available")
                        if (currentTokens.access != lastKnownInvalidAccessToken) {
                            AppLogger.network("✅ [TokenProvider] Token already refreshed.")
                            return@withLock
                        }
                        lastRefreshError?.let { throw it }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                AppLogger.network("⏰ [TokenProvider] Timeout while waiting for token refresh.")
                throw e
            }

            AppLogger.network("✅ [TokenProvider] Existing token refresh completed.")
            return true
        }

        return try {
            withTimeout(REFRESH_WAIT_TIMEOUT_MS) {
                refreshMutex.withLock {
                    AppLogger.network("🔐 [TokenProvider] Starting token refresh...")
                    lastRefreshError = null

                    val tokens = tokenDao.get().firstOrNull() ?: return@withLock false

                    val accessToken = tokens.access.orEmpty()
                    val refreshToken = tokens.refresh.orEmpty()

                    if (accessToken.isBlank() || refreshToken.isBlank()) return@withLock false

                    lastKnownInvalidAccessToken = accessToken

                    try {
                        val refresh = performTokenRefresh(
                            client = response.call.client,
                            refreshToken = refreshToken
                        )

                        val id = AppLogger.checkOrLog(refresh.id) {
                            "TokenResponse.id is null"
                        } ?: return@withLock false

                        tokenDao.insert(
                            TokenEntity(
                                id = id,
                                access = refresh.accessToken,
                                refresh = refresh.refreshToken
                            )
                        )

                        AppLogger.network("✅ [TokenProvider] Token refresh successful.")
                        true
                    } catch (e: Throwable) {
                        handleRefreshFailure(e)
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            AppLogger.network("⏰ [TokenProvider] Timeout while trying to refresh token.")
            handleRefreshFailure(e)
        }
    }

    private suspend fun performTokenRefresh(
        client: HttpClient,
        refreshToken: String
    ): TokenResponse {
        return client.submitForm {
            url {
                method = HttpMethod.Post
                path("/auth/register")
                setBody(RefreshBody(refreshToken = refreshToken))
            }
        }.body()
    }

    private suspend fun handleRefreshFailure(e: Throwable): Nothing {
        lastRefreshError = e
        lastKnownInvalidAccessToken = null
        tokenDao.delete()
        AppLogger.network("❌ [TokenProvider] Token refresh failed: ${e.message}")
        throw e
    }
}