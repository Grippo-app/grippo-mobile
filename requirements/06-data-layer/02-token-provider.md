# `TokenProvider` — Ktor Auth with refresh

`TokenProvider` implements Ktor's `AuthProvider`. It:

1. Injects `Authorization: Bearer <accessToken>` on every authenticated request.
2. On 401, attempts a **single** token refresh (mutex-guarded; concurrent requests wait).
3. Retries the refresh with exponential backoff (max 3 attempts).
4. On refresh failure with 401 (`RefreshUnauthorizedException`), deletes tokens — triggers auto-logout via the `RootViewModel`'s token observer.
5. Uses `AuthCircuitBreaker` attribute on the refresh request itself so Ktor doesn't recurse.

This is **load-bearing** infrastructure. Modify only with explicit review.

## Class signature

```kotlin
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
        val applicable = auth is HttpAuthHeader.Parameterized && auth.authScheme == AuthScheme.Bearer
        return applicable
    }

    override suspend fun addRequestHeaders(
        request: HttpRequestBuilder,
        authHeader: HttpAuthHeader?,
    ) {
        val token = getCurrentToken()
        val accessToken = token?.access
        request.headers {
            remove("Authorization")
            if (!accessToken.isNullOrBlank()) {
                append("Authorization", "Bearer $accessToken")
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
    // ... private helpers below
}
```

## `addRequestHeaders` — every request

```kotlin
override suspend fun addRequestHeaders(
    request: HttpRequestBuilder,
    authHeader: HttpAuthHeader?,
) {
    val token = getCurrentToken()
    val accessToken = token?.access
    request.headers {
        remove("Authorization")
        if (!accessToken.isNullOrBlank()) {
            append("Authorization", "Bearer $accessToken")
        }
    }
}
```

`getCurrentToken()` reads the active user's token from `TokenDao` via `UserActiveDao.get().firstOrNull()`. **Every request** re-reads the token — no caching, no `lateinit`. If the token was rotated, the next request picks up the new value.

## `refreshToken` — 401 handler

```kotlin
override suspend fun refreshToken(response: HttpResponse): Boolean {
    return if (refreshMutex.isLocked) {
        waitForOngoingRefresh()
    } else {
        performTokenRefreshBlock(response)
    }
}
```

Two paths:

- **Lock is held** (another request is already refreshing): `waitForOngoingRefresh()` blocks until the in-flight refresh completes, then returns its result. The caller's request can then retry with the new token.
- **Lock is free**: this caller is the leader; perform the refresh.

### `waitForOngoingRefresh`

```kotlin
private suspend fun waitForOngoingRefresh(): Boolean {
    return try {
        withTimeout(REFRESH_WAIT_TIMEOUT_MS) {
            refreshMutex.withLock {
                val token = getCurrentToken()
                if (token?.access != lastKnownInvalidAccessToken) {
                    return@withLock        // already handled by the leader
                }
                lastRefreshError?.let { throw it }
            }
        }
        true
    } catch (e: TimeoutCancellationException) {
        throw e
    }
}
```

Waits up to 10s for the leader to finish. Checks `lastKnownInvalidAccessToken` to see whether the leader has rotated the token. If `lastRefreshError != null`, the leader failed; rethrow its exception so this caller also fails.

### `performTokenRefreshBlock`

```kotlin
private suspend fun performTokenRefreshBlock(response: HttpResponse): Boolean {
    return try {
        withTimeout(REFRESH_WAIT_TIMEOUT_MS) {
            refreshMutex.withLock {
                lastRefreshError = null
                val (_, token) = getCurrentUserAndToken() ?: return@withLock false
                val accessToken = token.requireAccess()
                val refreshToken = token.requireRefresh()
                lastKnownInvalidAccessToken = accessToken

                try {
                    val refresh = retryWithBackoff {
                        performTokenRefresh(response.call.client, refreshToken)
                    }
                    val newId = AppLogger.Mapping.log(refresh.id) { "TokenResponse.id is null" }
                        ?: return@withLock false
                    tokenDao.insertOrUpdate(
                        TokenEntity(
                            id = newId,
                            access = refresh.accessToken,
                            refresh = refresh.refreshToken,
                        )
                    )
                    true
                } catch (e: Throwable) {
                    handleRefreshFailure(e)
                }
            }
        }
    } catch (e: TimeoutCancellationException) {
        handleRefreshFailure(e)
    }
}
```

The leader:

1. Resets `lastRefreshError`.
2. Reads the current refresh token.
3. Records `lastKnownInvalidAccessToken` so waiters can detect "already rotated".
4. Calls `performTokenRefresh` with `retryWithBackoff` (3 attempts, exponential).
5. On success, writes new tokens to `TokenDao`.
6. On failure, `handleRefreshFailure(e)` deletes tokens (triggering logout) and rethrows.

### `performTokenRefresh`

