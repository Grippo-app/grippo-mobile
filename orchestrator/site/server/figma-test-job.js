'use strict';

// Test connection state machine and verify-before-save file candidates. Figma
// calls run only through exact server-owned session actions. Test receipts are
// runtime-only; project config and committed design artifacts are untouched.

var crypto = require('crypto');
var path = require('path');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var figma = require('./figma');
var sessions = require('./sessions');
var actions = require('./figma-session-actions');
var configUpdate = require('./project-config-update');

var CONNECTOR_TTL = 30 * 1000;
var ACCOUNT_TTL = 10 * 60 * 1000;
var ACCESS_TTL = 10 * 60 * 1000;
var ACTION_TIMEOUT = 10 * 60 * 1000;
var STARTUP_RETRY_MS = 1000;
var STARTUP_PROBE_TIMEOUT = 60 * 1000;
var RECEIPT_MAX = 32 * 1024;
var RECEIPT_SCAN_MAX = 1000;
var JOB_RE = /^ftj-[a-f0-9]{32}$/;
var CANDIDATE_RE = /^fvc-[a-f0-9]{32}$/;
var NONCE_RE = /^[a-f0-9]{32}$/;
var HASH_RE = /^sha256:[a-f0-9]{64}$/;
var jobs = Object.create(null);
var candidates = Object.create(null);
var activeJobId = null;
var latestCandidateId = null;
var activeCandidateId = null;
var latestAccess = null;
var startupVerificationStarted = false;
var startupVerificationTimer = null;
var resetPaused = false;
var syncActive = function () { return false; };
var syncRecoveryState = function () { return 'ready'; };
var notify = function () {};

