#!/usr/bin/env node

// Receipt/summary schemas, freshness, forgery and recovery semantics
// (improvement 05, Phase 3). The structural kind can never impersonate
// executable evidence; the summary can never express an inconsistent green.

import assert from 'node:assert/strict';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const receiptContract = require('../task-test-receipt-contract.cjs');
const summaryContract = require('../task-test-summary-contract.cjs');
const registry = require('../task-receipt-registry.cjs');
const taskInputContract = require('../task-test-input-contract.cjs');

const failures = [];
let checks = 0;

async function check(name, fn) {
  checks++;
  try { await fn(); console.log(`PASS ${name}`); }
  catch (error) { failures.push({ name, error }); console.error(`FAIL ${name}\n${error && error.stack || error}`); }
}

const H = (c) => 'sha256:' + c.repeat(64);
const ISO = '2026-08-02T10:00:00.000Z';
const ISO2 = '2026-08-02T10:00:05.000Z';

function commandReceipt(overrides = {}) {
  const receipt = {
    version: 1,
    kind: 'test-command',
    stage: 'terminal',
    taskStem: 'TASK_5_save_note',
    runId: 'run-ev1',
    sessionId: 'sess-1',
    lockStage: 'run',
    taskInputHash: H('1'),
    sourceSnapshotHash: H('2'),
    impactHash: H('3'),
    policyHash: H('4'),
    suite: 'save-note',
    tier: 'certification-direct',
    lane: 'host',
    taskPaths: [':lib:testAndroidHostTest'],
    cwd: '/product',
    envFingerprint: H('5'),
    toolchain: { gradle: '9.1.0', kotlin: '2.3.21', agp: '9.0.1', jdk: '19', os: 'darwin', arch: 'arm64' },
    startedAt: ISO,
    startedReceiptHash: H('6'),
    endedAt: ISO2,
    durationMs: 5000,
    exitCode: 0,
    signal: null,
    timedOut: false,
    disposition: 'executed',
    counts: { discovered: 3, executed: 3, passed: 3, failed: 0, skipped: 0, aborted: 0 },
    leafResults: [{ taskPath: ':lib:testAndroidHostTest', outcome: 'passed', disposition: 'executed' }],
    discoveredTestIdentities: ['spike.CoreTest.adds', 'spike.CoreTest.greets', 'spike.CoreTest.bounds'],
    reportArtifacts: [{ path: 'lib/build/test-results/TEST-x.xml', bytes: 512, hash: H('7'), parser: 'junit-xml' }],
    outputDigest: { bytes: 100, hash: H('8'), redacted: true },
    retryHistory: [],
    pid: 4242,
    processGroup: 4242,
    executionRootKind: 'shared-serial',
    receiptHash: H('0'),
    ...overrides
  };
  receipt.receiptHash = receiptContract.receiptHashOf(receipt);
  return receipt;
}

function structuralReceipt(overrides = {}) {
  const receipt = {
    version: 1,
    kind: 'test-structural-gate',
    stage: 'terminal',
    taskStem: 'TASK_5_save_note',
    runId: 'run-ev1',
    sessionId: 'sess-1',
    lockStage: 'run',
    taskInputHash: H('1'),
    sourceSnapshotHash: H('2'),
    policyHash: H('4'),
    gateId: 'docs-contract-gate',
    executionMode: 'in-process',
    tool: 'orchestrator/tasks/validate-task-state.mjs',
    validatorCodeHash: H('9'),
    pid: null,
    processGroup: null,
    startedAt: ISO,
    startedReceiptHash: H('6'),
    endedAt: ISO2,
    durationMs: 100,
    exitCode: null,
    signal: null,
    timedOut: false,
    result: 'passed',
    artifacts: [],
    outputDigest: { bytes: 10, hash: H('8'), redacted: true },
    receiptHash: H('0'),
    ...overrides
  };
  receipt.receiptHash = receiptContract.receiptHashOf(receipt);
  return receipt;
}

function rejects(fn, name, code) {
  try { fn(); }
  catch (error) {
    assert.equal(error.name, name, String(error && error.stack || error));
    assert.equal(error.code, code);
    return;
  }
  assert.fail(`expected ${name} ${code}`);
}

