'use strict';

// ---------------------------------------------------------------------------
// Sealed test-summary contract (improvement 05, Phase 3). Validates the
// aggregate certification summary against test-summary.schema.json semantics,
// recomputes the domain hash (`test-summary\0`) and enforces the verdict
// grammar intra-document:
//   PASS    — snapshot current, every required lane executed, every required
//             suite passed, every anchor verified, no zero-test/flaky entries,
//             full suite passed when required, at least one command receipt;
//   SKIPPED — a proven typed N/A: no lanes/suites/anchors/command receipts,
//             at least one structural receipt;
//   BLOCKED/FAIL — must carry the matching reason codes.
// Cross-artifact freshness and receipt/report transitive verification are the
// deterministic aggregator's job; the finalizer rechecks its published
// product. This module guarantees the summary can never even EXPRESS an
// inconsistent green.
// ---------------------------------------------------------------------------

const crypto = require('crypto');

const SUMMARY_DOMAIN = 'test-summary';
const HASH_RE = /^sha256:[0-9a-f]{64}$/;
const LANES = Object.freeze(['android-device', 'common', 'host', 'ios-simulator', 'screenshot', 'structural']);
const VERDICTS = Object.freeze(['PASS', 'SKIPPED', 'BLOCKED', 'FAIL']);
const TOP_KEYS = Object.freeze([
  'anchorEvidence', 'commandReceiptHashes', 'coverage', 'executedLanes',
  'failBeforePassAfter', 'flakyVerdicts', 'fullSuiteRequired', 'fullSuiteResult',
  'observedImpactHash', 'passedSuites', 'plannedImpactHash', 'policyHash',
  'policyVersion', 'requiredLanes', 'requiredSuites', 'runId', 'sessionId',
  'snapshotVerification', 'sourceSnapshotHash', 'structuralReceiptHashes',
  'summaryHash', 'taskInputHash', 'taskStem', 'verdict', 'verdictReasons',
  'version', 'zeroTestVerdicts'
]);

class TestSummaryError extends Error {
  constructor(code, message) {
    super(code + ': ' + message);
    this.name = 'TestSummaryError';
    this.code = code;
  }
}

function fail(code, message) { throw new TestSummaryError(code, message); }

function sha256(value) {
  return 'sha256:' + crypto.createHash('sha256').update(value).digest('hex');
}

function canonicalJson(value) {
  if (value === null || typeof value !== 'object') return JSON.stringify(value);
  if (Array.isArray(value)) return '[' + value.map(canonicalJson).join(',') + ']';
  return '{' + Object.keys(value).sort().map((key) => JSON.stringify(key) + ':' + canonicalJson(value[key])).join(',') + '}';
}

function summaryHashOf(summary) {
  const model = {};
  for (const key of Object.keys(summary)) {
    if (key !== 'summaryHash') model[key] = summary[key];
  }
  return sha256(SUMMARY_DOMAIN + '\0' + canonicalJson(model));
}

function checkHashList(name, value, max) {
  if (!Array.isArray(value) || value.length > max) fail('SUMMARY_INVALID', name + ' bounds');
  const seen = new Set();
  for (const hash of value) {
    if (!HASH_RE.test(String(hash))) fail('SUMMARY_INVALID', name + ' entry grammar');
    if (seen.has(hash)) fail('SUMMARY_INVALID', name + ' duplicate');
    seen.add(hash);
  }
}

function checkLaneList(name, value) {
  if (!Array.isArray(value) || value.length > LANES.length) fail('SUMMARY_INVALID', name + ' bounds');
  const seen = new Set();
  for (const lane of value) {
    if (!LANES.includes(lane)) fail('SUMMARY_INVALID', name + ' unknown lane: ' + lane);
    if (seen.has(lane)) fail('SUMMARY_INVALID', name + ' duplicate lane');
    seen.add(lane);
  }
}

function checkIdentityList(name, value, max) {
  if (!Array.isArray(value) || value.length > max) fail('SUMMARY_INVALID', name + ' bounds');
  const seen = new Set();
  for (const identity of value) {
    if (typeof identity !== 'string' || identity.length === 0 || identity.length > 300) {
      fail('SUMMARY_INVALID', name + ' entry grammar');
    }
    if (seen.has(identity)) fail('SUMMARY_INVALID', name + ' duplicate');
    seen.add(identity);
  }
}

