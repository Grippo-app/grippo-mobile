'use strict';

// Exact per-environment opaque credential store. Secret bytes cross this
// module only for guarded reads/writes and are never included in public status,
// logs, reports, prompts, argv or environment variables.

var fs = require('fs');
var path = require('path');
var crypto = require('crypto');
var os = require('os');
var paths = require('./paths');
var environments = require('./backend-environments');
var writerLeases = require('../../tasks/writer-leases.cjs');
var fileGuards = require('./file-guards');

var SECRETS_DIR = path.join(paths.API_CONTRACT_DIR, '.secrets');
var STATE_FILE = path.join(paths.API_CONTRACT_CACHE_DIR, 'credential-state.json');
var MUTEX_DIR = path.join(paths.API_CONTRACT_CACHE_DIR, 'credential-state-mutex');
var MUTEX_FILE = path.join(MUTEX_DIR, 'credential-state.lock');
var MAX_SECRET_BYTES = 16 * 1024;
var MAX_STATE_BYTES = 128 * 1024;
var stateMutex = Promise.resolve();
var mutexWait = new Int32Array(new SharedArrayBuffer(4));

function initialState() { return { schemaVersion: 1, revisions: {}, fileProofs: {}, idempotency: [] }; }
function sha(value) { return 'sha256:' + crypto.createHash('sha256').update(value).digest('hex'); }
function exactKeys(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}
function validIdempotencyKey(value) { return /^[A-Za-z0-9._:-]{8,128}$/.test(String(value || '')); }
function modeOf(st) { return typeof st.mode === 'bigint' ? Number(st.mode & 0o777n) : (st.mode & 0o777); }
function statProof(st) { return [String(st.dev), String(st.ino), String(st.size), String(st.mtimeNs), String(st.ctimeNs)].join(':'); }
function sameFileGeneration(left, right) {
  return left && right && left.isFile() && right.isFile() && !left.isSymbolicLink() && !right.isSymbolicLink() &&
    left.dev === right.dev && left.ino === right.ino && left.mode === right.mode && left.nlink === right.nlink &&
    left.size === right.size && left.mtimeNs === right.mtimeNs && left.ctimeNs === right.ctimeNs;
}
function parentAnchored(file) {
  var root = path.resolve(paths.PROJECT_ROOT), parent = path.resolve(path.dirname(file));
  var rel = path.relative(root, parent);
  if (rel === '..' || rel.startsWith('..' + path.sep) || path.isAbsolute(rel)) return false;
  return fs.realpathSync(parent) === path.join(fs.realpathSync(root), rel);
}
function guardedBytes(file, maxBytes, requiredMode) {
  var fd;
  try {
    if (!parentAnchored(file)) throw new Error('credential-parent-unsafe');
    var before = fs.lstatSync(file, { bigint: true });
    if (!before.isFile() || before.isSymbolicLink() || before.nlink !== 1n || before.size < 1n || before.size > BigInt(maxBytes) ||
        (requiredMode !== null && modeOf(before) !== requiredMode)) throw new Error('credential-file-unsafe');
    fd = fs.openSync(file, fs.constants.O_RDONLY | (fs.constants.O_NOFOLLOW || 0));
    var opened = fs.fstatSync(fd, { bigint: true });
    if (!sameFileGeneration(before, opened)) throw new Error('credential-file-raced');
    var bytes = Buffer.alloc(Number(opened.size)), offset = 0;
    while (offset < bytes.length) { var count = fs.readSync(fd, bytes, offset, bytes.length - offset, offset); if (!count) throw new Error('credential-short-read'); offset += count; }
    var afterFd = fs.fstatSync(fd, { bigint: true }), afterPath = fs.lstatSync(file, { bigint: true });
    if (!sameFileGeneration(opened, afterFd) || !sameFileGeneration(opened, afterPath) || !parentAnchored(file)) throw new Error('credential-file-raced');
    return bytes;
  } finally { if (fd !== undefined) try { fs.closeSync(fd); } catch (ignore) {} }
}

