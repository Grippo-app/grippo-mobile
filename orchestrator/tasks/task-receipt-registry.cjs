'use strict';

// ---------------------------------------------------------------------------
// Typed receipt-kind dispatcher (improvement 05, Phase 3) — the ONE adapter
// that closes the existing checkpoint `receiptVerifier` seam
// (orchestrator/site/server/task-checkpoints.js answers
// `receipt-verification-unavailable` fail-closed until a verifier is
// registered). There is exactly one registry: new receipt kinds are added
// HERE, never as a parallel registry, and the test verifier never impersonates
// a verifier for Figma/action receipt families (unknown kinds stay typed
// failures for their own owners to claim).
//
// A receipt id is content-addressed: `<kind>:<receiptHash>`. Verification
// re-validates the exact receipt bytes through the kind's contract and
// compares the recomputed hash against the id — a forged, truncated or
// re-keyed receipt can never verify.
// ---------------------------------------------------------------------------

const receiptContract = require('./task-test-receipt-contract.cjs');
const summaryContract = require('./task-test-summary-contract.cjs');
const impactContract = require('./task-test-impact-contract.cjs');
const policyContract = require('./task-test-policy-contract.cjs');
const snapshotContract = require('./content-snapshot.cjs');

const RECEIPT_ID_RE = /^(test-command|test-structural-gate|test-summary):sha256:[0-9a-f]{64}$/;

class ReceiptRegistryError extends Error {
  constructor(code, message) {
    super(code + ': ' + message);
    this.name = 'ReceiptRegistryError';
    this.code = code;
  }
}

function fail(code, message) { throw new ReceiptRegistryError(code, message); }

function receiptIdOf(kind, hash) {
  const id = kind + ':' + hash;
  if (!RECEIPT_ID_RE.test(id)) fail('RECEIPT_ID_INVALID', 'not a canonical receipt id: ' + id);
  return id;
}

function parseReceiptId(receiptId) {
  if (typeof receiptId !== 'string' || !RECEIPT_ID_RE.test(receiptId)) {
    fail('RECEIPT_ID_INVALID', 'not a canonical receipt id: ' + String(receiptId).slice(0, 120));
  }
  const separator = receiptId.indexOf(':');
  return { kind: receiptId.slice(0, separator), hash: receiptId.slice(separator + 1) };
}

function sortedUnique(values) { return [...new Set(values)].sort(); }
function same(left, right) { return receiptContract.canonicalJson(left) === receiptContract.canonicalJson(right); }

function receiptContext(receipt) {
  const base = {
    kind: receipt.kind, taskStem: receipt.taskStem, runId: receipt.runId,
    sessionId: receipt.sessionId, lockStage: receipt.lockStage,
    taskInputHash: receipt.taskInputHash, sourceSnapshotHash: receipt.sourceSnapshotHash,
    policyHash: receipt.policyHash, startedAt: receipt.startedAt,
    pid: receipt.pid, processGroup: receipt.processGroup
  };
  if (receipt.kind === 'test-command') return {
    ...base, impactHash: receipt.impactHash, suite: receipt.suite, tier: receipt.tier,
    lane: receipt.lane, taskPaths: receipt.taskPaths, cwd: receipt.cwd,
    envFingerprint: receipt.envFingerprint, toolchain: receipt.toolchain,
    executionRootKind: receipt.executionRootKind
  };
  return {
    ...base, gateId: receipt.gateId, executionMode: receipt.executionMode,
    tool: receipt.tool, validatorCodeHash: receipt.validatorCodeHash
  };
}

function linkedReceipts(kind, hashes, loadReceipt) {
  const validate = kind === 'test-command'
    ? receiptContract.validateCommandReceipt : receiptContract.validateStructuralReceipt;
  return hashes.map((hash) => {
    const terminal = validate(loadReceipt(kind, hash));
    if (terminal.stage !== 'terminal' || terminal.receiptHash !== hash) {
      fail('SUMMARY_EVIDENCE_INVALID', 'summary references a non-terminal or re-keyed receipt');
    }
    const started = validate(loadReceipt(kind, terminal.startedReceiptHash));
    if (started.stage !== 'started' || started.receiptHash !== terminal.startedReceiptHash ||
        !same(receiptContext(started), receiptContext(terminal))) {
      fail('SUMMARY_EVIDENCE_INVALID', 'terminal receipt has no exact started proof');
    }
    return terminal;
  });
}

