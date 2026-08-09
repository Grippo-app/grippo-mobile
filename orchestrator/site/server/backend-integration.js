'use strict';

// Aggregated public model for Integrations -> Backend. This module performs
// local reads only and never exposes credential bytes or secret-derived data.

var path = require('path');
var environments = require('./backend-environments');
var credentials = require('./backend-credentials');
var generation = require('./contract-generation');
var history = require('./contract-history');
var persistence = require('./persistence');
var jobs = require('./contract-job');
var paths = require('./paths');
var writerLeases = require('../../tasks/writer-leases.cjs');
var resetActive = false;
var resetIdempotency = new Map();

function projectRelative(file) {
  return path.relative(paths.PROJECT_ROOT, file).split(path.sep).join('/');
}
function resolveActive(envState, persisted) {
  if (envState.mode !== 'manifest') return null;
  var selected = persisted.backendActiveEnvironmentId;
  var active = selected ? environments.environmentById(envState, selected) : null;
  return active || environments.environmentById(envState, envState.manifest.defaultEnvironmentId);
}
function snapshotSummary(snapshot) {
  if (!snapshot.ok) return { present: false, invalid: true, error: snapshot.error };
  if (snapshot.mode === 'none') return { present: false, invalid: false, hash: null, environmentId: null };
  var inventory = snapshot.inventory || {}, source = inventory.source || {}, stats = inventory.stats || {};
  return { present: true, invalid: false, mode: snapshot.mode, hash: snapshot.snapshotHash,
    environmentId: snapshot.environmentId, generationId: snapshot.manifest && snapshot.manifest.generationId || null,
    pulledAt: snapshot.manifest && snapshot.manifest.committedAt || source.fetchedAt || source.postmanImportedAt || null,
    sourceKind: snapshot.manifest && snapshot.manifest.sourceKind || source.kind || null,
    title: source.title || null, version: source.openApiVersion || null,
    endpoints: Number.isInteger(stats.endpoints) ? stats.endpoints : (Array.isArray(inventory.endpoints) ? inventory.endpoints.length : 0),
    areas: Number.isInteger(stats.areas) ? stats.areas : Object.keys(inventory.areas || {}).length };
}
function previewFor(active, envState, auth, snapshot, persisted) {
  if (!active) return null;
  var report = history.latest('probe', active.id);
  if (!report) return null;
  var fresh = report.state === 'success' && report.environmentRevision === envState.revision && report.authRevision === auth.revision &&
    report.selectionRevision === (persisted.backendSelectionRevision || 0) &&
    (report.snapshotFingerprint || null) === (snapshot.snapshotHash || null) && Number.isFinite(Date.parse(report.expiresAt)) && Date.parse(report.expiresAt) > Date.now();
  return Object.assign({}, report, { fresh: fresh, staleReason: fresh ? null : 'preview-stale' });
}
function snapshotFailure(snapshot) {
  if (!snapshot || !snapshot.invalid) return null;
  return { code: 'snapshot-invalid', message: 'The saved contract generation is invalid.' };
}
function reportFailure(report) {
  return report && (report.state === 'failed' || report.state === 'interrupted') && report.error ? report.error : null;
}
function lastFailure(snapshot, preview, latestRefresh) {
  return reportFailure(preview) || reportFailure(latestRefresh) || snapshotFailure(snapshot);
}
function overallState(envState, active, auth, snapshot, preview, latestRefresh, activeJobs) {
  if (activeJobs.some(function (job) { return job.reportType === 'refresh'; })) return 'refreshing';
  if (envState.mode === 'invalid' || !active) return 'needs-setup';
  if (auth.state === 'missing' || auth.state === 'invalid') return 'needs-setup';
  var failure = lastFailure(snapshot, preview, latestRefresh);
  if (failure && ['source-unreachable', 'auth-rejected'].indexOf(failure.code) >= 0) return 'source-unavailable';
  if (failure) return 'attention-required';
  if (preview && preview.fresh && (!snapshot.present || preview.delta &&
      (preview.delta.added || preview.delta.changed || preview.delta.removed))) return 'changes-available';
  if (!snapshot.present) return 'needs-test';
  return 'ready';
}

