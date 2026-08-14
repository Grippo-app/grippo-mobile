#!/usr/bin/env node

// Deterministic receipt aggregator. This is the only code that can publish a
// test summary: it discovers every sealed receipt in the current run root,
// revalidates started -> terminal linkage and sealed report/output artifacts,
// rechecks the source snapshot and planned -> observed widening, and derives
// every summary field. No caller-supplied summary or verdict exists.

import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const receiptContract = require('./task-test-receipt-contract.cjs');
const summaryContract = require('./task-test-summary-contract.cjs');
const impactContract = require('./task-test-impact-contract.cjs');
const snapshotContract = require('./content-snapshot.cjs');
const registry = require('./task-receipt-registry.cjs');
const fileGuards = require('../site/server/file-guards');

const HASH_RE = /^sha256:[0-9a-f]{64}$/;
const RECEIPT_MAX_BYTES = 2 * 1024 * 1024;
const RECEIPT_MAX_COUNT = 640;

class TestAggregationError extends Error {
  constructor(code, message) {
    super(code + ': ' + message);
    this.name = 'TestAggregationError';
    this.code = code;
  }
}

function fail(code, message) { throw new TestAggregationError(code, message); }
function sha256(bytes) { return 'sha256:' + crypto.createHash('sha256').update(bytes).digest('hex'); }
function sortedUnique(values) { return [...new Set(values)].sort(); }

function realDirectory(directory, expectedParent = null) {
  const resolved = path.resolve(directory);
  let stat;
  try { stat = fs.lstatSync(resolved); }
  catch (error) { fail('CERTIFICATION_MISSING', resolved + ' (' + error.code + ')'); }
  if (stat.isSymbolicLink() || !stat.isDirectory()) fail('CERTIFICATION_UNSAFE', 'not a real directory: ' + resolved);
  const real = fs.realpathSync.native(resolved);
  if (expectedParent) {
    const rel = path.relative(expectedParent, real);
    if (!rel || rel === '..' || rel.startsWith('..' + path.sep) || path.isAbsolute(rel)) {
      fail('CERTIFICATION_UNSAFE', 'directory escapes its owner: ' + real);
    }
  }
  return real;
}

function readSealedJson(file, ownerRoot) {
  const bytes = readSealedBytes(file, ownerRoot, RECEIPT_MAX_BYTES);
  try { return JSON.parse(bytes.toString('utf8')); }
  catch (error) { fail('RECEIPT_INVALID', 'receipt is not JSON: ' + path.basename(file)); }
}

function readSealedBytes(file, ownerRoot, maxBytes = 64 * 1024 * 1024) {
  const resolved = path.resolve(file);
  const rel = path.relative(ownerRoot, resolved);
  if (!rel || rel === '..' || rel.startsWith('..' + path.sep) || path.isAbsolute(rel)) {
    fail('ARTIFACT_UNSAFE', 'artifact escapes run root');
  }
  const guarded = fileGuards.boundedRegularFileUnder(ownerRoot, path.dirname(resolved), resolved, maxBytes);
  if (!guarded) fail('ARTIFACT_UNSAFE', 'artifact or an ancestor changed while reading: ' + path.basename(file));
  return guarded.bytes;
}

function pairProjection(receipt) {
  const common = {
    kind: receipt.kind, taskStem: receipt.taskStem, runId: receipt.runId,
    sessionId: receipt.sessionId, lockStage: receipt.lockStage,
    taskInputHash: receipt.taskInputHash, sourceSnapshotHash: receipt.sourceSnapshotHash,
    policyHash: receipt.policyHash, startedAt: receipt.startedAt,
    pid: receipt.pid, processGroup: receipt.processGroup
  };
  if (receipt.kind === 'test-command') return {
    ...common, impactHash: receipt.impactHash, suite: receipt.suite, tier: receipt.tier,
    lane: receipt.lane, taskPaths: receipt.taskPaths, cwd: receipt.cwd,
    envFingerprint: receipt.envFingerprint, toolchain: receipt.toolchain,
    executionRootKind: receipt.executionRootKind
  };
  return {
    ...common, gateId: receipt.gateId, executionMode: receipt.executionMode,
    tool: receipt.tool, validatorCodeHash: receipt.validatorCodeHash
  };
}

