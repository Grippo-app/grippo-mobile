#!/usr/bin/env node
// Exact-schema pins for the Phase 1 worktree/integration record contracts
// (pipeline improvement 01): field sets, grammars, hash binding, bounds, and
// the WAL phase lattice. These records gate every later mutation phase, so
// the contracts must reject everything they do not exactly expect.

import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const require = createRequire(import.meta.url)
const worktree = require('../worktree-record-contract.cjs')
const integration = require('../integration-record-contract.cjs')

let checks = 0
function check(name, fn) { fn(); checks++; console.log(`ok ${checks} - ${name}`) }

function invalidUtf8For(value) {
  const bytes = Buffer.from(JSON.stringify(value), 'utf8')
  const replacement = Buffer.from('\uFFFD', 'utf8')
  const offset = bytes.indexOf(replacement)
  assert.notEqual(offset, -1, 'fixture must contain one replacement character')
  return Buffer.concat([bytes.subarray(0, offset), Buffer.from([0xff]),
    bytes.subarray(offset + replacement.length)])
}

const H = 'sha256:' + 'a'.repeat(64)
const AT = '2026-08-09T10:00:00.000Z'
const LATER = '2026-08-09T10:00:01.000Z'

function validWorktreeRecord(patch = {}) {
  const record = {
    version: 1, worktreeId: 'wt-' + 'ab'.repeat(16), runId: '1700000000000-r1',
    requestId: '1700000000000-q1', stem: 'TASK_42_sample_task', status: 'ready',
    controlProjectId: H,
    gitCommonDirIdentity: { path: '/repo/.git', dev: '1', ino: '2' },
    controlRoot: { path: '/repo', dev: '1', ino: '3' },
    executionRoot: { path: '/Users/x/.orchestrator-worktrees/h/wt-1', dev: '1', ino: '4' },
    targetRef: 'refs/heads/skill-first-migration',
    candidateRef: 'refs/heads/orchestrator/task/TASK_42-' + 'ab'.repeat(6) + '/r1short',
    baseCommit: 'c'.repeat(40), baseTree: 'd'.repeat(40),
    taskState: 'todo', taskSourceRevision: H, taskSnapshotHash: H, projectConfigHash: H,
    dependencySnapshotHash: H, figmaGenerationHash: null, apiGenerationHash: null,
    capabilities: [],
    owner: { hostname: 'mac', pid: 123, processStartId: null, startedAt: AT },
    createdAt: AT, updatedAt: LATER, recordHash: H,
    ...patch
  }
  record.recordHash = worktree.recordHash(record)
  return record
}
function refuseWorktree(patch, expectation, mutate) {
  const record = validWorktreeRecord(patch)
  if (mutate) mutate(record)
  assert.throws(() => worktree.validate(record),
    (error) => error.code === 'WORKTREE_RECORD_INVALID' && expectation.test(error.message),
    expectation.source)
}

check('valid worktree record round-trips through validate and validateBytes', () => {
  const record = validWorktreeRecord()
  worktree.validate(record)
  worktree.validateBytes(Buffer.from(JSON.stringify(record), 'utf8'))
})

check('worktree record rejects field drift, version drift and tampered hash', () => {
  refuseWorktree({}, /exact v1 schema/, (record) => { record.extra = true })
  refuseWorktree({}, /exact v1 schema/, (record) => { delete record.baseTree })
  refuseWorktree({ version: 2 }, /version must be 1/)
  refuseWorktree({}, /recordHash does not match/, (record) => { record.baseCommit = 'e'.repeat(40) })
  refuseWorktree({}, /recordHash does not match/, (record) => {
    // Even a valid replacement generation must trip the record hash.
    record.capabilities = record.capabilities.slice()
    record.dependencySnapshotHash = 'sha256:' + 'b'.repeat(64)
  })
  refuseWorktree({ dependencySnapshotHash: null }, /dependencySnapshotHash must be an exact sha256 hash/)
})

