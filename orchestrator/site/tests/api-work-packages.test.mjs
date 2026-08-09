import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { createRequire } from 'node:module'
import { spawnSync } from 'node:child_process'
import { createHash } from 'node:crypto'
import { fileURLToPath } from 'node:url'

const REPO = fileURLToPath(new URL('../../../', import.meta.url))
const require = createRequire(import.meta.url)
const apiWorkPackage = require(join(
  REPO, 'orchestrator', 'tasks', 'api-work-package-contract.cjs',
))
const apiTasks = require(join(
  REPO, 'orchestrator', 'site', 'server', 'api-task-actions.js',
))
const taskSource = require(join(
  REPO, 'orchestrator', 'tasks', 'task-source-contract.cjs',
))

const HASH_A = 'sha256:' + 'a'.repeat(64)
const HASH_B = 'sha256:' + 'b'.repeat(64)
const CHANGE = (character) => 'api:change:chg-' + character.repeat(24)
const MISSING = (character) => 'api:missing:missing-' + character.repeat(24)
const MISMATCH = (character) => 'api:mismatch:mismatch-' + character.repeat(24)

function sourceRow(index, overrides = {}) {
  const sourceId = overrides.sourceId || CHANGE(
    (index % 10).toString(16),
  )
  return {
    sourceId,
    type: overrides.type || (
      sourceId.startsWith('api:missing:') ? 'api-missing'
        : sourceId.startsWith('api:mismatch:') ? 'api-mismatch'
          : 'api-change'
    ),
    operationId: overrides.operationId || 'operation' + index,
    title: overrides.title || 'Finding ' + index,
    summary: overrides.summary || 'Finding summary ' + index,
    evidence: overrides.evidence || {},
    areas: overrides.areas || ['widgets'],
    operationIds: overrides.operationIds || ['operation' + index],
    modelIds: overrides.modelIds || ['Widget' + index],
    fingerprint: overrides.fingerprint ||
      'sha256:' + (index % 10).toString(16).repeat(64),
    tasks: overrides.tasks || { open: [], resolved: [] },
  }
}

function snapshot(rows) {
  return {
    committedGenerationId: 'gen-20260101T000000Z-abcdef123456',
    contractHash: HASH_A,
    environmentId: 'local',
    projectCodeRevision: HASH_B,
    reportHashes: {
      changes: HASH_A,
      consumers: HASH_A,
      drift: HASH_A,
      implementation: HASH_A,
    },
    changes: {
      current: true,
      value: { changeSetId: 'changes-' + 'c'.repeat(24) },
    },
    _apiTaskRows: {
      rows,
      bySourceId: Object.fromEntries(rows.map((row) => [row.sourceId, row])),
    },
  }
}

test('canonical API work-package metadata is deterministic and tamper-evident', () => {
  const sources = [MISSING('b'), CHANGE('a')]
  const metadata = apiWorkPackage.create('area:widgets', sources)
  assert.deepEqual(metadata.sourceIds, sources.slice().sort())
  assert.match(metadata.packageId, /^pkg-[a-f0-9]{24}$/)
  assert.deepEqual(
    apiWorkPackage.create('area:widgets', sources.slice().reverse()),
    metadata,
  )

  const rendered = apiWorkPackage.render(metadata)
  const parsed = apiWorkPackage.parse(
    '# TASK 1 — Package\n\n' + rendered + '\n\n## Goal\n\nDeliver.\n',
  )
  assert.equal(parsed.valid, true)
  assert.deepEqual(parsed.value, metadata)

  const tampered = rendered.replace(CHANGE('a'), CHANGE('c'))
  assert.equal(apiWorkPackage.parse(tampered).valid, false)
  assert.equal(apiWorkPackage.parse(rendered + '\n\n' + rendered).valid, false)
  assert.throws(
    () => apiWorkPackage.create('area:widgets', [CHANGE('a')]),
    (error) => error && error.code === 'api-work-package-invalid',
  )
  assert.throws(
    () => apiWorkPackage.create('area:widgets', [
      CHANGE('a'), CHANGE('a'),
    ]),
    (error) => error && error.code === 'api-work-package-invalid',
  )
})

