'use strict';

// ---------------------------------------------------------------------------
// Request queue. The site→Claude trigger. Each "Run in Claude" button on the
// Typed server-owned task admission drops a JSON file under
// orchestrator/.cache/tasks/requests/<id>.json:
//
//   { version, action, stem, expectedState, sourceRevision, dedupKey,
//     dedupReport, projectRoot, prompt, createdAt }
//
// `projectRoot` is the absolute root of the project this server serves
// (paths.PROJECT_ROOT). The standby /loop checks it against its own cwd and
// refuses to run a request stamped for a different project — so a drainer
// opened in the wrong directory fails loudly instead of executing the prompt
// against the wrong repo.
//
// `prompt` is the FULL rendered text the user would otherwise copy-paste
// (built by board.js's prompt builders — single source of truth). The
// standby `/loop` session (.claude/commands/serve-queue.md) claims the
// oldest request into a private run directory, executes `prompt` verbatim,
// releases its writer lease, and only then consumes the claim. `action` +
// `stem` are metadata for the board's "queued" badge and for dispatch
// logging; only `prompt` is executed.
//
// Queue publication and ownership transfers are whole-file, fsynced and
// no-clobber. A separate per-stem reservation serializes the admission scan and
// stays owned through claim, writer-lease attachment and the last read-only
// execution fence. The "queued" badge (request present) hands off to the "in
// progress" badge (lock present) when the prompt acquires its canonical lock.
// ---------------------------------------------------------------------------

var fs     = require('fs');
var path   = require('path');
var crypto = require('crypto');
var paths  = require('./paths');
var fileGuards = require('./file-guards');
var taskCore = require('../../tasks/task-state-core.cjs');

var REQUESTS_DIR = paths.REQUESTS_DIR;
var REQUEST_RESERVATIONS_DIR = paths.REQUEST_RESERVATIONS_DIR;
var RUNS_DIR = paths.RUNS_DIR;
var SUPERSEDED_DIR = paths.SUPERSEDED_DIR;

// Action allow-list for queued site→Claude requests. The standby /loop
// session (serve-queue) dispatches on this value; anything else is rejected
// on enqueue and ignored on read. Every queued action acts on an existing stem.
// Backlog creation is deliberately absent: the deterministic server endpoint
// owns numbering/file/INDEX publication, while shallow AI intake is a separate
// advisory process rather than a queue action.
// `drop` removes an obsolete task only through the canonical transition helper
// after an acknowledged dependent-impact snapshot; the prompt never performs a
// raw task-file delete or standalone INDEX rewrite.
var REQUEST_ACTIONS = new Set(['prep', 'answers', 'run', 'drop', 'reopen']);
var REQUEST_VERSION = 2;
var REQUEST_STATES = new Set(['backlog', 'pending', 'todo', 'done', 'corrupt']);
var REQUEST_REVISION_RE = /^sha256:[a-f0-9]{64}$/;
var REQUEST_FIELDS = ['action', 'createdAt', 'dedupKey', 'dedupReport', 'expectedState', 'projectRoot', 'prompt', 'sourceRevision', 'stem', 'version'];

// Request id shape: <epochMs>-<rand>. Server-generated, never client-
// supplied. The regex guards the clear endpoint against path traversal.
var REQUEST_ID_RE = /^[0-9]+-[a-z0-9]+$/;
var REQUEST_DEDUP_KEY_RE = /^[A-Za-z0-9_.:-]{1,240}$/;
var REQUEST_DEDUP_REPORT_RE = /^sha256:[a-f0-9]{64}$/i;

// Upper bound on a stored prompt, in CHARACTERS (the check is prompt.length).
// The `answers` prompt embeds the filled sidecar markdown — the largest
// realistic payload. NOTE: 60000 chars is up to ~120 KB as 2-byte UTF-8
// (Cyrillic), so MAX_BODY_BYTES in http.js (256 KB) is sized to fit this in any
// locale — do not drop the body cap below ~3× this value or non-ASCII answers
// prompts will be rejected by the byte cap before this char check runs.
var REQUEST_PROMPT_MAX = 60000;
var REQUEST_PROMPT_MAX_BYTES = REQUEST_PROMPT_MAX * 3;

// A reservation is the cross-process linearization point for enqueue.  It is
// intentionally separate from the queue record: it remains owned while the
// request moves requests/<id>.json -> requests/.<id>.claim (or a private
// standby claim under runs/), and
// is withdrawn only under an attached writer lease after the last read-only
// execution fence.  There is no age-based cleanup: age is not owner-death
// proof, so crash recovery is an explicit root/operator action.
var REQUEST_RESERVATION_VERSION = 1;
var REQUEST_RESERVATION_FIELDS = ['createdAt', 'fingerprint', 'requestId', 'stem', 'token', 'version'];
var REQUEST_FINGERPRINT_RE = /^sha256:[a-f0-9]{64}$/;
var REQUEST_RESERVATION_TOKEN_RE = /^[a-f0-9]{64}$/;
var REQUEST_RESERVATION_MAX_BYTES = 4096;

// Every queue reader is intentionally bounded before it sorts or parses
// directory contents. A malformed, incomplete, or unsafe scan is unavailable
// to Board/SSE and blocks admission before any ownership transfer.
var ADMISSION_DIRECTORY_ENTRIES_MAX = 10000;
var ADMISSION_RECORDS_MAX = 1000;
var ADMISSION_TOTAL_BYTES_MAX = 8 * 1024 * 1024;
var REQUEST_RECORD_MAX_BYTES = 256 * 1024;

var SUPERSEDED_VERSION = 1;
var SUPERSEDED_FIELDS = [
  'action', 'admittedAt', 'expectedSourceRevision', 'expectedState',
  'findings', 'observedSourceRevision', 'observedState', 'reason',
  'requestId', 'snapshotHash', 'status', 'stem', 'supersededAt', 'version'
];
var SUPERSEDED_FINDING_FIELDS = ['code', 'paths', 'severity'];
var SUPERSEDED_REASONS = new Set([
  'state-changed', 'source-revision-changed',
  'task-integrity-invalid', 'finalization-active'
]);
var SUPERSEDED_SEVERITIES = new Set(['info', 'warning', 'error', 'blocker']);
var SUPERSEDED_MAX_BYTES = 256 * 1024;
var SUPERSEDED_FINDINGS_MAX = 30;
var SUPERSEDED_PATHS_MAX = 20;
var SUPERSEDED_PATH_MAX = 240;
var SUPERSEDED_RETAIN = 300;

function canonicalStem(value) {
  if (typeof value !== 'string' || value.length > taskCore.STEM_MAX || taskCore.safeIntegerId(value) === null) return false;
  return taskCore.STEM_RE.test(value);
}

// One exact v2 contract for queued snapshots, active-claim discovery and the
// post-claim runner.  The runner remains the authority because it validates
// bytes after atomic ownership, but pre-claim readers never let malformed or
// hand-written records influence dedup/gating decisions.
function requestRecordIssue(value, expectedProjectRoot) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return 'request must be an object';
  var fields = Object.keys(value).sort();
  if (fields.length !== REQUEST_FIELDS.length || fields.some(function (field, i) { return field !== REQUEST_FIELDS[i]; })) {
    return 'request fields do not match the exact queue contract';
  }
  if (value.version !== REQUEST_VERSION) return 'request version is unsupported';
  if (!REQUEST_ACTIONS.has(value.action)) return 'request action is unsupported';
  if (!canonicalStem(value.stem)) return 'request stem is not canonical';
  var allowedStates = taskCore.ACTION_STATES[value.action];
  if (!Array.isArray(allowedStates) || allowedStates.indexOf(value.expectedState) < 0) return 'request expectedState is invalid for its action';
  if (typeof value.sourceRevision !== 'string' || !REQUEST_REVISION_RE.test(value.sourceRevision)) return 'request sourceRevision is invalid';
  if (typeof value.projectRoot !== 'string' || value.projectRoot !== expectedProjectRoot) return 'request projectRoot does not exactly match this server';
  if (typeof value.prompt !== 'string' || !value.prompt || value.prompt.length > REQUEST_PROMPT_MAX) return 'request prompt type/length is invalid';
  var promptBytes = Buffer.from(value.prompt, 'utf8');
  if (promptBytes.length > REQUEST_PROMPT_MAX_BYTES || promptBytes.toString('utf8') !== value.prompt || value.prompt.indexOf('\0') >= 0) {
    return 'request prompt UTF-8 bytes are invalid or oversized';
  }
  if (value.dedupKey !== null && (typeof value.dedupKey !== 'string' || !REQUEST_DEDUP_KEY_RE.test(value.dedupKey))) return 'request dedupKey is invalid';
  if (value.dedupReport !== null && (typeof value.dedupReport !== 'string' || !REQUEST_DEDUP_REPORT_RE.test(value.dedupReport))) return 'request dedupReport is invalid';
  if (typeof value.createdAt !== 'string' || !/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/.test(value.createdAt) ||
      !Number.isFinite(Date.parse(value.createdAt)) || new Date(value.createdAt).toISOString() !== value.createdAt) return 'request createdAt is invalid';
  return null;
}

