#!/usr/bin/env node

import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { existsSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, renameSync, rmSync, statSync, symlinkSync, unlinkSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { tmpdir } from 'node:os'

const root = mkdtempSync(join(tmpdir(), 'creation-markers-'))
const creations = join(root, 'creations')
const finalizationsDir = join(root, 'finalizations')
mkdirSync(creations, { recursive: true })
mkdirSync(finalizationsDir, { recursive: true })
process.env.ORCHESTRATOR_TASK_CREATIONS_DIR = creations
process.env.ORCHESTRATOR_FINALIZATIONS_DIR = finalizationsDir
process.env.ORCHESTRATOR_PROJECT_ROOT = root

const require = createRequire(import.meta.url)
const markers = require('../server/creation-markers.js')
const finalizations = require('../server/finalizations.js')
const markerContract = require('../../tasks/creation-marker-contract.cjs')
const taskSource = require('../../tasks/task-source-contract.cjs')
const childProcess = require('node:child_process')
const backlogCreate = require('../server/backlog-create.js')
const H = (c) => 'sha256:' + c.repeat(64)
const at = '2026-07-12T00:00:00.000Z'
function marker(keyChar, status = 'incomplete', n = null, body = '') {
  const intent = {
    version: 1, title: 'Intent', body, originStem: null, dedupKey: null, dedupReport: null,
    source: taskSource.manualForIntent('creation-marker-' + keyChar, 'manual', 'fixture:creation-marker-' + keyChar)
  }
  return {
    version: 2,
    transactionId: keyChar.repeat(32),
    keyHash: H(keyChar),
    payloadHash: markerContract.digest(intent),
    status,
    phase: status === 'completed' ? 'completed' : 'claimed',
    effect: status === 'completed' ? 'domain-dedup' : null,
    number: n,
    slug: null,
    stem: n == null ? null : `TASK_${n}_receipt`,
    sourceHash: status === 'completed' ? H('a') : null,
    column: status === 'completed' ? 'backlog' : null,
    createdAt: at,
    updatedAt: at,
    revision: status === 'completed' ? 2 : 1,
    lastError: null,
    intent: status === 'completed' ? null : intent,
    targetProof: null
  }
}
function write(m) { writeFileSync(join(creations, m.keyHash.slice(7) + '.json'), JSON.stringify(m) + '\n') }
let checks = 0
function check(name, fn) { fn(); checks++; console.log(`ok ${checks} - ${name}`) }

try {
  check('completed idempotency receipts never block shared publication', () => {
    write(marker('1', 'completed', 1))
    const scan = markers.scan()
    assert.equal(scan.completedCount, 1)
    assert.equal(scan.blocking.length, 0)
    assert.equal(finalizations.mutationBlocked(null), false)
  })

  check('one valid incomplete receipt blocks all writers except its exact recovery key', () => {
    const m = marker('2')
    write(m)
    assert.equal(markers.blockingIssue().code, 'CREATION_INCOMPLETE')
    assert.equal(finalizations.mutationBlocked(null), true)
    const unrelated = finalizations.beginMutation({ kind: 'workspace-session', key: 'unrelated' })
    assert.equal(unrelated.ok, false)
    const recovery = finalizations.beginMutation({ kind: 'workspace-session', key: 'recovery', creationKeyHash: m.keyHash })
    assert.equal(recovery.ok, true)
    assert.equal(finalizations.endMutation(recovery.handle), true)
  })

  check('multiple incomplete receipts require recover-all authority', () => {
    const m = marker('3')
    write(m)
    const single = finalizations.beginMutation({ kind: 'workspace-session', key: 'single', creationKeyHash: m.keyHash })
    assert.equal(single.ok, false)
    const all = finalizations.beginMutation({ kind: 'workspace-session', key: 'all', allowAllCreationRecovery: true })
    assert.equal(all.ok, true)
    assert.equal(finalizations.endMutation(all.handle), true)
  })

  check('a maximum-size recovery receipt remains recoverable', () => {
    const m = marker('5', 'incomplete', null, 'x'.repeat(64 * 1024))
    write(m)
    const file = join(creations, m.keyHash.slice(7) + '.json')
    assert.ok(statSync(file).size > 64 * 1024)
    const scan = markers.scan()
    assert.ok(scan.incomplete.some((row) => row.keyHash === m.keyHash))
    assert.ok(scan.blocking.some((row) => row.code === 'CREATION_INCOMPLETE' && row.keyHash === m.keyHash))
    unlinkSync(file)
  })

  check('a malformed completed receipt is fail-closed rather than counted as safe history', () => {
    const m = marker('6', 'completed', 6)
    m.replayed = false
    writeFileSync(join(creations, m.keyHash.slice(7) + '.json'), JSON.stringify(m) + '\n')
    const scan = markers.scan()
    assert.ok(scan.blocking.some((row) => row.code === 'CREATION_MARKER_INVALID' && row.keyHash === m.keyHash))
    unlinkSync(join(creations, m.keyHash.slice(7) + '.json'))
  })

  check('incomplete intent rejects forbidden title categories and invalid Unicode scalars', () => {
    for (const [key, title] of [['7', 'bad\u2028title'], ['8', 'bad\uD800title']]) {
      const m = marker(key)
      m.intent.title = title
      m.payloadHash = markerContract.digest(m.intent)
      write(m)
      const scan = markers.scan()
      assert.ok(scan.blocking.some((row) => row.code === 'CREATION_MARKER_INVALID' && row.keyHash === m.keyHash))
      unlinkSync(join(creations, m.keyHash.slice(7) + '.json'))
    }
  })

  check('creation controllers reject non-canonical and unsafe task numbers in durable lineage', () => {
    for (const [unsafeStem, suffix] of [
      ['TASK_01_leading_zero', 'leading-zero'],
      ['TASK_9007199254740992_unsafe', 'unsafe-number'],
    ]) {
      const m = marker('a')
      m.intent.originStem = unsafeStem
      m.payloadHash = markerContract.digest(m.intent)
      assert.throws(() => markerContract.validate(m, m.keyHash.slice(7) + '.json'), /originStem/)
      const completed = marker('b', 'completed', 1)
      completed.stem = unsafeStem
      assert.throws(() => markerContract.validate(completed, completed.keyHash.slice(7) + '.json'), /stem/)
      assert.throws(() => backlogCreate.validateInput({
        title: 'Unsafe lineage', body: '', idempotencyKey: 'unsafe-origin-' + suffix,
        originStem: unsafeStem, dedupKey: null, dedupReport: null,
        source: taskSource.manualForIntent('unsafe-origin-' + suffix, 'manual', 'unsafe-origin-' + suffix)
      }), (error) => error && error.code === 'bad-origin-stem')
    }
  })

  check('corrupt marker stays fail-closed even for recover-all', () => {
    writeFileSync(join(creations, '4'.repeat(64) + '.json'), '{broken\n')
    assert.ok(markers.scan().blocking.some((issue) => issue.code === 'CREATION_MARKER_CORRUPT'))
    const all = finalizations.beginMutation({ kind: 'workspace-session', key: 'all', allowAllCreationRecovery: true })
    assert.equal(all.ok, false)
  })

  check('symlink and oversized marker entries fail closed without reading their targets', () => {
    const outside = join(root, 'outside-marker.json')
    const symlinkName = '9'.repeat(64) + '.json'
    writeFileSync(outside, JSON.stringify(marker('9', 'completed', 9)) + '\n')
    symlinkSync(outside, join(creations, symlinkName))
    writeFileSync(join(creations, 'a'.repeat(64) + '.json'), Buffer.alloc(markerContract.MAX_BYTES + 1, 0x78))
    const state = markers.scan()
    assert.ok(state.blocking.some((row) => row.code === 'CREATION_MARKER_UNSAFE' && row.filename === symlinkName))
    assert.ok(state.blocking.some((row) => row.code === 'CREATION_MARKER_TOO_LARGE'))
    assert.equal(readFileSync(outside, 'utf8'), JSON.stringify(marker('9', 'completed', 9)) + '\n')
    unlinkSync(join(creations, symlinkName))
    unlinkSync(join(creations, 'a'.repeat(64) + '.json'))
  })

  check('ancestor swap cannot redirect a marker scan and swap-back is not accepted as one snapshot', () => {
    const displaced = join(root, 'creations-displaced')
    const outsideDir = join(root, 'outside-creations')
    mkdirSync(outsideDir)
    write(marker('b', 'completed', 11))
    const originalSpawnSync = childProcess.spawnSync
    let swapped = false
    childProcess.spawnSync = function (command, args, options) {
      const request = options && typeof options.input === 'string' ? JSON.parse(options.input) : null
      if (!swapped && request && request.action === 'directory-names') {
        swapped = true
        renameSync(creations, displaced)
        symlinkSync(outsideDir, creations, 'dir')
      }
      return originalSpawnSync.call(childProcess, command, args, options)
    }
    try {
      const state = markers.scan()
      assert.equal(swapped, true)
      assert.ok(state.blocking.some((row) => row.code === 'CREATION_MARKER_DIR_UNAVAILABLE'))
      assert.equal(existsSync(join(displaced, 'b'.repeat(64) + '.json')), true)
      assert.deepEqual(readdirSync(outsideDir), [])
    } finally {
      childProcess.spawnSync = originalSpawnSync
      rmSync(creations, { recursive: true, force: true })
      renameSync(displaced, creations)
      unlinkSync(join(creations, 'b'.repeat(64) + '.json'))
    }
  })

  check('creation event append refuses final symlinks, oversized logs, and swapped ancestors', () => {
    const events = join(creations, '.events.jsonl')
    const outside = join(root, 'outside-events.jsonl')
    writeFileSync(outside, 'outside-secret\n')
    symlinkSync(outside, events)
    assert.equal(backlogCreate._appendCreationEvent('fixture', { ok: true }), false)
    assert.equal(readFileSync(outside, 'utf8'), 'outside-secret\n')
    unlinkSync(events)
    writeFileSync(events, Buffer.alloc(1024 * 1024, 0x78))
    assert.equal(backlogCreate._appendCreationEvent('fixture', { ok: true }), false)
    assert.equal(statSync(events).size, 1024 * 1024)
    unlinkSync(events)

    const displaced = join(root, 'append-displaced')
    const outsideDir = join(root, 'append-outside')
    mkdirSync(outsideDir)
    const originalSpawnSync = childProcess.spawnSync
    let swapped = false
    childProcess.spawnSync = function (command, args, options) {
      const request = options && typeof options.input === 'string' ? JSON.parse(options.input) : null
      if (!swapped && request && request.action === 'append-bounded') {
        swapped = true
        renameSync(creations, displaced)
        symlinkSync(outsideDir, creations, 'dir')
      }
      return originalSpawnSync.call(childProcess, command, args, options)
    }
    try {
      assert.equal(backlogCreate._appendCreationEvent('fixture', { ok: true }), false)
      assert.equal(swapped, true)
      assert.equal(existsSync(join(outsideDir, '.events.jsonl')), false)
    } finally {
      childProcess.spawnSync = originalSpawnSync
      rmSync(creations, { recursive: true, force: true })
      renameSync(displaced, creations)
    }
  })

  check('creation marker enumeration rejects 10001 entries without materializing a partial result', () => {
    rmSync(creations, { recursive: true, force: true })
    mkdirSync(creations)
    for (let i = 0; i < 10001; i++) writeFileSync(join(creations, `noise-${String(i).padStart(5, '0')}`), '')
    const state = markers.scan()
    assert.equal(state.incomplete.length, 0)
    assert.equal(state.completedCount, 0)
    assert.ok(state.blocking.some((row) => row.code === 'CREATION_MARKER_DIR_TOO_LARGE'))
  })

  console.log(`creation-markers: ${checks} checks passed`)
} finally {
  rmSync(root, { recursive: true, force: true })
}
