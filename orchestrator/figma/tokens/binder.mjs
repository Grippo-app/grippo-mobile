import { canonicalHash, canonicalJson, compareText } from '../runtime/canonical-json.mjs'
import { contextKey } from './source-contract.mjs'
import { mappingRegistryHash, mappingRegistrySemanticError } from './mapping-contract.mjs'

function pathKey(path) {
  return path.join('\u0000')
}

function startsWithPath(path, prefix) {
  return prefix.length <= path.length && prefix.every((segment, index) => segment === path[index])
}

function matchesContext(context, when) {
  return Object.entries(when).every(([key, value]) => context[key] === value)
}

function transformSegment(segment, kind) {
  if (kind === 'preserve') return segment
  const words = segment.split(/[-_\s]+/).filter(Boolean)
  if (kind === 'kebab') return words.map((word) => word.toLowerCase()).join('-')
  if (kind === 'snake') return words.map((word) => word.toLowerCase()).join('_')
  if (kind === 'camel') {
    return words.map((word, index) => index === 0
      ? word.charAt(0).toLowerCase() + word.slice(1)
      : word.charAt(0).toUpperCase() + word.slice(1)).join('')
  }
  throw new Error(`TOKEN_BINDING_TRANSFORM_INVALID: ${kind}`)
}

function ruleTarget(rule, providerPath) {
  if (rule.kind === 'exact-path') {
    return pathKey(rule.providerPath) === pathKey(providerPath) ? rule.projectPath : null
  }
  if (rule.kind === 'explicit-table') {
    const hit = rule.entries.find((entry) => pathKey(entry.providerPath) === pathKey(providerPath))
    return hit ? hit.projectPath : null
  }
  if (!startsWithPath(providerPath, rule.providerPrefix)) return null
  if (rule.excludeExact.some((path) => pathKey(path) === pathKey(providerPath)) ||
      rule.excludePrefix.some((prefix) => startsWithPath(providerPath, prefix))) return null
  return rule.projectPrefix.concat(providerPath.slice(rule.providerPrefix.length)
    .map((segment) => transformSegment(segment, rule.caseTransform)))
}

function coordinateKind(coordinate) {
  if (coordinate.status !== 'consistent' || coordinate.values.length !== 1) return null
  const kind = coordinate.values[0].kind
  return kind === 'unsupported' ? null : kind
}

function compatibleProjectKind(observedKind, projectKind) {
  return observedKind === projectKind ||
    observedKind === 'number' && projectKind === 'dimension'
}

function bindingKey(observedTokenKey, coordinate) {
  return observedTokenKey + '\u0000' + contextKey(coordinate.context)
}

function sortSnapshot(snapshot) {
  const cmp = (left, right) =>
    compareText(left.observedTokenKey, right.observedTokenKey) ||
    compareText(left.contextKey, right.contextKey) ||
    compareText(left.adapterId || '', right.adapterId || '') ||
    compareText(left.projectTokenId || '', right.projectTokenId || '')
  snapshot.bindings.sort(cmp)
  snapshot.suggestions.sort(cmp)
  snapshot.conflicts.sort(cmp)
}

