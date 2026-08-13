'use strict';

// Read-only adapter around the canonical task-state engine.  HTTP admission,
// queue execution and the diagnostics endpoint all call this module so they
// cannot drift into three different interpretations of a task column.

var core = require('../../tasks/task-state-core.cjs');
var paths = require('./paths');
var runtimeIntegrity = require('./runtime-integrity');

var PUBLIC_FINDINGS_MAX = 200;
var PUBLIC_STEMS_MAX = 1000;
var PUBLIC_PATHS_MAX = 20;
var PUBLIC_TEXT_MAX = 500;
var PUBLIC_RUNTIME_STATUSES_MAX = 500;
var CACHE_TTL_MS = 1500;
// 60s: the freshness probe re-parses the whole Kotlin tree (~30s wall on a real
// product repo); a 5s TTL kept it running back-to-back on every scan tick.
var ARCH_CACHE_TTL_MS = 60000;
var cachedAt = 0;
var cachedResult = null;
var archCachedAt = 0;
var archCachedResult = null;
var archCheckInFlight = null;

function unavailableArchitecture() {
  return {
    version: 1, checked: false, ok: false, status: 'unavailable', fresh: false,
    actualHash: null, expectedHash: null,
    findings: [{ code: 'ARCH_MAP_UNAVAILABLE', severity: 'warning', paths: [],
      message: 'Architecture freshness could not be checked safely.',
      recovery: 'Retry the read-only architecture check after its generator/runtime is available.' }]
  };
}

function options(extra) {
  return Object.assign({
    tasksDir: paths.TASKS_DIR,
    repoRoot: paths.PROJECT_ROOT,
    locksDir: paths.LOCKS_DIR,
    checkIndex: true,
    runtimeInspector: runtimeIntegrity.scanIntegrity
  }, extra || {});
}

function emitObservation(caller, result) {
  try {
    process.stderr.write('[task-state] ' + JSON.stringify(core.observationFor(result, {
      caller: caller || 'server',
      slowThresholdMs: 100
    })) + '\n');
  } catch (_) {
    // Observability is intentionally non-authoritative: validation must never
    // turn green or fail merely because stderr is unavailable.
  }
}

function observed(caller, seed, operation) {
  var started = Date.now();
  try {
    var result = operation();
    emitObservation(caller, Object.assign({}, result, {
      scope: result.scope || seed.stem || 'all',
      action: result.action || seed.action || null
    }));
    return result;
  } catch (error) {
    var rawCode = String(error && error.code || 'TASK_STATE_UNAVAILABLE');
    var safeCode = /^[A-Za-z0-9_.:-]{1,80}$/.test(rawCode) ? rawCode : 'TASK_STATE_UNAVAILABLE';
    emitObservation(caller, {
      version: core.VERSION,
      ok: false,
      overallOk: false,
      scope: seed.stem || 'all',
      action: seed.action || null,
      findings: [{ code: safeCode, severity: 'blocker' }],
      stats: { durationMs: Date.now() - started, scanMode: seed.stem ? 'stem-closure' : 'full', taskBodyReads: 0 }
    });
    throw error;
  }
}

function validateAction(action, stem, caller) {
  return observed(caller || 'server', { action: action, stem: stem }, function () {
    // Action admission must match validate-task-state.mjs: runtime owners are
    // part of the executable precondition, not diagnostics-only metadata.
    // Queue fences, HTTP admission and the CLI therefore hash and evaluate the
    // same locks, leases, finalization/transition markers and owner findings.
    return core.validateAction(options({ action: action, stem: stem, includeRuntime: true }));
  });
}

function validateAll(caller) {
  return observed(caller || 'server', { action: 'integrity-scan' }, function () {
    var result = core.validateTaskState(options({ includeRuntime: true }));
    var architecture = architectureStateCached();
    result.derivedState = { arch: architecture };
    result.derivedOk = architecture.ok;
    result.overallOk = result.ok && result.derivedOk;
    return result;
  });
}

function architectureStateCached() {
  var at = Date.now();
  if (archCachedResult && at - archCachedAt < ARCH_CACHE_TTL_MS) return archCachedResult;
  prewarmArchitectureState();
  return unavailableArchitecture();
}

// The architecture scan walks the product source tree and can take tens of
// seconds. Keep it entirely off the HTTP/SSE event loop. Until an initial or
// expired-cache prewarm settles, callers receive a strict unavailable verdict
// rather than a guessed green state.
function prewarmArchitectureState(force) {
  var at = Date.now();
  if (!force && archCachedResult && at - archCachedAt < ARCH_CACHE_TTL_MS) {
    return Promise.resolve(archCachedResult);
  }
  if (archCheckInFlight) return archCheckInFlight;
  archCheckInFlight = core.checkArchitectureStateAsync({ repoRoot: paths.PROJECT_ROOT })
    .catch(function () { return unavailableArchitecture(); })
    .then(function (result) {
      archCachedResult = result;
      archCachedAt = Date.now();
      // A prior hot-path snapshot may contain the strict unavailable verdict
      // emitted while this refresh was in flight. Retire it immediately so
      // the settled architecture result is visible on the next state read.
      cachedResult = null;
      cachedAt = 0;
      return result;
    })
    .finally(function () { archCheckInFlight = null; });
  return archCheckInFlight;
}

