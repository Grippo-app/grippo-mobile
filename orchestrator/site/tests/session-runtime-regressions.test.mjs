#!/usr/bin/env node

import assert from 'node:assert/strict'
import { EventEmitter } from 'node:events'
import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { PassThrough } from 'node:stream'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const root = mkdtempSync(join(tmpdir(), 'session-runtime-regressions-'))
const cache = join(root, 'orchestrator', '.cache', 'tasks')
const runs = join(cache, 'runs')
for (const name of ['runs', 'finalizations', 'locks', 'requests', 'request-reservations', 'creations', 'edits', 'intake']) {
  mkdirSync(join(cache, name), { recursive: true })
}
mkdirSync(join(root, 'orchestrator', 'figma'), { recursive: true })
process.env.ORCHESTRATOR_PROJECT_ROOT = root
process.env.ORCHESTRATOR_RUNS_DIR = runs
process.env.ORCHESTRATOR_FINALIZATIONS_DIR = join(cache, 'finalizations')
process.env.ORCHESTRATOR_LOCKS_DIR = join(cache, 'locks')
process.env.ORCHESTRATOR_REQUESTS_DIR = join(cache, 'requests')
process.env.ORCHESTRATOR_REQUEST_RESERVATIONS_DIR = join(cache, 'request-reservations')
process.env.ORCHESTRATOR_TASK_CREATIONS_DIR = join(cache, 'creations')
process.env.ORCHESTRATOR_TASK_EDITS_DIR = join(cache, 'edits')
process.env.ORCHESTRATOR_TASK_INTAKE_DIR = join(cache, 'intake')

const childProcess = require('node:child_process')
const originalSpawn = childProcess.spawn
let spawnMode = 'throw'
let spawnCount = 0
let child = null
let lastSpawn = null
childProcess.spawn = function (command, args, options) {
  spawnCount++
  if (spawnMode === 'throw') throw new Error('synthetic spawn failure')
  child = new EventEmitter()
  child.pid = 99999999
  child.stdout = new PassThrough()
  child.stderr = new PassThrough()
  child.stdinLines = []
  if (spawnMode === 'delayed-stdin-failure') {
    child.stdin = new EventEmitter()
    child.stdin.writable = true
    child.stdin.write = (chunk, callback) => {
      child.stdinLines.push(chunk.toString()); child.stdinCallback = callback; return true
    }
    child.stdin.end = () => { child.stdin.writable = false }
  } else {
    child.stdin = new PassThrough()
    child.stdin.on('data', (chunk) => child.stdinLines.push(chunk.toString()))
  }
  child.kill = () => true
  lastSpawn = { command, args: args.slice(), options: { ...options, env: { ...options.env } } }
  return child
}

let checks = 0
function check(name, fn) { fn(); checks++; console.log(`ok ${checks} - ${name}`) }
async function checkAsync(name, fn) { await fn(); checks++; console.log(`ok ${checks} - ${name}`) }