test('planner keeps the exact selection in one package for one API group', () => {
  const rows = Array.from({ length: 40 }, (_, index) => sourceRow(index, {
    sourceId: 'api:change:chg-' + index.toString(16).padStart(24, '0'),
    areas: index < 39 ? ['widgets'] : ['billing'],
  }))
  const snap = snapshot(rows)
  const selected = rows.slice(0, 38)
  const planned = apiTasks._test.plan(snap, selected, 'package')
  assert.deepEqual(planned.scopeRows, selected)
  assert.deepEqual(planned.actions.map((action) => action.sourceIds.length), [38])
  assert.equal(planned.actions.every((action) =>
    action.group.key === 'area:widgets'), true)
  assert.equal(planned.actions.flatMap((action) => action.sourceIds)
    .includes(rows[38].sourceId), false)
  assert.equal(planned.actions.flatMap((action) => action.sourceIds)
    .includes(rows[39].sourceId), false)
  assert.equal(planned.actions.every((action) =>
    apiWorkPackage.normalize(action.metadata)), true)
})

test('task preview accepts large explicit selections and still validates every source', () => {
  const sourceIds = Array.from({ length: 300 }, (_, index) =>
    'api:change:chg-' + index.toString(16).padStart(24, '0'))
  const request = {
    expectedGenerationId: 'gen-20260101T000000Z-abcdef123456',
    expectedReportHashes: {
      changes: HASH_A,
      consumers: HASH_A,
      drift: HASH_A,
      implementation: HASH_A,
    },
    expectedTaskIndexRevision: HASH_B,
    mode: 'package',
    sourceIds,
  }
  assert.equal(apiTasks._test.validPreviewRequest(request), true)
  assert.equal(apiTasks._test.validPreviewRequest({
    ...request,
    mode: 'hotfix',
  }), false)
  assert.equal(apiTasks._test.validPreviewRequest({
    ...request,
    sourceIds: sourceIds.concat(sourceIds[0]),
  }), false)
  const maximum = Array.from({ length: apiTasks.MAX_SELECTED_ITEMS },
    (_, index) => 'api:change:chg-' + index.toString(16).padStart(24, '0'))
  assert.equal(apiWorkPackage.MAX_SOURCES, apiTasks.MAX_SELECTED_ITEMS)
  assert.equal(apiTasks._test.validPreviewRequest({
    ...request,
    sourceIds: maximum,
  }), true)
  assert.equal(apiTasks._test.validPreviewRequest({
    ...request,
    sourceIds: maximum.concat(
      'api:change:chg-' + maximum.length.toString(16).padStart(24, '0'),
    ),
  }), false)
})

test('planner preserves open work coverage and blocks accidental singleton packages', () => {
  const openTask = {
    stem: 'TASK_9_existing',
    title: 'Existing API task',
    column: 'todo',
  }
  const rows = [
    sourceRow(1, {
      sourceId: CHANGE('1'),
      tasks: { open: [openTask], resolved: [] },
    }),
    sourceRow(2, { sourceId: MISSING('2') }),
  ]
  const snap = snapshot(rows)
  const planned = apiTasks._test.plan(snap, rows, 'package')
  assert.equal(planned.existing.length, 1)
  assert.equal(planned.actions.length, 0)
  assert.equal(planned.blocked.length, 1)
  assert.equal(planned.blocked[0].sourceId, rows[1].sourceId)
  assert.equal(planned.blocked[0].reason, 'api-package-requires-hotfix')

  const hotfix = apiTasks._test.plan(snap, [rows[1]], 'hotfix')
  assert.equal(hotfix.actions.length, 1)
  assert.equal(hotfix.actions[0].mode, 'hotfix')
  assert.deepEqual(hotfix.actions[0].sourceIds, [rows[1].sourceId])
  assert.match(
    apiTasks._test.hotfixBody(hotfix.actions[0].rows[0], snap),
    /explicit one-source exception/,
  )
})

