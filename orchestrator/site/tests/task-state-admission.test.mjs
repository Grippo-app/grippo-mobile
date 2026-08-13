#!/usr/bin/env node

import assert from 'node:assert/strict'
import { EventEmitter } from 'node:events'
import { createServer } from 'node:http'
import { chmodSync, existsSync, lstatSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, renameSync, rmSync, symlinkSync, writeFileSync } from 'node:fs'
import { hostname, tmpdir } from 'node:os'
import { join } from 'node:path'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const taskSource = require('../../tasks/task-source-contract.cjs')
const SOURCE_BLOCK = taskSource.render(taskSource.manualForIntent('task-state-admission', 'manual', 'fixture:task-state-admission'))
const root = mkdtempSync(join(tmpdir(), 'task-state-admission-'))
const scratch = mkdtempSync(join(tmpdir(), 'task-state-admission-scratch-'))
if (process.platform !== 'win32') chmodSync(scratch, 0o700)
const tasks = join(root, 'orchestrator', 'tasks')
const cache = join(root, 'orchestrator', '.cache', 'tasks')
for (const column of ['backlog', 'pending', 'todo', 'done']) mkdirSync(join(tasks, column), { recursive: true })
for (const dir of ['locks', 'requests', 'request-reservations', 'runs', 'superseded', 'finalizations', 'creations', 'edits', 'intake']) {
  mkdirSync(join(cache, dir), { recursive: true })
}
if (process.platform !== 'win32') chmodSync(join(cache, 'intake'), 0o700)
// This fixture exercises the enabled Figma session rail explicitly. Never
// inherit the caller product's permanent feature-gate choice.
writeFileSync(join(root, 'orchestrator', 'project-config.md'), [
  '---',
  'figmaEnabled: true',
  '---',
  '',
].join('\n'))

const stem = 'TASK_1_queue_fencing'
const taskFile = join(tasks, 'backlog', stem + '.md')
writeFileSync(taskFile, '# TASK 1 — Queue fencing\n\n' + SOURCE_BLOCK + '\n\n## Goal\nFence stale work.\n')
const dependentStem = 'TASK_2_queue_dependent'
writeFileSync(join(tasks, 'backlog', dependentStem + '.md'), [
  '# TASK 2 — Queue dependent', '', SOURCE_BLOCK, '', '## Goal', 'Exercise exact drop impact.', '',
  '## Depends on', '- ' + stem, ''
].join('\n'))

const core = require('../../tasks/task-state-core.cjs')
const writerLeases = require('../../tasks/writer-leases.cjs')
const initial = core.validateTaskState({ tasksDir: tasks, repoRoot: root, checkIndex: false })
assert.equal(initial.ok, true)
writeFileSync(join(tasks, 'INDEX.json'), JSON.stringify(core.deriveIndex(initial._model, '2026-07-13T00:00:00Z'), null, 2) + '\n')

process.env.ORCHESTRATOR_PROJECT_ROOT = root
process.env.ORCHESTRATOR_TASKS_DIR = tasks
process.env.ORCHESTRATOR_LOCKS_DIR = join(cache, 'locks')
process.env.ORCHESTRATOR_REQUESTS_DIR = join(cache, 'requests')
process.env.ORCHESTRATOR_REQUEST_RESERVATIONS_DIR = join(cache, 'request-reservations')
process.env.ORCHESTRATOR_RUNS_DIR = join(cache, 'runs')
process.env.ORCHESTRATOR_SUPERSEDED_DIR = join(cache, 'superseded')
process.env.ORCHESTRATOR_FINALIZATIONS_DIR = join(cache, 'finalizations')
process.env.ORCHESTRATOR_TASK_CREATIONS_DIR = join(cache, 'creations')
process.env.ORCHESTRATOR_TASK_EDITS_DIR = join(cache, 'edits')
process.env.ORCHESTRATOR_TASK_INTAKE_DIR = join(cache, 'intake')
process.env.SHALLOW_INTAKE_SCRATCH_DIR = scratch
process.env.ORCHESTRATOR_STATE_FILE = join(root, 'orchestrator', '.cache', 'site', 'state.json')
process.env.RUNNER_DISABLED = '1'

