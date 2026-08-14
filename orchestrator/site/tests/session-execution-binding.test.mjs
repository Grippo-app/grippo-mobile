#!/usr/bin/env node
// Phase 2 execution binding at the session boundary (pipeline improvement 01):
// a task `run` NEVER spawns in the control root — start() refuses without a
// manager-resolved opaque execution generation and refuses a context on any non-run
// action; the spawned child gets cwd = the execution root plus the control
// root re-anchor; the sidecar v3 carries the execution binding.

import assert from 'node:assert/strict'
import { EventEmitter } from 'node:events'
import { mkdirSync, mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const root = mkdtempSync(join(tmpdir(), 'session-exec-'))
const cache = join(root, 'orchestrator', '.cache', 'tasks')
for (const dir of ['runs', 'locks', 'finalizations', 'creations', 'edits']) {
  mkdirSync(join(cache, dir), { recursive: true })
}
const execRoot = join(root, 'exec-worktree')
mkdirSync(execRoot, { recursive: true })

process.env.ORCHESTRATOR_PROJECT_ROOT = root
process.env.ORCHESTRATOR_RUNS_DIR = join(cache, 'runs')
process.env.ORCHESTRATOR_LOCKS_DIR = join(cache, 'locks')
process.env.ORCHESTRATOR_FINALIZATIONS_DIR = join(cache, 'finalizations')
process.env.ORCHESTRATOR_TASK_CREATIONS_DIR = join(cache, 'creations')
process.env.ORCHESTRATOR_TASK_EDITS_DIR = join(cache, 'edits')
// The screenshot-gate run-gate is an orthogonal Phase-0 fence with its own
// documented opt-out; this suite pins execution binding only.
process.env.FIGMA_WIRING_GATE = '0'
process.env.SESSION_LEASE_FAST_PROBE_MS = '5'
process.env.SESSION_LEASE_FAST_DEADLINE_MS = '30'

const cp = require('node:child_process')
const originalSpawn = cp.spawn
const originalProcessKill = process.kill
let lastSpawn = null
let lastChild = null
let syntheticGroupLive = false
process.kill = function (pid, signal) {
  // The production child is detached, but this in-process child stub is not.
  // Refuse only the synthetic process-group signal so signalSessionTree uses
  // child.kill without terminating the test runner itself. Positive PID probes
  // remain real so writer-lease attachment still proves a live process.
  if (pid === -process.pid) {
    if (syntheticGroupLive) return true
    const error = new Error('synthetic child has no detached process group')
    error.code = 'ESRCH'
    throw error
  }
  return originalProcessKill.call(process, pid, signal)
}
cp.spawn = function (command, args, options) {
  lastSpawn = { command, args, options }
  const child = new EventEmitter()
  lastChild = child
  child.pid = process.pid
  // A realistic stdin: sessions.js writes with a completion callback and
  // treats a missing/failed callback as a delivery failure.
  child.stdin = new EventEmitter()
  child.stdin.writable = true
  child.stdin.write = (chunk, encoding, callback) => {
    const done = typeof encoding === 'function' ? encoding : callback
    if (typeof done === 'function') queueMicrotask(() => done(null))
    return true
  }
  child.stdin.end = () => {}
  child.stdout = new EventEmitter(); child.stderr = new EventEmitter()
  child.kill = (signal) => {
    child.pid = null
    child.emit('exit', null, signal)
    return true
  }
  return child
}

let checks = 0
async function check(name, fn) { await fn(); checks++; console.log(`ok ${checks} - ${name}`) }

try {
  const worktreeManager = require('../server/worktree-manager.js')
  const originalSessionExecutionContext = worktreeManager.sessionExecutionContext
  const originalSeal = worktreeManager.seal
  const sessions = require('../server/sessions.js')

  const resolvedContext = {
    worktreeId: 'wt-' + 'ab'.repeat(16),
    runId: 'run-1700000000000-r1',
    executionRoot: execRoot,
    manifestFile: join(cache, 'worktrees', '.manifests', 'wt.json'),
    taskSnapshotFile: join(cache, 'worktrees', '.snapshots', 'abc.md'),
    taskSnapshotHash: 'sha256:' + 'a'.repeat(64),
    baseCommit: 'c'.repeat(40),
    candidateRef: 'refs/heads/orchestrator/task/TASK_7-' + 'ab'.repeat(6) + '/r1',
  }
  const context = { worktreeId: resolvedContext.worktreeId, runId: resolvedContext.runId }
  worktreeManager.sessionExecutionContext = ({ worktreeId, runId, stem, sourceRevision }) => {
    if (worktreeId !== context.worktreeId || runId !== context.runId ||
        stem !== 'TASK_7_probe' || sourceRevision !== 'sha256:' + 'b'.repeat(64)) {
      return { ok: false, code: 'SESSION_EXECUTION_BINDING_INVALID' }
    }
    return { ok: true, context: resolvedContext }
  }

  await check('a task run without an execution context is refused, never spawned in the control root', () => {
    lastSpawn = null
    const result = sessions.start('task:TASK_7_probe', { stem: 'TASK_7_probe', action: 'run', prompt: 'do it' })
    assert.equal(result.running, false)
    assert.equal(result.error, 'execution-context-required')
    assert.equal(lastSpawn, null, 'no child may spawn without a verified worktree')
  })

  await check('a non-run action carrying an execution context is refused', () => {
    lastSpawn = null
    const result = sessions.start('task:TASK_7_probe', {
      stem: 'TASK_7_probe', action: 'prep', prompt: 'prep it', executionContext: context,
    })
    assert.equal(result.running, false)
    assert.equal(result.error, 'execution-context-not-allowed')
    assert.equal(lastSpawn, null)
  })

  await check('a caller cannot forge a raw execution root beside the opaque generation', () => {
    const bad = Object.assign({}, resolvedContext, { executionRoot: execRoot })
    const result = sessions.start('task:TASK_7_probe', {
      stem: 'TASK_7_probe', action: 'run', prompt: 'do it', executionContext: bad,
    })
    assert.equal(result.running, false)
    assert.equal(result.error, 'execution-context-invalid')
  })

  await check('a run context without its exact generation runId is refused', () => {
    const bad = Object.assign({}, context)
    delete bad.runId
    const result = sessions.start('task:TASK_7_probe', {
      stem: 'TASK_7_probe', action: 'run', prompt: 'do it', executionContext: bad,
    })
    assert.equal(result.running, false)
    assert.equal(result.error, 'execution-context-invalid')
    assert.equal(lastSpawn, null)
  })

  await check('an opaque run generation is re-resolved by the manager before spawn', () => {
    lastSpawn = null
    const result = sessions.start('task:TASK_7_probe', {
      stem: 'TASK_7_probe', action: 'run', prompt: 'do it', executionContext: context,
      sourceRevision: 'sha256:' + 'b'.repeat(64),
      beforePrompt: () => true,
    })
    assert.equal(result.running, true, result.error)
    assert.ok(lastSpawn, 'the child must spawn')
    assert.equal(lastSpawn.command, 'claude')
    assert.equal(lastSpawn.options.cwd, execRoot, 'cwd is the isolated worktree')
    const env = lastSpawn.options.env
    assert.equal(env.ORCHESTRATOR_PROJECT_ROOT, root, 'control plane stays anchored at the control root')
    assert.equal(env.ORCHESTRATOR_EXECUTION_ROOT, execRoot)
    assert.equal(env.ORCHESTRATOR_EXECUTION_MANIFEST, resolvedContext.manifestFile)
    assert.equal(env.ORCHESTRATOR_TASK_SNAPSHOT_FILE, resolvedContext.taskSnapshotFile)
    assert.equal(env.ORCHESTRATOR_TASK_SNAPSHOT_HASH, resolvedContext.taskSnapshotHash)
    assert.equal(env.ORCHESTRATOR_WORKTREE_ID, resolvedContext.worktreeId)
    assert.equal(env.ORCHESTRATOR_RUN_ID, resolvedContext.runId)
    sessions.cancel('task:TASK_7_probe')
  })

  await check('candidate sealing waits for exact process-tree death and writer-lease release', async () => {
    let seals = 0
    worktreeManager.seal = () => { seals++; return { ok: false, code: 'TEST_SEAL' } }
    worktreeManager.sessionExecutionContext = ({ worktreeId, runId, stem, sourceRevision }) => {
      if (worktreeId !== context.worktreeId || runId !== context.runId ||
          stem !== 'TASK_8_probe' || sourceRevision !== 'sha256:' + 'b'.repeat(64)) {
        return { ok: false, code: 'SESSION_EXECUTION_BINDING_INVALID' }
      }
      return { ok: true, context: { ...resolvedContext, candidateRef: resolvedContext.candidateRef.replace('TASK_7', 'TASK_8') } }
    }
    syntheticGroupLive = true
    const started = sessions.start('task:TASK_8_probe', {
      stem: 'TASK_8_probe', action: 'run', prompt: 'do it', executionContext: context,
      sourceRevision: 'sha256:' + 'b'.repeat(64),
    })
    assert.equal(started.running, true, started.error)
    lastChild.emit('exit', 0, null)
    lastChild.emit('close')
    await new Promise((resolve) => setTimeout(resolve, 15))
    assert.equal(seals, 0, 'a detached writer can still change the candidate tree')
    syntheticGroupLive = false
    await new Promise((resolve) => setTimeout(resolve, 30))
    assert.equal(seals, 1, 'seal starts only after process-tree death and exact lease release')
  })

  await check('the sidecar validator enforces a complete-or-absent execution binding', () => {
    const modulePath = require.resolve('../server/sessions.js')
    const source = require('node:fs').readFileSync(modulePath, 'utf8')
    // v3 added runId, so a sidecar can be traced back to the queue request and
    // the generation record. The version is a hard constant: an older sidecar
    // is rejected outright, never upgraded.
    assert.match(source, /var SESSION_SIDECAR_VERSION = 3;/)
    assert.match(source, /'runId'/)
    assert.match(source, /sidecar execution binding must be complete or absent/)
    assert.match(source, /sidecar execution binding is only legal for task run sessions/)
    // The runner is the only execution-context producer: sessions.start
    // refuses HTTP-origin cwd and there is no shared-root run fallback.
    assert.match(source, /execution-context-required/)
    assert.match(source, /sessionExecutionContext/)
    assert.match(source, /cwd: executionContext \? executionContext\.executionRoot : PROJECT_ROOT/)
  })

  worktreeManager.sessionExecutionContext = originalSessionExecutionContext
  worktreeManager.seal = originalSeal

  console.log(`session-execution-binding: ${checks} checks passed`)
} finally {
  try { require('../server/sessions.js').killAll() } catch {}
  cp.spawn = originalSpawn
  process.kill = originalProcessKill
  rmSync(root, { recursive: true, force: true })
}
