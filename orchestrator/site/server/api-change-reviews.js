'use strict';

// User acknowledgements are stored separately from immutable semantic diffs.
// Rows are bound to a stable change id and change set; corrupt optional state
// degrades to a typed limitation and never changes contract readiness.

var path = require('path');
var crypto = require('crypto');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var relations = require('./api-relations');
var writerLeases = require(path.join(paths.ORCHESTRATOR_DIR, 'tasks', 'writer-leases.cjs'));

var FILE = path.join(paths.API_CONTRACT_CACHE_DIR, 'reports', 'change-reviews.json');
var MAX_BYTES = 16 * 1024 * 1024;
// A single committed change set can contain 10,000 rows. The review store must
// be able to acknowledge all of them without evicting reviews from that same set.
var MAX_ROWS = 10000;
var CHANGE_RE = /^chg-[a-f0-9]{24}$/;
var SET_RE = /^changes-[a-f0-9]{24}$/;
var KEY_RE = /^[A-Za-z0-9_.:-]{16,200}$/;

function exact(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}
function sha(value) {
  return 'sha256:' + crypto.createHash('sha256')
    .update(Buffer.isBuffer(value) ? value : Buffer.from(String(value), 'utf8')).digest('hex');
}
function initial() { return { schemaVersion: 1, reviews: [] }; }
function valid(value) {
  if (!exact(value, ['reviews', 'schemaVersion']) || value.schemaVersion !== 1 ||
      !Array.isArray(value.reviews) || value.reviews.length > MAX_ROWS) return false;
  var seen = Object.create(null);
  var idempotencyKeys = Object.create(null);
  return value.reviews.every(function (row) {
    var key = row && row.changeSetId + '\0' + row.changeId;
    if (!exact(row, [
      'changeId', 'changeSetId', 'idempotencyKey', 'reviewedAt'
    ]) || CHANGE_RE.test(String(row.changeId || '')) === false ||
        SET_RE.test(String(row.changeSetId || '')) === false ||
        KEY_RE.test(String(row.idempotencyKey || '')) === false ||
        !Number.isFinite(Date.parse(row.reviewedAt)) || seen[key] ||
        idempotencyKeys[row.idempotencyKey]) return false;
    seen[key] = 1;
    idempotencyKeys[row.idempotencyKey] = 1;
    return true;
  });
}
function read() {
  try {
    var hit = fileGuards.boundedRegularFileUnder(
      paths.PROJECT_ROOT, path.dirname(FILE), FILE, MAX_BYTES
    );
    if (!hit) {
      var inspected = fileGuards.inspectEntryUnder(
        paths.PROJECT_ROOT, path.dirname(FILE), FILE
      );
      if (inspected && inspected.status === 'missing') return initial();
      return null;
    }
    if (!hit.stat || String(hit.stat.nlink) !== '1') return null;
    var value = JSON.parse(hit.bytes.toString('utf8'));
    return valid(value) ? value : null;
  } catch (error) {
    return error && error.code === 'ENOENT' ? initial() : null;
  }
}
function revision(value) {
  return sha(JSON.stringify(value));
}
function write(value) {
  if (!valid(value)) throw new Error('api-change-review-state-invalid');
  var bytes = Buffer.from(JSON.stringify(value, null, 2) + '\n');
  var result = fileGuards.atomicReplaceRegularFileResult(
    paths.PROJECT_ROOT, path.dirname(FILE), FILE, bytes,
    { create: true, directoryMode: 0o700, mode: 0o600, maxBytes: MAX_BYTES }
  );
  if (!result || !result.ok) throw new Error('api-change-review-write-failed');
}
function state(changeSetId) {
  var value = read();
  if (!value) return {
    ok: false, revision: null, reviewed: Object.create(null),
    limitation: 'api-change-review-state-invalid'
  };
  var reviewed = Object.create(null);
  value.reviews.forEach(function (row) {
    if (row.changeSetId === changeSetId) reviewed[row.changeId] = row;
  });
  return {
    ok: true, revision: revision(value), reviewed: reviewed,
    limitation: null, value: value
  };
}
function currentChange(snapshot, changeSetId, changeId) {
  var report = snapshot && snapshot.changes && snapshot.changes.current
    ? snapshot.changes.value : null;
  return report && report.changeSetId === changeSetId &&
    report.changes.some(function (row) { return row.id === changeId; });
}
function mark(request) {
  if (!exact(request, [
    'changeId', 'changeSetId', 'expectedGenerationId',
    'expectedReviewRevision', 'idempotencyKey'
  ]) || !CHANGE_RE.test(String(request.changeId || '')) ||
      !SET_RE.test(String(request.changeSetId || '')) ||
      !relations.GENERATION_RE.test(String(request.expectedGenerationId || '')) ||
      !relations.HASH_RE.test(String(request.expectedReviewRevision || '')) ||
      !KEY_RE.test(String(request.idempotencyKey || ''))) {
    return { ok: false, status: 400, error: 'bad-api-change-review-request' };
  }
  var snapshot = relations.snapshot();
  if (!snapshot.ok) return snapshot;
  var conflict = relations.checkExpected(snapshot, request.expectedGenerationId);
  if (conflict) return conflict;
  if (!currentChange(snapshot, request.changeSetId, request.changeId)) {
    return Object.assign(relations.meta(snapshot), {
      ok: false, status: 409, error: 'api-change-review-stale'
    });
  }
  var before = state(request.changeSetId);
  if (!before.ok) return Object.assign(relations.meta(snapshot), {
    ok: false, status: 409, error: 'api-change-review-state-invalid'
  });
  var existing = before.reviewed[request.changeId];
  if (existing) {
    return Object.assign(relations.meta(snapshot), {
      ok: true, status: 200, reviewed: true,
      changeId: request.changeId, reviewRevision: before.revision
    });
  }
  var handle;
  try {
    writerLeases.reconcileStaleMutations(
      paths.WRITER_LEASES_DIR, paths.WRITER_AUTHORITY_ROOT
    );
    handle = writerLeases.acquire(paths.WRITER_LEASES_DIR, {
      kind: 'api-change-review',
      key: 'api-change:review',
      ownerPid: process.pid,
      pendingChild: false,
      ttlMs: 30000,
      rootDir: paths.WRITER_AUTHORITY_ROOT
    });
    var scan = writerLeases.scan(paths.WRITER_LEASES_DIR, paths.WRITER_AUTHORITY_ROOT);
    if (scan.issues.length || scan.stale.length || scan.active.some(function (row) {
      return row.leaseId !== handle.leaseId;
    })) throw new Error('api-change-review-busy');
    var current = relations.snapshot();
    if (!current.ok || current.committedGenerationId !== request.expectedGenerationId ||
        !currentChange(current, request.changeSetId, request.changeId)) {
      return Object.assign(relations.meta(current.ok ? current : snapshot), {
        ok: false, status: 409, error: 'api-change-review-stale'
      });
    }
    var held = state(request.changeSetId);
    if (!held.ok) return Object.assign(relations.meta(current), {
      ok: false, status: 409, error: 'api-change-review-state-invalid'
    });
    var duplicate = held.reviewed[request.changeId];
    if (duplicate) {
      return Object.assign(relations.meta(current), {
        ok: true, status: 200, reviewed: true,
        changeId: request.changeId, reviewRevision: held.revision
      });
    }
    if (held.revision !== request.expectedReviewRevision) {
      return Object.assign(relations.meta(current), {
        ok: false, status: 409, error: 'api-change-review-conflict',
        reviewRevision: held.revision
      });
    }
    if (held.value.reviews.some(function (row) {
      return row.idempotencyKey === request.idempotencyKey;
    })) {
      return Object.assign(relations.meta(current), {
        ok: false, status: 409, error: 'api-change-review-conflict',
        reviewRevision: held.revision
      });
    }
    var next = held.value;
    next.reviews.unshift({
      changeSetId: request.changeSetId,
      changeId: request.changeId,
      reviewedAt: new Date().toISOString(),
      idempotencyKey: request.idempotencyKey
    });
    next.reviews = next.reviews.slice(0, MAX_ROWS);
    write(next);
    var after = state(request.changeSetId);
    return Object.assign(relations.meta(current), {
      ok: true, status: 200, reviewed: true,
      changeId: request.changeId, reviewRevision: after.revision
    });
  } catch (error) {
    return Object.assign(relations.meta(snapshot), {
      ok: false, status: error && error.message === 'api-change-review-busy' ? 409 : 503,
      error: error && error.message === 'api-change-review-busy'
        ? 'api-change-review-busy' : 'api-change-review-write-failed'
    });
  } finally {
    if (handle) try { writerLeases.release(handle); } catch (ignore) {}
  }
}

module.exports = {
  state: state,
  mark: mark,
  _test: { valid: valid, revision: revision }
};
