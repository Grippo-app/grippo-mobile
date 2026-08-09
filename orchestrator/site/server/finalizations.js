'use strict';

// Marker-first recovery facade for finalize-task.mjs. This module never
// reconstructs state from INDEX.json or the task lock: the durable marker is
// the sole authority, so recovery remains available when the move already
// landed but INDEX regeneration did not.

var fs = require('fs');
var path = require('path');
var os = require('os');
var crypto = require('crypto');
var childProcess = require('child_process');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var writerLeases = require('../../tasks/writer-leases.cjs');
var taskStateCore = require('../../tasks/task-state-core.cjs');
var isTestInjectionKey = require('./child-env').isTestInjectionKey;
var writerLeaseInspector = require('./writer-lease-inspector');
var creationMarkers = require('./creation-markers');
var editMarkers = require('./edit-markers');

var DIR = paths.FINALIZATIONS_DIR;
var STEM_RE = taskStateCore.STEM_RE;
var ETAG_RE = /^sha256:[a-f0-9]{64}$/;
var RUN_ID_RE = /^[A-Za-z0-9][A-Za-z0-9._-]{0,159}$/;
var MAX_MARKER = 256 * 1024;
var MAX_REPLACE_WAL = 40 * 1024 * 1024;
var MAX_OUTPUT = 16 * 1024;
var DEFAULT_TIMEOUT_MS = 20 * 60 * 1000;
var MIN_TIMEOUT_MS = 100;
var MAX_TIMEOUT_MS = 60 * 60 * 1000;
var TERMINATION_GRACE_MS = 1000;
var TERMINATION_VERIFY_MS = 5000;
var TERMINATION_POLL_MS = 50;
var MAX_RUNTIME_ENTRIES = 10000;
var PHASES = { outcome: 1, components: 1, tokens: 1, ship: 1, index: 1, arch: 1, verify: 1, unlock: 1, cleanup: 1 };
var PHASE_STATES = { pending: 1, running: 1, failed: 1, succeeded: 1, skipped: 1 };
var IDENTITY_PROOF_FIELDS = ['ctimeNs', 'dev', 'hash', 'ino', 'kind', 'mode', 'mtimeNs', 'size'];
var children = Object.create(null);
var lastRuns = Object.create(null);
var localHost = os.hostname();
function windowsJobMode() { return process.platform === 'win32' || process.env.FINALIZATION_TEST_WINDOWS_JOB === '1'; }

