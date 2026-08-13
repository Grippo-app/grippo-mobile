'use strict';

// Bounded, advisory backlog triage. This controller is intentionally separate
// from sessions.js/runner.js: Claude receives no tools, no repository cwd and
// no writer lease. Task identity and task Markdown remain server-owned.

var fs = require('fs');
var path = require('path');
var os = require('os');
var crypto = require('crypto');
var cp = require('child_process');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var requestsMod = require('./requests');
var contract = require('./shallow-intake-contract');
var taskIndexSource = require('./task-source');
var finalizationsMod = require('./finalizations');
var windowsRuntime = require('./windows-runtime-proof');
var shallowOwnerGuard = require('./shallow-owner-guard');
var writerLeases = require(path.join(__dirname, '..', '..', 'tasks', 'writer-leases.cjs'));

var DIR = paths.TASK_INTAKE_DIR;
var STEM_LOCKS_DIR = path.join(DIR, '.stem-locks');
var SCRATCH_DIR = process.env.SHALLOW_INTAKE_SCRATCH_DIR || path.join(
  os.tmpdir(), 'orchestrator-shallow-intake', crypto.createHash('sha256').update(paths.PROJECT_ROOT).digest('hex').slice(0, 20)
);
var WORKER_FILE = path.join(DIR, '.worker.json');
var EVENTS_FILE = path.join(DIR, 'events.jsonl');
var HASH_RE = contract.SOURCE_HASH_RE;
var RESULT_MAX = 64 * 1024;
var STDERR_CLASSIFICATION_MAX = 16 * 1024;
// Storage accepts the canonical creator's complete rendered task envelope.
// The model context remains intentionally smaller (`contract.LIMITS.taskBytes`):
// prepareRequest converts a larger valid task into a durable CONTEXT_TOO_LARGE
// advisory failure instead of making the task unreadable/unretryable.
var TASK_STORAGE_MAX = 64 * 1024 + 4096; // create-backlog.py:TASK_MAX_BYTES
var QUESTIONS_MAX = 8 * 1024 * 1024;
var OWNER_MAX = 4096;
var EVENT_MAX = 1024 * 1024;
// A bounded-append manifest contains the complete replacement image for
// crash-safe CAS recovery. At the one-megabyte event-log cap its base64 WAL is
// below two megabytes; integrity must be able to snapshot that exact evidence
// instead of misclassifying a valid interrupted append as oversized/unsafe.
var GUARD_EVIDENCE_MAX = 2 * 1024 * 1024;
var GUARD_EVIDENCE_TOTAL_MAX = 4 * 1024 * 1024;
var MAX_RUNTIME_ENTRIES = 10000;
var MAX_INTEGRITY_ARTIFACTS = 512;
var MAX_INTEGRITY_PROCESS_PROOFS = 32;
var MAX_INTEGRITY_PROCESS_PROOF_MS = 1000;
var MAX_SCRATCH_ROOT_ENTRIES = 512;
var MAX_SCRATCH_INNER_ENTRIES = 32;
var MAX_SCRATCH_ROOT_TRANSACTIONS = MAX_SCRATCH_ROOT_ENTRIES;
var MAX_SCRATCH_INNER_TRANSACTIONS = MAX_SCRATCH_INNER_ENTRIES;
var MAX_SCRATCH_PAGE = 64;
var MAX_SCRATCH_SWEEP = 16;
var PINNED_EXECUTABLE_NAME = '.model-executable';
var MAX_MODEL_EXECUTABLE_BYTES = 512 * 1024 * 1024;
var QUEUE_MAX = 128;
var LOCK_WAIT_MS = 2000;
var DIR_CREATE_REPROOF_ATTEMPTS = 4;
var DIR_CREATE_REPROOF_WAIT_MS = 5;
var host = os.hostname();
var selfProcessStartIdCache;
// One durable global slot, rather than one limit per Node process. This keeps
// two site instances from multiplying model concurrency.
var concurrency = 1;
var timeoutMs = boundedInt(process.env.SHALLOW_INTAKE_TIMEOUT_MS, 60000, 1000, 10 * 60 * 1000);
var queue = [];
var queued = Object.create(null);
var jobs = Object.create(null);
var active = 0;
var pumpScheduled = false;
var scratchSafetyError = null;
var pendingStemReleases = Object.create(null);
var scratchSweepCursor = null;
var TASKS_AUTHORITY_ROOT = path.resolve(process.env.ORCHESTRATOR_PROJECT_ROOT
  ? paths.PROJECT_ROOT
  : (process.env.ORCHESTRATOR_TASKS_DIR ? path.dirname(paths.TASKS_DIR) : paths.PROJECT_ROOT));
var INTAKE_AUTHORITY_ROOT = path.resolve(process.env.ORCHESTRATOR_PROJECT_ROOT
  ? paths.PROJECT_ROOT
  : (process.env.ORCHESTRATOR_TASK_INTAKE_DIR ? path.dirname(DIR) : paths.WRITER_AUTHORITY_ROOT));
var SCRATCH_AUTHORITY_ROOT = path.resolve(process.env.SHALLOW_INTAKE_SCRATCH_ROOT ||
  (process.env.SHALLOW_INTAKE_SCRATCH_DIR ? path.dirname(SCRATCH_DIR) : os.tmpdir()));

function windowsJobMode() {
  return process.platform === 'win32' || process.env.SHALLOW_INTAKE_TEST_WINDOWS_JOB === '1';
}

