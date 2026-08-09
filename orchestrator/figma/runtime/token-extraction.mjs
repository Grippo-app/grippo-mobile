// Shared extraction pipeline: validated adapter config -> per-adapter
// project token inventories + the analysis index. Used by the trusted runner
// child (local compare) and by task finalization (which already runs inside
// its own writer authority). Pure apart from the injected snapshot reads:
// every output is schema+semantic validated before it is returned.
import { takeInputSnapshot } from './input-snapshot.mjs'
import { adapterImplementation } from './adapter-registry.mjs'
import { canonicalHash } from './canonical-json.mjs'
import { typedError } from './typed-error.mjs'
import { ADAPTER_ERROR_CODES } from './error-codes.mjs'
import { TOKEN_LIMITS } from '../tokens/limits.mjs'
import { projectInventorySemanticError, projectInventorySemanticHash, analysisIndexSemanticError } from '../tokens/project-inventory-contract.mjs'
import { projectBranchKey } from './project-identity.mjs'

const SNAPSHOT_LIMITS = Object.freeze({
  filesMax: TOKEN_LIMITS.projectFilesMax,
  fileBytesMax: TOKEN_LIMITS.projectFileBytesMax,
  totalBytesMax: TOKEN_LIMITS.projectTotalBytesMax
})

export function adapterSnapshot(projectRoot, adapter, options) {
  const snapshot = takeInputSnapshot({
    projectRoot,
    roots: adapter.tokens.roots,
    include: adapter.tokens.include,
    exclude: adapter.tokens.exclude || [],
    limits: SNAPSHOT_LIMITS,
    keepText: !!(options && options.keepText)
  })
  snapshot.branchKey = projectBranchKey(projectRoot)
  return snapshot
}

// The adapter-scope IDENTITY a published inventory and every confirmed
// mapping pin. Deliberately configuration-shaped (§11.10): adapter kind /
// version / extractor version / tokens config / Git branch. Project SOURCE
// content is NOT part of it — a value edit inside the scope must surface as
// value-drift on the next compare, never silently retire the mapping to
// target-out-of-scope.
export function adapterScopeFingerprint(adapter, branchKey) {
  const implementation = adapterImplementation(adapter.kind)
  return canonicalHash({
    kind: adapter.kind,
    version: adapter.version,
    extractorVersion: implementation.extractorVersion,
    tokensConfig: adapter.tokens,
    branchKey: typeof branchKey === 'string' && branchKey ? branchKey : 'none'
  })
}

// Build one adapter's inventory from an already-taken snapshot (keepText).
export function buildAdapterInventory({ adapter, snapshot, configHash, schemaValidate }) {
  const implementation = adapterImplementation(adapter.kind)
  if (adapter.version !== implementation.version) {
    throw typedError(ADAPTER_ERROR_CODES.PROJECT_ADAPTER_PROTOCOL_UNSUPPORTED,
      `adapter ${adapter.id} requests version ${adapter.version}; shipped ${adapter.kind} is version ${implementation.version}`)
  }
  const extraction = implementation.extractTokens({
    files: snapshot.files,
    tokensConfig: adapter.tokens,
    adapterId: adapter.id
  })
  const failedPaths = new Set(extraction.parseFailures.map((failure) => failure.path))
  const scopeFingerprint = adapterScopeFingerprint(adapter, snapshot.branchKey)
  let unsupportedModes = 0
  let aliasEdges = 0
  for (const token of extraction.tokens) {
    for (const entry of Object.values(token.modes)) if (entry.unsupported !== undefined) unsupportedModes++
    for (const edge of token.edges) if (edge.kind === 'alias') aliasEdges++
  }
  const witness = {
    rootsConfigured: adapter.tokens.roots.length,
    rootsResolved: adapter.tokens.roots.length - snapshot.rootsMissing.length,
    rootsMissing: snapshot.rootsMissing,
    filesMatched: snapshot.files.length,
    filesParsed: snapshot.files.length - failedPaths.size,
    parseFailures: extraction.parseFailures.slice(0, TOKEN_LIMITS.parseFailuresListedMax),
    limitsHit: extraction.limitations.filter((item) => item.startsWith('limit:')).slice(0, 32),
    complete: false
  }
  witness.complete = witness.rootsMissing.length === 0 &&
    witness.parseFailures.length === 0 &&
    witness.limitsHit.length === 0 &&
    witness.filesParsed === witness.filesMatched
  const inventory = {
    schemaVersion: 2,
    adapterId: adapter.id,
    adapterKind: adapter.kind,
    adapterVersion: adapter.version,
    scopeFingerprint,
    configHash,
    modes: adapter.tokens.modes,
    tokens: extraction.tokens,
    witness,
    counts: {
      tokens: extraction.tokens.length,
      unsupportedModes,
      aliasEdges
    }
  }
  if (typeof schemaValidate === 'function' && !schemaValidate(inventory)) {
    const first = (schemaValidate.errors || [])[0]
    throw typedError(ADAPTER_ERROR_CODES.PROJECT_ADAPTER_OUTPUT_INVALID,
      `adapter ${adapter.id} output schema-invalid: ${(first && (first.instancePath || '/') + ' ' + first.message) || 'unknown'}`)
  }
  const semantic = projectInventorySemanticError(inventory)
  if (semantic) {
    throw typedError(ADAPTER_ERROR_CODES.PROJECT_ADAPTER_OUTPUT_INVALID,
      `adapter ${adapter.id} output contract-invalid: ${semantic}`)
  }
  return inventory
}

// Full extraction over every enabled token adapter. Returns
// { inventories: [inventory...], index, fingerprintsByAdapterId }.
export function extractProjectTokens({ projectRoot, config, configHash, schemaValidators }) {
  const adapters = config.enabledTokenAdapters
  if (!adapters.length) {
    throw typedError(ADAPTER_ERROR_CODES.PROJECT_ADAPTERS_UNCONFIGURED, 'no enabled token adapters are configured')
  }
  const inventories = []
  const fingerprintsByAdapterId = {}
  for (const adapter of adapters) {
    const snapshot = adapterSnapshot(projectRoot, adapter, { keepText: true })
    fingerprintsByAdapterId[adapter.id] = snapshot.fingerprint
    inventories.push(buildAdapterInventory({
      adapter, snapshot, configHash,
      schemaValidate: schemaValidators && schemaValidators.projectInventory
    }))
  }
  const index = {
    schemaVersion: 2,
    configHash,
    adapters: inventories.map((inventory) => ({
      adapterId: inventory.adapterId,
      role: `project-token-inventory:${inventory.adapterId}`,
      inventoryHash: projectInventorySemanticHash(inventory),
      scopeFingerprint: inventory.scopeFingerprint,
      complete: inventory.witness.complete
    })),
    complete: inventories.every((inventory) => inventory.witness.complete)
  }
  if (typeof (schemaValidators && schemaValidators.analysisIndex) === 'function' &&
      !schemaValidators.analysisIndex(index)) {
    throw typedError(ADAPTER_ERROR_CODES.PROJECT_ADAPTER_OUTPUT_INVALID, 'analysis index schema-invalid')
  }
  const indexError = analysisIndexSemanticError(
    index,
    new Map(inventories.map((inventory) => [inventory.adapterId, inventory])),
    { enabledAdapterIds: adapters.map((adapter) => adapter.id), configHash }
  )
  if (indexError) {
    throw typedError(ADAPTER_ERROR_CODES.PROJECT_ADAPTER_OUTPUT_INVALID, `analysis index contract-invalid: ${indexError}`)
  }
  return { inventories, index, fingerprintsByAdapterId }
}
