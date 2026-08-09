'use strict';

// Durable operational health for observed-token sources. Immutable snapshots
// are written first and a tiny current pointer is switched with exact CAS.
// Design generations never read this store as token-value authority.

var crypto = require('crypto');
var path = require('path');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var limits = require(path.join(paths.ORCHESTRATOR_DIR, 'figma', 'runtime', 'program-limits.cjs'));
var identity = require(path.join(paths.ORCHESTRATOR_DIR, 'figma', 'runtime', 'token-identity.cjs'));
var core = require(path.join(paths.ORCHESTRATOR_DIR, 'figma', 'runtime', 'token-source-health-core.cjs'));

var ROOT = path.join(paths.FIGMA_CACHE_DIR, 'token-source-health');
var SNAPSHOTS_DIR = path.join(ROOT, 'snapshots');
var POINTER_FILE = path.join(ROOT, 'current.json');
var POINTER_MAX = 16 * 1024;
var SNAPSHOT_MAX = limits.artifactBytesMax;
var SNAPSHOT_RE = /^health-([0-9]{1,16})-([a-f0-9]{64})\.json$/;
var ZERO_HASH = 'sha256:' + '0'.repeat(64);

function now() { return new Date().toISOString(); }
function bytesHash(bytes) {
  return 'sha256:' + crypto.createHash('sha256').update(bytes).digest('hex');
}
function exact(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}
function jsonBytes(value) { return Buffer.from(JSON.stringify(value, null, 2) + '\n'); }
function ensureDirectories() {
  if (!fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, ROOT, { create: true, mode: 0o700 }) ||
      !fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, SNAPSHOTS_DIR, { create: true, mode: 0o700 })) {
    throw new Error('TOKEN_SOURCE_HEALTH_DIRECTORY_UNSAFE');
  }
}
function validWrapper(value) {
  try {
    return exact(value, ['schemaVersion', 'createdAt', 'index', 'shards']) &&
      value.schemaVersion === 1 && new Date(value.createdAt).toISOString() === value.createdAt &&
      !core.sourceHealthSemanticError({ index: value.index, shards: value.shards });
  } catch (error) { return false; }
}
function readPointer() {
  var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, ROOT, POINTER_FILE);
  if (inspected && inspected.status === 'missing') return { state: 'absent' };
  var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, ROOT, POINTER_FILE, POINTER_MAX);
  if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') return { state: 'invalid' };
  var value;
  try { value = JSON.parse(hit.bytes.toString('utf8')); } catch (error) { return { state: 'invalid' }; }
  if (!exact(value, ['schemaVersion', 'snapshotFile', 'snapshotHash', 'healthRevision', 'indexSemanticHash', 'updatedAt']) ||
      value.schemaVersion !== 1 || !SNAPSHOT_RE.test(String(value.snapshotFile || '')) ||
      !/^sha256:[a-f0-9]{64}$/.test(String(value.snapshotHash || '')) ||
      !/^sha256:[a-f0-9]{64}$/.test(String(value.indexSemanticHash || '')) ||
      !Number.isSafeInteger(value.healthRevision) || value.healthRevision < 0) return { state: 'invalid' };
  return { state: 'present', value: value, bytes: hit.bytes, proof: hit.stat };
}
function pointerForRecovery() {
  var pointer = readPointer();
  if (pointer.state !== 'invalid') return pointer;
  var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, ROOT, POINTER_FILE, POINTER_MAX);
  if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') {
    throw new Error('TOKEN_SOURCE_HEALTH_RECOVERY_FAILED');
  }
  return { state: 'invalid', bytes: hit.bytes, proof: hit.stat };
}
function readSnapshot(name, expectedHash) {
  var nameMatch = SNAPSHOT_RE.exec(String(name || ''));
  if (!nameMatch) return null;
  var file = path.join(SNAPSHOTS_DIR, name);
  var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, SNAPSHOTS_DIR, file, SNAPSHOT_MAX);
  if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') return null;
  var actualHash = bytesHash(hit.bytes);
  if (actualHash !== 'sha256:' + nameMatch[2] ||
      expectedHash && actualHash !== expectedHash) return null;
  var value;
  try { value = JSON.parse(hit.bytes.toString('utf8')); } catch (error) { return null; }
  return validWrapper(value) && value.index.healthRevision === Number(nameMatch[1])
    ? { value: value, bytes: hit.bytes, proof: hit.stat } : null;
}
function recoverWithoutPointer() {
  var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, SNAPSHOTS_DIR, 1024);
  if (!listed.ok) throw new Error('TOKEN_SOURCE_HEALTH_RECOVERY_FAILED');
  var candidates = listed.names.map(function (name) {
    var match = SNAPSHOT_RE.exec(name);
    if (!match) throw new Error('TOKEN_SOURCE_HEALTH_RECOVERY_FAILED');
    var read = readSnapshot(name);
    if (!read) throw new Error('TOKEN_SOURCE_HEALTH_RECOVERY_FAILED');
    return { name: name, revision: Number(match[1]), read: read };
  }).sort(function (a, b) {
    return b.revision - a.revision || a.name.localeCompare(b.name);
  });
  if (!candidates.length) return null;
  var top = candidates.filter(function (candidate) { return candidate.revision === candidates[0].revision; });
  if (top.length !== 1) throw new Error('TOKEN_SOURCE_HEALTH_RECOVERY_AMBIGUOUS');
  return top[0];
}
function current(options) {
  options = options || {};
  ensureDirectories();
  var pointer = readPointer();
  if (pointer.state === 'invalid') throw new Error('TOKEN_SOURCE_HEALTH_POINTER_INVALID');
  if (pointer.state === 'present') {
    var read = readSnapshot(pointer.value.snapshotFile, pointer.value.snapshotHash);
    if (!read || read.value.index.healthRevision !== pointer.value.healthRevision ||
        read.value.index.semanticHash !== pointer.value.indexSemanticHash) {
      throw new Error('TOKEN_SOURCE_HEALTH_SNAPSHOT_INVALID');
    }
    return {
      snapshot: { index: read.value.index, shards: read.value.shards },
      pointer: pointer
    };
  }
  var recovered = recoverWithoutPointer();
  if (recovered) {
    var recoveredPointer = pointerFor(recovered.name, recovered.read.bytes, recovered.read.value);
    var result = fileGuards.publishNoClobberRegularFileUnder(
      paths.PROJECT_ROOT, ROOT, POINTER_FILE, jsonBytes(recoveredPointer),
      { create: true, directoryMode: 0o700, mode: 0o600, maxBytes: POINTER_MAX }
    );
    if (!result.ok && result.code !== 'already-exists') throw new Error('TOKEN_SOURCE_HEALTH_RECOVERY_FAILED');
    return current(options);
  }
  var sourceIndexHash = options.sourceIndexHash || ZERO_HASH;
  return {
    snapshot: core.materializeSourceHealth({
      previous: null,
      sourceIndexHash: sourceIndexHash,
      healthRevision: 0,
      updates: []
    }),
    pointer: { state: 'absent' }
  };
}
function peek() {
  var root = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, path.dirname(ROOT), ROOT);
  if (root && root.status === 'missing') return null;
  if (!root || root.status !== 'present' || !root.stat || !root.stat.isDirectory() ||
      root.stat.isSymbolicLink()) throw new Error('TOKEN_SOURCE_HEALTH_DIRECTORY_UNSAFE');
  var pointer = readPointer();
  if (pointer.state === 'absent') return null;
  if (pointer.state === 'invalid') throw new Error('TOKEN_SOURCE_HEALTH_POINTER_INVALID');
  var read = readSnapshot(pointer.value.snapshotFile, pointer.value.snapshotHash);
  if (!read || read.value.index.healthRevision !== pointer.value.healthRevision ||
      read.value.index.semanticHash !== pointer.value.indexSemanticHash) {
    throw new Error('TOKEN_SOURCE_HEALTH_SNAPSHOT_INVALID');
  }
  return { snapshot: { index: read.value.index, shards: read.value.shards }, pointer: pointer };
}
function pointerFor(snapshotFile, bytes, wrapper) {
  return {
    schemaVersion: 1,
    snapshotFile: snapshotFile,
    snapshotHash: bytesHash(bytes),
    healthRevision: wrapper.index.healthRevision,
    indexSemanticHash: wrapper.index.semanticHash,
    updatedAt: now()
  };
}
function publishSnapshot(snapshot, expectedPointer) {
  var wrapper = {
    schemaVersion: 1,
    createdAt: now(),
    index: snapshot.index,
    shards: snapshot.shards
  };
  if (!validWrapper(wrapper)) throw new Error('TOKEN_SOURCE_HEALTH_SNAPSHOT_INVALID');
  var bytes = jsonBytes(wrapper);
  if (bytes.length > SNAPSHOT_MAX) throw new Error('TOKEN_SOURCE_HEALTH_SIZE_LIMIT_EXCEEDED');
  var digest = bytesHash(bytes).slice('sha256:'.length);
  var name = 'health-' + snapshot.index.healthRevision + '-' + digest + '.json';
  var file = path.join(SNAPSHOTS_DIR, name);
  var write = fileGuards.atomicReplaceRegularFileResult(
    paths.PROJECT_ROOT, SNAPSHOTS_DIR, file, bytes,
    { create: true, directoryMode: 0o700, mode: 0o600, maxBytes: SNAPSHOT_MAX }
  );
  if (!write.ok) throw new Error('TOKEN_SOURCE_HEALTH_SNAPSHOT_WRITE_FAILED');
  var pointerBytes = jsonBytes(pointerFor(name, bytes, wrapper));
  var switched;
  if (expectedPointer.state === 'absent') {
    switched = fileGuards.publishNoClobberRegularFileUnder(
      paths.PROJECT_ROOT, ROOT, POINTER_FILE, pointerBytes,
      { create: true, directoryMode: 0o700, mode: 0o600, maxBytes: POINTER_MAX }
    );
  } else if (expectedPointer.state === 'present' || expectedPointer.state === 'invalid') {
    switched = fileGuards.compareAndSwapRegularFileUnder(
      paths.PROJECT_ROOT, ROOT, POINTER_FILE, POINTER_MAX,
      { proof: expectedPointer.proof, bytes: expectedPointer.bytes },
      pointerBytes, { mode: 0o600 }
    );
  } else throw new Error('TOKEN_SOURCE_HEALTH_POINTER_INVALID');
  if (!switched.ok) return { ok: false, conflict: true, code: switched.code };
  cleanup(name);
  return { ok: true, snapshot: snapshot };
}
function cleanup(currentName) {
  var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, SNAPSHOTS_DIR, 1024);
  if (!listed.ok) return false;
  var rows = listed.names.map(function (name) {
    var match = SNAPSHOT_RE.exec(name);
    return match ? { name: name, revision: Number(match[1]) } : null;
  }).filter(Boolean).sort(function (a, b) {
    return b.revision - a.revision || a.name.localeCompare(b.name);
  });
  var keep = Object.create(null);
  keep[currentName] = 1;
  rows.forEach(function (row) {
    if (Object.keys(keep).length < limits.sourceHealthSnapshotsMax) keep[row.name] = 1;
  });
  rows.forEach(function (row) {
    if (keep[row.name]) return;
    fileGuards.unlinkRegularFileUnder(paths.PROJECT_ROOT, SNAPSHOTS_DIR,
      path.join(SNAPSHOTS_DIR, row.name), { allowMissing: true });
  });
  return true;
}
function recordBySource(snapshot) {
  var records = new Map();
  snapshot.shards.forEach(function (shard) {
    shard.records.forEach(function (record) { records.set(record.sourceId, record); });
  });
  return records;
}
function highWatermarks(sourceIndexHash) {
  var held = current({ sourceIndexHash: sourceIndexHash }).snapshot;
  if (held.index.sourceIndexRevision !== sourceIndexHash) {
    throw new Error('TOKEN_SOURCE_HEALTH_REBASE_REQUIRED');
  }
  var rows = [];
  held.shards.forEach(function (shard) {
    shard.records.forEach(function (record) {
      rows.push({
        sourceId: record.sourceId,
        acceptedSequence: record.issuedSequenceHighWatermark
      });
    });
  });
  return rows.sort(function (left, right) { return left.sourceId.localeCompare(right.sourceId); });
}
function sourceFreshness(sourceIndex) {
  try {
    var present = peek();
    if (!present) return { state: 'unknown', reason: 'source-health-unavailable' };
    return core.sourceFreshness(present.snapshot, sourceIndex);
  } catch (error) {
    if (/^TOKEN_SOURCE_HEALTH_/.test(String(error && error.message || ''))) {
      return { state: 'unknown', reason: 'source-health-unavailable' };
    }
    throw error;
  }
}
function recoverySnapshots() {
  ensureDirectories();
  var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, SNAPSHOTS_DIR, 1024);
  if (!listed.ok) throw new Error('TOKEN_SOURCE_HEALTH_RECOVERY_SCAN_INCOMPLETE');
  var rows = listed.names.map(function (name) {
    var match = SNAPSHOT_RE.exec(name);
    if (!match) throw new Error('TOKEN_SOURCE_HEALTH_RECOVERY_SCAN_INCOMPLETE');
    var read = readSnapshot(name);
    if (!read) throw new Error('TOKEN_SOURCE_HEALTH_RECOVERY_SCAN_INCOMPLETE');
    return { name: name, revision: Number(match[1]), read: read };
  }).sort(function (left, right) {
    return right.revision - left.revision || left.name.localeCompare(right.name);
  });
  if (!rows.length) throw new Error('TOKEN_SOURCE_HEALTH_RECOVERY_UNPROVEN');
  if (rows.length > 1 && rows[0].revision === rows[1].revision) {
    throw new Error('TOKEN_SOURCE_HEALTH_RECOVERY_AMBIGUOUS');
  }
  return rows;
}
function canonicalInstant(value) {
  try { return typeof value === 'string' && new Date(value).toISOString() === value; }
  catch (error) { return false; }
}
function recoverExact(options) {
  options = options || {};
  if (!/^sha256:[a-f0-9]{64}$/.test(String(options.sourceIndexHash || '')) ||
      !Array.isArray(options.acceptedSources) || !Array.isArray(options.reservations) ||
      !Array.isArray(options.prospectiveReservations || [])) {
    throw new Error('TOKEN_SOURCE_HEALTH_RECOVERY_INVALID');
  }
  var expectedPointer = pointerForRecovery();
  var snapshots = recoverySnapshots();
  var base = snapshots[0].read.value;
  var candidates = Object.create(null);
  var acceptedBySource = Object.create(null);
  function merge(row, label) {
    identity.sourceBucket(row.sourceId);
    if (!/^tokop_[A-Za-z0-9_-]{16,96}$/.test(String(row.captureOperationId || '')) ||
        !Number.isSafeInteger(row.captureSequence) || row.captureSequence < 1 ||
        row.at != null && !canonicalInstant(row.at) ||
        row.successAt != null && !canonicalInstant(row.successAt) ||
        row.captureEvidenceHash != null &&
          !/^sha256:[a-f0-9]{64}$/.test(String(row.captureEvidenceHash || ''))) {
      throw new Error('TOKEN_SOURCE_HEALTH_RECOVERY_INVALID: ' + label);
    }
    var prior = candidates[row.sourceId];
    if (prior && prior.captureSequence === row.captureSequence &&
        prior.captureOperationId !== row.captureOperationId) {
      throw new Error('TOKEN_SOURCE_HEALTH_RECOVERY_AMBIGUOUS');
    }
    if (!prior || row.captureSequence > prior.captureSequence) {
      candidates[row.sourceId] = {
        sourceId: row.sourceId,
        captureOperationId: row.captureOperationId,
        captureSequence: row.captureSequence,
        at: row.at || null,
        successAt: row.successAt || null,
        captureEvidenceHash: row.captureEvidenceHash || null
      };
    } else if (row.captureSequence === prior.captureSequence) {
      if (prior.captureEvidenceHash && row.captureEvidenceHash &&
          prior.captureEvidenceHash !== row.captureEvidenceHash) {
        throw new Error('TOKEN_SOURCE_HEALTH_RECOVERY_AMBIGUOUS');
      }
      if (!prior.at && row.at) prior.at = row.at;
      if (!prior.successAt && row.successAt) prior.successAt = row.successAt;
      if (!prior.captureEvidenceHash && row.captureEvidenceHash) {
        prior.captureEvidenceHash = row.captureEvidenceHash;
      }
    }
  }
  snapshots.forEach(function (snapshot) {
    snapshot.read.value.shards.forEach(function (shard) {
      shard.records.forEach(function (record) {
        merge({
          sourceId: record.sourceId,
          captureOperationId: record.latestAttempt.operationId,
          captureSequence: record.issuedSequenceHighWatermark,
          at: record.latestAttempt.at,
          successAt: record.latestAttempt.captureSequence === record.issuedSequenceHighWatermark &&
            (record.latestAttempt.outcome === 'published' || record.latestAttempt.outcome === 'no-op')
            ? record.latestAttempt.at : null,
          captureEvidenceHash: record.latestAttempt.captureSequence === record.issuedSequenceHighWatermark &&
            (record.latestAttempt.outcome === 'published' || record.latestAttempt.outcome === 'no-op')
            ? record.latestAttempt.evidenceHash : null
        }, 'health snapshot');
      });
    });
  });
  options.acceptedSources.forEach(function (row) {
    if (acceptedBySource[row.sourceId]) {
      throw new Error('TOKEN_SOURCE_HEALTH_RECOVERY_AMBIGUOUS');
    }
    acceptedBySource[row.sourceId] = row;
    merge(row, 'accepted source');
  });
  options.reservations.forEach(function (row) { merge(row, 'reservation owner'); });

  var baseSnapshot = { index: base.index, shards: base.shards };
  var baseRecords = recordBySource(baseSnapshot);
  var recoveryAt = now(), updates = [];
  Object.keys(candidates).sort().forEach(function (sourceId) {
    var candidate = candidates[sourceId];
    var record = baseRecords.get(sourceId);
    if (record && record.issuedSequenceHighWatermark > candidate.captureSequence) return;
    if (record && record.issuedSequenceHighWatermark === candidate.captureSequence) {
      if (record.latestAttempt.operationId !== candidate.captureOperationId) {
        throw new Error('TOKEN_SOURCE_HEALTH_RECOVERY_AMBIGUOUS');
      }
      return;
    }
    var accepted = acceptedBySource[candidate.sourceId];
    var canRestoreSuccess = accepted &&
      accepted.captureOperationId === candidate.captureOperationId &&
      accepted.captureSequence === candidate.captureSequence &&
      accepted.captureEvidenceHash === candidate.captureEvidenceHash &&
      candidate.successAt;
    var attempt = {
      operationId: candidate.captureOperationId,
      captureSequence: candidate.captureSequence,
      at: canRestoreSuccess ? candidate.successAt : candidate.at || recoveryAt,
      outcome: canRestoreSuccess ? 'published' : 'superseded'
    };
    if (canRestoreSuccess) attempt.evidenceHash = candidate.captureEvidenceHash;
    updates.push({
      sourceId: candidate.sourceId,
      issuedSequenceHighWatermark: candidate.captureSequence,
      latestAttempt: attempt
    });
  });
  var heldProspective = Object.create(null);
  base.index.prospectiveReservations.forEach(function (row) {
    heldProspective[row.captureOperationId] = row;
  });
  var newProspective = [];
  (options.prospectiveReservations || []).forEach(function (row) {
    validateProspectiveForRecovery(row);
    var prior = heldProspective[row.captureOperationId];
    if (prior && identity.canonical(prior) !== identity.canonical(row)) {
      throw new Error('TOKEN_SOURCE_HEALTH_RECOVERY_AMBIGUOUS');
    }
    if (!prior) {
      heldProspective[row.captureOperationId] = row;
      newProspective.push(row);
    }
  });
  var startedAt = recoveryAt;
  var snapshot = core.materializeSourceHealth({
    previous: baseSnapshot,
    sourceIndexHash: options.sourceIndexHash,
    healthRevision: snapshots[0].revision + 1,
    prospectiveReservations: newProspective,
    updates: updates,
    jobSummary: {
      jobId: 'fsj-' + crypto.createHash('sha256').update(
        options.sourceIndexHash + '\0' + snapshots.map(function (row) { return row.name; }).join('\0')
      ).digest('hex').slice(0, 32),
      action: 'health-recovery',
      startedAt: startedAt,
      finishedAt: now(),
      outcome: 'no-op',
      sourceCount: Object.keys(candidates).length
    }
  });
  var published = publishSnapshot(snapshot, expectedPointer);
  if (!published.ok) throw new Error('TOKEN_SOURCE_HEALTH_WRITE_CONFLICT');
  return published.snapshot;
}
function validateProspectiveForRecovery(row) {
  if (!exact(row, ['captureOperationId', 'captureSequence', 'reservedAt', 'ownerId']) ||
      !/^tokop_[A-Za-z0-9_-]{16,96}$/.test(String(row.captureOperationId || '')) ||
      row.captureSequence !== 1 ||
      !/^(?:fsj-[a-f0-9]{32}|tokplan_[A-Za-z0-9_-]{16,96})$/.test(String(row.ownerId || '')) ||
      !canonicalInstant(row.reservedAt)) {
    throw new Error('TOKEN_SOURCE_HEALTH_RECOVERY_INVALID');
  }
}
function transact(sourceIndexHash, update) {
  for (var attempt = 0; attempt < 5; attempt++) {
    var held = current({ sourceIndexHash: sourceIndexHash });
    var next = update(held.snapshot);
    var result = publishSnapshot(next, held.pointer);
    if (result.ok) return result.snapshot;
  }
  throw new Error('TOKEN_SOURCE_HEALTH_WRITE_CONFLICT');
}
function reserveMany(options) {
  options = options || {};
  if (!/^sha256:[a-f0-9]{64}$/.test(String(options.sourceIndexHash || '')) ||
      !Array.isArray(options.sources) || options.sources.length > limits.tokenSourceRecordsMax ||
      !/^(?:fsj-[a-f0-9]{32}|tokplan_[A-Za-z0-9_-]{16,96})$/.test(String(options.ownerId || '')) ||
      !Number.isSafeInteger(options.prospectiveCount || 0) ||
      (options.prospectiveCount || 0) < 0 || (options.prospectiveCount || 0) > 128 ||
      ((options.prospectiveCount || 0) > 0 && !/^fsj-[a-f0-9]{32}$/.test(String(options.ownerId || '')))) {
    throw new Error('TOKEN_SOURCE_RESERVATION_INVALID');
  }
  var reservedAt = now(), reservations = null, prospectiveReservations = [];
  for (var prospectiveIndex = 0; prospectiveIndex < (options.prospectiveCount || 0); prospectiveIndex++) {
    prospectiveReservations.push({
      captureOperationId: 'tokop_' + crypto.randomBytes(16).toString('hex'),
      captureSequence: 1,
      reservedAt: reservedAt,
      ownerId: options.ownerId
    });
  }
  var snapshot = transact(options.sourceIndexHash, function (previous) {
    var records = recordBySource(previous);
    reservations = options.sources.slice().sort(function (a, b) {
      return a.sourceId.localeCompare(b.sourceId);
    }).map(function (source) {
      identity.sourceBucket(source.sourceId);
      var prior = records.get(source.sourceId);
      var high = Math.max(
        prior ? prior.issuedSequenceHighWatermark : 0,
        Number.isSafeInteger(source.acceptedSequence) ? source.acceptedSequence : 0
      );
      var operationId = 'tokop_' + crypto.randomBytes(16).toString('hex');
      return {
        sourceId: source.sourceId,
        captureOperationId: operationId,
        captureSequence: high + 1,
        reservedAt: reservedAt
      };
    });
    return core.materializeSourceHealth({
      previous: previous,
      sourceIndexHash: options.sourceIndexHash,
      healthRevision: previous.index.healthRevision + 1,
      prospectiveReservations: prospectiveReservations,
      updates: reservations.map(function (reservation) {
        return {
          sourceId: reservation.sourceId,
          issuedSequenceHighWatermark: reservation.captureSequence,
          latestAttempt: {
            operationId: reservation.captureOperationId,
            captureSequence: reservation.captureSequence,
            at: reservation.reservedAt,
            outcome: 'reserved',
            ownerId: options.ownerId
          }
        };
      })
    });
  });
  return {
    reservations: reservations,
    prospectiveReservations: prospectiveReservations,
    healthSnapshotHash: core.sourceHealthSnapshotHash(snapshot)
  };
}
function complete(options) {
  options = options || {};
  if (!Array.isArray(options.reservations) ||
      !Array.isArray(options.prospectiveReservations || []) ||
      !Array.isArray(options.unusedReservations || []) ||
      !Array.isArray(options.evidenceSources || []) ||
      !/^sha256:[a-f0-9]{64}$/.test(String(options.sourceIndexHash || ''))) {
    throw new Error('TOKEN_SOURCE_HEALTH_COMPLETION_INVALID');
  }
  var completedAt = now();
  var outcome = options.outcome;
  if (outcome !== 'published' && outcome !== 'no-op' && outcome !== 'failed') {
    throw new Error('TOKEN_SOURCE_HEALTH_COMPLETION_INVALID');
  }
  var evidenceBySource = Object.create(null);
  (options.evidenceSources || []).forEach(function (evidence) {
    if (!exact(evidence, ['sourceId', 'captureOperationId', 'captureSequence', 'captureEvidenceHash']) ||
        !/^otsrc:sha256:[a-f0-9]{64}$/.test(String(evidence.sourceId || '')) ||
        !/^tokop_[A-Za-z0-9_-]{16,96}$/.test(String(evidence.captureOperationId || '')) ||
        !Number.isSafeInteger(evidence.captureSequence) || evidence.captureSequence < 1 ||
        !/^sha256:[a-f0-9]{64}$/.test(String(evidence.captureEvidenceHash || '')) ||
        evidenceBySource[evidence.sourceId]) {
      throw new Error('TOKEN_SOURCE_HEALTH_EVIDENCE_INVALID');
    }
    evidenceBySource[evidence.sourceId] = evidence;
  });
  if (outcome === 'failed' && Object.keys(evidenceBySource).length ||
      (outcome === 'published' || outcome === 'no-op') &&
        (Object.keys(evidenceBySource).length !== options.reservations.length ||
          options.reservations.some(function (reservation) {
            var evidence = evidenceBySource[reservation.sourceId];
            return !evidence ||
              evidence.captureOperationId !== reservation.captureOperationId ||
              evidence.captureSequence !== reservation.captureSequence;
          }))) {
    throw new Error('TOKEN_SOURCE_HEALTH_EVIDENCE_INVALID');
  }
  return transact(options.sourceIndexHash, function (previous) {
    var records = recordBySource(previous), updates = [];
    var prospective = Object.create(null), prospectiveUsed = Object.create(null);
    var heldProspective = Object.create(null);
    previous.index.prospectiveReservations.forEach(function (reservation) {
      heldProspective[reservation.captureOperationId] = reservation;
    });
    (options.prospectiveReservations || []).forEach(function (reservation) {
      var held = heldProspective[reservation.captureOperationId];
      if (!held || held.captureSequence !== reservation.captureSequence ||
          held.reservedAt !== reservation.reservedAt || held.ownerId !== reservation.ownerId ||
          prospective[reservation.captureOperationId]) {
        throw new Error('TOKEN_SOURCE_PROSPECTIVE_RESERVATION_MISMATCH');
      }
      prospective[reservation.captureOperationId] = reservation;
    });
    options.reservations.forEach(function (reservation) {
      var record = records.get(reservation.sourceId);
      var prospectiveReservation = prospective[reservation.captureOperationId];
      if (record && prospectiveReservation) {
        throw new Error('TOKEN_SOURCE_PROSPECTIVE_RESERVATION_MISMATCH');
      }
      if (!record && prospectiveReservation) {
        if (prospectiveReservation.captureSequence !== reservation.captureSequence ||
            reservation.captureSequence !== 1 || prospectiveUsed[reservation.captureOperationId]) {
          throw new Error('TOKEN_SOURCE_PROSPECTIVE_RESERVATION_MISMATCH');
        }
        prospectiveUsed[reservation.captureOperationId] = 1;
        record = {
          sourceId: reservation.sourceId,
          issuedSequenceHighWatermark: 1,
          latestAttempt: {
            operationId: reservation.captureOperationId,
            captureSequence: 1,
            at: completedAt,
            outcome: 'reserved',
            ownerId: prospectiveReservation.ownerId
          }
        };
      }
      if (!record) throw new Error('TOKEN_SOURCE_HEALTH_RESERVATION_MISSING');
      if (!record || record.issuedSequenceHighWatermark > reservation.captureSequence) return;
      if (record.issuedSequenceHighWatermark !== reservation.captureSequence ||
          record.latestAttempt.operationId !== reservation.captureOperationId) return;
      var attempt = {
        operationId: reservation.captureOperationId,
        captureSequence: reservation.captureSequence,
        at: completedAt,
        outcome: outcome
      };
      if (outcome === 'published' || outcome === 'no-op') {
        attempt.evidenceHash = evidenceBySource[reservation.sourceId].captureEvidenceHash;
      }
      var update = {
        sourceId: reservation.sourceId,
        issuedSequenceHighWatermark: reservation.captureSequence,
        latestAttempt: attempt
      };
      if (outcome === 'failed') {
        update.failure = {
          operationId: reservation.captureOperationId,
          captureSequence: reservation.captureSequence,
          at: completedAt,
          code: options.errorCode || 'TOKEN_CAPTURE_FAILED',
          retryable: options.retryable !== false
        };
      }
      updates.push(update);
    });
    (options.unusedReservations || []).forEach(function (reservation) {
      var record = records.get(reservation.sourceId);
      if (!record || record.issuedSequenceHighWatermark !== reservation.captureSequence ||
          record.latestAttempt.operationId !== reservation.captureOperationId) return;
      updates.push({
        sourceId: reservation.sourceId,
        issuedSequenceHighWatermark: reservation.captureSequence,
        latestAttempt: {
          operationId: reservation.captureOperationId,
          captureSequence: reservation.captureSequence,
          at: completedAt,
          outcome: 'superseded'
        }
      });
    });
    var summaryOutcome = options.summaryOutcome ||
      (outcome === 'failed' ? 'failed' : outcome);
    return core.materializeSourceHealth({
      previous: previous,
      sourceIndexHash: options.sourceIndexHash,
      healthRevision: previous.index.healthRevision + 1,
      releaseProspectiveOperationIds: (options.prospectiveReservations || []).map(function (reservation) {
        return reservation.captureOperationId;
      }),
      updates: updates,
      jobSummary: {
        jobId: options.jobId,
        action: options.action || 'refresh-known-token-sources',
        startedAt: options.startedAt,
        finishedAt: completedAt,
        outcome: summaryOutcome,
        sourceCount: options.reservations.length,
        ...(outcome === 'failed' ? { errorCode: options.errorCode || 'TOKEN_CAPTURE_FAILED' } : {})
      }
    });
  });
}

