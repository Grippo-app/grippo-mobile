#!/usr/bin/env node

// Canonical INDEX derivation and publication authority.  Source reads are
// provided by task-state-core's pinned snapshot boundary.  Publication uses a
// long-lived Python descriptor boundary that owns the kernel lock and durable
// conditional CAS for the complete scan -> publish -> postcheck transaction.

import path from 'node:path';
import os from 'node:os';
import { spawn, spawnSync } from 'node:child_process';
import { createRequire } from 'node:module';
import { fileURLToPath, pathToFileURL } from 'node:url';

const require = createRequire(import.meta.url);
const core = require('./task-state-core.cjs');
const platformSupport = require('./platform-support.cjs');

const HERE = path.dirname(fileURLToPath(import.meta.url));
const BOUNDARY = path.join(HERE, 'index-publication-boundary.py');
const CANONICAL_ROOT = path.resolve(HERE, '..', '..');
const INDEX_MAX_BYTES = 8 * 1024 * 1024;
const DIAGNOSTIC_MAX_BYTES = 256 * 1024;
const DIAGNOSTIC_FINDINGS_MAX = 64;
const BOUNDARY_OUTPUT_MAX = 2 * 1024 * 1024;
const HASH_RE = /^sha256:[a-f0-9]{64}$/;
const DECIMAL_RE = /^(?:0|[1-9][0-9]*)$/;
const PROOF_FIELDS = ['ctimeNs', 'dev', 'ino', 'mode', 'mtimeNs', 'size'];

class IndexOperationError extends Error {
  constructor(code, message, exitCode = 1, details = {}) {
    super(message);
    this.name = 'IndexOperationError';
    this.code = code;
    this.exitCode = exitCode;
    Object.assign(this, details);
  }
}

function pathWithin(authority, candidate) {
  const relative = path.relative(path.resolve(authority), path.resolve(candidate));
  return relative === '' || (relative !== '..' && !relative.startsWith('..' + path.sep) && !path.isAbsolute(relative));
}

function fixtureAllowed(repoRoot) {
  const fixture = process.env.TASK_FS_TEST_ROOT ? path.resolve(process.env.TASK_FS_TEST_ROOT) : null;
  if (!fixture || fixture === CANONICAL_ROOT || repoRoot === CANONICAL_ROOT) return false;
  return pathWithin(fixture, repoRoot) && pathWithin(path.resolve(os.tmpdir()), fixture) && !pathWithin(CANONICAL_ROOT, fixture);
}

function exactProof(value) {
  if (!value || typeof value !== 'object' || Array.isArray(value) ||
      Object.keys(value).sort().join('\0') !== PROOF_FIELDS.join('\0') ||
      typeof value.dev !== 'string' || !DECIMAL_RE.test(value.dev) ||
      typeof value.ino !== 'string' || !DECIMAL_RE.test(value.ino) ||
      typeof value.mtimeNs !== 'string' || !DECIMAL_RE.test(value.mtimeNs) ||
      typeof value.ctimeNs !== 'string' || !DECIMAL_RE.test(value.ctimeNs) ||
      !Number.isSafeInteger(value.mode) || value.mode < 0 || !Number.isSafeInteger(value.size) || value.size < 0) {
    throw new IndexOperationError('PUBLICATION_BOUNDARY_INVALID', 'publication boundary returned an invalid exact stat proof', 4);
  }
  return value;
}

function destinationSnapshot(value) {
  if (value === null) return null;
  if (!value || typeof value !== 'object' || Array.isArray(value) ||
      Object.keys(value).sort().join('\0') !== ['hash', 'proof'].join('\0') || !HASH_RE.test(String(value.hash || ''))) {
    throw new IndexOperationError('PUBLICATION_BOUNDARY_INVALID', 'publication boundary returned an invalid destination snapshot', 4);
  }
  exactProof(value.proof);
  return value;
}

