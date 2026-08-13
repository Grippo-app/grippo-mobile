#!/usr/bin/env node

import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, unlinkSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join, resolve } from 'node:path'
import { createRequire } from 'node:module'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const TASK_SCRIPTS = resolve(HERE, '../../tasks')
const CREATE = join(TASK_SCRIPTS, 'create-backlog.py')
const EDIT = join(TASK_SCRIPTS, 'edit-backlog.py')
const REGEN = join(TASK_SCRIPTS, 'regen-index.py')
const root = mkdtempSync(join(tmpdir(), 'publication-recovery-'))
const tasks = join(root, 'orchestrator', 'tasks')
const cache = join(root, 'orchestrator', '.cache', 'tasks')
const creations = join(cache, 'creations')
const edits = join(cache, 'edits')
const finalizationsDir = join(cache, 'finalizations')
const writers = join(finalizationsDir, '.writers')
for (const column of ['backlog', 'pending', 'todo', 'done']) mkdirSync(join(tasks, column), { recursive: true })
for (const dir of [creations, edits, finalizationsDir]) mkdirSync(dir, { recursive: true })
writeFileSync(join(tasks, 'INDEX.json'), JSON.stringify({
  version: 2, generatedAt: '1970-01-01T00:00:00Z', backlog: [], pending: [], todo: [], done: []
}, null, 2) + '\n')

Object.assign(process.env, {
  PYTHONDONTWRITEBYTECODE: '1',
  CREATE_BACKLOG_PROJECT_ROOT: root,
  CREATE_BACKLOG_TASKS_DIR: tasks,
  CREATE_BACKLOG_CACHE_DIR: cache,
  CREATE_BACKLOG_REGEN_INDEX: REGEN,
  CREATE_BACKLOG_MUTEX_TIMEOUT_MS: '10000',
  EDIT_BACKLOG_EDITS_DIR: edits,
  EDIT_BACKLOG_FINALIZATIONS_DIR: finalizationsDir,
  EDIT_BACKLOG_TEST_ALLOW_UNLEASED: '1',
  ORCHESTRATOR_PROJECT_ROOT: root,
  ORCHESTRATOR_TASKS_DIR: tasks,
  ORCHESTRATOR_TASK_CREATIONS_DIR: creations,
  ORCHESTRATOR_TASK_CREATIONS_AUTHORITY_ROOT: root,
  ORCHESTRATOR_TASK_EDITS_DIR: edits,
  ORCHESTRATOR_TASK_EDITS_AUTHORITY_ROOT: root,
  ORCHESTRATOR_FINALIZATIONS_DIR: finalizationsDir,
  ORCHESTRATOR_WRITER_AUTHORITY_ROOT: root
})

const require = createRequire(import.meta.url)
const backlogCreate = require('../server/backlog-create.js')
const creationMarkers = require('../server/creation-markers.js')
const editMarkers = require('../server/edit-markers.js')
const finalizations = require('../server/finalizations.js')
const writerLeases = require('../../tasks/writer-leases.cjs')
const taskSource = require('../../tasks/task-source-contract.cjs')
const SOURCE = taskSource.manualForIntent('publication-recovery-fixture', 'manual', 'fixture:publication-recovery')
const SOURCE_BLOCK = taskSource.render(SOURCE)
const taskMarkdown = (title, body) => `# TASK 1 — ${title}\n\n${SOURCE_BLOCK}\n\n${body}\n`

