// Semantic contract for per-adapter project token inventories and the
// analysis index. Schema proves shape; this module proves identity
// uniqueness, mode/edge referential integrity, count reconciliation, the
// exact one-of raw/resolved/unsupported mode semantics, and the completeness
// rules that decide whether an inventory may ground absence conclusions.
import { canonicalHash } from '../runtime/canonical-json.mjs'

const isRecord = (value) => !!value && typeof value === 'object' && !Array.isArray(value)

function semanticToken(token) {
  // fileHash proves the exact source bytes consumed by this artifact, but is
  // not token semantics. In particular LF <-> CRLF must not manufacture
  // project drift when the parser projection is otherwise identical.
  const source = { ...token.source }
  delete source.fileHash
  return { ...token, source }
}

export function projectInventorySemanticError(inventory) {
  if (!isRecord(inventory)) return 'inventory must be an object'
  const adapterPrefix = inventory.adapterId + ':'
  const declaredModes = new Set(inventory.modes)
  if (declaredModes.size !== inventory.modes.length) return 'modes must be unique'

  const ids = new Set()
  const caseFolded = new Map()
  let unsupportedModes = 0
  let aliasEdges = 0
  for (const token of inventory.tokens) {
    if (!token.projectTokenId.startsWith(adapterPrefix)) {
      return `projectTokenId ${JSON.stringify(token.projectTokenId)} is not namespaced by adapter ${inventory.adapterId}`
    }
    if (ids.has(token.projectTokenId)) return `duplicate projectTokenId ${JSON.stringify(token.projectTokenId)}`
    ids.add(token.projectTokenId)
    const folded = token.projectTokenId.toLowerCase()
    if (caseFolded.has(folded) && caseFolded.get(folded) !== token.projectTokenId) {
      return `case-folded identity collision between ${caseFolded.get(folded)} and ${token.projectTokenId}`
    }
    caseFolded.set(folded, token.projectTokenId)
    if (token.displayName !== token.semanticPath.join('.')) {
      return `token ${JSON.stringify(token.projectTokenId)} displayName must equal the joined semanticPath`
    }
    for (const [mode, entry] of Object.entries(token.modes)) {
      if (!declaredModes.has(mode)) return `token ${JSON.stringify(token.projectTokenId)} carries undeclared mode ${JSON.stringify(mode)}`
      const hasResolved = entry.resolved !== undefined
      const hasUnsupported = entry.unsupported !== undefined
      if (hasResolved === hasUnsupported) {
        return `token ${JSON.stringify(token.projectTokenId)} mode ${JSON.stringify(mode)} must carry exactly one of resolved/unsupported`
      }
      if (hasResolved && entry.resolved.kind !== token.kind) {
        return `token ${JSON.stringify(token.projectTokenId)} mode ${JSON.stringify(mode)} resolved kind ${entry.resolved.kind} disagrees with token kind ${token.kind}`
      }
      if (hasUnsupported) unsupportedModes++
    }
    for (const edge of token.edges) {
      if (edge.kind === 'alias') aliasEdges++
      if (edge.targetProjectTokenId === token.projectTokenId) {
        return `token ${JSON.stringify(token.projectTokenId)} carries a self-edge`
      }
      if (edge.mode !== undefined && !declaredModes.has(edge.mode)) {
        return `token ${JSON.stringify(token.projectTokenId)} edge references undeclared mode ${JSON.stringify(edge.mode)}`
      }
    }
  }
  // Edge targets must exist inside this inventory (cross-adapter edges are
  // not a supported v1 relation).
  for (const token of inventory.tokens) {
    for (const edge of token.edges) {
      if (!ids.has(edge.targetProjectTokenId)) {
        return `token ${JSON.stringify(token.projectTokenId)} edge targets unknown ${JSON.stringify(edge.targetProjectTokenId)}`
      }
    }
  }

  const witness = inventory.witness
  if (witness.rootsResolved + witness.rootsMissing.length !== witness.rootsConfigured) {
    return 'witness roots do not reconcile: resolved + missing must equal configured'
  }
  if (witness.filesParsed > witness.filesMatched) return 'witness.filesParsed exceeds filesMatched'
  const expectedComplete = witness.rootsMissing.length === 0 &&
    witness.parseFailures.length === 0 &&
    witness.limitsHit.length === 0 &&
    witness.filesParsed === witness.filesMatched
  if (witness.complete !== expectedComplete) {
    return `witness.complete must be ${expectedComplete} given its own evidence`
  }

  const counts = inventory.counts
  if (counts.tokens !== inventory.tokens.length) return 'counts.tokens does not match tokens length'
  if (counts.unsupportedModes !== unsupportedModes) return 'counts.unsupportedModes does not reconcile'
  if (counts.aliasEdges !== aliasEdges) return 'counts.aliasEdges does not reconcile'
  return null
}