await check('task-input hash ignores only the anchored Outcome and changes on every task-body edit', () => {
  const task = '# TASK 5 — Save note\n\n## Goal\n- Save a note.\n';
  const withOutcome = task.trimEnd() + '\n\n---\n\n## Outcome\n\n**Status**: completed\n';
  assert.equal(taskInputContract.taskInputHashOf(task), taskInputContract.taskInputHashOf(withOutcome));
  assert.equal(taskInputContract.taskInputHashOf(task + '\n \n'),
    taskInputContract.taskInputHashOf(withOutcome));
  assert.notEqual(taskInputContract.taskInputHashOf(task),
    taskInputContract.taskInputHashOf(task.replace('Save a note.', 'Save two notes.')));
  assert.throws(() => taskInputContract.taskInputHashOf(task.replace(/\n/g, '\r\n')),
    (error) => error && error.code === 'TASK_INPUT_INVALID');
});

await check('command receipt validates and every tamper breaks the hash binding', () => {
  const valid = receiptContract.validateCommandReceipt(commandReceipt());
  assert.equal(valid.kind, 'test-command');
  const tamper = (mutate, code = 'HASH_MISMATCH') => {
    const copy = JSON.parse(JSON.stringify(commandReceipt()));
    mutate(copy);
    rejects(() => receiptContract.validateCommandReceipt(copy), 'TestReceiptError', code);
  };
  tamper((r) => { r.exitCode = 1; });
  tamper((r) => { r.counts.failed = 1; }, 'RECEIPT_INVALID');
  tamper((r) => { r.sourceSnapshotHash = H('e'); });
  tamper((r) => { r.extra = true; }, 'RECEIPT_INVALID');
  tamper((r) => { delete r.envFingerprint; }, 'RECEIPT_INVALID');
  tamper((r) => { r.reportArtifacts[0].path = '../outside.xml'; }, 'RECEIPT_INVALID');
  tamper((r) => { r.reportArtifacts[0].parser = 'stdout-heuristics'; }, 'RECEIPT_INVALID');
  tamper((r) => { r.outputDigest.redacted = false; }, 'RECEIPT_INVALID');
  tamper((r) => { r.retryHistory = [{ reason: 'diag', priorReceiptHash: H('a') }, { reason: 'diag', priorReceiptHash: H('b') }]; }, 'RECEIPT_INVALID');
  tamper((r) => { r.toolchain.gradle = { value: '9.1.0' }; }, 'RECEIPT_INVALID');
  tamper((r) => { r.leafResults[0].taskPath = ':foreign:test'; }, 'RECEIPT_INVALID');
  tamper((r) => { r.counts.discovered = 4; }, 'RECEIPT_INVALID');
  tamper((r) => { r.reportArtifacts[0].bytes = 70 * 1024 * 1024; }, 'RECEIPT_INVALID');
});

await check('started/terminal staging is strict in both kinds', () => {
  rejects(() => receiptContract.validateCommandReceipt(commandReceipt({ stage: 'started' })),
    'TestReceiptError', 'RECEIPT_INVALID');
  const started = commandReceipt({
    stage: 'started', startedReceiptHash: null, endedAt: null, durationMs: null,
    exitCode: null, disposition: 'pending',
    counts: { discovered: 0, executed: 0, passed: 0, failed: 0, skipped: 0, aborted: 0 },
    leafResults: [], discoveredTestIdentities: [], reportArtifacts: [],
    outputDigest: { bytes: 0, hash: H('8'), redacted: true }
  });
  assert.equal(receiptContract.validateCommandReceipt(started).stage, 'started');
  rejects(() => receiptContract.validateCommandReceipt(commandReceipt({
    stage: 'started', startedReceiptHash: null, endedAt: null, durationMs: null,
    exitCode: null, disposition: 'pending'
  })), 'TestReceiptError', 'RECEIPT_INVALID');
  rejects(() => receiptContract.validateStructuralReceipt(structuralReceipt({ result: 'pending' })),
    'TestReceiptError', 'RECEIPT_INVALID');
});

