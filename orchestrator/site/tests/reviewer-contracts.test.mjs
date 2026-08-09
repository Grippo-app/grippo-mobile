#!/usr/bin/env node

import assert from 'node:assert/strict'
import {
  chmodSync,
  linkSync,
  mkdirSync,
  mkdtempSync,
  readFileSync,
  realpathSync,
  rmSync,
  unlinkSync,
  writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { EventEmitter } from 'node:events'
import { PassThrough } from 'node:stream'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const require = createRequire(import.meta.url)
const REPO = fileURLToPath(new URL('../../../', import.meta.url))
const root = mkdtempSync(join(tmpdir(), 'reviewer-contracts-'))
const orchestrator = join(root, 'orchestrator')
const tasksDir = join(orchestrator, 'tasks')
const cacheDir = join(orchestrator, '.cache')
const journalDir = join(cacheDir, 'tasks', 'journal')
const runsDir = join(cacheDir, 'tasks', 'runs')
const finalizationsDir = join(cacheDir, 'tasks', 'finalizations')
const stateFile = join(cacheDir, 'site', '.site-state.json')
const configFile = join(orchestrator, 'project-config.md')
const home = join(root, 'home')

for (const dir of [
  join(tasksDir, 'backlog'),
  join(tasksDir, 'pending'),
  join(tasksDir, 'todo'),
  join(tasksDir, 'done'),
  journalDir,
  runsDir,
  finalizationsDir,
  join(stateFile, '..'),
  home,
]) mkdirSync(dir, { recursive: true })

process.env.ORCHESTRATOR_PROJECT_ROOT = root
process.env.ORCHESTRATOR_CACHE_DIR = cacheDir
process.env.ORCHESTRATOR_STATE_FILE = stateFile
process.env.ORCHESTRATOR_JOURNAL_DIR = journalDir
process.env.ORCHESTRATOR_RUNS_DIR = runsDir
process.env.ORCHESTRATOR_FINALIZATIONS_DIR = finalizationsDir

const originalConfig = [
  '---',
  '# Reviewer policy is edited in place.',
  'codexEnabled: auto # canonical Reviewer backing field',
  'figmaLibraryUrl: <figma-file-url>',
  'productName: Fixture',
  '---',
  '',
  '# Body',
  '',
  'Keep this body byte-for-byte.',
  '',
].join('\n')
writeFileSync(configFile, originalConfig, { mode: 0o640 })

const detector = require('../../tasks/reviewer-status.cjs')
const configUpdate = require('../server/project-config-update.js')
const finalizations = require('../server/finalizations.js')
const persistence = require('../server/persistence.js')

let checks = 0
function check(name, fn) {
  fn()
  checks++
  console.log(`ok ${checks} - ${name}`)
}
async function checkAsync(name, fn) {
  await fn()
  checks++
  console.log(`ok ${checks} - ${name}`)
}

function journal(stem, events) {
  writeFileSync(join(journalDir, stem + '.jsonl'), events.map((row) => JSON.stringify(row)).join('\n') + '\n', {
    mode: 0o600,
  })
}

function event(stem, ts, kind, status, meta) {
  const value = { ts, stem, kind, phase: 'review', status }
  if (meta) value.meta = meta
  return value
}

try {
  check('shared detector requires the active official plugin readiness contract without a review probe', () => {
    const empty = detector.detect({
      projectRoot: root,
      home,
      pathValue: '',
      env: { HOME: home, PATH: '' },
      now: Date.parse('2026-07-16T08:00:00Z'),
    })
    assert.deepEqual(
      [empty.availability, empty.installed, empty.reasonCode, empty.source],
      ['unavailable', 'no', 'codex-not-installed', 'none'],
    )

    const cliOnlyBin = join(root, 'cli-only-bin')
    mkdirSync(cliOnlyBin)
    writeFileSync(join(cliOnlyBin, 'codex'), '#!/bin/sh\nexit 0\n')
    chmodSync(join(cliOnlyBin, 'codex'), 0o700)
    const cliOnly = detector.detect({
      projectRoot: root,
      home,
      pathValue: cliOnlyBin,
      env: { HOME: home, PATH: cliOnlyBin },
    })
    assert.deepEqual(
      [cliOnly.availability, cliOnly.installed, cliOnly.reasonCode, cliOnly.source],
      ['unavailable', 'no', 'codex-not-installed', 'none'],
    )

    const pluginRoot = join(root, 'official-plugin')
    const setupScript = join(pluginRoot, 'scripts', 'codex-companion.mjs')
    mkdirSync(join(pluginRoot, 'scripts'), { recursive: true })
    writeFileSync(setupScript, '// fixture path; invocation is mocked\n')
    const pluginBin = join(root, 'plugin-bin')
    mkdirSync(pluginBin)
    writeFileSync(join(pluginBin, 'claude'), '#!/bin/sh\nexit 0\n')
    chmodSync(join(pluginBin, 'claude'), 0o700)
    const installedRow = (overrides = {}) => ({
      id: 'codex@openai-codex',
      version: '1.0.6',
      installPath: pluginRoot,
      installedAt: '2026-07-16T08:00:00Z',
      lastUpdated: '2026-07-16T08:00:00Z',
      scope: 'user',
      enabled: true,
      errors: [],
      ...overrides,
    })
    const readyReport = (overrides = {}) => ({
      ready: true,
      node: { available: true },
      codex: { available: true },
      auth: { loggedIn: true },
      ...overrides,
    })
    let invocation = null
    const available = detector.detect({
      projectRoot: root,
      home,
      pathValue: pluginBin,
      env: {
        HOME: home,
        PATH: pluginBin,
        NODE_OPTIONS: '--require=/tmp/untrusted-preload.cjs',
        NODE_PATH: '/tmp/untrusted-node-path',
        LD_PRELOAD: '/tmp/untrusted-loader.so',
      },
      spawnSync(command, args, options) {
        if (args[0] === 'plugin') {
          return { status: 0, stdout: JSON.stringify([installedRow()]), stderr: '' }
        }
        invocation = { command, args, options }
        return { status: 0, stdout: JSON.stringify(readyReport()), stderr: '' }
      },
    })
    assert.equal(available.availability, 'available')
    assert.equal(available.installed, 'yes')
    assert.equal(available.source, 'claude-plugin')
    assert.equal(invocation.command, process.execPath)
    assert.deepEqual(invocation.args, [realpathSync(setupScript), 'setup', '--json'])
    assert.equal(invocation.options.timeout, detector.DEFAULT_TIMEOUT_MS)
    for (const key of ['NODE_OPTIONS', 'NODE_PATH', 'LD_PRELOAD']) {
      assert.equal(Object.hasOwn(invocation.options.env, key), false, key)
    }
    assert.equal(invocation.options.env.PATH, pluginBin)
    assert.equal(invocation.options.env.NO_COLOR, '1')

    const failed = detector.detect({
      projectRoot: root,
      home,
      pathValue: pluginBin,
      env: { HOME: home, PATH: pluginBin },
      spawnSync(command, args) {
        return args[0] === 'plugin'
          ? { status: 0, stdout: JSON.stringify([installedRow()]), stderr: '' }
          : { status: 1, stdout: '', stderr: 'private raw failure' }
      },
    })
    assert.deepEqual(
      [failed.availability, failed.installed, failed.reasonCode],
      ['unavailable', 'yes', 'codex-invocation-failed'],
    )
    assert.equal(JSON.stringify(failed).includes('private raw failure'), false)

    const timedOut = detector.detect({
      projectRoot: root,
      home,
      pathValue: pluginBin,
      env: { HOME: home, PATH: pluginBin },
      spawnSync(command, args) {
        return args[0] === 'plugin'
          ? { status: 0, stdout: JSON.stringify([installedRow()]), stderr: '' }
          : { error: { code: 'ETIMEDOUT' } }
      },
    })
    assert.deepEqual(
      [timedOut.availability, timedOut.installed, timedOut.reasonCode],
      ['unknown', 'yes', 'codex-check-timeout'],
    )
    const outputLimited = detector.detect({
      projectRoot: root,
      home,
      pathValue: pluginBin,
      env: { HOME: home, PATH: pluginBin },
      spawnSync(command, args) {
        return args[0] === 'plugin'
          ? { status: 0, stdout: JSON.stringify([installedRow()]), stderr: '' }
          : { error: { code: 'ENOBUFS' } }
      },
    })
    assert.deepEqual(
      [outputLimited.availability, outputLimited.installed, outputLimited.reasonCode],
      ['unknown', 'yes', 'codex-check-output-limit'],
    )

    const stalePlugin = join(home, '.claude', 'plugins', 'cache', 'openai-codex', 'codex', 'stale')
    mkdirSync(join(stalePlugin, '.claude-plugin'), { recursive: true })
    mkdirSync(join(stalePlugin, 'agents'))
    writeFileSync(join(stalePlugin, '.claude-plugin', 'plugin.json'), JSON.stringify({ name: 'codex' }))
    writeFileSync(join(stalePlugin, 'agents', 'codex-review.md'), '# stale cache\n')
    const staleCacheIgnored = detector.detect({
      projectRoot: root,
      home,
      pathValue: '',
      env: { HOME: home, PATH: '' },
    })
    assert.deepEqual(
      [staleCacheIgnored.availability, staleCacheIgnored.installed, staleCacheIgnored.reasonCode],
      ['unavailable', 'no', 'codex-not-installed'],
    )

    const authMissing = detector.detect({
      projectRoot: root,
      home,
      pathValue: pluginBin,
      env: { HOME: home, PATH: pluginBin },
      spawnSync(command, args) {
        return args[0] === 'plugin'
          ? { status: 0, stdout: JSON.stringify([installedRow()]), stderr: '' }
          : {
              status: 0,
              stdout: JSON.stringify(readyReport({
                ready: false,
                auth: { loggedIn: false },
              })),
              stderr: '',
            }
      },
    })
    assert.deepEqual(
      [authMissing.availability, authMissing.installed, authMissing.reasonCode],
      ['unavailable', 'yes', 'codex-auth-missing'],
    )

    const disabledPlugin = detector.detect({
      projectRoot: root,
      home,
      pathValue: pluginBin,
      env: { HOME: home, PATH: pluginBin },
      spawnSync() {
        return {
          status: 0,
          stdout: JSON.stringify([installedRow({ enabled: false })]),
          stderr: '',
        }
      },
    })
    assert.deepEqual(
      [disabledPlugin.availability, disabledPlugin.installed, disabledPlugin.reasonCode],
      ['unavailable', 'yes', 'codex-plugin-disabled'],
    )

    const brokenPlugin = detector.detect({
      projectRoot: root,
      home,
      pathValue: pluginBin,
      env: { HOME: home, PATH: pluginBin },
      spawnSync() {
        return {
          status: 0,
          stdout: JSON.stringify([installedRow({ errors: ['load failed'] })]),
          stderr: '',
        }
      },
    })
    assert.deepEqual(
      [brokenPlugin.availability, brokenPlugin.installed, brokenPlugin.reasonCode],
      ['unavailable', 'yes', 'codex-plugin-broken'],
    )

    const contractMissing = detector.detect({
      projectRoot: root,
      home,
      pathValue: pluginBin,
      env: { HOME: home, PATH: pluginBin },
      spawnSync() {
        return {
          status: 0,
          stdout: JSON.stringify([installedRow({ installPath: join(root, 'missing-plugin') })]),
          stderr: '',
        }
      },
    })
    assert.deepEqual(
      [contractMissing.availability, contractMissing.installed, contractMissing.reasonCode],
      ['unavailable', 'yes', 'codex-contract-missing'],
    )

    const malformedPluginInventory = detector.detect({
      projectRoot: root,
      home,
      pathValue: pluginBin,
      env: { HOME: home, PATH: pluginBin },
      spawnSync() { return { status: 0, stdout: '{"installed":[]}', stderr: '' } },
    })
    assert.deepEqual(
      [malformedPluginInventory.availability, malformedPluginInventory.installed,
        malformedPluginInventory.reasonCode],
      ['unknown', 'unknown', 'codex-check-malformed'],
    )
    const duplicatePluginInventory = detector.detect({
      projectRoot: root,
      home,
      pathValue: pluginBin,
      env: { HOME: home, PATH: pluginBin },
      spawnSync() {
        return { status: 0, stdout: JSON.stringify([installedRow(), installedRow()]), stderr: '' }
      },
    })
    assert.deepEqual(
      [duplicatePluginInventory.availability, duplicatePluginInventory.installed,
        duplicatePluginInventory.reasonCode],
      ['unknown', 'yes', 'codex-check-malformed'],
    )

    const malformedReadiness = detector.detect({
      projectRoot: root,
      home,
      pathValue: pluginBin,
      env: { HOME: home, PATH: pluginBin },
      spawnSync(command, args) {
        return args[0] === 'plugin'
          ? { status: 0, stdout: JSON.stringify([installedRow()]), stderr: '' }
          : {
              status: 0,
              stdout: JSON.stringify(readyReport({ ready: false })),
              stderr: '',
            }
      },
    })
    assert.deepEqual(
      [malformedReadiness.availability, malformedReadiness.installed, malformedReadiness.reasonCode],
      ['unknown', 'yes', 'codex-check-malformed'],
    )
  })

  check('project-config updater preserves unrelated bytes and enforces CAS/capability/inode guards', () => {
    const before = configUpdate.read()
    assert.equal(before.ok, true)
    assert.equal(before.codexEnabled, 'auto')
    const updated = configUpdate.update({
      capability: 'reviewer',
      field: 'codexEnabled',
      value: 'true',
      expectedRevision: before.revision,
    })
    assert.equal(updated.ok, true)
    assert.equal(updated.value, 'true')
    const text = readFileSync(configFile, 'utf8')
    assert.equal(
      text.replace('codexEnabled: true # canonical Reviewer backing field',
        'codexEnabled: auto # canonical Reviewer backing field'),
      originalConfig,
    )
    assert.equal((readFileSync(configFile).length > 0), true)

    const stale = configUpdate.update({
      capability: 'reviewer',
      field: 'codexEnabled',
      value: 'false',
      expectedRevision: before.revision,
    })
    assert.equal(stale.error, 'project-config-revision-conflict')
    assert.equal(configUpdate.update({
      capability: 'reviewer',
      field: 'figmaLibraryUrl',
      value: 'https://www.figma.com/design/abcdefgh',
      expectedRevision: updated.revision,
    }).error, 'config-field-forbidden')

    const sibling = join(orchestrator, 'project-config-hardlink.md')
    linkSync(configFile, sibling)
    assert.equal(configUpdate.read().error, 'project-config-unsafe')
    unlinkSync(sibling)

    const valid = readFileSync(configFile)
    const withoutReviewerField = valid.toString('utf8').replace(
      'codexEnabled: true # canonical Reviewer backing field\n',
      '',
    )
    writeFileSync(configFile, withoutReviewerField)
    const missing = configUpdate.read()
    assert.equal(missing.ok, true)
    assert.equal(missing.hasCodexField, false)
    const inserted = configUpdate.update({
      capability: 'reviewer',
      field: 'codexEnabled',
      value: 'false',
      expectedRevision: missing.revision,
    })
    assert.equal(inserted.ok, true)
    const insertedText = readFileSync(configFile, 'utf8')
    assert.equal((insertedText.match(/^codexEnabled:/gm) || []).length, 1)
    assert.equal(insertedText.replace('codexEnabled: false\n', ''), withoutReviewerField)

    writeFileSync(configFile, valid)
    writeFileSync(configFile, valid.toString('utf8').replace(
      'codexEnabled: true # canonical Reviewer backing field',
      'codexEnabled: true # canonical Reviewer backing field\ncodexEnabled: false',
    ))
    assert.equal(configUpdate.read().error, 'project-config-duplicate-key')
    writeFileSync(configFile, valid)
    writeFileSync(configFile, 'codexEnabled: auto\n')
    assert.equal(configUpdate.read().error, 'project-config-frontmatter-missing')
    writeFileSync(configFile, originalConfig, { mode: 0o640 })

    const originalEndMutation = finalizations.endMutation
    finalizations.endMutation = function (handle) {
      originalEndMutation(handle)
      return false
    }
    try {
      const current = configUpdate.read()
      const releaseFailed = configUpdate.update({
        capability: 'reviewer',
        field: 'codexEnabled',
        value: 'false',
        expectedRevision: current.revision,
      })
      assert.equal(releaseFailed.ok, false)
      assert.equal(releaseFailed.status, 503)
      assert.equal(releaseFailed.error, 'writer-lease-release-failed')
      assert.equal(configUpdate.read().codexEnabled, 'false')
    } finally {
      finalizations.endMutation = originalEndMutation
      writeFileSync(configFile, originalConfig, { mode: 0o640 })
    }
  })

  check('persistence accepts only the exact current envelope and never rewrites rejected state', () => {
    writeFileSync(stateFile, JSON.stringify({
      ...persistence.DEFAULT_PERSISTED,
      setupForm: { productName: 'Persisted' },
      uiLang: 'ru',
    }))
    const current = persistence.readPersisted()
    assert.equal(current.schemaVersion, 1)
    assert.deepEqual(current.setupForm, { productName: 'Persisted' })
    assert.equal(current.uiLang, 'ru')

    writeFileSync(stateFile, JSON.stringify({
      ...persistence.DEFAULT_PERSISTED,
      setupForm: { productName: 'Persisted', codexEnabled: 'false' },
      uiLang: 'ru',
    }))
    assert.throws(() => persistence.readPersisted(), (error) => error && error.code === 'SITE_STATE_INVALID')

    writeFileSync(stateFile, JSON.stringify({
      ...persistence.DEFAULT_PERSISTED,
      setupForm: { productName: 'Persisted', futureField: 'not-known-legacy' },
    }))
    assert.throws(() => persistence.readPersisted(), (error) => error && error.code === 'SITE_STATE_INVALID')

    writeFileSync(stateFile, JSON.stringify({
      ...persistence.DEFAULT_PERSISTED,
      version: 5,
    }))
    const oldBytes = readFileSync(stateFile, 'utf8')
    assert.throws(() => persistence.readPersisted(), (error) => error && error.code === 'SITE_STATE_INVALID')
    assert.equal(readFileSync(stateFile, 'utf8'), oldBytes)
  })

  await checkAsync('server detector normalizes malformed output, caches results, and deduplicates forced checks', async () => {
    const childProcess = require('node:child_process')
    const detectorPath = require.resolve('../server/reviewer-detector.js')
    const originalSpawn = childProcess.spawn
    const originalNodeOptions = process.env.NODE_OPTIONS
    const originalNodePath = process.env.NODE_PATH
    const originalLdPreload = process.env.LD_PRELOAD
    process.env.NODE_OPTIONS = '--require=/tmp/untrusted-preload.cjs'
    process.env.NODE_PATH = '/tmp/untrusted-node-path'
    process.env.LD_PRELOAD = '/tmp/untrusted-loader.so'
    let spawnCount = 0
    const spawnEnvironments = []
    childProcess.spawn = function (command, args, options) {
      spawnCount++
      spawnEnvironments.push(options.env)
      const child = new EventEmitter()
      child.stdout = new PassThrough()
      child.stderr = new PassThrough()
      child.kill = function () {}
      process.nextTick(function () {
        if (spawnCount === 1) {
          child.stdout.end('{malformed')
        } else if (spawnCount === 2) {
          child.stdout.end(JSON.stringify({
            schemaVersion: 1,
            availability: 'available',
            installed: 'yes',
            checkedAt: '2026-07-16T08:00:00.000Z',
            reasonCode: null,
            detectorVersion: 'reviewer-status-v2',
            source: 'claude-plugin',
          }))
        } else {
          child.stdout.end(JSON.stringify({
            schemaVersion: 1,
            availability: 'available',
            installed: 'no',
            checkedAt: '2026-07-16T08:00:00.000Z',
            reasonCode: null,
            detectorVersion: 'reviewer-status-v2',
            source: 'none',
          }))
        }
        child.stderr.end()
        child.emit('close', 0)
      })
      return child
    }
    try {
      delete require.cache[detectorPath]
      const wrapper = require(detectorPath)
      const malformed = await wrapper.get(false)
      assert.equal(malformed.availability, 'unknown')
      assert.equal(malformed.reasonCode, 'codex-check-malformed')
      assert.equal(spawnCount, 1)
      assert.strictEqual(await wrapper.get(false), malformed)
      assert.equal(spawnCount, 1)
      const first = wrapper.get(true)
      const second = wrapper.get(true)
      assert.strictEqual(first, second)
      const refreshed = await first
      assert.equal(refreshed.availability, 'available')
      assert.equal(spawnCount, 2)
      const inconsistent = await wrapper.get(true)
      assert.equal(inconsistent.availability, 'unknown')
      assert.equal(inconsistent.reasonCode, 'codex-check-malformed')
      assert.equal(spawnCount, 3)
      for (const environment of spawnEnvironments) {
        for (const key of ['NODE_OPTIONS', 'NODE_PATH', 'LD_PRELOAD']) {
          assert.equal(Object.hasOwn(environment, key), false, key)
        }
      }
    } finally {
      childProcess.spawn = originalSpawn
      if (originalNodeOptions === undefined) delete process.env.NODE_OPTIONS
      else process.env.NODE_OPTIONS = originalNodeOptions
      if (originalNodePath === undefined) delete process.env.NODE_PATH
      else process.env.NODE_PATH = originalNodePath
      if (originalLdPreload === undefined) delete process.env.LD_PRELOAD
      else process.env.LD_PRELOAD = originalLdPreload
      delete require.cache[detectorPath]
    }
  })

  const stems = {
    overlap: 'TASK_1_overlapping_attempts',
    codex: 'TASK_2_codex_success',
    internal: 'TASK_3_auto_internal',
    blocked: 'TASK_4_require_blocked',
    retry: 'TASK_5_retry_success',
    orphan: 'TASK_6_orphan_review',
    conflictCodex: 'TASK_7_conflict_codex',
    conflictInternal: 'TASK_8_conflict_internal',
    corrupt: 'TASK_9_corrupt_journal',
    done: 'TASK_10_done_outcome',
    waiting: 'TASK_11_waiting_review',
    doneRecovered: 'TASK_12_done_closes_failure',
    missingOutcome: 'TASK_13_missing_outcome',
    semanticConflict: 'TASK_14_semantic_conflict',
  }
  const doneKeys = new Set(['done', 'doneRecovered', 'missingOutcome'])
  const sourceFingerprint = 'sha256:' + '1'.repeat(64)
  const indexRow = (stem, title, state, doneAt = null) => ({
    stem, title, state, createdAt: '2026-07-15T00:00:00.000Z', doneAt,
    sourceRevision: sourceFingerprint,
    origin: { kind: 'manual', type: 'manual', ref: `fixture:${stem}`, fingerprint: sourceFingerprint },
    dependsOn: [], splitFrom: null,
    outcomeStatus: state === 'done' ? 'completed' : null, questionsCount: null, round: null,
  })
  const todo = Object.entries(stems).filter(([key]) => !doneKeys.has(key)).map(([key, stem]) =>
    indexRow(stem, key.replaceAll(/([A-Z])/g, ' $1').trim(), 'todo'))
  writeFileSync(join(tasksDir, 'INDEX.json'), JSON.stringify({
    version: 2,
    generatedAt: '2026-07-16T12:30:00.000Z',
    backlog: [],
    pending: [],
    todo,
    done: [
      indexRow(stems.done, 'Done outcome', 'done', '2026-07-15T10:00:00.000Z'),
      indexRow(stems.doneRecovered, 'Done closes failure', 'done', '2026-07-16T10:30:00.000Z'),
      indexRow(stems.missingOutcome, 'Missing outcome', 'done', '2026-07-15T07:00:00.000Z'),
    ],
  }, null, 2) + '\n')

  journal(stems.overlap, [
    event(stems.overlap, '2026-07-16T08:00:00Z', 'phase-start', 'info', {
      reviewer: 'codex', reviewAttempt: '1', selectionReason: 'codex-available',
    }),
    event(stems.overlap, '2026-07-16T08:01:00Z', 'phase-start', 'info', {
      reviewer: 'internal-reviewer', reviewAttempt: '2', selectionReason: 'forced-internal',
    }),
    event(stems.overlap, '2026-07-16T08:03:00Z', 'phase-end', 'ok', {
      reviewer: 'codex', reviewAttempt: '1',
    }),
  ])
  journal(stems.codex, [
    event(stems.codex, '2026-07-16T09:00:00Z', 'phase-start', 'info', {
      reviewer: 'codex', reviewAttempt: '1', selectionReason: 'codex-available',
    }),
    event(stems.codex, '2026-07-16T09:04:00Z', 'phase-end', 'ok', {
      reviewer: 'codex', reviewAttempt: '1',
    }),
  ])
  journal(stems.internal, [
    event(stems.internal, '2026-07-16T12:00:00Z', 'phase-start', 'info', {
      reviewer: 'internal-reviewer',
      reviewAttempt: '1',
      selectionReason: 'codex-unavailable',
      reasonCode: 'fallback-used',
    }),
    event(stems.internal, '2026-07-16T12:02:00Z', 'phase-end', 'ok', {
      reviewer: 'internal-reviewer', reviewAttempt: '1',
    }),
  ])
  journal(stems.blocked, [
    event(stems.blocked, '2026-07-16T10:00:00Z', 'phase-start', 'info', {
      reviewer: 'codex', reviewAttempt: '1', selectionReason: 'forced-codex',
    }),
    event(stems.blocked, '2026-07-16T10:00:01Z', 'stop', 'blocked', {
      reviewer: 'codex', reviewAttempt: '1', reasonCode: 'require-codex-blocked',
    }),
  ])
  journal(stems.retry, [
    event(stems.retry, '2026-07-16T10:10:00Z', 'phase-start', 'info', {
      reviewer: 'internal-reviewer', reviewAttempt: '1', selectionReason: 'forced-internal',
    }),
    event(stems.retry, '2026-07-16T10:11:00Z', 'phase-end', 'fail', {
      reviewer: 'internal-reviewer', reviewAttempt: '1', reasonCode: 'review-failed',
    }),
    event(stems.retry, '2026-07-16T10:12:00Z', 'phase-start', 'info', {
      reviewer: 'internal-reviewer', reviewAttempt: '2', selectionReason: 'forced-internal',
    }),
    event(stems.retry, '2026-07-16T10:13:00Z', 'phase-end', 'ok', {
      reviewer: 'internal-reviewer', reviewAttempt: '2',
    }),
  ])
  journal(stems.orphan, [
    event(stems.orphan, '2026-07-16T11:00:00Z', 'phase-start', 'info', {
      reviewer: 'codex', reviewAttempt: '1', selectionReason: 'codex-available',
    }),
  ])
  journal(stems.conflictCodex, [
    event(stems.conflictCodex, '2026-07-16T11:10:00Z', 'phase-start', 'info', {
      reviewer: 'codex', reviewAttempt: '1', selectionReason: 'codex-available',
    }),
  ])
  journal(stems.conflictInternal, [
    event(stems.conflictInternal, '2026-07-16T11:11:00Z', 'phase-start', 'info', {
      reviewer: 'internal-reviewer', reviewAttempt: '1', selectionReason: 'codex-unavailable',
    }),
  ])
  writeFileSync(join(journalDir, stems.corrupt + '.jsonl'), '{"privatePrompt":"do not leak"}\n', { mode: 0o600 })
  journal(stems.waiting, [
    event(stems.waiting, '2026-07-16T11:30:00Z', 'gate', 'info'),
  ])
  journal(stems.doneRecovered, [
    event(stems.doneRecovered, '2026-07-16T10:20:00Z', 'phase-start', 'info', {
      reviewer: 'codex', reviewAttempt: '1', selectionReason: 'codex-available',
    }),
    event(stems.doneRecovered, '2026-07-16T10:21:00Z', 'phase-end', 'fail', {
      reviewer: 'codex', reviewAttempt: '1', reasonCode: 'review-failed',
    }),
  ])
  journal(stems.semanticConflict, [
    event(stems.semanticConflict, '2026-07-16T07:00:00Z', 'phase-start', 'info', {
      reviewer: 'codex', reviewAttempt: '1', selectionReason: 'codex-available',
    }),
    event(stems.semanticConflict, '2026-07-16T07:00:00Z', 'phase-start', 'info', {
      reviewer: 'internal-reviewer', reviewAttempt: '1', selectionReason: 'forced-internal',
    }),
    event(stems.semanticConflict, '2026-07-16T07:01:00Z', 'phase-end', 'ok', {
      reviewer: 'internal-reviewer', reviewAttempt: '1',
    }),
    event(stems.semanticConflict, '2026-07-16T07:01:00Z', 'phase-end', 'ok', {
      reviewer: 'codex', reviewAttempt: '1',
    }),
  ])
  writeFileSync(join(tasksDir, 'done', stems.done + '.md'), [
    '# TASK 10 — Done outcome',
    '',
    '---',
    '',
    '## Outcome',
    '',
    '**Status**: completed',
    '**Completed at**: 2026-07-15T10:00:00Z',
    '**Reviewer**: codex',
    '**Review iterations**: 1',
    '',
    '### Build gates',
    '- none',
    '',
    '### Runtime verify',
    '- Gate: skipped (fixture)',
    '- Result: n/a — fixture',
    '',
    '### Acceptance trace',
    '- `fixture` — verified — complete',
    '',
    '### Caveats',
    '- none',
    '',
    '### Follow-ups',
    '- none',
    '',
    '### Files touched',
    '- none',
    '',
  ].join('\n'))
  writeFileSync(join(tasksDir, 'done', stems.doneRecovered + '.md'), [
    '# TASK 12 — Done closes failure',
    '',
    '---',
    '',
    '## Outcome',
    '',
    '**Status**: completed',
    '**Completed at**: 2026-07-16T10:30:00Z',
    '**Reviewer**: codex',
    '**Review iterations**: 2',
    '',
    '### Build gates',
    '- none',
    '',
    '### Runtime verify',
    '- Gate: skipped (fixture)',
    '- Result: n/a — fixture',
    '',
    '### Acceptance trace',
    '- `fixture` — verified — complete',
    '',
    '### Caveats',
    '- none',
    '',
    '### Follow-ups',
    '- none',
    '',
    '### Files touched',
    '- none',
    '',
  ].join('\n'))

  const activity = require('../server/reviewer-activity.js')
  const reviewer = require('../server/reviewer.js')

  await checkAsync('activity rejects non-canonical INDEX timestamps instead of widening the board contract', async () => {
    const indexFile = join(tasksDir, 'INDEX.json')
    const canonical = readFileSync(indexFile, 'utf8')
    const invalid = JSON.parse(canonical)
    invalid.generatedAt = '2026-07-16T12:30:00+00:00'
    writeFileSync(indexFile, JSON.stringify(invalid, null, 2) + '\n')
    const rejected = activity.snapshot()
    assert.equal(rejected.partial, true)
    assert.equal(rejected.pending.length, 0)
    assert.equal(rejected.history.length, 0)
    writeFileSync(indexFile, canonical)
    await new Promise((resolve) => setTimeout(resolve, 800))
    assert.equal(activity.snapshot().pending.length, 6)
  })

  check('activity projection handles overlap, success, fallback, block, retry, orphan, outcome, and corrupt journals', () => {
    const snapshot = activity.snapshot()
    assert.equal(snapshot.partial, true)
    assert.equal(snapshot.reasonCode, 'journal-partial')
    assert.equal(snapshot.pending.length, 6)
    assert.equal(snapshot.failed.length, 1)
    assert.equal(snapshot.failed[0].taskStem, stems.blocked)
    assert.equal(snapshot.failed.some((row) => row.taskStem === stems.retry), false)
    assert.equal(snapshot.active.find((row) => row.taskStem === stems.overlap).reviewer, 'internal-reviewer')
    assert.equal(snapshot.active.find((row) => row.taskStem === stems.overlap).startedAt, '2026-07-16T08:01:00Z')
    assert.equal(snapshot.history.some((row) => row.taskStem === stems.overlap &&
      row.reviewAttempt === 1 && row.status === 'passed'), true)
    assert.equal(snapshot.active.find((row) => row.taskStem === stems.orphan).reviewer, 'codex')
    assert.equal(snapshot.pending.find((row) => row.taskStem === stems.waiting).waitingToStart, true)
    assert.equal(snapshot.completed.some((row) => row.taskStem === stems.done && row.fromOutcome), true)
    assert.equal(snapshot.completed.some((row) => row.taskStem === stems.doneRecovered && row.fromOutcome), true)
    assert.equal(snapshot.failed.some((row) => row.taskStem === stems.doneRecovered), false)
    assert.equal(snapshot.lastReview.taskStem, stems.internal)
    assert.equal(snapshot.lastReview.selectionReason, 'codex-unavailable')
    assert.equal(JSON.stringify(snapshot).includes('privatePrompt'), false)
  })

  check('activity pagination is bounded, opaque, revision-bound, and deduplicates all-state blocked rows', () => {
    const page = activity.list('all', null, 2)
    assert.equal(page.ok, true)
    assert.equal(page.rows.length, 2)
    assert.equal(new Set(page.rows.map((row) => `${row.taskStem}:${row.reviewAttempt}`)).size, 2)
    assert.ok(page.nextCursor)
    const next = activity.list('all', page.nextCursor, 2)
    assert.equal(next.ok, true)
    const full = activity.list('all', null, 100)
    const retryHistory = full.rows.filter((row) => row.taskStem === stems.retry)
    assert.deepEqual(retryHistory.map((row) => [row.reviewAttempt, row.status]), [
      [2, 'passed'],
      [1, 'failed'],
    ])
    assert.equal(full.rows.some((row) => row.taskStem === stems.codex && row.status === 'passed'), true)
    assert.equal(full.rows.some((row) => row.taskStem === stems.done && row.fromOutcome), true)
    assert.equal(full.rows.some((row) => row.taskStem === stems.doneRecovered && row.status === 'failed'), true)
    assert.equal(full.rows.some((row) => row.taskStem === stems.doneRecovered && row.fromOutcome), true)
    assert.equal(full.rows.filter((row) => row.taskStem === stems.blocked).length, 1)
    assert.deepEqual(
      full.rows.filter((row) => row.taskStem === stems.semanticConflict)
        .map((row) => row.reviewer).sort(),
      ['codex', 'internal-reviewer'],
    )
    assert.equal(activity.list('all', 'not-a-cursor', 2).error, 'stale-activity-cursor')
    assert.equal(activity.list('pending', null, 101).error, 'bad-limit')
    assert.equal(JSON.stringify(page).includes('privatePrompt'), false)
  })

  await checkAsync('Done Outcome integrity participates in partial state and cache revision', async () => {
    const before = activity.snapshot()
    const doneFile = join(tasksDir, 'done', stems.done + '.md')
    writeFileSync(doneFile, readFileSync(doneFile, 'utf8').replace(
      '**Reviewer**: codex',
      '**Reviewer**: internal-reviewer',
    ))
    await new Promise((resolve) => setTimeout(resolve, 800))
    const changed = activity.snapshot()
    assert.notEqual(changed.revision, before.revision)
    assert.equal(changed.completed.find((row) => row.taskStem === stems.done).reviewer, 'internal-reviewer')

    journal(stems.corrupt, [])
    await new Promise((resolve) => setTimeout(resolve, 800))
    assert.equal(activity.snapshot().partial, true)

    writeFileSync(join(tasksDir, 'done', stems.missingOutcome + '.md'), [
      '# TASK 13 — Missing outcome repaired',
      '',
      '---',
      '',
      '## Outcome',
      '',
      '**Status**: completed',
      '**Completed at**: 2026-07-15T07:00:00Z',
      '**Reviewer**: internal-reviewer',
      '**Review iterations**: 1',
      '',
      '### Build gates',
      '- none',
      '',
      '### Runtime verify',
      '- Gate: skipped (fixture)',
      '- Result: n/a — fixture',
      '',
      '### Acceptance trace',
      '- `fixture` — verified — complete',
      '',
      '### Caveats',
      '- none',
      '',
      '### Follow-ups',
      '- none',
      '',
      '### Files touched',
      '- none',
      '',
    ].join('\n'))
    journal(stems.semanticConflict, [
      event(stems.semanticConflict, '2026-07-16T07:00:00Z', 'phase-start', 'info', {
        reviewer: 'codex', reviewAttempt: '1', selectionReason: 'codex-available',
      }),
      event(stems.semanticConflict, '2026-07-16T07:01:00Z', 'phase-end', 'ok', {
        reviewer: 'codex', reviewAttempt: '1',
      }),
    ])
    await new Promise((resolve) => setTimeout(resolve, 800))
    assert.equal(activity.snapshot().partial, false)

    const outcomeSibling = join(tasksDir, 'done', stems.missingOutcome + '.hardlink.md')
    linkSync(join(tasksDir, 'done', stems.missingOutcome + '.md'), outcomeSibling)
    await new Promise((resolve) => setTimeout(resolve, 800))
    assert.equal(activity.snapshot().partial, true)
    unlinkSync(outcomeSibling)
    await new Promise((resolve) => setTimeout(resolve, 800))
    assert.equal(activity.snapshot().partial, false)
  })

  await checkAsync('aggregate reports mixed active reviewers and maps all three user modes canonically', async () => {
    let status = await reviewer.status(false)
    assert.equal(status.schemaVersion, 1)
    assert.equal(status.config.mode, 'automatic')
    assert.equal(status.review.activeReviewer, 'mixed')
    assert.equal(status.review.activeReviewerBasis, 'conflicting-active-events')
    assert.equal(status.review.integrityWarning, 'conflicting-active-events')
    assert.equal(status.lastReview.selectionFallbackUsed, true)
    assert.equal(status.counts.pending, 6)
    assert.equal(status.counts.failed, 1)
    assert.equal(JSON.stringify(status).includes('/opt/homebrew'), false)

    journal(stems.conflictInternal, [
      event(stems.conflictInternal, '2026-07-16T11:11:00Z', 'phase-start', 'info', {
        reviewer: 'internal-reviewer', reviewAttempt: '1', selectionReason: 'codex-unavailable',
      }),
      event(stems.conflictInternal, '2026-07-16T11:12:00Z', 'phase-end', 'ok', {
        reviewer: 'internal-reviewer', reviewAttempt: '1',
      }),
    ])
    await new Promise((resolve) => setTimeout(resolve, 800))
    status = await reviewer.status(false)
    assert.equal(status.review.activeReviewer, 'mixed')
    assert.equal(status.review.integrityWarning, 'conflicting-active-events')

    journal(stems.overlap, [
      event(stems.overlap, '2026-07-16T08:00:00Z', 'phase-start', 'info', {
        reviewer: 'codex', reviewAttempt: '1', selectionReason: 'codex-available',
      }),
      event(stems.overlap, '2026-07-16T08:01:00Z', 'phase-start', 'info', {
        reviewer: 'internal-reviewer', reviewAttempt: '2', selectionReason: 'forced-internal',
      }),
      event(stems.overlap, '2026-07-16T08:03:00Z', 'phase-end', 'ok', {
        reviewer: 'codex', reviewAttempt: '1',
      }),
      event(stems.overlap, '2026-07-16T08:04:00Z', 'phase-end', 'ok', {
        reviewer: 'internal-reviewer', reviewAttempt: '2',
      }),
    ])
    await new Promise((resolve) => setTimeout(resolve, 800))
    status = await reviewer.status(false)
    assert.equal(status.review.activeReviewer, 'codex')
    assert.equal(status.review.activeReviewerBasis, 'active-review')
    assert.equal(status.review.fallbackPolicy, 'none')

    for (const [mode, raw] of [
      ['require-codex', 'true'],
      ['internal-only', 'false'],
      ['automatic', 'auto'],
    ]) {
      const current = configUpdate.read()
      const result = await reviewer.settings({
        mode,
        expectedRevision: current.revision,
        idempotencyKey: `reviewer-contract:${mode}`,
      })
      assert.equal(result.ok, true)
      assert.equal(result.reviewer.config.mode, mode)
      assert.match(readFileSync(configFile, 'utf8'),
        new RegExp(`^codexEnabled: ${raw} # canonical Reviewer backing field$`, 'm'))
    }

    const current = configUpdate.read()
    const idempotentRequest = {
      mode: 'internal-only',
      expectedRevision: current.revision,
      idempotencyKey: 'reviewer-contract:idempotent',
    }
    const [first, replay] = await Promise.all([
      reviewer.settings(idempotentRequest),
      reviewer.settings(idempotentRequest),
    ])
    assert.equal(first.ok, true)
    assert.equal(replay.ok, true)
    assert.equal(replay.reviewer.config.revision, first.reviewer.config.revision)
    const conflict = await reviewer.settings({
      mode: 'automatic',
      expectedRevision: first.reviewer.config.revision,
      idempotencyKey: 'reviewer-contract:idempotent',
    })
    assert.equal(conflict.error, 'idempotency-conflict')

    const originalUpdate = configUpdate.update
    try {
      configUpdate.update = function () {
        return { ok: false, status: 503, error: 'writer-lease-release-failed' }
      }
      const busy = await reviewer.settings({
        mode: 'automatic',
        expectedRevision: first.reviewer.config.revision,
        idempotencyKey: 'reviewer-contract:lease-failed',
      })
      assert.deepEqual([busy.status, busy.error], [503, 'project-busy'])

      configUpdate.update = function () {
        return { ok: false, status: 500, error: '/private/path/write exploded' }
      }
      const redacted = await reviewer.settings({
        mode: 'automatic',
        expectedRevision: first.reviewer.config.revision,
        idempotencyKey: 'reviewer-contract:redacted-failure',
      })
      assert.deepEqual([redacted.status, redacted.error], [500, 'reviewer-settings-failed'])
      assert.equal(JSON.stringify(redacted).includes('/private/path'), false)
    } finally {
      configUpdate.update = originalUpdate
    }
  })

  console.log(`reviewer-contracts: ${checks} checks passed`)
} finally {
  rmSync(root, { recursive: true, force: true })
}