function bounded(value, max) {
  var s = String(value == null ? '' : value);
  max = max || 500;
  return s.length <= max ? s : s.slice(0, max - 1) + '…';
}
var PUBLIC_ERROR_CODES = new Set([
  'BAD_MARKER_NAME',
  'FINALIZATION_MARKER_RECOVERY_UNSAFE',
  'MARKER_CORRUPT',
  'MARKER_DIR_UNAVAILABLE',
  'MARKER_DIR_UNSAFE',
  'MARKER_DIR_TOO_LARGE',
  'MARKER_INVALID',
  'MARKER_READ_FAILED',
  'MARKER_TOO_LARGE',
  'MARKER_VERSION_UNSUPPORTED',
  'UNSAFE_MARKER'
]);
var PUBLIC_BUSY_CODES = new Set([
  'FINALIZATION_MUTEX_BUSY',
  'FINALIZATION_SERVER_BUSY',
  'MUTEX_CORRUPT',
  'MUTEX_INVALID',
  'WORKSPACE_WRITER_ACTIVE',
  'WRITER_LEASE_DIR_UNSAFE'
]);
var PUBLIC_PROCESS_CODES = new Set([
  'FINALIZATION_PROCESS_FAILED',
  'FINALIZATION_SPAWN_FAILED',
  'FINALIZATION_TREE_UNVERIFIED'
]);
function publicErrorCode(value) {
  var code = typeof value === 'string' ? value : '';
  if (PUBLIC_ERROR_CODES.has(code)) return 'finalization-state-invalid';
  if (PUBLIC_BUSY_CODES.has(code)) return 'finalization-owner-unavailable';
  if (code === 'NODE_VERSION_UNSUPPORTED') return 'finalization-runtime-unsupported';
  if (code === 'FINALIZATION_TIMEOUT') return 'finalization-timeout';
  if (PUBLIC_PROCESS_CODES.has(code)) return 'finalization-process-failed';
  if (/^FINALIZATION_[A-Z0-9_]{1,68}$/.test(code)) return 'finalization-step-failed';
  return code ? 'finalization-unavailable' : null;
}
function publicProjection(value) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return null;
  return {
    version: value.version === 1 ? 1 : null,
    stem: typeof value.stem === 'string' ? value.stem : '',
    status: value.status,
    state: value.state,
    phase: value.phase,
    observedColumn: value.observedColumn,
    revision: value.revision,
    etag: value.etag,
    createdAt: value.createdAt,
    updatedAt: value.updatedAt,
    recoveryRunning: value.recoveryRunning === true,
    recoverable: value.recoverable === true,
    errorCode: publicErrorCode(value.errorCode)
  };
}
function digest(raw) { return 'sha256:' + crypto.createHash('sha256').update(raw).digest('hex'); }
function validStem(stem) { return taskStateCore.safeIntegerId(stem) !== null; }
function markerPath(stem) { return validStem(stem) ? path.join(DIR, stem + '.json') : null; }
function exactFields(value, fields) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return false;
  var keys = Object.keys(value).sort();
  return keys.length === fields.length && keys.every(function (key, index) { return key === fields[index]; });
}
function validIdentityProof(value) {
  if (!exactFields(value, IDENTITY_PROOF_FIELDS)) return false;
  return ETAG_RE.test(String(value.hash || '')) && value.kind === 'file' &&
    ['dev', 'ino'].every(function (key) { return /^(?:0|[1-9][0-9]*)$/.test(String(value[key] || '')); }) &&
    ['mtimeNs', 'ctimeNs'].every(function (key) {
      return /^-?(?:0|[1-9][0-9]*)$/.test(String(value[key] || '')) && value[key] !== '-0';
    }) &&
    ['mode', 'size'].every(function (key) { return Number.isSafeInteger(value[key]) && value[key] >= 0; });
}
function processStartIdValid(value) {
  return process.platform === 'linux' || process.platform === 'darwin' || process.platform === 'win32'
    ? writerLeases.PROCESS_START_ID_RE.test(String(value || ''))
    : value === null;
}
function processOwnerActive(owner) {
  if (!owner || !Number.isInteger(owner.pid) || owner.pid <= 0) return false;
  if (owner.hostname !== localHost) return true;
  var state = writerLeases.processIdentityState(owner.pid, owner.processStartId);
  return state !== 'dead' && state !== 'reused';
}
function sameDirectorySnapshot(left, right) {
  return left && right && left.dev === right.dev && left.ino === right.ino && left.modeExact === right.modeExact &&
    left.sizeExact === right.sizeExact && left.mtimeNs === right.mtimeNs && left.ctimeNs === right.ctimeNs;
}
function directoryChain(options) {
  var chain = fileGuards.realDirectoryUnder(paths.WRITER_AUTHORITY_ROOT, DIR, options || {});
  if (!chain) {
    var error = new Error('finalization directory contains a symlink, non-directory, or escapes its authority root');
    error.code = 'MARKER_DIR_UNSAFE';
    throw error;
  }
  return chain;
}
function ensureDir() { return directoryChain({ create: true, mode: 0o700 }); }
function boundedDirectoryNames() {
  var listed = fileGuards.boundedDirectoryNamesUnder(paths.WRITER_AUTHORITY_ROOT, DIR, MAX_RUNTIME_ENTRIES);
  if (!listed.ok) {
    var error = new Error(listed.code === 'directory-entry-limit'
      ? 'finalization directory exceeds ' + MAX_RUNTIME_ENTRIES + ' entries'
      : 'finalization directory cannot be enumerated safely');
    error.code = listed.code === 'directory-entry-limit' ? 'MARKER_DIR_TOO_LARGE' : 'MARKER_DIR_UNSAFE';
    throw error;
  }
  return listed.names.slice().sort();
}
function timeoutMs() {
  var raw = Number(process.env.FINALIZATION_TIMEOUT_MS || DEFAULT_TIMEOUT_MS);
  if (!Number.isFinite(raw)) raw = DEFAULT_TIMEOUT_MS;
  return Math.max(MIN_TIMEOUT_MS, Math.min(MAX_TIMEOUT_MS, Math.floor(raw)));
}
function ownerActive(owner) {
  return processOwnerActive(owner);
}
function mutexRecordIssue(owner) {
  if (!owner || typeof owner !== 'object' || Array.isArray(owner)) return 'global finalization mutex owner record is invalid';
  var fields = Object.keys(owner).sort();
  var expected = ['hostname', 'invocationId', 'pid', 'processStartId', 'released', 'startedAt', 'version'];
  if (fields.length !== expected.length || fields.some(function (field, index) { return field !== expected[index]; })) {
    return 'global finalization mutex fields do not match the exact v1 contract';
  }
  if (owner.version !== 1 || !Number.isInteger(owner.pid) || owner.pid <= 0 ||
      typeof owner.hostname !== 'string' || !owner.hostname || owner.hostname.length > 255 ||
      typeof owner.invocationId !== 'string' || !owner.invocationId || owner.invocationId.length > 200 ||
      typeof owner.startedAt !== 'string' || !Number.isFinite(Date.parse(owner.startedAt)) ||
      !processStartIdValid(owner.processStartId) || typeof owner.released !== 'boolean') {
    return 'global finalization mutex owner record is invalid';
  }
  return null;
}
function mutexIssue() {
  var p = path.join(DIR, '.mutex.json'), st, read, owner, parentBefore;
  try {
    parentBefore = directoryChain({ allowMissing: true });
    if (!parentBefore.exists) return null;
    st = fs.lstatSync(p);
    if (st.isSymbolicLink() || !st.isFile() || st.size > 4096) return { code: 'MUTEX_INVALID', message: 'global finalization mutex is unsafe or oversized' };
    read = fileGuards.boundedRegularFileUnder(paths.WRITER_AUTHORITY_ROOT, DIR, p, 4096);
    if (!read) return { code: 'MUTEX_INVALID', message: 'global finalization mutex changed while reading' };
    var parentAfter = directoryChain();
    if (!sameDirectorySnapshot(parentBefore.stat, parentAfter.stat)) return { code: 'MUTEX_INVALID', message: 'global finalization mutex directory changed while reading' };
    owner = JSON.parse(read.bytes.toString('utf8'));
  } catch (e) {
    if (e && e.code === 'ENOENT') return null;
    return { code: 'MUTEX_CORRUPT', message: bounded(e && e.message || e) };
  }
  var invalid = mutexRecordIssue(owner);
  if (invalid) return { code: 'MUTEX_CORRUPT', message: invalid };
  // finalize-lock.py intentionally keeps the stable lock inode after release;
  // this flag distinguishes the harmless owner record from an active flock.
  if (owner.released === true) return null;
  if (!processOwnerActive(owner)) return null;
  return { code: 'FINALIZATION_MUTEX_BUSY', message: 'another finalization owns the global publication mutex' };
}
function writerLeaseIssue() {
  var scan = writerLeases.scan(paths.WRITER_LEASES_DIR, paths.WRITER_AUTHORITY_ROOT);
  if (scan.issues.length) return {
    code: scan.issues[0].code || 'WRITER_LEASE_INVALID',
    message: 'workspace writer lease state is unsafe: ' + bounded(scan.issues[0].message)
  };
  if (!scan.active.length) return null;
  var row = scan.active[0];
  return {
    code: 'WORKSPACE_WRITER_ACTIVE',
    message: 'a workspace-writing turn is still active' + (row.stem ? ' for ' + row.stem : '') +
      (scan.active.length > 1 ? ' (+' + (scan.active.length - 1) + ' more)' : '')
  };
}
function exclusivePublicationLease(row) {
  return row && (row.kind === 'runtime-build' || typeof row.key === 'string' && (
    row.key === 'task:create-backlog' ||
    row.key === 'task:recover-backlog-creations' ||
    row.key === 'task:recover-backlog-edits' ||
    row.key.indexOf('task:edit-backlog:') === 0 ||
    row.key === 'figma:ship-drift-artifacts'
  ));
}
function mutationWriterIssue(stem, options) {
  options = options || {};
  var scan = writerLeases.scan(paths.WRITER_LEASES_DIR, paths.WRITER_AUTHORITY_ROOT);
  if (scan.issues.length) return {
    code: scan.issues[0].code || 'WRITER_LEASE_INVALID',
    message: 'workspace writer lease state is unsafe: ' + bounded(scan.issues[0].message)
  };
  var foreign = scan.active.filter(function (row) { return row.leaseId !== options.ownWriterLeaseId; });
  var conflict = foreign.find(function (row) {
    // Deterministic create/edit helpers republish shared task + INDEX state;
    // ship-drift sweep writes across every shipped task. Their leases therefore
    // gate every other writer even before a later artifact/marker lands.
    if (exclusivePublicationLease(row)) return true;
    // A global deterministic publisher must start only when it is the sole
    // active writer.  Its already-published lease makes later writers lose the
    // opposite side of the same lease-before-second-check handshake.
    if (options.requireSoleWriter === true) return true;
    // Frozen serial safety (pipeline improvement 01, Phase 0): board-task
    // writers are mutually exclusive across stems AND drainers. A second
    // task-session acquisition loses the publish-then-scan handshake against
    // any live board-task writer — a runner session, a standby execution, or
    // a detached orphan surviving a dead site process — not only same-stem.
    // Control-plane writers (workspace-session, runtime-build) keep the
    // narrower rules below. Relaxing this back to per-stem is allowed only
    // atomically with per-task worktree isolation.
    if (options.kind === 'task-session' && row.kind === 'task-session') return true;
    // Never two owners for the same task/key (including a detached orphan
    // left by a dead site process).
    if (stem && row.stem === stem) return true;
    return options.key && row.key === options.key;
  });
  return conflict ? {
    code: 'WORKSPACE_WRITER_ACTIVE',
    message: 'another workspace writer is active' + (conflict.stem ? ' for ' + conflict.stem : '')
  } : null;
}
// Frozen serial safety (pipeline improvement 01, Phase 0): durable evidence
// that a board-task writer is alive OUTSIDE this server process — a child
// orphaned by an unclean site death, a standby /serve-queue execution, or a
// second site process on the same project root. The runner consults this
// before claiming queued work; the in-memory taskRunningCount cap cannot see
// any of those. Scan issues fail closed: an unprovable writer state pauses
// the queue drain instead of admitting a second shared-root writer.
// beginMutation stays the authoritative arbiter via the same lease scan.
function foreignTaskSessionWriterIssue() {
  var scan;
  try { scan = writerLeases.scan(paths.WRITER_LEASES_DIR, paths.WRITER_AUTHORITY_ROOT); }
  catch (e) { return { code: 'WRITER_LEASE_DIR_UNSAFE', message: bounded(e && e.message || e) }; }
  if (scan.issues.length) return {
    code: scan.issues[0].code || 'WRITER_LEASE_INVALID',
    message: 'workspace writer lease state is unsafe: ' + bounded(scan.issues[0].message)
  };
  var row = scan.active.find(function (candidate) {
    return candidate.kind === 'task-session' &&
      !(candidate.owner && candidate.owner.pid === process.pid &&
        writerLeases.processIdentityMatches(candidate.owner.pid, candidate.owner.processStartId));
  });
  return row ? {
    code: 'TASK_WRITER_ACTIVE',
    // Name the owner pid: when the writer is a leaked-but-live CLI lease, the
    // operator's recovery is to end THAT session/process cleanly — the message
    // must say which one instead of leaving a mystery hold.
    message: 'a board-task writer outside this process is active' +
      (row.stem ? ' for ' + row.stem : '') + ' (lease ' + row.leaseId +
      (row.owner && row.owner.pid ? ', owner pid ' + row.owner.pid : '') + ')'
  } : null;
}
// Companion probe: TRUE while THIS live process owns an ACTIVE board-task
// writer lease — i.e. a mutating or warm task-session child of ours is still
// alive. The runner uses it to decide whether the .runner-alive marker must
// keep standing the standby worker down through a CLI auth flip. Scan
// problems return true: an unprovable writer state must not hand the queue
// to a second drainer while our own children may still be writing.
function ownTaskSessionWriterActive() {
  var scan;
  try { scan = writerLeases.scan(paths.WRITER_LEASES_DIR, paths.WRITER_AUTHORITY_ROOT); }
  catch (e) { return true; }
  if (scan.issues.length) return true;
  return scan.active.some(function (candidate) {
    return candidate.kind === 'task-session' &&
      candidate.owner && candidate.owner.pid === process.pid &&
      writerLeases.processIdentityMatches(candidate.owner.pid, candidate.owner.processStartId);
  });
}
function observedColumn(stem) {
  var found = [];
  var specs = [
    ['backlog', path.join(paths.ORCHESTRATOR_DIR, 'tasks', 'backlog', stem + '.md')],
    ['pending', path.join(paths.ORCHESTRATOR_DIR, 'tasks', 'pending', stem + '.questions.md')],
    ['todo', path.join(paths.ORCHESTRATOR_DIR, 'tasks', 'todo', stem + '.md')],
    ['done', path.join(paths.ORCHESTRATOR_DIR, 'tasks', 'done', stem + '.md')]
  ];
  for (var i = 0; i < specs.length; i++) {
    try {
      var st = fs.lstatSync(specs[i][1]);
      if (st.isFile() && !st.isSymbolicLink()) found.push(specs[i][0]);
      else found.push('unsafe-' + specs[i][0]);
    } catch (e) {
      if (!e || e.code !== 'ENOENT') found.push('unreadable-' + specs[i][0]);
    }
  }
  return found.length === 1 ? found[0] : (found.length ? 'conflict:' + found.join(',') : 'missing');
}
function corrupt(stem, code, message, etag) {
  return {
    stem: stem, status: 'corrupt', state: 'corrupt', phase: null,
    observedColumn: validStem(stem) ? observedColumn(stem) : 'unknown',
    revision: null, etag: etag || null, createdAt: null, updatedAt: null,
    recoveryRunning: !!children[stem], recoverable: false,
    errorCode: code, errorMessage: bounded(message)
  };
}
function replacementArtifactStem(name) {
  var match = /^\.(TASK_\d+_[A-Za-z0-9_]+)\.json\.replace-(?:reservation\.json|wal\.json|candidate-[a-f0-9]{32}|detached-[a-f0-9]{32})$/.exec(String(name || ''));
  return match && validStem(match[1]) ? match[1] : null;
}
function replacementArtifacts(stem, names) {
  var prefix = '.' + stem + '.json.replace-';
  return (names || []).filter(function (name) { return name.indexOf(prefix) === 0; }).sort();
}
function replacementProjection(stem, names) {
  var artifacts = replacementArtifacts(stem, names);
  if (!artifacts.length) return null;
  var walName = '.' + stem + '.json.replace-wal.json';
  var reservationName = '.' + stem + '.json.replace-reservation.json';
  var authorityName = artifacts.indexOf(walName) >= 0 ? walName :
    (artifacts.indexOf(reservationName) >= 0 ? reservationName : null);
  if (!authorityName || artifacts.some(function (name) { return replacementArtifactStem(name) !== stem; })) {
    return corrupt(stem, 'FINALIZATION_MARKER_RECOVERY_UNSAFE', 'private marker replacement artifacts are malformed or missing durable intent');
  }
  var authorityPath = path.join(DIR, authorityName);
  var read = fileGuards.boundedRegularFileUnder(paths.WRITER_AUTHORITY_ROOT, DIR, authorityPath, MAX_REPLACE_WAL);
  if (!read) return corrupt(stem, 'FINALIZATION_MARKER_RECOVERY_UNSAFE', 'finalization-marker replacement intent is unsafe, unstable, or oversized');
  var running = !!children[stem];
  var otherServerStem = Object.keys(children).find(function (activeStem) { return activeStem !== stem; });
  var unavailable = !running && otherServerStem ? {
    code: 'FINALIZATION_SERVER_BUSY', message: 'another task finalization is still running: ' + otherServerStem
  } : (!running ? (writerLeaseIssue() || mutexIssue()) : null);
  return {
    version: 1, stem: stem, status: 'incomplete', state: 'incomplete', phase: 'marker-replace-recovery',
    observedColumn: observedColumn(stem), revision: 1, etag: digest(read.bytes),
    createdAt: null, updatedAt: null, recoveryRunning: running,
    recoverable: !running && !unavailable,
    errorCode: unavailable ? unavailable.code : 'FINALIZATION_MARKER_RECOVERY_REQUIRED',
    errorMessage: unavailable ? unavailable.message : 'durable finalization-marker replacement will be reconciled by the canonical finalizer'
  };
}
function markerShapeError(marker, stem) {
  var phase, entry;
  if (!marker || typeof marker !== 'object' || Array.isArray(marker)) return 'finalization marker must contain an object';
  if (marker.version !== 1) return 'unsupported finalization marker version';
  if (marker.stem !== stem) return 'marker stem does not match its filename';
  if (!/^fin-[A-Za-z0-9._-]{1,160}$/.test(String(marker.transactionId || ''))) return 'marker transaction id is invalid';
  if (!Number.isInteger(marker.revision) || marker.revision < 1) return 'marker revision must be a positive integer';
  if (['running', 'incomplete', 'completed'].indexOf(marker.status) < 0) return 'marker status is invalid';
  if (!Object.prototype.hasOwnProperty.call(PHASES, marker.phase)) return 'marker phase is invalid';
  if (typeof marker.createdAt !== 'string' || !Number.isFinite(Date.parse(marker.createdAt)) ||
      typeof marker.updatedAt !== 'string' || !Number.isFinite(Date.parse(marker.updatedAt))) return 'marker timestamps are invalid';
  var validOwner = marker.owner === null || (marker.owner &&
    exactFields(marker.owner, ['hostname', 'invocationId', 'pid', 'processStartId', 'startedAt']) &&
    Number.isInteger(marker.owner.pid) && marker.owner.pid > 0 &&
    typeof marker.owner.hostname === 'string' && marker.owner.hostname && typeof marker.owner.invocationId === 'string' && marker.owner.invocationId &&
    typeof marker.owner.startedAt === 'string' && Number.isFinite(Date.parse(marker.owner.startedAt)) &&
    processStartIdValid(marker.owner.processStartId));
  if (!validOwner || (marker.status === 'running' && marker.owner === null)) return 'marker owner is invalid';
  if (!marker.source || ['originalHash', 'intendedHash', 'intendedLogicalHash', 'outcomeHash', 'snapshotHash', 'publishFromHash'].some(function (key) {
    return !ETAG_RE.test(String(marker.source[key] || ''));
  }) || !marker.source.lock || marker.source.lock.present !== true ||
      !exactFields(marker.source.lock, ['present'].concat(IDENTITY_PROOF_FIELDS).sort()) ||
      !validIdentityProof(Object.fromEntries(IDENTITY_PROOF_FIELDS.map(function (key) { return [key, marker.source.lock[key]]; })))) {
    return 'marker source hashes/lock ownership are invalid';
  }
  if (!marker.phases || typeof marker.phases !== 'object' || Array.isArray(marker.phases)) return 'marker phases are missing';
  if (!marker.figma || typeof marker.figma !== 'object' || typeof marker.figma.enabled !== 'boolean' || !ETAG_RE.test(String(marker.figma.configHash || '')) ||
      (marker.figma.pipelineRunId !== null && typeof marker.figma.pipelineRunId !== 'string')) return 'marker Figma state is invalid';
  if (marker.figma.enabled && !RUN_ID_RE.test(String(marker.figma.pipelineRunId || ''))) return 'enabled Figma marker must carry a valid pinned pipeline run id';
  if (!marker.figma.enabled && marker.figma.pipelineRunId !== null) return 'disabled Figma marker must not carry a pipeline run id';
  for (phase in PHASES) {
    if (!Object.prototype.hasOwnProperty.call(PHASES, phase)) continue;
    entry = marker.phases[phase];
    if (!entry || !Object.prototype.hasOwnProperty.call(PHASE_STATES, entry.state) || !Number.isInteger(entry.attempts) || entry.attempts < 0) {
      return 'marker phase state ' + phase + ' is invalid';
    }
  }
  if (marker.status === 'completed') {
    for (phase in PHASES) {
      if (!Object.prototype.hasOwnProperty.call(PHASES, phase)) continue;
      entry = marker.phases[phase];
      if (entry.state !== 'succeeded' && entry.state !== 'skipped') return 'completed marker does not have every phase succeeded/skipped';
    }
  }
  if (marker.status === 'running' && marker.phases[marker.phase].state !== 'running') return 'running marker phase is not running';
  if (!marker.artifacts || typeof marker.artifacts !== 'object' || Array.isArray(marker.artifacts)) return 'marker artifacts state is invalid';
  if (marker.artifacts.unlockDetached !== undefined && marker.artifacts.unlockDetached !== true) return 'marker unlock proof state is invalid';
  if ((marker.artifacts.tokenMappingHash !== undefined) !== (marker.artifacts.tokenMappingRevision !== undefined)) {
    return 'marker token mapping publication state must carry hash and revision together';
  }
  if (marker.artifacts.tokenMappingHash !== undefined &&
      (!ETAG_RE.test(String(marker.artifacts.tokenMappingHash || '')) ||
        !Number.isInteger(marker.artifacts.tokenMappingRevision) || marker.artifacts.tokenMappingRevision < 1)) {
    return 'marker token mapping publication state is invalid';
  }
  if ((marker.artifacts.componentMappingHash !== undefined) !== (marker.artifacts.componentMappingRevision !== undefined)) {
    return 'marker component mapping publication state must carry hash and revision together';
  }
  if (marker.artifacts.componentMappingHash !== undefined &&
      (!ETAG_RE.test(String(marker.artifacts.componentMappingHash || '')) ||
        !Number.isInteger(marker.artifacts.componentMappingRevision) || marker.artifacts.componentMappingRevision < 1)) {
    return 'marker component mapping publication state is invalid';
  }
  if (marker.artifacts.unlockSourceToken !== undefined && !/^[a-f0-9]{32}$/.test(String(marker.artifacts.unlockSourceToken))) return 'marker unlock source token is invalid';
  if (marker.artifacts.outcomeDetachToken !== undefined && !/^[a-f0-9]{32}$/.test(String(marker.artifacts.outcomeDetachToken))) return 'marker Outcome detach token is invalid';
  var outcomeProofKeys = ['outcomeSourceProof', 'outcomePublicationProof', 'outcomeTargetProof'];
  for (var proofIndex = 0; proofIndex < outcomeProofKeys.length; proofIndex++) {
    var proofKey = outcomeProofKeys[proofIndex];
    if (marker.artifacts[proofKey] !== undefined && !validIdentityProof(marker.artifacts[proofKey])) return 'marker ' + proofKey + ' is invalid';
  }
  if (outcomeProofKeys.some(function (key) { return marker.artifacts[key] !== undefined; }) && marker.artifacts.outcomeDetachToken === undefined) {
    return 'marker Outcome proof is missing its detach token';
  }
  if (marker.lastError !== null && (!marker.lastError || typeof marker.lastError !== 'object' || typeof marker.lastError.code !== 'string' ||
      typeof marker.lastError.message !== 'string' || typeof marker.lastError.at !== 'string' || !Number.isFinite(Date.parse(marker.lastError.at)))) return 'marker lastError is invalid';
  return null;
}
function effectiveError(marker, stem) {
  var persisted = marker.lastError && typeof marker.lastError === 'object' ? marker.lastError : null;
  var run = lastRuns[stem];
  if (persisted && !run) return persisted;
  if (!run) return null;
  if (persisted) {
    var persistedTime = Date.parse(persisted.at || '');
    var runStart = Date.parse(run.startedAt || '');
    // A structured marker error written by this recovery attempt is more
    // precise than the wrapper process exit. An older/stampless marker error
    // must not hide a newer crash/timeout retained by the server.
    if (Number.isFinite(persistedTime) && Number.isFinite(runStart) && persistedTime >= runStart) return persisted;
  }
  var markerTime = Date.parse(marker.updatedAt || '');
  var runTime = Date.parse(run.at || '');
  if (Number.isFinite(markerTime) && Number.isFinite(runTime) && runTime < markerTime) return null;
  return run;
}
function readOne(stem) {
  if (!validStem(stem)) return corrupt(stem, 'BAD_MARKER_NAME', 'unsafe finalization marker filename');
  var parentBefore;
  try {
    parentBefore = directoryChain({ allowMissing: true });
    if (!parentBefore.exists) return corrupt(stem, 'MARKER_READ_FAILED', 'finalization marker directory does not exist');
  }
  catch (directoryError) { return corrupt(stem, directoryError.code || 'MARKER_DIR_UNSAFE', directoryError.message); }
  var runtimeNames;
  try { runtimeNames = boundedDirectoryNames(); }
  catch (listError) { return corrupt(stem, listError.code || 'MARKER_DIR_UNSAFE', listError.message); }
  var replacement = replacementProjection(stem, runtimeNames);
  if (replacement) return replacement;
  var p = markerPath(stem), st, read, raw;
  try {
    st = fs.lstatSync(p);
    if (st.isSymbolicLink() || !st.isFile()) return corrupt(stem, 'UNSAFE_MARKER', 'marker must be a regular file');
    if (st.size > MAX_MARKER) return corrupt(stem, 'MARKER_TOO_LARGE', 'marker exceeds the size limit');
    read = fileGuards.boundedRegularFileUnder(paths.WRITER_AUTHORITY_ROOT, DIR, p, MAX_MARKER);
    if (!read) return corrupt(stem, 'MARKER_READ_FAILED', 'marker changed while reading');
    var parentAfter = directoryChain();
    if (!sameDirectorySnapshot(parentBefore.stat, parentAfter.stat)) return corrupt(stem, 'MARKER_READ_FAILED', 'marker directory changed while reading');
    raw = read.bytes;
  } catch (e) { return corrupt(stem, 'MARKER_READ_FAILED', e && e.message || e); }
  var etag = digest(raw), marker;
  try { marker = JSON.parse(raw.toString('utf8')); }
  catch (e2) { return corrupt(stem, 'MARKER_CORRUPT', e2.message, etag); }
  var shapeError = markerShapeError(marker, stem);
  if (shapeError) return corrupt(stem, marker && marker.version !== 1 ? 'MARKER_VERSION_UNSUPPORTED' : 'MARKER_INVALID', shapeError, etag);
  // Self-heal on read via the single reaper: release any retained ownership
  // record whose process group is provably dead — this stem's own record
  // stranded past termination verification, AND any other stem's, so both the
  // recoverable verdict and the cross-stem FINALIZATION_SERVER_BUSY surface
  // below never report a provably-dead group as busy. Fail-closed for a live
  // group; no server restart required.
  reapProvablyDeadFinalizations();
  var last = effectiveError(marker, stem);
  var running = !!children[stem] || ownerActive(marker.owner);
  var treeIssue = children[stem] && children[stem].treeUnverified ? lastRuns[stem] : null;
  var runtimeUnsupported = !!(marker.figma && marker.figma.enabled) && Number(String(process.versions.node || '').split('.')[0]) < 22;
  var mutex = mutexIssue();
  var writer = writerLeaseIssue();
  var otherServerStem = Object.keys(children).find(function (activeStem) { return activeStem !== stem; });
  var unavailable = runtimeUnsupported ? {
    code: 'NODE_VERSION_UNSUPPORTED',
    message: 'Figma finalization requires Node >=22; restart the site with a supported Node runtime.'
  } : (!running && otherServerStem ? {
    code: 'FINALIZATION_SERVER_BUSY',
    message: 'another task finalization is still running or its process group is not yet proven dead: ' + otherServerStem
  } : (!running ? (writer || mutex) : null));
  return {
    version: 1,
    stem: stem,
    status: marker.status === 'completed' ? 'completed' : 'incomplete',
    state: marker.status === 'completed' ? 'completed' : 'incomplete',
    phase: marker.phase,
    observedColumn: observedColumn(stem),
    revision: marker.revision,
    etag: etag,
    createdAt: typeof marker.createdAt === 'string' ? marker.createdAt : null,
    updatedAt: typeof marker.updatedAt === 'string' ? marker.updatedAt : null,
    recoveryRunning: running,
    recoverable: !running && !unavailable,
    errorCode: unavailable ? unavailable.code : (treeIssue && treeIssue.code ? treeIssue.code : (last && typeof last.code === 'string' ? bounded(last.code, 80) : null)),
    errorMessage: unavailable ? unavailable.message : (treeIssue && treeIssue.message ? bounded(treeIssue.message) : (last && typeof last.message === 'string' ? bounded(last.message) : null))
  };
}
function list() {
  var names;
  try { names = boundedDirectoryNames(); }
  catch (e) {
    if (e && e.code === 'ENOENT') return [];
    return [corrupt('__FINALIZATIONS__', 'MARKER_DIR_UNAVAILABLE', e && e.message || e)];
  }
  var stems = Object.create(null), invalid = [];
  names.forEach(function (name) {
    if (name === '.mutex.json') return;
    var replacementStem = replacementArtifactStem(name);
    if (replacementStem) { stems[replacementStem] = true; return; }
    if (name.endsWith('.json')) {
      var markerStem = name.slice(0, -5);
      if (validStem(markerStem)) stems[markerStem] = true;
      else invalid.push(markerStem);
    }
  });
  return Object.keys(stems).sort().map(readOne).concat(invalid.sort().map(function (value) {
    return corrupt(value, 'BAD_MARKER_NAME', 'unsafe finalization marker filename');
  }));
}
function hasMarker(stem) {
  var p = markerPath(stem);
  if (!p) return false;
  try {
    var chain = directoryChain({ allowMissing: true });
    if (!chain.exists) return false;
  } catch (directoryError) { return true; }
  var entry = fileGuards.inspectEntryUnder(paths.WRITER_AUTHORITY_ROOT, DIR, p);
  if (entry.status !== 'missing') return true;
  try { return replacementArtifacts(stem, boundedDirectoryNames()).length > 0; }
  catch (listError) { return true; }
}
function hasAnyMarker() {
  var names;
  try { names = boundedDirectoryNames(); }
  catch (e) { return true; }
  for (var i = 0; i < names.length; i++) {
    if (names[i] !== '.mutex.json' && (names[i].endsWith('.json') || names[i].indexOf('.replace-') >= 0)) return true;
  }
  return false;
}
// Mutation guard closes the marker-creation window: finalize-task acquires the
// global flock before it prepares/publishes its first marker. While that flock
// is active, task/session/lock writers must wait even if the stem marker has
// not landed yet. A GLOBAL mutation (stem == null: registry/evidence/Figma
// sessions/create-backlog) is also blocked by ANY durable marker, including an
// invalid filename or an unreadable marker directory. Corrupt/unreadable state
// is deliberately fail-closed.
function mutationBlocked(stem, options) {
  options = options || {};
  // Self-heal first: release any retained ownership record whose process group
  // is already provably dead (ESRCH / Windows drain) but was stranded past the
  // termination-verification deadline with no marker left to trigger readOne's
  // lazy re-probe. The next mutation attempt (create/run) therefore clears the
  // block on its own — no server restart. Release proof is unchanged, so a live
  // or reused group still blocks below (fail-closed).
  reapProvablyDeadFinalizations();
  // Server-owned recovery/termination records are an authority even if an
  // external actor removed the marker or the mutex helper already released.
  // In particular, a timed-out descendant retained for group verification must
  // block every workspace mutation until its process group is provably dead.
  if (Object.keys(children).length) return true;
  // Every finalization mutates shared INDEX/architecture/registry state. An
  // idle marker for task A therefore blocks task B too; allowing unrelated
  // task sessions while A awaits recovery would defeat the global transaction.
  if (hasAnyMarker()) return true;
  // Creation and finalization both publish task files plus the shared INDEX.
  // A crashed create therefore blocks every writer just like a finalization
  // marker. The deterministic creator may resume its sole matching receipt;
  // corrupt state or multiple incomplete transactions remain fail-closed.
  if (creationMarkers.blockingIssue(options.creationKeyHash || null, options.allowAllCreationRecovery === true)) return true;
  if (editMarkers.blockingIssue(stem, options.allowAllEditRecovery === true)) return true;
  if (mutationWriterIssue(stem, options)) return true;
  return !!mutexIssue();
}
// Writer/finalizer handshake. Publication of the lease happens before the
// finalization re-check; finalize-task takes the opposite side by acquiring its
// kernel mutex and then scanning active leases. Whichever side wins is visible
// to the other before either begins a workspace mutation.
function beginMutation(options) {
  options = options || {};
  var handle;
  try {
    handle = writerLeases.acquire(paths.WRITER_LEASES_DIR, {
      kind: options.kind,
      stem: options.stem || null,
      sessionId: options.sessionId || null,
      key: options.key || null,
      ownerPid: process.pid,
      // Publish fail-closed before spawn. updateChildPid durably clears this
      // pending state in the same replacement that records the child PID/PGID,
      // so SIGKILL between spawn and attach can never expose a verified-null
      // lease while an untracked writer is alive.
      pendingChild: options.pendingChild !== false,
      ttlMs: options.ttlMs || 0,
      rootDir: paths.WRITER_AUTHORITY_ROOT
    });
  } catch (e) {
    return { ok: false, error: 'writer-lease-unavailable', detail: bounded(e && e.message || e) };
  }
  // Mandatory second half of the handshake. Never move this check before the
  // lease publication: that would recreate the check -> spawn/write race.
  if (mutationBlocked(options.stem || null, {
    creationKeyHash: options.creationKeyHash || null,
    allowAllCreationRecovery: options.allowAllCreationRecovery === true,
    allowAllEditRecovery: options.allowAllEditRecovery === true,
    ownWriterLeaseId: handle.leaseId,
    requireSoleWriter: options.requireSoleWriter === true,
    key: options.key || null,
    kind: options.kind || null
  })) {
    try { writerLeases.release(handle); }
    catch (releaseError) {
      return { ok: false, error: 'writer-lease-release-failed', detail: bounded(releaseError && releaseError.message || releaseError) };
    }
    return { ok: false, error: 'finalization-active', detail: 'durable task creation/finalization recovery owns shared publication state' };
  }
  return { ok: true, handle: handle };
}
function createWriterSessionId() { return writerLeases.createSessionId(); }
function attachMutationChild(handle, pid) {
  try { writerLeases.updateChildPid(handle, pid); return { ok: true }; }
  catch (e) { return { ok: false, error: 'writer-lease-update-failed', detail: bounded(e && e.message || e) }; }
}
function retainMutation(handle, reason) {
  try { writerLeases.markUnverified(handle, reason); return true; }
  catch (e) {
    console.error('[site] could not persist unverified writer-tree state: ' + bounded(e && e.message || e));
    return false;
  }
}
function endMutation(handle) {
  try { return writerLeases.release(handle); }
  catch (e) {
    // Leaving the lease behind is intentionally fail-closed. A finalizer will
    // report the unsafe/active lease instead of overlapping an unproven writer.
    console.error('[site] writer lease release failed: ' + bounded(e && e.message || e));
    return false;
  }
}
function appendOutput(record, chunk) {
  record.output += String(chunk || '');
  if (record.output.length > MAX_OUTPUT) record.output = record.output.slice(-MAX_OUTPUT);
}
function appendRecoveryStderr(record, chunk) {
  if (!record.windowsJobMode) { appendOutput(record, chunk); return; }
  record.jobControlBuffer += String(chunk || '');
  var newline;
  while ((newline = record.jobControlBuffer.indexOf('\n')) >= 0) {
    var line = record.jobControlBuffer.slice(0, newline).replace(/\r$/, '');
    record.jobControlBuffer = record.jobControlBuffer.slice(newline + 1);
    var parts = line.split(' ');
    if (parts[0] === 'WINDOWS_JOB_READY' && parts[1] === record.jobNonce && /^\d+$/.test(parts[2] || '')) {
      record.windowsJobReady = true;
      record.finalizerPid = Number(parts[2]);
    } else if (parts[0] === 'WINDOWS_JOB_DRAINED' && parts[1] === record.jobNonce) {
      record.windowsJobDrained = true;
    } else if (parts[0] === 'WINDOWS_JOB_UNVERIFIED' && parts[1] === record.jobNonce) {
      record.windowsJobUnverified = true;
    } else if (line) {
      appendOutput(record, line + '\n');
    }
  }
}
function failureMessage(record, code, signal) {
  var output = String(record.output || '').trim().split(/\r?\n/).filter(Boolean).slice(-3).join(' | ');
  if (record.timedOut) return 'task finalization exceeded ' + record.timeoutMs + 'ms and its process tree was terminated' + (output ? ': ' + bounded(output, 500) : '');
  return 'task finalization process exited with ' + (code == null ? 'no exit code' : code) + (signal ? ' (' + signal + ')' : '') + (output ? ': ' + bounded(output, 500) : '');
}
function killTree(child, signal, done) {
  done = typeof done === 'function' ? done : function () {};
  if (!child || !child.pid) { done(false); return; }
  try {
    if (process.platform !== 'win32') {
      process.kill(-child.pid, signal);
      done(true);
    } else {
      var killer = childProcess.spawn('taskkill', ['/pid', String(child.pid), '/t'].concat(signal === 'SIGKILL' ? ['/f'] : []), { stdio: 'ignore', windowsHide: true });
      var settled = false;
      function finish(ok) { if (!settled) { settled = true; done(ok); } }
      killer.on('error', function () { finish(false); });
      killer.on('close', function (code) { finish(code === 0); });
    }
  } catch (e) {
    try { child.kill(signal); done(true); } catch (ignored) { done(false); }
  }
}
function processGroupGone(record) {
  if (!record || !record.child || !record.child.pid) return true;
  if (record.windowsJobMode) return record.windowsJobReady === true && record.windowsJobDrained === true;
  try {
    process.kill(-record.child.pid, 0);
    return false;
  } catch (e) {
    return !!(e && e.code === 'ESRCH');
  }
}
function releaseRecord(stem, record) {
  if (children[stem] !== record) return;
  clearTimeout(record.timeoutTimer);
  clearTimeout(record.killTimer);
  clearTimeout(record.verifyTimer);
  delete children[stem];
}
function markTreeUnverified(stem, record) {
  if (children[stem] !== record) return;
  record.treeUnverified = true;
  record.verifyTimer = null;
  lastRuns[stem] = {
    code: 'FINALIZATION_TREE_UNVERIFIED',
    message: 'the finalization parent exited, but the server could not prove its process group is dead; recovery remains blocked to prevent overlapping writers',
    at: new Date().toISOString(),
    startedAt: record.startedAt
  };
}
// Marker-independent reaper for stranded in-memory ownership records. The
// termination-verification chain (verifyTerminatedTree → markTreeUnverified)
// STOPS polling once its deadline passes: a record whose process group was not
// yet provably dead at that instant is left tree-unverified with no timer to
// re-check it. readOne() also re-probes (it calls this reaper), but a
// finalization that already published/cleaned its marker (task moved to done)
// can strand its record forever, and mutationBlocked's children guard then
// blocks EVERY workspace mutation (task creation, task runs, Figma/registry
// writes) with no restart-free recovery.
//
// A record is a reap candidate ONLY once it has reached termination — its
// parent 'close' fired, so endedAt is stamped. That deliberately excludes two
// windows where processGroupGone() can transiently observe ESRCH for a
// still-live finalizer: the spawn-time gap before the detached child calls
// setsid(), and the gap between real group death and Node delivering 'close'.
// Given a terminated record, release uses the SAME death proof as every other
// release site (processGroupGone → ESRCH on the detached group, or the
// authenticated Windows Job drain): only a provably-dead group is released; a
// live or PID/PGID-reused group stays fail-closed. Never releases by age.
// Idempotent: releaseRecord() guards children[stem] === record, and iterating a
// key snapshot makes the in-loop deletes safe. Returns the released stems.
function reapProvablyDeadFinalizations() {
  var released = [];
  // Snapshot the keys so in-loop releaseRecord() deletes never disturb iteration.
  // Each record is guarded independently: this runs on the mutationBlocked hot
  // path and from a bare interval, so one malformed record must never throw into
  // a caller or wedge the sweep.
  Object.keys(children).forEach(function (stem) {
    try {
      var record = children[stem];
      // Terminal (endedAt stamped by 'close') AND provably-dead only; anything
      // else stays fail-closed.
      if (!record || !record.endedAt || !processGroupGone(record)) return;
      // Refresh the diagnostic to the terminal outcome exactly as readOne()'s
      // lazy re-probe did, with the same code precedence as the close handler,
      // so a released record never keeps markTreeUnverified's now-false
      // "recovery remains blocked" message.
      lastRuns[stem] = {
        code: record.timedOut ? 'FINALIZATION_TIMEOUT'
          : (record.spawnError ? 'FINALIZATION_SPAWN_FAILED' : 'FINALIZATION_PROCESS_FAILED'),
        message: failureMessage(record, record.exitCode, record.signal),
        at: record.endedAt || new Date().toISOString(),
        startedAt: record.startedAt
      };
      releaseRecord(stem, record);
      released.push(stem);
    } catch (error) {
      console.error('[finalizations] reaper skipped a record for ' + stem + ': ' + bounded(error && error.message || error));
    }
  });
  if (released.length) {
    console.warn('[finalizations] released ' + released.length +
      ' provably-dead finalization record(s) (process group gone; no restart needed): ' + released.join(', '));
  }
  return released;
}
function verifyTerminatedTree(stem, record) {
  if (children[stem] !== record) return;
  if (processGroupGone(record)) { releaseRecord(stem, record); return; }
  if (Date.now() >= record.terminationVerifyDeadline) { markTreeUnverified(stem, record); return; }
  record.verifyTimer = setTimeout(function () { verifyTerminatedTree(stem, record); }, TERMINATION_POLL_MS);
  if (typeof record.verifyTimer.unref === 'function') record.verifyTimer.unref();
}
function beginTerminationVerification(stem, record) {
  if (children[stem] !== record || record.verificationStarted) return;
  record.killTimer = null;
  record.verificationStarted = true;
  record.terminationVerifyDeadline = Date.now() + TERMINATION_VERIFY_MS;
  if (record.windowsJobMode) {
    // taskkill is only an escalation for a wedged wrapper. Its success is NOT a
    // descendant-death proof; only the nonce-authenticated DRAINED record from
    // the Job Object wrapper can release ownership.
    if (processGroupGone(record)) { releaseRecord(stem, record); return; }
    killTree(record.child, 'SIGKILL', function () { markTreeUnverified(stem, record); });
    return;
  }
  killTree(record.child, 'SIGKILL');
  verifyTerminatedTree(stem, record);
}
function handleWindowsControlError(stem, record, error) {
  if (!record) return;
  record.stdinControlError = error || new Error('Windows Job control stdin failed');
  record.windowsJobUnverified = true;
  appendOutput(record, 'Windows Job control channel failed: ' + bounded(record.stdinControlError.message || record.stdinControlError) + '\n');
  if (children[stem] !== record) return;
  // Losing the wrapper control pipe is never a drain proof. Keep the record
  // authoritative and ensure the existing Job/tree verification escalation
  // still runs; only nonce-authenticated DRAINED may release it.
  record.terminationRequested = true;
  if (!record.killTimer) {
    record.killTimer = setTimeout(function () { beginTerminationVerification(stem, record); }, TERMINATION_GRACE_MS);
    if (typeof record.killTimer.unref === 'function') record.killTimer.unref();
  }
}
function terminateRecord(stem, record) {
  if (!record || !record.child) return;
  if (record.treeUnverified) {
    record.treeUnverified = false;
    record.verificationStarted = false;
  }
  record.terminationRequested = true;
  if (record.windowsJobMode) {
    // Ask the wrapper to terminate its Job and wait until ActiveProcesses == 0.
    // Killing the wrapper directly would lose the authenticated drain proof.
    try {
      if (record.child.stdin && record.child.stdin.writable) record.child.stdin.end('TERMINATE\n');
    } catch (e) { handleWindowsControlError(stem, record, e); }
    if (!record.killTimer) {
      record.killTimer = setTimeout(function () { beginTerminationVerification(stem, record); }, TERMINATION_GRACE_MS);
      if (typeof record.killTimer.unref === 'function') record.killTimer.unref();
    }
    return;
  }
  killTree(record.child, 'SIGTERM');
  if (!record.killTimer) {
    record.killTimer = setTimeout(function () { beginTerminationVerification(stem, record); }, TERMINATION_GRACE_MS);
    if (typeof record.killTimer.unref === 'function') record.killTimer.unref();
  }
}
function resume(stem, expectedRevision, expectedEtag) {
  if (!validStem(stem)) return { ok: false, statusCode: 400, error: 'bad-stem' };
  if (!Number.isInteger(expectedRevision) || expectedRevision < 1 || !ETAG_RE.test(String(expectedEtag || ''))) {
    return { ok: false, statusCode: 400, error: 'bad-concurrency-token' };
  }
  var current = readOne(stem);
  if (current.status === 'corrupt') return { ok: false, statusCode: 409, error: 'finalization-corrupt', detail: current.errorMessage };
  if (current.revision !== expectedRevision || current.etag !== expectedEtag) {
    return { ok: false, statusCode: 409, error: 'finalization-changed', current: current };
  }
  if (children[stem] || current.recoveryRunning) return { ok: true, accepted: false, alreadyRunning: true, finalization: current };
  var activeStems = Object.keys(children);
  if (activeStems.length) {
    return { ok: false, statusCode: 409, error: 'finalization-busy', detail: 'Another task finalization is already running: ' + activeStems[0] };
  }
  if (!current.recoverable) return { ok: false, statusCode: 409, error: 'finalization-unavailable', detail: current.errorMessage, current: current };

  var script = process.env.FINALIZE_TASK_SCRIPT || path.join(paths.ORCHESTRATOR_DIR, 'tasks', 'finalize-task.mjs');
  var child, useJob = windowsJobMode();
  var jobNonce = useJob ? crypto.randomBytes(24).toString('hex') : null;
  var childEnv = Object.assign({}, process.env, {
    FINALIZE_PROJECT_ROOT: paths.PROJECT_ROOT,
    FINALIZE_WRITER_AUTHORITY_ROOT: paths.WRITER_AUTHORITY_ROOT,
    FINALIZE_LOCKS_DIR: paths.LOCKS_DIR,
    FINALIZE_STATE_DIR: DIR,
    FINALIZE_CREATIONS_DIR: paths.TASK_CREATIONS_DIR,
    FINALIZE_EDITS_DIR: paths.TASK_EDITS_DIR
  });
  // A site-started recovery is not the owner Claude turn. Never inherit a
  // session credential from whatever shell launched the site, or it could
  // bypass an unrelated active writer lease in finalize-task.
  delete childEnv.ORCHESTRATOR_WRITER_SESSION_ID;
  delete childEnv.ORCHESTRATOR_WRITER_STEM;
  delete childEnv.ORCHESTRATOR_WRITER_LEASE_ID;
  delete childEnv.ORCHESTRATOR_WRITER_DELEGATION_TOKEN;
  // Same rule, one owner: this env is assembled here rather than through
  // child-env's builder, so the test-injection predicate is applied explicitly.
  Object.keys(childEnv).forEach(function (key) {
    if (isTestInjectionKey(key)) delete childEnv[key];
  });
  var command = process.execPath;
  var commandArgs = [script, stem, '--json'];
  if (useJob) {
    command = process.env.FINALIZATION_WINDOWS_JOB_PYTHON || process.env.FINALIZE_LOCK_PYTHON || 'python';
    commandArgs = [process.env.FINALIZATION_WINDOWS_JOB_WRAPPER || path.join(paths.ORCHESTRATOR_DIR, 'tasks', 'windows-job.py'),
      '--', process.execPath, script, stem, '--json'];
    childEnv.FINALIZATION_JOB_NONCE = jobNonce;
  }
  try {
    child = childProcess.spawn(command, commandArgs, {
      cwd: paths.PROJECT_ROOT,
      env: childEnv,
      shell: false,
      // A separate process group lets shutdown terminate spawnSync descendants
      // (ship/Figma/Python), not only their finalizer parent.
      detached: !useJob,
      stdio: [useJob ? 'pipe' : 'ignore', 'pipe', 'pipe'],
      windowsHide: true
    });
  } catch (e) {
    return { ok: false, statusCode: 500, error: 'finalization-spawn-failed', detail: bounded(e.message) };
  }
  delete lastRuns[stem];
  var record = {
    child: child, output: '', startedAt: new Date().toISOString(), timeoutMs: timeoutMs(), timedOut: false,
    windowsJobMode: useJob, jobNonce: jobNonce, jobControlBuffer: '', windowsJobReady: false,
    windowsJobDrained: false, windowsJobUnverified: false, stdinControlError: null
  };
  children[stem] = record;
  if (useJob && child.stdin) {
    // child.stdin emits asynchronous EPIPE separately from ChildProcess. Keep
    // this listener for the whole wrapper lifetime so timeout/shutdown cannot
    // crash the site while writing TERMINATE.
    child.stdin.on('error', function (error) { handleWindowsControlError(stem, record, error); });
  }
  child.stdout.on('data', function (chunk) { appendOutput(record, chunk); });
  child.stderr.on('data', function (chunk) { appendRecoveryStderr(record, chunk); });
  child.on('error', function (err) {
    record.spawnError = err;
    appendOutput(record, err && err.message || err);
    lastRuns[stem] = { code: 'FINALIZATION_SPAWN_FAILED', message: bounded(err && err.message || err), at: new Date().toISOString(), startedAt: record.startedAt };
  });
  record.timeoutTimer = setTimeout(function () {
    if (children[stem] !== record) return;
    record.timedOut = true;
    lastRuns[stem] = { code: 'FINALIZATION_TIMEOUT', message: failureMessage(record, null, null), at: new Date().toISOString(), startedAt: record.startedAt };
    terminateRecord(stem, record);
  }, record.timeoutMs);
  if (typeof record.timeoutTimer.unref === 'function') record.timeoutTimer.unref();
  child.on('close', function (code, signal) {
    clearTimeout(record.timeoutTimer);
    if (record.windowsJobMode && record.jobControlBuffer) {
      appendOutput(record, record.jobControlBuffer);
      record.jobControlBuffer = '';
    }
    record.exitCode = code;
    record.signal = signal || null;
    record.endedAt = new Date().toISOString();
    if (record.timedOut) {
      lastRuns[stem] = { code: 'FINALIZATION_TIMEOUT', message: failureMessage(record, code, signal), at: record.endedAt, startedAt: record.startedAt };
    } else if (record.spawnError) {
      lastRuns[stem] = { code: 'FINALIZATION_SPAWN_FAILED', message: bounded(record.spawnError.message || record.spawnError), at: record.endedAt, startedAt: record.startedAt };
    } else if (code !== 0) {
      lastRuns[stem] = { code: 'FINALIZATION_PROCESS_FAILED', message: failureMessage(record, code, signal), at: record.endedAt, startedAt: record.startedAt };
    } else {
      delete lastRuns[stem];
      // Post-finalization comparison enqueue (REQ-CONC-006): the committed
      // mutation (mapping revision / project sources) is already durably
      // stale-marking; the recompare only shortens the window and runs after
      // the finalizer released its writer authority. Best-effort by design.
      setTimeout(function () {
        try { require('./design-token-compare').ensureFresh('finalization'); } catch (error) {}
      }, 250).unref();
    }
    // A Windows wrapper close is never itself a tree-death proof. Release only
    // after the nonce-authenticated wrapper reported Job ActiveProcesses == 0.
    // A spawn error with no PID is the one safe exception: no wrapper/child was
    // created at all.
    if (record.windowsJobMode) {
      if (record.spawnError && !record.child.pid) releaseRecord(stem, record);
      else if (processGroupGone(record)) releaseRecord(stem, record);
      else { clearTimeout(record.killTimer); markTreeUnverified(stem, record); }
      return;
    }
    // A timed-out/shutdown parent can exit on SIGTERM while one of its
    // descendants ignores the signal. Do not make recovery available merely
    // because the parent closed: retain the in-memory ownership record until
    // the process group is provably gone (or stay fail-closed if it cannot be
    // proved). On POSIX, even an unsolicited exit is checked for a surviving
    // detached-group member before its ownership record is released.
    if (record.terminationRequested) {
      if (processGroupGone(record)) releaseRecord(stem, record);
      // Otherwise the grace timer escalates to SIGKILL and verifies the group.
    } else if (!processGroupGone(record)) {
      // Even an unsolicited crash/nonzero exit can strand an async descendant.
      // The detached group is still observable after the leader closes, so
      // force/verify it before admitting another writer.
      record.terminationRequested = true;
      beginTerminationVerification(stem, record);
    } else {
      releaseRecord(stem, record);
    }
  });
  return { ok: true, accepted: true, alreadyRunning: false, finalization: Object.assign({}, current, { recoveryRunning: true, recoverable: false }) };
}
function dirMtime() {
  try {
    var chain = directoryChain({ allowMissing: true });
    return chain.exists ? chain.stat.mtimeMs : 0;
  } catch (e) { return 0; }
}
function init() {
  try { ensureDir(); }
  catch (e) { console.error('[site] finalization marker directory is unavailable: ' + bounded(e && e.message || e)); }
  var pending = list();
  if (pending.length) console.warn('[site] found ' + pending.length + ' unfinished/corrupt task finalization(s); recovery is available on the Board');
}
function killAll() {
  Object.keys(children).forEach(function (stem) {
    var record = children[stem];
    if (!record) return;
    clearTimeout(record.timeoutTimer);
    terminateRecord(stem, record);
  });
}

