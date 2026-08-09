'use strict';

// ---------------------------------------------------------------------------
// In-progress locks. The canonical task-lock helper publishes a JSON sidecar
// under orchestrator/.cache/tasks/locks/<STEM>.json at the start of a guarded
// run; the UI reads `progress.inProgress` to paint "в работе" badges on
// cards keyed by stem. Lock lifecycle:
//
//   - task-prep: writes on every invocation, removes on every exit
//     (one-shot per backlog/pending file). See the task-prep skill
//     Step 0 / Step 7.5.
//
//   - orchestrator: acquires at Step 0.5; the finalizer removes it only after
//     the done/index postconditions pass. Intermediate BLOCKED
//     returns keep the lock — the orchestrator session stays alive
//     waiting for clarification, so the badge correctly reads "still in
//     the pipeline". See the task-orchestrator skill (run-loop, Lock discipline).
//
// An old timestamp is presentation-only and never proves abandonment. Normal
// release requires the exact canonical task-lock receipt; browser recovery is
// delegated to the canonical helper's two-phase owner-status/recover-owner
// protocol and is not a repair of durable task state.
//
// Publication is no-clobber and release requires the exact immutable receipt;
// this module never performs read-modify-delete itself.
// ---------------------------------------------------------------------------

var fs     = require('fs');
var path   = require('path');
var cp     = require('child_process');
var paths  = require('./paths');
var fileGuards = require('./file-guards');
var childEnv = require('./child-env').childEnv;
var taskCore = require('../../tasks/task-state-core.cjs');

var LOCKS_DIR   = paths.LOCKS_DIR;
var RUNS_DIR    = paths.RUNS_DIR;     // CLI-runner session transcript + sidecar (server-spawned runs)
var JOURNAL_DIR = paths.JOURNAL_DIR;  // per-task pipeline phase log (helper-written; both run types)
var MAX_RUNTIME_ENTRIES = 10000;
var TASK_LOCK_HELPER = path.join(paths.ORCHESTRATOR_DIR, 'tasks', 'task-lock.mjs');
var HASH_RE = /^sha256:[a-f0-9]{64}$/;
var HELPER_TIMEOUT_MS = 30000;
var HELPER_MAX_BUFFER = 256 * 1024;

// Stage allow-list for lock files. Any other value collapses to 'unknown'
// on read so a garbage write can't crash the UI label dispatcher.
var LOCK_STAGES = new Set(['task-prep', 'orchestrator']);

// Stem allow-list: TASK_<N>_<snake_case>. Same shape the regen scripts
// produce. Used to reject random files dropped under .cache/tasks/locks/.
var LOCK_STEM_RE = taskCore.STEM_RE;
var LOCK_RELEASE_RE = /^\.(TASK_\d+_[A-Za-z0-9_]+)\.json\.release-[a-f0-9]{36}$/;

function validTaskStem(stem) {
  return taskCore.safeIntegerId(stem) !== null;
}

