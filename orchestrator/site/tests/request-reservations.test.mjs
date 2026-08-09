#!/usr/bin/env node

import assert from 'node:assert/strict'
import { spawn, spawnSync } from 'node:child_process'
import {
  chmodSync, existsSync, linkSync, lstatSync, mkdirSync, mkdtempSync, readFileSync, readdirSync,
  rmSync, symlinkSync, unlinkSync, writeFileSync
} from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const here = dirname(fileURLToPath(import.meta.url))
const modulePath = join(here, '..', 'server', 'requests.js')
const cliPath = join(here, '..', 'scripts', 'request-reservation.mjs')
const httpModulePath = join(here, '..', 'server', 'http.js')
const root = mkdtempSync(join(tmpdir(), 'request-reservations-'))
const scratch = mkdtempSync(join(tmpdir(), 'request-reservations-scratch-'))
const cache = join(root, 'orchestrator', '.cache', 'tasks')
const tasksDir = join(root, 'orchestrator', 'tasks')
const requestsDir = join(cache, 'requests')
const reservationsDir = join(cache, 'request-reservations')
const runsDir = join(cache, 'runs')
for (const column of ['backlog', 'pending', 'todo', 'done']) mkdirSync(join(tasksDir, column), { recursive: true })
for (const dir of [
  requestsDir, reservationsDir, runsDir, join(cache, 'locks'),
  join(cache, 'superseded'), join(cache, 'finalizations'),
  join(cache, 'creations'), join(cache, 'edits'), join(cache, 'intake')
]) {
  mkdirSync(dir, { recursive: true })
}
if (process.platform !== 'win32') {
  chmodSync(join(cache, 'intake'), 0o700)
  chmodSync(scratch, 0o700)
}

process.env.ORCHESTRATOR_PROJECT_ROOT = root
process.env.ORCHESTRATOR_REQUESTS_DIR = requestsDir
process.env.ORCHESTRATOR_REQUEST_RESERVATIONS_DIR = reservationsDir
process.env.ORCHESTRATOR_RUNS_DIR = runsDir
process.env.ORCHESTRATOR_TASKS_DIR = tasksDir
process.env.ORCHESTRATOR_LOCKS_DIR = join(cache, 'locks')
process.env.ORCHESTRATOR_SUPERSEDED_DIR = join(cache, 'superseded')
process.env.ORCHESTRATOR_FINALIZATIONS_DIR = join(cache, 'finalizations')
process.env.ORCHESTRATOR_TASK_CREATIONS_DIR = join(cache, 'creations')
process.env.ORCHESTRATOR_TASK_EDITS_DIR = join(cache, 'edits')
process.env.ORCHESTRATOR_TASK_INTAKE_DIR = join(cache, 'intake')
process.env.SHALLOW_INTAKE_SCRATCH_DIR = scratch
process.env.ORCHESTRATOR_STATE_FILE = join(root, 'orchestrator', '.cache', 'site', 'state.json')
process.env.RUNNER_DISABLED = '1'

const require = createRequire(import.meta.url)
const requests = require(modulePath)
const taskSource = require('../../tasks/task-source-contract.cjs')
const SOURCE_BLOCK = taskSource.render(taskSource.manualForIntent('request-reservations', 'manual', 'fixture:request-reservations'))
const stem = 'TASK_9_atomic_request_handoff'
writeFileSync(join(tasksDir, 'backlog', stem + '.md'), '# TASK 9 — Atomic request handoff\n\n' + SOURCE_BLOCK + '\n\n## Goal\nSerialize Site admission.\n')
const taskCore = require('../../tasks/task-state-core.cjs')
const taskSnapshot = taskCore.validateTaskState({ tasksDir, repoRoot: root, checkIndex: false })
assert.equal(taskSnapshot.ok, true)
writeFileSync(join(tasksDir, 'INDEX.json'), JSON.stringify(taskCore.deriveIndex(taskSnapshot._model, '2026-07-13T12:00:00Z'), null, 2) + '\n')
const base = {
  version: 2,
  action: 'prep',
  stem,
  expectedState: 'backlog',
  sourceRevision: 'sha256:' + 'a'.repeat(64),
  dedupKey: null,
  dedupReport: null,
  projectRoot: root,
  prompt: 'Prepare the exact task.',
  createdAt: '2026-07-13T12:00:00.000Z'
}

