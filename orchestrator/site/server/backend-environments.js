'use strict';

// Canonical, non-secret Backend source definitions. The manifest is exact-key
// v1 JSON and is the only backend source authority.

var fs = require('fs');
var path = require('path');
var crypto = require('crypto');
var paths = require('./paths');
var writerLeases = require('../../tasks/writer-leases.cjs');
var fileGuards = require('./file-guards');

var IDS = Object.freeze(['local', 'dev', 'stage', 'prod']);
var ID_SET = Object.freeze({ local: 1, dev: 1, stage: 1, prod: 1 });
var MANIFEST_FILE = path.join(paths.API_CONTRACT_DIR, 'environments.json');
var MAX_MANIFEST_BYTES = 64 * 1024;
var IDEMPOTENCY_MAX = 100;
var idempotency = new Map();

function sha(value) {
  return 'sha256:' + crypto.createHash('sha256').update(value).digest('hex');
}

function exactKeys(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}

function isLoopbackHost(host) {
  var h = String(host || '').toLowerCase().replace(/^\[|\]$/g, '');
  if (h === 'localhost' || h === '::1' || h === '0:0:0:0:0:0:0:1') return true;
  var m = /^(\d+)\.(\d+)\.(\d+)\.(\d+)$/.exec(h);
  return !!m && Number(m[1]) === 127 && m.slice(1).every(function (x) { return Number(x) <= 255; });
}

function canonicalUrl(value, environmentId, field) {
  if (typeof value !== 'string' || !value || /[^\x20-\x7e]/.test(value)) {
    return { error: field + '-invalid' };
  }
  var parsed;
  try { parsed = new URL(value); } catch (e) { return { error: field + '-invalid' }; }
  if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') return { error: field + '-scheme' };
  if (parsed.username || parsed.password) return { error: field + '-userinfo-forbidden' };
  if (parsed.search || parsed.hash) return { error: field + '-query-fragment-forbidden' };
  var localHost = isLoopbackHost(parsed.hostname);
  if (environmentId === 'local' && !localHost) return { error: field + '-loopback-required' };
  if (localHost && environmentId !== 'local') return { error: field + '-loopback-nonlocal' };
  if (parsed.protocol === 'http:' && (environmentId !== 'local' || !localHost)) {
    return { error: field + '-https-required' };
  }
  var canonical = parsed.toString();
  if (Buffer.byteLength(canonical, 'ascii') > 2048) return { error: field + '-too-long' };
  return { value: canonical };
}

function validateEnvironment(input) {
  var keys = ['authRef', 'id', 'label', 'postmanEnrichmentUrl', 'sourceKind', 'sourceUrl'];
  var canonicalKeys = keys.concat(['authKind']);
  if (!exactKeys(input, keys) && !exactKeys(input, canonicalKeys)) return { ok: false, error: 'environment-fields-invalid' };
  if (!ID_SET[input.id]) return { ok: false, error: 'environment-id-invalid' };
  if (typeof input.label !== 'string' || !input.label || /[\0\r\n]/.test(input.label) ||
      Buffer.byteLength(input.label, 'utf8') > 64) return { ok: false, error: 'environment-label-invalid' };
  if (input.sourceKind !== 'openapi' && input.sourceKind !== 'postman') return { ok: false, error: 'source-kind-invalid' };
  var authKind = input.authKind === undefined ? 'bearer' : input.authKind;
  if (authKind !== 'bearer' && authKind !== 'x-api-key') return { ok: false, error: 'auth-kind-invalid' };
  if (input.authRef !== null && input.authRef !== input.id) return { ok: false, error: 'auth-ref-invalid' };
  var source = canonicalUrl(input.sourceUrl, input.id, 'source-url');
  if (source.error) return { ok: false, error: source.error };
  if (input.authRef !== null && source.value.indexOf('http:') === 0) {
    return { ok: false, error: 'local-http-auth-forbidden' };
  }
  var enrichment = null;
  if (input.postmanEnrichmentUrl !== null) {
    if (input.sourceKind !== 'openapi') return { ok: false, error: 'postman-enrichment-primary-invalid' };
    enrichment = canonicalUrl(input.postmanEnrichmentUrl, input.id, 'postman-enrichment-url');
    if (enrichment.error) return { ok: false, error: enrichment.error };
  }
  return { ok: true, value: {
    id: input.id,
    label: input.label,
    sourceKind: input.sourceKind,
    authKind: authKind,
    sourceUrl: source.value,
    postmanEnrichmentUrl: enrichment ? enrichment.value : null,
    authRef: input.authRef
  } };
}

