#!/usr/bin/env node

// Deterministic owner for every non-finalizer column mutation.  Prompts and the
// site may prepare Markdown, but they never move/delete durable task artifacts
// themselves.  This helper fences the source revision, publishes a recovery
// marker, mutates with no-clobber/private-detach semantics, validates the
// filesystem postcondition, regenerates INDEX, and validates the final state.

import path from 'node:path';
import crypto from 'node:crypto';
import os from 'node:os';
import fs from 'node:fs';
import { spawn, spawnSync } from 'node:child_process';
import { createRequire } from 'node:module';
import { dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const require = createRequire(import.meta.url);
const core = require('./task-state-core.cjs');
const writerLeases = require('./writer-leases.cjs');
const fileGuards = require('../site/server/file-guards.js');
const platformSupport = require('./platform-support.cjs');

const HERE = dirname(fileURLToPath(import.meta.url));
const TASKS_DIR = path.resolve(process.env.ORCHESTRATOR_TASKS_DIR || HERE);
const PROJECT_ROOT = path.resolve(process.env.ORCHESTRATOR_PROJECT_ROOT || path.join(TASKS_DIR, '..', '..'));
const STATE_DIR = path.resolve(process.env.ORCHESTRATOR_TRANSITIONS_DIR || path.join(PROJECT_ROOT, 'orchestrator', '.cache', 'tasks', 'transitions'));
const LEASES_DIR = path.resolve(process.env.ORCHESTRATOR_WRITER_LEASES_DIR || path.join(PROJECT_ROOT, 'orchestrator', '.cache', 'tasks', 'finalizations', '.writers'));
const FINALIZATIONS_DIR = path.resolve(process.env.ORCHESTRATOR_FINALIZATIONS_DIR || path.join(PROJECT_ROOT, 'orchestrator', '.cache', 'tasks', 'finalizations'));
const DEPENDENCY_GRAPH_MUTEX = path.join(FINALIZATIONS_DIR, '.mutex.json');
const LOCKS_DIR = path.resolve(process.env.ORCHESTRATOR_LOCKS_DIR || path.join(PROJECT_ROOT, 'orchestrator', '.cache', 'tasks', 'locks'));
const JOURNAL_DIR = path.resolve(process.env.ORCHESTRATOR_JOURNAL_DIR || path.join(PROJECT_ROOT, 'orchestrator', '.cache', 'tasks', 'journal'));
const REOPEN_EVIDENCE_DIR = path.join(TASKS_DIR, 'evidence', 'reopen');
const TRANSITION_GUARDS_DIR = path.join(STATE_DIR, '.guards');
const INPUT_MAX = 8 * 1024 * 1024;
const STDIN_CHUNK_BYTES = 64 * 1024;
const MARKER_MAX = 20 * 1024 * 1024;
const REPLACE_WAL_MAX = 40 * 1024 * 1024;
const GUARD_MAX = 4096;
const TASK_LOCK_MAX = 32 * 1024;
const MAX_RUNTIME_ENTRIES = 10000;
const VERSION = 1;
const FS_BOUNDARY = path.join(HERE, 'finalize-lock.py');
const CANONICAL_PROJECT_ROOT = path.resolve(HERE, '..', '..');
let taskLockFixtureMutated = false;
let sourceDetachFixtureMutated = false;
let activeCommand = null;
let activeDependencyGraphMutex = null;

class TransitionError extends Error {
  constructor(code, message, exitCode = 1, details = {}) {
    super(message); this.name = 'TransitionError'; this.code = code; this.exitCode = exitCode; Object.assign(this, details);
  }
}

function now() { return new Date().toISOString(); }
function hash(bytes) { return core.sha256(bytes); }
function randomId() { return 'tr-' + crypto.randomBytes(18).toString('hex'); }
function fail(code, message, exitCode = 1, details) { throw new TransitionError(code, message, exitCode, details); }
function observationCaller(options = {}) {
  const action = String(options.action || '');
  const transition = String(options.transition || '');
  if (action === 'drop' || activeCommand === 'drop' || transition.endsWith(':absent')) return 'drop';
  if (action === 'reopen' || activeCommand === 'reopen' || transition === 'done:todo') return 'reopen';
  // In-body question rails belong to a running task, not to preparation.
  if (activeCommand === 'publish-questions' || activeCommand === 'persist-task-answers') return 'orchestrator';
  return 'task-prep';
}
function observedValidation(_kind, options, fn) {
  const started = process.hrtime.bigint();
  let result, thrown;
  try { result = fn(); return result; }
  catch (error) { thrown = error; throw error; }
  finally {
    const durationMs = Number((process.hrtime.bigint() - started) / 1000000n);
    const rawCode = String(thrown && thrown.code || 'TASK_STATE_UNAVAILABLE');
    const code = /^[A-Za-z0-9_.:-]{1,80}$/.test(rawCode) ? rawCode : 'TASK_STATE_UNAVAILABLE';
    const base = result || {
      version: 1, ok: false, overallOk: false,
      findings: [{ code, severity: 'blocker' }],
      stats: { scanMode: null, taskBodyReads: 0 },
    };
    const measured = {
      ...base,
      scope: base.scope || options && options.stem || null,
      action: base.action || options && options.action || activeCommand || null,
      transition: base.transition || options && options.transition || null,
      phase: base.phase || options && options.phase || null,
      expectedState: base.expectedState || options && options.expect || null,
      stats: { ...(base.stats || {}), durationMs },
    };
    process.stderr.write('[task-state] ' + JSON.stringify(core.observationFor(measured, {
      caller: observationCaller(options), slowThresholdMs: 100,
    })) + '\n');
  }
}
function ensureInside(root, target, label) {
  const rel = path.relative(root, target);
  if (rel === '..' || rel.startsWith('..' + path.sep) || path.isAbsolute(rel)) fail('PATH_OUTSIDE_ROOT', label + ' escapes its root', 2);
}
function boundary(action, values = {}) {
  if (activeDependencyGraphMutex) assertDependencyGraphMutexProcess(activeDependencyGraphMutex);
  const request = {
    version: 1,
    action,
    authorityRoot: PROJECT_ROOT,
    canonicalRoot: CANONICAL_PROJECT_ROOT,
    fixture: PROJECT_ROOT !== CANONICAL_PROJECT_ROOT,
    ...values,
  };
  const run = spawnSync(process.env.FINALIZE_LOCK_PYTHON || process.env.PYTHON || 'python3', [FS_BOUNDARY, 'fs-op'], {
    cwd: PROJECT_ROOT,
    env: process.env,
    input: JSON.stringify(request),
    encoding: 'utf8',
    maxBuffer: 48 * 1024 * 1024,
    timeout: 30000,
  });
  if (activeDependencyGraphMutex) assertDependencyGraphMutexProcess(activeDependencyGraphMutex);
  if (run.error) fail('FILESYSTEM_BOUNDARY_FAILED', run.error.message, 3);
  if (run.status === 88 && PROJECT_ROOT !== CANONICAL_PROJECT_ROOT &&
      process.env.FINALIZE_FS_TEST_CRASH_STAGE && process.env.FINALIZE_FS_TEST_CRASH_TARGET &&
      process.env.FINALIZE_FS_TEST_CRASH_SENTINEL && process.env.FINALIZE_FS_TEST_ROOT) {
    process.exit(88);
  }
  if (run.status !== 0) fail('FILESYSTEM_BOUNDARY_FAILED', String(run.stderr || 'filesystem worker failed').trim().slice(0, 500), 3);
  let envelope;
  try { envelope = JSON.parse(run.stdout); }
  catch (_) { fail('FILESYSTEM_BOUNDARY_FAILED', 'filesystem worker returned an invalid envelope', 3); }
  if (!envelope || envelope.version !== 1 || envelope.ok !== true) {
    const error = envelope && envelope.error || {};
    const wrapped = new Error(String(error.message || 'filesystem boundary rejected the operation'));
    wrapped.code = error.code === 'TARGET_EXISTS' ? 'EEXIST' : String(error.code || 'FILESYSTEM_BOUNDARY_FAILED');
    throw wrapped;
  }
  return envelope.result;
}
function statEntry(file, allowMissing = true) {
  ensureInside(PROJECT_ROOT, file, 'filesystem entry');
  return boundary('stat', { path: file, allowMissing });
}
function ensureDir(dir) {
  ensureInside(PROJECT_ROOT, dir, 'directory');
  try { return boundary('ensure-dir', { path: dir }).stat; }
  catch (error) { fail('RUNTIME_DIR_UNSAFE', String(error.message || error), error.code === 'DIRECTORY_CHANGED' ? 4 : 3); }
}
function inspectRealDirectory(dir, label) {
  ensureInside(PROJECT_ROOT, dir, label + ' directory');
  let result;
  try { result = boundary('stat', { path: dir, allowMissing: true }); }
  catch (error) { fail('RUNTIME_DIR_UNSAFE', label + ' directory cannot be inspected safely: ' + error.message, error.code === 'DIRECTORY_CHANGED' ? 4 : 3); }
  if (result.missing) return { exists: false };
  if (result.stat.kind !== 'directory') fail('RUNTIME_DIR_UNSAFE', label + ' path is not a real directory', 3);
  return { exists: true, ...result.stat };
}
function sameDirectoryIdentity(left, right) {
  return left && right && left.exists && right.exists && left.dev === right.dev && left.ino === right.ino &&
    left.mode === right.mode && left.size === right.size && left.mtimeNs === right.mtimeNs && left.ctimeNs === right.ctimeNs;
}
function boundedDirectoryNames(dir, label) {
  try {
    const result = boundary('list', { path: dir, maxEntries: MAX_RUNTIME_ENTRIES, allowMissing: true });
    return result.missing ? null : result.names;
  } catch (error) {
    if (error.code === 'DIRECTORY_TOO_LARGE') fail('RUNTIME_DIRECTORY_TOO_LARGE', label + ' directory exceeds the bounded entry limit', 1);
    fail(error.code === 'DIRECTORY_CHANGED' ? 'RUNTIME_DIR_CHANGED' : 'RUNTIME_DIR_UNSAFE', label + ' directory cannot be enumerated safely: ' + error.message, error.code === 'DIRECTORY_CHANGED' ? 4 : 3);
  }
}
function fsyncDir(dir) {
  try { boundary('fsync-dir', { path: dir }); } catch (_) {}
}
function writeExclusive(file, bytes, mode = 0o600) {
  ensureInside(PROJECT_ROOT, file, 'exclusive publication');
  return boundary('write-exclusive', { path: file, rawBase64: Buffer.from(bytes).toString('base64'), mode }).stat;
}
function atomicReplace(file, bytes, mode = 0o600) {
  ensureDir(path.dirname(file));
  const observed = statEntry(file, true);
  const expected = observed.missing ? null : observed.stat;
  return boundary('replace', { path: file, expected, rawBase64: Buffer.from(bytes).toString('base64'), mode, maxBytes: MARKER_MAX }).stat;
}
function safeReadSnapshot(file, max = INPUT_MAX) {
  ensureInside(PROJECT_ROOT, file, 'bounded file');
  try {
    const snapshot = rawReadSnapshot(file, max);
    const bytes = snapshot.bytes;
    try { new TextDecoder('utf-8', { fatal: true }).decode(bytes); }
    catch (_) { fail('INPUT_UTF8_INVALID', 'input must be canonical UTF-8', 2); }
    if (bytes.includes(0)) fail('INPUT_UTF8_INVALID', 'input contains a NUL byte', 2);
    return snapshot;
  } catch (error) {
    if (error instanceof TransitionError) throw error;
    if (error && ['PATH_MISSING', 'ENTRY_CHANGED', 'DIRECTORY_CHANGED'].includes(error.code)) fail('INPUT_CHANGED', 'input changed during bounded read', 4);
    if (error && ['ENTRY_UNSAFE', 'ENTRY_TOO_LARGE', 'DIRECTORY_UNSAFE', 'PATH_OUTSIDE_AUTHORITY'].includes(error.code)) fail('INPUT_UNSAFE', 'input must be a bounded regular file', 2);
    throw error;
  }
}
function rawReadSnapshot(file, max = INPUT_MAX) {
  ensureInside(PROJECT_ROOT, file, 'bounded file');
  try {
    const result = boundary('read', { path: file, maxBytes: max });
    return { bytes: Buffer.from(result.rawBase64, 'base64'), stat: result.stat };
  } catch (error) {
    if (error instanceof TransitionError) throw error;
    if (error && ['PATH_MISSING', 'ENTRY_CHANGED', 'DIRECTORY_CHANGED'].includes(error.code)) {
      fail('INPUT_CHANGED', 'input changed during bounded read', 4);
    }
    if (error && ['ENTRY_UNSAFE', 'ENTRY_TOO_LARGE', 'DIRECTORY_UNSAFE', 'PATH_OUTSIDE_AUTHORITY'].includes(error.code)) {
      fail('INPUT_UNSAFE', 'input must be a bounded regular file', 2);
    }
    throw error;
  }
}
function safeRead(file, max = INPUT_MAX) { return safeReadSnapshot(file, max).bytes; }

function dependencyGraphMutexWaitMs() {
  const raw = String(process.env.ORCHESTRATOR_DEPENDENCY_GRAPH_MUTEX_TIMEOUT_MS || '600000');
  if (!/^[1-9][0-9]{0,9}$/.test(raw)) fail('DEPENDENCY_GRAPH_MUTEX_TIMEOUT_INVALID', 'dependency-graph mutex timeout must be a positive integer', 2);
  return Math.min(Number(raw), 60 * 60 * 1000);
}

function assertDependencyGraphMutexProcess(lease) {
  const child = lease && lease.child;
  if (!child || lease.lost || lease.closed || child.exitCode !== null || child.signalCode !== null || child.killed || !Number.isInteger(child.pid)) {
    fail('DEPENDENCY_GRAPH_MUTEX_OWNERSHIP_LOST', 'dependency-graph kernel mutex helper exited or became uncertain', 4);
  }
  try { process.kill(child.pid, 0); }
  catch (error) {
    if (!error || error.code !== 'EPERM') fail('DEPENDENCY_GRAPH_MUTEX_OWNERSHIP_LOST', 'dependency-graph kernel mutex helper is no longer alive', 4);
  }
}

function dependencyGraphMutexRecord(lease, released = false) {
  let record;
  try { record = JSON.parse(safeRead(DEPENDENCY_GRAPH_MUTEX, 4096).toString('utf8')); }
  catch (error) { fail('DEPENDENCY_GRAPH_MUTEX_OWNERSHIP_LOST', 'dependency-graph mutex owner record is unreadable or unstable: ' + String(error.message || error).slice(0, 300), 4); }
  const fields = record && typeof record === 'object' && !Array.isArray(record) ? Object.keys(record).sort() : [];
  const expected = ['hostname', 'invocationId', 'pid', 'processStartId', 'released', 'startedAt', 'version'];
  if (fields.length !== expected.length || !expected.every((field, index) => fields[index] === field) ||
      record.version !== 1 || record.invocationId !== lease.invocationId || record.pid !== lease.child.pid ||
      typeof record.hostname !== 'string' || !record.hostname || record.hostname.length > 255 || record.released !== released ||
      !writerLeases.PROCESS_START_ID_RE.test(String(record.processStartId || '')) ||
      typeof record.startedAt !== 'string' || !Number.isFinite(Date.parse(record.startedAt))) {
    fail('DEPENDENCY_GRAPH_MUTEX_OWNERSHIP_LOST', 'dependency-graph mutex owner record does not identify this invocation', 4);
  }
  if (!released && !writerLeases.processIdentityMatches(record.pid, record.processStartId)) {
    fail('DEPENDENCY_GRAPH_MUTEX_OWNERSHIP_LOST', 'dependency-graph mutex process generation is no longer live', 4);
  }
  return record;
}

function assertDependencyGraphMutexHeld(lease) {
  assertDependencyGraphMutexProcess(lease);
  dependencyGraphMutexRecord(lease, false);
  assertDependencyGraphMutexProcess(lease);
}

async function acquireDependencyGraphMutex() {
  ensureDir(FINALIZATIONS_DIR);
  const invocationId = 'dependency-graph:transition:' + crypto.randomBytes(12).toString('hex');
  const python = process.env.FINALIZE_LOCK_PYTHON || process.env.PYTHON || 'python3';
  const child = spawn(python, [FS_BOUNDARY, DEPENDENCY_GRAPH_MUTEX, invocationId, PROJECT_ROOT], {
    cwd: PROJECT_ROOT, env: process.env, stdio: ['pipe', 'pipe', 'pipe'],
  });
  const lease = { child, invocationId, lost: false, releasing: false, closed: false, stderr: '' };
  child.stdin.on('error', () => {
    if (!lease.releasing) lease.lost = true;
    try { child.kill('SIGTERM'); } catch (_) {}
  });
  const waitMs = dependencyGraphMutexWaitMs();
  await new Promise((resolveLock, rejectLock) => {
    let stdout = '', settled = false;
    const timer = setTimeout(() => {
      if (settled) return;
      settled = true;
      try { child.kill('SIGKILL'); } catch (_) {}
      rejectLock(new TransitionError('DEPENDENCY_GRAPH_BUSY', 'dependency-graph mutex was not acquired within the bounded wait', 4));
    }, waitMs);
    const rejectOnce = (error) => {
      if (settled) return;
      settled = true; clearTimeout(timer);
      try { child.kill('SIGKILL'); } catch (_) {}
      rejectLock(error);
    };
    child.on('error', (error) => rejectOnce(new TransitionError('DEPENDENCY_GRAPH_MUTEX_FAILED', 'dependency-graph mutex helper could not start: ' + error.message, 3)));
    child.stderr.on('data', (chunk) => { lease.stderr = (lease.stderr + String(chunk)).slice(-2000); });
    child.stdout.on('data', (chunk) => {
      if (settled) return;
      stdout += String(chunk);
      if (stdout.length > 4096) return rejectOnce(new TransitionError('DEPENDENCY_GRAPH_MUTEX_FAILED', 'dependency-graph mutex helper output exceeded its bound', 3));
      if (!stdout.includes('LOCKED\n')) return;
      settled = true; clearTimeout(timer); resolveLock();
    });
    child.on('close', (status, signal) => {
      lease.closed = true;
      if (!lease.releasing) lease.lost = true;
      if (!settled) rejectOnce(new TransitionError('DEPENDENCY_GRAPH_MUTEX_FAILED', 'dependency-graph mutex helper exited before acquisition (' + String(status ?? signal) + ')' + (lease.stderr ? ': ' + lease.stderr : ''), 3));
    });
  });
  assertDependencyGraphMutexHeld(lease);
  activeDependencyGraphMutex = lease;
  return lease;
}

async function releaseDependencyGraphMutex(lease) {
  if (!lease || !lease.child) return;
  assertDependencyGraphMutexHeld(lease);
  lease.releasing = true;
  activeDependencyGraphMutex = null;
  const child = lease.child;
  if (lease.closed || child.exitCode !== null || child.signalCode !== null) return;
  await new Promise((resolveRelease) => {
    let done = false; let timer;
    const finish = () => { if (!done) { done = true; if (timer) clearTimeout(timer); resolveRelease(); } };
    child.once('close', finish);
    try { child.stdin.end('RELEASE\n'); } catch (_) { try { child.kill('SIGTERM'); } catch (_) {} }
    timer = setTimeout(() => { try { child.kill('SIGKILL'); } catch (_) {}; finish(); }, 2000);
  });
  if (!lease.closed || child.exitCode !== 0) {
    fail('DEPENDENCY_GRAPH_MUTEX_RELEASE_FAILED', 'dependency-graph mutex helper did not release cleanly', 4);
  }
}

function validateProposalBytes(bytes) {
  if (!Buffer.isBuffer(bytes) || bytes.length === 0) fail('INPUT_EMPTY', 'input proposal must be non-empty', 2);
  try { new TextDecoder('utf-8', { fatal: true }).decode(bytes); }
  catch (_) { fail('INPUT_UTF8_INVALID', 'input proposal must be valid UTF-8', 2); }
  if (bytes.includes(0)) fail('INPUT_UTF8_INVALID', 'input proposal contains a NUL byte', 2);
  return bytes;
}

function readBoundedStdin() {
  const chunks = [];
  let total = 0;
  try {
    while (total <= INPUT_MAX) {
      const capacity = Math.min(STDIN_CHUNK_BYTES, INPUT_MAX + 1 - total);
      const chunk = Buffer.allocUnsafe(capacity);
      const count = fs.readSync(0, chunk, 0, capacity, null);
      if (count === 0) break;
      chunks.push(chunk.subarray(0, count));
      total += count;
      if (total > INPUT_MAX) fail('INPUT_TOO_LARGE', 'stdin input proposal exceeds ' + INPUT_MAX + ' bytes', 2);
    }
  } catch (error) {
    if (error instanceof TransitionError) throw error;
    fail('INPUT_STDIN_READ_FAILED', 'stdin input proposal could not be read safely', 2);
  }
  return validateProposalBytes(Buffer.concat(chunks, total));
}

function readTransitionInput(raw) {
  if (raw !== '-') fail('INVOCATION_INVALID', '--input must be - (bounded stdin)', 2);
  return readBoundedStdin();
}

const FILE_PROOF_FIELDS = Object.freeze(['ctimeNs', 'dev', 'hash', 'ino', 'kind', 'mode', 'mtimeNs', 'path', 'size']);
function proofFromSnapshot(file, snapshot) {
  return {
    path: file,
    hash: hash(snapshot.bytes),
    dev: String(snapshot.stat.dev),
    ino: String(snapshot.stat.ino),
    kind: snapshot.stat.kind,
    mode: snapshot.stat.mode,
    size: snapshot.stat.size,
    mtimeNs: String(snapshot.stat.mtimeNs),
    ctimeNs: String(snapshot.stat.ctimeNs),
  };
}
function proofFromStat(file, stat) {
  return {
    path: file,
    hash: null,
    dev: String(stat.dev),
    ino: String(stat.ino),
    kind: stat.kind || 'file',
    mode: stat.mode,
    size: stat.size,
    mtimeNs: String(stat.mtimeNs),
    ctimeNs: String(stat.ctimeNs),
  };
}
function fileProofShape(value, expectedPath = null) {
  const fields = value && typeof value === 'object' && !Array.isArray(value) ? Object.keys(value).sort() : [];
  return fields.length === FILE_PROOF_FIELDS.length && fields.every((field, index) => field === FILE_PROOF_FIELDS[index]) &&
    typeof value.path === 'string' && (!expectedPath || value.path === expectedPath) &&
    (value.hash === null || core.HASH_RE.test(String(value.hash || ''))) &&
    ['dev', 'ino'].every((field) => /^(?:0|[1-9][0-9]*)$/.test(String(value[field] || ''))) &&
    ['mtimeNs', 'ctimeNs'].every((field) => /^-?(?:0|[1-9][0-9]*)$/.test(String(value[field] || '')) && value[field] !== '-0') &&
    value.kind === 'file' && ['mode', 'size'].every((field) => Number.isSafeInteger(value[field]) && value[field] >= 0);
}
function hashedFileProofShape(value, expectedPath = null) {
  return fileProofShape(value, expectedPath) && core.HASH_RE.test(String(value.hash || ''));
}
function sameExactProof(left, right) {
  return fileProofShape(left) && fileProofShape(right) && FILE_PROOF_FIELDS.every((field) => left[field] === right[field]);
}
function sameFileGeneration(left, right) {
  return fileProofShape(left) && fileProofShape(right) &&
    ['hash', 'dev', 'ino', 'kind', 'mode', 'size', 'mtimeNs'].every((field) => left[field] === right[field]);
}
function readFileProof(file, max = INPUT_MAX) {
  // Ownership proofs cover bytes, inode and metadata. They intentionally do
  // not require UTF-8 so the drop recovery path can safely own and remove a
  // malformed task while content-producing transitions still use safeRead().
  return proofFromSnapshot(file, rawReadSnapshot(file, max));
}
function readProofLike(file, expected) {
  if (!fileProofShape(expected)) fail('TRANSITION_PROOF_INVALID', 'owned file proof is malformed', 1);
  if (expected.hash !== null) return readFileProof(file);
  const observed = statEntry(file, false);
  if (!observed || observed.missing || !observed.stat) {
    fail('TRANSITION_SOURCE_CHANGED', 'owned file disappeared', 4, { path: file });
  }
  return proofFromStat(file, observed.stat);
}
function assertExactFileProof(proof, code = 'TRANSITION_SOURCE_CHANGED', exitCode = 4) {
  let current;
  try { current = readProofLike(proof.path, proof); }
  catch (error) {
    if (error instanceof TransitionError || error && ['ENOENT', 'ELOOP', 'ESTALE'].includes(error.code)) {
      fail(code, 'owned file disappeared or became unsafe', exitCode, { path: proof.path });
    }
    throw error;
  }
  if (!sameExactProof(current, proof)) fail(code, 'owned file generation changed', exitCode, { path: proof.path });
  return current;
}
function assertGenerationAt(file, proof, code, message) {
  let current;
  try { current = readProofLike(file, proof); }
  catch (error) {
    if (error instanceof TransitionError || error && ['ENOENT', 'ELOOP', 'ESTALE'].includes(error.code)) {
      fail(code, message, 4, { path: file });
    }
    throw error;
  }
  if (!sameFileGeneration(current, proof)) fail(code, message, 4, { path: file });
  return current;
}

function guardPath(stem) { return path.join(TRANSITION_GUARDS_DIR, stem + '.json'); }
function guardBytes(record) { return Buffer.from(JSON.stringify(record, null, 2) + '\n'); }
function validGuard(record, stem) {
  const fields = record && typeof record === 'object' && !Array.isArray(record) ? Object.keys(record).sort() : [];
  const expected = ['createdAt', 'hostname', 'pid', 'processStartId', 'stem', 'token', 'version'];
  return fields.length === expected.length && fields.every((field, index) => field === expected[index]) &&
    record.version === 1 && record.stem === stem && core.safeIntegerId(stem) !== null &&
    Number.isInteger(record.pid) && record.pid > 0 && typeof record.hostname === 'string' && record.hostname &&
    /^[a-f0-9]{48}$/.test(String(record.token || '')) && typeof record.createdAt === 'string' && Number.isFinite(Date.parse(record.createdAt)) &&
    (process.platform === 'linux' || process.platform === 'darwin' || process.platform === 'win32'
      ? writerLeases.PROCESS_START_ID_RE.test(String(record.processStartId || ''))
      : record.processStartId === null);
}
function readGuard(file, stem) {
  let snapshot;
  try { snapshot = safeReadSnapshot(file, GUARD_MAX); }
  catch (error) {
    if (error instanceof TransitionError && error.code === 'INPUT_CHANGED') fail('TRANSITION_GUARD_CHANGED', 'transition guard changed while being read', 4);
    if (error instanceof TransitionError) fail('TRANSITION_GUARD_INVALID', 'transition guard is unsafe or oversized', 1);
    throw error;
  }
  const bytes = snapshot.bytes;
  let record;
  try { record = JSON.parse(bytes.toString('utf8')); }
  catch (_) { fail('TRANSITION_GUARD_INVALID', 'transition guard is not JSON', 1); }
  if (!validGuard(record, stem) || !bytes.equals(guardBytes(record))) fail('TRANSITION_GUARD_INVALID', 'transition guard does not match its canonical owner contract', 1);
  return { record, bytes, stat: snapshot.stat, hash: hash(bytes) };
}
function guardOwnerActive(record) {
  if (record.hostname !== os.hostname()) return true;
  return writerLeases.processIdentityMatches(record.pid, record.processStartId);
}
function guardRecoveryNames(stem) {
  const names = boundedDirectoryNames(TRANSITION_GUARDS_DIR, 'transition-guard') || [];
  const prefix = '.' + stem + '.guard-recovery-';
  const pattern = new RegExp('^\\.' + stem.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '\\.guard-recovery-[a-f0-9]{36}$');
  const malformed = names.find((name) => name.startsWith(prefix) && !pattern.test(name));
  if (malformed) fail('TRANSITION_GUARD_RECOVERY_UNSAFE', 'malformed transition-guard recovery prefix blocks ownership inference: ' + malformed, 1);
  return names.filter((name) => pattern.test(name));
}
function sameGuardGeneration(left, right) {
  return left.record.token === right.record.token && left.hash === right.hash &&
    left.stat.dev === right.stat.dev && left.stat.ino === right.stat.ino;
}
function restoreGuardQuarantine(quarantine, target) {
  try {
    const retained = readGuard(quarantine, path.basename(target, '.json'));
    boundary('link', { source: quarantine, target, expected: retained.stat, maxBytes: GUARD_MAX });
    boundary('remove', { path: quarantine, expected: retained.stat, maxBytes: GUARD_MAX });
    return true;
  } catch (_) { return false; }
}
function reclaimTransitionGuard(target, current, stem) {
  const quarantine = path.join(TRANSITION_GUARDS_DIR, '.' + stem + '.guard-recovery-' + crypto.randomBytes(18).toString('hex'));
  try { boundary('move', { source: target, target: quarantine, expected: current.stat, maxBytes: GUARD_MAX }); }
  catch (error) {
    if (error && ['ENOENT', 'ESTALE'].includes(error.code)) fail('TRANSITION_GUARD_CHANGED', 'transition guard changed before recovery', 4);
    throw error;
  }
  let moved;
  try { moved = readGuard(quarantine, stem); }
  catch (error) {
    restoreGuardQuarantine(quarantine, target);
    throw error;
  }
  if (moved.hash !== current.hash || moved.stat.dev !== current.stat.dev || moved.stat.ino !== current.stat.ino) {
    if (!restoreGuardQuarantine(quarantine, target)) {
      fail('TRANSITION_GUARD_RECOVERY_REQUIRED', 'a racing transition guard was quarantined and could not be restored no-clobber', 4);
    }
    fail('TRANSITION_GUARD_CHANGED', 'transition guard generation changed before recovery', 4);
  }
  boundary('remove', { path: quarantine, expected: moved.stat, maxBytes: GUARD_MAX });
}
function consumeGuardRecovery(file, current, stem) {
  const rotated = path.join(TRANSITION_GUARDS_DIR, '.' + stem + '.guard-recovery-' + crypto.randomBytes(18).toString('hex'));
  try { boundary('move', { source: file, target: rotated, expected: current.stat, maxBytes: GUARD_MAX }); }
  catch (error) {
    if (error && ['ENOENT', 'ESTALE'].includes(error.code)) fail('TRANSITION_GUARD_CHANGED', 'private transition-guard recovery generation changed before reconciliation', 4);
    throw error;
  }
  let moved;
  try { moved = readGuard(rotated, stem); }
  catch (error) {
    if (!restoreGuardQuarantine(rotated, file)) fail('TRANSITION_GUARD_RECOVERY_REQUIRED', 'private transition-guard recovery generation could not be restored', 4);
    throw error;
  }
  if (!sameGuardGeneration(moved, current)) {
    if (!restoreGuardQuarantine(rotated, file)) fail('TRANSITION_GUARD_RECOVERY_REQUIRED', 'private transition-guard recovery generation changed and could not be restored', 4);
    fail('TRANSITION_GUARD_CHANGED', 'private transition-guard recovery generation changed during reconciliation', 4);
  }
  boundary('remove', { path: rotated, expected: moved.stat, maxBytes: GUARD_MAX });
}
function reconcileGuardRecovery(stem, recovery) {
  const names = guardRecoveryNames(stem);
  if (!names.length) return;
  if (!recovery) fail('TRANSITION_GUARD_RECOVERY_REQUIRED', 'a private transition-guard recovery generation requires reconciliation', 1);
  if (names.length !== 1) fail('TRANSITION_GUARD_RECOVERY_AMBIGUOUS', 'multiple private transition-guard recovery generations require manual inspection', 1);
  const file = path.join(TRANSITION_GUARDS_DIR, names[0]);
  const retained = readGuard(file, stem);
  if (guardOwnerActive(retained.record)) fail('TRANSITION_BUSY', 'the owner of a private transition-guard recovery generation is still active', 4);

  const target = guardPath(stem);
  let published = null;
  try { published = readGuard(target, stem); }
  catch (error) {
    if (!error || !['ENOENT', 'ESTALE'].includes(error.code)) throw error;
  }
  // A crash in no-clobber restoration may leave both names hard-linked to the
  // same generation. Any distinct published generation is ambiguous and must
  // remain untouched for inspection.
  if (published && !sameGuardGeneration(published, retained)) {
    fail('TRANSITION_GUARD_RECOVERY_AMBIGUOUS', 'private and published transition guards identify different generations', 1);
  }
  consumeGuardRecovery(file, retained, stem);
}
function acquireTransitionGuard(stem, recovery) {
  ensureDir(TRANSITION_GUARDS_DIR);
  reconcileGuardRecovery(stem, recovery);
  const processStartId = writerLeases.captureProcessStartId(process.pid);
  if ((process.platform === 'linux' || process.platform === 'darwin' || process.platform === 'win32') &&
      !writerLeases.PROCESS_START_ID_RE.test(String(processStartId || ''))) {
    fail('TRANSITION_GUARD_UNAVAILABLE', 'exact transition process identity is unavailable', 3);
  }
  const record = {
    version: 1, stem, token: crypto.randomBytes(24).toString('hex'), pid: process.pid,
    processStartId: processStartId || null, hostname: os.hostname(), createdAt: now()
  };
  const target = guardPath(stem), bytes = guardBytes(record);
  while (true) {
    try {
      writeExclusive(target, bytes);
      const published = readGuard(target, stem);
      if (published.record.token !== record.token || !published.bytes.equals(bytes)) fail('TRANSITION_GUARD_CHANGED', 'published transition guard does not match its owner', 4);
      return { target, record, hash: published.hash, stat: published.stat };
    } catch (error) {
      if (!error || error.code !== 'EEXIST') throw error;
      const current = readGuard(target, stem);
      if (guardOwnerActive(current.record)) fail('TRANSITION_BUSY', 'another process owns this task transition', 4);
      if (!recovery) fail('TRANSITION_RECOVERY_REQUIRED', 'a dead transition owner must be reconciled through recover before a new operation', 1);
      reclaimTransitionGuard(target, current, stem);
    }
  }
}
function releaseTransitionGuard(handle) {
  const current = readGuard(handle.target, handle.record.stem);
  if (current.record.token !== handle.record.token || current.hash !== handle.hash ||
      current.stat.dev !== handle.stat.dev || current.stat.ino !== handle.stat.ino) {
    fail('TRANSITION_GUARD_CHANGED', 'refusing to release a foreign transition guard generation', 4);
  }
  const quarantine = path.join(TRANSITION_GUARDS_DIR, '.' + handle.record.stem + '.guard-recovery-' + crypto.randomBytes(18).toString('hex'));
  boundary('move', { source: handle.target, target: quarantine, expected: current.stat, maxBytes: GUARD_MAX });
  if (process.env.TASK_TRANSITION_TEST_CRASH_AFTER_GUARD_DETACH === '1' && TASKS_DIR !== HERE) process.exit(88);
  const moved = readGuard(quarantine, handle.record.stem);
  if (moved.hash !== handle.hash || moved.stat.dev !== handle.stat.dev || moved.stat.ino !== handle.stat.ino) {
    if (!restoreGuardQuarantine(quarantine, handle.target)) fail('TRANSITION_GUARD_RECOVERY_REQUIRED', 'transition guard release requires recovery', 4);
    fail('TRANSITION_GUARD_CHANGED', 'transition guard changed at the release boundary', 4);
  }
  boundary('remove', { path: quarantine, expected: moved.stat, maxBytes: GUARD_MAX });
}

function markerPath(id) { return path.join(STATE_DIR, id + '.json'); }
function privateDir(id) { return path.join(STATE_DIR, '.private', id); }
function reconcileMarkerReplacements() {
  ensureDir(STATE_DIR);
  try {
    return boundary('recover-replaces', {
      path: STATE_DIR,
      maxEntries: MAX_RUNTIME_ENTRIES,
      maxBytes: MARKER_MAX,
    });
  } catch (error) {
    fail('TRANSITION_MARKER_RECOVERY_REQUIRED', 'durable marker replacement requires exact recovery: ' + String(error.message || error).slice(0, 300), 4);
  }
}
function markerBytes(marker) {
  const bytes = Buffer.from(JSON.stringify(marker, null, 2) + '\n');
  if (bytes.length > MARKER_MAX) fail('MARKER_TOO_LARGE', 'transition marker exceeds its bound', 3);
  return bytes;
}
function writeMarker(marker, create = false) {
  ensureDir(STATE_DIR);
  marker.updatedAt = now();
  if (create) writeExclusive(markerPath(marker.transactionId), markerBytes(marker));
  else atomicReplace(markerPath(marker.transactionId), markerBytes(marker));
}
function cleanupMarker(marker, committed = marker.phase === 'complete') {
  // Cleanup is after the durable commit boundary. Never erase the marker if
  // its private backups could not be removed, and never let a cleanup failure
  // fall through to the mutation rollback path.
  if (process.env.TASK_TRANSITION_TEST_FAIL_CLEANUP === '1' && TASKS_DIR !== HERE) {
    fail('TRANSITION_CLEANUP_REQUIRED', 'fixture interrupted cleanup after the transition committed', 4);
  }
  if (committed) assertHistoryOwned(marker);
  cleanupPrivateArtifacts(marker);
  try {
    const markerProof = readFileProof(markerPath(marker.transactionId), MARKER_MAX);
    boundary('remove', { path: markerPath(marker.transactionId), expected: markerProof, maxBytes: MARKER_MAX });
  }
  catch (error) {
    if (!error || error.code !== 'PATH_MISSING') fail('TRANSITION_CLEANUP_REQUIRED', 'committed transition marker cleanup failed: ' + String(error.message || error).slice(0, 300), 4);
  }
}

function artifactPath(column, stem) {
  return path.join(TASKS_DIR, column, stem + (column === 'pending' ? '.questions.md' : '.md'));
}
function durablePaths(stem) {
  return { backlog: artifactPath('backlog', stem), pending: artifactPath('pending', stem), todo: artifactPath('todo', stem), done: artifactPath('done', stem) };
}
function existsRegular(file) {
  const result = statEntry(file, true);
  return !result.missing && result.stat.kind === 'file';
}
function existsAny(file) {
  return !statEntry(file, true).missing;
}
function taskLockRecoveryNames(stem, names) {
  const prefix = '.' + stem + '.json.release-';
  const pattern = new RegExp('^\\.' + stem.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '\\.json\\.release-[a-f0-9]{36}$');
  const malformed = (names || []).find((name) => name.startsWith(prefix) && !pattern.test(name));
  if (malformed) fail('LOCK_RELEASE_RECOVERY_UNSAFE', 'malformed task-lock release recovery prefix blocks mutation: ' + malformed, 1);
  return (names || []).filter((name) => pattern.test(name));
}
function taskLockProofShape(value, stem) {
  const fields = value && typeof value === 'object' && !Array.isArray(value) ? Object.keys(value).sort() : [];
  const expected = ['ctimeNs', 'dev', 'hash', 'ino', 'kind', 'mode', 'mtimeNs', 'runId', 'sessionId', 'size', 'stage', 'stem'];
  return fields.length === expected.length && fields.every((field, index) => field === expected[index]) &&
    value.stem === stem && core.safeIntegerId(stem) !== null && ['task-prep', 'orchestrator'].includes(value.stage) &&
    typeof value.runId === 'string' && value.runId.length >= 8 && typeof value.sessionId === 'string' && writerLeases.SESSION_ID_RE.test(value.sessionId) &&
    core.HASH_RE.test(String(value.hash || '')) && value.kind === 'file' &&
    ['dev', 'ino'].every((field) => /^(?:0|[1-9][0-9]*)$/.test(String(value[field] || ''))) &&
    ['mtimeNs', 'ctimeNs'].every((field) => /^-?(?:0|[1-9][0-9]*)$/.test(String(value[field] || '')) && value[field] !== '-0') &&
    ['mode', 'size'].every((field) => Number.isSafeInteger(value[field]) && value[field] >= 0);
}
function readOwnedTaskLock(stem, expectedStage, expectedSessionId = null) {
  const directory = inspectRealDirectory(LOCKS_DIR, 'task-lock');
  if (!directory.exists) fail('TASK_LOCK_MISSING', 'canonical task lock is required for this mutation', 1);
  const names = boundedDirectoryNames(LOCKS_DIR, 'task-lock');
  const retained = taskLockRecoveryNames(stem, names);
  if (retained.length) fail('LOCK_RELEASE_RECOVERY_REQUIRED', 'a retained task-lock release generation requires reconciliation before mutation', 1, { paths: retained });
  const file = path.join(LOCKS_DIR, stem + '.json');
  let snapshot;
  try { snapshot = safeReadSnapshot(file, TASK_LOCK_MAX); }
  catch (error) {
    if (error instanceof TransitionError && error.code === 'INPUT_CHANGED') fail('TASK_LOCK_CHANGED', 'task lock changed during bounded ownership read', 4);
    if (error instanceof TransitionError) fail('TASK_LOCK_INVALID', 'task lock is not a bounded regular ownership record', 1);
    if (error && ['ENOENT', 'ELOOP', 'ESTALE'].includes(error.code)) fail('TASK_LOCK_CHANGED', 'task lock changed during bounded ownership read', 4);
    throw error;
  }
  const after = inspectRealDirectory(LOCKS_DIR, 'task-lock');
  if (!sameDirectoryIdentity(directory, after)) fail('RUNTIME_DIR_CHANGED', 'task-lock directory changed during ownership read', 4);
  let record;
  try { record = JSON.parse(snapshot.bytes.toString('utf8')); }
  catch (_) { fail('TASK_LOCK_INVALID', 'task lock is not valid JSON', 1); }
  if (!core.canonicalLockV1(record, stem)) fail('TASK_LOCK_INVALID', 'task lock does not match the canonical v1 ownership contract', 1);
  if (record.stage !== expectedStage) fail('TASK_LOCK_STAGE_MISMATCH', 'task lock stage does not authorize this transition', 1);
  if (expectedSessionId && record.sessionId !== expectedSessionId) fail('TASK_LOCK_OWNER_MISMATCH', 'task lock session does not match the verified writer authority', 1);
  return {
    stem,
    stage: record.stage,
    runId: record.runId,
    sessionId: record.sessionId,
    hash: hash(snapshot.bytes),
    dev: String(snapshot.stat.dev),
    ino: String(snapshot.stat.ino),
    kind: snapshot.stat.kind,
    mode: snapshot.stat.mode,
    size: snapshot.stat.size,
    mtimeNs: String(snapshot.stat.mtimeNs),
    ctimeNs: String(snapshot.stat.ctimeNs),
  };
}
function sameTaskLockProof(left, right) {
  return taskLockProofShape(left, left && left.stem) && taskLockProofShape(right, right && right.stem) &&
    Object.keys(left).every((field) => left[field] === right[field]);
}
function assertTaskLockOwned(proof) {
  if (!proof) return;
  const current = readOwnedTaskLock(proof.stem, proof.stage, proof.sessionId);
  if (!sameTaskLockProof(current, proof)) fail('TASK_LOCK_CHANGED', 'task lock generation changed after transition admission', 4);
}
function maybeMutateFixtureTaskLock(proof) {
  if (!proof || taskLockFixtureMutated || process.env.TASK_TRANSITION_TEST_MUTATE_LOCK_BEFORE_RECONFIRM !== '1' || TASKS_DIR === HERE) return;
  taskLockFixtureMutated = true;
  const file = path.join(LOCKS_DIR, proof.stem + '.json');
  const current = safeRead(file, TASK_LOCK_MAX);
  atomicReplace(file, Buffer.concat([current, Buffer.from(' ')]), 0o600);
}
function requireTaskLockAbsent(stem) {
  const directory = inspectRealDirectory(LOCKS_DIR, 'task-lock');
  if (!directory.exists) return;
  const names = boundedDirectoryNames(LOCKS_DIR, 'task-lock');
  const retained = taskLockRecoveryNames(stem, names)[0];
  if (retained) {
    fail('LOCK_RELEASE_RECOVERY_REQUIRED', 'a quarantined lock generation still owns this task; recover it before the transition', 1, {
      lockPath: path.relative(PROJECT_ROOT, path.join(LOCKS_DIR, retained)).split(path.sep).join('/')
    });
  }
  const file = path.join(LOCKS_DIR, stem + '.json');
  let stat;
  try {
    const entry = statEntry(file, true);
    if (entry.missing) {
      const after = inspectRealDirectory(LOCKS_DIR, 'task-lock');
      if (!sameDirectoryIdentity(directory, after)) fail('RUNTIME_DIR_CHANGED', 'task-lock directory changed during the ownership check', 4);
      return;
    }
    stat = entry.stat;
  } catch (error) { throw error; }
  const after = inspectRealDirectory(LOCKS_DIR, 'task-lock');
  if (!sameDirectoryIdentity(directory, after)) fail('RUNTIME_DIR_CHANGED', 'task-lock directory changed during the ownership check', 4);
  // A lock is runtime ownership, not disposable task content. Drop/reopen do
  // not possess its immutable receipt and therefore must never infer that it
  // is abandoned or transactionally delete it. Even an unsafe entry blocks.
  fail('TASK_LOCK_PRESENT', 'task lock ownership is still present; resume or recover its exact owner before this transition', 1, {
    lockPath: path.relative(PROJECT_ROOT, file).split(path.sep).join('/'),
    lockKind: stat.kind
  });
}
function validate(options) {
  return observedValidation('state', options, () => core.validateTaskState({ tasksDir: TASKS_DIR, repoRoot: PROJECT_ROOT, includeRuntime: false, ...options }));
}
function validateAction(options) {
  return observedValidation('action', options, () => core.validateAction({ tasksDir: TASKS_DIR, repoRoot: PROJECT_ROOT, includeRuntime: false, ...options }));
}
function requireVerdict(result, code) {
  if (!result.ok) fail(code, 'canonical validation failed', 1, { result });
  return result;
}
function requireRevision(stem, expected, action) {
  if (!core.HASH_RE.test(String(expected || ''))) fail('SOURCE_REVISION_REQUIRED', '--source-revision is required', 2);
  const result = validateAction({ stem, action, checkIndex: true });
  const admission = core.actionAdmission(result);
  if (!admission.ok) fail('TRANSITION_PRECONDITION_FAILED', 'canonical action admission failed', 1, {
    result, blockers: admission.blockers
  });
  if (result.sourceRevision !== expected) fail('TRANSITION_SOURCE_CHANGED', 'task source revision changed before mutation', 4, { expected, observed: result.sourceRevision, result });
  return result;
}
function requireDropRevision(stem, expected) {
  if (!core.HASH_RE.test(String(expected || ''))) fail('SOURCE_REVISION_REQUIRED', '--source-revision is required', 2);
  const result = validateAction({ stem, action: 'drop', checkIndex: true });
  const admission = core.dropAdmission(result, stem);
  if (!admission.ok) fail('TRANSITION_PRECONDITION_FAILED', 'canonical drop admission failed', 1, {
    result, blockers: admission.blockers
  });
  if (result.sourceRevision !== expected) {
    fail('TRANSITION_SOURCE_CHANGED', 'task source revision changed before mutation', 4, {
      expected, observed: result.sourceRevision, result
    });
  }
  return result;
}
function requireStateRevision(stem, expected, allowedStates) {
  if (!core.HASH_RE.test(String(expected || ''))) fail('SOURCE_REVISION_REQUIRED', '--source-revision is required', 2);
  const result = validate({ stem, checkIndex: true });
  const admission = core.actionAdmission(result);
  if (!admission.ok) fail('TRANSITION_PRECONDITION_FAILED', 'canonical action admission failed', 1, {
    result, blockers: admission.blockers
  });
  if (!allowedStates.includes(result.observedState)) fail('TRANSITION_PRECONDITION_FAILED', 'operation is not allowed from ' + result.observedState, 1, { result });
  if (result.sourceRevision !== expected) fail('TRANSITION_SOURCE_CHANGED', 'task source revision changed before mutation', 4, { expected, observed: result.sourceRevision, result });
  return result;
}

function activeFinalizationMarker() {
  let names;
  try { names = boundedDirectoryNames(FINALIZATIONS_DIR, 'finalization'); }
  catch (error) {
    if (error instanceof TransitionError) throw error;
    return 'unreadable finalization directory';
  }
  if (names === null) return null;
  return names.find((name) => name.endsWith('.json') && name !== '.mutex.json') || null;
}
function verifyAuthority(stem, args) {
  const authorityStem = args['--authority-stem'] || stem;
  if (core.safeIntegerId(authorityStem) === null) fail('INVOCATION_INVALID', '--authority-stem is invalid', 2);
  // Fixture roots can opt out only when they are not the checked-in task root.
  if (process.env.TASK_TRANSITION_TEST_UNLEASED === '1' && TASKS_DIR !== HERE) {
    const fixtureSession = process.env.TASK_TRANSITION_TEST_SESSION_ID || null;
    if (fixtureSession && !writerLeases.SESSION_ID_RE.test(fixtureSession)) fail('WRITER_AUTHORITY_REQUIRED', 'fixture session id is invalid', 2);
    var fixtureProof = null;
    var fixtureSentinel = String(process.env.TASK_TRANSITION_TEST_AUTHORITY_SENTINEL || '');
    if (fixtureSentinel) {
      fixtureSentinel = path.resolve(fixtureSentinel);
      ensureInside(PROJECT_ROOT, fixtureSentinel, 'fixture authority sentinel');
      try { fixtureProof = readFileProof(fixtureSentinel, 256); }
      catch (error) { fail('WRITER_AUTHORITY_REQUIRED', 'fixture writer authority is absent before admission', 2); }
    }
    const fixtureAuthority = {
      kind: 'fixture', authorityStem, sessionId: fixtureSession,
      bypass: !fixtureSession, fixtureProof
    };
    maybePauseFixture('authority-verified');
    return fixtureAuthority;
  }
  const sessionId = process.env.ORCHESTRATOR_WRITER_SESSION_ID;
  if (sessionId) {
    const run = spawnSync(process.execPath, [path.join(HERE, 'writer-lease.mjs'), 'verify-session', '--guard-finalization', '--session-id', sessionId, '--stem', authorityStem], {
      cwd: PROJECT_ROOT, env: process.env, encoding: 'utf8', timeout: 15000
    });
    if (run.status !== 0) fail('WRITER_AUTHORITY_REQUIRED', String(run.stderr || run.stdout || 'site writer session verification failed').trim().slice(0, 500), 2);
    var siteReceipt;
    try { siteReceipt = JSON.parse(run.stdout); } catch (error) { siteReceipt = null; }
    if (!siteReceipt || siteReceipt.verified !== true || !writerLeases.LEASE_ID_RE.test(String(siteReceipt.leaseId || ''))) {
      fail('WRITER_AUTHORITY_REQUIRED', 'site writer session returned an invalid authority receipt', 2);
    }
    const siteAuthority = { kind: 'site-session', authorityStem, sessionId, leaseId: siteReceipt.leaseId, bypass: false };
    maybePauseFixture('authority-verified');
    return siteAuthority;
  }
  const leaseId = args['--lease-id'], token = args['--lease-token'];
  if (!writerLeases.LEASE_ID_RE.test(String(leaseId || '')) || !/^[a-f0-9]{32,128}$/.test(String(token || ''))) {
    fail('WRITER_AUTHORITY_REQUIRED', 'a verified site session or guarded writer lease is required', 2);
  }
  const scan = writerLeases.scan(LEASES_DIR, PROJECT_ROOT);
  if (scan.issues.length) fail('WRITER_AUTHORITY_REQUIRED', scan.issues[0].message, 2);
  const own = scan.active.filter((row) => row.leaseId === leaseId && row.token === token && row.stem === authorityStem && !row.unverified);
  if (own.length !== 1) fail('WRITER_AUTHORITY_REQUIRED', 'writer lease is absent, stale, or belongs to another task', 2);
  const conflict = scan.active.find((row) => row.leaseId !== leaseId && (row.stem === authorityStem || row.key === 'task:' + authorityStem));
  if (conflict) fail('WORKSPACE_WRITER_ACTIVE', 'another active writer owns this task', 2);
  const marker = activeFinalizationMarker();
  if (marker) fail('FINALIZATION_ACTIVE', 'durable finalization marker exists: ' + marker, 2);
  const leaseAuthority = {
    kind: 'bounded-lease', authorityStem, sessionId: own[0].sessionId,
    leaseId, leaseToken: token, key: own[0].key, bypass: false
  };
  maybePauseFixture('authority-verified');
  return leaseAuthority;
}

function reverifyAuthority(authority) {
  if (!authority || !authority.kind) fail('WRITER_AUTHORITY_LOST', 'writer authority receipt is unavailable after serialization', 4);
  if (authority.kind === 'fixture') {
    if (authority.fixtureProof) {
      var currentFixtureProof;
      try { currentFixtureProof = readFileProof(authority.fixtureProof.path, 256); }
      catch (error) { fail('WRITER_AUTHORITY_LOST', 'fixture writer authority was revoked while waiting for serialization', 4); }
      if (!sameFileGeneration(currentFixtureProof, authority.fixtureProof)) {
        fail('WRITER_AUTHORITY_LOST', 'fixture writer authority generation changed while waiting for serialization', 4);
      }
    }
    maybePauseFixture('authority-reverified');
    return;
  }

  var command;
  if (authority.kind === 'site-session') {
    command = ['verify-session', '--session-id', authority.sessionId, '--stem', authority.authorityStem];
  } else if (authority.kind === 'bounded-lease') {
    command = ['verify', '--lease-id', authority.leaseId, '--token', authority.leaseToken,
      '--session-id', authority.sessionId, '--stem', authority.authorityStem];
  } else {
    fail('WRITER_AUTHORITY_LOST', 'writer authority kind changed while waiting for serialization', 4);
  }
  const run = spawnSync(process.execPath, [path.join(HERE, 'writer-lease.mjs'), ...command], {
    cwd: PROJECT_ROOT, env: process.env, encoding: 'utf8', timeout: 15000
  });
  if (run.status !== 0) {
    fail('WRITER_AUTHORITY_LOST', String(run.stderr || run.stdout || 'writer authority expired during serialization').trim().slice(0, 500), 4);
  }
  var receipt;
  try { receipt = JSON.parse(run.stdout); } catch (error) { receipt = null; }
  if (!receipt || receipt.verified !== true || receipt.leaseId !== authority.leaseId ||
      receipt.sessionId !== authority.sessionId || receipt.stem !== authority.authorityStem) {
    fail('WRITER_AUTHORITY_LOST', 'writer authority receipt changed while waiting for serialization', 4);
  }
  maybePauseFixture('authority-reverified');
}

function requireAuthorityScope(stem, operation, pre, authority) {
  if (authority.authorityStem === stem) return;
  const meta = pre && pre._model && pre._model.metadata && pre._model.metadata.get(stem);
  if (operation !== 'promote' || pre.observedState !== 'backlog' || !meta ||
      !Array.isArray(meta.lineage) || meta.lineage.length !== 1 || meta.lineage[0] !== authority.authorityStem) {
    fail('WRITER_AUTHORITY_SCOPE_MISMATCH', 'parent authority may promote only its exact backlog child with canonical split lineage', 2);
  }
  // For the TYPED delegated families (structural split and the
  // test-foundation prerequisite) the canonical Source Ref must be the
  // delegating parent itself — a foundation child with foreign provenance
  // under a crafted Origin bullet is a scope violation, not a promotable
  // child. Children of other Source kinds keep the existing lineage-only
  // contract above.
  const childSource = meta.origin;
  if (childSource && childSource.kind === 'follow-up' &&
      ['task-split', 'test-foundation-prerequisite'].includes(childSource.type) &&
      childSource.ref !== authority.authorityStem) {
    fail('WRITER_AUTHORITY_SCOPE_MISMATCH', 'a delegated ' + childSource.type + ' child must carry the delegating parent as its Source Ref', 2);
  }
}

function relayIndexObservations(stderr, check, success) {
  const action = check ? 'index-check' : 'index-publish';
  const phases = check ? ['pre', 'post'] : ['pre', 'pre', 'pre', 'post', 'post', 'post'];
  const events = core.projectObservationStream(String(stderr || ''), {
    caller: 'server', scope: 'all', action, transition: null,
  }, success ? {
    expectedCount: phases.length, syntheticPhases: phases,
    fallbackCode: 'INDEX_OBSERVATION_INVALID',
  } : {
    syntheticPhase: 'post', fallbackCode: 'INDEX_OBSERVATION_INVALID',
  });
  for (const event of events) process.stderr.write('[task-state] ' + JSON.stringify(event) + '\n');
}

function runIndex(check = false) {
  const command = [path.join(HERE, 'regen-index.py')];
  if (check) command.push('--check');
  const run = spawnSync(process.env.PYTHON || 'python3', command, {
    cwd: PROJECT_ROOT,
    env: { ...process.env, ORCHESTRATOR_PROJECT_ROOT: PROJECT_ROOT, ORCHESTRATOR_TASKS_DIR: TASKS_DIR },
    encoding: 'utf8', timeout: 60000
  });
  relayIndexObservations(run.stderr, check, !run.error && run.status === 0);
  if (run.error) fail('INDEX_PUBLICATION_FAILED', run.error.message, 4);
  if (run.status !== 0) fail('INDEX_PUBLICATION_FAILED', 'canonical index publisher failed with exit ' + String(run.status), run.status === 4 ? 4 : 1);
}

function captureSourceArtifacts(pre) {
  const group = pre && pre._model && pre._model.artifacts && pre._model.artifacts.get(pre.scope);
  if (!group) fail('TRANSITION_SOURCE_CHANGED', 'canonical source artifacts disappeared before ownership capture', 4);
  const proofs = Object.create(null);
  for (const column of core.COLUMNS) {
    const artifact = group[column];
    if (!artifact) continue;
    const expected = artifactPath(column, pre.scope);
    if (artifact.absolutePath !== expected) fail('TRANSITION_SOURCE_CHANGED', 'canonical source path changed before ownership capture', 4);
    let proof;
    if (core.HASH_RE.test(String(artifact.contentHash || ''))) {
      proof = readFileProof(expected);
      if (proof.hash !== artifact.contentHash) {
        fail('TRANSITION_SOURCE_CHANGED', 'canonical source bytes changed before ownership capture', 4);
      }
    } else if (artifact.inventoryTooLarge === true && artifact.stat) {
      const frozen = proofFromStat(expected, artifact.stat);
      proof = readProofLike(expected, frozen);
      if (!sameExactProof(proof, frozen)) {
        fail('TRANSITION_SOURCE_CHANGED', 'opaque task generation changed before ownership capture', 4);
      }
    } else {
      fail('TRANSITION_SOURCE_CHANGED', 'canonical source generation cannot be proven for deletion', 4);
    }
    proofs[column] = proof;
  }
  if (!Object.keys(proofs).length) fail('TRANSITION_SOURCE_CHANGED', 'transition source has no owned artifacts', 4);
  return proofs;
}

function assertDetachedBackup(marker, column) {
  const row = marker.detached[column], sourceProof = marker.sourceArtifacts[column];
  if (!row || !sourceProof) fail('TRANSITION_BACKUP_INVALID', 'detached source has no frozen ownership proof', 1);
  const expected = row.proof || sourceProof;
  const current = readProofLike(row.backup, expected);
  if (!(row.proof ? sameExactProof(current, row.proof) : sameFileGeneration(current, expected))) {
    fail('TRANSITION_BACKUP_CHANGED', 'private transition backup changed after detach', 1, { path: row.backup });
  }
  if (!row.proof) { row.proof = current; writeMarker(marker); }
  return current;
}

function assertSourceOwnership(marker, allowCleanedBackups = false) {
  for (const [column, proof] of Object.entries(marker.sourceArtifacts || {})) {
    const row = marker.detached[column];
    if (row && existsRegular(row.backup)) assertDetachedBackup(marker, column);
    else if (row && existsAny(row.backup)) fail('TRANSITION_BACKUP_CHANGED', 'private transition backup became unsafe', 1);
    else if (row && allowCleanedBackups) continue;
    else assertExactFileProof(proof);
  }
}

function publishTarget(marker, column, file, bytes) {
  ensureDir(path.dirname(file));
  ensureDir(privateDir(marker.transactionId));
  const publication = path.join(privateDir(marker.transactionId), 'target-' + column + path.extname(file));
  if (existsAny(publication)) fail('PRIVATE_BACKUP_CONFLICT', 'transition publication proof already exists', 1);
  marker.targetArtifact = { column, path: file, publication, publicationProof: null, targetProof: null };
  writeMarker(marker);
  writeExclusive(publication, bytes, 0o600);
  const beforeLink = readFileProof(publication);
  if (beforeLink.hash !== marker.intendedHash) fail('TARGET_PUBLICATION_INVALID', 'private target publication differs from the transition intent', 1);
  marker.targetArtifact.publicationProof = beforeLink;
  writeMarker(marker);
  try { boundary('link', { source: publication, target: file, expected: beforeLink, maxBytes: INPUT_MAX }); }
  catch (error) {
    if (error && error.code === 'EEXIST') fail('TARGET_CONFLICT', 'transition target already exists', 1);
    throw error;
  }
  fsyncDir(path.dirname(file));
  const published = readFileProof(file), retained = readFileProof(publication);
  if (!sameFileGeneration(published, retained) || published.hash !== marker.intendedHash) {
    fail('TARGET_PUBLICATION_INVALID', 'published target is not the retained transaction generation', 4);
  }
  marker.targetArtifact.publicationProof = retained;
  marker.targetArtifact.targetProof = published;
  marker.phase = 'target-published';
  writeMarker(marker);
}

function assertTargetOwned(marker) {
  const row = marker.targetArtifact;
  if (!row) return null;
  const target = readFileProof(row.path);
  if (target.hash !== marker.intendedHash) fail('RECOVERY_TARGET_CHANGED', 'published transition target no longer matches intended bytes', 1);
  let retained = null;
  if (existsRegular(row.publication)) {
    retained = readFileProof(row.publication);
    if (!row.publicationProof || !sameFileGeneration(retained, row.publicationProof) || !sameFileGeneration(target, retained)) {
      fail('RECOVERY_TARGET_CHANGED', 'published target is no longer the retained transaction generation', 1);
    }
  } else if (!row.targetProof || !sameFileGeneration(target, row.targetProof)) {
    fail('RECOVERY_TARGET_CHANGED', 'published target ownership proof is missing or changed', 1);
  }
  if (!row.targetProof || !sameFileGeneration(target, row.targetProof)) {
    row.targetProof = target;
    if (retained) row.publicationProof = retained;
    writeMarker(marker);
  }
  return target;
}

function removeOwnedTarget(marker) {
  const row = marker.targetArtifact;
  if (!row || !existsAny(row.path)) return;
  if (!existsRegular(row.publication)) fail('ROLLBACK_CONFLICT', 'private target ownership proof is missing', 1);
  const retained = readFileProof(row.publication);
  if (!row.publicationProof || !sameFileGeneration(retained, row.publicationProof)) {
    fail('ROLLBACK_CONFLICT', 'private target ownership proof changed', 1);
  }
  const quarantine = path.join(privateDir(marker.transactionId), 'rollback-target-' + row.column + path.extname(row.path));
  if (existsAny(quarantine)) fail('ROLLBACK_CONFLICT', 'rollback target quarantine already exists', 1);
  const targetBefore = readFileProof(row.path);
  boundary('move', { source: row.path, target: quarantine, expected: targetBefore, maxBytes: INPUT_MAX });
  const moved = readFileProof(quarantine), liveRetained = readFileProof(row.publication);
  if (!sameFileGeneration(moved, liveRetained)) {
    try {
      boundary('link', { source: quarantine, target: row.path, expected: moved, maxBytes: INPUT_MAX });
      boundary('remove', { path: quarantine, expected: moved, maxBytes: INPUT_MAX });
    }
    catch (_) { fail('ROLLBACK_CONFLICT', 'foreign target was quarantined and could not be restored no-clobber', 1); }
    fail('ROLLBACK_CONFLICT', 'target generation changed before rollback detach', 1);
  }
  boundary('remove', { path: quarantine, expected: moved, maxBytes: INPUT_MAX });
}

function maybeMutateFixtureBeforeDetach(source) {
  if (sourceDetachFixtureMutated || process.env.TASK_TRANSITION_TEST_MUTATE_BEFORE_DETACH !== '1' || TASKS_DIR === HERE) return;
  sourceDetachFixtureMutated = true;
  const current = safeRead(source);
  atomicReplace(source, Buffer.concat([current, Buffer.from('\nFixture mutation immediately before destructive detach.\n')]), 0o644);
}

function detach(marker, column, source) {
  ensureDir(privateDir(marker.transactionId));
  const backup = path.join(privateDir(marker.transactionId), column + path.extname(source));
  const sourceProof = marker.sourceArtifacts[column];
  if (!sourceProof || sourceProof.path !== source) fail('TRANSITION_SOURCE_CHANGED', 'source has no matching frozen artifact proof', 4);
  if (existsAny(backup)) fail('PRIVATE_BACKUP_CONFLICT', 'transition backup path already exists', 1);
  marker.detached[column] = { source, backup, proof: null };
  writeMarker(marker);
  maybeMutateFixtureBeforeDetach(source);
  assertExactFileProof(sourceProof);
  boundary('move', { source, target: backup, expected: sourceProof, maxBytes: INPUT_MAX });
  if (process.env.TASK_TRANSITION_TEST_CRASH_AFTER_DETACH === '1' && TASKS_DIR !== HERE) process.exit(86);
  const moved = readProofLike(backup, sourceProof);
  if (!sameFileGeneration(moved, sourceProof)) fail('TRANSITION_SOURCE_CHANGED', 'detached source is not the frozen generation', 4);
  marker.detached[column].proof = moved;
  writeMarker(marker);
}

function restoreDetached(marker) {
  const columns = Object.keys(marker.detached || {}).reverse();
  for (const column of columns) {
    const row = marker.detached[column], target = artifactPath(column, marker.stem);
    if (!existsAny(row.backup)) continue;
    const backup = assertDetachedBackup(marker, column);
    if (existsAny(target)) {
      const current = readProofLike(target, marker.sourceArtifacts[column]);
      if (!sameFileGeneration(current, marker.sourceArtifacts[column])) {
        fail('ROLLBACK_CONFLICT', 'cannot restore ' + column + ': destination now contains another generation', 1);
      }
      boundary('remove', { path: row.backup, expected: backup, maxBytes: INPUT_MAX });
      continue;
    }
    try { boundary('link', { source: row.backup, target, expected: backup, maxBytes: INPUT_MAX }); }
    catch (error) { if (error && error.code === 'EEXIST') fail('ROLLBACK_CONFLICT', 'cannot restore ' + column + ': destination appeared', 1); throw error; }
    const restored = readProofLike(target, backup), retained = readProofLike(row.backup, backup);
    if (!sameFileGeneration(restored, retained) || !sameFileGeneration(restored, backup)) {
      fail('ROLLBACK_CONFLICT', 'restored source does not match its private backup', 1);
    }
    const retainedAfter = readProofLike(row.backup, retained);
    boundary('remove', { path: row.backup, expected: retainedAfter, maxBytes: INPUT_MAX });
  }
}

function detachRuntime(marker, label, source) {
  const directory = inspectRealDirectory(path.dirname(source), label);
  if (!directory.exists) return;
  const entry = statEntry(source, true);
  if (entry.missing) return;
  if (entry.stat.kind !== 'file') fail('RUNTIME_ARTIFACT_UNSAFE', 'refusing unsafe runtime artifact: ' + path.basename(source), 1);
  let sourceProof;
  sourceProof = readFileProof(source);
  const after = inspectRealDirectory(path.dirname(source), label);
  if (!sameDirectoryIdentity(directory, after)) fail('RUNTIME_DIR_CHANGED', label + ' directory changed during the ownership check', 4);
  ensureInside(PROJECT_ROOT, source, 'runtime artifact');
  ensureDir(privateDir(marker.transactionId));
  const backup = path.join(privateDir(marker.transactionId), 'runtime-' + label + path.extname(source));
  if (existsAny(backup)) fail('PRIVATE_BACKUP_CONFLICT', 'runtime transition backup path already exists', 1);
  marker.runtimeDetached[label] = { source, backup, sourceProof, proof: null };
  writeMarker(marker);
  assertExactFileProof(sourceProof, 'RUNTIME_ARTIFACT_CHANGED');
  boundary('move', { source, target: backup, expected: sourceProof, maxBytes: INPUT_MAX });
  const moved = readFileProof(backup);
  if (!sameFileGeneration(moved, sourceProof)) fail('RUNTIME_ARTIFACT_CHANGED', 'runtime artifact changed at detach', 4);
  marker.runtimeDetached[label].proof = moved;
  writeMarker(marker);
}

function assertRuntimeBackup(marker, label) {
  const row = marker.runtimeDetached[label];
  const current = readFileProof(row.backup);
  const expected = row.proof || row.sourceProof;
  if (!(row.proof ? sameExactProof(current, row.proof) : sameFileGeneration(current, expected))) {
    fail('RUNTIME_BACKUP_CHANGED', 'private runtime backup changed after detach', 1);
  }
  if (!row.proof) { row.proof = current; writeMarker(marker); }
  return current;
}

function restoreRuntime(marker) {
  for (const label of Object.keys(marker.runtimeDetached || {}).reverse()) {
    const row = marker.runtimeDetached[label];
    if (!row || !existsAny(row.backup)) continue;
    const backup = assertRuntimeBackup(marker, label);
    if (existsAny(row.source)) fail('ROLLBACK_CONFLICT', 'cannot restore runtime ' + label + ': destination now exists', 1);
    ensureDir(path.dirname(row.source));
    try { boundary('link', { source: row.backup, target: row.source, expected: backup, maxBytes: INPUT_MAX }); }
    catch (error) { if (error && error.code === 'EEXIST') fail('ROLLBACK_CONFLICT', 'runtime destination appeared during restore', 1); throw error; }
    const restored = readFileProof(row.source), retained = readFileProof(row.backup);
    if (!sameFileGeneration(restored, retained) || !sameFileGeneration(restored, backup)) fail('ROLLBACK_CONFLICT', 'runtime restore generation mismatch', 1);
    const retainedAfter = readFileProof(row.backup);
    boundary('remove', { path: row.backup, expected: retainedAfter, maxBytes: INPUT_MAX });
  }
}

function runtimePaths(marker, label) {
  const source = label === 'lock' ? path.join(LOCKS_DIR, marker.stem + '.json')
    : label === 'journal' ? path.join(JOURNAL_DIR, marker.stem + '.jsonl') : null;
  if (!source) return null;
  return { source, backup: path.join(privateDir(marker.transactionId), 'runtime-' + label + path.extname(source)) };
}

function assertHistoryOwned(marker) {
  if (!marker.history) return;
  const current = assertExactFileProof(marker.history.proof, 'REOPEN_HISTORY_CHANGED', 1);
  if (current.hash !== marker.sourceArtifacts.done.hash) {
    fail('REOPEN_HISTORY_CHANGED', 'reopen history no longer contains the frozen done generation', 1);
  }
}

function cleanupPrivateArtifacts(marker) {
  const directory = privateDir(marker.transactionId);
  const inspected = inspectRealDirectory(directory, 'transition private');
  if (!inspected.exists) {
    if (marker.phase === 'complete' && marker.targetArtifact) assertTargetOwned(marker);
    return;
  }
  const expected = new Map();
  for (const [column, row] of Object.entries(marker.detached || {})) expected.set(path.basename(row.backup), { kind: 'source', column, row });
  for (const [label, row] of Object.entries(marker.runtimeDetached || {})) expected.set(path.basename(row.backup), { kind: 'runtime', label, row });
  if (marker.targetArtifact) expected.set(path.basename(marker.targetArtifact.publication), { kind: 'publication', row: marker.targetArtifact });
  const names = boundedDirectoryNames(directory, 'transition private') || [];
  const extra = names.filter((name) => !expected.has(name));
  if (extra.length) fail('TRANSITION_CLEANUP_REQUIRED', 'private transition directory contains unowned entries', 4, { entries: extra.slice(0, 20) });
  if (marker.phase === 'complete' && marker.targetArtifact) assertTargetOwned(marker);
  for (const name of names) {
    const owned = expected.get(name), file = path.join(directory, name);
    if (owned.kind === 'source') assertDetachedBackup(marker, owned.column);
    else if (owned.kind === 'runtime') assertRuntimeBackup(marker, owned.label);
    else {
      const current = readFileProof(file);
      if (!owned.row.publicationProof || !sameFileGeneration(current, owned.row.publicationProof)) {
        fail('TRANSITION_CLEANUP_REQUIRED', 'private target publication proof changed', 4);
      }
    }
  }
  for (const name of names) {
    const owned = expected.get(name), file = path.join(directory, name);
    let proof;
    if (owned.kind === 'source') {
      const row = marker.detached[owned.column];
      proof = readProofLike(file, row.proof || marker.sourceArtifacts[owned.column]);
    } else if (owned.kind === 'runtime') {
      const row = marker.runtimeDetached[owned.label];
      proof = readProofLike(file, row.proof || row.sourceProof);
    } else {
      proof = readFileProof(file);
    }
    boundary('remove', { path: file, expected: proof, maxBytes: INPUT_MAX });
  }
  try {
    // File removal advances the directory metadata. Freeze the exact empty
    // generation after those removals so a whole-directory replacement cannot
    // be mistaken for the transition-owned cleanup target.
    const empty = boundary('list', { path: directory, maxEntries: MAX_RUNTIME_ENTRIES });
    if (empty.names.length) fail('TRANSITION_CLEANUP_REQUIRED', 'private transition directory changed during cleanup', 4);
    boundary('remove-empty-dir', { path: directory, expected: empty.stat });
  }
  catch (error) { fail('TRANSITION_CLEANUP_REQUIRED', 'private transition directory could not be removed exactly: ' + String(error.message || error).slice(0, 300), 4); }
}

function requireRecoveryMarker(marker, fileName) {
  const id = fileName.slice(0, -'.json'.length);
  const sourceStates = {
    ask: ['backlog', 'pending'],
    promote: ['backlog', 'pending'],
    edit: ['backlog', 'todo'],
    drop: ['backlog', 'pending', 'todo', 'done', 'corrupt'],
    reopen: ['done']
  };
  const requiredFields = ['authorityStem', 'createdAt', 'detached', 'history', 'intendedHash', 'operation', 'phase',
    'runtimeDetached', 'sourceArtifacts', 'sourceRevision', 'sourceState', 'stem', 'targetArtifact', 'taskLock',
    'transactionId', 'updatedAt', 'version'];
  const optionalFields = new Set(['errorCode', 'errorMessage']);
  const keys = marker && typeof marker === 'object' && !Array.isArray(marker) ? Object.keys(marker).sort() : [];
  const fieldsValid = requiredFields.every((field) => Object.prototype.hasOwnProperty.call(marker || {}, field)) &&
    keys.every((field) => requiredFields.includes(field) || optionalFields.has(field));
  if (!marker || typeof marker !== 'object' || Array.isArray(marker) || !fieldsValid || marker.version !== VERSION ||
      marker.transactionId !== id || !/^tr-[a-f0-9]{36}$/.test(id) || core.safeIntegerId(marker.stem) === null ||
      core.safeIntegerId(marker.authorityStem) === null ||
      !Object.prototype.hasOwnProperty.call(sourceStates, marker.operation) || !sourceStates[marker.operation].includes(marker.sourceState) ||
      !core.HASH_RE.test(String(marker.sourceRevision || '')) ||
      (marker.operation === 'drop' ? marker.intendedHash !== null : !core.HASH_RE.test(String(marker.intendedHash || ''))) ||
      typeof marker.createdAt !== 'string' || !Number.isFinite(Date.parse(marker.createdAt)) ||
      typeof marker.updatedAt !== 'string' || !Number.isFinite(Date.parse(marker.updatedAt)) ||
      Date.parse(marker.updatedAt) < Date.parse(marker.createdAt) ||
      !['prepared', 'target-published', 'filesystem-valid', 'complete', 'recovery-required'].includes(marker.phase) ||
      !marker.detached || typeof marker.detached !== 'object' || Array.isArray(marker.detached) ||
      !marker.runtimeDetached || typeof marker.runtimeDetached !== 'object' || Array.isArray(marker.runtimeDetached) ||
      !marker.sourceArtifacts || typeof marker.sourceArtifacts !== 'object' || Array.isArray(marker.sourceArtifacts) ||
      !(marker.taskLock === null || taskLockProofShape(marker.taskLock, marker.stem)) ||
      (marker.errorCode !== undefined && (marker.phase !== 'recovery-required' || typeof marker.errorCode !== 'string' || !marker.errorCode || marker.errorCode.length > 100)) ||
      (marker.errorMessage !== undefined && (marker.phase !== 'recovery-required' || typeof marker.errorMessage !== 'string' || marker.errorMessage.length > 500))) {
    fail('TRANSITION_MARKER_INVALID', 'transition marker does not match the recovery schema', 1);
  }
  if (marker.authorityStem !== marker.stem && (marker.operation !== 'promote' || marker.sourceState !== 'backlog')) {
    fail('TRANSITION_MARKER_INVALID', 'delegated authority is valid only for backlog child promotion', 1);
  }
  const sourceColumns = Object.keys(marker.sourceArtifacts).sort();
  const expectedSourceColumns = marker.sourceState === 'pending' ? ['backlog', 'pending'] : [marker.sourceState];
  const corruptSourceSetValid = marker.sourceState === 'corrupt' && sourceColumns.length > 0 &&
    sourceColumns.every((column) => core.COLUMNS.includes(column)) &&
    core.classify(Object.fromEntries(sourceColumns.map((column) => [column, true]))) === 'corrupt';
  if (!corruptSourceSetValid &&
      (sourceColumns.length !== expectedSourceColumns.length ||
        !sourceColumns.every((column, index) => column === expectedSourceColumns[index]))) {
    fail('TRANSITION_MARKER_INVALID', 'transition marker source-artifact set does not match its source state', 1);
  }
  for (const column of sourceColumns) {
    const proof = marker.sourceArtifacts[column];
    const statOnlyOversizedDrop = marker.operation === 'drop' && proof && proof.hash === null && proof.size > INPUT_MAX;
    if (!fileProofShape(proof, artifactPath(column, marker.stem)) ||
        !(hashedFileProofShape(proof) || statOnlyOversizedDrop)) {
      fail('TRANSITION_MARKER_INVALID', 'transition marker contains an invalid source-artifact proof', 1);
    }
  }
  const allowedDetached = marker.operation === 'ask' ? ['pending']
    : marker.operation === 'promote' ? ['backlog', 'pending']
      : marker.operation === 'edit' ? [marker.sourceState]
      : marker.operation === 'drop' ? ['backlog', 'pending', 'todo', 'done']
        : ['done'];
  for (const column of Object.keys(marker.detached)) {
    const row = marker.detached[column];
    const source = artifactPath(column, marker.stem), backup = path.join(privateDir(id), column + '.md');
    const detachedProofValid = row && (row.proof === null ||
      (fileProofShape(row.proof, backup) && sameFileGeneration(row.proof, marker.sourceArtifacts[column])));
    if (!allowedDetached.includes(column) || !marker.sourceArtifacts[column] || !row || typeof row !== 'object' || Array.isArray(row) ||
        Object.keys(row).sort().join(',') !== 'backup,proof,source' || row.source !== source || row.backup !== backup ||
        !detachedProofValid) {
      fail('TRANSITION_MARKER_INVALID', 'transition marker contains an unsafe detached path', 1);
    }
  }
  const runtimeLabels = Object.keys(marker.runtimeDetached);
  if (marker.operation !== 'drop' && runtimeLabels.length) fail('TRANSITION_MARKER_INVALID', 'only drop may detach runtime task metadata', 1);
  for (const label of runtimeLabels) {
    const row = marker.runtimeDetached[label], expected = runtimePaths(marker, label);
    const runtimeProofValid = row && (row.proof === null ||
      (hashedFileProofShape(row.proof, expected && expected.backup) && sameFileGeneration(row.proof, row.sourceProof)));
    if (!expected || !row || typeof row !== 'object' || Array.isArray(row) ||
        Object.keys(row).sort().join(',') !== 'backup,proof,source,sourceProof' || row.source !== expected.source || row.backup !== expected.backup ||
        !hashedFileProofShape(row.sourceProof, expected.source) || !runtimeProofValid) {
      fail('TRANSITION_MARKER_INVALID', 'transition marker contains an unsafe runtime-detached path', 1);
    }
  }
  const targetColumn = marker.operation === 'ask' ? 'pending'
    : marker.operation === 'edit' ? marker.sourceState
      : marker.operation === 'drop' ? null : 'todo';
  if (marker.targetArtifact !== null) {
    const row = marker.targetArtifact;
    const target = targetColumn && artifactPath(targetColumn, marker.stem);
    const publication = targetColumn && path.join(privateDir(id), 'target-' + targetColumn + path.extname(target));
    const publicationProofValid = row && (row.publicationProof === null ||
      (hashedFileProofShape(row.publicationProof, publication) && row.publicationProof.hash === marker.intendedHash));
    const targetProofValid = row && (row.targetProof === null ||
      (hashedFileProofShape(row.targetProof, target) && row.targetProof.hash === marker.intendedHash &&
        (row.publicationProof === null || sameFileGeneration(row.targetProof, row.publicationProof))));
    if (!targetColumn || !row || typeof row !== 'object' || Array.isArray(row) ||
        Object.keys(row).sort().join(',') !== 'column,path,publication,publicationProof,targetProof' ||
        row.column !== targetColumn || row.path !== target || row.publication !== publication ||
        !publicationProofValid || !targetProofValid) {
      fail('TRANSITION_MARKER_INVALID', 'transition marker contains an invalid target ownership proof', 1);
    }
  }
  if (['target-published', 'filesystem-valid', 'complete'].includes(marker.phase) && targetColumn && marker.targetArtifact === null) {
    fail('TRANSITION_MARKER_INVALID', 'published transition marker is missing its target ownership proof', 1);
  }
  if (marker.history !== null) {
    const expectedHash = marker.sourceArtifacts.done && marker.sourceArtifacts.done.hash.slice('sha256:'.length);
    const historyPath = expectedHash && path.join(REOPEN_EVIDENCE_DIR, marker.stem, expectedHash + '.md');
    if (marker.operation !== 'reopen' || !historyPath || !marker.history || typeof marker.history !== 'object' || Array.isArray(marker.history) ||
        Object.keys(marker.history).sort().join(',') !== 'path,proof' || marker.history.path !== historyPath ||
        !hashedFileProofShape(marker.history.proof, historyPath) || marker.history.proof.hash !== marker.sourceArtifacts.done.hash) {
      fail('TRANSITION_MARKER_INVALID', 'transition marker contains an unsafe reopen-history proof', 1);
    }
  }
  return marker;
}

function activeTransitionMarkers(stem) {
  ensureDir(STATE_DIR);
  reconcileMarkerReplacements();
  const listed = boundedDirectoryNames(STATE_DIR, 'transition') || [];
  const malformed = listed.find((name) => name.startsWith('tr-') && !/^tr-[a-f0-9]{36}\.json$/.test(name));
  if (malformed) fail('TRANSITION_MARKER_NAME_INVALID', 'malformed transition marker/recovery prefix blocks mutation: ' + malformed, 1);
  const names = listed.filter((name) => /^tr-[a-f0-9]{36}\.json$/.test(name));
  const matches = [];
  for (const name of names) {
    const bytes = safeRead(path.join(STATE_DIR, name), MARKER_MAX);
    let marker;
    try { marker = JSON.parse(bytes.toString('utf8')); }
    catch (_) { fail('TRANSITION_MARKER_INVALID', 'transition marker is not JSON', 1); }
    marker = requireRecoveryMarker(marker, name);
    if (marker.stem === stem) matches.push(marker);
  }
  return matches;
}

function requireNoActiveTransition(stem) {
  const active = activeTransitionMarkers(stem);
  if (active.length) {
    fail('TRANSITION_RECOVERY_REQUIRED', 'an earlier task transition must be recovered before starting another operation', 1, {
      transactionIds: active.map((marker) => marker.transactionId).sort()
    });
  }
}

function newMarker(operation, stem, pre, intendedHash, authority, taskLock = null) {
  const marker = {
    version: VERSION,
    transactionId: randomId(),
    operation,
    stem,
    authorityStem: authority.authorityStem,
    taskLock,
    sourceState: pre.observedState,
    sourceRevision: pre.sourceRevision,
    sourceArtifacts: captureSourceArtifacts(pre),
    intendedHash: intendedHash || null,
    createdAt: now(),
    updatedAt: now(),
    phase: 'prepared',
    detached: Object.create(null),
    runtimeDetached: Object.create(null),
    targetArtifact: null,
    history: null,
  };
  writeMarker(marker, true);
  return marker;
}

function filesystemPost(stem, transition) {
  return requireVerdict(validate({ stem, transition, phase: 'post', checkIndex: false }), 'TRANSITION_POSTCONDITION_FAILED');
}
function dropPost(stem, transition, checkIndex) {
  const result = validate({ stem, transition, phase: 'post', checkIndex });
  const admission = core.dropAdmission(result, stem, { allowAbsent: true });
  if (!admission.ok) fail('TRANSITION_POSTCONDITION_FAILED', 'canonical drop postcondition failed', 1, {
    result, blockers: admission.blockers
  });
  return result;
}
function finalPost(stem, transition) {
  return requireVerdict(validate({ stem, transition, phase: 'post', checkIndex: true }), 'TRANSITION_INDEX_NOT_FRESH');
}
function publishIndexOrDefer(withoutIndex, withIndex) {
  try {
    runIndex(false);
    return { result: withIndex(), indexDeferred: false };
  } catch (_) {
    // INDEX is a derived projection. If another task is temporarily malformed,
    // preserve the proven task-scoped mutation and let the diagnostics/status
    // path report that regeneration is still pending.
    return { result: withoutIndex(), indexDeferred: true };
  }
}
function sameStatePost(stem, state, checkIndex) {
  return requireVerdict(validate({ stem, expect: state, checkIndex }), checkIndex ? 'TRANSITION_INDEX_NOT_FRESH' : 'TRANSITION_POSTCONDITION_FAILED');
}

function maybeMutateFixtureSource(pre) {
  // Deterministic race injection for the isolated fixture corpus only.  It is
  // impossible to enable against this repository's checked-in task root.
  if (process.env.TASK_TRANSITION_TEST_MUTATE_BEFORE_RECONFIRM !== '1' || TASKS_DIR === HERE) return;
  const column = pre.observedState === 'pending' ? 'backlog' : pre.observedState;
  const file = artifactPath(column, pre.scope);
  const current = safeRead(file);
  atomicReplace(file, Buffer.concat([current, Buffer.from('\nFixture mutation between pre-check and mutation.\n')]), 0o644);
}

function maybePauseFixture(stage) {
  const wanted = String(process.env.TASK_TRANSITION_TEST_PAUSE_STAGE || '');
  const sentinelRaw = String(process.env.TASK_TRANSITION_TEST_PAUSE_SENTINEL || '');
  const delayRaw = String(process.env.TASK_TRANSITION_TEST_PAUSE_MS || '');
  if (!wanted && !sentinelRaw && !delayRaw) return;
  if (!wanted || !sentinelRaw || !/^[1-9][0-9]{0,3}$/.test(delayRaw) || Number(delayRaw) > 5000) {
    fail('TEST_HOOK_INVALID', 'transition pause hook is incomplete or exceeds its bound', 2);
  }
  if (wanted !== stage) return;
  if (TASKS_DIR === HERE) fail('TEST_HOOK_INVALID', 'transition pause hook is disabled for the canonical task root', 2);
  const sentinel = path.resolve(sentinelRaw);
  ensureInside(PROJECT_ROOT, sentinel, 'transition pause sentinel');
  try { writeExclusive(sentinel, Buffer.from(stage + '\n')); }
  catch (error) {
    if (!error || error.code !== 'EEXIST') throw error;
  }
  Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, Number(delayRaw));
}