function now() { return new Date().toISOString(); }
function randomId(prefix) { return prefix + '-' + crypto.randomBytes(16).toString('hex'); }
function fingerprint(label, value) { return 'sha256:' + crypto.createHash('sha256').update(label + '\0' + String(value || ''), 'utf8').digest('hex'); }
function accountFingerprint(account) {
  if (!account || !(account.email || account.handle)) return null;
  return fingerprint('figma-account-v1', String(account.email || '').trim().toLowerCase() + '\0' + String(account.handle || '').trim().toLowerCase());
}
function fileKeyFingerprint(key) { return fingerprint('figma-file-v1', key); }
function fresh(checkedAt, ttl) {
  var stamp = Date.parse(checkedAt || '');
  return Number.isFinite(stamp) && stamp <= Date.now() + 5 * 60 * 1000 && Date.now() - stamp <= ttl;
}
function isoTimestamp(value) {
  // Runtime receipts are commonly stamped either by Date#toISOString()
  // (millisecond precision) or by `date -u` (second precision). Both are
  // canonical UTC timestamps; keep the grammar deliberately narrow and do
  // not accept offsets, arbitrary fractional precision, or normalized dates.
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{3})?Z$/.test(value)) return false;
  var parsed = new Date(value);
  if (!Number.isFinite(parsed.getTime())) return false;
  var canonical = parsed.toISOString();
  return canonical === value || canonical.replace(/\.000Z$/, 'Z') === value;
}
var PUBLIC_REASON_CODES = Object.freeze({
  'connector-missing': 'connector-missing',
  'auth-required': 'auth-required',
  'connector-conflict': 'connector-conflict',
  'connector-unavailable': 'connector-unavailable',
  'account-stale': 'account-stale',
  'file-missing': 'file-missing',
  'file-invalid': 'file-invalid',
  'access-unverified': 'access-unverified',
  'file-not-found': 'file-not-found',
  'access-denied': 'access-denied',
  'quota-risk': 'quota-risk',
  'figma-action-timeout': 'connection-test-timeout',
  'figma-session-start-refused': 'connection-test-start-failed',
  'file-access-receipt-invalid': 'access-unverified',
  'file-access-receipt-cleanup-failed': 'access-unverified',
  'figma-file-key-invalid': 'file-invalid',
  'figma-verification-unavailable': 'connector-unavailable',
  'finalization-active': 'connection-test-busy',
  'figma-session-active': 'connection-test-busy',
  'figma-sync-active': 'connection-test-busy',
  'figma-test-active': 'connection-test-busy',
  'file-verification-active': 'connection-test-busy',
  'bad-figma-action': 'integration-failed',
  'figma-action-key-mismatch': 'integration-failed',
  'figma-prompt-contract-invalid': 'integration-failed',
  'figma-prompt-contract-unavailable': 'integration-failed',
  'figma-server-context-invalid': 'integration-failed',
  'figma-internal-action-forbidden': 'integration-failed'
});
function reasonCode(value, fallback) {
  var exact = PUBLIC_REASON_CODES[String(value || '')];
  return exact || fallback || 'integration-failed';
}
function connectorSessionActive() {
  try {
    var active = sessions.list();
    return Object.keys(active).some(function (key) {
      return active[key] && active[key].running && (key.indexOf('figma:') === 0 || key.indexOf('task:') === 0);
    });
  } catch (error) { return true; }
}
function publicJob(job) {
  if (!job) return null;
  return { id: job.id, state: job.state, phase: job.phase, reasonCode: job.reasonCode, startedAt: job.startedAt, finishedAt: job.finishedAt };
}
function publicCandidate(candidate) {
  if (!candidate) return null;
  return {
    id: candidate.id,
    state: candidate.state,
    fileName: candidate.fileName || null,
    maskedKey: candidate.fileKey ? maskKey(candidate.fileKey) : null,
    reasonCode: candidate.reasonCode || null,
    checkedAt: candidate.checkedAt || null,
    expiresAt: candidate.expiresAt || null
  };
}
function maskKey(key) {
  key = String(key || '');
  return key.length <= 8 ? '••••' : key.slice(0, 4) + '…' + key.slice(-4);
}
function emit(eventName, payload) {
  try { notify(eventName, payload); } catch (error) {}
}
function connectorReady(status) {
  return !!(status && status.state === 'connected' && status.local && status.local.present && !(status.global && status.global.present));
}
function connectorReason(status) {
  if (status && status.global && status.global.present) return 'connector-conflict';
  if (!status || status.state === 'unknown') return 'connector-unavailable';
  if (status.state === 'connected' && (!status.local || !status.local.present)) return 'connector-missing';
  if (status.state === 'local-absent') return 'connector-missing';
  if (status.state === 'needs-auth') return 'auth-required';
  if (status.state === 'misconfigured') return 'connector-conflict';
  if (status.state === 'cli-missing') return 'connector-missing';
  return 'connector-unavailable';
}
function syncBlockCode() {
  try {
    if (syncRecoveryState() === 'failed') return 'figma-sync-recovery-failed';
    return syncActive() ? 'figma-sync-active' : null;
  } catch (error) { return 'figma-sync-active'; }
}

function waitFor(check, timeoutMs, intervalMs) {
  return new Promise(function (resolve, reject) {
    var started = Date.now();
    function tick() {
      var result;
      try { result = check(); } catch (error) { reject(error); return; }
      if (result && result.done) { resolve(result.value); return; }
      if (Date.now() - started >= timeoutMs) { reject(new Error('figma-action-timeout')); return; }
      var timer = setTimeout(tick, intervalMs || 250);
      if (typeof timer.unref === 'function') timer.unref();
    }
    tick();
  });
}

function runRuntimeAction(key, action, context) {
  return actions.resolveServerAction(key, action, context).then(function (resolved) {
    if (!resolved.ok) throw new Error(resolved.error || 'figma-action-invalid');
    return waitFor(function () {
      var current = sessions.status(key);
      return sessions.settled(current) ? { done: true, value: null } : { done: false };
    }, 60 * 1000, 250).then(function () {
      var started = sessions.start(key, { action: resolved.action, prompt: resolved.prompt, runtimeOnly: true });
      if (!started || !started.running || started.error) throw new Error('figma-session-start-refused');
      return waitFor(function () {
        var state = sessions.status(key);
        return sessions.settled(state) ? { done: true, value: state } : { done: false };
      }, ACTION_TIMEOUT, 500);
    });
  });
}

