import assert from 'node:assert/strict'
import { copyFileSync, existsSync, mkdtempSync, mkdirSync, readFileSync, rmSync, symlinkSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'
import { bytesHash, canonicalHash, canonicalJson } from '../runtime/canonical-json.mjs'
import { sourceBucket } from '../tokens/source-contract.mjs'
import { FILE_FINGERPRINT, validObservedCapture } from './observed-token-fixtures.mjs'
import { normalizeSourceCapture } from '../tokens/source-normalizer.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const RUN_PLAN = join(HERE, '..', 'runtime', 'run-plan.mjs')
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
function runPlan(stageDir, plan) {
  const planFile = join(stageDir, '.run-plan.json')
  writeFileSync(planFile, JSON.stringify({ ...plan, stageDir }))
  const child = spawnSync(process.execPath, [RUN_PLAN, '--plan', planFile], { encoding: 'utf8', env: {} })
  assert.equal(child.status, 0, child.stderr)
  const lines = child.stdout.split('\n').filter(Boolean)
  return JSON.parse(lines.at(-1))
}
function captureShard(capture) {
  const logicalBytes = Buffer.from(canonicalJson(capture), 'utf8')
  return {
    schemaVersion: 1,
    bucket: sourceBucket(capture.source.sourceId),
    captures: [{
      captureOperationId: capture.captureOperationId,
      captureSequence: capture.captureSequence,
      captureBytesHash: bytesHash(logicalBytes),
      semanticPreflightHash: canonicalHash({
        captureOperationId: capture.captureOperationId,
        captureSequence: capture.captureSequence,
        sourceId: capture.source.sourceId
      }),
      capture
    }]
  }
}
function expected(capture) {
  return {
    sourceId: capture.source.sourceId,
    captureOperationId: capture.captureOperationId,
    captureSequence: capture.captureSequence,
    accountFingerprint: capture.accountFingerprint,
    connectorRevision: capture.connectorRevision
  }
}
function exactPlan(stage, files = ['capture-000.json'], captures = [validObservedCapture()], originsBySource = new Map()) {
  const byBucket = new Map()
  for (const capture of captures) {
    const bucket = sourceBucket(capture.source.sourceId)
    const rows = byBucket.get(bucket) || []
    rows.push({
      captureOperationId: capture.captureOperationId,
      captureSequence: capture.captureSequence,
      accountFingerprint: capture.accountFingerprint,
      connectorRevision: capture.connectorRevision,
      semanticPreflightHash: canonicalHash({
        captureOperationId: capture.captureOperationId,
        captureSequence: capture.captureSequence,
        sourceId: capture.source.sourceId
      }),
      source: capture.source,
      origins: originsBySource.get(capture.source.sourceId) || [capture.source.origin]
    })
    byBucket.set(bucket, rows)
  }
  const expectedCapturePlanFiles = []
  for (const [bucket, records] of byBucket) {
    const name = `refresh-plan-${String(bucket).padStart(3, '0')}.json`
    writeFileSync(join(stage, name), JSON.stringify({
      schemaVersion: 1,
      bucket,
      scope: { fileKeyFingerprint: FILE_FINGERPRINT, branchKey: 'none' },
      sourceIndexHash: 'uninitialized',
      records
    }))
    expectedCapturePlanFiles.push(name)
  }
  return {
    op: 'normalize-token-captures',
    captureShardFiles: files,
    expectedCapturePlanFiles,
    outDir: 'publication',
    scope: { fileKeyFingerprint: FILE_FINGERPRINT, branchKey: 'none' },
    revision: 1
  }
}

function copyPublishedTokenDomain(seed, stage, sourceId) {
  const shardFile = `source-shard-${String(sourceBucket(sourceId)).padStart(3, '0')}.json`
  copyFileSync(join(seed, 'publication', shardFile), join(stage, 'existing-shard.json'))
  copyFileSync(join(seed, 'publication', 'source-index.json'), join(stage, 'previous-index.json'))
  copyFileSync(join(seed, 'publication', 'observed-token-catalog.json'), join(stage, 'previous-catalog.json'))
  return JSON.parse(readFileSync(join(seed, 'publication', 'source-index.json'), 'utf8'))
}

function ingestionPlan(sourceIndex, overrides = {}) {
  return {
    op: 'ingest-token-receipt',
    captureFiles: [],
    existingSourceShardFiles: ['existing-shard.json'],
    intentSources: [],
    outDir: 'publication',
    scope: { fileKeyFingerprint: FILE_FINGERPRINT, branchKey: 'none' },
    revision: sourceIndex.revision + 1,
    previousCatalogFile: 'previous-catalog.json',
    previousIndexFile: 'previous-index.json',
    ...overrides
  }
}

check('runner separates capture intake from normalized publication output', () => {
  const stage = mkdtempSync(join(tmpdir(), 'observed-token-runner-'))
  try {
    const capture = validObservedCapture()
    writeFileSync(join(stage, 'capture-000.json'), JSON.stringify(captureShard(capture)))
    const result = runPlan(stage, exactPlan(stage, undefined, [capture]))
    assert.equal(result.ok, true, JSON.stringify(result))
    assert.equal(result.counts.activeSources, 1)
    assert.ok(result.artifacts.some((row) => row.role === 'observed-token-source-index'))
    assert.ok(result.artifacts.some((row) => row.role === 'observed-token-catalog'))
    const normalized = normalizeSourceCapture(
      capture,
      Buffer.from(canonicalJson(capture), 'utf8'),
      expected(capture)
    )
    assert.deepEqual(result.healthEvidence, [{
      sourceId: capture.source.sourceId,
      captureOperationId: capture.captureOperationId,
      captureSequence: capture.captureSequence,
      captureEvidenceHash: normalized.captureEvidenceHash
    }])
    assert.equal(existsSync(join(stage, 'publication', 'source-index.json')), true)
    assert.equal(existsSync(join(stage, 'publication', 'observed-token-catalog.json')), true)
    const catalog = JSON.parse(readFileSync(join(stage, 'publication', 'observed-token-catalog.json'), 'utf8'))
    assert.equal(catalog.counts.activeTokens, 4)
  } finally {
    rmSync(stage, { recursive: true, force: true })
  }
})

check('refresh publication preserves every server-planned provenance origin', () => {
  const stage = mkdtempSync(join(tmpdir(), 'observed-token-runner-'))
  try {
    const capture = validObservedCapture()
    const secondOrigin = {
      kind: 'task-screen',
      taskStem: 'TASK_43_Settings',
      screenKey: 'Settings',
      variantId: 'light-default-shared'
    }
    writeFileSync(join(stage, 'capture-000.json'), JSON.stringify(captureShard(capture)))
    const result = runPlan(stage, exactPlan(
      stage,
      undefined,
      [capture],
      new Map([[capture.source.sourceId, [capture.source.origin, secondOrigin]]])
    ))
    assert.equal(result.ok, true, JSON.stringify(result))
    const index = JSON.parse(readFileSync(join(stage, 'publication', 'source-index.json'), 'utf8'))
    assert.deepEqual(index.sources[0].origins, [secondOrigin, capture.source.origin].sort((left, right) =>
      canonicalJson(left).localeCompare(canonicalJson(right))))
  } finally {
    rmSync(stage, { recursive: true, force: true })
  }
})

check('strict-empty initializer publishes only compatible index and catalog', () => {
  const stage = mkdtempSync(join(tmpdir(), 'observed-token-runner-'))
  try {
    const result = runPlan(stage, exactPlan(stage, [], []))
    assert.equal(result.ok, true, JSON.stringify(result))
    assert.deepEqual(result.artifacts.map((row) => row.role), ['observed-token-source-index', 'observed-token-catalog'])
    assert.equal(result.counts.activeSources, 0)
    assert.deepEqual(result.healthEvidence, [])
  } finally {
    rmSync(stage, { recursive: true, force: true })
  }
})

check('semantic no-op refresh updates only per-source health evidence', () => {
  const seed = mkdtempSync(join(tmpdir(), 'observed-token-seed-'))
  const stage = mkdtempSync(join(tmpdir(), 'observed-token-noop-'))
  try {
    const initialCapture = validObservedCapture()
    writeFileSync(join(seed, 'capture-000.json'), JSON.stringify(captureShard(initialCapture)))
    const initial = runPlan(seed, exactPlan(seed, undefined, [initialCapture]))
    assert.equal(initial.ok, true, JSON.stringify(initial))
    const sourceIndex = copyPublishedTokenDomain(seed, stage, initialCapture.source.sourceId)

    const refreshedCapture = structuredClone(initialCapture)
    refreshedCapture.captureOperationId = 'tokop_refresh_noop_0002'
    refreshedCapture.captureSequence = initialCapture.captureSequence + 1
    writeFileSync(join(stage, 'capture-000.json'), JSON.stringify(captureShard(refreshedCapture)))
    const plan = {
      ...exactPlan(stage, undefined, [refreshedCapture]),
      existingSourceShardFiles: ['existing-shard.json'],
      previousIndexFile: 'previous-index.json',
      previousCatalogFile: 'previous-catalog.json',
      revision: sourceIndex.revision + 1
    }
    const result = runPlan(stage, plan)
    const refreshedBatch = normalizeSourceCapture(
      refreshedCapture,
      Buffer.from(canonicalJson(refreshedCapture), 'utf8'),
      expected(refreshedCapture)
    )
    assert.equal(result.ok, true, JSON.stringify(result))
    assert.equal(result.noOp, true)
    assert.equal(result.sourceIndexHash, sourceIndex.semanticHash)
    assert.deepEqual(result.artifacts, [])
    assert.deepEqual(result.healthEvidence, [{
      sourceId: refreshedCapture.source.sourceId,
      captureOperationId: refreshedCapture.captureOperationId,
      captureSequence: refreshedCapture.captureSequence,
      captureEvidenceHash: refreshedBatch.captureEvidenceHash
    }])
    assert.equal(existsSync(join(stage, 'publication')), false)
  } finally {
    rmSync(seed, { recursive: true, force: true })
    rmSync(stage, { recursive: true, force: true })
  }
})

check('tampered capture record hashes fail before any publication output', () => {
  const stage = mkdtempSync(join(tmpdir(), 'observed-token-runner-'))
  try {
    const shard = captureShard(validObservedCapture())
    shard.captures[0].captureBytesHash = 'sha256:' + 'f'.repeat(64)
    writeFileSync(join(stage, 'capture-000.json'), JSON.stringify(shard))
    const result = runPlan(stage, exactPlan(stage, undefined, [shard.captures[0].capture]))
    assert.equal(result.ok, false)
    assert.equal(result.code, 'TOKEN_SOURCE_CAPTURE_INVALID')
    assert.equal(existsSync(join(stage, 'publication')), false)
  } finally {
    rmSync(stage, { recursive: true, force: true })
  }
})

check('capture bucket mismatch and path escape fail closed', () => {
  const stage = mkdtempSync(join(tmpdir(), 'observed-token-runner-'))
  try {
    const shard = captureShard(validObservedCapture())
    shard.bucket = (shard.bucket + 1) % 128
    writeFileSync(join(stage, 'capture-000.json'), JSON.stringify(shard))
    assert.equal(runPlan(stage, exactPlan(stage, undefined, [shard.captures[0].capture])).ok, false)
    assert.equal(runPlan(stage, exactPlan(stage, ['../outside.json'], [shard.captures[0].capture])).ok, false)
  } finally {
    rmSync(stage, { recursive: true, force: true })
  }
})

check('symlink capture intake is rejected by the single-link reader', () => {
  const stage = mkdtempSync(join(tmpdir(), 'observed-token-runner-'))
  try {
    writeFileSync(join(stage, 'real.json'), JSON.stringify(captureShard(validObservedCapture())))
    symlinkSync(join(stage, 'real.json'), join(stage, 'capture-000.json'))
    const capture = validObservedCapture()
    const result = runPlan(stage, exactPlan(stage, undefined, [capture]))
    assert.equal(result.ok, false)
  } finally {
    rmSync(stage, { recursive: true, force: true })
  }
})

check('non-exact runner plan is rejected', () => {
  const stage = mkdtempSync(join(tmpdir(), 'observed-token-runner-'))
  try {
    mkdirSync(join(stage, 'unused'))
    const result = runPlan(stage, { ...exactPlan(stage, [], []), forbiddenField: true })
    assert.deepEqual({ ok: result.ok, code: result.code }, { ok: false, code: 'RUN_PLAN_INVALID' })
  } finally {
    rmSync(stage, { recursive: true, force: true })
  }
})

check('task receipt ingestion rebases idempotently without a new publication', () => {
  const seed = mkdtempSync(join(tmpdir(), 'observed-token-seed-'))
  const stage = mkdtempSync(join(tmpdir(), 'observed-token-ingest-'))
  try {
    const capture = validObservedCapture()
    writeFileSync(join(seed, 'capture-000.json'), JSON.stringify(captureShard(capture)))
    assert.equal(runPlan(seed, exactPlan(seed, undefined, [capture])).ok, true)
    const shardName = readFileSync(join(seed, 'publication', 'source-index.json'), 'utf8')
    const sourceIndex = JSON.parse(shardName)
    const shardFile = `source-shard-${String(sourceBucket(capture.source.sourceId)).padStart(3, '0')}.json`
    copyFileSync(join(seed, 'publication', shardFile), join(stage, 'existing-shard.json'))
    copyFileSync(join(seed, 'publication', 'source-index.json'), join(stage, 'previous-index.json'))
    copyFileSync(join(seed, 'publication', 'observed-token-catalog.json'), join(stage, 'previous-catalog.json'))
    const captureBytes = Buffer.from(canonicalJson(capture), 'utf8')
    writeFileSync(join(stage, 'capture.json'), captureBytes)
    const batch = normalizeSourceCapture(capture, captureBytes)
    const result = runPlan(stage, {
      op: 'ingest-token-receipt',
      captureFiles: ['capture.json'],
      existingSourceShardFiles: ['existing-shard.json'],
      intentSources: [{
        sourceId: capture.source.sourceId,
        captureOperationId: capture.captureOperationId,
        captureSequence: capture.captureSequence,
        semanticHash: batch.batchSemanticHash
      }],
      outDir: 'publication',
      scope: { fileKeyFingerprint: FILE_FINGERPRINT, branchKey: 'none' },
      revision: sourceIndex.revision + 1,
      previousCatalogFile: 'previous-catalog.json',
      previousIndexFile: 'previous-index.json'
    })
    assert.equal(result.ok, true, JSON.stringify(result))
    assert.equal(result.noOp, true)
    assert.deepEqual(result.healthEvidence, [{
      sourceId: capture.source.sourceId,
      captureOperationId: capture.captureOperationId,
      captureSequence: capture.captureSequence,
      captureEvidenceHash: batch.captureEvidenceHash
    }])
    assert.deepEqual(result.acceptedSources, [capture.source.sourceId])
    assert.equal(existsSync(join(stage, 'publication')), false)
  } finally {
    rmSync(seed, { recursive: true, force: true })
    rmSync(stage, { recursive: true, force: true })
  }
})

check('newer task receipt sequence publishes a rebased catalog', () => {
  const seed = mkdtempSync(join(tmpdir(), 'observed-token-seed-'))
  const stage = mkdtempSync(join(tmpdir(), 'observed-token-ingest-'))
  try {
    const capture = validObservedCapture()
    writeFileSync(join(seed, 'capture-000.json'), JSON.stringify(captureShard(capture)))
    assert.equal(runPlan(seed, exactPlan(seed, undefined, [capture])).ok, true)
    const sourceIndex = JSON.parse(readFileSync(join(seed, 'publication', 'source-index.json'), 'utf8'))
    const shardFile = `source-shard-${String(sourceBucket(capture.source.sourceId)).padStart(3, '0')}.json`
    copyFileSync(join(seed, 'publication', shardFile), join(stage, 'existing-shard.json'))
    copyFileSync(join(seed, 'publication', 'source-index.json'), join(stage, 'previous-index.json'))
    copyFileSync(join(seed, 'publication', 'observed-token-catalog.json'), join(stage, 'previous-catalog.json'))
    const newer = validObservedCapture({
      captureOperationId: 'tokop_abcdef0123456789',
      captureSequence: 2,
      observations: [{ providerName: 'spacing/md', rawValue: 20, providerType: 'FLOAT' }]
    })
    const captureBytes = Buffer.from(JSON.stringify(newer, null, 2) + '\n')
    writeFileSync(join(stage, 'capture.json'), captureBytes)
    const batch = normalizeSourceCapture(newer, captureBytes)
    const result = runPlan(stage, {
      op: 'ingest-token-receipt',
      captureFiles: ['capture.json'],
      existingSourceShardFiles: ['existing-shard.json'],
      intentSources: [{
        sourceId: newer.source.sourceId,
        captureOperationId: newer.captureOperationId,
        captureSequence: newer.captureSequence,
        semanticHash: batch.batchSemanticHash
      }],
      outDir: 'publication',
      scope: { fileKeyFingerprint: FILE_FINGERPRINT, branchKey: 'none' },
      revision: sourceIndex.revision + 1,
      previousCatalogFile: 'previous-catalog.json',
      previousIndexFile: 'previous-index.json'
    })
    assert.equal(result.ok, true, JSON.stringify(result))
    assert.equal(result.noOp, false)
    assert.deepEqual(result.acceptedSources, [newer.source.sourceId])
    const publishedIndex = JSON.parse(readFileSync(join(stage, 'publication', 'source-index.json'), 'utf8'))
    assert.equal(publishedIndex.sources[0].acceptedBatch.captureSequence, 2)
  } finally {
    rmSync(seed, { recursive: true, force: true })
    rmSync(stage, { recursive: true, force: true })
  }
})

check('newer task receipt adds its exact origin without dropping existing active provenance', () => {
  const seed = mkdtempSync(join(tmpdir(), 'observed-token-seed-'))
  const stage = mkdtempSync(join(tmpdir(), 'observed-token-ingest-'))
  try {
    const capture = validObservedCapture()
    writeFileSync(join(seed, 'capture-000.json'), JSON.stringify(captureShard(capture)))
    assert.equal(runPlan(seed, exactPlan(seed, undefined, [capture])).ok, true)
    const sourceIndex = copyPublishedTokenDomain(seed, stage, capture.source.sourceId)
    const secondOrigin = {
      kind: 'task-screen',
      taskStem: 'TASK_43_Settings',
      screenKey: 'Settings',
      variantId: 'light-default-shared'
    }
    const newer = validObservedCapture({
      source: { ...capture.source, origin: secondOrigin },
      captureOperationId: 'tokop_secondorigin012345',
      captureSequence: 2
    })
    const bytes = Buffer.from(JSON.stringify(newer, null, 2) + '\n')
    writeFileSync(join(stage, 'capture.json'), bytes)
    const batch = normalizeSourceCapture(newer, bytes)
    const result = runPlan(stage, ingestionPlan(sourceIndex, {
      captureFiles: ['capture.json'],
      intentSources: [{
        sourceId: newer.source.sourceId,
        captureOperationId: newer.captureOperationId,
        captureSequence: newer.captureSequence,
        semanticHash: batch.batchSemanticHash
      }]
    }))
    assert.equal(result.ok, true, JSON.stringify(result))
    const index = JSON.parse(readFileSync(join(stage, 'publication', 'source-index.json'), 'utf8'))
    assert.equal(index.sources[0].origins.length, 2)
    assert.ok(index.sources[0].origins.some((origin) => canonicalJson(origin) === canonicalJson(capture.source.origin)))
    assert.ok(index.sources[0].origins.some((origin) => canonicalJson(origin) === canonicalJson(secondOrigin)))
  } finally {
    rmSync(seed, { recursive: true, force: true })
    rmSync(stage, { recursive: true, force: true })
  }
})

check('same source sequence with different evidence cannot replace accepted bytes', () => {
  const seed = mkdtempSync(join(tmpdir(), 'observed-token-seed-'))
  const stage = mkdtempSync(join(tmpdir(), 'observed-token-ingest-'))
  try {
    const capture = validObservedCapture()
    writeFileSync(join(seed, 'capture-000.json'), JSON.stringify(captureShard(capture)))
    assert.equal(runPlan(seed, exactPlan(seed, undefined, [capture])).ok, true)
    const sourceIndex = copyPublishedTokenDomain(seed, stage, capture.source.sourceId)
    const conflicting = validObservedCapture({
      captureOperationId: 'tokop_conflicting0123456',
      captureSequence: 1,
      observations: [{ providerName: 'spacing/md', rawValue: 99, providerType: 'FLOAT' }]
    })
    const bytes = Buffer.from(JSON.stringify(conflicting, null, 2) + '\n')
    writeFileSync(join(stage, 'capture.json'), bytes)
    const batch = normalizeSourceCapture(conflicting, bytes)
    const result = runPlan(stage, ingestionPlan(sourceIndex, {
      captureFiles: ['capture.json'],
      intentSources: [{
        sourceId: conflicting.source.sourceId,
        captureOperationId: conflicting.captureOperationId,
        captureSequence: conflicting.captureSequence,
        semanticHash: batch.batchSemanticHash
      }]
    }))
    assert.equal(result.ok, false)
    assert.equal(result.code, 'TOKEN_SOURCE_SEQUENCE_CONFLICT')
    assert.equal(existsSync(join(stage, 'publication')), false)
  } finally {
    rmSync(seed, { recursive: true, force: true })
    rmSync(stage, { recursive: true, force: true })
  }
})

check('retirement retains an immutable tombstone and removes its tokens from active aggregation', () => {
  const seed = mkdtempSync(join(tmpdir(), 'observed-token-seed-'))
  const stage = mkdtempSync(join(tmpdir(), 'observed-token-retire-'))
  try {
    const capture = validObservedCapture()
    writeFileSync(join(seed, 'capture-000.json'), JSON.stringify(captureShard(capture)))
    assert.equal(runPlan(seed, exactPlan(seed, undefined, [capture])).ok, true)
    const sourceIndex = copyPublishedTokenDomain(seed, stage, capture.source.sourceId)
    const result = runPlan(stage, ingestionPlan(sourceIndex, {
      retireSourceIds: [capture.source.sourceId]
    }))
    assert.equal(result.ok, true, JSON.stringify(result))
    assert.equal(result.noOp, false)
    const retiredIndex = JSON.parse(readFileSync(join(stage, 'publication', 'source-index.json'), 'utf8'))
    const retiredCatalog = JSON.parse(readFileSync(join(stage, 'publication', 'observed-token-catalog.json'), 'utf8'))
    const retainedShard = JSON.parse(readFileSync(join(stage, 'publication',
      `source-shard-${String(sourceBucket(capture.source.sourceId)).padStart(3, '0')}.json`), 'utf8'))
    assert.equal(retiredIndex.sources[0].lifecycle, 'retired')
    assert.deepEqual(retiredIndex.counts, { active: 0, retired: 1, shards: 1 })
    assert.equal(retiredCatalog.counts.activeSources, 0)
    assert.equal(retiredCatalog.counts.notObserved, capture.observations.length)
    assert.equal(retainedShard.sources[0].sourceId, capture.source.sourceId)
  } finally {
    rmSync(seed, { recursive: true, force: true })
    rmSync(stage, { recursive: true, force: true })
  }
})

check('all-source refresh carries retired tombstones without recapturing or reactivating them', () => {
  const seed = mkdtempSync(join(tmpdir(), 'observed-token-seed-'))
  const retiredStage = mkdtempSync(join(tmpdir(), 'observed-token-retire-'))
  const refreshStage = mkdtempSync(join(tmpdir(), 'observed-token-refresh-'))
  try {
    const capture = validObservedCapture()
    writeFileSync(join(seed, 'capture-000.json'), JSON.stringify(captureShard(capture)))
    assert.equal(runPlan(seed, exactPlan(seed, undefined, [capture])).ok, true)
    const activeIndex = copyPublishedTokenDomain(seed, retiredStage, capture.source.sourceId)
    assert.equal(runPlan(retiredStage, ingestionPlan(activeIndex, {
      retireSourceIds: [capture.source.sourceId]
    })).ok, true)
    const retiredIndex = copyPublishedTokenDomain(retiredStage, refreshStage, capture.source.sourceId)
    const result = runPlan(refreshStage, {
      ...exactPlan(refreshStage, [], []),
      revision: retiredIndex.revision + 1,
      previousCatalogFile: 'previous-catalog.json',
      previousIndexFile: 'previous-index.json',
      existingSourceShardFiles: ['existing-shard.json']
    })
    assert.equal(result.ok, true, JSON.stringify(result))
    assert.equal(result.noOp, true)
    assert.equal(result.sourceIndexHash, retiredIndex.semanticHash)
    assert.deepEqual(result.healthEvidence, [])
    assert.equal(existsSync(join(refreshStage, 'publication')), false)
    assert.equal(retiredIndex.sources.length, 1)
    assert.equal(retiredIndex.sources[0].lifecycle, 'retired')
    assert.equal(retiredIndex.sources[0].acceptedBatch.captureSequence, 1)
  } finally {
    rmSync(seed, { recursive: true, force: true })
    rmSync(retiredStage, { recursive: true, force: true })
    rmSync(refreshStage, { recursive: true, force: true })
  }
})

check('detaching the last exact origin retires instead of deleting the source', () => {
  const seed = mkdtempSync(join(tmpdir(), 'observed-token-seed-'))
  const stage = mkdtempSync(join(tmpdir(), 'observed-token-detach-'))
  try {
    const capture = validObservedCapture()
    writeFileSync(join(seed, 'capture-000.json'), JSON.stringify(captureShard(capture)))
    assert.equal(runPlan(seed, exactPlan(seed, undefined, [capture])).ok, true)
    const sourceIndex = copyPublishedTokenDomain(seed, stage, capture.source.sourceId)
    const result = runPlan(stage, ingestionPlan(sourceIndex, {
      detachOrigin: capture.source.origin
    }))
    assert.equal(result.ok, true, JSON.stringify(result))
    const detachedIndex = JSON.parse(readFileSync(join(stage, 'publication', 'source-index.json'), 'utf8'))
    assert.equal(detachedIndex.sources.length, 1)
    assert.equal(detachedIndex.sources[0].lifecycle, 'retired')
    assert.deepEqual(detachedIndex.sources[0].origins, [capture.source.origin])
  } finally {
    rmSync(seed, { recursive: true, force: true })
    rmSync(stage, { recursive: true, force: true })
  }
})

check('only a newer successful exact recapture reactivates a retired source', () => {
  const seed = mkdtempSync(join(tmpdir(), 'observed-token-seed-'))
  const retiredStage = mkdtempSync(join(tmpdir(), 'observed-token-retire-'))
  const recaptureStage = mkdtempSync(join(tmpdir(), 'observed-token-reactivate-'))
  try {
    const capture = validObservedCapture()
    writeFileSync(join(seed, 'capture-000.json'), JSON.stringify(captureShard(capture)))
    assert.equal(runPlan(seed, exactPlan(seed, undefined, [capture])).ok, true)
    const sourceIndex = copyPublishedTokenDomain(seed, retiredStage, capture.source.sourceId)
    assert.equal(runPlan(retiredStage, ingestionPlan(sourceIndex, {
      retireSourceIds: [capture.source.sourceId]
    })).ok, true)
    const retiredIndex = copyPublishedTokenDomain(retiredStage, recaptureStage, capture.source.sourceId)
    const recapture = validObservedCapture({
      captureOperationId: 'tokop_reactivate0123456789',
      captureSequence: 2
    })
    const bytes = Buffer.from(JSON.stringify(recapture, null, 2) + '\n')
    writeFileSync(join(recaptureStage, 'capture.json'), bytes)
    const batch = normalizeSourceCapture(recapture, bytes)
    const result = runPlan(recaptureStage, ingestionPlan(retiredIndex, {
      captureFiles: ['capture.json'],
      intentSources: [{
        sourceId: recapture.source.sourceId,
        captureOperationId: recapture.captureOperationId,
        captureSequence: recapture.captureSequence,
        semanticHash: batch.batchSemanticHash
      }]
    }))
    assert.equal(result.ok, true, JSON.stringify(result))
    const activeIndex = JSON.parse(readFileSync(join(recaptureStage, 'publication', 'source-index.json'), 'utf8'))
    assert.equal(activeIndex.sources[0].lifecycle, 'active')
    assert.equal(activeIndex.sources[0].acceptedBatch.captureSequence, 2)
  } finally {
    rmSync(seed, { recursive: true, force: true })
    rmSync(retiredStage, { recursive: true, force: true })
    rmSync(recaptureStage, { recursive: true, force: true })
  }
})

if (!process.exitCode) console.log(`observed token runner: ${passed} checks passed`)