let checks = 0
async function check(name, fn) {
  await fn()
  checks++
  console.log(`ok ${checks} - ${name}`)
}

function childAcquire(id, record, startAt) {
  const source = [
    'const mod=require(process.argv[1]);',
    'const id=process.argv[2];',
    'const record=JSON.parse(Buffer.from(process.argv[3],"base64").toString("utf8"));',
    'const start=Number(process.argv[4]);',
    'const wait=new Int32Array(new SharedArrayBuffer(4));',
    'while(Date.now()<start) Atomics.wait(wait,0,0,Math.min(20,start-Date.now()));',
    'process.stdout.write(JSON.stringify(mod.acquireRequestReservation(id,record)));'
  ].join('')
  const encoded = Buffer.from(JSON.stringify(record)).toString('base64')
  const child = spawn(process.execPath, ['-e', source, modulePath, id, encoded, String(startAt)], {
    env: process.env, stdio: ['ignore', 'pipe', 'pipe']
  })
  return new Promise((resolve, reject) => {
    let stdout = '', stderr = ''
    child.stdout.on('data', (chunk) => { stdout += chunk })
    child.stderr.on('data', (chunk) => { stderr += chunk })
    child.on('error', reject)
    child.on('close', (code) => {
      if (code !== 0) reject(new Error(`child ${id} exited ${code}: ${stderr}`))
      else {
        try { resolve(JSON.parse(stdout)) }
        catch (error) { reject(new Error(`child ${id} returned invalid JSON: ${stdout}`)) }
      }
    })
  })
}

function startHttpServer() {
  const source = [
    'const http=require("node:http");',
    'const handler=require(process.argv[1]);',
    'const server=http.createServer(handler.handle);',
    'server.listen(0,"127.0.0.1",()=>process.stdout.write("READY "+server.address().port+"\\n"));',
    'process.on("SIGTERM",()=>server.close(()=>process.exit(0)));'
  ].join('')
  const child = spawn(process.execPath, ['-e', source, httpModulePath], {
    env: process.env, stdio: ['ignore', 'pipe', 'pipe']
  })
  return new Promise((resolve, reject) => {
    let stdout = '', stderr = '', settled = false
    const timer = setTimeout(() => {
      if (settled) return
      settled = true
      child.kill('SIGKILL')
      reject(new Error('HTTP child startup timed out: ' + stderr))
    }, 10000)
    child.stdout.on('data', (chunk) => {
      stdout += chunk
      const match = /(?:^|\n)READY (\d+)\n/.exec(stdout)
      if (!match || settled) return
      settled = true
      clearTimeout(timer)
      resolve({ child, base: `http://127.0.0.1:${match[1]}` })
    })
    child.stderr.on('data', (chunk) => { stderr += chunk })
    child.on('error', (error) => {
      if (!settled) { settled = true; clearTimeout(timer); reject(error) }
    })
    child.on('close', (code) => {
      if (!settled) {
        settled = true
        clearTimeout(timer)
        reject(new Error(`HTTP child exited ${code}: ${stderr}`))
      }
    })
  })
}

function stopHttpServer(instance) {
  return new Promise((resolve) => {
    if (!instance || !instance.child || instance.child.exitCode !== null) return resolve()
    const timer = setTimeout(() => { instance.child.kill('SIGKILL') }, 3000)
    instance.child.once('close', () => { clearTimeout(timer); resolve() })
    instance.child.kill('SIGTERM')
  })
}