test('explicit groups stay separate and oversized selections fail closed', () => {
  const shared = [
    sourceRow(1, {
      sourceId: CHANGE('1'),
      areas: ['billing', 'widgets'],
      modelIds: ['SharedModel'],
    }),
    sourceRow(2, {
      sourceId: MISMATCH('2'),
      areas: ['billing', 'widgets'],
      modelIds: ['SharedModel'],
    }),
  ]
  const sharedPlan = apiTasks._test.plan(
    snapshot(shared), shared, 'package',
  )
  assert.equal(sharedPlan.actions.length, 1)
  assert.match(sharedPlan.actions[0].group.key, /^model:/)
  assert.equal(sharedPlan.actions[0].sourceIds.length, 2)

  const grouped = [
    sourceRow(3, { sourceId: CHANGE('3'), areas: ['widgets'] }),
    sourceRow(4, { sourceId: CHANGE('4'), areas: ['widgets'] }),
    sourceRow(5, { sourceId: CHANGE('5'), areas: ['billing'] }),
    sourceRow(6, { sourceId: CHANGE('6'), areas: ['billing'] }),
  ]
  const groupedPlan = apiTasks._test.plan(snapshot(grouped), grouped, 'package')
  assert.deepEqual(
    groupedPlan.actions.map((action) => [action.group.key, action.sourceIds.length]),
    [['area:billing', 2], ['area:widgets', 2]],
  )

  const large = Array.from({ length: apiTasks.MAX_SELECTED_ITEMS + 1 },
    (_, index) => sourceRow(index, {
      sourceId: 'api:change:chg-' + index.toString(16).padStart(24, '0'),
      areas: ['large-area'],
    }))
  const largePlan = apiTasks._test.plan(
    snapshot(large), large, 'package',
  )
  assert.equal(largePlan.error, 'api-package-scope-too-large')
  assert.equal(largePlan.scopeSize, apiTasks.MAX_SELECTED_ITEMS + 1)

  const verbose = Array.from({ length: 80 }, (_, index) => sourceRow(index, {
    sourceId: 'api:change:chg-' + index.toString(16).padStart(24, '0'),
    areas: ['verbose-area'],
    summary: 'x'.repeat(1000),
  }))
  const verbosePlan = apiTasks._test.plan(
    snapshot(verbose), verbose, 'package',
  )
  assert.equal(verbosePlan.error, 'api-package-scope-too-large')
  assert.equal(verbosePlan.scopeSize, verbose.length)
})

test('package body carries exact aliases and task scans restore every relation after restart', () => {
  const rows = [
    sourceRow(1, { sourceId: CHANGE('1') }),
    sourceRow(2, { sourceId: MISSING('2') }),
    sourceRow(3, { sourceId: MISMATCH('3') }),
  ]
  const snap = snapshot(rows)
  const action = apiTasks._test.plan(snap, rows, 'package').actions[0]
  const body = apiTasks._test.packageBody(action, snap)
  const metadata = apiWorkPackage.parse(body)
  assert.equal(metadata.valid, true)
  assert.deepEqual(metadata.value.sourceIds, action.sourceIds)

  const root = mkdtempSync(join(tmpdir(), 'api-package-index-'))
  try {
    const tasksDir = join(root, 'tasks')
    const backlogDir = join(tasksDir, 'backlog')
    mkdirSync(backlogDir, { recursive: true })
    const source = {
      kind: 'api',
      type: 'api-work-package',
      ref: 'api:package:' + action.metadata.packageId,
      fingerprint: action.fingerprint,
    }
    const markdown = '# TASK 1 — Package\n\n' +
      taskSource.render(source) + '\n\n' + body + '\n'
    const stem = 'TASK_1_api_package'
    writeFileSync(join(backlogDir, stem + '.md'), markdown)
    const sourceRevision = 'sha256:' + createHash('sha256')
      .update(markdown).digest('hex')
    writeFileSync(join(tasksDir, 'INDEX.json'), JSON.stringify({
      version: 2,
      generatedAt: '2026-01-01T00:00:00Z',
      backlog: [{
        stem,
        title: 'Package',
        state: 'backlog',
        sourceRevision,
        createdAt: '2026-01-01T00:00:00Z',
        splitFrom: null,
        round: null,
        questionsCount: null,
        doneAt: null,
        outcomeStatus: null,
        dependsOn: [],
        origin: source,
      }],
      pending: [],
      todo: [],
      done: [],
    }))

    const script = `
      const taskSource = require(${JSON.stringify(join(
        REPO, 'orchestrator', 'site', 'server', 'task-source.js',
      ))});
      const result = taskSource.scanOpen();
      process.stdout.write(JSON.stringify({
        ok: result.ok,
        aliases: ${JSON.stringify(action.sourceIds)}.map((id) =>
          (result.byRef[id] || []).map((row) => row.stem)),
        package: (result.byRef[${JSON.stringify(source.ref)}] || [])
          .map((row) => row.stem)
      }));
    `
    const result = spawnSync(process.execPath, ['-e', script], {
      env: {
        ...process.env,
        ORCHESTRATOR_TASKS_DIR: tasksDir,
      },
      encoding: 'utf8',
    })
    assert.equal(result.status, 0, result.stderr)
    const projection = JSON.parse(result.stdout)
    assert.equal(projection.ok, true)
    assert.deepEqual(projection.aliases, action.sourceIds.map(() => [stem]))
    assert.deepEqual(projection.package, [stem])

    const stored = readFileSync(join(backlogDir, stem + '.md'), 'utf8')
    assert.match(stored, /## API Work Package/)
  } finally {
    rmSync(root, { recursive: true, force: true })
  }
})