function receiptFile(nonce) { return path.join(paths.FIGMA_CACHE_DIR, 'integration', 'file-access-' + nonce + '.json'); }
function receiptRelative(nonce) { return 'orchestrator/.cache/figma/integration/file-access-' + nonce + '.json'; }
function readReceipt(nonce, expected) {
  if (!NONCE_RE.test(String(nonce || ''))) return null;
  var file = receiptFile(nonce), directory = path.dirname(file);
  var hit;
  try { hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, directory, file, RECEIPT_MAX); } catch (error) { return null; }
  if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') return null;
  var value;
  try { value = JSON.parse(hit.bytes.toString('utf8')); } catch (parseError) { return null; }
  var keys = value && typeof value === 'object' && !Array.isArray(value) ? Object.keys(value).sort().join('\0') : '';
  var expectedReason = { verified: '', denied: 'access-denied', 'not-found': 'file-not-found', 'quota-blocked': 'quota-risk' }[value && value.state];
  if (keys !== ['schemaVersion', 'verificationNonce', 'fileKey', 'accountFingerprint', 'state', 'fileName', 'checkedAt', 'reasonCode'].sort().join('\0') ||
      value.schemaVersion !== 1 || value.verificationNonce !== nonce || value.fileKey !== expected.fileKey ||
      value.accountFingerprint !== expected.accountFingerprint || ['verified', 'denied', 'not-found', 'quota-blocked'].indexOf(value.state) < 0 ||
      typeof value.fileName !== 'string' || value.fileName.length > 300 || /[\x00-\x1f\x7f]/.test(value.fileName) ||
      !isoTimestamp(value.checkedAt) || !fresh(value.checkedAt, ACCESS_TTL) ||
      value.reasonCode !== expectedReason) return null;
  // The metadata probe proves access but does not always expose the file-level name.
  // Keep that optional display metadata separate from the access evidence.
  value.fileName = value.fileName.trim() || null;
  return value;
}

function pruneReceipts(keepNonce) {
  var directory = path.join(paths.FIGMA_CACHE_DIR, 'integration');
  var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, directory, RECEIPT_SCAN_MAX);
  if (!listed.ok) return false;
  var ok = true;
  listed.names.forEach(function (name) {
    var match = /^file-access-([a-f0-9]{32})\.json$/.exec(name);
    if (!match || match[1] === keepNonce) return;
    if (!fileGuards.unlinkRegularFileUnder(paths.PROJECT_ROOT, directory, path.join(directory, name), { allowMissing: true })) ok = false;
  });
  return ok;
}

function verifyAccess(fileKey, account, owner) {
  var accountHash = accountFingerprint(account);
  var nonce = crypto.randomBytes(16).toString('hex');
  var context = {
    figmaFileKey: fileKey,
    accessNonce: nonce,
    accountFingerprint: accountHash,
    receiptPath: receiptRelative(nonce)
  };
  return runRuntimeAction('figma:fileaccess', 'file-access', context).then(function () {
    var receipt = readReceipt(nonce, { fileKey: fileKey, accountFingerprint: accountHash });
    if (!receipt) throw new Error('file-access-receipt-invalid');
    if (!pruneReceipts(nonce)) throw new Error('file-access-receipt-cleanup-failed');
    latestAccess = {
      fileKey: fileKey,
      fileKeyFingerprint: fileKeyFingerprint(fileKey),
      accountFingerprint: accountHash,
      state: receipt.state,
      fileName: receipt.fileName,
      checkedAt: receipt.checkedAt,
      reasonCode: receipt.reasonCode || (receipt.state === 'denied' ? 'access-denied' : receipt.state === 'not-found' ? 'file-not-found' : receipt.state === 'quota-blocked' ? 'quota-risk' : null)
    };
    if (owner) owner.access = latestAccess;
    emit('figma-integration', { phase: 'file-access', state: receipt.state });
    return latestAccess;
  });
}