function reconfirmAction(pre, action, marker) {
  maybeMutateFixtureSource(pre);
  maybeMutateFixtureTaskLock(marker.taskLock);
  assertTaskLockOwned(marker.taskLock);
  const current = validateAction({ stem: pre.scope, action, checkIndex: true });
  if (current.observedState !== pre.observedState || current.sourceRevision !== pre.sourceRevision) {
    fail('TRANSITION_SOURCE_CHANGED', 'task source changed immediately before mutation', 4, {
      expected: pre.sourceRevision, observed: current.sourceRevision, result: current
    });
  }
  const admission = core.actionAdmission(current);
  if (!admission.ok) fail('TRANSITION_PRECONDITION_FAILED', 'canonical action admission failed', 1, {
    result: current, blockers: admission.blockers
  });
  assertSourceOwnership(marker);
  assertTaskLockOwned(marker.taskLock);
  return current;
}

function reconfirmState(pre, allowedStates, marker) {
  maybeMutateFixtureSource(pre);
  maybeMutateFixtureTaskLock(marker.taskLock);
  assertTaskLockOwned(marker.taskLock);
  const current = validate({ stem: pre.scope, checkIndex: true });
  if (!allowedStates.includes(current.observedState) || current.observedState !== pre.observedState || current.sourceRevision !== pre.sourceRevision) {
    fail('TRANSITION_SOURCE_CHANGED', 'task source changed immediately before mutation', 4, {
      expected: pre.sourceRevision, observed: current.sourceRevision, result: current
    });
  }
  const admission = core.actionAdmission(current);
  if (!admission.ok) fail('TRANSITION_PRECONDITION_FAILED', 'canonical action admission failed', 1, {
    result: current, blockers: admission.blockers
  });
  assertSourceOwnership(marker);
  assertTaskLockOwned(marker.taskLock);
  return current;
}

