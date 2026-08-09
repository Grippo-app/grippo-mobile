import assert from 'node:assert/strict'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { createSchemaRegistry } from '../runtime/schema-registry.mjs'
import {
  materializeSourceHealth,
  reserveSourceSequence,
  sourceFreshness,
  sourceHealthSemanticError,
  sourceHealthSnapshotHash
} from '../tokens/source-health.mjs'
import { sourceIdentity } from './observed-token-fixtures.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const schemas = createSchemaRegistry(join(HERE, '..', 'schemas'))
const SOURCE_INDEX_HASH = 'sha256:' + 'c'.repeat(64)
const AT = '2026-07-23T10:00:00.000Z'
const CHECKED_AT = Date.parse('2026-07-23T10:00:03.000Z')
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
function validate(snapshot) {
  assert.equal(schemas.validate('token-source-health-index')(snapshot.index), true)
  for (const shard of snapshot.shards) assert.equal(schemas.validate('token-source-health-shard')(shard), true)
  assert.equal(sourceHealthSemanticError({ index: snapshot.index, shards: snapshot.shards }), null)
}

check('fresh source sequence is durably represented by a reservation attempt', () => {
  const sourceId = sourceIdentity().sourceId
  const initial = materializeSourceHealth({
    sourceIndexHash: SOURCE_INDEX_HASH,
    healthRevision: 0
  })
  const reserved = reserveSourceSequence({
    snapshot: initial,
    sourceId,
    operationId: 'tokop_health_reserve_0001',
    ownerId: 'fsj-' + '1'.repeat(32),
    at: AT
  })
  assert.equal(reserved.reservation.captureSequence, 1)
  assert.equal(reserved.shards[0].records[0].issuedSequenceHighWatermark, 1)
  validate(reserved)
})

check('sequence reservations are monotonic and abandoned numbers are not reused', () => {
  const sourceId = sourceIdentity().sourceId
  const initial = materializeSourceHealth({ sourceIndexHash: SOURCE_INDEX_HASH, healthRevision: 0 })
  const first = reserveSourceSequence({
    snapshot: initial,
    sourceId,
    operationId: 'tokop_health_reserve_0001',
    ownerId: 'fsj-' + '1'.repeat(32),
    at: AT
  })
  const second = reserveSourceSequence({
    snapshot: first,
    sourceId,
    operationId: 'tokop_health_reserve_0002',
    ownerId: 'fsj-' + '2'.repeat(32),
    at: '2026-07-23T10:00:01.000Z'
  })
  assert.equal(second.reservation.captureSequence, 2)
  validate(second)
})

check('successful no-op updates evidence without changing design authority', () => {
  const sourceId = sourceIdentity().sourceId
  const initial = materializeSourceHealth({ sourceIndexHash: SOURCE_INDEX_HASH, healthRevision: 0 })
  const snapshot = materializeSourceHealth({
    previous: initial,
    sourceIndexHash: SOURCE_INDEX_HASH,
    healthRevision: 1,
    updates: [{
      sourceId,
      latestAttempt: {
        operationId: 'tokop_health_noop_0001',
        captureSequence: 1,
        at: AT,
        outcome: 'no-op',
        evidenceHash: 'sha256:' + 'd'.repeat(64)
      }
    }],
    jobSummary: {
      jobId: 'token-job-0001',
      action: 'refresh-known-token-sources',
      startedAt: AT,
      finishedAt: '2026-07-23T10:00:01.000Z',
      outcome: 'no-op',
      sourceCount: 1
    }
  })
  validate(snapshot)
  assert.equal(snapshot.index.sourceIndexRevision, SOURCE_INDEX_HASH)
  assert.equal(snapshot.index.jobSummaries[0].outcome, 'no-op')
  const sourceIndex = {
    semanticHash: SOURCE_INDEX_HASH,
    sources: [{
      ...sourceIdentity(),
      lifecycle: 'active',
      acceptedBatch: {
        captureSequence: 1
      }
    }]
  }
  assert.deepEqual(sourceFreshness(snapshot, sourceIndex, CHECKED_AT),
    { state: 'current', reason: 'source-health-current' })
})