const sha = (bytes) => 'sha256:' + createHash('sha256').update(bytes).digest('hex')
const baseEnv = (extra = {}) => ({ ...process.env, ...extra })
function regen(check = false) {
  const result = spawnSync('python3', [REGEN].concat(check ? ['--check'] : []), { cwd: root, env: baseEnv(), encoding: 'utf8' })
  assert.equal(result.status, 0, result.stderr)
}
function crashEdit(markdown, failpoint = 'after-marker') {
  const stem = 'TASK_1_recovery_base'
  const before = readFileSync(join(tasks, 'backlog', stem + '.md'))
  const result = spawnSync('python3', [EDIT], {
    cwd: root, env: baseEnv({ EDIT_BACKLOG_FAILPOINT: failpoint }), encoding: 'utf8',
    input: JSON.stringify({ version: 1, stem, expectedSourceHash: sha(before), markdown })
  })
  assert.equal(result.status, 87, result.stderr)
  assert.equal(result.stdout.trim(), 'READY')
}
function crashCreate(key = 'publication.recovery.create.0001') {
  const result = spawnSync('python3', [CREATE], {
    cwd: root, env: baseEnv({ CREATE_BACKLOG_FAILPOINT: 'after-marker', CREATE_BACKLOG_TEST_ALLOW_UNLEASED: '1' }), encoding: 'utf8',
    input: JSON.stringify({ version: 1, title: 'Recovered creation', body: '## Goal\n\nCreate deterministically.\n', key,
      originStem: null, dedupKey: null, dedupReport: null, source: SOURCE })
  })
  assert.equal(result.status, 86, result.stderr)
  assert.equal(result.stdout.trim(), 'READY')
}
async function waitFor(fn, timeout = 5000) {
  const deadline = Date.now() + timeout
  while (Date.now() < deadline) {
    const value = fn()
    if (value) return value
    await new Promise((resolveWait) => setTimeout(resolveWait, 20))
  }
  assert.fail('timed out waiting for publication recovery')
}
let checks = 0
async function check(name, fn) { await fn(); checks++; console.log(`ok ${checks} - ${name}`) }

