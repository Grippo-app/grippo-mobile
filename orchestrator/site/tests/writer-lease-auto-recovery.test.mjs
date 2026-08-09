#!/usr/bin/env node

import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join, resolve } from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const ROOT = mkdtempSync(join(tmpdir(), 'writer-auto-recovery-'))
const FINALIZATIONS = join(ROOT, 'orchestrator', '.cache', 'tasks', 'finalizations')
const WRITERS = join(FINALIZATIONS, '.writers')
const TASKS = join(ROOT, 'orchestrator', 'tasks')
mkdirSync(FINALIZATIONS, { recursive: true })
mkdirSync(TASKS, { recursive: true })
Object.assign(process.env, {
  ORCHESTRATOR_PROJECT_ROOT: ROOT,
  ORCHESTRATOR_TASKS_DIR: TASKS,
  ORCHESTRATOR_FINALIZATIONS_DIR: FINALIZATIONS,
  ORCHESTRATOR_WRITER_AUTHORITY_ROOT: ROOT,
})

const require = createRequire(import.meta.url)
const backlogCreate = require('../server/backlog-create.js')
const writerLeases = require('../../tasks/writer-leases.cjs')
const MODULE = resolve(HERE, '../../tasks/writer-leases.cjs')
const CHILD = String.raw`
const fs = require('fs')
const writer = require(process.argv[1])
const root = process.argv[2]
const dir = process.argv[3]
const handle = writer.acquire(dir, {
  rootDir: root, kind: 'task-session', stem: 'TASK_72_auto_recovery', key: 'direct:auto-recovery',
  sessionId: writer.createSessionId(), ownerPid: process.pid, ttlMs: 60000
})
fs.writeSync(1, JSON.stringify({ path: handle.path, before: fs.readFileSync(handle.path, 'base64') }) + '\n')
writer.renew(handle, 120000)
`
const emptyCreation = () => ({ blocking: [], incomplete: [], completedCount: 0 })
const emptyEdit = () => ({ blocking: [], incomplete: [], completedCount: 0 })
let checks = 0
async function check(name, fn) { await fn(); checks++; console.log(`ok ${checks} - ${name}`) }

