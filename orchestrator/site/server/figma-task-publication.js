'use strict';

// Durable bridge from the per-task Figma session to immutable design
// generations. A successful screen pull or screen-drift turn is validated
// while that session still owns its writer lease, recorded as ready, and only
// then published pointer-last through figma-sync's common domain publisher.

var crypto = require('crypto');
var path = require('path');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var generation = require('./figma-generation');
var screens = require('./figma-screens');
var configUpdate = require('./project-config-update');
var figma = require('./figma');
var testJobs = require('./figma-test-job');
var designHistory = require('./design-history');
var screenTokenPlans = require('./screen-token-plans');

var DIR = path.join(paths.FIGMA_CACHE_DIR, 'task-publications');
var ID_RE = /^fsj-[a-f0-9]{32}$/;
var STEM_RE = /^TASK_([1-9][0-9]*)_[A-Za-z0-9_]+$/;
var ISO_RE = /^\d{4}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\d|3[01])T(?:[01]\d|2[0-3]):[0-5]\d:[0-5]\d\.\d{3}Z$/;
var MARKER_MAX = 32 * 1024;
var ENTRIES_MAX = 5002;
var ARTIFACTS_MAX = 4999;
var TOTAL_MAX = 64 * 1024 * 1024;
var publisher = null;
var notify = function () {};
// Modules are also loaded in isolated HTTP/unit harnesses that do not run the
// server bootstrap. The real server explicitly enters recovery before it
// exposes sessions; this neutral default keeps read-only harnesses inert.
var initialized = true;
var recoveryError = null;
var outstanding = Object.create(null);
var queue = Promise.resolve();
var resetPaused = false;
var PUBLICATION_ERROR_CODES = Object.freeze({
  'task-publication-marker-invalid': true,
  'task-publication-marker-write-failed': true,
  'task-publication-artifact-unsafe': true,
  'task-surface-directory-invalid': true,
  'task-surface-entry-unsafe': true,
  'task-surface-entry-invalid': true,
  'task-surface-artifact-limit': true,
  'task-surface-size-limit': true,
  'task-surface-index-invalid': true,
  'task-surface-required-artifact-missing': true,
  'task-surface-drift-invalid': true,
  'task-publication-stem-invalid': true,
  'task-publication-action-invalid': true,
  'task-publication-binding-unavailable': true,
  'task-publication-recovery-failed': true,
  'task-publication-recovering': true,
  'task-publication-session-invalid': true,
  'task-token-plan-invalid': true,
  'task-token-health-update-failed': true,
  'task-publication-marker-cleanup-failed': true,
  'task-publication-completion-proof-missing': true,
  'TOKEN_TASK_PLAN_INVALID': true,
  'TOKEN_TASK_SCREEN_INDEX_UNSAFE': true,
  'TOKEN_TASK_SCREEN_INDEX_INVALID': true,
  'TOKEN_TASK_SCREEN_VARIANT_DUPLICATE': true,
  'TOKEN_TASK_SCREEN_VARIANT_MISMATCH': true,
  'TOKEN_TASK_SIDECAR_UNSAFE': true,
  'TOKEN_TASK_SIDECAR_INVALID': true,
  'TOKEN_TASK_SIDECAR_PLAN_MISMATCH': true,
  'TOKEN_TASK_UNPLANNED_VARIANT': true
});

function publicationErrorCode(error, fallback) {
  var code = error && error.message;
  return Object.prototype.hasOwnProperty.call(PUBLICATION_ERROR_CODES, code)
    ? code : (fallback || 'task-publication-validation-failed');
}