check('worktree record identity grammar: ids, stem, commits, timestamps', () => {
  refuseWorktree({ worktreeId: 'wt-XYZ' }, /worktreeId/)
  refuseWorktree({ runId: 'run one' }, /runId/)
  refuseWorktree({ requestId: '17-' }, /requestId/)
  refuseWorktree({ stem: 'TASK_0_zero' }, /stem/)
  refuseWorktree({ stem: 'TASK_7_профиль' }, /stem/)
  refuseWorktree({ baseCommit: 'c'.repeat(39) }, /baseCommit/)
  refuseWorktree({ baseTree: 'D'.repeat(40) }, /baseTree/)
  refuseWorktree({ createdAt: '2026-08-09T10:00:00Z' }, /createdAt/)
  refuseWorktree({ createdAt: LATER, updatedAt: AT }, /updatedAt must not precede/)
  refuseWorktree({ taskState: 'backlog' }, /run tasks in todo/)
  refuseWorktree({ status: 'improvised' }, /lifecycle state/)
})

check('filesystem identities accept spaces and Unicode but refuse control bytes and non-NFC', () => {
  const cyrillic = validWorktreeRecord({
    executionRoot: { path: '/Users/пользователь/.orchestrator-worktrees/каталог с пробелами', dev: '9', ino: '9' }
  })
  worktree.validate(cyrillic)
  refuseWorktree({ executionRoot: { path: 'relative/path', dev: '1', ino: '4' } }, /absolute canonical path/)
  refuseWorktree({ executionRoot: { path: '/ab', dev: '1', ino: '4' } }, /absolute canonical path/)
  refuseWorktree({ executionRoot: { path: '/café', dev: '1', ino: '4' } }, /absolute canonical path/) // NFD
  refuseWorktree({ executionRoot: { path: '/trailing/', dev: '1', ino: '4' } }, /must not end with a slash/)
  refuseWorktree({ executionRoot: { path: '/x', dev: '1', ino: '4', extra: '1' } }, /exactly path\/dev\/ino/)
  refuseWorktree({ executionRoot: { path: '/x', dev: '01', ino: '4' } }, /unsigned decimal/)
})

check('execution root nullability follows the lifecycle status', () => {
  const intent = validWorktreeRecord({ status: 'create-intent', executionRoot: null })
  worktree.validate(intent)
  refuseWorktree({ status: 'ready', executionRoot: null }, /must be a filesystem identity/)
  refuseWorktree({}, /never alias the control root/, (record) => {
    record.executionRoot = { path: record.controlRoot.path, dev: '7', ino: '8' }
    record.recordHash = worktree.recordHash(record)
  })
})

check('target ref grammar mirrors git rules and excludes the manager namespace', () => {
  for (const ok of ['refs/heads/main', 'refs/heads/skill-first-migration',
    'refs/heads/feature/nested', 'refs/heads/ветка']) {
    assert.equal(worktree.targetRefValid(ok), true, ok)
  }
  for (const bad of ['refs/heads/orchestrator/task/TASK_1-abc/x', 'refs/heads/a..b',
    'refs/heads/a@{b}', 'refs/heads/a//b', 'refs/heads/a b', 'refs/heads/a~b',
    'refs/heads/-lead', 'refs/heads/tail/', 'refs/heads/tail.lock', 'refs/heads/.hidden',
    'refs/heads/seg./x', 'refs/tags/v1', 'main', 'refs/heads/' + 'x'.repeat(230)]) {
    assert.equal(worktree.targetRefValid(bad), false, bad)
  }
  refuseWorktree({ targetRef: 'refs/heads/orchestrator/task/TASK_1-abcdefabcdef/x' }, /manager namespace/, (record) => {
    record.recordHash = worktree.recordHash(record)
  })
})