function finishJob(job, state, reasonCode) {
  job.state = state; job.reasonCode = reasonCode ? reasonCodeValue(reasonCode) : null; job.finishedAt = now();
  if (activeJobId === job.id) activeJobId = null;
  emit('figma-integration', { jobId: job.id, state: job.state, phase: job.phase, reasonCode: job.reasonCode });
  return publicJob(job);
}

function reasonCodeValue(value) { return reasonCode(value, 'integration-failed'); }

function runTest(job, force) {
  Promise.resolve().then(function () {
    job.phase = 'checking-connector'; emit('figma-integration', publicJob(job));
    var before = figma.status() || {};
    if (force || !fresh(before.checkedAt, CONNECTOR_TTL)) {
      var priorCheckedAt = before.checkedAt;
      figma.invalidateIdentity();
      return waitFor(function () {
        var current = figma.status();
        var probe = figma.probeState();
        return !probe.probing && current.checkedAt !== priorCheckedAt ? { done: true, value: current } : { done: false };
      }, 45 * 1000, 300);
    }
    return before;
  }).then(function (connector) {
    if (!connectorReady(connector)) throw new Error(connectorReason(connector));
    job.phase = 'verifying-account'; emit('figma-integration', publicJob(job));
    var account = figma.account();
    if (account && fresh(account.checkedAt, ACCOUNT_TTL)) return account;
    return runRuntimeAction('figma:whoami', 'whoami', {
      verificationNonce: connector.verificationNonce,
      figmaFileKey: job.fileKey || ''
    }).then(function () {
      var checked = figma.account();
      if (!checked || !fresh(checked.checkedAt, ACCOUNT_TTL)) throw new Error('auth-required');
      return checked;
    });
  }).then(function (account) {
    job.accountFingerprint = accountFingerprint(account);
    if (!job.fileKey) throw new Error('file-missing');
    job.phase = 'checking-file-access'; emit('figma-integration', publicJob(job));
    if (!force && latestAccess && latestAccess.fileKey === job.fileKey && latestAccess.accountFingerprint === job.accountFingerprint && fresh(latestAccess.checkedAt, ACCESS_TTL)) return latestAccess;
    return verifyAccess(job.fileKey, account, job);
  }).then(function (access) {
    if (access.state !== 'verified') throw new Error(access.reasonCode || (access.state === 'denied' ? 'access-denied' : 'file-not-found'));
    job.phase = 'ready'; finishJob(job, 'completed', null);
  }).catch(function (error) {
    finishJob(job, 'failed', String(error && error.message || error).slice(0, 80));
  });
}

function start(request) {
  if (resetPaused) return { ok: false, status: 409, error: 'writer-lease-conflict' };
  request = request || {};
  if (Object.keys(request).sort().join('\0') !== ['expectedFileKey', 'force'].sort().join('\0') ||
      !(request.expectedFileKey === null || typeof request.expectedFileKey === 'string') || typeof request.force !== 'boolean') {
    return { ok: false, status: 400, error: 'bad-figma-test-request' };
  }
  var config = configUpdate.read();
  if (!config.ok) return { ok: false, status: 409, error: config.error };
  if (config.figmaFieldState === 'invalid') return { ok: false, status: 409, error: 'file-invalid' };
  var expected = request.expectedFileKey;
  if (expected !== null && expected !== config.figmaFileKey) return { ok: false, status: 409, error: 'project-file-changed' };
  if (activeJobId && jobs[activeJobId]) return { ok: true, status: 202, job: publicJob(jobs[activeJobId]) };
  var syncBlocked = syncBlockCode();
  if (syncBlocked) return { ok: false, status: 409, error: syncBlocked };
  if (activeCandidateId && candidates[activeCandidateId] && candidates[activeCandidateId].state === 'verifying') return { ok: false, status: 409, error: 'file-verification-active' };
  if (connectorSessionActive()) return { ok: false, status: 409, error: 'figma-session-active' };
  Object.keys(jobs).map(function (id) { return jobs[id]; }).filter(function (job) { return job.finishedAt; })
    .sort(function (a, b) { return Date.parse(b.finishedAt) - Date.parse(a.finishedAt); }).slice(50)
    .forEach(function (job) { delete jobs[job.id]; });
  var id = randomId('ftj');
  var job = jobs[id] = { id: id, state: 'running', phase: 'checking-connector', reasonCode: null, startedAt: now(), finishedAt: null, fileKey: config.figmaFileKey, accountFingerprint: null, access: null };
  activeJobId = id;
  runTest(job, request.force);
  return { ok: true, status: 202, job: publicJob(job) };
}

