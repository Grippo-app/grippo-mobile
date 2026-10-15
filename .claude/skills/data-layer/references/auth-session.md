# Authentication session ownership

Inspect `data-features/authorization/`, `data-services/backend/.../client/TokenProvider.kt`,
the credential adapters (`data-services/google-auth`, `data-services/apple-auth`), `TokenDao`,
`UserActiveDao` and the actual Login/Register/Logout UseCases before changing session behavior.

## TokenProvider (verified shape)

`@Single internal class TokenProvider(tokenDao, userActiveDao) : AuthProvider`; bearer scheme;
`sendWithoutRequest = true`, so the header is attached on the first attempt.

- `addRequestHeaders` reads the current token on every request through
  `userActiveDao.get().firstOrNull()?.let { tokenDao.getById(it).firstOrNull() }`, removes any
  existing `Authorization` header and appends `Bearer <access>` only when the token is non-blank.
- `refreshToken(response)`: if `refreshMutex` is already locked the call enters
  `waitForOngoingRefresh()` (`withTimeout(10_000 ms)` around the lock; returns `true` when the
  stored access token changed, otherwise rethrows the recorded `lastRefreshError`); otherwise
  `performTokenRefreshBlock` runs `withTimeout(10_000 ms) { refreshMutex.withLock { ... } }`.
- The refresh request is its own `client.submitForm` to `/auth/refresh` with
  `attributes.put(AuthCircuitBreaker, Unit)`, so Ktor's Auth plugin does not refresh the refresh.
  It runs through `retryWithBackoff(maxAttempts = 3, initialDelay = 500 ms, factor = 2.0)`, which
  rethrows `CancellationException`, `Error` and the private
  `RefreshUnauthorizedException : IllegalStateException` immediately and retries any other failure.
- The shared validator classifies the refresh response before TokenProvider's own status check
  ([backend transport](backend-client.md)): a 4xx arrives as `AppError.Network.Expected` and is
  retried; `RefreshUnauthorizedException` is thrown only when an unauthorized response reaches
  TokenProvider unvalidated.
- `handleRefreshFailure(e)` records the error, clears the invalid-token marker, deletes the stored
  tokens with `tokenDao.delete(userId)` unless `e` is a `CancellationException`, then rethrows.
  The 10 s `TimeoutCancellationException` is a `CancellationException`, so a timeout rethrows
  WITHOUT deleting tokens, and so does caller cancellation. Once the token row is gone, the
  `RootViewModel` token observer (`authorizationFeature.getToken()` -> `RootDirection.Login` on
  `null`) navigates to login.
- A successful refresh stores `TokenEntity(id, access, refresh)` through `tokenDao.insertOrUpdate`;
  a refresh response without `id` returns `false` (no new token, no deletion).

## Rules

- Provider credentials and backend access/refresh tokens are different contracts. Preserve the
  current exchange and storage ownership; UI consumes the credential adapters only where the
  architecture already allows them (`:ui-screen-features:authorization`).
- Preserve complete operation outcomes: authentication, profile detection/hydration and required
  cleanup. Do not infer login success from one Result or change token deletion conditions.
- Refresh synchronization/retry/circuit-breaker is load-bearing network core; edit it only when the
  authorized scope covers that behavior. Preserve caller cancellation and keep the timeout policy
  (no deletion) distinct from a rejected refresh (deletion).
- Credentials remain excluded from analytics and StateKeeper. Input status analytics never carry
  credential values.
- Logout clearing follows the actual session/domain contract; identify retained local drafts and
  owner boundaries before changing cleanup.
