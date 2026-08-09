// Semantic contract for the design component capture and the published design
// component inventory. JSON Schema proves the outer shapes; this module proves
// the cross-field invariants: witness/page reconciliation, entity/variant
// referential integrity, sparse-variant exactness (no Cartesian expansion),
// identity uniqueness, count reconciliation, slot-derivation agreement, hash
// correctness, and the completeness/consistency gates that decide whether a
// capture may publish at all and whether it may ever prove absence.
import { createHash } from 'node:crypto'
import { canonicalHash } from '../runtime/canonical-json.mjs'
import { COMPONENT_LIMITS } from './limits.mjs'

const HASH_RE = /^sha256:[a-f0-9]{64}$/
const SCOPE_RE = /^figma:file:[a-f0-9]{16}:branch:(?:none|[a-f0-9]{16}):components:[a-f0-9]{16}$/
const DESIGN_COMPONENT_ID_RE = /^figma-component:[a-f0-9]{16}:(?:none|[a-f0-9]{16}):[A-Za-z0-9][A-Za-z0-9:;_-]{0,79}$/

const isRecord = (value) => !!value && typeof value === 'object' && !Array.isArray(value)
const short = (value) => createHash('sha256').update(String(value), 'utf8').digest('hex').slice(0, 16)

function scopeParts(providerIdentity) {
  const filePart = short(providerIdentity.fileKeyFingerprint)
  const branchPart = providerIdentity.branchKey === 'none' ? 'none' : short(providerIdentity.branchKey)
  return { filePart, branchPart }
}

export function designComponentIdFor(providerIdentity, nodeId) {
  const { filePart, branchPart } = scopeParts(providerIdentity)
  return `figma-component:${filePart}:${branchPart}:${nodeId}`
}

// Deterministic scope identity: provider + file + branch + the requested
// component scope declaration. Renaming a page does not change the scope;
// requesting a different page set does.
export function deriveScopeId(capture) {
  const { filePart, branchPart } = scopeParts(capture.providerIdentity)
  const declaration = capture.scope.kind === 'pages'
    ? { kind: 'pages', pageIds: capture.scope.pageIds.slice().sort() }
    : { kind: 'all-pages' }
  return `figma:file:${filePart}:branch:${branchPart}:components:${short(JSON.stringify(declaration))}`
}

