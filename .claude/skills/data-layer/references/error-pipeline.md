# Data operation errors and cancellation

Read the affected operation and `ui-core/foundation/` error pipeline before changing failure behavior.
A Result wrapper succeeding does not establish a successful business outcome.

- Throwing UseCase commands unwrap required Feature results with `getOrThrow()`; preserve their
  declared Boolean/Unit/domain return semantics. A false domain outcome is not an exception.
- Required payload validation, hydration and cleanup must complete before reporting success.
  Never turn missing required IDs or failed cleanup into fallback success.
- Result-returning operations keep failures in their declared Result; throwing operations propagate
  the original failure. Do not catch-and-ignore failures or translate them into UI dialogs here.
- Preserve caller `CancellationException`; do not store it as an ordinary recoverable Result failure.
  If a Result boundary needs explicit exception handling, rethrow cancellation before handling
  expected ordinary failures. Do not catch fatal Errors, create generic recovery wrappers or
  add manual `ensureActive()` calls. Existing bare suspend `runCatching` is an implementation
  deviation, not an endorsed recipe; repair only within authorized scope.
- UI unwraps required failures inside `safeLaunch`; the existing ErrorProvider owns presentation.
  Telemetry must observe the complete operation without controlling recovery or outcome.

Before completion trace initial success, alternate paths and failure of each required step through
actual source. Report runtime behavior that was not manually exercised.
