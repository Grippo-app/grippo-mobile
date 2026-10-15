# Core toolkit ownership

Inspect the affected `toolkit/<module>/` implementation and direct consumers.

- `context` supplies NativeContext platform access. `http-client` owns the engines (OkHttp on
  Android, Darwin on iOS) and the response validator described in
  [backend transport](../../data-layer/references/backend-client.md); backend authentication stays
  in its service owner. `serialization` owns the shared `Json` (`SerializationModule`); its effect
  on request bodies is described in [DTO/API](../../data-layer/references/dtos-and-api.md#omitted-versus-explicit-null).
- `logger` owns sinks/categories. AppLogger currently dispatches in all build modes;
  do not assume its onDebug method checks the build variant.
- `date-utils` owns timezone/date representation. Persist UTC-normalized values and review round-trip
  conversions; formatting is presentation, a clock-based business decision belongs to its operation owner.
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