try {
  const original = taskMarkdown('Recovery base', '## Goal\n\nOriginal.')
  writeFileSync(join(tasks, 'backlog', 'TASK_1_recovery_base.md'), original)
  regen()

  await check('edit validation measures the canonical newline-normalized durable envelope', () => {
    const stem = 'TASK_1_recovery_base'
    const expectedSourceHash = 'sha256:' + 'a'.repeat(64)
    const heading = '# TASK 1 — Recovery base\n\n' + SOURCE_BLOCK + '\n\n'
    const exactWithoutNewline = heading + 'x'.repeat(backlogCreate.TASK_STORAGE_MAX_BYTES - 1 - Buffer.byteLength(heading))
    const accepted = backlogCreate.validateEditInput({ stem, expectedSourceHash, markdown: exactWithoutNewline })
    assert.equal(Buffer.byteLength(accepted.markdown), backlogCreate.TASK_STORAGE_MAX_BYTES)
    assert.ok(accepted.markdown.endsWith('\n'))
    const exactWithNewline = heading + 'x'.repeat(backlogCreate.TASK_STORAGE_MAX_BYTES - Buffer.byteLength(heading) - 1) + '\n'
    assert.equal(Buffer.byteLength(backlogCreate.validateEditInput({ stem, expectedSourceHash, markdown: exactWithNewline }).markdown), backlogCreate.TASK_STORAGE_MAX_BYTES)
    assert.throws(
      () => backlogCreate.validateEditInput({ stem, expectedSourceHash, markdown: exactWithoutNewline + 'x' }),
      (error) => error && error.code === 'bad-markdown'
    )
    assert.throws(() => backlogCreate.validateEditInput({ stem: 'TASK_0_zero', expectedSourceHash, markdown: '# TASK 0 — Zero\n' }), (error) => error && error.code === 'bad-stem')
    assert.throws(() => backlogCreate.validateEditInput({ stem: 'TASK_9007199254740992_huge', expectedSourceHash, markdown: '# TASK 9007199254740992 — Huge\n' }), (error) => error && error.code === 'bad-stem')
  })

  await check('create preflight mirrors durable Unicode, control and metadata boundaries', () => {
    const base = { body: 'Goal.\n', idempotencyKey: 'publication.recovery.validation.0001', originStem: null, dedupKey: null, dedupReport: null, source: SOURCE }
    assert.equal(backlogCreate.validateInput({ ...base, title: 'a'.repeat(200) }).title.length, 200)
    for (const title of ['a'.repeat(201), 'bad\ud800', 'line\u2028break']) {
      assert.throws(() => backlogCreate.validateInput({ ...base, title }), (error) => error && error.code === 'bad-title')
    }
    for (const body of ['bad\ud800', 'bad\0body', 'bad\u0001body']) {
      assert.throws(() => backlogCreate.validateInput({ ...base, title: 'Valid', body }), (error) => error && error.code === 'bad-body')
    }
    assert.throws(() => backlogCreate.validateInput({ ...base, title: 'Origin conflict', originStem: 'TASK_1_parent', body: '## Origin\n- existing\n' }), (error) => error && error.code === 'origin-conflict')
    assert.throws(() => backlogCreate.validateInput({ ...base, title: 'Report only', dedupReport: 'sha256:' + 'a'.repeat(64) }), (error) => error && error.code === 'bad-dedup-report')
    assert.throws(() => backlogCreate.validateInput({ ...base, title: 'Missing source', source: null }), (error) => error && error.code === 'bad-task-source')
  })

  await check('combined authority resolves simultaneous valid create and edit WALs in deterministic order', async () => {
    const edited = taskMarkdown('Recovery base edited', '## Goal\n\nRecovered edit.')
    crashEdit(edited)
    crashCreate()
    assert.equal(creationMarkers.scan().incomplete.length, 1)
    assert.equal(editMarkers.scan().incomplete.length, 1)

    const result = await backlogCreate.recoverPublications()
    assert.equal(result.creations.recoveredCount, 1)
    assert.equal(result.edits.recoveredCount, 1)
    assert.equal(creationMarkers.scan().incomplete.length, 0)
    assert.equal(editMarkers.scan().incomplete.length, 0)
    assert.equal(readFileSync(join(tasks, 'backlog', 'TASK_1_recovery_base.md'), 'utf8'), edited)
    assert.equal(existsSync(join(tasks, 'backlog', 'TASK_2_recovered_creation.md')), true)
    regen(true)
  })

  await check('a phase-boundary helper failure leaves durable state retryable and the next combined pass is idempotent', async () => {
    const edited = taskMarkdown('Phase boundary retry', '## Goal\n\nResume the second phase automatically.')
    crashEdit(edited)
    crashCreate('publication.recovery.create.phase-boundary.0002')
    // Keep the helper alive through stdin delivery, then return one canonical
    // failure. A missing script races Python startup against stdin.end(): some
    // Node/platform combinations report spawn/attach failure while others
    // surface EPIPE first, so it cannot pin the recovery phase deterministically.
    const failingHelper = join(root, 'failing-edit-helper.py')
    writeFileSync(failingHelper, [
      'import json, sys',
      'print("READY", flush=True)',
      'sys.stdin.buffer.read()',
      'print(json.dumps({"ok": False, "error": {"code": "EDIT_FIXTURE_FAILURE", "message": "fixture phase failure"}}, separators=(",", ":")), flush=True)',
      'raise SystemExit(1)',
      ''
    ].join('\n'))
    process.env.EDIT_BACKLOG_SCRIPT = failingHelper
    try {
      await assert.rejects(backlogCreate.recoverPublications(), (error) => error && error.code === 'EDIT_FIXTURE_FAILURE')
    } finally {
      delete process.env.EDIT_BACKLOG_SCRIPT
    }
    assert.equal(creationMarkers.scan().incomplete.length, 0)
    assert.equal(editMarkers.scan().incomplete.length, 1)
    const retried = await backlogCreate.recoverPublications()
    assert.equal(retried.creations.recoveredCount, 0)
    assert.equal(retried.edits.recoveredCount, 1)
    assert.equal(readFileSync(join(tasks, 'backlog', 'TASK_1_recovery_base.md'), 'utf8'), edited)
    assert.equal(existsSync(join(tasks, 'backlog', 'TASK_3_recovered_creation.md')), true)
    regen(true)
  })

  await check('corrupt state remains fail-closed even under combined recovery authority', async () => {
    crashEdit(taskMarkdown('Corruption fenced', '## Goal\n\nDo not publish yet.'))
    const corrupt = join(creations, 'f'.repeat(64) + '.json')
    writeFileSync(corrupt, '{broken\n')
    await assert.rejects(backlogCreate.recoverPublications(), (error) => error && error.code === 'finalization-active')
    assert.equal(editMarkers.scan().incomplete.length, 1)
    unlinkSync(corrupt)
    const recovered = await backlogCreate.recoverPublications()
    assert.equal(recovered.edits.recoveredCount, 1)
  })

  await check('same-stem orphan ownership is excluded while a different stem is admitted', () => {
    const foreign = writerLeases.acquire(writers, {
      kind: 'task-session', stem: 'TASK_1_recovery_base', key: 'task:TASK_1_recovery_base',
      ownerPid: process.pid, childPid: process.pid
    })
    try {
      const same = finalizations.beginMutation({
        kind: 'task-session', stem: 'TASK_1_recovery_base', key: 'task:TASK_1_recovery_base',
        sessionId: finalizations.createWriterSessionId()
      })
      assert.equal(same.ok, false)
      assert.equal(writerLeases.scan(writers).active.length, 1)
      // With per-task worktree isolation complete (Phases 1-5) a DIFFERENT stem
      // is admitted: the two runs own disjoint checkouts, so an orphan holding
      // one task can no longer stall every other task.
      const other = finalizations.beginMutation({
        kind: 'task-session', stem: 'TASK_99_parallel', key: 'task:TASK_99_parallel',
        sessionId: finalizations.createWriterSessionId()
      })
      assert.equal(other.ok, true, 'a different stem must not be blocked by a foreign orphan')
      assert.equal(writerLeases.scan(writers).active.length, 2)
      finalizations.endMutation(other.handle)
    } finally { writerLeases.release(foreign) }
  })

  await check('poller retries an incomplete edit automatically after a foreign writer lease disappears', async () => {
    crashEdit(taskMarkdown('Retry after writer', '## Goal\n\nAutomatic retry.'))
    const foreign = writerLeases.acquire(writers, {
      kind: 'task-session', stem: 'TASK_77_foreign', key: 'task:TASK_77_foreign',
      ownerPid: process.pid, childPid: process.pid
    })
    let errors = 0, deferred = 0, recovered = 0
    const controller = backlogCreate.createRecoveryController({
      intervalMs: 25, retryMs: 50,
      onError: () => { errors++ },
      onAttemptSettled: (outcome) => {
        if (outcome?.code === 'finalization-active') deferred++
      },
      onRecovered: () => { recovered++ }
    })
    try {
      await controller.start()
      assert.equal(errors, 0)
      assert.equal(deferred, 1)
      assert.equal(editMarkers.scan().incomplete.length, 1)
      writerLeases.release(foreign)
      await waitFor(() => recovered === 1 && editMarkers.scan().incomplete.length === 0)
      assert.equal(recovered, 1)
      assert.equal(errors, 0)
      crashEdit(taskMarkdown('Second retry generation', '## Goal\n\nSame stem and phase, new transaction.'))
      await waitFor(() => recovered === 2 && editMarkers.scan().incomplete.length === 0)
      assert.equal(errors, 0)
      regen(true)
    } finally {
      controller.stop()
      try { writerLeases.release(foreign) } catch {}
    }
  })

  await check('poller retries an untracked transient helper failure with bounded backoff', async () => {
    const creationState = { blocking: [], incomplete: [], completedCount: 0 }
    const editState = {
      blocking: [{ code: 'EDIT_INCOMPLETE', stem: 'TASK_88_transient' }],
      incomplete: [{ stem: 'TASK_88_transient', transactionId: '8'.repeat(32), phase: 'claimed', revision: 1,
        updatedAt: '2026-07-12T00:00:00.000000Z', errorCode: null }],
      completedCount: 0
    }
    let attempts = 0, recovered = 0
    const controller = backlogCreate.createRecoveryController({
      creationScan: () => creationState,
      editScan: () => editState,
      recover: async () => {
        attempts++
        if (attempts === 1) throw Object.assign(new Error('runtime temporarily absent'), { code: 'CLI_UNAVAILABLE' })
        editState.blocking = []; editState.incomplete = []
        return { creations: { recoveredCount: 0, recovered: [] }, edits: { recoveredCount: 1, recovered: [] } }
      },
      onRecovered: () => { recovered++ },
      intervalMs: 25, retryMs: 40, retryMaxMs: 80
    })
    try {
      await controller.start()
      assert.equal(attempts, 1)
      await waitFor(() => recovered === 1)
      assert.equal(attempts, 2)
    } finally { controller.stop() }
  })

  console.log(`publication-recovery: ${checks} checks passed`)
} finally {
  backlogCreate.killAll()
  rmSync(root, { recursive: true, force: true })
}
