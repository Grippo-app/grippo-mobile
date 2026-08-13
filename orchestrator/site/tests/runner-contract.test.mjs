#!/usr/bin/env node

import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const here = dirname(fileURLToPath(import.meta.url))
const root = join(here, '..', '..', '..')
const require = createRequire(import.meta.url)
const runner = require('../server/runner.js')
const requests = require('../server/requests.js')
const paths = require('../server/paths.js')
const expectedRoot = '/fixture/project'
const valid = {
  version: 3,
  action: 'prep',
  stem: 'TASK_7_profile_note_archive',
  expectedState: 'backlog',
  sourceRevision: 'sha256:' + 'a'.repeat(64),
  dedupKey: null,
  dedupReport: null,
  projectRoot: expectedRoot,
  prompt: 'Run task-prep.',
  createdAt: '2026-07-12T12:34:56.789Z'
}

let checks = 0
function check(name, fn) { fn(); checks++; console.log(`ok ${checks} - ${name}`) }
function issue(patch, remove) {
  const value = { ...valid, ...(patch || {}) }
  if (remove) delete value[remove]
  return runner.claimedRequestIssue(value, expectedRoot)
}

check('exact server-written claimed request is accepted without coercion', () => {
  assert.equal(issue(), null)
  for (const [action, expectedState] of [
    ['prep', 'backlog'], ['answers', 'pending'], ['run', 'todo'],
    ['drop', 'backlog'], ['drop', 'pending'], ['drop', 'todo'], ['drop', 'done'],
    ['drop', 'corrupt'], ['reopen', 'done']
  ]) assert.equal(issue({ action, expectedState }), null)
})

check('run requests receive the canonical certification generation id', () => {
  assert.equal(runner.executionRunIdForRequest('1786651740499-e73e65d4b30ace2f'),
    'run-1786651740499-e73e65d4b30ace2f')
  assert.equal(runner.executionRunIdForRequest('../escape'), null)
  assert.equal(runner.executionRunIdForRequest('run-1786651740499-e73e65d4b30ace2f'), null)
})

check('unsupported, missing and invented request shapes are rejected', () => {
  assert.match(issue({ action: 'createBacklog' }), /action/)
  assert.match(issue({ version: 1 }), /version/)
  assert.match(issue(null, 'action'), /fields/)
  assert.match(issue({ extra: true }), /fields/)
  assert.match(runner.claimedRequestIssue([], expectedRoot), /object/)
  assert.match(runner.claimedRequestIssue('prompt', expectedRoot), /object/)
})

check('stem must be canonical, positive, safe and bounded', () => {
  for (const stem of [
    '../../TASK_7_escape', 'TASK_0_zero', 'TASK_01_leading_zero',
    'TASK_9007199254740992_too_large', 'TASK_7_профиль',
    'TASK_7_' + 'x'.repeat(121)
  ]) assert.match(issue({ stem }), /stem/, stem)
})

check('projectRoot is required as an exact string', () => {
  assert.match(issue(null, 'projectRoot'), /fields/)
  assert.match(issue({ projectRoot: expectedRoot + '/.' }), /projectRoot/)
  assert.match(issue({ projectRoot: { toString: () => expectedRoot } }), /projectRoot/)
})

check('prompt is a non-empty bounded canonical UTF-8 string', () => {
  assert.equal(issue({ prompt: '\u0800'.repeat(60000) }), null, 'exact three-byte/code-unit bound')
  assert.match(issue({ prompt: '\u0800'.repeat(60001) }), /prompt/)
  assert.match(issue({ prompt: '' }), /prompt/)
  assert.match(issue({ prompt: ['do work'] }), /prompt/)
  assert.match(issue({ prompt: 'bad\0prompt' }), /UTF-8/)
  assert.match(issue({ prompt: 'bad\ud800prompt' }), /UTF-8/)
})