check('candidate ref must be namespaced and carry the record task number', () => {
  refuseWorktree({ candidateRef: 'refs/heads/orchestrator/task/TASK_41-' + 'ab'.repeat(6) + '/r1' }, /task number/)
  refuseWorktree({ candidateRef: 'refs/heads/feature/x' }, /manager-generated/)
  refuseWorktree({ candidateRef: 'refs/heads/orchestrator/task/TASK_42-UPPER/r1' }, /manager-generated/)
})

check('v1 capabilities allowlist is empty and owner identity is exact', () => {
  refuseWorktree({ capabilities: ['task-lock'] }, /empty v1 allowlist/)
  refuseWorktree({ owner: { hostname: '', pid: 1, processStartId: null, startedAt: AT } }, /hostname/)
  refuseWorktree({ owner: { hostname: 'mac', pid: 0, processStartId: null, startedAt: AT } }, /pid/)
  refuseWorktree({ owner: { hostname: 'mac', pid: 1, processStartId: 'bogus', startedAt: AT } }, /processStartId/)
})

check('worktree record byte gate: oversized and malformed bytes fail closed', () => {
  assert.throws(() => worktree.validateBytes(Buffer.from('{broken', 'utf8')), /not valid JSON/)
  assert.throws(() => worktree.validateBytes(Buffer.alloc(0)), /bounded size/)
  assert.throws(() => worktree.validateBytes(Buffer.alloc(worktree.MAX_BYTES + 1, 0x20)), /bounded size/)
  assert.throws(() => worktree.validateBytes('not-a-buffer'), /must be a Buffer/)
  const invalidUtf8 = validWorktreeRecord()
  invalidUtf8.owner.hostname = 'fixture-\uFFFD'
  invalidUtf8.recordHash = worktree.recordHash(invalidUtf8)
  assert.throws(() => worktree.validateBytes(invalidUtf8For(invalidUtf8)), /UTF-8/)
})

function validIntegrationRecord(patch = {}) {
  const marks = {}
  for (const phase of integration.PHASES) marks[phase] = { intentAt: null, provenAt: null }
  marks.prepared = { intentAt: AT, provenAt: LATER }
  const record = {
    version: 1, integrationId: 'ig-' + 'ab'.repeat(16), stem: 'TASK_42_sample_task',
    runId: '1700000000000-r1', worktreeId: 'wt-' + 'ab'.repeat(16),
    phase: 'prepared', status: 'active',
    candidate: { commit: 'c'.repeat(40), tree: 'd'.repeat(40), diffHash: H, receiptHash: H },
    target: { ref: 'refs/heads/skill-first-migration', baseCommit: 'e'.repeat(40), baseTree: 'f'.repeat(40) },
    controlSnapshot: {
      headCommit: 'e'.repeat(40),
      dirtyAllowedPaths: [{ path: 'orchestrator/tasks/todo/TASK_42_sample_task.md', hash: H }]
    },
    commitPin: { stagedTreeHash: null, messageHash: null, expectedParent: null, publishedCommit: null },
    finalizerPrepared: null, phases: marks,
    owner: { hostname: 'mac', pid: 5, processStartId: null, startedAt: AT },
    createdAt: AT, updatedAt: LATER, recordHash: H,
    ...patch
  }
  record.recordHash = integration.recordHash(record)
  return record
}
function refuseIntegration(expectation, mutate) {
  const record = validIntegrationRecord()
  mutate(record)
  record.recordHash = integration.recordHash(record)
  assert.throws(() => integration.validate(record),
    (error) => error.code === 'INTEGRATION_RECORD_INVALID' && expectation.test(error.message),
    expectation.source)
}

check('valid integration record round-trips and pins candidate base to target HEAD', () => {
  integration.validate(validIntegrationRecord())
  refuseIntegration(/candidate base = current target HEAD/, (record) => {
    record.controlSnapshot.headCommit = '9'.repeat(40)
  })
  refuseIntegration(/one canonical commit on the exact base/, (record) => {
    record.commitPin.expectedParent = '9'.repeat(40)
  })
})

