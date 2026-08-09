import assert from 'node:assert/strict'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { canonicalJson } from '../runtime/canonical-json.mjs'
import { createSchemaRegistry } from '../runtime/schema-registry.mjs'
import { aggregateObservedTokens } from '../tokens/catalog-aggregator.mjs'
import {
  bucketRole,
  catalogSemanticError,
  sourceBatchSemanticError,
  sourceBucket,
  sourceIdFor
} from '../tokens/source-contract.mjs'
import { normalizeSourceCapture } from '../tokens/source-normalizer.mjs'
import {
  FILE_FINGERPRINT,
  immutablePlan,
  sourceIdentity,
  validObservedCapture
} from './observed-token-fixtures.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const registry = createSchemaRegistry(join(HERE, '..', 'schemas'))
let passed = 0
function check(name, fn) {
  try {
    fn()
    passed++
    console.log(`PASS ${name}`)
  } catch (error) {
    console.error(`FAIL ${name}\n  ${error.stack || error.message}`)
    process.exitCode = 1
  }
}
function bytes(value) {
  return Buffer.from(JSON.stringify(value), 'utf8')
}
function normalized(capture = validObservedCapture()) {
  return normalizeSourceCapture(capture, bytes(capture), immutablePlan(capture))
}
function assertSchema(name, value) {
  const validate = registry.validate(name)
  assert.equal(validate(value), true, JSON.stringify(validate.errors))
}
const scope = { fileKeyFingerprint: FILE_FINGERPRINT, branchKey: 'none' }

check('all strict schemas load through the shared registry', () => {
  assert.ok(registry.names.includes('observed-token-source-capture'))
  assert.ok(registry.names.includes('observed-token-catalog'))
  assert.ok(registry.names.includes('token-binding-snapshot'))
})

check('source capture and normalized batch satisfy strict schemas and semantics', () => {
  const capture = validObservedCapture()
  assertSchema('observed-token-source-capture', capture)
  const batch = normalized(capture)
  assertSchema('observed-token-source', batch)
  assert.equal(sourceBatchSemanticError(batch), null)
  const unknown = { ...capture, unexpected: true }
  assert.equal(registry.validate('observed-token-source-capture')(unknown), false)
})

check('unknown schema versions fail closed', () => {
  const capture = validObservedCapture()
  capture.schemaVersion = 2
  assert.equal(registry.validate('observed-token-source-capture')(capture), false)
})

check('source identity is exact, deterministic, and context-sensitive', () => {
  const source = sourceIdentity()
  assert.equal(sourceIdFor(source), source.sourceId)
  assert.notEqual(sourceIdFor({ ...source, context: { ...source.context, theme: 'dark' } }), source.sourceId)
  assert.throws(() => normalized(validObservedCapture({ source: { ...source, sourceId: 'otsrc:sha256:' + '0'.repeat(64) } })), /sourceId/)
})

check('normalizer preserves the supported value matrix without invented units or modes', () => {
  const batch = normalized()
  const byName = new Map(batch.observations.map((row) => [row.providerName, row]))
  assert.deepEqual(byName.get('color/content/primary').value, { space: 'srgb', hex: '#336699FF' })
  assert.equal(byName.get('spacing/md').valueKind, 'number')
  assert.equal(byName.get('content/title').valueKind, 'string')
  assert.equal(byName.get('feature/enabled').valueKind, 'boolean')
  assert.equal(Object.hasOwn(byName.get('spacing/md'), 'unit'), false)
})

check('credential-like values block publication without leaking the value', () => {
  const secret = 'sk-proj-' + 'S'.repeat(32)
  for (const rawValue of [
    secret,
    { nested: ['safe', secret] }
  ]) {
    const capture = validObservedCapture({
      observations: [{ providerName: 'content/private', rawValue, providerType: 'STRING' }]
    })
    assert.throws(
      () => normalized(capture),
      (error) => {
        assert.equal(error && error.code, 'TOKEN_SENSITIVE_VALUE_REJECTED')
        assert.equal(String(error && error.message).includes(secret), false)
        assert.equal(String(error && error.stack).includes(secret), false)
        return true
      }
    )
  }
})