await check('structural receipts cannot impersonate executable evidence', () => {
  const valid = receiptContract.validateStructuralReceipt(structuralReceipt());
  assert.equal(valid.kind, 'test-structural-gate');
  assert.ok(!('counts' in valid) && !('discoveredTestIdentities' in valid) && !('lane' in valid),
    'the structural key set has no test-count surface at all');
  const smuggled = structuralReceipt();
  smuggled.counts = { discovered: 5, executed: 5, passed: 5, failed: 0, skipped: 0, aborted: 0 };
  smuggled.receiptHash = receiptContract.receiptHashOf(smuggled);
  rejects(() => receiptContract.validateStructuralReceipt(smuggled), 'TestReceiptError', 'RECEIPT_INVALID');
  rejects(() => receiptContract.validateStructuralReceipt(structuralReceipt({ validatorCodeHash: null })),
    'TestReceiptError', 'RECEIPT_INVALID');
  rejects(() => receiptContract.validateStructuralReceipt(structuralReceipt({ executionMode: 'external', pid: null, processGroup: null })),
    'TestReceiptError', 'RECEIPT_INVALID');
  rejects(() => receiptContract.validateStructuralReceipt(structuralReceipt({
    executionMode: 'external', validatorCodeHash: null, pid: 4242, processGroup: 4242,
    exitCode: 1, timedOut: true, result: 'passed'
  })), 'TestReceiptError', 'RECEIPT_INVALID');
});

await check('flaky fail→pass retries and all-skipped runs are typed violations', () => {
  const flaky = receiptContract.evaluateCommandReceipt(commandReceipt({
    retryHistory: [{ reason: 'diagnostic-rerun', priorReceiptHash: H('a') }]
  }), { testsRequired: true });
  assert.equal(flaky.passed, false);
  assert.ok(flaky.violations.includes('flaky-fail-then-pass'));

  const allSkipped = receiptContract.evaluateCommandReceipt(commandReceipt({
    counts: { discovered: 3, executed: 0, passed: 0, failed: 0, skipped: 3, aborted: 0 }
  }), { testsRequired: true });
  assert.equal(allSkipped.passed, false);
  assert.ok(allSkipped.violations.includes('zero-tests-executed'));

  const greenNoReports = receiptContract.evaluateCommandReceipt(commandReceipt({ reportArtifacts: [] }),
    { testsRequired: true });
  assert.ok(greenNoReports.violations.includes('green-exit-without-reports'));
});

function summary(overrides = {}) {
  const document = {
    version: 1,
    taskStem: 'TASK_5_save_note',
    runId: 'run-ev1',
    sessionId: 'sess-1',
    taskInputHash: H('1'),
    sourceSnapshotHash: H('2'),
    policyVersion: 1,
    policyHash: H('4'),
    plannedImpactHash: H('3'),
    observedImpactHash: H('3'),
    requiredLanes: ['host'],
    executedLanes: ['host'],
    anchorEvidence: [{
      anchor: 'test:save-note-button-disables',
      testIdentities: ['spike.CoreTest.adds'],
      receiptHashes: [H('a')],
      verified: true
    }],
    requiredSuites: ['save-note'],
    passedSuites: ['save-note'],
    fullSuiteRequired: false,
    fullSuiteResult: null,
    failBeforePassAfter: [],
    zeroTestVerdicts: [],
    flakyVerdicts: [],
    coverage: null,
    snapshotVerification: 'current',
    commandReceiptHashes: [H('a')],
    structuralReceiptHashes: [],
    verdict: 'PASS',
    verdictReasons: ['all-required-lanes-proven'],
    summaryHash: H('0'),
    ...overrides
  };
  document.summaryHash = summaryContract.summaryHashOf(document);
  return document;
}