function validateSummary(summary) {
  if (!summary || typeof summary !== 'object' || Array.isArray(summary)) fail('SUMMARY_INVALID', 'summary must be an object');
  const keys = Object.keys(summary).sort();
  if (keys.length !== TOP_KEYS.length || keys.some((key, i) => key !== TOP_KEYS[i])) {
    fail('SUMMARY_INVALID', 'top-level keys must be exactly the v1 contract');
  }
  if (summary.version !== 1) fail('SUMMARY_INVALID', 'unsupported version');
  if (!/^TASK_[1-9][0-9]*_[A-Za-z0-9_]+$/.test(String(summary.taskStem))) fail('SUMMARY_INVALID', 'taskStem grammar');
  if (!/^run-[A-Za-z0-9][A-Za-z0-9-]{0,79}$/.test(String(summary.runId))) fail('SUMMARY_INVALID', 'runId grammar');
  if (!/^[A-Za-z0-9][A-Za-z0-9._:-]{0,162}$/.test(String(summary.sessionId))) fail('SUMMARY_INVALID', 'sessionId grammar');
  for (const name of ['taskInputHash', 'sourceSnapshotHash', 'policyHash', 'plannedImpactHash', 'observedImpactHash']) {
    if (!HASH_RE.test(String(summary[name]))) fail('SUMMARY_INVALID', name + ' grammar');
  }
  if (summary.policyVersion !== 1) fail('SUMMARY_INVALID', 'unsupported policyVersion');
  checkLaneList('requiredLanes', summary.requiredLanes);
  checkLaneList('executedLanes', summary.executedLanes);
  if (!Array.isArray(summary.anchorEvidence) || summary.anchorEvidence.length > 200) fail('SUMMARY_INVALID', 'anchorEvidence bounds');
  const seenAnchors = new Set();
  for (const evidence of summary.anchorEvidence) {
    const evidenceKeys = Object.keys(evidence || {}).sort();
    if (evidenceKeys.join(',') !== 'anchor,receiptHashes,testIdentities,verified') fail('SUMMARY_INVALID', 'anchor evidence keys');
    if (!/^test:[a-z0-9]+(?:-[a-z0-9]+)*$/.test(String(evidence.anchor))) fail('SUMMARY_INVALID', 'anchor grammar');
    if (seenAnchors.has(evidence.anchor)) fail('SUMMARY_INVALID', 'anchor duplicate');
    seenAnchors.add(evidence.anchor);
    checkHashList('anchor receiptHashes', evidence.receiptHashes, 16);
    checkIdentityList('anchor testIdentities', evidence.testIdentities, 64);
    if (typeof evidence.verified !== 'boolean') fail('SUMMARY_INVALID', 'anchor verified flag');
    if (evidence.verified && (evidence.testIdentities.length === 0 || evidence.receiptHashes.length === 0)) {
      fail('SUMMARY_INVALID', 'a verified anchor requires identities and receipts');
    }
  }
  for (const name of ['requiredSuites', 'passedSuites']) {
    if (!Array.isArray(summary[name]) || summary[name].length > 64) fail('SUMMARY_INVALID', name + ' bounds');
    const seen = new Set();
    for (const suite of summary[name]) {
      if (!/^[a-z][a-z0-9-]{0,79}$/.test(String(suite))) fail('SUMMARY_INVALID', name + ' grammar');
      if (seen.has(suite)) fail('SUMMARY_INVALID', name + ' duplicate');
      seen.add(suite);
    }
  }
  if (typeof summary.fullSuiteRequired !== 'boolean') fail('SUMMARY_INVALID', 'fullSuiteRequired');
  if (summary.fullSuiteResult !== null && !['passed', 'failed', 'blocked'].includes(summary.fullSuiteResult)) {
    fail('SUMMARY_INVALID', 'fullSuiteResult grammar');
  }
  if (!Array.isArray(summary.failBeforePassAfter) || summary.failBeforePassAfter.length > 64) fail('SUMMARY_INVALID', 'failBeforePassAfter bounds');
  for (const pair of summary.failBeforePassAfter) {
    const pairKeys = Object.keys(pair || {}).sort();
    if (pairKeys.join(',') !== 'expectedFailure,failReceiptHash,passReceiptHash,testIdentity') fail('SUMMARY_INVALID', 'red/green pair keys');
    if (!HASH_RE.test(String(pair.failReceiptHash)) || !HASH_RE.test(String(pair.passReceiptHash))) fail('SUMMARY_INVALID', 'red/green pair hashes');
    if (pair.failReceiptHash === pair.passReceiptHash) fail('SUMMARY_INVALID', 'red/green receipts must be distinct');
    if (typeof pair.testIdentity !== 'string' || pair.testIdentity.length === 0 || pair.testIdentity.length > 300) {
      fail('SUMMARY_INVALID', 'red/green testIdentity grammar');
    }
    if (pair.expectedFailure !== true) fail('SUMMARY_INVALID', 'a red run must be the expected assertion failure, not setup noise');
  }
  for (const name of ['zeroTestVerdicts', 'flakyVerdicts']) {
    if (!Array.isArray(summary[name]) || summary[name].length > 64) fail('SUMMARY_INVALID', name + ' bounds');
    for (const entry of summary[name]) {
      const entryKeys = Object.keys(entry || {}).sort();
      if (entryKeys.join(',') !== 'reason,subject') fail('SUMMARY_INVALID', name + ' keys');
      if (!/^[a-z][a-z0-9_-]{0,79}$/.test(String(entry.reason))) fail('SUMMARY_INVALID', name + ' reason grammar');
      if (typeof entry.subject !== 'string' || entry.subject.length === 0 || entry.subject.length > 300) {
        fail('SUMMARY_INVALID', name + ' subject grammar');
      }
    }
  }
  if (summary.coverage !== null) {
    const coverageKeys = Object.keys(summary.coverage || {}).sort();
    if (coverageKeys.join(',') !== 'label,reportHash') fail('SUMMARY_INVALID', 'coverage keys');
    if (summary.coverage.label !== 'jvm-host-coverage') fail('SUMMARY_INVALID', 'coverage label is jvm-host-coverage only');
    if (!HASH_RE.test(String(summary.coverage.reportHash))) fail('SUMMARY_INVALID', 'coverage report hash');
  }
  if (!['current', 'stale'].includes(summary.snapshotVerification)) fail('SUMMARY_INVALID', 'snapshotVerification grammar');
  checkHashList('commandReceiptHashes', summary.commandReceiptHashes, 256);
  checkHashList('structuralReceiptHashes', summary.structuralReceiptHashes, 64);
  if (!VERDICTS.includes(summary.verdict)) fail('SUMMARY_INVALID', 'unknown verdict');
  if (!Array.isArray(summary.verdictReasons) || summary.verdictReasons.length === 0 || summary.verdictReasons.length > 64) {
    fail('SUMMARY_INVALID', 'verdictReasons bounds');
  }
  for (const reason of summary.verdictReasons) {
    if (!/^[a-z][a-z0-9_-]{0,79}$/.test(String(reason))) fail('SUMMARY_INVALID', 'verdict reason grammar');
  }

  // Verdict grammar — a summary can never express an inconsistent green.
  if (summary.verdict === 'PASS') {
    if (summary.snapshotVerification !== 'current') fail('VERDICT_INCONSISTENT', 'PASS requires a current snapshot');
    for (const lane of summary.requiredLanes) {
      if (!summary.executedLanes.includes(lane)) fail('VERDICT_INCONSISTENT', 'PASS with unexecuted required lane: ' + lane);
    }
    for (const suite of summary.requiredSuites) {
      if (!summary.passedSuites.includes(suite)) fail('VERDICT_INCONSISTENT', 'PASS with unpassed required suite: ' + suite);
    }
    if (summary.anchorEvidence.some((evidence) => !evidence.verified)) fail('VERDICT_INCONSISTENT', 'PASS with an unverified anchor');
    if (summary.zeroTestVerdicts.length > 0) fail('VERDICT_INCONSISTENT', 'PASS with zero-test findings');
    if (summary.flakyVerdicts.length > 0) fail('VERDICT_INCONSISTENT', 'PASS with flaky findings');
    if (summary.fullSuiteRequired && summary.fullSuiteResult !== 'passed') fail('VERDICT_INCONSISTENT', 'PASS without the required full suite');
    if (summary.fullSuiteResult !== null && summary.fullSuiteResult !== 'passed') {
      fail('VERDICT_INCONSISTENT', 'PASS cannot retain a failed or blocked full-suite result');
    }
    if (summary.commandReceiptHashes.length === 0) fail('VERDICT_INCONSISTENT', 'PASS requires command receipts');
  }
  if (summary.verdict === 'SKIPPED') {
    if (summary.commandReceiptHashes.length > 0 || summary.anchorEvidence.length > 0 ||
        summary.requiredLanes.length > 0 || summary.requiredSuites.length > 0) {
      fail('VERDICT_INCONSISTENT', 'SKIPPED is a typed N/A, never a synonym for skipping tests');
    }
    if (summary.structuralReceiptHashes.length === 0) fail('VERDICT_INCONSISTENT', 'SKIPPED requires structural-gate receipts');
    if (summary.snapshotVerification !== 'current') fail('VERDICT_INCONSISTENT', 'SKIPPED requires a current snapshot');
    if (summary.fullSuiteRequired || summary.fullSuiteResult !== null) {
      fail('VERDICT_INCONSISTENT', 'SKIPPED cannot carry a full-suite requirement or result');
    }
    if (summary.failBeforePassAfter.length > 0 || summary.zeroTestVerdicts.length > 0 ||
        summary.flakyVerdicts.length > 0 || summary.coverage !== null) {
      fail('VERDICT_INCONSISTENT', 'SKIPPED cannot carry executable test evidence');
    }
    if (!summary.verdictReasons.some((reason) => reason.startsWith('test-not-applicable'))) {
      fail('VERDICT_INCONSISTENT', 'SKIPPED must carry the typed N/A reason');
    }
  }
  if (!HASH_RE.test(String(summary.summaryHash))) fail('SUMMARY_INVALID', 'summaryHash grammar');
  if (summaryHashOf(summary) !== summary.summaryHash) fail('HASH_MISMATCH', 'summaryHash does not match content');
  return Object.freeze(JSON.parse(JSON.stringify(summary)));
}

module.exports = {
  SUMMARY_DOMAIN,
  TestSummaryError,
  canonicalJson,
  summaryHashOf,
  validateSummary
};
