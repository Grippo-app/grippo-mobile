'use strict';

// Generation-bound read model and durable mutation journal for observed-token
// sources. Detach/retire publish the complete token domain through the same
// pointer-last writer as capture ingestion. Reactivation only schedules a
// successful exact recapture; it never flips lifecycle from retained bytes.

var crypto = require('crypto');
var path = require('path');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var generation = require('./figma-generation');
var tokenJobs = require('./figma-token-jobs');
var tokenHealth = require('./token-source-health-store');
var configUpdate = require('./project-config-update');
var testJobs = require('./figma-test-job');
var projectIdentity = require(path.join(paths.ORCHESTRATOR_DIR, 'figma', 'runtime', 'project-identity.cjs'));

var ROOT = path.join(paths.FIGMA_CACHE_DIR, 'token-source-mutations');
var STAGES_ROOT = path.join(paths.FIGMA_CACHE_DIR, 'generations');
var RECORD_MAX = 64 * 1024;
var RECORDS_MAX = 4096;
var DEFAULT_LIMIT = 100;
var MAX_LIMIT = 500;
var MUTATION_RE = /^tsm_[A-Za-z0-9_-]{16,96}$/;
var SOURCE_RE = /^otsrc:sha256:[a-f0-9]{64}$/;
var HASH_RE = /^sha256:[a-f0-9]{64}$/;
var FILE_RE = /^(tsm_[A-Za-z0-9_-]{16,96})\.json$/;
var callbacks = null;
var ready = false;
var running = Object.create(null);
var recoveryTimer = null;
var resetPaused = false;
var MUTATION_ERROR_CODES = Object.freeze({
  'TOKEN_SOURCE_MUTATION_DIRECTORY_UNSAFE': 1,
  'TOKEN_SOURCE_MUTATION_RECORD_INVALID': 1,
  'TOKEN_SOURCE_MUTATION_RECORD_WRITE_FAILED': 1,
  'TOKEN_SOURCE_MUTATION_RECORD_CONFLICT': 1,
  'TOKEN_SOURCE_MUTATION_ENTRY_INVALID': 1,
  'TOKEN_GENERATION_RESYNC_REQUIRED': 1,
  'TOKEN_SOURCE_MUTATION_NO_EFFECT': 1,
  'TOKEN_SOURCE_HEALTH_RECOVERY_REQUIRED': 1,
  'TOKEN_SOURCE_CAS_CONFLICT': 1,
  'TOKEN_SOURCE_NOT_FOUND': 1,
  'TOKEN_SOURCE_REACTIVATION_START_FAILED': 1,
  'TOKEN_SOURCE_REACTIVATION_FAILED': 1
});
var HEALTH_RECOVERY_RETRY_CODES = Object.freeze({
  'TOKEN_SOURCE_HEALTH_COMPLETION_INVALID': 1,
  'TOKEN_SOURCE_HEALTH_DIRECTORY_UNSAFE': 1,
  'TOKEN_SOURCE_HEALTH_EVIDENCE_INVALID': 1,
  'TOKEN_SOURCE_HEALTH_POINTER_INVALID': 1,
  'TOKEN_SOURCE_HEALTH_REBASE_REQUIRED': 1,
  'TOKEN_SOURCE_HEALTH_RECOVERY_AMBIGUOUS': 1,
  'TOKEN_SOURCE_HEALTH_RECOVERY_CONFLICT': 1,
  'TOKEN_SOURCE_HEALTH_RECOVERY_FAILED': 1,
  'TOKEN_SOURCE_HEALTH_RECOVERY_INVALID': 1,
  'TOKEN_SOURCE_HEALTH_RECOVERY_SCAN_INCOMPLETE': 1,
  'TOKEN_SOURCE_HEALTH_RECOVERY_UNPROVEN': 1,
  'TOKEN_SOURCE_HEALTH_RESERVATION_MISSING': 1,
  'TOKEN_SOURCE_HEALTH_SIZE_LIMIT_EXCEEDED': 1,
  'TOKEN_SOURCE_HEALTH_SNAPSHOT_INVALID': 1,
  'TOKEN_SOURCE_HEALTH_SNAPSHOT_WRITE_FAILED': 1,
  'TOKEN_SOURCE_HEALTH_WRITE_CONFLICT': 1
});