// Semantic identity of an inventory (used for index hashes, comparison
// inputs, and staleness checks).
export function projectInventorySemanticHash(inventory) {
  return canonicalHash({
    schemaVersion: inventory.schemaVersion,
    adapterId: inventory.adapterId,
    adapterKind: inventory.adapterKind,
    adapterVersion: inventory.adapterVersion,
    scopeFingerprint: inventory.scopeFingerprint,
    configHash: inventory.configHash,
    modes: inventory.modes,
    tokens: inventory.tokens.map(semanticToken),
    witness: inventory.witness,
    counts: inventory.counts
  })
}

// Cross-artifact contract between the analysis index and its per-adapter
// inventories. inventoriesByAdapterId maps adapterId -> validated inventory.
export function analysisIndexSemanticError(index, inventoriesByAdapterId, expected) {
  if (!isRecord(index)) return 'analysis index must be an object'
  const seen = new Set()
  for (const row of index.adapters) {
    if (seen.has(row.adapterId)) return `analysis index lists adapter ${JSON.stringify(row.adapterId)} twice`
    seen.add(row.adapterId)
    if (row.role !== `project-token-inventory:${row.adapterId}`) {
      return `analysis index role ${JSON.stringify(row.role)} does not derive from its adapterId`
    }
    const inventory = inventoriesByAdapterId.get(row.adapterId)
    if (!inventory) return `analysis index references adapter ${JSON.stringify(row.adapterId)} without an inventory artifact`
    if (projectInventorySemanticHash(inventory) !== row.inventoryHash) {
      return `analysis index inventoryHash for ${row.adapterId} does not match the inventory artifact`
    }
    if (inventory.scopeFingerprint !== row.scopeFingerprint) {
      return `analysis index scopeFingerprint for ${row.adapterId} does not match the inventory artifact`
    }
    if (inventory.witness.complete !== row.complete) {
      return `analysis index completeness for ${row.adapterId} does not match the inventory witness`
    }
    if (inventory.configHash !== index.configHash) {
      return `inventory ${row.adapterId} was produced under a different adapter config`
    }
  }
  if (inventoriesByAdapterId.size !== index.adapters.length) {
    return 'analysis index must list exactly the produced inventories'
  }
  if (expected && expected.enabledAdapterIds) {
    const required = expected.enabledAdapterIds.slice().sort()
    const listed = index.adapters.map((row) => row.adapterId).sort()
    if (required.join('\u0000') !== listed.join('\u0000')) {
      return `analysis index adapters [${listed.join(', ')}] do not equal the enabled token adapters [${required.join(', ')}]`
    }
    if (expected.configHash && expected.configHash !== index.configHash) {
      return 'analysis index configHash does not match the validated adapter config'
    }
  }
  const expectedComplete = index.adapters.every((row) => row.complete)
  if (index.complete !== expectedComplete) return `analysis index complete must be ${expectedComplete}`
  return null
}
