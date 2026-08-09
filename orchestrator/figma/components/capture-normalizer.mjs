// Deterministic capture normalizer: validated staged capture -> published
// design component inventory. Pure data transformation — no filesystem, no
// clock, no provider access. Sparse variant tuples pass through exactly as the
// provider declared them; anything the capture cannot prove stays unsupported
// with a reason instead of becoming a guessed structure (REQ-FIGMA-004).
import { typedError } from '../runtime/typed-error.mjs'
import { compareText } from '../runtime/canonical-json.mjs'
import { COMPONENT_ERROR_CODES } from './error-codes.mjs'
import {
  captureSemanticError, captureCompletenessGate, deriveScopeId, designComponentIdFor,
  inventorySemanticError, structuralHashOf, sourceHashOf
} from './design-inventory-contract.mjs'

function normalizeProperty(property, resolveTarget) {
  const normalized = {
    propertyId: property.propertyId,
    name: property.name,
    type: property.type,
    idQuality: property.idQuality
  }
  if (property.options) normalized.options = property.options.slice()
  if (property.defaultValue !== undefined) {
    normalized.defaultValue = property.defaultValue
    normalized.defaultKnown = true
  } else {
    normalized.defaultKnown = false
  }
  if (property.swapTargets) {
    normalized.swapTargets = property.swapTargets.map((target) => {
      const out = {}
      const resolved = target.targetNodeId ? resolveTarget(target.targetNodeId) : null
      if (resolved) out.targetDesignComponentId = resolved.designComponentId
      if (target.targetNodeId) out.targetNodeId = target.targetNodeId
      if (target.targetComponentKey) out.targetComponentKey = target.targetComponentKey
      if (target.remote === true) out.remote = true
      return out
    })
  }
  if (property.exposedFromNestedInstance === true) normalized.exposedFromNestedInstance = true
  return normalized
}

function deriveSlots(entity, properties) {
  const slots = []
  for (const property of properties) {
    if (property.type === 'text') {
      slots.push({ slotId: `prop:${property.propertyId}`, kind: 'text-property', name: property.name, propertyId: property.propertyId })
    } else if (property.type === 'instance-swap') {
      slots.push({ slotId: `prop:${property.propertyId}`, kind: 'instance-swap', name: property.name, propertyId: property.propertyId })
    }
  }
  const boundLayers = new Set()
  for (const layer of entity.textLayers || []) {
    if (layer.boundTextPropertyId !== undefined) { boundLayers.add(layer.nodeId); continue }
    slots.push({ slotId: `layer:${layer.nodeId}`, kind: 'static-text', name: layer.name, layerNodeId: layer.nodeId })
  }
  return slots
}

