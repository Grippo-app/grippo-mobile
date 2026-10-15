# Data operation errors and cancellation

Read the affected operation and `ui-core/foundation/` error pipeline before changing failure behavior.
A Result wrapper succeeding does not establish a successful business outcome.

- Throwing UseCase commands unwrap required Feature results with `getOrThrow()`. They preserve
  their declared Boolean/Unit/domain return semantics. A false domain outcome is not an exception.
- Required payload validation, hydration (loading and storing the related data a record needs,
  for example a training's exercises and iterations) and cleanup complete before the operation
  reports success. Never turn missing required IDs or failed cleanup into fallback success.
- Result-returning operations keep failures in their declared Result. Throwing operations
  propagate the original failure. Do not catch-and-ignore failures here, and do not translate
  them into UI dialogs here.
- Preserve the caller's `CancellationException`. Do not store it as an ordinary recoverable
  Result failure. If a Result boundary needs explicit exception handling, rethrow cancellation
  before handling expected ordinary failures. Do not catch fatal Errors. Do not create generic
  recovery wrappers. Explicit cancellation checks follow the
  [canonical coroutine rules](../../validation-gates/references/forbidden-patterns.md#coroutines).
  `data-features/**` contains no `runCatching`; do not introduce one at a suspend boundary. The
  existing Result boundaries (`GrippoApi.request`, `LocalSettingsRepositoryImpl`) rethrow
  `CancellationException` and store other `Exception`s as `Result.failure`.
- UI unwraps required failures inside `safeLaunch`. The existing ErrorProvider owns presentation.
  Telemetry observes the complete operation without controlling recovery or outcome.

## Operation outcomes and cache reconciliation (canonical)

- The shared ResponseValidator ([backend transport](backend-client.md)) classifies HTTP/status
  and decoding failures. No auth-specific sanitation is applied. `GrippoApi` delivers them as
  `Result.failure(AppError.Network...)`. Pass existing transport failures through
  repository/Feature/UseCase boundaries without wrapping or reclassifying them.
- Mappers return null for objects that cannot be constructed. They do not create AppError, and
  they do not decide whether partial data is a successful operation.
- The Repository owns required payload completeness and cache consistency. Use the shared
  `AppError.Network.IncompleteResponse` when a successful response cannot hydrate required
  data. Confirmed empty collections are valid. A missing required collection or a dropped
  malformed row makes the response incomplete. Do not overwrite a complete cache with partial
  records.
- No offline write queue exists. A mutation without connectivity fails through the standard
  pipeline and the cache stays unchanged. A queue is a separate architecture decision, never an
  incidental addition.
- MUST update an API-backed cache only after confirmed server success, within the existing
  multi-row transaction boundary. Local drafts and preferences retain their Repository contract.
- Identify the required save, hydration and cleanup steps from the UseCase and its callers.
  Required failures MUST NOT become success. Optional readback may fail without changing
  server-write success only when the operation explicitly defines that partial-success policy
  in the use case's KDoc; without such a statement a failed readback is a failed operation.
- MUST reconcile only the requested owner/range (the cached rows the operation is allowed to
  replace, for example the trainings whose `createdAt` lies in the requested window). A missing
  required collection or any missing required server ID MUST prohibit range deletion. Confirmed
  valid empty data may clear the range. When all raw server IDs are known, malformed rows MUST
  retain their cached records and raw IDs; the existing partial-update policy may then reconcile
  other stale rows while reporting incomplete required hydration. A malformed payload is not an
  absent server record.
- MUST validate every record before reconciliation. Do not use a short-circuiting `any`/`all`
  when validation also writes the valid records. Preserve the operation's explicit atomic or
  partial-update policy, and protect incomplete raw server IDs from deletion.
- Domain rejection uses the existing domain contract. Add a shared error subtype only for a
  distinct meaning or required handling, not per endpoint or message. Update AppError,
  AppErrorState and ErrorProviderImpl together; keep one canonical rule here.
- UI `mapError` changes presentation only; logging and analytics receive the original failure.

Verified example (`data-features/trainings/.../TrainingRepositoryImpl.kt`, `getTrainings(start, end)`;
re-inspect before relying on it): the range is the cached trainings whose `createdAt` lies between
`start` and `end`. The repository first runs `provideTraining` on every response row (`count`, not
`any`/`all`); that step writes a training only when the training itself, all its exercises and all
their iterations map. If any row lacks an `id`, it returns `AppError.Network.IncompleteResponse()`
without touching the range. Otherwise it deletes the range except the raw server IDs
(`deleteByCreatedAtRangeExceptIds`), so a malformed row keeps its cached record, and it clears the
whole range only for a confirmed empty response. If fewer rows were written than received, it still
returns `IncompleteResponse` after that reconciliation.

Before completion, MUST trace the initial success, the alternate paths and the failure of each
required step through actual source. Report runtime behavior that was not manually exercised.