check('optional metadata and timestamp are exact, never coerced', () => {
  assert.match(issue({ dedupKey: [] }), /dedupKey/)
  assert.match(issue({ dedupReport: 'sha256:bad' }), /dedupReport/)
  assert.match(issue({ createdAt: Date.now() }), /createdAt/)
  assert.match(issue({ createdAt: '2026-07-12' }), /createdAt/)
  assert.match(issue({ createdAt: '2026-02-31T12:34:56.789Z' }), /createdAt/)
})

check('admitted state and source revision are action-bound exact v3 fields', () => {
  assert.match(issue({ expectedState: 'todo' }), /expectedState/)
  assert.match(issue({ action: 'answers', expectedState: 'backlog' }), /expectedState/)
  assert.match(issue({ sourceRevision: 'sha256:bad' }), /sourceRevision/)
  assert.match(issue({ sourceRevision: null }), /sourceRevision/)
  assert.match(issue(null, 'sourceRevision'), /fields/)
})

check('standby publishes its lease before the combined publication guard', () => {
  const doc = readFileSync(join(root, '.claude', 'commands', 'serve-queue.md'), 'utf8')
  assert.match(doc, /writer-lease\.mjs acquire \\\n\s+--guard-finalization \\\n\s+--kind task-session/)
  assert.match(doc, /rejects a foreign ACTIVE board-task[\s\S]{0,120}for the SAME stem — any drainer/)
  assert.match(doc, /global deterministic creation\/edit[\s\S]{0,80}publication lease/)
  assert.match(doc, /Exit 2 means another writer or[\s\S]{0,80}publication recovery refused the lease/)
  assert.match(doc, /Only `standby-queue\.mjs restore` may restore the private claim[\s\S]{0,120}link\/rename\/unlink sequences are forbidden/)
  assert.match(doc, /complete returned receipt \(`version`, `leaseId`, `token`, `kind`,[\s\S]{0,80}`sessionId`, `expiresAt`\)/)
  assert.match(doc, /writer-lease\.mjs verify --guard-finalization \\\n\s+--lease-id[\s\S]{0,120}--session-id[\s\S]{0,80}--stem/)
  assert.match(doc, /Canonical same-stem finalization probe[\s\S]{0,500}record-finalization-superseded --handle/)
  assert.match(doc, /error:finalization-supersession-unproven[\s\S]{0,240}guarded\s+acquisition in Step 5/)
  assert.match(doc, /mandatory second finalization\/mutex[\s\S]{0,220}writer-lease\.mjs verify --guard-finalization/)
  assert.match(doc, /\[FINALIZATION_MARKER_ACTIVE\][\s\S]{0,260}record-finalization-superseded --handle/)
  assert.match(doc, /task-lock\.mjs inspect --stem "<request stem>"[\s\S]{0,220}code:"LOCK_NOT_FOUND"/)
  assert.match(doc, /task-lock\.mjs acquire[\s\S]{0,260}`--writer-lease-id`[\s\S]{0,120}`--writer-lease-token`/)
  assert.match(doc, /`--owner-kind standby`[\s\S]{0,80}`--owner-id standby:<leaseId>`/)
  assert.match(doc, /before[\s\S]{0,30}after no-clobber lock publication/)
  assert.match(doc, /selected skill may renew\s+and reverify[\s\S]{0,100}MUST NOT release it/)
  assert.match(doc, /standby pass owns the one release:[\s\S]{0,100}Step 6 for a retry,[\s\S]{0,80}Step 8 after execution/)
  assert.match(doc, /claim-next --pass-token "<passToken>"/)
  assert.match(doc, /heartbeat stores only a SHA-256 hash[\s\S]{0,160}exactly one following claim attempt/)
  assert.match(doc, /record-superseded \\\n+\s+--handle[\s\S]{0,160}--lease-id[\s\S]{0,100}--session-id/)
  assert.match(doc, /record-finalization-superseded --handle/)
  assert.doesNotMatch(doc, /record-superseded[\s\S]{0,120}--reason/)
  assert.match(doc, /standby never claims `run`[\s\S]{0,180}every standby-executed action[\s\S]{0,120}proven-absent canonical task lock/i)
  assert.doesNotMatch(doc, /For `run`, lock absence is an accepted/)
  assert.doesNotMatch(doc, /canonical retained orchestrator lock/)
})