// Cross-field invariants of a schema-valid capture. Returns null or the first
// human-actionable error string.
export function captureSemanticError(capture) {
  if (!isRecord(capture)) return 'capture must be an object'
  const witness = capture.witness
  if (Date.parse(witness.finishedAt) < Date.parse(witness.startedAt)) {
    return 'witness.finishedAt precedes witness.startedAt'
  }
  if (capture.scope.kind === 'pages' && !Array.isArray(capture.scope.pageIds)) {
    return 'scope.kind pages requires scope.pageIds'
  }
  if (capture.scope.kind === 'all-pages' && capture.scope.pageIds !== undefined) {
    return 'scope.kind all-pages must not carry pageIds'
  }

  const pagesById = new Map()
  for (const page of capture.pages) {
    if (pagesById.has(page.pageId)) return `duplicate pageId ${JSON.stringify(page.pageId)}`
    pagesById.set(page.pageId, page)
  }
  const requested = new Set(witness.requestedPageIds)
  const read = new Set(witness.readPageIds)
  for (const id of read) {
    if (!requested.has(id)) return `witness read page ${JSON.stringify(id)} was never requested`
  }
  if (capture.scope.kind === 'pages') {
    const declared = new Set(capture.scope.pageIds)
    if (declared.size !== requested.size || [...declared].some((id) => !requested.has(id))) {
      return 'witness.requestedPageIds must equal scope.pageIds exactly'
    }
  }
  for (const page of capture.pages) {
    if (!read.has(page.pageId)) return `captured page ${JSON.stringify(page.pageId)} is absent from witness.readPageIds`
  }
  for (const id of read) {
    if (!pagesById.has(id)) return `witness claims page ${JSON.stringify(id)} was read but it is not captured`
  }

  const entitiesByNode = new Map()
  const variantOwnerByNode = new Map()
  for (const entity of capture.entities) {
    if (entity.idQuality !== 'stable') {
      return `entity ${JSON.stringify(entity.nodeId)} must use provider-stable node identity`
    }
    if (entitiesByNode.has(entity.nodeId)) return `duplicate entity nodeId ${JSON.stringify(entity.nodeId)}`
    entitiesByNode.set(entity.nodeId, entity)
    if (!pagesById.has(entity.pageId)) {
      return `entity ${JSON.stringify(entity.nodeId)} references unknown page ${JSON.stringify(entity.pageId)}`
    }

    const propertyById = new Map()
    for (const property of entity.properties) {
      if (propertyById.has(property.propertyId)) {
        return `entity ${JSON.stringify(entity.nodeId)} declares duplicate propertyId ${JSON.stringify(property.propertyId)}`
      }
      propertyById.set(property.propertyId, property)
      if (property.type === 'variant') {
        if (!property.options) return `entity ${JSON.stringify(entity.nodeId)} variant property ${JSON.stringify(property.propertyId)} has no options`
        if (new Set(property.options).size !== property.options.length) {
          return `entity ${JSON.stringify(entity.nodeId)} property ${JSON.stringify(property.propertyId)} lists duplicate options`
        }
        if (property.defaultValue !== undefined) {
          if (typeof property.defaultValue !== 'string' || !property.options.includes(property.defaultValue)) {
            return `entity ${JSON.stringify(entity.nodeId)} property ${JSON.stringify(property.propertyId)} default is outside its options`
          }
        }
      } else if (property.options !== undefined) {
        return `entity ${JSON.stringify(entity.nodeId)} property ${JSON.stringify(property.propertyId)} carries options outside a variant property`
      }
      if (property.type === 'boolean' && property.defaultValue !== undefined && typeof property.defaultValue !== 'boolean') {
        return `entity ${JSON.stringify(entity.nodeId)} boolean property ${JSON.stringify(property.propertyId)} default must be boolean`
      }
      if (property.type === 'text' && property.defaultValue !== undefined && typeof property.defaultValue !== 'string') {
        return `entity ${JSON.stringify(entity.nodeId)} text property ${JSON.stringify(property.propertyId)} default must be a string`
      }
      if (property.type === 'unknown' && !property.providerType) {
        return `entity ${JSON.stringify(entity.nodeId)} unknown-type property ${JSON.stringify(property.propertyId)} must carry providerType evidence`
      }
      if (property.type !== 'instance-swap' && property.swapTargets !== undefined) {
        return `entity ${JSON.stringify(entity.nodeId)} property ${JSON.stringify(property.propertyId)} carries swapTargets outside instance-swap`
      }
    }

    const variantProperties = entity.properties.filter((property) => property.type === 'variant')
    if (entity.kind === 'component') {
      if (entity.variants.length) return `standalone component ${JSON.stringify(entity.nodeId)} must not carry variant rows`
      if (entity.expectedVariantCount !== 0) return `standalone component ${JSON.stringify(entity.nodeId)} must declare expectedVariantCount 0`
      if (entity.defaultVariantNodeId !== undefined) return `standalone component ${JSON.stringify(entity.nodeId)} must not declare a default variant`
      if (variantProperties.length) return `standalone component ${JSON.stringify(entity.nodeId)} cannot declare variant-type properties`
      continue
    }

    // component-set
    const seenAssignments = new Set()
    let defaults = 0
    for (const variant of entity.variants) {
      if (entitiesByNode.has(variant.nodeId) || variantOwnerByNode.has(variant.nodeId)) {
        return `variant nodeId ${JSON.stringify(variant.nodeId)} collides with another captured node`
      }
      variantOwnerByNode.set(variant.nodeId, entity.nodeId)
      if (variant.isDefault) defaults++
      const keys = Object.keys(variant.assignments)
      for (const propertyId of keys) {
        const property = propertyById.get(propertyId)
        if (!property || property.type !== 'variant') {
          return `variant ${JSON.stringify(variant.nodeId)} assigns undeclared variant property ${JSON.stringify(propertyId)}`
        }
        if (!property.options.includes(variant.assignments[propertyId])) {
          return `variant ${JSON.stringify(variant.nodeId)} assigns ${JSON.stringify(variant.assignments[propertyId])} outside the declared options of ${JSON.stringify(propertyId)}`
        }
      }
      if (!entity.captureIssue) {
        for (const property of variantProperties) {
          if (!Object.prototype.hasOwnProperty.call(variant.assignments, property.propertyId)) {
            return `variant ${JSON.stringify(variant.nodeId)} misses an assignment for property ${JSON.stringify(property.propertyId)}; a partial child capture must carry captureIssue`
          }
        }
        const tuple = variantProperties.map((property) => `${property.propertyId}\u0000${variant.assignments[property.propertyId]}`).join('\u0001')
        if (seenAssignments.has(tuple)) {
          return `component set ${JSON.stringify(entity.nodeId)} captures two variants with identical assignments`
        }
        seenAssignments.add(tuple)
      }
    }
    if (defaults > 1) return `component set ${JSON.stringify(entity.nodeId)} declares more than one default variant`
    if (entity.defaultVariantNodeId !== undefined &&
        !entity.variants.some((variant) => variant.nodeId === entity.defaultVariantNodeId)) {
      return `component set ${JSON.stringify(entity.nodeId)} default variant ${JSON.stringify(entity.defaultVariantNodeId)} is not among its variants`
    }
    if (!entity.captureIssue && entity.expectedVariantCount >= 0 && entity.expectedVariantCount !== entity.variants.length) {
      return `component set ${JSON.stringify(entity.nodeId)} captured ${entity.variants.length} variants but the provider reported ${entity.expectedVariantCount}; a partial child capture must carry captureIssue`
    }
    for (const ref of entity.nestedRefs) {
      if (!ref.targetNodeId && !ref.targetComponentKey) {
        return `entity ${JSON.stringify(entity.nodeId)} nested ref carries neither targetNodeId nor targetComponentKey`
      }
      if (ref.viaPropertyId !== undefined) {
        const property = propertyById.get(ref.viaPropertyId)
        if (!property || property.type !== 'instance-swap') {
          return `entity ${JSON.stringify(entity.nodeId)} nested ref cites ${JSON.stringify(ref.viaPropertyId)} which is not an instance-swap property`
        }
      }
    }
    for (const layer of entity.textLayers || []) {
      if (layer.boundTextPropertyId !== undefined) {
        const property = propertyById.get(layer.boundTextPropertyId)
        if (!property || property.type !== 'text') {
          return `entity ${JSON.stringify(entity.nodeId)} text layer ${JSON.stringify(layer.nodeId)} binds ${JSON.stringify(layer.boundTextPropertyId)} which is not a text property`
        }
      }
    }
  }
  // Standalone components may also carry nested refs / text layer checks.
  for (const entity of capture.entities) {
    if (entity.kind !== 'component') continue
    const propertyById = new Map(entity.properties.map((property) => [property.propertyId, property]))
    for (const ref of entity.nestedRefs) {
      if (!ref.targetNodeId && !ref.targetComponentKey) {
        return `entity ${JSON.stringify(entity.nodeId)} nested ref carries neither targetNodeId nor targetComponentKey`
      }
      if (ref.viaPropertyId !== undefined) {
        const property = propertyById.get(ref.viaPropertyId)
        if (!property || property.type !== 'instance-swap') {
          return `entity ${JSON.stringify(entity.nodeId)} nested ref cites ${JSON.stringify(ref.viaPropertyId)} which is not an instance-swap property`
        }
      }
    }
    for (const layer of entity.textLayers || []) {
      if (layer.boundTextPropertyId !== undefined) {
        const property = propertyById.get(layer.boundTextPropertyId)
        if (!property || property.type !== 'text') {
          return `entity ${JSON.stringify(entity.nodeId)} text layer ${JSON.stringify(layer.nodeId)} binds ${JSON.stringify(layer.boundTextPropertyId)} which is not a text property`
        }
      }
    }
  }

  let variantTotal = 0
  for (const entity of capture.entities) variantTotal += entity.variants.length
  if (variantTotal > COMPONENT_LIMITS.captureVariantsTotalMax) {
    return `capture carries ${variantTotal} variants over the ${COMPONENT_LIMITS.captureVariantsTotalMax} total limit`
  }

  const visualSeen = new Set()
  for (const entry of capture.visual) {
    const entity = entitiesByNode.get(entry.entityNodeId)
    if (!entity) return `visual entry references unknown entity ${JSON.stringify(entry.entityNodeId)}`
    if (entry.variantNodeId !== null) {
      if (variantOwnerByNode.get(entry.variantNodeId) !== entity.nodeId) {
        return `visual entry references variant ${JSON.stringify(entry.variantNodeId)} outside entity ${JSON.stringify(entry.entityNodeId)}`
      }
    }
    const key = `${entry.entityNodeId}\u0000${entry.variantNodeId || ''}\u0000${entry.role}`
    if (visualSeen.has(key)) return `duplicate visual entry for ${JSON.stringify(entry.entityNodeId)} role ${entry.role}`
    visualSeen.add(key)
  }

  if (witness.readEntityCount !== capture.entities.length) {
    return `witness.readEntityCount ${witness.readEntityCount} does not equal captured entities ${capture.entities.length}`
  }
  if (witness.expectedEntityCount >= 0 && witness.expectedEntityCount !== capture.entities.length &&
      witness.completeness === 'complete') {
    return `witness declares completeness=complete but expectedEntityCount ${witness.expectedEntityCount} differs from captured ${capture.entities.length}`
  }
  if (witness.consistency === 'proven') {
    if (!witness.providerRevisionBefore || !witness.providerRevisionAfter) {
      return 'consistency=proven requires providerRevisionBefore and providerRevisionAfter'
    }
    if (witness.providerRevisionBefore !== witness.providerRevisionAfter) {
      return 'consistency=proven requires identical before/after provider revisions'
    }
  }
  return null
}

