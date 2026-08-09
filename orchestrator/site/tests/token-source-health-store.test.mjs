#!/usr/bin/env node

import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { mkdtempSync, readFileSync, readdirSync, rmSync, unlinkSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { createRequire } from 'node:module'

const fixture = mkdtempSync(join(tmpdir(), 'token-source-health-store-'))
process.env.ORCHESTRATOR_PROJECT_ROOT = fixture
process.env.ORCHESTRATOR_CACHE_DIR = join(fixture, 'orchestrator', '.cache')

const require = createRequire(import.meta.url)
const identity = require('../../figma/runtime/token-identity.cjs')
const store = require('../server/token-source-health-store.js')

const fingerprint = 'sha256:' + '1'.repeat(64)
const sourceId = identity.sourceIdFor({
  fileKeyFingerprint: fingerprint,
  branchKey: 'branch:test',
  nodeId: '12:34',
  context: { theme: 'light', locale: 'default', platform: 'shared' }
})
const prospectiveSourceId = identity.sourceIdFor({
  fileKeyFingerprint: fingerprint,
  branchKey: 'branch:test',
  nodeId: '55:89',
  context: { theme: 'dark', locale: 'default', platform: 'shared' }
})
const sourceIndexHash = 'sha256:' + '2'.repeat(64)
let checks = 0

function check(name, fn) {
  fn()
  checks++
  console.log(`ok ${checks} - ${name}`)
}

try {
  check('reservation starts above accepted design sequence and is durable', () => {
    const reserved = store.reserveMany({
      sourceIndexHash,
      ownerId: 'fsj-' + '1'.repeat(32),
      sources: [{ sourceId, acceptedSequence: 7 }]
    })
    assert.equal(reserved.reservations[0].captureSequence, 8)
    const current = store.current()
    assert.equal(current.snapshot.shards[0].records[0].issuedSequenceHighWatermark, 8)
    assert.equal(current.snapshot.shards[0].records[0].latestAttempt.outcome, 'reserved')
  })

  check('failed reservation is never reused', () => {
    const current = store.current()
    const first = current.snapshot.shards[0].records[0].latestAttempt
    store.complete({
      sourceIndexHash,
      reservations: [{
        sourceId,
        captureOperationId: first.operationId,
        captureSequence: first.captureSequence
      }],
      outcome: 'failed',
      errorCode: 'TOKEN_CAPTURE_INTERRUPTED',
      retryable: true,
      jobId: 'health-fixture-1',
      action: 'health-recovery',
      startedAt: new Date().toISOString(),
      summaryOutcome: 'failed'
    })
    const second = store.reserveMany({
      sourceIndexHash,
      ownerId: 'fsj-' + '2'.repeat(32),
      sources: [{ sourceId, acceptedSequence: 7 }]
    })
    assert.equal(second.reservations[0].captureSequence, 9)
  })

  check('success is recorded separately from the high-watermark', () => {
    const current = store.current()
    const pending = current.snapshot.shards[0].records[0].latestAttempt
    store.complete({
      sourceIndexHash: 'sha256:' + '3'.repeat(64),
      reservations: [{
        sourceId,
        captureOperationId: pending.operationId,
        captureSequence: pending.captureSequence
      }],
      outcome: 'published',
      evidenceSources: [{
        sourceId,
        captureOperationId: pending.operationId,
        captureSequence: pending.captureSequence,
        captureEvidenceHash: 'sha256:' + '4'.repeat(64)
      }],
      jobId: 'health-fixture-2',
      action: 'refresh-known-token-sources',
      startedAt: new Date().toISOString(),
      summaryOutcome: 'published'
    })
    const record = store.current().snapshot.shards[0].records[0]
    assert.equal(record.latestSuccess.captureSequence, 9)
    assert.equal(record.latestSuccess.evidenceHash, 'sha256:' + '4'.repeat(64))
    assert.equal(record.issuedSequenceHighWatermark, 9)
  })

  check('pointer loss recovers the unique newest immutable snapshot', () => {
    const before = store.current().snapshot.index.semanticHash
    rmSync(store.POINTER_FILE)
    const recovered = store.current().snapshot.index.semanticHash
    assert.equal(recovered, before)
  })

  check('pointer loss never skips a corrupt immutable snapshot or initializes empty health', () => {
    const corruptBytes = Buffer.from('{corrupt-health')
    const corruptHash = createHash('sha256').update(corruptBytes).digest('hex')
    const corruptFile = join(store.SNAPSHOTS_DIR, `health-999-${corruptHash}.json`)
    writeFileSync(corruptFile, corruptBytes)
    unlinkSync(store.POINTER_FILE)
    assert.throws(() => store.current(), /TOKEN_SOURCE_HEALTH_RECOVERY_FAILED/)
    unlinkSync(corruptFile)
    assert.match(store.current().snapshot.index.semanticHash, /^sha256:[a-f0-9]{64}$/)
  })

  check('prospective component sources have unique durable operations before identity claim', () => {
    const ownerId = 'fsj-' + 'a'.repeat(32)
    const reserved = store.reserveMany({
      sourceIndexHash,
      sources: [],
      prospectiveCount: 2,
      ownerId
    })
    assert.equal(reserved.prospectiveReservations.length, 2)
    assert.notEqual(
      reserved.prospectiveReservations[0].captureOperationId,
      reserved.prospectiveReservations[1].captureOperationId
    )
    assert.equal(store.current().snapshot.index.prospectiveReservations.length, 2)
    const claimed = reserved.prospectiveReservations[0]
    store.complete({
      sourceIndexHash,
      reservations: [{
        sourceId: prospectiveSourceId,
        captureOperationId: claimed.captureOperationId,
        captureSequence: claimed.captureSequence
      }],
      prospectiveReservations: reserved.prospectiveReservations,
      outcome: 'published',
      evidenceSources: [{
        sourceId: prospectiveSourceId,
        captureOperationId: claimed.captureOperationId,
        captureSequence: claimed.captureSequence,
        captureEvidenceHash: 'sha256:' + '5'.repeat(64)
      }],
      jobId: ownerId,
      action: 'component-token-capture',
      startedAt: new Date().toISOString(),
      summaryOutcome: 'published'
    })
    const current = store.current().snapshot
    assert.equal(current.index.prospectiveReservations.length, 0)
    const record = current.shards.flatMap((shard) => shard.records)
      .find((row) => row.sourceId === prospectiveSourceId)
    assert.equal(record.latestSuccess.operationId, claimed.captureOperationId)
    assert.equal(record.issuedSequenceHighWatermark, 1)
  })

  check('prospective operation cannot be forged or claimed by two source identities', () => {
    const ownerId = 'fsj-' + 'b'.repeat(32)
    const reserved = store.reserveMany({
      sourceIndexHash,
      sources: [],
      prospectiveCount: 1,
      ownerId
    })
    const claim = reserved.prospectiveReservations[0]
    assert.throws(() => store.complete({
      sourceIndexHash,
      reservations: [],
      prospectiveReservations: [{ ...claim, ownerId: 'fsj-' + 'c'.repeat(32) }],
      outcome: 'failed',
      errorCode: 'TOKEN_CAPTURE_INTERRUPTED',
      retryable: true,
      jobId: ownerId,
      action: 'component-token-capture',
      startedAt: new Date().toISOString(),
      summaryOutcome: 'failed'
    }), /PROSPECTIVE_RESERVATION_MISMATCH/)
    assert.throws(() => store.complete({
      sourceIndexHash,
      reservations: [{
        sourceId,
        captureOperationId: claim.captureOperationId,
        captureSequence: 1
      }, {
        sourceId: prospectiveSourceId,
        captureOperationId: claim.captureOperationId,
        captureSequence: 1
      }],
      prospectiveReservations: reserved.prospectiveReservations,
      outcome: 'failed',
      errorCode: 'TOKEN_CAPTURE_INTERRUPTED',
      retryable: true,
      jobId: ownerId,
      action: 'component-token-capture',
      startedAt: new Date().toISOString(),
      summaryOutcome: 'failed'
    }), /PROSPECTIVE_RESERVATION_MISMATCH/)
    store.complete({
      sourceIndexHash,
      reservations: [],
      prospectiveReservations: reserved.prospectiveReservations,
      outcome: 'failed',
      errorCode: 'TOKEN_CAPTURE_INTERRUPTED',
      retryable: true,
      jobId: ownerId,
      action: 'component-token-capture',
      startedAt: new Date().toISOString(),
      summaryOutcome: 'failed'
    })
  })

  check('restart recovery releases unclaimed prospects and marks interrupted source gaps failed', () => {
    const ownerId = 'fsj-' + 'd'.repeat(32)
    const reserved = store.reserveMany({
      sourceIndexHash,
      ownerId,
      sources: [{ sourceId, acceptedSequence: 9 }],
      prospectiveCount: 2
    })
    const recovered = store.reconcileSyncReservations({
      terminalJobs: {
        [ownerId]: { result: 'interrupted', finishedAt: '2026-07-23T12:00:00.000Z' }
      },
      ownerScanComplete: true,
      sourceIndexHash,
      acceptedSources: []
    })
    assert.equal(recovered.changed, true)
    assert.equal(recovered.releasedProspects, 2)
    const current = store.current().snapshot
    assert.equal(current.index.prospectiveReservations.length, 0)
    const record = current.shards.flatMap((shard) => shard.records)
      .find((row) => row.sourceId === sourceId)
    assert.equal(record.latestAttempt.operationId, reserved.reservations[0].captureOperationId)
    assert.equal(record.latestAttempt.outcome, 'failed')
    assert.equal(record.latestFailure.code, 'TOKEN_CAPTURE_INTERRUPTED')
    assert.equal(record.issuedSequenceHighWatermark, reserved.reservations[0].captureSequence)
  })

  check('restart recovery binds a committed prospective operation to exact accepted identity', () => {
    const ownerId = 'fsj-' + 'e'.repeat(32)
    const recoveredSourceId = identity.sourceIdFor({
      fileKeyFingerprint: fingerprint,
      branchKey: 'branch:test',
      nodeId: '144:233',
      context: { theme: 'light', locale: 'uk', platform: 'shared' }
    })
    const reserved = store.reserveMany({
      sourceIndexHash,
      ownerId,
      sources: [],
      prospectiveCount: 1
    })
    const prospective = reserved.prospectiveReservations[0]
    const evidenceHash = 'sha256:' + '6'.repeat(64)
    const recovered = store.reconcileSyncReservations({
      terminalJobs: {
        [ownerId]: { result: 'success', finishedAt: '2026-07-23T12:01:00.000Z' }
      },
      ownerScanComplete: true,
      sourceIndexHash: 'sha256:' + '7'.repeat(64),
      acceptedSources: [{
        sourceId: recoveredSourceId,
        captureOperationId: prospective.captureOperationId,
        captureSequence: prospective.captureSequence,
        captureEvidenceHash: evidenceHash
      }]
    })
    assert.equal(recovered.changed, true)
    const current = store.current().snapshot
    assert.equal(current.index.sourceIndexRevision, 'sha256:' + '7'.repeat(64))
    assert.equal(current.index.prospectiveReservations.length, 0)
    const record = current.shards.flatMap((shard) => shard.records)
      .find((row) => row.sourceId === recoveredSourceId)
    assert.equal(record.latestSuccess.operationId, prospective.captureOperationId)
    assert.equal(record.latestSuccess.evidenceHash, evidenceHash)
  })

  check('complete restart scan settles a pre-history reservation owner as interrupted', () => {
    const ownerId = 'fsj-' + 'f'.repeat(32)
    const preHistorySourceId = identity.sourceIdFor({
      fileKeyFingerprint: fingerprint,
      branchKey: 'branch:test',
      nodeId: '1597:2584',
      context: { theme: 'dark', locale: 'default', platform: 'shared' }
    })
    const held = store.reserveMany({
      sourceIndexHash: 'sha256:' + '7'.repeat(64),
      ownerId,
      sources: [{ sourceId: preHistorySourceId, acceptedSequence: 0 }],
      prospectiveCount: 1
    })
    assert.throws(() => store.reconcileSyncReservations({
      terminalJobs: {},
      sourceIndexHash: 'sha256:' + '7'.repeat(64),
      acceptedSources: []
    }), /TOKEN_SOURCE_HEALTH_RECOVERY_INVALID/)
    const recovered = store.reconcileSyncReservations({
      terminalJobs: {},
      ownerScanComplete: true,
      sourceIndexHash: 'sha256:' + '7'.repeat(64),
      acceptedSources: []
    })
    assert.equal(recovered.releasedProspects, 1)
    assert.equal(recovered.recoveredSources, 1)
    assert.equal(store.current().snapshot.index.prospectiveReservations.length, 0)
    const record = store.current().snapshot.shards.flatMap((shard) => shard.records)
      .find((row) => row.sourceId === preHistorySourceId)
    assert.equal(record.latestAttempt.operationId, held.reservations[0].captureOperationId)
    assert.equal(record.latestAttempt.outcome, 'failed')
    assert.equal(record.latestFailure.code, 'TOKEN_CAPTURE_INTERRUPTED')
  })

  check('screen-plan owner scan fails orphan reservations without reusing their sequence', () => {
    const screenSourceId = identity.sourceIdFor({
      fileKeyFingerprint: fingerprint,
      branchKey: 'branch:test',
      nodeId: '233:377',
      context: { theme: 'dark', locale: 'uk', platform: 'shared' }
    })
    const ownerId = 'tokplan_' + '1'.repeat(32)
    const reserved = store.reserveMany({
      sourceIndexHash: 'sha256:' + '7'.repeat(64),
      ownerId,
      sources: [{ sourceId: screenSourceId, acceptedSequence: 0 }]
    })
    assert.deepEqual(store.reconcileScreenReservations({
      plans: [{
        planId: ownerId,
        sourceIndexHash: 'sha256:' + '7'.repeat(64),
        reservations: reserved.reservations
      }]
    }), { changed: false, recoveredSources: 0 })
    const recovered = store.reconcileScreenReservations({ plans: [] })
    assert.equal(recovered.changed, true)
    assert.equal(recovered.recoveredSources, 1)
    const record = store.current().snapshot.shards.flatMap((shard) => shard.records)
      .find((row) => row.sourceId === screenSourceId)
    assert.equal(record.latestAttempt.outcome, 'failed')
    assert.equal(record.latestFailure.code, 'TOKEN_CAPTURE_INTERRUPTED')
    const next = store.reserveMany({
      sourceIndexHash: 'sha256:' + '7'.repeat(64),
      ownerId: 'tokplan_' + '2'.repeat(32),
      sources: [{ sourceId: screenSourceId, acceptedSequence: 0 }]
    })
    assert.equal(next.reservations[0].captureSequence,
      reserved.reservations[0].captureSequence + 1)
    store.complete({
      sourceIndexHash: 'sha256:' + '7'.repeat(64),
      reservations: next.reservations,
      outcome: 'failed',
      errorCode: 'TOKEN_CAPTURE_INTERRUPTED',
      retryable: true,
      jobId: 'tokplan_' + '2'.repeat(32),
      action: 'task-ingestion',
      startedAt: new Date().toISOString(),
      summaryOutcome: 'failed'
    })
  })

  check('exact recovery takes the verified max across owner evidence after pointer corruption', () => {
    const recoveredSourceId = identity.sourceIdFor({
      fileKeyFingerprint: fingerprint,
      branchKey: 'branch:test',
      nodeId: '377:610',
      context: { theme: 'light', locale: 'uk', platform: 'android' }
    })
    const operationId = 'tokop_' + '9'.repeat(32)
    writeFileSync(store.POINTER_FILE, '{corrupt-pointer')
    const recovered = store.recoverExact({
      sourceIndexHash: 'sha256:' + '8'.repeat(64),
      acceptedSources: [],
      reservations: [{
        sourceId: recoveredSourceId,
        captureOperationId: operationId,
        captureSequence: 5,
        at: '2026-07-23T12:02:00.000Z'
      }],
      prospectiveReservations: []
    })
    const record = recovered.shards.flatMap((shard) => shard.records)
      .find((row) => row.sourceId === recoveredSourceId)
    assert.equal(record.issuedSequenceHighWatermark, 5)
    assert.equal(record.latestAttempt.operationId, operationId)
    assert.equal(record.latestAttempt.outcome, 'superseded')
    const next = store.reserveMany({
      sourceIndexHash: 'sha256:' + '8'.repeat(64),
      ownerId: 'fsj-' + '9'.repeat(32),
      sources: [{ sourceId: recoveredSourceId, acceptedSequence: 0 }]
    })
    assert.equal(next.reservations[0].captureSequence, 6)
  })

  check('exact recovery requires a committed success witness before restoring freshness', () => {
    const stageSourceId = identity.sourceIdFor({
      fileKeyFingerprint: fingerprint,
      branchKey: 'branch:test',
      nodeId: '610:987',
      context: { theme: 'dark', locale: 'uk', platform: 'android' }
    })
    const receiptSourceId = identity.sourceIdFor({
      fileKeyFingerprint: fingerprint,
      branchKey: 'branch:test',
      nodeId: '987:1597',
      context: { theme: 'light', locale: 'uk', platform: 'android' }
    })
    const stageOperation = 'tokop_' + 'a'.repeat(32)
    const receiptOperation = 'tokop_' + 'b'.repeat(32)
    const stageEvidence = 'sha256:' + 'a'.repeat(64)
    const receiptEvidence = 'sha256:' + 'b'.repeat(64)
    writeFileSync(store.POINTER_FILE, '{corrupt-pointer')
    const recovered = store.recoverExact({
      sourceIndexHash: 'sha256:' + '9'.repeat(64),
      acceptedSources: [{
        sourceId: stageSourceId,
        captureOperationId: stageOperation,
        captureSequence: 3,
        captureEvidenceHash: stageEvidence
      }, {
        sourceId: receiptSourceId,
        captureOperationId: receiptOperation,
        captureSequence: 4,
        captureEvidenceHash: receiptEvidence
      }],
      reservations: [{
        sourceId: stageSourceId,
        captureOperationId: stageOperation,
        captureSequence: 3,
        at: '2026-07-23T12:03:00.000Z'
      }, {
        sourceId: receiptSourceId,
        captureOperationId: receiptOperation,
        captureSequence: 4,
        at: '2026-07-23T12:04:00.000Z',
        successAt: '2026-07-23T12:04:00.000Z',
        captureEvidenceHash: receiptEvidence
      }],
      prospectiveReservations: []
    })
    const rows = recovered.shards.flatMap((shard) => shard.records)
    assert.equal(rows.find((row) => row.sourceId === stageSourceId).latestAttempt.outcome,
      'superseded')
    const receiptRecord = rows.find((row) => row.sourceId === receiptSourceId)
    assert.equal(receiptRecord.latestAttempt.outcome, 'published')
    assert.equal(receiptRecord.latestSuccess.evidenceHash, receiptEvidence)
  })

  check('exact recovery rejects duplicate accepted identities and malformed owner timestamps', () => {
    const operationId = 'tokop_' + 'c'.repeat(32)
    const accepted = {
      sourceId,
      captureOperationId: operationId,
      captureSequence: 20,
      captureEvidenceHash: 'sha256:' + 'c'.repeat(64)
    }
    assert.throws(() => store.recoverExact({
      sourceIndexHash,
      acceptedSources: [accepted, accepted],
      reservations: [],
      prospectiveReservations: []
    }), /TOKEN_SOURCE_HEALTH_RECOVERY_AMBIGUOUS/)
    assert.throws(() => store.recoverExact({
      sourceIndexHash,
      acceptedSources: [],
      reservations: [{
        sourceId,
        captureOperationId: operationId,
        captureSequence: 20,
        at: 'not-an-instant'
      }],
      prospectiveReservations: []
    }), /TOKEN_SOURCE_HEALTH_RECOVERY_INVALID/)
  })

  check('exact recovery rejects two valid immutable snapshots at the same top revision', () => {
    const pointer = JSON.parse(readFileSync(store.POINTER_FILE, 'utf8'))
    const original = JSON.parse(readFileSync(join(store.SNAPSHOTS_DIR, pointer.snapshotFile), 'utf8'))
    original.createdAt = original.createdAt === '2026-07-23T12:05:00.000Z'
      ? '2026-07-23T12:05:01.000Z' : '2026-07-23T12:05:00.000Z'
    const bytes = Buffer.from(JSON.stringify(original, null, 2) + '\n')
    const digest = createHash('sha256').update(bytes).digest('hex')
    const duplicate = join(store.SNAPSHOTS_DIR,
      `health-${original.index.healthRevision}-${digest}.json`)
    writeFileSync(duplicate, bytes)
    try {
      assert.throws(() => store.recoverExact({
        sourceIndexHash,
        acceptedSources: [],
        reservations: [],
        prospectiveReservations: []
      }), /TOKEN_SOURCE_HEALTH_RECOVERY_AMBIGUOUS/)
    } finally {
      unlinkSync(duplicate)
    }
  })

  check('snapshot retention stays bounded', () => {
    for (let index = 0; index < 8; index++) {
      const reserved = store.reserveMany({
        sourceIndexHash,
        ownerId: 'fsj-' + String(index + 3).padStart(32, '0'),
        sources: [{ sourceId, acceptedSequence: 7 }]
      })
      store.complete({
        sourceIndexHash,
        reservations: reserved.reservations,
        outcome: 'failed',
        errorCode: 'TOKEN_CAPTURE_FAILED',
        retryable: true,
        jobId: `health-fixture-${index + 10}`,
        action: 'health-recovery',
        startedAt: new Date().toISOString(),
        summaryOutcome: 'failed'
      })
    }
    const names = readdirSync(store.SNAPSHOTS_DIR).filter((name) => /^health-/.test(name))
    assert.ok(names.length <= 10)
  })

  console.log(`token source health store: ${checks} checks passed`)
} finally {
  rmSync(fixture, { recursive: true, force: true })
}
