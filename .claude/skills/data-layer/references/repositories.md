# Repository and Feature ownership

The live layout is `data-features/<feature>/src/commonMain/kotlin/`; public models,
Feature interfaces and UseCases live in `data-features/feature-api/`.

- Repository owns service access, mapping, cache writes and reconciliation. FeatureImpl exposes
  the public contract; a composing UseCase owns complete domain operations spanning Features.
- Observation returns domain Flow from the authoritative local owner. One-shot methods preserve
  their actual Result/throwing shape; do not mechanically convert all methods into one pattern.
- API-backed writes update the cache only after server success. Local drafts/preferences do not
  require fictional server confirmation. Keep multi-row consistency in the existing transaction boundary.
- Reconcile only the requested range and owner. Distinguish a confirmed empty server collection
  from missing/invalid payload; dropping malformed rows must not silently justify deleting valid cache.
- A server save, cache hydration and cleanup may form one required operation. Read the UseCase
  and caller before choosing success. Optional refresh requires an explicit partial-success policy.
- Repository never leaks DTO/Entity through Feature contracts. Compose directional mapper calls
  here rather than making mapper modules depend on each other.

Use `data-features/trainings/` and `data-features/user/` as source examples, inspecting their
actual operation semantics rather than copying every dependency or failure policy.