// Newest "real activity" instant for a stem, as an ISO string (or null when no
// activity file exists yet). Status and task-summary projections expose the
// last activity instead of only the lock's start, so a long-but-live run keeps
// a current activity timestamp. Two sources, whichever is newer:
//   - .cache/tasks/journal/<STEM>.jsonl — the pipeline phase log the helper appends at every
//     phase, so it advances for BOTH server-spawned and external /loop runs
//     (stem-keyed; see orchestrator/tasks/log-event.py). Inert until the helper
//     emit is wired, at which point this lights up with no change here.
//   - .cache/tasks/runs/<safeKey>.{events.jsonl,session.json} — the CLI-runner's session
//     transcript + status sidecar (server-spawned runs; survives a server
//     restart, when the in-memory session is gone but the lock persists). safeKey
//     mirrors sessions.js: 'task:<stem>' with each non-[A-Za-z0-9_.-] run → '_'.
function mtimeMs(p) {
  // isFile() so a directory accidentally created at the path (or a placeholder
  // dir) can't masquerade as a fresh activity file; any error / absence → 0.
  try { var st = fs.lstatSync(p); return st.isFile() && !st.isSymbolicLink() ? st.mtimeMs : 0; }
  catch (e) { return 0; }
}
// Round activity to 30 s. The freshness thresholds are measured in minutes, so
// second-precision buys nothing — and the lock object rides progress.inProgress into the SSE
// state-hash (sse.js stateHash), so a streaming run whose .cache/tasks/runs/* mtime advances
// every poll would otherwise fire a 'change' SSE every ~1.5 s for the whole run.
// Bucketing pins it to the board's 30 s refresh cadence — the same churn-control
// rationale as sse.js's heartbeatBucket.
var ACTIVITY_BUCKET_MS = 30000;
function lastActivityIso(stem) {
  var safe = ('task:' + stem).replace(/[^A-Za-z0-9_.-]+/g, '_');
  var ms = Math.max(
    mtimeMs(path.join(JOURNAL_DIR, stem + '.jsonl')),
    mtimeMs(path.join(RUNS_DIR, safe + '.events.jsonl')),
    mtimeMs(path.join(RUNS_DIR, safe + '.session.json'))
  );
  if (!(ms > 0) || !isFinite(ms)) return null;   // no activity file yet, or NaN slipped through
  // Cap a future-dated mtime (NFS / clock skew) at 'now' BEFORE bucketing, so every
  // consumer sees the same capped value: the board card (lock.lastActivityAt) AND the
  // header (status.locks.oldestActivityAt, derived from it). Without this, a future
  // "activity" stamp would suppress the stale signal on the header side only.
  ms = Math.min(ms, Date.now());
  ms = Math.floor(ms / ACTIVITY_BUCKET_MS) * ACTIVITY_BUCKET_MS;
  return new Date(ms).toISOString();
}

function projectedLock(value, stem) {
  if (taskCore.canonicalLockV1(value, stem)) {
    return { stage: value.stage, startedAt: value.startedAt };
  }
  // Presence remains visible even when ownership bytes are corrupt. Mutation
  // admission separately blocks on the filesystem generation; the board must
  // not make malformed bytes look absent or borrow trusted fields from them.
  return { stage: 'unknown', startedAt: null };
}

function boundedLockNames() {
  var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, LOCKS_DIR, MAX_RUNTIME_ENTRIES);
  if (!listed.ok) {
    var error = new Error(listed.code === 'directory-entry-limit'
      ? 'runtime-locks-entry-limit'
      : 'runtime-locks-unavailable');
    error.code = error.message;
    throw error;
  }
  return listed.names.slice().sort();
}

function readLocksResult() {
  var entries;
  try {
    entries = boundedLockNames();
  } catch (e) {
    return {
      available: false,
      errorCode: e && e.code === 'runtime-locks-entry-limit'
        ? 'runtime-locks-entry-limit'
        : 'runtime-locks-unavailable',
      rows: []
    };
  }
  var out = [];
  for (var i = 0; i < entries.length; i++) {
    var name = entries[i];
    if (!name.endsWith('.json')) continue;
    var stem = name.substring(0, name.length - '.json'.length);
    if (!validTaskStem(stem)) continue;
    var p = path.join(LOCKS_DIR, name);
    var read = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, LOCKS_DIR, p, 32 * 1024);
    if (!read) {
      // The bounded directory scan proved that a canonical lock name exists.
      // Unsafe, changing, oversized or unreadable bytes must therefore remain
      // visible as an unknown lock; they may never collapse to "not locked".
      out.push({ stem: stem, stage: 'unknown', startedAt: null, lastActivityAt: lastActivityIso(stem) });
      continue;
    }
    var parsed;
    try { parsed = JSON.parse(read.bytes.toString('utf8')); }
    catch (e) {
      out.push({ stem: stem, stage: 'unknown', startedAt: null, lastActivityAt: lastActivityIso(stem) });
      continue;
    }
    var projected = projectedLock(parsed, stem);
    out.push({ stem: stem, stage: projected.stage, startedAt: projected.startedAt, lastActivityAt: lastActivityIso(stem) });
  }
  // Stable order so the SSE diff hash doesn't flap on readdir reordering.
  out.sort(function (a, b) { return a.stem < b.stem ? -1 : a.stem > b.stem ? 1 : 0; });
  return { available: true, errorCode: null, rows: out };
}

