#!/usr/bin/env node

import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { hostname, tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import crypto from 'node:crypto'

const root = mkdtempSync(join(tmpdir(), 'runtime-owner-self-admission-'))
const cache = join(root, 'orchestrator', '.cache', 'tasks')
const creations = join(cache, 'creations')
const edits = join(cache, 'edits')
const finalizations = join(cache, 'finalizations')
const writers = join(finalizations, '.writers')
for (const dir of [creations, edits, writers]) mkdirSync(dir, { recursive: true })
process.env.ORCHESTRATOR_PROJECT_ROOT = root
process.env.ORCHESTRATOR_TASK_CREATIONS_DIR = creations
process.env.ORCHESTRATOR_TASK_EDITS_DIR = edits
process.env.ORCHESTRATOR_FINALIZATIONS_DIR = finalizations
process.env.ORCHESTRATOR_WRITER_LEASES_DIR = writers

const require = createRequire(import.meta.url)
const creationMarkers = require('../server/creation-markers.js')
const editMarkers = require('../server/edit-markers.js')
const runtimeIntegrity = require('../server/runtime-integrity.js')
const writerInspector = require('../server/writer-lease-inspector.js')
const writerLeases = require('../../tasks/writer-leases.cjs')
const creationContract = require('../../tasks/creation-marker-contract.cjs')
const taskSource = require('../../tasks/task-source-contract.cjs')
const editContract = require('../../tasks/edit-marker-contract.cjs')
const H = (bytes) => 'sha256:' + crypto.createHash('sha256').update(bytes).digest('hex')
const at = '2026-07-13T00:00:00.000Z'
let checks = 0
function check(name, fn) { fn(); checks++; console.log(`ok ${checks} - ${name}`) }
function sorted(value) {
  if (Array.isArray(value)) return value.map(sorted)
  if (value && typeof value === 'object') return Object.fromEntries(Object.keys(value).sort().map((key) => [key, sorted(value[key])]))
  return value
}
function creationMarker(key, transaction) {
  const intent = {
    version: 1, title: 'Owner proof', body: '', originStem: null, dedupKey: null, dedupReport: null,
    source: taskSource.manualForIntent(key, 'manual', 'fixture:' + key)
  }
  return {
    version: 2, transactionId: transaction, keyHash: H(Buffer.from(key)), payloadHash: creationContract.digest(intent), intent,
    status: 'incomplete', phase: 'claimed', effect: null, number: null, slug: null, stem: null, sourceHash: null, column: null,
    createdAt: at, updatedAt: at, revision: 1, lastError: null, targetProof: null
  }
}
function writeCreation(record) {
  const file = join(creations, record.keyHash.slice(7) + '.json')
  writeFileSync(file, JSON.stringify(record) + '\n')
  return file
}
function editMarker(stem, transaction, title = 'Owner proof') {
  const body = Buffer.from(`# TASK ${stem.match(/^TASK_(\d+)_/)[1]} — ${title}\n\n## Goal\n\nExercise exact ownership.\n`, 'utf8')
  return {
    version: 1, transactionId: transaction, stem, expectedSourceHash: H(Buffer.from('old-' + stem)), requestedSourceHash: H(body),
    recoveryMarkdownBase64: body.toString('base64'), status: 'incomplete', phase: 'claimed', effect: null, sourceHash: null,
    createdAt: at, updatedAt: at, revision: 1, lastError: null
  }
}
function writeEdit(record) {
  const file = join(edits, record.stem + '.json')
  writeFileSync(file, JSON.stringify(sorted(record), null, 2) + '\n')
  assert.equal(editContract.validateRecord(record, record.stem), null)
  return file
}
function contextFor(record, file, lease, publicationKey, kind) {
  const common = {
    version: 1, transactionId: record.transactionId, revision: record.revision, contentHash: H(readFileSync(file)),
    pid: process.pid, processStartId: writerLeases.captureProcessStartId(process.pid),
    authorityLeaseId: lease.leaseId, mode: 'owned-lease', publicationKey
  }
  return kind === 'creation'
    ? { ...common, keyHash: record.keyHash, publicationGuardLeaseId: null }
    : { ...common, stem: record.stem }
}
function hasFinding(result, code) { return result.findings.some((row) => row.code === code) }
function acquire(key, stem = null) {
  return writerLeases.acquire(writers, {
    rootDir: root, kind: 'workspace-session', key, stem, ownerPid: process.pid, childPid: process.pid
  })
}

let handle = null
try {
  check('runtime status transport preserves only a canonical lock-generation digest', () => {
    const generation = 'sha256:' + '7'.repeat(64)
    const safe = runtimeIntegrity._safeStatus({
      owner: 'finalizations', kind: 'marker', state: 'running', stem: 'TASK_70_finalize',
      lockGenerationHash: generation,
    }, 'finalizations')
    assert.equal(safe.lockGenerationHash, generation)
    const invalid = runtimeIntegrity._safeStatus({
      owner: 'finalizations', kind: 'marker', state: 'running', stem: 'TASK_70_finalize',
      lockGenerationHash: 'not-a-generation-hash',
    }, 'finalizations')
    assert.equal(Object.prototype.hasOwnProperty.call(invalid, 'lockGenerationHash'), false)
  })

  const first = creationMarker('owner-proof-one', '1'.repeat(32))
  const firstFile = writeCreation(first)
  handle = acquire('task:create-backlog')
  const createContext = contextFor(first, firstFile, handle, 'task:create-backlog', 'creation')

  check('composite snapshot hash commits active authority and normalized runtime verdicts', () => {
    const saved = process.env.ORCHESTRATOR_ACTIVE_CREATION_CONTEXT
    try {
      delete process.env.ORCHESTRATOR_ACTIVE_CREATION_CONTEXT
      const external = runtimeIntegrity.scanIntegrity({ stem: null, roots: runtimeIntegrity.loadedRoots() })
      process.env.ORCHESTRATOR_ACTIVE_CREATION_CONTEXT = JSON.stringify(createContext)
      const admitted = runtimeIntegrity.scanIntegrity({ stem: null, roots: runtimeIntegrity.loadedRoots() })
      const verdict = (value) => value.snapshotInputs.find((row) => row.owner === 'composite' && row.kind === 'verdict')
      assert.ok(verdict(external))
      assert.ok(verdict(admitted))
      assert.notEqual(verdict(external).hash, verdict(admitted).hash,
        'the same owner bytes under a different exact authority context must produce a different snapshot fence')
      assert.equal(JSON.stringify(verdict(admitted)).includes(createContext.transactionId), false,
        'the synthetic input exposes only a digest, never capability-bearing context')
      assert.equal(admitted.stats.snapshotInputs, admitted.snapshotInputs.length)
    } finally {
      if (saved === undefined) delete process.env.ORCHESTRATOR_ACTIVE_CREATION_CONTEXT
      else process.env.ORCHESTRATOR_ACTIVE_CREATION_CONTEXT = saved
    }
  })

  check('one exact creation generation with one exact live writer is status-only', () => {
    const result = creationMarkers.scanIntegrity({ creationContext: createContext, writerLeaseInspection: writerInspector.inspect() })
    assert.equal(result.statuses.find((row) => row.kind === 'marker').state, 'active')
    assert.equal(hasFinding(result, 'CREATION_INCOMPLETE'), false)
  })

  check('creation marker replacement after context capture remains recovery-red', () => {
    const replaced = { ...first, revision: 2, updatedAt: '2026-07-13T00:00:01.000Z' }
    writeCreation(replaced)
    const result = creationMarkers.scanIntegrity({ creationContext: createContext, writerLeaseInspection: writerInspector.inspect() })
    assert.equal(hasFinding(result, 'CREATION_INCOMPLETE'), true)
    writeCreation(first)
  })

  check('a second live writer and a wrong publication key cannot forge creation ownership', () => {
    const foreign = writerLeases.acquire(writers, {
      rootDir: root, kind: 'task-session', key: 'task:TASK_9_parent', stem: 'TASK_9_parent',
      sessionId: writerLeases.createSessionId(), ownerPid: process.pid, childPid: process.pid
    })
    try {
      assert.equal(hasFinding(creationMarkers.scanIntegrity({ creationContext: createContext, writerLeaseInspection: writerInspector.inspect() }), 'CREATION_INCOMPLETE'), true)
    } finally { writerLeases.release(foreign) }
    const wrong = { ...createContext, publicationKey: 'task:recover-backlog-creations' }
    assert.equal(hasFinding(creationMarkers.scanIntegrity({ creationContext: wrong, writerLeaseInspection: writerInspector.inspect() }), 'CREATION_INCOMPLETE'), true)
  })

  check('a valid CAS recovery directory prevents creation self-admission', () => {
    const cas = join(creations, '.durable-cas-' + 'a'.repeat(16) + '-' + 'b'.repeat(16) + '-' + 'c'.repeat(16))
    mkdirSync(cas)
    try {
      const result = creationMarkers.scanIntegrity({ creationContext: createContext, writerLeaseInspection: writerInspector.inspect() })
      assert.equal(hasFinding(result, 'CREATION_MARKER_CAS_RECOVERY_REQUIRED'), true)
      assert.equal(hasFinding(result, 'CREATION_INCOMPLETE'), true)
    } finally { rmSync(cas, { recursive: true, force: true }) }
  })

  writerLeases.release(handle); handle = null
  const second = creationMarker('owner-proof-two', '2'.repeat(32))
  writeCreation(second)
  handle = acquire('task:recover-backlog-creations')
  const recoverContext = contextFor(first, firstFile, handle, 'task:recover-backlog-creations', 'creation')
  check('one exact recover-all writer owns every valid creation marker in the serialized set', () => {
    const result = creationMarkers.scanIntegrity({ creationContext: recoverContext, writerLeaseInspection: writerInspector.inspect() })
    assert.equal(result.statuses.filter((row) => row.kind === 'marker' && row.state === 'active').length, 2)
    assert.equal(hasFinding(result, 'CREATION_INCOMPLETE'), false)
  })

  writerLeases.release(handle); handle = null
  const deadId = writerLeases.createLeaseId()
  const deadStart = 'psid-v1:' + process.platform + ':' + '0'.repeat(64)
  const deadRow = {
    version: 1, leaseId: deadId, token: 'a'.repeat(48), kind: 'workspace-session', stem: null, sessionId: null,
    key: 'task:create-backlog', owner: { pid: 2147483000, hostname: hostname(), startedAt: at, processStartId: deadStart },
    childPid: 2147483000, childProcessStartId: deadStart, delegationHash: null, unverified: false, unverifiedReason: null,
    createdAt: at, updatedAt: at, expiresAt: null
  }
  writeFileSync(join(writers, deadId + '.json'), JSON.stringify(deadRow, null, 2) + '\n')
  check('a dead exact-shape writer generation never proves creation ownership', () => {
    const inspection = writerInspector.inspect()
    assert.equal(inspection.stale.length, 1)
    const deadContext = { ...createContext, pid: deadRow.childPid, processStartId: deadStart, authorityLeaseId: deadId }
    assert.equal(hasFinding(creationMarkers.scanIntegrity({ creationContext: deadContext, writerLeaseInspection: inspection }), 'CREATION_INCOMPLETE'), true)
  })
  rmSync(join(writers, deadId + '.json'))

  // Edit uses the same anchored lease proof, but binds the row and context to
  // one exact stem during normal publication.
  const editOne = editMarker('TASK_71_owner_proof', '3'.repeat(32))
  const editOneFile = writeEdit(editOne)
  handle = acquire('task:edit-backlog:' + editOne.stem, editOne.stem)
  const editContext = contextFor(editOne, editOneFile, handle, 'task:edit-backlog:' + editOne.stem, 'edit')
  check('one exact edit generation with its exact live per-stem writer is status-only', () => {
    const result = editMarkers.scanIntegrity({ stem: editOne.stem, editContext, writerLeaseInspection: writerInspector.inspect() })
    assert.equal(result.statuses.find((row) => row.kind === 'marker').state, 'active')
    assert.equal(hasFinding(result, 'EDIT_INCOMPLETE'), false)
  })

  check('edit replacement and CAS anomaly both stay recovery-red', () => {
    writeEdit({ ...editOne, revision: 2, updatedAt: '2026-07-13T00:00:01.000Z' })
    assert.equal(hasFinding(editMarkers.scanIntegrity({ stem: editOne.stem, editContext, writerLeaseInspection: writerInspector.inspect() }), 'EDIT_INCOMPLETE'), true)
    writeEdit(editOne)
    const cas = join(edits, '.durable-cas-' + 'd'.repeat(16) + '-' + 'e'.repeat(16) + '-' + 'f'.repeat(16))
    mkdirSync(cas)
    try {
      const result = editMarkers.scanIntegrity({ stem: editOne.stem, editContext, writerLeaseInspection: writerInspector.inspect() })
      assert.equal(hasFinding(result, 'EDIT_MARKER_CAS_RECOVERY_REQUIRED'), true)
      assert.equal(hasFinding(result, 'EDIT_INCOMPLETE'), true)
    } finally { rmSync(cas, { recursive: true, force: true }) }
  })

  writerLeases.release(handle); handle = null
  const editTwo = editMarker('TASK_72_owner_proof', '4'.repeat(32))
  writeEdit(editTwo)
  handle = acquire('task:recover-backlog-edits')
  const editRecover = contextFor(editOne, editOneFile, handle, 'task:recover-backlog-edits', 'edit')
  check('one exact recover-all writer owns every valid edit marker in the serialized set', () => {
    const result = editMarkers.scanIntegrity({ stem: null, editContext: editRecover, writerLeaseInspection: writerInspector.inspect() })
    assert.equal(result.statuses.filter((row) => row.kind === 'marker' && row.state === 'active').length, 2)
    assert.equal(hasFinding(result, 'EDIT_INCOMPLETE'), false)
  })

  writerLeases.release(handle); handle = null
  handle = acquire('task:recover-backlog-creations')
  const combinedContext = contextFor(first, firstFile, handle, 'task:recover-backlog-creations', 'creation')
  check('only an exact combined create-recovery projection queues every valid disjoint edit generation', () => {
    const inspection = writerInspector.inspect()
    const projection = creationMarkers.inspectIntegrity({
      creationContext: combinedContext, writerLeaseInspection: inspection, combinedPublicationRecoveryRequested: true
    })
    assert.equal(projection.combinedRecoveryActive, true)
    const result = editMarkers.scanIntegrity({ stem: 'TASK_999_unrelated', writerLeaseInspection: inspection, combinedCreationProjection: projection })
    assert.equal(result.statuses.filter((row) => row.kind === 'marker' && row.state === 'queued').length, 2)
    assert.equal(hasFinding(result, 'EDIT_INCOMPLETE'), false)
  })

  check('a flag, forged projection, or second live writer cannot queue edit recovery', () => {
    const inspection = writerInspector.inspect()
    const noContext = creationMarkers.inspectIntegrity({
      creationContext: null, writerLeaseInspection: inspection, combinedPublicationRecoveryRequested: true
    })
    assert.equal(noContext.combinedRecoveryActive, false)
    assert.equal(hasFinding(editMarkers.scanIntegrity({ combinedCreationProjection: noContext }), 'EDIT_INCOMPLETE'), true)
    const forged = { combinedRecoveryActive: true, activeIdentities: [] }
    assert.equal(hasFinding(editMarkers.scanIntegrity({ combinedCreationProjection: forged }), 'EDIT_INCOMPLETE'), true)
    const foreign = writerLeases.acquire(writers, {
      rootDir: root, kind: 'task-session', key: 'task:TASK_9_foreign', stem: 'TASK_9_foreign',
      sessionId: writerLeases.createSessionId(), ownerPid: process.pid, childPid: process.pid
    })
    try {
      const denied = creationMarkers.inspectIntegrity({
        creationContext: combinedContext, writerLeaseInspection: writerInspector.inspect(), combinedPublicationRecoveryRequested: true
      })
      assert.equal(denied.combinedRecoveryActive, false)
      assert.equal(hasFinding(editMarkers.scanIntegrity({ combinedCreationProjection: denied }), 'EDIT_INCOMPLETE'), true)
    } finally { writerLeases.release(foreign) }
  })

  check('an intersecting creation/edit identity keeps the whole combined recovery set red', () => {
    const intersecting = {
      ...first, phase: 'number-reserved', number: 71, slug: 'owner_proof', stem: editOne.stem,
      revision: 2, updatedAt: '2026-07-13T00:00:02.000Z'
    }
    const intersectingFile = writeCreation(intersecting)
    const inspection = writerInspector.inspect()
    const intersectingContext = contextFor(intersecting, intersectingFile, handle, 'task:recover-backlog-creations', 'creation')
    const projection = creationMarkers.inspectIntegrity({
      creationContext: intersectingContext, writerLeaseInspection: inspection, combinedPublicationRecoveryRequested: true
    })
    assert.equal(projection.combinedRecoveryActive, true)
    const result = editMarkers.scanIntegrity({ combinedCreationProjection: projection })
    assert.equal(hasFinding(result, 'COMBINED_PUBLICATION_IDENTITY_INTERSECTION'), true)
    assert.equal(hasFinding(result, 'EDIT_INCOMPLETE'), true)
    assert.equal(result.statuses.some((row) => row.state === 'queued'), false)
    writeCreation(first)
  })

  check('creation CAS state prevents cross-owner queueing even with the exact recovery lease', () => {
    const cas = join(creations, '.durable-cas-' + '1'.repeat(16) + '-' + '2'.repeat(16) + '-' + '3'.repeat(16))
    mkdirSync(cas)
    try {
      const projection = creationMarkers.inspectIntegrity({
        creationContext: combinedContext, writerLeaseInspection: writerInspector.inspect(), combinedPublicationRecoveryRequested: true
      })
      assert.equal(projection.combinedRecoveryActive, false)
      assert.equal(hasFinding(projection.envelope, 'CREATION_MARKER_CAS_RECOVERY_REQUIRED'), true)
      assert.equal(hasFinding(editMarkers.scanIntegrity({ combinedCreationProjection: projection }), 'EDIT_INCOMPLETE'), true)
    } finally { rmSync(cas, { recursive: true, force: true }) }
  })
} finally {
  if (handle) try { writerLeases.release(handle) } catch {}
  rmSync(root, { recursive: true, force: true })
}

console.log(`runtime-owner-self-admission: ${checks} checks passed`)
