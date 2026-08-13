#!/usr/bin/env node

// Canonical owner for `.cache/tasks/locks/<stem>.json`.
//
// A task lock is a runtime ownership projection, not durable task state.  This
// helper deliberately does not repair columns, infer abandonment from age, or
// overwrite an existing owner.  It validates the canonical task state before
// acquisition, publishes with no-clobber semantics, reads bounded regular
// files with O_NOFOLLOW, and requires the exact run/session/hash tuple for a
// normal release.  A release interrupted after its atomic detach can be
// completed only by `recover-release` with the exact run/session/hash tuple.
// A dead Site owner can be recovered through the separate two-phase
// `owner-status` -> `recover-owner` path.  That path never trusts age or an
// ended transcript: it requires an exact local process-generation verdict, no
// conflicting writer tree, an exact lock hash, and a live lock-writer lease
// that participates in the global finalizer handshake.
// `finalize-task.mjs` remains the only happy-path releaser for orchestrator
// locks and uses its stronger transaction-owned lock snapshot.

import crypto from 'node:crypto';
import os from 'node:os';
import path from 'node:path';
import { spawn, spawnSync } from 'node:child_process';
import { createRequire } from 'node:module';
import { dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const require = createRequire(import.meta.url);
const core = require('./task-state-core.cjs');
const writerLeases = require('./writer-leases.cjs');
const platformSupport = require('./platform-support.cjs');

const HERE = dirname(fileURLToPath(import.meta.url));
const PROJECT_ROOT = path.resolve(process.env.ORCHESTRATOR_PROJECT_ROOT || path.join(HERE, '..', '..'));
const TASKS_DIR = path.resolve(process.env.ORCHESTRATOR_TASKS_DIR || HERE);
const LOCKS_DIR = path.resolve(process.env.ORCHESTRATOR_LOCKS_DIR || path.join(PROJECT_ROOT, 'orchestrator', '.cache', 'tasks', 'locks'));
const WRITER_LEASES_DIR = path.resolve(process.env.ORCHESTRATOR_WRITER_LEASES_DIR ||
  path.join(PROJECT_ROOT, 'orchestrator', '.cache', 'tasks', 'finalizations', '.writers'));
const WRITER_AUTHORITY_ROOT = path.resolve(process.env.ORCHESTRATOR_WRITER_AUTHORITY_ROOT || PROJECT_ROOT);
const VERSION = 1;
const MAX_BYTES = 32 * 1024;
const RUN_ID_RE = /^[A-Za-z0-9][A-Za-z0-9._:-]{7,159}$/;
const SESSION_ID_RE = /^ws-[A-Za-z0-9][A-Za-z0-9._-]{15,159}$/;
const HASH_RE = /^sha256:[a-f0-9]{64}$/;
const WRITER_TOKEN_RE = /^[a-f0-9]{32,128}$/;
const OWNER_KINDS = new Set(['site', 'standby', 'direct', 'agent']);
const STAGES = new Set(['task-prep', 'orchestrator']);
const STAGE_STATES = Object.freeze({
  'task-prep': Object.freeze(['backlog', 'pending']),
  orchestrator: Object.freeze(['todo']),
});
// A standby worker owns prep/answers capabilities only: since per-task
// worktree isolation (pipeline improvement 01, Phase 2) a `run` executes
// exclusively inside the site runner's provisioned worktree, so no standby
// capability may ever take an orchestrator-stage lock.
const STANDBY_KEYS_BY_STAGE = Object.freeze({
  'task-prep': Object.freeze(new Set(['standby:prep', 'standby:answers'])),
  orchestrator: Object.freeze(new Set()),
});
const RECORD_FIELDS = Object.freeze(['owner', 'runId', 'sessionId', 'stage', 'startedAt', 'stem', 'version']);
const OWNER_FIELDS = Object.freeze(['hostname', 'id', 'kind', 'pid', 'processStartId', 'startedAt']);
const MAX_RUNTIME_ENTRIES = 10000;
const MAX_COMPLETED_RELEASES_PER_STEM = 256;
const FS_BOUNDARY = path.join(HERE, 'anchored-task-fs.py');
const MUTEX_HELPER = path.join(HERE, 'finalize-lock.py');
const CANONICAL_PROJECT_ROOT = path.resolve(HERE, '..', '..');
const DECIMAL_STAT_RE = /^(?:0|[1-9][0-9]*)$/;

class LockError extends Error {
  constructor(code, message, exitCode = 1, details = null) {
    super(message);
    this.name = 'LockError';
    this.code = code;
    this.exitCode = exitCode;
    this.details = details;
  }
}

function fail(code, message, exitCode = 1, details = null) {
  throw new LockError(code, message, exitCode, details);
}

function observedValidation(options, caller, fn) {
  const started = process.hrtime.bigint();
  let result, thrown;
  try { result = fn(); return result; }
  catch (error) { thrown = error; throw error; }
  finally {
    const durationMs = Number((process.hrtime.bigint() - started) / 1000000n);
    const rawCode = String(thrown && thrown.code || 'TASK_STATE_UNAVAILABLE');
    const code = /^[A-Za-z0-9_.:-]{1,80}$/.test(rawCode) ? rawCode : 'TASK_STATE_UNAVAILABLE';
    const eventResult = result || {
      version: 1, ok: false, overallOk: false,
      scope: options && options.stem || null,
      action: options && options.action || null,
      transition: options && options.transition || null,
      phase: options && options.phase || null,
      observedState: null,
      expectedState: options && options.expect || null,
      snapshotHash: null,
      findings: [{ code, severity: 'blocker' }],
      stats: { durationMs, scanMode: null, taskBodyReads: 0 },
    };
    const measured = {
      ...eventResult,
      scope: eventResult.scope || options && options.stem || null,
      action: eventResult.action || options && options.action || null,
      transition: eventResult.transition || options && options.transition || null,
      phase: eventResult.phase || options && options.phase || null,
      expectedState: eventResult.expectedState || options && options.expect || null,
      stats: { ...(eventResult.stats || {}), durationMs },
    };
    process.stderr.write('[task-state] ' + JSON.stringify(core.observationFor(measured, {
      caller: caller || 'task-prep', slowThresholdMs: 100,
    })) + '\n');
  }
}

function now() { return new Date().toISOString(); }
function randomId(prefix) { return `${prefix}-${crypto.randomBytes(18).toString('hex')}`; }
function sha256(bytes) { return `sha256:${crypto.createHash('sha256').update(bytes).digest('hex')}`; }
function fixturePause(name) {
  if (!fixtureBoundaryAllowed()) return;
  const raw = process.env[name];
  if (!/^[1-9][0-9]{0,3}$/.test(String(raw || ''))) return;
  const delayMs = Number(raw);
  if (delayMs > 5000) return;
  Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, delayMs);
}
function sortedKeys(value) { return Object.keys(value).sort(); }
function sameFields(value, fields) {
  const keys = sortedKeys(value);
  return keys.length === fields.length && keys.every((key, index) => key === fields[index]);
}
function validIso(value) {
  return typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/.test(value) &&
    Number.isFinite(Date.parse(value)) && new Date(value).toISOString() === value;
}
function boundedText(value, max) {
  return typeof value === 'string' && value.length > 0 && value.length <= max && !/[\0\r\n]/.test(value);
}
function helperEnv() {
  const env = { ...process.env };
  // The JS process consumes the exact writer capability.  Filesystem/mutex
  // helpers need only paths and test failpoints, so never copy plaintext
  // capabilities into their environments.
  for (const key of ['ORCHESTRATOR_WRITER_SESSION_ID', 'ORCHESTRATOR_WRITER_STEM',
    'ORCHESTRATOR_WRITER_LEASE_ID', 'ORCHESTRATOR_WRITER_LEASE_TOKEN',
    'ORCHESTRATOR_WRITER_DELEGATION_TOKEN']) delete env[key];
  return env;
}
function validPid(value) { return Number.isInteger(value) && value > 0 && value <= 0x7fffffff; }
function sameStat(left, right) {
  return !!left && !!right && left.dev === right.dev && left.ino === right.ino && left.mode === right.mode &&
    left.size === right.size && left.mtimeNs === right.mtimeNs && left.ctimeNs === right.ctimeNs;
}
function sameMovedStat(left, right) {
  return !!left && !!right && left.dev === right.dev && left.ino === right.ino && left.mode === right.mode &&
    left.size === right.size && left.mtimeNs === right.mtimeNs;
}

function pathWithin(root, target) {
  const rel = path.relative(path.resolve(root), path.resolve(target));
  return rel === '' || (rel !== '..' && !rel.startsWith(`..${path.sep}`) && !path.isAbsolute(rel));
}

function fixtureBoundaryAllowed() {
  const fixture = process.env.TASK_FS_TEST_ROOT ? path.resolve(process.env.TASK_FS_TEST_ROOT) : null;
  const temp = path.resolve(os.tmpdir());
  return !!fixture && fixture !== temp && fixture !== CANONICAL_PROJECT_ROOT &&
    pathWithin(temp, fixture) && pathWithin(fixture, PROJECT_ROOT) && !pathWithin(CANONICAL_PROJECT_ROOT, fixture);
}

function boundary(request, maxBuffer = 1024 * 1024) {
  const run = spawnSync('python3', [FS_BOUNDARY], {
    input: JSON.stringify({ ...request, version: 1, canonicalRoot: CANONICAL_PROJECT_ROOT, fixture: fixtureBoundaryAllowed() }),
    encoding: 'utf8', timeout: 15000, maxBuffer, env: helperEnv(),
  });
  if (run.error || run.status !== 0) fail('LOCK_BOUNDARY_FAILED', String(run.error && run.error.message || run.stderr || 'filesystem helper failed').slice(0, 500), 3);
  let value;
  try { value = JSON.parse(run.stdout); }
  catch (_) { fail('LOCK_BOUNDARY_FAILED', 'filesystem helper returned invalid JSON', 3); }
  if (!value || value.version !== 1 || typeof value.ok !== 'boolean') fail('LOCK_BOUNDARY_FAILED', 'filesystem helper returned an invalid envelope', 3);
  if (!value.ok) {
    const error = new Error(String(value.error && value.error.message || 'filesystem boundary failed'));
    error.code = value.error && value.error.code || 'BOUNDARY_FAILED';
    throw error;
  }
  return value.result;
}

function statFromProof(raw) {
  if (!raw || typeof raw !== 'object' || !DECIMAL_STAT_RE.test(String(raw.dev || '')) ||
      !DECIMAL_STAT_RE.test(String(raw.ino || '')) || !DECIMAL_STAT_RE.test(String(raw.mtimeNs || '')) ||
      !DECIMAL_STAT_RE.test(String(raw.ctimeNs || '')) || !Number.isSafeInteger(raw.mode) ||
      !Number.isSafeInteger(raw.size) || !['file', 'directory', 'symlink', 'other'].includes(raw.kind)) {
    fail('LOCK_BOUNDARY_FAILED', 'filesystem helper returned a malformed exact stat proof', 3);
  }
  const mode = raw.mode;
  return Object.freeze({
    dev: String(raw.dev), ino: String(raw.ino), mode, size: raw.size,
    mtimeNs: String(raw.mtimeNs), ctimeNs: String(raw.ctimeNs),
    mtimeMs: Number(BigInt(raw.mtimeNs) / 1000000n), ctimeMs: Number(BigInt(raw.ctimeNs) / 1000000n),
    isFile: () => (mode & 0o170000) === 0o100000,
    isDirectory: () => (mode & 0o170000) === 0o040000,
    isSymbolicLink: () => (mode & 0o170000) === 0o120000,
  });
}