function now() { return new Date().toISOString(); }
function id() { return 'fsj-' + crypto.randomBytes(16).toString('hex'); }
function iso(value) {
  if (typeof value !== 'string' || !ISO_RE.test(value)) return false;
  var parsed = new Date(value);
  return Number.isFinite(parsed.getTime()) && parsed.toISOString() === value;
}
function exact(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}
function safeStem(value) {
  if (typeof value !== 'string' || value.length > 120) return null;
  var match = STEM_RE.exec(value);
  return match && Number.isSafeInteger(Number(match[1])) && String(Number(match[1])) === match[1] ? value : null;
}
function markerFile(value) { return ID_RE.test(String(value || '')) ? path.join(DIR, value + '.json') : null; }
function markerValid(value) {
  return exact(value, ['schemaVersion', 'id', 'key', 'action', 'stem', 'state', 'createdAt', 'settledAt',
    'baselineProof', 'inputFingerprint', 'accountFingerprint', 'fileKeyFingerprint', 'screenTokenPlanId']) &&
    value.schemaVersion === 3 && ID_RE.test(String(value.id || '')) &&
    value.key === 'figma:screens:' + value.stem && safeStem(value.stem) &&
    (value.action === 'screen-pull' || value.action === 'screen-drift') &&
    (value.action === 'screen-pull'
      ? /^tokplan_[A-Za-z0-9_-]{16,96}$/.test(String(value.screenTokenPlanId || ''))
      : value.screenTokenPlanId === null) &&
    (value.state === 'pending' || value.state === 'ready') && iso(value.createdAt) &&
    (value.baselineProof === null || generation.HASH_RE.test(String(value.baselineProof || ''))) &&
    (value.state === 'pending' ? value.settledAt === null && value.inputFingerprint === null &&
      value.accountFingerprint === null && value.fileKeyFingerprint === null :
      iso(value.settledAt) && Date.parse(value.settledAt) >= Date.parse(value.createdAt) &&
      generation.HASH_RE.test(String(value.inputFingerprint || '')) &&
      generation.HASH_RE.test(String(value.accountFingerprint || '')) && generation.HASH_RE.test(String(value.fileKeyFingerprint || '')));
}
function atomicMarker(value) {
  if (!markerValid(value)) throw new Error('task-publication-marker-invalid');
  var bytes = Buffer.from(JSON.stringify(value, null, 2) + '\n', 'utf8');
  var result = fileGuards.atomicReplaceRegularFileResult(paths.PROJECT_ROOT, DIR, markerFile(value.id), bytes,
    { create: true, directoryMode: 0o700, mode: 0o600, maxBytes: MARKER_MAX });
  if (!result.ok) throw new Error('task-publication-marker-write-failed');
}
function removeMarker(publicationId) {
  var file = markerFile(publicationId);
  var removed = !!file && fileGuards.unlinkRegularFileUnder(paths.PROJECT_ROOT, DIR, file, { allowMissing: true });
  if (removed) delete outstanding[publicationId];
  return removed;
}
function readMarker(publicationId) {
  var file = markerFile(publicationId);
  if (!file) return null;
  var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, DIR, file, MARKER_MAX);
  if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') return null;
  var value;
  try { value = JSON.parse(hit.bytes.toString('utf8')); } catch (error) { return null; }
  return markerValid(value) && value.id === publicationId ? value : null;
}
function safeFile(directory, file) {
  var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, directory, file, generation.ARTIFACT_MAX);
  if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') throw new Error('task-publication-artifact-unsafe');
  return hit.bytes;
}
function proofFile(action, stem) {
  var name = action === 'screen-pull' ? 'screen-cache-' + stem + '.json' : 'screen-drift-' + stem + '.json';
  return path.join(paths.FIGMA_CACHE_DIR, 'reports', name);
}
function proofHash(action, stem) {
  var file = proofFile(action, stem), directory = path.dirname(file);
  var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, directory, file);
  if (inspected && inspected.status === 'missing') return null;
  return generation.sha(safeFile(directory, file));
}
function artifactRole(domain, logicalPath) {
  return 'surface-artifact:' + crypto.createHash('sha256').update(domain + '\0' + logicalPath).digest('hex').slice(0, 32);
}
function fingerprint(rows) {
  return generation.sha(Buffer.from(JSON.stringify(rows.map(function (row) {
    return { logicalPath: row.logicalPath, hash: row.hash, size: row.size };
  }).sort(function (left, right) { return left.logicalPath.localeCompare(right.logicalPath); })), 'utf8'));
}
function collectSurface(stem) {
  var directory = path.join(paths.FIGMA_CACHE_DIR, 'screens', stem);
  var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, directory, ENTRIES_MAX);
  if (!listed.ok) throw new Error('task-surface-directory-invalid');
  var names = [], total = 0;
  listed.names.sort(function (left, right) { return left.localeCompare(right); }).forEach(function (name) {
    var file = path.join(directory, name);
    var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, directory, file);
    if (!inspected || inspected.status !== 'present' || !inspected.stat || inspected.stat.isSymbolicLink()) {
      throw new Error('task-surface-entry-unsafe');
    }
    if (name === '.drift' && inspected.stat.isDirectory()) return;
    if (!inspected.stat.isFile() || !/^[A-Za-z0-9][A-Za-z0-9_.-]{0,199}\.(?:json|png)$/.test(name)) {
      throw new Error('task-surface-entry-invalid');
    }
    names.push(name);
  });
  if (!names.length || names.length > ARTIFACTS_MAX || names.indexOf('index.json') < 0) {
    throw new Error('task-surface-artifact-limit');
  }
  var loaded = Object.create(null);
  names.forEach(function (name) {
    var bytes = safeFile(directory, path.join(directory, name));
    total += bytes.length;
    if (total > TOTAL_MAX) throw new Error('task-surface-size-limit');
    loaded[name] = bytes;
  });
  var index;
  try { index = JSON.parse(loaded['index.json'].toString('utf8')); } catch (error) { throw new Error('task-surface-index-invalid'); }
  if (!screens.validScreenIndex(index, stem)) throw new Error('task-surface-index-invalid');
  var required = Object.create(null), proofRequired = Object.create(null);
  required['index.json'] = 1; proofRequired['index.json'] = 1;
  Object.keys(index.nodes).forEach(function (screen) {
    var node = index.nodes[screen];
    node.variants.forEach(function (variant) {
      required[variant.imageFile] = 1;
      required[variant.tokensFile] = 1;
      proofRequired[variant.tokensFile] = 1;
      if (variant.specFile) required[variant.specFile] = 1;
      if (variant.instancesFile) required[variant.instancesFile] = 1;
    });
    if (node.url) {
      required[screen + '.spec.json'] = 1;
      required[screen + '.png'] = 1;
      required[screen + '.instances.json'] = 1;
      required[screen + '.context.json'] = 1;
      proofRequired[screen + '.spec.json'] = 1;
      proofRequired[screen + '.png'] = 1;
      proofRequired[screen + '.instances.json'] = 1;
      proofRequired[screen + '.context.json'] = 1;
    }
    if (node.darkUrl) {
      required[screen + '.dark.spec.json'] = 1;
      required[screen + '.dark.png'] = 1;
      proofRequired[screen + '.dark.spec.json'] = 1;
      proofRequired[screen + '.dark.png'] = 1;
    }
  });
  if (Object.keys(required).some(function (name) { return !loaded[name]; })) throw new Error('task-surface-required-artifact-missing');
  var domain = 'surface:' + stem.toLowerCase();
  var artifacts = names.map(function (name) {
    var bytes = loaded[name];
    var logicalPath = 'orchestrator/.cache/figma/screens/' + stem + '/' + name;
    return {
      source: stem + '/' + name,
      logicalPath: logicalPath,
      role: name === 'index.json' ? generation.surfaceIndexRole(stem) : artifactRole(domain, logicalPath),
      persistence: 'committed', required: true, schemaVersion: 1,
      bytes: bytes, hash: generation.sha(bytes), size: bytes.length
    };
  });
  return {
    group: 'surfaces', domain: domain, artifacts: artifacts,
    requiredNames: Object.keys(proofRequired).sort(), fingerprint: fingerprint(artifacts)
  };
}
function validDriftReport(value, stem) {
  if (!value || typeof value !== 'object' || Array.isArray(value) || value.schemaVersion !== 1 ||
      value.taskStem !== stem || value.reportRelPath !== 'orchestrator/.cache/figma/reports/screen-drift-' + stem + '.json' ||
      !iso(value.generatedAt) || ['PASS', 'WARN', 'BLOCKER', 'INCOMPLETE'].indexOf(value.overall) < 0 ||
      !Array.isArray(value.screens) || !value.screens.length || value.screens.length > screens.MAX_SCREEN_DRIFT_ROWS) return false;
  return value.screens.every(function (row) {
    return row && typeof row === 'object' && screens.validScreenName(row.screen) &&
      (row.theme === 'primary' || row.theme === 'dark') &&
      ['CLEAN', 'DRIFTED', 'NOT_CHECKED'].indexOf(row.status) >= 0 && Array.isArray(row.changes) && row.changes.length <= 30 &&
      row.changes.every(function (change) { return typeof change === 'string' && change.length <= 2048; });
  });
}
function validScreenCacheProof(stem, snapshot) {
  var file = proofFile('screen-pull', stem), bytes = safeFile(path.dirname(file), file), value;
  try { value = JSON.parse(bytes.toString('utf8')); } catch (error) { return false; }
  if (!(value && typeof value === 'object' && !Array.isArray(value) && value.schemaVersion === 1 &&
    value.taskStem === stem && value.mode === 'gate' && (value.overall === 'PASS' || value.overall === 'WARN') && value.blockingCount === 0 &&
    iso(value.generatedAt) &&
    value.reportRelPath === 'orchestrator/.cache/figma/reports/screen-cache-' + stem + '.json' &&
    value.inputHashes && typeof value.inputHashes === 'object' && !Array.isArray(value.inputHashes))) return false;
  var byName = Object.create(null);
  snapshot.artifacts.forEach(function (artifact) {
    byName[path.basename(artifact.logicalPath)] = artifact.hash;
  });
  var reported = Object.create(null);
  Object.keys(value.inputHashes).forEach(function (inputPath) {
    var normalized = inputPath.split(path.sep).join('/');
    var marker = '/screens/' + stem + '/';
    var offset = normalized.lastIndexOf(marker);
    if (offset >= 0) reported[normalized.slice(offset + marker.length)] = value.inputHashes[inputPath];
  });
  return Array.isArray(snapshot.requiredNames) && snapshot.requiredNames.every(function (name) {
    return reported[name] === byName[name];
  });
}
function collectDrift(stem) {
  var name = 'screen-drift-' + stem + '.json';
  var directory = path.join(paths.FIGMA_CACHE_DIR, 'reports');
  var file = path.join(directory, name), bytes = safeFile(directory, file), value;
  try { value = JSON.parse(bytes.toString('utf8')); } catch (error) { throw new Error('task-surface-drift-invalid'); }
  if (!validDriftReport(value, stem)) throw new Error('task-surface-drift-invalid');
  var domain = 'surface-drift:' + stem.toLowerCase();
  var artifact = {
    source: name, logicalPath: 'orchestrator/.cache/figma/reports/' + name,
    role: generation.surfaceDriftRole(stem), persistence: 'committed', required: true, schemaVersion: 1,
    bytes: bytes, hash: generation.sha(bytes), size: bytes.length
  };
  return { group: 'drift', domain: domain, artifacts: [artifact], fingerprint: fingerprint([artifact]) };
}
function collect(action, stem) {
  if (!safeStem(stem)) throw new Error('task-publication-stem-invalid');
  if (action === 'screen-pull') return collectSurface(stem);
  if (action === 'screen-drift') return collectDrift(stem);
  throw new Error('task-publication-action-invalid');
}
function binding() {
  var config = configUpdate.read();
  var account = figma.account();
  var accountFingerprint = testJobs.accountFingerprint(account);
  var fileKeyFingerprint = config.ok && config.figmaFileKey ? testJobs.fileKeyFingerprint(config.figmaFileKey) : null;
  if (!accountFingerprint || !fileKeyFingerprint) throw new Error('task-publication-binding-unavailable');
  return { accountFingerprint: accountFingerprint, fileKeyFingerprint: fileKeyFingerprint };
}
function prepareTurn(info) {
  if (!info || typeof info.key !== 'string' || info.key.indexOf('figma:screens:') !== 0 ||
      (info.action !== 'screen-pull' && info.action !== 'screen-drift')) return null;
  if (recoveryError) throw new Error('task-publication-recovery-failed');
  if (resetPaused) throw new Error('task-publication-recovering');
  if (!initialized) throw new Error('task-publication-recovering');
  var stem = safeStem(info.stem || info.key.slice('figma:screens:'.length));
  if (!stem || info.key !== 'figma:screens:' + stem) throw new Error('task-publication-session-invalid');
  var tokenPlanId = info.action === 'screen-pull' ? info.screenTokenPlanId : null;
  if (info.action === 'screen-pull') {
    var tokenPlan = screenTokenPlans.read(tokenPlanId);
    if (!tokenPlan || tokenPlan.taskStem !== stem) throw new Error('task-token-plan-invalid');
  }
  var marker = {
    schemaVersion: 3, id: id(), key: info.key, action: info.action, stem: stem,
    state: 'pending', createdAt: now(), settledAt: null,
    baselineProof: proofHash(info.action, stem), inputFingerprint: null,
    accountFingerprint: null, fileKeyFingerprint: null,
    screenTokenPlanId: tokenPlanId
  };
  atomicMarker(marker);
  return marker.id;
}
function markResult(publicationId, success) {
  if (resetPaused) return { ready: false, error: 'figma-session-active' };
  if (!publicationId) return { ready: false };
  var marker = readMarker(publicationId);
  if (!marker || marker.state !== 'pending') {
    failRecovery(new Error('task-publication-marker-invalid'));
    return { ready: false, error: 'task-publication-marker-invalid' };
  }
  if (!success) {
    if (marker.screenTokenPlanId) {
      try { screenTokenPlans.fail(marker.screenTokenPlanId, 'TOKEN_TASK_CAPTURE_FAILED'); }
      catch (error) { failRecovery(error); return { ready: false, error: 'task-token-health-update-failed' }; }
    }
    if (!removeMarker(publicationId)) {
      failRecovery(new Error('task-publication-marker-cleanup-failed'));
      return { ready: false, error: 'task-publication-marker-cleanup-failed' };
    }
    return { ready: false };
  }
  try {
    var snapshot = collect(marker.action, marker.stem);
    if (marker.screenTokenPlanId) screenTokenPlans.validateCaptured(marker.screenTokenPlanId);
    var sourceBinding = binding();
    var currentProof = proofHash(marker.action, marker.stem);
    if (!currentProof || currentProof === marker.baselineProof ||
        marker.action === 'screen-pull' && !validScreenCacheProof(marker.stem, snapshot)) {
      throw new Error('task-publication-completion-proof-missing');
    }
    marker.state = 'ready'; marker.settledAt = now(); marker.inputFingerprint = snapshot.fingerprint;
    marker.accountFingerprint = sourceBinding.accountFingerprint;
    marker.fileKeyFingerprint = sourceBinding.fileKeyFingerprint;
    atomicMarker(marker);
    outstanding[publicationId] = 1;
    return { ready: true };
  } catch (error) {
    if (marker.screenTokenPlanId) {
      try { screenTokenPlans.fail(marker.screenTokenPlanId, 'TOKEN_TASK_CAPTURE_INVALID'); }
      catch (healthError) { failRecovery(healthError); }
    }
    if (!removeMarker(publicationId)) failRecovery(new Error('task-publication-marker-cleanup-failed'));
    return { ready: false, error: publicationErrorCode(error) };
  }
}
function publishOne(publicationId) {
  var marker = readMarker(publicationId);
  if (!marker || marker.state !== 'ready') return Promise.reject(new Error('task-publication-marker-invalid'));
  var snapshot;
  try {
    snapshot = collect(marker.action, marker.stem);
    if (snapshot.fingerprint !== marker.inputFingerprint) {
      if (!removeMarker(publicationId)) throw new Error('task-publication-marker-cleanup-failed');
      failRecovery(new Error('task-publication-input-changed'));
      return Promise.reject(new Error('task-publication-input-changed'));
    }
  } catch (error) {
    if (!removeMarker(publicationId)) error = new Error('task-publication-marker-cleanup-failed');
    failRecovery(error);
    return Promise.reject(error);
  }
  return publisher({
    id: marker.id,
    accountFingerprint: marker.accountFingerprint,
    fileKeyFingerprint: marker.fileKeyFingerprint,
    inputFingerprint: snapshot.fingerprint,
    completed: [{
      group: snapshot.group, domain: snapshot.domain, inputFingerprint: snapshot.fingerprint,
      stage: { artifacts: snapshot.artifacts, messages: [] }
    }],
    verifyInputs: function () {
      try {
        var currentBinding = binding();
        return currentBinding.accountFingerprint === marker.accountFingerprint &&
          currentBinding.fileKeyFingerprint === marker.fileKeyFingerprint &&
          collect(marker.action, marker.stem).fingerprint === snapshot.fingerprint;
      }
      catch (error) { return false; }
    }
  }).then(function (generationId) {
    if (!removeMarker(publicationId)) {
      failRecovery(new Error('task-publication-marker-cleanup-failed'));
      throw new Error('task-publication-marker-cleanup-failed');
    }
    try { notify('figma-task-publication', { stem: marker.stem, action: marker.action, generationId: generationId }); } catch (error) {}
    return designHistory.recordCurrent().then(function () { return true; }, function () { return true; });
  });
}
function schedule(publicationId) {
  if (!publicationId || !initialized || resetPaused) return;
  queue = queue.then(function () { return publishOne(publicationId); }).catch(function (error) {
    failRecovery(error || new Error('task-publication-publication-failed'));
    console.error('[figma-task-publication] publication recovery failed:', publicationErrorCode(error, 'task-publication-publication-failed'));
    return false;
  });
}
function dispatch(publicationId) { schedule(publicationId); }
function abort(publicationId) {
  if (resetPaused) return;
  var marker = publicationId ? readMarker(publicationId) : null;
  if (marker && marker.screenTokenPlanId) {
    try { screenTokenPlans.fail(marker.screenTokenPlanId, 'TOKEN_TASK_CAPTURE_CANCELLED'); }
    catch (error) { failRecovery(error); }
  }
  if (publicationId && !removeMarker(publicationId)) {
    failRecovery(new Error('task-publication-marker-cleanup-failed'));
  }
}
function init(options) {
  publisher = options && typeof options.publishDomains === 'function' ? options.publishDomains : null;
  notify = options && typeof options.notify === 'function' ? options.notify : function () {};
  if (!publisher) return Promise.reject(new Error('task-publication-publisher-missing'));
  recoveryError = null;
  var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, DIR, ENTRIES_MAX);
  if (!listed.ok) {
    var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, path.dirname(DIR), DIR);
    if (!(inspected && inspected.status === 'missing')) return Promise.reject(new Error('task-publication-directory-invalid'));
    initialized = true;
    return Promise.resolve(0);
  }
  var ready = [];
  for (var i = 0; i < listed.names.length; i++) {
    var match = /^(fsj-[a-f0-9]{32})\.json$/.exec(listed.names[i]);
    if (!match) return Promise.reject(new Error('task-publication-entry-invalid'));
    var marker = readMarker(match[1]);
    if (!marker) return Promise.reject(new Error('task-publication-marker-invalid'));
    if (marker.state === 'pending') {
      if (marker.screenTokenPlanId) screenTokenPlans.fail(marker.screenTokenPlanId, 'TOKEN_TASK_CAPTURE_INTERRUPTED');
      if (!removeMarker(marker.id)) return Promise.reject(new Error('task-publication-marker-cleanup-failed'));
    } else { outstanding[marker.id] = 1; ready.push(marker.id); }
  }
  initialized = true;
  ready.forEach(schedule);
  return Promise.resolve(ready.length);
}
function failRecovery(error) {
  recoveryError = publicationErrorCode(error, 'task-publication-recovery-failed');
  initialized = false;
}
function beginRecovery() { initialized = false; recoveryError = null; }
function recoveryState() { return recoveryError ? 'failed' : initialized ? 'ready' : 'recovering'; }
function busy() { return recoveryState() !== 'ready' || Object.keys(outstanding).length > 0; }
function resetReady() { return Object.keys(outstanding).length === 0; }
function beginReset() {
  if (!resetReady()) return false;
  resetPaused = true;
  return true;
}
function endReset() { resetPaused = false; }
function clearRuntime() {
  if (!resetReady()) return false;
  initialized = true;
  recoveryError = null;
  outstanding = Object.create(null);
  queue = Promise.resolve();
  return true;
}

module.exports = {
  init: init,
  prepareTurn: prepareTurn,
  markResult: markResult,
  dispatch: dispatch,
  abort: abort,
  collect: collect,
  failRecovery: failRecovery,
  beginRecovery: beginRecovery,
  recoveryState: recoveryState,
  busy: busy,
  resetReady: resetReady,
  beginReset: beginReset,
  endReset: endReset,
  clearRuntime: clearRuntime,
  publicationErrorCode: publicationErrorCode
};