function boundedInt(value, fallback, min, max) {
  var n = Number(value);
  if (!Number.isFinite(n)) n = fallback;
  return Math.max(min, Math.min(max, Math.floor(n)));
}
function now() { return new Date().toISOString(); }
function sha256(bytes) { return 'sha256:' + crypto.createHash('sha256').update(bytes).digest('hex'); }
function randomId() { return 'intake-' + crypto.randomBytes(16).toString('hex'); }
function bounded(value, max) { var s = String(value == null ? '' : value); return s.length <= max ? s : s.slice(0, max - 1) + '…'; }
function codedError(code, message, status) { var e = new Error(message || code); e.code = code; e.httpStatus = status || 500; return e; }
function sleepSync(ms) {
  try { Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, ms); }
  catch (error) { var until = Date.now() + ms; while (Date.now() < until) {} }
}
function exactProcessIdentityRequired() { return process.platform === 'linux' || process.platform === 'darwin' || process.platform === 'win32'; }
function validOwnerIsoUtc(value) {
  return typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/.test(value) &&
    value.slice(0, 4) !== '0000' &&
    Number.isFinite(Date.parse(value)) && new Date(Date.parse(value)).toISOString() === value;
}
function validProcessStartId(value) {
  return writerLeases.PROCESS_START_ID_RE.test(String(value || '')) &&
    String(value).indexOf('psid-v1:' + process.platform + ':') === 0;
}
function selfProcessStartId() {
  if (selfProcessStartIdCache !== undefined) return selfProcessStartIdCache;
  try { selfProcessStartIdCache = writerLeases.captureProcessStartId(process.pid); }
  catch (error) { throw codedError('INTAKE_PROCESS_IDENTITY_UNAVAILABLE', bounded(error && error.message || error, 300)); }
  if (exactProcessIdentityRequired() && !validProcessStartId(selfProcessStartIdCache)) {
    throw codedError('INTAKE_PROCESS_IDENTITY_UNAVAILABLE', 'cannot capture exact shallow-intake owner generation');
  }
  return selfProcessStartIdCache || null;
}
function managedRoot(candidate) {
  var target = path.resolve(candidate);
  if (fileGuards.isUnder(path.resolve(DIR), target)) return INTAKE_AUTHORITY_ROOT;
  if (fileGuards.isUnder(path.resolve(paths.LOCKS_DIR), target)) return path.resolve(paths.WRITER_AUTHORITY_ROOT);
  if (fileGuards.isUnder(path.resolve(paths.TASKS_DIR), target)) return TASKS_AUTHORITY_ROOT;
  if (fileGuards.isUnder(path.resolve(SCRATCH_DIR), target)) return SCRATCH_AUTHORITY_ROOT;
  if (fileGuards.isUnder(path.resolve(paths.PROJECT_ROOT), target)) return path.resolve(paths.PROJECT_ROOT);
  return null;
}
function safeDirectory(dir, options) {
  var root = managedRoot(dir);
  var chain = root && fileGuards.realDirectoryUnder(root, dir, options || {});
  if (!chain) throw codedError('INTAKE_DIR_UNSAFE', 'intake path has a symlink/non-directory ancestor or escapes its authority root');
  return chain;
}
function ensureDir(dir) {
  var snapshot, createError, createFailure = {};
  try { snapshot = safeDirectory(dir, { create: true, mode: 0o700, failure: createFailure }); }
  catch (error) { createError = error; }
  if (!snapshot) {
    // The only retryable creation failure is exact absence becoming EEXIST in
    // the rooted worker: another initializer may have created that component.
    // Never retry an unsynced mutation, changed ancestor, transport failure, or
    // ambiguous post-operation result. Each retry is non-creating and must
    // independently prove the current public chain before privacy policy runs.
    if (!createError || createFailure.code !== 'guard-component-raced') throw createError;
    var reproofError = createError;
    for (var attempt = 0; attempt < DIR_CREATE_REPROOF_ATTEMPTS && !snapshot; attempt++) {
      try { snapshot = safeDirectory(dir); }
      catch (error2) {
        reproofError = error2;
        if (attempt + 1 < DIR_CREATE_REPROOF_ATTEMPTS) sleepSync(DIR_CREATE_REPROOF_WAIT_MS);
      }
    }
    if (!snapshot) throw reproofError;
  }
  if (process.platform !== 'win32') return hardenOwnedPrivateDirectory(dir, snapshot);
  var state = windowsRuntime.privatePathState(path.resolve(dir), snapshot.stat);
  if (state !== 'private') {
    state = windowsRuntime.hardenPrivatePath(path.resolve(dir), snapshot.stat);
    if (state !== 'private') throw codedError('INTAKE_DIR_UNSAFE', 'Windows intake directory DACL cannot be made private');
    snapshot = safeDirectory(dir);
  }
  if (windowsRuntime.privatePathState(path.resolve(dir), snapshot.stat) !== 'private') {
    throw codedError('INTAKE_DIR_UNSAFE', 'Windows intake directory privacy proof changed');
  }
  return snapshot;
}
function hardenOwnedPrivateDirectory(dir, snapshot) {
  if (privateScratchDirectory(snapshot, path.resolve(dir))) return snapshot;
  if (typeof process.geteuid !== 'function' || !snapshot || !snapshot.stat) {
    throw codedError('INTAKE_DIR_UNSAFE', 'POSIX intake directory ownership cannot be proven');
  }
  var mode = Number(BigInt(snapshot.stat.modeExact || snapshot.stat.mode) & 0o777n);
  if ((mode & 0o022) !== 0) {
    throw codedError('INTAKE_DIR_UNSAFE', 'POSIX intake directory is group/world writable');
  }
  var fd;
  try {
    fd = fs.openSync(path.resolve(dir), fs.constants.O_RDONLY |
      (fs.constants.O_DIRECTORY || 0) | (fs.constants.O_NOFOLLOW || 0));
    var opened = fs.fstatSync(fd, { bigint: true });
    if (!opened.isDirectory() || opened.uid !== BigInt(process.geteuid()) ||
        String(opened.dev) !== String(snapshot.stat.dev) ||
        String(opened.ino) !== String(snapshot.stat.ino)) {
      throw codedError('INTAKE_DIR_UNSAFE', 'POSIX intake directory identity changed before hardening');
    }
    fs.fchmodSync(fd, 0o700);
    var hardened = fs.fstatSync(fd, { bigint: true });
    if ((hardened.mode & 0o777n) !== 0o700n || hardened.uid !== opened.uid ||
        hardened.dev !== opened.dev || hardened.ino !== opened.ino) {
      throw codedError('INTAKE_DIR_UNSAFE', 'POSIX intake directory privacy hardening did not settle exactly');
    }
  } finally {
    if (fd !== undefined) try { fs.closeSync(fd); } catch (ignore) {}
  }
  snapshot = safeDirectory(dir);
  if (!privateScratchDirectory(snapshot, path.resolve(dir))) {
    throw codedError('INTAKE_DIR_UNSAFE', 'POSIX intake directory privacy proof changed after hardening');
  }
  return snapshot;
}
function boundedDirectoryNames(dir) {
  var root = managedRoot(dir);
  var listed = root && fileGuards.boundedDirectoryNamesUnder(root, dir, MAX_RUNTIME_ENTRIES);
  if (!listed || !listed.ok) {
    throw codedError(listed && listed.code === 'directory-entry-limit' ? 'INTAKE_DIR_TOO_LARGE' : 'INTAKE_DIR_UNSAFE',
      listed && listed.code === 'directory-entry-limit' ? 'intake directory exceeds its bounded entry limit' : 'intake directory cannot be enumerated safely');
  }
  return listed.names.slice().sort();
}
function isResultName(name) {
  return typeof name === 'string' && name.endsWith('.json') && taskIndexSource.safeTaskStem(name.slice(0, -5));
}
function scratchDirectoryFor(requestId) {
  if (typeof requestId !== 'string' || !contract.REQUEST_ID_RE.test(requestId)) return null;
  var root = path.resolve(SCRATCH_DIR);
  var candidate = path.resolve(root, requestId);
  return path.dirname(candidate) === root && path.basename(candidate) === requestId ? candidate : null;
}
function privateScratchDirectory(snapshot, directory) {
  if (!snapshot || snapshot.exists !== true || !snapshot.stat || !snapshot.stat.isDirectory() ||
      snapshot.stat.isSymbolicLink() || typeof directory !== 'string') return false;
  if (process.platform === 'win32') return windowsRuntime.privatePathState(path.resolve(directory), snapshot.stat) === 'private';
  if (typeof process.geteuid !== 'function' || !Number.isInteger(snapshot.stat.mode) ||
      (snapshot.stat.mode & 0o077) !== 0) return false;
  try {
    var live = fs.lstatSync(directory, { bigint: true });
    return live.isDirectory() && !live.isSymbolicLink() && live.uid === BigInt(process.geteuid()) &&
      String(live.dev) === String(snapshot.stat.dev) && String(live.ino) === String(snapshot.stat.ino) &&
      String(live.mode) === String(snapshot.stat.modeExact || snapshot.stat.mode) &&
      (live.mode & 0o77n) === 0n;
  } catch (error) { return false; }
}
function scratchCleanupResult(ok, code) { return { ok: ok, code: code || (ok ? 'clean' : 'retained') }; }
function inspectScratchDirectory(scratch) {
  try {
    var snapshot = safeDirectory(scratch, { allowMissing: true });
    if (!snapshot.exists) return { ok: true, missing: true, snapshot: snapshot };
    if (!privateScratchDirectory(snapshot, scratch)) return { ok: false, missing: false, code: 'directory-not-private' };
    return { ok: true, missing: false, snapshot: snapshot };
  } catch (error) {
    return { ok: false, missing: false, code: 'directory-unsafe' };
  }
}
function scratchNames(scratch) {
  var listed = fileGuards.boundedDirectoryNamesUnder(
    SCRATCH_AUTHORITY_ROOT, scratch, MAX_SCRATCH_INNER_ENTRIES);
  return listed && listed.ok ? { ok: true, names: listed.names.slice().sort() } :
    { ok: false, code: listed && listed.code || 'directory-list-unsafe', names: [] };
}
// Delete only the two server-owned files, then the exact empty request
// directory generation. Every mutation is WAL-backed in file-guards. Unknown
// content is evidence, never permission for recursive cleanup.
function cleanupScratchDirectory(scratch) {
  var requestId = path.basename(String(scratch || ''));
  if (scratchDirectoryFor(requestId) !== scratch) return scratchCleanupResult(false, 'path-unsafe');
  var inspected = inspectScratchDirectory(scratch);
  if (!inspected.ok) return scratchCleanupResult(false, inspected.code);
  if (inspected.missing) return scratchCleanupResult(true, 'already-missing');

  var recovered = fileGuards.reconcileGuardTransactionsUnder(SCRATCH_AUTHORITY_ROOT, scratch, {
    maxEntries: MAX_SCRATCH_INNER_ENTRIES,
    maxTransactions: MAX_SCRATCH_INNER_TRANSACTIONS
  });
  if (!recovered.ok) return scratchCleanupResult(false, recovered.code || 'inner-recovery-pending');

  var before = scratchNames(scratch);
  if (!before.ok) return scratchCleanupResult(false, before.code);
  var ownedNames = ['prompt.txt', PINNED_EXECUTABLE_NAME];
  var unknown = before.names.filter(function (name) { return ownedNames.indexOf(name) < 0; });
  if (unknown.length) return scratchCleanupResult(false, 'unknown-content');

  // A successful target-specific call may have completed an older WAL while
  // preserving a newer public generation. Re-inspect and allow one bounded
  // retry against that newer generation; never infer absence from `true`.
  for (var ownedIndex = 0; ownedIndex < ownedNames.length; ownedIndex++) {
    var ownedName = ownedNames[ownedIndex];
    for (var ownedAttempt = 0; ownedAttempt < 2; ownedAttempt++) {
      var ownedBefore = scratchNames(scratch);
      if (!ownedBefore.ok) return scratchCleanupResult(false, ownedBefore.code);
      if (ownedBefore.names.indexOf(ownedName) < 0) break;
      var ownedFile = path.join(scratch, ownedName);
      if (!fileGuards.unlinkRegularFileUnder(SCRATCH_AUTHORITY_ROOT, scratch, ownedFile, { allowMissing: true })) {
        return scratchCleanupResult(false, ownedName === 'prompt.txt' ? 'prompt-retained' : 'model-executable-retained');
      }
      var ownedEntry = fileGuards.inspectEntryUnder(SCRATCH_AUTHORITY_ROOT, scratch, ownedFile);
      if (ownedEntry && ownedEntry.status === 'missing') break;
      if (ownedAttempt === 1) {
        return scratchCleanupResult(false, ownedName === 'prompt.txt' ? 'prompt-replaced' : 'model-executable-replaced');
      }
    }
  }

  var empty = scratchNames(scratch);
  if (!empty.ok || empty.names.length) {
    return scratchCleanupResult(false, empty.ok ? 'content-retained' : empty.code);
  }
  for (var directoryAttempt = 0; directoryAttempt < 2; directoryAttempt++) {
    var fresh = inspectScratchDirectory(scratch);
    if (!fresh.ok) return scratchCleanupResult(false, fresh.code);
    if (fresh.missing) return scratchCleanupResult(true, 'removed');
    var freshNames = scratchNames(scratch);
    if (!freshNames.ok || freshNames.names.length) {
      return scratchCleanupResult(false, freshNames.ok ? 'content-replaced' : freshNames.code);
    }
    if (!fileGuards.removeEmptyDirectoryUnder(
      SCRATCH_AUTHORITY_ROOT, path.resolve(SCRATCH_DIR), scratch, fresh.snapshot.stat)) {
      return scratchCleanupResult(false, 'directory-retained');
    }
    var after = inspectScratchDirectory(scratch);
    if (!after.ok) return scratchCleanupResult(false, after.code);
    if (after.missing) return scratchCleanupResult(true, 'removed');
    if (directoryAttempt === 1) return scratchCleanupResult(false, 'directory-replaced');
  }
  return scratchCleanupResult(false, 'directory-retained');
}
function workerHandleState(handle, item) {
  if (!handle || !handle.record || handle.token !== handle.record.token || !validWorker(handle.record)) return 'different';
  var current;
  try { current = readJson(WORKER_FILE, OWNER_MAX * 4, true, true); }
  catch (error) {
    var root = managedRoot(path.dirname(WORKER_FILE));
    var entry = root && fileGuards.inspectEntryUnder(root, path.dirname(WORKER_FILE), WORKER_FILE);
    return entry && entry.status === 'missing' ? 'different' : 'unavailable';
  }
  if (!validWorker(current) || current.token !== handle.token || current.pid !== process.pid ||
      current.processStartId !== selfProcessStartId() || JSON.stringify(current) !== JSON.stringify(handle.record)) return 'different';
  return !item || current.stem === item.stem && current.sourceHash === item.sourceHash &&
    current.requestId === item.requestId && current.attempt === item.attempt ? 'match' : 'different';
}
function workerHandleCurrent(handle, item) { return workerHandleState(handle, item) === 'match'; }
function scratchRecoveryEvent(code, requestId) {
  appendEvent('shallow-intake-scratch-retained', {
    requestId: contract.REQUEST_ID_RE.test(String(requestId || '')) ? requestId : undefined,
    reasonCode: bounded(code || 'scratch-retained', 80)
  });
}
function scratchWorkerReservation() {
  var directory = path.dirname(WORKER_FILE), root = managedRoot(directory), value;
  try { value = readJson(WORKER_FILE, OWNER_MAX * 4, true, true); }
  catch (error) {
    var entry = root && fileGuards.inspectEntryUnder(root, directory, WORKER_FILE);
    return entry && entry.status === 'missing'
      ? { ok: true, requestId: null }
      : { ok: false, requestId: null };
  }
  if (!validWorker(value)) return { ok: false, requestId: null };
  return { ok: true, requestId: value.requestId };
}
function reconcileScratchRoot(activeRequestId, sweep) {
  var rootRecovery = fileGuards.reconcileGuardTransactionsUnder(
    SCRATCH_AUTHORITY_ROOT, path.resolve(SCRATCH_DIR), {
      maxEntries: MAX_SCRATCH_ROOT_ENTRIES,
      maxTransactions: MAX_SCRATCH_ROOT_TRANSACTIONS
  });
  if (!rootRecovery.ok) scratchRecoveryEvent(rootRecovery.code || 'root-recovery-pending', activeRequestId);
  if (!sweep) return;
  var reservation = scratchWorkerReservation();
  if (!reservation.ok) {
    scratchRecoveryEvent('worker-reservation-unavailable', activeRequestId);
    return;
  }

  var page = fileGuards.boundedDirectoryPageUnder(
    SCRATCH_AUTHORITY_ROOT, path.resolve(SCRATCH_DIR), {
      pageSize: MAX_SCRATCH_PAGE,
      maxScanEntries: 100000,
      cursor: scratchSweepCursor
    });
  if (!page || !page.ok) {
    if (scratchSweepCursor !== null) scratchSweepCursor = null;
    scratchRecoveryEvent(page && page.code || 'root-scan-unsafe', activeRequestId);
    return;
  }
  scratchSweepCursor = page.done ? null : page.nextCursor;
  var candidates = page.names.filter(function (name) {
    return name !== activeRequestId && name !== reservation.requestId && contract.REQUEST_ID_RE.test(name);
  }).sort();
  if (!candidates.length) return;
  candidates = candidates.slice(0, MAX_SCRATCH_SWEEP);
  candidates.forEach(function (requestId) {
    var result = cleanupScratchDirectory(scratchDirectoryFor(requestId));
    if (!result.ok) scratchRecoveryEvent(result.code, requestId);
  });
}
function prepareScratchDirectory(handle, item) {
  var scratch = scratchDirectoryFor(item && item.requestId);
  if (!scratch) throw codedError('INTAKE_REQUEST_ID_INVALID', 'shallow-intake request id is not canonical');
  if (!workerHandleCurrent(handle, item)) throw codedError('INTAKE_WORKER_CHANGED', 'global intake worker ownership changed');

  // The parent contains rmdir WAL. Reconcile it before inspecting the current
  // request, but retain corrupt/over-cap old evidence so one poisoned orphan
  // cannot permanently deny unrelated advisory work.
  reconcileScratchRoot(item.requestId, false);
  var currentCleanup = cleanupScratchDirectory(scratch);
  if (!currentCleanup.ok) throw codedError('INTAKE_SCRATCH_UNSAFE', 'current scratch cannot be made fresh: ' + currentCleanup.code);
  reconcileScratchRoot(item.requestId, true);
  if (!workerHandleCurrent(handle, item)) throw codedError('INTAKE_WORKER_CHANGED', 'global intake worker ownership changed during scratch recovery');

  var created = ensureDir(scratch);
  if (!privateScratchDirectory(created, scratch)) throw codedError('INTAKE_SCRATCH_UNSAFE', 'fresh scratch directory is not private');
  var names = scratchNames(scratch);
  if (!names.ok || names.names.length) throw codedError('INTAKE_SCRATCH_UNSAFE', 'fresh scratch directory is not empty');
  if (!workerHandleCurrent(handle, item)) throw codedError('INTAKE_WORKER_CHANGED', 'global intake worker ownership changed before scratch use');
  return scratch;
}
function exactPrivateRegularFile(file, read) {
  if (!read || !read.stat || !Buffer.isBuffer(read.bytes) || String(read.stat.nlink) !== '1' ||
      read.stat.type !== 'file') return false;
  if (process.platform === 'win32') {
    return windowsRuntime.privatePathState(path.resolve(file), read.stat) === 'private';
  }
  if (typeof process.geteuid !== 'function') return false;
  var live;
  try { live = fs.lstatSync(file, { bigint: true }); }
  catch (error) { return false; }
  if (!live.isFile() || live.isSymbolicLink() || live.nlink !== 1n ||
      live.uid !== BigInt(process.geteuid()) || (live.mode & 0o77n) !== 0n) return false;
  var proof = read.stat;
  var pairs = [
    [live.dev, proof.dev], [live.ino, proof.ino], [live.mode, proof.modeExact],
    [live.nlink, proof.nlink], [live.size, proof.sizeExact],
    [live.mtimeNs, proof.mtimeNs], [live.ctimeNs, proof.ctimeNs]
  ];
  return pairs.every(function (pair) { return String(pair[0]) === String(pair[1]); });
}
function privatePinnedExecutable(file) {
  if (process.platform === 'win32' || typeof process.geteuid !== 'function') return false;
  try {
    var live = fs.lstatSync(file, { bigint: true });
    return live.isFile() && !live.isSymbolicLink() && live.nlink === 1n &&
      live.uid === BigInt(process.geteuid()) && (live.mode & 0o77n) === 0n &&
      (live.mode & 0o111n) !== 0n && live.size > 0n && live.size <= BigInt(MAX_MODEL_EXECUTABLE_BYTES);
  } catch (error) { return false; }
}
function ensurePublishedPrivateFile(file, maxBytes, expectedBytes, errorCode) {
  var directory = path.dirname(file), root = managedRoot(directory);
  var read = root && fileGuards.boundedRegularFileUnder(root, directory, file, maxBytes);
  if (!read || !read.bytes.equals(expectedBytes)) {
    throw codedError(errorCode || 'INTAKE_FILE_UNSAFE', 'published private file identity changed');
  }
  if (process.platform === 'win32' && !exactPrivateRegularFile(file, read)) {
    if (windowsRuntime.hardenPrivatePath(path.resolve(file), read.stat) !== 'private') {
      throw codedError(errorCode || 'INTAKE_FILE_UNSAFE', 'published Windows file DACL cannot be made private');
    }
    read = fileGuards.boundedRegularFileUnder(root, directory, file, maxBytes);
  }
  if (!read || !read.bytes.equals(expectedBytes) || !exactPrivateRegularFile(file, read)) {
    throw codedError(errorCode || 'INTAKE_FILE_UNSAFE', 'published file is not an exact private generation');
  }
  return read;
}
var APPLICATION_RECORD_STAT_FIELDS = ['dev', 'ino', 'modeExact', 'nlink', 'sizeExact', 'mtimeNs', 'ctimeNs', 'type'];
var APPLICATION_RECORD_REPROOF_ATTEMPTS = 16;
function recognizeDurablePrivateWorkerRecord(expectedBytes, expectedRecord, identity) {
  identity = identity || {};
  var canonical;
  try { canonical = Buffer.from(JSON.stringify(expectedRecord) + '\n', 'utf8'); }
  catch (encodeError) { return false; }
  if (!validWorker(expectedRecord) || !canonical.equals(expectedBytes) ||
      expectedRecord.hostname !== host || expectedRecord.pid !== process.pid ||
      expectedRecord.processStartId !== selfProcessStartId() || expectedRecord.token !== identity.token ||
      expectedRecord.childPid !== identity.childPid || expectedRecord.childProcessStartId !== identity.childProcessStartId ||
      expectedRecord.modelPid !== identity.modelPid || expectedRecord.modelProcessStartId !== identity.modelProcessStartId) return false;
  var file = WORKER_FILE, maxBytes = OWNER_MAX * 4;
  var directory = path.dirname(file), root = managedRoot(directory), observed = null;
  if (!root) return false;
  // A sibling transaction may age the private directory while an anchored
  // read is being re-proved. Retry only until one exact application generation
  // is captured; after that, every durability retry stays bound to its full
  // stat identity and any observed generation change is terminal.
  for (var readAttempt = 0; readAttempt < APPLICATION_RECORD_REPROOF_ATTEMPTS && !observed; readAttempt++) {
    try { observed = ensurePublishedPrivateFile(file, maxBytes, expectedBytes, 'INTAKE_PUBLICATION_UNCERTAIN'); }
    catch (readError) {
      if (readAttempt + 1 < APPLICATION_RECORD_REPROOF_ATTEMPTS) sleepSync(DIR_CREATE_REPROOF_WAIT_MS);
    }
  }
  if (!observed) return false;
  for (var syncAttempt = 0; syncAttempt < APPLICATION_RECORD_REPROOF_ATTEMPTS; syncAttempt++) {
    var fileSynced = fileGuards.fsyncRegularFileUnder(root, directory, file);
    var directorySynced = fileGuards.fsyncDirectoryUnder(root, directory);
    var verified = null;
    try { verified = ensurePublishedPrivateFile(file, maxBytes, expectedBytes, 'INTAKE_PUBLICATION_UNCERTAIN'); }
    catch (verifyError) {}
    if (verified) {
      var same = APPLICATION_RECORD_STAT_FIELDS.every(function (field) {
        return String(observed.stat[field]) === String(verified.stat[field]);
      });
      if (!same) return false;
      if (fileSynced && directorySynced) return true;
    }
    if (syncAttempt + 1 < APPLICATION_RECORD_REPROOF_ATTEMPTS) sleepSync(DIR_CREATE_REPROOF_WAIT_MS);
  }
  return false;
}
function readRegular(file, max, required, options) {
  options = options || {};
  var directory = path.dirname(file), root = managedRoot(directory);
  if (!root) throw codedError('INTAKE_FILE_UNSAFE', 'file escapes its authority root');
  var read = fileGuards.boundedRegularFileUnder(root, directory, file, max);
  var privateFile = exactPrivateRegularFile(file, read);
  if (read && read.stat && String(read.stat.nlink) === '1' && (!options.private || privateFile)) return read.bytes;
  var entry = fileGuards.inspectEntryUnder(root, directory, file);
  if (entry && entry.status === 'missing') {
    if (!required) return null;
    throw codedError('INTAKE_FILE_MISSING', 'required intake file is missing');
  }
  throw codedError('INTAKE_FILE_UNSAFE', 'intake file is unsafe, unstable, or oversized');
}
function atomicJson(file, value) {
  var bytes = Buffer.from(JSON.stringify(value) + '\n', 'utf8');
  if (bytes.length > RESULT_MAX) throw codedError('INTAKE_RESULT_TOO_LARGE');
  var directory = path.dirname(file), root = managedRoot(directory);
  if (!root) throw codedError('INTAKE_FILE_UNSAFE', 'atomic intake publication escaped its authority root');
  var published = fileGuards.atomicReplaceRegularFileResult(root, directory, file, bytes, {
    create: true, directoryMode: 0o700, mode: 0o600, maxBytes: RESULT_MAX, maxExistingBytes: RESULT_MAX
  });
  if (published && published.ok) {
    var privatePublished = ensurePublishedPrivateFile(file, RESULT_MAX, bytes, 'INTAKE_FILE_UNSAFE');
    return Object.assign({}, published, { stat: privatePublished.stat });
  }
  if (published && published.uncertain) {
    // Application identity is the canonical JSON bytes, which include every
    // generation token/request id. If transport lost the worker verdict,
    // accept only a freshly durable, byte-exact and stat-stable public value.
    var observed = fileGuards.boundedRegularFileUnder(root, directory, file, RESULT_MAX);
    if (observed && observed.stat && String(observed.stat.nlink) === '1' && observed.bytes.equals(bytes) &&
        fileGuards.fsyncRegularFileUnder(root, directory, file) && fileGuards.fsyncDirectoryUnder(root, directory)) {
      var verified = fileGuards.boundedRegularFileUnder(root, directory, file, RESULT_MAX);
      var fields = ['dev', 'ino', 'modeExact', 'nlink', 'sizeExact', 'mtimeNs', 'ctimeNs', 'type'];
      if (verified && verified.bytes.equals(bytes) && fields.every(function (field) {
        return String(verified.stat[field]) === String(observed.stat[field]);
      })) {
        var privateVerified = ensurePublishedPrivateFile(file, RESULT_MAX, bytes, 'INTAKE_PUBLICATION_UNCERTAIN');
        return { ok: true, code: 'published-application-recognized', uncertain: false, stat: privateVerified.stat };
      }
    }
    throw codedError('INTAKE_PUBLICATION_UNCERTAIN', 'atomic intake publication has an unresolved commit verdict');
  }
  throw codedError('INTAKE_FILE_UNSAFE', 'atomic intake publication failed closed');
}
function readJson(file, max, required, canonical) {
  var raw = readRegular(file, max, required, { private: canonical === true });
  if (raw === null) return null;
  try {
    var value = JSON.parse(raw.toString('utf8'));
    if (canonical === true && !Buffer.from(JSON.stringify(value) + '\n', 'utf8').equals(raw)) {
      throw codedError('INTAKE_JSON_INVALID', 'JSON is not one canonical line');
    }
    return value;
  }
  catch (error) { throw codedError('INTAKE_JSON_INVALID', 'invalid JSON: ' + path.basename(file)); }
}
function resultPath(stem) { return path.join(DIR, stem + '.json'); }
function ownerPath(dir, key) { return path.join(dir, key + '.json'); }
function ownerRecord(token, details) {
  return Object.assign({ version: 1, pid: process.pid, processStartId: selfProcessStartId(), hostname: host, token: token, createdAt: now() }, details || {});
}
function validOwnerFields(value) {
  var exactIdentity = exactProcessIdentityRequired()
    ? validProcessStartId(value && value.processStartId)
    : value && value.processStartId === null;
  return value && value.version === 1 && Number.isInteger(value.pid) && value.pid > 0 && value.pid <= 0x7fffffff &&
    exactIdentity && typeof value.hostname === 'string' && value.hostname.length >= 1 && value.hostname.length <= 255 &&
    !/[\u0000-\u001f\u007f]/.test(value.hostname) && /^[A-Za-z0-9._:-]{8,160}$/.test(String(value.token || '')) &&
    validOwnerIsoUtc(value.createdAt);
}
function validOwner(value) {
  return validOwnerFields(value) && exactRecordKeys(value, ['createdAt', 'hostname', 'pid', 'processStartId', 'token', 'version']);
}
function ownerLiveness(value, stateFn, treeGoneFn) {
  if (!validOwner(value) && !validWorker(value)) return 'invalid';
  if (value.hostname !== host) return 'remote';
  stateFn = stateFn || writerLeases.processIdentityState;
  treeGoneFn = treeGoneFn || workerRecordTreeGone;
  if (typeof stateFn === 'function') {
    var state = stateFn(value.pid, value.processStartId);
    if (state === 'match' || state === 'pid-live') return 'active';
    if (state === 'dead' || state === 'reused') {
      return validWorker(value) && !treeGoneFn(value, stateFn) ? 'unverified' : 'recovery-required';
    }
    return 'unverified';
  }
  return writerLeases.processIdentityMatches(value.pid, value.processStartId) ? 'active' : 'unverified';
}
function exactProcessGenerationGone(pid, startId, stateFn) {
  if (!Number.isInteger(pid) || pid <= 0 || !validProcessStartId(startId)) return false;
  var state;
  try { state = (stateFn || writerLeases.processIdentityState)(pid, startId); }
  catch (error) { return false; }
  return state === 'dead' || state === 'reused';
}
function workerRecordTreeGone(value, stateFn) {
  if (!validWorker(value)) return false;
  // GO is sent only after the wrapper pair is durably attached. A null wrapper
  // pair is therefore a no-spawn proof. A bound wrapper with a null model pair
  // is also recoverable only after exact wrapper/containment death: the native
  // pre-exec gate cannot receive BOUND until the model pair CAS exists.
  if (value.childPid === null) return value.spawnState === 'not-started' && value.modelPid === null;
  if (!exactProcessGenerationGone(value.childPid, value.childProcessStartId, stateFn)) return false;
  if (value.modelPid !== null && !exactProcessGenerationGone(value.modelPid, value.modelProcessStartId, stateFn)) return false;
  // A Windows Job wrapper exits normally only after ActiveProcesses reaches
  // zero; an abrupt wrapper exit closes its KILL_ON_JOB_CLOSE handle. Exact
  // wrapper+model death is therefore the durable empty-Job proof.
  if (process.platform === 'win32') return true;
  // A killed Darwin wrapper cannot durably communicate the host-wide
  // exact-generation scan which follows direct-model exit. Never downgrade
  // direct PID + old PGID disappearance into a recovered DRAINED receipt.
  // Pre-bind records remain recoverable because BOUND cannot precede modelPid
  // publication; a bound record is deliberately retained for manual recovery.
  if (process.platform === 'darwin' && value.modelPid !== null) return false;
  try { process.kill(-value.childPid, 0); return false; }
  catch (error) { return !!(error && error.code === 'ESRCH'); }
}
var WORKER_SIGNAL_STAT_FIELDS = ['dev', 'ino', 'modeExact', 'nlink', 'sizeExact', 'mtimeNs', 'ctimeNs', 'type'];
function exactWorkerSignalAuthority(file) {
  if (process.platform !== 'darwin' || path.resolve(String(file || '')) !== path.resolve(WORKER_FILE)) return null;
  var directory = path.dirname(WORKER_FILE), root = managedRoot(directory);
  try {
    var directoryProof = safeDirectory(directory);
    if (!privateScratchDirectory(directoryProof, directory)) return null;
  } catch (directoryError) { return null; }
  var read = root && fileGuards.boundedRegularFileUnder(root, directory, WORKER_FILE, OWNER_MAX * 4);
  if (!read || !exactPrivateRegularFile(WORKER_FILE, read)) return null;
  var value;
  try { value = JSON.parse(read.bytes.toString('utf8')); }
  catch (error) { return null; }
  if (!Buffer.from(JSON.stringify(value) + '\n', 'utf8').equals(read.bytes) || !validWorker(value)) return null;
  var proof = {};
  for (var index = 0; index < WORKER_SIGNAL_STAT_FIELDS.length; index++) {
    var field = WORKER_SIGNAL_STAT_FIELDS[index];
    proof[field] = String(read.stat[field]);
  }
  return { bytes: Buffer.from(read.bytes), proof: proof, record: value };
}
function sameWorkerSignalAuthority(authority) {
  var current = exactWorkerSignalAuthority(WORKER_FILE);
  return !!current && current.bytes.equals(authority.bytes) && WORKER_SIGNAL_STAT_FIELDS.every(function (field) {
    return current.proof[field] === authority.proof[field];
  });
}
function reapDarwinOrphanedModel(file, options) {
  options = options || {};
  var platform = options.platform || process.platform;
  if (platform !== 'darwin' || path.resolve(String(file || '')) !== path.resolve(WORKER_FILE)) {
    return { ok: false, code: 'not-applicable', signalled: false };
  }
  var authority = exactWorkerSignalAuthority(file);
  if (!authority) return { ok: false, code: 'authority-unavailable', signalled: false };
  var record = authority.record;
  if (record.hostname !== host || record.spawnState !== 'started' ||
      record.childPid === null || record.modelPid === null || record.modelPid === record.childPid ||
      record.pid === process.pid || record.childPid === process.pid || record.modelPid === process.pid) {
    return { ok: false, code: 'record-ineligible', signalled: false };
  }
  var stateFn = options.stateFn || writerLeases.processIdentityState;
  var proofFn = options.proofFn || writerLeases.processTreeProof;
  var sleepFn = options.sleepFn || sleepSync;
  var signalFn = options.signalFn || function (pgid, signal) { process.kill(-pgid, signal); };
  var beforeSignal = typeof options.beforeSignal === 'function' ? options.beforeSignal : null;
  function state(pid, startId) {
    try { return stateFn(pid, startId); }
    catch (error) { return 'unknown'; }
  }
  function deadGeneration(pid, startId) {
    var observed = state(pid, startId);
    return observed === 'dead' || observed === 'reused';
  }
  function exactOrphanProof() {
    if (!sameWorkerSignalAuthority(authority) ||
        !deadGeneration(record.pid, record.processStartId) ||
        !deadGeneration(record.childPid, record.childProcessStartId) ||
        state(record.modelPid, record.modelProcessStartId) !== 'match') return false;
    var proof;
    try {
      proof = proofFn(record.modelPid, record.modelProcessStartId,
        record.modelPid, record.modelProcessStartId);
    } catch (error) { return false; }
    return !!proof && proof.ok === true && proof.caller && proof.caller.pgid === record.childPid;
  }
  function signalPhase(signal) {
    if (!exactOrphanProof()) return false;
    if (beforeSignal) {
      try { beforeSignal(authority, signal); }
      catch (error) { return false; }
    }
    // Re-prove both the private record inode and the exact live model
    // generation immediately before addressing its isolated wrapper PGID.
    if (!exactOrphanProof()) return false;
    try { signalFn(record.childPid, signal); return true; }
    catch (error) { return !!(error && error.code === 'ESRCH' &&
      state(record.modelPid, record.modelProcessStartId) !== 'match'); }
  }
  function awaitModelGone(limitMs) {
    var deadline = Date.now() + limitMs;
    while (Date.now() <= deadline) {
      var observed = state(record.modelPid, record.modelProcessStartId);
      if (observed === 'dead' || observed === 'reused') return true;
      if (observed !== 'match') return false;
      sleepFn(50);
    }
    return false;
  }

  if (!exactOrphanProof()) return { ok: false, code: 'orphan-unverified', signalled: false };
  if (!signalPhase('SIGTERM')) return { ok: false, code: 'term-unverified', signalled: false };
  if (!awaitModelGone(500)) {
    if (!signalPhase('SIGKILL')) return { ok: false, code: 'kill-unverified', signalled: true };
    if (!awaitModelGone(2000)) return { ok: false, code: 'drain-unverified', signalled: true };
  }
  if (!sameWorkerSignalAuthority(authority) || !workerRecordTreeGone(record)) {
    return { ok: false, code: 'terminal-proof-unavailable', signalled: true };
  }
  return { ok: true, code: 'exact-orphan-drained', signalled: true };
}
var ownerGuard = shallowOwnerGuard.create({
  fileGuards: fileGuards,
  root: INTAKE_AUTHORITY_ROOT,
  maxBytes: OWNER_MAX * 4,
  hostname: host,
  platform: process.platform,
  currentUid: process.platform === 'win32' ? undefined :
    (typeof process.geteuid === 'function' ? process.geteuid() : undefined),
  siteIdentity: function () { return { pid: process.pid, processStartId: selfProcessStartId() }; },
  validateOwner: validOwner,
  validateWorker: validWorker,
  processIdentityState: writerLeases.processIdentityState,
  privatePathState: windowsRuntime.privatePathState,
  proveNoSpawnPossible: function (record) {
    return validWorker(record) && record.spawnState === 'not-started' &&
      record.childPid === null && record.childProcessStartId === null &&
      record.modelPid === null && record.modelProcessStartId === null;
  },
  // READY is emitted before the site can publish modelPid, while EXEC/Resume is
  // authorized only after that exact CAS and BOUND. Therefore a durable null
  // model pair cannot have executed model bytes. Once the exact wrapper and its
  // containment are both gone, this partial generation is safe to recover.
  proveUnboundModelCannotExecute: function (record) {
    return validWorker(record) && record.spawnState === 'started' &&
      record.childPid !== null && record.childProcessStartId !== null &&
      record.modelPid === null && record.modelProcessStartId === null;
  },
  workerContainmentState: function (record) {
    return workerRecordTreeGone(record) ? 'drained' : 'unverified';
  }
});
function isManagedOwnerTarget(file) {
  var resolved = path.resolve(file);
  if (resolved === path.resolve(WORKER_FILE)) return true;
  if (path.dirname(resolved) !== path.resolve(STEM_LOCKS_DIR)) return false;
  var name = path.basename(resolved);
  return name.endsWith('.json') && contract.STEM_RE.test(name.slice(0, -5));
}
function recoverStaleOwner(file) {
  if (!isManagedOwnerTarget(file)) return false;
  if (process.platform === 'darwin' && path.resolve(file) === path.resolve(WORKER_FILE)) {
    reapDarwinOrphanedModel(file);
  }
  var result = ownerGuard.recover(path.resolve(file));
  return !!(result && result.ok && result.committed);
}
function acquireOwner(file, token, waitMs, details) {
  ensureDir(path.dirname(file));
  var deadline = Date.now() + waitMs;
  var ownedRecord = ownerRecord(token, details);
  var ownedBytes = Buffer.from(JSON.stringify(ownedRecord) + '\n', 'utf8');
  var directory = path.dirname(file), root = managedRoot(directory);
  if (!root || ownedBytes.length > OWNER_MAX) throw codedError('INTAKE_OWNER_INVALID', 'intake owner state is unsafe');
  while (true) {
    // Every exact-identity platform enters the same recovery mutex before a
    // publication attempt. This reconciles a helper killed after manifest,
    // detach, or delete and prevents a missing public name from hiding a live
    // owner generation in a private capture.
    if (exactProcessIdentityRequired()) recoverStaleOwner(file);
    var published = fileGuards.publishNoClobberRegularFileUnder(root, directory, file, ownedBytes, {
      create: true, directoryMode: 0o700, mode: 0o600, maxBytes: OWNER_MAX
    });
    if (published && published.ok) {
      try {
        ensurePublishedPrivateFile(file, OWNER_MAX * 4, ownedBytes, 'INTAKE_OWNER_INVALID');
      } catch (privacyError) {
        // Publication already happened. Remove only this exact generation when
        // possible; otherwise retain it fail-closed and surface the error.
        var unsafeRead = fileGuards.boundedRegularFileUnder(root, directory, file, OWNER_MAX * 4);
        if (unsafeRead && unsafeRead.bytes.equals(ownedBytes)) {
          fileGuards.unlinkRegularFileMatchingResultUnder(root, directory, file, OWNER_MAX * 4,
            { bytes: unsafeRead.bytes, proof: unsafeRead.stat });
        }
        throw privacyError;
      }
      return ownedRecord;
    }
    var current;
    try { current = readJson(file, OWNER_MAX, true, true); }
    catch (readError) {
      // Another exact owner may release between our no-clobber `exists`
      // result and the anchored read. Missing is the only benign handoff: try
      // publication again within the original bounded acquisition deadline.
      // Unsafe, unstable, linked, oversized, or malformed state remains a
      // non-retryable authority failure.
      if (readError && readError.code === 'INTAKE_FILE_MISSING' &&
          published && published.code === 'exists') {
        if (Date.now() >= deadline) return false;
        sleepSync(25);
        continue;
      }
      throw codedError('INTAKE_OWNER_INVALID', 'intake owner state is unsafe');
    }
    if (JSON.stringify(current) === JSON.stringify(ownedRecord)) {
      // The publication worker may commit durably and lose any response code.
      // Exact canonical bytes include the random token and process generation,
      // so they are the application identity regardless of transport outcome.
      // Rebind privacy/identity first, then make both file and directory durable
      // before accepting this response-loss replay as our own generation.
      ensurePublishedPrivateFile(file, OWNER_MAX * 4, ownedBytes, 'INTAKE_OWNER_INVALID');
      if (fileGuards.fsyncRegularFileUnder(root, directory, file) &&
          fileGuards.fsyncDirectoryUnder(root, directory)) return ownedRecord;
      throw codedError('INTAKE_OWNER_INVALID', 'owned intake publication durability is unverified');
    }
    if (!validOwner(current) && !validWorker(current)) throw codedError('INTAKE_OWNER_INVALID', 'intake owner state is unsafe');
    if (current.hostname === host && !writerLeases.processIdentityMatches(current.pid, current.processStartId) && recoverStaleOwner(file)) continue;
    if (Date.now() >= deadline) return false;
    sleepSync(25);
  }
}
function releaseOwner(file, token) {
  var result = ownerGuard.release(path.resolve(file), token);
  return !!(result && result.ok && result.committed);
}
function stemOwnerHandleState(file, token) {
  var current;
  try { current = readJson(file, OWNER_MAX, true, true); }
  catch (error) {
    var directory = path.dirname(file), root = managedRoot(directory);
    var entry = root && fileGuards.inspectEntryUnder(root, directory, file);
    return entry && entry.status === 'missing' ? 'released' : 'unavailable';
  }
  if (current === null) return 'released';
  return validOwner(current) && current.hostname === host && current.pid === process.pid &&
    current.processStartId === selfProcessStartId() && current.token === token ? 'match' : 'different';
}
function clearPendingStemRelease(file, entry) {
  if (pendingStemReleases[file] !== entry) return;
  if (entry.timer) clearTimeout(entry.timer);
  delete pendingStemReleases[file];
}
function retryPendingStemRelease(file, scheduleAgain) {
  var entry = pendingStemReleases[file];
  if (!entry) return true;
  entry.timer = null;
  var state = stemOwnerHandleState(file, entry.token);
  if (state === 'released' || state === 'different') {
    clearPendingStemRelease(file, entry);
    return true;
  }
  var released = false;
  try { released = releaseOwner(file, entry.token) === true; }
  catch (error) { released = false; }
  state = stemOwnerHandleState(file, entry.token);
  if (released || state === 'released' || state === 'different') {
    clearPendingStemRelease(file, entry);
    return true;
  }
  if (scheduleAgain !== false && !entry.timer) {
    entry.timer = setTimeout(function () { retryPendingStemRelease(file, true); }, 100);
    if (typeof entry.timer.unref === 'function') entry.timer.unref();
  }
  return false;
}
function retainStemRelease(file, token, stem) {
  var existing = pendingStemReleases[file];
  if (existing && existing.token !== token) {
    retryPendingStemRelease(file, false);
    existing = pendingStemReleases[file];
  }
  if (!existing) pendingStemReleases[file] = { token: token, stem: stem, timer: null };
  appendEvent('shallow-intake-lock-retained', { stem: stem, reasonCode: 'owner-release-unverified' });
  retryPendingStemRelease(file, true);
}
function withStemLock(stem, fn) {
  var token = randomId(), file = ownerPath(STEM_LOCKS_DIR, stem);
  for (var pendingAttempt = 0; pendingStemReleases[file] && pendingAttempt < 3; pendingAttempt++) {
    retryPendingStemRelease(file, false);
    if (pendingStemReleases[file]) sleepSync(25);
  }
  if (pendingStemReleases[file]) throw codedError('INTAKE_BUSY', 'previous intake lock release is still converging', 409);
  if (!acquireOwner(file, token, LOCK_WAIT_MS)) throw codedError('INTAKE_BUSY', 'intake state is busy', 409);
  try { return fn(); }
  finally {
    var released = false;
    try { released = releaseOwner(file, token) === true; }
    catch (error) { released = false; }
    var releaseState = released ? 'released' : stemOwnerHandleState(file, token);
    if (!released && (releaseState === 'match' || releaseState === 'unavailable')) retainStemRelease(file, token, stem);
    else if (!released && releaseState === 'different') {
      throw codedError('INTAKE_LOCK_LOST', 'intake state lock ownership changed before release');
    }
  }
}
function existsUnsafe(file) {
  var directory = path.dirname(file), root = managedRoot(directory);
  var entry = root && fileGuards.inspectEntryUnder(root, directory, file);
  if (entry && entry.status === 'missing') return false;
  if (!entry || entry.status !== 'present' || !entry.stat || !entry.stat.isFile() || entry.stat.isSymbolicLink() ||
      String(entry.stat.nlink) !== '1') {
    throw codedError('TASK_STATE_UNSAFE', 'unsafe task-state artifact');
  }
  return true;
}
function hasAuthoritativeWork(stem) {
  if (existsUnsafe(path.join(paths.LOCKS_DIR, stem + '.json'))) return true;
  // A queued request has not started and may sit behind an offline worker for
  // an arbitrary time. Keep the advisory preview eligible until a worker has
  // actually claimed the task; execution admission then supersedes/cancels it
  // immediately before the model turn. This preserves useful preview work
  // without allowing it to publish once authoritative work owns the stem.
  var claimScan = requestsMod.scanActiveClaims();
  if (!claimScan.ok) throw codedError('TASK_CLAIM_STATE_UNAVAILABLE', claimScan.code);
  var claims = claimScan.rows;
  for (var i = 0; i < claims.length; i++) {
    if (claims[i] && claims[i].stem === stem && ['prep', 'answers', 'drop'].indexOf(claims[i].action) >= 0) return true;
  }
  var leases = writerLeases.scan(paths.WRITER_LEASES_DIR, paths.WRITER_AUTHORITY_ROOT);
  if (leases.issues.length) throw codedError(leases.issues[0].code || 'WRITER_LEASE_INVALID', leases.issues[0].message);
  for (var j = 0; j < leases.active.length; j++) {
    var lease = leases.active[j];
    // A task session owns its stem after the request file has been claimed.
    // Workspace-session helpers publish shared task/INDEX state, so their short
    // mutation window blocks all intake reads rather than exposing a mixed view.
    if (lease.stem === stem || lease.kind === 'workspace-session') return true;
  }
  return false;
}
function sourceState(stem) {
  if (!taskIndexSource.safeTaskStem(stem)) throw codedError('BAD_STEM', 'invalid task stem', 400);
  var backlog = path.join(paths.TASKS_DIR, 'backlog', stem + '.md');
  if (existsUnsafe(path.join(paths.TASKS_DIR, 'pending', stem + '.questions.md'))) return { eligible: false, reason: 'pending' };
  if (existsUnsafe(path.join(paths.TASKS_DIR, 'todo', stem + '.md'))) return { eligible: false, reason: 'todo' };
  if (existsUnsafe(path.join(paths.TASKS_DIR, 'done', stem + '.md'))) return { eligible: false, reason: 'done' };
  var raw = readRegular(backlog, TASK_STORAGE_MAX, false);
  if (raw === null) return { eligible: false, reason: 'missing' };
  var text = raw.toString('utf8');
  if (!Buffer.from(text, 'utf8').equals(raw) || text.indexOf('\u0000') >= 0) throw codedError('TASK_UTF8_INVALID', 'backlog task must be valid UTF-8');
  var source = { eligible: true, bytes: raw, text: text, sourceHash: sha256(raw) };
  // Keep the exact source binding while an authoritative claim temporarily
  // shadows this advisory result.  Runtime integrity still needs to validate
  // the durable complete record during the runner's claim -> handoff window;
  // UI projection continues to hide it because eligible remains false.
  if (hasAuthoritativeWork(stem)) return Object.assign(source, { eligible: false, reason: 'task-prep' });
  if (finalizationsMod.mutationBlocked(stem)) return Object.assign(source, { eligible: false, reason: 'publication-recovery' });
  return source;
}
function goalExcerpt(text) {
  var match = /(?:^|\n)##[ \t]+Goal[ \t]*\r?\n([\s\S]*?)(?=\r?\n##[ \t]+|$)/i.exec(text || '');
  return bounded((match ? match[1] : '').replace(/\s+/g, ' ').trim(), contract.LIMITS.candidateGoalChars);
}
function titleOfTask(text, fallback) {
  var match = /(?:^|\n)#\s*TASK\s+\d+\s*[—-]\s*([^\r\n]+)/.exec(text || '');
  return bounded((match && match[1] || fallback || '').trim(), contract.LIMITS.candidateTitleChars);
}
function tokens(text) {
  var out = Object.create(null);
  String(text || '').normalize('NFKC').toLowerCase().split(/[^\p{L}\p{N}]+/u).forEach(function (x) { if (x.length > 1) out[x] = true; });
  return out;
}
function similarity(a, b) {
  var aa = tokens(a), bb = tokens(b), keys = Object.keys(aa), common = 0, total = Object.keys(bb).length;
  keys.forEach(function (k) { if (bb[k]) common++; else total++; });
  return total ? common / total : 0;
}
function readIndex() {
  var current = taskIndexSource.readIndex();
  if (!current) throw codedError('INDEX_INVALID', 'task INDEX is invalid');
  return current.value;
}
function candidateContext(stem, taskText) {
  var index = readIndex(), targetTitle = titleOfTask(taskText, stem);
  var targetSignal = targetTitle + ' ' + goalExcerpt(taskText), rows = [];
  ['backlog', 'pending', 'todo'].forEach(function (column) {
    index[column].forEach(function (entry) {
      if (!entry || entry.stem === stem || !taskIndexSource.safeTaskStem(entry.stem)) return;
      var artifact = activeTaskArtifact(entry.stem, column);
      if (!artifact) return;
      var text = artifact.bytes.toString('utf8');
      var title = titleOfTask(text, entry.title || entry.stem);
      rows.push({ stem: entry.stem, title: title, goalExcerpt: goalExcerpt(text), column: column,
        exact: title.toLocaleLowerCase() === targetTitle.toLocaleLowerCase(), score: similarity(targetSignal, title + ' ' + goalExcerpt(text)) });
    });
  });
  rows.sort(function (a, b) { return Number(b.exact) - Number(a.exact) || b.score - a.score || a.stem.localeCompare(b.stem); });
  var chars = 0, out = [];
  for (var i = 0; i < rows.length && out.length < contract.LIMITS.candidates; i++) {
    var n = rows[i].title.length + rows[i].goalExcerpt.length;
    if (chars + n > 16000) continue;
    chars += n;
    out.push({ stem: rows[i].stem, title: rows[i].title, goalExcerpt: rows[i].goalExcerpt, column: rows[i].column });
  }
  return out;
}