function sameDestination(left, right) {
  if (left === null || right === null) return left === right;
  return left.hash === right.hash && PROOF_FIELDS.every((field) =>
    String(left.proof[field]) === String(right.proof[field]));
}

function boundaryExitCode(code) {
  if (['ARGUMENT_INVALID', 'PATH_OUTSIDE_AUTHORITY', 'PATH_UNSAFE', 'PATH_MISSING',
    'DIRECTORY_UNSAFE', 'ENTRY_UNSAFE', 'ENTRY_TOO_LARGE', 'LOCK_UNSAFE',
    'RECOVERY_NAME_UNSAFE', 'RECOVERY_LIMIT', 'RECOVERY_UNSAFE',
    'RECOVERY_ARTIFACT_UNSAFE', 'RECOVERY_ARTIFACT_TOO_LARGE',
    'RECOVERY_MANIFEST_MISSING', 'MANIFEST_INVALID', 'RECOVERY_CANDIDATE_INVALID',
    'RECOVERY_COMMIT_INVALID', 'DIRECTORY_ENTRY_LIMIT', 'NO_CLOBBER_UNSUPPORTED'].includes(code)) return 3;
  return 4;
}

function boundaryFailure(payload, defaultCode = 'PUBLICATION_BOUNDARY_FAILED') {
  const code = payload && typeof payload.code === 'string' ? payload.code.slice(0, 100) : defaultCode;
  const message = payload && typeof payload.message === 'string'
    ? payload.message.slice(0, 500) : 'publication boundary failed without a valid diagnostic';
  const mapped = code === 'ENTRY_UNSAFE' ? 'DESTINATION_UNSAFE' :
    code === 'ENTRY_TOO_LARGE' ? 'DESTINATION_UNSAFE' : code;
  return new IndexOperationError(mapped, message, boundaryExitCode(code));
}

function parseBoundaryEnvelope(raw) {
  let value;
  try { value = JSON.parse(raw); }
  catch (_) { throw new IndexOperationError('PUBLICATION_BOUNDARY_INVALID', 'publication boundary returned invalid JSON', 4); }
  if (!value || value.version !== 1 || typeof value.ok !== 'boolean' ||
      Object.keys(value).sort().join('\0') !== (value.ok ? ['ok', 'result', 'version'] : ['error', 'ok', 'version']).join('\0')) {
    throw new IndexOperationError('PUBLICATION_BOUNDARY_INVALID', 'publication boundary returned an invalid envelope', 4);
  }
  if (!value.ok) throw boundaryFailure(value.error);
  return value.result;
}

function inspectBoundary(options) {
  const request = {
    version: 1,
    action: 'inspect',
    repoRoot: options.repoRoot,
    tasksDir: options.tasksDir,
    canonicalRoot: CANONICAL_ROOT,
    fixture: fixtureAllowed(options.repoRoot)
  };
  const child = spawnSync('python3', [BOUNDARY], {
    input: JSON.stringify(request) + '\n',
    encoding: 'utf8',
    maxBuffer: BOUNDARY_OUTPUT_MAX,
    timeout: 20000,
    env: { ...process.env, PYTHONDONTWRITEBYTECODE: '1' }
  });
  if (child.error || child.status !== 0) {
    throw new IndexOperationError('PUBLICATION_BOUNDARY_FAILED',
      String(child.error && child.error.message || child.stderr || 'read-only boundary failed').slice(0, 500), 4);
  }
  const result = parseBoundaryEnvelope(String(child.stdout || '').trim());
  if (!result || !Number.isSafeInteger(result.activeRecovery) || result.activeRecovery < 0 ||
      !Number.isSafeInteger(result.retainedEvidence) || result.retainedEvidence < 0) {
    throw new IndexOperationError('PUBLICATION_BOUNDARY_INVALID', 'read-only boundary returned an invalid inventory', 4);
  }
  destinationSnapshot(result.destination);
  if (result.activeRecovery) {
    throw new IndexOperationError('INDEX_RECOVERY_REQUIRED',
      'an interrupted INDEX publication requires automatic recovery by the next publisher', 4);
  }
  return result;
}

