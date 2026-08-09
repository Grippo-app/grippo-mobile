// Semantic contract for per-adapter project component inventories and the
// analysis index. JSON Schema proves shapes; this module proves identity
// uniqueness (including case-folded collisions), referential integrity of
// variant properties/slots against the declared API, dependency target
// integrity, witness/count reconciliation, and index/inventory agreement.
import { canonicalHash } from '../runtime/canonical-json.mjs'
import { COMPONENT_LIMITS } from './limits.mjs'

const isRecord = (value) => !!value && typeof value === 'object' && !Array.isArray(value)

function semanticComponent(component) {
  // Keep the exact fileHash in the published evidence artifact, but exclude
  // it from component semantics so line-ending-only byte changes cannot
  // create API/behavior drift.
  const source = { ...component.source }
  delete source.fileHash
  return { ...component, source }
}

function projectComponentIdFor(adapterId, fqSymbol, overloadDiscriminator) {
  return `${adapterId}:symbol:${fqSymbol}${overloadDiscriminator ? `#${overloadDiscriminator}` : ''}`
}

export function projectInventorySemanticError(inventory) {
  if (!isRecord(inventory)) return 'inventory must be an object'

  const ids = new Set()
  const caseFolded = new Map()
  let variantPropertyTotal = 0
  let slotTotal = 0
  let dependencyTotal = 0
  for (const component of inventory.components) {
    if (!component.projectComponentId.startsWith(inventory.adapterId + ':symbol:')) {
      return `projectComponentId ${JSON.stringify(component.projectComponentId)} is outside adapter ${inventory.adapterId}`
    }
    if (ids.has(component.projectComponentId)) return `duplicate projectComponentId ${JSON.stringify(component.projectComponentId)}`
    ids.add(component.projectComponentId)
    const folded = component.projectComponentId.toLowerCase()
    if (caseFolded.has(folded) && caseFolded.get(folded) !== component.projectComponentId) {
      return `case-folded identity collision between ${JSON.stringify(caseFolded.get(folded))} and ${JSON.stringify(component.projectComponentId)}`
    }
    caseFolded.set(folded, component.projectComponentId)

    const expectedId = projectComponentIdFor(
      inventory.adapterId,
      component.fqName || component.name,
      component.overloadDiscriminator
    )
    if (component.projectComponentId !== expectedId) {
      return `projectComponentId ${JSON.stringify(component.projectComponentId)} does not derive from its symbol identity`
    }

    const parameterByName = new Map()
    for (const parameter of component.api.parameters) {
      if (parameterByName.has(parameter.name)) {
        return `component ${JSON.stringify(component.projectComponentId)} declares duplicate parameter ${JSON.stringify(parameter.name)}`
      }
      parameterByName.set(parameter.name, parameter)
      if (parameter.required && parameter.hasDefault) {
        return `component ${JSON.stringify(component.projectComponentId)} parameter ${JSON.stringify(parameter.name)} cannot be required and defaulted at once`
      }
    }

    const propertyIds = new Set()
    for (const property of component.variantProperties) {
      variantPropertyTotal++
      if (propertyIds.has(property.projectPropertyId)) {
        return `component ${JSON.stringify(component.projectComponentId)} declares duplicate variant property ${JSON.stringify(property.projectPropertyId)}`
      }
      propertyIds.add(property.projectPropertyId)
      const parameterName = property.projectPropertyId.slice('param:'.length)
      if (!parameterByName.has(parameterName)) {
        return `variant property ${JSON.stringify(property.projectPropertyId)} cites a parameter absent from the API of ${JSON.stringify(component.projectComponentId)}`
      }
      const values = property.values.map((row) => row.value)
      if (new Set(values).size !== values.length) {
        return `variant property ${JSON.stringify(property.projectPropertyId)} lists duplicate values`
      }
      if (property.source === 'boolean') {
        const allowed = new Set(['true', 'false'])
        if (!values.every((value) => allowed.has(value))) {
          return `boolean variant property ${JSON.stringify(property.projectPropertyId)} may only carry true/false values`
        }
      }
      if (property.defaultKnown && property.defaultValue === undefined) {
        return `variant property ${JSON.stringify(property.projectPropertyId)} claims a known default without a value`
      }
      if (property.defaultValue !== undefined && !values.includes(property.defaultValue)) {
        return `variant property ${JSON.stringify(property.projectPropertyId)} default is outside its values`
      }
    }

    const slotIds = new Set()
    for (const slot of component.slots) {
      slotTotal++
      if (slotIds.has(slot.slotId)) {
        return `component ${JSON.stringify(component.projectComponentId)} declares duplicate slot ${JSON.stringify(slot.slotId)}`
      }
      slotIds.add(slot.slotId)
      const parameterName = slot.slotId.slice('param:'.length)
      const parameter = parameterByName.get(parameterName)
      if (!parameter) {
        return `slot ${JSON.stringify(slot.slotId)} cites a parameter absent from the API of ${JSON.stringify(component.projectComponentId)}`
      }
      if (propertyIds.has(slot.slotId)) {
        return `parameter ${JSON.stringify(parameterName)} of ${JSON.stringify(component.projectComponentId)} is claimed as both a variant property and a slot`
      }
      if (slot.required !== parameter.required) {
        return `slot ${JSON.stringify(slot.slotId)} requiredness disagrees with its parameter`
      }
    }

    for (const dependency of component.dependencies) {
      dependencyTotal++
      if (dependency.kind === 'component' && !dependency.targetProjectComponentId && !dependency.symbol) {
        return `component dependency of ${JSON.stringify(component.projectComponentId)} names no target`
      }
      if (dependency.kind === 'token' && !dependency.projectTokenId) {
        return `token dependency of ${JSON.stringify(component.projectComponentId)} names no project token`
      }
      if (dependency.kind === 'framework' && !dependency.symbol) {
        return `framework dependency of ${JSON.stringify(component.projectComponentId)} names no symbol`
      }
    }
    if (component.wrapperOf !== undefined) {
      if (component.wrapperOf === component.projectComponentId) {
        return `component ${JSON.stringify(component.projectComponentId)} cannot wrap itself`
      }
    }
  }

  // Wrapper targets and in-scope component dependency targets must exist.
  for (const component of inventory.components) {
    if (component.wrapperOf !== undefined && !ids.has(component.wrapperOf)) {
      return `component ${JSON.stringify(component.projectComponentId)} wraps ${JSON.stringify(component.wrapperOf)} which is not in this inventory`
    }
    for (const dependency of component.dependencies) {
      if (dependency.kind === 'component' && dependency.targetProjectComponentId && !ids.has(dependency.targetProjectComponentId)) {
        return `component ${JSON.stringify(component.projectComponentId)} depends on ${JSON.stringify(dependency.targetProjectComponentId)} which is not in this inventory`
      }
    }
  }

  const witness = inventory.witness
  if (witness.rootsResolved !== witness.rootsConfigured - witness.rootsMissing.length) {
    return 'witness.rootsResolved does not reconcile with configured/missing roots'
  }
  if (witness.filesParsed > witness.filesMatched) return 'witness.filesParsed exceeds filesMatched'
  const expectedComplete = witness.rootsMissing.length === 0 &&
    witness.parseFailures.length === 0 &&
    witness.limitsHit.length === 0 &&
    witness.filesParsed === witness.filesMatched
  if (witness.complete !== expectedComplete) {
    return 'witness.complete disagrees with its own evidence'
  }

  const counts = inventory.counts
  if (counts.components !== inventory.components.length) return 'counts.components does not match components length'
  if (counts.variantProperties !== variantPropertyTotal) return 'counts.variantProperties does not match'
  if (counts.slots !== slotTotal) return 'counts.slots does not match'
  if (counts.dependencyEdges !== dependencyTotal) return 'counts.dependencyEdges does not match'
  if (counts.components > COMPONENT_LIMITS.projectComponentsMax) return 'counts.components exceeds the project component limit'
  return null
}

