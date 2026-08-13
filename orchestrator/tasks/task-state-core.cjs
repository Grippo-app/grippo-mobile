'use strict';

// Canonical, side-effect-free task-state engine.  Every lifecycle caller uses
// this module for classification, parsing, dependency checks, revision fencing
// and INDEX derivation.  Publication/mutation deliberately live elsewhere.

const fs = require('fs');
const path = require('path');
const os = require('os');
const crypto = require('crypto');
const childProcess = require('child_process');
const durableCas = require('./durable-cas-contract.cjs');
const platformSupport = require('./platform-support.cjs');
const taskSource = require('./task-source-contract.cjs');
const designParser = require('../figma/scripts/design-parser.cjs');

const VERSION = 1;
const INDEX_VERSION = 2;
const STEM_MAX = 120;
const MAX_SAFE_ID = BigInt(Number.MAX_SAFE_INTEGER);
const STEM_RE = /^TASK_([1-9][0-9]*)_([A-Za-z0-9_]+)$/;
const HASH_RE = /^sha256:[a-f0-9]{64}$/;
const STATES = Object.freeze(['absent', 'backlog', 'pending', 'todo', 'done', 'corrupt']);
const COLUMNS = Object.freeze(['backlog', 'pending', 'todo', 'done']);
const LIVE_STATES = Object.freeze(['backlog', 'pending', 'todo']);
const ALLOWED_TRANSITIONS = new Set([
  'absent:backlog',
  'backlog:pending',
  'backlog:todo',
  'pending:pending',
  'pending:todo',
  'todo:done',
  'done:todo',
  'backlog:absent',
  'pending:absent',
  'todo:absent',
  'done:absent',
  'corrupt:absent'
]);
const ACTION_STATES = Object.freeze({
  prep: ['backlog'],
  answers: ['pending'],
  run: ['todo'],
  finalize: ['todo', 'done'],
  drop: ['backlog', 'pending', 'todo', 'done', 'corrupt'],
  reopen: ['done']
});

const MAX_TASK_BYTES = 8 * 1024 * 1024;
const MAX_INDEX_BYTES = 8 * 1024 * 1024;
const MAX_RUNTIME_BYTES = 256 * 1024;
const MAX_LOCK_BYTES = 32 * 1024;
const MAX_DURABLE_CAS_BYTES = 16 * 1024 * 1024;
const MAX_DURABLE_CAS_MANIFEST_BYTES = 16 * 1024;
const MAX_FILES = 10000;
const MAX_CORPUS_BYTES = 128 * 1024 * 1024;
const LOCK_RUN_ID_RE = /^[A-Za-z0-9][A-Za-z0-9._:-]{7,159}$/;
const LOCK_SESSION_ID_RE = /^ws-[A-Za-z0-9][A-Za-z0-9._-]{15,159}$/;
const LOCK_OWNER_KINDS = new Set(['site', 'standby', 'direct', 'agent']);
const LOCK_FIELDS_V1 = ['owner', 'runId', 'sessionId', 'stage', 'startedAt', 'stem', 'version'];
const LOCK_OWNER_FIELDS_V1 = ['hostname', 'id', 'kind', 'pid', 'processStartId', 'startedAt'];
// This is the same owned-file generation used by finalize-task when it proves
// that a retained lock is still the generation captured in its marker. ctime
// is deliberately excluded: publishing a hard-link ownership proof changes
// ctime without replacing the inode or its bytes.
const LOCK_GENERATION_FIELDS = Object.freeze(['dev', 'hash', 'ino', 'kind', 'mode', 'mtimeNs', 'size']);
const LOCK_PROCESS_START_ID_RE = /^psid-v1:(?:linux|darwin|win32):[a-f0-9]{64}$/;
const RUNTIME_STATUS_REQUIRED = ['kind', 'owner', 'state', 'stem'];
const RUNTIME_STATUS_OPTIONAL = new Set([
  'action', 'requestId', 'key', 'leaseId', 'writerKind', 'operation', 'phase',
  'createdAt', 'updatedAt', 'contentHash', 'lockGenerationHash', 'revision'
]);
const RUNTIME_SEVERITIES = new Set(['info', 'warning', 'error', 'blocker']);
const MAX_RUNTIME_PATHS = 20;
const MAX_RUNTIME_STATUSES = 5000;
const MAX_RUNTIME_FINDINGS = 2000;
const MAX_RUNTIME_SNAPSHOT_INPUTS = 12000;
const MAX_ARCH_OUTPUT_BYTES = 256 * 1024;
// A bound-product scan is about 30s on the reference repository. The global
// checker gets 50% headroom while staying below the explicit 60s API ceiling;
// Site callers run it asynchronously and cache a settled result for 60s.
const ARCH_CHECK_TIMEOUT_MS = 45000;
const MAX_COMPLETED_LOCK_RELEASES_PER_STEM = 256;

const RUNTIME_ROOT_FIELDS = Object.freeze([
  'projectRoot', 'tasksDir', 'locksDir', 'requestsDir', 'requestReservationsDir',
  'runsDir', 'finalizationsDir', 'writerLeasesDir', 'taskCreationsDir',
  'taskEditsDir', 'taskIntakeDir', 'transitionsDir', 'journalDir',
  'writerAuthorityRoot', 'taskCreationsAuthorityRoot', 'taskEditsAuthorityRoot'
]);

class ContractError extends Error {
  constructor(message) { super(message); this.name = 'ContractError'; this.exitCode = 3; }
}
class SnapshotRaceError extends Error {
  constructor(message) { super(message); this.name = 'SnapshotRaceError'; this.exitCode = 4; }
}

function sha256(value) {
  return 'sha256:' + crypto.createHash('sha256').update(value).digest('hex');
}

function canonicalJson(value) {
  if (value === null || typeof value !== 'object') return JSON.stringify(value);
  if (Array.isArray(value)) return '[' + value.map(canonicalJson).join(',') + ']';
  return '{' + Object.keys(value).sort().map((key) => JSON.stringify(key) + ':' + canonicalJson(value[key])).join(',') + '}';
}

function lockGenerationHash(value) {
  if (!value || typeof value !== 'object' || Array.isArray(value) ||
      typeof value.dev !== 'string' || !/^(?:0|[1-9][0-9]*)$/.test(value.dev) ||
      typeof value.ino !== 'string' || !/^(?:0|[1-9][0-9]*)$/.test(value.ino) ||
      typeof value.mtimeNs !== 'string' || !/^-?(?:0|[1-9][0-9]*)$/.test(value.mtimeNs) || value.mtimeNs === '-0' ||
      value.kind !== 'file' || !Number.isSafeInteger(value.mode) || value.mode < 0 ||
      !Number.isSafeInteger(value.size) || value.size < 0 || !HASH_RE.test(String(value.hash || ''))) return null;
  const proof = Object.fromEntries(LOCK_GENERATION_FIELDS.map((field) => [field, value[field]]));
  return sha256('task-lock-owned-generation-v1\0' + canonicalJson(proof));
}

function canonicalOrder(left, right) {
  const a = canonicalJson(left), b = canonicalJson(right);
  return a < b ? -1 : a > b ? 1 : 0;
}

function runtimeRootsFor(options = {}, resolved = {}) {
  const projectRoot = path.resolve(resolved.repoRoot || options.repoRoot || process.env.ORCHESTRATOR_PROJECT_ROOT || path.join(__dirname, '..', '..'));
  const projectRootConfigured = !!options.repoRoot || !!process.env.ORCHESTRATOR_PROJECT_ROOT;
  const tasksDir = path.resolve(resolved.tasksDir || options.tasksDir || process.env.ORCHESTRATOR_TASKS_DIR || path.join(projectRoot, 'orchestrator', 'tasks'));
  const runtimeCache = path.resolve(options.cacheDir || process.env.ORCHESTRATOR_CACHE_DIR || path.join(projectRoot, 'orchestrator', '.cache'));
  const taskCache = path.join(runtimeCache, 'tasks');
  const root = (optionName, envName, fallback) => path.resolve(options[optionName] || process.env[envName] || fallback);
  const locksDir = root('locksDir', 'ORCHESTRATOR_LOCKS_DIR', path.join(taskCache, 'locks'));
  const requestsDir = root('requestsDir', 'ORCHESTRATOR_REQUESTS_DIR', path.join(taskCache, 'requests'));
  const finalizationsDir = root('finalizationsDir', 'ORCHESTRATOR_FINALIZATIONS_DIR', path.join(taskCache, 'finalizations'));
  const taskCreationsDir = root('taskCreationsDir', 'ORCHESTRATOR_TASK_CREATIONS_DIR', path.join(taskCache, 'creations'));
  const taskEditsDir = root('taskEditsDir', 'ORCHESTRATOR_TASK_EDITS_DIR', path.join(taskCache, 'edits'));
  const roots = {
    projectRoot,
    tasksDir,
    locksDir,
    requestsDir,
    requestReservationsDir: root('requestReservationsDir', 'ORCHESTRATOR_REQUEST_RESERVATIONS_DIR', path.join(path.dirname(requestsDir), 'request-reservations')),
    runsDir: root('runsDir', 'ORCHESTRATOR_RUNS_DIR', path.join(taskCache, 'runs')),
    finalizationsDir,
    writerLeasesDir: root('writerLeasesDir', 'ORCHESTRATOR_WRITER_LEASES_DIR', path.join(finalizationsDir, '.writers')),
    taskCreationsDir,
    taskEditsDir,
    taskIntakeDir: root('taskIntakeDir', 'ORCHESTRATOR_TASK_INTAKE_DIR', path.join(taskCache, 'intake')),
    transitionsDir: root('transitionsDir', 'ORCHESTRATOR_TRANSITIONS_DIR', path.join(path.dirname(locksDir), 'transitions')),
    journalDir: root('journalDir', 'ORCHESTRATOR_JOURNAL_DIR', path.join(taskCache, 'journal')),
    writerAuthorityRoot: root('writerAuthorityRoot', 'ORCHESTRATOR_WRITER_AUTHORITY_ROOT',
      projectRootConfigured || !process.env.ORCHESTRATOR_FINALIZATIONS_DIR ? projectRoot : path.dirname(finalizationsDir)),
    taskCreationsAuthorityRoot: root('taskCreationsAuthorityRoot', 'ORCHESTRATOR_TASK_CREATIONS_AUTHORITY_ROOT',
      projectRootConfigured || !process.env.ORCHESTRATOR_TASK_CREATIONS_DIR ? projectRoot : path.dirname(taskCreationsDir)),
    taskEditsAuthorityRoot: root('taskEditsAuthorityRoot', 'ORCHESTRATOR_TASK_EDITS_AUTHORITY_ROOT',
      projectRootConfigured || !process.env.ORCHESTRATOR_TASK_EDITS_DIR ? projectRoot : path.dirname(taskEditsDir))
  };
  if (Object.keys(roots).sort().join('\0') !== RUNTIME_ROOT_FIELDS.slice().sort().join('\0') ||
      Object.values(roots).some((value) => !path.isAbsolute(value))) {
    throw new ContractError('runtime root derivation is invalid');
  }
  return Object.freeze(roots);
}

function repoRelative(repoRoot, absolutePath) {
  const rel = path.relative(repoRoot, absolutePath).split(path.sep).join('/');
  if (!rel || rel === '..' || rel.startsWith('../') || path.isAbsolute(rel)) return path.basename(absolutePath);
  return rel;
}

function finding(code, severity, stem, paths, message, recovery, details) {
  const out = {
    code,
    severity,
    stem: stem || null,
    paths: Array.from(new Set((paths || []).filter(Boolean))).sort(),
    message,
    recovery
  };
  if (details && typeof details === 'object' && Object.keys(details).length) out.details = details;
  return out;
}

function findingOrder(a, b) {
  const ak = a.stem || '', bk = b.stem || '';
  if (ak !== bk) return ak < bk ? -1 : 1;
  if (a.code !== b.code) return a.code < b.code ? -1 : 1;
  const ap = (a.paths || []).join('\0'), bp = (b.paths || []).join('\0');
  if (ap !== bp) return ap < bp ? -1 : 1;
  return a.message < b.message ? -1 : a.message > b.message ? 1 : 0;
}

function safeIntegerId(stem) {
  const match = STEM_RE.exec(String(stem || ''));
  if (!match || stem.length > STEM_MAX) return null;
  try {
    const value = BigInt(match[1]);
    return value > 0n && value <= MAX_SAFE_ID && String(value) === match[1] ? Number(value) : null;
  } catch (_) { return null; }
}

function exactObjectFields(value, fields) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return false;
  const keys = Object.keys(value).sort();
  return keys.length === fields.length && keys.every((key, index) => key === fields[index]);
}

function boundedRuntimeString(value, max, nonempty = true) {
  return typeof value === 'string' && (!nonempty || value.length > 0) && value.length <= max && !/[\0\r\n]/.test(value);
}

function exactAllowedFields(value, required, optional) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return false;
  const keys = Object.keys(value);
  return required.every((key) => Object.prototype.hasOwnProperty.call(value, key)) &&
    keys.every((key) => required.includes(key) || optional.has(key));
}

function exactIso(value) {
  return typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/.test(value) &&
    Number.isFinite(Date.parse(value)) && new Date(value).toISOString() === value;
}

// Task documents allow either whole seconds or 1-3 fractional digits.
// Date.parse() alone is not sufficient: JavaScript normalizes invalid
// calendar values such as February 31 instead of rejecting them. Compare the
// parsed instant with the exact zero-padded UTC spelling so normalized dates,
// offsets and local-time strings cannot cross a lifecycle boundary.
function exactTaskInstant(value) {
  if (typeof value !== 'string') return false;
  const match = /^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.(\d{1,3}))?Z$/.exec(value);
  if (!match || !Number.isFinite(Date.parse(value))) return false;
  const canonical = match[1] + '.' + String(match[2] || '').padEnd(3, '0') + 'Z';
  return new Date(value).toISOString() === canonical;
}

function canonicalLockV1(value, stem) {
  if (!exactObjectFields(value, LOCK_FIELDS_V1) || value.version !== 1 || value.stem !== stem ||
      !['task-prep', 'orchestrator'].includes(value.stage) || !LOCK_RUN_ID_RE.test(String(value.runId || '')) ||
      !LOCK_SESSION_ID_RE.test(String(value.sessionId || '')) || !exactIso(value.startedAt) ||
      !exactObjectFields(value.owner, LOCK_OWNER_FIELDS_V1) || !LOCK_OWNER_KINDS.has(value.owner.kind) ||
      typeof value.owner.id !== 'string' || !value.owner.id || value.owner.id.length > 240 || /[\0\r\n]/.test(value.owner.id) ||
      !Number.isInteger(value.owner.pid) || value.owner.pid <= 0 || value.owner.pid > 0x7fffffff ||
      (value.owner.processStartId !== null && !LOCK_PROCESS_START_ID_RE.test(String(value.owner.processStartId || ''))) ||
      typeof value.owner.hostname !== 'string' || !value.owner.hostname || value.owner.hostname.length > 255 || /[\0\r\n]/.test(value.owner.hostname) ||
      value.owner.startedAt !== value.startedAt) return false;
  return true;
}

function sameStat(a, b) {
  return !!a && !!b && a.dev === b.dev && a.ino === b.ino && a.mode === b.mode && a.size === b.size &&
    a.mtimeNs === b.mtimeNs && a.ctimeNs === b.ctimeNs;
}

const TASK_FS_BOUNDARY = path.join(__dirname, 'anchored-task-fs.py');
const CANONICAL_REPO_ROOT = path.resolve(__dirname, '..', '..');
const DECIMAL_STAT_RE = /^(?:0|[1-9][0-9]*)$/;

function statFromProof(raw) {
  if (!raw || typeof raw !== 'object' || !DECIMAL_STAT_RE.test(String(raw.dev || '')) ||
      !DECIMAL_STAT_RE.test(String(raw.ino || '')) || !DECIMAL_STAT_RE.test(String(raw.mtimeNs || '')) ||
      !DECIMAL_STAT_RE.test(String(raw.ctimeNs || '')) || !Number.isSafeInteger(raw.mode) || raw.mode < 0 ||
      !Number.isSafeInteger(raw.size) || raw.size < 0 || !['file', 'directory', 'symlink', 'other'].includes(raw.kind)) {
    throw new ContractError('anchored filesystem helper returned a malformed stat proof');
  }
  const mode = raw.mode;
  const mtimeNs = String(raw.mtimeNs), ctimeNs = String(raw.ctimeNs);
  return Object.freeze({
    dev: String(raw.dev), ino: String(raw.ino), mode, size: raw.size, mtimeNs, ctimeNs,
    // Millisecond projections are presentation-only. Identity comparisons use
    // the exact decimal nanosecond fields above and never IEEE-754 dev/ino.
    mtimeMs: Number(BigInt(mtimeNs) / 1000000n),
    ctimeMs: Number(BigInt(ctimeNs) / 1000000n),
    isFile: () => (mode & 0o170000) === 0o100000,
    isDirectory: () => (mode & 0o170000) === 0o040000,
    isSymbolicLink: () => (mode & 0o170000) === 0o120000
  });
}

function pathWithin(authorityRoot, candidate) {
  const rel = path.relative(path.resolve(authorityRoot), path.resolve(candidate));
  return rel === '' || (rel !== '..' && !rel.startsWith('..' + path.sep) && !path.isAbsolute(rel));
}

function fixtureBoundaryAllowed(authorityRoot) {
  const fixtureRoot = process.env.TASK_FS_TEST_ROOT ? path.resolve(process.env.TASK_FS_TEST_ROOT) : null;
  if (!fixtureRoot || fixtureRoot === CANONICAL_REPO_ROOT || !pathWithin(fixtureRoot, authorityRoot)) return false;
  const tempRoot = path.resolve(os.tmpdir());
  return fixtureRoot !== tempRoot && pathWithin(tempRoot, fixtureRoot) && !pathWithin(CANONICAL_REPO_ROOT, fixtureRoot);
}

function anchoredBoundary(request, outputBytes = 1024 * 1024) {
  const envelope = Object.assign({}, request, {
    version: 1,
    canonicalRoot: CANONICAL_REPO_ROOT,
    fixture: fixtureBoundaryAllowed(request.authorityRoot || request.repoRoot)
  });
  const run = childProcess.spawnSync('python3', [TASK_FS_BOUNDARY], {
    input: JSON.stringify(envelope),
    encoding: 'utf8',
    maxBuffer: Math.max(1024 * 1024, outputBytes),
    timeout: 15000
  });
  if (run.error || run.status !== 0) {
    throw new ContractError('anchored filesystem helper failed: ' + String(run.error && run.error.message || run.stderr || 'unknown failure').slice(0, 500));
  }
  let value;
  try { value = JSON.parse(run.stdout); }
  catch (_) { throw new ContractError('anchored filesystem helper returned invalid JSON'); }
  if (!value || value.version !== 1 || typeof value.ok !== 'boolean') {
    throw new ContractError('anchored filesystem helper returned an invalid envelope');
  }
  if (!value.ok) {
    const code = value.error && value.error.code || 'BOUNDARY_FAILED';
    const error = ['DIRECTORY_CHANGED', 'ENTRY_CHANGED'].includes(code)
      ? new SnapshotRaceError(String(value.error && value.error.message || code))
      : code === 'TOTAL_BYTES_LIMIT'
        ? new ContractError('task corpus exceeds the total bounded read limit')
        : new Error(String(value.error && value.error.message || code));
    error.code = code === 'PATH_MISSING' ? 'ENOENT' : code;
    throw error;
  }
  return value.result;
}

function readAnchoredFile(options = {}) {
  const target = path.resolve(String(options.path || ''));
  const authorityRoot = path.resolve(String(options.authorityRoot || ''));
  const maxBytes = options.maxBytes;
  if (!options.path || !options.authorityRoot || !pathWithin(authorityRoot, target) ||
      !Number.isSafeInteger(maxBytes) || maxBytes < 1 || maxBytes > 32 * 1024 * 1024) {
    const error = new Error('anchored file read arguments are invalid');
    error.exitCode = 2;
    throw error;
  }
  const raw = anchoredBoundary({ action: 'read', path: target, authorityRoot, maxBytes },
    Math.ceil(maxBytes * 1.5) + 1024 * 1024);
  const fileStat = statFromProof(raw.stat);
  if (raw.unsafe === true) return { unsafe: true, tooLarge: false, stat: fileStat };
  if (raw.tooLarge === true) return { unsafe: false, tooLarge: true, stat: fileStat };
  if (typeof raw.rawBase64 !== 'string') throw new ContractError('anchored file read omitted bytes');
  const buffer = Buffer.from(raw.rawBase64, 'base64');
  if (buffer.length !== fileStat.size || buffer.length > maxBytes) {
    throw new SnapshotRaceError('anchored file byte count differs from its exact proof');
  }
  return { unsafe: false, tooLarge: false, stat: fileStat, buffer };
}