function exactUtcNow() { return new Date().toISOString(); }

function exactUtcTimestamp(value) {
  return typeof value === 'string' &&
    /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/.test(value) &&
    Number.isFinite(Date.parse(value)) && new Date(value).toISOString() === value;
}

function sameDirectoryIdentity(left, right) {
  return !!left && !!right && left.isDirectory() && right.isDirectory() &&
    !left.isSymbolicLink() && !right.isSymbolicLink() &&
    left.dev === right.dev && left.ino === right.ino;
}

function sameFileSnapshot(left, right) {
  return !!left && !!right && left.isFile() && right.isFile() &&
    !left.isSymbolicLink() && !right.isSymbolicLink() &&
    left.dev === right.dev && left.ino === right.ino && left.modeExact === right.modeExact &&
    left.sizeExact === right.sizeExact && left.mtimeNs === right.mtimeNs && left.ctimeNs === right.ctimeNs;
}

function directoryIdentityCurrent(directory) {
  if (!directory || !directory.exists || !directory.stat) return false;
  try {
    var live = fs.lstatSync(directory.path, { bigint: true });
    return live.isDirectory() && !live.isSymbolicLink() &&
      directory.stat.dev === String(live.dev) && directory.stat.ino === String(live.ino);
  }
  catch (error) { return false; }
}

function safeDataDirectory(candidate, create) {
  return fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, candidate, {
    create: create === true,
    allowMissing: create !== true,
    mode: 0o700
  });
}

// Incremental opendir keeps hostile/non-canonical directory contents from
// being materialized without a ceiling.  Identity is rechecked after the scan
// so a replaced directory is never treated as a complete admission snapshot.
function boundedDirectoryNames(directory, maxEntries) {
  if (!directory || !directory.exists || !directory.stat ||
      !Number.isSafeInteger(maxEntries) || maxEntries < 1) {
    return { ok: false, code: 'directory-unsafe' };
  }
  return fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, directory.path, maxEntries);
}

// Walk only from the configured project root downward.  Host paths such as
// macOS /var may legitimately contain a system symlink above the project; the
// security boundary is the configured root itself and every cache ancestor
// below it.  A reservation root outside that boundary, or any symlink/non-dir
// component below it, is rejected.  `create=false` is strictly read-only.
function safeReservationDirectory(create) {
  var directory = fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, REQUEST_RESERVATIONS_DIR, {
    create: create === true, allowMissing: create !== true, mode: 0o700
  });
  if (!directory) return { ok: false, code: 'reservation-root-unsafe' };
  if (!directory.exists) return { ok: true, missing: true, path: REQUEST_RESERVATIONS_DIR };
  return {
    ok: true, missing: false, path: directory.path,
    dev: directory.stat.dev, ino: directory.stat.ino, stat: directory.stat
  };
}

function requestFingerprint(record) {
  if (requestRecordIssue(record, paths.PROJECT_ROOT)) return null;
  // createdAt is deliberately excluded: an HTTP transport retry admitted
  // against the same canonical snapshot is the same intent. Every executable
  // or dedup-relevant byte is included, so different answers/prompts are never
  // silently collapsed.
  var intent = {
    version: record.version,
    projectRoot: record.projectRoot,
    stem: record.stem,
    action: record.action,
    expectedState: record.expectedState,
    sourceRevision: record.sourceRevision,
    dedupKey: record.dedupKey,
    dedupReport: record.dedupReport,
    prompt: record.prompt
  };
  return 'sha256:' + crypto.createHash('sha256').update(JSON.stringify(intent), 'utf8').digest('hex');
}

function reservationPathFor(stem) {
  if (!canonicalStem(stem)) return null;
  return path.join(path.resolve(REQUEST_RESERVATIONS_DIR), stem + '.json');
}

function requestReservationIssue(value, expectedStem) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return 'reservation must be an object';
  var fields = Object.keys(value).sort();
  if (fields.length !== REQUEST_RESERVATION_FIELDS.length || fields.some(function (field, i) {
    return field !== REQUEST_RESERVATION_FIELDS[i];
  })) return 'reservation fields do not match the exact contract';
  if (value.version !== REQUEST_RESERVATION_VERSION) return 'reservation version is unsupported';
  if (!REQUEST_ID_RE.test(String(value.requestId || ''))) return 'reservation requestId is invalid';
  if (!canonicalStem(value.stem) || (expectedStem && value.stem !== expectedStem)) return 'reservation stem is invalid';
  if (typeof value.fingerprint !== 'string' || !REQUEST_FINGERPRINT_RE.test(value.fingerprint)) return 'reservation fingerprint is invalid';
  if (typeof value.token !== 'string' || !REQUEST_RESERVATION_TOKEN_RE.test(value.token)) return 'reservation token is invalid';
  if (typeof value.createdAt !== 'string' || !/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/.test(value.createdAt) ||
      !Number.isFinite(Date.parse(value.createdAt)) || new Date(value.createdAt).toISOString() !== value.createdAt) {
    return 'reservation createdAt is invalid';
  }
  return null;
}

function reservationHandle(record) {
  return {
    version: record.version,
    requestId: record.requestId,
    stem: record.stem,
    fingerprint: record.fingerprint,
    token: record.token,
    createdAt: record.createdAt
  };
}

// Read-only by contract: no mkdir, pruning, repair, timestamp touch or stale
// inference. Callers can expose the prompt-free metadata but must not return the
// token over HTTP.
function inspectRequestReservation(stem) {
  if (!canonicalStem(stem)) return { status: 'unsafe', code: 'bad-stem' };
  var directory = safeReservationDirectory(false);
  if (!directory.ok) return { status: 'unsafe', code: directory.code };
  if (directory.missing) return { status: 'missing' };
  var file = reservationPathFor(stem);
  var bounded = fileGuards.boundedRegularFileUnder(
    paths.PROJECT_ROOT, REQUEST_RESERVATIONS_DIR, file, REQUEST_RESERVATION_MAX_BYTES
  );
  if (!bounded || bounded.stat.nlink !== '1') {
    var entry = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, REQUEST_RESERVATIONS_DIR, file);
    if (entry.status === 'missing') return { status: 'missing' };
    return { status: 'unsafe', code: entry.status === 'present' ? 'reservation-file-unsafe' : 'reservation-unreadable' };
  }
  var value;
  try { value = JSON.parse(bounded.bytes.toString('utf8')); }
  catch (error) { value = null; }
  var issue = requestReservationIssue(value, stem);
  if (issue) return { status: 'unsafe', code: 'reservation-invalid', detail: issue };
  return { status: 'active', record: reservationHandle(value) };
}

function directoryFsyncUnavailable(error, platform) {
  if (platform !== 'win32' || !error) return false;
  // Windows/Node cannot open or fsync directory handles consistently across
  // supported filesystems.  File fsync + atomic hard-link/unlink remains the
  // portability boundary there, matching the repository's other durable
  // publishers. Do not mask arbitrary I/O failures on POSIX or a real Windows
  // storage failure outside the known unsupported-directory-handle codes.
  return ['EACCES', 'EBADF', 'EINVAL', 'EISDIR', 'ENOTSUP', 'EPERM'].indexOf(error.code) >= 0;
}

