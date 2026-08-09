'use strict';

// In-process coordinator for typed contract sidecars. Network access and
// committed writes happen only in scripts/backend-action.mjs. This module owns
// admission, idempotency, bounded progress, recovery state and public results.

var childProcess = require('child_process');
var path = require('path');
var crypto = require('crypto');
var paths = require('./paths');
var environments = require('./backend-environments');
var credentials = require('./backend-credentials');
var generation = require('./contract-generation');
var history = require('./contract-history');
var actions = require('./contract-session-actions');
var persistence = require('./persistence');

var jobs = Object.create(null);
var children = Object.create(null);
var probeByEnvironment = Object.create(null);
var refreshJobId = null;
var notifier = function () {};
var PROGRESS_MAX = 500;
var PROGRESS_BYTES_MAX = 256 * 1024;
var OUTPUT_BUFFER_MAX = 512 * 1024;
var PHASES = Object.freeze({ connecting: 1, authenticating: 1, 'validating-contract': 1,
  'resolving-source': 1, 'comparing-snapshot': 1, 'waiting-writer-lease': 1, 'ready-to-refresh': 1 });
var PUBLIC_FAILURE_CODES = Object.freeze({
  'auth-missing': 1, 'auth-invalid': 1, 'auth-rejected': 1,
  'source-unreachable': 1, 'source-content-type': 1, 'source-too-large': 1,
  'source-network-forbidden': 1, 'source-redirect-forbidden': 1, 'source-redirect-limit': 1,
  'invalid-openapi': 1, 'invalid-postman': 1, 'candidate-invalid': 1,
  'preview-stale': 1, 'source-changed': 1, 'write-conflict': 1,
  'snapshot-invalid': 1, 'writer-lease-conflict': 1,
  'environment-revision-conflict': 1, 'auth-revision-conflict': 1,
  'enrichment-unavailable': 1, 'report-size-limit': 1, 'request-too-large': 1,
  'bad-request': 1, 'generation-publication-failed': 1, 'source-missing': 1,
  'staging-conflict': 1, 'staging-unsafe': 1, 'job-interrupted': 1,
  'generation-publication-incomplete': 1, 'sidecar-failed': 1,
  'sidecar-output-limit': 1, 'job-progress-limit': 1
});
var PUBLIC_WARNING_CODES = Object.freeze({
  'enrichment-unavailable': 1,
  'postman-example-dropped': 1
});

