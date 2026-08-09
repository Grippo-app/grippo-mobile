#!/usr/bin/env node

import crypto from 'node:crypto';
import childProcess from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { TextDecoder } from 'node:util';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const paths = require('../server/paths.js');
const fileGuards = require('../server/file-guards.js');
const locks = require('../server/locks.js');
const requests = require('../server/requests.js');
const taskIntegrity = require('../server/task-integrity.js');
const writerLeases = require('../../tasks/writer-leases.cjs');
const taskSource = require('../../tasks/task-source-contract.cjs');

const HERE = path.dirname(fileURLToPath(import.meta.url));
const BOUNDARY = path.join(HERE, 'standby-queue-boundary.py');
const TASK_LOCK = path.join(HERE, '..', '..', 'tasks', 'task-lock.mjs');
const REQUEST_MAX = 256 * 1024;
const DIRECTORY_MAX = 10_000;
const RUNNER_MARKER_MAX = 4096;
const RUNNER_FRESH_MS = 30_000;
const RUNNER_FUTURE_SKEW_MS = 5_000;
const PASS_FRESH_MS = 30_000;
const HANDLE_RE = /^[a-f0-9]{64}$/;
const PASS_TOKEN_RE = /^[a-f0-9]{64}$/;
const REQUEST_NAME_RE = /^([0-9]+-[a-z0-9]+)\.json$/;
const SHA_RE = /^sha256:[a-f0-9]{64}$/;
const TOKEN_RE = /^[a-f0-9]{32,128}$/;

class QueueError extends Error {
  constructor(code, exitCode = 2) {
    super(code);
    this.code = code;
    this.exitCode = exitCode;
  }
}

function fail(code, exitCode) {
  throw new QueueError(code, exitCode);
}

function testCrash(label) {
  if (process.env.ORCHESTRATOR_STANDBY_TEST_HOOK !== label ||
      process.env.ORCHESTRATOR_STANDBY_TEST_ROOT !== paths.PROJECT_ROOT) return;
  try {
    const root = fs.realpathSync(paths.PROJECT_ROOT);
    const temp = fs.realpathSync(os.tmpdir());
    const relative = path.relative(temp, root);
    if (!relative || relative === '..' || relative.startsWith('..' + path.sep) || path.isAbsolute(relative)) return;
    process.exit(86);
  } catch {}
}

function exactKeys(value, expected) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return false;
  const actual = Object.keys(value).sort();
  const wanted = expected.slice().sort();
  return actual.length === wanted.length && actual.every((key, index) => key === wanted[index]);
}

function stableJson(value) {
  if (Array.isArray(value)) return '[' + value.map(stableJson).join(',') + ']';
  if (value && typeof value === 'object') return '{' + Object.keys(value).sort().map((key) => JSON.stringify(key) + ':' + stableJson(value[key])).join(',') + '}';
  return JSON.stringify(value);
}

function statProof(stat) {
  const proof = stat && {
    ctimeNs: stat.ctimeNs,
    dev: stat.dev,
    ino: stat.ino,
    mode: stat.modeExact,
    mtimeNs: stat.mtimeNs,
    nlink: stat.nlink,
    size: stat.sizeExact,
    type: stat.type,
  };
  if (!proof || !['ctimeNs', 'dev', 'ino', 'mode', 'mtimeNs', 'nlink', 'size'].every((key) => /^(?:0|[1-9][0-9]*)$/.test(String(proof[key]))) ||
      !['directory', 'file', 'symlink', 'other'].includes(proof.type)) fail('directory-proof-invalid');
  return Object.fromEntries(Object.entries(proof).map(([key, value]) => [key, String(value)]));
}

function relativeUnder(root, candidate) {
  const rel = path.relative(root, candidate);
  if (!rel || rel === '..' || rel.startsWith('..' + path.sep) || path.isAbsolute(rel)) fail('configured-path-outside-project');
  return rel.split(path.sep).join('/');
}

function anchoredDirectories({ worker = false, create = true } = {}) {
  let cwdReal, rootReal;
  try {
    cwdReal = fs.realpathSync(process.cwd());
    rootReal = fs.realpathSync(paths.PROJECT_ROOT);
  } catch { fail('wrong-project-root'); }
  if (cwdReal !== rootReal) fail('wrong-project-root');
  const root = fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, paths.PROJECT_ROOT);
  const options = create ? { create: true, mode: 0o700 } : {};
  const requestsDirectory = fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, paths.REQUESTS_DIR, options);
  const runsDirectory = fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, paths.RUNS_DIR, options);
  const workerDirectory = worker ? fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, paths.WORKER_DIR, options) : null;
  if (!root || !requestsDirectory || !requestsDirectory.exists || !runsDirectory || !runsDirectory.exists || (worker && (!workerDirectory || !workerDirectory.exists))) {
    fail('queue-authority-unsafe');
  }
  return { root, requestsDirectory, runsDirectory, workerDirectory };
}

function boundaryBase(directories) {
  return {
    version: 1,
    projectRoot: paths.PROJECT_ROOT,
    requestsRelative: relativeUnder(paths.PROJECT_ROOT, paths.REQUESTS_DIR),
    runsRelative: relativeUnder(paths.PROJECT_ROOT, paths.RUNS_DIR),
    rootProof: statProof(directories.root.stat),
    requestsProof: statProof(directories.requestsDirectory.stat),
    runsProof: statProof(directories.runsDirectory.stat),
  };
}

