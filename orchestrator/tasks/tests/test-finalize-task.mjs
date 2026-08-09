#!/usr/bin/env node

import assert from 'node:assert/strict'
import {
  existsSync, lstatSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync, symlinkSync, unlinkSync, writeFileSync,
} from 'node:fs'
import { spawn, spawnSync } from 'node:child_process'
import { dirname, join, resolve } from 'node:path'
import { hostname, tmpdir } from 'node:os'
import { fileURLToPath } from 'node:url'
import { createRequire } from 'node:module'
import { createHash } from 'node:crypto'
import { normalizeCapture as normalizeComponentCapture } from '../../figma/components/capture-normalizer.mjs'
import { normalizeSourceCapture } from '../../figma/tokens/source-normalizer.mjs'
import { aggregateObservedTokens } from '../../figma/tokens/catalog-aggregator.mjs'
import { contextKey } from '../../figma/tokens/source-contract.mjs'
import { immutablePlan, validObservedCapture } from '../../figma/tests/observed-token-fixtures.mjs'
import { loadAdapterConfig } from '../../figma/runtime/adapter-config.mjs'
import { extractProjectComponents } from '../../figma/runtime/component-extraction.mjs'
import { canonicalHash } from '../../figma/runtime/canonical-json.mjs'
import { materializeSourceHealth } from '../../figma/tokens/source-health.mjs'
import writerLeases from '../writer-leases.cjs'

const require = createRequire(import.meta.url)
const taskState = require('../task-state-core.cjs')
const taskSource = require('../task-source-contract.cjs')
const tokenBindingContract = require('../token-binding-contract.cjs')
const componentBindingContract = require('../component-binding-contract.cjs')
const testInputContract = require('../task-test-input-contract.cjs')
const testReceiptContract = require('../task-test-receipt-contract.cjs')
const testSummaryContract = require('../task-test-summary-contract.cjs')
const testImpactContract = require('../task-test-impact-contract.cjs')
const testPolicyContract = require('../task-test-policy-contract.cjs')
const contentSnapshot = require('../content-snapshot.cjs')

const ROOT = resolve(fileURLToPath(new URL('../../..', import.meta.url)))
const FINALIZER = join(ROOT, 'orchestrator', 'tasks', 'finalize-task.mjs')
const phases = ['outcome', 'components', 'tokens', 'ship', 'index', 'arch', 'verify', 'unlock', 'cleanup']
let checks = 0
const TEST_FILTERS = String(process.env.FINALIZE_TEST_FILTER || '').split(',').map((value) => value.trim()).filter(Boolean)

const outcomeDraft = `## Outcome

**Status**: completed
**Completed at**: 2026-01-01T00:00:00Z
**Reviewer**: codex
**Review iterations**: 1

### Build gates

- \`tests\` — skipped (test-not-applicable: documentation-only)

### Runtime verify

- Gate: skipped (no runtime-observable change)
- Result: n/a — no runtime change

### Acceptance trace

- \`render states — light — dark\` — verified — Exact variants passed.

### Caveats

- none

### Follow-ups

- none

### Files touched

- none
`

const fakeShip = `
import { linkSync, readFileSync, renameSync, unlinkSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
const root = process.env.FINALIZE_PROJECT_ROOT;
const stem = process.argv[2];
const todo = join(root, 'orchestrator/tasks/todo', stem + '.md');
const done = join(root, 'orchestrator/tasks/done', stem + '.md');
if (process.env.FAKE_SHIP_PROOF === '1') {
  const proof = join(root, 'orchestrator/tasks/todo', '.finalize-' + stem + '-' + process.env.FINALIZE_TRANSACTION_ID + '.ship');
  writeFileSync(proof, readFileSync(todo));
  if (process.env.FAKE_SHIP_CRASH_AFTER_PROOF === '1') process.exit(97);
  linkSync(proof, done);
  unlinkSync(todo);
} else renameSync(todo, done);
`

const fakeIndex = `
import { readFileSync, writeFileSync } from 'node:fs';
import { createRequire } from 'node:module';
const require = createRequire(import.meta.url);
const taskState = require(process.env.TASK_STATE_CORE);
const root = process.env.FINALIZE_PROJECT_ROOT;
const base = root + '/orchestrator/tasks';
const verdict = taskState.validateTaskState({ tasksDir: base, repoRoot: root, checkIndex: false, includeRuntime: false });
if (!verdict.ok) { console.error(JSON.stringify(verdict.findings)); process.exit(1); }
const out = taskState.deriveIndex(verdict._model, '2026-01-01T00:00:00Z');
const indexPath = base + '/INDEX.json';
if (process.argv.includes('--check')) {
  let current = null; try { current = JSON.parse(readFileSync(indexPath, 'utf8')); } catch {}
  if (!current) process.exit(1);
  const a = { ...current, generatedAt: '' }, b = { ...out, generatedAt: '' };
  process.exit(JSON.stringify(a) === JSON.stringify(b) ? 0 : 1);
}
writeFileSync(indexPath, JSON.stringify(out, null, 2) + '\\n');
`

const fakeArch = `
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
const args = process.argv.slice(2);
if (!args.includes('--check')) {
  const marker = JSON.parse(readFileSync(
    process.env.FINALIZE_STATE_DIR + '/' + process.env.FAKE_EXPECTED_ARCH_STEM + '.json',
    'utf8',
  ));
  const expected = [
    '--trigger', 'task-finalization',
    '--trigger-id', marker.transactionId,
    '--task-stem', process.env.FAKE_EXPECTED_ARCH_STEM,
  ];
  if (JSON.stringify(args) !== JSON.stringify(expected)) {
    console.error('unexpected architecture publication identity: ' + JSON.stringify(args));
    process.exit(96);
  }
}
if (process.env.FAKE_ARCH_DELAY_MS) await new Promise((resolve) => setTimeout(resolve, Number(process.env.FAKE_ARCH_DELAY_MS)));
if (process.env.FAKE_ARCH_GATE) {
  writeFileSync(process.env.FAKE_ARCH_GATE + '.ready', 'ready\\n');
  const deadline = Date.now() + 130000;
  while (!existsSync(process.env.FAKE_ARCH_GATE)) {
    if (Date.now() >= deadline) process.exit(98);
    await new Promise((resolve) => setTimeout(resolve, 25));
  }
}
process.exit(process.env.FAKE_ARCH_FAIL === '1' ? 1 : 0);
`
const fakeVerify = `process.exit(process.env.FAKE_VERIFY_FAIL === '1' ? 2 : 0);\n`

function mkdirs(root) {
  for (const rel of [
    'orchestrator/tasks/backlog', 'orchestrator/tasks/pending', 'orchestrator/tasks/todo',
    'orchestrator/tasks/done', 'orchestrator/.cache/tasks/locks',
    'orchestrator/.cache/tasks/finalizations', 'orchestrator/figma',
  ]) mkdirSync(join(root, rel), { recursive: true })
}