// The publication gate (CMP-CAP-COMPLETE / CMP-CAP-EMPTY). Returns
// { ok:true, absenceProofEligible } or { ok:false, code, detail }.
export function captureCompletenessGate(capture) {
  const witness = capture.witness
  if (witness.truncated) {
    return { ok: false, code: 'COMPONENT_DESIGN_CAPTURE_INCOMPLETE', detail: 'capture reports truncated enumeration' }
  }
  if (witness.permissionDegraded) {
    return { ok: false, code: 'COMPONENT_DESIGN_ACCESS_DEGRADED', detail: 'capture reports degraded permissions' }
  }
  if (witness.limitsHit.length) {
    return { ok: false, code: 'COMPONENT_DESIGN_CAPTURE_INCOMPLETE', detail: `capture hit limits: ${witness.limitsHit.join(', ')}` }
  }
  if (witness.completeness !== 'complete') {
    return { ok: false, code: 'COMPONENT_DESIGN_CAPTURE_INCOMPLETE', detail: 'capture completeness is not complete' }
  }
  const read = new Set(witness.readPageIds)
  for (const id of witness.requestedPageIds) {
    if (!read.has(id)) {
      return { ok: false, code: 'COMPONENT_DESIGN_CAPTURE_INCOMPLETE', detail: `requested page ${id} was not read` }
    }
  }
  if (capture.entities.length === 0) {
    // A valid authoritative empty scope needs the strengthened witness: the
    // provider stated a zero count itself; exhausted traversal alone is not
    // proof that nothing existed.
    if (witness.expectedEntityCount !== 0) {
      return { ok: false, code: 'COMPONENT_DESIGN_CAPTURE_INCOMPLETE', detail: 'empty capture without an authoritative zero entity count' }
    }
  }
  return { ok: true, absenceProofEligible: witness.consistency === 'proven' }
}