function validateRecord(value, expectedStem = null) {
  if (!value || typeof value !== 'object' || Array.isArray(value) || !sameFields(value, RECORD_FIELDS)) {
    return 'lock must contain exactly the canonical v1 fields';
  }
  if (value.version !== VERSION) return 'lock version is unsupported';
  if (core.safeIntegerId(value.stem) === null || (expectedStem && value.stem !== expectedStem)) return 'lock stem is invalid';
  if (!STAGES.has(value.stage)) return 'lock stage is invalid';
  if (!RUN_ID_RE.test(String(value.runId || ''))) return 'lock runId is invalid';
  if (!SESSION_ID_RE.test(String(value.sessionId || ''))) return 'lock sessionId is invalid';
  if (!validIso(value.startedAt)) return 'lock startedAt is invalid';
  if (!value.owner || typeof value.owner !== 'object' || Array.isArray(value.owner) || !sameFields(value.owner, OWNER_FIELDS)) {
    return 'lock owner must contain exactly the canonical v1 fields';
  }
  if (!OWNER_KINDS.has(value.owner.kind)) return 'lock owner kind is invalid';
  if (!boundedText(value.owner.id, 240)) return 'lock owner id is invalid';
  if (!validPid(value.owner.pid)) return 'lock owner pid is invalid';
  if (value.owner.processStartId !== null && !writerLeases.PROCESS_START_ID_RE.test(String(value.owner.processStartId || ''))) {
    return 'lock owner processStartId is invalid';
  }
  if (!boundedText(value.owner.hostname, 255)) return 'lock owner hostname is invalid';
  if (value.owner.startedAt !== value.startedAt) return 'lock owner timestamp must match startedAt';
  return null;
}

function ensureInside(root, target, label) {
  const rel = path.relative(root, target);
  if (rel === '..' || rel.startsWith(`..${path.sep}`) || path.isAbsolute(rel)) {
    fail('LOCK_PATH_OUTSIDE_ROOT', `${label} escapes the project root`, 2);
  }
}

function ensureSafeDir(dir, create = true) {
  ensureInside(PROJECT_ROOT, dir, 'runtime directory');
  try {
    if (create) boundary({ action: 'ensure-dir', path: dir, authorityRoot: PROJECT_ROOT });
    else boundary({ action: 'list', path: dir, authorityRoot: PROJECT_ROOT, maxEntries: 1 });
  } catch (error) {
    if (error && error.code === 'PATH_MISSING' && !create) fail('LOCK_NOT_FOUND', 'task lock directory does not exist', 1);
    if (error && ['DIRECTORY_UNSAFE', 'PATH_UNSAFE', 'PATH_OUTSIDE_AUTHORITY'].includes(error.code)) {
      fail('LOCK_DIRECTORY_UNSAFE', 'every lock-directory component must be a real directory', 3);
    }
    if (error && ['DIRECTORY_CHANGED', 'ENTRY_CHANGED'].includes(error.code)) fail('LOCK_DIRECTORY_CHANGED', 'task-lock directory changed', 4);
    throw error;
  }
}

function lockPath(stem) {
  if (core.safeIntegerId(stem) === null) fail('INVOCATION_INVALID', '--stem must be a canonical TASK_<N>_<slug>', 2);
  const target = path.join(LOCKS_DIR, `${stem}.json`);
  ensureInside(PROJECT_ROOT, target, 'lock path');
  return target;
}

function mutexPath(stem) {
  if (core.safeIntegerId(stem) === null) fail('INVOCATION_INVALID', '--stem must be a canonical TASK_<N>_<slug>', 2);
  const target = path.join(LOCKS_DIR, `.${stem}.task-lock.mutex`);
  ensureInside(PROJECT_ROOT, target, 'task-lock mutex path');
  return target;
}

async function acquireStemMutex(stem) {
  ensureSafeDir(LOCKS_DIR);
  const file = mutexPath(stem);
  const invocationId = `task-lock-${crypto.randomBytes(18).toString('hex')}`;
  const python = process.env.TASK_LOCK_MUTEX_PYTHON || process.env.FINALIZE_LOCK_PYTHON || 'python3';
  const helper = path.resolve(process.env.TASK_LOCK_MUTEX_HELPER || MUTEX_HELPER);
  const child = spawn(python, [helper, file, invocationId, PROJECT_ROOT], {
    cwd: PROJECT_ROOT,
    env: helperEnv(),
    stdio: ['pipe', 'pipe', 'pipe'],
  });
  const lease = { child, stem, file, invocationId, lost: false, releasing: false, closed: false };
  child.stdin.on('error', () => {
    if (!lease.releasing) lease.lost = true;
    try { child.kill('SIGTERM'); } catch (_) {}
  });
  const configured = Number(process.env.TASK_LOCK_MUTEX_WAIT_MS || 300000);
  const waitMs = Number.isFinite(configured) ? Math.max(1, Math.min(60 * 60 * 1000, Math.floor(configured))) : 300000;
  return await new Promise((resolveLock, rejectLock) => {
    let stdout = '', stderr = '', settled = false;
    const timer = setTimeout(() => {
      if (settled) return;
      settled = true;
      try { child.kill('SIGKILL'); } catch (_) {}
      rejectLock(new LockError('LOCK_MUTEX_BUSY', `task-lock mutex was not acquired within ${waitMs}ms`, 4));
    }, waitMs);
    const rejectOnce = (error) => {
      if (settled) return;
      settled = true; clearTimeout(timer);
      try { child.kill('SIGKILL'); } catch (_) {}
      rejectLock(error);
    };
    child.on('error', (error) => rejectOnce(new LockError('LOCK_MUTEX_HELPER_FAILED', `task-lock mutex helper could not start: ${error.message}`, 3)));
    child.stderr.on('data', (chunk) => { stderr = String(stderr + chunk).slice(-2000); });
    child.stdout.on('data', (chunk) => {
      if (settled) return;
      stdout = String(stdout + chunk).slice(-2000);
      if (!stdout.includes('LOCKED\n')) return;
      settled = true; clearTimeout(timer); resolveLock(lease);
    });
    child.on('close', (code, signal) => {
      lease.closed = true;
      if (!lease.releasing) lease.lost = true;
      if (!settled) rejectOnce(new LockError('LOCK_MUTEX_HELPER_FAILED',
        `task-lock mutex helper exited before acquisition (${code == null ? signal : code})${stderr ? `: ${stderr}` : ''}`, 3));
    });
  });
}

function assertStemMutexHeld(lease) {
  const child = lease && lease.child;
  if (!child || lease.lost || lease.closed || child.exitCode !== null || child.signalCode !== null || child.killed || !Number.isInteger(child.pid)) {
    fail('LOCK_MUTEX_OWNERSHIP_LOST', 'task-lock mutex helper exited or ownership became uncertain', 4);
  }
  try { process.kill(child.pid, 0); }
  catch (error) {
    if (!error || error.code !== 'EPERM') fail('LOCK_MUTEX_OWNERSHIP_LOST', 'task-lock mutex helper is no longer alive', 4);
  }
  let record;
  try {
    const read = safeReadBytes(lease.file, 4096);
    record = JSON.parse(new TextDecoder('utf-8', { fatal: true }).decode(read.bytes));
  } catch (error) {
    fail('LOCK_MUTEX_OWNERSHIP_LOST', 'task-lock mutex ownership record is unreadable or unstable', 4);
  }
  const fields = ['hostname', 'invocationId', 'pid', 'processStartId', 'released', 'startedAt', 'version'];
  if (!record || !sameFields(record, fields) || record.version !== 1 || record.released !== false ||
      record.invocationId !== lease.invocationId || record.pid !== child.pid ||
      !writerLeases.PROCESS_START_ID_RE.test(String(record.processStartId || '')) ||
      !writerLeases.processIdentityMatches(record.pid, record.processStartId)) {
    fail('LOCK_MUTEX_OWNERSHIP_LOST', 'task-lock mutex record no longer identifies this invocation', 4);
  }
}

async function releaseStemMutex(lease) {
  if (!lease || !lease.child) return;
  lease.releasing = true;
  const child = lease.child;
  if (lease.closed || child.exitCode !== null || child.signalCode !== null) return;
  await new Promise((resolveRelease) => {
    let done = false, timer;
    const finish = () => { if (!done) { done = true; if (timer) clearTimeout(timer); resolveRelease(); } };
    child.once('close', finish);
    try { child.stdin.end('RELEASE\n'); } catch (_) { try { child.kill('SIGTERM'); } catch (_) {} }
    timer = setTimeout(() => { try { child.kill('SIGKILL'); } catch (_) {}; finish(); }, 2000);
    if (typeof timer.unref === 'function') timer.unref();
  });
}

async function withStemMutex(stem, operation) {
  const lease = await acquireStemMutex(stem);
  try {
    assertStemMutexHeld(lease);
    const result = await operation(lease);
    assertStemMutexHeld(lease);
    return result;
  } finally {
    await releaseStemMutex(lease);
  }
}

function exactStatRequest(stat) {
  return stat ? { dev: stat.dev, ino: stat.ino, mode: stat.mode, size: stat.size,
    mtimeNs: stat.mtimeNs, ctimeNs: stat.ctimeNs } : null;
}

function safeReadBytes(file, maxBytes = MAX_BYTES, expectedParent = null, expectedFile = null) {
  let raw;
  try {
    raw = boundary({ action: 'read', path: file, authorityRoot: PROJECT_ROOT, maxBytes,
      expectedParent: expectedParent ? { dev: expectedParent.dev, ino: expectedParent.ino } : null,
      expectedFile: exactStatRequest(expectedFile) }, Math.ceil(maxBytes * 1.5) + 1024 * 1024);
    const live = statFromProof(raw.stat);
    if (raw.unsafe || !live.isFile() || live.isSymbolicLink()) fail('LOCK_UNSAFE', 'ownership artifact must be a regular non-symlink file', 3);
    if (raw.tooLarge) fail('LOCK_TOO_LARGE', `ownership artifact exceeds ${maxBytes} bytes`, 3);
    if (typeof raw.rawBase64 !== 'string') fail('LOCK_BOUNDARY_FAILED', 'filesystem helper omitted lock bytes', 3);
    const bytes = Buffer.from(raw.rawBase64, 'base64');
    if (bytes.length !== live.size) fail('LOCK_CHANGED', 'task lock changed while reading', 4);
    return { bytes, stat: live };
  } catch (error) {
    if (error instanceof LockError) throw error;
    if (error && error.code === 'PATH_MISSING') fail('LOCK_NOT_FOUND', 'task lock does not exist', 1);
    if (error && ['DIRECTORY_CHANGED', 'ENTRY_CHANGED', 'ELOOP', 'ENOENT', 'ESTALE'].includes(error.code)) {
      fail('LOCK_CHANGED', 'task lock changed during bounded read', 4);
    }
    throw error;
  }
}

function safeRead(file, expectedStem, expectedParent = null, expectedFile = null) {
  try {
    const { bytes, stat: live } = safeReadBytes(file, MAX_BYTES, expectedParent, expectedFile);
    let text;
    try { text = new TextDecoder('utf-8', { fatal: true }).decode(bytes); }
    catch (_) { fail('LOCK_INVALID', 'task lock is not canonical UTF-8', 3); }
    if (text.includes('\0')) fail('LOCK_INVALID', 'task lock contains a NUL byte', 3);
    let value;
    try { value = JSON.parse(text); }
    catch (_) { fail('LOCK_INVALID', 'task lock is not valid JSON', 3); }
    const issue = validateRecord(value, expectedStem);
    if (issue) fail('LOCK_INVALID', issue, 3);
    return { value, bytes, hash: sha256(bytes), stat: live };
  } catch (error) {
    if (error instanceof LockError) throw error;
    throw error;
  }
}

