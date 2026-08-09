#!/usr/bin/env node

import assert from 'node:assert/strict'
import { existsSync, linkSync, lstatSync, mkdirSync, mkdtempSync, readFileSync, rmSync, symlinkSync, unlinkSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { spawnSync } from 'node:child_process'
import { createRequire } from 'node:module'
import { fileURLToPath, pathToFileURL } from 'node:url'

const require = createRequire(import.meta.url)
const WORKSPACE = fileURLToPath(new URL('../../..', import.meta.url))
const PYTHON_CLI = join(WORKSPACE, 'orchestrator', 'tasks', 'log-event.py')
const NODE_CLI = join(WORKSPACE, 'orchestrator', 'tasks', 'task-journal.mjs')
const root = mkdtempSync(join(tmpdir(), 'task-journal-'))
const journal = join(root, 'orchestrator', '.cache', 'tasks', 'journal')
const tasks = join(root, 'orchestrator', 'tasks')
const outside = join(root, 'outside')
mkdirSync(tasks, { recursive: true })
mkdirSync(outside, { recursive: true })

process.env.ORCHESTRATOR_PROJECT_ROOT = root
process.env.ORCHESTRATOR_JOURNAL_DIR = journal

const tasksLog = require('../server/tasks-log.js')
const contract = require('../../tasks/task-journal-contract.cjs')
const stem = 'TASK_7_journal_fixture'
const file = join(journal, stem + '.jsonl')
let checks = 0
function check(name, fn) { fn(); checks++; console.log(`ok ${checks} - ${name}`) }
function runPython(args) {
  return spawnSync('python3', [PYTHON_CLI, stem, ...args], {
    cwd: root,
    env: { ...process.env, ORCHESTRATOR_PROJECT_ROOT: root, ORCHESTRATOR_JOURNAL_DIR: journal },
    encoding: 'utf8',
  })
}
function validEvent(patch = {}) {
  return { ts: '2026-07-13T12:00:00Z', stem, kind: 'note', phase: 'prep', status: 'info', detail: 'bounded note', ...patch }
}

try {
  await import(pathToFileURL(NODE_CLI).href + '?inert=' + Date.now())
  check('importing the journal helper has no filesystem side effects', () => {
    assert.equal(existsSync(journal), false)
  })

  const appended = runPython(['note', '--phase', 'prep', '--status', 'info', '--detail', '  one\n bounded   line  ', '--meta', 'round=2'])
  check('Python CLI delegates one canonical private append', () => {
    assert.equal(appended.status, 0, appended.stdout + appended.stderr)
    assert.equal(lstatSync(file).mode & 0o777, 0o600)
    const result = tasksLog.readLog(stem)
    assert.equal(result.truncated, false)
    assert.deepEqual(result.events, [{
      ts: result.events[0].ts,
      stem,
      kind: 'note',
      phase: 'prep',
      status: 'info',
      detail: 'one bounded line',
      meta: { round: '2' },
    }])
    assert.equal(contract.validateEvent(result.events[0], stem), null)
  })

  const journalRevision = tasksLog.revision()
  const revisionAppend = runPython(['note', '--phase', 'prep', '--status', 'info', '--detail', 'revision tick'])
  check('journal revision changes on append for task-summary SSE invalidation', () => {
    assert.equal(revisionAppend.status, 0, revisionAppend.stdout + revisionAppend.stderr)
    assert.notEqual(tasksLog.revision(), journalRevision)
  })

  const beforeUnknown = readFileSync(file)
  const unknown = runPython(['invented-kind', '--phase', 'prep'])
  check('unknown enums are best-effort drops rather than browser-visible extensions', () => {
    assert.equal(unknown.status, 0)
    assert.match(unknown.stderr, /event dropped/)
    assert.deepEqual(readFileSync(file), beforeUnknown)
  })

  writeFileSync(file, JSON.stringify({ ...validEvent(), privatePrompt: 'SECRET PROMPT' }) + '\n')
  check('reader rejects unknown fields instead of forwarding them to the browser', () => {
    const result = tasksLog.readLog(stem)
    assert.equal(result.truncated, true)
    assert.deepEqual(result.events, [])
    assert.doesNotMatch(JSON.stringify(result), /SECRET PROMPT/)
  })

  writeFileSync(file, JSON.stringify(validEvent()) + '\n{"ts":')
  check('reader keeps complete canonical lines but marks a partial tail omitted', () => {
    const result = tasksLog.readLog(stem)
    assert.equal(result.truncated, true)
    assert.equal(result.events.length, 1)
  })

  const outsideFile = join(outside, 'foreign.jsonl')
  writeFileSync(outsideFile, 'FOREIGN SECRET\n')
  unlinkSync(file)
  symlinkSync(outsideFile, file)
  const symlinkAppend = runPython(['note', '--phase', 'prep'])
  check('final symlink is neither read nor appended through', () => {
    assert.equal(symlinkAppend.status, 0)
    assert.deepEqual(readFileSync(outsideFile, 'utf8'), 'FOREIGN SECRET\n')
    const result = tasksLog.readLog(stem)
    assert.equal(result.truncated, true)
    assert.deepEqual(result.events, [])
  })

  unlinkSync(file)
  linkSync(outsideFile, file)
  const hardlinkAppend = runPython(['note', '--phase', 'prep'])
  check('hardlinked journal generation is neither read nor appended through', () => {
    assert.equal(hardlinkAppend.status, 0)
    assert.deepEqual(readFileSync(outsideFile, 'utf8'), 'FOREIGN SECRET\n')
    assert.deepEqual(tasksLog.readLog(stem).events, [])
  })

  unlinkSync(file)
  const rows = []
  for (let i = 0; i < tasksLog.MAX_EVENTS + 25; i++) rows.push(JSON.stringify(validEvent({ detail: `event ${i}` })))
  writeFileSync(file, rows.join('\n') + '\n', { mode: 0o600 })
  check('event count is capped deterministically to the newest canonical rows', () => {
    const result = tasksLog.readLog(stem)
    assert.equal(result.truncated, true)
    assert.equal(result.events.length, tasksLog.MAX_EVENTS)
    assert.equal(result.events[0].detail, 'event 25')
    assert.equal(result.events.at(-1).detail, `event ${tasksLog.MAX_EVENTS + 24}`)
  })

  const hugeDetail = 'x'.repeat(200)
  const hugeScreens = 's'.repeat(500)
  const hugeRows = []
  for (let i = 0; i < 6000; i++) hugeRows.push(JSON.stringify(validEvent({ detail: hugeDetail, meta: { screens: hugeScreens } })))
  writeFileSync(file, hugeRows.join('\n') + '\n', { mode: 0o600 })
  check('byte-bounded tail never materializes an oversized full journal', () => {
    const result = tasksLog.readLog(stem)
    assert.equal(result.truncated, true)
    assert.ok(result.events.length <= tasksLog.MAX_EVENTS)
  })

  const journalReal = journal + '-real'
  rmSync(journal, { recursive: true, force: true })
  mkdirSync(journalReal, { recursive: true })
  symlinkSync(journalReal, journal)
  const ancestorAppend = runPython(['note', '--phase', 'prep'])
  check('symlinked journal directory is fail-closed for reader and writer', () => {
    assert.equal(ancestorAppend.status, 0)
    assert.equal(existsSync(join(journalReal, stem + '.jsonl')), false)
    assert.deepEqual(tasksLog.readLog(stem).events, [])
  })

  check('stem and event contract reject traversal, unsafe numeric ids, and extra meta', () => {
    assert.equal(contract.validStem('../TASK_1_bad'), false)
    assert.equal(contract.validStem('TASK_9007199254740992_bad'), false)
    assert.match(contract.validateEvent(validEvent({ meta: { token: 'SECRET' } }), stem), /meta/)
  })

  check('review metadata is structured, paired, canonical, and strict', () => {
    const start = validEvent({
      kind: 'phase-start',
      phase: 'review',
      status: 'info',
      meta: {
        reviewer: 'codex',
        reviewAttempt: '2',
        selectionReason: 'codex-available',
      },
    })
    const end = validEvent({
      kind: 'phase-end',
      phase: 'review',
      status: 'ok',
      meta: { reviewer: 'codex', reviewAttempt: '2' },
    })
    assert.equal(contract.validateEvent(start, stem), null)
    assert.equal(contract.validateEvent(end, stem), null)
    assert.match(contract.validateEvent({
      ...start, status: 'ok',
    }, stem), /phase-start status/)
    assert.match(contract.validateEvent({
      ...end, status: 'blocked',
    }, stem), /phase-end status/)
    assert.match(contract.validateEvent(validEvent({
      kind: 'stop',
      phase: 'review',
      status: 'ok',
      meta: { reviewer: 'codex', reviewAttempt: '2' },
    }), stem), /stop status/)
    assert.match(contract.validateEvent(validEvent({
      kind: 'gate',
      phase: 'review',
      status: 'skipped',
    }), stem), /gate status/)
    assert.match(contract.validateEvent(validEvent({
      kind: 'phase-start', phase: 'review', status: 'info',
    }), stem), /requires reviewer/)
    assert.match(contract.validateEvent(validEvent({
      kind: 'phase-end', phase: 'review', status: 'ok',
    }), stem), /requires reviewer/)
    assert.match(contract.validateEvent({
      ...start, meta: { reviewer: 'codex', selectionReason: 'codex-available' },
    }, stem), /together/)
    assert.match(contract.validateEvent({
      ...start, meta: { reviewer: 'codex', reviewAttempt: '2' },
    }, stem), /selectionReason/)
    assert.match(contract.validateEvent({
      ...start,
      meta: {
        reviewer: 'codex',
        reviewAttempt: '2',
        selectionReason: 'codex-unavailable',
      },
    }, stem), /inconsistent/)
    assert.match(contract.validateEvent({
      ...start,
      meta: {
        reviewer: 'internal-reviewer',
        reviewAttempt: '2',
        selectionReason: 'forced-internal',
        reasonCode: 'fallback-used',
      },
    }, stem), /fallback-used/)
    assert.match(contract.validateEvent({
      ...end, meta: { reviewer: 'codex', reviewAttempt: '02' },
    }, stem), /reviewAttempt/)
    assert.match(contract.validateEvent({
      ...end, meta: { reviewer: 'codex', reviewAttempt: '2', selectionReason: 'forced-codex' },
    }, stem), /selectionReason/)
    assert.match(contract.validateEvent(validEvent({
      kind: 'phase-end',
      phase: 'review',
      status: 'fail',
      meta: { reasonCode: 'review-failed' },
    }), stem), /requires reviewer/)
    assert.match(contract.validateEvent(validEvent({
      kind: 'retry',
      phase: 'review',
      status: 'info',
      meta: { reviewer: 'codex', reviewAttempt: '3' },
    }), stem), /review meta/)
    assert.match(contract.validateEvent(validEvent({
      kind: 'note',
      phase: 'prep',
      status: 'info',
      meta: { reasonCode: 'codex-plugin-disabled' },
    }), stem), /review reasonCode/)
    assert.equal(contract.validateEvent(validEvent({
      kind: 'gate',
      phase: 'review',
      status: 'info',
      meta: { reasonCode: 'codex-plugin-disabled' },
    }), stem), null)
    for (const reasonCode of ['codex-auth-missing', 'codex-plugin-broken', 'config-invalid']) {
      assert.equal(contract.validateEvent(validEvent({
        kind: 'gate',
        phase: 'review',
        status: 'info',
        meta: { reasonCode },
      }), stem), null, reasonCode)
    }
  })

  check('shared journal metadata stays bounded and kind-aware', () => {
    assert.equal(contract.MAX_META_ENTRIES, 8)
    assert.equal(contract.validateEvent(validEvent({
      kind: 'note',
      meta: {
        blockType: 'general',
        checkpointId: 'checkpoint:prep:2',
        children: 'TASK_8_child',
        gate: 'prep',
        reasonCode: 'manual-retry',
        reportId: 'report:prep:2',
        round: '2',
        screens: 'Home',
      },
    }), stem), null)
    assert.match(contract.validateEvent(validEvent({
      kind: 'note',
      meta: {
        blockType: 'general',
        checkpointId: 'checkpoint:prep:2',
        children: 'TASK_8_child',
        gate: 'prep',
        reasonCode: 'manual-retry',
        reportId: 'report:prep:2',
        retryPolicy: 'manual',
        round: '2',
        screens: 'Home',
      },
    }), stem), /meta fields/)
    assert.match(contract.validateEvent(validEvent({
      kind: 'phase-start',
      meta: { reportId: 'report:prep:2' },
    }), stem), /reportId/)
    assert.match(contract.validateEvent(validEvent({
      kind: 'phase-end',
      meta: { retryPolicy: 'manual' },
    }), stem), /retryPolicy/)
  })

  console.log(`tasks-log: ${checks} checks passed`)
} finally {
  rmSync(root, { recursive: true, force: true })
}