function verifyArtifacts(runRoot, receipt, ordinal) {
  const reportsRoot = realDirectory(path.join(runRoot, 'reports'), runRoot);
  const outputName = receipt.kind === 'test-command'
    ? ordinal + '-output.log' : ordinal + '-structural-output.log';
  const output = readSealedBytes(path.join(reportsRoot, outputName), runRoot, 16 * 1024 * 1024);
  if (output.length !== receipt.outputDigest.bytes || sha256(output) !== receipt.outputDigest.hash) {
    fail('ARTIFACT_HASH_MISMATCH', 'output digest mismatch for receipt ' + receipt.receiptHash);
  }
  if (receipt.kind !== 'test-command') return;
  if (receipt.reportArtifacts.length === 0) {
    const xmlPath = path.join(reportsRoot, ordinal + '-xml');
    if (fs.existsSync(xmlPath) && fs.readdirSync(realDirectory(xmlPath, runRoot)).length > 0) {
      fail('ARTIFACT_SET_MISMATCH', 'sealed reports exist but the terminal receipt declares none');
    }
    return;
  }
  const xmlRoot = realDirectory(path.join(reportsRoot, ordinal + '-xml'), runRoot);
  const entries = fs.readdirSync(xmlRoot).sort();
  const available = entries.map((name) => {
    const bytes = readSealedBytes(path.join(xmlRoot, name), runRoot);
    return { name, bytes: bytes.length, hash: sha256(bytes) };
  });
  for (const artifact of receipt.reportArtifacts) {
    const basename = path.basename(artifact.path);
    const matches = available.filter((candidate) => candidate.name.endsWith('-' + basename) &&
      candidate.bytes === artifact.bytes && candidate.hash === artifact.hash);
    if (matches.length !== 1) {
      fail('ARTIFACT_HASH_MISMATCH', 'sealed report proof missing or ambiguous: ' + artifact.path);
    }
  }
  if (available.length !== receipt.reportArtifacts.length) {
    fail('ARTIFACT_SET_MISMATCH', 'sealed report set differs from the terminal receipt');
  }
}

function loadReceiptFamily(runRoot, family, validate) {
  const familyPath = path.join(runRoot, family);
  if (!fs.existsSync(familyPath)) return [];
  const directory = realDirectory(familyPath, runRoot);
  const names = fs.readdirSync(directory).sort();
  if (names.length > RECEIPT_MAX_COUNT) fail('RECEIPT_BOUNDS', family + ' receipt count exceeds the limit');
  const byHash = new Map();
  const byOrdinalStage = new Set();
  const records = [];
  for (const name of names) {
    const match = /^([0-9]{3})-(started|terminal)-([0-9a-f]{64})\.json$/.exec(name);
    if (!match) fail('RECEIPT_INVALID', 'unexpected receipt filename: ' + name);
    const document = validate(readSealedJson(path.join(directory, name), runRoot));
    if (document.stage !== match[2] || document.receiptHash !== 'sha256:' + match[3]) {
      fail('RECEIPT_FILENAME_MISMATCH', 'filename does not bind receipt content: ' + name);
    }
    const ordinalStage = match[1] + ':' + match[2];
    if (byOrdinalStage.has(ordinalStage) || byHash.has(document.receiptHash)) {
      fail('RECEIPT_DUPLICATE', 'duplicate receipt identity: ' + name);
    }
    byOrdinalStage.add(ordinalStage);
    byHash.set(document.receiptHash, document);
    records.push({ ordinal: match[1], receipt: document });
  }
  const terminals = [];
  for (const record of records.filter(({ receipt }) => receipt.stage === 'terminal')) {
    const started = byHash.get(record.receipt.startedReceiptHash);
    if (!started || started.stage !== 'started') fail('RECEIPT_LINK_MISSING', 'terminal receipt has no sealed started receipt');
    if (receiptContract.canonicalJson(pairProjection(started)) !==
        receiptContract.canonicalJson(pairProjection(record.receipt))) {
      fail('RECEIPT_LINK_MISMATCH', 'started and terminal receipt identity/evidence differ');
    }
    verifyArtifacts(runRoot, record.receipt, record.ordinal);
    terminals.push(record.receipt);
  }
  for (const record of records.filter(({ receipt }) => receipt.stage === 'started')) {
    if (!terminals.some((terminal) => terminal.startedReceiptHash === record.receipt.receiptHash)) {
      fail('RECEIPT_INCOMPLETE', 'started receipt has no terminal receipt: ' + record.receipt.receiptHash);
    }
  }
  return terminals.sort((left, right) => left.receiptHash.localeCompare(right.receiptHash));
}