function boundary(directories, action, extra = {}) {
  const input = JSON.stringify({ ...boundaryBase(directories), action, ...extra });
  if (Buffer.byteLength(input, 'utf8') > 1024 * 1024) fail('boundary-input-too-large');
  const env = { ...process.env };
  delete env.PYTHONPATH;
  delete env.PYTHONHOME;
  delete env.PYTHONSTARTUP;
  delete env.PYTHONINSPECT;
  delete env.PYTHONBREAKPOINT;
  delete env.LD_PRELOAD;
  delete env.DYLD_INSERT_LIBRARIES;
  delete env.DYLD_LIBRARY_PATH;
  delete env.NODE_OPTIONS;
  delete env.NODE_PATH;
  const result = childProcess.spawnSync('python3', ['-I', '-B', BOUNDARY], {
    cwd: paths.PROJECT_ROOT,
    env,
    input,
    encoding: 'utf8',
    maxBuffer: 3 * 1024 * 1024,
    windowsHide: true,
  });
  let parsed = null;
  try { parsed = result && typeof result.stdout === 'string' ? JSON.parse(result.stdout) : null; } catch {}
  if (!result || result.error || result.signal || result.status !== 0 ||
      !parsed || typeof parsed !== 'object' || Array.isArray(parsed) || parsed.ok !== true) {
    const code = parsed && typeof parsed.code === 'string' && /^[a-z0-9-]{1,80}$/.test(parsed.code) ? parsed.code :
      (result && result.status === 86 ? 'test-crash' : 'boundary-unavailable');
    fail(code);
  }
  return parsed;
}

function decodeClaimHex(value) {
  if (typeof value !== 'string' || value.length > REQUEST_MAX * 2 || value.length % 2 || !/^[a-f0-9]*$/.test(value)) fail('boundary-claim-envelope-invalid');
  return Buffer.from(value, 'hex');
}

function parseClaim(bytes) {
  let text;
  try { text = new TextDecoder('utf-8', { fatal: true }).decode(bytes); } catch { fail('claimed-contract-invalid'); }
  let record;
  try { record = JSON.parse(text); } catch { fail('claimed-contract-invalid'); }
  if (requests.requestRecordIssue(record, paths.PROJECT_ROOT)) fail('claimed-contract-invalid');
  return record;
}

function fingerprint(record) {
  const value = requests.requestFingerprint(record);
  if (!SHA_RE.test(String(value || ''))) fail('claimed-fingerprint-invalid');
  return value;
}

function promptHash(record) {
  return 'sha256:' + crypto.createHash('sha256').update(record.prompt, 'utf8').digest('hex');
}

function projection(id, record) {
  const fingerprintValue = fingerprint(record);
  const promptHashValue = promptHash(record);
  return {
    id,
    version: record.version,
    action: record.action,
    stem: record.stem,
    expectedState: record.expectedState,
    sourceRevision: record.sourceRevision,
    createdAt: record.createdAt,
    fingerprint: fingerprintValue,
    promptHash: promptHashValue,
  };
}

function validateLoaded(loaded) {
  if (!exactKeys(loaded, ['ok', 'status', 'id', 'bytes', 'request', 'phases', 'executionFence', 'disclosureFence']) || loaded.status !== 'loaded' ||
      typeof loaded.id !== 'string' || !exactKeys(loaded.phases, ['invalid', 'execution', 'disclosed', 'restore', 'restored', 'consume', 'detached', 'consumed']) ||
      !Object.values(loaded.phases).every((value) => typeof value === 'boolean') ||
      (loaded.executionFence !== null && !executionFenceShape(loaded.executionFence)) ||
      (loaded.disclosureFence !== null && !executionFenceShape(loaded.disclosureFence)) ||
      loaded.phases.execution !== (loaded.executionFence !== null) ||
      loaded.phases.disclosed !== (loaded.disclosureFence !== null)) fail('boundary-load-envelope-invalid');
  const record = parseClaim(decodeClaimHex(loaded.bytes));
  const expected = projection(loaded.id, record);
  if (stableJson(expected) !== stableJson(loaded.request)) fail('claim-projection-mismatch');
  return { loaded, record, projection: expected };
}

function loadHandle(directories, handle) {
  return validateLoaded(boundary(directories, 'load', { handle }));
}

function reservationMatches(id, record) {
  const inspected = requests.inspectRequestReservation(record.stem);
  const expectedFingerprint = fingerprint(record);
  return inspected.status === 'active' && inspected.record && inspected.record.requestId === id &&
    inspected.record.stem === record.stem && inspected.record.fingerprint === expectedFingerprint;
}

function reservationMissing(record) {
  return requests.inspectRequestReservation(record.stem).status === 'missing';
}

function deterministicPublicationLease(row) {
  return row && typeof row.key === 'string' && (
    row.key === 'task:create-backlog' ||
    row.key === 'task:recover-backlog-creations' ||
    row.key === 'task:recover-backlog-edits' ||
    row.key.startsWith('task:edit-backlog:')
  );
}

function leaseReceiptShape(receipt) {
  return exactKeys(receipt, ['leaseId', 'token', 'sessionId']) &&
    writerLeases.LEASE_ID_RE.test(String(receipt.leaseId || '')) && TOKEN_RE.test(String(receipt.token || '')) &&
    writerLeases.SESSION_ID_RE.test(String(receipt.sessionId || ''));
}