function publishNoClobber(target, bytes) {
  ensureSafeDir(LOCKS_DIR);
  try {
    boundary({ action: 'publish', path: target, authorityRoot: PROJECT_ROOT,
      rawBase64: bytes.toString('base64'), maxBytes: MAX_BYTES });
  } catch (error) {
    if (error && error.code === 'EEXIST') { const exists = new Error('lock already exists'); exists.code = 'EEXIST'; throw exists; }
    if (error && ['DIRECTORY_CHANGED', 'ENTRY_CHANGED'].includes(error.code)) fail('LOCK_CHANGED', 'lock directory changed during no-clobber publication', 4);
    if (error && ['DIRECTORY_UNSAFE', 'PATH_UNSAFE'].includes(error.code)) fail('LOCK_DIRECTORY_UNSAFE', 'lock directory is unsafe', 3);
    throw error;
  }
}

function mkdirAnchored(target) {
  try {
    return statFromProof(boundary({ action: 'mkdir', path: target, authorityRoot: PROJECT_ROOT }).stat);
  } catch (error) {
    if (error && error.code === 'EEXIST') fail('LOCK_CHANGED', 'private release path unexpectedly exists', 4);
    if (error && ['DIRECTORY_CHANGED', 'ENTRY_CHANGED'].includes(error.code)) fail('LOCK_CHANGED', 'lock directory changed while creating private release state', 4);
    throw error;
  }
}

function renameExact(source, target, expected) {
  try {
    return statFromProof(boundary({ action: 'rename-exact', sourcePath: source, targetPath: target,
      authorityRoot: PROJECT_ROOT, expectedSource: exactStatRequest(expected) }).stat);
  } catch (error) {
    if (error && error.code === 'EEXIST') fail('LOCK_CHANGED', 'release destination unexpectedly exists', 4);
    if (error && ['DIRECTORY_CHANGED', 'ENTRY_CHANGED', 'PATH_MISSING'].includes(error.code)) {
      fail('LOCK_CHANGED', 'exact lock generation changed at the anchored rename boundary', 4);
    }
    throw error;
  }
}

function stableDirectoryNames(dir, changedMessage) {
  try {
    const raw = boundary({ action: 'list', path: dir, authorityRoot: PROJECT_ROOT, maxEntries: MAX_RUNTIME_ENTRIES + 1 },
      Math.max(1024 * 1024, MAX_RUNTIME_ENTRIES * 1400));
    if (raw.truncated || raw.names.length > MAX_RUNTIME_ENTRIES) fail('LOCK_DIRECTORY_TOO_LARGE', 'task-lock directory exceeds its bounded entry limit', 1);
    const entries = Object.create(null);
    for (const name of raw.names) entries[name] = statFromProof(raw.entries[name]);
    return { names: raw.names, stat: statFromProof(raw.stat), entries };
  } catch (error) {
    if (error instanceof LockError) throw error;
    if (error && ['DIRECTORY_CHANGED', 'ENTRY_CHANGED'].includes(error.code)) fail('LOCK_DIRECTORY_CHANGED', changedMessage, 4);
    if (error && ['DIRECTORY_UNSAFE', 'PATH_UNSAFE'].includes(error.code)) fail('LOCK_DIRECTORY_UNSAFE', 'task-lock recovery paths must be real directories', 3);
    throw error;
  }
}

function assertCanonicalAbsent(stem) {
  const scan = stableDirectoryNames(LOCKS_DIR, 'task-lock directory changed while proving released absence');
  if (scan.names.includes(`${stem}.json`)) {
    fail('LOCK_RELEASE_FOREIGN_CANONICAL', 'release receipt exists but a canonical lock is still present; it was preserved', 1);
  }
}

function retainedReleaseEntries(stem) {
  ensureSafeDir(LOCKS_DIR);
  const prefix = `.${stem}.json.release-`;
  const scan = stableDirectoryNames(LOCKS_DIR, 'task-lock directory changed during release-recovery scan');
  return scan.names.filter((name) => name.startsWith(prefix)).map((name) => ({
    name,
    path: path.join(LOCKS_DIR, name),
    validName: /^[a-f0-9]{36}$/.test(name.slice(prefix.length)),
  }));
}

function completedReleaseEntries(stem) {
  ensureSafeDir(LOCKS_DIR);
  const prefix = `.${stem}.json.released-`;
  const scan = stableDirectoryNames(LOCKS_DIR, 'task-lock directory changed during completed-release scan');
  return scan.names.filter((name) => name.startsWith(prefix)).map((name) => ({
    name,
    path: path.join(LOCKS_DIR, name),
    validName: /^[a-f0-9]{36}$/.test(name.slice(prefix.length)),
  }));
}

const RELEASE_MANIFEST_FIELDS = Object.freeze(['createdAt', 'expectedHash', 'runId', 'sessionId', 'sourceProof', 'stem', 'version']);
const RELEASE_PROOF_FIELDS = Object.freeze(['ctimeNs', 'dev', 'ino', 'mode', 'mtimeNs', 'size']);

function releaseProof(stat) {
  return { dev: stat.dev, ino: stat.ino, mode: stat.mode, size: stat.size,
    mtimeNs: stat.mtimeNs, ctimeNs: stat.ctimeNs };
}

function releaseManifest(stem, runId, sessionId, expectedHash, stat) {
  return { version: 1, stem, runId, sessionId, expectedHash, createdAt: now(), sourceProof: releaseProof(stat) };
}

function validateReleaseManifest(value, stem = null) {
  if (!value || typeof value !== 'object' || Array.isArray(value) || !sameFields(value, RELEASE_MANIFEST_FIELDS) ||
      value.version !== 1 || core.safeIntegerId(value.stem) === null || (stem && value.stem !== stem) ||
      !RUN_ID_RE.test(String(value.runId || '')) || !SESSION_ID_RE.test(String(value.sessionId || '')) ||
      !HASH_RE.test(String(value.expectedHash || '')) || !validIso(value.createdAt) ||
      !value.sourceProof || typeof value.sourceProof !== 'object' || Array.isArray(value.sourceProof) ||
      !sameFields(value.sourceProof, RELEASE_PROOF_FIELDS)) return false;
  const proof = value.sourceProof;
  return DECIMAL_STAT_RE.test(String(proof.dev || '')) && DECIMAL_STAT_RE.test(String(proof.ino || '')) &&
    DECIMAL_STAT_RE.test(String(proof.mtimeNs || '')) && DECIMAL_STAT_RE.test(String(proof.ctimeNs || '')) &&
    Number.isSafeInteger(proof.mode) && proof.mode >= 0 && Number.isSafeInteger(proof.size) && proof.size >= 0;
}

function manifestBytes(value) {
  if (!validateReleaseManifest(value, value && value.stem)) fail('LOCK_RELEASE_MANIFEST_INVALID', 'generated release manifest is invalid', 3);
  return Buffer.from(`${JSON.stringify(value, null, 2)}\n`, 'utf8');
}

function readReleaseManifest(file, stem, parentScan) {
  let read;
  try { read = safeReadBytes(file, 16 * 1024, parentScan && parentScan.stat, parentScan && parentScan.entries[path.basename(file)]); }
  catch (error) {
    if (error instanceof LockError && error.code === 'LOCK_INVALID') throw error;
    fail('LOCK_RELEASE_RECOVERY_INVALID', 'release manifest cannot be read safely', 1);
  }
  let value;
  try { value = JSON.parse(new TextDecoder('utf-8', { fatal: true }).decode(read.bytes)); }
  catch (_) { fail('LOCK_RELEASE_RECOVERY_INVALID', 'release manifest is not valid canonical JSON', 1); }
  if (!validateReleaseManifest(value, stem) || !read.bytes.equals(manifestBytes(value))) {
    fail('LOCK_RELEASE_RECOVERY_INVALID', 'release manifest does not match the canonical schema', 1);
  }
  return { value, bytes: read.bytes, stat: read.stat };
}

function requireNoRetainedRelease(stem) {
  const retained = retainedReleaseEntries(stem);
  if (retained.length) {
    const malformed = retained.some((entry) => !entry.validName);
    fail(malformed ? 'LOCK_RELEASE_RECOVERY_INVALID' : 'LOCK_RELEASE_RECOVERY_REQUIRED', malformed
      ? 'a malformed retained lock-release path must be inspected before acquiring a new owner'
      : 'a retained lock-release generation must be recovered before acquiring a new owner', 1, {
      paths: retained.slice(0, 50).map((entry) => path.relative(PROJECT_ROOT, entry.path).split(path.sep).join('/')),
      malformed,
    });
  }
  const completed = completedReleaseEntries(stem);
  if (completed.some((entry) => !entry.validName) || completed.length > MAX_COMPLETED_RELEASES_PER_STEM) {
    fail('LOCK_RELEASE_RECEIPT_LIMIT', 'completed release receipts are malformed or exceed their per-task bound', 1, {
      count: completed.length, limit: MAX_COMPLETED_RELEASES_PER_STEM,
      malformed: completed.some((entry) => !entry.validName),
    });
  }
  for (const entry of completed) {
    try { readRetainedRelease(entry, stem); }
    catch (error) {
      fail('LOCK_RELEASE_RECEIPT_INVALID', 'a completed release receipt has an unsafe or malformed retained proof', 1, {
        path: path.relative(PROJECT_ROOT, entry.path).split(path.sep).join('/'),
        receiptCode: error && error.code || 'LOCK_RELEASE_RECOVERY_INVALID',
      });
    }
  }
}

function validateStageState(stem, stage) {
  let initial;
  try {
    initial = observedValidation({ stem, action: 'lock-acquire' }, stage === 'task-prep' ? 'task-prep' : 'runner', () => core.validateTaskState({
      repoRoot: PROJECT_ROOT,
      tasksDir: TASKS_DIR,
      stem,
      checkIndex: true,
      includeRuntime: false,
    }));
  } catch (error) {
    if (error instanceof core.SnapshotRaceError || error && error.exitCode === 4) {
      fail('TASK_STATE_CHANGED', 'task state changed during lock admission', 4);
    }
    fail('TASK_STATE_UNAVAILABLE', String(error && error.message || error).slice(0, 500), error && error.exitCode === 2 ? 2 : 3);
  }
  if (!initial.ok) {
    fail('TASK_STATE_INVALID', 'canonical task-state validation failed before lock acquisition', 1, {
      observedState: initial.observedState,
      sourceRevision: initial.sourceRevision,
      findings: initial.findings.slice(0, 50),
    });
  }
  if (!STAGE_STATES[stage].includes(initial.observedState)) {
    fail('LOCK_STAGE_STATE_MISMATCH', `${stage} cannot own a task in ${initial.observedState}`, 1, {
      observedState: initial.observedState,
      allowedStates: STAGE_STATES[stage],
      sourceRevision: initial.sourceRevision,
    });
  }
  const action = stage === 'orchestrator' ? 'run' : initial.observedState === 'backlog' ? 'prep' : 'answers';
  let admitted;
  try {
    admitted = observedValidation({ stem, action }, stage === 'task-prep' ? 'task-prep' : 'runner', () => core.validateAction({
      repoRoot: PROJECT_ROOT,
      tasksDir: TASKS_DIR,
      stem,
      action,
      checkIndex: true,
      includeRuntime: false,
    }));
  } catch (error) {
    if (error instanceof core.SnapshotRaceError || error && error.exitCode === 4) {
      fail('TASK_STATE_CHANGED', 'task state changed during action admission', 4);
    }
    fail('TASK_STATE_UNAVAILABLE', String(error && error.message || error).slice(0, 500), error && error.exitCode === 2 ? 2 : 3);
  }
  if (admitted.sourceRevision !== initial.sourceRevision || admitted.observedState !== initial.observedState) {
    fail('TASK_STATE_CHANGED', 'task state changed between state and action admission snapshots', 4, {
      beforeState: initial.observedState,
      afterState: admitted.observedState,
      beforeRevision: initial.sourceRevision,
      afterRevision: admitted.sourceRevision,
    });
  }
  if (!admitted.ok) {
    fail('TASK_ACTION_NOT_ADMITTED', `canonical ${action} admission failed before lock acquisition`, 1, {
      action,
      observedState: admitted.observedState,
      sourceRevision: admitted.sourceRevision,
      findings: admitted.findings.slice(0, 50),
    });
  }
  return admitted;
}