class BoundarySession {
  constructor(options) {
    this.options = options;
    this.child = null;
    this.buffer = '';
    this.pending = [];
    this.stderr = '';
    this.ended = false;
  }

  async open() {
    this.child = spawn('python3', [BOUNDARY], {
      stdio: ['pipe', 'pipe', 'pipe'],
      env: { ...process.env, PYTHONDONTWRITEBYTECODE: '1' }
    });
    this.child.stdout.setEncoding('utf8');
    this.child.stderr.setEncoding('utf8');
    this.child.stdout.on('data', (chunk) => this.#onData(chunk));
    this.child.stderr.on('data', (chunk) => { this.stderr = (this.stderr + chunk).slice(-4000); });
    this.child.on('error', (error) => this.#end(error));
    this.child.on('close', (status, signal) => this.#end(new Error(
      'publication boundary exited ' + String(status === null ? signal : status))));
    const ready = await this.request({
      action: 'open',
      repoRoot: this.options.repoRoot,
      tasksDir: this.options.tasksDir,
      canonicalRoot: CANONICAL_ROOT,
      fixture: fixtureAllowed(this.options.repoRoot),
      timeoutMs: this.options.lockTimeoutMs
    }, this.options.lockTimeoutMs + 10000);
    if (!ready || typeof ready.sessionId !== 'string' || !/^[a-f0-9]{48}$/.test(ready.sessionId) ||
        !Array.isArray(ready.recovered) || !Number.isSafeInteger(ready.retainedEvidence) ||
        typeof ready.diagnosticAvailable !== 'boolean' ||
        (ready.diagnosticError !== null && typeof ready.diagnosticError !== 'string')) {
      throw new IndexOperationError('PUBLICATION_BOUNDARY_INVALID', 'publication boundary returned an invalid live session handshake', 4);
    }
    destinationSnapshot(ready.destination);
    destinationSnapshot(ready.lock);
    return ready;
  }

  #onData(chunk) {
    this.buffer += chunk;
    if (this.buffer.length > BOUNDARY_OUTPUT_MAX) {
      this.#end(new Error('publication boundary output exceeded its byte bound'));
      try { this.child.kill('SIGKILL'); } catch (_) {}
      return;
    }
    while (true) {
      const newline = this.buffer.indexOf('\n');
      if (newline < 0) return;
      const line = this.buffer.slice(0, newline);
      this.buffer = this.buffer.slice(newline + 1);
      const waiter = this.pending.shift();
      if (!waiter) {
        this.#end(new Error('publication boundary sent an unsolicited response'));
        return;
      }
      clearTimeout(waiter.timer);
      try { waiter.resolve(parseBoundaryEnvelope(line)); }
      catch (error) { waiter.reject(error); }
    }
  }

  #end(error) {
    if (this.ended) return;
    this.ended = true;
    const detail = String(error && error.message || error || this.stderr || 'boundary ended').slice(0, 500);
    const terminal = error instanceof IndexOperationError ? error :
      new IndexOperationError('PUBLICATION_BOUNDARY_EXITED', detail, 4);
    for (const waiter of this.pending.splice(0)) {
      clearTimeout(waiter.timer);
      waiter.reject(terminal);
    }
  }

  request(payload, timeoutMs = 70000) {
    if (!this.child || this.ended || !this.child.stdin.writable) {
      return Promise.reject(new IndexOperationError('PUBLICATION_BOUNDARY_EXITED', 'publication boundary is not live', 4));
    }
    const line = JSON.stringify({ version: 1, ...payload }) + '\n';
    if (Buffer.byteLength(line) > 16 * 1024 * 1024) {
      return Promise.reject(new IndexOperationError('OUTPUT_TOO_LARGE', 'publication boundary request exceeds its byte bound', 3));
    }
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        // End the protocol synchronously before killing the child.  Removing a
        // waiter and accepting a late response could otherwise shift that
        // response onto the next request.
        this.#end(new IndexOperationError('PUBLICATION_BOUNDARY_TIMEOUT',
          'publication boundary response timed out', 4));
        try { this.child.kill('SIGKILL'); } catch (_) {}
      }, timeoutMs);
      this.pending.push({ resolve, reject, timer });
      this.child.stdin.write(line, 'utf8', (error) => {
        if (error) this.#end(error);
      });
    });
  }

  async close() {
    if (!this.child) return;
    if (!this.ended) {
      try { await this.request({ action: 'close' }, 5000); } catch (_) {}
    }
    try { this.child.stdin.end(); } catch (_) {}
    if (!this.ended) {
      await new Promise((resolve) => {
        const timer = setTimeout(() => { try { this.child.kill('SIGKILL'); } catch (_) {} resolve(); }, 2000);
        this.child.once('close', () => { clearTimeout(timer); resolve(); });
      });
    }
  }
}

