#!/usr/bin/env node

// Server-verified test-certification projection (improvement 05 §19.4): the
// Site renders the typed status from the sealed summary and never computes
// PASS itself; forged or foreign summaries are integrity drift, and done
// done tasks without current evidence are invalid — never synthesized.

import assert from 'node:assert/strict'
import { test } from 'node:test'
import { spawnSync } from 'node:child_process'
import { linkSync, mkdirSync, mkdtempSync, rmSync, symlinkSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { createRequire } from 'node:module'

const HERE = dirname(fileURLToPath(import.meta.url))
const REPO = join(HERE, '..', '..', '..')
const require = createRequire(import.meta.url)
const summaryContract = require('../../tasks/task-test-summary-contract.cjs')
const receiptContract = require('../../tasks/task-test-receipt-contract.cjs')
const snapshotContract = require('../../tasks/content-snapshot.cjs')
const policyContract = require('../../tasks/task-test-policy-contract.cjs')
const resolver = await import('../../tasks/resolve-test-impact.mjs')

const STEM = 'TASK_5_save_note'
const H = (c) => 'sha256:' + c.repeat(64)

function sealedSummary(patch = {}) {
  const document = {
    version: 1, taskStem: STEM, runId: 'run-cert1', sessionId: 'sess-1',
    taskInputHash: H('1'), sourceSnapshotHash: H('2'),
    policyVersion: 1, policyHash: H('4'),
    plannedImpactHash: H('3'), observedImpactHash: H('3'),
    requiredLanes: ['host'], executedLanes: ['host'],
    anchorEvidence: [{ anchor: 'test:save-note-works', testIdentities: ['a.B.c'], receiptHashes: [H('a')], verified: true }],
    requiredSuites: ['save-note'], passedSuites: ['save-note'],
    fullSuiteRequired: false, fullSuiteResult: null,
    failBeforePassAfter: [], zeroTestVerdicts: [], flakyVerdicts: [], coverage: null,
    snapshotVerification: 'current',
    commandReceiptHashes: [H('a')], structuralReceiptHashes: [],
    verdict: 'PASS', verdictReasons: ['all-required-lanes-proven'],
    summaryHash: H('0'),
    ...patch,
  }
  document.summaryHash = summaryContract.summaryHashOf(document)
  return document
}

function writeEvidenceGraph(runDir, sourceOptions = null) {
  const policy = policyContract.loadPolicy()
  const source = snapshotContract.captureSnapshot(sourceOptions || { root: runDir, paths: [] })
  const proposal = {
    taskStem: STEM, runId: 'run-cert1', taskInputHash: H('1'),
    sourceSnapshotHash: source.snapshotHash, capabilityInventoryHash: H('5'), moduleGraphHash: H('6'),
    behaviors: [{ anchor: 'test:save-note-works', acceptanceHash: H('7'), changeKind: 'di-composition-root', ownerBuilder: 'feature-builder',
      ownerModule: ':lib', testLayer: 'host', testFile: 'lib/Test.kt', changeKind: 'di-composition-root',
      proposedTestCases: ['a.B.c'], observedTestCases: [], requiredLanes: ['host'], negativeCases: [] }],
    affectedModules: [':lib'], affectedConsumers: [], requiredSuites: ['save-note'],
    requiredCapabilities: ['base'], fullSuiteRequired: false, testNotApplicable: null,
  }
  const planned = resolver.resolveImpact({ policy, proposal: { ...proposal, phase: 'planned' }, facts: {} }).impact
  const observed = resolver.resolveImpact({
    policy, proposal: { ...proposal, phase: 'observed',
      behaviors: proposal.behaviors.map((behavior) => ({ ...behavior, observedTestCases: ['a.B.c'] })) }, facts: {},
  }).impact
  const started = {
    version: 1, kind: 'test-command', stage: 'started', taskStem: STEM, runId: 'run-cert1', sessionId: 'sess-1',
    lockStage: 'orchestrator', taskInputHash: H('1'), sourceSnapshotHash: source.snapshotHash,
    impactHash: observed.impactHash, policyHash: policy.policyHash, suite: 'save-note', tier: 'certification-direct',
    lane: 'host', taskPaths: [':lib:testAndroidHostTest'], cwd: '/product', envFingerprint: H('8'),
    toolchain: { gradle: '9.1.0', kotlin: '2.3.21', agp: '9.0.1', jdk: '21', os: 'darwin', arch: 'arm64' },
    startedAt: '2026-08-03T10:00:00.000Z', startedReceiptHash: null, endedAt: null, durationMs: null,
    exitCode: null, signal: null, timedOut: false, disposition: 'pending',
    counts: { discovered: 0, executed: 0, passed: 0, failed: 0, skipped: 0, aborted: 0 },
    leafResults: [], discoveredTestIdentities: [], reportArtifacts: [],
    outputDigest: { bytes: 0, hash: H('9'), redacted: true }, retryHistory: [], pid: 42, processGroup: 42,
    executionRootKind: 'shared-serial', receiptHash: H('0'),
  }
  started.receiptHash = receiptContract.receiptHashOf(started)
  const terminal = {
    ...started, stage: 'terminal', startedReceiptHash: started.receiptHash,
    endedAt: '2026-08-03T10:00:01.000Z', durationMs: 1000, exitCode: 0, disposition: 'executed',
    counts: { discovered: 1, executed: 1, passed: 1, failed: 0, skipped: 0, aborted: 0 },
    leafResults: [{ taskPath: ':lib:testAndroidHostTest', outcome: 'passed', disposition: 'executed' }],
    discoveredTestIdentities: ['a.B.c'],
    reportArtifacts: [{ path: 'lib/build/test-results/TEST-a.xml', bytes: 100, hash: H('a'), parser: 'junit-xml' }],
    outputDigest: { bytes: 20, hash: H('b'), redacted: true }, receiptHash: H('0'),
  }
  terminal.receiptHash = receiptContract.receiptHashOf(terminal)
  const summary = sealedSummary({
    sourceSnapshotHash: source.snapshotHash, policyHash: policy.policyHash,
    plannedImpactHash: planned.impactHash, observedImpactHash: observed.impactHash,
    anchorEvidence: [{ anchor: 'test:save-note-works', testIdentities: ['a.B.c'],
      receiptHashes: [terminal.receiptHash], verified: true }],
    commandReceiptHashes: [terminal.receiptHash], verdictReasons: ['all-required-test-evidence-proven'],
  })
  for (const [name, document] of Object.entries({
    'policy.json': policy, 'source-snapshot.json': source,
    'planned-impact.json': planned, 'observed-impact.json': observed, 'summary.json': summary,
  })) writeFileSync(join(runDir, name), JSON.stringify(document, null, 2))
  const commands = join(runDir, 'commands')
  mkdirSync(commands)
  writeFileSync(join(commands, '000-started-' + started.receiptHash.slice(7) + '.json'), JSON.stringify(started, null, 2))
  writeFileSync(join(commands, '000-terminal-' + terminal.receiptHash.slice(7) + '.json'), JSON.stringify(terminal, null, 2))
  return summary
}

function statusVia(cacheRoot, args) {
  const script = `
    const module = require(${JSON.stringify(join(REPO, 'orchestrator', 'site', 'server', 'task-test-certification.js'))})
    process.stdout.write(JSON.stringify(module.statusFor(${JSON.stringify(args.stem)}, ${JSON.stringify(args.options || {})})))
  `
  const result = spawnSync(process.execPath, ['-e', script], {
    encoding: 'utf8',
    env: { ...process.env, ORCHESTRATOR_CACHE_DIR: cacheRoot,
      ORCHESTRATOR_PROJECT_ROOT: args.projectRoot || REPO },
  })
  assert.equal(result.status, 0, result.stderr)
  return JSON.parse(result.stdout)
}

test('site projects only the server-verified sealed summary and never computes PASS', () => {
  const cacheRoot = mkdtempSync(join(tmpdir(), 'test-cert-view-'))
  const runDir = join(cacheRoot, 'tasks', 'test-certification', STEM, 'run-cert1')
  mkdirSync(runDir, { recursive: true })

  const summary = writeEvidenceGraph(runDir)
  const pass = statusVia(cacheRoot, { stem: STEM })
  assert.equal(pass.state, 'pass')
  assert.equal(pass.verdict, 'PASS')
  assert.equal(pass.summaryHash, summary.summaryHash)
  assert.deepEqual(pass.requiredLanes, ['host'])
  assert.equal(pass.anchorsVerified, 1)
  assert.equal(pass.stale, false)
  for (const reason of pass.reasons) assert.match(reason, /^[a-z][a-z0-9_-]{0,79}$/)

  // A fully self-hashed PASS that cites a nonexistent terminal receipt is
  // invalid transitively; this is the forged-summary mutation that the old
  // standalone summary validator could not detect.
  const missingHash = H('c')
  const missingReceipt = sealedSummary({
    sourceSnapshotHash: summary.sourceSnapshotHash, policyHash: summary.policyHash,
    plannedImpactHash: summary.plannedImpactHash, observedImpactHash: summary.observedImpactHash,
    anchorEvidence: [{ anchor: 'test:save-note-works', testIdentities: ['a.B.c'],
      receiptHashes: [missingHash], verified: true }],
    commandReceiptHashes: [missingHash], verdictReasons: ['all-required-test-evidence-proven'],
  })
  writeFileSync(join(runDir, 'summary.json'), JSON.stringify(missingReceipt, null, 2))
  assert.equal(statusVia(cacheRoot, { stem: STEM }).state, 'invalid')

  // Forged bytes are integrity drift: a blocking `invalid`, never a verdict.
  const forged = { ...summary, verdict: 'FAIL' }
  writeFileSync(join(runDir, 'summary.json'), JSON.stringify(forged, null, 2))
  assert.equal(statusVia(cacheRoot, { stem: STEM }).state, 'invalid')

  // A foreign task's sealed summary never projects onto this stem.
  const foreign = sealedSummary({ taskStem: 'TASK_6_other_task' })
  writeFileSync(join(runDir, 'summary.json'), JSON.stringify(foreign, null, 2))
  assert.equal(statusVia(cacheRoot, { stem: STEM }).reasons[0], 'summary-foreign-task')

  const foreignRun = sealedSummary({ runId: 'run-other' })
  writeFileSync(join(runDir, 'summary.json'), JSON.stringify(foreignRun, null, 2))
  assert.equal(statusVia(cacheRoot, { stem: STEM }).reasons[0], 'summary-foreign-run')

  // A second hard link would let an external name mutate the supposedly
  // sealed generation; the guarded reader refuses it.
  writeFileSync(join(runDir, 'summary.json'), JSON.stringify(summary, null, 2))
  linkSync(join(runDir, 'summary.json'), join(runDir, 'summary-alias.json'))
  assert.equal(statusVia(cacheRoot, { stem: STEM }).state, 'invalid')
  rmSync(join(runDir, 'summary-alias.json'))

  // Input drift stays advisory: a sealed stale summary projects with a badge.
  const stale = sealedSummary({
    sourceSnapshotHash: summary.sourceSnapshotHash, policyHash: summary.policyHash,
    plannedImpactHash: summary.plannedImpactHash, observedImpactHash: summary.observedImpactHash,
    anchorEvidence: summary.anchorEvidence, commandReceiptHashes: summary.commandReceiptHashes,
    snapshotVerification: 'stale', verdict: 'BLOCKED',
    verdictReasons: ['source-snapshot-stale'],
  })
  writeFileSync(join(runDir, 'summary.json'), JSON.stringify(stale, null, 2))
  const staleStatus = statusVia(cacheRoot, { stem: STEM })
  assert.equal(staleStatus.state, 'blocked')
  assert.equal(staleStatus.stale, true)

  rmSync(cacheRoot, { recursive: true, force: true })
})

test('site recomputes source-snapshot freshness instead of trusting a stored current flag', () => {
  const projectRoot = mkdtempSync(join(tmpdir(), 'test-cert-source-'))
  const cacheRoot = join(projectRoot, '.cache')
  const runDir = join(cacheRoot, 'tasks', 'test-certification', STEM, 'run-cert1')
  mkdirSync(runDir, { recursive: true })
  writeFileSync(join(projectRoot, 'tracked.txt'), 'before\n')
  writeEvidenceGraph(runDir, { root: projectRoot, paths: ['tracked.txt'] })
  const current = statusVia(cacheRoot, { stem: STEM, projectRoot })
  assert.equal(current.state, 'pass')
  assert.equal(current.stale, false)
  writeFileSync(join(projectRoot, 'tracked.txt'), 'after\n')
  const stale = statusVia(cacheRoot, { stem: STEM, projectRoot })
  assert.equal(stale.state, 'pass')
  assert.equal(stale.stale, true)
  rmSync(projectRoot, { recursive: true, force: true })
})

test('symlinked run directories are integrity drift, never selected evidence', () => {
  const cacheRoot = mkdtempSync(join(tmpdir(), 'test-cert-view-'))
  const stemDir = join(cacheRoot, 'tasks', 'test-certification', STEM)
  const outside = mkdtempSync(join(tmpdir(), 'test-cert-outside-'))
  mkdirSync(stemDir, { recursive: true })
  writeFileSync(join(outside, 'summary.json'), JSON.stringify(sealedSummary(), null, 2))
  symlinkSync(outside, join(stemDir, 'run-cert1'))
  const result = statusVia(cacheRoot, { stem: STEM })
  assert.equal(result.state, 'invalid')
  rmSync(cacheRoot, { recursive: true, force: true })
  rmSync(outside, { recursive: true, force: true })
})

test('missing evidence is typed: not-run for live tasks and invalid for done tasks', () => {
  const cacheRoot = mkdtempSync(join(tmpdir(), 'test-cert-view-'))
  assert.equal(statusVia(cacheRoot, { stem: STEM }).state, 'not-run')
  const done = statusVia(cacheRoot, { stem: STEM, options: { doneTask: true } })
  assert.equal(done.state, 'invalid')
  assert.equal(done.reasons[0], 'test-evidence-missing')
  assert.equal(statusVia(cacheRoot, { stem: '../escape' }).state, 'invalid')
  rmSync(cacheRoot, { recursive: true, force: true })
})

test('board rows carry the projection field only through the server module', () => {
  const source = spawnSync(process.execPath, ['-e', `
    const fs = require('node:fs')
    const path = ${JSON.stringify(join(REPO, 'orchestrator', 'site', 'server', 'task-summary.js'))}
    const text = fs.readFileSync(path, 'utf8')
    process.stdout.write(JSON.stringify({
      wired: text.includes("require('./task-test-certification')"),
      attached: text.includes('testCertification:'),
    }))
  `], { encoding: 'utf8' })
  const flags = JSON.parse(source.stdout)
  assert.equal(flags.wired, true)
  assert.equal(flags.attached, true)
})
