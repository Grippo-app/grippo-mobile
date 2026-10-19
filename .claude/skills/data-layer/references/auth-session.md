# Authentication session ownership

Inspect `data-features/authorization/`, `data-services/backend/.../client/TokenProvider.kt`,
the credential adapters (`data-services/google-auth`, `data-services/apple-auth`), `TokenDao`,
`UserActiveDao` and the actual Login/Register/Logout UseCases before changing session behavior.

## Credential adapters (canonical)

`GoogleAuthUiProvider.signIn` and `AppleAuthUiProvider.signIn` follow the
[throwing suspend contract](operation-outcomes.md#throwing-suspend-contracts-canonical).
Inspect both platform implementations and all callers when changing either adapter.

- Keep each adapter's existing failure classification at the platform boundary. A translation
  uses the existing typed Google/Apple auth errors and retains the original exception as `cause`.
  Preserve existing typed errors, coroutine `CancellationException` and fatal `Error` unchanged. A provider's
  user-cancel result retains its existing typed cancellation meaning.
- Native callback bridges use `suspendCancellableCoroutine`: complete at most once, resume
  failures exceptionally and retain main-thread ownership and cancellation cleanup. A standard
  `Result` used only by `Continuation.resumeWith` is callback plumbing, not an operation outcome.
- Required configuration lookup, credential acquisition, token exchange/decoding and sign-out
  failures propagate. `isSupported` distinguishes an absent primary configuration key from
  an invalid supplied value. Supplied blank, wrong-type or invalid values fail in `getUiProvider`
  or `signIn`, inside the existing UI operation; do not hide them as unsupported or throw them
  while constructing a ViewModel. Missing optional account display fields keep their declared defaults;
  malformed required credentials do not become an account with fallback values.
- Preserve the existing provider strategy and its explicit fallback eligibility. Retry/fallback
  belongs to that SDK boundary; it is not a recovery wrapper for callers or a successful account
  after all strategies fail. Credentials remain inside the complete login operation's existing
  UI error/analytics boundary.

## TokenProvider (verified shape)

`@Single internal class TokenProvider(tokenDao, userActiveDao) : AuthProvider`; bearer scheme;
`sendWithoutRequest = true`, so the header is attached on the first attempt.

- `addRequestHeaders` reads the current token on every request through
  `userActiveDao.get().firstOrNull()?.let { tokenDao.getById(it).firstOrNull() }`, removes any
  existing `Authorization` header and appends `Bearer <access>` only when the token is non-blank.
- The first header application pins `RequestSession` in Ktor request attributes, including an
  explicit anonymous snapshot. Refresh and header replay stay with that owner; a changed owner
  fails instead of rebinding the request to another account. This check also covers Ktor's
  provider-version shortcut, which can replay without calling `refreshToken`. Internal refresh
  retries retain the original snapshot. Attributes stay local, outside wire payloads and logging.
- `refreshToken(response)` uses a short `refreshMutex.withLock` section to prune completed
  entries and select an unparented completion from `refreshAttempts`, keyed by the captured
  `TokenEntity`, or publish a new one. A stale or different session cannot replace another
  snapshot's incomplete attempt and allow duplicate rotating refreshes. Only the new
  attempt's owner runs `performTokenRefreshBlock` under the 10 s timeout, in its caller's coroutine.
  Completion is published after the timeout block returns: the rejected `TokenEntity` after a
  valid replacement is stored, `null` for the existing AuthProvider failure outcomes below, or
  the original throwable via `completeExceptionally` before rethrowing it. Waiters capture that
  attempt under the mutex and await it within 10 s, without cancelling the shared completion.
  Only after a successful completion do they read the current token; `true` requires the same owner and a changed
  nonblank access token. Do not use `isLocked`/`tryLock` followed by a separate waiter-registration
  step, or mutable last-error/access markers shared across attempts: a later owner can otherwise
  erase the result before a delayed waiter consumes it. No new scope or background refresh job.
  A same-owner replacement already stored before Ktor publishes its new provider version needs
  no further refresh; accept only its changed nonblank access token.
- The refresh request is its own `client.submitForm` to `/auth/refresh` with
  `attributes.put(AuthCircuitBreaker, Unit)`, so Ktor's Auth plugin does not refresh the refresh.
  It runs through `retryWithBackoff(maxAttempts = 3, initialDelay = 500 ms, factor = 2.0)`, which
  rethrows `CancellationException`, non-`Exception` throwables (including `Error`) and the private
  `RefreshUnauthorizedException : IllegalStateException` and `RefreshSessionChangedException`
  immediately and retries any other failure. A replaced session cannot become current again by waiting.
- The shared validator classifies the refresh response before TokenProvider's own status check
  ([backend transport](backend-client.md)): a 4xx arrives as `AppError.Network.Expected` and is
  retried; `RefreshUnauthorizedException` is thrown only when an unauthorized response reaches
  TokenProvider unvalidated.
- Refresh writes use `TokenDao.updateIfCurrent`; failure cleanup uses `deleteIfCurrent`. Both
  SQL mutations atomically match the captured ID/access/refresh and active owner. A late refresh
  cannot recreate a logged-out row, overwrite a new login or delete its credentials. A zero-row
  update is an AuthProvider failure, never a stored refresh. Do not replace these checks with a
  read followed by an unconditional write/delete.
- Ordinary refresh failures conditionally delete the captured failing snapshot, then rethrow.
  Do not re-read the active account to choose a cleanup owner. The captured attempt keeps the
  original failure available to its waiters. An ordinary cleanup failure is suppressed on the original refresh error;
  cleanup cancellation/fatal errors remain primary with the original failure suppressed.
  The 10 s `TimeoutCancellationException` is a `CancellationException`; owner timeout, caller
  cancellation and fatal errors rethrow without initiating token deletion. This does not roll
  back cleanup already performed for an earlier ordinary refresh failure. Token-loss navigation
  belongs to the [Root and ProfileCreation observers](../../ui-feature/references/navigation.md#direction--navigation-translation).
- A successful refresh updates the matching captured token row;
  a refresh response without `id` retains the AuthProvider `false` failure outcome (no new token,
  no deletion). A supplied ID must match the captured session, and the ID/access/refresh values
  must be nonblank before storage. This infrastructure Boolean does not represent business success.

## Rules

- Provider credentials and backend access/refresh tokens are different contracts. Preserve the
  current exchange and storage ownership; UI consumes the credential adapters only where the
  architecture already allows them (`:ui-screen-features:authorization`).
- Preserve complete operation outcomes: authentication, profile detection/hydration and required
  cleanup. Do not infer login success from one returned request or change token deletion conditions.
  Login/Register/provider exchange reject blank required ID/access/refresh values before either
  token storage or active-session publication; validation belongs to the Repository owner.
- Refresh synchronization/retry/circuit-breaker is load-bearing network core; edit it only when the
  authorized scope covers that behavior. Preserve caller cancellation and keep the timeout policy
  (no deletion) distinct from a rejected refresh (deletion).
- Credentials remain excluded from analytics and StateKeeper. Input status analytics never carry
  credential values.
- Logout clearing follows the actual session/domain contract; identify retained local drafts and
  owner boundaries before changing cleanup.