function utcNow() {
  return new Date().toISOString().replace(/\.\d{3}Z$/, 'Z');
}

function sanitizeText(value, repoRoot, max = 300, redactions = []) {
  let text = String(value || '').replaceAll(repoRoot, '.');
  for (const hidden of redactions) if (hidden && hidden !== repoRoot) text = text.replaceAll(String(hidden), '<contract>');
  text = text.replace(/[\r\n\0]+/g, ' ');
  if (text.length > max) text = text.slice(0, max - 1) + '…';
  return text;
}

function boundedFindings(findings, repoRoot, redactions = []) {
  return (Array.isArray(findings) ? findings : []).slice(0, DIAGNOSTIC_FINDINGS_MAX).map((item) => ({
    code: sanitizeText(item.code, repoRoot, 80, redactions),
    severity: sanitizeText(item.severity, repoRoot, 20, redactions),
    stem: typeof item.stem === 'string' ? item.stem.slice(0, 120) : null,
    paths: (Array.isArray(item.paths) ? item.paths : []).slice(0, 6).map((entry) => sanitizeText(entry, repoRoot, 240, redactions)),
    message: sanitizeText(item.message, repoRoot, 240, redactions),
    recovery: sanitizeText(item.recovery, repoRoot, 240, redactions)
  }));
}

function publicationBlockers(result) {
  return result.findings.filter((item) => item.severity === 'error' || item.severity === 'blocker');
}

function diagnosticFor({ operation, ok, code, result, error, repoRoot, generatedAt = null, redactions = [] }) {
  const findings = result ? result.findings : (error && error.findings) || [];
  return {
    version: 1,
    kind: 'task-index-diagnostic',
    operation,
    ok,
    integrityOk: result ? result.ok : false,
    code,
    recordedAt: utcNow(),
    generatedAt,
    snapshotHash: result ? result.snapshotHash : (error && error.snapshotHash) || null,
    indexStatus: result ? result.indexStatus : 'unchecked',
    message: error ? (error instanceof core.ContractError
      ? 'Required task-state contract is unreadable or malformed.'
      : sanitizeText(error.message, repoRoot, 400, redactions)) : null,
    findings: boundedFindings(findings, repoRoot, redactions),
    stats: result ? result.stats : { tasks: 0, files: 0, durationMs: 0 }
  };
}

function validationOptions(options, checkIndex = false) {
  return {
    repoRoot: options.repoRoot,
    tasksDir: options.tasksDir,
    outcomeShapePath: options.outcomeShapePath,
    checkIndex,
    includeRuntime: false
  };
}

function validationObservationCode(error) {
  const candidate = String(error && error.code || 'TASK_STATE_VALIDATION_FAILED');
  return /^[A-Za-z0-9_.:-]{1,80}$/.test(candidate) ? candidate : 'TASK_STATE_VALIDATION_FAILED';
}

