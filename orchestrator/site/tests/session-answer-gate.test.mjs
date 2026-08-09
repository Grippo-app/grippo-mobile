#!/usr/bin/env node

import assert from 'node:assert/strict'
import { EventEmitter } from 'node:events'
import { mkdirSync, mkdtempSync, rmSync, symlinkSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { createRequire } from 'node:module'

const root = mkdtempSync(join(tmpdir(), 'session-answer-gate-'))
const cache = join(root, 'orchestrator', '.cache', 'tasks')
const tasks = join(root, 'orchestrator', 'tasks')
for (const column of ['backlog', 'pending', 'todo', 'done']) mkdirSync(join(tasks, column), { recursive: true })
for (const dir of ['locks', 'runs', 'finalizations', 'creations', 'edits']) mkdirSync(join(cache, dir), { recursive: true })

process.env.ORCHESTRATOR_PROJECT_ROOT = root
process.env.ORCHESTRATOR_TASKS_DIR = tasks
process.env.ORCHESTRATOR_LOCKS_DIR = join(cache, 'locks')
process.env.ORCHESTRATOR_RUNS_DIR = join(cache, 'runs')
process.env.ORCHESTRATOR_FINALIZATIONS_DIR = join(cache, 'finalizations')
process.env.ORCHESTRATOR_TASK_CREATIONS_DIR = join(cache, 'creations')
process.env.ORCHESTRATOR_TASK_EDITS_DIR = join(cache, 'edits')
process.env.FIGMA_WIRING_GATE = '0'

const require = createRequire(import.meta.url)
const cp = require('node:child_process')
const originalSpawn = cp.spawn

class FakeInput extends EventEmitter {
  constructor() { super(); this.writable = true; this.lines = [] }
  write(line, callback) {
    this.lines.push(line)
    if (callback) callback()
    return true
  }
  end() { this.writable = false }
}

class FakeChild extends EventEmitter {
  constructor() {
    super()
    this.pid = 424242
    this.stdout = new EventEmitter()
    this.stderr = new EventEmitter()
    this.stdin = new FakeInput()
  }
  kill() { return true }
}

let child
let spawnCount = 0
const spawnArgs = []
const spawnOptions = []
cp.spawn = function (_command, args, options) {
  spawnCount++
  spawnArgs.push(args.slice())
  spawnOptions.push(options)
  child = new FakeChild()
  return child
}

const finalizations = require('../server/finalizations.js')
const locks = require('../server/locks.js')
const taskIntegrity = require('../server/task-integrity.js')
const originals = {
  mutationBlocked: finalizations.mutationBlocked,
  createWriterSessionId: finalizations.createWriterSessionId,
  beginMutation: finalizations.beginMutation,
  attachMutationChild: finalizations.attachMutationChild,
  retainMutation: finalizations.retainMutation,
  endMutation: finalizations.endMutation,
  lockOwnedBySession: locks.lockOwnedBySession,
  validateAction: taskIntegrity.validateAction
}

let beginCount = 0
let attachCount = 0
let endCount = 0
let lockOwned = true
const admittedRevision = 'sha256:' + 'a'.repeat(64)
let currentRevision = 'sha256:' + 'b'.repeat(64)
finalizations.mutationBlocked = () => false
finalizations.createWriterSessionId = () => 'session-answer-gate-0001'
finalizations.beginMutation = () => ({
  ok: true,
  handle: {
    leaseId: `lease-answer-${++beginCount}`,
    delegationToken: 'd'.repeat(64)
  }
})
finalizations.attachMutationChild = () => { attachCount++; return { ok: true } }
finalizations.retainMutation = () => true
finalizations.endMutation = () => { endCount++; return true }
locks.lockOwnedBySession = () => lockOwned
  ? { owned: true, reason: 'exact-session-owner' }
  : { owned: false, reason: 'foreign-session-owner' }
taskIntegrity.validateAction = (action, stem) => ({
  ok: action === 'prep' && /^TASK_[12]_answer_gate$/.test(stem),
  indexStatus: 'fresh',
  observedState: 'backlog',
  sourceRevision: currentRevision
})

const sessions = require('../server/sessions.js')
const key = 'task:TASK_1_answer_gate'

function emit(value) {
  child.stdout.emit('data', Buffer.from(JSON.stringify(value) + '\n'))
}

try {
  const started = sessions.start(key, {
    stem: 'TASK_1_answer_gate',
    action: 'prep',
    expectedState: 'backlog',
    sourceRevision: admittedRevision,
    prompt: 'initial canonical queue turn'
  })
  assert.equal(started.running, true)
  assert.equal(spawnCount, 1)
  assert.equal(beginCount, 1)
  assert.equal(attachCount, 1)
  assert.equal(child.stdin.lines.length, 1)

  emit({ type: 'system', subtype: 'init', session_id: '11111111-1111-4111-8111-111111111111' })
  emit({ type: 'result', result: 'warm and idle' })
  assert.equal(endCount, 0, 'a live task child retains its exact environment-bound lease')
  assert.equal(sessions.status(key).inputReady, true, 'a live exact locked continuation is advertised as sendable')
  const continued = sessions.sendOrResume(key, 'continue the blocked task')
  assert.equal(continued.sent, true)
  assert.equal(continued.error, null)
  assert.equal(beginCount, 1, 'manual continuation reuses the exact child-lifetime lease')
  assert.equal(attachCount, 1)
  assert.equal(child.stdin.lines.length, 2)
  assert.equal(sessions.status(key).inputReady, false, 'an active turn is never advertised as sendable')
  const duplicateContinuation = sessions.sendOrResume(key, 'must not overlap the active continuation')
  assert.equal(duplicateContinuation.sent, true)
  assert.equal(duplicateContinuation.queued, true)
  assert.equal(duplicateContinuation.busy, true)
  assert.equal(child.stdin.lines.length, 2, 'a double-send must add no stdin byte')
  emit({ type: 'result', result: 'manual continuation applied' })
  await new Promise((resolve) => setTimeout(resolve, 10))
  assert.equal(child.stdin.lines.length, 3, 'queued input drains only after the current result fence')
  assert.equal(JSON.parse(child.stdin.lines[2]).message.content[0].text, 'must not overlap the active continuation')
  assert.equal(sessions.status(key).queuedInputCount, 0)
  emit({ type: 'result', result: 'queued continuation applied' })
  assert.equal(endCount, 0)

  // The question snapshot is captured after the turn, so legitimate same-turn
  // task edits (revision a -> b) do not make the answer stale.
  emit({ type: 'system', needs_action: 'Choose one option' })
  const accepted = sessions.sendOrResume(key, 'Option A')
  assert.equal(accepted.sent, true)
  assert.equal(accepted.error, null)
  assert.equal(beginCount, 1, 'answer reuses the exact child-lifetime lease')
  assert.equal(attachCount, 1)
  assert.equal(child.stdin.lines.length, 4)
  emit({ type: 'result', result: 'answer applied' })
  assert.equal(endCount, 0)

  // A stale task snapshot is still accepted by the terminal UI, but no byte is
  // injected into the ownership-bound child. It queues for a later read-only
  // continuation instead of reviving mutation authority from sidecar state.
  emit({ type: 'system', needs_action: 'Confirm against the current snapshot' })
  currentRevision = 'sha256:' + 'c'.repeat(64)
  const stale = sessions.sendOrResume(key, 'stale answer')
  assert.equal(stale.sent, true)
  assert.equal(stale.queued, true)
  assert.equal(child.stdin.lines.length, 4)
  assert.equal(endCount, 0, 'refused answer must not withdraw the pre-existing child-lifetime lease')

  child.emit('exit', 0, null)
  child.emit('close', 0, null)
  assert.equal(endCount, 1, 'the exact lease releases only after process-tree close proof')
  assert.equal(sessions.status(key).inputReady, false)
  await new Promise((resolve) => setTimeout(resolve, 10))
  assert.equal(spawnCount, 2, 'queued stale input resumes only after the old writer is proven closed')
  assert.ok(spawnArgs[1].includes('--resume'))
  assert.ok(spawnArgs[1].includes('--safe-mode'))
  assert.ok(spawnArgs[1].includes('Read,Grep,Glob'))
  assert.ok(spawnArgs[1].includes('--disable-slash-commands'))
  assert.ok(!spawnArgs[1].includes('--dangerously-skip-permissions'))
  assert.equal(spawnOptions[1].env.ORCHESTRATOR_WRITER_SESSION_ID, undefined)
  assert.equal(spawnOptions[1].env.ORCHESTRATOR_WRITER_STEM, undefined)
  assert.equal(spawnOptions[1].env.ORCHESTRATOR_WRITER_LEASE_ID, undefined)
  assert.equal(child.stdin.lines.length, 1)
  assert.equal(JSON.parse(child.stdin.lines[0]).message.content[0].text, 'stale answer')
  emit({ type: 'result', result: 'read-only follow-up answered' })
  child.emit('exit', 0, null)
  child.emit('close', 0, null)

  // Board Prepare owns a no-questions contract. A model-level needs_action is
  // never exposed to the user; the exact live child receives one bounded,
  // ownership-fenced safe-default continuation instead.
  currentRevision = admittedRevision
  lockOwned = true
  const noQuestionsKey = 'task:TASK_2_answer_gate'
  const noQuestionsStarted = sessions.start(noQuestionsKey, {
    stem: 'TASK_2_answer_gate',
    action: 'prep',
    noQuestions: true,
    expectedState: 'backlog',
    sourceRevision: admittedRevision,
    prompt: 'Board Prepare canonical queue turn'
  })
  assert.equal(noQuestionsStarted.running, true)
  assert.equal(spawnCount, 3)
  assert.equal(spawnOptions[2].env.ORCHESTRATOR_TASK_PREP_NO_QUESTIONS, '1')
  emit({ type: 'system', subtype: 'init', session_id: '33333333-3333-4333-8333-333333333333' })
  emit({ type: 'system', needs_action: 'Which implementation should I choose?' })
  assert.equal(sessions.status(noQuestionsKey).askedThisTurn, false)
  assert.equal(sessions.status(noQuestionsKey).inputReady, false)
  assert.equal(sessions.status(noQuestionsKey).queuedInputCount, 1)
  const noQuestionEvents = sessions.eventsSince(noQuestionsKey, 0)
  assert.equal(noQuestionEvents.some((event) => event.kind === 'needs_action'), false)
  assert.equal(JSON.stringify(noQuestionEvents).includes('Which implementation should I choose?'), false)
  emit({ type: 'result', result: 'requested clarification' })
  await new Promise((resolve) => setTimeout(resolve, 10))
  assert.equal(child.stdin.lines.length, 2)
  assert.match(
    JSON.parse(child.stdin.lines[1]).message.content[0].text,
    /BOARD PREP POLICY: NO QUESTIONS\./
  )
  assert.equal(sessions.status(noQuestionsKey).queuedInputCount, 0)
  emit({ type: 'result', result: 'promoted with safe defaults' })
  child.emit('exit', 0, null)
  child.emit('close', 0, null)

  const figmaKey = 'figma:shipdriftsweep'
  const figmaStarted = sessions.start(figmaKey, { prompt: 'canonical Figma action' })
  assert.equal(figmaStarted.running, true)
  assert.equal(spawnCount, 4)
  assert.equal(child.stdin.lines.length, 1)

  emit({ type: 'system', subtype: 'init', session_id: '22222222-2222-4222-8222-222222222222' })
  emit({ type: 'result', result: 'warm and idle' })
  child.emit('exit', 0, null)
  child.emit('close', 0, null)

  finalizations.mutationBlocked = () => true
  const endedFigma = sessions.sendOrResume(figmaKey, 'explain the completed Figma action')
  finalizations.mutationBlocked = () => false
  assert.equal(endedFigma.sent, true)
  assert.equal(endedFigma.resumed, true)
  assert.equal(spawnCount, 5)
  assert.ok(spawnArgs[4].includes('--safe-mode'))
  assert.ok(!spawnArgs[4].includes('--dangerously-skip-permissions'))

  emit({ type: 'result', result: 'read-only Figma follow-up answered' })
  child.emit('exit', 0, null)
  child.emit('close', 0, null)

  // A hard runtime refusal must be reported immediately. It must not enter the
  // transient FIFO and spin forever while the UI claims the message is queued.
  const runs = join(cache, 'runs')
  rmSync(runs, { recursive: true, force: true })
  symlinkSync(tasks, runs)
  const refused = sessions.sendOrResume(figmaKey, 'must not be queued')
  assert.equal(refused.sent, false)
  assert.equal(refused.error, 'session-runtime-unsafe')
  assert.equal(sessions.status(figmaKey).queuedInputCount, 0)

  console.log('session-answer-gate: live, queued, and read-only continuation checks passed')
} finally {
  cp.spawn = originalSpawn
  for (const [name, value] of Object.entries(originals)) {
    if (name === 'lockOwnedBySession') locks[name] = value
    else if (name === 'validateAction') taskIntegrity[name] = value
    else finalizations[name] = value
  }
  rmSync(root, { recursive: true, force: true })
}