check('WAL phase lattice: intent-before-effect, strict order, honest furthest phase', () => {
  refuseIntegration(/previous phase to be proven/, (record) => {
    record.phases['product-applied'] = { intentAt: LATER, provenAt: null }
    record.phase = 'product-applied'
  })
  refuseIntegration(/proof requires a recorded intent/, (record) => {
    record.phases['product-applying'] = { intentAt: null, provenAt: LATER }
  })
  refuseIntegration(/furthest phase with a recorded intent/, (record) => {
    record.phases['product-applying'] = { intentAt: LATER, provenAt: null }
    // phase left at 'prepared' while a later intent exists
  })
  refuseIntegration(/at least the prepared intent/, (record) => {
    record.phases.prepared = { intentAt: null, provenAt: null }
    record.phase = 'prepared'
  })
  refuseIntegration(/completed phase to be proven/, (record) => {
    record.status = 'completed'
  })
  refuseIntegration(/requires the commit-publishing intent/, (record) => {
    record.commitPin.publishedCommit = 'a'.repeat(40)
  })
  refuseIntegration(/requires the prepared path pins/, (record) => {
    for (const phase of ['product-applying', 'product-applied', 'finalizer-preparing', 'finalizer-prepared']) {
      record.phases[phase] = { intentAt: LATER, provenAt: LATER }
    }
    record.phase = 'finalizer-prepared'
    record.finalizerPrepared = null
  })
})

check('integration path pins: repo-relative only, deduplicated, bounded', () => {
  refuseIntegration(/repo-relative path/, (record) => {
    record.controlSnapshot.dirtyAllowedPaths = [{ path: '/absolute', hash: H }]
  })
  refuseIntegration(/dot-dot components/, (record) => {
    record.controlSnapshot.dirtyAllowedPaths = [{ path: 'a/../b', hash: H }]
  })
  refuseIntegration(/must not repeat a path/, (record) => {
    record.controlSnapshot.dirtyAllowedPaths = [
      { path: 'same/file.md', hash: H }, { path: 'same/file.md', hash: H }
    ]
  })
  refuseIntegration(/bounded entry count/, (record) => {
    record.controlSnapshot.dirtyAllowedPaths = Array.from({ length: 201 }, (_, index) => (
      { path: 'p/' + index, hash: H }
    ))
  })
  const unicode = validIntegrationRecord()
  unicode.controlSnapshot.dirtyAllowedPaths = [{ path: 'каталог с пробелами/файл.md', hash: H }]
  unicode.recordHash = integration.recordHash(unicode)
  integration.validate(unicode)
})

check('WAL commit pins are coupled to their phases in BOTH directions', () => {
  // §10.3 phase 6: the intent IS the exact staged tree/message/parent pins.
  refuseIntegration(/exact stagedTreeHash\/messageHash\/expectedParent pins/, (record) => {
    for (const phase of ['product-applying', 'product-applied', 'finalizer-preparing', 'finalizer-prepared']) {
      record.phases[phase] = { intentAt: LATER, provenAt: LATER }
    }
    record.finalizerPrepared = [{ path: 'orchestrator/tasks/done/TASK_42_sample_task.md', hash: H }]
    record.phases['commit-publishing'] = { intentAt: LATER, provenAt: null }
    record.phase = 'commit-publishing'
  })
  // §10.3 phase 7: a proven publication must name the commit it proved.
  refuseIntegration(/commit-published proof requires the published commit pin/, (record) => {
    for (const phase of ['product-applying', 'product-applied', 'finalizer-preparing', 'finalizer-prepared', 'commit-publishing', 'commit-published']) {
      record.phases[phase] = { intentAt: LATER, provenAt: LATER }
    }
    record.finalizerPrepared = [{ path: 'orchestrator/tasks/done/TASK_42_sample_task.md', hash: H }]
    record.commitPin = { stagedTreeHash: H, messageHash: H, expectedParent: record.target.baseCommit, publishedCommit: null }
    record.phase = 'commit-published'
  })
  // Within one phase, proof can never precede intent.
  refuseIntegration(/proof must not precede its intent/, (record) => {
    record.phases.prepared = { intentAt: LATER, provenAt: AT }
  })
  // A proven completed phase with an 'active' status is a contradiction.
  refuseIntegration(/incompatible with active status/, (record) => {
    for (const phase of integration.PHASES) record.phases[phase] = { intentAt: AT, provenAt: LATER }
    record.finalizerPrepared = [{ path: 'orchestrator/tasks/done/TASK_42_sample_task.md', hash: H }]
    record.commitPin = { stagedTreeHash: H, messageHash: H, expectedParent: record.target.baseCommit, publishedCommit: 'a'.repeat(40) }
    record.phase = 'completed'
    record.status = 'active'
  })
})

