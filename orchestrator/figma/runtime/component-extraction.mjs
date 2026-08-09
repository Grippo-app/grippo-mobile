// Shared extraction pipeline for the component capability: validated adapter
// config -> per-adapter project component inventories + the analysis index.
// Used by the trusted runner child (local compare) and by task finalization
// (which already runs inside its own writer authority). Pure apart from the
// injected snapshot reads: every output is schema+semantic validated before it
// is returned. Mirrors runtime/token-extraction.mjs module-for-module.
import { takeInputSnapshot } from './input-snapshot.mjs'
import { adapterImplementation } from './adapter-registry.mjs'
import { canonicalHash } from './canonical-json.mjs'
import { typedError } from './typed-error.mjs'
import { ADAPTER_ERROR_CODES } from './error-codes.mjs'
import { projectBranchKey } from './project-identity.mjs'
import { COMPONENT_LIMITS } from '../components/limits.mjs'
import { projectInventorySemanticError, projectInventorySemanticHash, analysisIndexSemanticError } from '../components/project-inventory-contract.mjs'

const SNAPSHOT_LIMITS = Object.freeze({
  filesMax: COMPONENT_LIMITS.projectFilesMax,
  fileBytesMax: COMPONENT_LIMITS.projectFileBytesMax,
  totalBytesMax: COMPONENT_LIMITS.projectTotalBytesMax
})

// Three bounded snapshots per adapter: authoritative component roots plus the
// optional preview/screenshot evidence roots. The fingerprint covers all
// three — evidence changes invalidate the analysis exactly like source
// changes do.
export function componentAdapterSnapshot(projectRoot, adapter, options) {
  const keepText = !!(options && options.keepText)
  const components = adapter.components
  const main = takeInputSnapshot({
    projectRoot,
    roots: components.roots,
    include: components.include,
    exclude: components.exclude || [],
    limits: SNAPSHOT_LIMITS,
    keepText
  })
  const evidenceSnapshot = (roots) => roots && roots.length
    ? takeInputSnapshot({
      projectRoot,
      roots,
      include: components.include,
      exclude: components.exclude || [],
      limits: SNAPSHOT_LIMITS,
      keepText
    })
    : { files: [], rootsMissing: [], fingerprint: null }
  const previews = evidenceSnapshot(components.previewRoots)
  const screenshots = evidenceSnapshot(components.screenshotTestRoots)
  return {
    main,
    previews,
    screenshots,
    branchKey: projectBranchKey(projectRoot),
    fingerprint: canonicalHash({
      main: main.fingerprint,
      previews: previews.fingerprint,
      screenshots: screenshots.fingerprint
    })
  }
}

// The adapter-scope IDENTITY a published inventory and every confirmed
// mapping implementation pin. Deliberately configuration-shaped (same
// doctrine as tokens, §11.10): adapter kind / version / extractor version /
// components config / Git branch. Project SOURCE content is NOT part of it —
// an API edit inside the scope must surface as drift on the next compare,
// never silently retire the mapping to target-out-of-scope.
export function componentScopeFingerprint(adapter, branchKey) {
  const implementation = adapterImplementation(adapter.kind)
  return canonicalHash({
    kind: adapter.kind,
    version: adapter.version,
    extractorVersion: implementation.componentsExtractorVersion || 'none',
    componentsConfig: adapter.components,
    branchKey: typeof branchKey === 'string' && branchKey ? branchKey : 'none'
  })
}

