'use strict';

// Server-verified projection of the sealed test-certification summary
// (improvement 05 §19.4). The Site NEVER computes PASS: this reader validates
// the sealed summary through the canonical contract and exposes a bounded
// typed status. Two staleness axes only (the existing display doctrine):
// integrity drift (invalid/forged/unreadable → blocking `invalid`) and input
// drift (a live re-verification of the sealed source manifest → advisory
// `stale` flag; the stored `snapshotVerification` word is not trusted alone).
// Raw report bytes are never served from here; reports stay behind the
// hash-bound byte gates. A done task without current evidence is invalid;
// there is no historical/legacy compatibility projection.

var path = require('path');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var summaryContract = require('../../tasks/task-test-summary-contract.cjs');
var receiptRegistry = require('../../tasks/task-receipt-registry.cjs');
var snapshotContract = require('../../tasks/content-snapshot.cjs');

var STATES = Object.freeze([
  'not-run', 'pass', 'skipped', 'fail', 'blocked', 'invalid'
]);
var RUN_RE = /^run-[A-Za-z0-9][A-Za-z0-9-]{0,79}$/;
var MAX_RUN_DIRS = 200;
var MAX_SUMMARY_BYTES = 1024 * 1024;
var MAX_REASONS = 8;
var AUTHORITY_ROOT = path.resolve(paths.TEST_CERTIFICATION_DIR, '..', '..');

function runLoader(stem, runId) {
  var base = path.join(paths.TEST_CERTIFICATION_DIR, stem, runId);
  return function load(kind, hash) {
    var fixed = {
      'test-summary': 'summary.json',
      'test-policy': 'policy.json',
      'source-snapshot': 'source-snapshot.json',
      'test-impact-planned': 'planned-impact.json',
      'test-impact-observed': 'observed-impact.json'
    };
    var candidates = [];
    if (fixed[kind]) candidates.push(path.join(base, fixed[kind]));
    else if (kind === 'test-command' || kind === 'test-structural-gate') {
      var family = kind === 'test-command' ? 'commands' : 'structural';
      var directory = path.join(base, family);
      var listing = fileGuards.boundedDirectoryNamesUnder(AUTHORITY_ROOT, directory, 641);
      if (!listing.ok || listing.exists === false || listing.names.length > 640) return null;
      var suffix = '-' + hash.slice('sha256:'.length) + '.json';
      listing.names.filter(function (name) { return name.endsWith(suffix); })
        .forEach(function (name) { candidates.push(path.join(directory, name)); });
    }
    if (candidates.length !== 1) return null;
    var file = candidates[0];
    var read = fileGuards.boundedRegularFileUnder(AUTHORITY_ROOT, path.dirname(file), file, 2 * 1024 * 1024);
    if (!read) return null;
    try { return JSON.parse(new TextDecoder('utf-8', { fatal: true }).decode(read.bytes)); }
    catch (_) { return null; }
  };
}

function unavailable(state, reason) {
  return { state: state, verdict: null, runId: null, summaryHash: null,
    requiredLanes: [], executedLanes: [], anchorsVerified: 0, anchorsTotal: 0,
    stale: false, reasons: reason ? [reason] : [] };
}

function readSummary(stem, runId) {
  var directory = path.join(paths.TEST_CERTIFICATION_DIR, stem, runId);
  var proof = fileGuards.realDirectoryUnder(AUTHORITY_ROOT, directory, { allowMissing: true });
  if (!proof) return { invalid: true };
  if (!proof.exists) return { missing: true };
  var file = path.join(directory, 'summary.json');
  var read = fileGuards.boundedRegularFileUnder(AUTHORITY_ROOT, directory, file, MAX_SUMMARY_BYTES);
  if (!read) return { invalid: true };
  var parsed;
  try { parsed = JSON.parse(new TextDecoder('utf-8', { fatal: true }).decode(read.bytes)); }
  catch (_) { return { invalid: true }; }
  try {
    var summary = summaryContract.validateSummary(parsed);
    // Preserve the typed foreign-identity projection while still refusing it
    // before any verdict can be exposed.
    if (summary.taskStem !== stem || summary.runId !== runId) return { summary: summary };
    var verified = receiptRegistry.verifyReceiptId(
      receiptRegistry.receiptIdOf('test-summary', summary.summaryHash), runLoader(stem, runId)
    );
    if (!verified.verified) return { invalid: true };
    var source = runLoader(stem, runId)('source-snapshot', verified.receipt.sourceSnapshotHash);
    var sourceVerification = snapshotContract.verifySnapshot(source, { root: paths.PROJECT_ROOT });
    return { summary: verified.receipt, sourceCurrent: sourceVerification.ok === true };
  }
  catch (_) { return { invalid: true }; }
}

// Latest run wins; registry verification reconstructs its evidence graph, so
// ordering only picks WHICH transitive sealed aggregate to project.
function latestRunId(stem) {
  var directory = path.join(paths.TEST_CERTIFICATION_DIR, stem);
  var listing = fileGuards.boundedDirectoryNamesUnder(AUTHORITY_ROOT, directory, MAX_RUN_DIRS + 1);
  if (!listing.ok) return { invalid: true };
  if (listing.exists === false) return null;
  var names = listing.names;
  var runs = names.filter(function (name) { return RUN_RE.test(name); }).sort();
  if (names.length > MAX_RUN_DIRS) return { overflow: true };
  return runs.length ? runs[runs.length - 1] : null;
}

function statusFor(stem, options) {
  options = options || {};
  if (!/^TASK_[1-9][0-9]*_[A-Za-z0-9_]+$/.test(String(stem || ''))) return unavailable('invalid', 'stem-invalid');
  var runId = options.runId || latestRunId(stem);
  if (runId && runId.invalid) return unavailable('invalid', 'run-directory-unsafe');
  if (runId && runId.overflow) return unavailable('invalid', 'run-directory-overflow');
  if (!runId) {
    return unavailable(options.doneTask ? 'invalid' : 'not-run',
      options.doneTask ? 'test-evidence-missing' : null);
  }
  if (!RUN_RE.test(runId)) return unavailable('invalid', 'run-id-invalid');
  var read = readSummary(stem, runId);
  if (read.missing) {
    return unavailable(options.doneTask ? 'invalid' : 'not-run',
      options.doneTask ? 'test-evidence-missing' : null);
  }
  if (read.invalid) return unavailable('invalid', 'summary-integrity-drift');
  var summary = read.summary;
  if (summary.taskStem !== stem) return unavailable('invalid', 'summary-foreign-task');
  if (summary.runId !== runId) return unavailable('invalid', 'summary-foreign-run');
  var state = summary.verdict === 'PASS' ? 'pass'
    : summary.verdict === 'SKIPPED' ? 'skipped'
    : summary.verdict === 'FAIL' ? 'fail' : 'blocked';
  return {
    state: state,
    verdict: summary.verdict,
    runId: summary.runId,
    summaryHash: summary.summaryHash,
    requiredLanes: summary.requiredLanes.slice(),
    executedLanes: summary.executedLanes.slice(),
    anchorsVerified: summary.anchorEvidence.filter(function (row) { return row.verified; }).length,
    anchorsTotal: summary.anchorEvidence.length,
    stale: summary.snapshotVerification !== 'current' || read.sourceCurrent !== true,
    reasons: summary.verdictReasons.slice(0, MAX_REASONS)
  };
}

module.exports = { STATES: STATES, statusFor: statusFor };