// A server restart creates a new connector identity generation, so the prior
// nonce-bound account receipt must remain invalid. Re-run the exact same strict
// test job automatically once the initial connector probe and any competing
// writer session have settled; never recover identity by trusting old bytes.
function startupVerificationAttempt(startedAt) {
  if (resetPaused) return true;
  var config = configUpdate.read();
  if (!config.ok || config.figmaFieldState !== 'selected' || !config.figmaFileKey) return false;
  var connector = figma.status() || {};
  var probe = figma.probeState();
  if (probe && probe.probing) return true;
  if (!connectorReady(connector)) {
    return connector.state === 'unknown' && Date.now() - startedAt < STARTUP_PROBE_TIMEOUT;
  }
  var result = start({ expectedFileKey: config.figmaFileKey, force: false });
  if (result && result.ok) return false;
  return !!(result && {
    'figma-sync-active': true,
    'file-verification-active': true,
    'figma-session-active': true
  }[result.error]);
}

function startupVerify() {
  if (startupVerificationStarted) return false;
  startupVerificationStarted = true;
  var startedAt = Date.now();
  function tick() {
    startupVerificationTimer = null;
    if (!startupVerificationAttempt(startedAt)) return;
    startupVerificationTimer = setTimeout(tick, STARTUP_RETRY_MS);
    if (typeof startupVerificationTimer.unref === 'function') startupVerificationTimer.unref();
  }
  tick();
  return true;
}

function verifyCandidate(request) {
  if (resetPaused) return { ok: false, status: 409, error: 'writer-lease-conflict' };
  request = request || {};
  if (Object.keys(request).sort().join('\0') !== ['expectedConfigRevision', 'urlOrKey'].sort().join('\0') ||
      typeof request.expectedConfigRevision !== 'string' || typeof request.urlOrKey !== 'string') return { ok: false, status: 400, error: 'bad-file-verify-request' };
  var config = configUpdate.read();
  if (!config.ok) return { ok: false, status: 409, error: config.error };
  if (config.revision !== request.expectedConfigRevision) return { ok: false, status: 409, error: 'project-config-revision-conflict', currentRevision: config.revision };
  var normalized = configUpdate.normalizeFigmaInput(request.urlOrKey);
  if (!normalized) return { ok: false, status: 400, error: 'figma-file-key-invalid' };
  var connector = figma.status(), account = figma.account();
  if (!connectorReady(connector)) return { ok: false, status: 409, error: connectorReason(connector) };
  if (!account || !fresh(account.checkedAt, ACCOUNT_TTL)) return { ok: false, status: 409, error: 'account-stale' };
  if (activeCandidateId && candidates[activeCandidateId] && candidates[activeCandidateId].state === 'verifying') {
    var active = candidates[activeCandidateId];
    if (active.fileKey === normalized.key && active.url === normalized.url && active.configRevision === config.revision && active.accountFingerprint === accountFingerprint(account)) {
      return { ok: true, status: 202, candidate: publicCandidate(active) };
    }
    return { ok: false, status: 409, error: 'file-verification-active' };
  }
  var syncBlocked = syncBlockCode();
  if (syncBlocked) return { ok: false, status: 409, error: syncBlocked };
  if (activeJobId && jobs[activeJobId]) return { ok: false, status: 409, error: 'figma-test-active' };
  if (connectorSessionActive()) return { ok: false, status: 409, error: 'figma-session-active' };
  Object.keys(candidates).filter(function (candidateId) {
    var value = candidates[candidateId];
    return value.state !== 'verifying' && value.expiresAt && Date.parse(value.expiresAt) <= Date.now();
  }).forEach(function (candidateId) { delete candidates[candidateId]; });
  Object.keys(candidates).map(function (candidateId) { return candidates[candidateId]; })
    .filter(function (value) { return value.state !== 'verifying'; })
    .sort(function (a, b) { return Date.parse(b.checkedAt || b.expiresAt || 0) - Date.parse(a.checkedAt || a.expiresAt || 0); })
    .slice(49).forEach(function (value) { delete candidates[value.id]; });
  var id = randomId('fvc');
  var candidate = candidates[id] = {
    id: id, state: 'verifying', fileKey: normalized.key, url: normalized.url,
    fileName: null, reasonCode: null, checkedAt: null, expiresAt: null,
    configRevision: config.revision, accountFingerprint: accountFingerprint(account)
  };
  latestCandidateId = id; activeCandidateId = id;
  verifyAccess(normalized.key, account, candidate).then(function (access) {
    candidate.state = access.state === 'verified' ? 'verified' : 'failed';
    candidate.fileName = access.fileName; candidate.reasonCode = access.reasonCode;
    candidate.checkedAt = access.checkedAt; candidate.expiresAt = new Date(Date.parse(access.checkedAt) + ACCESS_TTL).toISOString();
    if (activeCandidateId === id) activeCandidateId = null;
    emit('figma-integration', { candidate: publicCandidate(candidate) });
  }, function (error) {
    candidate.state = 'failed'; candidate.reasonCode = reasonCodeValue(error && error.message || error);
    candidate.checkedAt = now(); candidate.expiresAt = new Date(Date.now() + ACCESS_TTL).toISOString();
    if (activeCandidateId === id) activeCandidateId = null;
    emit('figma-integration', { candidate: publicCandidate(candidate) });
  });
  return { ok: true, status: 202, candidate: publicCandidate(candidate) };
}