function locksDirMtime() {
  // Folder mtime bumps on any add/remove. Files inside also bump it on
  // atomic-rename writes (mv replaces the dirent). Watching this single
  // number in stateHash() is enough to drive SSE 'change'.
  try {
    var chain = fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, LOCKS_DIR, { allowMissing: true });
    return chain && chain.exists ? chain.stat.mtimeMs : 0;
  } catch (e) {
    return 0;
  }
}

function lockPathFor(stem) {
  if (!validTaskStem(stem)) return null;
  return path.normalize(path.join(LOCKS_DIR, stem + '.json'));
}

// Presence is an ownership boundary, not a liveness verdict. Any filesystem
// entry at the canonical lock path (including malformed/symlink/special bytes)
// blocks a new task mutation until the exact owner-safe recovery path resolves
// it. Callers must never turn a read error into "unlocked".
function lockPresence(stem) {
  var target = lockPathFor(stem);
  if (!target) return { validStem: false, present: false, readable: false };
  var names;
  try { names = boundedLockNames(); }
  catch (scanError) { return { validStem: true, present: true, readable: false, safe: false, recovery: true }; }
  var retained = names.find(function (name) {
    var match = LOCK_RELEASE_RE.exec(name);
    return match && validTaskStem(match[1]) && match[1] === stem;
  });
  if (retained) return { validStem: true, present: true, readable: true, safe: false, recovery: true };
  var entry = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, LOCKS_DIR, target);
  if (entry.status === 'missing') return { validStem: true, present: false, readable: true, safe: true };
  if (entry.status === 'present') return { validStem: true, present: true, readable: true,
    safe: entry.stat.isFile() && !entry.stat.isSymbolicLink() && entry.stat.nlink === '1' };
  return { validStem: true, present: true, readable: false, safe: false };
}

// Strong task-session continuation check. Presence alone blocks competing
// mutations, but a user answer may enter a live Claude process only when the
// current canonical v1 lock names that exact session generation. Malformed,
// malformed, oversized, symlinked or racing bytes all fail closed.
function lockOwnedBySession(stem, sessionId) {
  var target = lockPathFor(stem);
  if (!target || typeof sessionId !== 'string') return { owned: false, reason: 'invalid-identity' };
  var presence = lockPresence(stem);
  if (presence.recovery) return { owned: false, reason: 'release-recovery-required' };
  var chain = fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, LOCKS_DIR, { allowMissing: true });
  if (!chain || !chain.exists) return { owned: false, reason: chain ? 'missing' : 'unsafe-directory' };
  var read = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, LOCKS_DIR, target, 32 * 1024);
  if (!read) return { owned: false, reason: 'unsafe-lock' };
  var value;
  try { value = JSON.parse(read.bytes.toString('utf8')); }
  catch (error) { return { owned: false, reason: 'malformed-lock' }; }
  if (!taskCore.canonicalLockV1(value, stem)) return { owned: false, reason: 'noncanonical-lock' };
  if (value.sessionId !== sessionId) return { owned: false, reason: 'foreign-session' };
  return { owned: true, stage: value.stage, runId: value.runId, sessionId: value.sessionId };
}

