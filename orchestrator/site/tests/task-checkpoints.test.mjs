import assert from 'node:assert/strict'
import { mkdtempSync, readFileSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join, resolve } from 'node:path'
import { spawnSync } from 'node:child_process'
import { createRequire } from 'node:module'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'

const require = createRequire(import.meta.url)
const REPO = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..', '..')
const contract = require('../../tasks/task-checkpoint-contract.cjs')
const schema = JSON.parse(readFileSync(new URL('../../tasks/task-checkpoint.schema.json', import.meta.url), 'utf8'))

const STEM = 'TASK_71_checkpoint_contract'
const HASH_A = 'sha256:' + 'a'.repeat(64)
const HASH_B = 'sha256:' + 'b'.repeat(64)
const EXECUTION_PIN = Object.freeze({
  worktreeId: 'wt-' + '1'.repeat(32),
  baseCommit: '1'.repeat(40),
  baseTree: '2'.repeat(40),
  executionTree: '3'.repeat(40),
  targetRef: 'refs/heads/main',
  targetCommit: '1'.repeat(40),
})

function sealed(patch = {}) {
  return contract.seal({
    schemaVersion: contract.SCHEMA_VERSION,
    checkpointId: 'cp-' + '1'.repeat(32),
    stem: STEM,
    runId: 'run-71',
    phase: 'review',
    attempt: 1,
    status: 'failed',
    createdAt: '2026-07-27T10:00:00Z',
    taskState: 'todo',
    taskSourceRevision: HASH_A,
    projectSourceRevision: HASH_B,
    configHash: 'sha256:' + 'c'.repeat(64),
    dependencySnapshotHash: 'sha256:' + 'd'.repeat(64),
    inputFingerprint: 'sha256:' + 'e'.repeat(64),
    testPolicyHash: 'sha256:' + 'f'.repeat(64),
    outputReceiptIds: [],
    priorPhaseReceiptIds: [],
    failureCode: 'review-failed',
    executionPin: EXECUTION_PIN,
    retryPolicy: { kind: 'retry-phase', safePhase: 'review', reasonCode: 'review-failed' },
    checkpointHash: 'sha256:' + '0'.repeat(64),
    ...patch,
  })
}

test('checkpoint schema and semantic contract are exact, hashed and phase-safe', () => {
  assert.equal(schema.additionalProperties, false)
  assert.equal(schema.properties.schemaVersion.const, 1)
  assert.ok(schema.required.includes('testPolicyHash'), 'the current protocol binds the machine test-policy hash')
  assert.equal(schema.properties.executionPin.type, 'object',
    'a run checkpoint must never carry an execution-pin exemption')
  assert.ok(schema.properties.phase.enum.includes('tests'))
  assert.ok(schema.$defs.phase.enum.includes('tests'),
    'retryPolicy.safePhase must accept the activated tests phase too')
  assert.equal(schema.properties.attempt.maximum, 99)
  const value = sealed()
  assert.equal(contract.validate(value, STEM), null)
  assert.match(value.checkpointHash, /^sha256:[a-f0-9]{64}$/)
  assert.match(contract.validate({ ...value, unknown: true }, STEM), /fields/)
  assert.match(contract.validate({ ...value, checkpointHash: HASH_A }, STEM), /hash/)
  const incomplete = { ...value }
  delete incomplete.testPolicyHash
  assert.match(contract.validate(incomplete, STEM), /fields/)
  assert.match(contract.validate({ ...value, schemaVersion: 2 }, STEM), /schemaVersion/)
  assert.throws(() => sealed({ executionPin: null }), /execution pin/)
  // `tests` is exact-retryable (diagnostic/BLOCKED only) and restarting PAST
  // it demands the sealed PASS summary receipt among the priors.
  assert.equal(contract.validate(sealed({
    phase: 'tests', status: 'blocked', failureCode: 'phase-blocked',
    retryPolicy: { kind: 'retry-phase', safePhase: 'tests', reasonCode: 'phase-blocked' },
  }), STEM), null)
  assert.throws(() => sealed({
    phase: 'review',
    retryPolicy: { kind: 'restart-from-phase', safePhase: 'assemble-gate', reasonCode: 'phase-failed' },
    failureCode: 'phase-failed',
    priorPhaseReceiptIds: ['action:one'],
  }), /test-summary/)
  assert.equal(contract.validate(sealed({
    phase: 'review',
    retryPolicy: { kind: 'restart-from-phase', safePhase: 'assemble-gate', reasonCode: 'phase-failed' },
    failureCode: 'phase-failed',
    priorPhaseReceiptIds: ['test-summary:sha256:' + 'a'.repeat(64)],
  }), STEM), null)
  assert.throws(() => sealed({
    phase: 'builders',
    retryPolicy: { kind: 'retry-phase', safePhase: 'builders', reasonCode: 'phase-failed' },
    failureCode: 'phase-failed',
  }), /not eligible/)
  assert.throws(() => sealed({
    phase: 'review',
    status: 'completed',
    failureCode: null,
    retryPolicy: { kind: 'retry-phase', safePhase: 'review', reasonCode: 'manual-retry' },
  }), /failed or blocked/)
  assert.throws(() => sealed({
    retryPolicy: { kind: 'restart-from-phase', safePhase: 'validators', reasonCode: 'review-failed' },
  }), /prior phase receipt/)
  assert.throws(() => sealed({
    phase: 'planner',
    status: 'completed',
    failureCode: null,
    retryPolicy: { kind: 'resume-run', safePhase: 'review', reasonCode: 'review-failed' },
  }), /complete planner checkpoint/)
  assert.throws(() => sealed({
    phase: 'planner',
    status: 'completed',
    failureCode: null,
    retryPolicy: { kind: 'resume-run', safePhase: 'planner', reasonCode: 'review-failed' },
  }), /planner input receipt/)
  assert.throws(() => sealed({
    phase: 'ship', status: 'completed', failureCode: null,
    outputReceiptIds: [], priorPhaseReceiptIds: [],
    retryPolicy: { kind: 'restart-task', safePhase: null, reasonCode: 'manual-retry' },
  }), /test-summary/)
})