export function bindObservedTokens({
  catalog,
  projectInventories,
  adapterConfig,
  mappingRegistry,
  taskBindings = [],
  projectAnalysisHash
}) {
  const mappingError = mappingRegistrySemanticError(mappingRegistry)
  if (mappingError) throw new Error(`TOKEN_MAPPING_INVALID: ${mappingError}`)
  if (mappingRegistry.scope.fileKeyFingerprint !== catalog.scope.fileKeyFingerprint ||
      mappingRegistry.scope.branchKey !== catalog.scope.branchKey) {
    throw new Error('TOKEN_MAPPING_SCOPE_CHANGED')
  }
  const inventoryByAdapter = new Map(projectInventories.map((inventory) => [inventory.adapterId, inventory]))
  const adapterById = new Map(adapterConfig.enabledTokenAdapters.map((adapter) => [adapter.id, adapter]))
  const projectById = new Map()
  const projectByAddress = new Map()
  for (const inventory of projectInventories) {
    for (const token of inventory.tokens) {
      projectById.set(token.projectTokenId, { inventory, token })
      const key = inventory.adapterId + '\u0000' + pathKey(token.semanticPath)
      const rows = projectByAddress.get(key) || []
      rows.push({ inventory, token })
      projectByAddress.set(key, rows)
    }
  }
  const manualByKey = new Map()
  for (const mapping of mappingRegistry.mappings.filter((row) => row.state === 'active')) {
    const selectors = mapping.contextSelector || {}
    const rows = manualByKey.get(mapping.observedTokenKey) || []
    rows.push({ mapping, selectors })
    manualByKey.set(mapping.observedTokenKey, rows)
  }
  const taskByKey = new Map(taskBindings.map((binding) => [
    binding.observedTokenKey + '\u0000' + binding.contextKey,
    binding
  ]))

  const snapshot = {
    schemaVersion: 1,
    observedCatalogHash: catalog.semanticHash,
    projectAnalysisHash,
    adapterConfigHash: adapterConfig.tokenConfigHash,
    mappingRevision: mappingRegistry.revision,
    mappingHash: mappingRegistryHash(mappingRegistry),
    bindings: [],
    suggestions: [],
    conflicts: [],
    semanticHash: 'sha256:' + '0'.repeat(64)
  }
  for (const token of catalog.tokens) {
    if (token.presenceStatus === 'not-observed') continue
    for (const coordinate of token.coordinates) {
      const coordinateKey = contextKey(coordinate.context)
      const key = bindingKey(token.observedTokenKey, coordinate)
      const observedKind = coordinateKind(coordinate)
      if (!observedKind) {
        snapshot.conflicts.push({
          observedTokenKey: token.observedTokenKey,
          contextKey: coordinateKey,
          code: coordinate.status === 'source-conflict' ? 'TOKEN_SOURCE_CONFLICT' : 'TOKEN_BINDING_TARGET_INCOMPATIBLE',
          candidateCount: coordinate.values.length
        })
        continue
      }
      const authorities = []
      const task = taskByKey.get(key)
      if (task) authorities.push({
        authority: 'task',
        adapterId: task.adapterId,
        projectTokenIds: [task.projectTokenId],
        projectPath: task.projectSemanticPath,
        relation: 'direct'
      })
      const manual = (manualByKey.get(token.observedTokenKey) || [])
        .filter((row) => matchesContext(coordinate.context, row.selectors))
      for (const row of manual) authorities.push({
        authority: 'manual',
        adapterId: row.mapping.adapterId,
        projectTokenIds: row.mapping.projectTokenIds,
        projectPath: null,
        relation: row.mapping.relation === 'transform' ? 'generated' : 'direct'
      })
      for (const adapter of adapterConfig.enabledTokenAdapters) {
        for (const rule of adapter.tokens.bindingRules) {
          if (rule.tokenKind !== observedKind) continue
          const target = ruleTarget(rule, token.displayPath)
          if (target) authorities.push({
            authority: 'rule',
            adapterId: adapter.id,
            projectTokenIds: null,
            projectPath: target,
            relation: 'derived',
            ruleId: rule.ruleId
          })
        }
      }
      const signatures = new Map()
      for (const authority of authorities) {
        const signature = authority.adapterId + '\u0000' +
          (authority.projectTokenIds ? authority.projectTokenIds.slice().sort(compareText).join('\u0001') : pathKey(authority.projectPath))
        const existing = signatures.get(signature)
        if (!existing || existing.authority === 'rule') signatures.set(signature, authority)
      }
      if (signatures.size > 1) {
        snapshot.conflicts.push({
          observedTokenKey: token.observedTokenKey,
          contextKey: coordinateKey,
          code: 'TOKEN_BINDING_AUTHORITY_CONFLICT',
          candidateCount: signatures.size
        })
        continue
      }
      if (!signatures.size) continue
      const authority = signatures.values().next().value
      const adapter = adapterById.get(authority.adapterId)
      if (!adapter) {
        snapshot.conflicts.push({
          observedTokenKey: token.observedTokenKey,
          contextKey: coordinateKey,
          code: 'TOKEN_BINDING_TARGET_INCOMPATIBLE',
          candidateCount: 0
        })
        continue
      }
      const contextRules = adapter.tokens.contextMap.filter((row) => matchesContext(coordinate.context, row.when))
      if (contextRules.length !== 1) {
        snapshot.conflicts.push({
          observedTokenKey: token.observedTokenKey,
          contextKey: coordinateKey,
          code: 'TOKEN_CONTEXT_MAP_REQUIRED',
          candidateCount: contextRules.length
        })
        continue
      }
      const candidates = authority.projectTokenIds
        ? authority.projectTokenIds.map((id) => projectById.get(id)).filter(Boolean)
        : projectByAddress.get(authority.adapterId + '\u0000' + pathKey(authority.projectPath)) || []
      const compatible = candidates.filter((row) => compatibleProjectKind(observedKind, row.token.kind))
      if (candidates.length && !compatible.length) {
        snapshot.conflicts.push({
          observedTokenKey: token.observedTokenKey,
          contextKey: coordinateKey,
          code: 'TOKEN_BINDING_TARGET_INCOMPATIBLE',
          candidateCount: candidates.length
        })
        continue
      }
      if (compatible.length > 1 || authority.projectTokenIds && authority.projectTokenIds.length > 1) {
        snapshot.conflicts.push({
          observedTokenKey: token.observedTokenKey,
          contextKey: coordinateKey,
          code: 'TOKEN_BINDING_AMBIGUOUS',
          candidateCount: compatible.length
        })
        continue
      }
      const project = compatible[0]
      const projectPath = authority.projectPath || (project ? project.token.semanticPath : [authority.projectTokenIds[0]])
      snapshot.bindings.push({
        observedTokenKey: token.observedTokenKey,
        contextKey: coordinateKey,
        ...(project ? { projectTokenId: project.token.projectTokenId } : {}),
        projectSemanticPath: projectPath,
        adapterId: authority.adapterId,
        authority: authority.authority,
        relation: authority.relation,
        targetState: project ? 'present' : 'expected-missing',
        projectMode: contextRules[0].projectMode
      })
    }
  }
  sortSnapshot(snapshot)
  snapshot.semanticHash = canonicalHash({
    schemaVersion: snapshot.schemaVersion,
    observedCatalogHash: snapshot.observedCatalogHash,
    projectAnalysisHash: snapshot.projectAnalysisHash,
    adapterConfigHash: snapshot.adapterConfigHash,
    mappingRevision: snapshot.mappingRevision,
    mappingHash: snapshot.mappingHash,
    bindings: snapshot.bindings,
    suggestions: snapshot.suggestions,
    conflicts: snapshot.conflicts
  })
  return snapshot
}

export function bindingSnapshotSemanticError(snapshot) {
  const keys = new Set()
  for (const binding of snapshot.bindings) {
    const key = binding.observedTokenKey + '\u0000' + binding.contextKey
    if (keys.has(key)) return `duplicate effective binding ${key}`
    keys.add(key)
    if (binding.targetState === 'present' && !binding.projectTokenId) return `present binding ${key} lacks projectTokenId`
    if (binding.targetState === 'expected-missing' && binding.projectTokenId) return `missing binding ${key} carries projectTokenId`
  }
  const expectedHash = canonicalHash({
    schemaVersion: snapshot.schemaVersion,
    observedCatalogHash: snapshot.observedCatalogHash,
    projectAnalysisHash: snapshot.projectAnalysisHash,
    adapterConfigHash: snapshot.adapterConfigHash,
    mappingRevision: snapshot.mappingRevision,
    mappingHash: snapshot.mappingHash,
    bindings: snapshot.bindings,
    suggestions: snapshot.suggestions,
    conflicts: snapshot.conflicts
  })
  if (snapshot.semanticHash !== expectedHash) return 'binding snapshot semanticHash mismatch'
  return null
}