function fsyncDirectory(directory) {
  var fd;
  try {
    fd = fs.openSync(directory, fs.constants.O_RDONLY | (fs.constants.O_DIRECTORY || 0));
    fs.fsyncSync(fd);
    return true;
  } catch (error) { return directoryFsyncUnavailable(error, process.platform); }
  finally { if (fd !== undefined) try { fs.closeSync(fd); } catch (closeError) {} }
}

// Atomic no-clobber per-stem acquisition. The hidden temp is fully durable
// before link(2) publishes it; only one process can create <stem>.json. Existing
// bytes are never replaced or repaired automatically.
function acquireRequestReservation(id, record) {
  if (!REQUEST_ID_RE.test(String(id || ''))) return { ok: false, code: 'bad-request-id' };
  var issue = requestRecordIssue(record, paths.PROJECT_ROOT);
  if (issue) return { ok: false, code: 'bad-request-contract', detail: issue };
  var fingerprint = requestFingerprint(record);
  var directory = safeReservationDirectory(true);
  if (!directory.ok) return { ok: false, code: 'request-reservation-unsafe', detail: directory.code };
  var reservation = {
    version: REQUEST_RESERVATION_VERSION,
    requestId: id,
    stem: record.stem,
    fingerprint: fingerprint,
    token: crypto.randomBytes(32).toString('hex'),
    createdAt: exactUtcNow()
  };
  var target = reservationPathFor(record.stem);
  var bytes = Buffer.from(JSON.stringify(reservation, null, 2) + '\n', 'utf8');
  var published = fileGuards.publishNoClobberRegularFileUnder(
    paths.PROJECT_ROOT, REQUEST_RESERVATIONS_DIR, target, bytes,
    { create: true, mode: 0o600, maxBytes: REQUEST_RESERVATION_MAX_BYTES }
  );
  if (published.ok) {
    return { ok: true, acquired: true, handle: reservationHandle(reservation) };
  }
  if (published.code === 'exists' || published.code === 'published-unverified') {
    var existing = inspectRequestReservation(record.stem);
    if (existing.status === 'active') {
      // EEXIST may be a retry of a generation whose prior publisher lost only
      // the final directory-fsync result. Make the existing inode + directory
      // durable now before exposing it as adoptable ownership.
      if (!fileGuards.fsyncRegularFileUnder(paths.PROJECT_ROOT, REQUEST_RESERVATIONS_DIR, target) ||
          !fileGuards.fsyncDirectoryUnder(paths.PROJECT_ROOT, REQUEST_RESERVATIONS_DIR)) {
        return { ok: false, code: 'request-reservation-sync-failed' };
      }
      return { ok: false, code: 'request-reservation-active', existing: existing.record, fingerprint: fingerprint };
    }
    return { ok: false, code: 'request-reservation-unsafe', detail: existing.code || 'existing reservation is unreadable' };
  }
  return { ok: false, code: 'request-reservation-write-failed', detail: published.code || 'anchored publication failed' };
}

// Runner/standby admission requires the exact reservation published with the
// queue record. A missing or foreign reservation is never synthesized or
// stolen, even when its prompt hash happens to match.
function ensureRequestReservation(id, record) {
  if (!REQUEST_ID_RE.test(String(id || ''))) return { ok: false, code: 'bad-request-id' };
  var issue = requestRecordIssue(record, paths.PROJECT_ROOT);
  if (issue) return { ok: false, code: 'bad-request-contract', detail: issue };
  var fingerprint = requestFingerprint(record);
  var existing = inspectRequestReservation(record.stem);
  if (existing.status === 'active' && existing.record.requestId === id &&
      existing.record.stem === record.stem && existing.record.fingerprint === fingerprint) {
    return { ok: true, acquired: false, handle: reservationHandle(existing.record) };
  }
  return { ok: false, code: existing.status === 'active' ? 'request-reservation-active' : 'request-reservation-unavailable',
    detail: existing.code || existing.status };
}

function sameReservation(left, right) {
  return !!left && !!right && left.version === right.version &&
    left.requestId === right.requestId && left.stem === right.stem &&
    left.fingerprint === right.fingerprint && left.token === right.token &&
    left.createdAt === right.createdAt;
}

// Exact, token-authenticated withdrawal. Wrong/stale handles are read-only and
// return false. The reservation path and directory identities are rechecked
// immediately before unlink so a symlink/replacement never receives authority.
function releaseRequestReservation(handle) {
  if (requestReservationIssue(handle, handle && handle.stem)) return false;
  var directory = safeReservationDirectory(false);
  if (!directory.ok || directory.missing) return false;
  var file = reservationPathFor(handle.stem);
  return fileGuards.unlinkRegularFileIfUnder(
    paths.PROJECT_ROOT, REQUEST_RESERVATIONS_DIR, file, REQUEST_RESERVATION_MAX_BYTES,
    function (bounded) {
      var value;
      try { value = JSON.parse(bounded.bytes.toString('utf8')); } catch (error) { return false; }
      return !requestReservationIssue(value, handle.stem) && sameReservation(value, handle);
    }
  );
}

function readRequestRecordFile(file, expectedProjectRoot) {
  var value = readBoundedJsonFile(file, REQUEST_RECORD_MAX_BYTES);
  return requestRecordIssue(value, expectedProjectRoot || paths.PROJECT_ROOT) ? null : value;
}

function requestFileMatches(id, expected) {
  if (!requestPathFor(id) || requestRecordIssue(expected, paths.PROJECT_ROOT)) return false;
  var directory = safeDataDirectory(REQUESTS_DIR, false);
  if (!directory || !directory.exists) return false;
  var observed = readRequestRecordFile(requestPathFor(id), paths.PROJECT_ROOT);
  if (!observed) return false;
  return REQUEST_FIELDS.every(function (field) { return observed[field] === expected[field]; });
}

function requestSnapshotRow(id, parsed) {
  return {
    id: id,
    version: parsed.version,
    action: parsed.action,
    stem: parsed.stem,
    expectedState: parsed.expectedState,
    sourceRevision: parsed.sourceRevision,
    dedupKey: parsed.dedupKey,
    dedupReport: parsed.dedupReport,
    fingerprint: requestFingerprint(parsed),
    createdAt: parsed.createdAt
  };
}

function scanRequests() {
  var directory = safeDataDirectory(REQUESTS_DIR, false);
  if (!directory) return { ok: false, code: 'request-directory-unsafe', rows: [] };
  if (!directory.exists) return { ok: true, rows: [] };
  var listed = boundedDirectoryNames(directory, ADMISSION_DIRECTORY_ENTRIES_MAX);
  if (!listed.ok) return { ok: false, code: 'request-' + listed.code, rows: [] };
  var entries = listed.names;
  var candidates = [];
  for (var n = 0; n < entries.length; n++) {
    var candidateName = entries[n];
    if (!candidateName.endsWith('.json')) continue;
    var candidateId = candidateName.substring(0, candidateName.length - '.json'.length);
    if (!REQUEST_ID_RE.test(candidateId)) {
      return { ok: false, code: 'request-name-invalid', rows: [] };
    }
    if (candidates.length >= ADMISSION_RECORDS_MAX) {
      return { ok: false, code: 'request-record-limit', rows: [] };
    }
    candidates.push({ id: candidateId, name: candidateName });
  }
  candidates.sort(function (a, b) { return a.id < b.id ? -1 : a.id > b.id ? 1 : 0; });
  var out = [];
  var totalBytes = 0;
  for (var i = 0; i < candidates.length; i++) {
    var row = candidates[i];
    var bounded = fileGuards.boundedRegularFileUnder(
      paths.PROJECT_ROOT, REQUESTS_DIR, path.join(REQUESTS_DIR, row.name), REQUEST_RECORD_MAX_BYTES
    );
    if (!bounded || bounded.stat.nlink !== '1') {
      return { ok: false, code: 'request-record-unsafe', rows: [] };
    }
    totalBytes += bounded.bytes.length;
    if (totalBytes > ADMISSION_TOTAL_BYTES_MAX) return { ok: false, code: 'request-byte-limit', rows: [] };
    var parsed;
    try { parsed = JSON.parse(bounded.bytes.toString('utf8')); }
    catch (parseError) {
      return { ok: false, code: 'request-record-invalid', rows: [] };
    }
    if (requestRecordIssue(parsed, paths.PROJECT_ROOT)) {
      return { ok: false, code: 'request-record-invalid', rows: [] };
    }
    // `prompt` is deliberately omitted from the snapshot — it is a large
    // executable blob the board/API status projection never returns.
    out.push(requestSnapshotRow(row.id, parsed));
  }
  if (!directoryIdentityCurrent(directory)) return { ok: false, code: 'request-directory-identity-changed', rows: [] };
  return { ok: true, rows: out };
}