function validateObserved(options, checkIndex, phase) {
  const started = process.hrtime.bigint();
  const context = {
    scope: 'all',
    action: options.check ? 'index-check' : 'index-publish',
    phase
  };
  let result;
  try {
    result = core.validateTaskState(validationOptions(options, checkIndex));
  } catch (error) {
    if (options.observe === true) {
      const durationMs = Math.min(Number((process.hrtime.bigint() - started) / 1000000n), 60 * 60 * 1000);
      const observation = core.observationFor({
        ...context,
        ok: false,
        overallOk: false,
        findings: [{ code: validationObservationCode(error), severity: 'blocker' }],
        stats: { durationMs, scanMode: 'full', taskBodyReads: 0 }
      }, { caller: 'server' });
      process.stderr.write('[task-state] ' + JSON.stringify(observation) + '\n');
    }
    throw error;
  }
  if (options.observe === true) {
    const observation = core.observationFor({ ...result, ...context }, { caller: 'server' });
    process.stderr.write('[task-state] ' + JSON.stringify(observation) + '\n');
  }
  return result;
}

function sourceSnapshotHash(result) {
  const inputs = result._model.snapshotInputs.filter((row) => !/(?:^|\/)INDEX\.json$/.test(row.path));
  return core.sha256('task-state-source-snapshot-v1\0' + core.canonicalJson(inputs));
}

function assertStableSource(reference, candidate, message, raceCode = 'TASK_SNAPSHOT_CHANGED') {
  const blockers = publicationBlockers(candidate).filter((item) => !String(item.code || '').startsWith('INDEX_'));
  if (blockers.length) {
    throw new IndexOperationError('INDEX_PUBLICATION_BLOCKED',
      'task integrity became invalid during canonical publication; last valid INDEX.json was preserved', 1, {
        findings: candidate.findings, snapshotHash: candidate.snapshotHash, result: candidate
      });
  }
  if (sourceSnapshotHash(reference) !== sourceSnapshotHash(candidate)) {
    throw new IndexOperationError(raceCode, message, 4, {
      findings: candidate.findings, snapshotHash: candidate.snapshotHash, result: candidate
    });
  }
}

async function executeCheck(options) {
  inspectBoundary(options);
  const first = validateObserved(options, false, 'pre');
  const firstBlockers = publicationBlockers(first);
  if (firstBlockers.length) {
    throw new IndexOperationError('INDEX_PUBLICATION_BLOCKED',
      'task integrity has error or blocker findings', 1, {
        findings: first.findings, snapshotHash: first.snapshotHash, result: first
      });
  }
  if (typeof options.beforeRescan === 'function') await options.beforeRescan(first);
  const second = validateObserved(options, true, 'post');
  assertStableSource(first, second, 'task corpus changed between read-only canonical scans');
  if (second.indexStatus !== 'fresh') {
    throw new IndexOperationError('INDEX_NOT_FRESH',
      'INDEX.json is missing, unsafe, invalid, or structurally stale', 1, {
        findings: second.findings, snapshotHash: second.snapshotHash, result: second
      });
  }
  return { operation: 'check', result: second, code: 'INDEX_FRESH', generatedAt: null };
}