test('checkpoint producer exposes no migration or cleanup command', () => {
  const producer = join(REPO, 'orchestrator', 'tasks', 'task-checkpoint.mjs')
  const source = readFileSync(producer, 'utf8')
  assert.doesNotMatch(source, /auditV1|audit-v1|migrat|convert|cleanup/i)
  const result = spawnSync(process.execPath, [producer, 'audit-v1'], {
    cwd: REPO,
    encoding: 'utf8',
  })
  assert.equal(result.status, 2)
  assert.match(result.stderr, /^usage:/)
})

test('checkpoint storage, freshness, retry preview and one-use confirmation fail closed', () => {
  const root = mkdtempSync(join(tmpdir(), 'task-checkpoint-test-'))
  const modulePath = join(REPO, 'orchestrator', 'site', 'server', 'task-checkpoints.js')
  const script = `
    const assert = require('node:assert/strict')
    const fs = require('node:fs')
    const path = require('node:path')
    const checkpoints = require(${JSON.stringify(modulePath)})
    const checkpointContract = require(${JSON.stringify(join(REPO, 'orchestrator', 'tasks', 'task-checkpoint-contract.cjs'))})
    const stem = ${JSON.stringify(STEM)}
    const taskSnapshot = { state: 'todo', sourceRevision: ${JSON.stringify(HASH_A)}, dependencies: [] }
    const projectRevision = { available: true, revision: ${JSON.stringify(HASH_B)}, limitations: [] }
    const configHash = 'sha256:' + 'c'.repeat(64)
    const executionPin = { worktreeId: 'wt-' + '1'.repeat(32),
      baseCommit: '1'.repeat(40), baseTree: '2'.repeat(40), executionTree: '3'.repeat(40),
      targetRef: 'refs/heads/main', targetCommit: '1'.repeat(40) }
    const common = { taskSnapshot, projectRevision, configHash, activeRunId: 'run-71',
      receiptVerifier: () => true, executionPin }

    const created = checkpoints.create(stem, {
      runId: 'run-71',
      phase: 'security-review',
      attempt: 2,
      status: 'failed',
      outputReceiptIds: [],
      priorPhaseReceiptIds: ['phase-receipt-1'],
      failureCode: 'review-failed',
      retryPolicy: { kind: 'restart-from-phase', safePhase: 'validators', reasonCode: 'review-failed' }
    }, common)
    assert.equal(created.ok, true)
    assert.equal(created.idempotentReplay, false)
    const value = checkpoints.read(stem, created.checkpoint.checkpointId)
    assert.equal(value.checkpointHash, created.checkpoint.checkpointHash)
    assert.equal(checkpoints.summary(stem).count, 1)
    assert.match(checkpoints.summary(stem).revision, /^sha256:[a-f0-9]{64}$/)
    assert.equal(checkpoints.freshness(value, common).current, true)
    assert.equal(checkpoints.freshness(value, { ...common, activeRunId: 'foreign-run' }).reasonCode,
      'run-owner-changed', 'checkpoint freshness must keep proving the exact active task-lock generation')

    const checkpointPath = path.join(process.env.ORCHESTRATOR_CHECKPOINTS_DIR, stem,
      value.checkpointId + '.json')
    const currentBytes = fs.readFileSync(checkpointPath)
    const indexPath = path.join(process.env.ORCHESTRATOR_CHECKPOINTS_DIR, stem, 'index.json')
    const currentIndexBytes = fs.readFileSync(indexPath)
    const substituted = checkpointContract.seal({ ...value,
      checkpointId: 'cp-' + '2'.repeat(32), checkpointHash: 'sha256:' + '0'.repeat(64) })
    fs.writeFileSync(checkpointPath, checkpointContract.canonical(substituted) + '\\n')
    const substitutedIndex = JSON.parse(currentIndexBytes)
    substitutedIndex.entries[0].checkpointHash = substituted.checkpointHash
    fs.writeFileSync(indexPath, checkpointContract.canonical(substitutedIndex) + '\\n')
    assert.equal(checkpoints.read(stem, value.checkpointId), null,
      'checkpoint bytes must identify the generation named by their canonical file and index entry')
    fs.writeFileSync(checkpointPath, currentBytes)
    fs.writeFileSync(indexPath, currentIndexBytes)
    fs.writeFileSync(checkpointPath, JSON.stringify({ ...value, schemaVersion: 2 }))
    const incompatibleBytes = fs.readFileSync(checkpointPath)
    assert.equal(checkpoints.read(stem, value.checkpointId), null)
    assert.deepEqual(fs.readFileSync(checkpointPath), incompatibleBytes,
      'the reader must not convert or delete incompatible bytes')
    fs.writeFileSync(checkpointPath, currentBytes)
    assert.equal(checkpoints.read(stem, value.checkpointId).schemaVersion, 1)
    assert.equal(checkpoints.freshness(value, { ...common,
      projectRevision: { available: true, revision: 'sha256:' + 'f'.repeat(64) }
    }).reasonCode, 'project-changed')
    assert.equal(checkpoints.retryStatus(stem, value.checkpointId, {
      dependencies: common
    }).candidate.id, value.checkpointId)
    const staleStatus = checkpoints.retryStatus(stem, value.checkpointId, {
      dependencies: { ...common,
        projectRevision: { available: true, revision: 'sha256:' + 'f'.repeat(64) }
      }
    })
    assert.equal(staleStatus.candidate, null)
    assert.equal(staleStatus.projection.freshness.reasonCode, 'project-changed')
    assert.equal(checkpoints.retryStatus(stem, 'cp-' + '9'.repeat(32), {
      dependencies: common
    }).projection.freshness.reasonCode, 'checkpoint-invalid')

    const actionRevision = 'sha256:' + '7'.repeat(64)
    const summary = { task: { primaryAction: {
      kind: 'retry-phase', checkpointId: value.checkpointId, actionRevision
    } } }
    const preview = checkpoints.preview(stem, {
      checkpointId: value.checkpointId,
      checkpointHash: value.checkpointHash,
      actionRevision
    }, summary, common)
    assert.equal(preview.ok, true)
    assert.equal(preview.confirmationRequired, true)
    assert.equal(typeof preview.confirmationToken, 'string')
    assert.equal(checkpoints.consumeConfirmation(value, actionRevision, preview.confirmationToken), true)
    assert.equal(checkpoints.consumeConfirmation(value, actionRevision, preview.confirmationToken), false)

    const costlyCheckpoint = checkpoints.create(stem, {
      runId: 'run-71',
      phase: 'assemble-gate',
      attempt: 3,
      status: 'failed',
      outputReceiptIds: [],
      priorPhaseReceiptIds: [],
      failureCode: 'phase-failed',
      retryPolicy: { kind: 'retry-phase', safePhase: 'assemble-gate', reasonCode: 'phase-failed' }
    }, common)
    assert.equal(costlyCheckpoint.ok, true)
    const costlyRevision = 'sha256:' + '8'.repeat(64)
    const costlyPreview = checkpoints.preview(stem, {
      checkpointId: costlyCheckpoint.checkpoint.checkpointId,
      checkpointHash: costlyCheckpoint.checkpoint.checkpointHash,
      actionRevision: costlyRevision
    }, { task: { primaryAction: {
      kind: 'retry-phase',
      checkpointId: costlyCheckpoint.checkpoint.checkpointId,
      actionRevision: costlyRevision
    } } }, common)
    assert.equal(costlyPreview.ok, true)
    assert.equal(costlyPreview.confirmationRequired, true)
    assert.equal(typeof costlyPreview.confirmationToken, 'string')

    const receiptCheckpoint = checkpoints.create(stem, {
      runId: 'run-71',
      phase: 'review',
      attempt: 3,
      status: 'failed',
      outputReceiptIds: ['review-report-1'],
      priorPhaseReceiptIds: [],
      failureCode: 'review-failed',
      retryPolicy: { kind: 'retry-phase', safePhase: 'review', reasonCode: 'review-failed' }
    }, common)
    assert.equal(receiptCheckpoint.ok, true)
    const receiptValue = receiptCheckpoint.checkpoint
    assert.equal(checkpoints.freshness(receiptValue, {
      ...common, receiptVerifier: undefined
    }).reasonCode,
      'receipt-verification-unavailable')
    assert.equal(checkpoints.freshness(receiptValue, {
      ...common, receiptVerifier: (id) => id === 'review-report-1'
    }).current, true)

    assert.equal(checkpoints.create(stem, {
      runId: 'foreign-run',
      phase: 'review',
      attempt: 4,
      status: 'failed',
      outputReceiptIds: [],
      priorPhaseReceiptIds: [],
      failureCode: 'review-failed',
      retryPolicy: { kind: 'retry-phase', safePhase: 'review', reasonCode: 'review-failed' }
    }, common).error, 'checkpoint-owner-unverified')

    const ship = checkpoints.create(stem, {
      runId: 'run-71', phase: 'ship', attempt: 1, status: 'completed',
      outputReceiptIds: [], priorPhaseReceiptIds: ['test-summary:sha256:' + '6'.repeat(64)], failureCode: null,
      retryPolicy: { kind: 'restart-task', safePhase: null, reasonCode: 'manual-retry' }
    }, common)
    assert.equal(ship.ok, true)
    assert.equal(checkpoints.sealingGate(stem, 'run-71', executionPin, common).ok, true,
      'a current completed ship checkpoint authorizes only its exact candidate pin')
    const movedPin = { ...executionPin, executionTree: '4'.repeat(40) }
    const staleGate = checkpoints.sealingGate(stem, 'run-71', movedPin, common)
    assert.equal(staleGate.ok, false)
    assert.equal(staleGate.code, 'SEAL_GATE_STALE')
    assert.equal(staleGate.reasonCode, 'execution-changed')
    assert.equal(checkpoints.sealingGate(stem, 'foreign-run', executionPin, common).code, 'SEAL_GATE_ABSENT')

    fs.writeFileSync(checkpointPath, Buffer.alloc(65 * 1024, 0x20))
    assert.equal(checkpoints.read(stem, value.checkpointId), null)
    assert.equal(checkpoints.list(stem).partial, true)
    fs.writeFileSync(path.join(process.env.ORCHESTRATOR_CHECKPOINTS_DIR, stem, 'orphan.json'), '{}')
    assert.equal(checkpoints.readIndex(stem).invalid, true)
    assert.equal(checkpoints.read(stem, value.checkpointId), null)
  `
  const result = spawnSync(process.execPath, ['-e', script], {
    cwd: REPO,
    env: {
      ...process.env,
      ORCHESTRATOR_PROJECT_ROOT: root,
      ORCHESTRATOR_CHECKPOINTS_DIR: join(root, 'orchestrator', '.cache', 'tasks', 'checkpoints'),
    },
    encoding: 'utf8',
  })
  try {
    assert.equal(result.status, 0, result.stderr || result.stdout)
  } finally {
    rmSync(root, { recursive: true, force: true })
  }
})

