# Null handling and parent identity

```kotlin
val mappedId = AppLogger.Mapping.log(id) { "Response.id is null" } ?: return null
```

This is a required-field guard pattern, not a complete DTO example. Messages name the missing
field/type. Use `mapNotNull` for a collection only when dropping malformed
rows is the agreed behavior. Consumers MUST apply the canonical
[hydration/completeness rules](../../data-layer/references/operation-outcomes.md); mapper nullability
does not establish a successful partial business outcome.

- Required fields guard once. Nullable targets remain nullable. Aliases may coalesce inside one
  guard only when the authoritative contract defines both names and their precedence.
- A missing collection is distinct from a confirmed empty collection. Coalesce only where the
  domain/storage contract permits that interpretation; do not trigger destructive reconciliation.
- Map required entity relations and dictionary/date parsing failures explicitly. Do not force-unwrap,
  choose arbitrary defaults or catch cancellation/fatal Errors while parsing.
- If the wire payload carries the parent FK, use and validate it. A parent ID supplied by the URL
  or existing session may be an explicit parameter only after the contract establishes that scope.
- Local draft mapping receives already-created parent/child IDs and timestamps from its owner.
  It must not call UUID or the clock (no mapper in `data-mappers/**` does today).