async function executePublish(options, session) {
  const first = validateObserved(options, false, 'pre');
  const firstBlockers = publicationBlockers(first);
  if (firstBlockers.length) {
    throw new IndexOperationError('INDEX_PUBLICATION_BLOCKED',
      'task integrity has error or blocker findings; last valid INDEX.json was preserved', 1, {
        findings: first.findings, snapshotHash: first.snapshotHash, result: first
      });
  }
  if (typeof options.beforeRescan === 'function') await options.beforeRescan(first);
  const second = validateObserved(options, false, 'pre');
  assertStableSource(first, second, 'task corpus changed between canonical scans; no index was published');
  if (typeof options.beforePublish === 'function') await options.beforePublish(second);
  const finalSource = validateObserved(options, false, 'pre');
  assertStableSource(second, finalSource,
    'task corpus changed before conditional index publication; no index was published');

  const index = core.deriveIndex(finalSource._model);
  const bytes = Buffer.from(JSON.stringify(index, null, 2) + '\n');
  if (bytes.length > INDEX_MAX_BYTES) {
    throw new IndexOperationError('OUTPUT_TOO_LARGE', 'INDEX.json exceeds its bounded output limit', 3);
  }
  const staged = await session.request({ action: 'stage', rawBase64: bytes.toString('base64') });
  destinationSnapshot(staged && staged.destination);
  if (typeof options.afterPublish === 'function') await options.afterPublish(finalSource, index);

  // Keep rollback authority until two independent post-publication scans and
  // exact destination proofs have both succeeded.  The commit directory rename
  // is the durable boundary after which recovery completes rather than rolls
  // back the generation.
  const post = validateObserved(options, true, 'post');
  assertStableSource(finalSource, post,
    'task corpus changed after conditional index publication; incumbent will be restored',
    'TASK_SNAPSHOT_CHANGED_AFTER_PUBLICATION');
  if (post.indexStatus !== 'fresh') {
    throw new IndexOperationError('DESTINATION_CHANGED',
      'published INDEX generation is not the exact canonical snapshot', 4, {
        findings: post.findings, snapshotHash: post.snapshotHash, result: post
      });
  }
  destinationSnapshot((await session.request({ action: 'verify' })).destination);
  const finalPost = validateObserved(options, true, 'post');
  assertStableSource(post, finalPost,
    'task corpus changed at the final publication fence; incumbent will be restored',
    'TASK_SNAPSHOT_CHANGED_AFTER_PUBLICATION');
  if (finalPost.indexStatus !== 'fresh') {
    throw new IndexOperationError('DESTINATION_CHANGED',
      'INDEX generation changed at the final publication fence', 4, {
        findings: finalPost.findings, snapshotHash: finalPost.snapshotHash, result: finalPost
      });
  }
  const committed = destinationSnapshot((await session.request({ action: 'commit' })).destination);
  const immediate = destinationSnapshot((await session.request({ action: 'snapshot' })).destination);
  if (!sameDestination(committed, immediate)) {
    throw new IndexOperationError('DESTINATION_CHANGED',
      'INDEX generation changed immediately after the durable commit boundary', 4, {
        findings: finalPost.findings, snapshotHash: finalPost.snapshotHash, result: finalPost
      });
  }

  // A final read-only verdict prevents a false success if a non-cooperating
  // writer replaces the committed target immediately after the durable fence.
  const confirmed = validateObserved(options, true, 'post');
  assertStableSource(finalPost, confirmed,
    'task corpus changed immediately after INDEX commit; retry from a fresh snapshot');
  if (confirmed.indexStatus !== 'fresh') {
    throw new IndexOperationError('TASK_SNAPSHOT_CHANGED_AFTER_PUBLICATION',
      'INDEX generation changed immediately after durable commit', 4, {
        findings: confirmed.findings, snapshotHash: confirmed.snapshotHash, result: confirmed
      });
  }
  const finalDestination = destinationSnapshot((await session.request({ action: 'snapshot' })).destination);
  if (!sameDestination(committed, finalDestination)) {
    throw new IndexOperationError('DESTINATION_CHANGED',
      'INDEX exact generation changed during the final read-only confirmation', 4, {
        findings: confirmed.findings, snapshotHash: confirmed.snapshotHash, result: confirmed
      });
  }
  return { operation: 'publish', result: confirmed, code: 'INDEX_PUBLISHED',
    generatedAt: index.generatedAt, index };
}