function sourceAlreadyRestored(marker) {
  try {
    const current = validate({ stem: marker.stem, checkIndex: false });
    const admitted = marker.operation === 'drop'
      ? core.dropAdmission(current, marker.stem).ok : current.ok;
    return admitted && current.observedState === marker.sourceState &&
      current.sourceRevision === marker.sourceRevision;
  } catch (_) { return false; }
}

function rollback(marker, targetColumn) {
  try {
    const detachedMutation = Object.values(marker.detached || {}).some((row) => row && existsAny(row.backup));
    const runtimeMutation = Object.values(marker.runtimeDetached || {}).some((row) => row && existsAny(row.backup));
    const targetMutation = marker.targetArtifact && existsAny(marker.targetArtifact.path);
    // Admission can fail because an external writer changed the source before
    // this transaction touched any durable task/runtime path. Preserve that
    // newer generation, remove only our private intent, and surface the
    // original retryable race instead of pretending rollback owns the source.
    if (!detachedMutation && !runtimeMutation && !targetMutation) {
      cleanupMarker(marker, false);
      return;
    }
    if (!sourceAlreadyRestored(marker)) removeOwnedTarget(marker);
    restoreDetached(marker);
    restoreRuntime(marker);
    let indexPublished = true;
    try { runIndex(false); }
    catch (_) { indexPublished = false; }
    const restored = validate({ stem: marker.stem, checkIndex: indexPublished });
    const restoredOk = marker.operation === 'drop'
      ? core.dropAdmission(restored, marker.stem).ok : restored.ok;
    if (!restoredOk || restored.observedState !== marker.sourceState || restored.sourceRevision !== marker.sourceRevision) {
      fail('ROLLBACK_SOURCE_MISMATCH', 'rollback did not restore the exact frozen source revision', 1, { result: restored });
    }
    cleanupMarker(marker, false);
  } catch (error) {
    marker.phase = 'recovery-required'; marker.errorCode = error.code || 'ROLLBACK_FAILED'; marker.errorMessage = String(error.message || error).slice(0, 500);
    try { writeMarker(marker); } catch (_) {}
    throw error;
  }
}

