// Universal component comparator. Pure: consumes only validated immutable
// artifacts (design component inventory, per-adapter project inventories +
// analysis index, mapping registry snapshot, optional token snapshot, optional
// baseline) plus explicit context; it never touches the filesystem,
// environment, network, Figma, or a clock. Status, change direction, and
// findings are independent axes; sparse provider variants are compared as-is
// (no Cartesian expansion); token-caused impact is concluded only through the
// exact causality proof of §17.12; a limit breach is a typed failure, never a
// partial report.
import { createHash } from 'node:crypto'
import { canonicalHash, compareText } from '../runtime/canonical-json.mjs'
import { typedError } from '../runtime/typed-error.mjs'
import { COMPONENT_ERROR_CODES } from './error-codes.mjs'
import { COMPONENT_LIMITS } from './limits.mjs'
import { buildSuggestionIndex, suggestCandidates } from './mapping-suggestions.mjs'
import { mappingRegistryHash } from './mapping-contract.mjs'
import { inventorySemanticHash, structuralHashOf } from './design-inventory-contract.mjs'
import { projectInventorySemanticHash } from './project-inventory-contract.mjs'

const COMPONENT_COMPARATOR_VERSION = 'component-comparator-v2'

// The declared comparison policy of this comparator version. Mapping-level
// bindings refine it per row; this hash pins the global semantics so a
// comparator behavior change is a visible input change.
const GLOBAL_POLICY = Object.freeze({
  version: 2,
  variantModel: 'sparse-provider-tuples-only',
  propertyMapping: 'explicit-id-bindings-only',
  requiredSlotKinds: ['text-property', 'instance-swap'],
  visualComparison: 'design-evidence-hash-drift-only',
  tokenCausality: 'structural-hash-and-token-row-proof'
})

const findingIdFor = (parts) =>
  'cmpf-' + createHash('sha256').update(parts.join('\0'), 'utf8').digest('hex').slice(0, 24)

function makeFinding(row, family, severity, detail, extra) {
  return {
    findingId: findingIdFor([row.designComponentId, family, (extra && (extra.designPropertyId || extra.designSlotId || extra.observedTokenKey || extra.adapterId)) || '', detail.slice(0, 120)]),
    family,
    severity,
    detail: detail.slice(0, 500),
    ...(extra || {})
  }
}

// Canonical project-side projection of one implementation: the API surface the
// baseline direction logic hashes.
function projectProjectionOf(components) {
  return components.map((component) => ({
    projectComponentId: component.projectComponentId,
    parameters: component.api.parameters,
    variantProperties: component.variantProperties,
    combinationsKnown: component.combinationsKnown,
    slots: component.slots,
    dependencies: component.dependencies,
    deprecated: component.deprecated === true
  }))
}

function mappingProjectionOf(mapping) {
  return canonicalHash({
    implementations: mapping.implementations,
    propertyMappings: mapping.propertyMappings,
    slotMappings: mapping.slotMappings,
    visualPolicy: mapping.visualPolicy || null
  })
}

function visualHashOf(designComponent) {
  return canonicalHash(designComponent.visualEvidence.entries)
}