check('untyped canonical colors are recognized but typed STRING stays string', () => {
  const capture = validObservedCapture({
    observations: [
      { providerName: 'untyped/color', rawValue: '#abcdef' },
      { providerName: 'typed/string', rawValue: '#abcdef', providerType: 'STRING' }
    ]
  })
  const batch = normalized(capture)
  assert.equal(batch.observations[0].valueKind, 'string')
  assert.equal(batch.observations[1].valueKind, 'color')
})

check('duplicate identical observations dedupe and conflicting duplicates block', () => {
  const row = { providerName: 'spacing/md', rawValue: 16, providerType: 'FLOAT' }
  const same = validObservedCapture({ observations: [row, { ...row }] })
  assert.equal(normalized(same).observations.length, 1)
  const conflict = validObservedCapture({ observations: [row, { ...row, rawValue: 20 }] })
  assert.throws(() => normalized(conflict), /TOKEN_SOURCE_DUPLICATE_CONFLICT/)
})

check('exact empty capture is valid while incomplete/truncated evidence blocks', () => {
  const empty = validObservedCapture({ observations: [] })
  assert.equal(normalized(empty).observations.length, 0)
  const truncated = validObservedCapture({ witness: { truncated: true } })
  assert.throws(() => normalized(truncated), /truncated/)
  const issue = validObservedCapture({ witness: { issues: ['rate-limit'] } })
  assert.throws(() => normalized(issue), /provider issues/)
})

check('immutable plan identity mismatch blocks before normalization', () => {
  const capture = validObservedCapture()
  const plan = immutablePlan(capture)
  plan.connectorRevision = 'different'
  assert.throws(() => normalizeSourceCapture(capture, bytes(capture), plan), /immutable plan/)
})

check('semantic output is independent from capture order and witness timing', () => {
  const first = validObservedCapture()
  const second = validObservedCapture({
    observations: [...first.observations].reverse(),
    witness: { startedAt: '2026-07-23T11:00:00.000Z', finishedAt: '2026-07-23T11:00:01.000Z' }
  })
  assert.equal(normalized(first).batchSemanticHash, normalized(second).batchSemanticHash)
})

check('fixed 128-bucket assignment and role formatting are deterministic', () => {
  const sourceId = sourceIdentity().sourceId
  const bucket = sourceBucket(sourceId)
  assert.ok(bucket >= 0 && bucket < 128)
  assert.match(bucketRole('observed-token-source-shard', bucket), /^observed-token-source-shard:[0-9]{3}$/)
})

check('one accepted source in every fixed bucket publishes exactly all 128 shard roles', () => {
  const byBucket = new Map()
  for (let candidate = 1; byBucket.size < 128 && candidate <= 100000; candidate++) {
    const source = sourceIdentity({
      nodeId: `128:${candidate}`,
      origin: {
        kind: 'task-screen',
        taskStem: `TASK_${candidate}_Shard`,
        screenKey: `Shard${candidate}`,
        variantId: 'light-default-shared'
      }
    })
    const bucket = sourceBucket(source.sourceId)
    if (!byBucket.has(bucket)) byBucket.set(bucket, source)
  }
  assert.equal(byBucket.size, 128)
  const batches = [...byBucket.entries()].sort(([left], [right]) => left - right).map(([bucket, source]) => {
    const capture = validObservedCapture({
      source,
      captureOperationId: `tokop_${bucket.toString(16).padStart(16, '0')}`,
      captureSequence: bucket + 1
    })
    return normalized(capture)
  })
  const result = aggregateObservedTokens({ scope, batches, revision: 128 })
  assert.equal(result.shards.length, 128)
  assert.deepEqual(
    result.shards.map((row) => row.role),
    Array.from({ length: 128 }, (_, bucket) =>
      `observed-token-source-shard:${String(bucket).padStart(3, '0')}`)
  )
  assert.ok(result.shards.every((row) => row.shard.sources.length === 1))
  assert.equal(result.index.counts.shards, 128)
})