// INDEX is derived state and can be briefly stale while another task moves or
// drops. Duplicate links are advisory, but they still must resolve to the
// expected live representation for that column. Unsafe/symlinked artifacts are
// treated as absent rather than exposed as clickable candidates.
function activeTaskArtifact(stem, column) {
  try {
    var backlog = path.join(paths.TASKS_DIR, 'backlog', stem + '.md');
    var pending = path.join(paths.TASKS_DIR, 'pending', stem + '.questions.md');
    if (column === 'backlog') {
      if (readRegular(pending, QUESTIONS_MAX, false) !== null) return null;
      var backlogBytes = readRegular(backlog, TASK_STORAGE_MAX, false);
      return backlogBytes === null ? null : { bytes: backlogBytes };
    }
    if (column === 'pending') {
      var body = readRegular(backlog, TASK_STORAGE_MAX, false);
      var questions = readRegular(pending, QUESTIONS_MAX, false);
      return body === null || questions === null ? null : { bytes: body };
    }
    if (column === 'todo') {
      var todo = readRegular(path.join(paths.TASKS_DIR, 'todo', stem + '.md'), TASK_STORAGE_MAX, false);
      return todo === null ? null : { bytes: todo };
    }
  } catch (error) {}
  return null;
}
function projectFlags() {
  var raw;
  try { raw = readRegular(paths.PROJECT_CONFIG_FILE, 256 * 1024, false); }
  catch (error) { return {}; }
  if (!raw) return {};
  var text = raw.toString('utf8'), front = /^---\s*\r?\n([\s\S]*?)\r?\n---/.exec(text), fm = front ? front[1] : '';
  function scalar(key) { var m = new RegExp('^' + key + ':[ \\t]*(.+?)[ \\t]*$', 'm').exec(fm); return m ? m[1].trim() : ''; }
  function bool(key) { var v = scalar(key); return v === 'true' ? true : v === 'false' ? false : undefined; }
  var out = {}, v;
  ['figmaEnabled', 'prelaunch', 'iosEnabled'].forEach(function (key) { v = bool(key); if (v !== undefined) out[key] = v; });
  v = scalar('backendContractEnabled'); if (['auto', 'true', 'false'].indexOf(v) >= 0) out.backendContractEnabled = v;
  var localeBlock = /(?:^|\n)supportedLocales:[^\n]*\n((?:[ \t]+-[^\n]*\n?)*)/.exec(fm);
  out.supportedLocaleCount = localeBlock ? (localeBlock[1].match(/^[ \t]+-/gm) || []).length : 0;
  return out;
}
function buildContext(stem, source) {
  return { stem: stem, taskText: source.text, candidates: candidateContext(stem, source.text), projectFlags: projectFlags() };
}