function secretPath(id) {
  if (environments.IDS.indexOf(id) < 0) throw new Error('environment-id-invalid');
  return path.join(SECRETS_DIR, id + '.token');
}

function inspectFile(file, allowMissing) {
  var st;
  try { if (!parentAnchored(file)) throw new Error('credential-parent-unsafe'); st = fs.lstatSync(file, { bigint: true }); }
  catch (e) { if (allowMissing && e && e.code === 'ENOENT') return { exists: false, proof: 'absent' }; throw e; }
  if (!st.isFile() || st.isSymbolicLink() || st.nlink !== 1n) throw new Error('credential-file-unsafe');
  if (modeOf(st) !== 0o600) throw new Error('credential-mode-invalid');
  if (st.size < 1n || st.size > BigInt(MAX_SECRET_BYTES)) throw new Error('credential-size-invalid');
  return { exists: true, proof: statProof(st) };
}

function validateSecret(secret) {
  if (typeof secret !== 'string' || /[\0\r\n]/.test(secret)) return { ok: false, error: 'credential-value-invalid' };
  var bytes = Buffer.from(secret, 'utf8');
  if (bytes.length < 1 || bytes.length > MAX_SECRET_BYTES || bytes.toString('utf8') !== secret) {
    return { ok: false, error: 'credential-value-invalid' };
  }
  return { ok: true, bytes: bytes };
}

function validateState(value) {
  if (!exactKeys(value, ['fileProofs', 'idempotency', 'revisions', 'schemaVersion']) || value.schemaVersion !== 1 ||
      !value.revisions || typeof value.revisions !== 'object' || Array.isArray(value.revisions) ||
      !value.fileProofs || typeof value.fileProofs !== 'object' || Array.isArray(value.fileProofs) ||
      !Array.isArray(value.idempotency) || value.idempotency.length > 100) return false;
  var ids = Object.keys(value.revisions).concat(Object.keys(value.fileProofs));
  if (ids.some(function (id) { return environments.IDS.indexOf(id) < 0; })) return false;
  if (Object.keys(value.revisions).some(function (id) { return !Number.isSafeInteger(value.revisions[id]) || value.revisions[id] < 0; })) return false;
  if (Object.keys(value.fileProofs).some(function (id) { return value.fileProofs[id] !== null && (typeof value.fileProofs[id] !== 'string' || value.fileProofs[id].length > 160); })) return false;
  return value.idempotency.every(function (row) {
    return exactKeys(row, ['createdAt', 'environmentId', 'fingerprint', 'key', 'revision']) &&
      validIdempotencyKey(row.key) && /^sha256:[0-9a-f]{64}$/.test(row.fingerprint) &&
      environments.IDS.indexOf(row.environmentId) >= 0 && Number.isSafeInteger(row.revision) && row.revision >= 0 &&
      typeof row.createdAt === 'string' && Number.isFinite(Date.parse(row.createdAt));
  });
}

function safeReadState() {
  try {
    var parsed = JSON.parse(guardedBytes(STATE_FILE, MAX_STATE_BYTES, 0o600).toString('utf8'));
    if (!validateState(parsed)) throw new Error('credential-state-invalid');
    return parsed;
  } catch (e) {
    if (e && e.code === 'ENOENT') return initialState();
    throw new Error('credential-state-invalid');
  }
}

function ensureSecretsDir() {
  if (!fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, SECRETS_DIR, { create: true, mode: 0o700 })) throw new Error('credential-directory-unsafe');
  var st = fs.lstatSync(SECRETS_DIR);
  if (!st.isDirectory() || st.isSymbolicLink()) throw new Error('credential-directory-unsafe');
  if (modeOf(st) !== 0o700) fs.chmodSync(SECRETS_DIR, 0o700);
  var real = fs.realpathSync(SECRETS_DIR);
  var contractReal = fs.realpathSync(paths.API_CONTRACT_DIR);
  if (real !== path.join(contractReal, '.secrets')) throw new Error('credential-directory-unsafe');
}