await check('summary can never express an inconsistent green', () => {
  assert.equal(summaryContract.validateSummary(summary()).verdict, 'PASS');
  const cases = [
    [{ snapshotVerification: 'stale' }, 'VERDICT_INCONSISTENT'],
    [{ executedLanes: [] }, 'VERDICT_INCONSISTENT'],
    [{ passedSuites: [] }, 'VERDICT_INCONSISTENT'],
    [{ anchorEvidence: [{ anchor: 'test:save-note-button-disables', testIdentities: [], receiptHashes: [], verified: false }] }, 'VERDICT_INCONSISTENT'],
    [{ zeroTestVerdicts: [{ subject: ':lib:testAndroidHostTest', reason: 'zero-tests-discovered' }] }, 'VERDICT_INCONSISTENT'],
    [{ flakyVerdicts: [{ subject: 'spike.CoreTest.adds', reason: 'fail-then-pass' }] }, 'VERDICT_INCONSISTENT'],
    [{ fullSuiteRequired: true, fullSuiteResult: 'failed' }, 'VERDICT_INCONSISTENT'],
    [{ commandReceiptHashes: [] }, 'VERDICT_INCONSISTENT'],
    [{ fullSuiteResult: 'failed' }, 'VERDICT_INCONSISTENT'],
    [{ anchorEvidence: [{
      anchor: 'test:save-note-button-disables',
      testIdentities: 'spike.CoreTest.adds',
      receiptHashes: [H('a')],
      verified: true
    }] }, 'SUMMARY_INVALID']
  ];
  for (const [patch, code] of cases) {
    rejects(() => summaryContract.validateSummary(summary(patch)), 'TestSummaryError', code);
  }
  rejects(() => {
    const tampered = summary();
    tampered.verdict = 'FAIL';
    summaryContract.validateSummary(tampered);
  }, 'TestSummaryError', 'HASH_MISMATCH');
});

await check('SKIPPED is a proven typed N/A, never a synonym for skipping tests', () => {
  const skipped = summary({
    verdict: 'SKIPPED',
    verdictReasons: ['test-not-applicable-documentation-only'],
    requiredLanes: [], executedLanes: [], requiredSuites: [], passedSuites: [],
    anchorEvidence: [], commandReceiptHashes: [], structuralReceiptHashes: [H('b')]
  });
  assert.equal(summaryContract.validateSummary(skipped).verdict, 'SKIPPED');
  rejects(() => summaryContract.validateSummary(summary({
    verdict: 'SKIPPED', verdictReasons: ['test-not-applicable-documentation-only']
  })), 'TestSummaryError', 'VERDICT_INCONSISTENT');
  rejects(() => summaryContract.validateSummary(summary({
    verdict: 'SKIPPED',
    verdictReasons: ['builder-said-so'],
    requiredLanes: [], executedLanes: [], requiredSuites: [], passedSuites: [],
    anchorEvidence: [], commandReceiptHashes: [], structuralReceiptHashes: [H('b')]
  })), 'TestSummaryError', 'VERDICT_INCONSISTENT');
  rejects(() => summaryContract.validateSummary(summary({
    verdict: 'SKIPPED', verdictReasons: ['test-not-applicable-documentation-only'],
    requiredLanes: [], executedLanes: [], requiredSuites: [], passedSuites: [],
    anchorEvidence: [], commandReceiptHashes: [], structuralReceiptHashes: [H('b')],
    snapshotVerification: 'stale'
  })), 'TestSummaryError', 'VERDICT_INCONSISTENT');
  rejects(() => summaryContract.validateSummary(summary({
    verdict: 'SKIPPED', verdictReasons: ['test-not-applicable-documentation-only'],
    requiredLanes: [], executedLanes: [], requiredSuites: [], passedSuites: [],
    anchorEvidence: [], commandReceiptHashes: [], structuralReceiptHashes: [H('b')],
    fullSuiteRequired: true, fullSuiteResult: 'passed'
  })), 'TestSummaryError', 'VERDICT_INCONSISTENT');
});

await check('red/green bugfix pairs demand the expected assertion failure', () => {
  const withPair = summary({
    failBeforePassAfter: [{
      testIdentity: 'spike.BugTest.regression', failReceiptHash: H('c'), passReceiptHash: H('d'), expectedFailure: true
    }]
  });
  assert.equal(summaryContract.validateSummary(withPair).failBeforePassAfter.length, 1);
  rejects(() => summaryContract.validateSummary(summary({
    failBeforePassAfter: [{
      testIdentity: 'spike.BugTest.regression', failReceiptHash: H('c'), passReceiptHash: H('d'), expectedFailure: false
    }]
  })), 'TestSummaryError', 'SUMMARY_INVALID');
  rejects(() => summaryContract.validateSummary(summary({
    failBeforePassAfter: [{
      testIdentity: 'spike.BugTest.regression', failReceiptHash: H('c'), passReceiptHash: H('c'), expectedFailure: true
    }]
  })), 'TestSummaryError', 'SUMMARY_INVALID');
  rejects(() => summaryContract.validateSummary(summary({
    failBeforePassAfter: [{
      testIdentity: '', failReceiptHash: H('c'), passReceiptHash: H('d'), expectedFailure: true
    }]
  })), 'TestSummaryError', 'SUMMARY_INVALID');
});

