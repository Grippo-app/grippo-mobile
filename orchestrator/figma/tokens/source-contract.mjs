import { canonicalHash, canonicalJson, compareText } from '../runtime/canonical-json.mjs'
import {
  TOKEN_IDENTITY_QUALITY,
  TOKEN_PROVIDER_CAPABILITY,
  TOKEN_SOURCE_BUCKET_COUNT
} from '../runtime/provider-capabilities.mjs'
import limits from '../runtime/program-limits.cjs'
import tokenIdentity from '../runtime/token-identity.cjs'

const HASH_RE = /^sha256:[a-f0-9]{64}$/
const SOURCE_RE = /^otsrc:sha256:[a-f0-9]{64}$/
const TOKEN_KEY_RE = /^otk:sha256:[a-f0-9]{64}$/
const NODE_RE = /^[0-9]+:[0-9]+$/
const CONTROL_RE = /[\u0000-\u001f\u007f-\u009f]/
const CONTEXT_KEYS = Object.freeze(['theme', 'locale', 'platform', 'state'])
const REQUIRED_CONTEXT_KEYS = Object.freeze(['theme', 'locale', 'platform'])
const LIMITATIONS = Object.freeze([
  'usage-scoped-observations-only',
  'no-file-wide-variable-census',
  'no-stable-variable-ids',
  'no-collections-modes-aliases'
])

const isRecord = (value) => !!value && typeof value === 'object' && !Array.isArray(value)

export function canonicalContext(input) {
  if (!isRecord(input)) throw new Error('TOKEN_SOURCE_CONTEXT_INVALID: context must be an object')
  const unknown = Object.keys(input).filter((key) => !CONTEXT_KEYS.includes(key))
  if (unknown.length) throw new Error(`TOKEN_SOURCE_CONTEXT_INVALID: unknown context key ${JSON.stringify(unknown[0])}`)
  const out = {}
  for (const key of REQUIRED_CONTEXT_KEYS) {
    if (typeof input[key] !== 'string' || !input[key]) {
      throw new Error(`TOKEN_SOURCE_CONTEXT_INVALID: context.${key} is required`)
    }
  }
  for (const key of CONTEXT_KEYS) {
    if (!Object.hasOwn(input, key)) continue
    const value = String(input[key]).normalize('NFC')
    if (!value || CONTROL_RE.test(value) || Buffer.byteLength(value, 'utf8') > 64) {
      throw new Error(`TOKEN_SOURCE_CONTEXT_INVALID: context.${key} is invalid`)
    }
    out[key] = value
  }
  return out
}

export function contextKey(context) {
  return tokenIdentity.contextKey(canonicalContext(context))
}

export function canonicalProviderName(input) {
  if (typeof input !== 'string') throw new Error('TOKEN_PROVIDER_NAME_INVALID: provider name must be a string')
  const value = input.normalize('NFC')
  if (!value || value !== input.normalize('NFC') || value.trim() !== value || CONTROL_RE.test(value)) {
    throw new Error('TOKEN_PROVIDER_NAME_INVALID: provider name is empty, framed, or contains controls')
  }
  if (Buffer.byteLength(value, 'utf8') > limits.providerNameBytesMax) {
    throw new Error('TOKEN_PROVIDER_NAME_LIMIT_EXCEEDED')
  }
  return value
}

export function displayPath(providerName) {
  return canonicalProviderName(providerName)
    .split('/')
    .map((segment) => segment.trim())
    .filter(Boolean)
    .slice(0, 32)
    .map((segment) => segment.slice(0, 128))
}

export function sourceIdFor({ fileKeyFingerprint, branchKey, nodeId, context }) {
  if (!HASH_RE.test(String(fileKeyFingerprint || ''))) throw new Error('TOKEN_SOURCE_ID_INVALID: file fingerprint')
  if (typeof branchKey !== 'string' || !branchKey) throw new Error('TOKEN_SOURCE_ID_INVALID: branch key')
  if (!NODE_RE.test(String(nodeId || ''))) throw new Error('TOKEN_SOURCE_ID_INVALID: node id')
  return tokenIdentity.sourceIdFor({ fileKeyFingerprint, branchKey, nodeId, context: canonicalContext(context) })
}

export function observedTokenKeyFor({ fileKeyFingerprint, branchKey, providerName }) {
  if (!HASH_RE.test(String(fileKeyFingerprint || ''))) throw new Error('TOKEN_KEY_INVALID: file fingerprint')
  if (typeof branchKey !== 'string' || !branchKey) throw new Error('TOKEN_KEY_INVALID: branch key')
  return tokenIdentity.observedTokenKeyFor({
    fileKeyFingerprint,
    branchKey,
    providerName: canonicalProviderName(providerName)
  })
}

export function sourceBucket(sourceId) {
  if (!SOURCE_RE.test(String(sourceId || ''))) throw new Error('TOKEN_SOURCE_ID_INVALID')
  return tokenIdentity.sourceBucket(sourceId)
}