check('integration record version drift and tampered hash fail closed', () => {
  const drifted = validIntegrationRecord()
  drifted.version = 2
  drifted.recordHash = integration.recordHash(drifted)
  assert.throws(() => integration.validate(drifted), /version must be 1/)
  const tampered = validIntegrationRecord()
  tampered.candidate.diffHash = 'sha256:' + 'f'.repeat(64)
  assert.throws(() => integration.validate(tampered), /recordHash does not match/)
  assert.ok(integration.INTEGRATION_ID_RE.test('ig-' + 'ab'.repeat(16)))
  assert.equal(integration.INTEGRATION_ID_RE.test('wt-' + 'ab'.repeat(16)), false)
})

check('the reserved branch name HEAD is refused like git check-ref-format does', () => {
  assert.equal(worktree.targetRefValid('refs/heads/HEAD'), false)
  assert.equal(worktree.targetRefValid('refs/heads/nested/HEAD'), true, 'only the exact reserved name is special')
})

check('integration record byte gate and grammar', () => {
  assert.throws(() => integration.validateBytes(Buffer.from('[]', 'utf8')), /exact v1 schema/)
  assert.throws(() => integration.validateBytes(Buffer.alloc(integration.MAX_BYTES + 1, 0x20)), /bounded size/)
  const invalidUtf8 = validIntegrationRecord()
  invalidUtf8.owner.hostname = 'fixture-\uFFFD'
  invalidUtf8.recordHash = integration.recordHash(invalidUtf8)
  assert.throws(() => integration.validateBytes(invalidUtf8For(invalidUtf8)), /UTF-8/)
  refuseIntegration(/integrationId/, (record) => { record.integrationId = 'wt-' + 'ab'.repeat(16) })
  refuseIntegration(/target\.ref/, (record) => { record.target.ref = 'refs/heads/orchestrator/task/TASK_1-abcdefabcdef/x' })
})

check('the published JSON schemas describe exactly the contracts they publish', () => {
  // §19.1: these two record types are durable, cross-process contracts, so
  // their closed field sets and enums are published in the repository's own
  // schema style — and kept honest by this test rather than by hope. A schema
  // that drifts from its .cjs contract is worse than no schema: it documents a
  // shape the code does not enforce.
  const here = dirname(fileURLToPath(import.meta.url))
  const read = (name) => JSON.parse(readFileSync(join(here, '..', name), 'utf8'))
  const pairs = [
    [read('worktree-record.schema.json'), worktree, 'worktree'],
    [read('integration-record.schema.json'), integration, 'integration'],
  ]
  for (const [schema, contract, label] of pairs) {
    assert.equal(schema.additionalProperties, false, label + ' schema must be closed')
    assert.equal(schema.properties.version.const, 1)
    assert.deepEqual(schema.required, [...contract.FIELDS].sort(),
      label + ' schema required set must equal the contract field set')
    assert.deepEqual(schema.properties.status.enum, [...contract.STATUSES].sort(),
      label + ' schema status enum must equal the contract status set')
  }
  assert.deepEqual(pairs[1][0].properties.phase.enum, [...integration.PHASES].sort(),
    'the WAL phase lattice is part of the published contract')
})