// Build one adapter's component inventory from an already-taken snapshot
// bundle (keepText).
export function buildComponentInventory({ adapter, snapshot, configHash, schemaValidate }) {
  const implementation = adapterImplementation(adapter.kind)
  if (adapter.version !== implementation.version) {
    throw typedError(ADAPTER_ERROR_CODES.PROJECT_ADAPTER_PROTOCOL_UNSUPPORTED,
      `adapter ${adapter.id} requests version ${adapter.version}; shipped ${adapter.kind} is version ${implementation.version}`)
  }
  if (typeof implementation.extractComponents !== 'function') {
    throw typedError(ADAPTER_ERROR_CODES.PROJECT_ADAPTER_PROTOCOL_UNSUPPORTED,
      `adapter kind ${adapter.kind} does not ship the components capability`)
  }
  const extraction = implementation.extractComponents({
    files: snapshot.main.files,
    previewFiles: snapshot.previews.files,
    screenshotTestFiles: snapshot.screenshots.files,
    componentsConfig: adapter.components,
    tokensConfig: adapter.tokens,
    adapterId: adapter.id,
    platform: adapter.platform
  })
  if (extraction.components.length > COMPONENT_LIMITS.projectComponentsMax) {
    throw typedError(ADAPTER_ERROR_CODES.PROJECT_ADAPTER_OUTPUT_INVALID,
      `adapter ${adapter.id} extracted more components than the ${COMPONENT_LIMITS.projectComponentsMax} limit`)
  }
  const failedPaths = new Set(extraction.parseFailures.map((failure) => failure.path))
  const scopeFingerprint = componentScopeFingerprint(adapter, snapshot.branchKey)
  const witness = {
    rootsConfigured: adapter.components.roots.length,
    rootsResolved: adapter.components.roots.length - snapshot.main.rootsMissing.length,
    rootsMissing: snapshot.main.rootsMissing,
    filesMatched: snapshot.main.files.length,
    filesParsed: snapshot.main.files.length - [...failedPaths].filter((path) =>
      snapshot.main.files.some((file) => file.path === path)).length,
    parseFailures: extraction.parseFailures.slice(0, COMPONENT_LIMITS.parseFailuresListedMax),
    limitsHit: extraction.limitations.filter((item) => item.startsWith('limit:')).slice(0, 32),
    complete: false
  }
  witness.complete = witness.rootsMissing.length === 0 &&
    witness.parseFailures.length === 0 &&
    witness.limitsHit.length === 0 &&
    witness.filesParsed === witness.filesMatched
  let variantPropertyTotal = 0
  let slotTotal = 0
  let dependencyTotal = 0
  for (const component of extraction.components) {
    variantPropertyTotal += component.variantProperties.length
    slotTotal += component.slots.length
    dependencyTotal += component.dependencies.length
  }
  const inventory = {
    schemaVersion: 2,
    adapterId: adapter.id,
    adapterKind: adapter.kind,
    adapterVersion: adapter.version,
    platform: adapter.platform,
    scopeFingerprint,
    configHash,
    components: extraction.components,
    witness,
    counts: {
      components: extraction.components.length,
      variantProperties: variantPropertyTotal,
      slots: slotTotal,
      dependencyEdges: dependencyTotal
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

// Full extraction over every enabled component adapter. Returns
// { inventories: [inventory...], index, fingerprintsByAdapterId }.
export function extractProjectComponents({ projectRoot, config, configHash, schemaValidators }) {
  const adapters = config.enabledComponentAdapters
  if (!adapters.length) {
    throw typedError(ADAPTER_ERROR_CODES.PROJECT_ADAPTERS_UNCONFIGURED, 'no enabled component adapters are configured')
  }
  const inventories = []
  const fingerprintsByAdapterId = {}
  for (const adapter of adapters) {
    const snapshot = componentAdapterSnapshot(projectRoot, adapter, { keepText: true })
    fingerprintsByAdapterId[adapter.id] = snapshot.fingerprint
    inventories.push(buildComponentInventory({
      adapter, snapshot, configHash,
      schemaValidate: schemaValidators && schemaValidators.projectInventory
    }))
  }
  const index = {
    schemaVersion: 2,
    configHash,
    adapters: inventories.map((inventory) => ({
      adapterId: inventory.adapterId,
      platform: inventory.platform,
      role: `project-component-inventory:${inventory.adapterId}`,
      inventoryHash: projectInventorySemanticHash(inventory),
      scopeFingerprint: inventory.scopeFingerprint,
      complete: inventory.witness.complete
    })),
    complete: inventories.every((inventory) => inventory.witness.complete)
  }
  if (typeof (schemaValidators && schemaValidators.analysisIndex) === 'function' &&
      !schemaValidators.analysisIndex(index)) {
    throw typedError(ADAPTER_ERROR_CODES.PROJECT_ADAPTER_OUTPUT_INVALID, 'component analysis index schema-invalid')
  }
  const indexError = analysisIndexSemanticError(
    index,
    new Map(inventories.map((inventory) => [inventory.adapterId, inventory])),
    { enabledAdapterIds: adapters.map((adapter) => adapter.id), configHash }
  )
  if (indexError) {
    throw typedError(ADAPTER_ERROR_CODES.PROJECT_ADAPTER_OUTPUT_INVALID, `component analysis index contract-invalid: ${indexError}`)
  }
  return { inventories, index, fingerprintsByAdapterId }
}
