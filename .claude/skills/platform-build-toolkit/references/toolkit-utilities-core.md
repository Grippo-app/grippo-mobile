# Core toolkit ownership

Inspect the affected `toolkit/<module>/` implementation and direct consumers.

- `context` supplies NativeContext platform access. `http-client` owns engines/response validation;
  backend authentication stays in its existing service owner. Serialization owns the shared Json policy.
- `logger` owns sinks/categories. Messages must exclude secret values, prefixes and hashes in all
  build modes. Auth transport logs contain only method/path/status; no headers/bodies.
- `date-utils` owns timezone/date representation. Persist UTC-normalized values and review round-trip
  conversions; formatting is presentation, a clock-based business decision belongs to its operation owner.
- Analytics/crash-reporting/messaging API modules expose typed consumer/bootstrap contracts;
  cores own app-lifetime state/policy. SDK transport is app-owned. Do not add data/domain dependencies.
- Preserve existing dependency exceptions rather than extending them by analogy. Pure helpers do
  not acquire DI/lifecycle state to work around a consumer bug.

Review callback lifetime, owner shutdown, cancellation, shared state and platform behavior in source.