// inputs: {
//   designInventory, projectInventories: [inventory...], analysisIndex,
//   mappingRegistry, baseline: baseline|null,
//   tokenSnapshot: { report: tokenComparison|null, registry: tokenMappings|null },
//   context: { designGenerationId, adapterConfigHash, adapterConfigFileHash }
// }
export function compareComponents(inputs) {
  const design = inputs.designInventory
  const registry = inputs.mappingRegistry
  const baseline = inputs.baseline || null
  const inventories = inputs.projectInventories
  const tokenReport = (inputs.tokenSnapshot && inputs.tokenSnapshot.report) || null
  const tokenBindings = (inputs.tokenSnapshot && inputs.tokenSnapshot.bindingSnapshot) || null
  const tokenSnapshotCompatible = tokenReport && tokenBindings &&
    tokenReport.schemaVersion === 2 &&
    tokenReport.complete === true &&
    tokenReport.operationalState === 'current' &&
    tokenReport.inputs.sourceFreshness === 'current' &&
    Array.isArray(tokenReport.blockers) && tokenReport.blockers.length === 0 &&
    tokenBindings.schemaVersion === 1 &&
    tokenBindings.observedCatalogHash === tokenReport.inputs.observedCatalogHash &&
    tokenBindings.semanticHash === tokenReport.inputs.bindingSnapshotHash

  for (const inventory of inventories) {
    if (!inventory.witness.complete) {
      throw typedError(COMPONENT_ERROR_CODES.COMPONENT_PROJECT_INVENTORY_INCOMPLETE,
        `adapter ${inventory.adapterId} inventory is incomplete; comparison is blocked`)
    }
  }
  if (design.components.length + design.unsupportedComponents.length > COMPONENT_LIMITS.comparisonRowsMax) {
    throw typedError(COMPONENT_ERROR_CODES.COMPONENT_COMPARISON_LIMIT_EXCEEDED,
      `design inventory carries more entities than the ${COMPONENT_LIMITS.comparisonRowsMax} row limit`)
  }

  const scopeMatches = registry.designScopeId === design.scopeId
  const inventoriesByAdapter = new Map(inventories.map((inventory) => [inventory.adapterId, inventory]))
  const projectById = new Map()
  for (const inventory of inventories) {
    for (const component of inventory.components) projectById.set(component.projectComponentId, component)
  }
  const designById = new Map(design.components.map((component) => [component.designComponentId, component]))
  const designUnsupportedById = new Map(design.unsupportedComponents.map((item) => [item.designComponentId, item]))

  const activeMappings = registry.mappings.filter((mapping) => mapping.state === 'active' && scopeMatches)
  const activeByDesignComponent = new Map(activeMappings.map((mapping) => [mapping.designComponentId, mapping]))
  const exclusiveClaims = new Map()
  const sharedClaims = new Map()
  for (const mapping of activeMappings) {
    for (const implementation of mapping.implementations) {
      for (const target of implementation.projectComponentIds) {
        if (implementation.relation === 'shared-implementation') {
          let bucket = sharedClaims.get(target)
          if (!bucket) { bucket = []; sharedClaims.set(target, bucket) }
          bucket.push(mapping.mappingId)
        } else {
          exclusiveClaims.set(target, mapping.mappingId)
        }
      }
    }
  }
  const designDispositions = new Map()
  const projectDispositions = new Map()
  for (const disposition of registry.dispositions) {
    if (disposition.target.side === 'design') designDispositions.set(disposition.target.designComponentId, disposition)
    else projectDispositions.set(disposition.target.projectComponentId, disposition)
  }
  const retiredByDisplayName = new Map()
  for (const mapping of registry.mappings) {
    if (mapping.state === 'retired' && mapping.retirement && mapping.retirement.lastSeenDisplayName) {
      retiredByDisplayName.set(mapping.retirement.lastSeenDisplayName, mapping)
    }
  }
  const baselineByMapping = new Map()
  if (baseline && baseline.designScopeId === design.scopeId) {
    for (const entry of baseline.entries) baselineByMapping.set(entry.mappingId, entry)
  }

  // Token causality lookups (§17.12): token rows that changed on the design
  // side, and the confirmed token mapping targets per design token.
  const changedTokenRows = new Map()
  if (tokenSnapshotCompatible) {
    for (const row of tokenReport.observedRows || []) {
      if (row.valueStatus === 'value-drift' &&
          (row.direction === 'design-observation-changed' || row.direction === 'both-changed')) {
        changedTokenRows.set(row.observedTokenKey + '\u0000' + row.contextKey, row)
      }
    }
  }
  const tokenMappingTargets = new Map()
  if (tokenSnapshotCompatible) {
    for (const binding of tokenBindings.bindings || []) {
      if (binding.targetState === 'present' && binding.projectTokenId) {
        tokenMappingTargets.set(binding.observedTokenKey + '\u0000' + binding.contextKey,
          new Set([binding.projectTokenId]))
      }
    }
  }

  const rows = []
  const blockers = []
  const baselineCandidates = []
  const suggestionsOut = []
  const reverseSuggestionCounts = new Map()
  const suggestionIndex = buildSuggestionIndex(inventories)

  // ── design-side traversal ────────────────────────────────────────────────
  for (const designComponent of design.components) {
    const disposition = designDispositions.get(designComponent.designComponentId)
    const row = {
      findingId: findingIdFor(['design', designComponent.designComponentId, 'row']),
      designComponentId: designComponent.designComponentId,
      displayName: designComponent.name,
      kind: designComponent.kind,
      status: 'unmapped',
      change: { changedSide: 'unknown', designChanged: false, projectChanged: false, mappingChanged: false, confidence: 'none' },
      findings: [],
      limitations: []
    }
    if (designComponent.tokenRefs.length && tokenReport && tokenBindings && !tokenSnapshotCompatible) {
      row.limitations.push('token-observation-snapshot-mismatch')
    }
    if (designComponent.providerIdentity.idQuality !== 'stable') {
      row.findings.push(makeFinding(row, 'identity-quality-degraded', 'info',
        'provider id is name-derived; renames read as remove+add'))
    }
    for (const property of designComponent.unsupportedProperties) {
      row.findings.push(makeFinding(row, 'unsupported-property', 'review',
        `property ${property.name}: ${property.reason}`, { designPropertyId: property.propertyId }))
    }

    if (disposition) {
      row.status = disposition.kind === 'ignored' || disposition.kind === 'unsupported-by-policy' ? 'ignored' : 'design-only'
      row.statusDetail = disposition.kind
      row.dispositionId = disposition.dispositionId
      rows.push(row)
      continue
    }

    const mapping = activeByDesignComponent.get(designComponent.designComponentId)
    if (!mapping) {
      const suggestion = suggestCandidates(designComponent, suggestionIndex, { exclusiveClaims, sharedClaims })
      for (const limitation of suggestion.limitations) {
        if (!row.limitations.includes(limitation)) row.limitations.push(limitation)
      }
      if (suggestion.candidates.length || suggestion.limitations.length) {
        suggestionsOut.push({
          designComponentId: designComponent.designComponentId,
          ambiguous: suggestion.ambiguous,
          candidates: suggestion.candidates,
          ...(suggestion.limitations.length ? { limitations: suggestion.limitations } : {})
        })
      }
      for (const candidate of suggestion.candidates) {
        reverseSuggestionCounts.set(candidate.projectComponentId,
          (reverseSuggestionCounts.get(candidate.projectComponentId) || 0) + 1)
      }
      row.suggestionsCount = suggestion.candidates.length
      row.suggestionsAmbiguous = suggestion.ambiguous
      row.status = suggestion.ambiguous ? 'ambiguous' : 'unmapped'
      if (suggestion.ambiguous) row.statusDetail = 'two or more strong candidates compete; confirmation required'
      const recreated = retiredByDisplayName.get(designComponent.name)
      if (recreated && recreated.designComponentId !== designComponent.designComponentId) {
        row.findings.push(makeFinding(row, 'possible-recreate', 'review',
          `a retired mapping (${recreated.mappingId}) last saw this display name under ${recreated.designComponentId}; identity is not inherited`))
      }
      rows.push(row)
      continue
    }

    row.mappingId = mapping.mappingId
    if (mapping.expectedKind !== designComponent.kind) {
      row.mappingState = 'incompatible'
      row.status = 'unsupported'
      row.statusDetail = `design entity kind changed to ${designComponent.kind}; mapping expects ${mapping.expectedKind}`
      row.findings.push(makeFinding(row, 'design-kind-changed', 'blocking',
        `mapping ${mapping.mappingId} expects ${mapping.expectedKind}, the design entity is now a ${designComponent.kind}; review the mapping`))
      rows.push(row)
      continue
    }

    // ── per-implementation resolution ─────────────────────────────────────
    const platforms = []
    const resolvedComponents = []
    let anyOutOfScope = false
    let anyMissing = false
    let requiredMissing = false
    for (const implementation of mapping.implementations) {
      const coverage = {
        adapterId: implementation.adapterId,
        platform: implementation.platform,
        relation: implementation.relation,
        required: implementation.required,
        state: 'unknown',
        projectRefs: []
      }
      if (implementation.relation === 'external') {
        coverage.state = 'external'
        platforms.push(coverage)
        continue
      }
      const inventory = inventoriesByAdapter.get(implementation.adapterId)
      if (!inventory) {
        coverage.state = 'out-of-scope'
        anyOutOfScope = true
        row.limitations.push(`adapter-not-analyzed:${implementation.adapterId}`)
        platforms.push(coverage)
        continue
      }
      if (implementation.projectScopeFingerprint !== inventory.scopeFingerprint) {
        coverage.state = 'out-of-scope'
        anyOutOfScope = true
        platforms.push(coverage)
        continue
      }
      let allPresent = true
      for (const projectComponentId of implementation.projectComponentIds) {
        const component = projectById.get(projectComponentId)
        coverage.projectRefs.push({
          projectComponentId,
          present: !!component,
          ...(component ? { sourcePath: component.source.path, sourceSymbol: component.source.symbol } : {})
        })
        if (component) resolvedComponents.push({ implementation, component, inventory })
        else allPresent = false
      }
      if (!allPresent) {
        coverage.state = 'missing'
        anyMissing = true
        if (implementation.required) requiredMissing = true
        row.findings.push(makeFinding(row, 'platform-implementation-missing',
          implementation.required ? 'breaking' : 'review',
          `mapped implementation for ${implementation.platform} is absent from a complete scan`,
          { adapterId: implementation.adapterId, platform: implementation.platform }))
      } else {
        coverage.state = 'matched'
      }
      platforms.push(coverage)
    }
    row.platforms = platforms

    if (anyOutOfScope && !resolvedComponents.length) {
      row.mappingState = 'target-out-of-scope'
      row.status = 'unmapped'
      row.statusDetail = 'mapping was confirmed against a different adapter scope; review it against the current scan'
      rows.push(row)
      continue
    }
    if (requiredMissing && !resolvedComponents.length) {
      row.mappingState = 'orphaned-project'
      row.status = 'missing-in-project'
      row.statusDetail = 'every mapped implementation is absent from a complete scan'
      rows.push(row)
      continue
    }
    row.mappingState = 'active'

    // ── property comparison (explicit bindings only) ──────────────────────
    const designProperties = new Map(designComponent.properties.map((property) => [property.propertyId, property]))
    const mappableDesignProperties = designComponent.properties.filter(
      (property) => property.type === 'variant' || property.type === 'boolean')
    const propertyMappingsByAdapter = new Map()
    for (const propertyMapping of mapping.propertyMappings) {
      let bucket = propertyMappingsByAdapter.get(propertyMapping.adapterId)
      if (!bucket) { bucket = new Map(); propertyMappingsByAdapter.set(propertyMapping.adapterId, bucket) }
      bucket.set(propertyMapping.designPropertyId, propertyMapping)
    }

    let propertiesMapped = 0
    let propertiesDrifted = 0
    const expressibleByAdapter = new Map()
    let variantsProvable = true
    for (const { implementation, component } of resolvedComponents) {
      if (implementation.relation === 'composite') {
        // Composite property routing is owner semantics; static per-property
        // verification is out of the provable set.
        row.limitations.push(`composite-static-verification-limited:${implementation.adapterId}`)
      }
      if (component.combinationsKnown !== 'all') variantsProvable = false
      const bindings = propertyMappingsByAdapter.get(implementation.adapterId) || new Map()
      const projectProperties = new Map(component.variantProperties.map((property) => [property.projectPropertyId, property]))
      const valueMapsByProperty = new Map()

      for (const designProperty of mappableDesignProperties) {
        const binding = bindings.get(designProperty.propertyId)
        if (!binding) {
          row.findings.push(makeFinding(row, 'property-unmapped', 'review',
            `design property ${designProperty.name} has no confirmed binding for ${implementation.platform}`,
            { designPropertyId: designProperty.propertyId, adapterId: implementation.adapterId, platform: implementation.platform }))
          continue
        }
        const projectProperty = projectProperties.get(binding.projectPropertyId)
        if (!projectProperty) {
          propertiesDrifted++
          row.findings.push(makeFinding(row, 'project-api-removed', 'breaking',
            `bound project property ${binding.projectPropertyId} no longer exists on ${component.name}`,
            { designPropertyId: designProperty.propertyId, adapterId: implementation.adapterId, platform: implementation.platform }))
          continue
        }
        propertiesMapped++
        const ignored = new Set((binding.ignoredValues || []).map((entry) => entry.value))
        const valueMap = binding.valueMap || {}
        valueMapsByProperty.set(designProperty.propertyId, { valueMap, ignored, projectProperty })
        const projectValues = new Set(projectProperty.values.map((entry) => entry.value))
        const designValues = designProperty.type === 'boolean' ? ['true', 'false'] : designProperty.options
        let drifted = false
        for (const designValue of designValues) {
          if (ignored.has(designValue)) continue
          const mappedValue = valueMap[designValue]
          if (mappedValue === undefined) {
            row.findings.push(makeFinding(row, 'value-unmapped', 'review',
              `design value ${designProperty.name}=${designValue} has no confirmed project value`,
              { designPropertyId: designProperty.propertyId, adapterId: implementation.adapterId }))
            drifted = true
            continue
          }
          if (!projectValues.has(mappedValue)) {
            row.findings.push(makeFinding(row, 'value-removed', 'breaking',
              `mapped project value ${mappedValue} for ${designProperty.name}=${designValue} no longer exists`,
              { designPropertyId: designProperty.propertyId, adapterId: implementation.adapterId }))
            drifted = true
          }
        }
        // Mapped design values that vanished from the design property itself.
        for (const mappedDesignValue of Object.keys(valueMap)) {
          const stillExists = designProperty.type === 'boolean'
            ? mappedDesignValue === 'true' || mappedDesignValue === 'false'
            : designProperty.options.includes(mappedDesignValue)
          if (!stillExists) {
            row.findings.push(makeFinding(row, 'value-removed', 'behavioral',
              `design value ${mappedDesignValue} bound for ${designProperty.name} no longer exists on the design side`,
              { designPropertyId: designProperty.propertyId, adapterId: implementation.adapterId }))
            drifted = true
          }
        }
        // Defaults.
        const designDefaultKnown = designProperty.defaultKnown === true
        if (!designDefaultKnown || !projectProperty.defaultKnown) {
          row.findings.push(makeFinding(row, 'default-unknown', 'info',
            `default agreement for ${designProperty.name} cannot be verified (${designDefaultKnown ? 'project' : 'design'} default unknown)`,
            { designPropertyId: designProperty.propertyId, adapterId: implementation.adapterId }))
        } else {
          const designDefault = String(designProperty.defaultValue)
          const mappedDefault = ignored.has(designDefault) ? undefined : valueMap[designDefault]
          if (mappedDefault !== undefined && mappedDefault !== projectProperty.defaultValue) {
            row.findings.push(makeFinding(row, 'default-changed', 'behavioral',
              `design default ${designProperty.name}=${designDefault} maps to ${mappedDefault}, project defaults to ${projectProperty.defaultValue}`,
              { designPropertyId: designProperty.propertyId, adapterId: implementation.adapterId }))
            drifted = true
          }
        }
        if (drifted) propertiesDrifted++
      }

      // Bindings citing design properties that vanished from the design side.
      for (const [designPropertyId] of bindings) {
        if (!designProperties.has(designPropertyId)) {
          row.findings.push(makeFinding(row, 'property-removed', 'breaking',
            `mapped design property ${designPropertyId} no longer exists on the design side`,
            { designPropertyId, adapterId: implementation.adapterId }))
        } else if (!mappableDesignProperties.some((property) => property.propertyId === designPropertyId)) {
          row.findings.push(makeFinding(row, 'property-type-changed', 'breaking',
            `design property ${designPropertyId} is no longer a variant/boolean property; its binding is stale`,
            { designPropertyId, adapterId: implementation.adapterId }))
        }
      }
      // New project variant properties no binding cites (single-component
      // relations only — composite routing is owner semantics).
      if (implementation.relation !== 'composite') {
        const cited = new Set([...bindings.values()].map((binding) => binding.projectPropertyId))
        for (const projectProperty of component.variantProperties) {
          if (!cited.has(projectProperty.projectPropertyId)) {
            row.findings.push(makeFinding(row, 'project-api-added', 'info',
              `project property ${projectProperty.name} on ${component.name} is not bound to any design property`,
              { adapterId: implementation.adapterId }))
          }
        }
      }

      // ── sparse variant expressibility for this implementation ───────────
      let expressible = 0
      for (const variant of designComponent.variants) {
        let ok = true
        for (const [propertyId, designValue] of Object.entries(variant.assignments)) {
          const bound = valueMapsByProperty.get(propertyId)
          if (!bound) { ok = false; break }
          if (bound.ignored.has(designValue)) continue
          const mapped = bound.valueMap[designValue]
          if (mapped === undefined) { ok = false; break }
          if (!bound.projectProperty.values.some((entry) => entry.value === mapped)) { ok = false; break }
        }
        if (ok) expressible++
      }
      expressibleByAdapter.set(implementation.adapterId, expressible)
    }
    if (!variantsProvable && designComponent.variants.length) {
      row.findings.push(makeFinding(row, 'variant-combination-unprovable', 'review',
        'a project side cannot statically prove its allowed combinations; full variant coverage is not claimed'))
    }

    // ── slot comparison ───────────────────────────────────────────────────
    const requiredSlotKinds = new Set(GLOBAL_POLICY.requiredSlotKinds)
    const mappableSlots = designComponent.semanticSlots.filter((slot) => requiredSlotKinds.has(slot.kind))
    const slotMappingsByAdapter = new Map()
    for (const slotMapping of mapping.slotMappings) {
      let bucket = slotMappingsByAdapter.get(slotMapping.adapterId)
      if (!bucket) { bucket = new Map(); slotMappingsByAdapter.set(slotMapping.adapterId, bucket) }
      bucket.set(slotMapping.designSlotId, slotMapping)
    }
    let slotsMapped = 0
    let slotsIgnored = 0
    for (const { implementation, component } of resolvedComponents) {
      const bucket = slotMappingsByAdapter.get(implementation.adapterId) || new Map()
      const projectSlots = new Map(component.slots.map((slot) => [slot.slotId, slot]))
      for (const designSlot of mappableSlots) {
        const slotMapping = bucket.get(designSlot.slotId)
        if (!slotMapping) {
          row.findings.push(makeFinding(row, 'slot-unmapped', 'review',
            `design slot ${designSlot.name} (${designSlot.kind}) has no confirmed binding for ${implementation.platform}`,
            { designSlotId: designSlot.slotId, adapterId: implementation.adapterId, platform: implementation.platform }))
          continue
        }
        if (slotMapping.ignoredReason !== undefined) { slotsIgnored++; continue }
        const projectSlot = projectSlots.get(slotMapping.projectSlotId)
        if (!projectSlot) {
          row.findings.push(makeFinding(row, 'slot-removed', 'breaking',
            `bound project slot ${slotMapping.projectSlotId} no longer exists on ${component.name}`,
            { designSlotId: designSlot.slotId, adapterId: implementation.adapterId }))
          continue
        }
        slotsMapped++
      }
      for (const [designSlotId] of bucket) {
        if (!designComponent.semanticSlots.some((slot) => slot.slotId === designSlotId)) {
          row.findings.push(makeFinding(row, 'slot-removed', 'behavioral',
            `mapped design slot ${designSlotId} no longer exists on the design side`,
            { designSlotId, adapterId: implementation.adapterId }))
        }
      }
    }

    // ── nested dependency coverage ────────────────────────────────────────
    const resolvedDependencies = designComponent.dependencies.filter((dependency) => dependency.resolved)
    let dependenciesCovered = 0
    for (const dependency of resolvedDependencies) {
      const targetMapping = activeByDesignComponent.get(dependency.targetDesignComponentId)
      if (!targetMapping) {
        row.findings.push(makeFinding(row, 'nested-dependency-changed', 'review',
          `nested design component ${dependency.targetDesignComponentId} is not mapped; the dependency cannot be verified`))
        continue
      }
      let covered = false
      for (const { implementation, component } of resolvedComponents) {
        const targetImplementation = targetMapping.implementations.find((impl) => impl.adapterId === implementation.adapterId)
        if (!targetImplementation) continue
        const targets = new Set(targetImplementation.projectComponentIds)
        if (component.dependencies.some((edge) => edge.kind === 'component' && targets.has(edge.targetProjectComponentId))) {
          covered = true
          break
        }
      }
      if (covered) dependenciesCovered++
      else {
        row.findings.push(makeFinding(row, 'nested-dependency-changed', 'review',
          `mapped implementation does not reference the implementation of nested component ${dependency.targetDesignComponentId}`))
      }
    }

    // ── token binding coverage ────────────────────────────────────────────
    // Statically resolvable only when the implementation carries exact token
    // dependency edges; an implementation without any resolvable token edge
    // is unverifiable (info), never invented drift.
    const anyTokenEdges = resolvedComponents.some(({ component }) =>
      component.dependencies.some((edge) => edge.kind === 'token' && edge.projectTokenId))
    for (const tokenRef of designComponent.tokenRefs) {
      if (!tokenSnapshotCompatible) break
      const tokenKey = tokenRef.observedTokenKey + '\u0000' + tokenRef.contextKey
      const targets = tokenMappingTargets.get(tokenKey)
      if (!targets) {
        row.findings.push(makeFinding(row, 'token-binding-changed', 'info',
          `observed token ${tokenRef.observedTokenKey} at ${tokenRef.contextKey} (${tokenRef.field}) has no effective binding; dependency drift cannot be verified`,
          { observedTokenKey: tokenRef.observedTokenKey }))
        continue
      }
      if (!resolvedComponents.length) continue
      if (!anyTokenEdges) {
        row.findings.push(makeFinding(row, 'token-binding-changed', 'info',
          `project token references could not be statically resolved; the binding of ${tokenRef.observedTokenKey} (${tokenRef.field}) is not verified`,
          { observedTokenKey: tokenRef.observedTokenKey }))
        continue
      }
      const referenced = resolvedComponents.some(({ component }) =>
        component.dependencies.some((edge) => edge.kind === 'token' && targets.has(edge.projectTokenId)))
      if (!referenced) {
        row.findings.push(makeFinding(row, 'token-binding-changed', 'review',
          `project implementation does not reference the effective project token for ${tokenRef.observedTokenKey} (${tokenRef.field})`,
          { observedTokenKey: tokenRef.observedTokenKey }))
      }
    }

    // ── render/test evidence ─────────────────────────────────────────────
    const anyPreviews = resolvedComponents.some(({ component }) => component.evidence.previews.length > 0)
    const anyScreenshotTests = resolvedComponents.some(({ component }) => component.evidence.screenshotTests.length > 0)
    if (resolvedComponents.length && !anyScreenshotTests) {
      row.findings.push(makeFinding(row, 'test-coverage-missing', 'info',
        'no screenshot/golden test evidence exists for the mapped implementation'))
    }

    // ── direction vs baseline + lifecycle findings ───────────────────────
    const designProjectionHash = designComponent.structuralHash
    const projectProjectionHash = canonicalHash(projectProjectionOf(
      resolvedComponents.map(({ component }) => component).sort((a, b) => compareText(a.projectComponentId, b.projectComponentId))))
    const mappingProjectionHash = mappingProjectionOf(mapping)
    const designVisualHash = visualHashOf(designComponent)
    const baseEntry = baselineByMapping.get(mapping.mappingId)
    let designVisualChanged = false
    if (baseEntry) {
      const designChanged = baseEntry.designProjectionHash !== designProjectionHash
      const projectChanged = baseEntry.projectProjectionHash !== projectProjectionHash
      const mappingChanged = baseEntry.mappingProjectionHash !== mappingProjectionHash
      row.change = {
        changedSide: designChanged && projectChanged ? 'both'
          : designChanged ? 'design'
          : projectChanged ? 'project'
          : mappingChanged ? 'mapping'
          : 'none',
        designChanged,
        projectChanged,
        mappingChanged,
        confidence: 'exact'
      }
      if (mappingChanged) {
        row.findings.push(makeFinding(row, 'mapping-policy-changed', 'info',
          'implementations, property/slot bindings, or visual policy changed since the baseline'))
      }
      if (baseEntry.designDisplayName !== designComponent.name) {
        row.findings.push(makeFinding(row, 'design-renamed', 'info',
          `display name changed from ${JSON.stringify(baseEntry.designDisplayName)} (stable id kept)`))
      }
      if (baseEntry.designPageId && baseEntry.designPageId !== designComponent.page.pageId) {
        row.findings.push(makeFinding(row, 'design-moved', 'info',
          'the component moved to another page (stable id kept)'))
      }
      const baselineVariants = new Set(baseEntry.designVariantIds)
      const currentVariants = new Set(designComponent.variants.map((variant) => variant.variantId))
      for (const variantId of currentVariants) {
        if (!baselineVariants.has(variantId)) {
          row.findings.push(makeFinding(row, 'allowed-variant-added', 'additive',
            `allowed variant ${variantId} appeared since the baseline`))
        }
      }
      for (const variantId of baselineVariants) {
        if (!currentVariants.has(variantId)) {
          row.findings.push(makeFinding(row, 'allowed-variant-removed', 'behavioral',
            `allowed variant ${variantId} disappeared since the baseline`))
        }
      }
      if (baseEntry.designVisualHash !== designVisualHash) {
        designVisualChanged = true
        row.findings.push(makeFinding(row, 'visual-evidence-drift', 'review',
          'design visual evidence changed since the baseline'))
      }
    }

    // ── token-caused causality (§17.12) ──────────────────────────────────
    // Concluded only when: structural hash unchanged, the only non-info drift
    // is the visual evidence drift, every changed bound token is confirmed by
    // the token comparator, and the project still references the mapped
    // project tokens.
    if (designVisualChanged && baseEntry && baseEntry.designProjectionHash === designProjectionHash && tokenSnapshotCompatible) {
      // "No other findings": anything beyond the visual drift itself at
      // review severity or above defeats the causality proof (fail toward
      // NOT suppressing the task).
      const semanticDrift = row.findings.some((finding) =>
        finding.family !== 'visual-evidence-drift' && finding.severity !== 'info')
      const changedBoundTokens = designComponent.tokenRefs.filter((ref) =>
        changedTokenRows.has(ref.observedTokenKey + '\u0000' + ref.contextKey))
      if (!semanticDrift && changedBoundTokens.length) {
        const allCausallyExplained = changedBoundTokens.every((ref) => {
          const targets = tokenMappingTargets.get(ref.observedTokenKey + '\u0000' + ref.contextKey)
          if (!targets) return false
          return resolvedComponents.some(({ component }) =>
            component.dependencies.some((edge) => edge.kind === 'token' && targets.has(edge.projectTokenId)))
        })
        if (allCausallyExplained) {
          row.findings.push(makeFinding(row, 'underlying-token-change', 'info',
            `visual change is explained by ${changedBoundTokens.length} changed bound token(s); no component task is proposed`,
            { observedTokenKey: changedBoundTokens[0].observedTokenKey, suppressesTask: true }))
        }
      }
    }

    // ── dimensions + status ──────────────────────────────────────────────
    const expressible = designComponent.variants.length === 0
      ? 0
      : Math.min(...[...expressibleByAdapter.values(), designComponent.variants.length])
    row.dimensions = {
      properties: {
        designTotal: mappableDesignProperties.length,
        mapped: Math.min(propertiesMapped, 80),
        drifted: Math.min(propertiesDrifted, 80),
        unsupported: designComponent.unsupportedProperties.length
      },
      variants: {
        designTotal: designComponent.variants.length,
        expressible,
        provable: variantsProvable
      },
      slots: {
        designTotal: mappableSlots.length,
        mapped: Math.min(slotsMapped, 128),
        ignored: Math.min(slotsIgnored, 128)
      },
      dependencies: {
        designTotal: resolvedDependencies.length,
        resolved: dependenciesCovered
      },
      visual: mapping.implementations.every((implementation) => implementation.relation === 'external')
        ? 'not-applicable'
        : designComponent.visualEvidence.coverage === 'none' || !anyScreenshotTests && !anyPreviews
          ? 'insufficient-evidence'
          : designVisualChanged ? 'review-required' : 'not-run',
      testReadiness: { previews: anyPreviews, screenshotTests: anyScreenshotTests }
    }

    // Deterministic status priority: incomplete bindings first (the mapping
    // relationship is under-specified — nothing may claim matched or invent
    // drift), then material drift, then matched.
    const suppressed = row.findings.some((finding) => finding.family === 'underlying-token-change' && finding.suppressesTask === true)
    const bindingGaps = row.findings.some((finding) =>
      ['property-unmapped', 'value-unmapped', 'slot-unmapped'].includes(finding.family))
    const materialSeverities = new Set(['additive', 'behavioral', 'breaking', 'blocking'])
    const materialDrift = row.findings.some((finding) => materialSeverities.has(finding.severity)) ||
      row.findings.some((finding) => finding.severity === 'review' &&
        ['variant-combination-unprovable', 'nested-dependency-changed', 'possible-recreate', 'token-binding-changed'].includes(finding.family))
    if (bindingGaps) {
      row.status = 'ambiguous'
      row.statusDetail = 'property/slot bindings are incomplete; finish the mapping review before this row can be verified'
    } else if (anyMissing && resolvedComponents.length || materialDrift) {
      row.status = 'drifted'
      if (anyMissing && resolvedComponents.length) row.statusDetail = 'a mapped platform implementation is missing'
    } else {
      row.status = 'matched'
    }
    if (row.findings.length > COMPONENT_LIMITS.findingsPerRowMax) {
      row.findings = row.findings.slice(0, COMPONENT_LIMITS.findingsPerRowMax)
      row.limitations.push('findings-truncated-at-limit')
    }
    row.limitations = [...new Set(row.limitations)].sort().slice(0, 16)

    if (row.status === 'matched' || row.status === 'drifted') {
      baselineCandidates.push({
        mappingId: mapping.mappingId,
        designComponentId: designComponent.designComponentId,
        designProjectionHash,
        projectProjectionHash,
        mappingProjectionHash,
        designDisplayName: designComponent.name,
        designPageId: designComponent.page.pageId,
        designVariantIds: designComponent.variants.map((variant) => variant.variantId).sort(),
        designVisualHash,
        status: row.status
      })
    }
    rows.push(row)
  }

  // ── design-side unsupported inventory entities ──────────────────────────
  for (const item of design.unsupportedComponents) {
    const disposition = designDispositions.get(item.designComponentId)
    rows.push({
      findingId: findingIdFor(['design', item.designComponentId, 'row']),
      designComponentId: item.designComponentId,
      displayName: item.name,
      kind: 'unsupported',
      status: disposition ? (disposition.kind === 'ignored' || disposition.kind === 'unsupported-by-policy' ? 'ignored' : 'design-only') : 'unsupported',
      statusDetail: disposition ? disposition.kind : item.reason,
      ...(disposition ? { dispositionId: disposition.dispositionId } : {}),
      change: { changedSide: 'unknown', designChanged: false, projectChanged: false, mappingChanged: false, confidence: 'none' },
      findings: [],
      limitations: []
    })
  }

  // ── mappings whose design component vanished ────────────────────────────
  for (const mapping of activeMappings) {
    if (designById.has(mapping.designComponentId) || designUnsupportedById.has(mapping.designComponentId)) continue
    if (design.capture.absenceProofEligible === true) {
      rows.push({
        findingId: findingIdFor(['design', mapping.designComponentId, 'row']),
        designComponentId: mapping.designComponentId,
        displayName: mapping.designComponentId,
        kind: mapping.expectedKind,
        status: 'missing-in-design',
        statusDetail: `mapping ${mapping.mappingId} targets a design component absent from a complete, consistency-proven capture`,
        mappingId: mapping.mappingId,
        mappingState: 'orphaned-design',
        change: { changedSide: 'design', designChanged: true, projectChanged: false, mappingChanged: false, confidence: baselineByMapping.has(mapping.mappingId) ? 'exact' : 'none' },
        findings: [],
        limitations: []
      })
    } else {
      blockers.push({
        code: COMPONENT_ERROR_CODES.COMPONENT_DESIGN_ABSENCE_UNPROVEN,
        detail: `mapping ${mapping.mappingId} target ${mapping.designComponentId} is absent, but the capture cannot prove absence (consistency unproven); no deletion is concluded`
      })
    }
  }

  // ── project-only traversal ───────────────────────────────────────────────
  const projectOnly = []
  let projectOnlyCount = 0
  for (const inventory of inventories) {
    for (const component of inventory.components) {
      if (exclusiveClaims.has(component.projectComponentId) || sharedClaims.has(component.projectComponentId)) continue
      projectOnlyCount++
    }
  }
  if (projectOnlyCount > COMPONENT_LIMITS.projectOnlyRowsMax) {
    throw typedError(COMPONENT_ERROR_CODES.COMPONENT_COMPARISON_LIMIT_EXCEEDED,
      `project-only rows (${projectOnlyCount}) exceed the ${COMPONENT_LIMITS.projectOnlyRowsMax} limit`)
  }
  for (const inventory of inventories) {
    for (const component of inventory.components) {
      if (exclusiveClaims.has(component.projectComponentId) || sharedClaims.has(component.projectComponentId)) continue
      const disposition = projectDispositions.get(component.projectComponentId)
      const classification = disposition
        ? (disposition.kind === 'intentionally-project-only' ? 'intentionally-project-only'
          : disposition.kind === 'external' ? 'external'
          : disposition.kind === 'deprecated' ? 'deprecated'
          : disposition.kind === 'superseded' ? 'superseded'
          : disposition.kind === 'deferred' ? 'deferred'
          : 'ignored')
        : 'unclassified'
      const entry = {
        findingId: findingIdFor(['project', component.projectComponentId, 'row']),
        projectComponentId: component.projectComponentId,
        adapterId: inventory.adapterId,
        platform: inventory.platform,
        displayName: component.name,
        kind: component.kind,
        classification,
        sourcePath: component.source.path,
        sourceSymbol: component.source.symbol
      }
      if (disposition) entry.dispositionId = disposition.dispositionId
      const reverse = reverseSuggestionCounts.get(component.projectComponentId)
      if (!disposition && reverse) entry.suggestionsCount = Math.min(reverse, COMPONENT_LIMITS.suggestionsPerComponentMax)
      projectOnly.push(entry)
    }
  }

  rows.sort((a, b) => compareText(a.designComponentId, b.designComponentId))
  projectOnly.sort((a, b) => compareText(a.projectComponentId, b.projectComponentId))
  suggestionsOut.sort((a, b) => compareText(a.designComponentId, b.designComponentId))
  blockers.sort((a, b) => compareText(a.code, b.code) || compareText(a.detail, b.detail))
  if (rows.length > COMPONENT_LIMITS.comparisonRowsMax) {
    throw typedError(COMPONENT_ERROR_CODES.COMPONENT_COMPARISON_LIMIT_EXCEEDED,
      `comparison rows exceed the ${COMPONENT_LIMITS.comparisonRowsMax} limit`)
  }

  const count = (status) => rows.filter((row) => row.status === status).length
  const designComponents = design.components.length + design.unsupportedComponents.length
  const ignoredDesignComponents = rows.filter((row) =>
    (row.status === 'ignored' || row.status === 'design-only') &&
    (designById.has(row.designComponentId) || designUnsupportedById.has(row.designComponentId))).length
  const coverage = {
    designComponents,
    ignoredDesignComponents,
    denominator: designComponents - ignoredDesignComponents,
    matched: count('matched'),
    drifted: count('drifted'),
    unmapped: count('unmapped'),
    ambiguous: count('ambiguous'),
    missingInProject: count('missing-in-project'),
    missingInDesign: count('missing-in-design'),
    designOnly: count('design-only'),
    unsupported: count('unsupported'),
    projectOnly: projectOnly.length
  }

  const inputsBlock = {
    designGenerationId: inputs.context.designGenerationId,
    designInventoryHash: inventorySemanticHash(design),
    designScopeId: design.scopeId,
    analysisIndexHash: canonicalHash(inputs.analysisIndex),
    mappingHash: mappingRegistryHash(registry),
    mappingRevision: registry.revision,
    adapterConfigHash: inputs.context.adapterConfigHash,
    adapterConfigFileHash: inputs.context.adapterConfigFileHash,
    policyHash: canonicalHash(GLOBAL_POLICY),
    baselineHash: baseline ? canonicalHash(baseline) : 'none',
    tokenSnapshot: {
      comparisonSemanticHash: tokenReport ? tokenReport.semanticHash : 'none',
      bindingSnapshotHash: tokenBindings ? tokenBindings.semanticHash : 'none'
    },
    designAbsenceProofEligible: design.capture.absenceProofEligible === true,
    projectComplete: true
  }
  if (!scopeMatches) {
    blockers.push({
      code: COMPONENT_ERROR_CODES.COMPONENT_DESIGN_SCOPE_CHANGED,
      detail: `mapping registry is bound to scope ${registry.designScopeId}; the design inventory is scope ${design.scopeId} — mappings do not apply and every component reads unmapped until an explicit onboarding`
    })
    blockers.sort((a, b) => compareText(a.code, b.code) || compareText(a.detail, b.detail))
  }

  const semanticPayload = { inputs: inputsBlock, rows, projectOnly, coverage, blockers }
  const report = {
    schemaVersion: 2,
    comparatorVersion: COMPONENT_COMPARATOR_VERSION,
    inputs: inputsBlock,
    rows,
    projectOnly,
    coverage,
    ...(blockers.length ? { blockers } : {}),
    complete: true,
    semanticHash: canonicalHash(semanticPayload)
  }

  const suggestions = {
    schemaVersion: 2,
    comparisonSemanticHash: report.semanticHash,
    byDesignComponent: suggestionsOut
  }

  // Baseline candidate (REQ-CMP-005): eligible only when the run had no
  // structural blockers and both sides were complete.
  const baselineCandidate = blockers.length === 0 ? {
    schemaVersion: 2,
    designScopeId: design.scopeId,
    source: {
      comparisonSemanticHash: report.semanticHash,
      designInventoryHash: inputsBlock.designInventoryHash,
      analysisIndexHash: inputsBlock.analysisIndexHash,
      mappingHash: inputsBlock.mappingHash,
      mappingRevision: inputsBlock.mappingRevision,
      eligibleAt: null
    },
    entries: baselineCandidates.sort((a, b) => compareText(a.mappingId, b.mappingId))
  } : null

  return { report, suggestions, baselineCandidate }
}

export { structuralHashOf }