```kotlin
private suspend fun performTokenRefresh(
    client: HttpClient,
    refreshToken: String,
): TokenResponse {
    return client.submitForm {
        attributes.put(AuthCircuitBreaker, Unit)
        url {
            method = HttpMethod.Post
            path("/auth/refresh")
            setBody(RefreshBody(refreshToken = refreshToken))
        }
    }.let { response ->
        if (response.status == HttpStatusCode.Unauthorized) {
            throw RefreshUnauthorizedException("Refresh token was rejected by backend")
        }
        response.body()
    }
}
```

**`attributes.put(AuthCircuitBreaker, Unit)`** is critical: Ktor's `Auth` plugin sees this attribute on the request and **skips** the auth flow for it (no header injection, no 401 refresh attempt). Without this, a 401 on the refresh would trigger another refresh, then another — infinite recursion.

If the refresh itself returns 401, we know the refresh token is dead → `RefreshUnauthorizedException`.

### `handleRefreshFailure`

```kotlin
private suspend fun handleRefreshFailure(e: Throwable): Nothing {
    lastRefreshError = e
    lastKnownInvalidAccessToken = null

    when (e) {
        is CancellationException -> {
            // skip deletion to avoid side effects from coroutine cancellation
        }
        else -> {
            val userId = userActiveDao.get().firstOrNull()
            if (userId != null) {
                tokenDao.delete(userId)
            }
        }
    }
    throw e
}
```

- Sets `lastRefreshError` so any waiting `waitForOngoingRefresh` sees and rethrows it.
- **Deletes tokens** unless we hit a `CancellationException` (which is a coroutine teardown signal, not an auth failure).
- After tokens are deleted, `RootViewModel`'s `authorizationFeature.getToken()` observer sees `null` and calls `navigateTo(RootDirection.Login)` — auto-logout.

### `retryWithBackoff`

```kotlin
private suspend fun <T> retryWithBackoff(
    maxAttempts: Int = 3,
    initialDelay: Long = 500,
    factor: Double = 2.0,
    block: suspend () -> T,
): T {
    var currentDelay = initialDelay
    repeat(maxAttempts - 1) { attempt ->
        try {
            return block()
        } catch (e: Throwable) {
            if (e is RefreshUnauthorizedException) throw e
            // log + continue
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong()
    }
    return block()
}
```

- 3 attempts total: at 0ms, 500ms, 1500ms (delays double).
- **`RefreshUnauthorizedException` is fatal** — no retry; the refresh token is dead.
- **Other exceptions trigger retry** — transient network failures get another chance.

## `isApplicable`

```kotlin
override fun isApplicable(auth: HttpAuthHeader): Boolean =
    auth is HttpAuthHeader.Parameterized && auth.authScheme == AuthScheme.Bearer
```

Tells Ktor's Auth plugin: this provider handles `WWW-Authenticate: Bearer ...` challenges. If the server responds with a different challenge scheme (e.g. `Basic`), this provider is skipped.

## `sendWithoutRequest`

```kotlin
override fun sendWithoutRequest(request: HttpRequestBuilder): Boolean = true
```

Always attach `Authorization` on outgoing requests (don't wait for a 401 challenge). This is the right default for a Bearer token API — we always know we'll need it.

If some endpoints **shouldn't** be authenticated (login, refresh, public read), they should set `attributes.put(AuthCircuitBreaker, Unit)` on those specific requests. The reference repo handles this for the refresh endpoint inside `performTokenRefresh`.

## `RefreshUnauthorizedException`

```kotlin
private class RefreshUnauthorizedException(message: String) : IllegalStateException(message)
```

Internal to `TokenProvider`. Signals "refresh token is dead, don't retry, delete tokens". The `retryWithBackoff` checks for it and stops retrying immediately.

## Auto-logout integration

`RootViewModel`:

```kotlin
init {
    authorizationFeature.getToken()
        .onEach { token -> if (token == null) navigateTo(RootDirection.Login) }
        .safeLaunch()
}
```

`authorizationFeature.getToken()` is a `Flow<TokenEntity?>` from `TokenDao.observe(activeUserId)`. When `TokenProvider.handleRefreshFailure` calls `tokenDao.delete(userId)`, this Flow emits `null` → `RootViewModel` navigates to Login.

## Anti-patterns

- **Caching the access token in `TokenProvider` state.** Always read from DAO; rotations come from many places (login, refresh, manual reset).
- **Mutex inside the `request()` flow** instead of inside `refreshToken`. Double-locking; bad.
- **Skipping `AuthCircuitBreaker` on the refresh call.** Causes infinite recursion on 401.
- **Catching `RefreshUnauthorizedException` outside `handleRefreshFailure`.** That's the signal to give up; silencing it leaves the user in a broken auth state.
- **Retrying `RefreshUnauthorizedException`** — futile.
- **Increasing `maxAttempts` to 5+.** Wastes time and battery on a hopeless refresh.
- **Reducing `REFRESH_WAIT_TIMEOUT_MS` below 10s.** Concurrent requests need time for the leader to complete.