function exactActiveLease(record, receipt) {
  if (!leaseReceiptShape(receipt)) fail('writer-lease-receipt-invalid', 64);
  let scan;
  try { scan = writerLeases.scan(paths.WRITER_LEASES_DIR, paths.WRITER_AUTHORITY_ROOT); }
  catch { fail('writer-lease-scan-unavailable'); }
  if (!scan || !Array.isArray(scan.active) || !Array.isArray(scan.issues) || scan.issues.length) {
    fail('writer-lease-scan-blocked');
  }
  const now = Date.now();
  const matches = scan.active.filter((row) => row.leaseId === receipt.leaseId && row.token === receipt.token &&
    row.kind === 'task-session' && row.stem === record.stem && row.sessionId === receipt.sessionId &&
    row.key === `standby:${record.action}` && row.unverified === false && row.expiresAt !== null &&
    Number.isFinite(Date.parse(row.expiresAt)) && Date.parse(row.expiresAt) > now && row.owner &&
    row.owner.hostname === os.hostname() && writerLeases.processIdentityMatches(row.owner.pid, row.owner.processStartId));
  if (matches.length !== 1) fail('writer-lease-authority-lost');
  const own = matches[0];
  const conflict = scan.active.find((row) => row.leaseId !== own.leaseId &&
    (deterministicPublicationLease(row) || row.stem === record.stem || row.key === `task:${record.stem}`));
  if (conflict) fail('writer-lease-conflict');
  return own;
}

function executionFenceShape(value) {
  return exactKeys(value, ['version', 'action', 'stem', 'expectedState', 'sourceRevision', 'snapshotHash', 'indexStatus', 'leaseId', 'sessionId', 'taskLock']) &&
    value.version === 1 && requests.REQUEST_ACTIONS.has(value.action) &&
    taskSource.safeTaskStem(value.stem) &&
    ['backlog', 'pending', 'todo', 'done', 'corrupt'].includes(value.expectedState) && SHA_RE.test(String(value.sourceRevision || '')) &&
    SHA_RE.test(String(value.snapshotHash || '')) && ['fresh', 'stale', 'invalid'].includes(value.indexStatus) &&
    writerLeases.LEASE_ID_RE.test(String(value.leaseId || '')) && writerLeases.SESSION_ID_RE.test(String(value.sessionId || '')) &&
    value.taskLock === null;
}

function sameFenceAuthority(left, right) {
  if (!executionFenceShape(left) || !executionFenceShape(right)) return false;
  const stableFields = ['version', 'action', 'stem', 'expectedState', 'sourceRevision', 'leaseId', 'sessionId'];
  return stableFields.every((field) => left[field] === right[field]) && stableJson(left.taskLock) === stableJson(right.taskLock);
}

function proveTaskLockAbsent(stem) {
  const lock = locks.lockPresence(stem);
  // `present:false` is returned only after the bounded directory scan and the
  // exact canonical entry inspection both prove absence. Every unreadable,
  // malformed, recovery, symlink or special-file case is projected as
  // `present:true` and therefore remains fail-closed.
  if (!lock || lock.validStem !== true || lock.present !== false) fail('task-lock-absence-unproven');
}

function taskLockRun(args) {
  const env = { ...process.env };
  delete env.NODE_OPTIONS;
  delete env.NODE_PATH;
  const result = childProcess.spawnSync(process.execPath, [TASK_LOCK, ...args], {
    cwd: paths.PROJECT_ROOT,
    env,
    encoding: 'utf8',
    timeout: 30_000,
    maxBuffer: 4 * 1024 * 1024,
    windowsHide: true,
  });
  let value = null;
  try { value = result && result.status === 0 ? JSON.parse(result.stdout) : null; } catch {}
  return { result, value, stderr: String(result && result.stderr || '') };
}

function expectedWithdrawnReservationFinding(item, record, requestId) {
  if (!item || item.code !== 'REQUEST_RESERVATION_RECORD_MISMATCH' || item.stem !== record.stem ||
      !Array.isArray(item.paths) || item.paths.length !== 1) return false;
  const runsRelative = path.relative(paths.PROJECT_ROOT, paths.RUNS_DIR).split(path.sep).join('/');
  const expectedPrefix = `${runsRelative}/.standby-${requestId}-sq-`;
  return item.paths[0].startsWith(expectedPrefix) && item.paths[0].endsWith('/request.claim');
}

function freshExecutionFence(record, receipt, options = {}) {
  exactActiveLease(record, receipt);
  proveTaskLockAbsent(record.stem);
  let result;
  try { result = taskIntegrity.validateAction(record.action, record.stem, 'standby'); }
  catch { fail('task-state-fence-unavailable'); }
  const blockingFindings = taskIntegrity.admissionForAction(result, record.stem).blockers;
  const expectedReservationWithdrawal = options.reservationWithdrawn === true && typeof options.requestId === 'string' &&
    blockingFindings.length === 1 && expectedWithdrawnReservationFinding(blockingFindings[0], record, options.requestId);
  if (!result || (blockingFindings.length > 0 && !expectedReservationWithdrawal) ||
      result.action !== record.action || result.observedState !== record.expectedState ||
      result.sourceRevision !== record.sourceRevision ||
      !['fresh', 'stale', 'invalid'].includes(result.indexStatus) ||
      !SHA_RE.test(String(result.snapshotHash || ''))) {
    fail('task-state-fence-rejected');
  }
  if ((result.runtimeStatus || []).some((row) => row && row.owner === 'finalizations' &&
      (row.kind === 'marker' || row.kind === 'mutex'))) fail('task-state-fence-rejected');
  // Re-scan after the complete task-state snapshot. This closes a lease
  // release/replacement race without trusting the validator's redacted
  // runtime projection as a capability check. `task-lock.mjs acquire` also
  // checks the active standby lease before and after publication, so another
  // lock generation cannot cross this final absence proof while this exact
  // lease remains active.
  exactActiveLease(record, receipt);
  proveTaskLockAbsent(record.stem);
  return {
    version: 1,
    action: record.action,
    stem: record.stem,
    expectedState: record.expectedState,
    sourceRevision: record.sourceRevision,
    snapshotHash: result.snapshotHash,
    indexStatus: result.indexStatus,
    leaseId: receipt.leaseId,
    sessionId: receipt.sessionId,
    taskLock: null,
  };
}

