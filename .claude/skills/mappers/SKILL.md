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
- A mapper file holds only its area's conversion extensions: the public `toState`/`toDomain`/…
  functions and private conversions of nested source model types into their targets
  (`DomainTrainingStreakFeatured.toState()` in `TrainingStreakMapper.kt`). MUST NOT add private
  helper functions, factories or constants that are not such conversions; a private function
  that wraps one field's factory call (`LocalDate.toNextTrainingDateState()`) is a helper. Build
  that value inline at its property, as `TrainingStreakMapper.kt` does (excerpt):
  `nextTrainingDate = nextTrainingDate?.let { date -> DateFormatState.of(value = date, range = …, format = …) }`.
  A calculation that is not a conversion belongs at its source type or utility owner
  ([helper ownership](../validation-gates/references/code-style.md#visibility-and-files)).
- MUST guard required nullable fields and failed dictionary/relation promotion with
  `AppLogger.Mapping.log(value) { specificMessage } ?: return null`. MUST NOT use `!!` or invented
  placeholders. Nullable targets pass through; default an absent collection only where the target
  semantics explicitly define it as empty.
- DTOs/entities retain wire values. Promote enum/date/duration at the domain boundary and reverse
  exact dictionary keys for requests. Verify wire changes with [backend-contract-client](../backend-contract-client/SKILL.md).
- Wrapping already-typed values into a single-field `*Body` is request construction and
  belongs to the Repository; a mapper converts models, not one primitive into a body.
- Representation-only formatting, stable presentation sorting and collection conversion are allowed.
  Business validation, outcomes, authorization, metric computation, UUID generation and `now()`
  MUST remain in the owning business/state operation, outside mappers (no mapper in
  `data-mappers/**` generates identity or time today). When a signature inside the authorized
  scope needs identity or time, pass it explicitly from the owner.
- Resolve an unknown source/target shape before writing code. A direction outside the seven or a
  new module/dependency edge is scope expansion ([scope discipline](../../../CLAUDE.md#scope-discipline)).

## Completion gate

MUST review required/optional null semantics, closed-set unknowns, parent IDs, timezone/unit
conversion, round-trip identity, all affected callers and that each changed mapper file holds
only conversion extensions. MUST keep dependencies directional;
Domain↔State modules may depend on `:ui-core:state`, not feature UI or Compose components.
Run the permitted checks under the [verification policy](../../../CLAUDE.md#verification-policy)
and report unverified behavior.

Read [mapping conventions](references/mapping-conventions.md) for signatures and
[null handling](references/null-safety-and-logging.md) for missing data/parent IDs.
