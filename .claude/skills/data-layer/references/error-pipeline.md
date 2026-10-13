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

## Error ownership (MUST)

- HTTP/status and decoding failures are classified by the shared ResponseValidator;
  no auth-specific sanitation is applied. Pass existing transport failures through
  repository/Feature/UseCase boundaries without wrapping or reclassifying them.
- Mappers return null for objects that cannot be constructed; they do not create AppError
  or decide whether partial data is a successful operation.
- Repository owns required payload completeness and cache consistency. Use the shared
  AppError.Network.IncompleteResponse when a successful response cannot hydrate required
  data. Confirmed empty collections are valid; missing required collections or dropped
  malformed rows are incomplete. Do not overwrite complete cache with partial records.
- Validate every record before reconciliation; do not use short-circuit any/all when
  validation also writes valid records. Preserve the operation's explicit atomic or
  partial-update policy, and protect incomplete raw server IDs from deletion.
- Domain rejection uses the existing domain contract. Add a shared error subtype only
  for a distinct meaning or required handling, not per endpoint/message. Update AppError,
  AppErrorState and ErrorProviderImpl together; keep one canonical rule here.
- UI mapError changes presentation only; logging/analytics receive the original failure.

Before completion trace initial success, alternate paths and failure of each required step through
actual source. Report runtime behavior that was not manually exercised.