function saveCandidate(request) {
  if (resetPaused) return { ok: false, status: 409, error: 'writer-lease-conflict' };
  request = request || {};
  if (Object.keys(request).sort().join('\0') !== ['candidateId', 'expectedConfigRevision'].sort().join('\0') ||
      !CANDIDATE_RE.test(String(request.candidateId || '')) || typeof request.expectedConfigRevision !== 'string') {
    return { ok: false, status: 400, error: 'bad-file-save-request' };
  }
  var syncBlocked = syncBlockCode();
  if (syncBlocked) return { ok: false, status: 409, error: syncBlocked };
  if (activeJobId && jobs[activeJobId]) return { ok: false, status: 409, error: 'figma-test-active' };
  if (activeCandidateId && candidates[activeCandidateId] && candidates[activeCandidateId].state === 'verifying') return { ok: false, status: 409, error: 'file-verification-active' };
  if (connectorSessionActive()) return { ok: false, status: 409, error: 'figma-session-active' };
  var candidate = candidates[request.candidateId];
  if (!candidate) return { ok: false, status: 409, error: 'file-candidate-unverified' };
  if (candidate.id !== latestCandidateId) return { ok: false, status: 409, error: 'file-candidate-stale' };
  if (candidate.state !== 'verified') return { ok: false, status: 409, error: 'file-candidate-unverified' };
  if (!fresh(candidate.checkedAt, ACCESS_TTL)) return { ok: false, status: 409, error: 'file-candidate-expired' };
  if (candidate.configRevision !== request.expectedConfigRevision) return { ok: false, status: 409, error: 'project-config-revision-conflict' };
  var account = figma.account();
  if (accountFingerprint(account) !== candidate.accountFingerprint) return { ok: false, status: 409, error: 'account-changed' };
  var result = configUpdate.update({ capability: 'figma', field: 'figmaLibraryUrl', value: candidate.url, expectedRevision: request.expectedConfigRevision });
  if (result.ok) {
    latestAccess = {
      fileKey: candidate.fileKey, fileKeyFingerprint: fileKeyFingerprint(candidate.fileKey), accountFingerprint: candidate.accountFingerprint,
      state: 'verified', fileName: candidate.fileName, checkedAt: candidate.checkedAt, reasonCode: null
    };
  }
  return result;
}