const httpMod = require('../server/http.js')
const runner = require('../server/runner.js')
const requests = require('../server/requests.js')
const locks = require('../server/locks.js')
const taskIntegrity = require('../server/task-integrity.js')
const sessions = require('../server/sessions.js')
const cli = require('../server/cli.js')
cli.status = () => ({ installed: true, loggedIn: true, authProblem: null })
const server = createServer(httpMod.handle)
// The retention case performs more than one hundred synchronous guarded
// publications without touching HTTP. Keep the fixture connection alive for
// the whole bounded test so undici cannot race the server's 5 s idle reap when
// the final integrity request resumes.
server.keepAliveTimeout = 60_000
await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve))
const base = `http://127.0.0.1:${server.address().port}`
let checks = 0
async function check(name, fn) { await fn(); checks++; console.log(`ok ${checks} - ${name}`) }

function tombstoneFor(id, patch = {}) {
  return {
    version: 1,
    status: 'superseded',
    requestId: id,
    action: 'prep',
    stem,
    reason: 'task-integrity-invalid',
    expectedState: 'backlog',
    observedState: 'backlog',
    expectedSourceRevision: 'sha256:' + 'a'.repeat(64),
    observedSourceRevision: 'sha256:' + 'b'.repeat(64),
    admittedAt: '2026-07-13T00:00:00.000Z',
    supersededAt: '2026-07-13T00:00:01.000Z',
    snapshotHash: 'sha256:' + 'c'.repeat(64),
    findings: [{ code: 'TASK_FIXTURE', severity: 'error', paths: ['orchestrator/tasks/backlog/' + stem + '.md'] }],
    ...patch
  }
}