function reconcileSyncReservations(options) {
  options = options || {};
  if (!options.terminalJobs || typeof options.terminalJobs !== 'object' ||
      Array.isArray(options.terminalJobs) || !Array.isArray(options.acceptedSources || []) ||
      options.ownerScanComplete !== true ||
      options.sourceIndexHash != null &&
        !/^sha256:[a-f0-9]{64}$/.test(String(options.sourceIndexHash || ''))) {
    throw new Error('TOKEN_SOURCE_HEALTH_RECOVERY_INVALID');
  }
  Object.keys(options.terminalJobs).forEach(function (ownerId) {
    var owner = options.terminalJobs[ownerId];
    if (!/^fsj-[a-f0-9]{32}$/.test(ownerId) || !owner ||
        ['success', 'partial', 'failed', 'cancelled', 'interrupted'].indexOf(owner.result) < 0 ||
        !canonicalInstant(owner.finishedAt)) {
      throw new Error('TOKEN_SOURCE_HEALTH_RECOVERY_INVALID');
    }
  });
  var present = peek();
  if (!present) return { changed: false, recoveredSources: 0, releasedProspects: 0 };
  var acceptedBySource = Object.create(null), acceptedByOperation = Object.create(null);
  (options.acceptedSources || []).forEach(function (source) {
    identity.sourceBucket(source.sourceId);
    if (!/^tokop_[A-Za-z0-9_-]{16,96}$/.test(String(source.captureOperationId || '')) ||
        !Number.isSafeInteger(source.captureSequence) || source.captureSequence < 1 ||
        !/^sha256:[a-f0-9]{64}$/.test(String(source.captureEvidenceHash || '')) ||
        acceptedBySource[source.sourceId] || acceptedByOperation[source.captureOperationId]) {
      throw new Error('TOKEN_SOURCE_HEALTH_RECOVERY_INVALID');
    }
    acceptedBySource[source.sourceId] = source;
    acceptedByOperation[source.captureOperationId] = source;
  });
  var targetIndexHash = options.sourceIndexHash || present.snapshot.index.sourceIndexRevision;
  var hasPendingSyncReservation = present.snapshot.index.prospectiveReservations.length > 0 ||
    present.snapshot.shards.some(function (shard) {
      return shard.records.some(function (record) {
        return record.latestAttempt && record.latestAttempt.outcome === 'reserved' &&
          /^fsj-/.test(String(record.latestAttempt.ownerId || ''));
      });
    });
  if (!hasPendingSyncReservation &&
      present.snapshot.index.sourceIndexRevision === targetIndexHash) {
    return { changed: false, recoveredSources: 0, releasedProspects: 0 };
  }
  var recoveredSources = 0, releasedProspects = 0, recoveredAt = now();
  var snapshot = transact(targetIndexHash, function (previous) {
    var records = recordBySource(previous), updates = [], releases = [];
    previous.shards.forEach(function (shard) {
      shard.records.forEach(function (record) {
        var attempt = record.latestAttempt;
        if (!attempt || attempt.outcome !== 'reserved' || !/^fsj-/.test(String(attempt.ownerId || ''))) return;
        var owner = options.terminalJobs[attempt.ownerId];
        var accepted = acceptedBySource[record.sourceId];
        var published = accepted &&
          accepted.captureOperationId === attempt.operationId &&
          accepted.captureSequence === attempt.captureSequence;
        if (!owner && published) throw new Error('TOKEN_SOURCE_HEALTH_RECOVERY_CONFLICT');
        var settledAt = owner ? owner.finishedAt : recoveredAt;
        var terminalAttempt = {
          operationId: attempt.operationId,
          captureSequence: attempt.captureSequence,
          at: settledAt,
          outcome: published ? 'published' : 'failed'
        };
        if (published) terminalAttempt.evidenceHash = accepted.captureEvidenceHash;
        var update = {
          sourceId: record.sourceId,
          issuedSequenceHighWatermark: record.issuedSequenceHighWatermark,
          latestAttempt: terminalAttempt
        };
        if (!published) {
          update.failure = {
            operationId: attempt.operationId,
            captureSequence: attempt.captureSequence,
            at: settledAt,
            code: 'TOKEN_CAPTURE_INTERRUPTED',
            retryable: true
          };
        }
        updates.push(update);
        recoveredSources++;
      });
    });
    previous.index.prospectiveReservations.forEach(function (reservation) {
      var owner = options.terminalJobs[reservation.ownerId];
      var accepted = acceptedByOperation[reservation.captureOperationId];
      if (!owner && accepted) throw new Error('TOKEN_SOURCE_HEALTH_RECOVERY_CONFLICT');
      if (accepted) {
        if (records.has(accepted.sourceId) ||
            accepted.captureSequence !== reservation.captureSequence) {
          throw new Error('TOKEN_SOURCE_HEALTH_RECOVERY_CONFLICT');
        }
        var recoveredAttempt = {
          operationId: accepted.captureOperationId,
          captureSequence: accepted.captureSequence,
          at: owner.finishedAt,
          outcome: 'published',
        };
        recoveredAttempt.evidenceHash = accepted.captureEvidenceHash;
        updates.push({
          sourceId: accepted.sourceId,
          issuedSequenceHighWatermark: accepted.captureSequence,
          latestAttempt: recoveredAttempt
        });
        recoveredSources++;
      }
      releases.push(reservation.captureOperationId);
      releasedProspects++;
    });
    return core.materializeSourceHealth({
      previous: previous,
      sourceIndexHash: targetIndexHash,
      healthRevision: previous.index.healthRevision + 1,
      updates: updates,
      releaseProspectiveOperationIds: releases
    });
  });
  return {
    changed: core.sourceHealthSnapshotHash(snapshot) !== core.sourceHealthSnapshotHash(present.snapshot),
    recoveredSources: recoveredSources,
    releasedProspects: releasedProspects
  };
}