check('published contract surfaces are exact: versions, field sets, statuses, canonical form', () => {
  assert.equal(worktree.VERSION, 1)
  assert.equal(integration.VERSION, 1)
  assert.equal(worktree.FIELDS.length, 26)
  assert.equal(integration.FIELDS.length, 17)
  // No 'integrating' and no 'completed': the integration WAL carries the
  // transaction's own status, and a generation is either materialized or
  // released. Neither was ever produced, so the filters reading them were
  // unreachable code.
  assert.deepEqual([...worktree.STATUSES].sort(), ['create-intent',
    'ready', 'ready-for-integration', 'recovery-required', 'released', 'releasing', 'revalidation-required', 'sealing'])
  assert.deepEqual([...worktree.MATERIALIZED_STATUSES].sort(),
    ['ready', 'ready-for-integration', 'revalidation-required', 'sealing'])
  assert.deepEqual([...worktree.CHECKOUT_OWNING_STATUSES].sort(),
    ['ready', 'ready-for-integration', 'recovery-required', 'releasing', 'revalidation-required', 'sealing'])
  assert.deepEqual([...worktree.RELEASABLE_STATUSES].sort(),
    ['ready', 'ready-for-integration', 'recovery-required', 'releasing', 'revalidation-required'])
  // 'abandoned' is the operator's terminal decision on a recovery-required
  // record: it releases the repository-wide mutex without repairing, rolling
  // back or deleting anything. It is never reached automatically.
  assert.deepEqual([...integration.STATUSES].sort(),
    ['abandoned', 'active', 'completed', 'recovery-required'])
  // canonical() is key-order independent and refuses undefined/non-finite.
  assert.equal(worktree.canonical({ b: 1, a: [2, 'x'] }), worktree.canonical({ a: [2, 'x'], b: 1 }))
  assert.throws(() => worktree.canonical({ a: undefined }), /undefined/)
  assert.throws(() => worktree.canonical(Infinity), /non-finite/)
})

check('the post-implementation amendment inventory cannot carry a stale cardinality', () => {
  const here = dirname(fileURLToPath(import.meta.url))
  const plan = readFileSync(join(here, '..', '..', '..', 'PIPELINE_IMPROVEMENT_01_PER_TASK_WORKTREE.md'), 'utf8')
  const roadmap = readFileSync(join(here, '..', '..', '..', 'WORKTREE_REMEDIATION_ROADMAP.md'), 'utf8')
  const amendment = plan.slice(plan.indexOf('## 28. Поправки после реализации'))
  assert.notEqual(amendment.length, plan.length, 'the §28 amendment section must exist')
  const planInventory = [...amendment.matchAll(/^### 28\.(\d+) (.+)$/gm)]
    .map((match) => [Number(match[1]), match[2]])
  assert.deepEqual(planInventory.map(([number]) => number), [1, 2, 3, 4, 5, 6, 7],
    'the amendment inventory must remain explicit and sequential')
  assert.doesNotMatch(amendment, /Общий принцип всех (?:шести|семи) расхождений/,
    'the amendment prose must not duplicate its mechanically visible cardinality')
  const roadmapInventory = [...roadmap.matchAll(/^### 10\.(\d+) (.+)$/gm)]
    .map((match) => [Number(match[1]), match[2]])
  assert.deepEqual(roadmapInventory, planInventory,
    'roadmap wave 10 must project every pending §28 amendment with the same title')
  assert.doesNotMatch(roadmap, /^(?:Шесть|Семь) мест,/m,
    'roadmap prose must not keep a second hand-maintained amendment count')
})

console.log(`worktree contracts: ${checks} checks passed`)