function abortOperation(marker, targetColumn, error) {
  // Once `complete` is durably recorded, the filesystem and INDEX have both
  // passed their postconditions. Rolling that state back because a later
  // cleanup/read failed can destroy the committed target after backups were
  // already removed. Leave the marker as the idempotent recovery receipt.
  if (marker.phase === 'complete') {
    fail('TRANSITION_COMMITTED_RECOVERY_REQUIRED', 'transition committed, but post-commit cleanup did not finish; run canonical recovery', 4, {
      causeCode: error && error.code || 'POST_COMMIT_FAILURE'
    });
  }
  rollback(marker, targetColumn);
  throw error;
}

function exactTaskInstant(value) {
  if (typeof value !== 'string') return false;
  const match = /^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.(\d{1,3}))?Z$/.exec(value);
  if (!match || !Number.isFinite(Date.parse(value))) return false;
  const canonical = match[1] + '.' + String(match[2] || '').padEnd(3, '0') + 'Z';
  return new Date(value).toISOString() === canonical;
}

function pendingCounter(value, minimum = 0) {
  if (typeof value !== 'string' || !/^[0-9]+$/.test(value) || value.length > 16) return null;
  const parsed = BigInt(value);
  return parsed >= BigInt(minimum) && parsed <= BigInt(Number.MAX_SAFE_INTEGER) ? Number(parsed) : null;
}