function requestsDirMtime() {
  var directory = safeDataDirectory(REQUESTS_DIR, false);
  return directory && directory.exists ? directory.stat.mtimeMs : 0;
}

function requestPathFor(id) {
  if (!id || !REQUEST_ID_RE.test(id)) return null;
  return path.normalize(path.join(REQUESTS_DIR, id + '.json'));
}

// Atomic no-clobber write: publish a fully fsynced hidden temp through a hard
// link.  Unlike rename(), link() cannot replace a request id that appeared
// after our initial checks, so concurrent producers have exactly one winner.
function writeRequestFile(id, obj) {
  var target = requestPathFor(id);
  if (!target || requestRecordIssue(obj, paths.PROJECT_ROOT)) return false;
  var bytes = Buffer.from(JSON.stringify(obj, null, 2) + '\n');
  return fileGuards.publishNoClobberRegularFileUnder(
    paths.PROJECT_ROOT, REQUESTS_DIR, target, bytes,
    { create: true, mode: 0o600, maxBytes: REQUEST_RECORD_MAX_BYTES }
  ).ok;
}

// Same-filesystem ownership transfer without rename(2)'s replace semantics.
// link(target) is the no-clobber publication; only its winner unlinks source.
// If source withdrawal loses a concurrent race, roll back our link and report
// failure, leaving the other claimant as the sole owner.
function transferFileNoClobber(source, target) {
  if (typeof source !== 'string' || typeof target !== 'string' ||
      path.dirname(source) !== path.dirname(target)) return false;
  return fileGuards.transferFileNoClobberSameDirectoryUnder(
    paths.PROJECT_ROOT, path.dirname(source), source, target
  );
}

// Atomically win cancellation against both runner claim layouts, then release
// only the exact reservation belonging to the bytes we removed. A lost claim
// race is a successful no-op and must not withdraw the runner's handoff fence.
function cancelQueuedRequest(id) {
  var source = requestPathFor(id);
  if (!source) return { ok: false, code: 'bad-id' };
  var cancelPath = path.join(REQUESTS_DIR, '.' + id + '-' + process.pid + '-' + crypto.randomBytes(8).toString('hex') + '.cancel');
  if (!transferFileNoClobber(source, cancelPath)) {
    // The queue path may still exist when a foreign hidden destination caused
    // the failure. Never guess ownership; the caller may retry.
    var sourceEntry = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, REQUESTS_DIR, source);
    if (sourceEntry.status === 'missing') return { ok: true, removed: false };
    return { ok: false, code: sourceEntry.status === 'present' ? 'cancel-claim-failed' : 'cancel-claim-unverified' };
  }
  var record = readRequestRecordFile(cancelPath, paths.PROJECT_ROOT);
  if (!record) {
    // The bytes may once have owned a valid reservation, but after corruption
    // their stem/fingerprint cannot authenticate which receipt to withdraw.
    // Preserve the private cancel claim for explicit recovery instead of
    // silently discarding the only evidence and stranding hidden ownership.
    return { ok: false, code: 'cancel-record-unsafe-private-claim-retained' };
  }
  if (record) {
    var inspected = inspectRequestReservation(record.stem);
    if (inspected.status === 'active') {
      var expectedFingerprint = requestFingerprint(record);
      if (inspected.record.requestId !== id || inspected.record.fingerprint !== expectedFingerprint) {
        if (!transferFileNoClobber(cancelPath, source)) {
          return { ok: false, code: 'reservation-release-failed-private-claim-retained' };
        }
        return { ok: false, code: 'request-reservation-mismatch' };
      }
      if (!releaseRequestReservation(inspected.record)) {
        var afterRelease = inspectRequestReservation(record.stem);
        // unlink may have succeeded while the directory fsync failed. In that
        // case durability is unproven and the hidden claim must remain private;
        // restoring it claimable without a reservation would be unsafe.
        if (afterRelease.status === 'missing') {
          return { ok: false, code: 'reservation-release-unverified-private-claim-retained' };
        }
        if (!transferFileNoClobber(cancelPath, source)) {
          return { ok: false, code: 'reservation-release-failed-private-claim-retained' };
        }
        return { ok: false, code: 'request-reservation-release-failed' };
      }
    } else if (inspected.status === 'unsafe') {
      if (!transferFileNoClobber(cancelPath, source)) {
        return { ok: false, code: 'reservation-unsafe-private-claim-retained' };
      }
      return { ok: false, code: 'request-reservation-unsafe' };
    }
  }
  if (fileGuards.unlinkRegularFileUnder(paths.PROJECT_ROOT, REQUESTS_DIR, cancelPath)) {
    return { ok: true, removed: true };
  }
  // Reservation is already absent, so requeueing here would violate the
  // queue+reservation invariant. Keep the hidden cancel claim for explicit
  // recovery; it is never claimable by a runner.
  return { ok: false, code: 'cancel-consume-failed-private-claim-retained' };
}
function publicReservationErrorCode(result) {
  return result && result.code === 'request-reservation-active'
    ? 'request-reservation-active' : 'request-reservation-failed';
}
function publicCancellationErrorCode(result) {
  return result && result.code === 'bad-id' ? 'bad-id' : 'request-cancel-failed';
}

