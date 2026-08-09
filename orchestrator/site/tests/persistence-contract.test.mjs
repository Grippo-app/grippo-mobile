#!/usr/bin/env node

import assert from 'node:assert/strict'
import {
  existsSync,
  mkdirSync,
  mkdtempSync,
  readFileSync,
  rmSync,
  writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const root = mkdtempSync(join(tmpdir(), 'site-persistence-contract-'))
const cacheDir = join(root, 'orchestrator', '.cache')
const stateFile = join(cacheDir, 'site', '.site-state.json')
mkdirSync(join(cacheDir, 'site'), { recursive: true })

process.env.ORCHESTRATOR_PROJECT_ROOT = root
process.env.ORCHESTRATOR_CACHE_DIR = cacheDir
process.env.ORCHESTRATOR_STATE_FILE = stateFile

const persistence = require('../server/persistence.js')
const sse = require('../server/sse.js')

let checks = 0
function check(name, fn) {
  fn()
  checks++
  console.log(`ok ${checks} - ${name}`)
}
function write(value) {
  writeFileSync(stateFile, JSON.stringify(value, null, 2) + '\n')
}

try {
  check('a missing file yields the exact current default without creating state', () => {
    const value = persistence.readPersisted()
    assert.deepEqual(value, persistence.DEFAULT_PERSISTED)
    assert.equal(value.schemaVersion, persistence.SCHEMA_VERSION)
    assert.equal(existsSync(stateFile), false)
  })

  check('the exact current envelope round-trips without mutation', () => {
    const current = {
      ...persistence.DEFAULT_PERSISTED,
      setupForm: {
        firstDomain: 'auth',
        figmaLibraryUrl: 'https://www.figma.com/design/abcdefgh',
      },
      manualSteps: { 'setup:requirementsVerified': true },
      taskTiming: {
        TASK_1_existing: {
          startedAt: '2026-07-19T08:00:00.000Z',
          doneAt: null,
          durationMs: null,
        },
      },
      taskLifecycle: {
        TASK_1_existing: [{ column: 'todo', enteredAt: '2026-07-19T08:00:00.000Z', source: 'observed' }],
      },
      uiLang: 'uk',
      backendActiveEnvironmentId: 'stage',
      backendSelectionRevision: 7,
    }
    write(current)
    const before = readFileSync(stateFile, 'utf8')
    assert.deepEqual(persistence.readPersisted(), current)
    assert.equal(readFileSync(stateFile, 'utf8'), before)
    persistence.writePersisted(current)
    assert.deepEqual(JSON.parse(readFileSync(stateFile, 'utf8')), current)
    assert.equal(existsSync(stateFile + '.tmp'), false)
  })

  check('every non-current envelope fails closed without rewriting bytes', () => {
    const missingCurrentField = { ...persistence.DEFAULT_PERSISTED }
    delete missingCurrentField.appRunPreferences
    const invalidStates = [
      { ...persistence.DEFAULT_PERSISTED, schemaVersion: 2 },
      { ...persistence.DEFAULT_PERSISTED, version: 5 },
      { ...persistence.DEFAULT_PERSISTED, unexpected: true },
      missingCurrentField,
      { ...persistence.DEFAULT_PERSISTED, setupForm: { openApiSpecUrl: 'https://example.invalid/openapi.json' } },
      { ...persistence.DEFAULT_PERSISTED, setupForm: { futureField: 'unrecognized' } },
      { ...persistence.DEFAULT_PERSISTED, manualSteps: { STEP_1: 'yes' } },
    ]
    for (const invalid of invalidStates) {
      write(invalid)
      const before = readFileSync(stateFile, 'utf8')
      assert.throws(() => persistence.readPersisted(), (error) => error && error.code === 'SITE_STATE_INVALID')
      assert.equal(readFileSync(stateFile, 'utf8'), before)
    }
    writeFileSync(stateFile, '{broken json\n')
    const broken = readFileSync(stateFile, 'utf8')
    assert.throws(() => persistence.readPersisted(), (error) => error && error.code === 'SITE_STATE_INVALID')
    assert.equal(readFileSync(stateFile, 'utf8'), broken)
  })

  check('write rejects invalid state without replacing the current file', () => {
    const current = { ...persistence.DEFAULT_PERSISTED, uiLang: 'ru' }
    write(current)
    const before = readFileSync(stateFile, 'utf8')
    assert.throws(() => persistence.writePersisted({ ...current, version: 5 }),
      (error) => error && error.code === 'SITE_STATE_INVALID')
    assert.equal(readFileSync(stateFile, 'utf8'), before)
    assert.equal(existsSync(stateFile + '.tmp'), false)
  })

  check('missing INDEX authority preserves timing and lifecycle instead of projecting an empty board', () => {
    const taskTiming = {
      TASK_1_existing: {
        startedAt: '2026-07-19T08:00:00.000Z', doneAt: null, durationMs: null,
      },
    }
    const taskLifecycle = {
      TASK_1_existing: [{ column: 'todo', enteredAt: '2026-07-19T08:00:00.000Z', source: 'observed' }],
      TASK_2_done: [{ column: 'done', enteredAt: '2026-07-19T09:00:00.000Z', source: 'agent' }],
    }
    const result = sse.reconcileProgress(
      { taskTiming, taskLifecycle }, null, [], [], '2026-07-19T10:00:00.000Z',
    )
    assert.equal(result.timing.changed, false)
    assert.equal(result.lifecycle.changed, false)
    assert.deepEqual(result.timing.map, taskTiming)
    assert.deepEqual(result.lifecycle.map, taskLifecycle)
  })

  console.log(`persistence contract: ${checks} checks passed`)
} finally {
  rmSync(root, { recursive: true, force: true })
}