function validateManifest(input) {
  if (!exactKeys(input, ['defaultEnvironmentId', 'environments', 'schemaVersion']) || input.schemaVersion !== 1) {
    return { ok: false, error: 'environment-manifest-fields-invalid' };
  }
  if (!Array.isArray(input.environments) || input.environments.length < 1 || input.environments.length > 4) {
    return { ok: false, error: 'environment-count-invalid' };
  }
  var seen = Object.create(null), environments = [];
  for (var i = 0; i < input.environments.length; i++) {
    var checked = validateEnvironment(input.environments[i]);
    if (!checked.ok) return checked;
    if (seen[checked.value.id]) return { ok: false, error: 'environment-id-duplicate' };
    seen[checked.value.id] = 1;
    environments.push(checked.value);
  }
  if (!seen[input.defaultEnvironmentId]) return { ok: false, error: 'default-environment-invalid' };
  return { ok: true, value: {
    schemaVersion: 1,
    environments: environments,
    defaultEnvironmentId: input.defaultEnvironmentId
  } };
}

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
function safeRead(file, maxBytes) {
  var fd, before;
  try {
    if (!parentAnchored(file)) throw new Error('unsafe-parent');
    before = fs.lstatSync(file, { bigint: true });
    if (!before.isFile() || before.isSymbolicLink() || before.nlink !== 1n || before.size > BigInt(maxBytes)) throw new Error('unsafe-file');
    fd = fs.openSync(file, fs.constants.O_RDONLY | (fs.constants.O_NOFOLLOW || 0));
    var opened = fs.fstatSync(fd, { bigint: true });
    if (!sameFileGeneration(before, opened)) throw new Error('raced-file');
    var bytes = Buffer.alloc(Number(opened.size)), offset = 0;
    while (offset < bytes.length) { var count = fs.readSync(fd, bytes, offset, bytes.length - offset, offset); if (!count) throw new Error('short-read'); offset += count; }
    var afterFd = fs.fstatSync(fd, { bigint: true }), afterPath = fs.lstatSync(file, { bigint: true });
    if (!sameFileGeneration(opened, afterFd) || !sameFileGeneration(opened, afterPath) || !parentAnchored(file)) throw new Error('raced-file');
    return bytes;
  } catch (error) {
    if (error && error.code === 'ENOENT') throw error;
    var unsafe = new Error('unsafe regular-file contract'); unsafe.code = 'unsafe-file'; throw unsafe;
  } finally { if (fd !== undefined) try { fs.closeSync(fd); } catch (ignore) {} }
}

function read() {
  var raw;
  try { raw = safeRead(MANIFEST_FILE, MAX_MANIFEST_BYTES); }
  catch (e) {
    if (e && e.code === 'ENOENT') return { mode: 'missing', revision: 'absent', manifest: null };
    return { mode: 'invalid', revision: null, manifest: null, error: e.code || 'manifest-read-failed' };
  }
  var parsed;
  try { parsed = JSON.parse(raw.toString('utf8')); }
  catch (e2) { return { mode: 'invalid', revision: sha(raw), manifest: null, error: 'manifest-json-invalid' }; }
  var checked = validateManifest(parsed);
  if (!checked.ok) return { mode: 'invalid', revision: sha(raw), manifest: null, error: checked.error };
  return { mode: 'manifest', revision: sha(Buffer.from(JSON.stringify(checked.value))), manifest: checked.value };
}

