#!/usr/bin/env node

import assert from 'node:assert/strict'
import {
  closeSync, existsSync, lstatSync, mkdirSync, mkdtempSync, readFileSync,
  readdirSync, renameSync, rmSync, symlinkSync, unlinkSync, writeFileSync, writeSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const base = mkdtempSync(join(tmpdir(), 'runtime-path-safety-'))
const root = join(base, 'project')
const outside = join(base, 'outside')
const orchestrator = join(root, 'orchestrator')
const cache = join(orchestrator, '.cache', 'tasks')
const outsideRuns = join(outside, 'runs')
for (const directory of [root, orchestrator, cache, outsideRuns]) mkdirSync(directory, { recursive: true })

if (process.platform === 'win32') {
  console.log('runtime-path-safety: skipped (symlink ancestor fixture requires Unix semantics)')
  rmSync(base, { recursive: true, force: true })
  process.exit(0)
}

const redirectedParent = join(orchestrator, 'runtime-link')
symlinkSync(outside, redirectedParent, 'dir')
const unsafeRuns = join(redirectedParent, 'runs')
const sidecar = join(outsideRuns, 'task_TASK_1_escape.session.json')
const originalSidecar = Buffer.from(JSON.stringify({
  version: 1,
  key: 'task:TASK_1_escape', stem: 'TASK_1_escape', action: 'prep',
  running: true, awaitingTurn: false,
  startedAt: '2026-07-13T00:00:00.000Z', endedAt: null,
  exitCode: null, canceled: false, sessionId: null, nextSeq: 0, minSeq: 0,
}, null, 2) + '\n')
writeFileSync(sidecar, originalSidecar)
writeFileSync(join(outsideRuns, '.cli-install.log'), 'outside install secret\n')
writeFileSync(join(outsideRuns, '.cli-login.log'), 'outside login secret\n')

for (const name of [
  'locks', 'requests', 'request-reservations', 'superseded', 'finalizations',
  'creations', 'edits', 'intake',
]) mkdirSync(join(cache, name), { recursive: true })

process.env.ORCHESTRATOR_PROJECT_ROOT = root
process.env.ORCHESTRATOR_RUNS_DIR = unsafeRuns
process.env.ORCHESTRATOR_LOCKS_DIR = join(cache, 'locks')
process.env.ORCHESTRATOR_REQUESTS_DIR = join(cache, 'requests')
process.env.ORCHESTRATOR_REQUEST_RESERVATIONS_DIR = join(cache, 'request-reservations')
process.env.ORCHESTRATOR_SUPERSEDED_DIR = join(cache, 'superseded')
process.env.ORCHESTRATOR_FINALIZATIONS_DIR = join(cache, 'finalizations')
process.env.ORCHESTRATOR_TASK_CREATIONS_DIR = join(cache, 'creations')
process.env.ORCHESTRATOR_TASK_EDITS_DIR = join(cache, 'edits')
process.env.ORCHESTRATOR_TASK_INTAKE_DIR = join(cache, 'intake')
process.env.RUNNER_DISABLED = '1'

const childProcess = require('node:child_process')
const originalSpawn = childProcess.spawn
let spawnCount = 0
childProcess.spawn = function () {
  spawnCount++
  throw new Error('unsafe runtime must refuse before spawn')
}

let checks = 0
function check(name, fn) { fn(); checks++; console.log(`ok ${checks} - ${name}`) }

try {
  const sessions = require('../server/sessions.js')
  const runner = require('../server/runner.js')
  const cli = require('../server/cli.js')
  const fileGuards = require('../server/file-guards.js')
  const paths = require('../server/paths.js')

  check('sidecar caches follow the isolated project runtime root', () => {
    assert.equal(paths.FIGMA_CACHE_DIR, join(root, 'orchestrator', '.cache', 'figma'))
    assert.equal(paths.API_CONTRACT_CACHE_DIR, join(root, 'orchestrator', '.cache', 'api-contract'))
  })

  check('session start rejects a symlink ancestor before lease, transcript, or child spawn', () => {
    const status = sessions.start('setup', { action: 'prep', prompt: 'must not execute' })
    assert.equal(status.running, false)
    assert.equal(status.error, 'session-runtime-unsafe')
    assert.equal(spawnCount, 0)
    assert.equal(existsSync(join(outsideRuns, 'setup.events.jsonl')), false)
    assert.equal(existsSync(join(outsideRuns, 'setup.session.json')), false)
  })

  check('session init/reconcile leaves redirected outside sidecars byte-for-byte untouched', () => {
    sessions.init()
    assert.deepEqual(readFileSync(sidecar), originalSidecar)
    assert.equal(sessions.boundedRunsNames().ok, false)
  })

  check('runner directory and marker writers fail closed without touching outside', () => {
    assert.equal(runner.ensureRunsDir(), false)
    assert.equal(runner.touchMarker(), false)
    assert.equal(existsSync(join(outsideRuns, '.runner-alive')), false)
  })

  check('CLI install and fresh login refuse before every subprocess spawn', () => {
    const install = cli.install()
    const login = cli.login({ fresh: true })
    assert.equal(install.running, false)
    assert.equal(install.error, 'cli-runtime-unsafe')
    assert.equal(login.running, false)
    assert.equal(login.error, 'cli-runtime-unsafe')
    assert.equal(spawnCount, 0)
  })

  check('CLI log reads never follow the redirected runtime ancestor', () => {
    assert.equal(cli.readJobLog('install'), '')
    assert.equal(cli.readJobLog('login'), '')
  })

  check('atomic runtime publication replaces a final symlink instead of following it', () => {
    const safeRuns = join(root, 'safe-runs')
    mkdirSync(safeRuns)
    const outsideTarget = join(outside, 'must-not-change.txt')
    const target = join(safeRuns, 'events.jsonl')
    writeFileSync(outsideTarget, 'outside-original\n')
    symlinkSync(outsideTarget, target)
    const fd = fileGuards.openAtomicReplaceRegularFile(root, safeRuns, target, {
      create: true, mode: 0o600, initialBytes: Buffer.from('inside-start\n'), maxBytes: 1024,
    })
    assert.notEqual(fd, null)
    writeSync(fd, Buffer.from('inside-end\n'))
    closeSync(fd)
    assert.equal(lstatSync(target).isSymbolicLink(), false)
    assert.equal(readFileSync(target, 'utf8'), 'inside-start\ninside-end\n')
    assert.equal(readFileSync(outsideTarget, 'utf8'), 'outside-original\n')
  })

  check('concurrent ancestor swap cannot redirect even temporary publication bytes', () => {
    const parent = join(root, 'race-parent')
    const raceRuns = join(parent, 'runs')
    const displaced = join(root, 'race-parent-displaced')
    const outsideRace = join(outside, 'race-target')
    mkdirSync(raceRuns, { recursive: true })
    mkdirSync(join(outsideRace, 'runs'), { recursive: true })
    const originalSpawnSync = childProcess.spawnSync
    let swapped = false
    childProcess.spawnSync = function (command, args, options) {
      const request = options && typeof options.input === 'string' ? JSON.parse(options.input) : null
      if (!swapped && request && request.action === 'atomic-replace') {
        swapped = true
        renameSync(parent, displaced)
        symlinkSync(outsideRace, parent, 'dir')
      }
      return originalSpawnSync.call(childProcess, command, args, options)
    }
    try {
      const result = fileGuards.atomicReplaceRegularFile(
        root, raceRuns, join(raceRuns, 'race.json'), Buffer.from('must-stay-inside\n'),
        { create: true, mode: 0o600, maxBytes: 1024 },
      )
      assert.equal(result, false)
      assert.equal(swapped, true)
      assert.deepEqual(readdirSync(join(outsideRace, 'runs')), [])
      assert.equal(existsSync(join(outsideRace, 'runs', 'race.json')), false)
    } finally {
      childProcess.spawnSync = originalSpawnSync
      if (swapped) {
        rmSync(parent, { recursive: true, force: true })
        renameSync(displaced, parent)
      }
    }
  })

  check('no-clobber queue publication remains pinned across an ancestor swap', () => {
    const parent = join(root, 'claim-race-parent')
    const queue = join(parent, 'requests')
    const displaced = join(root, 'claim-race-parent-displaced')
    const outsideRace = join(outside, 'claim-race-target')
    mkdirSync(queue, { recursive: true })
    mkdirSync(join(outsideRace, 'requests'), { recursive: true })
    const originalSpawnSync = childProcess.spawnSync
    let swapped = false
    childProcess.spawnSync = function (command, args, options) {
      const request = options && typeof options.input === 'string' ? JSON.parse(options.input) : null
      if (!swapped && request && request.action === 'publish-no-clobber') {
        swapped = true
        renameSync(parent, displaced)
        symlinkSync(outsideRace, parent, 'dir')
      }
      return originalSpawnSync.call(childProcess, command, args, options)
    }
    try {
      const result = fileGuards.publishNoClobberRegularFileUnder(
        root, queue, join(queue, 'intent.json'), Buffer.from('private intent\n'),
        { create: true, mode: 0o600, maxBytes: 1024 },
      )
      assert.equal(result.ok, false)
      assert.equal(swapped, true)
      assert.deepEqual(readdirSync(join(outsideRace, 'requests')), [])
      assert.equal(existsSync(join(outsideRace, 'requests', 'intent.json')), false)
    } finally {
      childProcess.spawnSync = originalSpawnSync
      if (swapped) {
        rmSync(parent, { recursive: true, force: true })
        renameSync(displaced, parent)
      }
    }
  })

  check('authenticated unlink never deletes a raced replacement', () => {
    const directory = join(root, 'unlink-race')
    const target = join(directory, 'reservation.json')
    mkdirSync(directory)
    writeFileSync(target, 'owned-generation\n')
    const originalSpawnSync = childProcess.spawnSync
    let swapped = false
    childProcess.spawnSync = function (command, args, options) {
      const request = options && typeof options.input === 'string' ? JSON.parse(options.input) : null
      if (!swapped && request && request.action === 'quarantine-finalize' && request.decision === 'delete') {
        swapped = true
        writeFileSync(target, 'replacement-generation\n')
      }
      return originalSpawnSync.call(childProcess, command, args, options)
    }
    try {
      const removed = fileGuards.unlinkRegularFileIfUnder(
        root, directory, target, 1024,
        (bounded) => bounded.bytes.toString('utf8') === 'owned-generation\n',
      )
      assert.equal(removed, true)
      assert.equal(swapped, true)
      assert.equal(readFileSync(target, 'utf8'), 'replacement-generation\n')
      assert.deepEqual(readdirSync(directory), ['reservation.json'])
    } finally { childProcess.spawnSync = originalSpawnSync }
  })

  check('rejected conditional unlink never overwrites a raced replacement', () => {
    const directory = join(root, 'unlink-restore-race')
    const target = join(directory, 'reservation.json')
    mkdirSync(directory)
    writeFileSync(target, 'owned-generation\n')
    const originalSpawnSync = childProcess.spawnSync
    let swapped = false
    childProcess.spawnSync = function (command, args, options) {
      const request = options && typeof options.input === 'string' ? JSON.parse(options.input) : null
      if (!swapped && request && request.action === 'quarantine-finalize' && request.decision === 'restore') {
        swapped = true
        writeFileSync(target, 'replacement-generation\n')
      }
      return originalSpawnSync.call(childProcess, command, args, options)
    }
    try {
      const removed = fileGuards.unlinkRegularFileIfUnder(
        root, directory, target, 1024,
        () => false,
      )
      assert.equal(removed, false)
      assert.equal(swapped, true)
      assert.equal(readFileSync(target, 'utf8'), 'replacement-generation\n')
      const evidence = readdirSync(directory).filter((name) => name !== 'reservation.json')
      const captures = evidence.filter((name) => name.startsWith('.guard-capture-'))
      const wal = evidence.filter((name) => name.startsWith('.guard-txn-'))
      assert.equal(captures.length, 1)
      assert.equal(wal.length, 2, 'manifest plus durable restore decision retain exact recovery authority')
      assert.equal(readFileSync(join(directory, captures[0]), 'utf8'), 'owned-generation\n')
    } finally { childProcess.spawnSync = originalSpawnSync }
  })

  check('guarded operations never mutate the long-lived server cwd', () => {
    const directory = join(root, 'cwd-isolation')
    const target = join(directory, 'state.json')
    mkdirSync(directory)
    const originalChdir = process.chdir
    let calls = 0
    process.chdir = function () { calls++; throw new Error('server process chdir is forbidden') }
    try {
      assert.equal(fileGuards.atomicReplaceRegularFile(
        root, directory, target, Buffer.from('isolated\n'),
        { mode: 0o600, maxBytes: 1024 },
      ), true)
      assert.equal(calls, 0)
      assert.equal(readFileSync(target, 'utf8'), 'isolated\n')
    } finally { process.chdir = originalChdir }
  })

  console.log(`runtime-path-safety: ${checks} checks passed`)
} finally {
  childProcess.spawn = originalSpawn
  rmSync(base, { recursive: true, force: true })
}
