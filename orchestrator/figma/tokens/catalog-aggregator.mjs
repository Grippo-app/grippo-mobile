import { canonicalHash, canonicalJson, compareText } from '../runtime/canonical-json.mjs'
import { TOKEN_IDENTITY_QUALITY, TOKEN_PROVIDER_CAPABILITY, TOKEN_SOURCE_BUCKET_COUNT } from '../runtime/provider-capabilities.mjs'
import limits from '../runtime/program-limits.cjs'
import {
  OBSERVED_TOKEN_LIMITATIONS,
  bucketRole,
  catalogSemanticError,
  catalogSemanticPayload,
  contextKey,
  displayPath,
  sourceBatchSemanticError,
  sourceBucket,
  sourceIndexSemanticError,
  sourceIndexSemanticPayload
} from './source-contract.mjs'

function uniqueSorted(values) {
  return [...new Set(values)].sort(compareText)
}

function withoutHash(value) {
  const { semanticHash, ...payload } = value
  return payload
}

function dedupeOrigins(origins) {
  const byKey = new Map(origins.map((origin) => [canonicalJson(origin), origin]))
  return [...byKey.entries()].sort(([a], [b]) => compareText(a, b)).map(([, origin]) => origin)
}

function pickLatestBatches(batches) {
  const bySource = new Map()
  for (const batch of batches) {
    const semantic = sourceBatchSemanticError(batch)
    if (semantic) throw new Error(`TOKEN_SOURCE_BATCH_INVALID: ${semantic}`)
    const prior = bySource.get(batch.sourceId)
    if (!prior || prior.captureSequence < batch.captureSequence) bySource.set(batch.sourceId, batch)
    else if (prior.captureSequence === batch.captureSequence) {
      if (prior.batchSemanticHash !== batch.batchSemanticHash ||
          prior.captureOperationId !== batch.captureOperationId) {
        throw new Error(`TOKEN_SOURCE_SEQUENCE_CONFLICT: ${batch.sourceId}`)
      }
      if (prior.captureEvidenceHash !== batch.captureEvidenceHash) {
        throw new Error(`TOKEN_SOURCE_EVIDENCE_CONFLICT: ${batch.sourceId}`)
      }
      bySource.set(batch.sourceId, {
        ...prior,
        origins: dedupeOrigins([...prior.origins, ...batch.origins]),
        captureEvidenceHash: prior.captureEvidenceHash
      })
    }
  }
  return bySource
}