// Claims are normally visible for only a few milliseconds, but the standby
// drainer deliberately keeps its private claim for the whole model turn. Scan
// both layouts so every caller observes the same fail-closed ownership state.
function scanActiveClaims() {
  var directory = safeDataDirectory(RUNS_DIR, false);
  if (!directory) return { ok: false, code: 'claim-directory-unsafe', rows: [] };
  var requestDirectory = safeDataDirectory(REQUESTS_DIR, false);
  if (!requestDirectory) return { ok: false, code: 'claim-request-directory-unsafe', rows: [] };
  var listed = directory.exists ? boundedDirectoryNames(directory, ADMISSION_DIRECTORY_ENTRIES_MAX) : { ok: true, names: [] };
  var requestListed = requestDirectory.exists ? boundedDirectoryNames(requestDirectory, ADMISSION_DIRECTORY_ENTRIES_MAX) : { ok: true, names: [] };
  if (!listed.ok) return { ok: false, code: 'claim-' + listed.code, rows: [] };
  if (!requestListed.ok) return { ok: false, code: 'claim-request-' + requestListed.code, rows: [] };
  var names = listed.names.sort();
  var candidates = [];
  for (var q = 0; q < requestListed.names.length; q++) {
    var site = /^\.([0-9]+-[a-z0-9]+)\.claim$/.exec(requestListed.names[q]);
    if (!site) continue;
    if (candidates.length >= ADMISSION_RECORDS_MAX) return { ok: false, code: 'claim-record-limit', rows: [] };
    candidates.push({ id: site[1], file: path.join(REQUESTS_DIR, requestListed.names[q]), directory: REQUESTS_DIR });
  }
  for (var i = 0; i < names.length; i++) {
    var name = names[i];
    var direct = /^\.([0-9]+-[a-z0-9]+)\.claim$/.exec(name);
    if (direct) {
      if (candidates.length >= ADMISSION_RECORDS_MAX) return { ok: false, code: 'claim-record-limit', rows: [] };
      candidates.push({ id: direct[1], file: path.join(RUNS_DIR, name), directory: RUNS_DIR });
      continue;
    }
    var standby = /^\.standby-([0-9]+-[a-z0-9]+)-/.exec(name);
    if (standby) {
      var privateDir = path.join(RUNS_DIR, name);
      var safePrivate = safeDataDirectory(privateDir, false);
      if (!safePrivate || !safePrivate.exists) {
        return { ok: false, code: 'claim-private-directory-unsafe', rows: [] };
      }
      if (candidates.length >= ADMISSION_RECORDS_MAX) return { ok: false, code: 'claim-record-limit', rows: [] };
      candidates.push({ id: standby[1], file: path.join(privateDir, 'request.claim'), directory: privateDir, parent: safePrivate });
    }
  }
  var out = [];
  var totalBytes = 0;
  for (var j = 0; j < candidates.length; j++) {
    var row = candidates[j];
    var bounded = fileGuards.boundedRegularFileUnder(
      paths.PROJECT_ROOT, row.directory, row.file, REQUEST_RECORD_MAX_BYTES
    );
    if (!bounded || bounded.stat.nlink !== '1') {
      return { ok: false, code: 'claim-record-unsafe', rows: [] };
    }
    if (row.parent && !directoryIdentityCurrent(row.parent)) {
      return { ok: false, code: 'claim-private-directory-changed', rows: [] };
    }
    totalBytes += bounded.bytes.length;
    if (totalBytes > ADMISSION_TOTAL_BYTES_MAX) return { ok: false, code: 'claim-byte-limit', rows: [] };
    var value;
    try { value = JSON.parse(bounded.bytes.toString('utf8')); }
    catch (parseError) {
      return { ok: false, code: 'claim-record-invalid', rows: [] };
    }
    if (requestRecordIssue(value, paths.PROJECT_ROOT)) {
      return { ok: false, code: 'claim-record-invalid', rows: [] };
    }
    out.push(requestSnapshotRow(row.id, value));
  }
  if ((directory.exists && !directoryIdentityCurrent(directory)) ||
      (requestDirectory.exists && !directoryIdentityCurrent(requestDirectory))) {
    return { ok: false, code: 'claim-directory-identity-changed', rows: [] };
  }
  out.sort(function (a, b) { return a.id < b.id ? -1 : a.id > b.id ? 1 : 0; });
  return { ok: true, rows: out };
}

function readBoundedFile(file, maxBytes) {
  if (typeof file !== 'string' || file !== path.resolve(file)) return null;
  var bounded = fileGuards.boundedRegularFileUnder(
    paths.PROJECT_ROOT, path.dirname(file), file, maxBytes
  );
  return bounded && bounded.stat.nlink === '1' ? bounded : null;
}

function readBoundedJsonFile(file, maxBytes) {
  var bounded = readBoundedFile(file, maxBytes);
  if (!bounded) return null;
  try { return JSON.parse(bounded.bytes.toString('utf8')); }
  catch (error) { return null; }
}

function safeSupersededPath(value) {
  if (typeof value !== 'string' || !value || value.length > SUPERSEDED_PATH_MAX || /[\0\r\n]/.test(value)) return false;
  if (path.isAbsolute(value) || /^[A-Za-z]:[\\/]/.test(value)) return false;
  var parts = value.replace(/\\/g, '/').split('/');
  return parts.every(function (part) { return part && part !== '.' && part !== '..'; });
}

function supersededRecordIssue(value, expectedId) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return 'superseded record must be an object';
  var fields = Object.keys(value).sort();
  if (fields.length !== SUPERSEDED_FIELDS.length || fields.some(function (field, i) { return field !== SUPERSEDED_FIELDS[i]; })) {
    return 'superseded record fields do not match the exact contract';
  }
  if (value.version !== SUPERSEDED_VERSION || value.status !== 'superseded') return 'superseded version/status is invalid';
  if (!REQUEST_ID_RE.test(String(value.requestId || '')) || (expectedId && value.requestId !== expectedId)) return 'superseded requestId is invalid';
  if (!REQUEST_ACTIONS.has(value.action) || !canonicalStem(value.stem)) return 'superseded action/stem is invalid';
  var allowedStates = taskCore.ACTION_STATES[value.action];
  if (!Array.isArray(allowedStates) || allowedStates.indexOf(value.expectedState) < 0) return 'superseded expectedState is invalid';
  if (value.observedState !== null && !REQUEST_STATES.has(value.observedState)) return 'superseded observedState is invalid';
  if (!SUPERSEDED_REASONS.has(value.reason)) return 'superseded reason is invalid';
  if (!REQUEST_REVISION_RE.test(String(value.expectedSourceRevision || ''))) return 'superseded expectedSourceRevision is invalid';
  if (value.observedSourceRevision !== null && !REQUEST_REVISION_RE.test(String(value.observedSourceRevision || ''))) {
    return 'superseded observedSourceRevision is invalid';
  }
  if (value.snapshotHash !== null && !REQUEST_REVISION_RE.test(String(value.snapshotHash || ''))) return 'superseded snapshotHash is invalid';
  if (!exactUtcTimestamp(value.admittedAt) || !exactUtcTimestamp(value.supersededAt)) return 'superseded timestamp is invalid';
  if (!Array.isArray(value.findings) || value.findings.length > SUPERSEDED_FINDINGS_MAX) return 'superseded findings are invalid';
  for (var i = 0; i < value.findings.length; i++) {
    var finding = value.findings[i];
    if (!finding || typeof finding !== 'object' || Array.isArray(finding)) return 'superseded finding must be an object';
    var findingFields = Object.keys(finding).sort();
    if (findingFields.length !== SUPERSEDED_FINDING_FIELDS.length || findingFields.some(function (field, index) {
      return field !== SUPERSEDED_FINDING_FIELDS[index];
    })) return 'superseded finding fields do not match the exact contract';
    if (typeof finding.code !== 'string' || !/^[A-Za-z0-9_.:-]{1,120}$/.test(finding.code)) return 'superseded finding code is invalid';
    if (!SUPERSEDED_SEVERITIES.has(finding.severity)) return 'superseded finding severity is invalid';
    if (!Array.isArray(finding.paths) || finding.paths.length > SUPERSEDED_PATHS_MAX ||
        finding.paths.some(function (entry) { return !safeSupersededPath(entry); })) {
      return 'superseded finding paths are invalid';
    }
  }
  return null;
}

function canonicalSupersededRecord(value, expectedId) {
  if (supersededRecordIssue(value, expectedId)) return null;
  return {
    version: value.version,
    status: value.status,
    requestId: value.requestId,
    action: value.action,
    stem: value.stem,
    reason: value.reason,
    expectedState: value.expectedState,
    observedState: value.observedState,
    expectedSourceRevision: value.expectedSourceRevision,
    observedSourceRevision: value.observedSourceRevision,
    admittedAt: value.admittedAt,
    supersededAt: value.supersededAt,
    snapshotHash: value.snapshotHash,
    findings: value.findings.map(function (finding) {
      return { code: finding.code, severity: finding.severity, paths: finding.paths.slice() };
    })
  };
}

function canonicalSupersededBytes(value, expectedId) {
  var canonical = canonicalSupersededRecord(value, expectedId);
  if (!canonical) return null;
  var bytes = Buffer.from(JSON.stringify(canonical, null, 2) + '\n', 'utf8');
  return bytes.length <= SUPERSEDED_MAX_BYTES ? bytes : null;
}

function readSupersededFile(id) {
  if (!REQUEST_ID_RE.test(String(id || ''))) return null;
  var bounded = readBoundedFile(path.join(SUPERSEDED_DIR, id + '.json'), SUPERSEDED_MAX_BYTES);
  if (!bounded || !supersededModeSafe(bounded.stat, process.platform)) return null;
  var value;
  try { value = JSON.parse(bounded.bytes.toString('utf8')); }
  catch (error) { return null; }
  var canonical = canonicalSupersededBytes(value, id);
  if (!canonical || !bounded.bytes.equals(canonical)) return null;
  return canonicalSupersededRecord(value, id);
}