function normalizeOptions(rawOptions) {
  const repoRoot = path.resolve(rawOptions.repoRoot || process.env.ORCHESTRATOR_PROJECT_ROOT || process.cwd());
  const tasksDir = path.resolve(rawOptions.tasksDir || process.env.ORCHESTRATOR_TASKS_DIR || path.join(repoRoot, 'orchestrator', 'tasks'));
  const outcomeShapePath = path.resolve(rawOptions.outcomeShapePath || process.env.ORCHESTRATOR_OUTCOME_SHAPE_PATH ||
    path.join(HERE, '..', 'contracts', 'outcome-shape.json'));
  if (!pathWithin(repoRoot, tasksDir) || tasksDir === repoRoot) {
    throw new IndexOperationError('PATH_OUTSIDE_REPOSITORY', 'task directory must remain inside the repository', 3);
  }
  if (!pathWithin(repoRoot, outcomeShapePath)) {
    const rawAuthority = rawOptions.outcomeShapeAuthorityRoot || process.env.ORCHESTRATOR_OUTCOME_SHAPE_AUTHORITY_ROOT;
    if (!rawAuthority) {
      throw new IndexOperationError('CONTRACT_AUTHORITY_REQUIRED',
        'an outcome contract outside the repository requires an explicit authority root', 3);
    }
    const authority = path.resolve(rawAuthority);
    if (!pathWithin(authority, outcomeShapePath)) {
      throw new IndexOperationError('CONTRACT_AUTHORITY_INVALID',
        'outcome contract escapes its explicit authority root', 3);
    }
  }
  const lockTimeoutMs = rawOptions.lockTimeoutMs === undefined
    ? Number(process.env.TASK_INDEX_LOCK_TIMEOUT_MS || 30000) : rawOptions.lockTimeoutMs;
  if (!Number.isSafeInteger(lockTimeoutMs) || lockTimeoutMs < 100 || lockTimeoutMs > 60000) {
    throw new IndexOperationError('INVOCATION_INVALID', 'INDEX lock timeout must be 100..60000 ms', 2);
  }
  return { ...rawOptions, repoRoot, tasksDir, outcomeShapePath, lockTimeoutMs };
}

async function publishDiagnostic(session, diagnostic, options) {
  const bytes = Buffer.from(JSON.stringify(diagnostic, null, 2) + '\n');
  if (bytes.length > DIAGNOSTIC_MAX_BYTES) {
    diagnostic.diagnosticWriteError = 'bounded diagnostic exceeds its byte limit';
    return;
  }
  try {
    await session.request({ action: 'diagnostic', rawBase64: bytes.toString('base64') });
  } catch (error) {
    diagnostic.diagnosticWriteError = sanitizeText(error && error.message || error,
      options.repoRoot, 300, [options.outcomeShapePath, CANONICAL_ROOT]);
  }
}

