#!/usr/bin/env node

import assert from 'node:assert/strict'
import { EventEmitter } from 'node:events'
import { existsSync, lstatSync, mkdirSync, mkdtempSync, readFileSync, rmSync, unlinkSync, utimesSync as fsUtimes, writeFileSync } from 'node:fs'
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
      version: 3, action: 'prep', stem, expectedState: 'backlog', sourceRevision: revision,
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

  check('retry never republishes a same-bytes foreign claim generation', () => {
    const id = '17000000003015-retryrace'
    const stem = 'TASK_21_retry_generation'
    enqueue(id, stem)
    const originalStart = sessions.start
    let originalIno = null
    let foreignIno = null
    sessions.start = function () {
      const privateClaim = join(dirs.requests, '.' + id + '.claim')
      const bytes = readFileSync(privateClaim)
      originalIno = lstatSync(privateClaim).ino
      unlinkSync(privateClaim)
      writeFileSync(privateClaim, bytes, { mode: 0o600 })
      foreignIno = lstatSync(privateClaim).ino
      return { running: false, error: 'synthetic-refusal' }
    }
    try { runner.tick() } finally { sessions.start = originalStart }
    assert.notEqual(String(originalIno), String(foreignIno))
    assert.equal(existsSync(join(dirs.requests, id + '.json')), false)
    assert.equal(existsSync(join(dirs.requests, '.' + id + '.claim')), true)
    assert.equal(requests.inspectRequestReservation(stem).status, 'active')
    unlinkSync(join(dirs.requests, '.' + id + '.claim'))
    assert.equal(requests.releaseRequestReservation(requests.inspectRequestReservation(stem).record), true)
  })

  check('terminal settlement never deletes a same-bytes foreign claim generation', () => {
    const id = '17000000003016-consumerace'
    const stem = 'TASK_22_consume_generation'
    enqueue(id, stem)
    runner.tick()
    const started = starts.at(-1)
    assert.equal(started.accepted, true)
    const privateClaim = join(dirs.requests, '.' + id + '.claim')
    const bytes = readFileSync(privateClaim)
    const originalIno = lstatSync(privateClaim).ino
    unlinkSync(privateClaim)
    writeFileSync(privateClaim, bytes, { mode: 0o600 })
    const foreignIno = lstatSync(privateClaim).ino
    assert.notEqual(String(originalIno), String(foreignIno))
    started.meta.onPromptSettled(true, null)
    assert.equal(existsSync(privateClaim), true)
    assert.equal(requests.inspectRequestReservation(stem).status, 'missing')
    assert.equal(starts.pop(), started)
    unlinkSync(privateClaim)
  })

  check('a standby Site process cannot age-requeue a live foreign runner claim during init', () => {
    const id = '17000000003017-liveclaim'
    const stem = 'TASK_23_live_foreign_claim'
    enqueue(id, stem)
    const visible = join(dirs.requests, id + '.json')
    const privateClaim = join(dirs.requests, '.' + id + '.claim')
    assert.equal(requests.transferFileNoClobber(visible, privateClaim), true)
    const aged = new Date(Date.now() - 10 * 60 * 1000)
    fsUtimes(privateClaim, aged, aged)
    const childSource = [
      'const {EventEmitter}=require("node:events");',
      'const cp=require("node:child_process");',
      'cp.spawn=()=>{const c=new EventEmitter();c.kill=()=>true;queueMicrotask(()=>c.emit("close",0));return c};',
      'global.setInterval=()=>({unref(){}});',
      'const cli=require(process.argv[2]);cli.status=()=>({installed:true,loggedIn:true,authProblem:null});',
      'const runner=require(process.argv[1]);runner.init();',
      'setImmediate(()=>process.exit(0));'
    ].join('')
    const child = cp.spawnSync(process.execPath, [
      '-e', childSource,
      require.resolve('../server/runner.js'), require.resolve('../server/cli.js')
    ], { env: process.env, encoding: 'utf8' })
    assert.equal(child.status, 0, child.stderr)
    assert.equal(existsSync(visible), false)
    assert.equal(existsSync(privateClaim), true)
    assert.equal(requests.inspectRequestReservation(stem).status, 'active')
    unlinkSync(privateClaim)
    assert.equal(requests.releaseRequestReservation(requests.inspectRequestReservation(stem).record), true)
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

  check('queued admission waits until a capacity slot frees', () => {
    rmSync(join(dirs.requests, '1700000000303-malformed.json'), { force: true })
    // Deliberately-retained leftover from the warm-child policy check above —
    // drained here so serial-safety claim accounting stays exact.
    rmSync(join(dirs.requests, '1700000000302-policy.json'), { force: true })
    const id = '1700000000304-capacity'
    const stem = 'TASK_4_capacity_hold'
    enqueue(id, stem)
    sessions.runningInfoForStem = () => null
    // Fill every slot the canary cap allows.
    sessions.taskRunningCount = () => runner.MAX_PARALLEL
    runner.tick()
    assert.equal(starts.length, 2, 'no admission while every capacity slot is occupied')
    assert.equal(existsSync(join(dirs.requests, id + '.json')), true)
    assert.equal(existsSync(join(dirs.requests, '.' + id + '.claim')), false)
    sessions.taskRunningCount = () => 0
    runner.tick()
    assert.equal(starts.length, 3, 'freed slot drains the queued request')
    starts[2].meta.onPromptSettled(true, null)
  })

  check('a foreign live board-task writer lease holds ITS OWN task until it clears', () => {
    const writerLeases = require('../../tasks/writer-leases.cjs')
    const paths = require('../server/paths.js')
    const id = '1700000000305-occupied'
    const stem = 'TASK_5_occupied'
    enqueue(id, stem)
    // A board-task writer owned by a live process that is NOT this server —
    // the durable shape left by a restart-orphaned child or a standby run.
    // Since per-task worktree isolation the hold is stem-scoped: a writer for
    // ANOTHER task no longer stalls this one, so the orphan must name THIS stem
    // to hold it — that is exactly the restart-orphan / standby-run shape.
    const foreign = writerLeases.acquire(paths.WRITER_LEASES_DIR, {
      kind: 'task-session', stem: stem, key: 'task:' + stem,
      sessionId: writerLeases.createSessionId(), ownerPid: process.ppid,
      rootDir: paths.WRITER_AUTHORITY_ROOT
    })
    runner.tick()
    assert.equal(starts.length, 3, 'durable foreign writer for this task must hold its admission')
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

  check('an exact foreign live marker stands the runner down; a dead owner is taken over', () => {
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
      version: 1, at: new Date().toISOString(), pid: process.ppid,
      processStartId: foreignStartId, projectRoot: root
    }) + '\n', { mode: 0o600 })
    runner.tick()
    assert.equal(starts.length, 4, 'foreign live runner marker must stand this runner down')
    assert.equal(existsSync(join(dirs.requests, id + '.json')), true)
    assert.equal(JSON.parse(readFileSync(marker, 'utf8')).pid, process.ppid,
      'a live foreign marker is never stolen')
    // A previous/unversioned shape never ages into takeover authority. The
    // operator must remove bytes whose exact owner identity cannot be proved.
    writeFileSync(marker, JSON.stringify({
      at: new Date(Date.now() - 120000).toISOString(), pid: process.ppid,
      processStartId: null, projectRoot: root
    }) + '\n', { mode: 0o600 })
    const aged = new Date(Date.now() - 120000)
    fsUtimes(marker, aged, aged)
    runner.tick()
    assert.equal(starts.length, 4, 'non-conforming marker remains fail-closed regardless of age')
    assert.equal(JSON.parse(readFileSync(marker, 'utf8')).pid, process.ppid)
    // Explicit recovery removes the unprovable marker. A conforming marker
    // with a provably dead owner is then replaced even while fresh.
    unlinkSync(marker)
    cliStatus = { installed: true, loggedIn: false, authProblem: null }
    runner.tick()
    assert.equal(existsSync(marker), false)
    cliStatus = { installed: true, loggedIn: true, authProblem: null }
    const nextId = '1700000000307-deadowner'
    enqueue(nextId, 'TASK_7_dead_owner')
    const dead = cp.spawnSync(process.execPath, ['-e', ''])
    writeFileSync(marker, JSON.stringify({
      version: 1, at: new Date().toISOString(), pid: dead.pid,
      processStartId: writerLeases.captureProcessStartId(process.pid), projectRoot: root
    }) + '\n', { mode: 0o600 })
    runner.tick()
    assert.equal(starts.length, 6,
      'dead-owner marker is taken over and every available queue slot drains')
    assert.equal(JSON.parse(readFileSync(marker, 'utf8')).pid, process.pid)
    assert.equal(JSON.parse(readFileSync(marker, 'utf8')).version, 1)
    assert.equal(starts[4].accepted, true)
    assert.equal(starts[5].accepted, true)
    assert.equal(existsSync(join(dirs.requests, id + '.json')), false)
    assert.equal(existsSync(join(dirs.requests, nextId + '.json')), false)
    assert.equal(existsSync(join(dirs.requests, '.' + id + '.claim')), true)
    assert.equal(existsSync(join(dirs.requests, '.' + nextId + '.claim')), true)
    starts[4].meta.onPromptSettled(true, null)
    starts[5].meta.onPromptSettled(true, null)
    assert.equal(existsSync(join(dirs.requests, '.' + id + '.claim')), false)
    assert.equal(existsSync(join(dirs.requests, '.' + nextId + '.claim')), false)
  })

  check('non-conforming runner-marker content is never parsed as an exact live owner', () => {
    const writerLeases = require('../../tasks/writer-leases.cjs')
    const marker = join(dirs.runs, '.runner-alive')
    // Drop our exact descriptor-backed marker before installing foreign bytes.
    cliStatus = { installed: true, loggedIn: false, authProblem: null }
    runner.tick()
    assert.equal(existsSync(marker), false)
    cliStatus = { installed: true, loggedIn: true, authProblem: null }
    const id = '1700000000308-malformedowner'
    enqueue(id, 'TASK_10_malformed_owner')
    let foreignStartId = null
    try { foreignStartId = writerLeases.captureProcessStartId(process.ppid) } catch {}
    // The extra key makes this content non-conforming. Neither source fields
    // nor age can turn it into an owner or takeover capability.
    writeFileSync(marker, JSON.stringify({
      version: 1, at: new Date().toISOString(), pid: process.ppid,
      processStartId: foreignStartId, projectRoot: root, extra: true
    }) + '\n', { mode: 0o600 })
    runner.tick()
    assert.equal(starts.length, 6, 'fresh non-conforming content stands the runner down')
    const aged = new Date(Date.now() - 120000)
    fsUtimes(marker, aged, aged)
    runner.tick()
    assert.equal(starts.length, 6, 'stale non-conforming content remains fail-closed')
    assert.equal(JSON.parse(readFileSync(marker, 'utf8')).pid, process.ppid)
  })

  console.log(`runner-handoff-settlement: ${checks} checks passed`)
} finally {
  cp.spawn = originalSpawn
  global.setInterval = originalSetInterval
  rmSync(root, { recursive: true, force: true })
}