function pendingShapeIssue(stem, parsed) {
  if (parsed.errors.length || parsed.duplicateFields.length) return 'pending sidecar syntax is not canonical';
  const fields = parsed.fields;
  if (fields.forTask !== stem) return 'pending forTask does not match the task stem';
  const round = pendingCounter(fields.round, 1), gapCount = pendingCounter(fields.gapCount, 0);
  if (!exactTaskInstant(fields.createdAt) || !exactTaskInstant(fields.updatedAt) ||
      Date.parse(fields.updatedAt) < Date.parse(fields.createdAt) || round === null || gapCount === null ||
      gapCount < parsed.questions.length ||
      (fields.prevGapCount !== undefined && pendingCounter(fields.prevGapCount, 0) === null)) {
    return 'pending timestamps or convergence counters are invalid';
  }
  if (!parsed.questions.length) return 'pending sidecar contains no questions';
  const ids = new Set(); let previous = 0;
  for (const question of parsed.questions) {
    if (!Number.isSafeInteger(question.id) || question.id < 1 || ids.has(question.id) || question.id <= previous) {
      return 'pending question ids must be positive, unique, and ordered';
    }
    ids.add(question.id); previous = question.id;
    if (question.types.length !== 1 || !['text', 'choice', 'multiselect'].includes(question.types[0]) || question.answerCount !== 1) {
      return 'pending question type or Answer structure is invalid';
    }
    if (question.types[0] === 'choice' || question.types[0] === 'multiselect') {
      const options = question.options.length === 1 ? question.options[0].split(',').map((item) => item.trim()).filter(Boolean) : [];
      const keys = options.map((item) => item.normalize('NFC').toLowerCase());
      if (options.length < 2 || new Set(keys).size !== keys.length) return 'pending choice options are invalid';
    } else if (question.options.length) return 'pending text question must not declare options';
  }
  return null;
}