function sha(value) { return 'sha256:' + crypto.createHash('sha256').update(value).digest('hex'); }
function stable(value) {
  if (Array.isArray(value)) return value.map(stable);
  if (!value || typeof value !== 'object') return value;
  var out = {}; Object.keys(value).sort().forEach(function (key) { out[key] = stable(value[key]); }); return out;
}
function fingerprint(value) { return sha(Buffer.from(JSON.stringify(stable(value)))); }
function exactKeys(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}
function validIdempotency(value) { return /^[A-Za-z0-9._:-]{8,128}$/.test(String(value || '')); }
function jobId() { return 'job-' + crypto.randomBytes(16).toString('hex'); }
function publicFailureCode(value) {
  var code = String(value && value.code || value || '');
  return Object.prototype.hasOwnProperty.call(PUBLIC_FAILURE_CODES, code) ? code : 'sidecar-failed';
}
function publicFailure(value) {
  return value ? { code: publicFailureCode(value) } : null;
}
function publicWarning(value) {
  var code = String(value && value.code || '');
  return { code: Object.prototype.hasOwnProperty.call(PUBLIC_WARNING_CODES, code) ? code : 'sidecar-warning' };
}
function cleanResolutionString(value, limit) {
  return String(value == null ? '' : value).replace(/[\u0000-\u001f\u007f-\u009f]/g, '').slice(0, limit);
}
function publicResolutionUrl(value) {
  try {
    var url = new URL(cleanResolutionString(value, 2048));
    if (['http:', 'https:'].indexOf(url.protocol) < 0) return null;
    var projected = cleanResolutionString(url.origin + url.pathname, 2048);
    return projected.length <= 200 ? projected : null;
  } catch (ignore) { return null; }
}
function publicResolutionCandidate(value) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return null;
  var out = {};
  var url = publicResolutionUrl(value.url);
  var uid = cleanResolutionString(value.uid, 120);
  var title = cleanResolutionString(value.title, 120);
  var kind = cleanResolutionString(value.kind, 32);
  if (url) out.url = url;
  if (uid) out.uid = uid;
  if (title) out.title = title;
  if (kind) out.kind = kind;
  return out.url || out.uid ? out : null;
}
function publicResolution(value) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return null;
  var out = {};
  var resolvedUrl = publicResolutionUrl(value.resolvedUrl);
  var candidates = Array.isArray(value.candidates) ? value.candidates.slice(0, 20)
    .map(publicResolutionCandidate).filter(Boolean) : [];
  var probedPaths = Array.isArray(value.probedPaths) ? value.probedPaths.slice(0, 8)
    .map(publicResolutionUrl).filter(Boolean) : [];
  var reason = cleanResolutionString(value.reason, 64);
  var method = cleanResolutionString(value.method, 64);
  var detectedKind = cleanResolutionString(value.detectedKind, 32);
  out.state = resolvedUrl ? 'resolved' : (candidates.length > 1 ? 'ambiguous' : 'unrecognized');
  if (reason) out.reason = reason;
  if (method) out.method = method;
  if (resolvedUrl) out.resolvedUrl = resolvedUrl;
  if (detectedKind) out.detectedKind = detectedKind;
  if (candidates.length) out.candidates = candidates;
  if (probedPaths.length) out.probedPaths = probedPaths;
  if (value.truncated === true) out.truncated = true;
  return out;
}
function publicReport(report) {
  if (!report || typeof report !== 'object' || Array.isArray(report)) return null;
  var out = {};
  [
    'schemaVersion', 'reportType', 'jobId', 'state', 'environmentId', 'startedAt', 'finishedAt',
    'previewId', 'sourceKind', 'environmentRevision', 'authRevision', 'selectionRevision',
    'sourceFingerprint', 'snapshotFingerprint', 'checkedAt', 'expiresAt', 'sourceSummary',
    'delta', 'authState', 'result', 'previousHash', 'currentHash', 'addedEndpoints',
    'changedEndpoints', 'removedEndpoints', 'breakingChanges', 'generatedAreas',
    'committedGenerationId', 'fresh', 'staleReason'
  ].forEach(function (key) {
    if (Object.prototype.hasOwnProperty.call(report, key)) out[key] = report[key];
  });
  out.error = publicFailure(report.error);
  out.warnings = Array.isArray(report.warnings) ? report.warnings.map(publicWarning) : [];
  if (Object.prototype.hasOwnProperty.call(report, 'resolution')) out.resolution = publicResolution(report.resolution);
  return out;
}
function publicJob(job) {
  if (!job) return null;
  return { jobId: job.jobId, reportType: job.reportType, environmentId: job.environmentId, state: job.state,
    phase: job.phase, progress: job.progress.map(function (row) { return { at: row.at, phase: row.phase }; }),
    startedAt: job.startedAt, finishedAt: job.finishedAt,
    error: publicFailure(job.error), result: publicReport(job.result) };
}
function notify(job) { try { notifier('backend-job', publicJob(job)); } catch (ignore) {} }
function historyRow(job) {
  return { jobId: job.jobId, reportType: job.reportType, environmentId: job.environmentId, state: job.state,
    startedAt: job.startedAt, finishedAt: job.finishedAt, idempotencyKey: job.idempotencyKey,
    idempotencyFingerprint: job.idempotencyFingerprint };
}
function conflict(error, detail) { return { ok: false, status: 409, error: error, detail: detail || null }; }
function bad(error) { return { ok: false, status: 400, error: error }; }

function replay(idempotencyKey, intentFingerprint) {
  var row = history.findIdempotency(idempotencyKey);
  if (!row) return null;
  if (row.idempotencyFingerprint !== intentFingerprint) return conflict('idempotency-conflict');
  var live = jobs[row.jobId];
  if (live) return { ok: true, status: 200, replayed: true, job: publicJob(live) };
  var report = history.readReport(row.reportType, row.jobId);
  return { ok: true, status: 200, replayed: true, job: {
    jobId: row.jobId, reportType: row.reportType, environmentId: row.environmentId, state: row.state,
    phase: null, progress: [], startedAt: row.startedAt, finishedAt: row.finishedAt,
    error: publicFailure(report && report.error), result: publicReport(report)
  } };
}