async function typedPrepare(server, csrf, idempotencyKey) {
  const summaryResponse = await fetch(server.base + '/api/tasks/' + stem + '/summary')
  assert.equal(summaryResponse.status, 200)
  const summary = await summaryResponse.json()
  const action = summary.task.primaryAction
  assert.equal(action.kind, 'prepare')
  return {
    url: server.base + '/api/tasks/actions',
    options: {
      method: 'POST',
      headers: {
        'content-type': 'application/json',
        'x-orchestrator-csrf': csrf,
        origin: server.base,
      },
      body: JSON.stringify({
        stem,
        actionId: action.id,
        actionRevision: action.actionRevision,
        action: action.kind,
        expectedState: action.expectedState,
        expectedSourceRevision: action.expectedSourceRevision,
        checkpointId: null,
        confirmation: null,
        confirmationToken: null,
        answers: null,
        questionRound: null,
        expectedQuestionsRevision: null,
        liveSessionId: null,
        expectedSessionRevision: null,
        idempotencyKey,
      }),
    },
  }
}

try {
  await check('fingerprint covers executable intent and excludes only transport timestamp', () => {
    const fingerprint = requests.requestFingerprint(base)
    assert.match(fingerprint, /^sha256:[a-f0-9]{64}$/)
    assert.equal(requests.requestFingerprint({ ...base, createdAt: '2026-07-13T12:00:01.000Z' }), fingerprint)
    for (const changed of [
      { ...base, prompt: 'A different answers payload.' },
      { ...base, action: 'answers', expectedState: 'pending' },
      { ...base, sourceRevision: 'sha256:' + 'b'.repeat(64) },
      { ...base, dedupKey: 'report:one' },
      { ...base, dedupKey: 'report:one', dedupReport: 'sha256:' + 'c'.repeat(64) }
    ]) assert.notEqual(requests.requestFingerprint(changed), fingerprint)
  })

  const firstId = '1700000000000-first'
  let firstHandle
  await check('acquire/inspect/ensure are exact and read-only; wrong token cannot withdraw', () => {
    const acquired = requests.acquireRequestReservation(firstId, base)
    assert.equal(acquired.ok, true)
    assert.equal(acquired.acquired, true)
    firstHandle = acquired.handle
    const file = join(reservationsDir, stem + '.json')
    const bytes = readFileSync(file)
    const before = lstatSync(file)
    const inspected = requests.inspectRequestReservation(stem)
    const after = lstatSync(file)
    assert.equal(inspected.status, 'active')
    assert.deepEqual(inspected.record, firstHandle)
    assert.deepEqual(readFileSync(file), bytes)
    assert.equal(after.mtimeMs, before.mtimeMs)
    assert.equal(after.ino, before.ino)

    const exact = requests.ensureRequestReservation(firstId, base)
    assert.equal(exact.ok, true)
    assert.equal(exact.acquired, false)
    assert.deepEqual(exact.handle, firstHandle)
    assert.equal(requests.ensureRequestReservation('1700000000001-second', base).ok, false)
    assert.equal(requests.ensureRequestReservation(firstId, { ...base, prompt: 'different answers' }).ok, false)

    assert.equal(requests.releaseRequestReservation({ ...firstHandle, token: 'f'.repeat(64) }), false)
    assert.deepEqual(readFileSync(file), bytes)
  })

  await check('read-only CLI inspect redacts token and does not touch reservation bytes', () => {
    const file = join(reservationsDir, stem + '.json')
    const bytes = readFileSync(file)
    const before = lstatSync(file)
    const result = spawnSync(process.execPath, [cliPath, 'inspect', '--stem', stem], {
      env: process.env, encoding: 'utf8'
    })
    assert.equal(result.status, 0, result.stderr)
    const output = JSON.parse(result.stdout)
    assert.equal(output.status, 'active')
    assert.equal(Object.hasOwn(output.record, 'token'), false)
    assert.equal(result.stdout.includes(firstHandle.token), false)
    assert.deepEqual(readFileSync(file), bytes)
    assert.equal(lstatSync(file).mtimeMs, before.mtimeMs)
  })

  await check('exact release removes the first generation', () => {
    assert.equal(requests.releaseRequestReservation(firstHandle), true)
    assert.equal(requests.inspectRequestReservation(stem).status, 'missing')
  })

  await check('stem-scoped integrity finds an orphan queue even when no reservation exists', () => {
    const id = '1700000000002-orphan'
    assert.equal(requests.writeRequestFile(id, base), true)
    const result = requests.scanIntegrity(stem)
    assert.equal(result.findings.some((finding) => finding.code === 'REQUEST_RESERVATION_RECORD_MISMATCH' &&
      finding.stem === stem), true)
    assert.equal(result.statuses.some((status) => status.kind === 'queue' && status.requestId === id &&
      status.stem === stem), true)
    assert.equal(result.snapshotInputs.some((input) => input.path === join(requestsDir, id + '.json')), true)
    unlinkSync(join(requestsDir, id + '.json'))
  })

  await check('stem-scoped integrity reports an extra orphan beside the indexed owner', () => {
    const indexedId = '1700000000003-indexed'
    const orphanId = '1700000000004-extra'
    const acquired = requests.acquireRequestReservation(indexedId, base)
    assert.equal(acquired.ok, true)
    assert.equal(requests.writeRequestFile(indexedId, base), true)
    assert.equal(requests.writeRequestFile(orphanId, { ...base, createdAt: '2026-07-13T12:00:04.000Z' }), true)
    const result = requests.scanIntegrity(stem)
    assert.equal(result.findings.some((finding) => finding.code === 'REQUEST_RESERVATION_RECORD_MISMATCH'), true)
    assert.equal(result.findings.some((finding) => finding.code === 'REQUEST_MULTIPLE_OWNERS' && finding.stem === stem), true)
    assert.deepEqual(result.statuses.filter((status) => status.kind === 'queue').map((status) => status.requestId).sort(),
      [indexedId, orphanId].sort())
    unlinkSync(join(requestsDir, indexedId + '.json'))
    unlinkSync(join(requestsDir, orphanId + '.json'))
    assert.equal(requests.releaseRequestReservation(acquired.handle), true)
  })

  await check('independent processes have exactly one per-stem acquisition winner', async () => {
    const startAt = Date.now() + 500
    const attempts = Array.from({ length: 8 }, (_, i) => childAcquire(
      `17000000001${i}-race${i}`,
      { ...base, prompt: `answers generation ${i}`, createdAt: `2026-07-13T12:00:0${i}.000Z` },
      startAt
    ))
    const results = await Promise.all(attempts)
    const verdicts = results.map((row) => ({ ok: row.ok, acquired: row.acquired, code: row.code }))
    assert.equal(results.filter((row) => row.ok && row.acquired).length, 1,
      `acquisition verdicts: ${JSON.stringify(verdicts)}`)
    assert.equal(results.filter((row) => !row.ok && row.code === 'request-reservation-active').length, 7,
      `acquisition verdicts: ${JSON.stringify(verdicts)}`)
    assert.deepEqual(readdirSync(reservationsDir).filter((name) => name.endsWith('.json')), [stem + '.json'])
    const active = requests.inspectRequestReservation(stem)
    assert.equal(active.status, 'active')
    assert.equal(requests.releaseRequestReservation(active.record), true)
  })

  await check('two independent HTTP Site processes cannot publish two ids for one stem', async () => {
    let firstServer, secondServer
    try {
      ;[firstServer, secondServer] = await Promise.all([startHttpServer(), startHttpServer()])
      const firstState = await (await fetch(firstServer.base + '/api/state')).json()
      const secondState = await (await fetch(secondServer.base + '/api/state')).json()
      const setup = await fetch(firstServer.base + '/api/state-patch', {
        method: 'POST',
        headers: {
          'content-type': 'application/json',
          'x-orchestrator-csrf': firstState.csrfToken,
          origin: firstServer.base,
        },
        body: JSON.stringify({
          manualSteps: {
            'setup:requirementsVerified': true,
            'setup:yamlPasted': true,
            'setup:agentsInstalled': true,
          },
        }),
      })
      assert.equal(setup.status, 200)
      assert.equal((await setup.json()).progress.setupDone, true)
      const [firstIntent, secondIntent] = await Promise.all([
        typedPrepare(firstServer, firstState.csrfToken, 'cross-process-prepare-a'),
        typedPrepare(secondServer, secondState.csrfToken, 'cross-process-prepare-b'),
      ])
      const responses = await Promise.all([
        fetch(firstIntent.url, firstIntent.options),
        fetch(secondIntent.url, secondIntent.options),
      ])
      const bodies = await Promise.all(responses.map((response) => response.json()))
      assert.equal(responses.every((response) => [200, 409].includes(response.status)), true,
        JSON.stringify({ statuses: responses.map((response) => response.status), bodies }))
      assert.equal(responses.some((response) => response.status === 200), true)
      const accepted = bodies.find((body) => body.requestId && !body.error)
      assert.ok(accepted && requests.REQUEST_ID_RE.test(accepted.requestId))
      bodies.filter((body) => body.error).forEach((body) => {
        assert.ok(['task-action-active', 'action-stale'].includes(body.error))
      })
      assert.equal(readdirSync(requestsDir).filter((name) => name.endsWith('.json')).length, 1)
      assert.deepEqual(readdirSync(reservationsDir).filter((name) => name.endsWith('.json')), [stem + '.json'])
      const queued = JSON.parse(readFileSync(join(requestsDir, accepted.requestId + '.json'), 'utf8'))
      assert.match(queued.prompt, /task-prep|Task Preparation|prepare/i)
      assert.doesNotMatch(queued.prompt, /HTTP process [AB] answers/)
      assert.deepEqual(requests.cancelQueuedRequest(accepted.requestId), { ok: true, removed: true })
    } finally {
      await Promise.all([stopHttpServer(firstServer), stopHttpServer(secondServer)])
    }
  })

  await check('queue cancellation wins ownership no-clobber and releases only its exact reservation', () => {
    const id = '1700000000200-cancel'
    const acquired = requests.acquireRequestReservation(id, base)
    assert.equal(acquired.ok, true)
    assert.equal(requests.writeRequestFile(id, base), true)
    const canceled = requests.cancelQueuedRequest(id)
    assert.deepEqual(canceled, { ok: true, removed: true })
    assert.equal(existsSync(join(requestsDir, id + '.json')), false)
    assert.equal(requests.inspectRequestReservation(stem).status, 'missing')
  })

  await check('cancel loses cleanly after a no-clobber runner claim and keeps its reservation', () => {
    const id = '1700000000201-claimed'
    const acquired = requests.acquireRequestReservation(id, base)
    assert.equal(acquired.ok, true)
    assert.equal(requests.writeRequestFile(id, base), true)
    const claim = join(requestsDir, '.' + id + '.claim')
    assert.equal(requests.transferFileNoClobber(join(requestsDir, id + '.json'), claim), true)
    assert.deepEqual(requests.cancelQueuedRequest(id), { ok: true, removed: false })
    assert.equal(existsSync(claim), true)
    assert.equal(requests.inspectRequestReservation(stem).status, 'active')
    assert.equal(requests.releaseRequestReservation(acquired.handle), true)
    unlinkSync(claim)
  })

  await check('malformed cancellation bytes retain private evidence and never guess a reservation', () => {
    const id = '17000000002015-malformed'
    const acquired = requests.acquireRequestReservation(id, base)
    assert.equal(acquired.ok, true)
    writeFileSync(join(requestsDir, id + '.json'), '{not-json\n')
    const canceled = requests.cancelQueuedRequest(id)
    assert.equal(canceled.ok, false)
    assert.equal(canceled.code, 'cancel-record-unsafe-private-claim-retained')
    const privateName = readdirSync(requestsDir).find((name) => name.startsWith('.' + id + '-') && name.endsWith('.cancel'))
    assert.ok(privateName)
    assert.equal(readFileSync(join(requestsDir, privateName), 'utf8'), '{not-json\n')
    assert.equal(requests.inspectRequestReservation(stem).status, 'active')
    assert.equal(requests.releaseRequestReservation(acquired.handle), true)
    unlinkSync(join(requestsDir, privateName))
  })

  await check('no-clobber transfer preserves both owners when the destination already exists', () => {
    const source = join(requestsDir, 'source.fixture')
    const target = join(runsDir, 'target.fixture')
    writeFileSync(source, 'source')
    writeFileSync(target, 'target')
    assert.equal(requests.transferFileNoClobber(source, target), false)
    assert.equal(readFileSync(source, 'utf8'), 'source')
    assert.equal(readFileSync(target, 'utf8'), 'target')
  })

  await check('interrupted link-before-unlink claim stays fail-closed and cannot be claimed twice', () => {
    const source = join(requestsDir, '1700000000202-crash.json')
    const target = join(requestsDir, '.1700000000202-crash.claim')
    writeFileSync(source, 'crash-window')
    linkSync(source, target) // exact on-disk state after process death between link and unlink
    assert.equal(lstatSync(source).nlink, 2)
    assert.equal(requests.transferFileNoClobber(source, target), false)
    assert.equal(readFileSync(source, 'utf8'), 'crash-window')
    assert.equal(readFileSync(target, 'utf8'), 'crash-window')
    assert.equal(requests.scanRequests().code, 'request-record-unsafe')
    assert.equal(requests.scanActiveClaims().code, 'claim-record-unsafe')
    unlinkSync(target)
    unlinkSync(source)
  })

  await check('directory fsync fallback is Windows-only and limited to unsupported-handle errors', () => {
    assert.equal(requests.directoryFsyncUnavailable({ code: 'EPERM' }, 'win32'), true)
    assert.equal(requests.directoryFsyncUnavailable({ code: 'EISDIR' }, 'win32'), true)
    assert.equal(requests.directoryFsyncUnavailable({ code: 'EPERM' }, 'linux'), false)
    assert.equal(requests.directoryFsyncUnavailable({ code: 'EIO' }, 'win32'), false)
    assert.equal(requests.supersededModeSafe({ isFile: () => true, mode: 0o100666 }, 'win32'), true)
    assert.equal(requests.supersededModeSafe({ isFile: () => true, mode: 0o100666 }, 'linux'), false)
    assert.equal(requests.supersededModeSafe({ isFile: () => true, mode: 0o100600 }, 'linux'), true)
  })

  await check('oversized queue/claim scans fail closed and HTTP publishes nothing', async () => {
    for (let i = 0; i <= requests.ADMISSION_RECORDS_MAX; i++) {
      writeFileSync(join(requestsDir, `${1800000000000 + i}-flood${i}.json`), '')
    }
    const queueScan = requests.scanRequests()
    assert.equal(queueScan.ok, false)
    assert.equal(queueScan.code, 'request-record-limit')
    const beforeNames = readdirSync(requestsDir).sort()
    let server
    try {
      server = await startHttpServer()
      const response = await fetch(server.base + '/api/tasks/' + stem + '/summary')
      assert.ok([200, 409].includes(response.status))
      const refusal = await response.json()
      if (response.status === 200) {
        assert.equal(refusal.task.primaryAction.kind, 'resolve-blocker')
        assert.equal(refusal.task.primaryAction.behavior, 'open-details')
      } else {
        assert.equal(refusal.error, 'task-integrity')
      }
      assert.deepEqual(readdirSync(requestsDir).sort(), beforeNames)
      assert.equal(requests.inspectRequestReservation(stem).status, 'missing')
    } finally { await stopHttpServer(server) }
    rmSync(requestsDir, { recursive: true, force: true })
    mkdirSync(requestsDir, { recursive: true })

    for (let i = 0; i <= requests.ADMISSION_RECORDS_MAX; i++) {
      writeFileSync(join(runsDir, `.${1900000000000 + i}-claim${i}.claim`), '')
    }
    const claimScan = requests.scanActiveClaims()
    assert.equal(claimScan.ok, false)
    assert.equal(claimScan.code, 'claim-record-limit')
    assert.equal(readdirSync(runsDir).filter((name) => /^\.\d+-claim\d+\.claim$/.test(name)).length, requests.ADMISSION_RECORDS_MAX + 1)
    rmSync(runsDir, { recursive: true, force: true })
    mkdirSync(runsDir, { recursive: true })
  })

  if (process.platform !== 'win32') {
    await check('a final-component reservation symlink is unsafe, never treated as missing', () => {
      const outside = join(root, 'outside-reservation.json')
      const link = join(reservationsDir, stem + '.json')
      writeFileSync(outside, '{}\n')
      symlinkSync(outside, link)
      try {
        assert.equal(requests.inspectRequestReservation(stem).status, 'unsafe')
        const acquired = requests.acquireRequestReservation('1700000000299-leaflink', base)
        assert.equal(acquired.ok, false)
        assert.equal(acquired.code, 'request-reservation-unsafe')
        assert.equal(readFileSync(outside, 'utf8'), '{}\n')
      } finally { unlinkSync(link) }
    })

    await check('a symlinked reservation ancestor below project root fails closed', () => {
      const unsafeRoot = mkdtempSync(join(tmpdir(), 'request-reservation-symlink-'))
      try {
        const real = join(unsafeRoot, 'orchestrator', 'real-cache')
        const link = join(unsafeRoot, 'orchestrator', 'cache-link')
        mkdirSync(real, { recursive: true })
        symlinkSync(real, link, 'dir')
        const record = { ...base, projectRoot: unsafeRoot }
        const source = [
          'const mod=require(process.argv[1]);',
          'const record=JSON.parse(Buffer.from(process.argv[2],"base64").toString("utf8"));',
          'process.stdout.write(JSON.stringify(mod.acquireRequestReservation("1700000000300-unsafe",record)));'
        ].join('')
        const result = spawnSync(process.execPath, ['-e', source, modulePath, Buffer.from(JSON.stringify(record)).toString('base64')], {
          env: {
            ...process.env,
            ORCHESTRATOR_PROJECT_ROOT: unsafeRoot,
            ORCHESTRATOR_REQUESTS_DIR: join(unsafeRoot, 'orchestrator', 'requests'),
            ORCHESTRATOR_REQUEST_RESERVATIONS_DIR: join(link, 'request-reservations'),
            ORCHESTRATOR_RUNS_DIR: join(unsafeRoot, 'orchestrator', 'runs')
          },
          encoding: 'utf8'
        })
        assert.equal(result.status, 0, result.stderr)
        const outcome = JSON.parse(result.stdout)
        assert.equal(outcome.ok, false)
        assert.equal(outcome.code, 'request-reservation-unsafe')
        assert.equal(existsSync(join(real, 'request-reservations')), false)
      } finally { rmSync(unsafeRoot, { recursive: true, force: true }) }
    })
  }

  console.log(`request-reservations: ${checks} checks passed`)
} finally {
  rmSync(root, { recursive: true, force: true })
  rmSync(scratch, { recursive: true, force: true })
}
