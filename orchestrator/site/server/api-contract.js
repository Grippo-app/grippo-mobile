'use strict';

// ---------------------------------------------------------------------------
// Project -> API read model (snapshot/drift/coverage freshness) for the
// /api/state `contract` slice.
//
// GOLDEN INVARIANT (same as Figma): this module NEVER calls the backend — no
// HTTP client, no probe loop. The live spec fetch happens only inside the typed
// backend sidecar; the interactive contract:diff session remains local-only.
// This module only reads the local artifacts those actions leave behind:
//   - the generation-aware committed snapshot,
//   - the drift report (.cache/api-contract/reports/drift.json, gitignored contents),
//   - the endpoint-coverage plan (.cache/api-contract/reports/suggested-endpoints.json,
//     gitignored) — its `generatedAt` is the freshness key the API panel's
//     current API readers watch so they re-fetch the moment a contract:suggest run
//     lands a new plan (the task summary reads the same bounded report state).
//
// Reads are cheap: the two optional reports are mtime-cached (re-parsed only
// when fs.lstatSync reports a new mtime), so the steady-state SSE poll costs
// two stat calls. Missing and invalid remain distinct public states.
// ---------------------------------------------------------------------------

var fs     = require('fs');
var path   = require('path');
var paths  = require('./paths');
var contractGeneration = require('./contract-generation');
var fileGuards = require('./file-guards');

var DRIFT_FILE     = path.join(paths.API_CONTRACT_CACHE_DIR, 'reports', 'drift.json');
var COVERAGE_FILE  = path.join(paths.API_CONTRACT_CACHE_DIR, 'reports', 'suggested-endpoints.json');
var REPORTS_DIR = path.dirname(DRIFT_FILE);
var REPORT_MAX_BYTES = 16 * 1024 * 1024;

// mtime-keyed JSON reader. Missing is an allowed not-yet-produced state;
// malformed, unsafe, oversized, array, and scalar artifacts are invalid.
function makeCachedReader(file, invalidCode) {
  var mtimeMs = -1;
  var cached = { state: 'missing', value: null, error: null };
  return function () {
    var st;
    try { st = fs.lstatSync(file); }
    catch (error) {
      mtimeMs = -1;
      cached = error && error.code === 'ENOENT'
        ? { state: 'missing', value: null, error: null }
        : { state: 'invalid', value: null, error: invalidCode };
      return cached;
    }
    if (!st.isFile() || st.isSymbolicLink() || String(st.nlink) !== '1') {
      mtimeMs = st.mtimeMs;
      cached = { state: 'invalid', value: null, error: invalidCode };
      return cached;
    }
    if (st.mtimeMs !== mtimeMs) {
      mtimeMs = st.mtimeMs;
      try {
        var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, REPORTS_DIR, file, REPORT_MAX_BYTES);
        if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') {
          cached = { state: 'invalid', value: null, error: invalidCode };
          return cached;
        }
        var obj = JSON.parse(hit.bytes.toString('utf8'));
        cached = obj && typeof obj === 'object' && !Array.isArray(obj)
          ? { state: 'present', value: obj, error: null }
          : { state: 'invalid', value: null, error: invalidCode };
      } catch (error) {
        cached = { state: 'invalid', value: null, error: invalidCode };
      }
    }
    return cached;
  };
}

var readDriftFile = makeCachedReader(DRIFT_FILE, 'contract-drift-invalid');
var readCoverageFile = makeCachedReader(COVERAGE_FILE, 'contract-coverage-invalid');

function str(v) { return typeof v === 'string' && v ? v : null; }

// Whole days since `iso`, clamped at 0; null when the stamp is missing or
// unparseable. Computed at call time (NOT cached) so a snapshot goes stale on
// the panel without the file ever changing.
function daysSince(iso) {
  if (typeof iso !== 'string' || !iso) return null;
  var t = Date.parse(iso);
  if (!isFinite(t)) return null;
  var d = Math.floor((Date.now() - t) / 86400000);
  return d > 0 ? d : 0;
}

// Compact freshness view of the generation-aware committed snapshot.
// { present:false } until a typed refresh has written one.
function snapshotStatus() {
  var generation = contractGeneration.current();
  if (!generation.ok) return { present: false, invalid: true, error: generation.error };
  var inv = generation.inventory;
  if (!inv) return { present: false };
  var src   = (inv.source && typeof inv.source === 'object') ? inv.source : {};
  var stats = (inv.stats && typeof inv.stats === 'object') ? inv.stats : {};
  var endpoints = (typeof stats.endpoints === 'number') ? stats.endpoints
                : (Array.isArray(inv.endpoints) ? inv.endpoints.length : 0);
  var areas = (typeof stats.areas === 'number') ? stats.areas
            : ((inv.areas && typeof inv.areas === 'object') ? Object.keys(inv.areas).length : 0);
  var fetchedAt = str(src.fetchedAt);
  return {
    present: true,
    fetchedAt: fetchedAt,
    specHash: generation.snapshotHash || str(src.specHash),
    sourceKind: str(src.kind),
    title: str(src.title),
    openApiVersion: str(src.openApiVersion),
    endpoints: endpoints,
    areas: areas,
    staleDays: daysSince(generation.manifest && generation.manifest.committedAt || fetchedAt),
    environmentId: generation.environmentId || null,
    generationId: generation.manifest && generation.manifest.generationId || null
  };
}

// Headline numbers of the most-recent drift report (.cache/api-contract/reports/drift.json — left
// behind by the backend-contract-drift validator / a contract:diff session).
function driftStatus() {
  var state = readDriftFile();
  if (state.state === 'missing') return { present: false };
  if (state.state === 'invalid') return { present: false, invalid: true, error: state.error };
  var rep = state.value;
  var sum = (rep.summary && typeof rep.summary === 'object') ? rep.summary : {};
  var n = function (x) { return (x | 0) || 0; };
  return {
    present: true,
    checkedAt: str(rep.checkedAt),
    errors: n(sum.errors),
    warnings: n(sum.warnings),
    infos: n(sum.infos)
  };
}

// Freshness view of the endpoint-coverage plan (.cache/api-contract/reports/suggested-endpoints.json —
// written by a contract:suggest run, which the contract:diff session also triggers).
// `generatedAt` is the current report key consumers use to re-fetch the
// full plan the instant a new plan lands — closing the
// stale-plan window where the drift report updated the slice but the plan did not.
function coverageStatus() {
  var state = readCoverageFile();
  if (state.state === 'missing') return { present: false };
  if (state.state === 'invalid') return { present: false, invalid: true, error: state.error };
  var plan = state.value;
  var sum = (plan.summary && typeof plan.summary === 'object') ? plan.summary : {};
  var n = function (x) { return (x | 0) || 0; };
  return {
    present: true,
    generatedAt: str(plan.generatedAt),
    notImplemented: n(sum.notImplemented),
    drift: n(sum.drift),
    implemented: n(sum.implemented),
    total: n(sum.total)
  };
}

// The full /api/state `contract` slice used by Project -> API.
function status() {
  return {
    snapshot: snapshotStatus(),
    drift: driftStatus(),
    coverage: coverageStatus()
  };
}

module.exports = {
  status: status
};
