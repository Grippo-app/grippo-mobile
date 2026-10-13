# Repository and Feature ownership

The live layout is `data-features/<feature>/src/commonMain/kotlin/`; public models,
Feature interfaces and UseCases live in `data-features/feature-api/`.

- Repository owns service access, mapping, cache writes and reconciliation. FeatureImpl exposes
  the public contract; a composing UseCase owns complete domain operations spanning Features.
- Observation returns domain Flow from the authoritative local owner. One-shot methods preserve
  their actual Result/throwing shape; do not mechanically convert all methods into one pattern.
- MUST apply the canonical [operation outcome/cache rules](error-pipeline.md) before API-backed
  cache writes, range reconciliation or deciding that optional readback failure is nonblocking.
  Local drafts/preferences follow their owner contract; they do not require server confirmation.
- Repository never leaks DTO/Entity through Feature contracts. Compose directional mapper calls
  here rather than making mapper modules depend on each other.

Use `data-features/trainings/` and `data-features/user/` as source examples, inspecting their
actual operation semantics rather than copying every dependency or failure policy.
