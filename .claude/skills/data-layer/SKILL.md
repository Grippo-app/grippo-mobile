---
name: data-layer
description: Change Grippo Feature/Repository/UseCase contracts, API-backed data flow, Room persistence or DataStore preferences. Use for domain behavior and storage ownership; mapper implementation belongs to mappers.
---

# Grippo data layer

Read `CLAUDE.md`, the actual affected Feature/UseCase/Repository and callers before editing.
UI consumes public contracts in `:data-features:feature-api`; FeatureImpl and Repository
implementations MUST remain internal. MUST NOT add analytics dependencies to the business layer.

## Workflow

1. Establish the existing operation contract: Flow observation, Result-returning method or
   throwing command. Preserve required outcomes, cleanup, hydration and cancellation.
2. For wire changes, read [backend-contract-client](../backend-contract-client/SKILL.md).
   Verify authoritative fields/method/path first. Never invent transport shape.
3. Keep conversions in the seven directional modules using [mappers](../mappers/SKILL.md).
   Domain closed sets are typed; transport/storage retain wire values.
4. MUST apply [operation outcome and cache rules](references/error-pipeline.md) to required
   hydration, partial success and reconciliation; local drafts/preferences keep their owner contract.
5. Use [DI](../di-modules/SKILL.md) for bindings. Add only dependencies the implementation uses.
6. For changed feature behavior apply [analytics](../feature-analytics/SKILL.md) and
   [state preservation](../state-preservation/SKILL.md); business code stays telemetry-free.

## Decisions and gates

- MUST resolve unclear domain shapes and partial-success policies with the user before implementing them.
- MUST keep module, schema/migration and auth/network-core changes within authorized scope.
  Necessary steps of an authorized task do not need repeated consent; scope expansion or
  a locked architectural decision requires a question under `CLAUDE.md`.
- Response and request DTO fields, including collections, are nullable with `= null` and
  explicit unchanged wire keys. Existing source deviations do not weaken this standard;
  unrelated DTO cleanup requires its own scope. Request validity is enforced before sending.
- MUST preserve caller cancellation at suspend Result boundaries. Bare `runCatching`/`mapCatching`
  can capture cancellation; do not copy them as a compliant pattern or add generic wrappers/
  manual `ensureActive()` checks. Review actual callees and retain the declared error contract.
- Before completion MUST review ownership, required outcomes, cache reconciliation, UTC conversion,
  privacy and cancellation in source/diff. MUST apply `CLAUDE.md` Verification policy, including
  its explicit-user-request exception for automated tests; report unverified behavior.

Read the relevant reference only:
[repositories](references/repositories.md), [operation errors](references/error-pipeline.md),
[DTO/API](references/dtos-and-api.md), [Room](references/persistence-room.md),
[migration](references/cookbook-room-migration.md), [DataStore](references/datastore.md),
[network](references/backend-client.md), [auth](references/auth-session.md).