export function bucketRole(prefix, bucket) {
  if (!Number.isInteger(bucket) || bucket < 0 || bucket >= TOKEN_SOURCE_BUCKET_COUNT) {
    throw new Error('TOKEN_SOURCE_BUCKET_INVALID')
  }
  return `${prefix}:${String(bucket).padStart(3, '0')}`
}

function originKey(origin) {
  return canonicalJson(origin)
}

export function captureSemanticError(capture, immutablePlan = null) {
  if (!isRecord(capture)) return 'capture must be an object'
  if (capture.providerCapability !== TOKEN_PROVIDER_CAPABILITY) return 'providerCapability mismatch'
  if (capture.provider !== 'figma-mcp') return 'provider mismatch'
  const source = capture.source
  if (!isRecord(source)) return 'source missing'
  let expectedSourceId
  try { expectedSourceId = sourceIdFor(source) } catch (error) { return error.message }
  if (source.sourceId !== expectedSourceId) return 'sourceId does not derive from exact source identity'
  const expectedKind = source.origin &&
    (source.origin.kind === 'task-screen' || source.origin.kind === 'project-config')
    ? 'screen' : 'component'
  if (source.kind !== expectedKind) return 'source kind does not agree with origin kind'
  if (capture.witness.nodeId !== source.nodeId) return 'witness nodeId mismatch'
  if (capture.witness.accountFingerprint !== capture.accountFingerprint) return 'witness accountFingerprint mismatch'
  if (capture.witness.connectorRevision !== capture.connectorRevision) return 'witness connectorRevision mismatch'
  if (capture.witness.observationCount !== capture.observations.length) return 'witness observationCount mismatch'
  if (capture.witness.sourceCompleteness !== 'complete-returned-payload') return 'source payload is incomplete'
  if (capture.witness.truncated) return 'source payload is truncated'
  if (capture.witness.issues.length) return 'source witness carries provider issues'
  const started = Date.parse(capture.witness.startedAt)
  const finished = Date.parse(capture.witness.finishedAt)
  if (!Number.isFinite(started) || !Number.isFinite(finished) || finished < started) return 'witness timing invalid'
  if (immutablePlan) {
    for (const key of ['captureOperationId', 'captureSequence', 'accountFingerprint', 'connectorRevision']) {
      if (capture[key] !== immutablePlan[key]) return `${key} differs from immutable plan`
    }
    if (immutablePlan.sourceId !== source.sourceId) return 'sourceId differs from immutable plan'
  }
  if (Buffer.byteLength(canonicalJson(capture), 'utf8') > limits.tokenLogicalCaptureBytesMax) {
    return 'logical source capture exceeds byte limit'
  }
  return null
}

export function sourceBatchSemanticPayload(batch) {
  return {
    schemaVersion: batch.schemaVersion,
    sourceId: batch.sourceId,
    provider: batch.provider,
    providerCapability: batch.providerCapability,
    fileKeyFingerprint: batch.fileKeyFingerprint,
    branchKey: batch.branchKey,
    nodeId: batch.nodeId,
    kind: batch.kind,
    context: batch.context,
    identityQuality: batch.identityQuality,
    observations: batch.observations
  }
}

export function sourceBatchSemanticError(batch) {
  if (!isRecord(batch)) return 'source batch must be an object'
  if (!SOURCE_RE.test(String(batch.sourceId || ''))) return 'sourceId malformed'
  if (batch.identityQuality !== TOKEN_IDENTITY_QUALITY) return 'identityQuality mismatch'
  if (batch.providerCapability !== TOKEN_PROVIDER_CAPABILITY) return 'providerCapability mismatch'
  if (sourceIdFor(batch) !== batch.sourceId) return 'sourceId does not derive from source identity'
  if (!Array.isArray(batch.origins) || !batch.origins.length) return 'source origins missing'
  const originKeys = batch.origins.map(originKey)
  if (new Set(originKeys).size !== originKeys.length) return 'source origins contain duplicates'
  if (originKeys.some((key, index) => index > 0 && compareText(originKeys[index - 1], key) >= 0)) {
    return 'source origins are not strictly sorted'
  }
  if (batch.witness.observationCount !== batch.observations.length) return 'witness observationCount mismatch'
  const names = new Set()
  let previous = null
  for (const observation of batch.observations) {
    if (!TOKEN_KEY_RE.test(observation.observedTokenKey)) return 'observedTokenKey malformed'
    if (observedTokenKeyFor({ ...batch, providerName: observation.providerName }) !== observation.observedTokenKey) {
      return `observedTokenKey does not derive from providerName ${JSON.stringify(observation.providerName)}`
    }
    if (names.has(observation.providerName)) return `duplicate providerName ${JSON.stringify(observation.providerName)}`
    names.add(observation.providerName)
    if (previous !== null && compareText(previous, observation.providerName) >= 0) return 'observations are not strictly sorted'
    previous = observation.providerName
  }
  if (canonicalHash(sourceBatchSemanticPayload(batch)) !== batch.batchSemanticHash) {
    return 'batchSemanticHash mismatch'
  }
  return null
}