function now() { return new Date().toISOString(); }
function exact(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}
function hash(value) {
  return 'sha256:' + crypto.createHash('sha256').update(
    Buffer.isBuffer(value) ? value : Buffer.from(JSON.stringify(value), 'utf8')
  ).digest('hex');
}
function canonicalValue(value) {
  if (Array.isArray(value)) return value.map(canonicalValue);
  if (!value || typeof value !== 'object') return value;
  var out = {};
  Object.keys(value).sort().forEach(function (key) { out[key] = canonicalValue(value[key]); });
  return out;
}
function jsonBytes(value) { return Buffer.from(JSON.stringify(value, null, 2) + '\n'); }
function fail(status, error, detail, extra) {
  return Object.assign({
    ok: false,
    status: status,
    error: error,
    detail: String(detail || '').replace(/[^\x20-\x7e]/g, ' ').slice(0, 300)
  }, extra || {});
}
function ensureRoot() {
  if (!fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, ROOT, { create: true, mode: 0o700 })) {
    throw new Error('TOKEN_SOURCE_MUTATION_DIRECTORY_UNSAFE');
  }
}
function recordFile(mutationId) { return path.join(ROOT, mutationId + '.json'); }
function validRecord(value) {
  var keys = [
    'schemaVersion', 'mutationId', 'requestHash', 'action', 'sourceId', 'state',
    'expectedGenerationRevision', 'expectedSourceIndexHash', 'expectedSourceIndexRevision',
    'expectedCaptureSequence', 'jobId', 'reason', 'origin', 'confirmedOriginCount',
    'confirmedAffectedTokenCount', 'detachOrigins', 'preparedAt', 'finishedAt',
    'resultGenerationId', 'errorCode'
  ];
  if (!exact(value, keys) || value.schemaVersion !== 1 ||
      !MUTATION_RE.test(String(value.mutationId || '')) ||
      !HASH_RE.test(String(value.requestHash || '')) ||
      ['detach-origin', 'retire-source', 'reactivate-source'].indexOf(value.action) < 0 ||
      !SOURCE_RE.test(String(value.sourceId || '')) ||
      ['prepared', 'running', 'published', 'published-health-recovery-required', 'failed']
        .indexOf(value.state) < 0 ||
      !HASH_RE.test(String(value.expectedGenerationRevision || '')) ||
      !HASH_RE.test(String(value.expectedSourceIndexHash || '')) ||
      !Number.isSafeInteger(value.expectedSourceIndexRevision) || value.expectedSourceIndexRevision < 0 ||
      !Number.isSafeInteger(value.expectedCaptureSequence) || value.expectedCaptureSequence < 1 ||
      !/^fsj-[a-f0-9]{32}$/.test(String(value.jobId || '')) ||
      typeof value.reason !== 'string' || !value.reason.trim() || value.reason.length > 500 ||
      !Number.isSafeInteger(value.confirmedAffectedTokenCount) || value.confirmedAffectedTokenCount < 0 ||
      value.origin !== null && !validOrigin(value.origin) ||
      value.confirmedOriginCount !== null &&
        (!Number.isSafeInteger(value.confirmedOriginCount) || value.confirmedOriginCount < 1) ||
      value.detachOrigins !== null && value.detachOrigins !== true) return false;
  if (value.action === 'detach-origin' &&
      (!value.origin || value.confirmedOriginCount === null || value.detachOrigins !== null) ||
      value.action === 'retire-source' &&
      (value.origin !== null || value.confirmedOriginCount === null || value.detachOrigins !== true) ||
      value.action === 'reactivate-source' &&
      (value.origin !== null || value.confirmedOriginCount !== null || value.detachOrigins !== null)) return false;
  try {
    if (new Date(value.preparedAt).toISOString() !== value.preparedAt) return false;
    if (value.finishedAt !== null && new Date(value.finishedAt).toISOString() !== value.finishedAt) return false;
  } catch (error) { return false; }
  if (value.resultGenerationId !== null &&
      !/^gen-[a-f0-9]{32}$/.test(String(value.resultGenerationId || ''))) return false;
  if (value.errorCode !== null &&
      !/^[A-Z0-9_-]{1,100}$/.test(String(value.errorCode || ''))) return false;
  if (value.state === 'published' && (!value.finishedAt || !value.resultGenerationId || value.errorCode !== null) ||
      value.state === 'published-health-recovery-required' &&
        (!value.finishedAt || !value.resultGenerationId ||
          value.errorCode !== 'TOKEN_SOURCE_HEALTH_RECOVERY_REQUIRED') ||
      value.state === 'failed' && (!value.finishedAt || value.resultGenerationId !== null || !value.errorCode) ||
      (value.state === 'prepared' || value.state === 'running') &&
        (value.finishedAt !== null || value.resultGenerationId !== null || value.errorCode !== null)) return false;
  return true;
}
function readRecord(mutationId, rootVerified) {
  if (!rootVerified) ensureRoot();
  var file = recordFile(mutationId);
  var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, ROOT, file);
  if (inspected && inspected.status === 'missing') return null;
  var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, ROOT, file, RECORD_MAX);
  if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') {
    throw new Error('TOKEN_SOURCE_MUTATION_RECORD_INVALID');
  }
  var value;
  try { value = JSON.parse(hit.bytes.toString('utf8')); }
  catch (error) { throw new Error('TOKEN_SOURCE_MUTATION_RECORD_INVALID'); }
  if (!validRecord(value)) throw new Error('TOKEN_SOURCE_MUTATION_RECORD_INVALID');
  return { value: value, bytes: hit.bytes, proof: hit.stat };
}
function createRecord(value) {
  if (!validRecord(value)) throw new Error('TOKEN_SOURCE_MUTATION_RECORD_INVALID');
  ensureRoot();
  var result = fileGuards.publishNoClobberRegularFileUnder(
    paths.PROJECT_ROOT, ROOT, recordFile(value.mutationId), jsonBytes(value),
    { create: true, directoryMode: 0o700, mode: 0o600, maxBytes: RECORD_MAX }
  );
  if (result.ok) return readRecord(value.mutationId);
  if (result.code === 'already-exists') return readRecord(value.mutationId);
  throw new Error('TOKEN_SOURCE_MUTATION_RECORD_WRITE_FAILED');
}
function replaceRecord(held, patch) {
  var next = Object.assign({}, held.value, patch);
  if (!validRecord(next)) throw new Error('TOKEN_SOURCE_MUTATION_RECORD_INVALID');
  var result = fileGuards.compareAndSwapRegularFileUnder(
    paths.PROJECT_ROOT, ROOT, recordFile(next.mutationId), RECORD_MAX,
    { proof: held.proof, bytes: held.bytes }, jsonBytes(next), { mode: 0o600 }
  );
  if (!result.ok) throw new Error('TOKEN_SOURCE_MUTATION_RECORD_CONFLICT');
  return readRecord(next.mutationId);
}
function records(readOnly) {
  if (readOnly) {
    var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, path.dirname(ROOT), ROOT);
    if (inspected && inspected.status === 'missing') return [];
    if (!inspected || inspected.status !== 'present' || !inspected.stat ||
        !inspected.stat.isDirectory() || inspected.stat.isSymbolicLink()) {
      throw new Error('TOKEN_SOURCE_MUTATION_DIRECTORY_UNSAFE');
    }
  } else ensureRoot();
  var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, ROOT, RECORDS_MAX);
  if (!listed.ok) throw new Error('TOKEN_SOURCE_MUTATION_DIRECTORY_UNSAFE');
  return listed.names.map(function (name) {
    var match = FILE_RE.exec(name);
    if (!match) throw new Error('TOKEN_SOURCE_MUTATION_ENTRY_INVALID');
    return readRecord(match[1], true).value;
  }).sort(function (left, right) {
    return Date.parse(right.preparedAt) - Date.parse(left.preparedAt) ||
      left.mutationId.localeCompare(right.mutationId);
  });
}
function jsonRole(active, role) {
  if (!active || !active.ok || active.mode !== 'generation') return null;
  var entry = active.manifest.artifacts.find(function (candidate) { return candidate.role === role; });
  var bytes = entry && generation.readEntry(entry);
  return bytes ? JSON.parse(bytes.toString('utf8')) : null;
}
function sourceBatch(active, source) {
  var entry = active.manifest.artifacts.find(function (candidate) {
    return candidate.role === source.acceptedBatch.shardRole;
  });
  var bytes = entry && generation.readEntry(entry), shard;
  shard = bytes ? JSON.parse(bytes.toString('utf8')) : null;
  return shard && Array.isArray(shard.sources)
    ? shard.sources.find(function (batch) { return batch.sourceId === source.sourceId; }) || null
    : null;
}
function currentContext() {
  var active = generation.current();
  if (!active.ok || active.mode !== 'generation') {
    return fail(409, 'design-generation-invalid', active.error || 'no committed token generation');
  }
  var index, catalog;
  try {
    index = jsonRole(active, 'observed-token-source-index');
    catalog = jsonRole(active, 'observed-token-catalog');
  } catch (error) {
    return fail(409, 'TOKEN_GENERATION_RESYNC_REQUIRED',
      'observed token source index/catalog JSON is invalid');
  }
  if (!index || !catalog || catalog.sourceIndexHash !== index.semanticHash ||
      catalog.sourceIndexRevision !== index.revision) {
    return fail(409, 'TOKEN_GENERATION_RESYNC_REQUIRED', 'observed token source index/catalog are incomplete');
  }
  return { ok: true, active: active, index: index, catalog: catalog };
}
function configuredScopeMatches(context) {
  var config = configUpdate.read();
  if (!config.ok || !config.figmaFileKey) return false;
  return testJobs.fileKeyFingerprint(config.figmaFileKey) === context.index.scope.fileKeyFingerprint &&
    projectIdentity.projectBranchKey(paths.PROJECT_ROOT) === context.index.scope.branchKey;
}
function healthBySource(sourceIndexHash) {
  var out = Object.create(null), summary = null;
  try {
    var read = tokenHealth.peek();
    if (!read) return { available: false, rows: out, latestJob: null };
    var held = read.snapshot;
    if (held.index.sourceIndexRevision !== sourceIndexHash) {
      return { available: false, rows: out, latestJob: null };
    }
    held.shards.forEach(function (shard) {
      shard.records.forEach(function (record) { out[record.sourceId] = record; });
    });
    summary = held.index.jobSummaries.length
      ? held.index.jobSummaries[held.index.jobSummaries.length - 1]
      : null;
  } catch (error) {
    if (/^TOKEN_SOURCE_HEALTH_/.test(String(error && error.message || ''))) {
      return { available: false, rows: out, latestJob: null };
    }
    throw error;
  }
  return { available: true, rows: out, latestJob: summary };
}
function figmaDeepLink(nodeId) {
  var config = configUpdate.read();
  if (!config.ok || !config.figmaLibraryUrl) return null;
  try {
    var url = new URL(config.figmaLibraryUrl);
    if (url.protocol !== 'https:' || !/(^|\.)figma\.com$/i.test(url.hostname)) return null;
    url.searchParams.set('node-id', String(nodeId).replace(':', '-'));
    return url.toString();
  } catch (error) { return null; }
}
function affectedCounts(catalog, ordinal) {
  var affected = 0, exclusive = 0;
  catalog.tokens.forEach(function (token) {
    if (!Array.isArray(token.sourceRefs) || token.sourceRefs.indexOf(ordinal) < 0) return;
    affected++;
    if (token.sourceRefs.length === 1) exclusive++;
  });
  return { affected: affected, exclusive: exclusive };
}
function sameOrigin(left, right) {
  if (!left || !right) return false;
  var leftKeys = Object.keys(left).sort(), rightKeys = Object.keys(right).sort();
  return leftKeys.length === rightKeys.length &&
    leftKeys.every(function (key, index) {
      return key === rightKeys[index] && left[key] === right[key];
    });
}
function actionMatrix(source, scopeMismatch) {
  if (scopeMismatch) {
    return {
      detachOrigin: false, retire: true, reactivate: false,
      retry: false, reason: 'source-scope-mismatch'
    };
  }
  if (source.lifecycle === 'retired') {
    return {
      detachOrigin: false, retire: false, reactivate: true,
      retry: false, reason: 'successful-recapture-required'
    };
  }
  return {
    detachOrigin: true, retire: true, reactivate: false,
    retry: true, reason: null
  };
}
function encodeCursor(revision, sourceId) {
  return Buffer.from(JSON.stringify({ v: 1, r: revision, s: sourceId }), 'utf8').toString('base64url');
}
function decodeCursor(raw, revision) {
  if (!raw) return null;
  try {
    var value = JSON.parse(Buffer.from(raw, 'base64url').toString('utf8'));
    return exact(value, ['v', 'r', 's']) && value.v === 1 && value.r === revision &&
      SOURCE_RE.test(String(value.s || '')) ? value.s : false;
  } catch (error) { return false; }
}
function publicHistory(row) {
  return {
    mutationId: row.mutationId,
    action: row.action,
    state: row.state,
    preparedAt: row.preparedAt,
    finishedAt: row.finishedAt || null,
    resultGenerationId: row.resultGenerationId || null,
    jobId: row.jobId,
    errorCode: row.errorCode || null,
    reason: row.reason,
    origin: row.origin
  };
}
function list(params) {
  params = params || {};
  var allowed = ['query', 'status', 'cursor', 'limit', 'expectedGenerationRevision'];
  if (Object.keys(params).some(function (key) { return allowed.indexOf(key) < 0; })) {
    return fail(400, 'bad-token-source-query', 'unknown query field');
  }
  var context = currentContext();
  if (!context.ok) return context;
  if (params.expectedGenerationRevision &&
      params.expectedGenerationRevision !== context.active.pointer.manifestHash) {
    return fail(409, 'design-generation-conflict', 'the committed generation changed', {
      generationRevision: context.active.pointer.manifestHash
    });
  }
  var limit = params.limit === undefined ? DEFAULT_LIMIT : Number(params.limit);
  if (!Number.isInteger(limit) || limit < 1 || limit > MAX_LIMIT ||
      params.status && ['active', 'retired', 'scope-mismatch'].indexOf(params.status) < 0 ||
      params.query && (typeof params.query !== 'string' || params.query.length > 200)) {
    return fail(400, 'bad-token-source-query', 'invalid source filter or limit');
  }
  var cursor = decodeCursor(params.cursor, context.active.pointer.manifestHash);
  if (cursor === false) return fail(400, 'bad-token-source-cursor', 'cursor is malformed or stale');
  var scopeMismatch = !configuredScopeMatches(context);
  var health = healthBySource(context.index.semanticHash);
  var history;
  try { history = records(true); }
  catch (error) {
    return fail(409,
      mutationErrorCode(error && error.message, 'TOKEN_SOURCE_MUTATION_FAILED'),
      'source mutation audit is unavailable');
  }
  var historyBySource = Object.create(null);
  history.forEach(function (row) {
    (historyBySource[row.sourceId] = historyBySource[row.sourceId] || []).push(publicHistory(row));
  });
  var query = String(params.query || '').toLowerCase();
  var rows;
  try {
    rows = context.index.sources.map(function (source) {
      var batch = sourceBatch(context.active, source);
      if (!batch || batch.batchSemanticHash !== source.acceptedBatch.batchSemanticHash ||
          batch.observations.length !== source.acceptedBatch.observationCount) {
        throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED');
      }
      var impact = affectedCounts(context.catalog, source.ordinal);
      var healthRow = health.rows[source.sourceId] || null;
      var state = scopeMismatch ? 'scope-mismatch' : source.lifecycle;
      return {
      sourceId: source.sourceId,
      nodeId: source.nodeId,
      deepLink: figmaDeepLink(source.nodeId),
      kind: source.kind,
      context: source.context,
      state: state,
      activeOrigins: source.lifecycle === 'active' ? source.origins : [],
      retainedOrigins: source.lifecycle === 'retired' ? source.origins : [],
      acceptedBatch: {
        semanticHash: source.acceptedBatch.batchSemanticHash,
        captureSequence: source.acceptedBatch.captureSequence,
        observationCount: source.acceptedBatch.observationCount,
        becameEmpty: source.acceptedBatch.observationCount === 0 &&
          Number.isSafeInteger(source.acceptedBatch.previousObservationCount),
        previousObservationCount: source.acceptedBatch.previousObservationCount === undefined
          ? null : source.acceptedBatch.previousObservationCount
      },
      health: healthRow ? {
        latestAttempt: healthRow.latestAttempt || null,
        latestSuccess: healthRow.latestSuccess || null,
        latestFailure: healthRow.latestFailure || null
      } : null,
      affectedTokenCount: impact.affected,
      exclusivelyAffectedTokenCount: impact.exclusive,
      actions: actionMatrix(source, scopeMismatch),
        history: (historyBySource[source.sourceId] || []).slice(0, 100)
      };
    });
  } catch (error) {
    return fail(409, 'TOKEN_GENERATION_RESYNC_REQUIRED', 'source index and retained shards disagree');
  }
  rows = rows.filter(function (row) {
    return (!params.status || row.state === params.status) &&
      (!query || [row.sourceId, row.nodeId, row.kind, JSON.stringify(row.context)]
        .join(' ').toLowerCase().indexOf(query) >= 0) &&
      (!cursor || row.sourceId > cursor);
  }).sort(function (left, right) { return left.sourceId.localeCompare(right.sourceId); });
  var page = rows.slice(0, limit);
  return {
    ok: true,
    status: 200,
    schemaVersion: 1,
    committedGenerationId: context.active.manifest.generationId,
    generationRevision: context.active.pointer.manifestHash,
    sourceIndexRevision: context.index.revision,
    sourceIndexHash: context.index.semanticHash,
    currentAccepted: {
      sourceCount: context.index.sources.length,
      active: context.index.counts.active,
      retired: context.index.counts.retired
    },
    sourceHealthAvailable: health.available,
    latestAttempt: health.latestJob,
    scope: {
      fileKeyFingerprint: context.index.scope.fileKeyFingerprint,
      branchKey: context.index.scope.branchKey,
      state: scopeMismatch ? 'scope-mismatch' : 'current'
    },
    limitations: [
      'usage-scoped-observations-only',
      'no-file-wide-variable-census',
      'reactivation-requires-successful-exact-recapture'
    ],
    rows: page,
    nextCursor: rows.length > limit
      ? encodeCursor(context.active.pointer.manifestHash, page[page.length - 1].sourceId)
      : null
  };
}
function validOrigin(value) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return false;
  if (value.kind === 'task-screen') {
    return exact(value, ['kind', 'taskStem', 'screenKey', 'variantId']) &&
      /^TASK_[1-9][0-9]*_[A-Za-z0-9_]+$/.test(String(value.taskStem || '')) &&
      typeof value.screenKey === 'string' && value.screenKey.length > 0 && value.screenKey.length <= 128 &&
      typeof value.variantId === 'string' && value.variantId.length > 0 && value.variantId.length <= 128;
  }
  if (value.kind === 'task-component') {
    return exact(value, ['kind', 'taskStem', 'designComponentId']) &&
      /^TASK_[1-9][0-9]*_[A-Za-z0-9_]+$/.test(String(value.taskStem || '')) &&
      typeof value.designComponentId === 'string' &&
      value.designComponentId.length >= 12 && value.designComponentId.length <= 256;
  }
  if (value.kind === 'project-config') {
    return exact(value, ['kind', 'configField']) &&
      value.configField === 'figmaLibraryUrl';
  }
  return value.kind === 'component-inventory' &&
    exact(value, ['kind', 'componentScopeId', 'captureRootNodeId']) &&
    typeof value.componentScopeId === 'string' &&
    value.componentScopeId.length >= 12 && value.componentScopeId.length <= 256 &&
    /^[0-9]+:[0-9]+$/.test(String(value.captureRootNodeId || ''));
}
function validateRequest(request) {
  if (!request || typeof request !== 'object' || Array.isArray(request)) return 'request must be an object';
  var allowed = [
    'mutationId', 'action', 'sourceId', 'expectedGenerationRevision',
    'expectedSourceIndexHash', 'expectedSourceIndexRevision', 'origin',
    'confirmedOriginCount', 'confirmedAffectedTokenCount', 'detachOrigins', 'reason'
  ];
  if (Object.keys(request).some(function (key) { return allowed.indexOf(key) < 0; }) ||
      !MUTATION_RE.test(String(request.mutationId || '')) ||
      ['detach-origin', 'retire-source', 'reactivate-source'].indexOf(request.action) < 0 ||
      !SOURCE_RE.test(String(request.sourceId || '')) ||
      !HASH_RE.test(String(request.expectedGenerationRevision || '')) ||
      !HASH_RE.test(String(request.expectedSourceIndexHash || '')) ||
      !Number.isSafeInteger(request.expectedSourceIndexRevision) || request.expectedSourceIndexRevision < 0 ||
      typeof request.reason !== 'string' || !request.reason.trim() || request.reason.length > 500 ||
      !Number.isSafeInteger(request.confirmedAffectedTokenCount) || request.confirmedAffectedTokenCount < 0) {
    return 'request fields are invalid';
  }
  if (request.action === 'detach-origin') {
    if (!validOrigin(request.origin) ||
        !Number.isSafeInteger(request.confirmedOriginCount) || request.confirmedOriginCount < 1 ||
        request.detachOrigins !== undefined) return 'detach-origin confirmation is invalid';
  } else if (request.action === 'retire-source') {
    if (request.origin !== undefined ||
        !Number.isSafeInteger(request.confirmedOriginCount) || request.confirmedOriginCount < 1 ||
        request.detachOrigins !== true) return 'retire-source confirmation is invalid';
  } else if (request.origin !== undefined || request.confirmedOriginCount !== undefined ||
      request.detachOrigins !== undefined) return 'reactivate-source request is invalid';
  return null;
}
function idempotentResult(record) {
  if (record.state === 'published' || record.state === 'published-health-recovery-required') {
    return {
      ok: true,
      status: record.state === 'published' ? 200 : 202,
      idempotent: true,
      healthRecoveryRequired: record.state === 'published-health-recovery-required',
      mutation: publicHistory(record)
    };
  }
  if (record.state === 'failed') {
    return fail(409, record.errorCode || 'TOKEN_SOURCE_MUTATION_FAILED',
      'the idempotent mutation already finished unsuccessfully', { mutation: publicHistory(record) });
  }
  var job = callbacks && callbacks.job ? callbacks.job(record.jobId) : null;
  return {
    ok: true,
    status: 202,
    idempotent: true,
    mutation: publicHistory(record),
    job: job || null
  };
}
function directMutation(request, context, source, held) {
  var stageDir = path.join(STAGES_ROOT, held.value.jobId, 'source-management');
  var runOptions = {
    intent: {
      scope: context.index.scope,
      receiptManifestHash: held.value.requestHash,
      sources: []
    },
    sidecars: [],
    stageDir: stageDir
  };
  if (request.action === 'retire-source') runOptions.retireSourceIds = [source.sourceId];
  else runOptions.detachOrigin = request.origin;
  return tokenJobs.executeTaskTokenIngestion(runOptions).then(function (result) {
    if (result.noOp) throw new Error('TOKEN_SOURCE_MUTATION_NO_EFFECT');
    return callbacks.publishDomains({
      id: held.value.jobId,
      accountFingerprint: context.active.manifest.accountFingerprint,
      fileKeyFingerprint: context.index.scope.fileKeyFingerprint,
      inputFingerprint: held.value.requestHash,
      completed: [result.domainResult],
      verifyInputs: function () {
        var current = generation.current();
        var latest = readRecord(request.mutationId);
        return current.ok && current.mode === 'generation' &&
          current.pointer.manifestHash === request.expectedGenerationRevision &&
          latest && latest.value.state === 'prepared' &&
          latest.value.requestHash === held.value.requestHash;
      }
    });
  }).then(function (generationId) {
    var current = generation.current();
    var index = jsonRole(current, 'observed-token-source-index');
    var latest = readRecord(request.mutationId);
    latest = replaceRecord(latest, {
      state: 'published-health-recovery-required',
      resultGenerationId: generationId,
      errorCode: 'TOKEN_SOURCE_HEALTH_RECOVERY_REQUIRED',
      finishedAt: now()
    });
    try {
      tokenHealth.complete({
        sourceIndexHash: index.semanticHash,
        reservations: [],
        outcome: 'no-op',
        jobId: held.value.jobId,
        action: request.action,
        startedAt: held.value.preparedAt,
        summaryOutcome: 'published'
      });
      latest = replaceRecord(latest, {
        state: 'published',
        errorCode: null
      });
    } catch (error) {
      if (latest.value.state !== 'published-health-recovery-required') throw error;
    }
    Promise.resolve().then(function () {
      return callbacks.requestDriftComparison('token-source-' + request.action);
    }).catch(function () {});
    return {
      ok: true,
      status: latest.value.state === 'published' ? 200 : 202,
      idempotent: false,
      healthRecoveryRequired: latest.value.state === 'published-health-recovery-required',
      mutation: publicHistory(latest.value),
      generationRevision: current.pointer.manifestHash,
      sourceIndexRevision: index.revision,
      sourceIndexHash: index.semanticHash
    };
  }).catch(function (error) {
    var latest;
    try {
      latest = readRecord(request.mutationId);
      if (latest && latest.value.state === 'prepared') {
        latest = replaceRecord(latest, {
          state: 'failed',
          errorCode: mutationErrorCode(
            error && (error.tokenCode || error.message),
            'TOKEN_SOURCE_MUTATION_FAILED'
          ),
          finishedAt: now()
        });
      }
    } catch (recordError) { /* recovery will fail closed on the malformed journal */ }
    return fail(409, latest && latest.value.errorCode || 'TOKEN_SOURCE_MUTATION_FAILED',
      'source mutation did not publish', { mutation: latest && publicHistory(latest.value) });
  }).finally(function () {
    if (callbacks.cleanupStage) callbacks.cleanupStage(held.value.jobId);
  });
}
function mutateOnce(request) {
  if (resetPaused || !ready || !callbacks) return Promise.resolve(fail(409, 'token-source-management-recovering', 'source management is not ready'));
  var invalid = validateRequest(request);
  if (invalid) return Promise.resolve(fail(400, 'bad-token-source-mutation', invalid));
  var requestHash = hash(canonicalValue(request));
  var existing;
  try { existing = readRecord(request.mutationId); }
  catch (error) {
    return Promise.resolve(fail(409,
      mutationErrorCode(error && error.message, 'TOKEN_SOURCE_MUTATION_FAILED'),
      'source mutation audit is unavailable'));
  }
  if (existing) {
    if (existing.value.requestHash !== requestHash) {
      return Promise.resolve(fail(409, 'token-source-idempotency-conflict', 'mutationId is already bound to different bytes'));
    }
    return Promise.resolve(idempotentResult(existing.value));
  }
  var context = currentContext();
  if (!context.ok) return Promise.resolve(context);
  if (context.active.pointer.manifestHash !== request.expectedGenerationRevision ||
      context.index.semanticHash !== request.expectedSourceIndexHash ||
      context.index.revision !== request.expectedSourceIndexRevision) {
    return Promise.resolve(fail(409, 'token-source-cas-conflict', 'generation or source index changed', {
      generationRevision: context.active.pointer.manifestHash,
      sourceIndexHash: context.index.semanticHash,
      sourceIndexRevision: context.index.revision
    }));
  }
  var source = context.index.sources.find(function (row) { return row.sourceId === request.sourceId; });
  if (!source) return Promise.resolve(fail(404, 'token-source-not-found', 'source is not present in the retained registry'));
  if (!configuredScopeMatches(context) && request.action !== 'retire-source') {
    return Promise.resolve(fail(409, 'token-source-scope-mismatch',
      'detach and reactivation require the currently configured exact source scope'));
  }
  var impact = affectedCounts(context.catalog, source.ordinal);
  if (request.confirmedAffectedTokenCount !== impact.affected ||
      request.action !== 'reactivate-source' && request.confirmedOriginCount !== source.origins.length) {
    return Promise.resolve(fail(409, 'token-source-confirmation-stale', 'origin or affected-token count changed', {
      confirmedOriginCount: source.origins.length,
      confirmedAffectedTokenCount: impact.affected
    }));
  }
  if (request.action === 'detach-origin') {
    if (source.lifecycle !== 'active' ||
        !source.origins.some(function (origin) { return sameOrigin(origin, request.origin); })) {
      return Promise.resolve(fail(409, 'token-source-origin-not-active', 'the exact origin is not active on this source'));
    }
  } else if (request.action === 'retire-source') {
    if (source.lifecycle !== 'active') {
      return Promise.resolve(fail(409, 'token-source-already-retired', 'the source is already retired'));
    }
  } else if (source.lifecycle !== 'retired') {
    return Promise.resolve(fail(409, 'token-source-not-retired', 'only a retained retired source can be reactivated'));
  }
  var jobId = 'fsj-' + crypto.createHash('sha256').update(request.mutationId + '\0' + requestHash).digest('hex').slice(0, 32);
  var held;
  try {
    held = createRecord({
      schemaVersion: 1,
      mutationId: request.mutationId,
      requestHash: requestHash,
      action: request.action,
      sourceId: request.sourceId,
      state: 'prepared',
      expectedGenerationRevision: request.expectedGenerationRevision,
      expectedSourceIndexHash: request.expectedSourceIndexHash,
      expectedSourceIndexRevision: request.expectedSourceIndexRevision,
      expectedCaptureSequence: source.acceptedBatch.captureSequence,
      jobId: jobId,
      reason: request.reason.trim(),
      origin: request.origin || null,
      confirmedOriginCount: request.confirmedOriginCount === undefined
        ? null : request.confirmedOriginCount,
      confirmedAffectedTokenCount: request.confirmedAffectedTokenCount,
      detachOrigins: request.detachOrigins === true ? true : null,
      preparedAt: now(),
      finishedAt: null,
      resultGenerationId: null,
      errorCode: null
    });
  } catch (error) {
    return Promise.resolve(fail(409,
      mutationErrorCode(error && error.message, 'TOKEN_SOURCE_MUTATION_FAILED'),
      'could not durably prepare source mutation'));
  }
  if (held.value.requestHash !== requestHash) {
    return Promise.resolve(fail(409, 'token-source-idempotency-conflict', 'mutationId is already bound to different bytes'));
  }
  if (request.action !== 'reactivate-source') return directMutation(request, context, source, held);
  var started = callbacks.startReactivation({
    sourceId: source.sourceId,
    expectedGenerationRevision: request.expectedGenerationRevision,
    expectedSourceIndexHash: request.expectedSourceIndexHash,
    expectedSourceIndexRevision: request.expectedSourceIndexRevision,
    mutationId: request.mutationId,
    jobId: held.value.jobId
  });
  if (!started || !started.ok || !started.job || started.job.id !== held.value.jobId) {
    held = replaceRecord(held, {
      state: 'failed',
      errorCode: mutationErrorCode(
        started && started.error,
        'TOKEN_SOURCE_REACTIVATION_START_FAILED'
      ),
      finishedAt: now()
    });
    return Promise.resolve(fail(started && started.status || 409, held.value.errorCode,
      'successful recapture could not be started', { mutation: publicHistory(held.value) }));
  }
  held = replaceRecord(held, { state: 'running' });
  return Promise.resolve({
    ok: true,
    status: 202,
    idempotent: false,
    mutation: publicHistory(held.value),
    job: started.job
  });
}
function mutate(request) {
  var key = request && request.mutationId;
  if (!MUTATION_RE.test(String(key || ''))) return mutateOnce(request);
  if (running[key]) return running[key];
  running[key] = mutateOnce(request).finally(function () { delete running[key]; });
  return running[key];
}
function mutationErrorCode(value, fallback) {
  var code = typeof value === 'string' ? value : '';
  return Object.prototype.hasOwnProperty.call(MUTATION_ERROR_CODES, code)
    ? code : fallback || 'TOKEN_SOURCE_MUTATION_FAILED';
}
function publicErrorCode(error) {
  return mutationErrorCode(error && (error.code || error.message), 'TOKEN_SOURCE_MUTATION_FAILED');
}
function finishFailed(held, code) {
  return replaceRecord(held, {
    state: 'failed',
    errorCode: mutationErrorCode(code, 'TOKEN_SOURCE_MUTATION_FAILED'),
    finishedAt: now()
  });
}
function directPublicationRecovered(held, context) {
  if (!context.ok || context.active.manifest.syncJobId !== held.value.jobId) return false;
  var source = context.index.sources.find(function (row) { return row.sourceId === held.value.sourceId; });
  if (!source) return false;
  if (held.value.action === 'retire-source') return source.lifecycle === 'retired';
  return held.value.action === 'detach-origin' &&
    (source.lifecycle === 'retired' ||
      !source.origins.some(function (origin) { return sameOrigin(origin, held.value.origin); }));
}
function recoveredPublished(held, context) {
  return replaceRecord(held, {
    state: 'published-health-recovery-required',
    resultGenerationId: context.active.manifest.generationId,
    errorCode: 'TOKEN_SOURCE_HEALTH_RECOVERY_REQUIRED',
    finishedAt: context.active.pointer.committedAt
  });
}
function reconcilePublishedHealth(held, context) {
  if (!context.ok ||
      context.active.manifest.generationId !== held.value.resultGenerationId) return held;
  tokenHealth.complete({
    sourceIndexHash: context.index.semanticHash,
    reservations: [],
    outcome: 'no-op',
    jobId: held.value.jobId,
    action: held.value.action,
    startedAt: held.value.preparedAt,
    summaryOutcome: 'published'
  });
  return replaceRecord(held, {
    state: 'published',
    errorCode: null
  });
}
function replayDirect(held, context) {
  if (context.active.pointer.manifestHash !== held.value.expectedGenerationRevision ||
      context.index.semanticHash !== held.value.expectedSourceIndexHash ||
      context.index.revision !== held.value.expectedSourceIndexRevision) {
    return Promise.resolve(finishFailed(held, 'TOKEN_SOURCE_CAS_CONFLICT'));
  }
  var source = context.index.sources.find(function (row) { return row.sourceId === held.value.sourceId; });
  if (!source) return Promise.resolve(finishFailed(held, 'TOKEN_SOURCE_NOT_FOUND'));
  return directMutation({
    mutationId: held.value.mutationId,
    action: held.value.action,
    sourceId: held.value.sourceId,
    expectedGenerationRevision: held.value.expectedGenerationRevision,
    expectedSourceIndexHash: held.value.expectedSourceIndexHash,
    expectedSourceIndexRevision: held.value.expectedSourceIndexRevision,
    origin: held.value.origin,
    confirmedOriginCount: held.value.confirmedOriginCount,
    confirmedAffectedTokenCount: held.value.confirmedAffectedTokenCount,
    detachOrigins: held.value.detachOrigins,
    reason: held.value.reason
  }, context, source, held).then(function () {
    return readRecord(held.value.mutationId);
  });
}
function resumeReactivation(held) {
  var started;
  try {
    started = callbacks.startReactivation({
      sourceId: held.value.sourceId,
      expectedGenerationRevision: held.value.expectedGenerationRevision,
      expectedSourceIndexHash: held.value.expectedSourceIndexHash,
      expectedSourceIndexRevision: held.value.expectedSourceIndexRevision,
      mutationId: held.value.mutationId,
      jobId: held.value.jobId
    });
  } catch (error) {
    return Promise.resolve(finishFailed(held, error && error.message));
  }
  if (!started || !started.ok || !started.job || started.job.id !== held.value.jobId) {
    return Promise.resolve(finishFailed(held,
      started && started.error || 'TOKEN_SOURCE_REACTIVATION_START_FAILED'));
  }
  return Promise.resolve(replaceRecord(held, { state: 'running' }));
}
function reconcileRecord(held) {
  var context = currentContext();
  if (!context.ok) return Promise.resolve(held);
  if (held.value.state === 'published-health-recovery-required') {
    try { return Promise.resolve(reconcilePublishedHealth(held, context)); }
    catch (error) {
      if (Object.prototype.hasOwnProperty.call(
        HEALTH_RECOVERY_RETRY_CODES,
        error && error.message
      )) {
        return Promise.resolve(held);
      }
      return Promise.reject(error);
    }
  }
  if (held.value.state === 'prepared') {
    if (directPublicationRecovered(held, context)) {
      return reconcileRecord(recoveredPublished(held, context));
    }
    if (held.value.action !== 'reactivate-source') return replayDirect(held, context);
    return resumeReactivation(held).then(function (next) {
      return next.value.state === 'running' ? reconcileRecord(next) : next;
    });
  }
  if (held.value.state !== 'running') return Promise.resolve(held);
  var source = context.index.sources.find(function (row) { return row.sourceId === held.value.sourceId; });
  var job = callbacks && callbacks.job ? callbacks.job(held.value.jobId) : null;
  if (source && source.lifecycle === 'active' &&
      source.acceptedBatch.captureSequence > held.value.expectedCaptureSequence &&
      job && ['success', 'partial'].indexOf(job.result) >= 0 && job.committedGenerationId) {
    return Promise.resolve(replaceRecord(held, {
      state: 'published',
      resultGenerationId: job.committedGenerationId,
      finishedAt: job.finishedAt || now()
    }));
  }
  if (job && ['failed', 'cancelled', 'interrupted'].indexOf(job.result) >= 0) {
    return Promise.resolve(replaceRecord(held, {
      state: 'failed',
      errorCode: mutationErrorCode(job.errorCode, 'TOKEN_SOURCE_REACTIVATION_FAILED'),
      finishedAt: job.finishedAt || now()
    }));
  }
  return Promise.resolve(held);
}
function reconcile() {
  if (resetPaused) return Promise.resolve(false);
  if (!callbacks) return Promise.resolve(false);
  var rows;
  try { rows = records(); }
  catch (error) { return Promise.reject(error); }
  return rows.filter(function (row) {
    return row.state === 'prepared' || row.state === 'running' ||
      row.state === 'published-health-recovery-required';
  }).reduce(function (chain, row) {
    return chain.then(function () { return reconcileRecord(readRecord(row.mutationId)); });
  }, Promise.resolve()).then(function () { return true; });
}
function resetReady() { return Object.keys(running).length === 0; }
function beginReset() {
  if (!resetReady()) return false;
  resetPaused = true;
  return true;
}
function endReset() { resetPaused = false; }
function init(options) {
  if (!options || typeof options.publishDomains !== 'function' ||
      typeof options.requestDriftComparison !== 'function' ||
      typeof options.startReactivation !== 'function' ||
      typeof options.job !== 'function' ||
      typeof options.cleanupStage !== 'function') {
    return Promise.reject(new Error('TOKEN_SOURCE_MANAGEMENT_CALLBACKS_INVALID'));
  }
  callbacks = options;
  ready = false;
  return reconcile().then(function () {
    ready = true;
    if (!recoveryTimer) {
      recoveryTimer = setInterval(function () {
        reconcile().catch(function (error) {
          console.error('[token-sources] recovery failed:', error && error.message || error);
        });
      }, 30000);
      if (typeof recoveryTimer.unref === 'function') recoveryTimer.unref();
    }
    return true;
  });
}

module.exports = {
  ROOT: ROOT,
  init: init,
  list: list,
  mutate: mutate,
  publicErrorCode: publicErrorCode,
  reconcile: reconcile,
  resetReady: resetReady,
  beginReset: beginReset,
  endReset: endReset,
  validateRequest: validateRequest,
  _resetForTests: function () {
    ready = false;
    callbacks = null;
    running = Object.create(null);
    resetPaused = false;
    if (recoveryTimer) clearInterval(recoveryTimer);
    recoveryTimer = null;
  }
};