function exactUtc(value) {
  return typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/.test(value) &&
    Number.isFinite(Date.parse(value)) && new Date(value).toISOString() === value;
}

function runnerMarkerStatus(directories, now = Date.now()) {
  const marker = path.join(paths.RUNS_DIR, '.runner-alive');
  const entry = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, paths.RUNS_DIR, marker);
  if (!entry || entry.status === 'missing') return 'absent';
  if (entry.status !== 'present') return 'unknown';
  const bounded = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, paths.RUNS_DIR, marker, RUNNER_MARKER_MAX);
  if (!bounded || bounded.stat.nlink !== '1' || (process.platform !== 'win32' && (bounded.stat.mode & 0o777) !== 0o600)) return 'unknown';
  let value = null;
  try { value = JSON.parse(bounded.bytes.toString('utf8')); } catch {}
  const conforming = value !== null && typeof value === 'object' &&
    exactKeys(value, ['at', 'pid', 'processStartId', 'projectRoot']) && exactUtc(value.at) &&
    value.projectRoot === paths.PROJECT_ROOT &&
    Number.isSafeInteger(value.pid) && value.pid > 0 &&
    (value.processStartId === null || (typeof value.processStartId === 'string' &&
      writerLeases.PROCESS_START_ID_RE.test(value.processStartId)));
  if (!conforming) {
    // Non-conforming CONTENT: a torn mid-refresh read, a pre-processStartId
    // legacy marker, or foreign garbage. A live runner rewrites the file every
    // tick, so fresh mtime is fail-closed 'unknown', while stale mtime proves
    // no live runner refreshes it — 'stale', so a runner-dormant deployment is
    // never wedged forever by a dead runner's leftover bytes. Structural
    // violations (missing, symlink, extra hardlink, wrong mode) above stay
    // hard 'unknown' and never age out.
    const mtimeMs = Number(bounded.stat.mtimeMs);
    if (!Number.isFinite(mtimeMs)) return 'unknown';
    return now - mtimeMs > RUNNER_FRESH_MS ? 'stale' : 'unknown';
  }
  const age = now - Date.parse(value.at);
  if (age < -RUNNER_FUTURE_SKEW_MS) return 'unknown';
  return age <= RUNNER_FRESH_MS ? 'active' : 'stale';
}

function passTokenHash(passToken) {
  return 'sha256:' + crypto.createHash('sha256').update(passToken, 'utf8').digest('hex');
}

function heartbeatBytes(value) {
  return Buffer.from(stableJson(value) + '\n', 'utf8');
}

function writeHeartbeat(passToken) {
  const bytes = heartbeatBytes({
    version: 1,
    projectRoot: paths.PROJECT_ROOT,
    at: new Date().toISOString(),
    state: 'ready',
    passTokenHash: passTokenHash(passToken),
  });
  if (bytes.length > RUNNER_MARKER_MAX || !fileGuards.atomicReplaceRegularFile(
    paths.PROJECT_ROOT,
    paths.WORKER_DIR,
    paths.HEARTBEAT_FILE,
    bytes,
    { create: true, mode: 0o600, maxBytes: RUNNER_MARKER_MAX, directoryMode: 0o700 },
  )) fail('heartbeat-publication-failed');
}

function beginPass() {
  const directories = anchoredDirectories({ worker: true });
  const status = runnerMarkerStatus(directories);
  if (status === 'active') return { status: 'runner-active' };
  if (status === 'unknown') return { status: 'runner-unknown' };
  const passToken = crypto.randomBytes(32).toString('hex');
  writeHeartbeat(passToken);
  return { status: 'ready', passToken };
}