function atomicWrite(file, data, mode) {
  var maxBytes = file === STATE_FILE ? MAX_STATE_BYTES : MAX_SECRET_BYTES;
  var published = fileGuards.atomicReplaceRegularFileResult(paths.PROJECT_ROOT, path.dirname(file), file, data,
    { create: true, directoryMode: 0o700, mode: mode, maxBytes: maxBytes });
  if (!published.ok) throw new Error(published.code || 'credential-write-failed');
}

function writeState(value) { atomicWrite(STATE_FILE, Buffer.from(JSON.stringify(value, null, 2) + '\n'), 0o600); }

// Cross-process exact-file mutex. Acquisition publishes a private candidate
// with link(2), so the canonical path is no-clobber. Release first hard-links
// the exact owned inode to a private recovery name, rechecks token+inode, and
// only then unlinks the canonical name. That prevents a stale releaser from
// deleting a newer lock generation. Crash recovery applies the same protocol
// only after exact process-generation death has been proven.
function ensureMutexDir() {
  if (!fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, MUTEX_DIR, { create: true, mode: 0o700 })) throw new Error('credential-state-mutex-directory-unsafe');
  var st = fs.lstatSync(MUTEX_DIR);
  if (!st.isDirectory() || st.isSymbolicLink()) throw new Error('credential-state-mutex-directory-unsafe');
  if (modeOf(st) !== 0o700) fs.chmodSync(MUTEX_DIR, 0o700);
  var projectPath = path.resolve(paths.PROJECT_ROOT), projectReal = fs.realpathSync(projectPath);
  var lexicalRel = path.relative(projectPath, path.resolve(MUTEX_DIR));
  if (fs.realpathSync(MUTEX_DIR) !== path.join(projectReal, lexicalRel)) throw new Error('credential-state-mutex-directory-unsafe');
  var rel = path.relative(projectReal, fs.realpathSync(MUTEX_DIR));
  if (!rel || rel === '..' || rel.startsWith('..' + path.sep) || path.isAbsolute(rel)) throw new Error('credential-state-mutex-directory-unsafe');
}
function mutexBytes(record) { return Buffer.from(JSON.stringify(record) + '\n'); }
function validMutexRecord(record) {
  return exactKeys(record, ['createdAt', 'hostname', 'pid', 'processStartId', 'schemaVersion', 'token']) && record.schemaVersion === 1 &&
    Number.isInteger(record.pid) && record.pid > 0 && record.pid <= 0x7fffffff &&
    (record.processStartId === null || writerLeases.PROCESS_START_ID_RE.test(String(record.processStartId))) &&
    typeof record.hostname === 'string' && record.hostname.length > 0 && record.hostname.length <= 255 &&
    /^[a-f0-9]{48}$/.test(String(record.token || '')) && typeof record.createdAt === 'string' && Number.isFinite(Date.parse(record.createdAt));
}
function sameMutexProof(a, b) { return a && b && a.dev === b.dev && a.ino === b.ino; }
function readMutex() {
  var before;
  try { before = fs.lstatSync(MUTEX_FILE); }
  catch (e) { if (e && e.code === 'ENOENT') return null; throw e; }
  if (!before.isFile() || before.isSymbolicLink() || before.nlink < 1 || before.nlink > 8 || before.size < 2 || before.size > 4096 || modeOf(before) !== 0o600) {
    throw new Error('credential-state-mutex-unsafe');
  }
  var fd = fs.openSync(MUTEX_FILE, fs.constants.O_RDONLY | fs.constants.O_NOFOLLOW), bytes, held;
  try {
    held = fs.fstatSync(fd);
    if (!sameMutexProof(before, held) || !held.isFile() || held.size !== before.size) throw new Error('credential-state-mutex-raced');
    bytes = fs.readFileSync(fd);
  } finally { fs.closeSync(fd); }
  var record;
  try { record = JSON.parse(bytes.toString('utf8')); } catch (e2) { throw new Error('credential-state-mutex-unsafe'); }
  if (!validMutexRecord(record)) throw new Error('credential-state-mutex-unsafe');
  return { record: record, bytes: bytes, proof: { dev: before.dev, ino: before.ino } };
}
function ownerGone(record) {
  if (record.hostname !== os.hostname()) return false;
  var state = writerLeases.processIdentityState(record.pid, record.processStartId);
  return state === 'dead' || state === 'reused';
}
function unlinkExactMutex(held, requireOwner) {
  if (requireOwner && (held.record.pid !== process.pid || held.record.hostname !== os.hostname() ||
      held.record.processStartId !== writerLeases.captureProcessStartId(process.pid))) return false;
  if (!requireOwner && !ownerGone(held.record)) return false;
  var alias = path.join(MUTEX_DIR, '.credential-state.recovery-' + process.pid + '-' + crypto.randomBytes(12).toString('hex'));
  try {
    fs.linkSync(MUTEX_FILE, alias);
    var live = readMutex();
    var aliasStat = fs.lstatSync(alias);
    if (!live || !sameMutexProof(live.proof, held.proof) || !sameMutexProof(aliasStat, held.proof) || !live.bytes.equals(held.bytes) ||
        (requireOwner ? live.record.token !== held.record.token : !ownerGone(live.record))) return false;
    fs.unlinkSync(MUTEX_FILE);
    return true;
  } catch (e) {
    if (e && e.code === 'ENOENT') return false;
    throw e;
  } finally { try { fs.unlinkSync(alias); } catch (ignore) {} }
}
function acquireStateMutex() {
  ensureMutexDir();
  var deadline = Date.now() + 5000;
  while (Date.now() < deadline) {
    var held = readMutex();
    if (held) {
      if (ownerGone(held.record)) { unlinkExactMutex(held, false); continue; }
      Atomics.wait(mutexWait, 0, 0, 20); continue;
    }
    var record = { schemaVersion: 1, pid: process.pid, processStartId: writerLeases.captureProcessStartId(process.pid),
      hostname: os.hostname(), token: crypto.randomBytes(24).toString('hex'), createdAt: new Date().toISOString() };
    if (!validMutexRecord(record)) throw new Error('credential-state-mutex-owner-identity-unavailable');
    var bytes = mutexBytes(record);
    var candidate = path.join(MUTEX_DIR, '.credential-state.candidate-' + process.pid + '-' + crypto.randomBytes(12).toString('hex'));
    var fd = fs.openSync(candidate, fs.constants.O_CREAT | fs.constants.O_EXCL | fs.constants.O_WRONLY | fs.constants.O_NOFOLLOW, 0o600);
    try { fs.writeFileSync(fd, bytes); fs.fsyncSync(fd); } finally { fs.closeSync(fd); }
    try {
      fs.linkSync(candidate, MUTEX_FILE);
      fs.unlinkSync(candidate);
      var published = readMutex();
      if (!published || published.record.token !== record.token || !published.bytes.equals(bytes)) throw new Error('credential-state-mutex-publication-raced');
      return published;
    } catch (e2) {
      try { fs.unlinkSync(candidate); } catch (ignore2) {}
      if (e2 && e2.code !== 'EEXIST') throw e2;
    }
  }
  throw new Error('credential-state-mutex-timeout');
}
function withStateMutex(fn) {
  var held = acquireStateMutex();
  try { return fn(); }
  finally {
    var current = readMutex();
    if (!current || current.record.token !== held.record.token || !unlinkExactMutex(current, true)) {
      throw new Error('credential-state-mutex-release-failed');
    }
  }
}

