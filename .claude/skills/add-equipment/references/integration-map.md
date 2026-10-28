# Equipment integration map

These paths describe the current implementation. Open them and search all uses
of the neighboring type before editing; the source owns signatures and membership.
Use [equipment identifiers](naming.md) to resolve identity and names across these
owners before creating declarations.

## Catalog identity and data flow

Inspect [GrippoApi](../../../../data-services/backend/src/commonMain/kotlin/com/grippo/services/backend/GrippoApi.kt),
[EquipmentRepositoryImpl](../../../../data-features/equipment/src/commonMain/kotlin/com/grippo/data/features/equipment/data/EquipmentRepositoryImpl.kt)
and [EquipmentDao](../../../../data-services/database/src/commonMain/kotlin/com/grippo/services/database/dao/EquipmentDao.kt).
The current catalog is fetched by `getEquipments` from `GET /equipments`, validated
and written to Room. Catalog observation reads Room; refresh removes local IDs
absent from the server response. A client-only row cannot establish catalog support.

[ExcludedEquipmentsRepositoryImpl](../../../../data-features/excluded-equipments/src/commonMain/kotlin/com/grippo/data/features/excluded/equipments/data/ExcludedEquipmentsRepositoryImpl.kt)
sends equipment IDs through the existing exclusions API. Independently selectable
variants therefore need separate server rows and IDs as well as separate type keys.

Find authoritative backend source or documentation under
[contract evidence](../../backend-contract-client/references/overview.md). Inspect
its equipment dictionary, group, persistence and catalog creation/update path.
If the type is new there too and backend work is within the requested scope,
include the required backend source and catalog change; follow that repository's
migration and deployment rules. For mobile-only work, implement the verified
client contract and report the separate server prerequisite. Report which
environment was actually updated. A backend enum alone does not prove that the
catalog contains a selectable row.

## Mobile touchpoints

| Source | What to reconcile |
|---|---|
| [EquipmentEnum](../../../../data-features/feature-api/src/commonMain/kotlin/com/grippo/data.features.api/equipment/models/EquipmentEnum.kt) | Exact backend type key and `of` promotion; preserve existing keys |
| [EquipmentEnumState](../../../../ui-core/state/src/commonMain/kotlin/com/grippo/core/state/equipments/EquipmentEnumState.kt) | New member, vector import and `icon`, `sortPriority` (intended position relative to similar machines), `TITLE_RES` branch |
| [EquipmentEnumMapper](../../../../data-mappers/domain-to-state/src/commonMain/kotlin/com/grippo/domain/state/equipment/EquipmentEnumMapper.kt) | Exhaustive domain-to-state branch |
| [Equipment diagrams](../../../../design-system/resources/provider/src/commonMain/kotlin/com/grippo/design.resources/provider/equipment/) | Native diagram under the [vector pipeline](imagevector-pipeline.md) |
| [Compose resources](../../../../design-system/resources/provider/src/commonMain/composeResources/) | `equipment_*` title in every current locale's `strings.xml` |
| [EquipmentGroupState](../../../../ui-core/state/src/commonMain/kotlin/com/grippo/core/state/equipments/EquipmentGroupState.kt) | `stubEquipments` entry in the correct preview group with a unique preview ID |

Search every mapper direction and every `EquipmentEnum` / `EquipmentEnumState`
use, including sorting and grouped branches. Update actual consumers rather than
creating a mapper merely because a module exists.

When the request requires a new group, also reconcile
[EquipmentGroupEnum](../../../../data-features/feature-api/src/commonMain/kotlin/com/grippo/data.features.api/equipment/models/EquipmentGroupEnum.kt),
[EquipmentGroupEnumState](../../../../ui-core/state/src/commonMain/kotlin/com/grippo/core/state/equipments/EquipmentGroupEnumState.kt),
[EquipmentGroupEnumMapper](../../../../data-mappers/domain-to-state/src/commonMain/kotlin/com/grippo/domain/state/equipment/EquipmentGroupEnumMapper.kt)
and the group's localized title with the backend group identity. An equipment
addition that fits an existing group keeps that group contract unchanged.