check('aggregation is order-independent and publishes exact source/index/catalog hashes', () => {
  const light = normalized()
  const darkSource = sourceIdentity({
    nodeId: '10:21',
    context: { theme: 'dark', locale: 'default', platform: 'shared' },
    origin: {
      kind: 'task-screen',
      taskStem: 'TASK_43_HomeDark',
      screenKey: 'Home',
      variantId: 'dark-default-shared'
    }
  })
  const darkCapture = validObservedCapture({ source: darkSource, captureSequence: 2 })
  const dark = normalized(darkCapture)
  const a = aggregateObservedTokens({ scope, batches: [light, dark], revision: 1 })
  const b = aggregateObservedTokens({ scope, batches: [dark, light], revision: 1 })
  assert.equal(canonicalJson(a), canonicalJson(b))
  assertSchema('observed-token-source-index', a.index)
  assertSchema('observed-token-catalog', a.catalog)
  for (const row of a.shards) assertSchema('observed-token-source-shard', row.shard)
  assert.equal(catalogSemanticError(a.catalog, a.index), null)
  assert.equal(a.catalog.counts.activeSources, 2)
})

check('same token/context with different values is a representable source conflict', () => {
  const first = normalized()
  const secondSource = sourceIdentity({
    nodeId: '10:22',
    origin: {
      kind: 'task-screen',
      taskStem: 'TASK_44_HomeCopy',
      screenKey: 'HomeCopy',
      variantId: 'light-default-shared'
    }
  })
  const secondCapture = validObservedCapture({
    source: secondSource,
    captureSequence: 2,
    observations: [{ providerName: 'spacing/md', rawValue: 20, providerType: 'FLOAT' }]
  })
  const result = aggregateObservedTokens({ scope, batches: [first, normalized(secondCapture)], revision: 2 })
  const token = result.catalog.tokens.find((row) => row.providerName === 'spacing/md')
  assert.equal(token.presenceStatus, 'conflicting')
  assert.equal(token.coordinates[0].status, 'source-conflict')
  assert.equal(token.coordinates[0].values.length, 2)
})

check('source replacement removes stale contributions without claiming design deletion', () => {
  const initialBatch = normalized()
  const initial = aggregateObservedTokens({ scope, batches: [initialBatch], revision: 1 })
  const emptyCapture = validObservedCapture({ captureSequence: 2, observations: [] })
  const replacement = aggregateObservedTokens({
    scope,
    batches: [normalized(emptyCapture)],
    revision: 2,
    previousCatalog: initial.catalog,
    previousIndex: initial.index
  })
  assert.equal(replacement.catalog.counts.notObserved, initial.catalog.tokens.length)
  assert.ok(replacement.catalog.tokens.every((row) => row.presenceStatus === 'not-observed'))
  assert.ok(replacement.catalog.tokens.every((row) => row.limitations.includes('not-observed-after-source-replacement')))
  assert.equal(replacement.index.sources[0].lifecycle, 'active')
  assert.equal(replacement.index.sources[0].acceptedBatch.observationCount, 0)
  assert.equal(
    replacement.index.sources[0].acceptedBatch.previousObservationCount,
    initialBatch.observations.length
  )
})

check('tampering with a batch semantic hash is rejected', () => {
  const batch = normalized()
  batch.batchSemanticHash = 'sha256:' + 'f'.repeat(64)
  assert.throws(() => aggregateObservedTokens({ scope, batches: [batch] }), /batchSemanticHash mismatch/)
})

if (!process.exitCode) console.log(`observed token domain: ${passed} checks passed`)