check('standby mirrors the exact claimed-file contract and quarantines invalid input', () => {
  const doc = readFileSync(join(root, '.claude', 'commands', 'serve-queue.md'), 'utf8')
  assert.match(doc, /exactly `action,createdAt,dedupKey,dedupReport,expectedState,projectRoot,prompt,sourceRevision,stem,version`/)
  assert.match(doc, /O_RDONLY\|O_NOFOLLOW[\s\S]{0,180}256 KiB/)
  assert.match(doc, /\^TASK_\(\[1-9\]\[0-9\]\*\)_\[A-Za-z0-9_\]\+\$/)
  assert.match(doc, /60,000 UTF-16 code units and 180,000[\s\S]{0,40}UTF-8 bytes/)
  assert.match(doc, /version[^\n]{0,100}`3`/i)
  assert.match(doc, /sourceRevision[\s\S]{0,120}sha256/i)
  assert.match(doc, /mismatched root, or unsupported `createBacklog` action is permanently invalid/)
  assert.match(doc, /do not execute, restore, or unlink it/)
  assert.match(doc, /Retain the private claim[\s\S]{0,100}explicit recovery/)
  assert.doesNotMatch(doc, /Missing `projectRoot`[\s\S]{0,80}implicit acceptance/)
})

check('reservation fingerprint distinguishes prompt, snapshot and dedup intent', () => {
  const fingerprintValid = { ...valid, projectRoot: paths.PROJECT_ROOT }
  const fingerprint = requests.requestFingerprint(fingerprintValid)
  assert.match(fingerprint, /^sha256:[a-f0-9]{64}$/)
  assert.equal(requests.requestFingerprint({ ...fingerprintValid, createdAt: '2026-07-12T12:34:57.789Z' }), fingerprint)
  assert.notEqual(requests.requestFingerprint({ ...fingerprintValid, prompt: 'different answers' }), fingerprint)
  assert.notEqual(requests.requestFingerprint({ ...fingerprintValid, sourceRevision: 'sha256:' + 'b'.repeat(64) }), fingerprint)
  assert.notEqual(requests.requestFingerprint({ ...fingerprintValid, dedupKey: 'report:one' }), fingerprint)
})

check('standby reservation survives claim and is released only after lease plus final fence', () => {
  const doc = readFileSync(join(root, '.claude', 'commands', 'serve-queue.md'), 'utf8')
  const ensureAt = doc.indexOf('standby-queue.mjs ensure-reservation')
  const leaseAt = doc.indexOf('writer-lease.mjs acquire')
  const fenceAt = doc.indexOf('**Execution fence')
  const releaseAt = doc.indexOf('request-reservation.mjs release')
  const executeAt = doc.indexOf('After successful reservation release')
  assert.ok(ensureAt > 0 && ensureAt < leaseAt)
  assert.ok(leaseAt < fenceAt && fenceAt < releaseAt && releaseAt < executeAt)
  assert.match(doc, /standby crash keeps private claim \+ reservation[\s\S]{0,100}neither is automatically stale/i)
  assert.match(doc, /crash after reservation withdrawal[\s\S]{0,180}never auto-requeued/i)
  assert.match(doc, /terminal id and admitted[\s\S]{0,80}lineage are immutable[\s\S]{0,180}exact durable tombstone[\s\S]{0,100}idempotent[\s\S]{0,140}lineage mismatch/i)
  assert.doesNotMatch(doc, /publishes by atomic rename/)
})