function receipt(read, created) {
  return {
    version: VERSION,
    ok: true,
    created,
    stem: read.value.stem,
    stage: read.value.stage,
    runId: read.value.runId,
    sessionId: read.value.sessionId,
    startedAt: read.value.startedAt,
    owner: read.value.owner,
    lockHash: read.hash,
  };
}

function recoveryWriterScan() {
  let scan;
  try { scan = writerLeases.scan(WRITER_LEASES_DIR, WRITER_AUTHORITY_ROOT); }
  catch (_) { return { ok: false, reason: 'writer-authority-unavailable', scan: null }; }
  if (!scan || !Array.isArray(scan.active) || !Array.isArray(scan.stale) ||
      !Array.isArray(scan.issues) || scan.issues.length) {
    return { ok: false, reason: 'writer-authority-unavailable', scan };
  }
  return { ok: true, reason: null, scan };
}

function recoveryWriterConflict(row, stem, lockSessionId, ownLeaseId = null) {
  if (!row || row.leaseId === ownLeaseId) return false;
  if (row.stem === stem || row.sessionId === lockSessionId) return true;
  // A stem-less writer owns shared workspace publication state.  The Site
  // admission path normally rejects it before minting our lease; repeating the
  // check here keeps direct/helper invocations fail-closed too.
  return row.stem === null;
}

function recoveryWriterCapability(scan, stem) {
  const leaseId = process.env.ORCHESTRATOR_WRITER_LEASE_ID;
  const token = process.env.ORCHESTRATOR_WRITER_LEASE_TOKEN;
  const sessionId = process.env.ORCHESTRATOR_WRITER_SESSION_ID;
  const envStem = process.env.ORCHESTRATOR_WRITER_STEM;
  if (!writerLeases.LEASE_ID_RE.test(String(leaseId || '')) ||
      !WRITER_TOKEN_RE.test(String(token || '')) ||
      !SESSION_ID_RE.test(String(sessionId || '')) || envStem !== stem) return null;
  const matching = scan.active.filter((row) => row && row.leaseId === leaseId);
  if (matching.length !== 1) return null;
  const row = matching[0];
  if (row.kind !== 'lock-writer' || row.stem !== stem || row.sessionId !== sessionId ||
      row.key !== `task-lock-recovery:${stem}` || row.token !== token || row.unverified !== false ||
      !row.owner || row.owner.hostname !== os.hostname() ||
      !writerLeases.processIdentityMatches(row.owner.pid, row.owner.processStartId)) return null;
  return row;
}

// A stale/unpublished INDEX is a WORKSPACE finding (`stem: null`), not a claim
// about this task or its dead owner, and no release path publishes an INDEX.
// Gating dead-owner recovery on it made the browser's only recovery affordance
// unreachable after the exact crash it exists for: a killed run leaves the INDEX
// stale AND the lock held, and the site exposes no INDEX publisher, so the loop
// closed only in a shell. task-index.mjs already excludes INDEX_* from its own
// publication blockers.
// INDEX_UNSAFE also covers "INDEX.json is a symlink or special file" — a
// filesystem-tamper signal of the TASK_ROOT_UNSAFE family, which every other
// consumer keeps hard. Only the publication-state findings are excused here.
const INDEX_PUBLICATION_BLOCKERS = new Set([
  'INDEX_STALE', 'INDEX_SCHEMA_INVALID', 'INDEX_JSON_INVALID',
  'INDEX_TOO_LARGE', 'INDEX_IDENTITY_INVALID',
]);
function nonIndexBlockers(result) {
  return (result && Array.isArray(result.findings) ? result.findings : []).filter((item) =>
    item && (item.severity === 'error' || item.severity === 'blocker') &&
    !INDEX_PUBLICATION_BLOCKERS.has(String(item.code || '')));
}
function recoveryStateAcceptable(result) {
  return result.ok === true || nonIndexBlockers(result).length === 0;
}

function ownerRecoveryVerdict(read, requireCapability = false) {
  const value = read.value;
  const owner = value.owner;
  let ownerState = 'unknown';
  if (owner.kind !== 'site') {
    return { recoverable: false, reason: 'owner-kind-unsupported', ownerState };
  }
  if (owner.hostname !== os.hostname()) {
    return { recoverable: false, reason: 'owner-host-foreign', ownerState };
  }
  if (!writerLeases.PROCESS_START_ID_RE.test(String(owner.processStartId || ''))) {
    return { recoverable: false, reason: 'owner-identity-unavailable', ownerState };
  }
  try { ownerState = writerLeases.processIdentityState(owner.pid, owner.processStartId); }
  catch (_) { ownerState = 'unknown'; }
  if (ownerState !== 'dead' && ownerState !== 'reused') {
    return { recoverable: false, reason: ownerState === 'match' || ownerState === 'pid-live'
      ? 'owner-active' : 'owner-liveness-unavailable', ownerState };
  }
  const writer = recoveryWriterScan();
  if (!writer.ok) return { recoverable: false, reason: writer.reason, ownerState };
  let own = null;
  if (requireCapability) {
    own = recoveryWriterCapability(writer.scan, value.stem);
    if (!own) return { recoverable: false, reason: 'recovery-authority-missing', ownerState };
  }
  const conflict = writer.scan.active.find((row) =>
    recoveryWriterConflict(row, value.stem, value.sessionId, own && own.leaseId));
  if (conflict) return { recoverable: false, reason: 'writer-active', ownerState };
  const retained = retainedReleaseEntries(value.stem);
  if (retained.length) {
    return { recoverable: false, reason: 'release-recovery-required', ownerState };
  }
  return { recoverable: true, reason: 'owner-dead', ownerState };
}

function ownerStatus(args) {
  const stem = args['--stem'];
  ensureSafeDir(LOCKS_DIR, false);
  const read = safeRead(lockPath(stem), stem);
  const verdict = ownerRecoveryVerdict(read, false);
  return {
    version: VERSION,
    ok: true,
    operation: 'owner-status',
    stem,
    stage: read.value.stage,
    startedAt: read.value.startedAt,
    lockHash: read.hash,
    ownerState: verdict.ownerState,
    recoverable: verdict.recoverable,
    reason: verdict.reason,
  };
}

function validateOwnerRecoveryState(stem, stage) {
  let result;
  try {
    result = observedValidation({ stem, action: 'lock-owner-recovery' },
      stage === 'orchestrator' ? 'runner' : 'task-prep', () => core.validateTaskState({
        repoRoot: PROJECT_ROOT,
        tasksDir: TASKS_DIR,
        stem,
        checkIndex: true,
        includeRuntime: false,
      }));
  } catch (error) {
    if (error instanceof core.SnapshotRaceError || error && error.exitCode === 4) {
      fail('LOCK_OWNER_RECOVERY_STATE_CHANGED', 'task state changed during owner recovery', 4);
    }
    fail('LOCK_OWNER_RECOVERY_STATE_UNAVAILABLE',
      'fresh task state and INDEX could not be validated for owner recovery', 3);
  }
  if (!recoveryStateAcceptable(result) || !STAGE_STATES[stage].includes(result.observedState) ||
      !HASH_RE.test(String(result.sourceRevision || ''))) {
    fail('LOCK_OWNER_RECOVERY_STATE_INVALID',
      'task state or stage is not valid for dead-owner recovery; the lock was preserved', 4, {
        observedState: result.observedState,
        allowedStates: STAGE_STATES[stage],
        indexStatus: result.indexStatus || null,
        findings: nonIndexBlockers(result).slice(0, 50),
      });
  }
  return result;
}

function recoverOwnerLocked(args, mutexLease) {
  const stem = args['--stem'];
  const expectedHash = args['--expected-hash'];
  if (!HASH_RE.test(String(expectedHash || ''))) {
    fail('INVOCATION_INVALID', 'recover-owner requires a valid --expected-hash', 2);
  }
  assertStemMutexHeld(mutexLease);
  ensureSafeDir(LOCKS_DIR, false);
  const read = safeRead(lockPath(stem), stem);
  if (read.hash !== expectedHash) {
    fail('LOCK_IDENTITY_MISMATCH',
      'owner-recovery proof does not identify the current exact lock bytes', 1);
  }
  const verdict = ownerRecoveryVerdict(read, true);
  if (!verdict.recoverable) {
    fail('LOCK_OWNER_RECOVERY_REFUSED',
      'canonical owner death or exclusive recovery authority could not be proven; the lock was preserved', 1, {
        reason: verdict.reason,
        ownerState: verdict.ownerState,
      });
  }
  const state = validateOwnerRecoveryState(stem, read.value.stage);
  const released = releaseLocked({
    '--stem': stem,
    '--run-id': read.value.runId,
    '--session-id': read.value.sessionId,
    '--expected-hash': read.hash,
    '--expected-state': state.observedState,
    '--source-revision': state.sourceRevision,
  }, { ownerRecovery: true }, mutexLease);
  return {
    ...released,
    operation: 'recover-owner',
    ownerState: verdict.ownerState,
  };
}

function standbyLeaseFence(args) {
  const stem = args['--stem'];
  const stage = args['--stage'];
  const ownerKind = args['--owner-kind'] ||
    (args['--session-id'] && args['--session-id'] === process.env.ORCHESTRATOR_WRITER_SESSION_ID ? 'site' : 'direct');
  const suppliedLeaseId = args['--writer-lease-id'] || null;
  const suppliedToken = args['--writer-lease-token'] || null;
  let scan;
  try { scan = writerLeases.scan(WRITER_LEASES_DIR, WRITER_AUTHORITY_ROOT); }
  catch (_) { fail('WRITER_LEASE_AUTHORITY_UNAVAILABLE', 'standby writer authority could not be scanned', 3); }
  if (!scan || !Array.isArray(scan.active) || !Array.isArray(scan.stale) || !Array.isArray(scan.issues) || scan.issues.length) {
    fail('WRITER_LEASE_AUTHORITY_UNAVAILABLE', 'standby writer authority contains unreadable or retained recovery evidence', 3);
  }
  const allowedKeys = STANDBY_KEYS_BY_STAGE[stage] || new Set();
  const activeStandby = scan.active.filter((row) => row && row.kind === 'task-session' && row.stem === stem &&
    typeof row.key === 'string' && row.key.startsWith('standby:'));
  if (activeStandby.length === 0) {
    if (ownerKind === 'standby' || suppliedLeaseId !== null || suppliedToken !== null) {
      fail('STANDBY_WRITER_AUTHORITY_LOST', 'standby lock acquisition requires one exact active writer-lease capability', 4);
    }
    return null;
  }
  if (activeStandby.length !== 1 || ownerKind !== 'standby' ||
      !writerLeases.LEASE_ID_RE.test(String(suppliedLeaseId || '')) || !WRITER_TOKEN_RE.test(String(suppliedToken || ''))) {
    fail('STANDBY_WRITER_CONFLICT', 'an active standby writer blocks every task-lock acquisition except its exact capability', 4);
  }
  const row = activeStandby[0];
  if (!allowedKeys.has(row.key) || row.leaseId !== suppliedLeaseId || row.token !== suppliedToken ||
      row.sessionId !== args['--session-id'] || row.unverified !== false || row.expiresAt === null ||
      !Number.isFinite(Date.parse(row.expiresAt)) || Date.parse(row.expiresAt) <= Date.now() ||
      !row.owner || row.owner.hostname !== os.hostname() ||
      !writerLeases.processIdentityMatches(row.owner.pid, row.owner.processStartId) ||
      args['--owner-id'] !== `standby:${row.leaseId}`) {
    fail('STANDBY_WRITER_AUTHORITY_LOST', 'standby task-lock capability does not match the exact active writer generation', 4);
  }
  const conflict = scan.active.find((candidate) => candidate && candidate.leaseId !== row.leaseId &&
    (candidate.stem === stem || candidate.key === `task:${stem}` || candidate.key === 'task:create-backlog' ||
      candidate.key === 'task:recover-backlog-creations' || candidate.key === 'task:recover-backlog-edits' ||
      typeof candidate.key === 'string' && candidate.key.startsWith('task:edit-backlog:')));
  if (conflict) {
    fail('STANDBY_WRITER_CONFLICT', 'a conflicting writer or deterministic publication generation blocks task-lock acquisition', 4);
  }
  return Object.freeze({ leaseId: row.leaseId, sessionId: row.sessionId, key: row.key });
}