export function aggregateObservedTokens({
  scope,
  batches,
  revision = 0,
  previousCatalog = null,
  previousIndex = null,
  retiredSources = []
}) {
  const latest = pickLatestBatches(batches)
  if (latest.size + retiredSources.length > limits.tokenSourceRecordsMax) throw new Error('TOKEN_SOURCE_RECORD_LIMIT_EXCEEDED')
  const totalObservations = [...latest.values()].reduce((sum, batch) => sum + batch.observations.length, 0)
  if (totalObservations > limits.tokenObservationsTotalMax) throw new Error('TOKEN_OBSERVATION_LIMIT_EXCEEDED')
  for (const batch of latest.values()) {
    if (batch.fileKeyFingerprint !== scope.fileKeyFingerprint || batch.branchKey !== scope.branchKey) {
      throw new Error(`TOKEN_SOURCE_SCOPE_CHANGED: ${batch.sourceId}`)
    }
  }

  const sources = [...latest.values()]
    .map((batch) => ({
      sourceId: batch.sourceId,
      lifecycle: 'active',
      nodeId: batch.nodeId,
      kind: batch.kind,
      context: batch.context,
      origins: batch.origins,
      batch
    }))
    .concat(retiredSources.map((source) => ({ ...source, lifecycle: 'retired', batch: source.batch })))
    .sort((a, b) => compareText(a.sourceId, b.sourceId))
  sources.forEach((source, ordinal) => { source.ordinal = ordinal })

  const shardRows = Array.from({ length: TOKEN_SOURCE_BUCKET_COUNT }, () => [])
  for (const source of sources) {
    if (!source.batch) throw new Error(`TOKEN_RETIRED_SOURCE_BATCH_REQUIRED: ${source.sourceId}`)
    shardRows[sourceBucket(source.sourceId)].push(source.batch)
  }
  const shards = []
  const shardsByRole = new Map()
  for (let bucket = 0; bucket < shardRows.length; bucket++) {
    if (!shardRows[bucket].length) continue
    const role = bucketRole('observed-token-source-shard', bucket)
    const shard = {
      schemaVersion: 1,
      bucket,
      sources: shardRows[bucket].sort((a, b) => compareText(a.sourceId, b.sourceId)),
      semanticHash: 'sha256:' + '0'.repeat(64)
    }
    shard.semanticHash = canonicalHash(withoutHash(shard))
    if (Buffer.byteLength(canonicalJson(shard), 'utf8') > limits.artifactBytesMax) throw new Error(`TOKEN_SOURCE_SHARD_LIMIT_EXCEEDED: ${role}`)
    shards.push({ role, shard })
    shardsByRole.set(role, shard)
  }
  const tokenShardsBytes = shards.reduce((sum, row) => sum + Buffer.byteLength(canonicalJson(row.shard), 'utf8'), 0)
  if (tokenShardsBytes > limits.tokenShardsBytesMax) throw new Error('TOKEN_SOURCE_SHARDS_TOTAL_LIMIT_EXCEEDED')

  const index = {
    schemaVersion: 1,
    revision,
    provider: 'figma-mcp',
    providerCapability: TOKEN_PROVIDER_CAPABILITY,
    scope,
    sources: sources.map((source) => {
      const prior = previousIndex && previousIndex.sources
        ? previousIndex.sources.find((row) => row.sourceId === source.sourceId)
        : null
      const observationCount = source.batch.observations.length
      const previousObservationCount = observationCount === 0
        ? prior && prior.acceptedBatch.observationCount > 0
          ? prior.acceptedBatch.observationCount
          : prior && prior.acceptedBatch.previousObservationCount
        : null
      return {
        sourceId: source.sourceId,
        ordinal: source.ordinal,
        lifecycle: source.lifecycle,
        nodeId: source.nodeId,
        kind: source.kind,
        context: source.context,
        origins: dedupeOrigins(source.origins),
        acceptedBatch: {
          shardRole: bucketRole('observed-token-source-shard', sourceBucket(source.sourceId)),
          batchSemanticHash: source.batch.batchSemanticHash,
          captureSequence: source.batch.captureSequence,
          observationCount,
          ...(previousObservationCount ? { previousObservationCount } : {})
        }
      }
    }),
    shards: shards.map(({ role, shard }) => ({ role, hash: shard.semanticHash, sourceCount: shard.sources.length })),
    counts: {
      active: sources.filter((source) => source.lifecycle === 'active').length,
      retired: sources.filter((source) => source.lifecycle === 'retired').length,
      shards: shards.length
    },
    semanticHash: 'sha256:' + '0'.repeat(64)
  }
  index.semanticHash = canonicalHash(sourceIndexSemanticPayload(index))
  const indexError = sourceIndexSemanticError(index, shardsByRole)
  if (indexError) throw new Error(`TOKEN_SOURCE_INDEX_INVALID: ${indexError}`)

  const tokenGroups = new Map()
  for (const source of sources.filter((row) => row.lifecycle === 'active')) {
    for (const observation of source.batch.observations) {
      let group = tokenGroups.get(observation.observedTokenKey)
      if (!group) {
        group = { providerName: observation.providerName, coordinates: new Map(), sourceRefs: new Set(), limitations: new Set() }
        tokenGroups.set(observation.observedTokenKey, group)
      }
      if (group.providerName !== observation.providerName) throw new Error('TOKEN_NAME_CANONICALIZATION_COLLISION')
      const coordinateKey = contextKey(source.context)
      let coordinate = group.coordinates.get(coordinateKey)
      if (!coordinate) {
        coordinate = { context: source.context, values: new Map() }
        group.coordinates.set(coordinateKey, coordinate)
      }
      const valueKey = canonicalJson({ kind: observation.valueKind, value: observation.value })
      let value = coordinate.values.get(valueKey)
      if (!value) {
        value = { kind: observation.valueKind, value: observation.value, sourceRefs: new Set() }
        coordinate.values.set(valueKey, value)
      }
      value.sourceRefs.add(source.ordinal)
      group.sourceRefs.add(source.ordinal)
      if (observation.limitation) group.limitations.add(observation.limitation)
    }
  }
  if (tokenGroups.size > limits.observedTokenKeysMax) throw new Error('TOKEN_KEY_LIMIT_EXCEEDED')

  const currentKeys = new Set(tokenGroups.keys())
  const tokenRows = []
  for (const [observedTokenKey, group] of [...tokenGroups.entries()].sort(([a], [b]) => compareText(a, b))) {
    const coordinates = [...group.coordinates.entries()]
      .sort(([a], [b]) => compareText(a, b))
      .map(([, coordinate]) => {
        const values = [...coordinate.values.entries()]
          .sort(([a], [b]) => compareText(a, b))
          .map(([, value]) => ({ ...value, sourceRefs: [...value.sourceRefs].sort((a, b) => a - b) }))
        const supported = values.filter((value) => value.kind !== 'unsupported')
        return {
          context: coordinate.context,
          status: supported.length === 0 ? 'unsupported' : supported.length > 1 || values.length > supported.length ? 'source-conflict' : 'consistent',
          values
        }
      })
    const anyConflict = coordinates.some((row) => row.status === 'source-conflict')
    const allUnsupported = coordinates.length > 0 && coordinates.every((row) => row.status === 'unsupported')
    tokenRows.push({
      observedTokenKey,
      providerName: group.providerName,
      displayPath: displayPath(group.providerName),
      identityQuality: TOKEN_IDENTITY_QUALITY,
      presenceStatus: anyConflict ? 'conflicting' : allUnsupported ? 'unsupported' : 'active',
      coordinates,
      sourceRefs: [...group.sourceRefs].sort((a, b) => a - b),
      limitations: uniqueSorted(group.limitations)
    })
  }
  for (const prior of previousCatalog ? previousCatalog.tokens : []) {
    if (currentKeys.has(prior.observedTokenKey)) continue
    tokenRows.push({
      observedTokenKey: prior.observedTokenKey,
      providerName: prior.providerName,
      displayPath: prior.displayPath,
      identityQuality: TOKEN_IDENTITY_QUALITY,
      presenceStatus: 'not-observed',
      coordinates: [],
      sourceRefs: [],
      limitations: ['not-observed-after-source-replacement']
    })
  }
  tokenRows.sort((a, b) => compareText(a.observedTokenKey, b.observedTokenKey))

  const catalog = {
    schemaVersion: 1,
    providerCapability: TOKEN_PROVIDER_CAPABILITY,
    identityQuality: TOKEN_IDENTITY_QUALITY,
    scope,
    sourceIndexRevision: index.revision,
    sourceIndexHash: index.semanticHash,
    tokens: tokenRows,
    counts: {
      activeSources: index.counts.active,
      activeTokens: tokenRows.filter((row) => row.presenceStatus === 'active').length,
      notObserved: tokenRows.filter((row) => row.presenceStatus === 'not-observed').length,
      conflicting: tokenRows.filter((row) => row.presenceStatus === 'conflicting').length,
      unsupported: tokenRows.filter((row) => row.presenceStatus === 'unsupported').length,
      observations: totalObservations
    },
    limitations: [...OBSERVED_TOKEN_LIMITATIONS],
    semanticHash: 'sha256:' + '0'.repeat(64)
  }
  catalog.semanticHash = canonicalHash(catalogSemanticPayload(catalog))
  const catalogError = catalogSemanticError(catalog, index)
  if (catalogError) throw new Error(`TOKEN_CATALOG_INVALID: ${catalogError}`)
  const indexCatalogBytes = Buffer.byteLength(canonicalJson(index), 'utf8') + Buffer.byteLength(canonicalJson(catalog), 'utf8')
  if (indexCatalogBytes > limits.tokenIndexCatalogBytesMax) throw new Error('TOKEN_INDEX_CATALOG_LIMIT_EXCEEDED')
  return { shards, index, catalog }
}