// Content hash of the semantic inventory payload (identity for staleness
// checks and index binding).
export function projectInventorySemanticHash(inventory) {
  return canonicalHash({
    schemaVersion: inventory.schemaVersion,
    adapterId: inventory.adapterId,
    adapterKind: inventory.adapterKind,
    adapterVersion: inventory.adapterVersion,
    platform: inventory.platform,
    scopeFingerprint: inventory.scopeFingerprint,
    components: inventory.components.map(semanticComponent),
    witness: inventory.witness,
    counts: inventory.counts
  })
}

export function analysisIndexSemanticError(index, inventoriesByAdapterId, context) {
  if (!isRecord(index)) return 'analysis index must be an object'
  if (context && context.configHash && index.configHash !== context.configHash) {
    return 'analysis index configHash does not match the validated adapter config'
  }
  const seen = new Set()
  for (const row of index.adapters) {
    if (seen.has(row.adapterId)) return `analysis index lists adapter ${JSON.stringify(row.adapterId)} twice`
    seen.add(row.adapterId)
    if (row.role !== `project-component-inventory:${row.adapterId}`) {
      return `analysis index role ${JSON.stringify(row.role)} does not derive from its adapterId`
    }
    const inventory = inventoriesByAdapterId.get(row.adapterId)
    if (!inventory) return `analysis index cites adapter ${JSON.stringify(row.adapterId)} without an inventory`
    if (projectInventorySemanticHash(inventory) !== row.inventoryHash) {
      return `analysis index hash for ${JSON.stringify(row.adapterId)} does not match its inventory`
    }
    if (inventory.scopeFingerprint !== row.scopeFingerprint) {
      return `analysis index scope fingerprint for ${JSON.stringify(row.adapterId)} does not match its inventory`
    }
    if (inventory.platform !== row.platform) {
      return `analysis index platform for ${JSON.stringify(row.adapterId)} does not match its inventory`
    }
    if (inventory.witness.complete !== row.complete) {
      return `analysis index completeness for ${JSON.stringify(row.adapterId)} does not match its inventory witness`
    }
  }
  if (context && Array.isArray(context.enabledAdapterIds)) {
    for (const adapterId of context.enabledAdapterIds) {
      if (!seen.has(adapterId)) return `analysis index misses enabled adapter ${JSON.stringify(adapterId)}`
    }
    if (index.adapters.length !== context.enabledAdapterIds.length) {
      return 'analysis index lists adapters outside the enabled set'
    }
  }
  const expectedComplete = index.adapters.every((row) => row.complete)
  if (index.complete !== expectedComplete) return 'analysis index complete flag disagrees with its rows'
  return null
}