await check('receipts and summaries accept any id the canonical orchestrator lock can carry', () => {
  // task-state-core.cjs LOCK_SESSION_ID_RE tops out at 163 characters. A
  // narrower receipt grammar would reject a legal session only AFTER the
  // build it just started, stranding that process group.
  const sessionId = 'ws-' + 'a'.repeat(160);
  assert.equal(sessionId.length, 163);
  assert.equal(receiptContract.validateCommandReceipt(commandReceipt({ sessionId })).sessionId, sessionId);
  assert.equal(receiptContract.validateStructuralReceipt(structuralReceipt({ sessionId })).sessionId, sessionId);
  assert.equal(summaryContract.validateSummary(summary({ sessionId })).sessionId, sessionId);
  rejects(() => receiptContract.validateCommandReceipt(commandReceipt({ sessionId: 'ws-' + 'a'.repeat(161) })),
    'TestReceiptError', 'RECEIPT_INVALID');
});

await check('hash domains are frozen literals and never cross-comparable', () => {
  assert.equal(receiptContract.COMMAND_DOMAIN, 'test-command-receipt');
  assert.equal(receiptContract.STRUCTURAL_DOMAIN, 'test-structural-receipt');
  assert.equal(summaryContract.SUMMARY_DOMAIN, 'test-summary');
  const impactContract = require('../task-test-impact-contract.cjs');
  assert.equal(impactContract.IMPACT_DOMAIN, 'test-impact');
  const domains = [receiptContract.COMMAND_DOMAIN, receiptContract.STRUCTURAL_DOMAIN,
    summaryContract.SUMMARY_DOMAIN, impactContract.IMPACT_DOMAIN];
  assert.equal(new Set(domains).size, domains.length, 'every artifact family owns its own namespace');
  const parsed = registry.parseReceiptId('test-command:' + H('a'));
  assert.deepEqual(parsed, { kind: 'test-command', hash: H('a') });
  assert.throws(() => registry.parseReceiptId('test-command:' + 'a'.repeat(64)), /canonical receipt id/);
});

await check('the registry speaks only for test kinds and refuses foreign or re-keyed ids', () => {
  const receipt = commandReceipt();
  const id = registry.receiptIdOf('test-command', receipt.receiptHash);
  assert.equal(registry.verifyReceiptId(id, () => receipt).verified, true);
  assert.equal(registry.verifyReceiptId(id, () => null).code, 'RECEIPT_MISSING');
  rejects(() => registry.receiptIdOf('figma-action', H('a')), 'ReceiptRegistryError', 'RECEIPT_ID_INVALID');
  const adapter = registry.checkpointReceiptVerifier(() => receipt);
  assert.deepEqual(adapter('figma:' + H('a')), { ok: false, code: 'unknown-receipt-kind' });
  assert.equal(adapter(id).ok, true);
  const started = commandReceipt({
    stage: 'started', startedReceiptHash: null, endedAt: null, durationMs: null,
    exitCode: null, disposition: 'pending',
    counts: { discovered: 0, executed: 0, passed: 0, failed: 0, skipped: 0, aborted: 0 },
    leafResults: [], discoveredTestIdentities: [], reportArtifacts: [],
    outputDigest: { bytes: 0, hash: H('8'), redacted: true }
  });
  const startedId = registry.receiptIdOf('test-command', started.receiptHash);
  assert.equal(registry.verifyReceiptId(startedId, () => started).code, 'RECEIPT_NOT_TERMINAL',
    'a started receipt is never reusable evidence');
  const summaryDoc = summary();
  const summaryId = registry.receiptIdOf('test-summary', summaryDoc.summaryHash);
  const isolated = registry.verifyReceiptId(summaryId, (kind) => kind === 'test-summary' ? summaryDoc : null);
  assert.equal(isolated.verified, false, 'a self-hashed summary without its evidence graph is never verified');
  assert.notEqual(isolated.code, 'RECEIPT_VERIFIED');
});

if (failures.length > 0) {
  console.error(`test-evidence: ${failures.length}/${checks} checks failed`);
  process.exit(1);
}
console.log(`test-evidence: ${checks} checks passed`);