try {
  const sessions = require('../server/sessions.js')

  check('startup interrupts current-schema orphaned runs', () => {
    const startedAt = '2026-07-14T10:37:50.329Z'
    const finishedAt = '2026-07-14T10:38:16.232Z'
    // The CURRENT schema, whatever it is: this check is about interrupting an
    // orphaned run, and a fixture pinned to a superseded version would be
    // rejected for the wrong reason and prove nothing.
    const sidecar = (key, stem, running) => ({
        version: 3,
        key, stem, action: null, dedupKey: null, dedupReport: null,
        running, awaitingTurn: running, startedAt, endedAt: running ? null : finishedAt,
        exitCode: running ? null : 0, canceled: false, sessionId: null, nextSeq: 3, minSeq: 0,
        worktreeId: null, runId: null, executionRoot: null, baseCommit: null, candidateRef: null,
      })
    const fixtures = [
      ['task_TASK_1_current_finished.session.json', sidecar('task:TASK_1_current_finished', 'TASK_1_current_finished', false)],
      ['figma_sync-components.session.json', sidecar('figma:sync-components', null, true)],
    ]
    fixtures.forEach(([name, value]) => writeFileSync(join(runs, name), JSON.stringify(value) + '\n'))

    sessions.init()

    const persisted = fixtures.map(([name]) => JSON.parse(readFileSync(join(runs, name), 'utf8')))
    const interrupted = persisted[1]
    persisted.forEach((value) => {
      assert.equal(value.version, 3)
      assert.equal(Object.hasOwn(value, 'dedupKey'), true)
      assert.equal(Object.hasOwn(value, 'dedupReport'), true)
      assert.equal(value.worktreeId, null)
      assert.equal(value.executionRoot, null)
      assert.equal(value.running, false)
      assert.ok(value.endedAt)
    })
    assert.equal(interrupted.running, false)
    assert.equal(interrupted.awaitingTurn, false)
    assert.ok(interrupted.endedAt)
    assert.deepEqual(sessions.scanIntegrity().findings, [])
    fixtures.forEach(([name]) => rmSync(join(runs, name)))
  })

  check('startup rejects a superseded sidecar without rewriting it', () => {
    const file = join(runs, 'task_TASK_2_superseded.session.json')
    const bytes = JSON.stringify({
      version: 1,
      key: 'task:TASK_2_superseded', stem: 'TASK_2_superseded', action: null,
      dedupKey: null, dedupReport: null, running: false, awaitingTurn: false,
      startedAt: '2026-07-14T10:37:50.329Z', endedAt: '2026-07-14T10:38:16.232Z',
      exitCode: 0, canceled: false, sessionId: null, nextSeq: 3, minSeq: 0,
    }) + '\n'
    writeFileSync(file, bytes)

    sessions.init()

    assert.equal(readFileSync(file, 'utf8'), bytes)
    assert.equal(sessions.scanIntegrity().findings.some((finding) =>
      finding.code === 'SESSION_SIDECAR_INVALID' && finding.paths.includes(file)), true)
    rmSync(file)
  })

  check('startup leaves a malformed sidecar fail-closed', () => {
    const file = join(runs, 'task_TASK_2_malformed_extra.session.json')
    writeFileSync(file, JSON.stringify({
      key: 'task:TASK_2_malformed_extra', stem: 'TASK_2_malformed_extra', action: null,
      dedupKey: null, dedupReport: null, running: false, awaitingTurn: false,
      startedAt: '2026-07-14T10:37:50.329Z', endedAt: '2026-07-14T10:38:16.232Z',
      exitCode: 0, canceled: false, sessionId: null, nextSeq: 3, minSeq: 0,
      unexpected: true,
    }) + '\n')
    sessions.init()
    assert.equal(Object.hasOwn(JSON.parse(readFileSync(file, 'utf8')), 'version'), false)
    assert.equal(sessions.scanIntegrity().findings.some((finding) => finding.code === 'SESSION_SIDECAR_INVALID'), true)
    rmSync(file)
  })

  check('writer lease keys stay per-session except the sweep aggregate', () => {
    assert.equal(sessions.writerLeaseKeyFor('figma:shipdriftsweep'), 'figma:ship-drift-artifacts')
    assert.equal(sessions.writerLeaseKeyFor('figma:screens:TASK_1_x'), 'figma:screens:TASK_1_x')
  })

  check('an exact generation-bound Outcome draft bypasses the fresh-lock auto-close defer', () => {
    const stem = 'TASK_9_ready_handoff'
    const worktreeId = 'wt-' + 'a'.repeat(32)
    const lock = join(cache, 'locks', stem + '.json')
    const activity = join(runs, 'task_' + stem + '.session.json')
    const draft = join(cache, 'finalizations', stem + '.' + worktreeId + '.draft.md')
    const session = {
      key: 'task:' + stem,
      stem,
      action: 'run',
      executionContext: { worktreeId },
    }
    writeFileSync(lock, '{}\n')
    writeFileSync(activity, '{}\n')

    assert.equal(sessions.freshLockBlocksAutoClose(session), true,
      'a fresh task lock still defers an intermediate run turn')
    writeFileSync(join(cache, 'finalizations', stem + '.wt-' + 'b'.repeat(32) + '.draft.md'), 'foreign\n')
    assert.equal(sessions.freshLockBlocksAutoClose(session), true,
      'another generation cannot close this run')
    writeFileSync(draft, '---\n\n## Outcome\n')
    assert.equal(sessions.completionHandoffReady(session), true)
    assert.equal(sessions.freshLockBlocksAutoClose(session), false,
      'the exact handoff must let the manager close and seal the child')
    assert.equal(sessions.completionHandoffReady({ ...session, action: 'prepare' }), false)
  })

  check('Figma session keys are an exact action allowlist (retired token sessions rejected)', () => {
    for (const key of [
      'figma:whoami', 'figma:fileaccess', 'figma:sync-tokens', 'figma:sync-components',
      'figma:shipdriftsweep', 'figma:screens:TASK_1_x', 'figma:rebundle:TASK_1_x',
    ]) assert.equal(sessions.validSessionKey(key), true, key)
    for (const key of [
      'figma:tokens', 'figma:derive', 'figma:componentdrift', 'figma:screens', 'figma:overwrite',
      'figma:rebundle', 'figma:tokens:normalize', 'figma:screens:../x',
      'figma:unknown', 'figma:extract', 'task:../unsafe',
      'figma:variables', 'figma:styles', 'figma:tokendrift', 'figma:sync-drift',
    ]) {
      assert.equal(sessions.validSessionKey(key), false, key)
    }
  })

  check('hostile persisted keys neither crash list nor mutate its prototype', () => {
    for (const [name, key] of [['a', 'hasOwnProperty'], ['b', '__proto__'], ['c', 'normal']]) {
      writeFileSync(join(runs, name + '.session.json'), JSON.stringify({
        key, running: false, startedAt: null, endedAt: null, nextSeq: 0, minSeq: 0,
      }) + '\n')
    }
    const listed = sessions.list()
    assert.equal(Object.getPrototypeOf(listed), Object.prototype)
    assert.equal(Object.hasOwn(listed, 'hasOwnProperty'), false)
    assert.equal(Object.hasOwn(listed, '__proto__'), false)
    assert.equal(Object.hasOwn(listed, 'normal'), false)
  })

  check('spawn failure preserves the prior transcript byte-for-byte', () => {
    const transcript = join(runs, 'setup.events.jsonl')
    const old = Buffer.from('{"seq":0,"kind":"system","text":"old"}\n')
    writeFileSync(transcript, old)
    const status = sessions.start('setup', { prompt: 'must fail before publication' })
    assert.equal(status.running, false)
    assert.equal(status.error, 'session-spawn-failed')
    assert.deepEqual(readFileSync(transcript), old)
  })

  check('a closing live predecessor blocks same-key replacement until exit proof', () => {
    spawnMode = 'live'
    const started = sessions.start('setup')
    assert.equal(started.running, true, started.error)
    assert.equal(sessions.cancel('setup'), true)
    const before = spawnCount
    const refused = sessions.start('setup')
    assert.equal(refused.running, false)
    assert.match(refused.error, /writer-termination-pending/)
    assert.equal(spawnCount, before)
    child.emit('exit', 0, 'SIGTERM')
    child.emit('close', 0, 'SIGTERM')
  })

  await checkAsync('task-surface publication is proven before lease release and canceled turns are aborted', async () => {
    const finalizations = require('../server/finalizations.js')
    const originalBeginMutation = finalizations.beginMutation
    const originalAttachMutationChild = finalizations.attachMutationChild
    const originalEndMutation = finalizations.endMutation
    const originalRetainMutation = finalizations.retainMutation
    const events = []
    try {
      finalizations.beginMutation = () => ({ ok: true, handle: { leaseId: 'fixture', delegationToken: 'fixture-token' } })
      finalizations.attachMutationChild = () => ({ ok: true })
      finalizations.endMutation = () => { events.push(['release']); return true }
      finalizations.retainMutation = () => true
      sessions.configureTurnPublication({
        prepareTurn(info) {
          events.push(['prepare', info.key, info.action, info.stem])
          return 'publication-fixture'
        },
        markResult(publicationId, success) {
          events.push(['mark', publicationId, success])
          return { ready: true }
        },
        dispatch(publicationId) { events.push(['dispatch', publicationId]) },
        abort(publicationId) { events.push(['abort', publicationId]) },
      })
      spawnMode = 'live'
      const key = 'figma:screens:TASK_1_surface'
      const started = sessions.start(key, {
        stem: 'TASK_1_surface', action: 'screen-pull', prompt: 'fixture screen pull'
      })
      assert.equal(started.running, true, started.error)
      await new Promise((resolve) => setImmediate(resolve))
      child.stdout.write(JSON.stringify({ type: 'result', subtype: 'success', is_error: false, result: 'done' }) + '\n')
      await new Promise((resolve) => setImmediate(resolve))
      assert.deepEqual(events.slice(0, 4), [
        ['prepare', key, 'screen-pull', 'TASK_1_surface'],
        ['mark', 'publication-fixture', true],
        ['release'],
        ['dispatch', 'publication-fixture'],
      ])
      child.emit('exit', 0, null)
      child.emit('close', 0, null)

      events.length = 0
      const canceledKey = 'figma:screens:TASK_2_surface'
      const canceled = sessions.start(canceledKey, {
        stem: 'TASK_2_surface', action: 'screen-drift', prompt: 'fixture screen drift'
      })
      assert.equal(canceled.running, true, canceled.error)
      assert.equal(sessions.cancel(canceledKey), true)
      assert.deepEqual(events, [
        ['prepare', canceledKey, 'screen-drift', 'TASK_2_surface'],
        ['abort', 'publication-fixture'],
      ])
      child.emit('exit', 0, 'SIGTERM')
      child.emit('close', 0, 'SIGTERM')
    } finally {
      sessions.configureTurnPublication(null)
      finalizations.beginMutation = originalBeginMutation
      finalizations.attachMutationChild = originalAttachMutationChild
      finalizations.endMutation = originalEndMutation
      finalizations.retainMutation = originalRetainMutation
    }
  })

  await checkAsync('a result racing a late stdin failure aborts the ready publication before lease settlement', async () => {
    const finalizations = require('../server/finalizations.js')
    const originalBeginMutation = finalizations.beginMutation
    const originalAttachMutationChild = finalizations.attachMutationChild
    const originalEndMutation = finalizations.endMutation
    const originalRetainMutation = finalizations.retainMutation
    const events = []
    try {
      finalizations.beginMutation = () => ({ ok: true, handle: { leaseId: 'stdin-race', delegationToken: 'stdin-race-token' } })
      finalizations.attachMutationChild = () => ({ ok: true })
      finalizations.endMutation = () => { events.push(['release']); return true }
      finalizations.retainMutation = () => true
      sessions.configureTurnPublication({
        prepareTurn() { events.push(['prepare']); return 'stdin-race-publication' },
        markResult(publicationId, success) { events.push(['mark', publicationId, success]); return { ready: true } },
        dispatch(publicationId) { events.push(['dispatch', publicationId]) },
        abort(publicationId) { events.push(['abort', publicationId]) },
      })
      spawnMode = 'delayed-stdin-failure'
      const key = 'figma:screens:TASK_3_stdin_race'
      const started = sessions.start(key, {
        stem: 'TASK_3_stdin_race', action: 'screen-pull', prompt: 'fixture delayed write'
      })
      assert.equal(started.running, true, started.error)
      assert.equal(typeof child.stdinCallback, 'function')
      child.stdout.write(JSON.stringify({ type: 'result', subtype: 'success', is_error: false, result: 'done' }) + '\n')
      await new Promise((resolve) => setImmediate(resolve))
      assert.deepEqual(events, [
        ['prepare'],
        ['mark', 'stdin-race-publication', true],
      ])
      child.stdinCallback(new Error('synthetic EPIPE'))
      await new Promise((resolve) => setImmediate(resolve))
      assert.deepEqual(events.slice(0, 3), [
        ['prepare'],
        ['mark', 'stdin-race-publication', true],
        ['abort', 'stdin-race-publication'],
      ])
      child.emit('exit', 1, 'SIGTERM')
      child.emit('close', 1, 'SIGTERM')
      await new Promise((resolve) => setImmediate(resolve))
      assert.equal(events.some((event) => event[0] === 'dispatch'), false)
    } finally {
      sessions.configureTurnPublication(null)
      finalizations.beginMutation = originalBeginMutation
      finalizations.attachMutationChild = originalAttachMutationChild
      finalizations.endMutation = originalEndMutation
      finalizations.retainMutation = originalRetainMutation
      spawnMode = 'live'
    }
  })

  await checkAsync('runtime-only whoami delivers its initial prompt without weakening writer leases or receipt validation', async () => {
    const finalizations = require('../server/finalizations.js')
    const figma = require('../server/figma.js')
    const actions = require('../server/figma-session-actions.js')
    const originalBeginMutation = finalizations.beginMutation
    const connectedAt = new Date().toISOString()
    figma._test.installSnapshot({
      state: 'connected',
      local: { present: true, status: 'connected', url: 'https://mcp.figma.com/mcp' },
      global: { present: false, name: null, status: 'unknown' },
      checkedAt: connectedAt
    })
    const verificationNonce = figma.status().verificationNonce
    assert.match(verificationNonce, /^[a-f0-9]{32}$/)
    const resolved = await actions.resolveServerAction('figma:whoami', 'whoami', {
      figmaFileKey: '',
      verificationNonce
    })
    assert.equal(resolved.ok, true, resolved.error)

    let beginCalls = 0
    try {
      finalizations.beginMutation = function () {
        beginCalls++
        return { ok: false, error: 'writer-lease-fixture-refused', detail: 'fixture writer is busy' }
      }
      spawnMode = 'live'
      const beforeRuntimeSpawn = spawnCount
      const started = sessions.start('figma:whoami', {
        action: resolved.action,
        prompt: resolved.prompt,
        runtimeOnly: true
      })
      assert.equal(started.running, true, started.error)
      assert.equal(started.canceled, false)
      assert.equal(spawnCount, beforeRuntimeSpawn + 1)
      assert.equal(beginCalls, 0, 'runtime-only initial prompt must not request a workspace writer lease')
      assert.equal(lastSpawn.command, 'claude')
      assert.equal(Object.hasOwn(lastSpawn.options.env, 'ORCHESTRATOR_WRITER_LEASE_ID'), false)
      await new Promise((resolve) => setImmediate(resolve))
      const delivered = child.stdinLines.join('').trim().split('\n').map((line) => JSON.parse(line))
      assert.equal(delivered.length, 1)
      assert.equal(delivered[0].type, 'user')
      assert.equal(delivered[0].message.content[0].text, resolved.prompt)
      assert.equal(sessions.status('figma:whoami').canceled, false)
      child.stdout.write(JSON.stringify({
        type: 'system',
        needs_action: 'Authenticate the Figma MCP connector'
      }) + '\n')
      await new Promise((resolve) => setImmediate(resolve))
      assert.equal(sessions.status('figma:whoami').askedThisTurn, false,
        'runtime-only probes cannot wait for an interactive answer')
      assert.equal(sessions.status('figma:whoami').awaitingTurn, false)
      await new Promise((resolve) => setTimeout(resolve, 900))
      assert.equal(sessions.status('figma:whoami').closing, true,
        'an authentication needs_action must close the probe instead of pinning Figma check')
      child.emit('exit', 0, null)
      child.emit('close', 0, null)

      const beforeMutatingSpawn = spawnCount
      const invalidBypass = sessions.start('figma:shipdriftsweep', {
        action: 'ship-drift-sweep',
        prompt: 'mutating fixture prompt',
        runtimeOnly: true
      })
      assert.equal(invalidBypass.running, false)
      assert.match(invalidBypass.error, /runtime-only-contract-invalid/)
      assert.equal(beginCalls, 0, 'mutating actions cannot opt out of writer lease acquisition')
      assert.equal(spawnCount, beforeMutatingSpawn, 'an invalid runtime-only contract must block process spawn')

      for (const invalidMeta of [
        { action: 'component-drift', prompt: resolved.prompt, runtimeOnly: true },
        { action: 'whoami', prompt: resolved.prompt, runtimeOnly: true, resume: 'prior-session-id' },
        { action: 'whoami', prompt: resolved.prompt, runtimeOnly: true, beforePrompt: () => true },
        { action: 'whoami', runtimeOnly: true },
      ]) {
        const invalidContract = sessions.start('figma:whoami', invalidMeta)
        assert.equal(invalidContract.running, false)
        assert.match(invalidContract.error, /runtime-only-contract-invalid/)
        assert.equal(beginCalls, 0)
        assert.equal(spawnCount, beforeMutatingSpawn)
      }

      const refused = sessions.start('figma:shipdriftsweep', { action: 'ship-drift-sweep', prompt: 'mutating fixture prompt' })
      assert.equal(refused.running, false)
      assert.equal(refused.error, 'workspace-writer-lease-refused')
      assert.equal(beginCalls, 1, 'ordinary initial prompts must pass through writer lease acquisition')
      assert.equal(spawnCount, beforeMutatingSpawn, 'a refused writer lease must block process spawn')
    } finally {
      finalizations.beginMutation = originalBeginMutation
    }

    const accountFile = join(root, 'orchestrator', 'figma', '.account.json')
    const currentReceipt = {
      handle: 'fixture-user',
      email: 'fixture@example.test',
      tier: 'professional',
      seat: 'dev',
      checkedAt: new Date().toISOString(),
      verificationNonce
    }
    mkdirSync(dirname(accountFile), { recursive: true })
    writeFileSync(accountFile, JSON.stringify({ ...currentReceipt, unexpected: true }) + '\n')
    assert.equal(figma.account(), null, 'invalid receipt shape must fail closed')
    writeFileSync(accountFile, JSON.stringify({ ...currentReceipt, verificationNonce: '0'.repeat(32) }) + '\n')
    assert.equal(figma.account(), null, 'receipt from an older verification nonce must fail closed')
    writeFileSync(accountFile, JSON.stringify({ ...currentReceipt, checkedAt: '2020-01-01T00:00:00.000Z' }) + '\n')
    assert.equal(figma.account(), null, 'receipt older than the current connector episode must fail closed')
    writeFileSync(accountFile, JSON.stringify({ ...currentReceipt, checkedAt: new Date(Date.now() + 6 * 60 * 1000).toISOString() }) + '\n')
    assert.equal(figma.account(), null, 'receipt too far in the future must fail closed')
    writeFileSync(accountFile, JSON.stringify(currentReceipt) + '\n')
    assert.deepEqual(figma.account(), {
      handle: 'fixture-user',
      email: 'fixture@example.test',
      tier: 'professional',
      seat: 'dev',
      checkedAt: currentReceipt.checkedAt
    })
  })

  console.log(`session-runtime-regressions: ${checks} checks passed`)
} finally {
  childProcess.spawn = originalSpawn
  rmSync(root, { recursive: true, force: true })
}
