#!/usr/bin/env node

import assert from 'node:assert/strict'
import { createServer } from 'node:http'
import { createRequire } from 'node:module'
import { spawnSync } from 'node:child_process'
import { mkdtempSync, mkdirSync, readFileSync, rmSync, symlinkSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { tmpdir } from 'node:os'
import { fileURLToPath } from 'node:url'

const root = mkdtempSync(join(tmpdir(), 'site-finalizations-'))
const dir = join(root, 'finalizations')
const runsDir = join(root, 'runs')
const creationsDir = join(root, 'creations')
const editsDir = join(root, 'edits')
for (const path of [dir, runsDir, creationsDir, editsDir]) mkdirSync(path, { recursive: true })
// Keep every server-derived path inside the fixture. Reading the developer's
// workspace state here previously hid obsolete persisted envelopes behind the
// production reader and made this test depend on unrelated local cache bytes.
process.env.ORCHESTRATOR_PROJECT_ROOT = root
process.env.ORCHESTRATOR_FINALIZATIONS_DIR = dir
process.env.ORCHESTRATOR_RUNS_DIR = runsDir
process.env.ORCHESTRATOR_TASK_CREATIONS_DIR = creationsDir
process.env.ORCHESTRATOR_TASK_EDITS_DIR = editsDir

const stem = 'TASK_9_recover_me'
const phaseNames = ['outcome', 'components', 'tokens', 'ship', 'index', 'arch', 'verify', 'unlock', 'cleanup']
function identityProof(hashByte = '9') {
  return {
    ctimeNs: '4000000000', dev: '1', hash: 'sha256:' + hashByte.repeat(64), ino: '2',
    kind: 'file', mode: 33152, mtimeNs: '3000000000', size: 128,
  }
}
function validMarker(revision = 3, markerStem = stem) {
  const phases = Object.fromEntries(phaseNames.map((phase) => [phase, { state: 'pending', attempts: 0 }]))
  phases.index = { state: 'failed', attempts: 1 }
  return {
    version: 1,
    revision,
    stem: markerStem,
    transactionId: 'fin-test',
    status: 'incomplete',
    phase: 'index',
    createdAt: '2026-01-01T00:00:00.000Z',
    updatedAt: '2026-01-01T00:01:00.000Z',
    owner: null,
    source: {
      originalHash: 'sha256:' + '0'.repeat(64),
      intendedHash: 'sha256:' + 'a'.repeat(64),
      intendedLogicalHash: 'sha256:' + 'b'.repeat(64),
      outcomeHash: 'sha256:' + 'c'.repeat(64),
      snapshotHash: 'sha256:' + 'd'.repeat(64),
      publishFromHash: 'sha256:' + 'e'.repeat(64),
      lock: { present: true, ...identityProof('9') },
      secretInternalValue: 'must-not-leak'
    },
    figma: { enabled: false, configHash: 'sha256:' + 'f'.repeat(64), pipelineRunId: null },
    phases,
    artifacts: {},
    lastError: { code: 'INDEX_REGEN_FAILED', message: 'fixture failure', at: '2026-01-01T00:01:00.000Z', privateTrace: 'must-not-leak' }
  }
}
function clone(value) { return JSON.parse(JSON.stringify(value)) }
function writeMarker(marker) { writeFileSync(join(dir, marker.stem + '.json'), JSON.stringify(marker, null, 2) + '\n') }
writeFileSync(join(dir, stem + '.json'), JSON.stringify(validMarker(), null, 2) + '\n')
writeFileSync(join(dir, 'TASK_10_corrupt.json'), '{broken')
writeFileSync(join(dir, 'TASK_12_oversized.json'), Buffer.alloc(256 * 1024 + 1, 0x20))
const outside = join(root, 'outside.json'); writeFileSync(outside, JSON.stringify(validMarker()))
symlinkSync(outside, join(dir, 'TASK_11_symlink.json'))

const fakeFinalizer = join(root, 'fake-finalizer.mjs')
writeFileSync(fakeFinalizer, `setTimeout(() => process.exit(0), 500);\n`)
process.env.FINALIZE_TASK_SCRIPT = fakeFinalizer

const require = createRequire(import.meta.url)
const finalizations = require('../server/finalizations.js')
const taskStateCore = require('../../tasks/task-state-core.cjs')

let checks = 0
function check(name, fn) {
  return Promise.resolve().then(fn).then(function () { checks++; console.log('PASS ' + name) })
}
async function waitFor(predicate, timeout = 4000) {
  const deadline = Date.now() + timeout
  while (true) {
    if (predicate()) return
    if (Date.now() >= deadline) break
    await new Promise((resolveDelay) => setTimeout(resolveDelay, 25))
  }
  assert.fail('timed out waiting for fixture condition')
}
function processAlive(pid) {
  try { process.kill(pid, 0); return true } catch (error) { return error && error.code === 'EPERM' }
}

try {
  await check('startup scan sees markers that predate module init', function () {
    finalizations.init()
    const list = finalizations.list()
    const good = list.find((x) => x.stem === stem)
    assert.ok(good)
    assert.equal(good.phase, 'index')
    assert.equal(good.errorCode, 'INDEX_REGEN_FAILED')
    assert.equal(JSON.stringify(good).includes('must-not-leak'), false)
    assert.equal(finalizations.mutationBlocked(null), true, 'global mutations must fail closed on any durable marker')
    assert.equal(finalizations.mutationBlocked('TASK_999_unrelated'), true, 'a marker must block unrelated task writers because derived artifacts are shared')
  })

  await check('corrupt and symlink markers are surfaced, not skipped', function () {
    const list = finalizations.list()
    assert.equal(list.find((x) => x.stem === 'TASK_10_corrupt').status, 'corrupt')
    assert.equal(list.find((x) => x.stem === 'TASK_11_symlink').errorCode, 'UNSAFE_MARKER')
    assert.equal(list.find((x) => x.stem === 'TASK_12_oversized').errorCode, 'MARKER_TOO_LARGE')
    assert.equal(finalizations.hasMarker('TASK_11_symlink'), true)
  })

  await check('pre-WAL finalization-marker reservation stays visible and automatically recoverable by its real stem', function () {
    const recoveryStem = 'TASK_24_marker_replace_recovery'
    const reservation = join(dir, `.${recoveryStem}.json.replace-reservation.json`)
    const candidate = join(dir, `.${recoveryStem}.json.replace-candidate-${'a'.repeat(32)}`)
    writeFileSync(reservation, '{"fixture":"durable-reservation"}\n')
    writeFileSync(candidate, 'candidate marker bytes\n')
    const projected = finalizations.readOne(recoveryStem)
    assert.equal(projected.status, 'incomplete')
    assert.equal(projected.phase, 'marker-replace-recovery')
    assert.equal(projected.errorCode, 'FINALIZATION_MARKER_RECOVERY_REQUIRED')
    assert.equal(projected.recoverable, true)
    assert.equal(finalizations.hasMarker(recoveryStem), true)
    const rows = finalizations.list().filter((row) => row.stem === recoveryStem)
    assert.equal(rows.length, 1, 'private replace files must project one real-stem recovery row')
    const integrity = finalizations.scanIntegrity({
      stem: recoveryStem,
      writerLeaseInspection: { active: [], stale: [], issues: [], snapshotInputs: [], truncated: false },
    })
    assert.ok(integrity.findings.some((row) => row.code === 'FINALIZATION_MARKER_RECOVERY_REQUIRED'))
    assert.ok(integrity.snapshotInputs.some((row) => row.kind === 'marker-replace'))
  })

  await check('Site marker schema matches the finalizer v1 owner, lock-proof, phase, Figma, and artifact contract', function () {
    const missingSource = clone(validMarker(1, 'TASK_13_missing_source'))
    delete missingSource.source.originalHash
    writeMarker(missingSource)

    const badPhase = clone(validMarker(1, 'TASK_14_bad_phase'))
    badPhase.phases.verify = { state: 'mystery', attempts: 0 }
    writeMarker(badPhase)

    const badFigma = clone(validMarker(1, 'TASK_15_bad_figma'))
    badFigma.figma = { enabled: true, pipelineRunId: null }
    writeMarker(badFigma)

    const badComponentPair = clone(validMarker(1, 'TASK_16_bad_component_pair'))
    badComponentPair.artifacts.componentMappingHash = 'sha256:' + '9'.repeat(64)
    writeMarker(badComponentPair)

    const badCompleted = clone(validMarker(1, 'TASK_17_bad_completed'))
    badCompleted.status = 'completed'
    writeMarker(badCompleted)

    const badUnlockProof = clone(validMarker(1, 'TASK_18_bad_unlock_proof'))
    badUnlockProof.artifacts.unlockDetached = false
    writeMarker(badUnlockProof)

    const incompleteLockProof = clone(validMarker(1, 'TASK_19_incomplete_lock_proof'))
    incompleteLockProof.source.lock = { present: false, hash: null, dev: null, ino: null }
    writeMarker(incompleteLockProof)

    const ownerWithoutGeneration = clone(validMarker(1, 'TASK_20_owner_without_generation'))
    ownerWithoutGeneration.status = 'running'
    ownerWithoutGeneration.phases.index = { state: 'running', attempts: 1 }
    ownerWithoutGeneration.owner = {
      pid: process.pid, hostname: require('node:os').hostname(), invocationId: 'fixture',
      startedAt: new Date().toISOString(),
    }
    writeMarker(ownerWithoutGeneration)

    const badUnlockToken = clone(validMarker(1, 'TASK_21_bad_unlock_token'))
    badUnlockToken.artifacts.unlockSourceToken = 'not-a-token'
    writeMarker(badUnlockToken)

    const orphanOutcomeProof = clone(validMarker(1, 'TASK_22_orphan_outcome_proof'))
    orphanOutcomeProof.artifacts.outcomeSourceProof = identityProof('8')
    writeMarker(orphanOutcomeProof)

    for (const badStem of ['TASK_13_missing_source', 'TASK_14_bad_phase', 'TASK_15_bad_figma', 'TASK_16_bad_component_pair',
      'TASK_17_bad_completed', 'TASK_18_bad_unlock_proof', 'TASK_19_incomplete_lock_proof',
      'TASK_20_owner_without_generation', 'TASK_21_bad_unlock_token', 'TASK_22_orphan_outcome_proof']) {
      const projected = finalizations.readOne(badStem)
      assert.equal(projected.status, 'corrupt', badStem)
      assert.equal(projected.errorCode, 'MARKER_INVALID', badStem)
      assert.equal(projected.recoverable, false, badStem)
    }

    const writerLeases = require('../../tasks/writer-leases.cjs')
    const liveStem = 'TASK_23_live_incomplete_owner'
    const liveIncomplete = clone(validMarker(1, liveStem))
    liveIncomplete.owner = {
      pid: process.pid,
      processStartId: process.platform === 'linux' || process.platform === 'darwin'
        ? writerLeases.captureProcessStartId(process.pid) : null,
      hostname: require('node:os').hostname(),
      invocationId: 'live-incomplete-fixture',
      startedAt: new Date().toISOString(),
    }
    writeMarker(liveIncomplete)
    const projectedLive = finalizations.readOne(liveStem)
    assert.equal(projectedLive.recoveryRunning, true)
    assert.equal(projectedLive.recoverable, false)
    const integrity = finalizations.scanIntegrity({
      stem: liveStem,
      writerLeaseInspection: { active: [], stale: [], issues: [], snapshotInputs: [], truncated: false },
    })
    const markerStatus = integrity.statuses.find((row) => row.kind === 'marker')
    assert.equal(markerStatus.state, 'running')
    assert.equal(markerStatus.lockGenerationHash,
      taskStateCore.lockGenerationHash(liveIncomplete.source.lock),
      'the safe marker status must bind recovery to the exact captured owned-lock generation')
    assert.equal(integrity.findings.some((row) => row.code === 'FINALIZATION_RECOVERY_REQUIRED'), false)
  })

  await check('filesystem EACCES/EIO is fail-closed for hasMarker and list', function () {
    const fsCjs = require('node:fs')
    const fileGuards = require('../server/file-guards.js')
    const inaccessibleStem = 'TASK_18_io_error'
    const inaccessiblePath = join(dir, inaccessibleStem + '.json')
    const originalLstat = fsCjs.lstatSync
    const originalInspect = fileGuards.inspectEntryUnder
    fsCjs.lstatSync = function (target, ...args) {
      if (target === inaccessiblePath) { const error = new Error('fixture EACCES'); error.code = 'EACCES'; throw error }
      return originalLstat.call(this, target, ...args)
    }
    fileGuards.inspectEntryUnder = function (rootArg, directoryArg, target) {
      if (target === inaccessiblePath) return { status: 'unsafe' }
      return originalInspect.call(this, rootArg, directoryArg, target)
    }
    try {
      assert.equal(finalizations.hasMarker(inaccessibleStem), true)
      assert.equal(finalizations.readOne(inaccessibleStem).errorCode, 'MARKER_READ_FAILED')
    } finally {
      fsCjs.lstatSync = originalLstat
      fileGuards.inspectEntryUnder = originalInspect
    }

    const originalBoundedNames = fileGuards.boundedDirectoryNamesUnder
    fileGuards.boundedDirectoryNamesUnder = function (rootArg, target, maxEntries) {
      if (target === dir) return { ok: false, code: 'directory-unsafe', names: [] }
      return originalBoundedNames.call(this, rootArg, target, maxEntries)
    }
    try {
      const projected = finalizations.list()
      assert.equal(projected.length, 1)
      assert.equal(projected[0].status, 'corrupt')
      assert.equal(projected[0].errorCode, 'MARKER_DIR_UNAVAILABLE')
      assert.equal(projected[0].recoverable, false)
      assert.equal(finalizations.mutationBlocked(null), true, 'global mutation guard must fail closed when marker directory cannot be listed')
    } finally {
      fileGuards.boundedDirectoryNamesUnder = originalBoundedNames
    }
  })

  await check('stable mutex owner record distinguishes a released flock from a live holder', function () {
    const mutex = join(dir, '.mutex.json')
    const writerLeases = require('../../tasks/writer-leases.cjs')
    const processStartId = process.platform === 'linux' || process.platform === 'darwin'
      ? writerLeases.captureProcessStartId(process.pid) : null
    const base = { version: 1, pid: process.pid, processStartId, hostname: require('node:os').hostname(), invocationId: 'fixture', startedAt: new Date().toISOString() }
    writeFileSync(mutex, JSON.stringify({ ...base, released: true }) + '\n')
    assert.equal(finalizations.readOne(stem).recoverable, true)
    writeFileSync(mutex, JSON.stringify({ ...base, released: false }) + '\n')
    const busy = finalizations.readOne(stem)
    assert.equal(busy.recoverable, false)
    assert.equal(busy.errorCode, 'FINALIZATION_MUTEX_BUSY')
    const incompleteReleased = { ...base, released: true }
    delete incompleteReleased.processStartId
    assert.match(finalizations.mutexRecordIssue(incompleteReleased), /fields do not match|invalid/)
    const incompleteActive = { ...incompleteReleased, released: false }
    assert.match(finalizations.mutexRecordIssue(incompleteActive), /fields do not match|invalid/)
    if (processStartId) {
      const last = processStartId.endsWith('0') ? '1' : '0'
      writeFileSync(mutex, JSON.stringify({ ...base, processStartId: processStartId.slice(0, -1) + last, released: false }) + '\n')
      assert.equal(finalizations.readOne(stem).recoverable, true, 'a reused PID must not resurrect a foreign mutex owner')
    }
    const missingGeneration = { ...base, released: false }
    delete missingGeneration.processStartId
    assert.match(finalizations.mutexRecordIssue(missingGeneration), /fields do not match|invalid/)
    rmSync(mutex, { force: true })
  })

  await check('stale concurrency token is rejected and valid resume deduplicates', async function () {
    const current = finalizations.readOne(stem)
    const stale = finalizations.resume(stem, current.revision - 1, current.etag)
    assert.equal(stale.statusCode, 409)
    assert.equal(stale.error, 'finalization-changed')
    const accepted = finalizations.resume(stem, current.revision, current.etag)
    assert.equal(accepted.accepted, true)
    const duplicate = finalizations.resume(stem, current.revision, current.etag)
    assert.equal(duplicate.alreadyRunning, true)
    const otherStem = 'TASK_12_other_recovery'
    writeMarker(validMarker(1, otherStem))
    const other = finalizations.readOne(otherStem)
    assert.equal(other.recoverable, false)
    assert.equal(other.errorCode, 'FINALIZATION_SERVER_BUSY')
    const globallyBusy = finalizations.resume(otherStem, other.revision, other.etag)
    assert.equal(globallyBusy.statusCode, 409)
    assert.equal(globallyBusy.error, 'finalization-busy')
  })

  await check('HTTP recovery blocks competing mutations but keeps read-only terminal follow-up available', async function () {
    const sessionsMod = require('../server/sessions.js')
    const originalSendOrResume = sessionsMod.sendOrResume
    const terminalSends = []
    sessionsMod.sendOrResume = function (key, text) {
      terminalSends.push({ key, text })
      return { sent: true, resumed: true, queued: false }
    }
    const httpMod = require('../server/http.js')
    const server = createServer(httpMod.handle)
    await new Promise((resolveListen) => server.listen(0, '127.0.0.1', resolveListen))
    const port = server.address().port
    const base = 'http://127.0.0.1:' + port
    try {
      const state = await (await fetch(base + '/api/state')).json()
      assert.ok(state.csrfToken)
      assert.ok(state.progress.finalizations.some((x) => x.stem === stem))
      const headers = { 'content-type': 'application/json', 'x-orchestrator-csrf': state.csrfToken, origin: base }

      const noCsrf = await fetch(base + '/api/finalizations/resume', { method: 'POST', headers: { 'content-type': 'application/json' }, body: '{}' })
      assert.equal(noCsrf.status, 403)

      const marker = finalizations.readOne(stem)
      const stale = await fetch(base + '/api/finalizations/resume', {
        method: 'POST', headers,
        body: JSON.stringify({ stem, expectedRevision: marker.revision + 1, expectedEtag: marker.etag })
      })
      assert.equal(stale.status, 409)

      const enqueue = await fetch(base + '/api/requests', {
        method: 'POST', headers,
        body: JSON.stringify({ action: 'run', stem, prompt: 'do not run' })
      })
      assert.equal(enqueue.status, 404)

      const session = await fetch(base + '/api/session/send', {
        method: 'POST', headers,
        body: JSON.stringify({ key: 'task:' + stem, text: 'continue' })
      })
      assert.equal(session.status, 200)
      assert.equal((await session.json()).sent, true)

      const createBacklog = await fetch(base + '/api/tasks/backlog', {
        method: 'POST', headers,
        body: JSON.stringify({ title: 'Do not create globally', body: '## Goal\nBlocked by finalization.', idempotencyKey: 'finalization-block-0001', originStem: null, dedupKey: null, dedupReport: null })
      })
      assert.equal(createBacklog.status, 409)
      assert.equal((await createBacklog.json()).error, 'finalization-active')

      const addLocal = await fetch(base + '/api/figma/add-local', { method: 'POST', headers, body: '{}' })
      assert.equal(addLocal.status, 409)
      assert.equal((await addLocal.json()).error, 'finalization-active')

      const directTaskSession = await fetch(base + '/api/session/start', {
        method: 'POST', headers,
        body: JSON.stringify({ key: 'task:TASK_999_unrelated', prompt: 'do not bypass canonical admission' })
      })
      assert.equal(directTaskSession.status, 400)
      assert.equal((await directTaskSession.json()).error, 'task-session-start-forbidden')

      for (const key of ['figma:sync-components', 'figma:rebundle:' + stem, 'figma:screens:' + stem, 'contract:diff', 'setup']) {
        const figmaSession = await fetch(base + '/api/session/start', {
          method: 'POST', headers,
          body: JSON.stringify({ key, prompt: 'do not mutate Figma artifacts' })
        })
        assert.equal(figmaSession.status, 409, key)
        assert.equal((await figmaSession.json()).error, 'finalization-active', key)
      }

      const figmaSend = await fetch(base + '/api/session/send', {
        method: 'POST', headers,
        body: JSON.stringify({ key: 'figma:rebundle:' + stem, text: 'do not continue evidence mutation' })
      })
      assert.equal(figmaSend.status, 200)
      assert.equal((await figmaSend.json()).sent, true)
      assert.deepEqual(terminalSends, [
        { key: 'task:' + stem, text: 'continue' },
        { key: 'figma:rebundle:' + stem, text: 'do not continue evidence mutation' }
      ])
    } finally {
      await new Promise((resolveClose) => server.close(resolveClose))
      sessionsMod.sendOrResume = originalSendOrResume
    }
  })

  await check('standby helper claims before trust, pairs reservation, guards twice, and discloses only after final fence', function () {
    const doc = readFileSync(new URL('../../../.claude/commands/serve-queue.md', import.meta.url), 'utf8')
    const atomicClaim = doc.indexOf('standby-queue.mjs claim-next')
    const exactValidation = doc.indexOf('The claimed-file contract is exact')
    const reservation = doc.indexOf('standby-queue.mjs ensure-reservation')
    const firstMarker = doc.indexOf('### 4. Canonical same-stem finalization probe')
    const lease = doc.indexOf('writer-lease.mjs acquire')
    const secondMarker = doc.indexOf('After a successful acquire, immediately rerun the exact guarded verification')
    const editGuard = doc.indexOf('edit-marker.mjs guard')
    const lockInspect = doc.indexOf('task-lock.mjs inspect')
    const actionValidation = doc.indexOf('validate-task-state.mjs')
    const prepare = doc.indexOf('standby-queue.mjs prepare-execution')
    const disclosure = doc.indexOf('standby-queue.mjs read-prompt')
    const release = doc.lastIndexOf('writer-lease.mjs release')
    const consume = doc.lastIndexOf('standby-queue.mjs consume')
    assert.ok(atomicClaim >= 0 && atomicClaim < exactValidation && exactValidation < reservation && reservation < firstMarker &&
      firstMarker < lease && lease < secondMarker && secondMarker < editGuard && editGuard < lockInspect &&
      lockInspect < actionValidation && actionValidation < prepare && prepare < disclosure &&
      disclosure < release && release < consume)
    assert.match(doc, /Raw filesystem\s+link\/rename\/unlink sequences are forbidden/)
    assert.match(doc, /Only `standby-queue\.mjs restore`/)
    assert.match(doc, /sole prompt disclosure\s+boundary/)
    assert.match(doc, /writer-lease\.mjs acquire/)
    assert.match(doc, /writer-lease\.mjs release/)
    assert.match(doc, /--writer-session-id <sessionId>/)
  })

  await check('session writer lease closes the finalizer check-to-spawn race and releases on result', function () {
    const script = String.raw`
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const events = require('node:events');
const stream = require('node:stream');
const cp = require('node:child_process');
const state = fs.mkdtempSync(path.join(os.tmpdir(), 'writer-handshake-'));
process.env.ORCHESTRATOR_PROJECT_ROOT = state;
process.env.ORCHESTRATOR_RUNS_DIR = path.join(state, 'runs');
process.env.ORCHESTRATOR_FINALIZATIONS_DIR = state;
process.env.ORCHESTRATOR_TASK_CREATIONS_DIR = path.join(state, 'creations');
process.env.ORCHESTRATOR_TASK_EDITS_DIR = path.join(state, 'edits');
const leases = require('./orchestrator/tasks/writer-leases.cjs');
const originalAcquire = leases.acquire;
let injectMarker = true, spawnCount = 0, lastChild = null;
leases.acquire = function (dir, options) {
  const handle = originalAcquire(dir, options);
  if (injectMarker) fs.writeFileSync(path.join(state, 'TASK_1_race.json'), '{}\n');
  return handle;
};
class FakeChild extends events.EventEmitter {
  constructor() { super(); this.pid = process.pid; this.stdout = new stream.PassThrough(); this.stderr = new stream.PassThrough(); this.stdin = new stream.PassThrough(); }
  kill() { this.emit('exit', 0, 'SIGTERM'); return true; }
}
cp.spawn = function () { spawnCount++; lastChild = new FakeChild(); return lastChild; };
const sessions = require('./orchestrator/site/server/sessions.js');
const finalizationsApi = require('./orchestrator/site/server/finalizations.js');
const raced = sessions.start('setup', { prompt: 'mutate' });
assert.equal(raced.running, false);
assert.match(raced.error, /finalization-active/);
assert.equal(spawnCount, 0, 'initial prompted session must not spawn after the post-lease recheck fails');
assert.equal(leases.scan(path.join(state, '.writers')).active.length, 0, 'refused lease must be withdrawn');
fs.rmSync(path.join(state, 'TASK_1_race.json'));
injectMarker = false;
const pending = finalizationsApi.beginMutation({ kind: 'task-session', stem: 'TASK_8_pending_gap', sessionId: leases.createSessionId(), key: 'task:TASK_8_pending_gap' });
assert.equal(pending.ok, true);
let pendingRows = leases.scan(path.join(state, '.writers')).active;
assert.equal(pendingRows.length, 1);
assert.equal(pendingRows[0].unverified, true, 'site lease must be fail-closed before spawn/PID attach');
finalizationsApi.endMutation(pending.handle);
const started = sessions.start('setup', { prompt: 'mutate safely' });
assert.equal(started.running, true);
assert.equal(spawnCount, 1);
assert.equal(leases.scan(path.join(state, '.writers')).active.length, 1);
lastChild.stdout.write(JSON.stringify({ type: 'result', result: 'ok' }) + '\n');
setImmediate(() => {
  assert.equal(leases.scan(path.join(state, '.writers')).active.length, 0, 'result releases only after the successful stdin write callback settles');
  lastChild.emit('exit', 0, null); lastChild.emit('close', 0, null);
  cp.spawn = function () { spawnCount++; lastChild = new FakeChild(); delete lastChild.pid; return lastChild; };
  const unavailable = sessions.start('task:TASK_4_spawn_failure', { stem: 'TASK_4_spawn_failure', prompt: 'must not run' });
  assert.equal(unavailable.running, false);
  assert.equal(unavailable.error, 'workspace-writer-lease-attach-failed');
  lastChild.emit('error', new Error('fixture ENOENT'));
  assert.equal(leases.scan(path.join(state, '.writers')).active.length, 0, 'a spawn failure with no PID leaves no process tree and may withdraw its lease');
  fs.rmSync(state, { recursive: true, force: true });
});
`;
    const result = spawnSync(process.execPath, ['-e', script], { cwd: fileURLToPath(new URL('../../..', import.meta.url)), encoding: 'utf8' })
    assert.equal(result.status, 0, result.stderr + result.stdout)
  })

  await check('board-task writer leases are mutually exclusive across stems and drainers (frozen serial safety)', function () {
    const script = String.raw`
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const state = fs.mkdtempSync(path.join(os.tmpdir(), 'task-writer-exclusive-'));
process.env.ORCHESTRATOR_PROJECT_ROOT = state;
process.env.ORCHESTRATOR_RUNS_DIR = path.join(state, 'runs');
process.env.ORCHESTRATOR_FINALIZATIONS_DIR = state;
process.env.ORCHESTRATOR_TASK_CREATIONS_DIR = path.join(state, 'creations');
process.env.ORCHESTRATOR_TASK_EDITS_DIR = path.join(state, 'edits');
const leases = require('./orchestrator/tasks/writer-leases.cjs');
const finalizationsApi = require('./orchestrator/site/server/finalizations.js');
const writers = path.join(state, '.writers');
// A live board-task writer that this server process does NOT own: the lease
// owner is the test's parent process (alive for the whole check).
const foreign = leases.acquire(writers, {
  kind: 'task-session', stem: 'TASK_1_first', key: 'task:TASK_1_first',
  sessionId: leases.createSessionId(), ownerPid: process.ppid
});
// 1. A second board-task acquisition for a DIFFERENT stem is refused and its
//    just-published lease is withdrawn (publish-then-scan handshake).
const refused = finalizationsApi.beginMutation({
  kind: 'task-session', stem: 'TASK_2_second',
  sessionId: leases.createSessionId(), key: 'task:TASK_2_second'
});
assert.equal(refused.ok, false);
assert.equal(refused.error, 'finalization-active');
assert.equal(leases.scan(writers).active.length, 1, 'refused acquisition must withdraw its own lease');
// 2. Control-plane writers keep the narrower per-stem/key rules.
const workspace = finalizationsApi.beginMutation({
  kind: 'workspace-session', stem: null,
  sessionId: leases.createSessionId(), key: 'figma:screens:TASK_1_first'
});
assert.equal(workspace.ok, true, 'workspace-session writers stay compatible with a live board-task writer');
finalizationsApi.endMutation(workspace.handle);
// 3. The runner's durable occupancy probe reports the foreign writer, and
//    clears once it is released.
const occupied = finalizationsApi.foreignTaskSessionWriterIssue();
assert.ok(occupied, 'foreign live board-task lease must occupy the serial slot');
assert.equal(occupied.code, 'TASK_WRITER_ACTIVE');
assert.match(occupied.message, /TASK_1_first/);
leases.release(foreign);
assert.equal(finalizationsApi.foreignTaskSessionWriterIssue(), null);
// 4. A lease owned by THIS live process is not "foreign": the in-memory
//    running count already accounts for it.
const own = leases.acquire(writers, {
  kind: 'task-session', stem: 'TASK_3_own', key: 'task:TASK_3_own',
  sessionId: leases.createSessionId(), ownerPid: process.pid
});
assert.equal(finalizationsApi.foreignTaskSessionWriterIssue(), null,
  'a lease owned by this live process must not hold the drain');
leases.release(own);
fs.rmSync(state, { recursive: true, force: true });
`;
    const result = spawnSync(process.execPath, ['-e', script], { cwd: fileURLToPath(new URL('../../..', import.meta.url)), encoding: 'utf8', timeout: 15000 })
    assert.equal(result.status, 0, result.stderr + result.stdout)
  })

  await check('asynchronous session stdin EPIPE is contained and retains ownership through process-tree proof', function () {
    if (process.platform === 'win32') return
    const script = String.raw`
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const events = require('node:events');
const stream = require('node:stream');
const cp = require('node:child_process');
const state = fs.mkdtempSync(path.join(os.tmpdir(), 'session-stdin-epipe-'));
process.env.ORCHESTRATOR_PROJECT_ROOT = state;
process.env.ORCHESTRATOR_RUNS_DIR = path.join(state, 'runs');
process.env.ORCHESTRATOR_FINALIZATIONS_DIR = state;
process.env.ORCHESTRATOR_TASK_CREATIONS_DIR = path.join(state, 'creations');
process.env.ORCHESTRATOR_TASK_EDITS_DIR = path.join(state, 'edits');
process.env.SESSION_STDIN_KILL_GRACE_MS = '80';
process.env.SESSION_LEASE_FAST_DEADLINE_MS = '40';
process.env.SESSION_LEASE_FAST_PROBE_MS = '5';
process.env.SESSION_LEASE_REAPER_MS = '20';
const originalKill = process.kill;
const originalSpawn = cp.spawn;
const authorityChild = originalSpawn(process.execPath, ['-e', "process.on('SIGTERM',()=>{});setInterval(()=>{},1000)"], {
  detached: true, stdio: 'ignore'
});
const fakePid = authorityChild.pid;
let groupAlive = true;
const signals = [];
let child;
function esrch() { const error = new Error('fixture process group is gone'); error.code = 'ESRCH'; return error; }
process.kill = function (pid, signal) {
  if (pid !== -fakePid) return originalKill.call(process, pid, signal);
  if (signal === 0 || signal === undefined) { if (groupAlive) return true; throw esrch(); }
  signals.push(signal);
  if (signal === 'SIGKILL' && groupAlive) {
    groupAlive = false;
    try { originalKill.call(process, -fakePid, 'SIGKILL'); } catch {}
    setImmediate(() => { child.emit('exit', 1, 'SIGKILL'); child.emit('close', 1, 'SIGKILL'); });
  }
  return true;
};
class BrokenStdin extends stream.Writable {
  _write(_chunk, _encoding, callback) {
    const error = new Error('fixture broken pipe'); error.code = 'EPIPE';
    // Race a result ahead of the asynchronous write failure. The result must
    // not withdraw ownership until stdin settlement proves the turn was sent.
    setImmediate(() => {
      child.stdout.write(JSON.stringify({ type: 'result', result: 'racing stale result' }) + '\n');
      setImmediate(() => callback(error));
    });
  }
}
class FakeChild extends events.EventEmitter {
  constructor() {
    super(); this.pid = fakePid; this.stdout = new stream.PassThrough(); this.stderr = new stream.PassThrough(); this.stdin = new BrokenStdin();
  }
  kill(signal) { return process.kill(-fakePid, signal); }
}
cp.spawn = function () { child = new FakeChild(); return child; };
const leases = require('./orchestrator/tasks/writer-leases.cjs');
const sessions = require('./orchestrator/site/server/sessions.js');
const writers = path.join(state, '.writers');
async function waitFor(predicate, timeout = 2000) {
  const deadline = Date.now() + timeout;
  while (true) {
    if (predicate()) return;
    if (Date.now() >= deadline) break;
    await new Promise((resolve) => setTimeout(resolve, 5));
  }
  throw new Error('fixture wait timed out');
}
(async () => {
  const stem = 'TASK_81_stdin_epipe';
  const started = sessions.start('task:' + stem, { stem, prompt: 'this write fails asynchronously' });
  assert.equal(started.running, true, 'write() returned before the asynchronous EPIPE');
  await waitFor(() => {
    const rows = leases.scan(writers).active;
    return rows.length === 1 && rows[0].unverified === true;
  });
  assert.equal(leases.scan(writers).active.length, 1, 'EPIPE must not release the lease while the group is live');
  assert.equal(signals.includes('SIGTERM'), true, 'stdin failure starts graceful tree termination');
  assert.equal(sessions.taskRunningCount(), 1, 'capacity remains occupied until the child exit event');
  assert.equal(sessions.eventsSince('task:' + stem, 0).some((event) =>
    event.kind === 'error' && event.code === 'session-input-failed' && !event.text
  ), true);
  await waitFor(() => leases.scan(writers).active.length === 0);
  assert.equal(signals.includes('SIGKILL'), true, 'SIGTERM-ignoring tree receives the fail-safe escalation');
  assert.equal(sessions.taskRunningCount(), 0);
  cp.spawn = originalSpawn; process.kill = originalKill;
  fs.rmSync(state, { recursive: true, force: true });
})().catch((error) => {
  try { originalKill.call(process, -fakePid, 'SIGKILL'); } catch {}
  cp.spawn = originalSpawn; process.kill = originalKill;
  console.error(error.stack || error); process.exitCode = 1;
});
`;
    const result = spawnSync(process.execPath, ['-e', script], {
      // This fixture intentionally replaces child_process.spawn before the
      // guarded writer module loads. File transactions therefore use their
      // isolated one-shot fallback. The polling deadline still rejects an unmet
      // condition, while the outer timeout bounds a synchronous fsync stall.
      cwd: fileURLToPath(new URL('../../..', import.meta.url)), encoding: 'utf8', timeout: 10000
    })
    assert.equal(result.status, 0, result.stderr + result.stdout)
  })

  await check('session lease reaper continues after the fast deadline and retries a failed proven release', function () {
    if (process.platform === 'win32') return
    const script = String.raw`
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const events = require('node:events');
const stream = require('node:stream');
const cp = require('node:child_process');
const state = fs.mkdtempSync(path.join(os.tmpdir(), 'session-lease-reaper-'));
process.env.ORCHESTRATOR_PROJECT_ROOT = state;
process.env.ORCHESTRATOR_RUNS_DIR = path.join(state, 'runs');
process.env.ORCHESTRATOR_FINALIZATIONS_DIR = state;
process.env.ORCHESTRATOR_TASK_CREATIONS_DIR = path.join(state, 'creations');
process.env.ORCHESTRATOR_TASK_EDITS_DIR = path.join(state, 'edits');
process.env.SESSION_LEASE_FAST_DEADLINE_MS = '20';
process.env.SESSION_LEASE_FAST_PROBE_MS = '5';
process.env.SESSION_LEASE_REAPER_MS = '30';
const originalKill = process.kill;
const originalSpawn = cp.spawn;
const authorityChild = originalSpawn(process.execPath, ['-e', 'setInterval(()=>{},1000)'], {
  detached: true, stdio: 'ignore'
});
const fakePid = authorityChild.pid;
let groupAlive = true, killProbes = 0;
let child;
function esrch() { const error = new Error('fixture process group is gone'); error.code = 'ESRCH'; return error; }
process.kill = function (pid, signal) {
  if (pid !== -fakePid) return originalKill.call(process, pid, signal);
  if (signal === 0 || signal === undefined) { if (groupAlive) return true; throw esrch(); }
  if (signal === 'SIGKILL') killProbes++;
  return true;
};
class FakeChild extends events.EventEmitter {
  constructor() {
    super(); this.pid = fakePid; this.stdout = new stream.PassThrough(); this.stderr = new stream.PassThrough(); this.stdin = new stream.PassThrough();
  }
  kill(signal) { return process.kill(-fakePid, signal); }
}
cp.spawn = function () { child = new FakeChild(); return child; };
const leases = require('./orchestrator/tasks/writer-leases.cjs');
const finalizations = require('./orchestrator/site/server/finalizations.js');
const sessions = require('./orchestrator/site/server/sessions.js');
const writers = path.join(state, '.writers');
const originalEnd = finalizations.endMutation;
let releaseCalls = 0;
finalizations.endMutation = function (handle) {
  releaseCalls++;
  if (releaseCalls === 1) return false;
  return originalEnd(handle);
};
async function waitFor(predicate, timeout = 2000) {
  const deadline = Date.now() + timeout;
  while (true) {
    if (predicate()) return;
    if (Date.now() >= deadline) break;
    await new Promise((resolve) => setTimeout(resolve, 5));
  }
  throw new Error('fixture wait timed out');
}
(async () => {
  const stem = 'TASK_82_background_reaper';
  assert.equal(sessions.start('task:' + stem, { stem, prompt: 'hold the writer lease' }).running, true);
  assert.equal(leases.scan(writers).active.length, 1);
  child.emit('exit', 1, null); child.emit('close', 1, null);
  const realNow = Date.now;
  Date.now = () => realNow() + 6 * 60 * 1000;
  const retainedSnapshot = sessions.list()['task:' + stem];
  Date.now = realNow;
  assert.equal(retainedSnapshot.closing, true, 'TTL prune must retain the live lease/reaper session object, not fall back to a closed sidecar projection');
  await new Promise((resolve) => setTimeout(resolve, 80));
  const afterDeadline = killProbes;
  assert.ok(afterDeadline >= 2, 'fast probe must attempt process-group termination');
  assert.equal(leases.scan(writers).active.length, 1, 'deadline retains ownership while the group is still observable');
  await new Promise((resolve) => setTimeout(resolve, 70));
  assert.ok(killProbes > afterDeadline, 'low-frequency reaper must keep probing beyond the initial five-second window');
  groupAlive = false;
  try { originalKill.call(process, -fakePid, 'SIGKILL'); } catch {}
  await waitFor(() => releaseCalls >= 1);
  assert.equal(leases.scan(writers).active.length, 1, 'failed release proof must restore and retain the lease');
  await waitFor(() => releaseCalls >= 2 && leases.scan(writers).active.length === 0);
  finalizations.endMutation = originalEnd; cp.spawn = originalSpawn; process.kill = originalKill;
  fs.rmSync(state, { recursive: true, force: true });
})().catch((error) => {
  try { originalKill.call(process, -fakePid, 'SIGKILL'); } catch {}
  finalizations.endMutation = originalEnd; cp.spawn = originalSpawn; process.kill = originalKill;
  console.error(error.stack || error); process.exitCode = 1;
});
`;
    const result = spawnSync(process.execPath, ['-e', script], {
      // The fixture replaces child_process.spawn before the guarded modules
      // load, so exact file transactions use the isolated one-shot fallback.
      // The polling deadline rejects an unmet condition; the outer timeout also
      // bounds any synchronous durable transaction that blocks the fixture loop.
      cwd: fileURLToPath(new URL('../../..', import.meta.url)), encoding: 'utf8', timeout: 10000
    })
    assert.equal(result.status, 0, result.stderr + result.stdout)
  })

  await check('durable writer scan keeps a detached descendant active after its session leader dies', function () {
    if (process.platform === 'win32') return
    const script = String.raw`
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { spawn } = require('node:child_process');
const leases = require('./orchestrator/tasks/writer-leases.cjs');
const root = fs.mkdtempSync(path.join(os.tmpdir(), 'writer-orphan-group-'));
const ready = path.join(root, 'ready');
const releaseLeader = path.join(root, 'release-leader');
const grandchild = 'const fs=require("node:fs"); fs.writeFileSync(process.env.FIXTURE_READY,"ready"); setInterval(()=>{},1000)';
const leader = 'const fs=require("node:fs"); const {spawn}=require("node:child_process"); spawn(process.execPath,["-e",' + JSON.stringify(grandchild) + '],{stdio:"ignore",env:{...process.env,FIXTURE_READY:' + JSON.stringify(ready) + '}}); const gate=setInterval(()=>{if(fs.existsSync(' + JSON.stringify(releaseLeader) + ')){clearInterval(gate);process.exit(0)}},5)';
const child = spawn(process.execPath, ['-e', leader], { detached: true, stdio: 'ignore' });
const closed = new Promise((resolve) => child.once('close', resolve));
const handle = leases.acquire(path.join(root, '.writers'), {
  kind: 'task-session', stem: 'TASK_5_orphan_group', key: 'task:TASK_5_orphan_group',
  sessionId: leases.createSessionId(), ownerPid: 2147483647
});
leases.updateChildPid(handle, child.pid);
fs.writeFileSync(releaseLeader, 'release\n');
(async () => {
  const deadline = Date.now() + 3000;
  while (!fs.existsSync(ready) && Date.now() < deadline) await new Promise((resolve) => setTimeout(resolve, 10));
  assert.equal(fs.existsSync(ready), true);
  await closed;
  assert.equal(leases.scan(path.join(root, '.writers')).active.length, 1, 'surviving PGID descendant must keep the lease active');
  process.kill(-child.pid, 'SIGKILL');
  const gone = Date.now() + 3000;
  while (leases.scan(path.join(root, '.writers')).active.length && Date.now() < gone) await new Promise((resolve) => setTimeout(resolve, 25));
  assert.equal(leases.scan(path.join(root, '.writers')).active.length, 0);
  leases.release(handle);
  fs.rmSync(root, { recursive: true, force: true });
})().catch((error) => { try { process.kill(-child.pid, 'SIGKILL') } catch {} console.error(error.stack || error); process.exitCode = 1; });
`;
    const result = spawnSync(process.execPath, ['-e', script], {
      // The explicit leader-release handshake and post-SIGKILL group proof stay
      // bounded even when durable lease operations are slow under parallel load.
      cwd: fileURLToPath(new URL('../../..', import.meta.url)), encoding: 'utf8', timeout: 20000
    })
    assert.equal(result.status, 0, result.stderr + result.stdout)
  })

  await check('cancel retains a durable unverified lease until the detached process group is dead', function () {
    if (process.platform === 'win32') return
    const script = String.raw`
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const cp = require('node:child_process');
const state = fs.mkdtempSync(path.join(os.tmpdir(), 'writer-cancel-tree-'));
process.env.ORCHESTRATOR_PROJECT_ROOT = state;
process.env.ORCHESTRATOR_RUNS_DIR = path.join(state, 'runs');
process.env.ORCHESTRATOR_FINALIZATIONS_DIR = state;
process.env.ORCHESTRATOR_TASK_CREATIONS_DIR = path.join(state, 'creations');
process.env.ORCHESTRATOR_TASK_EDITS_DIR = path.join(state, 'edits');
const originalSpawn = cp.spawn;
const readyFile = path.join(state, 'tree-ready');
const grandchildCode = 'const fs=require("node:fs"); process.on("SIGTERM",()=>{}); fs.writeFileSync(process.env.FIXTURE_READY,"ready"); setInterval(()=>{},1000)';
const childCode = 'const {spawn}=require("node:child_process"); process.on("SIGTERM",()=>{}); spawn(process.execPath,["-e",' + JSON.stringify(grandchildCode) + '],{stdio:"ignore",env:{...process.env,FIXTURE_READY:' + JSON.stringify(readyFile) + '}}); setInterval(()=>{},1000)';
cp.spawn = function (_command, _args, options) {
  return originalSpawn(process.execPath, ['-e', childCode], { cwd: options.cwd, env: options.env, stdio: ['pipe','pipe','pipe'], detached: true });
};
const leases = require('./orchestrator/tasks/writer-leases.cjs');
const sessions = require('./orchestrator/site/server/sessions.js');
const finalizationsApi = require('./orchestrator/site/server/finalizations.js');
const stem = 'TASK_2_cancel_tree';
const started = sessions.start('task:' + stem, { stem, prompt: 'mutate until canceled' });
assert.equal(started.running, true);
assert.equal(leases.scan(path.join(state, '.writers')).active.length, 1);
const readyDeadline = Date.now() + 3000;
while (!fs.existsSync(readyFile) && Date.now() < readyDeadline) Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 10);
assert.equal(fs.existsSync(readyFile), true, 'fixture tree must install its SIGTERM handlers before cancel');
sessions.cancel('task:' + stem);
let rows = leases.scan(path.join(state, '.writers')).active;
assert.equal(rows.length, 1, 'cancel must not release before process-tree death');
assert.equal(rows[0].unverified, true, 'cancel must durably survive a server restart as fail-closed');
const overlapping = sessions.start('task:' + stem, { stem, prompt: 'must wait for the canceled tree' });
assert.equal(overlapping.running, false);
assert.match(overlapping.error, /writer-termination-pending|finalization-active/, 'same-key restart must not overlap the retained writer tree');
(async () => {
  await new Promise((resolve) => setTimeout(resolve, 300));
  assert.equal(leases.scan(path.join(state, '.writers')).active.length, 1, 'SIGTERM-ignoring tree must still own the lease');
  const deadline = Date.now() + 6000;
  while (Date.now() < deadline && leases.scan(path.join(state, '.writers')).active.length) await new Promise((resolve) => setTimeout(resolve, 50));
  assert.equal(leases.scan(path.join(state, '.writers')).active.length, 0, 'lease may release only after SIGKILL + group-gone proof');

  finalizationsApi.attachMutationChild = () => ({ ok: false, error: 'fixture-attach-failed', detail: 'injected' });
  const attachStem = 'TASK_3_attach_failure';
  const refused = sessions.start('task:' + attachStem, { stem: attachStem, prompt: 'must never run unleased' });
  assert.equal(refused.running, false);
  rows = leases.scan(path.join(state, '.writers')).active;
  assert.equal(rows.length, 1, 'attach failure must retain its lease while killing the spawned child');
  assert.equal(rows[0].unverified, true, 'attach failure must persist fail-closed before signaling the child');
  const attachDeadline = Date.now() + 6000;
  while (Date.now() < attachDeadline && leases.scan(path.join(state, '.writers')).active.length) await new Promise((resolve) => setTimeout(resolve, 50));
  assert.equal(leases.scan(path.join(state, '.writers')).active.length, 0, 'attach-failure lease releases only after group-gone proof');
  fs.rmSync(state, { recursive: true, force: true });
})().catch((error) => { console.error(error.stack || error); process.exitCode = 1 });
`;
    const result = spawnSync(process.execPath, ['-e', script], {
      // Setup has a 3s readiness bound, followed by two sequential 6s
      // process-group proof windows and synchronous durable lease operations.
      cwd: fileURLToPath(new URL('../../..', import.meta.url)), encoding: 'utf8', timeout: 30000
    })
    assert.equal(result.status, 0, result.stderr + result.stdout)
  })

  await waitFor(() => !finalizations.readOne(stem).recoveryRunning)

  await check('recovery forces authoritative roots and projects a nonzero child failure', async function () {
    const envStem = 'TASK_19_env_failure'
    const marker = validMarker(1, envStem)
    marker.lastError = { code: 'OLD_FAILURE', message: 'must not mask the new child exit', at: '2020-01-01T00:00:00.000Z' }
    marker.updatedAt = new Date(Date.now() - 1000).toISOString()
    writeMarker(marker)
    const capture = join(root, 'captured-env.json')
    const envFinalizer = join(root, 'env-finalizer.mjs')
    writeFileSync(envFinalizer, `
import { writeFileSync } from 'node:fs'
writeFileSync(process.env.FINALIZATION_CAPTURE, JSON.stringify({
  root: process.env.FINALIZE_PROJECT_ROOT,
  locks: process.env.FINALIZE_LOCKS_DIR,
  state: process.env.FINALIZE_STATE_DIR,
  writerSessionId: process.env.ORCHESTRATOR_WRITER_SESSION_ID || null,
  writerStem: process.env.ORCHESTRATOR_WRITER_STEM || null,
  failpoint: process.env.FINALIZE_FAILPOINT || null,
  replaceTodo: process.env.FINALIZE_TEST_REPLACE_TODO_BEFORE_OUTCOME_COMMIT || null
}))
console.error('fixture child exploded')
process.exit(7)
`)
    const serverPaths = require('../server/paths.js')
    process.env.FINALIZE_TASK_SCRIPT = envFinalizer
    process.env.FINALIZATION_CAPTURE = capture
    process.env.FINALIZE_PROJECT_ROOT = '/attacker/project'
    process.env.FINALIZE_LOCKS_DIR = '/attacker/locks'
    process.env.FINALIZE_STATE_DIR = '/attacker/state'
    process.env.ORCHESTRATOR_WRITER_SESSION_ID = 'ws-attacker-session-credential'
    process.env.ORCHESTRATOR_WRITER_STEM = envStem
    // Test-only injection seams: a server environment must never hand the
    // finalizer a crash failpoint or a todo-replacement source file.
    process.env.FINALIZE_FAILPOINT = 'after-intent:outcome'
    process.env.FINALIZE_TEST_REPLACE_TODO_BEFORE_OUTCOME_COMMIT = join(root, 'attacker-todo.md')
    process.env.FINALIZATION_TIMEOUT_MS = '5000'
    try {
      const current = finalizations.readOne(envStem)
      assert.equal(finalizations.resume(envStem, current.revision, current.etag).accepted, true)
      await waitFor(() => !finalizations.readOne(envStem).recoveryRunning)
      const captured = JSON.parse(readFileSync(capture, 'utf8'))
      assert.deepEqual(captured, {
        root: serverPaths.PROJECT_ROOT,
        locks: serverPaths.LOCKS_DIR,
        state: dir,
        writerSessionId: null,
        writerStem: null,
        failpoint: null,
        replaceTodo: null
      })
      const failed = finalizations.readOne(envStem)
      assert.equal(failed.errorCode, 'FINALIZATION_PROCESS_FAILED')
      assert.match(failed.errorMessage, /exited with 7/)
      assert.match(failed.errorMessage, /fixture child exploded/)
      assert.equal(failed.recoverable, true)
    } finally {
      delete process.env.FINALIZATION_CAPTURE
      delete process.env.FINALIZE_PROJECT_ROOT
      delete process.env.FINALIZE_LOCKS_DIR
      delete process.env.FINALIZE_STATE_DIR
      delete process.env.ORCHESTRATOR_WRITER_SESSION_ID
      delete process.env.ORCHESTRATOR_WRITER_STEM
      delete process.env.FINALIZE_FAILPOINT
      delete process.env.FINALIZE_TEST_REPLACE_TODO_BEFORE_OUTCOME_COMMIT
    }
  })

  await check('Windows recovery wrapper requires an authenticated drained proof before ownership release', async function () {
    const jobStem = 'TASK_21_windows_job'
    const marker = validMarker(1, jobStem)
    marker.lastError = null
    marker.updatedAt = new Date(Date.now() - 1000).toISOString()
    writeMarker(marker)
    const finalizer = join(root, 'windows-job-finalizer.mjs')
    writeFileSync(finalizer, `setTimeout(() => process.exit(0), 80)\n`)
    const wrapper = join(root, 'fake-job-wrapper.mjs')
    writeFileSync(wrapper, `
import { spawn } from 'node:child_process'
const argv = process.argv.slice(2); if (argv[0] === '--') argv.shift()
const nonce = process.env.FINALIZATION_JOB_NONCE
const env = { ...process.env }; delete env.FINALIZATION_JOB_NONCE
const child = spawn(argv[0], argv.slice(1), { stdio: ['ignore', 'inherit', 'inherit'], env })
console.error('WINDOWS_JOB_READY ' + nonce + ' ' + child.pid)
process.stdin.on('data', () => { try { child.kill('SIGKILL') } catch {} })
child.on('close', (code) => {
  console.error('WINDOWS_JOB_DRAINED ' + nonce + ' ' + (code == null ? 1 : code))
  process.exit(code == null ? 1 : code)
})
`)
    process.env.FINALIZATION_TEST_WINDOWS_JOB = '1'
    process.env.FINALIZATION_WINDOWS_JOB_PYTHON = process.execPath
    process.env.FINALIZATION_WINDOWS_JOB_WRAPPER = wrapper
    process.env.FINALIZE_TASK_SCRIPT = finalizer
    process.env.FINALIZATION_TIMEOUT_MS = '5000'
    try {
      const current = finalizations.readOne(jobStem)
      assert.equal(finalizations.resume(jobStem, current.revision, current.etag).accepted, true)
      await waitFor(() => !finalizations.readOne(jobStem).recoveryRunning)
      assert.equal(finalizations.readOne(jobStem).recoverable, true)
    } finally {
      delete process.env.FINALIZATION_TEST_WINDOWS_JOB
      delete process.env.FINALIZATION_WINDOWS_JOB_PYTHON
      delete process.env.FINALIZATION_WINDOWS_JOB_WRAPPER
      delete process.env.FINALIZATION_TIMEOUT_MS
    }

    const wrapperSource = readFileSync(new URL('../../tasks/windows-job.py', import.meta.url), 'utf8')
    const parsed = spawnSync('python3', ['-c', 'import ast,sys; ast.parse(sys.stdin.read())'], { input: wrapperSource, encoding: 'utf8' })
    assert.equal(parsed.status, 0, parsed.stderr)
    assert.match(wrapperSource, /CREATE_SUSPENDED/)
    assert.match(wrapperSource, /AssignProcessToJobObject/)
    assert.match(wrapperSource, /ActiveProcesses == 0/)
    assert.match(wrapperSource, /WINDOWS_JOB_DRAINED/)

    const isolated = mkdtempSync(join(tmpdir(), 'windows-job-unverified-'))
    const isolatedState = join(isolated, 'finalizations'); mkdirSync(isolatedState)
    const isolatedStem = 'TASK_22_windows_unverified'
    writeFileSync(join(isolatedState, isolatedStem + '.json'), JSON.stringify(validMarker(1, isolatedStem), null, 2) + '\n')
    const badWrapper = join(isolated, 'bad-wrapper.mjs')
    writeFileSync(badWrapper, 'process.exit(125)\n')
    const probe = String.raw`
const assert = require('node:assert/strict');
const finalizations = require('./orchestrator/site/server/finalizations.js');
const stem = process.env.PROBE_STEM;
const current = finalizations.readOne(stem);
assert.equal(finalizations.resume(stem, current.revision, current.etag).accepted, true);
setTimeout(() => {
  const after = finalizations.readOne(stem);
  assert.equal(after.recoveryRunning, true, 'wrapper exit without DRAINED must retain ownership');
  assert.equal(after.errorCode, 'FINALIZATION_TREE_UNVERIFIED');
  process.exit(0);
}, 250);
`;
    const unverified = spawnSync(process.execPath, ['-e', probe], {
      cwd: fileURLToPath(new URL('../../..', import.meta.url)), encoding: 'utf8', timeout: 3000,
      env: {
        ...process.env,
        ORCHESTRATOR_PROJECT_ROOT: isolated,
        ORCHESTRATOR_FINALIZATIONS_DIR: isolatedState,
        FINALIZATION_TEST_WINDOWS_JOB: '1',
        FINALIZATION_WINDOWS_JOB_PYTHON: process.execPath,
        FINALIZATION_WINDOWS_JOB_WRAPPER: badWrapper,
        FINALIZE_TASK_SCRIPT: finalizer,
        PROBE_STEM: isolatedStem
      }
    })
    assert.equal(unverified.status, 0, unverified.stderr + unverified.stdout)
    rmSync(isolated, { recursive: true, force: true })

    const epipeRoot = mkdtempSync(join(tmpdir(), 'windows-job-stdin-epipe-'))
    const epipeState = join(epipeRoot, 'finalizations'); mkdirSync(epipeState)
    const epipeStem = 'TASK_23_windows_stdin_epipe'
    writeFileSync(join(epipeState, epipeStem + '.json'), JSON.stringify(validMarker(1, epipeStem), null, 2) + '\n')
    const epipeWrapper = join(epipeRoot, 'epipe-wrapper.mjs')
    writeFileSync(epipeWrapper, `
import fs from 'node:fs'
const nonce = process.env.FINALIZATION_JOB_NONCE
console.error('WINDOWS_JOB_READY ' + nonce + ' ' + process.pid)
// Close the control channel but keep the wrapper alive past the server timeout.
// terminateRecord() then gets an asynchronous EPIPE from stdin.end().
try { fs.closeSync(0) } catch {}
setTimeout(() => process.exit(125), 250)
`)
    const epipeProbe = String.raw`
const assert = require('node:assert/strict');
const finalizations = require('./orchestrator/site/server/finalizations.js');
const stem = process.env.PROBE_STEM;
const current = finalizations.readOne(stem);
assert.equal(finalizations.resume(stem, current.revision, current.etag).accepted, true);
setTimeout(() => {
  const after = finalizations.readOne(stem);
  assert.equal(after.recoveryRunning, true, 'EPIPE without authenticated DRAINED must retain recovery ownership');
  assert.equal(after.errorCode, 'FINALIZATION_TREE_UNVERIFIED');
  process.exit(0);
}, 400);
`;
    const epipeResult = spawnSync(process.execPath, ['-e', epipeProbe], {
      cwd: fileURLToPath(new URL('../../..', import.meta.url)), encoding: 'utf8', timeout: 3000,
      env: {
        ...process.env,
        ORCHESTRATOR_PROJECT_ROOT: epipeRoot,
        ORCHESTRATOR_FINALIZATIONS_DIR: epipeState,
        FINALIZATION_TEST_WINDOWS_JOB: '1',
        FINALIZATION_WINDOWS_JOB_PYTHON: process.execPath,
        FINALIZATION_WINDOWS_JOB_WRAPPER: epipeWrapper,
        FINALIZE_TASK_SCRIPT: finalizer,
        FINALIZATION_TIMEOUT_MS: '100',
        PROBE_STEM: epipeStem
      }
    })
    assert.equal(epipeResult.status, 0, epipeResult.stderr + epipeResult.stdout)
    rmSync(epipeRoot, { recursive: true, force: true })
  })

  await check('Windows terminateRecord contains asynchronous control-pipe EPIPE and retains ownership', function () {
    const isolated = mkdtempSync(join(tmpdir(), 'windows-control-epipe-unit-'))
    const state = join(isolated, 'finalizations'); mkdirSync(state)
    const unitStem = 'TASK_24_windows_control_epipe'
    writeFileSync(join(state, unitStem + '.json'), JSON.stringify(validMarker(1, unitStem), null, 2) + '\n')
    const script = String.raw`
const assert = require('node:assert/strict');
const events = require('node:events');
const stream = require('node:stream');
const cp = require('node:child_process');
let child, nonce, errorDelivered = false;
class BrokenControl extends stream.Writable {
  _write(_chunk, _encoding, callback) {
    const error = new Error('fixture Windows control EPIPE'); error.code = 'EPIPE';
    setImmediate(() => { errorDelivered = true; callback(error); });
  }
}
class FakeChild extends events.EventEmitter {
  constructor() {
    super(); this.pid = 2147483003; this.stdout = new stream.PassThrough(); this.stderr = new stream.PassThrough(); this.stdin = new BrokenControl();
  }
  kill() { return true; }
}
cp.spawn = function (_command, _args, options) {
  nonce = options.env.FINALIZATION_JOB_NONCE;
  child = new FakeChild();
  return child;
};
const finalizations = require('./orchestrator/site/server/finalizations.js');
const stem = process.env.PROBE_STEM;
const current = finalizations.readOne(stem);
assert.equal(finalizations.resume(stem, current.revision, current.etag).accepted, true);
child.stderr.write('WINDOWS_JOB_READY ' + nonce + ' ' + child.pid + '\n');
setTimeout(() => {
  assert.equal(errorDelivered, true, 'timeout must exercise the asynchronous EPIPE callback');
  assert.equal(finalizations.readOne(stem).recoveryRunning, true, 'control EPIPE is not a drain proof');
  child.emit('close', 125, null);
  const after = finalizations.readOne(stem);
  assert.equal(after.recoveryRunning, true, 'wrapper close without DRAINED remains authoritative');
  assert.equal(after.errorCode, 'FINALIZATION_TREE_UNVERIFIED');
  process.exit(0);
}, 180);
`;
    const result = spawnSync(process.execPath, ['-e', script], {
      cwd: fileURLToPath(new URL('../../..', import.meta.url)), encoding: 'utf8', timeout: 3000,
      env: {
        ...process.env,
        ORCHESTRATOR_PROJECT_ROOT: isolated,
        ORCHESTRATOR_FINALIZATIONS_DIR: state,
        FINALIZATION_TEST_WINDOWS_JOB: '1',
        FINALIZATION_TIMEOUT_MS: '100',
        PROBE_STEM: unitStem
      }
    })
    assert.equal(result.status, 0, result.stderr + result.stdout)
    rmSync(isolated, { recursive: true, force: true })
  })

  await check('recovery timeout terminates the detached process tree and remains recoverable', async function () {
    const timeoutStem = 'TASK_20_timeout_tree'
    const marker = validMarker(1, timeoutStem)
    marker.lastError = null
    marker.updatedAt = new Date(Date.now() - 1000).toISOString()
    writeMarker(marker)
    const grandchildPidFile = join(root, 'grandchild.pid')
    const grandchildReadyFile = join(root, 'grandchild.ready')
    const timeoutFinalizer = join(root, 'timeout-finalizer.mjs')
    writeFileSync(timeoutFinalizer, `
import { spawn } from 'node:child_process'
import { writeFileSync } from 'node:fs'
// The parent exits on SIGTERM while its child deliberately survives. Recovery
// must remain blocked through the grace period until the process group sweep
// has killed and verified that stubborn descendant.
process.on('SIGTERM', () => process.exit(143))
const grandchild = spawn(process.execPath, ['-e', 'const fs=require("node:fs"); process.on("SIGTERM",()=>{}); fs.writeFileSync(process.env.GRANDCHILD_READY_FILE,"ready"); setInterval(()=>{},1000)'], {
  stdio: 'ignore', env: { ...process.env, GRANDCHILD_READY_FILE: process.env.GRANDCHILD_READY_FILE }
})
writeFileSync(process.env.GRANDCHILD_PID_FILE, String(grandchild.pid))
setInterval(() => {}, 1000)
`)
    process.env.FINALIZE_TASK_SCRIPT = timeoutFinalizer
    process.env.GRANDCHILD_PID_FILE = grandchildPidFile
    process.env.GRANDCHILD_READY_FILE = grandchildReadyFile
    const timeoutMs = 1000
    process.env.FINALIZATION_TIMEOUT_MS = String(timeoutMs)
    try {
      const current = finalizations.readOne(timeoutStem)
      const timeoutStartedAt = Date.now()
      assert.equal(finalizations.resume(timeoutStem, current.revision, current.etag).accepted, true)
      await waitFor(() => {
        try {
          const rawPid = readFileSync(grandchildPidFile, 'utf8').trim()
          return /^\d+$/.test(rawPid) && Number(rawPid) > 0 && readFileSync(grandchildReadyFile, 'utf8') === 'ready'
        } catch { return false }
      })
      const grandchildPid = Number(readFileSync(grandchildPidFile, 'utf8'))
      await new Promise((resolveDelay) => setTimeout(resolveDelay,
        Math.max(0, timeoutStartedAt + timeoutMs + 250 - Date.now())))
      assert.equal(processAlive(grandchildPid), true, 'fixture grandchild must survive the initial SIGTERM')
      assert.equal(finalizations.readOne(timeoutStem).recoveryRunning, true, 'recovery must remain fail-closed while a descendant is still alive')
      await waitFor(() => !processAlive(grandchildPid))
      await waitFor(() => !finalizations.readOne(timeoutStem).recoveryRunning)
      const timedOut = finalizations.readOne(timeoutStem)
      assert.equal(timedOut.errorCode, 'FINALIZATION_TIMEOUT')
      assert.match(timedOut.errorMessage, /exceeded 1000ms/)
      assert.equal(timedOut.recoverable, true)
    } finally {
      delete process.env.GRANDCHILD_PID_FILE
      delete process.env.GRANDCHILD_READY_FILE
      delete process.env.FINALIZATION_TIMEOUT_MS
    }
  })

  await check('reaper releases a stranded, marker-less, provably-dead finalization with no restart', function () {
    if (process.platform === 'win32') return   // POSIX process-group death proof; the Windows Job drain is a separate release path
    const reaperState = mkdtempSync(join(tmpdir(), 'finalization-reaper-'))
    for (const part of ['finalizations', 'runs', 'creations', 'edits']) mkdirSync(join(reaperState, part), { recursive: true })
    writeFileSync(join(reaperState, 'finalizations', 'TASK_88_reaper.json'),
      JSON.stringify(validMarker(3, 'TASK_88_reaper'), null, 2) + '\n')
    // Isolated subprocess: it stubs child_process.spawn + process.kill BEFORE the
    // module loads, so the finalizer child is a fake whose process-group liveness
    // the test owns outright (groupAlive) and whose parent-'close' the test emits
    // by hand. A clean state dir makes mutationBlocked a pristine observable.
    const script = String.raw`
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const events = require('node:events');
const stream = require('node:stream');
const cp = require('node:child_process');
const state = process.env.REAPER_STATE;
process.env.ORCHESTRATOR_PROJECT_ROOT = state;
process.env.ORCHESTRATOR_FINALIZATIONS_DIR = path.join(state, 'finalizations');
process.env.ORCHESTRATOR_RUNS_DIR = path.join(state, 'runs');
process.env.ORCHESTRATOR_TASK_CREATIONS_DIR = path.join(state, 'creations');
process.env.ORCHESTRATOR_TASK_EDITS_DIR = path.join(state, 'edits');
const originalKill = process.kill;
const originalSpawn = cp.spawn;
const authorityChild = originalSpawn(process.execPath, ['-e', 'setInterval(function () {}, 1000)'], { detached: true, stdio: 'ignore' });
const fakePid = authorityChild.pid;
// If the helper spawn silently failed, fakePid would be undefined and the kill
// stub (pid !== -NaN) plus processGroupGone's !child.pid short-circuit would
// invert the fixture into reporting a live group as dead. Fail loudly instead.
assert.ok(Number.isInteger(fakePid) && fakePid > 0, 'fixture authority child must own a real pid');
let groupAlive = true, lastChild = null;
function esrch() { const error = new Error('fixture process group is gone'); error.code = 'ESRCH'; return error; }
process.kill = function (pid, signal) {
  if (pid !== -fakePid) return originalKill.call(process, pid, signal);
  if (signal === 0 || signal === undefined) { if (groupAlive) return true; throw esrch(); }
  return true;   // record signals only; the test owns group liveness via groupAlive
};
class FakeChild extends events.EventEmitter {
  constructor() { super(); this.pid = fakePid; this.stdout = new stream.PassThrough(); this.stderr = new stream.PassThrough(); this.stdin = new stream.PassThrough(); }
  kill(signal) { return process.kill(-fakePid, signal); }
}
cp.spawn = function () { lastChild = new FakeChild(); return lastChild; };
const finalizations = require('./orchestrator/site/server/finalizations.js');
const stem = 'TASK_88_reaper';
const markerFile = path.join(state, 'finalizations', stem + '.json');
const savedMarker = fs.readFileSync(markerFile);
function cleanup(code) {
  cp.spawn = originalSpawn;
  try { originalKill.call(process, -fakePid, 'SIGKILL'); } catch (error) {}
  process.kill = originalKill;
  process.exit(code);
}
function token() { const c = finalizations.readOne(stem); return { revision: c.revision, etag: c.etag, recoverable: c.recoverable }; }
function resumeOwnership() {
  const t = token();
  assert.equal(t.recoverable, true, 'fixture marker must be recoverable');
  assert.equal(finalizations.resume(stem, t.revision, t.etag).accepted, true, 'resume takes in-memory ownership');
}
// Strand a record the way markTreeUnverified does: the parent 'close' fires
// while the group is still alive, so endedAt is stamped and the record is
// retained (verification pending) rather than released by the close handler.
function strand() { resumeOwnership(); lastChild.emit('close', 1, null); }
try {
  assert.equal(finalizations.mutationBlocked(null), true, 'a pending marker blocks global mutation by design');

  // 1) Terminal guard — a record whose 'close' has NOT fired is never reaped,
  //    even if its group momentarily reads dead (the spawn-time setsid window
  //    and the death-before-'close' window both look like this).
  resumeOwnership();
  groupAlive = false;
  assert.deepEqual(finalizations.reap(), [], 'a not-yet-terminated record is never reaped, even with a dead group');
  lastChild.emit('close', 1, null);   // a normal close on a dead group -> the close handler itself releases it
  assert.equal(!!finalizations.readOne(stem).recoveryRunning, false, 'the close handler released the un-stranded record');
  groupAlive = true;

  // 2) Stranded record — terminated + group still alive is fail-closed; once the
  //    group dies, reap-on-read heals it AND refreshes the terminal diagnostic.
  strand();
  assert.deepEqual(finalizations.reap(), [], 'reap never touches a terminated record whose group is still alive');
  assert.equal(finalizations.mutationBlocked(null), true, 'a stranded-but-alive finalization keeps the block');
  groupAlive = false;
  const healed = finalizations.readOne(stem);
  assert.equal(healed.recoveryRunning, false, 'readOne reaps the provably-dead record');
  assert.equal(healed.recoverable, true, 'the task is recoverable again with no restart');
  assert.equal(healed.errorCode, 'FINALIZATION_PROCESS_FAILED', 'the diagnostic is refreshed to the terminal outcome, not left tree-unverified');
  assert.deepEqual(finalizations.reap(), [], 'reap is idempotent');
  groupAlive = true;

  // 3) Marker-independent DIRECT reap — the exact incident: marker already
  //    published/cleaned, record stranded, group dead. readOne cannot heal it
  //    (no marker to read); the marker-independent reaper must.
  strand();
  fs.rmSync(markerFile);
  assert.equal(finalizations.mutationBlocked(null), true, 'the in-memory record, not the marker, is the blocker');
  groupAlive = false;
  assert.deepEqual(finalizations.reap(), [stem], 'reap releases the marker-less, provably-dead record');
  assert.deepEqual(finalizations.reap(), [], 'reap is idempotent');
  assert.equal(finalizations.mutationBlocked(null), false, 'the global block clears with no restart');
  groupAlive = true;

  // 4) Lazy heal on the mutation path — mutationBlocked itself reaps.
  fs.writeFileSync(markerFile, savedMarker);
  strand();
  fs.rmSync(markerFile);
  groupAlive = false;
  assert.equal(finalizations.mutationBlocked(null), false, 'mutationBlocked reaps the dead record itself and unblocks');
  assert.deepEqual(finalizations.reap(), [], 'nothing left to reap after the lazy heal');
  cleanup(0);
} catch (error) { fs.writeSync(2, String(error && error.stack || error) + '\n'); cleanup(1); }
`
    const result = spawnSync(process.execPath, ['-e', script], {
      cwd: fileURLToPath(new URL('../../..', import.meta.url)),
      env: { ...process.env, REAPER_STATE: reaperState },
      encoding: 'utf8', timeout: 10000
    })
    rmSync(reaperState, { recursive: true, force: true })
    assert.equal(result.status, 0, result.stderr + result.stdout)
  })

  console.log(`\nsite finalizations: ${checks} checks passed`)
} finally {
  finalizations.killAll()
  // Give SIGTERM a moment so the fake child cannot outlive fixture cleanup.
  await new Promise((resolveDelay) => setTimeout(resolveDelay, 50))
  rmSync(root, { recursive: true, force: true })
}