Inspect [EquipmentResponse](../../../../data-services/backend/src/commonMain/kotlin/com/grippo/services/backend/dto/equipment/EquipmentResponse.kt),
[EquipmentEntity](../../../../data-services/database/src/commonMain/kotlin/com/grippo/services/database/entity/EquipmentEntity.kt)
and the [entity-to-domain mapper](../../../../data-mappers/entity-to-domain/src/commonMain/kotlin/com/grippo/entity/domain/equipment/EquipmentMapper.kt).
Their current `type` path carries a raw string until `EquipmentEnum.of` resolves
it. A new supported dictionary member with unchanged fields does not itself
require a DTO shape or Room schema change. Preserve existing unknown-value and
incomplete-response handling under the mapper and data-layer rules.

This handling also affects rollout. Under the catalog exception in
[operation outcomes](../../data-layer/references/operation-outcomes.md#operation-outcomes-and-cache-reconciliation-canonical),
a build hides equipment whose type it does not know until an app update adds the
type. Until then, on that build:

- the user cannot select or exclude hidden equipment; for an item whose own type is
  unknown, the example filters (`UserExerciseExamplesUseCase`,
  `GeneratePresetTrainingUseCase`) also ignore an exclusion made from a newer build,
  while a known-type item hidden with its unknown group keeps its exclusion;
- saving the selection (`ProfileEquipmentsViewModel.onApply` ->
  `setExcludedEquipments`) sends only visible IDs, and the backend replaces the
  whole set, so a hidden item's exclusion is removed for the user. This is an
  accepted rollout risk.

Store and TestFlight builds without the exception react differently; inspect
`getEquipments` and `SplashViewModel` at each such build's commit. A build whose
`getEquipments` rejects an unknown type during domain promotion keeps retrying
Splash. A build that refreshes the catalogs only before sign-in fails a signed-in
user's exercise-example refresh on the missing equipment parent. Publishing a new
dictionary value therefore requires a compatibility decision for every store or
TestFlight build still in use that lacks the exception, not only a backend enum
edit. Where the current build refreshes the catalogs is owned by the
[operation outcomes exception](../../data-layer/references/operation-outcomes.md#operation-outcomes-and-cache-reconciliation-canonical).
The [backend task](backend-task-prompt.md) owns the contents of the handoff for
remaining server work.

`stubEquipments` supplies preview data. Use a real known ID or an explicitly
preview-only identity there; it never replaces the production catalog row.

## Selection, analytics and preservation

Inspect [EquipmentSelection](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/equipment/EquipmentSelection.kt)
and [EquipmentCard](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/equipment/EquipmentCard.kt).
Selection currently renders the chosen group's rows keyed by equipment ID and
forwards that ID; the card resolves the state type's title and vector. Check the
new and existing machines together, including selected and deselected appearances.

Trace the actual owning screens, currently including
[ProfileEquipmentsViewModel](../../../../ui-screen-features/profile/src/commonMain/kotlin/com/grippo/profile/equipments/ProfileEquipmentsViewModel.kt)
and [MissingEquipmentsViewModel](../../../../ui-screen-features/authorization/src/commonMain/kotlin/com/grippo/authorization/profile/creation/missing/equipments/MissingEquipmentsViewModel.kt).
Inspect their states, Components and coverage records before making gate claims.
Their current catalog reconciliation distinguishes known IDs from new IDs;
selection callbacks already report generic selection facts. Exercise the new
row's independent toggle, save/exclusion round trip, re-entry and arrival after
an empty or previously known catalog under the canonical gate policies.

Use [feature-analytics](../../feature-analytics/SKILL.md) and
[state-preservation](../../state-preservation/SKILL.md) for the decisions and
required records. Retain existing hooks and draft owners when they already cover
the addition; a new catalog member does not by itself justify a new event or draft.