function consumePassToken(directories, passToken, now = Date.now()) {
  const bounded = fileGuards.boundedRegularFileUnder(
    paths.PROJECT_ROOT,
    paths.WORKER_DIR,
    paths.HEARTBEAT_FILE,
    RUNNER_MARKER_MAX,
  );
  if (!bounded || bounded.stat.nlink !== '1' ||
      (process.platform !== 'win32' && (bounded.stat.mode & 0o777) !== 0o600)) fail('pass-token-invalid');
  let value;
  try { value = JSON.parse(bounded.bytes.toString('utf8')); }
  catch { fail('pass-token-invalid'); }
  if (!exactKeys(value, ['at', 'passTokenHash', 'projectRoot', 'state', 'version']) || value.version !== 1 ||
      value.projectRoot !== paths.PROJECT_ROOT || value.state !== 'ready' || !exactUtc(value.at) ||
      !SHA_RE.test(String(value.passTokenHash || '')) ||
      !bounded.bytes.equals(heartbeatBytes(value))) fail('pass-token-invalid');
  const age = now - Date.parse(value.at);
  if (age < -RUNNER_FUTURE_SKEW_MS || age > PASS_FRESH_MS) fail('pass-token-expired');
  const expectedHash = Buffer.from(value.passTokenHash, 'utf8');
  const suppliedHash = Buffer.from(passTokenHash(passToken), 'utf8');
  if (expectedHash.length !== suppliedHash.length || !crypto.timingSafeEqual(expectedHash, suppliedHash)) {
    fail('pass-token-invalid');
  }
  const consumed = {
    ...value,
    at: new Date(now).toISOString(),
    state: 'claimed',
    passTokenHash: passTokenHash(crypto.randomBytes(32).toString('hex')),
  };
  const replacement = heartbeatBytes(consumed);
  const swapped = fileGuards.compareAndSwapRegularFileUnder(
    paths.PROJECT_ROOT,
    paths.WORKER_DIR,
    paths.HEARTBEAT_FILE,
    RUNNER_MARKER_MAX,
    { proof: statProof(bounded.stat), bytes: bounded.bytes },
    replacement,
    { mode: 0o600 },
  );
  if (!swapped || !swapped.ok) fail('pass-token-consume-failed');
}

function runnerExclusionResult(directories) {
  const status = runnerMarkerStatus(directories);
  if (status === 'active') return { status: 'runner-active' };
  if (status === 'unknown') return { status: 'runner-unknown' };
  return null;
}

// Frozen serial safety (pipeline improvement 01, Phase 0): never start a NEW
// claim while any board-task writer lease is active — a site runner session,
// a runner-orphaned child, or another standby execution. Recovery of an
// already-claimed op stays ungated in claimNext so a crashed pass can always
// restore itself (restoring is what releases its own lingering lease). The
// Step-5 guarded acquire stays the authoritative arbiter; this check only
// avoids a claim→refuse→restore churn against a known-occupied workspace.
function taskWriterOccupancyResult() {
  let scan;
  try { scan = writerLeases.scan(paths.WRITER_LEASES_DIR, paths.WRITER_AUTHORITY_ROOT); }
  catch { return { status: 'blocked', code: 'writer-scan-unavailable' }; }
  if (scan.issues.length) return { status: 'blocked', code: 'writer-scan-blocked' };
  return scan.active.some((row) => row.kind === 'task-session')
    ? { status: 'blocked', code: 'task-writer-active' }
    : null;
}

function claimNext(passToken) {
  const directories = anchoredDirectories({ worker: true, create: false });
  consumePassToken(directories, passToken);
  const runnerExclusion = runnerExclusionResult(directories);
  if (runnerExclusion) return runnerExclusion;
  let claimed = boundary(directories, 'claim', { candidate: null, nonce: null });
  if (claimed.status === 'recovered') return { status: 'recovered' };
  if (claimed.status === 'none') {
    const listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, paths.REQUESTS_DIR, DIRECTORY_MAX);
    if (!listed || !listed.ok || !Array.isArray(listed.names)) fail('request-scan-incomplete');
    const names = listed.names.filter((name) => REQUEST_NAME_RE.test(name)).sort();
    if (!names.length) return { status: 'empty' };
    const finalRunnerExclusion = runnerExclusionResult(directories);
    if (finalRunnerExclusion) return finalRunnerExclusion;
    const occupancy = taskWriterOccupancyResult();
    if (occupancy) return occupancy;
    claimed = boundary(directories, 'claim', { candidate: names[0], nonce: crypto.randomBytes(24).toString('hex') });
  }
  if (claimed.status !== 'claimed') {
    if (claimed.status === 'retained' || claimed.status === 'blocked' || claimed.status === 'retry') {
      return { status: claimed.status, code: String(claimed.code || 'queue-boundary-blocked') };
    }
    fail('boundary-claim-envelope-invalid');
  }
  if (!exactKeys(claimed, ['ok', 'status', 'op', 'id', 'bytes', 'claimSha256']) || !SHA_RE.test(String(claimed.claimSha256 || ''))) {
    fail('boundary-claim-envelope-invalid');
  }
  let record;
  try { record = parseClaim(decodeClaimHex(claimed.bytes)); }
  catch (error) {
    boundary(directories, 'mark-invalid', { op: claimed.op, code: 'contract-invalid' });
    return { status: 'retained', code: 'contract-invalid' };
  }
  const requestProjection = projection(claimed.id, record);
  const handle = crypto.randomBytes(32).toString('hex');
  const offered = boundary(directories, 'offer', {
    op: claimed.op,
    handle,
    claimSha256: claimed.claimSha256,
    fingerprint: requestProjection.fingerprint,
    promptHash: requestProjection.promptHash,
    request: requestProjection,
  });
  if (!exactKeys(offered, ['ok', 'status', 'handle', 'request']) || offered.status !== 'offered' || offered.handle !== handle ||
      stableJson(offered.request) !== stableJson(requestProjection)) fail('boundary-offer-envelope-invalid');
  return { status: 'claimed', handle, request: requestProjection };
}

function queueStatus(handle) {
  const loaded = loadHandle(anchoredDirectories(), handle);
  return { status: 'claimed', handle, request: loaded.projection, phases: loaded.loaded.phases };
}