export function sourceIndexSemanticPayload(index) {
  const { semanticHash, ...payload } = index
  return payload
}

export function sourceIndexSemanticError(index, shardsByRole = new Map()) {
  if (!isRecord(index)) return 'source index must be an object'
  const ids = new Set()
  const ordinals = new Set()
  let previous = null
  for (const source of index.sources) {
    if (ids.has(source.sourceId)) return `duplicate sourceId ${source.sourceId}`
    if (ordinals.has(source.ordinal)) return `duplicate source ordinal ${source.ordinal}`
    if (previous !== null && compareText(previous, source.sourceId) >= 0) return 'sources are not strictly sorted'
    ids.add(source.sourceId)
    ordinals.add(source.ordinal)
    previous = source.sourceId
    if (source.ordinal !== ids.size - 1) return 'source ordinals are not dense sorted ordinals'
    if (source.acceptedBatch.shardRole !== bucketRole('observed-token-source-shard', sourceBucket(source.sourceId))) {
      return `source ${source.sourceId} shardRole mismatch`
    }
    const originKeys = source.origins.map(originKey)
    if (new Set(originKeys).size !== originKeys.length) return `source ${source.sourceId} has duplicate origins`
  }
  if (index.counts.active !== index.sources.filter((row) => row.lifecycle === 'active').length) return 'counts.active mismatch'
  if (index.counts.retired !== index.sources.filter((row) => row.lifecycle === 'retired').length) return 'counts.retired mismatch'
  if (index.counts.shards !== index.shards.length) return 'counts.shards mismatch'
  for (const descriptor of index.shards) {
    const shard = shardsByRole.get(descriptor.role)
    if (!shard) continue
    if (shard.semanticHash !== descriptor.hash) return `shard hash mismatch for ${descriptor.role}`
    if (shard.sources.length !== descriptor.sourceCount) return `shard sourceCount mismatch for ${descriptor.role}`
    for (const batch of shard.sources) {
      const source = index.sources.find((row) => row.sourceId === batch.sourceId)
      if (!source || source.acceptedBatch.batchSemanticHash !== batch.batchSemanticHash ||
          source.acceptedBatch.captureSequence !== batch.captureSequence ||
          source.acceptedBatch.observationCount !== batch.observations.length) {
        return `source batch reference mismatch for ${batch.sourceId}`
      }
      if (source.acceptedBatch.previousObservationCount !== undefined &&
          (source.acceptedBatch.observationCount !== 0 ||
            source.acceptedBatch.previousObservationCount < 1)) {
        return `source previous observation count invalid for ${batch.sourceId}`
      }
    }
  }
  if (canonicalHash(sourceIndexSemanticPayload(index)) !== index.semanticHash) return 'source index semanticHash mismatch'
  return null
}

export function catalogSemanticPayload(catalog) {
  const { semanticHash, ...payload } = catalog
  return payload
}

export function catalogSemanticError(catalog, sourceIndex) {
  if (!isRecord(catalog)) return 'catalog must be an object'
  if (sourceIndex) {
    if (catalog.sourceIndexRevision !== sourceIndex.revision) return 'catalog sourceIndexRevision mismatch'
    if (catalog.sourceIndexHash !== sourceIndex.semanticHash) return 'catalog sourceIndexHash mismatch'
  }
  const ids = new Set()
  let previous = null
  for (const token of catalog.tokens) {
    if (ids.has(token.observedTokenKey)) return `duplicate observedTokenKey ${token.observedTokenKey}`
    ids.add(token.observedTokenKey)
    if (previous !== null && compareText(previous, token.observedTokenKey) >= 0) return 'tokens are not strictly sorted'
    previous = token.observedTokenKey
    for (const ref of token.sourceRefs) {
      if (!sourceIndex || !sourceIndex.sources[ref]) return `source ordinal ${ref} is not resolvable`
    }
  }
  const counts = catalog.counts
  if (counts.activeTokens !== catalog.tokens.filter((row) => row.presenceStatus === 'active').length) return 'counts.activeTokens mismatch'
  if (counts.notObserved !== catalog.tokens.filter((row) => row.presenceStatus === 'not-observed').length) return 'counts.notObserved mismatch'
  if (counts.conflicting !== catalog.tokens.filter((row) => row.presenceStatus === 'conflicting').length) return 'counts.conflicting mismatch'
  if (counts.unsupported !== catalog.tokens.filter((row) => row.presenceStatus === 'unsupported').length) return 'counts.unsupported mismatch'
  if (canonicalJson(catalog.limitations) !== canonicalJson(LIMITATIONS)) return 'catalog limitations mismatch'
  if (canonicalHash(catalogSemanticPayload(catalog)) !== catalog.semanticHash) return 'catalog semanticHash mismatch'
  return null
}

export const OBSERVED_TOKEN_LIMITATIONS = LIMITATIONS