function helperEnvironment(extra) {
  return childEnv(Object.assign({
    ORCHESTRATOR_PROJECT_ROOT: paths.PROJECT_ROOT,
    ORCHESTRATOR_TASKS_DIR: paths.TASKS_DIR,
    ORCHESTRATOR_LOCKS_DIR: paths.LOCKS_DIR,
    ORCHESTRATOR_WRITER_LEASES_DIR: paths.WRITER_LEASES_DIR,
    ORCHESTRATOR_WRITER_AUTHORITY_ROOT: paths.WRITER_AUTHORITY_ROOT
  }, extra || {}));
}

function trailingJson(text) {
  var raw = String(text || '').trim();
  if (!raw) return null;
  var starts = [0];
  var cursor = raw.indexOf('\n{');
  while (cursor >= 0) {
    starts.push(cursor + 1);
    cursor = raw.indexOf('\n{', cursor + 2);
  }
  for (var i = starts.length - 1; i >= 0; i--) {
    try { return JSON.parse(raw.slice(starts[i])); }
    catch (e) { /* try an earlier JSON boundary */ }
  }
  return null;
}

function helperError(error, stderr) {
  var envelope = trailingJson(stderr);
  var out = new Error(String(envelope && envelope.message || error && error.message || 'task-lock helper failed').slice(0, 500));
  out.code = envelope && typeof envelope.code === 'string' ? envelope.code :
    (error && error.killed ? 'LOCK_HELPER_TIMEOUT' : 'LOCK_HELPER_FAILED');
  out.retryable = !!(envelope && envelope.retryable);
  out.details = envelope && envelope.details && typeof envelope.details === 'object' ? envelope.details : null;
  out.httpStatus = out.code === 'LOCK_NOT_FOUND' ? 404 :
    (out.code === 'INVOCATION_INVALID' ? 400 : 409);
  return out;
}

var PUBLIC_RECOVERY_REASON_CODES = new Set([
  'bad-stem',
  'INVOCATION_INVALID',
  'LOCK_NOT_FOUND',
  'LOCK_IDENTITY_MISMATCH',
  'LOCK_HELPER_TIMEOUT',
  'LOCK_HELPER_FAILED',
  'LOCK_HELPER_INVALID',
  'LOCK_RECOVERY_AUTHORITY_INVALID',
  // Phase two refuses for reasons that have nothing to do with owner liveness.
  // Collapsing them all into LOCK_RECOVERY_FAILED made the UI claim the owner
  // could not be proven dead while owner-status was proving exactly that, and
  // left the user re-running the owner check forever.
  'LOCK_OWNER_RECOVERY_REFUSED',
  'LOCK_OWNER_RECOVERY_STATE_INVALID',
  'LOCK_OWNER_RECOVERY_STATE_CHANGED',
  'LOCK_OWNER_RECOVERY_STATE_UNAVAILABLE',
  'LOCK_RELEASE_POSTCONDITION_FAILED',
  'LOCK_RELEASE_POSTCONDITION_CHANGED',
  'LOCK_RELEASE_POSTCONDITION_UNAVAILABLE',
  'LOCK_RELEASE_RECOVERY_REQUIRED',
  'LOCK_CHANGED'
]);
function publicRecoveryReasonCode(value) {
  return PUBLIC_RECOVERY_REASON_CODES.has(value) ? value : 'LOCK_RECOVERY_FAILED';
}

function runHelper(args, extraEnv) {
  return new Promise(function (resolve, reject) {
    cp.execFile(process.execPath, [TASK_LOCK_HELPER].concat(args), {
      cwd: paths.PROJECT_ROOT,
      env: helperEnvironment(extraEnv),
      encoding: 'utf8',
      timeout: HELPER_TIMEOUT_MS,
      maxBuffer: HELPER_MAX_BUFFER,
      windowsHide: true
    }, function (error, stdout, stderr) {
      if (error) { reject(helperError(error, stderr)); return; }
      var value = trailingJson(stdout);
      if (!value || value.version !== 1 || value.ok !== true) {
        var invalid = new Error('task-lock helper returned an invalid success envelope');
        invalid.code = 'LOCK_HELPER_INVALID'; invalid.httpStatus = 500;
        reject(invalid); return;
      }
      resolve(value);
    });
  });
}

