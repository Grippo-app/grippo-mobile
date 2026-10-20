# Repository and Feature ownership

The live layout is `data-features/<feature>/src/commonMain/kotlin/`; public models,
Feature interfaces and UseCases live in `data-features/feature-api/`.

- Repository owns service access, mapping, cache writes and reconciliation. FeatureImpl exposes
  the public contract; a composing UseCase owns complete domain operations spanning Features.
- Observation returns domain Flow from the authoritative local owner. One-shot commands follow
  [throwing suspend contracts](operation-outcomes.md#throwing-suspend-contracts-canonical).
- MUST apply the canonical [operation outcome/cache rules](operation-outcomes.md) before API-backed
  cache writes, range reconciliation or required follow-up reads.
  Local drafts/preferences follow their owner contract; they do not require server confirmation.
- Repository never leaks DTO/Entity through Feature contracts. Compose directional mapper calls
  here rather than making mapper modules depend on each other.

Use `data-features/trainings/` and `data-features/user/` as source examples, inspecting their
actual operation semantics rather than copying every dependency or failure policy.

## Stateless repositories (canonical)

Repository implementations MUST be stateless. Their only retained instance properties are
constructor-injected dependencies. MUST NOT store request results, caches, mutable flows,
flags, Jobs, coroutine scopes or synchronization primitives in a repository. Class-body
properties, including immutable and `by lazy` properties, MUST NOT retain values or services
derived from dependencies. In particular, do not obtain and store a DAO from `Database` in
the repository body; inject the existing DAO binding through the constructor using
[DI](../../di-modules/SKILL.md).

Per-call locals for request inputs, mapping, validation and transaction work, and pure
computed getters without backing state, are allowed.
Authoritative cache and observable data stay with their existing storage/service owner;
lifecycle-driven loading and cancellation stay with their existing lifecycle owner. A
repository delegates observations to that owner rather than retaining its own stream or snapshot.
This ownership rule does not authorize introducing a new owner or refactoring unrelated code.

Completion check: inspect constructor properties, every class-body property initializer or
delegate, observation sources and coroutine ownership against this section.

## Minimal domain models

Constructor fields represent independent domain facts. MUST NOT store a count
beside its complete collection, a value beside the source that determines it, or
a discriminator already expressed by a sealed subtype. Derive needed values at
the operation or with a computed getter; constructor defaults still duplicate
state and `copy` can make them inconsistent. Do not introduce completeness or
availability flags merely to support a metric branch.

Before keeping an apparent duplicate, inspect the real DTO/entity/mapper graph:
a server-provided summary can be independent of an unloaded detail collection,
but that distinction needs an actual contract. Keep mapping omissions and
unavailable data honest at their existing boundary; do not infer completeness
from redundant copied counts. Review constructor, mapper, `copy` and preview
callers together when removing a field.

When the accepted product contract calculates only from available mapped details,
state that scope beside the affected UI totals, including zero, and in the
coverage record. Lossy mapping MUST NOT turn that sample into a claim about the
complete history. Preserve independently recorded aggregates at their existing
owner; do not label them as detail-derived or add a guessed completeness flag.

## UseCase shapes (verified)

Public UseCases live in `data-features/feature-api/.../<area>/*UseCase.kt` and are
registered in the existing hand-written `FeatureApiModule` (see
[DI](../../di-modules/SKILL.md)). Inspect the actual source before changing a contract.

- `SetTrainingUseCase` and `UpdateTrainingUseCase` return the saved training's non-null ID
  after required hydration and user refresh. `DeleteTrainingUseCase` and `UpdateWeightUseCase`
  complete their required mutation and refresh before returning `Unit`.
- Login/Register commands return the existing profile-presence Boolean. Logout, profile creation
  and account deletion complete their required cleanup/preparation before returning.
- `UserExerciseExamplesUseCase` and validator context observations retain their Flow contracts.
  Metrics and validators keep their pure calculator return types.

Return/error semantics are owned by
[operation outcomes](operation-outcomes.md#throwing-suspend-contracts-canonical).