check('a newer successful no-op keeps an older accepted batch fresh', () => {
  const sourceId = sourceIdentity().sourceId
  const accepted = materializeSourceHealth({
    sourceIndexHash: SOURCE_INDEX_HASH,
    healthRevision: 1,
    updates: [{
      sourceId,
      latestAttempt: {
        operationId: 'tokop_health_published_0001',
        captureSequence: 1,
        at: AT,
        outcome: 'published',
        evidenceHash: 'sha256:' + 'd'.repeat(64)
      }
    }]
  })
  const noOp = materializeSourceHealth({
    previous: accepted,
    sourceIndexHash: SOURCE_INDEX_HASH,
    healthRevision: 2,
    updates: [{
      sourceId,
      latestAttempt: {
        operationId: 'tokop_health_noop_0002',
        captureSequence: 2,
        at: '2026-07-23T10:00:02.000Z',
        outcome: 'no-op',
        evidenceHash: 'sha256:' + 'e'.repeat(64)
      }
    }]
  })
  const sourceIndex = {
    semanticHash: SOURCE_INDEX_HASH,
    sources: [{
      ...sourceIdentity(),
      lifecycle: 'active',
      acceptedBatch: {
        captureSequence: 1
      }
    }]
  }
  validate(noOp)
  assert.deepEqual(sourceFreshness(noOp, sourceIndex, CHECKED_AT),
    { state: 'current', reason: 'source-health-current' })
})

check('failed attempt preserves the latest accepted success', () => {
  const sourceId = sourceIdentity().sourceId
  const success = materializeSourceHealth({
    sourceIndexHash: SOURCE_INDEX_HASH,
    healthRevision: 1,
    updates: [{
      sourceId,
      latestAttempt: {
        operationId: 'tokop_health_good_0001',
        captureSequence: 1,
        at: AT,
        outcome: 'published',
        evidenceHash: 'sha256:' + 'e'.repeat(64)
      }
    }]
  })
  const failure = materializeSourceHealth({
    previous: success,
    sourceIndexHash: SOURCE_INDEX_HASH,
    healthRevision: 2,
    updates: [{
      sourceId,
      latestAttempt: {
        operationId: 'tokop_health_fail_0002',
        captureSequence: 2,
        at: '2026-07-23T10:00:02.000Z',
        outcome: 'failed'
      },
      failure: {
        operationId: 'tokop_health_fail_0002',
        captureSequence: 2,
        at: '2026-07-23T10:00:02.000Z',
        code: 'TOKEN_SOURCE_CAPTURE_INCOMPLETE',
        retryable: true
      }
    }]
  })
  validate(failure)
  const record = failure.shards[0].records[0]
  assert.equal(record.latestSuccess.captureSequence, 1)
  assert.equal(record.latestFailure.captureSequence, 2)
  const sourceIndex = {
    semanticHash: SOURCE_INDEX_HASH,
    sources: [{
      ...sourceIdentity(),
      lifecycle: 'active',
      acceptedBatch: {
        captureSequence: 1
      }
    }]
  }
  assert.deepEqual(sourceFreshness(success, sourceIndex, CHECKED_AT),
    { state: 'current', reason: 'source-health-current' })
  assert.deepEqual(sourceFreshness(failure, sourceIndex, CHECKED_AT),
    { state: 'stale', reason: 'source-refresh-required' })
  assert.deepEqual(sourceFreshness(success, sourceIndex, CHECKED_AT + 24 * 60 * 60 * 1000 + 1),
    { state: 'stale', reason: 'source-refresh-required' })
  assert.deepEqual(sourceFreshness(failure,
    { ...sourceIndex, semanticHash: 'sha256:' + 'f'.repeat(64) }, CHECKED_AT),
    { state: 'unknown', reason: 'source-health-unavailable' })
})

check('superseded sequence cannot overwrite a higher high-watermark', () => {
  const sourceId = sourceIdentity().sourceId
  const snapshot = materializeSourceHealth({
    sourceIndexHash: SOURCE_INDEX_HASH,
    healthRevision: 1,
    updates: [{
      sourceId,
      issuedSequenceHighWatermark: 3,
      latestAttempt: {
        operationId: 'tokop_health_reserved_3',
        captureSequence: 3,
        at: AT,
        outcome: 'reserved',
        ownerId: 'fsj-' + '3'.repeat(32)
      }
    }]
  })
  assert.throws(() => materializeSourceHealth({
    previous: snapshot,
    sourceIndexHash: SOURCE_INDEX_HASH,
    healthRevision: 2,
    updates: [{
      sourceId,
      latestAttempt: {
        operationId: 'tokop_health_old_00002',
        captureSequence: 2,
        at: AT,
        outcome: 'failed'
      },
      failure: {
        operationId: 'tokop_health_old_00002',
        captureSequence: 2,
        at: AT,
        code: 'TOKEN_SOURCE_CAPTURE_INCOMPLETE',
        retryable: true
      }
    }]
  }), /SUPERSEDED/)
})

check('snapshot identity is deterministic', () => {
  const snapshot = materializeSourceHealth({ sourceIndexHash: SOURCE_INDEX_HASH, healthRevision: 0 })
  assert.equal(sourceHealthSnapshotHash(snapshot), sourceHealthSnapshotHash(structuredClone(snapshot)))
})

if (!process.exitCode) console.log(`token source health: ${passed} checks passed`)
