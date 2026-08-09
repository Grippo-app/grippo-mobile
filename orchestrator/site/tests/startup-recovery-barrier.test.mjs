#!/usr/bin/env node

import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const require = createRequire(import.meta.url)
const startupRecovery = require('../server/startup-recovery.js')
const backlogCreate = require('../server/backlog-create.js')

let now = Date.parse('2026-07-19T00:00:00.000Z')
let verification = { ok: true, reasonCode: null, findingCount: 0 }
let starts = 0
const barrier = startupRecovery._createBarrier({
  clock: () => now,
  verify: () => verification,
  startRunner: () => { starts++ },
})

assert.deepEqual(barrier.snapshot(), {
  version: 1, status: 'pending', attempts: 0,
  startedAt: '2026-07-19T00:00:00.000Z', updatedAt: '2026-07-19T00:00:00.000Z',
  readyAt: null, reasonCode: null, findingCount: 0,
})

now += 1000
let state = barrier.settle({ version: 1, sequence: 1, ok: false, code: 'WRITER_LEASE_RECOVERY_BLOCKED' })
assert.equal(state.status, 'blocked')
assert.equal(state.attempts, 1)
assert.equal(state.reasonCode, 'startup-recovery-failed')
assert.equal(starts, 0)

// Duplicate/delayed callbacks are idempotent and cannot overwrite the latest
// startup verdict.
assert.deepEqual(barrier.settle({ version: 1, sequence: 1, ok: true }), state)

verification = { ok: false, reasonCode: 'FINALIZATION_RECOVERY_REQUIRED', findingCount: 2 }
now += 1000
state = barrier.settle({ version: 1, sequence: 2, ok: true })
assert.equal(state.status, 'blocked')
assert.equal(state.attempts, 2)
assert.equal(state.reasonCode, 'startup-integrity-blocked')
assert.equal(state.findingCount, 2)
assert.equal(starts, 0)

verification = { ok: true, reasonCode: null, findingCount: 0 }
now += 1000
state = barrier.settle({ version: 1, sequence: 3, ok: true })
assert.equal(state.status, 'ready')
assert.equal(state.attempts, 3)
assert.equal(state.readyAt, '2026-07-19T00:00:03.000Z')
assert.equal(starts, 1)
assert.deepEqual(barrier.settle({ version: 1, sequence: 4, ok: true }), state)
assert.equal(starts, 1, 'a later recovery poll must never initialize the runner twice')

let startAttempts = 0
const retryRunner = startupRecovery._createBarrier({
  verify: () => ({ ok: true }),
  startRunner: () => { if (++startAttempts === 1) throw Object.assign(new Error('no runner'), { code: 'RUNNER_BOOT_FAILED' }) },
})
assert.equal(retryRunner.settle({ version: 1, sequence: 1, ok: true }).reasonCode, 'runner-start-failed')
assert.equal(retryRunner.settle({ version: 1, sequence: 2, ok: true }).status, 'ready')
assert.equal(startAttempts, 2)

assert.equal(
  startupRecovery.publicReasonCode('/private/runtime/secret', 'startup-recovery-failed'),
  'startup-recovery-failed'
)

// Controller outcomes drive blocked -> ready retries without conflating an
// unsafe durable marker with "no recovery work".
let unsafe = true
const outcomes = []
const empty = () => ({ blocking: [], incomplete: [], completedCount: 0 })
const controller = backlogCreate.createRecoveryController({
  writerReconcile: () => ({ reconciled: [], blocked: [] }),
  creationScan: () => unsafe
    ? { blocking: [{ code: 'CREATION_MARKER_INVALID', stem: 'TASK_1_bad' }], incomplete: [], completedCount: 0 }
    : empty(),
  editScan: empty,
  onAttemptSettled: (outcome) => outcomes.push(outcome),
  intervalMs: 60000,
})
try {
  await controller.start()
  assert.equal(outcomes[0].ok, false)
  assert.equal(outcomes[0].code, 'PUBLICATION_RECOVERY_UNSAFE')
  unsafe = false
  await controller.poll()
  assert.equal(outcomes[1].ok, true)
  assert.ok(outcomes[1].sequence > outcomes[0].sequence)
} finally {
  controller.stop()
}