function supersededModeSafe(stat, platform) {
  if (!stat || !stat.isFile()) return false;
  // Windows chmod/stat exposes only a read-only approximation rather than
  // POSIX owner/group bits. The real-directory boundary and inherited ACL are
  // authoritative there; exact 0600 remains mandatory everywhere else.
  return platform === 'win32' || (stat.mode & 0o777) === 0o600;
}

function fsyncRegularFile(file) {
  return fileGuards.fsyncRegularFileUnder(
    paths.PROJECT_ROOT, path.dirname(file), file
  );
}

// Keep `preserveId` immutable even when it is lexically the oldest entry.
// maxOthers reserves one slot before a new publication and bounds the complete
// directory after publication.  Every deletion is directory-fsynced before a
// terminal claim can be consumed.
function pruneSuperseded(maxOthers, preserveId, directory) {
  if (!Number.isSafeInteger(maxOthers) || maxOthers < 0 || !directory || !directory.exists) return false;
  var listed = boundedDirectoryNames(directory, ADMISSION_DIRECTORY_ENTRIES_MAX);
  if (!listed.ok) return false;
  var names = listed.names.filter(function (name) {
    return name.endsWith('.json') && REQUEST_ID_RE.test(name.slice(0, -5)) && name.slice(0, -5) !== preserveId;
  }).sort();
  var excess = names.length - maxOthers;
  for (var i = 0; i < excess; i++) {
    if (!fileGuards.unlinkRegularFileUnder(
      paths.PROJECT_ROOT, SUPERSEDED_DIR, path.join(SUPERSEDED_DIR, names[i])
    )) return false;
  }
  return fileGuards.fsyncDirectoryUnder(paths.PROJECT_ROOT, SUPERSEDED_DIR);
}

function acceptExistingSuperseded(id, bytes, directory) {
  var target = path.join(SUPERSEDED_DIR, id + '.json');
  var bounded = readBoundedFile(target, SUPERSEDED_MAX_BYTES);
  if (!bounded || !supersededModeSafe(bounded.stat, process.platform) || !bounded.bytes.equals(bytes)) return false;
  var value;
  try { value = JSON.parse(bounded.bytes.toString('utf8')); }
  catch (error) { return false; }
  if (supersededRecordIssue(value, id)) return false;
  if (!pruneSuperseded(SUPERSEDED_RETAIN - 1, id, directory)) return false;
  return fsyncRegularFile(target) && fileGuards.fsyncDirectoryUnder(paths.PROJECT_ROOT, SUPERSEDED_DIR);
}

// Persist a prompt-free terminal record before consuming a stale claim. The
// destination is immutable: hard-link publication can never replace an
// existing id. An exact canonical replay is idempotent; conflicting bytes leave
// both the existing tombstone and the private claim untouched.
function writeSupersededFile(id, obj) {
  if (!REQUEST_ID_RE.test(String(id || ''))) return false;
  var bytes = canonicalSupersededBytes(obj, id);
  if (!bytes) return false;
  var directory = safeDataDirectory(SUPERSEDED_DIR, true);
  if (!directory || !directory.exists) return false;
  var target = path.join(SUPERSEDED_DIR, id + '.json');
  var existing = readBoundedFile(target, SUPERSEDED_MAX_BYTES);
  if (existing) return acceptExistingSuperseded(id, bytes, directory);
  // Reserve capacity before publication. Failure leaves no new terminal state
  // and the caller retains/requeues the claim.
  if (!pruneSuperseded(SUPERSEDED_RETAIN - 1, id, directory)) return false;
  var published = fileGuards.publishNoClobberRegularFileUnder(
    paths.PROJECT_ROOT, SUPERSEDED_DIR, target, bytes,
    { create: true, mode: 0o600, maxBytes: SUPERSEDED_MAX_BYTES }
  );
  if (!published.ok) {
    if (published.code === 'exists' || published.code === 'published-unverified') {
      return acceptExistingSuperseded(id, bytes, directory);
    }
    return false;
  }
  if (!fsyncRegularFile(target)) return false;
  return pruneSuperseded(SUPERSEDED_RETAIN - 1, id, directory);
}

