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
   throwing command ([repositories](references/repositories.md) lists the current UseCase shapes).
   Preserve required outcomes, cleanup, hydration and cancellation.
2. For wire changes, read [backend-contract-client](../backend-contract-client/SKILL.md).
   Verify authoritative fields/method/path first. Never invent transport shape.
3. Keep conversions in the seven directional modules using [mappers](../mappers/SKILL.md).
   Domain closed sets are typed; transport/storage retain wire values.
4. MUST apply [operation outcome and cache rules](references/operation-outcomes.md) to required
   hydration, partial success and reconciliation; local drafts/preferences keep their owner contract.
5. Use [DI](../di-modules/SKILL.md) for bindings. Add only dependencies the implementation uses.
6. For changed feature behavior apply [analytics](../feature-analytics/SKILL.md) and
   [state preservation](../state-preservation/SKILL.md); business code stays telemetry-free.

## Decisions and gates

- Resolve an unclear domain shape or partial-success policy with the user before implementing it
  ([when to stop and ask](../../../CLAUDE.md#when-to-stop-and-ask)).
- Module, schema/migration and auth/network-core changes stay inside the authorized scope: its
  necessary steps need no repeated consent; scope expansion or a locked-decision change needs a
  question ([scope discipline](../../../CLAUDE.md#scope-discipline)).
- Response and request DTO fields, including collections, are nullable with `= null` and explicit
  wire keys; a request field the server must receive as explicit `null` carries
  `@EncodeDefault(EncodeDefault.Mode.ALWAYS)` ([DTO/API](references/dtos-and-api.md)).
  Request validity is enforced before sending.
- MUST preserve caller cancellation at suspend Result boundaries: rethrow `CancellationException`
  before handling ordinary failures and add no generic wrappers. `data-features/**` contains no
  `runCatching`; the only `runCatching` calls in the data layer are the google-auth/apple-auth
  platform adapters, whose suspend paths rethrow cancellation and fatal errors. Explicit
  cancellation checks follow the [canonical coroutine rules](../validation-gates/references/forbidden-patterns.md#coroutines).
  Review actual callees and retain the declared error contract.
- Before completion MUST review ownership, required outcomes, cache reconciliation, UTC conversion,
  privacy and cancellation in the source/diff, run the permitted checks under the
  [verification policy](../../../CLAUDE.md#verification-policy) and report unverified runtime behavior.

Read the relevant reference only:
[repositories](references/repositories.md), [operation outcomes](references/operation-outcomes.md),
[DTO/API](references/dtos-and-api.md), [network](references/backend-client.md),
[auth session](references/auth-session.md), [Room](references/persistence-room.md),
[migration](references/cookbook-room-migration.md), [DataStore](references/datastore.md).
