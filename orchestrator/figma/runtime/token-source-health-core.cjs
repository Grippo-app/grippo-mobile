'use strict';

// Pure source-health state machine shared by the ESM domain tests and the
// CommonJS site runtime. Health is operational evidence, never design
// authority. In particular, reserving a sequence cannot change a token value.

var limits = require('./program-limits.cjs');
var identity = require('./token-identity.cjs');

function compareText(left, right) {
  var a = String(left), b = String(right);
  return a < b ? -1 : a > b ? 1 : 0;
}
function clone(value) { return JSON.parse(JSON.stringify(value)); }
function exact(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}
function withoutHash(value) {
  var out = Object.assign({}, value);
  delete out.semanticHash;
  return out;
}
function iso(value) {
  var parsed = Date.parse(value);
  return Number.isFinite(parsed) && new Date(parsed).toISOString() === value;
}
function bucketRole(prefix, bucket) {
  if (!Number.isInteger(bucket) || bucket < 0 || bucket >= limits.tokenHealthBuckets) {
    throw new Error('TOKEN_SOURCE_BUCKET_INVALID');
  }
  return prefix + ':' + String(bucket).padStart(3, '0');
}
function validateAttempt(attempt, sourceId) {
  if (!attempt || typeof attempt !== 'object' || Array.isArray(attempt)) {
    throw new Error('TOKEN_SOURCE_HEALTH_ATTEMPT_INVALID: ' + sourceId);
  }
  if (!iso(attempt.at)) throw new Error('TOKEN_SOURCE_HEALTH_TIME_INVALID: ' + sourceId);
  if (!Number.isSafeInteger(attempt.captureSequence) || attempt.captureSequence < 1) {
    throw new Error('TOKEN_SOURCE_SEQUENCE_INVALID: ' + sourceId);
  }
  if (!/^tokop_[A-Za-z0-9_-]{16,96}$/.test(String(attempt.operationId || '')) ||
      ['reserved', 'published', 'no-op', 'failed', 'superseded'].indexOf(attempt.outcome) < 0) {
    throw new Error('TOKEN_SOURCE_HEALTH_ATTEMPT_INVALID: ' + sourceId);
  }
  if (attempt.outcome === 'reserved' &&
      !/^(?:fsj-[a-f0-9]{32}|tokplan_[A-Za-z0-9_-]{16,96})$/.test(String(attempt.ownerId || ''))) {
    throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_INVALID: ' + sourceId);
  }
  var expectedKeys = ['operationId', 'captureSequence', 'at', 'outcome']
    .concat(attempt.outcome === 'reserved' ? ['ownerId'] : [])
    .concat(attempt.outcome === 'published' || attempt.outcome === 'no-op' ? ['evidenceHash'] : []);
  if (!exact(attempt, expectedKeys) ||
      (attempt.outcome === 'published' || attempt.outcome === 'no-op') &&
        !/^sha256:[a-f0-9]{64}$/.test(String(attempt.evidenceHash || ''))) {
    throw new Error('TOKEN_SOURCE_HEALTH_ATTEMPT_INVALID: ' + sourceId);
  }
}
function validateProspectiveReservation(reservation) {
  if (!reservation || typeof reservation !== 'object' || Array.isArray(reservation) ||
      !/^tokop_[A-Za-z0-9_-]{16,96}$/.test(String(reservation.captureOperationId || '')) ||
      reservation.captureSequence !== 1 || !iso(reservation.reservedAt) ||
      !/^fsj-[a-f0-9]{32}$/.test(String(reservation.ownerId || ''))) {
    throw new Error('TOKEN_SOURCE_PROSPECTIVE_RESERVATION_INVALID');
  }
}
function materializeSourceHealth(options) {
  options = options || {};
  var previous = options.previous || null;
  var sourceIndexHash = options.sourceIndexHash;
  var healthRevision = options.healthRevision;
  var updates = options.updates || [];
  var prospectiveReservations = options.prospectiveReservations || [];
  var releaseProspectiveOperationIds = options.releaseProspectiveOperationIds || [];
  var jobSummary = options.jobSummary || null;
  if (!/^sha256:[a-f0-9]{64}$/.test(String(sourceIndexHash || ''))) {
    throw new Error('TOKEN_SOURCE_HEALTH_INDEX_REVISION_INVALID');
  }
  if (!Number.isSafeInteger(healthRevision) || healthRevision < 0) {
    throw new Error('TOKEN_SOURCE_HEALTH_REVISION_INVALID');
  }
  if (previous && sourceHealthSemanticError({ index: previous.index, shards: previous.shards })) {
    throw new Error('TOKEN_SOURCE_HEALTH_PREVIOUS_INVALID');
  }
  var records = new Map();
  (previous ? previous.shards : []).forEach(function (shard) {
    shard.records.forEach(function (record) { records.set(record.sourceId, clone(record)); });
  });
  var prospective = new Map();
  if (previous) {
    if (!Array.isArray(previous.index.prospectiveReservations)) {
      throw new Error('TOKEN_SOURCE_PROSPECTIVE_RESERVATIONS_INVALID');
    }
    previous.index.prospectiveReservations.forEach(function (reservation) {
      validateProspectiveReservation(reservation);
      if (prospective.has(reservation.captureOperationId)) {
        throw new Error('TOKEN_SOURCE_PROSPECTIVE_RESERVATION_DUPLICATE');
      }
      prospective.set(reservation.captureOperationId, clone(reservation));
    });
  }
  releaseProspectiveOperationIds.forEach(function (operationId) {
    if (!/^tokop_[A-Za-z0-9_-]{16,96}$/.test(String(operationId || ''))) {
      throw new Error('TOKEN_SOURCE_PROSPECTIVE_RESERVATION_INVALID');
    }
    prospective.delete(operationId);
  });
  prospectiveReservations.forEach(function (reservation) {
    validateProspectiveReservation(reservation);
    if (prospective.has(reservation.captureOperationId)) {
      throw new Error('TOKEN_SOURCE_PROSPECTIVE_RESERVATION_DUPLICATE');
    }
    prospective.set(reservation.captureOperationId, clone(reservation));
  });
  updates.forEach(function (update) {
    validateAttempt(update.latestAttempt, update.sourceId);
    var prior = records.get(update.sourceId);
    if (prior && update.latestAttempt.captureSequence < prior.issuedSequenceHighWatermark) {
      throw new Error('TOKEN_SOURCE_SEQUENCE_SUPERSEDED: ' + update.sourceId);
    }
    var issuedSequenceHighWatermark = Math.max(
      prior ? prior.issuedSequenceHighWatermark : 0,
      update.latestAttempt.captureSequence,
      update.issuedSequenceHighWatermark || 0
    );
    var next = {
      sourceId: update.sourceId,
      issuedSequenceHighWatermark: issuedSequenceHighWatermark,
      latestAttempt: update.latestAttempt
    };
    if (update.latestAttempt.outcome === 'published' || update.latestAttempt.outcome === 'no-op') {
      next.latestSuccess = update.latestAttempt;
      if (prior && prior.latestFailure) next.latestFailure = prior.latestFailure;
    } else if (update.latestAttempt.outcome === 'failed') {
      if (!update.failure || update.failure.captureSequence !== update.latestAttempt.captureSequence) {
        throw new Error('TOKEN_SOURCE_HEALTH_FAILURE_INVALID: ' + update.sourceId);
      }
      next.latestFailure = update.failure;
      if (prior && prior.latestSuccess) next.latestSuccess = prior.latestSuccess;
    } else {
      if (prior && prior.latestSuccess) next.latestSuccess = prior.latestSuccess;
      if (prior && prior.latestFailure) next.latestFailure = prior.latestFailure;
    }
    records.set(update.sourceId, next);
  });
  if (records.size + prospective.size > limits.tokenSourceRecordsMax) {
    throw new Error('TOKEN_SOURCE_HEALTH_RECORD_LIMIT_EXCEEDED');
  }
  var bucketRows = Array.from({ length: limits.tokenHealthBuckets }, function () { return []; });
  records.forEach(function (record) { bucketRows[identity.sourceBucket(record.sourceId)].push(record); });
  var shards = [];
  bucketRows.forEach(function (rows, bucket) {
    if (!rows.length) return;
    var shard = {
      schemaVersion: 1,
      bucket: bucket,
      records: rows.sort(function (a, b) { return compareText(a.sourceId, b.sourceId); }),
      semanticHash: 'sha256:' + '0'.repeat(64)
    };
    shard.semanticHash = identity.hash(withoutHash(shard));
    shards.push(shard);
  });
  var summaries = []
    .concat(previous ? previous.index.jobSummaries : [])
    .concat(jobSummary ? [jobSummary] : [])
    .slice(-limits.jobSummariesMax);
  var index = {
    schemaVersion: 1,
    healthRevision: healthRevision,
    sourceIndexRevision: sourceIndexHash,
    prospectiveReservations: Array.from(prospective.values()).sort(function (a, b) {
      return compareText(a.captureOperationId, b.captureOperationId);
    }),
    shards: shards.map(function (shard) {
      return {
        role: bucketRole('token-source-health-shard', shard.bucket),
        hash: shard.semanticHash,
        recordCount: shard.records.length
      };
    }),
    jobSummaries: summaries,
    semanticHash: 'sha256:' + '0'.repeat(64)
  };
  index.semanticHash = identity.hash(withoutHash(index));
  return { index: index, shards: shards };
}
function reserveSourceSequence(options) {
  var snapshot = options.snapshot || null;
  var sourceId = options.sourceId;
  var operationId = options.operationId;
  var ownerId = options.ownerId;
  var at = options.at;
  if (!/^tokop_[A-Za-z0-9_-]{16,96}$/.test(String(operationId || ''))) {
    throw new Error('TOKEN_SOURCE_OPERATION_ID_INVALID');
  }
  if (!iso(at)) throw new Error('TOKEN_SOURCE_RESERVATION_TIME_INVALID');
  if (!/^(?:fsj-[a-f0-9]{32}|tokplan_[A-Za-z0-9_-]{16,96})$/.test(String(ownerId || ''))) {
    throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_INVALID');
  }
  var existing = (snapshot ? snapshot.shards : []).flatMap(function (shard) { return shard.records; })
    .find(function (record) { return record.sourceId === sourceId; });
  var captureSequence = (existing ? existing.issuedSequenceHighWatermark : 0) + 1;
  var attempt = {
    operationId: operationId,
    captureSequence: captureSequence,
    at: at,
    outcome: 'reserved',
    ownerId: ownerId
  };
  var materialized = materializeSourceHealth({
    previous: snapshot,
    sourceIndexHash: snapshot ? snapshot.index.sourceIndexRevision : 'sha256:' + '0'.repeat(64),
    healthRevision: snapshot ? snapshot.index.healthRevision + 1 : 0,
    updates: [{ sourceId: sourceId, issuedSequenceHighWatermark: captureSequence, latestAttempt: attempt }]
  });
  materialized.reservation = { sourceId: sourceId, operationId: operationId, captureSequence: captureSequence };
  return materialized;
}
function sourceHealthSemanticError(snapshot) {
  if (!snapshot || typeof snapshot !== 'object' || Array.isArray(snapshot) ||
      !exact(snapshot, ['index', 'shards']) || !snapshot.index || !Array.isArray(snapshot.shards)) {
    return 'health snapshot malformed';
  }
  var index = snapshot.index;
  if (!exact(index, ['schemaVersion', 'healthRevision', 'sourceIndexRevision',
    'prospectiveReservations', 'shards', 'jobSummaries', 'semanticHash']) ||
      index.schemaVersion !== 1 ||
      !Number.isSafeInteger(index.healthRevision) || index.healthRevision < 0 ||
      !/^sha256:[a-f0-9]{64}$/.test(String(index.sourceIndexRevision || '')) ||
      !/^sha256:[a-f0-9]{64}$/.test(String(index.semanticHash || '')) ||
      !Array.isArray(index.prospectiveReservations) || !Array.isArray(index.shards) ||
      !Array.isArray(index.jobSummaries) || index.shards.length > limits.tokenHealthBuckets ||
      snapshot.shards.length > limits.tokenHealthBuckets) return 'health index malformed';
  if (snapshot.shards.length !== snapshot.index.shards.length) return 'health shard count mismatch';
  var prospectiveOperations = new Set();
  var previousProspectiveOperation = null;
  for (var p = 0; p < snapshot.index.prospectiveReservations.length; p++) {
    var prospective = snapshot.index.prospectiveReservations[p];
    try { validateProspectiveReservation(prospective); } catch (error) {
      return 'health prospective reservation invalid';
    }
    if (prospectiveOperations.has(prospective.captureOperationId)) {
      return 'duplicate health prospective reservation';
    }
    if (previousProspectiveOperation !== null &&
        compareText(previousProspectiveOperation, prospective.captureOperationId) >= 0) {
      return 'health prospective reservations are not sorted';
    }
    previousProspectiveOperation = prospective.captureOperationId;
    prospectiveOperations.add(prospective.captureOperationId);
  }
  var roles = new Set(), sourceIds = new Set();
  var recordCount = snapshot.index.prospectiveReservations.length, previousRole = null;
  for (var i = 0; i < snapshot.shards.length; i++) {
    var shard = snapshot.shards[i];
    if (!exact(shard, ['schemaVersion', 'bucket', 'records', 'semanticHash']) ||
        shard.schemaVersion !== 1 || !Number.isInteger(shard.bucket) ||
        shard.bucket < 0 || shard.bucket >= limits.tokenHealthBuckets ||
        !Array.isArray(shard.records) || !shard.records.length ||
        !/^sha256:[a-f0-9]{64}$/.test(String(shard.semanticHash || ''))) {
      return 'health shard malformed';
    }
    var role = bucketRole('token-source-health-shard', shard.bucket);
    var descriptor = snapshot.index.shards.find(function (row) { return row.role === role; });
    if (!descriptor ||
        !exact(descriptor, ['role', 'hash', 'recordCount']) ||
        !/^sha256:[a-f0-9]{64}$/.test(String(descriptor.hash || '')) ||
        !Number.isSafeInteger(descriptor.recordCount) || descriptor.recordCount < 1 ||
        descriptor.hash !== shard.semanticHash || descriptor.recordCount !== shard.records.length) {
      return 'health descriptor mismatch for ' + role;
    }
    if (identity.hash(withoutHash(shard)) !== shard.semanticHash) return 'health shard hash mismatch for ' + role;
    if (roles.has(role)) return 'duplicate health shard ' + role;
    if (previousRole !== null && compareText(previousRole, role) >= 0) return 'health shards are not sorted';
    previousRole = role;
    roles.add(role);
    recordCount += shard.records.length;
    var previousSourceId = null;
    for (var r = 0; r < shard.records.length; r++) {
      var record = shard.records[r];
      if (!record || typeof record !== 'object' || Array.isArray(record) ||
          !/^otsrc:sha256:[a-f0-9]{64}$/.test(String(record.sourceId || '')) ||
          identity.sourceBucket(record.sourceId) !== shard.bucket ||
          sourceIds.has(record.sourceId) ||
          previousSourceId !== null && compareText(previousSourceId, record.sourceId) >= 0 ||
          !Number.isSafeInteger(record.issuedSequenceHighWatermark) ||
          record.issuedSequenceHighWatermark < 1) return 'health record malformed';
      var expectedRecordKeys = ['sourceId', 'issuedSequenceHighWatermark', 'latestAttempt']
        .concat(record.latestSuccess ? ['latestSuccess'] : [])
        .concat(record.latestFailure ? ['latestFailure'] : []);
      if (!exact(record, expectedRecordKeys)) return 'health record has unknown fields';
      try {
        validateAttempt(record.latestAttempt, record.sourceId);
        if (record.latestSuccess) validateAttempt(record.latestSuccess, record.sourceId);
      } catch (error) { return 'health attempt malformed for ' + record.sourceId; }
      if (record.latestSuccess &&
          ['published', 'no-op'].indexOf(record.latestSuccess.outcome) < 0) {
        return 'health latest success outcome invalid for ' + record.sourceId;
      }
      if (record.latestFailure) {
        if (!exact(record.latestFailure,
          ['operationId', 'captureSequence', 'at', 'code', 'retryable']) ||
            !/^tokop_[A-Za-z0-9_-]{16,96}$/.test(String(record.latestFailure.operationId || '')) ||
            !Number.isSafeInteger(record.latestFailure.captureSequence) ||
            record.latestFailure.captureSequence < 1 || !iso(record.latestFailure.at) ||
            !/^TOKEN_[A-Z0-9_]{1,90}$/.test(String(record.latestFailure.code || '')) ||
            typeof record.latestFailure.retryable !== 'boolean') {
          return 'health latest failure malformed for ' + record.sourceId;
        }
      }
      if (record.latestAttempt.captureSequence > record.issuedSequenceHighWatermark ||
          record.latestSuccess &&
            record.latestSuccess.captureSequence > record.issuedSequenceHighWatermark ||
          record.latestFailure &&
            record.latestFailure.captureSequence > record.issuedSequenceHighWatermark) {
        return 'health high-watermark behind ' + record.sourceId;
      }
      sourceIds.add(record.sourceId);
      previousSourceId = record.sourceId;
    }
  }
  if (recordCount > limits.tokenSourceRecordsMax) return 'health record limit exceeded';
  if (snapshot.index.jobSummaries.length > limits.jobSummariesMax) return 'health job history limit exceeded';
  for (var j = 0; j < snapshot.index.jobSummaries.length; j++) {
    var summary = snapshot.index.jobSummaries[j];
    var summaryKeys = ['jobId', 'action', 'startedAt', 'finishedAt', 'outcome', 'sourceCount']
      .concat(summary && summary.errorCode !== undefined ? ['errorCode'] : []);
    if (!exact(summary, summaryKeys) ||
        typeof summary.jobId !== 'string' || summary.jobId.length < 8 || summary.jobId.length > 128 ||
        ['refresh-known-token-sources', 'component-token-capture', 'task-ingestion',
          'health-recovery', 'detach-origin', 'retire-source'].indexOf(summary.action) < 0 ||
        !iso(summary.startedAt) || !iso(summary.finishedAt) ||
        Date.parse(summary.finishedAt) < Date.parse(summary.startedAt) ||
        ['published', 'no-op', 'failed', 'cancelled',
          'published-health-recovery-required'].indexOf(summary.outcome) < 0 ||
        !Number.isSafeInteger(summary.sourceCount) || summary.sourceCount < 0 ||
        summary.sourceCount > limits.tokenSourceRecordsMax ||
        summary.errorCode !== undefined &&
          !/^TOKEN_[A-Z0-9_]{1,90}$/.test(String(summary.errorCode || ''))) {
      return 'health job summary malformed';
    }
  }
  if (identity.hash(withoutHash(snapshot.index)) !== snapshot.index.semanticHash) return 'health index hash mismatch';
  return null;
}
function sourceHealthSnapshotHash(snapshot) {
  return identity.hash({
    index: snapshot.index,
    shards: snapshot.shards.map(function (shard) {
      return {
        role: bucketRole('token-source-health-shard', shard.bucket),
        semanticHash: shard.semanticHash
      };
    }).sort(function (a, b) { return compareText(a.role, b.role); })
  });
}
function sourceFreshness(snapshot, sourceIndex, checkedAtMs) {
  if (sourceHealthSemanticError(snapshot) ||
      !sourceIndex || typeof sourceIndex !== 'object' || Array.isArray(sourceIndex) ||
      !/^sha256:[a-f0-9]{64}$/.test(String(sourceIndex.semanticHash || '')) ||
      !Array.isArray(sourceIndex.sources) ||
      snapshot.index.sourceIndexRevision !== sourceIndex.semanticHash) {
    return { state: 'unknown', reason: 'source-health-unavailable' };
  }
  var evaluatedAt = Number.isFinite(checkedAtMs) ? checkedAtMs : Date.now();
  var records = new Map();
  snapshot.shards.forEach(function (shard) {
    shard.records.forEach(function (record) { records.set(record.sourceId, record); });
  });
  var stale = false;
  for (var index = 0; index < sourceIndex.sources.length; index++) {
    var source = sourceIndex.sources[index];
    if (!source || source.lifecycle !== 'active') continue;
    var accepted = source.acceptedBatch;
    var record = records.get(source.sourceId);
    // A later successful semantic no-op is fresh evidence that the already
    // accepted batch still represents the provider. It deliberately does not
    // republish the source index, so its sequence may be newer than the
    // accepted batch sequence. Only a success older than the accepted batch
    // fails to prove that batch.
    if (!accepted || !record || !record.latestSuccess ||
        record.latestSuccess.captureSequence < accepted.captureSequence ||
        !/^sha256:[a-f0-9]{64}$/.test(String(record.latestSuccess.evidenceHash || ''))) {
      return { state: 'unknown', reason: 'source-health-acceptance-unproven' };
    }
    if (evaluatedAt - Date.parse(record.latestSuccess.at) >
        limits.tokenSourceFreshnessMaxAgeMs) stale = true;
    if (record.latestAttempt &&
        (record.latestAttempt.outcome === 'failed' ||
          record.latestAttempt.outcome === 'reserved')) stale = true;
  }
  return stale
    ? { state: 'stale', reason: 'source-refresh-required' }
    : { state: 'current', reason: 'source-health-current' };
}

module.exports = {
  materializeSourceHealth: materializeSourceHealth,
  reserveSourceSequence: reserveSourceSequence,
  sourceHealthSemanticError: sourceHealthSemanticError,
  sourceHealthSnapshotHash: sourceHealthSnapshotHash,
  sourceFreshness: sourceFreshness
};