function integrityDigest(bytes) { return 'sha256:' + crypto.createHash('sha256').update(bytes).digest('hex'); }
function requestIntegrityFinding(code, stem, file, message) {
  return { code: code, severity: 'error', stem: stem || null, paths: file ? [file] : [], message: message,
    recovery: 'Recover the exact queue/reservation generation through its owning request flow; never delete runtime records by age.' };
}
function readIntegrityRecord(directory, file, maxBytes, issueFn, expected) {
  var bounded = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, directory, file, maxBytes);
  if (!bounded || bounded.stat.nlink !== '1') return { issue: 'record is unsafe, unstable, or oversized' };
  var value;
  try { value = JSON.parse(bounded.bytes.toString('utf8')); } catch (error) { value = null; }
  var invalid = issueFn(value, expected);
  return { bytes: bounded.bytes, value: value, issue: invalid };
}
function scanIntegrity(scope) {
  var stem = typeof scope === 'string' ? scope : scope && scope.stem || null;
  var out = { version: 1, owner: 'requests', statuses: [], findings: [], snapshotInputs: [], truncated: false };
  var reservations = Object.create(null);
  var reservationDirectory = safeReservationDirectory(false);
  if (!reservationDirectory.ok) {
    out.findings.push(requestIntegrityFinding('REQUEST_RESERVATION_DIRECTORY_UNSAFE', stem, REQUEST_RESERVATIONS_DIR, 'Request reservation directory cannot be read safely.'));
    return out;
  }
  var reservationNames = [];
  if (!reservationDirectory.missing) {
    if (stem) reservationNames = [stem + '.json'];
    else {
      var reservationListed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, REQUEST_RESERVATIONS_DIR, ADMISSION_DIRECTORY_ENTRIES_MAX);
      if (!reservationListed.ok) {
        out.findings.push(requestIntegrityFinding(reservationListed.code === 'directory-entry-limit' ? 'REQUEST_RESERVATION_SCAN_LIMIT' : 'REQUEST_RESERVATION_DIRECTORY_UNSAFE', null, REQUEST_RESERVATIONS_DIR,
          reservationListed.code === 'directory-entry-limit' ? 'Request reservation directory exceeds its bounded scan limit.' : 'Request reservation directory cannot be enumerated safely.'));
        out.truncated = reservationListed.code === 'directory-entry-limit'; return out;
      }
      reservationNames = reservationListed.names.filter(function (name) { return name.endsWith('.json'); }).sort();
    }
  }
  if (reservationNames.length > ADMISSION_RECORDS_MAX) {
    out.findings.push(requestIntegrityFinding('REQUEST_RESERVATION_SCAN_LIMIT', null, REQUEST_RESERVATIONS_DIR, 'Request reservation count exceeds its bounded scan limit.'));
    out.truncated = true; return out;
  }
  var totalBytes = 0;
  reservationNames.forEach(function (name) {
    var expectedStem = name.endsWith('.json') ? name.slice(0, -5) : null;
    if (!canonicalStem(expectedStem)) {
      out.findings.push(requestIntegrityFinding('REQUEST_RESERVATION_NAME_INVALID', null, path.join(REQUEST_RESERVATIONS_DIR, name), 'Request reservation filename is invalid.'));
      return;
    }
    var file = path.join(REQUEST_RESERVATIONS_DIR, name);
    var entry = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, REQUEST_RESERVATIONS_DIR, file);
    if (entry.status === 'missing' && stem) return;
    var read = readIntegrityRecord(REQUEST_RESERVATIONS_DIR, file, REQUEST_RESERVATION_MAX_BYTES, requestReservationIssue, expectedStem);
    if (!read.bytes) {
      out.findings.push(requestIntegrityFinding('REQUEST_RESERVATION_UNSAFE', expectedStem, file, read.issue)); return;
    }
    totalBytes += read.bytes.length;
    out.snapshotInputs.push({ owner: 'requests', kind: 'reservation', path: file, hash: integrityDigest(read.bytes), size: read.bytes.length });
    if (read.issue) {
      out.findings.push(requestIntegrityFinding('REQUEST_RESERVATION_INVALID', expectedStem, file, read.issue)); return;
    }
    var record = read.value;
    if (reservations[record.requestId]) {
      out.findings.push(requestIntegrityFinding('REQUEST_RESERVATION_DUPLICATE_ID', record.stem, file, 'Multiple stem reservations claim the same request id.'));
    }
    reservations[record.requestId] = record;
    out.statuses.push({ owner: 'requests', kind: 'reservation', stem: record.stem, requestId: record.requestId,
      state: 'active', createdAt: record.createdAt, contentHash: integrityDigest(read.bytes) });
  });
  if (totalBytes > ADMISSION_TOTAL_BYTES_MAX) {
    out.findings.push(requestIntegrityFinding('REQUEST_RUNTIME_SCAN_LIMIT', stem, REQUEST_RESERVATIONS_DIR, 'Request runtime bytes exceed the bounded aggregate limit.'));
    out.truncated = true; return out;
  }

  function addRequestLocation(id, kind, directory, file, recoveryRequired) {
    var read = readIntegrityRecord(directory, file, REQUEST_RECORD_MAX_BYTES, requestRecordIssue, paths.PROJECT_ROOT);
    if (!read.bytes) { out.findings.push(requestIntegrityFinding('REQUEST_RUNTIME_RECORD_UNSAFE', stem, file, read.issue)); return; }
    totalBytes += read.bytes.length;
    out.snapshotInputs.push({ owner: 'requests', kind: kind, path: file, hash: integrityDigest(read.bytes), size: read.bytes.length });
    if (read.issue) { out.findings.push(requestIntegrityFinding('REQUEST_RUNTIME_RECORD_INVALID', stem, file, read.issue)); return; }
    var record = read.value;
    if (stem && record.stem !== stem) {
      out.findings.push(requestIntegrityFinding('REQUEST_RESERVATION_RECORD_MISMATCH', stem, file, 'Reservation index points to a request for another stem.')); return;
    }
    var reservation = reservations[id];
    var fingerprint = requestFingerprint(record);
    if (!reservation || reservation.stem !== record.stem || reservation.requestId !== id || reservation.fingerprint !== fingerprint) {
      out.findings.push(requestIntegrityFinding('REQUEST_RESERVATION_RECORD_MISMATCH', record.stem, file, 'Request record does not have an exact matching reservation.'));
    }
    out.statuses.push({ owner: 'requests', kind: kind, stem: record.stem, requestId: id, action: record.action,
      state: recoveryRequired ? 'recovery-required' : kind === 'queue' ? 'queued' : 'claimed',
      createdAt: record.createdAt, contentHash: integrityDigest(read.bytes) });
    if (recoveryRequired) out.findings.push(requestIntegrityFinding('REQUEST_PRIVATE_CLAIM_RECOVERY_REQUIRED', record.stem, file, 'A private cancellation/claim generation requires explicit recovery.'));
  }

  if (stem) {
    var indexedIds = Object.keys(reservations);
    var requestDirectory = safeDataDirectory(REQUESTS_DIR, false);
    var runsDirectory = safeDataDirectory(RUNS_DIR, false);
    if (!requestDirectory || !runsDirectory) {
      out.findings.push(requestIntegrityFinding('REQUEST_RUNTIME_DIRECTORY_UNSAFE', stem, REQUESTS_DIR, 'Request/claim runtime directory cannot be inspected safely.')); return out;
    }
    // Request filenames carry only opaque ids. Even when the stem reservation
    // is absent or stale, a scoped verdict must find orphan queue/claim owners
    // for this stem. Enumerate under fixed bounds, read each candidate through
    // the anchored guard, and include every inspected byte hash in the scoped
    // snapshot before filtering on record.stem.
    var locations = [], scopedCandidates = [];
    function scopedCandidate(id, kind, directory, file, recovery) {
      if (scopedCandidates.length >= ADMISSION_RECORDS_MAX) return false;
      scopedCandidates.push({ id: id, kind: kind, directory: directory, file: file, recovery: recovery });
      return true;
    }
    if (requestDirectory.exists) {
      var requestNames = boundedDirectoryNames(requestDirectory, ADMISSION_DIRECTORY_ENTRIES_MAX);
      if (!requestNames.ok) { out.findings.push(requestIntegrityFinding('REQUEST_RUNTIME_SCAN_LIMIT', stem, REQUESTS_DIR, 'Request directory scan is incomplete.')); out.truncated = true; return out; }
      requestNames.names.forEach(function (name) {
        var match;
        if ((match = /^([0-9]+-[a-z0-9]+)\.json$/.exec(name))) scopedCandidate(match[1], 'queue', REQUESTS_DIR, path.join(REQUESTS_DIR, name), false);
        else if ((match = /^\.([0-9]+-[a-z0-9]+)\.claim$/.exec(name))) scopedCandidate(match[1], 'claim', REQUESTS_DIR, path.join(REQUESTS_DIR, name), false);
        else if ((match = /^\.([0-9]+-[a-z0-9]+)-[0-9]+-[a-f0-9]+\.cancel$/.exec(name))) scopedCandidate(match[1], 'cancel-claim', REQUESTS_DIR, path.join(REQUESTS_DIR, name), true);
        else if (name.endsWith('.json')) out.findings.push(requestIntegrityFinding('REQUEST_RUNTIME_NAME_INVALID', null, path.join(REQUESTS_DIR, name), 'Request runtime filename is invalid.'));
      });
    }
    if (runsDirectory.exists && scopedCandidates.length < ADMISSION_RECORDS_MAX) {
      var runNames = boundedDirectoryNames(runsDirectory, ADMISSION_DIRECTORY_ENTRIES_MAX);
      if (!runNames.ok) { out.findings.push(requestIntegrityFinding('REQUEST_RUNTIME_SCAN_LIMIT', stem, RUNS_DIR, 'Claim directory scan is incomplete.')); out.truncated = true; return out; }
      runNames.names.forEach(function (name) {
        var match;
        if ((match = /^\.([0-9]+-[a-z0-9]+)\.claim$/.exec(name))) scopedCandidate(match[1], 'claim', RUNS_DIR, path.join(RUNS_DIR, name), false);
        else if ((match = /^\.standby-([0-9]+-[a-z0-9]+)-/.exec(name))) {
          var standbyDirectory = path.join(RUNS_DIR, name);
          scopedCandidate(match[1], 'standby-claim', standbyDirectory, path.join(standbyDirectory, 'request.claim'), false);
        }
      });
    }
    if (scopedCandidates.length >= ADMISSION_RECORDS_MAX) {
      out.findings.push(requestIntegrityFinding('REQUEST_RUNTIME_SCAN_LIMIT', stem, null, 'Request/claim record count reached the bounded scan limit.'));
      out.truncated = true;
    }
    scopedCandidates.sort(function (left, right) { return left.file < right.file ? -1 : left.file > right.file ? 1 : 0; }).forEach(function (row) {
      var read = readIntegrityRecord(row.directory, row.file, REQUEST_RECORD_MAX_BYTES, requestRecordIssue, paths.PROJECT_ROOT);
      if (!read.bytes) { out.findings.push(requestIntegrityFinding('REQUEST_RUNTIME_RECORD_UNSAFE', null, row.file, read.issue)); return; }
      totalBytes += read.bytes.length;
      out.snapshotInputs.push({ owner: 'requests', kind: row.kind, path: row.file, hash: integrityDigest(read.bytes), size: read.bytes.length });
      if (read.issue) { out.findings.push(requestIntegrityFinding('REQUEST_RUNTIME_RECORD_INVALID', null, row.file, read.issue)); return; }
      var record = read.value;
      if (record.stem !== stem) {
        if (indexedIds.indexOf(row.id) >= 0) out.findings.push(requestIntegrityFinding('REQUEST_RESERVATION_RECORD_MISMATCH', stem, row.file, 'Reservation index points to a request for another stem.'));
        return;
      }
      var reservation = reservations[row.id];
      if (!reservation || reservation.stem !== record.stem || reservation.requestId !== row.id || reservation.fingerprint !== requestFingerprint(record)) {
        out.findings.push(requestIntegrityFinding('REQUEST_RESERVATION_RECORD_MISMATCH', record.stem, row.file, 'Request record does not have an exact matching reservation.'));
      }
      locations.push(row);
      out.statuses.push({ owner: 'requests', kind: row.kind, stem: record.stem, requestId: row.id, action: record.action,
        state: row.recovery ? 'recovery-required' : row.kind === 'queue' ? 'queued' : 'claimed',
        createdAt: record.createdAt, contentHash: integrityDigest(read.bytes) });
      if (row.recovery) out.findings.push(requestIntegrityFinding('REQUEST_PRIVATE_CLAIM_RECOVERY_REQUIRED', record.stem, row.file, 'A private cancellation/claim generation requires explicit recovery.'));
    });
    if (locations.length > 1) out.findings.push(requestIntegrityFinding('REQUEST_MULTIPLE_OWNERS', stem, null, 'This task exists in multiple request ownership locations.'));
  } else {
    var requestDirectoryAll = safeDataDirectory(REQUESTS_DIR, false);
    var runsDirectoryAll = safeDataDirectory(RUNS_DIR, false);
    if (!requestDirectoryAll || !runsDirectoryAll) {
      out.findings.push(requestIntegrityFinding('REQUEST_RUNTIME_DIRECTORY_UNSAFE', null, REQUESTS_DIR, 'Request/claim runtime directory cannot be inspected safely.'));
      return out;
    }
    var candidates = [], ownerCounts = Object.create(null);
    function queueCandidate(id, kind, directory, file, recovery) {
      if (candidates.length >= ADMISSION_RECORDS_MAX) return false;
      candidates.push({ id: id, kind: kind, directory: directory, file: file, recovery: recovery });
      ownerCounts[id] = (ownerCounts[id] || 0) + 1;
      return true;
    }
    if (requestDirectoryAll.exists) {
      var requestListedAll = boundedDirectoryNames(requestDirectoryAll, ADMISSION_DIRECTORY_ENTRIES_MAX);
      if (!requestListedAll.ok) {
        out.findings.push(requestIntegrityFinding('REQUEST_RUNTIME_SCAN_LIMIT', null, REQUESTS_DIR, 'Request directory scan is incomplete.'));
        out.truncated = true; return out;
      }
      for (var requestIndex = 0; requestIndex < requestListedAll.names.length; requestIndex++) {
        var requestName = requestListedAll.names[requestIndex], match;
        if ((match = /^([0-9]+-[a-z0-9]+)\.json$/.exec(requestName))) {
          if (!queueCandidate(match[1], 'queue', REQUESTS_DIR, path.join(REQUESTS_DIR, requestName), false)) break;
        } else if ((match = /^\.([0-9]+-[a-z0-9]+)\.claim$/.exec(requestName))) {
          if (!queueCandidate(match[1], 'claim', REQUESTS_DIR, path.join(REQUESTS_DIR, requestName), false)) break;
        } else if ((match = /^\.([0-9]+-[a-z0-9]+)-[0-9]+-[a-f0-9]+\.cancel$/.exec(requestName))) {
          if (!queueCandidate(match[1], 'cancel-claim', REQUESTS_DIR, path.join(REQUESTS_DIR, requestName), true)) break;
        } else if (requestName.endsWith('.json')) {
          out.findings.push(requestIntegrityFinding('REQUEST_RUNTIME_NAME_INVALID', null, path.join(REQUESTS_DIR, requestName), 'Request runtime filename is invalid.'));
        }
      }
    }
    if (runsDirectoryAll.exists && candidates.length < ADMISSION_RECORDS_MAX) {
      var runsListedAll = boundedDirectoryNames(runsDirectoryAll, ADMISSION_DIRECTORY_ENTRIES_MAX);
      if (!runsListedAll.ok) {
        out.findings.push(requestIntegrityFinding('REQUEST_RUNTIME_SCAN_LIMIT', null, RUNS_DIR, 'Claim directory scan is incomplete.'));
        out.truncated = true; return out;
      }
      for (var runIndex = 0; runIndex < runsListedAll.names.length; runIndex++) {
        var runName = runsListedAll.names[runIndex], runMatch;
        if ((runMatch = /^\.([0-9]+-[a-z0-9]+)\.claim$/.exec(runName))) {
          if (!queueCandidate(runMatch[1], 'claim', RUNS_DIR, path.join(RUNS_DIR, runName), false)) break;
        } else if ((runMatch = /^\.standby-([0-9]+-[a-z0-9]+)-/.exec(runName))) {
          var standbyDirectory = path.join(RUNS_DIR, runName);
          if (!queueCandidate(runMatch[1], 'standby-claim', standbyDirectory, path.join(standbyDirectory, 'request.claim'), false)) break;
        }
      }
    }
    if (candidates.length >= ADMISSION_RECORDS_MAX) {
      out.findings.push(requestIntegrityFinding('REQUEST_RUNTIME_SCAN_LIMIT', null, null, 'Request/claim record count reached the bounded scan limit.'));
      out.truncated = true;
    }
    candidates.sort(function (left, right) { return left.file < right.file ? -1 : left.file > right.file ? 1 : 0; }).forEach(function (row) {
      addRequestLocation(row.id, row.kind, row.directory, row.file, row.recovery);
    });
    Object.keys(ownerCounts).sort().forEach(function (id) {
      if (ownerCounts[id] > 1) out.findings.push(requestIntegrityFinding('REQUEST_MULTIPLE_OWNERS', reservations[id] && reservations[id].stem, null,
        'One request id exists in multiple ownership locations.'));
    });
  }
  if (totalBytes > ADMISSION_TOTAL_BYTES_MAX) {
    out.findings.push(requestIntegrityFinding('REQUEST_RUNTIME_SCAN_LIMIT', stem, null, 'Request runtime bytes exceed the bounded aggregate limit.'));
    out.truncated = true;
  }
  return out;
}