// Canonical structural projection: the semantic anchor for token-caused-change
// causality (§17.12) and for rename detection. Display metadata (name,
// description, page) and visual evidence are excluded deliberately.
function structuralProjectionOf(component) {
  return {
    kind: component.kind,
    properties: component.properties.map((property) => ({
      propertyId: property.propertyId,
      type: property.type,
      options: property.options || null,
      defaultValue: property.defaultValue === undefined ? null : property.defaultValue,
      defaultKnown: property.defaultKnown === true,
      swapTargets: (property.swapTargets || []).map((target) => ({
        targetDesignComponentId: target.targetDesignComponentId || null,
        targetNodeId: target.targetNodeId || null,
        targetComponentKey: target.targetComponentKey || null
      }))
    })),
    unsupportedProperties: component.unsupportedProperties.map((property) => property.propertyId),
    variants: component.variants.map((variant) => ({
      variantId: variant.variantId,
      assignments: variant.assignments,
      isDefault: variant.isDefault
    })),
    defaultVariantId: component.defaultVariantId,
    defaultKnown: component.defaultKnown,
    semanticSlots: component.semanticSlots.map((slot) => ({ slotId: slot.slotId, kind: slot.kind })),
    dependencies: component.dependencies.map((dependency) => ({
      targetDesignComponentId: dependency.targetDesignComponentId || null,
      targetNodeId: dependency.targetNodeId || null,
      targetComponentKey: dependency.targetComponentKey || null,
      viaPropertyId: dependency.viaPropertyId || null,
      swappable: dependency.swappable,
      resolved: dependency.resolved
    })),
    tokenRefs: component.tokenRefs.map((ref) => ({
      observedTokenKey: ref.observedTokenKey,
      contextKey: ref.contextKey,
      sourceId: ref.sourceId,
      providerName: ref.providerName,
      field: ref.field
    }))
  }
}