function addProgress(job, phase, detail) {
  if (!PHASES[phase]) return false;
  if (job.progress.length >= PROGRESS_MAX) return false;
  var row = { at: new Date().toISOString(), phase: phase, detail: typeof detail === 'string' ? detail.slice(0, 500) : null };
  var bytes = Buffer.byteLength(JSON.stringify(job.progress.concat([row])), 'utf8');
  if (bytes > PROGRESS_BYTES_MAX) return false;
  job.phase = phase; job.progress.push(row); notify(job);
  return true;
}

function finalizeJob(job, exitCode) {
  if (job._finished) return;
  job._finished = true;
  var report = history.readReport(job.reportType, job.jobId);
  var finishedAt = new Date().toISOString();
  var committedRefresh = false;
  if (job.reportType === 'refresh' && report && (report.state === 'success' || report.state === 'partial') && report.committedGenerationId) {
    var current = generation.current();
    committedRefresh = !!(current.ok && current.mode === 'generation' && current.manifest &&
      current.manifest.generationId === report.committedGenerationId && current.environmentId === job.environmentId &&
      current.snapshotHash === report.currentHash);
  }
  var typedFailure = report && report.state === 'failed' && report.error && typeof report.error.code === 'string';
  if (job._forcedError || !report || (exitCode !== 0 && !committedRefresh && !typedFailure) ||
      (job.reportType === 'refresh' && (report.state === 'success' || report.state === 'partial') && !committedRefresh)) {
    var failure = { schemaVersion: 1, reportType: job.reportType, jobId: job.jobId, state: 'failed',
      environmentId: job.environmentId, startedAt: job.startedAt, finishedAt: finishedAt,
      error: job._forcedError || { code: job.reportType === 'refresh' ? 'generation-publication-incomplete' : 'sidecar-failed',
        message: job.reportType === 'refresh'
          ? 'The refresh did not resolve to a complete current generation.'
          : 'The contract sidecar failed safely.' } };
    if (job.reportType === 'refresh') { failure.result = 'failed'; failure.committedGenerationId = null; }
    try { report = history.writeReport(job.reportType, job.jobId, failure); } catch (ignore) { report = failure; }
  }
  job.finishedAt = report && report.finishedAt || finishedAt;
  job.result = report;
  job.state = report && ['success', 'partial', 'failed', 'interrupted'].indexOf(report.state) >= 0
    ? report.state : 'failed';
  job.error = report && report.error || (job.state === 'failed' ? { code: 'sidecar-failed', message: 'The contract sidecar failed safely.' } : null);
  if (job.reportType === 'probe' && probeByEnvironment[job.environmentId] === job.jobId) delete probeByEnvironment[job.environmentId];
  if (job.reportType === 'refresh' && refreshJobId === job.jobId) refreshJobId = null;
  delete children[job.jobId];
  history.upsert(historyRow(job)).then(function () {
    notify(job);
    try { notifier('backend-integration', { changed: true }); } catch (ignore) {}
    if (job.reportType === 'refresh' &&
        (job.state === 'success' || job.state === 'partial')) {
      try {
        notifier('api-overview', {
          changed: true,
          committedGenerationId: job.result && job.result.committedGenerationId || null
        });
      } catch (ignore2) {}
    }
  });
}

