#!/usr/bin/env node

import assert from 'node:assert/strict'
import { EventEmitter } from 'node:events'
import { existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, utimesSync as fsUtimes, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const root = mkdtempSync(join(tmpdir(), 'runner-handoff-settlement-'))
const cache = join(root, 'orchestrator', '.cache', 'tasks')
const dirs = {
  requests: join(cache, 'requests'), reservations: join(cache, 'request-reservations'),
  runs: join(cache, 'runs'), locks: join(cache, 'locks'), superseded: join(cache, 'superseded'),
  finalizations: join(cache, 'finalizations'), creations: join(cache, 'creations'),
  edits: join(cache, 'edits'), intake: join(cache, 'intake'),
}
for (const directory of Object.values(dirs)) mkdirSync(directory, { recursive: true })

process.env.ORCHESTRATOR_PROJECT_ROOT = root
process.env.ORCHESTRATOR_REQUESTS_DIR = dirs.requests
process.env.ORCHESTRATOR_REQUEST_RESERVATIONS_DIR = dirs.reservations
process.env.ORCHESTRATOR_RUNS_DIR = dirs.runs
process.env.ORCHESTRATOR_LOCKS_DIR = dirs.locks
process.env.ORCHESTRATOR_SUPERSEDED_DIR = dirs.superseded
process.env.ORCHESTRATOR_FINALIZATIONS_DIR = dirs.finalizations
process.env.ORCHESTRATOR_TASK_CREATIONS_DIR = dirs.creations
process.env.ORCHESTRATOR_TASK_EDITS_DIR = dirs.edits
process.env.ORCHESTRATOR_TASK_INTAKE_DIR = dirs.intake
delete process.env.RUNNER_DISABLED

const cp = require('node:child_process')
const originalSpawn = cp.spawn
const originalSetInterval = global.setInterval
cp.spawn = function () {
  const child = new EventEmitter()
  child.kill = () => true
  queueMicrotask(() => child.emit('close', 0))
  return child
}
global.setInterval = () => ({ unref() {} })

let checks = 0
function check(name, fn) { fn(); checks++; console.log(`ok ${checks} - ${name}`) }

try {
  const requests = require('../server/requests.js')
  const sessions = require('../server/sessions.js')
  const taskIntegrity = require('../server/task-integrity.js')
  const locks = require('../server/locks.js')
  const finalizations = require('../server/finalizations.js')
  const cli = require('../server/cli.js')

  sessions.taskRunningCount = () => 0
  sessions.runGateError = () => null
  sessions.runningInfoForStem = () => null
  sessions.list = () => ({})
  locks.lockPresence = () => ({ validStem: true, present: false, recovery: false })
  finalizations.hasMarker = () => false
  finalizations.mutationBlocked = () => false
  let cliStatus = { installed: true, loggedIn: true, authProblem: null }
  cli.status = () => cliStatus

  const revision = 'sha256:' + 'a'.repeat(64)
  taskIntegrity.validateAction = (action, stem) => ({
    ok: true, action, stem, observedState: 'backlog', sourceRevision: revision,
    indexStatus: 'fresh', snapshotHash: revision, findings: [],
  })

  const starts = []
  sessions.start = function (_key, meta) {
    const accepted = meta.beforePrompt() === true
    starts.push({ meta, accepted })
    return { running: accepted, error: accepted ? null : 'handoff-refused' }
  }

  const runner = require('../server/runner.js')
  runner.init()
  await new Promise((resolve) => setImmediate(resolve))
  assert.equal(runner.isEnabled(), true)

  function enqueue(id, stem) {
    const record = {
      version: 2, action: 'prep', stem, expectedState: 'backlog', sourceRevision: revision,
      dedupKey: null, dedupReport: null, projectRoot: root,
      prompt: 'prepare ' + stem, createdAt: '2026-07-13T00:00:00.000Z',
    }
    const reservation = requests.acquireRequestReservation(id, record)
    assert.equal(reservation.ok, true)
    assert.equal(requests.writeRequestFile(id, record), true)
    return record
  }

  check('logged-out local CLI leaves the durable request for a later runner or standby worker', () => {
    const id = '1700000000299-auth'
    const stem = 'TASK_9_auth_wait'
    enqueue(id, stem)
    cliStatus = { installed: true, loggedIn: false, authProblem: null }
    runner.tick()
    assert.equal(runner.isReady(), false)
    assert.equal(existsSync(join(dirs.requests, id + '.json')), true)
    assert.equal(existsSync(join(dirs.runs, '.runner-alive')), false)
    assert.equal(starts.length, 0)

    cliStatus = { installed: true, loggedIn: true, authProblem: null }
    runner.tick()
    assert.equal(runner.isReady(), true)
    assert.equal(starts.length, 1)
    assert.equal(starts[0].accepted, true)
    starts[0].meta.onPromptSettled(true, null)
    assert.equal(existsSync(join(dirs.requests, id + '.json')), false)
    assert.equal(existsSync(join(dirs.requests, '.' + id + '.claim')), false)
    starts.length = 0
  })

  check('failed asynchronous stdin settlement retains the private at-most-once claim', () => {
    const id = '1700000000300-epipe'
    const stem = 'TASK_1_async_epipe'
    enqueue(id, stem)
    runner.tick()
    assert.equal(starts.length, 1)
    assert.equal(starts[0].accepted, true)
    assert.equal(starts[0].meta.noQuestions, true)
    assert.equal(existsSync(join(dirs.requests, id + '.json')), false)
    assert.equal(existsSync(join(dirs.requests, '.' + id + '.claim')), true)
    assert.equal(requests.inspectRequestReservation(stem).status, 'missing')
    starts[0].meta.onPromptSettled(false, Object.assign(new Error('late EPIPE'), { code: 'EPIPE' }))
    assert.equal(existsSync(join(dirs.requests, '.' + id + '.claim')), true)
  })

  check('claim is consumed only after the stdin callback confirms delivery', () => {
    const id = '1700000000301-delivered'
    const stem = 'TASK_2_async_delivered'
    enqueue(id, stem)
    runner.tick()
    assert.equal(starts.length, 2)
    assert.equal(starts[1].meta.noQuestions, true)
    assert.equal(existsSync(join(dirs.requests, '.' + id + '.claim')), true)
    starts[1].meta.onPromptSettled(true, null)
    assert.equal(existsSync(join(dirs.requests, '.' + id + '.claim')), false)
  })

  check('Prepare is not delivered into a warm child without the action-scoped policy', () => {
    const id = '1700000000302-policy'
    const stem = 'TASK_3_policy_boundary'
    enqueue(id, stem)
    let sendCalls = 0
    sessions.runningInfoForStem = () => ({
      key: 'task:' + stem,
      busy: false,
      noQuestions: false,
    })
    sessions.send = () => { sendCalls++; return true }
    runner.tick()
    assert.equal(sendCalls, 0)
    assert.equal(starts.length, 2)
    assert.equal(existsSync(join(dirs.requests, id + '.json')), true)
    assert.equal(existsSync(join(dirs.requests, '.' + id + '.claim')), false)
  })

  check('malformed public bytes pause the strict queue before any ownership transfer', () => {
    const id = '1700000000303-malformed'
    writeFileSync(join(dirs.requests, id + '.json'), '{broken-json\n')
    runner.tick()
    assert.equal(existsSync(join(dirs.requests, id + '.json')), true)
    assert.equal(existsSync(join(dirs.requests, '.' + id + '.claim')), false)
    assert.equal(starts.length, 2)
  })

  // --- Frozen serial safety (pipeline improvement 01, Phase 0) -------------

  check('a live in-memory task child holds queued admission until the slot frees', () => {
    rmSync(join(dirs.requests, '1700000000303-malformed.json'), { force: true })
    // Deliberately-retained leftover from the warm-child policy check above —
    // drained here so serial-safety claim accounting stays exact.
    rmSync(join(dirs.requests, '1700000000302-policy.json'), { force: true })
    const id = '1700000000304-capacity'
    const stem = 'TASK_4_capacity_hold'
    enqueue(id, stem)
    sessions.runningInfoForStem = () => null
    sessions.taskRunningCount = () => 1
    runner.tick()
    assert.equal(starts.length, 2, 'no admission while the single slot is occupied')
    assert.equal(existsSync(join(dirs.requests, id + '.json')), true)
    assert.equal(existsSync(join(dirs.requests, '.' + id + '.claim')), false)
    sessions.taskRunningCount = () => 0
    runner.tick()
    assert.equal(starts.length, 3, 'freed slot drains the queued request')
    starts[2].meta.onPromptSettled(true, null)
  })

  check('a foreign live board-task writer lease holds the drain until it clears', () => {
    const writerLeases = require('../../tasks/writer-leases.cjs')
    const paths = require('../server/paths.js')
    const id = '1700000000305-occupied'
    const stem = 'TASK_5_occupied'
    enqueue(id, stem)
    // A board-task writer owned by a live process that is NOT this server —
    // the durable shape left by a restart-orphaned child or a standby run.
    const foreign = writerLeases.acquire(paths.WRITER_LEASES_DIR, {
      kind: 'task-session', stem: 'TASK_9_orphan', key: 'task:TASK_9_orphan',
      sessionId: writerLeases.createSessionId(), ownerPid: process.ppid,
      rootDir: paths.WRITER_AUTHORITY_ROOT
    })
    runner.tick()
    assert.equal(starts.length, 3, 'durable foreign writer must hold admission')
    assert.equal(existsSync(join(dirs.requests, id + '.json')), true)
    assert.equal(existsSync(join(dirs.requests, '.' + id + '.claim')), false)
    writerLeases.release(foreign)
    runner.tick()
    assert.equal(starts.length, 4, 'released writer resumes the drain')
    starts[3].meta.onPromptSettled(true, null)
  })

  check('auth flip mid-run keeps the marker only while our child owns a board-task lease', () => {
    const writerLeases = require('../../tasks/writer-leases.cjs')
    const paths = require('../server/paths.js')
    assert.equal(existsSync(join(dirs.runs, '.runner-alive')), true)
    const own = writerLeases.acquire(paths.WRITER_LEASES_DIR, {
      kind: 'task-session', stem: 'TASK_8_own_child', key: 'task:TASK_8_own_child',
      sessionId: writerLeases.createSessionId(), ownerPid: process.pid,
      rootDir: paths.WRITER_AUTHORITY_ROOT
    })
    cliStatus = { installed: true, loggedIn: false, authProblem: null }
    sessions.taskRunningCount = () => 1
    runner.tick()
    assert.equal(existsSync(join(dirs.runs, '.runner-alive')), true,
      'marker must keep standing the standby down while our writer child runs')
    // Non-writer children (skills installs, read-only terminals) keep the
    // session count above zero but hold no board-task lease — the queue must
    // be handed to the standby then.
    writerLeases.release(own)
    runner.tick()
    assert.equal(existsSync(join(dirs.runs, '.runner-alive')), false,
      'a logged-out runner with only non-writer children hands the queue to the standby')
    sessions.taskRunningCount = () => 0
    cliStatus = { installed: true, loggedIn: true, authProblem: null }
    runner.tick()
    assert.equal(existsSync(join(dirs.runs, '.runner-alive')), true)
  })

  check('a fresh foreign live marker stands the runner down; a dead owner is taken over', () => {
    const writerLeases = require('../../tasks/writer-leases.cjs')
    // Release our own marker fd first (logged-out idle path withdraws it).
    cliStatus = { installed: true, loggedIn: false, authProblem: null }
    runner.tick()
    assert.equal(existsSync(join(dirs.runs, '.runner-alive')), false)
    cliStatus = { installed: true, loggedIn: true, authProblem: null }
    const id = '1700000000306-foreign'
    enqueue(id, 'TASK_6_foreign_runner')
    let foreignStartId = null
    try { foreignStartId = writerLeases.captureProcessStartId(process.ppid) } catch {}
    const marker = join(dirs.runs, '.runner-alive')
    writeFileSync(marker, JSON.stringify({
      at: new Date().toISOString(), pid: process.ppid,
      processStartId: foreignStartId, projectRoot: root
    }) + '\n', { mode: 0o600 })
    runner.tick()
    assert.equal(starts.length, 4, 'foreign live runner marker must stand this runner down')
    assert.equal(existsSync(join(dirs.requests, id + '.json')), true)
    assert.equal(JSON.parse(readFileSync(marker, 'utf8')).pid, process.ppid,
      'a live foreign marker is never stolen')
    // Unproven identity (no processStartId → 'pid-live') plus a STALE file is
    // taken over: a live runner refreshes the marker every tick, so a stale
    // mtime proves the writer is gone and the live pid is a bystander/reuse.
    writeFileSync(marker, JSON.stringify({
      at: new Date(Date.now() - 120000).toISOString(), pid: process.ppid,
      processStartId: null, projectRoot: root
    }) + '\n', { mode: 0o600 })
    const aged = new Date(Date.now() - 120000)
    fsUtimes(marker, aged, aged)
    runner.tick()
    assert.equal(starts.length, 5, 'stale unproven-identity marker is taken over and the queue drains')
    assert.equal(JSON.parse(readFileSync(marker, 'utf8')).pid, process.pid)
    starts[4].meta.onPromptSettled(true, null)
    // A provably dead owner is replaced even while FRESH.
    cliStatus = { installed: true, loggedIn: false, authProblem: null }
    runner.tick()                                    // idle logged-out → withdraw own marker
    assert.equal(existsSync(marker), false)
    cliStatus = { installed: true, loggedIn: true, authProblem: null }
    const nextId = '1700000000307-deadowner'
    enqueue(nextId, 'TASK_7_dead_owner')
    const dead = cp.spawnSync(process.execPath, ['-e', ''])
    writeFileSync(marker, JSON.stringify({
      at: new Date().toISOString(), pid: dead.pid,
      processStartId: null, projectRoot: root
    }) + '\n', { mode: 0o600 })
    runner.tick()
    assert.equal(starts.length, 6, 'dead-owner marker is taken over and the queue drains')
    assert.equal(JSON.parse(readFileSync(marker, 'utf8')).pid, process.pid)
    starts[5].meta.onPromptSettled(true, null)
  })

  console.log(`runner-handoff-settlement: ${checks} checks passed`)
} finally {
  cp.spawn = originalSpawn
  global.setInterval = originalSetInterval
  rmSync(root, { recursive: true, force: true })
}