function siteSessionFence(args, stage, sessionId, runId, ownerKind, ownerPid, ownerProcessStartId) {
  if (ownerKind !== 'site') return null;
  const stem = args['--stem'];
  const leaseId = process.env.ORCHESTRATOR_WRITER_LEASE_ID;
  const delegationToken = process.env.ORCHESTRATOR_WRITER_DELEGATION_TOKEN;
  const envSessionId = process.env.ORCHESTRATOR_WRITER_SESSION_ID;
  const envStem = process.env.ORCHESTRATOR_WRITER_STEM;
  if (!writerLeases.LEASE_ID_RE.test(String(leaseId || '')) ||
      !/^[a-f0-9]{48}$/.test(String(delegationToken || '')) ||
      envSessionId !== sessionId || envStem !== stem) {
    fail('SITE_WRITER_AUTHORITY_INVALID',
      'site lock acquisition requires the exact inherited writer delegation capability', 4);
  }
  if (stage === 'orchestrator') {
    if (process.env.ORCHESTRATOR_RUN_ID !== runId) {
      fail('SITE_RUN_GENERATION_MISMATCH',
        'site orchestrator lock runId does not match the manager-issued generation', 4);
    }
    if (!/^wt-[a-f0-9]{32}$/.test(String(process.env.ORCHESTRATOR_WORKTREE_ID || ''))) {
      fail('SITE_RUN_GENERATION_MISMATCH',
        'site orchestrator lock has no canonical manager-issued worktree generation', 4);
    }
  }
  let scan;
  try { scan = writerLeases.scan(WRITER_LEASES_DIR, WRITER_AUTHORITY_ROOT); }
  catch (_) { fail('SITE_WRITER_AUTHORITY_INVALID', 'site writer authority could not be scanned', 4); }
  if (!scan || !Array.isArray(scan.active) || !Array.isArray(scan.issues) || scan.issues.length) {
    fail('SITE_WRITER_AUTHORITY_INVALID',
      'site writer authority contains unreadable or retained recovery evidence', 4);
  }
  const expectedDelegationHash = sha256(Buffer.from(delegationToken, 'ascii'));
  const callerStartId = writerLeases.captureProcessStartId(process.pid);
  if (!writerLeases.PROCESS_START_ID_RE.test(String(callerStartId || ''))) {
    fail('SITE_WRITER_AUTHORITY_INVALID', 'site lock caller process generation is unavailable', 4);
  }
  if (!validPid(ownerPid) || !writerLeases.PROCESS_START_ID_RE.test(String(ownerProcessStartId || ''))) {
    fail('SITE_WRITER_AUTHORITY_INVALID', 'site lock owner process generation is unavailable', 4);
  }
  const matches = scan.active.filter((row) => row && row.leaseId === leaseId &&
    row.kind === 'task-session' && row.stem === stem && row.sessionId === sessionId &&
    row.key === `task:${stem}` && row.delegationHash === expectedDelegationHash &&
    row.unverified === false && row.expiresAt === null && row.owner && row.owner.hostname === os.hostname() &&
    writerLeases.processTreeProof(process.pid, callerStartId,
      row.childPid, row.childProcessStartId).ok &&
    writerLeases.processTreeProof(ownerPid, ownerProcessStartId,
      row.childPid, row.childProcessStartId).ok);
  if (matches.length !== 1) {
    fail('SITE_WRITER_AUTHORITY_INVALID',
      'site lock acquisition requires one exact active delegated writer generation', 4);
  }
  const own = matches[0];
  const conflict = scan.active.find((row) => row && row.leaseId !== own.leaseId &&
    (writerLeases.deterministicPublisherLease(row) || row.stem === stem || row.key === `task:${stem}`));
  if (conflict) fail('SITE_WRITER_CONFLICT',
    'another writer conflicts with the delegated site task generation', 4);
  return Object.freeze({
    leaseId: own.leaseId, sessionId: own.sessionId, childPid: own.childPid,
    childProcessStartId: own.childProcessStartId, delegationHash: own.delegationHash
  });
}

function confirmAcquisition(stem, stage, before, published, created, mutexLease, args,
  initialWriterFence, initialSiteFence) {
  assertStemMutexHeld(mutexLease);
  let after;
  try {
    requireNoRetainedRelease(stem);
    after = validateStageState(stem, stage);
    if (after.observedState !== before.observedState || after.sourceRevision !== before.sourceRevision) {
      fail('TASK_STATE_CHANGED', 'task state or source revision changed after lock publication', 4, {
        beforeState: before.observedState,
        afterState: after.observedState,
        beforeRevision: before.sourceRevision,
        afterRevision: after.sourceRevision,
      });
    }
    fixturePause('TASK_LOCK_TEST_PAUSE_BEFORE_WRITER_RECHECK_MS');
    const finalWriterFence = standbyLeaseFence(args);
    if (JSON.stringify(finalWriterFence) !== JSON.stringify(initialWriterFence)) {
      fail('STANDBY_WRITER_AUTHORITY_CHANGED', 'standby writer authority changed while task-lock ownership was being published', 4);
    }
    const finalSiteFence = siteSessionFence(args, stage, published.value.sessionId,
      published.value.runId, published.value.owner.kind,
      published.value.owner.pid, published.value.owner.processStartId);
    if (JSON.stringify(finalSiteFence) !== JSON.stringify(initialSiteFence)) {
      fail('SITE_WRITER_AUTHORITY_CHANGED',
        'site writer authority changed while task-lock ownership was being published', 4);
    }
  } catch (error) {
    if (created) {
      try {
        assertStemMutexHeld(mutexLease);
        releaseLocked({
          '--stem': stem,
          '--run-id': published.value.runId,
          '--session-id': published.value.sessionId,
          '--expected-hash': published.hash,
        }, { internalRollback: true }, mutexLease);
      } catch (releaseError) {
        fail('LOCK_ROLLBACK_FAILED', 'post-publication validation failed and exact owned-lock release could not be proven; recovery is required', 4, {
          validationCode: error && error.code || 'TASK_STATE_CHANGED',
          releaseCode: releaseError && releaseError.code || 'LOCK_RELEASE_FAILED',
        });
      }
    }
    if (error instanceof LockError && error.exitCode === 4) throw error;
    fail('LOCK_POST_VALIDATION_FAILED', created
      ? 'fresh action validation failed after lock publication; the exact owned generation was released'
      : 'fresh action validation failed during an idempotent acquire; the existing generation was preserved', 4, {
      validationCode: error && error.code || 'TASK_STATE_INVALID',
    });
  }
  assertStemMutexHeld(mutexLease);
  return {
    ...receipt(published, created),
    action: after.action,
    observedState: after.observedState,
    sourceRevision: after.sourceRevision,
  };
}

function acquireLocked(args, mutexLease) {
  const stem = args['--stem'];
  const stage = args['--stage'];
  if (!STAGES.has(stage)) fail('INVOCATION_INVALID', '--stage must be task-prep or orchestrator', 2);
  assertStemMutexHeld(mutexLease);
  const target = lockPath(stem);
  requireNoRetainedRelease(stem);
  const inheritedSession = process.env.ORCHESTRATOR_WRITER_SESSION_ID;
  const suppliedSession = args['--session-id'] || (SESSION_ID_RE.test(String(inheritedSession || '')) ? inheritedSession : null);
  const sessionId = suppliedSession || randomId('ws');
  const runId = args['--run-id'] || randomId('run');
  if (!RUN_ID_RE.test(runId)) fail('INVOCATION_INVALID', '--run-id is invalid', 2);
  if (!SESSION_ID_RE.test(sessionId)) fail('INVOCATION_INVALID', '--session-id is invalid', 2);
  const ownerKind = args['--owner-kind'] || (suppliedSession === inheritedSession ? 'site' : 'direct');
  if (!OWNER_KINDS.has(ownerKind)) fail('INVOCATION_INVALID', '--owner-kind is invalid', 2);
  const ownerPid = args['--owner-pid'] === undefined ? process.ppid : Number(args['--owner-pid']);
  if (!validPid(ownerPid) || String(ownerPid) !== String(args['--owner-pid'] === undefined ? ownerPid : args['--owner-pid'])) {
    fail('INVOCATION_INVALID', '--owner-pid must be a positive decimal process id', 2);
  }
  const ownerId = args['--owner-id'] || `${ownerKind}:${sessionId}`;
  if (!boundedText(ownerId, 240)) fail('INVOCATION_INVALID', '--owner-id is invalid', 2);
  const ownerProcessStartId = writerLeases.captureProcessStartId(ownerPid);
  if ((process.platform === 'linux' || process.platform === 'darwin' || process.platform === 'win32') &&
      !writerLeases.PROCESS_START_ID_RE.test(String(ownerProcessStartId || ''))) {
    fail('LOCK_OWNER_GENERATION_UNAVAILABLE', 'owner process-start generation cannot be proven on this platform', 3);
  }
  const initialWriterFence = standbyLeaseFence(args);
  const initialSiteFence = siteSessionFence(args, stage, sessionId, runId, ownerKind,
    ownerPid, ownerProcessStartId);
  const state = validateStageState(stem, stage);
  const retryHash = args['--expected-hash'];
  if (retryHash) {
    let current;
    try { current = safeRead(target, stem); }
    catch (error) {
      if (error instanceof LockError && error.code === 'LOCK_NOT_FOUND') {
        fail('LOCK_RETRY_NOT_FOUND', 'exact idempotent acquire proof names no current lock generation; nothing was published', 1);
      }
      throw error;
    }
    const exactOwner = current.hash === retryHash && current.value.stage === stage &&
      current.value.runId === runId && current.value.sessionId === sessionId &&
      current.value.owner.kind === ownerKind && current.value.owner.id === ownerId &&
      current.value.owner.pid === ownerPid && current.value.owner.hostname === os.hostname() &&
      current.value.owner.processStartId === ownerProcessStartId;
    if (!exactOwner) {
      fail('LOCK_ALREADY_OWNED', 'current lock bytes/owner differ from the explicit idempotent acquire proof', 1, {
        stage: current.value.stage, runId: current.value.runId, sessionId: current.value.sessionId,
        startedAt: current.value.startedAt, lockHash: current.hash,
      });
    }
    return confirmAcquisition(stem, stage, state, current, false, mutexLease, args,
      initialWriterFence, initialSiteFence);
  }
  const startedAt = now();
  const record = {
    version: VERSION,
    stem,
    stage,
    runId,
    sessionId,
    startedAt,
    owner: {
      kind: ownerKind,
      id: ownerId,
      pid: ownerPid,
      processStartId: ownerProcessStartId || null,
      hostname: os.hostname(),
      startedAt,
    },
  };
  const shapeIssue = validateRecord(record, stem);
  if (shapeIssue) fail('LOCK_RECORD_INVALID', shapeIssue, 3);
  const bytes = Buffer.from(`${JSON.stringify(record, null, 2)}\n`, 'utf8');
  if (bytes.length > MAX_BYTES) fail('LOCK_RECORD_INVALID', 'generated lock exceeds its size limit', 3);
  assertStemMutexHeld(mutexLease);
  try {
    publishNoClobber(target, bytes);
  } catch (error) {
    if (!error || error.code !== 'EEXIST') throw error;
    const current = safeRead(target, stem);
    fail('LOCK_ALREADY_OWNED', 'task lock already exists; retry requires its exact --expected-hash and owner tuple', 1, {
      stage: current.value.stage,
      runId: current.value.runId,
      sessionId: current.value.sessionId,
      startedAt: current.value.startedAt,
      lockHash: current.hash,
    });
  }
  let published;
  try {
    assertStemMutexHeld(mutexLease);
    published = safeRead(target, stem);
    if (!published.bytes.equals(bytes)) fail('LOCK_PUBLICATION_MISMATCH', 'published lock bytes do not match the candidate', 4);
  } catch (error) {
    try {
      assertStemMutexHeld(mutexLease);
      releaseLocked({
        '--stem': stem,
        '--run-id': runId,
        '--session-id': sessionId,
        '--expected-hash': sha256(bytes),
      }, { internalRollback: true }, mutexLease);
    } catch (releaseError) {
      fail('LOCK_ROLLBACK_FAILED', 'lock publication could not be verified and exact owned-lock release could not be proven; recovery is required', 4, {
        publicationCode: error && error.code || 'LOCK_PUBLICATION_MISMATCH',
        releaseCode: releaseError && releaseError.code || 'LOCK_RELEASE_FAILED',
      });
    }
    fail('LOCK_PUBLICATION_MISMATCH', 'lock publication could not be verified; the exact owned generation was released', 4, {
      publicationCode: error && error.code || 'LOCK_CHANGED',
    });
  }
  return confirmAcquisition(stem, stage, state, published, true, mutexLease, args,
    initialWriterFence, initialSiteFence);
}

