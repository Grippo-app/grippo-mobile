---
name: add-equipment
description: Add or distinguish Grippo equipment with consistent identifiers and complete catalog, enum, mapper, localization, ImageVector and selection integration, then prepare any remaining backend task. Use when adding an equipment type or variant; an explicitly requested artwork-only task uses the vector pipeline alone.
---

# Add equipment

By default, deliver equipment that has its own identity and can be selected
independently. Honor explicit mobile-only or artwork-only scope; complete that
requested slice and report any server prerequisite for using it in the live catalog.
Read [CLAUDE.md](../../../CLAUDE.md) and the affected source, then choose the
requested scope before loading the references.

## Choose the scope

- **Artwork only:** use the [ImageVector pipeline](references/imagevector-pipeline.md)
  and its vector completion gate. Catalog identifiers, integration steps and
  selection gates below do not apply; no backend task is needed.
- **Mobile integration:** follow the workflow below and the
  [identifier rules](references/naming.md) and
  [integration map](references/integration-map.md). Complete the verified client
  contract and generate a backend handoff for remaining server work.
- **Full integration:** follow the same workflow with the backend work authorized
  by the request. Verify the target catalog separately from source support.

For either integration scope, use the vector pipeline only when diagram work is
needed; reuse an existing suitable asset. The completion gate below applies to
integration tasks.

## Workflow

1. **Identify the machine.** Establish the mechanical distinction from existing
   equipment, its group, visible name, weight-entry convention and closest diagram.
   Resolve exact and semantic matches under the identifier rules before allocating
   names. A variant intended for independent selection needs its own catalog
   identity. Preserve existing types and user choices unless the request changes them.
2. **Establish catalog support.** Trace the production catalog source and confirm
   the backend type key, group and equipment ID through
   [backend-contract-client](../backend-contract-client/SKILL.md). Inspect the
   backend repository's own instructions before changing its enum or catalog
   when backend work is within the requested scope.
   Separate source support, a stored catalog row and deployment availability in
   the report. If a prerequisite is missing, complete independent artwork and
   localization work and state exactly what still prevents selection. An
   unconfirmed wire key blocks dependent enum and mapper edits; an agreed key
   can support mobile-only work while the server row remains pending. Finalize
   the naming tuple from that evidence before creating dependent declarations.
3. **Implement the consumer chain.** Follow the integration map through domain
   promotion, state enum, all actual exhaustive mappings, image, localized title
   and hint, exercise ordering and preview data. Search by both the closest
   existing type and the new identifiers to find additional consumers. Apply
   [data-layer](../data-layer/SKILL.md), [mappers](../mappers/SKILL.md) and
   [design-system](../design-system/SKILL.md) at their owners.
4. **Trace selection and saving.** Check catalog refresh, independent toggling,
   exclusions and re-entry in the actual selection owners. Apply
   [feature-analytics](../feature-analytics/SKILL.md) and
   [state-preservation](../state-preservation/SKILL.md) to the affected surfaces;
   existing generic callbacks and ID-based drafts may already cover the new row.
   Add behavior or persistence code only for a demonstrated gap.
5. **Review and verify.** Use [task-orchestrator](../task-orchestrator/SKILL.md)
   for independent code, design and gate review. Builds and runtime checks follow
   the [verification policy](../../../CLAUDE.md#verification-policy); resource and
   screen evidence follows [visual consistency](../design-system/references/visual-consistency.md).
6. **Prepare the backend task.** Apply the
   [backend prompt handoff](references/backend-task-prompt.md) after an integration
   that leaves server work pending. Generate a filled, ready-to-copy prompt in
   the user's language, preserving the requested backend outcome under that
   handoff's rules and using the final client contract and actual evidence.
   Deliver it under [final response delivery](references/backend-task-prompt.md#final-response-delivery).
   If existing backend support is verified, report that no backend task is needed.

Creating or invoking this skill does not grant deployment or live catalog-write
permissions. Apply the task's existing authorization and the backend's own rules.

## Completion gate

- Backend evidence identifies the distinct type and group; the required catalog
  row and ID exist in the target environment, or that remaining dependency is
  reported explicitly and live selection is not claimed complete. An explicit
  mobile-only task can complete its client outcome without modifying the server.
- The domain type reaches the correct state type through every affected mapping;
  title, hint, image and ordering resolve, with real translations in every current
  locale and a distinct preview identity.
- The identifier naming check passes, including collisions, explicit legacy/reuse
  mappings and agreement between source and the backend handoff.
- The diagram passes its vector pipeline checks; both the ordinary type and the
  variant remain recognizable when the task requires both.
- Selection, saved exclusions and restored drafts retain independent IDs;
  analytics and preservation gates are reconciled against the affected owners.
- Required reviews and permitted checks have actual results. Distinguish source
  integration, isolated artwork renders, application checks and deployed data.
- Any remaining backend work has a concrete prompt whose contract facts,
  compatibility concerns and unresolved environment/data inputs are explicit.
  The final response passes its [delivery gate](references/backend-task-prompt.md#final-response-delivery).

## References

- [Equipment identifiers](references/naming.md): identity collisions and naming
  across contract, enums, artwork, resources and fixtures; read for integration.
- [Integration map](references/integration-map.md): source locations and catalog
  dependencies; read for every equipment integration.
- [ImageVector pipeline](references/imagevector-pipeline.md): equipment-specific
  visual family, native authoring and export inspection; read for diagram work.
- [Backend prompt handoff](references/backend-task-prompt.md): read when an
  integration leaves server work pending or the user requests a backend task.
