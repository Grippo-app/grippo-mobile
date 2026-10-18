# Core toolkit ownership

Inspect the affected `toolkit/<module>/` implementation and direct consumers.

- `context` supplies NativeContext platform access. `http-client` owns the engines (OkHttp on
  Android, Darwin on iOS) and the response validator described in
  [backend transport](../../data-layer/references/backend-client.md); backend authentication stays
  in its service owner. `serialization` owns the shared `Json` (`SerializationModule`); its effect
  on request bodies is described in [DTO/API](../../data-layer/references/dtos-and-api.md#omitted-versus-explicit-null).
- `logger` owns sinks/categories. AppLogger currently dispatches in all build modes;
  do not assume its onDebug method checks the build variant.
- `localization` owns locale access (`AppLocale`).
- `number-utils` owns pure numeric representation with explicit separator inputs.
  Inspect `NumberFormatting.kt` and its callers before adding another numeric helper.
  Keep stateless transformations as top-level functions, independent of Compose,
  resources, locale lookup, DI and domain types. Unit, nullable-data and
  business-validation policies stay with their consumers.
- `date-utils` owns timezone/date representation. Persist UTC-normalized values and review round-trip
  conversions; formatting is presentation, a clock-based business decision belongs to its operation owner.
  A new clock Flow/timer needs a concrete consumer that becomes stale while its
  source data stays unchanged. Prefer existing source/lifecycle emissions when
  they satisfy that contract; otherwise choose the required boundary (for example
  local midnight), keep it cancellable and avoid arbitrary polling. Before
  removing a clock source, inspect retained screens/dialogs and fixed date ranges
  whose equality filters can suppress an otherwise necessary refresh.
- Analytics/crash-reporting/messaging API modules expose typed consumer/bootstrap contracts;
  cores own app-lifetime state/policy. SDK transport is app-owned. Do not add data/domain dependencies.
- Documented upward dependency exceptions ([module dependency rules](../../../../CLAUDE.md#module-dependency-rules)),
  not to be extended by analogy: `toolkit/http-client` -> `:ui-core:error:error-provider` (the
  validator throws `AppError`), and `toolkit/date-utils` -> `:design-system:core` +
  `:design-system:resources:provider` + Compose foundation (`DateCompose.kt`, locale-aware formats).
  Because `:data-features:feature-api` depends on `toolkit/date-utils`, every domain consumer
  transitively pulls design-system and Compose; add no further UI edges there.
  `:data-services:database` declares `:ui-core:error:error-provider` without using it; do not
  start using it. Pure helpers do not acquire DI/lifecycle state to work around a consumer bug.

Review callback lifetime, owner shutdown, cancellation, shared state and platform behavior in source.
