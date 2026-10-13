# Core toolkit ownership

Inspect the affected `toolkit/<module>/` implementation and direct consumers.

- `context` supplies NativeContext platform access. `http-client` owns engines/response validation;
  backend authentication stays in its existing service owner. Serialization owns the shared Json policy.
- `logger` owns sinks/categories. AppLogger currently dispatches in all build modes;
  do not assume its onDebug method checks the build variant.
- `date-utils` owns timezone/date representation. Persist UTC-normalized values and review round-trip
  conversions; formatting is presentation, a clock-based business decision belongs to its operation owner.
- Analytics/crash-reporting/messaging API modules expose typed consumer/bootstrap contracts;
  cores own app-lifetime state/policy. SDK transport is app-owned. Do not add data/domain dependencies.
- Preserve existing dependency exceptions rather than extending them by analogy. Pure helpers do
  not acquire DI/lifecycle state to work around a consumer bug.

Review callback lifetime, owner shutdown, cancellation, shared state and platform behavior in source.