function get() {
  var envState = environments.read(), persisted = persistence.readPersisted();
  var active = resolveActive(envState, persisted);
  var auth = active ? credentials.publicStatus(active) : {
    state: active ? (active.authRef ? 'unknown' : 'not-required') : 'unknown',
    kind: active && active.authRef ? active.authKind : 'none',
    checkedAt: null, revision: 0, dormant: false
  };
  var current = generation.current(), snapshot = snapshotSummary(current), running = jobs.active();
  var latestRefresh = active ? history.latest('refresh', active.id) : null;
  var preview = previewFor(active, envState, auth, current, persisted);
  if (preview && latestRefresh && Number.isFinite(Date.parse(preview.checkedAt)) &&
      Number.isFinite(Date.parse(latestRefresh.finishedAt || latestRefresh.startedAt)) &&
      Date.parse(preview.checkedAt) > Date.parse(latestRefresh.finishedAt || latestRefresh.startedAt)) latestRefresh = null;
  if (preview && preview.fresh && latestRefresh && latestRefresh.state === 'failed' && latestRefresh.error &&
      Number.isFinite(Date.parse(latestRefresh.startedAt)) && Date.parse(latestRefresh.startedAt) >= Date.parse(preview.checkedAt) &&
      ['preview-stale', 'source-changed', 'write-conflict', 'environment-revision-conflict', 'auth-revision-conflict'].indexOf(latestRefresh.error.code) >= 0) {
    preview = Object.assign({}, preview, { fresh: false, staleReason: latestRefresh.error.code });
  }
  var mismatch = !!(snapshot.present && active && snapshot.environmentId && snapshot.environmentId !== active.id);
  var manifestEnvironments = envState.mode === 'manifest' ? envState.manifest.environments : (active ? [active] : []);
  return {
    schemaVersion: 1,
    state: overallState(envState, active, auth, snapshot, preview, latestRefresh, running),
    sourceMode: envState.mode,
    sourceError: envState.error || null,
    environmentRevision: envState.revision,
    selectionRevision: persisted.backendSelectionRevision || 0,
    defaultEnvironmentId: envState.mode === 'manifest' ? envState.manifest.defaultEnvironmentId : null,
    activeEnvironmentId: active && active.id || null,
    environments: manifestEnvironments.map(function (row) {
      return { id: row.id, label: row.label, sourceKind: row.sourceKind, sourceUrl: row.sourceUrl,
        postmanEnrichmentUrl: row.postmanEnrichmentUrl, authRef: row.authRef, authKind: row.authKind,
        advanced: row.sourceKind === 'postman' || !!row.postmanEnrichmentUrl };
    }),
    source: active ? { title: active.label, kind: active.sourceKind, url: active.sourceUrl, authKind: active.authKind,
      postmanEnrichmentUrl: active.postmanEnrichmentUrl } : null,
    authentication: auth,
    snapshot: Object.assign(snapshot, { environmentMismatch: mismatch }),
    preview: jobs.publicReport(preview),
    latestRefresh: jobs.publicReport(latestRefresh),
    jobs: running,
    actions: {
      canTest: !!(!resetActive && active && envState.mode === 'manifest' && auth.state !== 'missing' && auth.state !== 'invalid' && !probeByEnvironment(running, active.id)),
      canRefresh: !!(!resetActive && preview && preview.fresh && !running.some(function (job) { return job.reportType === 'refresh'; })),
      canCreateSource: !resetActive && envState.mode === 'missing',
      canEditSource: !resetActive && envState.mode === 'manifest',
      canSetCredential: !!(!resetActive && active && active.authRef === active.id),
      canDeleteCredential: !!(!resetActive && active && (auth.state === 'configured' || auth.dormant)),
      canClearIntegration: !resetActive && running.length === 0 &&
        (envState.mode !== 'missing' || snapshot.present || snapshot.invalid)
    },
    diagnostics: {
      generationPointer: projectRelative(generation.POINTER_FILE),
      reportsDirectory: projectRelative(history.REPORTS_DIR),
      generationId: snapshot.generationId || null,
      snapshotHash: snapshot.hash || null,
      lastError: jobs.publicFailure(lastFailure(snapshot, preview, latestRefresh))
    }
  };
}
function probeByEnvironment(activeJobs, environmentId) {
  return activeJobs.some(function (job) { return job.reportType === 'probe' && job.environmentId === environmentId; });
}