function inspectDrop(stem) {
  var result = validateAction('drop', stem, 'drop');
  var admission = core.dropAdmission(result, stem);
  return {
    result: result,
    admission: admission,
    impact: {
      version: 1,
      ok: admission.ok,
      operation: 'inspect-drop',
      stem: stem,
      state: result.observedState,
      sourceRevision: result.sourceRevision,
      // The corpus itself is capped at 10,000 files by the canonical scanner,
      // so Drop can return the complete dependent set without an unbounded
      // response. Never truncate a destructive-impact confirmation.
      dependents: Array.isArray(result.dependents) ? result.dependents.slice() : [],
      impactHash: result.sourceRevision && Array.isArray(result.dependents)
        ? core.dropImpactHash(stem, result.sourceRevision, result.dependents)
        : null,
      integrity: publicResult(result)
    }
  };
}

function actionAdmission(result) {
  return core.actionAdmission(result);
}

function dropAdmission(result, stem, options) {
  return core.dropAdmission(result, stem, options);
}

function admissionForAction(result, stem, options) {
  return core.admissionForAction(result, stem, options);
}

// `/api/state` is a hot SSE-derived path. Cache only that observational view;
// enqueue and execution admission always call validateAction() fresh and are
// never allowed to reuse this result.
function validateAllCached() {
  var at = Date.now();
  var architectureStillFresh = archCachedResult &&
    at - archCachedAt < ARCH_CACHE_TTL_MS;
  if (cachedResult && at - cachedAt < CACHE_TTL_MS && architectureStillFresh) {
    return cachedResult;
  }
  cachedResult = validateAll();
  cachedAt = at;
  return cachedResult;
}

