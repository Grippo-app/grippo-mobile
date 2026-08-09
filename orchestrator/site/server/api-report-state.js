'use strict';

// ---------------------------------------------------------------------------
// Read-only report state used by the current API workbench and task summary.
// The server NEVER calls the backend here: drift and endpoint-planning reports
// are local bounded artifacts published by the typed sidecar.
// ---------------------------------------------------------------------------

var fs = require('fs');
var path  = require('path');
var paths = require('./paths');
var fileGuards = require('./file-guards');

var REPORTS_DIR   = path.join(paths.API_CONTRACT_CACHE_DIR, 'reports');
var REPORT_MAX_BYTES = 16 * 1024 * 1024;

// Missing optional reports and corrupt reports are different states. A
// malformed/unsafe artifact must never be projected as a successful empty
// report because that would hide a broken local comparison result.
function readJson(file) {
  var stat;
  try {
    stat = fs.lstatSync(file);
  } catch (error) {
    return error && error.code === 'ENOENT'
      ? { state: 'missing', value: null }
      : { state: 'invalid', value: null };
  }
  if (!stat.isFile() || stat.isSymbolicLink() || String(stat.nlink) !== '1') {
    return { state: 'invalid', value: null };
  }
  try {
    var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, REPORTS_DIR, file, REPORT_MAX_BYTES);
    if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') return { state: 'invalid', value: null };
    var obj = JSON.parse(hit.bytes.toString('utf8'));
    return obj && typeof obj === 'object' && !Array.isArray(obj)
      ? { state: 'present', value: obj }
      : { state: 'invalid', value: null };
  } catch (error) {
    return { state: 'invalid', value: null };
  }
}

// The most-recent drift report (gitignored runtime file under .cache/api-contract/reports/; the dir is created at runtime).
function readDrift() {
  var result = readJson(path.join(REPORTS_DIR, 'drift.json'));
  if (result.state === 'missing') return { present: false };
  if (result.state === 'invalid') return { present: false, invalid: true, error: 'contract-drift-invalid' };
  return Object.assign({}, result.value, { present: true });
}

// The endpoint-coverage plan (.cache/api-contract/reports/suggested-endpoints.json — left behind by a
// contract:suggest run, which the contract:diff session also triggers). Per-endpoint
// state (not-implemented / drift / implemented) + the ready-to-create taskTitle/taskBody
// the API panel's Endpoints tab turns into a backlog task. Suggestion-only, gitignored
// contents; { present:false } until the planner has run. Like readDrift: never calls the
// backend, just reads the local plan file.
function readCoverage() {
  var result = readJson(path.join(REPORTS_DIR, 'suggested-endpoints.json'));
  if (result.state === 'missing') return { present: false };
  if (result.state === 'invalid') return { present: false, invalid: true, error: 'contract-coverage-invalid' };
  return Object.assign({}, result.value, { present: true });
}

module.exports = {
  readDrift: readDrift,
  readCoverage: readCoverage
};