// capture (schema-valid) + the sha256 of its staged bytes -> inventory |
// throws TypedError. captureHash is supplied by the caller that read the
// staged file so the published witness stays bound to exact capture bytes.
export function normalizeCapture(capture, captureHash) {
  if (!/^sha256:[a-f0-9]{64}$/.test(String(captureHash || ''))) {
    throw typedError(COMPONENT_ERROR_CODES.COMPONENT_DESIGN_CAPTURE_INVALID, 'captureHash must be a sha256 content hash of the staged capture bytes')
  }
  const semantic = captureSemanticError(capture)
  if (semantic) throw typedError(COMPONENT_ERROR_CODES.COMPONENT_DESIGN_CAPTURE_INVALID, semantic)
  const gate = captureCompletenessGate(capture)
  if (!gate.ok) throw typedError(gate.code, gate.detail)

  const pagesById = new Map(capture.pages.map((page) => [page.pageId, page]))
  const entityByNode = new Map()
  const ownerByVariantNode = new Map()
  for (const entity of capture.entities) {
    entityByNode.set(entity.nodeId, entity)
    for (const variant of entity.variants) ownerByVariantNode.set(variant.nodeId, entity)
  }
  const resolveTarget = (targetNodeId) => {
    const direct = entityByNode.get(targetNodeId)
    if (direct) return { designComponentId: designComponentIdFor(capture.providerIdentity, direct.nodeId) }
    const owner = ownerByVariantNode.get(targetNodeId)
    if (owner) {
      return {
        designComponentId: designComponentIdFor(capture.providerIdentity, owner.nodeId),
        targetVariantId: targetNodeId
      }
    }
    return null
  }
  const visualByEntity = new Map()
  for (const entry of capture.visual) {
    let bucket = visualByEntity.get(entry.entityNodeId)
    if (!bucket) { bucket = []; visualByEntity.set(entry.entityNodeId, bucket) }
    bucket.push(entry)
  }

  const components = []
  const unsupportedComponents = []
  for (const entity of capture.entities) {
    const page = pagesById.get(entity.pageId)
    const base = {
      designComponentId: designComponentIdFor(capture.providerIdentity, entity.nodeId),
      providerIdentity: { nodeId: entity.nodeId, idQuality: entity.idQuality },
      kind: entity.kind,
      name: entity.name,
      page: { pageId: page.pageId, name: page.name }
    }
    if (entity.remote === true) {
      unsupportedComponents.push({ ...base, reason: 'remote component is referenced-only under the local-authoritative policy' })
      continue
    }
    if (entity.captureIssue) {
      unsupportedComponents.push({ ...base, reason: entity.captureIssue.slice(0, 300) })
      continue
    }

    const supportedProperties = []
    const unsupportedProperties = []
    for (const property of entity.properties) {
      if (property.type === 'unknown') {
        unsupportedProperties.push({
          propertyId: property.propertyId,
          name: property.name,
          ...(property.providerType ? { providerType: property.providerType } : {}),
          reason: `provider property type ${property.providerType || 'unknown'} is not supported`
        })
        continue
      }
      supportedProperties.push(normalizeProperty(property, resolveTarget))
    }

    let defaultVariantId = null
    if (entity.kind === 'component-set') {
      if (entity.defaultVariantNodeId !== undefined) defaultVariantId = entity.defaultVariantNodeId
      else {
        const declared = entity.variants.filter((variant) => variant.isDefault === true)
        if (declared.length === 1) defaultVariantId = declared[0].nodeId
      }
    }
    const variants = entity.kind === 'component-set'
      ? entity.variants
        .slice()
        .sort((a, b) => compareText(a.nodeId, b.nodeId))
        .map((variant) => ({
          variantId: variant.nodeId,
          nodeId: variant.nodeId,
          name: variant.name,
          assignments: Object.fromEntries(Object.keys(variant.assignments).sort().map((key) => [key, variant.assignments[key]])),
          isDefault: variant.nodeId === defaultVariantId
        }))
      : []

    const dependencies = entity.nestedRefs.map((ref) => {
      const resolved = ref.targetNodeId ? resolveTarget(ref.targetNodeId) : null
      const dependency = { kind: 'component', swappable: ref.swappable, resolved: !!resolved }
      if (resolved) {
        dependency.targetDesignComponentId = resolved.designComponentId
        if (resolved.targetVariantId) dependency.targetVariantId = resolved.targetVariantId
      }
      if (ref.targetNodeId) dependency.targetNodeId = ref.targetNodeId
      if (ref.targetComponentKey) dependency.targetComponentKey = ref.targetComponentKey
      if (ref.viaPropertyId !== undefined) dependency.viaPropertyId = ref.viaPropertyId
      if (ref.layerName !== undefined) dependency.layerName = ref.layerName
      if (ref.remote === true) dependency.remote = true
      return dependency
    })

    const slots = deriveSlots(entity, supportedProperties)
    for (const dependency of dependencies) {
      if (dependency.swappable || !dependency.resolved) continue
      const anchor = dependency.targetNodeId || dependency.targetComponentKey
      const slotId = `nested:${anchor}`
      if (slots.some((slot) => slot.slotId === slotId)) continue
      slots.push({ slotId, kind: 'nested-component', name: dependency.layerName || anchor })
    }

    const tokenRefSeen = new Set()
    const tokenRefs = []
    for (const bound of entity.boundVariables) {
      const ref = {
        observedTokenKey: bound.observedTokenKey,
        contextKey: bound.contextKey,
        sourceId: bound.sourceId,
        providerName: bound.providerName,
        field: bound.field
      }
      if (bound.layerName !== undefined) ref.layerName = bound.layerName
      if (bound.remote === true) ref.remote = true
      const key = `${ref.observedTokenKey} ${ref.contextKey} ${ref.field} ${ref.layerName || ''}`
      if (tokenRefSeen.has(key)) continue
      tokenRefSeen.add(key)
      tokenRefs.push(ref)
    }
    tokenRefs.sort((a, b) =>
      compareText(a.observedTokenKey, b.observedTokenKey) ||
      compareText(a.contextKey, b.contextKey) ||
      compareText(a.field, b.field))

    const visualEntries = (visualByEntity.get(entity.nodeId) || [])
      .slice()
      .sort((a, b) => compareText(a.variantNodeId || '', b.variantNodeId || '') || compareText(a.role, b.role))
      .map((entry) => ({ variantId: entry.variantNodeId, role: entry.role, imageHash: entry.sha256 }))
    const coverage = visualEntries.length === 0
      ? 'none'
      : visualEntries.some((entry) => entry.role === 'default' || entry.role === 'representative') ? 'representative' : 'partial'

    const component = {
      ...base,
      library: {
        remote: false,
        published: entity.published === true,
        hidden: entity.hidden === true
      },
      properties: supportedProperties,
      unsupportedProperties,
      variants,
      defaultVariantId,
      defaultKnown: entity.kind === 'component' ? true : defaultVariantId !== null,
      semanticSlots: slots,
      dependencies,
      tokenRefs,
      visualEvidence: { coverage, entries: visualEntries }
    }
    if (entity.description) component.description = entity.description
    if (entity.autoLayout) component.autoLayout = entity.autoLayout
    component.structuralHash = structuralHashOf(component)
    component.sourceHash = sourceHashOf(component)
    components.push(component)
  }

  components.sort((a, b) => compareText(a.designComponentId, b.designComponentId))
  unsupportedComponents.sort((a, b) => compareText(a.designComponentId, b.designComponentId))

  let variantTotal = 0
  let setCount = 0
  let standaloneCount = 0
  for (const component of components) {
    variantTotal += component.variants.length
    if (component.kind === 'component-set') setCount++
    else standaloneCount++
  }

  const inventory = {
    schemaVersion: 2,
    provider: 'figma',
    scopeId: deriveScopeId(capture),
    providerIdentity: {
      fileKeyFingerprint: capture.providerIdentity.fileKeyFingerprint,
      branchKey: capture.providerIdentity.branchKey,
      libraryOriginPolicy: capture.providerIdentity.libraryOriginPolicy
    },
    capture: {
      startedAt: capture.witness.startedAt,
      finishedAt: capture.witness.finishedAt,
      ...(capture.witness.providerRevisionBefore ? { providerRevisionBefore: capture.witness.providerRevisionBefore } : {}),
      ...(capture.witness.providerRevisionAfter ? { providerRevisionAfter: capture.witness.providerRevisionAfter } : {}),
      consistency: capture.witness.consistency,
      completeness: 'complete',
      absenceProofEligible: gate.absenceProofEligible === true,
      pagesRequested: capture.witness.requestedPageIds.length,
      pagesRead: capture.witness.readPageIds.length,
      truncated: false,
      permissionDegraded: false,
      limitsHit: [],
      captureHash
    },
    components,
    unsupportedComponents,
    counts: {
      pages: capture.pages.length,
      componentSets: setCount,
      standaloneComponents: standaloneCount,
      components: components.length,
      unsupportedComponents: unsupportedComponents.length,
      variants: variantTotal,
      capturedEntities: components.length + unsupportedComponents.length
    }
  }

  const inventoryError = inventorySemanticError(inventory)
  if (inventoryError) {
    throw typedError(COMPONENT_ERROR_CODES.COMPONENT_DESIGN_CAPTURE_INVALID, `normalized inventory failed its own contract: ${inventoryError}`)
  }
  return inventory
}
