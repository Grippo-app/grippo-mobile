# Equipment identifiers

This reference owns the equipment-specific mapping between identifiers. General
Kotlin and resource style belongs to
[code style](../../validation-gates/references/code-style.md); wire evidence belongs
to [backend-contract-client](../../backend-contract-client/SKILL.md).

## Resolve identity before allocating names

Search the requested key, proposed declarations and mechanical synonyms in the
catalog enums, localized labels, equipment vectors and preview fixtures. Compare
the mechanism as well as the spelling: "forearm pec deck" and "butterfly with
forearm pads" can describe the same machine.

- For an existing identity, reuse its complete mapping and verify catalog
  availability separately. A different display name alone does not create a type.
- For an explicitly distinct catalog identity with the same mechanism, preserve
  the old identity and reuse suitable artwork. Record that reuse in the mapping.
- For a mechanically distinct variant, allocate its own confirmed type and the
  artwork it needs. Resolve unclear overlap with the requester before creating
  duplicate identities or geometry; an already explicit distinction needs no
  repeated confirmation.

Rows with the same type currently resolve the same UI title and icon even when
their server IDs differ. Inspect
[EquipmentEnumState](../../../../ui-core/state/src/commonMain/kotlin/com/grippo/core/state/equipments/EquipmentEnumState.kt)
and [EquipmentMapper](../../../../data-mappers/domain-to-state/src/commonMain/kotlin/com/grippo/domain/state/equipment/EquipmentMapper.kt)
before promising that two such rows will look different.

## Define the naming tuple

For integration, record the final tuple in the task handoff and reconcile it with
the source. The defaults below apply to new identifiers; they do not rename
existing contracts or aliases.

| Namespace | New-identifier default | Current forearm example |
|---|---|---|
| Raw equipment type | Exact confirmed backend key; propose `lower_snake_case` until confirmed | `butterfly_forearm` |
| Domain enum member | Same stem in `UPPER_SNAKE_CASE` | `EquipmentEnum.BUTTERFLY_FOREARM` |
| UI enum member | Same member as domain, connected by the explicit mapper | `EquipmentEnumState.BUTTERFLY_FOREARM` |
| New vector file / public property | Same stem in `PascalCase`, with `.kt` for the file | `ButterflyForearm.kt` / `ButterflyForearm` |
| New vector builder name / private cache | Public property spelling / `_` plus that spelling | `"ButterflyForearm"` / `_ButterflyForearm` |
| Title / weight-hint resources | `equipment_<type>` / `equipment_hint_<type>` | `equipment_butterfly_forearm` / `equipment_hint_butterfly_forearm` |
| New synthetic preview row ID | `preview-` plus the type stem with hyphens | `preview-butterfly-forearm` |
| Raw group / domain and UI group members | Reuse the confirmed existing group mapping | `machines` / `EquipmentGroupEnum.MACHINES` / `EquipmentGroupEnumState.MACHINES` |
| Group title resource | Reuse the existing key; new groups use `equipment_group_<group>` | `equipment_group_machines` |
| Server equipment ID / group ID | Real persistent identities verified in the target catalog | Unresolved until backend verification |

For a new confirmed key such as `pec_deck_elbow_supports`, the default new member
is `PEC_DECK_ELBOW_SUPPORTS` and the default new asset is `PecDeckElbowSupports`.
This is a naming example, not an additional equipment request. First apply the
identity check above; reuse `ButterflyForearm` when that existing geometry fits.
An explicitly supplied asset name, an existing suitable asset or an authoritative
legacy mapping takes precedence over deriving a new asset name. Carry the exact
resulting mapping into the handoff rather than silently substituting a synonym.

For artwork-only work, choose a descriptive `PascalCase` mechanical name after
the collision search and apply only the vector file/property/builder/cache rules.
Drawing an icon does not require inventing a wire key, group or catalog row.

## Preserve namespace boundaries

- Treat raw type/group keys as case-sensitive contract data.
  [EquipmentEnum.of](../../../../data-features/feature-api/src/commonMain/kotlin/com/grippo/data.features.api/equipment/models/EquipmentEnum.kt)
  and [EquipmentGroupEnum.of](../../../../data-features/feature-api/src/commonMain/kotlin/com/grippo/data.features.api/equipment/models/EquipmentGroupEnum.kt)
  currently use exact equality. Do not trim, case-fold, translate or repair their
  spelling without a contract change. Never serialize `enum.name` as the raw key.
- Keep the conversion explicit in the existing mapper and presentation branches.
  The defaults describe how to name new declarations, not a runtime algorithm
  for constructing enum values, resource names or vector lookups.
- Localized labels describe the machine naturally in each language. Backend
  `name` is separate required catalog data; the current domain-to-state mapper
  discards it in favor of enum resources. Neither string determines identity.
  A vector builder name is also an internal drawing identifier, not an API value.
- Row IDs must be unique within their fixture. Synthetic/preview-only IDs remain
  outside the production catalog and exclusion payloads. A fixture may contain a
  known real server ID; its presence there does not prove target availability.
  Existing component previews may use enum names as fixture IDs; the synthetic-row
  default applies to new stub rows.
  Extend the owning component's preview functions rather than adding a wrapper
  solely to encode the equipment name.
- Reuse the existing group tuple unless a new group is actually requested.
  A group key and a group UUID are different values. New groups require their
  own contract evidence and the touchpoints in the
  [integration map](integration-map.md#mobile-touchpoints).

Existing mappings are not uniformly derivable: inspect `ROMAIN_CHAIR` /
`romain_chair`, which map to `RomanChair`, title `equipment_roman_chair` and hint
`equipment_hint_romain_chair`. Some existing vector builder/cache spellings also
differ from their public properties. Preserve these explicit mappings unless
their rename is part of the requested scope; do not copy their irregularities
into a new declaration or perform a catalog-wide cleanup.

## Naming check

Search every finalized identifier for declarations, collisions and consumers.
Verify the exact key, both enum members, explicit mapping, reused/new asset tuple,
title/hint keys in every current locale, group tuple and preview identity against
the [integration map](integration-map.md). Carry this same tuple into any
[backend task](backend-task-prompt.md), marking client-only identifiers and
unresolved persistent IDs. Name consistency does not establish deployed support.
