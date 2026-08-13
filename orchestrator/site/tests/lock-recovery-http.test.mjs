#!/usr/bin/env node

import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { spawn } from 'node:child_process'
import { createServer } from 'node:http'
import { chmodSync, existsSync, mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { hostname, tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const REPO = join(HERE, '..', '..', '..')
const OUTCOME_SHAPE = join(REPO, 'orchestrator', 'contracts', 'outcome-shape.json')
const root = mkdtempSync(join(tmpdir(), 'lock-recovery-http-'))
const scratch = mkdtempSync(join(tmpdir(), 'lock-recovery-http-scratch-'))
const require = createRequire(import.meta.url)
const taskSource = require('../../tasks/task-source-contract.cjs')
const SOURCE_BLOCK = taskSource.render(taskSource.manualForIntent('lock-recovery-http', 'manual', 'fixture:lock-recovery-http'))
const tasks = join(root, 'orchestrator', 'tasks')
const cache = join(root, 'orchestrator', '.cache', 'tasks')
for (const column of ['backlog', 'pending', 'todo', 'done']) mkdirSync(join(tasks, column), { recursive: true })
for (const dir of ['locks', 'requests', 'request-reservations', 'runs', 'superseded', 'finalizations',
  'finalizations/.writers', 'creations', 'edits', 'intake']) mkdirSync(join(cache, dir), { recursive: true })
if (process.platform !== 'win32') chmodSync(join(cache, 'intake'), 0o700)

const stem = 'TASK_1_dead_site_lock'
writeFileSync(join(tasks, 'todo', stem + '.md'), [
  '# TASK 1 — Dead Site lock', '',
  SOURCE_BLOCK, '',
  '## Goal', '', 'Recover an exact dead Site owner.', '',
  '## Inputs', '', '- Canonical task-lock state.', '',
  '## Acceptance', '', '### Automated', '', '- Run `node orchestrator/site/tests/lock-recovery-http.test.mjs`.', '',
  '### Manual', '', '- Inspect the Board control.', '',
  '## Out of scope', '', '- Durable task repair.', ''
].join('\n'))

const core = require('../../tasks/task-state-core.cjs')
const writerLeases = require('../../tasks/writer-leases.cjs')
const initial = core.validateTaskState({ repoRoot: root, tasksDir: tasks, checkIndex: false, includeRuntime: false,
  outcomeShapePath: OUTCOME_SHAPE })
assert.equal(initial.ok, true, JSON.stringify(initial.findings))
writeFileSync(join(tasks, 'INDEX.json'), JSON.stringify(core.deriveIndex(initial._model, '2026-07-18T00:00:00.000Z'), null, 2) + '\n')

process.env.ORCHESTRATOR_PROJECT_ROOT = root
process.env.ORCHESTRATOR_TASKS_DIR = tasks
process.env.ORCHESTRATOR_LOCKS_DIR = join(cache, 'locks')
process.env.ORCHESTRATOR_REQUESTS_DIR = join(cache, 'requests')
process.env.ORCHESTRATOR_REQUEST_RESERVATIONS_DIR = join(cache, 'request-reservations')
process.env.ORCHESTRATOR_RUNS_DIR = join(cache, 'runs')
process.env.ORCHESTRATOR_SUPERSEDED_DIR = join(cache, 'superseded')
process.env.ORCHESTRATOR_FINALIZATIONS_DIR = join(cache, 'finalizations')
process.env.ORCHESTRATOR_WRITER_LEASES_DIR = join(cache, 'finalizations', '.writers')
process.env.ORCHESTRATOR_TASK_CREATIONS_DIR = join(cache, 'creations')
process.env.ORCHESTRATOR_TASK_EDITS_DIR = join(cache, 'edits')
process.env.ORCHESTRATOR_TASK_INTAKE_DIR = join(cache, 'intake')
process.env.ORCHESTRATOR_STATE_FILE = join(root, 'orchestrator', '.cache', 'site', 'state.json')
process.env.SHALLOW_INTAKE_SCRATCH_DIR = scratch
process.env.ORCHESTRATOR_OUTCOME_SHAPE_PATH = OUTCOME_SHAPE
process.env.TASK_FS_TEST_ROOT = root
process.env.RUNNER_DISABLED = '1'
process.env.FIGMA_WIRING_GATE = '0'

async function waitUntil(fn, message) {
  const deadline = Date.now() + 10000
  while (Date.now() < deadline) {
    try { if (fn()) return }
    catch (_) { /* retry */ }
    await new Promise((resolve) => setTimeout(resolve, 10))
  }
  throw new Error(message)
}

const owner = spawn(process.execPath, ['-e', 'setInterval(() => {}, 1000)'], { stdio: 'ignore' })
await waitUntil(() => writerLeases.PROCESS_START_ID_RE.test(writerLeases.captureProcessStartId(owner.pid)),
  'owner identity did not become observable')
const startedAt = new Date().toISOString()
const lockRecord = {
  version: 1,
  stem,
  stage: 'orchestrator',
  runId: 'run-lock-recovery-http-0001',
  sessionId: 'ws-lock-recovery-http-session-0001',
  startedAt,
  owner: {
    kind: 'site',
    id: 'site:http-fixture',
    pid: owner.pid,
    processStartId: writerLeases.captureProcessStartId(owner.pid),
    hostname: hostname(),
    startedAt,
  },
}
const lockBytes = Buffer.from(JSON.stringify(lockRecord, null, 2) + '\n', 'utf8')
writeFileSync(join(cache, 'locks', stem + '.json'), lockBytes, { flag: 'wx', mode: 0o600 })
const acquired = {
  ...lockRecord,
  lockHash: 'sha256:' + createHash('sha256').update(lockBytes).digest('hex'),
}
owner.kill('SIGTERM')
await new Promise((resolve) => owner.once('close', resolve))

const httpMod = require('../server/http.js')
const server = createServer(httpMod.handle)
await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve))
const base = `http://127.0.0.1:${server.address().port}`
let checks = 0
async function check(name, fn) { await fn(); checks++; console.log(`ok ${checks} - ${name}`) }

