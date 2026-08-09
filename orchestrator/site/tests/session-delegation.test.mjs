#!/usr/bin/env node

import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { once } from 'node:events'
import { mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const root = mkdtempSync(join(tmpdir(), 'session-delegation-'))
const cache = join(root, 'orchestrator', '.cache', 'tasks')
const finalizationsDir = join(cache, 'finalizations')
const runsDir = join(cache, 'runs')
for (const dir of [
  finalizationsDir, runsDir, join(cache, 'creations'), join(cache, 'edits'),
  join(cache, 'locks'), join(cache, 'requests'), join(cache, 'request-reservations')
]) mkdirSync(dir, { recursive: true })

process.env.ORCHESTRATOR_PROJECT_ROOT = root
process.env.ORCHESTRATOR_FINALIZATIONS_DIR = finalizationsDir
process.env.ORCHESTRATOR_RUNS_DIR = runsDir
process.env.ORCHESTRATOR_TASK_CREATIONS_DIR = join(cache, 'creations')
process.env.ORCHESTRATOR_TASK_EDITS_DIR = join(cache, 'edits')
process.env.ORCHESTRATOR_LOCKS_DIR = join(cache, 'locks')
process.env.ORCHESTRATOR_REQUESTS_DIR = join(cache, 'requests')
process.env.ORCHESTRATOR_REQUEST_RESERVATIONS_DIR = join(cache, 'request-reservations')
process.env.ORCHESTRATOR_WRITER_SESSION_ID = 'ws-' + 's'.repeat(32)
process.env.ORCHESTRATOR_WRITER_STEM = 'TASK_999_stale_parent'
process.env.ORCHESTRATOR_WRITER_LEASE_ID = 'wl_' + 'f'.repeat(32)
process.env.ORCHESTRATOR_WRITER_LEASE_TOKEN = 'e'.repeat(48)
process.env.ORCHESTRATOR_WRITER_DELEGATION_TOKEN = 'd'.repeat(48)
process.env.ORCHESTRATOR_TASK_PREP_NO_QUESTIONS = '1'
process.env.NODE_OPTIONS = '--require=/tmp/untrusted-preload.cjs'
process.env.NODE_PATH = '/tmp/untrusted-node-path'
process.env.LD_PRELOAD = '/tmp/untrusted-loader.so'

const childProcess = require('node:child_process')
const originalSpawn = childProcess.spawn
let captured = null
let dummy = null
let spawnCalls = 0
childProcess.spawn = function (command, args, options) {
  spawnCalls++
  captured = { command, args: args.slice(), env: { ...options.env } }
  // A real detached, stdin-consuming process gives updateChildPid a genuine
  // process-start identity and exercises the actual attach path.
  dummy = originalSpawn(process.execPath, [
    '-e',
    "require('readline').createInterface({input:process.stdin}).on('line',()=>process.stdout.write(JSON.stringify({type:'result',result:'done'})+'\\n'));setInterval(()=>{},1000)"
  ], options)
  return dummy
}

let checks = 0
function check(name, fn) { fn(); checks++; console.log(`ok ${checks} - ${name}`) }

try {
  const childEnv = require('../server/child-env.js').childEnv
  check('unrelated child environments scrub writer capabilities and runtime injection hooks', () => {
    const unrelated = childEnv()
    for (const key of [
      'ORCHESTRATOR_WRITER_SESSION_ID', 'ORCHESTRATOR_WRITER_STEM',
      'ORCHESTRATOR_WRITER_LEASE_ID', 'ORCHESTRATOR_WRITER_LEASE_TOKEN',
      'ORCHESTRATOR_WRITER_DELEGATION_TOKEN',
      'ORCHESTRATOR_TASK_PREP_NO_QUESTIONS',
      'NODE_OPTIONS', 'NODE_PATH', 'LD_PRELOAD',
    ]) assert.equal(Object.hasOwn(unrelated, key), false, key)
  })

  check('test-injection seams never reach a child environment', () => {
    process.env.FINALIZE_FAILPOINT = 'after-intent:outcome'
    process.env.FINALIZE_TEST_REPLACE_TODO_BEFORE_OUTCOME_COMMIT = '/tmp/attacker-todo.md'
    try {
      const inherited = childEnv()
      assert.equal(Object.hasOwn(inherited, 'FINALIZE_FAILPOINT'), false)
      assert.equal(Object.hasOwn(inherited, 'FINALIZE_TEST_REPLACE_TODO_BEFORE_OUTCOME_COMMIT'), false)
      // An explicit caller override cannot smuggle one back in either.
      const forced = childEnv({ FINALIZE_FAILPOINT: 'x', FINALIZE_TEST_ANYTHING: 'y', FINALIZE_STATE_DIR: '/state' })
      assert.equal(Object.hasOwn(forced, 'FINALIZE_FAILPOINT'), false)
      assert.equal(Object.hasOwn(forced, 'FINALIZE_TEST_ANYTHING'), false)
      assert.equal(forced.FINALIZE_STATE_DIR, '/state', 'real finalizer configuration still passes')
    } finally {
      delete process.env.FINALIZE_FAILPOINT
      delete process.env.FINALIZE_TEST_REPLACE_TODO_BEFORE_OUTCOME_COMMIT
    }
  })

  const sessions = require('../server/sessions.js')
  const writerLeases = require('../../tasks/writer-leases.cjs')
  const stem = 'TASK_42_delegated_nested_create'
  const key = 'task:' + stem
  let hookCalled = false
  const status = sessions.start(key, {
    stem,
    action: 'prep',
    noQuestions: true,
    prompt: 'delegation environment fixture',
    beforePrompt: () => { hookCalled = true; return true }
  })

  check('real child spawn receives the exact fresh lease/session/stem capability', () => {
    assert.equal(status.running, true, status.error)
    assert.equal(hookCalled, true)
    assert.ok(dummy && Number.isInteger(dummy.pid) && dummy.pid > 0)
    assert.equal(captured.command, 'claude')
    assert.equal(writerLeases.LEASE_ID_RE.test(captured.env.ORCHESTRATOR_WRITER_LEASE_ID), true)
    assert.match(captured.env.ORCHESTRATOR_WRITER_DELEGATION_TOKEN, /^[a-f0-9]{48}$/)
    assert.match(captured.env.ORCHESTRATOR_WRITER_SESSION_ID, /^ws-[A-Za-z0-9._-]{16,159}$/)
    assert.equal(captured.env.ORCHESTRATOR_WRITER_STEM, stem)
    assert.equal(captured.env.ORCHESTRATOR_TASK_PREP_NO_QUESTIONS, '1')
    assert.notEqual(captured.env.ORCHESTRATOR_WRITER_LEASE_ID, 'wl_' + 'f'.repeat(32))
    assert.notEqual(captured.env.ORCHESTRATOR_WRITER_DELEGATION_TOKEN, 'd'.repeat(48))
    assert.notEqual(captured.env.ORCHESTRATOR_WRITER_SESSION_ID, 'ws-' + 's'.repeat(32))
  })

  const writersDir = join(finalizationsDir, '.writers')
  const leaseId = captured.env.ORCHESTRATOR_WRITER_LEASE_ID
  const delegationToken = captured.env.ORCHESTRATOR_WRITER_DELEGATION_TOKEN
  const rowBytes = readFileSync(join(writersDir, leaseId + '.json'), 'utf8')
  const row = JSON.parse(rowBytes)
  const scan = writerLeases.scan(writersDir)

  check('durable row binds the exact child env by hash without plaintext token', () => {
    assert.equal(row.leaseId, leaseId)
    assert.equal(row.sessionId, captured.env.ORCHESTRATOR_WRITER_SESSION_ID)
    assert.equal(row.stem, stem)
    assert.equal(row.childPid, dummy.pid)
    assert.equal(row.delegationHash, 'sha256:' + createHash('sha256').update(delegationToken, 'ascii').digest('hex'))
    assert.equal(rowBytes.includes(delegationToken), false)
    assert.equal(Object.hasOwn(row, 'delegationToken'), false)
    assert.equal(JSON.stringify(scan).includes(delegationToken), false)
    assert.equal(scan.active.some((item) => item.leaseId === leaseId), true)
  })

  check('delegation capability is absent from sidecar and event transcript', () => {
    const artifacts = readdirSync(runsDir).filter((name) => name.endsWith('.json') || name.endsWith('.jsonl'))
    assert.ok(artifacts.length >= 2)
    for (const name of artifacts) {
      const bytes = readFileSync(join(runsDir, name), 'utf8')
      assert.equal(bytes.includes(delegationToken), false, name)
      assert.equal(bytes.includes(leaseId), false, name)
    }
    assert.equal(JSON.stringify(sessions.status(key)).includes(delegationToken), false)
    assert.equal(JSON.stringify(sessions.list()).includes(delegationToken), false)
  })

  for (let attempt = 0; attempt < 100 && sessions.status(key).awaitingTurn; attempt++) {
    await new Promise((resolve) => setTimeout(resolve, 10))
  }
  let warmHookCalled = false
  const warmAccepted = sessions.send(key, 'second warm task turn', {
    action: 'prep',
    beforePrompt: () => { warmHookCalled = true; return true },
  })
  const warmRow = JSON.parse(readFileSync(join(writersDir, leaseId + '.json'), 'utf8'))
  check('warm task turn reuses the exact child environment lease and delegation hash', () => {
    assert.equal(warmAccepted, true)
    assert.equal(warmHookCalled, true)
    assert.equal(spawnCalls, 1)
    assert.equal(warmRow.leaseId, leaseId)
    assert.equal(warmRow.sessionId, captured.env.ORCHESTRATOR_WRITER_SESSION_ID)
    assert.equal(warmRow.delegationHash, row.delegationHash)
    assert.equal(writerLeases.scan(writersDir).active.filter((item) => item.leaseId === leaseId).length, 1)
  })

  sessions.cancel(key)
  if (dummy.exitCode === null && dummy.signalCode === null) await once(dummy, 'close')
  console.log(`session-delegation: ${checks} checks passed`)
} finally {
  childProcess.spawn = originalSpawn
  if (dummy && dummy.exitCode === null && dummy.signalCode === null) {
    try { process.kill(-dummy.pid, 'SIGKILL') } catch {}
  }
  rmSync(root, { recursive: true, force: true })
}