function finalizationIntegrityFinding(code, stem, file, message) {
  return { code: code, severity: 'error', stem: stem || null, paths: file ? [file] : [], message: message,
    recovery: 'Resume or reconcile the exact finalization/writer generation through its owner; do not clear publication state by age.' };
}
function scanIntegrity(scope) {
  var stem = typeof scope === 'string' ? scope : scope && scope.stem || null;
  var out = { version: 1, owner: 'finalizations', statuses: [], findings: [], snapshotInputs: [], truncated: false };
  var directory;
  try { directory = directoryChain({ allowMissing: true }); }
  catch (error) {
    out.findings.push(finalizationIntegrityFinding('FINALIZATION_DIRECTORY_UNSAFE', stem, DIR, 'Finalization runtime directory cannot be inspected safely.'));
    return out;
  }
  if (directory.exists) {
    var names, listed = fileGuards.boundedDirectoryNamesUnder(paths.WRITER_AUTHORITY_ROOT, DIR, MAX_RUNTIME_ENTRIES);
    if (!listed.ok) {
      out.findings.push(finalizationIntegrityFinding(listed.code === 'directory-entry-limit' ? 'FINALIZATION_SCAN_LIMIT' : 'FINALIZATION_DIRECTORY_UNSAFE', null, DIR,
        listed.code === 'directory-entry-limit' ? 'Finalization runtime directory exceeds its bounded scan limit.' : 'Finalization runtime directory cannot be enumerated safely.'));
      out.truncated = listed.code === 'directory-entry-limit'; names = [];
    } else {
      var replacementNames = listed.names.filter(function (name) {
        var rowStem = replacementArtifactStem(name);
        return rowStem && (!stem || rowStem === stem);
      }).sort();
      var malformedReplacementNames = listed.names.filter(function (name) {
        if (name.indexOf('.replace-') < 0 || replacementArtifactStem(name)) return false;
        return !stem || name.indexOf('.' + stem + '.json.replace-') === 0;
      }).sort();
      replacementNames.forEach(function (name) {
        var rowStem = replacementArtifactStem(name), file = path.join(DIR, name);
        var maximum = name.endsWith('.replace-wal.json') || name.endsWith('.replace-reservation.json') ? MAX_REPLACE_WAL : MAX_MARKER;
        var replacementRead = fileGuards.boundedRegularFileUnder(paths.WRITER_AUTHORITY_ROOT, DIR, file, maximum);
        if (replacementRead) out.snapshotInputs.push({ owner: 'finalizations', kind: 'marker-replace', path: file, hash: digest(replacementRead.bytes), size: replacementRead.bytes.length });
        out.findings.push(finalizationIntegrityFinding('FINALIZATION_MARKER_RECOVERY_REQUIRED', rowStem, file,
          'A durable finalization-marker replacement artifact requires exact reconciliation.'));
      });
      malformedReplacementNames.forEach(function (name) {
        out.findings.push(finalizationIntegrityFinding('FINALIZATION_MARKER_RECOVERY_UNSAFE', stem, path.join(DIR, name),
          'A malformed private marker-replacement artifact blocks ownership inference.'));
      });
      names = stem ? [stem + '.json'] : listed.names.filter(function (name) {
        return name.endsWith('.json') && name !== '.mutex.json' && !replacementArtifactStem(name);
      }).sort();
    }
    names.forEach(function (name) {
      var rowStem = name.slice(0, -5), file = path.join(DIR, name);
      if (!validStem(rowStem)) { out.findings.push(finalizationIntegrityFinding('FINALIZATION_MARKER_NAME_INVALID', null, file, 'Finalization marker filename is invalid.')); return; }
      var entry = fileGuards.inspectEntryUnder(paths.WRITER_AUTHORITY_ROOT, DIR, file);
      if (stem && entry.status === 'missing') return;
      var read = fileGuards.boundedRegularFileUnder(paths.WRITER_AUTHORITY_ROOT, DIR, file, MAX_MARKER);
      if (!read) { out.findings.push(finalizationIntegrityFinding('FINALIZATION_MARKER_UNSAFE', rowStem, file, 'Finalization marker is unsafe, unstable, or oversized.')); return; }
      var hash = digest(read.bytes), marker;
      out.snapshotInputs.push({ owner: 'finalizations', kind: 'marker', path: file, hash: hash, size: read.bytes.length });
      try { marker = JSON.parse(read.bytes.toString('utf8')); } catch (error) { marker = null; }
      var invalid = markerShapeError(marker, rowStem);
      if (invalid) { out.findings.push(finalizationIntegrityFinding('FINALIZATION_MARKER_INVALID', rowStem, file, invalid)); return; }
      // The finalizer legitimately publishes a live owner while the marker is
      // still `incomplete` (recovery claim -> beginPhase) and while it is
      // already `completed` (durable commit -> proof cleanup -> marker
      // removal). Owner generation, not the transient status spelling, is the
      // authority for whether recovery may start.
      var active = ownerActive(marker.owner);
      out.statuses.push({ owner: 'finalizations', kind: 'marker', stem: rowStem,
        state: active ? 'running' : 'recovery-required', phase: marker.phase, revision: marker.revision,
        createdAt: marker.createdAt, updatedAt: marker.updatedAt, contentHash: hash,
        lockGenerationHash: taskStateCore.lockGenerationHash(marker.source.lock) });
      if (!active) out.findings.push(finalizationIntegrityFinding('FINALIZATION_RECOVERY_REQUIRED', rowStem, file,
        marker.status === 'completed' ? 'A completed finalization marker remains and requires cleanup/reconciliation.' : 'An unfinished finalization marker has no provably active owner.'));
    });

    var mutexFile = path.join(DIR, '.mutex.json');
    var mutexEntry = fileGuards.inspectEntryUnder(paths.WRITER_AUTHORITY_ROOT, DIR, mutexFile);
    if (mutexEntry.status === 'present') {
      var mutexRead = fileGuards.boundedRegularFileUnder(paths.WRITER_AUTHORITY_ROOT, DIR, mutexFile, 4096);
      if (!mutexRead) out.findings.push(finalizationIntegrityFinding('FINALIZATION_MUTEX_INVALID', null, mutexFile, 'Global finalization mutex is unsafe, unstable, or oversized.'));
      else {
        var mutexHash = digest(mutexRead.bytes), mutex;
        out.snapshotInputs.push({ owner: 'finalizations', kind: 'mutex', path: mutexFile, hash: mutexHash, size: mutexRead.bytes.length });
        try { mutex = JSON.parse(mutexRead.bytes.toString('utf8')); } catch (error) { mutex = null; }
        var mutexInvalid = mutexRecordIssue(mutex);
        if (mutexInvalid) out.findings.push(finalizationIntegrityFinding('FINALIZATION_MUTEX_INVALID', null, mutexFile, mutexInvalid));
        else if (!mutex.released) {
          var mutexActive = processOwnerActive(mutex);
          out.statuses.push({ owner: 'finalizations', kind: 'mutex', stem: null, state: mutexActive ? 'active' : 'recovery-required',
            createdAt: mutex.startedAt, updatedAt: mutex.startedAt, contentHash: mutexHash });
          if (!mutexActive) out.findings.push(finalizationIntegrityFinding('FINALIZATION_MUTEX_RECOVERY_REQUIRED', null, mutexFile, 'An unreleased local mutex record has no live owner.'));
        }
      }
    } else if (mutexEntry.status !== 'missing') {
      out.findings.push(finalizationIntegrityFinding('FINALIZATION_MUTEX_INVALID', null, mutexFile, 'Global finalization mutex cannot be inspected safely.'));
    }
  }

  var leaseScan;
  try { leaseScan = scope && scope.writerLeaseInspection || writerLeaseInspector.inspect(); }
  catch (error) { leaseScan = { active: [], stale: [], issues: [{ code: 'WRITER_LEASE_DIR_UNSAFE', message: String(error && error.message || error) }] }; }
  out.snapshotInputs.push.apply(out.snapshotInputs, leaseScan.snapshotInputs || []);
  out.truncated = out.truncated || leaseScan.truncated === true;
  (leaseScan.issues || []).forEach(function (row) {
    out.findings.push(finalizationIntegrityFinding(row.code || 'WRITER_LEASE_INVALID', null, paths.WRITER_LEASES_DIR, bounded(row.message || 'Writer lease runtime is invalid.')));
  });
  function leaseStatus(row, state) {
    if (stem && row.stem !== stem && row.stem !== null) return;
    var safe = { leaseId: row.leaseId, kind: row.kind, stem: row.stem, sessionId: row.sessionId, key: row.key,
      createdAt: row.createdAt, updatedAt: row.updatedAt, expiresAt: row.expiresAt, unverified: row.unverified };
    var hash = digest(Buffer.from(JSON.stringify(safe), 'utf8'));
    out.statuses.push({ owner: 'finalizations', kind: 'writer-lease', stem: row.stem || null, state: state,
      leaseId: row.leaseId, writerKind: row.kind, createdAt: row.createdAt, updatedAt: row.updatedAt, contentHash: hash });
    if (state === 'recovery-required') out.findings.push(finalizationIntegrityFinding('WRITER_LEASE_RECOVERY_REQUIRED', row.stem || null,
      path.join(paths.WRITER_LEASES_DIR, row.leaseId + '.json'), 'A stale writer lease requires exact ownership reconciliation.'));
  }
  (leaseScan.active || []).forEach(function (row) { leaseStatus(row, 'active'); });
  (leaseScan.stale || []).forEach(function (row) { leaseStatus(row, 'recovery-required'); });
  return out;
}

module.exports = {
  STEM_RE: STEM_RE,
  init: init,
  list: list,
  readOne: readOne,
  publicErrorCode: publicErrorCode,
  publicProjection: publicProjection,
  hasMarker: hasMarker,
  mutationBlocked: mutationBlocked,
  foreignTaskSessionWriterIssue: foreignTaskSessionWriterIssue,
  ownTaskSessionWriterActive: ownTaskSessionWriterActive,
  reap: reapProvablyDeadFinalizations,
  createWriterSessionId: createWriterSessionId,
  beginMutation: beginMutation,
  attachMutationChild: attachMutationChild,
  retainMutation: retainMutation,
  endMutation: endMutation,
  resume: resume,
  killAll: killAll,
  dirMtime: dirMtime,
  mutexRecordIssue: mutexRecordIssue,
  scanIntegrity: scanIntegrity
};