function spawn(job, sidecarRequest) {
  var script = path.join(paths.API_CONTRACT_DIR, 'scripts', 'backend-action.mjs');
  var child;
  try {
    child = childProcess.spawn(process.execPath, [script], { cwd: paths.PROJECT_ROOT,
      env: Object.assign({}, process.env, { ORCHESTRATOR_PROJECT_ROOT: paths.PROJECT_ROOT }),
      stdio: ['pipe', 'pipe', 'pipe'] });
  } catch (e) { finalizeJob(job, 1); return; }
  children[job.jobId] = child;
  var buffer = '', outputBytes = 0, stderrBytes = 0;
  child.stdout.on('data', function (chunk) {
    outputBytes += chunk.length;
    if (outputBytes > OUTPUT_BUFFER_MAX) {
      job._forcedError = { code: 'sidecar-output-limit', message: 'The contract sidecar exceeded its bounded output contract.' };
      try { child.kill('SIGTERM'); } catch (ignore) {} return;
    }
    buffer += chunk.toString('utf8');
    var lines = buffer.split('\n'); buffer = lines.pop();
    lines.forEach(function (line) {
      var event; try { event = JSON.parse(line); } catch (ignore) { return; }
      if (event && event.type === 'progress' && !addProgress(job, event.phase, event.detail)) {
        job._forcedError = { code: 'job-progress-limit', message: 'The contract job exceeded its bounded progress contract.' };
        try { child.kill('SIGTERM'); } catch (ignore) {}
      }
    });
  });
  // Drain but never publish or log stderr; reports carry only typed errors.
  child.stderr.on('data', function (chunk) {
    stderrBytes += chunk.length;
    if (stderrBytes > OUTPUT_BUFFER_MAX) {
      job._forcedError = { code: 'sidecar-output-limit', message: 'The contract sidecar exceeded its bounded output contract.' };
      try { child.kill('SIGTERM'); } catch (ignore) {}
    }
  });
  child.on('error', function () { finalizeJob(job, 1); });
  child.on('close', function (code) { finalizeJob(job, Number.isInteger(code) ? code : 1); });
  child.stdin.on('error', function () {});
  child.stdin.end(JSON.stringify(sidecarRequest));
}

function createJob(reportType, environmentId, idempotencyKey, intentFingerprint) {
  var now = new Date().toISOString(), id = jobId();
  var job = { jobId: id, reportType: reportType, environmentId: environmentId, state: 'queued', phase: null,
    progress: [], startedAt: now, finishedAt: null, error: null, result: null,
    idempotencyKey: idempotencyKey, idempotencyFingerprint: intentFingerprint, _finished: false };
  jobs[id] = job;
  return job;
}

function startProbe(request) {
  var keys = ['environmentId', 'expectedAuthRevision', 'expectedEnvironmentRevision', 'idempotencyKey'];
  if (!exactKeys(request, keys) || !environments.IDS.includes(request.environmentId) || !validIdempotency(request.idempotencyKey) ||
      !Number.isSafeInteger(request.expectedAuthRevision) || request.expectedAuthRevision < 0 ||
      !/^sha256:[a-f0-9]{64}$/.test(String(request.expectedEnvironmentRevision || ''))) return Promise.resolve(bad('bad-test-request'));
  var intent = fingerprint(request), existing = replay(request.idempotencyKey, intent);
  if (existing) return Promise.resolve(existing);
  if (probeByEnvironment[request.environmentId]) return Promise.resolve(conflict('probe-already-running'));
  var envState = environments.read();
  if (envState.mode !== 'manifest' || envState.revision !== request.expectedEnvironmentRevision) return Promise.resolve(conflict('environment-revision-conflict'));
  var environment = environments.environmentById(envState, request.environmentId);
  if (!environment) return Promise.resolve({ ok: false, status: 404, error: 'environment-not-found' });
  var persisted = persistence.readPersisted();
  var selectedEnvironmentId = persisted.backendActiveEnvironmentId || envState.manifest.defaultEnvironmentId;
  if (selectedEnvironmentId !== environment.id) return Promise.resolve(conflict('environment-selection-conflict'));
  var auth = credentials.publicStatus(environment);
  if (auth.revision !== request.expectedAuthRevision) return Promise.resolve(conflict('auth-revision-conflict'));
  if (auth.state === 'missing' || auth.state === 'invalid') return Promise.resolve(conflict(auth.state === 'missing' ? 'auth-missing' : 'auth-invalid'));
  var job = createJob('probe', environment.id, request.idempotencyKey, intent);
  var selectionRevision = persisted.backendSelectionRevision || 0;
  probeByEnvironment[environment.id] = job.jobId;
  return history.upsert(historyRow(job)).then(function () {
    job.state = 'running'; notify(job);
    spawn(job, { schemaVersion: 1, jobId: job.jobId, action: actions.ACTIONS.probe, environmentId: environment.id,
      environmentRevision: envState.revision, authRevision: auth.revision, selectionRevision: selectionRevision });
    return { ok: true, status: 202, replayed: false, job: publicJob(job) };
  });
}