export async function runIndexOperation(rawOptions = {}) {
  let options;
  try {
    // The exported API is a public lifecycle boundary too.  Keep the platform
    // assertion here (rather than relying on main()) so embedders cannot open
    // the long-lived publication session on an unsupported host.
    platformSupport.assertCanonicalTaskPlatform();
    options = normalizeOptions(rawOptions);
  }
  catch (error) {
    const repoRoot = path.resolve(rawOptions.repoRoot || process.env.ORCHESTRATOR_PROJECT_ROOT || process.cwd());
    const diagnostic = diagnosticFor({ operation: rawOptions.check ? 'check' : 'publish', ok: false,
      code: error.code || 'INVOCATION_INVALID', error, repoRoot });
    return { exitCode: Number.isInteger(error.exitCode) ? error.exitCode : 2, diagnostic, index: null };
  }
  const operation = options.check ? 'check' : 'publish';
  const redactions = [options.outcomeShapePath, CANONICAL_ROOT];
  let session = null;
  try {
    if (options.check) {
      const completed = await executeCheck(options);
      const diagnostic = diagnosticFor({ operation, ok: true, code: completed.code,
        result: completed.result, repoRoot: options.repoRoot, generatedAt: null, redactions });
      return { exitCode: 0, diagnostic, index: null };
    }
    session = new BoundarySession(options);
    await session.open();
    const completed = await executePublish(options, session);
    const diagnostic = diagnosticFor({ operation, ok: true, code: completed.code,
      result: completed.result, repoRoot: options.repoRoot,
      generatedAt: completed.generatedAt, redactions });
    await publishDiagnostic(session, diagnostic, options);
    return { exitCode: 0, diagnostic, index: completed.index };
  } catch (error) {
    if (session) {
      try { await session.request({ action: 'abort' }, 15000); } catch (_) {}
    }
    const exitCode = Number.isInteger(error && error.exitCode) ? error.exitCode :
      error instanceof core.ContractError ? 3 : error instanceof core.SnapshotRaceError ? 4 : 4;
    const result = error && error.result ? error.result : null;
    const code = error && error.code ? error.code :
      error instanceof core.ContractError
        ? (/unsafe|oversized/i.test(String(error.message || '')) ? 'CONTRACT_UNSAFE' : 'CONTRACT_UNREADABLE') :
        error instanceof core.SnapshotRaceError ? 'TASK_SNAPSHOT_RACE' : 'INDEX_OPERATION_IO';
    const diagnostic = diagnosticFor({ operation, ok: false, code, result, error,
      repoRoot: options.repoRoot, redactions });
    if (session && !options.check) await publishDiagnostic(session, diagnostic, options);
    return { exitCode, diagnostic, index: null };
  } finally {
    if (session) await session.close();
  }
}

function usage() {
  return 'usage: node orchestrator/tasks/task-index.mjs [--check] [--json] [--quiet]';
}

function parseArgs(argv) {
  const parsed = { check: false, json: false, quiet: false, help: false };
  for (const arg of argv) {
    if (arg === '--check') parsed.check = true;
    else if (arg === '--json') parsed.json = true;
    else if (arg === '--quiet') parsed.quiet = true;
    else if (arg === '--help' || arg === '-h') parsed.help = true;
    else throw new IndexOperationError('INVOCATION_INVALID', 'unknown option: ' + arg, 2);
  }
  if (parsed.json && parsed.quiet) throw new IndexOperationError('INVOCATION_INVALID', '--json and --quiet are mutually exclusive', 2);
  return parsed;
}

async function main() {
  try {
    const parsed = parseArgs(process.argv.slice(2));
    if (parsed.help) { process.stdout.write(usage() + '\n'); return; }
    platformSupport.assertCanonicalTaskPlatform();
    const outcome = await runIndexOperation({ check: parsed.check, observe: !parsed.quiet });
    if (!parsed.quiet) {
      if (parsed.json) process.stdout.write(JSON.stringify(outcome.diagnostic, null, 2) + '\n');
      else if (outcome.exitCode === 0) {
        process.stdout.write(parsed.check ? 'regen-index.py --check: INDEX.json is structurally fresh\n' :
          'regen-index.py: published canonical INDEX.json (' + outcome.diagnostic.snapshotHash + ')\n');
      } else {
        process.stderr.write('regen-index.py: ' + outcome.diagnostic.code + ': ' +
          (outcome.diagnostic.message || 'index operation failed') + '\n');
        for (const item of outcome.diagnostic.findings) {
          if (item.severity === 'blocker' || item.severity === 'error') {
            process.stderr.write('[' + item.severity.toUpperCase() + '] ' + item.code + ': ' + item.message + '\n');
          }
        }
      }
    }
    process.exitCode = outcome.exitCode;
  } catch (error) {
    process.stderr.write('task-index: ' + String(error && error.message || error) + '\n' + usage() + '\n');
    process.exitCode = Number.isInteger(error && error.exitCode) ? error.exitCode : 2;
  }
}

const isMain = process.argv[1] && pathToFileURL(path.resolve(process.argv[1])).href === import.meta.url;
if (isMain) await main();