test('restart past tests accepts only a current PASS summary for the same checkpoint identity', () => {
  const root = mkdtempSync(join(tmpdir(), 'task-checkpoint-summary-test-'))
  const modulePath = join(REPO, 'orchestrator', 'site', 'server', 'task-checkpoints.js')
  const script = `
    const assert = require('node:assert/strict')
    const fs = require('node:fs')
    const path = require('node:path')
    const checkpoints = require(${JSON.stringify(modulePath)})
    const summaryContract = require(${JSON.stringify(join(REPO, 'orchestrator', 'tasks', 'task-test-summary-contract.cjs'))})
    const receiptContract = require(${JSON.stringify(join(REPO, 'orchestrator', 'tasks', 'task-test-receipt-contract.cjs'))})
    const impactContract = require(${JSON.stringify(join(REPO, 'orchestrator', 'tasks', 'task-test-impact-contract.cjs'))})
    const snapshotContract = require(${JSON.stringify(join(REPO, 'orchestrator', 'tasks', 'content-snapshot.cjs'))})
    const policy = require(${JSON.stringify(join(REPO, 'orchestrator', 'tasks', 'task-test-policy-contract.cjs'))}).loadPolicy()
    const stem = ${JSON.stringify(STEM)}
    const H = (c) => 'sha256:' + c.repeat(64)
    const runDir = path.join(process.env.ORCHESTRATOR_TEST_CERTIFICATION_DIR, stem, 'run-71')
    fs.mkdirSync(runDir, { recursive: true })
    const source = snapshotContract.captureSnapshot({ root: runDir, paths: [] })
    const impactBase = {
      version: 1, policyVersion: 1, policyHash: policy.policyHash, taskStem: stem, runId: 'run-71',
      taskInputHash: H('1'), sourceSnapshotHash: source.snapshotHash, capabilityInventoryHash: H('5'),
      moduleGraphHash: H('6'), affectedModules: [':lib'], affectedConsumers: [], requiredSuites: ['owner'],
      fullSuiteRequired: false, requiredCapabilities: ['base'], testNotApplicable: null,
      notApplicableValidators: [], unknownDependencies: [], selectionReasons: ['planner-proposed']
    }
    const behavior = { anchor: 'test:checkpoint', acceptanceHash: H('7'), changeKind: 'di-composition-root', ownerBuilder: 'feature-builder',
      ownerModule: ':lib', testLayer: 'host', testFile: 'lib/Test.kt', proposedTestCases: ['a.B.c'],
      observedTestCases: [], requiredLanes: ['host'], negativeCases: [] }
    const planned = { ...impactBase, phase: 'planned', behaviors: [behavior], impactHash: H('0') }
    planned.impactHash = impactContract.impactHashOf(planned)
    const observed = { ...impactBase, phase: 'observed',
      behaviors: [{ ...behavior, observedTestCases: ['a.B.c'] }], impactHash: H('0') }
    observed.impactHash = impactContract.impactHashOf(observed)
    const started = {
      version: 1, kind: 'test-command', stage: 'started', taskStem: stem, runId: 'run-71', sessionId: 'sess-71',
      lockStage: 'orchestrator', taskInputHash: H('1'), sourceSnapshotHash: source.snapshotHash,
      impactHash: observed.impactHash, policyHash: policy.policyHash, suite: 'owner', tier: 'certification-direct',
      lane: 'host', taskPaths: [':lib:testAndroidHostTest'], cwd: '/product', envFingerprint: H('8'),
      toolchain: { gradle: '9.1.0', kotlin: '2.3.21', agp: '9.0.1', jdk: '21', os: 'darwin', arch: 'arm64' },
      startedAt: '2026-08-03T10:00:00.000Z', startedReceiptHash: null, endedAt: null, durationMs: null,
      exitCode: null, signal: null, timedOut: false, disposition: 'pending',
      counts: { discovered: 0, executed: 0, passed: 0, failed: 0, skipped: 0, aborted: 0 },
      leafResults: [], discoveredTestIdentities: [], reportArtifacts: [],
      outputDigest: { bytes: 0, hash: H('9'), redacted: true }, retryHistory: [], pid: 42, processGroup: 42,
      executionRootKind: 'shared-serial', receiptHash: H('0')
    }
    started.receiptHash = receiptContract.receiptHashOf(started)
    const terminal = { ...started, stage: 'terminal', startedReceiptHash: started.receiptHash,
      endedAt: '2026-08-03T10:00:01.000Z', durationMs: 1000, exitCode: 0, disposition: 'executed',
      counts: { discovered: 1, executed: 1, passed: 1, failed: 0, skipped: 0, aborted: 0 },
      leafResults: [{ taskPath: ':lib:testAndroidHostTest', outcome: 'passed', disposition: 'executed' }],
      discoveredTestIdentities: ['a.B.c'],
      reportArtifacts: [{ path: 'lib/build/test-results/TEST-a.xml', bytes: 100, hash: H('a'), parser: 'junit-xml' }],
      outputDigest: { bytes: 20, hash: H('b'), redacted: true }, receiptHash: H('0') }
    terminal.receiptHash = receiptContract.receiptHashOf(terminal)
    const summary = (verdict) => {
      const value = {
        version: 1, taskStem: stem, runId: 'run-71', sessionId: 'sess-71',
        taskInputHash: H('1'), sourceSnapshotHash: source.snapshotHash, policyVersion: 1,
        policyHash: policy.policyHash, plannedImpactHash: planned.impactHash, observedImpactHash: observed.impactHash,
        requiredLanes: ['host'], executedLanes: ['host'],
        anchorEvidence: [{ anchor: 'test:checkpoint', testIdentities: ['a.B.c'], receiptHashes: [terminal.receiptHash], verified: true }],
        requiredSuites: ['owner'], passedSuites: ['owner'],
        fullSuiteRequired: false, fullSuiteResult: null, failBeforePassAfter: [],
        zeroTestVerdicts: [], flakyVerdicts: [], coverage: null,
        snapshotVerification: 'current', commandReceiptHashes: [terminal.receiptHash],
        structuralReceiptHashes: [], verdict,
        verdictReasons: [verdict === 'PASS' ? 'all-required-test-evidence-proven' : 'command-receipt-failed'],
        summaryHash: H('0')
      }
      value.summaryHash = summaryContract.summaryHashOf(value)
      return value
    }
    const taskSnapshot = { state: 'todo', sourceRevision: ${JSON.stringify(HASH_A)}, dependencies: [] }
    const projectRevision = { available: true, revision: ${JSON.stringify(HASH_B)}, limitations: [] }
    const common = { taskSnapshot, projectRevision, configHash: H('c'), activeRunId: 'run-71',
      testPolicyHash: policy.policyHash, executionPin: {
        worktreeId: 'wt-' + '1'.repeat(32), baseCommit: '1'.repeat(40), baseTree: '2'.repeat(40),
        executionTree: '3'.repeat(40), targetRef: 'refs/heads/main', targetCommit: '1'.repeat(40)
      } }
    for (const [name, document] of Object.entries({ 'policy.json': policy, 'source-snapshot.json': source,
      'planned-impact.json': planned, 'observed-impact.json': observed })) {
      fs.writeFileSync(path.join(runDir, name), JSON.stringify(document))
    }
    const commandsDir = path.join(runDir, 'commands')
    fs.mkdirSync(commandsDir)
    fs.writeFileSync(path.join(commandsDir, '000-started-' + started.receiptHash.slice(7) + '.json'), JSON.stringify(started))
    fs.writeFileSync(path.join(commandsDir, '000-terminal-' + terminal.receiptHash.slice(7) + '.json'), JSON.stringify(terminal))
    const failed = summary('FAIL')
    fs.writeFileSync(path.join(runDir, 'summary.json'), JSON.stringify(failed))
    const created = checkpoints.create(stem, {
      runId: 'run-71', phase: 'review', attempt: 1, status: 'failed', outputReceiptIds: [],
      priorPhaseReceiptIds: ['test-summary:' + failed.summaryHash], failureCode: 'phase-failed',
      retryPolicy: { kind: 'restart-from-phase', safePhase: 'assemble-gate', reasonCode: 'phase-failed' }
    }, common)
    assert.equal(created.ok, true)
    assert.equal(checkpoints.freshness(created.checkpoint, common).current, false)
    const passed = summary('PASS')
    fs.writeFileSync(path.join(runDir, 'summary.json'), JSON.stringify(passed))
    const passCheckpoint = checkpoints.create(stem, {
      runId: 'run-71', phase: 'review', attempt: 2, status: 'failed', outputReceiptIds: [],
      priorPhaseReceiptIds: ['test-summary:' + passed.summaryHash], failureCode: 'phase-failed',
      retryPolicy: { kind: 'restart-from-phase', safePhase: 'assemble-gate', reasonCode: 'phase-failed' }
    }, common)
    assert.equal(passCheckpoint.ok, true)
    assert.equal(checkpoints.freshness(passCheckpoint.checkpoint, common).current, true)
  `
  const result = spawnSync(process.execPath, ['-e', script], {
    cwd: REPO,
    env: {
      ...process.env,
      ORCHESTRATOR_PROJECT_ROOT: root,
      ORCHESTRATOR_CHECKPOINTS_DIR: join(root, 'orchestrator', '.cache', 'tasks', 'checkpoints'),
      ORCHESTRATOR_TEST_CERTIFICATION_DIR: join(root, 'orchestrator', '.cache', 'tasks', 'test-certification'),
    },
    encoding: 'utf8',
  })
  try {
    assert.equal(result.status, 0, result.stderr || result.stdout)
  } finally {
    rmSync(root, { recursive: true, force: true })
  }
})

