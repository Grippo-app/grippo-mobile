package com.grippo.network.client

import com.grippo.database.dao.TokenDao
import com.grippo.database.entity.TokenEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.auth.RefreshBody
import com.grippo.network.dto.auth.TokenResponse
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
        val applicable = auth is HttpAuthHeader.Parameterized && auth.authScheme == AuthScheme.Bearer
        AppLogger.network("🔍 [TokenProvider] isApplicable=${applicable}, scheme=${(auth as? HttpAuthHeader.Parameterized)?.authScheme}")
        return applicable
    }

    override suspend fun addRequestHeaders(
        request: HttpRequestBuilder,
        authHeader: HttpAuthHeader?
    ) {
        AppLogger.network("📦 [TokenProvider] Adding Authorization header to request")
        val tokenEntity = tokenDao.get().firstOrNull()
        if (tokenEntity == null) {
            AppLogger.network("⚠️ [TokenProvider] No token found in DB when adding headers")
        } else {
            AppLogger.network("✅ [TokenProvider] Access token retrieved for header: ${tokenEntity.access?.take(15)}...")
        }

        val accessToken = tokenEntity?.access?.let {
            "Authorization" to "Bearer $it"
        }

        request.headers {
            if (contains("Authorization")) {
                AppLogger.network("🧹 [TokenProvider] Removing existing Authorization header")
                remove("Authorization")
            }
            accessToken?.let {
                AppLogger.network("🔐 [TokenProvider] Setting Authorization: ${it.second.take(25)}...")
                append(it.first, it.second)
            } ?: AppLogger.network("❌ [TokenProvider] Access token was null — nothing added")
        }
    }

    override suspend fun refreshToken(response: HttpResponse): Boolean {
        AppLogger.network("🌀 [TokenProvider] refreshToken() called")

        if (refreshMutex.isLocked) {
            AppLogger.network("🔄 [TokenProvider] Waiting for ongoing token refresh...")

            try {
                withTimeout(REFRESH_WAIT_TIMEOUT_MS) {
                    refreshMutex.withLock {
                        AppLogger.network("🔄 [TokenProvider] Acquired lock after waiting")
                        val currentTokens = tokenDao.get().firstOrNull()
                        if (currentTokens == null) {
                            AppLogger.network("❌ [TokenProvider] No tokens available during wait — throwing")
                            throw IllegalStateException("No tokens available")
                        }
                        AppLogger.network("🔍 [TokenProvider] Comparing current=${currentTokens.access?.take(10)}... vs invalid=${lastKnownInvalidAccessToken?.take(10)}...")
                        if (currentTokens.access != lastKnownInvalidAccessToken) {
                            AppLogger.network("✅ [TokenProvider] Token already refreshed by another request")
                            return@withLock
                        }

                        if (lastRefreshError != null) {
                            AppLogger.network("❌ [TokenProvider] Last refresh had error: ${lastRefreshError?.message}")
                            throw lastRefreshError as Throwable
                        }

                        AppLogger.network("⚠️ [TokenProvider] Token still invalid and no error — strange fallback")
                    }
                }
            } catch (e: TimeoutCancellationException) {
                AppLogger.network("⏰ [TokenProvider] Timeout while waiting for token refresh")
                throw e
            }

            AppLogger.network("✅ [TokenProvider] Existing token refresh completed after wait")
            return true
        }

        AppLogger.network("🔐 [TokenProvider] Entering exclusive refresh block...")

        return try {
            withTimeout(REFRESH_WAIT_TIMEOUT_MS) {
                refreshMutex.withLock {
                    AppLogger.network("🔐 [TokenProvider] Acquired lock to refresh")
                    lastRefreshError = null

                    val tokens = tokenDao.get().firstOrNull()
                    if (tokens == null) {
                        AppLogger.network("❌ [TokenProvider] No tokens available — aborting refresh")
                        return@withLock false
                    }

                    val accessToken = tokens.access.orEmpty()
                    val refreshToken = tokens.refresh.orEmpty()

                    AppLogger.network("🔍 [TokenProvider] Tokens from DB: access=${accessToken.take(10)}..., refresh=${refreshToken.take(10)}...")

                    if (accessToken.isBlank() || refreshToken.isBlank()) {
                        AppLogger.network("❌ [TokenProvider] access or refresh token is blank — aborting")
                        return@withLock false
                    }

                    lastKnownInvalidAccessToken = accessToken

                    try {
                        AppLogger.network("📡 [TokenProvider] Performing actual refresh call...")
                        val refresh = performTokenRefresh(
                            client = response.call.client,
                            refreshToken = refreshToken
                        )

                        val id = AppLogger.checkOrLog(refresh.id) {
                            "TokenResponse.id is null"
                        }

                        if (id == null) {
                            AppLogger.network("❌ [TokenProvider] Received null ID from refresh — aborting")
                            return@withLock false
                        }

                        AppLogger.network("💾 [TokenProvider] Storing new tokens to DB")
                        tokenDao.insert(
                            TokenEntity(
                                id = id,
                                access = refresh.accessToken,
                                refresh = refresh.refreshToken
                            )
                        )

                        AppLogger.network("✅ [TokenProvider] Token refresh successful!")
                        true
                    } catch (e: Throwable) {
                        AppLogger.network("🔥 [TokenProvider] Token refresh failed in try block: ${e.message}")
                        handleRefreshFailure(e)
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            AppLogger.network("⏰ [TokenProvider] Timeout during refresh process")
            handleRefreshFailure(e)
        }
    }

    private suspend fun performTokenRefresh(
        client: HttpClient,
        refreshToken: String
    ): TokenResponse {
        AppLogger.network("🌐 [TokenProvider] Sending refresh request to /auth/refresh")
        return client.submitForm {
            url {
                method = HttpMethod.Post
                path("/auth/refresh")
                setBody(RefreshBody(refreshToken = refreshToken))
            }
        }.also {
            AppLogger.network("📥 [TokenProvider] Received refresh response with status=${it.status.value}")
        }.body()
    }

    private suspend fun handleRefreshFailure(e: Throwable): Nothing {
        AppLogger.network("🧨 [TokenProvider] Handling refresh failure: ${e.message}")
        lastRefreshError = e
        lastKnownInvalidAccessToken = null
        AppLogger.network("🧼 [TokenProvider] Deleting tokens from DB after failure")
        tokenDao.delete()
        AppLogger.network("❌ [TokenProvider] Token refresh failed — throwing up")
        throw e
    }
}