function writeSummary(runRoot, summary) {
  const file = path.join(runRoot, 'summary.json');
  let fd;
  try { fd = fs.openSync(file, fs.constants.O_WRONLY | fs.constants.O_CREAT | fs.constants.O_EXCL | fs.constants.O_NOFOLLOW, 0o600); }
  catch (error) { fail('NO_CLOBBER', 'summary already exists or is unsafe (' + error.code + ')'); }
  try {
    fs.writeFileSync(fd, JSON.stringify(summary, null, 2) + '\n');
    fs.fsyncSync(fd);
    const stat = fs.fstatSync(fd, { bigint: true });
    if (!stat.isFile() || stat.nlink !== 1n) fail('CERTIFICATION_UNSAFE', 'summary is not a single-link regular file');
  } finally { fs.closeSync(fd); }
}

function publishContext(runRoot, name, document) {
  const file = path.join(runRoot, name);
  const bytes = Buffer.from(JSON.stringify(document, null, 2) + '\n');
  if (fs.existsSync(file)) {
    const existing = readSealedBytes(file, runRoot, 16 * 1024 * 1024);
    if (!existing.equals(bytes)) fail('CONTEXT_NO_CLOBBER', name + ' already exists with different bytes');
    return;
  }
  let fd;
  try { fd = fs.openSync(file, fs.constants.O_WRONLY | fs.constants.O_CREAT | fs.constants.O_EXCL | fs.constants.O_NOFOLLOW, 0o600); }
  catch (error) { fail('CONTEXT_NO_CLOBBER', name + ' cannot be published (' + error.code + ')'); }
  try {
    fs.writeFileSync(fd, bytes);
    fs.fsyncSync(fd);
    const stat = fs.fstatSync(fd, { bigint: true });
    if (!stat.isFile() || stat.nlink !== 1n) fail('CERTIFICATION_UNSAFE', name + ' is not a single-link regular file');
  } finally { fs.closeSync(fd); }
}