check('in-process handoff fences under lease before exact release and stdin', () => {
  const runnerSource = readFileSync(join(root, 'orchestrator', 'site', 'server', 'runner.js'), 'utf8')
  const sessionsSource = readFileSync(join(root, 'orchestrator', 'site', 'server', 'sessions.js'), 'utf8')
  const httpSource = readFileSync(join(root, 'orchestrator', 'site', 'server', 'http.js'), 'utf8')
  const hookAt = runnerSource.indexOf('const beforePrompt = function ()')
  const finalFenceAt = runnerSource.indexOf('inspectClaimForExecution(req)', hookAt)
  const releaseAt = runnerSource.indexOf('releaseRequestReservation(reservationHandle)', finalFenceAt)
  assert.ok(hookAt > 0 && finalFenceAt > hookAt && releaseAt > finalFenceAt)
  const inspectAt = runnerSource.indexOf('function inspectClaimForExecution(req)')
  const executionLockAt = runnerSource.indexOf('locksMod.lockPresence', inspectAt)
  const executionIntegrityAt = runnerSource.indexOf('taskIntegrity.validateAction', executionLockAt)
  assert.ok(inspectAt > 0 && executionLockAt > inspectAt && executionIntegrityAt > executionLockAt)
  const sessionHookAt = sessionsSource.indexOf("typeof meta.beforePrompt === 'function'", sessionsSource.indexOf('function send('))
  const stdinAt = sessionsSource.indexOf('s.child.stdin.write', sessionHookAt)
  assert.ok(sessionHookAt > 0 && stdinAt > sessionHookAt)
  const reserveAt = httpSource.indexOf('acquireRequestReservation(id, requestRecord)')
  const secondWriterGuardAt = httpSource.indexOf('finalizationsMod.mutationBlocked(stem)', reserveAt)
  const finalLockAt = httpSource.indexOf('locksMod.lockPresence(stem)', secondWriterGuardAt)
  const finalIntegrityAt = httpSource.indexOf('taskIntegrityMod.validateAction(action, stem)', finalLockAt)
  const queuePublishAt = httpSource.indexOf('writeRequestFile(id, requestRecord)', finalIntegrityAt)
  assert.ok(reserveAt > 0 && secondWriterGuardAt > reserveAt && finalLockAt > secondWriterGuardAt &&
    finalIntegrityAt > finalLockAt && queuePublishAt > finalIntegrityAt)
  assert.match(sessionsSource, /lockOwnedBySession\(s\.stem \|\| key\.slice\('task:'\.length\), s\.writerSessionId\)/)
  assert.match(sessionsSource, /s\.idleContinuationFence = captureTaskIdleContinuationFence\(s\)/)
  assert.match(sessionsSource, /answeringQuestion \? s\.answerFence : s\.idleContinuationFence/)
  assert.match(sessionsSource, /!s\.askedThisTurn && s\.turnResultSeen && s\.idleContinuationFence === answerFence/)
  assert.match(sessionsSource, /s\.writerLease !== writerLease/)
  assert.match(sessionsSource, /current\.sourceRevision !== answerFence\.sourceRevision/)
  assert.match(sessionsSource, /CONVERSATION_ONLY_INITIAL_PROMPT/)
  assert.match(sessionsSource, /'--safe-mode', '--tools', 'Read,Grep,Glob'/)
  assert.match(sessionsSource, /Once a context has pending input, preserve strict submission order/)
})

