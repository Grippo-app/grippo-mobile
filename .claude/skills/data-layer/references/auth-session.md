# Authentication session ownership

Inspect `data-features/authorization/`, `data-services/backend/.../client/TokenProvider.kt`,
credential wrappers, token DAO and the actual Login/Register UseCases before changing session behavior.

- Provider credentials and backend access/refresh tokens are different contracts. Preserve the
  current exchange and storage ownership; UI consumes authorized credential adapters only where
  the architecture already allows them.
- Preserve complete operation outcomes: authentication, profile detection/hydration and required
  cleanup. Do not infer login success from only one Result or change token deletion conditions.
- Refresh synchronization/retry/circuit-breaker is load-bearing network core; edit only within
  authorization covering that behavior. Preserve caller cancellation and distinguish timeout policy
  from ordinary cancellation by reading actual source.
- Credentials remain excluded from analytics and StateKeeper. Input status analytics never carry credential values.
- Logout clearing must follow the actual session/domain contract; identify retained local drafts
  and owner boundaries before changing cleanup.