function reconcileRevision(state, id, inspection) {
  var stored = Object.prototype.hasOwnProperty.call(state.fileProofs, id) ? state.fileProofs[id] : null;
  var current = inspection.exists ? inspection.proof : null;
  if (stored === current) return false;
  state.revisions[id] = (state.revisions[id] || 0) + 1;
  state.fileProofs[id] = current;
  return true;
}

function guardedRead(file) {
  var bytes = guardedBytes(file, MAX_SECRET_BYTES, 0o600), value = bytes.toString('utf8');
  if (!Buffer.from(value, 'utf8').equals(bytes) || Buffer.byteLength(value, 'utf8') < 1 ||
      Buffer.byteLength(value, 'utf8') > MAX_SECRET_BYTES || /[\0\r\n]/.test(value)) throw new Error('credential-value-invalid');
  return value;
}

function credentialKind(environment) {
  return environment.authRef === null ? 'none' : (environment.authKind || 'bearer');
}

function projectedStatus(environment, state, inspection, invalid) {
  var dormant = inspection.exists && environment.authRef === null;
  var authState = environment.authRef === null ? 'not-required' : (invalid ? 'invalid' : (inspection.exists ? 'configured' : 'missing'));
  return { state: authState, kind: credentialKind(environment), checkedAt: null,
    revision: state.revisions[environment.id] || 0, dormant: dormant };
}