function baseRecord(stem, sourceHash, status, requestId, attempt, createdAt) {
  return { version: 1, stem: stem, sourceHash: sourceHash, createdAt: createdAt || now(), status: status,
    requestId: requestId || randomId(), attempt: attempt || 1 };
}
function retryableError(error) {
  var code = String(error && error.code || 'INTAKE_PREPARE_FAILED');
  return !['CONTEXT_TOO_LARGE', 'CONTEXT_INVALID', 'TASK_UTF8_INVALID', 'INTAKE_OWNER_INVALID'].includes(code);
}
function failedRecord(stem, sourceHash, requestId, attempt, createdAt, error) {
  var value = baseRecord(stem, sourceHash, 'failed', requestId, attempt, createdAt);
  value.errorCode = bounded(error && error.code || 'INTAKE_PREPARE_FAILED', 80);
  value.retryable = retryableError(error);
  return value;
}
function exactRecordKeys(value, expected) {
  var keys = value && typeof value === 'object' && !Array.isArray(value) ? Object.keys(value).sort() : [];
  var wanted = expected.slice().sort();
  return keys.length === wanted.length && keys.every(function (key, i) { return key === wanted[i]; });
}
function validateRecord(value, stem) {
  var common = ['version', 'stem', 'sourceHash', 'createdAt', 'status', 'requestId', 'attempt'];
  if (!value || value.version !== 1 || value.stem !== stem || !HASH_RE.test(String(value.sourceHash || '')) ||
      !['queued', 'checking', 'complete', 'failed', 'dismissed', 'superseded'].includes(value.status) ||
      !contract.REQUEST_ID_RE.test(String(value.requestId || '')) || !Number.isInteger(value.attempt) ||
      value.attempt < 1 || value.attempt > contract.LIMITS.attempts || !contract.validIsoUtc(value.createdAt)) {
    throw codedError('INTAKE_RESULT_INVALID', 'stored intake result is invalid');
  }
  if (value.status === 'failed') {
    if (!exactRecordKeys(value, common.concat(['errorCode', 'retryable'])) ||
        typeof value.errorCode !== 'string' || value.errorCode.length < 1 || value.errorCode.length > 80 ||
        typeof value.retryable !== 'boolean') throw codedError('INTAKE_RESULT_INVALID', 'stored failed intake result is invalid');
  } else if (value.status === 'superseded') {
    if (!exactRecordKeys(value, common.concat(['reasonCode'])) ||
        typeof value.reasonCode !== 'string' || value.reasonCode.length < 1 || value.reasonCode.length > 80) {
      throw codedError('INTAKE_RESULT_INVALID', 'stored superseded intake result is invalid');
    }
  } else if (value.status === 'complete') {
    if (!exactRecordKeys(value, common.concat(['readiness', 'summary', 'likelyAreas', 'possibleDuplicates',
      'missingContext', 'riskFlags', 'modelDurationMs', 'resultBytes'])) ||
        !Array.isArray(value.possibleDuplicates) || !Array.isArray(value.missingContext) || !Array.isArray(value.riskFlags)) {
      throw codedError('INTAKE_RESULT_INVALID', 'stored complete intake result is invalid');
    }
  } else if (!exactRecordKeys(value, common)) {
    throw codedError('INTAKE_RESULT_INVALID', 'stored intake result has unexpected fields');
  }
  return value;
}
function readRecord(stem) { var value = readJson(resultPath(stem), RESULT_MAX, false, true); return value === null ? null : validateRecord(value, stem); }
function writeRecord(stem, value) { validateRecord(value, stem); atomicJson(resultPath(stem), value); return value; }
function quarantineInvalidRecord(stem, source, error) {
  if (!source || !source.eligible || !error || ['INTAKE_JSON_INVALID', 'INTAKE_RESULT_INVALID'].indexOf(error.code) < 0) return null;
  try {
    return withStemLock(stem, function () {
      var liveSource = sourceState(stem);
      if (!liveSource.eligible || liveSource.sourceHash !== source.sourceHash) return null;
      // Do not clobber a concurrently repaired generation. Cooperative writers
      // use this same stem lock; re-read and fully project the live record before
      // replacing only state that is still malformed.
      try {
        var repaired = readRecord(stem);
        if (repaired && repaired.status === 'complete') projectCompleteRecord(stem, repaired, liveSource);
        return repaired;
      } catch (liveError) {
        if (!liveError || ['INTAKE_JSON_INVALID', 'INTAKE_RESULT_INVALID'].indexOf(liveError.code) < 0) return null;
      }
      // This is advisory state only.  Replace a regular but malformed cache
      // record with a bounded retryable failure so it cannot poison /api/state
      // and a user can recover without deleting files by hand.
      var failed = failedRecord(stem, source.sourceHash, null, 1, null, codedError('INTAKE_CACHE_INVALID'));
      writeRecord(stem, failed);
      appendEvent('shallow-intake-failed', { stem: stem, sourceHash: source.sourceHash, requestId: failed.requestId,
        attempt: failed.attempt, errorCode: failed.errorCode, durationMs: 0 });
      return failed;
    });
  } catch (persistError) { return null; }
}
function appendEvent(kind, data) {
  try {
    var line = Object.assign({ timestamp: now(), event: kind }, data || {});
    var bytes = Buffer.from(JSON.stringify(line) + '\n');
    if (bytes.length > 2048) return;
    for (var attempt = 0; attempt < 3; attempt++) {
      var existing = fileGuards.boundedRegularFileUnder(INTAKE_AUTHORITY_ROOT, DIR, EVENTS_FILE, EVENT_MAX);
      if (!existing) {
        var entry = fileGuards.inspectEntryUnder(INTAKE_AUTHORITY_ROOT, DIR, EVENTS_FILE);
        if (!entry || entry.status !== 'missing') return;
        var empty = Buffer.alloc(0);
        var created = fileGuards.publishNoClobberRegularFileUnder(
          INTAKE_AUTHORITY_ROOT, DIR, EVENTS_FILE, empty,
          { create: true, directoryMode: 0o700, mode: 0o600, maxBytes: EVENT_MAX });
        if (!created || (!created.ok && created.code !== 'exists' && created.code !== 'published-unverified')) return;
        existing = fileGuards.boundedRegularFileUnder(INTAKE_AUTHORITY_ROOT, DIR, EVENTS_FILE, EVENT_MAX);
      }
      if (!existing || existing.bytes.length + bytes.length > EVENT_MAX) return;
      existing = ensurePublishedPrivateFile(EVENTS_FILE, EVENT_MAX, existing.bytes, 'INTAKE_EVENT_UNSAFE');
      var appended = fileGuards.appendBoundedRegularFileUnder(INTAKE_AUTHORITY_ROOT, DIR, EVENTS_FILE, bytes, {
        create: false, mode: 0o600, maxBytes: EVENT_MAX, maxAppendBytes: 2048,
        expectedProof: existing.stat
      });
      if (!appended || !appended.ok) {
        if (appended && (appended.code === 'expected-changed' || appended.code === 'expected-missing')) continue;
        return;
      }
      var expected = Buffer.concat([existing.bytes, bytes]);
      ensurePublishedPrivateFile(EVENTS_FILE, EVENT_MAX, expected, 'INTAKE_EVENT_UNSAFE');
      return;
    }
  } catch (error2) {}
}
function queueKey(stem, requestId) { return stem + '\u0000' + requestId; }
function enqueue(record, reason) {
  var key = queueKey(record.stem, record.requestId);
  if (queued[key] || jobs[record.stem] && jobs[record.stem].requestId === record.requestId) return false;
  if (queue.length >= QUEUE_MAX) throw codedError('INTAKE_QUEUE_FULL', 'shallow intake queue is full', 503);
  queued[key] = true; queue.push({ stem: record.stem, sourceHash: record.sourceHash, requestId: record.requestId, attempt: record.attempt, reason: reason || 'schedule' });
  appendEvent('shallow-intake-queued', { stem: record.stem, sourceHash: record.sourceHash, requestId: record.requestId, attempt: record.attempt, reasonCode: bounded(reason || 'schedule', 80) });
  schedulePump(); return true;
}
function schedule(stem, reason, options) {
  options = options || {};
  ensureSafeScratch();
  var source = sourceState(stem);
  if (!source.eligible) throw codedError('INTAKE_NOT_ELIGIBLE', 'task is not an idle backlog item: ' + source.reason, 409);
  return withStemLock(stem, function () {
    var current = readRecord(stem);
    if (!options.force && current && current.sourceHash === source.sourceHash) {
      if (current.status === 'queued' || current.status === 'checking') enqueue(current, 'reconcile');
      if (current.status !== 'superseded') return current;
    }
    cancelLocal(stem);
    var attempt = options.force && current && current.sourceHash === source.sourceHash
      ? Math.min(contract.LIMITS.attempts, current.attempt + 1) : 1;
    var next = baseRecord(stem, source.sourceHash, 'queued', null, attempt);
    writeRecord(stem, next);
    try { enqueue(next, reason || 'created'); }
    catch (error) {
      writeRecord(stem, failedRecord(stem, source.sourceHash, next.requestId, attempt, next.createdAt, error));
      throw error;
    }
    return next;
  });
}
function schedulePump() {
  if (pumpScheduled) return;
  pumpScheduled = true;
  setImmediate(function () { pumpScheduled = false; pump(); });
}
function modelEnv() {
  var allowed = ['PATH', 'HOME', 'USER', 'LOGNAME', 'SHELL', 'TMPDIR', 'TMP', 'TEMP', 'LANG', 'LC_ALL',
    'XDG_CONFIG_HOME', 'XDG_CACHE_HOME', 'CLAUDE_CONFIG_DIR', 'ANTHROPIC_API_KEY'];
  // CreateProcess/Python and .cmd/.bat resolution rely on these system-owned
  // values. Keep the allow-list explicit; no arbitrary parent environment is
  // delegated to the model wrapper.
  if (windowsJobMode()) allowed.push('SystemRoot', 'WINDIR', 'COMSPEC', 'PATHEXT');
  var env = {};
  allowed.forEach(function (key) { if (process.env[key] != null) env[key] = process.env[key]; });
  return env;
}
function validWorker(value) {
  var keys = ['attempt', 'childPid', 'childProcessStartId', 'createdAt', 'hostname', 'pid', 'processStartId', 'requestId',
    'modelPid', 'modelProcessStartId', 'sourceHash', 'spawnState', 'stem', 'token', 'updatedAt', 'version'];
  var childIdentity = value && (value.childPid === null && value.childProcessStartId === null ||
    Number.isInteger(value.childPid) && value.childPid > 0 && value.childPid <= 0x7fffffff && (exactProcessIdentityRequired()
      ? validProcessStartId(value.childProcessStartId)
      : value.childProcessStartId === null));
  var modelIdentity = value && (value.modelPid === null && value.modelProcessStartId === null ||
    Number.isInteger(value.modelPid) && value.modelPid > 0 && value.modelPid <= 0x7fffffff &&
      value.modelPid !== value.childPid && value.childPid !== null && (exactProcessIdentityRequired()
        ? validProcessStartId(value.modelProcessStartId)
        : value.modelProcessStartId === null));
  var spawnState = value && (value.spawnState === 'not-started'
    ? value.childPid === null && value.modelPid === null
    : value.spawnState === 'started' && value.childPid !== null);
  return validOwnerFields(value) && exactRecordKeys(value, keys) && taskIndexSource.safeTaskStem(value.stem) && HASH_RE.test(String(value.sourceHash || '')) &&
    contract.REQUEST_ID_RE.test(String(value.requestId || '')) && Number.isInteger(value.attempt) && value.attempt >= 1 &&
    value.attempt <= contract.LIMITS.attempts && childIdentity && modelIdentity && spawnState && validOwnerIsoUtc(value.updatedAt);
}
function workerState() {
  var value = readJson(WORKER_FILE, OWNER_MAX * 4, false, true);
  if (value === null) return null;
  if (!validWorker(value)) throw codedError('INTAKE_WORKER_INVALID', 'global intake worker state is unsafe');
  if (value.hostname === host && !writerLeases.processIdentityMatches(value.pid, value.processStartId) && recoverStaleOwner(WORKER_FILE)) return null;
  return value;
}
function acquireWorker(item) {
  var token = randomId();
  var details = {
    stem: item.stem,
    sourceHash: item.sourceHash,
    requestId: item.requestId,
    attempt: item.attempt,
    childPid: null,
    childProcessStartId: null,
    modelPid: null,
    modelProcessStartId: null,
    spawnState: 'not-started',
    updatedAt: now()
  };
  // acquireOwner returns the exact record bytes it published. Reconstructing
  // here would create a different `createdAt` generation; doing an unowned
  // re-read failure after publication could also strand a live global owner.
  var record = acquireOwner(WORKER_FILE, token, 0, details);
  if (!record) return null;
  if (!validWorker(record) || record.token !== token || record.pid !== process.pid ||
      record.processStartId !== selfProcessStartId()) {
    throw codedError('INTAKE_WORKER_ACQUIRE_FAILED', 'published global intake worker record is invalid');
  }
  return { token: token, record: record };
}
function attachWorkerChild(handle, childPid) {
  if (!handle || !Number.isInteger(childPid) || childPid <= 0) throw codedError('INTAKE_WORKER_ATTACH_FAILED');
  var directory = path.dirname(WORKER_FILE), root = managedRoot(directory);
  var read = root && fileGuards.boundedRegularFileUnder(root, directory, WORKER_FILE, OWNER_MAX * 4);
  if (!read || !read.stat || String(read.stat.nlink) !== '1') {
    throw codedError('INTAKE_WORKER_ATTACH_FAILED', 'global intake worker cannot be read with an exact proof');
  }
  var current;
  try {
    current = JSON.parse(read.bytes.toString('utf8'));
    if (!Buffer.from(JSON.stringify(current) + '\n', 'utf8').equals(read.bytes)) throw new Error('non-canonical');
  } catch (error) { throw codedError('INTAKE_WORKER_ATTACH_FAILED', 'global intake worker record is invalid'); }
  if (!validWorker(current) || current.token !== handle.token || current.pid !== process.pid ||
      current.spawnState !== 'not-started' || current.childPid !== null || current.modelPid !== null) {
    throw codedError('INTAKE_WORKER_ATTACH_FAILED', 'global intake worker ownership changed');
  }
  var childProcessStartId;
  try { childProcessStartId = writerLeases.captureProcessStartId(childPid); }
  catch (error) { throw codedError('INTAKE_WORKER_ATTACH_FAILED', 'cannot capture exact worker process generation'); }
  if (exactProcessIdentityRequired() && !validProcessStartId(childProcessStartId)) {
    throw codedError('INTAKE_WORKER_ATTACH_FAILED', 'worker process generation disappeared before durable binding');
  }
  if (exactProcessIdentityRequired()) {
    var childProof = writerLeases.processTreeProof(childPid, childProcessStartId, process.pid, selfProcessStartId());
    if (!childProof || childProof.ok !== true || childProof.depth !== 1 || !childProof.caller ||
        (!windowsJobMode() && childProof.caller.pgid !== childPid)) {
      throw codedError('INTAKE_WORKER_ATTACH_FAILED', 'spawned wrapper ancestry or detached process-group identity is unverified');
    }
  }
  var attached = Object.assign({}, current, {
    childPid: childPid,
    childProcessStartId: childProcessStartId || null,
    spawnState: 'started',
    updatedAt: now()
  });
  var attachedBytes = Buffer.from(JSON.stringify(attached) + '\n', 'utf8');
  var swapped = fileGuards.compareAndSwapRegularFileUnder(root, directory, WORKER_FILE, OWNER_MAX * 4,
    { bytes: read.bytes, proof: read.stat }, attachedBytes, { mode: 0o600 });
  if ((!swapped || !swapped.ok) &&
      !recognizeDurablePrivateWorkerRecord(attachedBytes, attached, {
        token: handle.token, childPid: childPid, childProcessStartId: childProcessStartId || null,
        modelPid: null, modelProcessStartId: null
      })) {
    throw codedError('INTAKE_WORKER_ATTACH_FAILED', 'global intake worker changed or its exact attach commit is unverified');
  }
  handle.record = attached;
  return attached;
}
function bindWorkerModel(handle, job, modelPid) {
  if (!handle || !job || !Number.isInteger(modelPid) || modelPid <= 0 || !job.child ||
      modelPid === job.child.pid) throw codedError('INTAKE_MODEL_BIND_FAILED', 'reported model PID is invalid');
  var directory = path.dirname(WORKER_FILE), root = managedRoot(directory);
  var read = root && fileGuards.boundedRegularFileUnder(root, directory, WORKER_FILE, OWNER_MAX * 4);
  if (!read || !read.stat || String(read.stat.nlink) !== '1') {
    throw codedError('INTAKE_MODEL_BIND_FAILED', 'global worker cannot be read with an exact proof');
  }
  var current;
  try {
    current = JSON.parse(read.bytes.toString('utf8'));
    if (!Buffer.from(JSON.stringify(current) + '\n', 'utf8').equals(read.bytes)) throw new Error('non-canonical');
  } catch (error) { throw codedError('INTAKE_MODEL_BIND_FAILED', 'global worker record is invalid'); }
  if (!validWorker(current) || current.token !== handle.token || current.pid !== process.pid ||
      current.processStartId !== selfProcessStartId() || current.childPid !== job.child.pid ||
      current.childProcessStartId !== job.childProcessStartId || current.modelPid !== null ||
      current.spawnState !== 'started' ||
      JSON.stringify(current) !== JSON.stringify(handle.record)) {
    throw codedError('INTAKE_MODEL_BIND_FAILED', 'global worker generation changed before model binding');
  }
  var modelProcessStartId;
  try { modelProcessStartId = writerLeases.captureProcessStartId(modelPid); }
  catch (error) { throw codedError('INTAKE_MODEL_BIND_FAILED', 'cannot capture exact model process generation'); }
  if (exactProcessIdentityRequired() && !validProcessStartId(modelProcessStartId)) {
    throw codedError('INTAKE_MODEL_BIND_FAILED', 'model process disappeared before durable binding');
  }
  if (exactProcessIdentityRequired()) {
    var modelProof = writerLeases.processTreeProof(
      modelPid, modelProcessStartId, current.childPid, current.childProcessStartId);
    if (!modelProof || modelProof.ok !== true || modelProof.depth !== 1) {
      throw codedError('INTAKE_MODEL_BIND_FAILED', 'reported model is not the direct child of the bound wrapper generation');
    }
  }
  var bound = Object.assign({}, current, {
    modelPid: modelPid,
    modelProcessStartId: modelProcessStartId || null,
    updatedAt: now()
  });
  var boundBytes = Buffer.from(JSON.stringify(bound) + '\n', 'utf8');
  var swapped = fileGuards.compareAndSwapRegularFileUnder(root, directory, WORKER_FILE, OWNER_MAX * 4,
    { bytes: read.bytes, proof: read.stat }, boundBytes, { mode: 0o600 });
  if ((!swapped || !swapped.ok) &&
      !recognizeDurablePrivateWorkerRecord(boundBytes, bound, {
        token: handle.token, childPid: job.child.pid, childProcessStartId: job.childProcessStartId,
        modelPid: modelPid, modelProcessStartId: modelProcessStartId || null
      })) {
    throw codedError('INTAKE_MODEL_BIND_FAILED', 'global worker changed or its exact model-bind commit is unverified');
  }
  handle.record = bound;
  job.modelPid = modelPid;
  job.modelProcessStartId = modelProcessStartId || null;
  job.modelBindingState = 'bound';
  return bound;
}
function releaseWorker(handle, job) {
  if (!handle || !handle.record || !validWorker(handle.record)) return false;
  // Keep the destructive primitive behind the same live containment proof;
  // callers cannot accidentally turn wrapper exit into release authority.
  if (handle.record.childPid !== null && (!job || !processTreeGone(job))) return false;
  return releaseOwner(WORKER_FILE, handle.token);
}
function processTreeGone(job) {
  var child = job && job.child;
  if (!child || !child.pid) return true;
  // A Windows wrapper/leader exit is not descendant-death proof.  The dedicated
  // Job Object wrapper emits this nonce-authenticated record only after
  // ActiveProcesses reaches zero; without it the durable global slot stays
  // fail-closed even when the wrapper PID has already disappeared.
  if (job.windowsJobMode) return job.windowsJobDrained === true && job.windowsJobUnverified !== true;
  if (job.posixControlUnverified === true) return false;
  if (job.modelBindingState !== 'bound' && job.posixControlTerminal !== 'empty' &&
      job.posixControlTerminal !== 'drained') return false;
  if (job.modelBindingState === 'bound' &&
      !exactProcessGenerationGone(job.modelPid, job.modelProcessStartId)) return false;
  if (process.platform === 'darwin' && job.modelBindingState === 'bound' &&
      job.posixControlTerminal !== 'drained') return false;
  try { process.kill(-child.pid, 0); return false; }
  catch (error) { return !!(error && error.code === 'ESRCH'); }
}
function exactChildStillOwned(job) {
  if (!job || !job.child || !job.child.pid || job.childClosed) return false;
  if (!exactProcessIdentityRequired()) return true;
  return validProcessStartId(job.childProcessStartId) &&
    writerLeases.processIdentityMatches(job.child.pid, job.childProcessStartId);
}
function exactModelStillOwned(job) {
  if (!job || job.modelBindingState !== 'bound' || !Number.isInteger(job.modelPid)) return false;
  if (!exactProcessIdentityRequired()) return true;
  return validProcessStartId(job.modelProcessStartId) &&
    writerLeases.processIdentityMatches(job.modelPid, job.modelProcessStartId);
}
function appendWorkerStderr(job, chunk) {
  var bytes = Buffer.isBuffer(chunk) ? chunk : Buffer.from(String(chunk || ''), 'utf8');
  job.stderrBytes = Math.min(STDERR_CLASSIFICATION_MAX, job.stderrBytes + bytes.length);
  var sample = Buffer.concat([job.stderrSample || Buffer.alloc(0), bytes]);
  job.stderrSample = sample.length <= STDERR_CLASSIFICATION_MAX
    ? sample : sample.subarray(sample.length - STDERR_CLASSIFICATION_MAX);
}
function modelProcessFailure(job) {
  // Keep stderr memory-only and bounded: it may contain provider diagnostics,
  // but never belongs in a public result/event. This exact signature is the
  // known Claude CLI/macOS incompatibility where Keychain access is blocked;
  // retrying unchanged cannot succeed.
  var stderr = job && job.stderrSample ? job.stderrSample.toString('utf8') : '';
  var keychainSpawn = /\bposix_spawn\s+['"](?:\/usr\/bin\/)?security['"]/i.test(stderr);
  var permissionDenied = /\bEPERM\b|operation not permitted/i.test(stderr);
  if (keychainSpawn && permissionDenied) {
    return { code: 'MODEL_KEYCHAIN_SANDBOX_INCOMPATIBLE', retryable: false };
  }
  if (/--json-schema is not a valid JSON Schema/i.test(stderr)) {
    return { code: 'MODEL_SCHEMA_TRANSPORT_INCOMPATIBLE', retryable: false };
  }
  return { code: 'MODEL_PROCESS_FAILED', retryable: true };
}
function appendWorkerControl(job, chunk) {
  job.workerControlBuffer += String(chunk || '');
  var newline;
  while ((newline = job.workerControlBuffer.indexOf('\n')) >= 0) {
    var line = job.workerControlBuffer.slice(0, newline).replace(/\r$/, '');
    job.workerControlBuffer = job.workerControlBuffer.slice(newline + 1);
    var parts = line.split(' ');
    if (job.windowsJobMode && parts.length === 3 && parts[0] === 'INTAKE_WINDOWS_JOB_READY' && parts[1] === job.workerControlNonce &&
        /^\d+$/.test(parts[2] || '') && Number.isSafeInteger(Number(parts[2])) && Number(parts[2]) > 0 &&
        !job.windowsJobReady && !job.windowsJobTerminal) {
      job.windowsJobReady = true;
      acceptModelReady(job, Number(parts[2]));
    } else if (job.windowsJobMode && parts.length === 2 && parts[0] === 'INTAKE_WINDOWS_JOB_DRAINED' && parts[1] === job.workerControlNonce && !job.windowsJobTerminal) {
      job.windowsJobTerminal = 'drained';
      job.windowsJobDrained = true;
    } else if (job.windowsJobMode && parts.length === 2 && parts[0] === 'INTAKE_WINDOWS_JOB_UNVERIFIED' && parts[1] === job.workerControlNonce && !job.windowsJobTerminal) {
      job.windowsJobTerminal = 'unverified';
      job.windowsJobUnverified = true;
    } else if (!job.windowsJobMode && parts.length === 3 && parts[0] === 'INTAKE_POSIX_MODEL_READY' &&
        parts[1] === job.workerControlNonce && /^\d+$/.test(parts[2] || '') &&
        Number.isSafeInteger(Number(parts[2])) && Number(parts[2]) > 0 && !job.posixControlReady &&
        !job.posixControlTerminal) {
      job.posixControlReady = true;
      acceptModelReady(job, Number(parts[2]));
    } else if (!job.windowsJobMode && parts.length === 3 && parts[0] === 'INTAKE_POSIX_MODEL_DRAINED' &&
        parts[1] === job.workerControlNonce && /^\d+$/.test(parts[2] || '') &&
        Number(parts[2]) === job.reportedModelPid && !job.posixControlTerminal) {
      job.posixControlTerminal = 'drained';
    } else if (!job.windowsJobMode && parts.length === 2 && parts[0] === 'INTAKE_POSIX_MODEL_EMPTY' &&
        parts[1] === job.workerControlNonce && !job.posixControlReady && !job.posixControlTerminal) {
      job.posixControlTerminal = 'empty';
    } else if (line) {
      if (job.windowsJobMode) job.windowsJobUnverified = true;
      else job.posixControlUnverified = true;
    }
  }
  // This is a dedicated wrapper-only pipe. Keep it bounded anyway: malformed
  // helper output is not allowed to consume memory or become a drain proof.
  if (job.workerControlBuffer.length > 4096) {
    if (job.windowsJobMode) job.windowsJobUnverified = true;
    else job.posixControlUnverified = true;
    job.workerControlBuffer = job.workerControlBuffer.slice(-512);
  }
}
function acceptModelReady(job, modelPid) {
  if (job.reportedModelPid !== null || job.modelBindingState !== 'prebind') {
    if (job.windowsJobMode) job.windowsJobUnverified = true;
    else job.posixControlUnverified = true;
    return false;
  }
  job.reportedModelPid = modelPid;
  try {
    bindWorkerModel(job.workerHandle, job, modelPid);
    var binding = 'BOUND ' + job.workerHandle.token + ' ' + job.workerControlNonce + ' ' + modelPid + '\n';
    function bindingFlushed(error) {
      if (error) {
        if (!job.finished) {
          error.code = 'MODEL_STDIN_FAILED';
          job.spawnError = job.spawnError || error;
          if (job.windowsJobMode) job.windowsJobUnverified = true;
          else terminateJob(job, 'SIGKILL');
        }
        return;
      }
      job.modelBindingAcknowledged = true;
      if (!job.windowsJobMode) {
        try { job.child.stdin.end(); }
        catch (endError) {
          if (!job.finished) {
            endError.code = 'MODEL_STDIN_FAILED';
            job.spawnError = job.spawnError || endError;
            terminateJob(job, 'SIGKILL');
          }
        }
      }
    }
    if (job.windowsJobMode) {
      // The native model is still CREATE_SUSPENDED. Only this exact record,
      // sent after the model CAS committed, authorizes ResumeThread.
      job.child.stdin.write(binding, bindingFlushed);
    } else {
      // Keep BOUND and EOF as two observable protocol steps: only a successful
      // write callback authorizes the local acknowledgement flag and closes
      // stdin. A write error stays behind the pre-exec gate and enters the
      // ordinary fail-closed termination/settlement path.
      job.child.stdin.write(binding, bindingFlushed);
    }
    return true;
  } catch (error) {
    job.spawnError = job.spawnError || error;
    if (job.windowsJobMode) {
      job.windowsJobUnverified = true;
      terminateJob(job, 'SIGKILL');
    } else {
      try { job.child.stdin.end(); } catch (stdinError) {}
    }
    return false;
  }
}
function terminateJob(job, signal) {
  if (!job || !job.child) return;
  if (!job.windowsJobMode) {
    // The POSIX wrapper is the containment proof. Never SIGKILL that proof:
    // it catches TERM, drains adopted Linux descendants (including setsid),
    // or owns the macOS exact-generation scan, and exits only when containment
    // is empty. If draining cannot be proven it deliberately remains alive.
    if (exactChildStillOwned(job)) {
      if (job.posixTerminationSent) return;
      job.posixTerminationSent = true;
      try { process.kill(-job.child.pid, 'SIGTERM'); }
      catch (error) { if (!error || error.code !== 'ESRCH') job.treeSignalUnverified = true; }
      return;
    }
    // If an external SIGKILL removed the wrapper, its PGID is no longer an
    // exact signalling authority. The durably bound direct model generation
    // remains safe to address by PID after a fresh generation check.
    if (exactModelStillOwned(job)) {
      var directSignal = signal === 'SIGKILL' ? 'SIGKILL' : 'SIGTERM';
      if (job.modelTerminationSent === directSignal) return;
      job.modelTerminationSent = directSignal;
      try { process.kill(job.modelPid, directSignal); }
      catch (modelError) { if (!modelError || modelError.code !== 'ESRCH') job.treeSignalUnverified = true; }
      return;
    }
    job.treeSignalUnverified = true;
    return;
  }
  if (job.windowsTerminationSent) return;
  job.windowsTerminationSent = true;
  try {
    if (job.child.stdin && job.child.stdin.writable) job.child.stdin.end('TERMINATE\n');
  } catch (error) {
    job.windowsJobUnverified = true;
  }
}
function publishIfCurrent(job, value, eventName, eventData) {
  return withStemLock(job.stem, function () {
    if (job.child) {
      // On Darwin a bound model may have escaped the wrapper's original PGID.
      // Exact direct-model death is therefore necessary but not sufficient:
      // only the authenticated wrapper DRAINED receipt proves that the pinned
      // executable has no surviving generation. Never turn a wrapper crash
      // into a terminal advisory result while that containment proof is lost.
      if (process.platform === 'darwin' && !job.windowsJobMode &&
          job.modelBindingState === 'bound' &&
          (job.posixControlUnverified === true || job.posixControlTerminal !== 'drained')) {
        appendEvent('shallow-intake-stale', {
          stem: job.stem, sourceHash: job.sourceHash, requestId: job.requestId,
          reasonCode: 'containment-drain-unverified'
        });
        return false;
      }
      var authenticatedEmpty = value && value.status === 'failed' &&
        (job.windowsJobMode ? job.windowsJobDrained === true && job.windowsJobUnverified !== true :
          job.posixControlUnverified !== true &&
            (job.posixControlTerminal === 'empty' || job.posixControlTerminal === 'drained'));
      if (job.modelBindingState !== 'bound' && !authenticatedEmpty ||
          job.modelBindingState === 'bound' && exactModelStillOwned(job)) {
        appendEvent('shallow-intake-stale', {
          stem: job.stem, sourceHash: job.sourceHash, requestId: job.requestId,
          reasonCode: job.modelBindingState === 'bound' ? 'model-generation-still-live' : 'model-generation-unbound'
        });
        return false;
      }
    }
    var ownerState = workerHandleState(job.workerHandle, job);
    if (ownerState !== 'match') {
      appendEvent('shallow-intake-stale', {
        stem: job.stem,
        sourceHash: job.sourceHash,
        requestId: job.requestId,
        reasonCode: ownerState === 'different' ? 'worker-generation-changed' : 'worker-proof-unavailable'
      });
      return false;
    }
    var current = readRecord(job.stem), source = sourceState(job.stem);
    if (!current || current.requestId !== job.requestId || current.sourceHash !== job.sourceHash || current.status !== 'checking' ||
        !source.eligible || source.sourceHash !== job.sourceHash) {
      appendEvent('shallow-intake-stale', { stem: job.stem, sourceHash: job.sourceHash, requestId: job.requestId, reasonCode: source.reason || 'generation-changed' });
      return false;
    }
    writeRecord(job.stem, value);
    appendEvent(eventName, Object.assign({ stem: job.stem, sourceHash: job.sourceHash, requestId: job.requestId, attempt: job.attempt }, eventData || {}));
    return true;
  });
}
function finishJob(job) {
  if (job.finished) return;
  job.finished = true;
  if (job.timer) clearTimeout(job.timer);
  delete jobs[job.stem];
  var deadline = Date.now() + 5000;
  function releaseLocalSlot() {
    if (job.slotReleased) return;
    job.slotReleased = true;
    active = Math.max(0, active - 1); schedulePump();
  }
  (function prove() {
    if (processTreeGone(job)) {
      if (job.deferredFailure) {
        var deferred = job.deferredFailure;
        job.deferredFailure = null;
        try { publishIfCurrent(job, deferred.value, deferred.eventName, deferred.eventData); }
        catch (deferredError) {}
      }
      var handleState = workerHandleState(job.workerHandle, job);
      if (handleState === 'different') {
        if (!job.releaseWarned) {
          job.releaseWarned = true;
          appendEvent('shallow-intake-worker-retained', {
            stem: job.stem, requestId: job.requestId, reasonCode: 'owner-generation-changed'
          });
        }
        // Never clean or release through a handle which no longer owns the
        // public generation. The process tree is gone, so only this process's
        // local slot may be released.
        releaseLocalSlot();
        return;
      }
      if (handleState === 'unavailable') {
        if (!job.releaseWarned) {
          job.releaseWarned = true;
          appendEvent('shallow-intake-worker-retained', {
            stem: job.stem, requestId: job.requestId, reasonCode: 'owner-proof-unavailable'
          });
        }
        // An inspector/transport failure is not proof that another generation
        // owns the name. Keep the exact token in this closure and retry. The
        // local slot may be released after the bounded wait; the durable owner
        // still keeps every process globally fail-closed.
        if (Date.now() >= deadline) releaseLocalSlot();
        var proofRetry = setTimeout(prove, 250);
        if (typeof proofRetry.unref === 'function') proofRetry.unref();
        return;
      }
      if (!job.scratchCleanupAttempted) {
        job.scratchCleanupAttempted = true;
        var cleanup = cleanupScratchDirectory(job.scratch);
        if (!cleanup.ok) scratchRecoveryEvent(cleanup.code, job.requestId);
      }
      var released = false;
      try { released = releaseWorker(job.workerHandle, job) === true; }
      catch (error) { released = false; }
      if (!released) {
        if (!job.releaseWarned) {
          job.releaseWarned = true;
          appendEvent('shallow-intake-worker-retained', {
            stem: job.stem, requestId: job.requestId, reasonCode: 'owner-release-failed'
          });
        }
        // Retry only while the exact same owner generation is still public.
        // A changed/missing owner must never be touched through this handle.
        var releaseState = workerHandleState(job.workerHandle, job);
        if (releaseState === 'match' || releaseState === 'unavailable') {
          if (Date.now() >= deadline) releaseLocalSlot();
          var releaseRetry = setTimeout(prove, 250);
          if (typeof releaseRetry.unref === 'function') releaseRetry.unref();
          return;
        }
      }
      releaseLocalSlot();
      return;
    }
    if (!job.treeKillSent && (exactChildStillOwned(job) || exactModelStillOwned(job))) {
      job.treeKillSent = true; terminateJob(job, 'SIGKILL');
    }
    if (Date.now() >= deadline) {
      // The durable global worker record keeps every site process fail-closed.
      // Startup reconciliation may remove it only after TERM/KILL/ESRCH proof.
      if (!job.treeWarned) appendEvent('shallow-intake-worker-retained', { stem: job.stem, requestId: job.requestId, reasonCode: 'process-tree-unverified' });
      job.treeWarned = true;
      releaseLocalSlot();
      // Keep the exact token in this closure and continue ESRCH proof. The
      // owner PID is this still-live site, so abandoning the handle here would
      // wedge the global slot until restart even if the descendant exits at
      // 5.1 seconds.
      if (exactChildStillOwned(job) || exactModelStillOwned(job)) terminateJob(job, 'SIGKILL');
    }
    var timer = setTimeout(prove, job.treeWarned ? 250 : 50);
    if (typeof timer.unref === 'function') timer.unref();
  })();
}
function failJob(job, code, retryable) {
  var value = baseRecord(job.stem, job.sourceHash, 'failed', job.requestId, job.attempt, job.createdAt);
  value.errorCode = bounded(code || 'MODEL_FAILED', 80); value.retryable = retryable !== false;
  var eventData = { errorCode: value.errorCode, durationMs: Date.now() - job.startedMs };
  if (job.child && job.modelBindingState === 'bound' && exactModelStillOwned(job)) {
    // Wrapper exit is not publication authority. Preserve the source-bound
    // failure in memory and publish it only after exact model death; a site
    // crash merely loses advisory output while the durable owner stays closed.
    job.deferredFailure = { value: value, eventName: 'shallow-intake-failed', eventData: eventData };
  } else {
    try { publishIfCurrent(job, value, 'shallow-intake-failed', eventData); }
    catch (error) {}
  }
  finishJob(job);
}
function failQueued(item, error) {
  try {
    return withStemLock(item.stem, function () {
      var current = readRecord(item.stem);
      if (!current || current.requestId !== item.requestId || current.sourceHash !== item.sourceHash || current.status !== 'queued') return false;
      var failed = failedRecord(item.stem, item.sourceHash, item.requestId, item.attempt, current.createdAt, error);
      writeRecord(item.stem, failed);
      appendEvent('shallow-intake-failed', { stem: item.stem, sourceHash: item.sourceHash, requestId: item.requestId,
        attempt: item.attempt, errorCode: failed.errorCode, durationMs: 0 });
      return true;
    });
  } catch (persistError) { return false; }
}
function settleUnstartedWorker(handle, item, scratch) {
  var state = workerHandleState(handle, item);
  if (state === 'different') return false;
  // Reuse the same owner-release state machine as a spawned job. `active` is
  // still zero, so releaseLocalSlot's saturating decrement is harmless; its
  // pump wakeup is required if release needs an asynchronous retry.
  finishJob({
    stem: item.stem, sourceHash: item.sourceHash, requestId: item.requestId,
    attempt: item.attempt, workerHandle: handle,
    scratch: scratch || scratchDirectoryFor(item.requestId), child: null,
    finished: false, slotReleased: false
  });
  return state === 'match';
}
function recordFailure(stem, error) {
  if (!taskIndexSource.safeTaskStem(stem)) return null;
  try {
    var raw = readRegular(path.join(paths.TASKS_DIR, 'backlog', stem + '.md'), TASK_STORAGE_MAX, false);
    if (!raw) return null;
    var text = raw.toString('utf8');
    if (!Buffer.from(text, 'utf8').equals(raw) || text.indexOf('\u0000') >= 0) return null;
    var hash = sha256(raw);
    return withStemLock(stem, function () {
      var current = readRecord(stem);
      if (current && current.sourceHash === hash && ['complete', 'dismissed', 'superseded'].includes(current.status)) return current;
      var attempt = current && current.sourceHash === hash ? current.attempt : 1;
      var failed = failedRecord(stem, hash, null, attempt, current && current.createdAt, error);
      writeRecord(stem, failed);
      appendEvent('shallow-intake-failed', { stem: stem, sourceHash: hash, requestId: failed.requestId,
        attempt: attempt, errorCode: failed.errorCode, durationMs: 0 });
      return failed;
    });
  } catch (persistError) { return null; }
}
function writePrompt(file, text) {
  var bytes = Buffer.from(text, 'utf8');
  if (bytes.length > 128 * 1024) throw codedError('CONTEXT_TOO_LARGE', 'model prompt exceeds its transport budget');
  var directory = path.dirname(file), root = managedRoot(directory);
  var published = root && fileGuards.publishNoClobberRegularFileUnder(root, directory, file, bytes, {
    create: false, mode: 0o600, maxBytes: 128 * 1024
  });
  if (!published || !published.ok) throw codedError('CONTEXT_TRANSPORT_UNSAFE', 'model prompt publication failed closed');
  var privatePrompt = ensurePublishedPrivateFile(
    file, 128 * 1024, bytes, 'CONTEXT_TRANSPORT_UNSAFE');
  return { sha256: sha256(bytes), stat: privatePrompt.stat };
}
function launch(item) {
  try { ensureSafeScratch(); }
  catch (scratchSafetyFailure) { failQueued(item, scratchSafetyFailure); return 'drop'; }
  var source;
  try { source = sourceState(item.stem); }
  catch (error) { failQueued(item, error); return 'drop'; }
  if (!source.eligible || source.sourceHash !== item.sourceHash) return 'drop';
  var prepared, checking;
  try {
    prepared = contract.prepareRequest(buildContext(item.stem, source));
  } catch (prepareError) { failQueued(item, prepareError); return 'drop'; }
  var workerHandle;
  try { workerHandle = acquireWorker(item); }
  catch (workerError) { failQueued(item, workerError); return 'drop'; }
  if (!workerHandle) return 'defer';
  var scratch;
  try { scratch = prepareScratchDirectory(workerHandle, item); }
  catch (scratchPreparationError) {
    settleUnstartedWorker(workerHandle, item, scratch);
    if (scratchPreparationError && scratchPreparationError.code === 'INTAKE_WORKER_CHANGED') return 'defer';
    failQueued(item, scratchPreparationError);
    return 'drop';
  }
  try {
    checking = withStemLock(item.stem, function () {
      var current = readRecord(item.stem);
      if (!current || current.requestId !== item.requestId || current.sourceHash !== item.sourceHash || current.status !== 'queued') return null;
      var live = sourceState(item.stem);
      if (!live.eligible || live.sourceHash !== item.sourceHash) return null;
      current = baseRecord(item.stem, item.sourceHash, 'checking', item.requestId, item.attempt, current.createdAt);
      return writeRecord(item.stem, current);
    });
  } catch (error2) {
    settleUnstartedWorker(workerHandle, item, scratch);
    failQueued(item, error2); return 'drop';
  }
  if (!checking) {
    settleUnstartedWorker(workerHandle, item, scratch);
    return 'drop';
  }
  var promptFile = path.join(scratch, 'prompt.txt'), promptPublication;
  try { promptPublication = writePrompt(promptFile, prepared.prompt); }
  catch (promptError) {
    var promptJob = { stem: item.stem, sourceHash: item.sourceHash, requestId: item.requestId, attempt: item.attempt,
      createdAt: checking.createdAt, startedMs: Date.now(), workerHandle: workerHandle, scratch: scratch, child: null };
    failJob(promptJob, promptError.code || 'CONTEXT_TOO_LARGE', retryableError(promptError)); return 'drop';
  }
  var command = process.env.SHALLOW_INTAKE_CLAUDE || 'claude';
  var args = ['-p', '--safe-mode', '--no-session-persistence', '--disable-slash-commands', '--tools', '',
    '--system-prompt', 'You are a bounded JSON classifier. Use only the user prompt as data and instructions. Do not inspect the environment, repository, memory, tools, files, network, or shell. Return only the requested schema-bound object.',
    '--output-format', 'json', '--json-schema', JSON.stringify(prepared.cliSchema), '--effort', 'low'];
  // Containment scripts and their interpreter are fixed production authority.
  // A configurable wrapper could claim a drained PGID/Job while leaving an
  // escaped descendant alive. Only the explicit non-Windows Job simulation
  // used by this module's regression test may substitute them.
  var wrapper = path.join(paths.ORCHESTRATOR_DIR, 'tasks', 'intake-model-wrapper.py');
  var python = 'python3';
  var useWindowsJob = windowsJobMode();
  var controlNonce = crypto.randomBytes(24).toString('hex');
  if (useWindowsJob) {
    var simulatedWindowsJob = process.platform !== 'win32' && process.env.SHALLOW_INTAKE_TEST_WINDOWS_JOB === '1';
    wrapper = simulatedWindowsJob && process.env.SHALLOW_INTAKE_WINDOWS_JOB_WRAPPER ||
      path.join(paths.ORCHESTRATOR_DIR, 'tasks', 'intake-windows-job.py');
    python = simulatedWindowsJob && process.env.SHALLOW_INTAKE_WINDOWS_JOB_PYTHON || 'python';
  }
  var promptStat = promptPublication.stat;
  var wrapperArgs = [wrapper, '--token', workerHandle.token, '--prompt', promptFile,
    '--prompt-sha256', promptPublication.sha256,
    '--prompt-dev', String(promptStat.dev), '--prompt-ino', String(promptStat.ino),
    '--prompt-mode', String(promptStat.modeExact || promptStat.mode), '--prompt-nlink', String(promptStat.nlink),
    '--prompt-size', String(promptStat.sizeExact || promptStat.size), '--prompt-mtime-ns', String(promptStat.mtimeNs),
    '--prompt-ctime-ns', String(promptStat.ctimeNs),
    '--timeout-ms', String(timeoutMs), '--', command].concat(args);
  var child;
  var workerEnv = modelEnv();
  if (useWindowsJob) {
    workerEnv.SHALLOW_INTAKE_JOB_NONCE = controlNonce;
    workerEnv.SHALLOW_INTAKE_JOB_CONTROL_FD = '3';
  } else {
    workerEnv.SHALLOW_INTAKE_POSIX_CONTROL_NONCE = controlNonce;
    workerEnv.SHALLOW_INTAKE_POSIX_CONTROL_FD = '3';
  }
  try { child = cp.spawn(python, wrapperArgs, {
    cwd: scratch, env: workerEnv, detached: !useWindowsJob,
    stdio: ['pipe', 'pipe', 'pipe', 'pipe']
  }); }
  catch (spawnError) {
    var fake = { stem: item.stem, sourceHash: item.sourceHash, requestId: item.requestId, attempt: item.attempt, createdAt: checking.createdAt,
      startedMs: Date.now(), workerHandle: workerHandle, scratch: scratch, child: null };
    failJob(fake, 'CLI_UNAVAILABLE', false); return;
  }
  var job = { stem: item.stem, sourceHash: item.sourceHash, requestId: item.requestId, attempt: item.attempt, createdAt: checking.createdAt,
    child: child, workerHandle: workerHandle, scratch: scratch, context: prepared.validationContext, stdout: Buffer.alloc(0),
    stderrBytes: 0, stderrSample: Buffer.alloc(0),
    startedMs: Date.now(), overflow: false, timedOut: false, finished: false,
    modelPid: null, modelProcessStartId: null, modelBindingState: 'prebind', modelBindingAcknowledged: false,
    reportedModelPid: null, workerControlNonce: controlNonce, workerControlBuffer: '',
    posixControlReady: false, posixControlTerminal: null, posixControlUnverified: false,
    windowsJobMode: useWindowsJob, windowsJobReady: false, windowsJobDrained: false,
    windowsJobUnverified: false, windowsJobTerminal: null, windowsTerminationSent: false };
  jobs[item.stem] = job; active++;
  child.stdin.on('error', function (error) {
    if (job.finished) return;
    error.code = 'MODEL_STDIN_FAILED'; job.spawnError = job.spawnError || error;
    if (job.windowsJobMode) job.windowsJobUnverified = true;
    else terminateJob(job, 'SIGKILL');
  });
  child.stdout.on('data', function (chunk) {
    if (job.stdout.length + chunk.length > contract.LIMITS.claudeEnvelopeBytes) { job.overflow = true; terminateJob(job, 'SIGKILL'); return; }
    job.stdout = Buffer.concat([job.stdout, chunk]);
  });
  child.stderr.on('data', function (chunk) { appendWorkerStderr(job, chunk); });
  if (child.stdio[3]) {
    child.stdio[3].on('data', function (chunk) { appendWorkerControl(job, chunk); });
    child.stdio[3].on('error', function () {
      if (job.windowsJobMode) job.windowsJobUnverified = true;
      else job.posixControlUnverified = true;
    });
  } else {
    job.spawnError = codedError('MODEL_JOB_CONTROL_FAILED', 'model identity control pipe is unavailable');
    terminateJob(job, 'SIGKILL');
  }
  child.on('error', function (error) { job.spawnError = error; });
  child.on('exit', function (exitCode, exitSignal) {
    job.childExited = true;
    job.childExitCode = exitCode;
    job.childExitSignal = exitSignal;
    // `close` waits for every inherited stdout/stderr descriptor. A direct
    // model orphan keeps those pipes open after an abrupt wrapper SIGKILL, so
    // wrapper exit itself must trigger the exact-generation kill. Publication
    // still waits for the later close + proven model death.
    if (!job.windowsJobMode) {
      job.childClosed = true;
      if (exactModelStillOwned(job)) terminateJob(job, 'SIGKILL');
    }
  });
  child.on('close', function (exitCode) {
    if (job.finished) return;
    job.childClosed = true;
    if (job.workerControlBuffer) {
      if (job.windowsJobMode) job.windowsJobUnverified = true;
      else job.posixControlUnverified = true;
      job.workerControlBuffer = '';
    }
    if (job.cancelled) { finishJob(job); return; }
    if (job.timedOut) { failJob(job, 'MODEL_TIMEOUT', true); return; }
    if (job.overflow) { failJob(job, 'MODEL_OUTPUT_TOO_LARGE', false); return; }
    if (job.spawnError) { failJob(job, job.spawnError.code === 'ENOENT' ? 'CLI_UNAVAILABLE' : 'MODEL_SPAWN_FAILED', false); return; }
    if (exitCode === 73) { failJob(job, 'MODEL_TIMEOUT', true); return; }
    if (exitCode === 74) { failJob(job, 'CLI_UNAVAILABLE', false); return; }
    if (exitCode !== 0) {
      var processFailure = modelProcessFailure(job);
      failJob(job, processFailure.code, processFailure.retryable); return;
    }
    try {
      var model = contract.parseClaudeEnvelope(job.stdout, job.context);
      var duration = Math.max(0, Date.now() - job.startedMs);
      var complete = contract.createCompleteResult(model, {
        stem: job.stem, sourceHash: job.sourceHash, createdAt: job.createdAt, requestId: job.requestId,
        attempt: job.attempt, modelDurationMs: duration, resultBytes: job.stdout.length
      }, job.context);
      publishIfCurrent(job, complete, 'shallow-intake-completed', {
        durationMs: duration, resultBytes: job.stdout.length, duplicateCount: complete.possibleDuplicates.length
      });
      finishJob(job);
    } catch (error) { failJob(job, error && error.code || 'SCHEMA_INVALID', false); }
  });
  try {
    var attachedWorker = attachWorkerChild(workerHandle, child.pid);
    job.childProcessStartId = attachedWorker.childProcessStartId;
    appendEvent('shallow-intake-started', { stem: item.stem, sourceHash: item.sourceHash, requestId: item.requestId, attempt: item.attempt });
    child.stdin.write('GO ' + workerHandle.token + '\n');
  } catch (attachError) {
    job.spawnError = attachError;
    try { child.stdin.end(); } catch (stdinCloseError) {}
    terminateJob(job, 'SIGKILL');
  }
  job.timer = setTimeout(function () {
    if (job.finished) return;
    job.timedOut = true; terminateJob(job, 'SIGTERM');
  }, timeoutMs);
  if (typeof job.timer.unref === 'function') job.timer.unref();
  return 'started';
}
function pump() {
  while (active < concurrency && queue.length) {
    var item = queue.shift(); delete queued[queueKey(item.stem, item.requestId)];
    if (jobs[item.stem]) {
      queued[queueKey(item.stem, item.requestId)] = true; queue.unshift(item);
      setTimeout(schedulePump, 100).unref(); break;
    }
    if (launch(item) === 'defer') {
      queued[queueKey(item.stem, item.requestId)] = true; queue.unshift(item);
      setTimeout(schedulePump, 100).unref(); break;
    }
  }
}
function cancelLocal(stem) {
  queue = queue.filter(function (item) {
    if (item.stem === stem) { delete queued[queueKey(item.stem, item.requestId)]; return false; }
    return true;
  });
  var job = jobs[stem];
  if (job) {
    job.cancelled = true; terminateJob(job, 'SIGTERM');
  }
}
function supersede(stem, reason, options) {
  options = options || {};
  if (!taskIndexSource.safeTaskStem(stem)) return null;
  cancelLocal(stem);
  return withStemLock(stem, function () {
    var current = readRecord(stem);
    if (options.onlyIfIneligible) {
      var liveSource = sourceState(stem);
      if (liveSource.eligible) return current;
    }
    if (!current || current.status === 'superseded') return current;
    var next = baseRecord(stem, current.sourceHash, 'superseded', randomId(), current.attempt, current.createdAt);
    next.reasonCode = bounded(reason || 'authoritative-work', 80);
    writeRecord(stem, next);
    appendEvent('shallow-intake-superseded', { stem: stem, sourceHash: current.sourceHash, requestId: current.requestId, reasonCode: next.reasonCode });
    return next;
  });
}
function retry(stem, expectedHash) {
  var source = sourceState(stem);
  if (!HASH_RE.test(String(expectedHash || '')) || !source.eligible || source.sourceHash !== expectedHash) throw codedError('source-changed', 'task source changed', 409);
  return schedule(stem, 'manual-retry', { force: true });
}
function dismiss(stem, expectedHash) {
  var source = sourceState(stem);
  if (!HASH_RE.test(String(expectedHash || '')) || !source.eligible || source.sourceHash !== expectedHash) throw codedError('source-changed', 'task source changed', 409);
  cancelLocal(stem);
  return withStemLock(stem, function () {
    var current = readRecord(stem);
    if (!current || current.sourceHash !== expectedHash) throw codedError('source-changed', 'task preview changed', 409);
    var next = baseRecord(stem, expectedHash, 'dismissed', randomId(), current.attempt, current.createdAt);
    writeRecord(stem, next); appendEvent('shallow-intake-superseded', { stem: stem, sourceHash: expectedHash, requestId: current.requestId, reasonCode: 'dismissed' });
    return next;
  });
}
function publicErrorCode(error, fallback) {
  if (error && error.code === 'bad-json') return 'bad-json';
  if (error && error.code === 'BAD_STEM') return 'bad-stem';
  if (error && error.code === 'source-changed') return 'source-changed';
  return fallback;
}
function publicFailureCode(code) {
  code = String(code || '');
  if (code === 'MODEL_KEYCHAIN_SANDBOX_INCOMPATIBLE' ||
      code === 'MODEL_SCHEMA_TRANSPORT_INCOMPATIBLE') return code;
  if (code === 'CONTEXT_TOO_LARGE') return 'INTAKE_CONTEXT_TOO_LARGE';
  if (['CONTEXT_INVALID', 'TASK_UTF8_INVALID', 'INDEX_INVALID'].includes(code)) {
    return 'INTAKE_CONTEXT_INVALID';
  }
  if (['INTAKE_BUSY', 'INTAKE_QUEUE_FULL', 'INTAKE_LOCK_LOST', 'INTAKE_WORKER_CHANGED'].includes(code)) {
    return 'INTAKE_BUSY';
  }
  if (code === 'CLI_UNAVAILABLE') return 'INTAKE_CLI_UNAVAILABLE';
  if (code === 'MODEL_TIMEOUT') return 'INTAKE_MODEL_TIMEOUT';
  if (['MODEL_OUTPUT_TOO_LARGE', 'ENVELOPE_TOO_LARGE'].includes(code)) {
    return 'INTAKE_MODEL_OUTPUT_TOO_LARGE';
  }
  if (['INVALID_ENVELOPE', 'INVALID_JSON', 'SCHEMA_INVALID', 'SCHEMA_LOAD_FAILED',
    'COHERENCE_INVALID', 'GROUNDING_INVALID', 'DUPLICATE_NOT_ALLOWED',
    'RESULT_INVALID', 'METADATA_INVALID'].includes(code)) {
    return 'INTAKE_MODEL_OUTPUT_INVALID';
  }
  if (['MODEL_PROCESS_FAILED', 'MODEL_SPAWN_FAILED', 'MODEL_STDIN_FAILED',
    'MODEL_JOB_CONTROL_FAILED'].includes(code)) {
    return 'INTAKE_MODEL_FAILED';
  }
  if (/^INTAKE_[A-Z0-9_]+$/.test(code) || code === 'TASK_STATE_UNSAFE' ||
      code === 'CONTEXT_TRANSPORT_UNSAFE') {
    return 'INTAKE_RUNTIME_UNAVAILABLE';
  }
  return 'INTAKE_FAILED';
}
function publicProjection(value) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return null;
  if (value.status !== 'failed') return value;
  return {
    version: value.version === 1 ? 1 : undefined,
    stem: typeof value.stem === 'string' ? value.stem : undefined,
    sourceHash: typeof value.sourceHash === 'string' ? value.sourceHash : undefined,
    createdAt: typeof value.createdAt === 'string' ? value.createdAt : undefined,
    status: 'failed',
    requestId: typeof value.requestId === 'string' ? value.requestId : undefined,
    attempt: Number.isInteger(value.attempt) ? value.attempt : undefined,
    errorCode: publicFailureCode(value.errorCode),
    retryable: value.retryable !== false
  };
}
function reconcile() {
  ensureSafeScratch();
  var names, activeStems;
  try { names = boundedDirectoryNames(DIR); } catch (error) { return; }
  names.filter(isResultName).forEach(function (name) {
    var stem = name.slice(0, -5), current, source;
    try { source = sourceState(stem); }
    catch (sourceError) { return; }
    try { current = readRecord(stem); }
    catch (recordError) { quarantineInvalidRecord(stem, source, recordError); return; }
    if (!current) return;
    if (!source.eligible) {
      if (current.status !== 'superseded') try {
        supersede(stem, source.reason, { onlyIfIneligible: true });
      } catch (error2) {}
      return;
    }
    if (current.sourceHash !== source.sourceHash) { try { schedule(stem, 'source-changed', { force: true }); } catch (error3) {} return; }
    if (current.status === 'superseded') { try { schedule(stem, 'eligibility-restored'); } catch (restoredError) {} return; }
    if (current.status === 'complete') {
      try {
        if (!activeStems) activeStems = activeStemSet();
        projectCompleteRecord(stem, current, source, activeStems);
      } catch (completeError) {
        if (completeError && completeError.code !== 'INTAKE_RESULT_INVALID') completeError = codedError('INTAKE_RESULT_INVALID');
        quarantineInvalidRecord(stem, source, completeError);
      }
      return;
    }
    if (current.status === 'queued') {
      try { enqueue(current, 'restart-recovery'); }
      catch (queueError) { failQueued(current, queueError); }
    }
    if (current.status === 'checking' && !jobs[stem]) {
      var worker;
      try { worker = workerState(); } catch (error4) { return; }
      var exactWorker = worker && worker.stem === stem && worker.sourceHash === current.sourceHash && worker.requestId === current.requestId;
      if (!exactWorker) try { schedule(stem, 'restart-recovery', { force: true }); } catch (error5) {}
    }
  });
}
function activeStemSet() {
  var index = readIndex(), out = Object.create(null);
  ['backlog', 'pending', 'todo'].forEach(function (column) {
    index[column].forEach(function (entry) {
      if (entry && taskIndexSource.safeTaskStem(entry.stem) && activeTaskArtifact(entry.stem, column)) {
        out[entry.stem] = column;
      }
    });
  });
  return out;
}
function storedValidationContext(stem, source, value, activeStems) {
  var candidates = [];
  value.possibleDuplicates.forEach(function (row) {
    var column = activeStems[row.stem];
    if (!column) return;
    var artifact = activeTaskArtifact(row.stem, column);
    if (!artifact) return;
    var text = artifact.bytes.toString('utf8');
    candidates.push({
      stem: row.stem,
      title: titleOfTask(text, row.stem),
      goalExcerpt: goalExcerpt(text),
      column: column
    });
  });
  return { stem: stem, taskText: source.text, candidates: candidates, projectFlags: projectFlags() };
}
function projectCompleteRecord(stem, value, source, activeStems) {
  if (!Array.isArray(value.possibleDuplicates)) throw codedError('INTAKE_RESULT_INVALID', 'stored complete intake result is invalid');
  activeStems = activeStems || activeStemSet();
  var filtered = value.possibleDuplicates.filter(function (row) { return row && activeStems[row.stem]; });
  var projected = value;
  if (filtered.length !== value.possibleDuplicates.length) {
    projected = JSON.parse(JSON.stringify(value));
    projected.possibleDuplicates = filtered;
    projected.readiness = filtered.length ? 'possible-duplicate' : (projected.missingContext && projected.missingContext.length ? 'needs-context' : 'ready');
  }
  // Revalidation needs only the at-most-five referenced duplicates. Rebuilding
  // the full active-task candidate catalog once per visible preview would turn
  // every SSE poll into O(previews × tasks) task-file reads.
  try { return contract.validateCompleteResult(projected, storedValidationContext(stem, source, projected, activeStems)); }
  catch (error) { throw codedError('INTAKE_RESULT_INVALID', 'stored complete intake result is invalid'); }
}
function snapshot() {
  var out = Object.create(null), names, activeStems;
  try { names = boundedDirectoryNames(DIR); } catch (error) { return out; }
  names.filter(isResultName).forEach(function (name) {
    var stem = name.slice(0, -5), value, source;
    try { value = readRecord(stem); }
    catch (error) { return; }
    if (!value || value.status === 'dismissed' || value.status === 'superseded') return;
    try { source = sourceState(stem); }
    catch (error2) { return; }
    if (!source.eligible || source.sourceHash !== value.sourceHash) return;
    if (value.status === 'complete') {
      // The result was schema/grounding validated before durable publication.
      // Candidate activity can drift independently of the target source; hide
      // only links that no longer exist instead of discarding the whole preview.
      try { if (!activeStems) activeStems = activeStemSet(); }
      catch (error3) { return; }
      // Re-validate the exact durable result at the read boundary.  Publication
      // already validates grounding against its generation context; this pass
      // additionally prevents later cache corruption from reaching the UI.
      // A removed duplicate is filtered first because that independent drift is
      // an expected advisory condition, not corruption of the target task.
      try { value = projectCompleteRecord(stem, value, source, activeStems); }
      catch (error4) { return; }
    }
    out[stem] = publicProjection(value);
  });
  return out;
}
function ensureSafeScratch() {
  if (scratchSafetyError) throw scratchSafetyError;
  try {
    var runtimeSnapshot = ensureDir(DIR), stemLocksSnapshot = ensureDir(STEM_LOCKS_DIR);
    if (!privateScratchDirectory(runtimeSnapshot, path.resolve(DIR)) ||
        !privateScratchDirectory(stemLocksSnapshot, path.resolve(STEM_LOCKS_DIR))) {
      throw codedError('INTAKE_DIR_UNSAFE', 'shallow-intake runtime and lock directories must be owned and private');
    }
    var rootResolved = path.resolve(paths.PROJECT_ROOT), scratchResolved = path.resolve(SCRATCH_DIR);
    var lexical = path.relative(rootResolved, scratchResolved);
    if (lexical === '' || lexical.split(path.sep)[0] !== '..') {
      throw codedError('INTAKE_SCRATCH_UNSAFE', 'shallow-intake scratch must be outside the project repository');
    }
    var scratchRootSnapshot;
    try { scratchRootSnapshot = ensureDir(SCRATCH_DIR); }
    catch (scratchDirectoryError) {
      throw codedError('INTAKE_SCRATCH_UNSAFE', String(scratchDirectoryError && scratchDirectoryError.message || scratchDirectoryError));
    }
    if (!privateScratchDirectory(scratchRootSnapshot, path.resolve(SCRATCH_DIR))) {
      throw codedError('INTAKE_SCRATCH_UNSAFE', 'shallow-intake scratch root is not owned and private');
    }
    var rootReal = fs.realpathSync(paths.PROJECT_ROOT), scratchReal = fs.realpathSync(SCRATCH_DIR);
    var relative = path.relative(rootReal, scratchReal);
    if (relative === '' || relative.split(path.sep)[0] !== '..') {
      throw codedError('INTAKE_SCRATCH_UNSAFE', 'shallow-intake scratch must resolve outside the project repository');
    }
  } catch (error) {
    scratchSafetyError = codedError(error && error.code || 'INTAKE_SCRATCH_UNSAFE', String(error && error.message || error));
    throw scratchSafetyError;
  }
}
function init() {
  ensureSafeScratch();
  try { workerState(); } catch (error) { appendEvent('shallow-intake-worker-retained', { reasonCode: bounded(error.code || 'worker-invalid', 80) }); }
  reconcile();
}
function dirMtime() {
  try {
    var chain = safeDirectory(DIR, { allowMissing: true });
    return chain.exists ? chain.stat.mtimeMs : 0;
  } catch (error) { return 0; }
}
function killAll() {
  queue = []; queued = Object.create(null);
  Object.keys(jobs).forEach(function (stem) { jobs[stem].cancelled = true; terminateJob(jobs[stem], 'SIGTERM'); });
}