// Reconstruct every summary relationship from the sealed context snapshots
// and terminal receipts. A self-hashed PASS document with a missing receipt,
// foreign impact, invented anchor or downgraded tier is invalid downstream.
function verifySummaryEvidence(summary, loadReceipt) {
  const policy = policyContract.validatePolicy(loadReceipt('test-policy', summary.policyHash));
  const source = snapshotContract.validateManifest(loadReceipt('source-snapshot', summary.sourceSnapshotHash));
  const planned = impactContract.validateImpact(loadReceipt('test-impact-planned', summary.plannedImpactHash), { policy });
  const observed = impactContract.validateImpact(loadReceipt('test-impact-observed', summary.observedImpactHash), { policy });
  impactContract.checkWidening(planned, observed, { policy });
  if (policy.policyHash !== summary.policyHash || source.snapshotHash !== summary.sourceSnapshotHash ||
      planned.impactHash !== summary.plannedImpactHash || observed.impactHash !== summary.observedImpactHash ||
      planned.taskStem !== summary.taskStem || observed.taskStem !== summary.taskStem ||
      planned.runId !== summary.runId || observed.runId !== summary.runId ||
      planned.taskInputHash !== summary.taskInputHash || observed.taskInputHash !== summary.taskInputHash ||
      planned.sourceSnapshotHash !== source.snapshotHash || observed.sourceSnapshotHash !== source.snapshotHash) {
    fail('SUMMARY_EVIDENCE_INVALID', 'summary context snapshots do not bind the same task/run/input');
  }
  const commands = linkedReceipts('test-command', summary.commandReceiptHashes, loadReceipt);
  const structural = linkedReceipts('test-structural-gate', summary.structuralReceiptHashes, loadReceipt);
  for (const receipt of [...commands, ...structural]) {
    if (receipt.taskStem !== summary.taskStem || receipt.runId !== summary.runId ||
        receipt.sessionId !== summary.sessionId || receipt.taskInputHash !== summary.taskInputHash ||
        receipt.sourceSnapshotHash !== summary.sourceSnapshotHash || receipt.policyHash !== summary.policyHash ||
        (receipt.kind === 'test-command' && receipt.impactHash !== summary.observedImpactHash)) {
      fail('SUMMARY_EVIDENCE_INVALID', 'linked receipt belongs to another context');
    }
  }
  const evaluations = commands.map((receipt) => receiptContract.evaluateCommandReceipt(receipt, { testsRequired: true }));
  const successful = evaluations.filter((evaluation) => evaluation.passed).map((evaluation) => evaluation.receipt);
  const requiredLanes = sortedUnique(observed.behaviors.flatMap((behavior) => behavior.requiredLanes));
  const structuralOnly = observed.testNotApplicable === null &&
    commands.length === 0 &&
    observed.requiredSuites.length === 0 &&
    requiredLanes.length > 0 &&
    requiredLanes.every((lane) => lane === 'structural');
  const bootstrapReceipts = structural.filter((receipt) =>
    receipt.gateId === 'bootstrap-foundation-fixture' && receipt.result === 'passed');
  const executedLanes = sortedUnique([
    ...successful.map((receipt) => receipt.lane),
    ...(structural.some((receipt) => receipt.gateId === 'bootstrap-foundation-fixture' && receipt.result === 'passed')
      ? ['structural'] : [])
  ]);
  const passedSuites = sortedUnique(successful.map((receipt) => receipt.suite));
  const anchorEvidence = observed.behaviors.map((behavior) => {
    // Structural-only bootstrap tasks intentionally have no command receipts.
    // Reconstruct their anchor proof from the same sealed structural receipt
    // that aggregate-test-certification.mjs uses, otherwise a freshly sealed
    // PASS summary immediately becomes receipt-stale at the checkpoint gate.
    if (structuralOnly && behavior.requiredLanes.every((lane) => lane === 'structural')) {
      return {
        anchor: behavior.anchor,
        testIdentities: [...behavior.observedTestCases],
        receiptHashes: sortedUnique(bootstrapReceipts.map((receipt) => receipt.receiptHash)),
        verified: bootstrapReceipts.length === 1
      };
    }
    const proving = successful.filter((receipt) =>
      behavior.observedTestCases.some((identity) => receipt.discoveredTestIdentities.includes(identity)));
    const identities = sortedUnique(behavior.observedTestCases.filter((identity) =>
      proving.some((receipt) => receipt.discoveredTestIdentities.includes(identity))));
    return {
      anchor: behavior.anchor,
      testIdentities: identities,
      receiptHashes: sortedUnique(proving.map((receipt) => receipt.receiptHash)),
      verified: identities.length === behavior.observedTestCases.length && behavior.observedTestCases.length > 0
    };
  });
  const zeroReasons = new Set(['zero-tests-discovered', 'zero-tests-executed', 'all-tests-skipped',
    'no-source-with-required-tests', 'green-exit-without-reports']);
  const zeroTestVerdicts = evaluations.flatMap((evaluation) => evaluation.violations
    .filter((reason) => zeroReasons.has(reason)).map((reason) => ({ subject: evaluation.receipt.suite, reason })));
  const flakyVerdicts = evaluations.flatMap((evaluation) => evaluation.violations
    .filter((reason) => reason === 'flaky-fail-then-pass').map((reason) => ({ subject: evaluation.receipt.suite, reason })));
  const fullSuiteRuns = evaluations.filter((evaluation) => evaluation.receipt.tier === 'full-suite');
  const fullSuiteResult = fullSuiteRuns.length === 0 ? null
    : fullSuiteRuns.every((evaluation) => evaluation.passed) ? 'passed' : 'failed';
  const expectedFullSuiteResult = observed.fullSuiteRequired && fullSuiteResult === null ? 'blocked' : fullSuiteResult;
  const expectedStructural = observed.testNotApplicable !== null
    ? observed.notApplicableValidators
    : requiredLanes.includes('structural') ? ['bootstrap-foundation-fixture'] : [];
  const structuralByGate = new Map(structural.map((receipt) => [receipt.gateId, receipt]));
  const structuralExact = structural.length === expectedStructural.length &&
    expectedStructural.every((gateId) => structuralByGate.get(gateId)?.result === 'passed');
  if (!same(summary.commandReceiptHashes, [...summary.commandReceiptHashes].sort()) ||
      !same(summary.structuralReceiptHashes, [...summary.structuralReceiptHashes].sort()) ||
      !same(summary.requiredLanes, requiredLanes) || !same(summary.executedLanes, executedLanes) ||
      !same(summary.requiredSuites, observed.requiredSuites) || !same(summary.passedSuites, passedSuites) ||
      !same(summary.anchorEvidence, anchorEvidence) || !same(summary.zeroTestVerdicts, zeroTestVerdicts) ||
      !same(summary.flakyVerdicts, flakyVerdicts) || summary.fullSuiteRequired !== observed.fullSuiteRequired ||
      summary.fullSuiteResult !== expectedFullSuiteResult || summary.failBeforePassAfter.length !== 0 ||
      summary.coverage !== null) {
    fail('SUMMARY_EVIDENCE_INVALID', 'summary fields were not deterministically derived from linked evidence');
  }
  const snapshotCurrent = summary.snapshotVerification === 'current';
  const commandFailed = evaluations.some((evaluation) => !evaluation.passed);
  const lanesComplete = requiredLanes.every((lane) => executedLanes.includes(lane));
  const suitesComplete = observed.requiredSuites.every((suite) => passedSuites.includes(suite));
  const anchorsComplete = anchorEvidence.every((evidence) => evidence.verified);
  const fullSuiteComplete = !observed.fullSuiteRequired || fullSuiteResult === 'passed';
  const failBeforeComplete = !observed.behaviors.some((behavior) =>
    policy.changeKinds[behavior.changeKind].minimumEvidence.includes('fail-before-pass-after'));
  const coverageComplete = !observed.requiredCapabilities.includes('coverage');
  let expectedVerdict;
  let expectedReasons;
  if (observed.testNotApplicable !== null) {
    if (commands.length > 0 || !structuralExact || !snapshotCurrent) {
      expectedVerdict = structural.some((receipt) => receipt.result !== 'passed') ? 'FAIL' : 'BLOCKED';
      expectedReasons = [expectedVerdict === 'FAIL' ? 'structural-gate-failed' : 'missing-not-applicable-proof'];
    } else {
      expectedVerdict = 'SKIPPED';
      expectedReasons = ['test-not-applicable-' + observed.testNotApplicable];
    }
  } else if (commandFailed) {
    expectedVerdict = 'FAIL';
    expectedReasons = ['command-receipt-failed'];
  } else if (!snapshotCurrent || (!structuralOnly && commands.length === 0) || !lanesComplete || !suitesComplete ||
      !anchorsComplete || !fullSuiteComplete || !failBeforeComplete || !coverageComplete || !structuralExact) {
    expectedVerdict = 'BLOCKED';
    expectedReasons = [!snapshotCurrent ? 'source-snapshot-stale' : 'missing-required-test-evidence'];
  } else {
    expectedVerdict = 'PASS';
    expectedReasons = ['all-required-test-evidence-proven'];
  }
  if (summary.verdict !== expectedVerdict || !same(summary.verdictReasons, expectedReasons)) {
    fail('SUMMARY_EVIDENCE_INVALID', 'verdict was not deterministically derived from linked evidence');
  }
  return true;
}