export function aggregateAndSeal(options) {
  const {
    certificationRoot, certificationOwnerRoot, productRoot, identity, taskInputHash,
    sourceManifest, policy, plannedImpact, observedImpact
  } = options;
  const evidenceOwnerRoot = certificationOwnerRoot || productRoot;
  const canonicalProductRoot = realDirectory(productRoot);
  const canonicalCertificationOwnerRoot = realDirectory(evidenceOwnerRoot);
  const canonicalCertificationRoot = realDirectory(certificationRoot, canonicalCertificationOwnerRoot);
  const taskRoot = realDirectory(path.join(canonicalCertificationRoot, identity.taskStem), canonicalCertificationRoot);
  const runRoot = realDirectory(path.join(taskRoot, identity.runId), taskRoot);
  const planned = impactContract.validateImpact(plannedImpact, { policy });
  const observed = impactContract.validateImpact(observedImpact, { policy });
  impactContract.checkWidening(planned, observed, { policy });
  if (planned.taskStem !== identity.taskStem || planned.runId !== identity.runId ||
      observed.taskStem !== identity.taskStem || observed.runId !== identity.runId ||
      observed.taskInputHash !== taskInputHash || planned.taskInputHash !== taskInputHash) {
    fail('IDENTITY_MISMATCH', 'impact identity does not match the active certification request');
  }
  const manifest = snapshotContract.validateManifest(sourceManifest);
  if (manifest.snapshotHash !== observed.sourceSnapshotHash || manifest.snapshotHash !== planned.sourceSnapshotHash) {
    fail('SNAPSHOT_MISMATCH', 'impact artifacts do not bind the supplied source snapshot');
  }
  const snapshot = snapshotContract.verifySnapshot(manifest, { root: canonicalProductRoot });

  const commands = loadReceiptFamily(runRoot, 'commands', receiptContract.validateCommandReceipt);
  const structural = loadReceiptFamily(runRoot, 'structural', receiptContract.validateStructuralReceipt);
  for (const receipt of [...commands, ...structural]) {
    if (receipt.taskStem !== identity.taskStem || receipt.runId !== identity.runId ||
        receipt.sessionId !== identity.sessionId || receipt.lockStage !== identity.lockStage ||
        receipt.taskInputHash !== taskInputHash || receipt.sourceSnapshotHash !== manifest.snapshotHash ||
        receipt.policyHash !== policy.policyHash) {
      fail('RECEIPT_CONTEXT_MISMATCH', 'receipt does not bind the active identity and snapshots: ' + receipt.receiptHash);
    }
    if (receipt.kind === 'test-command' && receipt.impactHash !== observed.impactHash) {
      fail('RECEIPT_CONTEXT_MISMATCH', 'command receipt binds a different observed impact');
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
    if (structuralOnly && behavior.requiredLanes.every((lane) => lane === 'structural')) {
      return {
        anchor: behavior.anchor,
        testIdentities: [...behavior.observedTestCases],
        receiptHashes: sortedUnique(bootstrapReceipts.map((receipt) => receipt.receiptHash)),
        verified: bootstrapReceipts.length === 1
      };
    }
    const proving = successful.filter((receipt) =>
      behavior.observedTestCases.some((identityValue) => receipt.discoveredTestIdentities.includes(identityValue)));
    const provenIdentities = sortedUnique(behavior.observedTestCases.filter((identityValue) =>
      proving.some((receipt) => receipt.discoveredTestIdentities.includes(identityValue))));
    return {
      anchor: behavior.anchor,
      testIdentities: provenIdentities,
      receiptHashes: sortedUnique(proving.map((receipt) => receipt.receiptHash)),
      verified: provenIdentities.length === behavior.observedTestCases.length && behavior.observedTestCases.length > 0
    };
  });
  const zeroReasons = new Set(['zero-tests-discovered', 'zero-tests-executed', 'all-tests-skipped',
    'no-source-with-required-tests', 'green-exit-without-reports']);
  const zeroTestVerdicts = evaluations.flatMap((evaluation) => evaluation.violations
    .filter((reason) => zeroReasons.has(reason))
    .map((reason) => ({ subject: evaluation.receipt.suite, reason })));
  const flakyVerdicts = evaluations.flatMap((evaluation) => evaluation.violations
    .filter((reason) => reason === 'flaky-fail-then-pass')
    .map((reason) => ({ subject: evaluation.receipt.suite, reason })));
  const fullSuiteRuns = evaluations.filter((evaluation) => evaluation.receipt.tier === 'full-suite');
  const fullSuiteResult = fullSuiteRuns.length === 0 ? null
    : fullSuiteRuns.every((evaluation) => evaluation.passed) ? 'passed' : 'failed';

  const snapshotCurrent = snapshot.ok;
  const commandFailed = evaluations.some((evaluation) => !evaluation.passed);
  const lanesComplete = requiredLanes.every((lane) => executedLanes.includes(lane));
  const suitesComplete = observed.requiredSuites.every((suite) => passedSuites.includes(suite));
  const anchorsComplete = anchorEvidence.every((evidence) => evidence.verified);
  const fullSuiteComplete = !observed.fullSuiteRequired || fullSuiteResult === 'passed';
  const failBeforeComplete = !observed.behaviors.some((behavior) =>
    policy.changeKinds[behavior.changeKind].minimumEvidence.includes('fail-before-pass-after'));
  const coverageComplete = !observed.requiredCapabilities.includes('coverage');
  const structuralByGate = new Map(structural.map((receipt) => [receipt.gateId, receipt]));
  const expectedStructural = observed.testNotApplicable !== null
    ? observed.notApplicableValidators
    : requiredLanes.includes('structural') ? ['bootstrap-foundation-fixture'] : [];
  const structuralExact = structural.length === expectedStructural.length &&
    expectedStructural.every((gateId) => structuralByGate.get(gateId)?.result === 'passed');

  let verdict;
  let verdictReasons;
  if (observed.testNotApplicable !== null) {
    if (commands.length > 0 || !structuralExact || !snapshotCurrent) {
      verdict = structural.some((receipt) => receipt.result !== 'passed') ? 'FAIL' : 'BLOCKED';
      verdictReasons = [verdict === 'FAIL' ? 'structural-gate-failed' : 'missing-not-applicable-proof'];
    } else {
      verdict = 'SKIPPED';
      verdictReasons = ['test-not-applicable-' + observed.testNotApplicable];
    }
  } else if (commandFailed) {
    verdict = 'FAIL';
    verdictReasons = ['command-receipt-failed'];
  } else if (!snapshotCurrent || (!structuralOnly && commands.length === 0) || !lanesComplete || !suitesComplete ||
      !anchorsComplete || !fullSuiteComplete || !failBeforeComplete || !coverageComplete || !structuralExact) {
    verdict = 'BLOCKED';
    verdictReasons = [!snapshotCurrent ? 'source-snapshot-stale' : 'missing-required-test-evidence'];
  } else {
    verdict = 'PASS';
    verdictReasons = ['all-required-test-evidence-proven'];
  }

  const summary = {
    version: 1,
    taskStem: identity.taskStem,
    runId: identity.runId,
    sessionId: identity.sessionId,
    taskInputHash,
    sourceSnapshotHash: manifest.snapshotHash,
    policyVersion: policy.version,
    policyHash: policy.policyHash,
    plannedImpactHash: planned.impactHash,
    observedImpactHash: observed.impactHash,
    requiredLanes,
    executedLanes,
    anchorEvidence,
    requiredSuites: [...observed.requiredSuites],
    passedSuites,
    fullSuiteRequired: observed.fullSuiteRequired,
    fullSuiteResult: observed.fullSuiteRequired && fullSuiteResult === null ? 'blocked' : fullSuiteResult,
    failBeforePassAfter: [],
    zeroTestVerdicts,
    flakyVerdicts,
    coverage: null,
    snapshotVerification: snapshotCurrent ? 'current' : 'stale',
    commandReceiptHashes: commands.map((receipt) => receipt.receiptHash),
    structuralReceiptHashes: structural.map((receipt) => receipt.receiptHash),
    verdict,
    verdictReasons,
    summaryHash: 'sha256:' + '0'.repeat(64)
  };
  summary.summaryHash = summaryContract.summaryHashOf(summary);
  const validated = summaryContract.validateSummary(summary);
  publishContext(runRoot, 'policy.json', policy);
  publishContext(runRoot, 'source-snapshot.json', manifest);
  publishContext(runRoot, 'planned-impact.json', planned);
  publishContext(runRoot, 'observed-impact.json', observed);
  writeSummary(runRoot, validated);
  return {
    summary: validated,
    receiptId: registry.receiptIdOf('test-summary', validated.summaryHash)
  };
}