function accessFor(fileKey, accountHash) {
  return latestAccess && latestAccess.fileKey === fileKey && latestAccess.accountFingerprint === accountHash && fresh(latestAccess.checkedAt, ACCESS_TTL)
    ? Object.assign({}, latestAccess) : null;
}
function currentJob() { return activeJobId ? publicJob(jobs[activeJobId]) : null; }
function busy() {
  return !!(activeJobId && jobs[activeJobId]) ||
    !!(activeCandidateId && candidates[activeCandidateId] && candidates[activeCandidateId].state === 'verifying');
}
function resetReady() { return !busy(); }
function beginReset() {
  if (!resetReady()) return false;
  resetPaused = true;
  return true;
}
function endReset() { resetPaused = false; }
function clearRuntime() {
  if (!resetReady()) return false;
  if (startupVerificationTimer) clearTimeout(startupVerificationTimer);
  startupVerificationTimer = null;
  startupVerificationStarted = true;
  jobs = Object.create(null);
  candidates = Object.create(null);
  activeJobId = null;
  latestCandidateId = null;
  activeCandidateId = null;
  latestAccess = null;
  return true;
}
function lastJob() {
  var values = Object.keys(jobs).map(function (id) { return jobs[id]; }).sort(function (a, b) { return Date.parse(b.startedAt) - Date.parse(a.startedAt); });
  return values.length ? publicJob(values[0]) : null;
}
function latestCandidate() { return latestCandidateId ? publicCandidate(candidates[latestCandidateId]) : null; }
function recoverLatestAccess() {
  var config = configUpdate.read(), account = figma.account();
  var accountHash = accountFingerprint(account);
  if (!config.ok || !config.figmaFileKey || !accountHash) return;
  var directory = path.join(paths.FIGMA_CACHE_DIR, 'integration');
  var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, directory, RECEIPT_SCAN_MAX);
  if (!listed.ok) return;
  var best = null;
  listed.names.forEach(function (name) {
    var match = /^file-access-([a-f0-9]{32})\.json$/.exec(name);
    if (!match) return;
    var receipt = readReceipt(match[1], { fileKey: config.figmaFileKey, accountFingerprint: accountHash });
    if (receipt && (!best || Date.parse(receipt.checkedAt) > Date.parse(best.receipt.checkedAt))) best = { receipt: receipt, nonce: match[1] };
  });
  if (best) {
    var recovered = best.receipt;
    if (!pruneReceipts(best.nonce)) return;
    latestAccess = {
    fileKey: config.figmaFileKey,
    fileKeyFingerprint: fileKeyFingerprint(config.figmaFileKey),
    accountFingerprint: accountHash,
    state: recovered.state,
    fileName: recovered.fileName,
    checkedAt: recovered.checkedAt,
    reasonCode: recovered.reasonCode || null
    };
  }
}
function init(options) {
  notify = options && typeof options.notify === 'function' ? options.notify : function () {};
  syncActive = options && typeof options.syncActive === 'function' ? options.syncActive : function () { return false; };
  syncRecoveryState = options && typeof options.syncRecoveryState === 'function' ? options.syncRecoveryState : function () { return 'ready'; };
  recoverLatestAccess();
}

module.exports = {
  ACCOUNT_TTL: ACCOUNT_TTL,
  JOB_RE: JOB_RE,
  init: init,
  startupVerify: startupVerify,
  start: start,
  verifyCandidate: verifyCandidate,
  saveCandidate: saveCandidate,
  currentJob: currentJob,
  busy: busy,
  resetReady: resetReady,
  beginReset: beginReset,
  endReset: endReset,
  clearRuntime: clearRuntime,
  lastJob: lastJob,
  latestCandidate: latestCandidate,
  accessFor: accessFor,
  accountFingerprint: accountFingerprint,
  fileKeyFingerprint: fileKeyFingerprint,
  fresh: fresh,
  maskKey: maskKey,
  publicReasonCode: reasonCodeValue
};
