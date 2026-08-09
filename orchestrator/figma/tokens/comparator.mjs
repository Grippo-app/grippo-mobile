import { canonicalHash, canonicalJson, compareText } from '../runtime/canonical-json.mjs'
import { contextKey } from './source-contract.mjs'

const HASH_NONE = 'none'

function findingId(kind, ...parts) {
  return 'tokf-' + canonicalHash({ kind, parts }).slice('sha256:'.length, 'sha256:'.length + 24)
}

function normalizeProjectValue(resolved) {
  if (!resolved) return null
  if (resolved.kind === 'color') {
    if (resolved.space === 'srgb' && resolved.hex) return { kind: 'color', value: { space: 'srgb', hex: resolved.hex.toUpperCase() } }
    if (resolved.colorSpace === 'srgb' && resolved.value) return { kind: 'color', value: { space: 'srgb', hex: resolved.value.toUpperCase() } }
  }
  if (resolved.kind === 'dimension') {
    return { kind: 'number', value: resolved.value, unit: resolved.unit }
  }
  if (resolved.kind === 'number') return { kind: 'number', value: resolved.value, unit: resolved.unit }
  if (resolved.kind === 'string' || resolved.kind === 'boolean') return { kind: resolved.kind, value: resolved.value }
  return null
}

function compareValue(design, project) {
  if (!project) return { status: 'not-compared', limitation: 'project-value-unsupported' }
  if (design.kind !== project.kind) return { status: 'not-compared', limitation: 'value-kind-incompatible' }
  if (design.kind === 'number' && project.unit && project.unit !== 'unitless') {
    return { status: 'not-compared', limitation: 'number-unit-policy-required' }
  }
  return {
    status: canonicalJson(design.value) === canonicalJson(project.value) ? 'matched' : 'value-drift'
  }
}

function previousByCoordinate(baseline) {
  return new Map((baseline ? baseline.entries : []).map((entry) => [
    entry.observedTokenKey + '\u0000' + entry.contextKey,
    entry
  ]))
}

function directionFor(previous, designHash, projectHash, bindingHash) {
  if (!previous) return 'unknown-no-baseline'
  const designChanged = previous.designProjectionHash !== designHash
  const projectChanged = previous.projectProjectionHash !== projectHash ||
    previous.bindingProjectionHash !== bindingHash
  if (designChanged && projectChanged) return 'both-changed'
  if (designChanged) return 'design-observation-changed'
  if (projectChanged) return 'project-changed'
  return 'unchanged'
}