try {
  const state = await (await fetch(base + '/api/state')).json()
  const csrf = state.csrfToken
  const headers = { 'content-type': 'application/json', 'x-orchestrator-csrf': csrf, origin: base }
  const setupResponse = await fetch(base + '/api/state-patch', {
    method: 'POST',
    headers,
    body: JSON.stringify({
      manualSteps: {
        'setup:requirementsVerified': true,
        'setup:yamlPasted': true,
        'setup:agentsInstalled': true,
      },
    }),
  })
  assert.equal(setupResponse.status, 200)
  assert.equal((await setupResponse.json()).progress.setupDone, true)
  const actionCache = new Map()
  const initialSummary = await (await fetch(base + '/api/tasks/' + stem + '/summary')).json()
  assert.equal(initialSummary.task.primaryAction.kind, 'prepare')
  actionCache.set('prep', initialSummary.task.primaryAction)
  const enqueue = async (requestedAction, intent = requestedAction + '-fixture') => {
    const summaryResponse = await fetch(base + '/api/tasks/' + stem + '/summary')
    let action = null
    if (summaryResponse.ok) {
      const summary = await summaryResponse.json()
      const actions = [summary.task.primaryAction].concat(summary.task.secondaryActions || [])
      action = requestedAction === 'prep'
        ? actions.find((candidate) => candidate.kind === 'prepare')
        : actions.find((candidate) => candidate.kind === requestedAction)
      if (!action && requestedAction === 'answers') action = summary.task.primaryAction
      const prepare = actions.find((candidate) => candidate.kind === 'prepare')
      if (prepare) actionCache.set('prep', prepare)
      if (action) actionCache.set(requestedAction, action)
    }
    action ||= actionCache.get(requestedAction) || actionCache.get('prep')
    assert.ok(action, 'typed action fixture is unavailable for ' + requestedAction)
    let confirmation = null
    if (requestedAction === 'drop') {
      const impactResponse = await fetch(base + '/api/tasks/drop-impact?stem=' + encodeURIComponent(stem))
      if (impactResponse.ok) {
        const impact = await impactResponse.json()
        confirmation = {
          sourceRevision: impact.sourceRevision,
          impactHash: impact.impactHash,
          dependents: impact.dependents,
        }
      }
    }
    const submitAnswers = requestedAction === 'answers'
    const body = {
      stem,
      actionId: action.id,
      actionRevision: action.actionRevision,
      action: submitAnswers ? 'submit-answers' : action.kind,
      expectedState: action.expectedState,
      expectedSourceRevision: action.expectedSourceRevision,
      checkpointId: action.checkpointId || null,
      confirmation,
      confirmationToken: null,
      answers: submitAnswers ? [{ questionId: 1, optionIds: [], text: 'fixture' }] : null,
      questionRound: submitAnswers ? 1 : null,
      expectedQuestionsRevision: submitAnswers ? 'sha256:' + 'f'.repeat(64) : null,
      liveSessionId: null,
      expectedSessionRevision: null,
      idempotencyKey: 'admission-' + requestedAction + '-' +
        Buffer.from(intent).toString('hex').slice(0, 48),
    }
    return fetch(base + '/api/tasks/actions', {
      method: 'POST', headers, body: JSON.stringify(body),
    })
  }

  await check('read-only integrity endpoint returns a fresh bounded canonical snapshot', async () => {
    const response = await fetch(base + '/api/tasks/integrity')
    assert.equal(response.status, 200)
    const body = await response.json()
    assert.equal(body.ok, true)
    assert.equal(body.indexStatus, 'fresh')
    assert.equal(body.truncated, false)
    assert.equal(Object.hasOwn(body, '_model'), false)
  })

  await check('public integrity findings never expose parser-only task excerpts', async () => {
    const exposed = taskIntegrity.publicResult({
      version: 1, ok: false, scope: stem, affectedStems: [stem], findings: [{
        code: 'TODO_AUTOMATION_ANCHOR_MISSING', severity: 'error', stem,
        paths: ['orchestrator/tasks/todo/' + stem + '.md'],
        message: 'generic finding', recovery: 'generic recovery',
        details: { bullet: 'SECRET TASK BODY EXCERPT', answers: ['SECRET ANSWER'] }
      }]
    })
    assert.equal(Object.hasOwn(exposed.findings[0], 'details'), false)
    assert.doesNotMatch(JSON.stringify(exposed), /SECRET/)
  })

  await check('drop impact is complete, revision-bound, deterministic, and read-only', async () => {
    const before = readFileSync(taskFile)
    const response = await fetch(base + '/api/tasks/drop-impact?stem=' + encodeURIComponent(stem))
    assert.equal(response.status, 200)
    const body = await response.json()
    assert.equal(body.ok, true)
    assert.equal(body.operation, 'inspect-drop')
    assert.equal(body.state, 'backlog')
    assert.deepEqual(body.dependents, [dependentStem])
    assert.match(body.sourceRevision, /^sha256:[a-f0-9]{64}$/)
    assert.equal(body.impactHash, core.dropImpactHash(stem, body.sourceRevision, body.dependents))
    assert.deepEqual(readFileSync(taskFile), before)
    const invalid = await fetch(base + '/api/tasks/drop-impact?stem=../escape')
    assert.equal(invalid.status, 400)
  })

  await check('any task-lock generation blocks Drop inspection and queue admission without deletion', async () => {
    const lockPath = join(cache, 'locks', stem + '.json')
    const lockBytes = Buffer.from('{"stage":"task-prep","startedAt":"2026-07-13T00:00:00Z"}\n')
    writeFileSync(lockPath, lockBytes)
    const impact = await fetch(base + '/api/tasks/drop-impact?stem=' + encodeURIComponent(stem))
    assert.equal(impact.status, 409)
    assert.equal((await impact.json()).error, 'task-lock-present')
    const queued = await enqueue('drop')
    assert.equal(queued.status, 409)
    assert.equal((await queued.json()).error, 'action-disabled')
    assert.deepEqual(readFileSync(lockPath), lockBytes)
    rmSync(lockPath)
  })

  await check('state projection never follows or reads an oversized task lock', async () => {
    const lockPath = join(cache, 'locks', stem + '.json')
    writeFileSync(lockPath, 'x'.repeat(32 * 1024 + 1))
    let state = await (await fetch(base + '/api/state')).json()
    assert.equal(state.progress.inProgress.find((item) => item.stem === stem)?.stage, 'unknown')
    assert.equal(taskIntegrity.validateAll().findings.some((item) => item.code === 'LOCK_UNSAFE'), true)
    rmSync(lockPath)

    if (process.platform !== 'win32') {
      const outside = join(root, 'outside-lock.json')
      writeFileSync(outside, JSON.stringify({ stage: 'task-prep', startedAt: '2026-07-13T00:00:00Z' }))
      symlinkSync(outside, lockPath)
      state = await (await fetch(base + '/api/state')).json()
      assert.equal(state.progress.inProgress.find((item) => item.stem === stem)?.stage, 'unknown')
      assert.equal(taskIntegrity.validateAll().findings.some((item) => item.code === 'LOCK_UNSAFE'), true)
      rmSync(lockPath)
    }
  })

  await check('lock projection trusts only the canonical v1 schema', async () => {
    const lockPath = join(cache, 'locks', stem + '.json')
    const sessionId = 'ws-' + 'a'.repeat(32)
    const startedAt = '2026-07-13T12:00:00.000Z'
    const canonical = {
      version: 1,
      stem,
      stage: 'task-prep',
      runId: 'run_' + 'b'.repeat(32),
      sessionId,
      startedAt,
      owner: {
        kind: 'site', id: 'site:' + sessionId, pid: process.pid,
        processStartId: writerLeases.captureProcessStartId(process.pid), hostname: hostname(), startedAt,
      },
    }
    writeFileSync(lockPath, JSON.stringify(canonical) + '\n')
    let row = locks.readLocksResult().rows.find((item) => item.stem === stem)
    assert.equal(row.stage, 'task-prep')
    assert.equal(row.startedAt, startedAt)

    writeFileSync(lockPath, JSON.stringify({ ...canonical, ignoredAuthority: true }) + '\n')
    row = locks.readLocksResult().rows.find((item) => item.stem === stem)
    assert.equal(row.stage, 'unknown')
    assert.equal(row.startedAt, null)

    writeFileSync(lockPath, JSON.stringify({ stage: 'orchestrator', startedAt: '2026-07-13T12:00:00Z' }) + '\n')
    assert.equal(locks.readLocksResult().rows.find((row) => row.stem === stem).stage, 'unknown')

    writeFileSync(lockPath, '{broken\n')
    row = locks.readLocksResult().rows.find((item) => item.stem === stem)
    assert.equal(row.stage, 'unknown')
    assert.equal(row.startedAt, null)

    writeFileSync(lockPath, 'null\n')
    row = locks.readLocksResult().rows.find((item) => item.stem === stem)
    assert.equal(row.stage, 'unknown', 'valid JSON primitives must remain visibly locked')
    assert.equal(row.startedAt, null)
    rmSync(lockPath)
  })

  await check('task answer ownership accepts only an exact canonical lock session generation', async () => {
    const lockPath = join(cache, 'locks', stem + '.json')
    const sessionId = 'ws-' + 'a'.repeat(32)
    const startedAt = '2026-07-13T12:00:00.000Z'
    writeFileSync(lockPath, JSON.stringify({
      version: 1,
      stem,
      stage: 'task-prep',
      runId: 'run_' + 'b'.repeat(32),
      sessionId,
      startedAt,
      owner: {
        kind: 'site',
        id: 'site:' + sessionId,
        pid: process.pid,
        processStartId: writerLeases.captureProcessStartId(process.pid),
        hostname: hostname(),
        startedAt
      }
    }, null, 2) + '\n')
    assert.equal(locks.lockOwnedBySession(stem, sessionId).owned, true)
    assert.equal(locks.lockOwnedBySession(stem, 'ws-' + 'c'.repeat(32)).owned, false)
    writeFileSync(lockPath, '{"stage":"task-prep","startedAt":"2026-07-13T12:00:00Z"}\n')
    assert.equal(locks.lockOwnedBySession(stem, sessionId).owned, false)
    rmSync(lockPath)
  })

  await check('malformed pre-claim v2 bytes cannot influence queue dedup state', async () => {
    const invalidId = '1000000000000-invalid'
    const invalidPath = join(cache, 'requests', invalidId + '.json')
    const snapshot = core.validateTaskState({ tasksDir: tasks, repoRoot: root, stem, checkIndex: true })
    writeFileSync(invalidPath, JSON.stringify({
      version: 2, action: 'prep', stem, expectedState: 'backlog', sourceRevision: snapshot.sourceRevision,
      dedupKey: null, dedupReport: null, projectRoot: root, prompt: 'malformed extra-field fixture',
      createdAt: '2026-07-13T00:00:00.000Z', extra: true
    }))
    assert.deepEqual(requests.scanRequests(), {
      ok: false,
      code: 'request-record-invalid',
      rows: [],
    })
    rmSync(invalidPath)
  })

  await check('request publication is no-clobber for an already claimed id', async () => {
    const id = '1000000000001-noclobber'
    const snapshot = core.validateTaskState({ tasksDir: tasks, repoRoot: root, stem, checkIndex: true })
    const record = {
      version: 2, action: 'prep', stem, expectedState: 'backlog', sourceRevision: snapshot.sourceRevision,
      dedupKey: null, dedupReport: null, projectRoot: root, prompt: 'first complete request',
      createdAt: '2026-07-13T00:00:00.000Z'
    }
    assert.equal(requests.writeRequestFile(id, record), true)
    const target = join(cache, 'requests', id + '.json')
    const firstBytes = readFileSync(target)
    assert.equal(requests.writeRequestFile(id, { ...record, prompt: 'foreign replacement' }), false)
    assert.deepEqual(readFileSync(target), firstBytes)
    rmSync(target)
  })

  await check('wrong-column action is rejected as stale task state before queueing', async () => {
    const response = await enqueue('answers')
    assert.equal(response.status, 409)
    const body = await response.json()
    assert.equal(body.error, 'action-stale')
    assert.deepEqual(readdirSync(join(cache, 'requests')).filter((name) => name.endsWith('.json')), [])
  })

  await check('direct task start stays forbidden while terminal follow-up uses the guarded send rail', async () => {
    let response = await fetch(base + '/api/session/start', {
      method: 'POST', headers, body: JSON.stringify({ key: 'task:' + stem, prompt: 'bypass admission' })
    })
    assert.equal(response.status, 400)
    assert.equal((await response.json()).error, 'task-session-start-forbidden')

    const sessions = require('../server/sessions.js')
    const originalSendOrResume = sessions.sendOrResume
    try {
      sessions.sendOrResume = function () { return { sent: true, resumed: true, queued: false } }
      response = await fetch(base + '/api/session/send', {
        method: 'POST', headers, body: JSON.stringify({ key: 'task:' + stem, text: 'explain the completed run' })
      })
      assert.equal(response.status, 200)
      const body = await response.json()
      assert.equal(body.sent, true)
      assert.equal(body.resumed, true)
    } finally {
      sessions.sendOrResume = originalSendOrResume
    }
  })

  await check('a paused Figma session accepts free text without invoking Figma action admission', async () => {
    const figma = require('../server/figma.js')
    const sessions = require('../server/sessions.js')
    const originalAdmission = figma.sessionAdmission
    const originalStatus = sessions.status
    const originalSendOrResume = sessions.sendOrResume
    let admitted = false
    let sent = false
    try {
      figma.sessionAdmission = function () { admitted = true; return null }
      sessions.status = function () { return { running: true, askedThisTurn: true, awaitingTurn: false } }
      sessions.sendOrResume = function () { sent = true; return { sent: true } }
      const response = await fetch(base + '/api/session/send', {
        method: 'POST', headers, body: JSON.stringify({ key: 'figma:sync-components', text: 'continue the paused export' })
      })
      assert.equal(response.status, 200)
      assert.equal((await response.json()).sent, true)
      assert.equal(admitted, false)
      assert.equal(sent, true)
    } finally {
      figma.sessionAdmission = originalAdmission
      sessions.status = originalStatus
      sessions.sendOrResume = originalSendOrResume
    }
  })

  await check('an unknown Figma action key is rejected before session admission', async () => {
    const response = await fetch(base + '/api/session/start', {
      method: 'POST', headers, body: JSON.stringify({ key: 'figma:overwrite', prompt: 'arbitrary workspace writer' })
    })
    assert.equal(response.status, 400)
    assert.equal((await response.json()).error, 'bad-key')
  })

  await check('a refused generic session start is a typed HTTP failure without raw runtime detail', async () => {
    const originalStart = sessions.start
    sessions.start = function () {
      return { running: false, error: 'sensitive spawn detail from the local runtime' }
    }
    try {
      const response = await fetch(base + '/api/session/start', {
        method: 'POST', headers, body: JSON.stringify({ key: 'setup', prompt: 'fixture' })
      })
      assert.equal(response.status, 409)
      assert.deepEqual(await response.json(), { error: 'session-start-refused' })
    } finally {
      sessions.start = originalStart
    }
  })

  await check('a refused Figma action start is localized by code and never returns raw session status', async () => {
    const figma = require('../server/figma.js')
    const figmaSync = require('../server/figma-sync.js')
    const figmaTestJob = require('../server/figma-test-job.js')
    const figmaSessionActions = require('../server/figma-session-actions.js')
    const originals = {
      admission: figma.sessionAdmission,
      recoveryState: figmaSync.recoveryState,
      syncBusy: figmaSync.busy,
      testBusy: figmaTestJob.busy,
      resolveAction: figmaSessionActions.resolveAction,
      status: sessions.status,
      start: sessions.start,
    }
    try {
      figma.sessionAdmission = function () { return null }
      figmaSync.recoveryState = function () { return 'ready' }
      figmaSync.busy = function () { return false }
      figmaTestJob.busy = function () { return false }
      figmaSessionActions.resolveAction = function () {
        return Promise.resolve({ ok: true, action: 'screen-drift', prompt: 'server-owned fixture prompt' })
      }
      sessions.status = function () { return { running: false } }
      sessions.start = function () {
        return { running: false, error: 'sensitive spawn detail from the local runtime' }
      }
      const response = await fetch(base + '/api/session/start', {
        method: 'POST',
        headers,
        body: JSON.stringify({ key: 'figma:screens:' + stem, figmaAction: 'screen-drift' })
      })
      assert.equal(response.status, 409)
      assert.deepEqual(await response.json(), { error: 'session-start-refused' })
    } finally {
      figma.sessionAdmission = originals.admission
      figmaSync.recoveryState = originals.recoveryState
      figmaSync.busy = originals.syncBusy
      figmaTestJob.busy = originals.testBusy
      figmaSessionActions.resolveAction = originals.resolveAction
      sessions.status = originals.status
      sessions.start = originals.start
    }
  })

  await check('final admission fence rejects a lock created after the initial snapshot without leaks', async () => {
    const originalAcquire = requests.acquireRequestReservation
    const lockPath = join(cache, 'locks', stem + '.json')
    requests.acquireRequestReservation = function (...args) {
      const result = originalAcquire(...args)
      if (result.ok) writeFileSync(lockPath, '{}\n')
      return result
    }
    try {
      const response = await enqueue('prep', 'lock-race fixture')
      assert.equal(response.status, 409)
      assert.equal((await response.json()).error, 'task-lock-present')
      assert.deepEqual(readdirSync(join(cache, 'requests')).filter((name) => name.endsWith('.json')), [])
      assert.equal(requests.inspectRequestReservation(stem).status, 'missing')
    } finally {
      requests.acquireRequestReservation = originalAcquire
      rmSync(lockPath, { force: true })
    }
  })

  await check('final admission fence rejects source drift after reservation without leaks', async () => {
    const originalAcquire = requests.acquireRequestReservation
    const originalTask = readFileSync(taskFile)
    requests.acquireRequestReservation = function (...args) {
      const result = originalAcquire(...args)
      if (result.ok) writeFileSync(taskFile, Buffer.concat([originalTask, Buffer.from('\nManual edit during admission.\n')]))
      return result
    }
    try {
      const response = await enqueue('prep', 'source-race fixture')
      assert.equal(response.status, 409)
      assert.ok(['stale-task-state', 'task-integrity'].includes((await response.json()).error))
      assert.deepEqual(readdirSync(join(cache, 'requests')).filter((name) => name.endsWith('.json')), [])
      assert.equal(requests.inspectRequestReservation(stem).status, 'missing')
    } finally {
      requests.acquireRequestReservation = originalAcquire
      writeFileSync(taskFile, originalTask)
      const restored = core.validateTaskState({ tasksDir: tasks, repoRoot: root, checkIndex: false })
      assert.equal(restored.ok, true)
      writeFileSync(join(tasks, 'INDEX.json'), JSON.stringify(core.deriveIndex(restored._model, '2026-07-13T00:00:00Z'), null, 2) + '\n')
    }
  })

  let requestId
  let requestRecord
  await check('admission writes an exact server-stamped v2 state/revision record', async () => {
    const response = await enqueue('prep')
    const responseBody = await response.json()
    assert.equal(response.status, 200, JSON.stringify(responseBody))
    requestId = responseBody.requestId
    requestRecord = JSON.parse(readFileSync(join(cache, 'requests', requestId + '.json'), 'utf8'))
    assert.deepEqual(Object.keys(requestRecord).sort(), [
      'action', 'createdAt', 'dedupKey', 'dedupReport', 'expectedState',
      'projectRoot', 'prompt', 'sourceRevision', 'stem', 'version'
    ])
    assert.equal(requestRecord.version, 2)
    assert.equal(requestRecord.expectedState, 'backlog')
    assert.match(requestRecord.sourceRevision, /^sha256:[a-f0-9]{64}$/)
    assert.equal(runner.claimedRequestIssue(requestRecord, root), null)
  })

  await check('execution fence treats a post-admission lock race as retryable ownership', async () => {
    const lockPath = join(cache, 'locks', stem + '.json')
    writeFileSync(lockPath, '{}\n')
    try {
      const fenced = runner.fenceClaimForExecution(requestId, requestRecord)
      assert.equal(fenced.ok, false)
      assert.equal(fenced.retry, true)
      assert.equal(fenced.error, 'task-lock-present')
      assert.equal(existsSync(join(cache, 'superseded', requestId + '.json')), false)
      assert.equal(existsSync(join(cache, 'requests', requestId + '.json')), true)
      assert.equal(requests.inspectRequestReservation(stem).status, 'active')
    } finally { rmSync(lockPath, { force: true }) }
  })

  await check('retained lock-release recovery artifact requeues without launching a session', async () => {
    const releaseArtifact = join(cache, 'locks', '.' + stem + '.json.release-' + 'a'.repeat(36))
    writeFileSync(releaseArtifact, '{}\n')
    const sessions = require('../server/sessions.js')
    const childProcess = require('node:child_process')
    const originalStart = sessions.start
    const originalSpawn = childProcess.spawn
    const originalSetInterval = global.setInterval
    let starts = 0
    try {
      sessions.start = function () { starts++; return { running: true } }
      childProcess.spawn = function () {
        const child = new EventEmitter()
        child.kill = function () { return true }
        queueMicrotask(() => child.emit('close', 0))
        return child
      }
      global.setInterval = function () { return { unref() {} } }
      delete process.env.RUNNER_DISABLED
      runner.init()
      await new Promise((resolve) => setImmediate(resolve))
      assert.equal(runner.isEnabled(), true)
      runner.tick()
      assert.equal(starts, 0)
      assert.equal(existsSync(join(cache, 'requests', requestId + '.json')), true)
      assert.equal(existsSync(join(cache, 'runs', '.' + requestId + '.claim')), false)
      assert.equal(existsSync(join(cache, 'superseded', requestId + '.json')), false)
      assert.equal(requests.inspectRequestReservation(stem).status, 'active')
      assert.equal(locks.lockPresence(stem).recovery, true)
    } finally {
      process.env.RUNNER_DISABLED = '1'
      global.setInterval = originalSetInterval
      childProcess.spawn = originalSpawn
      sessions.start = originalStart
      rmSync(releaseArtifact, { force: true })
    }
  })

  await check('typed idempotency replays exact intent while stale or active actions stay fenced', async () => {
    let response = await enqueue('prep')
    assert.equal(response.status, 200)
    assert.equal((await response.json()).idempotentReplay, true)
    assert.equal(readdirSync(join(cache, 'requests')).filter((name) => name.endsWith('.json')).length, 1)
    assert.deepEqual(readdirSync(join(cache, 'request-reservations')).filter((name) => name.endsWith('.json')), [stem + '.json'])

    response = await enqueue('prep', 'different answer payload')
    assert.equal(response.status, 409)
    assert.equal((await response.json()).error, 'action-stale')
    assert.equal(readdirSync(join(cache, 'requests')).filter((name) => name.endsWith('.json')).length, 1)

    response = await enqueue('drop')
    assert.equal(response.status, 409)
    const body = await response.json()
    assert.equal(body.error, 'action-disabled')
  })

  await check('execution revision mismatch becomes a bounded durable tombstone', async () => {
    const privateClaim = join(cache, 'runs', '.' + requestId + '.claim')
    renameSync(join(cache, 'requests', requestId + '.json'), privateClaim)
    const conflict = await enqueue('drop')
    assert.equal(conflict.status, 409)
    // The atomic queue claim is intentionally private, so the summary can no
    // longer project the queued row while the durable reservation still owns
    // the task. Queue admission is the final authority for this transition.
    assert.equal((await conflict.json()).error, 'task-action-active')
    writeFileSync(taskFile, readFileSync(taskFile, 'utf8') + '\nChanged after admission.\n')
    const fenced = runner.fenceClaimForExecution(requestId, requestRecord)
    assert.equal(fenced.ok, false)
    assert.equal(fenced.retry, false)
    assert.equal(fenced.reason, 'source-revision-changed')
    const tombstone = JSON.parse(readFileSync(join(cache, 'superseded', requestId + '.json'), 'utf8'))
    assert.equal(tombstone.status, 'superseded')
    assert.equal(tombstone.reason, 'source-revision-changed')
    assert.equal(tombstone.stem, stem)
    assert.equal(Object.hasOwn(tombstone, 'prompt'), false)
    assert.ok(tombstone.findings.length <= 30)
  })

  await check('superseded ids are immutable and only exact canonical replay is idempotent', async () => {
    const id = '1999999999999-immutable'
    const record = tombstoneFor(id)
    assert.equal(requests.writeSupersededFile(id, record), true)
    const file = join(cache, 'superseded', id + '.json')
    const bytes = readFileSync(file)
    assert.equal(lstatSync(file).mode & 0o777, 0o600)
    assert.equal(requests.writeSupersededFile(id, record), true)
    assert.deepEqual(readFileSync(file), bytes)
    assert.equal(requests.writeSupersededFile(id, { ...record, reason: 'state-changed' }), false)
    assert.deepEqual(readFileSync(file), bytes)
    assert.deepEqual(requests.readSupersededFile(id), record)
  })

  await check('superseded audit retention is deterministic and bounded', async () => {
    for (let i = 0; i < requests.SUPERSEDED_RETAIN + 5; i++) {
      const id = String(1000000000000 + i) + '-retained'
      assert.equal(requests.writeSupersededFile(id, tombstoneFor(id)), true)
    }
    const kept = readdirSync(join(cache, 'superseded')).filter((name) => /^\d+-[a-z0-9]+\.json$/.test(name))
    assert.equal(kept.length, requests.SUPERSEDED_RETAIN)
    assert.equal(kept.includes('1000000000000-retained.json'), false)
  })

  await check('invalid/stale INDEX is surfaced without blocking a healthy task admission', async () => {
    const indexPath = join(tasks, 'INDEX.json')
    const staleIndex = JSON.parse(readFileSync(indexPath, 'utf8'))
    staleIndex.backlog[0].title = 'Stale title'
    writeFileSync(indexPath, JSON.stringify(staleIndex, null, 2) + '\n')
    const integrity = await (await fetch(base + '/api/tasks/integrity')).json()
    assert.equal(integrity.ok, false)
    assert.equal(integrity.indexStatus, 'stale')
    const admission = taskIntegrity.validateAction('prep', stem, 'test')
    assert.equal(admission.indexStatus, 'stale')
    assert.equal(taskIntegrity.actionAdmission(admission).ok, true)
    const execution = runner.inspectClaimForExecution({
      action: 'prep',
      stem,
      expectedState: admission.observedState,
      sourceRevision: admission.sourceRevision,
    })
    assert.equal(execution.ok, true)
  })

  console.log(`task-state-admission: ${checks} checks passed`)
} finally {
  await new Promise((resolve) => server.close(resolve))
  rmSync(root, { recursive: true, force: true })
  rmSync(scratch, { recursive: true, force: true })
}