function atomicWrite(file, value) {
  var bytes = Buffer.from(JSON.stringify(value, null, 2) + '\n');
  var published = fileGuards.atomicReplaceRegularFileResult(paths.PROJECT_ROOT, path.dirname(file), file, bytes,
    { create: true, directoryMode: 0o755, mode: 0o644, maxBytes: MAX_MANIFEST_BYTES });
  if (!published.ok) throw new Error(published.code || 'environment-write-failed');
}

function rememberIdempotency(key, fingerprint, result) {
  if (idempotency.size >= IDEMPOTENCY_MAX) idempotency.delete(idempotency.keys().next().value);
  idempotency.set(key, { fingerprint: fingerprint, result: result });
}

function mutate(request) {
  if (!request || typeof request !== 'object' || Array.isArray(request)) return { ok: false, status: 400, error: 'bad-request' };
  var exactRequestFields = request.operation === 'delete'
      ? ['defaultEnvironmentId', 'environmentId', 'expectedRevision', 'idempotencyKey', 'operation']
      : (request.operation === 'create' || request.operation === 'upsert')
        ? ['defaultEnvironmentId', 'environment', 'expectedRevision', 'idempotencyKey', 'operation'] : [];
  if (!exactRequestFields.length || !exactKeys(request, exactRequestFields)) return { ok: false, status: 400, error: 'bad-request-fields' };
  if (!/^[A-Za-z0-9._:-]{8,128}$/.test(String(request.idempotencyKey || ''))) return { ok: false, status: 400, error: 'bad-idempotency-key' };
  var fingerprint = sha(Buffer.from(JSON.stringify(request)));
  var prior = idempotency.get(request.idempotencyKey);
  if (prior) return prior.fingerprint === fingerprint ? prior.result : { ok: false, status: 409, error: 'idempotency-conflict' };
  var current = read();
  if (request.expectedRevision !== current.revision) return { ok: false, status: 409, error: 'environment-revision-conflict', currentRevision: current.revision };
  var next;
  if (request.operation === 'create') {
    if (current.mode !== 'missing') return { ok: false, status: 409, error: current.mode === 'invalid' ? current.error : 'environment-manifest-already-exists' };
    var created = validateEnvironment(request.environment);
    if (!created.ok) return { ok: false, status: 400, error: created.error };
    next = { schemaVersion: 1, environments: [created.value], defaultEnvironmentId: request.defaultEnvironmentId };
    var createdManifest = validateManifest(next);
    if (!createdManifest.ok) return { ok: false, status: 400, error: createdManifest.error };
    next = createdManifest.value;
  } else {
    if (current.mode !== 'manifest') return { ok: false, status: 409, error: current.mode === 'invalid' ? current.error : 'environment-manifest-missing' };
    next = JSON.parse(JSON.stringify(current.manifest));
    if (request.operation === 'upsert') {
      var environment = validateEnvironment(request.environment);
      if (!environment.ok) return { ok: false, status: 400, error: environment.error };
      var index = next.environments.findIndex(function (row) { return row.id === environment.value.id; });
      if (index < 0) next.environments.push(environment.value); else next.environments[index] = environment.value;
      if (request.defaultEnvironmentId !== undefined) next.defaultEnvironmentId = request.defaultEnvironmentId;
    } else if (request.operation === 'delete') {
      if (!ID_SET[request.environmentId]) return { ok: false, status: 400, error: 'environment-id-invalid' };
      next.environments = next.environments.filter(function (row) { return row.id !== request.environmentId; });
      if (!next.environments.length) return { ok: false, status: 409, error: 'last-environment-delete-forbidden' };
      if (next.defaultEnvironmentId === request.environmentId) {
        if (!ID_SET[request.defaultEnvironmentId]) return { ok: false, status: 400, error: 'default-environment-required' };
        next.defaultEnvironmentId = request.defaultEnvironmentId;
      }
    } else return { ok: false, status: 400, error: 'environment-operation-invalid' };
    var manifest = validateManifest(next);
    if (!manifest.ok) return { ok: false, status: 400, error: manifest.error };
    next = manifest.value;
  }

  var handle;
  try {
    handle = writerLeases.acquire(paths.WRITER_LEASES_DIR, { kind: 'site-config', key: 'backend:environments',
      ownerPid: process.pid, ttlMs: 60 * 1000, rootDir: paths.WRITER_AUTHORITY_ROOT });
    var scan = writerLeases.scan(paths.WRITER_LEASES_DIR, paths.WRITER_AUTHORITY_ROOT);
    if (scan.issues.length || scan.active.some(function (row) { return row.leaseId !== handle.leaseId; })) {
      writerLeases.release(handle); handle = null;
      return { ok: false, status: 409, error: 'writer-lease-conflict' };
    }
    var recheck = read();
    if (recheck.revision !== request.expectedRevision) return { ok: false, status: 409, error: 'environment-revision-conflict', currentRevision: recheck.revision };
    atomicWrite(MANIFEST_FILE, next);
  } catch (e3) { return { ok: false, status: 500, error: 'environment-write-failed' }; }
  finally { if (handle) try { writerLeases.release(handle); } catch (ignore) {} }
  var after = read();
  var result = { ok: true, status: 200, revision: after.revision, manifest: after.manifest };
  rememberIdempotency(request.idempotencyKey, fingerprint, result);
  return result;
}