check('runner concurrency is a source constant at the canary value', () => {
  // The pre-isolation freeze existed because every task session worked in ONE
  // tree, so a second run could attribute a neighbour's bytes to its own
  // result. Per-task worktree isolation removed that failure mode, and the cap
  // is now the plan's CANARY value — still a source constant, because a knob
  // that could silently raise it would defeat the guarantee. Raising it to the
  // eventual default is an explicit owner decision.
  assert.equal(runner.MAX_PARALLEL, 2)
  const runnerPath = join(root, 'orchestrator', 'site', 'server', 'runner.js')
  const runnerSource = readFileSync(runnerPath, 'utf8')
  assert.match(runnerSource, /var MAX_PARALLEL = 2;/)
  assert.doesNotMatch(runnerSource, /parseInt\(process\.env\.RUNNER_MAX_PARALLEL/)
  // The header must not resurrect the pre-freeze parallel-drain description.
  assert.doesNotMatch(runnerSource, /in\s+parallel up to MAX_PARALLEL/)
  // The serial gate must run before any claim/spawn work in the tick, and the
  // durable occupancy hold (foreign board-task writer leases) must sit between
  // the in-memory capacity gate and the queue scan.
  const capacityAt = runnerSource.indexOf('var capacity = MAX_PARALLEL - runningCount();')
  const capacityGateAt = runnerSource.indexOf('if (capacity <= 0) return;', capacityAt)
  // Unscoped, the hold answers only "is the lease store provable"; it must
  // still run before the queue scan, because an unprovable store may never
  // admit a writer.
  const occupancyAt = runnerSource.indexOf('finalizations.foreignTaskSessionWriterIssue()', capacityGateAt)
  const queueScanAt = runnerSource.indexOf('requestsMod.scanRequests()', occupancyAt)
  assert.ok(capacityAt > 0 && capacityGateAt > capacityAt && occupancyAt > capacityGateAt && queueScanAt > occupancyAt)
  // Per-TASK occupancy is decided at claim time, so a live writer for another
  // stem no longer holds the whole drain.
  assert.match(runnerSource, /finalizations\.foreignTaskSessionWriterIssue\(stem\)/)
  // Foreign-runner marker exclusion runs before this process publishes its own
  // marker, and an auth flip keeps the marker while our task children live.
  const foreignProbeAt = runnerSource.indexOf('foreignRunnerOwner()', runnerSource.indexOf('function tick()'))
  const touchAt = runnerSource.indexOf('if (!touchMarker())', foreignProbeAt)
  assert.ok(foreignProbeAt > 0 && touchAt > foreignProbeAt)
  assert.match(runnerSource, /holding the runner marker while a task child is still running/)
  // The auth-flip hold keys on durable board-task writer evidence, not the raw
  // session count (skills:*/read-only children must not block standby takeover),
  // and its marker refresh must never steal a foreign live runner's marker.
  assert.match(runnerSource, /runningCount\(\) > 0 && finalizations\.ownTaskSessionWriterActive\(\)/)
  assert.match(runnerSource, /runnerMarkerFd !== null \|\| !foreignRunnerOwner\(\)/)
  // Guarded CLI task-session acquires must anchor the lease to the long-lived
  // session process — a per-call shell owner degrades the cross-process
  // exclusion into a bare TTL timer and breaks verify/renew mid-run.
  for (const surface of [
    join(root, '.claude', 'commands', 'serve-queue.md'),
    join(root, 'orchestrator', 'skills', 'task-orchestrator', 'references', 'run-loop.md'),
    join(root, 'orchestrator', 'skills', 'task-orchestrator', 'references', 'task-drop.md'),
    join(root, 'orchestrator', 'skills', 'task-prep', 'references', 'prep-flow.md'),
  ]) {
    const doc = readFileSync(surface, 'utf8')
    assert.ok(doc.replace(/\s+/g, ' ').includes('--kind task-session'), surface)
    assert.ok(doc.includes('--owner-pid "$PPID"'), surface + ' must anchor the guarded acquire to $PPID')
    assert.match(doc, /never inside `bash -c`, a\s+heredoc, or any\s+nested shell/i,
      surface + ' must keep the short-lived nested-shell warning beside the $PPID anchor')
  }
  // Board-task writers are exclusive PER STEM at the lease layer, and both
  // halves of the cross-process rule must say the same thing: the site
  // predicate and the guarded CLI mirror. Neither may keep the cross-stem
  // freeze, or a second isolated run could never start.
  const finalizationsSource = readFileSync(join(root, 'orchestrator', 'site', 'server', 'finalizations.js'), 'utf8')
  assert.doesNotMatch(finalizationsSource, /options\.kind === 'task-session' && row\.kind === 'task-session'/)
  assert.match(finalizationsSource, /if \(stem && row\.stem === stem\) return true;/)
  assert.match(finalizationsSource, /function foreignTaskSessionWriterIssue\(stem\)/)
  const writerLeaseCli = readFileSync(join(root, 'orchestrator', 'tasks', 'writer-lease.mjs'), 'utf8')
  assert.doesNotMatch(writerLeaseCli, /activeOwn\.kind === 'task-session' && row\.kind === 'task-session'/)
  assert.match(writerLeaseCli, /stem && \(row\.stem === stem \|\| row\.key === `task:\$\{stem\}`\)/)
  // The standby drainer refuses NEW claims while any board-task writer lease
  // is active, but keeps recovery of an already-claimed op ungated.
  const standbySource = readFileSync(join(root, 'orchestrator', 'site', 'scripts', 'standby-queue.mjs'), 'utf8')
  const standbyRecoveryClaimAt = standbySource.indexOf("boundary(directories, 'claim', { candidate: null")
  const standbyOccupancyAt = standbySource.indexOf('taskWriterOccupancyResult()', standbyRecoveryClaimAt)
  const standbyFreshClaimAt = standbySource.indexOf("boundary(directories, 'claim', { candidate: candidate", standbyOccupancyAt)
  assert.ok(standbyRecoveryClaimAt > 0 && standbyOccupancyAt > standbyRecoveryClaimAt && standbyFreshClaimAt > standbyOccupancyAt)
  // Worktree isolation Phase 2: `run` requests are invisible to the standby
  // in BOTH mirrors — the JS peek and the python boundary's oldest-first rule.
  assert.match(standbySource, /run-requires-site-runner/)
  const boundarySource = readFileSync(join(root, 'orchestrator', 'site', 'scripts', 'standby-queue-boundary.py'), 'utf8')
  assert.match(boundarySource, /def _standby_claimable/)
  assert.match(boundarySource, /value\.get\("action"\) == "run"/)
  // Frozen contract prose stays in lockstep with the code.
  const loopContract = readFileSync(join(root, 'orchestrator', 'contracts', 'orchestrator-loop.md'), 'utf8')
  assert.match(loopContract, /`MAX_PARALLEL=2` \(canary; source constant, no env override\)/)
  assert.doesNotMatch(loopContract, /clamp 1–16/)
  const runLoop = readFileSync(join(root, 'orchestrator', 'skills', 'task-orchestrator', 'references', 'run-loop.md'), 'utf8')
  assert.match(runLoop, /canary value `MAX_PARALLEL=2`/)
  // Behavioral proof: the environment cannot raise the cap.
  const probe = spawnSync(process.execPath,
    ['-e', 'console.log(require(process.argv[1]).MAX_PARALLEL)', runnerPath],
    { env: { ...process.env, RUNNER_MAX_PARALLEL: '8' }, encoding: 'utf8' })
  assert.equal(probe.status, 0, probe.stderr)
  assert.equal(probe.stdout.trim(), '2')
})

check('site child receives only its exact in-memory writer delegation capability', () => {
  const source = readFileSync(join(root, 'orchestrator', 'site', 'server', 'sessions.js'), 'utf8')
  assert.match(source, /delete sessionEnv\.ORCHESTRATOR_WRITER_LEASE_ID/)
  assert.match(source, /delete sessionEnv\.ORCHESTRATOR_WRITER_DELEGATION_TOKEN/)
  assert.match(source, /sessionEnv\.ORCHESTRATOR_WRITER_LEASE_ID = initialWriterLease\.leaseId/)
  assert.match(source, /sessionEnv\.ORCHESTRATOR_WRITER_DELEGATION_TOKEN = initialWriterLease\.delegationToken/)
  const sidecarBody = source.slice(source.indexOf('function writeSidecar('), source.indexOf('function redactSecrets('))
  assert.doesNotMatch(sidecarBody, /delegationToken|WRITER_DELEGATION_TOKEN|writerLease/)
})

console.log(`runner-contract: ${checks} checks passed`)
