import { canonicalHash, canonicalJson } from '../runtime/canonical-json.mjs'

const isRecord = (value) => !!value && typeof value === 'object' && !Array.isArray(value)

function selectorKey(selector) {
  return canonicalJson(selector || {})
}

export function mappingRegistrySemanticError(registry) {
  if (!isRecord(registry) || registry.schemaVersion !== 2) return 'registry must use schemaVersion 2'
  const receiptIds = new Set()
  for (const receipt of registry.operationReceipts) {
    if (receiptIds.has(receipt.operationId)) return `duplicate operation receipt ${receipt.operationId}`
    receiptIds.add(receipt.operationId)
    if (receipt.revision > registry.revision) return `operation receipt ${receipt.operationId} exceeds registry revision`
  }
  const mappingIds = new Set()
  const activeAuthorities = new Map()
  for (const mapping of registry.mappings) {
    if (mappingIds.has(mapping.mappingId)) return `duplicate mappingId ${mapping.mappingId}`
    mappingIds.add(mapping.mappingId)
    const key = mapping.observedTokenKey + '\u0000' + selectorKey(mapping.contextSelector)
    if (mapping.state === 'active') {
      if (activeAuthorities.has(key)) return `two active mappings claim ${mapping.observedTokenKey} and the same context`
      activeAuthorities.set(key, mapping.mappingId)
    }
    if (mapping.projectTokenIds.some((id) => !id.startsWith(mapping.adapterId + ':'))) {
      return `mapping ${mapping.mappingId} targets a project token outside adapter ${mapping.adapterId}`
    }
    const count = mapping.projectTokenIds.length
    if ((mapping.relation === 'one-to-one' || mapping.relation === 'alias' || mapping.relation === 'transform') && count !== 1) {
      return `mapping ${mapping.mappingId} relation ${mapping.relation} requires exactly one target`
    }
    if ((mapping.relation === 'one-to-many' || mapping.relation === 'many-to-one') && count < 2) {
      return `mapping ${mapping.mappingId} relation ${mapping.relation} requires multiple targets`
    }
    if (mapping.relation === 'transform') {
      if (!mapping.transform) return `mapping ${mapping.mappingId} transform relation requires transform config`
      if (mapping.transform.kind === 'number-scale') {
        if (mapping.expectedKind !== 'number' || !Number.isFinite(mapping.transform.factor) || Object.is(mapping.transform.factor, -0)) {
          return `mapping ${mapping.mappingId} has invalid number-scale transform`
        }
      }
      if (mapping.transform.kind === 'color-alpha' &&
          (mapping.expectedKind !== 'color' || typeof mapping.transform.alpha !== 'number')) {
        return `mapping ${mapping.mappingId} has invalid color-alpha transform`
      }
    } else if (mapping.transform) {
      return `mapping ${mapping.mappingId} carries transform outside transform relation`
    }
    if (mapping.state === 'retired' && !mapping.retirement) return `mapping ${mapping.mappingId} retired without retirement record`
    if (mapping.state === 'active' && mapping.retirement) return `mapping ${mapping.mappingId} active with retirement record`
    if (mapping.provenance.kind === 'design-task-finalization' && !mapping.provenance.taskStem) {
      return `mapping ${mapping.mappingId} task provenance requires taskStem`
    }
  }
  const dispositionIds = new Set()
  const dispositionTargets = new Set()
  for (const disposition of registry.dispositions) {
    if (dispositionIds.has(disposition.dispositionId)) return `duplicate dispositionId ${disposition.dispositionId}`
    dispositionIds.add(disposition.dispositionId)
    const target = disposition.target.side === 'observed'
      ? disposition.target.observedTokenKey
      : disposition.target.adapterId + '\u0000' + disposition.target.projectTokenId
    const key = disposition.target.side + '\u0000' + target
    if (dispositionTargets.has(key)) return `two dispositions claim ${target}`
    dispositionTargets.add(key)
    if (disposition.reviewPolicy === 'manual-date' && !disposition.reviewAt) {
      return `disposition ${disposition.dispositionId} manual-date requires reviewAt`
    }
    if (disposition.reviewPolicy !== 'manual-date' && disposition.reviewAt) {
      return `disposition ${disposition.dispositionId} carries reviewAt outside manual-date`
    }
    if (disposition.target.side === 'observed' &&
        [...activeAuthorities.keys()].some((authority) => authority.startsWith(disposition.target.observedTokenKey + '\u0000'))) {
      return `observed token ${disposition.target.observedTokenKey} has both active mapping and disposition`
    }
  }
  return null
}

export function mappingRegistryHash(registry) {
  return canonicalHash({
    schemaVersion: registry.schemaVersion,
    revision: registry.revision,
    scope: registry.scope,
    mappings: registry.mappings,
    dispositions: registry.dispositions
  })
}

export function emptyMappingRegistry(scope) {
  return {
    schemaVersion: 2,
    revision: 0,
    scope,
    operationReceipts: [],
    mappings: [],
    dispositions: []
  }
}