function startRefresh(request) {
  var keys = ['acknowledgements', 'expectedSnapshotHash', 'idempotencyKey', 'previewId'];
  if (!exactKeys(request, keys) || !history.PREVIEW_RE.test(String(request.previewId || '')) || !validIdempotency(request.idempotencyKey) ||
      (request.expectedSnapshotHash !== null && !/^sha256:[a-f0-9]{64}$/.test(String(request.expectedSnapshotHash || ''))) ||
      !Array.isArray(request.acknowledgements) || request.acknowledgements.length > 10 ||
      request.acknowledgements.some(function (code) { return !/^[a-z][a-z0-9-]{0,63}$/.test(String(code)); })) return Promise.resolve(bad('bad-refresh-request'));
  var intent = fingerprint(request), existing = replay(request.idempotencyKey, intent);
  if (existing) return Promise.resolve(existing);
  if (refreshJobId) return Promise.resolve(conflict('refresh-already-running'));
  var preview = history.findPreview(request.previewId);
  if (!preview || preview.state !== 'success') return Promise.resolve(conflict('preview-stale'));
  if (!Number.isFinite(Date.parse(preview.expiresAt)) || Date.parse(preview.expiresAt) <= Date.now()) return Promise.resolve(conflict('preview-stale'));
  var envState = environments.read();
  if (envState.mode !== 'manifest' || envState.revision !== preview.environmentRevision) return Promise.resolve(conflict('preview-stale'));
  var environment = environments.environmentById(envState, preview.environmentId);
  if (!environment) return Promise.resolve(conflict('preview-stale'));
  var persisted = persistence.readPersisted();
  var selectedEnvironmentId = persisted.backendActiveEnvironmentId || envState.manifest.defaultEnvironmentId;
  if ((persisted.backendSelectionRevision || 0) !== preview.selectionRevision || selectedEnvironmentId !== preview.environmentId) {
    return Promise.resolve(conflict('preview-stale'));
  }
  var auth = credentials.publicStatus(environment);
  if (auth.revision !== preview.authRevision) return Promise.resolve(conflict('preview-stale'));
  var snapshot = generation.current();
  if (!snapshot.ok || (snapshot.snapshotHash || null) !== (preview.snapshotFingerprint || null) ||
      (snapshot.snapshotHash || null) !== (request.expectedSnapshotHash || null)) return Promise.resolve(conflict('write-conflict'));
  var action = actions.refreshAction(environment.sourceKind);
  var job = createJob('refresh', environment.id, request.idempotencyKey, intent);
  refreshJobId = job.jobId;
  return history.upsert(historyRow(job)).then(function () {
    job.state = 'running'; notify(job);
    spawn(job, { schemaVersion: 1, jobId: job.jobId, action: action, environmentId: environment.id,
      environmentRevision: envState.revision, authRevision: auth.revision, previewId: preview.previewId,
      expectedSnapshotHash: request.expectedSnapshotHash, expectedSourceFingerprint: preview.sourceFingerprint,
      selectionRevision: preview.selectionRevision, acknowledgements: request.acknowledgements });
    return { ok: true, status: 202, replayed: false, job: publicJob(job) };
  });
}

function get(id) {
  if (!history.JOB_RE.test(String(id || ''))) return null;
  if (jobs[id]) return publicJob(jobs[id]);
  var row = history.findJob(id);
  if (!row) return null;
  var report = history.readReport(row.reportType, row.jobId);
  return { jobId: row.jobId, reportType: row.reportType, environmentId: row.environmentId, state: row.state,
    phase: null, progress: [], startedAt: row.startedAt, finishedAt: row.finishedAt,
    error: publicFailure(report && report.error), result: publicReport(report) };
}
function active() {
  return Object.keys(jobs).map(function (id) { return jobs[id]; }).filter(function (job) { return !job._finished; }).map(publicJob);
}
function forgetFinished() {
  Object.keys(jobs).forEach(function (id) { if (jobs[id]._finished) delete jobs[id]; });
}
function init(options) {
  if (options && typeof options.notify === 'function') notifier = options.notify;
  return history.recoverInterrupted();
}
function killAll() {
  Object.keys(children).forEach(function (id) { try { children[id].kill('SIGTERM'); } catch (ignore) {} });
}

module.exports = {
  init: init,
  startProbe: startProbe,
  startRefresh: startRefresh,
  get: get,
  active: active,
  forgetFinished: forgetFinished,
  killAll: killAll,
  publicFailureCode: publicFailureCode,
  publicFailure: publicFailure,
  publicReport: publicReport
};