function observationFor(result = {}, options = {}) {
  const caller = /^[a-z][a-z0-9-]{0,39}$/.test(String(options.caller || ''))
    ? String(options.caller) : 'unknown';
  const durationRaw = result.stats && result.stats.durationMs;
  const durationMs = Number.isFinite(durationRaw) && durationRaw >= 0
    ? Math.min(Math.round(durationRaw), 60 * 60 * 1000) : 0;
  const thresholdRaw = options.slowThresholdMs;
  const slowThresholdMs = Number.isSafeInteger(thresholdRaw) && thresholdRaw >= 0 && thresholdRaw <= 60000
    ? thresholdRaw : 100;
  const findings = Array.isArray(result.findings) ? result.findings : [];
  const observationFindings = findings.slice(0, 100).map((item) => Object.freeze({
    code: String(item && item.code || 'UNKNOWN').slice(0, 80),
    severity: ['warning', 'error', 'blocker'].includes(item && item.severity) ? item.severity : 'error'
  }));
  const bounded = (value, max = 160) => typeof value === 'string' ? value.slice(0, max) : null;
  const ok = result.ok === true;
  const overallOk = result.overallOk === undefined ? ok : result.overallOk === true;
  return Object.freeze({
    version: 1,
    event: 'task-state-validation',
    caller,
    scope: bounded(result.scope),
    action: bounded(result.action),
    transition: bounded(result.transition),
    phase: bounded(result.phase),
    observedState: bounded(result.observedState),
    expectedState: bounded(result.expectedState),
    snapshotHash: bounded(result.snapshotHash, 96),
    durationMs,
    slowThresholdMs,
    slow: durationMs >= slowThresholdMs,
    scanMode: bounded(result.stats && result.stats.scanMode),
    taskBodyReads: Number.isSafeInteger(result.stats && result.stats.taskBodyReads)
      ? Math.max(0, result.stats.taskBodyReads) : 0,
    architectureStatus: bounded(result.derivedState && result.derivedState.arch && result.derivedState.arch.status),
    findings: Object.freeze(observationFindings),
    findingsTruncated: findings.length > 100,
    result: overallOk ? 'valid' : 'invalid',
    ok,
    overallOk
  });
}

const OBSERVATION_FIELDS = Object.freeze([
  'action', 'architectureStatus', 'caller', 'durationMs', 'event', 'expectedState',
  'findings', 'findingsTruncated', 'observedState', 'ok', 'overallOk', 'phase',
  'result', 'scanMode', 'scope', 'slow', 'slowThresholdMs', 'snapshotHash',
  'taskBodyReads', 'transition', 'version'
]);

// Strict privacy boundary for relaying an observation through a parent
// process.  Never forward the original object: accepting only the complete
// public schema and rebuilding it prevents an otherwise valid child event
// from smuggling task prose, paths, prompts, or secrets in unknown fields.
function projectObservation(value, constraints = {}) {
  if (!value || typeof value !== 'object' || Array.isArray(value) ||
      Object.keys(value).sort().join('\0') !== OBSERVATION_FIELDS.join('\0') ||
      value.version !== 1 || value.event !== 'task-state-validation' ||
      !/^[a-z][a-z0-9-]{0,39}$/.test(String(value.caller || '')) ||
      (value.scope !== null && value.scope !== 'all' && safeIntegerId(value.scope) === null) ||
      (value.action !== null && !/^[a-z][a-z0-9-]{0,39}$/.test(String(value.action || ''))) ||
      (value.transition !== null && !ALLOWED_TRANSITIONS.has(value.transition)) ||
      ![null, 'pre', 'post'].includes(value.phase) ||
      ![null, ...STATES].includes(value.observedState) ||
      ![null, ...STATES].includes(value.expectedState) ||
      (value.snapshotHash !== null && !HASH_RE.test(String(value.snapshotHash || ''))) ||
      !Number.isSafeInteger(value.durationMs) || value.durationMs < 0 || value.durationMs > 60 * 60 * 1000 ||
      !Number.isSafeInteger(value.slowThresholdMs) || value.slowThresholdMs < 0 || value.slowThresholdMs > 60000 ||
      typeof value.slow !== 'boolean' || value.slow !== (value.durationMs >= value.slowThresholdMs) ||
      ![null, 'full', 'stem-closure'].includes(value.scanMode) ||
      !Number.isSafeInteger(value.taskBodyReads) || value.taskBodyReads < 0 || value.taskBodyReads > 10000000 ||
      (value.architectureStatus !== null && !/^[a-z][a-z0-9-]{0,79}$/.test(String(value.architectureStatus || ''))) ||
      !Array.isArray(value.findings) || value.findings.length > 100 ||
      typeof value.findingsTruncated !== 'boolean' || !['valid', 'invalid'].includes(value.result) ||
      typeof value.ok !== 'boolean' || typeof value.overallOk !== 'boolean' ||
      value.result !== (value.overallOk ? 'valid' : 'invalid')) return null;
  if (Object.prototype.hasOwnProperty.call(constraints, 'caller') && value.caller !== constraints.caller) return null;
  if (Object.prototype.hasOwnProperty.call(constraints, 'scope') && value.scope !== constraints.scope) return null;
  if (Object.prototype.hasOwnProperty.call(constraints, 'action') && value.action !== constraints.action) return null;
  if (Object.prototype.hasOwnProperty.call(constraints, 'transition') && value.transition !== constraints.transition) return null;
  const findings = [];
  for (const item of value.findings) {
    if (!item || typeof item !== 'object' || Array.isArray(item) ||
        Object.keys(item).sort().join('\0') !== 'code\0severity' ||
        !/^[A-Za-z0-9_.:-]{1,80}$/.test(String(item.code || '')) ||
        !['warning', 'error', 'blocker'].includes(item.severity)) return null;
    findings.push(Object.freeze({ code: item.code, severity: item.severity }));
  }
  return Object.freeze({
    version: 1,
    event: 'task-state-validation',
    caller: value.caller,
    scope: value.scope,
    action: value.action,
    transition: value.transition,
    phase: value.phase,
    observedState: value.observedState,
    expectedState: value.expectedState,
    snapshotHash: value.snapshotHash,
    durationMs: value.durationMs,
    slowThresholdMs: value.slowThresholdMs,
    slow: value.slow,
    scanMode: value.scanMode,
    taskBodyReads: value.taskBodyReads,
    architectureStatus: value.architectureStatus,
    findings: Object.freeze(findings),
    findingsTruncated: value.findingsTruncated,
    result: value.result,
    ok: value.ok,
    overallOk: value.overallOk
  });
}

function projectObservationStream(raw, constraints = {}, options = {}) {
  const text = typeof raw === 'string' ? raw : '';
  const maximum = Number.isSafeInteger(options.maxBytes) && options.maxBytes >= 1 && options.maxBytes <= 1024 * 1024
    ? options.maxBytes : 256 * 1024;
  const expectedCount = options.expectedCount === undefined || options.expectedCount === null
    ? null : options.expectedCount;
  if (expectedCount !== null && (!Number.isSafeInteger(expectedCount) || expectedCount < 0 || expectedCount > 100)) {
    throw new Error('observation expectedCount is invalid');
  }
  const phases = Array.isArray(options.syntheticPhases) ? options.syntheticPhases.slice() : [];
  if (phases.some((phase) => !['pre', 'post'].includes(phase)) ||
      (expectedCount !== null && phases.length !== expectedCount)) {
    throw new Error('observation synthetic phases are invalid');
  }
  let malformed = Buffer.byteLength(text, 'utf8') > maximum;
  const projected = [];
  if (!malformed) {
    for (const line of text.split(/\r?\n/)) {
      if (!line.startsWith('[task-state] ')) continue;
      let value;
      try { value = JSON.parse(line.slice('[task-state] '.length)); }
      catch (_) { malformed = true; continue; }
      const event = projectObservation(value, constraints);
      if (event) projected.push(event);
      else malformed = true;
    }
  }
  const fallbackCode = /^[A-Za-z0-9_.:-]{1,80}$/.test(String(options.fallbackCode || ''))
    ? String(options.fallbackCode) : 'TASK_STATE_OBSERVATION_INVALID';
  const fallbackPhase = options.syntheticPhase === undefined ? 'post' : options.syntheticPhase;
  if (!['pre', 'post'].includes(fallbackPhase)) throw new Error('observation synthetic phase is invalid');
  const synthetic = (phase) => {
    const event = observationFor({
      version: 1,
      ok: false,
      overallOk: false,
      scope: Object.prototype.hasOwnProperty.call(constraints, 'scope') ? constraints.scope : null,
      action: Object.prototype.hasOwnProperty.call(constraints, 'action') ? constraints.action : null,
      transition: Object.prototype.hasOwnProperty.call(constraints, 'transition') ? constraints.transition : null,
      phase,
      observedState: null,
      expectedState: null,
      snapshotHash: null,
      findings: [{ code: fallbackCode, severity: 'blocker' }],
      stats: { durationMs: 0, scanMode: 'full', taskBodyReads: 0 }
    }, { caller: constraints.caller, slowThresholdMs: 100 });
    const projected = projectObservation(event, constraints);
    if (!projected) throw new Error('observation constraints are invalid');
    return projected;
  };
  if (expectedCount !== null && (malformed || projected.length !== expectedCount)) {
    return Object.freeze(phases.map(synthetic));
  }
  if (malformed) projected.push(synthetic(fallbackPhase));
  return Object.freeze(projected);
}

function assertFixtureReadHookAllowed(testReadHook, tasksDir, repoRoot) {
  if (!testReadHook) return null;
  if (typeof testReadHook !== 'function') throw new ContractError('test read hook must be a function');
  const canonicalTasksDir = path.resolve(__dirname);
  const canonicalRepoRoot = path.resolve(__dirname, '..', '..');
  const resolvedTasksDir = path.resolve(tasksDir);
  const resolvedRepoRoot = path.resolve(repoRoot);
  const tempRelative = path.relative(path.resolve(os.tmpdir()), resolvedRepoRoot);
  const isTempFixture = tempRelative !== '' && tempRelative !== '..' && !tempRelative.startsWith('..' + path.sep) && !path.isAbsolute(tempRelative);
  if (resolvedTasksDir === canonicalTasksDir || resolvedRepoRoot === canonicalRepoRoot || !isTempFixture) {
    throw new ContractError('test read hook is restricted to isolated temporary fixtures');
  }
  return testReadHook;
}

function decodeUtf8(buffer) {
  try { return new TextDecoder('utf-8', { fatal: true }).decode(buffer); }
  catch (_) { return null; }
}

// Markdown structural whitespace is ASCII space/tab. String#trim also drops
// VT/FF/NBSP and can silently canonicalize a forged heading, field or edge.
function horizontalTrim(value) {
  return String(value == null ? '' : value).replace(/^[ \t]+|[ \t]+$/g, '');
}

function horizontalBlank(value) { return /^[ \t]*$/.test(String(value == null ? '' : value)); }

// Design extraction and lifecycle parsing must share one CommonMark view;
// otherwise a fenced/HTML heading could be interpreted differently by the
// validator and the downstream Figma gates.
function structuralText(text) {
  return designParser.structuralText(text);
}

function parseHeading(text) {
  const structural = structuralText(text);
  const first = structural.split('\n').find((line) => !horizontalBlank(line));
  if (!first) return { issue: 'missing' };
  const atx = designParser.parseAtxHeadingLine(first);
  const raw = horizontalTrim(first);
  const match = atx && atx.level === 1
    ? /^TASK ([1-9][0-9]*)[ \t]+([—-])[ \t]+(.+?)[ \t]*$/.exec(atx.name)
    : null;
  if (!match) return { issue: 'noncanonical', raw };
  let number;
  try {
    const big = BigInt(match[1]);
    if (big > MAX_SAFE_ID) return { issue: 'noncanonical', raw };
    number = Number(big);
  } catch (_) { return { issue: 'noncanonical', raw }; }
  return { number, title: horizontalTrim(match[3]), canonicalDash: match[2] === '—', raw };
}

function h2Sections(text) {
  const scanned = designParser.scanAtxHeadings(text, 2);
  const structural = scanned.structural;
  const matches = scanned.headings;
  const out = [];
  for (let i = 0; i < matches.length; i++) {
    out.push({
      name: matches[i].name,
      start: matches[i].start,
      // Preserve the previous section-body projection: it begins at the end
      // of the heading line (and therefore includes its newline) and stops at
      // the next H2's indentation. Only heading recognition is broadened.
      body: structural.slice(matches[i].headEnd, matches[i + 1] ? matches[i + 1].start : structural.length)
    });
  }
  return out;
}

function sectionMap(text) {
  const map = new Map();
  for (const section of h2Sections(text)) {
    const key = section.name.toLowerCase();
    if (!map.has(key)) map.set(key, []);
    map.get(key).push(section);
  }
  return map;
}

const TOP_LEVEL_BULLET_RE = /^ {0,3}-[ \t]+(.+?)[ \t]*$/;

function bullets(body) {
  return String(body || '').split('\n').map((line) => TOP_LEVEL_BULLET_RE.exec(line)).filter(Boolean).map((m) => horizontalTrim(m[1]));
}

function strictBullets(body) {
  const values = [];
  let residue = false;
  for (const line of String(body || '').split('\n')) {
    if (horizontalBlank(line)) continue;
    const match = TOP_LEVEL_BULLET_RE.exec(line);
    if (!match) residue = true;
    else values.push(horizontalTrim(match[1]));
  }
  return { values, residue };
}

function boundedDecimal(value, minimum = 0) {
  if (typeof value !== 'string' || !/^[0-9]+$/.test(value) || value.length > 16) return null;
  try {
    const parsed = BigInt(value);
    return parsed >= BigInt(minimum) && parsed <= MAX_SAFE_ID ? Number(parsed) : null;
  } catch (_) { return null; }
}

function parseDependencies(text) {
  const map = sectionMap(text);
  const sections = [];
  for (const [name, values] of map.entries()) if (/^depends on(?:[ \t]+\(optional\))?$/.test(name)) sections.push(...values);
  const deps = [], invalid = [];
  for (const section of sections) {
    const lines = String(section.body).split('\n');
    let lastContentIndex = -1;
    for (let index = 0; index < lines.length; index++) if (!horizontalBlank(lines[index])) lastContentIndex = index;
    for (let index = 0; index < lines.length; index++) {
      const line = lines[index];
      if (horizontalBlank(line)) continue;
      if (horizontalTrim(line) === '---') {
        // The final Outcome separator may be the last non-empty line of a
        // dependency section projection. A separator before more content is
        // residue, never an instruction to stop parsing: otherwise a real
        // dependency below it disappears from run admission and INDEX.
        if (index !== lastContentIndex) invalid.push('---');
        continue;
      }
      const bullet = TOP_LEVEL_BULLET_RE.exec(line);
      if (!bullet) {
        invalid.push(line.trim().slice(0, 160));
        continue;
      }
      const body = horizontalTrim(bullet[1]);
      const match = /^`?(TASK_[1-9][0-9]*_[A-Za-z0-9_]+)`?(?:[ \t]*[—-][ \t]*(.*))?$/.exec(body);
      const note = match && match[2] || '';
      // A dependency bullet owns exactly one edge. A second task-looking
      // token in its note is ambiguity, never prose that may be discarded.
      if (!match || safeIntegerId(match[1]) === null || /TASK_[0-9]+_/i.test(note.normalize('NFKC'))) invalid.push(body.slice(0, 160));
      else deps.push(match[1]);
    }
  }
  return { deps, invalid, sectionCount: sections.length };
}

function parseLineage(text) {
  const map = sectionMap(text);
  const values = map.get('origin') || [];
  const parents = [], invalid = [];
  for (const section of values) {
    const lines = String(section.body).split('\n');
    let lastContentIndex = -1;
    for (let index = 0; index < lines.length; index++) if (!horizontalBlank(lines[index])) lastContentIndex = index;
    for (let index = 0; index < lines.length; index++) {
      const line = lines[index];
      if (horizontalBlank(line)) continue;
      if (horizontalTrim(line) === '---') {
        if (index !== lastContentIndex) invalid.push('---');
        continue;
      }
      const bullet = TOP_LEVEL_BULLET_RE.exec(line);
      if (!bullet) {
        invalid.push(line.trim().slice(0, 160));
        continue;
      }
      const body = horizontalTrim(bullet[1]);
      const match = /^(?:split[ \t]+from|from|parent)[ \t]*:?[ \t]*`?(TASK_[1-9][0-9]*_[A-Za-z0-9_]+)`?[ \t]*$/i.exec(body);
      if (!match || safeIntegerId(match[1]) === null) invalid.push(body.slice(0, 160));
      else parents.push(match[1]);
    }
  }
  return { parents, invalid, sectionCount: values.length };
}

function hasOutcome(text) {
  return h2Sections(text).some((section) => section.name === 'Outcome');
}