export function compareTokens({
  observedCatalog,
  sourceIndex,
  projectInventories,
  analysisIndex,
  bindingSnapshot,
  mappingRegistry,
  baseline = null,
  context
}) {
  if (!context || !['current', 'stale', 'unknown'].includes(context.sourceFreshness)) {
    throw new Error('TOKEN_SOURCE_FRESHNESS_REQUIRED')
  }
  const inventoryByAdapter = new Map(projectInventories.map((inventory) => [inventory.adapterId, inventory]))
  const projectById = new Map()
  for (const inventory of projectInventories) {
    for (const token of inventory.tokens) projectById.set(token.projectTokenId, { inventory, token })
  }
  const bindings = new Map(bindingSnapshot.bindings.map((binding) => [
    binding.observedTokenKey + '\u0000' + binding.contextKey,
    binding
  ]))
  const conflicts = new Map(bindingSnapshot.conflicts.map((conflict) => [
    conflict.observedTokenKey + '\u0000' + conflict.contextKey,
    conflict
  ]))
  const prior = previousByCoordinate(baseline)
  const observedRows = []
  const baselineEntries = []

  for (const token of observedCatalog.tokens) {
    if (token.presenceStatus === 'not-observed') {
      const historical = [...prior.values()].filter((entry) => entry.observedTokenKey === token.observedTokenKey)
      for (const entry of historical.length ? historical : [{ contextKey: 'historical' }]) {
        observedRows.push({
          findingId: findingId('observed', token.observedTokenKey, entry.contextKey),
          observedTokenKey: token.observedTokenKey,
          providerName: token.providerName,
          contextKey: entry.contextKey,
          lifecycleStatus: 'not-observed',
          sourceStatus: 'consistent',
          bindingStatus: 'unbound',
          valueStatus: 'not-compared',
          direction: 'design-observation-changed',
          limitations: ['not-observed-is-not-file-wide-deletion']
        })
      }
      continue
    }
    for (const coordinate of token.coordinates) {
      const coordinateId = contextKey(coordinate.context)
      const key = token.observedTokenKey + '\u0000' + coordinateId
      const previous = prior.get(key)
      const lifecycleStatus = previous ? 'active' : 'newly-observed'
      const sourceStatus = coordinate.status === 'source-conflict'
        ? 'source-conflict'
        : coordinate.status === 'unsupported' ? 'unsupported' : 'consistent'
      const row = {
        findingId: findingId('observed', token.observedTokenKey, coordinateId),
        observedTokenKey: token.observedTokenKey,
        providerName: token.providerName,
        contextKey: coordinateId,
        lifecycleStatus,
        sourceStatus,
        bindingStatus: 'unbound',
        valueStatus: 'not-compared',
        direction: previous ? 'unchanged' : 'unknown-no-baseline',
        limitations: []
      }
      const conflict = conflicts.get(key)
      if (sourceStatus !== 'consistent') {
        row.bindingStatus = sourceStatus === 'source-conflict' ? 'authority-conflict' : 'unbound'
        row.limitations.push(sourceStatus === 'source-conflict' ? 'source-conflict-blocks-comparison' : 'unsupported-observation')
        observedRows.push(row)
        continue
      }
      if (conflict) {
        row.bindingStatus = conflict.code === 'TOKEN_BINDING_AMBIGUOUS'
          ? 'ambiguous-binding'
          : conflict.code === 'TOKEN_CONTEXT_MAP_REQUIRED'
            ? 'context-map-required'
            : 'authority-conflict'
        row.limitations.push(conflict.code)
        observedRows.push(row)
        continue
      }
      const binding = bindings.get(key)
      if (!binding) {
        observedRows.push(row)
        continue
      }
      row.bindingStatus = binding.targetState === 'expected-missing'
        ? 'expected-target-missing'
        : binding.authority === 'manual'
          ? 'manual-bound'
          : binding.authority === 'task' ? 'task-bound' : 'auto-bound'
      row.adapterId = binding.adapterId
      row.projectMode = binding.projectMode
      if (binding.projectTokenId) row.projectTokenId = binding.projectTokenId
      const design = coordinate.values[0]
      row.designValue = design.value
      const designHash = canonicalHash({ kind: design.kind, value: design.value })
      const bindingHash = canonicalHash(binding)
      let projectHash = canonicalHash({ absent: true })
      if (binding.targetState === 'expected-missing') {
        row.valueStatus = 'missing-in-project'
      } else {
        const project = projectById.get(binding.projectTokenId)
        const modeEntry = project && project.token.modes && project.token.modes[binding.projectMode]
        const projectValue = normalizeProjectValue(modeEntry && modeEntry.resolved)
        if (projectValue) {
          row.projectValue = projectValue.value
          projectHash = canonicalHash(projectValue)
        }
        const comparison = compareValue(design, projectValue)
        row.valueStatus = comparison.status
        if (comparison.limitation) row.limitations.push(comparison.limitation)
      }
      row.direction = directionFor(previous, designHash, projectHash, bindingHash)
      if (row.valueStatus === 'matched' || row.valueStatus === 'value-drift') {
        baselineEntries.push({
          observedTokenKey: token.observedTokenKey,
          contextKey: coordinateId,
          designProjectionHash: designHash,
          projectProjectionHash: projectHash,
          bindingProjectionHash: bindingHash,
          valueStatus: row.valueStatus
        })
      }
      observedRows.push(row)
    }
  }
  observedRows.sort((a, b) =>
    compareText(a.observedTokenKey, b.observedTokenKey) || compareText(a.contextKey, b.contextKey))

  const boundProjectIds = new Set(bindingSnapshot.bindings
    .filter((binding) => binding.targetState === 'present')
    .map((binding) => binding.projectTokenId))
  const projectOnly = []
  const projectComplete = analysisIndex.complete === true &&
    projectInventories.every((inventory) => !inventory.witness || inventory.witness.complete === true)
  if (projectComplete) {
    for (const inventory of projectInventories) {
      for (const token of inventory.tokens) {
        if (boundProjectIds.has(token.projectTokenId)) continue
        projectOnly.push({
          findingId: findingId('project-only', inventory.adapterId, token.projectTokenId),
          rowKind: 'project-only',
          adapterId: inventory.adapterId,
          projectTokenId: token.projectTokenId,
          displayName: token.displayName || token.semanticPath.join('.'),
          kind: token.kind === 'dimension' ? 'number' : token.kind,
          classification: 'unclassified'
        })
      }
    }
  }
  projectOnly.sort((a, b) => compareText(a.adapterId, b.adapterId) || compareText(a.projectTokenId, b.projectTokenId))
  const countRows = (predicate) => observedRows.filter(predicate).length
  const coverage = {
    label: 'Observed token coverage',
    denominator: countRows((row) => row.sourceStatus === 'consistent' && row.bindingStatus !== 'context-map-required' && row.lifecycleStatus !== 'not-observed'),
    matched: countRows((row) => row.valueStatus === 'matched'),
    valueDrift: countRows((row) => row.valueStatus === 'value-drift'),
    missingInProject: countRows((row) => row.valueStatus === 'missing-in-project'),
    unbound: countRows((row) => row.bindingStatus === 'unbound' && row.lifecycleStatus !== 'not-observed'),
    excludedUnsupported: countRows((row) => row.sourceStatus === 'unsupported'),
    excludedConflicting: countRows((row) => row.sourceStatus === 'source-conflict'),
    excludedContext: countRows((row) => row.bindingStatus === 'context-map-required'),
    notObserved: countRows((row) => row.lifecycleStatus === 'not-observed'),
    projectOnly: projectOnly.length
  }
  const blockers = []
  if (!projectComplete) blockers.push({
    code: 'PROJECT_TOKEN_INVENTORY_INCOMPLETE',
    detail: 'Project-only classification and baseline publication require every configured adapter inventory.'
  })
  if (context.sourceFreshness === 'unknown') blockers.push({
    code: 'TOKEN_SOURCE_HEALTH_UNAVAILABLE',
    detail: 'Source Health is unavailable or cannot prove the accepted source batches; comparison is read-only.'
  })
  if (context.sourceFreshness === 'stale') blockers.push({
    code: 'TOKEN_SOURCE_REFRESH_FAILED',
    detail: 'At least one active source has a newer unsuccessful refresh attempt; comparison is read-only.'
  })
  const report = {
    schemaVersion: 2,
    comparatorVersion: 'observed-token-comparator-v2',
    inputs: {
      observedCatalogHash: observedCatalog.semanticHash,
      sourceIndexHash: sourceIndex.semanticHash,
      sourceFreshness: context.sourceFreshness,
      analysisIndexHash: context.analysisIndexHash,
      bindingSnapshotHash: bindingSnapshot.semanticHash,
      mappingRevision: mappingRegistry.revision,
      mappingHash: bindingSnapshot.mappingHash,
      adapterConfigHash: context.adapterConfigHash,
      baselineHash: context.baselineHash || HASH_NONE
    },
    observedRows,
    projectOnly,
    coverage,
    operationalState: context.sourceFreshness === 'current'
      ? (blockers.length ? 'blocked' : 'current')
      : 'stale',
    blockers,
    complete: blockers.length === 0,
    semanticHash: 'sha256:' + '0'.repeat(64)
  }
  const { semanticHash, ...semanticPayload } = report
  report.semanticHash = canonicalHash(semanticPayload)
  return {
    report,
    baselineCandidate: {
      scope: observedCatalog.scope,
      entries: baselineEntries.sort((a, b) =>
        compareText(a.observedTokenKey, b.observedTokenKey) || compareText(a.contextKey, b.contextKey)),
      eligible: blockers.length === 0 &&
        !observedRows.some((row) => row.sourceStatus === 'source-conflict')
    }
  }
}