export function structuralHashOf(component) {
  return canonicalHash(structuralProjectionOf(component))
}

export function sourceHashOf(component) {
  return canonicalHash({
    designComponentId: component.designComponentId,
    name: component.name,
    description: component.description || '',
    page: component.page,
    library: component.library,
    structural: structuralProjectionOf(component),
    visualEvidence: component.visualEvidence,
    autoLayout: component.autoLayout || null
  })
}

// Cross-field invariants of a schema-valid published inventory.
export function inventorySemanticError(inventory) {
  if (!isRecord(inventory)) return 'inventory must be an object'
  if (!SCOPE_RE.test(inventory.scopeId)) return 'scopeId is not canonical'
  if (inventory.capture.absenceProofEligible !== (inventory.capture.consistency === 'proven')) {
    return 'capture.absenceProofEligible must equal (consistency === proven)'
  }

  const ids = new Set()
  let setCount = 0
  let standaloneCount = 0
  let variantTotal = 0
  for (const component of inventory.components) {
    if (ids.has(component.designComponentId)) return `duplicate designComponentId ${JSON.stringify(component.designComponentId)}`
    ids.add(component.designComponentId)
    if (component.designComponentId !== designComponentIdFor(inventory.providerIdentity, component.providerIdentity.nodeId)) {
      return `designComponentId ${JSON.stringify(component.designComponentId)} does not derive from its provider node id`
    }
    if (component.providerIdentity.idQuality !== 'stable') {
      return `component ${JSON.stringify(component.designComponentId)} must use provider-stable node identity`
    }
    if (component.kind === 'component-set') setCount++
    else standaloneCount++
    variantTotal += component.variants.length

    const propertyById = new Map()
    for (const property of component.properties) {
      if (propertyById.has(property.propertyId)) {
        return `component ${JSON.stringify(component.designComponentId)} declares duplicate propertyId ${JSON.stringify(property.propertyId)}`
      }
      propertyById.set(property.propertyId, property)
      if (property.type === 'variant' && !property.options) {
        return `component ${JSON.stringify(component.designComponentId)} variant property ${JSON.stringify(property.propertyId)} has no options`
      }
      if (property.defaultKnown === true && property.defaultValue === undefined) {
        return `component ${JSON.stringify(component.designComponentId)} property ${JSON.stringify(property.propertyId)} claims a known default without a value`
      }
    }
    for (const property of component.unsupportedProperties) {
      if (propertyById.has(property.propertyId)) {
        return `component ${JSON.stringify(component.designComponentId)} lists ${JSON.stringify(property.propertyId)} as both supported and unsupported`
      }
    }

    if (component.kind === 'component') {
      if (component.variants.length) return `standalone ${JSON.stringify(component.designComponentId)} must not carry variant rows`
      if (component.defaultVariantId !== null) return `standalone ${JSON.stringify(component.designComponentId)} must carry defaultVariantId null`
      if (component.properties.some((property) => property.type === 'variant')) {
        return `standalone ${JSON.stringify(component.designComponentId)} cannot declare variant-type properties`
      }
    } else {
      const variantIds = new Set()
      let defaults = 0
      const variantProperties = component.properties.filter((property) => property.type === 'variant')
      for (const variant of component.variants) {
        if (variant.variantId !== variant.nodeId) return `variant ${JSON.stringify(variant.variantId)} must equal its nodeId`
        if (variantIds.has(variant.variantId)) return `component ${JSON.stringify(component.designComponentId)} lists duplicate variant ${JSON.stringify(variant.variantId)}`
        variantIds.add(variant.variantId)
        if (variant.isDefault) defaults++
        for (const propertyId of Object.keys(variant.assignments)) {
          const property = propertyById.get(propertyId)
          if (!property || property.type !== 'variant') {
            return `variant ${JSON.stringify(variant.variantId)} assigns undeclared variant property ${JSON.stringify(propertyId)}`
          }
          if (!property.options.includes(variant.assignments[propertyId])) {
            return `variant ${JSON.stringify(variant.variantId)} assigns a value outside the options of ${JSON.stringify(propertyId)}`
          }
        }
        for (const property of variantProperties) {
          if (!Object.prototype.hasOwnProperty.call(variant.assignments, property.propertyId)) {
            return `variant ${JSON.stringify(variant.variantId)} misses an assignment for ${JSON.stringify(property.propertyId)}`
          }
        }
      }
      if (defaults > 1) return `component ${JSON.stringify(component.designComponentId)} declares more than one default variant`
      if (component.defaultVariantId !== null && !variantIds.has(component.defaultVariantId)) {
        return `component ${JSON.stringify(component.designComponentId)} default variant is not among its variants`
      }
      if (component.defaultKnown !== (component.defaultVariantId !== null)) {
        return `component ${JSON.stringify(component.designComponentId)} defaultKnown disagrees with defaultVariantId`
      }
    }

    const slotIds = new Set()
    for (const slot of component.semanticSlots) {
      if (slotIds.has(slot.slotId)) return `component ${JSON.stringify(component.designComponentId)} declares duplicate slot ${JSON.stringify(slot.slotId)}`
      slotIds.add(slot.slotId)
      if (slot.kind === 'text-property' || slot.kind === 'instance-swap') {
        const property = propertyById.get(slot.propertyId)
        const expected = slot.kind === 'text-property' ? 'text' : 'instance-swap'
        if (!property || property.type !== expected) {
          return `slot ${JSON.stringify(slot.slotId)} cites ${JSON.stringify(slot.propertyId)} which is not a ${expected} property`
        }
        if (slot.slotId !== `prop:${slot.propertyId}`) return `slot ${JSON.stringify(slot.slotId)} id does not derive from its property`
      }
      if (slot.kind === 'static-text' && (!slot.layerNodeId || slot.slotId !== `layer:${slot.layerNodeId}`)) {
        return `static-text slot ${JSON.stringify(slot.slotId)} must derive from its layer node`
      }
    }
    for (const property of component.properties) {
      if (property.type === 'text' && !slotIds.has(`prop:${property.propertyId}`)) {
        return `text property ${JSON.stringify(property.propertyId)} of ${JSON.stringify(component.designComponentId)} has no derived slot`
      }
      if (property.type === 'instance-swap' && !slotIds.has(`prop:${property.propertyId}`)) {
        return `instance-swap property ${JSON.stringify(property.propertyId)} of ${JSON.stringify(component.designComponentId)} has no derived slot`
      }
    }

    for (const dependency of component.dependencies) {
      if (dependency.resolved && !dependency.targetDesignComponentId) {
        return `component ${JSON.stringify(component.designComponentId)} carries a resolved dependency without a target id`
      }
      if (!dependency.resolved && dependency.targetDesignComponentId) {
        return `component ${JSON.stringify(component.designComponentId)} carries an unresolved dependency with a target id`
      }
      if (dependency.targetDesignComponentId && !DESIGN_COMPONENT_ID_RE.test(dependency.targetDesignComponentId)) {
        return `component ${JSON.stringify(component.designComponentId)} dependency target id is malformed`
      }
    }

    const visual = component.visualEvidence
    if ((visual.coverage === 'none') !== (visual.entries.length === 0)) {
      return `component ${JSON.stringify(component.designComponentId)} visual coverage disagrees with its entries`
    }
    for (const entry of visual.entries) {
      if (entry.variantId !== null && component.kind === 'component-set' &&
          !component.variants.some((variant) => variant.variantId === entry.variantId)) {
        return `component ${JSON.stringify(component.designComponentId)} visual entry cites unknown variant ${JSON.stringify(entry.variantId)}`
      }
    }

    if (structuralHashOf(component) !== component.structuralHash) {
      return `component ${JSON.stringify(component.designComponentId)} structuralHash does not match its structure`
    }
    if (sourceHashOf(component) !== component.sourceHash) {
      return `component ${JSON.stringify(component.designComponentId)} sourceHash does not match its content`
    }
  }

  for (const item of inventory.unsupportedComponents) {
    if (ids.has(item.designComponentId)) {
      return `unsupported entry duplicates designComponentId ${JSON.stringify(item.designComponentId)}`
    }
    ids.add(item.designComponentId)
    if (item.designComponentId !== designComponentIdFor(inventory.providerIdentity, item.providerIdentity.nodeId)) {
      return `unsupported designComponentId ${JSON.stringify(item.designComponentId)} does not derive from its provider node id`
    }
    if (item.providerIdentity.idQuality !== 'stable') {
      return `unsupported component ${JSON.stringify(item.designComponentId)} must use provider-stable node identity`
    }
  }

  // Resolved dependency targets must exist in the captured scope.
  for (const component of inventory.components) {
    for (const dependency of component.dependencies) {
      if (dependency.resolved && !ids.has(dependency.targetDesignComponentId)) {
        return `component ${JSON.stringify(component.designComponentId)} resolved dependency targets ${JSON.stringify(dependency.targetDesignComponentId)} outside the inventory`
      }
    }
  }

  const counts = inventory.counts
  if (counts.components !== inventory.components.length) return 'counts.components does not match components length'
  if (counts.componentSets !== setCount) return 'counts.componentSets does not match'
  if (counts.standaloneComponents !== standaloneCount) return 'counts.standaloneComponents does not match'
  if (counts.unsupportedComponents !== inventory.unsupportedComponents.length) return 'counts.unsupportedComponents does not match'
  if (counts.variants !== variantTotal) return 'counts.variants does not match the variant total'
  if (counts.capturedEntities !== counts.components + counts.unsupportedComponents) {
    return 'counts.capturedEntities must equal components + unsupportedComponents'
  }
  if (counts.components > COMPONENT_LIMITS.captureEntitiesMax) return 'counts.components exceeds the capture entity limit'
  if (!HASH_RE.test(inventory.capture.captureHash)) return 'capture.captureHash malformed'
  return null
}

// Content hash of the semantic inventory payload (identity for carry-forward,
// comparison inputs and staleness checks). Capture timing metadata is part of
// the artifact but not of the semantic identity.
export function inventorySemanticHash(inventory) {
  return canonicalHash({
    schemaVersion: inventory.schemaVersion,
    provider: inventory.provider,
    scopeId: inventory.scopeId,
    providerIdentity: inventory.providerIdentity,
    components: inventory.components,
    unsupportedComponents: inventory.unsupportedComponents,
    counts: inventory.counts
  })
}