function publicStatusUnlocked(environment) {
  if (!environment) return { state: 'unknown', kind: 'none', checkedAt: null, revision: 0, dormant: false };
  var state = safeReadState(), inspection, invalid = null;
  try { inspection = inspectFile(secretPath(environment.id), true); }
  catch (e) { inspection = { exists: true, proof: 'invalid' }; invalid = e.message; }
  if (reconcileRevision(state, environment.id, inspection)) writeState(state);
  return projectedStatus(environment, state, inspection, invalid);
}

function publicStatus(environment) {
  if (!environment) return publicStatusUnlocked(environment);
  try {
    var state = safeReadState(), inspection, invalid = null;
    try { inspection = inspectFile(secretPath(environment.id), true); }
    catch (e) { inspection = { exists: true, proof: 'invalid' }; invalid = e.message; }
    var stored = Object.prototype.hasOwnProperty.call(state.fileProofs, environment.id) ? state.fileProofs[environment.id] : null;
    var current = inspection.exists ? inspection.proof : null;
    return stored === current ? projectedStatus(environment, state, inspection, invalid)
      : withStateMutex(function () { return publicStatusUnlocked(environment); });
  } catch (stateError) {
    return { state: 'invalid', kind: credentialKind(environment), checkedAt: null,
      revision: 0, dormant: false };
  }
}

function currentRevision(id) {
  var envState = environments.read();
  var environment = environments.environmentById(envState, id);
  return publicStatus(environment).revision;
}

function readForJob(environment, expectedRevision) {
  try {
    return withStateMutex(function () {
      var status = publicStatusUnlocked(environment);
      if (status.revision !== expectedRevision) return { ok: false, error: 'auth-revision-conflict', status: status };
      if (environment.authRef === null) return { ok: true, secret: null, status: status };
      if (status.state !== 'configured') return { ok: false, error: status.state === 'invalid' ? 'auth-invalid' : 'auth-missing', status: status };
      try { return { ok: true, secret: guardedRead(secretPath(environment.id)), status: status }; }
      catch (e) { return { ok: false, error: 'auth-invalid', status: publicStatusUnlocked(environment) }; }
    });
  } catch (stateError) {
    return { ok: false, error: 'auth-invalid', status: publicStatus(environment) };
  }
}