function ensureReservation(handle) {
  const directories = anchoredDirectories();
  const { loaded, record, projection: projected } = loadHandle(directories, handle);
  if (loaded.phases.execution || loaded.phases.disclosed || loaded.phases.restore || loaded.phases.consume) fail('reservation-phase-closed');
  const ensured = requests.ensureRequestReservation(projected.id, record);
  if (!ensured || !ensured.ok || !ensured.handle || ensured.handle.requestId !== projected.id ||
      ensured.handle.stem !== record.stem || ensured.handle.fingerprint !== projected.fingerprint) fail('request-reservation-unavailable');
  return { status: 'reserved', handle: ensured.handle, request: projected };
}

function restore(handle) {
  const directories = anchoredDirectories();
  const { loaded, record, projection: projected } = loadHandle(directories, handle);
  if (loaded.phases.disclosed || loaded.phases.consume) fail('restore-after-disclosure');
  if (!reservationMatches(projected.id, record)) fail('exact-reservation-required');
  // Requeue is authorized only after the writer generation that could have
  // received execution authority is mechanically absent. Otherwise the public
  // request and a still-running writer could become two executable copies.
  proveWriterReleaseBeforeConsume(record, loaded.executionFence, loaded.executionFence !== null);
  const restored = boundary(directories, 'restore', { handle });
  if (restored.status === 'blocked') return { status: 'blocked', code: String(restored.code || 'restore-blocked') };
  if (restored.status !== 'restored' || restored.id !== projected.id) fail('restore-envelope-invalid');
  return { status: 'restored', id: projected.id };
}

function prepareExecution(handle, receipt) {
  const directories = anchoredDirectories();
  const { loaded, record, projection: projected } = loadHandle(directories, handle);
  if (loaded.phases.disclosed || loaded.phases.restore || loaded.phases.consume) fail('execution-phase-closed');
  if (!reservationMatches(projected.id, record)) fail('exact-reservation-required');
  const fence = freshExecutionFence(record, receipt);
  if (loaded.executionFence !== null && !sameFenceAuthority(loaded.executionFence, fence)) fail('execution-fence-lineage-mismatch');
  const phase = boundary(directories, 'phase', { handle, phase: 'execution', fence });
  if (phase.status !== 'execution' && phase.status !== 'already-execution') fail('execution-receipt-failed');
  if (!executionFenceShape(phase.fence) || !sameFenceAuthority(phase.fence, fence)) fail('execution-fence-receipt-mismatch');
  return { status: 'execution-prepared', request: projected };
}

function readPrompt(handle, receipt) {
  const directories = anchoredDirectories();
  const { loaded, record, projection: projected } = loadHandle(directories, handle);
  if (!loaded.phases.execution) fail('execution-receipt-required');
  if (loaded.phases.disclosed) fail('prompt-already-disclosed');
  if (loaded.phases.restore || loaded.phases.consume) fail('prompt-phase-closed');
  if (!reservationMissing(record)) fail('reservation-still-active');
  const fence = freshExecutionFence(record, receipt, {
    reservationWithdrawn: true,
    requestId: projected.id,
  });
  if (!sameFenceAuthority(loaded.executionFence, fence)) fail('execution-fence-lineage-mismatch');
  const phase = boundary(directories, 'phase', { handle, phase: 'disclosed', fence });
  if (phase.status !== 'disclosed') fail('prompt-disclosure-ambiguous');
  if (!executionFenceShape(phase.fence) || stableJson(phase.fence) !== stableJson(fence)) fail('disclosure-fence-receipt-mismatch');
  return { raw: record.prompt };
}

function proveWriterReleaseBeforeConsume(record, fence, exact) {
  let scan;
  try { scan = writerLeases.scan(paths.WRITER_LEASES_DIR, paths.WRITER_AUTHORITY_ROOT); }
  catch { fail('writer-lease-release-unproven'); }
  if (!scan || !Array.isArray(scan.active) || !Array.isArray(scan.stale) || !Array.isArray(scan.issues) || scan.issues.length) {
    fail('writer-lease-release-unproven');
  }
  const rows = scan.active.concat(scan.stale);
  if (exact && (!executionFenceShape(fence) || rows.some((row) => row.leaseId === fence.leaseId ||
      row.sessionId === fence.sessionId && row.stem === record.stem))) {
    fail('writer-lease-release-unproven');
  }
  if (!exact && rows.some((row) => row.kind === 'task-session' && row.stem === record.stem &&
      row.key === `standby:${record.action}`)) fail('writer-lease-release-unproven');
}