function verify(args) {
  const stem = args['--stem'];
  const target = lockPath(stem);
  ensureSafeDir(LOCKS_DIR, false);
  const read = safeRead(target, stem);
  if (args['--stage'] && read.value.stage !== args['--stage']) fail('LOCK_IDENTITY_MISMATCH', 'lock stage does not match', 1);
  if (args['--run-id'] && read.value.runId !== args['--run-id']) fail('LOCK_IDENTITY_MISMATCH', 'lock runId does not match', 1);
  if (args['--session-id'] && read.value.sessionId !== args['--session-id']) fail('LOCK_IDENTITY_MISMATCH', 'lock sessionId does not match', 1);
  if (args['--expected-hash'] && read.hash !== args['--expected-hash']) fail('LOCK_IDENTITY_MISMATCH', 'lock hash does not match', 1);
  return receipt(read, false);
}

function readRetainedRelease(entry, stem) {
  if (!entry.validName) {
    fail('LOCK_RELEASE_RECOVERY_INVALID', 'retained lock-release directory name is malformed', 1, {
      path: path.relative(PROJECT_ROOT, entry.path).split(path.sep).join('/'),
    });
  }
  let scan;
  try { scan = stableDirectoryNames(entry.path, 'retained lock-release directory changed during recovery'); }
  catch (error) {
    if (error instanceof LockError && ['LOCK_DIRECTORY_CHANGED', 'LOCK_CHANGED'].includes(error.code)) {
      fail('LOCK_RELEASE_RECOVERY_CHANGED', 'retained lock-release directory changed during recovery', 4);
    }
    if (error instanceof LockError) fail('LOCK_RELEASE_RECOVERY_INVALID', 'retained lock-release path must be a private real directory', 1);
    throw error;
  }
  if (!scan.stat.isDirectory() || scan.stat.isSymbolicLink() || (scan.stat.mode & 0o777) !== 0o700) {
    fail('LOCK_RELEASE_RECOVERY_INVALID', 'retained lock-release path must be a private real directory', 1);
  }
  const allowedNames = new Set(['candidate', 'canonical', 'manifest.json']);
  const validShapes = scan.names.length <= 3 && scan.names.every((name) => allowedNames.has(name)) &&
    (!scan.names.includes('canonical') || scan.names.includes('candidate'));
  if (!validShapes) {
    fail('LOCK_RELEASE_RECOVERY_INVALID', 'retained lock-release directory has an unsupported crash-state shape', 1, {
      path: path.relative(PROJECT_ROOT, entry.path).split(path.sep).join('/'), entries: scan.names.slice(0, 50),
    });
  }
  const manifest = scan.names.includes('manifest.json')
    ? readReleaseManifest(path.join(entry.path, 'manifest.json'), stem, scan) : null;
  const candidatePath = scan.names.includes('candidate') ? path.join(entry.path, 'candidate') : null;
  const candidate = candidatePath ? safeRead(candidatePath, stem, scan.stat, scan.entries.candidate) : null;
  const capturedCanonicalPath = scan.names.includes('canonical') ? path.join(entry.path, 'canonical') : null;
  const capturedCanonical = capturedCanonicalPath
    ? safeRead(capturedCanonicalPath, stem, scan.stat, scan.entries.canonical) : null;
  if (candidate && capturedCanonical && (candidate.stat.dev !== capturedCanonical.stat.dev ||
      candidate.stat.ino !== capturedCanonical.stat.ino || candidate.hash !== capturedCanonical.hash)) {
    fail('LOCK_RELEASE_RECOVERY_INVALID', 'captured canonical is not the exact retained candidate inode', 1);
  }
  const after = stableDirectoryNames(entry.path, 'retained lock-release directory changed while reading its proof');
  if (!sameStat(scan.stat, after.stat) || canonicalDirectoryNames(scan.names) !== canonicalDirectoryNames(after.names)) {
    fail('LOCK_RELEASE_RECOVERY_CHANGED', 'retained lock-release proofs changed while being read', 4);
  }
  return { ...entry, candidatePath, candidate, capturedCanonicalPath, capturedCanonical, manifest, dirStat: after.stat, names: after.names,
    kind: candidate && manifest ? 'manifest-candidate' : candidate ? 'candidate' : manifest ? 'prepared' : 'empty' };
}

function canonicalDirectoryNames(names) { return JSON.stringify(names.slice().sort()); }

function recoveryProofMatches(recovery, runId, sessionId, expectedHash) {
  if (recovery.manifest) {
    const value = recovery.manifest.value;
    if (value.runId !== runId || value.sessionId !== sessionId || value.expectedHash !== expectedHash) return false;
    if (recovery.candidate) {
      const proof = value.sourceProof;
      if (recovery.candidate.hash !== expectedHash || recovery.candidate.value.runId !== runId ||
          recovery.candidate.value.sessionId !== sessionId ||
          !sameMovedStat(recovery.candidate.stat, statFromProof({ ...proof, kind: 'file' }))) return false;
    }
    return true;
  }
  return !!recovery.candidate && recovery.candidate.hash === expectedHash &&
    recovery.candidate.value.runId === runId && recovery.candidate.value.sessionId === sessionId;
}

function completedPathFor(activePath) {
  const next = activePath.replace('.json.release-', '.json.released-');
  if (next === activePath) fail('LOCK_RELEASE_RECOVERY_INVALID', 'release path cannot be converted to a completed receipt', 3);
  return next;
}

function completeReleaseDirectory(recovery, mutexLease) {
  const fresh = stableDirectoryNames(recovery.path, 'release directory changed before completion');
  if (!sameStat(fresh.stat, recovery.dirStat) || canonicalDirectoryNames(fresh.names) !== canonicalDirectoryNames(recovery.names)) {
    fail('LOCK_RELEASE_RECOVERY_CHANGED', 'release directory changed before completion; it was preserved', 4);
  }
  const completedPath = completedPathFor(recovery.path);
  assertStemMutexHeld(mutexLease);
  renameExact(recovery.path, completedPath, fresh.stat);
  assertStemMutexHeld(mutexLease);
  return completedPath;
}

function matchingCompletedReleases(stem, runId, sessionId, expectedHash) {
  const entries = completedReleaseEntries(stem);
  if (entries.some((entry) => !entry.validName) || entries.length > MAX_COMPLETED_RELEASES_PER_STEM) {
    fail('LOCK_RELEASE_RECOVERY_INVALID', 'malformed completed lock-release receipts require inspection', 1);
  }
  const matches = [];
  for (const entry of entries) {
    const read = readRetainedRelease(entry, stem);
    if (read.kind === 'empty') {
      fail('LOCK_RELEASE_RECOVERY_INVALID', 'completed lock-release receipt is empty and proves no lock generation', 1);
    }
    if (recoveryProofMatches(read, runId, sessionId, expectedHash)) matches.push(read);
  }
  return { matches };
}

function reactivateCompletedRelease(recovery, stem, mutexLease) {
  if (core.safeIntegerId(stem) === null) fail('LOCK_RELEASE_RECOVERY_INVALID', 'completed release proof has no canonical stem', 1);
  const activePath = path.join(LOCKS_DIR, `.${stem}.json.release-${crypto.randomBytes(18).toString('hex')}`);
  assertStemMutexHeld(mutexLease);
  renameExact(recovery.path, activePath, recovery.dirStat);
  assertStemMutexHeld(mutexLease);
  return readRetainedRelease({ name: path.basename(activePath), path: activePath, validName: true }, stem);
}

function commitRecoveredRelease(recovery, stem, args, mutexLease, options = {}) {
  fixturePause('TASK_LOCK_TEST_PAUSE_AFTER_RECOVERY_DETACH_MS');
  assertStemMutexHeld(mutexLease);
  const finalState = validateReleasePostcondition(stem, args['--expected-state'], args['--source-revision'],
    args['--expected-state'] === 'todo' ? 'runner' : 'task-prep',
    { ownerRecovery: options.ownerRecovery === true });
  completeReleaseDirectory(recovery, mutexLease);
  return finalState;
}

function validateReleasePostcondition(stem, expectedState, sourceRevision, caller = 'task-prep', options = {}) {
  if (!['backlog', 'pending', 'todo'].includes(String(expectedState || '')) ||
      !HASH_RE.test(String(sourceRevision || ''))) {
    fail('LOCK_RELEASE_POSTCONDITION_REQUIRED',
      'release requires --expected-state backlog|pending|todo and --source-revision from a fresh final transition receipt', 2);
  }
  let result;
  try {
    result = observedValidation({ stem, expect: expectedState, action: 'lock-release' }, caller, () => core.validateTaskState({
      repoRoot: PROJECT_ROOT,
      tasksDir: TASKS_DIR,
      stem,
      expect: expectedState,
      checkIndex: true,
      includeRuntime: false,
    }));
  } catch (error) {
    if (error instanceof core.SnapshotRaceError || error && error.exitCode === 4) {
      fail('LOCK_RELEASE_POSTCONDITION_CHANGED', 'task state changed during final lock-release validation', 4);
    }
    fail('LOCK_RELEASE_POSTCONDITION_UNAVAILABLE',
      'fresh task state and INDEX could not be validated before lock release', 3);
  }
  // Dead-owner recovery reaches this through recoverOwnerLocked; the normal
  // release follows a transition that just published the INDEX, so only the
  // recovery context tolerates an unpublished one.
  const stateOk = options.ownerRecovery === true ? recoveryStateAcceptable(result) : result.ok === true;
  if (!stateOk || result.observedState !== expectedState || result.sourceRevision !== sourceRevision) {
    fail('LOCK_RELEASE_POSTCONDITION_FAILED',
      'task state, source revision, or INDEX no longer matches the final transition receipt; the lock was preserved', 4, {
        expectedState,
        observedState: result.observedState,
        expectedSourceRevision: sourceRevision,
        observedSourceRevision: result.sourceRevision,
        snapshotHash: result.snapshotHash,
        findings: result.findings.slice(0, 50),
      });
  }
  return result;
}