// `loadReceipt(kind, hash)` is caller-supplied storage access (the registry
// owns dispatch, not I/O). For summaries it also resolves the sealed context
// kinds `test-policy`, `source-snapshot`, `test-impact-planned`, and
// `test-impact-observed`, plus both started and terminal receipt hashes.
function verifyReceiptId(receiptId, loadReceipt) {
  const { kind, hash } = parseReceiptId(receiptId);
  if (typeof loadReceipt !== 'function') fail('LOADER_REQUIRED', 'a receipt loader is required');
  const document = loadReceipt(kind, hash);
  if (document === null || document === undefined) {
    return { verified: false, code: 'RECEIPT_MISSING', kind, hash };
  }
  let validated;
  try {
    if (kind === 'test-command') validated = receiptContract.validateCommandReceipt(document);
    else if (kind === 'test-structural-gate') validated = receiptContract.validateStructuralReceipt(document);
    else validated = summaryContract.validateSummary(document);
  } catch (error) {
    return { verified: false, code: error.code || 'RECEIPT_INVALID', kind, hash, message: error.message };
  }
  const actualHash = kind === 'test-summary' ? validated.summaryHash : validated.receiptHash;
  if (actualHash !== hash) {
    return { verified: false, code: 'RECEIPT_ID_MISMATCH', kind, hash, actualHash };
  }
  if (kind === 'test-summary') {
    try { verifySummaryEvidence(validated, loadReceipt); }
    catch (error) {
      return { verified: false, code: error.code || 'SUMMARY_EVIDENCE_INVALID', kind, hash, message: error.message };
    }
  }
  if (kind !== 'test-summary' && validated.stage !== 'terminal') {
    return { verified: false, code: 'RECEIPT_NOT_TERMINAL', kind, hash };
  }
  return { verified: true, code: 'RECEIPT_VERIFIED', kind, hash, receipt: validated };
}

// Checkpoint seam adapter: the shape task-checkpoints.js expects from its
// registered `receiptVerifier`. Ids of foreign families are a typed refusal —
// this verifier only speaks for the test evidence kinds above.
function checkpointReceiptVerifier(loadReceipt) {
  return function verify(receiptId) {
    if (typeof receiptId !== 'string' || !RECEIPT_ID_RE.test(receiptId)) {
      return { ok: false, code: 'unknown-receipt-kind' };
    }
    const result = verifyReceiptId(receiptId, loadReceipt);
    return result.verified
      ? { ok: true, code: 'verified', kind: result.kind }
      : { ok: false, code: result.code.toLowerCase().replace(/_/g, '-'), kind: result.kind };
  };
}

module.exports = {
  RECEIPT_ID_RE,
  ReceiptRegistryError,
  receiptIdOf,
  parseReceiptId,
  verifyReceiptId,
  checkpointReceiptVerifier
};
