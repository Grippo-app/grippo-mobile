import { existsSync, readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { createSchemaRegistry } from '../../runtime/schema-registry.mjs'
import {
  catalogSemanticError,
  sourceIndexSemanticError
} from '../../tokens/source-contract.mjs'
import { bindingSnapshotSemanticError } from '../../tokens/binder.mjs'
import { readGenerationArtifact } from './generation-artifacts.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const schemas = createSchemaRegistry(join(HERE, '..', '..', 'schemas'))
const resync = (detail) => new Error(`TOKEN_GENERATION_RESYNC_REQUIRED: ${detail}`)

function parse(bytes, label) {
  try { return JSON.parse(bytes.toString('utf8')) }
  catch (error) { throw resync(`${label} is not valid JSON (${error.message})`) }
}

function fixtureOrGeneration(options, optionKey, role) {
  const fixture = options && options[optionKey]
  if (fixture) {
    if (!existsSync(fixture)) return { present: false }
    return { present: true, bytes: readFileSync(fixture), generationId: null }
  }
  return readGenerationArtifact(role, resync)
}

export function loadObservedTokenDomain(options = {}) {
  const catalogHit = fixtureOrGeneration(options, 'fixtureCatalogFile', 'observed-token-catalog')
  const indexHit = fixtureOrGeneration(options, 'fixtureSourceIndexFile', 'observed-token-source-index')
  if (!catalogHit.present && !indexHit.present) return { present: false }
  if (!catalogHit.present || !indexHit.present) throw resync('observed token catalog/source index pair is incomplete')
  const catalog = parse(catalogHit.bytes, 'observed token catalog')
  const sourceIndex = parse(indexHit.bytes, 'observed token source index')
  const validateCatalog = schemas.validate('observed-token-catalog')
  const validateIndex = schemas.validate('observed-token-source-index')
  if (!validateCatalog(catalog)) throw resync('observed token catalog fails the current schema')
  if (!validateIndex(sourceIndex)) throw resync('observed token source index fails the current schema')
  const indexError = sourceIndexSemanticError(sourceIndex)
  if (indexError) throw resync(indexError)
  const catalogError = catalogSemanticError(catalog, sourceIndex)
  if (catalogError) throw resync(catalogError)
  if (catalog.scope.fileKeyFingerprint !== sourceIndex.scope.fileKeyFingerprint ||
      catalog.scope.branchKey !== sourceIndex.scope.branchKey) throw resync('catalog/source index scope mismatch')
  return {
    present: true,
    catalog,
    sourceIndex,
    generationId: catalogHit.generationId,
    catalogArtifactHash: catalogHit.artifactHash || null,
    sourceIndexArtifactHash: indexHit.artifactHash || null
  }
}

export function loadPublishedTokenAnalysis(options = {}) {
  const indexHit = fixtureOrGeneration(options, 'fixtureAnalysisIndexFile', 'project-token-analysis-index')
  if (!indexHit.present) return { present: false }
  const index = parse(indexHit.bytes, 'project token analysis index')
  const validateIndex = schemas.validate('project-token-analysis-index')
  if (!validateIndex(index)) throw resync('project token analysis index fails the current schema')
  return { present: true, index, generationId: indexHit.generationId }
}

export function loadPublishedBindingSnapshot(options = {}) {
  const hit = fixtureOrGeneration(options, 'fixtureBindingSnapshotFile', 'token-binding-snapshot')
  if (!hit.present) return { present: false }
  const snapshot = parse(hit.bytes, 'token binding snapshot')
  const validate = schemas.validate('token-binding-snapshot')
  if (!validate(snapshot)) throw resync('token binding snapshot fails the current schema')
  const semantic = bindingSnapshotSemanticError(snapshot)
  if (semantic) throw resync(semantic)
  return { present: true, snapshot, generationId: hit.generationId }
}