function inspectOwnerRecovery(stem) {
  if (!lockPathFor(stem)) {
    var invalid = new Error('task stem is invalid');
    invalid.code = 'bad-stem'; invalid.httpStatus = 400;
    return Promise.reject(invalid);
  }
  return runHelper(['owner-status', '--stem', stem]).then(function (value) {
    if (value.operation !== 'owner-status' || value.stem !== stem ||
        !HASH_RE.test(String(value.lockHash || '')) ||
        typeof value.recoverable !== 'boolean' || typeof value.reason !== 'string' ||
        typeof value.ownerState !== 'string') {
      var malformed = new Error('task-lock owner-status result is malformed');
      malformed.code = 'LOCK_HELPER_INVALID'; malformed.httpStatus = 500;
      throw malformed;
    }
    return {
      version: 1,
      ok: true,
      operation: 'owner-status',
      stem: stem,
      stage: value.stage,
      startedAt: value.startedAt,
      lockHash: value.lockHash,
      ownerState: value.ownerState,
      recoverable: value.recoverable,
      reason: value.reason
    };
  });
}

function recoverOwner(stem, expectedHash, lease) {
  if (!lockPathFor(stem) || !HASH_RE.test(String(expectedHash || '')) || !lease ||
      !lease.record || lease.record.kind !== 'lock-writer' || lease.record.stem !== stem ||
      lease.record.key !== 'task-lock-recovery:' + stem ||
      typeof lease.leaseId !== 'string' || typeof lease.token !== 'string' ||
      typeof lease.record.sessionId !== 'string') {
    var invalid = new Error('exact lock-recovery authority is invalid');
    invalid.code = 'LOCK_RECOVERY_AUTHORITY_INVALID'; invalid.httpStatus = 400;
    return Promise.reject(invalid);
  }
  return runHelper(['recover-owner', '--stem', stem, '--expected-hash', expectedHash], {
    ORCHESTRATOR_WRITER_SESSION_ID: lease.record.sessionId,
    ORCHESTRATOR_WRITER_STEM: stem,
    ORCHESTRATOR_WRITER_LEASE_ID: lease.leaseId,
    ORCHESTRATOR_WRITER_LEASE_TOKEN: lease.token
  }).then(function (value) {
    if (value.operation !== 'recover-owner' || value.stem !== stem ||
        value.released !== true || value.lockHash !== expectedHash ||
        ['dead', 'reused'].indexOf(value.ownerState) < 0 ||
        ['backlog', 'pending', 'todo'].indexOf(value.state) < 0 ||
        ['fresh', 'stale', 'invalid'].indexOf(value.indexStatus) < 0 ||
        !HASH_RE.test(String(value.sourceRevision || ''))) {
      var malformed = new Error('task-lock recover-owner result is malformed');
      malformed.code = 'LOCK_HELPER_INVALID'; malformed.httpStatus = 500;
      throw malformed;
    }
    return {
      version: 1,
      ok: true,
      operation: 'recover-owner',
      stem: stem,
      released: true,
      lockHash: value.lockHash,
      ownerState: value.ownerState,
      state: value.state,
      sourceRevision: value.sourceRevision,
      indexStatus: value.indexStatus
    };
  });
}

module.exports = {
  LOCK_STEM_RE: LOCK_STEM_RE,
  validTaskStem: validTaskStem,
  readLocksResult: readLocksResult,
  lastActivityIso: lastActivityIso,
  locksDirMtime: locksDirMtime,
  lockPathFor: lockPathFor,
  lockPresence: lockPresence,
  lockOwnedBySession: lockOwnedBySession,
  publicRecoveryReasonCode: publicRecoveryReasonCode,
  inspectOwnerRecovery: inspectOwnerRecovery,
  recoverOwner: recoverOwner
};