function select(request) {
  if (!request || !exactKeys(request, ['environmentId', 'expectedStateRevision']) || !environments.IDS.includes(request.environmentId) ||
      !Number.isSafeInteger(request.expectedStateRevision) || request.expectedStateRevision < 0) return { ok: false, status: 400, error: 'bad-selection-request' };
  if (resetActive) return { ok: false, status: 409, error: 'writer-lease-conflict' };
  var envState = environments.read();
  if (envState.mode !== 'manifest' || !environments.environmentById(envState, request.environmentId)) return { ok: false, status: 404, error: 'environment-not-found' };
  var persisted = persistence.readPersisted();
  if ((persisted.backendSelectionRevision || 0) !== request.expectedStateRevision) return { ok: false, status: 409, error: 'selection-revision-conflict' };
  if (persisted.backendActiveEnvironmentId !== request.environmentId) {
    persisted.backendActiveEnvironmentId = request.environmentId;
    persisted.backendSelectionRevision = (persisted.backendSelectionRevision || 0) + 1;
    persistence.writePersisted(persisted);
  }
  return { ok: true, status: 200, integration: get() };
}
function repairSelection() {
  if (resetActive) return false;
  var envState = environments.read(), persisted = persistence.readPersisted();
  if (envState.mode !== 'manifest') return false;
  if (!persisted.backendActiveEnvironmentId || environments.environmentById(envState, persisted.backendActiveEnvironmentId)) return false;
  persisted.backendActiveEnvironmentId = '';
  persisted.backendSelectionRevision = (persisted.backendSelectionRevision || 0) + 1;
  persistence.writePersisted(persisted);
  return true;
}
function rememberReset(key, fingerprint, result) {
  if (resetIdempotency.size >= 100) resetIdempotency.delete(resetIdempotency.keys().next().value);
  resetIdempotency.set(key, { fingerprint: fingerprint, result: result });
}
function releaseResetLease(handle) {
  if (handle) try { writerLeases.release(handle); } catch (ignore) {}
}
function reset(request) {
  var keys = ['expectedEnvironmentRevision', 'expectedSnapshotHash', 'expectedStateRevision', 'idempotencyKey'];
  if (!request || !exactKeys(request, keys) || !/^[A-Za-z0-9._:-]{8,128}$/.test(String(request.idempotencyKey || '')) ||
      !Number.isSafeInteger(request.expectedStateRevision) || request.expectedStateRevision < 0 ||
      (request.expectedEnvironmentRevision !== null && request.expectedEnvironmentRevision !== 'absent' &&
        !/^sha256:[a-f0-9]{64}$/.test(String(request.expectedEnvironmentRevision || ''))) ||
      (request.expectedSnapshotHash !== null && !/^sha256:[a-f0-9]{64}$/.test(String(request.expectedSnapshotHash || '')))) {
    return Promise.resolve({ ok: false, status: 400, error: 'bad-request' });
  }
  var fingerprint = JSON.stringify(request), prior = resetIdempotency.get(request.idempotencyKey);
  if (prior) return Promise.resolve(prior.fingerprint === fingerprint ? prior.result :
    { ok: false, status: 409, error: 'idempotency-conflict' });
  if (resetActive) return Promise.resolve({ ok: false, status: 409, error: 'writer-lease-conflict' });
  resetActive = true;
  var running, envState, persisted, generationResult, resetLease;
  try {
    running = jobs.active();
    envState = environments.read();
    persisted = persistence.readPersisted();
  } catch (readError) {
    resetActive = false;
    return Promise.resolve({ ok: false, status: 500, error: 'internal' });
  }
  if (running.length) {
    resetActive = false;
    return Promise.resolve({ ok: false, status: 409,
      error: running.some(function (job) { return job.reportType === 'refresh'; }) ? 'refresh-already-running' : 'probe-already-running' });
  }
  if (envState.revision !== request.expectedEnvironmentRevision) {
    resetActive = false;
    return Promise.resolve({ ok: false, status: 409, error: 'environment-revision-conflict' });
  }
  if ((persisted.backendSelectionRevision || 0) !== request.expectedStateRevision) {
    resetActive = false;
    return Promise.resolve({ ok: false, status: 409, error: 'selection-revision-conflict' });
  }
  try {
    resetLease = writerLeases.acquire(paths.WRITER_LEASES_DIR, { kind: 'site-config', key: 'backend:integration-reset',
      ownerPid: process.pid, ttlMs: 5 * 60 * 1000, rootDir: paths.WRITER_AUTHORITY_ROOT });
    var leaseScan = writerLeases.scan(paths.WRITER_LEASES_DIR, paths.WRITER_AUTHORITY_ROOT);
    if (leaseScan.issues.length || leaseScan.stale.length ||
        !leaseScan.active.some(function (row) { return row.leaseId === resetLease.leaseId; }) ||
        leaseScan.active.some(function (row) { return row.leaseId !== resetLease.leaseId; })) {
      releaseResetLease(resetLease); resetLease = null; resetActive = false;
      return Promise.resolve({ ok: false, status: 409, error: 'writer-lease-conflict' });
    }
  } catch (leaseError) {
    releaseResetLease(resetLease); resetLease = null; resetActive = false;
    return Promise.resolve({ ok: false, status: 409, error: 'writer-lease-conflict' });
  }
  try { generationResult = generation.clearAll(request.expectedSnapshotHash); }
  catch (generationError) {
    releaseResetLease(resetLease);
    resetActive = false;
    return Promise.resolve({ ok: false, status: 500, error: 'internal' });
  }
  if (!generationResult.ok) {
    releaseResetLease(resetLease);
    resetActive = false;
    return Promise.resolve({ ok: false, status: 409, error: generationResult.error });
  }
  return history.clearAll().then(function () {
    jobs.forgetFinished();
    return credentials.clearAll();
  }).then(function () {
    var environmentResult = environments.clearAll(request.expectedEnvironmentRevision, resetLease);
    if (!environmentResult.ok) return environmentResult;
    var latest = persistence.readPersisted();
    if ((latest.backendSelectionRevision || 0) !== request.expectedStateRevision) {
      return { ok: false, status: 409, error: 'selection-revision-conflict' };
    }
    latest.backendActiveEnvironmentId = '';
    latest.backendSelectionRevision = (latest.backendSelectionRevision || 0) + 1;
    persistence.writePersisted(latest);
    return { ok: true, status: 200 };
  }).then(function (result) {
    releaseResetLease(resetLease);
    resetActive = false;
    if (!result.ok) return result;
    var success = { ok: true, status: 200, integration: get() };
    rememberReset(request.idempotencyKey, fingerprint, success);
    return success;
  }, function (error) {
    releaseResetLease(resetLease);
    resetActive = false;
    var code = error && error.message;
    return { ok: false, status: 500,
      error: code === 'history-index-invalid' || code === 'credential-state-invalid' ? code : 'internal' };
  });
}
function resetting() { return resetActive; }
function exactKeys(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}

module.exports = { get: get, select: select, repairSelection: repairSelection, reset: reset, resetting: resetting };