test('durable task-action idempotency replays exact bodies and conflicts on key reuse', () => {
  const root = mkdtempSync(join(tmpdir(), 'task-action-receipt-test-'))
  const modulePath = join(REPO, 'orchestrator', 'site', 'server', 'task-action-receipts.js')
  const script = `
    const assert = require('node:assert/strict')
    const receipts = require(${JSON.stringify(modulePath)})
    const request = { idempotencyKey: 'action-key-71', stem: ${JSON.stringify(STEM)},
      kind: 'run', value: 1 }
    const first = receipts.reserve(request)
    assert.equal(first.ok, true)
    assert.ok(first.handle)
    assert.equal(receipts.reserve(request).error, 'task-action-recovery-required')
    const response = { schemaVersion: 1, action: 'run', status: 'accepted',
      requestId: '1-a', sessionId: null,
      resultingActionRevision: ${JSON.stringify(HASH_A)},
      taskSummaryRevision: ${JSON.stringify(HASH_B)}, idempotentReplay: false }
    assert.equal(receipts.complete(first.handle, response), true)
    const replay = receipts.reserve(request)
    assert.equal(replay.replay, true)
    assert.equal(replay.response.idempotentReplay, true)
    assert.equal(receipts.reserve({ ...request, value: 2 }).error, 'idempotency-key-conflict')

    const releasable = receipts.reserve({ ...request, idempotencyKey: 'action-key-72' })
    assert.equal(receipts.release(releasable.handle), true)
    assert.equal(receipts.read('action-key-72'), null)

    const fs = require('node:fs')
    fs.writeFileSync(require('node:path').join(
      process.env.ORCHESTRATOR_TASK_ACTION_RECEIPTS_DIR, 'foreign.txt'), 'foreign\\n')
    assert.equal(
      receipts.reserve({ ...request, idempotencyKey: 'action-key-73' }).error,
      'task-action-idempotency-unavailable'
    )
  `
  const result = spawnSync(process.execPath, ['-e', script], {
    cwd: REPO,
    env: {
      ...process.env,
      ORCHESTRATOR_PROJECT_ROOT: root,
      ORCHESTRATOR_TASK_ACTION_RECEIPTS_DIR:
        join(root, 'orchestrator', '.cache', 'tasks', 'action-receipts'),
    },
    encoding: 'utf8',
  })
  try {
    assert.equal(result.status, 0, result.stderr || result.stdout)
  } finally {
    rmSync(root, { recursive: true, force: true })
  }
})
