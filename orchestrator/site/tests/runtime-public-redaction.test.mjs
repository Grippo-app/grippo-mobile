#!/usr/bin/env node

import assert from 'node:assert/strict'
import { mkdirSync, mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const base = mkdtempSync(join(tmpdir(), 'runtime-public-redaction-'))
const root = join(base, 'project')
const inside = join(root, 'orchestrator', '.cache', 'tasks', 'owner.json')
const outside = join(base, 'host-secret', 'owner.json')
mkdirSync(dirname(inside), { recursive: true })
mkdirSync(dirname(outside), { recursive: true })

process.env.ORCHESTRATOR_PROJECT_ROOT = root

let checks = 0
function check(name, fn) { fn(); checks++; console.log(`ok ${checks} - ${name}`) }

try {
  const runtimeIntegrity = require('../server/runtime-integrity.js')
  const taskIntegrity = require('../server/task-integrity.js')

  check('runtime paths are projected to one canonical repository-relative spelling', () => {
    assert.equal(runtimeIntegrity.projectPath(inside), 'orchestrator/.cache/tasks/owner.json')
    assert.equal(runtimeIntegrity.projectPath('orchestrator/.cache/tasks/owner.json'), 'orchestrator/.cache/tasks/owner.json')
    assert.equal(runtimeIntegrity.projectPath(outside), null)
    assert.equal(runtimeIntegrity.projectPath('../host-secret/owner.json'), null)
    assert.equal(runtimeIntegrity.projectPath('orchestrator/../.cache/tasks/owner.json'), '.cache/tasks/owner.json')
  })

  check('public findings redact outside/traversal paths and every project-root occurrence', () => {
    const projected = taskIntegrity.publicResult({
      version: 1,
      ok: false,
      scope: 'all',
      affectedStems: [],
      findings: [{
        code: 'RUNTIME_FIXTURE',
        severity: 'error',
        stem: null,
        paths: [inside, outside, '../host-secret/owner.json'],
        message: `failed below ${root}; foreign path ${outside}`,
        recovery: `inspect ${root}/orchestrator/.cache or /tmp/host-private`,
      }],
    })
    assert.deepEqual(projected.findings[0].paths, ['orchestrator/.cache/tasks/owner.json'])
    assert.equal(projected.findings[0].pathsRedacted, true)
    assert.equal(projected.truncated, true)
    const json = JSON.stringify(projected)
    assert.doesNotMatch(json, new RegExp(base.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
    assert.match(projected.findings[0].message, /<project-root>/)
    assert.match(projected.findings[0].message, /<absolute-path>/)
    assert.match(projected.findings[0].recovery, /<project-root>/)
    assert.match(projected.findings[0].recovery, /<absolute-path>/)
  })

  check('server validation emits one bounded observation on success and failure', () => {
    const core = require('../../tasks/task-state-core.cjs')
    const originalValidate = core.validateAction
    const originalValidateAll = core.validateTaskState
    const originalArchitecture = core.checkArchitectureState
    const originalWrite = process.stderr.write
    const lines = []
    process.stderr.write = function (chunk) { lines.push(String(chunk)); return true }
    try {
      core.validateAction = function ({ action, stem }) {
        return {
          version: 1, ok: true, overallOk: true, scope: stem, action,
          observedState: 'backlog', expectedState: null,
          snapshotHash: 'sha256:' + 'a'.repeat(64), findings: [],
          stats: { durationMs: 2, scanMode: 'stem-closure', taskBodyReads: 1 },
        }
      }
      taskIntegrity.validateAction('prep', 'TASK_1_fixture', 'runner')
      core.validateAction = function () { const error = new Error('SECRET TASK BODY'); error.code = 'FIXTURE_UNAVAILABLE'; throw error }
      assert.throws(() => taskIntegrity.validateAction('prep', 'TASK_1_fixture', 'server'), /SECRET TASK BODY/)
      core.validateTaskState = function () {
        return {
          version: 1, ok: true, overallOk: true, scope: 'all',
          observedState: null, expectedState: null,
          snapshotHash: 'sha256:' + 'b'.repeat(64), findings: [],
          stats: { durationMs: 3, scanMode: 'full', taskBodyReads: 2 },
        }
      }
      core.checkArchitectureState = function () {
        return { version: 1, checked: true, ok: true, status: 'fresh', fresh: true, findings: [] }
      }
      taskIntegrity.validateAll('server')
    } finally {
      core.validateAction = originalValidate
      core.validateTaskState = originalValidateAll
      core.checkArchitectureState = originalArchitecture
      process.stderr.write = originalWrite
    }
    assert.equal(lines.length, 3)
    const events = lines.map((line) => JSON.parse(line.replace(/^\[task-state\]\s*/, '')))
    assert.equal(events[0].caller, 'runner')
    assert.equal(events[0].ok, true)
    assert.deepEqual(events[1].findings, [{ code: 'FIXTURE_UNAVAILABLE', severity: 'blocker' }])
    assert.equal(events[0].result, 'valid')
    assert.equal(events[1].result, 'invalid')
    assert.equal(events[1].ok, false)
    assert.equal(events[2].caller, 'server')
    assert.equal(events[2].scope, 'all')
    assert.equal(events[2].action, 'integrity-scan')
    assert.equal(events[2].result, 'valid')
    assert.doesNotMatch(JSON.stringify(events), /SECRET|answer|prompt/i)
  })

  check('error and stats projections drop absolute paths and unknown private fields', () => {
    const error = new Error(`failed at ${outside} with SECRET`)
    const response = taskIntegrity.errorResponse(error)
    assert.equal(response.body.detail, 'Required task-state authority is unavailable or malformed.')
    assert.doesNotMatch(JSON.stringify(response), new RegExp(base.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
    assert.doesNotMatch(JSON.stringify(response), /SECRET/)
    const projected = taskIntegrity.publicResult({
      version: 1, ok: true, affectedStems: [], findings: [],
      stats: { durationMs: 1, scanMode: 'full', privatePath: outside },
      runtimeStats: { inspected: true, statuses: 1, findings: 0, snapshotInputs: 1, truncated: false, secret: 'SECRET' },
    })
    assert.deepEqual(projected.stats, { durationMs: 1, scanMode: 'full' })
    assert.deepEqual(projected.runtimeStats, { inspected: true, truncated: false, statuses: 1, findings: 0, snapshotInputs: 1 })
    assert.doesNotMatch(JSON.stringify(projected), /SECRET|privatePath|host-secret/)
  })

  check('public finding machine fields reject arbitrary owner text', () => {
    const projected = taskIntegrity.publicResult({
      version: 1, ok: false, affectedStems: [], findings: [{
        code: 'secret:/private/path', severity: 'catastrophic', stem: null,
        paths: [], message: 'bounded diagnostic', recovery: 'bounded recovery',
      }],
    })
    assert.equal(projected.findings[0].code, 'TASK_STATE_FINDING')
    assert.equal(projected.findings[0].severity, 'error')
    assert.doesNotMatch(JSON.stringify(projected), /secret:|catastrophic/)
  })

  console.log(`runtime-public-redaction: ${checks} checks passed`)
} finally {
  rmSync(base, { recursive: true, force: true })
}