function proveExecutedTaskLockSettlement(record, fence, requestId) {
  if (record.action !== 'run') {
    proveTaskLockAbsent(record.stem);
    return;
  }
  const presence = locks.lockPresence(record.stem);
  if (presence && presence.validStem === true && presence.present === false) return;
  if (!presence || presence.validStem !== true || presence.present !== true || presence.recovery === true ||
      presence.safe !== true || presence.readable !== true) fail('task-lock-settlement-unproven');
  const inspected = taskLockRun(['inspect', '--stem', record.stem]);
  const value = inspected.value;
  const owner = value && value.owner;
  if (!inspected.result || inspected.result.status !== 0 || !value || value.version !== 1 || value.ok !== true ||
      value.stem !== record.stem || value.stage !== 'orchestrator' ||
      !/^[A-Za-z0-9][A-Za-z0-9._:-]{7,159}$/.test(String(value.runId || '')) ||
      value.sessionId !== fence.sessionId || !SHA_RE.test(String(value.lockHash || '')) || !exactUtc(value.startedAt) ||
      !owner || owner.kind !== 'standby' || owner.id !== `standby:${fence.leaseId}` ||
      !Number.isSafeInteger(owner.pid) || owner.pid <= 0 || owner.hostname !== os.hostname() ||
      owner.startedAt !== value.startedAt || !writerLeases.PROCESS_START_ID_RE.test(String(owner.processStartId || ''))) {
    fail('task-lock-settlement-unproven');
  }
  let canonical;
  try { canonical = taskIntegrity.validateAction('run', record.stem, 'standby-consume'); }
  catch { fail('task-lock-settlement-unproven'); }
  const blockers = taskIntegrity.actionAdmission(canonical).blockers;
  const unexpected = blockers.filter((item) => !expectedWithdrawnReservationFinding(item, record, requestId));
  if (!canonical || canonical.observedState !== 'todo' || canonical.sourceRevision !== record.sourceRevision ||
      !SHA_RE.test(String(canonical.snapshotHash || '')) || unexpected.length) {
    fail('task-lock-settlement-unproven');
  }
}

function consume(handle, kind) {
  const directories = anchoredDirectories();
  const { loaded, record, projection: projected } = loadHandle(directories, handle);
  if (!reservationMissing(record)) fail('reservation-still-active');
  if (kind === 'executed') {
    if (!loaded.phases.disclosed) fail('disclosure-receipt-required');
    proveWriterReleaseBeforeConsume(record, loaded.disclosureFence, true);
    proveExecutedTaskLockSettlement(record, loaded.disclosureFence, projected.id);
  } else if (kind === 'superseded') {
    const tombstone = requests.readSupersededFile(projected.id);
    if (!tombstone || tombstone.requestId !== projected.id || tombstone.stem !== record.stem || tombstone.action !== record.action ||
        tombstone.expectedState !== record.expectedState || tombstone.expectedSourceRevision !== record.sourceRevision || tombstone.admittedAt !== record.createdAt) {
      fail('exact-superseded-receipt-required');
    }
    proveWriterReleaseBeforeConsume(record, null, false);
  } else fail('bad-consume-kind', 64);
  const result = boundary(directories, 'consume', { handle, kind });
  if (result.status !== 'consumed' || result.id !== projected.id || result.kind !== kind) fail('consume-envelope-invalid');
  return { status: 'consumed', id: projected.id, kind };
}

function canonicalSupersessionVerdict(record, receipt, finalizationOnly) {
  if (!finalizationOnly) {
    exactActiveLease(record, receipt);
    proveTaskLockAbsent(record.stem);
  }
  let result;
  try { result = taskIntegrity.validateAction(record.action, record.stem, 'standby-supersession'); }
  catch { fail('supersession-authority-unavailable'); }
  if (!result || !SHA_RE.test(String(result.snapshotHash || '')) ||
      (result.observedState !== null && !['backlog', 'pending', 'todo', 'done', 'corrupt'].includes(result.observedState)) ||
      (result.sourceRevision !== null && !SHA_RE.test(String(result.sourceRevision || ''))) || !Array.isArray(result.findings)) {
    fail('supersession-authority-invalid');
  }
  if (!finalizationOnly) {
    exactActiveLease(record, receipt);
    proveTaskLockAbsent(record.stem);
  }
  const publicResult = taskIntegrity.publicResult(result);
  const projectedFindings = publicResult.findings.map((item) => ({
    code: item.code,
    severity: item.severity,
    paths: item.paths.slice(0, 20),
  }));
  const findings = projectedFindings.filter((item) => item.severity === 'error' || item.severity === 'blocker')
    .concat(projectedFindings.filter((item) => item.severity !== 'error' && item.severity !== 'blocker')).slice(0, 30);
  let reason;
  if (finalizationOnly) {
    const markers = (result.runtimeStatus || []).filter((row) => row && row.owner === 'finalizations' &&
      row.kind === 'marker' && row.stem === record.stem && row.state === 'running');
    if (markers.length !== 1) fail('finalization-supersession-unproven');
    reason = 'finalization-active';
  } else if (result.observedState !== record.expectedState) {
    reason = 'state-changed';
  } else if (result.sourceRevision !== record.sourceRevision) {
    reason = 'source-revision-changed';
  } else if (!taskIntegrity.admissionForAction(result, record.stem).ok) {
    if (!findings.some((item) => item.severity === 'error' || item.severity === 'blocker')) {
      fail('supersession-authority-invalid');
    }
    reason = 'task-integrity-invalid';
  } else {
    fail('supersession-not-proven');
  }
  return {
    reason,
    observedState: result.observedState,
    sourceRevision: result.sourceRevision,
    snapshotHash: result.snapshotHash,
    findings,
  };
}