// Retry backoff must preserve the failed verdict. An incomplete generation is
// never reported as clean merely because its next retry is not due yet.
const retryOutcomes = []
const incomplete = () => ({
  blocking: [{ code: 'EDIT_INCOMPLETE', stem: 'TASK_2_retry' }],
  incomplete: [{ stem: 'TASK_2_retry', transactionId: '2'.repeat(32) }],
  completedCount: 0,
})
const retryController = backlogCreate.createRecoveryController({
  writerReconcile: () => ({ reconciled: [], blocked: [] }),
  creationScan: empty,
  editScan: incomplete,
  recover: async () => { throw Object.assign(new Error('transient helper failure'), { code: 'HELPER_UNAVAILABLE' }) },
  onAttemptSettled: (outcome) => retryOutcomes.push(outcome),
  retryMs: 60000,
  intervalMs: 60000,
})
try {
  await retryController.start()
  assert.equal(retryOutcomes.length, 1)
  assert.equal(retryOutcomes[0].ok, false)
  await retryController.poll()
  assert.equal(retryOutcomes.length, 1, 'cooldown must retain the prior blocked outcome')
} finally {
  retryController.stop()
}

const server = readFileSync(resolve(HERE, '../server.js'), 'utf8')
const stateSource = readFileSync(resolve(HERE, '../server/state.js'), 'utf8')
const board = readFileSync(resolve(HERE, '../scripts/panels/board.js'), 'utf8')
const boardTaskCardFactory = readFileSync(
  resolve(HERE, '../scripts/board/board-task-card-factory.js'), 'utf8')
const boardTaskActionController = readFileSync(
  resolve(HERE, '../scripts/board/board-task-action-controller.js'), 'utf8')
const boardHealth = readFileSync(resolve(HERE, '../scripts/board/board-health.js'), 'utf8')
const boardRenderer = readFileSync(
  resolve(HERE, '../scripts/board/board-render-controller.js'), 'utf8')
assert.match(server, /onAttemptSettled:\s*settleStartupRecovery/)
assert.match(server, /startRunner:\s*function \(\) \{ runner\.init\(\); \}/)
assert.doesNotMatch(server, /\.finally\(function \(\) \{[\s\S]{0,1800}try \{ runner\.init\(\)/,
  'runner initialization must be owned by the ready transition, not an unconditional finally')
assert.match(stateSource, /startupRecovery:\s*startupRecoveryMod\.snapshot\(\)/)
assert.match(board, /startupRecoveryBlocksMutation\(\)/)
assert.match(board,
  /mutationsBlocked:\s*boardReadiness\.startupRecoveryBlocksMutation/)
assert.match(boardTaskCardFactory,
  /var mutationsBlocked = dependencies\.mutationsBlocked\(\)/)
assert.match(boardTaskCardFactory, /mutationsBlocked:\s*mutationsBlocked/)
assert.match(board,
  /startupRecoveryBlocksMutation:\s*boardReadiness\.startupRecoveryBlocksMutation/)
assert.match(boardTaskActionController,
  /if \(dependencies\.startupRecoveryBlocksMutation\(\)\)/)
assert.match(boardTaskActionController, /board\.startupRecovery\.actionBlocked/)
assert.match(board, /health:\s*boardHealth/)
assert.match(boardRenderer, /dependencies\.health\.render\(storeState\)/)
assert.match(boardHealth, /var startup = startupRecoveryState\(\)/)
assert.match(server, /ok:\s*!!\(result && result\._model\)/)

console.log('startup recovery barrier: blocked, retry, ready, and single-runner checks passed')