try {
  await check('controller startup automatically reconciles a dead writer mutation before publication polling', async () => {
    const child = spawnSync(process.execPath, ['-e', CHILD, MODULE, ROOT, WRITERS], {
      encoding: 'utf8', env: { ...process.env, WRITER_LEASE_FAILPOINT: 'renewal:after-detach' }
    })
    assert.equal(child.signal, 'SIGKILL', child.stderr + child.stdout)
    const handle = JSON.parse(child.stdout.split('\n').find(Boolean))
    assert.equal(existsSync(handle.path), false)
    assert.ok(writerLeases.scan(WRITERS, ROOT).issues.length > 0)
    let reconciled = null
    const controller = backlogCreate.createRecoveryController({
      creationScan: emptyCreation, editScan: emptyEdit,
      onWriterReconciled: (result) => { reconciled = result },
      intervalMs: 60000,
    })
    try { await controller.start() } finally { controller.stop() }
    assert.equal(reconciled?.reconciled.length, 1)
    assert.deepEqual(reconciled?.blocked, [])
    assert.deepEqual(readFileSync(handle.path), Buffer.from(handle.before, 'base64'),
      'the restored bounded generation remains protected until its explicit TTL expires')
    assert.deepEqual(writerLeases.scan(WRITERS, ROOT).issues, [])
  })

  await check('controller releases a clean canonical lease only after its exact owner generation is gone', async () => {
    const childScript = String.raw`
const writer = require(process.argv[1])
const root = process.argv[2]
const dir = process.argv[3]
const handle = writer.acquire(dir, {
  rootDir: root, kind: 'task-session', stem: 'TASK_73_canonical_recovery', key: 'task:TASK_73_canonical_recovery',
  sessionId: writer.createSessionId(), ownerPid: process.pid
})
process.stdout.write(handle.path + '\n')
`
    const child = spawnSync(process.execPath, ['-e', childScript, MODULE, ROOT, WRITERS], {
      encoding: 'utf8', env: { ...process.env }
    })
    assert.equal(child.status, 0, child.stderr)
    const stalePath = child.stdout.trim()
    assert.equal(existsSync(stalePath), true)
    assert.equal(writerLeases.scan(WRITERS, ROOT).stale.length, 1)

    let reconciled = null
    const controller = backlogCreate.createRecoveryController({
      creationScan: emptyCreation, editScan: emptyEdit,
      onWriterReconciled: (result) => { reconciled = result },
      intervalMs: 60000,
    })
    try { await controller.start() } finally { controller.stop() }
    assert.deepEqual(reconciled?.blocked, [])
    assert.equal(reconciled?.reconciled.some((row) => row.leaseId && row.state === 'stale-canonical-released'), true)
    assert.equal(existsSync(stalePath), false)
    assert.equal(writerLeases.scan(WRITERS, ROOT).stale.length, 0)

    const live = writerLeases.acquire(WRITERS, {
      rootDir: ROOT, kind: 'task-session', stem: 'TASK_74_live_owner', key: 'task:TASK_74_live_owner',
      sessionId: writerLeases.createSessionId(), ownerPid: process.pid,
    })
    const liveResult = writerLeases.reconcileStaleMutations(WRITERS, ROOT)
    assert.deepEqual(liveResult, { reconciled: [], blocked: [] })
    assert.equal(existsSync(live.path), true, 'an exact live owner must never be reconciled')
    assert.equal(writerLeases.release(live), true)
  })

  await check('controller preserves malformed writer artifacts and reports a bounded recovery error', async () => {
    const leaseId = writerLeases.createLeaseId()
    const artifact = join(WRITERS, `.${leaseId}.malformed-wal`)
    writeFileSync(artifact, 'foreign-secret-bytes')
    let error = null
    const controller = backlogCreate.createRecoveryController({
      creationScan: emptyCreation, editScan: emptyEdit,
      onError: (value) => { error = value },
      intervalMs: 60000,
    })
    try { await controller.start() } finally { controller.stop() }
    assert.equal(error?.code, 'WRITER_LEASE_RECOVERY_BLOCKED')
    assert.match(error?.message || '', /reconciliation remains blocked/)
    assert.equal(readFileSync(artifact, 'utf8'), 'foreign-secret-bytes')
  })

  await check('controller never starts publication recovery beside a live workspace writer', async () => {
    const creationState = {
      blocking: [{ code: 'CREATION_INCOMPLETE', stem: 'TASK_75_live_create' }],
      incomplete: [{
        keyHash: 'sha256:' + '7'.repeat(64),
        transactionId: '7'.repeat(32),
        stem: 'TASK_75_live_create',
      }],
      completedCount: 0,
    }
    let recoveryCalls = 0
    let recoveryOutcome = null
    const controller = backlogCreate.createRecoveryController({
      creationScan: () => creationState,
      editScan: emptyEdit,
      writerReconcile: () => ({ reconciled: [], blocked: [] }),
      writerScan: () => ({
        active: [{ leaseId: 'wr-live-create-owner' }],
        stale: [],
        issues: [],
      }),
      recover: async () => { recoveryCalls++; return null },
      onAttemptSettled: (outcome) => { recoveryOutcome = outcome },
      intervalMs: 60000,
    })
    try { await controller.start() } finally { controller.stop() }
    assert.equal(recoveryCalls, 0,
      'the poller must not perturb writer state while the live publisher performs its final validation')
    assert.equal(recoveryOutcome?.ok, false)
    assert.equal(recoveryOutcome?.code, 'finalization-active')
  })

  console.log(`writer-lease auto recovery: ${checks} checks passed`)
} finally {
  rmSync(ROOT, { recursive: true, force: true })
}