function captureCanonicalIntoReceipt(canonical, recovery, name, mutexLease) {
  const targetPath = path.join(recovery.path, name);
  assertStemMutexHeld(mutexLease);
  renameExact(lockPath(canonical.value.stem), targetPath, canonical.stat);
  assertStemMutexHeld(mutexLease);
  const refreshed = readRetainedRelease(recovery, canonical.value.stem);
  const captured = name === 'candidate' ? refreshed.candidate : refreshed.capturedCanonical;
  if (!captured || canonical.hash !== captured.hash || !canonical.bytes.equals(captured.bytes) ||
      !sameMovedStat(canonical.stat, captured.stat)) {
    fail('LOCK_RELEASE_RECOVERY_CHANGED', 'captured canonical does not match its exact pre-rename proof', 4);
  }
  return refreshed;
}

function recoverReleaseLocked(args, mutexLease, options = {}) {
  const stem = args['--stem'];
  const runId = args['--run-id'];
  const sessionId = args['--session-id'];
  const expectedHash = args['--expected-hash'];
  if (!RUN_ID_RE.test(String(runId || '')) || !SESSION_ID_RE.test(String(sessionId || '')) || !HASH_RE.test(String(expectedHash || ''))) {
    fail('INVOCATION_INVALID', 'recover-release requires valid --run-id, --session-id and --expected-hash', 2);
  }
  assertStemMutexHeld(mutexLease);
  const target = lockPath(stem);
  ensureSafeDir(LOCKS_DIR, false);
  let finalState = validateReleasePostcondition(stem, args['--expected-state'], args['--source-revision'],
    args['--expected-state'] === 'todo' ? 'runner' : 'task-prep',
    // Inherited from the caller: a dead-owner recovery that converges here must
    // not re-acquire the INDEX precondition it was exempted from one frame up.
    { ownerRecovery: options.ownerRecovery === true });
  const retained = retainedReleaseEntries(stem);
  if (retained.some((entry) => !entry.validName)) {
    fail('LOCK_RELEASE_RECOVERY_INVALID', 'malformed retained lock-release paths make ownership ambiguous', 1, {
      paths: retained.slice(0, 50).map((entry) => path.relative(PROJECT_ROOT, entry.path).split(path.sep).join('/')),
    });
  }
  if (retained.length > 1) {
    fail('LOCK_RELEASE_RECOVERY_AMBIGUOUS', 'multiple retained lock-release generations exist; none was changed', 1, {
      paths: retained.slice(0, 50).map((entry) => path.relative(PROJECT_ROOT, entry.path).split(path.sep).join('/')),
    });
  }
  let canonical = null;
  try { canonical = safeRead(target, stem); }
  catch (error) {
    if (!(error instanceof LockError) || error.code !== 'LOCK_NOT_FOUND') {
      fail('LOCK_RELEASE_FOREIGN_CANONICAL', 'the canonical lock is unsafe or invalid; release receipts were preserved', 1, {
        canonicalCode: error && error.code || 'LOCK_READ_FAILED',
      });
    }
  }

  if (retained.length === 0) {
    const completed = matchingCompletedReleases(stem, runId, sessionId, expectedHash);
    if (canonical) {
      const exactCanonical = canonical.value.runId === runId && canonical.value.sessionId === sessionId && canonical.hash === expectedHash;
      if (!exactCanonical) fail('LOCK_RELEASE_FOREIGN_CANONICAL', 'canonical lock has a different exact identity; it was preserved', 1);
      const duplicate = completed.matches.find((row) => row.candidate &&
        row.candidate.stat.dev === canonical.stat.dev && row.candidate.stat.ino === canonical.stat.ino);
      if (duplicate) {
        const activeProof = reactivateCompletedRelease(duplicate, stem, mutexLease);
        const captured = captureCanonicalIntoReceipt(canonical, activeProof, 'canonical', mutexLease);
        finalState = commitRecoveredRelease(captured, stem, args, mutexLease, options);
        assertCanonicalAbsent(stem);
        return { version: VERSION, ok: true, recovered: true, alreadyRecovered: false,
          mode: 'reappeared-captured', stem, runId, sessionId, lockHash: expectedHash,
          state: finalState.observedState, sourceRevision: finalState.sourceRevision, snapshotHash: finalState.snapshotHash };
      }
      const prepared = completed.matches.find((row) => row.kind === 'prepared' &&
        sameMovedStat(canonical.stat, statFromProof({ ...row.manifest.value.sourceProof, kind: 'file' })));
      if (prepared) {
        const activeProof = reactivateCompletedRelease(prepared, stem, mutexLease);
        const refreshed = captureCanonicalIntoReceipt(canonical, activeProof, 'candidate', mutexLease);
        if (!recoveryProofMatches(refreshed, runId, sessionId, expectedHash)) {
          fail('LOCK_RELEASE_RECOVERY_CHANGED', 'captured prepared release does not match its manifest', 4);
        }
        finalState = commitRecoveredRelease(refreshed, stem, args, mutexLease, options);
        assertCanonicalAbsent(stem);
        return { version: VERSION, ok: true, recovered: true, alreadyRecovered: false,
          mode: 'pre-detach-captured', stem, runId, sessionId, lockHash: expectedHash,
          state: finalState.observedState, sourceRevision: finalState.sourceRevision, snapshotHash: finalState.snapshotHash };
      }
      fail('LOCK_RELEASE_FOREIGN_CANONICAL', 'a canonical generation reappeared without an exact retained inode proof; it was preserved', 1);
    }
    if (completed.matches.length) {
      assertCanonicalAbsent(stem);
      return { version: VERSION, ok: true, recovered: false, alreadyRecovered: true,
        mode: 'detached', stem, runId, sessionId, lockHash: expectedHash,
        state: finalState.observedState, sourceRevision: finalState.sourceRevision, snapshotHash: finalState.snapshotHash };
    }
    fail('LOCK_RELEASE_RECOVERY_NOT_FOUND', 'no active or completed release proof identifies these exact credentials', 1);
  }

  const recovery = readRetainedRelease(retained[0], stem);
  if (recovery.kind !== 'empty' && !recoveryProofMatches(recovery, runId, sessionId, expectedHash)) {
    fail('LOCK_IDENTITY_MISMATCH', 'recovery credentials do not identify the exact retained lock proof', 1);
  }

  if (canonical) {
    if (recovery.candidate) {
      const duplicate = canonical.stat.dev === recovery.candidate.stat.dev && canonical.stat.ino === recovery.candidate.stat.ino &&
        canonical.hash === expectedHash && canonical.value.runId === runId && canonical.value.sessionId === sessionId;
      if (!duplicate) {
        fail('LOCK_RELEASE_FOREIGN_CANONICAL', 'a foreign canonical lock exists; canonical and retained generations were preserved', 1, {
          canonicalRunId: canonical.value.runId, canonicalSessionId: canonical.value.sessionId,
          canonicalHash: canonical.hash, recoveryPath: path.relative(PROJECT_ROOT, recovery.path).split(path.sep).join('/'),
        });
      }
      const captured = captureCanonicalIntoReceipt(canonical, recovery, 'canonical', mutexLease);
      finalState = commitRecoveredRelease(captured, stem, args, mutexLease, options);
      assertCanonicalAbsent(stem);
      return { version: VERSION, ok: true, recovered: true, alreadyRecovered: false,
        mode: 'duplicate', stem, runId, sessionId, lockHash: expectedHash,
        state: finalState.observedState, sourceRevision: finalState.sourceRevision, snapshotHash: finalState.snapshotHash };
    }
    const exactCanonical = canonical.value.runId === runId && canonical.value.sessionId === sessionId && canonical.hash === expectedHash;
    if (!exactCanonical || (recovery.manifest && !sameMovedStat(canonical.stat, statFromProof({ ...recovery.manifest.value.sourceProof, kind: 'file' })))) {
      fail('LOCK_RELEASE_FOREIGN_CANONICAL', 'pre-detach recovery does not match the exact canonical generation; all state was preserved', 1);
    }
    const captured = captureCanonicalIntoReceipt(canonical, recovery, 'candidate', mutexLease);
    if (captured.manifest && !recoveryProofMatches(captured, runId, sessionId, expectedHash)) {
      fail('LOCK_RELEASE_RECOVERY_CHANGED', 'captured pre-detach generation differs from its manifest', 4);
    }
    finalState = commitRecoveredRelease(captured, stem, args, mutexLease, options);
    assertCanonicalAbsent(stem);
    return { version: VERSION, ok: true, released: true, recovered: true, alreadyRecovered: false,
      mode: 'pre-detach', stem, runId, sessionId, lockHash: expectedHash,
      state: finalState.observedState, sourceRevision: finalState.sourceRevision, snapshotHash: finalState.snapshotHash };
  }

  if (recovery.kind === 'prepared' || recovery.kind === 'empty') {
    fail('LOCK_RELEASE_RECOVERY_AMBIGUOUS', 'pre-detach release lost its canonical generation; receipt was preserved', 1);
  }
  finalState = commitRecoveredRelease(recovery, stem, args, mutexLease, options);
  assertCanonicalAbsent(stem);
  return { version: VERSION, ok: true, recovered: true, alreadyRecovered: false,
    mode: 'detached',
    stem, runId, sessionId, lockHash: expectedHash,
    state: finalState.observedState, sourceRevision: finalState.sourceRevision, snapshotHash: finalState.snapshotHash };
}