function canonicalTodo(raw) {
  let text = String(raw || '').trimEnd() + '\n'
  if (!taskSource.parse(text).present) {
    const heading = /^(# TASK ([1-9][0-9]*) — [^\n]+)\n/.exec(text)
    if (!heading) throw new Error('finalize fixture task heading is invalid')
    const ref = `finalize-fixture-${heading[2]}`
    const source = taskSource.render(taskSource.manualForIntent(ref, 'manual', ref))
    text = heading[1] + '\n\n' + source + '\n\n' + text.slice(heading[0].length).replace(/^\n+/, '')
  }
  if (!/^## Goal\s*$/m.test(text)) text += '\n## Goal\n- Finish the fixture through `finalize-task.mjs`.\n'
  if (!/^## Inputs\s*$/m.test(text)) text += '\n## Inputs\n- Fixture source at `orchestrator/tasks/todo`.\n'
  if (!/^## Acceptance\s*$/m.test(text)) text += '\n## Acceptance\n\n### Automated\n- `finalize-task.mjs` publishes one canonical done artifact.\n'
  if (!/^## Out of scope\s*$/m.test(text)) text += '\n## Out of scope\n- No unrelated fixture files change.\n'
  return text
}

function canonicalLock(stem, sessionId = 'ws-fixture-session-000000000000', stage = 'orchestrator') {
  const startedAt = '2026-01-01T00:00:00.000Z'
  return {
    version: 1,
    stem,
    stage,
    runId: 'run-fixture-00000001',
    sessionId,
    startedAt,
    owner: {
      kind: 'direct',
      id: `fixture:${stem}`,
      pid: process.pid,
      processStartId: writerLeases.captureProcessStartId(process.pid),
      hostname: hostname(),
      startedAt,
    },
  }
}

function writeCanonicalLock(root, stem, sessionId, stage = 'orchestrator') {
  writeFileSync(
    join(root, 'orchestrator/.cache/tasks/locks', stem + '.json'),
    JSON.stringify(canonicalLock(stem, sessionId, stage), null, 2) + '\n',
  )
}
function fixtureHash(value) {
  return `sha256:${createHash('sha256').update(String(value)).digest('hex')}`
}
function writeJson(file, value) { writeFileSync(file, JSON.stringify(value, null, 2) + '\n') }
function writeTestEvidence(root, stem, { forgePass = false } = {}) {
  const lock = JSON.parse(readFileSync(
    join(root, 'orchestrator/.cache/tasks/locks', stem + '.json'), 'utf8'))
  const runRoot = join(root, 'orchestrator/.cache/tasks/test-certification', stem, lock.runId)
  const structuralDir = join(runRoot, 'structural')
  mkdirSync(structuralDir, { recursive: true })
  const policy = testPolicyContract.loadPolicy()
  const taskInputHash = testInputContract.taskInputHashOf(
    readFileSync(join(root, 'orchestrator/tasks/todo', stem + '.md')))
  // taskInputHash binds the task separately; an empty generic content closure
  // keeps the fixture focused on finalizer graph/lock semantics.
  const source = contentSnapshot.captureSnapshot({ root, paths: [] })
  const impactBase = {
    version: 1,
    policyVersion: policy.version,
    policyHash: policy.policyHash,
    taskStem: stem,
    runId: lock.runId,
    taskInputHash,
    sourceSnapshotHash: source.snapshotHash,
    capabilityInventoryHash: fixtureHash('fixture-capabilities'),
    moduleGraphHash: fixtureHash('fixture-module-graph'),
    behaviors: [],
    affectedModules: [],
    affectedConsumers: [],
    requiredSuites: [],
    fullSuiteRequired: false,
    requiredCapabilities: [],
    testNotApplicable: 'documentation-only',
    notApplicableValidators: [...policy.notApplicableValidators],
    unknownDependencies: [],
    selectionReasons: ['planner-proposed'],
    impactHash: fixtureHash('placeholder'),
  }
  const plannedCandidate = { ...impactBase, phase: 'planned' }
  plannedCandidate.impactHash = testImpactContract.impactHashOf(plannedCandidate)
  const planned = testImpactContract.validateImpact(plannedCandidate, { policy })
  const observedCandidate = { ...impactBase, phase: 'observed' }
  observedCandidate.impactHash = testImpactContract.impactHashOf(observedCandidate)
  const observed = testImpactContract.validateImpact(observedCandidate, { policy })
  const structuralReceiptHashes = []
  for (let index = 0; index < policy.notApplicableValidators.length; index++) {
    const gateId = policy.notApplicableValidators[index]
    const ordinal = String(index).padStart(3, '0')
    const common = {
      version: 1,
      kind: 'test-structural-gate',
      taskStem: stem,
      runId: lock.runId,
      sessionId: lock.sessionId,
      lockStage: lock.stage,
      taskInputHash,
      sourceSnapshotHash: source.snapshotHash,
      policyHash: policy.policyHash,
      gateId,
      executionMode: 'in-process',
      tool: 'fixture structural validator',
      validatorCodeHash: fixtureHash('validator:' + gateId),
      pid: null,
      processGroup: null,
      startedAt: '2026-01-01T00:00:00.000Z',
      signal: null,
      timedOut: false,
      artifacts: [],
      outputDigest: { bytes: 0, hash: fixtureHash(''), redacted: true },
    }
    const startedCandidate = {
      ...common, stage: 'started', startedReceiptHash: null,
      endedAt: null, durationMs: null, exitCode: null, result: 'pending',
      receiptHash: fixtureHash('placeholder'),
    }
    startedCandidate.receiptHash = testReceiptContract.receiptHashOf(startedCandidate)
    const started = testReceiptContract.validateStructuralReceipt(startedCandidate)
    const terminalCandidate = {
      ...common, stage: 'terminal', startedReceiptHash: started.receiptHash,
      endedAt: '2026-01-01T00:00:01.000Z', durationMs: 1000,
      exitCode: null, result: 'passed', receiptHash: fixtureHash('placeholder'),
    }
    terminalCandidate.receiptHash = testReceiptContract.receiptHashOf(terminalCandidate)
    const terminal = testReceiptContract.validateStructuralReceipt(terminalCandidate)
    writeJson(join(structuralDir, `${ordinal}-started-${started.receiptHash.slice(7)}.json`), started)
    writeJson(join(structuralDir, `${ordinal}-terminal-${terminal.receiptHash.slice(7)}.json`), terminal)
    structuralReceiptHashes.push(terminal.receiptHash)
  }
  const summaryCandidate = {
    version: 1,
    taskStem: stem,
    runId: lock.runId,
    sessionId: lock.sessionId,
    taskInputHash,
    sourceSnapshotHash: source.snapshotHash,
    policyVersion: policy.version,
    policyHash: policy.policyHash,
    plannedImpactHash: planned.impactHash,
    observedImpactHash: observed.impactHash,
    requiredLanes: [], executedLanes: [], anchorEvidence: [],
    requiredSuites: [], passedSuites: [],
    fullSuiteRequired: false, fullSuiteResult: null,
    failBeforePassAfter: [], zeroTestVerdicts: [], flakyVerdicts: [], coverage: null,
    snapshotVerification: 'current',
    commandReceiptHashes: forgePass ? [fixtureHash('missing-command')] : [],
    structuralReceiptHashes: forgePass ? [] : structuralReceiptHashes.sort(),
    verdict: forgePass ? 'PASS' : 'SKIPPED',
    verdictReasons: [forgePass ? 'all-required-test-evidence-proven' : 'test-not-applicable-documentation-only'],
    summaryHash: fixtureHash('placeholder'),
  }
  summaryCandidate.summaryHash = testSummaryContract.summaryHashOf(summaryCandidate)
  const summary = testSummaryContract.validateSummary(summaryCandidate)
  writeJson(join(runRoot, 'policy.json'), policy)
  writeJson(join(runRoot, 'source-snapshot.json'), source)
  writeJson(join(runRoot, 'planned-impact.json'), planned)
  writeJson(join(runRoot, 'observed-impact.json'), observed)
  writeJson(join(runRoot, 'summary.json'), summary)
  return summary
}
function completedCreationReceipt(filenameHash, transactionId) {
  const hash = `sha256:${'1'.repeat(64)}`
  return {
    version: 2, transactionId, keyHash: `sha256:${filenameHash}`, payloadHash: hash,
    intent: null, status: 'completed', phase: 'completed', effect: 'domain-dedup',
    number: 7, slug: null, stem: 'TASK_7_existing', sourceHash: hash, column: 'backlog',
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:01Z',
    revision: 2, lastError: null, targetProof: null,
  }
}

function fixture(opts = {}) {
  const root = mkdtempSync(join(tmpdir(), 'finalize-task-'))
  mkdirs(root)
  const stem = opts.stem || 'TASK_7_atomic_finish'
  writeFileSync(join(root, 'orchestrator/project-config.md'), `---\nfigmaEnabled: ${opts.figma ? 'true' : 'false'}\n---\n`)
  writeFileSync(join(root, 'orchestrator/tasks/todo', stem + '.md'), canonicalTodo(opts.task || `# TASK 7 — Atomic finish\n\n## Goal\n- finish safely through \`finalize-task.mjs\`.\n`))
  writeCanonicalLock(root, stem, opts.lockSessionId || 'ws-fixture-session-000000000000', opts.lockStage || 'orchestrator')
  writeTestEvidence(root, stem, { forgePass: opts.forgePass === true })
  const draft = join(root, 'outcome.md')
  writeFileSync(draft, outcomeDraft)
  const scriptsDir = join(root, 'scripts'); mkdirSync(scriptsDir)
  const files = {
    ship: join(scriptsDir, 'ship.mjs'), index: join(scriptsDir, 'index.mjs'),
    arch: join(scriptsDir, 'arch.mjs'), verify: join(scriptsDir, 'verify.mjs'),
  }
  writeFileSync(files.ship, fakeShip); writeFileSync(files.index, fakeIndex)
  writeFileSync(files.arch, fakeArch); writeFileSync(files.verify, fakeVerify)
  const initial = taskState.validateTaskState({ tasksDir: join(root, 'orchestrator/tasks'), repoRoot: root, checkIndex: false, includeRuntime: false })
  assert.equal(initial.ok, true, JSON.stringify(initial.findings))
  writeFileSync(join(root, 'orchestrator/tasks/INDEX.json'), JSON.stringify(taskState.deriveIndex(initial._model, '2026-01-01T00:00:00Z'), null, 2) + '\n')
  const env = {
    ...process.env,
    FINALIZE_PROJECT_ROOT: root,
    FINALIZE_STATE_DIR: join(root, 'orchestrator/.cache/tasks/finalizations'),
    FINALIZE_LOCKS_DIR: join(root, 'orchestrator/.cache/tasks/locks'),
    FINALIZE_SHIP_SCRIPT: files.ship,
    FINALIZE_INDEX_SCRIPT: files.index,
    FINALIZE_ARCH_SCRIPT: files.arch,
    FINALIZE_VERIFY_DONE_SCRIPT: files.verify,
    FINALIZE_PYTHON: process.execPath,
    // A complete finalization deliberately crosses the anchored fs boundary
    // many times.  The concurrent convergence check therefore needs a mutex
    // budget for one full serialized run, not the short polling budget used by
    // waitUntil() below.  Production defaults to 15 minutes; 120 seconds keeps
    // this fixture bounded while avoiding a false FINALIZATION_BUSY result on
    // slower CI hosts.
    FINALIZE_MUTEX_WAIT_MS: '120000',
    TASK_STATE_CORE: join(ROOT, 'orchestrator/tasks/task-state-core.cjs'),
    FAKE_EXPECTED_ARCH_STEM: stem,
  }
  return { root, stem, draft, env }
}

function run(fx, args, extraEnv = {}) {
  return spawnSync(process.execPath, [FINALIZER, ...args, ...(fx.fixtureArgs || [])], {
    cwd: fx.root, env: { ...fx.env, ...extraEnv }, encoding: 'utf8', timeout: 120000,
  })
}
function taskStateEvents(stderr) {
  return String(stderr || '').split(/\r?\n/)
    .filter((line) => line.startsWith('[task-state] '))
    .map((line) => JSON.parse(line.slice('[task-state] '.length)))
}
function runAsync(fx, args, extraEnv = {}) {
  const child = spawn(process.execPath, [FINALIZER, ...args, ...(fx.fixtureArgs || [])], {
    cwd: fx.root, env: { ...fx.env, ...extraEnv }, stdio: ['ignore', 'pipe', 'pipe'],
  })
  let stdout = '', stderr = ''
  child.stdout.on('data', (chunk) => { stdout += String(chunk) })
  child.stderr.on('data', (chunk) => { stderr += String(chunk) })
  const done = new Promise((resolveDone) => child.on('close', (status, signal) => resolveDone({ status, signal, stdout, stderr })))
  return { child, done }
}
async function waitUntil(predicate, message, timeoutMs = 5000) {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    try { if (predicate()) return } catch {}
    await new Promise((resolveWait) => setTimeout(resolveWait, 20))
  }
  throw new Error(`timed out waiting for ${message}`)
}
function marker(fx) {
  return JSON.parse(readFileSync(join(fx.root, 'orchestrator/.cache/tasks/finalizations', fx.stem + '.json'), 'utf8'))
}
function assertComplete(fx) {
  assert.ok(existsSync(join(fx.root, 'orchestrator/tasks/done', fx.stem + '.md')))
  assert.ok(!existsSync(join(fx.root, 'orchestrator/tasks/todo', fx.stem + '.md')))
  assert.ok(!existsSync(join(fx.root, 'orchestrator/.cache/tasks/locks', fx.stem + '.json')))
  assert.ok(!existsSync(join(fx.root, 'orchestrator/.cache/tasks/finalizations', fx.stem + '.json')))
  assert.equal(readFileNames(join(fx.root, 'orchestrator/.cache/tasks/finalizations')).filter((name) => name.startsWith(fx.stem + '.') && name.endsWith('.outcome.md')).length, 0)
  const index = JSON.parse(readFileSync(join(fx.root, 'orchestrator/tasks/INDEX.json'), 'utf8'))
  assert.equal(index.done.filter((x) => x.stem === fx.stem).length, 1)
  assert.equal(index.todo.filter((x) => x.stem === fx.stem).length, 0)
}
function readFileNames(dir) { try { return readdirSync(dir) } catch { return [] } }
function check(name, fn) {
  if (TEST_FILTERS.length && !TEST_FILTERS.some((filter) => name.includes(filter))) return Promise.resolve()
  return Promise.resolve().then(fn).then(function () {
    checks++
    console.log('PASS ' + name)
  }, function (e) {
    e.message = name + ': ' + e.message
    throw e
  })
}

await check('happy path owns Outcome, move, derived checks, unlock, and cleanup', function () {
  const fx = fixture()
  try {
    const result = run(fx, [fx.stem, '--outcome-file', fx.draft, '--json'])
    assert.equal(result.status, 0, result.stderr + result.stdout)
    assert.deepEqual(JSON.parse(result.stdout), { stem: fx.stem, completed: true, transactionId: JSON.parse(result.stdout).transactionId })
    const events = taskStateEvents(result.stderr)
    assert.ok(events.length >= 6, 'every canonical finalizer validation must be observable')
    for (const event of events) {
      assert.equal(event.version, 1)
      assert.equal(event.event, 'task-state-validation')
      assert.equal(event.caller, 'finalizer')
      assert.equal(event.scope, fx.stem)
      assert.ok(['valid', 'invalid'].includes(event.result))
      assert.equal(JSON.stringify(event).includes('Finish the fixture'), false)
      assert.equal(JSON.stringify(event).includes('Runtime verify'), false)
    }
    assert.equal(events.some((event) => event.action === 'run'), false,
      'finalizer telemetry must never disguise finalize admission as a runner action')
    assert.equal(result.stderr.includes('task-validation-event'), false,
      'unversioned validation telemetry must not be emitted')
    assertComplete(fx)
    assert.match(readFileSync(join(fx.root, 'orchestrator/tasks/done', fx.stem + '.md'), 'utf8'), /## Outcome/)
    const again = run(fx, [fx.stem])
    assert.equal(again.status, 0, again.stderr + again.stdout)
    assert.match(again.stdout, /already fully finalized/)
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('marker activation binds the exact transitive test graph and active lock before publication', function () {
  const fx = fixture()
  try {
    const crashed = run(fx, [fx.stem, '--outcome-file', fx.draft], { FINALIZE_FAILPOINT: 'after-intent:outcome' })
    assert.equal(crashed.status, 97, crashed.stderr + crashed.stdout)
    const binding = marker(fx).artifacts
    assert.deepEqual(Object.keys(binding).filter((key) => key.startsWith('test')).sort(), [
      'testLockHash', 'testPolicyHash', 'testRunId', 'testSessionId',
      'testSourceSnapshotHash', 'testSummaryHash', 'testTaskInputHash',
    ])
    const summary = JSON.parse(readFileSync(join(
      fx.root, 'orchestrator/.cache/tasks/test-certification', fx.stem,
      'run-fixture-00000001', 'summary.json'), 'utf8'))
    assert.equal(binding.testSummaryHash, summary.summaryHash)
    assert.equal(binding.testTaskInputHash, summary.taskInputHash)
    assert.equal(binding.testRunId, 'run-fixture-00000001')
    const resumed = run(fx, [fx.stem])
    assert.equal(resumed.status, 0, resumed.stderr + resumed.stdout)
    assertComplete(fx)
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('forged self-hashed PASS with a missing receipt is rejected before marker publication', function () {
  const fx = fixture({ forgePass: true })
  try {
    const result = run(fx, [fx.stem, '--outcome-file', fx.draft])
    assert.equal(result.status, 1, result.stderr + result.stdout)
    assert.match(result.stderr, /TEST_EVIDENCE_STALE/)
    assert.ok(existsSync(join(fx.root, 'orchestrator/tasks/todo', fx.stem + '.md')))
    assert.ok(!existsSync(join(fx.root, 'orchestrator/.cache/tasks/finalizations', fx.stem + '.json')))
    assert.equal(readFileNames(join(fx.root, 'orchestrator/.cache/tasks/finalizations'))
      .filter((name) => name.endsWith('.outcome.md')).length, 0)
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('new finalization cannot omit the mandatory tests gate or its binding', function () {
  const fx = fixture()
  try {
    writeFileSync(fx.draft, outcomeDraft.replace(
      '- `tests` — skipped (test-not-applicable: documentation-only)', '- none'))
    const result = run(fx, [fx.stem, '--outcome-file', fx.draft])
    assert.equal(result.status, 1, result.stderr + result.stdout)
    assert.match(result.stderr, /OUTCOME_TESTS_GATE_MISSING/)
    assert.ok(!existsSync(join(fx.root, 'orchestrator/.cache/tasks/finalizations', fx.stem + '.json')))
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('a task-body tests decoy cannot impersonate the Outcome Build gates row', function () {
  const fx = fixture({ task: '# TASK 7 — Tests decoy\n\n## Goal\n- `tests` — pass\n' })
  try {
    writeFileSync(fx.draft, outcomeDraft.replace(
      '- `tests` — skipped (test-not-applicable: documentation-only)', '- none'))
    const result = run(fx, [fx.stem, '--outcome-file', fx.draft])
    assert.equal(result.status, 1, result.stderr + result.stdout)
    assert.match(result.stderr, /OUTCOME_TESTS_GATE_MISSING/)
    assert.ok(!existsSync(join(fx.root, 'orchestrator/.cache/tasks/finalizations', fx.stem + '.json')))
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('the published tests row and the verified tests row are the same row', function () {
  // A fenced or indented near-miss would let the Outcome a human reads
  // disagree with the gate the finalizer verified.
  const variants = [
    ['fenced', outcomeDraft.replace('- `tests` — skipped (test-not-applicable: documentation-only)',
      '- none\n\n```\n- `tests` — pass\n```')],
    ['indented duplicate', outcomeDraft.replace('- `tests` — skipped (test-not-applicable: documentation-only)',
      '- `tests` — skipped (test-not-applicable: documentation-only)\n  - `tests` — pass')],
  ]
  for (const [label, draft] of variants) {
    const fx = fixture()
    try {
      writeFileSync(fx.draft, draft)
      const result = run(fx, [fx.stem, '--outcome-file', fx.draft])
      assert.equal(result.status, 1, `${label}: ${result.stderr}${result.stdout}`)
      assert.match(result.stderr, /OUTCOME_TESTS_GATE_INVALID/, label)
      assert.ok(!existsSync(join(fx.root, 'orchestrator/.cache/tasks/finalizations', fx.stem + '.json')), label)
    } finally { rmSync(fx.root, { recursive: true, force: true }) }
  }
})

for (const phase of phases) for (const boundary of ['after-intent', 'after-effect']) {
  await check(`crash recovery ${boundary}:${phase}`, function () {
    const fx = fixture()
    try {
      const crashed = run(fx, [fx.stem, '--outcome-file', fx.draft], { FINALIZE_FAILPOINT: `${boundary}:${phase}` })
      assert.equal(crashed.status, 97, crashed.stderr + crashed.stdout)
      assert.ok(existsSync(join(fx.root, 'orchestrator/.cache/tasks/finalizations', fx.stem + '.json')))
      const resumed = run(fx, [fx.stem])
      assert.equal(resumed.status, 0, resumed.stderr + resumed.stdout)
      assertComplete(fx)
    } finally { rmSync(fx.root, { recursive: true, force: true }) }
  })
}

for (const stage of ['after-replace-candidate', 'after-replace-wal', 'after-replace-detach', 'after-replace-publish']) {
  await check(`marker replacement protocol recovers a hard crash at ${stage}`, function () {
    const fx = fixture()
    try {
      const stateDir = join(fx.root, 'orchestrator/.cache/tasks/finalizations')
      const target = join(stateDir, fx.stem + '.json')
      const sentinel = join(fx.root, `marker-wal-crash-${stage}`)
      const crashed = run(fx, [fx.stem, '--outcome-file', fx.draft], {
        FINALIZE_FS_TEST_CRASH_STAGE: stage,
        FINALIZE_FS_TEST_CRASH_TARGET: target,
        FINALIZE_FS_TEST_CRASH_SENTINEL: sentinel,
        FINALIZE_FS_TEST_ROOT: fx.root,
      })
      assert.equal(crashed.status, 88, crashed.stderr + crashed.stdout)
      assert.ok(existsSync(sentinel), 'hard-crash failpoint did not fire')
      const residue = readFileNames(stateDir)
      if (stage === 'after-replace-candidate') {
        assert.ok(residue.includes(`.${fx.stem}.json.replace-reservation.json`),
          'durable pre-WAL reservation must survive the candidate crash')
        assert.equal(residue.includes(`.${fx.stem}.json.replace-wal.json`), false,
          'candidate failpoint must exercise the pre-WAL interval')
      } else {
        assert.ok(residue.includes(`.${fx.stem}.json.replace-wal.json`), 'durable replace WAL must survive the crash')
      }
      if (stage === 'after-replace-detach') {
        assert.equal(existsSync(target), false, 'fixture must exercise the missing-marker window')
        assert.ok(residue.some((name) => name.startsWith(`.${fx.stem}.json.replace-detached-`)))
      }
      const status = run(fx, [fx.stem, '--status', '--json'])
      assert.equal(status.status, 0, status.stderr + status.stdout)
      assert.equal(JSON.parse(status.stdout).status, 'recovery-required',
        'read-only status must not misreport a detached marker as absent')
      const listed = run(fx, ['--list-incomplete', '--json'])
      assert.equal(listed.status, 0, listed.stderr + listed.stdout)
      const projectedRows = JSON.parse(listed.stdout).filter((row) => row.stem === fx.stem)
      assert.equal(projectedRows.length, 1)
      assert.equal(projectedRows[0].status, 'recovery-required')
      const resumed = run(fx, [fx.stem])
      assert.equal(resumed.status, 0, resumed.stderr + resumed.stdout)
      assertComplete(fx)
      assert.equal(readFileNames(stateDir).some((name) => name.includes('.replace-')), false,
        'successful recovery must remove every owned replace artifact')
    } finally { rmSync(fx.root, { recursive: true, force: true }) }
  })
}

await check('Outcome compare-and-detach crash restores the exact source and resumes', function () {
  const fx = fixture()
  try {
    const crashed = run(fx, [fx.stem, '--outcome-file', fx.draft], { FINALIZE_FAILPOINT: 'after-detach:outcome' })
    assert.equal(crashed.status, 97, crashed.stderr + crashed.stdout)
    assert.ok(readFileNames(join(fx.root, 'orchestrator/tasks/todo')).some((name) => name.endsWith('.outcome-detach.md')))

    const resumed = run(fx, [fx.stem])
    assert.equal(resumed.status, 0, resumed.stderr + resumed.stdout)
    assertComplete(fx)
    assert.equal(readFileNames(join(fx.root, 'orchestrator/tasks/todo')).filter((name) => name.includes('.outcome-')).length, 0)
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('todo replacement immediately before Outcome commit is restored and never overwritten or adopted', function () {
  const fx = fixture()
  try {
    const todo = join(fx.root, 'orchestrator/tasks/todo', fx.stem + '.md')
    const replacement = join(fx.root, 'concurrent-todo.md')
    const replacementBytes = canonicalTodo('# TASK 7 — Concurrent replacement\n')
    writeFileSync(replacement, replacementBytes)
    const raced = run(fx, [fx.stem, '--outcome-file', fx.draft], {
      FINALIZE_TEST_REPLACE_TODO_BEFORE_OUTCOME_COMMIT: replacement,
    })
    assert.equal(raced.status, 1, raced.stderr + raced.stdout)
    assert.match(raced.stderr, /TASK_CHANGED_DURING_FINALIZATION/)
    assert.equal(readFileSync(todo, 'utf8'), replacementBytes)
    assert.ok(readFileNames(dirname(todo)).some((name) => name.endsWith('.outcome-source.md')),
      'the original pre-Outcome generation remains as recovery evidence')

    const retried = run(fx, [fx.stem])
    assert.equal(retried.status, 1, retried.stderr + retried.stdout)
    assert.match(retried.stderr, /TASK_CHANGED_DURING_FINALIZATION/)
    assert.equal(readFileSync(todo, 'utf8'), replacementBytes)
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('child failure records exact phase and keeps lock', function () {
  const fx = fixture()
  try {
    const result = run(fx, [fx.stem, '--outcome-file', fx.draft], { FAKE_ARCH_FAIL: '1' })
    assert.equal(result.status, 1)
    const m = marker(fx)
    assert.equal(m.phase, 'arch')
    assert.equal(m.lastError.code, 'ARCH_REGEN_FAILED')
    assert.ok(existsSync(join(fx.root, 'orchestrator/.cache/tasks/locks', fx.stem + '.json')))
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('new finalization requires the task run lock it will later release', function () {
  const fx = fixture()
  try {
    rmSync(join(fx.root, 'orchestrator/.cache/tasks/locks', fx.stem + '.json'))
    const result = run(fx, [fx.stem, '--outcome-file', fx.draft])
    assert.equal(result.status, 1)
    assert.match(result.stderr, /TASK_LOCK_MISSING/)
    assert.ok(existsSync(join(fx.root, 'orchestrator/tasks/todo', fx.stem + '.md')))
    assert.ok(!existsSync(join(fx.root, 'orchestrator/.cache/tasks/finalizations', fx.stem + '.json')))
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('new finalization accepts only a canonical orchestrator lock owned by its live writer session', function () {
  const malformed = fixture()
  try {
    writeFileSync(join(malformed.root, 'orchestrator/.cache/tasks/locks', malformed.stem + '.json'), '{"stage":"orchestrator","owner":"fixture"}\n')
    const result = run(malformed, [malformed.stem, '--outcome-file', malformed.draft])
    assert.equal(result.status, 1)
    assert.match(result.stderr, /TASK_LOCK_INVALID/)
    assert.ok(existsSync(join(malformed.root, 'orchestrator/tasks/todo', malformed.stem + '.md')))
    assert.ok(!existsSync(join(malformed.root, 'orchestrator/.cache/tasks/finalizations', malformed.stem + '.json')))
  } finally { rmSync(malformed.root, { recursive: true, force: true }) }

  const prep = fixture({ lockStage: 'task-prep' })
  try {
    const result = run(prep, [prep.stem, '--outcome-file', prep.draft])
    assert.equal(result.status, 1)
    assert.match(result.stderr, /TASK_LOCK_INVALID/)
    assert.ok(existsSync(join(prep.root, 'orchestrator/tasks/todo', prep.stem + '.md')))
    assert.ok(!existsSync(join(prep.root, 'orchestrator/.cache/tasks/finalizations', prep.stem + '.json')))
  } finally { rmSync(prep.root, { recursive: true, force: true }) }

  const foreign = fixture()
  let lease
  try {
    lease = writerLeases.acquire(join(foreign.root, 'orchestrator/.cache/tasks/finalizations/.writers'), {
      kind: 'task-session', stem: foreign.stem, key: 'task:' + foreign.stem,
      sessionId: writerLeases.createSessionId(), ownerPid: process.pid,
    })
    const result = run(foreign, [foreign.stem, '--outcome-file', foreign.draft], {
      ORCHESTRATOR_WRITER_SESSION_ID: lease.record.sessionId,
    })
    assert.equal(result.status, 1)
    assert.match(result.stderr, /TASK_LOCK_OWNER_MISMATCH/)
    assert.ok(existsSync(join(foreign.root, 'orchestrator/tasks/todo', foreign.stem + '.md')))
    assert.ok(!existsSync(join(foreign.root, 'orchestrator/.cache/tasks/finalizations', foreign.stem + '.json')))
  } finally {
    if (lease) writerLeases.release(lease)
    rmSync(foreign.root, { recursive: true, force: true })
  }
})

await check('invalid canonical done candidates are rejected before marker, snapshot, or ship publication', function () {
  const fx = fixture()
  try {
    const cases = [
      { draft: outcomeDraft.replace('**Completed at**: 2026-01-01T00:00:00Z', '**Completed at**: yesterday'), code: 'OUTCOME_CANONICAL_INVALID' },
      { draft: outcomeDraft.replace('**Review iterations**: 1', '**Review iterations**: -99'), code: 'OUTCOME_CANONICAL_INVALID' },
      { draft: outcomeDraft.replace('### Files touched\n\n- none', '### Files touched\n\n- `../../outside` — modified'), code: 'OUTCOME_INVALID' },
    ]
    for (const item of cases) {
      writeFileSync(fx.draft, item.draft)
      const result = run(fx, [fx.stem, '--outcome-file', fx.draft])
      assert.equal(result.status, 1, result.stderr + result.stdout)
      assert.match(result.stderr, new RegExp(item.code))
      assert.ok(existsSync(join(fx.root, 'orchestrator/tasks/todo', fx.stem + '.md')))
      assert.ok(!existsSync(join(fx.root, 'orchestrator/tasks/done', fx.stem + '.md')))
      assert.ok(!existsSync(join(fx.root, 'orchestrator/.cache/tasks/finalizations', fx.stem + '.json')))
      assert.equal(readFileNames(join(fx.root, 'orchestrator/.cache/tasks/finalizations')).filter((name) => name.endsWith('.outcome.md')).length, 0)
    }
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('post-mutex writer handshake blocks foreign writers but admits the same-stem owner turn', function () {
  const blocked = fixture()
  try {
    const leasesDir = join(blocked.root, 'orchestrator/.cache/tasks/finalizations/.writers')
    const foreign = writerLeases.acquire(leasesDir, {
      kind: 'workspace-session', key: 'setup', sessionId: writerLeases.createSessionId(), ownerPid: process.pid
    })
    const refused = run(blocked, [blocked.stem, '--outcome-file', blocked.draft])
    assert.equal(refused.status, 1)
    assert.match(refused.stderr, /WORKSPACE_WRITER_ACTIVE/)
    assert.ok(existsSync(join(blocked.root, 'orchestrator/tasks/todo', blocked.stem + '.md')))
    assert.ok(!existsSync(join(blocked.root, 'orchestrator/.cache/tasks/finalizations', blocked.stem + '.json')))
    writerLeases.release(foreign)
    const resumed = run(blocked, [blocked.stem, '--outcome-file', blocked.draft])
    assert.equal(resumed.status, 0, resumed.stderr + resumed.stdout)
  } finally { rmSync(blocked.root, { recursive: true, force: true }) }

  const owner = fixture()
  try {
    const ownerLease = writerLeases.acquire(join(owner.root, 'orchestrator/.cache/tasks/finalizations/.writers'), {
      kind: 'task-session', stem: owner.stem, key: 'task:' + owner.stem,
      sessionId: writerLeases.createSessionId(), ownerPid: process.pid
    })
    writeCanonicalLock(owner.root, owner.stem, ownerLease.record.sessionId)
    writeTestEvidence(owner.root, owner.stem)
    const result = run(owner, [owner.stem, '--outcome-file', owner.draft], {
      ORCHESTRATOR_WRITER_SESSION_ID: ownerLease.record.sessionId,
    })
    assert.equal(result.status, 0, result.stderr + result.stdout)
    assertComplete(owner)
    writerLeases.release(ownerLease)
  } finally { rmSync(owner.root, { recursive: true, force: true }) }

  const ambiguous = fixture()
  try {
    const dir = join(ambiguous.root, 'orchestrator/.cache/tasks/finalizations/.writers')
    const first = writerLeases.acquire(dir, {
      kind: 'task-session', stem: ambiguous.stem, key: 'task:' + ambiguous.stem,
      sessionId: writerLeases.createSessionId(), ownerPid: process.pid,
    })
    const second = writerLeases.acquire(dir, {
      kind: 'task-session', stem: ambiguous.stem, key: 'task:' + ambiguous.stem,
      sessionId: writerLeases.createSessionId(), ownerPid: process.pid,
    })
    const refused = run(ambiguous, [ambiguous.stem, '--outcome-file', ambiguous.draft])
    assert.equal(refused.status, 1)
    assert.match(refused.stderr, /WORKSPACE_WRITER_ACTIVE/)
    const mismatched = run(ambiguous, [ambiguous.stem, '--outcome-file', ambiguous.draft], {
      ORCHESTRATOR_WRITER_SESSION_ID: writerLeases.createSessionId(),
    })
    assert.equal(mismatched.status, 1)
    assert.match(mismatched.stderr, /WRITER_SESSION_OWNER_MISSING/)
    writerLeases.release(first)
    writerLeases.release(second)
  } finally { rmSync(ambiguous.root, { recursive: true, force: true }) }

  const standby = fixture()
  try {
    const lease = writerLeases.acquire(join(standby.root, 'orchestrator/.cache/tasks/finalizations/.writers'), {
      kind: 'task-session', stem: standby.stem, key: 'standby:run',
      sessionId: writerLeases.createSessionId(), ownerPid: process.pid,
    })
    writeCanonicalLock(standby.root, standby.stem, lease.record.sessionId)
    writeTestEvidence(standby.root, standby.stem)
    const result = run(standby, [standby.stem, '--outcome-file', standby.draft, '--writer-session-id', lease.record.sessionId])
    assert.equal(result.status, 0, result.stderr + result.stdout)
    assertComplete(standby)
    writerLeases.release(lease)
  } finally { rmSync(standby.root, { recursive: true, force: true }) }

  const unverified = fixture()
  try {
    const lease = writerLeases.acquire(join(unverified.root, 'orchestrator/.cache/tasks/finalizations/.writers'), {
      kind: 'task-session', stem: unverified.stem, key: 'task:' + unverified.stem,
      sessionId: writerLeases.createSessionId(), ownerPid: process.pid,
    })
    writerLeases.markUnverified(lease, 'fixture descendant death was not proven')
    const refused = run(unverified, [unverified.stem, '--outcome-file', unverified.draft], {
      ORCHESTRATOR_WRITER_SESSION_ID: lease.record.sessionId,
    })
    assert.equal(refused.status, 1)
    assert.match(refused.stderr, /WRITER_TREE_UNVERIFIED/)
    assert.ok(!existsSync(join(unverified.root, 'orchestrator/.cache/tasks/finalizations', unverified.stem + '.json')))
    writerLeases.release(lease)
  } finally { rmSync(unverified.root, { recursive: true, force: true }) }
})

await check('changed lock is never removed', function () {
  const fx = fixture()
  try {
    const crashed = run(fx, [fx.stem, '--outcome-file', fx.draft], { FINALIZE_FAILPOINT: 'after-effect:verify' })
    assert.equal(crashed.status, 97)
    const lp = join(fx.root, 'orchestrator/.cache/tasks/locks', fx.stem + '.json')
    writeFileSync(lp, '{"stage":"new-owner"}\n')
    const resumed = run(fx, [fx.stem])
    assert.equal(resumed.status, 1)
    assert.equal(marker(fx).lastError.code, 'LOCK_OWNERSHIP_CONFLICT')
    assert.match(readFileSync(lp, 'utf8'), /new-owner/)
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('architecture gate is repeated immediately before unlock', function () {
  const fx = fixture()
  try {
    const crashed = run(fx, [fx.stem, '--outcome-file', fx.draft], { FINALIZE_FAILPOINT: 'after-effect:verify' })
    assert.equal(crashed.status, 97, crashed.stderr + crashed.stdout)
    const resumed = run(fx, [fx.stem], { FAKE_ARCH_FAIL: '1' })
    assert.equal(resumed.status, 1, resumed.stderr + resumed.stdout)
    assert.match(resumed.stderr, /ARCH_CHECK_FAILED/)
    assert.ok(existsSync(join(fx.root, 'orchestrator/.cache/tasks/locks', fx.stem + '.json')),
      'derived-state failure must preserve the canonical task lock')
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('Figma done gate is repeated immediately before unlock', function () {
  const fx = fixture({ figma: true })
  try {
    const crashed = run(fx, [fx.stem, '--outcome-file', fx.draft], { FINALIZE_FAILPOINT: 'after-effect:verify' })
    assert.equal(crashed.status, 97, crashed.stderr + crashed.stdout)
    const resumed = run(fx, [fx.stem], { FAKE_VERIFY_FAIL: '1' })
    assert.equal(resumed.status, 1, resumed.stderr + resumed.stdout)
    assert.match(resumed.stderr, /FIGMA_DONE_VERIFY_FAILED/)
    assert.ok(existsSync(join(fx.root, 'orchestrator/.cache/tasks/locks', fx.stem + '.json')))
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('a foreign lock detached in the unlock race is restored without clobbering and retained as proof', function () {
  const fx = fixture()
  try {
    const crashed = run(fx, [fx.stem, '--outcome-file', fx.draft], { FINALIZE_FAILPOINT: 'after-intent:unlock' })
    assert.equal(crashed.status, 97, crashed.stderr + crashed.stdout)
    const lock = join(fx.root, 'orchestrator/.cache/tasks/locks', fx.stem + '.json')
    rmSync(lock)
    writeFileSync(lock, '{"stage":"foreign-owner"}\n')
    const resumed = run(fx, [fx.stem])
    assert.equal(resumed.status, 1, resumed.stderr + resumed.stdout)
    assert.match(resumed.stderr, /LOCK_OWNERSHIP_CONFLICT/)
    assert.match(readFileSync(lock, 'utf8'), /foreign-owner/, 'foreign canonical lock must be restored')
    const proof = readFileNames(dirname(lock)).find((name) => name.endsWith('.unlock.json'))
    assert.ok(proof, 'detached foreign inode remains at the private proof path')
    assert.match(readFileSync(join(dirname(lock), proof), 'utf8'), /foreign-owner/)
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('unlock never overwrites a racing no-clobber proof', function () {
  const fx = fixture()
  try {
    const crashed = run(fx, [fx.stem, '--outcome-file', fx.draft], { FINALIZE_FAILPOINT: 'after-intent:unlock' })
    assert.equal(crashed.status, 97, crashed.stderr + crashed.stdout)
    const m = marker(fx)
    const lockDir = join(fx.root, 'orchestrator/.cache/tasks/locks')
    const lock = join(lockDir, fx.stem + '.json')
    const original = readFileSync(lock, 'utf8')
    const proof = join(lockDir, `.finalize-${fx.stem}-${m.transactionId}.unlock.json`)
    writeFileSync(proof, '{"stage":"racing-proof-owner"}\n')
    const resumed = run(fx, [fx.stem])
    assert.equal(resumed.status, 1, resumed.stderr + resumed.stdout)
    assert.match(resumed.stderr, /LOCK_OWNERSHIP_CONFLICT/)
    assert.equal(readFileSync(lock, 'utf8'), original, 'captured canonical lock remains untouched')
    assert.match(readFileSync(proof, 'utf8'), /racing-proof-owner/, 'racing proof is never overwritten')
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('lock disappearance before the first unlock effect is a conflict', function () {
  const fx = fixture()
  try {
    const crashed = run(fx, [fx.stem, '--outcome-file', fx.draft], { FINALIZE_FAILPOINT: 'after-effect:verify' })
    assert.equal(crashed.status, 97)
    rmSync(join(fx.root, 'orchestrator/.cache/tasks/locks', fx.stem + '.json'))
    const resumed = run(fx, [fx.stem])
    assert.equal(resumed.status, 1)
    assert.match(resumed.stderr, /LOCK_DISAPPEARED/)
    assert.ok(existsSync(join(fx.root, 'orchestrator/.cache/tasks/finalizations', fx.stem + '.json')))
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('lock disappearance after unlock intent is still ambiguous and never auto-accepted', function () {
  const fx = fixture()
  try {
    const crashed = run(fx, [fx.stem, '--outcome-file', fx.draft], { FINALIZE_FAILPOINT: 'after-intent:unlock' })
    assert.equal(crashed.status, 97, crashed.stderr + crashed.stdout)
    rmSync(join(fx.root, 'orchestrator/.cache/tasks/locks', fx.stem + '.json'))
    const resumed = run(fx, [fx.stem])
    assert.equal(resumed.status, 1)
    assert.match(resumed.stderr, /LOCK_DISAPPEARED/)
    assert.ok(existsSync(join(fx.root, 'orchestrator/.cache/tasks/finalizations', fx.stem + '.json')))
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('durable unlock detachment proof makes the owned rename recoverable but not forgeable by absence', function () {
  const recoverable = fixture()
  try {
    const crashed = run(recoverable, [recoverable.stem, '--outcome-file', recoverable.draft], { FINALIZE_FAILPOINT: 'after-detach:unlock' })
    assert.equal(crashed.status, 97, crashed.stderr + crashed.stdout)
    assert.ok(!existsSync(join(recoverable.root, 'orchestrator/.cache/tasks/locks', recoverable.stem + '.json')))
    const proofs = readFileNames(join(recoverable.root, 'orchestrator/.cache/tasks/locks')).filter((name) => name.endsWith('.unlock.json'))
    assert.equal(proofs.length, 1)
    const resumed = run(recoverable, [recoverable.stem])
    assert.equal(resumed.status, 0, resumed.stderr + resumed.stdout)
    assertComplete(recoverable)
  } finally { rmSync(recoverable.root, { recursive: true, force: true }) }

  const missingProof = fixture()
  try {
    const crashed = run(missingProof, [missingProof.stem, '--outcome-file', missingProof.draft], { FINALIZE_FAILPOINT: 'after-detach:unlock' })
    assert.equal(crashed.status, 97, crashed.stderr + crashed.stdout)
    const proofDir = join(missingProof.root, 'orchestrator/.cache/tasks/locks')
    const proof = readFileNames(proofDir).find((name) => name.endsWith('.unlock.json'))
    assert.ok(proof)
    rmSync(join(proofDir, proof))
    const resumed = run(missingProof, [missingProof.stem])
    assert.equal(resumed.status, 1)
    assert.match(resumed.stderr, /LOCK_DISAPPEARED/)
  } finally { rmSync(missingProof.root, { recursive: true, force: true }) }
})

await check('unexpected task bytes are never overwritten during Outcome recovery', function () {
  const fx = fixture()
  try {
    const crashed = run(fx, [fx.stem, '--outcome-file', fx.draft], { FINALIZE_FAILPOINT: 'after-intent:outcome' })
    assert.equal(crashed.status, 97, crashed.stderr + crashed.stdout)
    const todo = join(fx.root, 'orchestrator/tasks/todo', fx.stem + '.md')
    writeFileSync(todo, '# externally replaced task\n')
    const resumed = run(fx, [fx.stem])
    assert.equal(resumed.status, 1)
    assert.match(resumed.stderr, /OUTCOME_INVALID|TASK_CHANGED_DURING_FINALIZATION/)
    assert.equal(readFileSync(todo, 'utf8'), '# externally replaced task\n')
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('rollback to original todo bytes after Outcome success re-arms snapshot publication', function () {
  const fx = fixture()
  try {
    const todo = join(fx.root, 'orchestrator/tasks/todo', fx.stem + '.md')
    const original = readFileSync(todo)
    const crashed = run(fx, [fx.stem, '--outcome-file', fx.draft], { FINALIZE_FAILPOINT: 'after-intent:components' })
    assert.equal(crashed.status, 97, crashed.stderr + crashed.stdout)
    assert.equal(marker(fx).phases.outcome.state, 'succeeded')
    writeFileSync(todo, original)
    const resumed = run(fx, [fx.stem])
    assert.equal(resumed.status, 0, resumed.stderr + resumed.stdout)
    assert.match(readFileSync(join(fx.root, 'orchestrator/tasks/done', fx.stem + '.md'), 'utf8'), /## Outcome/)
    assertComplete(fx)
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('Outcome refresh crash preserves the previously committed immutable snapshot', function () {
  const fx = fixture()
  try {
    const first = run(fx, [fx.stem, '--outcome-file', fx.draft], { FINALIZE_FAILPOINT: 'after-effect:outcome' })
    assert.equal(first.status, 97, first.stderr + first.stdout)
    const before = marker(fx)
    const refreshedDraft = join(fx.root, 'outcome-refreshed.md')
    writeFileSync(refreshedDraft, outcomeDraft.replace('2026-01-01T00:00:00Z', '2026-02-02T00:00:00Z'))
    const second = run(fx, [fx.stem, '--outcome-file', refreshedDraft], { FINALIZE_FAILPOINT: 'after-snapshot:outcome-refresh' })
    assert.equal(second.status, 97, second.stderr + second.stdout)
    const after = marker(fx)
    assert.equal(after.source.snapshotHash, before.source.snapshotHash, 'marker must still reference the old committed snapshot')
    const resumed = run(fx, [fx.stem])
    assert.equal(resumed.status, 0, resumed.stderr + resumed.stdout)
    const done = readFileSync(join(fx.root, 'orchestrator/tasks/done', fx.stem + '.md'), 'utf8')
    assert.match(done, /2026-01-01T00:00:00Z/)
    assert.doesNotMatch(done, /2026-02-02T00:00:00Z/)
    assertComplete(fx)
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('a changed pre-ship intent discards only validated stale publication/receipt proofs and rebuilds them', function () {
  const fx = fixture()
  try {
    const first = run(fx, [fx.stem, '--outcome-file', fx.draft], {
      FAKE_SHIP_PROOF: '1', FAKE_SHIP_CRASH_AFTER_PROOF: '1',
    })
    assert.equal(first.status, 1, first.stderr + first.stdout)
    const before = marker(fx)
    assert.equal(before.phase, 'ship')
    const proof = join(fx.root, 'orchestrator/tasks/todo', `.finalize-${fx.stem}-${before.transactionId}.ship`)
    assert.ok(existsSync(proof), 'old immutable publication proof must exist')
    const tempAlias = `${proof}.tmp.crash-window`
    writeFileSync(tempAlias, 'partial candidate bytes\n')
    const stage = join(fx.root, 'orchestrator/.cache/tasks/finalizations', `.finalize-${fx.stem}-${before.transactionId}.receipts`)
    mkdirSync(stage, { recursive: true })
    writeFileSync(join(stage, 'stale-receipt'), 'old receipt bytes\n')

    const refreshedDraft = join(fx.root, 'outcome-refreshed-after-proof.md')
    writeFileSync(refreshedDraft, outcomeDraft.replace('2026-01-01T00:00:00Z', '2026-05-05T00:00:00Z'))
    const resumed = run(fx, [fx.stem, '--outcome-file', refreshedDraft], { FAKE_SHIP_PROOF: '1' })
    assert.equal(resumed.status, 0, resumed.stderr + resumed.stdout)
    const done = readFileSync(join(fx.root, 'orchestrator/tasks/done', fx.stem + '.md'), 'utf8')
    assert.match(done, /2026-05-05T00:00:00Z/)
    assert.doesNotMatch(done, /2026-01-01T00:00:00Z/)
    assert.ok(!existsSync(tempAlias))
    assert.ok(!existsSync(stage))
    assertComplete(fx)
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('completed cleanup marker is reverified and retained if a new lock appears', function () {
  const fx = fixture()
  try {
    const crashed = run(fx, [fx.stem, '--outcome-file', fx.draft], { FINALIZE_FAILPOINT: 'after-effect:cleanup' })
    assert.equal(crashed.status, 97, crashed.stderr + crashed.stdout)
    const markerPath = join(fx.root, 'orchestrator/.cache/tasks/finalizations', fx.stem + '.json')
    assert.equal(JSON.parse(readFileSync(markerPath, 'utf8')).status, 'completed')
    writeFileSync(join(fx.root, 'orchestrator/.cache/tasks/locks', fx.stem + '.json'), '{"stage":"new-owner"}\n')
    const resumed = run(fx, [fx.stem])
    assert.equal(resumed.status, 1)
    assert.match(resumed.stderr, /LOCK_REAPPEARED/)
    assert.ok(existsSync(markerPath), 'recovery authority must remain')
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('cleanup remains durably completed after snapshots are deleted and retry finishes idempotently', function () {
  const fx = fixture()
  try {
    const crashed = run(fx, [fx.stem, '--outcome-file', fx.draft], { FINALIZE_FAILPOINT: 'after-snapshots:cleanup' })
    assert.equal(crashed.status, 97, crashed.stderr + crashed.stdout)
    const markerPath = join(fx.root, 'orchestrator/.cache/tasks/finalizations', fx.stem + '.json')
    assert.equal(JSON.parse(readFileSync(markerPath, 'utf8')).status, 'completed')
    assert.equal(readFileNames(dirname(markerPath)).filter((name) => name.startsWith(fx.stem + '.') && name.endsWith('.outcome.md')).length, 0)

    const resumed = run(fx, [fx.stem])
    assert.equal(resumed.status, 0, resumed.stderr + resumed.stdout)
    assertComplete(fx)
    assert.equal(existsSync(markerPath), false)
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('transaction publication proof survives crashes until completed cleanup verifies and removes it', function () {
  const fx = fixture()
  try {
    const crashed = run(fx, [fx.stem, '--outcome-file', fx.draft], {
      FAKE_SHIP_PROOF: '1', FINALIZE_FAILPOINT: 'after-effect:cleanup',
    })
    assert.equal(crashed.status, 97, crashed.stderr + crashed.stdout)
    const proof = readFileNames(join(fx.root, 'orchestrator/tasks/todo')).find((name) => name.endsWith('.ship'))
    assert.ok(proof, 'publication proof must remain while the completed marker is recovery authority')
    const resumed = run(fx, [fx.stem], { FAKE_SHIP_PROOF: '1' })
    assert.equal(resumed.status, 0, resumed.stderr + resumed.stdout)
    assertComplete(fx)
    assert.equal(readFileNames(join(fx.root, 'orchestrator/tasks/todo')).filter((name) => name.endsWith('.ship')).length, 0)
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('cleanup rejects even metadata-only edits to the published done artifact', function () {
  const fx = fixture()
  try {
    const crashed = run(fx, [fx.stem, '--outcome-file', fx.draft], { FINALIZE_FAILPOINT: 'after-intent:cleanup' })
    assert.equal(crashed.status, 97, crashed.stderr + crashed.stdout)
    const done = join(fx.root, 'orchestrator/tasks/done', fx.stem + '.md')
    writeFileSync(done, readFileSync(done, 'utf8') + '- Figma meta: forged metadata-only edit\n')
    const resumed = run(fx, [fx.stem])
    assert.equal(resumed.status, 1)
    assert.match(resumed.stderr, /DONE_TASK_CHANGED/)
    assert.ok(existsSync(join(fx.root, 'orchestrator/.cache/tasks/finalizations', fx.stem + '.json')))
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('project Figma applicability/config is frozen for the whole transaction', function () {
  const fx = fixture()
  try {
    const crashed = run(fx, [fx.stem, '--outcome-file', fx.draft], { FINALIZE_FAILPOINT: 'after-intent:components' })
    assert.equal(crashed.status, 97, crashed.stderr + crashed.stdout)
    writeFileSync(join(fx.root, 'orchestrator/project-config.md'), '---\nfigmaEnabled: false\nchangedDuringFinalization: true\n---\n')
    const resumed = run(fx, [fx.stem])
    assert.equal(resumed.status, 1)
    assert.match(resumed.stderr, /PROJECT_CONFIG_CHANGED/)
    assert.ok(existsSync(join(fx.root, 'orchestrator/.cache/tasks/finalizations', fx.stem + '.json')))
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('invalid startup config leaves no orphan Outcome snapshot without a marker', function () {
  for (const config of [
    'figmaEnabled: maybe\n',
    'figmaEnabled:\ntrue\n',
    'figmaEnabled: TRUE\n',
    'figmaEnabled:true\n',
    'figmaEnabled: true\nfigmaEnabled: false\n',
  ]) {
    const fx = fixture()
    try {
      writeFileSync(join(fx.root, 'orchestrator/project-config.md'), config)
      const result = run(fx, [fx.stem, '--outcome-file', fx.draft])
      assert.equal(result.status, 1, JSON.stringify(config) + '\n' + result.stderr + result.stdout)
      assert.match(result.stderr, /PROJECT_CONFIG_INVALID/)
      const stateNames = readFileNames(join(fx.root, 'orchestrator/.cache/tasks/finalizations'))
      assert.ok(!stateNames.includes(fx.stem + '.json'))
      assert.equal(stateNames.filter((name) => name.startsWith(fx.stem + '.') && name.endsWith('.outcome.md')).length, 0)
    } finally { rmSync(fx.root, { recursive: true, force: true }) }
  }
})

await check('a late conflicting Outcome is rejected instead of silently ignored', function () {
  const completed = fixture()
  try {
    const first = run(completed, [completed.stem, '--outcome-file', completed.draft])
    assert.equal(first.status, 0, first.stderr + first.stdout)
    const conflicting = join(completed.root, 'conflicting-outcome.md')
    writeFileSync(conflicting, outcomeDraft.replace('2026-01-01T00:00:00Z', '2026-03-03T00:00:00Z'))
    const late = run(completed, [completed.stem, '--outcome-file', conflicting])
    assert.equal(late.status, 1)
    assert.match(late.stderr, /OUTCOME_TOO_LATE/)
  } finally { rmSync(completed.root, { recursive: true, force: true }) }

  const retained = fixture()
  try {
    const crashed = run(retained, [retained.stem, '--outcome-file', retained.draft], { FINALIZE_FAILPOINT: 'after-effect:cleanup' })
    assert.equal(crashed.status, 97, crashed.stderr + crashed.stdout)
    const conflicting = join(retained.root, 'conflicting-outcome.md')
    writeFileSync(conflicting, outcomeDraft.replace('2026-01-01T00:00:00Z', '2026-04-04T00:00:00Z'))
    const late = run(retained, [retained.stem, '--outcome-file', conflicting])
    assert.equal(late.status, 1)
    assert.match(late.stderr, /OUTCOME_TOO_LATE/)
    assert.ok(existsSync(join(retained.root, 'orchestrator/.cache/tasks/finalizations', retained.stem + '.json')))
  } finally { rmSync(retained.root, { recursive: true, force: true }) }
})

await check('Outcome draft must be only one appendix and combined task respects recovery size limit', function () {
  const full = fixture()
  try {
    writeFileSync(full.draft, `# copied whole task\n\n${outcomeDraft}`)
    const rejected = run(full, [full.stem, '--outcome-file', full.draft])
    assert.equal(rejected.status, 1)
    assert.match(rejected.stderr, /OUTCOME_DRAFT_INVALID/)
    assert.ok(!existsSync(join(full.root, 'orchestrator/.cache/tasks/finalizations', full.stem + '.json')))
  } finally { rmSync(full.root, { recursive: true, force: true }) }

  const huge = fixture()
  try {
    const prefix = canonicalTodo('# TASK 7 — Large task\n')
    const targetBytes = 8 * 1024 * 1024 - 64
    writeFileSync(join(huge.root, 'orchestrator/tasks/todo', huge.stem + '.md'), prefix + 'x'.repeat(targetBytes - Buffer.byteLength(prefix)))
    const refreshed = taskState.validateTaskState({ tasksDir: join(huge.root, 'orchestrator/tasks'), repoRoot: huge.root, checkIndex: false, includeRuntime: false })
    assert.equal(refreshed.ok, true, JSON.stringify(refreshed.findings))
    writeFileSync(join(huge.root, 'orchestrator/tasks/INDEX.json'), JSON.stringify(taskState.deriveIndex(refreshed._model, '2026-01-01T00:00:00Z'), null, 2) + '\n')
    const rejected = run(huge, [huge.stem, '--outcome-file', huge.draft])
    assert.equal(rejected.status, 1)
    assert.match(rejected.stderr, /OUTCOME_TASK_TOO_LARGE/)
    assert.ok(!existsSync(join(huge.root, 'orchestrator/.cache/tasks/finalizations', huge.stem + '.json')))
    assert.equal(readFileNames(join(huge.root, 'orchestrator/.cache/tasks/finalizations')).filter((name) => name.endsWith('.outcome.md')).length, 0)
  } finally { rmSync(huge.root, { recursive: true, force: true }) }
})

await check('a task symlink fails closed', function () {
  const link = fixture()
  try {
    const todo = join(link.root, 'orchestrator/tasks/todo', link.stem + '.md')
    rmSync(todo)
    const outside = join(link.root, 'outside.md'); writeFileSync(outside, '# outside\n')
    symlinkSync(outside, todo)
    const result = run(link, [link.stem, '--outcome-file', link.draft])
    assert.equal(result.status, 1)
    assert.match(result.stderr, /UNSAFE_TASK_FILE/)
  } finally { rmSync(link.root, { recursive: true, force: true }) }
})

await check('finalizer rejects a runtime symlink ancestor before external writes', function () {
  if (process.platform === 'win32') return
  const fx = fixture()
  const outside = mkdtempSync(join(tmpdir(), 'finalize-runtime-outside-'))
  try {
    rmSync(join(fx.root, 'orchestrator/.cache'), { recursive: true, force: true })
    symlinkSync(outside, join(fx.root, 'orchestrator/.cache'), 'dir')
    const result = run(fx, [fx.stem, '--outcome-file', fx.draft])
    assert.equal(result.status, 1, result.stderr + result.stdout)
    assert.match(result.stderr, /DIRECTORY_UNSAFE/)
    assert.deepEqual(readdirSync(outside), [], 'no finalization or lock artifact may be created through the symlink')
  } finally {
    rmSync(fx.root, { recursive: true, force: true })
    rmSync(outside, { recursive: true, force: true })
  }
})

await check('mutex helper keeps its runtime path anchored to project authority', function () {
  if (process.platform === 'win32') return
  const root = mkdtempSync(join(tmpdir(), 'finalize-mutex-authority-'))
  const outside = mkdtempSync(join(tmpdir(), 'finalize-mutex-outside-'))
  try {
    mkdirSync(join(root, 'orchestrator'), { recursive: true })
    symlinkSync(outside, join(root, 'orchestrator', '.cache'), 'dir')
    const helper = join(ROOT, 'orchestrator', 'tasks', 'finalize-lock.py')
    const lock = join(root, 'orchestrator', '.cache', 'tasks', 'finalizations', '.mutex.json')
    const result = spawnSync(process.env.FINALIZE_LOCK_PYTHON || 'python3', [
      helper, lock, 'anchored-mutex-test', root,
    ], { input: '', encoding: 'utf8' })
    assert.equal(result.status, 2, result.stderr + result.stdout)
    assert.match(result.stderr, /DIRECTORY_UNSAFE/)
    assert.deepEqual(readdirSync(outside), [], 'anchored mutex helper must not create its lock through a symlinked ancestor')
  } finally {
    rmSync(root, { recursive: true, force: true })
    rmSync(outside, { recursive: true, force: true })
  }
})

await check('corrupt marker is surfaced and never overwritten', function () {
  const fx = fixture()
  try {
    const p = join(fx.root, 'orchestrator/.cache/tasks/finalizations', fx.stem + '.json')
    writeFileSync(p, '{broken')
    const result = run(fx, [fx.stem])
    assert.equal(result.status, 1)
    assert.match(result.stderr, /MARKER_CORRUPT/)
    assert.equal(readFileSync(p, 'utf8'), '{broken')
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('oversized marker is rejected before parsing or replacement', function () {
  const fx = fixture()
  try {
    const p = join(fx.root, 'orchestrator/.cache/tasks/finalizations', fx.stem + '.json')
    writeFileSync(p, Buffer.alloc(256 * 1024 + 1, 0x20))
    const result = run(fx, [fx.stem])
    assert.equal(result.status, 1)
    assert.match(result.stderr, /UNSAFE_MARKER|exceeds 262144 bytes/)
    assert.equal(readFileSync(p).length, 256 * 1024 + 1)
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('concurrent first mutex creators serialize on one stable inode and converge', async function () {
  const fx = fixture()
  try {
    function launch() {
      return new Promise(function (resolveChild) {
        const child = spawn(process.execPath, [FINALIZER, fx.stem, '--outcome-file', fx.draft], { cwd: fx.root, env: fx.env })
        let stdout = '', stderr = ''
        child.stdout.on('data', (x) => { stdout += x })
        child.stderr.on('data', (x) => { stderr += x })
        child.on('close', (status) => resolveChild({ status, stdout, stderr }))
      })
    }
    // Darwin can return ENOENT to losers of concurrent
    // openat(O_CREAT|O_NOFOLLOW) on an absent final component.  A wider first
    // creation wave makes this kernel race a stable regression while every
    // helper must still serialize on the one O_EXCL-created inode.
    const concurrency = 8
    const results = await Promise.all(Array.from({ length: concurrency }, launch))
    assert.deepEqual(results.map((x) => x.status), Array(concurrency).fill(0),
      results.map((x) => x.stderr + x.stdout).join('\n'))
    const mutex = JSON.parse(readFileSync(join(fx.root, 'orchestrator/.cache/tasks/finalizations/.mutex.json'), 'utf8'))
    assert.equal(mutex.released, true)
    assertComplete(fx)
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('anchored lock helper first creators serialize on one stable inode', async function () {
  const root = mkdtempSync(join(tmpdir(), 'anchored-lock-race-'))
  const directory = join(root, 'shared')
  const lock = join(directory, 'shared-resource.lock')
  const helper = join(ROOT, 'orchestrator/tasks/finalize-lock.py')
  try {
    function launch(index) {
      return new Promise((resolveChild) => {
        const child = spawn(process.env.FINALIZE_LOCK_PYTHON || 'python3', [
          helper, lock, `anchored-lock-race-${index}`, root,
        ], { cwd: root, env: process.env, stdio: ['pipe', 'pipe', 'pipe'] })
        let stdout = '', stderr = '', acquired = false, settled = false, inode = null
        const finish = (result) => {
          if (settled) return
          settled = true
          clearTimeout(timer)
          resolveChild({ ...result, stdout, stderr, inode })
        }
        const timer = setTimeout(() => {
          try { child.kill('SIGKILL') } catch {}
          finish({ status: 'timeout' })
        }, 30000)
        child.stderr.on('data', (chunk) => { stderr += String(chunk) })
        child.stdout.on('data', (chunk) => {
          stdout += String(chunk)
          if (acquired || !stdout.includes('LOCKED\n')) return
          acquired = true
          const st = lstatSync(lock, { bigint: true })
          inode = `${st.dev}:${st.ino}`
          child.stdin.end()
        })
        child.on('error', (error) => finish({ status: 'spawn-error', error }))
        child.on('close', (status, signal) => finish({ status, signal }))
      })
    }
    const concurrency = 8
    const results = await Promise.all(Array.from({ length: concurrency }, (_, index) => launch(index)))
    assert.deepEqual(results.map((row) => row.status), Array(concurrency).fill(0),
      results.map((row) => row.stderr + row.stdout).join('\n'))
    assert.equal(new Set(results.map((row) => row.inode)).size, 1,
      'every lock helper must flock the same first-created inode')
    assert.equal(JSON.parse(readFileSync(lock, 'utf8')).released, true)
  } finally { rmSync(root, { recursive: true, force: true }) }
})

await check('loss of the mutex helper cannot admit a second finalizer over a live owner', async function () {
  const fx = fixture()
  try {
    const archGate = join(fx.root, 'release-arch-gate')
    const first = runAsync(fx, [fx.stem, '--outcome-file', fx.draft], { FAKE_ARCH_GATE: archGate })
    // Reaching arch includes the full anchored outcome/components/tokens/ship/index
    // prefix.  Bound the wait independently from the short post-condition
    // polling default so slower CI hosts do not fail before the race begins.
    await waitUntil(() => marker(fx).phase === 'arch' && marker(fx).status === 'running', 'first finalizer to enter arch', 120000)
    await waitUntil(() => existsSync(archGate + '.ready'), 'first finalizer arch gate', 120000)
    const mutexPath = join(fx.root, 'orchestrator/.cache/tasks/finalizations/.mutex.json')
    await waitUntil(() => JSON.parse(readFileSync(mutexPath, 'utf8')).released === false, 'mutex helper ownership record')
    const helperPid = JSON.parse(readFileSync(mutexPath, 'utf8')).pid
    process.kill(helperPid, 'SIGKILL')

    const contender = run(fx, [fx.stem])
    // Keep the marker owner process live until the contender has made its
    // claim decision, then release the deterministic fixture gate regardless
    // of the contender verdict so the owner can observe helper loss and exit.
    writeFileSync(archGate, 'release\n')
    assert.equal(contender.status, 1, contender.stderr + contender.stdout)
    assert.match(contender.stderr, /FINALIZATION_OWNER_ACTIVE/)

    const firstResult = await first.done
    assert.equal(firstResult.status, 1, firstResult.stderr + firstResult.stdout)
    assert.match(firstResult.stderr, /MUTEX_OWNERSHIP_LOST/)
    assert.ok(existsSync(join(fx.root, 'orchestrator/.cache/tasks/finalizations', fx.stem + '.json')), 'recovery marker remains authoritative')

    const recovered = run(fx, [fx.stem])
    assert.equal(recovered.status, 0, recovered.stderr + recovered.stdout)
    assertComplete(fx)
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('asynchronous mutex-release stdin EPIPE is contained without weakening finalization ownership', function () {
  const fx = fixture()
  try {
    const helper = join(fx.root, 'scripts', 'broken-release-helper.mjs')
    writeFileSync(helper, `
import fs from 'node:fs'
import os from 'node:os'
import writerLeases from ${JSON.stringify(join(ROOT, 'orchestrator', 'tasks', 'writer-leases.cjs'))}
const lockPath = process.argv[2]
const invocationId = process.argv[3]
fs.writeFileSync(lockPath, JSON.stringify({
  version: 1, pid: process.pid, processStartId: writerLeases.captureProcessStartId(process.pid), hostname: os.hostname(), invocationId,
  startedAt: new Date().toISOString(), released: false
}) + '\\n')
// Close the read side before the parent sends its RELEASE frame, then remain
// alive so the write fails asynchronously with EPIPE rather than looking like
// an ordinary already-closed helper.
try { fs.closeSync(0) } catch {}
process.stdout.write('LOCKED\\n')
setInterval(() => {}, 1000)
`)
    const result = run(fx, [fx.stem, '--outcome-file', fx.draft], {
      FINALIZE_LOCK_PYTHON: process.execPath,
      FINALIZE_MUTEX_HELPER: helper
    })
    assert.equal(result.status, 0, result.stderr + result.stdout)
    assertComplete(fx)
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('unrelated staged, unstaged, and untracked work is byte-preserved', function () {
  const fx = fixture()
  try {
    const git = (args) => spawnSync('git', args, { cwd: fx.root, encoding: 'utf8' })
    assert.equal(git(['init', '-q']).status, 0)
    git(['config', 'user.email', 'fixture@example.test']); git(['config', 'user.name', 'Fixture'])
    writeFileSync(join(fx.root, 'user.txt'), 'base\n')
    git(['add', '.']); assert.equal(git(['commit', '-qm', 'fixture']).status, 0)
    writeFileSync(join(fx.root, 'user.txt'), 'staged\n'); git(['add', 'user.txt'])
    writeFileSync(join(fx.root, 'user.txt'), 'staged\nunstaged\n')
    writeFileSync(join(fx.root, 'untracked.bin'), Buffer.from([0, 1, 2, 255]))
    const beforeFile = readFileSync(join(fx.root, 'user.txt'))
    const beforeUntracked = readFileSync(join(fx.root, 'untracked.bin'))
    const beforeCached = git(['diff', '--cached', '--', 'user.txt']).stdout
    const beforeWork = git(['diff', '--', 'user.txt']).stdout
    const result = run(fx, [fx.stem, '--outcome-file', fx.draft])
    assert.equal(result.status, 0, result.stderr + result.stdout)
    assert.deepEqual(readFileSync(join(fx.root, 'user.txt')), beforeFile)
    assert.deepEqual(readFileSync(join(fx.root, 'untracked.bin')), beforeUntracked)
    assert.equal(git(['diff', '--cached', '--', 'user.txt']).stdout, beforeCached)
    assert.equal(git(['diff', '--', 'user.txt']).stdout, beforeWork)
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('hidden durable CAS, malformed lookalikes, and duplicate creation transactions block finalization', function () {
  const fx = fixture()
  try {
    const creations = join(fx.root, 'orchestrator/.cache/tasks/creations')
    const edits = join(fx.root, 'orchestrator/.cache/tasks/edits')
    mkdirSync(creations, { recursive: true })
    mkdirSync(edits, { recursive: true })
    const casName = '.durable-cas-' + ['a'.repeat(16), 'b'.repeat(16), 'c'.repeat(16)].join('-')
    mkdirSync(join(creations, casName))
    let result = run(fx, [fx.stem])
    assert.equal(result.status, 1)
    assert.match(result.stderr, /CREATION_MARKER_CAS_RECOVERY_REQUIRED/)
    rmSync(join(creations, casName), { recursive: true, force: true })

    writeFileSync(join(edits, '.durable-cas-malformed'), '')
    result = run(fx, [fx.stem])
    assert.equal(result.status, 1)
    assert.match(result.stderr, /EDIT_MARKER_CAS_NAME_UNSAFE/)
    unlinkSync(join(edits, '.durable-cas-malformed'))

    const a = '2'.repeat(64), b = '3'.repeat(64), tx = '4'.repeat(32)
    writeFileSync(join(creations, `${a}.json`), JSON.stringify(completedCreationReceipt(a, tx)) + '\n')
    writeFileSync(join(creations, `${b}.json`), JSON.stringify(completedCreationReceipt(b, tx)) + '\n')
    result = run(fx, [fx.stem])
    assert.equal(result.status, 1)
    assert.match(result.stderr, /CREATION_MARKER_INVALID.*transaction ids are not unique/)
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('finalization owner processStartId prevents live PID reuse from resurrecting a dead owner', function () {
  const fx = fixture()
  try {
    const crashed = run(fx, [fx.stem, '--outcome-file', fx.draft], { FINALIZE_FAILPOINT: 'after-intent:outcome' })
    assert.equal(crashed.status, 97, crashed.stderr + crashed.stdout)
    const path = join(fx.root, 'orchestrator/.cache/tasks/finalizations', fx.stem + '.json')
    const value = JSON.parse(readFileSync(path, 'utf8'))
    const live = writerLeases.captureProcessStartId(process.pid)
    value.owner.pid = process.pid
    value.owner.hostname = hostname()
    value.owner.processStartId = live.slice(0, -1) + (live.endsWith('0') ? '1' : '0')
    writeFileSync(path, JSON.stringify(value, null, 2) + '\n')
    const resumed = run(fx, [fx.stem])
    assert.equal(resumed.status, 0, resumed.stderr + resumed.stdout)
    assertComplete(fx)
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('decimal proofs above Number.MAX_SAFE_INTEGER remain lossless and do not alias live lock ownership', function () {
  const fx = fixture()
  try {
    const crashed = run(fx, [fx.stem, '--outcome-file', fx.draft], { FINALIZE_FAILPOINT: 'after-intent:outcome' })
    assert.equal(crashed.status, 97, crashed.stderr + crashed.stdout)
    const path = join(fx.root, 'orchestrator/.cache/tasks/finalizations', fx.stem + '.json')
    const value = JSON.parse(readFileSync(path, 'utf8'))
    value.source.lock.dev = '9007199254740993'
    value.source.lock.ino = '9007199254740995'
    writeFileSync(path, JSON.stringify(value, null, 2) + '\n')
    const resumed = run(fx, [fx.stem])
    assert.equal(resumed.status, 1)
    assert.match(resumed.stderr, /LOCK_OWNERSHIP_CONFLICT/)
    assert.doesNotMatch(resumed.stderr, /MARKER_INVALID/)
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('compare-at-delete preserves a foreign finalization-marker replacement', function () {
  const fx = fixture()
  try {
    const crashed = run(fx, [fx.stem, '--outcome-file', fx.draft], { FINALIZE_FAILPOINT: 'after-effect:cleanup' })
    assert.equal(crashed.status, 97, crashed.stderr + crashed.stdout)
    const target = join(fx.root, 'orchestrator/.cache/tasks/finalizations', fx.stem + '.json')
    const replacement = join(fx.root, 'foreign-marker.json')
    const sentinel = join(fx.root, 'delete-hook-fired')
    const foreign = '{"foreign":true}\n'
    writeFileSync(replacement, foreign)
    const resumed = run(fx, [fx.stem], {
      FINALIZE_FS_TEST_STAGE: 'before-remove', FINALIZE_FS_TEST_TARGET: target,
      FINALIZE_FS_TEST_REPLACEMENT: replacement, FINALIZE_FS_TEST_ROOT: fx.root,
      FINALIZE_FS_TEST_SENTINEL: sentinel,
    })
    assert.equal(resumed.status, 4, resumed.stderr + resumed.stdout)
    assert.match(resumed.stderr, /FINALIZATION_FILESYSTEM_CHANGED/)
    assert.equal(readFileSync(target, 'utf8'), foreign)
    assert.ok(existsSync(target + '.test-displaced'), 'owned marker generation remains preserved for recovery')
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

// ── tokens phase ─────────────────────────────────────────────────────────────
// Design-origin token tasks publish their authorized mapping inside the
// finalization transaction; these fixtures drive the real extraction and
// comparator over a json-tokens project inside the fixture root, with the
// design inventory supplied through the explicit fixture CLI argument
// fixture seam.

function tokenFixture(opts = {}) {
  const healthAt = opts.healthAt || new Date().toISOString()
  const capture = validObservedCapture({
    observations: opts.observations || [
      { providerName: 'palette/error/400', rawValue: '#EF5350', providerType: 'COLOR' }
    ]
  })
  const captureBytes = Buffer.from(JSON.stringify(capture))
  const batch = normalizeSourceCapture(capture, captureBytes, immutablePlan(capture))
  const observed = aggregateObservedTokens({
    scope: { fileKeyFingerprint: capture.source.fileKeyFingerprint, branchKey: capture.source.branchKey },
    batches: [batch],
    revision: 1
  })
  const frozenToken = observed.catalog.tokens[0]
  const coordinate = frozenToken.coordinates[0]
  const frozenContextKey = contextKey(coordinate.context)
  const tokenSourceId = `design:observed-token:${frozenToken.observedTokenKey}:token-implement`
  const bindingSnapshot = {
    schemaVersion: 1,
    observedCatalogHash: observed.catalog.semanticHash,
    projectAnalysisHash: 'sha256:' + '3'.repeat(64),
    adapterConfigHash: 'sha256:' + '4'.repeat(64),
    mappingRevision: 0,
    mappingHash: 'sha256:' + '5'.repeat(64),
    bindings: [],
    suggestions: [],
    conflicts: [],
    semanticHash: 'sha256:' + '0'.repeat(64)
  }
  bindingSnapshot.semanticHash = canonicalHash({
    schemaVersion: bindingSnapshot.schemaVersion,
    observedCatalogHash: bindingSnapshot.observedCatalogHash,
    projectAnalysisHash: bindingSnapshot.projectAnalysisHash,
    adapterConfigHash: bindingSnapshot.adapterConfigHash,
    mappingRevision: bindingSnapshot.mappingRevision,
    mappingHash: bindingSnapshot.mappingHash,
    bindings: bindingSnapshot.bindings,
    suggestions: bindingSnapshot.suggestions,
    conflicts: bindingSnapshot.conflicts
  })
  const binding = {
    schemaVersion: 2,
    sourceId: tokenSourceId,
    intent: 'implement',
    observedTokenKey: frozenToken.observedTokenKey,
    contextKey: frozenContextKey,
    catalogHash: observed.catalog.semanticHash,
    sourceIndexHash: observed.index.semanticHash,
    bindingSnapshotHash: bindingSnapshot.semanticHash,
    expectedKind: 'color',
    frozenValue: coordinate.values[0].value,
    intendedAdapterId: 'fixture-json',
    intendedSemanticPath: ['palette', 'error', '400'],
    intendedProjectMode: 'shared',
    findingId: 'tokf-' + '0'.repeat(24),
    comparisonSemanticHash: 'sha256:' + 'b'.repeat(64),
    mappingRevision: 0,
    ...(opts.binding || {}),
  }
  const bindingBytes = Buffer.from(JSON.stringify(binding, null, 2) + '\n')
  const task = [
    '# TASK 7 — Implement design token palette error 400',
    '',
    '## Source',
    '',
    '- Kind: figma',
    '- Type: design-finding',
    '- Ref: ' + tokenSourceId,
    '- Fingerprint: ' + tokenBindingContract.sha256(bindingBytes),
    '',
    '## Goal',
    '- Implement the bound design token in the fixture project.',
  ].join('\n') + '\n'
  const fx = fixture({ figma: true, task })
  mkdirSync(join(fx.root, 'design/tokens'), { recursive: true })
  writeFileSync(join(fx.root, 'design/tokens/tokens.json'),
    opts.projectTokensRaw !== undefined
      ? opts.projectTokensRaw
      : JSON.stringify(opts.projectTokens !== undefined ? opts.projectTokens : { palette: { error: { 400: { value: '#EF5350' } } } }, null, 2))
  writeFileSync(join(fx.root, 'orchestrator/figma/project-adapters.json'), JSON.stringify({
    schemaVersion: 2,
    adapters: [{
      id: 'fixture-json', kind: 'json-tokens', version: 2, enabled: true,
      capabilities: ['tokens'], platform: 'web', authority: 'handwritten',
      tokens: {
        roots: ['design/tokens'], include: ['**/*.json'], exclude: [], modes: ['shared'],
        authorities: { color: { contracts: ['palette'] } },
        contextMap: [{ when: capture.source.context, projectMode: 'shared' }],
        bindingRules: [],
      },
    }],
  }, null, 2))
  const bindingPath = join(fx.root, tokenBindingContract.bindingRelativePath(tokenSourceId))
  mkdirSync(dirname(bindingPath), { recursive: true })
  writeFileSync(bindingPath, bindingBytes)
  const catalogPath = join(fx.root, 'observed-token-catalog-fixture.json')
  const sourceIndexPath = join(fx.root, 'observed-token-source-index-fixture.json')
  const bindingSnapshotPath = join(fx.root, 'token-binding-snapshot-fixture.json')
  writeFileSync(catalogPath, JSON.stringify(observed.catalog, null, 2))
  writeFileSync(sourceIndexPath, JSON.stringify(observed.index, null, 2))
  writeFileSync(bindingSnapshotPath, JSON.stringify(bindingSnapshot, null, 2))
  const observedBatches = new Map(observed.shards.flatMap(({ shard }) =>
    shard.sources.map((batch) => [batch.sourceId, batch])))
  const health = materializeSourceHealth({
    sourceIndexHash: observed.index.semanticHash,
    healthRevision: 1,
    updates: observed.index.sources.map((source) => {
      const batch = observedBatches.get(source.sourceId)
      assert.ok(batch, `fixture source batch missing for ${source.sourceId}`)
      return {
        sourceId: source.sourceId,
        issuedSequenceHighWatermark: source.acceptedBatch.captureSequence,
        latestAttempt: {
          operationId: batch.captureOperationId,
          captureSequence: source.acceptedBatch.captureSequence,
          at: healthAt,
          outcome: 'published',
          evidenceHash: batch.captureEvidenceHash,
        },
      }
    }),
  })
  const healthWrapper = {
    schemaVersion: 1,
    createdAt: healthAt,
    index: health.index,
    shards: health.shards,
  }
  const healthBytes = Buffer.from(JSON.stringify(healthWrapper, null, 2) + '\n')
  const healthHash = `sha256:${createHash('sha256').update(healthBytes).digest('hex')}`
  const healthName = `health-1-${healthHash.slice('sha256:'.length)}.json`
  const healthRoot = join(fx.root, 'orchestrator/.cache/figma/token-source-health')
  mkdirSync(join(healthRoot, 'snapshots'), { recursive: true })
  writeFileSync(join(healthRoot, 'snapshots', healthName), healthBytes)
  const healthPointer = join(healthRoot, 'current.json')
  writeFileSync(healthPointer, JSON.stringify({
    schemaVersion: 1,
    snapshotFile: healthName,
    snapshotHash: healthHash,
    healthRevision: health.index.healthRevision,
    indexSemanticHash: health.index.semanticHash,
    updatedAt: healthAt,
  }, null, 2) + '\n')
  fx.fixtureArgs = [
    '--fixture-observed-token-catalog', catalogPath,
    '--fixture-observed-token-source-index', sourceIndexPath,
    '--fixture-token-binding-snapshot', bindingSnapshotPath
  ]
  fx.bindingPath = bindingPath
  fx.registryPath = join(fx.root, 'orchestrator/figma/token-mappings.json')
  fx.observedTokenKey = frozenToken.observedTokenKey
  fx.observedScope = observed.catalog.scope
  fx.healthPointer = healthPointer
  refreshIndex(fx)
  return fx
}

// Rebuild INDEX.json after fixture-side task/evidence writes so the
// finalizer's canonical pre-validation sees a fresh index.
function refreshIndex(fx) {
  const model = taskState.validateTaskState({ tasksDir: join(fx.root, 'orchestrator/tasks'), repoRoot: fx.root, checkIndex: false, includeRuntime: false })
  assert.equal(model.ok, true, JSON.stringify(model.findings))
  writeFileSync(join(fx.root, 'orchestrator/tasks/INDEX.json'), JSON.stringify(taskState.deriveIndex(model._model, '2026-01-01T00:00:00Z'), null, 2) + '\n')
}

function readTokenRegistryFile(fx) {
  return JSON.parse(readFileSync(fx.registryPath, 'utf8'))
}

await check('tokens phase publishes the bound mapping atomically inside the finalization', function () {
  const fx = tokenFixture()
  try {
    const result = run(fx, [fx.stem, '--outcome-file', fx.draft])
    assert.equal(result.status, 0, result.stderr + result.stdout)
    assertComplete(fx)
    const registry = readTokenRegistryFile(fx)
    assert.equal(registry.revision, 1)
    assert.equal(registry.mappings.length, 1)
    const mapping = registry.mappings[0]
    assert.equal(mapping.observedTokenKey, fx.observedTokenKey)
    assert.deepEqual(mapping.projectTokenIds, ['fixture-json:palette.error.400'])
    assert.deepEqual(mapping.contextSelector, { locale: 'default', platform: 'shared', theme: 'light' })
    assert.equal(mapping.state, 'active')
    assert.equal(mapping.provenance.kind, 'design-task-finalization')
    assert.equal(mapping.provenance.taskStem, fx.stem)
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('design-origin token finalization blocks on corrupt Source Health', function () {
  const fx = tokenFixture()
  try {
    writeFileSync(fx.healthPointer, '{}\n')
    const result = run(fx, [fx.stem, '--outcome-file', fx.draft])
    assert.equal(result.status, 1, result.stderr + result.stdout)
    assert.match(result.stderr, /TOKEN_SOURCE_HEALTH_UNAVAILABLE/)
    assert.equal(existsSync(fx.registryPath), false)
    assert.ok(existsSync(join(fx.root, 'orchestrator/.cache/tasks/locks', fx.stem + '.json')))
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('design-origin token finalization blocks on stale Source Health', function () {
  const fx = tokenFixture({ healthAt: new Date(Date.now() - 48 * 60 * 60 * 1000).toISOString() })
  try {
    const result = run(fx, [fx.stem, '--outcome-file', fx.draft])
    assert.equal(result.status, 1, result.stderr + result.stdout)
    assert.match(result.stderr, /TOKEN_SOURCE_REFRESH_FAILED/)
    assert.equal(existsSync(fx.registryPath), false)
    assert.ok(existsSync(join(fx.root, 'orchestrator/.cache/tasks/locks', fx.stem + '.json')))
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('tokens crash before mapping publish leaves no registry bytes and resumes to exactly one mapping', function () {
  const fx = tokenFixture()
  try {
    const crashed = run(fx, [fx.stem, '--outcome-file', fx.draft], { FINALIZE_FAILPOINT: 'before-token-mapping-publish:tokens' })
    assert.equal(crashed.status, 97, crashed.stderr + crashed.stdout)
    assert.equal(existsSync(fx.registryPath), false, 'no canonical registry bytes may exist before the publish point')
    const resumed = run(fx, [fx.stem])
    assert.equal(resumed.status, 0, resumed.stderr + resumed.stdout)
    assertComplete(fx)
    const registry = readTokenRegistryFile(fx)
    assert.equal(registry.revision, 1)
    assert.equal(registry.mappings.length, 1)
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('tokens crash after mapping publish resumes idempotently without a duplicate mapping', function () {
  const fx = tokenFixture()
  try {
    const crashed = run(fx, [fx.stem, '--outcome-file', fx.draft], { FINALIZE_FAILPOINT: 'after-token-mapping-publish:tokens' })
    assert.equal(crashed.status, 97, crashed.stderr + crashed.stdout)
    assert.equal(readTokenRegistryFile(fx).revision, 1, 'registry bytes are already published at the crash point')
    const resumed = run(fx, [fx.stem])
    assert.equal(resumed.status, 0, resumed.stderr + resumed.stdout)
    assertComplete(fx)
    const registry = readTokenRegistryFile(fx)
    assert.equal(registry.revision, 1, 'idempotent resume must reconcile, never re-publish')
    assert.equal(registry.mappings.length, 1)
    assert.equal(registry.mappings[0].provenance.taskStem, fx.stem)
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('a changed observed value blocks the tokens phase as stale and keeps the lock', function () {
  const fx = tokenFixture({
    observations: [{ providerName: 'palette/error/400', rawValue: '#000000', providerType: 'COLOR' }],
    binding: { frozenValue: { space: 'srgb', hex: '#EF5350FF' } }
  })
  try {
    const result = run(fx, [fx.stem, '--outcome-file', fx.draft])
    assert.equal(result.status, 1, result.stderr + result.stdout)
    assert.match(result.stderr, /TOKEN_TASK_BINDING_STALE/)
    assert.equal(existsSync(fx.registryPath), false)
    assert.ok(existsSync(join(fx.root, 'orchestrator/.cache/tasks/locks', fx.stem + '.json')))
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('binding evidence that does not re-hash to the Source fingerprint is refused', function () {
  const fx = tokenFixture()
  try {
    writeFileSync(fx.bindingPath, readFileSync(fx.bindingPath, 'utf8') + '\n')
    const result = run(fx, [fx.stem, '--outcome-file', fx.draft])
    assert.equal(result.status, 1, result.stderr + result.stdout)
    assert.match(result.stderr, /TOKEN_TASK_BINDING_INVALID/)
    assert.equal(existsSync(fx.registryPath), false)
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('a missing intended project token is ambiguous, never auto-created', function () {
  const fx = tokenFixture({ projectTokens: { palette: { error: { 500: { value: '#AA0000' } } } } })
  try {
    const result = run(fx, [fx.stem, '--outcome-file', fx.draft])
    assert.equal(result.status, 1, result.stderr + result.stdout)
    assert.match(result.stderr, /TOKEN_TASK_BINDING_AMBIGUOUS/)
    assert.equal(existsSync(fx.registryPath), false)
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('a moved mapping registry revision blocks the binding as stale', function () {
  const fx = tokenFixture()
  try {
    writeFileSync(fx.registryPath, JSON.stringify({
      schemaVersion: 2, revision: 3, scope: fx.observedScope,
      operationReceipts: [], mappings: [], dispositions: [],
    }, null, 2) + '\n')
    const result = run(fx, [fx.stem, '--outcome-file', fx.draft])
    assert.equal(result.status, 1, result.stderr + result.stdout)
    assert.match(result.stderr, /TOKEN_TASK_BINDING_STALE/)
    assert.equal(readTokenRegistryFile(fx).revision, 3, 'a stale binding must never mutate the registry')
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('generic tasks pass the token touch-gate leniently without a prior complete analysis', function () {
  const fx = tokenFixture()
  try {
    // Make the task generic (manual source) while adapters stay configured
    // and the token scope is broken: with no previously published complete
    // analysis the gate must stay lenient.
    writeFileSync(join(fx.root, 'orchestrator/tasks/todo', fx.stem + '.md'),
      canonicalTodo('# TASK 7 — Generic change beside broken tokens\n'))
    refreshIndex(fx)
    writeTestEvidence(fx.root, fx.stem)
    writeFileSync(join(fx.root, 'design/tokens/tokens.json'), '{ broken json')
    const result = run(fx, [fx.stem, '--outcome-file', fx.draft])
    assert.equal(result.status, 0, result.stderr + result.stdout)
    assertComplete(fx)
    assert.equal(existsSync(fx.registryPath), false, 'a generic task never auto-maps')
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('generic tasks are blocked when they break a previously complete token analysis', function () {
  const fx = tokenFixture()
  try {
    writeFileSync(join(fx.root, 'orchestrator/tasks/todo', fx.stem + '.md'),
      canonicalTodo('# TASK 7 — Generic change that breaks token extraction\n'))
    refreshIndex(fx)
    writeTestEvidence(fx.root, fx.stem)
    writeFileSync(join(fx.root, 'design/tokens/tokens.json'), '{ broken json')
    const analysisPath = join(fx.root, 'published-analysis-index.json')
    writeFileSync(analysisPath, JSON.stringify({
      schemaVersion: 2, configHash: 'sha256:' + 'd'.repeat(64),
      adapters: [{ adapterId: 'fixture-json', role: 'project-token-inventory:fixture-json',
        inventoryHash: 'sha256:' + 'e'.repeat(64), scopeFingerprint: 'sha256:' + 'b'.repeat(64), complete: true }],
      complete: true
    }))
    const result = run(fx, [fx.stem, '--outcome-file', fx.draft, '--fixture-token-analysis-index', analysisPath])
    assert.equal(result.status, 1, result.stderr + result.stdout)
    assert.match(result.stderr, /TOKEN_EXTRACTION_REGRESSION/)
    assert.ok(existsSync(join(fx.root, 'orchestrator/.cache/tasks/locks', fx.stem + '.json')))
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

// ── components phase ─────────────────────────────────────────────────────────
// Design-origin component tasks publish their binding-authorized mapping inside
// the finalization transaction; these fixtures drive the real component
// extraction (component-manifest adapter) and comparator over a manifest
// project inside the fixture root, with the design component inventory
// supplied through the explicit component fixture CLI argument.

function componentCaptureFixture() {
  return {
    schemaVersion: 2,
    provider: 'figma',
    providerIdentity: {
      fileKeyFingerprint: 'sha256:' + 'a'.repeat(64),
      branchKey: 'none',
      libraryOriginPolicy: 'local-authoritative',
    },
    scope: { kind: 'all-pages' },
    pages: [{ pageId: '1:1', name: 'Components' }],
    entities: [
      {
        nodeId: '10:1', pageId: '1:1', kind: 'component-set', name: 'AppButton', idQuality: 'stable',
        properties: [
          { propertyId: 'p:size', name: 'Size', type: 'variant', idQuality: 'stable', options: ['Small', 'Large'], defaultValue: 'Small' },
          { propertyId: 'p:enabled', name: 'Enabled', type: 'boolean', idQuality: 'stable', defaultValue: true },
          { propertyId: 'p:label', name: 'Label', type: 'text', idQuality: 'stable' },
          { propertyId: 'p:icon', name: 'LeadingIcon', type: 'instance-swap', idQuality: 'stable', swapTargets: [{ targetNodeId: '20:1' }] },
        ],
        variants: [
          { nodeId: '10:2', name: 'Size=Small', assignments: { 'p:size': 'Small' }, isDefault: true },
          { nodeId: '10:3', name: 'Size=Large', assignments: { 'p:size': 'Large' } },
        ],
        expectedVariantCount: 2,
        nestedRefs: [{ targetNodeId: '20:1', swappable: true, viaPropertyId: 'p:icon' }],
        boundVariables: [{
          observedTokenKey: 'otk:sha256:' + '1'.repeat(64),
          contextKey: '{"locale":"und","platform":"web","theme":"light"}',
          sourceId: 'otsrc:sha256:' + '2'.repeat(64),
          providerName: 'AppButton/Background',
          field: 'fills'
        }],
      },
      {
        nodeId: '20:1', pageId: '1:1', kind: 'component', name: 'AppIcon', idQuality: 'stable',
        properties: [], variants: [], expectedVariantCount: 0, nestedRefs: [], boundVariables: [],
      },
    ],
    visual: [],
    witness: {
      startedAt: '2026-01-01T00:00:00.000Z', finishedAt: '2026-01-01T00:00:05.000Z',
      providerRevisionBefore: 'r1', providerRevisionAfter: 'r1',
      consistency: 'proven', completeness: 'complete',
      requestedPageIds: ['1:1'], readPageIds: ['1:1'],
      expectedEntityCount: 2, readEntityCount: 2,
      truncated: false, permissionDegraded: false, limitsHit: [],
    },
  }
}

function componentInventoryFrom(capture) {
  const bytes = Buffer.from(JSON.stringify(capture))
  return normalizeComponentCapture(capture, 'sha256:' + createHash('sha256').update(bytes).digest('hex'))
}

const componentManifestFixture = {
  schemaVersion: 2,
  components: [
    {
      name: 'AppButton', symbol: 'ui/button', visibility: 'public',
      props: [
        { name: 'size', kind: 'enum', values: ['Small', 'Large'], default: 'Small' },
        { name: 'enabled', kind: 'boolean', default: true },
        { name: 'label', kind: 'text', required: true },
        { name: 'leadingIcon', kind: 'content' },
      ],
      uses: { components: ['ui/icon'] },
    },
    { name: 'AppIcon', symbol: 'ui/icon', visibility: 'public', props: [] },
  ],
}

function componentFixture(opts = {}) {
  const frozenInventory = componentInventoryFrom(componentCaptureFixture())
  const frozenComponent = frozenInventory.components.find((component) => component.kind === 'component-set')
  const sourceId = `design:component:${frozenComponent.designComponentId}:component-implement`
  const binding = {
    schemaVersion: 2,
    sourceId,
    intent: 'implement',
    designComponentId: frozenComponent.designComponentId,
    designScopeId: frozenInventory.scopeId,
    designGenerationId: 'gen-' + 'a'.repeat(32),
    designInventoryHash: 'sha256:' + 'a'.repeat(64),
    expectedKind: 'component-set',
    frozenStructuralHash: frozenComponent.structuralHash,
    frozenSourceHash: frozenComponent.sourceHash,
    frozenSpec: {
      name: frozenComponent.name,
      properties: frozenComponent.properties.map((property) => ({ propertyId: property.propertyId, name: property.name, type: property.type })),
      variants: frozenComponent.variants.map((variant) => ({ variantId: variant.variantId, name: variant.name })),
      slots: frozenComponent.semanticSlots.map((slot) => ({ slotId: slot.slotId, kind: slot.kind, name: slot.name })),
      tokenRefs: frozenComponent.tokenRefs.map((ref) => ({
        observedTokenKey: ref.observedTokenKey,
        contextKey: ref.contextKey,
        sourceId: ref.sourceId,
        providerName: ref.providerName,
        field: ref.field
      })),
    },
    intendedAdapterId: 'fixture-manifest',
    intendedPlatform: 'web',
    intendedRelation: 'direct',
    intendedProjectSymbol: 'ui/button',
    intendedPropertyMappings: [
      { designPropertyId: 'p:size', projectPropertyId: 'param:size', valueMap: { Small: 'Small', Large: 'Large' } },
      { designPropertyId: 'p:enabled', projectPropertyId: 'param:enabled', valueMap: { true: 'true', false: 'false' } },
    ],
    findingId: 'cmpf-' + '0'.repeat(24),
    comparisonSemanticHash: 'sha256:' + 'b'.repeat(64),
    mappingRevision: 0,
    ...(opts.binding || {}),
  }
  const bindingBytes = Buffer.from(JSON.stringify(binding, null, 2) + '\n')
  const task = [
    '# TASK 7 — Implement design component AppButton',
    '',
    '## Source',
    '',
    '- Kind: figma',
    '- Type: design-finding',
    '- Ref: ' + binding.sourceId,
    '- Fingerprint: ' + componentBindingContract.sha256(bindingBytes),
    '',
    '## Goal',
    '- Implement the bound design component in the fixture project.',
  ].join('\n') + '\n'
  const fx = fixture({ figma: true, task })
  mkdirSync(join(fx.root, 'design/components'), { recursive: true })
  writeFileSync(join(fx.root, 'design/components/manifest.json'),
    opts.manifestRaw !== undefined
      ? opts.manifestRaw
      : JSON.stringify(opts.manifest !== undefined ? opts.manifest : componentManifestFixture, null, 2))
  writeFileSync(join(fx.root, 'orchestrator/figma/project-adapters.json'), JSON.stringify({
    schemaVersion: 2,
    adapters: [{
      id: 'fixture-manifest', kind: 'component-manifest', version: 2, enabled: true,
      capabilities: ['components'], platform: 'web', authority: 'handwritten',
      components: { roots: ['design/components'], include: ['**/*.json'], exclude: [], visibility: ['public'] },
    }],
  }, null, 2))
  const bindingPath = join(fx.root, componentBindingContract.bindingRelativePath(binding.sourceId))
  mkdirSync(dirname(bindingPath), { recursive: true })
  writeFileSync(bindingPath, bindingBytes)
  const liveCapture = componentCaptureFixture()
  if (opts.mutateCapture) opts.mutateCapture(liveCapture)
  const liveInventory = componentInventoryFrom(liveCapture)
  const inventoryPath = join(fx.root, 'design-component-inventory-fixture.json')
  writeFileSync(inventoryPath, JSON.stringify(liveInventory, null, 2))
  fx.fixtureArgs = ['--fixture-component-inventory', inventoryPath]
  fx.bindingPath = bindingPath
  fx.componentRegistryPath = join(fx.root, 'orchestrator/figma/component-mappings.json')
  fx.designComponentId = frozenComponent.designComponentId
  fx.designScopeId = frozenInventory.scopeId
  refreshIndex(fx)
  return fx
}

function readComponentRegistryFile(fx) {
  return JSON.parse(readFileSync(fx.componentRegistryPath, 'utf8'))
}

await check('components phase publishes the bound mapping atomically inside the finalization', function () {
  const fx = componentFixture()
  try {
    const result = run(fx, [fx.stem, '--outcome-file', fx.draft])
    assert.equal(result.status, 0, result.stderr + result.stdout)
    assertComplete(fx)
    const registry = readComponentRegistryFile(fx)
    assert.equal(registry.revision, 1)
    assert.equal(registry.mappings.length, 1)
    const mapping = registry.mappings[0]
    assert.equal(mapping.designComponentId, fx.designComponentId)
    assert.equal(mapping.state, 'active')
    assert.equal(mapping.implementations.length, 1)
    assert.equal(mapping.implementations[0].adapterId, 'fixture-manifest')
    assert.equal(mapping.implementations[0].platform, 'web')
    assert.equal(mapping.implementations[0].relation, 'direct')
    assert.deepEqual(mapping.implementations[0].projectComponentIds, ['fixture-manifest:symbol:ui/button'])
    assert.equal(mapping.propertyMappings.length, 2)
    assert.ok(mapping.slotMappings.length >= 2, 'exact-name slot pairs (label + leadingIcon) must be auto-frozen')
    assert.equal(mapping.provenance.kind, 'task-binding')
    assert.equal(mapping.provenance.taskStem, fx.stem)
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('components crash before mapping publish leaves no registry bytes and resumes to exactly one mapping', function () {
  const fx = componentFixture()
  try {
    const crashed = run(fx, [fx.stem, '--outcome-file', fx.draft], { FINALIZE_FAILPOINT: 'before-component-mapping-publish:components' })
    assert.equal(crashed.status, 97, crashed.stderr + crashed.stdout)
    assert.equal(existsSync(fx.componentRegistryPath), false, 'no canonical registry bytes may exist before the publish point')
    const resumed = run(fx, [fx.stem])
    assert.equal(resumed.status, 0, resumed.stderr + resumed.stdout)
    assertComplete(fx)
    const registry = readComponentRegistryFile(fx)
    assert.equal(registry.revision, 1)
    assert.equal(registry.mappings.length, 1)
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('components crash after mapping publish resumes idempotently without a duplicate mapping', function () {
  const fx = componentFixture()
  try {
    const crashed = run(fx, [fx.stem, '--outcome-file', fx.draft], { FINALIZE_FAILPOINT: 'after-component-mapping-publish:components' })
    assert.equal(crashed.status, 97, crashed.stderr + crashed.stdout)
    assert.equal(readComponentRegistryFile(fx).revision, 1, 'registry bytes are already published at the crash point')
    const resumed = run(fx, [fx.stem])
    assert.equal(resumed.status, 0, resumed.stderr + resumed.stdout)
    assertComplete(fx)
    const registry = readComponentRegistryFile(fx)
    assert.equal(registry.revision, 1, 'idempotent resume must reconcile, never re-publish')
    assert.equal(registry.mappings.length, 1)
    assert.equal(registry.mappings[0].provenance.taskStem, fx.stem)
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('a changed design component structure blocks the components phase as stale and keeps the lock', function () {
  const fx = componentFixture({
    mutateCapture(capture) {
      capture.entities[0].properties[0].options = ['Small', 'Medium', 'Large']
    },
  })
  try {
    const result = run(fx, [fx.stem, '--outcome-file', fx.draft])
    assert.equal(result.status, 1, result.stderr + result.stdout)
    assert.match(result.stderr, /COMPONENT_TASK_BINDING_STALE/)
    assert.equal(existsSync(fx.componentRegistryPath), false)
    assert.ok(existsSync(join(fx.root, 'orchestrator/.cache/tasks/locks', fx.stem + '.json')))
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('component binding evidence that does not re-hash to the Source fingerprint is refused', function () {
  const fx = componentFixture()
  try {
    writeFileSync(fx.bindingPath, readFileSync(fx.bindingPath, 'utf8') + '\n')
    const result = run(fx, [fx.stem, '--outcome-file', fx.draft])
    assert.equal(result.status, 1, result.stderr + result.stdout)
    assert.match(result.stderr, /COMPONENT_TASK_BINDING_INVALID/)
    assert.equal(existsSync(fx.componentRegistryPath), false)
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('a missing intended project component symbol is ambiguous, never auto-created', function () {
  const fx = componentFixture({
    manifest: {
      schemaVersion: 2,
      components: [{ name: 'AppIcon', symbol: 'ui/icon', visibility: 'public', props: [] }],
    },
  })
  try {
    const result = run(fx, [fx.stem, '--outcome-file', fx.draft])
    assert.equal(result.status, 1, result.stderr + result.stdout)
    assert.match(result.stderr, /COMPONENT_TASK_BINDING_AMBIGUOUS/)
    assert.equal(existsSync(fx.componentRegistryPath), false)
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('a moved component mapping registry revision blocks the binding as stale', function () {
  const fx = componentFixture()
  try {
    writeFileSync(fx.componentRegistryPath, JSON.stringify({
      schemaVersion: 2, revision: 3, designScopeId: fx.designScopeId,
      mappings: [], dispositions: [],
    }, null, 2) + '\n')
    const result = run(fx, [fx.stem, '--outcome-file', fx.draft])
    assert.equal(result.status, 1, result.stderr + result.stdout)
    assert.match(result.stderr, /COMPONENT_TASK_BINDING_STALE/)
    assert.equal(readComponentRegistryFile(fx).revision, 3, 'a stale binding must never mutate the registry')
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('component mapping registry tampering after publication is caught before unlock', function () {
  const fx = componentFixture()
  try {
    const crashed = run(fx, [fx.stem, '--outcome-file', fx.draft], { FINALIZE_FAILPOINT: 'after-effect:verify' })
    assert.equal(crashed.status, 97, crashed.stderr + crashed.stdout)
    const registry = readComponentRegistryFile(fx)
    registry.mappings[0].implementations[0].projectComponentIds = ['fixture-manifest:symbol:ui/forged']
    writeFileSync(fx.componentRegistryPath, JSON.stringify(registry, null, 2) + '\n')
    const resumed = run(fx, [fx.stem])
    assert.equal(resumed.status, 1, resumed.stderr + resumed.stdout)
    assert.match(resumed.stderr, /COMPONENT_MAPPING_PUBLICATION_RECOVERY_REQUIRED/)
    assert.ok(existsSync(join(fx.root, 'orchestrator/.cache/tasks/locks', fx.stem + '.json')))
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('generic tasks pass the component touch-gate leniently without a prior complete analysis', function () {
  const fx = componentFixture()
  try {
    writeFileSync(join(fx.root, 'orchestrator/tasks/todo', fx.stem + '.md'),
      canonicalTodo('# TASK 7 — Generic change beside broken components\n'))
    refreshIndex(fx)
    writeTestEvidence(fx.root, fx.stem)
    writeFileSync(join(fx.root, 'design/components/manifest.json'), '{ broken json')
    const result = run(fx, [fx.stem, '--outcome-file', fx.draft])
    assert.equal(result.status, 0, result.stderr + result.stdout)
    assertComplete(fx)
    assert.equal(existsSync(fx.componentRegistryPath), false, 'a generic task never auto-maps')
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

await check('generic tasks are blocked when they break a previously complete component analysis', function () {
  const fx = componentFixture()
  try {
    writeFileSync(join(fx.root, 'orchestrator/tasks/todo', fx.stem + '.md'),
      canonicalTodo('# TASK 7 — Generic change that breaks component extraction\n'))
    refreshIndex(fx)
    writeTestEvidence(fx.root, fx.stem)
    const configState = loadAdapterConfig({ projectRoot: fx.root })
    const extracted = extractProjectComponents({ projectRoot: fx.root, config: configState.config,
      configHash: configState.componentConfigHash })
    const analysisDir = join(fx.root, 'published-component-analysis')
    mkdirSync(analysisDir, { recursive: true })
    for (const inventory of extracted.inventories) {
      writeFileSync(join(analysisDir, `project-inventory-${inventory.adapterId}.json`), JSON.stringify(inventory, null, 2))
    }
    const analysisPath = join(fx.root, 'published-component-analysis-index.json')
    writeFileSync(analysisPath, JSON.stringify(extracted.index, null, 2))
    writeFileSync(join(fx.root, 'design/components/manifest.json'), '{ broken json')
    const result = run(fx, [fx.stem, '--outcome-file', fx.draft,
      '--fixture-component-analysis-index', analysisPath, '--fixture-component-analysis-dir', analysisDir])
    assert.equal(result.status, 1, result.stderr + result.stdout)
    assert.match(result.stderr, /COMPONENT_EXTRACTION_REGRESSION/)
    assert.ok(existsSync(join(fx.root, 'orchestrator/.cache/tasks/locks', fx.stem + '.json')))
  } finally { rmSync(fx.root, { recursive: true, force: true }) }
})

console.log(`\nfinalize-task: ${checks} checks passed`)