function reconcileScreenReservations(options) {
  options = options || {};
  if (!Array.isArray(options.plans)) {
    throw new Error('TOKEN_SOURCE_SCREEN_OWNER_SCAN_INVALID');
  }
  var owners = Object.create(null);
  options.plans.forEach(function (plan) {
    if (!plan || !/^tokplan_[A-Za-z0-9_-]{16,96}$/.test(String(plan.planId || '')) ||
        !/^sha256:[a-f0-9]{64}$/.test(String(plan.sourceIndexHash || '')) ||
        !Array.isArray(plan.reservations) || owners[plan.planId]) {
      throw new Error('TOKEN_SOURCE_SCREEN_OWNER_SCAN_INVALID');
    }
    var held = Object.create(null);
    plan.reservations.forEach(function (reservation) {
      identity.sourceBucket(reservation.sourceId);
      if (!/^tokop_[A-Za-z0-9_-]{16,96}$/.test(String(reservation.captureOperationId || '')) ||
          !Number.isSafeInteger(reservation.captureSequence) || reservation.captureSequence < 1 ||
          held[reservation.sourceId]) {
        throw new Error('TOKEN_SOURCE_SCREEN_OWNER_SCAN_INVALID');
      }
      held[reservation.sourceId] = reservation;
    });
    owners[plan.planId] = held;
  });
  var present = peek();
  if (!present) return { changed: false, recoveredSources: 0 };
  var pending = [];
  present.snapshot.shards.forEach(function (shard) {
    shard.records.forEach(function (record) {
      var attempt = record.latestAttempt;
      if (!attempt || attempt.outcome !== 'reserved' ||
          !/^tokplan_/.test(String(attempt.ownerId || ''))) return;
      var owner = owners[attempt.ownerId];
      if (owner) {
        var reservation = owner[record.sourceId];
        if (!reservation || reservation.captureOperationId !== attempt.operationId ||
            reservation.captureSequence !== attempt.captureSequence) {
          throw new Error('TOKEN_SOURCE_SCREEN_OWNER_MISMATCH');
        }
        return;
      }
      pending.push(record.sourceId);
    });
  });
  if (!pending.length) return { changed: false, recoveredSources: 0 };
  var recoveredAt = now();
  var snapshot = transact(present.snapshot.index.sourceIndexRevision, function (previous) {
    var updates = [];
    previous.shards.forEach(function (shard) {
      shard.records.forEach(function (record) {
        var attempt = record.latestAttempt;
        if (!attempt || attempt.outcome !== 'reserved' ||
            !/^tokplan_/.test(String(attempt.ownerId || ''))) return;
        var owner = owners[attempt.ownerId];
        if (owner) {
          var reservation = owner[record.sourceId];
          if (!reservation || reservation.captureOperationId !== attempt.operationId ||
              reservation.captureSequence !== attempt.captureSequence) {
            throw new Error('TOKEN_SOURCE_SCREEN_OWNER_MISMATCH');
          }
          return;
        }
        updates.push({
          sourceId: record.sourceId,
          issuedSequenceHighWatermark: record.issuedSequenceHighWatermark,
          latestAttempt: {
            operationId: attempt.operationId,
            captureSequence: attempt.captureSequence,
            at: recoveredAt,
            outcome: 'failed'
          },
          failure: {
            operationId: attempt.operationId,
            captureSequence: attempt.captureSequence,
            at: recoveredAt,
            code: 'TOKEN_CAPTURE_INTERRUPTED',
            retryable: true
          }
        });
      });
    });
    return core.materializeSourceHealth({
      previous: previous,
      sourceIndexHash: previous.index.sourceIndexRevision,
      healthRevision: previous.index.healthRevision + 1,
      updates: updates
    });
  });
  return {
    changed: core.sourceHealthSnapshotHash(snapshot) !==
      core.sourceHealthSnapshotHash(present.snapshot),
    recoveredSources: pending.length
  };
}

module.exports = {
  current: current,
  peek: peek,
  highWatermarks: highWatermarks,
  sourceFreshness: sourceFreshness,
  reserveMany: reserveMany,
  complete: complete,
  reconcileSyncReservations: reconcileSyncReservations,
  reconcileScreenReservations: reconcileScreenReservations,
  recoverExact: recoverExact,
  ROOT: ROOT,
  SNAPSHOTS_DIR: SNAPSHOTS_DIR,
  POINTER_FILE: POINTER_FILE
};