function intakeIntegrityFinding(code, stem, file, message) {
  return { code: code, severity: 'error', stem: stem || null, paths: file ? [file] : [], message: message,
    recovery: 'Recover the exact shallow-intake result/owner generation through the intake controller; do not infer owner death from age.' };
}
function integrityRead(file, maxBytes) {
  var raw = integrityRawRead(file, maxBytes);
  if (!raw.bytes) return raw;
  var value;
  try { value = JSON.parse(raw.bytes.toString('utf8')); } catch (error) { value = null; }
  var canonical = value !== null && Buffer.from(JSON.stringify(value) + '\n', 'utf8').equals(raw.bytes);
  return { bytes: raw.bytes, value: value, canonical: canonical, stat: raw.stat };
}
function integrityRawRead(file, maxBytes) {
  var directory = path.dirname(file), root = managedRoot(directory);
  if (!root) return { unsafe: true };
  var read = fileGuards.boundedRegularFileUnder(root, directory, file, maxBytes);
  if (!exactPrivateRegularFile(file, read)) {
    var entry = fileGuards.inspectEntryUnder(root, directory, file);
    return { missing: !!entry && entry.status === 'missing', unsafe: !entry || entry.status !== 'missing' };
  }
  return { bytes: read.bytes, stat: read.stat };
}
function scanIntegrity(scope) {
  var stem = typeof scope === 'string' ? scope : scope && scope.stem || null;
  var out = { version: 1, owner: 'shallow-intake', statuses: [], findings: [], snapshotInputs: [], truncated: false };
  var artifactBudget = MAX_INTEGRITY_ARTIFACTS;
  var guardEvidenceBytesRemaining = GUARD_EVIDENCE_TOTAL_MAX;
  var processProofCache = Object.create(null), processProofCount = 0;
  var processProofDeadline = Date.now() + MAX_INTEGRITY_PROCESS_PROOF_MS;
  var processProofBudgetFinding = false;
  if (stem !== null && !taskIndexSource.safeTaskStem(stem)) {
    out.findings.push(intakeIntegrityFinding('INTAKE_SCOPE_INVALID', null, null,
      'Shallow-intake integrity scope must be one canonical task stem.'));
    return out;
  }
  var evidenceSeen = Object.create(null);
  var artifactBudgetFinding = false;
  var guardEvidenceBudgetFinding = false;
  function spendArtifactBudget(file) {
    if (artifactBudget > 0) { artifactBudget--; return true; }
    if (!artifactBudgetFinding) {
      artifactBudgetFinding = true;
      out.truncated = true;
      out.findings.push(intakeIntegrityFinding('INTAKE_ARTIFACT_SCAN_BUDGET', stem, file || DIR,
        'Shallow-intake integrity reached its fixed artifact budget; request a narrower stem-scoped verdict.'));
    }
    return false;
  }
  function integrityProcessState(pid, startId) {
    var key = String(pid) + '\0' + String(startId || '');
    if (Object.prototype.hasOwnProperty.call(processProofCache, key)) return processProofCache[key];
    if (processProofCount >= MAX_INTEGRITY_PROCESS_PROOFS || Date.now() > processProofDeadline) {
      if (!processProofBudgetFinding) {
        processProofBudgetFinding = true;
        out.truncated = true;
        out.findings.push(intakeIntegrityFinding('INTAKE_PROCESS_PROOF_BUDGET', stem, null,
          'Shallow-intake owner liveness exceeded its fixed proof budget; use a narrower stem scope.'));
      }
      return 'unverified-budget';
    }
    processProofCount++;
    try { processProofCache[key] = writerLeases.processIdentityState(pid, startId); }
    catch (error) { processProofCache[key] = 'unknown'; }
    return processProofCache[key];
  }
  function isGuardEvidenceName(name) {
    return /^(?:\.guard-txn-[a-f0-9]{64}(?:\.json(?:\.stage)?|\.decision\.json(?:\.stage)?|\.receipt\.json(?:\.stage)?)|\.guard-publish-[a-f0-9]{64}(?:\.json(?:\.stage)?|\.link\.json(?:\.stage)?|\.receipt\.json(?:\.stage)?)|\.guard-(?:capture|publish-data|cas-old)-[a-f0-9]{32})$/.test(name);
  }
  function recordRecoveryEvidence(file, evidenceStem, message) {
    if (evidenceSeen[file]) return;
    evidenceSeen[file] = true;
    var evidenceDir = path.dirname(file), evidenceRoot = managedRoot(evidenceDir);
    var entry = evidenceRoot && fileGuards.inspectEntryUnder(evidenceRoot, evidenceDir, file);
    if (entry && entry.status === 'missing') return;
    if (!spendArtifactBudget(file)) return;
    var externalScratchEvidence = fileGuards.isUnder(path.resolve(SCRATCH_DIR), path.resolve(file));
    var evidenceStat = fileGuards.statRegularFileUnder(evidenceRoot, evidenceDir, file);
    var evidenceLimit = Math.min(GUARD_EVIDENCE_MAX, guardEvidenceBytesRemaining);
    var overEvidenceBudget = evidenceStat &&
      (!Number.isSafeInteger(evidenceStat.size) || evidenceStat.size > evidenceLimit);
    var read = overEvidenceBudget ? {} : integrityRawRead(file, evidenceLimit);
    if (read.bytes) {
      guardEvidenceBytesRemaining -= read.bytes.length;
      if (!externalScratchEvidence) {
        out.snapshotInputs.push({ owner: 'shallow-intake', kind: 'guard-transaction-evidence', path: file,
          hash: sha256(read.bytes), size: read.bytes.length });
      }
    }
    if (overEvidenceBudget && !guardEvidenceBudgetFinding) {
      guardEvidenceBudgetFinding = true;
      out.truncated = true;
      out.findings.push(intakeIntegrityFinding('INTAKE_GUARD_EVIDENCE_BUDGET', evidenceStem,
        externalScratchEvidence ? null : file,
        'Guard transaction evidence exceeded the fixed aggregate byte budget; request a narrower stem-scoped verdict.'));
    }
    out.findings.push(intakeIntegrityFinding('INTAKE_GUARD_TRANSACTION_EVIDENCE', evidenceStem,
      externalScratchEvidence ? null : file, message));
  }
  function recordTargetGuardEvidence(target, evidenceStem, message) {
    var evidence = fileGuards.guardTransactionEvidenceForTarget(path.dirname(target), target);
    if (!evidence || !Array.isArray(evidence.entries)) {
      out.findings.push(intakeIntegrityFinding('INTAKE_GUARD_EVIDENCE_CONTRACT', evidenceStem, target,
        'The file-guard evidence naming contract rejected a canonical shallow-intake target.'));
      return;
    }
    evidence.entries.forEach(function (entry) {
      recordRecoveryEvidence(entry.path, evidenceStem, message);
    });
  }
  var root = managedRoot(DIR);
  var directory;
  try { directory = root && fileGuards.realDirectoryUnder(root, DIR, { allowMissing: true }); }
  catch (error) { directory = null; }
  if (!directory) {
    out.findings.push(intakeIntegrityFinding('INTAKE_RUNTIME_DIRECTORY_UNSAFE', stem, DIR, 'Shallow-intake runtime directory cannot be inspected safely.'));
    return out;
  }
  if (!directory.exists) return out;
  if (!privateScratchDirectory(directory, path.resolve(DIR))) {
    out.findings.push(intakeIntegrityFinding('INTAKE_RUNTIME_DIRECTORY_UNSAFE', stem, DIR,
      'Shallow-intake runtime directory is not owned and private.'));
    return out;
  }
  try {
    var scratchRootIntegrity = safeDirectory(path.resolve(SCRATCH_DIR), { allowMissing: true });
    if (!scratchRootIntegrity.exists || !privateScratchDirectory(scratchRootIntegrity, path.resolve(SCRATCH_DIR))) {
      out.findings.push(intakeIntegrityFinding('INTAKE_SCRATCH_ROOT_UNSAFE', stem, null,
        'Shallow-intake scratch root is missing, replaced, non-private, or not owned by this process user.'));
    } else {
      out.statuses.push({ owner: 'shallow-intake', kind: 'scratch-root', stem: null, state: 'private' });
    }
  } catch (scratchIntegrityError) {
    out.findings.push(intakeIntegrityFinding('INTAKE_SCRATCH_ROOT_UNSAFE', stem, null,
      'Shallow-intake scratch root cannot be inspected safely.'));
  }
  var names;
  var runtimeNames = null;
  var integrityActiveStems;
  var currentScratchRecords = [];
  if (stem) names = [stem + '.json'];
  else {
    var listed = fileGuards.boundedDirectoryNamesUnder(root, DIR, MAX_RUNTIME_ENTRIES);
    if (!listed.ok) {
      out.findings.push(intakeIntegrityFinding(listed.code === 'directory-entry-limit' ? 'INTAKE_RUNTIME_SCAN_LIMIT' : 'INTAKE_RUNTIME_DIRECTORY_UNSAFE', null, DIR,
        listed.code === 'directory-entry-limit' ? 'Shallow-intake runtime directory exceeds its bounded scan limit.' : 'Shallow-intake runtime directory cannot be enumerated safely.'));
      out.truncated = listed.code === 'directory-entry-limit'; return out;
    }
    runtimeNames = listed.names.slice().sort();
    names = runtimeNames.filter(isResultName).sort();
    if (names.length > artifactBudget) {
      out.findings.push(intakeIntegrityFinding('INTAKE_RESULT_SCAN_BUDGET', null, DIR,
        'Shallow-intake result scan reached its fixed artifact budget; request stem-scoped diagnostics for the remainder.'));
      out.truncated = true;
      names = names.slice(0, artifactBudget);
    }
    artifactBudget -= names.length;
    if (runtimeNames.indexOf('events.jsonl') >= 0) {
      var eventRead = integrityRawRead(EVENTS_FILE, EVENT_MAX);
      if (!eventRead.bytes) {
        out.findings.push(intakeIntegrityFinding('INTAKE_EVENT_UNSAFE', null, EVENTS_FILE,
          'Shallow-intake event history is non-private, unstable, or oversized.'));
      } else {
        out.snapshotInputs.push({ owner: 'shallow-intake', kind: 'events', path: EVENTS_FILE,
          hash: sha256(eventRead.bytes), size: eventRead.bytes.length });
      }
    }
    runtimeNames.forEach(function (name) {
      var nestedScratch = path.dirname(path.resolve(SCRATCH_DIR)) === path.resolve(DIR) && name === path.basename(path.resolve(SCRATCH_DIR));
      if (isResultName(name) ||
          ['.stem-locks', '.worker.json', 'events.jsonl'].indexOf(name) >= 0 || nestedScratch) return;
      if (isGuardEvidenceName(name)) {
        recordRecoveryEvidence(path.join(DIR, name), null,
          'A durable file-guard transaction remains and must reconcile before the next shallow-intake publication.');
      } else if (spendArtifactBudget(path.join(DIR, name))) {
        out.findings.push(intakeIntegrityFinding('INTAKE_RUNTIME_ENTRY_UNRECOGNIZED', null, path.join(DIR, name), 'Unrecognized shallow-intake runtime state is present.'));
      }
    });
  }
  names.forEach(function (name) {
    var rowStem = name.slice(0, -5), file = path.join(DIR, name), read = integrityRead(file, RESULT_MAX);
    if (read.missing && stem) return;
    if (!read.bytes || !read.canonical) { out.findings.push(intakeIntegrityFinding('INTAKE_RESULT_UNSAFE', rowStem, file, 'Shallow-intake result is unsafe, non-canonical, unstable, or oversized.')); return; }
    out.snapshotInputs.push({ owner: 'shallow-intake', kind: 'result', path: file, hash: sha256(read.bytes), size: read.bytes.length });
    try {
      validateRecord(read.value, rowStem);
      if (read.value.status === 'complete') {
        var completeSource = sourceState(rowStem);
        var authoritativeShadow = !completeSource.eligible && completeSource.reason === 'task-prep' &&
          completeSource.sourceHash === read.value.sourceHash;
        if ((!completeSource.eligible && !authoritativeShadow) || completeSource.sourceHash !== read.value.sourceHash) {
          throw codedError('INTAKE_RESULT_INVALID', 'stored complete intake result is not bound to the current source');
        }
        if (!integrityActiveStems) integrityActiveStems = activeStemSet();
        projectCompleteRecord(rowStem, read.value, completeSource, integrityActiveStems);
      }
    }
    catch (error) { out.findings.push(intakeIntegrityFinding('INTAKE_RESULT_INVALID', rowStem, file, String(error && error.message || error))); return; }
    out.statuses.push({ owner: 'shallow-intake', kind: 'result', stem: rowStem, state: read.value.status,
      createdAt: read.value.createdAt, contentHash: sha256(read.bytes) });
    if (read.value.status === 'queued' || read.value.status === 'checking') {
      currentScratchRecords.push({ stem: rowStem, requestId: read.value.requestId, status: read.value.status });
    }
  });

  currentScratchRecords.forEach(function (record) {
    var scratch = scratchDirectoryFor(record.requestId);
    if (!scratch) {
      out.findings.push(intakeIntegrityFinding('INTAKE_SCRATCH_REQUEST_INVALID', record.stem, null,
        'The active shallow-intake result does not map to one canonical scratch generation.'));
      return;
    }
    var inspected = inspectScratchDirectory(scratch);
    if (!inspected.ok) {
      out.findings.push(intakeIntegrityFinding('INTAKE_SCRATCH_GENERATION_UNSAFE', record.stem, null,
        'The active shallow-intake scratch generation is replaced, non-private, or not safely inspectable.'));
      return;
    }
    if (inspected.missing) return;
    var scratchStatus = { owner: 'shallow-intake', kind: 'scratch-generation', stem: record.stem,
      state: record.status, requestId: record.requestId };
    out.statuses.push(scratchStatus);
    var prompt = path.join(scratch, 'prompt.txt');
    var pinnedExecutable = path.join(scratch, PINNED_EXECUTABLE_NAME);
    recordTargetGuardEvidence(prompt, record.stem,
      'A durable prompt file-guard transaction remains in the active scratch generation.');
    var listedScratch = scratchNames(scratch);
    if (!listedScratch.ok) {
      out.findings.push(intakeIntegrityFinding('INTAKE_SCRATCH_GENERATION_UNSAFE', record.stem, null,
        'The active scratch generation cannot be enumerated within its fixed bound.'));
      return;
    }
    listedScratch.names.forEach(function (name) {
      if (name === 'prompt.txt' || name === PINNED_EXECUTABLE_NAME) return;
      var file = path.join(scratch, name);
      if (isGuardEvidenceName(name)) {
        recordRecoveryEvidence(file, record.stem,
          'Durable file-guard evidence remains in the active scratch generation.');
      } else if (spendArtifactBudget(file)) {
        out.findings.push(intakeIntegrityFinding('INTAKE_SCRATCH_ENTRY_UNRECOGNIZED', record.stem, null,
          'The active scratch generation contains an unrecognized entry.'));
      }
    });
    if (listedScratch.names.indexOf('prompt.txt') >= 0) {
      if (!spendArtifactBudget(prompt)) return;
      var promptRead = integrityRawRead(prompt, 128 * 1024);
      if (!promptRead.bytes) {
        out.findings.push(intakeIntegrityFinding('INTAKE_PROMPT_UNSAFE', record.stem, null,
          'The active model prompt is unsafe, non-private, unstable, or oversized.'));
      } else {
        // Scratch intentionally lives outside the repository and cannot enter
        // the public runtime snapshot path contract. Keep only its bounded
        // content hash on the redacted status row.
        scratchStatus.contentHash = sha256(promptRead.bytes);
      }
    }
    if (listedScratch.names.indexOf(PINNED_EXECUTABLE_NAME) >= 0) {
      if (!spendArtifactBudget(pinnedExecutable)) return;
      if (!privatePinnedExecutable(pinnedExecutable)) {
        out.findings.push(intakeIntegrityFinding('INTAKE_MODEL_EXECUTABLE_UNSAFE', record.stem, null,
          'The active pinned model executable is replaced, non-private, linked, or oversized.'));
      } else {
        // Never copy the executable or its private scratch path into public
        // observations; this bit records only that the bounded pin is present.
        scratchStatus.modelExecutablePinned = true;
      }
    }
  });

  function inspectOwner(file, kind, expectedStem, worker) {
    var read = integrityRead(file, worker ? OWNER_MAX * 4 : OWNER_MAX);
    if (read.missing) return;
    if (!read.bytes || !read.canonical) { out.findings.push(intakeIntegrityFinding('INTAKE_OWNER_UNSAFE', expectedStem, file, 'Shallow-intake owner record is unsafe, non-canonical, unstable, or oversized.')); return; }
    out.snapshotInputs.push({ owner: 'shallow-intake', kind: kind, path: file, hash: sha256(read.bytes), size: read.bytes.length });
    var valid = worker ? validWorker(read.value) : validOwner(read.value);
    if (!valid || expectedStem && read.value.stem && read.value.stem !== expectedStem) {
      out.findings.push(intakeIntegrityFinding(worker ? 'INTAKE_WORKER_INVALID' : 'INTAKE_OWNER_INVALID', expectedStem, file, 'Shallow-intake owner record violates its owner schema.')); return;
    }
    var rowStem = read.value.stem || expectedStem || null;
    if (stem && rowStem !== stem) return;
    var liveness = ownerLiveness(read.value, integrityProcessState, function (record) {
      return workerRecordTreeGone(record, integrityProcessState);
    });
    var state = liveness === 'recovery-required' ? 'recovery-required' :
      (liveness === 'active' ? 'active' : 'unverified');
    out.statuses.push({ owner: 'shallow-intake', kind: kind, stem: rowStem, state: state,
      createdAt: read.value.createdAt, updatedAt: read.value.updatedAt || read.value.createdAt, contentHash: sha256(read.bytes) });
    if (liveness === 'recovery-required') out.findings.push(intakeIntegrityFinding('INTAKE_OWNER_RECOVERY_REQUIRED', rowStem, file, 'A local shallow-intake owner generation is gone and is eligible for authenticated recovery.'));
    else if (liveness !== 'active') out.findings.push(intakeIntegrityFinding('INTAKE_OWNER_UNVERIFIED', rowStem, file, 'Shallow-intake owner liveness cannot be proven; the owner remains fail-closed.'));
  }

  inspectOwner(WORKER_FILE, 'worker', null, true);
  var lockRoot = managedRoot(STEM_LOCKS_DIR);
  var lockDirectory = lockRoot && fileGuards.realDirectoryUnder(lockRoot, STEM_LOCKS_DIR, { allowMissing: true });
  if (!lockDirectory) out.findings.push(intakeIntegrityFinding('INTAKE_LOCK_DIRECTORY_UNSAFE', stem, STEM_LOCKS_DIR, 'Shallow-intake stem-lock directory cannot be inspected safely.'));
  else if (lockDirectory.exists && !privateScratchDirectory(lockDirectory, path.resolve(STEM_LOCKS_DIR))) {
    out.findings.push(intakeIntegrityFinding('INTAKE_LOCK_DIRECTORY_UNSAFE', stem, STEM_LOCKS_DIR,
      'Shallow-intake stem-lock directory is not owned and private.'));
  }
  else if (lockDirectory.exists) {
    var lockNames;
    if (stem) lockNames = [stem + '.json'];
    else {
      var lockListed = fileGuards.boundedDirectoryNamesUnder(lockRoot, STEM_LOCKS_DIR, MAX_RUNTIME_ENTRIES);
      if (!lockListed.ok) {
        out.findings.push(intakeIntegrityFinding(lockListed.code === 'directory-entry-limit' ? 'INTAKE_LOCK_SCAN_LIMIT' : 'INTAKE_LOCK_DIRECTORY_UNSAFE', null, STEM_LOCKS_DIR, 'Shallow-intake stem-lock scan is incomplete.'));
        out.truncated = lockListed.code === 'directory-entry-limit'; lockNames = [];
      } else {
        lockNames = lockListed.names.filter(isResultName).sort();
        if (lockNames.length > artifactBudget) {
          out.findings.push(intakeIntegrityFinding('INTAKE_LOCK_SCAN_BUDGET', null, STEM_LOCKS_DIR,
            'Shallow-intake lock scan reached its fixed artifact budget; request stem-scoped diagnostics for the remainder.'));
          out.truncated = true;
          lockNames = lockNames.slice(0, Math.max(0, artifactBudget));
        }
        artifactBudget -= lockNames.length;
        lockListed.names.forEach(function (name) {
          if (isResultName(name)) return;
          if (isGuardEvidenceName(name)) {
            recordRecoveryEvidence(path.join(STEM_LOCKS_DIR, name), null,
              'A durable file-guard transaction remains in the shallow-intake lock directory.');
          } else if (spendArtifactBudget(path.join(STEM_LOCKS_DIR, name))) {
            out.findings.push(intakeIntegrityFinding('INTAKE_LOCK_ENTRY_UNRECOGNIZED', null, path.join(STEM_LOCKS_DIR, name), 'Unrecognized shallow-intake lock state is present.'));
          }
        });
      }
    }
    lockNames.forEach(function (name) { inspectOwner(path.join(STEM_LOCKS_DIR, name), 'stem-lock', name.slice(0, -5), false); });
  }
  if (stem) {
    recordTargetGuardEvidence(resultPath(stem), stem,
      'A durable shallow-intake result publication transaction remains for this task.');
    recordTargetGuardEvidence(ownerPath(STEM_LOCKS_DIR, stem), stem,
      'A durable shallow-intake stem-lock transaction remains for this task.');
    recordTargetGuardEvidence(WORKER_FILE, stem,
      'A durable global shallow-intake worker transaction remains and can block this task.');
  }
  return out;
}

module.exports = {
  prepareRuntime: ensureSafeScratch,
  init: init,
  schedule: schedule,
  retry: retry,
  dismiss: dismiss,
  supersede: supersede,
  reconcile: reconcile,
  snapshot: snapshot,
  sourceState: sourceState,
  publicErrorCode: publicErrorCode,
  publicFailureCode: publicFailureCode,
  publicProjection: publicProjection,
  recordFailure: recordFailure,
  dirMtime: dirMtime,
  killAll: killAll,
  validateRecord: validateRecord,
  validOwner: validOwner,
  validWorker: validWorker,
  scanIntegrity: scanIntegrity,
  _reconcileScratchRoot: reconcileScratchRoot,
  _withStemLock: withStemLock,
  _workerRecordTreeGone: workerRecordTreeGone,
  _reapDarwinOrphanedModel: reapDarwinOrphanedModel,
  _recoverStaleOwner: recoverStaleOwner,
  _modelEnv: modelEnv
};
