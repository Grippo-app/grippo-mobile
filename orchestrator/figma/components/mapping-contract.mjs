// Semantic contract for the project-owned component mapping registry. Schema
// proves shape; this module proves identity uniqueness, relation/cardinality
// agreement, implementation exclusivity (shared-implementation is the only
// legal multi-claim), property/slot binding exactness, disposition target
// exactness, and the scope binding rules. A registry that fails here is
// COMPONENT_MAPPING_INVALID: it blocks comparison and mutation and is never
// treated as empty.
import { canonicalHash } from '../runtime/canonical-json.mjs'

export const COMPONENT_MAPPINGS_RELATIVE_PATH = 'orchestrator/figma/component-mappings.json'

const isRecord = (value) => !!value && typeof value === 'object' && !Array.isArray(value)

export function mappingRegistrySemanticError(registry) {
  if (!isRecord(registry)) return 'registry must be an object'

  const receiptIds = new Set()
  for (const receipt of registry.operationReceipts || []) {
    if (receiptIds.has(receipt.operationId)) return `duplicate operation receipt ${JSON.stringify(receipt.operationId)}`
    receiptIds.add(receipt.operationId)
    if (receipt.revision > registry.revision) return `operation receipt ${receipt.operationId} is newer than registry revision`
  }

  const mappingIds = new Set()
  const activeByDesignComponent = new Map()
  // projectComponentId -> [{ mappingId, relation }]
  const activeClaims = new Map()
  for (const mapping of registry.mappings) {
    if (mappingIds.has(mapping.mappingId)) return `duplicate mappingId ${JSON.stringify(mapping.mappingId)}`
    mappingIds.add(mapping.mappingId)

    const adapterSeen = new Set()
    const platformSeen = new Set()
    for (const implementation of mapping.implementations) {
      if (adapterSeen.has(implementation.adapterId)) {
        return `mapping ${mapping.mappingId} binds adapter ${implementation.adapterId} twice`
      }
      adapterSeen.add(implementation.adapterId)
      if (platformSeen.has(implementation.platform)) {
        return `mapping ${mapping.mappingId} binds platform ${implementation.platform} twice`
      }
      platformSeen.add(implementation.platform)

      const cardinality = implementation.projectComponentIds.length
      if (implementation.relation === 'external') {
        if (cardinality !== 0) return `mapping ${mapping.mappingId} external implementation must carry no project components`
        if (!implementation.externalRef) return `mapping ${mapping.mappingId} external implementation requires externalRef`
      } else {
        if (implementation.externalRef !== undefined) {
          return `mapping ${mapping.mappingId} carries externalRef outside an external relation`
        }
        if (implementation.relation === 'composite') {
          if (cardinality < 2) return `mapping ${mapping.mappingId} composite implementation requires at least two project components`
        } else if (cardinality !== 1) {
          return `mapping ${mapping.mappingId} relation ${implementation.relation} requires exactly one project component`
        }
      }
      for (const projectComponentId of implementation.projectComponentIds) {
        if (!projectComponentId.startsWith(implementation.adapterId + ':symbol:')) {
          return `mapping ${mapping.mappingId} target ${JSON.stringify(projectComponentId)} is outside adapter ${implementation.adapterId}`
        }
      }
      if (mapping.state === 'active') {
        for (const projectComponentId of implementation.projectComponentIds) {
          let claims = activeClaims.get(projectComponentId)
          if (!claims) { claims = []; activeClaims.set(projectComponentId, claims) }
          claims.push({ mappingId: mapping.mappingId, relation: implementation.relation })
        }
      }
    }

    const propertySeen = new Set()
    for (const propertyMapping of mapping.propertyMappings) {
      const key = `${propertyMapping.adapterId} ${propertyMapping.designPropertyId}`
      if (propertySeen.has(key)) {
        return `mapping ${mapping.mappingId} maps design property ${JSON.stringify(propertyMapping.designPropertyId)} twice for adapter ${propertyMapping.adapterId}`
      }
      propertySeen.add(key)
      if (!adapterSeen.has(propertyMapping.adapterId)) {
        return `mapping ${mapping.mappingId} property mapping cites adapter ${propertyMapping.adapterId} without an implementation`
      }
      if (propertyMapping.valueMap && propertyMapping.ignoredValues) {
        const ignored = new Set(propertyMapping.ignoredValues.map((row) => row.value))
        for (const designValue of Object.keys(propertyMapping.valueMap)) {
          if (ignored.has(designValue)) {
            return `mapping ${mapping.mappingId} property ${JSON.stringify(propertyMapping.designPropertyId)} both maps and ignores value ${JSON.stringify(designValue)}`
          }
        }
      }
      if (propertyMapping.ignoredValues) {
        const values = propertyMapping.ignoredValues.map((row) => row.value)
        if (new Set(values).size !== values.length) {
          return `mapping ${mapping.mappingId} property ${JSON.stringify(propertyMapping.designPropertyId)} ignores a value twice`
        }
      }
    }

    const slotSeen = new Set()
    for (const slotMapping of mapping.slotMappings) {
      const key = `${slotMapping.adapterId} ${slotMapping.designSlotId}`
      if (slotSeen.has(key)) {
        return `mapping ${mapping.mappingId} maps design slot ${JSON.stringify(slotMapping.designSlotId)} twice for adapter ${slotMapping.adapterId}`
      }
      slotSeen.add(key)
      if (!adapterSeen.has(slotMapping.adapterId)) {
        return `mapping ${mapping.mappingId} slot mapping cites adapter ${slotMapping.adapterId} without an implementation`
      }
      const bound = slotMapping.projectSlotId !== undefined
      const ignored = slotMapping.ignoredReason !== undefined
      if (bound === ignored) {
        return `mapping ${mapping.mappingId} slot ${JSON.stringify(slotMapping.designSlotId)} must carry exactly one of projectSlotId or ignoredReason`
      }
    }

    if (mapping.visualPolicy && mapping.visualPolicy.renderClass === null && mapping.visualPolicy.by !== 'owner') {
      return `mapping ${mapping.mappingId} render-class tombstone requires by=owner`
    }
    if (mapping.state === 'retired' && !mapping.retirement) {
      return `mapping ${mapping.mappingId} is retired without a retirement record`
    }
    if (mapping.state === 'active' && mapping.retirement) {
      return `mapping ${mapping.mappingId} is active but carries a retirement record`
    }
    if (mapping.provenance.kind === 'task-binding' && !mapping.provenance.taskStem) {
      return `mapping ${mapping.mappingId} task-binding provenance requires taskStem`
    }

    if (mapping.state === 'active') {
      const existing = activeByDesignComponent.get(mapping.designComponentId)
      if (existing) {
        return `design component ${mapping.designComponentId} has two active mappings (${existing} and ${mapping.mappingId})`
      }
      activeByDesignComponent.set(mapping.designComponentId, mapping.mappingId)
    }
  }

  // Exclusivity: one project component may implement several design families
  // only when EVERY active claim on it declares relation shared-implementation.
  for (const [projectComponentId, claims] of activeClaims) {
    if (claims.length < 2) continue
    if (!claims.every((claim) => claim.relation === 'shared-implementation')) {
      return `project component ${JSON.stringify(projectComponentId)} is claimed by ${claims.length} active mappings without shared-implementation relations`
    }
  }

  const dispositionIds = new Set()
  const activeDispositionKeys = new Set()
  for (const disposition of registry.dispositions) {
    if (dispositionIds.has(disposition.dispositionId)) return `duplicate dispositionId ${JSON.stringify(disposition.dispositionId)}`
    dispositionIds.add(disposition.dispositionId)
    const target = disposition.target
    if (target.side === 'design') {
      if (!target.designComponentId || target.projectComponentId || target.adapterId) {
        return `disposition ${disposition.dispositionId} design target must carry exactly designComponentId`
      }
      if (disposition.kind === 'intentionally-project-only') {
        return `disposition ${disposition.dispositionId} kind intentionally-project-only cannot target the design side`
      }
    } else {
      if (!target.projectComponentId || !target.adapterId || target.designComponentId) {
        return `disposition ${disposition.dispositionId} project target must carry projectComponentId and adapterId`
      }
      if (!target.projectComponentId.startsWith(target.adapterId + ':symbol:')) {
        return `disposition ${disposition.dispositionId} target is outside adapter ${target.adapterId}`
      }
      if (disposition.kind === 'intentionally-design-only') {
        return `disposition ${disposition.dispositionId} kind intentionally-design-only cannot target the project side`
      }
    }
    if (disposition.kind === 'superseded' && !disposition.supersededBy) {
      return `disposition ${disposition.dispositionId} kind superseded requires supersededBy`
    }
    if (disposition.kind !== 'superseded' && disposition.supersededBy) {
      return `disposition ${disposition.dispositionId} carries supersededBy outside kind superseded`
    }
    const key = target.side + ' ' + (target.designComponentId || target.projectComponentId)
    if (activeDispositionKeys.has(key)) {
      return `two dispositions target the same entity ${JSON.stringify(target.designComponentId || target.projectComponentId)}`
    }
    activeDispositionKeys.add(key)
    if (target.side === 'design' && activeByDesignComponent.has(target.designComponentId)) {
      return `design component ${target.designComponentId} has both an active mapping and a disposition`
    }
  }
  return null
}

// Content hash over the registry payload (revision included) used as the
// comparison input identity.
export function mappingRegistryHash(registry) {
  return canonicalHash({
    schemaVersion: registry.schemaVersion,
    revision: registry.revision,
    designScopeId: registry.designScopeId,
    mappings: registry.mappings,
    dispositions: registry.dispositions
  })
}

// The exact initial registry an explicit fresh onboarding writes. Adapter
// scope binding happens per implementation, so the empty registry only pins
// the design scope.
export function emptyMappingRegistry(designScopeId) {
  return {
    schemaVersion: 2,
    revision: 0,
    designScopeId,
    operationReceipts: [],
    mappings: [],
    dispositions: []
  }
}