try {
  const state = await (await fetch(base + '/api/state')).json()
  const headers = { 'content-type': 'application/json', 'x-orchestrator-csrf': state.csrfToken, origin: base }

  await check('inspection proves only exact dead-owner state and redacts private receipt fields', async () => {
    const response = await fetch(base + '/api/tasks/lock-recovery?stem=' + encodeURIComponent(stem))
    assert.equal(response.status, 200)
    const body = await response.json()
    assert.equal(body.operation, 'owner-status')
    assert.equal(body.recoverable, true)
    assert.equal(body.lockHash, acquired.lockHash)
    assert.ok(['dead', 'reused'].includes(body.ownerState))
    assert.equal(Object.hasOwn(body, 'runId'), false)
    assert.equal(Object.hasOwn(body, 'sessionId'), false)
    assert.equal(Object.hasOwn(body, 'owner'), false)
    assert.equal(Object.hasOwn(body, 'pid'), false)
  })

  await check('recovery mutation inherits CSRF/origin/body-shape guards', async () => {
    let response = await fetch(base + '/api/tasks/lock-recovery', {
      method: 'POST', headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ stem, expectedLockHash: acquired.lockHash }),
    })
    assert.equal(response.status, 403)
    assert.equal(existsSync(join(cache, 'locks', stem + '.json')), true)

    response = await fetch(base + '/api/tasks/lock-recovery', {
      method: 'POST', headers,
      body: JSON.stringify({ stem, expectedLockHash: acquired.lockHash, unexpected: true }),
    })
    assert.equal(response.status, 400)
    assert.equal((await response.json()).error, 'bad-lock-recovery-request')
    assert.equal(existsSync(join(cache, 'locks', stem + '.json')), true)
  })

  await check('a live same-task writer blocks both inspection verdict and phase two', async () => {
    const conflict = writerLeases.acquire(join(cache, 'finalizations', '.writers'), {
      rootDir: root, kind: 'task-session', stem, key: 'task:' + stem,
      ownerPid: process.pid, pendingChild: false, sessionId: writerLeases.createSessionId(),
    })
    try {
      let response = await fetch(base + '/api/tasks/lock-recovery?stem=' + encodeURIComponent(stem))
      assert.equal(response.status, 200)
      let body = await response.json()
      assert.equal(body.recoverable, false)
      assert.equal(body.reason, 'writer-active')

      response = await fetch(base + '/api/tasks/lock-recovery', {
        method: 'POST', headers,
        body: JSON.stringify({ stem, expectedLockHash: acquired.lockHash }),
      })
      assert.equal(response.status, 409)
      body = await response.json()
      assert.equal(body.error, 'finalization-active')
      assert.equal(existsSync(join(cache, 'locks', stem + '.json')), true)
    } finally {
      assert.equal(writerLeases.release(conflict), true)
    }
    const after = await (await fetch(base + '/api/tasks/lock-recovery?stem=' + encodeURIComponent(stem))).json()
    assert.equal(after.recoverable, true)
  })

  await check('stale inspection hashes fail closed and settle their writer lease', async () => {
    const response = await fetch(base + '/api/tasks/lock-recovery', {
      method: 'POST', headers,
      body: JSON.stringify({ stem, expectedLockHash: 'sha256:' + '0'.repeat(64) }),
    })
    assert.equal(response.status, 409)
    const body = await response.json()
    assert.equal(body.reasonCode, 'LOCK_IDENTITY_MISMATCH')
    assert.equal(existsSync(join(cache, 'locks', stem + '.json')), true)
    assert.deepEqual(writerLeases.scan(join(cache, 'finalizations', '.writers'), root).active, [])
  })

  await check('exact phase-two recovery releases once and returns a redacted receipt', async () => {
    const response = await fetch(base + '/api/tasks/lock-recovery', {
      method: 'POST', headers,
      body: JSON.stringify({ stem, expectedLockHash: acquired.lockHash }),
    })
    assert.equal(response.status, 200)
    const body = await response.json()
    assert.equal(body.operation, 'recover-owner')
    assert.equal(body.released, true)
    assert.equal(body.state, 'todo')
    assert.equal(Object.hasOwn(body, 'runId'), false)
    assert.equal(Object.hasOwn(body, 'sessionId'), false)
    assert.equal(Object.hasOwn(body, 'releaseReceipt'), false)
    assert.equal(existsSync(join(cache, 'locks', stem + '.json')), false)
    assert.deepEqual(writerLeases.scan(join(cache, 'finalizations', '.writers'), root).active, [])
  })

  await check('a repeated recovery cannot act on the old inspection', async () => {
    const response = await fetch(base + '/api/tasks/lock-recovery', {
      method: 'POST', headers,
      body: JSON.stringify({ stem, expectedLockHash: acquired.lockHash }),
    })
    assert.equal(response.status, 404)
    assert.equal((await response.json()).error, 'task-lock-not-found')
  })
} finally {
  await new Promise((resolve) => server.close(resolve))
  if (owner.exitCode === null && owner.signalCode === null) owner.kill('SIGKILL')
  rmSync(root, { recursive: true, force: true })
  rmSync(scratch, { recursive: true, force: true })
}

console.log(`lock-recovery-http: ${checks} checks passed`)
