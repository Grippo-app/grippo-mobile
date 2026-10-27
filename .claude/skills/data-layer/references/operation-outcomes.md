# Data operation errors and cancellation

Read the affected operation and `ui-core/foundation/` error pipeline before changing failure behavior.
A successful return establishes the complete operation outcome, not just an accepted server write.

## Throwing suspend contracts (canonical)

- `GrippoApi`, platform credential adapters, Repository, Feature and command UseCase methods MUST be ordinary `suspend`
  functions returning their success value `T` or `Unit`. They MUST NOT return `kotlin.Result`
  or convert exceptions into success values. Observations keep `Flow<Domain>`; pure calculators
  keep their direct return types.
- Call required steps directly and sequentially. Exceptions from requests, decoding, mapping,
  storage and required follow-up work propagate unchanged to the caller. MUST NOT add local
  `try/catch`, `runCatching`, Result composition or generic execution/recovery wrappers in this
  chain. `try/finally` remains valid for existing resource/Job cleanup. Infrastructure-owned
  transport classification, auth refresh/retry and
  [platform credential error translation](auth-session.md#credential-adapters-canonical) keep
  their separately documented boundaries; do not copy them into business operations.
- Required payload validation, hydration (loading and storing the related data a record needs,
  for example a training's exercises and iterations) and cleanup complete before success.
  A save returns its required non-null ID only after those steps. Never turn a missing ID,
  malformed required payload, failed cleanup or unavailable local mutation prerequisite into
  `null`, `false`, an empty collection or a silent no-op.
- Meaningful absence and domain rejection retain their explicit Boolean/nullable/domain contract.
  Distinguish an absent profile or goal from a malformed non-null response before mapping. A
  mapper returning null does not establish that the domain entity is absent. A missing local
  prerequisite for a requested mutation fails before the remote write, using an existing error
  meaning; it is not a malformed server response. When hydration requires a cached owner through
  a foreign key, resolve that owner before the remote mutation; an active-session ID alone does
  not establish its presence. Consumers MUST inspect Boolean/nullable
  outcomes when their operation requires that entity to exist. A required profile refresh
  returning false fails the composing operation; presence checks for routing keep that false.
- Preserve the caller's `CancellationException` and fatal `Error` through the chain. Direct
  suspend calls propagate them naturally. Explicit cancellation checks follow the
  [canonical coroutine rules](../../validation-gates/references/forbidden-patterns.md#coroutines).
- UI calls commands directly inside its existing `safeLaunch`, preserving any existing
  `analytics.trackOperation` around the complete operation. The existing ErrorProvider owns
  presentation. Credential commands run directly inside that same error/analytics block.
- A failed operation after an accepted server mutation does not imply rollback or safe replay.
  Preserve existing transaction boundaries and add no automatic retry or fallback.

## Operation outcomes and cache reconciliation (canonical)

- The shared ResponseValidator ([backend transport](backend-client.md)) classifies HTTP/status
  and decoding failures. No auth-specific sanitation is applied. `GrippoApi` propagates them
  as exceptions. Pass existing transport failures through
  repository/Feature/UseCase boundaries without wrapping or reclassifying them.
- Mappers return null for objects that cannot be constructed. They do not create AppError, and
  they do not decide whether partial data is a successful operation.
- The Repository owns required payload completeness and cache consistency. Use the shared
  `AppError.Network.IncompleteResponse` when a successful response cannot hydrate required
  data. Confirmed empty collections are valid. A missing required collection or a dropped
  malformed row makes the response incomplete. Do not overwrite a complete cache with partial
  records. Validate received objects' intrinsic enum, component and consumed date mappings
  before writing. A non-null entity is insufficient when those mappings reject its value.
  Exception: the equipment and muscle catalogs (`EquipmentRepositoryImpl.getEquipments`,
  `MuscleRepositoryImpl.getMuscles`) store group and item rows whose `type` key this app
  version does not know, so Splash's catalog refresh keeps succeeding when the
  server adds a type, and exercise-example snapshots written after that refresh find their
  foreign-key parents. The entity-to-domain mappers hide those rows; an unknown group type
  hides the whole group from the catalog projection, including its items of known types. Missing fields,
  duplicate IDs and wrong parents still make that response incomplete. For equipment, store
  and TestFlight builds without this exception and the user-visible effects of hiding are
  described in the [equipment integration map](../../add-equipment/references/integration-map.md#mobile-touchpoints).
  For muscles: a hidden muscle drops out of exercise-example bundles, so an example's load is
  shared among its visible muscles; the user cannot select or exclude it; for a muscle whose
  own type is unknown the example filters also ignore an exclusion made from a newer build,
  while a known-type muscle hidden with its unknown group keeps its exclusion; and saving the
  muscle selection (`ProfileMusclesViewModel.onApply`) removes a hidden muscle's exclusion
  because the backend replaces the whole set, an accepted rollout risk. Builds that reject an unknown muscle type keep retrying
  Splash, and builds that refresh catalogs only before sign-in fail a signed-in user's
  exercise-example refresh on the missing bundle parent, so a new muscle type needs the same
  compatibility decision as a new equipment type.
  Splash is the only catalog refresh. A process restored straight into Home skips it, so
  after the server adds a muscle or equipment row an exercise-example refresh fails on the
  missing foreign-key parent (the generic error dialog and a crash report; each Retry on Home's
  popup repeats it, any Completed screen in that process shows it once) until a cold start passes
  Splash again. This is an accepted risk:
  the owner adds catalog reloads where they are needed.
  Required children must belong to the enclosing parent; primary IDs must be unique across
  each table's snapshot. Row-count equality alone does not establish completeness.
  Observations that depend on separately refreshed catalogs retain their explicit availability
  policy ([available mapped details](repositories.md#minimal-domain-models)); validate the
  received records without manufacturing related catalog rows or fabricated metric totals.
- No offline write queue exists. A mutation without connectivity fails through the standard
  pipeline and the cache stays unchanged. A queue is a separate architecture decision, never an
  incidental addition.
- MUST update an API-backed cache only after confirmed server success, within the existing
  multi-row transaction boundary. Local drafts and preferences retain their Repository contract.
- Identify the required save, hydration and cleanup steps from the UseCase and its callers.
  Every step in the declared operation propagates failure; a failed readback is a failed
  operation. A code comment does not authorize suppressing an exception or omitting a required result.
  An intentionally separate follow-up has its own explicit operation contract and error owner;
  it is not an ignored call appended to an otherwise successful command.
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
  distinct meaning or required handling, not per endpoint or message. Error-type synchronization
  and presentation mapping follow the
  [UI error pipeline](../../ui-feature/references/error-pipeline.md#the-error-triad-ui-corestate-work).

Verified example (`data-features/trainings/.../TrainingRepositoryImpl.kt`, `getTrainings(start, end)`;
re-inspect before relying on it): the range is the cached trainings whose `createdAt` lies between
`start` and `end`. The repository identifies duplicate primary IDs across the response and skips
implicated records, then calls `provideTraining` for every eligible row through `count` (not a
short-circuiting `any`/`all`). A row is written only when its owned cache records map, required
children belong to their parents, primary IDs are unique and consumed dates parse. Independently
loaded example catalogs retain their observation availability policy. Missing raw training IDs
prohibit range deletion. With all raw IDs known, reconciliation preserves malformed rows' cached
records and deletes only stale IDs in the requested range; confirmed empty data clears that range.
Any skipped/incomplete row makes the operation throw `IncompleteResponse` after that allowed
reconciliation. Exceptions from parsing or storage propagate immediately and prevent subsequent
reconciliation; already completed row transactions are not rolled back as one global transaction.

Before completion, MUST trace the initial success, the alternate paths and the failure of each
required step through actual source. Report runtime behavior that was not manually exercised.
