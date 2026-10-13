---
name: mappers
description: Implement or review Grippo conversions among DTOs, Entities, domain models and UI state in the seven isolated mapper modules. Use for shape translation, nullable fields and domain type promotion.
---

# Grippo mappers

Read source and target declarations, existing area mapper and its callers. Conversions live in
`:data-mappers:*`; do not inline business-model conversion in a ViewModel/Screen or another layer.

| Direction | Module |
|---|---|
| DTO → Entity | `dto-to-entity` |
| Entity/Pack → Domain | `entity-to-domain` |
| DTO → Domain | `dto-to-domain` |
| Domain → State | `domain-to-state` |
| State → Domain | `state-to-domain` |
| Domain → Entity | `domain-to-entity` |
| Domain → DTO | `domain-to-dto` |

## Strict rules

- MUST use top-level extensions in the existing area file/package. MUST NOT add mapper classes,
  DI, shared bases or dependency/import across directional modules. Compose directions at the consumer call site.
- MUST guard required nullable fields and failed dictionary/relation promotion with
  `AppLogger.Mapping.log(value) { specificMessage } ?: return null`. MUST NOT use `!!` or invented
  placeholders. Nullable targets pass through; default an absent collection only where the target
  semantics explicitly define it as empty.
- DTOs/entities retain wire values. Promote enum/date/duration at the domain boundary and reverse
  exact dictionary keys for requests. Verify wire changes with [backend-contract-client](../backend-contract-client/SKILL.md).
- Representation-only formatting, stable presentation sorting and collection conversion are allowed.
  Business validation, outcomes, authorization, metric computation, UUID generation and `now()`
  MUST remain in the owning business/state operation, outside mappers. Pass required identity/time
  explicitly when authorized to change an affected signature.
- Existing UUID/time-generating mappers are deviations, not permission to copy them. Repair affected
  behavior within the task scope; do not silently change unrelated draft identities.
- Resolve unknown source/target shape or a direction outside the seven before writing code.
  MUST keep new module/dependency edges within authorized scope.

## Completion gate

MUST review required/optional null semantics, closed-set unknowns, parent IDs, timezone/unit
conversion, round-trip identity and all affected callers. MUST keep dependencies directional;
Domain↔State modules may depend on `:ui-core:state`, not feature UI or Compose components.
MUST apply `CLAUDE.md` Verification policy and report unverified behavior.

Read [mapping conventions](references/mapping-conventions.md) for signatures and
[null handling](references/null-safety-and-logging.md) for missing data/parent IDs.