function mutateSync(request) {
  if (!request || typeof request !== 'object' || Array.isArray(request)) return { ok: false, status: 400, error: 'bad-request' };
  var expectedKeys = ['environmentId', 'expectedAuthRevision', 'idempotencyKey', 'operation'].concat(request.operation === 'set' ? ['secret'] : []);
  if (!exactKeys(request, expectedKeys)) return { ok: false, status: 400, error: 'bad-request-fields' };
  if (environments.IDS.indexOf(request.environmentId) < 0 || !validIdempotencyKey(request.idempotencyKey) ||
      !Number.isSafeInteger(request.expectedAuthRevision) || request.expectedAuthRevision < 0) return { ok: false, status: 400, error: 'bad-request' };
  var envState = environments.read();
  var environment = environments.environmentById(envState, request.environmentId);
  if (!environment) return { ok: false, status: 404, error: 'environment-not-found' };
  if (request.operation === 'set' && environment.authRef !== environment.id) return { ok: false, status: 409, error: 'auth-ref-required' };
  if (request.operation !== 'set' && request.operation !== 'delete') return { ok: false, status: 400, error: 'credential-operation-invalid' };

  var secretCheck = null;
  if (request.operation === 'set') {
    secretCheck = validateSecret(request.secret);
    if (!secretCheck.ok) return { ok: false, status: 400, error: secretCheck.error };
  }
  var fingerprintInput = Object.assign({}, request, request.secret === undefined ? {} : { secret: sha(Buffer.from(request.secret)) });
  var fingerprint = sha(Buffer.from(JSON.stringify(fingerprintInput)));
  var state;
  try { state = safeReadState(); }
  catch (stateError) { return { ok: false, status: 409, error: 'credential-state-invalid' }; }
  var prior = state.idempotency.find(function (row) { return row.key === request.idempotencyKey; });
  if (prior) return prior.fingerprint === fingerprint
    ? { ok: true, status: 200, revision: prior.revision, credential: publicStatusUnlocked(environment), replayed: true }
    : { ok: false, status: 409, error: 'idempotency-conflict' };

  var before;
  try { before = inspectFile(secretPath(environment.id), true); }
  catch (e) { before = { exists: true, proof: 'invalid' }; }
  if (reconcileRevision(state, environment.id, before)) writeState(state);
  var revision = state.revisions[environment.id] || 0;
  if (revision !== request.expectedAuthRevision) return { ok: false, status: 409, error: 'auth-revision-conflict', currentRevision: revision };

  try {
    ensureSecretsDir();
    var file = secretPath(environment.id);
    if (request.operation === 'set') atomicWrite(file, secretCheck.bytes, 0o600);
    else if (before.exists) {
      var deletionProof = inspectFile(file, false).proof;
      if (deletionProof !== before.proof) return { ok: false, status: 409, error: 'credential-write-conflict' };
      fs.unlinkSync(file);
    }
  } catch (e2) { return { ok: false, status: 409, error: 'credential-write-failed' }; }

  var after = inspectFile(secretPath(environment.id), true);
  state.revisions[environment.id] = revision + 1;
  state.fileProofs[environment.id] = after.exists ? after.proof : null;
  state.idempotency.push({ key: request.idempotencyKey, fingerprint: fingerprint, environmentId: environment.id,
    revision: state.revisions[environment.id], createdAt: new Date().toISOString() });
  state.idempotency = state.idempotency.slice(-100);
  writeState(state);
  return { ok: true, status: 200, revision: state.revisions[environment.id], credential: publicStatusUnlocked(environment), replayed: false };
}

function serialize(fn) {
  var next = stateMutex.then(fn, fn);
  stateMutex = next.catch(function () {});
  return next;
}

function guardedMutation(request) {
  try { return withStateMutex(function () { return mutateSync(request); }); }
  catch (stateError) { return { ok: false, status: 409, error: 'credential-state-invalid' }; }
}
function mutate(request) { return serialize(function () { return guardedMutation(request); }); }

function clearAllSync() {
  ensureSecretsDir();
  for (var i = 0; i < environments.IDS.length; i++) {
    var file = secretPath(environments.IDS[i]);
    var inspection = inspectFile(file, true);
    if (!inspection.exists) continue;
    var bytes = guardedBytes(file, MAX_SECRET_BYTES, 0o600);
    if (!fileGuards.unlinkRegularFileMatchingUnder(paths.PROJECT_ROOT, SECRETS_DIR, file, MAX_SECRET_BYTES, bytes)) {
      throw new Error('credential-state-invalid');
    }
  }
  writeState(initialState());
  return true;
}
function clearAll() {
  return serialize(function () {
    try { return withStateMutex(clearAllSync); }
    catch (error) { throw new Error('credential-state-invalid'); }
  });
}

module.exports = {
  STATE_FILE: STATE_FILE,
  publicStatus: publicStatus,
  currentRevision: currentRevision,
  readForJob: readForJob,
  mutate: mutate,
  clearAll: clearAll
};