function environmentById(snapshot, id) {
  if (!snapshot || snapshot.mode !== 'manifest') return null;
  return snapshot.manifest.environments.find(function (row) { return row.id === id; }) || null;
}

function clearAll(expectedRevision, outerLease) {
  var current = read();
  if (current.revision !== expectedRevision) {
    return { ok: false, status: 409, error: 'environment-revision-conflict', currentRevision: current.revision };
  }
  if (current.mode === 'missing') return { ok: true, status: 200, revision: 'absent', manifest: null };
  var handle, ownsLease = false;
  try {
    if (outerLease) handle = outerLease;
    else {
      handle = writerLeases.acquire(paths.WRITER_LEASES_DIR, { kind: 'site-config', key: 'backend:environments',
        ownerPid: process.pid, ttlMs: 60 * 1000, rootDir: paths.WRITER_AUTHORITY_ROOT });
      ownsLease = true;
    }
    var scan = writerLeases.scan(paths.WRITER_LEASES_DIR, paths.WRITER_AUTHORITY_ROOT);
    if (scan.issues.length || scan.stale.length || !scan.active.some(function (row) { return row.leaseId === handle.leaseId; }) ||
        scan.active.some(function (row) { return row.leaseId !== handle.leaseId; })) {
      if (ownsLease) { writerLeases.release(handle); handle = null; }
      return { ok: false, status: 409, error: 'writer-lease-conflict' };
    }
    var recheck = read();
    if (recheck.revision !== expectedRevision) {
      return { ok: false, status: 409, error: 'environment-revision-conflict', currentRevision: recheck.revision };
    }
    var bytes = safeRead(MANIFEST_FILE, MAX_MANIFEST_BYTES);
    if (!fileGuards.unlinkRegularFileMatchingUnder(
      paths.PROJECT_ROOT, path.dirname(MANIFEST_FILE), MANIFEST_FILE, MAX_MANIFEST_BYTES, bytes
    )) return { ok: false, status: 500, error: 'environment-write-failed' };
  } catch (error) { return { ok: false, status: 500, error: 'environment-write-failed' }; }
  finally { if (handle && ownsLease) try { writerLeases.release(handle); } catch (ignore) {} }
  var after = read();
  return after.mode === 'missing'
    ? { ok: true, status: 200, revision: after.revision, manifest: null }
    : { ok: false, status: 500, error: 'environment-write-failed' };
}

module.exports = {
  IDS: IDS,
  validateEnvironment: validateEnvironment,
  validateManifest: validateManifest,
  read: read,
  mutate: mutate,
  clearAll: clearAll,
  environmentById: environmentById
};