function boundedText(value) {
  if (typeof value !== 'string') return null;
  var text = value;
  if (paths.PROJECT_ROOT && paths.PROJECT_ROOT !== '/') text = text.split(paths.PROJECT_ROOT).join('<project-root>');
  text = text.replace(/(^|[\s("'`:=])((?:[A-Za-z]:[\\/]|\/)[^\s"'`<>]*)/g, '$1<absolute-path>');
  return text.slice(0, PUBLIC_TEXT_MAX);
}

function publicPath(value) {
  return runtimeIntegrity.projectPath(value);
}

function publicFindingCode(value) {
  return typeof value === 'string' && /^[A-Z][A-Z0-9_]{2,79}$/.test(value)
    ? value : 'TASK_STATE_FINDING';
}

function publicSeverity(value) {
  return ['info', 'warning', 'error', 'blocker'].indexOf(value) >= 0 ? value : 'error';
}

function publicStats(value) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return null;
  var out = {};
  ['tasks', 'files', 'inventoryEntries', 'taskRelatedEntries', 'taskBodyReads', 'taskBodyBytes', 'durationMs'].forEach(function (key) {
    if (Number.isSafeInteger(value[key]) && value[key] >= 0) out[key] = value[key];
  });
  if (value.scanMode === 'full' || value.scanMode === 'stem-closure') out.scanMode = value.scanMode;
  return out;
}

function publicRuntimeStats(value) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return null;
  var out = { inspected: value.inspected === true, truncated: value.truncated === true };
  ['statuses', 'findings', 'snapshotInputs'].forEach(function (key) {
    out[key] = Number.isSafeInteger(value[key]) && value[key] >= 0 ? value[key] : 0;
  });
  return out;
}

// Findings inside the canonical engine may carry parser-only `details` such
// as a malformed bullet excerpt.  Those details are useful to the local CLI,
// but task bodies and answers are not part of the HTTP diagnostics contract.
// Copy only the fields the board needs and never forward unknown properties.
function publicFinding(item) {
  var pathsList = Array.isArray(item && item.paths) ? item.paths : [];
  var safePaths = pathsList.slice(0, PUBLIC_PATHS_MAX).map(publicPath).filter(Boolean);
  var result = {
    code: publicFindingCode(item && item.code),
    severity: publicSeverity(item && item.severity),
    stem: boundedText(item && item.stem),
    paths: safePaths,
    message: boundedText(item && item.message),
    recovery: boundedText(item && item.recovery)
  };
  if (pathsList.length > PUBLIC_PATHS_MAX) result.pathsTruncated = true;
  if (safePaths.length !== Math.min(pathsList.length, PUBLIC_PATHS_MAX)) result.pathsRedacted = true;
  return result;
}

// The core result deliberately keeps its parser model non-enumerable.  This
// additional copy bounds hot-path/API output and makes that privacy property
// explicit even if the core grows more internal fields later.
function publicResult(result) {
  var findings = Array.isArray(result && result.findings) ? result.findings : [];
  var stems = Array.isArray(result && result.affectedStems) ? result.affectedStems : [];
  var blockingCount = findings.filter(function (item) {
    return item && (item.severity === 'error' || item.severity === 'blocker');
  }).length;
  var pathsTruncated = findings.some(function (item) {
    return Array.isArray(item && item.paths) && (item.paths.length > PUBLIC_PATHS_MAX ||
      item.paths.slice(0, PUBLIC_PATHS_MAX).some(function (entry) { return !publicPath(entry); }));
  });
  var runtimeStatuses = Array.isArray(result && result.runtimeStatus) ? result.runtimeStatus : [];
  var arch = result && result.derivedState && result.derivedState.arch;
  var publicArch = arch ? {
    version: arch.version,
    checked: !!arch.checked,
    ok: !!arch.ok,
    status: boundedText(arch.status),
    fresh: !!arch.fresh,
    actualHash: boundedText(arch.actualHash),
    expectedHash: boundedText(arch.expectedHash),
    findings: (Array.isArray(arch.findings) ? arch.findings : []).slice(0, 20).map(publicFinding)
  } : null;
  return {
    version: result && result.version,
    ok: !!(result && result.ok),
    overallOk: result && result.overallOk === undefined ? !!(result && result.ok) : !!result.overallOk,
    derivedOk: result && result.derivedOk === undefined ? true : !!result.derivedOk,
    derivedState: { arch: publicArch },
    scope: result && result.scope,
    observedState: result && result.observedState,
    expectedState: result && result.expectedState,
    transition: result && result.transition,
    phase: result && result.phase,
    snapshotHash: result && result.snapshotHash,
    sourceRevision: result && result.sourceRevision,
    indexStatus: result && result.indexStatus,
    affectedStems: stems.slice(0, PUBLIC_STEMS_MAX),
    findings: findings.slice(0, PUBLIC_FINDINGS_MAX).map(publicFinding),
    findingCount: findings.length,
    blockingCount: blockingCount,
    affectedStemCount: stems.length,
    truncated: findings.length > PUBLIC_FINDINGS_MAX || stems.length > PUBLIC_STEMS_MAX || pathsTruncated,
    stats: publicStats(result && result.stats),
    runtimeStatus: runtimeStatuses.slice(0, PUBLIC_RUNTIME_STATUSES_MAX).map(function (row) {
      var safe = {};
      ['owner', 'kind', 'stem', 'state', 'action', 'requestId', 'key', 'leaseId', 'writerKind', 'operation', 'phase', 'createdAt', 'updatedAt', 'contentHash'].forEach(function (field) {
        if (typeof row[field] === 'string') safe[field] = boundedText(row[field]);
      });
      if (Number.isSafeInteger(row.revision)) safe.revision = row.revision;
      return safe;
    }),
    runtimeStatusCount: runtimeStatuses.length,
    runtimeStatusTruncated: runtimeStatuses.length > PUBLIC_RUNTIME_STATUSES_MAX,
    runtimeStats: publicRuntimeStats(result && result.runtimeStats)
  };
}

function errorResponse(error) {
  var exitCode = Number(error && error.exitCode);
  var detail = exitCode === 4
    ? 'Task-state inputs changed during validation; retry from a fresh snapshot.'
    : 'Required task-state authority is unavailable or malformed.';
  return {
    status: exitCode === 4 ? 503 : 500,
    body: {
      version: core.VERSION,
      ok: false,
      error: exitCode === 4 ? 'task-state-transient' : 'task-state-unavailable',
      detail: detail
    }
  };
}

function unavailableResult() {
  return {
    version: core.VERSION, ok: false, scope: 'all', observedState: null,
    expectedState: null, transition: null, phase: null,
    snapshotHash: null, sourceRevision: null, indexStatus: 'unavailable',
    affectedStems: [], findings: [{
      code: 'TASK_STATE_UNAVAILABLE', severity: 'blocker', stem: null, paths: [],
      message: 'Required task-state authority is unavailable or malformed.',
      recovery: 'Retry from a fresh snapshot or restore the task-state contract.'
    }], truncated: false, stats: null
  };
}

module.exports = {
  validateAction: validateAction,
  validateAll: validateAll,
  validateAllCached: validateAllCached,
  prewarmArchitectureState: prewarmArchitectureState,
  inspectDrop: inspectDrop,
  actionAdmission: actionAdmission,
  dropAdmission: dropAdmission,
  admissionForAction: admissionForAction,
  publicResult: publicResult,
  unavailableResult: unavailableResult,
  errorResponse: errorResponse,
  ACTION_STATES: core.ACTION_STATES,
  HASH_RE: core.HASH_RE,
  VERSION: core.VERSION
};