function rawLineRecords(text) {
  const lines = [];
  const breaks = /\r\n|\r|\n/g;
  let start = 0; let match;
  while ((match = breaks.exec(text)) !== null) {
    lines.push({ start, contentEnd: match.index, end: breaks.lastIndex });
    start = breaks.lastIndex;
  }
  lines.push({ start, contentEnd: text.length, end: text.length });
  return lines;
}

function pendingQuestionIdentity(parsed) {
  return parsed.questions.map((question) => ({
    id: question.id,
    title: question.title,
    types: question.types.slice(),
    options: question.options.slice(),
    answerCount: question.answerCount,
  }));
}

function pendingAnswerProjection(text) {
  const structuralLines = core.structuralText(text).split('\n');
  const rawLines = rawLineRecords(text);
  if (structuralLines.length !== rawLines.length) return null;
  const questionLines = [];
  for (let index = 0; index < structuralLines.length; index++) {
    if (/^##[ \t]+Q[0-9]+[ \t]+[—-][ \t]+.+?[ \t]*$/.test(structuralLines[index])) questionLines.push(index);
  }
  if (!questionLines.length) return null;
  const skeleton = []; const answers = []; let cursor = 0;
  for (let index = 0; index < questionLines.length; index++) {
    const questionLine = questionLines[index];
    const nextQuestionLine = index + 1 < questionLines.length ? questionLines[index + 1] : structuralLines.length;
    const answerLines = [];
    for (let line = questionLine + 1; line < nextQuestionLine; line++) {
      if (/^###[ \t]+Answer[ \t]*$/.test(structuralLines[line])) answerLines.push(line);
    }
    if (answerLines.length !== 1) return null;
    // Keep the exact raw heading bytes immutable. Its line ending and all
    // bytes until the next real CommonMark question belong to the answer body.
    // Raw fake headings inside a fenced/HTML answer therefore remain answer
    // content, while an opener that hides a later real question changes the
    // structural identity and is rejected.
    const answerStart = rawLines[answerLines[0]].contentEnd;
    const answerEnd = nextQuestionLine < rawLines.length ? rawLines[nextQuestionLine].start : text.length;
    skeleton.push(text.slice(cursor, answerStart), '\u0000ANSWER-BODY\u0000');
    answers.push(text.slice(answerStart, answerEnd));
    cursor = answerEnd;
  }
  skeleton.push(text.slice(cursor));
  return { skeleton: skeleton.join(''), answers };
}

function validatePendingWriteContract(pre, bytes, intent) {
  const stem = pre.scope, proposedText = bytes.toString('utf8');
  const proposed = core.parsePending(proposedText);
  const shapeIssue = pendingShapeIssue(stem, proposed);
  if (shapeIssue) fail('PENDING_WRITE_CONTRACT_INVALID', shapeIssue, 1);
  const nextFields = proposed.fields;
  if (pre.observedState === 'backlog') {
    if (intent !== 'ask' || pendingCounter(nextFields.round, 1) !== 1 || nextFields.prevGapCount !== undefined) {
      fail('PENDING_WRITE_CONTRACT_INVALID', 'initial backlog questions must start at round 1 without prevGapCount', 1);
    }
    return;
  }
  if (pre.observedState !== 'pending') fail('PENDING_WRITE_CONTRACT_INVALID', 'pending write requires backlog or pending state', 1);
  const group = pre._model && pre._model.artifacts && pre._model.artifacts.get(stem);
  const currentText = group && group.pending && group.pending.text;
  const current = typeof currentText === 'string' ? core.parsePending(currentText) : null;
  if (!current || pendingShapeIssue(stem, current)) fail('PENDING_WRITE_CONTRACT_INVALID', 'current pending generation is not canonical', 1);
  const oldFields = current.fields;
  if (intent === 'persist-answers') {
    const before = pendingAnswerProjection(currentText), after = pendingAnswerProjection(proposedText);
    if (core.canonicalJson(pendingQuestionIdentity(current)) !== core.canonicalJson(pendingQuestionIdentity(proposed)) ||
        !before || !after || before.skeleton !== after.skeleton || before.answers.length !== after.answers.length) {
      fail('PENDING_ANSWER_PERSISTENCE_INVALID', 'answer persistence may change only existing Answer bodies', 1);
    }
    if (!before.answers.some((answer, index) => answer !== after.answers[index])) {
      fail('PENDING_ANSWER_PERSISTENCE_INVALID', 'answer persistence contains no answer-body change', 1);
    }
  } else {
    const oldRound = pendingCounter(oldFields.round, 1), oldGapCount = pendingCounter(oldFields.gapCount, 0);
    const nextRound = pendingCounter(nextFields.round, 1), nextPrevGapCount = pendingCounter(nextFields.prevGapCount, 0);
    if (oldRound === null || oldGapCount === null || nextRound !== oldRound + 1 ||
        nextFields.createdAt !== oldFields.createdAt || nextPrevGapCount !== oldGapCount ||
        Date.parse(nextFields.updatedAt) < Date.parse(oldFields.updatedAt)) {
      fail('PENDING_ROUND_GENERATION_INVALID',
        'ask-again must preserve createdAt, advance exactly one round, and shift the previous gapCount', 1);
    }
  }
  const proposal = validate({
    stem, transition: 'pending:pending', phase: 'post', checkIndex: false,
    proposal: { stem, fromState: 'pending', state: 'pending', bytes },
  });
  requireVerdict(proposal, 'PENDING_WRITE_CONTRACT_INVALID');
}

function stripTrailingNewlines(value) {
  return String(value).replace(/\n+$/, '');
}

// In-body questions for a task that is already running. The section is the
// only writable region: `publish-questions` may append new blocks and nothing
// else, `persist-task-answers` may rewrite Answer bodies and nothing else.
// Everything outside the section — Source provenance included — is proven
// byte-identical here before any durable mutation, except the newlines that
// separate a first section from the body it is appended to.
function validateTaskQuestionsWriteContract(pre, bytes, intent) {
  const stem = pre.scope, proposedText = bytes.toString('utf8');
  const group = pre._model && pre._model.artifacts && pre._model.artifacts.get(stem);
  const currentText = group && group.todo && group.todo.text;
  if (typeof currentText !== 'string') {
    fail('TASK_QUESTIONS_WRITE_INVALID', 'current todo generation is not readable', 1);
  }
  const proposed = core.parseTaskQuestions(proposedText);
  const proposedIssue = core.taskQuestionsIssue(proposed);
  if (proposedIssue) fail('TASK_QUESTIONS_WRITE_INVALID', proposedIssue, 1);
  if (core.taskBodyStructureOpen(proposedText)) {
    fail('TASK_QUESTIONS_WRITE_INVALID', 'proposal leaves a fence, HTML block, or comment open at the end of the task body', 1);
  }
  const current = core.parseTaskQuestions(currentText);
  if (current.sectionCount > 1 || current.errors.length) {
    fail('TASK_QUESTIONS_WRITE_INVALID', 'current Questions section is not canonical', 1);
  }

  if (intent === 'publish-questions') {
    // Everything the current body already holds is immutable: the bytes before
    // and after the section, the section preamble, and every existing block
    // including its recorded answer. Only bytes after the last existing block
    // are new. A prefix comparison is not enough — it leaves the last answer
    // body (and the option bullets an owner reads) rewritable.
    const hadSection = current.present;
    if (hadSection) {
      const currentIssue = core.taskQuestionsIssue(current);
      if (currentIssue) fail('TASK_QUESTIONS_WRITE_INVALID', 'current Questions section is not canonical: ' + currentIssue, 1);
    }
    const beforeOuter = hadSection
      ? { prefix: currentText.slice(0, current.sectionStart), suffix: currentText.slice(current.sectionEnd) }
      : { prefix: currentText, suffix: '' };
    const afterOuter = {
      prefix: proposedText.slice(0, proposed.sectionStart),
      suffix: proposedText.slice(proposed.sectionEnd)
    };
    const sameOuter = hadSection
      ? beforeOuter.prefix === afterOuter.prefix && beforeOuter.suffix === afterOuter.suffix
      // A first section is appended at the end of the body, so the one byte
      // difference the contract absorbs is the blank line before it.
      : afterOuter.suffix === '' && stripTrailingNewlines(beforeOuter.prefix) === stripTrailingNewlines(afterOuter.prefix);
    if (!sameOuter) {
      fail('TASK_QUESTIONS_WRITE_INVALID', 'publishing questions may not change the task body outside its Questions section', 1);
    }
    if (proposed.questions.length <= current.questions.length) {
      fail('TASK_QUESTIONS_WRITE_INVALID', 'publishing questions must add at least one new question block', 1);
    }
    if (hadSection) {
      const beforePreamble = currentText.slice(current.sectionStart, current.questions[0].blockStart);
      const afterPreamble = proposedText.slice(proposed.sectionStart, proposed.questions[0].blockStart);
      if (beforePreamble !== afterPreamble) {
        fail('TASK_QUESTIONS_WRITE_INVALID', 'publishing questions may not change the Questions section preamble', 1);
      }
      for (let index = 0; index < current.questions.length; index++) {
        const carried = current.questions[index], candidate = proposed.questions[index];
        const before = currentText.slice(carried.blockStart, carried.blockEnd);
        const after = proposedText.slice(candidate.blockStart, candidate.blockEnd);
        // Appending a block after the last one necessarily grows that block's
        // trailing blank line. Newlines are the only byte the separator may
        // add; everything else — prose, option bullets, answer text — is
        // frozen, which is what keeps a recorded decision unrewritable.
        const separatorOnly = index === current.questions.length - 1 &&
          stripTrailingNewlines(before) === stripTrailingNewlines(after);
        if (before !== after && !separatorOnly) {
          fail('TASK_QUESTIONS_WRITE_INVALID', 'publishing questions may not rewrite an existing question block', 1);
        }
      }
    }
    const highest = current.questions.reduce((value, question) => Math.max(value, question.id), 0);
    const published = proposed.questions.slice(current.questions.length);
    if (published.some((question) => question.id <= highest)) {
      fail('TASK_QUESTIONS_WRITE_INVALID', 'new question ids must be greater than every existing question id', 1);
    }
    // A question the owner never saw cannot arrive pre-answered: that would
    // clear the board's unanswered count and skip the escalation entirely.
    if (published.some((question) => question.answer !== '')) {
      fail('TASK_QUESTIONS_WRITE_INVALID', 'a newly published question must carry an empty Answer', 1);
    }
  } else {
    const currentIssue = core.taskQuestionsIssue(current);
    if (currentIssue) fail('TASK_QUESTIONS_WRITE_INVALID', 'current Questions section is not canonical: ' + currentIssue, 1);
    const before = core.taskQuestionsProjection(currentText), after = core.taskQuestionsProjection(proposedText);
    if (core.canonicalJson(core.taskQuestionsIdentity(current)) !== core.canonicalJson(core.taskQuestionsIdentity(proposed)) ||
        !before || !after || before.skeleton !== after.skeleton || before.answers.length !== after.answers.length) {
      fail('TASK_ANSWER_PERSISTENCE_INVALID', 'answer persistence may change only existing Answer bodies', 1);
    }
    if (!before.answers.some((answer, index) => answer !== after.answers[index])) {
      fail('TASK_ANSWER_PERSISTENCE_INVALID', 'answer persistence contains no answer-body change', 1);
    }
  }
  const proposal = validate({
    stem, expect: 'todo', checkIndex: false,
    proposal: { stem, fromState: 'todo', state: 'todo', bytes },
  });
  requireVerdict(proposal, 'TASK_QUESTIONS_WRITE_INVALID');
}

function ask(stem, bytes, revision, authority, intent = 'ask') {
  const action = intent === 'persist-answers' ? 'answers' : preStateAction(stem, ['backlog', 'pending'], 'prep', 'answers');
  const pre = requireRevision(stem, revision, action);
  if (intent === 'persist-answers' && pre.observedState !== 'pending') {
    fail('TRANSITION_PRECONDITION_FAILED', 'persist-answers is allowed only from pending', 1);
  }
  const boardPrepareQuestionsDisabled =
    process.env.ORCHESTRATOR_TASK_PREP_NO_QUESTIONS === '1' ||
    (authority && authority.kind === 'bounded-lease' && authority.key === 'standby:prep');
  if (intent === 'ask' && boardPrepareQuestionsDisabled) {
    fail(
      'TASK_PREP_QUESTIONS_DISABLED',
      'Board Prepare must choose safe defaults or report a typed actionable blocker; it cannot publish pending questions.',
      1
    );
  }
  validatePendingWriteContract(pre, bytes, intent);
  requireAuthorityScope(stem, 'ask', pre, authority);
  const taskLock = authority.bypass ? null : readOwnedTaskLock(stem, 'task-prep', authority.sessionId);
  const transition = pre.observedState === 'backlog' ? 'backlog:pending' : 'pending:pending';
  const destination = artifactPath('pending', stem);
  const marker = newMarker('ask', stem, pre, hash(bytes), authority, taskLock);
  try {
    reconfirmAction(pre, action, marker);
    if (existsRegular(destination)) detach(marker, 'pending', destination);
    assertTaskLockOwned(marker.taskLock);
    publishTarget(marker, 'pending', destination, bytes);
    assertSourceOwnership(marker); assertTaskLockOwned(marker.taskLock);
    filesystemPost(stem, transition); marker.phase = 'filesystem-valid'; writeMarker(marker);
    const publication = publishIndexOrDefer(
      () => filesystemPost(stem, transition),
      () => finalPost(stem, transition)
    );
    const final = publication.result;
    assertSourceOwnership(marker); assertTaskLockOwned(marker.taskLock); marker.phase = 'complete'; writeMarker(marker);
    cleanupMarker(marker);
    return { version: 1, ok: true, operation: intent, stem, state: 'pending',
      sourceRevision: final.sourceRevision, indexDeferred: publication.indexDeferred };
  } catch (error) { abortOperation(marker, 'pending', error); }
}

function preStateAction(stem, allowed, firstAction, secondAction) {
  const observed = validate({ stem, checkIndex: false }).observedState;
  if (!allowed.includes(observed)) fail('TRANSITION_PRECONDITION_FAILED', 'operation is not allowed from ' + observed, 1);
  return observed === allowed[0] ? firstAction : secondAction;
}

function promote(stem, bytes, revision, authority) {
  const action = preStateAction(stem, ['backlog', 'pending'], 'prep', 'answers');
  const pre = requireRevision(stem, revision, action);
  requireAuthorityScope(stem, 'promote', pre, authority);
  const taskLock = authority.bypass ? null : readOwnedTaskLock(stem, 'task-prep', authority.sessionId);
  const transition = pre.observedState + ':todo';
  const target = artifactPath('todo', stem);
  const marker = newMarker('promote', stem, pre, hash(bytes), authority, taskLock);
  try {
    reconfirmAction(pre, action, marker);
    if (existsRegular(target)) fail('TARGET_CONFLICT', 'todo target already exists', 1);
    detach(marker, 'backlog', artifactPath('backlog', stem));
    if (pre.observedState === 'pending') detach(marker, 'pending', artifactPath('pending', stem));
    assertSourceOwnership(marker); assertTaskLockOwned(marker.taskLock);
    publishTarget(marker, 'todo', target, bytes);
    assertSourceOwnership(marker); assertTaskLockOwned(marker.taskLock);
    filesystemPost(stem, transition); marker.phase = 'filesystem-valid'; writeMarker(marker);
    const publication = publishIndexOrDefer(
      () => filesystemPost(stem, transition),
      () => finalPost(stem, transition)
    );
    const final = publication.result;
    assertSourceOwnership(marker); assertTaskLockOwned(marker.taskLock); marker.phase = 'complete'; writeMarker(marker);
    cleanupMarker(marker);
    return { version: 1, ok: true, operation: 'promote', stem, state: 'todo',
      sourceRevision: final.sourceRevision, indexDeferred: publication.indexDeferred };
  } catch (error) { abortOperation(marker, 'todo', error); }
}

// Authorized in-column content replacement for tooling that must adjust a
// live task without changing its lifecycle column (for example sanctioned
// Figma de-scope).  The old task is detached before publication, so an invalid
// proposal or failed INDEX publication restores the exact source bytes.
function edit(stem, bytes, revision, authority, intent = 'edit') {
  const allowed = intent === 'edit' ? ['backlog', 'todo'] : ['todo'];
  const pre = requireStateRevision(stem, revision, allowed);
  if (intent !== 'edit') validateTaskQuestionsWriteContract(pre, bytes, intent);
  requireAuthorityScope(stem, 'edit', pre, authority);
  const state = pre.observedState, source = artifactPath(state, stem);
  const taskLock = authority.bypass ? null : readOwnedTaskLock(stem, state === 'todo' ? 'orchestrator' : 'task-prep', authority.sessionId);
  const marker = newMarker('edit', stem, pre, hash(bytes), authority, taskLock);
  try {
    reconfirmState(pre, allowed, marker);
    detach(marker, state, source);
    assertTaskLockOwned(marker.taskLock);
    publishTarget(marker, state, source, bytes);
    assertSourceOwnership(marker); assertTaskLockOwned(marker.taskLock);
    sameStatePost(stem, state, false); marker.phase = 'filesystem-valid'; writeMarker(marker);
    const publication = publishIndexOrDefer(
      () => sameStatePost(stem, state, false),
      () => sameStatePost(stem, state, true)
    );
    if (!publication.indexDeferred) maybePauseFixture('edit-index-published');
    const final = publication.result;
    assertSourceOwnership(marker); assertTaskLockOwned(marker.taskLock); marker.phase = 'complete'; writeMarker(marker);
    cleanupMarker(marker);
    return { version: 1, ok: true, operation: intent, stem, state,
      sourceRevision: final.sourceRevision, indexDeferred: publication.indexDeferred };
  } catch (error) { abortOperation(marker, state, error); }
}

function impact(stem) {
  const result = validateAction({ stem, action: 'drop', checkIndex: true });
  const admission = core.dropAdmission(result, stem);
  if (!admission.ok) fail('TRANSITION_PRECONDITION_FAILED', 'canonical drop admission failed', 1, {
    result, blockers: admission.blockers
  });
  const dependents = result.dependents || [];
  return { version: 1, ok: true, operation: 'inspect-drop', stem, state: result.observedState, sourceRevision: result.sourceRevision, dependents, impactHash: core.dropImpactHash(stem, result.sourceRevision, dependents) };
}

function drop(stem, revision, ackImpact, authority) {
  if (authority.authorityStem !== stem) fail('WRITER_AUTHORITY_SCOPE_MISMATCH', 'drop cannot use delegated parent authority', 2);
  requireTaskLockAbsent(stem);
  const pre = requireDropRevision(stem, revision);
  const currentImpact = impact(stem);
  if (currentImpact.sourceRevision !== pre.sourceRevision || currentImpact.state !== pre.observedState) {
    fail('TRANSITION_SOURCE_CHANGED', 'task source changed while drop impact was resolved', 4, currentImpact);
  }
  if (currentImpact.dependents.length && ackImpact !== currentImpact.impactHash) fail('DROP_DEPENDENTS_PRESENT', 'dependent impact changed or was not explicitly acknowledged', 1, currentImpact);
  if (ackImpact && ackImpact !== currentImpact.impactHash) fail('DROP_IMPACT_CHANGED', 'drop impact acknowledgement does not match the current snapshot', 4, currentImpact);
  const transition = pre.observedState + ':absent';
  const marker = newMarker('drop', stem, pre, null, authority, null);
  try {
    const finalImpact = impact(stem);
    if (finalImpact.state !== currentImpact.state || finalImpact.sourceRevision !== currentImpact.sourceRevision || finalImpact.impactHash !== currentImpact.impactHash) {
      fail('DROP_IMPACT_CHANGED', 'task revision or dependent impact changed immediately before mutation', 4, finalImpact);
    }
    maybePauseFixture('drop-final-impact');
    requireTaskLockAbsent(stem);
    detachRuntime(marker, 'journal', path.join(JOURNAL_DIR, stem + '.jsonl'));
    for (const column of Object.keys(marker.sourceArtifacts).sort().reverse()) {
      detach(marker, column, artifactPath(column, stem));
    }
    requireTaskLockAbsent(stem);
    assertSourceOwnership(marker);
    dropPost(stem, transition, false); marker.phase = 'filesystem-valid'; writeMarker(marker);
    let indexPublished = true;
    try { runIndex(false); }
    catch (_) {
      // Other malformed tasks may keep the strict derived INDEX unpublished.
      // The board reads canonical artifacts directly, so deletion itself can
      // complete while integrity reports the stale projection for later repair.
      indexPublished = false;
    }
    const final = dropPost(stem, transition, indexPublished);
    if (indexPublished && final.indexStatus !== 'fresh') {
      fail('TRANSITION_INDEX_NOT_FRESH', 'drop index publication did not produce a fresh projection', 1, { result: final });
    }
    requireTaskLockAbsent(stem); assertSourceOwnership(marker); marker.phase = 'complete'; writeMarker(marker);
    cleanupMarker(marker);
    return { version: 1, ok: true, operation: 'drop', stem, state: 'absent',
      dependents: currentImpact.dependents, runtimeRemoved: Object.keys(marker.runtimeDetached).sort(),
      indexDeferred: !indexPublished };
  } catch (error) { abortOperation(marker, null, error); }
}

function stripOutcome(bytes) {
  const text = new TextDecoder('utf-8', { fatal: true }).decode(bytes);
  if (text.startsWith('\uFEFF') || text.includes('\r')) {
    fail('DONE_OUTCOME_INVALID', 'done task must use canonical UTF-8 text with LF line endings', 1);
  }
  const cut = core.outcomeAppendixStart(text);
  if (cut < 0) fail('DONE_OUTCOME_INVALID', 'done task has no anchored Outcome to strip', 1);
  return Buffer.from(text.slice(0, cut).trimEnd() + '\n');
}

function reopen(stem, revision, authority) {
  if (authority.authorityStem !== stem) fail('WRITER_AUTHORITY_SCOPE_MISMATCH', 'reopen cannot use delegated parent authority', 2);
  requireTaskLockAbsent(stem);
  const pre = requireRevision(stem, revision, 'reopen');
  const source = artifactPath('done', stem), sourceBytes = safeRead(source), todoBytes = stripOutcome(sourceBytes);
  const evidenceHash = hash(sourceBytes).slice('sha256:'.length);
  const evidenceDir = path.join(REOPEN_EVIDENCE_DIR, stem);
  const evidencePath = path.join(evidenceDir, evidenceHash + '.md');
  ensureInside(TASKS_DIR, evidencePath, 'reopen history');
  const marker = newMarker('reopen', stem, pre, hash(todoBytes), authority, null);
  const target = artifactPath('todo', stem);
  try {
    reconfirmAction(pre, 'reopen', marker);
    if (hash(sourceBytes) !== marker.sourceArtifacts.done.hash) fail('TRANSITION_SOURCE_CHANGED', 'done source changed before reopen history publication', 4);
    ensureDir(evidenceDir);
    if (!existsRegular(evidencePath)) writeExclusive(evidencePath, sourceBytes, 0o644);
    else if (hash(safeRead(evidencePath)) !== hash(sourceBytes)) fail('REOPEN_HISTORY_CONFLICT', 'content-addressed reopen history conflicts', 1);
    marker.history = { path: evidencePath, proof: readFileProof(evidencePath) }; writeMarker(marker); assertHistoryOwned(marker);
    detach(marker, 'done', source);
    assertSourceOwnership(marker); assertHistoryOwned(marker);
    publishTarget(marker, 'todo', target, todoBytes);
    requireTaskLockAbsent(stem);
    assertSourceOwnership(marker); assertHistoryOwned(marker);
    filesystemPost(stem, 'done:todo'); marker.phase = 'filesystem-valid'; writeMarker(marker);
    const publication = publishIndexOrDefer(
      () => filesystemPost(stem, 'done:todo'),
      () => finalPost(stem, 'done:todo')
    );
    const final = publication.result;
    requireTaskLockAbsent(stem); assertSourceOwnership(marker); assertHistoryOwned(marker); marker.phase = 'complete'; writeMarker(marker);
    cleanupMarker(marker);
    return { version: 1, ok: true, operation: 'reopen', stem, state: 'todo',
      historyPath: path.relative(PROJECT_ROOT, marker.history.path).split(path.sep).join('/'),
      sourceRevision: final.sourceRevision, indexDeferred: publication.indexDeferred };
  } catch (error) { abortOperation(marker, 'todo', error); }
}

function recover(stem, args, authority) {
  ensureDir(STATE_DIR);
  reconcileMarkerReplacements();
  const listed = boundedDirectoryNames(STATE_DIR, 'transition') || [];
  const malformed = listed.find((name) => name.startsWith('tr-') && !/^tr-[a-f0-9]{36}\.json$/.test(name));
  if (malformed) fail('TRANSITION_MARKER_NAME_INVALID', 'malformed transition marker/recovery prefix blocks recovery: ' + malformed, 1);
  const names = listed.filter((name) => /^tr-[a-f0-9]{36}\.json$/.test(name));
  const markers = [];
  for (const name of names) {
    const bytes = safeRead(path.join(STATE_DIR, name), MARKER_MAX);
    let marker;
    try { marker = JSON.parse(bytes.toString('utf8')); } catch (_) { fail('TRANSITION_MARKER_INVALID', 'transition marker is not JSON', 1); }
    marker = requireRecoveryMarker(marker, name);
    if (marker.stem === stem) markers.push(marker);
  }
  if (markers.length > 1) fail('TRANSITION_RECOVERY_AMBIGUOUS', 'multiple active transition markers exist for one task', 1);
  const expectedAuthority = markers.length ? markers[0].authorityStem : stem;
  if ((args['--authority-stem'] || stem) !== expectedAuthority) {
    fail('WRITER_AUTHORITY_SCOPE_MISMATCH', 'recovery authority does not match the frozen transition authority', 2);
  }
  const recovered = [];
  for (const marker of markers) {
    if (marker.taskLock) {
      if (!authority.bypass && marker.taskLock.sessionId !== authority.sessionId) {
        fail('TASK_LOCK_OWNER_MISMATCH', 'recovery writer session does not match the frozen task-lock owner', 1);
      }
      assertTaskLockOwned(marker.taskLock);
    }
    const current = validate({ stem, checkIndex: false });
    const desired = marker.operation === 'ask' ? 'pending' : marker.operation === 'promote' || marker.operation === 'reopen' ? 'todo' : marker.operation === 'edit' ? marker.sourceState : marker.operation === 'drop' ? 'absent' : null;
    const canCompleteForward = ['target-published', 'filesystem-valid', 'complete'].includes(marker.phase);
    if (canCompleteForward && desired && current.ok && current.observedState === desired) {
      if (marker.targetArtifact) assertTargetOwned(marker);
      assertSourceOwnership(marker, marker.phase === 'complete');
      assertHistoryOwned(marker);
      if (marker.taskLock) assertTaskLockOwned(marker.taskLock);
      runIndex(false);
      requireVerdict(validate({ stem, expect: desired, checkIndex: true }), 'TRANSITION_INDEX_NOT_FRESH');
      if (marker.taskLock) assertTaskLockOwned(marker.taskLock);
      marker.phase = 'complete'; writeMarker(marker);
      cleanupMarker(marker); recovered.push({ transactionId: marker.transactionId, resolution: 'completed-forward' });
    } else {
      const targetColumn = marker.operation === 'ask' ? 'pending' : marker.operation === 'promote' || marker.operation === 'reopen' ? 'todo' : marker.operation === 'edit' ? marker.sourceState : null;
      rollback(marker, targetColumn); recovered.push({ transactionId: marker.transactionId, resolution: 'rolled-back' });
    }
  }
  return { version: 1, ok: true, operation: 'recover', stem, recovered };
}

function transitionIntegrityFinding(code, stem, file, message) {
  return { code, severity: 'error', stem: stem || null, paths: file ? [file] : [], message,
    recovery: 'Recover the exact transition/guard generation through transition-task-state; never delete runtime ownership by age.' };
}

function transitionIntegrity(stem = null) {
  const out = { version: 1, owner: 'transitions', statuses: [], findings: [], snapshotInputs: [], truncated: false };
  const state = fileGuards.realDirectoryUnder(PROJECT_ROOT, STATE_DIR, { allowMissing: true });
  if (!state) {
    out.findings.push(transitionIntegrityFinding('TRANSITION_DIRECTORY_UNSAFE', stem, STATE_DIR, 'Transition runtime directory cannot be inspected safely.'));
    return out;
  }
  if (!state.exists) return out;
  const listed = fileGuards.boundedDirectoryNamesUnder(PROJECT_ROOT, STATE_DIR, MAX_RUNTIME_ENTRIES);
  if (!listed.ok) {
    out.findings.push(transitionIntegrityFinding(listed.code === 'directory-entry-limit' ? 'TRANSITION_SCAN_LIMIT' : 'TRANSITION_DIRECTORY_UNSAFE', stem, STATE_DIR,
      listed.code === 'directory-entry-limit' ? 'Transition runtime directory exceeds its bounded scan limit.' : 'Transition runtime directory cannot be enumerated safely.'));
    out.truncated = listed.code === 'directory-entry-limit'; return out;
  }

  const activeGuards = new Set();
  const guardDirectory = fileGuards.realDirectoryUnder(PROJECT_ROOT, TRANSITION_GUARDS_DIR, { allowMissing: true });
  if (!guardDirectory) out.findings.push(transitionIntegrityFinding('TRANSITION_GUARD_DIRECTORY_UNSAFE', stem, TRANSITION_GUARDS_DIR, 'Transition guard directory cannot be inspected safely.'));
  else if (guardDirectory.exists) {
    const guardListed = fileGuards.boundedDirectoryNamesUnder(PROJECT_ROOT, TRANSITION_GUARDS_DIR, MAX_RUNTIME_ENTRIES);
    if (!guardListed.ok) {
      out.findings.push(transitionIntegrityFinding(guardListed.code === 'directory-entry-limit' ? 'TRANSITION_GUARD_SCAN_LIMIT' : 'TRANSITION_GUARD_DIRECTORY_UNSAFE', stem, TRANSITION_GUARDS_DIR, 'Transition guard scan is incomplete.'));
      out.truncated = guardListed.code === 'directory-entry-limit';
    } else {
      const guardNames = stem ? [stem + '.json', ...guardListed.names.filter((name) => name.startsWith('.' + stem + '.guard-recovery-'))] : guardListed.names.slice().sort();
      for (const name of guardNames) {
        const recovery = /^\.(TASK_[0-9]+_[A-Za-z0-9_]+)\.guard-recovery-[a-f0-9]{36}$/.exec(name);
        if (recovery) {
          if (!stem || recovery[1] === stem) out.findings.push(transitionIntegrityFinding('TRANSITION_GUARD_RECOVERY_REQUIRED', recovery[1], path.join(TRANSITION_GUARDS_DIR, name), 'A private transition-guard generation requires exact reconciliation.'));
          continue;
        }
        if (name.startsWith('.') && name.includes('.guard-recovery-')) {
          out.findings.push(transitionIntegrityFinding('TRANSITION_GUARD_RECOVERY_UNSAFE', stem, path.join(TRANSITION_GUARDS_DIR, name), 'Malformed transition-guard recovery prefix blocks ownership inference.'));
          continue;
        }
        if (!name.endsWith('.json')) continue;
        const rowStem = name.slice(0, -5), file = path.join(TRANSITION_GUARDS_DIR, name);
        if (core.safeIntegerId(rowStem) === null) {
          out.findings.push(transitionIntegrityFinding('TRANSITION_GUARD_NAME_INVALID', null, file, 'Transition guard filename is invalid.')); continue;
        }
        if (stem && rowStem !== stem) continue;
        const entry = fileGuards.inspectEntryUnder(PROJECT_ROOT, TRANSITION_GUARDS_DIR, file);
        if (entry.status === 'missing' && stem) continue;
        const read = fileGuards.boundedRegularFileUnder(PROJECT_ROOT, TRANSITION_GUARDS_DIR, file, GUARD_MAX);
        if (!read) { out.findings.push(transitionIntegrityFinding('TRANSITION_GUARD_INVALID', rowStem, file, 'Transition guard is unsafe, unstable, or oversized.')); continue; }
        let record;
        try { record = JSON.parse(read.bytes.toString('utf8')); } catch (_) { record = null; }
        const contentHash = hash(read.bytes);
        out.snapshotInputs.push({ owner: 'transitions', kind: 'guard', path: file, hash: contentHash, size: read.bytes.length });
        if (!validGuard(record, rowStem) || !read.bytes.equals(guardBytes(record))) {
          out.findings.push(transitionIntegrityFinding('TRANSITION_GUARD_INVALID', rowStem, file, 'Transition guard violates its canonical owner contract.')); continue;
        }
        const active = guardOwnerActive(record);
        if (active) activeGuards.add(rowStem);
        out.statuses.push({ owner: 'transitions', kind: 'guard', stem: rowStem, state: active ? 'active' : 'recovery-required',
          createdAt: record.createdAt, updatedAt: record.createdAt, contentHash });
        if (!active) out.findings.push(transitionIntegrityFinding('TRANSITION_GUARD_RECOVERY_REQUIRED', rowStem, file, 'A local transition guard has no live owner and requires authenticated recovery.'));
      }
    }
  }

  const markerIds = new Set();
  for (const name of listed.names.filter((name) => name.includes('.replace-reservation') || name.includes('.replace-wal') || name.includes('.replace-candidate-') || name.includes('.replace-detached-')).sort()) {
    const file = path.join(STATE_DIR, name);
    const read = fileGuards.boundedRegularFileUnder(PROJECT_ROOT, STATE_DIR, file,
      name.includes('.replace-wal') || name.includes('.replace-reservation') ? REPLACE_WAL_MAX : MARKER_MAX);
    if (read) out.snapshotInputs.push({ owner: 'transitions', kind: 'marker-replace', path: file, hash: hash(read.bytes), size: read.bytes.length });
    out.findings.push(transitionIntegrityFinding('TRANSITION_MARKER_RECOVERY_REQUIRED', stem, file, 'A durable marker replacement artifact requires exact reconciliation.'));
  }
  for (const name of listed.names.filter((name) => (name.endsWith('.json') || name.startsWith('tr-')) &&
      !name.includes('.replace-reservation') && !name.includes('.replace-wal') && !name.includes('.replace-candidate-') && !name.includes('.replace-detached-')).sort()) {
    const file = path.join(STATE_DIR, name);
    if (!/^tr-[a-f0-9]{36}\.json$/.test(name)) {
      out.findings.push(transitionIntegrityFinding('TRANSITION_MARKER_NAME_INVALID', null, file, 'Transition marker filename is invalid.')); continue;
    }
    const read = fileGuards.boundedRegularFileUnder(PROJECT_ROOT, STATE_DIR, file, MARKER_MAX);
    if (!read) { out.findings.push(transitionIntegrityFinding('TRANSITION_MARKER_UNSAFE', null, file, 'Transition marker is unsafe, unstable, or oversized.')); continue; }
    let marker;
    try { marker = JSON.parse(read.bytes.toString('utf8')); } catch (_) { marker = null; }
    const contentHash = hash(read.bytes);
    out.snapshotInputs.push({ owner: 'transitions', kind: 'marker', path: file, hash: contentHash, size: read.bytes.length });
    try { marker = requireRecoveryMarker(marker, name); }
    catch (error) {
      out.findings.push(transitionIntegrityFinding(error.code || 'TRANSITION_MARKER_INVALID', marker && marker.stem, file, String(error.message || error).slice(0, 500))); continue;
    }
    markerIds.add(marker.transactionId);
    if (stem && marker.stem !== stem) continue;
    const active = activeGuards.has(marker.stem) && marker.phase !== 'recovery-required';
    out.statuses.push({ owner: 'transitions', kind: 'marker', stem: marker.stem, state: active ? 'active' : 'recovery-required',
      operation: marker.operation, phase: marker.phase, createdAt: marker.createdAt, updatedAt: marker.updatedAt, contentHash });
    if (!active) out.findings.push(transitionIntegrityFinding('TRANSITION_RECOVERY_REQUIRED', marker.stem, file,
      marker.phase === 'recovery-required' ? 'Transition marker explicitly requires recovery.' : 'Transition marker has no provably active guard owner.'));
  }

  const privateRoot = path.join(STATE_DIR, '.private');
  const privateDirectory = fileGuards.realDirectoryUnder(PROJECT_ROOT, privateRoot, { allowMissing: true });
  if (!privateDirectory) out.findings.push(transitionIntegrityFinding('TRANSITION_PRIVATE_DIRECTORY_UNSAFE', stem, privateRoot, 'Transition private directory cannot be inspected safely.'));
  else if (privateDirectory.exists) {
    const privateListed = fileGuards.boundedDirectoryNamesUnder(PROJECT_ROOT, privateRoot, MAX_RUNTIME_ENTRIES);
    if (!privateListed.ok) {
      out.findings.push(transitionIntegrityFinding(privateListed.code === 'directory-entry-limit' ? 'TRANSITION_PRIVATE_SCAN_LIMIT' : 'TRANSITION_PRIVATE_DIRECTORY_UNSAFE', stem, privateRoot, 'Transition private directory scan is incomplete.'));
      out.truncated = privateListed.code === 'directory-entry-limit';
    } else for (const name of privateListed.names.sort()) {
      if (!/^tr-[a-f0-9]{36}$/.test(name) || !markerIds.has(name)) {
        out.findings.push(transitionIntegrityFinding('TRANSITION_PRIVATE_RECOVERY_REQUIRED', null, path.join(privateRoot, name), 'Orphan or invalid transition private state requires exact reconciliation.'));
      }
    }
  }
  return out;
}

const INPUT_COMMANDS = ['ask', 'persist-answers', 'publish-questions', 'persist-task-answers', 'promote', 'edit'];

function parseArgs(argv) {
  const command = argv.shift();
  const allowedCommands = ['inspect-integrity', 'inspect-drop', 'ask', 'persist-answers', 'publish-questions', 'persist-task-answers', 'promote', 'edit', 'drop', 'reopen', 'recover'];
  if (!allowedCommands.includes(command)) fail('INVOCATION_INVALID', 'expected ' + allowedCommands.join('|'), 2);
  const out = { command };
  const valueFlags = new Set(['--stem', '--authority-stem', '--input', '--source-revision', '--ack-impact', '--lease-id', '--lease-token']);
  const commandFlags = {
    'inspect-integrity': new Set(['--stem']),
    'inspect-drop': new Set(['--stem']),
    ask: new Set(['--stem', '--input', '--source-revision', '--lease-id', '--lease-token']),
    'persist-answers': new Set(['--stem', '--input', '--source-revision', '--lease-id', '--lease-token']),
    promote: new Set(['--stem', '--authority-stem', '--input', '--source-revision', '--lease-id', '--lease-token']),
    edit: new Set(['--stem', '--input', '--source-revision', '--lease-id', '--lease-token']),
    'publish-questions': new Set(['--stem', '--input', '--source-revision', '--lease-id', '--lease-token']),
    'persist-task-answers': new Set(['--stem', '--input', '--source-revision', '--lease-id', '--lease-token']),
    drop: new Set(['--stem', '--source-revision', '--ack-impact', '--lease-id', '--lease-token']),
    reopen: new Set(['--stem', '--source-revision', '--lease-id', '--lease-token']),
    recover: new Set(['--stem', '--authority-stem', '--lease-id', '--lease-token']),
  };
  for (let i = 0; i < argv.length; i++) {
    const flag = argv[i];
    if (!valueFlags.has(flag) || !commandFlags[command].has(flag) || Object.prototype.hasOwnProperty.call(out, flag)) {
      fail('INVOCATION_INVALID', 'unknown, unsupported, or duplicate option: ' + flag, 2);
    }
    if (i + 1 >= argv.length || argv[i + 1].startsWith('--')) fail('INVOCATION_INVALID', flag + ' requires a value', 2);
    out[flag] = argv[++i];
  }
  if (command === 'inspect-integrity') {
    if (out['--stem'] !== undefined && core.safeIntegerId(out['--stem']) === null) fail('INVOCATION_INVALID', '--stem is invalid', 2);
    if (Object.keys(out).some((key) => key !== 'command' && key !== '--stem')) fail('INVOCATION_INVALID', 'inspect-integrity accepts only --stem', 2);
  } else if (core.safeIntegerId(out['--stem']) === null) fail('INVOCATION_INVALID', '--stem is invalid', 2);
  if (out['--authority-stem'] !== undefined && core.safeIntegerId(out['--authority-stem']) === null) fail('INVOCATION_INVALID', '--authority-stem is invalid', 2);
  if (INPUT_COMMANDS.includes(command) && !out['--input']) fail('INVOCATION_INVALID', '--input is required', 2);
  if (INPUT_COMMANDS.concat(['drop', 'reopen']).includes(command) && !core.HASH_RE.test(String(out['--source-revision'] || ''))) {
    fail('SOURCE_REVISION_REQUIRED', '--source-revision is required', 2);
  }
  if (out['--ack-impact'] !== undefined && !core.HASH_RE.test(String(out['--ack-impact']))) {
    fail('INVOCATION_INVALID', '--ack-impact is invalid', 2);
  }
  const hasLeaseId = out['--lease-id'] !== undefined, hasLeaseToken = out['--lease-token'] !== undefined;
  if (hasLeaseId !== hasLeaseToken) fail('INVOCATION_INVALID', '--lease-id and --lease-token must be provided together', 2);
  if (hasLeaseId && (!writerLeases.LEASE_ID_RE.test(String(out['--lease-id'])) || !/^[a-f0-9]{32,128}$/.test(String(out['--lease-token'])))) {
    fail('INVOCATION_INVALID', 'writer lease credentials have an invalid shape', 2);
  }
  return out;
}

function publicError(error) {
  return { version: 1, ok: false, code: error.code || 'TRANSITION_FAILED', message: String(error.message || error).slice(0, 500), retryable: error.exitCode === 4, details: error.result ? { observedState: error.result.observedState, sourceRevision: error.result.sourceRevision, findings: error.result.findings } : undefined };
}

try {
  platformSupport.assertCanonicalTaskPlatform();
  ensureInside(PROJECT_ROOT, TASKS_DIR, 'task directory');
  const args = parseArgs(process.argv.slice(2)), stem = args['--stem'];
  activeCommand = args.command;
  // Consume and validate the complete proposal before acquiring any guard or
  // touching runtime/task state. A slow producer may keep stdin open, but it
  // cannot make the transition visible until it closes EOF with valid bytes.
  const input = INPUT_COMMANDS.includes(args.command) ? readTransitionInput(args['--input']) : null;
  let result, authority, transitionGuard, dependencyGraphMutex;
  try {
    if (args.command !== 'inspect-drop' && args.command !== 'inspect-integrity') {
      // Authority must be admitted before either transition lock. In
      // particular, site-session verification includes the finalization
      // guard, which must observe the workspace before this invocation owns
      // the dependency-graph mutex. Reversing these two steps makes the
      // invocation reject its own live mutex forever.
      authority = verifyAuthority(stem, args);
      transitionGuard = acquireTransitionGuard(stem, args.command === 'recover');
      if (args.command !== 'recover') requireNoActiveTransition(stem);
      // Global lock order is writer authority -> per-stem transition guard ->
      // dependency-graph kernel mutex. Finalization owns only the last lock;
      // create/edit own their local mutex before the same last lock, so no
      // path can form a graph-mutex -> local-mutex cycle.
      dependencyGraphMutex = await acquireDependencyGraphMutex();
      assertDependencyGraphMutexHeld(dependencyGraphMutex);
      // The first check closes admission against pre-existing finalization
      // state. This second, receipt-only check closes the wait window without
      // re-running the finalization guard against our own mutex.
      reverifyAuthority(authority);
    }
    if (args.command === 'inspect-integrity') result = transitionIntegrity(stem || null);
    else if (args.command === 'inspect-drop') result = impact(stem);
    else if (args.command === 'ask') result = ask(stem, input, args['--source-revision'], authority);
    else if (args.command === 'persist-answers') result = ask(stem, input, args['--source-revision'], authority, 'persist-answers');
    else if (args.command === 'promote') result = promote(stem, input, args['--source-revision'], authority);
    else if (args.command === 'edit') result = edit(stem, input, args['--source-revision'], authority);
    else if (args.command === 'publish-questions') result = edit(stem, input, args['--source-revision'], authority, 'publish-questions');
    else if (args.command === 'persist-task-answers') result = edit(stem, input, args['--source-revision'], authority, 'persist-task-answers');
    else if (args.command === 'drop') result = drop(stem, args['--source-revision'], args['--ack-impact'], authority);
    else if (args.command === 'reopen') result = reopen(stem, args['--source-revision'], authority);
    else result = recover(stem, args, authority);
    if (dependencyGraphMutex) assertDependencyGraphMutexHeld(dependencyGraphMutex);
  } finally {
    try {
      if (dependencyGraphMutex) await releaseDependencyGraphMutex(dependencyGraphMutex);
    } finally {
      if (transitionGuard) releaseTransitionGuard(transitionGuard);
    }
  }
  process.stdout.write(JSON.stringify(result, null, 2) + '\n');
} catch (error) {
  const code = Number.isInteger(error && error.exitCode) ? error.exitCode : error instanceof core.SnapshotRaceError ? 4 : 1;
  process.stderr.write(JSON.stringify(publicError(error), null, 2) + '\n');
  process.exitCode = code;
}