function recordSuperseded(handle, receipt, finalizationOnly = false) {
  const directories = anchoredDirectories();
  const { loaded, record, projection: projected } = loadHandle(directories, handle);
  if (loaded.phases.execution || loaded.phases.disclosed || loaded.phases.restore || loaded.phases.consume) fail('superseded-phase-closed');
  if (!reservationMatches(projected.id, record)) fail('exact-reservation-required');
  const existing = requests.readSupersededFile(projected.id);
  if (existing) {
    if (existing.requestId !== projected.id || existing.stem !== record.stem || existing.action !== record.action ||
        existing.expectedState !== record.expectedState || existing.expectedSourceRevision !== record.sourceRevision ||
        existing.admittedAt !== record.createdAt) fail('superseded-receipt-lineage-mismatch');
    return { status: 'superseded-recorded', id: projected.id, reason: existing.reason };
  }
  const verdict = canonicalSupersessionVerdict(record, receipt, finalizationOnly);
  const reason = verdict.reason;
  const tombstone = {
    version: 1,
    status: 'superseded',
    requestId: projected.id,
    action: record.action,
    stem: record.stem,
    reason,
    expectedState: record.expectedState,
    observedState: verdict.observedState,
    expectedSourceRevision: record.sourceRevision,
    observedSourceRevision: verdict.sourceRevision,
    admittedAt: record.createdAt,
    supersededAt: new Date().toISOString(),
    snapshotHash: verdict.snapshotHash,
    findings: verdict.findings,
  };
  if (requests.supersededRecordIssue(tombstone, projected.id) || !requests.writeSupersededFile(projected.id, tombstone)) {
    fail('superseded-receipt-publication-failed');
  }
  testCrash('superseded-after-publication');
  return { status: 'superseded-recorded', id: projected.id, reason };
}

function parseArgs(argv) {
  if (!Array.isArray(argv) || argv.length < 1 || argv.length > 9 || argv.join('\0').length > 8192) fail('usage', 64);
  const command = argv[0];
  const options = {};
  for (let index = 1; index < argv.length; index += 2) {
    const key = argv[index], value = argv[index + 1];
    if (!/^--[a-z-]+$/.test(String(key || '')) || value === undefined || Object.hasOwn(options, key)) fail('usage', 64);
    options[key] = value;
  }
  if (command === 'begin-pass' && Object.keys(options).length === 0) return { command, options };
  if (command === 'claim-next' && exactKeys(options, ['--pass-token']) && PASS_TOKEN_RE.test(String(options['--pass-token'] || ''))) {
    return { command, options };
  }
  if (['status', 'ensure-reservation', 'restore'].includes(command) &&
      exactKeys(options, ['--handle']) && HANDLE_RE.test(options['--handle'])) return { command, options };
  if (['prepare-execution', 'read-prompt'].includes(command) &&
      exactKeys(options, ['--handle', '--lease-id', '--lease-token', '--session-id']) && HANDLE_RE.test(options['--handle']) &&
      writerLeases.LEASE_ID_RE.test(String(options['--lease-id'] || '')) && TOKEN_RE.test(String(options['--lease-token'] || '')) &&
      writerLeases.SESSION_ID_RE.test(String(options['--session-id'] || ''))) return { command, options };
  if (command === 'consume' && exactKeys(options, ['--handle', '--kind']) && HANDLE_RE.test(options['--handle']) &&
      ['executed', 'superseded'].includes(options['--kind'])) return { command, options };
  if (command === 'record-superseded' &&
      exactKeys(options, ['--handle', '--lease-id', '--lease-token', '--session-id']) && HANDLE_RE.test(options['--handle']) &&
      writerLeases.LEASE_ID_RE.test(String(options['--lease-id'] || '')) && TOKEN_RE.test(String(options['--lease-token'] || '')) &&
      writerLeases.SESSION_ID_RE.test(String(options['--session-id'] || ''))) return { command, options };
  if (command === 'record-finalization-superseded' && exactKeys(options, ['--handle']) && HANDLE_RE.test(options['--handle'])) {
    return { command, options };
  }
  fail('usage', 64);
}

export function runCli(argv) {
  const parsed = parseArgs(argv);
  const handle = parsed.options['--handle'];
  if (parsed.command === 'begin-pass') return beginPass();
  if (parsed.command === 'claim-next') return claimNext(parsed.options['--pass-token']);
  if (parsed.command === 'status') return queueStatus(handle);
  if (parsed.command === 'ensure-reservation') return ensureReservation(handle);
  if (parsed.command === 'restore') return restore(handle);
  const leaseReceipt = ['prepare-execution', 'read-prompt', 'record-superseded'].includes(parsed.command) ? {
    leaseId: parsed.options['--lease-id'], token: parsed.options['--lease-token'], sessionId: parsed.options['--session-id']
  } : null;
  if (parsed.command === 'prepare-execution') return prepareExecution(handle, leaseReceipt);
  if (parsed.command === 'read-prompt') return readPrompt(handle, leaseReceipt);
  if (parsed.command === 'consume') return consume(handle, parsed.options['--kind']);
  if (parsed.command === 'record-superseded') return recordSuperseded(handle, leaseReceipt);
  if (parsed.command === 'record-finalization-superseded') return recordSuperseded(handle, null, true);
  fail('usage', 64);
}

function publicError(error) {
  const code = error instanceof QueueError && /^[a-z0-9-]{1,80}$/.test(error.code) ? error.code : 'standby-queue-internal-error';
  return { status: 'error', code };
}

function isMain() {
  return typeof process.argv[1] === 'string' && import.meta.url === pathToFileURL(path.resolve(process.argv[1])).href;
}

if (isMain()) {
  try {
    const result = runCli(process.argv.slice(2));
    if (result && Object.hasOwn(result, 'raw')) process.stdout.write(result.raw);
    else process.stdout.write(JSON.stringify(result) + '\n');
  } catch (error) {
    process.stdout.write(JSON.stringify(publicError(error)) + '\n');
    process.exitCode = error instanceof QueueError ? error.exitCode : 2;
  }
}