function releaseLocked(args, options = {}, mutexLease) {
  const stem = args['--stem'];
  const runId = args['--run-id'];
  const sessionId = args['--session-id'];
  const expectedHash = args['--expected-hash'];
  if (!RUN_ID_RE.test(String(runId || '')) || !SESSION_ID_RE.test(String(sessionId || '')) || !HASH_RE.test(String(expectedHash || ''))) {
    fail('INVOCATION_INVALID', 'release requires valid --run-id, --session-id and --expected-hash', 2);
  }
  assertStemMutexHeld(mutexLease);
  const target = lockPath(stem);
  ensureSafeDir(LOCKS_DIR, false);
  let before;
  try {
    before = safeRead(target, stem);
  } catch (error) {
    if (!options.internalRollback && error instanceof LockError && error.code === 'LOCK_NOT_FOUND') {
      // A repeated exact release converges through the same recovery proof.
      // The per-stem kernel mutex makes this deterministic even when both
      // callers started before the first detach became visible.
      return recoverReleaseLocked(args, mutexLease, { ownerRecovery: options.ownerRecovery === true });
    }
    throw error;
  }
  if (before.value.runId !== runId || before.value.sessionId !== sessionId || before.hash !== expectedHash) {
    fail('LOCK_IDENTITY_MISMATCH', 'release credentials do not identify the current exact lock bytes', 1);
  }
  let finalState = null;
  if (!options.internalRollback) {
    finalState = validateReleasePostcondition(stem, args['--expected-state'], args['--source-revision'],
      before.value.stage === 'orchestrator' ? 'runner' : 'task-prep',
      { ownerRecovery: options.ownerRecovery === true });
  }
  ensureSafeDir(LOCKS_DIR);
  const active = retainedReleaseEntries(stem);
  if (active.length) fail('LOCK_RELEASE_RECOVERY_REQUIRED', 'an earlier release generation must be recovered before another release', 1);
  const releaseId = crypto.randomBytes(18).toString('hex');
  const activeDir = path.join(LOCKS_DIR, `.${path.basename(target)}.release-${releaseId}`);
  const candidatePath = path.join(activeDir, 'candidate');
  const manifestPath = path.join(activeDir, 'manifest.json');
  assertStemMutexHeld(mutexLease);
  mkdirAnchored(activeDir);
  if (process.env.TASK_LOCK_TEST_CRASH_AFTER_RELEASE_MKDIR === '1' && fixtureBoundaryAllowed()) process.exit(84);
  const manifest = releaseManifest(stem, runId, sessionId, expectedHash, before.stat);
  try {
    boundary({ action: 'publish', path: manifestPath, authorityRoot: PROJECT_ROOT,
      rawBase64: manifestBytes(manifest).toString('base64'), maxBytes: 16 * 1024 });
  } catch (error) {
    fail('LOCK_RELEASE_PREPARE_FAILED', 'private release manifest could not be published; recovery is required', 4, {
      prepareCode: error && error.code || 'LOCK_RELEASE_PREPARE_FAILED',
      recoveryPath: path.relative(PROJECT_ROOT, activeDir).split(path.sep).join('/'),
    });
  }
  if (process.env.TASK_LOCK_TEST_CRASH_AFTER_RELEASE_PREPARE === '1' && fixtureBoundaryAllowed()) process.exit(85);
  fixturePause('TASK_LOCK_TEST_PAUSE_AFTER_RELEASE_PREPARE_MS');
  assertStemMutexHeld(mutexLease);
  if (!options.internalRollback) {
    // The second fresh verdict is deliberately adjacent to the exact detach.
    // It closes the transition-receipt -> unlock gap even if task bytes or
    // INDEX changed while the release receipt itself was being prepared.
    finalState = validateReleasePostcondition(stem, args['--expected-state'], args['--source-revision'],
      before.value.stage === 'orchestrator' ? 'runner' : 'task-prep',
      { ownerRecovery: options.ownerRecovery === true });
    const currentLock = safeRead(target, stem);
    if (currentLock.hash !== before.hash || !currentLock.bytes.equals(before.bytes) || !sameStat(currentLock.stat, before.stat)) {
      fail('LOCK_CHANGED', 'task lock changed after final postcondition validation; prepared recovery state was preserved', 4);
    }
  }
  assertStemMutexHeld(mutexLease);
  try { renameExact(target, candidatePath, before.stat); }
  catch (error) {
    fail('LOCK_RELEASE_DETACH_FAILED', 'exact lock detach failed; prepared recovery state was preserved', 4, {
      detachCode: error && error.code || 'LOCK_CHANGED',
      recoveryPath: path.relative(PROJECT_ROOT, activeDir).split(path.sep).join('/'),
    });
  }
  if (process.env.TASK_LOCK_TEST_CRASH_AFTER_RELEASE_DETACH === '1' && fixtureBoundaryAllowed()) process.exit(87);
  fixturePause('TASK_LOCK_TEST_PAUSE_AFTER_RELEASE_DETACH_MS');
  assertStemMutexHeld(mutexLease);
  const activeScan = stableDirectoryNames(activeDir, 'release directory changed after detach');
  const confirmed = safeRead(candidatePath, stem, activeScan.stat, activeScan.entries.candidate);
  if (confirmed.hash !== expectedHash || confirmed.value.runId !== runId || confirmed.value.sessionId !== sessionId ||
      !sameMovedStat(confirmed.stat, before.stat)) {
    fail('LOCK_CHANGED', 'a different task-lock generation reached the anchored release boundary', 4);
  }
  const recovery = readRetainedRelease({ name: path.basename(activeDir), path: activeDir, validName: true }, stem);
  if (!recoveryProofMatches(recovery, runId, sessionId, expectedHash)) {
    fail('LOCK_RELEASE_RECOVERY_CHANGED', 'release proof changed before commit; active recovery state was preserved', 4);
  }
  if (!options.internalRollback) {
    // The active release generation still owns the lock while detached.  A
    // fresh post-detach verdict therefore closes the last transactional gap:
    // failure preserves the candidate and blocks every new acquisition.
    finalState = validateReleasePostcondition(stem, args['--expected-state'], args['--source-revision'],
      before.value.stage === 'orchestrator' ? 'runner' : 'task-prep',
      { ownerRecovery: options.ownerRecovery === true });
  }
  const completedPath = completeReleaseDirectory(recovery, mutexLease);
  if (process.env.TASK_LOCK_TEST_CRASH_AFTER_RELEASE_COMMIT === '1' && fixtureBoundaryAllowed()) process.exit(88);
  assertCanonicalAbsent(stem);
  return { version: VERSION, ok: true, released: true, stem, runId, sessionId, lockHash: expectedHash,
    ...(finalState ? { state: finalState.observedState, sourceRevision: finalState.sourceRevision,
      snapshotHash: finalState.snapshotHash,
      // The post-detach verdict is the authoritative one: an unrelated task moved
      // during the release can flip the INDEX without tripping this task's
      // postcondition, and reporting the pre-release sample would hide that.
      indexStatus: finalState.indexStatus === 'fresh' ? 'fresh'
        : finalState.indexStatus === 'invalid' ? 'invalid' : 'stale' } : {}),
    releaseReceipt: path.relative(PROJECT_ROOT, completedPath).split(path.sep).join('/') };
}

function parseArgs(argv) {
  const command = argv.shift();
  if (!['acquire', 'verify', 'release', 'recover-release', 'inspect', 'owner-status', 'recover-owner'].includes(command)) {
    fail('INVOCATION_INVALID', 'expected acquire|verify|release|recover-release|inspect|owner-status|recover-owner', 2);
  }
  const allowed = new Set(['--stem', '--stage', '--run-id', '--session-id', '--owner-kind', '--owner-id', '--owner-pid',
    '--writer-lease-id', '--writer-lease-token', '--expected-hash', '--expected-state', '--source-revision']);
  const out = { command };
  for (let i = 0; i < argv.length; i++) {
    const flag = argv[i];
    if (!allowed.has(flag) || Object.prototype.hasOwnProperty.call(out, flag)) fail('INVOCATION_INVALID', `unknown or duplicate option: ${flag}`, 2);
    if (i + 1 >= argv.length || argv[i + 1].startsWith('--')) fail('INVOCATION_INVALID', `${flag} requires a value`, 2);
    out[flag] = argv[++i];
  }
  if (!out['--stem']) fail('INVOCATION_INVALID', '--stem is required', 2);
  if (command === 'acquire' && !out['--stage']) fail('INVOCATION_INVALID', 'acquire requires --stage', 2);
  if (command === 'verify') {
    for (const required of ['--stage', '--run-id', '--session-id', '--expected-hash']) {
      if (!out[required]) fail('INVOCATION_INVALID', `verify requires ${required}`, 2);
    }
  }
  const commandFlags = {
    acquire: new Set(['--stem', '--stage', '--run-id', '--session-id', '--owner-kind', '--owner-id', '--owner-pid',
      '--writer-lease-id', '--writer-lease-token', '--expected-hash']),
    verify: new Set(['--stem', '--stage', '--run-id', '--session-id', '--expected-hash']),
    inspect: new Set(['--stem']),
    'owner-status': new Set(['--stem']),
    'recover-owner': new Set(['--stem', '--expected-hash']),
    release: new Set(['--stem', '--run-id', '--session-id', '--expected-hash', '--expected-state', '--source-revision']),
    'recover-release': new Set(['--stem', '--run-id', '--session-id', '--expected-hash', '--expected-state', '--source-revision']),
  }[command];
  for (const flag of Object.keys(out)) {
    if (flag !== 'command' && !commandFlags.has(flag)) fail('INVOCATION_INVALID', `${flag} is not valid for ${command}`, 2);
  }
  if (out['--stage'] && !STAGES.has(out['--stage'])) fail('INVOCATION_INVALID', '--stage must be task-prep or orchestrator', 2);
  if (out['--run-id'] && !RUN_ID_RE.test(out['--run-id'])) fail('INVOCATION_INVALID', '--run-id is invalid', 2);
  if (out['--session-id'] && !SESSION_ID_RE.test(out['--session-id'])) fail('INVOCATION_INVALID', '--session-id is invalid', 2);
  if (out['--expected-hash'] && !HASH_RE.test(out['--expected-hash'])) fail('INVOCATION_INVALID', '--expected-hash is invalid', 2);
  if (out['--writer-lease-id'] && !writerLeases.LEASE_ID_RE.test(out['--writer-lease-id'])) {
    fail('INVOCATION_INVALID', '--writer-lease-id is invalid', 2);
  }
  if (out['--writer-lease-token'] && !WRITER_TOKEN_RE.test(out['--writer-lease-token'])) {
    fail('INVOCATION_INVALID', '--writer-lease-token is invalid', 2);
  }
  if ((out['--writer-lease-id'] === undefined) !== (out['--writer-lease-token'] === undefined)) {
    fail('INVOCATION_INVALID', '--writer-lease-id and --writer-lease-token must be supplied together', 2);
  }
  if (out['--expected-state'] && !['backlog', 'pending', 'todo'].includes(out['--expected-state'])) fail('INVOCATION_INVALID', '--expected-state is invalid', 2);
  if (out['--source-revision'] && !HASH_RE.test(out['--source-revision'])) fail('INVOCATION_INVALID', '--source-revision is invalid', 2);
  return out;
}

function publicError(error) {
  const details = error && error.details && typeof error.details === 'object' ? error.details : null;
  return {
    version: VERSION,
    ok: false,
    code: error && error.code || 'LOCK_OPERATION_FAILED',
    message: String(error && error.message || error).slice(0, 500),
    retryable: Number(error && error.exitCode) === 4,
    ...(details ? { details } : {}),
  };
}

async function main() {
  try {
    platformSupport.assertCanonicalTaskPlatform();
    ensureInside(PROJECT_ROOT, TASKS_DIR, 'task directory');
    ensureInside(PROJECT_ROOT, LOCKS_DIR, 'lock directory');
    const args = parseArgs(process.argv.slice(2));
    let result;
    if (args.command === 'acquire') {
      result = await withStemMutex(args['--stem'], (lease) => acquireLocked(args, lease));
    } else if (args.command === 'release') {
      result = await withStemMutex(args['--stem'], (lease) => releaseLocked(args, {}, lease));
    } else if (args.command === 'recover-release') {
      result = await withStemMutex(args['--stem'], (lease) => recoverReleaseLocked(args, lease));
    } else if (args.command === 'recover-owner') {
      result = await withStemMutex(args['--stem'], (lease) => recoverOwnerLocked(args, lease));
    } else if (args.command === 'owner-status') {
      result = ownerStatus(args);
    } else {
      // Read-only inspection never participates in ownership mutation and
      // therefore does not wait behind the per-stem writer mutex.
      result = verify(args);
    }
    process.stdout.write(`${JSON.stringify(result, null, 2)}\n`);
  } catch (error) {
    const exitCode = Number.isInteger(error && error.exitCode) ? error.exitCode :
      error instanceof core.SnapshotRaceError ? 4 : 3;
    process.stderr.write(`${JSON.stringify(publicError(error), null, 2)}\n`);
    process.exitCode = exitCode;
  }
}

await main();