function parsePending(text) {
  const normalized = String(text || '');
  const lines = normalized.split('\n');
  const out = { fields: Object.create(null), duplicateFields: [], questions: [], errors: [] };
  if (normalized.startsWith('\uFEFF') || normalized.includes('\r')) {
    out.errors.push('noncanonical-text');
    return out;
  }
  if (horizontalTrim(lines[0]) !== '---') { out.errors.push('frontmatter-open'); return out; }
  let end = -1;
  for (let i = 1; i < lines.length; i++) if (horizontalTrim(lines[i]) === '---') { end = i; break; }
  if (end < 0) { out.errors.push('frontmatter-close'); return out; }
  for (let i = 1; i < end; i++) {
    if (horizontalBlank(lines[i]) || /^[ \t]*#/.test(lines[i])) continue;
    const match = /^([A-Za-z][A-Za-z0-9]*)[ \t]*:[ \t]*(.*?)[ \t]*$/.exec(lines[i]);
    if (!match) { out.errors.push('frontmatter-line'); continue; }
    if (Object.prototype.hasOwnProperty.call(out.fields, match[1])) out.duplicateFields.push(match[1]);
    out.fields[match[1]] = match[2];
  }
  const body = lines.slice(end + 1).join('\n');
  const structural = structuralText(body);
  for (const line of structural.split('\n')) {
    if (/^[ \t]*##(?:[ \t]|$)/.test(line) && !/^##[ \t]+Q[0-9]+[ \t]+[—-][ \t]+.+?[ \t]*$/.test(line)) {
      out.errors.push('question-heading');
    }
  }
  const qRe = /^##[ \t]+Q([0-9]+)[ \t]+[—-][ \t]+(.+?)[ \t]*$/gm;
  const matches = [];
  let match;
  while ((match = qRe.exec(structural)) !== null) matches.push({ id: Number(match[1]), title: horizontalTrim(match[2]), start: match.index, bodyStart: qRe.lastIndex });
  for (let i = 0; i < matches.length; i++) {
    const qBody = structural.slice(matches[i].bodyStart, matches[i + 1] ? matches[i + 1].start : structural.length);
    // Structural tokens are physical-line contracts. `\s` under multiline
    // mode can consume `\n`, so a split `**Type**:\nchoice` or
    // `###\nAnswer` must never become a valid sidecar token.
    const types = Array.from(qBody.matchAll(/^[ \t]*\*\*Type\*\*[ \t]*:[ \t]*(\S+)[ \t]*$/gm)).map((m) => m[1]);
    const options = Array.from(qBody.matchAll(/^[ \t]*\*\*Options\*\*[ \t]*:[ \t]*(.+?)[ \t]*$/gm)).map((m) => m[1]);
    const answerCount = Array.from(qBody.matchAll(/^###[ \t]+Answer[ \t]*$/gm)).length;
    out.questions.push({ id: matches[i].id, title: matches[i].title, types, options, answerCount });
  }
  return out;
}

const TASK_QUESTIONS_SECTION = 'Questions';
const TASK_QUESTION_HEADING_RE = /^###[ \t]+Q([0-9]+)[ \t]+[—-][ \t]+(.+?)[ \t]*$/;
const TASK_ANSWER_HEADING_RE = /^####[ \t]+Answer[ \t]*$/;
const TASK_QUESTION_TYPE_RE = /^[ \t]*\*\*Type\*\*[ \t]*:[ \t]*(\S+)[ \t]*$/;
const TASK_QUESTION_OPTIONS_RE = /^[ \t]*\*\*Options\*\*[ \t]*:[ \t]*(.+?)[ \t]*$/;
const TASK_QUESTION_TYPES = Object.freeze(['text', 'choice', 'multiselect']);
const MAX_TASK_QUESTIONS = 99;
const ANSWER_BODY_PLACEHOLDER = '\u0000ANSWER-BODY\u0000';

function lineRecords(text) {
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

// Questions asked while a task is already running live inside the todo body.
// A `pending/<stem>.questions.md` sidecar cannot exist next to
// `todo/<stem>.md` — classify() calls that pair corrupt — and parsePending
// hard-requires frontmatter, so the in-body rail owns a fragment grammar one
// heading level deeper: `## Questions` > `### Q<N> — …` > `#### Answer`.
// Everything is read through the CommonMark structural view and addressed by
// physical line index, so a fenced or HTML-masked decoy can never forge a
// question and the raw answer bytes stay recoverable.
function parseTaskQuestions(text) {
  const raw = String(text || '');
  const out = {
    present: false, sectionCount: 0, errors: [], questions: [],
    sectionStart: -1, sectionEnd: -1
  };
  if (raw.startsWith('\uFEFF') || raw.includes('\r')) { out.errors.push('noncanonical-text'); return out; }
  const structuralLines = structuralText(raw).split('\n');
  const rawLines = lineRecords(raw);
  if (structuralLines.length !== rawLines.length) { out.errors.push('noncanonical-text'); return out; }
  const headings = structuralLines.map((line) => designParser.parseAtxHeadingLine(line));
  const sectionLines = [];
  for (let index = 0; index < headings.length; index++) {
    if (headings[index] && headings[index].level === 2 && headings[index].name === TASK_QUESTIONS_SECTION) sectionLines.push(index);
  }
  out.sectionCount = sectionLines.length;
  if (!sectionLines.length) return out;
  if (sectionLines.length > 1) { out.errors.push('section-duplicate'); return out; }
  const sectionLine = sectionLines[0];
  let endLine = structuralLines.length;
  for (let index = sectionLine + 1; index < headings.length; index++) {
    if (headings[index] && headings[index].level <= 2) { endLine = index; break; }
  }
  out.present = true;
  out.sectionStart = rawLines[sectionLine].start;
  out.sectionEnd = endLine < rawLines.length ? rawLines[endLine].start : raw.length;
  const questionLines = [];
  for (let index = sectionLine + 1; index < endLine; index++) {
    const heading = headings[index];
    if (!heading) continue;
    if (heading.level === 3) {
      if (TASK_QUESTION_HEADING_RE.test(structuralLines[index])) questionLines.push(index);
      else out.errors.push('question-heading');
      continue;
    }
    if (heading.level === 4) {
      if (!TASK_ANSWER_HEADING_RE.test(structuralLines[index])) out.errors.push('answer-heading');
      continue;
    }
    out.errors.push('unexpected-heading');
  }
  for (let index = 0; index < questionLines.length; index++) {
    const questionLine = questionLines[index];
    const blockEnd = index + 1 < questionLines.length ? questionLines[index + 1] : endLine;
    const match = TASK_QUESTION_HEADING_RE.exec(structuralLines[questionLine]);
    const types = []; const options = []; const answerLines = [];
    for (let line = questionLine + 1; line < blockEnd; line++) {
      const typeMatch = TASK_QUESTION_TYPE_RE.exec(structuralLines[line]);
      if (typeMatch) types.push(typeMatch[1]);
      const optionMatch = TASK_QUESTION_OPTIONS_RE.exec(structuralLines[line]);
      if (optionMatch) options.push(optionMatch[1]);
      if (TASK_ANSWER_HEADING_RE.test(structuralLines[line])) answerLines.push(line);
    }
    // The Answer heading's own bytes and line ending belong to the immutable
    // skeleton; everything until the next real question is the answer body.
    const answerStart = answerLines.length === 1 ? rawLines[answerLines[0]].contentEnd : -1;
    const answerEnd = blockEnd < rawLines.length ? rawLines[blockEnd].start : raw.length;
    out.questions.push({
      id: Number(match[1]),
      title: horizontalTrim(match[2]),
      types,
      options,
      answerCount: answerLines.length,
      blockStart: rawLines[questionLine].start,
      blockEnd: answerEnd,
      answerStart,
      answerEnd,
      answer: answerStart < 0 ? '' : raw.slice(answerStart, answerEnd).trim()
    });
  }
  return out;
}

function taskQuestionsIssue(parsed) {
  if (parsed && parsed.sectionCount > 1) return 'task body must contain exactly one Questions section';
  if (!parsed || !parsed.present) return 'task body has no canonical Questions section';
  if (parsed.errors.length) return 'Questions section syntax is not canonical';
  if (!parsed.questions.length) return 'Questions section contains no questions';
  if (parsed.questions.length > MAX_TASK_QUESTIONS) return 'Questions section exceeds the bounded question count';
  const ids = new Set();
  let previous = 0;
  for (const question of parsed.questions) {
    if (!Number.isSafeInteger(question.id) || question.id < 1 || ids.has(question.id) || question.id <= previous) {
      return 'question ids must be positive, unique and strictly increasing';
    }
    ids.add(question.id);
    previous = question.id;
    if (!question.title) return 'question title is empty';
    if (question.types.length !== 1 || !TASK_QUESTION_TYPES.includes(question.types[0])) {
      return 'question type must be exactly one text, choice or multiselect line';
    }
    if (question.answerCount !== 1) return 'every question must contain exactly one Answer section';
    if (question.types[0] === 'text') {
      if (question.options.length) return 'text question must not declare Options';
    } else {
      if (question.options.length !== 1) return 'choice question must declare exactly one Options line';
      const values = question.options[0].split(',').map((value) => horizontalTrim(value)).filter(Boolean);
      const keys = values.map((value) => value.normalize('NFC').toLowerCase());
      if (values.length < 2 || new Set(keys).size !== keys.length) {
        return 'choice question options must list at least two unique values';
      }
    }
  }
  return null;
}

// An answer body is free text, so it can leave a fence, HTML block or HTML
// comment open at EOF. That silently masks every later heading — the next
// question round, and eventually the Outcome appendix — so a write is only
// canonical when a probe heading appended to the body still reads as one.
function taskBodyStructureOpen(text) {
  var probe = 'OrchestratorStructureProbe';
  var lines = structuralText(String(text || '') + '\n## ' + probe + '\n').split('\n');
  // Read the probe at its exact index. Scanning back for the last heading would
  // let a body that already ends with a literal probe heading vouch for a
  // container opened after it.
  var heading = designParser.parseAtxHeadingLine(lines[lines.length - 2]);
  return !(heading && heading.level === 2 && heading.name === probe);
}

function taskQuestionsIdentity(parsed) {
  return (parsed && parsed.questions || []).map((question) => ({
    id: question.id,
    title: question.title,
    types: question.types.slice(),
    options: question.options.slice(),
    answerCount: question.answerCount
  }));
}

function taskQuestionsProjection(text) {
  const raw = String(text || '');
  const parsed = parseTaskQuestions(raw);
  if (taskQuestionsIssue(parsed)) return null;
  const skeleton = []; const answers = [];
  let cursor = 0;
  for (const question of parsed.questions) {
    if (question.answerStart < cursor || question.answerEnd < question.answerStart) return null;
    skeleton.push(raw.slice(cursor, question.answerStart), ANSWER_BODY_PLACEHOLDER);
    answers.push(raw.slice(question.answerStart, question.answerEnd));
    cursor = question.answerEnd;
  }
  skeleton.push(raw.slice(cursor));
  return { skeleton: skeleton.join(''), answers };
}

function parseTodo(text) {
  const map = sectionMap(text);
  const deps = parseDependencies(text);
  const lineage = parseLineage(text);
  const acceptanceSections = map.get('acceptance') || [];
  let acceptance = { mode: 'missing', automated: [], manual: [], invalid: [] };
  if (acceptanceSections.length === 1) {
    const body = acceptanceSections[0].body;
    // Reuse the CommonMark structural scanner rather than a multiline `\s`
    // regex: acceptance subsection names cannot borrow bytes from the next
    // physical line, and fenced/HTML/closing-hash semantics stay canonical.
    const h3s = designParser.scanAtxHeadings(body, 3).headings.map((heading) => ({
      name: heading.name.toLowerCase(),
      start: heading.start,
      bodyStart: heading.headEnd,
    }));
    if (!h3s.length) {
      const parsed = strictBullets(body);
      acceptance = { mode: 'invalid', automated: parsed.values, manual: [], invalid: ['missing-subsections'].concat(parsed.residue ? ['non-bullet-content'] : []) };
    } else {
      acceptance = { mode: 'structured', automated: [], manual: [], invalid: [] };
      const direct = strictBullets(body.slice(0, h3s[0].start));
      if (direct.values.length || direct.residue) acceptance.invalid.push('content-before-subsections');
      const seen = new Map();
      for (let i = 0; i < h3s.length; i++) {
        const parsed = strictBullets(body.slice(h3s[i].bodyStart, h3s[i + 1] ? h3s[i + 1].start : body.length));
        seen.set(h3s[i].name, (seen.get(h3s[i].name) || 0) + 1);
        if (parsed.residue) acceptance.invalid.push('non-bullet-content:' + h3s[i].name);
        if (h3s[i].name === 'automated') acceptance.automated.push(...parsed.values);
        else if (h3s[i].name === 'manual') {
          acceptance.manual.push(...parsed.values);
          if (!parsed.values.length) acceptance.invalid.push('empty-manual');
        }
        else acceptance.invalid.push('unknown-subsection:' + h3s[i].name);
      }
      for (const name of ['automated', 'manual']) if ((seen.get(name) || 0) > 1) acceptance.invalid.push('duplicate-subsection:' + name);
    }
  }
  return { map, deps, lineage, acceptance };
}

function automationAnchored(value) {
  const text = String(value || '');
  return /\.\/?[A-Za-z0-9_.-]+(?:\/[A-Za-z0-9_.*-]+)+/.test(text) ||
    /(?:^|[\s(])(?:[A-Za-z0-9_.-]+\/)+[A-Za-z0-9_.*-]+/.test(text) ||
    /\.\/gradlew\s+:[A-Za-z0-9:_-]+/.test(text) ||
    /\b[A-Z][A-Za-z0-9_]*\.[A-Za-z_][A-Za-z0-9_]*\b/.test(text) ||
    /`[^`\n]+`/.test(text);
}

function loadOutcomeShape(outcomeShapePath, bytes) {
  let parsed;
  try { parsed = JSON.parse(bytes === undefined ? fs.readFileSync(outcomeShapePath, 'utf8') : bytes.toString('utf8')); }
  catch (error) { throw new ContractError('required outcome contract is unreadable or malformed'); }
  for (const key of ['statusValid', 'reviewerValid', 'headings', 'acceptanceVerdicts',
    'buildGateVerdicts', 'runtimeGates', 'runtimeResults', 'followUpColumns', 'fileChanges']) {
    if (!Array.isArray(parsed[key]) || !parsed[key].length || parsed[key].some((v) => typeof v !== 'string' || !v)) {
      throw new ContractError('outcome contract field ' + key + ' is invalid');
    }
  }
  return parsed;
}

function buildGatesValid(body, shape) {
  const parsed = strictBullets(body);
  if (parsed.residue || !parsed.values.length) return false;
  if (parsed.values.length === 1 && parsed.values[0].toLowerCase() === 'none') return true;
  if (parsed.values.some((value) => value.toLowerCase() === 'none')) return false;
  return parsed.values.every((value) => {
    const match = /^`([^`\r\n]+)`[ \t]+—[ \t]+([a-z]+)(?:[ \t]+\(([^()\r\n]{1,200})\))?$/.exec(value);
    return !!match && shape.buildGateVerdicts.includes(match[2]) && (match[2] === 'skipped') === !!match[3];
  });
}

function runtimeVerifyValid(body, shape) {
  const parsed = strictBullets(body);
  if (parsed.residue || parsed.values.length !== 2) return false;
  const gate = /^Gate:[ \t]+([a-z]+)(?:[ \t]+\(([^()\r\n]{1,200})\))?$/.exec(parsed.values[0]);
  const result = /^Result:[ \t]+(pass|fail|n\/a)[ \t]+—[ \t]+([^\r\n]{1,500})$/.exec(parsed.values[1]);
  if (!gate || !shape.runtimeGates.includes(gate[1]) || (gate[1] === 'ran') === !!gate[2] ||
      !result || !shape.runtimeResults.includes(result[1])) return false;
  return (gate[1] === 'ran') === (result[1] === 'pass' || result[1] === 'fail');
}

function sectionBullets(body) {
  const parsed = strictBullets(body);
  if (parsed.residue || !parsed.values.length) return null;
  if (parsed.values.length === 1 && parsed.values[0].toLowerCase() === 'none') return [];
  if (parsed.values.some((value) => value.toLowerCase() === 'none')) return null;
  return parsed.values;
}

function safeOutcomeFilePath(value) {
  const pieces = String(value || '').split('/');
  return !!value && value.length <= 300 && !value.includes('\\') && !value.includes('\0') &&
    !path.posix.isAbsolute(value) && !/^[A-Za-z][A-Za-z0-9+.-]*:/.test(value) &&
    value === path.posix.normalize(value) && !value.endsWith('/') &&
    !pieces.includes('') && !pieces.includes('.') && !pieces.includes('..');
}

function outcomeStructure(text) {
  const source = String(text == null ? '' : text);
  if (source.startsWith('\uFEFF') || source.includes('\r')) {
    return { structural: '', lastSep: -1, headings: [], outcomeHeadings: [], anchored: null, noncanonicalText: true };
  }
  const scanned = designParser.scanAtxHeadings(source, 2);
  const structural = scanned.structural;
  let lastSep = -1;
  for (const match of structural.matchAll(/^---[ \t]*$/gm)) lastSep = match.index;
  const outcomeHeadings = scanned.headings.filter((heading) => heading.name === 'Outcome');
  let anchored = null;
  if (lastSep >= 0) {
    for (const heading of outcomeHeadings) {
      if (heading.start <= lastSep) continue;
      const prefix = structural.slice(lastSep, heading.start);
      if (/^---[ \t]*\n(?:[ \t]*\n)*$/.test(prefix)) {
        anchored = heading;
        break;
      }
    }
  }
  return { structural, lastSep, headings: scanned.headings, outcomeHeadings, anchored };
}

function outcomeAppendixStart(text) {
  const parsed = outcomeStructure(text);
  return parsed.anchored ? parsed.lastSep : -1;
}

function parseOutcome(text, shape) {
  const parsedStructure = outcomeStructure(text);
  const { structural, lastSep, outcomeHeadings, anchored } = parsedStructure;
  const result = { valid: false, status: 'malformed', errors: [], sections: Object.create(null), files: [], acceptance: [] };
  if (parsedStructure.noncanonicalText) { result.errors.push('noncanonical-text'); return result; }
  if (lastSep < 0) { result.errors.push('separator'); return result; }
  if (!anchored) { result.errors.push('heading-anchor'); return result; }
  if (outcomeHeadings.length !== 1) result.errors.push('duplicate-outcome');
  if (parsedStructure.headings.some((heading) => heading.start > anchored.start && heading.name !== 'Outcome')) {
    result.errors.push('unexpected-h2');
  }
  const body = structural.slice(anchored.headEnd);
  // Keep every token on its physical line. In multiline mode, `\s` also
  // consumes newlines and turns a missing field/heading after many blank
  // lines into quadratic backtracking.
  const subRe = /^###[ \t]+(\S.*?)[ \t]*$/gm;
  const subs = [];
  let match;
  while ((match = subRe.exec(body)) !== null) subs.push({ name: horizontalTrim(match[1]), start: match.index, bodyStart: subRe.lastIndex });
  const expectedSections = shape.headings.concat(subs.length === shape.headings.length + 1 ? ['Execution log'] : []);
  if (subs.length !== expectedSections.length || subs.some((sub, index) => sub.name !== expectedSections[index])) {
    result.errors.push('section-order');
  }
  const head = body.slice(0, subs.length ? subs[0].start : body.length);
  for (let i = 0; i < subs.length; i++) {
    const name = subs[i].name;
    if (Object.prototype.hasOwnProperty.call(result.sections, name)) result.errors.push('duplicate-section:' + name);
    result.sections[name] = body.slice(subs[i].bodyStart, subs[i + 1] ? subs[i + 1].start : body.length);
  }
  const field = (name) => {
    const lineRe = new RegExp('^[ \\t]*\\*\\*' + name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '\\*\\*[ \\t]*:[ \\t]*(.+)[ \\t]*$');
    const rows = [];
    for (const line of head.split('\n')) {
      const row = lineRe.exec(line);
      if (row) rows.push(row);
    }
    if (rows.length !== 1) result.errors.push((rows.length ? 'duplicate-field:' : 'missing-field:') + name);
    return rows.length === 1 ? horizontalTrim(rows[0][1]) : '';
  };
  const status = field('Status').toLowerCase();
  const completedAt = field('Completed at');
  const reviewer = field('Reviewer').toLowerCase();
  const iterations = field('Review iterations');
  if (!shape.statusValid.includes(status)) result.errors.push('status');
  if (!shape.reviewerValid.includes(reviewer)) result.errors.push('reviewer');
  if (!exactTaskInstant(completedAt)) result.errors.push('completed-at');
  if (boundedDecimal(iterations, 0) === null) result.errors.push('review-iterations');
  for (const heading of shape.headings) {
    if (!Object.prototype.hasOwnProperty.call(result.sections, heading) || !String(result.sections[heading]).trim()) result.errors.push('section:' + heading);
  }
  if (result.sections['Build gates'] && !buildGatesValid(result.sections['Build gates'], shape)) result.errors.push('build-gates');
  if (result.sections['Runtime verify'] && !runtimeVerifyValid(result.sections['Runtime verify'], shape)) result.errors.push('runtime-verify');
  if (result.sections['Caveats']) {
    const caveats = sectionBullets(result.sections['Caveats']);
    if (caveats === null || caveats.some((value) => Array.from(value).length > 120 || /[\u0000-\u001f\u007f]/.test(value))) {
      result.errors.push('caveats');
    }
  }
  if (result.sections['Follow-ups']) {
    const followUps = sectionBullets(result.sections['Follow-ups']);
    if (followUps === null || followUps.some((value) => {
      const row = /^`(TASK_[1-9][0-9]*_[A-Za-z0-9_]+)`[ \t]+—[ \t]+([a-z]+)$/.exec(value);
      return !row || safeIntegerId(row[1]) === null || !shape.followUpColumns.includes(row[2]);
    })) result.errors.push('follow-ups');
  }
  if (Object.prototype.hasOwnProperty.call(result.sections, 'Execution log')) {
    const execution = strictBullets(result.sections['Execution log']);
    if (execution.residue || !execution.values.length || execution.values.length > 6) result.errors.push('execution-log');
  }
  const acceptanceBody = result.sections['Acceptance trace'];
  if (acceptanceBody) {
    const parsed = strictBullets(acceptanceBody);
    if (parsed.residue || !parsed.values.length || (parsed.values.length > 1 && parsed.values.some((value) => value.toLowerCase() === 'none'))) {
      result.errors.push('acceptance-trace');
    }
    for (const bullet of parsed.values) {
      if (bullet.toLowerCase() === 'none') continue;
      // The versioned Outcome contract is the authority for verdict values.
      // A quoted acceptance bullet may itself contain em-dash-separated prose.
      // Start only after the complete leading code span, exactly like the
      // finalizer's Outcome parser, so both authorities select the same token.
      const leadingCode = /^[ \t]*`[^`]*`/.exec(bullet);
      const separatorIndex = bullet.indexOf(' — ', leadingCode ? leadingCode[0].length : 0);
      const rest = separatorIndex < 0 ? '' : bullet.slice(separatorIndex + 3);
      const nextSeparator = rest.indexOf(' — ');
      const verdict = horizontalTrim(nextSeparator < 0 ? rest : rest.slice(0, nextSeparator)).toLowerCase();
      if (separatorIndex < 0 || !shape.acceptanceVerdicts.includes(verdict)) result.errors.push('acceptance-trace');
      else result.acceptance.push({ verdict });
    }
  }
  const filesBody = result.sections['Files touched'];
  if (filesBody) {
    const values = sectionBullets(filesBody);
    if (values === null) result.errors.push('files-touched');
    for (const bullet of values || []) {
      const fileMatch = /^`([^`\r\n]+)`[ \t]+—[ \t]+([a-z]+)$/.exec(bullet);
      if (!fileMatch || !shape.fileChanges.includes(fileMatch[2])) { result.errors.push('files-touched'); continue; }
      const value = fileMatch[1];
      if (!safeOutcomeFilePath(value)) result.errors.push('files-path');
      else result.files.push(value);
    }
  }
  result.valid = result.errors.length === 0;
  result.status = result.valid ? status : 'malformed';
  result.completedAt = completedAt;
  result.reviewer = reviewer;
  return result;
}

function isoFromMtime(stat) {
  return new Date(Math.floor(stat.mtimeMs / 1000) * 1000).toISOString().replace('.000Z', 'Z');
}

function artifactSuffix(column) { return column === 'pending' ? '.questions.md' : '.md'; }

function allowedColumnTemp(column, name) {
  // These are the only task-column transient names emitted by the canonical
  // create/edit/transition writers. Full validation reads their bytes; scoped
  // validation reads only a matching stem claim and otherwise fences the name.
  if (/^\.transition-[a-f0-9]{36}\.tmp$/.test(name)) return true;
  if (column === 'backlog' && /^\.(?:create|edit)-[a-f0-9]{32}\.tmp$/.test(name)) return true;
  if (column !== 'backlog') return false;
  const claim = /^\.(TASK_[1-9][0-9]*_[A-Za-z0-9_]+)\.md\.claim\.([1-9][0-9]*)\.([a-f0-9]{12})$/.exec(name);
  return !!claim && safeIntegerId(claim[1]) !== null;
}

function looksLikeColumnTemp(name) {
  return name.normalize('NFKC').toUpperCase().startsWith('.TASK_') || /^\.(?:create|edit)-/i.test(name) ||
    name.includes('.claim.') || name.endsWith('.tmp');
}

function scanCorpus(options = {}) {
  const tasksDir = path.resolve(options.tasksDir || process.env.ORCHESTRATOR_TASKS_DIR || __dirname);
  const repoRoot = path.resolve(options.repoRoot || process.env.ORCHESTRATOR_PROJECT_ROOT || path.join(tasksDir, '..', '..'));
  const outcomeShapePath = path.resolve(options.outcomeShapePath || process.env.ORCHESTRATOR_OUTCOME_SHAPE_PATH || path.join(__dirname, '..', 'contracts', 'outcome-shape.json'));
  const testReadHook = assertFixtureReadHookAllowed(options.testReadHook || null, tasksDir, repoRoot);
  // Reverse-dependent discovery is deliberately a full-corpus operation.
  // Ordinary single-stem validation (including scoped INDEX equivalence)
  // keeps a global filename inventory but reads only the dependency closure.
  const fullCorpus = options.fullCorpus === true || !options.stem || options.action === 'drop';
  const findings = [], snapshotInputs = [], directorySnapshots = [], casParentSnapshots = [];
  const maxFiles = Number.isSafeInteger(options.maxFiles) && options.maxFiles > 0 ? Math.min(options.maxFiles, MAX_FILES) : MAX_FILES;
  const maxCorpusBytes = Number.isSafeInteger(options.maxCorpusBytes) && options.maxCorpusBytes > 0 ? Math.min(options.maxCorpusBytes, MAX_CORPUS_BYTES) : MAX_CORPUS_BYTES;
  const maxDirectoryEntries = Number.isSafeInteger(options.maxDirectoryEntries) && options.maxDirectoryEntries > 0
    ? Math.min(options.maxDirectoryEntries, MAX_FILES) : MAX_FILES;
  const runtimeRoots = runtimeRootsFor(options, { repoRoot, tasksDir });
  let rawSnapshot;
  try {
    rawSnapshot = anchoredBoundary({
      action: 'task-snapshot', repoRoot, tasksDir, outcomeShapePath, fullCorpus,
      stem: options.stem || null,
      proposal: options.proposal && Buffer.isBuffer(options.proposal.bytes)
        ? { stem: options.proposal.stem, rawBase64: options.proposal.bytes.toString('base64') } : null,
      checkIndex: options.checkIndex === true,
      includeRuntime: options.includeRuntime === true,
      locksDir: runtimeRoots.locksDir,
      taskCreationsDir: runtimeRoots.taskCreationsDir,
      taskCreationsAuthorityRoot: runtimeRoots.taskCreationsAuthorityRoot,
      taskEditsDir: runtimeRoots.taskEditsDir,
      taskEditsAuthorityRoot: runtimeRoots.taskEditsAuthorityRoot,
      maxFiles, maxCorpusBytes, maxDirectoryEntries,
      maxRuntimeFiles: Number.isSafeInteger(options.maxRuntimeFiles) && options.maxRuntimeFiles > 0
        ? Math.min(options.maxRuntimeFiles, MAX_FILES) : MAX_FILES
    }, Math.ceil(maxCorpusBytes * 1.45) + 16 * 1024 * 1024);
  } catch (error) {
    if (error instanceof SnapshotRaceError || error instanceof ContractError) throw error;
    throw new ContractError('cannot build anchored task snapshot: ' + String(error && error.message || error).slice(0, 500));
  }
  if (!rawSnapshot || !rawSnapshot.directories || !rawSnapshot.files || !Array.isArray(rawSnapshot.selectedStems)) {
    throw new ContractError('anchored task snapshot is malformed');
  }
  const directoryProofs = new Map();
  const listDirectory = (absolutePath, maxEntries, authorityRoot = repoRoot, expected = null) => {
    const key = path.resolve(absolutePath);
    const raw = rawSnapshot.directories[key];
    if (!raw || raw.missing === true) { const error = new Error('directory is missing'); error.code = 'ENOENT'; throw error; }
    if (raw.error) { const error = new Error(String(raw.error.message || 'directory is unsafe')); error.code = raw.error.code; throw error; }
    const entries = Object.create(null);
    for (const name of raw.names || []) entries[name] = statFromProof(raw.entries && raw.entries[name]);
    const snapshot = { st: statFromProof(raw.stat), names: raw.names, entries, truncated: raw.truncated === true,
      limit: maxEntries, authorityRoot: path.resolve(authorityRoot) };
    if (expected && (snapshot.st.dev !== expected.dev || snapshot.st.ino !== expected.ino)) {
      throw new SnapshotRaceError('directory differs from its frozen generation: ' + key);
    }
    directoryProofs.set(path.resolve(absolutePath), snapshot);
    return snapshot;
  };
  const readArtifact = (absolutePath, maxBytes, authorityRoot = null, expectedFile = null) => {
    const key = path.resolve(absolutePath);
    const parent = directoryProofs.get(path.resolve(path.dirname(absolutePath)));
    const authority = authorityRoot || (parent && parent.authorityRoot) ||
      (pathWithin(repoRoot, absolutePath) ? repoRoot : path.dirname(absolutePath));
    const raw = rawSnapshot.files[key];
    if (!raw) throw new SnapshotRaceError('artifact is outside the frozen anchored snapshot: ' + repoRelative(repoRoot, key));
    const stat = statFromProof(raw.stat);
    if (expectedFile && !sameStat(stat, expectedFile)) throw new SnapshotRaceError('artifact differs from its frozen inventory proof: ' + repoRelative(repoRoot, key));
    const relPath = repoRelative(repoRoot, key);
    if (raw.unsafe === true) return { unsafe: true, stat, relPath };
    if (raw.tooLarge === true) return { tooLarge: true, stat, relPath };
    if (typeof raw.rawBase64 !== 'string') throw new ContractError('anchored task snapshot omitted file bytes');
    const buffer = Buffer.from(raw.rawBase64, 'base64');
    if (buffer.length !== stat.size || buffer.length > maxBytes) throw new SnapshotRaceError('artifact byte count violates its frozen proof: ' + relPath);
    if (testReadHook) {
      testReadHook({ absolutePath: key, relPath, phase: 'before-path-revalidation' });
      try {
        anchoredBoundary({ action: 'read', path: key, authorityRoot: authority, maxBytes,
          expectedParent: parent && { dev: parent.st.dev, ino: parent.st.ino },
          expectedFile: { dev: stat.dev, ino: stat.ino, mode: stat.mode, size: stat.size,
            mtimeNs: stat.mtimeNs, ctimeNs: stat.ctimeNs } },
        Math.ceil(maxBytes * 1.5) + 1024 * 1024);
      } catch (_) { throw new SnapshotRaceError('artifact was replaced while reading: ' + relPath); }
    }
    const contentHash = sha256(buffer);
    snapshotInputs.push({ path: relPath, kind: 'file', size: stat.size, hash: contentHash, mtimeMs: stat.mtimeMs });
    return { buffer, stat, relPath, contentHash };
  };
  let contractRead;
  try {
    // Absence/unreadability before the snapshot begins is a missing required
    // contract (exit 3). Disappearance after this probe remains a transient
    // snapshot race (exit 4) through safeReadFile.
    contractRead = readArtifact(outcomeShapePath, MAX_RUNTIME_BYTES);
  }
  catch (error) {
    if (error instanceof SnapshotRaceError) throw error;
    if (error instanceof ContractError) throw error;
    throw new ContractError('cannot read outcome contract safely: ' + error.message);
  }
  if (contractRead.unsafe || contractRead.tooLarge) throw new ContractError('outcome contract is unsafe or oversized');
  const shape = loadOutcomeShape(outcomeShapePath, contractRead.buffer);
  const artifacts = new Map();
  const runtimeLocks = new Map();
  const files = [];
  const loadedStems = new Set();
  let taskRelatedEntries = 0, boundedReadCount = 0, totalBytes = 0;
  let taskBodyReads = 0, taskBodyBytes = 0, inventoryEntries = 0;
  const accountTaskEntry = () => {
    taskRelatedEntries++;
    if (taskRelatedEntries > maxFiles) throw new ContractError('task corpus exceeds the file-count limit');
  };
  const account = (read) => {
    boundedReadCount++;
    if (read && Buffer.isBuffer(read.buffer)) {
      totalBytes += read.buffer.length;
      if (totalBytes > maxCorpusBytes) throw new ContractError('task corpus exceeds the total bounded read limit');
    }
  };
  const model = () => ({
    tasksDir, repoRoot, shape, findings, snapshotInputs, artifacts, files, directorySnapshots,
    loadedStems, fullCorpus, testReadHook, directoryProofs, listDirectory, readArtifact, rawSnapshot, runtimeLocks,
    ioStats: { inventoryEntries, taskRelatedEntries, taskBodyReads, taskBodyBytes, boundedReads: boundedReadCount, boundedReadBytes: totalBytes }
  });
  const nameSnapshot = (absolutePath) => {
    snapshotInputs.push({ path: repoRelative(repoRoot, absolutePath), kind: 'inventoried-name' });
  };
  const inspectNonBodyArtifact = (absolutePath, maxBytes, stem, unsafeCode, tooLargeCode, unsafeMessage, tooLargeMessage) => {
    const rel = repoRelative(repoRoot, absolutePath);
    if (!fullCorpus) { nameSnapshot(absolutePath); return; }
    let read;
    try { read = readArtifact(absolutePath, maxBytes); }
    catch (error) {
      if (error instanceof SnapshotRaceError) throw error;
      findings.push(finding(unsafeCode, 'blocker', stem, [rel], unsafeMessage, 'Inspect this exact path without following it and recover it explicitly.'));
      return;
    }
    account(read);
    if (read.unsafe) findings.push(finding(unsafeCode, 'blocker', stem, [rel], unsafeMessage, 'Inspect this exact path without following it and recover it explicitly.'));
    else if (read.tooLarge) findings.push(finding(tooLargeCode, 'blocker', stem, [rel], tooLargeMessage, 'Reduce or relocate this exact artifact explicitly.'));
  };
  const scannedCasParents = new Set();
  const casTargetStem = (targetName) => {
    if (typeof targetName !== 'string') return null;
    let stem = targetName;
    for (const suffix of ['.questions.md', '.md', '.json']) {
      if (stem.endsWith(suffix)) { stem = stem.slice(0, -suffix.length); break; }
    }
    return safeIntegerId(stem) === null ? null : stem;
  };
  const casMetadataSnapshot = (absolutePath, stat, kind) => {
    snapshotInputs.push({
      path: repoRelative(repoRoot, absolutePath),
      kind,
      mode: stat && stat.mode,
      size: stat && Number.isSafeInteger(stat.size) ? stat.size : null,
      dev: stat ? String(stat.dev) : null,
      ino: stat ? String(stat.ino) : null,
      mtimeMs: stat && Number.isFinite(stat.mtimeMs) ? stat.mtimeMs : null,
      ctimeMs: stat && Number.isFinite(stat.ctimeMs) ? stat.ctimeMs : null
    });
  };
  const snapshotOpaqueCasEntry = (absolutePath, stat, kind) => {
    if (stat && stat.isFile() && !stat.isSymbolicLink()) {
      try {
        const read = readArtifact(absolutePath, MAX_DURABLE_CAS_BYTES, null, stat);
        account(read);
        if (!read.unsafe && !read.tooLarge) return;
        casMetadataSnapshot(absolutePath, read.stat, kind);
        return;
      } catch (error) {
        if (error instanceof SnapshotRaceError) throw error;
      }
    }
    casMetadataSnapshot(absolutePath, stat, kind);
  };
  const inspectDurableCasOperation = (parentDir, name, sourceKind, authorityRoot, parentSnapshot) => {
    const operation = path.join(parentDir, name);
    const operationRel = repoRelative(repoRoot, operation);
    const operationStat = parentSnapshot && parentSnapshot.entries[name];
    if (!operationStat) throw new SnapshotRaceError('durable CAS operation changed during scan: ' + operationRel);
    if (operationStat.isSymbolicLink() || !operationStat.isDirectory()) {
      snapshotOpaqueCasEntry(operation, operationStat, 'durable-cas-operation-metadata');
      findings.push(finding('DURABLE_CAS_OPERATION_UNSAFE', 'blocker', null, [operationRel],
        'A canonical durable CAS operation name is not a real directory.',
        'Recover this exact operation through its create/edit owner without following or deleting it by age.'));
      return;
    }
    let before;
    try { before = listDirectory(operation, 9, authorityRoot); }
    catch (_) { throw new SnapshotRaceError('durable CAS operation changed during bounded scan: ' + operationRel); }
    directorySnapshots.push({ dir: operation, before, label: 'durable CAS operation' });
    inventoryEntries += before.names.length;
    snapshotInputs.push({ path: operationRel + '/', kind: 'durable-cas-operation', owner: sourceKind, names: before.names, truncated: before.truncated });
    if (before.truncated || before.names.length > 8) {
      findings.push(finding('DURABLE_CAS_OPERATION_INVALID', 'blocker', null, [operationRel],
        'A durable CAS operation exceeds its bounded eight-artifact contract.',
        'Recover this exact operation through its create/edit owner before retrying validation.'));
      findings.push(finding('DURABLE_CAS_RECOVERY_REQUIRED', 'error', null, [operationRel],
        'An unfinished durable CAS publication still owns recovery.',
        'Resume or reconcile this exact operation through its create/edit owner; never clear it by age.'));
      return;
    }
    const entries = [];
    const readsByKind = new Map();
    let unsafe = false;
    for (const artifactName of before.names) {
      accountTaskEntry();
      const artifact = path.join(operation, artifactName);
      const artifactKind = durableCas.classifyArtifactName(artifactName);
      const maxBytes = artifactKind === 'manifest' || artifactKind === 'manifest-partial'
        ? MAX_DURABLE_CAS_MANIFEST_BYTES : MAX_DURABLE_CAS_BYTES;
      let read;
      try { read = readArtifact(artifact, maxBytes, authorityRoot, before.entries[artifactName]); }
      catch (error) {
        if (error instanceof SnapshotRaceError) throw error;
        unsafe = true;
        findings.push(finding('DURABLE_CAS_OPERATION_INVALID', 'blocker', null, [repoRelative(repoRoot, artifact)],
          'A durable CAS artifact cannot be read safely.',
          'Recover this exact operation through its create/edit owner without following the artifact.'));
        continue;
      }
      account(read);
      if (read.unsafe || read.tooLarge) {
        unsafe = true;
        casMetadataSnapshot(artifact, read.stat, 'durable-cas-artifact-metadata');
      }
      const entry = {
        name: artifactName,
        kind: read.unsafe || read.tooLarge ? 'unsafe' : 'file',
        size: read.stat && Number.isSafeInteger(read.stat.size) ? read.stat.size : 0
      };
      if (!read.unsafe && !read.tooLarge) {
        if (artifactKind === 'manifest') entry.bytes = read.buffer;
        if (artifactKind) readsByKind.set(artifactKind, read);
      }
      entries.push(entry);
    }
    let inspected = null;
    let invalidReason = null;
    try {
      inspected = durableCas.validateOperationSnapshot(entries);
      if (inspected.manifest) {
        const candidate = readsByKind.get('candidate');
        const source = readsByKind.get('source');
        if (candidate && candidate.contentHash !== inspected.manifest.candidateHash) {
          throw new Error('durable CAS candidate hash differs from its manifest');
        }
        if (source && source.contentHash !== inspected.manifest.expectedProof.hash) {
          throw new Error('durable CAS detached source hash differs from its manifest');
        }
      }
    } catch (error) {
      invalidReason = String(error && error.message || error).slice(0, 300);
    }
    const findingStem = sourceKind === 'task-backlog' && inspected && inspected.manifest
      ? casTargetStem(inspected.manifest.targetName) : null;
    if (unsafe || invalidReason) {
      findings.push(finding('DURABLE_CAS_OPERATION_INVALID', 'blocker', findingStem, [operationRel],
        invalidReason || 'A durable CAS operation contains an unsafe or oversized artifact.',
        'Recover this exact operation through its create/edit owner; do not repair its private files manually.'));
    }
    findings.push(finding('DURABLE_CAS_RECOVERY_REQUIRED', 'error', findingStem, [operationRel],
      'An unfinished durable CAS publication still owns recovery.',
      'Resume or reconcile this exact operation through its create/edit owner; never clear it by age.',
      inspected ? { owner: sourceKind, phase: inspected.phase } : { owner: sourceKind }));
  };
  const scanDurableCasParent = (parentDir, authorityRoot, sourceKind, knownIdentity = null) => {
    const parent = path.resolve(parentDir);
    if (scannedCasParents.has(parent)) return;
    scannedCasParents.add(parent);
    const parentRel = repoRelative(repoRoot, parent);
    if (!pathWithin(authorityRoot, parent)) {
      findings.push(finding('DURABLE_CAS_DIRECTORY_UNSAFE', 'blocker', null, [parentRel],
        'A durable CAS parent escapes its configured authority root.',
        'Restore an explicit in-authority runtime root before task validation.'));
      return;
    }
    let before = knownIdentity;
    if (!before) {
      try { before = listDirectory(parent, maxDirectoryEntries + 1, authorityRoot); }
      catch (error) {
        if (error && error.code === 'ENOENT') return;
        if (error instanceof SnapshotRaceError) throw error;
        findings.push(finding('DURABLE_CAS_DIRECTORY_UNSAFE', 'blocker', null, [parentRel],
          'A durable CAS parent cannot be inspected through its anchored authority root.',
          'Restore a real directory chain inside its configured authority root.'));
        return;
      }
      casParentSnapshots.push({ dir: parent, before });
      inventoryEntries += before.names.length;
    }
    const casNames = before.names.filter((name) => durableCas.classifyName(name) !== null).sort();
    if (!knownIdentity) snapshotInputs.push({ path: parentRel + '/', kind: 'durable-cas-parent', owner: sourceKind, names: casNames, truncated: before.truncated });
    if (before.truncated || before.names.length > maxDirectoryEntries) {
      findings.push(finding('DURABLE_CAS_SCAN_LIMIT', 'blocker', null, [parentRel],
        'A durable CAS parent exceeds its bounded directory-entry limit.',
        'Reduce the owner-managed runtime corpus through authenticated recovery, then retry.'));
      return;
    }
    for (const name of casNames) {
      accountTaskEntry();
      const classification = durableCas.classifyName(name);
      if (classification === 'unsafe') {
        const absolutePath = path.join(parent, name);
        const stat = before.entries[name];
        if (!stat) throw new SnapshotRaceError('durable CAS lookalike changed during scan: ' + repoRelative(repoRoot, absolutePath));
        snapshotOpaqueCasEntry(absolutePath, stat, 'durable-cas-name-metadata');
        findings.push(finding('DURABLE_CAS_NAME_UNSAFE', 'blocker', null, [repoRelative(repoRoot, absolutePath)],
          'A malformed durable CAS lookalike is present in an owner-managed directory.',
          'Inspect this exact path without following it and recover it through the owning create/edit flow.'));
        continue;
      }
      inspectDurableCasOperation(parent, name, sourceKind, authorityRoot, before);
    }
  };

  const relativeTasks = path.relative(repoRoot, tasksDir);
  if (relativeTasks === '..' || relativeTasks.startsWith('..' + path.sep) || path.isAbsolute(relativeTasks)) {
    findings.push(finding('TASK_ROOT_UNSAFE', 'blocker', null, [repoRelative(repoRoot, tasksDir)], 'Task root escapes the project.', 'Restore a real in-project task directory chain before any lifecycle operation.'));
    return model();
  }

  let rootBefore;
  try { rootBefore = listDirectory(tasksDir, maxDirectoryEntries + 1, repoRoot); }
  catch (error) {
    if (error instanceof SnapshotRaceError) throw error;
    findings.push(finding('TASK_ROOT_UNSAFE', 'blocker', null, [repoRelative(repoRoot, tasksDir)], 'Task root or one of its ancestors is not a real directory.', 'Restore a real in-project task directory chain before any lifecycle operation.'));
    return model();
  }
  if (rootBefore.truncated || rootBefore.names.length > maxDirectoryEntries) {
    throw new ContractError('task root exceeds the bounded directory-entry limit');
  }
  directorySnapshots.push({ dir: tasksDir, before: rootBefore, label: 'task root' });
  inventoryEntries += rootBefore.names.length;
  const taskLikeRootNames = rootBefore.names.filter((name) => /^TASK_/i.test(name.normalize('NFKC'))).sort();
  snapshotInputs.push({ path: repoRelative(repoRoot, tasksDir) + '/', kind: 'task-root-directory', names: taskLikeRootNames });
  for (const name of taskLikeRootNames) {
    accountTaskEntry();
    const absolutePath = path.join(tasksDir, name);
    const rel = repoRelative(repoRoot, absolutePath);
    findings.push(finding('TASK_FILE_OUTSIDE_COLUMN', 'blocker', null, [rel], 'Task-like artifact is outside every canonical lifecycle column.', 'Move it explicitly into backlog, pending, todo, or done through the owning lifecycle operation.'));
    inspectNonBodyArtifact(absolutePath, MAX_TASK_BYTES, null, 'TASK_ARTIFACT_UNSAFE', 'TASK_ARTIFACT_TOO_LARGE',
      'Task-like root artifact is a symlink, special file, or unreadable.', 'Task-like root artifact exceeds the bounded read limit.');
  }
  for (const column of COLUMNS) {
    const dir = path.join(tasksDir, column);
    let before;
    try { before = listDirectory(dir, maxDirectoryEntries + 1, repoRoot); }
    catch (_) {
      findings.push(finding('TASK_COLUMN_UNSAFE', 'blocker', null, [repoRelative(repoRoot, dir)], 'Task column is missing or unreadable.', 'Restore the canonical task column as a regular directory.'));
      continue;
    }
    if (!before.st.isDirectory() || before.st.isSymbolicLink()) {
      findings.push(finding('TASK_COLUMN_UNSAFE', 'blocker', null, [repoRelative(repoRoot, dir)], 'Task column is not a real directory.', 'Replace the column with a regular directory without following the current path.'));
      continue;
    }
    if (before.truncated || before.names.length > maxDirectoryEntries) {
      throw new ContractError('task corpus exceeds the bounded directory-entry limit');
    }
    directorySnapshots.push({ dir, before });
    inventoryEntries += before.names.length;
    snapshotInputs.push({ path: repoRelative(repoRoot, dir) + '/', kind: 'directory', names: before.names });
    if (column === 'backlog') scanDurableCasParent(dir, repoRoot, 'task-backlog', before);
    for (const name of before.names) {
      if (name === '.gitkeep') continue;
      if (allowedColumnTemp(column, name)) {
        accountTaskEntry();
        const claim = /^\.(TASK_[1-9][0-9]*_[A-Za-z0-9_]+)\.md\.claim\./.exec(name);
        const matchesScope = claim && claim[1] === options.stem;
        if (fullCorpus || matchesScope) {
          const absolutePath = path.join(dir, name);
          const rel = repoRelative(repoRoot, absolutePath);
          const read = readArtifact(absolutePath, MAX_TASK_BYTES, repoRoot, before.entries[name]);
          account(read);
          if (read.unsafe) findings.push(finding('TASK_TEMP_UNSAFE', 'blocker', claim ? claim[1] : null, [rel], 'Allow-listed task temporary path is a symlink or special file.', 'Let the owning transaction recover, or inspect this exact temporary path before removing it.'));
          else if (read.tooLarge) findings.push(finding('TASK_TEMP_TOO_LARGE', 'blocker', claim ? claim[1] : null, [rel], 'Allow-listed task temporary file exceeds the bounded task size.', 'Let the owning transaction recover, or inspect this exact temporary path before removing it.'));
        } else nameSnapshot(path.join(dir, name));
        continue;
      }
      if (name.startsWith('.')) {
        if (looksLikeColumnTemp(name)) {
          accountTaskEntry();
          const rel = repoRelative(repoRoot, path.join(dir, name));
          findings.push(finding('TASK_TEMP_FILENAME_UNSUPPORTED', 'blocker', null, [rel], 'Task-column temporary filename is not emitted by a canonical writer.', 'Inspect this exact path and its owning transaction; it is never indexed automatically.'));
          inspectNonBodyArtifact(path.join(dir, name), MAX_TASK_BYTES, null, 'TASK_TEMP_UNSAFE', 'TASK_TEMP_TOO_LARGE',
            'Unsupported task temporary path is a symlink, special file, or unreadable.', 'Unsupported task temporary file exceeds the bounded task size.');
        }
        continue;
      }
      // A case/full-width variant can be visually indistinguishable from a
      // canonical task identity.  Treat its NFKC/case-folded TASK_ prefix as
      // task-related so it is reported instead of silently disappearing from
      // the corpus.
      if (!/^TASK_/i.test(name.normalize('NFKC'))) continue;
      accountTaskEntry();
      const suffix = artifactSuffix(column);
      let stem = null;
      if (column === 'pending' && name.endsWith(suffix)) stem = name.slice(0, -suffix.length);
      else if (column !== 'pending' && name.endsWith(suffix)) stem = name.slice(0, -suffix.length);
      const number = stem ? safeIntegerId(stem) : null;
      const rel = repoRelative(repoRoot, path.join(dir, name));
      if (!stem || number === null || name !== stem + suffix) {
        // An invalid/case/full-width identity cannot be safely scoped to one
        // canonical stem.  Make the finding global so validateAction(stem)
        // cannot hide a visually colliding artifact during admission.
        const findingStem = number === null ? null : stem;
        findings.push(finding('TASK_FILENAME_UNSUPPORTED', 'blocker', findingStem, [rel], 'Task-related filename does not match the canonical column shape.', 'Rename or relocate this artifact explicitly; it is never indexed automatically.'));
        inspectNonBodyArtifact(path.join(dir, name), MAX_TASK_BYTES, findingStem, 'TASK_ARTIFACT_UNSAFE', 'TASK_ARTIFACT_TOO_LARGE',
          'Task artifact is a symlink, special file, or unreadable.', 'Task artifact exceeds the bounded read limit.');
        continue;
      }
      const absolutePath = path.join(dir, name);
      const inventoryStat = before.entries[name];
      if (!inventoryStat) throw new SnapshotRaceError('task artifact changed during name inventory: ' + rel);
      const inventoryUnsafe = inventoryStat.isSymbolicLink() || !inventoryStat.isFile();
      const inventoryTooLarge = !inventoryUnsafe && inventoryStat.size > MAX_TASK_BYTES;
      const record = {
        column, stem, number, name, absolutePath, relPath: rel,
        // Keep the exact anchored inventory generation even when the body is
        // intentionally not read (for example, an oversized recovery target).
        // Destructive repair can then bind to inode + metadata without loading
        // arbitrary bytes into memory.
        loaded: false, readable: false, text: null, bytes: null, contentHash: null,
        stat: inventoryUnsafe ? null : inventoryStat,
        inventoryUnsafe, inventoryTooLarge, inventoryMode: inventoryStat.mode, inventorySize: inventoryStat.size
      };
      if (inventoryUnsafe) {
        findings.push(finding('TASK_ARTIFACT_UNSAFE', 'blocker', stem, [rel], 'Task artifact is a symlink or special file.', 'Replace it explicitly with a regular file inside the task column.'));
      } else if (inventoryTooLarge) {
        findings.push(finding('TASK_ARTIFACT_TOO_LARGE', 'blocker', stem, [rel], 'Task artifact exceeds the bounded read limit.', 'Reduce the artifact below the documented task size limit.'));
      }
      if (!artifacts.has(stem)) artifacts.set(stem, { stem, number, backlog: null, pending: null, todo: null, done: null });
      const group = artifacts.get(stem);
      if (group[column]) {
        findings.push(finding('TASK_PRESENT_IN_MULTIPLE_STATES', 'blocker', stem, [group[column].relPath, rel], 'More than one artifact claims the same task column.', 'Resolve the duplicate explicitly before regenerating derived state.'));
      } else group[column] = record;
    }
  }

  // Durable conditional-replace operations can live beside backlog bodies or
  // inside the creation/edit marker stores. They are owner state even when
  // the broader runtime composite is disabled, so action admission must never
  // treat their hidden names as an empty/green task snapshot.
  scanDurableCasParent(runtimeRoots.taskCreationsDir, runtimeRoots.taskCreationsAuthorityRoot, 'task-creations');
  scanDurableCasParent(runtimeRoots.taskEditsDir, runtimeRoots.taskEditsAuthorityRoot, 'task-edits');

  const loadRecord = (record) => {
    if (!record || record.loaded) return;
    record.loaded = true;
    if (record.inventoryUnsafe || record.inventoryTooLarge) {
      account(null);
      taskBodyReads++;
      snapshotInputs.push(record.inventoryUnsafe
        ? { path: record.relPath, kind: 'artifact-metadata', mode: record.inventoryMode }
        : {
            path: record.relPath,
            kind: 'artifact-metadata',
            dev: record.stat.dev,
            ino: record.stat.ino,
            mode: record.stat.mode,
            size: record.stat.size,
            mtimeNs: record.stat.mtimeNs,
            ctimeNs: record.stat.ctimeNs
          });
      return;
    }
    let read;
    const parent = directoryProofs.get(path.resolve(path.dirname(record.absolutePath)));
    try { read = readArtifact(record.absolutePath, MAX_TASK_BYTES, repoRoot, parent && parent.entries[record.name]); }
    catch (error) {
      if (error instanceof SnapshotRaceError) throw error;
      findings.push(finding('TASK_ARTIFACT_UNSAFE', 'blocker', record.stem, [record.relPath], 'Task artifact cannot be read safely.', 'Replace it explicitly with a regular file inside the task column.'));
      return;
    }
    account(read);
    taskBodyReads++;
    if (read && Buffer.isBuffer(read.buffer)) taskBodyBytes += read.buffer.length;
    if (read.unsafe) {
      findings.push(finding('TASK_ARTIFACT_UNSAFE', 'blocker', record.stem, [record.relPath], 'Task artifact is a symlink or special file.', 'Replace it explicitly with a regular file inside the task column.'));
      return;
    }
    if (read.tooLarge) {
      findings.push(finding('TASK_ARTIFACT_TOO_LARGE', 'blocker', record.stem, [record.relPath], 'Task artifact exceeds the bounded read limit.', 'Reduce the artifact below the documented task size limit.'));
      return;
    }
    // Preserve the exact bounded byte generation even when text decoding
    // fails. A malformed task must remain revision-fenced and safely
    // deletable through the repair-oriented drop path.
    Object.assign(record, {
      bytes: read.buffer,
      contentHash: read.contentHash,
      stat: read.stat
    });
    const text = decodeUtf8(read.buffer);
    if (text === null || text.includes('\0')) {
      findings.push(finding('TASK_ARTIFACT_UTF8_INVALID', 'blocker', record.stem, [record.relPath], 'Task artifact is not canonical UTF-8 text.', 'Rewrite the artifact as UTF-8 text without NUL bytes.'));
      return;
    }
    Object.assign(record, { readable: true, text });
    files.push(record);
  };
  const loadGroup = (stem) => {
    if (loadedStems.has(stem)) return;
    loadedStems.add(stem);
    const group = artifacts.get(stem);
    if (!group) return;
    for (const column of COLUMNS) loadRecord(group[column]);
  };

  if (fullCorpus) {
    for (const stem of Array.from(artifacts.keys()).sort()) loadGroup(stem);
  } else if (options.stem) {
    const queue = [options.stem];
    while (queue.length) {
      const current = queue.shift();
      if (loadedStems.has(current)) continue;
      loadGroup(current);
      const group = artifacts.get(current);
      const source = group && (group.backlog || group.todo || group.done);
      let dependencyText = source && source.readable ? source.text : null;
      if (options.proposal && options.proposal.stem === current &&
          ['backlog', 'todo', 'done'].includes(options.proposal.state) && Buffer.isBuffer(options.proposal.bytes)) {
        const proposedText = decodeUtf8(options.proposal.bytes);
        if (proposedText !== null && !proposedText.includes('\0')) dependencyText = proposedText;
      }
      if (dependencyText === null) continue;
      const deps = parseDependencies(dependencyText).deps;
      for (const dep of deps) if (!loadedStems.has(dep)) queue.push(dep);
    }
  }
  for (const item of casParentSnapshots) {
    let after;
    try { after = listDirectory(item.dir, item.before.limit, item.before.authorityRoot, item.before.st); }
    catch (_) { throw new SnapshotRaceError('durable CAS parent changed during scan: ' + item.dir); }
    const beforeNames = item.before.names.filter((name) => durableCas.classifyName(name) !== null).sort();
    const afterNames = after.names.filter((name) => durableCas.classifyName(name) !== null).sort();
    // Creation/edit marker traffic may legitimately alter unrelated names in
    // the same directory while validation runs. Fence the directory identity,
    // scan bound and exact CAS subset; do not turn unrelated owner progress
    // into a false task snapshot race.
    if (!after.st.isDirectory() || after.st.isSymbolicLink() ||
        item.before.st.dev !== after.st.dev || item.before.st.ino !== after.st.ino || item.before.st.mode !== after.st.mode ||
        item.before.truncated !== after.truncated || canonicalJson(beforeNames) !== canonicalJson(afterNames)) {
      throw new SnapshotRaceError('durable CAS parent changed during scan: ' + item.dir);
    }
  }
  for (const item of directorySnapshots) {
    const after = listDirectory(item.dir, item.before.limit, item.before.authorityRoot, item.before.st);
    if (!sameStat(item.before.st, after.st) || item.before.truncated !== after.truncated ||
        canonicalJson(item.before.names) !== canonicalJson(after.names)) {
      throw new SnapshotRaceError((item.label || 'task column') + ' changed during scan: ' + item.dir);
    }
  }
  return model();
}

function classify(group) {
  if (!group) return 'absent';
  const b = !!group.backlog, p = !!group.pending, t = !!group.todo, d = !!group.done;
  if (!b && !p && !t && !d) return 'absent';
  if (b && !p && !t && !d) return 'backlog';
  if (b && p && !t && !d) return 'pending';
  if (!b && !p && t && !d) return 'todo';
  if (!b && !p && !t && d) return 'done';
  return 'corrupt';
}

function applyProposal(model, proposal) {
  if (!proposal || typeof proposal !== 'object' || safeIntegerId(proposal.stem) === null ||
      !['backlog', 'pending', 'todo', 'done'].includes(proposal.state) ||
      !Buffer.isBuffer(proposal.bytes) || proposal.bytes.length < 1 || proposal.bytes.length > MAX_TASK_BYTES) {
    const error = new Error('proposal must contain a canonical stem/state and bounded non-empty bytes'); error.exitCode = 2; throw error;
  }
  let group = model.artifacts.get(proposal.stem);
  const currentState = group ? classify(group) : 'absent';
  const fromState = proposal.fromState === undefined ? proposal.state : proposal.fromState;
  const sameStateReplacement = fromState === proposal.state && group && group[proposal.state];
  const allowedCrossState = fromState !== proposal.state && ALLOWED_TRANSITIONS.has(fromState + ':' + proposal.state);
  if (!STATES.includes(fromState) || currentState !== fromState || (!sameStateReplacement && !allowedCrossState)) {
    const error = new Error('proposal is not valid for the current canonical state/transition'); error.exitCode = 2; throw error;
  }
  // Creation validates its exact rendered bytes before it publishes an
  // idempotency marker, reserves a number, or writes the backlog body. Model
  // the absent stem only in memory; the anchored snapshot has already fenced
  // the global name inventory and loaded the proposal's dependency closure.
  if (!group) {
    group = {
      stem: proposal.stem,
      number: safeIntegerId(proposal.stem),
      backlog: null,
      pending: null,
      todo: null,
      done: null
    };
    model.artifacts.set(proposal.stem, group);
    model.loadedStems.add(proposal.stem);
  }
  const text = decodeUtf8(proposal.bytes);
  if (text === null || text.includes('\0')) { const error = new Error('proposal must be UTF-8 text without NUL bytes'); error.exitCode = 2; throw error; }
  // Provenance is immutable after creation. Pending proposals are question
  // sidecars and deliberately carry no Source block; every body replacement or
  // cross-state body proposal must preserve the exact canonical envelope.
  if (fromState !== 'absent' && proposal.state !== 'pending') {
    const currentBody = group.backlog || group.todo || group.done;
    const beforeSource = currentBody && currentBody.readable ? taskSource.parse(currentBody.text) : null;
    const afterSource = taskSource.parse(text);
    const preserved = beforeSource && beforeSource.valid && afterSource.valid &&
      taskSource.same(beforeSource.source, afterSource.source) &&
      (!beforeSource.package ||
        (afterSource.package &&
          beforeSource.package.block === afterSource.package.block));
    if (!preserved) {
      model.findings.push(finding('TASK_SOURCE_IMMUTABLE', 'blocker', proposal.stem,
        currentBody ? [currentBody.relPath] : [],
        'Task Source provenance changed or disappeared in a lifecycle proposal.',
        'Preserve the canonical Source block and API Work Package metadata, when present, byte-for-byte.'));
    }
  }
  const previous = group[proposal.state] || group.backlog || group.pending || group.todo || group.done;
  const replacement = Object.assign({}, previous, {
    column: proposal.state,
    name: proposal.stem + artifactSuffix(proposal.state),
    relPath: '<proposal>/' + proposal.state + '/' + proposal.stem,
    absolutePath: null,
    text,
    bytes: proposal.bytes,
    contentHash: sha256(proposal.bytes),
    loaded: true,
    readable: true
  });
  if (fromState !== proposal.state) {
    for (const column of COLUMNS) {
      const source = group[column];
      if (source) {
        const index = model.files.indexOf(source);
        if (index >= 0) model.files.splice(index, 1);
      }
      group[column] = null;
    }
  }
  group[proposal.state] = replacement;
  const at = model.files.indexOf(previous);
  if (at >= 0) model.files[at] = replacement;
  else model.files.push(replacement);
  model.snapshotInputs.push({
    path: '<proposal>/' + proposal.state + '/' + proposal.stem,
    kind: 'proposal',
    size: proposal.bytes.length,
    hash: replacement.contentHash
  });
}

function revisionFor(group) {
  const state = classify(group);
  const rows = [];
  if (group) for (const column of COLUMNS) if (group[column]) {
    const record = group[column];
    // Valid/bounded artifacts are revisioned by content. Opaque recovery
    // targets (notably oversized files) are revisioned by the exact anchored
    // filesystem generation, so confirmation and transition capture still
    // detect replacement or in-place mutation without reading the body.
    const generation = record.loaded && HASH_RE.test(String(record.contentHash || ''))
      ? record.contentHash
      : record.stat
        ? {
            kind: 'metadata',
            dev: record.stat.dev,
            ino: record.stat.ino,
            mode: record.stat.mode,
            size: record.stat.size,
            mtimeNs: record.stat.mtimeNs,
            ctimeNs: record.stat.ctimeNs
          }
        : null;
    rows.push([column, record.relPath, generation]);
  }
  return sha256('task-state-revision-v1\0' + state + '\0' + canonicalJson(rows));
}

function validateContent(model) {
  const findings = model.findings;
  const identityByNumber = new Map();
  const aliasByKey = new Map();
  const metadata = new Map();

  for (const [stem, group] of model.artifacts.entries()) {
    const paths = COLUMNS.filter((c) => group[c]).map((c) => group[c].relPath);
    const state = classify(group);
    group.state = state;
    const bodyLoaded = model.loadedStems.has(stem);
    group.revision = bodyLoaded ? revisionFor(group) : null;
    if (group.pending && !group.backlog) {
      findings.push(finding('PENDING_SOURCE_MISSING', 'blocker', stem, [group.pending.relPath], 'Pending sidecar has no matching backlog source.', 'Restore the matching backlog source or explicitly remove the orphan sidecar.'));
    }
    if (state === 'corrupt') {
      findings.push(finding('TASK_PRESENT_IN_MULTIPLE_STATES', 'blocker', stem, paths, 'Task artifacts do not resolve to exactly one canonical logical state.', 'Resolve the listed column collision without deleting unrelated task history.'));
    }
    if (!identityByNumber.has(group.number)) identityByNumber.set(group.number, []);
    identityByNumber.get(group.number).push(stem);
    const aliasKey = stem.normalize('NFC').toLowerCase();
    if (!aliasByKey.has(aliasKey)) aliasByKey.set(aliasKey, []);
    aliasByKey.get(aliasKey).push(stem);

    // Filename identity and cross-column placement are global. Content shape
    // is intentionally limited to the selected dependency closure.
    if (!bodyLoaded) continue;

    const primary = [group.backlog, group.todo, group.done].find((record) => record && record.readable) || null;
    const source = primary || (group.pending && group.pending.readable ? group.pending : null);
    let heading = null;
    if (primary) {
      heading = parseHeading(primary.text);
      if (heading.issue === 'missing') findings.push(finding('TASK_HEADING_MISSING', 'error', stem, [primary.relPath], 'Task has no first non-empty heading.', 'Add the canonical # TASK <N> — <title> heading as the first non-empty line.'));
      else if (heading.issue === 'noncanonical') findings.push(finding('TASK_HEADING_NONCANONICAL', 'error', stem, [primary.relPath], 'First non-empty line is not a canonical task heading.', 'Use # TASK <N> — <non-empty title> with the filename number.'));
      else {
        if (heading.number !== group.number) findings.push(finding('TASK_HEADING_NUMBER_MISMATCH', 'error', stem, [primary.relPath], 'Task heading number does not match its filename.', 'Make the heading number equal the immutable task id in the filename.'));
        if (!heading.title) findings.push(finding('TASK_TITLE_EMPTY', 'error', stem, [primary.relPath], 'Task heading title is empty.', 'Add a concise non-empty task title.'));
        if (!heading.canonicalDash) findings.push(finding('TASK_HEADING_NONCANONICAL', 'error', stem, [primary.relPath], 'Task heading uses an unsupported separator.', 'Use the canonical em dash.'));
      }
    }

    const parseSource = primary;
    const deps = parseSource ? parseDependencies(parseSource.text) : { deps: [], invalid: [], sectionCount: 0 };
    const lineage = parseSource ? parseLineage(parseSource.text) : { parents: [], invalid: [], sectionCount: 0 };
    const parsedTaskSource = parseSource ? taskSource.parse(parseSource.text) : { present: false, valid: false, error: 'task-source-missing' };
    const splitFrom = lineage.parents.length === 1 ? lineage.parents[0] : null;
    const origin = parsedTaskSource.valid ? parsedTaskSource.source : null;
    metadata.set(stem, {
      state,
      revision: group.revision,
      title: heading && !heading.issue ? heading.title : '',
      heading,
      deps: deps.deps,
      lineage: lineage.parents,
      source,
      taskSource: parsedTaskSource,
      origin
    });

    if (!parsedTaskSource.valid) {
      const code = parsedTaskSource.error === 'task-source-missing'
        ? 'TASK_SOURCE_MISSING' : parsedTaskSource.error === 'task-source-duplicate'
        ? 'TASK_SOURCE_DUPLICATE' : parsedTaskSource.error === 'task-source-invalid'
          ? 'TASK_SOURCE_INVALID' : 'TASK_SOURCE_MALFORMED';
      findings.push(finding(code, 'error', stem, primary ? [primary.relPath] : paths,
        'Task must contain exactly one canonical Source provenance section.',
        'Create or repair the canonical Source block explicitly; provenance is never inferred from task prose, lineage, or filenames.'));
    } else if (parsedTaskSource.present && parsedTaskSource.source.kind === 'follow-up' &&
        ['task-split', 'test-foundation-prerequisite'].includes(parsedTaskSource.source.type) &&
        splitFrom !== parsedTaskSource.source.ref) {
      findings.push(finding('TASK_SOURCE_LINEAGE_MISMATCH', 'error', stem, primary ? [primary.relPath] : paths,
        'Delegated-child Source does not match the separate Origin lineage parent.',
        'Keep Source Ref and the single Origin parent aligned for a structural split or foundation prerequisite.'));
    }

    if (deps.sectionCount > 1) findings.push(finding('DEPENDENCY_SYNTAX_INVALID', 'error', stem, paths, 'Task contains multiple dependency sections.', 'Consolidate dependencies into one canonical section.'));
    if (deps.invalid.length) findings.push(finding('DEPENDENCY_SYNTAX_INVALID', 'error', stem, paths, 'One or more dependency bullets are ambiguous.', 'Use one canonical TASK_<N>_<slug> stem per dependency bullet.', { values: deps.invalid.slice(0, 20) }));
    if (deps.deps.includes(stem)) findings.push(finding('DEPENDENCY_SELF', 'blocker', stem, paths, 'Task depends on itself.', 'Remove the self-dependency before promotion or execution.'));
    const duplicateDeps = Array.from(new Set(deps.deps.filter((dep, index) => deps.deps.indexOf(dep) !== index))).sort();
    if (duplicateDeps.length) findings.push(finding('DEPENDENCY_DUPLICATE', 'error', stem, paths, 'Task repeats a dependency.', 'Keep each explicit dependency exactly once.', { dependencies: duplicateDeps }));
    if (lineage.sectionCount > 1 || lineage.parents.length > 1) findings.push(finding('LINEAGE_MULTIPLE_PARENTS', 'error', stem, paths, 'Task declares more than one lineage parent.', 'Retain exactly one informational origin parent.'));
    if (lineage.invalid.length) findings.push(finding('LINEAGE_INVALID', 'error', stem, paths, 'Origin section contains an ambiguous lineage bullet.', 'Use “- split from TASK_<N>_<slug>”.'));

    if ((state === 'backlog' || state === 'pending') && group.backlog && group.backlog.readable) {
      if (hasOutcome(group.backlog.text)) findings.push(finding('OUTCOME_FORBIDDEN_IN_LIVE_STATE', 'error', stem, [group.backlog.relPath], 'Backlog source contains an Outcome appendix.', 'Remove the Outcome trailer through an authorized reopen/edit flow.'));
    }
    if (state === 'pending' && group.pending && group.pending.readable) {
      const pending = parsePending(group.pending.text);
      const fields = pending.fields;
      if (pending.errors.some((item) => item !== 'question-heading') || pending.duplicateFields.length) findings.push(finding('PENDING_FRONTMATTER_INVALID', 'error', stem, [group.pending.relPath], 'Pending frontmatter is malformed or contains duplicate keys.', 'Regenerate the questions sidecar through task-prep.'));
      if (pending.errors.includes('question-heading')) findings.push(finding('PENDING_QUESTION_HEADING_INVALID', 'error', stem, [group.pending.relPath], 'Pending sidecar contains an unrecognized question heading.', 'Use exactly “## Q<N> — <question>” for every question block.'));
      if (fields.forTask !== stem) findings.push(finding('PENDING_FOR_TASK_MISMATCH', 'error', stem, [group.pending.relPath], 'Pending forTask does not match the task stem.', 'Regenerate the sidecar for the matching backlog source.'));
      const isoValid = (value) => typeof value === 'string' && value.length <= 32 && exactTaskInstant(value);
      const round = boundedDecimal(fields.round, 1);
      const gapCount = boundedDecimal(fields.gapCount, 0);
      const timestampsOrdered = isoValid(fields.createdAt) && isoValid(fields.updatedAt) &&
        Date.parse(fields.updatedAt) >= Date.parse(fields.createdAt);
      if (!timestampsOrdered || round === null || gapCount === null || gapCount < pending.questions.length ||
          (fields.prevGapCount !== undefined && boundedDecimal(fields.prevGapCount, 0) === null)) {
        findings.push(finding('PENDING_COUNTER_INVALID', 'error', stem, [group.pending.relPath], 'Pending timestamps, round, or convergence counters are invalid.', 'Regenerate the sidecar with canonical ISO timestamps and non-negative counters.'));
      }
      if (!pending.questions.length) findings.push(finding('PENDING_QUESTION_ID_INVALID', 'error', stem, [group.pending.relPath], 'Pending sidecar contains no canonical questions.', 'Write at least one atomic question or promote the task instead.'));
      const ids = new Set();
      let previous = 0;
      for (const question of pending.questions) {
        if (!Number.isSafeInteger(question.id) || question.id < 1 || ids.has(question.id)) findings.push(finding('PENDING_QUESTION_ID_INVALID', 'error', stem, [group.pending.relPath], 'Question ids must be positive and unique.', 'Renumber questions without duplicates.'));
        if (question.id <= previous) findings.push(finding('PENDING_QUESTION_ORDER_INVALID', 'error', stem, [group.pending.relPath], 'Question ids are not strictly ordered.', 'Order question blocks by increasing stable id.'));
        ids.add(question.id); previous = question.id;
        if (question.types.length !== 1 || !['text', 'choice', 'multiselect'].includes(question.types[0])) findings.push(finding('PENDING_QUESTION_TYPE_INVALID', 'error', stem, [group.pending.relPath], 'Question type is missing, duplicated, or unsupported.', 'Use exactly one text, choice, or multiselect Type line.'));
        const type = question.types[0];
        if (type === 'choice' || type === 'multiselect') {
          const opts = question.options.length === 1 ? question.options[0].split(',').map((v) => v.trim()).filter(Boolean) : [];
          const keys = opts.map((v) => v.normalize('NFC').toLowerCase());
          if (opts.length < 2 || new Set(keys).size !== keys.length) findings.push(finding('PENDING_QUESTION_OPTIONS_INVALID', 'error', stem, [group.pending.relPath], 'Choice question options are missing or duplicated.', 'Provide one Options line with at least two unique comma-separated values.'));
        } else if (question.options.length) findings.push(finding('PENDING_QUESTION_OPTIONS_INVALID', 'error', stem, [group.pending.relPath], 'Text question must not declare Options.', 'Remove the Options line from text questions.'));
        if (question.answerCount !== 1) findings.push(finding('PENDING_ANSWER_SECTION_INVALID', 'error', stem, [group.pending.relPath], 'Every question must contain exactly one Answer section.', 'Regenerate the sidecar without duplicate or missing Answer headings.'));
      }
    }
    if (state === 'todo' && group.todo && group.todo.readable) {
      if (hasOutcome(group.todo.text)) findings.push(finding('OUTCOME_FORBIDDEN_IN_LIVE_STATE', 'error', stem, [group.todo.relPath], 'Todo task contains an Outcome appendix.', 'Use the finalizer for completion or the reopen helper to strip history safely.'));
      const todo = parseTodo(group.todo.text);
      for (const required of ['goal', 'inputs', 'acceptance', 'out of scope']) {
        const values = todo.map.get(required) || [];
        if (!values.length) findings.push(finding('TODO_REQUIRED_SECTION_MISSING', 'error', stem, [group.todo.relPath], 'Todo task is missing “' + required + '”.', 'Return the task to prep and generate the complete runnable shape.'));
        else if (values.length > 1 || !values[0].body.trim()) findings.push(finding('TODO_REQUIRED_SECTION_EMPTY', 'error', stem, [group.todo.relPath], 'Todo section “' + required + '” is empty or duplicated.', 'Keep one non-empty canonical section.'));
      }
      if (!todo.acceptance.automated.length || todo.acceptance.invalid.length || todo.acceptance.manual.some((v) => v.toLowerCase() === 'none')) {
        findings.push(finding('TODO_ACCEPTANCE_INVALID', 'error', stem, [group.todo.relPath], 'Todo acceptance structure is not runnable.', 'Provide non-empty Automated bullets and only meaningful optional Manual bullets.'));
      }
      const designSections = todo.map.get('design') || [];
      if (designSections.length) {
        // Reuse the same CommonMark-aware representation that discovered the
        // section. Passing raw Markdown here would let a fenced/HTML decoy
        // heading win design-parser's first-heading scan while parseTodo sees
        // a different real section.
        const structuralTodo = structuralText(group.todo.text);
        const parsedDesign = designParser.parseDesign(structuralTodo);
        const blockingKinds = Array.from(new Set((parsedDesign.issues || [])
          .map((item) => item && item.kind).filter((kind) => kind && kind !== 'RISKY_SCREEN_NAME'))).sort();
        const malformedEntry = (parsedDesign.entries || []).some((entry) =>
          entry && !entry.none && Object.keys(entry.themes || {}).length === 0);
        if (designSections.length !== 1 || !designSections[0].body.trim() ||
            !(parsedDesign.entries || []).length || blockingKinds.length || malformedEntry ||
            designParser.hasMalformedDesign(structuralTodo)) {
          findings.push(finding('TODO_DESIGN_INVALID', 'error', stem, [group.todo.relPath],
            'Todo Design section is empty, duplicated, or contains a malformed node declaration.',
            'Keep one Design section with canonical node bullets; detailed evidence remains owned by the Figma gates.',
            blockingKinds.length ? { issueKinds: blockingKinds } : null));
        }
        const riskyNames = Array.from(new Set((parsedDesign.issues || [])
          .filter((item) => item && item.kind === 'RISKY_SCREEN_NAME')
          .map((item) => String(item.screen || '')).filter(Boolean))).sort();
        if (riskyNames.length) findings.push(finding('TODO_DESIGN_NAME_RISKY', 'warning', stem, [group.todo.relPath],
          'One or more Design node names are risky for deterministic capture filenames.',
          'Prefer stable alphanumeric node names before pulling design evidence.', { names: riskyNames }));
      }
      // `## Questions` is a reserved machine-owned section on a running task.
      // No canonical write can produce a malformed one, so a broken section is
      // task data that needs explicit repair, not a silently ignored heading.
      const parsedQuestions = parseTaskQuestions(group.todo.text);
      if (parsedQuestions.sectionCount > 0) {
        const questionsIssue = taskQuestionsIssue(parsedQuestions);
        // Questions is a reserved machine-owned section. A malformed instance
        // cannot be treated as prose or admitted through the current task
        // protocol; explicit Drop remains available as the repair operation.
        if (questionsIssue) findings.push(finding('TODO_QUESTIONS_INVALID', 'error', stem, [group.todo.relPath],
          'Todo Questions section is not canonical.',
          'Repair or remove the question blocks through an authorized in-column edit.',
          { reason: questionsIssue }));
      }
      for (const bullet of todo.acceptance.automated) if (!automationAnchored(bullet)) {
        findings.push(finding('TODO_AUTOMATION_ANCHOR_MISSING', 'error', stem, [group.todo.relPath], 'Automated acceptance bullet has no deterministic anchor.', 'Add a file path, class/member, backticked identifier, or Gradle task.', { bullet: bullet.slice(0, 120) }));
      }
    }
    if (state === 'done' && group.done && group.done.readable) {
      const outcome = parseOutcome(group.done.text, model.shape);
      metadata.get(stem).outcome = outcome;
      if (!outcome.valid) findings.push(finding('DONE_OUTCOME_INVALID', 'error', stem, [group.done.relPath], 'Done task has a malformed Outcome appendix.', 'Repair the exact Outcome fields and required sections without rewriting unrelated history.', { reasons: Array.from(new Set(outcome.errors)).sort().slice(0, 30) }));
      if (outcome.errors.some((v) => v === 'completed-at')) findings.push(finding('DONE_COMPLETED_AT_INVALID', 'error', stem, [group.done.relPath], 'Done completion timestamp is invalid.', 'Use a parseable UTC completion timestamp.'));
      if (outcome.errors.some((v) => v === 'acceptance-trace')) findings.push(finding('DONE_ACCEPTANCE_TRACE_INVALID', 'error', stem, [group.done.relPath], 'Done acceptance trace contains an unsupported verdict.', 'Use verified, manual, or deferred for every non-none bullet.'));
      if (outcome.errors.some((v) => v === 'files-touched' || v === 'files-path')) findings.push(finding('DONE_FILES_TOUCHED_INVALID', 'error', stem, [group.done.relPath], 'Done Files touched contains an invalid or unsafe path.', 'Use bounded repository-relative paths without traversal.'));
    }
  }

  for (const [number, stems] of identityByNumber.entries()) {
    const unique = Array.from(new Set(stems)).sort();
    if (unique.length > 1) findings.push(finding('TASK_NUMBER_CONFLICT', 'blocker', null, unique.flatMap((stem) => {
      const group = model.artifacts.get(stem); return COLUMNS.filter((c) => group[c]).map((c) => group[c].relPath);
    }), 'Numeric task id ' + number + ' is claimed by multiple stems.', 'Choose one durable identity explicitly; INDEX publication remains blocked.', { stems: unique }));
  }
  for (const stems of aliasByKey.values()) {
    const unique = Array.from(new Set(stems)).sort();
    if (unique.length > 1) findings.push(finding('TASK_STEM_ALIAS_COLLISION', 'blocker', null, unique.flatMap((stem) => {
      const group = model.artifacts.get(stem); return COLUMNS.filter((c) => group[c]).map((c) => group[c].relPath);
    }), 'Case/Unicode-equivalent task stems collide.', 'Choose one canonical ASCII identity and update references explicitly.', { stems: unique }));
  }

  // Dependency integrity. Unresolved live dependencies are warnings globally;
  // run admission upgrades them to blockers so Figma split parents stay valid
  // while their child tasks are still live.
  for (const [stem, meta] of metadata.entries()) {
    for (const dep of Array.from(new Set(meta.deps))) {
      const target = metadata.get(dep);
      const targetIntegrityError = findings.some((item) =>
        (item.severity === 'error' || item.severity === 'blocker') && scopeFindingApplies(item, dep));
      const accepted = target && !targetIntegrityError && target.state === 'done' && target.outcome && target.outcome.valid && ['completed', 'completed-with-caveats'].includes(target.outcome.status);
      if (!accepted) {
        findings.push(finding('DEPENDENCY_UNRESOLVED', 'warning', stem, meta.source ? [meta.source.relPath] : [], 'Dependency ' + dep + ' is not a valid accepted done task.', 'Complete or repair the dependency before executing this task.', { dependency: dep }));
      }
    }
  }
  const live = Array.from(metadata.entries()).filter(([, meta]) => meta.state !== 'done');
  const graph = new Map(live.map(([stem, meta]) => [stem, Array.from(new Set(meta.deps)).filter((dep) => metadata.has(dep) && metadata.get(dep).state !== 'done')]));
  const visiting = new Set(), visited = new Set(), stack = [];
  const cycles = new Set();
  function dfs(node) {
    if (visiting.has(node)) {
      const at = stack.indexOf(node);
      if (at >= 0) cycles.add(stack.slice(at).concat(node).join(' -> '));
      return;
    }
    if (visited.has(node)) return;
    visiting.add(node); stack.push(node);
    for (const next of graph.get(node) || []) dfs(next);
    stack.pop(); visiting.delete(node); visited.add(node);
  }
  for (const stem of graph.keys()) dfs(stem);
  for (const cycle of Array.from(cycles).sort()) {
    const chain = cycle.split(' -> ');
    const stems = Array.from(new Set(chain)).sort();
    const stem = chain[0];
    const paths = stems.flatMap((item) => metadata.get(item) && metadata.get(item).source ? [metadata.get(item).source.relPath] : []);
    findings.push(finding('DEPENDENCY_CYCLE', 'blocker', stem, paths, 'Live dependency cycle detected.', 'Break the explicit dependency cycle before execution.', { cycle, stems }));
  }
  model.metadata = metadata;
}

function deriveIndex(model, generatedAt) {
  const columns = { backlog: [], pending: [], todo: [], done: [] };
  const ordered = Array.from(model.artifacts.keys()).sort();
  for (const stem of ordered) {
    const group = model.artifacts.get(stem);
    if (!group || !STATES.includes(group.state) || group.state === 'absent') continue;
    if (group.state === 'pending') {
      if (!group.backlog || !group.backlog.readable || !group.pending || !group.pending.readable) continue;
    } else if (!group[group.state] || !group[group.state].readable) continue;
    const meta = model.metadata.get(stem) || { title: '', deps: [], lineage: [], origin: null, taskSource: null };
    const pending = group.state === 'pending' ? parsePending(group.pending.text) : null;
    const pendingRound = pending ? boundedDecimal(pending.fields.round, 1) : null;
    const outcome = group.state === 'done' ? (meta.outcome || parseOutcome(group.done.text, model.shape)) : null;
    const sourceRecord = group.state === 'pending' ? group.backlog : group[group.state];
    const row = {
      stem,
      title: meta.title || stem,
      state: group.state,
      sourceRevision: group.revision,
      createdAt: isoFromMtime(sourceRecord.stat),
      doneAt: group.state === 'done' ? isoFromMtime(group.done.stat) : null,
      origin: meta.taskSource && meta.taskSource.valid ? meta.origin : null,
      dependsOn: meta.deps.slice(),
      splitFrom: meta.lineage[0] || null,
      outcomeStatus: outcome ? outcome.status : null,
      questionsCount: pending ? pending.questions.length : null,
      round: pending ? (pendingRound === null ? 1 : pendingRound) : null
    };
    columns[group.state].push(row);
  }
  const any = COLUMNS.some((column) => columns[column].length);
  return {
    version: INDEX_VERSION,
    generatedAt: any ? (generatedAt || new Date().toISOString().replace(/\.\d{3}Z$/, 'Z')) : '1970-01-01T00:00:00Z',
    backlog: columns.backlog,
    pending: columns.pending,
    todo: columns.todo,
    done: columns.done
  };
}

function structuralIndex(index) {
  if (!index || typeof index !== 'object' || Array.isArray(index)) return index;
  const out = JSON.parse(JSON.stringify(index));
  delete out.generatedAt;
  return out;
}

function checkIndex(model) {
  const indexPath = path.join(model.tasksDir, 'INDEX.json');
  const rel = repoRelative(model.repoRoot, indexPath);
  const taskRoot = model.directoryProofs.get(path.resolve(model.tasksDir));
  if (!taskRoot || !taskRoot.names.includes('INDEX.json')) {
    model.findings.push(finding('INDEX_UNSAFE', 'error', null, [rel], 'INDEX.json is missing.', 'Regenerate INDEX only after the task corpus is valid.'));
    model.indexStatus = 'invalid';
    return;
  }
  let read;
  try { read = model.readArtifact(indexPath, MAX_INDEX_BYTES, model.repoRoot, taskRoot.entries['INDEX.json']); }
  catch (error) {
    if (error instanceof SnapshotRaceError) throw error;
    model.findings.push(finding('INDEX_UNSAFE', 'error', null, [rel], 'INDEX.json cannot be read safely.', 'Regenerate INDEX only after the task corpus is valid.'));
    model.indexStatus = 'invalid'; return;
  }
  if (read.unsafe) {
    model.findings.push(finding('INDEX_UNSAFE', 'error', null, [rel], 'INDEX.json is a symlink or special file.', 'Replace it explicitly with a regular generated file.'));
    model.indexStatus = 'invalid'; return;
  }
  if (read.tooLarge) {
    model.findings.push(finding('INDEX_TOO_LARGE', 'error', null, [rel], 'INDEX.json exceeds the bounded read limit.', 'Regenerate a bounded canonical index.'));
    model.indexStatus = 'invalid'; return;
  }
  const text = decodeUtf8(read.buffer);
  let current;
  try { current = text === null ? null : JSON.parse(text); }
  catch (_) { current = null; }
  if (!current || typeof current !== 'object' || Array.isArray(current)) {
    model.findings.push(finding('INDEX_JSON_INVALID', 'error', null, [rel], 'INDEX.json is not valid JSON object data.', 'Regenerate INDEX after repairing the task corpus.'));
    model.indexStatus = 'invalid'; return;
  }
  if (!exactObjectFields(current, ['backlog', 'done', 'generatedAt', 'pending', 'todo', 'version']) ||
      current.version !== INDEX_VERSION || !exactTaskInstant(current.generatedAt) || COLUMNS.some((column) => !Array.isArray(current[column]))) {
    model.findings.push(finding('INDEX_SCHEMA_INVALID', 'error', null, [rel], 'INDEX.json does not match the canonical task-index schema.', 'Regenerate INDEX with the current publisher.'));
    model.indexStatus = 'invalid'; return;
  }
  if (COLUMNS.reduce((total, column) => total + current[column].length, 0) > MAX_FILES) {
    model.findings.push(finding('INDEX_TOO_LARGE', 'error', null, [rel], 'INDEX.json exceeds the bounded task-row limit.', 'Regenerate a bounded canonical index from the task corpus.'));
    model.indexStatus = 'invalid'; return;
  }
  const indexStems = new Map();
  const numberStems = new Map();
  const aliasStems = new Map();
  let identityInvalid = false;
  for (const column of COLUMNS) {
    for (let index = 0; index < current[column].length; index++) {
      const row = current[column][index];
      if (!row || typeof row !== 'object' || Array.isArray(row) || safeIntegerId(row.stem) === null) {
        identityInvalid = true;
        continue;
      }
      const location = column + '[' + index + ']';
      if (indexStems.has(row.stem)) identityInvalid = true;
      else indexStems.set(row.stem, location);
      const number = safeIntegerId(row.stem);
      if (!numberStems.has(number)) numberStems.set(number, new Set());
      numberStems.get(number).add(row.stem);
      const alias = row.stem.normalize('NFC').toLowerCase();
      if (!aliasStems.has(alias)) aliasStems.set(alias, new Set());
      aliasStems.get(alias).add(row.stem);
    }
  }
  if (Array.from(numberStems.values()).some((stems) => stems.size > 1) ||
      Array.from(aliasStems.values()).some((stems) => stems.size > 1)) identityInvalid = true;
  if (identityInvalid) {
    model.findings.push(finding('INDEX_IDENTITY_INVALID', 'error', null, [rel], 'INDEX.json contains an invalid, duplicate, or colliding task identity.', 'Regenerate INDEX from the canonical global filename inventory.'));
    model.indexStatus = 'invalid'; return;
  }
  const canonicalExpected = deriveIndex(model, current.generatedAt);
  const expected = canonicalExpected;
  let observed = current;
  if (!model.fullCorpus) {
    const closure = model.loadedStems;
    observed = {
      version: current.version,
      generatedAt: current.generatedAt,
      backlog: current.backlog.filter((row) => closure.has(row.stem)),
      pending: current.pending.filter((row) => closure.has(row.stem)),
      todo: current.todo.filter((row) => closure.has(row.stem)),
      done: current.done.filter((row) => closure.has(row.stem))
    };
  }
  if (canonicalJson(structuralIndex(observed)) !== canonicalJson(structuralIndex(expected))) {
    const scopeMessage = model.fullCorpus ? 'the filesystem snapshot' : 'the selected task/dependency closure';
    model.findings.push(finding('INDEX_STALE', 'error', null, [rel], 'INDEX.json does not structurally match ' + scopeMessage + '.', 'Run the canonical index publisher after repairing all blocker findings.'));
    model.indexStatus = 'stale';
  } else model.indexStatus = 'fresh';
}

function checkRuntime(model, options) {
  const validationNowMs = options.nowMs === undefined ? Date.now() : options.nowMs;
  if (!Number.isSafeInteger(validationNowMs) || validationNowMs < 0) {
    throw new ContractError('runtime validation clock must be a non-negative safe integer');
  }
  const locksDir = path.resolve(options.locksDir || process.env.ORCHESTRATOR_LOCKS_DIR || path.join(model.repoRoot, 'orchestrator', '.cache', 'tasks', 'locks'));
  const relative = path.relative(model.repoRoot, locksDir);
  const locksRel = repoRelative(model.repoRoot, locksDir);
  if (relative === '..' || relative.startsWith('..' + path.sep) || path.isAbsolute(relative)) {
    model.findings.push(finding('LOCK_DIRECTORY_UNSAFE', 'error', null, [locksRel], 'Lock directory escapes the project root.', 'Restore the canonical in-project runtime directory before trusting lock state.'));
    return;
  }
  const maxRuntimeFiles = Number.isSafeInteger(options.maxRuntimeFiles) && options.maxRuntimeFiles > 0
    ? Math.min(options.maxRuntimeFiles, MAX_FILES) : MAX_FILES;
  let beforeDir;
  try {
    beforeDir = model.listDirectory(locksDir, maxRuntimeFiles + 1, model.repoRoot);
  } catch (error) {
    if (error && error.code === 'ENOENT') return;
    model.findings.push(finding('LOCK_DIRECTORY_UNSAFE', 'error', null, [locksRel], 'Lock directory cannot be read safely.', 'Restore the canonical runtime directory before trusting lock state.'));
    return;
  }
  const names = beforeDir.names;
  const scopedStem = options.stem && !model.fullCorpus ? options.stem : null;
  model.snapshotInputs.push({ path: locksRel, kind: 'directory', entriesHash: sha256(canonicalJson(names)) });
  if (beforeDir.truncated || names.length > maxRuntimeFiles) {
    model.findings.push(finding('LOCK_DIRECTORY_TOO_LARGE', 'error', null, [locksRel], 'Lock directory exceeds the bounded runtime entry limit.', 'Recover the exact owning runtime records before retrying validation; never bulk-delete locks by age.'));
    return;
  }
  const completedCounts = new Map();
  for (const name of names) {
    const completed = /^\.(TASK_([1-9][0-9]*)_[A-Za-z0-9_]+)\.json\.released-([a-f0-9]{36})$/.exec(name);
    if (completed && safeIntegerId(completed[1]) !== null) {
      completedCounts.set(completed[1], (completedCounts.get(completed[1]) || 0) + 1);
      continue;
    }
    const lookalike = /^\.(TASK_.+)\.json\.released-/.exec(name);
    if (lookalike && (!scopedStem || lookalike[1] === scopedStem)) {
      model.findings.push(finding('LOCK_RELEASE_RECEIPT_INVALID', 'error', safeIntegerId(lookalike[1]) === null ? null : lookalike[1],
        [repoRelative(model.repoRoot, path.join(locksDir, name))],
        'A completed lock-release receipt has a malformed identity.',
        'Inspect this exact retained receipt; never delete hidden runtime state by age.'));
    }
  }
  for (const [completedStem, count] of completedCounts) {
    if (count > MAX_COMPLETED_LOCK_RELEASES_PER_STEM && (!scopedStem || completedStem === scopedStem)) {
      model.findings.push(finding('LOCK_RELEASE_RECEIPT_LIMIT', 'error', completedStem, [locksRel],
        'Completed lock-release receipts exceed the bounded per-task history limit.',
        'Archive exact authenticated receipts through the owning maintenance flow before another lock mutation.',
        { count, limit: MAX_COMPLETED_LOCK_RELEASES_PER_STEM }));
    }
  }
  for (const name of names) {
    if (/^\.TASK_.+\.json\.released-/.test(name)) continue;
    const recoveryMatch = /^\.(TASK_([1-9][0-9]*)_[A-Za-z0-9_]+)\.json\.release-[a-f0-9]{36}$/.exec(name);
    if (recoveryMatch && safeIntegerId(recoveryMatch[1]) !== null) {
      if (scopedStem && recoveryMatch[1] !== scopedStem) continue;
      model.findings.push(finding('LOCK_RELEASE_RECOVERY_REQUIRED', 'error', recoveryMatch[1], [repoRelative(model.repoRoot, path.join(locksDir, name))], 'A quarantined lock generation remains after an unprovable release race.', 'Inspect the current lock and this private quarantine; recover ownership explicitly without deleting either generation by age.'));
      continue;
    }
    const recoveryLookalike = /^\.(TASK_.+)\.json\.release-/.exec(name);
    if (recoveryLookalike) {
      const recoveryStem = safeIntegerId(recoveryLookalike[1]) === null ? null : recoveryLookalike[1];
      // A parseable foreign stem stays outside a scoped validation. An
      // unparseable identity is global because it cannot be safely attributed
      // to (and therefore hidden from) any one canonical task.
      if (scopedStem && recoveryStem && recoveryStem !== scopedStem) continue;
      model.findings.push(finding('LOCK_RELEASE_RECOVERY_INVALID', 'error', recoveryStem,
        [repoRelative(model.repoRoot, path.join(locksDir, name))],
        'A lock-release recovery lookalike has a malformed identity or generation token.',
        'Inspect this exact retained path; never delete hidden runtime ownership state by age.'));
      continue;
    }
    if (!name.endsWith('.json')) continue;
    const stem = name.slice(0, -5);
    const rel = repoRelative(model.repoRoot, path.join(locksDir, name));
    if (safeIntegerId(stem) === null) {
      model.findings.push(finding('LOCK_INVALID', 'error', null, [rel], 'Lock filename has an invalid task stem.', 'Inspect and remove only the identified runtime record after proving no owner is active.'));
      continue;
    }
    if (scopedStem && stem !== scopedStem) continue;
    let read;
    try { read = model.readArtifact(path.join(locksDir, name), MAX_LOCK_BYTES, model.repoRoot, beforeDir.entries[name]); }
    catch (error) {
      if (error instanceof SnapshotRaceError) throw error;
      model.findings.push(finding('LOCK_UNSAFE', 'error', stem, [rel], 'Lock cannot be read safely.', 'Retry validation from a fresh snapshot.'));
      continue;
    }
    if (read.unsafe || read.tooLarge) { model.findings.push(finding('LOCK_UNSAFE', 'error', stem, [rel], 'Lock is unsafe or oversized.', 'Do not clear it until owner liveness is established.')); continue; }
    let value;
    try { value = JSON.parse(decodeUtf8(read.buffer)); } catch (_) { value = null; }
    const group = model.artifacts.get(stem), state = classify(group);
    const v1Valid = canonicalLockV1(value, stem);
    if (!v1Valid) model.findings.push(finding('LOCK_INVALID', 'error', stem, [rel], 'Lock metadata is malformed.', 'Recover through the owning session/finalization flow; never treat deletion as task repair.'));
    else {
      const generationHash = lockGenerationHash({
        dev: read.stat.dev, ino: read.stat.ino, kind: 'file', mode: read.stat.mode,
        size: read.stat.size, mtimeNs: read.stat.mtimeNs, hash: read.contentHash
      });
      if (!generationHash) throw new ContractError('canonical lock generation proof is invalid');
      model.runtimeLocks.set(stem, Object.freeze({ stage: value.stage, generationHash, path: rel }));
      // The action verdict consumes inode-generation identity in addition to
      // lock bytes, so commit that exact proof to the public snapshot fence.
      model.snapshotInputs.push({ path: rel, kind: 'lock-generation', hash: generationHash });
      const allowed = value.stage === 'task-prep' ? ['backlog', 'pending'] : ['todo'];
      if (!allowed.includes(state)) model.findings.push(finding('LOCK_STAGE_STATE_MISMATCH', 'error', stem, [rel], 'Lock stage is impossible for the current durable task state.', 'Resume the owning recovery transaction or inspect the exact column collision.'));
      // Age affects findings, so commit the resulting threshold class rather
      // than the wall clock itself. This keeps identical validations stable
      // while ensuring that crossing the six-hour boundary changes the fence.
      const old = validationNowMs - Date.parse(value.startedAt) > 6 * 60 * 60 * 1000;
      model.snapshotInputs.push({ path: rel, kind: 'lock-age-classification', old });
      if (old) model.findings.push(finding('LOCK_LIVENESS_UNPROVEN', 'warning', stem, [rel], 'Lock is old; age alone does not prove it is abandoned.', 'Check the owning session or process before clearing this lock.'));
    }
  }
  const afterDir = model.listDirectory(locksDir, beforeDir.limit, model.repoRoot, beforeDir.st);
  if (!sameStat(beforeDir.st, afterDir.st) || beforeDir.truncated !== afterDir.truncated ||
      canonicalJson(beforeDir.names) !== canonicalJson(afterDir.names)) {
    throw new SnapshotRaceError('lock directory changed during scan: ' + locksDir);
  }
}

function mergeRuntimeInspection(model, options) {
  model.runtimeStatus = [];
  model.runtimeStats = { inspected: false, statuses: 0, findings: 0, snapshotInputs: 0, truncated: false };
  if (typeof options.runtimeInspector !== 'function') return;
  const requestedScope = options.stem || 'all';
  try {
    const roots = runtimeRootsFor(options, { repoRoot: model.repoRoot, tasksDir: model.tasksDir });
    const raw = options.runtimeInspector(Object.freeze({ stem: options.stem || null, roots }));
    if (!exactObjectFields(raw, ['findings', 'ok', 'scope', 'snapshotInputs', 'stats', 'statuses', 'truncated', 'version']) ||
        raw.version !== 1 || raw.scope !== requestedScope || typeof raw.ok !== 'boolean' || typeof raw.truncated !== 'boolean' ||
        !Array.isArray(raw.statuses) || !Array.isArray(raw.findings) || !Array.isArray(raw.snapshotInputs) ||
        raw.statuses.length > MAX_RUNTIME_STATUSES || raw.findings.length > MAX_RUNTIME_FINDINGS ||
        raw.snapshotInputs.length > MAX_RUNTIME_SNAPSHOT_INPUTS ||
        !exactObjectFields(raw.stats, ['findings', 'snapshotInputs', 'statuses']) ||
        !Number.isSafeInteger(raw.stats.statuses) || !Number.isSafeInteger(raw.stats.findings) || !Number.isSafeInteger(raw.stats.snapshotInputs) ||
        raw.stats.statuses !== raw.statuses.length || raw.stats.findings !== raw.findings.length ||
        raw.stats.snapshotInputs !== raw.snapshotInputs.length) {
      throw new ContractError('runtime composite envelope is invalid');
    }
    const statuses = raw.statuses.map((status) => {
      if (!exactAllowedFields(status, RUNTIME_STATUS_REQUIRED, RUNTIME_STATUS_OPTIONAL) ||
          !boundedRuntimeString(status.owner, 80) || !boundedRuntimeString(status.kind, 80) ||
          !boundedRuntimeString(status.state, 80) || (status.stem !== null && safeIntegerId(status.stem) === null)) {
        throw new ContractError('runtime composite status is invalid');
      }
      const out = { owner: status.owner, kind: status.kind, state: status.state, stem: status.stem };
      for (const key of RUNTIME_STATUS_OPTIONAL) {
        if (!Object.prototype.hasOwnProperty.call(status, key)) continue;
        if (key === 'revision') {
          if (!Number.isSafeInteger(status[key]) || status[key] < 0) throw new ContractError('runtime composite status revision is invalid');
        } else if (key === 'lockGenerationHash') {
          if (!HASH_RE.test(String(status[key] || ''))) throw new ContractError('runtime composite lock generation hash is invalid');
        } else if (!boundedRuntimeString(status[key], 300, false)) {
          throw new ContractError('runtime composite status field is invalid');
        }
        out[key] = status[key];
      }
      return out;
    }).sort(canonicalOrder);
    const findings = raw.findings.map((item) => {
      if (!exactObjectFields(item, ['code', 'message', 'paths', 'recovery', 'severity', 'stem']) ||
          !boundedRuntimeString(item.code, 120) || !RUNTIME_SEVERITIES.has(item.severity) ||
          (item.stem !== null && safeIntegerId(item.stem) === null) || !Array.isArray(item.paths) ||
          item.paths.length > MAX_RUNTIME_PATHS || item.paths.some((entry) => !boundedRuntimeString(entry, 500, false)) ||
          !boundedRuntimeString(item.message, 1000, false) || !boundedRuntimeString(item.recovery, 1000, false)) {
        throw new ContractError('runtime composite finding is invalid');
      }
      return finding(item.code, item.severity, item.stem, item.paths, item.message, item.recovery);
    });
    const snapshots = raw.snapshotInputs.map((item) => {
      if (!exactAllowedFields(item, ['hash', 'kind', 'owner', 'path'], new Set(['size'])) ||
          !boundedRuntimeString(item.owner, 80) || !boundedRuntimeString(item.kind, 80) ||
          !boundedRuntimeString(item.path, 1000) || !HASH_RE.test(String(item.hash || '')) ||
          (item.size !== undefined && (!Number.isSafeInteger(item.size) || item.size < 0))) {
        throw new ContractError('runtime composite snapshot input is invalid');
      }
      const out = { path: item.path, kind: 'runtime-composite', owner: item.owner, runtimeKind: item.kind, hash: item.hash };
      if (item.size !== undefined) out.size = item.size;
      return out;
    }).sort(canonicalOrder);
    const blocking = findings.some((item) => item.severity === 'error' || item.severity === 'blocker');
    if (raw.ok !== (!raw.truncated && !blocking)) throw new ContractError('runtime composite ok flag is inconsistent');
    model.runtimeStatus = statuses;
    model.runtimeStats = {
      inspected: true,
      statuses: statuses.length,
      findings: findings.length,
      snapshotInputs: snapshots.length,
      truncated: raw.truncated
    };
    model.findings.push(...findings);
    model.snapshotInputs.push(...snapshots);
    if (raw.truncated && !blocking) {
      model.findings.push(finding('RUNTIME_COMPOSITE_TRUNCATED', 'error', null, [],
        'Runtime integrity inspection was truncated before a complete verdict.',
        'Recover or reduce the bounded owner state, then retry a complete inspection.'));
      model.runtimeStats.findings++;
    }
  } catch (_) {
    model.runtimeStatus = [];
    model.runtimeStats = { inspected: true, statuses: 0, findings: 1, snapshotInputs: 0, truncated: false };
    model.findings.push(finding('RUNTIME_INSPECTOR_UNAVAILABLE', 'error', options.stem || null, [],
      'Runtime integrity inspection failed closed.',
      'Retry after the owner-specific runtime inspectors are available and complete.'));
  }
}

function architectureCheckCommand(options = {}) {
  if (options.stem) {
    const error = new Error('architecture freshness is global-only'); error.exitCode = 2; throw error;
  }
  const repoRoot = path.resolve(options.repoRoot || process.env.ORCHESTRATOR_PROJECT_ROOT || path.join(__dirname, '..', '..'));
  const script = path.resolve(options.scriptPath || path.join(__dirname, 'regen-arch.py'));
  const timeout = options.timeoutMs === undefined ? ARCH_CHECK_TIMEOUT_MS : options.timeoutMs;
  if (!Number.isSafeInteger(timeout) || timeout < 1000 || timeout > 60000) {
    const error = new Error('architecture freshness timeout must be between 1000 and 60000ms');
    error.exitCode = 2;
    throw error;
  }
  return {
    command: options.python || process.env.PYTHON || 'python3',
    args: [script, '--check-json'],
    repoRoot,
    spawnOptions: {
    cwd: repoRoot,
    env: Object.assign({}, process.env, { PYTHONDONTWRITEBYTECODE: '1' }),
    encoding: 'utf8',
    timeout,
    maxBuffer: MAX_ARCH_OUTPUT_BYTES
    }
  };
}

function architectureStateFromOutput(repoRoot, status, stdout) {
  let payload;
  try { payload = JSON.parse(stdout); }
  catch (_) { throw new ContractError('architecture freshness check returned invalid machine output'); }
  const hashOrNull = (value) => value === null || HASH_RE.test(String(value || ''));
  const pairIsNull = payload.actualHash === null && payload.actualRevision === null;
  const pairIsHash = HASH_RE.test(String(payload.actualHash || '')) &&
    HASH_RE.test(String(payload.actualRevision || ''));
  if (!exactObjectFields(payload, [
    'actualHash', 'actualRevision', 'expectedHash', 'expectedRevision',
    'fresh', 'path', 'reason', 'status', 'version'
  ]) ||
      payload.version !== 2 || !['fresh', 'stale', 'absent'].includes(payload.status) ||
      typeof payload.fresh !== 'boolean' ||
      !hashOrNull(payload.actualHash) || !hashOrNull(payload.actualRevision) ||
      !hashOrNull(payload.expectedHash) || !hashOrNull(payload.expectedRevision) ||
      payload.fresh !== (payload.status !== 'stale') || (payload.status === 'stale') !== (status === 1) ||
      payload.path !== 'orchestrator/.arch-map.json' ||
      (payload.reason !== null && (typeof payload.reason !== 'string' || payload.reason.length > 100)) ||
      (payload.status === 'fresh' && (
        payload.reason !== null || !pairIsHash ||
        payload.actualHash !== payload.expectedHash ||
        payload.actualRevision !== payload.expectedRevision
      )) ||
      (payload.status === 'absent' && (
        payload.reason !== 'pre-bootstrap' || !pairIsNull ||
        payload.expectedHash !== null || payload.expectedRevision !== null
      )) ||
      (payload.status === 'stale' && (
        !['missing-or-invalid', 'source-revision-drift', 'structural-drift'].includes(payload.reason) ||
        !HASH_RE.test(String(payload.expectedHash || '')) ||
        !HASH_RE.test(String(payload.expectedRevision || '')) ||
        (payload.reason === 'missing-or-invalid' && !pairIsNull) ||
        (payload.reason !== 'missing-or-invalid' && !pairIsHash) ||
        (payload.reason === 'source-revision-drift' &&
          payload.actualRevision === payload.expectedRevision) ||
        (payload.reason === 'structural-drift' && (
          payload.actualRevision !== payload.expectedRevision ||
          payload.actualHash === payload.expectedHash
        ))
      ))) {
    throw new ContractError('architecture freshness check returned an invalid contract');
  }
  let publicReason = payload.reason;
  if (payload.reason === 'missing-or-invalid') {
    try {
      fs.lstatSync(path.join(repoRoot, payload.path));
      publicReason = 'unreadable-or-invalid';
    } catch (error) {
      publicReason = error && error.code === 'ENOENT' ? 'missing' : 'unreadable-or-invalid';
    }
  }
  const findings = payload.status === 'stale' ? [{
    code: 'ARCH_MAP_STALE',
    severity: 'error',
    paths: [payload.path],
    message: 'Derived architecture map does not match a fresh structural scan.',
    recovery: 'Regenerate it with python3 orchestrator/tasks/regen-arch.py after task state is stable.',
    details: { reason: publicReason || 'structural-drift' }
  }] : [];
  return {
    version: 1,
    checked: true,
    ok: payload.fresh,
    status: payload.status,
    fresh: payload.fresh,
    actualHash: payload.actualHash,
    expectedHash: payload.expectedHash,
    findings
  };
}

function checkArchitectureState(options = {}) {
  const invocation = architectureCheckCommand(options);
  const child = childProcess.spawnSync(invocation.command, invocation.args, invocation.spawnOptions);
  if (child.error || ![0, 1].includes(child.status)) {
    throw new ContractError('architecture freshness check could not complete safely: ' +
      String(child.error && child.error.message || child.stderr || ('exit ' + child.status)).slice(0, 300));
  }
  return architectureStateFromOutput(invocation.repoRoot, child.status, child.stdout);
}

function checkArchitectureStateAsync(options = {}) {
  let invocation;
  try { invocation = architectureCheckCommand(options); }
  catch (error) { return Promise.reject(error); }
  return new Promise((resolve, reject) => {
    childProcess.execFile(invocation.command, invocation.args, invocation.spawnOptions,
      function (error, stdout, stderr) {
        const status = error ? Number(error.code) : 0;
        if ((error && ![0, 1].includes(status)) || ![0, 1].includes(status)) {
          reject(new ContractError('architecture freshness check could not complete safely: ' +
            String(error && error.message || stderr || ('exit ' + status)).slice(0, 300)));
          return;
        }
        try { resolve(architectureStateFromOutput(invocation.repoRoot, status, stdout)); }
        catch (contractError) { reject(contractError); }
      });
  });
}

function scopeFindingApplies(item, stem) {
  if (!stem) return true;
  if (item.stem === stem) return true;
  // Some corpus findings are global only in reporting shape because they
  // describe more than one exact task (number/alias collisions). Their
  // bounded details.stems set is the action scope; unrelated tasks remain
  // executable. Findings without such a set are genuinely global.
  if (item.details && Array.isArray(item.details.stems)) {
    return item.details.stems.includes(stem);
  }
  return item.stem === null;
}

function validateTaskState(options = {}) {
  platformSupport.assertCanonicalTaskPlatform();
  const started = Date.now();
  const stem = options.stem || null;
  if (stem && safeIntegerId(stem) === null) {
    const error = new Error('stem must be a canonical TASK_<N>_<slug> identity'); error.exitCode = 2; throw error;
  }
  let expectedState = options.expect || null;
  const transition = options.transition || null;
  const phase = options.phase || null;
  if (expectedState && (!stem || !STATES.includes(expectedState))) {
    const error = new Error('--expect requires a canonical stem and supported state'); error.exitCode = 2; throw error;
  }
  if (transition) {
    if (!stem || !phase || !['pre', 'post'].includes(phase) || !ALLOWED_TRANSITIONS.has(transition)) {
      const error = new Error('unsupported transition or missing --stem/--phase'); error.exitCode = 2; throw error;
    }
    const [from, to] = transition.split(':');
    expectedState = phase === 'pre' ? from : to;
  } else if (phase) {
    const error = new Error('--phase requires --transition'); error.exitCode = 2; throw error;
  }

  const model = scanCorpus(options);
  if (options.proposal) applyProposal(model, options.proposal);
  validateContent(model);
  if (options.checkIndex) checkIndex(model);
  else model.indexStatus = 'unchecked';
  if (options.includeRuntime) {
    checkRuntime(model, options);
    mergeRuntimeInspection(model, options);
  } else {
    model.runtimeStatus = [];
    model.runtimeStats = { inspected: false, statuses: 0, findings: 0, snapshotInputs: 0, truncated: false };
  }

  const group = stem ? model.artifacts.get(stem) : null;
  const observedState = stem ? classify(group) : null;
  if (expectedState && observedState !== expectedState) {
    model.findings.push(finding(transition ? (phase === 'pre' ? 'TRANSITION_PRECONDITION_FAILED' : 'TRANSITION_POSTCONDITION_FAILED') : 'TASK_STATE_MISMATCH', 'blocker', stem,
      group ? COLUMNS.filter((c) => group[c]).map((c) => group[c].relPath) : [],
      'Observed state ' + observedState + ' does not match expected state ' + expectedState + '.',
      'Refresh task state and retry only from the documented lifecycle transition.'));
  }
  let findings = model.findings.filter((item) => scopeFindingApplies(item, stem));
  findings.sort(findingOrder);
  const sourceRevision = stem ? revisionFor(group) : null;
  // snapshotHash covers every bounded input used by this verdict, including
  // INDEX.json in --check-index mode, global name inventories, and any
  // matching/full-scan in-flight temporary bytes.
  const snapshotHash = sha256('task-state-snapshot-v1\0' + canonicalJson(model.snapshotInputs));
  const ok = !findings.some((item) => item.severity === 'error' || item.severity === 'blocker');
  const affectedStems = Array.from(new Set(findings.map((item) => item.stem).filter(Boolean).concat(findings.flatMap((item) => item.details && Array.isArray(item.details.stems) ? item.details.stems : [])))).sort();
  const result = {
    version: VERSION,
    ok,
    scope: stem || 'all',
    observedState,
    expectedState,
    transition,
    phase,
    snapshotHash,
    sourceRevision,
    indexStatus: model.indexStatus,
    runtimeStatus: model.runtimeStatus,
    runtimeStats: model.runtimeStats,
    affectedStems,
    findings,
    stats: {
      tasks: model.artifacts.size,
      files: model.files.length,
      inventoryEntries: model.ioStats.inventoryEntries,
      taskRelatedEntries: model.ioStats.taskRelatedEntries,
      taskBodyReads: model.ioStats.taskBodyReads,
      taskBodyBytes: model.ioStats.taskBodyBytes,
      scanMode: model.fullCorpus ? 'full' : 'stem-closure',
      durationMs: Date.now() - started
    }
  };
  Object.defineProperty(result, '_model', { value: model, enumerable: false });
  return result;
}

function applyFinalizeRecoveryAdmission(result, model, stem) {
  if (result.observedState !== 'done') return;
  const markerStatuses = (result.runtimeStatus || []).filter((status) =>
    status.owner === 'finalizations' && status.kind === 'marker' && status.stem === stem);
  const marker = markerStatuses.length === 1 && markerStatuses[0].state === 'running' &&
    HASH_RE.test(String(markerStatuses[0].lockGenerationHash || '')) ? markerStatuses[0] : null;
  const observedLock = model && model.runtimeLocks && model.runtimeLocks.get(stem);
  const lock = observedLock && observedLock.stage === 'orchestrator' &&
    HASH_RE.test(String(observedLock.generationHash || '')) ? observedLock : null;

  if (!marker) {
    result.findings.push(finding('FINALIZE_RECOVERY_MARKER_UNPROVEN', 'blocker', stem, [],
      'Finalize recovery from done requires one provably active finalization marker.',
      'Resume the canonical finalizer so it can claim the exact durable marker, then retry from a fresh runtime snapshot.'));
  }
  if (!lock) {
    result.findings.push(finding('FINALIZE_RECOVERY_LOCK_UNPROVEN', 'blocker', stem,
      observedLock && observedLock.path ? [observedLock.path] : [],
      'Finalize recovery from done requires the retained canonical orchestrator lock.',
      'Recover through the canonical finalizer; do not create, replace, or remove a task lock by hand.'));
  } else if (marker && marker.lockGenerationHash !== lock.generationHash) {
    result.findings.push(finding('FINALIZE_RECOVERY_LOCK_GENERATION_MISMATCH', 'blocker', stem, [lock.path],
      'The active finalization marker captured a different task-lock generation.',
      'Stop and reconcile the exact marker and retained lock ownership; never let one finalizer release another owner\'s lock.'));
  } else if (marker) {
    // A done task normally makes an orchestrator lock impossible. This one
    // narrow action-specific exception is valid only after the active marker
    // and the exact retained lock generation have matched. Preserve every
    // other structural/runtime finding.
    result.findings = result.findings.filter((item) =>
      !(item.code === 'LOCK_STAGE_STATE_MISMATCH' && item.stem === stem));
  }
}

function validateAction(options = {}) {
  const action = options.action;
  if (!Object.prototype.hasOwnProperty.call(ACTION_STATES, action)) { const e = new Error('unsupported task action'); e.exitCode = 2; throw e; }
  const result = validateTaskState(Object.assign({}, options, { stem: options.stem }));
  const allowed = ACTION_STATES[action];
  if (!allowed.includes(result.observedState)) {
    result.findings.push(finding('REQUEST_ACTION_STATE_MISMATCH', 'blocker', options.stem, [], 'Action ' + action + ' is not allowed from ' + result.observedState + '.', 'Refresh the board and use the action exposed for the current canonical state.', { allowedStates: allowed }));
  }
  const model = result._model;
  if (action === 'finalize') applyFinalizeRecoveryAdmission(result, model, options.stem);
  const meta = model && model.metadata && model.metadata.get(options.stem);
  // Finalize(todo) is a fresh execution/ship admission and must preserve the
  // same accepted-dependency gate as run. Finalize(done) is recovery-only: a
  // dependency reopened after the move must not strand marker cleanup.
  if ((action === 'run' || (action === 'finalize' && result.observedState === 'todo')) && meta) {
    const unresolved = result.findings.filter((item) => item.code === 'DEPENDENCY_UNRESOLVED' && item.stem === options.stem);
    for (const item of unresolved) result.findings.push(finding('RUN_DEPENDENCY_UNSATISFIED', 'blocker', options.stem, item.paths, item.message, item.recovery, item.details));
  }
  const dependents = [];
  if (model && model.metadata) {
    for (const [candidate, candidateMeta] of model.metadata.entries()) if (candidate !== options.stem && candidateMeta.state !== 'done' && candidateMeta.deps.includes(options.stem)) dependents.push(candidate);
  }
  if (action === 'drop' && dependents.length) {
    result.findings.push(finding('DROP_DEPENDENTS_PRESENT', 'warning', options.stem, [], 'Live tasks depend on this task.', 'Show this impact before explicit drop confirmation.', { dependents: dependents.sort() }));
  }
  result.action = action;
  result.dependents = dependents.sort();
  result.findings.sort(findingOrder);
  result.ok = !result.findings.some((item) => item.severity === 'error' || item.severity === 'blocker');
  result.affectedStems = Array.from(new Set(result.affectedStems.concat(result.findings.map((item) => item.stem).filter(Boolean)))).sort();
  return result;
}

function actionAdmission(result) {
  const blockers = (Array.isArray(result && result.findings) ? result.findings : []).filter((item) =>
    item && (item.severity === 'error' || item.severity === 'blocker') &&
    !/^INDEX_/.test(String(item.code || '')));
  return { ok: !!result && blockers.length === 0, blockers };
}

// Drop is a repair operation as well as a lifecycle action. Structural task
// content and INDEX findings describe bytes the operation is explicitly about
// to remove (or a projection it can safely leave stale); they must not make
// that task undeletable. Filesystem identity, ambiguous ownership and runtime
// owner findings remain hard blockers.
const DROP_REPAIRABLE_FINDING_RE = /^(?:INDEX_|DEPENDENCY_|LINEAGE_|PENDING_|TODO_|DONE_|OUTCOME_|TASK_HEADING_|TASK_SOURCE_|TASK_TITLE_|TASK_ARTIFACT_(?:UTF8_INVALID|TOO_LARGE)$|TASK_FILENAME_UNSUPPORTED$|TASK_PRESENT_IN_MULTIPLE_STATES$|TASK_STEM_ALIAS_COLLISION$|TASK_NUMBER_CONFLICT$|REQUEST_ACTION_STATE_MISMATCH$)/;
const DROP_HARD_FINDING_RE = /^(?:DURABLE_CAS_|FINALIZE_|LOCK_|RUNTIME_|TASK_ROOT_UNSAFE$|TASK_COLUMN_UNSAFE$|TASK_ARTIFACT_UNSAFE$|TASK_FILE_OUTSIDE_COLUMN$|TASK_TEMP_)/;

function oversizedDropGenerationAvailable(result, stem) {
  const group = result && result._model && result._model.artifacts &&
    result._model.artifacts.get(stem);
  if (!group) return false;
  const oversized = COLUMNS.map((column) => group[column]).filter((record) =>
    record && record.inventoryTooLarge === true);
  return oversized.length > 0 && oversized.every((record) => record.stat &&
    record.stat.isFile() && !record.stat.isSymbolicLink() &&
    Number.isSafeInteger(record.stat.size) && record.stat.size > MAX_TASK_BYTES);
}

function dropAdmission(result, stem, options = {}) {
  const allowAbsent = options.allowAbsent === true;
  const allowedStates = allowAbsent ? ACTION_STATES.drop.concat('absent') : ACTION_STATES.drop;
  const blockers = (Array.isArray(result && result.findings) ? result.findings : []).filter((item) => {
    if (!item || (item.severity !== 'error' && item.severity !== 'blocker')) return false;
    if (DROP_HARD_FINDING_RE.test(String(item.code || ''))) return true;
    if (item.code === 'TASK_ARTIFACT_TOO_LARGE' &&
        !oversizedDropGenerationAvailable(result, stem)) return true;
    return !DROP_REPAIRABLE_FINDING_RE.test(String(item.code || ''));
  });
  if (!result || result.scope !== stem || !allowedStates.includes(result.observedState)) {
    blockers.push(finding('DROP_TARGET_UNAVAILABLE', 'blocker', stem, [],
      'The exact task identity and lifecycle state could not be proven.',
      'Refresh the task state and retry deletion from the canonical card.'));
  }
  if (!allowAbsent && !HASH_RE.test(String(result && result.sourceRevision || ''))) {
    blockers.push(finding('DROP_SOURCE_REVISION_UNAVAILABLE', 'blocker', stem, [],
      'The exact source generation could not be frozen for deletion.',
      'Restore a readable canonical task artifact, then retry deletion.'));
  }
  blockers.sort(findingOrder);
  return { ok: blockers.length === 0, blockers };
}

function admissionForAction(result, stem, options = {}) {
  return result && result.action === 'drop'
    ? dropAdmission(result, stem, options)
    : actionAdmission(result);
}

function dropImpactHash(stem, sourceRevision, dependents) {
  if (safeIntegerId(stem) === null || !HASH_RE.test(String(sourceRevision || '')) ||
      !Array.isArray(dependents) || dependents.some((item) => safeIntegerId(item) === null)) {
    const error = new Error('invalid drop-impact inputs'); error.exitCode = 2; throw error;
  }
  const canonicalDependents = Array.from(new Set(dependents)).sort();
  return sha256('drop-impact-v1\0' + stem + '\0' + sourceRevision + '\0' + canonicalJson(canonicalDependents));
}

module.exports = {
  VERSION,
  INDEX_VERSION,
  STEM_RE,
  STEM_MAX,
  HASH_RE,
  STATES,
  COLUMNS,
  ALLOWED_TRANSITIONS,
  ACTION_STATES,
  ContractError,
  SnapshotRaceError,
  sha256,
  canonicalJson,
  lockGenerationHash,
  safeIntegerId,
  canonicalLockV1,
  sameExactStatProof: sameStat,
  readAnchoredFile,
  observationFor,
  projectObservation,
  projectObservationStream,
  structuralText,
  scanAtxHeadings: designParser.scanAtxHeadings,
  outcomeAppendixStart,
  parseHeading,
  parsePending,
  TASK_QUESTIONS_SECTION,
  TASK_QUESTION_TYPES,
  MAX_TASK_QUESTIONS,
  parseTaskQuestions,
  taskQuestionsIssue,
  taskBodyStructureOpen,
  taskQuestionsIdentity,
  taskQuestionsProjection,
  parseOutcome,
  parseDependencies,
  parseLineage,
  classify,
  validateTaskState,
  validateAction,
  actionAdmission,
  dropAdmission,
  admissionForAction,
  checkArchitectureState,
  checkArchitectureStateAsync,
  dropImpactHash,
  deriveIndex,
  finding,
  findingOrder
};
