#!/usr/bin/env node

import assert from 'node:assert/strict'
import { mkdtempSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const root = mkdtempSync(join(tmpdir(), 'runtime-scan-bounds-'))
const cache = join(root, 'orchestrator', '.cache', 'tasks')
const runs = join(cache, 'runs')
const requestsDir = join(cache, 'requests')
const locksDir = join(cache, 'locks')
for (const dir of [runs, requestsDir, locksDir]) mkdirSync(dir, { recursive: true })

process.env.ORCHESTRATOR_PROJECT_ROOT = root
process.env.ORCHESTRATOR_RUNS_DIR = runs
process.env.ORCHESTRATOR_REQUESTS_DIR = requestsDir
process.env.ORCHESTRATOR_LOCKS_DIR = locksDir
process.env.RUNNER_DISABLED = '1'

const childProcess = require('node:child_process')
const sessions = require('../server/sessions.js')
const runner = require('../server/runner.js')
const sidecar = join(runs, 'task_TASK_1_overflow.session.json')
writeFileSync(sidecar, JSON.stringify({
  version: 1,
  key: 'task:TASK_1_overflow', stem: 'TASK_1_overflow', action: 'prep',
  running: true, awaitingTurn: false,
  startedAt: '2026-07-13T00:00:00.000Z', endedAt: null,
  exitCode: null, canceled: false, sessionId: null, nextSeq: 0, minSeq: 0
}, null, 2) + '\n')
const before = readFileSync(sidecar)

const originalSpawnSync = childProcess.spawnSync

try {
  childProcess.spawnSync = function (command, args, options) {
    const request = options && typeof options.input === 'string' ? JSON.parse(options.input) : null
    if (request && request.action === 'directory-names') {
      return { status: 0, stdout: JSON.stringify({ ok: false, code: 'directory-entry-limit' }), stderr: '' }
    }
    return originalSpawnSync.call(childProcess, command, args, options)
  }

  const sessionScan = sessions.boundedRunsNames()
  assert.equal(sessionScan.ok, false)
  assert.equal(sessionScan.code, 'runs-directory-entry-limit')

  const runnerScan = runner.boundedRuntimeNames(runs)
  assert.equal(runnerScan.ok, false)
  assert.equal(runnerScan.code, 'directory-entry-limit')

  // Boot reconciliation is a mutation path. An incomplete directory snapshot
  // must leave even a persisted `running:true` sidecar byte-for-byte untouched.
  sessions.init()
  assert.deepEqual(readFileSync(sidecar), before)
  assert.deepEqual(sessions.list(), {})

  const runnerSource = readFileSync(new URL('../server/runner.js', import.meta.url), 'utf8')
  const sessionsSource = readFileSync(new URL('../server/sessions.js', import.meta.url), 'utf8')
  assert.doesNotMatch(runnerSource, /readdirSync/)
  assert.doesNotMatch(sessionsSource, /readdirSync/)

  console.log('runtime-scan-bounds: 5 checks passed')
} finally {
  childProcess.spawnSync = originalSpawnSync
  rmSync(root, { recursive: true, force: true })
}