module.exports = {
  REQUEST_ACTIONS: REQUEST_ACTIONS,
  REQUEST_VERSION: REQUEST_VERSION,
  REQUEST_ID_RE: REQUEST_ID_RE,
  REQUEST_DEDUP_KEY_RE: REQUEST_DEDUP_KEY_RE,
  REQUEST_DEDUP_REPORT_RE: REQUEST_DEDUP_REPORT_RE,
  REQUEST_PROMPT_MAX: REQUEST_PROMPT_MAX,
  REQUEST_PROMPT_MAX_BYTES: REQUEST_PROMPT_MAX_BYTES,
  REQUEST_RESERVATION_VERSION: REQUEST_RESERVATION_VERSION,
  ADMISSION_RECORDS_MAX: ADMISSION_RECORDS_MAX,
  requestRecordIssue: requestRecordIssue,
  requestFingerprint: requestFingerprint,
  inspectRequestReservation: inspectRequestReservation,
  acquireRequestReservation: acquireRequestReservation,
  ensureRequestReservation: ensureRequestReservation,
  releaseRequestReservation: releaseRequestReservation,
  readRequestRecordFile: readRequestRecordFile,
  requestFileMatches: requestFileMatches,
  scanRequests: scanRequests,
  requestsDirMtime: requestsDirMtime,
  writeRequestFile: writeRequestFile,
  transferFileNoClobber: transferFileNoClobber,
  cancelQueuedRequest: cancelQueuedRequest,
  publicReservationErrorCode: publicReservationErrorCode,
  publicCancellationErrorCode: publicCancellationErrorCode,
  scanActiveClaims: scanActiveClaims,
  supersededRecordIssue: supersededRecordIssue,
  readSupersededFile: readSupersededFile,
  writeSupersededFile: writeSupersededFile,
  SUPERSEDED_RETAIN: SUPERSEDED_RETAIN,
  directoryFsyncUnavailable: directoryFsyncUnavailable,
  supersededModeSafe: supersededModeSafe,
  scanIntegrity: scanIntegrity
};
